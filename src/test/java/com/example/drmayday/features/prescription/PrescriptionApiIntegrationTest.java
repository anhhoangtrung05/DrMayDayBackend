package com.example.drmayday.features.prescription;

import com.example.drmayday.features.security.JwtTokenProvider;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prescription APIs (PRESCRIPTIONS_*) through the real filter chain against PostgreSQL. Scenarios come from the
 * "Kịch bản kiểm thử" column of docs/API Design - IT4788 (3).xlsx.
 *
 * Each test runs in a rolled-back transaction, so it leaves no rows behind.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PrescriptionApiIntegrationTest {

    private static final String BASE = "/api/v1/prescriptions";
    private static final String STOP = BASE + "/stop-instructions";

    /** Patient and employee seeded by DatabaseSeeder. */
    private static final long PATIENT_ID = 2L;
    private static final long DOCTOR_EMPLOYEE_ID = 1L;
    private static final long MISSING_ID = 999_999_999L;
    private static final String CATALOG_MEDICATION = "IT_TEST_T01";

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @Autowired private JdbcTemplate jdbcTemplate;
    @PersistenceContext private EntityManager entityManager;

    private String doctorToken;
    private String ownerPatientToken;
    private String otherPatientToken;

    private long recordId;
    private long visitId;

    @BeforeEach
    void setUp() {
        doctorToken = jwtTokenProvider.generateToken(3L, "0912345679", "bac_si");
        ownerPatientToken = jwtTokenProvider.generateToken(PATIENT_ID, "0912345678", "benh_nhan");
        otherPatientToken = jwtTokenProvider.generateToken(77L, "0912000077", "benh_nhan");

        jdbcTemplate.update("INSERT INTO thuoc (ma_thuoc, ten_thuoc) VALUES (?, ?)", CATALOG_MEDICATION, "Cetirizin 10mg");
        recordId = givenRecord();
        visitId = givenVisit(recordId, 1);
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

    /** 400 + 2001 with the message naming the missing field. */
    private static void expectMissing(ResultActions result, String message) throws Exception {
        expect(result, 400, "2001");
        result.andExpect(jsonPath("$.message").value(message));
    }

    private int prescriptionCount() {
        return count("SELECT count(*) FROM donthuoc WHERE ma_ho_so = ?", recordId);
    }

    private long givenRecord() {
        Long id = jdbcTemplate.queryForObject(
                "INSERT INTO hosobenhan (ma_benh_nhan, loai_benh_an) VALUES (?, 'CAP') RETURNING ma_ho_so",
                Long.class, PATIENT_ID);
        jdbcTemplate.update("INSERT INTO benhanmaydaycap (ma_ho_so) VALUES (?)", id);
        return id;
    }

    private long givenVisit(long recordId, int visitNumber) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO phienkham (ma_ho_so, ma_bac_si, loai_benh_an, ngay_kham, lan_kham_thu) "
                        + "VALUES (?, ?, 'CAP', ?, ?) RETURNING ma_phien_kham",
                Long.class, recordId, DOCTOR_EMPLOYEE_ID, LocalDate.now(), visitNumber);
    }

    private long givenPrescription() {
        Long id = jdbcTemplate.queryForObject(
                "INSERT INTO donthuoc (ma_ho_so, ma_phien_kham) VALUES (?, ?) RETURNING ma_don_thuoc",
                Long.class, recordId, visitId);
        jdbcTemplate.update("INSERT INTO chitietdonthuoc (ma_don_thuoc, ma_thuoc, lieu_dung, thoi_gian_dung) "
                + "VALUES (?, ?, '1 viên', '7 ngày')", id, CATALOG_MEDICATION);
        return id;
    }

    private String createBody(String itemsJson) {
        return """
                {"record_id": %d, "phien_kham_id": %d, "items": %s}
                """.formatted(recordId, visitId, itemsJson);
    }

    /**
     * Reads the DB directly. The API call joined the test transaction, so pending
     * Hibernate changes are flushed first (this also runs the DB CHECK constraints).
     */
    private <T> T query(String sql, Class<T> type, Object... args) {
        entityManager.flush();
        return jdbcTemplate.queryForObject(sql, type, args);
    }

    private int count(String sql, Object... args) {
        Integer n = query(sql, Integer.class, args);
        return n == null ? 0 : n;
    }

    // ── PRESCRIPTIONS_CREATE_PRESCRIPTIONS ──

    @Nested
    @DisplayName("PRESCRIPTIONS_CREATE_PRESCRIPTIONS POST /prescriptions")
    class CreatePrescription {

        @Test
        @DisplayName("[Happy Path] Kê 2 thuốc (1 ma_thuoc, 1 ten_thuoc_bo_sung) hợp lệ trả về 1001")
        void createsPrescription() throws Exception {
            ResultActions result = send(post(BASE), doctorToken, createBody("""
                    [{"ma_thuoc": "%s", "lieu_dung": "1 viên/ngày", "thoi_gian_dung": "7 ngày"},
                     {"ten_thuoc_bo_sung": "Thuốc nam", "lieu_dung": "2 viên/ngày", "thoi_gian_dung": "5 ngày"}]
                    """.formatted(CATALOG_MEDICATION)));

            expect(result, 201, "1001");
            result.andExpect(jsonPath("$.data.trang_thai_don").value("DANG_DIEU_TRI"))
                    .andExpect(jsonPath("$.data.ngay_ke_don").value(LocalDate.now().toString()))
                    .andExpect(jsonPath("$.data.items.length()").value(2))
                    .andExpect(jsonPath("$.data.items[0].chi_tiet_id").exists());
            assertEquals(1, count("SELECT count(*) FROM donthuoc WHERE ma_phien_kham = ?", visitId));
            assertEquals(2, count("SELECT count(*) FROM chitietdonthuoc c JOIN donthuoc d USING (ma_don_thuoc) "
                    + "WHERE d.ma_phien_kham = ?", visitId));
        }

        @Test
        @DisplayName("[Unhappy Path] items rỗng trả về 8001")
        void emptyItems() throws Exception {
            expect(send(post(BASE), doctorToken, createBody("[]")), 400, "8001");
        }

        @Test
        @DisplayName("[Unhappy Path] Item có cả ma_thuoc lẫn ten_thuoc_bo_sung trả về 8004")
        void itemHasBothMedicationFields() throws Exception {
            expect(send(post(BASE), doctorToken, createBody("""
                    [{"ma_thuoc": "%s", "ten_thuoc_bo_sung": "B", "lieu_dung": "1", "thoi_gian_dung": "1"}]
                    """.formatted(CATALOG_MEDICATION))), 400, "8004");
        }

        @Test
        @DisplayName("[Unhappy Path] ma_thuoc không có trong danh mục trả về 8005")
        void medicationNotInCatalog() throws Exception {
            expect(send(post(BASE), doctorToken, createBody("""
                    [{"ma_thuoc": "KHONG_CO_TRONG_DANH_MUC", "lieu_dung": "1", "thoi_gian_dung": "1"}]
                    """)), 404, "8005");
        }

        @Test
        @DisplayName("[Exception Path] phien_kham_id không thuộc record_id trả về 5008")
        void visitNotInRecord() throws Exception {
            long otherRecord = givenRecord();
            expect(send(post(BASE), doctorToken, """
                    {"record_id": %d, "phien_kham_id": %d,
                     "items": [{"ten_thuoc_bo_sung": "B", "lieu_dung": "1", "thoi_gian_dung": "1"}]}
                    """.formatted(otherRecord, visitId)), 404, "5008");
        }

        @Test
        @DisplayName("[Unhappy Path] Thiếu record_id trả về 2001, nêu tên field và không tạo đơn")
        void missingRecordId() throws Exception {
            expectMissing(send(post(BASE), doctorToken, """
                    {"phien_kham_id": %d, "items": [{"ten_thuoc_bo_sung": "B", "lieu_dung": "1", "thoi_gian_dung": "1"}]}
                    """.formatted(visitId)), "Thiếu tham số bắt buộc: record_id");
            assertEquals(0, prescriptionCount());
        }

        @Test
        @DisplayName("[Unhappy Path] Thiếu phien_kham_id trả về 2001, nêu tên field và không tạo đơn")
        void missingVisitId() throws Exception {
            expectMissing(send(post(BASE), doctorToken, """
                    {"record_id": %d, "items": [{"ten_thuoc_bo_sung": "B", "lieu_dung": "1", "thoi_gian_dung": "1"}]}
                    """.formatted(recordId)), "Thiếu tham số bắt buộc: phien_kham_id");
            assertEquals(0, prescriptionCount());
        }

        @Test
        @DisplayName("[Unhappy Path] Không gửi items trả về 8001 và không tạo đơn")
        void missingItems() throws Exception {
            expect(send(post(BASE), doctorToken,
                    "{\"record_id\": %d, \"phien_kham_id\": %d}".formatted(recordId, visitId)), 400, "8001");
            assertEquals(0, prescriptionCount());
        }

        @Test
        @DisplayName("[Unhappy Path] Thuốc thứ 2 thiếu lieu_dung trả về 2001, nêu đúng thuốc thứ 2 và không tạo đơn")
        void itemMissingDosage() throws Exception {
            expectMissing(send(post(BASE), doctorToken, createBody("""
                    [{"ten_thuoc_bo_sung": "A", "lieu_dung": "1", "thoi_gian_dung": "1"},
                     {"ten_thuoc_bo_sung": "B", "thoi_gian_dung": "1"}]
                    """)), "Thuốc thứ 2: lieu_dung là bắt buộc");
            assertEquals(0, prescriptionCount());
        }

        @Test
        @DisplayName("[Unhappy Path] Item thiếu thoi_gian_dung trả về 2001 và nêu tên field")
        void itemMissingDuration() throws Exception {
            expectMissing(send(post(BASE), doctorToken, createBody("""
                    [{"ma_thuoc": "%s", "lieu_dung": "1"}]
                    """.formatted(CATALOG_MEDICATION))), "Thuốc thứ 1: thoi_gian_dung là bắt buộc");
            assertEquals(0, prescriptionCount());
        }

        @Test
        @DisplayName("[Unhappy Path] Body rỗng {} trả về 2001 và báo field bắt buộc đầu tiên (record_id)")
        void emptyJsonBody() throws Exception {
            expectMissing(send(post(BASE), doctorToken, "{}"), "Thiếu tham số bắt buộc: record_id");
        }

        @Test
        @DisplayName("[Unhappy Path] Không gửi body trả về 400 và mã 2003")
        void noBody() throws Exception {
            ResultActions result = send(post(BASE).contentType(MediaType.APPLICATION_JSON), doctorToken, null);
            expect(result, 400, "2003");
            result.andExpect(jsonPath("$.message").value("Thiếu hoặc không đọc được nội dung request body"));
        }

        @Test
        @DisplayName("[Exception Path] Bệnh nhân không được kê đơn (403)")
        void patientCannotPrescribe() throws Exception {
            send(post(BASE), ownerPatientToken, createBody("[]")).andExpect(status().isForbidden());
        }
    }

    // ── PRESCRIPTIONS_GET_PRESCRIPTIONS ──

    @Nested
    @DisplayName("PRESCRIPTIONS_GET_PRESCRIPTIONS GET /prescriptions")
    class GetPrescriptions {

        @Test
        @DisplayName("[Happy Path] Lọc theo phien_kham_id trả về các đơn của phiên")
        void filtersByVisit() throws Exception {
            long prescriptionId = givenPrescription();
            long otherVisit = givenVisit(recordId, 2);
            jdbcTemplate.update("INSERT INTO donthuoc (ma_ho_so, ma_phien_kham) VALUES (?, ?)", recordId, otherVisit);

            ResultActions result = send(get(BASE).param("phien_kham_id", String.valueOf(visitId)), doctorToken, null);

            expect(result, 200, "1000");
            result.andExpect(jsonPath("$.data.length()").value(1))
                    .andExpect(jsonPath("$.data[0].prescription_id").value(String.valueOf(prescriptionId)))
                    .andExpect(jsonPath("$.data[0].items[0].ma_thuoc").value(CATALOG_MEDICATION));
        }

        @Test
        @DisplayName("[Happy Path] Lọc theo record_id trả về tất cả đơn của hồ sơ")
        void filtersByRecord() throws Exception {
            givenPrescription();
            long otherVisit = givenVisit(recordId, 2);
            jdbcTemplate.update("INSERT INTO donthuoc (ma_ho_so, ma_phien_kham) VALUES (?, ?)", recordId, otherVisit);

            ResultActions result = send(get(BASE).param("record_id", String.valueOf(recordId)), doctorToken, null);

            expect(result, 200, "1000");
            result.andExpect(jsonPath("$.data.length()").value(2));
        }

        @Test
        @DisplayName("[Unhappy Path] Không truyền query nào trả về 2001")
        void missingFilters() throws Exception {
            expect(send(get(BASE), doctorToken, null), 400, "2001");
        }

        @Test
        @DisplayName("[Alternate Path] Bệnh nhân xem đơn thuốc của chính mình")
        void ownerPatientReads() throws Exception {
            givenPrescription();
            expect(send(get(BASE).param("record_id", String.valueOf(recordId)), ownerPatientToken, null), 200, "1000");
        }

        @Test
        @DisplayName("[Exception Path] Bệnh nhân xem đơn thuốc của người khác trả về 3106")
        void otherPatientReads() throws Exception {
            givenPrescription();
            expect(send(get(BASE).param("record_id", String.valueOf(recordId)), otherPatientToken, null), 403, "3106");
        }
    }

    // ── PRESCRIPTIONS_UPDATE_PRESCRIPTION ──

    @Nested
    @DisplayName("PRESCRIPTIONS_UPDATE_PRESCRIPTION PUT /prescriptions/{id}")
    class UpdatePrescription {

        @Test
        @DisplayName("[Happy Path] Đổi trang_thai_don sang HOAN_THANH trả về 1000")
        void updatesStatus() throws Exception {
            long id = givenPrescription();

            ResultActions result = send(put(BASE + "/" + id), doctorToken, "{\"trang_thai_don\": \"HOAN_THANH\"}");

            expect(result, 200, "1000");
            result.andExpect(jsonPath("$.data.trang_thai_don").value("HOAN_THANH"))
                    .andExpect(jsonPath("$.data.items.length()").value(1));
            assertEquals("HOAN_THANH", query(
                    "SELECT trang_thai_don FROM donthuoc WHERE ma_don_thuoc = ?", String.class, id));
        }

        @Test
        @DisplayName("[Happy Path] Gửi 3 thuốc mới thì thay toàn bộ danh sách cũ")
        void replacesItems() throws Exception {
            long id = givenPrescription();

            ResultActions result = send(put(BASE + "/" + id), doctorToken, """
                    {"items": [{"ten_thuoc_bo_sung": "A", "lieu_dung": "1", "thoi_gian_dung": "1"},
                               {"ten_thuoc_bo_sung": "B", "lieu_dung": "1", "thoi_gian_dung": "1"},
                               {"ma_thuoc": "%s", "lieu_dung": "1", "thoi_gian_dung": "1"}]}
                    """.formatted(CATALOG_MEDICATION));

            expect(result, 200, "1000");
            result.andExpect(jsonPath("$.data.items.length()").value(3));
            assertEquals(3, count("SELECT count(*) FROM chitietdonthuoc WHERE ma_don_thuoc = ?", id));
        }

        @Test
        @DisplayName("[Exception Path] id không tồn tại trả về 8003")
        void prescriptionNotFound() throws Exception {
            expect(send(put(BASE + "/" + MISSING_ID), doctorToken, "{\"trang_thai_don\": \"HUY\"}"), 404, "8003");
        }

        @Test
        @DisplayName("[Unhappy Path] Item vi phạm XOR trả về 8004 và giữ nguyên thuốc cũ")
        void itemViolatesXor() throws Exception {
            long id = givenPrescription();

            expect(send(put(BASE + "/" + id), doctorToken,
                    "{\"items\": [{\"lieu_dung\": \"1\", \"thoi_gian_dung\": \"1\"}]}"), 400, "8004");
            assertEquals(1, count("SELECT count(*) FROM chitietdonthuoc WHERE ma_don_thuoc = ?", id));
        }

        @Test
        @DisplayName("[Unhappy Path] Item mới thiếu thoi_gian_dung trả về 2001 và giữ nguyên thuốc cũ")
        void itemMissingDuration() throws Exception {
            long id = givenPrescription();

            expectMissing(send(put(BASE + "/" + id), doctorToken,
                    "{\"items\": [{\"ten_thuoc_bo_sung\": \"A\", \"lieu_dung\": \"1\"}]}"),
                    "Thuốc thứ 1: thoi_gian_dung là bắt buộc");
            assertEquals(1, count("SELECT count(*) FROM chitietdonthuoc WHERE ma_don_thuoc = ? AND ma_thuoc = ?",
                    id, CATALOG_MEDICATION));
        }
    }

    // ── PRESCRIPTIONS_UPSERT_STOP_MEDICATION ──

    @Nested
    @DisplayName("PRESCRIPTIONS_UPSERT_STOP_MEDICATION PUT /prescriptions/stop-instructions")
    class UpsertStopMedication {

        @Test
        @DisplayName("[Happy Path] Gửi 2 dòng Test kich thich thì ngung_thuoc_lan_sau = true")
        void savesInstructions() throws Exception {
            long id = givenPrescription();

            ResultActions result = send(put(STOP), doctorToken, """
                    {"don_thuoc_id": %d, "instructions": [
                      {"thuoc_can_ngung": "Cetirizin", "thoi_gian_ngung_ngay": 7, "muc_dich": "Test kich thich"},
                      {"thuoc_can_ngung": "Loratadin", "ngung_tu_ngay": "2026-12-01", "muc_dich": "Test kich thich"}]}
                    """.formatted(id));

            expect(result, 200, "1000");
            result.andExpect(jsonPath("$.data.ngung_thuoc_lan_sau").value(true))
                    .andExpect(jsonPath("$.data.instructions.length()").value(2))
                    .andExpect(jsonPath("$.data.instructions[1].ngung_tu_ngay").value("2026-12-01"));
            assertTrue(query(
                    "SELECT ngung_thuoc_lan_sau FROM donthuoc WHERE ma_don_thuoc = ?", Boolean.class, id));
            assertEquals("Cetirizin, Loratadin", query(
                    "SELECT thuoc_can_ngung_truoc_tai_kham FROM donthuoc WHERE ma_don_thuoc = ?", String.class, id));
        }

        @Test
        @DisplayName("[Unhappy Path] muc_dich = Khac không có ghi rõ trả về 8201")
        void otherPurposeWithoutDescription() throws Exception {
            long id = givenPrescription();
            expect(send(put(STOP), doctorToken, """
                    {"don_thuoc_id": %d, "instructions": [{"thuoc_can_ngung": "A", "muc_dich": "Khac"}]}
                    """.formatted(id)), 400, "8201");
        }

        @Test
        @DisplayName("[Alternate Path] Gửi mảng rỗng thì xoá hết và ngung_thuoc_lan_sau = false")
        void clearsInstructions() throws Exception {
            long id = givenPrescription();
            send(put(STOP), doctorToken, """
                    {"don_thuoc_id": %d, "instructions": [{"thuoc_can_ngung": "A", "muc_dich": "Test kich thich"}]}
                    """.formatted(id));

            ResultActions result = send(put(STOP), doctorToken, "{\"don_thuoc_id\": %d, \"instructions\": []}".formatted(id));

            expect(result, 200, "1000");
            result.andExpect(jsonPath("$.data.ngung_thuoc_lan_sau").value(false));
            assertEquals(0, count("SELECT count(*) FROM chidanngungthuoc WHERE ma_don_thuoc = ?", id));
            assertFalse(query(
                    "SELECT ngung_thuoc_lan_sau FROM donthuoc WHERE ma_don_thuoc = ?", Boolean.class, id));
            assertNull(query(
                    "SELECT thuoc_can_ngung_truoc_tai_kham FROM donthuoc WHERE ma_don_thuoc = ?", String.class, id));
        }

        @Test
        @DisplayName("[Exception Path] don_thuoc_id không tồn tại trả về 8003")
        void prescriptionNotFound() throws Exception {
            expect(send(put(STOP), doctorToken,
                    "{\"don_thuoc_id\": %d, \"instructions\": []}".formatted(MISSING_ID)), 404, "8003");
        }

        @Test
        @DisplayName("[Unhappy Path] Thiếu don_thuoc_id trả về 2001 và nêu tên field")
        void missingPrescriptionId() throws Exception {
            expectMissing(send(put(STOP), doctorToken,
                    "{\"instructions\": [{\"thuoc_can_ngung\": \"A\", \"muc_dich\": \"Test kich thich\"}]}"),
                    "Thiếu tham số bắt buộc: don_thuoc_id");
        }

        @Test
        @DisplayName("[Unhappy Path] Không gửi instructions trả về 2001 và không xoá chỉ dẫn cũ")
        void missingInstructions() throws Exception {
            long id = givenPrescription();
            jdbcTemplate.update("INSERT INTO chidanngungthuoc (ma_don_thuoc, thuoc_can_ngung) VALUES (?, 'A')", id);

            expectMissing(send(put(STOP), doctorToken, "{\"don_thuoc_id\": %d}".formatted(id)),
                    "Thiếu tham số bắt buộc: instructions (gửi mảng rỗng để xoá toàn bộ chỉ dẫn)");
            assertEquals(1, count("SELECT count(*) FROM chidanngungthuoc WHERE ma_don_thuoc = ?", id));
        }

        @Test
        @DisplayName("[Unhappy Path] Chỉ dẫn thứ 2 thiếu thuoc_can_ngung trả về 2001 và không xoá chỉ dẫn cũ")
        void instructionMissingMedication() throws Exception {
            long id = givenPrescription();
            jdbcTemplate.update("INSERT INTO chidanngungthuoc (ma_don_thuoc, thuoc_can_ngung) VALUES (?, 'Cu')", id);

            expectMissing(send(put(STOP), doctorToken, """
                    {"don_thuoc_id": %d, "instructions": [
                      {"thuoc_can_ngung": "Moi", "muc_dich": "Test kich thich"},
                      {"thoi_gian_ngung_ngay": 3, "muc_dich": "Test kich thich"}]}
                    """.formatted(id)), "Chỉ dẫn thứ 2: thuoc_can_ngung là bắt buộc");
            assertEquals("Cu", query(
                    "SELECT string_agg(thuoc_can_ngung, ',') FROM chidanngungthuoc WHERE ma_don_thuoc = ?", String.class, id));
            assertFalse(query(
                    "SELECT ngung_thuoc_lan_sau FROM donthuoc WHERE ma_don_thuoc = ?", Boolean.class, id));
        }
    }

    // ── PRESCRIPTIONS_GET_STOP_MEDICATION ──

    @Nested
    @DisplayName("PRESCRIPTIONS_GET_STOP_MEDICATION GET /prescriptions/stop-instructions")
    class GetStopMedication {

        @Test
        @DisplayName("[Happy Path] Đơn có 2 chỉ dẫn trả về mảng 2 phần tử")
        void returnsInstructions() throws Exception {
            long id = givenPrescription();
            jdbcTemplate.update("INSERT INTO chidanngungthuoc (ma_don_thuoc, thuoc_can_ngung) VALUES (?, 'A'), (?, 'B')", id, id);

            ResultActions result = send(get(STOP).param("don_thuoc_id", String.valueOf(id)), doctorToken, null);

            expect(result, 200, "1000");
            result.andExpect(jsonPath("$.data.instructions.length()").value(2))
                    .andExpect(jsonPath("$.data.instructions[0].thuoc_can_ngung").value("A"));
        }

        @Test
        @DisplayName("[Alternate Path] Đơn không có chỉ dẫn trả về mảng rỗng")
        void returnsEmptyList() throws Exception {
            long id = givenPrescription();

            ResultActions result = send(get(STOP).param("don_thuoc_id", String.valueOf(id)), doctorToken, null);

            expect(result, 200, "1000");
            result.andExpect(jsonPath("$.data.instructions.length()").value(0));
        }

        @Test
        @DisplayName("[Exception Path] don_thuoc_id không tồn tại trả về 8003")
        void prescriptionNotFound() throws Exception {
            expect(send(get(STOP).param("don_thuoc_id", String.valueOf(MISSING_ID)), doctorToken, null), 404, "8003");
        }

        @Test
        @DisplayName("[Unhappy Path] Thiếu don_thuoc_id trả về 2001")
        void missingPrescriptionId() throws Exception {
            expect(send(get(STOP), doctorToken, null), 400, "2001");
        }
    }
}
