package com.example.drmayday.features.medicalrecord.controller;

import com.example.drmayday.common.dto.BaseResponse;
import com.example.drmayday.features.medicalrecord.dto.AddLesionDetailsBulkRequest;
import com.example.drmayday.features.medicalrecord.dto.LesionDetailResponse;
import com.example.drmayday.features.medicalrecord.dto.UpdateLesionDetailRequest;
import com.example.drmayday.features.medicalrecord.service.LesionDetailsService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class LesionDetailsController {

    private final LesionDetailsService lesionDetailsService;

    public LesionDetailsController(LesionDetailsService lesionDetailsService) {
        this.lesionDetailsService = lesionDetailsService;
    }

    @PostMapping("/api/v1/lesion-details")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public ResponseEntity<BaseResponse<List<LesionDetailResponse>>> addLesionDetailsBulk(
            @Valid @RequestBody AddLesionDetailsBulkRequest req) {
        List<LesionDetailResponse> data = lesionDetailsService.addLesionDetailsBulk(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.created("Thêm chi tiết tổn thương thành công", data));
    }

    @GetMapping("/api/v1/lesion-details")
    @PreAuthorize("hasAnyRole('PATIENT','DOCTOR','ADMIN')")
    public ResponseEntity<BaseResponse<List<LesionDetailResponse>>> getListLesionDetails(
            @RequestParam(required = false) String exam_id) {
        List<LesionDetailResponse> data = lesionDetailsService.getListLesionDetails(exam_id);
        return ResponseEntity.ok(BaseResponse.success("Thành công", data));
    }

    @PutMapping("/api/v1/lesion-details/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public ResponseEntity<BaseResponse<LesionDetailResponse>> updateLesionDetail(
            @PathVariable String id,
            @RequestParam(required = false) String loai_benh_an,
            @Valid @RequestBody UpdateLesionDetailRequest req) {
        LesionDetailResponse data = lesionDetailsService.updateLesionDetail(id, loai_benh_an, req);
        return ResponseEntity.ok(BaseResponse.success("Cập nhật chi tiết tổn thương thành công", data));
    }

    @DeleteMapping("/api/v1/lesion-details/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public ResponseEntity<BaseResponse<Void>> deleteLesionDetail(
            @PathVariable String id,
            @RequestParam(required = false) String loai_benh_an) {
        lesionDetailsService.deleteLesionDetail(id, loai_benh_an);
        return ResponseEntity.ok(BaseResponse.success("Xóa chi tiết tổn thương thành công", null));
    }
}
