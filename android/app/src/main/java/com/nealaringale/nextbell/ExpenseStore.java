package com.nealaringale.nextbell;

import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ExpenseStore {
    private static final String KEY_EXPENSES = "expenses_v1";
    private static final String KEY_BUDGET_PREFIX = "expense_budget_";
    private static final String KEY_DAILY_LIMIT = "expense_daily_limit";
    private static final String KEY_ALERT_THRESHOLD = "expense_alert_threshold";
    private static final String KEY_ALERT_ENABLED = "expense_alert_enabled";
    private static final String KEY_ALERT_LAST_NOTIFIED = "expense_alert_last_notified";

    public static final String[] CATEGORIES = {
            "Food & Drinks",
            "Travel",
            "College",
            "Home & Bills",
            "Mobile",
            "Personal",
            "Fun & Social",
            "Shopping",
            "Health",
            "Other"
    };

    public static final String[] PAYMENT_MODES = {
            "UPI", "Cash", "Card", "Bank Transfer", "Other"
    };

    public static final class Expense {
        public long id;
        public long amountPaise;
        public LocalDate date;
        public String category;
        public String note;
        public String paymentMode;
        public String merchant;
        public String source;
        public float confidence;
        public String fingerprint;

        public Expense(long id, long amountPaise, LocalDate date,
                       String category, String note, String paymentMode) {
            this(id, amountPaise, date, category, note, paymentMode,
                    "", "MANUAL", 1.0f, "");
        }

        public Expense(long id, long amountPaise, LocalDate date,
                       String category, String note, String paymentMode,
                       String merchant, String source,
                       float confidence, String fingerprint) {
            this.id = id;
            this.amountPaise = amountPaise;
            this.date = date;
            this.category = category;
            this.note = note;
            this.paymentMode = paymentMode;
            this.merchant = merchant == null ? "" : merchant;
            this.source = source == null ? "MANUAL" : source;
            this.confidence = confidence;
            this.fingerprint = fingerprint == null ? "" : fingerprint;
        }
    }

    private ExpenseStore() {}

    public static List<Expense> load(SharedPreferences prefs) {
        List<Expense> result = new ArrayList<>();
        String raw = prefs.getString(KEY_EXPENSES, "[]");

        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.getJSONObject(i);
                result.add(new Expense(
                        item.optLong("id"),
                        item.optLong("amountPaise"),
                        LocalDate.parse(item.optString("date")),
                        normalizeCategory(item.optString("category", "Other")),
                        item.optString("note", ""),
                        item.optString("paymentMode", "UPI"),
                        item.optString("merchant", ""),
                        item.optString("source", "MANUAL"),
                        (float) item.optDouble("confidence", 1.0),
                        item.optString("fingerprint", "")
                ));
            }
        } catch (Exception ignored) {
            // Corrupt data is treated as an empty local expense list.
        }

        result.sort(Comparator.comparing((Expense e) -> e.date).reversed()
                .thenComparingLong(e -> e.id).reversed());
        return result;
    }

    public static void save(SharedPreferences prefs, List<Expense> expenses) {
        JSONArray array = new JSONArray();

        for (Expense expense : expenses) {
            JSONObject item = new JSONObject();
            try {
                item.put("id", expense.id);
                item.put("amountPaise", expense.amountPaise);
                item.put("date", expense.date.toString());
                item.put("category", expense.category);
                item.put("note", expense.note);
                item.put("paymentMode", expense.paymentMode);
                item.put("merchant", expense.merchant);
                item.put("source", expense.source);
                item.put("confidence", expense.confidence);
                item.put("fingerprint", expense.fingerprint);
                array.put(item);
            } catch (Exception ignored) {
                // Skip only an invalid object; other expenses remain persisted.
            }
        }

        prefs.edit().putString(KEY_EXPENSES, array.toString()).apply();
    }

    public static void upsert(SharedPreferences prefs, Expense updated) {
        List<Expense> expenses = load(prefs);
        boolean replaced = false;

        for (int i = 0; i < expenses.size(); i++) {
            if (expenses.get(i).id == updated.id) {
                expenses.set(i, updated);
                replaced = true;
                break;
            }
        }

        if (!replaced) expenses.add(updated);
        save(prefs, expenses);
    }

    public static void delete(SharedPreferences prefs, long id) {
        List<Expense> expenses = load(prefs);
        expenses.removeIf(expense -> expense.id == id);
        save(prefs, expenses);
    }
    public static boolean hasFingerprint(SharedPreferences prefs, String fingerprint) {
        if (fingerprint == null || fingerprint.isEmpty()) return false;
        for (Expense expense : load(prefs)) {
            if (fingerprint.equals(expense.fingerprint)) return true;
        }
        return false;
    }

    public static int deleteByFingerprint(SharedPreferences prefs, String fingerprint) {
        if (fingerprint == null || fingerprint.isEmpty()) return 0;
        List<Expense> expenses = load(prefs);
        int before = expenses.size();
        expenses.removeIf(expense -> fingerprint.equals(expense.fingerprint));
        if (expenses.size() != before) save(prefs, expenses);
        return before - expenses.size();
    }

    public static void learnMerchantCategory(SharedPreferences prefs,
                                              String merchant, String category) {
        if (merchant == null || merchant.trim().isEmpty() || !isKnownCategory(category)) return;

        String key = merchantKey(merchant);
        try {
            org.json.JSONObject aliases = new org.json.JSONObject(
                    prefs.getString("expense_merchant_categories", "{}")
            );
            aliases.put(key, category);
            prefs.edit().putString("expense_merchant_categories", aliases.toString()).apply();
        } catch (Exception ignored) {
        }
    }

    public static String learnedMerchantCategory(SharedPreferences prefs, String merchant) {
        if (merchant == null || merchant.trim().isEmpty()) return "";
        try {
            org.json.JSONObject aliases = new org.json.JSONObject(
                    prefs.getString("expense_merchant_categories", "{}")
            );
            return aliases.optString(merchantKey(merchant), "");
        } catch (Exception ignored) {
            return "";
        }
    }

    private static String merchantKey(String merchant) {
        return merchant.trim().toLowerCase(java.util.Locale.ENGLISH)
                .replaceAll("\\s+", " ")
                .replaceAll("[^a-z0-9 ]", "");
    }


    public static List<Expense> forMonth(SharedPreferences prefs, LocalDate monthAnchor) {
        List<Expense> result = new ArrayList<>();
        LocalDate first = monthAnchor.withDayOfMonth(1);
        LocalDate last = first.plusMonths(1);

        for (Expense expense : load(prefs)) {
            if (!expense.date.isBefore(first) && expense.date.isBefore(last)) {
                result.add(expense);
            }
        }

        result.sort(Comparator.comparing((Expense e) -> e.date).reversed()
                .thenComparingLong(e -> e.id).reversed());
        return result;
    }

    public static long totalPaise(List<Expense> expenses) {
        long total = 0L;
        for (Expense expense : expenses) {
            if (Long.MAX_VALUE - total < expense.amountPaise) return Long.MAX_VALUE;
            total += expense.amountPaise;
        }
        return total;
    }

    public static Map<String, Long> categoryTotals(List<Expense> expenses) {
        Map<String, Long> totals = new HashMap<>();
        for (String category : CATEGORIES) totals.put(category, 0L);

        for (Expense expense : expenses) {
            long old = totals.containsKey(expense.category) ? totals.get(expense.category) : 0L;
            long next = old + expense.amountPaise;
            totals.put(expense.category, next);
        }
        return totals;
    }

    public static long budgetPaise(SharedPreferences prefs, LocalDate monthAnchor) {
        return prefs.getLong(KEY_BUDGET_PREFIX + monthKey(monthAnchor), 0L);
    }

    public static void setBudgetPaise(SharedPreferences prefs, LocalDate monthAnchor, long paise) {
        prefs.edit()
                .putLong(KEY_BUDGET_PREFIX + monthKey(monthAnchor), Math.max(0L, paise))
                .apply();
    }

    private static String monthKey(LocalDate monthAnchor) {
        return monthAnchor.withDayOfMonth(1).toString().substring(0, 7);
    }

    private static String normalizeCategory(String value) {
        if (value == null) return "Other";
        switch (value) {
            case "Food": return "Food & Drinks";
            case "College & Study": return "College";
            case "Bills": return "Home & Bills";
            case "Mobile & Data": return "Mobile";
            case "Personal Care": return "Personal";
            case "Entertainment": return "Fun & Social";
            default: return isKnownCategory(value) ? value : "Other";
        }
    }

    private static boolean isKnownCategory(String value) {
        for (String category : CATEGORIES) {
            if (category.equals(value)) return true;
        }
        return false;
    }

    public static long totalForDate(SharedPreferences prefs, LocalDate date) {
        long total = 0L;
        for (Expense expense : load(prefs)) {
            if (date.equals(expense.date)) {
                if (Long.MAX_VALUE - total < expense.amountPaise) return Long.MAX_VALUE;
                total += expense.amountPaise;
            }
        }
        return total;
    }

    public static long totalBetween(SharedPreferences prefs, LocalDate startInclusive, LocalDate endExclusive) {
        long total = 0L;
        for (Expense expense : load(prefs)) {
            if (!expense.date.isBefore(startInclusive) && expense.date.isBefore(endExclusive)) {
                if (Long.MAX_VALUE - total < expense.amountPaise) return Long.MAX_VALUE;
                total += expense.amountPaise;
            }
        }
        return total;
    }

    public static long dailyLimitPaise(SharedPreferences prefs) {
        return prefs.getLong(KEY_DAILY_LIMIT, 0L);
    }

    public static void setDailyLimitPaise(SharedPreferences prefs, long paise) {
        prefs.edit().putLong(KEY_DAILY_LIMIT, Math.max(0L, paise)).apply();
    }

    public static long dailyAlertThresholdPaise(SharedPreferences prefs) {
        return prefs.getLong(KEY_ALERT_THRESHOLD, 5000L);
    }

    public static void setDailyAlertThresholdPaise(SharedPreferences prefs, long paise) {
        prefs.edit().putLong(KEY_ALERT_THRESHOLD, Math.max(0L, paise)).apply();
    }

    public static boolean dailyAlertsEnabled(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_ALERT_ENABLED, true);
    }

    public static void setDailyAlertsEnabled(SharedPreferences prefs, boolean enabled) {
        prefs.edit().putBoolean(KEY_ALERT_ENABLED, enabled).apply();
    }

    public static String lastAlertNotifiedDate(SharedPreferences prefs) {
        return prefs.getString(KEY_ALERT_LAST_NOTIFIED, "");
    }

    public static void setLastAlertNotifiedDate(SharedPreferences prefs, String date) {
        prefs.edit().putString(KEY_ALERT_LAST_NOTIFIED, date == null ? "" : date).apply();
    }

    public static int spendingStreakDays(SharedPreferences prefs, LocalDate endDate) {
        long limit = dailyLimitPaise(prefs);
        if (limit <= 0L) return 0;

        int streak = 0;
        LocalDate cursor = endDate;
        for (int i = 0; i < 366; i++) {
            long dayTotal = totalForDate(prefs, cursor);
            if (dayTotal <= limit) {
                streak++;
                cursor = cursor.minusDays(1);
            } else {
                break;
            }
        }
        return streak;
    }

    public static Map<String, Long> categoryTotalsForMonth(SharedPreferences prefs, LocalDate monthAnchor) {
        return categoryTotals(forMonth(prefs, monthAnchor));
    }

    public static long parseAmountToPaise(String raw) {
        BigDecimal amount = new BigDecimal(raw.trim());
        if (amount.signum() < 0) throw new NumberFormatException("Negative amount");
        return amount.movePointRight(2).setScale(0, java.math.RoundingMode.HALF_UP).longValueExact();
    }
}
