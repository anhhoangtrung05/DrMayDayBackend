package com.drmayday.authrbac.repository;

import com.drmayday.authrbac.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Integer> {
    Optional<Permission> findByName(String name);

    boolean existsByName(String name);

    boolean existsByNameAndPermissionIdNot(String name, Integer permissionId);
}
