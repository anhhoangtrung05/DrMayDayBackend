package com.example.drmayday.features.media.service;

import com.example.drmayday.common.exception.AppException;
import com.example.drmayday.common.exception.ErrorCode;
import com.example.drmayday.common.security.UserPrincipal;
import com.example.drmayday.entity.Attachment;
import com.example.drmayday.entity.MedicalRecord;
import com.example.drmayday.entity.Person;
import com.example.drmayday.entity.UserAccount;
import com.example.drmayday.features.media.dto.AttachmentResponse;
import com.example.drmayday.features.media.service.impl.AttachmentServiceImpl;
import com.example.drmayday.repository.AttachmentRepository;
import com.example.drmayday.repository.LesionPhotoRepository;
import com.example.drmayday.repository.MedicalRecordRepository;
import com.example.drmayday.repository.UserAccountRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttachmentServiceImplTest {

    @Mock private AttachmentRepository attachmentRepository;
    @Mock private MedicalRecordRepository medicalRecordRepository;
    @Mock private LesionPhotoRepository lesionPhotoRepository;
    @Mock private UserAccountRepository userAccountRepository;
    @Mock private StorageService storageService;

    @InjectMocks
    private AttachmentServiceImpl attachmentService;

    private static final Long PATIENT_ID = 2L;
    private static final Long OTHER_PATIENT_ID = 99L;
    private static final Long RECORD_ID = 10L;

    @BeforeEach
    void setUp() {
        setSecurityUser(PATIENT_ID, "0912345678", "benh_nhan");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setSecurityUser(Long userId, String phone, String role) {
        UserPrincipal principal = UserPrincipal.create(userId, phone, role);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private void mockUserAccount(Long accountId, Long personId) {
        Person person = Person.builder().personId(personId).build();
        UserAccount account = UserAccount.builder().accountId(accountId).person(person).build();
        when(userAccountRepository.findById(accountId)).thenReturn(Optional.of(account));
    }

    // ==========================================
    // API 84: createAttachment
    // ==========================================

    @Test
    @DisplayName("API 84: Benh nhan tai anh hop le (<5MB) len ho so cua minh thanh cong (1001)")
    void createAttachment_patientSuccessEvenWhenAccountIdDiffersFromPersonId() {
        setSecurityUser(50L, "0912345678", "benh_nhan");
        mockUserAccount(50L, PATIENT_ID);

        byte[] content2Mb = new byte[2 * 1024 * 1024];
        MockMultipartFile file = new MockMultipartFile("file", "skin_photo.jpg", "image/jpeg", content2Mb);
        MedicalRecord record = MedicalRecord.builder().medicalRecordId(RECORD_ID).patientId(PATIENT_ID).build();
        when(medicalRecordRepository.findById(RECORD_ID)).thenReturn(Optional.of(record));

        StorageService.StoredFile storedFile = new StorageService.StoredFile(
                "skin_photo.jpg", "attachments/uuid.jpg",
                "http://localhost:9000/drmayday/attachments/uuid.jpg",
                "jpg", "IMAGE", content2Mb.length
        );
        when(storageService.store(file)).thenReturn(storedFile);
        when(attachmentRepository.saveAndFlush(any(Attachment.class))).thenAnswer(inv -> {
            Attachment a = inv.getArgument(0);
            assertEquals(50L, a.getAccountId());
            a.setAttachmentId(101L);
            return a;
        });

        AttachmentResponse response = attachmentService.createAttachment(file, "10", "Tổn thương da", "Ảnh sẩn phù");

        assertNotNull(response);
        assertEquals("101", response.getAttachmentId());
        assertEquals("10", response.getRecordId());
        assertEquals("skin_photo.jpg", response.getOriginalFileName());
        assertEquals("IMAGE", response.getMediaType());
        assertEquals(content2Mb.length, response.getSizeBytes());
    }

    @Test
    @DisplayName("API 84: Bac si/Admin tai tep voi synthetic token khong co trong taikhoan -> accountId = null tranh loi FK")
    void createAttachment_doctorWithoutDbAccountSetsNullAccountId() {
        setSecurityUser(3L, "0912345679", "bac_si");
        when(userAccountRepository.findById(3L)).thenReturn(Optional.empty());

        MockMultipartFile file = new MockMultipartFile("file", "lab.pdf", "application/pdf", new byte[]{1, 2, 3});
        MedicalRecord record = MedicalRecord.builder().medicalRecordId(RECORD_ID).patientId(PATIENT_ID).build();
        when(medicalRecordRepository.findById(RECORD_ID)).thenReturn(Optional.of(record));

        StorageService.StoredFile storedFile = new StorageService.StoredFile(
                "lab.pdf", "attachments/lab.pdf",
                "http://localhost:9000/drmayday/attachments/lab.pdf",
                "pdf", "DOCUMENT", 3L
        );
        when(storageService.store(file)).thenReturn(storedFile);
        when(attachmentRepository.saveAndFlush(any(Attachment.class))).thenAnswer(inv -> {
            Attachment a = inv.getArgument(0);
            assertNull(a.getAccountId());
            a.setAttachmentId(102L);
            return a;
        });

        AttachmentResponse response = attachmentService.createAttachment(file, "10", "Ket qua xet nghiem", null);
        assertEquals("102", response.getAttachmentId());
        assertEquals("DOCUMENT", response.getMediaType());
    }

    @Test
    @DisplayName("API 84: Tep dung bang 5MB van duoc chap nhan (1001)")
    void createAttachment_exact5MbBoundaryAccepted() {
        mockUserAccount(PATIENT_ID, PATIENT_ID);
        byte[] exact5Mb = new byte[5 * 1024 * 1024];
        MockMultipartFile file = new MockMultipartFile("file", "boundary.png", "image/png", exact5Mb);
        MedicalRecord record = MedicalRecord.builder().medicalRecordId(RECORD_ID).patientId(PATIENT_ID).build();
        when(medicalRecordRepository.findById(RECORD_ID)).thenReturn(Optional.of(record));

        when(storageService.store(file)).thenReturn(new StorageService.StoredFile(
                "boundary.png", "attachments/b.png", "http://localhost:9000/drmayday/attachments/b.png",
                "png", "IMAGE", exact5Mb.length
        ));
        when(attachmentRepository.saveAndFlush(any(Attachment.class))).thenAnswer(inv -> {
            Attachment a = inv.getArgument(0);
            a.setAttachmentId(103L);
            return a;
        });

        AttachmentResponse res = attachmentService.createAttachment(file, "10", "  ", null);
        assertEquals("103", res.getAttachmentId());
        assertNull(res.getPurposeCategory());
    }

    @Test
    @DisplayName("API 84: Don dep file vat ly neu luu DB that bai")
    void createAttachment_cleansUpStoredFileWhenDbSaveFails() {
        mockUserAccount(PATIENT_ID, PATIENT_ID);
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", new byte[]{1, 2});
        MedicalRecord record = MedicalRecord.builder().medicalRecordId(RECORD_ID).patientId(PATIENT_ID).build();
        when(medicalRecordRepository.findById(RECORD_ID)).thenReturn(Optional.of(record));
        when(storageService.store(file)).thenReturn(new StorageService.StoredFile(
                "test.jpg", "attachments/orphan.jpg", "http://localhost:9000/drmayday/attachments/orphan.jpg",
                "jpg", "IMAGE", 2L
        ));
        when(attachmentRepository.saveAndFlush(any(Attachment.class)))
                .thenThrow(new DataIntegrityViolationException("simulated db error"));

        assertThrows(DataIntegrityViolationException.class, () ->
                attachmentService.createAttachment(file, "10", null, null));

        verify(storageService).delete("attachments/orphan.jpg");
    }

    @Test
    @DisplayName("API 84: That bai khi thieu file hoac file rong (6001)")
    void createAttachment_fileMissingOrEmpty() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "", "image/jpeg", new byte[0]);
        assertEquals(ErrorCode.IMAGE_FILE_REQUIRED, assertThrows(AppException.class, () ->
                attachmentService.createAttachment(emptyFile, "10", null, null)).getErrorCode());
        assertEquals(ErrorCode.IMAGE_FILE_REQUIRED, assertThrows(AppException.class, () ->
                attachmentService.createAttachment(null, "10", null, null)).getErrorCode());
    }

    @Test
    @DisplayName("API 84: That bai khi file vuot qua 5MB (6003)")
    void createAttachment_fileTooLarge() {
        byte[] largeBytes = new byte[5 * 1024 * 1024 + 1];
        MockMultipartFile largeFile = new MockMultipartFile("file", "large.png", "image/png", largeBytes);

        AppException ex = assertThrows(AppException.class, () ->
                attachmentService.createAttachment(largeFile, "10", null, null));
        assertEquals(ErrorCode.FILE_SIZE_EXCEEDS_LIMIT, ex.getErrorCode());
        verifyNoInteractions(storageService);
    }

    @Test
    @DisplayName("API 84: That bai khi thieu record_id (2001) hoac sai dinh dang / do dai (2003)")
    void createAttachment_invalidParams() {
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", new byte[]{1});

        assertEquals(ErrorCode.MISSING_REQUIRED_PARAMETER, assertThrows(AppException.class, () ->
                attachmentService.createAttachment(file, null, null, null)).getErrorCode());
        assertEquals(ErrorCode.MISSING_REQUIRED_PARAMETER, assertThrows(AppException.class, () ->
                attachmentService.createAttachment(file, "  ", null, null)).getErrorCode());
        assertEquals(ErrorCode.INVALID_PARAMETER_VALUE, assertThrows(AppException.class, () ->
                attachmentService.createAttachment(file, "abc", null, null)).getErrorCode());
        assertEquals(ErrorCode.INVALID_PARAMETER_VALUE, assertThrows(AppException.class, () ->
                attachmentService.createAttachment(file, "0", null, null)).getErrorCode());
        assertEquals(ErrorCode.INVALID_PARAMETER_VALUE, assertThrows(AppException.class, () ->
                attachmentService.createAttachment(file, "10", "x".repeat(101), null)).getErrorCode());
    }

    @Test
    @DisplayName("API 84: That bai khi ho so khong ton tai (4001) hoac cua benh nhan khac (3106)")
    void createAttachment_notFoundOrForbidden() {
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", new byte[]{1});

        when(medicalRecordRepository.findById(999L)).thenReturn(Optional.empty());
        assertEquals(ErrorCode.MEDICAL_RECORD_NOT_FOUND, assertThrows(AppException.class, () ->
                attachmentService.createAttachment(file, "999", null, null)).getErrorCode());

        mockUserAccount(PATIENT_ID, PATIENT_ID);
        MedicalRecord otherRecord = MedicalRecord.builder().medicalRecordId(RECORD_ID).patientId(OTHER_PATIENT_ID).build();
        when(medicalRecordRepository.findById(RECORD_ID)).thenReturn(Optional.of(otherRecord));
        assertEquals(ErrorCode.CANNOT_ACCESS_ANOTHER_USER_DATA, assertThrows(AppException.class, () ->
                attachmentService.createAttachment(file, "10", null, null)).getErrorCode());
    }

    // ==========================================
    // API 85: getAttachments
    // ==========================================

    @Test
    @DisplayName("API 85: Chu ho so va bac si/admin lay danh sach tep thanh cong (1000)")
    void getAttachments_successForOwnerAndDoctor() {
        mockUserAccount(PATIENT_ID, PATIENT_ID);
        MedicalRecord record = MedicalRecord.builder().medicalRecordId(RECORD_ID).patientId(PATIENT_ID).build();
        when(medicalRecordRepository.findById(RECORD_ID)).thenReturn(Optional.of(record));

        Attachment a1 = Attachment.builder().attachmentId(2L).medicalRecordId(RECORD_ID)
                .originalFileName("img2.jpg").createdAt(OffsetDateTime.now()).build();
        Attachment a2 = Attachment.builder().attachmentId(1L).medicalRecordId(RECORD_ID)
                .originalFileName("img1.jpg").createdAt(OffsetDateTime.now().minusMinutes(5)).build();
        when(attachmentRepository.findByMedicalRecordIdOrderByCreatedAtDescAttachmentIdDesc(RECORD_ID))
                .thenReturn(List.of(a1, a2));

        List<AttachmentResponse> list = attachmentService.getAttachments("10");
        assertEquals(2, list.size());
        assertEquals("img2.jpg", list.get(0).getOriginalFileName());

        setSecurityUser(1L, "0900000001", "admin");
        when(userAccountRepository.findById(1L)).thenReturn(Optional.empty());
        assertEquals(2, attachmentService.getAttachments("10").size());
    }

    @Test
    @DisplayName("API 85: Unhappy path - thieu record_id (2001), sai kieu (2003), 4001, 3106")
    void getAttachments_unhappyPaths() {
        assertEquals(ErrorCode.MISSING_REQUIRED_PARAMETER, assertThrows(AppException.class, () ->
                attachmentService.getAttachments("")).getErrorCode());
        assertEquals(ErrorCode.INVALID_PARAMETER_VALUE, assertThrows(AppException.class, () ->
                attachmentService.getAttachments("xyz")).getErrorCode());

        when(medicalRecordRepository.findById(999L)).thenReturn(Optional.empty());
        assertEquals(ErrorCode.MEDICAL_RECORD_NOT_FOUND, assertThrows(AppException.class, () ->
                attachmentService.getAttachments("999")).getErrorCode());

        mockUserAccount(PATIENT_ID, PATIENT_ID);
        MedicalRecord otherRecord = MedicalRecord.builder().medicalRecordId(RECORD_ID).patientId(OTHER_PATIENT_ID).build();
        when(medicalRecordRepository.findById(RECORD_ID)).thenReturn(Optional.of(otherRecord));
        assertEquals(ErrorCode.CANNOT_ACCESS_ANOTHER_USER_DATA, assertThrows(AppException.class, () ->
                attachmentService.getAttachments("10")).getErrorCode());
    }

    // ==========================================
    // API 86: deleteAttachment
    // ==========================================

    @Test
    @DisplayName("API 86: Xoa DB truoc khi xoa file vat ly (1000)")
    void deleteAttachment_deletesDbBeforePhysicalFile() {
        mockUserAccount(PATIENT_ID, PATIENT_ID);
        Attachment attachment = Attachment.builder().attachmentId(100L).medicalRecordId(RECORD_ID)
                .storagePath("attachments/test.jpg").build();
        when(attachmentRepository.findById(100L)).thenReturn(Optional.of(attachment));
        MedicalRecord record = MedicalRecord.builder().medicalRecordId(RECORD_ID).patientId(PATIENT_ID).build();
        when(medicalRecordRepository.findById(RECORD_ID)).thenReturn(Optional.of(record));
        when(lesionPhotoRepository.existsByAttachmentId(100L)).thenReturn(false);

        attachmentService.deleteAttachment("100");

        InOrder inOrder = inOrder(attachmentRepository, storageService);
        inOrder.verify(attachmentRepository).delete(attachment);
        inOrder.verify(attachmentRepository).flush();
        inOrder.verify(storageService).delete("attachments/test.jpg");
    }

    @Test
    @DisplayName("API 86: Khong tim thay tep (6005) hoac id khong hop le (2001/2003)")
    void deleteAttachment_notFoundAndInvalidId() {
        assertEquals(ErrorCode.MISSING_REQUIRED_PARAMETER, assertThrows(AppException.class, () ->
                attachmentService.deleteAttachment(" ")).getErrorCode());
        assertEquals(ErrorCode.INVALID_PARAMETER_VALUE, assertThrows(AppException.class, () ->
                attachmentService.deleteAttachment("abc")).getErrorCode());

        when(attachmentRepository.findById(999L)).thenReturn(Optional.empty());
        assertEquals(ErrorCode.ATTACHMENT_NOT_FOUND, assertThrows(AppException.class, () ->
                attachmentService.deleteAttachment("999")).getErrorCode());
    }

    @Test
    @DisplayName("API 86: Benh nhan xoa tep cua nguoi khac (3106)")
    void deleteAttachment_otherPatient() {
        mockUserAccount(PATIENT_ID, PATIENT_ID);
        Attachment attachment = Attachment.builder().attachmentId(100L).medicalRecordId(RECORD_ID).build();
        when(attachmentRepository.findById(100L)).thenReturn(Optional.of(attachment));
        MedicalRecord record = MedicalRecord.builder().medicalRecordId(RECORD_ID).patientId(OTHER_PATIENT_ID).build();
        when(medicalRecordRepository.findById(RECORD_ID)).thenReturn(Optional.of(record));

        assertEquals(ErrorCode.CANNOT_ACCESS_ANOTHER_USER_DATA, assertThrows(AppException.class, () ->
                attachmentService.deleteAttachment("100")).getErrorCode());
        verifyNoInteractions(storageService);
    }

    @Test
    @DisplayName("API 86: Tep dang duoc su dung boi anhchuptonthuong (6007) hoac loi FK flush")
    void deleteAttachment_inUse() {
        mockUserAccount(PATIENT_ID, PATIENT_ID);
        Attachment attachment = Attachment.builder().attachmentId(100L).medicalRecordId(RECORD_ID)
                .storagePath("attachments/used.jpg").build();
        when(attachmentRepository.findById(100L)).thenReturn(Optional.of(attachment));
        MedicalRecord record = MedicalRecord.builder().medicalRecordId(RECORD_ID).patientId(PATIENT_ID).build();
        when(medicalRecordRepository.findById(RECORD_ID)).thenReturn(Optional.of(record));
        when(lesionPhotoRepository.existsByAttachmentId(100L)).thenReturn(true);

        assertEquals(ErrorCode.ATTACHMENT_IN_USE, assertThrows(AppException.class, () ->
                attachmentService.deleteAttachment("100")).getErrorCode());
        verify(attachmentRepository, never()).delete(any());
        verify(storageService, never()).delete(any());
    }

    @Test
    @DisplayName("API 86: Concurrent FK DataIntegrityViolationException khi flush tra ve 6007 va giu file vat ly")
    void deleteAttachment_concurrentFkViolationMapsTo6007() {
        mockUserAccount(PATIENT_ID, PATIENT_ID);
        Attachment attachment = Attachment.builder().attachmentId(100L).medicalRecordId(RECORD_ID)
                .storagePath("attachments/used.jpg").build();
        when(attachmentRepository.findById(100L)).thenReturn(Optional.of(attachment));
        MedicalRecord record = MedicalRecord.builder().medicalRecordId(RECORD_ID).patientId(PATIENT_ID).build();
        when(medicalRecordRepository.findById(RECORD_ID)).thenReturn(Optional.of(record));
        when(lesionPhotoRepository.existsByAttachmentId(100L)).thenReturn(false);
        doThrow(new DataIntegrityViolationException("fk_anhtonthuong_tepdinhkem")).when(attachmentRepository).flush();

        assertEquals(ErrorCode.ATTACHMENT_IN_USE, assertThrows(AppException.class, () ->
                attachmentService.deleteAttachment("100")).getErrorCode());
        verify(storageService, never()).delete(any());
    }
}
