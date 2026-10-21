package com.claimguard.dashboard.repository;

import com.claimguard.dashboard.entity.ClaimDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClaimDocumentRepository extends JpaRepository<ClaimDocument, Long> {
    List<ClaimDocument> findByClaimId(Long claimId);
    long countByClaimId(Long claimId);
}
