package com.example.drmayday.repository;

import com.example.drmayday.entity.MedicalVisit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicalVisitRepository extends JpaRepository<MedicalVisit, Long> {
}
