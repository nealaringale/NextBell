package com.nealaringale.nextbell;

import static org.junit.Assert.assertEquals;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public class ExpenseStoreTest {
    @Test
    public void amountParsingUsesPaiseAndRoundsHalfUp() {
        assertEquals(123L, ExpenseStore.parseAmountToPaise("1.23"));
        assertEquals(124L, ExpenseStore.parseAmountToPaise("1.235"));
        assertEquals(0L, ExpenseStore.parseAmountToPaise(" 0 "));
        assertInvalidAmount("-1.00");
        assertInvalidAmount("not money");
        assertInvalidAmount("999999999999999999999999999999999");
    }

    @Test
    public void sortingUsesNewestDateThenNewestId() {
        LocalDate earlier = LocalDate.of(2026, 11, 6);
        LocalDate later = LocalDate.of(2026, 11, 7);
        ExpenseStore.Expense old = expense(9, earlier, 100);
        ExpenseStore.Expense sameDateOlderId = expense(2, later, 100);
        ExpenseStore.Expense sameDateNewerId = expense(8, later, 100);

        List<ExpenseStore.Expense> expenses = new ArrayList<>(Arrays.asList(
                sameDateOlderId, old, sameDateNewerId
        ));
        ExpenseStore.sortNewestFirst(expenses);

        assertEquals(sameDateNewerId, expenses.get(0));
        assertEquals(sameDateOlderId, expenses.get(1));
        assertEquals(old, expenses.get(2));
    }

    @Test
    public void moneyTotalsSaturateInsteadOfWrapping() {
        List<ExpenseStore.Expense> expenses = Arrays.asList(
                expense(1, LocalDate.of(2026, 11, 7), Long.MAX_VALUE - 5),
                expense(2, LocalDate.of(2026, 11, 7), 10)
        );

        assertEquals(Long.MAX_VALUE, ExpenseStore.totalPaise(expenses));
        Map<String, Long> categories = ExpenseStore.categoryTotals(expenses);
        assertEquals(Long.MAX_VALUE, categories.get("Food & Drinks").longValue());

        List<ExpenseStore.Expense> damaged = Arrays.asList(
                expense(3, LocalDate.of(2026, 11, 7), 500),
                expense(4, LocalDate.of(2026, 11, 7), -800)
        );
        assertEquals(500L, ExpenseStore.totalPaise(damaged));
    }

    private static ExpenseStore.Expense expense(long id, LocalDate date, long amount) {
        return new ExpenseStore.Expense(id, amount, date, "Food & Drinks", "", "Cash");
    }

    private static void assertInvalidAmount(String amount) {
        try {
            ExpenseStore.parseAmountToPaise(amount);
            throw new AssertionError("Expected invalid amount: " + amount);
        } catch (NumberFormatException | ArithmeticException expected) {
            // Invalid formats, negative amounts, and paise values outside long range are rejected.
        }
    }
}
