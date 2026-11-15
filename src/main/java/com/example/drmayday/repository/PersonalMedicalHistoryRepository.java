package com.example.drmayday.repository;

import com.example.drmayday.entity.PersonalMedicalHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PersonalMedicalHistoryRepository extends JpaRepository<PersonalMedicalHistory, Long> {

    Optional<PersonalMedicalHistory> findByMedicalRecordId(Long medicalRecordId);
}
