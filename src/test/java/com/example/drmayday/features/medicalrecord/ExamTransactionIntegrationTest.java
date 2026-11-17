package com.example.drmayday.features.medicalrecord;

import com.example.drmayday.features.security.JwtTokenProvider;
import com.jayway.jsonpath.JsonPath;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Kiểm thử phiên khám cần commit thật (spec 0004, AC-1, AC-4, AC-15): khóa ngoại hai chiều giữa phiên và subtype
 * chỉ được kiểm tra lúc commit, và các request ghi xếp hàng trên khóa dòng hồ sơ.
 * Test ghi thật vào database rồi tự dọn, nên chỉ chạy trên Postgres local.
 * Một kết nối khác giữ khóa hồ sơ trước, rồi test đọc pg_stat_activity để biết chắc các request đã bị chặn.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ExamTransactionIntegrationTest {

    private static final Duration WAIT_LIMIT = Duration.ofSeconds(15);
    private static final String LOCK_QUERY = "select * from hosobenhan";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DataSource dataSource;

    private final List<Long> createdPatients = new ArrayList<>();
    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    private String doctorToken;
    private Connection otherDoctor;

    @BeforeEach
    void setUp() throws SQLException {
        String url;
        try (Connection connection = dataSource.getConnection()) {
            url = connection.getMetaData().getURL();
        }
        assumeTrue(url.contains("localhost") || url.contains("127.0.0.1"),
                "Lớp test này commit dữ liệu thật nên chỉ chạy trên Postgres local");

        doctorToken = jwtTokenProvider.generateToken(6L, "0900000006", "bac_si");
        otherDoctor = dataSource.getConnection();
        otherDoctor.setAutoCommit(false);
    }

    @AfterEach
    void cleanUp() throws Exception {
        executor.shutdownNow();
        if (otherDoctor != null) {
            otherDoctor.rollback();
            otherDoctor.close();
        }
        // Khóa ngoại subtype là DEFERRED nên phải xóa cả chuỗi trong cùng một transaction
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            for (Long patient : createdPatients) {
                String records = "SELECT ma_ho_so FROM hosobenhan WHERE ma_benh_nhan = " + patient;
                String visits = "SELECT ma_phien_kham FROM phienkham WHERE ma_ho_so IN (" + records + ")";
                run(connection, "DELETE FROM phienkhamcap WHERE ma_phien_kham IN (" + visits + ")");
                run(connection, "DELETE FROM phienkhamman WHERE ma_phien_kham IN (" + visits + ")");
                run(connection, "DELETE FROM phienkham WHERE ma_ho_so IN (" + records + ")");
                run(connection, "DELETE FROM benhanmaydaycap WHERE ma_ho_so IN (" + records + ")");
                run(connection, "DELETE FROM benhanmaydayman WHERE ma_ho_so IN (" + records + ")");
                run(connection, "DELETE FROM hosobenhan WHERE ma_benh_nhan = ?", patient);
                run(connection, "DELETE FROM benhnhan WHERE ma_benh_nhan = ?", patient);
                run(connection, "DELETE FROM nguoi WHERE ma_nguoi = ?", patient);
            }
            connection.commit();
        }
    }

    // --- dữ liệu ---

    private static void run(Connection connection, String sql, Object... params) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                statement.setObject(i + 1, params[i]);
            }
            statement.execute();
        }
    }

    private static long insertReturningId(Connection connection, String sql, Object... params) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                statement.setObject(i + 1, params[i]);
            }
            try (ResultSet row = statement.executeQuery()) {
                row.next();
                return row.getLong(1);
            }
        }
    }

    private long givenCommittedRecord(String type) throws SQLException {
        String phone = "09" + String.format("%08d", ThreadLocalRandom.current().nextInt(100_000_000));
        Long patient = jdbcTemplate.queryForObject(
                "INSERT INTO nguoi (ho_ten, so_dien_thoai) VALUES ('Bệnh nhân Phiên Khám', ?) RETURNING ma_nguoi",
                Long.class, phone);
        createdPatients.add(patient);
        jdbcTemplate.update("INSERT INTO benhnhan (ma_benh_nhan) VALUES (?)", patient);
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            long record = insertReturningId(connection,
                    "INSERT INTO hosobenhan (ma_benh_nhan, loai_benh_an, trang_thai) VALUES (?, ?, 'in_progress') RETURNING ma_ho_so",
                    patient, type);
            run(connection, "INSERT INTO " + ("CAP".equals(type) ? "benhanmaydaycap" : "benhanmaydayman")
                    + " (ma_ho_so) VALUES (?)", record);
            connection.commit();
            return record;
        }
    }

    // --- gọi API ---

    private MockHttpServletResponse send(MockHttpServletRequestBuilder builder, String json) throws Exception {
        builder.header("Authorization", "Bearer " + doctorToken);
        if (json != null) {
            builder.contentType(MediaType.APPLICATION_JSON).content(json);
        }
        return mockMvc.perform(builder).andReturn().getResponse();
    }

    private Future<MockHttpServletResponse> sendInBackground(MockHttpServletRequestBuilder builder, String json) {
        Callable<MockHttpServletResponse> call = () -> send(builder, json);
        return executor.submit(call);
    }

    private long createdExam(long record) throws Exception {
        MockHttpServletResponse response = send(post("/api/v1/exams"), "{\"record_id\":\"" + record + "\"}");
        assertThat(response.getStatus()).isEqualTo(201);
        return Long.parseLong(JsonPath.read(response.getContentAsString(StandardCharsets.UTF_8), "$.data.exam_id"));
    }

    private void lockRecord(long record) throws SQLException {
        run(otherDoctor, "SELECT 1 FROM hosobenhan WHERE ma_ho_so = ? FOR UPDATE", record);
    }

    private void waitUntilBlocked(int count) {
        Awaitility.await().atMost(WAIT_LIMIT).pollInterval(Duration.ofMillis(50)).until(() -> {
            Integer blocked = jdbcTemplate.queryForObject("""
                    SELECT count(*) FROM pg_stat_activity
                    WHERE datname = current_database()
                      AND wait_event_type = 'Lock'
                      AND query ILIKE ?
                    """, Integer.class, LOCK_QUERY + "%");
            return blocked != null && blocked == count;
        });
    }

    private static MockHttpServletResponse result(Future<MockHttpServletResponse> call) throws Exception {
        return call.get(WAIT_LIMIT.toSeconds(), TimeUnit.SECONDS);
    }

    private static String json(MockHttpServletResponse response) throws Exception {
        return response.getContentAsString(StandardCharsets.UTF_8);
    }

    // --- kịch bản ---

    @Test
    @DisplayName("AC-1: tạo phiên CAP và MAN commit được, khóa ngoại hai chiều với subtype không báo lỗi")
    void createCommitsWithSubtype() throws Exception {
        long acuteExam = createdExam(givenCommittedRecord("CAP"));
        long chronicExam = createdExam(givenCommittedRecord("MAN"));

        assertThat(jdbcTemplate.queryForObject("SELECT ma_subtype_cap FROM phienkham WHERE ma_phien_kham = ?",
                Long.class, acuteExam)).isEqualTo(acuteExam);
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM phienkhamcap WHERE ma_phien_kham = ?",
                Integer.class, acuteExam)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM phienkhamman WHERE ma_phien_kham = ?",
                Integer.class, chronicExam)).isEqualTo(1);
    }

    @Test
    @DisplayName("AC-4: hai request tạo cùng lúc trên một hồ sơ nhận hai số lần khám liên tiếp, không có 4009")
    void concurrentCreatesGetConsecutiveNumbers() throws Exception {
        long record = givenCommittedRecord("CAP");
        lockRecord(record);

        String body = "{\"record_id\":\"" + record + "\"}";
        Future<MockHttpServletResponse> first = sendInBackground(post("/api/v1/exams"), body);
        Future<MockHttpServletResponse> second = sendInBackground(post("/api/v1/exams"), body);
        waitUntilBlocked(2);
        otherDoctor.commit();

        MockHttpServletResponse a = result(first);
        MockHttpServletResponse b = result(second);
        assertThat(List.<String>of(JsonPath.read(json(a), "$.code"), JsonPath.read(json(b), "$.code")))
                .containsExactly("1001", "1001");
        List<Integer> numbers = List.of(JsonPath.read(json(a), "$.data.lan_kham_thu"),
                JsonPath.read(json(b), "$.data.lan_kham_thu"));
        assertThat(numbers).containsExactlyInAnyOrder(1, 2);
    }

    @Test
    @DisplayName("AC-15: hai request sửa cùng lúc hai trường khác nhau, cả hai thay đổi đều được giữ")
    void concurrentUpdatesKeepBothChanges() throws Exception {
        long record = givenCommittedRecord("MAN");
        long exam = createdExam(record);
        lockRecord(record);

        Future<MockHttpServletResponse> first = sendInBackground(put("/api/v1/exams/" + exam), "{\"dan_do\":\"Dặn dò\"}");
        Future<MockHttpServletResponse> second = sendInBackground(put("/api/v1/exams/" + exam),
                "{\"tom_tat_buoi_kham\":\"Tóm tắt\"}");
        waitUntilBlocked(2);
        otherDoctor.commit();

        assertThat(result(first).getStatus()).isEqualTo(200);
        assertThat(result(second).getStatus()).isEqualTo(200);
        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT dan_do, tom_tat_buoi_kham FROM phienkham WHERE ma_phien_kham = ?", exam);
        assertThat(row).containsEntry("dan_do", "Dặn dò").containsEntry("tom_tat_buoi_kham", "Tóm tắt");
    }

    @Test
    @DisplayName("AC-15: request sửa chờ khóa hồ sơ; hồ sơ được đóng và commit trong lúc đó thì trả 4003")
    void updateAfterCommittedCloseIsRejected() throws Exception {
        long record = givenCommittedRecord("CAP");
        long exam = createdExam(record);
        lockRecord(record);
        run(otherDoctor, "UPDATE hosobenhan SET trang_thai = 'completed' WHERE ma_ho_so = ?", record);

        Future<MockHttpServletResponse> update = sendInBackground(put("/api/v1/exams/" + exam), "{\"dan_do\":\"Muộn\"}");
        waitUntilBlocked(1);
        otherDoctor.commit();

        MockHttpServletResponse response = result(update);
        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(JsonPath.<String>read(json(response), "$.code")).isEqualTo("4003");
        assertThat(jdbcTemplate.queryForObject("SELECT dan_do FROM phienkham WHERE ma_phien_kham = ?",
                String.class, exam)).isNull();
    }
}
