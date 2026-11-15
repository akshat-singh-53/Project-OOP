package com.itsm.intelligence;

import com.itsm.domain.incident.Incident;
import com.itsm.domain.incident.Priority;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Groups recent open incidents that share a service or category. Several simultaneous incidents
 * on one service usually point to a single infrastructure failure (a "major incident").
 */
@Component
public class CorrelationAnalyzer {

    public static final int MIN_CLUSTER_SIZE = 2;
    public static final int MAJOR_INCIDENT_SIZE = 3;

    public List<CorrelationCluster> analyse(List<Incident> recentOpenIncidents) {
        List<CorrelationCluster> clusters = new ArrayList<>();
        clusters.addAll(group(recentOpenIncidents, "SERVICE", i -> i.getService().getName()));
        clusters.addAll(group(recentOpenIncidents, "CATEGORY",
                i -> i.getCategory() == null ? null : i.getCategory().getName()));
        clusters.sort(Comparator.comparingInt(CorrelationCluster::incidentCount).reversed());
        return clusters;
    }

    private List<CorrelationCluster> group(List<Incident> incidents, String type, Function<Incident, String> key) {
        Map<String, List<Incident>> groups = new LinkedHashMap<>();
        for (Incident i : incidents) {
            String k = key.apply(i);
            if (k != null) {
                groups.computeIfAbsent(k, x -> new ArrayList<>()).add(i);
            }
        }
        List<CorrelationCluster> result = new ArrayList<>();
        groups.forEach((name, list) -> {
            if (list.size() < MIN_CLUSTER_SIZE) {
                return;
            }
            Priority highest = list.stream().map(Incident::getPriority).filter(p -> p != null)
                    .min(Comparator.comparingInt(Priority::getLevel)).orElse(Priority.P4);
            boolean major = "SERVICE".equals(type) && (list.size() >= MAJOR_INCIDENT_SIZE || highest == Priority.P1);
            String insight = major
                    ? list.size() + " open incidents on " + name + " - likely a common root cause. Consider "
                    + "declaring a major incident and linking duplicates."
                    : list.size() + " related open incidents in " + name + ".";
            result.add(new CorrelationCluster(type, name, list.size(),
                    list.stream().map(Incident::getCode).toList(), highest.name(), major, insight));
        });
        return result;
    }
}
