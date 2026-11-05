package com.example.drmayday.features.authorization.service.impl;

import com.example.drmayday.features.authorization.dto.CreateUserRequest;
import com.example.drmayday.features.authorization.dto.EditUserRequest;
import com.example.drmayday.features.authorization.dto.UserDetailsResponse;
import com.example.drmayday.features.authorization.dto.UserResponse;
import com.example.drmayday.entity.Patient;
import com.example.drmayday.entity.Person;
import com.example.drmayday.entity.Employee;
import com.example.drmayday.entity.UserAccount;
import com.example.drmayday.entity.Role;
import com.example.drmayday.features.authorization.service.AdminUserService;
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

    private final UserAccountRepository taiKhoanRepository;
    private final PersonRepository nguoiRepository;
    private final PatientRepository benhNhanRepository;
    private final EmployeeRepository nhanVienRepository;
    private final RoleRepository vaiTroRepository;
    private final BranchRepository chiNhanhRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminUserServiceImpl(UserAccountRepository taiKhoanRepository,
                                PersonRepository nguoiRepository,
                                PatientRepository benhNhanRepository,
                                EmployeeRepository nhanVienRepository,
                                RoleRepository vaiTroRepository,
                                BranchRepository chiNhanhRepository,
                                PasswordEncoder passwordEncoder) {
        this.taiKhoanRepository = taiKhoanRepository;
        this.nguoiRepository = nguoiRepository;
        this.benhNhanRepository = benhNhanRepository;
        this.nhanVienRepository = nhanVienRepository;
        this.vaiTroRepository = vaiTroRepository;
        this.chiNhanhRepository = chiNhanhRepository;
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
        
        Page<UserAccount> accountsPage = taiKhoanRepository.findAll(pageable);
        
        return accountsPage.map(this::mapToUserResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetailsResponse getUserDetails(String id) {
        log.info("Admin truy xuất chi tiết người dùng: id={}", id);
        Long userId = parseUserId(id);

        UserAccount taiKhoan = taiKhoanRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_ACCOUNT_NOT_FOUND));

        return mapToUserDetailsResponse(taiKhoan);
    }

    @Override
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        log.info("Admin tạo mới người dùng: phone={}, role_id={}", request.getPhone(), request.getRoleId());
        String phone = request.getPhone().trim();

        if (taiKhoanRepository.existsByUsername(phone) || nguoiRepository.existsByPhoneNumber(phone)) {
            throw new AppException(ErrorCode.USER_PHONE_ALREADY_EXISTS);
        }

        Role vaiTro = resolveRole(request.getRoleId());
        boolean active = !"0".equals(request.getIsActive()) && !"false".equalsIgnoreCase(request.getIsActive());

        OffsetDateTime creationTime = OffsetDateTime.now().minusSeconds(10);

        Person nguoi = Person.builder()
                .fullName(request.getFullName().trim())
                .phoneNumber(phone)
                .createdAt(creationTime)
                .updatedAt(creationTime)
                .build();
        nguoi = nguoiRepository.save(nguoi);

        if ("benh_nhan".equalsIgnoreCase(vaiTro.getName()) || Long.valueOf(4L).equals(vaiTro.getRoleId())) {
            Patient benhNhan = Patient.builder()
                    .patientId(nguoi.getPersonId())
                    .build();
            benhNhanRepository.save(benhNhan);
        } else {
            Long chiNhanhId = chiNhanhRepository.findFirstByOrderByBranchIdAsc()
                    .map(chiNhanh -> chiNhanh == null ? null : chiNhanh.getBranchId())
                    .orElse(1L);
            Employee nhanVien = Employee.builder()
                    .employeeId(nguoi.getPersonId())
                    .branchId(chiNhanhId)
                    .build();
            nhanVienRepository.save(nhanVien);
        }

        UserAccount taiKhoan = UserAccount.builder()
                .person(nguoi)
                .role(vaiTro)
                .username(phone)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .active(active)
                .createdAt(creationTime)
                .updatedAt(creationTime)
                .build();
        taiKhoan = taiKhoanRepository.save(taiKhoan);

        return mapToUserResponse(taiKhoan);
    }

    @Override
    @Transactional
    public UserResponse editUser(String id, EditUserRequest request) {
        log.info("Admin chỉnh sửa người dùng: id={}", id);
        Long userId = parseUserId(id);

        UserAccount taiKhoan = taiKhoanRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_ACCOUNT_NOT_FOUND));

        if (request.getFullName() != null && !request.getFullName().trim().isEmpty()) {
            if (taiKhoan.getPerson() != null) {
                taiKhoan.getPerson().setFullName(request.getFullName().trim());
                OffsetDateTime now = OffsetDateTime.now();
                if (taiKhoan.getPerson().getCreatedAt() != null && now.isBefore(taiKhoan.getPerson().getCreatedAt())) {
                    now = taiKhoan.getPerson().getCreatedAt();
                }
                taiKhoan.getPerson().setUpdatedAt(now);
                nguoiRepository.save(taiKhoan.getPerson());
            }
        }

        if (request.getIsActive() != null && !request.getIsActive().trim().isEmpty()) {
            boolean active = "1".equals(request.getIsActive().trim()) || "true".equalsIgnoreCase(request.getIsActive().trim());
            taiKhoan.setActive(active);
        }

        if (request.getRoleId() != null && !request.getRoleId().trim().isEmpty()) {
            Role vaiTro = resolveRole(request.getRoleId().trim());
            taiKhoan.setRole(vaiTro);

            if (taiKhoan.getPerson() != null) {
                Long personId = taiKhoan.getPerson().getPersonId();
                if ("benh_nhan".equalsIgnoreCase(vaiTro.getName()) || Long.valueOf(4L).equals(vaiTro.getRoleId())) {
                    if (!benhNhanRepository.existsById(personId)) {
                        Patient bn = Patient.builder()
                                .patientId(personId)
                                .build();
                        benhNhanRepository.save(bn);
                    }
                } else {
                    if (!nhanVienRepository.existsById(personId)) {
                        Long chiNhanhId = chiNhanhRepository.findFirstByOrderByBranchIdAsc()
                            .map(chiNhanh -> chiNhanh.getBranchId())
                                .orElse(1L);
                        Employee nv = Employee.builder()
                                .employeeId(personId)
                                .branchId(chiNhanhId)
                                .build();
                        nhanVienRepository.save(nv);
                    }
                }
            }
        }

        OffsetDateTime nowTaiKhoan = OffsetDateTime.now();
        if (taiKhoan.getCreatedAt() != null && nowTaiKhoan.isBefore(taiKhoan.getCreatedAt())) {
            nowTaiKhoan = taiKhoan.getCreatedAt();
        }
        taiKhoan.setUpdatedAt(nowTaiKhoan);
        taiKhoan = taiKhoanRepository.save(taiKhoan);

        return mapToUserResponse(taiKhoan);
    }

    private UserResponse mapToUserResponse(UserAccount taiKhoan) {
        Person nguoi = taiKhoan.getPerson();
        Role vaiTro = taiKhoan.getRole();

        return UserResponse.builder()
                .userId(String.valueOf(taiKhoan.getAccountId()))
                .fullName(nguoi != null ? nguoi.getFullName() : "")
                .phone(taiKhoan.getUsername())
                .roleId(vaiTro != null ? String.valueOf(vaiTro.getRoleId()) : "4")
                .isActive(Boolean.TRUE.equals(taiKhoan.getActive()) ? "1" : "0")
                .createdAt(formatOffsetDateTime(taiKhoan.getCreatedAt()))
                .updatedAt(formatOffsetDateTime(taiKhoan.getUpdatedAt()))
                .build();
    }

    private UserDetailsResponse mapToUserDetailsResponse(UserAccount taiKhoan) {
        Person nguoi = taiKhoan.getPerson();
        Role vaiTro = taiKhoan.getRole();

        return UserDetailsResponse.builder()
                .userId(String.valueOf(taiKhoan.getAccountId()))
                .fullName(nguoi != null ? nguoi.getFullName() : "")
                .phone(taiKhoan.getUsername())
                .roleId(vaiTro != null ? String.valueOf(vaiTro.getRoleId()) : "4")
                .isActive(Boolean.TRUE.equals(taiKhoan.getActive()) ? "1" : "0")
                .createdAt(formatOffsetDateTime(taiKhoan.getCreatedAt()))
                .updatedAt(formatOffsetDateTime(taiKhoan.getUpdatedAt()))
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
            throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE, "Vai trò (Role) không tồn tại");
        }
        String trimmed = roleIdentifier.trim();
        try {
            long roleId = Long.parseLong(trimmed);
            return vaiTroRepository.findById(roleId)
                    .orElseThrow(() -> new AppException(ErrorCode.INVALID_PARAMETER_VALUE, "Vai trò (Role) không tồn tại"));
        } catch (NumberFormatException e) {
            String roleName = trimmed.toLowerCase();
            if ("admin".equals(roleName)) {
                return vaiTroRepository.findByName("admin")
                        .orElseThrow(() -> new AppException(ErrorCode.INVALID_PARAMETER_VALUE, "Vai trò (Role) không tồn tại"));
            }
            if ("doctor".equals(roleName) || "bac_si".equals(roleName)) {
                return vaiTroRepository.findByName("bac_si")
                        .orElseThrow(() -> new AppException(ErrorCode.INVALID_PARAMETER_VALUE, "Vai trò (Role) không tồn tại"));
            }
            if ("nurse".equals(roleName) || "dieu_duong".equals(roleName)) {
                return vaiTroRepository.findByName("dieu_duong")
                        .orElseThrow(() -> new AppException(ErrorCode.INVALID_PARAMETER_VALUE, "Vai trò (Role) không tồn tại"));
            }
            if ("patient".equals(roleName) || "benh_nhan".equals(roleName)) {
                return vaiTroRepository.findByName("benh_nhan")
                        .orElseThrow(() -> new AppException(ErrorCode.INVALID_PARAMETER_VALUE, "Vai trò (Role) không tồn tại"));
            }
            return vaiTroRepository.findByName(roleName)
                    .orElseThrow(() -> new AppException(ErrorCode.INVALID_PARAMETER_VALUE, "Vai trò (Role) không tồn tại"));
        }
    }

    private String formatOffsetDateTime(OffsetDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATE_TIME_FORMATTER) : null;
    }
}
