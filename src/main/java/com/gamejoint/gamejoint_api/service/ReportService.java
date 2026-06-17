package com.gamejoint.gamejoint_api.service;

import com.gamejoint.gamejoint_api.dto.ReportCreateRequest;
import com.gamejoint.gamejoint_api.exception.AccountRestrictedException;
import com.gamejoint.gamejoint_api.exception.DuplicateResourceException;
import com.gamejoint.gamejoint_api.exception.ResourceNotFoundException;
import com.gamejoint.gamejoint_api.model.Report;
import com.gamejoint.gamejoint_api.model.Review;
import com.gamejoint.gamejoint_api.model.User;
import com.gamejoint.gamejoint_api.repository.ReportRepository;
import com.gamejoint.gamejoint_api.repository.ReviewRepository;
import com.gamejoint.gamejoint_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;

    @Transactional
    public void createReport(Long reporterId, ReportCreateRequest request) {
        
        User user = userRepository.findById(reporterId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
                
        Review review = reviewRepository.findById(request.getReviewId())
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        // 1. The Hard Ban Guard Clause
        if (user.getIsBanned() != null && user.getIsBanned()) {
            throw new AccountRestrictedException("Your account is restricted. You cannot submit reports.");
        }

        // 2. The Honeypot (Shadowban & Mod Cleared Logic)
        // We only check the boolean flags here. The 10-strike logic is safely handled by the Admin Panel.
        boolean isShadowbanned = user.getShadowbannedReports() != null && user.getShadowbannedReports();
        boolean isModCleared = review.getModCleared() != null && review.getModCleared();

        if (isShadowbanned || isModCleared) {
            // Silently drop the report into the void. The mobile app still gets a 200 OK.
            return; 
        }

        // 3. The Anti-Spam Guard Clause
        if (reportRepository.existsByReporterIdAndReviewId(reporterId, review.getId())) {
            throw new DuplicateResourceException("You have already reported this review.");
        }

        // 4. The Happy Path
        Report report = new Report();
        report.setReporter(user);
        report.setReview(review);
        
        String reasonsString = String.join(", ", request.getReasons());
        report.setReasons(reasonsString);
        
        reportRepository.save(report);
    }
}