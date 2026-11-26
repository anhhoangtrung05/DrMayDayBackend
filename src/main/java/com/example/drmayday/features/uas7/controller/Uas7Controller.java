package com.example.drmayday.features.uas7.controller;

import com.example.drmayday.common.dto.BaseResponse;
import com.example.drmayday.features.uas7.dto.CreateUas7CycleRequest;
import com.example.drmayday.features.uas7.dto.CreateUas7ScoreRequest;
import com.example.drmayday.features.uas7.dto.Uas7CycleDetailResponse;
import com.example.drmayday.features.uas7.dto.Uas7CycleResponse;
import com.example.drmayday.features.uas7.dto.Uas7EligibilityResponse;
import com.example.drmayday.features.uas7.dto.Uas7ScoreResponse;
import com.example.drmayday.features.uas7.dto.UpdateUas7CycleStatusRequest;
import com.example.drmayday.features.uas7.service.Uas7Service;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class Uas7Controller {

    private final Uas7Service uas7Service;

    @GetMapping("/api/v1/uas7-scores/eligibility")
    @PreAuthorize("hasAnyRole('PATIENT','BENH_NHAN')")
    public ResponseEntity<BaseResponse<Uas7EligibilityResponse>> checkUas7Eligibility() {
        return ResponseEntity.ok(BaseResponse.ok(uas7Service.checkEligibility()));
    }

    @PostMapping("/api/v1/uas7-scores")
    @PreAuthorize("hasAnyRole('PATIENT','BENH_NHAN')")
    public ResponseEntity<BaseResponse<Uas7ScoreResponse>> createUas7Score(
            @Valid @RequestBody CreateUas7ScoreRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.created(uas7Service.createScore(req)));
    }

    @GetMapping("/api/v1/uas7-scores/patient/{patient_id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<List<Uas7ScoreResponse>>> getUas7ScoresByPatient(
            @PathVariable("patient_id") String patientId,
            @RequestParam(name = "from_date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(name = "to_date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(name = "cycle_id", required = false) String cycleId) {
        return ResponseEntity.ok(BaseResponse.ok(uas7Service.getPatientScores(patientId, fromDate, toDate, cycleId)));
    }

    @PostMapping("/api/v1/patients/{patient_id}/uas7-cycles")
    @PreAuthorize("hasAnyRole('PATIENT','BENH_NHAN','ADMIN')")
    public ResponseEntity<BaseResponse<Uas7CycleResponse>> createUas7Cycle(
            @PathVariable("patient_id") String patientId,
            @Valid @RequestBody CreateUas7CycleRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.created(uas7Service.createCycle(patientId, req)));
    }

    @GetMapping("/api/v1/patients/{patient_id}/uas7-cycles")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<List<Uas7CycleResponse>>> getUas7CyclesByPatient(
            @PathVariable("patient_id") String patientId) {
        return ResponseEntity.ok(BaseResponse.ok(uas7Service.getPatientCycles(patientId)));
    }

    @GetMapping("/api/v1/uas7-cycles/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<Uas7CycleDetailResponse>> getUas7CycleDetail(
            @PathVariable("id") String id) {
        return ResponseEntity.ok(BaseResponse.ok(uas7Service.getCycleDetail(id)));
    }

    @PutMapping("/api/v1/uas7-cycles/{id}")
    @PreAuthorize("hasAnyRole('PATIENT','BENH_NHAN','ADMIN')")
    public ResponseEntity<BaseResponse<Uas7CycleResponse>> updateUas7CycleStatus(
            @PathVariable("id") String id,
            @Valid @RequestBody UpdateUas7CycleStatusRequest req) {
        return ResponseEntity.ok(BaseResponse.ok(uas7Service.updateCycleStatus(id, req)));
    }
}
