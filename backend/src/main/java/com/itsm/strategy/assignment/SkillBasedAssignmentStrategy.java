package com.itsm.strategy.assignment;

import com.itsm.domain.incident.Incident;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/** Technicians skilled in the incident category first; ties broken by seniority and experience. */
@Component
public class SkillBasedAssignmentStrategy implements AssignmentStrategy {

    public static final String KEY = "skill";

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public String displayName() {
        return "Skill-based";
    }

    @Override
    public String description() {
        return "Prefers technicians whose skills match the incident category, then the most experienced.";
    }

    @Override
    public List<TechnicianRecommendation> rank(Incident incident, List<TechnicianCandidate> candidates) {
        return candidates.stream()
                .map(c -> {
                    boolean skilled = c.technician().hasSkill(incident.getCategory());
                    double score = (skilled ? 70 : 0)
                            + c.technician().getLevel().getRank() * 5
                            + Math.min(c.technician().getExperienceYears(), 15);
                    String reason = skilled
                            ? "Skilled in " + incident.getCategory().getName()
                            : "No matching skill (fallback)";
                    return AssignmentStrategy.recommendation(c, incident, score,
                            List.of(reason, c.technician().getExperienceYears() + " years experience"));
                })
                .sorted(Comparator.comparingDouble(TechnicianRecommendation::score).reversed())
                .toList();
    }
}
