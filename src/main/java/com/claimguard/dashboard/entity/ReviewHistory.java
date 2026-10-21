package com.claimguard.dashboard.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "review_history")
public class ReviewHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "claim_id", nullable = false)
    @JsonBackReference
    private Claim claim;

    @Column(name = "action_type", nullable = false, length = 50)
    private String actionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", length = 30)
    private ClaimStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, length = 30)
    private ClaimStatus newStatus;

    @Column(name = "reviewer_name", length = 100)
    private String reviewerName;

    @Column(name = "reviewer_remarks", length = 2000)
    private String reviewerRemarks;

    @Column(name = "action_timestamp", nullable = false)
    private LocalDateTime actionTimestamp;

    public ReviewHistory() {
    }

    public ReviewHistory(String actionType, ClaimStatus previousStatus, ClaimStatus newStatus, String reviewerName, String reviewerRemarks, LocalDateTime actionTimestamp) {
        this.actionType = actionType;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.reviewerName = reviewerName;
        this.reviewerRemarks = reviewerRemarks;
        this.actionTimestamp = actionTimestamp;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Claim getClaim() {
        return claim;
    }

    public void setClaim(Claim claim) {
        this.claim = claim;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public ClaimStatus getPreviousStatus() {
        return previousStatus;
    }

    public void setPreviousStatus(ClaimStatus previousStatus) {
        this.previousStatus = previousStatus;
    }

    public ClaimStatus getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(ClaimStatus newStatus) {
        this.newStatus = newStatus;
    }

    public String getReviewerName() {
        return reviewerName;
    }

    public void setReviewerName(String reviewerName) {
        this.reviewerName = reviewerName;
    }

    public String getReviewerRemarks() {
        return reviewerRemarks;
    }

    public void setReviewerRemarks(String reviewerRemarks) {
        this.reviewerRemarks = reviewerRemarks;
    }

    public LocalDateTime getActionTimestamp() {
        return actionTimestamp;
    }

    public void setActionTimestamp(LocalDateTime actionTimestamp) {
        this.actionTimestamp = actionTimestamp;
    }
}
