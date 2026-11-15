package com.itsm.domain.incident;

import com.itsm.domain.catalog.Category;
import com.itsm.domain.catalog.ItService;
import com.itsm.domain.user.Technician;
import com.itsm.domain.user.User;
import com.itsm.exception.InvalidStateTransitionException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Aggregate root for an IT incident. Lifecycle rules are delegated to an {@link IncidentState}
 * object (State pattern), so this class never contains a large switch on its status.
 */
@Entity
@Table(name = "incidents")
public class Incident {

    public static final String CODE_PREFIX = "INC";
    private static final int CODE_OFFSET = 1000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, length = 4000)
    private String description;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    private User reporter;

    @ManyToOne(fetch = FetchType.EAGER)
    private Category category;

    private boolean categorySuggestedAutomatically;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    private ItService service;

    @Enumerated(EnumType.STRING)
    private Impact impact;

    @Enumerated(EnumType.STRING)
    private Urgency urgency;

    private int affectedUsers;

    @Enumerated(EnumType.STRING)
    private Priority priority;

    private double priorityScore;

    @Column(length = 500)
    private String priorityExplanation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IncidentStatus status = IncidentStatus.NEW;

    @ManyToOne(fetch = FetchType.EAGER)
    private Technician assignedTechnician;

    private String assignmentStrategy;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime assignedAt;
    private LocalDateTime acknowledgedAt;
    private LocalDateTime resolvedAt;
    private LocalDateTime verifiedAt;
    private LocalDateTime closedAt;

    private int responseTargetMinutes;
    private int resolutionTargetMinutes;
    private LocalDateTime responseDueAt;
    private LocalDateTime resolutionDueAt;
    private int slaWarningPercent = 75;

    @Enumerated(EnumType.STRING)
    private SlaStatus slaStatus = SlaStatus.ON_TRACK;

    private boolean responseBreached;
    private boolean resolutionBreached;
    private boolean responseWarned;
    private boolean resolutionWarned;

    private int escalationLevel;
    private LocalDateTime lastEscalatedAt;

    @Column(length = 4000)
    private String resolution;

    @Column(length = 1000)
    private String rootCause;

    @Column(length = 1000)
    private String closureNotes;

    private Integer feedbackRating;

    @Column(length = 1000)
    private String feedbackComment;

    @ManyToOne(fetch = FetchType.LAZY)
    private Incident duplicateOf;

    private String suspectedDuplicateCode;

    private int reopenCount;

    /** Composition: notes cannot exist without their incident. */
    @OneToMany(mappedBy = "incident", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<IncidentNote> notes = new ArrayList<>();

    protected Incident() {
    }

    public Incident(String title, String description, User reporter, ItService service,
                    Impact impact, Urgency urgency, int affectedUsers) {
        this.title = Objects.requireNonNull(title).trim();
        this.description = Objects.requireNonNull(description).trim();
        this.reporter = Objects.requireNonNull(reporter);
        this.service = Objects.requireNonNull(service);
        this.impact = Objects.requireNonNull(impact);
        this.urgency = Objects.requireNonNull(urgency);
        this.affectedUsers = Math.max(1, affectedUsers);
        this.createdAt = LocalDateTime.now();
        this.updatedAt = createdAt;
    }

    public static Long idFromCode(String code) {
        String digits = code.toUpperCase().replace(CODE_PREFIX, "").trim();
        return Long.parseLong(digits) - CODE_OFFSET;
    }

    public String getCode() {
        return CODE_PREFIX + (CODE_OFFSET + (id == null ? 0 : id));
    }

    // ---------------------------------------------------------------- State pattern

    public IncidentState state() {
        return IncidentStates.of(status);
    }

    public Set<IncidentAction> allowedActions() {
        return state().allowedActions();
    }

    /** Only lifecycle state objects in this package may change the status. */
    void transitionTo(IncidentStatus next) {
        this.status = next;
        touch();
    }

    // ---------------------------------------------------------------- lifecycle operations

    public void classify(Category category, boolean automatic) {
        state().classify(this);
        this.category = Objects.requireNonNull(category);
        this.categorySuggestedAutomatically = automatic;
    }

    public void applyPriority(Priority priority, double score, String explanation) {
        this.priority = Objects.requireNonNull(priority);
        this.priorityScore = score;
        this.priorityExplanation = explanation;
        touch();
    }

    public void updateAssessment(Impact impact, Urgency urgency, Integer affectedUsers) {
        if (!isOpen()) {
            throw new InvalidStateTransitionException(status, IncidentAction.CLASSIFY);
        }
        this.impact = Objects.requireNonNull(impact);
        this.urgency = Objects.requireNonNull(urgency);
        if (affectedUsers != null) {
            this.affectedUsers = Math.max(1, affectedUsers);
        }
        touch();
    }

    public void applySlaTargets(int responseMinutes, int resolutionMinutes, int warningPercent) {
        this.responseTargetMinutes = responseMinutes;
        this.resolutionTargetMinutes = resolutionMinutes;
        this.slaWarningPercent = warningPercent;
        this.responseDueAt = createdAt.plusMinutes(responseMinutes);
        this.resolutionDueAt = createdAt.plusMinutes(resolutionMinutes);
        touch();
    }

    public void assignTo(Technician technician, String strategyName) {
        state().assign(this);
        this.assignedTechnician = Objects.requireNonNull(technician);
        this.assignmentStrategy = strategyName;
        this.assignedAt = LocalDateTime.now();
    }

    public void acknowledge() {
        state().acknowledge(this);
        if (acknowledgedAt == null) {
            acknowledgedAt = LocalDateTime.now();
        }
    }

    public void startWork() {
        state().startWork(this);
    }

    public void requestInfo() {
        state().requestInfo(this);
    }

    public void provideInfo() {
        state().provideInfo(this);
    }

    public void resolve(String resolution, String rootCause) {
        state().resolve(this);
        this.resolution = Objects.requireNonNull(resolution);
        this.rootCause = rootCause;
        this.resolvedAt = LocalDateTime.now();
        if (acknowledgedAt == null) {
            acknowledgedAt = resolvedAt;
        }
        if (!resolutionBreached && !responseBreached) {
            this.slaStatus = SlaStatus.MET;
        }
    }

    public void verify() {
        state().verify(this);
        this.verifiedAt = LocalDateTime.now();
    }

    public void reopen() {
        state().reopen(this);
        this.reopenCount++;
        this.resolvedAt = null;
        this.verifiedAt = null;
        if (slaStatus == SlaStatus.MET) {
            slaStatus = resolutionWarned ? SlaStatus.AT_RISK : SlaStatus.ON_TRACK;
        }
    }

    public void close(String closureNotes) {
        state().close(this);
        this.closureNotes = closureNotes;
        this.closedAt = LocalDateTime.now();
    }

    public void markDuplicateOf(Incident original) {
        if (original == null || Objects.equals(original.getId(), id)) {
            throw new IllegalArgumentException("An incident cannot be a duplicate of itself");
        }
        state().markDuplicate(this);
        this.duplicateOf = original;
        this.closureNotes = "Closed as duplicate of " + original.getCode();
        this.closedAt = LocalDateTime.now();
    }

    // ---------------------------------------------------------------- SLA tracking

    public void markResponseWarned() {
        responseWarned = true;
        refreshSlaStatus();
    }

    public void markResolutionWarned() {
        resolutionWarned = true;
        refreshSlaStatus();
    }

    public void markResponseBreached() {
        responseBreached = true;
        refreshSlaStatus();
    }

    public void markResolutionBreached() {
        resolutionBreached = true;
        refreshSlaStatus();
    }

    private void refreshSlaStatus() {
        if (responseBreached || resolutionBreached) {
            slaStatus = SlaStatus.BREACHED;
        } else if (responseWarned || resolutionWarned) {
            slaStatus = SlaStatus.AT_RISK;
        }
        touch();
    }

    public int escalate() {
        escalationLevel++;
        lastEscalatedAt = LocalDateTime.now();
        touch();
        return escalationLevel;
    }

    // ---------------------------------------------------------------- collaboration

    public IncidentNote addNote(User author, NoteType type, String text) {
        IncidentNote note = new IncidentNote(this, author, type, text);
        notes.add(note);
        touch();
        return note;
    }

    public void recordFeedback(int rating, String comment) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }
        this.feedbackRating = rating;
        this.feedbackComment = comment;
        touch();
    }

    public void flagSuspectedDuplicate(String code) {
        this.suspectedDuplicateCode = code;
    }

    public void changeCategory(Category category) {
        this.category = Objects.requireNonNull(category);
        this.categorySuggestedAutomatically = false;
        touch();
    }

    /** Moves the whole timeline into the past. Used only to generate realistic demo history. */
    public void shiftTimeline(Duration back) {
        createdAt = minus(createdAt, back);
        updatedAt = minus(updatedAt, back);
        assignedAt = minus(assignedAt, back);
        acknowledgedAt = minus(acknowledgedAt, back);
        resolvedAt = minus(resolvedAt, back);
        verifiedAt = minus(verifiedAt, back);
        closedAt = minus(closedAt, back);
        responseDueAt = minus(responseDueAt, back);
        resolutionDueAt = minus(resolutionDueAt, back);
        notes.forEach(n -> n.shiftTimeline(back));
    }

    private static LocalDateTime minus(LocalDateTime t, Duration d) {
        return t == null ? null : t.minus(d);
    }

    private void touch() {
        this.updatedAt = LocalDateTime.now();
    }

    // ---------------------------------------------------------------- derived values

    public boolean isOpen() {
        return status.isOpen();
    }

    public Long getResolutionMinutes() {
        if (resolvedAt == null) {
            return null;
        }
        return Duration.between(createdAt, resolvedAt).toMinutes();
    }

    // ---------------------------------------------------------------- getters

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getSearchableText() {
        return title + " " + description;
    }

    public User getReporter() {
        return reporter;
    }

    public Category getCategory() {
        return category;
    }

    public boolean isCategorySuggestedAutomatically() {
        return categorySuggestedAutomatically;
    }

    public ItService getService() {
        return service;
    }

    public Impact getImpact() {
        return impact;
    }

    public Urgency getUrgency() {
        return urgency;
    }

    public int getAffectedUsers() {
        return affectedUsers;
    }

    public Priority getPriority() {
        return priority;
    }

    public double getPriorityScore() {
        return priorityScore;
    }

    public String getPriorityExplanation() {
        return priorityExplanation;
    }

    public IncidentStatus getStatus() {
        return status;
    }

    public Technician getAssignedTechnician() {
        return assignedTechnician;
    }

    public String getAssignmentStrategy() {
        return assignmentStrategy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public LocalDateTime getAcknowledgedAt() {
        return acknowledgedAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public LocalDateTime getVerifiedAt() {
        return verifiedAt;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public int getResponseTargetMinutes() {
        return responseTargetMinutes;
    }

    public int getResolutionTargetMinutes() {
        return resolutionTargetMinutes;
    }

    public LocalDateTime getResponseDueAt() {
        return responseDueAt;
    }

    public LocalDateTime getResolutionDueAt() {
        return resolutionDueAt;
    }

    public int getSlaWarningPercent() {
        return slaWarningPercent;
    }

    public SlaStatus getSlaStatus() {
        return slaStatus;
    }

    public boolean isResponseBreached() {
        return responseBreached;
    }

    public boolean isResolutionBreached() {
        return resolutionBreached;
    }

    public boolean isResponseWarned() {
        return responseWarned;
    }

    public boolean isResolutionWarned() {
        return resolutionWarned;
    }

    public int getEscalationLevel() {
        return escalationLevel;
    }

    public LocalDateTime getLastEscalatedAt() {
        return lastEscalatedAt;
    }

    public String getResolution() {
        return resolution;
    }

    public String getRootCause() {
        return rootCause;
    }

    public String getClosureNotes() {
        return closureNotes;
    }

    public Integer getFeedbackRating() {
        return feedbackRating;
    }

    public String getFeedbackComment() {
        return feedbackComment;
    }

    public Incident getDuplicateOf() {
        return duplicateOf;
    }

    public String getSuspectedDuplicateCode() {
        return suspectedDuplicateCode;
    }

    public int getReopenCount() {
        return reopenCount;
    }

    public List<IncidentNote> getNotes() {
        return Collections.unmodifiableList(notes);
    }
}
