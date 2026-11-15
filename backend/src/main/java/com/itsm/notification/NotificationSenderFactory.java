package com.itsm.notification;

import com.itsm.domain.notification.NotificationChannel;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Factory pattern: hands out the sender implementation for a requested channel. */
@Component
public class NotificationSenderFactory {

    private final Map<NotificationChannel, NotificationSender> senders = new EnumMap<>(NotificationChannel.class);

    public NotificationSenderFactory(List<NotificationSender> implementations) {
        implementations.forEach(s -> senders.put(s.channel(), s));
    }

    public NotificationSender forChannel(NotificationChannel channel) {
        NotificationSender sender = senders.get(channel);
        if (sender == null) {
            throw new IllegalArgumentException("No sender registered for channel " + channel);
        }
        return sender;
    }
}
