package com.example.drmayday.repository;

import com.example.drmayday.entity.Permission;
import com.example.drmayday.entity.RolePermission;
import com.example.drmayday.entity.RolePermissionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RolePermissionRepository extends JpaRepository<RolePermission, RolePermissionId> {

    @Query("SELECT rp.permission FROM RolePermission rp WHERE rp.roleId = :roleId ORDER BY rp.permissionId ASC")
    List<Permission> findPermissionsByRoleId(@Param("roleId") Long roleId);

    @Modifying
    @Query("DELETE FROM RolePermission rp WHERE rp.roleId = :roleId")
    void deleteByRoleId(@Param("roleId") Long roleId);

    @Modifying
    @Query("DELETE FROM RolePermission rp WHERE rp.roleId = :roleId AND rp.permissionId = :permissionId")
    void deleteByRoleIdAndPermissionId(@Param("roleId") Long roleId, @Param("permissionId") Long permissionId);

    boolean existsByRoleIdAndPermissionId(Long roleId, Long permissionId);
}
