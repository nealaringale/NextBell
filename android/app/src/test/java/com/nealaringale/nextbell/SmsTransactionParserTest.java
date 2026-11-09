package com.nealaringale.nextbell;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.time.LocalDate;

import org.junit.Test;

public class SmsTransactionParserTest {
    private static final LocalDate RECEIVED = LocalDate.of(2026, 11, 7);

    @Test
    public void parsesBankDebitFromAnUnlistedNotificationPackage() {
        SmsTransactionParser.ParsedTransaction parsed = SmsTransactionParser.parseNotification(
                "com.vendor.smsclient",
                "ICICI Bank",
                "Your A/c XX702 has been debited by INR 1,250.00 at Local Cafe.",
                RECEIVED
        );

        assertNotNull(parsed);
        assertEquals(125_000L, parsed.amountPaise);
        assertEquals(SmsTransactionParser.DEBIT, parsed.direction);
        assertEquals("Local Cafe", parsed.merchant);
        assertEquals("Other", parsed.category);
    }

    @Test
    public void parsesGenericPaymentNotificationFromUnknownAppPackage() {
        SmsTransactionParser.ParsedTransaction parsed = SmsTransactionParser.parseNotification(
                "com.example.notification.wrapper",
                "Payment successful",
                "You paid Rs 89.00 to Cafe using UPI successfully.",
                RECEIVED
        );

        assertNotNull(parsed);
        assertEquals(8_900L, parsed.amountPaise);
        assertEquals(SmsTransactionParser.DEBIT, parsed.direction);
        assertEquals("Cafe", parsed.merchant);
    }

    @Test
    public void parsesCardSpendNotificationWithSpentOnMerchantWording() {
        SmsTransactionParser.ParsedTransaction parsed = SmsTransactionParser.parseNotification(
                "com.unknown.card.alerts",
                "Card transaction",
                "INR 456.75 spent on Amazon using your card.",
                RECEIVED
        );

        assertNotNull(parsed);
        assertEquals(45_675L, parsed.amountPaise);
        assertEquals(SmsTransactionParser.DEBIT, parsed.direction);
        assertEquals("Amazon", parsed.merchant);
        assertEquals("Shopping", parsed.category);
    }

    @Test
    public void parsesIncomingCreditFromUnknownNotificationSource() {
        SmsTransactionParser.ParsedTransaction parsed = SmsTransactionParser.parseNotification(
                "com.unknown.sms.wrapper",
                "Bank credit",
                "INR 2,000.00 credited to your account from Rahul.",
                RECEIVED
        );

        assertNotNull(parsed);
        assertEquals(200_000L, parsed.amountPaise);
        assertEquals(SmsTransactionParser.CREDIT, parsed.direction);
        assertEquals("Rahul", parsed.merchant);
    }

    @Test
    public void rejectsUnrelatedPersonalPaidMessageFromUnknownApp() {
        assertNull(SmsTransactionParser.parseNotification(
                "com.unknown.sms.wrapper",
                "Message from friend",
                "I paid Rs 100 for lunch today.",
                RECEIVED
        ));
    }

    @Test
    public void parsesTruecallerSignedAmountPreviewWhenSmsBodyIsHidden() {
        SmsTransactionParser.ParsedTransaction parsed = SmsTransactionParser.parseNotification(
                "com.truecaller",
                "ICICI Bank",
                "- Rs1.00",
                RECEIVED
        );

        assertNotNull(parsed);
        assertEquals(100L, parsed.amountPaise);
        assertEquals(SmsTransactionParser.DEBIT, parsed.direction);
        assertEquals("Unidentified merchant", parsed.merchant);
        assertEquals("Other", parsed.category);
    }

    @Test
    public void extractsMerchantWhenTruecallerShowsFullUpiText() {
        SmsTransactionParser.ParsedTransaction parsed = SmsTransactionParser.parseNotification(
                "com.truecaller",
                "ICICI Bank",
                "ICICI Bank account debited by Rs 240.00 for UPI payment to Swiggy",
                RECEIVED
        );

        assertNotNull(parsed);
        assertEquals(24_000L, parsed.amountPaise);
        assertEquals(SmsTransactionParser.DEBIT, parsed.direction);
        assertEquals("Swiggy", parsed.merchant);
        assertEquals("Food & Drinks", parsed.category);
    }

    @Test
    public void explainsNotificationWithoutVisibleAmountOrDirection() {
        assertEquals("Amount not present in the notification preview",
                SmsTransactionParser.notificationFailureReason(
                        "com.truecaller", "ICICI Bank", "Bank alert received", RECEIVED
                ));
    }

    @Test
    public void ignoresOrdinaryPersonalMessagesFromSmsApps() {
        assertNull(SmsTransactionParser.parseNotification(
                "com.google.android.apps.messaging",
                "A friend",
                "I paid Rs. 100 for lunch, I will tell you later",
                RECEIVED
        ));
    }

    @Test
    public void parsesTruecallerForwardedIciciBankAlert() {
        SmsTransactionParser.ParsedTransaction parsed = SmsTransactionParser.parseNotification(
                "com.truecaller",
                "ICICI Bank",
                "ICICI Bank Acct XX702 debited for Rs 1.00.",
                RECEIVED
        );

        assertNotNull(parsed);
        assertEquals(100L, parsed.amountPaise);
        assertEquals(SmsTransactionParser.DEBIT, parsed.direction);
    }

    @Test
    public void parsesBankSmsNotificationWithoutSmsPermission() {
        SmsTransactionParser.ParsedTransaction parsed = SmsTransactionParser.parseNotification(
                "com.google.android.apps.messaging",
                "VM-HDFCBK",
                "A/c XX1234 debited by Rs 240.00 at Swiggy. Avl Bal INR 1,700.00",
                RECEIVED
        );

        assertNotNull(parsed);
        assertEquals(24_000L, parsed.amountPaise);
        assertEquals(170_000L, parsed.availableBalancePaise);
        assertEquals(SmsTransactionParser.DEBIT, parsed.direction);
        assertEquals("Swiggy", parsed.merchant);
        assertEquals("Food & Drinks", parsed.category);
    }

    @Test
    public void parsesPaymentAppNotificationAndRejectsUntrustedApps() {
        SmsTransactionParser.ParsedTransaction parsed = SmsTransactionParser.parseNotification(
                "com.phonepe.app",
                "PhonePe",
                "You paid Rs. 79.00 to Cafe successfully",
                RECEIVED
        );

        assertNotNull(parsed);
        assertEquals(7_900L, parsed.amountPaise);
        assertEquals(SmsTransactionParser.DEBIT, parsed.direction);

        assertNull(SmsTransactionParser.parseNotification(
                "com.random.game", "Reward", "You paid Rs. 79.00 successfully", RECEIVED
        ));
    }

    @Test
    public void parsesIncomingCreditFromPaymentNotification() {
        SmsTransactionParser.ParsedTransaction parsed = SmsTransactionParser.parseNotification(
                "com.google.android.apps.messaging",
                "Bank credit alert",
                "You received INR 500.00 from Rahul",
                RECEIVED
        );

        assertNotNull(parsed);
        assertEquals(50_000L, parsed.amountPaise);
        assertEquals(SmsTransactionParser.CREDIT, parsed.direction);
        assertEquals("Income & Refunds", parsed.category);
    }

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
