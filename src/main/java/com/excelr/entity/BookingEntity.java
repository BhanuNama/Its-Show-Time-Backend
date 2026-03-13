package com.excelr.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class BookingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Public booking reference shown to users (non-sequential).
     * Example: BK8F3KZ1P9Q2X
     */
    @Column(nullable = false, unique = true, length = 32)
    private String bookingCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler", "password" })
    @org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
    private UserEntity user; // FK to users

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingType type; // MOVIE, EVENT

    // For MOVIE bookings
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "show_id")
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    private ShowEntity show; // FK to shows (nullable)

    // For EVENT bookings
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler", "owner" })
    private EventEntity event; // FK to events (nullable)

    @Column(length = 50)
    private String eventDateId; // For events: which date from eventConfig.dates

    @Column(columnDefinition = "TEXT", nullable = false)
    private String bookingDetails; // JSON: seats for movies, zones for events

    @Column(nullable = false)
    private BigDecimal totalAmount;

    @Column(nullable = false, length = 50)
    private String paymentMethod; // "card", "upi", "netbanking", "wallet"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus paymentStatus; // PENDING, COMPLETED, FAILED, REFUNDED

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status; // CONFIRMED, CANCELLED, COMPLETED

    @Column(nullable = false)
    private LocalDateTime bookedAt;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
