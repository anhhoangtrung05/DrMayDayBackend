package com.example.drmayday.features.medicalrecord.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO tạo phiên khám (API 46). Gửi null tường minh được coi như không gửi.
 * Không có ma_bac_si: phiên luôn đứng tên người gọi. record_id là chuỗi để service trả 2001, 2002 theo đúng thứ tự.
 */
@Getter
@Setter
@NoArgsConstructor
public class CreateExamRequest extends UpdateExamRequest {

    @JsonProperty("record_id")
    private String recordId;

    @JsonProperty("lan_kham_thu")
    private Integer visitNumber;
}
