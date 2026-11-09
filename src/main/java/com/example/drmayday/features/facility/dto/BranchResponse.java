package com.example.drmayday.features.facility.dto;

import com.example.drmayday.entity.ChiNhanh;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO phản hồi chi nhánh (API 24 đến 27). branch_id luôn là chuỗi trong JSON.
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

    public static BranchResponse from(ChiNhanh chiNhanh) {
        return new BranchResponse(
                String.valueOf(chiNhanh.getMaChiNhanh()),
                chiNhanh.getTenChiNhanh(),
                chiNhanh.getDiaChi(),
                chiNhanh.getSoDienThoai(),
                chiNhanh.getTrangThai());
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
