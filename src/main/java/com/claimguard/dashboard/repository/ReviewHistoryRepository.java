package com.claimguard.dashboard.repository;

import com.claimguard.dashboard.entity.ReviewHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewHistoryRepository extends JpaRepository<ReviewHistory, Long> {
    List<ReviewHistory> findByClaimIdOrderByActionTimestampAsc(Long claimId);
}
