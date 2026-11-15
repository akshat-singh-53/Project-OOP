package com.itsm.domain.user;

public enum Role {
    EMPLOYEE("E"),
    TECHNICIAN("T"),
    ADMIN("A");

    private final String codePrefix;

    Role(String codePrefix) {
        this.codePrefix = codePrefix;
    }

    public String getCodePrefix() {
        return codePrefix;
    }
}
