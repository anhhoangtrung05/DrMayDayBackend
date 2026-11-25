package com.example.drmayday.features.topology.service;

import java.util.List;

import com.example.drmayday.features.topology.dto.BranchResponse;
import com.example.drmayday.features.topology.dto.CreateBranchRequest;
import com.example.drmayday.features.topology.dto.EditBranchRequest;

/**
 * Branch management operations for TOPOLOGY_SET_BRANCH, TOPOLOGY_GET_LIST_BRANCHES,
 * TOPOLOGY_GET_BRANCH_DETAIL, TOPOLOGY_EDIT_BRANCH, and TOPOLOGY_DELETE_BRANCH.
 */
public interface BranchService {

    BranchResponse createBranch(CreateBranchRequest req);

    List<BranchResponse> getListBranches();

    BranchResponse getBranchDetails(String id);

    BranchResponse editBranch(String id, EditBranchRequest req);

    void deleteBranch(String id);
}
