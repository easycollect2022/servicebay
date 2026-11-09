package com.servicebay.service;

import com.servicebay.domain.*;
import com.servicebay.web.dto.Dtos.*;

import java.util.List;

final class Mappers {
    private Mappers() {}

    static MerchantProfileView profile(Merchant m) {
        return new MerchantProfileView(m.getId(), m.getShopName(), m.getPhone(), m.getAddress(), m.getPincode(),
                m.getCity(), m.getLatitude(), m.getLongitude(), m.isServesBike(), m.isServesCar(),
                m.openDaySet().stream().sorted().toList(), m.getOpenTime(), m.getCloseTime(), m.getSlotMinutes(),
                m.isOnline(), m.isOnboarded(), m.getRatingAvg(), m.getRatingCount(),
                m.getPhotos().stream().map(p -> new PhotoView(p.getId(), p.getUrl())).toList());
    }

    static ServiceView service(ServiceOffering s) {
        return new ServiceView(s.getId(), s.getCategory(), s.getVehicleType(), s.getName(), s.getDescription(),
                s.getPrice(), s.getDurationMin(), s.getImageUrl(), s.isActive());
    }

    static CustomerBookingView customerBooking(Booking b, Integer myRating) {
        Merchant m = b.getMerchant();
        String otp = BookingStatus.OTP_VISIBLE.contains(b.getStatus()) ? b.getOtp() : null;
        return new CustomerBookingView(b.getId(), b.getCode(), m.getId(), m.getShopName(), m.getPhone(),
                m.getAddress(), m.getLatitude(), m.getLongitude(), b.getServiceName(), b.getVehicleType(),
                b.getPrice(), b.getSlotStart(), b.getStatus(), otp, myRating, b.getCreatedAt());
    }

    /** Customer phone and address are withheld until the merchant accepts. The OTP is never included. */
    static MerchantBookingView merchantBooking(Booking b) {
        boolean visible = BookingStatus.CONTACT_VISIBLE.contains(b.getStatus());
        return new MerchantBookingView(b.getId(), b.getCode(), b.getServiceName(), b.getVehicleType(), b.getPrice(),
                b.getSlotStart(), b.getStatus(), b.getCustomerName(), b.getCustomerPincode(),
                visible ? b.getCustomerPhone() : null, visible ? b.getCustomerAddress() : null, b.getCreatedAt());
    }

    static ReviewView review(Review r) {
        Booking b = r.getBooking();
        return new ReviewView(r.getId(), displayName(r.getCustomer().getName()), r.getRating(), r.getComment(),
                r.getReply(), b.getServiceName(), b.getVehicleType(), r.getCreatedAt());
    }

    static NotificationView notification(Notification n) {
        return new NotificationView(n.getId(), n.getType(), n.getTitle(), n.getBody(), n.getBookingId(),
                n.isRead(), n.getCreatedAt());
    }

    /** "Arjun Kumar" -> "Arjun K." so full names are not exposed publicly. */
    static String displayName(String name) {
        if (name == null || name.isBlank()) return "Customer";
        String[] parts = name.trim().split("\\s+");
        return parts.length == 1 ? parts[0] : parts[0] + " " + parts[parts.length - 1].charAt(0) + ".";
    }

    static <T> List<T> nonNull(List<T> list) {
        return list == null ? List.of() : list;
    }
}
