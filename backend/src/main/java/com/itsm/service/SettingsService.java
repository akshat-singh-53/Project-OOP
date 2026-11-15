package com.itsm.service;

import com.itsm.domain.setting.SystemSetting;
import com.itsm.repository.SystemSettingRepository;
import com.itsm.strategy.assignment.SmartAssignmentStrategy;
import com.itsm.strategy.priority.MatrixPriorityStrategy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

/** Runtime configuration stored in the database so administrators can change behaviour live. */
@Service
public class SettingsService {

    public static final String ASSIGNMENT_STRATEGY = "assignment.strategy";
    public static final String PRIORITY_STRATEGY = "priority.strategy";
    public static final String AUTO_ASSIGN = "assignment.auto";
    public static final String EMAIL_ENABLED = "notification.email.enabled";
    public static final String SMS_ENABLED = "notification.sms.enabled";
    public static final String ESCALATION_ENABLED = "escalation.enabled";
    public static final String AUTO_CLOSE_HOURS = "incident.autoCloseHours";
    public static final String AI_CLASSIFIER_ENABLED = "ai.classifier.enabled";

    private static final Map<String, String> DEFAULTS = new LinkedHashMap<>();

    static {
        DEFAULTS.put(ASSIGNMENT_STRATEGY, SmartAssignmentStrategy.KEY);
        DEFAULTS.put(PRIORITY_STRATEGY, MatrixPriorityStrategy.KEY);
        DEFAULTS.put(AUTO_ASSIGN, "true");
        DEFAULTS.put(EMAIL_ENABLED, "true");
        DEFAULTS.put(SMS_ENABLED, "true");
        DEFAULTS.put(ESCALATION_ENABLED, "true");
        DEFAULTS.put(AUTO_CLOSE_HOURS, "72");
        DEFAULTS.put(AI_CLASSIFIER_ENABLED, "true");
    }

    private final SystemSettingRepository repository;

    public SettingsService(SystemSettingRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public String get(String key) {
        return repository.findById(key).map(SystemSetting::getValue).orElse(DEFAULTS.get(key));
    }

    public boolean getBoolean(String key) {
        return Boolean.parseBoolean(get(key));
    }

    public int getInt(String key) {
        try {
            return Integer.parseInt(get(key));
        } catch (NumberFormatException ex) {
            return Integer.parseInt(DEFAULTS.get(key));
        }
    }

    @Transactional
    public void set(String key, String value) {
        if (!DEFAULTS.containsKey(key)) {
            throw new IllegalArgumentException("Unknown setting: " + key);
        }
        SystemSetting setting = repository.findById(key).orElseGet(() -> new SystemSetting(key, value));
        setting.setValue(value);
        repository.save(setting);
    }

    @Transactional(readOnly = true)
    public Map<String, String> all() {
        Map<String, String> result = new LinkedHashMap<>();
        DEFAULTS.keySet().forEach(k -> result.put(k, get(k)));
        return result;
    }
}
