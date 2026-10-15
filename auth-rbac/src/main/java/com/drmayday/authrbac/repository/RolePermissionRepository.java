package com.drmayday.authrbac.repository;

import com.drmayday.authrbac.entity.Permission;
import com.drmayday.authrbac.entity.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, Integer> {

    List<RolePermission> findByRoleId(Integer roleId);

    Optional<RolePermission> findByRoleIdAndPermissionId(Integer roleId, Integer permissionId);

    boolean existsByRoleIdAndPermissionId(Integer roleId, Integer permissionId);

    @Modifying
    @Query("DELETE FROM RolePermission rp WHERE rp.roleId = :roleId AND rp.permissionId = :permissionId")
    int deleteByRoleIdAndPermissionId(@Param("roleId") Integer roleId, @Param("permissionId") Integer permissionId);

    @Modifying
    @Query("DELETE FROM RolePermission rp WHERE rp.roleId = :roleId")
    int deleteByRoleId(@Param("roleId") Integer roleId);

    @Query("SELECT p FROM Permission p JOIN RolePermission rp ON p.permissionId = rp.permissionId WHERE rp.roleId = :roleId")
    List<Permission> findPermissionsByRoleId(@Param("roleId") Integer roleId);
}
