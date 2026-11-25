package com.example.drmayday.features.media.service.impl;

import com.example.drmayday.common.exception.AppException;
import com.example.drmayday.common.exception.ErrorCode;
import com.example.drmayday.common.security.SecurityUtils;
import com.example.drmayday.entity.Attachment;
import com.example.drmayday.entity.MedicalRecord;
import com.example.drmayday.entity.UserAccount;
import com.example.drmayday.features.media.dto.AttachmentResponse;
import com.example.drmayday.features.media.service.AttachmentService;
import com.example.drmayday.features.media.service.StorageService;
import com.example.drmayday.repository.AttachmentRepository;
import com.example.drmayday.repository.LesionPhotoRepository;
import com.example.drmayday.repository.MedicalRecordRepository;
import com.example.drmayday.repository.UserAccountRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class AttachmentServiceImpl implements AttachmentService {

    static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024;
    private static final int MAX_PURPOSE_LENGTH = 100;

    private final AttachmentRepository attachmentRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final LesionPhotoRepository lesionPhotoRepository;
    private final UserAccountRepository userAccountRepository;
    private final StorageService storageService;

    public AttachmentServiceImpl(AttachmentRepository attachmentRepository,
                                 MedicalRecordRepository medicalRecordRepository,
                                 LesionPhotoRepository lesionPhotoRepository,
                                 UserAccountRepository userAccountRepository,
                                 StorageService storageService) {
        this.attachmentRepository = attachmentRepository;
        this.medicalRecordRepository = medicalRecordRepository;
        this.lesionPhotoRepository = lesionPhotoRepository;
        this.userAccountRepository = userAccountRepository;
        this.storageService = storageService;
    }

    @Override
    @Transactional
    public AttachmentResponse createAttachment(MultipartFile file,
                                               String recordIdStr,
                                               String phanLoaiMucDich,
                                               String moTaTep) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.IMAGE_FILE_REQUIRED, "Tệp hình ảnh là bắt buộc");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new AppException(ErrorCode.FILE_SIZE_EXCEEDS_LIMIT, "Dung lượng tệp vượt quá giới hạn 5MB");
        }

        Long recordId = parsePositiveId(recordIdStr, "record_id");
        String purpose = normalizeOptionalPurpose(phanLoaiMucDich);

        MedicalRecord record = medicalRecordRepository.findById(recordId)
                .orElseThrow(() -> new AppException(ErrorCode.MEDICAL_RECORD_NOT_FOUND,
                        "Không tìm thấy hồ sơ bệnh án: " + recordId));

        UserAccount callerAccount = resolveCallerAccount();
        checkRecordAccess(record, callerAccount);

        StorageService.StoredFile storedFile = storageService.store(file);
        OffsetDateTime now = OffsetDateTime.now();

        try {
            Attachment attachment = Attachment.builder()
                    .medicalRecordId(recordId)
                    // Nullable FK ON DELETE SET NULL: only link an existing taikhoan row
                    .accountId(callerAccount != null ? callerAccount.getAccountId() : null)
                    .originalFileName(storedFile.sanitizedOriginalName())
                    .storagePath(storedFile.storagePath())
                    .url(storedFile.url())
                    .fileFormat(storedFile.format())
                    .mediaType(storedFile.mediaType())
                    .sizeBytes(storedFile.size())
                    .purposeCategory(purpose)
                    .fileDescription(moTaTep)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            Attachment saved = attachmentRepository.saveAndFlush(attachment);
            return mapToResponse(saved);
        } catch (RuntimeException ex) {
            storageService.delete(storedFile.storagePath());
            throw ex;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttachmentResponse> getAttachments(String recordIdStr) {
        Long recordId = parsePositiveId(recordIdStr, "record_id");

        MedicalRecord record = medicalRecordRepository.findById(recordId)
                .orElseThrow(() -> new AppException(ErrorCode.MEDICAL_RECORD_NOT_FOUND,
                        "Không tìm thấy hồ sơ bệnh án: " + recordId));

        UserAccount callerAccount = resolveCallerAccount();
        checkRecordAccess(record, callerAccount);

        return attachmentRepository.findByMedicalRecordIdOrderByCreatedAtDescAttachmentIdDesc(recordId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public void deleteAttachment(String attachmentIdStr) {
        Long attachmentId = parsePositiveId(attachmentIdStr, "attachment_id");

        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new AppException(ErrorCode.ATTACHMENT_NOT_FOUND,
                        "Không tìm thấy tệp đính kèm: " + attachmentId));

        MedicalRecord record = medicalRecordRepository.findById(attachment.getMedicalRecordId())
                .orElseThrow(() -> new AppException(ErrorCode.MEDICAL_RECORD_NOT_FOUND,
                        "Không tìm thấy hồ sơ bệnh án gắn với tệp"));

        UserAccount callerAccount = resolveCallerAccount();
        checkRecordAccess(record, callerAccount);

        if (lesionPhotoRepository.existsByAttachmentId(attachmentId)) {
            throw new AppException(ErrorCode.ATTACHMENT_IN_USE,
                    "Tệp đính kèm đang được sử dụng bởi ảnh tổn thương");
        }

        String storagePath = attachment.getStoragePath();
        try {
            attachmentRepository.delete(attachment);
            attachmentRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new AppException(ErrorCode.ATTACHMENT_IN_USE,
                    "Tệp đính kèm đang được sử dụng bởi ảnh tổn thương");
        }

        storageService.delete(storagePath);
    }

    private Long parsePositiveId(String rawId, String paramName) {
        if (rawId == null || rawId.isBlank()) {
            throw new AppException(ErrorCode.MISSING_REQUIRED_PARAMETER,
                    "Thiếu tham số bắt buộc: " + paramName);
        }
        try {
            long id = Long.parseLong(rawId.trim());
            if (id <= 0) {
                throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE, paramName + " phải lớn hơn 0");
            }
            return id;
        } catch (NumberFormatException ex) {
            throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE, paramName + " không hợp lệ: " + rawId);
        }
    }

    private String normalizeOptionalPurpose(String rawPurpose) {
        if (rawPurpose == null) {
            return null;
        }
        String trimmed = rawPurpose.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > MAX_PURPOSE_LENGTH) {
            throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE,
                    "phan_loai_muc_dich không được vượt quá " + MAX_PURPOSE_LENGTH + " ký tự");
        }
        return trimmed;
    }

    private UserAccount resolveCallerAccount() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        return userAccountRepository.findById(currentUserId).orElse(null);
    }

    private void checkRecordAccess(MedicalRecord record, UserAccount callerAccount) {
        boolean isPrivileged = SecurityUtils.hasRole("DOCTOR") || SecurityUtils.hasRole("BAC_SI")
                || SecurityUtils.hasRole("ADMIN") || SecurityUtils.hasRole("QUAN_TRI");
        if (isPrivileged) {
            return;
        }

        Long currentUserId = SecurityUtils.getCurrentUserId();
        Long callerPatientId = (callerAccount != null && callerAccount.getPerson() != null)
                ? callerAccount.getPerson().getPersonId()
                : currentUserId;

        if (!record.getPatientId().equals(callerPatientId)) {
            throw new AppException(ErrorCode.CANNOT_ACCESS_ANOTHER_USER_DATA,
                    "Không thể truy cập dữ liệu của người dùng khác");
        }
    }

    private AttachmentResponse mapToResponse(Attachment entity) {
        return AttachmentResponse.builder()
                .attachmentId(String.valueOf(entity.getAttachmentId()))
                .recordId(String.valueOf(entity.getMedicalRecordId()))
                .originalFileName(entity.getOriginalFileName())
                .url(entity.getUrl())
                .fileFormat(entity.getFileFormat())
                .mediaType(entity.getMediaType())
                .sizeBytes(entity.getSizeBytes())
                .purposeCategory(entity.getPurposeCategory())
                .fileDescription(entity.getFileDescription())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
