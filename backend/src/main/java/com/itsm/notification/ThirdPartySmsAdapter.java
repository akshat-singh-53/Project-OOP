package com.itsm.notification;

import com.itsm.exception.ExternalServiceException;
import org.springframework.stereotype.Component;

/** Adapter pattern: lets the vendor SMS client be used wherever an {@link SmsGateway} is expected. */
@Component
public class ThirdPartySmsAdapter implements SmsGateway {

    private static final int MAX_SMS_LENGTH = 160;

    private final ThirdPartySmsClient client = new ThirdPartySmsClient("ITSMDESK");

    @Override
    public void sendSms(String phoneNumber, String text) {
        String msisdn = toMsisdn(phoneNumber);
        String payload = text.length() > MAX_SMS_LENGTH ? text.substring(0, MAX_SMS_LENGTH - 3) + "..." : text;
        int status = client.pushMessage(msisdn, client.getSenderId(), payload);
        if (status != ThirdPartySmsClient.STATUS_ACCEPTED) {
            throw new ExternalServiceException("SMS gateway", new IllegalStateException("status " + status));
        }
    }

    /** Converts local 10-digit numbers to the +91 international format the vendor requires. */
    static String toMsisdn(String phone) {
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.length() == 10) {
            return "+91" + digits;
        }
        return "+" + digits;
    }
}
