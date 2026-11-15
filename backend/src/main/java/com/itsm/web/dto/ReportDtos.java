package com.itsm.web.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public final class ReportDtos {

    private ReportDtos() {
    }

    public record Overview(long totalIncidents, long openIncidents, long criticalOpen, long resolvedIncidents,
                           long closedIncidents, long unassigned, long escalated, long slaViolations,
                           double slaCompliancePercent, double avgResolutionMinutes, double avgFeedback,
                           Map<String, Long> byStatus, Map<String, Long> byPriority, List<NamedCount> byCategory,
                           List<ServiceCount> byService, List<TechnicianLoad> technicianWorkload,
                           List<DailyCount> trend, Map<String, Double> avgResolutionByPriority) {
    }

    public record NamedCount(String name, long count) {
    }

    public record ServiceCount(String name, int criticality, long total, long open, long breached) {
    }

    public record TechnicianLoad(Long id, String code, String name, String level, boolean available, int active,
                                 int max, long resolved, double avgRating) {
    }

    public record DailyCount(LocalDate date, long created, long resolved) {
    }
}
