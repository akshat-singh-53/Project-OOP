package com.itsm.notification;

import com.itsm.domain.notification.NotificationChannel;
import com.itsm.domain.user.User;

/** One delivery mechanism. Adding a new mechanism (e.g. push, Slack) means adding one implementation. */
public interface NotificationSender {

    NotificationChannel channel();

    void send(User recipient, NotificationMessage message);
}
