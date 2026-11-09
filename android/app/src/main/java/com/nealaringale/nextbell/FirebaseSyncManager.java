package com.nealaringale.nextbell;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Account-scoped Firebase sync shared with the NextBell web client.
 *
 * Each record is stored separately under users/{uid}/records so changes to
 * different expenses/tasks do not overwrite one another. Deletions are
 * represented by tombstones, preventing stale offline clients from reviving
 * deleted records when they reconnect.
 */
public final class FirebaseSyncManager implements SharedPreferences.OnSharedPreferenceChangeListener {
    public interface StatusListener {
        void onStatusChanged(String status);
        void onRemoteDataChanged();
    }

    private static final String KEY_CLOUD_OWNER_UID = "firebase_sync_owner_uid";
    private static final String[] COLLECTIONS = {
            "expenses", "attendance", "tasks", "recurringExpenses"
    };
    private static final Map<String, String> PREF_KEYS = new HashMap<>();
    static {
        PREF_KEYS.put("expenses", "expenses_v1");
        PREF_KEYS.put("attendance", "attendance_sessions_v1");
        PREF_KEYS.put("tasks", "student_planner_items_v1");
        PREF_KEYS.put("recurringExpenses", "recurring_expenses_v1");
    }

    private final Context appContext;
    private final SharedPreferences preferences;
    private final StatusListener statusListener;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Map<String, SyncedRecord> knownRecords = new HashMap<>();

    private FirebaseApp firebaseApp;
    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private FirebaseAuth.AuthStateListener authStateListener;
    private ListenerRegistration recordsListener;
    private String activeUid = "";
    private boolean initialized;
    private volatile boolean applyingRemote;
    private boolean preferencesListenerRegistered;
    private String status = "Firebase is not configured";

    public FirebaseSyncManager(Context context, SharedPreferences preferences,
                               StatusListener statusListener) {
        this.appContext = context.getApplicationContext();
        this.preferences = preferences;
        this.statusListener = statusListener;
        try {
            firebaseApp = FirebaseApp.initializeApp(appContext);
            if (firebaseApp != null) {
                auth = FirebaseAuth.getInstance(firebaseApp);
                firestore = FirebaseFirestore.getInstance(firebaseApp);
                status = "Firebase ready · sign in to enable sync";
            }
        } catch (RuntimeException error) {
            status = "Firebase setup error: " + error.getClass().getSimpleName();
        }
    }

    public boolean isAvailable() {
        return auth != null && firestore != null;
    }

    public FirebaseUser currentUser() {
        return auth == null ? null : auth.getCurrentUser();
    }

    public String status() {
        return status;
    }

    public void attach() {
        if (!isAvailable() || authStateListener != null) return;
        authStateListener = firebaseAuth -> {
            FirebaseUser user = firebaseAuth.getCurrentUser();
            if (user == null) {
                stopSync();
                setStatus("Not connected");
            } else if (!user.getUid().equals(activeUid) || !initialized) {
                stopSync();
                startSync(user.getUid());
                setStatus("Signed in · connecting data…");
            }
        };
        auth.addAuthStateListener(authStateListener);
    }

    public com.google.android.gms.tasks.Task<AuthResult> signIn(String email, String password) {
        if (!isAvailable()) throw new IllegalStateException("Add android/app/google-services.json first.");
        return auth.signInWithEmailAndPassword(email.trim(), password);
    }

    public com.google.android.gms.tasks.Task<AuthResult> createAccount(String email, String password) {
        if (!isAvailable()) throw new IllegalStateException("Add android/app/google-services.json first.");
        return auth.createUserWithEmailAndPassword(email.trim(), password);
    }

    public void signOut() {
        if (auth != null) auth.signOut();
    }

    public void destroy() {
        stopSync();
        if (auth != null && authStateListener != null) {
            auth.removeAuthStateListener(authStateListener);
            authStateListener = null;
        }
    }

