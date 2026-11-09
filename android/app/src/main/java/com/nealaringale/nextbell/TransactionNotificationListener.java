package com.nealaringale.nextbell;

import android.app.Notification;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Parcelable;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Captures financial transaction previews from notification sources after explicit user access. */
public final class TransactionNotificationListener extends NotificationListenerService {
    private static final ZoneId INDIA = ZoneId.of("Asia/Kolkata");
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "NextBell-payment-alerts");
        thread.setDaemon(true);
        return thread;
    });

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        try {
            StatusBarNotification[] active = getActiveNotifications();
            if (active != null) {
                for (StatusBarNotification item : active) handleNotification(item);
            }
        } catch (RuntimeException ignored) {
            // Notification access can be revoked while Android is reconnecting the listener.
        }
    }

    @Override
    public void onListenerDisconnected() {
        super.onListenerDisconnected();
        // Android documents this as the supported recovery path after disconnection.
        NotificationListenerService.requestRebind(
                new ComponentName(this, TransactionNotificationListener.class)
        );
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        handleNotification(sbn);
    }

    private void handleNotification(StatusBarNotification sbn) {
        if (sbn == null || sbn.getNotification() == null) return;
        String packageName = sbn.getPackageName();
        if (getPackageName().equals(packageName)) return;

        SharedPreferences preferences = getSharedPreferences(
                SmsTransactionService.PREFS_NAME, MODE_PRIVATE
        );
        if (!SmsTransactionService.isEnabled(preferences)
                || !TransactionNotificationAccess.hasAccess(this)) return;

        Notification notification = sbn.getNotification();
        Bundle extras = notification.extras;
        if (extras == null) return;

        String title = value(extras.getCharSequence(Notification.EXTRA_TITLE));
        String body = collectBody(extras);
        if (body.isEmpty() || !TransactionNotificationAccess.isLikelyFinancialNotification(
                packageName, title, body)) return;

        SmsTransactionService.recordNotificationSeen(preferences, packageName);
        long postedAt = sbn.getPostTime() > 0L ? sbn.getPostTime() : System.currentTimeMillis();
        LocalDate receivedDate = Instant.ofEpochMilli(postedAt).atZone(INDIA).toLocalDate();

        final String capturedPackage = packageName;
        final String capturedTitle = title;
        final String capturedBody = body;
        try {
            WORKER.execute(() -> {
                try {
                    SharedPreferences current = getSharedPreferences(
                            SmsTransactionService.PREFS_NAME, MODE_PRIVATE
                    );
                    if (!SmsTransactionService.isEnabled(current)
                            || !TransactionNotificationAccess.hasAccess(this)) return;
                    boolean imported = SmsTransactionService.processNotification(
                            current, capturedPackage, capturedTitle, capturedBody, receivedDate
                    );
                    if (imported) ExpenseDailyAlertReceiver.maybeNotifyNow(this);
                } catch (RuntimeException error) {
                    SmsTransactionService.recordNotificationProcessingError(
                            getSharedPreferences(SmsTransactionService.PREFS_NAME, MODE_PRIVATE), error
                    );
                } finally {
                    Intent processed = new Intent(SmsTransactionService.ACTION_NOTIFICATION_PROCESSED);
                    processed.setPackage(getPackageName());
                    sendBroadcast(processed);
                }
            });
        } catch (RuntimeException ignored) {
            // A full worker queue should not crash Android's notification pipeline.
        }
    }

    private static String collectBody(Bundle extras) {
        StringBuilder body = new StringBuilder();
        append(body, extras.getCharSequence(Notification.EXTRA_BIG_TEXT));
        append(body, extras.getCharSequence(Notification.EXTRA_TEXT));
        append(body, extras.getCharSequence(Notification.EXTRA_SUB_TEXT));
        append(body, extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT));
        append(body, extras.getCharSequence(Notification.EXTRA_INFO_TEXT));
        append(body, extras.getCharSequence(Notification.EXTRA_TITLE_BIG));
        append(body, extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE));

        CharSequence[] textLines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);
        if (textLines != null) {
            for (CharSequence line : textLines) append(body, line);
        }

        Parcelable[] messages = extras.getParcelableArray(Notification.EXTRA_MESSAGES);
        if (messages != null) {
            for (Parcelable item : messages) {
                if (item instanceof Bundle) {
                    append(body, ((Bundle) item).getCharSequence("text"));
                }
            }
        }
        String value = body.toString().trim();
        return value.length() > 2_000 ? value.substring(0, 2_000) : value;
    }

    private static void append(StringBuilder destination, CharSequence value) {
        if (value == null || TextUtils.isEmpty(value)) return;
        String text = value.toString().trim();
        if (text.isEmpty()) return;
        if (destination.length() > 0) destination.append(". ");
        destination.append(text);
    }

    private static String value(CharSequence value) {
        return value == null ? "" : value.toString().trim();
    }
}
