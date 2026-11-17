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
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Kiểm thử API 34 đến 39 qua filter chain thật, xuống PostgreSQL thật.
 * Mỗi test nằm trong một transaction được rollback, nên không để lại dòng nào. Khóa ngoại subtype là DEFERRED,
 * chỉ kiểm tra lúc commit, nên phiên khám chèn ở đây không cần dòng phienkhamcap.
 * Ca xóa bị khóa ngoại chặn (4007) nằm ở MedicalRecordTransactionIntegrationTest, vì lỗi database làm hỏng transaction của test.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ExtendWith(OutputCaptureExtension.class)
class MedicalRecordApiIntegrationTest {

    private static final String RECORDS = "/api/v1/medical-records";
    private static final long DOCTOR_ACCOUNT = 6L;

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
        adminToken = jwtTokenProvider.generateToken(1L, "0900000001", "admin");
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

    // Tài khoản mới có ma_tai_khoan khác ma_nguoi, đúng tình huống dễ nhầm hai loại id
    private String tokenForPatientAccountOf(long personId) {
        String phone = jdbcTemplate.queryForObject("SELECT so_dien_thoai FROM nguoi WHERE ma_nguoi = ?", String.class, personId);
        Long account = jdbcTemplate.queryForObject("""
                INSERT INTO taikhoan (ma_nguoi, ma_vai_tro, ten_dang_nhap, mat_khau_ma_hoa)
                SELECT ?, ma_vai_tro, ?, 'x' FROM vaitro WHERE ten_vai_tro = 'benh_nhan' RETURNING ma_tai_khoan
                """, Long.class, personId, phone);
        assertThat(account).isNotEqualTo(personId);
        return jwtTokenProvider.generateToken(account, phone, "benh_nhan");
    }

    private void givenVisit(long record, String type, String date, int number) {
        jdbcTemplate.update("""
                INSERT INTO phienkham (ma_ho_so, ma_bac_si, loai_benh_an, ngay_kham, lan_kham_thu)
                VALUES (?, (SELECT min(ma_nhan_vien) FROM nhanvien), ?, ?::date, ?)
                """, record, type, date, number);
    }

    private int count(String sql, Object... params) {
        Integer rows = jdbcTemplate.queryForObject(sql, Integer.class, params);
        return rows == null ? 0 : rows;
    }

    // --- gọi API ---

    // Mỗi request HTTP thật có persistence context riêng. Ở đây mọi request chạy chung transaction của test,
    // nên phải flush và clear trước mỗi request, nếu không entity vừa lưu (chưa nạp quan hệ) sẽ bị dùng lại
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

    private ResultActions create(long patientId, String token, String json) throws Exception {
        return send(post("/api/v1/patients/" + patientId + "/medical-records"), token, json);
    }

    private long createdRecord(long patientId, String type) throws Exception {
        String body = create(patientId, doctorToken, "{\"loai_benh_an\":\"" + type + "\"}")
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return Long.parseLong(JsonPath.read(body, "$.data.record_id"));
    }

    private ResultActions update(long id, String json) throws Exception {
        return send(put(RECORDS + "/" + id), doctorToken, json);
    }

    private static String body(ResultActions result) throws Exception {
        return result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    // --- API 34 ---

    @Test
    @DisplayName("AC-1: bác sĩ tạo hồ sơ MAN, nhận 1001, kèm đúng một dòng benhanmaydayman")
    void doctorCreatesChronicRecordWithSubtype() throws Exception {
        ResultActions result = create(patient, doctorToken, """
                {"loai_benh_an":"MAN","thoi_gian_khoi_phat_tuan":1.5,"thoi_diem_kham_lan_dau":"2026-11-15T02:30:00Z",
                 "tien_su_gia_dinh":"Co","tien_su_gia_dinh_ghi_ro":"Mẹ bị mày đay"}
                """);

        result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("1001"))
                .andExpect(jsonPath("$.data.patient_id").value(String.valueOf(patient)))
                .andExpect(jsonPath("$.data.loai_benh_an").value("MAN"))
                .andExpect(jsonPath("$.data.trang_thai").value("draft"))
                .andExpect(jsonPath("$.data.thoi_diem_kham_lan_dau").value("2026-11-15T09:30:00+07:00"))
                .andExpect(jsonPath("$.data.tien_su_gia_dinh_ghi_ro").value("Mẹ bị mày đay"));
        String json = body(result);
        assertThat(json).contains("\"thoi_gian_khoi_phat_tuan\":1.50");
        long record = Long.parseLong(JsonPath.read(json, "$.data.record_id"));
        assertThat(count("SELECT count(*) FROM benhanmaydayman WHERE ma_ho_so = ?", record)).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM benhanmaydaycap WHERE ma_ho_so = ?", record)).isZero();
    }

