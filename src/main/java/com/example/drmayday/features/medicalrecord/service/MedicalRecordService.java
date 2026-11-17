package com.example.drmayday.features.medicalrecord.service;

import com.example.drmayday.common.dto.PageResponse;
import com.example.drmayday.features.medicalrecord.dto.CreateMedicalRecordRequest;
import com.example.drmayday.features.medicalrecord.dto.ExamHistoryResponse;
import com.example.drmayday.features.medicalrecord.dto.MedicalRecordListItem;
import com.example.drmayday.features.medicalrecord.dto.MedicalRecordResponse;
import com.example.drmayday.features.medicalrecord.dto.UpdateMedicalRecordRequest;

import java.util.List;

/**
 * Nghiệp vụ hồ sơ bệnh án lõi (API 34 đến 39), theo spec 0002.
 */
public interface MedicalRecordService {

    MedicalRecordResponse createMedicalRecord(String patientId, CreateMedicalRecordRequest req);

    PageResponse<MedicalRecordListItem> listMedicalRecords(String patientId, String medicalRecordType,
                                                           String status, Integer page, Integer size);

    MedicalRecordResponse getMedicalRecord(String id);

    MedicalRecordResponse updateMedicalRecord(String id, UpdateMedicalRecordRequest req);

    void deleteMedicalRecord(String id);

    List<ExamHistoryResponse> getPatientRecordHistory(String patientId);
}
