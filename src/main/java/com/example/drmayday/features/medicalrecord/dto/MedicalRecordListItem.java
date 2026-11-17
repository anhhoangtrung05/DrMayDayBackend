package com.example.drmayday.features.medicalrecord.dto;

import com.example.drmayday.entity.MedicalRecord;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Một dòng trong danh sách hồ sơ bệnh án (API 35).
 */
public record MedicalRecordListItem(
        @JsonProperty("record_id") String recordId,
        @JsonProperty("patient_id") String patientId,
        @JsonProperty("ho_ten") String fullName,
        @JsonProperty("loai_benh_an") String medicalRecordType,
        @JsonProperty("trang_thai") String status,
        @JsonProperty("chan_doan_ban_dau_phong_kham") String initialClinicDiagnosis,
        @JsonProperty("thoi_diem_kham_lan_dau") String firstVisitAt) {

    // Cần patient và person đã được join fetch, nếu không sẽ tốn thêm truy vấn cho mỗi dòng
    public static MedicalRecordListItem from(MedicalRecord record) {
        return new MedicalRecordListItem(
                JsonValues.id(record.getMedicalRecordId()),
                JsonValues.id(record.getPatientId()),
                record.getPatient().getPerson().getFullName(),
                record.getMedicalRecordType(),
                record.getStatus(),
                record.getInitialClinicDiagnosis(),
                JsonValues.timestamp(record.getFirstVisitAt()));
    }
}
