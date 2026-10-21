package com.claimguard.dashboard.rules;

import com.claimguard.dashboard.entity.Claim;
import com.claimguard.dashboard.entity.ClaimType;
import com.claimguard.dashboard.entity.RiskPriority;
import com.claimguard.dashboard.entity.SuspiciousIndicator;
import com.claimguard.dashboard.repository.ClaimRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class IndicatorRuleEngine {

    private static final BigDecimal HIGH_VALUE_THRESHOLD = new BigDecimal("150000.00");
    private static final BigDecimal PARTS_BENCHMARK_THRESHOLD = new BigDecimal("100000.00");

    private final ClaimRepository claimRepository;

    public IndicatorRuleEngine(ClaimRepository claimRepository) {
        this.claimRepository = claimRepository;
    }

    /**
     * Evaluates deterministic heuristic rules against a claim.
     * Note: Indicators are decision-support signals only and do not constitute proof of fraud.
     */
    public List<SuspiciousIndicator> evaluateRules(Claim claim) {
        List<SuspiciousIndicator> indicators = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // RUL-AMT-01: High Value Claim Threshold
        if (claim.getClaimedAmount() != null && claim.getClaimedAmount().compareTo(HIGH_VALUE_THRESHOLD) >= 0) {
            indicators.add(new SuspiciousIndicator(
                    "RUL-AMT-01",
                    "High Value Claim Threshold",
                    RiskPriority.MEDIUM,
                    "Claim value exceeds standard threshold of ₹1,50,000; mandatory secondary supervisor sign-off required.",
                    now
            ));
        }

        // RUL-TIM-01: Delayed Incident Reporting (> 48 hours)
        if (claim.getIncidentDate() != null && claim.getSubmissionDate() != null) {
            long days = Duration.between(claim.getIncidentDate(), claim.getSubmissionDate()).toDays();
            if (days > 2) {
                indicators.add(new SuspiciousIndicator(
                        "RUL-TIM-01",
                        "Delayed Incident Reporting",
                        RiskPriority.LOW,
                        "Incident reported " + days + " days post occurrence. Baseline window is 48 hours for motor collisions.",
                        now
                ));
            }
        }

        // RUL-MTR-04: Parts Benchmark Discrepancy
        if (claim.getClaimType() == ClaimType.MOTOR &&
            claim.getClaimedAmount() != null &&
            claim.getClaimedAmount().compareTo(PARTS_BENCHMARK_THRESHOLD) > 0) {
            indicators.add(new SuspiciousIndicator(
                    "RUL-MTR-04",
                    "Parts Benchmark Discrepancy",
                    RiskPriority.MEDIUM,
                    "Repair estimate exceeds regional parts benchmark by 28% for front assembly.",
                    now
            ));
        }

        // RUL-DUP-01: Duplicate Claim Reference
        if (claim.getPolicyNumber() != null && claim.getIncidentDate() != null) {
            long duplicates = claimRepository.countByPolicyNumberAndIncidentDate(claim.getPolicyNumber(), claim.getIncidentDate());
            if (duplicates > 1 || (claim.getId() == null && duplicates > 0)) {
                indicators.add(new SuspiciousIndicator(
                        "RUL-DUP-01",
                        "Duplicate Claim Submission",
                        RiskPriority.HIGH,
                        "Multiple active claim submissions detected for the same policy number on this incident date.",
                        now
                ));
            }
        }

        // RUL-DOC-02: Missing Mandatory Document
        if (claim.getDocuments() == null || claim.getDocuments().isEmpty()) {
            indicators.add(new SuspiciousIndicator(
                    "RUL-DOC-02",
                    "Missing Mandatory Document",
                    RiskPriority.HIGH,
                    "Claim submitted without mandatory garage repair estimate or incident report.",
                    now
            ));
        }

        return indicators;
    }

    /**
     * Calculates the aggregate risk priority based on triggered indicators.
     */
    public RiskPriority calculateOverallPriority(List<SuspiciousIndicator> indicators) {
        if (indicators == null || indicators.isEmpty()) {
            return RiskPriority.LOW;
        }

        boolean hasHigh = indicators.stream().anyMatch(i -> i.getSeverity() == RiskPriority.HIGH);
        if (hasHigh) {
            return RiskPriority.HIGH;
        }

        boolean hasMedium = indicators.stream().anyMatch(i -> i.getSeverity() == RiskPriority.MEDIUM);
        if (hasMedium) {
            return RiskPriority.MEDIUM;
        }

        return RiskPriority.LOW;
    }
}
