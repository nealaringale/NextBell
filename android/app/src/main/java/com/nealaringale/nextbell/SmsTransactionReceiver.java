package com.nealaringale.nextbell;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;

public class SmsTransactionReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!"android.provider.Telephony.SMS_RECEIVED".equals(intent.getAction())) return;

        Bundle extras = intent.getExtras();
        if (extras == null) return;

        Object[] pdus;
        try {
            pdus = (Object[]) extras.get("pdus");
        } catch (Exception ignored) {
            return;
        }
        if (pdus == null || pdus.length == 0) return;

        String format = extras.getString("format");
        long receivedAt = System.currentTimeMillis();

        StringBuilder body = new StringBuilder();
        String sender = "";

        for (Object pdu : pdus) {
            try {
                SmsMessage message = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M
                        ? SmsMessage.createFromPdu((byte[]) pdu, format)
                        : SmsMessage.createFromPdu((byte[]) pdu);

                if (message == null) continue;
                if (sender.isEmpty()) sender = message.getDisplayOriginatingAddress();
                body.append(message.getMessageBody());
            } catch (Exception ignored) {
                // Ignore malformed multipart segments.
            }
        }

        if (!body.toString().trim().isEmpty()) {
            SmsAutoTracker.processIncoming(context, sender, body.toString(), receivedAt);
        }
    }
}
