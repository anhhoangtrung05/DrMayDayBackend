package com.example.drmayday.features.admin.service.impl;

import com.example.drmayday.features.admin.dto.CreateUserRequest;
import com.example.drmayday.features.admin.dto.EditUserRequest;
import com.example.drmayday.features.admin.dto.UserDetailsResponse;
import com.example.drmayday.features.admin.dto.UserResponse;
import com.example.drmayday.entity.Patient;
import com.example.drmayday.entity.Person;
import com.example.drmayday.entity.Employee;
import com.example.drmayday.entity.UserAccount;
import com.example.drmayday.entity.Role;
import com.example.drmayday.features.admin.service.AdminUserService;
import com.example.drmayday.repository.PatientRepository;
import com.example.drmayday.repository.BranchRepository;
import com.example.drmayday.repository.PersonRepository;
import com.example.drmayday.repository.EmployeeRepository;
import com.example.drmayday.repository.UserAccountRepository;
import com.example.drmayday.repository.RoleRepository;
import com.example.drmayday.common.exception.AppException;
import com.example.drmayday.common.exception.ErrorCode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class AdminUserServiceImpl implements AdminUserService {

    private static final Logger log = LoggerFactory.getLogger(AdminUserServiceImpl.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final UserAccountRepository userAccountRepository;
    private final PersonRepository personRepository;
    private final PatientRepository patientRepository;
    private final EmployeeRepository employeeRepository;
    private final RoleRepository roleRepository;
    private final BranchRepository branchRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminUserServiceImpl(UserAccountRepository userAccountRepository,
                                PersonRepository personRepository,
                                PatientRepository patientRepository,
                                EmployeeRepository employeeRepository,
                                RoleRepository roleRepository,
                                BranchRepository branchRepository,
                                PasswordEncoder passwordEncoder) {
        this.userAccountRepository = userAccountRepository;
        this.personRepository = personRepository;
        this.patientRepository = patientRepository;
        this.employeeRepository = employeeRepository;
        this.roleRepository = roleRepository;
        this.branchRepository = branchRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> getListUsers(int page, int limit) {
        log.info("Admin truy xuất danh sách người dùng: page={}, limit={}", page, limit);
        int pageIndex = Math.max(page - 1, 0);
        int pageSize = limit > 0 ? limit : 20;
        
        
        Pageable pageable = PageRequest.of(
            pageIndex,
            pageSize,
            Sort.by(Sort.Direction.DESC, "accountId")
        );
        
        Page<UserAccount> accountsPage = userAccountRepository.findAll(pageable);
        
        return accountsPage.map(this::mapToUserResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetailsResponse getUserDetails(String id) {
        log.info("Admin truy xuất chi tiết người dùng: id={}", id);
        Long userId = parseUserId(id);

        UserAccount userAccount = userAccountRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_ACCOUNT_NOT_FOUND));

        return mapToUserDetailsResponse(userAccount);
    }

    @Override
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        log.info("Admin tạo mới người dùng: phone={}, role_id={}", request.getPhone(), request.getRoleId());
        String phone = normalizePhone(request.getPhone());

        if (userAccountRepository.existsByUsername(phone) || personRepository.existsByPhoneNumber(phone)) {
            throw new AppException(ErrorCode.USER_PHONE_ALREADY_EXISTS);
        }

        Role role = resolveRole(request.getRoleId());
        boolean active = !"0".equals(request.getIsActive()) && !"false".equalsIgnoreCase(request.getIsActive());

        OffsetDateTime creationTime = OffsetDateTime.now().minusSeconds(10);

        Person person = Person.builder()
                .fullName(request.getFullName().trim())
                .phoneNumber(phone)
                .createdAt(creationTime)
                .updatedAt(creationTime)
                .build();
        person = personRepository.save(person);

        if ("benh_nhan".equalsIgnoreCase(role.getName()) || Long.valueOf(4L).equals(role.getRoleId())) {
            Patient patient = Patient.builder()
                    .person(person)
                    .build();
            patientRepository.save(patient);
        } else {
            Long branchId = branchRepository.findFirstByOrderByBranchIdAsc()
                    .map(Branch -> Branch == null ? null : Branch.getBranchId())
                    .orElse(1L);
            Employee employee = Employee.builder()
                    .branchId(branchId)
                    .person(person)
                    .build();
            employeeRepository.save(employee);
        }

        UserAccount userAccount = UserAccount.builder()
                .person(person)
                .role(role)
                .username(phone)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .active(active)
                .createdAt(creationTime)
                .updatedAt(creationTime)
                .build();
        userAccount = userAccountRepository.save(userAccount);

        return mapToUserResponse(userAccount);
    }

    @Override
    @Transactional
    public UserResponse editUser(String id, EditUserRequest request) {
        log.info("Admin chỉnh sửa người dùng: id={}", id);
        Long userId = parseUserId(id);

        UserAccount userAccount = userAccountRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_ACCOUNT_NOT_FOUND));

        if (request.getFullName() != null && !request.getFullName().trim().isEmpty()) {
            if (userAccount.getPerson() != null) {
                userAccount.getPerson().setFullName(request.getFullName().trim());
                OffsetDateTime now = OffsetDateTime.now();
                if (userAccount.getPerson().getCreatedAt() != null && now.isBefore(userAccount.getPerson().getCreatedAt())) {
                    now = userAccount.getPerson().getCreatedAt();
                }
                userAccount.getPerson().setUpdatedAt(now);
                personRepository.save(userAccount.getPerson());
            }
        }

        if (request.getIsActive() != null && !request.getIsActive().trim().isEmpty()) {
            boolean active = "1".equals(request.getIsActive().trim()) || "true".equalsIgnoreCase(request.getIsActive().trim());
            userAccount.setActive(active);
        }

        if (request.getRoleId() != null && !request.getRoleId().trim().isEmpty()) {
            Role role = resolveRole(request.getRoleId().trim());
            userAccount.setRole(role);

            if (userAccount.getPerson() != null) {
                Long personId = userAccount.getPerson().getPersonId();
                if ("benh_nhan".equalsIgnoreCase(role.getName()) || Long.valueOf(4L).equals(role.getRoleId())) {
                    if (!patientRepository.existsById(personId)) {
                        Patient bn = Patient.builder()
                                .person(userAccount.getPerson())
                                .build();
                        patientRepository.save(bn);
                    }
                } else {
                    if (!employeeRepository.existsById(personId)) {
                        Long branchId = branchRepository.findFirstByOrderByBranchIdAsc()
                            .map(Branch -> Branch.getBranchId())
                                .orElse(1L);
                        Employee nv = Employee.builder()
                                .branchId(branchId)
                                .person(userAccount.getPerson())
                                .build();
                        employeeRepository.save(nv);
                    }
                }
            }
        }

        OffsetDateTime nowUserAccount = OffsetDateTime.now();
        if (userAccount.getCreatedAt() != null && nowUserAccount.isBefore(userAccount.getCreatedAt())) {
            nowUserAccount = userAccount.getCreatedAt();
        }
        userAccount.setUpdatedAt(nowUserAccount);
        userAccount = userAccountRepository.save(userAccount);

        return mapToUserResponse(userAccount);
    }

    private UserResponse mapToUserResponse(UserAccount userAccount) {
        Person nguoi = userAccount.getPerson();
        Role role = userAccount.getRole();

        return UserResponse.builder()
                .userId(String.valueOf(userAccount.getAccountId()))
                .fullName(nguoi != null ? nguoi.getFullName() : "")
                .phone(userAccount.getUsername())
                .roleId(role != null ? String.valueOf(role.getRoleId()) : "4")
                .isActive(Boolean.TRUE.equals(userAccount.getActive()) ? "1" : "0")
                .createdAt(formatOffsetDateTime(userAccount.getCreatedAt()))
                .updatedAt(formatOffsetDateTime(userAccount.getUpdatedAt()))
                .build();
    }

    private UserDetailsResponse mapToUserDetailsResponse(UserAccount userAccount) {
        Person nguoi = userAccount.getPerson();
        Role role = userAccount.getRole();

        return UserDetailsResponse.builder()
                .userId(String.valueOf(userAccount.getAccountId()))
                .fullName(nguoi != null ? nguoi.getFullName() : "")
                .phone(userAccount.getUsername())
                .roleId(role != null ? String.valueOf(role.getRoleId()) : "4")
                .isActive(Boolean.TRUE.equals(userAccount.getActive()) ? "1" : "0")
                .createdAt(formatOffsetDateTime(userAccount.getCreatedAt()))
                .updatedAt(formatOffsetDateTime(userAccount.getUpdatedAt()))
                .build();
    }

    /**
     * Chuẩn hóa +84xxxxxxxxx về 0xxxxxxxxx, vì nguoi.so_dien_thoai chỉ nhận đúng 10 chữ số
     * và một số điện thoại chỉ được ứng với một tài khoản.
     */
    private String normalizePhone(String phone) {
        String trimmed = phone.trim();
        return trimmed.startsWith("+84") ? "0" + trimmed.substring(3) : trimmed;
    }

    private Long parseUserId(String id) {
        try {
            return Long.parseLong(id);
        } catch (NumberFormatException ex) {
            throw new AppException(ErrorCode.USER_ACCOUNT_NOT_FOUND);
        }
    }

    private Role resolveRole(String roleIdentifier) {
        if (roleIdentifier == null || roleIdentifier.trim().isEmpty()) {
            throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE, "Vai trò (Role) không tồn tại");
        }
        String trimmed = roleIdentifier.trim();
        try {
            long roleId = Long.parseLong(trimmed);
            return roleRepository.findById(roleId)
                    .orElseThrow(() -> new AppException(ErrorCode.INVALID_PARAMETER_VALUE, "Vai trò (Role) không tồn tại"));
        } catch (NumberFormatException e) {
            String roleName = trimmed.toLowerCase();
            if ("admin".equals(roleName)) {
                return roleRepository.findByName("admin")
                        .orElseThrow(() -> new AppException(ErrorCode.INVALID_PARAMETER_VALUE, "Vai trò (Role) không tồn tại"));
            }
            if ("doctor".equals(roleName) || "bac_si".equals(roleName)) {
                return roleRepository.findByName("bac_si")
                        .orElseThrow(() -> new AppException(ErrorCode.INVALID_PARAMETER_VALUE, "Vai trò (Role) không tồn tại"));
            }
            if ("nurse".equals(roleName) || "dieu_duong".equals(roleName)) {
                return roleRepository.findByName("dieu_duong")
                        .orElseThrow(() -> new AppException(ErrorCode.INVALID_PARAMETER_VALUE, "Vai trò (Role) không tồn tại"));
            }
            if ("patient".equals(roleName) || "benh_nhan".equals(roleName)) {
                return roleRepository.findByName("benh_nhan")
                        .orElseThrow(() -> new AppException(ErrorCode.INVALID_PARAMETER_VALUE, "Vai trò (Role) không tồn tại"));
            }
            return roleRepository.findByName(roleName)
                    .orElseThrow(() -> new AppException(ErrorCode.INVALID_PARAMETER_VALUE, "Vai trò (Role) không tồn tại"));
        }
    }

    private String formatOffsetDateTime(OffsetDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATE_TIME_FORMATTER) : null;
    }
}
