package com.itsm.repository;

import com.itsm.domain.notification.Notification;
import com.itsm.domain.notification.NotificationChannel;
import com.itsm.domain.user.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientAndChannelOrderByCreatedAtDesc(User recipient, NotificationChannel channel,
                                                                    Pageable pageable);

    long countByRecipientAndChannelAndReadFlagFalse(User recipient, NotificationChannel channel);

    List<Notification> findByRecipientAndChannelAndReadFlagFalse(User recipient, NotificationChannel channel);

    List<Notification> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<Notification> findByIncidentCode(String incidentCode);
}
