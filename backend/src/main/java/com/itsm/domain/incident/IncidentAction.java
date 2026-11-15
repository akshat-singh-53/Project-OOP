package com.itsm.domain.incident;

/** Every operation that can move an incident through its lifecycle. */
public enum IncidentAction {
    CLASSIFY,
    ASSIGN,
    ACKNOWLEDGE,
    START_WORK,
    REQUEST_INFO,
    PROVIDE_INFO,
    RESOLVE,
    VERIFY,
    REOPEN,
    CLOSE,
    MARK_DUPLICATE
}
