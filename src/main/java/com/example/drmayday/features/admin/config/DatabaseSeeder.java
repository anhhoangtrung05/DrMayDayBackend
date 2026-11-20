package com.example.drmayday.features.admin.config;

import com.example.drmayday.entity.Patient;
import com.example.drmayday.entity.Branch;
import com.example.drmayday.entity.Person;
import com.example.drmayday.entity.Employee;
import com.example.drmayday.entity.UserAccount;
import com.example.drmayday.entity.Role;
import com.example.drmayday.entity.Permission;
import com.example.drmayday.entity.Department;
import com.example.drmayday.repository.PatientRepository;
import com.example.drmayday.repository.BranchRepository;
import com.example.drmayday.repository.PersonRepository;
import com.example.drmayday.repository.EmployeeRepository;
import com.example.drmayday.repository.UserAccountRepository;
import com.example.drmayday.repository.RoleRepository;
import com.example.drmayday.repository.PermissionRepository;
import com.example.drmayday.repository.DepartmentRepository;

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
    private final BranchRepository branchRepository;
    private final RoleRepository roleRepository;
    private final PersonRepository personRepository;
    private final PatientRepository patientRepository;
    private final EmployeeRepository employeeRepository;
    private final UserAccountRepository userAccountRepository;
    private final PermissionRepository permissionRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;

    public DatabaseSeeder(JdbcTemplate jdbcTemplate,
                          BranchRepository branchRepository,
                          RoleRepository roleRepository,
                          PersonRepository personRepository,
                          PatientRepository patientRepository,
                          EmployeeRepository employeeRepository,
                          UserAccountRepository userAccountRepository,
                          PermissionRepository permissionRepository,
                          DepartmentRepository departmentRepository,
                          PasswordEncoder passwordEncoder) {
        this.jdbcTemplate = jdbcTemplate;
        this.branchRepository = branchRepository;
        this.roleRepository = roleRepository;
        this.personRepository = personRepository;
        this.patientRepository = patientRepository;
        this.employeeRepository = employeeRepository;
        this.userAccountRepository = userAccountRepository;
        this.permissionRepository = permissionRepository;
        this.departmentRepository = departmentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        try {
            initUserTokenVersionsTable();
            Branch defaultBranch = ensureDefaultBranch();
            ensureDefaultDepartment(defaultBranch);
            ensureRoles();
            ensurePermissions();
            ensureAdminAccount(defaultBranch);
            ensurePatientAccount();
            ensureSampleClinicalData();
            log.info("DatabaseSeeder completed sample data initialization successfully.");
        } catch (Exception ex) {
            log.warn("DatabaseSeeder failed during initialization: {}", ex.getMessage());
        }
    }

    private Department ensureDefaultDepartment(Branch branch) {
        return departmentRepository.findFirstByBranchIdOrderByDepartmentIdAsc(branch.getBranchId())
                .orElseGet(() -> departmentRepository.save(Department.builder()
                        .branchId(branch.getBranchId())
                        .name("Khoa Da lieu")
                        .build()));
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
            log.error("Unable to initialize the user_token_versions table", ex);
        }
    }

    private Branch ensureDefaultBranch() {
        return branchRepository.findFirstByOrderByBranchIdAsc().orElseGet(() -> {
            Branch branch = new Branch();
            branch.setName("Phòng khám Đa khoa DrMayDay - Cơ sở chính");
            branch.setAddress("123 Đường Giải Phóng, Hà Nội");
            branch.setPhoneNumber("02438888888");
            branch.setActive(true);
            return branchRepository.save(branch);
        });
    }

    private void ensureRoles() {
        if (!roleRepository.existsByName("admin")) {
            roleRepository.save(new Role(1L, "admin", "Quản trị viên", "Toàn quyền quản trị hệ thống", OffsetDateTime.now(), OffsetDateTime.now()));
        }
        if (!roleRepository.existsByName("bac_si")) {
            roleRepository.save(new Role(2L, "bac_si", "Bác sĩ", "Bác sĩ điều trị", OffsetDateTime.now(), OffsetDateTime.now()));
        }
        if (!roleRepository.existsByName("dieu_duong")) {
            roleRepository.save(new Role(3L, "dieu_duong", "Điều dưỡng", "Điều dưỡng viên hỗ trợ", OffsetDateTime.now(), OffsetDateTime.now()));
        }
        if (!roleRepository.existsByName("benh_nhan")) {
            roleRepository.save(new Role(4L, "benh_nhan", "Bệnh nhân", "Bệnh nhân sử dụng ứng dụng", OffsetDateTime.now(), OffsetDateTime.now()));
        }
    }

    private void ensureAdminAccount(Branch defaultBranch) {
        String adminPhone = "0900000001";
        if (!userAccountRepository.existsByUsername(adminPhone)) {
            Person nguoi = personRepository.findByPhoneNumber(adminPhone).orElseGet(() -> {
                Person n = Person.builder()
                        .fullName("Hệ thống Admin")
                        .phoneNumber(adminPhone)
                        .createdAt(OffsetDateTime.now())
                        .updatedAt(OffsetDateTime.now())
                        .build();
                return personRepository.save(n);
            });

            if (!employeeRepository.existsById(nguoi.getPersonId())) {
                Employee staff = Employee.builder()
                        .branchId(defaultBranch.getBranchId())
                        .person(nguoi)
                        .build();
                employeeRepository.save(staff);
            }

            Role adminRole = roleRepository.findByName("admin")
                    .orElseGet(() -> roleRepository.findById(1L).orElse(null));

            UserAccount taiKhoan = UserAccount.builder()
                    .person(nguoi)
                    .role(adminRole)
                    .username(adminPhone)
                    .passwordHash(passwordEncoder.encode("Admin@123"))
                    .active(true)
                    .createdAt(OffsetDateTime.now())
                    .updatedAt(OffsetDateTime.now())
                    .build();
            userAccountRepository.save(taiKhoan);
            log.info("Initialized the default admin account: {}", adminPhone);
        }
    }

    private void ensurePatientAccount() {
        String patientPhone = "0912345678";
        if (!userAccountRepository.existsByUsername(patientPhone)) {
            Person nguoi = personRepository.findByPhoneNumber(patientPhone).orElseGet(() -> {
                Person n = Person.builder()
                        .fullName("Nguyễn Văn A")
                        .phoneNumber(patientPhone)
                        .birthDate(LocalDate.of(1990, 1, 1))
                        .gender("Nam")
                        .streetAddress("123 Đường ABC, Phường 1, Quận 1, TP.HCM")
                        .createdAt(OffsetDateTime.now())
                        .updatedAt(OffsetDateTime.now())
                        .build();
                return personRepository.save(n);
            });

            if (!patientRepository.existsById(nguoi.getPersonId())) {
                Patient patient = Patient.builder()
                        .ethnicity("Kinh")
                        .occupation("Khac")
                        .healthInsuranceNumber("DN4010123456789")
                        .person(nguoi)
                        .build();
                patientRepository.save(patient);
            }

            Role patientRole = roleRepository.findByName("benh_nhan")
                    .orElseGet(() -> roleRepository.findById(4L).orElse(null));

            UserAccount taiKhoan = UserAccount.builder()
                    .person(nguoi)
                    .role(patientRole)
                    .username(patientPhone)
                    .passwordHash(passwordEncoder.encode("Patient@123"))
                    .active(true)
                    .createdAt(OffsetDateTime.now())
                    .updatedAt(OffsetDateTime.now())
                    .build();
            userAccountRepository.save(taiKhoan);
            log.info("Initialized the default patient account: {}", patientPhone);
        }
    }

    private void ensurePermissions() {
        Set<String> existingNames = permissionRepository.findAll().stream()
                .map(Permission::getName)
                .collect(Collectors.toSet());

        if (!existingNames.contains("manage_users_v1")) {
            permissionRepository.save(Permission.builder()
                    .name("manage_users_v1")
                    .permissionGroup("USER_MANAGEMENT")
                    .description("Quản lý người dùng hệ thống")
                    .createdAt(OffsetDateTime.now())
                    .updatedAt(OffsetDateTime.now())
                    .build());
        }
        if (!existingNames.contains("manage_clinical")) {
            permissionRepository.save(Permission.builder()
                    .name("manage_clinical")
                    .permissionGroup("CLINICAL")
                    .description("Quản lý hồ sơ bệnh án")
                    .createdAt(OffsetDateTime.now())
                    .updatedAt(OffsetDateTime.now())
                    .build());
        }
    }

    private void ensureSampleClinicalData() {
        try {
            Integer featureCount = jdbcTemplate.queryForObject("SELECT count(*) FROM dacdiemsanphu", Integer.class);
            if (featureCount == null || featureCount == 0) {
                jdbcTemplate.execute("INSERT INTO dacdiemsanphu (ten_dac_diem, mo_ta) VALUES " +
                        "('Hinh dang tron / bau duc', 'San phu tron hoac bau duc'), " +
                        "('Hinh dang dia ly', 'San phu da cung, vien nham nho'), " +
                        "('Mau do hong', 'San phu mau do hong')");
                log.info("Initialized the sample wheal feature catalogue.");
            }

            Integer visitCount = jdbcTemplate.queryForObject("SELECT count(*) FROM phienkham", Integer.class);
            if (visitCount == null || visitCount == 0) {
                Long patientId = jdbcTemplate.queryForObject("SELECT ma_benh_nhan FROM benhnhan LIMIT 1", Long.class);
                Long doctorId = jdbcTemplate.queryForObject("SELECT ma_nhan_vien FROM nhanvien LIMIT 1", Long.class);

                if (patientId != null && doctorId != null) {
                    Long capRecordId = jdbcTemplate.queryForObject(
                            "INSERT INTO hosobenhan (ma_benh_nhan, loai_benh_an, trang_thai) VALUES (?, 'CAP', 'in_progress') RETURNING ma_ho_so",
                            Long.class, patientId);
                    jdbcTemplate.update("INSERT INTO benhanmaydaycap (ma_ho_so, nhap_vien) VALUES (?, false)", capRecordId);
                    Long capVisitId = jdbcTemplate.queryForObject(
                            "INSERT INTO phienkham (ma_ho_so, ma_bac_si, loai_benh_an, ngay_kham, lan_kham_thu) VALUES (?, ?, 'CAP', CURRENT_DATE, 1) RETURNING ma_phien_kham",
                            Long.class, capRecordId, doctorId);
                    jdbcTemplate.update("INSERT INTO phienkhamcap (ma_phien_kham, co_san_phu, so_lan_non) VALUES (?, true, 0)", capVisitId);

                    Long manRecordId = jdbcTemplate.queryForObject(
                            "INSERT INTO hosobenhan (ma_benh_nhan, loai_benh_an, trang_thai) VALUES (?, 'MAN', 'in_progress') RETURNING ma_ho_so",
                            Long.class, patientId);
                    jdbcTemplate.update("INSERT INTO benhanmaydayman (ma_ho_so) VALUES (?)", manRecordId);
                    Long manVisitId = jdbcTemplate.queryForObject(
                            "INSERT INTO phienkham (ma_ho_so, ma_bac_si, loai_benh_an, ngay_kham, lan_kham_thu) VALUES (?, ?, 'MAN', CURRENT_DATE, 1) RETURNING ma_phien_kham",
                            Long.class, manRecordId, doctorId);
                    jdbcTemplate.update("INSERT INTO phienkhamman (ma_phien_kham, hien_tai_co_san_phu) VALUES (?, true)", manVisitId);

                    log.info("Initialized sample records and visits: CAP exam_id={}, MAN exam_id={}", capVisitId, manVisitId);
                }
            }
        } catch (Exception ex) {
            log.debug("Skipped sample clinical data initialization: {}", ex.getMessage());
        }
    }
}
