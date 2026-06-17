package com.gamejoint.gamejoint_api.repository;

import com.gamejoint.gamejoint_api.model.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    // Spring writes the SQL: SELECT EXISTS(SELECT 1 FROM reports WHERE reporter_id = ? AND review_id = ?)
    // This returns a simple true/false boolean, which is incredibly fast.
    boolean existsByReporterIdAndReviewId(Long reporterId, Long reviewId);

}