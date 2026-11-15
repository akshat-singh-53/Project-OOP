package com.itsm.service;

import com.itsm.domain.notification.Notification;
import com.itsm.domain.notification.NotificationChannel;
import com.itsm.domain.user.User;
import com.itsm.exception.ResourceNotFoundException;
import com.itsm.exception.UnauthorizedOperationException;
import com.itsm.notification.NotificationMessage;
import com.itsm.notification.NotificationSenderFactory;
import com.itsm.repository.NotificationRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class NotificationService {

    private final NotificationSenderFactory factory;
    private final NotificationRepository repository;
    private final SettingsService settings;

    public NotificationService(NotificationSenderFactory factory, NotificationRepository repository,
                               SettingsService settings) {
        this.factory = factory;
        this.repository = repository;
        this.settings = settings;
    }

    /** Sends the message to each distinct, active recipient over every enabled channel. */
    @Transactional
    public void notify(Collection<? extends User> recipients, Set<NotificationChannel> channels,
                       NotificationMessage message) {
        Map<Long, User> unique = new LinkedHashMap<>();
        recipients.stream().filter(u -> u != null && u.isActive()).forEach(u -> unique.putIfAbsent(u.getId(), u));
        Set<NotificationChannel> enabled = enabledChannels(channels);
        for (User user : unique.values()) {
            for (NotificationChannel channel : enabled) {
                factory.forChannel(channel).send(user, message);
            }
        }
    }

    private Set<NotificationChannel> enabledChannels(Set<NotificationChannel> requested) {
        Set<NotificationChannel> result = EnumSet.noneOf(NotificationChannel.class);
        for (NotificationChannel c : requested) {
            boolean on = switch (c) {
                case IN_APP -> true;
                case EMAIL -> settings.getBoolean(SettingsService.EMAIL_ENABLED);
                case SMS -> settings.getBoolean(SettingsService.SMS_ENABLED);
            };
            if (on) {
                result.add(c);
            }
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<Notification> inbox(User user, int limit) {
        return repository.findByRecipientAndChannelOrderByCreatedAtDesc(user, NotificationChannel.IN_APP,
                PageRequest.of(0, limit));
    }

    @Transactional(readOnly = true)
    public long unreadCount(User user) {
        return repository.countByRecipientAndChannelAndReadFlagFalse(user, NotificationChannel.IN_APP);
    }

    @Transactional
    public void markRead(User user, Long notificationId) {
        Notification n = repository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", notificationId));
        if (!n.getRecipient().getId().equals(user.getId())) {
            throw new UnauthorizedOperationException("This notification belongs to another user");
        }
        n.markRead();
    }

    @Transactional
    public void markAllRead(User user) {
        repository.findByRecipientAndChannelAndReadFlagFalse(user, NotificationChannel.IN_APP)
                .forEach(Notification::markRead);
    }

    @Transactional(readOnly = true)
    public List<Notification> outbox(int limit) {
        return repository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, limit));
    }
}
