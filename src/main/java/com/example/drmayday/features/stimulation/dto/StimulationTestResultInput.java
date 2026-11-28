package com.example.drmayday.features.stimulation.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
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
public class StimulationTestResultInput {

    @JsonProperty("ma_nghiem_phap")
    @JsonAlias({"maNghiemPhap", "provocationTestTypeId"})
    private Long maNghiemPhap;

    @JsonProperty("la_can_nguyen_khac")
    @JsonAlias({"laCanNguyenKhac", "otherCause"})
    private Boolean laCanNguyenKhac;

    @JsonProperty("can_nguyen_khac_ten")
    @JsonAlias({"canNguyenKhacTen", "otherCauseName"})
    private String canNguyenKhacTen;

    @JsonProperty("ket_qua")
    @JsonAlias({"ketQua", "testResult"})
    private String ketQua;

    @JsonProperty("nguong_gia_tri_so")
    @JsonAlias({"nguongGiaTriSo", "numericThreshold"})
    private BigDecimal nguongGiaTriSo;

    @JsonProperty("nguong_gia_tri_chu")
    @JsonAlias({"nguongGiaTriChu", "textThreshold"})
    private String nguongGiaTriChu;

    @JsonProperty("don_vi_do")
    @JsonAlias({"donViDo", "measurementUnit"})
    private String donViDo;

    @JsonProperty("diem_ngua_nrs")
    @JsonAlias({"diemNguaNrs", "itchNrsScore"})
    private Integer diemNguaNrs;

    @JsonProperty("diem_dau_nrs")
    @JsonAlias({"diemDauNrs", "painNrsScore"})
    private Integer diemDauNrs;

    @JsonProperty("diem_bong_rat_nrs")
    @JsonAlias({"diemBongRatNrs", "burningNrsScore"})
    private Integer diemBongRatNrs;
}
