package com.drmayday.authrbac.repository;

import com.drmayday.authrbac.entity.BenhNhan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BenhNhanRepository extends JpaRepository<BenhNhan, Long> {

    Optional<BenhNhan> findByMaBenhNhan(Long maBenhNhan);
}
