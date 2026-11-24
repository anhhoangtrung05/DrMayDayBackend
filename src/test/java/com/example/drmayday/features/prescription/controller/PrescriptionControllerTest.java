package com.example.drmayday.features.prescription.controller;

import com.example.drmayday.common.exception.AppException;
import com.example.drmayday.common.exception.ErrorCode;
import com.example.drmayday.common.exception.GlobalExceptionHandler;
import com.example.drmayday.features.prescription.dto.CreatePrescriptionRequest;
import com.example.drmayday.features.prescription.dto.PrescriptionItemResponse;
import com.example.drmayday.features.prescription.dto.PrescriptionResponse;
import com.example.drmayday.features.prescription.dto.StopInstructionResponse;
import com.example.drmayday.features.prescription.dto.StopMedicationResponse;
import com.example.drmayday.features.prescription.dto.UpdatePrescriptionRequest;
import com.example.drmayday.features.prescription.dto.UpsertStopMedicationRequest;
import com.example.drmayday.features.prescription.service.PrescriptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PrescriptionControllerTest {

    private static final String BASE = "/api/v1/prescriptions";
    private static final String STOP = BASE + "/stop-instructions";

    private MockMvc mockMvc;

    @Mock
    private PrescriptionService prescriptionService;

    @InjectMocks
    private PrescriptionController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private static PrescriptionResponse sampleResponse() {
        return PrescriptionResponse.builder()
                .prescriptionId("100").recordId("1").phienKhamId("10")
                .ngayKeDon(LocalDate.of(2026, 11, 24)).trangThaiDon("DANG_DIEU_TRI").ngungThuocLanSau(false)
                .items(List.of(
                        PrescriptionItemResponse.builder().chiTietId("1").maThuoc("T01").lieuDung("1 viên").thoiGianDung("7 ngày").build(),
                        PrescriptionItemResponse.builder().chiTietId("2").tenThuocBoSung("Thuốc nam").lieuDung("2 viên").thoiGianDung("5 ngày").build()))
                .build();
    }

    private static StopMedicationResponse sampleStopResponse() {
        return StopMedicationResponse.builder()
                .donThuocId("100").ngungThuocLanSau(true)
                .instructions(List.of(StopInstructionResponse.builder()
                        .chiDanId("1").thuocCanNgung("Cetirizin").mucDich("Test kich thich").build()))
                .build();
    }

    // ── PRESCRIPTIONS_CREATE_PRESCRIPTIONS ──

    @Nested
    @DisplayName("POST /api/v1/prescriptions")
    class CreatePrescription {

        @Test
        @DisplayName("[Happy Path] Kê đơn hợp lệ trả về 201 và mã 1001")
        void returnsCreated() throws Exception {
            when(prescriptionService.createPrescription(any(CreatePrescriptionRequest.class))).thenReturn(sampleResponse());

            mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content("""
                            {"record_id": "1", "phien_kham_id": 10, "ngay_ke_don": "2026-11-24",
                             "items": [{"ma_thuoc": "T01", "lieu_dung": "1 viên", "thoi_gian_dung": "7 ngày"},
                                       {"ten_thuoc_bo_sung": "Thuốc nam", "lieu_dung": "2 viên", "thoi_gian_dung": "5 ngày"}]}
                            """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.code").value("1001"))
                    .andExpect(jsonPath("$.data.prescription_id").value("100"))
                    .andExpect(jsonPath("$.data.ngay_ke_don").value("2026-11-24"))
                    .andExpect(jsonPath("$.data.items.length()").value(2))
                    .andExpect(jsonPath("$.data.items[1].ten_thuoc_bo_sung").value("Thuốc nam"));

            ArgumentCaptor<CreatePrescriptionRequest> captor = ArgumentCaptor.forClass(CreatePrescriptionRequest.class);
            verify(prescriptionService).createPrescription(captor.capture());
            assertEquals(1L, captor.getValue().getRecordId());
            assertEquals(LocalDate.of(2026, 11, 24), captor.getValue().getNgayKeDon());
            assertEquals("T01", captor.getValue().getItems().get(0).getMaThuoc());
        }

        @Test
        @DisplayName("[Unhappy Path] items rỗng trả về 400 và mã 8001")
        void emptyItems() throws Exception {
            when(prescriptionService.createPrescription(any())).thenThrow(new AppException(ErrorCode.PRESCRIPTION_ITEMS_EMPTY));

            mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"record_id\": 1, \"phien_kham_id\": 10, \"items\": []}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("8001"));
        }

        @Test
        @DisplayName("[Unhappy Path] ngay_ke_don sai định dạng YYYY-MM-DD trả về 400 và mã 2003")
        void malformedDate() throws Exception {
            mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"record_id\": 1, \"phien_kham_id\": 10, \"ngay_ke_don\": \"24/11/2026\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("2003"));
            verify(prescriptionService, never()).createPrescription(any());
        }

        @Test
        @DisplayName("[Exception Path] Phiên khám không thuộc hồ sơ trả về 404 và mã 5008")
        void visitNotInRecord() throws Exception {
            when(prescriptionService.createPrescription(any())).thenThrow(new AppException(ErrorCode.EXAM_NOT_FOUND));

            mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"record_id\": 1, \"phien_kham_id\": 99, \"items\": []}"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("5008"));
        }
    }

    // ── PRESCRIPTIONS_GET_PRESCRIPTIONS ──

    @Nested
    @DisplayName("GET /api/v1/prescriptions")
    class GetPrescriptions {

        @Test
        @DisplayName("[Happy Path] Lọc theo phien_kham_id trả về 200 và danh sách đơn")
        void filtersByVisit() throws Exception {
            when(prescriptionService.getPrescriptions(10L, null)).thenReturn(List.of(sampleResponse()));

            mockMvc.perform(get(BASE).param("phien_kham_id", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("1000"))
                    .andExpect(jsonPath("$.data[0].prescription_id").value("100"))
                    .andExpect(jsonPath("$.data[0].ngung_thuoc_lan_sau").value(false));
        }

        @Test
        @DisplayName("[Happy Path] Lọc theo record_id trả về 200")
        void filtersByRecord() throws Exception {
            when(prescriptionService.getPrescriptions(null, 1L)).thenReturn(List.of());

            mockMvc.perform(get(BASE).param("record_id", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(0));
        }

        @Test
        @DisplayName("[Unhappy Path] Không truyền query nào trả về 400 và mã 2001")
        void missingFilters() throws Exception {
            when(prescriptionService.getPrescriptions(null, null))
                    .thenThrow(new AppException(ErrorCode.MISSING_REQUIRED_PARAMETER, "Cần truyền ít nhất một trong hai tham số"));

            mockMvc.perform(get(BASE))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("2001"));
        }

        @Test
        @DisplayName("[Unhappy Path] phien_kham_id không phải số trả về 400 và mã 2002")
        void nonNumericFilter() throws Exception {
            mockMvc.perform(get(BASE).param("phien_kham_id", "abc"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("2002"));
        }
    }

    // ── PRESCRIPTIONS_UPDATE_PRESCRIPTION ──

    @Nested
    @DisplayName("PUT /api/v1/prescriptions/{id}")
    class UpdatePrescription {

        @Test
        @DisplayName("[Happy Path] Đổi trạng thái đơn trả về 200 và mã 1000")
        void updatesPrescription() throws Exception {
            PrescriptionResponse updated = sampleResponse();
            updated.setTrangThaiDon("HOAN_THANH");
            when(prescriptionService.updatePrescription(eq(100L), any(UpdatePrescriptionRequest.class))).thenReturn(updated);

            mockMvc.perform(put(BASE + "/100").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"trang_thai_don\": \"HOAN_THANH\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("1000"))
                    .andExpect(jsonPath("$.data.trang_thai_don").value("HOAN_THANH"));
        }

        @Test
        @DisplayName("[Exception Path] id không tồn tại trả về 404 và mã 8003")
        void prescriptionNotFound() throws Exception {
            when(prescriptionService.updatePrescription(eq(999L), any()))
                    .thenThrow(new AppException(ErrorCode.PRESCRIPTION_NOT_FOUND));

            mockMvc.perform(put(BASE + "/999").contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("8003"));
        }

        @Test
        @DisplayName("[Unhappy Path] Item vi phạm XOR trả về 400 và mã 8004")
        void itemViolatesXor() throws Exception {
            when(prescriptionService.updatePrescription(eq(100L), any()))
                    .thenThrow(new AppException(ErrorCode.PRESCRIPTION_ITEM_MEDICATION_CONFLICT));

            mockMvc.perform(put(BASE + "/100").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"items\": [{\"ma_thuoc\": \"T01\", \"ten_thuoc_bo_sung\": \"B\"}]}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("8004"));
        }
    }

    // ── PRESCRIPTIONS_UPSERT_STOP_MEDICATION ──

    @Nested
    @DisplayName("PUT /api/v1/prescriptions/stop-instructions")
    class UpsertStopMedication {

        @Test
        @DisplayName("[Happy Path] Ghi chỉ dẫn ngừng thuốc trả về 200, không bị nhầm sang PUT /{id}")
        void savesInstructions() throws Exception {
            when(prescriptionService.upsertStopMedication(any(UpsertStopMedicationRequest.class))).thenReturn(sampleStopResponse());

            mockMvc.perform(put(STOP).contentType(MediaType.APPLICATION_JSON).content("""
                            {"don_thuoc_id": 100,
                             "instructions": [{"thuoc_can_ngung": "Cetirizin", "muc_dich": "Test kich thich"}]}
                            """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("1000"))
                    .andExpect(jsonPath("$.data.don_thuoc_id").value("100"))
                    .andExpect(jsonPath("$.data.ngung_thuoc_lan_sau").value(true))
                    .andExpect(jsonPath("$.data.instructions[0].chi_dan_id").value("1"));

            verify(prescriptionService, never()).updatePrescription(any(), any());
        }

        @Test
        @DisplayName("[Unhappy Path] muc_dich = Khac không ghi rõ trả về 400 và mã 8201")
        void otherPurposeWithoutDescription() throws Exception {
            when(prescriptionService.upsertStopMedication(any()))
                    .thenThrow(new AppException(ErrorCode.STOP_PURPOSE_OTHER_DESCRIPTION_REQUIRED));

            mockMvc.perform(put(STOP).contentType(MediaType.APPLICATION_JSON).content(
                            "{\"don_thuoc_id\": 100, \"instructions\": [{\"thuoc_can_ngung\": \"A\", \"muc_dich\": \"Khac\"}]}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("8201"));
        }

        @Test
        @DisplayName("[Exception Path] don_thuoc_id không tồn tại trả về 404 và mã 8003")
        void prescriptionNotFound() throws Exception {
            when(prescriptionService.upsertStopMedication(any()))
                    .thenThrow(new AppException(ErrorCode.PRESCRIPTION_NOT_FOUND));

            mockMvc.perform(put(STOP).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"don_thuoc_id\": 999, \"instructions\": []}"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("8003"));
        }
    }

    // ── PRESCRIPTIONS_GET_STOP_MEDICATION ──

    @Nested
    @DisplayName("GET /api/v1/prescriptions/stop-instructions")
    class GetStopMedication {

        @Test
        @DisplayName("[Happy Path] Lấy chỉ dẫn ngừng thuốc trả về 200")
        void returnsInstructions() throws Exception {
            when(prescriptionService.getStopMedication(100L)).thenReturn(sampleStopResponse());

            mockMvc.perform(get(STOP).param("don_thuoc_id", "100"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("1000"))
                    .andExpect(jsonPath("$.data.instructions.length()").value(1));
        }

        @Test
        @DisplayName("[Unhappy Path] Thiếu don_thuoc_id trả về 400 và mã 2001")
        void missingPrescriptionId() throws Exception {
            when(prescriptionService.getStopMedication(null))
                    .thenThrow(new AppException(ErrorCode.MISSING_REQUIRED_PARAMETER, "Thiếu tham số bắt buộc: don_thuoc_id"));

            mockMvc.perform(get(STOP))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("2001"))
                    .andExpect(jsonPath("$.message").value("Thiếu tham số bắt buộc: don_thuoc_id"));
        }

        @Test
        @DisplayName("[Exception Path] don_thuoc_id không tồn tại trả về 404 và mã 8003")
        void prescriptionNotFound() throws Exception {
            when(prescriptionService.getStopMedication(999L)).thenThrow(new AppException(ErrorCode.PRESCRIPTION_NOT_FOUND));

            mockMvc.perform(get(STOP).param("don_thuoc_id", "999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("8003"));
        }
    }
}
