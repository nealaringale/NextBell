package com.nealaringale.nextbell;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.provider.Telephony;
import android.telephony.SmsMessage;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Handles only new transaction-like SMS while the user has enabled local tracking. */
public final class SmsTransactionReceiver extends BroadcastReceiver {
    private static final ZoneId INDIA = ZoneId.of("Asia/Kolkata");
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "NextBell-SMS");
        thread.setDaemon(true);
        return thread;
    });

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null
                || !Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())) {
            return;
        }

        Context appContext = context.getApplicationContext();
        SharedPreferences preferences = appContext.getSharedPreferences(
                SmsTransactionService.PREFS_NAME, Context.MODE_PRIVATE
        );
        if (!SmsTransactionService.isEnabled(preferences)
                || !SmsTransactionService.hasReceivePermission(appContext)) {
            return;
        }

        final SmsMessage[] messages;
        try {
            messages = Telephony.Sms.Intents.getMessagesFromIntent(intent);
        } catch (RuntimeException ignored) {
            return;
        }
        if (messages == null || messages.length == 0) return;

        StringBuilder body = new StringBuilder();
        String sender = "";
        long timestamp = Long.MAX_VALUE;
        for (SmsMessage message : messages) {
            if (message == null) continue;
            if (sender.isEmpty()) sender = message.getDisplayOriginatingAddress();
            String part = message.getMessageBody();
            if (part != null) body.append(part);
            long messageTime = message.getTimestampMillis();
            if (messageTime > 0L) timestamp = Math.min(timestamp, messageTime);
        }
        if (sender.isEmpty() || body.length() == 0) return;

        long receivedAt = timestamp == Long.MAX_VALUE ? System.currentTimeMillis() : timestamp;
        LocalDate receivedDate = Instant.ofEpochMilli(receivedAt).atZone(INDIA).toLocalDate();
        final String messageSender = sender;
        String messageBody = body.toString();
        PendingResult pending = goAsync();

        try {
            WORKER.execute(() -> {
                try {
                    SharedPreferences currentPreferences = appContext.getSharedPreferences(
                            SmsTransactionService.PREFS_NAME, Context.MODE_PRIVATE
                    );
                    // Re-check opt-in and permission in case the user disabled tracking
                    // while this broadcast was waiting in the worker queue.
                    if (SmsTransactionService.isEnabled(currentPreferences)
                            && SmsTransactionService.hasReceivePermission(appContext)) {
                        boolean imported = SmsTransactionService.processMessage(
                                currentPreferences, messageSender, messageBody, receivedDate
                        );
                        if (imported) ExpenseDailyAlertReceiver.maybeNotifyNow(appContext);
                    }
                } catch (RuntimeException ignored) {
                    // A malformed bank alert must never crash the SMS broadcast path.
                } finally {
                    pending.finish();
                }
            });
        } catch (RuntimeException rejected) {
            pending.finish();
        }
    }
}
