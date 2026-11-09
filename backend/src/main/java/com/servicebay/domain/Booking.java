package com.servicebay.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "booking")
@Getter
@Setter
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id")
    private AppUser customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "merchant_id")
    private Merchant merchant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id")
    private ServiceOffering service;

    @Column(nullable = false)
    private String serviceName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VehicleType vehicleType;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(name = "slot_start", nullable = false)
    private LocalDateTime slotStart;

    @Column(name = "slot_end", nullable = false)
    private LocalDateTime slotEnd;

    @Column(name = "duration_min", nullable = false)
    private int durationMin;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<BookingItem> items = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status;

    @Column(nullable = false)
    private String otp;

    private int otpAttempts;

    private String customerName;
    @Column(nullable = false)
    private String customerPhone;
    @Column(nullable = false)
    private String customerAddress;
    @Column(nullable = false)
    private String customerPincode;

    private boolean reminderSent;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
    private Instant acceptedAt;
    private Instant startedAt;
    private Instant completedAt;
}
