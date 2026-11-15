package com.itsm.service;

import com.itsm.domain.catalog.Category;
import com.itsm.domain.catalog.ItService;
import com.itsm.domain.incident.Incident;
import com.itsm.domain.incident.IncidentStatus;
import com.itsm.domain.user.Technician;
import com.itsm.exception.ExternalServiceException;
import com.itsm.intelligence.CategorySuggestion;
import com.itsm.intelligence.CorrelationAnalyzer;
import com.itsm.intelligence.CorrelationCluster;
import com.itsm.intelligence.DuplicateCandidate;
import com.itsm.intelligence.DuplicateDetector;
import com.itsm.intelligence.KeywordCategoryClassifier;
import com.itsm.intelligence.LlmCategoryClassifier;
import com.itsm.intelligence.ResolutionSuggester;
import com.itsm.intelligence.ResolutionSuggestion;
import com.itsm.intelligence.SlaRiskAssessment;
import com.itsm.intelligence.SlaRiskPredictor;
import com.itsm.intelligence.TextAnalyzer;
import com.itsm.repository.CategoryRepository;
import com.itsm.repository.IncidentRepository;
import com.itsm.repository.ItServiceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Set;

/** Facade over all intelligent / automated features. */
@Service
public class IntelligenceService {

    private static final Logger log = LoggerFactory.getLogger(IntelligenceService.class);

    static final Set<IncidentStatus> OPEN = EnumSet.of(IncidentStatus.NEW, IncidentStatus.CLASSIFIED,
            IncidentStatus.ASSIGNED, IncidentStatus.ACKNOWLEDGED, IncidentStatus.IN_PROGRESS,
            IncidentStatus.PENDING_INFO);
    private static final Set<IncidentStatus> SOLVED = EnumSet.of(IncidentStatus.RESOLVED, IncidentStatus.VERIFIED,
            IncidentStatus.CLOSED);

    private static final double KEYWORD_WEIGHT = 0.5;
    private static final double HISTORY_WEIGHT = 0.25;
    private static final double SERVICE_PRIOR_WEIGHT = 0.25;
    private static final int NEIGHBOURS = 5;

    private final IncidentRepository incidents;
    private final CategoryRepository categories;
    private final ItServiceRepository services;
    private final KeywordCategoryClassifier keywordClassifier;
    private final LlmCategoryClassifier llmClassifier;
    private final DuplicateDetector duplicateDetector;
    private final ResolutionSuggester resolutionSuggester;
    private final CorrelationAnalyzer correlationAnalyzer;
    private final SlaRiskPredictor riskPredictor;
    private final SettingsService settings;
    private final UserService userService;
    private final Clock clock;

    public IntelligenceService(IncidentRepository incidents, CategoryRepository categories,
                               ItServiceRepository services,
                               KeywordCategoryClassifier keywordClassifier, LlmCategoryClassifier llmClassifier,
                               DuplicateDetector duplicateDetector, ResolutionSuggester resolutionSuggester,
                               CorrelationAnalyzer correlationAnalyzer, SlaRiskPredictor riskPredictor,
                               SettingsService settings, UserService userService, Clock clock) {
        this.incidents = incidents;
        this.categories = categories;
        this.services = services;
        this.keywordClassifier = keywordClassifier;
        this.llmClassifier = llmClassifier;
        this.duplicateDetector = duplicateDetector;
        this.resolutionSuggester = resolutionSuggester;
        this.correlationAnalyzer = correlationAnalyzer;
        this.riskPredictor = riskPredictor;
        this.settings = settings;
        this.userService = userService;
        this.clock = clock;
    }

    // ------------------------------------------------------------- automatic categorisation

