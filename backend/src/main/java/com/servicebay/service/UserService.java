package com.servicebay.service;

import com.servicebay.domain.AppUser;
import com.servicebay.domain.DeviceToken;
import com.servicebay.domain.Merchant;
import com.servicebay.domain.Role;
import com.servicebay.repo.AppUserRepository;
import com.servicebay.repo.DeviceTokenRepository;
import com.servicebay.repo.MerchantRepository;
import com.servicebay.security.VerifiedToken;
import com.servicebay.web.ApiException;
import com.servicebay.web.dto.Dtos.MeResponse;
import com.servicebay.web.dto.Dtos.UpdateMeRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class UserService {
    private final AppUserRepository users;
    private final MerchantRepository merchants;
    private final DeviceTokenRepository tokens;

    public UserService(AppUserRepository users, MerchantRepository merchants, DeviceTokenRepository tokens) {
        this.users = users;
        this.merchants = merchants;
        this.tokens = tokens;
    }

    /** Finds the user for a verified Firebase identity, creating the row on first sign-in. */
    @Transactional
    public AppUser upsertFromToken(VerifiedToken t) {
        AppUser user = users.findByFirebaseUid(t.uid()).orElse(null);
        if (user == null) {
            user = new AppUser();
            user.setFirebaseUid(t.uid());
            user.setPhone(t.phone());
            user.setEmail(t.email());
            user.setName(t.name());
            try {
                return users.saveAndFlush(user);
            } catch (DataIntegrityViolationException race) {
                return users.findByFirebaseUid(t.uid()).orElseThrow();
            }
        }
        boolean dirty = false;
        if (user.getPhone() == null && t.phone() != null) { user.setPhone(t.phone()); dirty = true; }
        if (user.getEmail() == null && t.email() != null) { user.setEmail(t.email()); dirty = true; }
        if (user.getName() == null && t.name() != null) { user.setName(t.name()); dirty = true; }
        return dirty ? users.save(user) : user;
    }

    @Transactional(readOnly = true)
    public MeResponse me(Long userId) {
        AppUser u = users.findById(userId).orElseThrow(() -> ApiException.notFound("User"));
        Boolean onboarded = u.getRole() == Role.MERCHANT
                ? merchants.findByUserId(userId).map(Merchant::isOnboarded).orElse(false)
                : null;
        return new MeResponse(u.getId(), u.getRole(), u.getName(), u.getPhone(), u.getEmail(),
                u.getAddress(), u.getPincode(), onboarded);
    }

    /** Role is chosen once, right after the first sign-in. */
    @Transactional
    public MeResponse setRole(Long userId, Role role, String name) {
        AppUser u = users.findById(userId).orElseThrow(() -> ApiException.notFound("User"));
        if (u.getRole() != null && u.getRole() != role) {
            throw ApiException.conflict("ROLE_ALREADY_SET", "This account is already registered as " + u.getRole());
        }
        u.setRole(role);
        if (StringUtils.hasText(name)) u.setName(name.trim());
        if (role == Role.MERCHANT && merchants.findByUserId(userId).isEmpty()) {
            Merchant m = new Merchant();
            m.setUser(u);
            m.setPhone(u.getPhone());
            merchants.save(m);
        }
        return me(userId);
    }

    @Transactional
    public MeResponse update(Long userId, UpdateMeRequest req) {
        AppUser u = users.findById(userId).orElseThrow(() -> ApiException.notFound("User"));
        if (req.name() != null) u.setName(req.name().trim());
        if (req.address() != null) u.setAddress(req.address().trim());
        if (req.pincode() != null) u.setPincode(req.pincode());
        if (req.phone() != null) u.setPhone(req.phone().trim());
        return me(userId);
    }

    @Transactional
    public void registerDeviceToken(Long userId, String token) {
        AppUser u = users.getReferenceById(userId);
        DeviceToken dt = tokens.findByToken(token).orElseGet(DeviceToken::new);
        dt.setToken(token);
        dt.setUser(u);
        tokens.save(dt);
    }
}
