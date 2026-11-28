package com.example.drmayday.features.admin.controller;

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
import com.example.drmayday.features.admin.service.AdminUserService;
import com.example.drmayday.common.dto.BaseResponse;
import com.example.drmayday.features.profile.dto.EmployeeProfileResponse;
import com.example.drmayday.features.profile.dto.PatientProfileResponse;
import com.example.drmayday.features.profile.dto.PersonProfileResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Admin controller for user management (Administration).
 * Handles admin user, person, patient, and staff management.
 */
@RestController
@RequestMapping("/api/v1/admin")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    /**
     * ADMIN_GET_LIST_USERS
     * Retrieves a paginated list of system users. Access is restricted to admins.
     *
     * @param page Page number (defaults to 1)
     * @param limit Number of records per page (defaults to 20)
     * @return The paginated user list
     */
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<Page<UserResponse>>> getListUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {
        Page<UserResponse> usersPage = adminUserService.getListUsers(page, limit);
        return ResponseEntity.ok(BaseResponse.success(usersPage));
    }

    /**
     * ADMIN_GET_USER_DETAILS
     * Retrieves a user's account details by ID. Access is restricted to admins.
     *
     * @param id User ID
     * @return The user's account details
     */
    @GetMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<UserDetailsResponse>> getUserDetails(@PathVariable String id) {
        UserDetailsResponse userDetails = adminUserService.getUserDetails(id);
        return ResponseEntity.ok(BaseResponse.success(userDetails));
    }

    /**
     * ADMIN_CREATE_USER
     * Creates a user from the admin console. Access is restricted to admins.
     *
     * @param req New user data
     * @return The created user
     */
    @PostMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<CreatedUserResponse>> createUser(@Valid @RequestBody CreateUserRequest req) {
        CreatedUserResponse response = adminUserService.createUser(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.created("Thông báo tạo tài khoản thành công", response));
    }

    @PostMapping("/persons")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<PersonProfileResponse>> createPerson(
            @Valid @RequestBody CreatePersonRequest request) {
        PersonProfileResponse response = adminUserService.createPerson(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.created("Thông báo tạo Người thành công", response));
    }

    @PostMapping("/patients")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<PatientProfileResponse>> createPatient(
            @Valid @RequestBody CreatePatientRequest request) {
        PatientProfileResponse response = adminUserService.createPatient(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.created("Thông báo tạo hồ sơ Bệnh nhân thành công", response));
    }

    @PostMapping("/staff")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<EmployeeProfileResponse>> createStaff(
            @Valid @RequestBody CreateStaffRequest request) {
        EmployeeProfileResponse response = adminUserService.createStaff(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.created("Thông báo tạo hồ sơ Nhân viên thành công", response));
    }

    /**
     * ADMIN_EDIT_USER
     * Updates the supplied full name, role, or active status. Access is restricted to admins.
     *
     * @param id User ID
     * @param req User fields to update
     * @return The updated user
     */
    @PutMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<UserResponse>> editUser(
            @PathVariable String id,
            @RequestBody EditUserRequest req) {
        UserResponse response = adminUserService.editUser(id, req);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    /**
     * ADMIN_DELETE_USER
     * Deletes a user account by ID. Access is restricted to admins.
     *
     * @param id User ID
     * @return Success response
     */
    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<Void>> deleteUser(@PathVariable String id) {
        adminUserService.deleteUser(id);
        return ResponseEntity.ok(BaseResponse.ok());
    }

    /**
     * ADMIN_ASSIGN_USER
     * Assigns or transfers a medical staff member to a branch. Access is restricted to admins.
     *
     * @param req Assignment request containing user_id and branch_id
     * @return Staff assignment details
     */
    @PostMapping("/users/assign")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<StaffAssignResponse>> assignUser(
            @Valid @RequestBody AssignUserRequest req) {
        StaffAssignResponse response = adminUserService.assignUser(req);
        return ResponseEntity.ok(BaseResponse.success("Phân công nhân viên thành công", response));
    }
}
