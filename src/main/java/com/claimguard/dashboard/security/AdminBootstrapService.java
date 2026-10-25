package com.claimguard.dashboard.security;

import com.claimguard.dashboard.entity.User;
import com.claimguard.dashboard.entity.UserRole;
import com.claimguard.dashboard.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

@Component
@Order(1)
public class AdminBootstrapService implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(AdminBootstrapService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${claimguard.admin.username:${CLAIMGUARD_ADMIN_USERNAME:admin}}")
    private String adminUsername;

    @Value("${claimguard.admin.password:${CLAIMGUARD_ADMIN_PASSWORD:}}")
    private String adminPasswordConfig;

    @Value("${claimguard.reviewer.username:${CLAIMGUARD_REVIEWER_USERNAME:priya.sharma}}")
    private String reviewerUsername;

    @Value("${claimguard.reviewer.password:${CLAIMGUARD_REVIEWER_PASSWORD:}}")
    private String reviewerPasswordConfig;

    public AdminBootstrapService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        bootstrapAdminUser();
        bootstrapReviewerUser();
    }

    private void bootstrapAdminUser() {
        if (userRepository.existsByRole(UserRole.ADMIN)) {
            logger.info("[SECURITY BOOTSTRAP] Administrator account already exists in persistent database. Skipping bootstrap.");
            return;
        }

        String effectivePassword = adminPasswordConfig;
        boolean generated = false;
        if (effectivePassword == null || effectivePassword.isBlank()) {
            effectivePassword = generateSecureToken(16);
            generated = true;
        }

        String passwordHash = passwordEncoder.encode(effectivePassword);
        User adminUser = new User(adminUsername, passwordHash, "System Administrator", UserRole.ADMIN);
        userRepository.save(adminUser);

        if (generated) {
            logger.warn("==================================================================================");
            logger.warn("[SECURITY BOOTSTRAP] Initial Administrator account created:");
            logger.warn("  Username : {}", adminUsername);
            logger.warn("  Password : {}", effectivePassword);
            logger.warn("  Role     : ADMIN");
            logger.warn("NOTICE: Please save this one-time generated password or configure CLAIMGUARD_ADMIN_PASSWORD.");
            logger.warn("==================================================================================");
        } else {
            logger.info("[SECURITY BOOTSTRAP] Initial Administrator account '{}' successfully initialized via environment configuration.", adminUsername);
        }
    }

    private void bootstrapReviewerUser() {
        if (userRepository.existsByRole(UserRole.REVIEWER)) {
            logger.info("[SECURITY BOOTSTRAP] Reviewer account already exists in persistent database. Skipping bootstrap.");
            return;
        }

        String effectivePassword = reviewerPasswordConfig;
        boolean generated = false;
        if (effectivePassword == null || effectivePassword.isBlank()) {
            effectivePassword = generateSecureToken(16);
            generated = true;
        }

        String passwordHash = passwordEncoder.encode(effectivePassword);
        User reviewerUser = new User(reviewerUsername, passwordHash, "Priya Sharma", UserRole.REVIEWER);
        userRepository.save(reviewerUser);

        if (generated) {
            logger.warn("==================================================================================");
            logger.warn("[SECURITY BOOTSTRAP] Default Reviewer account created:");
            logger.warn("  Username : {}", reviewerUsername);
            logger.warn("  Password : {}", effectivePassword);
            logger.warn("  Role     : REVIEWER");
            logger.warn("NOTICE: Please save this one-time generated password or configure CLAIMGUARD_REVIEWER_PASSWORD.");
            logger.warn("==================================================================================");
        } else {
            logger.info("[SECURITY BOOTSTRAP] Default Reviewer account '{}' successfully initialized via environment configuration.", reviewerUsername);
        }
    }

    private String generateSecureToken(int byteLength) {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[byteLength];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
