package com.drmayday.authrbac.repository;

import com.drmayday.authrbac.entity.TaiKhoan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TaiKhoanRepository extends JpaRepository<TaiKhoan, Long> {

    Optional<TaiKhoan> findByTenDangNhap(String tenDangNhap);

    boolean existsByTenDangNhap(String tenDangNhap);

    @Query("SELECT t FROM TaiKhoan t LEFT JOIN FETCH t.nguoi LEFT JOIN FETCH t.vaiTro WHERE t.maTaiKhoan = :maTaiKhoan")
    Optional<TaiKhoan> findByIdWithDetails(@Param("maTaiKhoan") Long maTaiKhoan);

    @Query(value = "SELECT t FROM TaiKhoan t JOIN FETCH t.nguoi JOIN FETCH t.vaiTro",
           countQuery = "SELECT count(t) FROM TaiKhoan t")
    Page<TaiKhoan> findAllWithDetails(Pageable pageable);

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
