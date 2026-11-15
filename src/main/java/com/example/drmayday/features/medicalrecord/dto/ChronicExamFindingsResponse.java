package com.example.drmayday.features.medicalrecord.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChronicExamFindingsResponse {

    @JsonProperty("exam_id")
    private String examId;

    @JsonProperty("loai_benh_an")
    @Builder.Default
    private String loaiBenhAn = "MAN";

    @JsonProperty("hien_tai_co_san_phu")
    private Boolean hienTaiCoSanPhu;

    @JsonProperty("co_anh_san_phu")
    private Boolean coAnhSanPhu;

    @JsonProperty("uu_the_ngau_nhien")
    private Boolean uuTheNgauNhien;

    @JsonProperty("thoi_gian_ton_tai_san_phu")
    private String thoiGianTonTaiSanPhu;

    @JsonProperty("trong_dot_co_phu_mach")
    private Boolean trongDotCoPhuMach;

    @JsonProperty("hien_tai_co_phu_mach")
    private Boolean hienTaiCoPhuMach;

    @JsonProperty("co_anh_phu_mach")
    private Boolean coAnhPhuMach;

    @JsonProperty("phu_vi_tri_mat")
    private Boolean phuViTriMat;

    @JsonProperty("phu_vi_tri_moi")
    private Boolean phuViTriMoi;

    @JsonProperty("phu_vi_tri_luoi")
    private Boolean phuViTriLuoi;

    @JsonProperty("phu_vi_tri_thanh_quan")
    private Boolean phuViTriThanhQuan;

    @JsonProperty("phu_vi_tri_ban_tay_chan")
    private Boolean phuViTriBanTayChan;

    @JsonProperty("phu_vi_tri_bo_phan_sinh_duc")
    private Boolean phuViTriBoPhanSinhDuc;

    @JsonProperty("phu_vi_tri_khac")
    private Boolean phuViTriKhac;

    @JsonProperty("phu_vi_tri_khac_ghi_ro")
    private String phuViTriKhacGhiRo;

    @JsonProperty("thoi_gian_ton_tai_phu_mach")
    private String thoiGianTonTaiPhuMach;

    @JsonProperty("diem_aas7")
    private Integer diemAas7;

    @JsonProperty("chan_doan_tai_kham")
    private String chanDoanTaiKham;
}
