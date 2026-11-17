package com.example.drmayday.repository;

import com.example.drmayday.entity.MedicalVisit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MedicalVisitRepository extends JpaRepository<MedicalVisit, Long> {

    boolean existsByMedicalRecordId(Long medicalRecordId);

    @Query("SELECT v FROM MedicalVisit v JOIN v.medicalRecord r " +
           "WHERE r.patientId = :patientId ORDER BY v.visitDate DESC")
    List<MedicalVisit> findByPatientIdOrderByVisitDateDesc(@Param("patientId") Long patientId);
}
