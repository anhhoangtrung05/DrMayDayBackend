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
 * Controller quản lý phòng khám (Phân hệ Cơ sở vật chất).
 * Chứa API 29 (set_room), API 30 (get_list_rooms), API 31 (get_room_detail),
 * API 32 (edit_room), API 33 (delete_room).
 */
@RestController
@RequestMapping("/api/v1/rooms")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    /**
     * API 29: room01 / set_room
     * Admin tạo phòng khám trong một chi nhánh đang hoạt động.
     *
     * @param req Dữ liệu phòng khám mới
     * @return Phòng khám vừa tạo
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<RoomResponse>> createRoom(@Valid @RequestBody CreateRoomRequest req) {
        RoomResponse response = roomService.createRoom(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.created(response));
    }

    /**
     * API 30: room02 / get_list_rooms
     * Lấy mọi phòng khám, xếp theo room_id tăng dần, có thể lọc theo chi nhánh.
     *
     * @param branchId ID chi nhánh cần lọc, để trống nghĩa là không lọc
     * @return Danh sách phòng khám
     */
    @GetMapping
    public ResponseEntity<BaseResponse<List<RoomResponse>>> getListRooms(
            @RequestParam(name = "branch_id", required = false) Long branchId) {
        return ResponseEntity.ok(BaseResponse.success(roomService.getListRooms(branchId)));
    }

    /**
     * API 31: room03 / get_room_detail
     * Lấy chi tiết một phòng khám.
     *
     * @param id ID phòng khám
     * @return Chi tiết phòng khám
     */
    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse<RoomResponse>> getRoomDetails(@PathVariable String id) {
        return ResponseEntity.ok(BaseResponse.success(roomService.getRoomDetails(id)));
    }

    /**
     * API 32: room04 / edit_room
     * Admin đổi tên phòng hoặc chuyển phòng sang chi nhánh khác khi phòng chưa có phiên khám.
     *
     * @param id ID phòng khám cần sửa
     * @param req Thông tin cập nhật
     * @return Phòng khám sau khi sửa
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<RoomResponse>> editRoom(
            @PathVariable String id,
            @Valid @RequestBody EditRoomRequest req) {
        return ResponseEntity.ok(BaseResponse.success(roomService.editRoom(id, req)));
    }

    /**
     * API 33: room05 / delete_room
     * Admin xóa phòng khám chưa có phiên khám.
     *
     * @param id ID phòng khám cần xóa
     * @return Phản hồi thành công, không có data
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<Void>> deleteRoom(@PathVariable String id) {
        roomService.deleteRoom(id);
        return ResponseEntity.ok(BaseResponse.ok());
    }
}
