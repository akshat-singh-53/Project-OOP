package com.itsm.strategy.assignment;

import com.itsm.domain.incident.Incident;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/** Balances work: the technician with the lowest workload ratio gets the incident. */
@Component
public class LoadBasedAssignmentStrategy implements AssignmentStrategy {

    public static final String KEY = "load";

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public String displayName() {
        return "Load-based (least busy)";
    }

    @Override
    public String description() {
        return "Assigns to the technician with the lowest active workload relative to their capacity.";
    }

    @Override
    public List<TechnicianRecommendation> rank(Incident incident, List<TechnicianCandidate> candidates) {
        return candidates.stream()
                .map(c -> AssignmentStrategy.recommendation(c, incident, 100 * (1 - c.loadRatio()),
                        List.of(c.activeWorkload() + "/" + c.technician().getMaxWorkload() + " active incidents")))
                .sorted(Comparator.comparingDouble(TechnicianRecommendation::score).reversed()
                        .thenComparing(TechnicianRecommendation::technicianId))
                .toList();
    }
}
