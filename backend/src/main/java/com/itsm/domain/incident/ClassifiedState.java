package com.itsm.domain.incident;

import java.util.EnumSet;
import java.util.Set;

final class ClassifiedState extends OpenIncidentState {

    @Override
    public IncidentStatus status() {
        return IncidentStatus.CLASSIFIED;
    }

    @Override
    public Set<IncidentAction> allowedActions() {
        return EnumSet.of(IncidentAction.CLASSIFY, IncidentAction.ASSIGN, IncidentAction.MARK_DUPLICATE);
    }

    @Override
    public void classify(Incident incident) {
        // re-classification keeps the incident in the same state
    }

    @Override
    public void assign(Incident incident) {
        incident.transitionTo(IncidentStatus.ASSIGNED);
    }
}
