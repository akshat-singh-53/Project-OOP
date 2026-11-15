package com.itsm.web;

import com.itsm.domain.incident.Incident;
import com.itsm.domain.incident.IncidentStatus;
import com.itsm.domain.incident.Priority;
import com.itsm.domain.incident.SlaStatus;
import com.itsm.domain.user.Role;
import com.itsm.domain.user.User;
import com.itsm.intelligence.DuplicateCandidate;
import com.itsm.intelligence.ResolutionSuggestion;
import com.itsm.security.CurrentUser;
import com.itsm.security.RequiresRole;
import com.itsm.service.AssignmentService;
import com.itsm.service.IncidentService;
import com.itsm.service.IntelligenceService;
import com.itsm.strategy.assignment.TechnicianRecommendation;
import com.itsm.web.dto.IncidentDtos.AssignRequest;
import com.itsm.web.dto.IncidentDtos.CloseRequest;
import com.itsm.web.dto.IncidentDtos.CreateIncidentRequest;
import com.itsm.web.dto.IncidentDtos.CreateIncidentResponse;
import com.itsm.web.dto.IncidentDtos.DuplicateRequest;
import com.itsm.web.dto.IncidentDtos.FeedbackRequest;
import com.itsm.web.dto.IncidentDtos.IncidentDetailDto;
import com.itsm.web.dto.IncidentDtos.IncidentSearchCriteria;
import com.itsm.web.dto.IncidentDtos.IncidentSummaryDto;
import com.itsm.web.dto.IncidentDtos.NoteRequest;
import com.itsm.web.dto.IncidentDtos.ReasonRequest;
import com.itsm.web.dto.IncidentDtos.RecategoriseRequest;
import com.itsm.web.dto.IncidentDtos.ReprioritiseRequest;
import com.itsm.web.dto.IncidentDtos.ResolveRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/incidents")
public class IncidentController {

    private final IncidentService incidents;
    private final IntelligenceService intelligence;
    private final AssignmentService assignment;
    private final IncidentMapper mapper;

