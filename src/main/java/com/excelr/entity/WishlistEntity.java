package com.excelr.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "wishlist")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WishlistEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(nullable = false)
    private String itemId; 

    @Column(nullable = false)
    private String type; // "movie" or "event"

    @Column(nullable = false)
    private String title;

    @Column(length = 500)
    private String poster;

    private Double rating; 

    private String duration; 

    @Column(length = 500)
    private String genre; 

    private String venue; 

    private String date; 
}
