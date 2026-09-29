package com.nealaringale.nextbell;

import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class RecurringExpenseStore {
    private static final String KEY = "recurring_expenses_v1";

    public static final class Rule {
        public long id;
        public String title;
        public long amountPaise;
        public int dayOfMonth;
        public String category;
        public String paymentMode;
        public String note;
        public boolean active;
        public String lastGeneratedMonth;

        public Rule(long id, String title, long amountPaise, int dayOfMonth,
                    String category, String paymentMode, String note,
                    boolean active, String lastGeneratedMonth) {
            this.id = id;
            this.title = title == null ? "" : title;
            this.amountPaise = Math.max(0L, amountPaise);
            this.dayOfMonth = Math.max(1, Math.min(28, dayOfMonth));
            this.category = category == null ? "Other" : category;
            this.paymentMode = paymentMode == null ? "UPI" : paymentMode;
            this.note = note == null ? "" : note;
            this.active = active;
            this.lastGeneratedMonth = lastGeneratedMonth == null ? "" : lastGeneratedMonth;
        }
    }

    private RecurringExpenseStore() {}

    public static List<Rule> load(SharedPreferences prefs) {
        List<Rule> result = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(prefs.getString(KEY, "[]"));
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.getJSONObject(i);
                result.add(new Rule(
                        item.optLong("id"),
                        item.optString("title", ""),
                        item.optLong("amountPaise"),
                        item.optInt("dayOfMonth", 1),
                        item.optString("category", "Other"),
                        item.optString("paymentMode", "UPI"),
                        item.optString("note", ""),
                        item.optBoolean("active", true),
                        item.optString("lastGeneratedMonth", "")
                ));
            }
        } catch (Exception ignored) {}
        result.sort(Comparator.comparing(r -> r.dayOfMonth));
        return result;
    }

    public static void save(SharedPreferences prefs, List<Rule> rules) {
        JSONArray array = new JSONArray();
        for (Rule rule : rules) {
            try {
                JSONObject item = new JSONObject();
                item.put("id", rule.id);
                item.put("title", rule.title);
                item.put("amountPaise", rule.amountPaise);
                item.put("dayOfMonth", rule.dayOfMonth);
                item.put("category", rule.category);
                item.put("paymentMode", rule.paymentMode);
                item.put("note", rule.note);
                item.put("active", rule.active);
                item.put("lastGeneratedMonth", rule.lastGeneratedMonth);
                array.put(item);
            } catch (Exception ignored) {}
        }
        prefs.edit().putString(KEY, array.toString()).apply();
    }

    public static void upsert(SharedPreferences prefs, Rule updated) {
        List<Rule> rules = load(prefs);
        boolean replaced = false;
        for (int i = 0; i < rules.size(); i++) {
            if (rules.get(i).id == updated.id) {
                rules.set(i, updated);
                replaced = true;
                break;
            }
        }
        if (!replaced) rules.add(updated);
        save(prefs, rules);
    }

    public static void delete(SharedPreferences prefs, long id) {
        List<Rule> rules = load(prefs);
        rules.removeIf(rule -> rule.id == id);
        save(prefs, rules);
    }

    public static int generateDue(SharedPreferences prefs, LocalDate today) {
        int generated = 0;
        List<Rule> rules = load(prefs);
        String month = monthKey(today);
        boolean changed = false;

        for (Rule rule : rules) {
            if (!rule.active || rule.amountPaise <= 0L || today.getDayOfMonth() < rule.dayOfMonth) {
                continue;
            }
            if (month.equals(rule.lastGeneratedMonth)) continue;

            LocalDate dueDate = today.withDayOfMonth(rule.dayOfMonth);
            long id = Math.abs((rule.id * 31L) ^ dueDate.toEpochDay() ^ rule.amountPaise);
            if (id == 0L) id = Math.max(1L, System.nanoTime());

            ExpenseStore.upsert(
                    prefs,
                    new ExpenseStore.Expense(
                            id,
                            rule.amountPaise,
                            dueDate,
                            rule.category,
                            rule.note.isEmpty() ? rule.title + "  •  Recurring" : rule.note + "  •  Recurring",
                            rule.paymentMode,
                            rule.title,
                            "RECURRING",
                            1.0f,
                            "recurring:" + rule.id + ":" + month
                    )
            );

            rule.lastGeneratedMonth = month;
            generated++;
            changed = true;
        }

        if (changed) save(prefs, rules);
        return generated;
    }

    public static int dueCount(SharedPreferences prefs, LocalDate today) {
        int count = 0;
        String month = monthKey(today);
        for (Rule rule : load(prefs)) {
            if (rule.active && rule.amountPaise > 0L
                    && today.getDayOfMonth() >= rule.dayOfMonth
                    && !month.equals(rule.lastGeneratedMonth)) {
                count++;
            }
        }
        return count;
    }

    private static String monthKey(LocalDate date) {
        return date.toString().substring(0, 7);
    }
}
