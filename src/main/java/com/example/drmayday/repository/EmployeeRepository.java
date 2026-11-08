package com.example.drmayday.repository;

import com.example.drmayday.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    boolean existsByBranchId(Long branchId);
}
