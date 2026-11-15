package com.itsm.domain.incident;

import java.util.EnumSet;
import java.util.Set;

final class NewState extends OpenIncidentState {

    @Override
    public IncidentStatus status() {
        return IncidentStatus.NEW;
    }

    @Override
    public Set<IncidentAction> allowedActions() {
        return EnumSet.of(IncidentAction.CLASSIFY, IncidentAction.MARK_DUPLICATE);
    }

    @Override
    public void classify(Incident incident) {
        incident.transitionTo(IncidentStatus.CLASSIFIED);
    }
}
