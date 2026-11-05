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
 * Kiểm thử hành vi cần transaction commit thật: tranh chấp giữa hai Admin (AC-5) và rollback toàn bộ.
 * Lớp này ghi thật vào database rồi tự dọn, nên chỉ chạy trên Postgres local. Gặp server khác thì test bị bỏ qua.
 * Thay vì chờ một khoảng cố định, test đọc pg_stat_activity để biết chắc request đã bị database chặn.
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
                "Lớp test này commit dữ liệu thật nên chỉ chạy trên Postgres local");

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
    @DisplayName("AC-5: phòng được chèn vào chi nhánh đúng lúc đang xóa thì bên xóa nhận 2102, không phải 3006 hay 2003")
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
    @DisplayName("AC-5: hai Admin cùng xóa một chi nhánh thì một bên 1000, bên kia 2101, không có 9999")
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
    @DisplayName("Sửa thất bại giữa chừng thì trường hợp lệ gửi cùng request cũng không được lưu")
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
