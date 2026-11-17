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
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Kiểm thử API 43 đến 45 (spec 0003) qua filter chain thật, xuống PostgreSQL thật. Mỗi test được rollback.
 * Mọi request chạy chung transaction của test, nên phải flush và clear trước mỗi request để giống request HTTP thật.
 * Tranh chấp (AC-12) nằm ở MedicalHistoryTransactionIntegrationTest, vì test này không thử được khóa.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ExtendWith(OutputCaptureExtension.class)
class MedicalHistoryApiIntegrationTest {

    private static final long DOCTOR_ACCOUNT = 6L;

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @Autowired private JdbcTemplate jdbcTemplate;
    @PersistenceContext private EntityManager entityManager;

    private String doctorToken;
    private long chronicRecord;
    private long acuteRecord;

    @BeforeEach
    void setUp() throws Exception {
        doctorToken = jwtTokenProvider.generateToken(DOCTOR_ACCOUNT, "0900000006", "bac_si");
        String phone = "09" + String.format("%08d", ThreadLocalRandom.current().nextInt(100_000_000));
        Long patient = jdbcTemplate.queryForObject(
                "INSERT INTO nguoi (ho_ten, so_dien_thoai) VALUES ('Bệnh nhân Bệnh Sử', ?) RETURNING ma_nguoi",
                Long.class, phone);
        jdbcTemplate.update("INSERT INTO benhnhan (ma_benh_nhan) VALUES (?)", patient);
        chronicRecord = createRecord(patient, "MAN");
        acuteRecord = createRecord(patient, "CAP");
    }

    private long createRecord(long patient, String type) throws Exception {
        String json = body(send(post("/api/v1/patients/" + patient + "/medical-records"), doctorToken,
                "{\"loai_benh_an\":\"" + type + "\"}").andExpect(status().isCreated()));
        return Long.parseLong(JsonPath.read(json, "$.data.record_id"));
    }

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

    private ResultActions upsert(String path, long record, String fields) throws Exception {
        String json = "{\"record_id\":\"" + record + "\"" + (fields.isEmpty() ? "" : "," + fields) + "}";
        return send(put("/api/v1/" + path), doctorToken, json);
    }

