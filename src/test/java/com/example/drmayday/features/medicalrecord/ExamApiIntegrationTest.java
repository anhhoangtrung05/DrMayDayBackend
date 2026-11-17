package com.example.drmayday.features.medicalrecord;

import com.example.drmayday.features.security.JwtTokenProvider;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Kiểm thử API 46 đến 49 (spec 0004) qua filter chain thật, xuống PostgreSQL thật. Mỗi test được rollback.
 * Đồng hồ cố định ở 00:30 ngày 2026-11-16 giờ Việt Nam, lúc đó giờ UTC vẫn là ngày 2026-11-15,
 * nên "hôm nay" tính sai múi giờ sẽ làm hỏng các ca về ngày.
 * Khóa ngoại subtype là DEFERRED và không bao giờ được kiểm tra ở đây; ca commit nằm ở ExamTransactionIntegrationTest.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ExtendWith(OutputCaptureExtension.class)
class ExamApiIntegrationTest {

    private static final String EXAMS = "/api/v1/exams";
    private static final String TODAY = "2026-11-16";
    private static final long DOCTOR_ACCOUNT = 6L;
    private static final long ADMIN_ACCOUNT = 1L;

    @TestConfiguration
    static class FixedClock {
        @Bean
        @Primary
        Clock fixedClinicClock() {
            return Clock.fixed(Instant.parse("2026-11-15T17:30:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PersistenceContext
    private EntityManager entityManager;

    private String adminToken;
    private String doctorToken;
    private long patient;
    private String patientToken;
    private long otherPatient;

    @BeforeEach
    void setUp() {
        adminToken = jwtTokenProvider.generateToken(ADMIN_ACCOUNT, "0900000001", "admin");
        doctorToken = jwtTokenProvider.generateToken(DOCTOR_ACCOUNT, "0900000006", "bac_si");
        patient = givenPatient("Nguyễn Văn Chủ Hồ Sơ");
        patientToken = tokenForPatientAccountOf(patient);
        otherPatient = givenPatient("Trần Thị Người Khác");
    }

    // --- dữ liệu ---

    private static String randomPhone() {
        return "09" + String.format("%08d", ThreadLocalRandom.current().nextInt(100_000_000));
    }

    private long givenPatient(String name) {
        Long id = jdbcTemplate.queryForObject(
                "INSERT INTO nguoi (ho_ten, so_dien_thoai) VALUES (?, ?) RETURNING ma_nguoi", Long.class, name, randomPhone());
        jdbcTemplate.update("INSERT INTO benhnhan (ma_benh_nhan) VALUES (?)", id);
        return id;
    }

    private String tokenForPatientAccountOf(long personId) {
        String phone = jdbcTemplate.queryForObject("SELECT so_dien_thoai FROM nguoi WHERE ma_nguoi = ?", String.class, personId);
        Long account = jdbcTemplate.queryForObject("""
                INSERT INTO taikhoan (ma_nguoi, ma_vai_tro, ten_dang_nhap, mat_khau_ma_hoa)
                SELECT ?, ma_vai_tro, ?, 'x' FROM vaitro WHERE ten_vai_tro = 'benh_nhan' RETURNING ma_tai_khoan
                """, Long.class, personId, phone);
        return jwtTokenProvider.generateToken(account, phone, "benh_nhan");
    }

    private long givenRecord(long patientId, String type, String status) {
        Long record = jdbcTemplate.queryForObject(
                "INSERT INTO hosobenhan (ma_benh_nhan, loai_benh_an, trang_thai) VALUES (?, ?, ?) RETURNING ma_ho_so",
                Long.class, patientId, type, status);
        jdbcTemplate.update("INSERT INTO " + ("CAP".equals(type) ? "benhanmaydaycap" : "benhanmaydayman")
                + " (ma_ho_so) VALUES (?)", record);
        return record;
    }

    private long givenRecord(String type) {
        return givenRecord(patient, type, "in_progress");
    }

    private long givenRoom(boolean branchActive) {
        Long branch = jdbcTemplate.queryForObject(
                "INSERT INTO chinhanh (ten_chi_nhanh, dia_chi, trang_thai) VALUES (?, 'Hà Nội', ?) RETURNING ma_chi_nhanh",
                Long.class, "Chi nhánh " + randomPhone(), branchActive);
        return jdbcTemplate.queryForObject(
                "INSERT INTO phongkham (ma_chi_nhanh, ten_phong_kham) VALUES (?, 'Phòng 101') RETURNING ma_phong_kham",
                Long.class, branch);
    }

    private long personOf(long account) {
        return jdbcTemplate.queryForObject("SELECT ma_nguoi FROM taikhoan WHERE ma_tai_khoan = ?", Long.class, account);
    }

    private int count(String sql, Object... params) {
        Integer rows = jdbcTemplate.queryForObject(sql, Integer.class, params);
        return rows == null ? 0 : rows;
    }

    // --- gọi API ---

    // Mọi request chạy chung transaction của test, nên flush và clear trước mỗi request
    private ResultActions send(MockHttpServletRequestBuilder builder, String token, String json) throws Exception {
        entityManager.flush();
        entityManager.clear();
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        if (json != null) {
            builder.contentType(MediaType.APPLICATION_JSON).content(json);
        }
        return mockMvc.perform(builder);
    }

    private ResultActions create(String json) throws Exception {
        return send(post(EXAMS), doctorToken, json);
    }

    private ResultActions createOn(long record, String extraFields) throws Exception {
        String extra = extraFields.isEmpty() ? "" : "," + extraFields;
        return create("{\"record_id\":\"" + record + "\"" + extra + "}");
    }

    private long createdExam(long record, String extraFields) throws Exception {
        String body = body(createOn(record, extraFields).andExpect(status().isCreated()));
        return Long.parseLong(JsonPath.read(body, "$.data.exam_id"));
    }

    private ResultActions update(long exam, String json) throws Exception {
        return send(put(EXAMS + "/" + exam), doctorToken, json);
    }

    private static String body(ResultActions result) throws Exception {
        return result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    // --- API 46 ---

    @Test
    @DisplayName("AC-1, AC-2, AC-5, AC-17: bác sĩ tạo phiên CAP, có đúng một dòng phienkhamcap rỗng và đủ mặc định")
    void createsAcuteExamWithEmptySubtypeAndDefaults() throws Exception {
        long record = givenRecord("CAP");

        String body = body(createOn(record, "\"ma_bac_si\":\"999999\"")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("1001"))
                .andExpect(jsonPath("$.message").value("Khởi tạo phiên khám thành công"))
                .andExpect(jsonPath("$.data.record_id").value(String.valueOf(record)))
                .andExpect(jsonPath("$.data.ma_bac_si").value(String.valueOf(personOf(DOCTOR_ACCOUNT))))
                .andExpect(jsonPath("$.data.ma_phong_kham").value(nullValue()))
                .andExpect(jsonPath("$.data.loai_benh_an").value("CAP"))
                .andExpect(jsonPath("$.data.ngay_kham").value(TODAY))
                .andExpect(jsonPath("$.data.lan_kham_thu").value(1))
                .andExpect(jsonPath("$.data.loai_phien_kham").value("Kham lan dau"))
                .andExpect(jsonPath("$.data.luu_huyet_thanh").value(false))
                .andExpect(jsonPath("$.data.sot").value(nullValue()))
                .andExpect(jsonPath("$.data.sot_nhiet_do_c").value(nullValue()))
                .andExpect(jsonPath("$.data.ngay_hen_tai_kham").value(nullValue()))
                .andExpect(jsonPath("$.data.tom_tat_buoi_kham").value(nullValue()))
                .andExpect(jsonPath("$.data.huong_xu_tri_lan_sau").value(nullValue()))
                .andExpect(jsonPath("$.data.dan_do").value(nullValue())));
        String examId = JsonPath.read(body, "$.data.exam_id");

        Map<String, Object> subtype = jdbcTemplate.queryForMap("SELECT * FROM phienkhamcap WHERE ma_phien_kham = ?",
                Long.parseLong(examId));
        subtype.forEach((column, value) -> {
            if (!column.equals("ma_phien_kham") && !column.equals("loai_benh_an")) {
                assertThat(value).as(column).isNull();
            }
        });
        assertThat(count("SELECT count(*) FROM phienkhamman WHERE ma_phien_kham = ?", Long.parseLong(examId))).isZero();
    }

    @Test
    @DisplayName("AC-1, AC-3, AC-5: phiên MAN thứ hai có lan_kham_thu 2, loai_phien_kham Tai kham, dòng phienkhamman")
    void secondChronicExamIsFollowUp() throws Exception {
        long record = givenRecord("MAN");
        createdExam(record, "");

        String body = body(createOn(record, "")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.lan_kham_thu").value(2))
                .andExpect(jsonPath("$.data.loai_phien_kham").value("Tai kham"))
                .andExpect(jsonPath("$.data.loai_benh_an").value("MAN")));
        long examId = Long.parseLong(JsonPath.read(body, "$.data.exam_id"));
        assertThat(count("SELECT count(*) FROM phienkhamman WHERE ma_phien_kham = ?", examId)).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM phienkhamcap WHERE ma_phien_kham = ?", examId)).isZero();
    }

    @Test
    @DisplayName("AC-2: Admin tạo phiên thì phiên đứng tên Admin")
    void adminCreatesExamUnderOwnName() throws Exception {
        long record = givenRecord("CAP");
        send(post(EXAMS), adminToken, "{\"record_id\":\"" + record + "\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.ma_bac_si").value(String.valueOf(personOf(ADMIN_ACCOUNT))));
    }

    @Test
    @DisplayName("AC-3: lan_kham_thu nhảy cóc được, trùng trả 4009, nhỏ hơn hoặc bằng 0 trả 2003")
    void explicitVisitNumber() throws Exception {
        long record = givenRecord("CAP");
        createdExam(record, "");
        createOn(record, "\"lan_kham_thu\":5")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.lan_kham_thu").value(5))
                .andExpect(jsonPath("$.data.loai_phien_kham").value("Tai kham"));
        createOn(record, "\"lan_kham_thu\":5").andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("4009"));
        createOn(record, "\"lan_kham_thu\":0").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("2003"));
        createOn(record, "\"lan_kham_thu\":-1").andExpect(jsonPath("$.code").value("2003"));
        createOn(record, "").andExpect(jsonPath("$.data.lan_kham_thu").value(6));
    }

