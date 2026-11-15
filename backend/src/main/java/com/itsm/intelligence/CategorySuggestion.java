package com.itsm.intelligence;

import java.util.List;

public record CategorySuggestion(Long categoryId,
                                 String code,
                                 String name,
                                 double confidence,
                                 List<String> evidence,
                                 String source) {
}
