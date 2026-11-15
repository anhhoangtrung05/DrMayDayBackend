package com.example.drmayday.repository;

import com.example.drmayday.entity.WhealLesionDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WhealLesionDetailRepository extends JpaRepository<WhealLesionDetail, Long> {
    List<WhealLesionDetail> findByMedicalVisitId(Long medicalVisitId);
}
