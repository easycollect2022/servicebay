package com.servicebay.service;

import com.servicebay.web.dto.Dtos.SlotView;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/** Pure slot logic: no Spring, no database, so it is fully unit-testable. */
public final class SlotCalculator {
    private SlotCalculator() {}

    public static boolean isOpenOn(OpeningHours h, LocalDate date) {
        return h.days().contains(date.getDayOfWeek());
    }

    public static boolean isOpenNow(OpeningHours h, LocalDateTime now) {
        LocalTime t = now.toLocalTime();
        return isOpenOn(h, now.toLocalDate()) && !t.isBefore(h.open()) && t.isBefore(h.close());
    }

    /**
     * Slots start every slotMinutes from opening time; a slot is offered only if the whole service
     * (durationMin) fits before closing. A slot is unavailable when the time it needs overlaps an existing
     * booking, or when it starts less than leadMinutes from now.
     */
    public static List<SlotView> slots(OpeningHours h, LocalDate date, int durationMin, LocalDateTime now,
                                       List<TimeRange> busy, int leadMinutes) {
        List<SlotView> out = new ArrayList<>();
        if (!isOpenOn(h, date) || !h.open().isBefore(h.close()) || h.slotMinutes() <= 0) return out;
        LocalDateTime earliest = now.plusMinutes(leadMinutes);
        LocalDateTime start = date.atTime(h.open());
        LocalDateTime close = date.atTime(h.close());
        while (!start.plusMinutes(durationMin).isAfter(close)) {
            LocalDateTime end = start.plusMinutes(durationMin);
            LocalDateTime s0 = start;
            boolean available = !start.isBefore(earliest) && busy.stream().noneMatch(b -> b.overlaps(s0, end));
            out.add(new SlotView(start.toLocalTime(), available));
            start = start.plusMinutes(h.slotMinutes());
        }
        return out;
    }

    /** True when the requested start is one of the offered, available slots. */
    public static boolean isBookable(OpeningHours h, LocalDateTime start, int durationMin, LocalDateTime now,
                                     List<TimeRange> busy, int leadMinutes) {
        return slots(h, start.toLocalDate(), durationMin, now, busy, leadMinutes).stream()
                .anyMatch(s -> s.available() && s.time().equals(start.toLocalTime()));
    }

    /** Great-circle distance in km, rounded to 0.1. */
    public static Double distanceKm(Double lat1, Double lon1, Double lat2, Double lon2) {
        if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) return null;
        double r = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1), dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return Math.round(r * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)) * 10) / 10.0;
    }
}
