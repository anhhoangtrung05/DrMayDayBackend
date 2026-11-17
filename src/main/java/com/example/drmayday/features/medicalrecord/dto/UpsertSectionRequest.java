package com.example.drmayday.features.medicalrecord.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Phần chung của ba DTO upsert bệnh sử (API 43 đến 45): record_id và danh sách trường lạ.
 * Spring Boot tắt FAIL_ON_UNKNOWN_PROPERTIES, nên trường lạ được gom ở đây để service trả 2003.
 * Với cách ghi đè toàn bộ, một trường gõ sai mà bị bỏ qua sẽ âm thầm xóa trường thật.
 */
public abstract class UpsertSectionRequest {

    @NotBlank(message = "2001")
    @JsonProperty("record_id")
    private String recordId;

    private final Set<String> unknownFields = new LinkedHashSet<>();

    public String getRecordId() {
        return recordId;
    }

    public void setRecordId(String recordId) {
        this.recordId = recordId;
    }

    @JsonAnySetter
    void addUnknownField(String name, Object value) {
        unknownFields.add(name);
    }

    public boolean hasUnknownFields() {
        return !unknownFields.isEmpty();
    }

    public Set<String> unknownFields() {
        return Set.copyOf(unknownFields);
    }
}
