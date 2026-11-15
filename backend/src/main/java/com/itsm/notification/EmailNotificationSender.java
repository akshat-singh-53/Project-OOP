package com.itsm.notification;

import com.itsm.domain.notification.NotificationChannel;
import com.itsm.domain.user.User;
import com.itsm.repository.NotificationRepository;
import org.springframework.stereotype.Component;

@Component
public class EmailNotificationSender extends AbstractNotificationSender {

    private final EmailGateway gateway;

    public EmailNotificationSender(NotificationRepository repository, EmailGateway gateway) {
        super(repository);
        this.gateway = gateway;
    }

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    protected String destinationFor(User recipient) {
        return recipient.getEmail();
    }

    @Override
    protected void deliver(String destination, NotificationMessage message) {
        gateway.sendEmail(destination, message.subject(), message.body());
    }
}
