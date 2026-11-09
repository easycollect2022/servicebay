package com.servicebay.service;

import com.servicebay.web.dto.Dtos.SlotView;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SlotCalculatorTest {
    // Mon–Sat, 09:00–20:00, 30-minute slots
    private final OpeningHours hours = new OpeningHours(
            EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.SATURDAY), LocalTime.of(9, 0), LocalTime.of(20, 0), 30);
    private final LocalDate tuesday = LocalDate.of(2026, 11, 10);
    private final LocalDateTime mondayMorning = LocalDateTime.of(2026, 11, 9, 8, 0);

    @Test
    void closedDayHasNoSlots() {
        assertThat(SlotCalculator.slots(hours, LocalDate.of(2026, 11, 15), 30, mondayMorning, Set.of(), 30)).isEmpty();
        assertThat(SlotCalculator.isOpenOn(hours, LocalDate.of(2026, 11, 15))).isFalse();
    }

    @Test
    void slotsRunFromOpeningUntilServiceFitsBeforeClosing() {
        List<SlotView> s = SlotCalculator.slots(hours, tuesday, 30, mondayMorning, Set.of(), 30);
        assertThat(s).hasSize(22);
        assertThat(s.getFirst().time()).isEqualTo(LocalTime.of(9, 0));
        assertThat(s.getLast().time()).isEqualTo(LocalTime.of(19, 30));
        assertThat(s).allMatch(SlotView::available);
    }

    @Test
    void longServiceMustFinishBeforeClosing() {
        List<SlotView> s = SlotCalculator.slots(hours, tuesday, 180, mondayMorning, Set.of(), 30);
        assertThat(s.getLast().time()).isEqualTo(LocalTime.of(17, 0));
    }

    @Test
    void takenSlotIsUnavailable() {
        LocalDateTime taken = tuesday.atTime(16, 30);
        List<SlotView> s = SlotCalculator.slots(hours, tuesday, 30, mondayMorning, Set.of(taken), 30);
        assertThat(s).filteredOn(x -> x.time().equals(LocalTime.of(16, 30))).singleElement()
                .extracting(SlotView::available).isEqualTo(false);
        assertThat(SlotCalculator.isBookable(hours, taken, 30, mondayMorning, Set.of(taken), 30)).isFalse();
    }

    @Test
    void slotsInsideLeadTimeAreUnavailable() {
        LocalDateTime now = LocalDateTime.of(2026, 11, 10, 12, 10);
        List<SlotView> s = SlotCalculator.slots(hours, tuesday, 30, now, Set.of(), 30);
        assertThat(s).filteredOn(SlotView::available).first().extracting(SlotView::time).isEqualTo(LocalTime.of(13, 0));
        assertThat(SlotCalculator.isBookable(hours, tuesday.atTime(12, 30), 30, now, Set.of(), 30)).isFalse();
    }

    @Test
    void offGridTimeIsNotBookable() {
        assertThat(SlotCalculator.isBookable(hours, tuesday.atTime(10, 15), 30, mondayMorning, Set.of(), 30)).isFalse();
        assertThat(SlotCalculator.isBookable(hours, tuesday.atTime(10, 30), 30, mondayMorning, Set.of(), 30)).isTrue();
    }

    @Test
    void openNowRespectsDayAndHours() {
        assertThat(SlotCalculator.isOpenNow(hours, LocalDateTime.of(2026, 11, 9, 10, 0))).isTrue();
        assertThat(SlotCalculator.isOpenNow(hours, LocalDateTime.of(2026, 11, 9, 20, 0))).isFalse();
        assertThat(SlotCalculator.isOpenNow(hours, LocalDateTime.of(2026, 11, 15, 10, 0))).isFalse();
    }

    @Test
    void distanceIsRoundedKm() {
        assertThat(SlotCalculator.distanceKm(12.9352, 77.6245, 12.9279, 77.6271)).isBetween(0.7, 0.9);
        assertThat(SlotCalculator.distanceKm(null, 77.0, 12.0, 77.0)).isNull();
    }

    @Test
    void otpIsFourDigits() {
        for (int i = 0; i < 200; i++) assertThat(BookingService.newOtp()).matches("\\d{4}");
    }

    @Test
    void displayNameHidesSurname() {
        assertThat(Mappers.displayName("Arjun Kumar")).isEqualTo("Arjun K.");
        assertThat(Mappers.displayName("Meera")).isEqualTo("Meera");
        assertThat(Mappers.displayName(null)).isEqualTo("Customer");
    }
}
