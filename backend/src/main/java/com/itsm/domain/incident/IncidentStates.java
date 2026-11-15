package com.itsm.domain.incident;

import java.util.EnumMap;
import java.util.Map;

/** States are stateless, so one shared instance per status is enough (Flyweight). */
public final class IncidentStates {

    private static final Map<IncidentStatus, IncidentState> STATES = new EnumMap<>(IncidentStatus.class);

    static {
        register(new NewState());
        register(new ClassifiedState());
        register(new AssignedState());
        register(new AcknowledgedState());
        register(new InProgressState());
        register(new PendingInfoState());
        register(new ResolvedState());
        register(new VerifiedState());
        register(new ClosedState());
    }

    private IncidentStates() {
    }

    private static void register(IncidentState state) {
        STATES.put(state.status(), state);
    }

    public static IncidentState of(IncidentStatus status) {
        IncidentState state = STATES.get(status);
        if (state == null) {
            throw new IllegalStateException("No state object registered for " + status);
        }
        return state;
    }
}
