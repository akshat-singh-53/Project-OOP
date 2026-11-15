package com.itsm.exception;

import org.springframework.http.HttpStatus;

public class TechnicianUnavailableException extends ItsmException {

    public TechnicianUnavailableException(String message) {
        super(HttpStatus.CONFLICT, "TECHNICIAN_UNAVAILABLE", message);
    }
}
