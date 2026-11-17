package com.example.drmayday.features.medicalrecord.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO tạo hồ sơ bệnh án (API 34). Giống DTO sửa, thêm loai_benh_an bắt buộc.
 */
public class CreateMedicalRecordRequest extends UpdateMedicalRecordRequest {

    @NotBlank(message = "2001")
    @JsonProperty("loai_benh_an")
    private String medicalRecordType;

    public CreateMedicalRecordRequest() {
    }

    public String getMedicalRecordType() {
        return medicalRecordType;
    }

    public void setMedicalRecordType(String medicalRecordType) {
        this.medicalRecordType = medicalRecordType;
    }
}
