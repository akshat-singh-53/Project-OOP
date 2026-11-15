package com.itsm.intelligence;

import java.util.List;

public record CorrelationCluster(String groupType,
                                 String groupName,
                                 int incidentCount,
                                 List<String> incidentCodes,
                                 String highestPriority,
                                 boolean suspectedMajorIncident,
                                 String insight) {
}
