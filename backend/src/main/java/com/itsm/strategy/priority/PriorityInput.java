package com.itsm.strategy.priority;

import com.itsm.domain.incident.Impact;
import com.itsm.domain.incident.Urgency;

public record PriorityInput(Impact impact, Urgency urgency, int businessCriticality, int affectedUsers) {
}
