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

    boolean existsByBranchId(Long branchId);

    boolean existsByBranchIdAndName(Long branchId, String name);

    boolean existsByBranchIdAndNameAndClinicRoomIdNot(Long branchId, String name, Long clinicRoomId);

    // The phienkham table does not have an entity yet; it will be added in Part 4.
    @Query(value = "select exists(select 1 from phienkham where ma_phong_kham = :roomId)", nativeQuery = true)
    boolean hasPhienKham(@Param("roomId") Long roomId);

    // Implement FOR UPDATE manually: @Lock generates FOR NO KEY UPDATE, which does not conflict with FOR KEY SHARE
    // from phienkham's foreign key. Otherwise, a new exam session could be inserted while the room is being moved.
    // This must be the first room read in the transaction.
    @Query(value = "select * from phongkham where ma_phong_kham = :roomId for update", nativeQuery = true)
    Optional<ClinicRoom> findByIdForUpdate(@Param("roomId") Long roomId);
}
