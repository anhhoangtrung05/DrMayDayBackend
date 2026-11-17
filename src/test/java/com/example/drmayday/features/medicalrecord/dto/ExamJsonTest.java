package com.example.drmayday.features.medicalrecord.dto;

import com.example.drmayday.entity.AcuteVisit;
import com.example.drmayday.entity.ChronicVisit;
import com.example.drmayday.entity.ClinicRoom;
import com.example.drmayday.entity.Employee;
import com.example.drmayday.entity.MedicalVisit;
import com.example.drmayday.entity.Person;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Khóa hình dạng JSON của API 46 đến 49 (spec 0004) với mọi trường có giá trị khác nhau, để một getter đặt nhầm chỗ
 * hay một tên JSON gõ sai bị bắt. Test tích hợp chỉ thấy các cột subtype đều NULL, nên không phát hiện được lỗi này.
 * Chạy qua Jackson 3 như lúc chạy thật.
 */
class ExamJsonTest {

    // Đọc số thập phân thành BigDecimal để giữ nguyên chữ số 0 cuối mà server đã ghi (12.50 không thành 12.5)
    private static final JsonMapper JSON = JsonMapper.builder()
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS).build();

    private static JsonNode json(Object value) {
        return JSON.readTree(JSON.writeValueAsString(value));
    }

    // So từng trường theo dạng JSON thô, để 39.0 khác 39 và "7" khác 7
    private static void assertFields(JsonNode node, Map<String, String> expected) {
        assertThat(node.size()).as("số trường").isEqualTo(expected.size());
        expected.forEach((name, raw) -> {
            assertThat(node.has(name)).as("có trường " + name).isTrue();
            assertThat(node.get(name).toString()).as(name).isEqualTo(raw);
        });
    }

    private static MedicalVisit fullVisit(String type) {
        Employee employee = new Employee();
        employee.setPerson(Person.builder().personId(7L).fullName("Trần Bác Sĩ").build());
        ClinicRoom room = ClinicRoom.builder().clinicRoomId(30L).name("Phòng 101").build();
        return MedicalVisit.builder()
                .medicalVisitId(50L).medicalRecordId(10L).physicianId(7L).clinicRoomId(30L).medicalRecordType(type)
                .visitDate(LocalDate.of(2026, 11, 10)).visitNumber(3).visitType("Tai kham").serumSampleStored(true)
                .fever(true).feverTemperatureCelsius(new BigDecimal("39")).followUpDate(LocalDate.of(2026, 11, 20))
                .visitSummary("Tóm tắt").nextVisitManagement("Hướng xử trí").instructions("Dặn dò")
                .employee(employee).clinicRoom(room).build();
    }

    @Test
    @DisplayName("AC-17: ExamResponse (API 46, 49) có đủ 15 trường, id là chuỗi, nhiệt độ một chữ số thập phân")
    void examResponseShape() {
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("exam_id", "\"50\"");
        expected.put("record_id", "\"10\"");
        expected.put("ma_bac_si", "\"7\"");
        expected.put("ma_phong_kham", "\"30\"");
        expected.put("loai_benh_an", "\"CAP\"");
        expected.put("ngay_kham", "\"2026-11-10\"");
        expected.put("lan_kham_thu", "3");
        expected.put("loai_phien_kham", "\"Tai kham\"");
        expected.put("luu_huyet_thanh", "true");
        expected.put("sot", "true");
        expected.put("sot_nhiet_do_c", "39.0");
        expected.put("ngay_hen_tai_kham", "\"2026-11-20\"");
        expected.put("tom_tat_buoi_kham", "\"Tóm tắt\"");
        expected.put("huong_xu_tri_lan_sau", "\"Hướng xử trí\"");
        expected.put("dan_do", "\"Dặn dò\"");

        assertFields(json(ExamResponse.from(fullVisit("CAP"))), expected);
    }

    @Test
    @DisplayName("AC-10, AC-17: ExamSummaryResponse (API 47) có bac_si, phong_kham lồng nhau và đúng 12 trường")
    void summaryShape() {
        JsonNode node = json(new ExamSummaryResponse(fullVisit("CAP")));

        assertThat(node.size()).isEqualTo(12);
        assertThat(node.get("bac_si").toString()).isEqualTo("{\"ma_bac_si\":\"7\",\"ho_ten_bac_si\":\"Trần Bác Sĩ\"}");
        assertThat(node.get("phong_kham").toString()).isEqualTo("{\"ma_phong_kham\":\"30\",\"ten_phong_kham\":\"Phòng 101\"}");
        assertThat(node.get("sot_nhiet_do_c").toString()).isEqualTo("39.0");
        assertThat(node.get("lan_kham_thu").toString()).isEqualTo("3");
        assertThat(node.has("huong_xu_tri_lan_sau")).isFalse();
    }

    @Test
    @DisplayName("AC-10: phiên chưa có phòng thì phong_kham là null, không bị bỏ khỏi JSON")
    void summaryWithoutRoomHasNullRoom() {
        MedicalVisit visit = fullVisit("CAP");
        visit.setClinicRoom(null);
        visit.setClinicRoomId(null);

        JsonNode node = json(new ExamSummaryResponse(visit));

        assertThat(node.has("phong_kham")).isTrue();
        assertThat(node.get("phong_kham").isNull()).isTrue();
    }

    @Test
    @DisplayName("AC-11: chi tiết CAP đọc đúng từng cột phienkhamcap sang đúng tên JSON")
    void acuteDetailMapsEveryColumn() {
        AcuteVisit acute = AcuteVisit.builder().medicalVisitId(50L)
                .skinSymptoms("da").respiratorySymptoms("hô hấp").respiratorySymptomsComparedToUsual("hô hấp so")
                .cardiovascularSymptoms("tuần hoàn").cardiovascularSymptomsComparedToUsual("tuần hoàn so")
                .gastrointestinalSymptoms("tiêu hóa").gastrointestinalSymptomsComparedToUsual("tiêu hóa so")
                .hasWheals(true).hasWhealPhotos(false).vomitingEpisodes(4).build();
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("trieu_chung_da", "\"da\"");
        expected.put("trieu_chung_ho_hap", "\"hô hấp\"");
        expected.put("so_voi_thuong_ngay_ho_hap", "\"hô hấp so\"");
        expected.put("trieu_chung_tuan_hoan", "\"tuần hoàn\"");
        expected.put("so_voi_thuong_ngay_tuan_hoan", "\"tuần hoàn so\"");
        expected.put("trieu_chung_tieu_hoa", "\"tiêu hóa\"");
        expected.put("so_voi_thuong_ngay_tieu_hoa", "\"tiêu hóa so\"");
        expected.put("co_san_phu", "true");
        expected.put("co_anh_san_phu", "false");
        expected.put("so_lan_non", "4");

        JsonNode node = json(new AcuteExamDetailResponse(fullVisit("CAP"), AcuteExamFindingsDto.from(acute)));

        assertFields(node.get("chi_tiet_lam_sang"), expected);
        assertThat(node.get("loai_benh_an").toString()).isEqualTo("\"CAP\"");
        assertThat(node.get("dan_do").toString()).isEqualTo("\"Dặn dò\"");
        assertThat(node.size()).isEqualTo(16);
    }

    @Test
    @DisplayName("AC-11: chi tiết MAN đọc đúng từng cột phienkhamman, hai số thập phân luôn hai chữ số")
    void chronicDetailMapsEveryColumn() {
        ChronicVisit chronic = new ChronicVisit();
        chronic.setMedicalVisitId(50L);
        chronic.setCurrentlyHasWheals(true);
        chronic.setSpontaneousPredominance(false);
        chronic.setWhealLocations("vị trí");
        chronic.setPapularUrticariaAggravatingFactor("yếu tố");
        chronic.setWhealOnsetContext("hoàn cảnh sẩn");
        chronic.setAngioedemaDuringEpisode(true);
        chronic.setCurrentlyHasAngioedema(false);
        chronic.setAngioedemaEpisodesInCurrentPeriod(2);
        chronic.setAngioedemaOnsetContext("hoàn cảnh phù");
        chronic.setAngioedemaHospitalizations(1);
        chronic.setAngioedemaDurationWithMedication("có thuốc");
        chronic.setAngioedemaDurationWithoutMedication("không thuốc");
        chronic.setAas7Score((short) 42);
        chronic.setAngioedemaSide("trái");
        chronic.setSpontaneousDurationWeeks(new BigDecimal("12.5"));
        chronic.setInduciblePredominance(true);
        chronic.setInducibleDurationWeeks(new BigDecimal("3"));
        chronic.setEquivalentPredominance(false);
        chronic.setPredominanceUnknown(true);
        chronic.setOnsetOrderComparedToWheals("trước");
        chronic.setAngioedemaWithinLastWeek(false);
        chronic.setFollowUpDiagnosis("chẩn đoán");
        chronic.setOnsetInterval("khoảng cách");
        chronic.setEyelidEdema(true);
        chronic.setTongueEdema(false);
        chronic.setLipEdema(true);
        chronic.setOtherFacialEdema(false);
        chronic.setLaryngealEdema(true);
        chronic.setFootEdema(false);
        chronic.setGenitalEdema(true);
        chronic.setEdemaAtOtherLocation(true);
        chronic.setOtherEdemaLocationDescription("lưng");
        chronic.setHandEdema(false);
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("hien_tai_co_san_phu", "true");
        expected.put("uu_the_ngau_nhien", "false");
        expected.put("vi_tri_san_phu", "\"vị trí\"");
        expected.put("yeu_to_lam_nang_sa_phu", "\"yếu tố\"");
        expected.put("hoan_canh_xuat_hien_san_phu", "\"hoàn cảnh sẩn\"");
        expected.put("trong_dot_co_phu_mach", "true");
        expected.put("hien_tai_co_phu_mach", "false");
        expected.put("so_lan_phu_mach_dot_nay", "2");
        expected.put("hoan_canh_xuat_hien_phu_mach", "\"hoàn cảnh phù\"");
        expected.put("so_lan_phu_mach_nang_phai_nhap_vien", "1");
        expected.put("thoi_gian_ton_tai_phu_mach_co_dung_thuoc", "\"có thuốc\"");
        expected.put("thoi_gian_ton_tai_phu_mach_khong_dung_thuoc", "\"không thuốc\"");
        expected.put("diem_aas7", "42");
        expected.put("ben_phu_mach", "\"trái\"");
        expected.put("thoi_gian_ngau_nhien_tuan", "12.50");
        expected.put("uu_the_kich_thich", "true");
        expected.put("thoi_gian_kich_thich_tuan", "3.00");
        expected.put("uu_the_tuong_duong", "false");
        expected.put("uu_the_khong_ro", "true");
        expected.put("thu_tu_xuat_hien_so_voi_san_phu", "\"trước\"");
        expected.put("phu_mach_1_tuan_gan_day", "false");
        expected.put("chan_doan_tai_kham", "\"chẩn đoán\"");
        expected.put("khoang_cach_xuat_hien", "\"khoảng cách\"");
        expected.put("phu_mi_mat", "true");
        expected.put("phu_luoi", "false");
        expected.put("phu_moi", "true");
        expected.put("phu_vung_mat_khac", "false");
        expected.put("phu_thanh_quan", "true");
        expected.put("phu_ban_chan", "false");
        expected.put("phu_sinh_duc", "true");
        expected.put("phu_vi_tri_khac", "true");
        expected.put("phu_vi_tri_khac_ghi_ro", "\"lưng\"");
        expected.put("phu_ban_tay", "false");

        JsonNode node = json(new ChronicExamDetailResponse(fullVisit("MAN"), ChronicExamFindingsDto.from(chronic)));

        assertFields(node.get("chi_tiet_lam_sang"), expected);
        assertThat(node.get("loai_benh_an").toString()).isEqualTo("\"MAN\"");
    }

    @Test
    @DisplayName("AC-11: thiếu dòng subtype thì chi_tiet_lam_sang là null, vẫn có trong JSON")
    void missingSubtypeIsNullFindings() {
        JsonNode node = json(new AcuteExamDetailResponse(fullVisit("CAP"), AcuteExamFindingsDto.from(null)));

        assertThat(node.has("chi_tiet_lam_sang")).isTrue();
        assertThat(node.get("chi_tiet_lam_sang").isNull()).isTrue();
    }

    @Test
    @DisplayName("AC-17: oneDecimal làm tròn về đúng một chữ số thập phân, null giữ null")
    void oneDecimalFormatting() {
        assertThat(JsonValues.oneDecimal(new BigDecimal("39")).toPlainString()).isEqualTo("39.0");
        assertThat(JsonValues.oneDecimal(new BigDecimal("38.50")).toPlainString()).isEqualTo("38.5");
        assertThat(JsonValues.oneDecimal(null)).isNull();
    }
}
