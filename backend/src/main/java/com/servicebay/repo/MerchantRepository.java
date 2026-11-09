package com.servicebay.repo;

import com.servicebay.domain.Merchant;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface MerchantRepository extends JpaRepository<Merchant, Long> {
    Optional<Merchant> findByUserId(Long userId);

    List<Merchant> findByPincodeAndOnboardedTrue(String pincode);

    /** Row lock so two bookings for the same shop are checked one after another (no overlapping slots). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Merchant m where m.id = :id")
    Optional<Merchant> findByIdForUpdate(Long id);
}
