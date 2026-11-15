package com.itsm.domain.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** History entry. Business code never edits audit records. */
@Entity
@Table(name = "audit_records", indexes = @Index(columnList = "incidentCode"))
public class AuditRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false, length = 60)
    private String actor;

    @Column(nullable = false, length = 60)
    private String action;

    @Column(length = 20)
    private String incidentCode;

    @Column(length = 30)
    private String previousState;

    @Column(length = 30)
    private String newState;

    @Column(length = 1000)
    private String description;

    protected AuditRecord() {
    }

    public AuditRecord(LocalDateTime timestamp, String actor, String action, String incidentCode,
                       String previousState, String newState, String description) {
        this.timestamp = timestamp;
        this.actor = actor;
        this.action = action;
        this.incidentCode = incidentCode;
        this.previousState = previousState;
        this.newState = newState;
        this.description = description;
    }

    /** Used only by the demo-data seeder to build a realistic historical timeline. */
    public void shiftTimeline(java.time.Duration back) {
        this.timestamp = timestamp.minus(back);
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getActor() {
        return actor;
    }

    public String getAction() {
        return action;
    }

    public String getIncidentCode() {
        return incidentCode;
    }

    public String getPreviousState() {
        return previousState;
    }

    public String getNewState() {
        return newState;
    }

    public String getDescription() {
        return description;
    }
}
