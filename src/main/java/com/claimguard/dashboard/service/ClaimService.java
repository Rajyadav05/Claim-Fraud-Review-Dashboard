package com.claimguard.dashboard.service;

import com.claimguard.dashboard.dto.ClaimDetailDTO;
import com.claimguard.dashboard.dto.ClaimSummaryDTO;
import com.claimguard.dashboard.dto.CreateClaimRequest;
import com.claimguard.dashboard.dto.DocumentMetadataRequest;
import com.claimguard.dashboard.dto.SuspiciousIndicatorDTO;
import com.claimguard.dashboard.entity.*;
import com.claimguard.dashboard.exception.InvalidStateTransitionException;
import com.claimguard.dashboard.exception.ResourceNotFoundException;
import com.claimguard.dashboard.repository.ClaimDocumentRepository;
import com.claimguard.dashboard.repository.ClaimRepository;
import com.claimguard.dashboard.repository.ReviewHistoryRepository;
import com.claimguard.dashboard.repository.SuspiciousIndicatorRepository;
import com.claimguard.dashboard.rules.IndicatorRuleEngine;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;

@Service
@Transactional
public class ClaimService {

    private final ClaimRepository claimRepository;
    private final ClaimDocumentRepository documentRepository;
    private final ReviewHistoryRepository historyRepository;
    private final SuspiciousIndicatorRepository indicatorRepository;
    private final IndicatorRuleEngine ruleEngine;

    public ClaimService(ClaimRepository claimRepository,
                        ClaimDocumentRepository documentRepository,
                        ReviewHistoryRepository historyRepository,
                        SuspiciousIndicatorRepository indicatorRepository,
                        IndicatorRuleEngine ruleEngine) {
        this.claimRepository = claimRepository;
        this.documentRepository = documentRepository;
        this.historyRepository = historyRepository;
        this.indicatorRepository = indicatorRepository;
        this.ruleEngine = ruleEngine;
    }

    public Page<ClaimSummaryDTO> getClaims(ClaimStatus status, RiskPriority risk, String search, Pageable pageable) {
        return claimRepository.findWithFilters(status, risk, search, pageable)
                .map(ClaimSummaryDTO::fromEntity);
    }

    public ClaimDetailDTO getClaimById(Long id) {
        Claim claim = claimRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Claim with ID " + id + " not found."));
        return ClaimDetailDTO.fromEntity(claim);
    }

