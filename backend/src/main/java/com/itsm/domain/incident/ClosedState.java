package com.itsm.domain.incident;

import java.util.EnumSet;
import java.util.Set;

/** Terminal state: every operation is rejected by the inherited defaults. */
final class ClosedState extends AbstractIncidentState {

    @Override
    public IncidentStatus status() {
        return IncidentStatus.CLOSED;
    }

    @Override
    public Set<IncidentAction> allowedActions() {
        return EnumSet.noneOf(IncidentAction.class);
    }
}
