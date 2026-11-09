package com.servicebay.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Initialises the Firebase Admin SDK (token verification, FCM push, Storage).
 * Uses the service-account JSON at app.firebase.credentials-path, or Application Default Credentials.
 * Returns empty when neither is configured, so local dev and tests run without Firebase.
 */
@Slf4j
@Configuration
public class FirebaseConfig {

    @Bean
    public FirebaseAppHolder firebaseApp(AppProperties props) {
        AppProperties.Firebase fb = props.firebase();
        if (fb != null && StringUtils.hasText(fb.credentialsPath()) && !new java.io.File(fb.credentialsPath()).isFile()) {
            log.warn("Firebase credentials file {} not found: token verification and push are disabled", fb.credentialsPath());
            return new FirebaseAppHolder(null);
        }
        if (fb == null || (!StringUtils.hasText(fb.credentialsPath()) && !StringUtils.hasText(fb.projectId()))) {
            log.warn("Firebase not configured: token verification and push are disabled");
            return new FirebaseAppHolder(null);
        }
        if (!FirebaseApp.getApps().isEmpty()) {
            return new FirebaseAppHolder(FirebaseApp.getInstance());
        }
        try {
            GoogleCredentials credentials;
            if (StringUtils.hasText(fb.credentialsPath())) {
                try (InputStream in = new FileInputStream(fb.credentialsPath())) {
                    credentials = GoogleCredentials.fromStream(in);
                }
            } else {
                credentials = GoogleCredentials.getApplicationDefault();
            }
            FirebaseOptions.Builder options = FirebaseOptions.builder().setCredentials(credentials);
            if (StringUtils.hasText(fb.projectId())) options.setProjectId(fb.projectId());
            if (StringUtils.hasText(fb.storageBucket())) options.setStorageBucket(fb.storageBucket());
            log.info("Firebase Admin initialised for project {}", fb.projectId());
            return new FirebaseAppHolder(FirebaseApp.initializeApp(options.build()));
        } catch (IOException e) {
            throw new IllegalStateException("Could not initialise Firebase Admin SDK", e);
        }
    }
}
