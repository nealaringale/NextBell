package com.nealaringale.nextbell;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;
import android.telephony.SmsMessage;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/** Handles only new transaction-like SMS while the user has enabled local tracking. */
public final class SmsTransactionReceiver extends BroadcastReceiver {
    private static final ZoneId INDIA = ZoneId.of("Asia/Kolkata");

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())) return;
        android.content.SharedPreferences preferences = context.getSharedPreferences(
                SmsTransactionService.PREFS_NAME, Context.MODE_PRIVATE
        );
        if (!SmsTransactionService.isEnabled(preferences)
                || !SmsTransactionService.hasReceivePermission(context)) return;

        SmsMessage[] messages;
        try {
            messages = Telephony.Sms.Intents.getMessagesFromIntent(intent);
        } catch (RuntimeException ignored) {
            return;
        }
        if (messages == null) return;
        StringBuilder body = new StringBuilder();
        String sender = "";
        long timestamp = System.currentTimeMillis();
        for (SmsMessage message : messages) {
            if (message == null) continue;
            if (sender.isEmpty()) sender = message.getDisplayOriginatingAddress();
            String part = message.getMessageBody();
            if (part != null) body.append(part);
            timestamp = Math.min(timestamp, message.getTimestampMillis());
        }
        LocalDate receivedDate = Instant.ofEpochMilli(timestamp).atZone(INDIA).toLocalDate();
        boolean imported = SmsTransactionService.processMessage(
                preferences, sender, body.toString(), receivedDate
        );
        if (imported) ExpenseDailyAlertReceiver.maybeNotifyNow(context);
    }
}
