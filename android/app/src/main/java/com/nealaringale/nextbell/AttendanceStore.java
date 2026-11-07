package com.nealaringale.nextbell;

import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Local attendance ledger keyed by the actual class meeting, so double taps are harmless. */
public final class AttendanceStore {
    private static final String KEY = "attendance_sessions_v1";
    private static final String KEY_TARGET = "attendance_target_percent";

    public static final class Session {
        public final String key;
        public final String subject;
        public final String start;
        public final String end;
        public final LocalDate date;
        public final String status;

        Session(String key, String subject, String start, String end,
                LocalDate date, String status) {
            this.key = key;
            this.subject = subject;
            this.start = start;
            this.end = end;
            this.date = date;
            this.status = status;
        }
    }

    public static final class Stats {
        public final int attended;
        public final int held;
        public final int targetPercent;

        public Stats(int attended, int held, int targetPercent) {
            this.attended = Math.max(0, attended);
            this.held = Math.max(0, held);
            this.targetPercent = clampTarget(targetPercent);
        }

        public int percentage() {
            return held == 0 ? 0 : (int) Math.floor(attended * 100.0 / held + 0.5);
        }

        /** Number of future absences that still leave attendance at or above the target. */
        public int safeAbsences() {
            if (held == 0 || targetPercent <= 0) return 0;
            long maximumHeld = (long) attended * 100L / targetPercent;
            return (int) Math.max(0L, Math.min(Integer.MAX_VALUE, maximumHeld - held));
        }

        /** Consecutive attended sessions needed to reach the target when currently below it. */
        public int classesToTarget() {
            if (held == 0 || percentage() >= targetPercent) return 0;
            double required = (targetPercent * (double) held - 100.0 * attended)
                    / (100.0 - targetPercent);
            return Math.max(0, (int) Math.ceil(required));
        }
    }

    private AttendanceStore() {}

    public static List<Session> load(SharedPreferences preferences) {
        List<Session> sessions = new ArrayList<>();
        try {
            JSONArray values = new JSONArray(preferences.getString(KEY, "[]"));
            for (int i = 0; i < values.length(); i++) {
                try {
                    JSONObject value = values.getJSONObject(i);
                    String status = value.optString("status", "");
                    if (!"PRESENT".equals(status) && !"ABSENT".equals(status)) continue;
                    String subject = value.optString("subject", "").trim();
                    LocalDate date = LocalDate.parse(value.optString("date", ""));
                    if (subject.isEmpty()) continue;
                    sessions.add(new Session(
                            value.optString("key", key(date, subject, value.optString("start", ""))),
                            subject,
                            value.optString("start", ""),
                            value.optString("end", ""),
                            date,
                            status
                    ));
                } catch (Exception ignored) {
                    // A damaged meeting does not hide the other attendance records.
                }
            }
        } catch (Exception ignored) {}
        sessions.sort(Comparator.comparing((Session value) -> value.date).reversed());
        return sessions;
    }

    public static void mark(SharedPreferences preferences, LocalDate date,
                            TimetableData.ClassItem item, String status) {
        if (item == null || !item.isAcademic() || date == null) return;
        if (!"PRESENT".equals(status) && !"ABSENT".equals(status)) return;
        String meetingKey = key(date, item.subject, item.start);
        JSONArray values = safeArray(preferences);
        JSONArray updated = new JSONArray();
        for (int i = 0; i < values.length(); i++) {
            JSONObject value = values.optJSONObject(i);
            if (value != null && meetingKey.equals(value.optString("key"))) continue;
            if (value != null) updated.put(value);
        }
        JSONObject record = new JSONObject();
        try {
            record.put("key", meetingKey);
            record.put("subject", item.subject);
            record.put("start", item.start);
            record.put("end", item.end);
            record.put("date", date.toString());
            record.put("status", status);
            updated.put(record);
            preferences.edit().putString(KEY, updated.toString()).apply();
        } catch (Exception ignored) {}
    }

    public static void clear(SharedPreferences preferences, LocalDate date,
                             TimetableData.ClassItem item) {
        if (item == null || date == null) return;
        String meetingKey = key(date, item.subject, item.start);
        JSONArray values = safeArray(preferences);
        JSONArray updated = new JSONArray();
        for (int i = 0; i < values.length(); i++) {
            JSONObject value = values.optJSONObject(i);
            if (value != null && !meetingKey.equals(value.optString("key"))) updated.put(value);
        }
        preferences.edit().putString(KEY, updated.toString()).apply();
    }

    public static String statusFor(List<Session> sessions, LocalDate date,
                                    TimetableData.ClassItem item) {
        if (item == null || date == null) return "";
        String meetingKey = key(date, item.subject, item.start);
        for (Session session : sessions) if (meetingKey.equals(session.key)) return session.status;
        return "";
    }

    public static Stats stats(List<Session> sessions, String subject, int targetPercent) {
        int attended = 0;
        int held = 0;
        for (Session session : sessions) {
            if (!session.subject.equals(subject)) continue;
            held++;
            if ("PRESENT".equals(session.status)) attended++;
        }
        return new Stats(attended, held, targetPercent);
    }

    public static int targetPercent(SharedPreferences preferences) {
        return clampTarget(preferences.getInt(KEY_TARGET, 75));
    }

    public static void setTargetPercent(SharedPreferences preferences, int target) {
        preferences.edit().putInt(KEY_TARGET, clampTarget(target)).apply();
    }

    public static int clampTarget(int target) {
        return Math.max(50, Math.min(95, target));
    }

    private static JSONArray safeArray(SharedPreferences preferences) {
        try { return new JSONArray(preferences.getString(KEY, "[]")); }
        catch (Exception ignored) { return new JSONArray(); }
    }

    private static String key(LocalDate date, String subject, String start) {
        return date + "|" + subject.trim().toLowerCase(java.util.Locale.ENGLISH)
                + "|" + (start == null ? "" : start.trim());
    }
}
