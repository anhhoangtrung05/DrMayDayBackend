package com.example.drmayday.repository;

import com.example.drmayday.entity.InfectionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InfectionStatusRepository extends JpaRepository<InfectionStatus, Long> {

    Optional<InfectionStatus> findByMedicalRecordId(Long medicalRecordId);
}
