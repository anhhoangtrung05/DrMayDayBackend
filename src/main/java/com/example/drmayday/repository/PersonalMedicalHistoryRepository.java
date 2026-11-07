package com.example.drmayday.repository;

import com.example.drmayday.entity.PersonalMedicalHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonalMedicalHistoryRepository extends JpaRepository<PersonalMedicalHistory, Long> {
}
