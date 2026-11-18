package com.example.drmayday.features.medicalrecord.controller;

import com.example.drmayday.common.dto.BaseResponse;
import com.example.drmayday.features.medicalrecord.dto.AddVitalSignsRequest;
import com.example.drmayday.features.medicalrecord.dto.VitalSignsResponse;
import com.example.drmayday.features.medicalrecord.service.VitalSignsService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class VitalSignsController {

    private final VitalSignsService vitalSignsService;

    public VitalSignsController(VitalSignsService vitalSignsService) {
        this.vitalSignsService = vitalSignsService;
    }

    @PostMapping("/api/v1/vital-signs")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public ResponseEntity<BaseResponse<VitalSignsResponse>> addVitalSigns(
            @Valid @RequestBody AddVitalSignsRequest req) {
        VitalSignsResponse data = vitalSignsService.addVitalSigns(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.created("Thêm dấu hiệu sinh tồn thành công", data));
    }

    @GetMapping("/api/v1/vital-signs")
    @PreAuthorize("hasAnyRole('PATIENT','DOCTOR','ADMIN')")
    public ResponseEntity<BaseResponse<List<VitalSignsResponse>>> getListVitalSigns(
            @RequestParam(required = false) String exam_id) {
        List<VitalSignsResponse> data = vitalSignsService.getListVitalSigns(exam_id);
        return ResponseEntity.ok(BaseResponse.success("Thành công", data));
    }
}
