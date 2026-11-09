package com.servicebay.repo;

import com.servicebay.domain.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MerchantRepository extends JpaRepository<Merchant, Long> {
    Optional<Merchant> findByUserId(Long userId);

    List<Merchant> findByPincodeAndOnboardedTrue(String pincode);
}
