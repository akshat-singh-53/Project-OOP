package com.itsm.domain.incident;

import java.util.EnumSet;
import java.util.Set;

final class ResolvedState extends AbstractIncidentState {

    @Override
    public IncidentStatus status() {
        return IncidentStatus.RESOLVED;
    }

    @Override
    public Set<IncidentAction> allowedActions() {
        return EnumSet.of(IncidentAction.VERIFY, IncidentAction.REOPEN, IncidentAction.CLOSE);
    }

    @Override
    public void verify(Incident incident) {
        incident.transitionTo(IncidentStatus.VERIFIED);
    }

    @Override
    public void reopen(Incident incident) {
        incident.transitionTo(IncidentStatus.IN_PROGRESS);
    }

    /** Allowed so that unverified resolutions can be auto-closed after the confirmation window. */
    @Override
    public void close(Incident incident) {
        incident.transitionTo(IncidentStatus.CLOSED);
    }
}
