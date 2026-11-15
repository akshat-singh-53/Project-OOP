package com.itsm.intelligence;

public record DuplicateCandidate(String incidentCode,
                                 String title,
                                 String status,
                                 String serviceName,
                                 double textSimilarity,
                                 boolean sameService,
                                 double score) {
}
