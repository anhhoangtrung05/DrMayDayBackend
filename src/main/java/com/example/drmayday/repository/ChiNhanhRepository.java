package com.example.drmayday.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.drmayday.entity.ChiNhanh;

import java.util.Optional;

public interface ChiNhanhRepository extends JpaRepository<ChiNhanh, Long> {
    Optional<ChiNhanh> findFirstByOrderByMaChiNhanhAsc();
}
