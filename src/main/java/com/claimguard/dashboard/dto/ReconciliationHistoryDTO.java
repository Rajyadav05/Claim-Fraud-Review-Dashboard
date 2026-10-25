package com.claimguard.dashboard.dto;

import com.claimguard.dashboard.entity.ClaimStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public class ReconciliationHistoryDTO {

    @NotBlank(message = "Action type is required.")
    private String actionType;

    private ClaimStatus previousStatus;

    @NotNull(message = "New status is required.")
    private ClaimStatus newStatus;

    private String reviewerName;
    private String reviewerRemarks;

    @NotNull(message = "Action timestamp is required.")
    private LocalDateTime actionTimestamp;

    private Long id;
    private String sourceEventId;

    public ReconciliationHistoryDTO() {
    }

    public ReconciliationHistoryDTO(String actionType, ClaimStatus previousStatus, ClaimStatus newStatus, String reviewerName, String reviewerRemarks, LocalDateTime actionTimestamp) {
        this.actionType = actionType;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.reviewerName = reviewerName;
        this.reviewerRemarks = reviewerRemarks;
        this.actionTimestamp = actionTimestamp;
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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSourceEventId() {
        return sourceEventId;
    }

    public void setSourceEventId(String sourceEventId) {
        this.sourceEventId = sourceEventId;
    }

    public String getSourceEventIdentifier() {
        if (sourceEventId != null && !sourceEventId.trim().isEmpty()) {
            return sourceEventId.trim();
        }
        return id != null ? String.valueOf(id) : null;
    }
}
