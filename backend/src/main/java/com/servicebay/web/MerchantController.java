package com.servicebay.web;

import com.servicebay.security.CurrentUser;
import com.servicebay.service.BookingService;
import com.servicebay.service.MerchantService;
import com.servicebay.service.ReviewService;
import com.servicebay.web.dto.Dtos.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/** Merchant's own shop: profile, photos, services, orders, reviews. */
@RestController
@RequestMapping("/api/merchant")
public class MerchantController {
    private final MerchantService shop;
    private final BookingService bookings;
    private final ReviewService reviews;

    public MerchantController(MerchantService shop, BookingService bookings, ReviewService reviews) {
        this.shop = shop;
        this.bookings = bookings;
        this.reviews = reviews;
    }

    // ----- profile -----
    @GetMapping("/profile")
    public MerchantProfileView profile(@AuthenticationPrincipal CurrentUser me) {
        return shop.profile(me.userId());
    }

    @PutMapping("/profile")
    public MerchantProfileView updateProfile(@AuthenticationPrincipal CurrentUser me, @Valid @RequestBody MerchantProfileRequest r) {
        return shop.updateProfile(me.userId(), r);
    }

    @PutMapping("/online")
    public MerchantProfileView online(@AuthenticationPrincipal CurrentUser me, @RequestBody OnlineRequest r) {
        return shop.setOnline(me.userId(), r.online());
    }

    @PostMapping(value = "/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public MerchantProfileView addPhoto(@AuthenticationPrincipal CurrentUser me, @RequestParam("file") MultipartFile file) {
        return shop.addPhoto(me.userId(), file);
    }

    @DeleteMapping("/photos/{photoId}")
    public MerchantProfileView deletePhoto(@AuthenticationPrincipal CurrentUser me, @PathVariable Long photoId) {
        return shop.deletePhoto(me.userId(), photoId);
    }

    // ----- services -----
    @GetMapping("/services")
    public List<ServiceView> services(@AuthenticationPrincipal CurrentUser me) {
        return shop.listServices(me.userId());
    }

    @PostMapping("/services")
    @ResponseStatus(HttpStatus.CREATED)
    public ServiceView createService(@AuthenticationPrincipal CurrentUser me, @Valid @RequestBody ServiceRequest r) {
        return shop.createService(me.userId(), r);
    }

    @PutMapping("/services/{id}")
    public ServiceView updateService(@AuthenticationPrincipal CurrentUser me, @PathVariable Long id,
                                     @Valid @RequestBody ServiceRequest r) {
        return shop.updateService(me.userId(), id, r);
    }

    @DeleteMapping("/services/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteService(@AuthenticationPrincipal CurrentUser me, @PathVariable Long id) {
        shop.deactivateService(me.userId(), id);
    }

    @PostMapping(value = "/services/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ServiceView serviceImage(@AuthenticationPrincipal CurrentUser me, @PathVariable Long id,
                                    @RequestParam("file") MultipartFile file) {
        return shop.uploadServiceImage(me.userId(), id, file);
    }

    // ----- orders -----
    @GetMapping("/bookings")
    public List<MerchantBookingView> bookings(@AuthenticationPrincipal CurrentUser me,
                                              @RequestParam(defaultValue = "pending") String scope) {
        return bookings.merchantBookings(me.userId(), scope);
    }

    @GetMapping("/bookings/{id}")
    public MerchantBookingView booking(@AuthenticationPrincipal CurrentUser me, @PathVariable Long id) {
        return bookings.merchantBooking(me.userId(), id);
    }

    @PostMapping("/bookings/{id}/accept")
    public MerchantBookingView accept(@AuthenticationPrincipal CurrentUser me, @PathVariable Long id) {
        return bookings.accept(me.userId(), id);
    }

    @PostMapping("/bookings/{id}/decline")
    public MerchantBookingView decline(@AuthenticationPrincipal CurrentUser me, @PathVariable Long id) {
        return bookings.decline(me.userId(), id);
    }

    @PostMapping("/bookings/{id}/start")
    public MerchantBookingView start(@AuthenticationPrincipal CurrentUser me, @PathVariable Long id,
                                     @Valid @RequestBody StartServiceRequest r) {
        return bookings.start(me.userId(), id, r.otp());
    }

    @PostMapping("/bookings/{id}/complete")
    public MerchantBookingView complete(@AuthenticationPrincipal CurrentUser me, @PathVariable Long id) {
        return bookings.complete(me.userId(), id);
    }

    // ----- reviews -----
    @GetMapping("/reviews")
    public ReviewSummary reviews(@AuthenticationPrincipal CurrentUser me) {
        return reviews.forOwnShop(me.userId());
    }

    @PostMapping("/reviews/{id}/reply")
    public ReviewView reply(@AuthenticationPrincipal CurrentUser me, @PathVariable Long id, @Valid @RequestBody ReplyRequest r) {
        return reviews.reply(me.userId(), id, r.reply());
    }
}
