package com.gamejoint.gamejoint_api.repository;

import com.gamejoint.gamejoint_api.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    // You will use this during Registration to find the default role (e.g., "USER" or "ROLE_USER")
    Optional<Role> findByRoleName(String roleName);
    
}