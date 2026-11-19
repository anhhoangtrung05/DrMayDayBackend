package com.example.drmayday.features.uas7.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

@Data
public class CreateUas7ScoreRequest {
    @NotBlank(message = "2001") @JsonProperty("record_id") private String recordId;
    @NotNull(message = "2001") @Min(value = 0, message = "5003") @Max(value = 3, message = "5003") @JsonProperty("hss_score") private Integer hssScore;
    @NotNull(message = "2001") @Min(value = 0, message = "5003") @Max(value = 3, message = "5003") @JsonProperty("iss_score") private Integer issScore;
    @JsonProperty("date") private LocalDate date;
    @JsonProperty("trang_thai_dung_thuoc") private String medicationAdherenceStatus;
}
