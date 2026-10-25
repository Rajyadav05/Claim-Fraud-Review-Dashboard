package com.claimguard.dashboard.service;

import com.claimguard.dashboard.dto.*;
import com.claimguard.dashboard.entity.*;
import com.claimguard.dashboard.exception.ReconciliationConflictException;
import com.claimguard.dashboard.repository.ClaimRepository;
import com.claimguard.dashboard.repository.ReviewHistoryRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:recon_ext_test_db;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class ReconciliationExtendedValidationTest {

    @Autowired
    private ClaimReconciliationService reconciliationService;

    @Autowired
    private ClaimRepository claimRepository;

    @Autowired
    private ReviewHistoryRepository historyRepository;

    @Autowired
    private ClaimService claimService;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    @DisplayName("R1: Two distinct events with same action type, status, and timestamp but different remarks are preserved")
    void testTwoDistinctEventsSameActionTypeAndTimestampDifferentRemarks() {
        LocalDateTime timestamp = LocalDateTime.of(2026, 10, 22, 14, 0, 0);

        ReconciliationClaimItemDTO item = createClaimItem("CLM-2026-0040");
        item.setStatus(ClaimStatus.UNDER_REVIEW);

        // Event 1
        ReconciliationHistoryDTO h1 = new ReconciliationHistoryDTO(
                "NOTE_ADDED", ClaimStatus.UNDER_REVIEW, ClaimStatus.UNDER_REVIEW,
                "investigator1", "Interviewed policyholder regarding timeline", timestamp);

        // Event 2 with identical actionType, newStatus, and timestamp, but different reviewerRemarks
        ReconciliationHistoryDTO h2 = new ReconciliationHistoryDTO(
                "NOTE_ADDED", ClaimStatus.UNDER_REVIEW, ClaimStatus.UNDER_REVIEW,
                "investigator2", "Received repair estimate from certified shop", timestamp);

        item.setReviewHistories(List.of(h1, h2));
        ReconciliationPayloadDTO payload = new ReconciliationPayloadDTO("1.0", LocalDateTime.now(), List.of(item));

        // First import: both events must be persisted
        ReconciliationResultDTO res1 = reconciliationService.reconcile(payload);
        assertEquals("SUCCESS", res1.getStatus());
        assertEquals(1, res1.getClaimsInserted());
        assertEquals(2, res1.getHistoryRecordsInserted());

        Optional<Claim> persistedClaim = claimRepository.findByClaimReference("CLM-2026-0040");
        assertTrue(persistedClaim.isPresent());
        List<ReviewHistory> histories = historyRepository.findByClaimIdOrderByActionTimestampAsc(persistedClaim.get().getId());
        assertEquals(2, histories.size(), "Both distinct historical events must be persisted");
        assertEquals("Interviewed policyholder regarding timeline", histories.get(0).getReviewerRemarks());
        assertEquals("Received repair estimate from certified shop", histories.get(1).getReviewerRemarks());

        // Re-import identical payload: must be 100% idempotent (0 duplicates)
        ReconciliationResultDTO res2 = reconciliationService.reconcile(payload);
        assertEquals("SUCCESS", res2.getStatus());
        assertEquals(0, res2.getClaimsInserted());
        assertEquals(1, res2.getClaimsSkipped());
        assertEquals(0, res2.getHistoryRecordsInserted());
        assertEquals(2, historyRepository.findByClaimIdOrderByActionTimestampAsc(persistedClaim.get().getId()).size(),
                "History count must remain strictly 2 on reimport");
    }

    @Test
    @DisplayName("R2a: Two distinct events with identical timestamps and remarks are distinguished by sourceEventId")
    void testTwoEventsIdenticalTimestampAndRemarksWithSourceEventId() {
        LocalDateTime timestamp = LocalDateTime.of(2026, 10, 23, 11, 0, 0);

        ReconciliationClaimItemDTO item = createClaimItem("CLM-2026-0041");
        item.setStatus(ClaimStatus.UNDER_REVIEW);

        ReconciliationHistoryDTO h1 = new ReconciliationHistoryDTO(
                "STATUS_CHECK", ClaimStatus.UNDER_REVIEW, ClaimStatus.UNDER_REVIEW,
                "SYSTEM", "Automated periodic integrity ping", timestamp);
        h1.setSourceEventId("EVT-PING-001");

        ReconciliationHistoryDTO h2 = new ReconciliationHistoryDTO(
                "STATUS_CHECK", ClaimStatus.UNDER_REVIEW, ClaimStatus.UNDER_REVIEW,
                "SYSTEM", "Automated periodic integrity ping", timestamp);
        h2.setSourceEventId("EVT-PING-002");

        item.setReviewHistories(List.of(h1, h2));
        ReconciliationPayloadDTO payload = new ReconciliationPayloadDTO("1.0", LocalDateTime.now(), List.of(item));

        ReconciliationResultDTO result = reconciliationService.reconcile(payload);
        assertEquals("SUCCESS", result.getStatus());
        assertEquals(1, result.getClaimsInserted());
        assertEquals(2, result.getHistoryRecordsInserted());

        Claim claim = claimRepository.findByClaimReference("CLM-2026-0041").orElseThrow();
        List<ReviewHistory> histories = historyRepository.findByClaimIdOrderByActionTimestampAsc(claim.getId());
        assertEquals(2, histories.size());
        assertEquals("EVT-PING-001", histories.get(0).getSourceEventId());
        assertEquals("EVT-PING-002", histories.get(1).getSourceEventId());

        // Re-importing identical payload retains idempotency
        ReconciliationResultDTO reimport = reconciliationService.reconcile(payload);
        assertEquals(0, reimport.getHistoryRecordsInserted());
        assertEquals(2, historyRepository.findByClaimIdOrderByActionTimestampAsc(claim.getId()).size());
    }

    @Test
    @DisplayName("R2b: Indistinguishable duplicate events without distinct sourceEventId are safely rejected")
    void testIndistinguishableEventsWithoutSourceEventIdRejected() {
        LocalDateTime timestamp = LocalDateTime.of(2026, 10, 23, 11, 0, 0);

        ReconciliationClaimItemDTO item = createClaimItem("CLM-2026-0042");
        item.setStatus(ClaimStatus.UNDER_REVIEW);

        ReconciliationHistoryDTO h1 = new ReconciliationHistoryDTO(
                "PING", ClaimStatus.UNDER_REVIEW, ClaimStatus.UNDER_REVIEW,
                "SYSTEM", "Identical message", timestamp);

        ReconciliationHistoryDTO h2 = new ReconciliationHistoryDTO(
                "PING", ClaimStatus.UNDER_REVIEW, ClaimStatus.UNDER_REVIEW,
                "SYSTEM", "Identical message", timestamp);

        item.setReviewHistories(List.of(h1, h2));
        ReconciliationPayloadDTO payload = new ReconciliationPayloadDTO("1.0", LocalDateTime.now(), List.of(item));

        ReconciliationConflictException ex = assertThrows(ReconciliationConflictException.class,
                () -> reconciliationService.reconcile(payload));
        assertTrue(ex.getMessage().contains("Indistinguishable review history events"),
                "Should safely reject ambiguous duplicates lacking distinct source IDs");
    }

    @Test
    @DisplayName("R3: Reimporting the same history payload twice creates no duplicate entries")
    void testReimportingSameHistoryPayloadTwice() {
        ReconciliationPayloadDTO payload = createSamplePayload("CLM-2026-0081", "CLM-2026-0082");

        // Attempt 1
        ReconciliationResultDTO attempt1 = reconciliationService.reconcile(payload);
        assertEquals("SUCCESS", attempt1.getStatus());
        assertEquals(2, attempt1.getClaimsInserted());
        assertEquals(7, claimRepository.count());
        long totalHistoriesAttempt1 = historyRepository.count();

        // Attempt 2
        ReconciliationResultDTO attempt2 = reconciliationService.reconcile(payload);
        assertEquals("SUCCESS", attempt2.getStatus());
        assertEquals(0, attempt2.getClaimsInserted());
        assertEquals(2, attempt2.getClaimsSkipped());
        assertEquals(0, attempt2.getHistoryRecordsInserted());
        assertEquals(7, claimRepository.count());
        assertEquals(totalHistoriesAttempt1, historyRepository.count(),
                "Total history rows in DB must not change on identical reimport");
    }

    @Test
    @DisplayName("R4: Incrementally importing new history into an existing baseline claim")
    void testIncrementallyImportingNewHistoryIntoBaselineClaim() {
        Claim baseline1 = claimRepository.findByClaimReference("CLM-2026-0001").orElseThrow();
        long initialHistCount = historyRepository.findByClaimIdOrderByActionTimestampAsc(baseline1.getId()).size();
        assertTrue(initialHistCount >= 1, "Baseline claim 1 must have at least 1 history record");

        // Build reconciliation item matching baseline claim 1 exactly, with 1 additional new history event
        ReconciliationClaimItemDTO item = new ReconciliationClaimItemDTO();
        item.setClaimReference("CLM-2026-0001");
        item.setPolicyNumber("POL-MH-9482-1049");
        item.setClaimantName("Rajesh Kumar");
        item.setClaimType(ClaimType.MOTOR);
        item.setIncidentDate(baseline1.getIncidentDate());
        item.setSubmissionDate(baseline1.getSubmissionDate());
        item.setClaimedAmount(baseline1.getClaimedAmount());
        item.setStatus(ClaimStatus.UNDER_REVIEW);
        item.setRiskPriority(RiskPriority.HIGH);

        LocalDateTime newEventTime = LocalDateTime.now().minusHours(2).truncatedTo(ChronoUnit.SECONDS);
        ReconciliationHistoryDTO incrementalHist = new ReconciliationHistoryDTO(
                "FORENSIC_ESCALATION", ClaimStatus.UNDER_REVIEW, ClaimStatus.UNDER_REVIEW,
                "Dr. Aris Thorne", "Digital forensic audit requested for repair invoices", newEventTime);
        incrementalHist.setSourceEventId("EVT-FOR-099");
        item.setReviewHistories(List.of(incrementalHist));

        ReconciliationPayloadDTO payload = new ReconciliationPayloadDTO("1.0", LocalDateTime.now(), List.of(item));

        ReconciliationResultDTO result = reconciliationService.reconcile(payload);
        assertEquals("SUCCESS", result.getStatus());
        assertEquals(0, result.getClaimsInserted(), "Baseline claim must not be re-inserted");
        assertEquals(1, result.getClaimsSkipped(), "Baseline claim must be skipped");
        assertEquals(1, result.getHistoryRecordsInserted(), "Exactly 1 new history record must be inserted");

        List<ReviewHistory> updatedHistory = historyRepository.findByClaimIdOrderByActionTimestampAsc(baseline1.getId());
        assertEquals(initialHistCount + 1, updatedHistory.size(), "History count must increment by exactly 1");
        ReviewHistory latest = updatedHistory.get(updatedHistory.size() - 1);
        assertEquals("FORENSIC_ESCALATION", latest.getActionType());
        assertEquals("Dr. Aris Thorne", latest.getReviewerName());
        assertEquals("EVT-FOR-099", latest.getSourceEventId());
    }

    @Test
    @DisplayName("R5: Late-batch conflict causes complete transactional rollback of all earlier records")
    void testLateBatchConflictAndCompleteRollback() {
        long initialCount = claimRepository.count();
        assertEquals(5, initialCount, "Baseline has 5 claims");

        ReconciliationClaimItemDTO validItem1 = createClaimItem("CLM-2026-0051");
        ReconciliationClaimItemDTO validItem2 = createClaimItem("CLM-2026-0052");

        // Record 3: Late conflict with existing baseline CLM-2026-0001 (wrong claimedAmount)
        ReconciliationClaimItemDTO conflictItem = createClaimItem("CLM-2026-0001");
        conflictItem.setPolicyNumber("POL-MH-9482-1049");
        conflictItem.setClaimantName("Rajesh Kumar");
        conflictItem.setClaimType(ClaimType.MOTOR);
        conflictItem.setClaimedAmount(new BigDecimal("999999.00")); // Conflict! Baseline is 185000.00

        ReconciliationPayloadDTO payload = new ReconciliationPayloadDTO("1.0", LocalDateTime.now(),
                List.of(validItem1, validItem2, conflictItem));

        assertThrows(ReconciliationConflictException.class, () -> reconciliationService.reconcile(payload));

        assertEquals(initialCount, claimRepository.count(), "Database claim count must remain exactly baseline count (5)");
        assertFalse(claimRepository.findByClaimReference("CLM-2026-0051").isPresent(), "Record 1 must not be committed");
        assertFalse(claimRepository.findByClaimReference("CLM-2026-0052").isPresent(), "Record 2 must not be committed");
    }

    @Test
    @Transactional
    @DisplayName("R6: Sequence synchronization directly verifies review_history table and fails on invalid table names")
    void testSequenceSynchronizationForReviewHistoryTable() {
        // 1. Verify invalid table name throws an exception and is NOT silently ignored
        assertThrows(Exception.class, () -> reconciliationService.advanceSequence("claim_reviews"),
                "Synchronizing with invalid table name 'claim_reviews' must throw an exception");

        // 2. Insert explicit high ID record into review_history
        Claim claim = claimRepository.findByClaimReference("CLM-2026-0001").orElseThrow();
        entityManager.createNativeQuery(
                "INSERT INTO review_history (id, claim_id, action_type, new_status, action_timestamp, reviewer_name, reviewer_remarks) " +
                "VALUES (500, " + claim.getId() + ", 'TEST_SYNC', 'UNDER_REVIEW', CURRENT_TIMESTAMP, 'tester', 'Sync test')")
                .executeUpdate();

        // 3. Run sequence synchronization
        reconciliationService.syncIdentitySequences();

        // 4. Save new review history without ID via repository
        ReviewHistory newHist = new ReviewHistory(
                "POST_SYNC_EVENT", ClaimStatus.UNDER_REVIEW, ClaimStatus.UNDER_REVIEW,
                "reviewer.test", "Post sequence sync", LocalDateTime.now());
        newHist.setClaim(claim);
        ReviewHistory saved = historyRepository.save(newHist);

        // 5. Verify generated ID is greater than 500
        assertNotNull(saved.getId());
        assertTrue(saved.getId() > 500, "Generated ID must exceed the highest manual ID (500), but was: " + saved.getId());
    }

    @Test
    @DisplayName("R7: Post-reconciliation claim and history creation produces zero ID collisions")
    void testPostReconciliationClaimAndHistoryCreationNoCollisions() {
        assertEquals(5, claimRepository.count(), "Baseline has 5 claims");

        // Import candidate claims
        ReconciliationPayloadDTO candidatePayload = createSamplePayload("CLM-2026-0006", "CLM-2026-0007");
        ReconciliationResultDTO result = reconciliationService.reconcile(candidatePayload);
        assertEquals(2, result.getClaimsInserted());
        assertEquals(7, claimRepository.count());

        // Create standard claim via ClaimService
        CreateClaimRequest newReq = new CreateClaimRequest();
        newReq.setPolicyNumber("POL-NO-COLLIDE-01");
        newReq.setClaimantName("Collision Verifier");
        newReq.setContactPhone("555-901-2345");
        newReq.setContactEmail("collision.verifier@example.com");
        newReq.setClaimType(ClaimType.HEALTH);
        newReq.setIncidentDate(LocalDateTime.now().minusDays(1));
        newReq.setClaimedAmount(new BigDecimal("15000.00"));
        newReq.setIncidentDescription("Post-reconciliation collision test claim");

        ClaimDetailDTO created = claimService.createClaim(newReq);
        assertNotNull(created.getId());
        assertTrue(created.getId() >= 8, "New claim ID must be >= 8 to prevent collision");
        assertEquals(8, claimRepository.count());

        // Perform workflow action that adds review history
        assertDoesNotThrow(() -> claimService.startReview(created.getId(), "priya.sharma"),
                "Starting review on post-reconcile claim must not encounter review_history sequence collision");

        List<ReviewHistory> histories = historyRepository.findByClaimIdOrderByActionTimestampAsc(created.getId());
        assertTrue(histories.size() >= 2, "Claim must have ingestion and review_started history records");
    }

    @Test
    @DisplayName("R8: Concurrent import protection rejects overlapping jobs")
    void testConcurrentPayloadSubmission() throws Exception {
        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        List<Callable<Void>> tasks = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            tasks.add(() -> {
                readyLatch.countDown();
                startLatch.await();
                ReconciliationPayloadDTO payload = createSamplePayload("CLM-2026-006" + idx, "CLM-2026-007" + idx);
                try {
                    ReconciliationResultDTO res = reconciliationService.reconcile(payload);
                    if ("SUCCESS".equals(res.getStatus())) {
                        successCount.incrementAndGet();
                    }
                } catch (IllegalStateException e) {
                    if (e.getMessage().contains("currently in progress")) {
                        conflictCount.incrementAndGet();
                    }
                }
                return null;
            });
        }

        List<Future<Void>> futures = new ArrayList<>();
        for (Callable<Void> task : tasks) {
            futures.add(executor.submit(task));
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown();

        for (Future<Void> future : futures) {
            future.get(10, TimeUnit.SECONDS);
        }
        executor.shutdown();

        assertTrue(successCount.get() >= 1, "At least one import job must complete successfully");
        assertEquals(threadCount, successCount.get() + conflictCount.get(), "All threads must either succeed or receive lock rejection");
    }

    private ReconciliationPayloadDTO createSamplePayload(String ref1, String ref2) {
        ReconciliationClaimItemDTO item1 = createClaimItem(ref1);
        ReconciliationClaimItemDTO item2 = createClaimItem(ref2);
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
        item.setIncidentDate(LocalDateTime.now().minusDays(3).truncatedTo(ChronoUnit.SECONDS));
        item.setSubmissionDate(LocalDateTime.now().minusDays(2).truncatedTo(ChronoUnit.SECONDS));
        item.setClaimedAmount(new BigDecimal("25000.00"));
        item.setStatus(ClaimStatus.SUBMITTED);
        item.setRiskPriority(RiskPriority.MEDIUM);
        return item;
    }
}
