package com.itsm.repository;

import com.itsm.domain.user.Technician;
import com.itsm.domain.user.TechnicianLevel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TechnicianRepository extends JpaRepository<Technician, Long> {

    List<Technician> findByActiveTrueOrderByIdAsc();

    List<Technician> findByActiveTrueAndLevel(TechnicianLevel level);
}
