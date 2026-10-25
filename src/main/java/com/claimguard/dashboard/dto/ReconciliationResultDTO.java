package com.claimguard.dashboard.dto;

import java.util.ArrayList;
import java.util.List;

public class ReconciliationResultDTO {

    private int totalReceived;
    private int claimsInserted;
    private int claimsSkipped;
    private int historyRecordsInserted;
    private int indicatorsInserted;
    private int documentsInserted;
    private String status;
    private String message;
    private List<String> reconciledReferences = new ArrayList<>();

    public ReconciliationResultDTO() {
    }

    public ReconciliationResultDTO(int totalReceived, int claimsInserted, int claimsSkipped, int historyRecordsInserted, int indicatorsInserted, int documentsInserted, String status, String message, List<String> reconciledReferences) {
        this.totalReceived = totalReceived;
        this.claimsInserted = claimsInserted;
        this.claimsSkipped = claimsSkipped;
        this.historyRecordsInserted = historyRecordsInserted;
        this.indicatorsInserted = indicatorsInserted;
        this.documentsInserted = documentsInserted;
        this.status = status;
        this.message = message;
        this.reconciledReferences = reconciledReferences != null ? reconciledReferences : new ArrayList<>();
    }

    public int getTotalReceived() {
        return totalReceived;
    }

    public void setTotalReceived(int totalReceived) {
        this.totalReceived = totalReceived;
    }

    public int getClaimsInserted() {
        return claimsInserted;
    }

    public void setClaimsInserted(int claimsInserted) {
        this.claimsInserted = claimsInserted;
    }

    public int getClaimsSkipped() {
        return claimsSkipped;
    }

    public void setClaimsSkipped(int claimsSkipped) {
        this.claimsSkipped = claimsSkipped;
    }

    public int getHistoryRecordsInserted() {
        return historyRecordsInserted;
    }

    public void setHistoryRecordsInserted(int historyRecordsInserted) {
        this.historyRecordsInserted = historyRecordsInserted;
    }

    public int getIndicatorsInserted() {
        return indicatorsInserted;
    }

    public void setIndicatorsInserted(int indicatorsInserted) {
        this.indicatorsInserted = indicatorsInserted;
    }

    public int getDocumentsInserted() {
        return documentsInserted;
    }

    public void setDocumentsInserted(int documentsInserted) {
        this.documentsInserted = documentsInserted;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<String> getReconciledReferences() {
        return reconciledReferences;
    }

    public void setReconciledReferences(List<String> reconciledReferences) {
        this.reconciledReferences = reconciledReferences != null ? reconciledReferences : new ArrayList<>();
    }
}
