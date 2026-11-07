package com.example.drmayday.repository;

import com.example.drmayday.entity.QuyenHan;
import com.example.drmayday.entity.VaiTroQuyenHan;
import com.example.drmayday.entity.VaiTroQuyenHanId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VaiTroQuyenHanRepository extends JpaRepository<VaiTroQuyenHan, VaiTroQuyenHanId> {

    List<VaiTroQuyenHan> findByMaVaiTro(Long maVaiTro);

    Optional<VaiTroQuyenHan> findByMaVaiTroAndMaQuyenHan(Long maVaiTro, Long maQuyenHan);

    boolean existsByMaVaiTroAndMaQuyenHan(Long maVaiTro, Long maQuyenHan);

    @Modifying
    @Query("DELETE FROM VaiTroQuyenHan vtqh WHERE vtqh.maVaiTro = :maVaiTro AND vtqh.maQuyenHan = :maQuyenHan")
    int deleteByMaVaiTroAndMaQuyenHan(@Param("maVaiTro") Long maVaiTro, @Param("maQuyenHan") Long maQuyenHan);

    @Modifying
    @Query("DELETE FROM VaiTroQuyenHan vtqh WHERE vtqh.maVaiTro = :maVaiTro")
    int deleteByMaVaiTro(@Param("maVaiTro") Long maVaiTro);

    @Query("SELECT q FROM QuyenHan q JOIN VaiTroQuyenHan vtqh ON q.maQuyenHan = vtqh.maQuyenHan WHERE vtqh.maVaiTro = :maVaiTro")
    List<QuyenHan> findPermissionsByMaVaiTro(@Param("maVaiTro") Long maVaiTro);
}
