package com.example.drmayday.repository;

import com.example.drmayday.entity.TokenBlacklist;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TokenBlacklistRepository extends JpaRepository<TokenBlacklist, Integer> {

    boolean existsByJti(String jti);
}
