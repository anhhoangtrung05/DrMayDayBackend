package com.example.drmayday.features.medicalrecord.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Duration;

@Data
public class UpsertAcuteDiagnosisRequest {
    @NotNull(message = "2001")
    @JsonProperty("record_id")
    private Long recordId;

    @JsonProperty("nhap_vien") private Boolean nhapVien;
    @JsonProperty("cd_soc_phan_ve") private Boolean cdSocPhanVe;
    @JsonProperty("cd_phan_ve") private Boolean cdPhanVe;
    @JsonProperty("cd_may_day_cap_nang") private Boolean cdMayDayCapNang;
    @JsonProperty("cd_may_day_cap_thong_thuong") private Boolean cdMayDayCapThongThuong;

    @JsonProperty("tien_su_benh_nen_da") private String tienSuBenhNenDa;
    @JsonProperty("tien_su_benh_nen_ho_hap") private String tienSuBenhNenHoHap;
    @JsonProperty("tien_su_benh_nen_tuan_hoan") private String tienSuBenhNenTuanHoan;
    @JsonProperty("tien_su_benh_tim_mach_ghi_ro") private String tienSuBenhTimMachGhiRo;
    @JsonProperty("tien_su_benh_tieu_hoa_man_tinh_ghi_ro") private String tienSuBenhTieuHoaManTinhGhiRo;
    @JsonProperty("tien_su_benh_nen_tieu_hoa") private String tienSuBenhNenTieuHoa;

    @JsonProperty("nhom_yeu_to_nghi_ngo") private String nhomYeuToNghiNgo;
    @JsonProperty("co_nghi_ngo_dot_nay") private Boolean coNghiNgoDotNay;
    @JsonProperty("ten_yeu_to_nghi_ngo") private String tenYeuToNghiNgo;
    @JsonProperty("loai_tien_su") private String loaiTienSu;
    @JsonProperty("da_tung_phan_ung_truoc_day") private Boolean daTungPhanUngTruocDay;

    @JsonProperty("thoi_gian_tiep_xuc_den_khoi_phat") private String thoiGianTiepXucDenKhoiPhat;
    @JsonProperty("thoi_gian_keo_dai_tb") private String thoiGianKeoDaiTb;
    @JsonProperty("dot_gan_nhat_cach") private String dotGanNhatCach;

    @JsonProperty("tien_su_phan_ve_ghi_ro") private String tienSuPhanVeGhiRo;
    @JsonProperty("tien_su_di_ung_ghi_ro") private String tienSuDiUngGhiRo;

    @JsonProperty("da_dieu_tri_benh_dot_nay") private Boolean daDieuTriBenhDotNay;
    @JsonProperty("tuoi_lan_dau_bi_benh") private Integer tuoiLanDauBiBenh;
    @JsonProperty("so_lan_may_day_cap_truoc") private Integer soLanMayDayCapTruoc;
    @JsonProperty("benh_phu") private String benhPhu;
}
