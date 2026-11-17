package com.example.drmayday.features.medicalrecord.controller;

import com.example.drmayday.common.dto.BaseResponse;
import com.example.drmayday.features.medicalrecord.dto.AcuteDiagnosisResponse;
import com.example.drmayday.features.medicalrecord.dto.UpsertAcuteDiagnosisRequest;
import com.example.drmayday.features.medicalrecord.service.AcuteDiagnosisService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/acute-diagnosis")
public class AcuteDiagnosisController {

    private final AcuteDiagnosisService service;

    public AcuteDiagnosisController(AcuteDiagnosisService service) {
        this.service = service;
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('ADMIN','BAC_SI')")
    public ResponseEntity<BaseResponse<AcuteDiagnosisResponse>> upsertAcuteDiagnosis(
            @Valid @RequestBody UpsertAcuteDiagnosisRequest req) {
        return ResponseEntity.ok(BaseResponse.ok(service.upsertAcuteDiagnosis(req)));
    }
}
