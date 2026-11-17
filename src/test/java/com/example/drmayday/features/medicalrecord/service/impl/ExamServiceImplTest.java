package com.example.drmayday.features.medicalrecord.service.impl;

import com.example.drmayday.common.exception.AppException;
import com.example.drmayday.common.exception.ErrorCode;
import com.example.drmayday.common.security.UserPrincipal;
import com.example.drmayday.entity.AcuteVisit;
import com.example.drmayday.entity.Branch;
import com.example.drmayday.entity.ChronicVisit;
import com.example.drmayday.entity.ClinicRoom;
import com.example.drmayday.entity.Employee;
import com.example.drmayday.entity.MedicalRecord;
import com.example.drmayday.entity.MedicalVisit;
import com.example.drmayday.entity.Person;
import com.example.drmayday.entity.UserAccount;
import com.example.drmayday.features.medicalrecord.dto.CreateExamRequest;
import com.example.drmayday.features.medicalrecord.dto.ExamResponse;
import com.example.drmayday.features.medicalrecord.dto.UpdateExamRequest;
import com.example.drmayday.repository.AcuteVisitRepository;
import com.example.drmayday.repository.ChronicVisitRepository;
import com.example.drmayday.repository.ClinicRoomRepository;
import com.example.drmayday.repository.EmployeeRepository;
import com.example.drmayday.repository.MedicalRecordRepository;
import com.example.drmayday.repository.MedicalVisitRepository;
import com.example.drmayday.repository.UserAccountRepository;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Khóa luật nghiệp vụ của API 46 đến 49 (spec 0004) mà dữ liệu thật khó tạo: thứ tự kiểm tra, khóa hồ sơ trước khi đọc phiên,
 * luật gộp nhiệt độ, đổi lỗi database, và "hôm nay" theo giờ Việt Nam. Chỉ giả lập repository, là ranh giới với database.
 * Đồng hồ cố định ở 00:30 ngày 2026-11-16 giờ Việt Nam, lúc đó giờ UTC vẫn là 2026-11-15.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ExamServiceImplTest {

    private static final long RECORD_ID = 10L;
    private static final String RECORD = "10";
    private static final long VISIT_ID = 50L;
    private static final long ACCOUNT_ID = 20L;
    private static final long DOCTOR_PERSON = 7L;
    private static final long PATIENT_ID = 2L;
    private static final long ROOM_ID = 30L;
    private static final LocalDate TODAY_VN = LocalDate.of(2026, 11, 16);
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-15T17:30:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));

    @Mock private MedicalVisitRepository medicalVisitRepository;
    @Mock private AcuteVisitRepository acuteVisitRepository;
    @Mock private ChronicVisitRepository chronicVisitRepository;
    @Mock private MedicalRecordRepository medicalRecordRepository;
    @Mock private ClinicRoomRepository clinicRoomRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private UserAccountRepository userAccountRepository;

    private ExamServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ExamServiceImpl(medicalVisitRepository, acuteVisitRepository, chronicVisitRepository,
                medicalRecordRepository, clinicRoomRepository, employeeRepository, userAccountRepository,
                new RecordOwnerCheck(userAccountRepository), CLOCK);
        signInAs("bac_si", ACCOUNT_ID);

        when(userAccountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(
                UserAccount.builder().accountId(ACCOUNT_ID).person(Person.builder().personId(DOCTOR_PERSON).build()).build()));
        when(employeeRepository.existsById(DOCTOR_PERSON)).thenReturn(true);
        when(medicalVisitRepository.findMaxVisitNumber(RECORD_ID)).thenReturn(0);
        when(medicalVisitRepository.saveAndFlush(any(MedicalVisit.class))).thenAnswer(call -> {
            MedicalVisit visit = call.getArgument(0);
            if (visit.getMedicalVisitId() == null) {
                visit.setMedicalVisitId(VISIT_ID);
            }
            return visit;
        });
    }

    @AfterEach
    void signOut() {
        SecurityContextHolder.clearContext();
    }

    private static void signInAs(String role, long accountId) {
        UserPrincipal principal = UserPrincipal.create(accountId, "0900000000", role);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private MedicalRecord givenRecord(String type, String status) {
        MedicalRecord record = MedicalRecord.builder()
                .medicalRecordId(RECORD_ID).patientId(PATIENT_ID).medicalRecordType(type).status(status).build();
        when(medicalRecordRepository.findById(RECORD_ID)).thenReturn(Optional.of(record));
        when(medicalRecordRepository.findByIdForUpdate(RECORD_ID)).thenReturn(Optional.of(record));
        return record;
    }

    private void givenRoom(long roomId, boolean branchActive) {
        Branch branch = new Branch();
        branch.setActive(branchActive);
        ClinicRoom room = ClinicRoom.builder().clinicRoomId(roomId).name("Phòng " + roomId).branch(branch).build();
        when(clinicRoomRepository.findByIdWithBranch(roomId)).thenReturn(Optional.of(room));
    }

    private MedicalVisit givenStoredVisit(MedicalVisit.MedicalVisitBuilder builder) {
        MedicalVisit visit = builder.medicalVisitId(VISIT_ID).medicalRecordId(RECORD_ID).build();
        when(medicalVisitRepository.findRecordIdById(VISIT_ID)).thenReturn(Optional.of(RECORD_ID));
        when(medicalVisitRepository.findById(VISIT_ID)).thenReturn(Optional.of(visit));
        return visit;
    }

    private static MedicalVisit.MedicalVisitBuilder acuteVisit() {
        return MedicalVisit.builder().medicalRecordType("CAP").physicianId(DOCTOR_PERSON)
                .visitDate(LocalDate.of(2026, 11, 10)).visitNumber(1).visitType("Kham lan dau").serumSampleStored(false);
    }

    private static CreateExamRequest create(String recordId) {
        CreateExamRequest req = new CreateExamRequest();
        req.setRecordId(recordId);
        return req;
    }

    private static DataIntegrityViolationException dbError(String sqlState, String message) {
        return new DataIntegrityViolationException("lỗi ghi", new SQLException(message, sqlState));
    }

    private static void assertCode(ThrowingCallable call, ErrorCode code) {
        assertThatThrownBy(call)
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(code);
    }

    @Nested
    @DisplayName("API 46 tạo phiên")
    class Create {

        @Test
        @DisplayName("AC-1, AC-2: phiên CAP lưu ma_bac_si là ma_nguoi của tài khoản, không phải ma_tai_khoan, và chèn phienkhamcap")
        void acuteExamStoresCallerPersonAndAcuteSubtype() {
            givenRecord("CAP", "in_progress");

            ExamResponse response = service.createExam(create(RECORD));

            assertThat(response.physicianId()).isEqualTo(String.valueOf(DOCTOR_PERSON));
            assertThat(response.medicalRecordType()).isEqualTo("CAP");
            verify(acuteVisitRepository).saveAndFlush(any(AcuteVisit.class));
            verify(chronicVisitRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("AC-1: phiên MAN lấy loai_benh_an từ hồ sơ và chèn phienkhamman")
        void chronicExamUsesRecordTypeAndChronicSubtype() {
            givenRecord("MAN", "draft");

            ExamResponse response = service.createExam(create(RECORD));

            assertThat(response.medicalRecordType()).isEqualTo("MAN");
            verify(chronicVisitRepository).saveAndFlush(any(ChronicVisit.class));
            verify(acuteVisitRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("AC-3, AC-5: không gửi lan_kham_thu thì lấy số lớn nhất cộng 1, loai_phien_kham suy từ số đó")
        void computesNextVisitNumberAndVisitType() {
            givenRecord("CAP", "in_progress");
            when(medicalVisitRepository.findMaxVisitNumber(RECORD_ID)).thenReturn(4);

            ExamResponse response = service.createExam(create(RECORD));

            assertThat(response.visitNumber()).isEqualTo(5);
            assertThat(response.visitType()).isEqualTo("Tai kham");
        }

        @Test
        @DisplayName("AC-5: phiên đầu tiên là Kham lan dau, luu_huyet_thanh mặc định false")
        void firstVisitDefaults() {
            givenRecord("CAP", "in_progress");

            ExamResponse response = service.createExam(create(RECORD));

            assertThat(response.visitNumber()).isEqualTo(1);
            assertThat(response.visitType()).isEqualTo("Kham lan dau");
            assertThat(response.serumSampleStored()).isFalse();
        }

        @Test
        @DisplayName("AC-5: ngay_kham mặc định là hôm nay theo giờ Việt Nam (2026-11-16), không theo UTC")
        void defaultVisitDateUsesClinicZone() {
            givenRecord("CAP", "in_progress");

            ExamResponse response = service.createExam(create(RECORD));

            assertThat(response.visitDate()).isEqualTo(TODAY_VN.toString());
        }

        @Test
        @DisplayName("AC-6: ngay_kham là hôm nay theo giờ Việt Nam thì hợp lệ, ngày kế tiếp trả 2003")
        void visitDateBoundaryFollowsClinicZone() {
            givenRecord("CAP", "in_progress");
            CreateExamRequest today = create(RECORD);
            today.setVisitDate(TODAY_VN);
            CreateExamRequest tomorrow = create(RECORD);
            tomorrow.setVisitDate(TODAY_VN.plusDays(1));

            assertThat(service.createExam(today).visitDate()).isEqualTo("2026-11-16");
            assertCode(() -> service.createExam(tomorrow), ErrorCode.INVALID_PARAMETER_VALUE);
        }

        @Test
        @DisplayName("AC-8: record_id thiếu trả 2001 trước khi khóa hồ sơ")
        void missingRecordIdIsCheckedBeforeLocking() {
            assertCode(() -> service.createExam(create(null)), ErrorCode.MISSING_REQUIRED_PARAMETER);
            assertCode(() -> service.createExam(create("   ")), ErrorCode.MISSING_REQUIRED_PARAMETER);
            verifyNoInteractions(medicalRecordRepository);
        }

        @ParameterizedTest(name = "AC-8: record_id \"{0}\" trả 2002")
        @ValueSource(strings = {"abc", "0", "-1", "1.5", "99999999999999999999"})
        void invalidRecordIdIs2002(String value) {
            assertCode(() -> service.createExam(create(value)), ErrorCode.INVALID_PARAMETER_TYPE);
            verifyNoInteractions(medicalRecordRepository);
        }

        @Test
        @DisplayName("AC-8: hồ sơ không tồn tại trả 4001")
        void unknownRecordIs4001() {
            when(medicalRecordRepository.findByIdForUpdate(RECORD_ID)).thenReturn(Optional.empty());

            assertCode(() -> service.createExam(create(RECORD)), ErrorCode.MEDICAL_RECORD_NOT_FOUND);
        }

        @ParameterizedTest(name = "AC-9: hồ sơ {0} trả 4003 trước luật giá trị, không ghi gì")
        @ValueSource(strings = {"completed", "cancelled"})
        void closedRecordIsCheckedBeforeValueRules(String status) {
            givenRecord("CAP", status);
            CreateExamRequest req = create(RECORD);
            req.setVisitDate(TODAY_VN.plusYears(1));

            assertCode(() -> service.createExam(req), ErrorCode.MEDICAL_RECORD_ALREADY_COMPLETED);
            verify(medicalVisitRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("AC-6, AC-8: luật giá trị (2003) chạy trước kiểm tra phòng")
        void valueRulesRunBeforeRoomLookup() {
            givenRecord("CAP", "in_progress");
            CreateExamRequest req = create(RECORD);
            req.setVisitType("Khac");
            req.setClinicRoomId("999");

            assertCode(() -> service.createExam(req), ErrorCode.INVALID_PARAMETER_VALUE);
            verifyNoInteractions(clinicRoomRepository);
        }

        @Test
        @DisplayName("AC-3, AC-8: phòng không tồn tại (2103) được báo trước trùng lan_kham_thu (4009)")
        void roomIsCheckedBeforeDuplicateNumber() {
            givenRecord("CAP", "in_progress");
            when(clinicRoomRepository.findByIdWithBranch(999L)).thenReturn(Optional.empty());
            when(medicalVisitRepository.existsByMedicalRecordIdAndVisitNumber(RECORD_ID, 1)).thenReturn(true);
            CreateExamRequest req = create(RECORD);
            req.setClinicRoomId("999");
            req.setVisitNumber(1);

            assertCode(() -> service.createExam(req), ErrorCode.ROOM_NOT_FOUND);
        }

        @Test
        @DisplayName("AC-8: phòng thuộc chi nhánh ngừng hoạt động trả 2106")
        void inactiveBranchIs2106() {
            givenRecord("CAP", "in_progress");
            givenRoom(ROOM_ID, false);
            CreateExamRequest req = create(RECORD);
            req.setClinicRoomId(String.valueOf(ROOM_ID));

            assertCode(() -> service.createExam(req), ErrorCode.BRANCH_INACTIVE);
        }

        @Test
        @DisplayName("AC-3: lan_kham_thu gửi lên đã có trong hồ sơ trả 4009")
        void duplicateSentNumberIs4009() {
            givenRecord("CAP", "in_progress");
            when(medicalVisitRepository.existsByMedicalRecordIdAndVisitNumber(RECORD_ID, 3)).thenReturn(true);
            CreateExamRequest req = create(RECORD);
            req.setVisitNumber(3);

            assertCode(() -> service.createExam(req), ErrorCode.EXAM_SEQUENCE_DUPLICATE);
            verify(medicalVisitRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("AC-2: token có tài khoản không còn trong taikhoan trả 3106")
        void missingAccountIs3106() {
            givenRecord("CAP", "in_progress");
            when(userAccountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.empty());

            assertCode(() -> service.createExam(create(RECORD)), ErrorCode.CANNOT_ACCESS_ANOTHER_USER_DATA);
        }

        @Test
        @DisplayName("AC-2: ma_nguoi của người gọi không có trong nhanvien trả 3103")
        void callerWithoutEmployeeRowIs3103() {
            givenRecord("CAP", "in_progress");
            when(employeeRepository.existsById(DOCTOR_PERSON)).thenReturn(false);

            assertCode(() -> service.createExam(create(RECORD)), ErrorCode.DOCTOR_ROLE_REQUIRED);
            verify(medicalVisitRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("AC-7: ghi chú rỗng hoặc toàn khoảng trắng lưu NULL, có nội dung lưu nguyên")
        void blankNotesBecomeNull() {
            givenRecord("CAP", "in_progress");
            CreateExamRequest req = create(RECORD);
            req.setVisitSummary("  Ổn định  ");
            req.setNextVisitManagement("   ");
            req.setInstructions("");

            ExamResponse response = service.createExam(req);

            assertThat(response.visitSummary()).isEqualTo("  Ổn định  ");
            assertThat(response.nextVisitManagement()).isNull();
            assertThat(response.instructions()).isNull();
        }

        @Test
        @DisplayName("AC-3: trùng UNIQUE lúc ghi (23505) được đổi thành 4009")
        void uniqueViolationOnInsertIs4009() {
            givenRecord("CAP", "in_progress");
            when(medicalVisitRepository.saveAndFlush(any(MedicalVisit.class))).thenThrow(dbError("23505", "uq_phienkham_lan_kham"));

            assertCode(() -> service.createExam(create(RECORD)), ErrorCode.EXAM_SEQUENCE_DUPLICATE);
        }

        @Test
        @DisplayName("AC-8: phòng bị xóa giữa lúc kiểm tra và lúc ghi (23503 trên fk_phienkham_phong) trả 2103")
        void roomForeignKeyOnInsertIs2103() {
            givenRecord("CAP", "in_progress");
            givenRoom(ROOM_ID, true);
            when(medicalVisitRepository.saveAndFlush(any(MedicalVisit.class))).thenThrow(dbError("23503",
                    "insert or update on table \"phienkham\" violates foreign key constraint \"fk_phienkham_phong\""));
            CreateExamRequest req = create(RECORD);
            req.setClinicRoomId(String.valueOf(ROOM_ID));

            assertCode(() -> service.createExam(req), ErrorCode.ROOM_NOT_FOUND);
        }

        @Test
        @DisplayName("Khóa ngoại khác (ví dụ ma_bac_si) không bị đổi thành 2103 mà được ném lại")
        void otherForeignKeyIsRethrown() {
            givenRecord("CAP", "in_progress");
            when(medicalVisitRepository.saveAndFlush(any(MedicalVisit.class))).thenThrow(dbError("23503",
                    "insert or update on table \"phienkham\" violates foreign key constraint \"fk_phienkham_bacsi\""));

            assertThatThrownBy(() -> service.createExam(create(RECORD))).isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("Vi phạm CHECK lúc ghi subtype (23514) được đổi thành 2003")
        void checkViolationOnSubtypeIs2003() {
            givenRecord("CAP", "in_progress");
            when(acuteVisitRepository.saveAndFlush(any(AcuteVisit.class))).thenThrow(dbError("23514", "ck_phienkhamcap_so_lan_non"));

            assertCode(() -> service.createExam(create(RECORD)), ErrorCode.INVALID_PARAMETER_VALUE);
        }
    }

    @Nested
    @DisplayName("API 49 sửa phiên")
    class Update {

        @Test
        @DisplayName("AC-15: khóa dòng hồ sơ trước, rồi mới đọc entity phiên")
        void locksRecordBeforeReadingVisit() {
            givenRecord("CAP", "in_progress");
            givenStoredVisit(acuteVisit());

            service.updateExam(String.valueOf(VISIT_ID), new UpdateExamRequest());

            InOrder order = inOrder(medicalVisitRepository, medicalRecordRepository);
            order.verify(medicalVisitRepository).findRecordIdById(VISIT_ID);
            order.verify(medicalRecordRepository).findByIdForUpdate(RECORD_ID);
            order.verify(medicalVisitRepository).findById(VISIT_ID);
        }

        @Test
        @DisplayName("AC-13: id chữ hoặc không tồn tại trả 5008 và không khóa hồ sơ nào")
        void unknownExamIs5008WithoutLocking() {
            when(medicalVisitRepository.findRecordIdById(anyLong())).thenReturn(Optional.empty());

            assertCode(() -> service.updateExam("abc", new UpdateExamRequest()), ErrorCode.EXAM_NOT_FOUND);
            assertCode(() -> service.updateExam("999", new UpdateExamRequest()), ErrorCode.EXAM_NOT_FOUND);
            verifyNoInteractions(medicalRecordRepository);
        }

        @Test
        @DisplayName("AC-9, AC-15: hồ sơ đã đóng trả 4003 và không đọc phiên")
        void closedRecordIs4003BeforeReadingVisit() {
            givenRecord("CAP", "completed");
            givenStoredVisit(acuteVisit());

            assertCode(() -> service.updateExam(String.valueOf(VISIT_ID), new UpdateExamRequest()),
                    ErrorCode.MEDICAL_RECORD_ALREADY_COMPLETED);
            verify(medicalVisitRepository, never()).findById(anyLong());
        }

        @Test
        @DisplayName("AC-14: sot = false không kèm nhiệt độ thì xóa nhiệt độ đã lưu")
        void feverFalseClearsTemperature() {
            givenRecord("CAP", "in_progress");
            givenStoredVisit(acuteVisit().fever(true).feverTemperatureCelsius(new BigDecimal("39.0")));
            UpdateExamRequest req = new UpdateExamRequest();
            req.setFever(false);

            ExamResponse response = service.updateExam(String.valueOf(VISIT_ID), req);

            assertThat(response.fever()).isFalse();
            assertThat(response.feverTemperatureCelsius()).isNull();
        }

        @Test
        @DisplayName("AC-14: sot = false kèm nhiệt độ trả 2003")
        void feverFalseWithTemperatureIs2003() {
            givenRecord("CAP", "in_progress");
            givenStoredVisit(acuteVisit().fever(true).feverTemperatureCelsius(new BigDecimal("39.0")));
            UpdateExamRequest req = new UpdateExamRequest();
            req.setFever(false);
            req.setFeverTemperatureCelsius(new BigDecimal("38.5"));

            assertCode(() -> service.updateExam(String.valueOf(VISIT_ID), req), ErrorCode.INVALID_PARAMETER_VALUE);
        }

        @Test
        @DisplayName("AC-14: chỉ gửi nhiệt độ hợp lệ khi sot đã lưu là true, trả 2003 khi sot đã lưu không phải true")
        void temperatureAloneDependsOnStoredFever() {
            givenRecord("CAP", "in_progress");
            givenStoredVisit(acuteVisit().fever(true).feverTemperatureCelsius(new BigDecimal("39.0")));
            UpdateExamRequest req = new UpdateExamRequest();
            req.setFeverTemperatureCelsius(new BigDecimal("38.2"));

            assertThat(service.updateExam(String.valueOf(VISIT_ID), req).feverTemperatureCelsius())
                    .isEqualByComparingTo("38.2");

            givenStoredVisit(acuteVisit().fever(false));
            assertCode(() -> service.updateExam(String.valueOf(VISIT_ID), req), ErrorCode.INVALID_PARAMETER_VALUE);
        }

        @Test
        @DisplayName("AC-6: dòng cũ sai luật (ngày tương lai, loại lạ, sot true thiếu nhiệt độ) vẫn sửa được dan_do")
        void legacyRowStillEditableForUnrelatedFields() {
            givenRecord("CAP", "in_progress");
            givenStoredVisit(acuteVisit().visitDate(TODAY_VN.plusYears(1)).visitType("Cu").fever(true));
            UpdateExamRequest req = new UpdateExamRequest();
            req.setInstructions("Vẫn sửa được");

            assertThat(service.updateExam(String.valueOf(VISIT_ID), req).instructions()).isEqualTo("Vẫn sửa được");
        }

        @Test
        @DisplayName("AC-6: gửi ngay_kham sau ngay_hen_tai_kham đã lưu trả 2003 (kiểm tra trên giá trị cuối)")
        void visitDateIsComparedWithStoredFollowUp() {
            givenRecord("CAP", "in_progress");
            givenStoredVisit(acuteVisit().followUpDate(LocalDate.of(2026, 11, 12)));
            UpdateExamRequest req = new UpdateExamRequest();
            req.setVisitDate(LocalDate.of(2026, 11, 13));

            assertCode(() -> service.updateExam(String.valueOf(VISIT_ID), req), ErrorCode.INVALID_PARAMETER_VALUE);
        }

        @Test
        @DisplayName("AC-14: gửi lại đúng phòng hiện tại không tra phòng và chi nhánh")
        void sameRoomSkipsRoomLookup() {
            givenRecord("CAP", "in_progress");
            givenStoredVisit(acuteVisit().clinicRoomId(ROOM_ID));
            UpdateExamRequest req = new UpdateExamRequest();
            req.setClinicRoomId(String.valueOf(ROOM_ID));

            assertThat(service.updateExam(String.valueOf(VISIT_ID), req).clinicRoomId()).isEqualTo(String.valueOf(ROOM_ID));
            verifyNoInteractions(clinicRoomRepository);
        }

        @Test
        @DisplayName("AC-8: đổi sang phòng ở chi nhánh ngừng hoạt động trả 2106")
        void movingToInactiveBranchIs2106() {
            givenRecord("CAP", "in_progress");
            givenStoredVisit(acuteVisit());
            givenRoom(ROOM_ID, false);
            UpdateExamRequest req = new UpdateExamRequest();
            req.setClinicRoomId(String.valueOf(ROOM_ID));

            assertCode(() -> service.updateExam(String.valueOf(VISIT_ID), req), ErrorCode.BRANCH_INACTIVE);
        }

        @Test
        @DisplayName("AC-7, AC-13: ghi chú chuỗi rỗng xóa về NULL, trường null giữ nguyên")
        void blankClearsAndNullKeeps() {
            givenRecord("CAP", "in_progress");
            givenStoredVisit(acuteVisit().visitSummary("Cũ").instructions("Giữ").followUpDate(LocalDate.of(2026, 11, 30)));
            UpdateExamRequest req = new UpdateExamRequest();
            req.setVisitSummary(" ");

            ExamResponse response = service.updateExam(String.valueOf(VISIT_ID), req);

            assertThat(response.visitSummary()).isNull();
            assertThat(response.instructions()).isEqualTo("Giữ");
            assertThat(response.followUpDate()).isEqualTo("2026-11-30");
        }
    }

    @Nested
    @DisplayName("API 47, 48 đọc phiên")
    class Read {

        private MedicalVisit detailVisit(String type) {
            Employee employee = new Employee();
            employee.setPerson(Person.builder().personId(DOCTOR_PERSON).fullName("Bác sĩ A").build());
            MedicalVisit visit = acuteVisit().medicalVisitId(VISIT_ID).medicalRecordId(RECORD_ID)
                    .medicalRecordType(type).employee(employee).build();
            when(medicalVisitRepository.findDetailById(VISIT_ID)).thenReturn(Optional.of(visit));
            return visit;
        }

        @Test
        @DisplayName("AC-8: API 47 record_id thiếu trả 2001, không đọc hồ sơ")
        void listWithoutRecordIdIs2001() {
            assertCode(() -> service.listExams(null), ErrorCode.MISSING_REQUIRED_PARAMETER);
            verifyNoInteractions(medicalRecordRepository);
        }

        @Test
        @DisplayName("AC-12: bệnh nhân xem hồ sơ không tồn tại nhận 4001 trước 3106")
        void listChecksRecordBeforeOwner() {
            signInAs("benh_nhan", ACCOUNT_ID);
            when(medicalRecordRepository.findById(RECORD_ID)).thenReturn(Optional.empty());

            assertCode(() -> service.listExams(RECORD), ErrorCode.MEDICAL_RECORD_NOT_FOUND);
            verifyNoInteractions(userAccountRepository);
        }

        @Test
        @DisplayName("AC-12: bệnh nhân xem phiên trong hồ sơ người khác nhận 3106")
        void patientReadingOthersExamIs3106() {
            signInAs("benh_nhan", ACCOUNT_ID);
            givenRecord("CAP", "in_progress");
            detailVisit("CAP");
            when(userAccountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(
                    UserAccount.builder().accountId(ACCOUNT_ID).person(Person.builder().personId(99L).build()).build()));

            assertCode(() -> service.getExam(String.valueOf(VISIT_ID)), ErrorCode.CANNOT_ACCESS_ANOTHER_USER_DATA);
            verifyNoInteractions(acuteVisitRepository);
        }

        @Test
        @DisplayName("AC-12: bệnh nhân xem phiên của mình thành công, so ma_nguoi với ma_benh_nhan")
        void patientReadsOwnExam() {
            signInAs("benh_nhan", ACCOUNT_ID);
            givenRecord("CAP", "in_progress");
            detailVisit("CAP");
            when(userAccountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(
                    UserAccount.builder().accountId(ACCOUNT_ID).person(Person.builder().personId(PATIENT_ID).build()).build()));

            assertThat(service.getExam(String.valueOf(VISIT_ID)).getExamId()).isEqualTo(String.valueOf(VISIT_ID));
        }

        @Test
        @DisplayName("AC-11, AC-12: API 48 id không tồn tại trả 5008 trước mọi kiểm tra chủ hồ sơ")
        void detailNotFoundBeforeOwnerCheck() {
            signInAs("benh_nhan", ACCOUNT_ID);
            when(medicalVisitRepository.findDetailById(anyLong())).thenReturn(Optional.empty());

            assertCode(() -> service.getExam("999"), ErrorCode.EXAM_NOT_FOUND);
            assertCode(() -> service.getExam("abc"), ErrorCode.EXAM_NOT_FOUND);
            verifyNoInteractions(userAccountRepository);
        }

        @Test
        @DisplayName("AC-11: bác sĩ đọc chi tiết không bị kiểm tra chủ hồ sơ; phiên MAN đọc phienkhamman")
        void doctorSkipsOwnerCheckAndChronicReadsChronicSubtype() {
            detailVisit("MAN");

            service.getExam(String.valueOf(VISIT_ID));

            verify(chronicVisitRepository).findById(VISIT_ID);
            verifyNoInteractions(acuteVisitRepository, userAccountRepository);
            verify(medicalRecordRepository, never()).findById(anyLong());
        }
    }
}
