package com.itsm.strategy.assignment;

import com.itsm.domain.incident.Incident;
import com.itsm.domain.user.TechnicianLevel;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Intelligent recommendation combining several signals into one score out of 100:
 * skill match (40), spare capacity (25), experience (15), history with the same service (10)
 * and seniority for urgent incidents (10).
 */
@Component
public class SmartAssignmentStrategy implements AssignmentStrategy {

    public static final String KEY = "smart";

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public String displayName() {
        return "Smart recommendation (multi-factor)";
    }

    @Override
    public String description() {
        return "Scores skill match, spare capacity, experience, past work on the same service and "
                + "seniority for P1/P2 incidents.";
    }

    @Override
    public List<TechnicianRecommendation> rank(Incident incident, List<TechnicianCandidate> candidates) {
        return candidates.stream()
                .map(c -> score(incident, c))
                .sorted(Comparator.comparingDouble(TechnicianRecommendation::score).reversed())
                .toList();
    }

    private TechnicianRecommendation score(Incident incident, TechnicianCandidate c) {
        var tech = c.technician();
        List<String> reasons = new ArrayList<>();
        double score = 0;

        if (tech.hasSkill(incident.getCategory())) {
            score += 40;
            reasons.add("Skill match: " + incident.getCategory().getName());
        }

        double capacity = 25 * (1 - c.loadRatio());
        score += capacity;
        reasons.add(String.format("Workload %d/%d", c.activeWorkload(), tech.getMaxWorkload()));

        score += 15 * Math.min(tech.getExperienceYears(), 10) / 10.0;
        reasons.add(tech.getExperienceYears() + " yrs experience");

        if (c.serviceExperience() > 0) {
            score += 10 * Math.min(c.serviceExperience(), 5) / 5.0;
            reasons.add("Resolved " + c.serviceExperience() + " past incident(s) on " + incident.getService().getName());
        }

        boolean urgent = incident.getPriority() != null && incident.getPriority().isUrgent();
        if (urgent && tech.getLevel() != TechnicianLevel.L1) {
            score += 10;
            reasons.add("Senior engineer for " + incident.getPriority() + " incident");
        }
        return AssignmentStrategy.recommendation(c, incident, score, reasons);
    }
}
