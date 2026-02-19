package com.academy.repository;

import com.academy.entity.StudentAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentAccountRepository extends JpaRepository<StudentAccount, Long> {

    /**
     * Find student account by username and password
     * Used for authentication
     */
    Optional<StudentAccount> findByUsernameAndPassword(String username, String password);

    /**
     * Find student account by username only
     */
    Optional<StudentAccount> findByUsername(String username);

    /**
     * Find student account by student ID
     */
    Optional<StudentAccount> findByStudentId(Long studentId);

    /**
     * Check if username already exists
     */
    boolean existsByUsername(String username);

    /**
     * Check if student already has an account
     */
    boolean existsByStudentId(Long studentId);
}