package com.example.drmayday.features.facility;

import com.example.drmayday.features.security.JwtTokenProvider;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Kiểm thử API 29 đến 33 qua filter chain thật, xuống PostgreSQL thật.
 * Mỗi test nằm trong một transaction được rollback, nên không để lại dòng nào trong database.
 * Các khóa ngoại subtype là DEFERRED, chỉ kiểm tra lúc commit, nên phiên khám ở đây không cần dòng subtype.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RoomApiIntegrationTest {

    private static final String ROOMS = "/api/v1/rooms";
    private static final String BRANCHES = "/api/v1/branches";
    private static final long NO_SUCH_ID = 999_999_999L;
    private static final String TOO_BIG_FOR_LONG = "99999999999999999999";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String adminToken;
    private long branchId;

    @BeforeEach
    void setUp() {
        adminToken = tokenFor("admin");
        branchId = givenBranch("Cơ sở Test", true);
    }

    private String tokenFor(String role) {
        return jwtTokenProvider.generateToken(1L, "0900000001", role);
    }

    private ResultActions send(MockHttpServletRequestBuilder builder, String token, String json) throws Exception {
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        if (json != null) {
            builder.contentType(MediaType.APPLICATION_JSON).content(json);
        }
        return mockMvc.perform(builder);
    }

    private ResultActions createAsAdmin(String json) throws Exception {
        return send(post(ROOMS), adminToken, json);
    }

    private ResultActions editAsAdmin(Object id, String json) throws Exception {
        return send(put(ROOMS + "/" + id), adminToken, json);
    }

    private ResultActions deleteAsAdmin(Object id) throws Exception {
        return send(delete(ROOMS + "/" + id), adminToken, null);
    }

    private ResultActions listAsAdmin(String query) throws Exception {
        return send(get(ROOMS + query), adminToken, null);
    }

    private long givenBranch(String name, boolean active) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO chinhanh (ten_chi_nhanh, dia_chi, so_dien_thoai, trang_thai)
                VALUES (?, 'Số 1 Phố Huế', '0241234567', ?) RETURNING ma_chi_nhanh
                """, Long.class, name, active);
    }

    private long givenRoom(long inBranch, String name) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO phongkham (ma_chi_nhanh, ten_phong_kham) VALUES (?, ?) RETURNING ma_phong_kham",
                Long.class, inBranch, name);
    }

    private long givenRoom() {
        return givenRoom(branchId, "Phòng 101");
    }

    // phienkham cần hồ sơ thật của một bệnh nhân thật, transaction của test sẽ hoàn tác toàn bộ
    private void givenExamSessionIn(long roomId) {
        Long patient = jdbcTemplate.queryForObject(
                "INSERT INTO nguoi (ho_ten, so_dien_thoai) VALUES ('Bệnh nhân Test', '0987654321') RETURNING ma_nguoi",
                Long.class);
        jdbcTemplate.update("INSERT INTO benhnhan (ma_benh_nhan) VALUES (?)", patient);
        Long record = jdbcTemplate.queryForObject(
                "INSERT INTO hosobenhan (ma_benh_nhan, loai_benh_an) VALUES (?, 'CAP') RETURNING ma_ho_so",
                Long.class, patient);
        Long doctor = jdbcTemplate.queryForObject("SELECT min(ma_nhan_vien) FROM nhanvien", Long.class);
        assertThat(doctor).as("database phải có sẵn nhân viên do DatabaseSeeder tạo").isNotNull();
        jdbcTemplate.update("""
                INSERT INTO phienkham (ma_ho_so, ma_bac_si, ma_phong_kham, loai_benh_an, ngay_kham, lan_kham_thu)
                VALUES (?, ?, ?, 'CAP', CURRENT_DATE, 1)
                """, record, doctor, roomId);
    }

    private int roomCount(long inBranch) {
        Integer rows = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM phongkham WHERE ma_chi_nhanh = ?", Integer.class, inBranch);
        return rows == null ? 0 : rows;
    }

    private boolean roomExists(long id) {
        Integer rows = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM phongkham WHERE ma_phong_kham = ?", Integer.class, id);
        return rows != null && rows == 1;
    }

    private String roomName(long id) {
        return jdbcTemplate.queryForObject(
                "SELECT ten_phong_kham FROM phongkham WHERE ma_phong_kham = ?", String.class, id);
    }

    private long roomBranch(long id) {
        return jdbcTemplate.queryForObject(
                "SELECT ma_chi_nhanh FROM phongkham WHERE ma_phong_kham = ?", Long.class, id);
    }

    private static String content(ResultActions result) throws Exception {
        return result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private static void expectError(ResultActions result, int httpStatus, String code) throws Exception {
        result.andExpect(status().is(httpStatus)).andExpect(jsonPath("$.code").value(code));
    }

    private static String body(Object branch, String name) {
        return """
                {"branch_id": "%s", "name": "%s"}
                """.formatted(branch, name);
    }

    private static String nameBody(String name) {
        return """
                {"name": "%s"}
                """.formatted(name);
    }

    private static String branchBody(Object branch) {
        return """
                {"branch_id": "%s"}
                """.formatted(branch);
    }

    // Quyền truy cập, áp dụng chung cho phòng khám

    @Test
    @DisplayName("AC-1: chưa đăng nhập thì mọi API phòng khám trả 401, mã 3003")
    void anonymousIsRejectedEverywhere() throws Exception {
        long room = givenRoom();

        expectError(send(get(ROOMS), null, null), 401, "3003");
        expectError(send(get(ROOMS + "/" + room), null, null), 401, "3003");
        expectError(send(post(ROOMS), null, body(branchId, "Phòng 202")), 401, "3003");
        expectError(send(put(ROOMS + "/" + room), null, nameBody("Phòng 202")), 401, "3003");
        expectError(send(delete(ROOMS + "/" + room), null, null), 401, "3003");
    }

    @ParameterizedTest
    @ValueSource(strings = {"benh_nhan", "bac_si"})
    @DisplayName("AC-2: tài khoản không phải Admin bị chặn khi ghi, trả 403, mã 3102, dữ liệu không đổi")
    void nonAdminCannotWrite(String role) throws Exception {
        String token = tokenFor(role);
        long room = givenRoom();

        expectError(send(post(ROOMS), token, body(branchId, "Phòng 202")), 403, "3102");
        expectError(send(put(ROOMS + "/" + room), token, nameBody("Phòng 202")), 403, "3102");
        expectError(send(delete(ROOMS + "/" + room), token, null), 403, "3102");

        assertThat(roomCount(branchId)).isEqualTo(1);
        assertThat(roomName(room)).isEqualTo("Phòng 101");
    }

    @ParameterizedTest
    @ValueSource(strings = {"benh_nhan", "bac_si"})
    @DisplayName("AC-3: người đã đăng nhập xem được danh sách và chi tiết phòng")
    void anyAuthenticatedUserCanRead(String role) throws Exception {
        String token = tokenFor(role);
        long room = givenRoom();

        send(get(ROOMS), token, null).andExpect(status().isOk()).andExpect(jsonPath("$.code").value("1000"));
        send(get(ROOMS + "/" + room), token, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("1000"));
    }

    // API 29

    @Test
    @DisplayName("AC-14: Admin tạo phòng nhận 201, mã 1001, và room_id khớp dòng mới trong database")
    void adminCreatesRoom() throws Exception {
        ResultActions result = createAsAdmin(body(branchId, "Phòng 202"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("1001"))
                .andExpect(jsonPath("$.data.branch_id").value(String.valueOf(branchId)))
                .andExpect(jsonPath("$.data.branch_name").value("Cơ sở Test"))
                .andExpect(jsonPath("$.data.name").value("Phòng 202"));

        String roomId = JsonPath.read(content(result), "$.data.room_id");
        assertThat(roomName(Long.parseLong(roomId))).isEqualTo("Phòng 202");
        assertThat(roomBranch(Long.parseLong(roomId))).isEqualTo(branchId);
    }

    @Test
    @DisplayName("AC-4: branch_id gửi dạng số JSON vẫn được nhận, room_id và branch_id trả về là chuỗi")
    void acceptsNumericBranchIdAndAnswersWithStrings() throws Exception {
        createAsAdmin("""
                {"branch_id": %d, "name": "Phòng 202"}
                """.formatted(branchId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.room_id").isString())
                .andExpect(jsonPath("$.data.branch_id").isString());
    }

    @Test
    @DisplayName("Tên có khoảng trắng hai đầu được lưu sau khi trim")
    void trimsNameOnCreate() throws Exception {
        ResultActions result = createAsAdmin(body(branchId, "  Phòng 202  "))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Phòng 202"));

        String roomId = JsonPath.read(content(result), "$.data.room_id");
        assertThat(roomName(Long.parseLong(roomId))).isEqualTo("Phòng 202");
    }

    @Test
    @DisplayName("AC-15: thiếu branch_id, thiếu name, hoặc name chỉ có khoảng trắng đều trả 2001")
    void missingFieldsReturn2001() throws Exception {
        expectError(createAsAdmin(nameBody("Phòng 202")), 400, "2001");
        expectError(createAsAdmin(branchBody(branchId)), 400, "2001");
        expectError(createAsAdmin(body(branchId, "   ")), 400, "2001");
    }

    @Test
    @DisplayName("AC-15: name đúng 150 ký tự được nhận, 151 ký tự trả 2003")
    void nameLengthLimitIs150() throws Exception {
        createAsAdmin(body(branchId, "P".repeat(150))).andExpect(status().isCreated());

        expectError(createAsAdmin(body(branchId, "Q".repeat(151))), 400, "2003");
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "1.5", TOO_BIG_FOR_LONG})
    @DisplayName("AC-15: branch_id không phải số hoặc tràn số trả 2002")
    void nonNumericBranchIdReturns2002(String branch) throws Exception {
        expectError(createAsAdmin(body(branch, "Phòng 202")), 400, "2002");
    }

    @Test
    @DisplayName("AC-15: chi nhánh không tồn tại trả 404, mã 2101")
    void missingBranchReturns2101() throws Exception {
        expectError(createAsAdmin(body(NO_SUCH_ID, "Phòng 202")), 404, "2101");
    }

    @Test
    @DisplayName("AC-15: chi nhánh ngừng hoạt động trả 400, mã 2106, không tạo phòng")
    void inactiveBranchReturns2106() throws Exception {
        long inactive = givenBranch("Cơ sở Đóng Cửa", false);

        expectError(createAsAdmin(body(inactive, "Phòng 202")), 400, "2106");

        assertThat(roomCount(inactive)).isZero();
    }

    @Test
    @DisplayName("JSON hỏng trả 2003, không phải 2001")
    void malformedJsonReturns2003() throws Exception {
        expectError(createAsAdmin("{\"branch_id\": "), 400, "2003");
    }

    @Test
    @DisplayName("AC-16: trùng tên trong cùng chi nhánh trả 409, mã 2104, không phải 3006")
    void duplicateNameReturns2104() throws Exception {
        givenRoom();

        expectError(createAsAdmin(body(branchId, "Phòng 101")), 409, "2104");

        assertThat(roomCount(branchId)).isEqualTo(1);
    }

    @Test
    @DisplayName("AC-16: tên chỉ trùng sau khi trim vẫn trả 2104")
    void duplicateAfterTrimReturns2104() throws Exception {
        givenRoom();

        expectError(createAsAdmin(body(branchId, "  Phòng 101  ")), 409, "2104");
    }

    @Test
    @DisplayName("AC-16: tên khác hoa thường được chấp nhận")
    void differentCaseIsNotADuplicate() throws Exception {
        givenRoom();

        createAsAdmin(body(branchId, "PHÒNG 101")).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("AC-16: cùng tên ở chi nhánh khác được chấp nhận")
    void sameNameInOtherBranchIsAccepted() throws Exception {
        givenRoom();
        long other = givenBranch("Cơ sở Khác", true);

        createAsAdmin(body(other, "Phòng 101")).andExpect(status().isCreated());
    }

    // API 30

    @Test
    @DisplayName("AC-17: danh sách xếp room_id tăng dần và có cả phòng của chi nhánh ngừng hoạt động")
    void listsAllRoomsInOrderIncludingInactiveBranches() throws Exception {
        long first = givenRoom();
        long second = givenRoom(givenBranch("Cơ sở Đóng Cửa", false), "Phòng 909");

        ResultActions result = listAsAdmin("")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("1000"))
                .andExpect(jsonPath("$.data[*].room_id", hasItems(String.valueOf(first), String.valueOf(second))));

        List<String> ids = JsonPath.read(content(result), "$.data[*].room_id");
        assertThat(ids.stream().map(Long::parseLong).toList()).isSorted();
    }

    @Test
    @DisplayName("AC-17: bộ lọc branch_id chỉ trả phòng của chi nhánh đó")
    void filtersByBranch() throws Exception {
        long mine = givenRoom();
        long other = givenBranch("Cơ sở Khác", true);
        long notMine = givenRoom(other, "Phòng 909");

        listAsAdmin("?branch_id=" + branchId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].room_id").value(String.valueOf(mine)))
                .andExpect(jsonPath("$.data[*].room_id", not(hasItems(String.valueOf(notMine)))));
    }

    @Test
    @DisplayName("AC-17: chi nhánh không có phòng trả mảng rỗng, không phải 2101")
    void branchWithoutRoomsReturnsEmptyArray() throws Exception {
        listAsAdmin("?branch_id=" + branchId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("AC-17: bộ lọc với chi nhánh không tồn tại trả 404, mã 2101")
    void filterWithMissingBranchReturns2101() throws Exception {
        expectError(listAsAdmin("?branch_id=" + NO_SUCH_ID), 404, "2101");
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", TOO_BIG_FOR_LONG})
    @DisplayName("AC-17: bộ lọc không phải số hoặc tràn số trả 2002")
    void filterWithNonNumericBranchReturns2002(String branch) throws Exception {
        expectError(listAsAdmin("?branch_id=" + branch), 400, "2002");
    }

    @Test
    @DisplayName("AC-17: branch_id để trống được coi như không lọc")
    void blankFilterMeansNoFilter() throws Exception {
        long room = givenRoom();

        listAsAdmin("?branch_id=")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].room_id", hasItems(String.valueOf(room))));
    }

    // API 31

    @Test
    @DisplayName("AC-18: chi tiết trả đúng phòng với các trường dạng chuỗi")
    void returnsRoomDetails() throws Exception {
        long room = givenRoom();

        send(get(ROOMS + "/" + room), adminToken, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("1000"))
                .andExpect(jsonPath("$.data.room_id").value(String.valueOf(room)))
                .andExpect(jsonPath("$.data.branch_id").value(String.valueOf(branchId)))
                .andExpect(jsonPath("$.data.branch_name").value("Cơ sở Test"))
                .andExpect(jsonPath("$.data.name").value("Phòng 101"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"999999999", "abc", TOO_BIG_FOR_LONG, "-1", "0"})
    @DisplayName("AC-18: id không tồn tại hoặc không phải số trả 404, mã 2103")
    void invalidIdReturns2103(String id) throws Exception {
        expectError(send(get(ROOMS + "/" + id), adminToken, null), 404, "2103");
    }

    @Test
    @DisplayName("Đổi tên chi nhánh thì branch_name của phòng đổi theo")
    void branchNameFollowsTheBranch() throws Exception {
        long room = givenRoom();

        send(put(BRANCHES + "/" + branchId), adminToken, nameBody("Cơ sở Đổi Tên")).andExpect(status().isOk());

        send(get(ROOMS + "/" + room), adminToken, null)
                .andExpect(jsonPath("$.data.branch_name").value("Cơ sở Đổi Tên"));
    }

    // API 32, đổi tên

    @Test
    @DisplayName("AC-19: đổi tên được lưu sau khi trim, chi nhánh giữ nguyên")
    void renamesRoom() throws Exception {
        long room = givenRoom();

        editAsAdmin(room, nameBody("  Phòng 202  "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("1000"))
                .andExpect(jsonPath("$.data.name").value("Phòng 202"))
                .andExpect(jsonPath("$.data.branch_id").value(String.valueOf(branchId)));

        assertThat(roomName(room)).isEqualTo("Phòng 202");
        assertThat(roomBranch(room)).isEqualTo(branchId);
    }

    @Test
    @DisplayName("AC-19: đổi tên được cả khi chi nhánh đang ngừng hoạt động")
    void renamesRoomInInactiveBranch() throws Exception {
        long inactive = givenBranch("Cơ sở Đóng Cửa", false);
        long room = givenRoom(inactive, "Phòng 101");

        editAsAdmin(room, nameBody("Phòng 202")).andExpect(status().isOk());

        assertThat(roomName(room)).isEqualTo("Phòng 202");
    }

    @Test
    @DisplayName("Gửi lại chi nhánh ngừng hoạt động hiện tại không tính là chuyển, nên không trả 2106")
    void currentInactiveBranchIsNotAMove() throws Exception {
        long inactive = givenBranch("Cơ sở Đóng Cửa", false);
        long room = givenRoom(inactive, "Phòng 101");

        editAsAdmin(room, body(inactive, "Phòng 202")).andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    @DisplayName("AC-19: name rỗng hoặc chỉ có khoảng trắng trả 2003, tên cũ giữ nguyên")
    void blankNameReturns2003(String name) throws Exception {
        long room = givenRoom();

        expectError(editAsAdmin(room, nameBody(name)), 400, "2003");

        assertThat(roomName(room)).isEqualTo("Phòng 101");
    }

    @Test
    @DisplayName("AC-19: name 151 ký tự trả 2003")
    void tooLongNameReturns2003OnEdit() throws Exception {
        expectError(editAsAdmin(givenRoom(), nameBody("P".repeat(151))), 400, "2003");
    }

    @Test
    @DisplayName("AC-19: đổi sang tên phòng khác trong chi nhánh trả 409, mã 2104")
    void renameToExistingNameReturns2104() throws Exception {
        long room = givenRoom();
        givenRoom(branchId, "Phòng 202");

        expectError(editAsAdmin(room, nameBody("Phòng 202")), 409, "2104");

        assertThat(roomName(room)).isEqualTo("Phòng 101");
    }

    @ParameterizedTest
    @ValueSource(strings = {"999999999", "abc", TOO_BIG_FOR_LONG})
    @DisplayName("AC-19: id sai khi sửa trả 404, mã 2103")
    void editWithInvalidIdReturns2103(String id) throws Exception {
        expectError(editAsAdmin(id, nameBody("Phòng 202")), 404, "2103");
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"name\": \"Phòng 101\"}", "{\"name\": null, \"branch_id\": null}"})
    @DisplayName("AC-19, AC-20: body rỗng, giá trị không đổi, hoặc null thì trả 200 với dữ liệu hiện tại")
    void noChangeReturnsCurrentData(String json) throws Exception {
        long room = givenRoom();

        editAsAdmin(room, json)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.room_id").value(String.valueOf(room)))
                .andExpect(jsonPath("$.data.branch_id").value(String.valueOf(branchId)))
                .andExpect(jsonPath("$.data.name").value("Phòng 101"));
    }

    // API 32, chuyển chi nhánh

    @Test
    @DisplayName("AC-20: chuyển phòng chưa có phiên khám trả branch_id và branch_name mới")
    void movesRoomToOtherBranch() throws Exception {
        long room = givenRoom();
        long target = givenBranch("Cơ sở Đích", true);

        editAsAdmin(room, branchBody(target))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.branch_id").value(String.valueOf(target)))
                .andExpect(jsonPath("$.data.branch_name").value("Cơ sở Đích"))
                .andExpect(jsonPath("$.data.name").value("Phòng 101"));

        assertThat(roomBranch(room)).isEqualTo(target);
    }

    @Test
    @DisplayName("AC-20: chi nhánh đích không tồn tại trả 404, mã 2101")
    void moveToMissingBranchReturns2101() throws Exception {
        long room = givenRoom();

        expectError(editAsAdmin(room, branchBody(NO_SUCH_ID)), 404, "2101");

        assertThat(roomBranch(room)).isEqualTo(branchId);
    }

    @Test
    @DisplayName("AC-20: chi nhánh đích ngừng hoạt động trả 400, mã 2106")
    void moveToInactiveBranchReturns2106() throws Exception {
        long room = givenRoom();
        long inactive = givenBranch("Cơ sở Đóng Cửa", false);

        expectError(editAsAdmin(room, branchBody(inactive)), 400, "2106");

        assertThat(roomBranch(room)).isEqualTo(branchId);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "abc", TOO_BIG_FOR_LONG})
    @DisplayName("AC-20: branch_id rỗng, không phải số, hoặc tràn số trả 2002")
    void moveWithBadBranchIdReturns2002(String branch) throws Exception {
        expectError(editAsAdmin(givenRoom(), branchBody(branch)), 400, "2002");
    }

    @Test
    @DisplayName("AC-20: chi nhánh đích đã có phòng cùng tên trả 409, mã 2104")
    void moveIntoNameClashReturns2104() throws Exception {
        long room = givenRoom();
        long target = givenBranch("Cơ sở Đích", true);
        givenRoom(target, "Phòng 101");

        expectError(editAsAdmin(room, branchBody(target)), 409, "2104");

        assertThat(roomBranch(room)).isEqualTo(branchId);
    }

    @Test
    @DisplayName("AC-20: phòng có phiên khám không chuyển được, trả 400, mã 2105")
    void roomWithExamSessionCannotMove() throws Exception {
        long room = givenRoom();
        long target = givenBranch("Cơ sở Đích", true);
        givenExamSessionIn(room);

        expectError(editAsAdmin(room, branchBody(target)), 400, "2105");

        assertThat(roomBranch(room)).isEqualTo(branchId);
    }

    @Test
    @DisplayName("AC-20: phòng có phiên khám vẫn đổi tên được khi gửi branch_id trùng chi nhánh hiện tại")
    void roomWithExamSessionCanStillBeRenamed() throws Exception {
        long room = givenRoom();
        givenExamSessionIn(room);

        editAsAdmin(room, body(branchId, "Phòng 202")).andExpect(status().isOk());

        assertThat(roomName(room)).isEqualTo("Phòng 202");
    }

    // API 33

    @Test
    @DisplayName("AC-21: xóa phòng chưa có phiên khám trả 200, mã 1000, không có data")
    void deletesRoomWithoutExamSessions() throws Exception {
        long room = givenRoom();

        deleteAsAdmin(room)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("1000"))
                .andExpect(jsonPath("$.data").doesNotExist());

        assertThat(roomExists(room)).isFalse();
    }

    @Test
    @DisplayName("AC-22: xóa phòng có phiên khám trả 400, mã 2105, phòng vẫn còn")
    void roomWithExamSessionCannotBeDeleted() throws Exception {
        long room = givenRoom();
        givenExamSessionIn(room);

        expectError(deleteAsAdmin(room), 400, "2105");

        assertThat(roomExists(room)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"999999999", "abc", TOO_BIG_FOR_LONG})
    @DisplayName("Xóa với id sai trả 404, mã 2103")
    void deleteWithInvalidIdReturns2103(String id) throws Exception {
        expectError(deleteAsAdmin(id), 404, "2103");
    }
}
