package com.itsm.event;

import com.itsm.domain.incident.Incident;
import com.itsm.domain.incident.Priority;
import com.itsm.domain.notification.NotificationChannel;
import com.itsm.domain.user.TechnicianLevel;
import com.itsm.domain.user.User;
import com.itsm.notification.NotificationMessage;
import com.itsm.repository.AdministratorRepository;
import com.itsm.repository.TechnicianRepository;
import com.itsm.service.NotificationService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** Observer that turns incident events into notifications for the right people and channels. */
@Component
public class NotificationListener implements IncidentEventListener {

    private static final Set<NotificationChannel> APP = EnumSet.of(NotificationChannel.IN_APP);
    private static final Set<NotificationChannel> APP_EMAIL = EnumSet.of(NotificationChannel.IN_APP,
            NotificationChannel.EMAIL);
    private static final Set<NotificationChannel> APP_SMS = EnumSet.of(NotificationChannel.IN_APP,
            NotificationChannel.SMS);
    private static final Set<NotificationChannel> ALL = EnumSet.allOf(NotificationChannel.class);

    private final NotificationService notificationService;
    private final TechnicianRepository technicians;
    private final AdministratorRepository administrators;

    public NotificationListener(NotificationService notificationService, TechnicianRepository technicians,
                                AdministratorRepository administrators) {
        this.notificationService = notificationService;
        this.technicians = technicians;
        this.administrators = administrators;
    }

    @Override
    public void onIncidentEvent(IncidentEvent event) {
        Incident incident = event.incident();
        String code = incident.getCode();
        User reporter = incident.getReporter();
        User tech = incident.getAssignedTechnician();
        boolean critical = incident.getPriority() == Priority.P1;

        switch (event.type()) {
            case CREATED -> send(List.of(reporter), APP_EMAIL, code,
                    "We received your incident " + code,
                    "Your incident \"" + incident.getTitle() + "\" has been logged. We'll keep you updated.");
            case ASSIGNED -> {
                send(listOf(tech), critical ? ALL : APP_EMAIL, code,
                        "New " + incident.getPriority() + " incident assigned: " + code,
                        incident.getTitle() + " (" + incident.getService().getName() + "). Respond by "
                                + incident.getResponseDueAt());
                send(List.of(reporter), APP, code, code + " assigned to " + nameOf(tech),
                        "A technician is now responsible for your incident.");
            }
            case ASSIGNMENT_FAILED -> send(administrators.findByActiveTrue(), APP_EMAIL, code,
                    "Manual assignment needed for " + code, event.description());
            case ACKNOWLEDGED, WORK_STARTED -> send(List.of(reporter), APP, code,
                    code + ": " + event.type().getLabel(), nameOf(tech) + " is working on your incident.");
            case PRIORITY_CHANGED, CATEGORY_CHANGED -> send(listOf(reporter, tech), APP, code,
                    code + ": " + event.type().getLabel(), event.description());
            case INFO_REQUESTED -> send(List.of(reporter), APP_EMAIL, code,
                    "Action needed: more information requested on " + code, event.description());
            case INFO_PROVIDED -> send(listOf(tech), APP, code,
                    code + ": reporter responded", event.description());
            case NOTE_ADDED -> {
                if (event.actor().equals(reporter.getUsername())) {
                    send(listOf(tech), APP, code, code + ": new comment from reporter", event.description());
                }
            }
            case RESOLVED -> send(List.of(reporter), APP_EMAIL, code, code + " has been resolved",
                    "Please verify the resolution or reopen the incident if the problem persists.");
            case VERIFIED, FEEDBACK_RECEIVED -> send(listOf(tech), APP, code,
                    code + ": " + event.type().getLabel(), event.description());
            case REOPENED -> send(listOf(tech), APP_EMAIL, code, code + " was reopened by the reporter",
                    event.description());
            case CLOSED, MARKED_DUPLICATE -> send(List.of(reporter), APP_EMAIL, code, code + " is closed",
                    event.description());
            case DUPLICATE_SUSPECTED -> send(listOf(tech), APP, code, code + ": possible duplicate",
                    event.description());
            case SLA_WARNING -> {
                send(listOf(tech), APP_EMAIL, code, "SLA approaching on " + code, event.description());
                send(technicians.findByActiveTrueAndLevel(TechnicianLevel.TEAM_LEAD), APP, code,
                        "SLA risk on " + code, event.description());
            }
            case SLA_VIOLATED -> {
                send(listOf(tech), APP_SMS, code, "SLA VIOLATED on " + code, event.description());
                send(administrators.findByActiveTrue(), APP_EMAIL, code, "SLA violated: " + code,
                        event.description());
            }
            case ESCALATED -> notifyEscalation(incident, event);
            default -> {
                // CLASSIFIED, PRIORITY_CALCULATED, SLA_APPLIED: audit only
            }
        }
    }

    private void notifyEscalation(Incident incident, IncidentEvent event) {
        String code = incident.getCode();
        String subject = "ESCALATION L" + incident.getEscalationLevel() + ": " + code + " not acknowledged";
        switch (incident.getEscalationLevel()) {
            case 1 -> send(technicians.findByActiveTrueAndLevel(TechnicianLevel.TEAM_LEAD), APP_SMS, code,
                    subject, event.description());
            case 2 -> send(administrators.findByActiveTrue(), ALL, code, subject, event.description());
            default -> send(administrators.findByActiveTrueAndItManagerTrue(), ALL, code, subject,
                    event.description());
        }
    }

    private void send(List<? extends User> to, Set<NotificationChannel> channels, String code, String subject,
                      String body) {
        notificationService.notify(to, channels, new NotificationMessage(subject, body, code));
    }

    private static List<User> listOf(User... users) {
        List<User> list = new ArrayList<>();
        for (User u : users) {
            if (u != null) {
                list.add(u);
            }
        }
        return list;
    }

    private static String nameOf(User user) {
        return user == null ? "Support team" : user.getFullName();
    }
}
