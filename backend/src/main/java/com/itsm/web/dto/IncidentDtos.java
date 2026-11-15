package com.itsm.web.dto;

import com.itsm.domain.audit.AuditRecord;
import com.itsm.domain.incident.Impact;
import com.itsm.domain.incident.Incident;
import com.itsm.domain.incident.IncidentAction;
import com.itsm.domain.incident.IncidentNote;
import com.itsm.domain.incident.IncidentStatus;
import com.itsm.domain.incident.Priority;
import com.itsm.domain.incident.SlaStatus;
import com.itsm.domain.incident.Urgency;
import com.itsm.domain.notification.Notification;
import com.itsm.domain.sla.SlaViolation;
import com.itsm.intelligence.DuplicateCandidate;
import com.itsm.web.dto.CatalogDtos.CategoryRef;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public final class IncidentDtos {

    private IncidentDtos() {
    }

    public record CreateIncidentRequest(
            @NotBlank @Size(min = 5, max = 150) String title,
            @NotBlank @Size(min = 10, max = 4000) String description,
            @NotNull Long serviceId,
            Long categoryId,
            @NotNull Impact impact,
            @NotNull Urgency urgency,
            @Min(1) @Max(1_000_000) Integer affectedUsers) {
    }

    public record CreateIncidentResponse(IncidentDetailDto incident, List<DuplicateCandidate> possibleDuplicates,
                                         String assignmentMessage) {
    }

    public record PersonRef(Long id, String userCode, String fullName, String role) {
    }

    public record IncidentSummaryDto(Long id, String code, String title, IncidentStatus status, Priority priority,
                                     CategoryRef category, Long serviceId, String serviceName,
                                     PersonRef reporter, PersonRef technician, SlaStatus slaStatus,
                                     LocalDateTime createdAt, LocalDateTime updatedAt,
                                     LocalDateTime responseDueAt, LocalDateTime resolutionDueAt,
                                     int escalationLevel, String suspectedDuplicateCode) {

        public static IncidentSummaryDto from(Incident i) {
            return new IncidentSummaryDto(i.getId(), i.getCode(), i.getTitle(), i.getStatus(), i.getPriority(),
                    CategoryRef.from(i.getCategory()), i.getService().getId(), i.getService().getName(),
                    person(i.getReporter()), person(i.getAssignedTechnician()), i.getSlaStatus(),
                    i.getCreatedAt(), i.getUpdatedAt(), i.getResponseDueAt(), i.getResolutionDueAt(),
                    i.getEscalationLevel(), i.getSuspectedDuplicateCode());
        }
    }

    public record NoteDto(Long id, String type, String text, boolean internal, PersonRef author,
                          LocalDateTime createdAt) {
        public static NoteDto from(IncidentNote n) {
            return new NoteDto(n.getId(), n.getType().name(), n.getText(), n.isInternal(), person(n.getAuthor()),
                    n.getCreatedAt());
        }
    }

    public record AuditDto(Long id, LocalDateTime timestamp, String actor, String action, String incidentCode,
                           String previousState, String newState, String description) {
        public static AuditDto from(AuditRecord a) {
            return new AuditDto(a.getId(), a.getTimestamp(), a.getActor(), a.getAction(), a.getIncidentCode(),
                    a.getPreviousState(), a.getNewState(), a.getDescription());
        }
    }

    public record IncidentDetailDto(IncidentSummaryDto summary,
                                    String description,
                                    boolean categorySuggestedAutomatically,
                                    Impact impact, Urgency urgency, int affectedUsers, int businessCriticality,
                                    double priorityScore, String priorityExplanation,
                                    String assignmentStrategy,
                                    LocalDateTime assignedAt, LocalDateTime acknowledgedAt,
                                    LocalDateTime resolvedAt, LocalDateTime verifiedAt, LocalDateTime closedAt,
                                    int responseTargetMinutes, int resolutionTargetMinutes,
                                    boolean responseBreached, boolean resolutionBreached,
                                    String resolution, String rootCause, String closureNotes,
                                    Integer feedbackRating, String feedbackComment,
                                    String duplicateOfCode, int reopenCount,
                                    Set<IncidentAction> allowedActions,
                                    List<NoteDto> notes,
                                    List<AuditDto> history) {
    }

    public record NoteRequest(@NotBlank @Size(max = 4000) String text, Boolean internal) {
    }

    public record AssignRequest(@NotNull Long technicianId) {
    }

    public record ResolveRequest(@NotBlank @Size(min = 10, max = 4000) String resolution,
                                 @Size(max = 1000) String rootCause) {
    }

    public record CloseRequest(@Size(max = 1000) String notes) {
    }

    public record ReasonRequest(@NotBlank @Size(max = 2000) String reason) {
    }

    public record FeedbackRequest(@Min(1) @Max(5) int rating, @Size(max = 1000) String comment) {
    }

    public record DuplicateRequest(@NotBlank String originalCode) {
    }

    public record ReprioritiseRequest(@NotNull Impact impact, @NotNull Urgency urgency,
                                      @Min(1) Integer affectedUsers, @NotBlank String reason) {
    }

    public record RecategoriseRequest(@NotNull Long categoryId, @NotBlank String reason) {
    }

    public record IncidentSearchCriteria(String code, IncidentStatus status, Priority priority, Long categoryId,
                                         Long serviceId, Long technicianId, SlaStatus slaStatus,
                                         LocalDate from, LocalDate to, String text, Boolean openOnly,
                                         Boolean unassignedOnly) {
    }

    public record NotificationDto(Long id, String channel, String destination, String subject, String message,
                                  String incidentCode, LocalDateTime createdAt, boolean read, String recipientName) {
        public static NotificationDto from(Notification n) {
            return new NotificationDto(n.getId(), n.getChannel().name(), n.getDestination(), n.getSubject(),
                    n.getMessage(), n.getIncidentCode(), n.getCreatedAt(), n.isRead(), n.getRecipient().getFullName());
        }
    }

    public record SlaViolationDto(Long id, String incidentCode, String incidentTitle, String type, Priority priority,
                                  LocalDateTime dueAt, LocalDateTime detectedAt, String technicianName,
                                  String currentStatus) {
        public static SlaViolationDto from(SlaViolation v) {
            return new SlaViolationDto(v.getId(), v.getIncident().getCode(), v.getIncident().getTitle(),
                    v.getType().name(), v.getPriority(), v.getDueAt(), v.getDetectedAt(), v.getTechnicianName(),
                    v.getIncident().getStatus().name());
        }
    }

    static PersonRef person(com.itsm.domain.user.User u) {
        return u == null ? null : new PersonRef(u.getId(), u.getUserCode(), u.getFullName(), u.getRole().name());
    }
}
