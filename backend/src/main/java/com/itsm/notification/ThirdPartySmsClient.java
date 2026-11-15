package com.itsm.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stand-in for a vendor SDK (e.g. Twilio, MSG91) whose API does not match our {@link SmsGateway}
 * interface: it wants an international MSISDN, a sender id and returns a numeric status code.
 */
public class ThirdPartySmsClient {

    private static final Logger log = LoggerFactory.getLogger(ThirdPartySmsClient.class);

    public static final int STATUS_ACCEPTED = 202;

    private final String senderId;

    public ThirdPartySmsClient(String senderId) {
        this.senderId = senderId;
    }

    public int pushMessage(String msisdn, String senderId, String payload) {
        log.info("[SMS] from={} to={} text=\"{}\"", senderId, msisdn, payload);
        return STATUS_ACCEPTED;
    }

    public String getSenderId() {
        return senderId;
    }
}
