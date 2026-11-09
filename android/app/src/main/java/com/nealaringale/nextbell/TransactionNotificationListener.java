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

    private static String collectBody(Notification notification, Bundle extras) {
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
        if (android.os.Build.VERSION.SDK_INT >= 24) {
            try {
                Notification.MessagingStyle style =
                        Notification.MessagingStyle.extractMessagingStyleFromNotification(notification);
                if (style != null) {
                    for (Notification.MessagingStyle.Message message : style.getMessages()) {
                        append(body, message.getText());
                    }
                    if (android.os.Build.VERSION.SDK_INT >= 26) {
                        for (Notification.MessagingStyle.Message message : style.getHistoricMessages()) {
                            append(body, message.getText());
                        }
                    }
                }
            } catch (RuntimeException ignored) {
                // Fall back to text extras collected above.
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
