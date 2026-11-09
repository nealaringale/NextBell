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
            "com.canarabank.mpassbook"
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
        if (SUPPORTED_PACKAGES.contains(value)) return true;
        return value.contains("messaging")
                || value.endsWith(".mms")
                || value.endsWith(".sms")
                || value.contains("bank")
                || value.contains("upi")
                || value.contains("wallet")
                || value.contains("phonepe")
                || value.contains("paytm")
                || value.contains("gpay")
                || value.contains("mobikwik")
                || value.contains("freecharge");
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
