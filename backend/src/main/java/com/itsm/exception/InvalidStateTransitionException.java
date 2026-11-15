package com.itsm.exception;

import com.itsm.domain.incident.IncidentAction;
import com.itsm.domain.incident.IncidentStatus;
import org.springframework.http.HttpStatus;

public class InvalidStateTransitionException extends ItsmException {

    private final IncidentStatus currentStatus;
    private final IncidentAction attemptedAction;

    public InvalidStateTransitionException(IncidentStatus currentStatus, IncidentAction attemptedAction) {
        super(HttpStatus.CONFLICT, "INVALID_STATE_TRANSITION",
                "Cannot " + attemptedAction.name().toLowerCase().replace('_', ' ')
                        + " an incident that is " + currentStatus.name());
        this.currentStatus = currentStatus;
        this.attemptedAction = attemptedAction;
    }

    public IncidentStatus getCurrentStatus() {
        return currentStatus;
    }

    public IncidentAction getAttemptedAction() {
        return attemptedAction;
    }
}
