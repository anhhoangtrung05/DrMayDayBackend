package com.example.drmayday.features.facility.dto;

import com.example.drmayday.entity.Branch;
import com.example.drmayday.entity.ClinicRoom;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO phản hồi phòng khám (API 29 đến 32). room_id và branch_id luôn là chuỗi trong JSON.
 */
public class RoomResponse {

    @JsonProperty("room_id")
    private String roomId;

    @JsonProperty("branch_id")
    private String branchId;

    @JsonProperty("branch_name")
    private String branchName;

    @JsonProperty("name")
    private String name;

    public RoomResponse() {
    }

    public RoomResponse(String roomId, String branchId, String branchName, String name) {
        this.roomId = roomId;
        this.branchId = branchId;
        this.branchName = branchName;
        this.name = name;
    }

    public static RoomResponse from(ClinicRoom clinicRoom) {
        Branch branch = clinicRoom.getBranch();
        return new RoomResponse(
                String.valueOf(clinicRoom.getClinicRoomId()),
                String.valueOf(branch.getBranchId()),
                branch.getName(),
                clinicRoom.getName());
    }

    public String getRoomId() {
        return roomId;
    }

    public String getBranchId() {
        return branchId;
    }

    public String getBranchName() {
        return branchName;
    }

    public String getName() {
        return name;
    }
}
