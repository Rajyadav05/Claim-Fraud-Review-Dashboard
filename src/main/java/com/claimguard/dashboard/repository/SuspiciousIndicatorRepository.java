package com.claimguard.dashboard.repository;

import com.claimguard.dashboard.entity.SuspiciousIndicator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SuspiciousIndicatorRepository extends JpaRepository<SuspiciousIndicator, Long> {
    List<SuspiciousIndicator> findByClaimId(Long claimId);
}
