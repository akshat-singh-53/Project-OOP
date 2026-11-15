package com.itsm.domain.incident;

import java.util.Set;

/**
 * State pattern contract. Every lifecycle operation is declared here; each concrete state
 * implements only the operations that are legal from that state and rejects the rest.
 */
public interface IncidentState {

    IncidentStatus status();

    Set<IncidentAction> allowedActions();

    default boolean allows(IncidentAction action) {
        return allowedActions().contains(action);
    }

    void classify(Incident incident);

    void assign(Incident incident);

    void acknowledge(Incident incident);

    void startWork(Incident incident);

    void requestInfo(Incident incident);

    void provideInfo(Incident incident);

    void resolve(Incident incident);

    void verify(Incident incident);

    void reopen(Incident incident);

    void close(Incident incident);

    void markDuplicate(Incident incident);
}
