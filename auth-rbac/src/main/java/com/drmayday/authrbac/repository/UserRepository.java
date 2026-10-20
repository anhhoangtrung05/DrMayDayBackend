package com.drmayday.authrbac.repository;

import com.drmayday.authrbac.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByPhone(String phone);

    boolean existsByPhone(String phone);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.patient LEFT JOIN FETCH u.staff ORDER BY u.userId DESC")
    Page<User> findAllUsersWithRoles(Pageable pageable);
}
