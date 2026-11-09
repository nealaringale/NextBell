package com.nealaringale.nextbell;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

/** Approximate, user-visible deadline reminders; exact-alarm access is not required. */
public final class StudentPlannerScheduler {
    private static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");
    private static final String EXTRA_ITEM_ID = "planner_item_id";

    private StudentPlannerScheduler() {}

    public static void schedule(Context context, StudentPlannerStore.Item item) {
        if (item == null) return;
        cancel(context, item.id);
        if (item.completed || item.dueDate == null) return;

        LocalDateTime reminder = item.dueDate.minusDays(1).atTime(9, 0);
        LocalDateTime now = LocalDateTime.now(ZONE);
        if (!reminder.isAfter(now)) reminder = item.dueDate.atTime(9, 0);
        if (!reminder.isAfter(now)) return;

        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (manager == null) return;
        PendingIntent pending = pendingIntent(context, item.id);
        manager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                reminder.atZone(ZONE).toInstant().toEpochMilli(),
                pending
        );
    }

    public static void cancel(Context context, long id) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (manager != null) manager.cancel(pendingIntent(context, id));
    }

    public static void scheduleAll(Context context) {
        List<StudentPlannerStore.Item> items = StudentPlannerStore.load(
                context.getSharedPreferences(SmsTransactionService.PREFS_NAME, Context.MODE_PRIVATE)
        );
        for (StudentPlannerStore.Item item : items) schedule(context, item);
    }

    private static PendingIntent pendingIntent(Context context, long id) {
        Intent intent = new Intent(context, PlannerReminderReceiver.class)
                .putExtra(EXTRA_ITEM_ID, id);
        int requestCode = (int) (id ^ (id >>> 32));
        return PendingIntent.getBroadcast(context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    static long idFromIntent(Intent intent) {
        return intent == null ? -1L : intent.getLongExtra(EXTRA_ITEM_ID, -1L);
    }
}
