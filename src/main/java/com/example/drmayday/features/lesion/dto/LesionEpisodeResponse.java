package com.example.drmayday.features.lesion.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LesionEpisodeResponse {

    @JsonProperty("episode_id")
    private String episodeId;

    @JsonProperty("patient_id")
    private String patientId;

    @JsonProperty("ho_so_man_id")
    private String hoSoManId;

    @JsonProperty("ten_dot_ton_thuong_da")
    private String tenDotTonThuongDa;

    @JsonProperty("thoi_diem_xuat_hien")
    private OffsetDateTime thoiDiemXuatHien;

    @JsonProperty("hoan_canh_xuat_hien")
    private String hoanCanhXuatHien;

    @JsonProperty("hoan_canh_xuat_hien_khac")
    private String hoanCanhXuatHienKhac;

    @JsonProperty("ten_thuoc_dung_gan_thoi_diem_xuat_hien")
    private String tenThuocDungGanThoiDiemXuatHien;

    @JsonProperty("thoi_gian_dung_thuoc_truoc_khi_xuat_hien")
    private String thoiGianDungThuocTruocKhiXuatHien;

    @JsonProperty("don_vi_thoi_gian")
    private String donViThoiGian;

    @JsonProperty("chi_tiet_dot_ton_thuong")
    private String chiTietDotTonThuong;

    @JsonProperty("thoi_diem_tao")
    private OffsetDateTime thoiDiemTao;
}
