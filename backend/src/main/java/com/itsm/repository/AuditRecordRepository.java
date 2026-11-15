package com.itsm.repository;

import com.itsm.domain.audit.AuditRecord;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AuditRecordRepository extends JpaRepository<AuditRecord, Long> {

    List<AuditRecord> findByIncidentCodeOrderByTimestampAscIdAsc(String incidentCode);

    @Query("select a from AuditRecord a where "
            + "(:incidentCode is null or a.incidentCode = :incidentCode) and "
            + "(:actor is null or lower(a.actor) like lower(concat('%', :actor, '%'))) and "
            + "(:action is null or a.action = :action) "
            + "order by a.timestamp desc, a.id desc")
    List<AuditRecord> search(@Param("incidentCode") String incidentCode,
                             @Param("actor") String actor,
                             @Param("action") String action,
                             Pageable pageable);
}
