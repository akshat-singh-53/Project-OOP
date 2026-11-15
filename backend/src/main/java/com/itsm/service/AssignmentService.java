package com.itsm.service;

import com.itsm.domain.incident.Incident;
import com.itsm.domain.user.Technician;
import com.itsm.exception.TechnicianUnavailableException;
import com.itsm.exception.WorkloadExceededException;
import com.itsm.repository.IncidentRepository;
import com.itsm.repository.TechnicianRepository;
import com.itsm.strategy.StrategyRegistry;
import com.itsm.strategy.assignment.AssignmentStrategy;
import com.itsm.strategy.assignment.TechnicianCandidate;
import com.itsm.strategy.assignment.TechnicianRecommendation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/** Context of the assignment Strategy pattern. */
@Service
public class AssignmentService {

    private final StrategyRegistry<AssignmentStrategy> registry;
    private final SettingsService settings;
    private final TechnicianRepository technicians;
    private final IncidentRepository incidents;
    private final UserService userService;

    public AssignmentService(List<AssignmentStrategy> strategies, SettingsService settings,
                             TechnicianRepository technicians, IncidentRepository incidents, UserService userService) {
        this.registry = new StrategyRegistry<>("assignment", strategies);
        this.settings = settings;
        this.technicians = technicians;
        this.incidents = incidents;
        this.userService = userService;
    }

    public AssignmentStrategy current() {
        return registry.get(settings.get(SettingsService.ASSIGNMENT_STRATEGY));
    }

    public Collection<AssignmentStrategy> available() {
        return registry.all();
    }

    public void ensureExists(String key) {
        registry.get(key);
    }

    /** Active, available technicians who still have spare capacity. */
    @Transactional(readOnly = true)
    public List<TechnicianCandidate> candidates(Incident incident) {
        Map<Long, Integer> workload = userService.workloadByTechnician();
        return technicians.findByActiveTrueOrderByIdAsc().stream()
                .filter(Technician::isAvailable)
                .map(t -> new TechnicianCandidate(t, workload.getOrDefault(t.getId(), 0),
                        (int) incidents.countByAssignedTechnicianAndServiceAndResolvedAtIsNotNull(t, incident.getService())))
                .filter(TechnicianCandidate::hasCapacity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TechnicianRecommendation> recommend(Incident incident, String strategyKey) {
        AssignmentStrategy strategy = strategyKey == null ? current() : registry.get(strategyKey);
        return strategy.rank(incident, candidates(incident));
    }

    /** Chooses a technician with the configured strategy. */
    public Technician selectAutomatically(Incident incident) {
        List<TechnicianCandidate> candidates = candidates(incident);
        if (candidates.isEmpty()) {
            throw new TechnicianUnavailableException("No technician is available with spare capacity");
        }
        Long id = current().select(incident, candidates)
                .orElseThrow(() -> new TechnicianUnavailableException("Strategy found no suitable technician"));
        return candidates.stream().map(TechnicianCandidate::technician)
                .filter(t -> t.getId().equals(id)).findFirst().orElseThrow();
    }

    /** Validates a manual choice made by an administrator or team lead. */
    public void validateManualChoice(Technician technician) {
        if (!technician.isActive() || !technician.isAvailable()) {
            throw new TechnicianUnavailableException(technician.getFullName() + " is currently unavailable");
        }
        int load = userService.workloadOf(technician);
        if (load >= technician.getMaxWorkload()) {
            throw new WorkloadExceededException(technician.getFullName(), load, technician.getMaxWorkload());
        }
    }
}
