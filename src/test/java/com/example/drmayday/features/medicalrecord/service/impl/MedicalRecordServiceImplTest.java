package com.example.drmayday.features.medicalrecord.service.impl;

import com.example.drmayday.common.exception.AppException;
import com.example.drmayday.common.exception.ErrorCode;
import com.example.drmayday.common.security.UserPrincipal;
import com.example.drmayday.entity.AcuteUrticariaRecord;
import com.example.drmayday.entity.ChronicUrticariaRecord;
import com.example.drmayday.entity.MedicalRecord;
import com.example.drmayday.entity.Patient;
import com.example.drmayday.entity.Person;
import com.example.drmayday.entity.UserAccount;
import com.example.drmayday.features.medicalrecord.dto.ChronicMedicalRecordDetailResponse;
import com.example.drmayday.features.medicalrecord.dto.CreateMedicalRecordRequest;
import com.example.drmayday.features.medicalrecord.dto.MedicalRecordResponse;
import com.example.drmayday.features.medicalrecord.dto.UpdateMedicalRecordRequest;
import com.example.drmayday.repository.AcuteUrticariaRecordRepository;
import com.example.drmayday.repository.AllergyOnsetContextRepository;
import com.example.drmayday.repository.ChronicUrticariaRecordRepository;
import com.example.drmayday.repository.InfectionStatusRepository;
import com.example.drmayday.repository.MedicalRecordRepository;
import com.example.drmayday.repository.MedicalVisitRepository;
import com.example.drmayday.repository.PatientRepository;
import com.example.drmayday.repository.PersonalMedicalHistoryRepository;
import com.example.drmayday.repository.PreviousDiseaseEpisodeRepository;
import com.example.drmayday.repository.UserAccountRepository;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Khóa luật nghiệp vụ của API 34 đến 39 mà dữ liệu thật khó tạo: thứ tự kiểm tra, luật chuyển trạng thái,
 * kiểm tra trên giá trị cuối, và cách đổi lỗi database. Chỉ giả lập repository, là ranh giới với database.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MedicalRecordServiceImplTest {

    private static final long PATIENT_ID = 2L;
    private static final long RECORD_ID = 10L;
    private static final String RECORD = "10";
    private static final long ACCOUNT_ID = 20L;

    @Mock private MedicalRecordRepository medicalRecordRepository;
    @Mock private AcuteUrticariaRecordRepository acuteRecordRepository;
    @Mock private ChronicUrticariaRecordRepository chronicRecordRepository;
    @Mock private PersonalMedicalHistoryRepository selfHistoryRepository;
    @Mock private AllergyOnsetContextRepository allergyContextRepository;
    @Mock private PreviousDiseaseEpisodeRepository previousEpisodeRepository;
    @Mock private InfectionStatusRepository infectionStatusRepository;
    @Mock private MedicalVisitRepository medicalVisitRepository;
    @Mock private PatientRepository patientRepository;
    @Mock private UserAccountRepository userAccountRepository;

    private MedicalRecordServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MedicalRecordServiceImpl(medicalRecordRepository, acuteRecordRepository, chronicRecordRepository,
                selfHistoryRepository, allergyContextRepository, previousEpisodeRepository, infectionStatusRepository,
                medicalVisitRepository, patientRepository, new RecordOwnerCheck(userAccountRepository));
        signInAs("bac_si", 6L);

        when(patientRepository.findByIdForUpdate(PATIENT_ID)).thenReturn(Optional.of(Patient.builder().patientId(PATIENT_ID).build()));
        when(patientRepository.existsById(PATIENT_ID)).thenReturn(true);
        when(medicalRecordRepository.existsByPatientIdAndMedicalRecordTypeAndStatusIn(anyLong(), anyString(), any()))
                .thenReturn(false);
        when(medicalRecordRepository.saveAndFlush(any(MedicalRecord.class))).thenAnswer(call -> {
            MedicalRecord record = call.getArgument(0);
            if (record.getMedicalRecordId() == null) {
                record.setMedicalRecordId(RECORD_ID);
            }
            return record;
        });
    }

    @AfterEach
    void signOut() {
        SecurityContextHolder.clearContext();
    }

    private static void signInAs(String role, long accountId) {
        UserPrincipal principal = UserPrincipal.create(accountId, "0900000000", "Người dùng", role);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private static CreateMedicalRecordRequest createRequest(String type) {
        CreateMedicalRecordRequest req = new CreateMedicalRecordRequest();
        req.setMedicalRecordType(type);
        return req;
    }

    private MedicalRecord givenStoredRecord(String type, String status) {
        MedicalRecord record = MedicalRecord.builder()
                .medicalRecordId(RECORD_ID).patientId(PATIENT_ID).medicalRecordType(type).status(status).build();
        when(medicalRecordRepository.findById(RECORD_ID)).thenReturn(Optional.of(record));
        when(medicalRecordRepository.findByIdForUpdate(RECORD_ID)).thenReturn(Optional.of(record));
        return record;
    }

    private static void assertCode(ThrowingCallable call, ErrorCode code) {
        assertThatThrownBy(call).isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode()).isEqualTo(code);
    }

    private static DataIntegrityViolationException dbError(String sqlState) {
        return new DataIntegrityViolationException("lỗi ghi", new SQLException("lỗi gốc", sqlState));
    }

    @Nested
    @DisplayName("API 34: tạo hồ sơ")
    class Create {

        @Test
        @DisplayName("AC-1: hồ sơ CAP tạo kèm dòng benhanmaydaycap cùng ma_ho_so")
        void acuteRecordCreatesAcuteSubtype() {
            service.createMedicalRecord("2", createRequest("CAP"));

            ArgumentCaptor<AcuteUrticariaRecord> subtype = ArgumentCaptor.forClass(AcuteUrticariaRecord.class);
            verify(acuteRecordRepository).saveAndFlush(subtype.capture());
            assertThat(subtype.getValue().getMedicalRecordId()).isEqualTo(RECORD_ID);
            verify(chronicRecordRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("AC-1: hồ sơ MAN tạo kèm dòng benhanmaydayman, trạng thái mặc định draft")
        void chronicRecordCreatesChronicSubtypeAndDefaultsToDraft() {
            MedicalRecordResponse response = service.createMedicalRecord("2", createRequest("MAN"));

            ArgumentCaptor<ChronicUrticariaRecord> subtype = ArgumentCaptor.forClass(ChronicUrticariaRecord.class);
            verify(chronicRecordRepository).saveAndFlush(subtype.capture());
            assertThat(subtype.getValue().getMedicalRecordId()).isEqualTo(RECORD_ID);
            assertThat(response.getStatus()).isEqualTo("draft");
            assertThat(response.getRecordId()).isEqualTo("10");
        }

        @ParameterizedTest(name = "AC-2: trạng thái mở {0} được nhận khi tạo")
        @ValueSource(strings = {"draft", "pending", "in_progress"})
        void acceptsOpenStatusesOnCreate(String status) {
            CreateMedicalRecordRequest req = createRequest("MAN");
            req.setStatus(status);

            assertThat(service.createMedicalRecord("2", req).getStatus()).isEqualTo(status);
        }

        @ParameterizedTest(name = "AC-2: trạng thái {0} bị từ chối khi tạo")
        @ValueSource(strings = {"completed", "cancelled", "active", "DRAFT"})
        void rejectsClosedOrUnknownStatusesOnCreate(String status) {
            CreateMedicalRecordRequest req = createRequest("MAN");
            req.setStatus(status);

            assertCode(() -> service.createMedicalRecord("2", req), ErrorCode.INVALID_PARAMETER_VALUE);
        }

        @ParameterizedTest(name = "AC-2: loai_benh_an \"{0}\" bị từ chối và không khóa bệnh nhân")
        @ValueSource(strings = {"X", "cap", "man", " CAP", "CAP "})
        void rejectsUnknownTypeBeforeLockingPatient(String type) {
            assertCode(() -> service.createMedicalRecord("2", createRequest(type)), ErrorCode.INVALID_PARAMETER_VALUE);
            verifyNoInteractions(patientRepository);
        }

        @ParameterizedTest(name = "AC-2: thoi_gian_khoi_phat_tuan = {0} bị từ chối")
        @ValueSource(strings = {"-0.01", "10000000000", "1.234", "0.001"})
        void rejectsOnsetWeeksOutOfRange(String weeks) {
            CreateMedicalRecordRequest req = createRequest("MAN");
            req.setOnsetDurationWeeks(new BigDecimal(weeks));

            assertCode(() -> service.createMedicalRecord("2", req), ErrorCode.INVALID_PARAMETER_VALUE);
        }

        @ParameterizedTest(name = "AC-2: thoi_gian_khoi_phat_tuan = {0} được nhận")
        @ValueSource(strings = {"0", "1.50", "1.500", "9999999999.99"})
        void acceptsOnsetWeeksWithinRange(String weeks) {
            CreateMedicalRecordRequest req = createRequest("MAN");
            req.setOnsetDurationWeeks(new BigDecimal(weeks));

            assertThat(service.createMedicalRecord("2", req).getOnsetDurationWeeks()).hasScaleOf(2);
        }

        @ParameterizedTest(name = "AC-2: thời điểm \"{0}\" sai định dạng")
        @ValueSource(strings = {"2026-11-15T09:30:00", "2026-11-15 09:30:00+07:00", "2026-11-15T09:30:00.123+07:00",
                "15/11/2026", ""})
        void rejectsTimestampsWithoutTheAgreedFormat(String value) {
            CreateMedicalRecordRequest req = createRequest("MAN");
            req.setFirstVisitAt(value);

            assertCode(() -> service.createMedicalRecord("2", req), ErrorCode.INVALID_PARAMETER_VALUE);
        }

        @Test
        @DisplayName("Thời điểm có offset được trả về theo giờ Asia/Ho_Chi_Minh")
        void returnsTimestampInClinicZone() {
            CreateMedicalRecordRequest req = createRequest("MAN");
            req.setFirstVisitAt("2026-11-15T10:00:00+09:00");

            assertThat(service.createMedicalRecord("2", req).getFirstVisitAt()).isEqualTo("2026-11-15T08:00:00+07:00");
        }

        @Test
        @DisplayName("AC-2: ngày kết thúc điều trị trước ngày bắt đầu bị từ chối")
        void rejectsTreatmentEndBeforeStart() {
            CreateMedicalRecordRequest req = createRequest("MAN");
            req.setOutpatientTreatmentStartDate(LocalDate.of(2026, 11, 10));
            req.setOutpatientTreatmentEndDate(LocalDate.of(2026, 11, 1));

            assertCode(() -> service.createMedicalRecord("2", req), ErrorCode.INVALID_PARAMETER_VALUE);
        }

        @ParameterizedTest(name = "AC-2: ket_qua_dieu_tri_bao_hiem \"{0}\" bị từ chối")
        @ValueSource(strings = {"Tot", "khoi benh", ""})
        void rejectsUnknownInsuranceOutcome(String outcome) {
            CreateMedicalRecordRequest req = createRequest("MAN");
            req.setInsuranceTreatmentOutcome(outcome);

            assertCode(() -> service.createMedicalRecord("2", req), ErrorCode.INVALID_PARAMETER_VALUE);
        }

        @ParameterizedTest(name = "AC-2: tien_su_gia_dinh = Co với ghi chú \"{0}\" bị từ chối")
        @ValueSource(strings = {"", "   "})
        void rejectsFamilyHistoryYesWithBlankNote(String note) {
            CreateMedicalRecordRequest req = createRequest("MAN");
            req.setFamilyHistory("Co");
            req.setFamilyHistoryDescription(note);

            assertCode(() -> service.createMedicalRecord("2", req), ErrorCode.INVALID_PARAMETER_VALUE);
        }

        @ParameterizedTest(name = "AC-2: tien_su_gia_dinh = {0} luôn lưu ghi chú là NULL")
        @ValueSource(strings = {"Khong", "Khong ro"})
        void dropsFamilyHistoryNoteWhenAnswerIsNotYes(String answer) {
            CreateMedicalRecordRequest req = createRequest("MAN");
            req.setFamilyHistory(answer);
            req.setFamilyHistoryDescription("bị bỏ đi");

            assertThat(service.createMedicalRecord("2", req).getFamilyHistoryDescription()).isNull();
        }

        @Test
        @DisplayName("AC-2: tien_su_gia_dinh không thuộc danh mục bị từ chối")
        void rejectsUnknownFamilyHistory() {
            CreateMedicalRecordRequest req = createRequest("MAN");
            req.setFamilyHistory("Yes");

            assertCode(() -> service.createMedicalRecord("2", req), ErrorCode.INVALID_PARAMETER_VALUE);
        }

        @ParameterizedTest(name = "AC-3: patient_id \"{0}\" không phải số trả 3007")
        @ValueSource(strings = {"abc", "", "1.5", "99999999999999999999"})
        void nonNumericPatientIdReturns3007(String patientId) {
            assertThatThrownBy(() -> service.createMedicalRecord(patientId, createRequest("MAN")))
                    .isInstanceOf(AppException.class)
                    .hasMessage("Patient not found")
                    .extracting(ex -> ((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.USER_ACCOUNT_NOT_FOUND);
        }

        @Test
        @DisplayName("AC-3: bệnh nhân không tồn tại trả 3007 và không ghi gì")
        void missingPatientReturns3007() {
            when(patientRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

            assertCode(() -> service.createMedicalRecord("99", createRequest("MAN")), ErrorCode.USER_ACCOUNT_NOT_FOUND);
            verify(medicalRecordRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("AC-4: đã có hồ sơ cùng loại đang mở thì trả 4006 và không ghi gì")
        void openRecordOfSameTypeReturns4006() {
            when(medicalRecordRepository.existsByPatientIdAndMedicalRecordTypeAndStatusIn(eq(PATIENT_ID), eq("MAN"), any()))
                    .thenReturn(true);

            assertCode(() -> service.createMedicalRecord("2", createRequest("MAN")),
                    ErrorCode.PATIENT_ALREADY_HAS_ACTIVE_RECORD);
            verify(medicalRecordRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("AC-4: luật 4006 chỉ đếm ba trạng thái mở")
        @SuppressWarnings("unchecked")
        void openRecordCheckUsesOnlyOpenStatuses() {
            service.createMedicalRecord("2", createRequest("MAN"));

            ArgumentCaptor<java.util.Collection<String>> statuses = ArgumentCaptor.forClass(java.util.Collection.class);
            verify(medicalRecordRepository).existsByPatientIdAndMedicalRecordTypeAndStatusIn(
                    eq(PATIENT_ID), eq("MAN"), statuses.capture());
            assertThat(statuses.getValue()).containsExactlyInAnyOrder("draft", "pending", "in_progress");
        }

        @Test
        @DisplayName("AC-5: khóa dòng bệnh nhân trước khi kiểm tra hồ sơ mở, rồi mới ghi")
        void locksPatientBeforeCheckingOpenRecords() {
            service.createMedicalRecord("2", createRequest("MAN"));

            InOrder order = inOrder(patientRepository, medicalRecordRepository);
            order.verify(patientRepository).findByIdForUpdate(PATIENT_ID);
            order.verify(medicalRecordRepository).existsByPatientIdAndMedicalRecordTypeAndStatusIn(anyLong(), anyString(), any());
            order.verify(medicalRecordRepository).saveAndFlush(any());
        }

        @ParameterizedTest(name = "AC-2: lỗi database SQLState {0} trả 2003")
        @ValueSource(strings = {"23514", "22003"})
        void checkViolationFromDatabaseReturns2003(String sqlState) {
            when(medicalRecordRepository.saveAndFlush(any())).thenThrow(dbError(sqlState));

            assertCode(() -> service.createMedicalRecord("2", createRequest("MAN")), ErrorCode.INVALID_PARAMETER_VALUE);
        }

        @Test
        @DisplayName("Lỗi database khác được ném lại nguyên vẹn")
        void otherDatabaseErrorsAreRethrown() {
            DataIntegrityViolationException unique = dbError("23505");
            when(medicalRecordRepository.saveAndFlush(any())).thenThrow(unique);

            assertThatThrownBy(() -> service.createMedicalRecord("2", createRequest("MAN"))).isSameAs(unique);
        }
    }

    @Nested
    @DisplayName("API 35: danh sách")
    class ListRecords {

        @BeforeEach
        void emptyPage() {
            Page<MedicalRecord> empty = new PageImpl<>(List.of());
            when(medicalRecordRepository.search(any(), any(), any(), any(Pageable.class))).thenReturn(empty);
        }

        @Test
        @DisplayName("AC-6: mặc định trang 1, 20 dòng; bộ lọc rỗng coi như không lọc")
        void defaultsToFirstPageOfTwentyWithoutFilters() {
            service.listMedicalRecords(" ", "", null, null, null);

            ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
            verify(medicalRecordRepository).search(isNull(), isNull(), isNull(), pageable.capture());
            assertThat(pageable.getValue().getPageNumber()).isZero();
            assertThat(pageable.getValue().getPageSize()).isEqualTo(20);
        }

        @Test
        @DisplayName("AC-6: page bắt đầu từ 1 nên trang 3 là trang số 2 của Spring")
        void pageIsOneBased() {
            service.listMedicalRecords("2", "MAN", "draft", 3, 5);

            ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
            verify(medicalRecordRepository).search(eq(PATIENT_ID), eq("MAN"), eq("draft"), pageable.capture());
            assertThat(pageable.getValue().getPageNumber()).isEqualTo(2);
            assertThat(pageable.getValue().getPageSize()).isEqualTo(5);
        }

        @ParameterizedTest(name = "AC-7: page={0}, size={1} trả 2003")
        @CsvSource({"0,20", "-1,20", "1,0", "1,101"})
        void rejectsPagingOutOfRange(int page, int size) {
            assertCode(() -> service.listMedicalRecords(null, null, null, page, size), ErrorCode.INVALID_PARAMETER_VALUE);
        }

        @Test
        @DisplayName("AC-7: size = 100 là giới hạn trên được nhận")
        void acceptsMaximumPageSize() {
            assertThat(service.listMedicalRecords(null, null, null, 1, 100)).isNotNull();
        }

        @ParameterizedTest(name = "AC-7: bộ lọc patient_id={0}, loai={1}, trang_thai={2} trả 2003")
        @CsvSource(nullValues = "null", value = {"abc,null,null", "null,X,null", "null,cap,null", "null,null,active"})
        void rejectsInvalidFilters(String patientId, String type, String status) {
            assertCode(() -> service.listMedicalRecords(patientId, type, status, null, null),
                    ErrorCode.INVALID_PARAMETER_VALUE);
        }
    }

    @Nested
    @DisplayName("API 36: chi tiết và kiểm tra chủ hồ sơ")
    class Detail {

        @Test
        @DisplayName("AC-9: id không phải số trả 4001 mà không đọc database")
        void nonNumericIdReturns4001() {
            assertCode(() -> service.getMedicalRecord("abc"), ErrorCode.MEDICAL_RECORD_NOT_FOUND);
            verifyNoInteractions(medicalRecordRepository);
        }

        @Test
        @DisplayName("AC-10: 4001 được kiểm tra trước 3106, nên không đọc tài khoản")
        void notFoundIsCheckedBeforeOwnership() {
            signInAs("benh_nhan", ACCOUNT_ID);
            when(medicalRecordRepository.findById(RECORD_ID)).thenReturn(Optional.empty());

            assertCode(() -> service.getMedicalRecord(RECORD), ErrorCode.MEDICAL_RECORD_NOT_FOUND);
            verifyNoInteractions(userAccountRepository);
        }

        @Test
        @DisplayName("AC-10: bệnh nhân xem hồ sơ của mình qua taikhoan.ma_nguoi")
        void patientSeesOwnRecordResolvedThroughAccount() {
            signInAs("benh_nhan", ACCOUNT_ID);
            givenStoredRecord("MAN", "draft");
            when(userAccountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(
                    UserAccount.builder().accountId(ACCOUNT_ID).person(Person.builder().personId(PATIENT_ID).build()).build()));

            assertThat(service.getMedicalRecord(RECORD)).isInstanceOf(ChronicMedicalRecordDetailResponse.class);
        }

        @Test
        @DisplayName("AC-10: ma_tai_khoan trùng ma_benh_nhan nhưng ma_nguoi khác vẫn bị chặn 3106")
        void accountIdIsNeverComparedWithPatientId() {
            signInAs("benh_nhan", PATIENT_ID);
            givenStoredRecord("MAN", "draft");
            when(userAccountRepository.findById(PATIENT_ID)).thenReturn(Optional.of(
                    UserAccount.builder().accountId(PATIENT_ID).person(Person.builder().personId(77L).build()).build()));

            assertCode(() -> service.getMedicalRecord(RECORD), ErrorCode.CANNOT_ACCESS_ANOTHER_USER_DATA);
        }

        @Test
        @DisplayName("AC-10: token hợp lệ nhưng tài khoản không còn trả 3106")
        void missingAccountReturns3106() {
            signInAs("benh_nhan", ACCOUNT_ID);
            givenStoredRecord("MAN", "draft");
            when(userAccountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.empty());

            assertCode(() -> service.getMedicalRecord(RECORD), ErrorCode.CANNOT_ACCESS_ANOTHER_USER_DATA);
        }

        @Test
        @DisplayName("AC-10: bác sĩ không bị kiểm tra chủ hồ sơ")
        void doctorSkipsOwnershipCheck() {
            givenStoredRecord("CAP", "draft");

            service.getMedicalRecord(RECORD);

            verifyNoInteractions(userAccountRepository);
        }
    }

    @Nested
    @DisplayName("API 37: sửa hồ sơ")
    class Update {

        private UpdateMedicalRecordRequest withStatus(String status) {
            UpdateMedicalRecordRequest req = new UpdateMedicalRecordRequest();
            req.setStatus(status);
            return req;
        }

        @ParameterizedTest(name = "AC-12: {0} → {1} được phép")
        @CsvSource({"draft,pending", "draft,in_progress", "draft,completed", "pending,in_progress", "pending,completed",
                "in_progress,completed", "draft,cancelled", "pending,cancelled", "in_progress,cancelled",
                "draft,draft", "in_progress,in_progress"})
        void allowsForwardMovesAndCancel(String from, String to) {
            givenStoredRecord("MAN", from);

            assertThat(service.updateMedicalRecord(RECORD, withStatus(to)).getStatus()).isEqualTo(to);
        }

        @ParameterizedTest(name = "AC-12: {0} → {1} bị từ chối 2003")
        @CsvSource({"pending,draft", "in_progress,draft", "in_progress,pending", "draft,active", "draft,DRAFT"})
        void rejectsBackwardOrUnknownMoves(String from, String to) {
            givenStoredRecord("MAN", from);

            assertCode(() -> service.updateMedicalRecord(RECORD, withStatus(to)), ErrorCode.INVALID_PARAMETER_VALUE);
            verify(medicalRecordRepository, never()).saveAndFlush(any());
        }

        @ParameterizedTest(name = "AC-13: hồ sơ {0} trả 4003 với body rỗng, trước mọi kiểm tra khác")
        @ValueSource(strings = {"completed", "cancelled"})
        void closedRecordReturns4003BeforeAnyOtherCheck(String status) {
            givenStoredRecord("MAN", status);
            UpdateMedicalRecordRequest invalid = withStatus("not-a-status");

            assertCode(() -> service.updateMedicalRecord(RECORD, new UpdateMedicalRecordRequest()),
                    ErrorCode.MEDICAL_RECORD_ALREADY_COMPLETED);
            assertCode(() -> service.updateMedicalRecord(RECORD, invalid), ErrorCode.MEDICAL_RECORD_ALREADY_COMPLETED);
        }

        @Test
        @DisplayName("AC-9: hồ sơ không tồn tại trả 4001, đọc bằng khóa FOR UPDATE")
        void missingRecordReturns4001UsingRowLock() {
            when(medicalRecordRepository.findByIdForUpdate(RECORD_ID)).thenReturn(Optional.empty());

            assertCode(() -> service.updateMedicalRecord(RECORD, new UpdateMedicalRecordRequest()),
                    ErrorCode.MEDICAL_RECORD_NOT_FOUND);
            verify(medicalRecordRepository, never()).findById(anyLong());
        }

        @Test
        @DisplayName("AC-11: trường null giữ nguyên giá trị hiện tại")
        void nullFieldsKeepCurrentValues() {
            MedicalRecord record = givenStoredRecord("MAN", "draft");
            record.setInitialClinicDiagnosis("Mày đay mạn");
            record.setOnsetDurationWeeks(new BigDecimal("7.5"));

            MedicalRecordResponse response = service.updateMedicalRecord(RECORD, new UpdateMedicalRecordRequest());

            assertThat(response.getInitialClinicDiagnosis()).isEqualTo("Mày đay mạn");
            assertThat(response.getOnsetDurationWeeks()).isEqualByComparingTo("7.50");
        }

        @Test
        @DisplayName("AC-14: đổi sang Co dùng được ghi chú đã lưu")
        void familyHistoryYesCanReuseStoredNote() {
            MedicalRecord record = givenStoredRecord("MAN", "draft");
            record.setFamilyHistory("Co");
            record.setFamilyHistoryDescription("Mẹ bị mày đay");
            UpdateMedicalRecordRequest req = new UpdateMedicalRecordRequest();
            req.setFamilyHistory("Co");

            assertThat(service.updateMedicalRecord(RECORD, req).getFamilyHistoryDescription()).isEqualTo("Mẹ bị mày đay");
        }

        @Test
        @DisplayName("AC-14: đổi sang Khong xóa ghi chú đã lưu")
        void familyHistoryNoClearsStoredNote() {
            MedicalRecord record = givenStoredRecord("MAN", "draft");
            record.setFamilyHistory("Co");
            record.setFamilyHistoryDescription("Mẹ bị mày đay");
            UpdateMedicalRecordRequest req = new UpdateMedicalRecordRequest();
            req.setFamilyHistory("Khong");

            assertThat(service.updateMedicalRecord(RECORD, req).getFamilyHistoryDescription()).isNull();
        }

        @Test
        @DisplayName("AC-14: đổi sang Co khi chưa có ghi chú nào trả 2003")
        void familyHistoryYesWithoutAnyNoteReturns2003() {
            givenStoredRecord("MAN", "draft");
            UpdateMedicalRecordRequest req = new UpdateMedicalRecordRequest();
            req.setFamilyHistory("Co");

            assertCode(() -> service.updateMedicalRecord(RECORD, req), ErrorCode.INVALID_PARAMETER_VALUE);
        }

        @Test
        @DisplayName("AC-14: ngày kết thúc gửi lên được so với ngày bắt đầu đã lưu")
        void treatmentDatesAreCheckedOnFinalValues() {
            MedicalRecord record = givenStoredRecord("MAN", "draft");
            record.setOutpatientTreatmentStartDate(LocalDate.of(2026, 11, 10));
            UpdateMedicalRecordRequest req = new UpdateMedicalRecordRequest();
            req.setOutpatientTreatmentEndDate(LocalDate.of(2026, 11, 1));

            assertCode(() -> service.updateMedicalRecord(RECORD, req), ErrorCode.INVALID_PARAMETER_VALUE);
        }

        @Test
        @DisplayName("AC-2: lỗi CHECK từ database khi sửa trả 2003")
        void checkViolationOnUpdateReturns2003() {
            givenStoredRecord("MAN", "draft");
            when(medicalRecordRepository.saveAndFlush(any())).thenThrow(dbError("23514"));

            assertCode(() -> service.updateMedicalRecord(RECORD, new UpdateMedicalRecordRequest()),
                    ErrorCode.INVALID_PARAMETER_VALUE);
        }
    }

    @Nested
    @DisplayName("API 38: xóa hồ sơ")
    class Delete {

        @Test
        @DisplayName("AC-15: xóa subtype CAP rồi xóa hồ sơ, rồi flush trong cùng khối")
        void deletesSubtypeThenRecordThenFlushes() {
            MedicalRecord record = givenStoredRecord("CAP", "draft");
            AcuteUrticariaRecord subtype = AcuteUrticariaRecord.builder().medicalRecordId(RECORD_ID).build();
            when(acuteRecordRepository.findById(RECORD_ID)).thenReturn(Optional.of(subtype));

            service.deleteMedicalRecord(RECORD);

            InOrder order = inOrder(acuteRecordRepository, medicalRecordRepository);
            order.verify(acuteRecordRepository).delete(subtype);
            order.verify(medicalRecordRepository).delete(record);
            order.verify(medicalRecordRepository).flush();
        }

        @Test
        @DisplayName("AC-16: lỗi khóa ngoại khi flush trả 4007")
        void foreignKeyViolationReturns4007() {
            givenStoredRecord("MAN", "draft");
            doThrow(dbError("23503")).when(medicalRecordRepository).flush();

            assertCode(() -> service.deleteMedicalRecord(RECORD), ErrorCode.MEDICAL_RECORD_HAS_CHILD_DATA);
        }

        @Test
        @DisplayName("Lỗi database khác khi xóa được ném lại nguyên vẹn")
        void otherDeleteErrorsAreRethrown() {
            givenStoredRecord("MAN", "draft");
            DataIntegrityViolationException other = dbError("23514");
            doThrow(other).when(medicalRecordRepository).flush();

            assertThatThrownBy(() -> service.deleteMedicalRecord(RECORD)).isSameAs(other);
        }

        @Test
        @DisplayName("AC-9: hồ sơ đã bị xóa (khóa không thấy dòng) trả 4001")
        void alreadyDeletedRecordReturns4001() {
            when(medicalRecordRepository.findByIdForUpdate(RECORD_ID)).thenReturn(Optional.empty());

            assertCode(() -> service.deleteMedicalRecord(RECORD), ErrorCode.MEDICAL_RECORD_NOT_FOUND);
            verify(medicalRecordRepository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("API 39: lịch sử khám")
    class History {

        @Test
        @DisplayName("AC-17: bệnh nhân không tồn tại trả 3007 trước khi kiểm tra chủ sở hữu")
        void missingPatientIsCheckedBeforeOwnership() {
            signInAs("benh_nhan", ACCOUNT_ID);
            when(patientRepository.existsById(99L)).thenReturn(false);

            assertCode(() -> service.getPatientRecordHistory("99"), ErrorCode.USER_ACCOUNT_NOT_FOUND);
            verifyNoInteractions(userAccountRepository);
        }

        @Test
        @DisplayName("AC-17: bệnh nhân xem lịch sử của người khác trả 3106")
        void patientCannotReadAnotherPatientsHistory() {
            signInAs("benh_nhan", ACCOUNT_ID);
            when(userAccountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(
                    UserAccount.builder().accountId(ACCOUNT_ID).person(Person.builder().personId(77L).build()).build()));

            assertCode(() -> service.getPatientRecordHistory("2"), ErrorCode.CANNOT_ACCESS_ANOTHER_USER_DATA);
            verifyNoInteractions(medicalVisitRepository);
        }

        @Test
        @DisplayName("AC-17: chưa có phiên khám thì trả danh sách rỗng")
        void noVisitsReturnsEmptyList() {
            when(medicalVisitRepository.findHistoryByPatientId(PATIENT_ID)).thenReturn(List.of());

            assertThat(service.getPatientRecordHistory("2")).isEmpty();
        }
    }
}
