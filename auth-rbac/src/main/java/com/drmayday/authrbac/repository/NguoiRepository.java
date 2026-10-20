package com.drmayday.authrbac.repository;

import com.drmayday.authrbac.entity.Nguoi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NguoiRepository extends JpaRepository<Nguoi, Long> {

    Optional<Nguoi> findBySoDienThoai(String soDienThoai);

    boolean existsBySoDienThoai(String soDienThoai);
}
