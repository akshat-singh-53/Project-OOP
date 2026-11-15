package com.itsm.event;

public enum IncidentEventType {
    CREATED("Incident created"),
    CLASSIFIED("Incident classified"),
    PRIORITY_CALCULATED("Priority calculated"),
    CATEGORY_CHANGED("Category changed"),
    PRIORITY_CHANGED("Priority changed"),
    SLA_APPLIED("SLA applied"),
    DUPLICATE_SUSPECTED("Possible duplicate detected"),
    ASSIGNED("Technician assigned"),
    ASSIGNMENT_FAILED("Automatic assignment failed"),
    ACKNOWLEDGED("Technician acknowledged incident"),
    WORK_STARTED("Investigation started"),
    NOTE_ADDED("Note added"),
    INFO_REQUESTED("Additional information requested"),
    INFO_PROVIDED("Additional information provided"),
    RESOLVED("Incident resolved"),
    VERIFIED("Resolution verified"),
    REOPENED("Incident reopened"),
    CLOSED("Incident closed"),
    MARKED_DUPLICATE("Closed as duplicate"),
    FEEDBACK_RECEIVED("Feedback received"),
    SLA_WARNING("SLA approaching"),
    SLA_VIOLATED("SLA violated"),
    ESCALATED("Incident escalated");

    private final String label;

    IncidentEventType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
