package com.example.drmayday.features.lab.controller;

import com.example.drmayday.common.dto.BaseResponse;
import com.example.drmayday.features.lab.dto.LabResultResponse;
import com.example.drmayday.features.lab.dto.UpsertLabResultsRequest;
import com.example.drmayday.features.lab.service.LabResultService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/lab-results")
public class LabResultController {

    private final LabResultService labResultService;

    public LabResultController(LabResultService labResultService) {
        this.labResultService = labResultService;
    }

    /** Create-or-replace: 201 on the first write for a session, 200 afterwards. */
    @PutMapping
    @PreAuthorize("hasAnyRole('ADMIN','BAC_SI')")
    public ResponseEntity<BaseResponse<LabResultResponse>> upsertLabResults(
            @Valid @RequestBody UpsertLabResultsRequest req) {
        LabResultResponse data = labResultService.upsertLabResults(req);
        if (data.isCreated()) {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(BaseResponse.created("Lưu kết quả cận lâm sàng thành công", data));
        }
        return ResponseEntity.ok(BaseResponse.success("Cập nhật kết quả cận lâm sàng thành công", data));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<LabResultResponse>> getLabResults(
            @RequestParam(name = "phien_kham_id", required = false) Long examId) {
        return ResponseEntity.ok(BaseResponse.ok(labResultService.getLabResults(examId)));
    }
}
