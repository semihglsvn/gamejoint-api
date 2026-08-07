package com.gamejoint.gamejoint_api.repository;

import com.gamejoint.gamejoint_api.model.LinkedAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LinkedAccountRepository extends JpaRepository<LinkedAccount, Long> {
    
    Optional<LinkedAccount> findByProviderAndProviderId(String provider, String providerId);
    
    boolean existsByUserIdAndProvider(Long userId, String provider);
}