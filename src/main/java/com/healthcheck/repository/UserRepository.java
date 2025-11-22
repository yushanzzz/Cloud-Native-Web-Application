package com.healthcheck.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.healthcheck.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    @Query("SELECT u FROM User u WHERE u.email = :username")
    User findByUsername(String username);

    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.email = :username")
    boolean existsByUsername(String username);
}