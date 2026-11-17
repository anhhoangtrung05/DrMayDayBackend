package com.example.drmayday.features.medicalrecord.service.impl;

import com.example.drmayday.common.exception.AppException;
import com.example.drmayday.common.exception.ErrorCode;
import com.example.drmayday.common.security.UserPrincipal;
import com.example.drmayday.entity.AllergyOnsetContext;
import com.example.drmayday.entity.InfectionStatus;
import com.example.drmayday.entity.MedicalRecord;
import com.example.drmayday.entity.PreviousDiseaseEpisode;
import com.example.drmayday.features.medicalrecord.dto.AllergyContextResponse;
import com.example.drmayday.features.medicalrecord.dto.UpsertAllergyContextRequest;
import com.example.drmayday.features.medicalrecord.dto.UpsertInfectionStatusRequest;
import com.example.drmayday.features.medicalrecord.dto.UpsertPreviousEpisodesRequest;
import com.example.drmayday.repository.AllergyOnsetContextRepository;
import com.example.drmayday.repository.InfectionStatusRepository;
import com.example.drmayday.repository.MedicalRecordRepository;
import com.example.drmayday.repository.PreviousDiseaseEpisodeRepository;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Khóa luật của API 43 đến 45 (spec 0003): thứ tự kiểm tra, luật cờ và ghi chú, danh mục, nhóm triệu chứng,
 * ghi đè toàn bộ trên entity đang quản lý, và cách đổi lỗi database. Request được dựng từ JSON thật qua Jackson,
 * để tên trường, any setter và ép kiểu chạy đúng như lúc chạy thật. Chỉ giả lập repository.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MedicalHistoryServiceImplTest {

    private static final long RECORD_ID = 10L;
    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Mock private MedicalRecordRepository medicalRecordRepository;
    @Mock private AllergyOnsetContextRepository allergyContextRepository;
    @Mock private PreviousDiseaseEpisodeRepository previousEpisodeRepository;
    @Mock private InfectionStatusRepository infectionStatusRepository;

    private MedicalHistoryServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MedicalHistoryServiceImpl(medicalRecordRepository, allergyContextRepository,
                previousEpisodeRepository, infectionStatusRepository);
        UserPrincipal principal = UserPrincipal.create(6L, "0900000006", "Bác sĩ", "bac_si");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        givenRecord("MAN", "draft");
        when(allergyContextRepository.findByMedicalRecordId(RECORD_ID)).thenReturn(Optional.empty());
        when(previousEpisodeRepository.findByMedicalRecordId(RECORD_ID)).thenReturn(Optional.empty());
        when(infectionStatusRepository.findByMedicalRecordId(RECORD_ID)).thenReturn(Optional.empty());
        when(allergyContextRepository.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));
        when(previousEpisodeRepository.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));
        when(infectionStatusRepository.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));
    }

    @AfterEach
    void signOut() {
        SecurityContextHolder.clearContext();
    }

    private void givenRecord(String type, String status) {
        when(medicalRecordRepository.findByIdForUpdate(RECORD_ID)).thenReturn(Optional.of(MedicalRecord.builder()
                .medicalRecordId(RECORD_ID).patientId(2L).medicalRecordType(type).status(status).build()));
    }

    private static UpsertAllergyContextRequest allergy(String fields) {
        return JSON.readValue(body(fields), UpsertAllergyContextRequest.class);
    }

    private static UpsertPreviousEpisodesRequest episodes(String fields) {
        return JSON.readValue(body(fields), UpsertPreviousEpisodesRequest.class);
    }

    private static UpsertInfectionStatusRequest infection(String fields) {
        return JSON.readValue(body(fields), UpsertInfectionStatusRequest.class);
    }

    private static String body(String fields) {
        return "{\"record_id\":\"" + RECORD_ID + "\"" + (fields.isEmpty() ? "" : "," + fields) + "}";
    }

    private static void assertCode(ThrowingCallable call, ErrorCode code) {
        assertThatThrownBy(call).isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode()).isEqualTo(code);
    }

    private static DataIntegrityViolationException dbError(String sqlState) {
        return new DataIntegrityViolationException("lỗi ghi", new SQLException("lỗi gốc", sqlState));
    }

    @Nested
    @DisplayName("Thứ tự kiểm tra chung")
    class CheckOrder {

        @Test
        @DisplayName("AC-8: trường lạ trả 2003 trước khi đụng tới database")
        void unknownFieldIsRejectedBeforeAnyRead() {
            assertCode(() -> service.upsertAllergyContext(allergy("\"thuc_an_nghi_ngoo\":\"Tôm\"")),
                    ErrorCode.INVALID_PARAMETER_VALUE);
            verifyNoInteractions(medicalRecordRepository, allergyContextRepository);
        }

        @Test
        @DisplayName("AC-8: gửi ma_ho_so hay khóa chính cũng là trường lạ")
        void keysAreUnknownFields() {
            assertCode(() -> service.upsertAllergyContext(allergy("\"ma_ho_so\":\"1\"")), ErrorCode.INVALID_PARAMETER_VALUE);
            assertCode(() -> service.upsertInfectionStatus(infection("\"ma_tinh_trang_nhiem_trung\":\"1\"")),
                    ErrorCode.INVALID_PARAMETER_VALUE);
        }

        @ParameterizedTest(name = "AC-8: record_id \"{0}\" trả 2002 mà không đọc database")
        @ValueSource(strings = {"abc", "1.5", "99999999999999999999"})
        void nonIntegerRecordIdReturns2002(String recordId) {
            UpsertAllergyContextRequest req = allergy("");
            req.setRecordId(recordId);

            assertCode(() -> service.upsertAllergyContext(req), ErrorCode.INVALID_PARAMETER_TYPE);
            verifyNoInteractions(medicalRecordRepository);
        }

        @Test
        @DisplayName("AC-8: record_id có khoảng trắng hai đầu vẫn được nhận")
        void recordIdIsTrimmed() {
            UpsertAllergyContextRequest req = allergy("");
            req.setRecordId("  10  ");

            assertThat(service.upsertAllergyContext(req).recordId()).isEqualTo("10");
        }

        @Test
        @DisplayName("AC-8: hồ sơ không tồn tại trả 4001")
        void missingRecordReturns4001() {
            when(medicalRecordRepository.findByIdForUpdate(RECORD_ID)).thenReturn(Optional.empty());

            assertCode(() -> service.upsertAllergyContext(allergy("")), ErrorCode.MEDICAL_RECORD_NOT_FOUND);
        }

        @Test
        @DisplayName("AC-9: sai loại hồ sơ (4008) được báo trước hồ sơ đóng (4003)")
        void typeMismatchComesBeforeClosedRecord() {
            givenRecord("CAP", "completed");

            assertCode(() -> service.upsertPreviousEpisodes(episodes("")), ErrorCode.RECORD_TYPE_MISMATCH);
        }

        @ParameterizedTest(name = "AC-9: hồ sơ {0} trả 4003 trước khi kiểm tra giá trị")
        @ValueSource(strings = {"completed", "cancelled"})
        void closedRecordComesBeforeValueRules(String status) {
            givenRecord("MAN", status);

            assertCode(() -> service.upsertAllergyContext(allergy("\"thuc_an_tung_bi\":true")),
                    ErrorCode.MEDICAL_RECORD_ALREADY_COMPLETED);
            verify(allergyContextRepository, never()).saveAndFlush(any());
        }

        @ParameterizedTest(name = "AC-9: hồ sơ mở {0} vẫn ghi được")
        @ValueSource(strings = {"draft", "pending", "in_progress"})
        void openRecordsAreWritable(String status) {
            givenRecord("MAN", status);

            assertThat(service.upsertAllergyContext(allergy(""))).isNotNull();
        }
    }

    @Nested
    @DisplayName("Tạo mới và ghi đè toàn bộ")
    class CreateOrOverwrite {

        @Test
        @DisplayName("AC-1: chưa có dòng thì tạo entity mới chỉ với ma_ho_so, khóa chính để database sinh")
        void createsNewRowWhenMissing() {
            service.upsertAllergyContext(allergy("\"thuoc_nghi_ngo\":\"Penicillin\""));

            ArgumentCaptor<AllergyOnsetContext> saved = ArgumentCaptor.forClass(AllergyOnsetContext.class);
            verify(allergyContextRepository).saveAndFlush(saved.capture());
            assertThat(saved.getValue().getAllergyOnsetContextId()).isNull();
            assertThat(saved.getValue().getMedicalRecordId()).isEqualTo(RECORD_ID);
            assertThat(saved.getValue().getSuspectedMedicationTrigger()).isEqualTo("Penicillin");
        }

        @Test
        @DisplayName("AC-1: đã có dòng thì ghi đè trên chính entity đó, trường không gửi thành NULL")
        void overwritesManagedEntityAndClearsMissingFields() {
            AllergyOnsetContext existing = AllergyOnsetContext.builder().allergyOnsetContextId(77L)
                    .medicalRecordId(RECORD_ID).suspectedFoodTrigger("Tôm").foodTriggerDuration(new BigDecimal("2"))
                    .previousFoodReaction(true).foodHistoryDescription("Phù môi").build();
            when(allergyContextRepository.findByMedicalRecordId(RECORD_ID)).thenReturn(Optional.of(existing));

            AllergyContextResponse response = service.upsertAllergyContext(allergy("\"thuoc_nghi_ngo\":\"Aspirin\""));

            verify(allergyContextRepository).saveAndFlush(existing);
            assertThat(response.id()).isEqualTo("77");
            assertThat(existing.getSuspectedFoodTrigger()).isNull();
            assertThat(existing.getFoodTriggerDuration()).isNull();
            assertThat(existing.getPreviousFoodReaction()).isNull();
            assertThat(existing.getFoodHistoryDescription()).isNull();
            assertThat(existing.getSuspectedMedicationTrigger()).isEqualTo("Aspirin");
        }

        @Test
        @DisplayName("AC-10: body chỉ có record_id ghi một dòng toàn NULL")
        void emptyBodyWritesAllNullRow() {
            service.upsertInfectionStatus(infectionOnAcute(""));

            ArgumentCaptor<InfectionStatus> saved = ArgumentCaptor.forClass(InfectionStatus.class);
            verify(infectionStatusRepository).saveAndFlush(saved.capture());
            assertThat(saved.getValue()).hasAllNullFieldsOrPropertiesExcept("medicalRecordId");
        }

        @Test
        @DisplayName("Chuỗi được bỏ khoảng trắng hai đầu trước khi lưu")
        void textIsTrimmed() {
            service.upsertAllergyContext(allergy("\"thuoc_nghi_ngo\":\"  Aspirin  \""));

            ArgumentCaptor<AllergyOnsetContext> saved = ArgumentCaptor.forClass(AllergyOnsetContext.class);
            verify(allergyContextRepository).saveAndFlush(saved.capture());
            assertThat(saved.getValue().getSuspectedMedicationTrigger()).isEqualTo("Aspirin");
        }

        @Test
        @DisplayName("AC-1: số thập phân trả về luôn có hai chữ số, kể cả 1e3")
        void decimalsAreEchoedWithTwoDigits() {
            AllergyContextResponse response = service.upsertAllergyContext(
                    allergy("\"khac_nghi_ngo\":\"Phấn hoa\",\"khac_thoi_gian\":1e3,\"thuoc_nghi_ngo\":\"A\",\"thuoc_thoi_gian\":1.5"));

            assertThat(response.otherHours()).hasToString("1000.00");
            assertThat(response.medicationHours()).hasToString("1.50");
        }
    }

    private UpsertInfectionStatusRequest infectionOnAcute(String fields) {
        givenRecord("CAP", "draft");
        return infection(fields);
    }

    @Nested
    @DisplayName("Đổi lỗi database")
    class DatabaseErrors {

        @ParameterizedTest(name = "AC-2: SQLState {0} trả 2003")
        @ValueSource(strings = {"23514", "22003"})
        void checkViolationReturns2003(String sqlState) {
            when(allergyContextRepository.saveAndFlush(any())).thenThrow(dbError(sqlState));

            assertCode(() -> service.upsertAllergyContext(allergy("")), ErrorCode.INVALID_PARAMETER_VALUE);
        }

        @Test
        @DisplayName("Khóa ngoại tới subtype hỏng (23503) trả 4008")
        void brokenSubtypeForeignKeyReturns4008() {
            givenRecord("CAP", "draft");
            when(infectionStatusRepository.saveAndFlush(any())).thenThrow(dbError("23503"));

            assertCode(() -> service.upsertInfectionStatus(infection("")), ErrorCode.RECORD_TYPE_MISMATCH);
        }

        @Test
        @DisplayName("Lỗi database khác được ném lại nguyên vẹn")
        void otherErrorsAreRethrown() {
            DataIntegrityViolationException unique = dbError("23505");
            when(previousEpisodeRepository.saveAndFlush(any())).thenThrow(unique);

            assertThatThrownBy(() -> service.upsertPreviousEpisodes(episodes(""))).isSameAs(unique);
        }
    }

    @Nested
    @DisplayName("API 43: hoàn cảnh khởi phát dị ứng")
    class AllergyRules {

        @ParameterizedTest(name = "AC-2: {0} trả 2003")
        @ValueSource(strings = {
                "\"thuc_an_tung_bi\":true",
                "\"thuc_an_tung_bi\":true,\"thuc_an_tien_su_ghi_ro\":\"   \"",
                "\"thuc_an_tung_bi\":false,\"thuc_an_tien_su_ghi_ro\":\"Phù môi\"",
                "\"thuc_an_tien_su_ghi_ro\":\"Phù môi\"",
                "\"thuc_an_tung_bi\":true,\"thuc_an_tien_su_ghi_ro\":\"x\",\"thuc_an_loai_tien_su\":\"Khong\"",
                "\"thuc_an_tung_bi\":true,\"thuc_an_tien_su_ghi_ro\":\"x\",\"thuc_an_loai_tien_su\":\"di ung\"",
                "\"thuoc_loai_tien_su\":\"Phan ve\"",
                "\"thuoc_tung_bi\":false,\"thuoc_loai_tien_su\":\"Phan ve\"",
                "\"con_trung_thoi_gian\":3",
                "\"khac_nghi_ngo\":\"Bụi\",\"khac_thoi_gian\":-0.01",
                "\"khac_nghi_ngo\":\"Bụi\",\"khac_thoi_gian\":1.234",
                "\"khac_nghi_ngo\":\"Bụi\",\"khac_thoi_gian\":1000000000",
                "\"thuc_an_nghi_ngo\":\"\""
        })
        void rejectsRuleViolations(String fields) {
            assertCode(() -> service.upsertAllergyContext(allergy(fields)), ErrorCode.INVALID_PARAMETER_VALUE);
            verify(allergyContextRepository, never()).saveAndFlush(any());
        }

        @ParameterizedTest(name = "AC-2: {0} được nhận")
        @ValueSource(strings = {
                "\"thuc_an_tung_bi\":true,\"thuc_an_tien_su_ghi_ro\":\"Phù môi\"",
                "\"thuc_an_tung_bi\":true,\"thuc_an_tien_su_ghi_ro\":\"Phù môi\",\"thuc_an_loai_tien_su\":\"Phan ve\"",
                "\"thuoc_tung_bi\":true,\"thuoc_tien_su_ghi_ro\":\"Mẩn\",\"thuoc_loai_tien_su\":\"  Di ung  \"",
                "\"thuoc_tung_bi\":false",
                "\"khac_nghi_ngo\":\"Bụi\",\"khac_thoi_gian\":0",
                "\"khac_nghi_ngo\":\"Bụi\",\"khac_thoi_gian\":999999999.99"
        })
        void acceptsValidCombinations(String fields) {
            assertThat(service.upsertAllergyContext(allergy(fields))).isNotNull();
        }
    }

    @Nested
    @DisplayName("API 44: bệnh sử các đợt trước")
    class EpisodeRules {

        @Test
        @DisplayName("AC-7: hồ sơ CAP trả 4008 và không ghi gì")
        void acuteRecordReturns4008() {
            givenRecord("CAP", "draft");

            assertCode(() -> service.upsertPreviousEpisodes(episodes("")), ErrorCode.RECORD_TYPE_MISMATCH);
            verifyNoInteractions(previousEpisodeRepository);
        }

        @ParameterizedTest(name = "AC-4: {0} trả 2003")
        @ValueSource(strings = {
                "\"tuoi_khoi_phat\":-1",
                "\"tuoi_khoi_phat\":151",
                "\"so_dot_truoc\":2",
                "\"tung_co_dot_tuong_tu\":false,\"so_dot_truoc\":1",
                "\"so_lan_nhap_vien\":1",
                "\"da_tung_nhap_vien\":false,\"so_lan_nhap_vien\":3",
                "\"tung_co_dot_tuong_tu\":true,\"so_dot_truoc\":10001",
                "\"tung_co_dot_tuong_tu\":true,\"so_dot_truoc\":-1",
                "\"kieu_dien_bien\":\"Tung dot\"",
                "\"kieu_dien_bien\":\"khong ro\"",
                "\"bieu_hien_dot_truoc\":\"san phu\"",
                "\"bieu_hien_dot_truoc\":\"\"",
                "\"thoi_gian_tu_dot_gan_nhat\":-0.5",
                "\"thoi_gian_tu_dot_gan_nhat\":0.125",
                "\"dien_bien_cac_dot_truoc\":\"  \""
        })
        void rejectsRuleViolations(String fields) {
            assertCode(() -> service.upsertPreviousEpisodes(episodes(fields)), ErrorCode.INVALID_PARAMETER_VALUE);
        }

        @ParameterizedTest(name = "AC-3: {0} được nhận")
        @ValueSource(strings = {
                "\"tung_co_dot_tuong_tu\":true,\"so_dot_truoc\":3,\"tuoi_khoi_phat\":150",
                "\"tung_co_dot_tuong_tu\":false,\"so_dot_truoc\":0",
                "\"da_tung_nhap_vien\":true,\"so_lan_nhap_vien\":10000",
                "\"kieu_dien_bien\":\"Lien tuc, hau nhu khong co giai doan het trieu chung\"",
                "\"kieu_dien_bien\":\"  Khong ro \"",
                "\"bieu_hien_dot_truoc\":\"San phu + phu mach\"",
                "\"thoi_gian_tu_dot_gan_nhat\":6"
        })
        void acceptsValidValues(String fields) {
            assertThat(service.upsertPreviousEpisodes(episodes(fields))).isNotNull();
        }

        @Test
        @DisplayName("AC-4: danh mục được lưu sau khi bỏ khoảng trắng")
        void catalogValueIsStoredTrimmed() {
            service.upsertPreviousEpisodes(episodes("\"kieu_dien_bien\":\"  Khong ro \""));

            ArgumentCaptor<PreviousDiseaseEpisode> saved = ArgumentCaptor.forClass(PreviousDiseaseEpisode.class);
            verify(previousEpisodeRepository).saveAndFlush(saved.capture());
            assertThat(saved.getValue().getDiseaseCoursePattern()).isEqualTo("Khong ro");
        }
    }

    @Nested
    @DisplayName("API 45: tình trạng nhiễm trùng")
    class InfectionRules {

        @Test
        @DisplayName("AC-7: hồ sơ MAN trả 4008 và không ghi gì")
        void chronicRecordReturns4008() {
            assertCode(() -> service.upsertInfectionStatus(infection("")), ErrorCode.RECORD_TYPE_MISMATCH);
            verifyNoInteractions(infectionStatusRepository);
        }

        @ParameterizedTest(name = "AC-6: {0} trả 2003")
        @ValueSource(strings = {
                "\"ho_hap_thoi_gian\":3",
                "\"ho_hap_ho\":false,\"ho_hap_thoi_gian\":0",
                "\"ho_hap_ho\":false,\"ho_hap_dau_hong\":false,\"ho_hap_hat_hoi\":false,\"ho_hap_so_mui\":false,\"ho_hap_khoang_cach\":1",
                "\"tiet_nieu_khoang_cach\":1",
                "\"da_mun\":false,\"da_thoi_gian\":1",
                "\"khac_thoi_gian\":1",
                "\"khac_khoang_cach\":0",
                "\"khac_ghi_ro\":\"   \"",
                "\"ho_hap_ho\":true,\"ho_hap_thoi_gian\":-1",
                "\"da_nhot\":true,\"da_khoang_cach\":2.555"
        })
        void rejectsRuleViolations(String fields) {
            assertCode(() -> service.upsertInfectionStatus(infectionOnAcute(fields)), ErrorCode.INVALID_PARAMETER_VALUE);
        }

        @ParameterizedTest(name = "AC-5: {0} được nhận")
        @ValueSource(strings = {
                "\"ho_hap_so_mui\":true,\"ho_hap_thoi_gian\":3,\"ho_hap_khoang_cach\":1",
                "\"tiet_nieu_tieu_buot\":true,\"tiet_nieu_thoi_gian\":0",
                "\"da_nhot\":true,\"da_khoang_cach\":0",
                "\"khac_ghi_ro\":\"Sốt nhẹ\",\"khac_thoi_gian\":2.5",
                "\"ho_hap_ho\":true"
        })
        void acceptsValidGroups(String fields) {
            assertThat(service.upsertInfectionStatus(infectionOnAcute(fields))).isNotNull();
        }
    }
}
