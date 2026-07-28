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

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id")
    private Role role;

    private String username;
    private String email;
    
    @Column(name = "password_hash")
    private String passwordHash;
    
    private LocalDate dob;
    
    @Column(name = "is_banned")
    private Boolean isBanned; 
    
    @Column(name = "ban_expires_at")
    private LocalDateTime banExpiresAt; 
    
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

    @Column(name = "otp_code")
    private String otpCode;

    @Column(name = "otp_expires_at")
    private LocalDateTime otpExpiresAt;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "token_version", nullable = false)
    private Integer tokenVersion = 0;
    
    @Column(name = "deletion_scheduled_at")
    private LocalDateTime deletionScheduledAt;
    
    @PrePersist
    protected void onCreate() {
        if (this.isBanned == null) this.isBanned = false;
        if (this.isVerified == null) this.isVerified = false;
        if (this.role == null) {
            Role defaultRole = new Role();
            defaultRole.setId(5L); 
            this.role = defaultRole;
        }
        if(this.falseReportStrikes == null) this.falseReportStrikes = 0;
        if(this.shadowbannedReports == null) this.shadowbannedReports = false;
    }
}