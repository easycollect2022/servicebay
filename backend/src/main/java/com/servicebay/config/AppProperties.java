package com.servicebay.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String zone,
        String publicBaseUrl,
        Auth auth,
        Firebase firebase,
        Storage storage,
        Cors cors,
        BookingRules booking) {

    /** devMode: accept "Bearer dev:<uid>:<phone>:<name>" tokens. Never enable in production. */
    public record Auth(boolean devMode) {}

    public record Firebase(String credentialsPath, String projectId, String storageBucket) {}

    /** type: local | firebase */
    public record Storage(String type, String localDir, long maxBytes) {}

    public record Cors(List<String> allowedOrigins) {}

    public record BookingRules(int leadMinutes, int daysAhead, int maxOtpAttempts, int reminderMinutes) {}
}
