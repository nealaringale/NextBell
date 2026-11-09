package com.nealaringale.nextbell;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** User-controlled access and source allowlist for local transaction notification capture. */
public final class TransactionNotificationAccess {
    private static final Set<String> SUPPORTED_PACKAGES = new HashSet<>(Arrays.asList(
            "com.google.android.apps.messaging",
            "com.truecaller",
            "com.android.messaging",
            "com.samsung.android.messaging",
            "com.miui.mms",
            "com.android.mms",
            "com.coloros.mms",
            "com.oneplus.mms",
            "com.google.android.apps.nbu.paisa.user",
            "com.phonepe.app",
            "net.one97.paytm",
            "in.org.npci.upiapp",
            "com.amazon.mShop.android.shopping",
            "com.sbi.lotusintouch",
            "com.csam.icici.bank.imobile",
            "com.axis.mobile",
            "com.axis.mobilebanking",
            "com.hdfcbank",
            "com.hdfcbank.android.now",
            "com.kotak.mobilebanking",
            "com.rblbank.mobank",
            "com.idfcfirstbank.optimus",
            "com.indusind.mobilebanking",
            "com.yesbank.app",
            "com.federalbank.fedmobile",
            "com.bankofbaroda.mconnect",
            "com.pnbindia.mconnect",
            "com.unionbankofindia",
            "com.canarabank.mpassbook",
            "com.snapwork.hdfc",
            "com.kotak.bank.mobile",
            "com.aubank.au0101",
            "com.indianbank.indsmart",
            "com.bankofindia",
            "com.centralbank.mobilebanking",
            "com.ucobank.mbanking",
            "com.idbibank.mobilebanking",
            "com.bandhanbank.mobilebanking",
            "com.unionbank.usmart",
            "com.hdfcbank.payzapp",
            "com.dreamplug.androidapp",
            "com.mobikwik_new",
            "com.freecharge.android",
            "com.naviapp"
    ));

    private TransactionNotificationAccess() {}

    public static boolean hasAccess(Context context) {
        String enabled = Settings.Secure.getString(
                context.getContentResolver(), "enabled_notification_listeners"
        );
        if (enabled == null || enabled.trim().isEmpty()) return false;

        ComponentName expected = new ComponentName(
                context, TransactionNotificationListener.class
        );
        for (String entry : enabled.split(":")) {
            ComponentName component = ComponentName.unflattenFromString(entry);
            if (expected.equals(component)) return true;
        }
        return false;
    }

    public static boolean isSupportedPackage(String packageName) {
        if (packageName == null) return false;
        String value = packageName.trim().toLowerCase(Locale.ENGLISH);
        return SUPPORTED_PACKAGES.contains(value)
                || isMessagingPackage(value)
                || isRecognizedFinancePackage(value);
    }

    public static boolean isMessagingPackage(String packageName) {
        if (packageName == null) return false;
        String value = packageName.trim().toLowerCase(Locale.ENGLISH);
        return value.equals("com.google.android.apps.messaging")
                || value.equals("com.truecaller")
                || value.equals("com.android.messaging")
                || value.equals("com.samsung.android.messaging")
                || value.equals("com.miui.mms")
                || value.equals("com.android.mms")
                || value.equals("com.coloros.mms")
                || value.equals("com.oneplus.mms")
                || value.contains("messaging")
                || value.endsWith(".mms")
                || value.endsWith(".sms");
    }

    private static boolean isRecognizedFinancePackage(String value) {
        return value.contains("bank") || value.contains("hdfc")
                || value.contains("icici") || value.contains("sbi")
                || value.contains("axis") || value.contains("kotak")
                || value.contains("indus") || value.contains("yesbank")
                || value.contains("federal") || value.contains("baroda")
                || value.contains("canara") || value.contains("punjabnational")
                || value.contains("pnb") || value.contains("unionbank")
                || value.contains("idfc") || value.contains("rbl")
                || value.contains("upi") || value.contains("wallet")
                || value.contains("phonepe") || value.contains("paytm")
                || value.contains("gpay") || value.contains("mobikwik")
                || value.contains("freecharge") || value.contains("payzapp")
                || value.contains("dreamplug") || value.contains("naviapp")
                || value.contains("au0101");
    }

    /** Keeps ordinary personal SMS out of the finance parser. */
    public static boolean isLikelyFinancialSms(String title, String body) {
        String value = ((title == null ? "" : title) + " "
                + (body == null ? "" : body)).toLowerCase(Locale.ENGLISH);
        String[] cues = {
                "bank", "a/c", "account", "upi", "neft", "imps", "rtgs",
                "debited", "credited", "debit", "credit", "withdraw",
                "transaction", "txn", "avl bal", "available balance",
                "ledger balance", "refund", "cashback", "deposit",
                "pos txn", "atm", "card ending", "card xx", "transfer"
        };
        for (String cue : cues) {
            if (value.contains(cue)) return true;
        }
        return false;
    }

    public static String shortStatus(Context context, android.content.SharedPreferences preferences) {
        if (!hasAccess(context)) {
            return "Not connected · tap CONNECT and enable NextBell under Android Notification access.";
        }
        if (!SmsTransactionService.isEnabled(preferences)) {
            return "Access is granted · enable tracking to start importing supported payment alerts.";
        }
        return "Connected · monitoring supported messaging, bank and UPI notifications on this phone.";
    }

    public static Intent settingsIntent() {
        return new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
    }
}
