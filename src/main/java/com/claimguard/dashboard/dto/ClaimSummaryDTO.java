package com.claimguard.dashboard.dto;

import com.claimguard.dashboard.entity.Claim;
import com.claimguard.dashboard.entity.ClaimStatus;
import com.claimguard.dashboard.entity.ClaimType;
import com.claimguard.dashboard.entity.RiskPriority;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ClaimSummaryDTO {

    private Long id;
    private String claimReference;
    private String policyNumber;
    private String claimantName;
    private ClaimType claimType;
    private LocalDateTime incidentDate;
    private LocalDateTime submissionDate;
    private BigDecimal claimedAmount;
    private BigDecimal approvedAmount;
    private ClaimStatus status;
    private RiskPriority riskPriority;
    private int indicatorCount;
    private String vehicleRegNumber;
    private String assignedReviewer;

    public ClaimSummaryDTO() {
    }

    public static ClaimSummaryDTO fromEntity(Claim claim) {
        ClaimSummaryDTO dto = new ClaimSummaryDTO();
        dto.setId(claim.getId());
        dto.setClaimReference(claim.getClaimReference());
        dto.setPolicyNumber(claim.getPolicyNumber());
        dto.setClaimantName(claim.getClaimantName());
        dto.setClaimType(claim.getClaimType());
        dto.setIncidentDate(claim.getIncidentDate());
        dto.setSubmissionDate(claim.getSubmissionDate());
        dto.setClaimedAmount(claim.getClaimedAmount());
        dto.setApprovedAmount(claim.getApprovedAmount());
        dto.setStatus(claim.getStatus());
        dto.setRiskPriority(claim.getRiskPriority());
        dto.setIndicatorCount(claim.getSuspiciousIndicators() != null ? claim.getSuspiciousIndicators().size() : 0);
        dto.setVehicleRegNumber(claim.getVehicleRegNumber());
        dto.setAssignedReviewer(claim.getAssignedReviewer());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getClaimReference() {
        return claimReference;
    }

    public void setClaimReference(String claimReference) {
        this.claimReference = claimReference;
    }

    public String getPolicyNumber() {
        return policyNumber;
    }

    public void setPolicyNumber(String policyNumber) {
        this.policyNumber = policyNumber;
    }

    public String getClaimantName() {
        return claimantName;
    }

    public void setClaimantName(String claimantName) {
        this.claimantName = claimantName;
    }

    public ClaimType getClaimType() {
        return claimType;
    }

    public void setClaimType(ClaimType claimType) {
        this.claimType = claimType;
    }

    public LocalDateTime getIncidentDate() {
        return incidentDate;
    }

    public void setIncidentDate(LocalDateTime incidentDate) {
        this.incidentDate = incidentDate;
    }

    public LocalDateTime getSubmissionDate() {
        return submissionDate;
    }

    public void setSubmissionDate(LocalDateTime submissionDate) {
        this.submissionDate = submissionDate;
    }

    public BigDecimal getClaimedAmount() {
        return claimedAmount;
    }

    public void setClaimedAmount(BigDecimal claimedAmount) {
        this.claimedAmount = claimedAmount;
    }

    public BigDecimal getApprovedAmount() {
        return approvedAmount;
    }

    public void setApprovedAmount(BigDecimal approvedAmount) {
        this.approvedAmount = approvedAmount;
    }

    public ClaimStatus getStatus() {
        return status;
    }

    public void setStatus(ClaimStatus status) {
        this.status = status;
    }

    public RiskPriority getRiskPriority() {
        return riskPriority;
    }

    public void setRiskPriority(RiskPriority riskPriority) {
        this.riskPriority = riskPriority;
    }

    public int getIndicatorCount() {
        return indicatorCount;
    }

    public void setIndicatorCount(int indicatorCount) {
        this.indicatorCount = indicatorCount;
    }

    public String getVehicleRegNumber() {
        return vehicleRegNumber;
    }

    public void setVehicleRegNumber(String vehicleRegNumber) {
        this.vehicleRegNumber = vehicleRegNumber;
    }

    public String getAssignedReviewer() {
        return assignedReviewer;
    }

    public void setAssignedReviewer(String assignedReviewer) {
        this.assignedReviewer = assignedReviewer;
    }
}
