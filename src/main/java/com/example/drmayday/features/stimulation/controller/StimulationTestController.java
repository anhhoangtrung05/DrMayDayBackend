package com.example.drmayday.features.stimulation.controller;

import com.example.drmayday.common.dto.BaseResponse;
import com.example.drmayday.features.stimulation.dto.StimulationTestResultInput;
import com.example.drmayday.features.stimulation.dto.StimulationTestResultResponse;
import com.example.drmayday.features.stimulation.dto.StimulationTestSessionDetailResponse;
import com.example.drmayday.features.stimulation.dto.StimulationTestSessionResponse;
import com.example.drmayday.features.stimulation.dto.UpsertStimulationTestSessionRequest;
import com.example.drmayday.features.stimulation.service.StimulationTestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class StimulationTestController {

    private final StimulationTestService stimulationTestService;

    @PutMapping("/api/v1/medical-records/{ho_so_id}/exams/{phien_kham_id}/stimulation-test-session")
    @PreAuthorize("hasAnyRole('ADMIN','BAC_SI','DOCTOR','DIEU_DUONG','NURSE')")
    public ResponseEntity<BaseResponse<StimulationTestSessionResponse>> upsertStimulationTestSession(
            @PathVariable("ho_so_id") Long hoSoId,
            @PathVariable("phien_kham_id") Long phienKhamId,
            @RequestBody(required = false) UpsertStimulationTestSessionRequest request) {
        return ResponseEntity.ok(BaseResponse.success(
                "Saved successfully",
                stimulationTestService.upsertSession(hoSoId, phienKhamId, request)));
    }

    @GetMapping("/api/v1/medical-records/{ho_so_id}/exams/{phien_kham_id}/stimulation-test-session")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<StimulationTestSessionDetailResponse>> getStimulationTestSession(
            @PathVariable("ho_so_id") Long hoSoId,
            @PathVariable("phien_kham_id") Long phienKhamId) {
        return ResponseEntity.ok(BaseResponse.ok(
                stimulationTestService.getSessionDetail(hoSoId, phienKhamId)));
    }

    @PostMapping("/api/v1/medical-records/{ho_so_id}/exams/{phien_kham_id}/stimulation-test-session/results")
    @PreAuthorize("hasAnyRole('ADMIN','BAC_SI','DOCTOR','DIEU_DUONG','NURSE')")
    public ResponseEntity<BaseResponse<List<StimulationTestResultResponse>>> createStimulationTestResults(
            @PathVariable("ho_so_id") Long hoSoId,
            @PathVariable("phien_kham_id") Long phienKhamId,
            @RequestBody(required = false) List<StimulationTestResultInput> requests) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.created(
                        "Created successfully",
                        stimulationTestService.createResultsBulk(hoSoId, phienKhamId, requests)));
    }

    @PutMapping("/api/v1/stimulation-test-results/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','BAC_SI','DOCTOR','DIEU_DUONG','NURSE')")
    public ResponseEntity<BaseResponse<StimulationTestResultResponse>> updateStimulationTestResult(
            @PathVariable Long id,
            @RequestBody(required = false) StimulationTestResultInput request) {
        return ResponseEntity.ok(BaseResponse.success(
                "Updated successfully",
                stimulationTestService.updateResult(id, request)));
    }

    @DeleteMapping("/api/v1/stimulation-test-results/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','BAC_SI','DOCTOR','DIEU_DUONG','NURSE')")
    public ResponseEntity<BaseResponse<Void>> deleteStimulationTestResult(@PathVariable Long id) {
        stimulationTestService.deleteResult(id);
        return ResponseEntity.ok(BaseResponse.success("Deleted successfully", null));
    }
}
