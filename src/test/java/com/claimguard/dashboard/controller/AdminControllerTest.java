package com.claimguard.dashboard.controller;

import com.claimguard.dashboard.dto.ReconciliationClaimItemDTO;
import com.claimguard.dashboard.dto.ReconciliationPayloadDTO;
import com.claimguard.dashboard.dto.ReconciliationResultDTO;
import com.claimguard.dashboard.entity.ClaimStatus;
import com.claimguard.dashboard.entity.ClaimType;
import com.claimguard.dashboard.entity.RiskPriority;
import com.claimguard.dashboard.exception.GlobalExceptionHandler;
import com.claimguard.dashboard.exception.ReconciliationConflictException;
import com.claimguard.dashboard.repository.UserRepository;
import com.claimguard.dashboard.security.CustomUserDetailsService;
import com.claimguard.dashboard.security.SecurityConfig;
import com.claimguard.dashboard.service.ClaimReconciliationService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ClaimReconciliationService reconciliationService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @Test
    @DisplayName("Anonymous unauthenticated request to /api/admin/reconcile is rejected with 401 Unauthorized")
    void shouldRejectAnonymousRequest() throws Exception {
        ReconciliationPayloadDTO payload = createSamplePayload();

        mockMvc.perform(post("/api/admin/reconcile")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Reviewer role (REVIEWER) request to /api/admin/reconcile is denied with 403 Forbidden")
    @WithMockUser(username = "priya.sharma", roles = "REVIEWER")
    void shouldDenyReviewerAccess() throws Exception {
        ReconciliationPayloadDTO payload = createSamplePayload();

        mockMvc.perform(post("/api/admin/reconcile")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin role (ADMIN) request without CSRF is rejected with 403 Forbidden")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void shouldRejectAdminWithoutCsrf() throws Exception {
        ReconciliationPayloadDTO payload = createSamplePayload();

        mockMvc.perform(post("/api/admin/reconcile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin role (ADMIN) request with CSRF succeeds with 200 OK")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void shouldAllowAdminWithCsrf() throws Exception {
        ReconciliationPayloadDTO payload = createSamplePayload();
        ReconciliationResultDTO mockResult = new ReconciliationResultDTO(
                1, 1, 0, 1, 0, 0, "SUCCESS", "Reconciliation successfully executed.", List.of("CLM-2026-0006")
        );

        when(reconciliationService.reconcile(any(ReconciliationPayloadDTO.class))).thenReturn(mockResult);

        mockMvc.perform(post("/api/admin/reconcile")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.claimsInserted").value(1))
                .andExpect(jsonPath("$.totalReceived").value(1));
    }

    @Test
    @DisplayName("Malformed request payload is rejected with 400 Bad Request")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void shouldRejectMalformedPayload() throws Exception {
        // Missing schemaVersion and empty claims list violates @NotBlank and @Valid constraints
        ReconciliationPayloadDTO invalidPayload = new ReconciliationPayloadDTO("", LocalDateTime.now(), Collections.emptyList());

        mockMvc.perform(post("/api/admin/reconcile")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidPayload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Failed"));
    }

    @Test
    @DisplayName("Reconciliation conflict returns 409 Conflict")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void shouldReturnConflictStatus() throws Exception {
        ReconciliationPayloadDTO payload = createSamplePayload();
        when(reconciliationService.reconcile(any(ReconciliationPayloadDTO.class)))
                .thenThrow(new ReconciliationConflictException("Conflicting core claim attributes for reference 'CLM-2026-0001'."));

        mockMvc.perform(post("/api/admin/reconcile")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Reconciliation Conflict"))
                .andExpect(jsonPath("$.detail").value("Conflicting core claim attributes for reference 'CLM-2026-0001'."));
    }

    private ReconciliationPayloadDTO createSamplePayload() {
        ReconciliationClaimItemDTO item = new ReconciliationClaimItemDTO();
        item.setClaimReference("CLM-2026-0006");
        item.setPolicyNumber("POL-REC-001");
        item.setClaimantName("Test Person");
        item.setClaimType(ClaimType.MOTOR);
        item.setIncidentDate(LocalDateTime.now().minusDays(2));
        item.setSubmissionDate(LocalDateTime.now().minusDays(1));
        item.setClaimedAmount(new BigDecimal("15000.00"));
        item.setStatus(ClaimStatus.SUBMITTED);
        item.setRiskPriority(RiskPriority.MEDIUM);

        return new ReconciliationPayloadDTO("1.0", LocalDateTime.now(), List.of(item));
    }
}
