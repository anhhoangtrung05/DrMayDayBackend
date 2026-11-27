package com.example.drmayday.features.lab;

import com.example.drmayday.features.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Lab APIs (LAB_UPSERT_LAB_RESULTS, LAB_GET_LAB_RESULTS) through the real filter chain against PostgreSQL.
 *
 * Tests roll back, so the DEFERRABLE "subtype must exist" FKs would never fire at commit;
 * {@link #assertSubtypeConstraintsHold()} forces them to be checked inside the test.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LabResultApiIntegrationTest {

    private static final String BASE = "/api/v1/lab-results";
    private static final long PATIENT_ID = 2L;
    private static final long DOCTOR_EMPLOYEE_ID = 1L;
    private static final long MISSING_ID = 999_999_999L;

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @Autowired private JdbcTemplate jdbcTemplate;

    private String doctorToken;
    private String otherPatientToken;

    @BeforeEach
    void setUp() {
        doctorToken = jwtTokenProvider.generateToken(3L, "0912345679", "bac_si");
        otherPatientToken = jwtTokenProvider.generateToken(77L, "0912000077", "benh_nhan");
    }

    // ── Helpers ──

    private ResultActions send(MockHttpServletRequestBuilder builder, String token, String json) throws Exception {
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        if (json != null) {
            builder.contentType(MediaType.APPLICATION_JSON).content(json);
        }
        return mockMvc.perform(builder);
    }

    private static void expect(ResultActions result, int httpStatus, String code) throws Exception {
        result.andExpect(status().is(httpStatus)).andExpect(jsonPath("$.code").value(code));
    }

    private long givenRecord(String type) {
        Long id = jdbcTemplate.queryForObject(
                "INSERT INTO hosobenhan (ma_benh_nhan, loai_benh_an) VALUES (?, ?) RETURNING ma_ho_so",
                Long.class, PATIENT_ID, type);
        jdbcTemplate.update("INSERT INTO " + ("CAP".equals(type) ? "benhanmaydaycap" : "benhanmaydayman")
                + " (ma_ho_so) VALUES (?)", id);
        return id;
    }

    private long givenVisit(long recordId, String type) {
        Long id = jdbcTemplate.queryForObject(
                "INSERT INTO phienkham (ma_ho_so, ma_bac_si, loai_benh_an, ngay_kham, lan_kham_thu) "
                        + "VALUES (?, ?, ?, ?, 1) RETURNING ma_phien_kham",
                Long.class, recordId, DOCTOR_EMPLOYEE_ID, type, LocalDate.now());
        // phienkham requires its CAP/MAN subtype row (deferred FK fk_phien_bat_buoc_cap/man).
        jdbcTemplate.update("INSERT INTO " + ("CAP".equals(type) ? "phienkhamcap" : "phienkhamman")
                + " (ma_phien_kham) VALUES (?)", id);
        return id;
    }

    /** Fails if a ketquacanlamsang row was written without its CAP/MAN subtype. */
    private void assertSubtypeConstraintsHold() {
        jdbcTemplate.execute("SET CONSTRAINTS ALL IMMEDIATE");
    }

    private int resultCount(long visitId) {
        Integer n = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM ketquacanlamsang WHERE ma_phien_kham = ?", Integer.class, visitId);
        return n == null ? 0 : n;
    }

    // ── LAB_UPSERT_LAB_RESULTS ──

    @Nested
    @DisplayName("LAB_UPSERT_LAB_RESULTS PUT /lab-results")
    class UpsertLabResults {

        @Test
        @DisplayName("[Happy Path] Phiên CAP nhập WBC/NEU/LYM/PLT trả về 1001, NLR/PLR do DB tự tính")
        void createsAcuteResult() throws Exception {
            long record = givenRecord("CAP");
            long visit = givenVisit(record, "CAP");

            ResultActions result = send(put(BASE), doctorToken, """
                    {"record_id": %d, "phien_kham_id": %d, "wbc": 8.1, "neu": 4, "lym": 2, "plt": 250,
                     "thoi_diem_thuc_hien": "2026-11-24T08:30:00+07:00"}
                    """.formatted(record, visit));

            expect(result, 201, "1001");
            result.andExpect(jsonPath("$.data.loai").value("CAP"))
                    .andExpect(jsonPath("$.data.nlr").value(2.0))
                    .andExpect(jsonPath("$.data.plr").value(125.0));
            assertSubtypeConstraintsHold();
            assertEquals(1, resultCount(visit));
        }

        @Test
        @DisplayName("[Happy Path] Nhập lại (sửa CRP) trả về 1000 và không tạo thêm kết quả")
        void replacesExistingResult() throws Exception {
            long record = givenRecord("CAP");
            long visit = givenVisit(record, "CAP");
            String body = "{\"record_id\": %d, \"phien_kham_id\": %d, \"crp\": %s}";
            expect(send(put(BASE), doctorToken, body.formatted(record, visit, "3")), 201, "1001");

            ResultActions result = send(put(BASE), doctorToken, body.formatted(record, visit, "12.5"));

            expect(result, 200, "1000");
            result.andExpect(jsonPath("$.data.crp").value(12.5));
            assertSubtypeConstraintsHold();
            assertEquals(1, resultCount(visit));
        }

        @Test
        @DisplayName("[Alternate Path] Phiên MAN với test_huyet_thanh_tu_than = Chua lam lưu bình thường")
        void savesChronicResult() throws Exception {
            long record = givenRecord("MAN");
            long visit = givenVisit(record, "MAN");

            ResultActions result = send(put(BASE), doctorToken, """
                    {"record_id": %d, "phien_kham_id": %d, "test_huyet_thanh_tu_than": "Chua lam", "tsh": 1.2}
                    """.formatted(record, visit));

            expect(result, 201, "1001");
            result.andExpect(jsonPath("$.data.loai").value("MAN"))
                    .andExpect(jsonPath("$.data.test_huyet_thanh_tu_than").value("Chua lam"));
            assertSubtypeConstraintsHold();
        }

        @Test
        @DisplayName("[Unhappy Path] Phiên MAN gửi field lym/neu của CAP trả về 8103")
        void chronicVisitWithAcuteFields() throws Exception {
            long record = givenRecord("MAN");
            long visit = givenVisit(record, "MAN");

            expect(send(put(BASE), doctorToken, """
                    {"record_id": %d, "phien_kham_id": %d, "lym": 2, "neu": 4}
                    """.formatted(record, visit)), 400, "8103");
            assertEquals(0, resultCount(visit));
        }

        @Test
        @DisplayName("[Unhappy Path] asst_duong_kinh_mm khi test_huyet_thanh_tu_than khác Duong tinh trả về 8105")
        void asstWithoutPositiveSerumTest() throws Exception {
            long record = givenRecord("MAN");
            long visit = givenVisit(record, "MAN");

            expect(send(put(BASE), doctorToken, """
                    {"record_id": %d, "phien_kham_id": %d, "test_huyet_thanh_tu_than": "Am tinh", "asst_duong_kinh_mm": 5}
                    """.formatted(record, visit)), 400, "8105");
        }

        @Test
        @DisplayName("[Exception Path] Phiên khám không thuộc hồ sơ trả về 5008")
        void visitNotInRecord() throws Exception {
            long record = givenRecord("CAP");
            long otherRecord = givenRecord("CAP");
            long visit = givenVisit(otherRecord, "CAP");

            expect(send(put(BASE), doctorToken, "{\"record_id\": %d, \"phien_kham_id\": %d}".formatted(record, visit)),
                    404, "5008");
        }

        @Test
        @DisplayName("[Exception Path] Hồ sơ bệnh án không tồn tại trả về 4001")
        void recordNotFound() throws Exception {
            expect(send(put(BASE), doctorToken, "{\"record_id\": %d, \"phien_kham_id\": 1}".formatted(MISSING_ID)),
                    404, "4001");
        }

        @Test
        @DisplayName("[Unhappy Path] Thiếu record_id trả về 2001, nêu tên field và không lưu kết quả")
        void missingRecordId() throws Exception {
            long record = givenRecord("CAP");
            long visit = givenVisit(record, "CAP");

            ResultActions result = send(put(BASE), doctorToken, "{\"phien_kham_id\": %d, \"crp\": 3}".formatted(visit));

            expect(result, 400, "2001");
            result.andExpect(jsonPath("$.message").value("Thiếu tham số bắt buộc: record_id"));
            assertEquals(0, resultCount(visit));
        }

        @Test
        @DisplayName("[Unhappy Path] Thiếu phien_kham_id trả về 2001 và nêu tên field")
        void missingVisitId() throws Exception {
            long record = givenRecord("CAP");

            ResultActions result = send(put(BASE), doctorToken, "{\"record_id\": %d, \"crp\": 3}".formatted(record));

            expect(result, 400, "2001");
            result.andExpect(jsonPath("$.message").value("Thiếu tham số bắt buộc: phien_kham_id"));
        }

        @Test
        @DisplayName("[Unhappy Path] Body rỗng {} trả về 2001 và báo field bắt buộc đầu tiên (record_id)")
        void emptyJsonBody() throws Exception {
            ResultActions result = send(put(BASE), doctorToken, "{}");

            expect(result, 400, "2001");
            result.andExpect(jsonPath("$.message").value("Thiếu tham số bắt buộc: record_id"));
        }

        @Test
        @DisplayName("[Unhappy Path] Không gửi body trả về 400 và mã 2003")
        void noBody() throws Exception {
            ResultActions result = send(put(BASE).contentType(MediaType.APPLICATION_JSON), doctorToken, null);

            expect(result, 400, "2003");
            result.andExpect(jsonPath("$.message").value("Thiếu hoặc không đọc được nội dung request body"));
        }
    }

    // ── LAB_GET_LAB_RESULTS ──

    @Nested
    @DisplayName("LAB_GET_LAB_RESULTS GET /lab-results")
    class GetLabResults {

        @Test
        @DisplayName("[Happy Path] Phiên đã có kết quả CAP trả về field chung + CAP + nlr/plr")
        void returnsAcuteResult() throws Exception {
            long record = givenRecord("CAP");
            long visit = givenVisit(record, "CAP");
            send(put(BASE), doctorToken, """
                    {"record_id": %d, "phien_kham_id": %d, "crp": 3, "neu": 6, "lym": 3, "plt": 300}
                    """.formatted(record, visit));

            ResultActions result = send(get(BASE).param("phien_kham_id", String.valueOf(visit)), doctorToken, null);

            expect(result, 200, "1000");
            result.andExpect(jsonPath("$.data.record_id").value(String.valueOf(record)))
                    .andExpect(jsonPath("$.data.crp").value(3.0))
                    .andExpect(jsonPath("$.data.nlr").value(2.0))
                    .andExpect(jsonPath("$.data.plr").value(100.0));
        }

        @Test
        @DisplayName("[Happy Path] Phiên MAN có kết quả trả về field MAN")
        void returnsChronicResult() throws Exception {
            long record = givenRecord("MAN");
            long visit = givenVisit(record, "MAN");
            send(put(BASE), doctorToken, """
                    {"record_id": %d, "phien_kham_id": %d, "ana_hep2": "Am tinh", "ft4": 14}
                    """.formatted(record, visit));

            ResultActions result = send(get(BASE).param("phien_kham_id", String.valueOf(visit)), doctorToken, null);

            expect(result, 200, "1000");
            result.andExpect(jsonPath("$.data.loai").value("MAN"))
                    .andExpect(jsonPath("$.data.ana_hep2").value("Am tinh"))
                    .andExpect(jsonPath("$.data.ft4").value(14.0));
        }

        @Test
        @DisplayName("[Exception Path] Phiên chưa nhập kết quả trả về 8104")
        void resultNotFound() throws Exception {
            long record = givenRecord("CAP");
            long visit = givenVisit(record, "CAP");

            expect(send(get(BASE).param("phien_kham_id", String.valueOf(visit)), doctorToken, null), 404, "8104");
        }

        @Test
        @DisplayName("[Unhappy Path] Thiếu phien_kham_id trả về 2001")
        void missingVisitId() throws Exception {
            expect(send(get(BASE), doctorToken, null), 400, "2001");
        }

        @Test
        @DisplayName("[Exception Path] Bệnh nhân xem kết quả của người khác trả về 3106")
        void otherPatientReads() throws Exception {
            long record = givenRecord("CAP");
            long visit = givenVisit(record, "CAP");
            send(put(BASE), doctorToken, "{\"record_id\": %d, \"phien_kham_id\": %d, \"crp\": 1}".formatted(record, visit));

            expect(send(get(BASE).param("phien_kham_id", String.valueOf(visit)), otherPatientToken, null), 403, "3106");
        }
    }
}
