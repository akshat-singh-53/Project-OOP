package com.itsm.domain.incident;

import java.util.EnumSet;
import java.util.Set;

final class AcknowledgedState extends OpenIncidentState {

    @Override
    public IncidentStatus status() {
        return IncidentStatus.ACKNOWLEDGED;
    }

    @Override
    public Set<IncidentAction> allowedActions() {
        return EnumSet.of(IncidentAction.START_WORK, IncidentAction.ASSIGN, IncidentAction.MARK_DUPLICATE);
    }

    @Override
    public void startWork(Incident incident) {
        incident.transitionTo(IncidentStatus.IN_PROGRESS);
    }

    @Override
    public void assign(Incident incident) {
        incident.transitionTo(IncidentStatus.ASSIGNED);
    }
}
