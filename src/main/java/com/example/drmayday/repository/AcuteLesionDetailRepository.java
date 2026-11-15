package com.example.drmayday.repository;

import com.example.drmayday.entity.AcuteLesionDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AcuteLesionDetailRepository extends JpaRepository<AcuteLesionDetail, Long> {
    List<AcuteLesionDetail> findByMedicalVisitId(Long medicalVisitId);
}
