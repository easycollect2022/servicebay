package com.servicebay.service;

import com.servicebay.domain.AppUser;
import com.servicebay.domain.Notification;
import com.servicebay.repo.NotificationRepository;
import com.servicebay.web.dto.Dtos.NotificationList;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
public class NotificationService {
    private final NotificationRepository repo;
    private final ApplicationEventPublisher events;

    public NotificationService(NotificationRepository repo, ApplicationEventPublisher events) {
        this.repo = repo;
        this.events = events;
    }

    /** Saves an in-app notification and queues a push to the user's devices. */
    @Transactional
    public void notify(AppUser user, String type, String title, String body, Long bookingId, String link) {
        Notification n = new Notification();
        n.setUser(user);
        n.setType(type);
        n.setTitle(title);
        n.setBody(body);
        n.setBookingId(bookingId);
        repo.save(n);
        Map<String, String> data = new HashMap<>();
        data.put("type", type);
        if (bookingId != null) data.put("bookingId", bookingId.toString());
        if (link != null) data.put("link", link);
        events.publishEvent(new PushEvent(user.getId(), title, body, data));
    }

    @Transactional(readOnly = true)
    public NotificationList list(Long userId) {
        return new NotificationList(repo.countByUserIdAndReadFalse(userId),
                repo.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 50)).stream()
                        .map(Mappers::notification).toList());
    }

    @Transactional
    public void markAllRead(Long userId) {
        repo.markAllRead(userId);
    }
}
