package com.example.drmayday.features.medicalrecord.dto;

import com.example.drmayday.entity.MedicalVisit;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExamHistoryResponse {
    @JsonProperty("record_id") private Long recordId;
    @JsonProperty("loai_benh_an") private String loaiBenhAn;
    @JsonProperty("exam_id") private Long examId;
    @JsonProperty("lan_kham_thu") private Integer lanKhamThu;
    @JsonProperty("ngay_kham") private String ngayKham;
    @JsonProperty("ma_bac_si") private Long maBacSi;
    @JsonProperty("loai_phien_kham") private String loaiPhienKham;
    @JsonProperty("ngay_hen_tai_kham") private String ngayHenTaiKham;

    public static ExamHistoryResponse from(MedicalVisit v) {
        return ExamHistoryResponse.builder()
                .recordId(v.getMedicalRecordId())
                .loaiBenhAn(v.getMedicalRecordType())
                .examId(v.getMedicalVisitId())
                .lanKhamThu(v.getVisitNumber())
                .ngayKham(v.getVisitDate() != null ? v.getVisitDate().toString() : null)
                .maBacSi(v.getPhysicianId())
                .loaiPhienKham(v.getVisitType())
                .ngayHenTaiKham(v.getFollowUpDate() != null ? v.getFollowUpDate().toString() : null)
                .build();
    }
}
