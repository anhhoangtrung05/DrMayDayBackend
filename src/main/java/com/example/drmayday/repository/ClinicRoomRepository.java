package com.example.drmayday.repository;
import com.example.drmayday.entity.ClinicRoom;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

public interface ClinicRoomRepository extends JpaRepository<ClinicRoom, Long> {
    boolean existsByBranchId(Long branchId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM ClinicRoom r WHERE r.clinicRoomId = :clinicRoomId")
    Optional<ClinicRoom> findByIdForUpdate(@Param("clinicRoomId") Long clinicRoomId);

    boolean existsByBranchIdAndName(Long branchId, String name);

    Optional<ClinicRoom> findByBranchIdAndName(Long branchId, String name);

    List<ClinicRoom> findAllByBranchId(Long branchId);

    @Query("SELECT COUNT(mv) > 0 FROM MedicalVisit mv WHERE mv.clinicRoomId = :clinicRoomId")
    boolean hasMedicalVisits(@Param("clinicRoomId") Long clinicRoomId);

    boolean existsByBranchIdAndNameAndRoomIdNot(@Param("branchId") Long branchId, @Param("name") String name, @Param("clinicRoomId") Long clinicRoomId);

    @Query("SELECT COUNT(es) > 0 FROM ExamSession es WHERE es.clinicRoomId = :clinicRoomId")
    boolean hasExamSessions(@Param("clinicRoomId") Long clinicRoomId);
}
