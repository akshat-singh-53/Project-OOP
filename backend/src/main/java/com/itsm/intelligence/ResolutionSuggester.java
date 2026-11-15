package com.itsm.intelligence;

import com.itsm.domain.incident.Incident;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Knowledge reuse: recommends resolutions from previously solved incidents with similar text.
 * Same-category matches and well-rated resolutions get a small boost.
 */
@Component
public class ResolutionSuggester {

    private static final double MIN_SIMILARITY = 0.15;

    public List<ResolutionSuggestion> suggest(Incident incident, List<Incident> solved) {
        Map<String, Integer> query = TextAnalyzer.termFrequencies(incident.getSearchableText());
        return solved.stream()
                .filter(s -> !Objects.equals(s.getId(), incident.getId()) && s.getResolution() != null)
                .map(s -> {
                    double sim = TextAnalyzer.cosineSimilarity(query, TextAnalyzer.termFrequencies(s.getSearchableText()));
                    if (incident.getCategory() != null && s.getCategory() != null
                            && incident.getCategory().getId().equals(s.getCategory().getId())) {
                        sim += 0.1;
                    }
                    if (s.getFeedbackRating() != null && s.getFeedbackRating() >= 4) {
                        sim += 0.05;
                    }
                    return new ResolutionSuggestion(s.getCode(), s.getTitle(), s.getResolution(), s.getRootCause(),
                            Math.round(Math.min(sim, 1.0) * 100) / 100.0, s.getFeedbackRating());
                })
                .filter(r -> r.similarity() >= MIN_SIMILARITY)
                .sorted(Comparator.comparingDouble(ResolutionSuggestion::similarity).reversed())
                .limit(3)
                .toList();
    }
}
