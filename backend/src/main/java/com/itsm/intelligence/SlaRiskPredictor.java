package com.itsm.intelligence;

import com.itsm.domain.incident.Incident;
import com.itsm.domain.incident.IncidentStatus;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Predicts whether an open incident will miss its resolution deadline.
 * Expected remaining effort is estimated from the historical average resolution time for
 * similar incidents, inflated by the technician's current workload, and compared with the
 * time left before the SLA deadline.
 */
@Component
public class SlaRiskPredictor {

    private static final double WORKLOAD_PENALTY_PER_INCIDENT = 0.15;
    private static final double UNASSIGNED_PENALTY = 1.5;

    public SlaRiskAssessment assess(Incident incident, double historicalAvgMinutes, int technicianWorkload,
                                    LocalDateTime now) {
        long remaining = Duration.between(now, incident.getResolutionDueAt()).toMinutes();
        long elapsed = Math.max(0, Duration.between(incident.getCreatedAt(), now).toMinutes());
        double baseline = historicalAvgMinutes > 0 ? historicalAvgMinutes : incident.getResolutionTargetMinutes() * 0.6;

        double expected = Math.max(baseline - elapsed, baseline * 0.2);
        if (incident.getAssignedTechnician() == null) {
            expected *= UNASSIGNED_PENALTY;
        } else {
            expected *= 1 + WORKLOAD_PENALTY_PER_INCIDENT * Math.max(0, technicianWorkload - 1);
        }
        if (incident.getStatus() == IncidentStatus.PENDING_INFO) {
            expected *= 1.2;
        }

        int risk;
        String explanation;
        if (remaining <= 0) {
            risk = 100;
            explanation = "Resolution deadline already passed";
        } else {
            double elapsedFraction = (double) elapsed / Math.max(1, incident.getResolutionTargetMinutes());
            double pressure = expected / remaining;
            risk = (int) Math.round(Math.min(100, 70 * Math.min(pressure, 1.3) / 1.3 + 30 * Math.min(elapsedFraction, 1)));
            explanation = String.format("~%d min of work expected vs %d min left (%.0f%% of SLA used)",
                    Math.round(expected), remaining, Math.min(elapsedFraction, 1) * 100);
        }
        String level = risk >= 70 ? "HIGH" : risk >= 40 ? "MEDIUM" : "LOW";
        return new SlaRiskAssessment(incident.getCode(), incident.getTitle(),
                incident.getPriority() == null ? null : incident.getPriority().name(), incident.getStatus().name(),
                incident.getAssignedTechnician() == null ? null : incident.getAssignedTechnician().getFullName(),
                incident.getResolutionDueAt(), remaining, Math.round(expected), risk, level, explanation);
    }
}