    @Test
    @DisplayName("AC-1: admin tạo hồ sơ CAP, dòng benhanmaydaycap có thoi_diem_cap_nhat")
    void adminCreatesAcuteRecordWithTimestampedSubtype() throws Exception {
        String json = body(create(patient, adminToken, "{\"loai_benh_an\":\"CAP\"}").andExpect(status().isCreated()));

        long record = Long.parseLong(JsonPath.read(json, "$.data.record_id"));
        assertThat(count("SELECT count(*) FROM benhanmaydaycap WHERE ma_ho_so = ? AND thoi_diem_cap_nhat IS NOT NULL",
                record)).isEqualTo(1);
    }

    @Test
    @DisplayName("AC-2: tien_su_gia_dinh khác Co thì ghi chú được lưu là NULL")
    void familyHistoryNoteIsDroppedWhenAnswerIsNo() throws Exception {
        String json = body(create(patient, doctorToken,
                "{\"loai_benh_an\":\"CAP\",\"tien_su_gia_dinh\":\"Khong\",\"tien_su_gia_dinh_ghi_ro\":\"bỏ qua\"}"));

        long record = Long.parseLong(JsonPath.read(json, "$.data.record_id"));
        assertThat(count("SELECT count(*) FROM hosobenhan WHERE ma_ho_so = ? AND tien_su_gia_dinh_ghi_ro IS NULL", record))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("AC-2: thiếu loai_benh_an trả 2001")
    void missingTypeReturns2001() throws Exception {
        create(patient, doctorToken, "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("2001"));
    }

    @ParameterizedTest(name = "AC-2: body {0} trả 2003")
    @ValueSource(strings = {
            "{\"loai_benh_an\":\"X\"}",
            "{\"loai_benh_an\":\"cap\"}",
            "{\"loai_benh_an\":\"MAN\",\"tien_su_gia_dinh\":\"Co\"}",
            "{\"loai_benh_an\":\"MAN\",\"ket_qua_dieu_tri_bao_hiem\":\"Tot\"}",
            "{\"loai_benh_an\":\"MAN\",\"thoi_gian_khoi_phat_tuan\":10000000000}",
            "{\"loai_benh_an\":\"MAN\",\"ngay_bat_dau_dieu_tri_ngoai_tru\":\"15/11/2026\"}",
            "{\"loai_benh_an\":\"MAN\",\"thoi_diem_kham_lan_dau\":\"2026-11-15T09:30:00\"}",
            "{\"loai_benh_an\":\"MAN\",\"trang_thai\":\"completed\"}",
            "{\"loai_benh_an\":\"MAN\",\"da_dieu_tri_truoc_kham\":\"co le\"}"
    })
    void invalidValuesReturn2003(String json) throws Exception {
        create(patient, doctorToken, json)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("2003"));
        assertThat(count("SELECT count(*) FROM hosobenhan WHERE ma_benh_nhan = ?", patient)).isZero();
    }

    @ParameterizedTest(name = "AC-3: patient_id {0} trả 3007")
    @ValueSource(strings = {"abc", "999999999"})
    void unknownPatientReturns3007(String patientId) throws Exception {
        send(post("/api/v1/patients/" + patientId + "/medical-records"), doctorToken, "{\"loai_benh_an\":\"MAN\"}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("3007"))
                .andExpect(jsonPath("$.message").value("Patient not found"));
    }

    @Test
    @DisplayName("AC-4: hồ sơ MAN thứ hai đang mở trả 4006, nhưng CAP vẫn tạo được")
    void secondOpenRecordOfSameTypeReturns4006() throws Exception {
        createdRecord(patient, "MAN");

        create(patient, doctorToken, "{\"loai_benh_an\":\"MAN\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("4006"));
        create(patient, doctorToken, "{\"loai_benh_an\":\"CAP\"}").andExpect(status().isCreated());
    }

    @Test
    @DisplayName("AC-4: sau khi hồ sơ cũ bị hủy thì tạo được hồ sơ mới cùng loại")
    void newRecordAllowedAfterCancellingTheOpenOne() throws Exception {
        long first = createdRecord(patient, "MAN");
        update(first, "{\"trang_thai\":\"cancelled\"}").andExpect(status().isOk());

        create(patient, doctorToken, "{\"loai_benh_an\":\"MAN\"}").andExpect(status().isCreated());
    }

    // --- API 35 ---

    @Test
    @DisplayName("AC-6: danh sách lọc theo bệnh nhân trả PageResponse có ho_ten")
    void listFilteredByPatientReturnsPageWithNames() throws Exception {
        createdRecord(patient, "MAN");
        createdRecord(patient, "CAP");
        createdRecord(otherPatient, "MAN");

        send(get(RECORDS + "?patient_id=" + patient), doctorToken, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("1000"))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.total_elements").value(2))
                .andExpect(jsonPath("$.data.total_pages").value(1))
                .andExpect(jsonPath("$.data.content[*].ho_ten", everyItem(is("Nguyễn Văn Chủ Hồ Sơ"))))
                .andExpect(jsonPath("$.data.content[*].patient_id", everyItem(is(String.valueOf(patient)))));
    }

    @Test
    @DisplayName("AC-6: bộ lọc kết hợp bằng AND theo loại và trạng thái")
    void filtersCombineWithAnd() throws Exception {
        long man = createdRecord(patient, "MAN");
        createdRecord(patient, "CAP");
        update(man, "{\"trang_thai\":\"pending\"}").andExpect(status().isOk());

        send(get(RECORDS + "?patient_id=" + patient + "&loai_benh_an=MAN&trang_thai=pending"), doctorToken, null)
                .andExpect(jsonPath("$.data.total_elements").value(1))
                .andExpect(jsonPath("$.data.content[0].record_id").value(String.valueOf(man)));
    }

    @Test
    @DisplayName("AC-6: xếp theo thoi_diem_kham_lan_dau giảm dần, NULL cuối, rồi record_id giảm dần")
    void listIsOrderedByFirstVisitThenId() throws Exception {
        long noVisitOlder = createdRecord(patient, "MAN");
        update(noVisitOlder, "{\"trang_thai\":\"cancelled\"}").andExpect(status().isOk());
        long noVisitNewer = createdRecord(patient, "MAN");
        long early = createdRecord(patient, "CAP");
        update(early, "{\"thoi_diem_kham_lan_dau\":\"2026-01-01T08:00:00+07:00\",\"trang_thai\":\"cancelled\"}")
                .andExpect(status().isOk());
        long late = createdRecord(patient, "CAP");
        update(late, "{\"thoi_diem_kham_lan_dau\":\"2026-06-01T08:00:00+07:00\"}").andExpect(status().isOk());

        String json = body(send(get(RECORDS + "?patient_id=" + patient), doctorToken, null));

        List<String> ids = JsonPath.read(json, "$.data.content[*].record_id");
        assertThat(ids).containsExactly(String.valueOf(late), String.valueOf(early),
                String.valueOf(noVisitNewer), String.valueOf(noVisitOlder));
    }

    @Test
    @DisplayName("AC-7: patient_id không có hồ sơ trả trang rỗng, không phải 3007")
    void unknownPatientFilterReturnsEmptyPage() throws Exception {
        send(get(RECORDS + "?patient_id=999999999"), doctorToken, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total_elements").value(0));
    }

    @ParameterizedTest(name = "AC-7: query {0} trả 2003")
    @ValueSource(strings = {"?page=0", "?size=101", "?loai_benh_an=X", "?trang_thai=X", "?patient_id=abc"})
    void invalidQueryReturns2003(String query) throws Exception {
        send(get(RECORDS + query), doctorToken, null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("2003"));
    }

    @Test
    @DisplayName("AC-7: page không phải số nguyên trả 2002")
    void nonIntegerPageReturns2002() throws Exception {
        send(get(RECORDS + "?page=abc"), doctorToken, null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("2002"));
    }

    // --- API 36 ---

    @Test
    @DisplayName("AC-8: chi tiết MAN có diagnosis, previous_episodes null, không có infection_status")
    void chronicDetailHasChronicShape() throws Exception {
        long record = createdRecord(patient, "MAN");

        String json = body(send(get(RECORDS + "/" + record), doctorToken, null).andExpect(status().isOk()));

        assertThat(json).contains("\"previous_episodes\":null", "\"self_history\":null", "\"allergy_context\":null",
                "\"chan_doan_chinh\":null");
        assertThat(json).doesNotContain("infection_status", "cd_phan_ve");
    }

    @Test
    @DisplayName("AC-8: chi tiết CAP đọc được các bảng con thật, không có previous_episodes")
    void acuteDetailReadsChildTables() throws Exception {
        long record = createdRecord(patient, "CAP");
        jdbcTemplate.update("INSERT INTO tinhtrangnhiemtrung (ma_ho_so, ho_hap_ho) VALUES (?, true)", record);
        jdbcTemplate.update("INSERT INTO tiensubanthan (ma_ho_so, tien_su_co_dia, tien_su_co_dia_ghi_ro) VALUES (?, true, 'Hen')",
                record);
        jdbcTemplate.update("INSERT INTO hoancanhkhoiphatdiung (ma_ho_so, thuc_an_nghi_ngo) VALUES (?, 'Tôm')", record);

        send(get(RECORDS + "/" + record), doctorToken, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.infection_status.ho_hap_ho").value(true))
                .andExpect(jsonPath("$.data.self_history.tien_su_co_dia").value(true))
                .andExpect(jsonPath("$.data.self_history.tien_su_co_dia_ghi_ro").value("Hen"))
                .andExpect(jsonPath("$.data.allergy_context.thuc_an_nghi_ngo").value("Tôm"))
                .andExpect(jsonPath("$.data.diagnosis.cd_phan_ve").isEmpty())
                .andExpect(jsonPath("$.data.diagnosis.thoi_diem_cap_nhat")
                        .value(org.hamcrest.Matchers.matchesPattern("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\+07:00")))
                .andExpect(jsonPath("$.data.previous_episodes").doesNotExist());
    }

    @Test
    @DisplayName("AC-8: chi tiết MAN đọc được bệnh sử các đợt trước")
    void chronicDetailReadsPreviousEpisodes() throws Exception {
        long record = createdRecord(patient, "MAN");
        jdbcTemplate.update("""
                INSERT INTO benhsucacdotbenhtruoc (ma_ho_so, tung_co_dot_tuong_tu, bieu_hien_dot_truoc, da_tung_nhap_vien)
                VALUES (?, true, 'Sẩn phù toàn thân', false)
                """, record);

        send(get(RECORDS + "/" + record), doctorToken, null)
                .andExpect(jsonPath("$.data.previous_episodes.tung_co_dot_tuong_tu").value(true))
                .andExpect(jsonPath("$.data.previous_episodes.bieu_hien_dot_truoc").value("Sẩn phù toàn thân"))
                .andExpect(jsonPath("$.data.previous_episodes.da_tung_nhap_vien").value(false));
    }

    @ParameterizedTest(name = "AC-9: id {0} trả 4001 ở API 36, 37, 38")
    @ValueSource(strings = {"abc", "999999999"})
    void unknownIdReturns4001(String id) throws Exception {
        send(get(RECORDS + "/" + id), doctorToken, null).andExpect(jsonPath("$.code").value("4001"));
        send(put(RECORDS + "/" + id), doctorToken, "{}").andExpect(jsonPath("$.code").value("4001"));
        send(delete(RECORDS + "/" + id), doctorToken, null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("4001"));
    }

    @Test
    @DisplayName("AC-10: bệnh nhân xem được hồ sơ của mình nhưng nhận 3106 với hồ sơ người khác")
    void patientSeesOnlyOwnRecords() throws Exception {
        long own = createdRecord(patient, "MAN");
        long foreign = createdRecord(otherPatient, "MAN");

        send(get(RECORDS + "/" + own), patientToken, null).andExpect(status().isOk());
        send(get(RECORDS + "/" + foreign), patientToken, null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("3106"));
    }

    // --- API 37 ---

    @Test
    @DisplayName("AC-11: body rỗng trả dữ liệu hiện tại; loai_benh_an và patient_id gửi lên không có tác dụng")
    void emptyBodyKeepsDataAndIdentityFieldsAreIgnored() throws Exception {
        long record = createdRecord(patient, "MAN");

        update(record, "{}").andExpect(status().isOk()).andExpect(jsonPath("$.data.trang_thai").value("draft"));
        update(record, "{\"trang_thai\":\"in_progress\",\"loai_benh_an\":\"CAP\",\"patient_id\":\"" + otherPatient + "\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.trang_thai").value("in_progress"))
                .andExpect(jsonPath("$.data.loai_benh_an").value("MAN"))
                .andExpect(jsonPath("$.data.patient_id").value(String.valueOf(patient)));
    }

    @Test
    @DisplayName("AC-12: trạng thái không lùi được và giá trị lạ bị từ chối")
    void statusCannotMoveBackward() throws Exception {
        long record = createdRecord(patient, "MAN");
        update(record, "{\"trang_thai\":\"in_progress\"}").andExpect(status().isOk());

        update(record, "{\"trang_thai\":\"draft\"}").andExpect(jsonPath("$.code").value("2003"));
        update(record, "{\"trang_thai\":\"active\"}").andExpect(jsonPath("$.code").value("2003"));
        assertThat(jdbcTemplate.queryForObject("SELECT trang_thai FROM hosobenhan WHERE ma_ho_so = ?", String.class, record))
                .isEqualTo("in_progress");
    }

    @Test
    @DisplayName("AC-13: hồ sơ đã completed trả 4003 cho mọi body")
    void completedRecordIsFrozen() throws Exception {
        long record = createdRecord(patient, "CAP");
        update(record, "{\"trang_thai\":\"completed\"}").andExpect(status().isOk());

        update(record, "{}").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("4003"));
        update(record, "{\"trang_thai\":\"cancelled\"}").andExpect(jsonPath("$.code").value("4003"));
    }

    @Test
    @DisplayName("AC-14: kiểm tra trên giá trị cuối: Khong xóa ghi chú, Co không ghi chú bị từ chối, ngày so với ngày đã lưu")
    void validationRunsOnFinalValues() throws Exception {
        long record = Long.parseLong(JsonPath.read(body(create(patient, doctorToken,
                "{\"loai_benh_an\":\"MAN\",\"tien_su_gia_dinh\":\"Co\",\"tien_su_gia_dinh_ghi_ro\":\"Bố bị\"}")),
                "$.data.record_id"));

        update(record, "{\"tien_su_gia_dinh\":\"Khong\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tien_su_gia_dinh_ghi_ro").isEmpty());
        update(record, "{\"tien_su_gia_dinh\":\"Co\"}").andExpect(jsonPath("$.code").value("2003"));
        update(record, "{\"ngay_bat_dau_dieu_tri_ngoai_tru\":\"2026-11-10\"}").andExpect(status().isOk());
        update(record, "{\"ngay_ket_thuc_dieu_tri_ngoai_tru\":\"2026-11-01\"}").andExpect(jsonPath("$.code").value("2003"));
    }

    // --- API 38 ---

    @Test
    @DisplayName("AC-15: xóa hồ sơ rỗng trả 1000 và xóa cả hồ sơ lẫn subtype")
    void deletingEmptyRecordRemovesRecordAndSubtype() throws Exception {
        long record = createdRecord(patient, "MAN");

        send(delete(RECORDS + "/" + record), doctorToken, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("1000"))
                .andExpect(jsonPath("$.message").value("Xóa hồ sơ thành công"))
                .andExpect(jsonPath("$.data").doesNotExist());
        assertThat(count("SELECT count(*) FROM hosobenhan WHERE ma_ho_so = ?", record)).isZero();
        assertThat(count("SELECT count(*) FROM benhanmaydayman WHERE ma_ho_so = ?", record)).isZero();
    }

    // --- API 39 ---

    @Test
    @DisplayName("AC-17: lịch sử khám gồm phiên của mọi hồ sơ, ngày khám mới nhất trước, id là chuỗi")
    void historyListsVisitsNewestFirst() throws Exception {
        long acute = createdRecord(patient, "CAP");
        long chronic = createdRecord(patient, "MAN");
        givenVisit(acute, "CAP", "2026-11-01", 1);
        givenVisit(chronic, "MAN", "2026-11-14", 1);
        givenVisit(acute, "CAP", "2026-11-07", 2);

        send(get("/api/v1/patients/" + patient + "/medical-records/history"), patientToken, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].ngay_kham").value("2026-11-14"))
                .andExpect(jsonPath("$.data[0].record_id").value(String.valueOf(chronic)))
                .andExpect(jsonPath("$.data[0].loai_benh_an").value("MAN"))
                .andExpect(jsonPath("$.data[1].ngay_kham").value("2026-11-07"))
                .andExpect(jsonPath("$.data[1].lan_kham_thu").value(2))
                .andExpect(jsonPath("$.data[2].ngay_kham").value("2026-11-01"))
                .andExpect(jsonPath("$.data[0].ma_bac_si").isString())
                .andExpect(jsonPath("$.data[0].exam_id").isString());
    }

    @Test
    @DisplayName("AC-17: chưa có phiên khám trả mảng rỗng; bệnh nhân lạ trả 3007; xem của người khác trả 3106")
    void historyEdgeCases() throws Exception {
        send(get("/api/v1/patients/" + patient + "/medical-records/history"), patientToken, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
        send(get("/api/v1/patients/999999999/medical-records/history"), doctorToken, null)
                .andExpect(jsonPath("$.code").value("3007"));
        send(get("/api/v1/patients/" + otherPatient + "/medical-records/history"), patientToken, null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("3106"));
    }

    // --- AC-18: phân quyền ---

    @Test
    @DisplayName("AC-18: benh_nhan gửi body sai lên API 34 vẫn nhận 3102, không lộ 2001")
    void patientCannotCreateEvenWithBadBody() throws Exception {
        create(patient, patientToken, "{}")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("3102"));
    }

    @Test
    @DisplayName("AC-18: benh_nhan gọi API 35 với page sai nhận 3102, không lộ 2002")
    void patientCannotListEvenWithBadQuery() throws Exception {
        send(get(RECORDS + "?page=abc"), patientToken, null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("3102"));
    }

    @Test
    @DisplayName("AC-18: benh_nhan không sửa và không xóa được, kể cả hồ sơ của mình")
    void patientCannotUpdateOrDelete() throws Exception {
        long own = createdRecord(patient, "MAN");

        send(put(RECORDS + "/" + own), patientToken, "{}").andExpect(jsonPath("$.code").value("3102"));
        send(delete(RECORDS + "/" + own), patientToken, null).andExpect(jsonPath("$.code").value("3102"));
        assertThat(count("SELECT count(*) FROM hosobenhan WHERE ma_ho_so = ?", own)).isEqualTo(1);
    }

    @ParameterizedTest(name = "AC-18: vai trò {0} không xem được chi tiết hồ sơ")
    @ValueSource(strings = {"dieu_duong", "patient", "doctor"})
    void otherRolesAreDenied(String role) throws Exception {
        long record = createdRecord(patient, "MAN");
        String token = jwtTokenProvider.generateToken(3L, "0900000003", role);

        send(get(RECORDS + "/" + record), token, null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("3102"));
    }

    @Test
    @DisplayName("AC-18: không có token nhận 401")
    void missingTokenReturns401() throws Exception {
        send(get(RECORDS), null, null).andExpect(status().isUnauthorized());
        create(patient, null, "{\"loai_benh_an\":\"MAN\"}").andExpect(status().isUnauthorized());
    }

    // --- AC-19: log ---

    @Test
    @DisplayName("AC-19: tạo, sửa, xóa đều ghi log INFO có record_id và ma_tai_khoan người gọi")
    void writesAreLoggedWithActor(CapturedOutput output) throws Exception {
        long record = createdRecord(patient, "MAN");
        update(record, "{\"trang_thai\":\"pending\"}").andExpect(status().isOk());
        send(delete(RECORDS + "/" + record), doctorToken, null).andExpect(status().isOk());

        assertThat(output.getOut())
                .contains("Tạo hồ sơ bệnh án: recordId=" + record + ", loai=MAN, benhNhan=" + patient + ", taiKhoan=" + DOCTOR_ACCOUNT)
                .contains("Sửa hồ sơ bệnh án: recordId=" + record + ", trangThai=pending, taiKhoan=" + DOCTOR_ACCOUNT)
                .contains("Xóa hồ sơ bệnh án: recordId=" + record + ", taiKhoan=" + DOCTOR_ACCOUNT);
    }
}
