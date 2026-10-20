package com.drmayday.authrbac.repository;

import com.drmayday.authrbac.entity.VaiTro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VaiTroRepository extends JpaRepository<VaiTro, Long> {

    Optional<VaiTro> findByTenVaiTro(String tenVaiTro);

    boolean existsByTenVaiTro(String tenVaiTro);
}
