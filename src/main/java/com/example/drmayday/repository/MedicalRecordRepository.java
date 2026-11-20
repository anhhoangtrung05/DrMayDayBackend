package com.example.drmayday.repository;

import com.example.drmayday.entity.MedicalRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, Long> {

    boolean existsByPatientIdAndStatusNotIn(Long patientId, java.util.Collection<String> statuses);

    @Query("SELECT m FROM MedicalRecord m WHERE " +
           "(:patientId IS NULL OR m.patientId = :patientId) AND " +
           "(:type IS NULL OR m.medicalRecordType = :type) AND " +
           "(:status IS NULL OR m.status = :status) " +
           "ORDER BY m.firstVisitAt DESC NULLS LAST")
    Page<MedicalRecord> findFiltered(@Param("patientId") Long patientId,
                                     @Param("type") String type,
                                     @Param("status") String status,
                                     Pageable pageable);
    Optional<MedicalRecord> findFirstByPatientIdAndMedicalRecordTypeAndStatusNotInOrderByMedicalRecordIdDesc(
            Long patientId, String medicalRecordType, java.util.Collection<String> statuses);
}
