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

/** Locally parses only transaction-like SMS and saves derived ledger fields, never message text. */
public final class SmsTransactionService {
    public static final String PREFS_NAME = "nextbell_profile";
    public static final String KEY_ENABLED = "sms_transaction_tracking_enabled";
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
    public static boolean processMessage(SharedPreferences preferences, String sender,
                                         String body, LocalDate receivedDate) {
        SmsTransactionParser.ParsedTransaction parsed =
                SmsTransactionParser.parse(sender, body, receivedDate);
        if (parsed == null || parsed.fingerprint.isEmpty()
                || ExpenseStore.hasFingerprint(preferences, parsed.fingerprint)) return false;

        String category = parsed.category;
        if (SmsTransactionParser.DEBIT.equals(parsed.direction)) {
            String learned = ExpenseStore.learnedMerchantCategory(preferences, parsed.merchant);
            if (!learned.isEmpty()) category = learned;
        }
        ExpenseStore.upsert(preferences, new ExpenseStore.Expense(
                ExpenseStore.nextId(preferences), parsed.amountPaise, parsed.date,
                category, "", parsed.paymentMode, parsed.merchant, "SMS",
                parsed.confidence, parsed.fingerprint, parsed.direction,
                parsed.availableBalancePaise
        ));
        return true;
    }

    /** Explicit user action: scan at most the last 30 days of inbox messages on this device. */
    public static int importRecentInbox(Context context, SharedPreferences preferences) {
        if (!hasReadPermission(context)) return 0;
        long since = System.currentTimeMillis() - HISTORY_DAYS * 24L * 60L * 60L * 1000L;
        int imported = 0;
        int inspected = 0;
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
                if (processMessage(preferences, sender, body, receivedDate)) imported++;
            }
        } catch (SecurityException | IllegalArgumentException ignored) {
            return imported;
        }
        return imported;
    }
}
