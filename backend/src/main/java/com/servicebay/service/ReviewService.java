package com.servicebay.service;

import com.servicebay.domain.*;
import com.servicebay.repo.BookingRepository;
import com.servicebay.repo.MerchantRepository;
import com.servicebay.repo.ReviewRepository;
import com.servicebay.web.ApiException;
import com.servicebay.web.dto.Dtos.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class ReviewService {
    private final ReviewRepository reviews;
    private final BookingRepository bookings;
    private final MerchantRepository merchants;
    private final NotificationService notifications;
    private final Clock clock;

    public ReviewService(ReviewRepository reviews, BookingRepository bookings, MerchantRepository merchants,
                         NotificationService notifications, Clock clock) {
        this.reviews = reviews;
        this.bookings = bookings;
        this.merchants = merchants;
        this.notifications = notifications;
        this.clock = clock;
    }

    @Transactional
    public ReviewView create(Long userId, Long bookingId, ReviewRequest r) {
        Booking b = bookings.findById(bookingId)
                .filter(x -> x.getCustomer().getId().equals(userId))
                .orElseThrow(() -> ApiException.notFound("Booking"));
        if (b.getStatus() != BookingStatus.COMPLETED) {
            throw ApiException.conflict("NOT_COMPLETED", "You can rate a service once it is completed");
        }
        if (reviews.existsByBookingId(bookingId)) {
            throw ApiException.conflict("ALREADY_REVIEWED", "You have already rated this booking");
        }
        Review rv = new Review();
        rv.setBooking(b);
        rv.setMerchant(b.getMerchant());
        rv.setCustomer(b.getCustomer());
        rv.setRating((short) r.rating());
        rv.setComment(StringUtils.hasText(r.comment()) ? r.comment().trim() : null);
        reviews.saveAndFlush(rv);
        refreshRating(b.getMerchant());
        notifications.notify(b.getMerchant().getUser(), "NEW_REVIEW", "New " + r.rating() + "★ review",
                b.getServiceName() + (rv.getComment() != null ? ": " + truncate(rv.getComment(), 80) : ""),
                b.getId(), "/merchant/reviews");
        return Mappers.review(rv);
    }

    @Transactional(readOnly = true)
    public ReviewSummary forMerchant(Long merchantId, int limit) {
        Merchant m = merchants.findById(merchantId).orElseThrow(() -> ApiException.notFound("Shop"));
        Map<Integer, Long> dist = new TreeMap<>();
        for (int i = 1; i <= 5; i++) dist.put(i, 0L);
        for (Object[] row : reviews.ratingDistribution(merchantId)) {
            dist.put(((Number) row[0]).intValue(), ((Number) row[1]).longValue());
        }
        List<ReviewView> list = reviews.findByMerchantIdOrderByCreatedAtDesc(merchantId, PageRequest.of(0, limit))
                .stream().map(Mappers::review).toList();
        return new ReviewSummary(m.getRatingAvg(), m.getRatingCount(), dist, list);
    }

    @Transactional(readOnly = true)
    public ReviewSummary forOwnShop(Long userId) {
        Merchant m = merchants.findByUserId(userId).orElseThrow(() -> ApiException.notFound("Shop"));
        return forMerchant(m.getId(), 100);
    }

    @Transactional
    public ReviewView reply(Long userId, Long reviewId, String text) {
        Merchant m = merchants.findByUserId(userId).orElseThrow(() -> ApiException.notFound("Shop"));
        Review rv = reviews.findById(reviewId)
                .filter(x -> x.getMerchant().getId().equals(m.getId()))
                .orElseThrow(() -> ApiException.notFound("Review"));
        rv.setReply(text.trim());
        rv.setRepliedAt(Instant.now(clock));
        notifications.notify(rv.getCustomer(), "REVIEW_REPLY", m.getShopName() + " replied to your review",
                truncate(rv.getReply(), 120), rv.getBooking().getId(), "/bookings");
        return Mappers.review(rv);
    }

    private void refreshRating(Merchant m) {
        Object[] row = reviews.ratingStats(m.getId()).get(0);
        double avg = ((Number) row[0]).doubleValue();
        m.setRatingAvg(BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP));
        m.setRatingCount(((Number) row[1]).intValue());
    }

    private static String truncate(String s, int n) {
        return s.length() <= n ? s : s.substring(0, n - 1) + "…";
    }
}
