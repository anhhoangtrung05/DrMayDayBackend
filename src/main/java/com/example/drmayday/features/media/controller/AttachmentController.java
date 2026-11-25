package com.example.drmayday.features.media.controller;

import com.example.drmayday.common.dto.BaseResponse;
import com.example.drmayday.features.media.dto.AttachmentResponse;
import com.example.drmayday.features.media.service.AttachmentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/attachments")
public class AttachmentController {

    private final AttachmentService attachmentService;

    public AttachmentController(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('PATIENT','DOCTOR','ADMIN')")
    public ResponseEntity<BaseResponse<AttachmentResponse>> createAttachment(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "record_id", required = false) String recordId,
            @RequestParam(value = "phan_loai_muc_dich", required = false) String phanLoaiMucDich,
            @RequestParam(value = "mo_ta_tep", required = false) String moTaTep) {

        AttachmentResponse response = attachmentService.createAttachment(file, recordId, phanLoaiMucDich, moTaTep);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.created(response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PATIENT','DOCTOR','ADMIN')")
    public ResponseEntity<BaseResponse<List<AttachmentResponse>>> getAttachments(
            @RequestParam(value = "record_id", required = false) String recordId) {

        List<AttachmentResponse> response = attachmentService.getAttachments(recordId);
        return ResponseEntity.ok(BaseResponse.ok(response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('PATIENT','DOCTOR','ADMIN')")
    public ResponseEntity<BaseResponse<Void>> deleteAttachment(@PathVariable("id") String id) {
        attachmentService.deleteAttachment(id);
        return ResponseEntity.ok(BaseResponse.success("Xóa tệp đính kèm thành công", null));
    }
}
