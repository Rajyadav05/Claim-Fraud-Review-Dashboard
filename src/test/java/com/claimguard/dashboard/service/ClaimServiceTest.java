package com.claimguard.dashboard.service;

import com.claimguard.dashboard.dto.ClaimDetailDTO;
import com.claimguard.dashboard.dto.CreateClaimRequest;
import com.claimguard.dashboard.entity.*;
import com.claimguard.dashboard.exception.InvalidStateTransitionException;
import com.claimguard.dashboard.repository.ClaimDocumentRepository;
import com.claimguard.dashboard.repository.ClaimRepository;
import com.claimguard.dashboard.repository.ReviewHistoryRepository;
import com.claimguard.dashboard.rules.IndicatorRuleEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.claimguard.dashboard.dto.SuspiciousIndicatorDTO;
import com.claimguard.dashboard.exception.ResourceNotFoundException;
import com.claimguard.dashboard.repository.SuspiciousIndicatorRepository;

@ExtendWith(MockitoExtension.class)
class ClaimServiceTest {

    @Mock
    private ClaimRepository claimRepository;

    @Mock
    private ClaimDocumentRepository documentRepository;

    @Mock
    private ReviewHistoryRepository historyRepository;

    @Mock
    private SuspiciousIndicatorRepository indicatorRepository;

    @Mock
    private IndicatorRuleEngine ruleEngine;

    @InjectMocks
    private ClaimService claimService;

    private Claim testClaim;

    @BeforeEach
    void setUp() {
        testClaim = new Claim();
        testClaim.setId(1L);
        testClaim.setClaimReference("CLM-2026-0001");
        testClaim.setClaimantName("Rajesh Kumar");
        testClaim.setPolicyNumber("POL-MH-9482-1049");
        testClaim.setClaimType(ClaimType.MOTOR);
        testClaim.setClaimedAmount(new BigDecimal("185000.00"));
        testClaim.setStatus(ClaimStatus.SUBMITTED);
        testClaim.setRiskPriority(RiskPriority.HIGH);
        testClaim.setIncidentDate(LocalDateTime.now().minusDays(3));
        testClaim.setSubmissionDate(LocalDateTime.now());
    }

    @Test
    @DisplayName("Create Claim: Correctly assigns reference and evaluates initial status")
    void shouldCreateClaimSuccessfully() {
        CreateClaimRequest request = new CreateClaimRequest();
        request.setClaimantName("Rajesh Kumar");
        request.setPolicyNumber("POL-MH-9482-1049");
        request.setClaimType(ClaimType.MOTOR);
        request.setClaimedAmount(new BigDecimal("185000.00"));
        request.setIncidentDate(LocalDateTime.now().minusDays(2));

        when(claimRepository.count()).thenReturn(0L);
        when(ruleEngine.evaluateRules(any())).thenReturn(new ArrayList<>());
        when(ruleEngine.calculateOverallPriority(any())).thenReturn(RiskPriority.LOW);
        when(claimRepository.save(any(Claim.class))).thenAnswer(invocation -> {
            Claim c = invocation.getArgument(0);
            c.setId(1L);
            return c;
        });

        ClaimDetailDTO result = claimService.createClaim(request);

        assertNotNull(result);
        assertEquals(ClaimStatus.SUBMITTED, result.getStatus());
        assertEquals("CLM-2026-0001", result.getClaimReference());
        verify(claimRepository).save(any(Claim.class));
    }

    @Test
    @DisplayName("Approve Claim: Transitions to APPROVED and calculates net approved amount")
    void shouldApproveClaimSuccessfully() {
        when(claimRepository.findById(1L)).thenReturn(Optional.of(testClaim));
        when(claimRepository.save(any(Claim.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ClaimDetailDTO approved = claimService.approveClaim(1L, "Verified and approved.", new BigDecimal("5000.00"));

        assertEquals(ClaimStatus.APPROVED, approved.getStatus());
        assertEquals(new BigDecimal("180000.00"), approved.getApprovedAmount());
    }

    @Test
    @DisplayName("Reject Claim: Transitions to REJECTED")
    void shouldRejectClaimSuccessfully() {
        when(claimRepository.findById(1L)).thenReturn(Optional.of(testClaim));
        when(claimRepository.save(any(Claim.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ClaimDetailDTO rejected = claimService.rejectClaim(1L, "fraud_suspected", "Survey inspection discrepancies.");

        assertEquals(ClaimStatus.REJECTED, rejected.getStatus());
    }

    @Test
    @DisplayName("Invalid Transition: Cannot approve already resolved claim")
    void shouldThrowWhenApprovingAlreadyResolvedClaim() {
        testClaim.setStatus(ClaimStatus.REJECTED);
        when(claimRepository.findById(1L)).thenReturn(Optional.of(testClaim));

        assertThrows(InvalidStateTransitionException.class, () ->
                claimService.approveClaim(1L, "remarks", BigDecimal.ZERO));
    }

    @Test
    @DisplayName("Get Indicators: Returns mapped SuspiciousIndicatorDTO list when claim exists")
    void shouldReturnClaimIndicatorsSuccessfully() {
        when(claimRepository.existsById(1L)).thenReturn(true);
        SuspiciousIndicator ind = new SuspiciousIndicator("RUL-AMT-01", "High Value", RiskPriority.HIGH, "Amount >= 150000", LocalDateTime.now());
        ind.setId(10L);
        when(indicatorRepository.findByClaimId(1L)).thenReturn(List.of(ind));

        List<SuspiciousIndicatorDTO> indicators = claimService.getClaimIndicators(1L);

        assertNotNull(indicators);
        assertEquals(1, indicators.size());
        assertEquals("RUL-AMT-01", indicators.get(0).getRuleCode());
        assertEquals("High Value", indicators.get(0).getTitle());
        assertEquals(RiskPriority.HIGH, indicators.get(0).getSeverity());
    }

    @Test
    @DisplayName("Get Indicators: Throws ResourceNotFoundException when claim does not exist")
    void shouldThrowNotFoundWhenClaimIndicatorsClaimMissing() {
        when(claimRepository.existsById(999L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () ->
                claimService.getClaimIndicators(999L));
    }
}
