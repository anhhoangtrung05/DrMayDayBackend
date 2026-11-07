package com.example.drmayday.features.authorization.config;

import com.example.drmayday.entity.Patient;
import com.example.drmayday.entity.Branch;
import com.example.drmayday.entity.Person;
import com.example.drmayday.entity.Employee;
import com.example.drmayday.entity.UserAccount;
import com.example.drmayday.entity.Role;
import com.example.drmayday.entity.Permission;
import com.example.drmayday.repository.PatientRepository;
import com.example.drmayday.repository.BranchRepository;
import com.example.drmayday.repository.PersonRepository;
import com.example.drmayday.repository.EmployeeRepository;
import com.example.drmayday.repository.UserAccountRepository;
import com.example.drmayday.repository.RoleRepository;
import com.example.drmayday.repository.PermissionRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseSeeder.class);

    private final JdbcTemplate jdbcTemplate;
    private final BranchRepository chiNhanhRepository;
    private final RoleRepository vaiTroRepository;
    private final PersonRepository nguoiRepository;
    private final PatientRepository benhNhanRepository;
    private final EmployeeRepository nhanVienRepository;
    private final UserAccountRepository taiKhoanRepository;
    private final PermissionRepository quyenHanRepository;
    private final PasswordEncoder passwordEncoder;

    public DatabaseSeeder(JdbcTemplate jdbcTemplate,
                          BranchRepository chiNhanhRepository,
                          RoleRepository vaiTroRepository,
                          PersonRepository nguoiRepository,
                          PatientRepository benhNhanRepository,
                          EmployeeRepository nhanVienRepository,
                          UserAccountRepository taiKhoanRepository,
                          PermissionRepository quyenHanRepository,
                          PasswordEncoder passwordEncoder) {
        this.jdbcTemplate = jdbcTemplate;
        this.chiNhanhRepository = chiNhanhRepository;
        this.vaiTroRepository = vaiTroRepository;
        this.nguoiRepository = nguoiRepository;
        this.benhNhanRepository = benhNhanRepository;
        this.nhanVienRepository = nhanVienRepository;
        this.taiKhoanRepository = taiKhoanRepository;
        this.quyenHanRepository = quyenHanRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        try {
            initTokenBlacklistTable();
            initUserTokenVersionsTable();
            initPasswordResetTokensTable();
            Branch defaultBranch = ensureDefaultBranch();
            ensureRoles();
            ensurePermissions();
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

    private void initPasswordResetTokensTable() {
        try {
            jdbcTemplate.execute(
                    "CREATE TABLE IF NOT EXISTS password_reset_tokens (" +
                    "password_reset_token_id BIGSERIAL PRIMARY KEY, " +
                    "user_id BIGINT NOT NULL UNIQUE, " +
                    "token_hash VARCHAR(100) NOT NULL, " +
                    "expires_at TIMESTAMP NOT NULL, " +
                    "created_at TIMESTAMP NOT NULL, " +
                    "updated_at TIMESTAMP NOT NULL" +
                    ")"
            );
        } catch (Exception ex) {
            log.error("Không thể khởi tạo bảng password_reset_tokens", ex);
        }
    }

    private void initUserTokenVersionsTable() {
        try {
            jdbcTemplate.execute(
                    "CREATE TABLE IF NOT EXISTS user_token_versions (" +
                    "user_id BIGINT PRIMARY KEY, " +
                    "token_version BIGINT NOT NULL DEFAULT 0, " +
                    "updated_at TIMESTAMP NOT NULL" +
                    ")"
            );
        } catch (Exception ex) {
            log.error("Không thể khởi tạo bảng user_token_versions", ex);
        }
    }

    private Branch ensureDefaultBranch() {
        return chiNhanhRepository.findFirstByOrderByBranchIdAsc().orElseGet(() -> {
            Branch branch = new Branch();
            branch.setName("Phòng khám Đa khoa DrMayDay - Cơ sở chính");
            branch.setAddress("123 Đường Giải Phóng, Hà Nội");
            branch.setPhoneNumber("02438888888");
            branch.setActive(true);
            return chiNhanhRepository.save(branch);
        });
    }

    private void ensureRoles() {
        if (!vaiTroRepository.existsByName("admin")) {
            vaiTroRepository.save(new Role(1L, "admin", "Quản trị viên", "Toàn quyền quản trị hệ thống", OffsetDateTime.now(), OffsetDateTime.now()));
        }
        if (!vaiTroRepository.existsByName("bac_si")) {
            vaiTroRepository.save(new Role(2L, "bac_si", "Bác sĩ", "Bác sĩ điều trị", OffsetDateTime.now(), OffsetDateTime.now()));
        }
        if (!vaiTroRepository.existsByName("dieu_duong")) {
            vaiTroRepository.save(new Role(3L, "dieu_duong", "Điều dưỡng", "Điều dưỡng viên hỗ trợ", OffsetDateTime.now(), OffsetDateTime.now()));
        }
        if (!vaiTroRepository.existsByName("benh_nhan")) {
            vaiTroRepository.save(new Role(4L, "benh_nhan", "Bệnh nhân", "Bệnh nhân sử dụng ứng dụng", OffsetDateTime.now(), OffsetDateTime.now()));
        }
    }

    private void ensurePermissions() {
        Set<String> existingNames = quyenHanRepository.findAll().stream()
                .map(Permission::getName)
                .collect(Collectors.toSet());

        if (!existingNames.contains("manage_users_v1")) {
            quyenHanRepository.save(Permission.builder()
                    .name("manage_users_v1")
                    .permissionGroup("USER_MANAGEMENT")
                    .description("Quản lý người dùng hệ thống")
                    .createdAt(OffsetDateTime.now())
                    .updatedAt(OffsetDateTime.now())
                    .build());
        }
        if (!existingNames.contains("manage_clinical")) {
            quyenHanRepository.save(Permission.builder()
                    .name("manage_clinical")
                    .permissionGroup("CLINICAL")
                    .description("Quản lý hồ sơ bệnh án")
                    .createdAt(OffsetDateTime.now())
                    .updatedAt(OffsetDateTime.now())
                    .build());
        }
    }

    private void ensureAdminAccount(Branch defaultBranch) {
        String adminPhone = "0900000001";
        if (!taiKhoanRepository.existsByUsername(adminPhone)) {
            Person nguoi = nguoiRepository.findByPhoneNumber(adminPhone).orElseGet(() -> {
                Person n = Person.builder()
                        .fullName("Hệ thống Admin")
                        .phoneNumber(adminPhone)
                        .createdAt(OffsetDateTime.now())
                        .updatedAt(OffsetDateTime.now())
                        .build();
                return nguoiRepository.save(n);
            });

            if (!nhanVienRepository.existsById(nguoi.getPersonId())) {
                Employee staff = Employee.builder()
                        .employeeId(nguoi.getPersonId())
                        .branchId(defaultBranch.getBranchId())
                        .build();
                nhanVienRepository.save(staff);
            }

            Role adminRole = vaiTroRepository.findByName("admin")
                    .orElseGet(() -> vaiTroRepository.findById(1L).orElse(null));

            UserAccount taiKhoan = UserAccount.builder()
                    .person(nguoi)
                    .role(adminRole)
                    .username(adminPhone)
                    .passwordHash(passwordEncoder.encode("Admin@123"))
                    .active(true)
                    .createdAt(OffsetDateTime.now())
                    .updatedAt(OffsetDateTime.now())
                    .build();
            taiKhoanRepository.save(taiKhoan);
            log.info("Đã khởi tạo tài khoản Admin mặc định: {}", adminPhone);
        }
    }

    private void ensurePatientAccount() {
        String patientPhone = "0912345678";
        if (!taiKhoanRepository.existsByUsername(patientPhone)) {
            Person nguoi = nguoiRepository.findByPhoneNumber(patientPhone).orElseGet(() -> {
                Person n = Person.builder()
                        .fullName("Nguyễn Văn A")
                        .phoneNumber(patientPhone)
                        .birthDate(LocalDate.of(1990, 1, 1))
                        .gender("Nam")
                        .streetAddress("123 Đường ABC, Phường 1, Quận 1, TP.HCM")
                        .createdAt(OffsetDateTime.now())
                        .updatedAt(OffsetDateTime.now())
                        .build();
                return nguoiRepository.save(n);
            });

            if (!benhNhanRepository.existsById(nguoi.getPersonId())) {
                Patient patient = Patient.builder()
                        .patientId(nguoi.getPersonId())
                        .ethnicity("Kinh")
                        .occupation("Khac")
                        .healthInsuranceNumber("DN4010123456789")
                        .build();
                benhNhanRepository.save(patient);
            }

            Role patientRole = vaiTroRepository.findByName("benh_nhan")
                    .orElseGet(() -> vaiTroRepository.findById(4L).orElse(null));

            UserAccount taiKhoan = UserAccount.builder()
                    .person(nguoi)
                    .role(patientRole)
                    .username(patientPhone)
                    .passwordHash(passwordEncoder.encode("Patient@123"))
                    .active(true)
                    .createdAt(OffsetDateTime.now())
                    .updatedAt(OffsetDateTime.now())
                    .build();
            taiKhoanRepository.save(taiKhoan);
            log.info("Đã khởi tạo tài khoản Bệnh nhân mặc định: {}", patientPhone);
        }
    }
}