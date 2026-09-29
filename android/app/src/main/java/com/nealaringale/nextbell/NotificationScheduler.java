package com.nealaringale.nextbell;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class NotificationScheduler {
    public static final String CHANNEL_ID = "nextbell_class_reminders";
    public static final String LIVE_CHANNEL_ID = "nextbell_live_status";

    private static final String PREFS_NAME = "nextbell_profile";
    private static final String KEY_ROLL_NUMBER = "roll_number";
    private static final String KEY_SCHEDULED_ALARMS = "scheduled_alarm_ids";
    private static final String KEY_LIVE_ALARMS = "live_status_alarm_ids";

    private static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");
    private static final int REMINDER_MINUTES = 15;

    private NotificationScheduler() {}

    public static void scheduleUpcoming(Context context) {
        SharedPreferences preferences =
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Clear old alarms before rebuilding them so changing roll/profile
        // never leaves reminders or live-status alarms from the old schedule.
        cancelScheduled(context);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            NotificationManager manager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager == null || !manager.areNotificationsEnabled()) {
                return;
            }
        }

        int roll = preferences.getInt(KEY_ROLL_NUMBER, -1);
        if (roll < 1) return;

        ensureChannel(context);
        ensureLiveChannel(context);

        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        scheduleDailyMoneyAlert(context, alarmManager);

        Set<String> reminderIds = new HashSet<>();
        Set<String> liveIds = new HashSet<>();
        LocalDate today = LocalDate.now(ZONE);
        long nowMillis = System.currentTimeMillis();

        for (int offset = 0; offset < 8; offset++) {
            LocalDate date = today.plusDays(offset);
            String day = dayName(date);
            if (day.isEmpty()) continue;

            // Refresh the persistent "next class" notification shortly after
            // midnight so a new day never starts with stale information.
            LocalDateTime midnightRefresh = date.atTime(0, 1);
            scheduleLiveAlarm(
                    context,
                    alarmManager,
                    midnightRefresh.atZone(ZONE).toInstant().toEpochMilli(),
                    date + "_midnight",
                    liveIds,
                    nowMillis
            );

            List<TimetableData.ClassItem> classes =
                    TimetableData.forRollAndDay(roll, day);

            for (TimetableData.ClassItem item : classes) {
                if (!item.isAcademic()) continue;

                LocalTime start = LocalTime.parse(item.start);
                LocalTime end = LocalTime.parse(item.end);

                LocalDateTime reminderDateTime =
                        LocalDateTime.of(date, start).minusMinutes(REMINDER_MINUTES);

                long triggerMillis = reminderDateTime
                        .atZone(ZONE)
                        .toInstant()
                        .toEpochMilli();

                if (triggerMillis > nowMillis) {
                    String id = date + "_" + item.id;
                    int requestCode = stableRequestCode(id);

                    Intent intent = new Intent(context, ClassReminderReceiver.class);
                    intent.putExtra(ClassReminderReceiver.EXTRA_SUBJECT, item.subject);
                    intent.putExtra(ClassReminderReceiver.EXTRA_START, item.start);
                    intent.putExtra(ClassReminderReceiver.EXTRA_ROOM, item.room);
                    intent.putExtra(ClassReminderReceiver.EXTRA_BATCH, item.batch);
                    intent.putExtra(ClassReminderReceiver.EXTRA_DAY, day);

                    PendingIntent pendingIntent = PendingIntent.getBroadcast(
                            context,
                            requestCode,
                            intent,
                            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                    );

                    alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerMillis,
                            pendingIntent
                    );

                    reminderIds.add(id);
                }

                // Refresh when a class starts and ends, keeping the persistent
                // notification aligned with the live timetable.
                long startMillis = LocalDateTime.of(date, start)
                        .atZone(ZONE)
                        .toInstant()
                        .toEpochMilli();

                long endMillis = LocalDateTime.of(date, end)
                        .atZone(ZONE)
                        .toInstant()
                        .toEpochMilli();

                scheduleLiveAlarm(
                        context,
                        alarmManager,
                        startMillis,
                        date + "_" + item.id + "_start",
                        liveIds,
                        nowMillis
                );
                scheduleLiveAlarm(
                        context,
                        alarmManager,
                        endMillis,
                        date + "_" + item.id + "_end",
                        liveIds,
                        nowMillis
                );
            }
        }

        preferences.edit()
                .putStringSet(KEY_SCHEDULED_ALARMS, reminderIds)
                .putStringSet(KEY_LIVE_ALARMS, liveIds)
                .apply();

        // Update immediately whenever the app schedules/reschedules the timetable.
        LiveStatusReceiver.updateNow(context);
    }

    private static void scheduleLiveAlarm(
            Context context,
            AlarmManager alarmManager,
            long triggerMillis,
            String id,
            Set<String> ids,
            long nowMillis
    ) {
        if (triggerMillis <= nowMillis) return;

        Intent intent = new Intent(context, LiveStatusReceiver.class);
        intent.setAction(LiveStatusReceiver.ACTION_UPDATE_LIVE);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                stableRequestCode("live_" + id),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerMillis,
                pendingIntent
        );

        ids.add(id);
    }

    public static void cancelScheduled(Context context) {
        SharedPreferences preferences =
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        Set<String> reminderIds = preferences.getStringSet(
                KEY_SCHEDULED_ALARMS,
                new HashSet<>()
        );
        Set<String> liveIds = preferences.getStringSet(
                KEY_LIVE_ALARMS,
                new HashSet<>()
        );

        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        if (alarmManager != null) {
            for (String id : reminderIds) {
                Intent intent = new Intent(context, ClassReminderReceiver.class);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                        context,
                        stableRequestCode(id),
                        intent,
                        PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
                );

                if (pendingIntent != null) {
                    alarmManager.cancel(pendingIntent);
                    pendingIntent.cancel();
                }
            }

            for (String id : liveIds) {
                Intent intent = new Intent(context, LiveStatusReceiver.class);
                intent.setAction(LiveStatusReceiver.ACTION_UPDATE_LIVE);

                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                        context,
                        stableRequestCode("live_" + id),
                        intent,
                        PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
                );

                if (pendingIntent != null) {
                    alarmManager.cancel(pendingIntent);
                    pendingIntent.cancel();
                }
            }
        }

        preferences.edit()
                .remove(KEY_SCHEDULED_ALARMS)
                .remove(KEY_LIVE_ALARMS)
                .apply();
    }

    public static void scheduleDailyMoneyAlert(Context context, AlarmManager alarmManager) {
        if (alarmManager == null) return;

        SharedPreferences prefs =
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        Intent intent = new Intent(context, ExpenseDailyAlertReceiver.class);
        PendingIntent pending = PendingIntent.getBroadcast(
                context,
                7717,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (!ExpenseStore.dailyAlertsEnabled(prefs)
                || ExpenseStore.dailyAlertThresholdPaise(prefs) <= 0L) {
            alarmManager.cancel(pending);
            pending.cancel();
            return;
        }

        LocalDate today = LocalDate.now(ZONE);
        LocalDateTime target = today.atTime(20, 0);
        long trigger = target.atZone(ZONE).toInstant().toEpochMilli();
        if (trigger <= System.currentTimeMillis()) {
            trigger = today.plusDays(1).atTime(20, 0)
                    .atZone(ZONE).toInstant().toEpochMilli();
        }

        alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                trigger,
                pending
        );
    }

    public static void cancelDailyMoneyAlert(Context context) {
        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, ExpenseDailyAlertReceiver.class);
        PendingIntent pending = PendingIntent.getBroadcast(
                context,
                7717,
                intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
        );
        if (pending != null) {
            alarmManager.cancel(pending);
            pending.cancel();
        }
    }

    public static void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Class reminders",
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription("Reminders 15 minutes before your NextBell classes.");
        manager.createNotificationChannel(channel);
    }

    public static void ensureLiveChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;

        NotificationChannel channel = new NotificationChannel(
                LIVE_CHANNEL_ID,
                "Next class status",
                NotificationManager.IMPORTANCE_LOW
        );
        channel.setDescription("A quiet, continuously updated notification for your next class.");
        channel.setShowBadge(false);
        manager.createNotificationChannel(channel);
    }

    private static int stableRequestCode(String value) {
        return value.hashCode() & 0x7fffffff;
    }

    private static String dayName(LocalDate date) {
        switch (date.getDayOfWeek()) {
            case MONDAY: return "Monday";
            case TUESDAY: return "Tuesday";
            case WEDNESDAY: return "Wednesday";
            case THURSDAY: return "Thursday";
            case FRIDAY: return "Friday";
            default: return "";
        }
    }
}
