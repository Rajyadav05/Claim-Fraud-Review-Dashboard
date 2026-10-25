package com.claimguard.dashboard.security;

import com.claimguard.dashboard.entity.User;
import com.claimguard.dashboard.entity.UserRole;
import com.claimguard.dashboard.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "claimguard.admin.username=testadmin",
        "claimguard.admin.password=AdminPass123!",
        "claimguard.reviewer.username=testreviewer",
        "claimguard.reviewer.password=ReviewerPass123!",
        "CLAIMGUARD_DB_PATH=./data/test_sec_db"
})
@AutoConfigureMockMvc
class SecurityAuthenticationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("Verify passwords in database are hashed with BCrypt and not plaintext")
    void testPasswordsAreHashedWithBCrypt() {
        Optional<User> adminOpt = userRepository.findByUsername("testadmin");
        assertTrue(adminOpt.isPresent(), "Bootstrapped testadmin should exist");
        User admin = adminOpt.get();

        assertNotEquals("AdminPass123!", admin.getPasswordHash(), "Plaintext password must never be stored");
        assertTrue(admin.getPasswordHash().startsWith("$2a$") || admin.getPasswordHash().startsWith("$2b$"),
                "Password must be hashed with BCrypt");
        assertTrue(passwordEncoder.matches("AdminPass123!", admin.getPasswordHash()),
                "BCrypt encoder should match raw password against stored hash");
    }

    @Test
    @DisplayName("Verify successful login with valid credentials returns 200 and session")
    void testSuccessfulLogin() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", "testreviewer")
                        .param("password", "ReviewerPass123!"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("authenticated"))
                .andExpect(jsonPath("$.username").value("testreviewer"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        assertNotNull(session, "Session must be established on successful login");

        // Verify authenticated session can access /api/auth/me
        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.username").value("testreviewer"))
                .andExpect(jsonPath("$.role").value("REVIEWER"));
    }

    @Test
    @DisplayName("Verify login failure with invalid password returns 401 Unauthorized")
    void testFailedLoginWithBadCredentials() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", "testreviewer")
                        .param("password", "WrongPassword!"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.title").value("Authentication Failed"))
                .andExpect(jsonPath("$.detail").value("Invalid username or password."));
    }

    @Test
    @DisplayName("Verify anonymous request to protected API returns 401 Unauthorized JSON")
    void testAnonymousAccessToApiReturns401() throws Exception {
        mockMvc.perform(get("/api/claims"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.title").value("Unauthorized"));
    }

    @Test
    @DisplayName("Verify anonymous request to protected HTML page redirects to /login.html")
    void testAnonymousAccessToHtmlPageRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/index.html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("/login.html")));
    }

    @Test
    @DisplayName("Verify CSRF endpoint returns token")
    void testCsrfTokenEndpoint() throws Exception {
        mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.headerName").isNotEmpty());
    }

    @Test
    @DisplayName("Verify state-changing POST without CSRF token is rejected with 403 Forbidden")
    @WithMockUser(username = "testreviewer", roles = {"REVIEWER"})
    void testMutatingRequestWithoutCsrfIsForbidden() throws Exception {
        mockMvc.perform(post("/api/claims")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"policyNumber\":\"POL-1\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Verify disabled user cannot authenticate")
    void testDisabledUserCannotLogin() throws Exception {
        String testUser = "disabled_" + System.currentTimeMillis();
        User disabledUser = new User();
        disabledUser.setUsername(testUser);
        disabledUser.setPasswordHash(passwordEncoder.encode("DisabledPass123!"));
        disabledUser.setRole(UserRole.REVIEWER);
        disabledUser.setEnabled(false);
        userRepository.save(disabledUser);

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", testUser)
                        .param("password", "DisabledPass123!"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("Verify logout invalidates session")
    void testLogoutInvalidatesSession() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", "testreviewer")
                        .param("password", "ReviewerPass123!"))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertNotNull(session);

        mockMvc.perform(post("/api/auth/logout")
                        .with(csrf())
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("logged_out"));

        assertTrue(session.isInvalid());
    }

    @Test
    @DisplayName("Verify end-to-end login with real CSRF token obtained from /api/auth/csrf succeeds")
    void testEndToEndLoginWithRealCsrfToken() throws Exception {
        MvcResult csrfResult = mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn();

        String csrfContent = csrfResult.getResponse().getContentAsString();
        jakarta.servlet.http.Cookie xsrfCookie = csrfResult.getResponse().getCookie("XSRF-TOKEN");
        MockHttpSession session = (MockHttpSession) csrfResult.getRequest().getSession(false);

        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        com.fasterxml.jackson.databind.JsonNode csrfJson = mapper.readTree(csrfContent);
        String token = csrfJson.get("token").asText();
        String headerName = csrfJson.get("headerName").asText();

        var requestBuilder = post("/api/auth/login")
                .header(headerName, token)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("username", "testreviewer")
                .param("password", "ReviewerPass123!");

        if (xsrfCookie != null) {
            requestBuilder.cookie(xsrfCookie);
        }
        if (session != null) {
            requestBuilder.session(session);
        }

        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("authenticated"))
                .andExpect(jsonPath("$.username").value("testreviewer"))
                .andExpect(jsonPath("$.authorities[0]").value("ROLE_REVIEWER"));
    }

    @Test
    @DisplayName("Verify login without CSRF token is rejected with 403 Forbidden and access denied message")
    void testLoginWithoutCsrfTokenIsForbidden() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", "testreviewer")
                        .param("password", "ReviewerPass123!"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.title").value("Forbidden"))
                .andExpect(jsonPath("$.detail").value("Access denied: insufficient permissions to perform this operation."));
    }

    @Test
    @DisplayName("Verify login with invalid CSRF token is rejected with 403 Forbidden")
    void testLoginWithInvalidCsrfTokenIsForbidden() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf().useInvalidToken())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", "testreviewer")
                        .param("password", "ReviewerPass123!"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.title").value("Forbidden"))
                .andExpect(jsonPath("$.detail").value("Access denied: insufficient permissions to perform this operation."));
    }

    @Test
    @DisplayName("Verify reviewer authorization: allowed on /api/claims, forbidden on /api/admin/**")
    void testReviewerAuthorizationBoundaries() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", "testreviewer")
                        .param("password", "ReviewerPass123!"))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertNotNull(session);

        // Reviewer is permitted to access claims
        mockMvc.perform(get("/api/claims").session(session))
                .andExpect(status().isOk());

        // Reviewer is denied access to admin endpoints
        mockMvc.perform(get("/api/admin/reconcile").session(session))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.title").value("Forbidden"))
                .andExpect(jsonPath("$.detail").value("Access denied: insufficient permissions to perform this operation."));
    }
}
