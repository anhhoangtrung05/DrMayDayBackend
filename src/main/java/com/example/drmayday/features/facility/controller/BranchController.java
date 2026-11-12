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
 * Controller for branch management (Facility module).
 * Handles TOPOLOGY_SET_BRANCH, TOPOLOGY_GET_LIST_BRANCHES, TOPOLOGY_GET_BRANCH_DETAIL,
 * TOPOLOGY_EDIT_BRANCH, and TOPOLOGY_DELETE_BRANCH.
 */
@RestController
@RequestMapping("/api/v1/branches")
public class BranchController {

    private final BranchService branchService;

    public BranchController(BranchService branchService) {
        this.branchService = branchService;
    }

    /**
     * TOPOLOGY_SET_BRANCH
     * Creates a new branch. Access is restricted to admins.
     *
     * @param req New branch data
     * @return The created branch
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<BranchResponse>> createBranch(@Valid @RequestBody CreateBranchRequest req) {
        BranchResponse response = branchService.createBranch(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.created(response));
    }

    /**
     * TOPOLOGY_GET_LIST_BRANCHES
     * Retrieves all branches.
     *
     * @return The list of branches
     */
    @GetMapping
    public ResponseEntity<BaseResponse<List<BranchResponse>>> getListBranches() {
        return ResponseEntity.ok(BaseResponse.success(branchService.getListBranches()));
    }

    /**
     * TOPOLOGY_GET_BRANCH_DETAIL
     * Retrieves a branch by ID.
     *
     * @param id Branch ID
     * @return Branch details
     */
    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse<BranchResponse>> getBranchDetails(@PathVariable String id) {
        return ResponseEntity.ok(BaseResponse.success(branchService.getBranchDetails(id)));
    }

    /**
     * TOPOLOGY_EDIT_BRANCH
     * Updates the supplied branch fields. Access is restricted to admins.
     *
     * @param id ID of the branch to update
     * @param req Updated branch data
     * @return The updated branch
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<BranchResponse>> editBranch(
            @PathVariable String id,
            @Valid @RequestBody EditBranchRequest req) {
        return ResponseEntity.ok(BaseResponse.success(branchService.editBranch(id, req)));
    }

    /**
     * TOPOLOGY_DELETE_BRANCH
     * Deletes a branch if it has no dependent resources. Access is restricted to admins.
     *
     * @param id ID of the branch to delete
     * @return A success response with no data
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<Void>> deleteBranch(@PathVariable String id) {
        branchService.deleteBranch(id);
        return ResponseEntity.ok(BaseResponse.ok());
    }
}
