package com.nealaringale.nextbell;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.time.LocalDate;

import org.junit.Test;

public class SmsTransactionParserTest {
    private static final LocalDate RECEIVED = LocalDate.of(2026, 11, 7);

    @Test
    public void parsesDebitMerchantDateAndAvailableBalance() {
        SmsTransactionParser.ParsedTransaction parsed = SmsTransactionParser.parse(
                "VM-HDFCBK",
                "UPI INR 250.00 debited from a/c xx1234 at Swiggy on 06-11-2026. "
                        + "Available balance: INR 1,250.50",
                RECEIVED
        );

        assertNotNull(parsed);
        assertEquals(25_000L, parsed.amountPaise);
        assertEquals(125_050L, parsed.availableBalancePaise);
        assertEquals(LocalDate.of(2026, 11, 6), parsed.date);
        assertEquals(SmsTransactionParser.DEBIT, parsed.direction);
        assertEquals("Swiggy", parsed.merchant);
        assertEquals("Food & Drinks", parsed.category);
        assertEquals("UPI", parsed.paymentMode);
    }

    @Test
    public void classifiesRefundAsIncomeAndNeverAsSpend() {
        SmsTransactionParser.ParsedTransaction parsed = SmsTransactionParser.parse(
                "AD-ICICIB", "Rs. 99.50 refund credited to your account from Zomato", RECEIVED
        );

        assertNotNull(parsed);
        assertEquals(9_950L, parsed.amountPaise);
        assertEquals(SmsTransactionParser.CREDIT, parsed.direction);
        assertEquals("Income & Refunds", parsed.category);
    }

    @Test
    public void ignoresFailuresAndOrdinaryMessages() {
        assertNull(SmsTransactionParser.parse("VM-HDFCBK",
                "Your UPI payment of Rs 250 failed and was not debited", RECEIVED));
        assertNull(SmsTransactionParser.parse("Friend",
                "I will pay you Rs 250 tomorrow", RECEIVED));
        assertNull(SmsTransactionParser.parse("Neala",
                "UPI payment of Rs 250 debited at Zomato successfully", RECEIVED));
    }

    @Test
    public void parsesBankFormatWithoutCurrencyPrefixWhenAmountIsContextualized() {
        SmsTransactionParser.ParsedTransaction parsed = SmsTransactionParser.parse(
                "VM-HDFCBK", "A/c XX123 debited by 1,250.00 at Canteen. Avl balance Rs 500", RECEIVED
        );
        assertNotNull(parsed);
        assertEquals(125_000L, parsed.amountPaise);
        assertEquals(50_000L, parsed.availableBalancePaise);
    }

    @Test
    public void debitWithCreditedToVpaRemainsDebitAndExtractsIndianDate() {
        SmsTransactionParser.ParsedTransaction parsed = SmsTransactionParser.parse(
                "VM-HDFCBK",
                "UPI Rs 150.00 debited from A/c XX1234 on 09-Nov-26 "
                        + "and credited to VPA swiggy@icici. Avl Bal INR 4,500.00",
                RECEIVED
        );

        assertNotNull(parsed);
        assertEquals(15_000L, parsed.amountPaise);
        assertEquals(450_000L, parsed.availableBalancePaise);
        assertEquals(LocalDate.of(2026, 11, 9), parsed.date);
        assertEquals(SmsTransactionParser.DEBIT, parsed.direction);
        assertEquals("Swiggy", parsed.merchant);
        assertEquals("Food & Drinks", parsed.category);
    }

    @Test
    public void balanceBeforeDebitDoesNotBecomeTransactionAmount() {
        SmsTransactionParser.ParsedTransaction parsed = SmsTransactionParser.parse(
                "VM-SBIINB",
                "Bal INR 5,000. Rs 200 debited at Cafe on 09-11-2026",
                RECEIVED
        );

        assertNotNull(parsed);
        assertEquals(20_000L, parsed.amountPaise);
        assertEquals(500_000L, parsed.availableBalancePaise);
        assertEquals(SmsTransactionParser.DEBIT, parsed.direction);
    }

    @Test
    public void creditedToOwnAccountIsStillRecognizedAsIncome() {
        SmsTransactionParser.ParsedTransaction parsed = SmsTransactionParser.parse(
                "AD-ICICIB",
                "INR 500.00 credited to your account from Rahul",
                RECEIVED
        );

        assertNotNull(parsed);
        assertEquals(50_000L, parsed.amountPaise);
        assertEquals(SmsTransactionParser.CREDIT, parsed.direction);
    }

    @Test
    public void fuzzyMerchantMatchAndStableFingerprintWork() {
        String body = "UPI payment of Rs 150 debited at flipkartt successfully";
        SmsTransactionParser.ParsedTransaction first =
                SmsTransactionParser.parse("VM-SBIINB", body, RECEIVED);
        SmsTransactionParser.ParsedTransaction second =
                SmsTransactionParser.parse("VM-SBIINB", body, RECEIVED);

        assertNotNull(first);
        assertEquals("Flipkart", first.merchant);
        assertEquals("Shopping", first.category);
        assertEquals(first.fingerprint, second.fingerprint);
    }
}
