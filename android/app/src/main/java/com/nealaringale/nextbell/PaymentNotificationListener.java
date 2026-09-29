package com.nealaringale.nextbell;

import android.app.Notification;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;

public class PaymentNotificationListener extends NotificationListenerService {
    private static volatile PaymentNotificationListener current;

    static PaymentNotificationListener getCurrent() {
        return current;
    }

    @Override
    public void onListenerConnected() {
        current = this;
        scanActiveNotifications();
    }

    @Override
    public void onListenerDisconnected() {
        if (current == this) current = null;
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null || sbn.getNotification() == null) return;
        if (getPackageName().equals(sbn.getPackageName())) return;

        Notification notification = sbn.getNotification();
        CharSequence title = notification.extras.getCharSequence(Notification.EXTRA_TITLE);
        CharSequence text = notification.extras.getCharSequence(Notification.EXTRA_TEXT);
        CharSequence bigText = notification.extras.getCharSequence(Notification.EXTRA_BIG_TEXT);

        String body = firstUseful(bigText, text);
        if (TextUtils.isEmpty(body) && TextUtils.isEmpty(title)) return;

        NotificationAutoTracker.processNotification(
                this,
                sbn.getPackageName(),
                title == null ? "" : title.toString(),
                body,
                sbn.getPostTime()
        );
    }

    void scanActiveNotifications() {
        try {
            StatusBarNotification[] active = getActiveNotifications();
            if (active == null) return;

            for (StatusBarNotification sbn : active) {
                onNotificationPosted(sbn);
            }
        } catch (Exception ignored) {
        }
    }

    private String firstUseful(CharSequence preferred, CharSequence fallback) {
        if (preferred != null && !TextUtils.isEmpty(preferred.toString().trim())) {
            return preferred.toString();
        }
        return fallback == null ? "" : fallback.toString();
    }
}
