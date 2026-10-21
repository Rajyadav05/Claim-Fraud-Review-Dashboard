package com.claimguard.dashboard.dto;

import com.claimguard.dashboard.entity.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ClaimDetailDTO {

    private Long id;
    private String claimReference;
    private String policyNumber;
    private String claimantName;
    private String contactPhone;
    private String contactEmail;
    private ClaimType claimType;
    private LocalDateTime incidentDate;
    private LocalDateTime submissionDate;
    private BigDecimal claimedAmount;
    private BigDecimal approvedAmount;
    private ClaimStatus status;
    private RiskPriority riskPriority;
    private String incidentDescription;
    private String vehicleRegNumber;
    private String incidentLocation;
    private String assignedReviewer;

    private List<ClaimDocument> documents = new ArrayList<>();
    private List<SuspiciousIndicator> suspiciousIndicators = new ArrayList<>();
    private List<ReviewHistory> reviewHistories = new ArrayList<>();

    public ClaimDetailDTO() {
    }

    public static ClaimDetailDTO fromEntity(Claim claim) {
        ClaimDetailDTO dto = new ClaimDetailDTO();
        dto.setId(claim.getId());
        dto.setClaimReference(claim.getClaimReference());
        dto.setPolicyNumber(claim.getPolicyNumber());
        dto.setClaimantName(claim.getClaimantName());
        dto.setContactPhone(claim.getContactPhone());
        dto.setContactEmail(claim.getContactEmail());
        dto.setClaimType(claim.getClaimType());
        dto.setIncidentDate(claim.getIncidentDate());
        dto.setSubmissionDate(claim.getSubmissionDate());
        dto.setClaimedAmount(claim.getClaimedAmount());
        dto.setApprovedAmount(claim.getApprovedAmount());
        dto.setStatus(claim.getStatus());
        dto.setRiskPriority(claim.getRiskPriority());
        dto.setIncidentDescription(claim.getIncidentDescription());
        dto.setVehicleRegNumber(claim.getVehicleRegNumber());
        dto.setIncidentLocation(claim.getIncidentLocation());
        dto.setAssignedReviewer(claim.getAssignedReviewer());
        if (claim.getDocuments() != null) {
            dto.setDocuments(new ArrayList<>(claim.getDocuments()));
        }
        if (claim.getSuspiciousIndicators() != null) {
            dto.setSuspiciousIndicators(new ArrayList<>(claim.getSuspiciousIndicators()));
        }
        if (claim.getReviewHistories() != null) {
            dto.setReviewHistories(new ArrayList<>(claim.getReviewHistories()));
        }
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

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
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

    public String getIncidentDescription() {
        return incidentDescription;
    }

    public void setIncidentDescription(String incidentDescription) {
        this.incidentDescription = incidentDescription;
    }

    public String getVehicleRegNumber() {
        return vehicleRegNumber;
    }

    public void setVehicleRegNumber(String vehicleRegNumber) {
        this.vehicleRegNumber = vehicleRegNumber;
    }

    public String getIncidentLocation() {
        return incidentLocation;
    }

    public void setIncidentLocation(String incidentLocation) {
        this.incidentLocation = incidentLocation;
    }

    public String getAssignedReviewer() {
        return assignedReviewer;
    }

    public void setAssignedReviewer(String assignedReviewer) {
        this.assignedReviewer = assignedReviewer;
    }

    public List<ClaimDocument> getDocuments() {
        return documents;
    }

    public void setDocuments(List<ClaimDocument> documents) {
        this.documents = documents;
    }

    public List<SuspiciousIndicator> getSuspiciousIndicators() {
        return suspiciousIndicators;
    }

    public void setSuspiciousIndicators(List<SuspiciousIndicator> suspiciousIndicators) {
        this.suspiciousIndicators = suspiciousIndicators;
    }

    public List<ReviewHistory> getReviewHistories() {
        return reviewHistories;
    }

    public void setReviewHistories(List<ReviewHistory> reviewHistories) {
        this.reviewHistories = reviewHistories;
    }
}
