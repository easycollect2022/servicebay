package com.servicebay.security;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.servicebay.config.AppProperties;
import org.springframework.stereotype.Component;

import com.servicebay.config.FirebaseAppHolder;
import java.util.Optional;

@Component
public class TokenVerifier {
    private final Optional<FirebaseApp> firebaseApp;
    private final boolean devMode;

    public TokenVerifier(FirebaseAppHolder holder, AppProperties props) {
        this.firebaseApp = holder.get();
        this.devMode = props.auth() != null && props.auth().devMode();
    }

    /** @return the verified identity, or empty if the token is invalid. */
    public Optional<VerifiedToken> verify(String token) {
        if (devMode && token.startsWith("dev:")) {
            // dev:<uid>:<phone>:<name>  (local development and automated tests only)
            String[] p = token.split(":", 4);
            if (p.length < 2 || p[1].isBlank()) return Optional.empty();
            return Optional.of(new VerifiedToken(p[1], p.length > 2 ? p[2] : null, null, p.length > 3 ? p[3] : null));
        }
        if (firebaseApp.isEmpty()) return Optional.empty();
        try {
            FirebaseToken t = FirebaseAuth.getInstance(firebaseApp.get()).verifyIdToken(token, true);
            Object phone = t.getClaims().get("phone_number");
            return Optional.of(new VerifiedToken(t.getUid(), phone == null ? null : phone.toString(), t.getEmail(), t.getName()));
        } catch (FirebaseAuthException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
