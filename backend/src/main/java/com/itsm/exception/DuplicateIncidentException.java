package com.itsm.exception;

import org.springframework.http.HttpStatus;

public class DuplicateIncidentException extends ItsmException {

    public DuplicateIncidentException(String existingCode) {
        super(HttpStatus.CONFLICT, "DUPLICATE_INCIDENT",
                "You already reported this problem a few minutes ago as " + existingCode);
    }
}
