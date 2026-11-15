package com.itsm.web;

import com.itsm.domain.user.Role;
import com.itsm.domain.user.User;
import com.itsm.intelligence.LlmCategoryClassifier;
import com.itsm.notification.EmailGateway;
import com.itsm.security.CurrentUser;
import com.itsm.security.RequiresRole;
import com.itsm.service.AssignmentService;
import com.itsm.service.AuditService;
import com.itsm.service.PriorityService;
import com.itsm.service.SettingsService;
import com.itsm.strategy.NamedStrategy;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/settings")
@RequiresRole(Role.ADMIN)
public class SettingsController {

    private final SettingsService settings;
    private final AssignmentService assignment;
    private final PriorityService priority;
    private final LlmCategoryClassifier llm;
    private final AuditService audit;
    private final EmailGateway email;

    public SettingsController(SettingsService settings, AssignmentService assignment, PriorityService priority,
                              LlmCategoryClassifier llm, AuditService audit, EmailGateway email) {
        this.settings = settings;
        this.assignment = assignment;
        this.priority = priority;
        this.llm = llm;
        this.audit = audit;
        this.email = email;
    }

    public record StrategyInfo(String key, String name, String description) {
        static List<StrategyInfo> of(Collection<? extends NamedStrategy> strategies) {
            return strategies.stream().map(s -> new StrategyInfo(s.key(), s.displayName(), s.description())).toList();
        }
    }

    public record SettingsView(Map<String, String> values, List<StrategyInfo> assignmentStrategies,
                               List<StrategyInfo> priorityStrategies, boolean llmConfigured, String llmModel,
                               String emailDelivery) {
    }

    public record TestEmailRequest(@NotBlank @Email String to) {
    }

    @GetMapping
    public SettingsView get() {
        return new SettingsView(settings.all(), StrategyInfo.of(assignment.available()),
                StrategyInfo.of(priority.available()), llm.isConfigured(), llm.name(), email.description());
    }

    @PostMapping("/test-email")
    public Map<String, String> testEmail(@Valid @RequestBody TestEmailRequest r, @CurrentUser User user) {
        email.sendEmail(r.to().trim(), "Test e-mail",
                "This is a test message sent by " + user.getFullName() + " from the I-ITSM settings page.");
        audit.recordAdminAction(user.getUsername(), "TEST_EMAIL", "Test e-mail requested for " + r.to());
        return Map.of("message", "Test e-mail queued for " + r.to().trim(), "delivery", email.description());
    }

    @PutMapping
    public SettingsView update(@RequestBody Map<String, String> changes, @CurrentUser User user) {
        changes.forEach((key, value) -> {
            if (SettingsService.ASSIGNMENT_STRATEGY.equals(key)) {
                assignment.ensureExists(value);
            } else if (SettingsService.PRIORITY_STRATEGY.equals(key)) {
                priority.ensureExists(value);
            }
            settings.set(key, value);
            audit.recordAdminAction(user.getUsername(), "SETTING_CHANGED", key + " = " + value);
        });
        return get();
    }
}
