package com.example.drmayday.features.lesion.service;

import com.example.drmayday.common.exception.AppException;
import com.example.drmayday.common.exception.ErrorCode;
import com.example.drmayday.common.security.UserPrincipal;
import com.example.drmayday.entity.MedicalRecord;
import com.example.drmayday.entity.Person;
import com.example.drmayday.entity.SkinLesionEpisode;
import com.example.drmayday.entity.UserAccount;
import com.example.drmayday.features.lesion.dto.CreateLesionEpisodeRequest;
import com.example.drmayday.features.lesion.dto.LesionEpisodeResponse;
import com.example.drmayday.features.lesion.dto.UpdateLesionEpisodeRequest;
import com.example.drmayday.features.lesion.service.impl.LesionEpisodeServiceImpl;
import com.example.drmayday.repository.LesionLocationRepository;
import com.example.drmayday.repository.LesionPhotoRepository;
import com.example.drmayday.repository.MedicalRecordRepository;
import com.example.drmayday.repository.PatientRepository;
import com.example.drmayday.repository.SkinLesionEpisodeRepository;
import com.example.drmayday.repository.UserAccountRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LesionEpisodeServiceImplTest {

    @Mock private SkinLesionEpisodeRepository skinLesionEpisodeRepository;
    @Mock private MedicalRecordRepository medicalRecordRepository;
    @Mock private PatientRepository patientRepository;
    @Mock private LesionLocationRepository lesionLocationRepository;
    @Mock private LesionPhotoRepository lesionPhotoRepository;
    @Mock private UserAccountRepository userAccountRepository;

    @InjectMocks
    private LesionEpisodeServiceImpl lesionEpisodeService;

    private static final Long PATIENT_ID = 2L;
    private static final Long OTHER_PATIENT_ID = 99L;
    private static final Long CHRONIC_RECORD_ID = 10L;

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
    // API 87: createLesionEpisode
    // ==========================================

    @Test
    @DisplayName("API 87: Tao dot khong gan ho so, giu nguyen thoi_diem_xuat_hien=null neu khong gui (1001)")
    void createLesionEpisode_withoutRecordAndNullOnset() {
        setSecurityUser(50L, "0912345678", "benh_nhan");
        mockUserAccount(50L, PATIENT_ID);
        when(patientRepository.existsById(PATIENT_ID)).thenReturn(true);

        CreateLesionEpisodeRequest req = CreateLesionEpisodeRequest.builder()
                .tenDotTonThuongDa("  Đợt bùng phát sau ăn hải sản  ")
                .hoanCanhXuatHien("Sau an")
                .donViThoiGian("HOUR")
                .chiTietDotTonThuong("Ngứa nhiều")
                .build();

        when(skinLesionEpisodeRepository.saveAndFlush(any(SkinLesionEpisode.class))).thenAnswer(i -> {
            SkinLesionEpisode ep = i.getArgument(0);
            assertEquals(PATIENT_ID, ep.getPatientId());
            assertNull(ep.getChronicMedicalRecordId());
            assertNull(ep.getOnsetAt());
            assertEquals("hour", ep.getDurationUnit());
            ep.setSkinLesionEpisodeId(201L);
            return ep;
        });

        LesionEpisodeResponse res = lesionEpisodeService.createLesionEpisode(req);
        assertEquals("201", res.getEpisodeId());
        assertEquals("2", res.getPatientId());
        assertNull(res.getHoSoManId());
        assertNull(res.getThoiDiemXuatHien());
        assertEquals("Đợt bùng phát sau ăn hải sản", res.getTenDotTonThuongDa());
        assertEquals("hour", res.getDonViThoiGian());
    }

    @Test
    @DisplayName("API 87: Tao dot gan ho so MAN voi hoan canh 'Khác' chuan hoa 'Khac' (1001)")
    void createLesionEpisode_withChronicRecordAndOtherContext() {
        mockUserAccount(PATIENT_ID, PATIENT_ID);
        when(patientRepository.existsById(PATIENT_ID)).thenReturn(true);
        MedicalRecord record = MedicalRecord.builder()
                .medicalRecordId(CHRONIC_RECORD_ID).patientId(PATIENT_ID).medicalRecordType("MAN").build();
        when(medicalRecordRepository.findById(CHRONIC_RECORD_ID)).thenReturn(Optional.of(record));

        OffsetDateTime onset = OffsetDateTime.now().minusHours(2);
        CreateLesionEpisodeRequest req = CreateLesionEpisodeRequest.builder()
                .hoSoManId("10")
                .tenDotTonThuongDa("Đợt tái phát")
                .thoiDiemXuatHien(onset)
                .hoanCanhXuatHien("Khác")
                .hoanCanhXuatHienKhac("Đổi thời tiết")
                .donViThoiGian("day")
                .build();

        when(skinLesionEpisodeRepository.saveAndFlush(any(SkinLesionEpisode.class))).thenAnswer(i -> {
            SkinLesionEpisode ep = i.getArgument(0);
            ep.setSkinLesionEpisodeId(202L);
            return ep;
        });

        LesionEpisodeResponse res = lesionEpisodeService.createLesionEpisode(req);
        assertEquals("202", res.getEpisodeId());
        assertEquals("10", res.getHoSoManId());
        assertEquals("Khac", res.getHoanCanhXuatHien());
        assertEquals("Đổi thời tiết", res.getHoanCanhXuatHienKhac());
        assertEquals(onset, res.getThoiDiemXuatHien());
    }

    @Test
    @DisplayName("API 87: Unhappy - ten rong/null -> 7004, ho so CAP -> 2003, khong tim thay -> 4001, ho so nguoi khac -> 3106")
    void createLesionEpisode_validationAndOwnershipErrors() {
        // 7004 empty/null name
        assertEquals(ErrorCode.LESION_NAME_CANNOT_BE_EMPTY, assertThrows(AppException.class, () ->
                lesionEpisodeService.createLesionEpisode(null)).getErrorCode());
        assertEquals(ErrorCode.LESION_NAME_CANNOT_BE_EMPTY, assertThrows(AppException.class, () ->
                lesionEpisodeService.createLesionEpisode(CreateLesionEpisodeRequest.builder()
                        .tenDotTonThuongDa("   ").build())).getErrorCode());

        mockUserAccount(PATIENT_ID, PATIENT_ID);

        // 2003 non-numeric ho_so_man_id
        assertEquals(ErrorCode.INVALID_PARAMETER_VALUE, assertThrows(AppException.class, () ->
                lesionEpisodeService.createLesionEpisode(CreateLesionEpisodeRequest.builder()
                        .tenDotTonThuongDa("A").hoSoManId("abc").build())).getErrorCode());

        // 4001 missing ho_so_man_id
        when(medicalRecordRepository.findById(999L)).thenReturn(Optional.empty());
        assertEquals(ErrorCode.MEDICAL_RECORD_NOT_FOUND, assertThrows(AppException.class, () ->
                lesionEpisodeService.createLesionEpisode(CreateLesionEpisodeRequest.builder()
                        .tenDotTonThuongDa("A").hoSoManId("999").build())).getErrorCode());

        // 3106 other patient's record (checked even if that record is CAP!)
        MedicalRecord foreignCap = MedicalRecord.builder()
                .medicalRecordId(CHRONIC_RECORD_ID).patientId(OTHER_PATIENT_ID).medicalRecordType("CAP").build();
        when(medicalRecordRepository.findById(CHRONIC_RECORD_ID)).thenReturn(Optional.of(foreignCap));
        assertEquals(ErrorCode.CANNOT_ACCESS_ANOTHER_USER_DATA, assertThrows(AppException.class, () ->
                lesionEpisodeService.createLesionEpisode(CreateLesionEpisodeRequest.builder()
                        .tenDotTonThuongDa("A").hoSoManId("10").build())).getErrorCode());

        // 2003 own CAP record
        MedicalRecord ownCap = MedicalRecord.builder()
                .medicalRecordId(11L).patientId(PATIENT_ID).medicalRecordType("CAP").build();
        when(medicalRecordRepository.findById(11L)).thenReturn(Optional.of(ownCap));
        assertEquals(ErrorCode.INVALID_PARAMETER_VALUE, assertThrows(AppException.class, () ->
                lesionEpisodeService.createLesionEpisode(CreateLesionEpisodeRequest.builder()
                        .tenDotTonThuongDa("A").hoSoManId("11").build())).getErrorCode());
    }

    @Test
    @DisplayName("API 87: Rang buoc CHECK hoan canh, don vi thoi gian, do dai va benh nhan ton tai")
    void createLesionEpisode_constraintAndLengthGuards() {
        mockUserAccount(PATIENT_ID, PATIENT_ID);
        when(patientRepository.existsById(PATIENT_ID)).thenReturn(true);

        // Khac without explanation
        assertEquals(ErrorCode.INVALID_PARAMETER_VALUE, assertThrows(AppException.class, () ->
                lesionEpisodeService.createLesionEpisode(CreateLesionEpisodeRequest.builder()
                        .tenDotTonThuongDa("A").hoanCanhXuatHien("Khac").build())).getErrorCode());

        // Explanation provided when not Khac (violates ck_dottonthuongda_hoancanh)
        assertEquals(ErrorCode.INVALID_PARAMETER_VALUE, assertThrows(AppException.class, () ->
                lesionEpisodeService.createLesionEpisode(CreateLesionEpisodeRequest.builder()
                        .tenDotTonThuongDa("A").hoanCanhXuatHien("Sau an").hoanCanhXuatHienKhac("abc").build())).getErrorCode());

        // Invalid unit
        assertEquals(ErrorCode.INVALID_PARAMETER_VALUE, assertThrows(AppException.class, () ->
                lesionEpisodeService.createLesionEpisode(CreateLesionEpisodeRequest.builder()
                        .tenDotTonThuongDa("A").donViThoiGian("weeks").build())).getErrorCode());

        // Over max length
        assertEquals(ErrorCode.INVALID_PARAMETER_VALUE, assertThrows(AppException.class, () ->
                lesionEpisodeService.createLesionEpisode(CreateLesionEpisodeRequest.builder()
                        .tenDotTonThuongDa("x".repeat(256)).build())).getErrorCode());

        // Patient does not exist in DB
        when(patientRepository.existsById(PATIENT_ID)).thenReturn(false);
        assertEquals(ErrorCode.USER_ACCOUNT_NOT_FOUND, assertThrows(AppException.class, () ->
                lesionEpisodeService.createLesionEpisode(CreateLesionEpisodeRequest.builder()
                        .tenDotTonThuongDa("A").build())).getErrorCode());
    }

    // ==========================================
    // API 88: getMyLesionEpisodes
    // ==========================================

    @Test
    @DisplayName("API 88: Benh nhan loc theo ho so / xem tat ca; Bac si/Admin xem theo ho so hoac tat ca")
    void getMyLesionEpisodes_patientAndDoctorFlows() {
        mockUserAccount(PATIENT_ID, PATIENT_ID);
        SkinLesionEpisode ep1 = SkinLesionEpisode.builder().skinLesionEpisodeId(1L).patientId(PATIENT_ID)
                .chronicMedicalRecordId(CHRONIC_RECORD_ID).episodeName("Dot 1").build();
        when(skinLesionEpisodeRepository.findByPatientIdOrderByOnsetAtDesc(PATIENT_ID)).thenReturn(List.of(ep1));

        assertEquals(1, lesionEpisodeService.getMyLesionEpisodes(null).size());

        MedicalRecord record = MedicalRecord.builder().medicalRecordId(CHRONIC_RECORD_ID).patientId(PATIENT_ID).build();
        when(medicalRecordRepository.findById(CHRONIC_RECORD_ID)).thenReturn(Optional.of(record));
        when(skinLesionEpisodeRepository.findByPatientIdAndChronicMedicalRecordIdOrderByOnsetAtDesc(PATIENT_ID, CHRONIC_RECORD_ID))
                .thenReturn(List.of(ep1));
        assertEquals(1, lesionEpisodeService.getMyLesionEpisodes("10").size());

        // Doctor/Admin bypasses patient restriction
        setSecurityUser(3L, "0912345679", "bac_si");
        when(userAccountRepository.findById(3L)).thenReturn(Optional.empty());
        when(skinLesionEpisodeRepository.findByChronicMedicalRecordIdOrderByOnsetAtDesc(CHRONIC_RECORD_ID))
                .thenReturn(List.of(ep1));
        assertEquals(1, lesionEpisodeService.getMyLesionEpisodes("10").size());

        when(skinLesionEpisodeRepository.findAllOrderByOnsetAtDesc()).thenReturn(List.of(ep1));
        assertEquals(1, lesionEpisodeService.getMyLesionEpisodes("   ").size());
    }

    @Test
    @DisplayName("API 88: Unhappy - loc theo ho so nguoi khac (3106), khong ton tai (4001), sai kieu (2003)")
    void getMyLesionEpisodes_unhappy() {
        mockUserAccount(PATIENT_ID, PATIENT_ID);

        assertEquals(ErrorCode.INVALID_PARAMETER_VALUE, assertThrows(AppException.class, () ->
                lesionEpisodeService.getMyLesionEpisodes("abc")).getErrorCode());

        when(medicalRecordRepository.findById(999L)).thenReturn(Optional.empty());
        assertEquals(ErrorCode.MEDICAL_RECORD_NOT_FOUND, assertThrows(AppException.class, () ->
                lesionEpisodeService.getMyLesionEpisodes("999")).getErrorCode());

        MedicalRecord foreign = MedicalRecord.builder().medicalRecordId(CHRONIC_RECORD_ID).patientId(OTHER_PATIENT_ID).build();
        when(medicalRecordRepository.findById(CHRONIC_RECORD_ID)).thenReturn(Optional.of(foreign));
        assertEquals(ErrorCode.CANNOT_ACCESS_ANOTHER_USER_DATA, assertThrows(AppException.class, () ->
                lesionEpisodeService.getMyLesionEpisodes("10")).getErrorCode());
    }

    // ==========================================
    // API 89: getLesionEpisode
    // ==========================================

    @Test
    @DisplayName("API 89: Chu so huu va bac si xem chi tiet (1000); nguoi khac (3106), khong ton tai (7001)")
    void getLesionEpisode_paths() {
        SkinLesionEpisode ep = SkinLesionEpisode.builder().skinLesionEpisodeId(200L).patientId(PATIENT_ID).episodeName("A").build();
        when(skinLesionEpisodeRepository.findById(200L)).thenReturn(Optional.of(ep));
        mockUserAccount(PATIENT_ID, PATIENT_ID);
        assertEquals("200", lesionEpisodeService.getLesionEpisode("200").getEpisodeId());

        setSecurityUser(3L, "0912345679", "bac_si");
        assertEquals("200", lesionEpisodeService.getLesionEpisode("200").getEpisodeId());

        setSecurityUser(OTHER_PATIENT_ID, "0912000099", "patient");
        when(userAccountRepository.findById(OTHER_PATIENT_ID)).thenReturn(Optional.empty());
        assertEquals(ErrorCode.CANNOT_ACCESS_ANOTHER_USER_DATA, assertThrows(AppException.class, () ->
                lesionEpisodeService.getLesionEpisode("200")).getErrorCode());

        when(skinLesionEpisodeRepository.findById(999L)).thenReturn(Optional.empty());
        assertEquals(ErrorCode.LESION_EPISODE_NOT_FOUND, assertThrows(AppException.class, () ->
                lesionEpisodeService.getLesionEpisode("999")).getErrorCode());
        assertEquals(ErrorCode.INVALID_PARAMETER_VALUE, assertThrows(AppException.class, () ->
                lesionEpisodeService.getLesionEpisode("abc")).getErrorCode());
    }

    // ==========================================
    // API 90: editLesionEpisode
    // ==========================================

    @Test
    @DisplayName("API 90: Cap nhat mo ta, chuyen doi hoan_canh Khac <-> binh thuong an toan")
    void editLesionEpisode_contextTransitionsAndPartialUpdates() {
        mockUserAccount(PATIENT_ID, PATIENT_ID);
        SkinLesionEpisode ep = SkinLesionEpisode.builder()
                .skinLesionEpisodeId(200L)
                .patientId(PATIENT_ID)
                .episodeName("Ten cu")
                .onsetContext("Khac")
                .otherOnsetContext("Ly do cu")
                .build();
        when(skinLesionEpisodeRepository.findById(200L)).thenReturn(Optional.of(ep));
        when(skinLesionEpisodeRepository.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));

        // 1. Update ONLY hoan_canh_xuat_hien_khac without resending hoan_canh_xuat_hien
        LesionEpisodeResponse r1 = lesionEpisodeService.editLesionEpisode("200",
                UpdateLesionEpisodeRequest.builder().hoanCanhXuatHienKhac("Ly do moi").build());
        assertEquals("Khac", r1.getHoanCanhXuatHien());
        assertEquals("Ly do moi", r1.getHoanCanhXuatHienKhac());
        assertEquals("Ten cu", r1.getTenDotTonThuongDa());

        // 2. Switch hoan_canh_xuat_hien to non-Khac -> otherOnsetContext cleared to null
        LesionEpisodeResponse r2 = lesionEpisodeService.editLesionEpisode("200",
                UpdateLesionEpisodeRequest.builder().hoanCanhXuatHien("Sau an").build());
        assertEquals("Sau an", r2.getHoanCanhXuatHien());
        assertNull(r2.getHoanCanhXuatHienKhac());

        // 3. Cannot update hoan_canh_xuat_hien_khac alone while context != Khac
        assertEquals(ErrorCode.INVALID_PARAMETER_VALUE, assertThrows(AppException.class, () ->
                lesionEpisodeService.editLesionEpisode("200",
                        UpdateLesionEpisodeRequest.builder().hoanCanhXuatHienKhac("abc").build())).getErrorCode());
    }

    @Test
    @DisplayName("API 90: Unhappy - ten rong (7004), khong ton tai (7001), cua nguoi khac (3106), don vi sai (2003)")
    void editLesionEpisode_errors() {
        mockUserAccount(PATIENT_ID, PATIENT_ID);
        SkinLesionEpisode ep = SkinLesionEpisode.builder()
                .skinLesionEpisodeId(200L).patientId(PATIENT_ID).episodeName("Ten cu").build();
        when(skinLesionEpisodeRepository.findById(200L)).thenReturn(Optional.of(ep));

        assertEquals(ErrorCode.LESION_NAME_CANNOT_BE_EMPTY, assertThrows(AppException.class, () ->
                lesionEpisodeService.editLesionEpisode("200",
                        UpdateLesionEpisodeRequest.builder().tenDotTonThuongDa("  ").build())).getErrorCode());

        assertEquals(ErrorCode.INVALID_PARAMETER_VALUE, assertThrows(AppException.class, () ->
                lesionEpisodeService.editLesionEpisode("200",
                        UpdateLesionEpisodeRequest.builder().donViThoiGian("year").build())).getErrorCode());

        when(skinLesionEpisodeRepository.findById(999L)).thenReturn(Optional.empty());
        assertEquals(ErrorCode.LESION_EPISODE_NOT_FOUND, assertThrows(AppException.class, () ->
                lesionEpisodeService.editLesionEpisode("999",
                        UpdateLesionEpisodeRequest.builder().chiTietDotTonThuong("x").build())).getErrorCode());

        SkinLesionEpisode other = SkinLesionEpisode.builder().skinLesionEpisodeId(201L).patientId(OTHER_PATIENT_ID).build();
        when(skinLesionEpisodeRepository.findById(201L)).thenReturn(Optional.of(other));
        assertEquals(ErrorCode.CANNOT_ACCESS_ANOTHER_USER_DATA, assertThrows(AppException.class, () ->
                lesionEpisodeService.editLesionEpisode("201",
                        UpdateLesionEpisodeRequest.builder().tenDotTonThuongDa("x").build())).getErrorCode());
    }

    // ==========================================
    // API 91: deleteLesionEpisode
    // ==========================================

    @Test
    @DisplayName("API 91: Xoa dot rong (1000) va chan neu con vi tri/anh con hoac FK flush (7009)")
    void deleteLesionEpisode_paths() {
        mockUserAccount(PATIENT_ID, PATIENT_ID);
        SkinLesionEpisode ep = SkinLesionEpisode.builder().skinLesionEpisodeId(200L).patientId(PATIENT_ID).build();
        when(skinLesionEpisodeRepository.findById(200L)).thenReturn(Optional.of(ep));

        // Has child location -> 7009
        when(lesionLocationRepository.existsBySkinLesionEpisodeId(200L)).thenReturn(true);
        assertEquals(ErrorCode.EPISODE_HAS_CHILD_DATA, assertThrows(AppException.class, () ->
                lesionEpisodeService.deleteLesionEpisode("200")).getErrorCode());

        // Has child photo -> 7009
        when(lesionLocationRepository.existsBySkinLesionEpisodeId(200L)).thenReturn(false);
        when(lesionPhotoRepository.existsBySkinLesionEpisodeId(200L)).thenReturn(true);
        assertEquals(ErrorCode.EPISODE_HAS_CHILD_DATA, assertThrows(AppException.class, () ->
                lesionEpisodeService.deleteLesionEpisode("200")).getErrorCode());

        // Concurrent FK flush failure -> 7009
        when(lesionPhotoRepository.existsBySkinLesionEpisodeId(200L)).thenReturn(false);
        doThrow(new DataIntegrityViolationException("fk_vitritonthuong_dottonthuongda")).when(skinLesionEpisodeRepository).flush();
        assertEquals(ErrorCode.EPISODE_HAS_CHILD_DATA, assertThrows(AppException.class, () ->
                lesionEpisodeService.deleteLesionEpisode("200")).getErrorCode());

        // Clean delete -> 1000
        doNothing().when(skinLesionEpisodeRepository).flush();
        assertDoesNotThrow(() -> lesionEpisodeService.deleteLesionEpisode("200"));

        // Missing -> 7001, other patient -> 3106
        when(skinLesionEpisodeRepository.findById(999L)).thenReturn(Optional.empty());
        assertEquals(ErrorCode.LESION_EPISODE_NOT_FOUND, assertThrows(AppException.class, () ->
                lesionEpisodeService.deleteLesionEpisode("999")).getErrorCode());

        SkinLesionEpisode other = SkinLesionEpisode.builder().skinLesionEpisodeId(201L).patientId(OTHER_PATIENT_ID).build();
        when(skinLesionEpisodeRepository.findById(201L)).thenReturn(Optional.of(other));
        assertEquals(ErrorCode.CANNOT_ACCESS_ANOTHER_USER_DATA, assertThrows(AppException.class, () ->
                lesionEpisodeService.deleteLesionEpisode("201")).getErrorCode());
    }
}
