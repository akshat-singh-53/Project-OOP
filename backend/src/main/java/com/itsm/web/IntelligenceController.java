package com.itsm.web;

import com.itsm.domain.user.Role;
import com.itsm.intelligence.CategorySuggestion;
import com.itsm.intelligence.CorrelationCluster;
import com.itsm.intelligence.DuplicateCandidate;
import com.itsm.intelligence.SlaRiskAssessment;
import com.itsm.security.RequiresRole;
import com.itsm.service.IntelligenceService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/intelligence")
public class IntelligenceController {

    private final IntelligenceService intelligence;

    public IntelligenceController(IntelligenceService intelligence) {
        this.intelligence = intelligence;
    }

    public record TextRequest(@NotBlank @Size(max = 5000) String text, Long serviceId) {
    }

    /** Live suggestions while an employee is typing the incident description. */
    @PostMapping("/classify")
    public List<CategorySuggestion> classify(@Valid @RequestBody TextRequest r) {
        return intelligence.suggestCategories(r.text(), r.serviceId());
    }

    @PostMapping("/duplicates")
    public List<DuplicateCandidate> duplicates(@Valid @RequestBody TextRequest r) {
        return intelligence.findDuplicates(r.text(), r.serviceId(), null);
    }

    @GetMapping("/correlations")
    @RequiresRole({Role.ADMIN, Role.TECHNICIAN})
    public List<CorrelationCluster> correlations(@RequestParam(defaultValue = "24") int hours) {
        return intelligence.correlate(Math.max(1, Math.min(hours, 24 * 30)));
    }

    @GetMapping("/sla-risks")
    @RequiresRole({Role.ADMIN, Role.TECHNICIAN})
    public List<SlaRiskAssessment> slaRisks() {
        return intelligence.slaRisks();
    }
}
