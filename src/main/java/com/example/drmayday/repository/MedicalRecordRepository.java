package com.example.drmayday.repository;

import com.example.drmayday.entity.MedicalRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;

public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, Long> {

    // Khóa dòng hồ sơ khi sửa hoặc xóa. Phải là lần đọc hồ sơ đầu tiên trong giao dịch
    @Query(value = "select * from hosobenhan where ma_ho_so = :id for update", nativeQuery = true)
    Optional<MedicalRecord> findByIdForUpdate(@Param("id") Long id);

    boolean existsByPatientIdAndMedicalRecordTypeAndStatusIn(Long patientId, String medicalRecordType,
                                                             Collection<String> statuses);

    @Query(value = "select r from MedicalRecord r join fetch r.patient p join fetch p.person "
            + "where (:patientId is null or r.patientId = :patientId) "
            + "and (:type is null or r.medicalRecordType = :type) "
            + "and (:status is null or r.status = :status) "
            + "order by r.firstVisitAt desc nulls last, r.medicalRecordId desc",
            countQuery = "select count(r) from MedicalRecord r "
                    + "where (:patientId is null or r.patientId = :patientId) "
                    + "and (:type is null or r.medicalRecordType = :type) "
                    + "and (:status is null or r.status = :status)")
    Page<MedicalRecord> search(@Param("patientId") Long patientId,
                               @Param("type") String type,
                               @Param("status") String status,
                               Pageable pageable);
}
