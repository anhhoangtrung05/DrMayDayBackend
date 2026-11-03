package com.example.drmayday.repository;

import com.example.drmayday.entity.BenhNhan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BenhNhanRepository extends JpaRepository<BenhNhan, Long> {

    Optional<BenhNhan> findByMaBenhNhan(Long maBenhNhan);
}
