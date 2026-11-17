package com.example.drmayday.repository;

import com.example.drmayday.entity.ClinicRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClinicRoomRepository extends JpaRepository<ClinicRoom, Long> {

    @Query("select r from ClinicRoom r join fetch r.branch order by r.clinicRoomId")
    List<ClinicRoom> findAllWithBranch();

    @Query("select r from ClinicRoom r join fetch r.branch where r.branchId = :branchId order by r.clinicRoomId")
    List<ClinicRoom> findAllByBranchIdWithBranch(@Param("branchId") Long branchId);

    @Query("select r from ClinicRoom r join fetch r.branch where r.clinicRoomId = :id")
    Optional<ClinicRoom> findByIdWithBranch(@Param("id") Long id);

    boolean existsByBranchId(Long branchId);

    boolean existsByBranchIdAndName(Long branchId, String name);

    boolean existsByBranchIdAndNameAndClinicRoomIdNot(Long branchId, String name, Long clinicRoomId);

    // Bảng phienkham chưa có entity, Phần 4 sẽ thiết kế entity đầy đủ
    @Query(value = "select exists(select 1 from phienkham where ma_phong_kham = :roomId)", nativeQuery = true)
    boolean hasPhienKham(@Param("roomId") Long roomId);

    // Phải viết tay FOR UPDATE: @Lock sinh FOR NO KEY UPDATE, không xung đột với FOR KEY SHARE của khóa ngoại phienkham,
    // nên phiên khám mới sẽ lọt vào đúng lúc đang chuyển phòng. Hàm này phải là lệnh đầu tiên đọc phòng trong giao dịch
    @Query(value = "select * from phongkham where ma_phong_kham = :roomId for update", nativeQuery = true)
    Optional<ClinicRoom> findByIdForUpdate(@Param("roomId") Long roomId);
}
