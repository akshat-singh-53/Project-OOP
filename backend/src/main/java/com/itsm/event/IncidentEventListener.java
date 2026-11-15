package com.itsm.event;

/** Observer pattern: implementations react to incident events without the publisher knowing them. */
public interface IncidentEventListener {

    void onIncidentEvent(IncidentEvent event);
}
