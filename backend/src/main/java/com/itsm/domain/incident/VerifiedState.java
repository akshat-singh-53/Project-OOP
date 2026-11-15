package com.itsm.domain.incident;

import java.util.EnumSet;
import java.util.Set;

final class VerifiedState extends AbstractIncidentState {

    @Override
    public IncidentStatus status() {
        return IncidentStatus.VERIFIED;
    }

    @Override
    public Set<IncidentAction> allowedActions() {
        return EnumSet.of(IncidentAction.CLOSE);
    }

    @Override
    public void close(Incident incident) {
        incident.transitionTo(IncidentStatus.CLOSED);
    }
}
