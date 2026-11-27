package com.example.drmayday.features.notification.controller;

import com.example.drmayday.common.dto.BaseResponse;
import com.example.drmayday.features.notification.dto.FcmTokenResponse;
import com.example.drmayday.features.notification.dto.NotificationResponse;
import com.example.drmayday.features.notification.dto.SetDevTokenRequest;
import com.example.drmayday.features.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping("/token")
    public ResponseEntity<BaseResponse<FcmTokenResponse>> setDevToken(@RequestBody(required = false) SetDevTokenRequest req) {
        FcmTokenResponse response = notificationService.registerDeviceToken(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.created("Đăng ký Token thành công", response));
    }

    @GetMapping({"", "/"})
    public ResponseEntity<BaseResponse<List<NotificationResponse>>> getNotification(
            @RequestParam(required = false) String page,
            @RequestParam(required = false) String limit) {
        return ResponseEntity.ok(BaseResponse.ok(notificationService.getMyNotifications(page, limit)));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<BaseResponse<NotificationResponse>> setReadNotification(@PathVariable String id) {
        return ResponseEntity.ok(BaseResponse.success("Đã đánh dấu đọc", notificationService.markAsRead(id)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<BaseResponse<Void>> deleteNotification(@PathVariable String id) {
        notificationService.deleteNotification(id);
        return ResponseEntity.ok(BaseResponse.success("Xóa thông báo thành công", null));
    }
}
