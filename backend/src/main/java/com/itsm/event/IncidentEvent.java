package com.itsm.event;

import com.itsm.domain.incident.Incident;
import com.itsm.domain.incident.IncidentStatus;

import java.time.LocalDateTime;

/** Immutable message describing something that happened to an incident. */
public record IncidentEvent(IncidentEventType type,
                            Incident incident,
                            String actor,
                            IncidentStatus previousStatus,
                            IncidentStatus newStatus,
                            String description,
                            LocalDateTime occurredAt) {

    public static final String SYSTEM_ACTOR = "SYSTEM";

    public static IncidentEvent of(IncidentEventType type, Incident incident, String actor,
                                   IncidentStatus previousStatus, String description) {
        return new IncidentEvent(type, incident, actor, previousStatus, incident.getStatus(), description,
                LocalDateTime.now());
    }

    public boolean statusChanged() {
        return previousStatus != null && previousStatus != newStatus;
    }
}
