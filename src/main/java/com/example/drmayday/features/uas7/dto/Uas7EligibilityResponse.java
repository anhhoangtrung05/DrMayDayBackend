package com.example.drmayday.features.uas7.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Uas7EligibilityResponse {

    @JsonProperty("is_eligible")
    private boolean eligible;

    @JsonProperty("active_record_id")
    private String activeRecordId;

    @JsonProperty("active_cycle_id")
    private String activeCycleId;

    @JsonProperty("current_day_in_cycle")
    private Integer currentDayInCycle;

    @JsonProperty("scored_today")
    private boolean scoredToday;

    @JsonProperty("today_score_id")
    private String todayScoreId;
}
