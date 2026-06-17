package com.gamejoint.gamejoint_api.model;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "reviews")
@Data
@EntityListeners(AuditingEntityListener.class)
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Many reviews belong to One user
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Many reviews belong to One game
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    private Integer score;

    @Column(columnDefinition = "text")
    private String comment;

    // Tells Hibernate to save the actual word ('APPROVED') instead of a number (0, 1, 2)
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "enum('pending', 'approved', 'rejected')")
    private ReviewStatus status = ReviewStatus.APPROVED; // Sets the auto-approve default instantly

    @Column(name = "mod_cleared")
    private Boolean modCleared = false; // Maps tinyint(1) and defaults to 0/false

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // Defining the Enum directly inside the class keeps your architecture tidy
    public enum ReviewStatus {
        PENDING, 
        APPROVED, 
        REJECTED
    }
}