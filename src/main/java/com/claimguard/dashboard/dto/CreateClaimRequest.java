package com.claimguard.dashboard.dto;

import com.claimguard.dashboard.entity.ClaimType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class CreateClaimRequest {

    @NotBlank(message = "Policy number is required.")
    private String policyNumber;

    @NotBlank(message = "Claimant name is required.")
    private String claimantName;

    private String contactPhone;

    @Email(message = "Invalid email format.")
    private String contactEmail;

    @NotNull(message = "Claim type is required.")
    private ClaimType claimType;

    @NotNull(message = "Incident date is required.")
    @PastOrPresent(message = "Incident date cannot be in the future.")
    private LocalDateTime incidentDate;

    @NotNull(message = "Claimed amount is required.")
    @DecimalMin(value = "0.01", message = "Claimed amount must be greater than zero.")
    private BigDecimal claimedAmount;

    private String incidentDescription;

    private String vehicleRegNumber;

    private String incidentLocation;

    private List<DocumentMetadataRequest> documents;

    public CreateClaimRequest() {
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

    public BigDecimal getClaimedAmount() {
        return claimedAmount;
    }

    public void setClaimedAmount(BigDecimal claimedAmount) {
        this.claimedAmount = claimedAmount;
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

    public List<DocumentMetadataRequest> getDocuments() {
        return documents;
    }

    public void setDocuments(List<DocumentMetadataRequest> documents) {
        this.documents = documents;
    }
}
