package com.itsm.domain.incident;

public enum IncidentStatus {
    NEW,
    CLASSIFIED,
    ASSIGNED,
    ACKNOWLEDGED,
    IN_PROGRESS,
    PENDING_INFO,
    RESOLVED,
    VERIFIED,
    CLOSED;

    /** Open incidents still need work from the support team. */
    public boolean isOpen() {
        return this != RESOLVED && this != VERIFIED && this != CLOSED;
    }
}
