package com.example.drmayday.repository;

import com.example.drmayday.entity.MedicationDiscontinuation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicationDiscontinuationRepository extends JpaRepository<MedicationDiscontinuation, Long> {
}
