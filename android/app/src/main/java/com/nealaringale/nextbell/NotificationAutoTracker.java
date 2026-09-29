package com.nealaringale.nextbell;

import android.app.Notification;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class NotificationAutoTracker {
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final String PREF_AUTO = "expense_auto_enabled";
    private static final String PREF_LAST_SYNC = "expense_auto_last_sync";

    private NotificationAutoTracker() {}

    public static boolean isEnabled(SharedPreferences prefs) {
        return prefs.getBoolean(PREF_AUTO, false);
    }

    public static void setEnabled(SharedPreferences prefs, boolean enabled) {
        prefs.edit().putBoolean(PREF_AUTO, enabled).apply();
    }

    public static boolean hasAccess(Context context) {
        ComponentName component = new ComponentName(context, PaymentNotificationListener.class);
        return android.app.NotificationManager.get(context)
                .isNotificationListenerAccessGranted(component);
    }

    public static void openAccessSettings(Context context) {
        try {
            IntentHelper.openNotificationSettings(context);
        } catch (Exception ignored) {
            // Device does not expose notification listener settings.
        }
    }

    public static void importActiveNotifications(Context context) {
        Context app = context.getApplicationContext();
        EXECUTOR.execute(() -> {
            if (!isEnabled(app.getSharedPreferences("nextbell_profile", Context.MODE_PRIVATE))
                    || !hasAccess(app)) return;

            try {
                PaymentNotificationListener service = PaymentNotificationListener.getCurrent();
                if (service != null) {
                    service.scanActiveNotifications();
                }
            } catch (Exception ignored) {
            }
        });
    }

    public static void processNotification(
            Context context,
            String packageName,
            String title,
            String body,
            long when
    ) {
        Context app = context.getApplicationContext();
        EXECUTOR.execute(() -> importParsed(
                app,
                packageName,
                ((title == null ? "" : title) + " " + (body == null ? "" : body)).trim(),
                when
        ));
    }

    static void importParsed(Context context, String packageName, String text, long when) {
        SharedPreferences prefs =
                context.getSharedPreferences("nextbell_profile", Context.MODE_PRIVATE);

        if (!isEnabled(prefs) || text.isEmpty()) return;

        SmsTransactionParser.ParsedTransaction parsed =
                SmsTransactionParser.parse(packageName, text, when);
        if (parsed == null) return;

        if (ExpenseStore.hasFingerprint(prefs, parsed.fingerprint)) return;

        String learned = ExpenseStore.learnedMerchantCategory(prefs, parsed.merchant);
        String category = learned.isEmpty() ? parsed.category : learned;
        String note = parsed.merchant.isEmpty()
                ? "Auto-detected from payment notification"
                : parsed.merchant + "  •  Auto-detected";

        long candidateId = (parsed.smsTime * 31L)
                ^ parsed.amountPaise
                ^ parsed.fingerprint.hashCode();
        long id = candidateId == Long.MIN_VALUE ? 1L : Math.abs(candidateId);
        if (id == 0L) id = Math.max(1L, System.nanoTime());

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
                        "NOTIFICATION",
                        parsed.confidence,
                        parsed.fingerprint
                )
        );
        prefs.edit().putLong(PREF_LAST_SYNC, System.currentTimeMillis()).apply();
    }

    // Tiny indirection keeps settings-launch code out of the service.
    private static final class IntentHelper {
        static void openNotificationSettings(Context context) {
            android.content.Intent intent =
                    new android.content.Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }
}
