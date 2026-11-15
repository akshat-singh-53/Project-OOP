package com.itsm.strategy.assignment;

import com.itsm.domain.incident.Incident;
import com.itsm.strategy.NamedStrategy;

import java.util.List;
import java.util.Optional;

/**
 * Strategy pattern: decides which technician should work on an incident. New strategies are
 * added by writing one more Spring bean; nothing else in the application changes.
 */
public interface AssignmentStrategy extends NamedStrategy {

    /** Ranks candidates from best to worst. Candidates are already filtered for availability. */
    List<TechnicianRecommendation> rank(Incident incident, List<TechnicianCandidate> candidates);

    /** Picks the technician to assign. Strategies with internal state (round robin) override this. */
    default Optional<Long> select(Incident incident, List<TechnicianCandidate> candidates) {
        return rank(incident, candidates).stream().findFirst().map(TechnicianRecommendation::technicianId);
    }

    static TechnicianRecommendation recommendation(TechnicianCandidate c, Incident incident, double score,
                                                   List<String> reasons) {
        var t = c.technician();
        return new TechnicianRecommendation(t.getId(), t.getFullName(), t.getLevel().getTitle(),
                Math.round(score * 10) / 10.0, c.activeWorkload(), t.getMaxWorkload(),
                t.hasSkill(incident.getCategory()), reasons);
    }
}
