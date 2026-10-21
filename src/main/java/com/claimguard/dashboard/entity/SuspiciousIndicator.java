package com.claimguard.dashboard.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "suspicious_indicators")
public class SuspiciousIndicator {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "claim_id", nullable = false)
    @JsonBackReference
    private Claim claim;

    @Column(name = "rule_code", nullable = false, length = 30)
    private String ruleCode;

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private RiskPriority severity;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "triggered_timestamp", nullable = false)
    private LocalDateTime triggeredTimestamp;

    public SuspiciousIndicator() {
    }

    public SuspiciousIndicator(String ruleCode, String title, RiskPriority severity, String description, LocalDateTime triggeredTimestamp) {
        this.ruleCode = ruleCode;
        this.title = title;
        this.severity = severity;
        this.description = description;
        this.triggeredTimestamp = triggeredTimestamp;
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
