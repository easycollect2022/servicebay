package com.servicebay.web;

import com.servicebay.security.CurrentUser;
import com.servicebay.service.NotificationService;
import com.servicebay.service.UserService;
import com.servicebay.web.dto.Dtos.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/me")
public class MeController {
    private final UserService users;
    private final NotificationService notifications;

    public MeController(UserService users, NotificationService notifications) {
        this.users = users;
        this.notifications = notifications;
    }

    @GetMapping
    public MeResponse me(@AuthenticationPrincipal CurrentUser me) {
        return users.me(me.userId());
    }

    @PostMapping("/role")
    public MeResponse setRole(@AuthenticationPrincipal CurrentUser me, @Valid @RequestBody SetRoleRequest r) {
        return users.setRole(me.userId(), r.role(), r.name());
    }

    @PatchMapping
    public MeResponse update(@AuthenticationPrincipal CurrentUser me, @Valid @RequestBody UpdateMeRequest r) {
        return users.update(me.userId(), r);
    }

    @PutMapping("/device-token")
    public ResponseEntity<Void> deviceToken(@AuthenticationPrincipal CurrentUser me, @Valid @RequestBody FcmTokenRequest r) {
        users.registerDeviceToken(me.userId(), r.token());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/notifications")
    public NotificationList notifications(@AuthenticationPrincipal CurrentUser me) {
        return notifications.list(me.userId());
    }

    @PostMapping("/notifications/read-all")
    public ResponseEntity<Void> readAll(@AuthenticationPrincipal CurrentUser me) {
        notifications.markAllRead(me.userId());
        return ResponseEntity.noContent().build();
    }
}
