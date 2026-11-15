package com.itsm.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Subject of the Observer pattern. Every {@link IncidentEventListener} bean is subscribed
 * automatically; extra listeners can also subscribe at runtime.
 */
@Component
public class IncidentEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(IncidentEventPublisher.class);

    private final List<IncidentEventListener> listeners = new CopyOnWriteArrayList<>();

    public IncidentEventPublisher(List<IncidentEventListener> listeners) {
        this.listeners.addAll(listeners);
    }

    public void subscribe(IncidentEventListener listener) {
        listeners.add(listener);
    }

    public void unsubscribe(IncidentEventListener listener) {
        listeners.remove(listener);
    }

    public void publish(IncidentEvent event) {
        for (IncidentEventListener listener : listeners) {
            try {
                listener.onIncidentEvent(event);
            } catch (RuntimeException ex) {
                // one failing observer (e.g. SMS gateway down) must not break the business operation
                log.error("Listener {} failed for event {} on {}", listener.getClass().getSimpleName(),
                        event.type(), event.incident().getCode(), ex);
            }
        }
    }
}
