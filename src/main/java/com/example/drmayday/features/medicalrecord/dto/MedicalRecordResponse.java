package com.example.drmayday.features.medicalrecord.dto;

import com.example.drmayday.entity.MedicalRecord;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * DTO phản hồi hồ sơ bệnh án (API 34, 37). Id là chuỗi, tên trường là tên cột.
 */
@Getter
public class MedicalRecordResponse {

    @JsonProperty("record_id")
    private final String recordId;

    @JsonProperty("patient_id")
    private final String patientId;

    @JsonProperty("loai_benh_an")
    private final String medicalRecordType;

    @JsonProperty("trang_thai")
    private final String status;

    @JsonProperty("chan_doan_tuyen_truoc")
    private final String referringFacilityDiagnosis;

    @JsonProperty("chan_doan_ban_dau_phong_kham")
    private final String initialClinicDiagnosis;

    @JsonProperty("thoi_gian_khoi_phat_tuan")
    private final BigDecimal onsetDurationWeeks;

    @JsonProperty("thoi_diem_kham_lan_dau")
    private final String firstVisitAt;

    @JsonProperty("ngay_bat_dau_dieu_tri_ngoai_tru")
    private final String outpatientTreatmentStartDate;

    @JsonProperty("ngay_ket_thuc_dieu_tri_ngoai_tru")
    private final String outpatientTreatmentEndDate;

    @JsonProperty("ket_qua_dieu_tri_bao_hiem")
    private final String insuranceTreatmentOutcome;

    @JsonProperty("tien_su_gia_dinh")
    private final String familyHistory;

    @JsonProperty("tien_su_gia_dinh_ghi_ro")
    private final String familyHistoryDescription;

    @JsonProperty("da_dieu_tri_truoc_kham")
    private final Boolean treatedBeforeVisit;

    public MedicalRecordResponse(MedicalRecord record) {
        this.recordId = JsonValues.id(record.getMedicalRecordId());
        this.patientId = JsonValues.id(record.getPatientId());
        this.medicalRecordType = record.getMedicalRecordType();
        this.status = record.getStatus();
        this.referringFacilityDiagnosis = record.getReferringFacilityDiagnosis();
        this.initialClinicDiagnosis = record.getInitialClinicDiagnosis();
        this.onsetDurationWeeks = JsonValues.twoDecimals(record.getOnsetDurationWeeks());
        this.firstVisitAt = JsonValues.timestamp(record.getFirstVisitAt());
        this.outpatientTreatmentStartDate = JsonValues.date(record.getOutpatientTreatmentStartDate());
        this.outpatientTreatmentEndDate = JsonValues.date(record.getOutpatientTreatmentEndDate());
        this.insuranceTreatmentOutcome = record.getInsuranceTreatmentOutcome();
        this.familyHistory = record.getFamilyHistory();
        this.familyHistoryDescription = record.getFamilyHistoryDescription();
        this.treatedBeforeVisit = record.getTreatedBeforeVisit();
    }
}
