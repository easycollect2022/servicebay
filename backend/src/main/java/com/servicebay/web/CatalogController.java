package com.servicebay.web;

import com.servicebay.domain.ServiceCategory;
import com.servicebay.domain.VehicleType;
import com.servicebay.service.CatalogService;
import com.servicebay.service.ReviewService;
import com.servicebay.web.dto.Dtos;
import com.servicebay.web.dto.Dtos.*;
import jakarta.validation.constraints.Pattern;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/** Shop discovery for signed-in users. */
@Validated
@RestController
@RequestMapping("/api/merchants")
public class CatalogController {
    private final CatalogService catalog;
    private final ReviewService reviews;

    public CatalogController(CatalogService catalog, ReviewService reviews) {
        this.catalog = catalog;
        this.reviews = reviews;
    }

    @GetMapping
    public List<MerchantSummary> search(@RequestParam @Pattern(regexp = Dtos.PINCODE) String pincode,
                                        @RequestParam(required = false) VehicleType vehicle,
                                        @RequestParam(required = false) ServiceCategory category,
                                        @RequestParam(required = false) Double lat,
                                        @RequestParam(required = false) Double lng) {
        return catalog.search(pincode, vehicle, category, lat, lng);
    }

    @GetMapping("/{id}")
    public MerchantDetail detail(@PathVariable Long id,
                                 @RequestParam(required = false) Double lat,
                                 @RequestParam(required = false) Double lng) {
        return catalog.detail(id, lat, lng);
    }

    @GetMapping("/{id}/slots")
    public SlotsResponse slots(@PathVariable Long id,
                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                               @RequestParam(required = false) Long serviceId) {
        return catalog.slots(id, serviceId, date);
    }

    @GetMapping("/{id}/reviews")
    public ReviewSummary reviews(@PathVariable Long id) {
        return reviews.forMerchant(id, 30);
    }
}
