package com.servicebay.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.servicebay.domain.BookingStatus;
import com.servicebay.domain.Role;
import com.servicebay.domain.ServiceCategory;
import com.servicebay.domain.VehicleType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Request and response bodies for the REST API. */
public final class Dtos {
    private Dtos() {}

    public static final String PINCODE = "^[1-9][0-9]{5}$";
    public static final String PHONE = "^\\+?[0-9 ]{10,15}$";

    // ---------- user ----------
    public record MeResponse(Long id, Role role, String name, String phone, String email,
                             String address, String pincode, Boolean merchantOnboarded) {}

    public record SetRoleRequest(@NotNull Role role, @Size(max = 120) String name) {}

    public record UpdateMeRequest(@Size(max = 120) String name, @Size(max = 500) String address,
                                  @Pattern(regexp = PINCODE) String pincode,
                                  @Pattern(regexp = PHONE) String phone) {}

    public record FcmTokenRequest(@NotBlank @Size(max = 512) String token) {}

    // ---------- merchant profile ----------
    public record MerchantProfileRequest(
            @NotBlank @Size(max = 150) String shopName,
            @NotBlank @Pattern(regexp = PHONE) String phone,
            @NotBlank @Size(max = 500) String address,
            @NotBlank @Pattern(regexp = PINCODE) String pincode,
            @Size(max = 80) String city,
            @DecimalMin("-90") @DecimalMax("90") Double latitude,
            @DecimalMin("-180") @DecimalMax("180") Double longitude,
            boolean servesBike,
            boolean servesCar,
            @NotEmpty Set<DayOfWeek> openDays,
            @NotNull LocalTime openTime,
            @NotNull LocalTime closeTime,
            @Min(15) @Max(120) Integer slotMinutes) {}

    public record PhotoView(Long id, String url) {}

    public record MerchantProfileView(Long id, String shopName, String phone, String address, String pincode,
                                      String city, Double latitude, Double longitude, boolean servesBike,
                                      boolean servesCar, List<DayOfWeek> openDays, LocalTime openTime,
                                      LocalTime closeTime, int slotMinutes, boolean online, boolean onboarded,
                                      BigDecimal ratingAvg, int ratingCount, List<PhotoView> photos) {}

    public record OnlineRequest(boolean online) {}

    // ---------- services ----------
    public record ServiceRequest(@NotNull ServiceCategory category, @NotNull VehicleType vehicleType,
                                 @NotBlank @Size(max = 120) String name, @Size(max = 500) String description,
                                 @NotNull @DecimalMin("0") @Digits(integer = 8, fraction = 2) BigDecimal price,
                                 @Min(5) @Max(1440) int durationMin, Boolean active) {}

    public record ServiceView(Long id, ServiceCategory category, VehicleType vehicleType, String name,
                              String description, BigDecimal price, int durationMin, String imageUrl,
                              boolean active) {}

    // ---------- discovery ----------
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record MerchantSummary(Long id, String shopName, String address, String pincode, BigDecimal ratingAvg,
                                  int ratingCount, BigDecimal fromPrice, List<ServiceCategory> categories,
                                  String coverPhoto, boolean openNow, Double distanceKm,
                                  boolean servesBike, boolean servesCar) {}

    public record MerchantDetail(MerchantProfileView shop, List<ServiceView> services, boolean openNow,
                                 Double distanceKm) {}

    public record SlotView(LocalTime time, boolean available) {}

    public record SlotsResponse(LocalDate date, boolean open, List<SlotView> slots) {}

    // ---------- bookings ----------
    /** serviceIds: one or more services done one after another (serviceId kept for older clients). */
    public record CreateBookingRequest(@NotNull Long merchantId, Long serviceId,
                                       @Size(max = 10) List<@NotNull Long> serviceIds,
                                       @NotNull LocalDate date, @NotNull LocalTime time,
                                       @NotBlank @Size(max = 500) String address,
                                       @NotBlank @Pattern(regexp = PINCODE) String pincode,
                                       @NotBlank @Pattern(regexp = PHONE) String phone) {
        public List<Long> allServiceIds() {
            if (serviceIds != null && !serviceIds.isEmpty()) return serviceIds.stream().distinct().toList();
            return serviceId == null ? List.of() : List.of(serviceId);
        }
    }

    public record BookingItemView(Long serviceId, String name, BigDecimal price, int durationMin) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record CustomerBookingView(Long id, String code, Long merchantId, String shopName, String shopPhone,
                                      String shopAddress, Double shopLatitude, Double shopLongitude,
                                      String serviceName, List<BookingItemView> items, VehicleType vehicleType,
                                      BigDecimal price, int durationMin, LocalDateTime slotStart,
                                      LocalDateTime slotEnd, BookingStatus status, String otp,
                                      Integer myRating, Instant createdAt) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record MerchantBookingView(Long id, String code, String serviceName, List<BookingItemView> items,
                                      VehicleType vehicleType, BigDecimal price, int durationMin,
                                      LocalDateTime slotStart, LocalDateTime slotEnd, BookingStatus status,
                                      String customerName, String customerPincode, String customerPhone,
                                      String customerAddress, Instant createdAt) {}

    public record StartServiceRequest(@NotBlank @Pattern(regexp = "^[0-9]{4}$") String otp) {}

    // ---------- reviews ----------
    public record ReviewRequest(@Min(1) @Max(5) int rating, @Size(max = 1000) String comment) {}

    public record ReplyRequest(@NotBlank @Size(max = 1000) String reply) {}

    public record ReviewView(Long id, String customerName, int rating, String comment, String reply,
                             String serviceName, VehicleType vehicleType, Instant createdAt) {}

    public record ReviewSummary(BigDecimal ratingAvg, int ratingCount, Map<Integer, Long> distribution,
                                List<ReviewView> reviews) {}

    // ---------- notifications ----------
    public record NotificationView(Long id, String type, String title, String body, Long bookingId,
                                   boolean read, Instant createdAt) {}

    public record NotificationList(long unread, List<NotificationView> items) {}
}
