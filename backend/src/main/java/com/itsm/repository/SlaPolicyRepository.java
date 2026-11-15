package com.itsm.repository;

import com.itsm.domain.catalog.ItService;
import com.itsm.domain.incident.Priority;
import com.itsm.domain.sla.SlaPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SlaPolicyRepository extends JpaRepository<SlaPolicy, Long> {

    Optional<SlaPolicy> findFirstByPriorityAndServiceAndActiveTrue(Priority priority, ItService service);

    Optional<SlaPolicy> findFirstByPriorityAndServiceIsNullAndActiveTrue(Priority priority);
}
