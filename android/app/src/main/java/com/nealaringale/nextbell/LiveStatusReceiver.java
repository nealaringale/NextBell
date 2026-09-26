package com.nealaringale.nextbell;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

public class LiveStatusReceiver extends BroadcastReceiver {
    public static final String ACTION_UPDATE_LIVE = "com.nealaringale.nextbell.UPDATE_LIVE";
    private static final String PREFS_NAME = "nextbell_profile";
    private static final String KEY_ROLL_NUMBER = "roll_number";
    private static final String CHANNEL_ID = "nextbell_live_status";
    private static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");
    private static final int NOTIFICATION_ID = 4201;

    @Override
    public void onReceive(Context context, Intent intent) {
        updateNow(context);
    }

    public static void updateNow(Context context) {
        SharedPreferences prefs =
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            NotificationManager manager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager == null || !manager.areNotificationsEnabled()) return;
        }

        int roll = prefs.getInt(KEY_ROLL_NUMBER, -1);
        if (roll < 1) return;

        NotificationScheduler.ensureLiveChannel(context);

        LocalDate today = LocalDate.now(ZONE);
        LocalTime now = LocalTime.now(ZONE);

        TimetableData.ClassItem current = null;
        TimetableData.ClassItem next = null;
        LocalDate nextDate = null;

        String todayName = dayName(today);
        if (!todayName.isEmpty()) {
            List<TimetableData.ClassItem> entries =
                    TimetableData.forRollAndDay(roll, todayName);

            for (TimetableData.ClassItem item : entries) {
                if (!item.isAcademic()) continue;

                LocalTime start = LocalTime.parse(item.start);
                LocalTime end = LocalTime.parse(item.end);

                if (!now.isBefore(start) && now.isBefore(end)) {
                    current = item;
                    break;
                }

                if (now.isBefore(start) && next == null) {
                    next = item;
                    nextDate = today;
                }
            }
        }

        if (current == null && next == null) {
            for (int offset = 1; offset <= 7; offset++) {
                LocalDate date = today.plusDays(offset);
                String day = dayName(date);
                if (day.isEmpty()) continue;

                List<TimetableData.ClassItem> entries =
                        TimetableData.forRollAndDay(roll, day);

                for (TimetableData.ClassItem item : entries) {
                    if (item.isAcademic()) {
                        next = item;
                        nextDate = date;
                        break;
                    }
                }

                if (next != null) break;
            }
        }

        Intent openIntent = new Intent(context, MainActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(
                context,
                NOTIFICATION_ID,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        android.app.Notification.Builder builder =
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                        ? new android.app.Notification.Builder(context, CHANNEL_ID)
                        : new android.app.Notification.Builder(context);

        long countdownTargetMillis = 0L;

        if (current != null) {
            LocalTime end = LocalTime.parse(current.end);
            countdownTargetMillis = today.atTime(end).atZone(ZONE).toInstant().toEpochMilli();

            builder.setContentTitle("NextBell  ·  HAPPENING NOW")
                    .setContentText(current.subject
                            + location(current)
                            + "  ·  ends " + formatTime(current.end))
                    .setSubText("Class in progress")
                    .setCategory(android.app.Notification.CATEGORY_STATUS);
        } else if (next != null) {
            LocalTime start = LocalTime.parse(next.start);
            countdownTargetMillis =
                    nextDate.atTime(start).atZone(ZONE).toInstant().toEpochMilli();

            String prefix = nextDate.equals(today) ? "Next class" : "Next class · " + dayShort(nextDate);
            builder.setContentTitle("NextBell  ·  " + prefix)
                    .setContentText(next.subject
                            + location(next)
                            + "  ·  " + formatTime(next.start))
                    .setSubText("Your timetable at a glance")
                    .setCategory(android.app.Notification.CATEGORY_STATUS);
        } else {
            builder.setContentTitle("NextBell")
                    .setContentText("No upcoming classes scheduled.")
                    .setSubText("You're all clear.")
                    .setCategory(android.app.Notification.CATEGORY_STATUS);
        }

        builder.setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentIntent(contentIntent)
                .setOngoing(true)
                .setAutoCancel(false)
                .setOnlyAlertOnce(true)
                .setShowWhen(false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && countdownTargetMillis > 0L) {
            builder.setWhen(countdownTargetMillis)
                    .setShowWhen(true)
                    .setUsesChronometer(true)
                    .setChronometerCountDown(true);
        }

        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (manager != null) {
            manager.notify(NOTIFICATION_ID, builder.build());
        }
    }

    private static String location(TimetableData.ClassItem item) {
        StringBuilder result = new StringBuilder();

        String room = roomNumber(item);
        String wing = wing(item);

        if (!room.isEmpty()) result.append("  ·  Room ").append(room);
        if (!wing.isEmpty()) result.append("  ·  ").append(wing);

        return result.toString();
    }

    private static String roomNumber(TimetableData.ClassItem item) {
        if (item.room == null || item.room.isEmpty()) return "";

        java.util.regex.Matcher matcher =
                java.util.regex.Pattern.compile("\\b(\\d{3})\\b").matcher(item.room);
        if (matcher.find()) return matcher.group(1);

        return item.room.startsWith("Library") ? item.room : "";
    }

    private static String wing(TimetableData.ClassItem item) {
        if (item.room == null || item.room.isEmpty()) return "";

        java.util.regex.Matcher matcher =
                java.util.regex.Pattern.compile(
                        "Wing\\s+[A-Z]",
                        java.util.regex.Pattern.CASE_INSENSITIVE
                ).matcher(item.room);

        if (matcher.find()) return matcher.group().replace("wing", "Wing");

        if (item.room.matches("\\d{3}")) return "Wing C";
        return "";
    }

    private static String formatTime(String value) {
        LocalTime time = LocalTime.parse(value);
        int h = time.getHour();
        String suffix = h >= 12 ? "PM" : "AM";
        int hour = h % 12 == 0 ? 12 : h % 12;
        return hour + ":" + String.format("%02d", time.getMinute()) + " " + suffix;
    }

    private static String dayShort(LocalDate date) {
        String day = dayName(date);
        return day.isEmpty() ? "" : day.substring(0, 3);
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
