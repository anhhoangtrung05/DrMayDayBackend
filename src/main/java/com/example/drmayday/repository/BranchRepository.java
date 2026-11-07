package com.example.drmayday.repository;

import com.example.drmayday.entity.Branch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BranchRepository extends JpaRepository<Branch, Long> {
    Optional<Branch> findFirstByOrderByBranchIdAsc();
}
