package com.itsm.notification;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class SmtpEmailGatewayTest {

    private final JavaMailSender mailSender = mock(JavaMailSender.class);

    @Test
    @DisplayName("TC31 SMTP e-mail: real addresses are sent, demo .local addresses skipped, redirect and failures handled")
    void smtpGateway() {
        SmtpEmailGateway direct = new SmtpEmailGateway(mailSender, "desk@gmail.com", "", Runnable::run);
        direct.sendEmail("amit.sharma@itsm.local", "ignored", "body");
        verify(mailSender, never()).send(any(SimpleMailMessage.class));

        direct.sendEmail("student@gmail.com", "INC1001 assigned", "body");
        ArgumentCaptor<SimpleMailMessage> sent = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(sent.capture());
        assertThat(sent.getValue().getTo()).containsExactly("student@gmail.com");
        assertThat(sent.getValue().getFrom()).isEqualTo("desk@gmail.com");
        assertThat(sent.getValue().getSubject()).isEqualTo("[I-ITSM] INC1001 assigned");

        JavaMailSender redirectedSender = mock(JavaMailSender.class);
        new SmtpEmailGateway(redirectedSender, "desk@gmail.com", "demo@gmail.com", Runnable::run)
                .sendEmail("amit.sharma@itsm.local", "INC1002 resolved", "body");
        ArgumentCaptor<SimpleMailMessage> redirected = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(redirectedSender).send(redirected.capture());
        assertThat(redirected.getValue().getTo()).containsExactly("demo@gmail.com");
        assertThat(redirected.getValue().getSubject()).contains("amit.sharma@itsm.local");

        doThrow(new MailSendException("SMTP down")).when(mailSender).send(any(SimpleMailMessage.class));
        assertThatCode(() -> direct.sendEmail("student@gmail.com", "x", "y")).doesNotThrowAnyException();
    }
}
