package com.gamejoint.gamejoint_api.repository;

import com.gamejoint.gamejoint_api.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    // You will need this later for the mobile app login screen
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String mail);
}