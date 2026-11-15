package com.example.drmayday.features.facility;

import com.example.drmayday.entity.Branch;
import com.example.drmayday.features.security.JwtTokenProvider;
import com.example.drmayday.repository.BranchRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests branch management endpoints through the real filter chain against PostgreSQL.
 * Each test runs in a rolled-back transaction, so it leaves no rows in the database.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BranchApiIntegrationTest {

    private static final String BRANCHES = "/api/v1/branches";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String adminToken;
    private String patientToken;
    private String doctorToken;

    @BeforeEach
    void setUp() {
        adminToken = jwtTokenProvider.generateToken(1L, "0900000001", "admin");
        patientToken = jwtTokenProvider.generateToken(2L, "0912345678", "benh_nhan");
        doctorToken = jwtTokenProvider.generateToken(3L, "0912345679", "bac_si");
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
        return send(post(BRANCHES), adminToken, json);
    }

    private ResultActions editAsAdmin(long id, String json) throws Exception {
        return send(put(BRANCHES + "/" + id), adminToken, json);
    }

    private ResultActions deleteAsAdmin(long id) throws Exception {
        return send(delete(BRANCHES + "/" + id), adminToken, null);
    }

    private long givenBranch(String name, String phone, boolean active) {
        return branchRepository
                .saveAndFlush(new Branch(null, name, "Số 1 Phố Huế", phone, active))
                .getBranchId();
    }

    private long givenBranch() {
        return givenBranch("Cơ sở Test", "0241234567", true);
    }

    private void givenRoomIn(long branchId) {
        jdbcTemplate.update("INSERT INTO phongkham (ma_chi_nhanh, ten_phong_kham) VALUES (?, ?)",
                branchId, "Phòng 101");
    }

    // Move an existing employee to this branch; the test transaction will roll back the change.
    private void givenStaffIn(long branchId) {
        int moved = jdbcTemplate.update(
                "UPDATE nhanvien SET ma_chi_nhanh = ? WHERE ma_nhan_vien = (SELECT min(ma_nhan_vien) FROM nhanvien)",
                branchId);
        assertThat(moved).as("The database must contain an employee seeded by DatabaseSeeder").isEqualTo(1);
    }

    private boolean branchExists(long id) {
        Integer rows = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM chinhanh WHERE ma_chi_nhanh = ?", Integer.class, id);
        return rows != null && rows == 1;
    }

    private String phoneColumn(long id) {
        return jdbcTemplate.queryForObject(
                "SELECT so_dien_thoai FROM chinhanh WHERE ma_chi_nhanh = ?", String.class, id);
    }

    private String nameColumn(long id) {
        return jdbcTemplate.queryForObject(
                "SELECT ten_chi_nhanh FROM chinhanh WHERE ma_chi_nhanh = ?", String.class, id);
    }

    private static void expectError(ResultActions result, int httpStatus, String code) throws Exception {
        result.andExpect(status().is(httpStatus)).andExpect(jsonPath("$.code").value(code));
    }

    private static String validBody() {
        return """
                {"name": "Cơ sở Cầu Giấy", "address": "Số 2 Duy Tân"}
                """;
    }

    private static String bodyWithPhone(String phone) {
        return """
                {"name": "Cơ sở Cầu Giấy", "address": "Số 2 Duy Tân", "phone": "%s"}
                """.formatted(phone);
    }

    private static String body(String name, String address) {
        return """
                {"name": "%s", "address": "%s"}
                """.formatted(name, address);
    }

    // Access control.

    @ParameterizedTest
    @CsvSource({"GET, /api/v1/branches", "GET, /api/v1/branches/1", "POST, /api/v1/branches",
            "PUT, /api/v1/branches/1", "DELETE, /api/v1/branches/1"})
    @DisplayName("AC-1: no token returns 401 with code 3003")
    void rejectsMissingToken(String method, String path) throws Exception {
        expectError(send(request(HttpMethod.valueOf(method), path), null, validBody()), 401, "3003");
    }

    @Test
    @DisplayName("AC-1: a forged token returns 401 with code 3001")
    void rejectsTamperedToken() throws Exception {
        // Change the first signature character. Appending a character is insufficient because Base64 ignores incomplete trailing bytes.
        int signatureStart = adminToken.lastIndexOf('.') + 1;
        char replacement = adminToken.charAt(signatureStart) == 'A' ? 'B' : 'A';
        String tampered = adminToken.substring(0, signatureStart) + replacement
                + adminToken.substring(signatureStart + 1);

        expectError(send(get(BRANCHES), tampered, null), 401, "3001");
    }

    @ParameterizedTest
    @ValueSource(strings = {"patient", "doctor"})
    @DisplayName("AC-2: a non-admin POST returns 403 with code 3102 and does not create a branch")
    void nonAdminCannotCreate(String who) throws Exception {
        long before = branchRepository.count();

        expectError(send(post(BRANCHES), tokenOf(who), validBody()), 403, "3102");

        assertThat(branchRepository.count()).isEqualTo(before);
    }

    @ParameterizedTest
    @ValueSource(strings = {"patient", "doctor"})
    @DisplayName("AC-2: a non-admin PUT returns 403 with code 3102 and leaves the data unchanged")
    void nonAdminCannotEdit(String who) throws Exception {
        long id = givenBranch();

        expectError(send(put(BRANCHES + "/" + id), tokenOf(who), "{\"name\": \"Tên mới\"}"), 403, "3102");

        assertThat(nameColumn(id)).isEqualTo("Cơ sở Test");
    }

    @ParameterizedTest
    @ValueSource(strings = {"patient", "doctor"})
    @DisplayName("AC-2: a non-admin DELETE returns 403 with code 3102 and the branch remains")
    void nonAdminCannotDelete(String who) throws Exception {
        long id = givenBranch();

        expectError(send(delete(BRANCHES + "/" + id), tokenOf(who), null), 403, "3102");

        assertThat(branchExists(id)).isTrue();
    }

    private String tokenOf(String who) {
        return "patient".equals(who) ? patientToken : doctorToken;
    }

    @ParameterizedTest
    @ValueSource(strings = {"benh_nhan", "bac_si", "dieu_duong", "admin"})
    @DisplayName("AC-3: every authenticated role can read the branch list and details")
    void everyRoleCanRead(String role) throws Exception {
        long id = givenBranch();
        String token = jwtTokenProvider.generateToken(9L, "0900000009", role);

        send(get(BRANCHES), token, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("1000"));
        send(get(BRANCHES + "/" + id), token, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("1000"));
    }

    // TOPOLOGY_SET_BRANCH

    @Test
    @DisplayName("AC-4, AC-6: valid creation returns 201 with code 1001; branch_id is a string matching the new database row")
    void createsBranch() throws Exception {
        String response = createAsAdmin(bodyWithPhone("0241234567"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("1001"))
                .andExpect(jsonPath("$.data.branch_id").isString())
                .andExpect(jsonPath("$.data.name").value("Cơ sở Cầu Giấy"))
                .andExpect(jsonPath("$.data.address").value("Số 2 Duy Tân"))
                .andExpect(jsonPath("$.data.phone").value("0241234567"))
                .andExpect(jsonPath("$.data.status").value(true))
                .andReturn().getResponse().getContentAsString();

        long id = Long.parseLong(JsonPath.read(response, "$.data.branch_id"));
        assertThat(nameColumn(id)).isEqualTo("Cơ sở Cầu Giấy");
    }

    @Test
    @DisplayName("AC-6: the name and address are trimmed before saving")
    void trimsNameAndAddress() throws Exception {
        createAsAdmin(body("  Cơ sở Đống Đa  ", "  Số 3 Tây Sơn  "))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Cơ sở Đống Đa"))
                .andExpect(jsonPath("$.data.address").value("Số 3 Tây Sơn"));
    }

    @Test
    @DisplayName("AC-6: a false status supplied by the client is still set to true on creation")
    void createIgnoresClientStatus() throws Exception {
        createAsAdmin("""
                {"name": "Cơ sở A", "address": "Địa chỉ A", "status": false}
                """)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value(true));
    }

    @Test
    @DisplayName("AC-6: creating a branch with an empty phone stores NULL in the database")
    void createStoresEmptyPhoneAsNull() throws Exception {
        String response = createAsAdmin(bodyWithPhone(""))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        long id = Long.parseLong(JsonPath.read(response, "$.data.branch_id"));
        assertThat(phoneColumn(id)).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"address\": \"Số 2 Duy Tân\"}",
            "{\"name\": \"Cơ sở A\"}",
            "{\"name\": \"\", \"address\": \"Số 2 Duy Tân\"}",
            "{\"name\": \"   \", \"address\": \"Số 2 Duy Tân\"}",
            "{\"name\": \"Cơ sở A\", \"address\": \"   \"}",
            "{}"})
    @DisplayName("AC-7: missing or empty name or address returns 2001")
    void createRequiresNameAndAddress(String json) throws Exception {
        expectError(createAsAdmin(json), 400, "2001");
    }

    @Test
    @DisplayName("AC-7: a 150-character name is accepted, while a 151-character name returns 2003")
    void createEnforcesNameLength() throws Exception {
        createAsAdmin(body("a".repeat(150), "Số 2")).andExpect(status().isCreated());

        expectError(createAsAdmin(body("a".repeat(151), "Số 2")), 400, "2003");
    }

    @Test
    @DisplayName("AC-7: a 500-character address is accepted, while a 501-character address returns 2003")
    void createEnforcesAddressLength() throws Exception {
        createAsAdmin(body("Cơ sở A", "a".repeat(500))).andExpect(status().isCreated());

        expectError(createAsAdmin(body("Cơ sở A", "a".repeat(501))), 400, "2003");
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "1234567", "1234567890123456", "++84901234567", "0901 234 567"})
    @DisplayName("AC-7: an invalid phone format returns 2004")
    void createRejectsBadPhone(String phone) throws Exception {
        expectError(createAsAdmin(bodyWithPhone(phone)), 400, "2004");
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345678", "123456789012345", "+84901234567", "19001234"})
    @DisplayName("AC-7: an 8- to 15-digit phone number, optionally prefixed with a plus sign, is accepted unchanged")
    void createAcceptsValidPhone(String phone) throws Exception {
        createAsAdmin(bodyWithPhone(phone))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.phone").value(phone));
    }

    @Test
    @DisplayName("AC-7: duplicate branch names are accepted")
    void createAllowsDuplicateName() throws Exception {
        createAsAdmin(validBody()).andExpect(status().isCreated());

        createAsAdmin(validBody()).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Malformed JSON returns 2003 instead of 2001")
    void createRejectsMalformedJson() throws Exception {
        expectError(createAsAdmin("{\"name\": "), 400, "2003");
    }

    // TOPOLOGY_GET_LIST_BRANCHES

    @Test
    @DisplayName("AC-8: the list includes inactive branches and sorts branch_id numerically in ascending order")
    void listsAllBranchesInAscendingIdOrder() throws Exception {
        long active = givenBranch("Cơ sở Đang Mở", null, true);
        long inactive = givenBranch("Cơ sở Đã Đóng", null, false);

        String response = send(get(BRANCHES), adminToken, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("1000"))
                .andExpect(jsonPath("$.data[?(@.branch_id == '%s')].status".formatted(inactive)).value(false))
                .andReturn().getResponse().getContentAsString();

        List<String> ids = JsonPath.read(response, "$.data[*].branch_id");
        List<Long> numericIds = ids.stream().map(Long::valueOf).toList();
        assertThat(numericIds).contains(active, inactive).isSorted();
    }

    // TOPOLOGY_GET_BRANCH_DETAIL

    @Test
    @DisplayName("AC-4, AC-9: details return all branch fields, with branch_id as a string")
    void returnsBranchDetails() throws Exception {
        long id = givenBranch("Cơ sở Hoàn Kiếm", "0241234567", false);

        send(get(BRANCHES + "/" + id), patientToken, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("1000"))
                .andExpect(jsonPath("$.data.branch_id").value(String.valueOf(id)))
                .andExpect(jsonPath("$.data.name").value("Cơ sở Hoàn Kiếm"))
                .andExpect(jsonPath("$.data.address").value("Số 1 Phố Huế"))
                .andExpect(jsonPath("$.data.phone").value("0241234567"))
                .andExpect(jsonPath("$.data.status").value(false));
    }

    @ParameterizedTest
    @ValueSource(strings = {"9223372036854775807", "abc", "9223372036854775808", "-1", "0"})
    @DisplayName("AC-9: a missing or non-numeric ID returns 404 with code 2101")
    void detailOfUnknownIdIsNotFound(String id) throws Exception {
        expectError(send(get(BRANCHES + "/" + id), adminToken, null), 404, "2101");
    }

    // TOPOLOGY_EDIT_BRANCH

    @Test
    @DisplayName("AC-10: supplying only address leaves the other fields unchanged")
    void editChangesOnlySentFields() throws Exception {
        long id = givenBranch("Cơ sở Cũ", "0241234567", true);

        editAsAdmin(id, "{\"address\": \"  Địa chỉ mới  \"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("1000"))
                .andExpect(jsonPath("$.data.branch_id").value(String.valueOf(id)))
                .andExpect(jsonPath("$.data.name").value("Cơ sở Cũ"))
                .andExpect(jsonPath("$.data.address").value("Địa chỉ mới"))
                .andExpect(jsonPath("$.data.phone").value("0241234567"))
                .andExpect(jsonPath("$.data.status").value(true));
    }

    @Test
    @DisplayName("AC-10: status can be toggled")
    void editTogglesStatus() throws Exception {
        long id = givenBranch();

        editAsAdmin(id, "{\"status\": false}").andExpect(jsonPath("$.data.status").value(false));

        editAsAdmin(id, "{\"status\": true}").andExpect(jsonPath("$.data.status").value(true));
    }

    @Test
    @DisplayName("AC-10: an empty phone is stored as NULL in the database")
    void editClearsPhone() throws Exception {
        long id = givenBranch();

        editAsAdmin(id, "{\"phone\": \"\"}").andExpect(status().isOk());

        assertThat(phoneColumn(id)).isNull();
    }

    @Test
    @DisplayName("An empty {} body returns 200 with the current data")
    void editWithEmptyBodyKeepsEverything() throws Exception {
        long id = givenBranch("Cơ sở Cũ", "0241234567", true);

        editAsAdmin(id, "{}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Cơ sở Cũ"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"name\": \"\"}", "{\"name\": \"   \"}", "{\"address\": \"\"}", "{\"address\": \"   \"}"})
    @DisplayName("AC-10: an empty name or address returns 2003")
    void editRejectsBlankRequiredFields(String json) throws Exception {
        long id = givenBranch();

        expectError(editAsAdmin(id, json), 400, "2003");
    }

    @Test
    @DisplayName("AC-7: updating with a 151-character name returns 2003")
    void editEnforcesNameLength() throws Exception {
        long id = givenBranch();

        expectError(editAsAdmin(id, "{\"name\": \"%s\"}".formatted("a".repeat(151))), 400, "2003");
    }

    @Test
    @DisplayName("AC-7: updating with a 501-character address returns 2003")
    void editEnforcesAddressLength() throws Exception {
        long id = givenBranch();

        expectError(editAsAdmin(id, "{\"address\": \"%s\"}".formatted("a".repeat(501))), 400, "2003");
    }

    @Test
    @DisplayName("AC-7: updating with an invalid phone format returns 2004")
    void editRejectsBadPhone() throws Exception {
        long id = givenBranch();

        expectError(editAsAdmin(id, "{\"phone\": \"abc\"}"), 400, "2004");
    }

    @Test
    @DisplayName("A status with an invalid type returns 2003")
    void editRejectsNonBooleanStatus() throws Exception {
        long id = givenBranch();

        expectError(editAsAdmin(id, "{\"status\": \"maybe\"}"), 400, "2003");
    }

    @ParameterizedTest
    @ValueSource(strings = {"9223372036854775807", "abc"})
    @DisplayName("AC-10: updating a nonexistent ID returns 404 with code 2101")
    void editOfUnknownIdIsNotFound(String id) throws Exception {
        expectError(send(put(BRANCHES + "/" + id), adminToken, "{\"name\": \"Tên mới\"}"), 404, "2101");
    }

    // TOPOLOGY_DELETE_BRANCH

    @Test
    @DisplayName("AC-11: deleting an empty branch returns 200 with code 1000 and no data; it can no longer be retrieved")
    void deletesEmptyBranch() throws Exception {
        long id = givenBranch();

        deleteAsAdmin(id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("1000"))
                .andExpect(jsonPath("$.data").doesNotExist());

        assertThat(branchExists(id)).isFalse();
        expectError(send(get(BRANCHES + "/" + id), adminToken, null), 404, "2101");
    }

    @Test
    @DisplayName("AC-12: a branch with clinic rooms returns 400 with code 2102 and remains")
    void refusesToDeleteBranchWithRooms() throws Exception {
        long id = givenBranch();
        givenRoomIn(id);

        expectError(deleteAsAdmin(id), 400, "2102");

        assertThat(branchExists(id)).isTrue();
    }

    @Test
    @DisplayName("AC-12: a branch with employees returns 400 with code 2102 and remains")
    void refusesToDeleteBranchWithStaff() throws Exception {
        long id = givenBranch();
        givenStaffIn(id);

        expectError(deleteAsAdmin(id), 400, "2102");

        assertThat(branchExists(id)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"9223372036854775807", "abc"})
    @DisplayName("Deleting a nonexistent ID returns 404 with code 2101")
    void deleteOfUnknownIdIsNotFound(String id) throws Exception {
        expectError(send(delete(BRANCHES + "/" + id), adminToken, null), 404, "2101");
    }
}
