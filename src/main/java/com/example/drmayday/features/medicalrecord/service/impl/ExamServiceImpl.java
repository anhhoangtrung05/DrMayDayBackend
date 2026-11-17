package com.example.drmayday.features.medicalrecord.service.impl;

import com.example.drmayday.common.exception.AppException;
import com.example.drmayday.common.exception.ErrorCode;
import com.example.drmayday.common.security.SecurityUtils;
import com.example.drmayday.entity.AcuteVisit;
import com.example.drmayday.entity.ChronicVisit;
import com.example.drmayday.entity.ClinicRoom;
import com.example.drmayday.entity.MedicalRecord;
import com.example.drmayday.entity.MedicalVisit;
import com.example.drmayday.entity.Person;
import com.example.drmayday.entity.UserAccount;
import com.example.drmayday.features.medicalrecord.dto.AcuteExamDetailResponse;
import com.example.drmayday.features.medicalrecord.dto.AcuteExamFindingsDto;
import com.example.drmayday.features.medicalrecord.dto.ChronicExamDetailResponse;
import com.example.drmayday.features.medicalrecord.dto.ChronicExamFindingsDto;
import com.example.drmayday.features.medicalrecord.dto.CreateExamRequest;
import com.example.drmayday.features.medicalrecord.dto.ExamDetailResponse;
import com.example.drmayday.features.medicalrecord.dto.ExamResponse;
import com.example.drmayday.features.medicalrecord.dto.ExamSummaryResponse;
import com.example.drmayday.features.medicalrecord.dto.UpdateExamRequest;
import com.example.drmayday.features.medicalrecord.service.ExamService;
import com.example.drmayday.repository.AcuteVisitRepository;
import com.example.drmayday.repository.ChronicVisitRepository;
import com.example.drmayday.repository.ClinicRoomRepository;
import com.example.drmayday.repository.EmployeeRepository;
import com.example.drmayday.repository.MedicalRecordRepository;
import com.example.drmayday.repository.MedicalVisitRepository;
import com.example.drmayday.repository.UserAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class ExamServiceImpl implements ExamService {

    private static final Logger log = LoggerFactory.getLogger(ExamServiceImpl.class);

    private static final String TYPE_ACUTE = "CAP";
    private static final String VISIT_FIRST = "Kham lan dau";
    private static final String VISIT_FOLLOW_UP = "Tai kham";
    private static final Set<String> VISIT_TYPES = Set.of(VISIT_FIRST, VISIT_FOLLOW_UP);
    private static final Set<String> CLOSED_STATUSES = Set.of("completed", "cancelled");
    private static final BigDecimal MIN_TEMPERATURE = new BigDecimal("30.0");
    private static final BigDecimal MAX_TEMPERATURE = new BigDecimal("45.0");
    private static final String ROOM_FOREIGN_KEY = "fk_phienkham_phong";

    private final MedicalVisitRepository medicalVisitRepository;
    private final AcuteVisitRepository acuteVisitRepository;
    private final ChronicVisitRepository chronicVisitRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final ClinicRoomRepository clinicRoomRepository;
    private final EmployeeRepository employeeRepository;
    private final UserAccountRepository userAccountRepository;
    private final RecordOwnerCheck recordOwnerCheck;
    private final Clock clock;

    public ExamServiceImpl(MedicalVisitRepository medicalVisitRepository,
                           AcuteVisitRepository acuteVisitRepository,
                           ChronicVisitRepository chronicVisitRepository,
                           MedicalRecordRepository medicalRecordRepository,
                           ClinicRoomRepository clinicRoomRepository,
                           EmployeeRepository employeeRepository,
                           UserAccountRepository userAccountRepository,
                           RecordOwnerCheck recordOwnerCheck,
                           Clock clock) {
        this.medicalVisitRepository = medicalVisitRepository;
        this.acuteVisitRepository = acuteVisitRepository;
        this.chronicVisitRepository = chronicVisitRepository;
        this.medicalRecordRepository = medicalRecordRepository;
        this.clinicRoomRepository = clinicRoomRepository;
        this.employeeRepository = employeeRepository;
        this.userAccountRepository = userAccountRepository;
        this.recordOwnerCheck = recordOwnerCheck;
        this.clock = clock;
    }

    @Override
    @Transactional
    public ExamResponse createExam(CreateExamRequest req) {
        Long recordId = parseRecordIdValue(req.getRecordId());
        // Khóa hồ sơ trước mọi thao tác: số lần khám tự tính không trùng, và lần đóng hồ sơ song song không lọt qua
        MedicalRecord record = lockWritableRecord(recordId);

        LocalDate today = LocalDate.now(clock);
        LocalDate visitDate = req.getVisitDate() != null ? req.getVisitDate() : today;
        Integer requestedNumber = req.getVisitNumber();
        if (requestedNumber != null && requestedNumber <= 0) {
            throw invalidValue();
        }
        validateVisitType(req.getVisitType());
        validateVisitDate(visitDate, today);
        validateFollowUpDate(visitDate, req.getFollowUpDate());
        validateTemperature(req.getFever(), req.getFeverTemperatureCelsius());

        Long roomId = req.getClinicRoomId() != null ? requireUsableRoom(req.getClinicRoomId()) : null;

        int visitNumber;
        if (requestedNumber != null) {
            if (medicalVisitRepository.existsByMedicalRecordIdAndVisitNumber(recordId, requestedNumber)) {
                throw new AppException(ErrorCode.EXAM_SEQUENCE_DUPLICATE);
            }
            visitNumber = requestedNumber;
        } else {
            visitNumber = medicalVisitRepository.findMaxVisitNumber(recordId) + 1;
        }
        String visitType = req.getVisitType() != null
                ? req.getVisitType()
                : (visitNumber == 1 ? VISIT_FIRST : VISIT_FOLLOW_UP);

        MedicalVisit visit = MedicalVisit.builder()
                .medicalRecordId(recordId)
                .medicalRecordType(record.getMedicalRecordType())
                .physicianId(currentPhysicianId())
                .clinicRoomId(roomId)
                .visitDate(visitDate)
                .visitNumber(visitNumber)
                .visitType(visitType)
                .serumSampleStored(req.getSerumSampleStored() != null ? req.getSerumSampleStored() : Boolean.FALSE)
                .fever(req.getFever())
                .feverTemperatureCelsius(req.getFeverTemperatureCelsius())
                .followUpDate(req.getFollowUpDate())
                .visitSummary(blankToNull(req.getVisitSummary()))
                .nextVisitManagement(blankToNull(req.getNextVisitManagement()))
                .instructions(blankToNull(req.getInstructions()))
                .build();

        MedicalVisit saved;
        try {
            saved = medicalVisitRepository.saveAndFlush(visit);
            // Khóa ngoại hai chiều giữa phiên và subtype chỉ kiểm tra lúc commit, nên chèn subtype rỗng ngay trong giao dịch này
            Long visitId = saved.getMedicalVisitId();
            if (TYPE_ACUTE.equals(saved.getMedicalRecordType())) {
                acuteVisitRepository.saveAndFlush(AcuteVisit.builder().medicalVisitId(visitId).build());
            } else {
                chronicVisitRepository.saveAndFlush(ChronicVisit.builder().medicalVisitId(visitId).build());
            }
        } catch (DataIntegrityViolationException ex) {
            throw translateWriteError(ex);
        }

        log.info("Tạo phiên khám: examId={}, recordId={}, lanKham={}, taiKhoan={}",
                saved.getMedicalVisitId(), recordId, visitNumber, SecurityUtils.getCurrentUserId());
        // Dựng từ entity phiên: cột sinh loai_benh_an của subtype vẫn NULL trong bộ nhớ sau khi flush
        return ExamResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExamSummaryResponse> listExams(String recordIdValue) {
        Long recordId = parseRecordIdValue(recordIdValue);
        MedicalRecord record = medicalRecordRepository.findById(recordId)
                .orElseThrow(() -> new AppException(ErrorCode.MEDICAL_RECORD_NOT_FOUND));
        // Kiểm tra 4001 trước 3106, theo đúng thứ tự của đặc tả
        if (SecurityUtils.hasRole("BENH_NHAN")) {
            recordOwnerCheck.assertCurrentPatientOwns(record.getPatientId());
        }
        return medicalVisitRepository.findByRecordWithDoctorAndRoom(recordId).stream()
                .map(ExamSummaryResponse::new)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ExamDetailResponse getExam(String id) {
        MedicalVisit visit = medicalVisitRepository.findDetailById(parseExamId(id))
                .orElseThrow(ExamServiceImpl::examNotFound);
        // Kiểm tra 5008 trước 3106, theo đúng thứ tự của đặc tả
        if (SecurityUtils.hasRole("BENH_NHAN")) {
            MedicalRecord record = medicalRecordRepository.findById(visit.getMedicalRecordId())
                    .orElseThrow(ExamServiceImpl::examNotFound);
            recordOwnerCheck.assertCurrentPatientOwns(record.getPatientId());
        }
        Long visitId = visit.getMedicalVisitId();
        if (TYPE_ACUTE.equals(visit.getMedicalRecordType())) {
            return new AcuteExamDetailResponse(visit,
                    AcuteExamFindingsDto.from(acuteVisitRepository.findById(visitId).orElse(null)));
        }
        return new ChronicExamDetailResponse(visit,
                ChronicExamFindingsDto.from(chronicVisitRepository.findById(visitId).orElse(null)));
    }

    @Override
    @Transactional
    public ExamResponse updateExam(String id, UpdateExamRequest req) {
        Long visitId = parseExamId(id);
        Long recordId = medicalVisitRepository.findRecordIdById(visitId).orElseThrow(ExamServiceImpl::examNotFound);
        // Khóa hồ sơ trước rồi mới đọc phiên, cùng thứ tự với API 46 và API 38, để không deadlock
        lockWritableRecord(recordId);
        MedicalVisit visit = medicalVisitRepository.findById(visitId).orElseThrow(ExamServiceImpl::examNotFound);

        // Gộp giá trị gửi lên với giá trị hiện tại; luật nào chỉ chạy khi request gửi ít nhất một trường của luật đó
        LocalDate visitDate = valueOrCurrent(req.getVisitDate(), visit.getVisitDate());
        LocalDate followUpDate = valueOrCurrent(req.getFollowUpDate(), visit.getFollowUpDate());
        Boolean fever = valueOrCurrent(req.getFever(), visit.getFever());
        BigDecimal temperature;
        if (req.getFeverTemperatureCelsius() != null) {
            temperature = req.getFeverTemperatureCelsius();
        } else if (Boolean.FALSE.equals(req.getFever())) {
            // Gửi sot = false mà không gửi nhiệt độ thì xóa nhiệt độ đã lưu
            temperature = null;
        } else {
            temperature = visit.getFeverTemperatureCelsius();
        }

        if (req.getVisitType() != null) {
            validateVisitType(req.getVisitType());
        }
        if (req.getVisitDate() != null) {
            validateVisitDate(visitDate, LocalDate.now(clock));
        }
        if (req.getVisitDate() != null || req.getFollowUpDate() != null) {
            validateFollowUpDate(visitDate, followUpDate);
        }
        if (req.getFever() != null || req.getFeverTemperatureCelsius() != null) {
            validateTemperature(fever, temperature);
        }

        if (req.getClinicRoomId() != null) {
            Long roomId = parseRoomId(req.getClinicRoomId());
            // Gửi lại đúng phòng hiện tại không tính là đổi phòng, nên không kiểm tra lại chi nhánh
            if (!roomId.equals(visit.getClinicRoomId())) {
                requireUsableRoom(roomId);
                visit.setClinicRoomId(roomId);
            }
        }

        visit.setVisitDate(visitDate);
        visit.setFollowUpDate(followUpDate);
        visit.setVisitType(valueOrCurrent(req.getVisitType(), visit.getVisitType()));
        visit.setSerumSampleStored(valueOrCurrent(req.getSerumSampleStored(), visit.getSerumSampleStored()));
        visit.setFever(fever);
        visit.setFeverTemperatureCelsius(temperature);
        // Ghi chú gửi chuỗi rỗng hoặc toàn khoảng trắng thì xóa về NULL
        if (req.getVisitSummary() != null) {
            visit.setVisitSummary(blankToNull(req.getVisitSummary()));
        }
        if (req.getNextVisitManagement() != null) {
            visit.setNextVisitManagement(blankToNull(req.getNextVisitManagement()));
        }
        if (req.getInstructions() != null) {
            visit.setInstructions(blankToNull(req.getInstructions()));
        }

        MedicalVisit saved;
        try {
            saved = medicalVisitRepository.saveAndFlush(visit);
        } catch (DataIntegrityViolationException ex) {
            throw translateWriteError(ex);
        }

        log.info("Sửa phiên khám: examId={}, recordId={}, taiKhoan={}",
                saved.getMedicalVisitId(), recordId, SecurityUtils.getCurrentUserId());
        return ExamResponse.from(saved);
    }

    // FOR UPDATE phải là lần đọc hồ sơ đầu tiên trong giao dịch, để thấy bản mới nhất sau khi chờ khóa
    private MedicalRecord lockWritableRecord(Long recordId) {
        MedicalRecord record = medicalRecordRepository.findByIdForUpdate(recordId)
                .orElseThrow(() -> new AppException(ErrorCode.MEDICAL_RECORD_NOT_FOUND));
        if (CLOSED_STATUSES.contains(record.getStatus())) {
            throw new AppException(ErrorCode.MEDICAL_RECORD_ALREADY_COMPLETED);
        }
        return record;
    }

    // ma_bac_si luôn là người gọi, kể cả Admin. Token mang ma_tai_khoan nên phải đọc taikhoan để lấy ma_nguoi
    private Long currentPhysicianId() {
        UserAccount account = userAccountRepository.findById(SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> new AppException(ErrorCode.CANNOT_ACCESS_ANOTHER_USER_DATA));
        Person person = account.getPerson();
        if (person == null) {
            throw new AppException(ErrorCode.CANNOT_ACCESS_ANOTHER_USER_DATA);
        }
        if (!employeeRepository.existsById(person.getPersonId())) {
            throw new AppException(ErrorCode.DOCTOR_ROLE_REQUIRED);
        }
        return person.getPersonId();
    }

    private Long requireUsableRoom(String value) {
        Long roomId = parseRoomId(value);
        requireUsableRoom(roomId);
        return roomId;
    }

    private void requireUsableRoom(Long roomId) {
        ClinicRoom room = clinicRoomRepository.findByIdWithBranch(roomId)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));
        if (!Boolean.TRUE.equals(room.getBranch().getActive())) {
            throw new AppException(ErrorCode.BRANCH_INACTIVE);
        }
    }

    private static void validateVisitType(String visitType) {
        if (visitType != null && !VISIT_TYPES.contains(visitType)) {
            throw invalidValue();
        }
    }

    private static void validateVisitDate(LocalDate visitDate, LocalDate today) {
        if (visitDate.isAfter(today)) {
            throw invalidValue();
        }
    }

    private static void validateFollowUpDate(LocalDate visitDate, LocalDate followUpDate) {
        if (followUpDate != null && followUpDate.isBefore(visitDate)) {
            throw invalidValue();
        }
    }

    // Nhiệt độ có khi và chỉ khi sot = true, trong [30,0; 45,0], tối đa một chữ số thập phân
    private static void validateTemperature(Boolean fever, BigDecimal temperature) {
        if (!Boolean.TRUE.equals(fever)) {
            if (temperature != null) {
                throw invalidValue();
            }
            return;
        }
        if (temperature == null
                || temperature.compareTo(MIN_TEMPERATURE) < 0
                || temperature.compareTo(MAX_TEMPERATURE) > 0
                || temperature.stripTrailingZeros().scale() > 1) {
            throw invalidValue();
        }
    }

    private AppException translateWriteError(DataIntegrityViolationException ex) {
        // Với khóa dòng hồ sơ, trùng lan_kham_thu chỉ là lưới an toàn
        if (MedicalRecordSqlErrors.isUniqueViolation(ex)) {
            return new AppException(ErrorCode.EXAM_SEQUENCE_DUPLICATE);
        }
        // Phòng bị xóa giữa lúc kiểm tra và lúc ghi
        if (MedicalRecordSqlErrors.isForeignKeyViolationOn(ex, ROOM_FOREIGN_KEY)) {
            return new AppException(ErrorCode.ROOM_NOT_FOUND);
        }
        if (MedicalRecordSqlErrors.isInvalidValue(ex)) {
            log.warn("Dữ liệu phiên khám vi phạm ràng buộc database: {}", ex.getMostSpecificCause().getMessage());
            return invalidValue();
        }
        throw ex;
    }

    // record_id thiếu hoặc rỗng là 2001, không phải số nguyên dương là 2002
    private static Long parseRecordIdValue(String value) {
        if (value == null || value.isBlank()) {
            throw new AppException(ErrorCode.MISSING_REQUIRED_PARAMETER);
        }
        return parsePositiveId(value);
    }

    private static Long parseRoomId(String value) {
        if (value.isBlank()) {
            throw new AppException(ErrorCode.INVALID_PARAMETER_TYPE);
        }
        return parsePositiveId(value);
    }

    private static Long parsePositiveId(String value) {
        long id;
        try {
            id = Long.parseLong(value.trim());
        } catch (NumberFormatException ex) {
            throw new AppException(ErrorCode.INVALID_PARAMETER_TYPE);
        }
        if (id <= 0) {
            throw new AppException(ErrorCode.INVALID_PARAMETER_TYPE);
        }
        return id;
    }

    // Id phiên trên đường dẫn không phải số thì coi như không tồn tại
    private static Long parseExamId(String id) {
        try {
            return Long.parseLong(id);
        } catch (NumberFormatException ex) {
            throw examNotFound();
        }
    }

    private static AppException examNotFound() {
        return new AppException(ErrorCode.EXAM_NOT_FOUND);
    }

    private static AppException invalidValue() {
        return new AppException(ErrorCode.INVALID_PARAMETER_VALUE);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static <T> T valueOrCurrent(T sent, T current) {
        return sent != null ? sent : current;
    }
}
