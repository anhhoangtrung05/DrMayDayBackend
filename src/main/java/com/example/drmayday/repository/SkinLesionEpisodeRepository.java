package com.example.drmayday.repository;

import com.example.drmayday.entity.SkinLesionEpisode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SkinLesionEpisodeRepository extends JpaRepository<SkinLesionEpisode, Long> {

    @Query("SELECT e FROM SkinLesionEpisode e WHERE e.patientId = :patientId ORDER BY e.onsetAt DESC NULLS LAST, e.skinLesionEpisodeId DESC")
    List<SkinLesionEpisode> findByPatientIdOrderByOnsetAtDesc(@Param("patientId") Long patientId);

    @Query("SELECT e FROM SkinLesionEpisode e WHERE e.patientId = :patientId AND e.chronicMedicalRecordId = :chronicRecordId ORDER BY e.onsetAt DESC NULLS LAST, e.skinLesionEpisodeId DESC")
    List<SkinLesionEpisode> findByPatientIdAndChronicMedicalRecordIdOrderByOnsetAtDesc(@Param("patientId") Long patientId, @Param("chronicRecordId") Long chronicRecordId);

    @Query("SELECT e FROM SkinLesionEpisode e WHERE e.chronicMedicalRecordId = :chronicRecordId ORDER BY e.onsetAt DESC NULLS LAST, e.skinLesionEpisodeId DESC")
    List<SkinLesionEpisode> findByChronicMedicalRecordIdOrderByOnsetAtDesc(@Param("chronicRecordId") Long chronicRecordId);

    @Query("SELECT e FROM SkinLesionEpisode e ORDER BY e.onsetAt DESC NULLS LAST, e.skinLesionEpisodeId DESC")
    List<SkinLesionEpisode> findAllOrderByOnsetAtDesc();
}
