package com.itsm.strategy.assignment;

import java.util.List;

public record TechnicianRecommendation(Long technicianId,
                                       String technicianName,
                                       String level,
                                       double score,
                                       int activeWorkload,
                                       int maxWorkload,
                                       boolean skillMatch,
                                       List<String> reasons) {
}
