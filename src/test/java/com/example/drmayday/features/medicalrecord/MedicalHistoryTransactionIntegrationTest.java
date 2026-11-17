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

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Kiểm thử tranh chấp của API 43 đến 45 (AC-12). Lớp này commit thật nên chỉ chạy trên Postgres local,
 * và tự dọn dữ liệu. Thay vì chờ một khoảng cố định, test đọc pg_stat_activity để biết chắc request đang bị chặn.
 */
@SpringBootTest
@AutoConfigureMockMvc
class MedicalHistoryTransactionIntegrationTest {

    private static final Duration WAIT_LIMIT = Duration.ofSeconds(15);

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private DataSource dataSource;

    private final List<Long> createdPatients = new ArrayList<>();
    private final ExecutorService executor = Executors.newFixedThreadPool(3);

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
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            for (Long patient : createdPatients) {
                String records = "SELECT ma_ho_so FROM hosobenhan WHERE ma_benh_nhan = " + patient;
                run(connection, "DELETE FROM hoancanhkhoiphatdiung WHERE ma_ho_so IN (" + records + ")");
                run(connection, "DELETE FROM benhsucacdotbenhtruoc WHERE ma_ho_so IN (" + records + ")");
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

    // Hồ sơ bắt buộc có dòng subtype, khóa ngoại subtype được kiểm tra lúc commit
    private long givenCommittedRecord(String type) throws SQLException {
        String phone = "09" + String.format("%08d", ThreadLocalRandom.current().nextInt(100_000_000));
        Long patient = jdbcTemplate.queryForObject(
                "INSERT INTO nguoi (ho_ten, so_dien_thoai) VALUES ('Bệnh nhân Tranh Chấp', ?) RETURNING ma_nguoi",
                Long.class, phone);
        createdPatients.add(patient);
        jdbcTemplate.update("INSERT INTO benhnhan (ma_benh_nhan) VALUES (?)", patient);
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            long record;
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO hosobenhan (ma_benh_nhan, loai_benh_an) VALUES (?, ?) RETURNING ma_ho_so")) {
                statement.setLong(1, patient);
                statement.setString(2, type);
                try (ResultSet row = statement.executeQuery()) {
                    row.next();
                    record = row.getLong(1);
                }
            }
            run(connection, "INSERT INTO " + ("CAP".equals(type) ? "benhanmaydaycap" : "benhanmaydayman")
                    + " (ma_ho_so) VALUES (?)", record);
            connection.commit();
            return record;
        }
    }

    private Future<MockHttpServletResponse> upsertInBackground(String path, String json, CountDownLatch start) {
        return executor.submit(() -> {
            if (start != null) {
                start.await();
            }
            return mockMvc.perform(put("/api/v1/" + path)
                            .header("Authorization", "Bearer " + doctorToken)
                            .contentType(MediaType.APPLICATION_JSON).content(json))
                    .andReturn().getResponse();
        });
    }

    private void waitUntilBlocked(String queryPrefix, int count) {
        Awaitility.await().atMost(WAIT_LIMIT).pollInterval(Duration.ofMillis(50)).until(() -> {
            Integer blocked = jdbcTemplate.queryForObject("""
                    SELECT count(*) FROM pg_stat_activity
                    WHERE datname = current_database() AND wait_event_type = 'Lock' AND query ILIKE ?
                    """, Integer.class, queryPrefix + "%");
            return blocked != null && blocked == count;
        });
    }

    private static String codeOf(Future<MockHttpServletResponse> call) throws Exception {
        MockHttpServletResponse response = call.get(WAIT_LIMIT.toSeconds(), TimeUnit.SECONDS);
        return JsonPath.read(response.getContentAsString(StandardCharsets.UTF_8), "$.code");
    }

    @Test
    @DisplayName("AC-12: ba upsert đầu tiên cùng lúc đều trả 1000 và chỉ có một dòng, không có 3006")
    void concurrentFirstUpsertsLeaveOneRow() throws Exception {
        long record = givenCommittedRecord("MAN");
        CountDownLatch start = new CountDownLatch(1);
        List<Future<MockHttpServletResponse>> calls = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            calls.add(upsertInBackground("allergy-context",
                    "{\"record_id\":\"" + record + "\",\"thuoc_nghi_ngo\":\"T" + i + "\"}", start));
        }
        start.countDown();

        for (Future<MockHttpServletResponse> call : calls) {
            assertThat(codeOf(call)).isEqualTo("1000");
        }
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM hoancanhkhoiphatdiung WHERE ma_ho_so = ?",
                Integer.class, record)).isEqualTo(1);
    }

    @Test
    @DisplayName("AC-12: upsert chờ khóa hồ sơ; hồ sơ bị hủy trong lúc đó thì trả 4003 và không ghi gì")
    void upsertWaitingForLockSeesCancellation() throws Exception {
        long record = givenCommittedRecord("CAP");
        run(otherDoctor, "SELECT 1 FROM hosobenhan WHERE ma_ho_so = ? FOR UPDATE", record);
        run(otherDoctor, "UPDATE hosobenhan SET trang_thai = 'cancelled' WHERE ma_ho_so = ?", record);

        Future<MockHttpServletResponse> call = upsertInBackground("infection-status",
                "{\"record_id\":\"" + record + "\",\"ho_hap_ho\":true}", null);
        waitUntilBlocked("select * from hosobenhan", 1);
        otherDoctor.commit();

        assertThat(codeOf(call)).isEqualTo("4003");
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM tinhtrangnhiemtrung WHERE ma_ho_so = ?",
                Integer.class, record)).isZero();
    }

    @Test
    @DisplayName("AC-9: upsert chờ khóa trong lúc hồ sơ bị xóa thì trả 4001, không phải 9999")
    void upsertDuringCommittedDeleteReturns4001() throws Exception {
        long record = givenCommittedRecord("MAN");
        run(otherDoctor, "SELECT 1 FROM hosobenhan WHERE ma_ho_so = ? FOR UPDATE", record);
        run(otherDoctor, "DELETE FROM benhanmaydayman WHERE ma_ho_so = ?", record);
        run(otherDoctor, "DELETE FROM hosobenhan WHERE ma_ho_so = ?", record);

        Future<MockHttpServletResponse> call = upsertInBackground("previous-episodes",
                "{\"record_id\":\"" + record + "\"}", null);
        waitUntilBlocked("select * from hosobenhan", 1);
        otherDoctor.commit();

        assertThat(codeOf(call)).isEqualTo("4001");
    }
}
