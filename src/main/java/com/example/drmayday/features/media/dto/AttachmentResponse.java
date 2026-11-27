package com.example.drmayday.features.media.dto;

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
public class AttachmentResponse {

    @JsonProperty("attachment_id")
    private String attachmentId;

    @JsonProperty("record_id")
    private String recordId;

    @JsonProperty("ten_tep_goc")
    private String originalFileName;

    @JsonProperty("duong_dan_url")
    private String url;

    @JsonProperty("dinh_dang_tep")
    private String fileFormat;

    @JsonProperty("loai_phuong_tien")
    private String mediaType;

    @JsonProperty("dung_luong_byte")
    private Long sizeBytes;

    @JsonProperty("phan_loai_muc_dich")
    private String purposeCategory;

    @JsonProperty("mo_ta_tep")
    private String fileDescription;

    @JsonProperty("thoi_diem_tao")
    private OffsetDateTime createdAt;
}
