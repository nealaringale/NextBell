package com.nealaringale.nextbell;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class ClassReminderReceiver extends BroadcastReceiver {
    public static final String EXTRA_SUBJECT = "subject";
    public static final String EXTRA_START = "start";
    public static final String EXTRA_ROOM = "room";
    public static final String EXTRA_BATCH = "batch";
    public static final String EXTRA_DAY = "day";

    @Override
    public void onReceive(Context context, Intent intent) {
        NotificationScheduler.ensureChannel(context);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            NotificationManager manager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager == null || !manager.areNotificationsEnabled()) {
                return;
            }
        }

        String subject = safe(intent.getStringExtra(EXTRA_SUBJECT), "Upcoming class");
        String start = safe(intent.getStringExtra(EXTRA_START), "");
        String room = safe(intent.getStringExtra(EXTRA_ROOM), "");
        String batch = safe(intent.getStringExtra(EXTRA_BATCH), "");

        String roomLine = room.isEmpty() ? "" : "📍 " + room;
        String batchLine = batch.isEmpty() ? "" : " · " + batch;

        Intent openIntent = new Intent(context, MainActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(
                context,
                Math.abs((subject + start + room).hashCode()),
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        android.app.Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new android.app.Notification.Builder(context, NotificationScheduler.CHANNEL_ID);
        } else {
            builder = new android.app.Notification.Builder(context);
        }

        builder
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle("NextBell · 15 min")
                .setContentText(subject + (start.isEmpty() ? "" : " starts at " + formatTime(start)))
                .setStyle(new android.app.Notification.BigTextStyle()
                        .bigText(subject
                                + (start.isEmpty() ? "" : "\nStarts at " + formatTime(start))
                                + batchLine
                                + (roomLine.isEmpty() ? "" : "\n" + roomLine)))
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .setCategory(android.app.Notification.CATEGORY_REMINDER)
                .setPriority(android.app.Notification.PRIORITY_HIGH);

        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(Math.abs((subject + start + room + System.currentTimeMillis()).hashCode()), builder.build());
        }
    }

    private static String safe(String value, String fallback) {
        return value == null ? fallback : value;
    }

    private static String formatTime(String value) {
        java.time.LocalTime time = java.time.LocalTime.parse(value);
        int h = time.getHour();
        String suffix = h >= 12 ? "PM" : "AM";
        int hour = h % 12 == 0 ? 12 : h % 12;
        return hour + ":" + String.format("%02d", time.getMinute()) + " " + suffix;
    }
}
