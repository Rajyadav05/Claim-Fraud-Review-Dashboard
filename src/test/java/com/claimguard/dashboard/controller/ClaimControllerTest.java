package com.claimguard.dashboard.controller;

import com.claimguard.dashboard.dto.ClaimDetailDTO;
import com.claimguard.dashboard.entity.ClaimStatus;
import com.claimguard.dashboard.entity.ClaimType;
import com.claimguard.dashboard.entity.RiskPriority;
import com.claimguard.dashboard.service.ClaimService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {ClaimController.class, HealthController.class})
class ClaimControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ClaimService claimService;

    @Test
    @DisplayName("Health endpoint returns 200 OK and UP status")
    void shouldReturnHealthStatus() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.application").value("ClaimGuard Fraud Review Dashboard"));
    }

    @Test
    @DisplayName("GET /api/claims/{id} returns claim detail")
    void shouldReturnClaimDetail() throws Exception {
        ClaimDetailDTO dto = new ClaimDetailDTO();
        dto.setId(1L);
        dto.setClaimReference("CLM-2026-0001");
        dto.setClaimantName("Rajesh Kumar");
        dto.setPolicyNumber("POL-MH-9482-1049");
        dto.setClaimType(ClaimType.MOTOR);
        dto.setClaimedAmount(new BigDecimal("185000.00"));
        dto.setStatus(ClaimStatus.UNDER_REVIEW);
        dto.setRiskPriority(RiskPriority.HIGH);
        dto.setIncidentDate(LocalDateTime.now().minusDays(3));
        dto.setSubmissionDate(LocalDateTime.now().minusDays(1));

        when(claimService.getClaimById(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/claims/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.claimReference").value("CLM-2026-0001"))
                .andExpect(jsonPath("$.claimantName").value("Rajesh Kumar"))
                .andExpect(jsonPath("$.status").value("UNDER_REVIEW"))
                .andExpect(jsonPath("$.riskPriority").value("HIGH"));
    }
}
