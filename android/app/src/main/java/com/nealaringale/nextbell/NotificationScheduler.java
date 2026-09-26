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
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class NotificationScheduler {
    public static final String CHANNEL_ID = "nextbell_class_reminders";
    private static final String PREFS_NAME = "nextbell_profile";
    private static final String KEY_ROLL_NUMBER = "roll_number";
    private static final String KEY_SCHEDULED_ALARMS = "scheduled_alarm_ids";
    private static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");
    private static final int REMINDER_MINUTES = 15;

    private NotificationScheduler() {}

    public static void scheduleUpcoming(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            NotificationManager manager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager == null ||
                    manager.areNotificationsEnabled() == false) {
                return;
            }
        }

        ensureChannel(context);

        SharedPreferences preferences =
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int roll = preferences.getInt(KEY_ROLL_NUMBER, -1);
        if (roll < 1) return;

        cancelScheduled(context);

        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Set<String> ids = new HashSet<>();
        LocalDate today = LocalDate.now(ZONE);

        for (int offset = 0; offset < 8; offset++) {
            LocalDate date = today.plusDays(offset);
            String day = dayName(date);
            if (day.isEmpty()) continue;

            List<TimetableData.ClassItem> classes =
                    TimetableData.forRollAndDay(roll, day);

            for (TimetableData.ClassItem item : classes) {
                if (!item.isAcademic()) continue;

                LocalTime start = LocalTime.parse(item.start);
                LocalDateTime reminderDateTime =
                        LocalDateTime.of(date, start).minusMinutes(REMINDER_MINUTES);

                long triggerMillis = reminderDateTime
                        .atZone(ZONE)
                        .toInstant()
                        .toEpochMilli();

                if (triggerMillis <= System.currentTimeMillis()) continue;

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

                ids.add(id);
            }
        }

        preferences.edit().putStringSet(KEY_SCHEDULED_ALARMS, ids).apply();
    }

    public static void cancelScheduled(Context context) {
        SharedPreferences preferences =
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        Set<String> ids = preferences.getStringSet(
                KEY_SCHEDULED_ALARMS,
                new HashSet<>()
        );

        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        if (alarmManager != null) {
            for (String id : ids) {
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
        }

        preferences.edit().remove(KEY_SCHEDULED_ALARMS).apply();
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
