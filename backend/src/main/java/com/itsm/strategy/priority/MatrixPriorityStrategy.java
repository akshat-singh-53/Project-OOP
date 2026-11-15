package com.itsm.strategy.priority;

import com.itsm.domain.incident.Priority;
import org.springframework.stereotype.Component;

/**
 * Score = Impact (1-4) x Urgency (1-4) x Business criticality (1-5), giving 1..80.
 * P1 >= 40, P2 >= 20, P3 >= 8, otherwise P4.
 */
@Component
public class MatrixPriorityStrategy implements PriorityStrategy {

    public static final String KEY = "matrix";

    static final int P1_THRESHOLD = 40;
    static final int P2_THRESHOLD = 20;
    static final int P3_THRESHOLD = 8;

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public String displayName() {
        return "Impact x Urgency x Criticality";
    }

    @Override
    public String description() {
        return "Classic ITIL matrix: multiplies impact, urgency and the business criticality of the service.";
    }

    @Override
    public PriorityResult calculate(PriorityInput input) {
        int score = input.impact().getWeight() * input.urgency().getWeight() * input.businessCriticality();
        Priority priority;
        if (score >= P1_THRESHOLD) {
            priority = Priority.P1;
        } else if (score >= P2_THRESHOLD) {
            priority = Priority.P2;
        } else if (score >= P3_THRESHOLD) {
            priority = Priority.P3;
        } else {
            priority = Priority.P4;
        }
        String explanation = String.format("Impact %s(%d) x Urgency %s(%d) x Criticality %d = %d -> %s",
                input.impact(), input.impact().getWeight(), input.urgency(), input.urgency().getWeight(),
                input.businessCriticality(), score, priority);
        return new PriorityResult(priority, score, explanation);
    }
}
