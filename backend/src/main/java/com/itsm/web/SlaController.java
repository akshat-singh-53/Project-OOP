package com.itsm.web;

import com.itsm.domain.user.Role;
import com.itsm.domain.user.User;
import com.itsm.security.CurrentUser;
import com.itsm.security.RequiresRole;
import com.itsm.service.SlaMonitorService;
import com.itsm.service.SlaService;
import com.itsm.web.dto.CatalogDtos.SlaPolicyDto;
import com.itsm.web.dto.CatalogDtos.SlaPolicyRequest;
import com.itsm.web.dto.IncidentDtos.SlaViolationDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/sla")
@RequiresRole({Role.ADMIN, Role.TECHNICIAN})
public class SlaController {

    private final SlaService slaService;
    private final SlaMonitorService monitor;

    public SlaController(SlaService slaService, SlaMonitorService monitor) {
        this.slaService = slaService;
        this.monitor = monitor;
    }

    @GetMapping("/policies")
    public List<SlaPolicyDto> policies() {
        return slaService.list().stream().map(SlaPolicyDto::from).toList();
    }

    @PostMapping("/policies")
    @RequiresRole(Role.ADMIN)
    @ResponseStatus(HttpStatus.CREATED)
    public SlaPolicyDto create(@Valid @RequestBody SlaPolicyRequest r, @CurrentUser User user) {
        return SlaPolicyDto.from(slaService.create(r, user.getUsername()));
    }

    @PutMapping("/policies/{id}")
    @RequiresRole(Role.ADMIN)
    public SlaPolicyDto update(@PathVariable Long id, @Valid @RequestBody SlaPolicyRequest r, @CurrentUser User user) {
        return SlaPolicyDto.from(slaService.update(id, r, user.getUsername()));
    }

    @GetMapping("/violations")
    public List<SlaViolationDto> violations() {
        return slaService.violations().stream().map(SlaViolationDto::from).toList();
    }

    /** Lets an administrator trigger the background SLA check immediately (useful for demos). */
    @PostMapping("/monitor/run")
    @RequiresRole(Role.ADMIN)
    public SlaMonitorService.MonitorReport runMonitor() {
        return monitor.runOnce();
    }
}
