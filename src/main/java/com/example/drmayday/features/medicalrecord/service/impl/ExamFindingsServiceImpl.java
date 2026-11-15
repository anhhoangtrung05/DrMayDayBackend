package com.example.drmayday.features.medicalrecord.service.impl;

import com.example.drmayday.common.exception.AppException;
import com.example.drmayday.common.exception.ErrorCode;
import com.example.drmayday.entity.AcuteVisit;
import com.example.drmayday.entity.ChronicVisit;
import com.example.drmayday.entity.MedicalVisit;
import com.example.drmayday.features.medicalrecord.dto.AcuteExamFindingsResponse;
import com.example.drmayday.features.medicalrecord.dto.ChronicExamFindingsResponse;
import com.example.drmayday.features.medicalrecord.dto.UpsertAcuteExamFindingsRequest;
import com.example.drmayday.features.medicalrecord.dto.UpsertChronicExamFindingsRequest;
import com.example.drmayday.features.medicalrecord.service.ExamFindingsService;
import com.example.drmayday.repository.AcuteVisitRepository;
import com.example.drmayday.repository.ChronicVisitRepository;
import com.example.drmayday.repository.MedicalVisitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExamFindingsServiceImpl implements ExamFindingsService {

    private final MedicalVisitRepository medicalVisitRepository;
    private final AcuteVisitRepository acuteVisitRepository;
    private final ChronicVisitRepository chronicVisitRepository;

    public ExamFindingsServiceImpl(MedicalVisitRepository medicalVisitRepository,
                                  AcuteVisitRepository acuteVisitRepository,
                                  ChronicVisitRepository chronicVisitRepository) {
        this.medicalVisitRepository = medicalVisitRepository;
        this.acuteVisitRepository = acuteVisitRepository;
        this.chronicVisitRepository = chronicVisitRepository;
    }

    @Override
    @Transactional
    public AcuteExamFindingsResponse upsertAcuteExamFindings(UpsertAcuteExamFindingsRequest req) {
        if (req == null || req.getExamId() == null || req.getExamId().isBlank()) {
            throw new AppException(ErrorCode.MISSING_REQUIRED_PARAMETER, "Missing required parameter: exam_id");
        }

        Long examIdLong;
        try {
            examIdLong = Long.parseLong(req.getExamId().trim());
        } catch (NumberFormatException e) {
            throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE, "exam_id must be a numeric value");
        }

        MedicalVisit visit = medicalVisitRepository.findById(examIdLong)
                .orElseThrow(() -> new AppException(ErrorCode.EXAM_NOT_FOUND, "Exam not found: " + examIdLong));

        if (!"CAP".equalsIgnoreCase(visit.getMedicalRecordType())) {
            throw new AppException(ErrorCode.RECORD_EXAM_TYPE_MISMATCH, "Record/Exam type mismatch: exam is not CAP");
        }

        if (req.getSoLanNon() != null && req.getSoLanNon() < 0) {
            throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE, "so_lan_non must be >= 0");
        }

        AcuteVisit acuteVisit = acuteVisitRepository.findById(examIdLong).orElse(null);
        if (acuteVisit == null) {
            acuteVisit = AcuteVisit.builder()
                    .medicalVisitId(examIdLong)
                    .newEntity(true)
                    .build();
        }

        if (req.getCoSanPhu() != null) {
            acuteVisit.setHasWheals(req.getCoSanPhu());
        }
        if (req.getCoAnhSanPhu() != null) {
            acuteVisit.setHasWhealPhotos(req.getCoAnhSanPhu());
        }
        if (req.getSoLanNon() != null) {
            acuteVisit.setVomitingEpisodes(req.getSoLanNon());
        }
        if (req.getTrieuChungDaGhiRo() != null) {
            acuteVisit.setSkinSymptoms(req.getTrieuChungDaGhiRo());
        } else if (Boolean.TRUE.equals(req.getTrieuChungDaNiemMac())) {
            acuteVisit.setSkinSymptoms("Có triệu chứng da niêm mạc");
        }
        if (req.getTrieuChungHoHapGhiRo() != null) {
            acuteVisit.setRespiratorySymptoms(req.getTrieuChungHoHapGhiRo());
        } else if (Boolean.TRUE.equals(req.getTrieuChungHoHap())) {
            acuteVisit.setRespiratorySymptoms("Có triệu chứng hô hấp");
        }
        if (req.getTrieuChungTuanHoanGhiRo() != null) {
            acuteVisit.setCardiovascularSymptoms(req.getTrieuChungTuanHoanGhiRo());
        } else if (Boolean.TRUE.equals(req.getTrieuChungTuanHoan())) {
            acuteVisit.setCardiovascularSymptoms("Có triệu chứng tuần hoàn");
        }
        if (req.getTrieuChungTieuHoaGhiRo() != null) {
            acuteVisit.setGastrointestinalSymptoms(req.getTrieuChungTieuHoaGhiRo());
        } else if (Boolean.TRUE.equals(req.getTrieuChungTieuHoa())) {
            acuteVisit.setGastrointestinalSymptoms("Có triệu chứng tiêu hóa");
        }

        acuteVisitRepository.save(acuteVisit);

        return AcuteExamFindingsResponse.builder()
                .examId(String.valueOf(examIdLong))
                .loaiBenhAn("CAP")
                .coSanPhu(req.getCoSanPhu() != null ? req.getCoSanPhu() : acuteVisit.getHasWheals())
                .coAnhSanPhu(req.getCoAnhSanPhu() != null ? req.getCoAnhSanPhu() : acuteVisit.getHasWhealPhotos())
                .trieuChungDaNiemMac(req.getTrieuChungDaNiemMac())
                .trieuChungDaGhiRo(req.getTrieuChungDaGhiRo() != null ? req.getTrieuChungDaGhiRo() : acuteVisit.getSkinSymptoms())
                .trieuChungHoHap(req.getTrieuChungHoHap())
                .trieuChungHoHapGhiRo(req.getTrieuChungHoHapGhiRo() != null ? req.getTrieuChungHoHapGhiRo() : acuteVisit.getRespiratorySymptoms())
                .trieuChungTuanHoan(req.getTrieuChungTuanHoan())
                .trieuChungTuanHoanGhiRo(req.getTrieuChungTuanHoanGhiRo() != null ? req.getTrieuChungTuanHoanGhiRo() : acuteVisit.getCardiovascularSymptoms())
                .trieuChungTieuHoa(req.getTrieuChungTieuHoa())
                .trieuChungTieuHoaGhiRo(req.getTrieuChungTieuHoaGhiRo() != null ? req.getTrieuChungTieuHoaGhiRo() : acuteVisit.getGastrointestinalSymptoms())
                .soLanNon(req.getSoLanNon() != null ? req.getSoLanNon() : acuteVisit.getVomitingEpisodes())
                .trieuChungKhac(req.getTrieuChungKhac())
                .build();
    }

    @Override
    @Transactional
    public ChronicExamFindingsResponse upsertChronicExamFindings(UpsertChronicExamFindingsRequest req) {
        if (req == null || req.getExamId() == null || req.getExamId().isBlank()) {
            throw new AppException(ErrorCode.MISSING_REQUIRED_PARAMETER, "Missing required parameter: exam_id");
        }

        Long examIdLong;
        try {
            examIdLong = Long.parseLong(req.getExamId().trim());
        } catch (NumberFormatException e) {
            throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE, "exam_id must be a numeric value");
        }

        MedicalVisit visit = medicalVisitRepository.findById(examIdLong)
                .orElseThrow(() -> new AppException(ErrorCode.EXAM_NOT_FOUND, "Exam not found: " + examIdLong));

        if (!"MAN".equalsIgnoreCase(visit.getMedicalRecordType())) {
            throw new AppException(ErrorCode.RECORD_EXAM_TYPE_MISMATCH, "Record/Exam type mismatch: exam is not MAN");
        }

        if (req.getDiemAas7() != null && (req.getDiemAas7() < 0 || req.getDiemAas7() > 105)) {
            throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE, "diem_aas7 must be between 0 and 105");
        }

        if (Boolean.TRUE.equals(req.getPhuViTriKhac()) && (req.getPhuViTriKhacGhiRo() == null || req.getPhuViTriKhacGhiRo().isBlank())) {
            throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE, "phu_vi_tri_khac_ghi_ro is required when phu_vi_tri_khac is true");
        }

        ChronicVisit chronicVisit = chronicVisitRepository.findById(examIdLong).orElse(null);
        if (chronicVisit == null) {
            chronicVisit = ChronicVisit.builder()
                    .medicalVisitId(examIdLong)
                    .newEntity(true)
                    .build();
        }

        if (req.getHienTaiCoSanPhu() != null) {
            chronicVisit.setCurrentlyHasWheals(req.getHienTaiCoSanPhu());
        }
        if (req.getUuTheNgauNhien() != null) {
            chronicVisit.setSpontaneousPredominance(req.getUuTheNgauNhien());
        }
        if (req.getTrongDotCoPhuMach() != null) {
            chronicVisit.setAngioedemaDuringEpisode(req.getTrongDotCoPhuMach());
        }
        if (req.getHienTaiCoPhuMach() != null) {
            chronicVisit.setCurrentlyHasAngioedema(req.getHienTaiCoPhuMach());
        }
        if (req.getPhuViTriMat() != null) {
            chronicVisit.setEyelidEdema(req.getPhuViTriMat());
        }
        if (req.getPhuViTriMoi() != null) {
            chronicVisit.setLipEdema(req.getPhuViTriMoi());
        }
        if (req.getPhuViTriLuoi() != null) {
            chronicVisit.setTongueEdema(req.getPhuViTriLuoi());
        }
        if (req.getPhuViTriThanhQuan() != null) {
            chronicVisit.setLaryngealEdema(req.getPhuViTriThanhQuan());
        }
        if (req.getPhuViTriBanTayChan() != null) {
            chronicVisit.setHandEdema(req.getPhuViTriBanTayChan());
            chronicVisit.setFootEdema(req.getPhuViTriBanTayChan());
        }
        if (req.getPhuViTriBoPhanSinhDuc() != null) {
            chronicVisit.setGenitalEdema(req.getPhuViTriBoPhanSinhDuc());
        }
        if (req.getPhuViTriKhac() != null) {
            chronicVisit.setEdemaAtOtherLocation(req.getPhuViTriKhac());
        }
        if (req.getPhuViTriKhacGhiRo() != null) {
            chronicVisit.setOtherEdemaLocationDescription(req.getPhuViTriKhacGhiRo());
        }
        if (req.getDiemAas7() != null) {
            chronicVisit.setAas7Score(req.getDiemAas7().shortValue());
        }
        if (req.getChanDoanTaiKham() != null) {
            chronicVisit.setFollowUpDiagnosis(req.getChanDoanTaiKham());
        }
        if (req.getThoiGianTonTaiPhuMach() != null) {
            chronicVisit.setAngioedemaDurationWithoutMedication(req.getThoiGianTonTaiPhuMach());
        }

        chronicVisitRepository.save(chronicVisit);

        return ChronicExamFindingsResponse.builder()
                .examId(String.valueOf(examIdLong))
                .loaiBenhAn("MAN")
                .hienTaiCoSanPhu(req.getHienTaiCoSanPhu() != null ? req.getHienTaiCoSanPhu() : chronicVisit.getCurrentlyHasWheals())
                .coAnhSanPhu(req.getCoAnhSanPhu())
                .uuTheNgauNhien(req.getUuTheNgauNhien() != null ? req.getUuTheNgauNhien() : chronicVisit.getSpontaneousPredominance())
                .thoiGianTonTaiSanPhu(req.getThoiGianTonTaiSanPhu())
                .trongDotCoPhuMach(req.getTrongDotCoPhuMach() != null ? req.getTrongDotCoPhuMach() : chronicVisit.getAngioedemaDuringEpisode())
                .hienTaiCoPhuMach(req.getHienTaiCoPhuMach() != null ? req.getHienTaiCoPhuMach() : chronicVisit.getCurrentlyHasAngioedema())
                .coAnhPhuMach(req.getCoAnhPhuMach())
                .phuViTriMat(req.getPhuViTriMat() != null ? req.getPhuViTriMat() : chronicVisit.getEyelidEdema())
                .phuViTriMoi(req.getPhuViTriMoi() != null ? req.getPhuViTriMoi() : chronicVisit.getLipEdema())
                .phuViTriLuoi(req.getPhuViTriLuoi() != null ? req.getPhuViTriLuoi() : chronicVisit.getTongueEdema())
                .phuViTriThanhQuan(req.getPhuViTriThanhQuan() != null ? req.getPhuViTriThanhQuan() : chronicVisit.getLaryngealEdema())
                .phuViTriBanTayChan(req.getPhuViTriBanTayChan())
                .phuViTriBoPhanSinhDuc(req.getPhuViTriBoPhanSinhDuc() != null ? req.getPhuViTriBoPhanSinhDuc() : chronicVisit.getGenitalEdema())
                .phuViTriKhac(req.getPhuViTriKhac() != null ? req.getPhuViTriKhac() : chronicVisit.getEdemaAtOtherLocation())
                .phuViTriKhacGhiRo(req.getPhuViTriKhacGhiRo() != null ? req.getPhuViTriKhacGhiRo() : chronicVisit.getOtherEdemaLocationDescription())
                .thoiGianTonTaiPhuMach(req.getThoiGianTonTaiPhuMach() != null ? req.getThoiGianTonTaiPhuMach() : chronicVisit.getAngioedemaDurationWithoutMedication())
                .diemAas7(req.getDiemAas7() != null ? req.getDiemAas7() : (chronicVisit.getAas7Score() != null ? chronicVisit.getAas7Score().intValue() : null))
                .chanDoanTaiKham(req.getChanDoanTaiKham() != null ? req.getChanDoanTaiKham() : chronicVisit.getFollowUpDiagnosis())
                .build();
    }
}
