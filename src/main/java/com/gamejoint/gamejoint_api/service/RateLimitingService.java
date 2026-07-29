package com.gamejoint.gamejoint_api.service;

import com.gamejoint.gamejoint_api.exception.RateLimitExceededException;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class RateLimitingService {

    private final Map<String, Bucket> loginBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> registerBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> emailBuckets = new ConcurrentHashMap<>();
    private final Map<Long, Bucket> reviewBuckets = new ConcurrentHashMap<>();
    private final Map<Long, Bucket> reportBuckets = new ConcurrentHashMap<>();

    // ==========================================
    // BUCKET CONFIGURATIONS
    // ==========================================

    private Bucket newLoginBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(5)
                .refillGreedy(5, Duration.ofMinutes(1))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    private Bucket newRegisterBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(1)
                .refillGreedy(1, Duration.ofHours(1))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    private Bucket newEmailBucket() {
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
        Bandwidth limit = Bandwidth.builder()
                .capacity(5)
                .refillGreedy(5, Duration.ofMinutes(1))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    private Bucket newReportBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(10)
                .refillGreedy(10, Duration.ofHours(1))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    // ==========================================
    // HELPER: FORMAT DYNAMIC WAIT TIME
    // ==========================================
    private void checkAndConsume(Bucket bucket, String actionName) {
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (!probe.isConsumed()) {
            long nanos = probe.getNanosToWaitForRefill();
            long minutes = TimeUnit.NANOSECONDS.toMinutes(nanos);
            long seconds = TimeUnit.NANOSECONDS.toSeconds(nanos) % 60;
            
            String timeMessage = minutes > 0 
                ? String.format("%d minute(s) and %d second(s)", minutes, seconds)
                : String.format("%d second(s)", seconds);
                
            throw new RateLimitExceededException(
                String.format("Too many %s attempts. Please try again in %s.", actionName, timeMessage)
            );
        }
    }

    // ==========================================
    // VERIFICATION & REFUND METHODS
    // ==========================================

    public void verifyLoginAttempt(String ip) {
        Bucket bucket = loginBuckets.computeIfAbsent(ip, k -> newLoginBucket());
        checkAndConsume(bucket, "login");
    }

    public void verifyRegistrationAttempt(String ip) {
        Bucket bucket = registerBuckets.computeIfAbsent(ip, k -> newRegisterBucket());
        checkAndConsume(bucket, "registration");
    }

    public void refundRegistrationAttempt(String ip) {
        Bucket bucket = registerBuckets.get(ip);
        if (bucket != null) {
            bucket.addTokens(1); // Refund token if request fails due to server error or validation
        }
    }

    public void verifyEmailTrigger(String ip) {
        Bucket bucket = emailBuckets.computeIfAbsent(ip, k -> newEmailBucket());
        checkAndConsume(bucket, "email");
    }

    public void verifyReviewSubmission(Long userId) {
        Bucket bucket = reviewBuckets.computeIfAbsent(userId, k -> newReviewBucket());
        checkAndConsume(bucket, "review");
    }

    public void verifyReportSubmission(Long userId) {
        Bucket bucket = reportBuckets.computeIfAbsent(userId, k -> newReportBucket());
        checkAndConsume(bucket, "report");
    }
}