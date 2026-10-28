package com.example.drmayday.repository;

import com.example.drmayday.entity.VaiTro;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VaiTroRepository extends JpaRepository<VaiTro, Long> {

    Optional<VaiTro> findByTenVaiTro(String tenVaiTro);

    boolean existsByTenVaiTro(String tenVaiTro);
}
