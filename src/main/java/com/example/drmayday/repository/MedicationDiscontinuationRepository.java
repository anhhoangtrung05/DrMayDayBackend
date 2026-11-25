package com.example.drmayday.repository;

import com.example.drmayday.entity.MedicationDiscontinuation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MedicationDiscontinuationRepository extends JpaRepository<MedicationDiscontinuation, Long> {

    List<MedicationDiscontinuation> findByPrescriptionIdOrderByDiscontinuationIdAsc(Long prescriptionId);

    void deleteByPrescriptionId(Long prescriptionId);
}
