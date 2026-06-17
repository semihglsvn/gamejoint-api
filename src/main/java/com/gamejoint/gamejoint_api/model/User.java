package com.gamejoint.gamejoint_api.model;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@EntityListeners(AuditingEntityListener.class)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // This replaces the complex @ManyToMany junction table!
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id")
    private Role role;

    private String username;
    
    private String email;
    
    @Column(name = "password_hash")
    private String passwordHash;
    
    private LocalDate dob;
    
    @Column(name = "is_banned")
    private Boolean isBanned; // tinyint(1) translates to Boolean automatically
    
    @Column(name = "ban_expires_at")
    private LocalDateTime banExpiresAt; // datetime translates to LocalDateTime
    
    @Column(name = "false_report_strikes")
    private Integer falseReportStrikes;
    
    @Column(name = "shadowbanned_reports")
    private Boolean shadowbannedReports;
    
    @Column(name = "remember_token_hash")
    private String rememberTokenHash;
    
    @Column(name = "reset_token_hash")
    private String resetTokenHash;
    
    @Column(name = "reset_token_expires")
    private LocalDateTime resetTokenExpires;
    
    @Column(name = "is_verified")
    private Boolean isVerified;
    
    @Column(name = "verification_token")
    private String verificationToken;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}