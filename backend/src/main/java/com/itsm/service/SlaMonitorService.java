package com.itsm.service;

import com.itsm.domain.incident.Incident;
import com.itsm.domain.incident.IncidentStatus;
import com.itsm.domain.sla.SlaViolation;
import com.itsm.domain.sla.SlaViolationType;
import com.itsm.event.IncidentEvent;
import com.itsm.event.IncidentEventPublisher;
import com.itsm.event.IncidentEventType;
import com.itsm.repository.IncidentRepository;
import com.itsm.repository.SlaViolationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.EnumSet;

/**
 * Background job that continuously watches SLA deadlines: raises warnings before a deadline,
 * records violations, escalates unacknowledged urgent incidents and auto-closes stale resolutions.
 */
@Service
public class SlaMonitorService {

    private static final Logger log = LoggerFactory.getLogger(SlaMonitorService.class);

    public static final int MAX_ESCALATION_LEVEL = 3;
    private static final String[] ESCALATION_TARGETS = {"Technician", "Team Lead", "System Administrator", "IT Manager"};

    private final IncidentRepository incidents;
    private final SlaViolationRepository violations;
    private final IncidentEventPublisher events;
    private final SettingsService settings;
    private final Clock clock;
    private final TransactionTemplate transaction;

    public SlaMonitorService(IncidentRepository incidents, SlaViolationRepository violations,
                             IncidentEventPublisher events, SettingsService settings, Clock clock,
                             PlatformTransactionManager transactionManager) {
        this.incidents = incidents;
        this.violations = violations;
        this.events = events;
        this.settings = settings;
        this.clock = clock;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    public record MonitorReport(int checked, int warnings, int violations, int escalations, int autoClosed) {
    }

    @Scheduled(fixedDelayString = "${itsm.sla.monitor-interval-ms:30000}",
            initialDelayString = "${itsm.sla.monitor-initial-delay-ms:15000}")
    public void scheduledRun() {
        // A direct call to runOnce() bypasses the @Transactional proxy, so the incident flags would never be saved.
        MonitorReport report = transaction.execute(status -> runOnce());
        if (report.warnings() + report.violations() + report.escalations() + report.autoClosed() > 0) {
            log.info("SLA monitor: {}", report);
        }
    }

    @Transactional
    public MonitorReport runOnce() {
        LocalDateTime now = LocalDateTime.now(clock);
        int warnings = 0;
        int breaches = 0;
        int escalations = 0;
        var open = incidents.findByStatusIn(IntelligenceService.OPEN);
        for (Incident incident : open) {
            if (incident.getResponseDueAt() == null) {
                continue;
            }
            if (incident.getAcknowledgedAt() == null) {
                if (!incident.isResponseBreached() && now.isAfter(incident.getResponseDueAt())) {
                    incident.markResponseBreached();
                    recordViolation(incident, SlaViolationType.RESPONSE, incident.getResponseDueAt(), now);
                    breaches++;
                } else if (!incident.isResponseWarned() && !incident.isResponseBreached()
                        && elapsedPercent(incident.getCreatedAt(), incident.getResponseDueAt(), now) >= incident.getSlaWarningPercent()) {
                    incident.markResponseWarned();
                    publish(IncidentEventType.SLA_WARNING, incident, "Response deadline approaching ("
                            + minutesLeft(incident.getResponseDueAt(), now) + " min left)");
                    warnings++;
                }
            }
            if (!incident.isResolutionBreached() && now.isAfter(incident.getResolutionDueAt())) {
                incident.markResolutionBreached();
                recordViolation(incident, SlaViolationType.RESOLUTION, incident.getResolutionDueAt(), now);
                breaches++;
            } else if (!incident.isResolutionWarned() && !incident.isResolutionBreached()
                    && elapsedPercent(incident.getCreatedAt(), incident.getResolutionDueAt(), now) >= incident.getSlaWarningPercent()) {
                incident.markResolutionWarned();
                publish(IncidentEventType.SLA_WARNING, incident, "Resolution deadline approaching ("
                        + minutesLeft(incident.getResolutionDueAt(), now) + " min left)");
                warnings++;
            }
            if (shouldEscalate(incident, now)) {
                int level = incident.escalate();
                publish(IncidentEventType.ESCALATED, incident, "Not acknowledged "
                        + Duration.between(incident.getResponseDueAt(), now).toMinutes()
                        + " min after the response deadline - escalated to " + ESCALATION_TARGETS[level]);
                escalations++;
            }
        }
        int autoClosed = autoCloseStaleResolutions(now);
        return new MonitorReport(open.size(), warnings, breaches, escalations, autoClosed);
    }

    /** Escalate P1/P2 incidents nobody has acknowledged; one level per response window overdue. */
    private boolean shouldEscalate(Incident incident, LocalDateTime now) {
        if (!settings.getBoolean(SettingsService.ESCALATION_ENABLED)
                || incident.getAcknowledgedAt() != null
                || incident.getPriority() == null || !incident.getPriority().isUrgent()
                || incident.getEscalationLevel() >= MAX_ESCALATION_LEVEL) {
            return false;
        }
        long step = Math.max(1, incident.getResponseTargetMinutes());
        LocalDateTime threshold = incident.getResponseDueAt().plusMinutes(step * incident.getEscalationLevel());
        return now.isAfter(threshold);
    }

    private int autoCloseStaleResolutions(LocalDateTime now) {
        int hours = settings.getInt(SettingsService.AUTO_CLOSE_HOURS);
        if (hours <= 0) {
            return 0;
        }
        int closed = 0;
        for (Incident incident : incidents.findByStatusIn(EnumSet.of(IncidentStatus.RESOLVED))) {
            if (incident.getResolvedAt() != null && incident.getResolvedAt().plusHours(hours).isBefore(now)) {
                IncidentStatus previous = incident.getStatus();
                incident.close("Auto-closed: reporter did not respond within " + hours + " hours");
                events.publish(IncidentEvent.of(IncidentEventType.CLOSED, incident, IncidentEvent.SYSTEM_ACTOR,
                        previous, incident.getClosureNotes()));
                closed++;
            }
        }
        return closed;
    }

    private void recordViolation(Incident incident, SlaViolationType type, LocalDateTime dueAt, LocalDateTime now) {
        if (!violations.existsByIncidentAndType(incident, type)) {
            violations.save(new SlaViolation(incident, type, dueAt, now));
        }
        publish(IncidentEventType.SLA_VIOLATED, incident, type.name().charAt(0) + type.name().substring(1).toLowerCase()
                + " SLA violated: deadline was " + dueAt.withNano(0) + " (" + incident.getPriority() + ")");
    }

    private void publish(IncidentEventType type, Incident incident, String description) {
        events.publish(IncidentEvent.of(type, incident, IncidentEvent.SYSTEM_ACTOR, incident.getStatus(), description));
    }

    static double elapsedPercent(LocalDateTime start, LocalDateTime due, LocalDateTime now) {
        long total = Duration.between(start, due).toSeconds();
        if (total <= 0) {
            return 100;
        }
        return 100.0 * Duration.between(start, now).toSeconds() / total;
    }

    private static long minutesLeft(LocalDateTime due, LocalDateTime now) {
        return Math.max(0, Duration.between(now, due).toMinutes());
    }
}
