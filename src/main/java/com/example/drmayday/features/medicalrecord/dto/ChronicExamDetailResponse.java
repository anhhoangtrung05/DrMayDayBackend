package com.example.drmayday.features.medicalrecord.dto;

import com.example.drmayday.entity.MedicalVisit;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * Chi tiết phiên khám MAN (API 48). chi_tiet_lam_sang là dòng phienkhamman, null nếu dòng đó không có.
 */
@Getter
public class ChronicExamDetailResponse extends ExamDetailResponse {

    @JsonProperty("chi_tiet_lam_sang")
    private final ChronicExamFindingsDto findings;

    public ChronicExamDetailResponse(MedicalVisit visit, ChronicExamFindingsDto findings) {
        super(visit);
        this.findings = findings;
    }
}
