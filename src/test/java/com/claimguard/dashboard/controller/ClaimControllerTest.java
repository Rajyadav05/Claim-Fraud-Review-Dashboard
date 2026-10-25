package com.claimguard.dashboard.controller;

import com.claimguard.dashboard.dto.ClaimDetailDTO;
import com.claimguard.dashboard.entity.ClaimStatus;
import com.claimguard.dashboard.entity.ClaimType;
import com.claimguard.dashboard.entity.RiskPriority;
import com.claimguard.dashboard.service.ClaimService;
import com.claimguard.dashboard.repository.UserRepository;
import com.claimguard.dashboard.security.CustomUserDetailsService;
import com.claimguard.dashboard.security.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.claimguard.dashboard.dto.ClaimSummaryDTO;
import com.claimguard.dashboard.dto.SuspiciousIndicatorDTO;
import com.claimguard.dashboard.exception.GlobalExceptionHandler;
import com.claimguard.dashboard.exception.ResourceNotFoundException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {ClaimController.class, HealthController.class})
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
class ClaimControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ClaimService claimService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @Test
    @DisplayName("Health endpoint returns 200 OK for anonymous unauthenticated requests")
    void shouldReturnHealthStatus() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.application").value("ClaimGuard Fraud Review Dashboard"));
    }

    @Test
    @DisplayName("Security: Protected claims endpoint returns 401 Unauthorized for anonymous requests")
    void shouldReturnUnauthorizedWhenAnonymous() throws Exception {
        mockMvc.perform(get("/api/claims/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.title").value("Unauthorized"));
    }

    @Test
    @WithMockUser(roles = "REVIEWER")
    @DisplayName("GET /api/claims/{id} returns claim detail for authenticated reviewer")
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

    @Test
    @WithMockUser(roles = "REVIEWER")
    @DisplayName("GET /api/claims/{id}/indicators returns 200 OK with indicators list")
    void shouldReturnClaimIndicatorsSuccessfully() throws Exception {
        SuspiciousIndicatorDTO ind = new SuspiciousIndicatorDTO(
                1L, "RUL-AMT-01", "High Value Claim Threshold", RiskPriority.MEDIUM,
                "Claim value ₹1,85,000 exceeds threshold", LocalDateTime.now()
        );

        when(claimService.getClaimIndicators(1L)).thenReturn(List.of(ind));

        mockMvc.perform(get("/api/claims/1/indicators")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ruleCode").value("RUL-AMT-01"))
                .andExpect(jsonPath("$[0].title").value("High Value Claim Threshold"))
                .andExpect(jsonPath("$[0].severity").value("MEDIUM"));
    }

    @Test
    @WithMockUser(roles = "REVIEWER")
    @DisplayName("GET /api/claims/{id}/indicators returns 404 NOT_FOUND when claim does not exist")
    void shouldReturnNotFoundWhenClaimIndicatorsMissing() throws Exception {
        when(claimService.getClaimIndicators(999L))
                .thenThrow(new ResourceNotFoundException("Claim with ID 999 not found."));

        mockMvc.perform(get("/api/claims/999/indicators")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.detail").value("Claim with ID 999 not found."));
    }

    @Test
    @WithMockUser(roles = "REVIEWER")
    @DisplayName("GET /api/claims returns Page with content and page.totalElements metadata")
    void shouldReturnClaimsPageWithPaginationMetadata() throws Exception {
        ClaimSummaryDTO summary = new ClaimSummaryDTO();
        summary.setId(1L);
        summary.setClaimReference("CLM-2026-0001");
        summary.setClaimantName("Rajesh Kumar");
        summary.setClaimedAmount(new BigDecimal("185000.00"));
        summary.setStatus(ClaimStatus.UNDER_REVIEW);
        summary.setRiskPriority(RiskPriority.HIGH);

        PageImpl<ClaimSummaryDTO> page = new PageImpl<>(List.of(summary), PageRequest.of(0, 10), 25L);
        when(claimService.getClaims(any(), any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/claims?page=0&size=10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].claimReference").value("CLM-2026-0001"))
                .andExpect(jsonPath("$.page.totalElements").value(25))
                .andExpect(jsonPath("$.page.size").value(10))
                .andExpect(jsonPath("$.page.totalPages").value(3));
    }

    @Test
    @WithMockUser(roles = "REVIEWER")
    @DisplayName("Regression: GET /api/claims returns indicatorCount and riskPriority for active heuristic triggers")
    void shouldReturnClaimsWithIndicatorCountsAndRiskPriority() throws Exception {
        ClaimSummaryDTO highRiskClaim = new ClaimSummaryDTO();
        highRiskClaim.setId(1L);
        highRiskClaim.setClaimReference("CLM-2026-0001");
        highRiskClaim.setClaimantName("Rajesh Kumar");
        highRiskClaim.setClaimedAmount(new BigDecimal("185000.00"));
        highRiskClaim.setStatus(ClaimStatus.UNDER_REVIEW);
        highRiskClaim.setRiskPriority(RiskPriority.HIGH);
        highRiskClaim.setIndicatorCount(3);

        ClaimSummaryDTO lowRiskClaim = new ClaimSummaryDTO();
        lowRiskClaim.setId(2L);
        lowRiskClaim.setClaimReference("CLM-2026-0002");
        lowRiskClaim.setClaimantName("Anita Desai");
        lowRiskClaim.setClaimedAmount(new BigDecimal("45000.00"));
        lowRiskClaim.setStatus(ClaimStatus.SUBMITTED);
        lowRiskClaim.setRiskPriority(RiskPriority.LOW);
        lowRiskClaim.setIndicatorCount(0);

        PageImpl<ClaimSummaryDTO> page = new PageImpl<>(List.of(highRiskClaim, lowRiskClaim), PageRequest.of(0, 10), 2L);
        when(claimService.getClaims(any(), any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/claims?page=0&size=10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].riskPriority").value("HIGH"))
                .andExpect(jsonPath("$.content[0].indicatorCount").value(3))
                .andExpect(jsonPath("$.content[1].riskPriority").value("LOW"))
                .andExpect(jsonPath("$.content[1].indicatorCount").value(0));
    }
}