    public ClaimDetailDTO getClaimByReference(String reference) {
        Claim claim = claimRepository.findByClaimReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException("Claim with reference " + reference + " not found."));
        return ClaimDetailDTO.fromEntity(claim);
    }

    public List<SuspiciousIndicatorDTO> getClaimIndicators(Long id) {
        if (!claimRepository.existsById(id)) {
            throw new ResourceNotFoundException("Claim with ID " + id + " not found.");
        }
        return indicatorRepository.findByClaimId(id).stream()
                .map(SuspiciousIndicatorDTO::fromEntity)
                .toList();
    }

    public List<ReviewHistory> getClaimHistory(Long id) {
        if (!claimRepository.existsById(id)) {
            throw new ResourceNotFoundException("Claim with ID " + id + " not found.");
        }
        return historyRepository.findByClaimIdOrderByActionTimestampAsc(id);
    }

    public ClaimDetailDTO createClaim(CreateClaimRequest request) {
        Claim claim = new Claim();
        long nextSeq = claimRepository.count() + 1;
        String reference = String.format("CLM-%d-%04d", Year.now().getValue(), nextSeq);

        claim.setClaimReference(reference);
        claim.setPolicyNumber(request.getPolicyNumber());
        claim.setClaimantName(request.getClaimantName());
        claim.setContactPhone(request.getContactPhone());
        claim.setContactEmail(request.getContactEmail());
        claim.setClaimType(request.getClaimType());
        claim.setIncidentDate(request.getIncidentDate());
        claim.setSubmissionDate(LocalDateTime.now());
        claim.setClaimedAmount(request.getClaimedAmount());
        claim.setStatus(ClaimStatus.SUBMITTED);
        claim.setIncidentDescription(request.getIncidentDescription());
        claim.setVehicleRegNumber(request.getVehicleRegNumber());
        claim.setIncidentLocation(request.getIncidentLocation());

        if (request.getDocuments() != null) {
            for (DocumentMetadataRequest docReq : request.getDocuments()) {
                ClaimDocument doc = new ClaimDocument(
                        docReq.getFileName(),
                        docReq.getDocumentType(),
                        docReq.getFileSizeBytes() != null ? docReq.getFileSizeBytes() : 0L,
                        docReq.getSha256Hash(),
                        LocalDateTime.now()
                );
                claim.addDocument(doc);
            }
        }

        // Evaluate heuristic suspicious indicator rules
        List<SuspiciousIndicator> indicators = ruleEngine.evaluateRules(claim);
        indicators.forEach(claim::addSuspiciousIndicator);
        claim.setRiskPriority(ruleEngine.calculateOverallPriority(indicators));

        // Create initial immutable review history record
        ReviewHistory history = new ReviewHistory(
                "SUBMITTED",
                null,
                ClaimStatus.SUBMITTED,
                "System Intake",
                "Claim successfully ingested via API with " + indicators.size() + " heuristic indicator(s) evaluated.",
                LocalDateTime.now()
        );
        claim.addReviewHistory(history);

        Claim saved = claimRepository.save(claim);
        return ClaimDetailDTO.fromEntity(saved);
    }

    public ClaimDetailDTO startReview(Long id, String reviewerName) {
        Claim claim = claimRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Claim with ID " + id + " not found."));

        if (claim.getStatus() == ClaimStatus.APPROVED || claim.getStatus() == ClaimStatus.REJECTED) {
            throw new InvalidStateTransitionException("Cannot start review on a claim that is already " + claim.getStatus() + ".");
        }

        ClaimStatus prev = claim.getStatus();
        claim.setStatus(ClaimStatus.UNDER_REVIEW);
        if (reviewerName != null && !reviewerName.isBlank()) {
            claim.setAssignedReviewer(reviewerName);
        }

        ReviewHistory history = new ReviewHistory(
                "REVIEW_STARTED",
                prev,
                ClaimStatus.UNDER_REVIEW,
                claim.getAssignedReviewer() != null ? claim.getAssignedReviewer() : "Reviewer",
                "Claim review initiated by analyst.",
                LocalDateTime.now()
        );
        claim.addReviewHistory(history);

        return ClaimDetailDTO.fromEntity(claimRepository.save(claim));
    }

    public ClaimDetailDTO approveClaim(Long id, String remarks, BigDecimal deductible) {
        Claim claim = claimRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Claim with ID " + id + " not found."));

        if (claim.getStatus() == ClaimStatus.APPROVED || claim.getStatus() == ClaimStatus.REJECTED) {
            throw new InvalidStateTransitionException("Cannot approve a claim that has already been resolved.");
        }

        BigDecimal effectiveDeductible = deductible != null ? deductible : BigDecimal.ZERO;
        BigDecimal approved = claim.getClaimedAmount().subtract(effectiveDeductible);
        if (approved.compareTo(BigDecimal.ZERO) < 0) {
            approved = BigDecimal.ZERO;
        }

        ClaimStatus prev = claim.getStatus();
        claim.setStatus(ClaimStatus.APPROVED);
        claim.setApprovedAmount(approved);

        ReviewHistory history = new ReviewHistory(
                "APPROVED",
                prev,
                ClaimStatus.APPROVED,
                claim.getAssignedReviewer() != null ? claim.getAssignedReviewer() : "Adjudicator",
                remarks != null && !remarks.isBlank() ? remarks : "Claim settlement approved for ₹" + approved,
                LocalDateTime.now()
        );
        claim.addReviewHistory(history);

        return ClaimDetailDTO.fromEntity(claimRepository.save(claim));
    }

    public ClaimDetailDTO rejectClaim(Long id, String reasonCode, String remarks) {
        Claim claim = claimRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Claim with ID " + id + " not found."));

        if (claim.getStatus() == ClaimStatus.APPROVED || claim.getStatus() == ClaimStatus.REJECTED) {
            throw new InvalidStateTransitionException("Cannot reject a claim that has already been resolved.");
        }

        ClaimStatus prev = claim.getStatus();
        claim.setStatus(ClaimStatus.REJECTED);

        String detail = (reasonCode != null ? "[" + reasonCode + "] " : "") +
                        (remarks != null ? remarks : "Claim rejected based on forensic evaluation.");

        ReviewHistory history = new ReviewHistory(
                "REJECTED",
                prev,
                ClaimStatus.REJECTED,
                claim.getAssignedReviewer() != null ? claim.getAssignedReviewer() : "Adjudicator",
                detail,
                LocalDateTime.now()
        );
        claim.addReviewHistory(history);

        return ClaimDetailDTO.fromEntity(claimRepository.save(claim));
    }

    public ClaimDetailDTO requestMoreInfo(Long id, String query, String reviewerName) {
        Claim claim = claimRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Claim with ID " + id + " not found."));

        if (claim.getStatus() == ClaimStatus.APPROVED || claim.getStatus() == ClaimStatus.REJECTED) {
            throw new InvalidStateTransitionException("Cannot request additional info on a claim that is already " + claim.getStatus() + ".");
        }

        ClaimStatus prev = claim.getStatus();
        claim.setStatus(ClaimStatus.MORE_INFO_REQUIRED);
        if (reviewerName != null && !reviewerName.isBlank()) {
            claim.setAssignedReviewer(reviewerName);
        }

        ReviewHistory history = new ReviewHistory(
                "INFO_REQUESTED",
                prev,
                ClaimStatus.MORE_INFO_REQUIRED,
                claim.getAssignedReviewer() != null ? claim.getAssignedReviewer() : "Investigator",
                query != null && !query.isBlank() ? query : "Additional supporting evidence requested from policyholder.",
                LocalDateTime.now()
        );
        claim.addReviewHistory(history);

        return ClaimDetailDTO.fromEntity(claimRepository.save(claim));
    }

    public ClaimDetailDTO addDocumentMetadata(Long id, DocumentMetadataRequest docRequest) {
        Claim claim = claimRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Claim with ID " + id + " not found."));

        ClaimDocument document = new ClaimDocument(
                docRequest.getFileName(),
                docRequest.getDocumentType(),
                docRequest.getFileSizeBytes() != null ? docRequest.getFileSizeBytes() : 0L,
                docRequest.getSha256Hash(),
                LocalDateTime.now()
        );
        claim.addDocument(document);

        ClaimStatus prev = claim.getStatus();
        // If claim was waiting for more info, claimant document submission transitions it back to UNDER_REVIEW
        if (claim.getStatus() == ClaimStatus.MORE_INFO_REQUIRED) {
            claim.setStatus(ClaimStatus.UNDER_REVIEW);
        }

        ReviewHistory history = new ReviewHistory(
                "DOCUMENT_UPLOADED",
                prev,
                claim.getStatus(),
                claim.getAssignedReviewer() != null ? claim.getAssignedReviewer() : "System",
                "Supporting document metadata registered: " + docRequest.getFileName() + " (" + docRequest.getDocumentType() + ").",
                LocalDateTime.now()
        );
        claim.addReviewHistory(history);

        return ClaimDetailDTO.fromEntity(claimRepository.save(claim));
    }
}
