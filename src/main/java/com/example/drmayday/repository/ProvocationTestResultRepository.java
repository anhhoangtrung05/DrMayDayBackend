package com.example.drmayday.repository;

import com.example.drmayday.entity.ProvocationTestResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProvocationTestResultRepository extends JpaRepository<ProvocationTestResult, Long> {

    boolean existsByProvocationTestTypeId(Long provocationTestTypeId);

    List<ProvocationTestResult> findByProvocationTestSessionIdOrderByProvocationTestResultIdAsc(Long provocationTestSessionId);
}
