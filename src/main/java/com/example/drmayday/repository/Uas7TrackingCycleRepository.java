package com.example.drmayday.repository;

import com.example.drmayday.entity.Uas7TrackingCycle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface Uas7TrackingCycleRepository extends JpaRepository<Uas7TrackingCycle, Long> {
    @Query("select c from Uas7TrackingCycle c where c.patientId = :patientId and c.cycleStatus = 'DANG_THEO_DOI' and c.startDate <= :date and c.endDate >= :date order by c.startDate desc")
    Optional<Uas7TrackingCycle> findActiveForDate(@Param("patientId") Long patientId, @Param("date") LocalDate date);
    @Query("select case when count(c) > 0 then true else false end from Uas7TrackingCycle c where c.patientId = :patientId and c.cycleStatus = 'DANG_THEO_DOI' and c.startDate <= :endDate and c.endDate >= :startDate")
    boolean existsOverlappingActive(@Param("patientId") Long patientId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    List<Uas7TrackingCycle> findByPatientIdOrderByStartDateDesc(Long patientId);
}
