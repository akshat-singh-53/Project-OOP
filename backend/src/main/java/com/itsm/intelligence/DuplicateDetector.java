package com.itsm.intelligence;

import com.itsm.domain.incident.Incident;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Finds open incidents that probably describe the same underlying problem.
 * Score = 0.75 x cosine text similarity + 0.25 if the same service is affected.
 */
@Component
public class DuplicateDetector {

    public static final double CANDIDATE_THRESHOLD = 0.35;
    public static final double STRONG_MATCH_THRESHOLD = 0.55;

    private static final double TEXT_WEIGHT = 0.75;
    private static final double SERVICE_WEIGHT = 0.25;

    public List<DuplicateCandidate> findCandidates(String text, Long serviceId, Long excludeIncidentId,
                                                   List<Incident> openIncidents) {
        Map<String, Integer> query = TextAnalyzer.termFrequencies(text);
        return openIncidents.stream()
                .filter(i -> !Objects.equals(i.getId(), excludeIncidentId))
                .map(i -> {
                    double sim = TextAnalyzer.cosineSimilarity(query, TextAnalyzer.termFrequencies(i.getSearchableText()));
                    boolean sameService = serviceId != null && serviceId.equals(i.getService().getId());
                    double score = TEXT_WEIGHT * sim + (sameService && sim > 0 ? SERVICE_WEIGHT : 0);
                    return new DuplicateCandidate(i.getCode(), i.getTitle(), i.getStatus().name(),
                            i.getService().getName(), round(sim), sameService, round(score));
                })
                .filter(c -> c.score() >= CANDIDATE_THRESHOLD)
                .sorted(Comparator.comparingDouble(DuplicateCandidate::score).reversed())
                .limit(5)
                .toList();
    }

    private static double round(double v) {
        return Math.round(v * 100) / 100.0;
    }
}
