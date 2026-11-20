package com.example.drmayday.repository;

import com.example.drmayday.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
    boolean existsByDepartmentIdAndBranchId(Long departmentId, Long branchId);

    Optional<Department> findFirstByBranchIdOrderByDepartmentIdAsc(Long branchId);
}
