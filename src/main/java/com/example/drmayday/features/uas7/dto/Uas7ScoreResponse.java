package com.example.drmayday.features.uas7.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data @Builder
public class Uas7ScoreResponse {
    @JsonProperty("uas7_id") private String uas7Id;
    @JsonProperty("record_id") private String recordId;
    @JsonProperty("cycle_id") private String cycleId;
    @JsonProperty("hss_score") private Short hssScore;
    @JsonProperty("iss_score") private Short issScore;
    @JsonProperty("total_score") private Short totalScore;
    @JsonProperty("ngay_cham_diem") private LocalDate scoreDate;
    @JsonProperty("ngay_thu_may_trong_chu_ky") private Short cycleDay;
    @JsonProperty("trang_thai_ngay") private String dayStatus;
    @JsonProperty("trang_thai_dung_thuoc") private String medicationAdherenceStatus;
    @JsonProperty("created_at") private OffsetDateTime createdAt;
}
