package com.nealaringale.nextbell;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony;

import java.time.Instant;
import java.time.ZoneId;
import java.time.LocalDate;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class SmsAutoTracker {
    public interface Callback {
        void onComplete(int imported);
    }

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final String PREF_AUTO = "expense_sms_auto_enabled";
    private static final String PREF_LAST_SYNC = "expense_sms_last_sync";

    private SmsAutoTracker() {}

    public static boolean isEnabled(SharedPreferences prefs) {
        return prefs.getBoolean(PREF_AUTO, false);
    }

    public static void setEnabled(SharedPreferences prefs, boolean enabled) {
        prefs.edit().putBoolean(PREF_AUTO, enabled).apply();
    }

    public static boolean hasPermission(Context context) {
        return context.checkSelfPermission(Manifest.permission.READ_SMS)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean hasReceivePermission(Context context) {
        return context.checkSelfPermission(Manifest.permission.RECEIVE_SMS)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static void syncInbox(Context context, Callback callback) {
        Context app = context.getApplicationContext();
        EXECUTOR.execute(() -> {
            int imported = scanInbox(app);
            if (callback != null) {
                callback.onComplete(imported);
            }
        });
    }

    public static void processIncoming(Context context, String sender, String body, long smsTime) {
        Context app = context.getApplicationContext();
        EXECUTOR.execute(() -> importOne(app, sender, body, smsTime));
    }

    private static int scanInbox(Context context) {
        if (!hasPermission(context)) return 0;

        int imported = 0;
        long since = System.currentTimeMillis() - 365L * 24L * 60L * 60L * 1000L;
        Uri inbox = Telephony.Sms.Inbox.CONTENT_URI;

        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(
                    inbox,
                    new String[]{
                            Telephony.Sms._ID,
                            Telephony.Sms.ADDRESS,
                            Telephony.Sms.DATE,
                            Telephony.Sms.BODY
                    },
                    Telephony.Sms.DATE + " >= ?",
                    new String[]{String.valueOf(since)},
                    Telephony.Sms.DATE + " DESC"
            );

            if (cursor == null) return 0;

            while (cursor.moveToNext()) {
                String sender = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS));
                long date = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms.DATE));
                String body = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.BODY));

                if (importOne(context, sender, body, date)) imported++;
            }

            context.getSharedPreferences(MainActivity.PREFS_NAME_PUBLIC, Context.MODE_PRIVATE)
                    .edit()
                    .putLong(PREF_LAST_SYNC, System.currentTimeMillis())
                    .apply();
            return imported;
        } catch (SecurityException ignored) {
            return 0;
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    private static boolean importOne(
            Context context,
            String sender,
            String body,
            long smsTime
    ) {
        if (!isEnabled(context.getSharedPreferences(MainActivity.PREFS_NAME_PUBLIC, Context.MODE_PRIVATE))
                || !hasPermission(context)) {
            return false;
        }

        SmsTransactionParser.ParsedTransaction parsed =
                SmsTransactionParser.parse(sender, body, smsTime);
        if (parsed == null) return false;

        SharedPreferences prefs =
                context.getSharedPreferences(MainActivity.PREFS_NAME_PUBLIC, Context.MODE_PRIVATE);

        if (ExpenseStore.hasFingerprint(prefs, parsed.fingerprint)) return false;

        String learned = ExpenseStore.learnedMerchantCategory(prefs, parsed.merchant);
        String category = learned.isEmpty() ? parsed.category : learned;

        String note = parsed.merchant.isEmpty()
                ? "Auto-detected from SMS"
                : parsed.merchant + "  •  Auto-detected";

        long id = Math.abs(
                (parsed.smsTime * 31L)
                        ^ parsed.amountPaise
                        ^ parsed.fingerprint.hashCode()
        );
        if (id == 0) id = System.nanoTime();

        ExpenseStore.upsert(
                prefs,
                new ExpenseStore.Expense(
                        id,
                        parsed.amountPaise,
                        LocalDate.ofInstant(
                                Instant.ofEpochMilli(parsed.smsTime),
                                ZoneId.of("Asia/Kolkata")
                        ),
                        category,
                        note,
                        parsed.paymentMode,
                        parsed.merchant,
                        "SMS",
                        parsed.confidence,
                        parsed.fingerprint
                )
        );
        return true;
    }
}