    private static String body(ResultActions result) throws Exception {
        return result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private int rows(String table, long record) {
        Integer count = jdbcTemplate.queryForObject("SELECT count(*) FROM " + table + " WHERE ma_ho_so = ?",
                Integer.class, record);
        return count == null ? 0 : count;
    }

    // --- API 43 ---

    @Test
    @DisplayName("AC-1: lần đầu tạo dòng, trả record_id, khóa chính và các cột với hai chữ số thập phân")
    void firstAllergyUpsertCreatesRow() throws Exception {
        String json = body(upsert("allergy-context", chronicRecord, """
                "thuc_an_nghi_ngo":"Tôm cua","thuc_an_thoi_gian":1.5,"thuc_an_tung_bi":true,
                "thuc_an_loai_tien_su":"Di ung","thuc_an_tien_su_ghi_ro":"Phù môi 2024"
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("1000"))
                .andExpect(jsonPath("$.data.record_id").value(String.valueOf(chronicRecord)))
                .andExpect(jsonPath("$.data.ma_hoan_canh_khoi_phat").isString())
                .andExpect(jsonPath("$.data.thuc_an_loai_tien_su").value("Di ung"))
                .andExpect(jsonPath("$.data.thuoc_nghi_ngo").isEmpty()));

        assertThat(json).contains("\"thuc_an_thoi_gian\":1.50");
        assertThat(rows("hoancanhkhoiphatdiung", chronicRecord)).isEqualTo(1);
    }

    @Test
    @DisplayName("AC-1: lần sau ghi đè cùng dòng, giữ khóa chính, trường không gửi thành NULL")
    void secondAllergyUpsertOverwritesSameRow() throws Exception {
        String first = body(upsert("allergy-context", chronicRecord,
                "\"thuc_an_nghi_ngo\":\"Tôm\",\"thuc_an_thoi_gian\":2"));
        String id = JsonPath.read(first, "$.data.ma_hoan_canh_khoi_phat");

        upsert("allergy-context", chronicRecord, "\"thuoc_nghi_ngo\":\"  Aspirin  \"")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ma_hoan_canh_khoi_phat").value(id))
                .andExpect(jsonPath("$.data.thuc_an_nghi_ngo").isEmpty())
                .andExpect(jsonPath("$.data.thuc_an_thoi_gian").isEmpty())
                .andExpect(jsonPath("$.data.thuoc_nghi_ngo").value("Aspirin"));
        assertThat(rows("hoancanhkhoiphatdiung", chronicRecord)).isEqualTo(1);
    }

    @Test
    @DisplayName("AC-1: API 43 ghi được cho cả hồ sơ CAP; 1e3 được in lại là 1000.00")
    void allergyWorksOnAcuteRecordAndNormalizesExponent() throws Exception {
        String json = body(upsert("allergy-context", acuteRecord,
                "\"khac_nghi_ngo\":\"Phấn hoa\",\"khac_thoi_gian\":1e3").andExpect(status().isOk()));

        assertThat(json).contains("\"khac_thoi_gian\":1000.00");
    }

    @ParameterizedTest(name = "AC-2: {0} trả 2003 và không ghi gì")
    @ValueSource(strings = {
            "\"thuc_an_tung_bi\":true",
            "\"thuc_an_tung_bi\":false,\"thuc_an_tien_su_ghi_ro\":\"x\"",
            "\"thuc_an_tung_bi\":true,\"thuc_an_tien_su_ghi_ro\":\"x\",\"thuc_an_loai_tien_su\":\"Khong\"",
            "\"thuc_an_loai_tien_su\":\"Di ung\"",
            "\"thuc_an_thoi_gian\":3",
            "\"thuc_an_nghi_ngo\":\"   \""
    })
    void allergyRuleViolationsReturn2003(String fields) throws Exception {
        upsert("allergy-context", chronicRecord, fields)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("2003"));
        assertThat(rows("hoancanhkhoiphatdiung", chronicRecord)).isZero();
    }

    @ParameterizedTest(name = "AC-8: trường lạ {0} trả 2003")
    @ValueSource(strings = {"\"thuc_an_nghi_ngoo\":\"Tôm\"", "\"ma_ho_so\":\"1\"", "\"ma_hoan_canh_khoi_phat\":\"5\""})
    void unknownFieldsReturn2003(String fields) throws Exception {
        upsert("allergy-context", chronicRecord, fields)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("2003"));
        assertThat(rows("hoancanhkhoiphatdiung", chronicRecord)).isZero();
    }

    @Test
    @DisplayName("AC-10: body chỉ có record_id xóa trắng phần đó nhưng giữ dòng")
    void emptyBodyClearsSectionButKeepsRow() throws Exception {
        upsert("allergy-context", chronicRecord, "\"thuoc_nghi_ngo\":\"Aspirin\"").andExpect(status().isOk());

        upsert("allergy-context", chronicRecord, "").andExpect(status().isOk());

        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM hoancanhkhoiphatdiung WHERE ma_ho_so = ? AND thuoc_nghi_ngo IS NULL",
                Integer.class, chronicRecord)).isEqualTo(1);
    }

    // --- API 44 ---

    @Test
    @DisplayName("AC-3: bệnh sử các đợt trước cho hồ sơ MAN, đủ trường hợp lệ")
    void previousEpisodesOnChronicRecord() throws Exception {
        upsert("previous-episodes", chronicRecord, """
                "tung_co_dot_tuong_tu":true,"tuoi_khoi_phat":25,"so_dot_truoc":3,
                "kieu_dien_bien":"Tung dot, giua cac dot het hoan toan","bieu_hien_dot_truoc":"San phu + phu mach",
                "da_tung_nhap_vien":true,"so_lan_nhap_vien":1,"thoi_gian_tu_dot_gan_nhat":6
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ma_tien_su").isString())
                .andExpect(jsonPath("$.data.so_dot_truoc").value(3))
                .andExpect(jsonPath("$.data.kieu_dien_bien").value("Tung dot, giua cac dot het hoan toan"))
                .andExpect(jsonPath("$.data.thoi_gian_tu_dot_gan_nhat").value(6.0));
    }

    @ParameterizedTest(name = "AC-4: {0} trả 2003")
    @ValueSource(strings = {"\"tuoi_khoi_phat\":151", "\"so_dot_truoc\":2", "\"so_lan_nhap_vien\":1",
            "\"kieu_dien_bien\":\"Tung dot\"", "\"bieu_hien_dot_truoc\":\"san phu\"", "\"tuoi_khoi_phat\":40000"})
    void episodeRuleViolationsReturn2003(String fields) throws Exception {
        upsert("previous-episodes", chronicRecord, fields).andExpect(jsonPath("$.code").value("2003"));
    }

    @Test
    @DisplayName("AC-7: API 44 trên hồ sơ CAP trả 4008 và không ghi gì")
    void previousEpisodesOnAcuteRecordReturns4008() throws Exception {
        upsert("previous-episodes", acuteRecord, "\"tung_co_dot_tuong_tu\":true")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("4008"));
        assertThat(rows("benhsucacdotbenhtruoc", acuteRecord)).isZero();
    }

    // --- API 45 ---

    @Test
    @DisplayName("AC-5: tình trạng nhiễm trùng cho hồ sơ CAP")
    void infectionStatusOnAcuteRecord() throws Exception {
        String json = body(upsert("infection-status", acuteRecord,
                "\"ho_hap_ho\":true,\"ho_hap_thoi_gian\":3,\"khac_ghi_ro\":\"Sốt nhẹ\",\"khac_khoang_cach\":2.5")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ma_tinh_trang_nhiem_trung").isString())
                .andExpect(jsonPath("$.data.ho_hap_ho").value(true)));

        assertThat(json).contains("\"ho_hap_thoi_gian\":3.00", "\"khac_khoang_cach\":2.50");
    }

    @ParameterizedTest(name = "AC-6: {0} trả 2003")
    @ValueSource(strings = {"\"ho_hap_thoi_gian\":3", "\"ho_hap_ho\":false,\"ho_hap_thoi_gian\":0",
            "\"khac_thoi_gian\":1", "\"khac_ghi_ro\":\"  \""})
    void infectionRuleViolationsReturn2003(String fields) throws Exception {
        upsert("infection-status", acuteRecord, fields).andExpect(jsonPath("$.code").value("2003"));
    }

    @Test
    @DisplayName("AC-7: API 45 trên hồ sơ MAN trả 4008")
    void infectionStatusOnChronicRecordReturns4008() throws Exception {
        upsert("infection-status", chronicRecord, "\"ho_hap_ho\":true").andExpect(jsonPath("$.code").value("4008"));
        assertThat(rows("tinhtrangnhiemtrung", chronicRecord)).isZero();
    }

    // --- Lỗi chung và thứ tự ---

    @Test
    @DisplayName("AC-8: thiếu hoặc rỗng record_id trả 2001, sai kiểu trả 2002, không tồn tại trả 4001")
    void recordIdErrors() throws Exception {
        send(put("/api/v1/allergy-context"), doctorToken, "{}").andExpect(jsonPath("$.code").value("2001"));
        send(put("/api/v1/allergy-context"), doctorToken, "{\"record_id\":\"  \"}").andExpect(jsonPath("$.code").value("2001"));
        send(put("/api/v1/allergy-context"), doctorToken, "{\"record_id\":\"abc\"}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("2002"));
        send(put("/api/v1/allergy-context"), doctorToken, "{\"record_id\":\"999999999\"}")
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("4001"));
        send(put("/api/v1/allergy-context"), doctorToken, "{\"record_id\":\"0\"}").andExpect(jsonPath("$.code").value("4001"));
    }

    @Test
    @DisplayName("AC-9: sai kiểu JSON trả 2003 trước cả 4008")
    void jsonTypeErrorComesFirst() throws Exception {
        upsert("previous-episodes", acuteRecord, "\"tuoi_khoi_phat\":\"abc\"").andExpect(jsonPath("$.code").value("2003"));
    }

    @Test
    @DisplayName("AC-9: sai loại hồ sơ được báo trước giá trị sai")
    void typeMismatchComesBeforeValueErrors() throws Exception {
        upsert("previous-episodes", acuteRecord, "\"tuoi_khoi_phat\":-1").andExpect(jsonPath("$.code").value("4008"));
    }

    @Test
    @DisplayName("AC-9: hồ sơ completed trả 4003 với mọi body, dữ liệu cũ giữ nguyên")
    void completedRecordIsFrozen() throws Exception {
        upsert("infection-status", acuteRecord, "\"da_nhot\":true").andExpect(status().isOk());
        send(put("/api/v1/medical-records/" + acuteRecord), doctorToken, "{\"trang_thai\":\"completed\"}")
                .andExpect(status().isOk());

        upsert("infection-status", acuteRecord, "\"ho_hap_thoi_gian\":3")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("4003"));
        upsert("allergy-context", acuteRecord, "").andExpect(jsonPath("$.code").value("4003"));
        assertThat(jdbcTemplate.queryForObject("SELECT da_nhot FROM tinhtrangnhiemtrung WHERE ma_ho_so = ?",
                Boolean.class, acuteRecord)).isTrue();
    }

    // --- Ép kiểu của Jackson (giữ mặc định của dự án, spec 0003 yêu cầu ghi lại hành vi thật) ---

    @Test
    @DisplayName("Chuỗi \"true\" vào trường Boolean được nhận như true")
    void booleanStringIsCoerced() throws Exception {
        upsert("infection-status", acuteRecord, "\"da_mun\":\"true\"")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.da_mun").value(true));
    }

    @Test
    @DisplayName("Chữ không phải số vào trường số trả 2003")
    void nonNumericTextInNumberFieldReturns2003() throws Exception {
        upsert("infection-status", acuteRecord, "\"da_mun\":true,\"da_thoi_gian\":\"ba ngày\"")
                .andExpect(jsonPath("$.code").value("2003"));
    }

    // --- AC-11 ---

    @Test
    @DisplayName("AC-11: API 36 đọc đúng các phần vừa ghi, số thập phân hai chữ số")
    void detailShowsWrittenSections() throws Exception {
        upsert("allergy-context", chronicRecord, "\"thuoc_nghi_ngo\":\"Aspirin\",\"thuoc_thoi_gian\":30").andExpect(status().isOk());
        upsert("previous-episodes", chronicRecord, "\"thoi_gian_tu_dot_gan_nhat\":12").andExpect(status().isOk());

        String json = body(send(get("/api/v1/medical-records/" + chronicRecord), doctorToken, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.allergy_context.thuoc_nghi_ngo").value("Aspirin")));

        assertThat(json).contains("\"thuoc_thoi_gian\":30.00", "\"thoi_gian_tu_dot_gan_nhat\":12.00");
    }

    // --- AC-13 ---

    @ParameterizedTest(name = "AC-13: vai trò {0} gửi body rỗng nhận 3102, không phải 2001")
    @ValueSource(strings = {"benh_nhan", "dieu_duong", "patient"})
    void otherRolesAreDeniedBeforeValidation(String role) throws Exception {
        String token = jwtTokenProvider.generateToken(3L, "0900000003", role);

        for (String path : new String[]{"allergy-context", "previous-episodes", "infection-status"}) {
            send(put("/api/v1/" + path), token, "{}")
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("3102"));
        }
    }

    @Test
    @DisplayName("AC-13: không có token nhận 401")
    void missingTokenReturns401() throws Exception {
        send(put("/api/v1/previous-episodes"), null, "{\"record_id\":\"" + chronicRecord + "\"}")
                .andExpect(status().isUnauthorized());
    }

    // --- AC-14 ---

    @Test
    @DisplayName("AC-14: mỗi lần ghi có log INFO với tạo mới hay ghi đè và tài khoản người gọi")
    void writesAreLogged(CapturedOutput output) throws Exception {
        upsert("allergy-context", chronicRecord, "").andExpect(status().isOk());
        upsert("allergy-context", chronicRecord, "").andExpect(status().isOk());
        upsert("previous-episodes", chronicRecord, "").andExpect(status().isOk());
        upsert("infection-status", acuteRecord, "").andExpect(status().isOk());

        assertThat(output.getOut())
                .contains("Ghi hoàn cảnh khởi phát dị ứng: recordId=" + chronicRecord + ", taoMoi=true, taiKhoan=" + DOCTOR_ACCOUNT)
                .contains("Ghi hoàn cảnh khởi phát dị ứng: recordId=" + chronicRecord + ", taoMoi=false, taiKhoan=" + DOCTOR_ACCOUNT)
                .contains("Ghi bệnh sử các đợt trước: recordId=" + chronicRecord + ", taoMoi=true")
                .contains("Ghi tình trạng nhiễm trùng: recordId=" + acuteRecord + ", taoMoi=true");
    }
}
