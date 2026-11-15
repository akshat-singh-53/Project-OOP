package com.itsm.web;

import com.itsm.domain.incident.Impact;
import com.itsm.domain.incident.IncidentStatus;
import com.itsm.domain.incident.Priority;
import com.itsm.domain.incident.SlaStatus;
import com.itsm.domain.incident.Urgency;
import com.itsm.domain.user.TechnicianLevel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** Public, read-only reference data used to build the UI drop-downs. */
@RestController
@RequestMapping("/api")
public class MetaController {

    public record Option(String value, String label, String description) {
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }

    @GetMapping("/meta/enums")
    public Map<String, List<Option>> enums() {
        return Map.of(
                "impacts", Arrays.stream(Impact.values())
                        .map(i -> new Option(i.name(), i.name(), i.getDescription())).toList(),
                "urgencies", Arrays.stream(Urgency.values())
                        .map(u -> new Option(u.name(), u.name(), u.getDescription())).toList(),
                "priorities", Arrays.stream(Priority.values())
                        .map(p -> new Option(p.name(), p.name() + " - " + p.getLabel(), p.getLabel())).toList(),
                "statuses", Arrays.stream(IncidentStatus.values())
                        .map(s -> new Option(s.name(), s.name().replace('_', ' '), null)).toList(),
                "slaStatuses", Arrays.stream(SlaStatus.values())
                        .map(s -> new Option(s.name(), s.name().replace('_', ' '), null)).toList(),
                "technicianLevels", Arrays.stream(TechnicianLevel.values())
                        .map(l -> new Option(l.name(), l.getTitle(), null)).toList());
    }
}
