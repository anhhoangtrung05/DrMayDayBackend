package com.drmayday.authrbac.repository;

import com.drmayday.authrbac.entity.NhanVien;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NhanVienRepository extends JpaRepository<NhanVien, Long> {

    Optional<NhanVien> findByMaNhanVien(Long maNhanVien);
}
