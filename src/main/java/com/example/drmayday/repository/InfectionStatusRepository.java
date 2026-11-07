package com.example.drmayday.repository;

import com.example.drmayday.entity.InfectionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InfectionStatusRepository extends JpaRepository<InfectionStatus, Long> {
}
