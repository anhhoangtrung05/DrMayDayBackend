package com.example.drmayday.repository;

import com.example.drmayday.entity.TaiKhoan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TaiKhoanRepository extends JpaRepository<TaiKhoan, Long> {

    Optional<TaiKhoan> findByTenDangNhap(String tenDangNhap);

    boolean existsByTenDangNhap(String tenDangNhap);

    @EntityGraph(attributePaths = {"nguoi", "vaiTro"})
    Optional<TaiKhoan> findByMaTaiKhoan(Long maTaiKhoan);

    @EntityGraph(attributePaths = {"nguoi", "vaiTro"})
    Page<TaiKhoan> findAll(Pageable pageable);

    @Query(value = "SELECT t FROM TaiKhoan t JOIN FETCH t.nguoi n JOIN FETCH t.vaiTro v " +
                   "WHERE LOWER(t.tenDangNhap) LIKE LOWER(CONCAT('%', :search, '%')) " +
                   "OR LOWER(n.hoTen) LIKE LOWER(CONCAT('%', :search, '%')) " +
                   "OR LOWER(n.soDienThoai) LIKE LOWER(CONCAT('%', :search, '%'))",
           countQuery = "SELECT count(t) FROM TaiKhoan t JOIN t.nguoi n " +
                        "WHERE LOWER(t.tenDangNhap) LIKE LOWER(CONCAT('%', :search, '%')) " +
                        "OR LOWER(n.hoTen) LIKE LOWER(CONCAT('%', :search, '%')) " +
                        "OR LOWER(n.soDienThoai) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<TaiKhoan> searchWithDetails(@Param("search") String search, Pageable pageable);
}
