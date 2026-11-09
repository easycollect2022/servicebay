package com.servicebay.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.jobs.enabled", havingValue = "true", matchIfMissing = true)
public class ReminderJob {
    private final BookingService bookings;

    public ReminderJob(BookingService bookings) {
        this.bookings = bookings;
    }

    @Scheduled(fixedDelayString = "PT5M", initialDelayString = "PT1M")
    public void run() {
        int n = bookings.sendDueReminders();
        if (n > 0) log.info("Sent {} booking reminder(s)", n);
    }
}
