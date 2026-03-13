package com.excelr.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "ratings", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "user_id", "item_id", "item_type" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RatingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler", "password" })
    @org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
    private UserEntity user;

    @Column(name = "item_id", nullable = false)
    private String itemId; // TMDB movie ID or event ID

    @Column(name = "item_type", nullable = false, length = 20)
    private String itemType; // "movie" or "event"

    @Column(nullable = false)
    private Integer rating; // 1-5 stars

    @Column(length = 1000)
    private String review; // Optional review text

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
