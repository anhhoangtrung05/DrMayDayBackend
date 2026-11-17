package com.example.drmayday.features.medicalrecord.dto;

import com.example.drmayday.entity.MedicalRecord;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MedicalRecordResponse {
    @JsonProperty("record_id") private Long recordId;
    @JsonProperty("patient_id") private Long patientId;
    @JsonProperty("loai_benh_an") private String loaiBenhAn;
    @JsonProperty("trang_thai") private String trangThai;
    @JsonProperty("chan_doan_tuyen_truoc") private String chanDoanTuyenTruoc;
    @JsonProperty("chan_doan_ban_dau_phong_kham") private String chanDoanBanDauPhongKham;
    @JsonProperty("thoi_gian_khoi_phat_tuan") private BigDecimal thoiGianKhoiPhatTuan;
    @JsonProperty("thoi_diem_kham_lan_dau") private String thoiDiemKhamLanDau;
    @JsonProperty("ngay_bat_dau_dieu_tri_ngoai_tru") private String ngayBatDauDieuTriNgoaiTru;
    @JsonProperty("ngay_ket_thuc_dieu_tri_ngoai_tru") private String ngayKetThucDieuTriNgoaiTru;
    @JsonProperty("ket_qua_dieu_tri_bao_hiem") private String ketQuaDieuTriBaoHiem;
    @JsonProperty("tien_su_gia_dinh") private String tienSuGiaDinh;
    @JsonProperty("tien_su_gia_dinh_ghi_ro") private String tienSuGiaDinhGhiRo;
    @JsonProperty("da_dieu_tri_truoc_kham") private Boolean daDieuTriTruocKham;
    @JsonProperty("ho_ten") private String hoTen;

    public static MedicalRecordResponse from(MedicalRecord r) {
        return MedicalRecordResponse.builder()
                .recordId(r.getMedicalRecordId())
                .patientId(r.getPatientId())
                .loaiBenhAn(r.getMedicalRecordType())
                .trangThai(r.getStatus())
                .chanDoanTuyenTruoc(r.getReferringFacilityDiagnosis())
                .chanDoanBanDauPhongKham(r.getInitialClinicDiagnosis())
                .thoiGianKhoiPhatTuan(r.getOnsetDurationWeeks())
                .thoiDiemKhamLanDau(r.getFirstVisitAt() != null ? r.getFirstVisitAt().toString() : null)
                .ngayBatDauDieuTriNgoaiTru(r.getOutpatientTreatmentStartDate() != null ? r.getOutpatientTreatmentStartDate().toString() : null)
                .ngayKetThucDieuTriNgoaiTru(r.getOutpatientTreatmentEndDate() != null ? r.getOutpatientTreatmentEndDate().toString() : null)
                .ketQuaDieuTriBaoHiem(r.getInsuranceTreatmentOutcome())
                .tienSuGiaDinh(r.getFamilyHistory())
                .tienSuGiaDinhGhiRo(r.getFamilyHistoryDescription())
                .daDieuTriTruocKham(r.getTreatedBeforeVisit())
                .build();
    }

    public static MedicalRecordResponse fromWithName(MedicalRecord r, String hoTen) {
        MedicalRecordResponse resp = from(r);
        resp.setHoTen(hoTen);
        return resp;
    }
}
