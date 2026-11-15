package com.itsm.intelligence;

import com.itsm.domain.catalog.Category;

import java.util.List;

/** Suggests incident categories from free text. Results are ordered by descending confidence. */
public interface CategoryClassifier {

    String name();

    List<CategorySuggestion> classify(String text, List<Category> categories);
}
