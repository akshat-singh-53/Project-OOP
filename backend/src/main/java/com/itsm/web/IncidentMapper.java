package com.itsm.web;

import com.itsm.domain.incident.Incident;
import com.itsm.domain.user.Role;
import com.itsm.domain.user.User;
import com.itsm.service.AuditService;
import com.itsm.service.IncidentService;
import com.itsm.web.dto.IncidentDtos.AuditDto;
import com.itsm.web.dto.IncidentDtos.IncidentDetailDto;
import com.itsm.web.dto.IncidentDtos.IncidentSummaryDto;
import com.itsm.web.dto.IncidentDtos.NoteDto;
import org.springframework.stereotype.Component;

/** Builds the detail view; hides internal investigation notes from employees. */
@Component
public class IncidentMapper {

    private final IncidentService incidentService;
    private final AuditService auditService;

    public IncidentMapper(IncidentService incidentService, AuditService auditService) {
        this.incidentService = incidentService;
        this.auditService = auditService;
    }

    public IncidentDetailDto toDetail(Incident i, User viewer) {
        boolean staff = viewer.getRole() != Role.EMPLOYEE;
        return new IncidentDetailDto(
                IncidentSummaryDto.from(i),
                i.getDescription(),
                i.isCategorySuggestedAutomatically(),
                i.getImpact(), i.getUrgency(), i.getAffectedUsers(), i.getService().getBusinessCriticality(),
                i.getPriorityScore(), i.getPriorityExplanation(),
                i.getAssignmentStrategy(),
                i.getAssignedAt(), i.getAcknowledgedAt(), i.getResolvedAt(), i.getVerifiedAt(), i.getClosedAt(),
                i.getResponseTargetMinutes(), i.getResolutionTargetMinutes(),
                i.isResponseBreached(), i.isResolutionBreached(),
                i.getResolution(), i.getRootCause(), i.getClosureNotes(),
                i.getFeedbackRating(), i.getFeedbackComment(),
                i.getDuplicateOf() == null ? null : i.getDuplicateOf().getCode(),
                i.getReopenCount(),
                incidentService.actionsFor(i, viewer),
                i.getNotes().stream().filter(n -> staff || !n.isInternal()).map(NoteDto::from).toList(),
                auditService.forIncident(i.getCode()).stream()
                        .filter(a -> staff || !"NOTE_ADDED".equals(a.getAction()) || !a.getDescription().startsWith("Investigation"))
                        .map(AuditDto::from).toList());
    }
}
