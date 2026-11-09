package com.servicebay.repo;

import com.servicebay.domain.Review;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByMerchantIdOrderByCreatedAtDesc(Long merchantId, Pageable pageable);

    Optional<Review> findByBookingId(Long bookingId);

    boolean existsByBookingId(Long bookingId);

    @Query("select coalesce(avg(r.rating), 0), count(r) from Review r where r.merchant.id = :merchantId")
    List<Object[]> ratingStats(Long merchantId);

    @Query("select r.rating, count(r) from Review r where r.merchant.id = :merchantId group by r.rating")
    List<Object[]> ratingDistribution(Long merchantId);
}
