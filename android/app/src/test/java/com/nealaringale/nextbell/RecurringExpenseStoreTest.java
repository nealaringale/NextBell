package com.nealaringale.nextbell;

import static org.junit.Assert.assertEquals;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class RecurringExpenseStoreTest {
    @Test
    public void suggestsMonthlyRepeatWithSmallAmountVariation() {
        List<ExpenseStore.Expense> transactions = Arrays.asList(
                expense(1, LocalDate.of(2026, 9, 5), 100_00),
                expense(2, LocalDate.of(2026, 10, 5), 105_00),
                expense(3, LocalDate.of(2026, 11, 5), 100_00),
                new ExpenseStore.Expense(4, 5_000, LocalDate.of(2026, 11, 5),
                        "Income & Refunds", "", "UPI", "Rent", "SMS", 1f,
                        "credit", SmsTransactionParser.CREDIT, -1L)
        );

        List<RecurringExpenseStore.Candidate> candidates =
                RecurringExpenseStore.detectMonthlyCandidates(transactions);
        assertEquals(1, candidates.size());
        assertEquals("Coffee Shop", candidates.get(0).merchant);
        assertEquals(3, candidates.get(0).occurrences);
        assertEquals(10_000L, candidates.get(0).suggestedAmountPaise);
    }

    private static ExpenseStore.Expense expense(long id, LocalDate date, long amount) {
        return new ExpenseStore.Expense(id, amount, date, "Food & Drinks", "", "UPI",
                "Coffee Shop", "SMS", 0.9f, "f" + id);
    }
}
