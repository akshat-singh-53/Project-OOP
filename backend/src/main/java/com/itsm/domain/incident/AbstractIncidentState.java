package com.itsm.domain.incident;

import com.itsm.exception.InvalidStateTransitionException;

/** By default every operation is illegal; concrete states override what they permit. */
abstract class AbstractIncidentState implements IncidentState {

    protected final void reject(IncidentAction action) {
        throw new InvalidStateTransitionException(status(), action);
    }

    @Override
    public void classify(Incident incident) {
        reject(IncidentAction.CLASSIFY);
    }

    @Override
    public void assign(Incident incident) {
        reject(IncidentAction.ASSIGN);
    }

    @Override
    public void acknowledge(Incident incident) {
        reject(IncidentAction.ACKNOWLEDGE);
    }

    @Override
    public void startWork(Incident incident) {
        reject(IncidentAction.START_WORK);
    }

    @Override
    public void requestInfo(Incident incident) {
        reject(IncidentAction.REQUEST_INFO);
    }

    @Override
    public void provideInfo(Incident incident) {
        reject(IncidentAction.PROVIDE_INFO);
    }

    @Override
    public void resolve(Incident incident) {
        reject(IncidentAction.RESOLVE);
    }

    @Override
    public void verify(Incident incident) {
        reject(IncidentAction.VERIFY);
    }

    @Override
    public void reopen(Incident incident) {
        reject(IncidentAction.REOPEN);
    }

    @Override
    public void close(Incident incident) {
        reject(IncidentAction.CLOSE);
    }

    @Override
    public void markDuplicate(Incident incident) {
        reject(IncidentAction.MARK_DUPLICATE);
    }

    @Override
    public String toString() {
        return status().name();
    }
}
