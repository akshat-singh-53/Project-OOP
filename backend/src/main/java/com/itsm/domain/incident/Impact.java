package com.itsm.domain.incident;

/** How widely the business is affected. */
public enum Impact {
    LOW(1, "Single user, workaround available"),
    MEDIUM(2, "A team or department affected"),
    HIGH(3, "Multiple departments or customers affected"),
    CRITICAL(4, "Organisation-wide or revenue impacting");

    private final int weight;
    private final String description;

    Impact(int weight, String description) {
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
