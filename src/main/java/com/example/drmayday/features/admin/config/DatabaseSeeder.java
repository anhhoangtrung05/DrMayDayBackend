package com.example.drmayday.features.admin.config;

import com.example.drmayday.entity.Patient;
import com.example.drmayday.entity.Branch;
import com.example.drmayday.entity.Person;
import com.example.drmayday.entity.Employee;
import com.example.drmayday.entity.UserAccount;
import com.example.drmayday.entity.Role;
import com.example.drmayday.repository.PatientRepository;
import com.example.drmayday.repository.BranchRepository;
import com.example.drmayday.repository.PersonRepository;
import com.example.drmayday.repository.EmployeeRepository;
import com.example.drmayday.repository.UserAccountRepository;
import com.example.drmayday.repository.RoleRepository;

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
    private final BranchRepository branchRepository;
    private final RoleRepository roleRepository;
    private final PersonRepository personRepository;
    private final PatientRepository patientRepository;
    private final EmployeeRepository employeeRepository;
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    public DatabaseSeeder(JdbcTemplate jdbcTemplate,
                          BranchRepository branchRepository,
                          RoleRepository roleRepository,
                          PersonRepository personRepository,
                          PatientRepository patientRepository,
                          EmployeeRepository employeeRepository,
                          UserAccountRepository userAccountRepository,
                          PasswordEncoder passwordEncoder) {
        this.jdbcTemplate = jdbcTemplate;
        this.branchRepository = branchRepository;
        this.roleRepository = roleRepository;
        this.personRepository = personRepository;
        this.patientRepository = patientRepository;
        this.employeeRepository = employeeRepository;
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        try {
            initUserTokenVersionsTable();
            Branch defaultBranch = ensureDefaultBranch();
            ensureRoles();
            ensureAdminAccount(defaultBranch);
            ensurePatientAccount();
            log.info("DatabaseSeeder hoàn tất khởi tạo dữ liệu mẫu thành công.");
        } catch (Exception ex) {
            log.warn("Lỗi trong quá trình DatabaseSeeder: {}", ex.getMessage());
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
            log.info("Đã khởi tạo tài khoản Admin mặc định: {}", adminPhone);
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
            log.info("Đã khởi tạo tài khoản Bệnh nhân mặc định: {}", patientPhone);
        }
    }
}
