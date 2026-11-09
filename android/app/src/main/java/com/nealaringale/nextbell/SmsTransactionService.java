package com.nealaringale.nextbell;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.provider.Telephony;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Locally parses transaction alerts and saves derived ledger fields, never alert text. */
public final class SmsTransactionService {
    public static final String PREFS_NAME = "nextbell_profile";
    public static final String KEY_ENABLED = "sms_transaction_tracking_enabled";
    public static final String KEY_LAST_NOTIFICATION_AT = "payment_alert_last_seen_at";
    public static final String KEY_LAST_NOTIFICATION_PACKAGE = "payment_alert_last_package";
    public static final String KEY_LAST_NOTIFICATION_RESULT = "payment_alert_last_result";
    public static final String KEY_NOTIFICATION_IMPORTED_COUNT = "payment_alert_imported_count";
    public static final String KEY_NOTIFICATION_SEEN_COUNT = "payment_alert_seen_count";
    private static final int HISTORY_DAYS = 30;
    private static final int MAX_SCAN_MESSAGES = 2_000;
    private static final ZoneId INDIA = ZoneId.of("Asia/Kolkata");

    private SmsTransactionService() {}

    public static boolean isEnabled(SharedPreferences preferences) {
        return preferences.getBoolean(KEY_ENABLED, false);
    }

    public static void setEnabled(SharedPreferences preferences, boolean enabled) {
        preferences.edit().putBoolean(KEY_ENABLED, enabled).apply();
    }

    public static boolean hasReceivePermission(Context context) {
        return context.checkSelfPermission(Manifest.permission.RECEIVE_SMS)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean hasReadPermission(Context context) {
        return context.checkSelfPermission(Manifest.permission.READ_SMS)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean hasRequiredPermissions(Context context) {
        return hasReadPermission(context) && hasReceivePermission(context);
    }

    /** @return true only when a new supported transaction was added. */
    public static synchronized boolean processMessage(SharedPreferences preferences, String sender,
                                         String body, LocalDate receivedDate) {
        SmsTransactionParser.ParsedTransaction parsed =
                SmsTransactionParser.parse(sender, body, receivedDate);
        return saveParsedTransaction(preferences, parsed);
    }

    /** Records one supported notification and adds a matching transaction only once. */
    public static synchronized boolean processNotification(
            SharedPreferences preferences, String packageName, String title,
            String body, LocalDate receivedDate
    ) {
        SmsTransactionParser.ParsedTransaction parsed =
                SmsTransactionParser.parseNotification(packageName, title, body, receivedDate);
        if (parsed == null) {
            preferences.edit().putString(KEY_LAST_NOTIFICATION_RESULT, "Not a transaction alert").apply();
            return false;
        }
        if (parsed.fingerprint.isEmpty()
                || ExpenseStore.hasFingerprint(preferences, parsed.fingerprint)) {
            preferences.edit().putString(KEY_LAST_NOTIFICATION_RESULT, "Duplicate ignored").apply();
            return false;
        }
        ExpenseStore.upsert(preferences, toExpense(preferences, parsed,
                ExpenseStore.nextId(preferences)));
        preferences.edit()
                .putString(KEY_LAST_NOTIFICATION_RESULT, "Transaction added")
                .putLong(KEY_NOTIFICATION_IMPORTED_COUNT,
                        preferences.getLong(KEY_NOTIFICATION_IMPORTED_COUNT, 0L) + 1L)
                .apply();
        return true;
    }

    public static void recordNotificationSeen(SharedPreferences preferences, String packageName) {
        preferences.edit()
                .putLong(KEY_LAST_NOTIFICATION_AT, System.currentTimeMillis())
                .putString(KEY_LAST_NOTIFICATION_PACKAGE, packageName == null ? "" : packageName)
                .putLong(KEY_NOTIFICATION_SEEN_COUNT,
                        preferences.getLong(KEY_NOTIFICATION_SEEN_COUNT, 0L) + 1L)
                .putString(KEY_LAST_NOTIFICATION_RESULT, "Checking alert")
                .apply();
    }

    private static boolean saveParsedTransaction(
            SharedPreferences preferences, SmsTransactionParser.ParsedTransaction parsed
    ) {
        if (parsed == null || parsed.fingerprint.isEmpty()
                || ExpenseStore.hasFingerprint(preferences, parsed.fingerprint)) return false;
        ExpenseStore.upsert(preferences, toExpense(preferences, parsed,
                ExpenseStore.nextId(preferences)));
        return true;
    }

    private static ExpenseStore.Expense toExpense(
            SharedPreferences preferences,
            SmsTransactionParser.ParsedTransaction parsed,
            long id
    ) {
        String category = parsed.category;
        if (SmsTransactionParser.DEBIT.equals(parsed.direction)) {
            String learned = ExpenseStore.learnedMerchantCategory(preferences, parsed.merchant);
            if (!learned.isEmpty()) category = learned;
        }
        return new ExpenseStore.Expense(
                id, parsed.amountPaise, parsed.date, category, "", parsed.paymentMode,
                parsed.merchant, "SMS", parsed.confidence, parsed.fingerprint,
                parsed.direction, parsed.availableBalancePaise
        );
    }

    /** Explicit user action: scan at most the last 30 days of inbox messages on this device. */
    public static synchronized int importRecentInbox(Context context, SharedPreferences preferences) {
        if (!hasReadPermission(context)) return 0;
        long since = System.currentTimeMillis() - HISTORY_DAYS * 24L * 60L * 60L * 1000L;
        int imported = 0;
        int inspected = 0;
        List<ExpenseStore.Expense> expenses = ExpenseStore.load(preferences);
        Set<String> fingerprints = new HashSet<>();
        for (ExpenseStore.Expense expense : expenses) {
            if (!expense.fingerprint.isEmpty()) fingerprints.add(expense.fingerprint);
        }
        long nextId = ExpenseStore.nextId(preferences);
        try (Cursor cursor = context.getContentResolver().query(
                Telephony.Sms.Inbox.CONTENT_URI,
                new String[]{Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE},
                Telephony.Sms.DATE + " >= ?",
                new String[]{Long.toString(since)},
                Telephony.Sms.DATE + " DESC"
        )) {
            if (cursor == null) return 0;
            int addressIndex = cursor.getColumnIndex(Telephony.Sms.ADDRESS);
            int bodyIndex = cursor.getColumnIndex(Telephony.Sms.BODY);
            int dateIndex = cursor.getColumnIndex(Telephony.Sms.DATE);
            if (addressIndex < 0 || bodyIndex < 0 || dateIndex < 0) return 0;
            while (cursor.moveToNext() && inspected++ < MAX_SCAN_MESSAGES) {
                String sender = cursor.getString(addressIndex);
                String body = cursor.getString(bodyIndex);
                long timestamp = cursor.getLong(dateIndex);
                LocalDate receivedDate = Instant.ofEpochMilli(timestamp).atZone(INDIA).toLocalDate();
                SmsTransactionParser.ParsedTransaction parsed =
                        SmsTransactionParser.parse(sender, body, receivedDate);
                if (parsed == null || parsed.fingerprint.isEmpty()
                        || !fingerprints.add(parsed.fingerprint)) continue;
                expenses.add(toExpense(preferences, parsed, nextId++));
                imported++;
            }
        } catch (SecurityException | IllegalArgumentException ignored) {
        }
        if (imported > 0) ExpenseStore.save(preferences, expenses);
        return imported;
    }
}
