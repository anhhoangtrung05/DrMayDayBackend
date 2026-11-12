package com.example.drmayday.features.admin.controller;

import com.example.drmayday.features.admin.dto.CreateUserRequest;
import com.example.drmayday.features.admin.dto.EditUserRequest;
import com.example.drmayday.features.admin.dto.UserDetailsResponse;
import com.example.drmayday.features.admin.dto.UserResponse;
import com.example.drmayday.features.admin.service.AdminUserService;
import com.example.drmayday.common.dto.BaseResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Admin controller for user management (Administration and Authorization).
 * Handles ADMIN_GET_LIST_USERS, ADMIN_GET_USER_DETAILS, ADMIN_CREATE_USER, and ADMIN_EDIT_USER.
 */
@RestController
@RequestMapping("/api/v1/admin/users")
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
    @GetMapping
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
    @GetMapping("/{id}")
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
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<UserResponse>> createUser(@Valid @RequestBody CreateUserRequest req) {
        UserResponse response = adminUserService.createUser(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.created(response));
    }

    /**
     * ADMIN_EDIT_USER
     * Updates the supplied full name, role, or active status. Access is restricted to admins.
     *
     * @param id User ID
     * @param req User fields to update
     * @return The updated user
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<UserResponse>> editUser(
            @PathVariable String id,
            @RequestBody EditUserRequest req) {
        UserResponse response = adminUserService.editUser(id, req);
        return ResponseEntity.ok(BaseResponse.success(response));
    }
}
