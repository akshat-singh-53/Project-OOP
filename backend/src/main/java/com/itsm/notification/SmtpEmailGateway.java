package com.itsm.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Sends real e-mail over SMTP (e.g. Gmail). Delivery runs on a background thread so a slow
 * mail server never blocks an incident operation; failures are logged, not propagated.
 */
public class SmtpEmailGateway implements EmailGateway {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailGateway.class);

    /** Seeded demo accounts use this non-routable domain; mailing them would only bounce. */
    static final String DEMO_DOMAIN = ".local";

    private static final String FOOTER = "\n\n--\nI-ITSM Service Desk. This is an automated message, please do not reply.";

    private final JavaMailSender mailSender;
    private final String from;
    private final String redirectTo;
    private final Executor executor;

    public SmtpEmailGateway(JavaMailSender mailSender, String from, String redirectTo) {
        this(mailSender, from, redirectTo, Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "smtp-sender");
            thread.setDaemon(true);
            return thread;
        }));
    }

    SmtpEmailGateway(JavaMailSender mailSender, String from, String redirectTo, Executor executor) {
        this.mailSender = mailSender;
        this.from = from;
        this.redirectTo = redirectTo == null ? "" : redirectTo.trim();
        this.executor = executor;
    }

    @Override
    public void sendEmail(String to, String subject, String body) {
        boolean redirected = !redirectTo.isEmpty();
        String recipient = redirected ? redirectTo : to;
        if (recipient.toLowerCase(Locale.ROOT).endsWith(DEMO_DOMAIN)) {
            log.info("[EMAIL skipped: demo address] to={} subject=\"{}\"", to, subject);
            return;
        }
        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(from);
        mail.setTo(recipient);
        mail.setSubject(redirected ? "[I-ITSM] " + subject + " (for " + to + ")" : "[I-ITSM] " + subject);
        mail.setText(body + FOOTER);
        executor.execute(() -> {
            try {
                mailSender.send(mail);
                log.info("[EMAIL sent] to={} subject=\"{}\"", recipient, subject);
            } catch (MailException ex) {
                log.warn("[EMAIL failed] to={} subject=\"{}\": {}", recipient, subject, ex.getMessage());
            }
        });
    }

    @Override
    public String description() {
        return redirectTo.isEmpty()
                ? "Real e-mail via SMTP from " + from
                : "Real e-mail via SMTP from " + from + ", all messages redirected to " + redirectTo;
    }
}
