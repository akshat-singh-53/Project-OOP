package com.itsm.notification;

/** Target interface our application expects for sending text messages. */
public interface SmsGateway {

    void sendSms(String phoneNumber, String text);
}
