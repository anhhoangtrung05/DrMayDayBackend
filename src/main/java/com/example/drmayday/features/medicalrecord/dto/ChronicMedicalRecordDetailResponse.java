package com.example.drmayday.features.medicalrecord.dto;

import com.example.drmayday.entity.MedicalRecord;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * Chi tiết hồ sơ MAN (API 36). Không có @JsonInclude, nên bảng con chưa có dòng vẫn trả null.
 * Không có infection_status, vì bảng đó chỉ dành cho hồ sơ CAP.
 */
@Getter
public class ChronicMedicalRecordDetailResponse extends MedicalRecordResponse {

    @JsonProperty("diagnosis")
    private final ChronicDiagnosisDto diagnosis;

    @JsonProperty("self_history")
    private final SelfHistoryDto selfHistory;

    @JsonProperty("allergy_context")
    private final AllergyContextDto allergyContext;

    @JsonProperty("previous_episodes")
    private final PreviousEpisodesDto previousEpisodes;

    public ChronicMedicalRecordDetailResponse(MedicalRecord record, ChronicDiagnosisDto diagnosis,
                                              SelfHistoryDto selfHistory, AllergyContextDto allergyContext,
                                              PreviousEpisodesDto previousEpisodes) {
        super(record);
        this.diagnosis = diagnosis;
        this.selfHistory = selfHistory;
        this.allergyContext = allergyContext;
        this.previousEpisodes = previousEpisodes;
    }
}
