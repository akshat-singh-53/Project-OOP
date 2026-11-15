package com.itsm.exception;

import org.springframework.http.HttpStatus;

public class WorkloadExceededException extends ItsmException {

    public WorkloadExceededException(String technicianName, int current, int max) {
        super(HttpStatus.CONFLICT, "WORKLOAD_EXCEEDED",
                technicianName + " already has " + current + " active incidents (limit " + max + ")");
    }
}
