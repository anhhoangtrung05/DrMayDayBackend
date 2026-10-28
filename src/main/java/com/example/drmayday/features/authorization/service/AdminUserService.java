package com.example.drmayday.features.authorization.service;

import com.example.drmayday.features.authorization.dto.CreateUserRequest;
import com.example.drmayday.features.authorization.dto.EditUserRequest;
import com.example.drmayday.features.authorization.dto.UserDetailsResponse;
import com.example.drmayday.features.authorization.dto.UserResponse;
import org.springframework.data.domain.Page;

public interface AdminUserService {

    /**
     * API 9: admin02 / get_list_users - Admin truy xuất danh sách người dùng trong hệ thống (hỗ trợ phân trang).
     *
     * @param page Số trang hiện tại (1-based index)
     * @param limit Số bản ghi trên 1 trang
     * @return Trang danh sách người dùng
     */
    Page<UserResponse> getListUsers(int page, int limit);

    /**
     * API 10: admin03 / get_user_details - Lấy thông tin chi tiết một người dùng theo ID.
     *
     * @param id ID người dùng dạng chuỗi
     * @return Thông tin chi tiết người dùng
     */
    UserDetailsResponse getUserDetails(String id);

    /**
     * API 11: admin01 / create_user - Tạo mới người dùng từ trang quản trị Admin.
     *
     * @param request Dữ liệu tạo người dùng
     * @return Thông tin người dùng vừa tạo
     */
    UserResponse createUser(CreateUserRequest request);

    /**
     * API 12: admin04 / edit_user - Chỉnh sửa thông tin người dùng (Họ tên, Vai trò, Trạng thái).
     *
     * @param id ID người dùng cần chỉnh sửa
     * @param request Dữ liệu cập nhật
     * @return Thông tin người dùng sau khi cập nhật
     */
    UserResponse editUser(String id, EditUserRequest request);
}
