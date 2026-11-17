package com.example.drmayday.features.medicalrecord.service;

import com.example.drmayday.features.medicalrecord.dto.*;
import org.springframework.data.domain.Page;

import java.util.List;

public interface MedicalRecordService {

    MedicalRecordResponse createMedicalRecord(CreateMedicalRecordRequest req);

    Page<MedicalRecordResponse> listMedicalRecords(String patientId, String loaiBenhAn, String trangThai, int page, int size);

    MedicalRecordDetailResponse getMedicalRecord(String id);

    MedicalRecordResponse updateMedicalRecord(String id, UpdateMedicalRecordRequest req);

    void deleteMedicalRecord(String id);

    List<ExamHistoryResponse> getPatientRecordHistory(String patientId);

    SelfHistoryResponse upsertSelfHistory(UpsertSelfHistoryRequest req);
}