    private void startSync(String uid) {
        String previousOwner = preferences.getString(KEY_CLOUD_OWNER_UID, "");
        if (!previousOwner.isEmpty() && !previousOwner.equals(uid)) {
            clearSyncedLocalState();
            setStatus("Switched Firebase account · loading its data");
        }
        preferences.edit().putString(KEY_CLOUD_OWNER_UID, uid).apply();

        activeUid = uid;
        initialized = false;
        knownRecords.clear();
        if (!preferencesListenerRegistered) {
            preferences.registerOnSharedPreferenceChangeListener(this);
            preferencesListenerRegistered = true;
        }

        recordsListener = firestore.collection("users")
                .document(uid)
                .collection("records")
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        setStatus("Sync error: " + error.getMessage());
                        return;
                    }
                    if (snapshot == null) return;
                    if (!initialized) {
                        // Do not interpret an empty local cache as an empty cloud account.
                        if (snapshot.getMetadata().isFromCache()) {
                            setStatus("Waiting for Firebase connection…");
                            return;
                        }
                        initializeFromCloud(snapshot);
                    } else {
                        applyRemoteChanges(snapshot);
                    }
                });
    }

    /** Prevents a different Firebase account from inheriting the previous account's local data. */
    private void clearSyncedLocalState() {
        SharedPreferences.Editor editor = preferences.edit();
        for (String key : PREF_KEYS.values()) editor.putString(key, "[]");

        editor.remove("name")
                .remove("roll_number")
                .remove("firebase_profile_extras_v1")
                .remove("firebase_settings_extras_v1")
                .putInt("theme", 0)
                .putInt("attendance_target_percent", 75)
                .putLong("expense_daily_limit", 0L)
                .putLong("expense_weekly_allowance", 0L)
                .putLong("expense_alert_threshold", 5000L)
                .putBoolean("expense_alert_enabled", true)
                .putString("expense_merchant_categories", "{}");

        for (String key : new ArrayList<>(preferences.getAll().keySet())) {
            if (key.startsWith("expense_budget_")) editor.remove(key);
        }
        editor.commit();
    }

    private void stopSync() {
        if (recordsListener != null) {
            recordsListener.remove();
            recordsListener = null;
        }
        if (preferencesListenerRegistered) {
            preferences.unregisterOnSharedPreferenceChangeListener(this);
            preferencesListenerRegistered = false;
        }
        activeUid = "";
        initialized = false;
        knownRecords.clear();
    }

    private void initializeFromCloud(QuerySnapshot snapshot) {
        Map<String, Map<String, SyncedRecord>> remoteByCollection = new HashMap<>();
        for (DocumentSnapshot document : snapshot.getDocuments()) {
            SyncedRecord record = fromDocument(document);
            if (record == null) continue;
            knownRecords.put(document.getId(), record);
            remoteByCollection.computeIfAbsent(record.collection, ignored -> new HashMap<>())
                    .put(record.recordId, record);
        }

        applyingRemote = true;
        try {
            for (String collection : COLLECTIONS) {
                String preferenceKey = PREF_KEYS.get(collection);
                Map<String, JSONObject> merged = readLocalRows(preferenceKey);
                Map<String, SyncedRecord> remote = remoteByCollection.get(collection);
                if (remote != null) {
                    for (SyncedRecord record : remote.values()) {
                        if (record.deleted || record.payload == null) {
                            merged.remove(record.recordId);
                        } else {
                            merged.put(record.recordId, record.payload);
                        }
                    }
                }

                // Local-only records are migrated; cloud versions win for the same ID.
                for (Map.Entry<String, JSONObject> entry : readLocalRows(preferenceKey).entrySet()) {
                    if (remote == null || !remote.containsKey(entry.getKey())) {
                        writeRecord(collection, entry.getKey(), entry.getValue(), false);
                    }
                }
                saveRows(preferenceKey, merged);
            }

            SyncedRecord cloudProfile = getRemoteSingleton(remoteByCollection, "profile");
            if (cloudProfile != null && !cloudProfile.deleted && cloudProfile.payload != null) {
                applyProfile(cloudProfile.payload);
            } else {
                writeSingleton("profile", localProfile());
            }

            SyncedRecord cloudSettings = getRemoteSingleton(remoteByCollection, "settings");
            if (cloudSettings != null && !cloudSettings.deleted && cloudSettings.payload != null) {
                applySettings(cloudSettings.payload);
            } else {
                writeSingleton("settings", localSettings());
            }
        } finally {
            applyingRemote = false;
        }

        initialized = true;
        // Upload any local records not seen in the first remote snapshot.
        for (String collection : COLLECTIONS) {
            syncLocalCollection(collection);
        }
        setStatus("Synced · live updates enabled");
        notifyRemoteDataChanged();
    }

    private SyncedRecord getRemoteSingleton(Map<String, Map<String, SyncedRecord>> remote,
                                            String collection) {
        Map<String, SyncedRecord> values = remote.get(collection);
        return values == null ? null : values.get("__singleton__");
    }

    private void applyRemoteChanges(QuerySnapshot snapshot) {
        boolean changed = false;
        for (DocumentSnapshot document : snapshot.getDocuments()) {
            SyncedRecord record = fromDocument(document);
            if (record == null) continue;
            SyncedRecord previous = knownRecords.get(document.getId());
            // The snapshot represents Firestore's committed document state.
            // Client clocks can be skewed and offline writes can arrive late, so
            // compare content rather than trusting local wall-clock timestamps.
            boolean recordChanged = previous == null
                    || record.deleted != previous.deleted
                    || !safeJson(record.payload).equals(safeJson(previous.payload));
            knownRecords.put(document.getId(), record);
            if (!recordChanged || document.getMetadata().hasPendingWrites()) continue;

            if ("profile".equals(record.collection)) {
                if (!record.deleted && record.payload != null) applyProfile(record.payload);
                changed = true;
            } else if ("settings".equals(record.collection)) {
                if (!record.deleted && record.payload != null) applySettings(record.payload);
                changed = true;
            } else if (PREF_KEYS.containsKey(record.collection)) {
                applyRecord(record);
                changed = true;
            }
        }
        if (changed) notifyRemoteDataChanged();
        setStatus("Synced · live updates enabled");
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (applyingRemote || !initialized || key == null) return;
        if (PREF_KEYS.containsValue(key)) {
            for (Map.Entry<String, String> entry : PREF_KEYS.entrySet()) {
                if (entry.getValue().equals(key)) syncLocalCollection(entry.getKey());
            }
            return;
        }
        if ("name".equals(key) || "roll_number".equals(key)) {
            writeSingleton("profile", localProfile());
        } else if ("theme".equals(key) || "attendance_target_percent".equals(key)
                || "expense_daily_limit".equals(key)
                || "expense_weekly_allowance".equals(key)
                || "expense_alert_threshold".equals(key)
                || "expense_alert_enabled".equals(key)
                || "expense_merchant_categories".equals(key)
                || key.startsWith("expense_budget_")) {
            writeSingleton("settings", localSettings());
        }
    }

    private void syncLocalCollection(String collection) {
        String preferenceKey = PREF_KEYS.get(collection);
        if (preferenceKey == null || !initialized || activeUid.isEmpty()) return;
        Map<String, JSONObject> rows = readLocalRows(preferenceKey);
        Set<String> present = new HashSet<>(rows.keySet());

        for (Map.Entry<String, JSONObject> entry : rows.entrySet()) {
            String id = entry.getKey();
            String docId = documentId(collection, id);
            SyncedRecord known = knownRecords.get(docId);
            String payload = safeJson(entry.getValue());
            if (known != null && !known.deleted && payload.equals(safeJson(known.payload))) continue;
            writeRecord(collection, id, entry.getValue(), false);
        }

        for (Map.Entry<String, SyncedRecord> entry : new ArrayList<>(knownRecords.entrySet())) {
            SyncedRecord known = entry.getValue();
            if (collection.equals(known.collection) && !known.deleted
                    && !"__singleton__".equals(known.recordId)
                    && !present.contains(known.recordId)) {
                writeRecord(collection, known.recordId, null, true);
            }
        }
    }

    private void applyRecord(SyncedRecord record) {
        String preferenceKey = PREF_KEYS.get(record.collection);
        if (preferenceKey == null) return;
        Map<String, JSONObject> rows = readLocalRows(preferenceKey);
        if (record.deleted || record.payload == null) rows.remove(record.recordId);
        else rows.put(record.recordId, record.payload);
        applyingRemote = true;
        try {
            saveRows(preferenceKey, rows);
        } finally {
            applyingRemote = false;
        }
    }

    private void writeRecord(String collection, String id, JSONObject payload, boolean deleted) {
        if (firestore == null || activeUid.isEmpty()) return;
        long updatedAt = System.currentTimeMillis();
        String docId = documentId(collection, id);
        SyncedRecord record = new SyncedRecord(collection, id, payload, deleted, updatedAt);
        knownRecords.put(docId, record);

        Map<String, Object> value = new HashMap<>();
        value.put("collection", collection);
        value.put("recordId", id);
        value.put("payloadJson", deleted ? "" : safeJson(payload));
        value.put("deleted", deleted);
        value.put("updatedAtMs", updatedAt);
        value.put("schemaVersion", 1L);

        firestore.collection("users").document(activeUid).collection("records")
                .document(docId).set(value, SetOptions.merge())
                .addOnFailureListener(error -> setStatus(
                        "Waiting to sync: " + error.getMessage()
                ));
    }

    private void writeSingleton(String collection, JSONObject payload) {
        writeRecord(collection, "__singleton__", payload, false);
    }

    private void applyProfile(JSONObject profile) {
        applyingRemote = true;
        try {
            JSONObject extras;
            try {
                extras = new JSONObject(profile.toString());
            } catch (org.json.JSONException ignored) {
                extras = new JSONObject();
            }
            extras.remove("name");
            extras.remove("rollNo");
            extras.remove("roll_number");
            SharedPreferences.Editor editor = preferences.edit();
            String name = profile.optString("name", "");
            if (!name.trim().isEmpty()) editor.putString("name", name);
            int roll = profile.optInt("rollNo", profile.optInt("roll_number", 0));
            if (roll > 0 && roll <= 120) editor.putInt("roll_number", roll);
            editor.putString("firebase_profile_extras_v1", extras.toString());
            editor.commit();
        } finally {
            applyingRemote = false;
        }
    }

    private void applySettings(JSONObject settings) {
        applyingRemote = true;
        try {
            SharedPreferences.Editor editor = preferences.edit();
            if (settings.has("themeIndex")) {
                editor.putInt("theme", Math.max(0, Math.min(5, settings.optInt("themeIndex", 0))));
            }
            if (settings.has("attendanceTarget")) {
                editor.putInt("attendance_target_percent",
                        Math.max(1, Math.min(100, settings.optInt("attendanceTarget", 75))));
            }
            if (settings.has("dailyLimitPaise")) {
                editor.putLong("expense_daily_limit",
                        Math.max(0L, settings.optLong("dailyLimitPaise", 0L)));
            }
            if (settings.has("weeklyAllowancePaise")) {
                editor.putLong("expense_weekly_allowance",
                        Math.max(0L, settings.optLong("weeklyAllowancePaise", 0L)));
            }
            if (settings.has("dailyAlertThresholdPaise")) {
                editor.putLong("expense_alert_threshold",
                        Math.max(0L, settings.optLong("dailyAlertThresholdPaise", 5000L)));
            }
            if (settings.has("dailyAlertsEnabled")) {
                editor.putBoolean("expense_alert_enabled",
                        settings.optBoolean("dailyAlertsEnabled", true));
            }
            JSONObject budgets = settings.optJSONObject("monthlyBudgets");
            if (budgets != null) {
                Iterator<String> budgetKeys = budgets.keys();
                while (budgetKeys.hasNext()) {
                    String month = budgetKeys.next();
                    if (month.matches("\\d{4}-\\d{2}")) {
                        editor.putLong("expense_budget_" + month,
                                Math.max(0L, budgets.optLong(month, 0L)));
                    }
                }
            }
            JSONObject merchantCategories = settings.optJSONObject("merchantCategories");
            if (merchantCategories != null) {
                editor.putString("expense_merchant_categories", merchantCategories.toString());
            }
            JSONObject extras;
            try {
                extras = new JSONObject(settings.toString());
            } catch (org.json.JSONException ignored) {
                extras = new JSONObject();
            }
            extras.remove("themeIndex");
            extras.remove("attendanceTarget");
            extras.remove("dailyLimitPaise");
            extras.remove("weeklyAllowancePaise");
            editor.putString("firebase_settings_extras_v1", extras.toString());
            editor.commit();
        } finally {
            applyingRemote = false;
        }
    }

    private JSONObject localProfile() {
        JSONObject value;
        try {
            value = new JSONObject(preferences.getString("firebase_profile_extras_v1", "{}"));
        } catch (Exception ignored) {
            value = new JSONObject();
        }
        try {
            value.put("name", preferences.getString("name", ""));
            value.put("rollNo", preferences.getInt("roll_number", 34));
        } catch (Exception ignored) {}
        return value;
    }

    private JSONObject localSettings() {
        JSONObject value;
        try {
            value = new JSONObject(preferences.getString("firebase_settings_extras_v1", "{}"));
        } catch (Exception ignored) {
            value = new JSONObject();
        }
        try {
            value.put("themeIndex", preferences.getInt("theme", 0));
            value.put("attendanceTarget", preferences.getInt("attendance_target_percent", 75));
            value.put("dailyLimitPaise", preferences.getLong("expense_daily_limit", 0L));
            value.put("weeklyAllowancePaise", preferences.getLong("expense_weekly_allowance", 0L));
            value.put("dailyAlertThresholdPaise", preferences.getLong("expense_alert_threshold", 5000L));
            value.put("dailyAlertsEnabled", preferences.getBoolean("expense_alert_enabled", true));

            JSONObject monthlyBudgets = new JSONObject();
            for (Map.Entry<String, ?> entry : preferences.getAll().entrySet()) {
                if (entry.getKey().startsWith("expense_budget_")
                        && entry.getValue() instanceof Long) {
                    String month = entry.getKey().substring("expense_budget_".length());
                    if (month.matches("\\d{4}-\\d{2}")) {
                        monthlyBudgets.put(month, (Long) entry.getValue());
                    }
                }
            }
            value.put("monthlyBudgets", monthlyBudgets);
            JSONObject merchantCategories = new JSONObject(
                    preferences.getString("expense_merchant_categories", "{}")
            );
            value.put("merchantCategories", merchantCategories);
        } catch (Exception ignored) {}
        return value;
    }

    private Map<String, JSONObject> readLocalRows(String preferenceKey) {
        Map<String, JSONObject> values = new LinkedHashMap<>();
        JSONArray array;
        try {
            array = new JSONArray(preferences.getString(preferenceKey, "[]"));
        } catch (Exception ignored) {
            return values;
        }
        for (int i = 0; i < array.length(); i++) {
            JSONObject item = array.optJSONObject(i);
            if (item == null) continue;
            String id = "attendance_sessions_v1".equals(preferenceKey)
                    ? item.optString("key", "") : item.optString("id", "");
            if (!id.isEmpty()) values.put(id, item);
        }
        return values;
    }

    private void saveRows(String preferenceKey, Map<String, JSONObject> rows) {
        JSONArray array = new JSONArray();
        for (JSONObject value : rows.values()) array.put(value);
        preferences.edit().putString(preferenceKey, array.toString()).commit();
    }

    private SyncedRecord fromDocument(DocumentSnapshot document) {
        String collection = document.getString("collection");
        String id = document.getString("recordId");
        if (collection == null || id == null) return null;
        String raw = document.getString("payloadJson");
        JSONObject payload = null;
        if (raw != null && !raw.isEmpty()) {
            try {
                payload = new JSONObject(raw);
            } catch (Exception ignored) {}
        }
        Long updatedAt = document.getLong("updatedAtMs");
        Boolean deleted = document.getBoolean("deleted");
        return new SyncedRecord(collection, id, payload, Boolean.TRUE.equals(deleted),
                updatedAt == null ? 0L : updatedAt);
    }

    private String documentId(String collection, String id) {
        return collection + "__" + Uri.encode(id);
    }

    private String safeJson(JSONObject value) {
        return value == null ? "" : value.toString();
    }

    private void setStatus(String next) {
        status = next == null ? "" : next;
        if (statusListener != null) {
            mainHandler.post(() -> statusListener.onStatusChanged(status));
        }
    }

    private void notifyRemoteDataChanged() {
        if (statusListener != null) {
            mainHandler.post(statusListener::onRemoteDataChanged);
        }
    }

    private static final class SyncedRecord {
        final String collection;
        final String recordId;
        final JSONObject payload;
        final boolean deleted;
        final long updatedAtMs;

        SyncedRecord(String collection, String recordId, JSONObject payload,
                     boolean deleted, long updatedAtMs) {
            this.collection = collection;
            this.recordId = recordId;
            this.payload = payload;
            this.deleted = deleted;
            this.updatedAtMs = updatedAtMs;
        }
    }
}
