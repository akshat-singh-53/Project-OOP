package com.itsm.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends ItsmException {

    public ResourceNotFoundException(String resource, Object id) {
        this("NOT_FOUND", resource, id);
    }

    protected ResourceNotFoundException(String code, String resource, Object id) {
        super(HttpStatus.NOT_FOUND, code, resource + " not found: " + id);
    }
}
