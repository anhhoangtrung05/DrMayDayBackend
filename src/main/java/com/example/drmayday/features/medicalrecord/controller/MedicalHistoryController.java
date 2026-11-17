package com.example.drmayday.features.medicalrecord.controller;

import com.example.drmayday.common.dto.BaseResponse;
import com.example.drmayday.features.medicalrecord.dto.AllergyContextResponse;
import com.example.drmayday.features.medicalrecord.dto.InfectionStatusResponse;
import com.example.drmayday.features.medicalrecord.dto.PreviousEpisodesResponse;
import com.example.drmayday.features.medicalrecord.dto.UpsertAllergyContextRequest;
import com.example.drmayday.features.medicalrecord.dto.UpsertInfectionStatusRequest;
import com.example.drmayday.features.medicalrecord.dto.UpsertPreviousEpisodesRequest;
import com.example.drmayday.features.medicalrecord.service.MedicalHistoryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller bệnh sử theo hồ sơ (Phân hệ Bệnh nhân & Hồ sơ bệnh án).
 * Chứa API 43 (upsert_allergy_context), API 44 (upsert_previous_episodes), API 45 (upsert_infection_status).
 * Cả ba ghi đè toàn bộ phần đó: trường không gửi hoặc gửi null sẽ thành NULL, khác với API 37.
 */
@RestController
@RequestMapping("/api/v1")
public class MedicalHistoryController {

    private final MedicalHistoryService medicalHistoryService;

    public MedicalHistoryController(MedicalHistoryService medicalHistoryService) {
        this.medicalHistoryService = medicalHistoryService;
    }

    /**
     * API 43: upsert_allergy_context
     * Ghi hoàn cảnh khởi phát dị ứng cho hồ sơ CAP hoặc MAN đang mở. *_thoi_gian tính bằng giờ.
     *
     * @param req record_id và toàn bộ nội dung của phần
     * @return Phần vừa ghi
     */
    @PutMapping("/allergy-context")
    @PreAuthorize("hasAnyRole('ADMIN','BAC_SI')")
    public ResponseEntity<BaseResponse<AllergyContextResponse>> upsertAllergyContext(
            @Valid @RequestBody UpsertAllergyContextRequest req) {
        return ResponseEntity.ok(BaseResponse.success(medicalHistoryService.upsertAllergyContext(req)));
    }

    /**
     * API 44: upsert_previous_episodes
     * Ghi bệnh sử các đợt bệnh trước, chỉ cho hồ sơ MAN đang mở. thoi_gian_tu_dot_gan_nhat tính bằng tháng.
     *
     * @param req record_id và toàn bộ nội dung của phần
     * @return Phần vừa ghi
     */
    @PutMapping("/previous-episodes")
    @PreAuthorize("hasAnyRole('ADMIN','BAC_SI')")
    public ResponseEntity<BaseResponse<PreviousEpisodesResponse>> upsertPreviousEpisodes(
            @Valid @RequestBody UpsertPreviousEpisodesRequest req) {
        return ResponseEntity.ok(BaseResponse.success(medicalHistoryService.upsertPreviousEpisodes(req)));
    }

    /**
     * API 45: upsert_infection_status
     * Ghi tình trạng nhiễm trùng, chỉ cho hồ sơ CAP đang mở. *_thoi_gian và *_khoang_cach tính bằng ngày.
     *
     * @param req record_id và toàn bộ nội dung của phần
     * @return Phần vừa ghi
     */
    @PutMapping("/infection-status")
    @PreAuthorize("hasAnyRole('ADMIN','BAC_SI')")
    public ResponseEntity<BaseResponse<InfectionStatusResponse>> upsertInfectionStatus(
            @Valid @RequestBody UpsertInfectionStatusRequest req) {
        return ResponseEntity.ok(BaseResponse.success(medicalHistoryService.upsertInfectionStatus(req)));
    }
}
