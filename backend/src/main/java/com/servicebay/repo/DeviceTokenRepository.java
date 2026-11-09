package com.servicebay.repo;

import com.servicebay.domain.DeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {
    Optional<DeviceToken> findByToken(String token);

    @Query("select t.token from DeviceToken t where t.user.id = :userId")
    List<String> findTokensByUserId(Long userId);

    @Modifying
    @Query("delete from DeviceToken t where t.token in :tokens")
    void deleteByTokenIn(List<String> tokens);
}
