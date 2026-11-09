package com.servicebay.security;

import com.servicebay.domain.Role;

/** The authenticated caller, resolved from the Firebase ID token and the app_user row. */
public record CurrentUser(Long userId, String firebaseUid, Role role, String phone, String email, String name) {
    public boolean isMerchant() { return role == Role.MERCHANT; }
}
