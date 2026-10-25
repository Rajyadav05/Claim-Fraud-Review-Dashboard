package com.claimguard.dashboard.repository;

import com.claimguard.dashboard.entity.User;
import com.claimguard.dashboard.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByRole(UserRole role);

    boolean existsByUsername(String username);
}
