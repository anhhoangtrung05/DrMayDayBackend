package com.example.drmayday.repository;

import com.example.drmayday.entity.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {
    List<Attachment> findByMedicalRecordIdOrderByCreatedAtDescAttachmentIdDesc(Long medicalRecordId);
}
