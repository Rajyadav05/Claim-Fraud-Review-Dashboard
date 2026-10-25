package com.claimguard.dashboard.controller;

import com.claimguard.dashboard.dto.ReconciliationPayloadDTO;
import com.claimguard.dashboard.dto.ReconciliationResultDTO;
import com.claimguard.dashboard.service.ClaimReconciliationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final ClaimReconciliationService reconciliationService;

    public AdminController(ClaimReconciliationService reconciliationService) {
        this.reconciliationService = reconciliationService;
    }

    @PostMapping("/reconcile")
    public ResponseEntity<ReconciliationResultDTO> reconcileClaims(@Valid @RequestBody ReconciliationPayloadDTO payload) {
        ReconciliationResultDTO result = reconciliationService.reconcile(payload);
        return ResponseEntity.ok(result);
    }
}
