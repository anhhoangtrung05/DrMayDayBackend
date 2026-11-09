package com.example.drmayday.features.facility;

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Kiểm thử tranh chấp giữa hai giao dịch trên phòng khám (AC-5, AC-20). Lớp này cần commit thật.
 * Test ghi thật vào database rồi tự dọn, nên chỉ chạy trên Postgres local. Gặp server khác thì test bị bỏ qua.
 * Thay vì chờ một khoảng cố định, test đọc pg_stat_activity để biết chắc request đã bị database chặn.
 */
@SpringBootTest
@AutoConfigureMockMvc
class RoomTransactionIntegrationTest {

    private static final String ROOMS = "/api/v1/rooms";
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
    private final List<Long> createdPatients = new ArrayList<>();
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
        // Khóa ngoại subtype là DEFERRED nên phải xóa cả chuỗi trong cùng một transaction
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            for (Long patient : createdPatients) {
                run(connection, """
                        DELETE FROM phienkhamcap WHERE ma_phien_kham IN (
                            SELECT p.ma_phien_kham FROM phienkham p
                            JOIN hosobenhan h ON h.ma_ho_so = p.ma_ho_so WHERE h.ma_benh_nhan = ?)
                        """, patient);
                run(connection, """
                        DELETE FROM phienkham WHERE ma_ho_so IN (
                            SELECT ma_ho_so FROM hosobenhan WHERE ma_benh_nhan = ?)
                        """, patient);
                run(connection, """
                        DELETE FROM benhanmaydaycap WHERE ma_ho_so IN (
                            SELECT ma_ho_so FROM hosobenhan WHERE ma_benh_nhan = ?)
                        """, patient);
                run(connection, "DELETE FROM hosobenhan WHERE ma_benh_nhan = ?", patient);
                run(connection, "DELETE FROM benhnhan WHERE ma_benh_nhan = ?", patient);
                run(connection, "DELETE FROM nguoi WHERE ma_nguoi = ?", patient);
            }
            for (Long branch : createdBranches) {
                run(connection, "DELETE FROM phongkham WHERE ma_chi_nhanh = ?", branch);
                run(connection, "DELETE FROM chinhanh WHERE ma_chi_nhanh = ?", branch);
            }
            connection.commit();
        }
    }

    private static void run(Connection connection, String sql, Object... params) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, params);
            statement.execute();
        }
    }

    private static long insertReturningId(Connection connection, String sql, Object... params) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, params);
            try (ResultSet row = statement.executeQuery()) {
                row.next();
                return row.getLong(1);
            }
        }
    }

    private static void bind(PreparedStatement statement, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            statement.setObject(i + 1, params[i]);
        }
    }

    private long givenBranch(String name) {
        Long id = jdbcTemplate.queryForObject(
                "INSERT INTO chinhanh (ten_chi_nhanh, dia_chi, so_dien_thoai) VALUES (?, ?, ?) RETURNING ma_chi_nhanh",
                Long.class, name, "Số 1 Phố Huế", "0241234567");
        createdBranches.add(id);
        return id;
    }

    private long givenRoom(long branch, String name) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO phongkham (ma_chi_nhanh, ten_phong_kham) VALUES (?, ?) RETURNING ma_phong_kham",
                Long.class, branch, name);
    }

    // Hồ sơ bắt buộc có dòng subtype, khóa ngoại subtype được kiểm tra lúc commit
    private long givenCommittedMedicalRecord() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            long patient = insertReturningId(connection,
                    "INSERT INTO nguoi (ho_ten, so_dien_thoai) VALUES ('Bệnh nhân Test', '0987654321') RETURNING ma_nguoi");
            createdPatients.add(patient);
            run(connection, "INSERT INTO benhnhan (ma_benh_nhan) VALUES (?)", patient);
            long record = insertReturningId(connection,
                    "INSERT INTO hosobenhan (ma_benh_nhan, loai_benh_an) VALUES (?, 'CAP') RETURNING ma_ho_so",
                    patient);
            run(connection, "INSERT INTO benhanmaydaycap (ma_ho_so) VALUES (?)", record);
            connection.commit();
            return record;
        }
    }

    // Câu INSERT này lấy FOR KEY SHARE trên dòng phongkham, đúng thứ API 32 và 33 phải tranh chấp với
    private void otherAdminInsertsExamSession(long record, long room) throws SQLException {
        Long doctor = jdbcTemplate.queryForObject("SELECT min(ma_nhan_vien) FROM nhanvien", Long.class);
        long session = insertReturningId(otherAdmin, """
                INSERT INTO phienkham (ma_ho_so, ma_bac_si, ma_phong_kham, loai_benh_an, ngay_kham, lan_kham_thu)
                VALUES (?, ?, ?, 'CAP', CURRENT_DATE, 1) RETURNING ma_phien_kham
                """, record, doctor, room);
        run(otherAdmin, "INSERT INTO phienkhamcap (ma_phien_kham) VALUES (?)", session);
    }

    private Future<MockHttpServletResponse> sendInBackground(MockHttpServletRequestBuilder builder, String json) {
        builder.header("Authorization", "Bearer " + adminToken);
        if (json != null) {
            builder.contentType(MediaType.APPLICATION_JSON).content(json);
        }
        Callable<MockHttpServletResponse> call = () -> mockMvc.perform(builder).andReturn().getResponse();
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

    private boolean roomExists(long id) {
        Integer rows = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM phongkham WHERE ma_phong_kham = ?", Integer.class, id);
        return rows != null && rows == 1;
    }

    private long roomBranch(long id) {
        return jdbcTemplate.queryForObject(
                "SELECT ma_chi_nhanh FROM phongkham WHERE ma_phong_kham = ?", Long.class, id);
    }

    private String roomName(long id) {
        return jdbcTemplate.queryForObject(
                "SELECT ten_phong_kham FROM phongkham WHERE ma_phong_kham = ?", String.class, id);
    }

    private int roomCount(long branch, String name) {
        Integer rows = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM phongkham WHERE ma_chi_nhanh = ? AND ten_phong_kham = ?",
                Integer.class, branch, name);
        return rows == null ? 0 : rows;
    }

    @Test
    @DisplayName("AC-5: phòng cùng tên chen vào lúc đang tạo thì API 29 trả 2104, không phải 3006")
    void concurrentSameNameOnCreateReturns2104() throws Exception {
        long branch = givenBranch("Cơ sở Tranh Chấp");
        run(otherAdmin, "INSERT INTO phongkham (ma_chi_nhanh, ten_phong_kham) VALUES (?, ?)", branch, "Phòng 101");

        Future<MockHttpServletResponse> creation = sendInBackground(post(ROOMS), """
                {"branch_id": "%d", "name": "Phòng 101"}
                """.formatted(branch));
        waitUntilBlocked("insert into phongkham", 1);
        otherAdmin.commit();
        MockHttpServletResponse response = result(creation);

        assertThat(response.getStatus()).isEqualTo(409);
        assertThat(codeOf(response)).isEqualTo("2104");
        assertThat(roomCount(branch, "Phòng 101")).isEqualTo(1);
    }

    @Test
    @DisplayName("AC-5: phòng cùng tên chen vào lúc đang đổi tên thì API 32 trả 2104, tên cũ giữ nguyên")
    void concurrentSameNameOnRenameReturns2104() throws Exception {
        long branch = givenBranch("Cơ sở Tranh Chấp");
        long room = givenRoom(branch, "Phòng 101");
        run(otherAdmin, "INSERT INTO phongkham (ma_chi_nhanh, ten_phong_kham) VALUES (?, ?)", branch, "Phòng 202");

        Future<MockHttpServletResponse> rename = sendInBackground(put(ROOMS + "/" + room), """
                {"name": "Phòng 202"}
                """);
        waitUntilBlocked("update phongkham", 1);
        otherAdmin.commit();
        MockHttpServletResponse response = result(rename);

        assertThat(response.getStatus()).isEqualTo(409);
        assertThat(codeOf(response)).isEqualTo("2104");
        assertThat(roomName(room)).isEqualTo("Phòng 101");
    }

    @Test
    @DisplayName("AC-5: chi nhánh bị xóa đúng lúc đang tạo phòng thì API 29 trả 2101")
    void branchDeletedDuringCreateReturns2101() throws Exception {
        long branch = givenBranch("Cơ sở Tranh Chấp");
        run(otherAdmin, "DELETE FROM chinhanh WHERE ma_chi_nhanh = ?", branch);

        Future<MockHttpServletResponse> creation = sendInBackground(post(ROOMS), """
                {"branch_id": "%d", "name": "Phòng 101"}
                """.formatted(branch));
        waitUntilBlocked("insert into phongkham", 1);
        otherAdmin.commit();
        MockHttpServletResponse response = result(creation);

        assertThat(response.getStatus()).isEqualTo(404);
        assertThat(codeOf(response)).isEqualTo("2101");
    }

    @Test
    @DisplayName("AC-5: phiên khám chen vào lúc đang xóa phòng thì API 33 trả 2105, phòng vẫn còn")
    void examSessionDuringDeleteReturns2105() throws Exception {
        long branch = givenBranch("Cơ sở Tranh Chấp");
        long room = givenRoom(branch, "Phòng 101");
        long record = givenCommittedMedicalRecord();
        otherAdminInsertsExamSession(record, room);

        Future<MockHttpServletResponse> deletion = sendInBackground(delete(ROOMS + "/" + room), null);
        waitUntilBlocked("delete from phongkham", 1);
        otherAdmin.commit();
        MockHttpServletResponse response = result(deletion);

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(codeOf(response)).isEqualTo("2105");
        assertThat(roomExists(room)).isTrue();
    }

    @Test
    @DisplayName("AC-5, AC-20: phiên khám chưa commit chặn API 32, commit xong thì trả 2105 và phòng ở chi nhánh cũ")
    void examSessionCommittedDuringMoveReturns2105() throws Exception {
        long branch = givenBranch("Cơ sở Tranh Chấp");
        long target = givenBranch("Cơ sở Đích");
        long room = givenRoom(branch, "Phòng 101");
        long record = givenCommittedMedicalRecord();
        otherAdminInsertsExamSession(record, room);

        Future<MockHttpServletResponse> move = sendInBackground(put(ROOMS + "/" + room), """
                {"branch_id": "%d"}
                """.formatted(target));
        waitUntilBlocked("select * from phongkham", 1);
        otherAdmin.commit();
        MockHttpServletResponse response = result(move);

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(codeOf(response)).isEqualTo("2105");
        assertThat(roomBranch(room)).isEqualTo(branch);
    }

    @Test
    @DisplayName("AC-5, AC-20: FOR KEY SHARE chặn API 32, rollback xong thì chuyển phòng thành công")
    void moveWaitsForKeyShareLockThenSucceeds() throws Exception {
        long branch = givenBranch("Cơ sở Tranh Chấp");
        long target = givenBranch("Cơ sở Đích");
        long room = givenRoom(branch, "Phòng 101");
        run(otherAdmin, "SELECT 1 FROM phongkham WHERE ma_phong_kham = ? FOR KEY SHARE", room);

        Future<MockHttpServletResponse> move = sendInBackground(put(ROOMS + "/" + room), """
                {"branch_id": "%d"}
                """.formatted(target));
        waitUntilBlocked("select * from phongkham", 1);
        otherAdmin.rollback();
        MockHttpServletResponse response = result(move);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(codeOf(response)).isEqualTo("1000");
        assertThat(roomBranch(room)).isEqualTo(target);
    }

    @Test
    @DisplayName("AC-5: hai Admin cùng xóa một phòng thì một bên 1000, bên kia 2103, không có 9999")
    void concurrentDeletesReturnSuccessAndNotFound() throws Exception {
        long branch = givenBranch("Cơ sở Tranh Chấp");
        long room = givenRoom(branch, "Phòng 101");
        run(otherAdmin, "SELECT 1 FROM phongkham WHERE ma_phong_kham = ? FOR KEY SHARE", room);

        Future<MockHttpServletResponse> first = sendInBackground(delete(ROOMS + "/" + room), null);
        Future<MockHttpServletResponse> second = sendInBackground(delete(ROOMS + "/" + room), null);
        waitUntilBlocked("delete from phongkham", 2);
        otherAdmin.rollback();

        List<String> codes = List.of(codeOf(result(first)), codeOf(result(second)));
        assertThat(codes).containsExactlyInAnyOrder("1000", "2103");
        assertThat(roomExists(room)).isFalse();
    }

    @Test
    @DisplayName("Sửa thất bại giữa chừng thì tên hợp lệ gửi cùng request cũng không được lưu")
    void failedEditLeavesRowUntouched() throws Exception {
        long branch = givenBranch("Cơ sở Tranh Chấp");
        long room = givenRoom(branch, "Phòng 101");

        MockHttpServletResponse response = result(sendInBackground(put(ROOMS + "/" + room), """
                {"name": "Tên mới", "branch_id": "abc"}
                """));

        assertThat(codeOf(response)).isEqualTo("2002");
        assertThat(roomName(room)).isEqualTo("Phòng 101");
    }
}
