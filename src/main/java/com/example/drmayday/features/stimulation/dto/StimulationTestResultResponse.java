package com.example.drmayday.features.stimulation.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.ALWAYS)
public class StimulationTestResultResponse {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("ma_phien_nghiem_phap")
    private Long maPhienNghiemPhap;

    @JsonProperty("ma_nghiem_phap")
    private Long maNghiemPhap;

    @JsonProperty("ten_nghiem_phap")
    private String tenNghiemPhap;

    @JsonProperty("la_can_nguyen_khac")
    private Boolean laCanNguyenKhac;

    @JsonProperty("can_nguyen_khac_ten")
    private String canNguyenKhacTen;

    @JsonProperty("ket_qua")
    private String ketQua;

    @JsonProperty("nguong_gia_tri_so")
    private BigDecimal nguongGiaTriSo;

    @JsonProperty("nguong_gia_tri_chu")
    private String nguongGiaTriChu;

    @JsonProperty("don_vi_do")
    private String donViDo;

    @JsonProperty("diem_ngua_nrs")
    private Integer diemNguaNrs;

    @JsonProperty("diem_dau_nrs")
    private Integer diemDauNrs;

    @JsonProperty("diem_bong_rat_nrs")
    private Integer diemBongRatNrs;
}
