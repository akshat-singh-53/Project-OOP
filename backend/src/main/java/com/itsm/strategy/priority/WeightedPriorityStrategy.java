package com.itsm.strategy.priority;

import com.itsm.domain.incident.Priority;
import org.springframework.stereotype.Component;

/**
 * Weighted score on a 0-100 scale that also considers how many users are affected:
 * 35% impact + 30% urgency + 20% criticality + 15% affected users (logarithmic, 1000 users = full).
 */
@Component
public class WeightedPriorityStrategy implements PriorityStrategy {

    public static final String KEY = "weighted";

    private static final double IMPACT_WEIGHT = 0.35;
    private static final double URGENCY_WEIGHT = 0.30;
    private static final double CRITICALITY_WEIGHT = 0.20;
    private static final double USERS_WEIGHT = 0.15;

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public String displayName() {
        return "Weighted scoring (incl. affected users)";
    }

    @Override
    public String description() {
        return "Weighted 0-100 score: 35% impact, 30% urgency, 20% criticality, 15% number of affected users.";
    }

    @Override
    public PriorityResult calculate(PriorityInput input) {
        double impact = input.impact().getWeight() / 4.0;
        double urgency = input.urgency().getWeight() / 4.0;
        double criticality = input.businessCriticality() / 5.0;
        double users = Math.min(1.0, Math.log10(Math.max(1, input.affectedUsers()) + 1) / 3.0);

        double score = 100 * (IMPACT_WEIGHT * impact + URGENCY_WEIGHT * urgency
                + CRITICALITY_WEIGHT * criticality + USERS_WEIGHT * users);
        score = Math.round(score * 10) / 10.0;

        Priority priority;
        if (score >= 75) {
            priority = Priority.P1;
        } else if (score >= 55) {
            priority = Priority.P2;
        } else if (score >= 35) {
            priority = Priority.P3;
        } else {
            priority = Priority.P4;
        }
        String explanation = String.format(
                "Weighted score %.1f/100 (impact %s, urgency %s, criticality %d, %d users) -> %s",
                score, input.impact(), input.urgency(), input.businessCriticality(), input.affectedUsers(), priority);
        return new PriorityResult(priority, score, explanation);
    }
}
