package com.drmayday.facility.repository;

import com.drmayday.facility.entity.ChiNhanh;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChiNhanhRepository extends JpaRepository<ChiNhanh, Long> {
    Optional<ChiNhanh> findFirstByOrderByMaChiNhanhAsc();
}
