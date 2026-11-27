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
        SmsTransactionService.recordListenerConnected(getSharedPreferences(
                SmsTransactionService.PREFS_NAME, MODE_PRIVATE
        ));
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
        SmsTransactionService.recordListenerDisconnected(getSharedPreferences(
                SmsTransactionService.PREFS_NAME, MODE_PRIVATE
        ));
        // Android documents this as the supported recovery path after disconnection.
        NotificationListenerService.requestRebind(
                new ComponentName(this, TransactionNotificationListener.class)
        );
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        handleNotification(sbn);
    }

    @Override
    public void onNotificationPosted(
            StatusBarNotification sbn, NotificationListenerService.RankingMap rankingMap
    ) {
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

        // Count callbacks before filtering so diagnostics can identify OEM delivery failures.
        SmsTransactionService.recordNotificationCallback(preferences, packageName);
        Notification notification = sbn.getNotification();
        Bundle extras = notification.extras;
        if (extras == null) {
            SmsTransactionService.recordNotificationCallbackIgnored(
                    preferences, "Notification arrived without readable extras"
            );
            return;
        }

        String title = value(extras.getCharSequence(Notification.EXTRA_TITLE));
        String body = collectBody(notification, extras);
        if (title.isEmpty() && body.isEmpty()) {
            SmsTransactionService.recordNotificationCallbackIgnored(
                    preferences, "Notification callback had no readable title or text"
            );
            return;
        }
        if (!TransactionNotificationAccess.isLikelyFinancialNotification(
                packageName, title, body)) {
            SmsTransactionService.recordNotificationCallbackIgnored(
                    preferences, "Notification received but filtered as non-financial"
            );
            return;
        }

        SmsTransactionService.recordNotificationSeen(preferences, packageName);
        long postedAt = sbn.getPostTime() > 0L ? sbn.getPostTime() : System.currentTimeMillis();
        LocalDate receivedDate = Instant.ofEpochMilli(postedAt).atZone(INDIA).toLocalDate();

        final String capturedPackage = packageName;
        final String capturedTitle = title;
        final String capturedBody = body;
        final long capturedPostedAt = postedAt;
        try {
            WORKER.execute(() -> {
                try {
                    SharedPreferences current = getSharedPreferences(
                            SmsTransactionService.PREFS_NAME, MODE_PRIVATE
                    );
                    if (!SmsTransactionService.isEnabled(current)
                            || !TransactionNotificationAccess.hasAccess(this)) return;
                    boolean imported = SmsTransactionService.processNotification(
                            current, capturedPackage, capturedTitle, capturedBody,
                            receivedDate, capturedPostedAt
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

    private static String collectBody(Notification notification, Bundle extras) {
        // Android often repeats the same text in EXTRA_BIG_TEXT, EXTRA_TEXT and
        // EXTRA_TEXT_LINES. Concatenating all fields manufactured different
        // fingerprints for the same bank alert and made parsing less reliable.
        String bigText = value(extras.getCharSequence(Notification.EXTRA_BIG_TEXT));
        if (!bigText.isEmpty()) return limit(bigText);

        CharSequence[] textLines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);
        if (textLines != null && textLines.length > 0) {
            for (int i = textLines.length - 1; i >= 0; i--) {
                String line = value(textLines[i]);
                if (!line.isEmpty()) return limit(line);
            }
        }

        String text = value(extras.getCharSequence(Notification.EXTRA_TEXT));
        if (!text.isEmpty()) return limit(text);

        Parcelable[] messages = extras.getParcelableArray(Notification.EXTRA_MESSAGES);
        if (messages != null) {
            for (int i = messages.length - 1; i >= 0; i--) {
                Parcelable item = messages[i];
                if (item instanceof Bundle) {
                    String message = value(((Bundle) item).getCharSequence("text"));
                    if (!message.isEmpty()) return limit(message);
                }
            }
        }

        String[] fallbacks = {
                value(extras.getCharSequence(Notification.EXTRA_SUB_TEXT)),
                value(extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)),
                value(extras.getCharSequence(Notification.EXTRA_INFO_TEXT)),
                value(extras.getCharSequence(Notification.EXTRA_TITLE_BIG)),
                value(extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE))
        };
        for (String fallback : fallbacks) {
            if (!fallback.isEmpty()) return limit(fallback);
        }
        return "";
    }

    private static String limit(String value) {
        return value.length() > 2_000 ? value.substring(0, 2_000) : value;
    }

    private static String value(CharSequence value) {
        return value == null ? "" : value.toString().trim();
    }
}
