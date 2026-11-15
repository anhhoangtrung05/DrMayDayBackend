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
 * Tests clinic room management endpoints through the real filter chain against PostgreSQL.
 * Each test runs in a rolled-back transaction, so it leaves no rows in the database.
 * Subtype foreign keys are deferred until commit, so the exam session does not need a subtype row here.
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

    // phienkham requires a real patient record; the test transaction will roll back all changes.
    private void givenExamSessionIn(long roomId) {
        Long patient = jdbcTemplate.queryForObject(
                "INSERT INTO nguoi (ho_ten, so_dien_thoai) VALUES ('Bệnh nhân Test', '0987654321') RETURNING ma_nguoi",
                Long.class);
        jdbcTemplate.update("INSERT INTO benhnhan (ma_benh_nhan) VALUES (?)", patient);
        Long record = jdbcTemplate.queryForObject(
                "INSERT INTO hosobenhan (ma_benh_nhan, loai_benh_an) VALUES (?, 'CAP') RETURNING ma_ho_so",
                Long.class, patient);
        Long doctor = jdbcTemplate.queryForObject("SELECT min(ma_nhan_vien) FROM nhanvien", Long.class);
        assertThat(doctor).as("The database must contain an employee seeded by DatabaseSeeder").isNotNull();
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

    // Access control shared by all clinic room endpoints.

    @Test
    @DisplayName("AC-1: all clinic room endpoints return 401 with code 3003 when unauthenticated")
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
    @DisplayName("AC-2: non-admin accounts are blocked from writes with 403 and code 3102; data remains unchanged")
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
    @DisplayName("AC-3: authenticated users can view the room list and details")
    void anyAuthenticatedUserCanRead(String role) throws Exception {
        String token = tokenFor(role);
        long room = givenRoom();

        send(get(ROOMS), token, null).andExpect(status().isOk()).andExpect(jsonPath("$.code").value("1000"));
        send(get(ROOMS + "/" + room), token, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("1000"));
    }

    // TOPOLOGY_SET_ROOM

    @Test
    @DisplayName("AC-14: creating a room returns 201 with code 1001, and room_id matches the new database row")
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
    @DisplayName("AC-4: a numeric JSON branch_id is accepted; room_id and branch_id are returned as strings")
    void acceptsNumericBranchIdAndAnswersWithStrings() throws Exception {
        createAsAdmin("""
                {"branch_id": %d, "name": "Phòng 202"}
                """.formatted(branchId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.room_id").isString())
                .andExpect(jsonPath("$.data.branch_id").isString());
    }

    @Test
    @DisplayName("A name with surrounding whitespace is trimmed before saving")
    void trimsNameOnCreate() throws Exception {
        ResultActions result = createAsAdmin(body(branchId, "  Phòng 202  "))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Phòng 202"));

        String roomId = JsonPath.read(content(result), "$.data.room_id");
        assertThat(roomName(Long.parseLong(roomId))).isEqualTo("Phòng 202");
    }

    @Test
    @DisplayName("AC-15: missing branch_id, missing name, or a whitespace-only name returns 2001")
    void missingFieldsReturn2001() throws Exception {
        expectError(createAsAdmin(nameBody("Phòng 202")), 400, "2001");
        expectError(createAsAdmin(branchBody(branchId)), 400, "2001");
        expectError(createAsAdmin(body(branchId, "   ")), 400, "2001");
    }

    @Test
    @DisplayName("AC-15: a 150-character name is accepted, while a 151-character name returns 2003")
    void nameLengthLimitIs150() throws Exception {
        createAsAdmin(body(branchId, "P".repeat(150))).andExpect(status().isCreated());

        expectError(createAsAdmin(body(branchId, "Q".repeat(151))), 400, "2003");
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "1.5", TOO_BIG_FOR_LONG})
    @DisplayName("AC-15: a non-numeric or overflowing branch_id returns 2002")
    void nonNumericBranchIdReturns2002(String branch) throws Exception {
        expectError(createAsAdmin(body(branch, "Phòng 202")), 400, "2002");
    }

    @Test
    @DisplayName("AC-15: a nonexistent branch returns 404 with code 2101")
    void missingBranchReturns2101() throws Exception {
        expectError(createAsAdmin(body(NO_SUCH_ID, "Phòng 202")), 404, "2101");
    }

    @Test
    @DisplayName("AC-15: an inactive branch returns 400 with code 2106 and no room is created")
    void inactiveBranchReturns2106() throws Exception {
        long inactive = givenBranch("Cơ sở Đóng Cửa", false);

        expectError(createAsAdmin(body(inactive, "Phòng 202")), 400, "2106");

        assertThat(roomCount(inactive)).isZero();
    }

    @Test
    @DisplayName("Malformed JSON returns 2003 instead of 2001")
    void malformedJsonReturns2003() throws Exception {
        expectError(createAsAdmin("{\"branch_id\": "), 400, "2003");
    }

    @Test
    @DisplayName("AC-16: a duplicate name in the same branch returns 409 with code 2104, not 3006")
    void duplicateNameReturns2104() throws Exception {
        givenRoom();

        expectError(createAsAdmin(body(branchId, "Phòng 101")), 409, "2104");

        assertThat(roomCount(branchId)).isEqualTo(1);
    }

    @Test
    @DisplayName("AC-16: a name that becomes a duplicate after trimming still returns 2104")
    void duplicateAfterTrimReturns2104() throws Exception {
        givenRoom();

        expectError(createAsAdmin(body(branchId, "  Phòng 101  ")), 409, "2104");
    }

    @Test
    @DisplayName("AC-16: names with different letter case are accepted")
    void differentCaseIsNotADuplicate() throws Exception {
        givenRoom();

        createAsAdmin(body(branchId, "PHÒNG 101")).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("AC-16: the same name is accepted in a different branch")
    void sameNameInOtherBranchIsAccepted() throws Exception {
        givenRoom();
        long other = givenBranch("Cơ sở Khác", true);

        createAsAdmin(body(other, "Phòng 101")).andExpect(status().isCreated());
    }

    // TOPOLOGY_GET_LIST_ROOMS

    @Test
    @DisplayName("AC-17: the list sorts room_id in ascending order and includes rooms from inactive branches")
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
    @DisplayName("AC-17: the branch_id filter returns only rooms from that branch")
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
    @DisplayName("AC-17: a branch with no rooms returns an empty array, not 2101")
    void branchWithoutRoomsReturnsEmptyArray() throws Exception {
        listAsAdmin("?branch_id=" + branchId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("AC-17: filtering by a nonexistent branch returns 404 with code 2101")
    void filterWithMissingBranchReturns2101() throws Exception {
        expectError(listAsAdmin("?branch_id=" + NO_SUCH_ID), 404, "2101");
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", TOO_BIG_FOR_LONG})
    @DisplayName("AC-17: a non-numeric or overflowing filter returns 2002")
    void filterWithNonNumericBranchReturns2002(String branch) throws Exception {
        expectError(listAsAdmin("?branch_id=" + branch), 400, "2002");
    }

    @Test
    @DisplayName("AC-17: an empty branch_id is treated as no filter")
    void blankFilterMeansNoFilter() throws Exception {
        long room = givenRoom();

        listAsAdmin("?branch_id=")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].room_id", hasItems(String.valueOf(room))));
    }

    // TOPOLOGY_GET_ROOM_DETAIL

    @Test
    @DisplayName("AC-18: room details return the correct room with string-valued fields")
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
    @DisplayName("AC-18: a nonexistent or non-numeric ID returns 404 with code 2103")
    void invalidIdReturns2103(String id) throws Exception {
        expectError(send(get(ROOMS + "/" + id), adminToken, null), 404, "2103");
    }

    @Test
    @DisplayName("Renaming a branch also updates the room's branch_name")
    void branchNameFollowsTheBranch() throws Exception {
        long room = givenRoom();

        send(put(BRANCHES + "/" + branchId), adminToken, nameBody("Cơ sở Đổi Tên")).andExpect(status().isOk());

        send(get(ROOMS + "/" + room), adminToken, null)
                .andExpect(jsonPath("$.data.branch_name").value("Cơ sở Đổi Tên"));
    }

    // TOPOLOGY_EDIT_ROOM: rename the room.

    @Test
    @DisplayName("AC-19: the trimmed new name is saved and the branch remains unchanged")
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
    @DisplayName("AC-19: a room can be renamed while its branch is inactive")
    void renamesRoomInInactiveBranch() throws Exception {
        long inactive = givenBranch("Cơ sở Đóng Cửa", false);
        long room = givenRoom(inactive, "Phòng 101");

        editAsAdmin(room, nameBody("Phòng 202")).andExpect(status().isOk());

        assertThat(roomName(room)).isEqualTo("Phòng 202");
    }

    @Test
    @DisplayName("Resubmitting the current inactive branch does not count as a move and does not return 2106")
    void currentInactiveBranchIsNotAMove() throws Exception {
        long inactive = givenBranch("Cơ sở Đóng Cửa", false);
        long room = givenRoom(inactive, "Phòng 101");

        editAsAdmin(room, body(inactive, "Phòng 202")).andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    @DisplayName("AC-19: an empty or whitespace-only name returns 2003 and preserves the old name")
    void blankNameReturns2003(String name) throws Exception {
        long room = givenRoom();

        expectError(editAsAdmin(room, nameBody(name)), 400, "2003");

        assertThat(roomName(room)).isEqualTo("Phòng 101");
    }

    @Test
    @DisplayName("AC-19: a 151-character name returns 2003")
    void tooLongNameReturns2003OnEdit() throws Exception {
        expectError(editAsAdmin(givenRoom(), nameBody("P".repeat(151))), 400, "2003");
    }

    @Test
    @DisplayName("AC-19: changing to another room's name in the same branch returns 409 with code 2104")
    void renameToExistingNameReturns2104() throws Exception {
        long room = givenRoom();
        givenRoom(branchId, "Phòng 202");

        expectError(editAsAdmin(room, nameBody("Phòng 202")), 409, "2104");

        assertThat(roomName(room)).isEqualTo("Phòng 101");
    }

    @ParameterizedTest
    @ValueSource(strings = {"999999999", "abc", TOO_BIG_FOR_LONG})
    @DisplayName("AC-19: updating with an invalid ID returns 404 with code 2103")
    void editWithInvalidIdReturns2103(String id) throws Exception {
        expectError(editAsAdmin(id, nameBody("Phòng 202")), 404, "2103");
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"name\": \"Phòng 101\"}", "{\"name\": null, \"branch_id\": null}"})
    @DisplayName("AC-19, AC-20: an empty body, unchanged values, or null values return 200 with the current data")
    void noChangeReturnsCurrentData(String json) throws Exception {
        long room = givenRoom();

        editAsAdmin(room, json)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.room_id").value(String.valueOf(room)))
                .andExpect(jsonPath("$.data.branch_id").value(String.valueOf(branchId)))
                .andExpect(jsonPath("$.data.name").value("Phòng 101"));
    }

    // TOPOLOGY_EDIT_ROOM: move the room to another branch.

    @Test
    @DisplayName("AC-20: moving a room with no exam sessions returns the new branch_id and branch_name")
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
    @DisplayName("AC-20: a nonexistent target branch returns 404 with code 2101")
    void moveToMissingBranchReturns2101() throws Exception {
        long room = givenRoom();

        expectError(editAsAdmin(room, branchBody(NO_SUCH_ID)), 404, "2101");

        assertThat(roomBranch(room)).isEqualTo(branchId);
    }

    @Test
    @DisplayName("AC-20: an inactive target branch returns 400 with code 2106")
    void moveToInactiveBranchReturns2106() throws Exception {
        long room = givenRoom();
        long inactive = givenBranch("Cơ sở Đóng Cửa", false);

        expectError(editAsAdmin(room, branchBody(inactive)), 400, "2106");

        assertThat(roomBranch(room)).isEqualTo(branchId);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "abc", TOO_BIG_FOR_LONG})
    @DisplayName("AC-20: an empty, non-numeric, or overflowing branch_id returns 2002")
    void moveWithBadBranchIdReturns2002(String branch) throws Exception {
        expectError(editAsAdmin(givenRoom(), branchBody(branch)), 400, "2002");
    }

    @Test
    @DisplayName("AC-20: a target branch with a room of the same name returns 409 with code 2104")
    void moveIntoNameClashReturns2104() throws Exception {
        long room = givenRoom();
        long target = givenBranch("Cơ sở Đích", true);
        givenRoom(target, "Phòng 101");

        expectError(editAsAdmin(room, branchBody(target)), 409, "2104");

        assertThat(roomBranch(room)).isEqualTo(branchId);
    }

    @Test
    @DisplayName("AC-20: a room with exam sessions cannot be moved and returns 400 with code 2105")
    void roomWithExamSessionCannotMove() throws Exception {
        long room = givenRoom();
        long target = givenBranch("Cơ sở Đích", true);
        givenExamSessionIn(room);

        expectError(editAsAdmin(room, branchBody(target)), 400, "2105");

        assertThat(roomBranch(room)).isEqualTo(branchId);
    }

    @Test
    @DisplayName("AC-20: a room with exam sessions can still be renamed when branch_id matches its current branch")
    void roomWithExamSessionCanStillBeRenamed() throws Exception {
        long room = givenRoom();
        givenExamSessionIn(room);

        editAsAdmin(room, body(branchId, "Phòng 202")).andExpect(status().isOk());

        assertThat(roomName(room)).isEqualTo("Phòng 202");
    }

    // TOPOLOGY_DELETE_ROOM

    @Test
    @DisplayName("AC-21: deleting a room with no exam sessions returns 200 with code 1000 and no data")
    void deletesRoomWithoutExamSessions() throws Exception {
        long room = givenRoom();

        deleteAsAdmin(room)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("1000"))
                .andExpect(jsonPath("$.data").doesNotExist());

        assertThat(roomExists(room)).isFalse();
    }

    @Test
    @DisplayName("AC-22: deleting a room with exam sessions returns 400 with code 2105 and the room remains")
    void roomWithExamSessionCannotBeDeleted() throws Exception {
        long room = givenRoom();
        givenExamSessionIn(room);

        expectError(deleteAsAdmin(room), 400, "2105");

        assertThat(roomExists(room)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"999999999", "abc", TOO_BIG_FOR_LONG})
    @DisplayName("Deleting with an invalid ID returns 404 with code 2103")
    void deleteWithInvalidIdReturns2103(String id) throws Exception {
        expectError(deleteAsAdmin(id), 404, "2103");
    }
}
