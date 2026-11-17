package com.example.drmayday.features.facility;

import com.example.drmayday.features.security.JwtTokenProvider;
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
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Tests behavior that requires committed transactions: contention between two admins (AC-5) and full rollback.
 * This class writes to the database and cleans up afterward, so it only runs against local PostgreSQL.
 * Tests inspect pg_stat_activity to confirm that the database blocked a request instead of waiting for a fixed duration.
 */
@SpringBootTest
@AutoConfigureMockMvc
class BranchTransactionIntegrationTest {

    private static final String BRANCHES = "/api/v1/branches/";
    private static final Duration WAIT_LIMIT = Duration.ofSeconds(15);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DataSource dataSource;

    private final List<Long> createdBranches = new ArrayList<>();
    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    private String adminToken;
    private Connection otherAdmin;

    @BeforeEach
    void setUp() throws SQLException {
        String url;
        try (Connection connection = dataSource.getConnection()) {
            url = connection.getMetaData().getURL();
        }
        assumeTrue(url.contains("localhost") || url.contains("127.0.0.1"),
                "This test class commits real data and therefore only runs against local PostgreSQL.");

        adminToken = jwtTokenProvider.generateToken(1L, "0900000001", "admin");
        otherAdmin = dataSource.getConnection();
        otherAdmin.setAutoCommit(false);
    }

    @AfterEach
    void cleanUp() throws Exception {
        executor.shutdownNow();
        if (otherAdmin != null) {
            otherAdmin.rollback();
            otherAdmin.close();
        }
        for (Long id : createdBranches) {
            jdbcTemplate.update("DELETE FROM phongkham WHERE ma_chi_nhanh = ?", id);
            jdbcTemplate.update("DELETE FROM chinhanh WHERE ma_chi_nhanh = ?", id);
        }
    }

    private long givenBranch() {
        Long id = jdbcTemplate.queryForObject(
                "INSERT INTO chinhanh (ten_chi_nhanh, dia_chi, so_dien_thoai) VALUES (?, ?, ?) RETURNING ma_chi_nhanh",
                Long.class, "Cơ sở Test Giao Dịch", "Số 1 Phố Huế", "0241234567");
        createdBranches.add(id);
        return id;
    }

    private boolean branchExists(long id) {
        Integer rows = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM chinhanh WHERE ma_chi_nhanh = ?", Integer.class, id);
        return rows != null && rows == 1;
    }

    private void otherAdminRuns(String sql, Object... params) throws SQLException {
        try (PreparedStatement statement = otherAdmin.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                statement.setObject(i + 1, params[i]);
            }
            statement.execute();
        }
    }

    private Callable<MockHttpServletResponse> deleteCall(long id) {
        return () -> mockMvc.perform(delete(BRANCHES + id)
                        .header("Authorization", "Bearer " + adminToken))
                .andReturn().getResponse();
    }

    private void waitUntilDeletesAreBlocked(int count) {
        Awaitility.await().atMost(WAIT_LIMIT).pollInterval(Duration.ofMillis(50)).until(() -> {
            Integer blocked = jdbcTemplate.queryForObject("""
                    SELECT count(*) FROM pg_stat_activity
                    WHERE datname = current_database()
                      AND wait_event_type = 'Lock'
                      AND query ILIKE 'delete from chinhanh%'
                    """, Integer.class);
            return blocked != null && blocked == count;
        });
    }

    private static String codeOf(MockHttpServletResponse response) throws Exception {
        return com.jayway.jsonpath.JsonPath.read(
                response.getContentAsString(StandardCharsets.UTF_8), "$.code");
    }

    @Test
    @DisplayName("AC-5: a room inserted while a branch is being deleted makes the delete return 2102, not 3006 or 2003")
    void deleteBlockedByConcurrentRoomInsertReturns2102() throws Exception {
        long id = givenBranch();
        otherAdminRuns("INSERT INTO phongkham (ma_chi_nhanh, ten_phong_kham) VALUES (?, ?)", id, "Phòng tranh chấp");

        Future<MockHttpServletResponse> deletion = executor.submit(deleteCall(id));
        waitUntilDeletesAreBlocked(1);
        otherAdmin.commit();
        MockHttpServletResponse response = deletion.get(WAIT_LIMIT.toSeconds(), TimeUnit.SECONDS);

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(codeOf(response)).isEqualTo("2102");
        assertThat(branchExists(id)).isTrue();
    }

    @Test
    @DisplayName("AC-5: when two admins delete the same branch, one receives 1000 and the other 2101, never 9999")
    void concurrentDeletesReturnSuccessAndNotFound() throws Exception {
        long id = givenBranch();
        otherAdminRuns("SELECT 1 FROM chinhanh WHERE ma_chi_nhanh = ? FOR KEY SHARE", id);

        Future<MockHttpServletResponse> first = executor.submit(deleteCall(id));
        Future<MockHttpServletResponse> second = executor.submit(deleteCall(id));
        waitUntilDeletesAreBlocked(2);
        otherAdmin.rollback();

        List<String> codes = List.of(
                codeOf(first.get(WAIT_LIMIT.toSeconds(), TimeUnit.SECONDS)),
                codeOf(second.get(WAIT_LIMIT.toSeconds(), TimeUnit.SECONDS)));
        assertThat(codes).containsExactlyInAnyOrder("1000", "2101");
        assertThat(branchExists(id)).isFalse();
    }

    @Test
    @DisplayName("If an update fails midway, valid fields in the same request are not saved")
    void failedEditLeavesRowUntouched() throws Exception {
        long id = givenBranch();

        MockHttpServletResponse response = mockMvc.perform(put(BRANCHES + id)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Tên mới\", \"address\": \"   \"}"))
                .andReturn().getResponse();

        assertThat(codeOf(response)).isEqualTo("2003");
        String name = jdbcTemplate.queryForObject(
                "SELECT ten_chi_nhanh FROM chinhanh WHERE ma_chi_nhanh = ?", String.class, id);
        assertThat(name).isEqualTo("Cơ sở Test Giao Dịch");
    }
}
