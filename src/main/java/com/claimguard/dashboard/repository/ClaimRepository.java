package com.claimguard.dashboard.repository;

import com.claimguard.dashboard.entity.Claim;
import com.claimguard.dashboard.entity.ClaimStatus;
import com.claimguard.dashboard.entity.RiskPriority;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface ClaimRepository extends JpaRepository<Claim, Long> {

    Optional<Claim> findByClaimReference(String claimReference);

    long countByPolicyNumberAndIncidentDate(String policyNumber, LocalDateTime incidentDate);

    @Query("SELECT c FROM Claim c WHERE " +
           "(:status IS NULL OR c.status = :status) AND " +
           "(:risk IS NULL OR c.riskPriority = :risk) AND " +
           "(:search IS NULL OR LOWER(c.claimReference) LIKE LOWER(CONCAT('%', :search, '%')) " +
           " OR LOWER(c.claimantName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           " OR LOWER(c.policyNumber) LIKE LOWER(CONCAT('%', :search, '%')) " +
           " OR LOWER(c.vehicleRegNumber) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Claim> findWithFilters(
            @Param("status") ClaimStatus status,
            @Param("risk") RiskPriority risk,
            @Param("search") String search,
            Pageable pageable
    );
}
