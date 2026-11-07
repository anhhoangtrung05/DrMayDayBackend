package com.example.drmayday.repository;

import com.example.drmayday.entity.PreviousMedication;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PreviousMedicationRepository extends JpaRepository<PreviousMedication, Long> {
}
