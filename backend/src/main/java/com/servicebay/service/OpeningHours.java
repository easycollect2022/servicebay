package com.servicebay.service;

import com.servicebay.domain.Merchant;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;

public record OpeningHours(Set<DayOfWeek> days, LocalTime open, LocalTime close, int slotMinutes) {
    public static OpeningHours of(Merchant m) {
        return new OpeningHours(m.openDaySet(), m.getOpenTime(), m.getCloseTime(), m.getSlotMinutes());
    }
}
