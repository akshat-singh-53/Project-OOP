package com.itsm.service;

import com.itsm.domain.audit.AuditRecord;
import com.itsm.repository.AuditRecordRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuditService {

    private final AuditRecordRepository repository;

    public AuditService(AuditRecordRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void record(LocalDateTime at, String actor, String action, String incidentCode,
                       String previousState, String newState, String description) {
        repository.save(new AuditRecord(at, actor, action, incidentCode, previousState, newState, description));
    }

    /** Administrative actions that are not tied to a single incident. */
    @Transactional
    public void recordAdminAction(String actor, String action, String description) {
        record(LocalDateTime.now(), actor, action, null, null, null, description);
    }

    @Transactional(readOnly = true)
    public List<AuditRecord> forIncident(String incidentCode) {
        return repository.findByIncidentCodeOrderByTimestampAscIdAsc(incidentCode);
    }

    @Transactional(readOnly = true)
    public List<AuditRecord> search(String incidentCode, String actor, String action, int limit) {
        return repository.search(blankToNull(incidentCode), blankToNull(actor), blankToNull(action),
                PageRequest.of(0, Math.min(Math.max(limit, 1), 1000)));
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
