package com.example.drmayday.repository;

import com.example.drmayday.entity.AllergyOnsetContext;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AllergyOnsetContextRepository extends JpaRepository<AllergyOnsetContext, Long> {

    Optional<AllergyOnsetContext> findByMedicalRecordId(Long medicalRecordId);
}
