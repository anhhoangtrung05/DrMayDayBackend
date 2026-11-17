package com.example.drmayday.features.facility.dto;

import com.example.drmayday.entity.Branch;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Branch response DTO for TOPOLOGY_SET_BRANCH, TOPOLOGY_GET_LIST_BRANCHES,
 * TOPOLOGY_GET_BRANCH_DETAIL, and TOPOLOGY_EDIT_BRANCH. branch_id is always serialized as a JSON string.
 */
public class BranchResponse {

    @JsonProperty("branch_id")
    private String branchId;

    @JsonProperty("name")
    private String name;

    @JsonProperty("address")
    private String address;

    @JsonProperty("phone")
    private String phone;

    @JsonProperty("status")
    private Boolean status;

    public BranchResponse() {
    }

    public BranchResponse(String branchId, String name, String address, String phone, Boolean status) {
        this.branchId = branchId;
        this.name = name;
        this.address = address;
        this.phone = phone;
        this.status = status;
    }

    public static BranchResponse from(Branch branch) {
        return new BranchResponse(
                String.valueOf(branch.getBranchId()),
                branch.getName(),
                branch.getAddress(),
                branch.getPhoneNumber(),
                branch.getActive());
    }

    public String getBranchId() {
        return branchId;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public String getPhone() {
        return phone;
    }

    public Boolean getStatus() {
        return status;
    }
}
