package com.itsm.notification;

import com.itsm.domain.notification.NotificationChannel;
import com.itsm.domain.user.User;
import com.itsm.repository.NotificationRepository;
import org.springframework.stereotype.Component;

/** Dashboard notification: storing it is the delivery; the UI polls the bell icon. */
@Component
public class InAppNotificationSender extends AbstractNotificationSender {

    public InAppNotificationSender(NotificationRepository repository) {
        super(repository);
    }

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.IN_APP;
    }

    @Override
    protected String destinationFor(User recipient) {
        return recipient.getUsername();
    }

    @Override
    protected void deliver(String destination, NotificationMessage message) {
        // persisted by the base class
    }
}