    /**
     * Ensemble classifier: an external LLM if configured, otherwise 50% keyword rules, 25% votes
     * from the most similar historical incidents (k-nearest neighbours) and 25% prior from the
     * default category of the affected service.
     */
    @Transactional(readOnly = true)
    public List<CategorySuggestion> suggestCategories(String text, Long serviceId) {
        List<Category> active = categories.findByActiveTrueOrderByNameAsc();
        if (text == null || text.isBlank() || active.isEmpty()) {
            return List.of();
        }
        if (llmClassifier.isConfigured() && settings.getBoolean(SettingsService.AI_CLASSIFIER_ENABLED)) {
            try {
                List<CategorySuggestion> llm = llmClassifier.classify(text, active);
                if (!llm.isEmpty()) {
                    return llm;
                }
            } catch (ExternalServiceException ex) {
                log.warn("LLM classifier unavailable, falling back to local model: {}", ex.getMessage());
            }
        }

        Map<Long, Double> scores = new HashMap<>();
        Map<Long, List<String>> evidence = new HashMap<>();
        for (CategorySuggestion s : keywordClassifier.classify(text, active)) {
            scores.merge(s.categoryId(), KEYWORD_WEIGHT * s.confidence(), Double::sum);
            evidence.computeIfAbsent(s.categoryId(), k -> new ArrayList<>()).add("keywords: " + String.join(", ", s.evidence()));
        }

        var query = TextAnalyzer.termFrequencies(text);
        record Neighbour(Incident incident, double similarity) {
        }
        List<Neighbour> neighbours = incidents.findByStatusIn(SOLVED).stream()
                .filter(i -> i.getCategory() != null)
                .map(i -> new Neighbour(i, TextAnalyzer.cosineSimilarity(query, TextAnalyzer.termFrequencies(i.getSearchableText()))))
                .filter(n -> n.similarity() > 0.2)
                .sorted(Comparator.comparingDouble(Neighbour::similarity).reversed())
                .limit(NEIGHBOURS)
                .toList();
        // dividing by at least 1 stops a single weak neighbour from casting a full-weight vote
        double totalSim = Math.max(1.0, neighbours.stream().mapToDouble(Neighbour::similarity).sum());
        for (Neighbour n : neighbours) {
            Long id = n.incident().getCategory().getId();
            scores.merge(id, HISTORY_WEIGHT * n.similarity() / totalSim, Double::sum);
            evidence.computeIfAbsent(id, k -> new ArrayList<>()).add("similar to " + n.incident().getCode());
        }

        if (serviceId != null) {
            services.findById(serviceId).map(ItService::getDefaultCategory).ifPresent(c -> {
                scores.merge(c.getId(), SERVICE_PRIOR_WEIGHT, Double::sum);
                evidence.computeIfAbsent(c.getId(), k -> new ArrayList<>()).add("usual category for this service");
            });
        }

        Map<Long, Category> byId = new LinkedHashMap<>();
        active.forEach(c -> byId.put(c.getId(), c));
        return scores.entrySet().stream()
                .filter(e -> byId.containsKey(e.getKey()))
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(3)
                .map(e -> {
                    Category c = byId.get(e.getKey());
                    return new CategorySuggestion(c.getId(), c.getCode(), c.getName(),
                            Math.round(e.getValue() * 100) / 100.0, evidence.get(e.getKey()), "keyword+history");
                })
                .toList();
    }

    // ------------------------------------------------------------- duplicates & correlation

    @Transactional(readOnly = true)
    public List<DuplicateCandidate> findDuplicates(String text, Long serviceId, Long excludeId) {
        LocalDateTime since = LocalDateTime.now(clock).minusDays(3);
        return duplicateDetector.findCandidates(text, serviceId, excludeId,
                incidents.findByStatusInAndCreatedAtAfter(OPEN, since));
    }

    @Transactional(readOnly = true)
    public List<CorrelationCluster> correlate(int hours) {
        LocalDateTime since = LocalDateTime.now(clock).minusHours(hours);
        return correlationAnalyzer.analyse(incidents.findByStatusInAndCreatedAtAfter(OPEN, since));
    }

    // ------------------------------------------------------------- knowledge reuse

    @Transactional(readOnly = true)
    public List<ResolutionSuggestion> suggestResolutions(Incident incident) {
        return resolutionSuggester.suggest(incident, incidents.findByResolutionIsNotNull());
    }

    // ------------------------------------------------------------- SLA risk prediction

    @Transactional(readOnly = true)
    public List<SlaRiskAssessment> slaRisks() {
        LocalDateTime now = LocalDateTime.now(clock);
        List<Incident> solved = incidents.findByResolutionIsNotNull();
        Map<Long, Integer> workload = userService.workloadByTechnician();
        return incidents.findByStatusIn(OPEN).stream()
                .filter(i -> i.getResolutionDueAt() != null)
                .map(i -> {
                    Technician t = i.getAssignedTechnician();
                    return riskPredictor.assess(i, historicalAverage(i, solved),
                            t == null ? 0 : workload.getOrDefault(t.getId(), 0), now);
                })
                .sorted(Comparator.comparingInt(SlaRiskAssessment::riskScore).reversed())
                .toList();
    }

    private static double historicalAverage(Incident incident, List<Incident> solved) {
        OptionalDouble sameCategoryAndPriority = solved.stream()
                .filter(s -> s.getPriority() == incident.getPriority() && s.getCategory() != null
                        && incident.getCategory() != null && s.getCategory().getId().equals(incident.getCategory().getId()))
                .filter(s -> s.getResolutionMinutes() != null)
                .mapToLong(Incident::getResolutionMinutes).average();
        if (sameCategoryAndPriority.isPresent()) {
            return sameCategoryAndPriority.getAsDouble();
        }
        return solved.stream()
                .filter(s -> s.getPriority() == incident.getPriority() && s.getResolutionMinutes() != null)
                .mapToLong(Incident::getResolutionMinutes).average().orElse(0);
    }
}
