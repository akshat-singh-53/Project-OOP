package com.itsm.intelligence;

import java.time.LocalDateTime;

public record SlaRiskAssessment(String incidentCode,
                                String title,
                                String priority,
                                String status,
                                String technicianName,
                                LocalDateTime resolutionDueAt,
                                long minutesRemaining,
                                long expectedMinutesToResolve,
                                int riskScore,
                                String riskLevel,
                                String explanation) {
}
