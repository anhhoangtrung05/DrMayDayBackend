package com.example.drmayday.features.prescription.controller;

import com.example.drmayday.common.dto.BaseResponse;
import com.example.drmayday.features.prescription.dto.CreatePrescriptionRequest;
import com.example.drmayday.features.prescription.dto.PrescriptionResponse;
import com.example.drmayday.features.prescription.dto.StopMedicationResponse;
import com.example.drmayday.features.prescription.dto.UpdatePrescriptionRequest;
import com.example.drmayday.features.prescription.dto.UpsertStopMedicationRequest;
import com.example.drmayday.features.prescription.service.PrescriptionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Prescriptions and their stop-medication instructions. The stop-instruction endpoints
 * live under /prescriptions so the controller keeps a single root path.
 */
@RestController
@RequestMapping("/api/v1/prescriptions")
public class PrescriptionController {

    private final PrescriptionService prescriptionService;

    public PrescriptionController(PrescriptionService prescriptionService) {
        this.prescriptionService = prescriptionService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','BAC_SI')")
    public ResponseEntity<BaseResponse<PrescriptionResponse>> createPrescription(
            @Valid @RequestBody CreatePrescriptionRequest req) {
        PrescriptionResponse data = prescriptionService.createPrescription(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.created("Kê đơn thuốc thành công", data));
    }

    // Query params are optional at the binding level so a missing value is reported
    // by the service as 2001 with a specific message, not by the framework.
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<List<PrescriptionResponse>>> getPrescriptions(
            @RequestParam(name = "phien_kham_id", required = false) Long examId,
            @RequestParam(name = "record_id", required = false) Long recordId) {
        return ResponseEntity.ok(BaseResponse.ok(prescriptionService.getPrescriptions(examId, recordId)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','BAC_SI')")
    public ResponseEntity<BaseResponse<PrescriptionResponse>> updatePrescription(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdatePrescriptionRequest req) {
        PrescriptionResponse data = prescriptionService.updatePrescription(id, req);
        return ResponseEntity.ok(BaseResponse.success("Cập nhật đơn thuốc thành công", data));
    }

    @PutMapping("/stop-instructions")
    @PreAuthorize("hasAnyRole('ADMIN','BAC_SI')")
    public ResponseEntity<BaseResponse<StopMedicationResponse>> upsertStopMedication(
            @Valid @RequestBody UpsertStopMedicationRequest req) {
        StopMedicationResponse data = prescriptionService.upsertStopMedication(req);
        return ResponseEntity.ok(BaseResponse.success("Cập nhật chỉ dẫn ngừng thuốc thành công", data));
    }

    @GetMapping("/stop-instructions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<StopMedicationResponse>> getStopMedication(
            @RequestParam(name = "don_thuoc_id", required = false) Long prescriptionId) {
        return ResponseEntity.ok(BaseResponse.ok(prescriptionService.getStopMedication(prescriptionId)));
    }
}
