package com.example.drmayday.features.lesion;

import com.example.drmayday.features.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Path;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-stack integration tests for APIs 87-91 (LESION_*_LESION_EPISODE) through the real Spring Security
 * filter chain against PostgreSQL, including cross-module lifecycle with MEDICAL_RECORDS & MEDIA APIs.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LesionEpisodeApiIntegrationTest {

    private static final String BASE = "/api/v1/lesion-episodes";
    private static final long PATIENT_ID = 2L;
    private static final long MISSING_ID = 999_999_999L;

    @TempDir
    static Path tempUploadDir;

    @DynamicPropertySource
    static void overrideStorageProps(DynamicPropertyRegistry registry) {
        registry.add("app.storage.upload-dir", () -> tempUploadDir.toString());
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private ObjectMapper objectMapper;

    private String ownerPatientToken;
    private String ownerEnglishPatientToken;
    private String otherPatientToken;
    private String doctorToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        ownerPatientToken = jwtTokenProvider.generateToken(PATIENT_ID, "0912345678", "benh_nhan");
        ownerEnglishPatientToken = jwtTokenProvider.generateToken(PATIENT_ID, "0912345678", "patient");
        otherPatientToken = jwtTokenProvider.generateToken(77L, "0912000077", "benh_nhan");
        doctorToken = jwtTokenProvider.generateToken(3L, "0912345679", "bac_si");
        adminToken = jwtTokenProvider.generateToken(1L, "0900000001", "admin");
    }

    private long givenRecord(long patientId, String type) {
        Long id = jdbcTemplate.queryForObject(
                "INSERT INTO hosobenhan (ma_benh_nhan, loai_benh_an) VALUES (?, ?) RETURNING ma_ho_so",
                Long.class, patientId, type);
        jdbcTemplate.update("INSERT INTO " + ("CAP".equals(type) ? "benhanmaydaycap" : "benhanmaydayman")
                + " (ma_ho_so) VALUES (?)", id);
        return id;
    }

    private long createOtherPatientWithRecord(String type) {
        Long personId = jdbcTemplate.queryForObject(
                "INSERT INTO nguoi (ho_ten, so_dien_thoai) VALUES ('Benh nhan khac', '0988887777') RETURNING ma_nguoi",
                Long.class);
        jdbcTemplate.update("INSERT INTO benhnhan (ma_benh_nhan) VALUES (?)", personId);
        return givenRecord(personId, type);
    }

    @Nested
    @DisplayName("API 87: POST /api/v1/lesion-episodes")
    class CreateLesionEpisode {

        @Test
        @DisplayName("[Happy] Tao dot khong gan ho so (ho_so_man_id=null) va gan ho so MAN voi hoan canh Khac")
        void createsEpisodesWithAndWithoutChronicRecord() throws Exception {
            long manRecordId = givenRecord(PATIENT_ID, "MAN");

            // 1. Without record, without onsetAt -> ho_so_man_id = null, thoi_diem_xuat_hien omitted
            mockMvc.perform(post(BASE)
                            .header("Authorization", "Bearer " + ownerPatientToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"ten_dot_ton_thuong_da": "Dot khoi phat doc lap"}
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.code").value("1001"))
                    .andExpect(jsonPath("$.data.episode_id").isNotEmpty())
                    .andExpect(jsonPath("$.data.patient_id").value(String.valueOf(PATIENT_ID)))
                    .andExpect(jsonPath("$.data.ho_so_man_id").doesNotExist());

            // 2. With MAN record and "Khác" context (tests real PostgreSQL ck_dottonthuongda_hoancanh)
            mockMvc.perform(post(BASE)
                            .header("Authorization", "Bearer " + ownerEnglishPatientToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "ho_so_man_id": "%d",
                                      "ten_dot_ton_thuong_da": "Dot bung phat MAN",
                                      "thoi_diem_xuat_hien": "2026-11-25T10:15:00+07:00",
                                      "hoan_canh_xuat_hien": "Khác",
                                      "hoan_canh_xuat_hien_khac": "Di mua lanh",
                                      "ten_thuoc_dung_gan_thoi_diem_xuat_hien": "Cetirizine",
                                      "thoi_gian_dung_thuoc_truoc_khi_xuat_hien": "2",
                                      "don_vi_thoi_gian": "hour",
                                      "chi_tiet_dot_ton_thuong": "San phu vung canh tay"
                                    }
                                    """.formatted(manRecordId)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.code").value("1001"))
                    .andExpect(jsonPath("$.data.ho_so_man_id").value(String.valueOf(manRecordId)))
                    .andExpect(jsonPath("$.data.hoan_canh_xuat_hien").value("Khac"))
                    .andExpect(jsonPath("$.data.hoan_canh_xuat_hien_khac").value("Di mua lanh"));
        }

        @Test
        @DisplayName("[Unhappy & Security] Ten rong -> 7004; Ho so CAP -> 2003; Ho so nguoi khac -> 3106; Doctor -> 403; Anon -> 401")
        void rejectsInvalidCreateRequests() throws Exception {
            long capRecordId = givenRecord(PATIENT_ID, "CAP");
            long foreignManId = createOtherPatientWithRecord("MAN");

            mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content("{\"ten_dot_ton_thuong_da\":\"A\"}"))
                    .andExpect(status().isUnauthorized());

            mockMvc.perform(post(BASE)
                            .header("Authorization", "Bearer " + doctorToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"ten_dot_ton_thuong_da\":\"A\"}"))
                    .andExpect(status().isForbidden());

            // 2. Empty name -> 7004
            mockMvc.perform(post(BASE)
                            .header("Authorization", "Bearer " + ownerPatientToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"ten_dot_ton_thuong_da\":\"  \"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("7004"));

            // 3. CAP record -> 2003
            mockMvc.perform(post(BASE)
                            .header("Authorization", "Bearer " + ownerPatientToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"ten_dot_ton_thuong_da\":\"Dot CAP\",\"ho_so_man_id\":\"%d\"}".formatted(capRecordId)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("2003"));

            // 4. Another patient's MAN record -> 3106
            mockMvc.perform(post(BASE)
                            .header("Authorization", "Bearer " + ownerPatientToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"ten_dot_ton_thuong_da\":\"Dot khac\",\"ho_so_man_id\":\"%d\"}".formatted(foreignManId)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("3106"));

            // Non-existent record -> 4001
            mockMvc.perform(post(BASE)
                            .header("Authorization", "Bearer " + ownerPatientToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"ten_dot_ton_thuong_da\":\"Dot missing\",\"ho_so_man_id\":\"%d\"}".formatted(MISSING_ID)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("4001"));
        }
    }

    @Nested
    @DisplayName("API 88 & 89: GET /api/v1/lesion-episodes & /{id}")
    class GetLesionEpisodes {

        @Test
        @DisplayName("[Happy & IDOR] Sap xep DESC NULLS LAST, loc ho_so_man_id, bo qua patient_id gia mao, chan xem cheo (3106)")
        void listsOrdersFiltersAndEnforcesOwnership() throws Exception {
            long man1 = givenRecord(PATIENT_ID, "MAN");
            long man2 = givenRecord(PATIENT_ID, "MAN");
            long foreignMan = createOtherPatientWithRecord("MAN");

            // Newer episode attached to man1
            jdbcTemplate.update("""
                    INSERT INTO dottonthuongda (ma_benh_nhan, ma_ho_so_man, ten_dot_ton_thuong_da, thoi_diem_xuat_hien)
                    VALUES (?, ?, 'Dot moi nhat', '2026-11-25T12:00:00+07:00')
                    """, PATIENT_ID, man1);
            // Older episode attached to man2
            jdbcTemplate.update("""
                    INSERT INTO dottonthuongda (ma_benh_nhan, ma_ho_so_man, ten_dot_ton_thuong_da, thoi_diem_xuat_hien)
                    VALUES (?, ?, 'Dot cu hon', '2026-11-20T12:00:00+07:00')
                    """, PATIENT_ID, man2);
            // Episode with NULL thoi_diem_xuat_hien -> must come LAST under NULLS LAST
            jdbcTemplate.update("""
                    INSERT INTO dottonthuongda (ma_benh_nhan, ma_ho_so_man, ten_dot_ton_thuong_da, thoi_diem_xuat_hien)
                    VALUES (?, NULL, 'Dot khong ro gio', NULL)
                    """, PATIENT_ID);
            // Foreign episode
            Long foreignEpisodeId = jdbcTemplate.queryForObject("""
                    INSERT INTO dottonthuongda (ma_benh_nhan, ma_ho_so_man, ten_dot_ton_thuong_da)
                    VALUES ((SELECT ma_benh_nhan FROM hosobenhan WHERE ma_ho_so = ?), ?, 'Dot benh nhan khac')
                    RETURNING ma_dot_ton_thuong_da
                    """, Long.class, foreignMan, foreignMan);

            // Patient list all DESC NULLS LAST, even if spoofed ?patient_id=999 is passed
            mockMvc.perform(get(BASE)
                            .header("Authorization", "Bearer " + ownerPatientToken)
                            .param("patient_id", "999"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("1000"))
                    .andExpect(jsonPath("$.data.length()").value(3))
                    .andExpect(jsonPath("$.data[0].ten_dot_ton_thuong_da").value("Dot moi nhat"))
                    .andExpect(jsonPath("$.data[1].ten_dot_ton_thuong_da").value("Dot cu hon"))
                    .andExpect(jsonPath("$.data[2].ten_dot_ton_thuong_da").value("Dot khong ro gio"));

            // Filter by man1 -> 1 item
            mockMvc.perform(get(BASE)
                            .header("Authorization", "Bearer " + ownerPatientToken)
                            .param("ho_so_man_id", String.valueOf(man1)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(1))
                    .andExpect(jsonPath("$.data[0].ten_dot_ton_thuong_da").value("Dot moi nhat"));

            // Patient tries filtering by another patient's record -> 3106
            mockMvc.perform(get(BASE)
                            .header("Authorization", "Bearer " + ownerPatientToken)
                            .param("ho_so_man_id", String.valueOf(foreignMan)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("3106"));

            // Doctor/Admin can read foreign episode detail
            mockMvc.perform(get(BASE + "/" + foreignEpisodeId)
                            .header("Authorization", "Bearer " + doctorToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.ten_dot_ton_thuong_da").value("Dot benh nhan khac"));
            mockMvc.perform(get(BASE + "/" + foreignEpisodeId)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk());

            // Patient cannot view foreign episode -> 3106
            mockMvc.perform(get(BASE + "/" + foreignEpisodeId)
                            .header("Authorization", "Bearer " + ownerPatientToken))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("3106"));

            // Missing episode -> 7001
            mockMvc.perform(get(BASE + "/" + MISSING_ID)
                            .header("Authorization", "Bearer " + ownerPatientToken))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("7001"));
        }
    }

    @Nested
    @DisplayName("API 90 & 91 + Cross-Module Lifecycle")
    class EditDeleteAndCrossModuleFlow {

        @Test
        @DisplayName("[Happy + Cross-Module] Lien ket ho so MAN + Attachment + LesionEpisode + vitri/anh -> 6007/7009 -> xoa sach")
        void fullCrossModuleLifecycleWithRealPostgresConstraints() throws Exception {
            long manRecordId = givenRecord(PATIENT_ID, "MAN");

            // 1. Upload attachment via API 84
            MockMultipartFile img = new MockMultipartFile("file", "arm.jpg", "image/jpeg", new byte[]{1, 2, 3});
            MvcResult uploadRes = mockMvc.perform(multipart("/api/v1/attachments")
                            .file(img)
                            .header("Authorization", "Bearer " + ownerPatientToken)
                            .param("record_id", String.valueOf(manRecordId)))
                    .andExpect(status().isCreated())
                    .andReturn();
            long attachmentId = objectMapper.readTree(uploadRes.getResponse().getContentAsString())
                    .path("data").path("attachment_id").asLong();

            // 2. Create lesion episode via API 87
            MvcResult createEpRes = mockMvc.perform(post(BASE)
                            .header("Authorization", "Bearer " + ownerPatientToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "ho_so_man_id": "%d",
                                      "ten_dot_ton_thuong_da": "Dot ban dau",
                                      "hoan_canh_xuat_hien": "Khác",
                                      "hoan_canh_xuat_hien_khac": "Stress"
                                    }
                                    """.formatted(manRecordId)))
                    .andExpect(status().isCreated())
                    .andReturn();
            long episodeId = objectMapper.readTree(createEpRes.getResponse().getContentAsString())
                    .path("data").path("episode_id").asLong();

            // 3. Edit episode via API 90 (update only _khac, then switch to regular, verify empty name rejected 7004)
            mockMvc.perform(put(BASE + "/" + episodeId)
                            .header("Authorization", "Bearer " + ownerPatientToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"ten_dot_ton_thuong_da\":\"\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("7004"));

            mockMvc.perform(put(BASE + "/" + episodeId)
                            .header("Authorization", "Bearer " + ownerPatientToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"hoan_canh_xuat_hien_khac\":\"Thieu ngu\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.hoan_canh_xuat_hien").value("Khac"))
                    .andExpect(jsonPath("$.data.hoan_canh_xuat_hien_khac").value("Thieu ngu"));

            mockMvc.perform(put(BASE + "/" + episodeId)
                            .header("Authorization", "Bearer " + ownerPatientToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"hoan_canh_xuat_hien\":\"Sau tap luyen\",\"chi_tiet_dot_ton_thuong\":\"Da cap nhat\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.hoan_canh_xuat_hien").value("Sau tap luyen"))
                    .andExpect(jsonPath("$.data.hoan_canh_xuat_hien_khac").doesNotExist())
                    .andExpect(jsonPath("$.data.chi_tiet_dot_ton_thuong").value("Da cap nhat"));

            // Other patient edit -> 3106; Doctor edit -> 403
            mockMvc.perform(put(BASE + "/" + episodeId)
                            .header("Authorization", "Bearer " + otherPatientToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"chi_tiet_dot_ton_thuong\":\"hack\"}"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("3106"));
            mockMvc.perform(put(BASE + "/" + episodeId)
                            .header("Authorization", "Bearer " + doctorToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"chi_tiet_dot_ton_thuong\":\"doc\"}"))
                    .andExpect(status().isForbidden());

            // 4. Add child location & photo referencing episode & attachment
            Long locationId = jdbcTemplate.queryForObject(
                    "INSERT INTO vitritonthuong (ma_dot_ton_thuong_da, vi_tri_co_the) VALUES (?, 'Canh tay') RETURNING ma_vi_tri_ton_thuong",
                    Long.class, episodeId);
            Long photoId = jdbcTemplate.queryForObject(
                    "INSERT INTO anhchuptonthuong (ma_tep_dinh_kem, ma_dot_ton_thuong_da, ma_vi_tri_ton_thuong, thoi_diem_chup) VALUES (?, ?, ?, CURRENT_TIMESTAMP) RETURNING ma_anh",
                    Long.class, attachmentId, episodeId, locationId);

            // Attempt delete attachment -> 6007; attempt delete episode -> 7009
            mockMvc.perform(delete("/api/v1/attachments/" + attachmentId)
                            .header("Authorization", "Bearer " + ownerPatientToken))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("6007"));

            mockMvc.perform(delete(BASE + "/" + episodeId)
                            .header("Authorization", "Bearer " + ownerPatientToken))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("7009"));

            // Remove child photo, location still exists -> still 7009
            jdbcTemplate.update("DELETE FROM anhchuptonthuong WHERE ma_anh = ?", photoId);
            mockMvc.perform(delete(BASE + "/" + episodeId)
                            .header("Authorization", "Bearer " + ownerPatientToken))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("7009"));

            // Remove child location -> now both episode and attachment can be deleted cleanly (1000)
            jdbcTemplate.update("DELETE FROM vitritonthuong WHERE ma_vi_tri_ton_thuong = ?", locationId);

            mockMvc.perform(delete(BASE + "/" + episodeId)
                            .header("Authorization", "Bearer " + otherPatientToken))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("3106"));

            mockMvc.perform(delete(BASE + "/" + episodeId)
                            .header("Authorization", "Bearer " + ownerPatientToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("1000"));

            mockMvc.perform(delete("/api/v1/attachments/" + attachmentId)
                            .header("Authorization", "Bearer " + ownerPatientToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("1000"));
        }
    }
}
