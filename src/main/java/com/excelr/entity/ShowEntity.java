package com.excelr.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "shows")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class ShowEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "venue_id", nullable = false)
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler", "owner" })
    private VenueEntity venue; // FK to venues

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    @JsonIgnore // avoid serializing lazy schedule proxy in booking responses
    private MovieScheduleEntity schedule; // FK to movie_schedules

    @Column(nullable = false)
    private Long tmdbMovieId; // TMDB movie ID

    @Column(nullable = false)
    private LocalDate showDate; // Specific date (e.g., 2026-01-05)

    @Column(nullable = false, length = 20)
    private String showTime; // "09:00 AM", "12:00 PM"

    @Column(nullable = false)
    private BigDecimal silverPrice;

    @Column(nullable = false)
    private BigDecimal goldPrice;

    @Column(nullable = false)
    private BigDecimal vipPrice;

    @Column(columnDefinition = "TEXT")
    private String seatState; // JSON: { takenSeats: ["A1","A2"], vipRows: ["L","M"], totalSeats: 150 }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status; // ACTIVE, CANCELLED, COMPLETED

    @CreationTimestamp
    private LocalDateTime createdAt;
}
