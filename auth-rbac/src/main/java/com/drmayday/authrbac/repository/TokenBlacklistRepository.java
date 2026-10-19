package com.drmayday.authrbac.repository;

import com.drmayday.authrbac.entity.TokenBlacklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TokenBlacklistRepository extends JpaRepository<TokenBlacklist, Integer> {

    boolean existsByJti(String jti);
}
