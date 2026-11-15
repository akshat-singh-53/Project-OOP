package com.itsm.service;

import com.itsm.domain.catalog.Category;
import com.itsm.domain.catalog.ItService;
import com.itsm.domain.incident.Incident;
import com.itsm.domain.incident.IncidentAction;
import com.itsm.domain.incident.IncidentStatus;
import com.itsm.domain.incident.NoteType;
import com.itsm.domain.incident.Priority;
import com.itsm.domain.sla.SlaPolicy;
import com.itsm.domain.user.Role;
import com.itsm.domain.user.Technician;
import com.itsm.domain.user.User;
import com.itsm.event.IncidentEvent;
import com.itsm.event.IncidentEventPublisher;
import com.itsm.event.IncidentEventType;
import com.itsm.exception.DuplicateIncidentException;
import com.itsm.exception.ResourceNotFoundException;
import com.itsm.exception.TechnicianUnavailableException;
import com.itsm.exception.UnauthorizedOperationException;
import com.itsm.exception.ValidationException;
import com.itsm.intelligence.CategorySuggestion;
import com.itsm.intelligence.DuplicateCandidate;
import com.itsm.intelligence.DuplicateDetector;
import com.itsm.repository.IncidentRepository;
import com.itsm.strategy.priority.PriorityResult;
import com.itsm.web.dto.IncidentDtos.CreateIncidentRequest;
import com.itsm.web.dto.IncidentDtos.IncidentSearchCriteria;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Application service that orchestrates the incident lifecycle. Domain rules live in
 * {@link Incident} and its state objects; this class handles permissions, collaborators and events.
 */
@Service
public class IncidentService {

    private static final int RESUBMIT_WINDOW_MINUTES = 10;

    private final IncidentRepository incidents;
    private final CatalogService catalog;
    private final PriorityService priorityService;
    private final SlaService slaService;
    private final AssignmentService assignmentService;
    private final IntelligenceService intelligence;
    private final UserService userService;
    private final SettingsService settings;
    private final IncidentEventPublisher events;

    public IncidentService(IncidentRepository incidents, CatalogService catalog, PriorityService priorityService,
                           SlaService slaService, AssignmentService assignmentService,
                           IntelligenceService intelligence, UserService userService, SettingsService settings,
                           IncidentEventPublisher events) {
        this.incidents = incidents;
        this.catalog = catalog;
        this.priorityService = priorityService;
        this.slaService = slaService;
        this.assignmentService = assignmentService;
        this.intelligence = intelligence;
        this.userService = userService;
        this.settings = settings;
        this.events = events;
    }

    public record CreationResult(Incident incident, List<DuplicateCandidate> possibleDuplicates,
                                 String assignmentMessage) {
    }

    // =================================================================== creation pipeline

    /** Report -> Classify -> Prioritise -> SLA -> Duplicate check -> Assign -> Notify. */
    @Transactional
    public CreationResult create(CreateIncidentRequest request, User reporter) {
        String title = request.title().trim();
        incidents.findByReporterAndTitleIgnoreCaseAndCreatedAtAfter(reporter, title,
                        LocalDateTime.now().minusMinutes(RESUBMIT_WINDOW_MINUTES)).stream()
                .findFirst()
                .ifPresent(existing -> {
                    throw new DuplicateIncidentException(existing.getCode());
                });

        ItService service = catalog.service(request.serviceId());
        if (!service.isActive()) {
            throw new ValidationException("Service " + service.getName() + " is no longer supported");
        }
        Incident incident = incidents.save(new Incident(title, request.description(), reporter, service,
                request.impact(), request.urgency(), request.affectedUsers() == null ? 1 : request.affectedUsers()));
        publish(IncidentEventType.CREATED, incident, reporter.getUsername(), null,
                incident.getCode() + " created by " + reporter.getUserCode() + " for service " + service.getName());

        classifyNew(incident, request.categoryId());

        PriorityResult priority = priorityService.calculate(incident);
        incident.applyPriority(priority.priority(), priority.score(), priority.explanation());
        publish(IncidentEventType.PRIORITY_CALCULATED, incident, IncidentEvent.SYSTEM_ACTOR, incident.getStatus(),
                "Priority calculated as " + priority.priority() + " using " + priorityService.current().displayName()
                        + ": " + priority.explanation());

        SlaPolicy policy = slaService.applyTo(incident);
        publish(IncidentEventType.SLA_APPLIED, incident, IncidentEvent.SYSTEM_ACTOR, incident.getStatus(),
                "SLA '" + policy.getName() + "': respond within " + policy.getResponseMinutes()
                        + " min, resolve within " + policy.getResolutionMinutes() + " min");

        List<DuplicateCandidate> duplicates = intelligence.findDuplicates(incident.getSearchableText(),
                service.getId(), incident.getId());
        if (!duplicates.isEmpty() && duplicates.get(0).score() >= DuplicateDetector.STRONG_MATCH_THRESHOLD) {
            DuplicateCandidate top = duplicates.get(0);
            incident.flagSuspectedDuplicate(top.incidentCode());
            publish(IncidentEventType.DUPLICATE_SUSPECTED, incident, IncidentEvent.SYSTEM_ACTOR, incident.getStatus(),
                    "Possibly the same problem as " + top.incidentCode() + " (similarity score " + top.score() + ")");
        }

        String assignmentMessage = autoAssign(incident);
        return new CreationResult(incident, duplicates, assignmentMessage);
    }

