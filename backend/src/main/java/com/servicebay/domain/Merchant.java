package com.servicebay.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "merchant")
@Getter
@Setter
public class Merchant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private AppUser user;

    private String shopName;
    private String phone;
    private String address;
    private String pincode;
    private String city;
    private Double latitude;
    private Double longitude;
    private boolean servesBike = true;
    private boolean servesCar;

    /** Comma separated three-letter days, e.g. "MON,TUE,WED". */
    private String openDays = "MON,TUE,WED,THU,FRI,SAT";
    private LocalTime openTime = LocalTime.of(9, 0);
    private LocalTime closeTime = LocalTime.of(20, 0);
    private int slotMinutes = 30;
    private boolean online = true;
    private boolean onboarded;
    private BigDecimal ratingAvg = BigDecimal.ZERO;
    private int ratingCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @OneToMany(mappedBy = "merchant", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<MerchantPhoto> photos = new ArrayList<>();

    public Set<DayOfWeek> openDaySet() {
        if (openDays == null || openDays.isBlank()) return EnumSet.noneOf(DayOfWeek.class);
        return Arrays.stream(openDays.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Merchant::parseDay)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(DayOfWeek.class)));
    }

    public void setOpenDaySet(Set<DayOfWeek> days) {
        this.openDays = days.stream().sorted()
                .map(d -> d.name().substring(0, 3))
                .collect(Collectors.joining(","));
    }

    public boolean serves(VehicleType type) {
        return type == VehicleType.BIKE ? servesBike : servesCar;
    }

    private static DayOfWeek parseDay(String s) {
        String u = s.toUpperCase();
        for (DayOfWeek d : DayOfWeek.values()) {
            if (d.name().startsWith(u)) return d;
        }
        throw new IllegalArgumentException("Unknown day: " + s);
    }
}
