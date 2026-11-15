package com.example.drmayday.features.medicalrecord.controller;

import com.example.drmayday.common.dto.BaseResponse;
import com.example.drmayday.features.medicalrecord.dto.ChronicDiagnosisResponse;
import com.example.drmayday.features.medicalrecord.dto.UpsertChronicDiagnosisRequest;
import com.example.drmayday.features.medicalrecord.service.ChronicDiagnosisService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/chronic-diagnosis")
public class ChronicDiagnosisController {

    private final ChronicDiagnosisService service;

    public ChronicDiagnosisController(ChronicDiagnosisService service) {
        this.service = service;
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('ADMIN','BAC_SI')")
    public ResponseEntity<BaseResponse<ChronicDiagnosisResponse>> upsertChronicDiagnosis(
            @Valid @RequestBody UpsertChronicDiagnosisRequest req) {
        return ResponseEntity.ok(BaseResponse.ok(service.upsertChronicDiagnosis(req)));
    }
}
