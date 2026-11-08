package com.example.drmayday.features.facility.controller;

import com.example.drmayday.common.dto.BaseResponse;
import com.example.drmayday.features.facility.dto.BranchResponse;
import com.example.drmayday.features.facility.dto.CreateBranchRequest;
import com.example.drmayday.features.facility.dto.EditBranchRequest;
import com.example.drmayday.features.facility.service.BranchService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller quản lý chi nhánh (Phân hệ Cơ sở vật chất).
 * Chứa API 24 (set_branch), API 25 (get_list_branches), API 26 (get_branch_detail),
 * API 27 (edit_branch), API 28 (delete_branch).
 */
@RestController
@RequestMapping("/api/v1/branches")
public class BranchController {

    private final BranchService branchService;

    public BranchController(BranchService branchService) {
        this.branchService = branchService;
    }

    /**
     * API 24: branch01 / set_branch
     * Admin tạo chi nhánh mới, trạng thái mặc định là đang hoạt động.
     *
     * @param req Dữ liệu chi nhánh mới
     * @return Chi nhánh vừa tạo
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<BranchResponse>> createBranch(@Valid @RequestBody CreateBranchRequest req) {
        BranchResponse response = branchService.createBranch(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.created(response));
    }

    /**
     * API 25: branch02 / get_list_branches
     * Lấy mọi chi nhánh, kể cả chi nhánh ngừng hoạt động, xếp theo branch_id tăng dần.
     *
     * @return Danh sách chi nhánh
     */
    @GetMapping
    public ResponseEntity<BaseResponse<List<BranchResponse>>> getListBranches() {
        return ResponseEntity.ok(BaseResponse.success(branchService.getListBranches()));
    }

    /**
     * API 26: branch03 / get_branch_detail
     * Lấy chi tiết một chi nhánh.
     *
     * @param id ID chi nhánh
     * @return Chi tiết chi nhánh
     */
    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse<BranchResponse>> getBranchDetails(@PathVariable String id) {
        return ResponseEntity.ok(BaseResponse.success(branchService.getBranchDetails(id)));
    }

    /**
     * API 27: branch04 / edit_branch
     * Admin sửa chi nhánh, chỉ đổi những trường được gửi.
     *
     * @param id ID chi nhánh cần sửa
     * @param req Thông tin cập nhật
     * @return Chi nhánh sau khi sửa
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<BranchResponse>> editBranch(
            @PathVariable String id,
            @Valid @RequestBody EditBranchRequest req) {
        return ResponseEntity.ok(BaseResponse.success(branchService.editBranch(id, req)));
    }

    /**
     * API 28: branch05 / delete_branch
     * Admin xóa chi nhánh không còn phòng khám và nhân viên. Chi nhánh cuối cùng không xóa được.
     *
     * @param id ID chi nhánh cần xóa
     * @return Phản hồi thành công, không có data
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<Void>> deleteBranch(@PathVariable String id) {
        branchService.deleteBranch(id);
        return ResponseEntity.ok(BaseResponse.ok());
    }
}
