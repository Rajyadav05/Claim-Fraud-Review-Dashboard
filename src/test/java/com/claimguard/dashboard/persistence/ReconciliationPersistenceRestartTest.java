package com.claimguard.dashboard.persistence;

import com.claimguard.dashboard.ClaimGuardApplication;
import com.claimguard.dashboard.dto.*;
import com.claimguard.dashboard.entity.Claim;
import com.claimguard.dashboard.entity.ClaimStatus;
import com.claimguard.dashboard.entity.ClaimType;
import com.claimguard.dashboard.entity.RiskPriority;
import com.claimguard.dashboard.entity.ReviewHistory;
import com.claimguard.dashboard.repository.ClaimRepository;
import com.claimguard.dashboard.repository.ReviewHistoryRepository;
import com.claimguard.dashboard.service.ClaimReconciliationService;
import com.claimguard.dashboard.service.ClaimService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ReconciliationPersistenceRestartTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Verify reconciled candidate claims and history persist across application restart and identity sequences remain intact")
    void testReconciliationPersistenceAcrossRestarts() {
        String dbPath = tempDir.resolve("test_recon_db").toAbsolutePath().toString().replace("\\", "/");

        ReconciliationPayloadDTO payload = buildCandidatePayload();

        // Context 1: Reconcile payload into isolated file-backed DB, create post-reconcile claim, then shut down
        try (ConfigurableApplicationContext context1 = new SpringApplicationBuilder(ClaimGuardApplication.class)
                .properties(
                        "CLAIMGUARD_DB_PATH=" + dbPath,
                        "server.port=0",
                        "spring.jpa.hibernate.ddl-auto=update"
                ).run()) {

            ClaimRepository claimRepo = context1.getBean(ClaimRepository.class);
            ClaimReconciliationService reconService = context1.getBean(ClaimReconciliationService.class);
            ClaimService claimService = context1.getBean(ClaimService.class);

            assertEquals(5, claimRepo.count(), "Baseline seed data should consist of 5 claims");

            // Execute reconciliation
            ReconciliationResultDTO result = reconService.reconcile(payload);
            assertEquals("SUCCESS", result.getStatus());
            assertEquals(2, result.getClaimsInserted());
            assertEquals(0, result.getClaimsSkipped());
            assertEquals(4, result.getHistoryRecordsInserted());
            assertEquals(7, claimRepo.count(), "Total claims after reconciliation should be 7");

            // Create a brand new claim through ordinary service to verify identity sequence synchronization
            CreateClaimRequest postReconRequest = new CreateClaimRequest();
            postReconRequest.setPolicyNumber("POL-POST-RECON-01");
            postReconRequest.setClaimantName("Post Recon Applicant");
            postReconRequest.setContactPhone("555-888-9999");
            postReconRequest.setContactEmail("post.recon@example.com");
            postReconRequest.setClaimType(ClaimType.MOTOR);
            postReconRequest.setIncidentDate(LocalDateTime.now().minusDays(1));
            postReconRequest.setClaimedAmount(new BigDecimal("6200.00"));
            postReconRequest.setIncidentDescription("Post reconciliation fender bender");

            ClaimDetailDTO created = claimService.createClaim(postReconRequest);
            assertNotNull(created.getId());
            assertTrue(created.getId() > 7, "Generated ID should exceed the reconciled IDs");
            assertEquals(8, claimRepo.count(), "Total claims should now be 8 (5 baseline + 2 reconciled + 1 new)");
        }

        // Context 2: Restart application against the same persistent H2 database
        try (ConfigurableApplicationContext context2 = new SpringApplicationBuilder(ClaimGuardApplication.class)
                .properties(
                        "CLAIMGUARD_DB_PATH=" + dbPath,
                        "server.port=0",
                        "spring.jpa.hibernate.ddl-auto=update"
                ).run()) {

            ClaimRepository claimRepo = context2.getBean(ClaimRepository.class);
            ReviewHistoryRepository historyRepo = context2.getBean(ReviewHistoryRepository.class);
            ClaimReconciliationService reconService = context2.getBean(ClaimReconciliationService.class);
            ClaimService claimService = context2.getBean(ClaimService.class);

            // 1. Verify claims count survived restart
            assertEquals(8, claimRepo.count(), "Total claims after restart must remain 8");

            // 2. Verify candidate claims and review history details survived
            Optional<Claim> clm6Opt = claimRepo.findByClaimReference("CLM-2026-0006");
            assertTrue(clm6Opt.isPresent(), "CLM-2026-0006 must survive restart");
            Claim clm6 = clm6Opt.get();
            assertEquals("POL-AUTO-8890", clm6.getPolicyNumber());
            assertEquals(ClaimStatus.APPROVED, clm6.getStatus());
            assertEquals(0, new BigDecimal("45000.00").compareTo(clm6.getClaimedAmount()));

            List<ReviewHistory> clm6Histories = historyRepo.findByClaimIdOrderByActionTimestampAsc(clm6.getId());
            assertEquals(2, clm6Histories.size(), "CLM-2026-0006 review histories must survive restart");
            assertEquals("CLAIM_INGESTED", clm6Histories.get(0).getActionType());
            assertEquals("CLAIM_APPROVED", clm6Histories.get(1).getActionType());

            Optional<Claim> clm7Opt = claimRepo.findByClaimReference("CLM-2026-0007");
            assertTrue(clm7Opt.isPresent(), "CLM-2026-0007 must survive restart");
            Claim clm7 = clm7Opt.get();
            assertEquals(ClaimStatus.REJECTED, clm7.getStatus());

            // 3. Re-run identical payload — must be 100% idempotent
            ReconciliationResultDTO reimportResult = reconService.reconcile(payload);
            assertEquals("SUCCESS", reimportResult.getStatus());
            assertEquals(0, reimportResult.getClaimsInserted());
            assertEquals(2, reimportResult.getClaimsSkipped());
            assertEquals(0, reimportResult.getHistoryRecordsInserted());
            assertEquals(8, claimRepo.count(), "Total claims after reimport must remain 8");

            // 4. Create another new claim after restart to verify identity sequence continues smoothly
            CreateClaimRequest request2 = new CreateClaimRequest();
            request2.setPolicyNumber("POL-POST-RESTART-02");
            request2.setClaimantName("Post Restart Applicant");
            request2.setContactPhone("555-777-6666");
            request2.setContactEmail("post.restart@example.com");
            request2.setClaimType(ClaimType.HEALTH);
            request2.setIncidentDate(LocalDateTime.now().minusDays(2));
            request2.setClaimedAmount(new BigDecimal("12000.00"));
            request2.setIncidentDescription("Post restart append claim");

            ClaimDetailDTO created2 = claimService.createClaim(request2);
            assertNotNull(created2.getId());
            assertEquals(9, claimRepo.count(), "Total claims should now be 9");
        }
    }

    private ReconciliationPayloadDTO buildCandidatePayload() {
        ReconciliationPayloadDTO payload = new ReconciliationPayloadDTO();
        payload.setSchemaVersion("1.0");
        payload.setExportTimestamp(LocalDateTime.of(2026, 10, 25, 14, 0, 0));

        List<ReconciliationClaimItemDTO> claims = new ArrayList<>();

        // Candidate Claim 6
        ReconciliationClaimItemDTO c6 = new ReconciliationClaimItemDTO();
        c6.setClaimReference("CLM-2026-0006");
        c6.setPolicyNumber("POL-AUTO-8890");
        c6.setClaimantName("David K. Miller");
        c6.setContactPhone("555-019-2834");
        c6.setContactEmail("david.miller@example.com");
        c6.setClaimType(ClaimType.MOTOR);
        c6.setIncidentDate(LocalDateTime.of(2026, 10, 20, 11, 15, 0));
        c6.setSubmissionDate(LocalDateTime.of(2026, 10, 21, 9, 0, 0));
        c6.setClaimedAmount(new BigDecimal("45000.00"));
        c6.setStatus(ClaimStatus.APPROVED);
        c6.setRiskPriority(RiskPriority.HIGH);
        c6.setIncidentDescription("Candidate rollover collision incident");

        List<ReconciliationHistoryDTO> h6List = new ArrayList<>();
        ReconciliationHistoryDTO h6_1 = new ReconciliationHistoryDTO();
        h6_1.setActionType("CLAIM_INGESTED");
        h6_1.setPreviousStatus(null);
        h6_1.setNewStatus(ClaimStatus.SUBMITTED);
        h6_1.setActionTimestamp(LocalDateTime.of(2026, 10, 21, 9, 0, 0));
        h6_1.setReviewerName("SYSTEM");
        h6_1.setReviewerRemarks("Candidate claim ingestion");
        h6List.add(h6_1);

        ReconciliationHistoryDTO h6_2 = new ReconciliationHistoryDTO();
        h6_2.setActionType("CLAIM_APPROVED");
        h6_2.setPreviousStatus(ClaimStatus.UNDER_REVIEW);
        h6_2.setNewStatus(ClaimStatus.APPROVED);
        h6_2.setActionTimestamp(LocalDateTime.of(2026, 10, 22, 16, 30, 0));
        h6_2.setReviewerName("admin.raj");
        h6_2.setReviewerRemarks("Candidate manual review approved");
        h6List.add(h6_2);
        c6.setReviewHistories(h6List);

        claims.add(c6);

        // Candidate Claim 7
        ReconciliationClaimItemDTO c7 = new ReconciliationClaimItemDTO();
        c7.setClaimReference("CLM-2026-0007");
        c7.setPolicyNumber("POL-PROP-4411");
        c7.setClaimantName("Elena Rostova");
        c7.setContactPhone("555-014-9988");
        c7.setContactEmail("elena.rostova@example.com");
        c7.setClaimType(ClaimType.PROPERTY);
        c7.setIncidentDate(LocalDateTime.of(2026, 10, 18, 22, 0, 0));
        c7.setSubmissionDate(LocalDateTime.of(2026, 10, 19, 8, 30, 0));
        c7.setClaimedAmount(new BigDecimal("128000.00"));
        c7.setStatus(ClaimStatus.REJECTED);
        c7.setRiskPriority(RiskPriority.HIGH);
        c7.setIncidentDescription("Candidate commercial arson claim");

        List<ReconciliationHistoryDTO> h7List = new ArrayList<>();
        ReconciliationHistoryDTO h7_1 = new ReconciliationHistoryDTO();
        h7_1.setActionType("CLAIM_INGESTED");
        h7_1.setPreviousStatus(null);
        h7_1.setNewStatus(ClaimStatus.SUBMITTED);
        h7_1.setActionTimestamp(LocalDateTime.of(2026, 10, 19, 8, 30, 0));
        h7_1.setReviewerName("SYSTEM");
        h7_1.setReviewerRemarks("Candidate claim ingestion");
        h7List.add(h7_1);

        ReconciliationHistoryDTO h7_2 = new ReconciliationHistoryDTO();
        h7_2.setActionType("CLAIM_REJECTED");
        h7_2.setPreviousStatus(ClaimStatus.UNDER_REVIEW);
        h7_2.setNewStatus(ClaimStatus.REJECTED);
        h7_2.setActionTimestamp(LocalDateTime.of(2026, 10, 23, 11, 0, 0));
        h7_2.setReviewerName("admin.raj");
        h7_2.setReviewerRemarks("High risk fraud detected and rejected");
        h7List.add(h7_2);
        c7.setReviewHistories(h7List);

        claims.add(c7);

        payload.setClaims(claims);
        return payload;
    }
}
