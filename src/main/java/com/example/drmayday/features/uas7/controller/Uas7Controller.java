package com.example.drmayday.features.uas7.controller;

import com.example.drmayday.common.dto.BaseResponse;
import com.example.drmayday.features.uas7.dto.*;
import com.example.drmayday.features.uas7.service.Uas7Service;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class Uas7Controller {
    private final Uas7Service uas7Service;

    @GetMapping("/api/v1/uas7-scores/eligibility")
    @PreAuthorize("hasAnyRole('PATIENT','BENH_NHAN')")
    public ResponseEntity<BaseResponse<Uas7EligibilityResponse>> eligibility() {
        return ResponseEntity.ok(BaseResponse.ok(uas7Service.checkEligibility()));
    }

    @PostMapping("/api/v1/uas7-scores")
    @PreAuthorize("hasAnyRole('PATIENT','BENH_NHAN')")
    public ResponseEntity<BaseResponse<Uas7ScoreResponse>> createScore(@Valid @RequestBody CreateUas7ScoreRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(BaseResponse.created(uas7Service.createScore(request)));
    }

    @GetMapping("/api/v1/uas7-scores/patient/{patient_id}")
    @PreAuthorize("hasAnyRole('PATIENT','BENH_NHAN','DOCTOR','BAC_SI','ADMIN')")
    public ResponseEntity<BaseResponse<List<Uas7ScoreResponse>>> scores(
            @PathVariable String patient_id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from_date,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to_date,
            @RequestParam(required = false) String cycle_id) {
        return ResponseEntity.ok(BaseResponse.ok(uas7Service.getPatientScores(patient_id, from_date, to_date, cycle_id)));
    }

    @PostMapping("/api/v1/patients/{patient_id}/uas7-cycles")
    @PreAuthorize("hasAnyRole('PATIENT','BENH_NHAN','ADMIN')")
    public ResponseEntity<BaseResponse<Uas7CycleResponse>> createCycle(@PathVariable String patient_id,
                                                                         @Valid @RequestBody CreateUas7CycleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(BaseResponse.created(uas7Service.createCycle(patient_id, request)));
    }

    @GetMapping("/api/v1/patients/{patient_id}/uas7-cycles")
    @PreAuthorize("hasAnyRole('PATIENT','BENH_NHAN','DOCTOR','BAC_SI','ADMIN')")
    public ResponseEntity<BaseResponse<List<Uas7CycleResponse>>> cycles(@PathVariable String patient_id) {
        return ResponseEntity.ok(BaseResponse.ok(uas7Service.getPatientCycles(patient_id)));
    }

    @GetMapping("/api/v1/uas7-cycles/{id}")
    @PreAuthorize("hasAnyRole('PATIENT','BENH_NHAN','DOCTOR','BAC_SI','ADMIN')")
    public ResponseEntity<BaseResponse<Uas7CycleDetailResponse>> cycleDetail(@PathVariable String id) {
        return ResponseEntity.ok(BaseResponse.ok(uas7Service.getCycleDetail(id)));
    }

    @PutMapping("/api/v1/uas7-cycles/{id}")
    @PreAuthorize("hasAnyRole('PATIENT','BENH_NHAN','ADMIN')")
    public ResponseEntity<BaseResponse<Uas7CycleResponse>> updateCycle(@PathVariable String id,
                                                                         @Valid @RequestBody UpdateUas7CycleStatusRequest request) {
        return ResponseEntity.ok(BaseResponse.ok(uas7Service.updateCycleStatus(id, request)));
    }
}
