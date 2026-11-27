package com.example.drmayday.features.prescription.service;

import com.example.drmayday.common.exception.AppException;
import com.example.drmayday.common.exception.ErrorCode;
import com.example.drmayday.common.security.UserPrincipal;
import com.example.drmayday.entity.MedicalRecord;
import com.example.drmayday.entity.MedicalVisit;
import com.example.drmayday.entity.Medication;
import com.example.drmayday.entity.MedicationDiscontinuation;
import com.example.drmayday.entity.Prescription;
import com.example.drmayday.entity.PrescriptionItem;
import com.example.drmayday.features.prescription.dto.CreatePrescriptionRequest;
import com.example.drmayday.features.prescription.dto.PrescriptionItemRequest;
import com.example.drmayday.features.prescription.dto.PrescriptionResponse;
import com.example.drmayday.features.prescription.dto.StopInstructionRequest;
import com.example.drmayday.features.prescription.dto.StopMedicationResponse;
import com.example.drmayday.features.prescription.dto.UpdatePrescriptionRequest;
import com.example.drmayday.features.prescription.dto.UpsertStopMedicationRequest;
import com.example.drmayday.features.prescription.service.impl.PrescriptionServiceImpl;
import com.example.drmayday.repository.MedicalRecordRepository;
import com.example.drmayday.repository.MedicalVisitRepository;
import com.example.drmayday.repository.MedicationDiscontinuationRepository;
import com.example.drmayday.repository.MedicationRepository;
import com.example.drmayday.repository.PrescriptionItemRepository;
import com.example.drmayday.repository.PrescriptionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrescriptionServiceImplTest {

    private static final long RECORD_ID = 1L;
    private static final long OTHER_RECORD_ID = 2L;
    private static final long VISIT_ID = 10L;
    private static final long PRESCRIPTION_ID = 100L;
    private static final long PATIENT_ID = 5L;

    @Mock private PrescriptionRepository prescriptionRepository;
    @Mock private PrescriptionItemRepository prescriptionItemRepository;
    @Mock private MedicationRepository medicationRepository;
    @Mock private MedicationDiscontinuationRepository discontinuationRepository;
    @Mock private MedicalRecordRepository medicalRecordRepository;
    @Mock private MedicalVisitRepository medicalVisitRepository;

    @InjectMocks private PrescriptionServiceImpl service;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ── Fixtures ──

    private static MedicalRecord record(long id, long patientId) {
        return MedicalRecord.builder().medicalRecordId(id).patientId(patientId).medicalRecordType("CAP").build();
    }

    private static MedicalVisit visit(long id, long recordId) {
        return MedicalVisit.builder().medicalVisitId(id).medicalRecordId(recordId).medicalRecordType("CAP").build();
    }

    private static Prescription prescription() {
        return Prescription.builder()
                .prescriptionId(PRESCRIPTION_ID)
                .medicalRecordId(RECORD_ID)
                .medicalVisitId(VISIT_ID)
                .prescriptionDate(LocalDate.of(2026, 11, 20))
                .prescriptionStatus("DANG_DIEU_TRI")
                .stopMedicationAtNextVisit(false)
                .build();
    }

    private static PrescriptionItemRequest catalogItem(String medicationId) {
        return PrescriptionItemRequest.builder().maThuoc(medicationId).lieuDung("1 viên/ngày").thoiGianDung("7 ngày").build();
    }

    private static PrescriptionItemRequest freeTextItem(String name) {
        return PrescriptionItemRequest.builder().tenThuocBoSung(name).lieuDung("2 viên/ngày").thoiGianDung("5 ngày").build();
    }

    private static CreatePrescriptionRequest createRequest(List<PrescriptionItemRequest> items) {
        return CreatePrescriptionRequest.builder().recordId(RECORD_ID).phienKhamId(VISIT_ID).items(items).build();
    }

    private static StopInstructionRequest stopRow(String medication, String purpose, String otherDescription) {
        return StopInstructionRequest.builder()
                .thuocCanNgung(medication).thoiGianNgungNgay(3).mucDich(purpose).mucDichKhacGhiRo(otherDescription).build();
    }

    private void givenRecordAndVisit() {
        when(medicalRecordRepository.findById(RECORD_ID)).thenReturn(Optional.of(record(RECORD_ID, PATIENT_ID)));
        when(medicalVisitRepository.findById(VISIT_ID)).thenReturn(Optional.of(visit(VISIT_ID, RECORD_ID)));
    }

    private void givenCatalog(String... medicationIds) {
        List<Medication> found = new ArrayList<>();
        for (String id : medicationIds) {
            found.add(Medication.builder().medicationId(id).medicationName(id).build());
        }
        when(medicationRepository.findAllById(anyIterable())).thenReturn(found);
    }

    /** Assigns ids to saved rows like the IDENTITY column would. */
    private void givenSavesAssignIds() {
        when(prescriptionRepository.save(any(Prescription.class))).thenAnswer(inv -> {
            Prescription p = inv.getArgument(0);
            if (p.getPrescriptionId() == null) {
                p.setPrescriptionId(PRESCRIPTION_ID);
            }
            return p;
        });
        givenItemSavesAssignIds();
    }

    private void givenItemSavesAssignIds() {
        AtomicLong nextId = new AtomicLong(1000);
        when(prescriptionItemRepository.saveAll(anyList())).thenAnswer(inv -> {
            List<PrescriptionItem> items = inv.getArgument(0);
            items.forEach(i -> i.setPrescriptionItemId(nextId.getAndIncrement()));
            return items;
        });
    }

    private void loginAsPatient(long patientId) {
        UserPrincipal principal = UserPrincipal.create(patientId, "0912345678", "benh_nhan");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private static void assertError(ErrorCode expected, Executable call) {
        AppException ex = assertThrows(AppException.class, call);
        assertEquals(expected, ex.getErrorCode());
    }

    /** Also checks the specific message, which tells the frontend which field is wrong. */
    private static void assertError(ErrorCode expected, String message, Executable call) {
        AppException ex = assertThrows(AppException.class, call);
        assertEquals(expected, ex.getErrorCode());
        assertEquals(message, ex.getMessage());
    }

    // ── PRESCRIPTIONS_CREATE_PRESCRIPTIONS ──

    @Nested
    @DisplayName("createPrescription")
    class CreatePrescription {

        @Test
        @DisplayName("[Happy Path] Kê 2 thuốc (1 ma_thuoc, 1 ten_thuoc_bo_sung) hợp lệ, mặc định DANG_DIEU_TRI và hôm nay")
        void createsPrescriptionWithDefaults() {
            givenRecordAndVisit();
            givenCatalog("T01");
            givenSavesAssignIds();

            PrescriptionResponse res = service.createPrescription(
                    createRequest(List.of(catalogItem("T01"), freeTextItem("Thuốc nam"))));

            assertEquals(String.valueOf(PRESCRIPTION_ID), res.getPrescriptionId());
            assertEquals(String.valueOf(RECORD_ID), res.getRecordId());
            assertEquals(String.valueOf(VISIT_ID), res.getPhienKhamId());
            assertEquals("DANG_DIEU_TRI", res.getTrangThaiDon());
            assertEquals(LocalDate.now(), res.getNgayKeDon());
            assertFalse(res.getNgungThuocLanSau());
            assertEquals(2, res.getItems().size());
            assertEquals("T01", res.getItems().get(0).getMaThuoc());
            assertEquals("Thuốc nam", res.getItems().get(1).getTenThuocBoSung());
            assertEquals("1000", res.getItems().get(0).getChiTietId());
        }

        @Test
        @DisplayName("[Alternate Path] Giữ nguyên ngay_ke_don, trang_thai_don và ghi_chu_dieu_tri được gửi lên")
        void keepsExplicitHeaderFields() {
            givenRecordAndVisit();
            givenSavesAssignIds();
            CreatePrescriptionRequest req = createRequest(List.of(freeTextItem("Thuốc nam")));
            req.setNgayKeDon(LocalDate.of(2026, 1, 15));
            req.setTrangThaiDon("HOAN_THANH");
            req.setGhiChuDieuTri("Uống sau ăn");

            PrescriptionResponse res = service.createPrescription(req);

            assertEquals(LocalDate.of(2026, 1, 15), res.getNgayKeDon());
            assertEquals("HOAN_THANH", res.getTrangThaiDon());
            assertEquals("Uống sau ăn", res.getGhiChuDieuTri());
            verify(medicationRepository, never()).findAllById(anyIterable());
        }

        @Test
        @DisplayName("[Unhappy Path] Thiếu record_id trả về 2001, nêu tên field và không lưu gì")
        void missingRecordId() {
            CreatePrescriptionRequest req = createRequest(List.of(freeTextItem("A")));
            req.setRecordId(null);

            assertError(ErrorCode.MISSING_REQUIRED_PARAMETER, "Thiếu tham số bắt buộc: record_id",
                    () -> service.createPrescription(req));
            verify(medicalRecordRepository, never()).findById(any());
            verify(prescriptionRepository, never()).save(any());
        }

        @Test
        @DisplayName("[Unhappy Path] Thiếu phien_kham_id trả về 2001, nêu tên field và không lưu gì")
        void missingVisitId() {
            CreatePrescriptionRequest req = createRequest(List.of(freeTextItem("A")));
            req.setPhienKhamId(null);

            assertError(ErrorCode.MISSING_REQUIRED_PARAMETER, "Thiếu tham số bắt buộc: phien_kham_id",
                    () -> service.createPrescription(req));
            verify(prescriptionRepository, never()).save(any());
        }

        @Test
        @DisplayName("[Unhappy Path] Body rỗng {} trả về 2001 và báo field bắt buộc đầu tiên (record_id)")
        void emptyBody() {
            assertError(ErrorCode.MISSING_REQUIRED_PARAMETER, "Thiếu tham số bắt buộc: record_id",
                    () -> service.createPrescription(new CreatePrescriptionRequest()));
        }

        @Test
        @DisplayName("[Unhappy Path] Không có request trả về 2001")
        void nullRequest() {
            assertError(ErrorCode.MISSING_REQUIRED_PARAMETER, "Thiếu nội dung đơn thuốc",
                    () -> service.createPrescription(null));
        }

        @Test
        @DisplayName("[Exception Path] Hồ sơ bệnh án không tồn tại trả về 4001")
        void recordNotFound() {
            when(medicalRecordRepository.findById(RECORD_ID)).thenReturn(Optional.empty());

            assertError(ErrorCode.MEDICAL_RECORD_NOT_FOUND,
                    () -> service.createPrescription(createRequest(List.of(freeTextItem("A")))));
        }

        @Test
        @DisplayName("[Exception Path] phien_kham_id không thuộc record_id trả về 5008")
        void visitBelongsToAnotherRecord() {
            when(medicalRecordRepository.findById(RECORD_ID)).thenReturn(Optional.of(record(RECORD_ID, PATIENT_ID)));
            when(medicalVisitRepository.findById(VISIT_ID)).thenReturn(Optional.of(visit(VISIT_ID, OTHER_RECORD_ID)));

            assertError(ErrorCode.EXAM_NOT_FOUND,
                    () -> service.createPrescription(createRequest(List.of(freeTextItem("A")))));
            verify(prescriptionRepository, never()).save(any());
        }

        @Test
        @DisplayName("[Exception Path] Phiên khám không tồn tại trả về 5008")
        void visitNotFound() {
            when(medicalRecordRepository.findById(RECORD_ID)).thenReturn(Optional.of(record(RECORD_ID, PATIENT_ID)));
            when(medicalVisitRepository.findById(VISIT_ID)).thenReturn(Optional.empty());

            assertError(ErrorCode.EXAM_NOT_FOUND,
                    () -> service.createPrescription(createRequest(List.of(freeTextItem("A")))));
        }

        @Test
        @DisplayName("[Unhappy Path] items rỗng trả về 8001")
        void emptyItems() {
            givenRecordAndVisit();

            assertError(ErrorCode.PRESCRIPTION_ITEMS_EMPTY, () -> service.createPrescription(createRequest(List.of())));
            verify(prescriptionRepository, never()).save(any());
        }

        @Test
        @DisplayName("[Unhappy Path] Không gửi items trả về 8001")
        void nullItems() {
            givenRecordAndVisit();

            assertError(ErrorCode.PRESCRIPTION_ITEMS_EMPTY, () -> service.createPrescription(createRequest(null)));
        }

        @Test
        @DisplayName("[Unhappy Path] Item có cả ma_thuoc lẫn ten_thuoc_bo_sung trả về 8004")
        void itemHasBothMedicationFields() {
            givenRecordAndVisit();
            PrescriptionItemRequest item = catalogItem("T01");
            item.setTenThuocBoSung("Thuốc nam");

            assertError(ErrorCode.PRESCRIPTION_ITEM_MEDICATION_CONFLICT,
                    () -> service.createPrescription(createRequest(List.of(item))));
        }

        @Test
        @DisplayName("[Unhappy Path] Item không có ma_thuoc lẫn ten_thuoc_bo_sung trả về 8004")
        void itemHasNoMedicationField() {
            givenRecordAndVisit();
            PrescriptionItemRequest item = PrescriptionItemRequest.builder().lieuDung("1").thoiGianDung("1").build();

            assertError(ErrorCode.PRESCRIPTION_ITEM_MEDICATION_CONFLICT,
                    () -> service.createPrescription(createRequest(List.of(item))));
        }

        @Test
        @DisplayName("[Unhappy Path] ma_thuoc không có trong danh mục trả về 8005")
        void medicationNotInCatalog() {
            givenRecordAndVisit();
            givenCatalog("T01");

            AppException ex = assertThrows(AppException.class, () -> service.createPrescription(
                    createRequest(List.of(catalogItem("T01"), catalogItem("KHONG_CO")))));

            assertEquals(ErrorCode.MEDICATION_NOT_FOUND, ex.getErrorCode());
            assertTrue(ex.getMessage().contains("KHONG_CO"));
            verify(prescriptionRepository, never()).save(any());
        }

        @Test
        @DisplayName("[Unhappy Path] trang_thai_don ngoài DANG_DIEU_TRI/HOAN_THANH/HUY trả về 2003")
        void invalidStatus() {
            givenRecordAndVisit();
            CreatePrescriptionRequest req = createRequest(List.of(freeTextItem("A")));
            req.setTrangThaiDon("active");

            assertError(ErrorCode.INVALID_PARAMETER_VALUE, () -> service.createPrescription(req));
        }

        @Test
        @DisplayName("[Unhappy Path] Item chỉ có khoảng trắng ở lieu_dung trả về 2001 và nêu thứ tự thuốc")
        void itemMissingDosage() {
            givenRecordAndVisit();
            PrescriptionItemRequest item = freeTextItem("A");
            item.setLieuDung(" ");

            assertError(ErrorCode.MISSING_REQUIRED_PARAMETER, "Thuốc thứ 1: lieu_dung là bắt buộc",
                    () -> service.createPrescription(createRequest(List.of(item))));
            verify(prescriptionRepository, never()).save(any());
        }

        @Test
        @DisplayName("[Unhappy Path] Thuốc thứ 2 thiếu thoi_gian_dung trả về 2001, nêu đúng thuốc thứ 2 và không lưu gì")
        void itemMissingDuration() {
            givenRecordAndVisit();
            PrescriptionItemRequest item = freeTextItem("B");
            item.setThoiGianDung(null);

            assertError(ErrorCode.MISSING_REQUIRED_PARAMETER, "Thuốc thứ 2: thoi_gian_dung là bắt buộc",
                    () -> service.createPrescription(createRequest(List.of(freeTextItem("A"), item))));
            verify(prescriptionRepository, never()).save(any());
            verify(prescriptionItemRepository, never()).saveAll(anyList());
        }

        @Test
        @DisplayName("[Unhappy Path] ten_thuoc_bo_sung chỉ có khoảng trắng trả về 2003")
        void blankAdditionalName() {
            givenRecordAndVisit();

            assertError(ErrorCode.INVALID_PARAMETER_VALUE,
                    () -> service.createPrescription(createRequest(List.of(freeTextItem("   ")))));
        }

        @Test
        @DisplayName("[Unhappy Path] lieu_dung dài quá 100 ký tự trả về 2003")
        void dosageTooLong() {
            givenRecordAndVisit();
            PrescriptionItemRequest item = freeTextItem("A");
            item.setLieuDung("x".repeat(101));

            assertError(ErrorCode.INVALID_PARAMETER_VALUE,
                    () -> service.createPrescription(createRequest(List.of(item))));
        }
    }

    // ── PRESCRIPTIONS_GET_PRESCRIPTIONS ──

    @Nested
    @DisplayName("getPrescriptions")
    class GetPrescriptions {

        @Test
        @DisplayName("[Happy Path] Lọc theo phien_kham_id trả về các đơn của phiên kèm chi tiết")
        void filtersByVisit() {
            Prescription first = prescription();
            Prescription second = prescription();
            second.setPrescriptionId(101L);
            when(medicalVisitRepository.findById(VISIT_ID)).thenReturn(Optional.of(visit(VISIT_ID, RECORD_ID)));
            when(prescriptionRepository.findFiltered(VISIT_ID, null)).thenReturn(List.of(first, second));
            when(prescriptionItemRepository.findByPrescriptionIdInOrderByPrescriptionItemIdAsc(anyCollection()))
                    .thenReturn(List.of(
                            PrescriptionItem.builder().prescriptionItemId(1L).prescriptionId(PRESCRIPTION_ID).medicationId("T01").build(),
                            PrescriptionItem.builder().prescriptionItemId(2L).prescriptionId(PRESCRIPTION_ID).additionalMedicationName("B").build()));

            List<PrescriptionResponse> res = service.getPrescriptions(VISIT_ID, null);

            assertEquals(2, res.size());
            assertEquals(2, res.get(0).getItems().size());
            assertTrue(res.get(1).getItems().isEmpty());
        }

        @Test
        @DisplayName("[Happy Path] Lọc theo record_id trả về tất cả đơn của hồ sơ")
        void filtersByRecord() {
            when(prescriptionRepository.findFiltered(null, RECORD_ID)).thenReturn(List.of(prescription()));
            when(prescriptionItemRepository.findByPrescriptionIdInOrderByPrescriptionItemIdAsc(anyCollection()))
                    .thenReturn(List.of());

            List<PrescriptionResponse> res = service.getPrescriptions(null, RECORD_ID);

            assertEquals(1, res.size());
            assertEquals(String.valueOf(RECORD_ID), res.get(0).getRecordId());
        }

        @Test
        @DisplayName("[Alternate Path] Không có đơn nào trả về mảng rỗng và không truy vấn chi tiết")
        void noPrescriptions() {
            when(prescriptionRepository.findFiltered(null, RECORD_ID)).thenReturn(List.of());

            assertTrue(service.getPrescriptions(null, RECORD_ID).isEmpty());
            verify(prescriptionItemRepository, never()).findByPrescriptionIdInOrderByPrescriptionItemIdAsc(anyCollection());
        }

        @Test
        @DisplayName("[Unhappy Path] Không truyền phien_kham_id lẫn record_id trả về 2001")
        void missingBothFilters() {
            assertError(ErrorCode.MISSING_REQUIRED_PARAMETER, () -> service.getPrescriptions(null, null));
        }

        @Test
        @DisplayName("[Alternate Path] Bệnh nhân xem đơn thuốc của hồ sơ của chính mình")
        void patientReadsOwnRecord() {
            loginAsPatient(PATIENT_ID);
            when(medicalRecordRepository.findById(RECORD_ID)).thenReturn(Optional.of(record(RECORD_ID, PATIENT_ID)));
            when(prescriptionRepository.findFiltered(null, RECORD_ID)).thenReturn(List.of());

            assertTrue(service.getPrescriptions(null, RECORD_ID).isEmpty());
        }

        @Test
        @DisplayName("[Exception Path] Bệnh nhân xem đơn thuốc của hồ sơ người khác trả về 3106")
        void patientReadsAnotherPatientsRecord() {
            loginAsPatient(99L);
            when(medicalRecordRepository.findById(RECORD_ID)).thenReturn(Optional.of(record(RECORD_ID, PATIENT_ID)));

            assertError(ErrorCode.CANNOT_ACCESS_ANOTHER_USER_DATA, () -> service.getPrescriptions(null, RECORD_ID));
            verify(prescriptionRepository, never()).findFiltered(any(), any());
        }

        @Test
        @DisplayName("[Exception Path] Bệnh nhân lọc theo phiên khám của người khác trả về 3106")
        void patientReadsAnotherPatientsVisit() {
            loginAsPatient(99L);
            when(medicalVisitRepository.findById(VISIT_ID)).thenReturn(Optional.of(visit(VISIT_ID, RECORD_ID)));
            when(medicalRecordRepository.findById(RECORD_ID)).thenReturn(Optional.of(record(RECORD_ID, PATIENT_ID)));

            assertError(ErrorCode.CANNOT_ACCESS_ANOTHER_USER_DATA, () -> service.getPrescriptions(VISIT_ID, null));
        }
    }

    // ── PRESCRIPTIONS_UPDATE_PRESCRIPTION ──

    @Nested
    @DisplayName("updatePrescription")
    class UpdatePrescription {

        @Test
        @DisplayName("[Happy Path] Đổi trang_thai_don sang HOAN_THANH, giữ nguyên danh sách thuốc")
        void updatesStatusOnly() {
            Prescription existing = prescription();
            existing.setTreatmentNotes("Ghi chú cũ");
            when(prescriptionRepository.findById(PRESCRIPTION_ID)).thenReturn(Optional.of(existing));
            when(prescriptionRepository.save(any(Prescription.class))).thenAnswer(inv -> inv.getArgument(0));
            when(prescriptionItemRepository.findByPrescriptionIdOrderByPrescriptionItemIdAsc(PRESCRIPTION_ID))
                    .thenReturn(List.of(PrescriptionItem.builder().prescriptionItemId(1L).prescriptionId(PRESCRIPTION_ID).medicationId("T01").build()));

            PrescriptionResponse res = service.updatePrescription(PRESCRIPTION_ID,
                    UpdatePrescriptionRequest.builder().trangThaiDon("HOAN_THANH").build());

            assertEquals("HOAN_THANH", res.getTrangThaiDon());
            assertEquals("Ghi chú cũ", res.getGhiChuDieuTri());
            assertEquals(1, res.getItems().size());
            verify(prescriptionItemRepository, never()).deleteByPrescriptionId(any());
        }

        @Test
        @DisplayName("[Happy Path] Gửi 3 thuốc mới thì thay toàn bộ danh sách cũ")
        void replacesItems() {
            when(prescriptionRepository.findById(PRESCRIPTION_ID)).thenReturn(Optional.of(prescription()));
            when(prescriptionRepository.save(any(Prescription.class))).thenAnswer(inv -> inv.getArgument(0));
            givenCatalog("T01");
            givenItemSavesAssignIds();

            PrescriptionResponse res = service.updatePrescription(PRESCRIPTION_ID, UpdatePrescriptionRequest.builder()
                    .ghiChuDieuTri("Ghi chú mới")
                    .items(List.of(catalogItem("T01"), freeTextItem("B"), freeTextItem("C")))
                    .build());

            assertEquals(3, res.getItems().size());
            assertEquals("Ghi chú mới", res.getGhiChuDieuTri());
            verify(prescriptionItemRepository).deleteByPrescriptionId(PRESCRIPTION_ID);
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<PrescriptionItem>> saved = ArgumentCaptor.forClass(List.class);
            verify(prescriptionItemRepository).saveAll(saved.capture());
            assertTrue(saved.getValue().stream().allMatch(i -> i.getPrescriptionId() == PRESCRIPTION_ID));
        }

        @Test
        @DisplayName("[Exception Path] id đơn thuốc không tồn tại trả về 8003")
        void prescriptionNotFound() {
            when(prescriptionRepository.findById(PRESCRIPTION_ID)).thenReturn(Optional.empty());

            assertError(ErrorCode.PRESCRIPTION_NOT_FOUND, () -> service.updatePrescription(PRESCRIPTION_ID,
                    UpdatePrescriptionRequest.builder().trangThaiDon("HUY").build()));
        }

        @Test
        @DisplayName("[Unhappy Path] Item vi phạm XOR trả về 8004 và không xoá thuốc cũ")
        void itemViolatesXor() {
            when(prescriptionRepository.findById(PRESCRIPTION_ID)).thenReturn(Optional.of(prescription()));
            PrescriptionItemRequest item = catalogItem("T01");
            item.setTenThuocBoSung("B");

            assertError(ErrorCode.PRESCRIPTION_ITEM_MEDICATION_CONFLICT, () -> service.updatePrescription(
                    PRESCRIPTION_ID, UpdatePrescriptionRequest.builder().items(List.of(item)).build()));
            verify(prescriptionItemRepository, never()).deleteByPrescriptionId(any());
        }

        @Test
        @DisplayName("[Unhappy Path] Item mới thiếu lieu_dung trả về 2001 và không xoá thuốc cũ")
        void itemMissingDosage() {
            when(prescriptionRepository.findById(PRESCRIPTION_ID)).thenReturn(Optional.of(prescription()));
            PrescriptionItemRequest item = freeTextItem("A");
            item.setLieuDung(null);

            assertError(ErrorCode.MISSING_REQUIRED_PARAMETER, "Thuốc thứ 1: lieu_dung là bắt buộc",
                    () -> service.updatePrescription(
                            PRESCRIPTION_ID, UpdatePrescriptionRequest.builder().items(List.of(item)).build()));
            verify(prescriptionItemRepository, never()).deleteByPrescriptionId(any());
            verify(prescriptionRepository, never()).save(any());
        }

        @Test
        @DisplayName("[Unhappy Path] Gửi items rỗng trả về 8001")
        void emptyItems() {
            when(prescriptionRepository.findById(PRESCRIPTION_ID)).thenReturn(Optional.of(prescription()));

            assertError(ErrorCode.PRESCRIPTION_ITEMS_EMPTY, () -> service.updatePrescription(
                    PRESCRIPTION_ID, UpdatePrescriptionRequest.builder().items(List.of()).build()));
        }

        @Test
        @DisplayName("[Unhappy Path] trang_thai_don không hợp lệ trả về 2003")
        void invalidStatus() {
            when(prescriptionRepository.findById(PRESCRIPTION_ID)).thenReturn(Optional.of(prescription()));

            assertError(ErrorCode.INVALID_PARAMETER_VALUE, () -> service.updatePrescription(
                    PRESCRIPTION_ID, UpdatePrescriptionRequest.builder().trangThaiDon("Hoan thanh").build()));
        }

        @Test
        @DisplayName("[Unhappy Path] ma_thuoc mới không có trong danh mục trả về 8005")
        void medicationNotInCatalog() {
            when(prescriptionRepository.findById(PRESCRIPTION_ID)).thenReturn(Optional.of(prescription()));
            givenCatalog();

            assertError(ErrorCode.MEDICATION_NOT_FOUND, () -> service.updatePrescription(
                    PRESCRIPTION_ID, UpdatePrescriptionRequest.builder().items(List.of(catalogItem("X"))).build()));
        }
    }

    // ── PRESCRIPTIONS_UPSERT_STOP_MEDICATION ──

    @Nested
    @DisplayName("upsertStopMedication")
    class UpsertStopMedication {

        private UpsertStopMedicationRequest request(List<StopInstructionRequest> rows) {
            return UpsertStopMedicationRequest.builder().donThuocId(PRESCRIPTION_ID).instructions(rows).build();
        }

        private void givenPrescriptionAndSaves() {
            when(prescriptionRepository.findById(PRESCRIPTION_ID)).thenReturn(Optional.of(prescription()));
            when(prescriptionRepository.save(any(Prescription.class))).thenAnswer(inv -> inv.getArgument(0));
            AtomicLong nextId = new AtomicLong(1);
            when(discontinuationRepository.saveAll(anyList())).thenAnswer(inv -> {
                List<MedicationDiscontinuation> rows = inv.getArgument(0);
                rows.forEach(r -> r.setDiscontinuationId(nextId.getAndIncrement()));
                return rows;
            });
        }

        @Test
        @DisplayName("[Happy Path] Gửi 2 dòng Test kich thich thì ngung_thuoc_lan_sau = true")
        void savesInstructions() {
            givenPrescriptionAndSaves();

            StopMedicationResponse res = service.upsertStopMedication(request(List.of(
                    stopRow("Cetirizin", "Test kich thich", null),
                    stopRow("Loratadin", "Test kich thich", null))));

            assertTrue(res.getNgungThuocLanSau());
            assertEquals(2, res.getInstructions().size());
            assertEquals("1", res.getInstructions().get(0).getChiDanId());
            verify(discontinuationRepository).deleteByPrescriptionId(PRESCRIPTION_ID);
            ArgumentCaptor<Prescription> saved = ArgumentCaptor.forClass(Prescription.class);
            verify(prescriptionRepository).save(saved.capture());
            assertEquals("Cetirizin, Loratadin", saved.getValue().getMedicationsToStopBeforeFollowUp());
        }

        @Test
        @DisplayName("[Alternate Path] Gửi mảng rỗng thì xoá hết và ngung_thuoc_lan_sau = false")
        void clearsInstructions() {
            givenPrescriptionAndSaves();

            StopMedicationResponse res = service.upsertStopMedication(request(List.of()));

            assertFalse(res.getNgungThuocLanSau());
            assertTrue(res.getInstructions().isEmpty());
            verify(discontinuationRepository).deleteByPrescriptionId(PRESCRIPTION_ID);
            ArgumentCaptor<Prescription> saved = ArgumentCaptor.forClass(Prescription.class);
            verify(prescriptionRepository).save(saved.capture());
            assertNull(saved.getValue().getMedicationsToStopBeforeFollowUp());
        }

        @Test
        @DisplayName("[Alternate Path] muc_dich = Khac có muc_dich_khac_ghi_ro thì lưu bình thường")
        void acceptsOtherPurposeWithDescription() {
            givenPrescriptionAndSaves();

            StopMedicationResponse res = service.upsertStopMedication(
                    request(List.of(stopRow("Cetirizin", "Khac", "Nội soi"))));

            assertEquals("Nội soi", res.getInstructions().get(0).getMucDichKhacGhiRo());
        }

        @Test
        @DisplayName("[Unhappy Path] muc_dich = Khac không có muc_dich_khac_ghi_ro trả về 8201")
        void otherPurposeWithoutDescription() {
            when(prescriptionRepository.findById(PRESCRIPTION_ID)).thenReturn(Optional.of(prescription()));

            assertError(ErrorCode.STOP_PURPOSE_OTHER_DESCRIPTION_REQUIRED,
                    () -> service.upsertStopMedication(request(List.of(stopRow("Cetirizin", "Khac", null)))));
            verify(discontinuationRepository, never()).deleteByPrescriptionId(any());
        }

        @Test
        @DisplayName("[Unhappy Path] muc_dich ngoài 3 giá trị cho phép trả về 8202")
        void invalidPurpose() {
            when(prescriptionRepository.findById(PRESCRIPTION_ID)).thenReturn(Optional.of(prescription()));

            assertError(ErrorCode.STOP_PURPOSE_INVALID,
                    () -> service.upsertStopMedication(request(List.of(stopRow("Cetirizin", "Test kích thích", null)))));
        }

        @Test
        @DisplayName("[Unhappy Path] Có muc_dich_khac_ghi_ro khi muc_dich khác Khac trả về 2003")
        void descriptionWithoutOtherPurpose() {
            when(prescriptionRepository.findById(PRESCRIPTION_ID)).thenReturn(Optional.of(prescription()));

            assertError(ErrorCode.INVALID_PARAMETER_VALUE,
                    () -> service.upsertStopMedication(request(List.of(stopRow("Cetirizin", "Test kich thich", "X")))));
        }

        @Test
        @DisplayName("[Unhappy Path] thoi_gian_ngung_ngay âm trả về 2003")
        void negativeDays() {
            when(prescriptionRepository.findById(PRESCRIPTION_ID)).thenReturn(Optional.of(prescription()));
            StopInstructionRequest row = stopRow("Cetirizin", null, null);
            row.setThoiGianNgungNgay(-1);

            assertError(ErrorCode.INVALID_PARAMETER_VALUE, () -> service.upsertStopMedication(request(List.of(row))));
        }

        @Test
        @DisplayName("[Unhappy Path] Chỉ dẫn thứ 2 thiếu thuoc_can_ngung trả về 2001 và không xoá chỉ dẫn cũ")
        void missingMedicationToStop() {
            when(prescriptionRepository.findById(PRESCRIPTION_ID)).thenReturn(Optional.of(prescription()));

            assertError(ErrorCode.MISSING_REQUIRED_PARAMETER, "Chỉ dẫn thứ 2: thuoc_can_ngung là bắt buộc",
                    () -> service.upsertStopMedication(request(List.of(
                            stopRow("Cetirizin", null, null), stopRow(null, null, null)))));
            verify(discontinuationRepository, never()).deleteByPrescriptionId(any());
            verify(prescriptionRepository, never()).save(any());
        }

        @Test
        @DisplayName("[Unhappy Path] Thiếu don_thuoc_id trả về 2001 và nêu tên field")
        void missingPrescriptionId() {
            UpsertStopMedicationRequest req = request(List.of());
            req.setDonThuocId(null);

            assertError(ErrorCode.MISSING_REQUIRED_PARAMETER, "Thiếu tham số bắt buộc: don_thuoc_id",
                    () -> service.upsertStopMedication(req));
            verify(prescriptionRepository, never()).findById(any());
        }

        @Test
        @DisplayName("[Unhappy Path] Không gửi instructions trả về 2001 và gợi ý gửi mảng rỗng để xoá")
        void missingInstructions() {
            assertError(ErrorCode.MISSING_REQUIRED_PARAMETER,
                    "Thiếu tham số bắt buộc: instructions (gửi mảng rỗng để xoá toàn bộ chỉ dẫn)",
                    () -> service.upsertStopMedication(request(null)));
            verify(discontinuationRepository, never()).deleteByPrescriptionId(any());
        }

        @Test
        @DisplayName("[Exception Path] don_thuoc_id không tồn tại trả về 8003")
        void prescriptionNotFound() {
            when(prescriptionRepository.findById(PRESCRIPTION_ID)).thenReturn(Optional.empty());

            assertError(ErrorCode.PRESCRIPTION_NOT_FOUND, () -> service.upsertStopMedication(request(List.of())));
        }
    }

    // ── PRESCRIPTIONS_GET_STOP_MEDICATION ──

    @Nested
    @DisplayName("getStopMedication")
    class GetStopMedication {

        @Test
        @DisplayName("[Happy Path] Đơn có 2 chỉ dẫn trả về mảng 2 phần tử")
        void returnsInstructions() {
            Prescription p = prescription();
            p.setStopMedicationAtNextVisit(true);
            when(prescriptionRepository.findById(PRESCRIPTION_ID)).thenReturn(Optional.of(p));
            when(discontinuationRepository.findByPrescriptionIdOrderByDiscontinuationIdAsc(PRESCRIPTION_ID))
                    .thenReturn(List.of(
                            MedicationDiscontinuation.builder().discontinuationId(1L).medicationToStop("A").build(),
                            MedicationDiscontinuation.builder().discontinuationId(2L).medicationToStop("B").build()));

            StopMedicationResponse res = service.getStopMedication(PRESCRIPTION_ID);

            assertEquals(String.valueOf(PRESCRIPTION_ID), res.getDonThuocId());
            assertTrue(res.getNgungThuocLanSau());
            assertEquals(2, res.getInstructions().size());
        }

        @Test
        @DisplayName("[Alternate Path] Đơn không có chỉ dẫn trả về mảng rỗng")
        void returnsEmptyList() {
            when(prescriptionRepository.findById(PRESCRIPTION_ID)).thenReturn(Optional.of(prescription()));
            when(discontinuationRepository.findByPrescriptionIdOrderByDiscontinuationIdAsc(PRESCRIPTION_ID))
                    .thenReturn(List.of());

            StopMedicationResponse res = service.getStopMedication(PRESCRIPTION_ID);

            assertFalse(res.getNgungThuocLanSau());
            assertTrue(res.getInstructions().isEmpty());
        }

        @Test
        @DisplayName("[Unhappy Path] Thiếu don_thuoc_id trả về 2001")
        void missingPrescriptionId() {
            assertError(ErrorCode.MISSING_REQUIRED_PARAMETER, () -> service.getStopMedication(null));
        }

        @Test
        @DisplayName("[Exception Path] don_thuoc_id không tồn tại trả về 8003")
        void prescriptionNotFound() {
            when(prescriptionRepository.findById(PRESCRIPTION_ID)).thenReturn(Optional.empty());

            assertError(ErrorCode.PRESCRIPTION_NOT_FOUND, () -> service.getStopMedication(PRESCRIPTION_ID));
        }

        @Test
        @DisplayName("[Exception Path] Bệnh nhân xem chỉ dẫn của đơn thuốc người khác trả về 3106")
        void patientReadsAnotherPatientsPrescription() {
            loginAsPatient(99L);
            when(prescriptionRepository.findById(PRESCRIPTION_ID)).thenReturn(Optional.of(prescription()));
            when(medicalRecordRepository.findById(RECORD_ID)).thenReturn(Optional.of(record(RECORD_ID, PATIENT_ID)));

            assertError(ErrorCode.CANNOT_ACCESS_ANOTHER_USER_DATA, () -> service.getStopMedication(PRESCRIPTION_ID));
        }
    }
}
