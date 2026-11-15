package com.itsm.exception;

public class ServiceNotFoundException extends ResourceNotFoundException {

    public ServiceNotFoundException(Object id) {
        super("SERVICE_NOT_FOUND", "IT service", id);
    }
}
