package com.example.drmayday.repository;

import com.example.drmayday.entity.PhongKham;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PhongKhamRepository extends JpaRepository<PhongKham, Long> {

    @Query("select p from PhongKham p join fetch p.chiNhanh order by p.maPhongKham")
    List<PhongKham> findAllWithChiNhanh();

    @Query("select p from PhongKham p join fetch p.chiNhanh c "
            + "where c.maChiNhanh = :branchId order by p.maPhongKham")
    List<PhongKham> findAllByBranchWithChiNhanh(@Param("branchId") Long branchId);

    boolean existsByChiNhanh_MaChiNhanh(Long branchId);

    boolean existsByChiNhanh_MaChiNhanhAndTenPhongKham(Long branchId, String name);

    boolean existsByChiNhanh_MaChiNhanhAndTenPhongKhamAndMaPhongKhamNot(Long branchId, String name, Long roomId);

    // Bảng phienkham chưa có entity, Phần 4 sẽ thiết kế entity đầy đủ
    @Query(value = "select exists(select 1 from phienkham where ma_phong_kham = :roomId)", nativeQuery = true)
    boolean hasPhienKham(@Param("roomId") Long roomId);

    // Phải viết tay FOR UPDATE: @Lock sinh FOR NO KEY UPDATE, không xung đột với FOR KEY SHARE của khóa ngoại phienkham,
    // nên phiên khám mới sẽ lọt vào đúng lúc đang chuyển phòng. Hàm này phải là lệnh đầu tiên đọc phòng trong giao dịch
    @Query(value = "select * from phongkham where ma_phong_kham = :roomId for update", nativeQuery = true)
    Optional<PhongKham> findByIdForUpdate(@Param("roomId") Long roomId);
}
