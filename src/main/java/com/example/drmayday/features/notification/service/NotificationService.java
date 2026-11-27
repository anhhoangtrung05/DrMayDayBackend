package com.example.drmayday.features.notification.service;

import com.example.drmayday.features.notification.dto.FcmTokenResponse;
import com.example.drmayday.features.notification.dto.NotificationResponse;
import com.example.drmayday.features.notification.dto.SetDevTokenRequest;

import java.util.List;

public interface NotificationService {

    FcmTokenResponse registerDeviceToken(SetDevTokenRequest request);

    List<NotificationResponse> getMyNotifications(String page, String limit);

    NotificationResponse markAsRead(String id);

    void deleteNotification(String id);
}
