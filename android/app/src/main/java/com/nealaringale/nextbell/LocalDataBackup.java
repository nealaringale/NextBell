package com.nealaringale.nextbell;

import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/** Readable, user-directed export and restore of the app's local preferences. */
public final class LocalDataBackup {
    public static final String MIME_TYPE = "application/json";
    private static final String FORMAT = "com.nealaringale.nextbell.backup";
    private static final int SCHEMA_VERSION = 1;
    private static final int MAX_FILE_BYTES = 1_048_576;
    private static final int MAX_PREFERENCES = 2_000;

    private LocalDataBackup() {}

    public static void write(SharedPreferences preferences, OutputStream output)
            throws IOException, JSONException {
        JSONObject document = new JSONObject();
        document.put("format", FORMAT);
        document.put("schemaVersion", SCHEMA_VERSION);
        document.put("createdAt", Instant.now().toString());
        document.put("appVersion", BuildConfig.VERSION_NAME);

        JSONObject values = new JSONObject();
        for (Map.Entry<String, ?> entry : preferences.getAll().entrySet()) {
            values.put(entry.getKey(), encode(entry.getValue()));
        }
        document.put("preferences", values);
        output.write(document.toString(2).getBytes(StandardCharsets.UTF_8));
        output.flush();
    }

    public static Map<String, Object> read(InputStream input)
            throws IOException, JSONException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int total = 0;
        int count;
        while ((count = input.read(buffer)) != -1) {
            total += count;
            if (total > MAX_FILE_BYTES) {
                throw new IOException("This backup file is too large.");
            }
            bytes.write(buffer, 0, count);
        }

        JSONObject document = new JSONObject(bytes.toString(StandardCharsets.UTF_8.name()));
        if (!FORMAT.equals(document.optString("format"))
                || document.optInt("schemaVersion", -1) != SCHEMA_VERSION) {
            throw new JSONException("This is not a supported NextBell backup.");
        }

        JSONObject values = document.optJSONObject("preferences");
        if (values == null || values.length() > MAX_PREFERENCES) {
            throw new JSONException("The backup data is missing or invalid.");
        }

        Map<String, Object> restored = new HashMap<>();
        JSONArray names = values.names();
        if (names == null) return restored;
        for (int i = 0; i < names.length(); i++) {
            String key = names.getString(i);
            if (key.isEmpty() || key.length() > 150) {
                throw new JSONException("The backup contains an invalid setting.");
            }
            JSONObject encoded = values.optJSONObject(key);
            if (encoded == null) {
                throw new JSONException("The backup contains an invalid setting.");
            }
            restored.put(key, decode(encoded));
        }
        return restored;
    }

    public static void restore(SharedPreferences preferences, Map<String, Object> values)
            throws IOException {
        SharedPreferences.Editor editor = preferences.edit().clear();
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof String) {
                editor.putString(key, (String) value);
            } else if (value instanceof Boolean) {
                editor.putBoolean(key, (Boolean) value);
            } else if (value instanceof Integer) {
                editor.putInt(key, (Integer) value);
            } else if (value instanceof Long) {
                editor.putLong(key, (Long) value);
            } else if (value instanceof Float) {
                editor.putFloat(key, (Float) value);
            } else if (value instanceof Set<?>) {
                @SuppressWarnings("unchecked")
                Set<String> strings = (Set<String>) value;
                editor.putStringSet(key, strings);
            } else {
                throw new IOException("The backup contains an unsupported setting.");
            }
        }
        if (!editor.commit()) {
            throw new IOException("NextBell could not save the restored data.");
        }
    }

    private static JSONObject encode(Object value) throws JSONException {
        JSONObject encoded = new JSONObject();
        if (value instanceof String) {
            encoded.put("type", "string");
            encoded.put("value", value);
        } else if (value instanceof Boolean) {
            encoded.put("type", "boolean");
            encoded.put("value", value);
        } else if (value instanceof Integer) {
            encoded.put("type", "integer");
            encoded.put("value", value);
        } else if (value instanceof Long) {
            encoded.put("type", "long");
            encoded.put("value", value);
        } else if (value instanceof Float) {
            encoded.put("type", "float");
            encoded.put("value", value);
        } else if (value instanceof Set<?>) {
            JSONArray items = new JSONArray();
            Set<String> sortedItems = new TreeSet<>();
            for (Object item : (Set<?>) value) {
                if (!(item instanceof String)) {
                    throw new JSONException("Unsupported value in a string setting.");
                }
                sortedItems.add((String) item);
            }
            for (String item : sortedItems) items.put(item);
            encoded.put("type", "string_set");
            encoded.put("value", items);
        } else {
            throw new JSONException("NextBell found a setting it cannot export.");
        }
        return encoded;
    }

    private static Object decode(JSONObject encoded) throws JSONException {
        String type = encoded.optString("type");
        switch (type) {
            case "string":
                return encoded.getString("value");
            case "boolean":
                return encoded.getBoolean("value");
            case "integer":
                return encoded.getInt("value");
            case "long":
                return encoded.getLong("value");
            case "float":
                double number = encoded.getDouble("value");
                if (Double.isNaN(number) || Double.isInfinite(number)
                        || number < -Float.MAX_VALUE || number > Float.MAX_VALUE) {
                    throw new JSONException("The backup contains an invalid number.");
                }
                return (float) number;
            case "string_set":
                JSONArray items = encoded.getJSONArray("value");
                Set<String> strings = new TreeSet<>();
                for (int i = 0; i < items.length(); i++) {
                    strings.add(items.getString(i));
                }
                return strings;
            default:
                throw new JSONException("The backup uses an unsupported setting type.");
        }
    }
}
