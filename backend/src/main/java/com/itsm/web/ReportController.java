package com.itsm.web;

import com.itsm.domain.user.Role;
import com.itsm.domain.user.User;
import com.itsm.security.CurrentUser;
import com.itsm.security.RequiresRole;
import com.itsm.service.ReportService;
import com.itsm.web.dto.ReportDtos.Overview;
import com.itsm.web.dto.ReportDtos.TechnicianLoad;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reports;

    public ReportController(ReportService reports) {
        this.reports = reports;
    }

    @GetMapping("/overview")
    public Overview overview(@CurrentUser User user) {
        return reports.overview(user);
    }

    @GetMapping("/workload")
    @RequiresRole({Role.ADMIN, Role.TECHNICIAN})
    public List<TechnicianLoad> workload() {
        return reports.technicianWorkload();
    }
}
