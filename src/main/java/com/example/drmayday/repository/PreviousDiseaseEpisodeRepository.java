package com.example.drmayday.repository;

import com.example.drmayday.entity.PreviousDiseaseEpisode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PreviousDiseaseEpisodeRepository extends JpaRepository<PreviousDiseaseEpisode, Long> {

    Optional<PreviousDiseaseEpisode> findByMedicalRecordId(Long medicalRecordId);
}
