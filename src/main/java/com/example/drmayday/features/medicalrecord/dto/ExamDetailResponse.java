package com.example.drmayday.features.medicalrecord.dto;

import com.example.drmayday.entity.MedicalVisit;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * Phần chung của chi tiết phiên khám (API 48). Mỗi loại hồ sơ có lớp con riêng mang chi_tiet_lam_sang.
 */
@Getter
public abstract class ExamDetailResponse extends ExamSummaryResponse {

    @JsonProperty("loai_benh_an")
    private final String medicalRecordType;

    @JsonProperty("huong_xu_tri_lan_sau")
    private final String nextVisitManagement;

    @JsonProperty("dan_do")
    private final String instructions;

    protected ExamDetailResponse(MedicalVisit v) {
        super(v);
        this.medicalRecordType = v.getMedicalRecordType();
        this.nextVisitManagement = v.getNextVisitManagement();
        this.instructions = v.getInstructions();
    }
}
