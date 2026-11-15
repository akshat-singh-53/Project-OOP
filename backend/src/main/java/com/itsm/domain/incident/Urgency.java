package com.itsm.domain.incident;

/** How quickly the business needs the problem fixed. */
public enum Urgency {
    LOW(1, "Can wait several days"),
    MEDIUM(2, "Needed within a working day"),
    HIGH(3, "Needed within hours"),
    CRITICAL(4, "Work has stopped, needed immediately");

    private final int weight;
    private final String description;

    Urgency(int weight, String description) {
        this.weight = weight;
        this.description = description;
    }

    public int getWeight() {
        return weight;
    }

    public String getDescription() {
        return description;
    }
}
