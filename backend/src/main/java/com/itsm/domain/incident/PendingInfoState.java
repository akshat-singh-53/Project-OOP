package com.itsm.domain.incident;

import java.util.EnumSet;
import java.util.Set;

final class PendingInfoState extends OpenIncidentState {

    @Override
    public IncidentStatus status() {
        return IncidentStatus.PENDING_INFO;
    }

    @Override
    public Set<IncidentAction> allowedActions() {
        return EnumSet.of(IncidentAction.PROVIDE_INFO, IncidentAction.MARK_DUPLICATE);
    }

    @Override
    public void provideInfo(Incident incident) {
        incident.transitionTo(IncidentStatus.IN_PROGRESS);
    }
}
