package com.servicebay.service;

import com.servicebay.config.AppProperties;
import com.servicebay.domain.*;
import com.servicebay.repo.*;
import com.servicebay.security.CurrentUser;
import com.servicebay.web.ApiException;
import com.servicebay.web.dto.Dtos.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
public class BookingService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("EEE, d MMM, h:mm a", Locale.ENGLISH);

    private final BookingRepository bookings;
    private final MerchantRepository merchants;
    private final ServiceOfferingRepository services;
    private final AppUserRepository users;
    private final ReviewRepository reviews;
    private final NotificationService notifications;
    private final Clock clock;
    private final AppProperties props;

    public BookingService(BookingRepository bookings, MerchantRepository merchants, ServiceOfferingRepository services,
                          AppUserRepository users, ReviewRepository reviews, NotificationService notifications,
                          Clock clock, AppProperties props) {
        this.bookings = bookings;
        this.merchants = merchants;
        this.services = services;
        this.users = users;
        this.reviews = reviews;
        this.notifications = notifications;
        this.clock = clock;
        this.props = props;
    }

    // ================= customer =================

    @Transactional
    public CustomerBookingView create(CurrentUser me, CreateBookingRequest r) {
        Merchant m = merchants.findById(r.merchantId()).filter(Merchant::isOnboarded)
                .orElseThrow(() -> ApiException.notFound("Shop"));
        if (!m.isOnline()) throw ApiException.conflict("SHOP_OFFLINE", "This shop is not taking bookings right now");
        ServiceOffering s = services.findById(r.serviceId())
                .filter(x -> x.getMerchant().getId().equals(m.getId()) && x.isActive())
                .orElseThrow(() -> ApiException.notFound("Service"));

        LocalDateTime start = r.date().atTime(r.time());
        LocalDateTime now = LocalDateTime.now(clock);
        if (start.toLocalDate().isAfter(now.toLocalDate().plusDays(props.booking().daysAhead()))) {
            throw ApiException.badRequest("DATE_OUT_OF_RANGE", "Pick a date within the next " + props.booking().daysAhead() + " days");
        }
        Set<LocalDateTime> taken = new HashSet<>(bookings.findTakenSlots(m.getId(), start.toLocalDate().atStartOfDay(),
                start.toLocalDate().plusDays(1).atStartOfDay(), BookingStatus.ACTIVE));
        if (!SlotCalculator.isBookable(OpeningHours.of(m), start, s.getDurationMin(), now, taken, props.booking().leadMinutes())) {
            throw ApiException.conflict("SLOT_UNAVAILABLE", "That time slot is no longer available. Please pick another.");
        }

        AppUser customer = users.getReferenceById(me.userId());
        Booking b = new Booking();
        b.setCode(newCode());
        b.setCustomer(customer);
        b.setMerchant(m);
        b.setService(s);
        b.setServiceName(s.getName());
        b.setVehicleType(s.getVehicleType());
        b.setPrice(s.getPrice());
        b.setSlotStart(start);
        b.setStatus(BookingStatus.REQUESTED);
        b.setOtp(newOtp());
        b.setCustomerName(me.name());
        b.setCustomerPhone(r.phone().trim());
        b.setCustomerAddress(r.address().trim());
        b.setCustomerPincode(r.pincode());
        try {
            bookings.saveAndFlush(b);
        } catch (DataIntegrityViolationException e) {
            // uq_booking_active_slot: someone else took the slot a moment ago.
            throw ApiException.conflict("SLOT_UNAVAILABLE", "That time slot was just booked. Please pick another.");
        }

        // Remember the address and pincode for next time.
        AppUser cu = users.findById(me.userId()).orElseThrow();
        cu.setAddress(b.getCustomerAddress());
        cu.setPincode(b.getCustomerPincode());
        if (cu.getPhone() == null) cu.setPhone(b.getCustomerPhone());

        notifications.notify(m.getUser(), "BOOKING_REQUESTED", "New booking request",
                s.getName() + " · " + label(s.getVehicleType()) + " · " + WHEN.format(start) + " · ₹" + s.getPrice().stripTrailingZeros().toPlainString(),
                b.getId(), "/merchant/orders/" + b.getId());
        return Mappers.customerBooking(b, null);
    }

    @Transactional(readOnly = true)
    public List<CustomerBookingView> customerBookings(Long userId) {
        List<Booking> list = bookings.findByCustomerIdOrderBySlotStartDesc(userId);
        return list.stream().map(b -> Mappers.customerBooking(b,
                reviews.findByBookingId(b.getId()).map(rv -> (int) rv.getRating()).orElse(null))).toList();
    }

    @Transactional
    public CustomerBookingView cancel(Long userId, Long bookingId) {
        Booking b = customerOwned(userId, bookingId);
        requireStatus(b, BookingStatus.REQUESTED, BookingStatus.ACCEPTED);
        b.setStatus(BookingStatus.CANCELLED);
        notifications.notify(b.getMerchant().getUser(), "BOOKING_CANCELLED", "Booking cancelled",
                b.getServiceName() + " on " + WHEN.format(b.getSlotStart()) + " was cancelled by the customer",
                b.getId(), "/merchant/orders/" + b.getId());
        return Mappers.customerBooking(b, null);
    }

    // ================= merchant =================

    /** scope: pending | upcoming | history */
    @Transactional(readOnly = true)
    public List<MerchantBookingView> merchantBookings(Long userId, String scope) {
        Merchant m = ownMerchant(userId);
        Collection<BookingStatus> statuses = switch (scope == null ? "pending" : scope) {
            case "pending" -> EnumSet.of(BookingStatus.REQUESTED);
            case "upcoming" -> EnumSet.of(BookingStatus.ACCEPTED, BookingStatus.IN_PROGRESS);
            case "history" -> EnumSet.of(BookingStatus.COMPLETED, BookingStatus.DECLINED, BookingStatus.CANCELLED);
            default -> throw ApiException.badRequest("BAD_SCOPE", "scope must be pending, upcoming or history");
        };
        List<Booking> list = bookings.findByMerchantIdAndStatusInOrderBySlotStartAsc(m.getId(), statuses);
        if ("history".equals(scope)) list = list.reversed();
        return list.stream().map(Mappers::merchantBooking).toList();
    }

    @Transactional(readOnly = true)
    public MerchantBookingView merchantBooking(Long userId, Long bookingId) {
        return Mappers.merchantBooking(merchantOwned(userId, bookingId));
    }

    @Transactional
    public MerchantBookingView accept(Long userId, Long bookingId) {
        Booking b = merchantOwned(userId, bookingId);
        requireStatus(b, BookingStatus.REQUESTED);
        if (!b.getSlotStart().isAfter(LocalDateTime.now(clock))) {
            throw ApiException.conflict("SLOT_PASSED", "This booking's time has already passed");
        }
        b.setStatus(BookingStatus.ACCEPTED);
        b.setAcceptedAt(Instant.now(clock));
        notifications.notify(b.getCustomer(), "BOOKING_ACCEPTED", "Booking accepted ✓",
                b.getMerchant().getShopName() + " confirmed your " + b.getServiceName() + " on "
                        + WHEN.format(b.getSlotStart()) + ". Your start OTP is " + b.getOtp() + ".",
                b.getId(), "/bookings");
        return Mappers.merchantBooking(b);
    }

    @Transactional
    public MerchantBookingView decline(Long userId, Long bookingId) {
        Booking b = merchantOwned(userId, bookingId);
        requireStatus(b, BookingStatus.REQUESTED);
        b.setStatus(BookingStatus.DECLINED);
        notifications.notify(b.getCustomer(), "BOOKING_DECLINED", "Booking declined",
                b.getMerchant().getShopName() + " can't take your " + WHEN.format(b.getSlotStart())
                        + " slot. Try another time or shop.", b.getId(), "/bookings");
        return Mappers.merchantBooking(b);
    }

    /** Wrong attempts are saved even though the call fails, so the attempt limit holds. */
    @Transactional(noRollbackFor = ApiException.class)
    public MerchantBookingView start(Long userId, Long bookingId, String otp) {
        Booking b = merchantOwned(userId, bookingId);
        requireStatus(b, BookingStatus.ACCEPTED);
        int max = props.booking().maxOtpAttempts();
        if (b.getOtpAttempts() >= max) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "OTP_LOCKED",
                    "Too many wrong OTP attempts. Ask the customer to contact support.");
        }
        if (!MessageDigest.isEqual(b.getOtp().getBytes(StandardCharsets.UTF_8), otp.getBytes(StandardCharsets.UTF_8))) {
            b.setOtpAttempts(b.getOtpAttempts() + 1);
            int left = max - b.getOtpAttempts();
            throw ApiException.badRequest("OTP_INVALID",
                    left > 0 ? "Wrong OTP. " + left + " attempt" + (left == 1 ? "" : "s") + " left." : "Wrong OTP. No attempts left.");
        }
        b.setStatus(BookingStatus.IN_PROGRESS);
        b.setStartedAt(Instant.now(clock));
        notifications.notify(b.getCustomer(), "SERVICE_STARTED", "Service started",
                b.getMerchant().getShopName() + " has started your " + b.getServiceName() + ".", b.getId(), "/bookings");
        return Mappers.merchantBooking(b);
    }

    @Transactional
    public MerchantBookingView complete(Long userId, Long bookingId) {
        Booking b = merchantOwned(userId, bookingId);
        requireStatus(b, BookingStatus.IN_PROGRESS);
        b.setStatus(BookingStatus.COMPLETED);
        b.setCompletedAt(Instant.now(clock));
        notifications.notify(b.getCustomer(), "SERVICE_COMPLETED", "Service completed",
                b.getServiceName() + " at " + b.getMerchant().getShopName() + " is done. How was it? Tap to rate.",
                b.getId(), "/bookings");
        return Mappers.merchantBooking(b);
    }

    // ================= reminders =================

    /** Push "your service is in about an hour" once per accepted booking. */
    @Transactional
    public int sendDueReminders() {
        LocalDateTime now = LocalDateTime.now(clock);
        List<Booking> due = bookings.findDueForReminder(now, now.plusMinutes(props.booking().reminderMinutes()));
        for (Booking b : due) {
            b.setReminderSent(true);
            notifications.notify(b.getCustomer(), "BOOKING_REMINDER", "Reminder · your service is soon",
                    b.getServiceName() + " at " + b.getMerchant().getShopName() + " at "
                            + WHEN.format(b.getSlotStart()) + ". Keep OTP " + b.getOtp() + " ready.",
                    b.getId(), "/bookings");
        }
        return due.size();
    }

    // ================= helpers =================

    private Booking customerOwned(Long userId, Long bookingId) {
        Booking b = bookings.findById(bookingId).orElseThrow(() -> ApiException.notFound("Booking"));
        if (!b.getCustomer().getId().equals(userId)) throw ApiException.notFound("Booking");
        return b;
    }

    private Merchant ownMerchant(Long userId) {
        return merchants.findByUserId(userId).orElseThrow(() -> ApiException.notFound("Shop"));
    }

    private Booking merchantOwned(Long userId, Long bookingId) {
        Merchant m = ownMerchant(userId);
        Booking b = bookings.findById(bookingId).orElseThrow(() -> ApiException.notFound("Booking"));
        if (!b.getMerchant().getId().equals(m.getId())) throw ApiException.notFound("Booking");
        return b;
    }

    private static void requireStatus(Booking b, BookingStatus... allowed) {
        if (!Arrays.asList(allowed).contains(b.getStatus())) {
            throw ApiException.conflict("BAD_STATUS", "Booking is " + b.getStatus().name().toLowerCase().replace('_', ' '));
        }
    }

    private String newCode() {
        for (int attempt = 0; attempt < 10; attempt++) {
            StringBuilder sb = new StringBuilder("SB-");
            for (int i = 0; i < 6; i++) sb.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
            if (!bookings.existsByCode(sb.toString())) return sb.toString();
        }
        throw new IllegalStateException("Could not generate a unique booking code");
    }

    static String newOtp() {
        return String.format("%04d", RANDOM.nextInt(10_000));
    }

    private static String label(VehicleType v) {
        return v == VehicleType.BIKE ? "Bike" : "Car";
    }
}
