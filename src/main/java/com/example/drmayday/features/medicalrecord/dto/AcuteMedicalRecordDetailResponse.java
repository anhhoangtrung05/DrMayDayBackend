package com.example.drmayday.features.medicalrecord.dto;

import com.example.drmayday.entity.MedicalRecord;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * Chi tiết hồ sơ CAP (API 36). Không có @JsonInclude, nên bảng con chưa có dòng vẫn trả null.
 * Không có previous_episodes, vì bảng đó chỉ dành cho hồ sơ MAN.
 */
@Getter
public class AcuteMedicalRecordDetailResponse extends MedicalRecordResponse {

    @JsonProperty("diagnosis")
    private final AcuteDiagnosisDto diagnosis;

    @JsonProperty("self_history")
    private final SelfHistoryDto selfHistory;

    @JsonProperty("allergy_context")
    private final AllergyContextDto allergyContext;

    @JsonProperty("infection_status")
    private final InfectionStatusDto infectionStatus;

    public AcuteMedicalRecordDetailResponse(MedicalRecord record, AcuteDiagnosisDto diagnosis,
                                            SelfHistoryDto selfHistory, AllergyContextDto allergyContext,
                                            InfectionStatusDto infectionStatus) {
        super(record);
        this.diagnosis = diagnosis;
        this.selfHistory = selfHistory;
        this.allergyContext = allergyContext;
        this.infectionStatus = infectionStatus;
    }
}
