package com.example.drmayday.repository;

import com.example.drmayday.entity.Prescription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {

    @Query("SELECT p FROM Prescription p WHERE " +
           "(:medicalVisitId IS NULL OR p.medicalVisitId = :medicalVisitId) AND " +
           "(:medicalRecordId IS NULL OR p.medicalRecordId = :medicalRecordId) " +
           "ORDER BY p.prescriptionDate DESC, p.prescriptionId DESC")
    List<Prescription> findFiltered(@Param("medicalVisitId") Long medicalVisitId,
                                    @Param("medicalRecordId") Long medicalRecordId);
}
