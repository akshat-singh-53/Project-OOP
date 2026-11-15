package com.itsm.notification;

public interface EmailGateway {

    void sendEmail(String to, String subject, String body);

    /** Where e-mails actually go, shown to administrators on the settings page. */
    String description();
}
