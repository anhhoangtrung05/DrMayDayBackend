package com.example.drmayday.features.medicalrecord.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO sửa hồ sơ bệnh án (API 37). Trường vắng mặt hoặc null nghĩa là giữ nguyên.
 * Mọi kiểm tra giá trị nằm trong service, vì GlobalExceptionHandler biến lỗi validation thành 2001.
 */
public class UpdateMedicalRecordRequest {

    @JsonProperty("trang_thai")
    private String status;

    @JsonProperty("chan_doan_tuyen_truoc")
    private String referringFacilityDiagnosis;

    @JsonProperty("chan_doan_ban_dau_phong_kham")
    private String initialClinicDiagnosis;

    @JsonProperty("thoi_gian_khoi_phat_tuan")
    private BigDecimal onsetDurationWeeks;

    // Nhận dạng chuỗi để tự kiểm tra định dạng yyyy-MM-dd'T'HH:mm:ssXXX và trả 2003
    @JsonProperty("thoi_diem_kham_lan_dau")
    private String firstVisitAt;

    @JsonProperty("ngay_bat_dau_dieu_tri_ngoai_tru")
    private LocalDate outpatientTreatmentStartDate;

    @JsonProperty("ngay_ket_thuc_dieu_tri_ngoai_tru")
    private LocalDate outpatientTreatmentEndDate;

    @JsonProperty("ket_qua_dieu_tri_bao_hiem")
    private String insuranceTreatmentOutcome;

    @JsonProperty("tien_su_gia_dinh")
    private String familyHistory;

    @JsonProperty("tien_su_gia_dinh_ghi_ro")
    private String familyHistoryDescription;

    @JsonProperty("da_dieu_tri_truoc_kham")
    private Boolean treatedBeforeVisit;

    public UpdateMedicalRecordRequest() {
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReferringFacilityDiagnosis() {
        return referringFacilityDiagnosis;
    }

    public void setReferringFacilityDiagnosis(String referringFacilityDiagnosis) {
        this.referringFacilityDiagnosis = referringFacilityDiagnosis;
    }

    public String getInitialClinicDiagnosis() {
        return initialClinicDiagnosis;
    }

    public void setInitialClinicDiagnosis(String initialClinicDiagnosis) {
        this.initialClinicDiagnosis = initialClinicDiagnosis;
    }

    public BigDecimal getOnsetDurationWeeks() {
        return onsetDurationWeeks;
    }

    public void setOnsetDurationWeeks(BigDecimal onsetDurationWeeks) {
        this.onsetDurationWeeks = onsetDurationWeeks;
    }

    public String getFirstVisitAt() {
        return firstVisitAt;
    }

    public void setFirstVisitAt(String firstVisitAt) {
        this.firstVisitAt = firstVisitAt;
    }

    public LocalDate getOutpatientTreatmentStartDate() {
        return outpatientTreatmentStartDate;
    }

    public void setOutpatientTreatmentStartDate(LocalDate outpatientTreatmentStartDate) {
        this.outpatientTreatmentStartDate = outpatientTreatmentStartDate;
    }

    public LocalDate getOutpatientTreatmentEndDate() {
        return outpatientTreatmentEndDate;
    }

    public void setOutpatientTreatmentEndDate(LocalDate outpatientTreatmentEndDate) {
        this.outpatientTreatmentEndDate = outpatientTreatmentEndDate;
    }

    public String getInsuranceTreatmentOutcome() {
        return insuranceTreatmentOutcome;
    }

    public void setInsuranceTreatmentOutcome(String insuranceTreatmentOutcome) {
        this.insuranceTreatmentOutcome = insuranceTreatmentOutcome;
    }

    public String getFamilyHistory() {
        return familyHistory;
    }

    public void setFamilyHistory(String familyHistory) {
        this.familyHistory = familyHistory;
    }

    public String getFamilyHistoryDescription() {
        return familyHistoryDescription;
    }

    public void setFamilyHistoryDescription(String familyHistoryDescription) {
        this.familyHistoryDescription = familyHistoryDescription;
    }

    public Boolean getTreatedBeforeVisit() {
        return treatedBeforeVisit;
    }

    public void setTreatedBeforeVisit(Boolean treatedBeforeVisit) {
        this.treatedBeforeVisit = treatedBeforeVisit;
    }
}
