package com.servicebay.service;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;
import com.servicebay.config.FirebaseAppHolder;
import java.util.Optional;

@Slf4j
@Configuration
public class PushConfig {

    @Bean
    public PushSender pushSender(FirebaseAppHolder holder) {
        Optional<FirebaseApp> app = holder.get();
        if (app.isEmpty()) {
            return (tokens, title, body, data) -> {
                log.info("[push disabled] to {} device(s): {} - {}", tokens.size(), title, body);
                return List.of();
            };
        }
        FirebaseMessaging fcm = FirebaseMessaging.getInstance(app.get());
        return (tokens, title, body, data) -> {
            if (tokens.isEmpty()) return List.of();
            MulticastMessage msg = MulticastMessage.builder()
                    .addAllTokens(tokens)
                    .putAllData(data)
                    .setNotification(Notification.builder().setTitle(title).setBody(body).build())
                    .setWebpushConfig(WebpushConfig.builder()
                            .setNotification(WebpushNotification.builder()
                                    .setTitle(title).setBody(body).setIcon("/icons/icon-192.png").build())
                            .setFcmOptions(WebpushFcmOptions.withLink(data.getOrDefault("link", "/")))
                            .build())
                    .build();
            List<String> invalid = new ArrayList<>();
            try {
                BatchResponse res = fcm.sendEachForMulticast(msg);
                for (int i = 0; i < res.getResponses().size(); i++) {
                    SendResponse r = res.getResponses().get(i);
                    if (!r.isSuccessful() && r.getException() != null) {
                        MessagingErrorCode code = r.getException().getMessagingErrorCode();
                        if (code == MessagingErrorCode.UNREGISTERED || code == MessagingErrorCode.INVALID_ARGUMENT) {
                            invalid.add(tokens.get(i));
                        }
                    }
                }
            } catch (FirebaseMessagingException e) {
                log.warn("FCM send failed: {}", e.getMessage());
            }
            return invalid;
        };
    }
}