    @Test
    @DisplayName("AC-5, AC-7, AC-17: giá trị gửi lên được lưu; null tường minh như không gửi; ghi chú rỗng thành NULL")
    void storesSentValues() throws Exception {
        long record = givenRecord("CAP");
        String body = body(createOn(record, """
                "ngay_kham":"2026-11-10","loai_phien_kham":"Tai kham","luu_huyet_thanh":true,
                "sot":true,"sot_nhiet_do_c":39,"ngay_hen_tai_kham":"2026-11-20",
                "tom_tat_buoi_kham":"  Ổn định  ","huong_xu_tri_lan_sau":"   ","dan_do":"","ma_phong_kham":null
                """)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.ngay_kham").value("2026-11-10"))
                .andExpect(jsonPath("$.data.loai_phien_kham").value("Tai kham"))
                .andExpect(jsonPath("$.data.luu_huyet_thanh").value(true))
                .andExpect(jsonPath("$.data.sot").value(true))
                .andExpect(jsonPath("$.data.ngay_hen_tai_kham").value("2026-11-20"))
                .andExpect(jsonPath("$.data.tom_tat_buoi_kham").value("  Ổn định  "))
                .andExpect(jsonPath("$.data.huong_xu_tri_lan_sau").value(nullValue()))
                .andExpect(jsonPath("$.data.dan_do").value(nullValue())));
        assertThat(body).contains("\"sot_nhiet_do_c\":39.0");

        createOn(record, "\"ngay_kham\":null,\"luu_huyet_thanh\":null,\"loai_phien_kham\":null")
                .andExpect(jsonPath("$.data.ngay_kham").value(TODAY))
                .andExpect(jsonPath("$.data.luu_huyet_thanh").value(false))
                .andExpect(jsonPath("$.data.loai_phien_kham").value("Tai kham"));
    }

