package com.example.drmayday.features.medicalrecord;

import com.example.drmayday.features.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Covers APIs 30-38 ("Bệnh nhân & Hồ sơ bệnh án") through the real filter chain
 * against PostgreSQL. Scenarios come from the "Kịch bản kiểm thử" column of
 * docs/API Design - IT4788 (2).xlsx.
 *
 * Each test runs in a rolled-back transaction, so it leaves no rows behind.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MedicalRecordApiIntegrationTest {

    private static final String RECORDS = "/api/v1/medical-records";
    private static final String ACUTE_DIAGNOSIS = "/api/v1/acute-diagnosis";
    private static final String CHRONIC_DIAGNOSIS = "/api/v1/chronic-diagnosis";

    /** Patient seeded by DatabaseSeeder (nguoi/benhnhan ma_benh_nhan = 2). */
    private static final long PATIENT_ID = 2L;
    /** Employee seeded by DatabaseSeeder, used as the doctor on an exam session. */
    private static final long DOCTOR_EMPLOYEE_ID = 1L;
    private static final long MISSING_ID = 999_999L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String adminToken;
    private String doctorToken;
    private String ownerPatientToken;
    private String otherPatientToken;

    @BeforeEach
    void setUp() {
        adminToken = jwtTokenProvider.generateToken(1L, "0900000001", "admin");
        doctorToken = jwtTokenProvider.generateToken(3L, "0912345679", "bac_si");
        ownerPatientToken = jwtTokenProvider.generateToken(PATIENT_ID, "0912345678", "benh_nhan");
        otherPatientToken = jwtTokenProvider.generateToken(77L, "0912000077", "benh_nhan");
    }

    // ── Helpers ──

    private ResultActions send(MockHttpServletRequestBuilder builder, String token, String json) throws Exception {
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        if (json != null) {
            builder.contentType(MediaType.APPLICATION_JSON).content(json);
        }
        return mockMvc.perform(builder);
    }

    /** Inserts a medical record plus its mandatory subtype row, bypassing the API. */
    private long givenRecord(String type, String status) {
        Long id = jdbcTemplate.queryForObject(
                "INSERT INTO hosobenhan (ma_benh_nhan, loai_benh_an, trang_thai) VALUES (?, ?, ?) "
                        + "RETURNING ma_ho_so",
                Long.class, PATIENT_ID, type, status);
        String subtypeTable = "CAP".equals(type) ? "benhanmaydaycap" : "benhanmaydayman";
        jdbcTemplate.update("INSERT INTO " + subtypeTable + " (ma_ho_so) VALUES (?)", id);
        return id;
    }

    private long givenRecord(String type) {
        return givenRecord(type, "draft");
    }

    /** Adds an exam session so the record counts as having child data. */
    private void givenExamSession(long recordId, String type, LocalDate visitDate, int visitNumber) {
        jdbcTemplate.update(
                "INSERT INTO phienkham (ma_ho_so, ma_bac_si, loai_benh_an, ngay_kham, lan_kham_thu) "
                        + "VALUES (?, ?, ?, ?, ?)",
                recordId, DOCTOR_EMPLOYEE_ID, type, visitDate, visitNumber);
    }

    private static void expect(ResultActions result, int httpStatus, String code) throws Exception {
        result.andExpect(status().is(httpStatus)).andExpect(jsonPath("$.code").value(code));
    }

    private boolean recordExists(long id) {
        Integer rows = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM hosobenhan WHERE ma_ho_so = ?", Integer.class, id);
        return rows != null && rows == 1;
    }

    private boolean subtypeExists(String table, long recordId) {
        Integer rows = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM " + table + " WHERE ma_ho_so = ?", Integer.class, recordId);
        return rows != null && rows == 1;
    }

    // ── API 30: create_medical_record ──

    @Nested
    @DisplayName("API 30 POST /medical-records")
    class CreateMedicalRecord {

        @Test
        @DisplayName("1. Valid MAN record returns 1001 and creates the man subtype")
        void createsChronicRecordWithSubtype() throws Exception {
            ResultActions result = send(post(RECORDS), doctorToken, """
                    {"patient_id": %d, "loai_benh_an": "MAN", "trang_thai": "draft"}
                    """.formatted(PATIENT_ID));

            expect(result, 201, "1001");
            result.andExpect(jsonPath("$.data.loai_benh_an").value("MAN"))
                    .andExpect(jsonPath("$.data.patient_id").value((int) PATIENT_ID))
                    .andExpect(jsonPath("$.data.record_id").exists());
        }

        @Test
        @DisplayName("2. loai_benh_an outside {CAP,MAN} returns 2003")
        void rejectsUnknownRecordType() throws Exception {
            expect(send(post(RECORDS), doctorToken, """
                    {"patient_id": %d, "loai_benh_an": "X"}
                    """.formatted(PATIENT_ID)), 400, "2003");
        }

        @Test
        @DisplayName("3. Unknown patient_id returns 3007")
        void rejectsMissingPatient() throws Exception {
            expect(send(post(RECORDS), doctorToken, """
                    {"patient_id": %d, "loai_benh_an": "CAP"}
                    """.formatted(MISSING_ID)), 404, "3007");
        }

        @Test
        @DisplayName("4. tien_su_gia_dinh=Co without ghi_ro returns 2003")
        void rejectsFamilyHistoryWithoutDetail() throws Exception {
            expect(send(post(RECORDS), doctorToken, """
                    {"patient_id": %d, "loai_benh_an": "CAP", "tien_su_gia_dinh": "Co"}
                    """.formatted(PATIENT_ID)), 400, "2003");
        }

        @Test
        @DisplayName("5. Second active record for the same patient returns 4006")
        void rejectsSecondActiveRecord() throws Exception {
            givenRecord("MAN", "in_progress");

            expect(send(post(RECORDS), doctorToken, """
                    {"patient_id": %d, "loai_benh_an": "CAP"}
                    """.formatted(PATIENT_ID)), 409, "4006");
        }

        @Test
        @DisplayName("6. A patient may not create records")
        void forbidsPatientRole() throws Exception {
            send(post(RECORDS), ownerPatientToken, """
                    {"patient_id": %d, "loai_benh_an": "CAP"}
                    """.formatted(PATIENT_ID)).andExpect(status().isForbidden());
        }
    }

    // ── API 31: list_medical_records ──

    @Nested
    @DisplayName("API 31 GET /medical-records")
    class ListMedicalRecords {

        @Test
        @DisplayName("1. No filter returns a page of every record")
        void listsAllRecordsPaged() throws Exception {
            givenRecord("MAN");

            ResultActions result = send(get(RECORDS), adminToken, null);

            expect(result, 200, "1000");
            result.andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content.length()").value(1));
        }

        @Test
        @DisplayName("2. loai_benh_an=MAN returns only chronic records")
        void filtersByRecordType() throws Exception {
            givenRecord("MAN");

            ResultActions result = send(get(RECORDS + "?loai_benh_an=MAN"), adminToken, null);

            expect(result, 200, "1000");
            result.andExpect(jsonPath("$.data.content.length()").value(1))
                    .andExpect(jsonPath("$.data.content[0].loai_benh_an").value("MAN"));
        }

        @Test
        @DisplayName("3. patient_id filter returns that patient's records")
        void filtersByPatient() throws Exception {
            givenRecord("MAN");

            ResultActions result = send(get(RECORDS + "?patient_id=" + PATIENT_ID), adminToken, null);

            expect(result, 200, "1000");
            result.andExpect(jsonPath("$.data.content[0].patient_id").value((int) PATIENT_ID));
        }

        @Test
        @DisplayName("4. An unknown trang_thai returns 2003")
        void rejectsUnknownStatusFilter() throws Exception {
            expect(send(get(RECORDS + "?trang_thai=active"), adminToken, null), 400, "2003");
        }
    }

    // ── API 32: get_medical_record ──

    @Nested
    @DisplayName("API 32 GET /medical-records/{id}")
    class GetMedicalRecord {

        @Test
        @DisplayName("1. Chronic record comes back with its chronic diagnosis block")
        void returnsChronicRecordDetail() throws Exception {
            long id = givenRecord("MAN");

            ResultActions result = send(get(RECORDS + "/" + id), doctorToken, null);

            expect(result, 200, "1000");
            result.andExpect(jsonPath("$.data.record.loai_benh_an").value("MAN"))
                    .andExpect(jsonPath("$.data.diagnosis.loai_benh_an").value("MAN"));
        }

        @Test
        @DisplayName("2. Unknown id returns 4001")
        void rejectsMissingRecord() throws Exception {
            expect(send(get(RECORDS + "/" + MISSING_ID), doctorToken, null), 404, "4001");
        }

        @Test
        @DisplayName("3. A patient reading someone else's record returns 3106")
        void forbidsForeignPatient() throws Exception {
            long id = givenRecord("MAN");

            expect(send(get(RECORDS + "/" + id), otherPatientToken, null), 403, "3106");
        }

        @Test
        @DisplayName("4. The owning patient may read their own record")
        void allowsOwningPatient() throws Exception {
            long id = givenRecord("MAN");

            expect(send(get(RECORDS + "/" + id), ownerPatientToken, null), 200, "1000");
        }
    }

    // ── API 33: update_medical_record ──

    @Nested
    @DisplayName("API 33 PUT /medical-records/{id}")
    class UpdateMedicalRecord {

        @Test
        @DisplayName("1. Moving trang_thai draft -> in_progress returns 1000")
        void updatesStatus() throws Exception {
            long id = givenRecord("MAN", "draft");

            ResultActions result = send(put(RECORDS + "/" + id), doctorToken, """
                    {"trang_thai": "in_progress"}
                    """);

            expect(result, 200, "1000");
            result.andExpect(jsonPath("$.data.trang_thai").value("in_progress"));
        }

        @Test
        @DisplayName("2. Unknown id returns 4001")
        void rejectsMissingRecord() throws Exception {
            expect(send(put(RECORDS + "/" + MISSING_ID), doctorToken, """
                    {"trang_thai": "in_progress"}
                    """), 404, "4001");
        }

        @Test
        @DisplayName("3. tien_su_gia_dinh=Co without ghi_ro returns 2003")
        void rejectsFamilyHistoryWithoutDetail() throws Exception {
            long id = givenRecord("MAN");

            expect(send(put(RECORDS + "/" + id), doctorToken, """
                    {"tien_su_gia_dinh": "Co"}
                    """), 400, "2003");
        }

        @Test
        @DisplayName("4. An unknown trang_thai returns 2003")
        void rejectsUnknownStatus() throws Exception {
            long id = givenRecord("MAN");

            expect(send(put(RECORDS + "/" + id), doctorToken, """
                    {"trang_thai": "active"}
                    """), 400, "2003");
        }
    }

    // ── API 34: delete_medical_record ──

    @Nested
    @DisplayName("API 34 DELETE /medical-records/{id}")
    class DeleteMedicalRecord {

        @Test
        @DisplayName("1. Deleting a record with no children returns 1000 and removes both rows")
        void deletesEmptyRecord() throws Exception {
            long id = givenRecord("MAN");

            expect(send(delete(RECORDS + "/" + id), doctorToken, null), 200, "1000");

            org.assertj.core.api.Assertions.assertThat(recordExists(id)).isFalse();
            org.assertj.core.api.Assertions.assertThat(subtypeExists("benhanmaydayman", id)).isFalse();
        }

        @Test
        @DisplayName("2. Unknown id returns 4001")
        void rejectsMissingRecord() throws Exception {
            expect(send(delete(RECORDS + "/" + MISSING_ID), doctorToken, null), 404, "4001");
        }

        @Test
        @DisplayName("3. A record that already has an exam session returns 4007")
        void rejectsRecordWithChildData() throws Exception {
            long id = givenRecord("MAN");
            givenExamSession(id, "MAN", LocalDate.now(), 1);

            expect(send(delete(RECORDS + "/" + id), doctorToken, null), 409, "4007");
        }
    }

    // ── API 35: get_patient_record_history ──

    @Nested
    @DisplayName("API 35 GET /medical-records/by-patient/{patient_id}/history")
    class PatientRecordHistory {

        private String historyUrl(long patientId) {
            return RECORDS + "/by-patient/" + patientId + "/history";
        }

        @Test
        @DisplayName("1. History lists the patient's exam sessions, newest first")
        void listsExamSessionsNewestFirst() throws Exception {
            long id = givenRecord("MAN");
            givenExamSession(id, "MAN", LocalDate.of(2026, 1, 10), 1);
            givenExamSession(id, "MAN", LocalDate.of(2026, 3, 20), 2);

            ResultActions result = send(get(historyUrl(PATIENT_ID)), doctorToken, null);

            expect(result, 200, "1000");
            result.andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].ngay_kham").value("2026-03-20"))
                    .andExpect(jsonPath("$.data[1].ngay_kham").value("2026-01-10"));
        }

        @Test
        @DisplayName("2. Unknown patient_id returns 3007")
        void rejectsMissingPatient() throws Exception {
            expect(send(get(historyUrl(MISSING_ID)), doctorToken, null), 404, "3007");
        }

        @Test
        @DisplayName("3. A patient reading another patient's history returns 3106")
        void forbidsForeignPatient() throws Exception {
            expect(send(get(historyUrl(PATIENT_ID)), otherPatientToken, null), 403, "3106");
        }
    }

    // ── API 36: upsert_acute_diagnosis ──

    @Nested
    @DisplayName("API 36 PUT /acute-diagnosis")
    class UpsertAcuteDiagnosis {

        @Test
        @DisplayName("1. Writing an acute diagnosis returns 1000")
        void writesAcuteDiagnosis() throws Exception {
            long id = givenRecord("CAP");

            ResultActions result = send(put(ACUTE_DIAGNOSIS), doctorToken, """
                    {"record_id": %d, "cd_phan_ve": true, "nhap_vien": false,
                     "tuoi_lan_dau_bi_benh": 30, "so_lan_may_day_cap_truoc": 2}
                    """.formatted(id));

            expect(result, 200, "1000");
            result.andExpect(jsonPath("$.data.loai_benh_an").value("CAP"))
                    .andExpect(jsonPath("$.data.cd_phan_ve").value(true))
                    .andExpect(jsonPath("$.data.tuoi_lan_dau_bi_benh").value(30));
        }

        @Test
        @DisplayName("2. A chronic record returns 4008")
        void rejectsChronicRecord() throws Exception {
            long id = givenRecord("MAN");

            expect(send(put(ACUTE_DIAGNOSIS), doctorToken, """
                    {"record_id": %d, "cd_phan_ve": true}
                    """.formatted(id)), 400, "4008");
        }

        @Test
        @DisplayName("3. A negative tuoi_lan_dau_bi_benh returns 2003")
        void rejectsNegativeAge() throws Exception {
            long id = givenRecord("CAP");

            expect(send(put(ACUTE_DIAGNOSIS), doctorToken, """
                    {"record_id": %d, "tuoi_lan_dau_bi_benh": -1}
                    """.formatted(id)), 400, "2003");
        }

        @Test
        @DisplayName("4. Unknown record_id returns 4001")
        void rejectsMissingRecord() throws Exception {
            expect(send(put(ACUTE_DIAGNOSIS), doctorToken, """
                    {"record_id": %d, "cd_phan_ve": true}
                    """.formatted(MISSING_ID)), 404, "4001");
        }
    }

    // ── API 37: upsert_chronic_diagnosis ──

    @Nested
    @DisplayName("API 37 PUT /chronic-diagnosis")
    class UpsertChronicDiagnosis {

        @Test
        @DisplayName("1. chan_doan_chinh=CSU with phan_nhom_csu=CSU Type I returns 1000")
        void writesChronicDiagnosis() throws Exception {
            long id = givenRecord("MAN");

            ResultActions result = send(put(CHRONIC_DIAGNOSIS), doctorToken, """
                    {"record_id": %d, "chan_doan_chinh": "CSU", "phan_nhom_csu": "CSU Type I",
                     "chung_da_ve_noi": true}
                    """.formatted(id));

            expect(result, 200, "1000");
            result.andExpect(jsonPath("$.data.loai_benh_an").value("MAN"))
                    .andExpect(jsonPath("$.data.chan_doan_chinh").value("CSU"))
                    .andExpect(jsonPath("$.data.phan_nhom_csu").value("CSU Type I"));
        }

        @Test
        @DisplayName("2. An acute record returns 4008")
        void rejectsAcuteRecord() throws Exception {
            long id = givenRecord("CAP");

            expect(send(put(CHRONIC_DIAGNOSIS), doctorToken, """
                    {"record_id": %d, "chan_doan_chinh": "CSU"}
                    """.formatted(id)), 400, "4008");
        }

        @Test
        @DisplayName("3. An unknown chan_doan_chinh returns 2003")
        void rejectsUnknownPrimaryDiagnosis() throws Exception {
            long id = givenRecord("MAN");

            expect(send(put(CHRONIC_DIAGNOSIS), doctorToken, """
                    {"record_id": %d, "chan_doan_chinh": "abc"}
                    """.formatted(id)), 400, "2003");
        }

        @Test
        @DisplayName("4. may_day_cam_ung_khac=true without ghi_ro returns 2003")
        void rejectsInducibleFlagWithoutDetail() throws Exception {
            long id = givenRecord("MAN");

            expect(send(put(CHRONIC_DIAGNOSIS), doctorToken, """
                    {"record_id": %d, "may_day_cam_ung_khac": true}
                    """.formatted(id)), 400, "2003");
        }
    }

    // ── API 38: upsert_self_history ──

    @Nested
    @DisplayName("API 38 PUT /medical-records/{record_id}/self-history")
    class UpsertSelfHistory {

        private String selfHistoryUrl(long recordId) {
            return RECORDS + "/" + recordId + "/self-history";
        }

        @Test
        @DisplayName("1. First write creates the row and returns 1001")
        void createsSelfHistory() throws Exception {
            long id = givenRecord("MAN");

            ResultActions result = send(put(selfHistoryUrl(id)), doctorToken, """
                    {"tien_su_di_ung": "CO", "di_ung_thuoc": true,
                     "di_ung_thuoc_ghi_ro": "Kháng sinh Penicillin"}
                    """);

            expect(result, 201, "1001");
            result.andExpect(jsonPath("$.data.tien_su_di_ung").value("CO"))
                    .andExpect(jsonPath("$.data.di_ung_thuoc").value(true))
                    .andExpect(jsonPath("$.data.di_ung_thuoc_ghi_ro").value("Kháng sinh Penicillin"));
        }

        @Test
        @DisplayName("2. A second write updates the same row and returns 1000")
        void updatesExistingSelfHistory() throws Exception {
            long id = givenRecord("MAN");
            send(put(selfHistoryUrl(id)), doctorToken, """
                    {"tien_su_di_ung": "CO", "di_ung_thuoc": true,
                     "di_ung_thuoc_ghi_ro": "Kháng sinh Penicillin"}
                    """).andExpect(status().isCreated());

            ResultActions result = send(put(selfHistoryUrl(id)), doctorToken, """
                    {"tien_su_tuyen_giap": true, "tien_su_tuyen_giap_ghi_ro": "Basedow"}
                    """);

            expect(result, 200, "1000");
            result.andExpect(jsonPath("$.data.tien_su_tuyen_giap").value(true))
                    .andExpect(jsonPath("$.data.tien_su_tuyen_giap_ghi_ro").value("Basedow"));
        }

        @Test
        @DisplayName("3. Unknown record_id returns 4001")
        void rejectsMissingRecord() throws Exception {
            expect(send(put(selfHistoryUrl(MISSING_ID)), doctorToken, """
                    {"tien_su_di_ung": "CO"}
                    """), 404, "4001");
        }

        @Test
        @DisplayName("6. An unknown tien_su_co_dia tri-state returns 2003")
        void rejectsUnknownTriState() throws Exception {
            long id = givenRecord("MAN");

            expect(send(put(selfHistoryUrl(id)), doctorToken, """
                    {"tien_su_co_dia": "MAYBE"}
                    """), 400, "2003");
        }

        @Test
        @DisplayName("4. A true flag without its ghi_ro returns 2003")
        void rejectsFlagWithoutDetail() throws Exception {
            long id = givenRecord("MAN");

            expect(send(put(selfHistoryUrl(id)), doctorToken, """
                    {"tien_su_phan_ve": true}
                    """), 400, "2003");
        }

        @Test
        @DisplayName("5. A ghi_ro supplied while the flag is false returns 2003")
        void rejectsDetailWithoutFlag() throws Exception {
            long id = givenRecord("MAN");

            expect(send(put(selfHistoryUrl(id)), doctorToken, """
                    {"tien_su_tu_mien": false, "tien_su_tu_mien_ghi_ro": "Lupus"}
                    """), 400, "2003");
        }
    }
}
