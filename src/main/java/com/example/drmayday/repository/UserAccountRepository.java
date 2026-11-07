package com.example.drmayday.repository;

import com.example.drmayday.entity.UserAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByUsername(String username);

    @EntityGraph(attributePaths = {"person", "role"})
    Optional<UserAccount> findByPerson_PhoneNumber(String phoneNumber);

    boolean existsByUsername(String username);

    @Override
    @EntityGraph(attributePaths = {"person", "role"})
    Optional<UserAccount> findById(Long accountId);

    @Override
    @EntityGraph(attributePaths = {"person", "role"})
    Page<UserAccount> findAll(Pageable pageable);

    @Query(value = "SELECT u FROM UserAccount u JOIN FETCH u.person p JOIN FETCH u.role r " +
                   "WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%')) " +
                   "OR LOWER(p.fullName) LIKE LOWER(CONCAT('%', :search, '%')) " +
                   "OR LOWER(p.phoneNumber) LIKE LOWER(CONCAT('%', :search, '%'))",
           countQuery = "SELECT count(u) FROM UserAccount u JOIN u.person p " +
                        "WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%')) " +
                        "OR LOWER(p.fullName) LIKE LOWER(CONCAT('%', :search, '%')) " +
                        "OR LOWER(p.phoneNumber) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<UserAccount> searchWithDetails(@Param("search") String search, Pageable pageable);
}
