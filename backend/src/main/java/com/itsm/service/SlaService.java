package com.itsm.service;

import com.itsm.domain.catalog.ItService;
import com.itsm.domain.incident.Incident;
import com.itsm.domain.incident.Priority;
import com.itsm.domain.sla.SlaPolicy;
import com.itsm.exception.ResourceNotFoundException;
import com.itsm.exception.SlaConfigurationMissingException;
import com.itsm.exception.ValidationException;
import com.itsm.repository.SlaPolicyRepository;
import com.itsm.repository.SlaViolationRepository;
import com.itsm.domain.sla.SlaViolation;
import com.itsm.web.dto.CatalogDtos.SlaPolicyRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SlaService {

    private final SlaPolicyRepository policies;
    private final SlaViolationRepository violations;
    private final CatalogService catalog;
    private final AuditService audit;

    public SlaService(SlaPolicyRepository policies, SlaViolationRepository violations, CatalogService catalog,
                      AuditService audit) {
        this.policies = policies;
        this.violations = violations;
        this.catalog = catalog;
        this.audit = audit;
    }

    /** Service-specific policy wins over the organisation-wide default for the same priority. */
    @Transactional(readOnly = true)
    public SlaPolicy resolvePolicy(Priority priority, ItService service) {
        return policies.findFirstByPriorityAndServiceAndActiveTrue(priority, service)
                .or(() -> policies.findFirstByPriorityAndServiceIsNullAndActiveTrue(priority))
                .orElseThrow(() -> new SlaConfigurationMissingException(priority, service.getName()));
    }

    public SlaPolicy applyTo(Incident incident) {
        SlaPolicy policy = resolvePolicy(incident.getPriority(), incident.getService());
        incident.applySlaTargets(policy.getResponseMinutes(), policy.getResolutionMinutes(),
                policy.getWarningThresholdPercent());
        return policy;
    }

    @Transactional(readOnly = true)
    public List<SlaPolicy> list() {
        return policies.findAll(Sort.by("priority", "id"));
    }

    @Transactional
    public SlaPolicy create(SlaPolicyRequest r, String actor) {
        ItService service = r.serviceId() == null ? null : catalog.service(r.serviceId());
        SlaPolicy p = policies.save(build(r, service));
        audit.recordAdminAction(actor, "SLA_POLICY_CREATED", "Created SLA policy " + p.getName());
        return p;
    }

    @Transactional
    public SlaPolicy update(Long id, SlaPolicyRequest r, String actor) {
        SlaPolicy p = policies.findById(id).orElseThrow(() -> new ResourceNotFoundException("SLA policy", id));
        try {
            p.update(r.name(), r.responseMinutes(), r.resolutionMinutes(), r.warningThresholdPercent(),
                    r.active() == null || r.active());
        } catch (IllegalArgumentException ex) {
            throw new ValidationException(ex.getMessage());
        }
        audit.recordAdminAction(actor, "SLA_POLICY_UPDATED", "Updated SLA policy " + p.getName());
        return p;
    }

    private static SlaPolicy build(SlaPolicyRequest r, ItService service) {
        try {
            return new SlaPolicy(r.name(), r.priority(), service, r.responseMinutes(), r.resolutionMinutes(),
                    r.warningThresholdPercent());
        } catch (IllegalArgumentException ex) {
            throw new ValidationException(ex.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<SlaViolation> violations() {
        return violations.findAllByOrderByDetectedAtDesc();
    }
}
