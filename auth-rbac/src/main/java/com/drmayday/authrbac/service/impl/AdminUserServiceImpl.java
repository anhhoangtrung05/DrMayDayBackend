package com.drmayday.authrbac.service.impl;

import com.drmayday.authrbac.dto.CreateUserRequest;
import com.drmayday.authrbac.dto.EditUserRequest;
import com.drmayday.authrbac.dto.UserDetailsResponse;
import com.drmayday.authrbac.dto.UserResponse;
import com.drmayday.authrbac.entity.Patient;
import com.drmayday.authrbac.entity.Role;
import com.drmayday.authrbac.entity.Staff;
import com.drmayday.authrbac.entity.User;
import com.drmayday.authrbac.repository.PatientRepository;
import com.drmayday.authrbac.repository.RoleRepository;
import com.drmayday.authrbac.repository.StaffRepository;
import com.drmayday.authrbac.repository.UserRepository;
import com.drmayday.authrbac.service.AdminUserService;
import com.drmayday.common.exception.AppException;
import com.drmayday.common.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class AdminUserServiceImpl implements AdminUserService {

    private static final Logger log = LoggerFactory.getLogger(AdminUserServiceImpl.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final StaffRepository staffRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminUserServiceImpl(UserRepository userRepository,
                                PatientRepository patientRepository,
                                StaffRepository staffRepository,
                                RoleRepository roleRepository,
                                PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
        this.staffRepository = staffRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> getListUsers(int page, int limit) {
        log.info("Admin truy xuất danh sách người dùng: page={}, limit={}", page, limit);
        int pageIndex = Math.max(page - 1, 0);
        int pageSize = limit > 0 ? limit : 20;

        Pageable pageable = PageRequest.of(pageIndex, pageSize, Sort.by(Sort.Direction.DESC, "userId"));
        Page<User> usersPage = userRepository.findAllUsersWithRoles(pageable);

        return usersPage.map(this::mapToUserResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetailsResponse getUserDetails(String id) {
        log.info("Admin truy xuất chi tiết người dùng: id={}", id);
        Integer userId = parseUserId(id);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_ACCOUNT_NOT_FOUND));

        return UserDetailsResponse.builder()
                .userId(String.valueOf(user.getUserId()))
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .roleId(resolveRoleIdString(user))
                .isActive(String.valueOf(user.getIsActive()))
                .createdAt(formatDateTime(user.getCreatedAt()))
                .updatedAt(formatDateTime(user.getUpdatedAt()))
                .build();
    }

    @Override
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        log.info("Admin tạo mới người dùng: phone={}, role_id={}", request.getPhone(), request.getRoleId());

        if (userRepository.existsByPhone(request.getPhone().trim())) {
            throw new AppException(ErrorCode.USER_PHONE_ALREADY_EXISTS);
        }

        Role role = resolveRole(request.getRoleId());

        int active = "0".equals(request.getIsActive()) ? 0 : 1;

        User user = User.builder()
                .fullName(request.getFullName().trim())
                .phone(request.getPhone().trim())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .isActive(active)
                .build();

        // Lưu thông tin vai trò vào bảng tương ứng qua CascadeType.ALL
        if ("patient".equalsIgnoreCase(role.getName())) {
            Patient patient = Patient.builder()
                    .user(user)
                    .roleId(role.getRoleId())
                    .build();
            user.setPatient(patient);
        } else {
            Staff staff = Staff.builder()
                    .user(user)
                    .roleId(role.getRoleId())
                    .build();
            user.setStaff(staff);
        }

        user = userRepository.save(user);

        return UserResponse.builder()
                .userId(String.valueOf(user.getUserId()))
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .roleId(String.valueOf(role.getRoleId()))
                .isActive(String.valueOf(user.getIsActive()))
                .createdAt(formatDateTime(user.getCreatedAt()))
                .build();
    }

    @Override
    @Transactional
    public UserResponse editUser(String id, EditUserRequest request) {
        log.info("Admin chỉnh sửa người dùng: id={}", id);
        Integer userId = parseUserId(id);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_ACCOUNT_NOT_FOUND));

        if (request.getFullName() != null && !request.getFullName().trim().isEmpty()) {
            user.setFullName(request.getFullName().trim());
        }

        if (request.getIsActive() != null && !request.getIsActive().trim().isEmpty()) {
            int active = "1".equals(request.getIsActive().trim()) || "true".equalsIgnoreCase(request.getIsActive().trim()) ? 1 : 0;
            user.setIsActive(active);
        }

        if (request.getRoleId() != null && !request.getRoleId().trim().isEmpty()) {
            Role role = resolveRole(request.getRoleId().trim());

            if ("patient".equalsIgnoreCase(role.getName())) {
                Patient patient = patientRepository.findByUserId(userId).orElse(null);
                if (patient != null) {
                    patient.setRoleId(role.getRoleId());
                    patientRepository.save(patient);
                } else {
                    Patient newPatient = Patient.builder()
                            .user(user)
                            .roleId(role.getRoleId())
                            .build();
                    user.setPatient(newPatient);
                    user.setStaff(null);
                }
            } else {
                Staff staff = staffRepository.findByUserId(userId).orElse(null);
                if (staff != null) {
                    staff.setRoleId(role.getRoleId());
                    staffRepository.save(staff);
                } else {
                    Staff newStaff = Staff.builder()
                            .user(user)
                            .roleId(role.getRoleId())
                            .build();
                    user.setStaff(newStaff);
                    user.setPatient(null);
                }
            }
        }

        user = userRepository.save(user);

        return UserResponse.builder()
                .userId(String.valueOf(user.getUserId()))
                .fullName(user.getFullName())
                .roleId(resolveRoleIdString(user))
                .isActive(String.valueOf(user.getIsActive()))
                .updatedAt(formatDateTime(user.getUpdatedAt()))
                .build();
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .userId(String.valueOf(user.getUserId()))
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .roleId(resolveRoleIdString(user))
                .isActive(String.valueOf(user.getIsActive()))
                .createdAt(formatDateTime(user.getCreatedAt()))
                .updatedAt(formatDateTime(user.getUpdatedAt()))
                .build();
    }

    private Integer parseUserId(String id) {
        try {
            return Integer.parseInt(id);
        } catch (NumberFormatException ex) {
            throw new AppException(ErrorCode.USER_ACCOUNT_NOT_FOUND);
        }
    }

    private Role resolveRole(String roleIdentifier) {
        try {
            int roleId = Integer.parseInt(roleIdentifier);
            return roleRepository.findById(roleId)
                    .orElseThrow(() -> new AppException(ErrorCode.INVALID_PARAMETER_VALUE, "Vai trò (Role) không tồn tại"));
        } catch (NumberFormatException e) {
            return roleRepository.findByName(roleIdentifier)
                    .orElseThrow(() -> new AppException(ErrorCode.INVALID_PARAMETER_VALUE, "Vai trò (Role) không tồn tại"));
        }
    }

    private String resolveRoleIdString(User user) {
        Integer roleId = user.getEffectiveRoleId();
        return roleId != null ? String.valueOf(roleId) : "2";
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATE_TIME_FORMATTER) : null;
    }
}
