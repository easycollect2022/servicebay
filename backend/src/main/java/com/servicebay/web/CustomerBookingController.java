package com.servicebay.web;

import com.servicebay.security.CurrentUser;
import com.servicebay.service.BookingService;
import com.servicebay.service.ReviewService;
import com.servicebay.web.dto.Dtos.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class CustomerBookingController {
    private final BookingService bookings;
    private final ReviewService reviews;

    public CustomerBookingController(BookingService bookings, ReviewService reviews) {
        this.bookings = bookings;
        this.reviews = reviews;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerBookingView create(@AuthenticationPrincipal CurrentUser me, @Valid @RequestBody CreateBookingRequest r) {
        return bookings.create(me, r);
    }

    @GetMapping
    public List<CustomerBookingView> mine(@AuthenticationPrincipal CurrentUser me) {
        return bookings.customerBookings(me.userId());
    }

    @PostMapping("/{id}/cancel")
    public CustomerBookingView cancel(@AuthenticationPrincipal CurrentUser me, @PathVariable Long id) {
        return bookings.cancel(me.userId(), id);
    }

    @PostMapping("/{id}/review")
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewView review(@AuthenticationPrincipal CurrentUser me, @PathVariable Long id,
                             @Valid @RequestBody ReviewRequest r) {
        return reviews.create(me.userId(), id, r);
    }
}
