package com.servicebay.service;

import com.servicebay.domain.Merchant;
import com.servicebay.domain.MerchantPhoto;
import com.servicebay.domain.ServiceOffering;
import com.servicebay.domain.VehicleType;
import com.servicebay.repo.MerchantRepository;
import com.servicebay.repo.ServiceOfferingRepository;
import com.servicebay.web.ApiException;
import com.servicebay.web.dto.Dtos.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/** Operations a merchant performs on their own shop. */
@Service
public class MerchantService {
    static final int MAX_PHOTOS = 5;

    private final MerchantRepository merchants;
    private final ServiceOfferingRepository services;
    private final StorageService storage;

    public MerchantService(MerchantRepository merchants, ServiceOfferingRepository services, StorageService storage) {
        this.merchants = merchants;
        this.services = services;
        this.storage = storage;
    }

    Merchant own(Long userId) {
        return merchants.findByUserId(userId).orElseThrow(() -> ApiException.notFound("Shop"));
    }

    @Transactional(readOnly = true)
    public MerchantProfileView profile(Long userId) {
        return Mappers.profile(own(userId));
    }

    @Transactional
    public MerchantProfileView updateProfile(Long userId, MerchantProfileRequest r) {
        if (!r.openTime().isBefore(r.closeTime())) {
            throw ApiException.badRequest("BAD_TIMINGS", "Closing time must be after opening time");
        }
        if (!r.servesBike() && !r.servesCar()) {
            throw ApiException.badRequest("NO_VEHICLE", "Select at least one vehicle type");
        }
        Merchant m = own(userId);
        m.setShopName(r.shopName().trim());
        m.setPhone(r.phone().trim());
        m.setAddress(r.address().trim());
        m.setPincode(r.pincode());
        m.setCity(r.city());
        m.setLatitude(r.latitude());
        m.setLongitude(r.longitude());
        m.setServesBike(r.servesBike());
        m.setServesCar(r.servesCar());
        m.setOpenDaySet(r.openDays());
        m.setOpenTime(r.openTime());
        m.setCloseTime(r.closeTime());
        if (r.slotMinutes() != null) m.setSlotMinutes(r.slotMinutes());
        m.setOnboarded(true);
        return Mappers.profile(m);
    }

    @Transactional
    public MerchantProfileView setOnline(Long userId, boolean online) {
        Merchant m = own(userId);
        m.setOnline(online);
        return Mappers.profile(m);
    }

    @Transactional
    public MerchantProfileView addPhoto(Long userId, MultipartFile file) {
        Merchant m = own(userId);
        if (m.getPhotos().size() >= MAX_PHOTOS) {
            throw ApiException.badRequest("TOO_MANY_PHOTOS", "You can upload up to " + MAX_PHOTOS + " photos");
        }
        MerchantPhoto p = new MerchantPhoto();
        p.setMerchant(m);
        p.setUrl(storage.storeImage(file, "shops/" + m.getId()));
        p.setSortOrder(m.getPhotos().size());
        m.getPhotos().add(p);
        merchants.flush();
        return Mappers.profile(m);
    }

    @Transactional
    public MerchantProfileView deletePhoto(Long userId, Long photoId) {
        Merchant m = own(userId);
        boolean removed = m.getPhotos().removeIf(p -> p.getId().equals(photoId));
        if (!removed) throw ApiException.notFound("Photo");
        return Mappers.profile(m);
    }

    // ---------- services ----------

    @Transactional(readOnly = true)
    public List<ServiceView> listServices(Long userId) {
        return services.findByMerchantIdOrderByVehicleTypeAscPriceAsc(own(userId).getId())
                .stream().map(Mappers::service).toList();
    }

    @Transactional
    public ServiceView createService(Long userId, ServiceRequest r) {
        Merchant m = own(userId);
        ServiceOffering s = new ServiceOffering();
        s.setMerchant(m);
        apply(m, s, r);
        return Mappers.service(services.save(s));
    }

    @Transactional
    public ServiceView updateService(Long userId, Long serviceId, ServiceRequest r) {
        Merchant m = own(userId);
        ServiceOffering s = ownService(m, serviceId);
        apply(m, s, r);
        return Mappers.service(s);
    }

    @Transactional
    public void deactivateService(Long userId, Long serviceId) {
        // Soft delete: past bookings keep referencing the service.
        ownService(own(userId), serviceId).setActive(false);
    }

    @Transactional
    public ServiceView uploadServiceImage(Long userId, Long serviceId, MultipartFile file) {
        Merchant m = own(userId);
        ServiceOffering s = ownService(m, serviceId);
        s.setImageUrl(storage.storeImage(file, "services/" + m.getId()));
        return Mappers.service(s);
    }

    private ServiceOffering ownService(Merchant m, Long serviceId) {
        ServiceOffering s = services.findById(serviceId).orElseThrow(() -> ApiException.notFound("Service"));
        if (!s.getMerchant().getId().equals(m.getId())) throw ApiException.notFound("Service");
        return s;
    }

    private static void apply(Merchant m, ServiceOffering s, ServiceRequest r) {
        if (!m.serves(r.vehicleType())) {
            throw ApiException.badRequest("VEHICLE_NOT_SERVED",
                    "Your shop is not set up for " + (r.vehicleType() == VehicleType.BIKE ? "bikes" : "cars"));
        }
        s.setCategory(r.category());
        s.setVehicleType(r.vehicleType());
        s.setName(r.name().trim());
        s.setDescription(r.description());
        s.setPrice(r.price());
        s.setDurationMin(r.durationMin());
        if (r.active() != null) s.setActive(r.active());
    }
}
