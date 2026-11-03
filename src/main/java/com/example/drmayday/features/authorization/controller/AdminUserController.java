package com.example.drmayday.features.authorization.controller;

import com.example.drmayday.features.authorization.dto.CreateUserRequest;
import com.example.drmayday.features.authorization.dto.EditUserRequest;
import com.example.drmayday.features.authorization.dto.UserDetailsResponse;
import com.example.drmayday.features.authorization.dto.UserResponse;
import com.example.drmayday.features.authorization.service.AdminUserService;
import com.example.drmayday.common.dto.BaseResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controller quản trị người dùng dành cho Admin (Phân hệ Quản trị & Phân quyền).
 * Chứa API 9 (get_list_users), API 10 (get_user_details), API 11 (create_user), API 12 (edit_user).
 */
@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    /**
     * API 9: admin02 / get_list_users
     * Admin truy xuất danh sách người dùng trong hệ thống (hỗ trợ phân trang).
     *
     * @param page Số trang hiện tại (mặc định 1)
     * @param limit Số bản ghi trên 1 trang (mặc định 20)
     * @return Danh sách người dùng phân trang
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
     * API 10: admin03 / get_user_details
     * Lấy thông tin chi tiết của một người dùng theo ID.
     *
     * @param id ID người dùng cần xem
     * @return Chi tiết tài khoản người dùng
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<UserDetailsResponse>> getUserDetails(@PathVariable String id) {
        UserDetailsResponse userDetails = adminUserService.getUserDetails(id);
        return ResponseEntity.ok(BaseResponse.success(userDetails));
    }

    /**
     * API 11: admin01 / create_user
     * Tạo mới người dùng từ trang quản trị Admin.
     *
     * @param req Dữ liệu tạo người dùng mới
     * @return Thông tin người dùng vừa tạo
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<UserResponse>> createUser(@Valid @RequestBody CreateUserRequest req) {
        UserResponse response = adminUserService.createUser(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.created(response));
    }

    /**
     * API 12: admin04 / edit_user
     * Chỉnh sửa thông tin người dùng (Họ tên, Vai trò, Trạng thái hoạt động).
     *
     * @param id ID người dùng cần chỉnh sửa
     * @param req Thông tin cập nhật
     * @return Thông tin người dùng sau cập nhật
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
