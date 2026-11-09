package com.servicebay.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/** One service inside a booking (price and duration copied at booking time). */
@Entity
@Table(name = "booking_item")
@Getter
@Setter
public class BookingItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id")
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id")
    private ServiceOffering service;

    @Column(nullable = false)
    private String serviceName;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(name = "duration_min", nullable = false)
    private int durationMin;
}
