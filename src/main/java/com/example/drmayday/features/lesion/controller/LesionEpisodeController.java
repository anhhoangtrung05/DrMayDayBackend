package com.example.drmayday.features.lesion.controller;

import com.example.drmayday.common.dto.BaseResponse;
import com.example.drmayday.features.lesion.dto.CreateLesionEpisodeRequest;
import com.example.drmayday.features.lesion.dto.LesionEpisodeResponse;
import com.example.drmayday.features.lesion.dto.UpdateLesionEpisodeRequest;
import com.example.drmayday.features.lesion.service.LesionEpisodeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/lesion-episodes")
public class LesionEpisodeController {

    private final LesionEpisodeService lesionEpisodeService;

    public LesionEpisodeController(LesionEpisodeService lesionEpisodeService) {
        this.lesionEpisodeService = lesionEpisodeService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PATIENT')")
    public ResponseEntity<BaseResponse<LesionEpisodeResponse>> createLesionEpisode(
            @Valid @RequestBody(required = false) CreateLesionEpisodeRequest req) {
        LesionEpisodeResponse response = lesionEpisodeService.createLesionEpisode(
                req != null ? req : new CreateLesionEpisodeRequest());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.created(response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PATIENT','DOCTOR','ADMIN')")
    public ResponseEntity<BaseResponse<List<LesionEpisodeResponse>>> getMyLesionEpisodes(
            @RequestParam(value = "ho_so_man_id", required = false) String hoSoManId) {
        List<LesionEpisodeResponse> response = lesionEpisodeService.getMyLesionEpisodes(hoSoManId);
        return ResponseEntity.ok(BaseResponse.ok(response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PATIENT','DOCTOR','ADMIN')")
    public ResponseEntity<BaseResponse<LesionEpisodeResponse>> getLesionEpisode(
            @PathVariable("id") String id) {
        LesionEpisodeResponse response = lesionEpisodeService.getLesionEpisode(id);
        return ResponseEntity.ok(BaseResponse.ok(response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('PATIENT')")
    public ResponseEntity<BaseResponse<LesionEpisodeResponse>> editLesionEpisode(
            @PathVariable("id") String id,
            @Valid @RequestBody(required = false) UpdateLesionEpisodeRequest req) {
        LesionEpisodeResponse response = lesionEpisodeService.editLesionEpisode(
                id, req != null ? req : new UpdateLesionEpisodeRequest());
        return ResponseEntity.ok(BaseResponse.ok(response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('PATIENT')")
    public ResponseEntity<BaseResponse<Void>> deleteLesionEpisode(
            @PathVariable("id") String id) {
        lesionEpisodeService.deleteLesionEpisode(id);
        return ResponseEntity.ok(BaseResponse.success("Xóa đợt tổn thương thành công", null));
    }
}
