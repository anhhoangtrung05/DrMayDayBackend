package com.example.drmayday.features.testtype.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.ALWAYS)
public class TestTypeResponse {

    @JsonProperty("ma_nghiem_phap")
    private Long maNghiemPhap;

    @JsonProperty("ten_nghiem_phap")
    private String tenNghiemPhap;

    @JsonProperty("phuong_phap_thuc_hien")
    private String phuongPhapThucHien;

    @JsonProperty("thoi_gian_chuan")
    private Integer thoiGianChuan;

    @JsonProperty("la_can_nguyen_khac")
    private Boolean laCanNguyenKhac;
}
