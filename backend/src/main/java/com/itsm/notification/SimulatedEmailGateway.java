package com.itsm.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Writes e-mails to the log. Used whenever no SMTP account is configured. */
public class SimulatedEmailGateway implements EmailGateway {

    private static final Logger log = LoggerFactory.getLogger(SimulatedEmailGateway.class);

    @Override
    public void sendEmail(String to, String subject, String body) {
        log.info("[EMAIL] to={} subject=\"{}\"", to, subject);
    }

    @Override
    public String description() {
        return "Simulated: e-mails are written to the server log and the notification outbox";
    }
}
