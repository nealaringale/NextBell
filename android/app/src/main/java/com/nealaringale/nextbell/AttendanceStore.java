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
        public final String collegeId;

        Session(String key, String subject, String start, String end,
                LocalDate date, String status) {
            this(key, subject, start, end, date, status, CollegeCatalog.NMIET);
        }

        Session(String key, String subject, String start, String end,
                LocalDate date, String status, String collegeId) {
            this.key = key;
            this.subject = subject;
            this.start = start;
            this.end = end;
            this.date = date;
            this.status = status;
            this.collegeId = CollegeCatalog.normalize(collegeId);
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
                    String collegeId = CollegeCatalog.normalize(
                            value.optString("collegeId", CollegeCatalog.NMIET));
                    sessions.add(new Session(
                            value.optString("key", key(date, subject,
                                    value.optString("start", ""), collegeId)),
                            subject,
                            value.optString("start", ""),
                            value.optString("end", ""),
                            date,
                            status,
                            collegeId
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
        mark(preferences, date, item, status, CollegeCatalog.NMIET);
    }

    public static void mark(SharedPreferences preferences, LocalDate date,
                            TimetableData.ClassItem item, String status, String collegeId) {
        if (item == null || !item.isAcademic() || date == null) return;
        if (!"PRESENT".equals(status) && !"ABSENT".equals(status)) return;
        String normalizedCollege = CollegeCatalog.normalize(collegeId);
        String meetingKey = key(date, item.subject, item.start, normalizedCollege);
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
            record.put("collegeId", normalizedCollege);
            updated.put(record);
            preferences.edit().putString(KEY, updated.toString()).apply();
        } catch (Exception ignored) {}
    }

    public static void clear(SharedPreferences preferences, LocalDate date,
                             TimetableData.ClassItem item) {
        clear(preferences, date, item, CollegeCatalog.NMIET);
    }

    public static void clear(SharedPreferences preferences, LocalDate date,
                             TimetableData.ClassItem item, String collegeId) {
        if (item == null || date == null) return;
        String meetingKey = key(date, item.subject, item.start, collegeId);
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
        return statusFor(sessions, date, item, CollegeCatalog.NMIET);
    }

    public static String statusFor(List<Session> sessions, LocalDate date,
                                    TimetableData.ClassItem item, String collegeId) {
        if (item == null || date == null) return "";
        String normalizedCollege = CollegeCatalog.normalize(collegeId);
        String meetingKey = key(date, item.subject, item.start, normalizedCollege);
        for (Session session : sessions) {
            if (normalizedCollege.equals(session.collegeId) && meetingKey.equals(session.key)) {
                return session.status;
            }
        }
        return "";
    }

    public static Stats stats(List<Session> sessions, String subject, int targetPercent) {
        return stats(sessions, subject, targetPercent, CollegeCatalog.NMIET);
    }

    public static Stats stats(List<Session> sessions, String subject, int targetPercent,
                              String collegeId) {
        int attended = 0;
        int held = 0;
        String normalizedCollege = CollegeCatalog.normalize(collegeId);
        for (Session session : sessions) {
            if (!session.subject.equals(subject) || !normalizedCollege.equals(session.collegeId)) continue;
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

    private static String key(LocalDate date, String subject, String start, String collegeId) {
        String base = date + "|" + subject.trim().toLowerCase(java.util.Locale.ENGLISH)
                + "|" + (start == null ? "" : start.trim());
        String normalizedCollege = CollegeCatalog.normalize(collegeId);
        // Preserve legacy NMIET keys; other colleges use their own namespace.
        return CollegeCatalog.NMIET.equals(normalizedCollege)
                ? base : normalizedCollege + "|" + base;
    }
}
