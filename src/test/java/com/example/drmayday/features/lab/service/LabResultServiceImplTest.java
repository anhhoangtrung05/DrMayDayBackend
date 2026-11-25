package com.example.drmayday.features.lab.service;

import com.example.drmayday.common.exception.AppException;
import com.example.drmayday.common.exception.ErrorCode;
import com.example.drmayday.common.security.UserPrincipal;
import com.example.drmayday.entity.AcuteClinicalTestResult;
import com.example.drmayday.entity.ChronicClinicalTestResult;
import com.example.drmayday.entity.ClinicalTestResult;
import com.example.drmayday.entity.MedicalRecord;
import com.example.drmayday.entity.MedicalVisit;
import com.example.drmayday.features.lab.dto.LabResultResponse;
import com.example.drmayday.features.lab.dto.UpsertLabResultsRequest;
import com.example.drmayday.features.lab.service.impl.LabResultServiceImpl;
import com.example.drmayday.repository.AcuteClinicalTestResultRepository;
import com.example.drmayday.repository.ChronicClinicalTestResultRepository;
import com.example.drmayday.repository.ClinicalTestResultRepository;
import com.example.drmayday.repository.MedicalRecordRepository;
import com.example.drmayday.repository.MedicalVisitRepository;
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

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LabResultServiceImplTest {

    private static final long RECORD_ID = 1L;
    private static final long VISIT_ID = 10L;
    private static final long RESULT_ID = 500L;
    private static final long PATIENT_ID = 5L;

    @Mock private MedicalRecordRepository medicalRecordRepository;
    @Mock private MedicalVisitRepository medicalVisitRepository;
    @Mock private ClinicalTestResultRepository clinicalTestResultRepository;
    @Mock private AcuteClinicalTestResultRepository acuteRepository;
    @Mock private ChronicClinicalTestResultRepository chronicRepository;

    @InjectMocks private LabResultServiceImpl service;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ── Fixtures ──

    private void givenVisit(String type) {
        when(medicalRecordRepository.findById(RECORD_ID)).thenReturn(Optional.of(
                MedicalRecord.builder().medicalRecordId(RECORD_ID).patientId(PATIENT_ID).medicalRecordType(type).build()));
        when(medicalVisitRepository.findById(VISIT_ID)).thenReturn(Optional.of(visit(type)));
    }

    private static MedicalVisit visit(String type) {
        return MedicalVisit.builder().medicalVisitId(VISIT_ID).medicalRecordId(RECORD_ID).medicalRecordType(type).build();
    }

    private static UpsertLabResultsRequest.UpsertLabResultsRequestBuilder request() {
        return UpsertLabResultsRequest.builder().recordId(RECORD_ID).phienKhamId(VISIT_ID);
    }

    private static ClinicalTestResult parent(String type) {
        return ClinicalTestResult.builder()
                .clinicalTestResultId(RESULT_ID).medicalVisitId(VISIT_ID).clinicalTestResultType(type)
                .crp(new BigDecimal("3.5")).build();
    }

    private void givenNoExistingResult() {
        when(clinicalTestResultRepository.findFirstByMedicalVisitIdOrderByClinicalTestResultIdAsc(VISIT_ID))
                .thenReturn(Optional.empty());
        when(clinicalTestResultRepository.saveAndFlush(any(ClinicalTestResult.class))).thenAnswer(inv -> {
            ClinicalTestResult p = inv.getArgument(0);
            p.setClinicalTestResultId(RESULT_ID);
            return p;
        });
    }

    private void givenExistingResult(String type) {
        when(clinicalTestResultRepository.findFirstByMedicalVisitIdOrderByClinicalTestResultIdAsc(VISIT_ID))
                .thenReturn(Optional.of(parent(type)));
        when(clinicalTestResultRepository.saveAndFlush(any(ClinicalTestResult.class))).thenAnswer(inv -> inv.getArgument(0));
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

    // ── LAB_UPSERT_LAB_RESULTS ──

    @Nested
    @DisplayName("upsertLabResults")
    class UpsertLabResults {

        @Test
        @DisplayName("[Happy Path] Phiên CAP nhập WBC/NEU/LYM/PLT lần đầu thì tạo mới (created = true)")
        void createsAcuteResult() {
            givenVisit("CAP");
            givenNoExistingResult();
            when(acuteRepository.findById(RESULT_ID)).thenReturn(Optional.empty());
            when(acuteRepository.saveAndFlush(any(AcuteClinicalTestResult.class))).thenAnswer(inv -> {
                AcuteClinicalTestResult cap = inv.getArgument(0);
                cap.setNeutrophilToLymphocyteRatio(new BigDecimal("2.000000")); // computed by the DB
                return cap;
            });

            LabResultResponse res = service.upsertLabResults(request()
                    .wbc(new BigDecimal("8.1")).neu(new BigDecimal("4")).lym(new BigDecimal("2")).plt(new BigDecimal("250"))
                    .build());

            assertTrue(res.isCreated());
            assertEquals("CAP", res.getLoai());
            assertEquals(String.valueOf(RESULT_ID), res.getLabResultId());
            assertEquals(String.valueOf(RECORD_ID), res.getRecordId());
            assertEquals(new BigDecimal("4"), res.getNeu());
            assertEquals(new BigDecimal("2.000000"), res.getNlr());

            ArgumentCaptor<ClinicalTestResult> parent = ArgumentCaptor.forClass(ClinicalTestResult.class);
            verify(clinicalTestResultRepository).saveAndFlush(parent.capture());
            assertEquals("CAP", parent.getValue().getClinicalTestResultType());
            ArgumentCaptor<AcuteClinicalTestResult> cap = ArgumentCaptor.forClass(AcuteClinicalTestResult.class);
            verify(acuteRepository).saveAndFlush(cap.capture());
            assertEquals(RESULT_ID, cap.getValue().getClinicalTestResultId());
        }

        @Test
        @DisplayName("[Happy Path] Nhập lại (sửa CRP) thì cập nhật kết quả cũ (created = false)")
        void replacesExistingResult() {
            givenVisit("CAP");
            givenExistingResult("CAP");
            AcuteClinicalTestResult existingCap = AcuteClinicalTestResult.builder()
                    .clinicalTestResultId(RESULT_ID).lymphocytes(new BigDecimal("2")).build();
            when(acuteRepository.findById(RESULT_ID)).thenReturn(Optional.of(existingCap));
            when(acuteRepository.saveAndFlush(any(AcuteClinicalTestResult.class))).thenAnswer(inv -> inv.getArgument(0));

            LabResultResponse res = service.upsertLabResults(request().crp(new BigDecimal("12.5")).build());

            assertFalse(res.isCreated());
            assertEquals(new BigDecimal("12.5"), res.getCrp());
            // PUT replaces the result, so the subtype field that was not sent is cleared.
            assertNull(res.getLym());
        }

        @Test
        @DisplayName("[Alternate Path] Phiên MAN với test_huyet_thanh_tu_than = Chua lam lưu bình thường")
        void savesChronicResult() {
            givenVisit("MAN");
            givenNoExistingResult();
            when(chronicRepository.findById(RESULT_ID)).thenReturn(Optional.empty());
            when(chronicRepository.saveAndFlush(any(ChronicClinicalTestResult.class))).thenAnswer(inv -> inv.getArgument(0));

            LabResultResponse res = service.upsertLabResults(request()
                    .testHuyetThanhTuThan("Chua lam").anaHep2("Am tinh").tsh(new BigDecimal("1.2")).build());

            assertTrue(res.isCreated());
            assertEquals("MAN", res.getLoai());
            assertEquals("Chua lam", res.getTestHuyetThanhTuThan());
            assertEquals(new BigDecimal("1.2"), res.getTsh());
            verify(acuteRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("[Alternate Path] Phiên MAN có asst_duong_kinh_mm khi test_huyet_thanh_tu_than = Duong tinh")
        void acceptsAsstWithPositiveSerumTest() {
            givenVisit("MAN");
            givenNoExistingResult();
            when(chronicRepository.findById(RESULT_ID)).thenReturn(Optional.empty());
            when(chronicRepository.saveAndFlush(any(ChronicClinicalTestResult.class))).thenAnswer(inv -> inv.getArgument(0));

            LabResultResponse res = service.upsertLabResults(request()
                    .testHuyetThanhTuThan("Duong tinh").asstDuongKinhMm(new BigDecimal("5")).build());

            assertEquals(new BigDecimal("5"), res.getAsstDuongKinhMm());
        }

        @Test
        @DisplayName("[Unhappy Path] Phiên MAN gửi field lym/neu của CAP trả về 8103")
        void chronicVisitWithAcuteFields() {
            givenVisit("MAN");

            AppException ex = assertThrows(AppException.class, () -> service.upsertLabResults(
                    request().lym(new BigDecimal("2")).neu(new BigDecimal("4")).build()));

            assertEquals(ErrorCode.LAB_RESULT_FIELD_TYPE_MISMATCH, ex.getErrorCode());
            assertTrue(ex.getMessage().contains("lym, neu"));
            verify(clinicalTestResultRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("[Unhappy Path] Phiên CAP gửi field tsh của MAN trả về 8103")
        void acuteVisitWithChronicFields() {
            givenVisit("CAP");

            assertError(ErrorCode.LAB_RESULT_FIELD_TYPE_MISMATCH,
                    () -> service.upsertLabResults(request().tsh(new BigDecimal("1")).build()));
        }

        @Test
        @DisplayName("[Unhappy Path] asst_duong_kinh_mm khi test_huyet_thanh_tu_than khác Duong tinh trả về 8105")
        void asstWithoutPositiveSerumTest() {
            givenVisit("MAN");

            assertError(ErrorCode.ASST_REQUIRES_POSITIVE_SERUM_TEST, () -> service.upsertLabResults(request()
                    .testHuyetThanhTuThan("Am tinh").asstDuongKinhMm(new BigDecimal("5")).build()));
        }

        @Test
        @DisplayName("[Unhappy Path] Chỉ số âm trả về 2003")
        void negativeValue() {
            givenVisit("CAP");

            AppException ex = assertThrows(AppException.class,
                    () -> service.upsertLabResults(request().plt(new BigDecimal("-1")).build()));

            assertEquals(ErrorCode.INVALID_PARAMETER_VALUE, ex.getErrorCode());
            assertTrue(ex.getMessage().contains("plt"));
        }

        @Test
        @DisplayName("[Unhappy Path] Kết quả định tính ngoài Duong tinh/Am tinh/Chua lam trả về 2003")
        void invalidQualitativeResult() {
            givenVisit("MAN");

            assertError(ErrorCode.INVALID_PARAMETER_VALUE,
                    () -> service.upsertLabResults(request().batKetQua("Dương tính").build()));
        }

        @Test
        @DisplayName("[Unhappy Path] ana_hep2 = Chua lam (chỉ nhận Duong tinh/Am tinh) trả về 2003")
        void invalidAnaHep2() {
            givenVisit("MAN");

            assertError(ErrorCode.INVALID_PARAMETER_VALUE,
                    () -> service.upsertLabResults(request().anaHep2("Chua lam").build()));
        }

        @Test
        @DisplayName("[Unhappy Path] Thiếu record_id trả về 2001")
        void missingRecordId() {
            assertError(ErrorCode.MISSING_REQUIRED_PARAMETER,
                    () -> service.upsertLabResults(request().recordId(null).build()));
        }

        @Test
        @DisplayName("[Unhappy Path] Thiếu phien_kham_id trả về 2001")
        void missingVisitId() {
            assertError(ErrorCode.MISSING_REQUIRED_PARAMETER,
                    () -> service.upsertLabResults(request().phienKhamId(null).build()));
        }

        @Test
        @DisplayName("[Exception Path] Hồ sơ bệnh án không tồn tại trả về 4001")
        void recordNotFound() {
            when(medicalRecordRepository.findById(RECORD_ID)).thenReturn(Optional.empty());

            assertError(ErrorCode.MEDICAL_RECORD_NOT_FOUND, () -> service.upsertLabResults(request().build()));
        }

        @Test
        @DisplayName("[Exception Path] Phiên khám không thuộc hồ sơ trả về 5008")
        void visitNotInRecord() {
            when(medicalRecordRepository.findById(RECORD_ID)).thenReturn(Optional.of(
                    MedicalRecord.builder().medicalRecordId(RECORD_ID).patientId(PATIENT_ID).build()));
            MedicalVisit otherVisit = visit("CAP");
            otherVisit.setMedicalRecordId(2L);
            when(medicalVisitRepository.findById(VISIT_ID)).thenReturn(Optional.of(otherVisit));

            assertError(ErrorCode.EXAM_NOT_FOUND, () -> service.upsertLabResults(request().build()));
        }
    }

    // ── LAB_GET_LAB_RESULTS ──

    @Nested
    @DisplayName("getLabResults")
    class GetLabResults {

        @Test
        @DisplayName("[Happy Path] Phiên đã có kết quả CAP trả về field chung + CAP + nlr/plr")
        void returnsAcuteResult() {
            when(clinicalTestResultRepository.findFirstByMedicalVisitIdOrderByClinicalTestResultIdAsc(VISIT_ID))
                    .thenReturn(Optional.of(parent("CAP")));
            when(medicalVisitRepository.findById(VISIT_ID)).thenReturn(Optional.of(visit("CAP")));
            when(acuteRepository.findById(RESULT_ID)).thenReturn(Optional.of(AcuteClinicalTestResult.builder()
                    .clinicalTestResultId(RESULT_ID).neutrophils(new BigDecimal("4")).lymphocytes(new BigDecimal("2"))
                    .neutrophilToLymphocyteRatio(new BigDecimal("2")).plateletToLymphocyteRatio(new BigDecimal("125"))
                    .build()));

            LabResultResponse res = service.getLabResults(VISIT_ID);

            assertEquals("CAP", res.getLoai());
            assertEquals(new BigDecimal("3.5"), res.getCrp());
            assertEquals(new BigDecimal("2"), res.getNlr());
            assertEquals(new BigDecimal("125"), res.getPlr());
            assertEquals(String.valueOf(RECORD_ID), res.getRecordId());
        }

        @Test
        @DisplayName("[Happy Path] Phiên MAN có kết quả trả về field MAN")
        void returnsChronicResult() {
            when(clinicalTestResultRepository.findFirstByMedicalVisitIdOrderByClinicalTestResultIdAsc(VISIT_ID))
                    .thenReturn(Optional.of(parent("MAN")));
            when(medicalVisitRepository.findById(VISIT_ID)).thenReturn(Optional.of(visit("MAN")));
            when(chronicRepository.findById(RESULT_ID)).thenReturn(Optional.of(ChronicClinicalTestResult.builder()
                    .clinicalTestResultId(RESULT_ID).tsh(new BigDecimal("1.1")).anaHep2Result("Am tinh").build()));

            LabResultResponse res = service.getLabResults(VISIT_ID);

            assertEquals("MAN", res.getLoai());
            assertEquals(new BigDecimal("1.1"), res.getTsh());
            assertEquals("Am tinh", res.getAnaHep2());
            assertNull(res.getNlr());
        }

        @Test
        @DisplayName("[Exception Path] Phiên chưa nhập kết quả trả về 8104")
        void resultNotFound() {
            when(clinicalTestResultRepository.findFirstByMedicalVisitIdOrderByClinicalTestResultIdAsc(VISIT_ID))
                    .thenReturn(Optional.empty());

            assertError(ErrorCode.LAB_RESULT_NOT_FOUND, () -> service.getLabResults(VISIT_ID));
        }

        @Test
        @DisplayName("[Unhappy Path] Thiếu phien_kham_id trả về 2001")
        void missingVisitId() {
            assertError(ErrorCode.MISSING_REQUIRED_PARAMETER, () -> service.getLabResults(null));
        }

        @Test
        @DisplayName("[Exception Path] Bệnh nhân xem kết quả của hồ sơ người khác trả về 3106")
        void patientReadsAnotherPatientsResult() {
            loginAsPatient(99L);
            when(clinicalTestResultRepository.findFirstByMedicalVisitIdOrderByClinicalTestResultIdAsc(VISIT_ID))
                    .thenReturn(Optional.of(parent("CAP")));
            when(medicalVisitRepository.findById(VISIT_ID)).thenReturn(Optional.of(visit("CAP")));
            when(medicalRecordRepository.findById(RECORD_ID)).thenReturn(Optional.of(
                    MedicalRecord.builder().medicalRecordId(RECORD_ID).patientId(PATIENT_ID).build()));

            assertError(ErrorCode.CANNOT_ACCESS_ANOTHER_USER_DATA, () -> service.getLabResults(VISIT_ID));
        }
    }
}
