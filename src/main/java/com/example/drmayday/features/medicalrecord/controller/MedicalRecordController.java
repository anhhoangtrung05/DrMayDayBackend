package com.example.drmayday.features.medicalrecord.controller;

import com.example.drmayday.common.dto.BaseResponse;
import com.example.drmayday.features.medicalrecord.dto.*;
import com.example.drmayday.features.medicalrecord.service.MedicalRecordService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/medical-records")
public class MedicalRecordController {

    private final MedicalRecordService service;

    public MedicalRecordController(MedicalRecordService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','BAC_SI')")
    public ResponseEntity<BaseResponse<MedicalRecordResponse>> createMedicalRecord(
            @Valid @RequestBody CreateMedicalRecordRequest req) {
        MedicalRecordResponse response = service.createMedicalRecord(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.created(response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','BAC_SI')")
    public ResponseEntity<BaseResponse<Page<MedicalRecordResponse>>> listMedicalRecords(
            @RequestParam(name = "patient_id", required = false) String patientId,
            @RequestParam(name = "loai_benh_an", required = false) String loaiBenhAn,
            @RequestParam(name = "trang_thai", required = false) String trangThai,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<MedicalRecordResponse> result = service.listMedicalRecords(patientId, loaiBenhAn, trangThai, page, size);
        return ResponseEntity.ok(BaseResponse.ok(result));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','BAC_SI','BENH_NHAN')")
    public ResponseEntity<BaseResponse<MedicalRecordDetailResponse>> getMedicalRecord(
            @PathVariable String id) {
        return ResponseEntity.ok(BaseResponse.ok(service.getMedicalRecord(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','BAC_SI')")
    public ResponseEntity<BaseResponse<MedicalRecordResponse>> updateMedicalRecord(
            @PathVariable String id,
            @Valid @RequestBody UpdateMedicalRecordRequest req) {
        return ResponseEntity.ok(BaseResponse.ok(service.updateMedicalRecord(id, req)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','BAC_SI')")
    public ResponseEntity<BaseResponse<Void>> deleteMedicalRecord(@PathVariable String id) {
        service.deleteMedicalRecord(id);
        return ResponseEntity.ok(BaseResponse.ok());
    }

    @GetMapping("/by-patient/{patient_id}/history")
    @PreAuthorize("hasAnyRole('ADMIN','BAC_SI','BENH_NHAN')")
    public ResponseEntity<BaseResponse<List<ExamHistoryResponse>>> getPatientRecordHistory(
            @PathVariable("patient_id") String patientId) {
        return ResponseEntity.ok(BaseResponse.ok(service.getPatientRecordHistory(patientId)));
    }

    @PutMapping("/{record_id}/self-history")
    @PreAuthorize("hasAnyRole('ADMIN','BAC_SI')")
    public ResponseEntity<BaseResponse<SelfHistoryResponse>> upsertSelfHistory(
            @PathVariable("record_id") String recordId,
            @Valid @RequestBody UpsertSelfHistoryRequest req) {
        req.setRecordId(Long.parseLong(recordId));
        SelfHistoryResponse response = service.upsertSelfHistory(req);
        if (response.isCreated()) {
            return ResponseEntity.status(HttpStatus.CREATED).body(BaseResponse.created(response));
        }
        return ResponseEntity.ok(BaseResponse.ok(response));
    }
}
