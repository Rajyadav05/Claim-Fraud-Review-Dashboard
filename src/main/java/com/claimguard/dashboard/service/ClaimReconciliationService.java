package com.claimguard.dashboard.service;

import com.claimguard.dashboard.dto.*;
import com.claimguard.dashboard.entity.*;
import com.claimguard.dashboard.exception.ReconciliationConflictException;
import com.claimguard.dashboard.repository.ClaimDocumentRepository;
import com.claimguard.dashboard.repository.ClaimRepository;
import com.claimguard.dashboard.repository.ReviewHistoryRepository;
import com.claimguard.dashboard.repository.SuspiciousIndicatorRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class ClaimReconciliationService {

    private static final Logger logger = LoggerFactory.getLogger(ClaimReconciliationService.class);
    public static final int MAX_BATCH_SIZE = 500;
    public static final String SUPPORTED_SCHEMA_VERSION = "1.0";

    private final ClaimRepository claimRepository;
    private final ReviewHistoryRepository historyRepository;
    private final SuspiciousIndicatorRepository indicatorRepository;
    private final ClaimDocumentRepository documentRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private final ReentrantLock importLock = new ReentrantLock();

    public ClaimReconciliationService(ClaimRepository claimRepository,
                                      ReviewHistoryRepository historyRepository,
                                      SuspiciousIndicatorRepository indicatorRepository,
                                      ClaimDocumentRepository documentRepository) {
        this.claimRepository = claimRepository;
        this.historyRepository = historyRepository;
        this.indicatorRepository = indicatorRepository;
        this.documentRepository = documentRepository;
    }

    @Transactional(rollbackFor = Exception.class)
    public ReconciliationResultDTO reconcile(ReconciliationPayloadDTO payload) {
        if (!importLock.tryLock()) {
            throw new IllegalStateException("Concurrent reconciliation rejected: another import operation is currently in progress.");
        }

        try {
            return doReconcile(payload);
        } finally {
            importLock.unlock();
        }
    }

    private ReconciliationResultDTO doReconcile(ReconciliationPayloadDTO payload) {
        if (payload == null) {
            throw new IllegalArgumentException("Reconciliation payload cannot be null.");
        }

        if (payload.getSchemaVersion() == null || !SUPPORTED_SCHEMA_VERSION.equals(payload.getSchemaVersion().trim())) {
            throw new IllegalArgumentException("Unsupported schema version: " + payload.getSchemaVersion() + ". Expected: " + SUPPORTED_SCHEMA_VERSION);
        }

        List<ReconciliationClaimItemDTO> claims = payload.getClaims();
        if (claims == null || claims.isEmpty()) {
            throw new IllegalArgumentException("Reconciliation payload claims list cannot be empty.");
        }

        if (claims.size() > MAX_BATCH_SIZE) {
            throw new IllegalArgumentException("Payload size (" + claims.size() + ") exceeds maximum batch limit of " + MAX_BATCH_SIZE + " claims.");
        }

        // Validate payload duplicates internally within the incoming batch
        Set<String> seenInBatch = new HashSet<>();
        for (ReconciliationClaimItemDTO item : claims) {
            if (!seenInBatch.add(item.getClaimReference())) {
                throw new ReconciliationConflictException("Duplicate claim reference '" + item.getClaimReference() + "' detected within the same import payload.");
            }
        }

        int totalReceived = claims.size();
        int claimsInserted = 0;
        int claimsSkipped = 0;
        int historyRecordsInserted = 0;
        int indicatorsInserted = 0;
        int documentsInserted = 0;
        List<String> reconciledReferences = new ArrayList<>();

        for (ReconciliationClaimItemDTO item : claims) {
            Optional<Claim> existingOpt = claimRepository.findByClaimReference(item.getClaimReference());

            if (existingOpt.isPresent()) {
                Claim existing = existingOpt.get();
                validateNoConflict(existing, item);

                // Reconcile any missing history records for an existing claim idempotently
                int newHistCount = reconcileExistingClaimHistory(existing, item);
                if (newHistCount > 0) {
                    historyRecordsInserted += newHistCount;
                    reconciledReferences.add(item.getClaimReference() + " (history updated)");
                }
                claimsSkipped++;
            } else {
                // Ingest new candidate-era claim
                Claim newClaim = new Claim();
                newClaim.setClaimReference(item.getClaimReference());
                newClaim.setPolicyNumber(item.getPolicyNumber());
                newClaim.setClaimantName(item.getClaimantName());
                newClaim.setContactPhone(item.getContactPhone());
                newClaim.setContactEmail(item.getContactEmail());
                newClaim.setClaimType(item.getClaimType());
                newClaim.setIncidentDate(item.getIncidentDate());
                newClaim.setSubmissionDate(item.getSubmissionDate());
                newClaim.setClaimedAmount(item.getClaimedAmount());
                newClaim.setApprovedAmount(item.getApprovedAmount());
                newClaim.setStatus(item.getStatus());
                newClaim.setRiskPriority(item.getRiskPriority());
                newClaim.setIncidentDescription(item.getIncidentDescription());
                newClaim.setVehicleRegNumber(item.getVehicleRegNumber());
                newClaim.setIncidentLocation(item.getIncidentLocation());
                newClaim.setAssignedReviewer(item.getAssignedReviewer());

                // Ingest documents
                if (item.getDocuments() != null) {
                    for (ReconciliationDocumentDTO docDto : item.getDocuments()) {
                        ClaimDocument doc = new ClaimDocument(
                                docDto.getFileName(),
                                docDto.getDocumentType(),
                                docDto.getFileSizeBytes() != null ? docDto.getFileSizeBytes() : 0L,
                                docDto.getSha256Hash(),
                                docDto.getUploadedTimestamp() != null ? docDto.getUploadedTimestamp() : item.getSubmissionDate()
                        );
                        newClaim.addDocument(doc);
                        documentsInserted++;
                    }
                }

                // Ingest suspicious indicators
                if (item.getSuspiciousIndicators() != null) {
                    for (ReconciliationIndicatorDTO indDto : item.getSuspiciousIndicators()) {
                        SuspiciousIndicator ind = new SuspiciousIndicator(
                                indDto.getRuleCode(),
                                indDto.getTitle(),
                                indDto.getSeverity(),
                                indDto.getDescription(),
                                indDto.getTriggeredTimestamp() != null ? indDto.getTriggeredTimestamp() : item.getSubmissionDate()
                        );
                        newClaim.addSuspiciousIndicator(ind);
                        indicatorsInserted++;
                    }
                }

                // Ingest review history
                if (item.getReviewHistories() != null) {
                    validateDistinctHistoryEvents(item.getClaimReference(), item.getReviewHistories());
                    for (ReconciliationHistoryDTO histDto : item.getReviewHistories()) {
                        ReviewHistory hist = new ReviewHistory(
                                histDto.getActionType(),
                                histDto.getPreviousStatus(),
                                histDto.getNewStatus(),
                                histDto.getReviewerName(),
                                histDto.getReviewerRemarks(),
                                histDto.getActionTimestamp()
                        );
                        hist.setSourceEventId(histDto.getSourceEventIdentifier());
                        newClaim.addReviewHistory(hist);
                        historyRecordsInserted++;
                    }
                }

                claimRepository.save(newClaim);
                claimsInserted++;
                reconciledReferences.add(newClaim.getClaimReference());
            }
        }

        // Synchronize database identity sequences to ensure next auto-increment IDs do not collide
        syncIdentitySequences();

        logger.info("[RECONCILIATION SUCCESS] Received: {}, Inserted: {}, Skipped: {}, History Inserted: {}, Indicators: {}, Documents: {}",
                totalReceived, claimsInserted, claimsSkipped, historyRecordsInserted, indicatorsInserted, documentsInserted);

        return new ReconciliationResultDTO(
                totalReceived,
                claimsInserted,
                claimsSkipped,
                historyRecordsInserted,
                indicatorsInserted,
                documentsInserted,
                "SUCCESS",
                "Reconciliation successfully executed.",
                reconciledReferences
        );
    }

    private void validateNoConflict(Claim existing, ReconciliationClaimItemDTO item) {
        if (!Objects.equals(existing.getPolicyNumber(), item.getPolicyNumber()) ||
            !Objects.equals(existing.getClaimantName(), item.getClaimantName()) ||
            existing.getClaimType() != item.getClaimType() ||
            existing.getClaimedAmount().compareTo(item.getClaimedAmount()) != 0) {
            throw new ReconciliationConflictException("Conflicting core claim attributes for reference '" + item.getClaimReference() + "'. Baseline data does not match import payload.");
        }
    }

    private void validateDistinctHistoryEvents(String claimRef, List<ReconciliationHistoryDTO> histories) {
        if (histories == null || histories.size() <= 1) {
            return;
        }
        for (int i = 0; i < histories.size(); i++) {
            ReconciliationHistoryDTO h1 = histories.get(i);
            String id1 = h1.getSourceEventIdentifier();
            for (int j = i + 1; j < histories.size(); j++) {
                ReconciliationHistoryDTO h2 = histories.get(j);
                String id2 = h2.getSourceEventIdentifier();

                // If both records specify explicit source identifiers and they collide: conflict!
                if (id1 != null && id2 != null && Objects.equals(id1, id2)) {
                    throw new ReconciliationConflictException("Duplicate source event identifier '" + id1 + "' detected in review history for claim '" + claimRef + "'.");
                }

                // If neither has a distinct source identifier and all canonical event attributes match: ambiguous duplicate!
                if ((id1 == null || id2 == null) && isCanonicalFingerprintEqual(h1, h2)) {
                    throw new ReconciliationConflictException("Indistinguishable review history events detected for claim '" + claimRef + "'. Events sharing identical action type, statuses, remarks, and timestamps must provide distinct sourceEventId values.");
                }
            }
        }
    }

    private boolean isCanonicalFingerprintEqual(ReconciliationHistoryDTO h1, ReconciliationHistoryDTO h2) {
        if (!Objects.equals(h1.getActionType(), h2.getActionType())) return false;
        if (!Objects.equals(h1.getPreviousStatus(), h2.getPreviousStatus())) return false;
        if (!Objects.equals(h1.getNewStatus(), h2.getNewStatus())) return false;
        if (!Objects.equals(h1.getReviewerName(), h2.getReviewerName())) return false;
        if (!Objects.equals(h1.getReviewerRemarks(), h2.getReviewerRemarks())) return false;

        if (Objects.equals(h1.getActionTimestamp(), h2.getActionTimestamp())) return true;
        if (h1.getActionTimestamp() != null && h2.getActionTimestamp() != null) {
            return Math.abs(java.time.Duration.between(h1.getActionTimestamp(), h2.getActionTimestamp()).toMillis()) < 1000;
        }
        return false;
    }

    private boolean matchesExistingHistory(ReviewHistory existing, ReconciliationHistoryDTO candidate) {
        // Level 1: Authoritative match by stable sourceEventId if present on both
        String candidateSourceId = candidate.getSourceEventIdentifier();
        if (existing.getSourceEventId() != null && candidateSourceId != null) {
            return Objects.equals(existing.getSourceEventId(), candidateSourceId);
        }

        // Level 2: Canonical Event Fingerprint across immutable event fields
        if (!Objects.equals(existing.getActionType(), candidate.getActionType())) return false;
        if (!Objects.equals(existing.getPreviousStatus(), candidate.getPreviousStatus())) return false;
        if (!Objects.equals(existing.getNewStatus(), candidate.getNewStatus())) return false;
        if (!Objects.equals(existing.getReviewerName(), candidate.getReviewerName())) return false;
        if (!Objects.equals(existing.getReviewerRemarks(), candidate.getReviewerRemarks())) return false;

        if (Objects.equals(existing.getActionTimestamp(), candidate.getActionTimestamp())) return true;
        if (existing.getActionTimestamp() != null && candidate.getActionTimestamp() != null) {
            return Math.abs(java.time.Duration.between(
                    existing.getActionTimestamp(), candidate.getActionTimestamp()).toMillis()) < 1000;
        }
        return false;
    }

    private int reconcileExistingClaimHistory(Claim existing, ReconciliationClaimItemDTO item) {
        if (item.getReviewHistories() == null || item.getReviewHistories().isEmpty()) {
            return 0;
        }

        validateDistinctHistoryEvents(item.getClaimReference(), item.getReviewHistories());

        List<ReviewHistory> currentHistory = historyRepository.findByClaimIdOrderByActionTimestampAsc(existing.getId());
        int added = 0;

        for (ReconciliationHistoryDTO importHist : item.getReviewHistories()) {
            boolean exists = currentHistory.stream().anyMatch(h -> matchesExistingHistory(h, importHist));

            if (!exists) {
                ReviewHistory newHist = new ReviewHistory(
                        importHist.getActionType(),
                        importHist.getPreviousStatus(),
                        importHist.getNewStatus(),
                        importHist.getReviewerName(),
                        importHist.getReviewerRemarks(),
                        importHist.getActionTimestamp()
                );
                newHist.setSourceEventId(importHist.getSourceEventIdentifier());
                existing.addReviewHistory(newHist);
                existing.setStatus(importHist.getNewStatus());
                currentHistory.add(newHist);
                added++;
            }
        }

        if (added > 0) {
            claimRepository.save(existing);
        }
        return added;
    }

    void syncIdentitySequences() {
        if (entityManager == null) {
            return;
        }
        advanceSequence("claims");
        advanceSequence("review_history");
        advanceSequence("suspicious_indicators");
        advanceSequence("claim_documents");
    }

    void advanceSequence(String tableName) {
        Object maxObj = entityManager.createNativeQuery("SELECT COALESCE(MAX(id), 0) FROM " + tableName).getSingleResult();
        long nextId = ((Number) maxObj).longValue() + 1;
        entityManager.createNativeQuery("ALTER TABLE " + tableName + " ALTER COLUMN id RESTART WITH " + nextId).executeUpdate();
    }
}
