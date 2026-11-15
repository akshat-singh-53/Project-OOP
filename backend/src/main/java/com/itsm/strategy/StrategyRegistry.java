package com.itsm.strategy;

import com.itsm.exception.ValidationException;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Generic lookup of strategies by key, shared by priority and assignment strategies. */
public class StrategyRegistry<T extends NamedStrategy> {

    private final Map<String, T> strategies = new LinkedHashMap<>();
    private final String kind;

    public StrategyRegistry(String kind, List<T> implementations) {
        this.kind = kind;
        implementations.forEach(s -> strategies.put(s.key(), s));
    }

    public T get(String key) {
        T strategy = strategies.get(key);
        if (strategy == null) {
            throw new ValidationException("Unknown " + kind + " strategy '" + key + "'. Available: " + strategies.keySet());
        }
        return strategy;
    }

    public boolean contains(String key) {
        return strategies.containsKey(key);
    }

    public Collection<T> all() {
        return Collections.unmodifiableCollection(strategies.values());
    }
}
