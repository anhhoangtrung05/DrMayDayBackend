package com.example.drmayday.features.facility.service;

import com.example.drmayday.features.facility.dto.BranchResponse;
import com.example.drmayday.features.facility.dto.CreateBranchRequest;
import com.example.drmayday.features.facility.dto.EditBranchRequest;

import java.util.List;

/**
 * Nghiệp vụ quản lý chi nhánh (API 24 đến 28).
 */
public interface BranchService {

    BranchResponse createBranch(CreateBranchRequest req);

    List<BranchResponse> getListBranches();

    BranchResponse getBranchDetails(String id);

    BranchResponse editBranch(String id, EditBranchRequest req);

    void deleteBranch(String id);
}
