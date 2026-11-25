package com.example.drmayday.features.media.service;

import com.example.drmayday.features.media.dto.AttachmentResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AttachmentService {

    AttachmentResponse createAttachment(MultipartFile file, String recordId, String phanLoaiMucDich, String moTaTep);

    List<AttachmentResponse> getAttachments(String recordId);

    void deleteAttachment(String attachmentId);
}
