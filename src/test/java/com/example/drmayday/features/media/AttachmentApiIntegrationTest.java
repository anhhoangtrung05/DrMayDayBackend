package com.example.drmayday.features.media;

import com.example.drmayday.features.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-stack integration tests for APIs 84-86 (MEDIA_*) through the real Spring Security filter chain
 * against PostgreSQL and real multipart/file storage.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AttachmentApiIntegrationTest {

    private static final String BASE = "/api/v1/attachments";
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
    private String nurseToken;

    @BeforeEach
    void setUp() {
        ownerPatientToken = jwtTokenProvider.generateToken(PATIENT_ID, "0912345678", "benh_nhan");
        ownerEnglishPatientToken = jwtTokenProvider.generateToken(PATIENT_ID, "0912345678", "patient");
        otherPatientToken = jwtTokenProvider.generateToken(77L, "0912000077", "benh_nhan");
        doctorToken = jwtTokenProvider.generateToken(3L, "0912345679", "bac_si");
        adminToken = jwtTokenProvider.generateToken(1L, "0900000001", "admin");
        nurseToken = jwtTokenProvider.generateToken(4L, "0900000004", "dieu_duong");
    }

    private long givenRecord(long patientId, String type) {
        Long id = jdbcTemplate.queryForObject(
                "INSERT INTO hosobenhan (ma_benh_nhan, loai_benh_an) VALUES (?, ?) RETURNING ma_ho_so",
                Long.class, patientId, type);
        jdbcTemplate.update("INSERT INTO " + ("CAP".equals(type) ? "benhanmaydaycap" : "benhanmaydayman")
                + " (ma_ho_so) VALUES (?)", id);
        return id;
    }

    @Nested
    @DisplayName("API 84: POST /api/v1/attachments")
    class CreateAttachment {

        @Test
        @DisplayName("[Happy] Benh nhan tai anh 2MB (<5MB) voi token benh_nhan -> 201 Created (1001) & luu tep vat ly")
        void patientUploadsValid2MbImage() throws Exception {
            long recordId = givenRecord(PATIENT_ID, "MAN");
            MockMultipartFile file = new MockMultipartFile(
                    "file", "hives_2mb.png", "image/png", new byte[2 * 1024 * 1024]);

            MvcResult res = mockMvc.perform(multipart(BASE)
                            .file(file)
                            .header("Authorization", "Bearer " + ownerPatientToken)
                            .param("record_id", String.valueOf(recordId))
                            .param("phan_loai_muc_dich", "Anh ton thuong")
                            .param("mo_ta_tep", "San phu canh tay"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.code").value("1001"))
                    .andExpect(jsonPath("$.data.attachment_id").isNotEmpty())
                    .andExpect(jsonPath("$.data.record_id").value(String.valueOf(recordId)))
                    .andExpect(jsonPath("$.data.ten_tep_goc").value("hives_2mb.png"))
                    .andExpect(jsonPath("$.data.dinh_dang_tep").value("png"))
                    .andExpect(jsonPath("$.data.loai_phuong_tien").value("IMAGE"))
                    .andExpect(jsonPath("$.data.dung_luong_byte").value(2 * 1024 * 1024))
                    .andReturn();

            JsonNode root = objectMapper.readTree(res.getResponse().getContentAsString());
            long attachmentId = root.path("data").path("attachment_id").asLong();
            String storagePath = jdbcTemplate.queryForObject(
                    "SELECT duong_dan_luu_tru FROM tepdinhkem WHERE ma_tep_dinh_kem = ?",
                    String.class, attachmentId);
            assertNotNull(storagePath);
            assertTrue(Files.exists(tempUploadDir.resolve(Path.of(storagePath).getFileName())));
        }

        @Test
        @DisplayName("[Happy/Alt] Bac si (bac_si token khong co trong taikhoan) tai tai lieu len ho so -> 201 (1001)")
        void doctorUploadsDocumentWithSyntheticToken() throws Exception {
            long recordId = givenRecord(PATIENT_ID, "CAP");
            MockMultipartFile doc = new MockMultipartFile(
                    "file", "report.pdf", "application/pdf", new byte[]{1, 2, 3, 4});

            mockMvc.perform(multipart(BASE)
                            .file(doc)
                            .header("Authorization", "Bearer " + doctorToken)
                            .param("record_id", String.valueOf(recordId)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.code").value("1001"))
                    .andExpect(jsonPath("$.data.dinh_dang_tep").value("pdf"))
                    .andExpect(jsonPath("$.data.loai_phuong_tien").value("DOCUMENT"));
        }

        @Test
        @DisplayName("[Security] Chua xac thuc -> 401; Dieu duong -> 403")
        void securityGuards() throws Exception {
            long recordId = givenRecord(PATIENT_ID, "CAP");
            MockMultipartFile file = new MockMultipartFile("file", "a.jpg", "image/jpeg", new byte[]{1});

            mockMvc.perform(multipart(BASE).file(file).param("record_id", String.valueOf(recordId)))
                    .andExpect(status().isUnauthorized());

            mockMvc.perform(multipart(BASE).file(file)
                            .header("Authorization", "Bearer " + nurseToken)
                            .param("record_id", String.valueOf(recordId)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("[Unhappy] Thieu file -> 6001; Thieu record_id -> 2001; >5MB -> 6003; Ho so khong ton tai -> 4001; Cua nguoi khac -> 3106")
        void rejectsInvalidUploads() throws Exception {
            long recordId = givenRecord(PATIENT_ID, "MAN");
            MockMultipartFile smallFile = new MockMultipartFile("file", "a.jpg", "image/jpeg", new byte[]{1, 2});

            // 2. Missing file -> 6001
            mockMvc.perform(multipart(BASE)
                            .header("Authorization", "Bearer " + ownerEnglishPatientToken)
                            .param("record_id", String.valueOf(recordId)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("6001"));

            // 3. Missing record_id -> 2001
            mockMvc.perform(multipart(BASE)
                            .file(smallFile)
                            .header("Authorization", "Bearer " + ownerPatientToken))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("2001"));

            // 4. Other patient -> 3106
            mockMvc.perform(multipart(BASE)
                            .file(smallFile)
                            .header("Authorization", "Bearer " + otherPatientToken)
                            .param("record_id", String.valueOf(recordId)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("3106"));

            // 5. Missing medical record -> 4001
            mockMvc.perform(multipart(BASE)
                            .file(smallFile)
                            .header("Authorization", "Bearer " + ownerPatientToken)
                            .param("record_id", String.valueOf(MISSING_ID)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("4001"));

            // 6. Over 5MB -> 6003
            MockMultipartFile largeFile = new MockMultipartFile(
                    "file", "too_big.jpg", "image/jpeg", new byte[5 * 1024 * 1024 + 100]);
            mockMvc.perform(multipart(BASE)
                            .file(largeFile)
                            .header("Authorization", "Bearer " + ownerPatientToken)
                            .param("record_id", String.valueOf(recordId)))
                    .andExpect(status().isContentTooLarge())
                    .andExpect(jsonPath("$.code").value("6003"));
        }
    }

    @Nested
    @DisplayName("API 85: GET /api/v1/attachments")
    class GetAttachments {

        @Test
        @DisplayName("[Happy] Lay danh sach tep theo thu tu moi nhat truoc (1000) va chan IDOR (3106)")
        void listsAttachmentsAndBlocksCrossPatientAccess() throws Exception {
            long recordId = givenRecord(PATIENT_ID, "MAN");
            MockMultipartFile f1 = new MockMultipartFile("file", "first.jpg", "image/jpeg", new byte[]{1});
            MockMultipartFile f2 = new MockMultipartFile("file", "second.jpg", "image/jpeg", new byte[]{2});

            mockMvc.perform(multipart(BASE).file(f1)
                            .header("Authorization", "Bearer " + ownerPatientToken)
                            .param("record_id", String.valueOf(recordId)))
                    .andExpect(status().isCreated());
            mockMvc.perform(multipart(BASE).file(f2)
                            .header("Authorization", "Bearer " + ownerPatientToken)
                            .param("record_id", String.valueOf(recordId)))
                    .andExpect(status().isCreated());

            // Owner patient sees DESC list
            mockMvc.perform(get(BASE)
                            .header("Authorization", "Bearer " + ownerPatientToken)
                            .param("record_id", String.valueOf(recordId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("1000"))
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].ten_tep_goc").value("second.jpg"))
                    .andExpect(jsonPath("$.data[1].ten_tep_goc").value("first.jpg"));

            // Admin allowed
            mockMvc.perform(get(BASE)
                            .header("Authorization", "Bearer " + adminToken)
                            .param("record_id", String.valueOf(recordId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(2));

            // Missing record_id -> 2001
            mockMvc.perform(get(BASE).header("Authorization", "Bearer " + ownerPatientToken))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("2001"));

            // Other patient -> 3106
            mockMvc.perform(get(BASE)
                            .header("Authorization", "Bearer " + otherPatientToken)
                            .param("record_id", String.valueOf(recordId)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("3106"));

            // Non-existent record -> 4001
            mockMvc.perform(get(BASE)
                            .header("Authorization", "Bearer " + ownerPatientToken)
                            .param("record_id", String.valueOf(MISSING_ID)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("4001"));
        }
    }

    @Nested
    @DisplayName("API 86: DELETE /api/v1/attachments/{id}")
    class DeleteAttachment {

        @Test
        @DisplayName("[Happy + Unhappy] Xoa tep thanh cong (1000) va bi chan khi anhchuptonthuong tham chieu (6007)")
        void deletesUnusedAttachmentAndBlocksReferencedAttachment() throws Exception {
            long recordId = givenRecord(PATIENT_ID, "MAN");
            MockMultipartFile f1 = new MockMultipartFile("file", "unused.png", "image/png", new byte[]{1, 2});
            MockMultipartFile f2 = new MockMultipartFile("file", "linked.png", "image/png", new byte[]{3, 4});

            MvcResult r1 = mockMvc.perform(multipart(BASE).file(f1)
                            .header("Authorization", "Bearer " + ownerPatientToken)
                            .param("record_id", String.valueOf(recordId)))
                    .andReturn();
            long unusedId = objectMapper.readTree(r1.getResponse().getContentAsString())
                    .path("data").path("attachment_id").asLong();
            String unusedPath = jdbcTemplate.queryForObject(
                    "SELECT duong_dan_luu_tru FROM tepdinhkem WHERE ma_tep_dinh_kem = ?", String.class, unusedId);

            MvcResult r2 = mockMvc.perform(multipart(BASE).file(f2)
                            .header("Authorization", "Bearer " + ownerPatientToken)
                            .param("record_id", String.valueOf(recordId)))
                    .andReturn();
            long linkedId = objectMapper.readTree(r2.getResponse().getContentAsString())
                    .path("data").path("attachment_id").asLong();
            String linkedPath = jdbcTemplate.queryForObject(
                    "SELECT duong_dan_luu_tru FROM tepdinhkem WHERE ma_tep_dinh_kem = ?", String.class, linkedId);

            // Attach linkedId to a skin lesion episode + lesion photo
            Long episodeId = jdbcTemplate.queryForObject(
                    "INSERT INTO dottonthuongda (ma_benh_nhan, ma_ho_so_man, ten_dot_ton_thuong_da) "
                            + "VALUES (?, ?, 'Dot test') RETURNING ma_dot_ton_thuong_da",
                    Long.class, PATIENT_ID, recordId);
            jdbcTemplate.update(
                    "INSERT INTO anhchuptonthuong (ma_tep_dinh_kem, ma_dot_ton_thuong_da, thoi_diem_chup) "
                            + "VALUES (?, ?, CURRENT_TIMESTAMP)",
                    linkedId, episodeId);

            // Other patient -> 3106
            mockMvc.perform(delete(BASE + "/" + unusedId)
                            .header("Authorization", "Bearer " + otherPatientToken))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("3106"));

            // Non-existent -> 6005
            mockMvc.perform(delete(BASE + "/" + MISSING_ID)
                            .header("Authorization", "Bearer " + ownerPatientToken))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("6005"));

            // Referenced by anhchuptonthuong -> 6007 & keeps DB row + physical file
            mockMvc.perform(delete(BASE + "/" + linkedId)
                            .header("Authorization", "Bearer " + ownerPatientToken))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("6007"));
            assertTrue(Files.exists(tempUploadDir.resolve(Path.of(linkedPath).getFileName())));

            // Unused -> 1000 & removes DB row + physical file
            mockMvc.perform(delete(BASE + "/" + unusedId)
                            .header("Authorization", "Bearer " + ownerPatientToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("1000"));
            assertFalse(Files.exists(tempUploadDir.resolve(Path.of(unusedPath).getFileName())));
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM tepdinhkem WHERE ma_tep_dinh_kem = ?", Integer.class, unusedId);
            assertEquals(0, count);
        }
    }
}
