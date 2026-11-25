package com.example.drmayday.repository;

import com.example.drmayday.entity.ClinicalTestResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClinicalTestResultRepository extends JpaRepository<ClinicalTestResult, Long> {

    /** The schema has no UNIQUE on ma_phien_kham; the API keeps one result per visit. */
    Optional<ClinicalTestResult> findFirstByMedicalVisitIdOrderByClinicalTestResultIdAsc(Long medicalVisitId);
}
