package com.claimguard.dashboard.controller;

import com.claimguard.dashboard.dto.ClaimDetailDTO;
import com.claimguard.dashboard.dto.RequestInfoRequest;
import com.claimguard.dashboard.dto.ReviewDecisionRequest;
import com.claimguard.dashboard.service.ClaimService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/claims/{id}")
@CrossOrigin(origins = "*")
public class ReviewController {

    private final ClaimService claimService;

    public ReviewController(ClaimService claimService) {
        this.claimService = claimService;
    }

    @PatchMapping("/review")
    public ResponseEntity<ClaimDetailDTO> startReview(
            @PathVariable Long id,
            @RequestBody(required = false) ReviewDecisionRequest request) {
        String reviewer = (request != null && request.getReviewerName() != null) ? request.getReviewerName() : "Priya Sharma";
        return ResponseEntity.ok(claimService.startReview(id, reviewer));
    }

    @PostMapping("/approve")
    public ResponseEntity<ClaimDetailDTO> approveClaim(
            @PathVariable Long id,
            @RequestBody(required = false) ReviewDecisionRequest request) {
        String remarks = (request != null) ? request.getRemarks() : "Claim approved by adjudicator.";
        return ResponseEntity.ok(claimService.approveClaim(id, remarks, request != null ? request.getDeductible() : null));
    }

    @PostMapping("/reject")
    public ResponseEntity<ClaimDetailDTO> rejectClaim(
            @PathVariable Long id,
            @RequestBody(required = false) ReviewDecisionRequest request) {
        String reasonCode = (request != null && request.getReasonCode() != null) ? request.getReasonCode() : "forensic_mismatch";
        String remarks = (request != null && request.getRemarks() != null) ? request.getRemarks() : "Claim rejected upon review.";
        return ResponseEntity.ok(claimService.rejectClaim(id, reasonCode, remarks));
    }

    @PostMapping("/request-info")
    public ResponseEntity<ClaimDetailDTO> requestInfo(
            @PathVariable Long id,
            @Valid @RequestBody RequestInfoRequest request) {
        return ResponseEntity.ok(claimService.requestMoreInfo(id, request.getQuery(), request.getReviewerName()));
    }
}
