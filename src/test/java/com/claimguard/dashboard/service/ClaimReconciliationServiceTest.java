package com.claimguard.dashboard.service;

import com.claimguard.dashboard.dto.*;
import com.claimguard.dashboard.entity.*;
import com.claimguard.dashboard.exception.ReconciliationConflictException;
import com.claimguard.dashboard.repository.ClaimRepository;
import com.claimguard.dashboard.repository.ReviewHistoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:recon_service_test_db;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class ClaimReconciliationServiceTest {

    @Autowired
    private ClaimReconciliationService reconciliationService;

    @Autowired
    private ClaimRepository claimRepository;

    @Autowired
    private ReviewHistoryRepository historyRepository;

    @Autowired
    private ClaimService claimService;

    @Test
    @DisplayName("Import candidate records alongside 5-claim baseline")
    void testReconcileAlongsideBaseline() {
        assertEquals(5, claimRepository.count(), "Initial baseline should have 5 claims");

        ReconciliationPayloadDTO payload = createSamplePayload("CLM-2026-0006", "CLM-2026-0007");
        ReconciliationResultDTO result = reconciliationService.reconcile(payload);

        assertEquals("SUCCESS", result.getStatus());
        assertEquals(2, result.getClaimsInserted());
        assertEquals(0, result.getClaimsSkipped());
        assertEquals(7, claimRepository.count(), "Total claims should now be 7");

        Optional<Claim> c6 = claimRepository.findByClaimReference("CLM-2026-0006");
        assertTrue(c6.isPresent());
        assertEquals(ClaimStatus.UNDER_REVIEW, c6.get().getStatus());
        List<ReviewHistory> c6Hist = historyRepository.findByClaimIdOrderByActionTimestampAsc(c6.get().getId());
        assertEquals(2, c6Hist.size());
    }

    @Test
    @DisplayName("Idempotency: Reimporting identical payload does not increase counts or create duplicates")
    void testIdempotencyOnIdenticalReimport() {
        ReconciliationPayloadDTO payload = createSamplePayload("CLM-2026-0006", "CLM-2026-0007");

        // First import
        ReconciliationResultDTO res1 = reconciliationService.reconcile(payload);
        assertEquals(2, res1.getClaimsInserted());
        assertEquals(7, claimRepository.count());

        // Second import with identical payload
        ReconciliationResultDTO res2 = reconciliationService.reconcile(payload);
        assertEquals(0, res2.getClaimsInserted(), "Second import should insert zero new claims");
        assertEquals(2, res2.getClaimsSkipped(), "Second import should skip 2 duplicate claims");
        assertEquals(7, claimRepository.count(), "Total claims count must remain strictly 7");
    }

    @Test
    @DisplayName("Detect conflicting claim with same reference and reject with ReconciliationConflictException")
    void testDetectConflictingClaimWithSameReference() {
        // CLM-2026-0001 already exists in baseline (Rajesh Kumar, claimedAmount = 185000.00)
        ReconciliationClaimItemDTO conflictClaim = new ReconciliationClaimItemDTO();
        conflictClaim.setClaimReference("CLM-2026-0001");
        conflictClaim.setPolicyNumber("POL-DIFFERENT-1111"); // Conflict!
        conflictClaim.setClaimantName("Impostor Claimant"); // Conflict!
        conflictClaim.setClaimType(ClaimType.MOTOR);
        conflictClaim.setIncidentDate(LocalDateTime.now().minusDays(5));
        conflictClaim.setSubmissionDate(LocalDateTime.now().minusDays(2));
        conflictClaim.setClaimedAmount(new BigDecimal("99999.00")); // Conflict!
        conflictClaim.setStatus(ClaimStatus.SUBMITTED);
        conflictClaim.setRiskPriority(RiskPriority.HIGH);

        ReconciliationPayloadDTO payload = new ReconciliationPayloadDTO("1.0", LocalDateTime.now(), List.of(conflictClaim));

        assertThrows(ReconciliationConflictException.class, () -> reconciliationService.reconcile(payload));
        assertEquals(5, claimRepository.count(), "Database state must remain unaffected after rejected conflict");
    }

    @Test
    @DisplayName("Preserve original timestamps, statuses, and review-history relationships")
    void testPreserveOriginalTimestampsAndStatus() {
        LocalDateTime originalIncident = LocalDateTime.of(2026, 10, 10, 14, 30, 0);
        LocalDateTime originalSubmission = LocalDateTime.of(2026, 10, 11, 9, 15, 0);
        LocalDateTime originalAuditTime = LocalDateTime.of(2026, 10, 12, 11, 0, 0);

        ReconciliationClaimItemDTO item = new ReconciliationClaimItemDTO();
        item.setClaimReference("CLM-2026-0088");
        item.setPolicyNumber("POL-TIMETEST-01");
        item.setClaimantName("Kiran Rao");
        item.setClaimType(ClaimType.PROPERTY);
        item.setIncidentDate(originalIncident);
        item.setSubmissionDate(originalSubmission);
        item.setClaimedAmount(new BigDecimal("75000.00"));
        item.setStatus(ClaimStatus.UNDER_REVIEW);
        item.setRiskPriority(RiskPriority.MEDIUM);

        ReconciliationHistoryDTO hist = new ReconciliationHistoryDTO(
                "REVIEW_STARTED",
                ClaimStatus.SUBMITTED,
                ClaimStatus.UNDER_REVIEW,
                "Audit Reviewer",
                "Historical audit remark preserved",
                originalAuditTime
        );
        item.setReviewHistories(List.of(hist));

        ReconciliationPayloadDTO payload = new ReconciliationPayloadDTO("1.0", LocalDateTime.now(), List.of(item));
        reconciliationService.reconcile(payload);

        Claim saved = claimRepository.findByClaimReference("CLM-2026-0088").orElseThrow();
        assertEquals(originalIncident, saved.getIncidentDate());
        assertEquals(originalSubmission, saved.getSubmissionDate());
        assertEquals(ClaimStatus.UNDER_REVIEW, saved.getStatus());

        List<ReviewHistory> histories = historyRepository.findByClaimIdOrderByActionTimestampAsc(saved.getId());
        assertEquals(1, histories.size());
        assertEquals(originalAuditTime, histories.get(0).getActionTimestamp());
        assertEquals("Audit Reviewer", histories.get(0).getReviewerName());
        assertEquals(saved.getId(), histories.get(0).getClaim().getId(), "History must correctly point to parent claim");
    }

    @Test
    @DisplayName("Transactional rollback: Entire batch rolls back when any record fails validation")
    void testRollbackEntireBatchWhenOneRecordInvalid() {
        long initialCount = claimRepository.count();

        // Item 1 is valid
        ReconciliationClaimItemDTO validItem = new ReconciliationClaimItemDTO();
        validItem.setClaimReference("CLM-2026-0010");
        validItem.setPolicyNumber("POL-ROLLBACK-01");
        validItem.setClaimantName("Valid Claimant");
        validItem.setClaimType(ClaimType.MOTOR);
        validItem.setIncidentDate(LocalDateTime.now().minusDays(2));
        validItem.setSubmissionDate(LocalDateTime.now().minusDays(1));
        validItem.setClaimedAmount(new BigDecimal("10000.00"));
        validItem.setStatus(ClaimStatus.SUBMITTED);
        validItem.setRiskPriority(RiskPriority.LOW);

        // Item 2 has a conflicting reference with baseline CLM-2026-0001
        ReconciliationClaimItemDTO invalidItem = new ReconciliationClaimItemDTO();
        invalidItem.setClaimReference("CLM-2026-0001");
        invalidItem.setPolicyNumber("CONFLICT-POL");
        invalidItem.setClaimantName("Conflict Claimant");
        invalidItem.setClaimType(ClaimType.MOTOR);
        invalidItem.setIncidentDate(LocalDateTime.now().minusDays(2));
        invalidItem.setSubmissionDate(LocalDateTime.now().minusDays(1));
        invalidItem.setClaimedAmount(new BigDecimal("500.00"));
        invalidItem.setStatus(ClaimStatus.SUBMITTED);
        invalidItem.setRiskPriority(RiskPriority.LOW);

        ReconciliationPayloadDTO payload = new ReconciliationPayloadDTO("1.0", LocalDateTime.now(), List.of(validItem, invalidItem));

        assertThrows(ReconciliationConflictException.class, () -> reconciliationService.reconcile(payload));

        assertEquals(initialCount, claimRepository.count(), "Total claims count must remain unchanged after batch rollback");
        assertFalse(claimRepository.findByClaimReference("CLM-2026-0010").isPresent(), "First valid item must be rolled back completely");
    }

    @Test
    @DisplayName("Identity sequence synchronization: Next standard claim creation succeeds without ID collision")
    void testIdentityGenerationAfterReconciliation() {
        ReconciliationPayloadDTO payload = createSamplePayload("CLM-2026-0006", "CLM-2026-0007");
        reconciliationService.reconcile(payload);
        assertEquals(7, claimRepository.count());

        // Create a new claim using standard application ClaimService
        CreateClaimRequest newReq = new CreateClaimRequest();
        newReq.setPolicyNumber("POL-POST-RECON-001");
        newReq.setClaimantName("Post Recon Claimant");
        newReq.setClaimType(ClaimType.HEALTH);
        newReq.setIncidentDate(LocalDateTime.now().minusDays(1));
        newReq.setClaimedAmount(new BigDecimal("12000.00"));

        ClaimDetailDTO newlyCreated = claimService.createClaim(newReq);
        assertNotNull(newlyCreated.getId());
        assertTrue(newlyCreated.getId() >= 8, "Newly generated ID should be >= 8 to avoid collision (was: " + newlyCreated.getId() + ")");
        assertEquals(8, claimRepository.count(), "Total claims should be 8");
    }

    @Test
    @DisplayName("Reject duplicate claim references within the same incoming payload")
    void testRejectDuplicateReferenceInSameBatch() {
        ReconciliationClaimItemDTO item1 = createClaimItem("CLM-2026-0099");
        ReconciliationClaimItemDTO item2 = createClaimItem("CLM-2026-0099"); // Duplicate reference!

        ReconciliationPayloadDTO payload = new ReconciliationPayloadDTO("1.0", LocalDateTime.now(), List.of(item1, item2));

        assertThrows(ReconciliationConflictException.class, () -> reconciliationService.reconcile(payload));
    }

    @Test
    @DisplayName("Reject payload exceeding maximum allowed batch size")
    void testRejectOversizedPayload() {
        List<ReconciliationClaimItemDTO> largeList = new ArrayList<>();
        for (int i = 1; i <= ClaimReconciliationService.MAX_BATCH_SIZE + 1; i++) {
            largeList.add(createClaimItem(String.format("CLM-2026-%04d", i + 100)));
        }

        ReconciliationPayloadDTO payload = new ReconciliationPayloadDTO("1.0", LocalDateTime.now(), largeList);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> reconciliationService.reconcile(payload));
        assertTrue(ex.getMessage().contains("exceeds maximum batch limit"));
    }

    @Test
    @DisplayName("Reject unsupported schema version")
    void testRejectUnsupportedSchemaVersion() {
        ReconciliationClaimItemDTO item = createClaimItem("CLM-2026-0050");
        ReconciliationPayloadDTO payload = new ReconciliationPayloadDTO("9.9", LocalDateTime.now(), List.of(item));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> reconciliationService.reconcile(payload));
        assertTrue(ex.getMessage().contains("Unsupported schema version"));
    }

    private ReconciliationPayloadDTO createSamplePayload(String ref1, String ref2) {
        ReconciliationClaimItemDTO item1 = createClaimItem(ref1);
        item1.setStatus(ClaimStatus.UNDER_REVIEW);

        ReconciliationHistoryDTO h1 = new ReconciliationHistoryDTO(
                "SUBMITTED", null, ClaimStatus.SUBMITTED, "System Intake", "Initial ingestion", LocalDateTime.now().minusDays(2));
        ReconciliationHistoryDTO h2 = new ReconciliationHistoryDTO(
                "REVIEW_STARTED", ClaimStatus.SUBMITTED, ClaimStatus.UNDER_REVIEW, "Priya Sharma", "Review commenced", LocalDateTime.now().minusDays(1));
        item1.setReviewHistories(List.of(h1, h2));

        ReconciliationClaimItemDTO item2 = createClaimItem(ref2);
        item2.setStatus(ClaimStatus.SUBMITTED);
        ReconciliationHistoryDTO h3 = new ReconciliationHistoryDTO(
                "SUBMITTED", null, ClaimStatus.SUBMITTED, "System Intake", "Initial ingestion", LocalDateTime.now().minusHours(12));
        item2.setReviewHistories(List.of(h3));

        return new ReconciliationPayloadDTO("1.0", LocalDateTime.now(), List.of(item1, item2));
    }

    private ReconciliationClaimItemDTO createClaimItem(String ref) {
        ReconciliationClaimItemDTO item = new ReconciliationClaimItemDTO();
        item.setClaimReference(ref);
        item.setPolicyNumber("POL-" + ref);
        item.setClaimantName("Test Claimant " + ref);
        item.setContactPhone("555-123-4567");
        item.setContactEmail("test@example.com");
        item.setClaimType(ClaimType.MOTOR);
        item.setIncidentDate(LocalDateTime.now().minusDays(3));
        item.setSubmissionDate(LocalDateTime.now().minusDays(2));
        item.setClaimedAmount(new BigDecimal("25000.00"));
        item.setStatus(ClaimStatus.SUBMITTED);
        item.setRiskPriority(RiskPriority.MEDIUM);
        return item;
    }
}
