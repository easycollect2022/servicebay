package com.servicebay.service;

import java.time.LocalDateTime;

/** A half-open interval [start, end). */
public record TimeRange(LocalDateTime start, LocalDateTime end) {
    public boolean overlaps(LocalDateTime s, LocalDateTime e) {
        return s.isBefore(end) && e.isAfter(start);
    }
}
