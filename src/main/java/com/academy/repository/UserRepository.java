package com.academy.repository;

import com.academy.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find user by username (for login)
     */
    Optional<User> findByUsername(String username);

    /**
     * Find user by portal ID (for public teacher portal)
     */
    Optional<User> findByPortalId(String portalId);

    /**
     * Check if username already exists
     */
    boolean existsByUsername(String username);

//    Optional<User> findByUsernameAndTeacherId(String username, Long teacherId);

}