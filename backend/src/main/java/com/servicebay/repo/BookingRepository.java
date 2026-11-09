package com.servicebay.repo;

import com.servicebay.domain.Booking;
import com.servicebay.domain.BookingStatus;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByCustomerIdOrderBySlotStartDesc(Long customerId);

    List<Booking> findByMerchantIdAndStatusInOrderBySlotStartAsc(Long merchantId, Collection<BookingStatus> statuses);

    List<Booking> findByMerchantIdOrderBySlotStartDesc(Long merchantId);

    /** Active bookings that overlap [from, to). */
    @Query("""
            select new com.servicebay.service.TimeRange(b.slotStart, b.slotEnd) from Booking b
            where b.merchant.id = :merchantId
              and b.slotStart < :to and b.slotEnd > :from
              and b.status in :statuses
            """)
    List<com.servicebay.service.TimeRange> findBusy(Long merchantId, LocalDateTime from, LocalDateTime to,
                                                    Collection<BookingStatus> statuses);

    /** SKIP LOCKED so several API instances never send the same reminder twice. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query("""
            select b from Booking b
            where b.status = com.servicebay.domain.BookingStatus.ACCEPTED
              and b.reminderSent = false
              and b.slotStart > :from and b.slotStart <= :to
            """)
    List<Booking> findDueForReminder(LocalDateTime from, LocalDateTime to);

    boolean existsByCode(String code);
}
