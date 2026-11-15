package com.itsm.intelligence;

import com.itsm.domain.catalog.Category;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Rule-based classifier that matches the incident text against each category's keyword list. */
@Component
public class KeywordCategoryClassifier implements CategoryClassifier {

    @Override
    public String name() {
        return "keyword";
    }

    @Override
    public List<CategorySuggestion> classify(String text, List<Category> categories) {
        Set<String> tokens = TextAnalyzer.tokens(text);
        String lower = text == null ? "" : text.toLowerCase(Locale.ROOT);

        List<CategorySuggestion> raw = new ArrayList<>();
        double total = 0;
        for (Category category : categories) {
            List<String> matched = new ArrayList<>();
            for (String keyword : category.keywordList()) {
                boolean hit = keyword.contains(" ")
                        ? lower.contains(keyword)
                        : tokens.contains(TextAnalyzer.normalise(keyword));
                if (hit) {
                    matched.add(keyword);
                }
            }
            if (!matched.isEmpty()) {
                double score = matched.size();
                total += score;
                raw.add(new CategorySuggestion(category.getId(), category.getCode(), category.getName(), score,
                        matched, name()));
            }
        }
        final double sum = total;
        return raw.stream()
                .map(s -> new CategorySuggestion(s.categoryId(), s.code(), s.name(),
                        round(s.confidence() / sum), s.evidence(), s.source()))
                .sorted(Comparator.comparingDouble(CategorySuggestion::confidence).reversed())
                .toList();
    }

    private static double round(double v) {
        return Math.round(v * 100) / 100.0;
    }
}
