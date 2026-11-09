package com.servicebay.service;

import com.servicebay.config.AppProperties;
import com.servicebay.domain.*;
import com.servicebay.repo.BookingRepository;
import com.servicebay.repo.MerchantRepository;
import com.servicebay.repo.ServiceOfferingRepository;
import com.servicebay.web.ApiException;
import com.servicebay.web.dto.Dtos.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/** What customers browse: shops by pincode, a shop's services, and free slots. */
@Service
public class CatalogService {
    private final MerchantRepository merchants;
    private final ServiceOfferingRepository services;
    private final BookingRepository bookings;
    private final Clock clock;
    private final AppProperties props;

    public CatalogService(MerchantRepository merchants, ServiceOfferingRepository services,
                          BookingRepository bookings, Clock clock, AppProperties props) {
        this.merchants = merchants;
        this.services = services;
        this.bookings = bookings;
        this.clock = clock;
        this.props = props;
    }

    @Transactional(readOnly = true)
    public List<MerchantSummary> search(String pincode, VehicleType vehicle, ServiceCategory category,
                                        Double lat, Double lng) {
        List<Merchant> shops = merchants.findByPincodeAndOnboardedTrue(pincode).stream()
                .filter(Merchant::isOnline)
                .filter(m -> vehicle == null || m.serves(vehicle))
                .toList();
        if (shops.isEmpty()) return List.of();
        Map<Long, List<ServiceOffering>> byShop = services
                .findByMerchantIdInAndActiveTrue(shops.stream().map(Merchant::getId).toList()).stream()
                .filter(s -> vehicle == null || s.getVehicleType() == vehicle)
                .collect(Collectors.groupingBy(s -> s.getMerchant().getId()));
        LocalDateTime now = LocalDateTime.now(clock);

        return shops.stream()
                .filter(m -> !byShop.getOrDefault(m.getId(), List.of()).isEmpty())
                .filter(m -> category == null || byShop.get(m.getId()).stream().anyMatch(s -> s.getCategory() == category))
                .map(m -> {
                    List<ServiceOffering> list = byShop.get(m.getId());
                    BigDecimal from = list.stream()
                            .filter(s -> category == null || s.getCategory() == category)
                            .map(ServiceOffering::getPrice).min(Comparator.naturalOrder()).orElse(null);
                    List<ServiceCategory> cats = list.stream().map(ServiceOffering::getCategory).distinct().sorted().toList();
                    String cover = m.getPhotos().isEmpty() ? null : m.getPhotos().get(0).getUrl();
                    return new MerchantSummary(m.getId(), m.getShopName(), m.getAddress(), m.getPincode(),
                            m.getRatingAvg(), m.getRatingCount(), from, cats, cover,
                            SlotCalculator.isOpenNow(OpeningHours.of(m), now),
                            SlotCalculator.distanceKm(lat, lng, m.getLatitude(), m.getLongitude()),
                            m.isServesBike(), m.isServesCar());
                })
                .sorted(Comparator.comparing(MerchantSummary::ratingAvg).reversed()
                        .thenComparing(MerchantSummary::ratingCount, Comparator.reverseOrder()))
                .toList();
    }

    @Transactional(readOnly = true)
    public MerchantDetail detail(Long merchantId, Double lat, Double lng) {
        Merchant m = publicShop(merchantId);
        List<ServiceView> list = services.findByMerchantIdAndActiveTrueOrderByPriceAsc(m.getId())
                .stream().map(Mappers::service).toList();
        return new MerchantDetail(Mappers.profile(m), list,
                SlotCalculator.isOpenNow(OpeningHours.of(m), LocalDateTime.now(clock)),
                SlotCalculator.distanceKm(lat, lng, m.getLatitude(), m.getLongitude()));
    }

    @Transactional(readOnly = true)
    public SlotsResponse slots(Long merchantId, List<Long> serviceIds, LocalDate date) {
        Merchant m = publicShop(merchantId);
        LocalDate today = LocalDate.now(clock);
        if (date.isBefore(today) || date.isAfter(today.plusDays(props.booking().daysAhead()))) {
            throw ApiException.badRequest("DATE_OUT_OF_RANGE",
                    "Pick a date within the next " + props.booking().daysAhead() + " days");
        }
        int duration = m.getSlotMinutes();
        if (serviceIds != null && !serviceIds.isEmpty()) {
            duration = resolveServices(m, serviceIds).stream().mapToInt(ServiceOffering::getDurationMin).sum();
        }
        OpeningHours h = OpeningHours.of(m);
        List<TimeRange> busy = bookings.findBusy(m.getId(), date.atStartOfDay(),
                date.plusDays(1).atStartOfDay(), BookingStatus.ACTIVE);
        return new SlotsResponse(date, SlotCalculator.isOpenOn(h, date),
                SlotCalculator.slots(h, date, duration, LocalDateTime.now(clock), busy, props.booking().leadMinutes()));
    }

    /** Active services of this shop, in the order given; all must be for the same vehicle type. */
    List<ServiceOffering> resolveServices(Merchant m, List<Long> ids) {
        if (ids.isEmpty()) throw ApiException.badRequest("NO_SERVICE", "Select at least one service");
        List<ServiceOffering> list = new ArrayList<>();
        for (Long id : ids.stream().distinct().toList()) {
            list.add(services.findById(id)
                    .filter(x -> x.getMerchant().getId().equals(m.getId()) && x.isActive())
                    .orElseThrow(() -> ApiException.notFound("Service")));
        }
        if (list.stream().map(ServiceOffering::getVehicleType).distinct().count() > 1) {
            throw ApiException.badRequest("MIXED_VEHICLES", "Book bike and car services separately");
        }
        return list;
    }

    Merchant publicShop(Long merchantId) {
        return merchants.findById(merchantId)
                .filter(Merchant::isOnboarded)
                .orElseThrow(() -> ApiException.notFound("Shop"));
    }
}
