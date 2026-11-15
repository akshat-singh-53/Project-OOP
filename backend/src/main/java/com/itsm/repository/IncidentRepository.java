package com.itsm.repository;

import com.itsm.domain.catalog.ItService;
import com.itsm.domain.incident.Incident;
import com.itsm.domain.incident.IncidentStatus;
import com.itsm.domain.user.Technician;
import com.itsm.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface IncidentRepository extends JpaRepository<Incident, Long>, JpaSpecificationExecutor<Incident> {

    List<Incident> findByStatusIn(Collection<IncidentStatus> statuses);

    List<Incident> findByStatusInAndCreatedAtAfter(Collection<IncidentStatus> statuses, LocalDateTime after);

    List<Incident> findByResolutionIsNotNull();

    List<Incident> findByReporterAndTitleIgnoreCaseAndCreatedAtAfter(User reporter, String title,
                                                                     LocalDateTime after);

    List<Incident> findByAssignedTechnicianAndStatusIn(Technician technician, Collection<IncidentStatus> statuses);

    long countByAssignedTechnicianAndStatusIn(Technician technician, Collection<IncidentStatus> statuses);

    long countByAssignedTechnicianAndServiceAndResolvedAtIsNotNull(Technician technician, ItService service);

    @Query("select i.assignedTechnician.id, count(i) from Incident i "
            + "where i.assignedTechnician is not null and i.status in :statuses group by i.assignedTechnician.id")
    List<Object[]> countWorkloadByTechnician(@Param("statuses") Collection<IncidentStatus> statuses);

    @Query("select i.assignedTechnician.id, count(i) from Incident i "
            + "where i.assignedTechnician is not null and i.resolvedAt is not null group by i.assignedTechnician.id")
    List<Object[]> countResolvedByTechnician();
}
