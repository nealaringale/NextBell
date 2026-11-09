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
import java.time.ZoneId;

/** Shows a deadline reminder only for a still-open planner item. */
public final class PlannerReminderReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "nextbell_planner_deadlines";

    @Override
    public void onReceive(Context context, Intent intent) {
        long id = StudentPlannerScheduler.idFromIntent(intent);
        if (id < 0L) return;
        android.content.SharedPreferences preferences = context.getSharedPreferences(
                SmsTransactionService.PREFS_NAME, Context.MODE_PRIVATE
        );
        StudentPlannerStore.Item item = StudentPlannerStore.find(preferences, id);
        if (item == null || item.completed || item.dueDate == null) return;

        NotificationManager manager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null || !manager.areNotificationsEnabled()) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(new NotificationChannel(
                    CHANNEL_ID, "Study deadlines", NotificationManager.IMPORTANCE_DEFAULT
            ));
        }

        long now = System.currentTimeMillis();
        Intent open = new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 9210, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String relative = item.dueDate.isAfter(LocalDate.now(ZoneId.of("Asia/Kolkata")))
                ? "Due tomorrow" : "Due today";
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(context, CHANNEL_ID)
                : new Notification.Builder(context);
        Notification notification = builder
                .setSmallIcon(R.drawable.ic_nextbell_notification)
                .setContentTitle(relative + " · " + item.type.toLowerCase(java.util.Locale.ENGLISH))
                .setContentText(item.title)
                .setStyle(new Notification.BigTextStyle().bigText(
                        item.title + (item.course.isEmpty() ? "" : " · " + item.course)
                ))
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .setCategory(Notification.CATEGORY_REMINDER)
                .setWhen(now)
                .build();
        manager.notify((int) (id ^ (id >>> 32)), notification);
    }
}
