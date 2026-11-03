package com.example.drmayday.repository;

import com.example.drmayday.entity.Nguoi;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NguoiRepository extends JpaRepository<Nguoi, Long> {

    Optional<Nguoi> findBySoDienThoai(String soDienThoai);

    boolean existsBySoDienThoai(String soDienThoai);
}
