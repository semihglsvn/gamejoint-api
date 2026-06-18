package com.gamejoint.gamejoint_api.controller;

import com.gamejoint.gamejoint_api.dto.ReportCreateRequest;
import com.gamejoint.gamejoint_api.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    /**
     * Endpoint: POST /api/reports
     * Allows a logged-in user to report a specific review.
     */
    @PostMapping
    public ResponseEntity<Map<String, String>> submitReport(
            @RequestAttribute("userId") Long userId,
            @RequestBody ReportCreateRequest request) {
        
        // The service layer takes the secure userId and your new DTO
        reportService.createReport(userId, request);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Report submitted successfully. Our moderation team will review it shortly."));
    }
}