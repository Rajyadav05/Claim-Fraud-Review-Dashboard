package com.claimguard.dashboard.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ReconciliationPayloadDTO {

    @NotBlank(message = "Schema version is required.")
    private String schemaVersion;

    private LocalDateTime exportTimestamp;

    @NotNull(message = "Claims collection is required.")
    @Valid
    private List<ReconciliationClaimItemDTO> claims = new ArrayList<>();

    public ReconciliationPayloadDTO() {
    }

    public ReconciliationPayloadDTO(String schemaVersion, LocalDateTime exportTimestamp, List<ReconciliationClaimItemDTO> claims) {
        this.schemaVersion = schemaVersion;
        this.exportTimestamp = exportTimestamp;
        this.claims = claims != null ? claims : new ArrayList<>();
    }

    public String getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(String schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public LocalDateTime getExportTimestamp() {
        return exportTimestamp;
    }

    public void setExportTimestamp(LocalDateTime exportTimestamp) {
        this.exportTimestamp = exportTimestamp;
    }

    public List<ReconciliationClaimItemDTO> getClaims() {
        return claims;
    }

    public void setClaims(List<ReconciliationClaimItemDTO> claims) {
        this.claims = claims != null ? claims : new ArrayList<>();
    }
}
