package com.example.drmayday.features.admin.service.impl;

import com.example.drmayday.features.admin.dto.AssignUserRequest;
import com.example.drmayday.features.admin.dto.CreatePatientRequest;
import com.example.drmayday.features.admin.dto.CreatePersonRequest;
import com.example.drmayday.features.admin.dto.CreateStaffRequest;
import com.example.drmayday.features.admin.dto.CreateUserRequest;
import com.example.drmayday.features.admin.dto.CreatedUserResponse;
import com.example.drmayday.features.admin.dto.EditUserRequest;
import com.example.drmayday.features.admin.dto.StaffAssignResponse;
import com.example.drmayday.features.admin.dto.UserDetailsResponse;
import com.example.drmayday.features.admin.dto.UserResponse;
import com.example.drmayday.entity.Branch;
import com.example.drmayday.entity.Patient;
import com.example.drmayday.entity.Person;
import com.example.drmayday.entity.Employee;
import com.example.drmayday.entity.UserAccount;
import com.example.drmayday.entity.Role;
import com.example.drmayday.features.admin.service.AdminUserService;
import com.example.drmayday.features.profile.dto.EmployeeProfileResponse;
import com.example.drmayday.features.profile.dto.PatientProfileResponse;
import com.example.drmayday.features.profile.dto.PersonProfileResponse;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

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
        log.info("Admin requested the user list: page={}, limit={}", page, limit);
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
        log.info("Admin requested user details: id={}", id);
        Long userId = parseUserId(id);

        UserAccount userAccount = userAccountRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_ACCOUNT_NOT_FOUND));

        return mapToUserDetailsResponse(userAccount);
    }

    @Override
    @Transactional
    public CreatedUserResponse createUser(CreateUserRequest request) {
        if (request == null || request.personId() == null || request.roleId() == null
                || request.username() == null || request.username().isBlank()
                || request.password() == null || request.password().isBlank()) {
            throw new AppException(ErrorCode.MISSING_REQUIRED_PARAMETER);
        }

        Person person = personRepository.findById(request.personId())
                .orElseThrow(() -> new AppException(ErrorCode.PERSON_NOT_FOUND));
        if (userAccountRepository.existsByPerson_PersonId(person.getPersonId())) {
            throw new AppException(ErrorCode.PERSON_ALREADY_HAS_USER_ACCOUNT);
        }

        Role role = roleRepository.findById(request.roleId())
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_PARAMETER_VALUE));
        String username = request.username().trim();
        if (username.length() > 100 || userAccountRepository.existsByUsernameIgnoreCase(username)) {
            throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE, "Tên đăng nhập không hợp lệ");
        }

        OffsetDateTime now = OffsetDateTime.now();
        UserAccount userAccount = UserAccount.builder()
                .person(person)
                .role(role)
                .username(username)
                .passwordHash(passwordEncoder.encode(request.password()))
                .active(request.isActive() == null || request.isActive())
                .createdAt(now)
                .updatedAt(now)
                .build();

        try {
            return CreatedUserResponse.from(userAccountRepository.saveAndFlush(userAccount));
        } catch (DataIntegrityViolationException exception) {
            if (hasConstraint(exception, "uq_tai_khoan_nguoi")) {
                throw new AppException(ErrorCode.PERSON_ALREADY_HAS_USER_ACCOUNT);
            }
            if (hasConstraint(exception, "uq_tai_khoan_ten_dang_nhap")) {
                throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE, "Tên đăng nhập không hợp lệ");
            }
            throw exception;
        }
    }

    @Override
    @Transactional
    public PersonProfileResponse createPerson(CreatePersonRequest request) {
        if (request == null || request.fullName() == null || request.fullName().isBlank()
                || request.phoneNumber() == null || request.phoneNumber().isBlank()) {
            throw new AppException(ErrorCode.MISSING_REQUIRED_PARAMETER);
        }

        OffsetDateTime now = OffsetDateTime.now();
        Person person = Person.builder()
                .fullName(request.fullName().trim())
                .phoneNumber(request.phoneNumber())
                .birthDate(request.dateOfBirth())
                .gender(request.gender())
                .provinceCity(request.provinceCity())
                .communeWard(request.wardCommune())
                .streetAddress(request.addressLine())
                .createdAt(now)
                .updatedAt(now)
                .build();
        return PersonProfileResponse.from(personRepository.saveAndFlush(person));
    }

    @Override
    @Transactional
    public PatientProfileResponse createPatient(CreatePatientRequest request) {
        if (request == null || request.personId() == null) {
            throw new AppException(ErrorCode.MISSING_REQUIRED_PARAMETER);
        }

        Person person = personRepository.findById(request.personId())
                .orElseThrow(() -> new AppException(ErrorCode.PERSON_NOT_FOUND));
        if (patientRepository.existsById(person.getPersonId())) {
            throw new AppException(ErrorCode.PERSON_ALREADY_REGISTERED_AS_PATIENT);
        }

        String hisCode = trimToNull(request.hisCode());
        if (hisCode != null && patientRepository.existsByHisCode(hisCode)) {
            throw new AppException(ErrorCode.PATIENT_HIS_CODE_ALREADY_EXISTS);
        }

        Patient patient = Patient.builder()
                .person(person)
                .hisCode(hisCode)
                .occupation(request.occupation())
                .ethnicity(trimToNull(request.ethnicity()))
                .healthInsuranceNumber(trimToNull(request.healthInsuranceNumber()))
                .healthInsuranceValidUntil(request.healthInsuranceValidUntil())
                .emergencyContactNameAndAddress(trimToNull(request.emergencyContactNameAddress()))
                .emergencyContactPhone(trimToNull(request.emergencyContactPhone()))
                .build();
        try {
            return PatientProfileResponse.from(patientRepository.saveAndFlush(patient));
        } catch (DataIntegrityViolationException exception) {
            if (hasConstraint(exception, "uq_benh_nhan_ma_his")) {
                throw new AppException(ErrorCode.PATIENT_HIS_CODE_ALREADY_EXISTS);
            }
            if (hasConstraint(exception, "pk_benh_nhan")) {
                throw new AppException(ErrorCode.PERSON_ALREADY_REGISTERED_AS_PATIENT);
            }
            throw exception;
        }
    }

    @Override
    @Transactional
    public EmployeeProfileResponse createStaff(CreateStaffRequest request) {
        if (request == null || request.personId() == null || request.branchId() == null) {
            throw new AppException(ErrorCode.MISSING_REQUIRED_PARAMETER);
        }

        Person person = personRepository.findById(request.personId())
                .orElseThrow(() -> new AppException(ErrorCode.PERSON_NOT_FOUND));
        if (!branchRepository.existsById(request.branchId())) {
            throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE);
        }
        if (employeeRepository.existsById(person.getPersonId())) {
            throw new AppException(ErrorCode.PERSON_ALREADY_REGISTERED_AS_EMPLOYEE);
        }

        Employee employee = Employee.builder()
                .person(person)
                .branchId(request.branchId())
                .build();
        try {
            return EmployeeProfileResponse.from(employeeRepository.saveAndFlush(employee));
        } catch (DataIntegrityViolationException exception) {
            if (hasConstraint(exception, "pk_nhan_vien")) {
                throw new AppException(ErrorCode.PERSON_ALREADY_REGISTERED_AS_EMPLOYEE);
            }
            throw exception;
        }
    }

    @Override
    @Transactional
    public UserResponse editUser(String id, EditUserRequest request) {
        log.info("Admin updating a user: id={}", id);
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

    @Override
    @Transactional
    public void deleteUser(String id) {
        log.info("Admin deleting a user: id={}", id);
        Long userId = parseUserId(id);

        UserAccount userAccount = userAccountRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_ACCOUNT_NOT_FOUND));

        userAccountRepository.delete(userAccount);
        userAccountRepository.flush();
    }

    @Override
    @Transactional
    public StaffAssignResponse assignUser(AssignUserRequest request) {
        if (request == null || request.getUserId() == null || request.getBranchId() == null) {
            throw new AppException(ErrorCode.MISSING_REQUIRED_PARAMETER);
        }

        UserAccount account = userAccountRepository.findById(request.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_ACCOUNT_NOT_FOUND));
        if (account.getPerson() == null) {
            throw new AppException(ErrorCode.USER_ACCOUNT_NOT_FOUND);
        }

        Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_PARAMETER_VALUE, "Chi nhánh không tồn tại"));
        if (!Boolean.TRUE.equals(branch.getActive())) {
            throw new AppException(ErrorCode.BRANCH_INACTIVE);
        }

        Employee employee = employeeRepository.findById(account.getPerson().getPersonId())
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_PARAMETER_VALUE, "Người dùng không phải nhân viên y tế"));

        employee.setBranchId(branch.getBranchId());
        employeeRepository.saveAndFlush(employee);

        return StaffAssignResponse.builder()
                .staffId(String.valueOf(employee.getEmployeeId()))
                .userId(String.valueOf(account.getAccountId()))
                .branchId(String.valueOf(branch.getBranchId()))
                .branchName(branch.getName())
                .build();
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

    private Long parseUserId(String id) {
        try {
            return Long.parseLong(id);
        } catch (NumberFormatException ex) {
            throw new AppException(ErrorCode.USER_ACCOUNT_NOT_FOUND);
        }
    }

    private Role resolveRole(String roleIdentifier) {
        if (roleIdentifier == null || roleIdentifier.trim().isEmpty()) {
            throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE, "Vai trò không tồn tại");
        }
        String trimmed = roleIdentifier.trim();
        try {
            long roleId = Long.parseLong(trimmed);
            return roleRepository.findById(roleId)
                    .orElseThrow(() -> new AppException(ErrorCode.INVALID_PARAMETER_VALUE, "Vai trò không tồn tại"));
        } catch (NumberFormatException e) {
            String roleName = trimmed.toLowerCase();
            if ("admin".equals(roleName)) {
                return roleRepository.findByName("admin")
                        .orElseThrow(() -> new AppException(ErrorCode.INVALID_PARAMETER_VALUE, "Vai trò không tồn tại"));
            }
            if ("doctor".equals(roleName) || "bac_si".equals(roleName)) {
                return roleRepository.findByName("bac_si")
                        .orElseThrow(() -> new AppException(ErrorCode.INVALID_PARAMETER_VALUE, "Vai trò không tồn tại"));
            }
            if ("nurse".equals(roleName) || "dieu_duong".equals(roleName)) {
                return roleRepository.findByName("dieu_duong")
                        .orElseThrow(() -> new AppException(ErrorCode.INVALID_PARAMETER_VALUE, "Vai trò không tồn tại"));
            }
            if ("patient".equals(roleName) || "benh_nhan".equals(roleName)) {
                return roleRepository.findByName("benh_nhan")
                        .orElseThrow(() -> new AppException(ErrorCode.INVALID_PARAMETER_VALUE, "Vai trò không tồn tại"));
            }
            return roleRepository.findByName(roleName)
                    .orElseThrow(() -> new AppException(ErrorCode.INVALID_PARAMETER_VALUE, "Vai trò không tồn tại"));
        }
    }

    private String formatOffsetDateTime(OffsetDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATE_TIME_FORMATTER) : null;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean hasConstraint(Throwable throwable, String constraintName) {
        for (Throwable cause = throwable; cause != null; cause = cause.getCause()) {
            String message = cause.getMessage();
            if (message != null && message.toLowerCase(Locale.ROOT).contains(constraintName)) {
                return true;
            }
        }
        return false;
    }
}
