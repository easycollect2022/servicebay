package com.servicebay.repo;

import com.servicebay.domain.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByFirebaseUid(String firebaseUid);
}
