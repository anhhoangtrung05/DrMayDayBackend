package com.example.drmayday.features.lab.dto;

import com.example.drmayday.entity.AcuteClinicalTestResult;
import com.example.drmayday.entity.ChronicClinicalTestResult;
import com.example.drmayday.entity.ClinicalTestResult;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Flat lab result: common fields plus the fields of the result's own group (CAP or MAN).
 * Fields of the other group stay null; clients read {@code loai} to know which apply.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabResultResponse {

    /** True when the upsert inserted a new result (201), false when it replaced one (200). */
    @JsonIgnore
    private boolean created;

    @JsonProperty("lab_result_id") private String labResultId;
    @JsonProperty("record_id") private String recordId;
    @JsonProperty("phien_kham_id") private String phienKhamId;
    @JsonProperty("loai") private String loai;

    @JsonProperty("wbc") private BigDecimal wbc;
    @JsonProperty("crp") private BigDecimal crp;
    @JsonProperty("ige_toan_phan") private BigDecimal igeToanPhan;
    @JsonProperty("xet_nghiem_khac") private String xetNghiemKhac;
    @JsonProperty("can_lam_sang_khac_ghi_chu") private String canLamSangKhacGhiChu;
    @JsonProperty("thoi_diem_thuc_hien") private OffsetDateTime thoiDiemThucHien;

    // CAP
    @JsonProperty("lym") private BigDecimal lym;
    @JsonProperty("neu") private BigDecimal neu;
    @JsonProperty("plt") private BigDecimal plt;
    @JsonProperty("tryptase_cap") private BigDecimal tryptaseCap;
    @JsonProperty("tryptase_nen") private BigDecimal tryptaseNen;
    @JsonProperty("nlr") private BigDecimal nlr;
    @JsonProperty("plr") private BigDecimal plr;

    // MAN
    @JsonProperty("test_huyet_thanh_tu_than") private String testHuyetThanhTuThan;
    @JsonProperty("ige_khang_tpo_ket_qua") private String igeKhangTpoKetQua;
    @JsonProperty("bat_ket_qua") private String batKetQua;
    @JsonProperty("ige_khang_il24_ket_qua") private String igeKhangIl24KetQua;
    @JsonProperty("ana_hep2") private String anaHep2;
    @JsonProperty("asst_duong_kinh_mm") private BigDecimal asstDuongKinhMm;
    @JsonProperty("eo") private BigDecimal eo;
    @JsonProperty("ba_bach_cau") private BigDecimal baBachCau;
    @JsonProperty("anti_tpo") private BigDecimal antiTpo;
    @JsonProperty("tsh") private BigDecimal tsh;
    @JsonProperty("ft4") private BigDecimal ft4;
    @JsonProperty("ft3") private BigDecimal ft3;
    @JsonProperty("mau_lang_2h") private BigDecimal mauLang2h;
    @JsonProperty("mau_lang_1h") private BigDecimal mauLang1h;
    @JsonProperty("sieu_am_tuyen_giap") private String sieuAmTuyenGiap;

    public static LabResultResponse fromAcute(Long recordId, ClinicalTestResult parent, AcuteClinicalTestResult cap) {
        LabResultResponseBuilder b = common(recordId, parent);
        if (cap != null) {
            b.lym(cap.getLymphocytes())
                    .neu(cap.getNeutrophils())
                    .plt(cap.getPlatelets())
                    .tryptaseCap(cap.getAcuteTryptase())
                    .tryptaseNen(cap.getBaselineTryptase())
                    .nlr(cap.getNeutrophilToLymphocyteRatio())
                    .plr(cap.getPlateletToLymphocyteRatio());
        }
        return b.build();
    }

    public static LabResultResponse fromChronic(Long recordId, ClinicalTestResult parent, ChronicClinicalTestResult man) {
        LabResultResponseBuilder b = common(recordId, parent);
        if (man != null) {
            b.testHuyetThanhTuThan(man.getAutologousSerumTest())
                    .igeKhangTpoKetQua(man.getAntiTpoIgeResult())
                    .batKetQua(man.getBasophilActivationTestResult())
                    .igeKhangIl24KetQua(man.getAntiIl24IgeResult())
                    .anaHep2(man.getAnaHep2Result())
                    .asstDuongKinhMm(man.getAsstDiameterMm())
                    .eo(man.getEosinophils())
                    .baBachCau(man.getBasophils())
                    .antiTpo(man.getAntiTpo())
                    .tsh(man.getTsh())
                    .ft4(man.getFt4())
                    .ft3(man.getFt3())
                    .mauLang2h(man.getEsrAtTwoHours())
                    .mauLang1h(man.getEsrAtOneHour())
                    .sieuAmTuyenGiap(man.getThyroidUltrasoundResult());
        }
        return b.build();
    }

    private static LabResultResponseBuilder common(Long recordId, ClinicalTestResult parent) {
        return LabResultResponse.builder()
                .labResultId(String.valueOf(parent.getClinicalTestResultId()))
                .recordId(recordId == null ? null : String.valueOf(recordId))
                .phienKhamId(String.valueOf(parent.getMedicalVisitId()))
                .loai(parent.getClinicalTestResultType())
                .wbc(parent.getWbc())
                .crp(parent.getCrp())
                .igeToanPhan(parent.getTotalIge())
                .xetNghiemKhac(parent.getOtherLaboratoryTest())
                .canLamSangKhacGhiChu(parent.getOtherClinicalTestNotes())
                .thoiDiemThucHien(parent.getPerformedAt());
    }
}
