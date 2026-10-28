package com.example.drmayday.repository;

import com.example.drmayday.entity.NhanVien;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NhanVienRepository extends JpaRepository<NhanVien, Long> {

    Optional<NhanVien> findByMaNhanVien(Long maNhanVien);
}
