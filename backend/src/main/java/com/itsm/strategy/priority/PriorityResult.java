package com.itsm.strategy.priority;

import com.itsm.domain.incident.Priority;

public record PriorityResult(Priority priority, double score, String explanation) {
}
