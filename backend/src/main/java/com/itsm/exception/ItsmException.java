package com.itsm.exception;

import org.springframework.http.HttpStatus;

/** Root of all business exceptions. Each subtype knows which HTTP status it maps to. */
public abstract class ItsmException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;

    protected ItsmException(HttpStatus status, String errorCode, String message) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    protected ItsmException(HttpStatus status, String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.errorCode = errorCode;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
