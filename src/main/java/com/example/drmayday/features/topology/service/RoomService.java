package com.example.drmayday.features.topology.service;

import java.util.List;

import com.example.drmayday.features.topology.dto.CreateRoomRequest;
import com.example.drmayday.features.topology.dto.EditRoomRequest;
import com.example.drmayday.features.topology.dto.RoomResponse;

/**
 * Clinic room management operations for TOPOLOGY_SET_ROOM, TOPOLOGY_GET_LIST_ROOMS,
 * TOPOLOGY_GET_ROOM_DETAIL, TOPOLOGY_EDIT_ROOM, and TOPOLOGY_DELETE_ROOM.
 */
public interface RoomService {

    RoomResponse createRoom(CreateRoomRequest req);

    List<RoomResponse> getListRooms(Long branchId);

    RoomResponse getRoomDetails(String id);

    RoomResponse editRoom(String id, EditRoomRequest req);

    void deleteRoom(String id);
}
