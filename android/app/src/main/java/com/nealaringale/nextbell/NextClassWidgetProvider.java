package com.nealaringale.nextbell;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

/** A small, refreshed timetable glance card for the Android home screen. */
public final class NextClassWidgetProvider extends AppWidgetProvider {
    private static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] widgetIds) {
        refreshAll(context);
    }

    public static void refreshAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        ComponentName component = new ComponentName(context, NextClassWidgetProvider.class);
        int[] widgetIds = manager.getAppWidgetIds(component);
        for (int widgetId : widgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(),
                    R.layout.widget_next_class);
            updateContent(context, views);
            manager.updateAppWidget(widgetId, views);
        }
    }

    private static void updateContent(Context context, RemoteViews views) {
        android.content.SharedPreferences preferences = context.getSharedPreferences(
                SmsTransactionService.PREFS_NAME, Context.MODE_PRIVATE
        );
        int roll = preferences.getInt("roll_number", -1);
        String name = preferences.getString("name", "").trim();

        Intent open = new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent launch = PendingIntent.getActivity(context, 9230, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_next_class_root, launch);

        if (roll < 1 || name.isEmpty()) {
            views.setTextViewText(R.id.widget_eyebrow, "NEXTBELL · STUDENT DAY");
            views.setTextViewText(R.id.widget_subject, "Set up your timetable");
            views.setTextViewText(R.id.widget_meta, "Tap to add your student profile");
            views.setContentDescription(R.id.widget_next_class_root,
                    "Set up the NextBell timetable widget");
            return;
        }

        TimetableData.ClassItem current = null;
        TimetableData.ClassItem next = null;
        LocalDate nextDate = null;
        LocalDate today = LocalDate.now(ZONE);
        LocalTime now = LocalTime.now(ZONE);
        for (int offset = 0; offset < 8 && next == null; offset++) {
            LocalDate date = today.plusDays(offset);
            String day = date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
            List<TimetableData.ClassItem> entries = TimetableData.forRollAndDay(roll, day);
            for (TimetableData.ClassItem item : entries) {
                if (!item.isAcademic()) continue;
                LocalTime start = LocalTime.parse(item.start);
                LocalTime end = LocalTime.parse(item.end);
                if (offset == 0 && !now.isBefore(start) && now.isBefore(end)) {
                    current = item;
                    nextDate = date;
                    break;
                }
                if (offset > 0 || !start.isBefore(now)) {
                    next = item;
                    nextDate = date;
                    break;
                }
            }
            if (current != null) break;
        }

        TimetableData.ClassItem item = current != null ? current : next;
        if (item == null || nextDate == null) {
            views.setTextViewText(R.id.widget_eyebrow, "NEXTBELL · STUDENT DAY");
            views.setTextViewText(R.id.widget_subject, "No upcoming class");
            views.setTextViewText(R.id.widget_meta, "Your timetable is clear for now");
            views.setContentDescription(R.id.widget_next_class_root, "No upcoming class");
            return;
        }

        String day = nextDate.equals(today) ? "TODAY" : nextDate.getDayOfWeek()
                .getDisplayName(TextStyle.FULL, Locale.ENGLISH).toUpperCase(Locale.ENGLISH);
        String eyebrow = current != null ? "IN CLASS NOW · " + day : "NEXT CLASS · " + day;
        String time = current != null
                ? "Until " + LocalTime.parse(item.end).format(TIME)
                : "Starts " + LocalTime.parse(item.start).format(TIME);
        if (!item.room.trim().isEmpty()) time += "  ·  " + item.room;
        views.setTextViewText(R.id.widget_eyebrow, eyebrow);
        views.setTextViewText(R.id.widget_subject, item.subject);
        views.setTextViewText(R.id.widget_meta, time);
        views.setContentDescription(R.id.widget_next_class_root,
                eyebrow + ". " + item.subject + ". " + time);
    }
}
