package com.example.drmayday.features.facility.controller;

import com.example.drmayday.common.dto.BaseResponse;
import com.example.drmayday.features.facility.dto.CreateRoomRequest;
import com.example.drmayday.features.facility.dto.EditRoomRequest;
import com.example.drmayday.features.facility.dto.RoomResponse;
import com.example.drmayday.features.facility.service.RoomService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for clinic room management (Facility module).
 * Handles TOPOLOGY_SET_ROOM, TOPOLOGY_GET_LIST_ROOMS, TOPOLOGY_GET_ROOM_DETAIL,
 * TOPOLOGY_EDIT_ROOM, and TOPOLOGY_DELETE_ROOM.
 */
@RestController
@RequestMapping("/api/v1/rooms")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    /**
     * TOPOLOGY_SET_ROOM
     * Creates a clinic room in a branch. Access is restricted to admins.
     *
     * @param req New clinic room data
     * @return The created clinic room
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<RoomResponse>> createRoom(@Valid @RequestBody CreateRoomRequest req) {
        RoomResponse response = roomService.createRoom(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.created(response));
    }

    /**
     * TOPOLOGY_GET_LIST_ROOMS
     * Retrieves clinic rooms, optionally filtered by branch.
     *
     * @param branchId Branch ID to filter by; omit it to return all rooms
     * @return The list of clinic rooms
     */
    @GetMapping
    public ResponseEntity<BaseResponse<List<RoomResponse>>> getListRooms(
            @RequestParam(name = "branch_id", required = false) Long branchId) {
        return ResponseEntity.ok(BaseResponse.success(roomService.getListRooms(branchId)));
    }

    /**
     * TOPOLOGY_GET_ROOM_DETAIL
     * Retrieves a clinic room by ID.
     *
     * @param id Clinic room ID
     * @return Clinic room details
     */
    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse<RoomResponse>> getRoomDetails(@PathVariable String id) {
        return ResponseEntity.ok(BaseResponse.success(roomService.getRoomDetails(id)));
    }

    /**
     * TOPOLOGY_EDIT_ROOM
     * Updates the supplied clinic room fields. Access is restricted to admins.
     *
     * @param id ID of the clinic room to update
     * @param req Updated room data
     * @return The updated clinic room
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<RoomResponse>> editRoom(
            @PathVariable String id,
            @Valid @RequestBody EditRoomRequest req) {
        return ResponseEntity.ok(BaseResponse.success(roomService.editRoom(id, req)));
    }

    /**
     * TOPOLOGY_DELETE_ROOM
     * Deletes a clinic room with no exam sessions. Access is restricted to admins.
     *
     * @param id ID of the clinic room to delete
     * @return A success response with no data
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<Void>> deleteRoom(@PathVariable String id) {
        roomService.deleteRoom(id);
        return ResponseEntity.ok(BaseResponse.ok());
    }
}
