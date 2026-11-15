package com.itsm.domain.user;

public enum TechnicianLevel {
    L1("Support Engineer", 1),
    L2("Senior Engineer", 2),
    TEAM_LEAD("Team Lead", 3);

    private final String title;
    private final int rank;

    TechnicianLevel(String title, int rank) {
        this.title = title;
        this.rank = rank;
    }

    public String getTitle() {
        return title;
    }

    public int getRank() {
        return rank;
    }
}
