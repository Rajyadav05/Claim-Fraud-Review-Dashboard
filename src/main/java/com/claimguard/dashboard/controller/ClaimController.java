package com.claimguard.dashboard.controller;

import com.claimguard.dashboard.dto.ClaimDetailDTO;
import com.claimguard.dashboard.dto.ClaimSummaryDTO;
import com.claimguard.dashboard.dto.CreateClaimRequest;
import com.claimguard.dashboard.dto.DocumentMetadataRequest;
import com.claimguard.dashboard.dto.SuspiciousIndicatorDTO;
import com.claimguard.dashboard.entity.ClaimStatus;
import com.claimguard.dashboard.entity.ReviewHistory;
import com.claimguard.dashboard.entity.RiskPriority;
import com.claimguard.dashboard.entity.SuspiciousIndicator;
import com.claimguard.dashboard.service.ClaimService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/claims")
@CrossOrigin(origins = "*")
public class ClaimController {

    private final ClaimService claimService;

    public ClaimController(ClaimService claimService) {
        this.claimService = claimService;
    }

    @GetMapping
    public ResponseEntity<Page<ClaimSummaryDTO>> getClaims(
            @RequestParam(required = false) ClaimStatus status,
            @RequestParam(required = false) RiskPriority risk,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,desc") String[] sort) {

        Sort.Direction direction = Sort.Direction.DESC;
        String sortField = "id";
        if (sort.length > 0 && sort[0] != null) {
            String[] parts = sort[0].split(",");
            sortField = parts[0];
            if (parts.length > 1 && parts[1].equalsIgnoreCase("asc")) {
                direction = Sort.Direction.ASC;
            }
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));
        return ResponseEntity.ok(claimService.getClaims(status, risk, search, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClaimDetailDTO> getClaimById(@PathVariable Long id) {
        return ResponseEntity.ok(claimService.getClaimById(id));
    }

    @GetMapping("/reference/{reference}")
    public ResponseEntity<ClaimDetailDTO> getClaimByReference(@PathVariable String reference) {
        return ResponseEntity.ok(claimService.getClaimByReference(reference));
    }

    @GetMapping("/{id}/indicators")
    public ResponseEntity<List<SuspiciousIndicatorDTO>> getIndicators(@PathVariable Long id) {
        return ResponseEntity.ok(claimService.getClaimIndicators(id));
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<ReviewHistory>> getHistory(@PathVariable Long id) {
        return ResponseEntity.ok(claimService.getClaimHistory(id));
    }

    @PostMapping
    public ResponseEntity<ClaimDetailDTO> createClaim(@Valid @RequestBody CreateClaimRequest request) {
        ClaimDetailDTO created = claimService.createClaim(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/{id}/documents")
    public ResponseEntity<ClaimDetailDTO> addDocument(
            @PathVariable Long id,
            @Valid @RequestBody DocumentMetadataRequest docRequest) {
        ClaimDetailDTO updated = claimService.addDocumentMetadata(id, docRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(updated);
    }
}
