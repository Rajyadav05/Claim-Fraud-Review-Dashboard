package com.claimguard.dashboard.service;

import com.claimguard.dashboard.entity.*;
import com.claimguard.dashboard.repository.ClaimRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
@Order(2)
public class DataSeederService implements CommandLineRunner {

    private final ClaimRepository claimRepository;

    public DataSeederService(ClaimRepository claimRepository) {
        this.claimRepository = claimRepository;
    }

    @Override
    public void run(String... args) {
        if (claimRepository.count() > 0) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        // Claim 1: High-risk motor claim under review (Primary demonstration case)
        Claim claim1 = new Claim();
        claim1.setClaimReference("CLM-2026-0001");
        claim1.setPolicyNumber("POL-MH-9482-1049");
        claim1.setClaimantName("Rajesh Kumar");
        claim1.setContactPhone("+91 98201 12345");
        claim1.setContactEmail("r.kumar@nexuscorp.in");
        claim1.setClaimType(ClaimType.MOTOR);
        claim1.setIncidentDate(now.minusDays(4));
        claim1.setSubmissionDate(now.minusDays(1));
        claim1.setClaimedAmount(new BigDecimal("185000.00"));
        claim1.setStatus(ClaimStatus.UNDER_REVIEW);
        claim1.setRiskPriority(RiskPriority.HIGH);
        claim1.setIncidentDescription("Rear-end collision at Western Express Highway intersection during peak evening traffic. Front bumper, hood, and radiator damaged.");
        claim1.setVehicleRegNumber("MH-02-CE-8841");
        claim1.setIncidentLocation("Andheri East, Mumbai");
        claim1.setAssignedReviewer("Priya Sharma");

        claim1.addDocument(new ClaimDocument(
                "Garage_Estimate_ApexMotors.pdf",
                "INVOICE",
                2457600L,
                "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                now.minusDays(1)
        ));
        claim1.addDocument(new ClaimDocument(
                "Police_Accident_Report_FIR_0492.pdf",
                "POLICE_REPORT",
                1843200L,
                "b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9",
                now.minusDays(1)
        ));
        claim1.addDocument(new ClaimDocument(
                "Front_Impact_Damage_Photo_01.jpg",
                "DAMAGE_PHOTO",
                3145728L,
                "4b227777d4dd1fc61c6f884f48641d02b4d121d3fd328cb08b5531fcacdabf8a",
                now.minusDays(1)
        ));

        claim1.addSuspiciousIndicator(new SuspiciousIndicator(
                "RUL-AMT-01",
                "High Value Claim Threshold",
                RiskPriority.MEDIUM,
                "Claim value ₹1,85,000 exceeds standard threshold of ₹1,50,000; requires secondary supervisor sign-off.",
                now.minusDays(1)
        ));
        claim1.addSuspiciousIndicator(new SuspiciousIndicator(
                "RUL-MTR-04",
                "Parts Benchmark Discrepancy",
                RiskPriority.MEDIUM,
                "Repair estimate exceeds regional parts benchmark catalog by 28% for front radiator assembly.",
                now.minusDays(1)
        ));
        claim1.addSuspiciousIndicator(new SuspiciousIndicator(
                "RUL-TIM-01",
                "Delayed Incident Reporting",
                RiskPriority.LOW,
                "Incident reported 3 days post occurrence. Standard reporting window is 48 hours for motor collisions.",
                now.minusDays(1)
        ));

        claim1.addReviewHistory(new ReviewHistory(
                "SUBMITTED",
                null,
                ClaimStatus.SUBMITTED,
                "System Intake",
                "Synthetic claim ingested via API. Heuristic scoring triggered 3 suspicious indicators.",
                now.minusDays(1)
        ));
        claim1.addReviewHistory(new ReviewHistory(
                "REVIEW_STARTED",
                ClaimStatus.SUBMITTED,
                ClaimStatus.UNDER_REVIEW,
                "Priya Sharma",
                "Assigned to Senior Fraud Analyst Priya Sharma for catalog price cross-verification.",
                now.minusHours(6)
        ));

        claimRepository.save(claim1);

        // Claim 2: Low-risk freshly submitted claim
        Claim claim2 = new Claim();
        claim2.setClaimReference("CLM-2026-0002");
        claim2.setPolicyNumber("POL-KA-5512-3021");
        claim2.setClaimantName("Anita Desai");
        claim2.setContactPhone("+91 94481 67890");
        claim2.setContactEmail("anita.desai@gmail.com");
        claim2.setClaimType(ClaimType.HEALTH);
        claim2.setIncidentDate(now.minusDays(1));
        claim2.setSubmissionDate(now.minusHours(3));
        claim2.setClaimedAmount(new BigDecimal("45000.00"));
        claim2.setStatus(ClaimStatus.SUBMITTED);
        claim2.setRiskPriority(RiskPriority.LOW);
        claim2.setIncidentDescription("Day-care surgical procedure reimbursement for cataract extraction at Manipal Hospital.");
        claim2.setIncidentLocation("Indiranagar, Bengaluru");

        claim2.addDocument(new ClaimDocument(
                "Hospital_Discharge_Summary.pdf",
                "MEDICAL_REPORT",
                1245184L,
                "a1b2c3d4e5f67890123456789abcdef0123456789abcdef0123456789abcdef0",
                now.minusHours(3)
        ));
        claim2.addReviewHistory(new ReviewHistory(
                "SUBMITTED",
                null,
                ClaimStatus.SUBMITTED,
                "System Intake",
                "Claim ingested. No suspicious heuristic anomalies flagged.",
                now.minusHours(3)
        ));

        claimRepository.save(claim2);

        // Claim 3: Property claim with missing documents (High Risk)
        Claim claim3 = new Claim();
        claim3.setClaimReference("CLM-2026-0003");
        claim3.setPolicyNumber("POL-DL-8821-4491");
        claim3.setClaimantName("Vikram Mehta");
        claim3.setContactPhone("+91 98110 54321");
        claim3.setContactEmail("v.mehta@delhitech.org");
        claim3.setClaimType(ClaimType.PROPERTY);
        claim3.setIncidentDate(now.minusDays(6));
        claim3.setSubmissionDate(now.minusDays(2));
        claim3.setClaimedAmount(new BigDecimal("320000.00"));
        claim3.setStatus(ClaimStatus.MORE_INFO_REQUIRED);
        claim3.setRiskPriority(RiskPriority.HIGH);
        claim3.setIncidentDescription("Water seepage and electrical short-circuit damage to office server room during heavy monsoon downpour.");
        claim3.setIncidentLocation("Connaught Place, New Delhi");
        claim3.setAssignedReviewer("Arun Verma");

        claim3.addSuspiciousIndicator(new SuspiciousIndicator(
                "RUL-AMT-01",
                "High Value Claim Threshold",
                RiskPriority.MEDIUM,
                "Claim value ₹3,20,000 exceeds standard threshold of ₹1,50,000.",
                now.minusDays(2)
        ));
        claim3.addSuspiciousIndicator(new SuspiciousIndicator(
                "RUL-DOC-02",
                "Missing Mandatory Document",
                RiskPriority.HIGH,
                "Claim submitted without mandatory certified loss surveyor report or equipment repair invoice.",
                now.minusDays(2)
        ));
        claim3.addReviewHistory(new ReviewHistory(
                "SUBMITTED",
                null,
                ClaimStatus.SUBMITTED,
                "System Intake",
                "Claim ingested with 2 high-priority indicators.",
                now.minusDays(2)
        ));
        claim3.addReviewHistory(new ReviewHistory(
                "INFO_REQUESTED",
                ClaimStatus.SUBMITTED,
                ClaimStatus.MORE_INFO_REQUIRED,
                "Arun Verma",
                "Requested certified loss surveyor report and equipment replacement invoices from policyholder.",
                now.minusDays(1)
        ));

        claimRepository.save(claim3);

        // Claim 4: Approved Motor Claim
        Claim claim4 = new Claim();
        claim4.setClaimReference("CLM-2026-0004");
        claim4.setPolicyNumber("POL-DL-1102-7740");
        claim4.setClaimantName("Sunita Rao");
        claim4.setContactPhone("+91 99002 33445");
        claim4.setContactEmail("sunita.rao@outlook.com");
        claim4.setClaimType(ClaimType.MOTOR);
        claim4.setIncidentDate(now.minusDays(10));
        claim4.setSubmissionDate(now.minusDays(8));
        claim4.setClaimedAmount(new BigDecimal("82000.00"));
        claim4.setApprovedAmount(new BigDecimal("80000.00"));
        claim4.setStatus(ClaimStatus.APPROVED);
        claim4.setRiskPriority(RiskPriority.LOW);
        claim4.setIncidentDescription("Minor side panel dent and scratch caused while reversing in parking lot.");
        claim4.setVehicleRegNumber("DL-01-AB-1234");
        claim4.setIncidentLocation("Saket, New Delhi");
        claim4.setAssignedReviewer("Priya Sharma");

        claim4.addDocument(new ClaimDocument(
                "Garage_Repair_Invoice.pdf",
                "INVOICE",
                1048576L,
                "c0d1e2f3a4b5c6d7e8f90123456789abcdef0123456789abcdef0123456789ab",
                now.minusDays(8)
        ));
        claim4.addReviewHistory(new ReviewHistory(
                "SUBMITTED",
                null,
                ClaimStatus.SUBMITTED,
                "System Intake",
                "Claim submitted.",
                now.minusDays(8)
        ));
        claim4.addReviewHistory(new ReviewHistory(
                "APPROVED",
                ClaimStatus.SUBMITTED,
                ClaimStatus.APPROVED,
                "Priya Sharma",
                "Authorized ₹80,000 settlement after standard ₹2,000 policy deductible applied.",
                now.minusDays(5)
        ));

        claimRepository.save(claim4);

        // Claim 5: Rejected Fraudulent Motor Claim
        Claim claim5 = new Claim();
        claim5.setClaimReference("CLM-2026-0005");
        claim5.setPolicyNumber("POL-KA-7788-9900");
        claim5.setClaimantName("Rahul Verma");
        claim5.setContactPhone("+91 97401 22334");
        claim5.setContactEmail("r.verma88@yahoo.com");
        claim5.setClaimType(ClaimType.MOTOR);
        claim5.setIncidentDate(now.minusDays(15));
        claim5.setSubmissionDate(now.minusDays(10));
        claim5.setClaimedAmount(new BigDecimal("215000.00"));
        claim5.setStatus(ClaimStatus.REJECTED);
        claim5.setRiskPriority(RiskPriority.HIGH);
        claim5.setIncidentDescription("Total engine seizure claimed due to unexpected water-logging during flash flood.");
        claim5.setVehicleRegNumber("KA-03-MD-5521");
        claim5.setIncidentLocation("Whitefield, Bengaluru");
        claim5.setAssignedReviewer("Arun Verma");

        claim5.addSuspiciousIndicator(new SuspiciousIndicator(
                "RUL-AMT-01",
                "High Value Claim Threshold",
                RiskPriority.MEDIUM,
                "Claim value ₹2,15,000 exceeds standard threshold of ₹1,50,000.",
                now.minusDays(10)
        ));
        claim5.addSuspiciousIndicator(new SuspiciousIndicator(
                "RUL-TIM-01",
                "Delayed Incident Reporting",
                RiskPriority.LOW,
                "Incident reported 5 days post occurrence without emergency service log.",
                now.minusDays(10)
        ));
        claim5.addSuspiciousIndicator(new SuspiciousIndicator(
                "RUL-DOC-02",
                "Missing Mandatory Document",
                RiskPriority.HIGH,
                "No hydro-static lock survey verification or towing dispatch receipt provided.",
                now.minusDays(10)
        ));

        claim5.addReviewHistory(new ReviewHistory(
                "SUBMITTED",
                null,
                ClaimStatus.SUBMITTED,
                "System Intake",
                "Claim ingested with multiple heuristic flags.",
                now.minusDays(10)
        ));
        claim5.addReviewHistory(new ReviewHistory(
                "REJECTED",
                ClaimStatus.SUBMITTED,
                ClaimStatus.REJECTED,
                "Arun Verma",
                "Reason: [mismatch] Independent surveyor inspection confirmed engine damage predated policy inception date.",
                now.minusDays(7)
        ));

        claimRepository.save(claim5);
    }
}
