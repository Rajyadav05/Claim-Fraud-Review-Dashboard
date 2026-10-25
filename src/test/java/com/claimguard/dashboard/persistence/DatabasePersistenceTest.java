package com.claimguard.dashboard.persistence;

import com.claimguard.dashboard.ClaimGuardApplication;
import com.claimguard.dashboard.dto.ClaimDetailDTO;
import com.claimguard.dashboard.dto.CreateClaimRequest;
import com.claimguard.dashboard.entity.Claim;
import com.claimguard.dashboard.entity.ClaimStatus;
import com.claimguard.dashboard.entity.ClaimType;
import com.claimguard.dashboard.entity.ReviewHistory;
import com.claimguard.dashboard.repository.ClaimRepository;
import com.claimguard.dashboard.repository.ReviewHistoryRepository;
import com.claimguard.dashboard.service.ClaimService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class DatabasePersistenceTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Verify file-backed H2 persists claims, audit history, and maintains idempotency across restarts")
    void testFileBackedH2PersistenceAcrossRestarts() {
        String dbPath = tempDir.resolve("test_persistence_db").toAbsolutePath().toString().replace("\\", "/");

        Long createdClaimId;
        String createdClaimRef;

        // Context 1: First startup - seeds baseline data, creates a custom claim, performs a review action
        try (ConfigurableApplicationContext context1 = new SpringApplicationBuilder(ClaimGuardApplication.class)
                .properties(
                        "CLAIMGUARD_DB_PATH=" + dbPath,
                        "server.port=0",
                        "spring.jpa.hibernate.ddl-auto=update"
                ).run()) {

            ClaimRepository claimRepo = context1.getBean(ClaimRepository.class);
            ReviewHistoryRepository historyRepo = context1.getBean(ReviewHistoryRepository.class);
            ClaimService claimService = context1.getBean(ClaimService.class);

            long baselineCount = claimRepo.count();
            assertEquals(5, baselineCount, "Baseline seed data should consist of 5 claims");

            CreateClaimRequest request = new CreateClaimRequest();
            request.setPolicyNumber("POL-PERSIST-999");
            request.setClaimantName("Dr. Vikram Malhotra");
            request.setContactPhone("555-908-1122");
            request.setContactEmail("vikram.malhotra@hospital.org");
            request.setClaimType(ClaimType.HEALTH);
            request.setIncidentDate(LocalDateTime.now().minusDays(3));
            request.setClaimedAmount(new BigDecimal("18500.00"));
            request.setIncidentDescription("Emergency surgical procedure reimbursement claim");

            ClaimDetailDTO created = claimService.createClaim(request);
            assertNotNull(created.getId());
            assertNotNull(created.getClaimReference());
            createdClaimId = created.getId();
            createdClaimRef = created.getClaimReference();

            // Perform review transition to create audit history
            claimService.startReview(createdClaimId, "priya.sharma");

            assertEquals(6, claimRepo.count(), "Total claims should now be 6 (5 baseline + 1 created)");
            List<ReviewHistory> initialHistory = historyRepo.findByClaimIdOrderByActionTimestampAsc(createdClaimId);
            assertTrue(initialHistory.size() >= 2, "Review history should contain ingestion and start_review entries");
        }

        // Context 2: Second startup (restart) - points to the same file-backed H2 DB
        try (ConfigurableApplicationContext context2 = new SpringApplicationBuilder(ClaimGuardApplication.class)
                .properties(
                        "CLAIMGUARD_DB_PATH=" + dbPath,
                        "server.port=0",
                        "spring.jpa.hibernate.ddl-auto=update"
                ).run()) {

            ClaimRepository claimRepo = context2.getBean(ClaimRepository.class);
            ReviewHistoryRepository historyRepo = context2.getBean(ReviewHistoryRepository.class);

            // 1. Verify seed data is not duplicated on restart
            assertEquals(6, claimRepo.count(), "Total claims after restart must remain 6 (no duplicate seed data)");

            // 2. Verify custom claim survived restart
            Optional<Claim> retrievedOpt = claimRepo.findById(createdClaimId);
            assertTrue(retrievedOpt.isPresent(), "Created claim must survive application restart");

            Claim retrieved = retrievedOpt.get();
            assertEquals(createdClaimRef, retrieved.getClaimReference());
            assertEquals("POL-PERSIST-999", retrieved.getPolicyNumber());
            assertEquals("Dr. Vikram Malhotra", retrieved.getClaimantName());
            assertEquals(ClaimStatus.UNDER_REVIEW, retrieved.getStatus());
            assertEquals(0, new BigDecimal("18500.00").compareTo(retrieved.getClaimedAmount()));

            // 3. Verify audit history survived restart
            List<ReviewHistory> persistedHistory = historyRepo.findByClaimIdOrderByActionTimestampAsc(createdClaimId);
            assertTrue(persistedHistory.size() >= 2, "Persisted review history must survive application restart");
            assertEquals("priya.sharma", persistedHistory.get(persistedHistory.size() - 1).getReviewerName());
            assertEquals("REVIEW_STARTED", persistedHistory.get(persistedHistory.size() - 1).getActionType());
        }
    }
}
