package com.itsm.repository;

import com.itsm.domain.incident.Incident;
import com.itsm.domain.sla.SlaViolation;
import com.itsm.domain.sla.SlaViolationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SlaViolationRepository extends JpaRepository<SlaViolation, Long> {

    boolean existsByIncidentAndType(Incident incident, SlaViolationType type);

    List<SlaViolation> findAllByOrderByDetectedAtDesc();
}
