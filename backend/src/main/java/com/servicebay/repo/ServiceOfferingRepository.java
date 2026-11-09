package com.servicebay.repo;

import com.servicebay.domain.ServiceOffering;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ServiceOfferingRepository extends JpaRepository<ServiceOffering, Long> {
    List<ServiceOffering> findByMerchantIdOrderByVehicleTypeAscPriceAsc(Long merchantId);

    List<ServiceOffering> findByMerchantIdAndActiveTrueOrderByPriceAsc(Long merchantId);

    List<ServiceOffering> findByMerchantIdInAndActiveTrue(Collection<Long> merchantIds);
}
