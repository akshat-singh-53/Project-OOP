package com.itsm.domain.notification;

import com.itsm.domain.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** One delivered message. E-mail and SMS deliveries are kept as an outbox log. */
@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    private User recipient;

    @Enumerated(EnumType.STRING)
    private NotificationChannel channel;

    /** Address the message was sent to (e-mail, phone number or user name). */
    private String destination;

    @Column(length = 200)
    private String subject;

    @Column(length = 2000)
    private String message;

    private String incidentCode;

    private LocalDateTime createdAt;

    private boolean readFlag;

    protected Notification() {
    }

    public Notification(User recipient, NotificationChannel channel, String destination, String subject,
                        String message, String incidentCode) {
        this.recipient = recipient;
        this.channel = channel;
        this.destination = destination;
        this.subject = subject;
        this.message = message;
        this.incidentCode = incidentCode;
        this.createdAt = LocalDateTime.now();
    }

    public void markRead() {
        this.readFlag = true;
    }

    /** Used only by the demo-data seeder to build a realistic historical timeline. */
    public void shiftTimeline(java.time.Duration back) {
        this.createdAt = createdAt.minus(back);
    }

    public Long getId() {
        return id;
    }

    public User getRecipient() {
        return recipient;
    }

    public NotificationChannel getChannel() {
        return channel;
    }

    public String getDestination() {
        return destination;
    }

    public String getSubject() {
        return subject;
    }

    public String getMessage() {
        return message;
    }

    public String getIncidentCode() {
        return incidentCode;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean isRead() {
        return readFlag;
    }
}
