package com.example.drmayday.repository;

import com.example.drmayday.entity.DailyUasLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyUasLogRepository extends JpaRepository<DailyUasLog, Long> {
}
