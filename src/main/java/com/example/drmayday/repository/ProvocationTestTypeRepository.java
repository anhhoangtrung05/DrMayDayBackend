package com.example.drmayday.repository;

import com.example.drmayday.entity.ProvocationTestType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProvocationTestTypeRepository extends JpaRepository<ProvocationTestType, Long> {

    List<ProvocationTestType> findAllByOrderByProvocationTestIdAsc();

    boolean existsByTestName(String testName);

    boolean existsByTestNameAndProvocationTestIdNot(String testName, Long provocationTestId);
}
