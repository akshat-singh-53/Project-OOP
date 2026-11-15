package com.itsm.domain.incident;

public enum NoteType {
    COMMENT(false),
    INVESTIGATION(true),
    INFO_REQUEST(false),
    INFO_RESPONSE(false),
    RESOLUTION(false);

    /** Internal notes are only visible to the support team. */
    private final boolean internal;

    NoteType(boolean internal) {
        this.internal = internal;
    }

    public boolean isInternal() {
        return internal;
    }
}
