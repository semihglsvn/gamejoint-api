package com.gamejoint.gamejoint_api.service;

import com.gamejoint.gamejoint_api.exception.RateLimitExceededException;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitingService {

    // In-memory stores mapping an IP or UserID to their specific bucket
    private final Map<String, Bucket> loginBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> registerBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> emailBuckets = new ConcurrentHashMap<>();
    private final Map<Long, Bucket> reviewBuckets = new ConcurrentHashMap<>();
    private final Map<Long, Bucket> reportBuckets = new ConcurrentHashMap<>();

    // ==========================================
    // BUCKET CONFIGURATIONS (Modern Builder API)
    // ==========================================

    private Bucket newLoginBucket() {
        // 5 attempts per minute
        Bandwidth limit = Bandwidth.builder()
                .capacity(5)
                .refillGreedy(5, Duration.ofMinutes(1))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    private Bucket newRegisterBucket() {
        // 1 account creation per hour
        Bandwidth limit = Bandwidth.builder()
                .capacity(1)
                .refillGreedy(1, Duration.ofHours(1))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    private Bucket newEmailBucket() {
        // Dual-Layer Limit: Max 3 per hour AND Max 1 per 3 minutes
        Bandwidth hourlyLimit = Bandwidth.builder()
                .capacity(3)
                .refillGreedy(3, Duration.ofHours(1))
                .build();
                
        Bandwidth burstLimit = Bandwidth.builder()
                .capacity(1)
                .refillGreedy(1, Duration.ofMinutes(3))
                .build();
                
        return Bucket.builder()
                .addLimit(hourlyLimit)
                .addLimit(burstLimit)
                .build();
    }

    private Bucket newReviewBucket() {
        // 5 reviews per minute
        Bandwidth limit = Bandwidth.builder()
                .capacity(5)
                .refillGreedy(5, Duration.ofMinutes(1))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    private Bucket newReportBucket() {
        // 10 reports per hour
        Bandwidth limit = Bandwidth.builder()
                .capacity(10)
                .refillGreedy(10, Duration.ofHours(1))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    // ==========================================
    // VERIFICATION METHODS
    // ==========================================

    public void verifyLoginAttempt(String ip) {
        Bucket bucket = loginBuckets.computeIfAbsent(ip, k -> newLoginBucket());
        if (!bucket.tryConsume(1)) {
            throw new RateLimitExceededException("Too many login attempts. Please wait a minute.");
        }
    }

    public void verifyRegistrationAttempt(String ip) {
        Bucket bucket = registerBuckets.computeIfAbsent(ip, k -> newRegisterBucket());
        if (!bucket.tryConsume(1)) {
            throw new RateLimitExceededException("Registration limit reached for this network. Please try again later.");
        }
    }

    public void verifyEmailTrigger(String ip) {
        Bucket bucket = emailBuckets.computeIfAbsent(ip, k -> newEmailBucket());
        if (!bucket.tryConsume(1)) {
            throw new RateLimitExceededException("Too many email requests. Please wait a few minutes before trying again.");
        }
    }

    public void verifyReviewSubmission(Long userId) {
        Bucket bucket = reviewBuckets.computeIfAbsent(userId, k -> newReviewBucket());
        if (!bucket.tryConsume(1)) {
            throw new RateLimitExceededException("You are posting reviews too quickly. Please slow down.");
        }
    }

    public void verifyReportSubmission(Long userId) {
        Bucket bucket = reportBuckets.computeIfAbsent(userId, k -> newReportBucket());
        if (!bucket.tryConsume(1)) {
            throw new RateLimitExceededException("You have reached your report limit for this hour.");
        }
    }
}