package com.claimguard.dashboard.dto;

import com.claimguard.dashboard.entity.ClaimStatus;
import com.claimguard.dashboard.entity.ClaimType;
import com.claimguard.dashboard.entity.RiskPriority;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ReconciliationClaimItemDTO {

    @NotBlank(message = "Claim reference is required.")
    private String claimReference;

    @NotBlank(message = "Policy number is required.")
    private String policyNumber;

    @NotBlank(message = "Claimant name is required.")
    private String claimantName;

    private String contactPhone;
    private String contactEmail;

    @NotNull(message = "Claim type is required.")
    private ClaimType claimType;

    @NotNull(message = "Incident date is required.")
    private LocalDateTime incidentDate;

    @NotNull(message = "Submission date is required.")
    private LocalDateTime submissionDate;

    @NotNull(message = "Claimed amount is required.")
    @DecimalMin(value = "0.01", message = "Claimed amount must be greater than zero.")
    private BigDecimal claimedAmount;

    private BigDecimal approvedAmount;

    @NotNull(message = "Status is required.")
    private ClaimStatus status;

    @NotNull(message = "Risk priority is required.")
    private RiskPriority riskPriority;

    private String incidentDescription;
    private String vehicleRegNumber;
    private String incidentLocation;
    private String assignedReviewer;

    @Valid
    private List<ReconciliationDocumentDTO> documents = new ArrayList<>();

    @Valid
    private List<ReconciliationIndicatorDTO> suspiciousIndicators = new ArrayList<>();

    @Valid
    private List<ReconciliationHistoryDTO> reviewHistories = new ArrayList<>();

    public ReconciliationClaimItemDTO() {
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

    public List<ReconciliationDocumentDTO> getDocuments() {
        return documents;
    }

    public void setDocuments(List<ReconciliationDocumentDTO> documents) {
        this.documents = documents != null ? documents : new ArrayList<>();
    }

    public List<ReconciliationIndicatorDTO> getSuspiciousIndicators() {
        return suspiciousIndicators;
    }

    public void setSuspiciousIndicators(List<ReconciliationIndicatorDTO> suspiciousIndicators) {
        this.suspiciousIndicators = suspiciousIndicators != null ? suspiciousIndicators : new ArrayList<>();
    }

    public List<ReconciliationHistoryDTO> getReviewHistories() {
        return reviewHistories;
    }

    public void setReviewHistories(List<ReconciliationHistoryDTO> reviewHistories) {
        this.reviewHistories = reviewHistories != null ? reviewHistories : new ArrayList<>();
    }
}
