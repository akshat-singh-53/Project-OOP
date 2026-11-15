package com.itsm.domain.incident;

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

import java.time.Duration;
import java.time.LocalDateTime;

@Entity
@Table(name = "incident_notes")
public class IncidentNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Incident incident;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    private User author;

    @Enumerated(EnumType.STRING)
    private NoteType type;

    @Column(nullable = false, length = 4000)
    private String text;

    private LocalDateTime createdAt;

    protected IncidentNote() {
    }

    IncidentNote(Incident incident, User author, NoteType type, String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Note text cannot be empty");
        }
        this.incident = incident;
        this.author = author;
        this.type = type;
        this.text = text.trim();
        this.createdAt = LocalDateTime.now();
    }

    void shiftTimeline(Duration back) {
        createdAt = createdAt.minus(back);
    }

    public Long getId() {
        return id;
    }

    public User getAuthor() {
        return author;
    }

    public NoteType getType() {
        return type;
    }

    public String getText() {
        return text;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean isInternal() {
        return type.isInternal();
    }
}
