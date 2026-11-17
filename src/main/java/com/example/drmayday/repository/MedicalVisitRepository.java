package com.example.drmayday.repository;

import com.example.drmayday.entity.MedicalVisit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MedicalVisitRepository extends JpaRepository<MedicalVisit, Long> {

    boolean existsByMedicalRecordId(Long medicalRecordId);

    @Query("SELECT v FROM MedicalVisit v JOIN v.medicalRecord r " +
           "WHERE r.patientId = :patientId ORDER BY v.visitDate DESC")
    List<MedicalVisit> findByPatientIdOrderByVisitDateDesc(@Param("patientId") Long patientId);

    // Gọi sau khi đã khóa dòng hồ sơ, để hai request tạo cùng lúc không tính ra cùng một số
    @Query("select coalesce(max(v.visitNumber), 0) from MedicalVisit v where v.medicalRecordId = :recordId")
    int findMaxVisitNumber(@Param("recordId") Long recordId);

    boolean existsByMedicalRecordIdAndVisitNumber(Long medicalRecordId, Integer visitNumber);

    // Chỉ đọc ma_ho_so, không nạp entity phiên, để lần đọc phiên đầu tiên nằm sau khóa hồ sơ
    @Query("select v.medicalRecordId from MedicalVisit v where v.medicalVisitId = :id")
    Optional<Long> findRecordIdById(@Param("id") Long id);

    @Query("select v from MedicalVisit v join fetch v.employee e join fetch e.person "
            + "left join fetch v.clinicRoom where v.medicalRecordId = :recordId order by v.visitNumber")
    List<MedicalVisit> findByRecordWithDoctorAndRoom(@Param("recordId") Long recordId);

    @Query("select v from MedicalVisit v join fetch v.employee e join fetch e.person "
            + "left join fetch v.clinicRoom where v.medicalVisitId = :id")
    Optional<MedicalVisit> findDetailById(@Param("id") Long id);
}
