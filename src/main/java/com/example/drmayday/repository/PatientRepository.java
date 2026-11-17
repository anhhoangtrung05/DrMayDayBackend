package com.example.drmayday.repository;

import com.example.drmayday.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {

    // Khóa dòng bệnh nhân khi tạo hồ sơ, để hai request cùng lúc không cùng vượt qua kiểm tra 4006
    @Query(value = "select * from benhnhan where ma_benh_nhan = :id for update", nativeQuery = true)
    Optional<Patient> findByIdForUpdate(@Param("id") Long id);
}
