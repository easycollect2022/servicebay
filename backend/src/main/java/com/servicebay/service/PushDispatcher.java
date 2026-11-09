package com.servicebay.service;

import com.servicebay.repo.DeviceTokenRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

/** Sends push notifications only after the business transaction commits, off the request thread. */
@Component
public class PushDispatcher {
    private final DeviceTokenRepository tokens;
    private final PushSender sender;

    public PushDispatcher(DeviceTokenRepository tokens, PushSender sender) {
        this.tokens = tokens;
        this.sender = sender;
    }

    @Async
    @TransactionalEventListener(fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onPush(PushEvent e) {
        List<String> userTokens = tokens.findTokensByUserId(e.userId());
        List<String> invalid = sender.send(userTokens, e.title(), e.body(), e.data());
        if (!invalid.isEmpty()) tokens.deleteByTokenIn(invalid);
    }
}
