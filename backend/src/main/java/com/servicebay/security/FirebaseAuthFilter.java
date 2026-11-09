package com.servicebay.security;

import com.servicebay.domain.AppUser;
import com.servicebay.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/** Verifies "Authorization: Bearer <Firebase ID token>" and loads (or creates) the matching app_user. */
public class FirebaseAuthFilter extends OncePerRequestFilter {
    private final TokenVerifier verifier;
    private final UserService userService;

    public FirebaseAuthFilter(TokenVerifier verifier, UserService userService) {
        this.verifier = verifier;
        this.userService = userService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String header = req.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            verifier.verify(header.substring(7).trim()).ifPresent(token -> {
                AppUser user = userService.upsertFromToken(token);
                CurrentUser cu = new CurrentUser(user.getId(), user.getFirebaseUid(), user.getRole(),
                        user.getPhone(), user.getEmail(), user.getName());
                List<SimpleGrantedAuthority> authorities = user.getRole() == null
                        ? List.of(new SimpleGrantedAuthority("ROLE_USER"))
                        : List.of(new SimpleGrantedAuthority("ROLE_USER"),
                                  new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(cu, null, authorities));
            });
        }
        chain.doFilter(req, res);
    }
}
