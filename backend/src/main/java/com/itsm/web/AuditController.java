package com.itsm.web;

import com.itsm.domain.user.Role;
import com.itsm.security.RequiresRole;
import com.itsm.service.AuditService;
import com.itsm.web.dto.IncidentDtos.AuditDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/audit")
@RequiresRole(Role.ADMIN)
public class AuditController {

    private final AuditService audit;

    public AuditController(AuditService audit) {
        this.audit = audit;
    }

    @GetMapping
    public List<AuditDto> search(@RequestParam(required = false) String incidentCode,
                                 @RequestParam(required = false) String actor,
                                 @RequestParam(required = false) String action,
                                 @RequestParam(defaultValue = "300") int limit) {
        String code = incidentCode == null ? null : incidentCode.trim().toUpperCase();
        return audit.search(code, actor, action, limit).stream().map(AuditDto::from).toList();
    }
}
