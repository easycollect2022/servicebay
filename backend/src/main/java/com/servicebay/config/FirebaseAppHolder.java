package com.servicebay.config;

import com.google.firebase.FirebaseApp;

import java.util.Optional;

/** Wraps the (optional) FirebaseApp so other beans can work with or without Firebase configured. */
public record FirebaseAppHolder(FirebaseApp app) {
    public Optional<FirebaseApp> get() {
        return Optional.ofNullable(app);
    }
}
