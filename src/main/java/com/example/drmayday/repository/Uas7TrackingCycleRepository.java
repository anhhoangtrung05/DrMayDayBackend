package com.example.drmayday.repository;

import com.example.drmayday.entity.Uas7TrackingCycle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface Uas7TrackingCycleRepository extends JpaRepository<Uas7TrackingCycle, Long> {

    Optional<Uas7TrackingCycle> findFirstByPatientIdAndCycleStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateDesc(
            Long patientId, String cycleStatus, LocalDate dateForStart, LocalDate dateForEnd);

    @Query("SELECT COUNT(c) > 0 FROM Uas7TrackingCycle c WHERE c.patientId = :patientId "
            + "AND c.cycleStatus = 'DANG_THEO_DOI' AND c.startDate <= :endDate AND c.endDate >= :startDate")
    boolean existsOverlappingActive(@Param("patientId") Long patientId,
                                    @Param("startDate") LocalDate startDate,
                                    @Param("endDate") LocalDate endDate);

    List<Uas7TrackingCycle> findByPatientIdOrderByStartDateDescTrackingCycleIdDesc(Long patientId);
}