    @ParameterizedTest
    @DisplayName("AC-6: mỗi luật giá trị của API 46 trả 2003")
    @ValueSource(strings = {
            "\"loai_phien_kham\":\"kham lan dau\"",
            "\"loai_phien_kham\":\"Khac\"",
            "\"ngay_kham\":\"2026-11-17\"",
            "\"ngay_kham\":\"2026-11-10\",\"ngay_hen_tai_kham\":\"2026-11-09\"",
            "\"ngay_hen_tai_kham\":\"2026-11-15\"",
            "\"ngay_kham\":\"16/11/2026\"",
            "\"sot\":true",
            "\"sot_nhiet_do_c\":38.5",
            "\"sot\":false,\"sot_nhiet_do_c\":38.5",
            "\"sot\":true,\"sot_nhiet_do_c\":29.9",
            "\"sot\":true,\"sot_nhiet_do_c\":45.1",
            "\"sot\":true,\"sot_nhiet_do_c\":38.55",
            "\"lan_kham_thu\":\"abc\"",
            "\"lan_kham_thu\":99999999999",
            "\"sot\":\"maybe\"",
            "\"luu_huyet_thanh\":\"maybe\"",
            "\"sot_nhiet_do_c\":\"abc\""
    })
    void createRejectsInvalidValues(String fields) throws Exception {
        long record = givenRecord("CAP");
        createOn(record, fields)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("2003"));
        assertThat(count("SELECT count(*) FROM phienkham WHERE ma_ho_so = ?", record)).isZero();
    }

