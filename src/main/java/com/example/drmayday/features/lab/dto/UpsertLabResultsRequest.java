package com.example.drmayday.features.lab.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Create-or-replace payload for the lab result of one exam session. The CAP/MAN group
 * is taken from the session; sending fields of the other group is rejected (8103).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpsertLabResultsRequest {

    // ── Identification ──

    @JsonProperty("record_id")
    private Long recordId;

    @JsonProperty("phien_kham_id")
    private Long phienKhamId;

    // ── Common to CAP and MAN ──

    @JsonProperty("wbc")
    private BigDecimal wbc;

    @JsonProperty("crp")
    private BigDecimal crp;

    @JsonProperty("ige_toan_phan")
    private BigDecimal igeToanPhan;

    @JsonProperty("xet_nghiem_khac")
    private String xetNghiemKhac;

    @JsonProperty("can_lam_sang_khac_ghi_chu")
    private String canLamSangKhacGhiChu;

    @JsonProperty("thoi_diem_thuc_hien")
    private OffsetDateTime thoiDiemThucHien;

    // ── CAP only ──

    @JsonProperty("lym")
    private BigDecimal lym;

    @JsonProperty("neu")
    private BigDecimal neu;

    @JsonProperty("plt")
    private BigDecimal plt;

    @JsonProperty("tryptase_cap")
    private BigDecimal tryptaseCap;

    @JsonProperty("tryptase_nen")
    private BigDecimal tryptaseNen;

    // ── MAN only ──

    @JsonProperty("test_huyet_thanh_tu_than")
    private String testHuyetThanhTuThan;

    @JsonProperty("ige_khang_tpo_ket_qua")
    private String igeKhangTpoKetQua;

    @JsonProperty("bat_ket_qua")
    private String batKetQua;

    @JsonProperty("ige_khang_il24_ket_qua")
    private String igeKhangIl24KetQua;

    @JsonProperty("ana_hep2")
    private String anaHep2;

    @JsonProperty("asst_duong_kinh_mm")
    private BigDecimal asstDuongKinhMm;

    @JsonProperty("eo")
    private BigDecimal eo;

    @JsonProperty("ba_bach_cau")
    private BigDecimal baBachCau;

    @JsonProperty("anti_tpo")
    private BigDecimal antiTpo;

    @JsonProperty("tsh")
    private BigDecimal tsh;

    @JsonProperty("ft4")
    private BigDecimal ft4;

    @JsonProperty("ft3")
    private BigDecimal ft3;

    @JsonProperty("mau_lang_2h")
    private BigDecimal mauLang2h;

    @JsonProperty("mau_lang_1h")
    private BigDecimal mauLang1h;

    @JsonProperty("sieu_am_tuyen_giap")
    private String sieuAmTuyenGiap;
}
