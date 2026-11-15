package com.itsm.service;

import com.itsm.domain.catalog.ItService;
import com.itsm.domain.incident.Incident;
import com.itsm.domain.incident.IncidentStatus;
import com.itsm.domain.incident.Priority;
import com.itsm.domain.incident.SlaStatus;
import com.itsm.domain.user.Technician;
import com.itsm.domain.user.User;
import com.itsm.domain.user.Role;
import com.itsm.repository.IncidentRepository;
import com.itsm.repository.ItServiceRepository;
import com.itsm.repository.SlaViolationRepository;
import com.itsm.repository.TechnicianRepository;
import com.itsm.web.dto.ReportDtos.DailyCount;
import com.itsm.web.dto.ReportDtos.NamedCount;
import com.itsm.web.dto.ReportDtos.Overview;
import com.itsm.web.dto.ReportDtos.ServiceCount;
import com.itsm.web.dto.ReportDtos.TechnicianLoad;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private static final int TREND_DAYS = 14;

    private final IncidentRepository incidents;
    private final TechnicianRepository technicians;
    private final ItServiceRepository services;
    private final SlaViolationRepository violations;
    private final UserService userService;
    private final Clock clock;

    public ReportService(IncidentRepository incidents, TechnicianRepository technicians, ItServiceRepository services,
                         SlaViolationRepository violations, UserService userService, Clock clock) {
        this.incidents = incidents;
        this.technicians = technicians;
        this.services = services;
        this.violations = violations;
        this.userService = userService;
        this.clock = clock;
    }

    /** Dashboard for administrators (whole organisation) or scoped to one employee / technician. */
    @Transactional(readOnly = true)
    public Overview overview(User viewer) {
        Predicate<Incident> scope = switch (viewer.getRole()) {
            case EMPLOYEE -> i -> i.getReporter().getId().equals(viewer.getId());
            case TECHNICIAN -> i -> i.getAssignedTechnician() != null
                    && i.getAssignedTechnician().getId().equals(viewer.getId());
            case ADMIN -> i -> true;
        };
        List<Incident> all = incidents.findAll().stream().filter(scope).toList();
        List<Incident> solved = all.stream().filter(i -> i.getResolvedAt() != null).toList();

        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (IncidentStatus s : IncidentStatus.values()) {
            byStatus.put(s.name(), all.stream().filter(i -> i.getStatus() == s).count());
        }
        Map<String, Long> byPriority = new LinkedHashMap<>();
        Map<String, Double> mttr = new LinkedHashMap<>();
        for (Priority p : Priority.values()) {
            byPriority.put(p.name(), all.stream().filter(i -> i.getPriority() == p).count());
            mttr.put(p.name(), round(solved.stream().filter(i -> i.getPriority() == p)
                    .mapToLong(Incident::getResolutionMinutes).average().orElse(0)));
        }

        List<NamedCount> byCategory = all.stream().filter(i -> i.getCategory() != null)
                .collect(Collectors.groupingBy(i -> i.getCategory().getName(), Collectors.counting()))
                .entrySet().stream().map(e -> new NamedCount(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingLong(NamedCount::count).reversed()).toList();

        List<ServiceCount> byService = new ArrayList<>();
        for (ItService s : services.findAll()) {
            List<Incident> forService = all.stream().filter(i -> i.getService().getId().equals(s.getId())).toList();
            if (!forService.isEmpty() || viewer.getRole() == Role.ADMIN) {
                byService.add(new ServiceCount(s.getName(), s.getBusinessCriticality(), forService.size(),
                        forService.stream().filter(Incident::isOpen).count(),
                        forService.stream().filter(i -> i.getSlaStatus() == SlaStatus.BREACHED).count()));
            }
        }
        byService.sort(Comparator.comparingLong(ServiceCount::total).reversed());

        long breachedSolved = solved.stream().filter(i -> i.isResolutionBreached() || i.isResponseBreached()).count();
        double compliance = solved.isEmpty() ? 100 : 100.0 * (solved.size() - breachedSolved) / solved.size();

        return new Overview(
                all.size(),
                all.stream().filter(Incident::isOpen).count(),
                all.stream().filter(i -> i.isOpen() && i.getPriority() == Priority.P1).count(),
                solved.size(),
                all.stream().filter(i -> i.getStatus() == IncidentStatus.CLOSED).count(),
                all.stream().filter(i -> i.isOpen() && i.getAssignedTechnician() == null).count(),
                all.stream().filter(i -> i.getEscalationLevel() > 0).count(),
                viewer.getRole() == Role.ADMIN ? violations.count()
                        : all.stream().filter(i -> i.getSlaStatus() == SlaStatus.BREACHED).count(),
                round(compliance),
                round(solved.stream().mapToLong(Incident::getResolutionMinutes).average().orElse(0)),
                round(all.stream().map(Incident::getFeedbackRating).filter(Objects::nonNull)
                        .mapToInt(Integer::intValue).average().orElse(0)),
                byStatus, byPriority, byCategory, byService,
                viewer.getRole() == Role.EMPLOYEE ? List.of() : technicianWorkload(),
                trend(all), mttr);
    }

    @Transactional(readOnly = true)
    public List<TechnicianLoad> technicianWorkload() {
        Map<Long, Integer> active = userService.workloadByTechnician();
        Map<Long, Long> resolved = new HashMap<>();
        for (Object[] row : incidents.countResolvedByTechnician()) {
            resolved.put((Long) row[0], (Long) row[1]);
        }
        List<Incident> rated = incidents.findAll().stream()
                .filter(i -> i.getFeedbackRating() != null && i.getAssignedTechnician() != null).toList();
        return technicians.findByActiveTrueOrderByIdAsc().stream()
                .map((Technician t) -> new TechnicianLoad(t.getId(), t.getUserCode(), t.getFullName(),
                        t.getLevel().getTitle(), t.isAvailable(), active.getOrDefault(t.getId(), 0),
                        t.getMaxWorkload(), resolved.getOrDefault(t.getId(), 0L),
                        round(rated.stream().filter(i -> i.getAssignedTechnician().getId().equals(t.getId()))
                                .mapToInt(Incident::getFeedbackRating).average().orElse(0))))
                .toList();
    }

    private List<DailyCount> trend(List<Incident> all) {
        LocalDate today = LocalDate.now(clock);
        Map<LocalDate, long[]> days = new LinkedHashMap<>();
        for (int d = TREND_DAYS - 1; d >= 0; d--) {
            days.put(today.minusDays(d), new long[2]);
        }
        for (Incident i : all) {
            long[] created = days.get(i.getCreatedAt().toLocalDate());
            if (created != null) created[0]++;
            if (i.getResolvedAt() != null) {
                long[] resolved = days.get(i.getResolvedAt().toLocalDate());
                if (resolved != null) resolved[1]++;
            }
        }
        return days.entrySet().stream().map(e -> new DailyCount(e.getKey(), e.getValue()[0], e.getValue()[1])).toList();
    }

    private static double round(double v) {
        return Math.round(v * 10) / 10.0;
    }
}
