package com.example.drmayday.repository;

import com.example.drmayday.entity.QuyenHan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface QuyenHanRepository extends JpaRepository<QuyenHan, Long> {

    Optional<QuyenHan> findByTenQuyenHan(String tenQuyenHan);

    boolean existsByTenQuyenHan(String tenQuyenHan);

    boolean existsByTenQuyenHanAndMaQuyenHanNot(String tenQuyenHan, Long maQuyenHan);
}
