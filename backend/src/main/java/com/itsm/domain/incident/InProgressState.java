package com.itsm.domain.incident;

import java.util.EnumSet;
import java.util.Set;

final class InProgressState extends OpenIncidentState {

    @Override
    public IncidentStatus status() {
        return IncidentStatus.IN_PROGRESS;
    }

    @Override
    public Set<IncidentAction> allowedActions() {
        return EnumSet.of(IncidentAction.REQUEST_INFO, IncidentAction.RESOLVE, IncidentAction.ASSIGN,
                IncidentAction.MARK_DUPLICATE);
    }

    @Override
    public void requestInfo(Incident incident) {
        incident.transitionTo(IncidentStatus.PENDING_INFO);
    }

    @Override
    public void resolve(Incident incident) {
        incident.transitionTo(IncidentStatus.RESOLVED);
    }

    @Override
    public void assign(Incident incident) {
        incident.transitionTo(IncidentStatus.ASSIGNED);
    }
}
