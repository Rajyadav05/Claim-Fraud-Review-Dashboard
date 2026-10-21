package com.claimguard.dashboard.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "claims")
public class Claim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "claim_reference", nullable = false, unique = true, length = 30)
    private String claimReference;

    @Column(name = "policy_number", nullable = false, length = 50)
    private String policyNumber;

    @Column(name = "claimant_name", nullable = false, length = 100)
    private String claimantName;

    @Column(name = "contact_phone", length = 30)
    private String contactPhone;

    @Column(name = "contact_email", length = 100)
    private String contactEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "claim_type", nullable = false, length = 20)
    private ClaimType claimType;

    @Column(name = "incident_date", nullable = false)
    private LocalDateTime incidentDate;

    @Column(name = "submission_date", nullable = false)
    private LocalDateTime submissionDate;

    @Column(name = "claimed_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal claimedAmount;

    @Column(name = "approved_amount", precision = 12, scale = 2)
    private BigDecimal approvedAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ClaimStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_priority", nullable = false, length = 20)
    private RiskPriority riskPriority;

    @Column(name = "incident_description", length = 2000)
    private String incidentDescription;

    @Column(name = "vehicle_reg_number", length = 30)
    private String vehicleRegNumber;

    @Column(name = "incident_location", length = 150)
    private String incidentLocation;

    @Column(name = "assigned_reviewer", length = 100)
    private String assignedReviewer;

    @OneToMany(mappedBy = "claim", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<ClaimDocument> documents = new ArrayList<>();

    @OneToMany(mappedBy = "claim", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<SuspiciousIndicator> suspiciousIndicators = new ArrayList<>();

    @OneToMany(mappedBy = "claim", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("actionTimestamp ASC")
    @JsonManagedReference
    private List<ReviewHistory> reviewHistories = new ArrayList<>();

    public Claim() {
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

    public void addDocument(ClaimDocument document) {
        documents.add(document);
        document.setClaim(this);
    }

    public void addSuspiciousIndicator(SuspiciousIndicator indicator) {
        suspiciousIndicators.add(indicator);
        indicator.setClaim(this);
    }

    public void addReviewHistory(ReviewHistory history) {
        reviewHistories.add(history);
        history.setClaim(this);
    }
}