    @Test
    @DisplayName("AC-6: biên hợp lệ: ngày hôm nay theo giờ Việt Nam, 30.0, 45.0, 38.50, chuỗi số như API 34")
    void createAcceptsBoundaries() throws Exception {
        long record = givenRecord("CAP");
        createOn(record, "\"ngay_kham\":\"" + TODAY + "\",\"ngay_hen_tai_kham\":\"" + TODAY + "\"")
                .andExpect(status().isCreated());
        createOn(record, "\"sot\":true,\"sot_nhiet_do_c\":30.0").andExpect(status().isCreated());
        createOn(record, "\"sot\":\"true\",\"sot_nhiet_do_c\":45").andExpect(status().isCreated());
        String body = body(createOn(record, "\"sot\":true,\"sot_nhiet_do_c\":38.50,\"lan_kham_thu\":\"9\"")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.lan_kham_thu").value(9)));
        assertThat(body).contains("\"sot_nhiet_do_c\":38.5");
    }

    @Test
    @DisplayName("AC-8: record_id thiếu hoặc rỗng 2001, sai kiểu 2002, không tồn tại 4001")
    void createRecordIdErrors() throws Exception {
        create("{}").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("2001"));
        create("{\"record_id\":\"   \"}").andExpect(jsonPath("$.code").value("2001"));
        create("{\"record_id\":\"abc\"}").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("2002"));
        create("{\"record_id\":\"-3\"}").andExpect(jsonPath("$.code").value("2002"));
        create("{\"record_id\":\"1.5\"}").andExpect(jsonPath("$.code").value("2002"));
        create("{\"record_id\":\"999999999\"}").andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("4001"));
    }

    @Test
    @DisplayName("AC-8: ma_phong_kham rỗng hoặc chữ 2002, không tồn tại 2103, chi nhánh ngừng 2106, hợp lệ thì lưu")
    void createRoomErrors() throws Exception {
        long record = givenRecord("CAP");
        long activeRoom = givenRoom(true);
        long inactiveRoom = givenRoom(false);

        createOn(record, "\"ma_phong_kham\":\"\"").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("2002"));
        createOn(record, "\"ma_phong_kham\":\"  \"").andExpect(jsonPath("$.code").value("2002"));
        createOn(record, "\"ma_phong_kham\":\"abc\"").andExpect(jsonPath("$.code").value("2002"));
        createOn(record, "\"ma_phong_kham\":\"999999999\"").andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("2103"));
        createOn(record, "\"ma_phong_kham\":\"" + inactiveRoom + "\"")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("2106"));
        createOn(record, "\"ma_phong_kham\":\"" + activeRoom + "\"")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.ma_phong_kham").value(String.valueOf(activeRoom)));
    }

    @ParameterizedTest
    @DisplayName("AC-9: hồ sơ đã đóng: API 46 và 49 trả 4003, kể cả body rỗng, không ghi gì")
    @ValueSource(strings = {"completed", "cancelled"})
    void closedRecordRejectsWrites(String closed) throws Exception {
        long record = givenRecord("CAP");
        long exam = createdExam(record, "");
        jdbcTemplate.update("UPDATE hosobenhan SET trang_thai = ? WHERE ma_ho_so = ?", closed, record);

        createOn(record, "\"ngay_kham\":\"2030-01-01\"")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("4003"));
        update(exam, "{}").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("4003"));
        update(exam, "{\"dan_do\":\"x\"}").andExpect(jsonPath("$.code").value("4003"));
        assertThat(count("SELECT count(*) FROM phienkham WHERE ma_ho_so = ?", record)).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM phienkham WHERE ma_ho_so = ? AND dan_do IS NULL", record)).isEqualTo(1);
    }

    // --- API 47, 48 ---

    @Test
    @DisplayName("AC-10: danh sách theo lan_kham_thu tăng dần, có bác sĩ và phòng; hồ sơ chưa có phiên trả mảng rỗng")
    void listsExamsInOrder() throws Exception {
        long record = givenRecord("CAP");
        long room = givenRoom(true);
        createdExam(record, "\"lan_kham_thu\":3,\"ma_phong_kham\":\"" + room + "\",\"sot\":true,\"sot_nhiet_do_c\":38.5");
        createdExam(record, "\"lan_kham_thu\":1,\"tom_tat_buoi_kham\":\"Lần đầu\"");
        String doctorName = jdbcTemplate.queryForObject("SELECT ho_ten FROM nguoi WHERE ma_nguoi = ?",
                String.class, personOf(DOCTOR_ACCOUNT));

        String body = body(send(get(EXAMS).param("record_id", String.valueOf(record)), doctorToken, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("1000"))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].lan_kham_thu").value(1))
                .andExpect(jsonPath("$.data[0].phong_kham").value(nullValue()))
                .andExpect(jsonPath("$.data[0].tom_tat_buoi_kham").value("Lần đầu"))
                .andExpect(jsonPath("$.data[0].bac_si.ma_bac_si").value(String.valueOf(personOf(DOCTOR_ACCOUNT))))
                .andExpect(jsonPath("$.data[0].bac_si.ho_ten_bac_si").value(doctorName))
                .andExpect(jsonPath("$.data[1].lan_kham_thu").value(3))
                .andExpect(jsonPath("$.data[1].phong_kham.ma_phong_kham").value(String.valueOf(room)))
                .andExpect(jsonPath("$.data[1].phong_kham.ten_phong_kham").value("Phòng 101"))
                .andExpect(jsonPath("$.data[1].sot").value(true)));
        List<Map<String, Object>> items = JsonPath.read(body, "$.data");
        assertThat(items.get(0).keySet()).containsExactlyInAnyOrder("exam_id", "record_id", "lan_kham_thu", "ngay_kham",
                "loai_phien_kham", "bac_si", "phong_kham", "luu_huyet_thanh", "sot", "sot_nhiet_do_c",
                "ngay_hen_tai_kham", "tom_tat_buoi_kham");

        long empty = givenRecord(patient, "MAN", "draft");
        send(get(EXAMS).param("record_id", String.valueOf(empty)), doctorToken, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @DisplayName("AC-8: API 47 record_id thiếu 2001, sai kiểu 2002, không tồn tại 4001")
    void listRecordIdErrors() throws Exception {
        send(get(EXAMS), doctorToken, null).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("2001"));
        send(get(EXAMS).param("record_id", " "), doctorToken, null).andExpect(jsonPath("$.code").value("2001"));
        send(get(EXAMS).param("record_id", "abc"), doctorToken, null).andExpect(jsonPath("$.code").value("2002"));
        send(get(EXAMS).param("record_id", "999999999"), doctorToken, null)
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("4001"));
    }

    @Test
    @DisplayName("AC-11: chi tiết CAP và MAN: chi_tiet_lam_sang có đúng các cột của bảng subtype")
    void detailHasSubtypeColumns() throws Exception {
        long acute = createdExam(givenRecord("CAP"), "\"sot\":true,\"sot_nhiet_do_c\":39");
        long chronic = createdExam(givenRecord("MAN"), "\"dan_do\":\"Tái khám sau 2 tuần\"");

        String acuteBody = body(send(get(EXAMS + "/" + acute), doctorToken, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("1000"))
                .andExpect(jsonPath("$.data.loai_benh_an").value("CAP"))
                .andExpect(jsonPath("$.data.huong_xu_tri_lan_sau").value(nullValue()))
                .andExpect(jsonPath("$.data.chi_tiet_lam_sang.so_lan_non").value(nullValue())));
        assertThat(acuteBody).contains("\"sot_nhiet_do_c\":39.0");
        assertFindingsMatchTable(acuteBody, "phienkhamcap");

        String chronicBody = body(send(get(EXAMS + "/" + chronic), doctorToken, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.loai_benh_an").value("MAN"))
                .andExpect(jsonPath("$.data.dan_do").value("Tái khám sau 2 tuần"))
                .andExpect(jsonPath("$.data.bac_si.ho_ten_bac_si").exists()));
        assertFindingsMatchTable(chronicBody, "phienkhamman");

        Map<String, Object> detail = JsonPath.read(chronicBody, "$.data");
        assertThat(detail.keySet()).contains("exam_id", "record_id", "lan_kham_thu", "ngay_kham", "loai_phien_kham",
                "bac_si", "phong_kham", "luu_huyet_thanh", "sot", "sot_nhiet_do_c", "ngay_hen_tai_kham",
                "tom_tat_buoi_kham", "loai_benh_an", "huong_xu_tri_lan_sau", "dan_do", "chi_tiet_lam_sang");
    }

    @Test
    @DisplayName("AC-11: chi tiết trả đúng giá trị đã lưu trong phienkhamcap và phienkhamman, kể cả hai cột MAN vừa sửa mapping")
    void detailReadsStoredSubtypeValues() throws Exception {
        long acute = createdExam(givenRecord("CAP"), "");
        long chronic = createdExam(givenRecord("MAN"), "");
        jdbcTemplate.update("UPDATE phienkhamcap SET trieu_chung_da = 'Mẩn đỏ', so_lan_non = 3, co_san_phu = true "
                + "WHERE ma_phien_kham = ?", acute);
        jdbcTemplate.update("""
                UPDATE phienkhamman SET thoi_gian_ton_tai_phu_mach_co_dung_thuoc = '2 ngày',
                    thoi_gian_ton_tai_phu_mach_khong_dung_thuoc = '5 ngày', diem_aas7 = 42,
                    thoi_gian_ngau_nhien_tuan = 12.5, phu_vi_tri_khac = true, phu_vi_tri_khac_ghi_ro = 'Lưng'
                WHERE ma_phien_kham = ?""", chronic);

        send(get(EXAMS + "/" + acute), doctorToken, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.chi_tiet_lam_sang.trieu_chung_da").value("Mẩn đỏ"))
                .andExpect(jsonPath("$.data.chi_tiet_lam_sang.so_lan_non").value(3))
                .andExpect(jsonPath("$.data.chi_tiet_lam_sang.co_san_phu").value(true));
        String body = body(send(get(EXAMS + "/" + chronic), doctorToken, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.chi_tiet_lam_sang.thoi_gian_ton_tai_phu_mach_co_dung_thuoc").value("2 ngày"))
                .andExpect(jsonPath("$.data.chi_tiet_lam_sang.thoi_gian_ton_tai_phu_mach_khong_dung_thuoc").value("5 ngày"))
                .andExpect(jsonPath("$.data.chi_tiet_lam_sang.diem_aas7").value(42))
                .andExpect(jsonPath("$.data.chi_tiet_lam_sang.phu_vi_tri_khac_ghi_ro").value("Lưng")));
        assertThat(body).contains("\"thoi_gian_ngau_nhien_tuan\":12.50");
    }

    private void assertFindingsMatchTable(String body, String table) {
        Map<String, Object> findings = JsonPath.read(body, "$.data.chi_tiet_lam_sang");
        List<String> columns = jdbcTemplate.queryForList("""
                SELECT column_name FROM information_schema.columns
                WHERE table_schema = 'public' AND table_name = ? AND column_name NOT IN ('ma_phien_kham', 'loai_benh_an')
                """, String.class, table);
        assertThat(findings.keySet()).containsExactlyInAnyOrderElementsOf(columns);
        assertThat(findings.values()).allMatch(value -> value == null);
    }

    @Test
    @DisplayName("AC-11: dòng subtype không có thì chi_tiet_lam_sang là null; id sai hoặc không tồn tại trả 5008")
    void detailEdgeCases() throws Exception {
        long exam = createdExam(givenRecord("CAP"), "");
        jdbcTemplate.update("DELETE FROM phienkhamcap WHERE ma_phien_kham = ?", exam);
        send(get(EXAMS + "/" + exam), doctorToken, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.chi_tiet_lam_sang").value(nullValue()));

        send(get(EXAMS + "/abc"), doctorToken, null).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("5008"));
        send(get(EXAMS + "/999999999"), doctorToken, null).andExpect(jsonPath("$.code").value("5008"));
    }

    @Test
    @DisplayName("AC-12: bệnh nhân đọc được phiên của mình, của người khác trả 3106; 4001 và 5008 trước 3106")
    void patientReadsOnlyOwnExams() throws Exception {
        long own = givenRecord(patient, "CAP", "in_progress");
        long ownExam = createdExam(own, "");
        long others = givenRecord(otherPatient, "MAN", "in_progress");
        long othersExam = createdExam(others, "");

        send(get(EXAMS).param("record_id", String.valueOf(own)), patientToken, null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(1));
        send(get(EXAMS + "/" + ownExam), patientToken, null).andExpect(status().isOk());

        send(get(EXAMS).param("record_id", String.valueOf(others)), patientToken, null)
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("3106"));
        send(get(EXAMS + "/" + othersExam), patientToken, null)
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("3106"));

        send(get(EXAMS).param("record_id", "999999999"), patientToken, null).andExpect(jsonPath("$.code").value("4001"));
        send(get(EXAMS + "/999999999"), patientToken, null).andExpect(jsonPath("$.code").value("5008"));

        String ghost = jwtTokenProvider.generateToken(999_999_999L, "0999999999", "benh_nhan");
        send(get(EXAMS + "/" + ownExam), ghost, null).andExpect(jsonPath("$.code").value("3106"));
    }

    // --- API 49 ---

    @Test
    @DisplayName("AC-13: sửa kiểu gộp; body rỗng trả dữ liệu hiện tại; trường bất biến gửi lên bị bỏ qua")
    void updateMerges() throws Exception {
        long record = givenRecord("CAP");
        long exam = createdExam(record, "\"tom_tat_buoi_kham\":\"Cũ\",\"ngay_hen_tai_kham\":\"2026-11-30\"");

        update(exam, "{}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("1000"))
                .andExpect(jsonPath("$.data.tom_tat_buoi_kham").value("Cũ"));

        update(exam, """
                {"dan_do":"Uống nhiều nước","ngay_hen_tai_kham":null,"lan_kham_thu":7,"record_id":"1",
                 "loai_benh_an":"MAN","ma_bac_si":"1"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Cập nhật phiên khám thành công"))
                .andExpect(jsonPath("$.data.dan_do").value("Uống nhiều nước"))
                .andExpect(jsonPath("$.data.tom_tat_buoi_kham").value("Cũ"))
                .andExpect(jsonPath("$.data.ngay_hen_tai_kham").value("2026-11-30"))
                .andExpect(jsonPath("$.data.lan_kham_thu").value(1))
                .andExpect(jsonPath("$.data.record_id").value(String.valueOf(record)))
                .andExpect(jsonPath("$.data.loai_benh_an").value("CAP"))
                .andExpect(jsonPath("$.data.ma_bac_si").value(String.valueOf(personOf(DOCTOR_ACCOUNT))));

        update(exam, "{\"tom_tat_buoi_kham\":\"  \"}").andExpect(jsonPath("$.data.tom_tat_buoi_kham").value(nullValue()));

        update(999_999_999L, "{}").andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("5008"));
        send(put(EXAMS + "/abc"), doctorToken, "{}").andExpect(jsonPath("$.code").value("5008"));
    }

    @Test
    @DisplayName("AC-14: sot false xóa nhiệt độ; sot false kèm nhiệt độ 2003; chỉ gửi nhiệt độ hợp lệ khi sot đã là true")
    void updateTemperatureRules() throws Exception {
        long exam = createdExam(givenRecord("CAP"), "\"sot\":true,\"sot_nhiet_do_c\":39");

        update(exam, "{\"sot_nhiet_do_c\":38.2}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.sot_nhiet_do_c").value(38.2));
        update(exam, "{\"sot\":false,\"sot_nhiet_do_c\":38.5}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("2003"));
        update(exam, "{\"sot\":false}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sot").value(false))
                .andExpect(jsonPath("$.data.sot_nhiet_do_c").value(nullValue()));
        update(exam, "{\"sot_nhiet_do_c\":38.5}").andExpect(jsonPath("$.code").value("2003"));
        update(exam, "{\"sot\":true}").andExpect(jsonPath("$.code").value("2003"));
    }

    @Test
    @DisplayName("AC-6: dòng cũ sai luật vẫn sửa được trường khác; luật ngày chạy trên giá trị cuối")
    void updateRulesRunOnlyForSentFields() throws Exception {
        long record = givenRecord("CAP");
        jdbcTemplate.update("""
                INSERT INTO phienkham (ma_ho_so, ma_bac_si, loai_benh_an, ngay_kham, lan_kham_thu, loai_phien_kham, sot)
                VALUES (?, ?, 'CAP', '2026-12-31', 1, 'Cu', true)
                """, record, personOf(DOCTOR_ACCOUNT));
        Long legacy = jdbcTemplate.queryForObject("SELECT ma_phien_kham FROM phienkham WHERE ma_ho_so = ?", Long.class, record);
        jdbcTemplate.update("INSERT INTO phienkhamcap (ma_phien_kham) VALUES (?)", legacy);

        update(legacy, "{\"dan_do\":\"Vẫn sửa được\"}").andExpect(status().isOk());

        long exam = createdExam(record, "\"ngay_kham\":\"2026-11-10\",\"ngay_hen_tai_kham\":\"2026-11-12\"");
        update(exam, "{\"ngay_kham\":\"2026-11-13\"}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("2003"));
        update(exam, "{\"ngay_hen_tai_kham\":\"2026-11-09\"}").andExpect(jsonPath("$.code").value("2003"));
        update(exam, "{\"ngay_kham\":\"2026-11-17\",\"ngay_hen_tai_kham\":\"2026-11-20\"}")
                .andExpect(jsonPath("$.code").value("2003"));
        update(exam, "{\"loai_phien_kham\":\"Khac\"}").andExpect(jsonPath("$.code").value("2003"));
        update(exam, "{\"ngay_kham\":\"2026-11-11\",\"loai_phien_kham\":\"Kham lan dau\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ngay_kham").value("2026-11-11"))
                .andExpect(jsonPath("$.data.loai_phien_kham").value("Kham lan dau"));
    }

    @Test
    @DisplayName("AC-8, AC-14: đổi phòng kiểm tra phòng và chi nhánh; gửi lại phòng hiện tại không kiểm tra lại")
    void updateRoom() throws Exception {
        long record = givenRecord("CAP");
        long activeRoom = givenRoom(true);
        long inactiveRoom = givenRoom(false);
        long exam = createdExam(record, "");

        update(exam, "{\"ma_phong_kham\":\"\"}").andExpect(jsonPath("$.code").value("2002"));
        update(exam, "{\"ma_phong_kham\":\"999999999\"}").andExpect(jsonPath("$.code").value("2103"));
        update(exam, "{\"ma_phong_kham\":\"" + inactiveRoom + "\"}").andExpect(jsonPath("$.code").value("2106"));
        update(exam, "{\"ma_phong_kham\":\"" + activeRoom + "\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ma_phong_kham").value(String.valueOf(activeRoom)));

        jdbcTemplate.update("UPDATE phienkham SET ma_phong_kham = ? WHERE ma_phien_kham = ?", inactiveRoom, exam);
        update(exam, "{\"ma_phong_kham\":\"" + inactiveRoom + "\",\"dan_do\":\"x\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ma_phong_kham").value(String.valueOf(inactiveRoom)));
    }

    // --- phân quyền ---

    @Test
    @DisplayName("AC-16: benh_nhan không gọi được API 46, 49; dieu_duong không gọi được API nào, kể cả body sai")
    void rolesAreEnforced() throws Exception {
        long record = givenRecord("CAP");
        long exam = createdExam(record, "");
        String nurseToken = jwtTokenProvider.generateToken(3L, "0900000003", "dieu_duong");

        send(post(EXAMS), patientToken, "{\"record_id\":\"abc\"}")
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("3102"));
        send(put(EXAMS + "/" + exam), patientToken, "{\"sot\":\"x\"}")
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("3102"));

        send(post(EXAMS), nurseToken, "{bad json").andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("3102"));
        send(get(EXAMS), nurseToken, null).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("3102"));
        send(get(EXAMS + "/" + exam), nurseToken, null).andExpect(jsonPath("$.code").value("3102"));
        send(put(EXAMS + "/" + exam), nurseToken, "{}").andExpect(jsonPath("$.code").value("3102"));

        send(get(EXAMS).param("record_id", String.valueOf(record)), null, null).andExpect(status().isUnauthorized());
        send(post(EXAMS), null, "{}").andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("AC-16: DELETE và PATCH trên /api/v1/exams trả 405, mã 2007, header Allow (spec 0005)")
    void unsupportedMethods() throws Exception {
        long exam = createdExam(givenRecord("CAP"), "");
        String allow = send(delete(EXAMS + "/" + exam), doctorToken, null)
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("2007"))
                .andReturn().getResponse().getHeader("Allow");
        assertThat(allow).isNotNull();
        assertThat(List.of(allow.split(",\\s*"))).contains("GET", "PUT").doesNotContain("DELETE", "PATCH");
        send(patch(EXAMS + "/" + exam), doctorToken, "{}")
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("2007"));
        send(delete(EXAMS), doctorToken, null).andExpect(jsonPath("$.code").value("2007"));
    }

    @Test
    @DisplayName("AC-18: tạo và sửa ghi log INFO tiếng Việt có examId, recordId và taiKhoan, không ghi nội dung y tế")
    void writesAreLogged(CapturedOutput output) throws Exception {
        long record = givenRecord("CAP");
        long exam = createdExam(record, "\"dan_do\":\"Bí mật y tế\"");
        update(exam, "{\"tom_tat_buoi_kham\":\"Nội dung riêng\"}").andExpect(status().isOk());

        assertThat(output.getOut())
                .contains("Tạo phiên khám: examId=" + exam + ", recordId=" + record)
                .contains("Sửa phiên khám: examId=" + exam + ", recordId=" + record + ", taiKhoan=" + DOCTOR_ACCOUNT)
                .doesNotContain("Bí mật y tế")
                .doesNotContain("Nội dung riêng");
    }
}
