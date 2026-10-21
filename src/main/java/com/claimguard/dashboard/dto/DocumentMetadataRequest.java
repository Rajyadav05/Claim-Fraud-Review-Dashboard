package com.claimguard.dashboard.dto;

import jakarta.validation.constraints.NotBlank;

public class DocumentMetadataRequest {

    @NotBlank(message = "File name is required.")
    private String fileName;

    @NotBlank(message = "Document type is required.")
    private String documentType;

    private Long fileSizeBytes;

    private String sha256Hash;

    public DocumentMetadataRequest() {
    }

    public DocumentMetadataRequest(String fileName, String documentType, Long fileSizeBytes, String sha256Hash) {
        this.fileName = fileName;
        this.documentType = documentType;
        this.fileSizeBytes = fileSizeBytes;
        this.sha256Hash = sha256Hash;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public Long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public void setFileSizeBytes(Long fileSizeBytes) {
        this.fileSizeBytes = fileSizeBytes;
    }

    public String getSha256Hash() {
        return sha256Hash;
    }

    public void setSha256Hash(String sha256Hash) {
        this.sha256Hash = sha256Hash;
    }
}
