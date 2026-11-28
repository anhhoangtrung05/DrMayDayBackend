package com.example.drmayday.features.uas7.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUas7ScoreRequest {

    @JsonProperty("record_id")
    @JsonAlias({"recordId", "ma_ho_so_man"})
    private Long recordId;

    @JsonProperty("hss_score")
    @JsonAlias({"hssScore", "diem_san_phu_hss"})
    private Integer hssScore;

    @JsonProperty("iss_score")
    @JsonAlias({"issScore", "diem_ngua_iss"})
    private Integer issScore;

    @JsonProperty("date")
    @JsonAlias({"ngay_cham_diem"})
    private LocalDate date;

    @JsonProperty("trang_thai_dung_thuoc")
    @JsonAlias({"medicationAdherenceStatus"})
    private String trangThaiDungThuoc;
}
