package com.claimguard.dashboard.selenium;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * End-to-End Selenium WebDriver test suite for ClaimGuard web application.
 * <p>
 * Tests user journeys against live static web pages and backend REST APIs.
 * Base URL defaults to http://localhost:8080 and can be overridden via
 * JVM system property -Dclaimguard.base-url=http://your-host:port.
 */
@Tag("selenium")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ClaimGuardSeleniumE2ETest {

    private static final String BASE_URL = System.getProperty(
            "claimguard.base-url",
            System.getProperty("baseUrl", "http://localhost:8080")
    );

    private static final boolean HEADLESS = Boolean.parseBoolean(
            System.getProperty("selenium.headless", "true")
    );

    private WebDriver driver;
    private WebDriverWait wait;

    @BeforeAll
    static void verifyServerAvailability() {
        boolean serverRunning = isServerListening(BASE_URL);
        assumeTrue(serverRunning,
                "Selenium test execution skipped: ClaimGuard application is not responding at " + BASE_URL +
                ". Start the server (e.g. java -jar target/claimguard-dashboard-1.0.0-SNAPSHOT.jar) to run browser tests."
        );
    }

    private static boolean isServerListening(String baseUrlStr) {
        try {
            URL url = URI.create(baseUrlStr + "/api/health").toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(2000);
            int code = conn.getResponseCode();
            return code == 200;
        } catch (IOException e) {
            return false;
        }
    }

    @BeforeEach
    void setUp() {
        ChromeOptions options = new ChromeOptions();
        if (HEADLESS) {
            options.addArguments("--headless=new");
        }
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");

        driver = new ChromeDriver(options);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(3));
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    @AfterEach
    void tearDown() {
        if (driver != null) {
            try {
                driver.quit();
            } catch (Exception ignored) {
            }
        }
    }

    @Test
    @Order(1)
    @DisplayName("Test 1: Executive Dashboard loads and displays brand, metrics, and navigation")
    void testDashboardLoadsSuccessfully() {
        driver.get(BASE_URL + "/index.html");

        wait.until(ExpectedConditions.titleContains("Operations Executive Dashboard"));
        assertTrue(driver.getTitle().contains("Operations Executive Dashboard"),
                "Page title should contain executive dashboard");

        // Verify brand title and dashboard navigation link in sidebar
        WebElement brandHeading = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//span[text()='ClaimGuard']")
        ));
        assertNotNull(brandHeading, "ClaimGuard branding header should be present");

        WebElement dashboardNavLink = driver.findElement(By.xpath("//nav//a[contains(@href, 'index.html')]"));
        assertNotNull(dashboardNavLink, "Link to dashboard page should exist in navigation");

        WebElement claimsLink = driver.findElement(By.xpath("//a[contains(@href, 'claims.html')]"));
        assertNotNull(claimsLink, "Link to claims page should exist on dashboard");
    }

    @Test
    @Order(2)
    @DisplayName("Test 2: All Claims directory loads and displays claims table and search input")
    void testClaimsDirectoryLoadsSuccessfully() {
        driver.get(BASE_URL + "/claims.html");

        wait.until(ExpectedConditions.titleContains("All Claims Directory"));
        assertTrue(driver.getTitle().contains("All Claims Directory"),
                "Title should indicate All Claims Directory");

        // Verify claims table is rendered
        WebElement table = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.tagName("table")
        ));
        assertNotNull(table, "Claims table should be present");

        // Verify search input by its verified id
        WebElement searchInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.id("claims-search-input")
        ));
        assertNotNull(searchInput, "Claims search input (#claims-search-input) should be present");
    }

    @Test
    @Order(3)
    @DisplayName("Test 3: Submit Claim page loads and renders the intake form with required inputs")
    void testSubmitPageFormPresence() {
        driver.get(BASE_URL + "/submit.html");

        wait.until(ExpectedConditions.titleContains("ClaimGuard"));

        WebElement form = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.id("claim-submission-form")
        ));
        assertNotNull(form, "Intake form #claim-submission-form should be present");

        WebElement claimantName = driver.findElement(By.id("claimantName"));
        WebElement policyNum = driver.findElement(By.id("policyNum"));
        assertNotNull(claimantName, "claimantName input should exist");
        assertNotNull(policyNum, "policyNum input should exist");
    }

    @Test
    @Order(4)
    @DisplayName("Test 4: Submit Claim form enforces HTML5 mandatory field requirements")
    void testSubmitFormValidationRequirements() {
        driver.get(BASE_URL + "/submit.html");

        WebElement claimantName = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("claimantName")));
        WebElement policyNum = driver.findElement(By.id("policyNum"));

        assertNotNull(claimantName.getAttribute("required"), "claimantName must have required attribute");
        assertNotNull(policyNum.getAttribute("required"), "policyNum must have required attribute");

        claimantName.clear();

        JavascriptExecutor js = (JavascriptExecutor) driver;
        Boolean isClaimantValid = (Boolean) js.executeScript("return arguments[0].checkValidity();", claimantName);
        assertFalse(isClaimantValid, "Empty claimantName should be invalid according to HTML5 checkValidity()");
    }

    @Test
    @Order(5)
    @DisplayName("Test 5: Navigation from Dashboard to Claims Queue functions via UI link")
    void testNavigationFromDashboardToClaims() {
        driver.get(BASE_URL + "/index.html");

        WebElement allClaimsLink = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//nav//a[contains(@href, 'claims.html')]")
        ));
        allClaimsLink.click();

        wait.until(ExpectedConditions.urlContains("claims.html"));
        assertTrue(driver.getCurrentUrl().contains("claims.html"),
                "Current URL should contain claims.html after clicking link");

        wait.until(ExpectedConditions.titleContains("All Claims Directory"));
        assertTrue(driver.getTitle().contains("All Claims Directory"),
                "Page title should be updated to All Claims Directory");
    }

    @Test
    @Order(6)
    @DisplayName("Test 6: Forensic Review page loads claim dossier with main container")
    void testReviewDossierPageLoads() {
        driver.get(BASE_URL + "/review.html?id=1");

        wait.until(ExpectedConditions.titleContains("Claim Details"));
        assertTrue(driver.getTitle().contains("Claim Details"),
                "Review page title should contain Claim Details");

        WebElement main = wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("main")));
        assertNotNull(main, "Review main container should be rendered");
    }
}
