package com.example.drmayday.features.medicalrecord.dto;

import com.example.drmayday.entity.MedicalVisit;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * Chi tiết phiên khám CAP (API 48). chi_tiet_lam_sang là dòng phienkhamcap, null nếu dòng đó không có.
 */
@Getter
public class AcuteExamDetailResponse extends ExamDetailResponse {

    @JsonProperty("chi_tiet_lam_sang")
    private final AcuteExamFindingsDto findings;

    public AcuteExamDetailResponse(MedicalVisit visit, AcuteExamFindingsDto findings) {
        super(visit);
        this.findings = findings;
    }
}
