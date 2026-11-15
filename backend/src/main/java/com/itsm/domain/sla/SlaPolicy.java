package com.itsm.domain.sla;

import com.itsm.domain.catalog.ItService;
import com.itsm.domain.incident.Priority;
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

/**
 * Response and resolution targets for a priority. A policy without a service is the
 * organisation-wide default; a policy bound to a service overrides the default for it.
 */
@Entity
@Table(name = "sla_policies")
public class SlaPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority;

    @ManyToOne(fetch = FetchType.EAGER)
    private ItService service;

    private int responseMinutes;

    private int resolutionMinutes;

    /** Percentage of elapsed time after which an "SLA approaching" warning is raised. */
    private int warningThresholdPercent = 75;

    private boolean active = true;

    protected SlaPolicy() {
    }

    public SlaPolicy(String name, Priority priority, ItService service, int responseMinutes,
                     int resolutionMinutes, int warningThresholdPercent) {
        this.name = name;
        this.priority = priority;
        this.service = service;
        setTargets(responseMinutes, resolutionMinutes, warningThresholdPercent);
    }

    public void update(String name, int responseMinutes, int resolutionMinutes, int warningThresholdPercent,
                       boolean active) {
        this.name = name;
        setTargets(responseMinutes, resolutionMinutes, warningThresholdPercent);
        this.active = active;
    }

    private void setTargets(int response, int resolution, int warningPercent) {
        if (response <= 0 || resolution <= 0) {
            throw new IllegalArgumentException("SLA targets must be positive");
        }
        if (response > resolution) {
            throw new IllegalArgumentException("Response target cannot exceed resolution target");
        }
        if (warningPercent < 10 || warningPercent > 99) {
            throw new IllegalArgumentException("Warning threshold must be between 10 and 99 percent");
        }
        this.responseMinutes = response;
        this.resolutionMinutes = resolution;
        this.warningThresholdPercent = warningPercent;
    }

    public boolean isDefaultPolicy() {
        return service == null;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Priority getPriority() {
        return priority;
    }

    public ItService getService() {
        return service;
    }

    public int getResponseMinutes() {
        return responseMinutes;
    }

    public int getResolutionMinutes() {
        return resolutionMinutes;
    }

    public int getWarningThresholdPercent() {
        return warningThresholdPercent;
    }

    public boolean isActive() {
        return active;
    }
}
