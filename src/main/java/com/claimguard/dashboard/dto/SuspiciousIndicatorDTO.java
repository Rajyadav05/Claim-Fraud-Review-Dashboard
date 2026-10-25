package com.claimguard.dashboard.dto;

import com.claimguard.dashboard.entity.RiskPriority;
import com.claimguard.dashboard.entity.SuspiciousIndicator;

import java.time.LocalDateTime;

public class SuspiciousIndicatorDTO {

    private Long id;
    private String ruleCode;
    private String title;
    private RiskPriority severity;
    private String description;
    private LocalDateTime triggeredTimestamp;

    public SuspiciousIndicatorDTO() {
    }

    public SuspiciousIndicatorDTO(Long id, String ruleCode, String title, RiskPriority severity, String description, LocalDateTime triggeredTimestamp) {
        this.id = id;
        this.ruleCode = ruleCode;
        this.title = title;
        this.severity = severity;
        this.description = description;
        this.triggeredTimestamp = triggeredTimestamp;
    }

    public static SuspiciousIndicatorDTO fromEntity(SuspiciousIndicator entity) {
        if (entity == null) {
            return null;
        }
        return new SuspiciousIndicatorDTO(
                entity.getId(),
                entity.getRuleCode(),
                entity.getTitle(),
                entity.getSeverity(),
                entity.getDescription(),
                entity.getTriggeredTimestamp()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
