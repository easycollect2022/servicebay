package com.servicebay.domain;

import java.util.EnumSet;
import java.util.Set;

public enum BookingStatus {
    REQUESTED, ACCEPTED, DECLINED, IN_PROGRESS, COMPLETED, CANCELLED;

    /** Statuses that hold the slot. Mirrors the partial unique index uq_booking_active_slot. */
    public static final Set<BookingStatus> ACTIVE = EnumSet.of(REQUESTED, ACCEPTED, IN_PROGRESS);

    /** Statuses in which the merchant may see the customer's address and phone. */
    public static final Set<BookingStatus> CONTACT_VISIBLE = EnumSet.of(ACCEPTED, IN_PROGRESS, COMPLETED);

    /** Statuses in which the customer still needs the start OTP. */
    public static final Set<BookingStatus> OTP_VISIBLE = EnumSet.of(REQUESTED, ACCEPTED);
}
