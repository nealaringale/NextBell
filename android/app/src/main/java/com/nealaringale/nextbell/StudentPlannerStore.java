package com.nealaringale.nextbell;

import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** On-device assignments, exams, and tasks. No account or network service is used. */
public final class StudentPlannerStore {
    private static final String KEY = "student_planner_items_v1";

    public static final String ASSIGNMENT = "ASSIGNMENT";
    public static final String EXAM = "EXAM";
    public static final String TASK = "TASK";

    public static final class Item {
        public final long id;
        public final String type;
        public final String title;
        public final String course;
        public final String details;
        public final LocalDate dueDate;
        public final boolean completed;
        public final String priority;

        public Item(long id, String type, String title, String course,
                    String details, LocalDate dueDate, boolean completed) {
            this(id, type, title, course, details, dueDate, completed, "MEDIUM");
        }

        public Item(long id, String type, String title, String course,
                    String details, LocalDate dueDate, boolean completed, String priority) {
            this.id = id;
            this.type = normalizeType(type);
            this.title = clean(title, 120);
            this.course = clean(course, 100);
            this.details = clean(details, 500);
            this.dueDate = dueDate;
            this.completed = completed;
            this.priority = normalizePriority(priority);
        }
    }

    private StudentPlannerStore() {}

    public static List<Item> load(SharedPreferences preferences) {
        List<Item> items = new ArrayList<>();
        try {
            JSONArray values = new JSONArray(preferences.getString(KEY, "[]"));
            for (int i = 0; i < values.length(); i++) {
                try {
                    JSONObject value = values.getJSONObject(i);
                    String title = clean(value.optString("title", ""), 120);
                    if (title.isEmpty()) continue;
                    String rawDate = value.optString("dueDate", "");
                    LocalDate dueDate = rawDate.isEmpty() ? null : LocalDate.parse(rawDate);
                    items.add(new Item(
                            value.optLong("id"), value.optString("type", TASK), title,
                            value.optString("course", ""), value.optString("details", ""),
                            dueDate, value.optBoolean("completed", false),
                            value.optString("priority", "MEDIUM")
                    ));
                } catch (Exception ignored) {
                    // Keep every valid planner entry if one row is damaged.
                }
            }
        } catch (Exception ignored) {}
        sort(items);
        return items;
    }

    public static void upsert(SharedPreferences preferences, Item updated) {
        if (updated == null || updated.title.isEmpty()) return;
        List<Item> items = load(preferences);
        boolean replaced = false;
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).id == updated.id) {
                items.set(i, updated);
                replaced = true;
                break;
            }
        }
        if (!replaced) items.add(updated);
        sort(items);
        save(preferences, items);
    }

    public static void delete(SharedPreferences preferences, long id) {
        List<Item> items = load(preferences);
        items.removeIf(item -> item.id == id);
        save(preferences, items);
    }

    public static Item find(SharedPreferences preferences, long id) {
        for (Item item : load(preferences)) if (item.id == id) return item;
        return null;
    }

    public static long nextId(SharedPreferences preferences) {
        long next = System.currentTimeMillis();
        for (Item item : load(preferences)) {
            if (item.id >= next && item.id < Long.MAX_VALUE) next = item.id + 1L;
        }
        return next;
    }

    public static void sort(List<Item> items) {
        items.sort(Comparator
                .comparing((Item item) -> item.completed)
                .thenComparing(item -> item.dueDate == null ? LocalDate.MAX : item.dueDate)
                .thenComparing(item -> item.title.toLowerCase(java.util.Locale.ENGLISH)));
    }

    private static void save(SharedPreferences preferences, List<Item> items) {
        JSONArray values = new JSONArray();
        for (Item item : items) {
            try {
                JSONObject value = new JSONObject();
                value.put("id", item.id);
                value.put("type", item.type);
                value.put("title", item.title);
                value.put("course", item.course);
                value.put("details", item.details);
                value.put("dueDate", item.dueDate == null ? "" : item.dueDate.toString());
                value.put("completed", item.completed);
                value.put("priority", item.priority);
                values.put(value);
            } catch (Exception ignored) {}
        }
        preferences.edit().putString(KEY, values.toString()).apply();
    }

    private static String normalizeType(String type) {
        if (EXAM.equals(type) || ASSIGNMENT.equals(type)) return type;
        return TASK;
    }

    private static String normalizePriority(String priority) {
        if ("HIGH".equals(priority) || "LOW".equals(priority)) return priority;
        return "MEDIUM";
    }

    private static String clean(String value, int maxLength) {
        if (value == null) return "";
        String clean = value.trim().replaceAll("[\\p{Cntrl}&&[^\\n\\t]]", "");
        return clean.length() > maxLength ? clean.substring(0, maxLength) : clean;
    }
}