    private void classifyNew(Incident incident, Long categoryId) {
        IncidentStatus previous = incident.getStatus();
        if (categoryId != null) {
            incident.classify(catalog.category(categoryId), false);
            publish(IncidentEventType.CLASSIFIED, incident, incident.getReporter().getUsername(), previous,
                    "Incident classified as " + incident.getCategory().getName() + " (selected by reporter)");
            return;
        }
        List<CategorySuggestion> suggestions = intelligence.suggestCategories(incident.getSearchableText(),
                incident.getService().getId());
        if (!suggestions.isEmpty()) {
            CategorySuggestion best = suggestions.get(0);
            incident.classify(catalog.category(best.categoryId()), true);
            publish(IncidentEventType.CLASSIFIED, incident, IncidentEvent.SYSTEM_ACTOR, previous,
                    "Incident automatically classified as " + best.name() + " (confidence "
                            + Math.round(best.confidence() * 100) + "%, " + best.source() + ")");
            return;
        }
        Category fallback = incident.getService().getDefaultCategory();
        if (fallback == null) {
            fallback = catalog.categories(true).stream().findFirst()
                    .orElseThrow(() -> new ValidationException("No incident categories are configured"));
        }
        incident.classify(fallback, true);
        publish(IncidentEventType.CLASSIFIED, incident, IncidentEvent.SYSTEM_ACTOR, previous,
                "Incident classified as " + fallback.getName() + " (default category of the service)");
    }

    private String autoAssign(Incident incident) {
        if (!settings.getBoolean(SettingsService.AUTO_ASSIGN)) {
            return "Waiting in the queue for manual assignment";
        }
        IncidentStatus previous = incident.getStatus();
        try {
            Technician technician = assignmentService.selectAutomatically(incident);
            incident.assignTo(technician, assignmentService.current().key());
            publish(IncidentEventType.ASSIGNED, incident, IncidentEvent.SYSTEM_ACTOR, previous,
                    "Assigned to " + technician.getUserCode() + " " + technician.getFullName() + " by "
                            + assignmentService.current().displayName() + " strategy");
            return "Assigned to " + technician.getFullName();
        } catch (TechnicianUnavailableException ex) {
            publish(IncidentEventType.ASSIGNMENT_FAILED, incident, IncidentEvent.SYSTEM_ACTOR, previous,
                    ex.getMessage() + ". Manual assignment required.");
            return ex.getMessage() + " - an administrator will assign it shortly";
        }
    }

    // =================================================================== queries

    @Transactional(readOnly = true)
    public Incident getVisible(Long id, User viewer) {
        Incident incident = find(id);
        if (viewer.getRole() == Role.EMPLOYEE && !incident.getReporter().getId().equals(viewer.getId())) {
            throw new UnauthorizedOperationException("You can only view incidents you reported");
        }
        return incident;
    }

    @Transactional(readOnly = true)
    public Incident getVisibleByCode(String code, User viewer) {
        try {
            return getVisible(Incident.idFromCode(code), viewer);
        } catch (NumberFormatException ex) {
            throw new ResourceNotFoundException("Incident", code);
        }
    }

