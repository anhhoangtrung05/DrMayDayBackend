package com.example.drmayday.repository;

import com.example.drmayday.entity.ClinicalTestResult;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClinicalTestResultRepository extends JpaRepository<ClinicalTestResult, Long> {
}
