package com.itsm.domain.sla;

import com.itsm.domain.incident.Incident;
import com.itsm.domain.incident.Priority;
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

@Entity
@Table(name = "sla_violations")
public class SlaViolation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    private Incident incident;

    @Enumerated(EnumType.STRING)
    private SlaViolationType type;

    @Enumerated(EnumType.STRING)
    private Priority priority;

    private LocalDateTime dueAt;

    private LocalDateTime detectedAt;

    private String technicianName;

    protected SlaViolation() {
    }

    public SlaViolation(Incident incident, SlaViolationType type, LocalDateTime dueAt, LocalDateTime detectedAt) {
        this.incident = incident;
        this.type = type;
        this.priority = incident.getPriority();
        this.dueAt = dueAt;
        this.detectedAt = detectedAt;
        this.technicianName = incident.getAssignedTechnician() == null
                ? null : incident.getAssignedTechnician().getFullName();
    }

    public Long getId() {
        return id;
    }

    public Incident getIncident() {
        return incident;
    }

    public SlaViolationType getType() {
        return type;
    }

    public Priority getPriority() {
        return priority;
    }

    public LocalDateTime getDueAt() {
        return dueAt;
    }

    public LocalDateTime getDetectedAt() {
        return detectedAt;
    }

    public String getTechnicianName() {
        return technicianName;
    }
}
