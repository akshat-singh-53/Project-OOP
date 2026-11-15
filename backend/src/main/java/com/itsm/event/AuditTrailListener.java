package com.itsm.event;

import com.itsm.service.AuditService;
import org.springframework.stereotype.Component;

/** Observer that writes every incident event to the audit trail. */
@Component
public class AuditTrailListener implements IncidentEventListener {

    private final AuditService auditService;

    public AuditTrailListener(AuditService auditService) {
        this.auditService = auditService;
    }

    @Override
    public void onIncidentEvent(IncidentEvent event) {
        String previous = event.statusChanged() ? event.previousStatus().name() : null;
        String next = event.newStatus() == null ? null : event.newStatus().name();
        auditService.record(event.occurredAt(), event.actor(), event.type().name(), event.incident().getCode(),
                previous, next, event.description());
    }
}
