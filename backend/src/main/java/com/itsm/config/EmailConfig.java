package com.itsm.config;

import com.itsm.notification.EmailGateway;
import com.itsm.notification.SimulatedEmailGateway;
import com.itsm.notification.SmtpEmailGateway;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

/** Chooses real SMTP delivery when mail credentials are configured, otherwise the simulated gateway. */
@Configuration
public class EmailConfig {

    @Bean
    public EmailGateway emailGateway(ObjectProvider<JavaMailSender> mailSender,
                                     @Value("${spring.mail.username:}") String username,
                                     @Value("${spring.mail.password:}") String password,
                                     @Value("${itsm.mail.redirect-to:}") String redirectTo) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null || username.isBlank() || password.isBlank()) {
            return new SimulatedEmailGateway();
        }
        return new SmtpEmailGateway(sender, username.trim(), redirectTo);
    }
}
