package com.example.drmayday.features.uas7.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Uas7ScoreResponse {

    @JsonProperty("uas7_id")
    private String uas7Id;

    @JsonProperty("record_id")
    private String recordId;

    @JsonProperty("cycle_id")
    private String cycleId;

    @JsonProperty("hss_score")
    private Integer hssScore;

    @JsonProperty("iss_score")
    private Integer issScore;

    @JsonProperty("total_score")
    private Integer totalScore;

    @JsonProperty("ngay_cham_diem")
    private LocalDate ngayChamDiem;

    @JsonProperty("ngay_thu_may_trong_chu_ky")
    private Integer ngayThuMayTrongChuKy;

    @JsonProperty("trang_thai_ngay")
    private String trangThaiNgay;

    @JsonProperty("trang_thai_dung_thuoc")
    private String trangThaiDungThuoc;

    @JsonProperty("created_at")
    private OffsetDateTime createdAt;
}
