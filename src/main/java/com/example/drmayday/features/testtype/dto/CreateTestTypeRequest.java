package com.example.drmayday.features.testtype.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTestTypeRequest {

    @JsonProperty("ten_nghiem_phap")
    @JsonAlias({"tenNghiemPhap", "testName", "name"})
    private String tenNghiemPhap;

    @JsonProperty("phuong_phap_thuc_hien")
    @JsonAlias({"phuongPhapThucHien", "implementationMethod"})
    private String phuongPhapThucHien;

    @JsonProperty("thoi_gian_chuan")
    @JsonAlias({"thoiGianChuan", "standardDuration"})
    private Integer thoiGianChuan;

    @JsonProperty("la_can_nguyen_khac")
    @JsonAlias({"laCanNguyenKhac", "otherCause"})
    private Boolean laCanNguyenKhac;
}
