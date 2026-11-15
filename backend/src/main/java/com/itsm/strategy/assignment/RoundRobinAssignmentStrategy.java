package com.itsm.strategy.assignment;

import com.itsm.domain.incident.Incident;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/** Hands incidents to technicians in turn, regardless of skill or load. */
@Component
public class RoundRobinAssignmentStrategy implements AssignmentStrategy {

    public static final String KEY = "round-robin";

    private final AtomicInteger cursor = new AtomicInteger();

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public String displayName() {
        return "Round robin";
    }

    @Override
    public String description() {
        return "Distributes incidents to available technicians one after another in a fixed rotation.";
    }

    @Override
    public List<TechnicianRecommendation> rank(Incident incident, List<TechnicianCandidate> candidates) {
        List<TechnicianCandidate> ordered = rotation(candidates, cursor.get());
        List<TechnicianRecommendation> result = new ArrayList<>();
        for (int i = 0; i < ordered.size(); i++) {
            result.add(AssignmentStrategy.recommendation(ordered.get(i), incident, 100.0 - i,
                    List.of(i == 0 ? "Next in rotation" : "Position " + (i + 1) + " in rotation")));
        }
        return result;
    }

    @Override
    public Optional<Long> select(Incident incident, List<TechnicianCandidate> candidates) {
        if (candidates.isEmpty()) {
            return Optional.empty();
        }
        int turn = cursor.getAndIncrement();
        return Optional.of(rotation(candidates, turn).get(0).technician().getId());
    }

    private static List<TechnicianCandidate> rotation(List<TechnicianCandidate> candidates, int turn) {
        List<TechnicianCandidate> sorted = candidates.stream()
                .sorted(Comparator.comparing(c -> c.technician().getId()))
                .toList();
        if (sorted.isEmpty()) {
            return sorted;
        }
        int start = Math.floorMod(turn, sorted.size());
        List<TechnicianCandidate> rotated = new ArrayList<>(sorted.subList(start, sorted.size()));
        rotated.addAll(sorted.subList(0, start));
        return rotated;
    }
}
