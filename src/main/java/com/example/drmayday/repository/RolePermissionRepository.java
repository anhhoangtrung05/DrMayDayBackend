package com.example.drmayday.repository;

import com.example.drmayday.entity.RolePermission;
import com.example.drmayday.entity.RolePermissionId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RolePermissionRepository extends JpaRepository<RolePermission, RolePermissionId> {
}