    private Incident find(Long id) {
        return incidents.findById(id).orElseThrow(() -> new ResourceNotFoundException("Incident", id));
    }

    @Transactional(readOnly = true)
    public List<Incident> search(IncidentSearchCriteria c, User viewer) {
        Specification<Incident> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (viewer.getRole() == Role.EMPLOYEE) {
                p.add(cb.equal(root.get("reporter").get("id"), viewer.getId()));
            }
            if (c.code() != null && !c.code().isBlank()) {
                try {
                    p.add(cb.equal(root.get("id"), Incident.idFromCode(c.code())));
                } catch (NumberFormatException ex) {
                    p.add(cb.disjunction());
                }
            }
            if (c.status() != null) p.add(cb.equal(root.get("status"), c.status()));
            if (c.priority() != null) p.add(cb.equal(root.get("priority"), c.priority()));
            if (c.slaStatus() != null) p.add(cb.equal(root.get("slaStatus"), c.slaStatus()));
            if (c.categoryId() != null) p.add(cb.equal(root.get("category").get("id"), c.categoryId()));
            if (c.serviceId() != null) p.add(cb.equal(root.get("service").get("id"), c.serviceId()));
            if (c.technicianId() != null) p.add(cb.equal(root.get("assignedTechnician").get("id"), c.technicianId()));
            if (c.from() != null) p.add(cb.greaterThanOrEqualTo(root.get("createdAt"), c.from().atStartOfDay()));
            if (c.to() != null) p.add(cb.lessThan(root.get("createdAt"), c.to().plusDays(1).atStartOfDay()));
            if (Boolean.TRUE.equals(c.openOnly())) p.add(root.get("status").in(IntelligenceService.OPEN));
            if (Boolean.TRUE.equals(c.unassignedOnly())) p.add(cb.isNull(root.get("assignedTechnician")));
            if (c.text() != null && !c.text().isBlank()) {
                String like = "%" + c.text().trim().toLowerCase(Locale.ROOT) + "%";
                p.add(cb.or(cb.like(cb.lower(root.get("title")), like),
                        cb.like(cb.lower(root.get("description")), like)));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
        return incidents.findAll(spec, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    // =================================================================== permissions

    /** Actions this particular user may perform right now (state rules AND role rules). */
    public Set<IncidentAction> actionsFor(Incident incident, User user) {
        Set<IncidentAction> result = EnumSet.noneOf(IncidentAction.class);
        for (IncidentAction action : incident.allowedActions()) {
            if (isPermitted(incident, user, action)) {
                result.add(action);
            }
        }
        return result;
    }

    boolean isPermitted(Incident incident, User user, IncidentAction action) {
        boolean admin = user.getRole() == Role.ADMIN;
        boolean technician = user instanceof Technician;
        boolean teamLead = user instanceof Technician t && t.isTeamLead();
        boolean assignee = incident.getAssignedTechnician() != null
                && incident.getAssignedTechnician().getId().equals(user.getId());
        boolean reporter = incident.getReporter().getId().equals(user.getId());
        boolean unassigned = incident.getAssignedTechnician() == null;

        return switch (action) {
            case CLASSIFY, MARK_DUPLICATE -> admin || technician;
            case ASSIGN -> admin || teamLead || (technician && unassigned);
            case ACKNOWLEDGE, START_WORK, REQUEST_INFO, RESOLVE -> admin || assignee;
            case PROVIDE_INFO -> reporter || admin;
            case VERIFY, REOPEN -> reporter || admin;
            case CLOSE -> admin || assignee || teamLead;
        };
    }

    private void require(Incident incident, User user, IncidentAction action) {
        if (!isPermitted(incident, user, action)) {
            throw new UnauthorizedOperationException("You are not allowed to "
                    + action.name().toLowerCase(Locale.ROOT).replace('_', ' ') + " incident " + incident.getCode());
        }
    }

    // =================================================================== lifecycle commands

    @Transactional
    public Incident assign(Long id, Long technicianId, User actor) {
        Incident incident = find(id);
        require(incident, actor, IncidentAction.ASSIGN);
        Technician technician = userService.getTechnician(technicianId);
        if (actor instanceof Technician self && !self.isTeamLead() && !self.getId().equals(technicianId)) {
            throw new UnauthorizedOperationException("Technicians can only take unassigned incidents for themselves");
        }
        if (Objects.equals(incident.getAssignedTechnician(), technician)) {
            throw new ValidationException(technician.getFullName() + " is already assigned to " + incident.getCode());
        }
        assignmentService.validateManualChoice(technician);
        IncidentStatus previous = incident.getStatus();
        String previousTech = incident.getAssignedTechnician() == null ? null : incident.getAssignedTechnician().getFullName();
        incident.assignTo(technician, "manual");
        publish(IncidentEventType.ASSIGNED, incident, actor.getUsername(), previous,
                (previousTech == null ? "Assigned to " : "Reassigned from " + previousTech + " to ")
                        + technician.getUserCode() + " " + technician.getFullName() + " by " + actor.getFullName());
        return incident;
    }

    @Transactional
    public Incident acknowledge(Long id, User actor) {
        Incident incident = find(id);
        require(incident, actor, IncidentAction.ACKNOWLEDGE);
        IncidentStatus previous = incident.getStatus();
        incident.acknowledge();
        publish(IncidentEventType.ACKNOWLEDGED, incident, actor.getUsername(), previous,
                "Technician " + actor.getUserCode() + " acknowledged the incident");
        return incident;
    }

    @Transactional
    public Incident startWork(Long id, User actor) {
        Incident incident = find(id);
        require(incident, actor, IncidentAction.START_WORK);
        IncidentStatus previous = incident.getStatus();
        incident.startWork();
        publish(IncidentEventType.WORK_STARTED, incident, actor.getUsername(), previous, "Investigation started");
        return incident;
    }

    @Transactional
    public Incident addNote(Long id, String text, boolean internal, User actor) {
        Incident incident = getVisible(id, actor);
        if (!incident.isOpen() && incident.getStatus() == IncidentStatus.CLOSED) {
            throw new ValidationException("Closed incidents cannot be commented on");
        }
        boolean staff = actor.getRole() != Role.EMPLOYEE;
        NoteType type = staff && internal ? NoteType.INVESTIGATION : NoteType.COMMENT;
        incident.addNote(actor, type, text);
        publish(IncidentEventType.NOTE_ADDED, incident, actor.getUsername(), incident.getStatus(),
                (type == NoteType.INVESTIGATION ? "Investigation note: " : "Comment: ") + abbreviate(text));
        return incident;
    }

    @Transactional
    public Incident requestInfo(Long id, String question, User actor) {
        Incident incident = find(id);
        require(incident, actor, IncidentAction.REQUEST_INFO);
        IncidentStatus previous = incident.getStatus();
        incident.requestInfo();
        incident.addNote(actor, NoteType.INFO_REQUEST, question);
        publish(IncidentEventType.INFO_REQUESTED, incident, actor.getUsername(), previous, question);
        return incident;
    }

    @Transactional
    public Incident provideInfo(Long id, String answer, User actor) {
        Incident incident = find(id);
        require(incident, actor, IncidentAction.PROVIDE_INFO);
        IncidentStatus previous = incident.getStatus();
        incident.provideInfo();
        incident.addNote(actor, NoteType.INFO_RESPONSE, answer);
        publish(IncidentEventType.INFO_PROVIDED, incident, actor.getUsername(), previous, abbreviate(answer));
        return incident;
    }

    @Transactional
    public Incident resolve(Long id, String resolution, String rootCause, User actor) {
        Incident incident = find(id);
        require(incident, actor, IncidentAction.RESOLVE);
        IncidentStatus previous = incident.getStatus();
        incident.resolve(resolution.trim(), rootCause == null ? null : rootCause.trim());
        incident.addNote(actor, NoteType.RESOLUTION, resolution);
        publish(IncidentEventType.RESOLVED, incident, actor.getUsername(), previous,
                "Resolution provided: " + abbreviate(resolution));
        return incident;
    }

    @Transactional
    public Incident verify(Long id, User actor) {
        Incident incident = find(id);
        require(incident, actor, IncidentAction.VERIFY);
        IncidentStatus previous = incident.getStatus();
        incident.verify();
        publish(IncidentEventType.VERIFIED, incident, actor.getUsername(), previous,
                actor.getFullName() + " confirmed the resolution works");
        return incident;
    }

    @Transactional
    public Incident reopen(Long id, String reason, User actor) {
        Incident incident = find(id);
        require(incident, actor, IncidentAction.REOPEN);
        IncidentStatus previous = incident.getStatus();
        incident.reopen();
        incident.addNote(actor, NoteType.COMMENT, "Reopened: " + reason);
        publish(IncidentEventType.REOPENED, incident, actor.getUsername(), previous, "Reopened: " + reason);
        return incident;
    }

    @Transactional
    public Incident close(Long id, String notes, User actor) {
        Incident incident = find(id);
        require(incident, actor, IncidentAction.CLOSE);
        IncidentStatus previous = incident.getStatus();
        incident.close(notes);
        publish(IncidentEventType.CLOSED, incident, actor.getUsername(), previous,
                "Incident closed" + (notes == null || notes.isBlank() ? "" : ": " + notes));
        return incident;
    }

    @Transactional
    public Incident markDuplicate(Long id, String originalCode, User actor) {
        Incident incident = find(id);
        require(incident, actor, IncidentAction.MARK_DUPLICATE);
        Incident original;
        try {
            original = find(Incident.idFromCode(originalCode));
        } catch (NumberFormatException ex) {
            throw new ValidationException("Invalid incident code: " + originalCode);
        }
        IncidentStatus previous = incident.getStatus();
        try {
            incident.markDuplicateOf(original);
        } catch (IllegalArgumentException ex) {
            throw new ValidationException(ex.getMessage());
        }
        publish(IncidentEventType.MARKED_DUPLICATE, incident, actor.getUsername(), previous,
                "Closed as duplicate of " + original.getCode());
        original.addNote(actor, NoteType.COMMENT, incident.getCode() + " was linked as a duplicate of this incident");
        return incident;
    }

    @Transactional
    public Incident feedback(Long id, int rating, String comment, User actor) {
        Incident incident = find(id);
        if (!incident.getReporter().getId().equals(actor.getId())) {
            throw new UnauthorizedOperationException("Only the reporter can give feedback");
        }
        if (incident.isOpen()) {
            throw new ValidationException("Feedback can be given once the incident has been resolved");
        }
        try {
            incident.recordFeedback(rating, comment);
        } catch (IllegalArgumentException ex) {
            throw new ValidationException(ex.getMessage());
        }
        publish(IncidentEventType.FEEDBACK_RECEIVED, incident, actor.getUsername(), incident.getStatus(),
                "Rated " + rating + "/5" + (comment == null || comment.isBlank() ? "" : ": " + comment));
        return incident;
    }

    @Transactional
    public Incident reprioritise(Long id, com.itsm.domain.incident.Impact impact,
                                 com.itsm.domain.incident.Urgency urgency, Integer affectedUsers, String reason,
                                 User actor) {
        Incident incident = find(id);
        require(incident, actor, IncidentAction.CLASSIFY);
        Priority before = incident.getPriority();
        incident.updateAssessment(impact, urgency, affectedUsers);
        PriorityResult result = priorityService.calculate(incident);
        incident.applyPriority(result.priority(), result.score(), result.explanation());
        slaService.applyTo(incident);
        publish(IncidentEventType.PRIORITY_CHANGED, incident, actor.getUsername(), incident.getStatus(),
                "Priority re-assessed " + before + " -> " + result.priority() + " (" + reason + "). " + result.explanation());
        return incident;
    }

    @Transactional
    public Incident recategorise(Long id, Long categoryId, String reason, User actor) {
        Incident incident = find(id);
        require(incident, actor, IncidentAction.CLASSIFY);
        if (!incident.isOpen()) {
            throw new ValidationException("Only open incidents can be re-categorised");
        }
        Category category = catalog.category(categoryId);
        String before = incident.getCategory() == null ? "none" : incident.getCategory().getName();
        incident.changeCategory(category);
        publish(IncidentEventType.CATEGORY_CHANGED, incident, actor.getUsername(), incident.getStatus(),
                "Category changed from " + before + " to " + category.getName() + " (" + reason + ")");
        return incident;
    }

    // =================================================================== helpers

    void publish(IncidentEventType type, Incident incident, String actor, IncidentStatus previous, String description) {
        events.publish(IncidentEvent.of(type, incident, actor, previous, description));
    }

    private static String abbreviate(String text) {
        String t = text.trim();
        return t.length() > 140 ? t.substring(0, 137) + "..." : t;
    }
}
