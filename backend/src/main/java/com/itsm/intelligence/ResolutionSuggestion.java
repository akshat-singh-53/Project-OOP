package com.itsm.intelligence;

public record ResolutionSuggestion(String incidentCode,
                                   String title,
                                   String resolution,
                                   String rootCause,
                                   double similarity,
                                   Integer feedbackRating) {
}
