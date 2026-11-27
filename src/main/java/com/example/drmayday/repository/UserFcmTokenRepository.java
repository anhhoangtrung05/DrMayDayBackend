package com.example.drmayday.repository;

import com.example.drmayday.entity.UserFcmToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserFcmTokenRepository extends JpaRepository<UserFcmToken, Long> {

    Optional<UserFcmToken> findByFcmToken(String fcmToken);

    List<UserFcmToken> findByUserIdOrderByUpdatedAtDesc(Long userId);
}
