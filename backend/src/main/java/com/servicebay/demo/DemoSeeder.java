package com.servicebay.demo;

import com.servicebay.domain.*;
import com.servicebay.repo.AppUserRepository;
import com.servicebay.repo.MerchantRepository;
import com.servicebay.repo.ServiceOfferingRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.EnumSet;

/** Demo data on first start: two shops in pincode 560034 so customers have something to book. */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.seed-demo-data", havingValue = "true")
public class DemoSeeder implements ApplicationRunner {
    private final AppUserRepository users;
    private final MerchantRepository merchants;
    private final ServiceOfferingRepository services;

    public DemoSeeder(AppUserRepository users, MerchantRepository merchants, ServiceOfferingRepository services) {
        this.users = users;
        this.merchants = merchants;
        this.services = services;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (merchants.count() > 0) return;
        Merchant a = shop("9000012345", "Ravi Kumar", "Speed Auto Care", "#42, 5th Cross, 80 Feet Rd, Koramangala 4th Block", 12.9352, 77.6245, true, true, 0);
        add(a, ServiceCategory.WATER_WASH, VehicleType.BIKE, "Water wash", "Foam wash, chain lube, tyre polish", 149, 30);
        add(a, ServiceCategory.FULL_SERVICE, VehicleType.BIKE, "Full service", "Engine oil, filters, brakes, 25-point check", 1199, 180);
        add(a, ServiceCategory.OIL_CHANGE, VehicleType.BIKE, "Oil change", "Synthetic 10W-30", 449, 20);
        add(a, ServiceCategory.PUNCTURE, VehicleType.BIKE, "Puncture repair", "Tubeless / tube", 99, 15);
        add(a, ServiceCategory.BATTERY, VehicleType.BIKE, "Battery replacement", "Amaron 5Ah incl. fitting", 1650, 20);
        add(a, ServiceCategory.WATER_WASH, VehicleType.CAR, "Car foam wash", "Exterior foam wash and tyre dressing", 399, 45);
        add(a, ServiceCategory.FULL_SERVICE, VehicleType.CAR, "Car general service", "Oil, filters, 40-point check", 2499, 240);

        Merchant b = shop("9000054321", "Suresh", "Sri Ganesh Motors", "12, 1st Main, Ejipura", 12.9389, 77.6288, true, false, 1);
        add(b, ServiceCategory.PUNCTURE, VehicleType.BIKE, "Puncture repair", "Quick fix", 89, 15);
        add(b, ServiceCategory.TYRE, VehicleType.BIKE, "Tyre replacement", "Fitting charge, tyre extra", 150, 30);
        add(b, ServiceCategory.BATTERY, VehicleType.BIKE, "Battery check & charge", null, 120, 30);
        log.info("Demo data created: 2 shops in pincode 560034 (merchant logins 9000012345 and 9000054321)");
    }

    private Merchant shop(String mobile, String owner, String name, String address, double lat, double lng,
                          boolean bike, boolean car, int idx) {
        AppUser u = new AppUser();
        u.setFirebaseUid("dev-" + mobile);
        u.setPhone("+91" + mobile);
        u.setName(owner);
        u.setRole(Role.MERCHANT);
        users.save(u);
        Merchant m = new Merchant();
        m.setUser(u);
        m.setShopName(name);
        m.setPhone("+91 " + mobile);
        m.setAddress(address);
        m.setPincode("560034");
        m.setCity("Bengaluru");
        m.setLatitude(lat);
        m.setLongitude(lng);
        m.setServesBike(bike);
        m.setServesCar(car);
        m.setOpenDaySet(idx == 0 ? EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.SATURDAY) : EnumSet.allOf(DayOfWeek.class));
        m.setOpenTime(LocalTime.of(9, 0));
        m.setCloseTime(LocalTime.of(20, 0));
        m.setOnboarded(true);
        return merchants.save(m);
    }

    private void add(Merchant m, ServiceCategory c, VehicleType v, String name, String desc, int price, int minutes) {
        ServiceOffering s = new ServiceOffering();
        s.setMerchant(m);
        s.setCategory(c);
        s.setVehicleType(v);
        s.setName(name);
        s.setDescription(desc);
        s.setPrice(BigDecimal.valueOf(price));
        s.setDurationMin(minutes);
        services.save(s);
    }
}
