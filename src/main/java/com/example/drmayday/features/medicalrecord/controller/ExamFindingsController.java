package com.example.drmayday.features.medicalrecord.controller;

import com.example.drmayday.common.dto.BaseResponse;
import com.example.drmayday.features.medicalrecord.dto.AcuteExamFindingsResponse;
import com.example.drmayday.features.medicalrecord.dto.ChronicExamFindingsResponse;
import com.example.drmayday.features.medicalrecord.dto.UpsertAcuteExamFindingsRequest;
import com.example.drmayday.features.medicalrecord.dto.UpsertChronicExamFindingsRequest;
import com.example.drmayday.features.medicalrecord.service.ExamFindingsService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ExamFindingsController {

    private final ExamFindingsService examFindingsService;

    public ExamFindingsController(ExamFindingsService examFindingsService) {
        this.examFindingsService = examFindingsService;
    }

    @PutMapping("/api/v1/acute-exam-findings")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public ResponseEntity<BaseResponse<AcuteExamFindingsResponse>> upsertAcuteExamFindings(
            @Valid @RequestBody UpsertAcuteExamFindingsRequest req) {
        AcuteExamFindingsResponse data = examFindingsService.upsertAcuteExamFindings(req);
        return ResponseEntity.ok(BaseResponse.success("Cập nhật triệu chứng phiên khám cấp thành công", data));
    }

    @PutMapping("/api/v1/chronic-exam-findings")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public ResponseEntity<BaseResponse<ChronicExamFindingsResponse>> upsertChronicExamFindings(
            @Valid @RequestBody UpsertChronicExamFindingsRequest req) {
        ChronicExamFindingsResponse data = examFindingsService.upsertChronicExamFindings(req);
        return ResponseEntity.ok(BaseResponse.success("Cập nhật triệu chứng phiên khám mạn thành công", data));
    }
}
