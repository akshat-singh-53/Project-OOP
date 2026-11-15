package com.itsm.exception;

import com.itsm.domain.incident.Priority;
import org.springframework.http.HttpStatus;

public class SlaConfigurationMissingException extends ItsmException {

    public SlaConfigurationMissingException(Priority priority, String serviceName) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "SLA_CONFIGURATION_MISSING",
                "No SLA policy configured for priority " + priority + " on service " + serviceName);
    }
}
