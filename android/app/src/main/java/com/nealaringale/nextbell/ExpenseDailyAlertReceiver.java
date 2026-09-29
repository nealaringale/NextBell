package com.nealaringale.nextbell;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class ExpenseDailyAlertReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "nextbell_money_alerts";
    private static final int NOTIFICATION_ID = 7720;
    private static final int ALARM_REQUEST_CODE = 7717;

    @Override
    public void onReceive(Context context, Intent intent) {
        Context app = context.getApplicationContext();

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                NotificationManager manager =
                        (NotificationManager) app.getSystemService(Context.NOTIFICATION_SERVICE);
                if (manager == null || !manager.areNotificationsEnabled()) {
                    NotificationScheduler.scheduleUpcoming(app);
                    return;
                }
            }

            LocalDate today = LocalDate.now(java.time.ZoneId.of("Asia/Kolkata"));
            long spent = ExpenseStore.totalForDate(
                    app.getSharedPreferences("nextbell_profile", Context.MODE_PRIVATE),
                    today
            );

            android.content.SharedPreferences prefs =
                    app.getSharedPreferences("nextbell_profile", Context.MODE_PRIVATE);

            long threshold = ExpenseStore.dailyAlertThresholdPaise(prefs);
            long limit = ExpenseStore.dailyLimitPaise(prefs);
            boolean enabled = ExpenseStore.dailyAlertsEnabled(prefs);

            String dateKey = today.toString();
            String last = ExpenseStore.lastAlertNotifiedDate(prefs);

            if (enabled && !dateKey.equals(last)) {
                boolean thresholdReached = threshold > 0L && spent >= threshold;
                boolean limitExceeded = limit > 0L && spent > limit;

                if (thresholdReached || limitExceeded) {
                    showNotification(app, spent, threshold, limit, thresholdReached, limitExceeded);
                    ExpenseStore.setLastAlertNotifiedDate(prefs, dateKey);
                }
            }

            // Continue the daily alarm for tomorrow.
            NotificationScheduler.scheduleDailyMoneyAlert(
                    app,
                    (android.app.AlarmManager) app.getSystemService(Context.ALARM_SERVICE)
            );
        } catch (Exception ignored) {
        }
    }

    public static void maybeNotifyNow(Context context) {
        Context app = context.getApplicationContext();
        try {
            android.content.SharedPreferences prefs =
                    app.getSharedPreferences("nextbell_profile", Context.MODE_PRIVATE);
            if (!ExpenseStore.dailyAlertsEnabled(prefs)) return;

            LocalDate today = LocalDate.now(java.time.ZoneId.of("Asia/Kolkata"));
            String dateKey = today.toString();
            if (dateKey.equals(ExpenseStore.lastAlertNotifiedDate(prefs))) return;

            long spent = ExpenseStore.totalForDate(prefs, today);
            long threshold = ExpenseStore.dailyAlertThresholdPaise(prefs);
            long limit = ExpenseStore.dailyLimitPaise(prefs);

            boolean thresholdReached = threshold > 0L && spent >= threshold;
            boolean limitExceeded = limit > 0L && spent > limit;
            if (!thresholdReached && !limitExceeded) return;

            showNotification(app, spent, threshold, limit, thresholdReached, limitExceeded);
            ExpenseStore.setLastAlertNotifiedDate(prefs, dateKey);
        } catch (Exception ignored) {
        }
    }

    private static void showNotification(
            Context context,
            long spent,
            long threshold,
            long limit,
            boolean thresholdReached,
            boolean limitExceeded
    ) {
        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;

        ensureChannel(context);

        String title;
        if (limitExceeded) {
            title = "Daily limit exceeded";
        } else {
            title = "Daily spending alert";
        }

        StringBuilder body = new StringBuilder();
        body.append("Today: ").append(formatRupees(spent));

        if (thresholdReached && threshold > 0L) {
            body.append(" • alert at ").append(formatRupees(threshold));
        }
        if (limitExceeded && limit > 0L) {
            body.append(" • limit ").append(formatRupees(limit));
        }

        Intent openIntent = new Intent(context, MainActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(
                context,
                7721,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(context, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(context);
        }

        builder.setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("NextBell · " + title)
                .setContentText(body.toString())
                .setStyle(new Notification.BigTextStyle().bigText(
                        body + "\nOpen Money to review today's spending."
                ))
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .setCategory(Notification.CATEGORY_REMINDER)
                .setPriority(Notification.PRIORITY_DEFAULT);

        manager.notify(NOTIFICATION_ID, builder.build());
    }

    private static void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Money alerts",
                NotificationManager.IMPORTANCE_DEFAULT
        );
        channel.setDescription("Daily spending and budget alerts.");
        channel.setShowBadge(false);
        manager.createNotificationChannel(channel);
    }

    private static String formatRupees(long paise) {
        long rupees = Math.abs(paise) / 100L;
        java.text.NumberFormat formatter =
                java.text.NumberFormat.getIntegerInstance(Locale.ENGLISH);
        return (paise < 0 ? "-₹" : "₹") + formatter.format(rupees);
    }
}
