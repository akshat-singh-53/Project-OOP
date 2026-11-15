package com.itsm.notification;

import com.itsm.domain.notification.Notification;
import com.itsm.domain.user.User;
import com.itsm.repository.NotificationRepository;

/** Template method: subclasses deliver the message; this class always records it in the outbox. */
public abstract class AbstractNotificationSender implements NotificationSender {

    private final NotificationRepository repository;

    protected AbstractNotificationSender(NotificationRepository repository) {
        this.repository = repository;
    }

    @Override
    public final void send(User recipient, NotificationMessage message) {
        String destination = destinationFor(recipient);
        if (destination == null || destination.isBlank()) {
            return;
        }
        deliver(destination, message);
        repository.save(new Notification(recipient, channel(), destination, message.subject(), message.body(),
                message.incidentCode()));
    }

    protected abstract String destinationFor(User recipient);

    protected abstract void deliver(String destination, NotificationMessage message);
}