    public IncidentController(IncidentService incidents, IntelligenceService intelligence,
                              AssignmentService assignment, IncidentMapper mapper) {
        this.incidents = incidents;
        this.intelligence = intelligence;
        this.assignment = assignment;
        this.mapper = mapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateIncidentResponse create(@Valid @RequestBody CreateIncidentRequest request, @CurrentUser User user) {
        IncidentService.CreationResult result = incidents.create(request, user);
        return new CreateIncidentResponse(mapper.toDetail(result.incident(), user), result.possibleDuplicates(),
                result.assignmentMessage());
    }

    @GetMapping
    public List<IncidentSummaryDto> search(@CurrentUser User user,
                                           @RequestParam(required = false) String code,
                                           @RequestParam(required = false) IncidentStatus status,
                                           @RequestParam(required = false) Priority priority,
                                           @RequestParam(required = false) Long categoryId,
                                           @RequestParam(required = false) Long serviceId,
                                           @RequestParam(required = false) Long technicianId,
                                           @RequestParam(required = false) SlaStatus slaStatus,
                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                           @RequestParam(required = false) String text,
                                           @RequestParam(required = false) Boolean openOnly,
                                           @RequestParam(required = false) Boolean unassignedOnly) {
        var criteria = new IncidentSearchCriteria(code, status, priority, categoryId, serviceId, technicianId,
                slaStatus, from, to, text, openOnly, unassignedOnly);
        return incidents.search(criteria, user).stream().map(IncidentSummaryDto::from).toList();
    }

    @GetMapping("/{id}")
    public IncidentDetailDto get(@PathVariable Long id, @CurrentUser User user) {
        return mapper.toDetail(incidents.getVisible(id, user), user);
    }

    @GetMapping("/code/{code}")
    public IncidentDetailDto getByCode(@PathVariable String code, @CurrentUser User user) {
        return mapper.toDetail(incidents.getVisibleByCode(code, user), user);
    }

    // ------------------------------------------------------------------ lifecycle

    @PostMapping("/{id}/assign")
    @RequiresRole({Role.ADMIN, Role.TECHNICIAN})
    public IncidentDetailDto assign(@PathVariable Long id, @Valid @RequestBody AssignRequest r, @CurrentUser User user) {
        return mapper.toDetail(incidents.assign(id, r.technicianId(), user), user);
    }

    @PostMapping("/{id}/take")
    @RequiresRole(Role.TECHNICIAN)
    public IncidentDetailDto take(@PathVariable Long id, @CurrentUser User user) {
        return mapper.toDetail(incidents.assign(id, user.getId(), user), user);
    }

    @PostMapping("/{id}/acknowledge")
    @RequiresRole({Role.ADMIN, Role.TECHNICIAN})
    public IncidentDetailDto acknowledge(@PathVariable Long id, @CurrentUser User user) {
        return mapper.toDetail(incidents.acknowledge(id, user), user);
    }

    @PostMapping("/{id}/start")
    @RequiresRole({Role.ADMIN, Role.TECHNICIAN})
    public IncidentDetailDto start(@PathVariable Long id, @CurrentUser User user) {
        return mapper.toDetail(incidents.startWork(id, user), user);
    }

    @PostMapping("/{id}/notes")
    public IncidentDetailDto addNote(@PathVariable Long id, @Valid @RequestBody NoteRequest r, @CurrentUser User user) {
        return mapper.toDetail(incidents.addNote(id, r.text(), Boolean.TRUE.equals(r.internal()), user), user);
    }

    @PostMapping("/{id}/request-info")
    @RequiresRole({Role.ADMIN, Role.TECHNICIAN})
    public IncidentDetailDto requestInfo(@PathVariable Long id, @Valid @RequestBody ReasonRequest r,
                                         @CurrentUser User user) {
        return mapper.toDetail(incidents.requestInfo(id, r.reason(), user), user);
    }

    @PostMapping("/{id}/provide-info")
    public IncidentDetailDto provideInfo(@PathVariable Long id, @Valid @RequestBody ReasonRequest r,
                                         @CurrentUser User user) {
        return mapper.toDetail(incidents.provideInfo(id, r.reason(), user), user);
    }

    @PostMapping("/{id}/resolve")
    @RequiresRole({Role.ADMIN, Role.TECHNICIAN})
    public IncidentDetailDto resolve(@PathVariable Long id, @Valid @RequestBody ResolveRequest r, @CurrentUser User user) {
        return mapper.toDetail(incidents.resolve(id, r.resolution(), r.rootCause(), user), user);
    }

    @PostMapping("/{id}/verify")
    public IncidentDetailDto verify(@PathVariable Long id, @CurrentUser User user) {
        return mapper.toDetail(incidents.verify(id, user), user);
    }

    @PostMapping("/{id}/reopen")
    public IncidentDetailDto reopen(@PathVariable Long id, @Valid @RequestBody ReasonRequest r, @CurrentUser User user) {
        return mapper.toDetail(incidents.reopen(id, r.reason(), user), user);
    }

    @PostMapping("/{id}/close")
    @RequiresRole({Role.ADMIN, Role.TECHNICIAN})
    public IncidentDetailDto close(@PathVariable Long id, @Valid @RequestBody CloseRequest r, @CurrentUser User user) {
        return mapper.toDetail(incidents.close(id, r.notes(), user), user);
    }

    @PostMapping("/{id}/duplicate")
    @RequiresRole({Role.ADMIN, Role.TECHNICIAN})
    public IncidentDetailDto markDuplicate(@PathVariable Long id, @Valid @RequestBody DuplicateRequest r,
                                           @CurrentUser User user) {
        return mapper.toDetail(incidents.markDuplicate(id, r.originalCode(), user), user);
    }

    @PostMapping("/{id}/feedback")
    @RequiresRole(Role.EMPLOYEE)
    public IncidentDetailDto feedback(@PathVariable Long id, @Valid @RequestBody FeedbackRequest r,
                                      @CurrentUser User user) {
        return mapper.toDetail(incidents.feedback(id, r.rating(), r.comment(), user), user);
    }

    @PostMapping("/{id}/reprioritise")
    @RequiresRole({Role.ADMIN, Role.TECHNICIAN})
    public IncidentDetailDto reprioritise(@PathVariable Long id, @Valid @RequestBody ReprioritiseRequest r,
                                          @CurrentUser User user) {
        return mapper.toDetail(incidents.reprioritise(id, r.impact(), r.urgency(), r.affectedUsers(), r.reason(), user), user);
    }

    @PostMapping("/{id}/recategorise")
    @RequiresRole({Role.ADMIN, Role.TECHNICIAN})
    public IncidentDetailDto recategorise(@PathVariable Long id, @Valid @RequestBody RecategoriseRequest r,
                                          @CurrentUser User user) {
        return mapper.toDetail(incidents.recategorise(id, r.categoryId(), r.reason(), user), user);
    }

    // ------------------------------------------------------------------ intelligence per incident

    @GetMapping("/{id}/recommendations")
    @RequiresRole({Role.ADMIN, Role.TECHNICIAN})
    public List<TechnicianRecommendation> recommendations(@PathVariable Long id,
                                                          @RequestParam(required = false) String strategy,
                                                          @CurrentUser User user) {
        Incident incident = incidents.getVisible(id, user);
        return assignment.recommend(incident, strategy);
    }

    @GetMapping("/{id}/suggested-resolutions")
    @RequiresRole({Role.ADMIN, Role.TECHNICIAN})
    public List<ResolutionSuggestion> suggestedResolutions(@PathVariable Long id, @CurrentUser User user) {
        return intelligence.suggestResolutions(incidents.getVisible(id, user));
    }

    @GetMapping("/{id}/duplicates")
    @RequiresRole({Role.ADMIN, Role.TECHNICIAN})
    public List<DuplicateCandidate> duplicates(@PathVariable Long id, @CurrentUser User user) {
        Incident incident = incidents.getVisible(id, user);
        return intelligence.findDuplicates(incident.getSearchableText(), incident.getService().getId(), incident.getId());
    }
}
