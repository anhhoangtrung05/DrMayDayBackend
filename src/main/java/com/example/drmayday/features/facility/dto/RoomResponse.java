package com.example.drmayday.features.facility.dto;

import com.example.drmayday.entity.Branch;
import com.example.drmayday.entity.ClinicRoom;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Clinic room response DTO for TOPOLOGY_SET_ROOM, TOPOLOGY_GET_LIST_ROOMS,
 * TOPOLOGY_GET_ROOM_DETAIL, and TOPOLOGY_EDIT_ROOM. room_id and branch_id are always serialized as JSON strings.
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
