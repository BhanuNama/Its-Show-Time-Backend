package com.excelr.repository;

import com.excelr.entity.BookingEntity;
import com.excelr.entity.EventEntity;
import com.excelr.entity.ShowEntity;
import com.excelr.entity.Status;
import com.excelr.entity.UserEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<BookingEntity, Long> {

    @EntityGraph(attributePaths = {"user", "show", "show.venue", "show.schedule", "show.schedule.venue", "event", "event.venue"})
    List<BookingEntity> findByUser(UserEntity user);

    long countByShowIn(List<ShowEntity> shows);
    
    @EntityGraph(attributePaths = {"user", "show", "show.venue", "show.schedule", "show.schedule.venue", "event", "event.venue"})
    Optional<BookingEntity> findById(Long id);

    // Find bookings by show ID and status
    List<BookingEntity> findByShowIdAndStatus(Long showId, Status status);

    // Find bookings by event ID, event date ID, and status
    List<BookingEntity> findByEventIdAndEventDateIdAndStatus(Long eventId, String eventDateId, Status status);
    
    // Find bookings by event and status
    List<BookingEntity> findByEventAndStatus(EventEntity event, Status status);
    
    // Find bookings where show is in list and status matches
    List<BookingEntity> findByShowInAndStatus(List<ShowEntity> shows, Status status);

    boolean existsByBookingCode(String bookingCode);

    @EntityGraph(attributePaths = {"user", "show", "show.venue", "show.schedule", "show.schedule.venue", "event", "event.venue"})
    Optional<BookingEntity> findByBookingCode(String bookingCode);

}

