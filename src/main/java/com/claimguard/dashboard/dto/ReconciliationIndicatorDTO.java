package com.claimguard.dashboard.dto;

import com.claimguard.dashboard.entity.RiskPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public class ReconciliationIndicatorDTO {

    @NotBlank(message = "Rule code is required.")
    private String ruleCode;

    @NotBlank(message = "Title is required.")
    private String title;

    @NotNull(message = "Severity is required.")
    private RiskPriority severity;

    private String description;

    @NotNull(message = "Triggered timestamp is required.")
    private LocalDateTime triggeredTimestamp;

    public ReconciliationIndicatorDTO() {
    }

    public ReconciliationIndicatorDTO(String ruleCode, String title, RiskPriority severity, String description, LocalDateTime triggeredTimestamp) {
        this.ruleCode = ruleCode;
        this.title = title;
        this.severity = severity;
        this.description = description;
        this.triggeredTimestamp = triggeredTimestamp;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public void setRuleCode(String ruleCode) {
        this.ruleCode = ruleCode;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public RiskPriority getSeverity() {
        return severity;
    }

    public void setSeverity(RiskPriority severity) {
        this.severity = severity;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getTriggeredTimestamp() {
        return triggeredTimestamp;
    }

    public void setTriggeredTimestamp(LocalDateTime triggeredTimestamp) {
        this.triggeredTimestamp = triggeredTimestamp;
    }
}
