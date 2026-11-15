package com.itsm.domain.incident;

public enum Priority {
    P1("Critical", 1),
    P2("High", 2),
    P3("Medium", 3),
    P4("Low", 4);

    private final String label;
    private final int level;

    Priority(String label, int level) {
        this.label = label;
        this.level = level;
    }

    public String getLabel() {
        return label;
    }

    public int getLevel() {
        return level;
    }

    public boolean isHigherThan(Priority other) {
        return this.level < other.level;
    }

    public boolean isUrgent() {
        return this == P1 || this == P2;
    }
}
