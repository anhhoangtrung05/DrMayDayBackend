package com.example.drmayday.repository;

import com.example.drmayday.entity.VitalSign;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VitalSignRepository extends JpaRepository<VitalSign, Long> {
    List<VitalSign> findByMedicalVisitIdOrderByTimestampAsc(Long medicalVisitId);
}
