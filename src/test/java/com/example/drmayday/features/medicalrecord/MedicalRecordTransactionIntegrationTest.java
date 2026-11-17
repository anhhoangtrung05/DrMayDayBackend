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
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Kiểm thử tranh chấp và lỗi khóa ngoại của hồ sơ bệnh án (AC-5, AC-13, AC-16). Lớp này cần commit thật.
 * Test ghi thật vào database rồi tự dọn, nên chỉ chạy trên Postgres local. Gặp server khác thì test bị bỏ qua.
 * Thay vì chờ một khoảng cố định, test đọc pg_stat_activity để biết chắc request đã bị database chặn.
 */
@SpringBootTest
@AutoConfigureMockMvc
class MedicalRecordTransactionIntegrationTest {

    private static final Duration WAIT_LIMIT = Duration.ofSeconds(15);

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
                run(connection, "DELETE FROM tinhtrangnhiemtrung WHERE ma_ho_so IN (" + records + ")");
                run(connection, "DELETE FROM benhanmaydaycap WHERE ma_ho_so IN (" + records + ")");
                run(connection, "DELETE FROM benhanmaydayman WHERE ma_ho_so IN (" + records + ")");
                run(connection, "DELETE FROM hosobenhan WHERE ma_benh_nhan = ?", patient);
                run(connection, "DELETE FROM benhnhan WHERE ma_benh_nhan = ?", patient);
                run(connection, "DELETE FROM nguoi WHERE ma_nguoi = ?", patient);
            }
            connection.commit();
        }
    }

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

    private long givenCommittedPatient() {
        String phone = "09" + String.format("%08d", ThreadLocalRandom.current().nextInt(100_000_000));
        Long id = jdbcTemplate.queryForObject(
                "INSERT INTO nguoi (ho_ten, so_dien_thoai) VALUES ('Bệnh nhân Tranh Chấp', ?) RETURNING ma_nguoi",
                Long.class, phone);
        createdPatients.add(id);
        jdbcTemplate.update("INSERT INTO benhnhan (ma_benh_nhan) VALUES (?)", id);
        return id;
    }

    // Hồ sơ bắt buộc có dòng subtype, khóa ngoại subtype được kiểm tra lúc commit
    private long givenCommittedRecord(long patient, String type) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            long record = insertReturningId(connection,
                    "INSERT INTO hosobenhan (ma_benh_nhan, loai_benh_an) VALUES (?, ?) RETURNING ma_ho_so", patient, type);
            run(connection, "INSERT INTO " + ("CAP".equals(type) ? "benhanmaydaycap" : "benhanmaydayman")
                    + " (ma_ho_so) VALUES (?)", record);
            connection.commit();
            return record;
        }
    }

    private MockHttpServletResponse send(MockHttpServletRequestBuilder builder, String json) throws Exception {
        builder.header("Authorization", "Bearer " + doctorToken);
        if (json != null) {
            builder.contentType(MediaType.APPLICATION_JSON).content(json);
        }
        return mockMvc.perform(builder).andReturn().getResponse();
    }

    private Future<MockHttpServletResponse> sendInBackground(MockHttpServletRequestBuilder builder, String json,
                                                             CountDownLatch start) {
        Callable<MockHttpServletResponse> call = () -> {
            if (start != null) {
                start.await();
            }
            return send(builder, json);
        };
        return executor.submit(call);
    }

    private void waitUntilBlocked(String queryPrefix, int count) {
        Awaitility.await().atMost(WAIT_LIMIT).pollInterval(Duration.ofMillis(50)).until(() -> {
            Integer blocked = jdbcTemplate.queryForObject("""
                    SELECT count(*) FROM pg_stat_activity
                    WHERE datname = current_database()
                      AND wait_event_type = 'Lock'
                      AND query ILIKE ?
                    """, Integer.class, queryPrefix + "%");
            return blocked != null && blocked == count;
        });
    }

    private static MockHttpServletResponse result(Future<MockHttpServletResponse> call) throws Exception {
        return call.get(WAIT_LIMIT.toSeconds(), TimeUnit.SECONDS);
    }

    private static String codeOf(MockHttpServletResponse response) throws Exception {
        return JsonPath.read(response.getContentAsString(StandardCharsets.UTF_8), "$.code");
    }

    private int openRecords(long patient, String type) {
        Integer rows = jdbcTemplate.queryForObject("""
                SELECT count(*) FROM hosobenhan
                WHERE ma_benh_nhan = ? AND loai_benh_an = ? AND trang_thai IN ('draft', 'pending', 'in_progress')
                """, Integer.class, patient, type);
        return rows == null ? 0 : rows;
    }

    @Test
    @DisplayName("AC-5: hai request tạo CAP cùng lúc cho một bệnh nhân: một 1001, một 4006, chỉ một hồ sơ mở")
    void concurrentCreatesLeaveOneOpenRecord() throws Exception {
        long patient = givenCommittedPatient();
        String path = "/api/v1/patients/" + patient + "/medical-records";
        CountDownLatch start = new CountDownLatch(1);

        Future<MockHttpServletResponse> first = sendInBackground(post(path), "{\"loai_benh_an\":\"CAP\"}", start);
        Future<MockHttpServletResponse> second = sendInBackground(post(path), "{\"loai_benh_an\":\"CAP\"}", start);
        start.countDown();

        List<String> codes = List.of(codeOf(result(first)), codeOf(result(second)));
        assertThat(codes).containsExactlyInAnyOrder("1001", "4006");
        assertThat(openRecords(patient, "CAP")).isEqualTo(1);
    }

    @Test
    @DisplayName("AC-5: request tạo hồ sơ chờ khóa bệnh nhân, rồi thấy hồ sơ vừa commit và trả 4006")
    void createWaitsForPatientLockThenSeesCommittedRecord() throws Exception {
        long patient = givenCommittedPatient();
        run(otherDoctor, "SELECT 1 FROM benhnhan WHERE ma_benh_nhan = ? FOR UPDATE", patient);
        long record = insertReturningId(otherDoctor,
                "INSERT INTO hosobenhan (ma_benh_nhan, loai_benh_an) VALUES (?, 'MAN') RETURNING ma_ho_so", patient);
        run(otherDoctor, "INSERT INTO benhanmaydayman (ma_ho_so) VALUES (?)", record);

        Future<MockHttpServletResponse> creation = sendInBackground(
                post("/api/v1/patients/" + patient + "/medical-records"), "{\"loai_benh_an\":\"MAN\"}", null);
        waitUntilBlocked("select * from benhnhan", 1);
        otherDoctor.commit();
        MockHttpServletResponse response = result(creation);

        assertThat(response.getStatus()).isEqualTo(409);
        assertThat(codeOf(response)).isEqualTo("4006");
        assertThat(openRecords(patient, "MAN")).isEqualTo(1);
    }

    @Test
    @DisplayName("AC-13: API 37 chờ khóa hồ sơ; hồ sơ bị hủy trong lúc đó thì trả 4003 và không ghi đè")
    void updateWaitsForRecordLockThenSeesCancellation() throws Exception {
        long patient = givenCommittedPatient();
        long record = givenCommittedRecord(patient, "MAN");
        run(otherDoctor, "SELECT 1 FROM hosobenhan WHERE ma_ho_so = ? FOR UPDATE", record);
        run(otherDoctor, "UPDATE hosobenhan SET trang_thai = 'cancelled' WHERE ma_ho_so = ?", record);

        Future<MockHttpServletResponse> edit = sendInBackground(put("/api/v1/medical-records/" + record),
                "{\"chan_doan_tuyen_truoc\":\"ghi đè\"}", null);
        waitUntilBlocked("select * from hosobenhan", 1);
        otherDoctor.commit();
        MockHttpServletResponse response = result(edit);

        assertThat(codeOf(response)).isEqualTo("4003");
        assertThat(jdbcTemplate.queryForObject("SELECT chan_doan_tuyen_truoc FROM hosobenhan WHERE ma_ho_so = ?",
                String.class, record)).isNull();
    }

    @Test
    @DisplayName("AC-9: API 37 chờ khóa trong lúc hồ sơ bị xóa thì trả 4001, không phải 9999")
    void updateDuringCommittedDeleteReturns4001() throws Exception {
        long patient = givenCommittedPatient();
        long record = givenCommittedRecord(patient, "MAN");
        run(otherDoctor, "SELECT 1 FROM hosobenhan WHERE ma_ho_so = ? FOR UPDATE", record);
        run(otherDoctor, "DELETE FROM benhanmaydayman WHERE ma_ho_so = ?", record);
        run(otherDoctor, "DELETE FROM hosobenhan WHERE ma_ho_so = ?", record);

        Future<MockHttpServletResponse> edit = sendInBackground(put("/api/v1/medical-records/" + record), "{}", null);
        waitUntilBlocked("select * from hosobenhan", 1);
        otherDoctor.commit();
        MockHttpServletResponse response = result(edit);

        assertThat(response.getStatus()).isEqualTo(404);
        assertThat(codeOf(response)).isEqualTo("4001");
    }

    @Test
    @DisplayName("AC-16: xóa hồ sơ CAP đã có phiên khám trả 4007 và hồ sơ vẫn còn nguyên")
    void deletingRecordWithVisitReturns4007() throws Exception {
        long patient = givenCommittedPatient();
        long record = givenCommittedRecord(patient, "CAP");
        Long doctor = jdbcTemplate.queryForObject("SELECT min(ma_nhan_vien) FROM nhanvien", Long.class);
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            long visit = insertReturningId(connection, """
                    INSERT INTO phienkham (ma_ho_so, ma_bac_si, loai_benh_an, ngay_kham, lan_kham_thu)
                    VALUES (?, ?, 'CAP', CURRENT_DATE, 1) RETURNING ma_phien_kham
                    """, record, doctor);
            run(connection, "INSERT INTO phienkhamcap (ma_phien_kham) VALUES (?)", visit);
            connection.commit();
        }

        MockHttpServletResponse response = send(delete("/api/v1/medical-records/" + record), null);

        assertThat(response.getStatus()).isEqualTo(409);
        assertThat(codeOf(response)).isEqualTo("4007");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM hosobenhan WHERE ma_ho_so = ?", Integer.class, record)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM benhanmaydaycap WHERE ma_ho_so = ?", Integer.class, record)).isEqualTo(1);
    }

    @Test
    @DisplayName("AC-16: xóa hồ sơ CAP còn tình trạng nhiễm trùng (khóa ngoại tới subtype) trả 4007")
    void deletingRecordWithSubtypeChildReturns4007() throws Exception {
        long patient = givenCommittedPatient();
        long record = givenCommittedRecord(patient, "CAP");
        jdbcTemplate.update("INSERT INTO tinhtrangnhiemtrung (ma_ho_so, ho_hap_ho) VALUES (?, true)", record);

        MockHttpServletResponse response = send(delete("/api/v1/medical-records/" + record), null);

        assertThat(codeOf(response)).isEqualTo("4007");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM benhanmaydaycap WHERE ma_ho_so = ?", Integer.class, record)).isEqualTo(1);
    }
}
