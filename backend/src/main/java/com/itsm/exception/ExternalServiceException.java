package com.itsm.exception;

import org.springframework.http.HttpStatus;

/** Raised when a network call to a third-party service (AI provider, SMS gateway) fails. */
public class ExternalServiceException extends ItsmException {

    public ExternalServiceException(String service, Throwable cause) {
        super(HttpStatus.BAD_GATEWAY, "EXTERNAL_SERVICE_FAILURE",
                service + " is currently unreachable: " + cause.getMessage(), cause);
    }
}
