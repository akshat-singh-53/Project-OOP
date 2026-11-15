package com.itsm.notification;

import com.itsm.domain.notification.NotificationChannel;
import com.itsm.domain.user.User;
import com.itsm.repository.NotificationRepository;
import org.springframework.stereotype.Component;

@Component
public class SmsNotificationSender extends AbstractNotificationSender {

    private final SmsGateway gateway;

    public SmsNotificationSender(NotificationRepository repository, SmsGateway gateway) {
        super(repository);
        this.gateway = gateway;
    }

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.SMS;
    }

    @Override
    protected String destinationFor(User recipient) {
        return recipient.getPhone();
    }

    @Override
    protected void deliver(String destination, NotificationMessage message) {
        gateway.sendSms(destination, "[" + message.incidentCode() + "] " + message.subject());
    }
}
