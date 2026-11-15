package com.itsm.strategy.assignment;

import com.itsm.domain.user.Technician;

/**
 * A technician together with live statistics needed by assignment strategies. Keeping the
 * statistics here means strategies are pure functions and easy to unit test.
 */
public record TechnicianCandidate(Technician technician, int activeWorkload, int serviceExperience) {

    public double loadRatio() {
        return (double) activeWorkload / technician.getMaxWorkload();
    }

    public boolean hasCapacity() {
        return activeWorkload < technician.getMaxWorkload();
    }
}
