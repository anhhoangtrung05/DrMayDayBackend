package com.drmayday.authrbac.config;

import com.drmayday.authrbac.entity.BenhNhan;
import com.drmayday.authrbac.entity.Nguoi;
import com.drmayday.authrbac.entity.NhanVien;
import com.drmayday.authrbac.entity.TaiKhoan;
import com.drmayday.authrbac.entity.VaiTro;
import com.drmayday.authrbac.repository.BenhNhanRepository;
import com.drmayday.authrbac.repository.NguoiRepository;
import com.drmayday.authrbac.repository.NhanVienRepository;
import com.drmayday.authrbac.repository.TaiKhoanRepository;
import com.drmayday.authrbac.repository.VaiTroRepository;
import com.drmayday.facility.entity.ChiNhanh;
import com.drmayday.facility.repository.ChiNhanhRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseSeeder.class);

    private final JdbcTemplate jdbcTemplate;
    private final ChiNhanhRepository chiNhanhRepository;
    private final VaiTroRepository vaiTroRepository;
    private final NguoiRepository nguoiRepository;
    private final BenhNhanRepository benhNhanRepository;
    private final NhanVienRepository nhanVienRepository;
    private final TaiKhoanRepository taiKhoanRepository;
    private final PasswordEncoder passwordEncoder;

    public DatabaseSeeder(JdbcTemplate jdbcTemplate,
                          ChiNhanhRepository chiNhanhRepository,
                          VaiTroRepository vaiTroRepository,
                          NguoiRepository nguoiRepository,
                          BenhNhanRepository benhNhanRepository,
                          NhanVienRepository nhanVienRepository,
                          TaiKhoanRepository taiKhoanRepository,
                          PasswordEncoder passwordEncoder) {
        this.jdbcTemplate = jdbcTemplate;
        this.chiNhanhRepository = chiNhanhRepository;
        this.vaiTroRepository = vaiTroRepository;
        this.nguoiRepository = nguoiRepository;
        this.benhNhanRepository = benhNhanRepository;
        this.nhanVienRepository = nhanVienRepository;
        this.taiKhoanRepository = taiKhoanRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        try {
            initTokenBlacklistTable();
            ChiNhanh defaultBranch = ensureDefaultBranch();
            ensureRoles();
            ensureAdminAccount(defaultBranch);
            ensurePatientAccount();
            log.info("DatabaseSeeder hoàn tất khởi tạo dữ liệu mẫu thành công.");
        } catch (Exception ex) {
            log.warn("Lỗi trong quá trình DatabaseSeeder: {}", ex.getMessage());
        }
    }

    private void initTokenBlacklistTable() {
        try {
            jdbcTemplate.execute(
                    "CREATE TABLE IF NOT EXISTS token_blacklist (" +
                    "invalid_token_id SERIAL PRIMARY KEY, " +
                    "jti VARCHAR(64) NOT NULL UNIQUE, " +
                    "user_id BIGINT NOT NULL, " +
                    "expires_at TIMESTAMP NOT NULL, " +
                    "created_at TIMESTAMP NOT NULL, " +
                    "updated_at TIMESTAMP NOT NULL" +
                    ")"
            );
        } catch (Exception e) {
            log.debug("Khởi tạo bảng token_blacklist: {}", e.getMessage());
        }
    }

    private ChiNhanh ensureDefaultBranch() {
        return chiNhanhRepository.findFirstByOrderByMaChiNhanhAsc().orElseGet(() -> {
            ChiNhanh branch = new ChiNhanh();
            branch.setTenChiNhanh("Phòng khám Đa khoa DrMayDay - Cơ sở chính");
            branch.setDiaChi("123 Đường Giải Phóng, Hà Nội");
            branch.setSoDienThoai("02438888888");
            branch.setTrangThai(true);
            return chiNhanhRepository.save(branch);
        });
    }

    private void ensureRoles() {
        if (!vaiTroRepository.existsByTenVaiTro("admin")) {
            vaiTroRepository.save(new VaiTro(1L, "admin", "Quản trị viên", "Toàn quyền quản trị hệ thống", OffsetDateTime.now(), OffsetDateTime.now()));
        }
        if (!vaiTroRepository.existsByTenVaiTro("bac_si")) {
            vaiTroRepository.save(new VaiTro(2L, "bac_si", "Bác sĩ", "Bác sĩ điều trị", OffsetDateTime.now(), OffsetDateTime.now()));
        }
        if (!vaiTroRepository.existsByTenVaiTro("dieu_duong")) {
            vaiTroRepository.save(new VaiTro(3L, "dieu_duong", "Điều dưỡng", "Điều dưỡng viên hỗ trợ", OffsetDateTime.now(), OffsetDateTime.now()));
        }
        if (!vaiTroRepository.existsByTenVaiTro("benh_nhan")) {
            vaiTroRepository.save(new VaiTro(4L, "benh_nhan", "Bệnh nhân", "Bệnh nhân sử dụng ứng dụng", OffsetDateTime.now(), OffsetDateTime.now()));
        }
    }

    private void ensureAdminAccount(ChiNhanh defaultBranch) {
        String adminPhone = "0900000001";
        if (!taiKhoanRepository.existsByTenDangNhap(adminPhone)) {
            Nguoi nguoi = nguoiRepository.findBySoDienThoai(adminPhone).orElseGet(() -> {
                Nguoi n = Nguoi.builder()
                        .hoTen("Hệ thống Admin")
                        .soDienThoai(adminPhone)
                        .thoiDiemTao(OffsetDateTime.now())
                        .thoiDiemCapNhat(OffsetDateTime.now())
                        .build();
                return nguoiRepository.save(n);
            });

            if (!nhanVienRepository.existsById(nguoi.getMaNguoi())) {
                NhanVien staff = NhanVien.builder()
                        .maNhanVien(nguoi.getMaNguoi())
                        .nguoi(nguoi)
                        .maChiNhanh(defaultBranch.getMaChiNhanh())
                        .build();
                nhanVienRepository.save(staff);
            }

            VaiTro adminRole = vaiTroRepository.findByTenVaiTro("admin")
                    .orElseGet(() -> vaiTroRepository.findById(1L).orElse(null));

            TaiKhoan taiKhoan = TaiKhoan.builder()
                    .nguoi(nguoi)
                    .vaiTro(adminRole)
                    .tenDangNhap(adminPhone)
                    .matKhauMaHoa(passwordEncoder.encode("Admin@123"))
                    .trangThaiHoatDong(true)
                    .thoiDiemTao(OffsetDateTime.now())
                    .thoiDiemCapNhat(OffsetDateTime.now())
                    .build();
            taiKhoanRepository.save(taiKhoan);
            log.info("Đã khởi tạo tài khoản Admin mặc định: {}", adminPhone);
        }
    }

    private void ensurePatientAccount() {
        String patientPhone = "0912345678";
        if (!taiKhoanRepository.existsByTenDangNhap(patientPhone)) {
            Nguoi nguoi = nguoiRepository.findBySoDienThoai(patientPhone).orElseGet(() -> {
                Nguoi n = Nguoi.builder()
                        .hoTen("Nguyễn Văn A")
                        .soDienThoai(patientPhone)
                        .ngaySinh(LocalDate.of(1990, 1, 1))
                        .gioiTinh("Nam")
                        .diaChiSoNhaThonPho("123 Đường ABC, Phường 1, Quận 1, TP.HCM")
                        .thoiDiemTao(OffsetDateTime.now())
                        .thoiDiemCapNhat(OffsetDateTime.now())
                        .build();
                return nguoiRepository.save(n);
            });

            if (!benhNhanRepository.existsById(nguoi.getMaNguoi())) {
                BenhNhan patient = BenhNhan.builder()
                        .maBenhNhan(nguoi.getMaNguoi())
                        .danToc("Kinh")
                        .ngheNghiep("Khac")
                        .bhytSoThe("DN4010123456789")
                        .build();
                benhNhanRepository.save(patient);
            }

            VaiTro patientRole = vaiTroRepository.findByTenVaiTro("benh_nhan")
                    .orElseGet(() -> vaiTroRepository.findById(4L).orElse(null));

            TaiKhoan taiKhoan = TaiKhoan.builder()
                    .nguoi(nguoi)
                    .vaiTro(patientRole)
                    .tenDangNhap(patientPhone)
                    .matKhauMaHoa(passwordEncoder.encode("Patient@123"))
                    .trangThaiHoatDong(true)
                    .thoiDiemTao(OffsetDateTime.now())
                    .thoiDiemCapNhat(OffsetDateTime.now())
                    .build();
            taiKhoanRepository.save(taiKhoan);
            log.info("Đã khởi tạo tài khoản Bệnh nhân mặc định: {}", patientPhone);
        }
    }
}
