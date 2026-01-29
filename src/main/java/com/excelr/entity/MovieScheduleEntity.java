package com.excelr.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "movie_schedules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class MovieScheduleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venue_id", nullable = false)
    @JsonIgnore // avoid serializing lazy venue proxy
    private VenueEntity venue; // FK to venues

    @Column(nullable = false)
    private Long tmdbMovieId; // TMDB movie ID (no MovieEntity needed)

    @Column(nullable = false)
    private LocalDate startDate; // First day movie appears

    @Column(nullable = false)
    private LocalDate endDate; // Last day movie appears

    @Column(columnDefinition = "TEXT", nullable = false)
    private String showtimes; // JSON array: ["09:00 AM", "12:00 PM", "03:00 PM"]

    @Column(nullable = false)
    private BigDecimal silverPrice;

    @Column(nullable = false)
    private BigDecimal goldPrice;

    @Column(nullable = false)
    private BigDecimal vipPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status; // ACTIVE, CANCELLED, COMPLETED

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}

