package com.itsm.service;

import com.itsm.domain.incident.Incident;
import com.itsm.strategy.StrategyRegistry;
import com.itsm.strategy.priority.PriorityInput;
import com.itsm.strategy.priority.PriorityResult;
import com.itsm.strategy.priority.PriorityStrategy;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/** Context of the priority Strategy pattern: delegates to whichever algorithm the admin selected. */
@Service
public class PriorityService {

    private final StrategyRegistry<PriorityStrategy> registry;
    private final SettingsService settings;

    public PriorityService(List<PriorityStrategy> strategies, SettingsService settings) {
        this.registry = new StrategyRegistry<>("priority", strategies);
        this.settings = settings;
    }

    public PriorityStrategy current() {
        return registry.get(settings.get(SettingsService.PRIORITY_STRATEGY));
    }

    public PriorityResult calculate(Incident incident) {
        PriorityInput input = new PriorityInput(incident.getImpact(), incident.getUrgency(),
                incident.getService().getBusinessCriticality(), incident.getAffectedUsers());
        return current().calculate(input);
    }

    public Collection<PriorityStrategy> available() {
        return registry.all();
    }

    public void ensureExists(String key) {
        registry.get(key);
    }
}
