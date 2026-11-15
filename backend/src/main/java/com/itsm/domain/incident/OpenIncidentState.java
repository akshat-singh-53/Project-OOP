package com.itsm.domain.incident;

/** Shared behaviour for states in which the support team is still working on the incident. */
abstract class OpenIncidentState extends AbstractIncidentState {

    @Override
    public void markDuplicate(Incident incident) {
        incident.transitionTo(IncidentStatus.CLOSED);
    }
}
