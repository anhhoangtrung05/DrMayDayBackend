package com.example.drmayday.repository;

import com.example.drmayday.entity.DailyUasLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DailyUasLogRepository extends JpaRepository<DailyUasLog, Long> {

    Optional<DailyUasLog> findByPatientIdAndScoreDate(Long patientId, LocalDate scoreDate);

    boolean existsByPatientIdAndScoreDate(Long patientId, LocalDate scoreDate);

    List<DailyUasLog> findByTrackingCycleIdOrderByScoreDateAsc(Long trackingCycleId);

    long countByTrackingCycleIdAndDayStatus(Long trackingCycleId, String dayStatus);

    @Query("SELECT d.trackingCycleId, COUNT(d) FROM DailyUasLog d "
            + "WHERE d.trackingCycleId IN :cycleIds AND d.dayStatus = :dayStatus "
            + "GROUP BY d.trackingCycleId")
    List<Object[]> countByTrackingCycleIdsAndDayStatus(@Param("cycleIds") Collection<Long> cycleIds,
                                                       @Param("dayStatus") String dayStatus);

    @Query("SELECT d FROM DailyUasLog d WHERE d.patientId = :patientId "
            + "AND (:fromDate IS NULL OR d.scoreDate >= :fromDate) "
            + "AND (:toDate IS NULL OR d.scoreDate <= :toDate) "
            + "AND (:cycleId IS NULL OR d.trackingCycleId = :cycleId) "
            + "ORDER BY d.scoreDate DESC, d.dailyUasLogId DESC")
    List<DailyUasLog> findByPatientIdFiltered(@Param("patientId") Long patientId,
                                              @Param("fromDate") LocalDate fromDate,
                                              @Param("toDate") LocalDate toDate,
                                              @Param("cycleId") Long cycleId);
}
