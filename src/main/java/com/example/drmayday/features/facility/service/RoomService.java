package com.example.drmayday.features.facility.service;

import com.example.drmayday.features.facility.dto.CreateRoomRequest;
import com.example.drmayday.features.facility.dto.EditRoomRequest;
import com.example.drmayday.features.facility.dto.RoomResponse;

import java.util.List;

/**
 * Nghiệp vụ quản lý phòng khám (API 29 đến 33).
 */
public interface RoomService {

    RoomResponse createRoom(CreateRoomRequest req);

    List<RoomResponse> getListRooms(Long branchId);

    RoomResponse getRoomDetails(String id);

    RoomResponse editRoom(String id, EditRoomRequest req);

    void deleteRoom(String id);
}
