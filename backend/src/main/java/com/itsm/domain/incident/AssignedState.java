package com.itsm.domain.incident;

import java.util.EnumSet;
import java.util.Set;

final class AssignedState extends OpenIncidentState {

    @Override
    public IncidentStatus status() {
        return IncidentStatus.ASSIGNED;
    }

    @Override
    public Set<IncidentAction> allowedActions() {
        return EnumSet.of(IncidentAction.ACKNOWLEDGE, IncidentAction.ASSIGN, IncidentAction.MARK_DUPLICATE);
    }

    @Override
    public void assign(Incident incident) {
        // reassignment to another technician
    }

    @Override
    public void acknowledge(Incident incident) {
        incident.transitionTo(IncidentStatus.ACKNOWLEDGED);
    }
}
