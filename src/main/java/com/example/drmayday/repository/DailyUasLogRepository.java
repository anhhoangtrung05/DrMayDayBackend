package com.example.drmayday.repository;

import com.example.drmayday.entity.DailyUasLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyUasLogRepository extends JpaRepository<DailyUasLog, Long> {
    Optional<DailyUasLog> findByPatientIdAndScoreDate(Long patientId, LocalDate scoreDate);
    List<DailyUasLog> findByPatientIdOrderByScoreDateDesc(Long patientId);
    List<DailyUasLog> findByPatientIdAndScoreDateBetweenOrderByScoreDateDesc(Long patientId, LocalDate from, LocalDate to);
    List<DailyUasLog> findByPatientIdAndTrackingCycleIdOrderByScoreDateDesc(Long patientId, Long cycleId);
    List<DailyUasLog> findByTrackingCycleIdOrderByScoreDateAsc(Long cycleId);
    long countByTrackingCycleIdAndDayStatus(Long cycleId, String dayStatus);
}
