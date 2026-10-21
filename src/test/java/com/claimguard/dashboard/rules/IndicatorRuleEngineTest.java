package com.claimguard.dashboard.rules;

import com.claimguard.dashboard.entity.*;
import com.claimguard.dashboard.repository.ClaimRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IndicatorRuleEngineTest {

    @Mock
    private ClaimRepository claimRepository;

    private IndicatorRuleEngine ruleEngine;

    @BeforeEach
    void setUp() {
        ruleEngine = new IndicatorRuleEngine(claimRepository);
    }

    @Test
    @DisplayName("RUL-AMT-01: Flag claim with amount >= ₹1,50,000")
    void shouldTriggerHighValueThresholdRule() {
        Claim claim = new Claim();
        claim.setClaimedAmount(new BigDecimal("185000.00"));
        claim.setClaimType(ClaimType.HEALTH);
        claim.addDocument(new ClaimDocument("bill.pdf", "INVOICE", 100L, "hash", LocalDateTime.now()));

        List<SuspiciousIndicator> indicators = ruleEngine.evaluateRules(claim);

        assertTrue(indicators.stream().anyMatch(i -> "RUL-AMT-01".equals(i.getRuleCode())));
    }

    @Test
    @DisplayName("RUL-TIM-01: Flag claim with delayed reporting > 48 hours")
    void shouldTriggerDelayedReportingRule() {
        Claim claim = new Claim();
        claim.setClaimedAmount(new BigDecimal("50000.00"));
        claim.setClaimType(ClaimType.HEALTH);
        claim.setIncidentDate(LocalDateTime.now().minusDays(5));
        claim.setSubmissionDate(LocalDateTime.now());
        claim.addDocument(new ClaimDocument("bill.pdf", "INVOICE", 100L, "hash", LocalDateTime.now()));

        List<SuspiciousIndicator> indicators = ruleEngine.evaluateRules(claim);

        assertTrue(indicators.stream().anyMatch(i -> "RUL-TIM-01".equals(i.getRuleCode())));
    }

    @Test
    @DisplayName("RUL-DOC-02: Flag claim with zero attached documents")
    void shouldTriggerMissingDocumentRule() {
        Claim claim = new Claim();
        claim.setClaimedAmount(new BigDecimal("50000.00"));
        claim.setClaimType(ClaimType.HEALTH);

        List<SuspiciousIndicator> indicators = ruleEngine.evaluateRules(claim);

        assertTrue(indicators.stream().anyMatch(i -> "RUL-DOC-02".equals(i.getRuleCode())));
    }

    @Test
    @DisplayName("Priority calculation: HIGH priority if any indicator is HIGH")
    void shouldCalculateHighOverallPriority() {
        SuspiciousIndicator high = new SuspiciousIndicator("CODE", "High Rule", RiskPriority.HIGH, "desc", LocalDateTime.now());
        SuspiciousIndicator med = new SuspiciousIndicator("CODE", "Med Rule", RiskPriority.MEDIUM, "desc", LocalDateTime.now());

        RiskPriority priority = ruleEngine.calculateOverallPriority(List.of(med, high));

        assertEquals(RiskPriority.HIGH, priority);
    }
}
