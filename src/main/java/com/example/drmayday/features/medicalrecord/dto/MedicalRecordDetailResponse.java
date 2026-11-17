package com.example.drmayday.features.medicalrecord.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MedicalRecordDetailResponse {
    @JsonProperty("record") private MedicalRecordResponse record;
    @JsonProperty("diagnosis") private Object diagnosis;
    @JsonProperty("self_history") private SelfHistoryResponse selfHistory;
    @JsonProperty("allergy_context") private Object allergyContext;
    @JsonProperty("previous_episodes") private Object previousEpisodes;
    @JsonProperty("infection_status") private Object infectionStatus;
}
