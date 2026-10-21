# Task 9 — Selenium Test Design and Local Execution

**Project:** ClaimGuard — Claim Fraud Review Dashboard
**Assignment Module:** Jenkins Deployment for a Claim Fraud Review Dashboard
**Author:** Raj Yadav
**Date:** 2026-10-22
**Branch:** `main`
**Testing Framework:** Selenium WebDriver 4.19.1 + JUnit 5 (Jupiter)
**Browser Runtime:** Google Chrome 156.0.8078.48 (64-bit) + ChromeDriver managed via Selenium Manager
**Execution Mode:** Headless Chrome (`--headless=new`, configurable via `-Dselenium.headless=false`)
**Target Server Base URL:** `http://localhost:8080` (configurable via `-Dclaimguard.base-url=...`)

---

## 1. Executive Summary & Objective

In Tasks 5–8, automated testing focused on unit logic, rule engine heuristics (`IndicatorRuleEngine`), domain services (`ClaimService`), and Spring MVC HTTP controllers (`ClaimControllerTest` via MockMvc) with 11 automated tests. While these tests prove backend correctness, they cannot validate the end-user browser experience, JavaScript dynamic DOM rendering, HTML5 client-side validation, or cross-page browser navigation.

The objective of **Task 9** is to design, implement, and execute an automated **End-to-End (E2E) Browser Testing Suite** using **Selenium WebDriver 4** and **JUnit 5**:
1. Validate the live ClaimGuard web console against a running Spring Boot instance.
2. Automate user journeys across the four core views: Executive Dashboard, All Claims Queue, Submit Claim Intake Portal, and Forensic Adjudication Dossier.
3. Verify client-side HTML5 form constraint enforcement and dynamic DOM updates.
4. Integrate Selenium Manager for automatic driver lifecycle management without manual driver binaries.
5. Provide a deterministic test execution harness with graceful conditional execution (`Assumptions.assumeTrue`) so that headless CI jobs without an active server continue to run cleanly.

---

## 2. Selenium 4 & JUnit 5 Architecture

### 2.1 Technology Stack & Maven Configuration

The Selenium 4 test suite is integrated into `pom.xml` under test scope, utilizing version management from `spring-boot-starter-parent` (version `3.3.4`):

```xml
<dependency>
    <groupId>org.seleniumhq.selenium</groupId>
    <artifactId>selenium-java</artifactId>
    <scope>test</scope>
</dependency>
```

- **Selenium Version:** `4.19.1` (managed by Spring Boot 3.3.4 dependency management).
- **Selenium Manager:** Built directly into Selenium 4.19+. It automatically inspects the locally installed Google Chrome browser version (`156.0.8078.48`) and provisions the matching ChromeDriver into the user cache (`C:\Users\User\.cache\selenium\chromedriver\...`) with zero manual path configuration.
- **JUnit 5 Integration:** Annotated with `@Tag("selenium")` and `@TestMethodOrder(MethodOrderer.OrderAnnotation.class)` to ensure deterministic execution ordering.

### 2.2 Test Suite Architecture (`ClaimGuardSeleniumE2ETest`)

Located at:
[`src/test/java/com/claimguard/dashboard/selenium/ClaimGuardSeleniumE2ETest.java`](../src/test/java/com/claimguard/dashboard/selenium/ClaimGuardSeleniumE2ETest.java)

#### Key Architectural Highlights:
1. **Configurable Endpoints:**
   ```java
   private static final String BASE_URL = System.getProperty(
       "claimguard.base-url",
       System.getProperty("baseUrl", "http://localhost:8080")
   );
   ```
2. **Headless & Display Modes:**
   Defaults to `--headless=new` for headless CI environments, but can be switched to headed interactive mode via `-Dselenium.headless=false`.
3. **Fail-Safe Server Probe (`@BeforeAll`):**
   ```java
   @BeforeAll
   static void verifyServerAvailability() {
       boolean serverRunning = isServerListening(BASE_URL);
       assumeTrue(serverRunning, "Selenium skipped: server not listening at " + BASE_URL);
   }
   ```
   If the Spring Boot server is not running on port 8080, JUnit 5 aborts/skips the browser tests rather than failing standard compilation or unit test runs. When the application is active, all tests execute against the live web server.
4. **Reliable Driver Teardown (`@AfterEach`):**
   Ensures that browser sessions and OS processes (`chrome.exe`, `chromedriver.exe`) are terminated after each test.

---

## 3. Test Cases Specification

Six automated browser test cases were designed and verified against the actual application DOM:

| # | Test Method | Target Page | Real UI Elements Verified | Journey / Assertion |
|:---:|:---|:---|:---|:---|
| **1** | `testDashboardLoadsSuccessfully` | `http://localhost:8080/index.html` | Page title, `//span[text()='ClaimGuard']`, sidebar navigation links | Confirms Operations Executive Dashboard renders brand header, navigation menu, and link to claims directory. |
| **2** | `testClaimsDirectoryLoadsSuccessfully` | `http://localhost:8080/claims.html` | `<table>`, `input#claims-search-input` | Confirms All Claims Directory loads table container and real search filter input. |
| **3** | `testSubmitPageFormPresence` | `http://localhost:8080/submit.html` | `form#claim-submission-form`, `input#claimantName`, `input#policyNum` | Confirms Submit Claim Intake Portal renders form with required claimant and policy fields. |
| **4** | `testSubmitFormValidationRequirements` | `http://localhost:8080/submit.html` | `input#claimantName[required]`, JavaScript `checkValidity()` | Clears mandatory field and validates that browser constraint validation flags the input as invalid. |
| **5** | `testNavigationFromDashboardToClaims` | `index.html` → `claims.html` | Sidebar link `//nav//a[contains(@href, 'claims.html')]` | Clicks sidebar link, waits for URL transition, and confirms browser navigates to `/claims.html`. |
| **6** | `testReviewDossierPageLoads` | `http://localhost:8080/review.html?id=1` | `<main>` container, page title | Navigates to seeded claim `CLM-2026-0001` dossier and verifies forensic layout rendering. |

---

## 4. Local Execution Guide

### 4.1 Prerequisites

1. **Java Development Kit:** Adoptium OpenJDK 21 LTS (`JAVA_HOME` configured).
2. **Browser:** Google Chrome installed at standard Windows path (`C:\Program Files\Google\Chrome\Application\chrome.exe`).
3. **Application Server:** Spring Boot application running on `http://localhost:8080`.

### 4.2 Step-by-Step Execution Procedure

#### Step 1: Build Application Package
```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
.\mvnw.cmd clean package
```

#### Step 2: Start Application Server Locally
Start the packaged application in a dedicated terminal or background process:
```powershell
$javaExe = "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot\bin\java.exe"
Start-Process -FilePath $javaExe -ArgumentList "-jar", "target\claimguard-dashboard-1.0.0-SNAPSHOT.jar", "--server.port=8080"
```
Verify the server is operational:
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/health"
# Output: @{status=UP; application=ClaimGuard Dashboard; version=1.0.0-SNAPSHOT}
```

#### Step 3: Execute Selenium Test Suite
Run the dedicated Selenium suite using Maven:
```powershell
.\mvnw.cmd test -Dtest=ClaimGuardSeleniumE2ETest
```

To run in headed (visible browser) mode:
```powershell
.\mvnw.cmd test -Dtest=ClaimGuardSeleniumE2ETest -Dselenium.headless=false
```

To target a different server host or port:
```powershell
.\mvnw.cmd test -Dtest=ClaimGuardSeleniumE2ETest -Dclaimguard.base-url=http://127.0.0.1:9090
```

---

## 5. Actual Test Execution Evidence & Results

### 5.1 Real Maven Test Execution Output (2026-10-22)

```text
[INFO] Scanning for projects...
[INFO] ----------------< com.claimguard:claimguard-dashboard >-----------------
[INFO] Building claimguard-dashboard 1.0.0-SNAPSHOT
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] --- surefire:3.2.5:test (default-test) @ claimguard-dashboard ---
[INFO] Using auto detected provider org.apache.maven.surefire.junitplatform.JUnitPlatformProvider
[INFO] 
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.claimguard.dashboard.selenium.ClaimGuardSeleniumE2ETest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 18.58 s -- in com.claimguard.dashboard.selenium.ClaimGuardSeleniumE2ETest
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] Total time:  23.304 s
[INFO] Finished at: 2026-10-22T03:57:42+05:30
[INFO] ------------------------------------------------------------------------
```

### 5.2 Complete Project Test Portfolio (17 Total Tests)

| Category | Test Class | Executed Tests | Failures | Status |
|:---|:---|:---:|:---:|:---:|
| **Spring Context & Boot** | `ClaimGuardApplicationTests` | 1 | 0 | **PASS** |
| **REST Controller MockMvc** | `ClaimControllerTest` | 2 | 0 | **PASS** |
| **Heuristic Rule Engine** | `IndicatorRuleEngineTest` | 4 | 0 | **PASS** |
| **Claim Lifecycle Service** | `ClaimServiceTest` | 4 | 0 | **PASS** |
| **Selenium E2E Browser** | `ClaimGuardSeleniumE2ETest` | 6 | 0 | **PASS** |
| **Total Test Portfolio** | **All 5 Test Classes** | **17** | **0** | **100% PASS** |

---

## 6. Known Limitations & Design Decisions

1. **Ephemeral H2 Persistence:**
   Browser tests run against in-memory H2. Claims submitted during E2E testing exist in RAM for the process lifecycle and reset when the server restarts. Baseline synthetic claims (`CLM-2026-0001` through `CLM-2026-0005`) are re-seeded automatically.
2. **Headless Chrome in CI:**
   In virtualized or containerized CI runners, Chrome requires `--no-sandbox` and `--disable-dev-shm-usage` flags to prevent shared memory exhaustion. These flags are enabled by default in `ClaimGuardSeleniumE2ETest`.
3. **CDP Version Warning:**
   When Chrome automatically updates to bleeding-edge releases (e.g. Chrome 156), Selenium displays an informational warning regarding Chrome DevTools Protocol (`Unable to find CDP implementation matching 156`). Standard W3C WebDriver actions (navigation, clicks, element location) are completely unaffected.
4. **Interactive File Upload:**
   Physical document uploading is simulated at metadata level during MVP. Real OS file-picker automation is out of scope.

---

## 7. Screenshot & Evidence Checklist

| # | Evidence Item | Verification Location | Status |
|:---:|:---|:---|:---:|
| **1** | Maven Test Execution Console | Output showing `Tests run: 6, Failures: 0, Errors: 0, Skipped: 0` for `ClaimGuardSeleniumE2ETest` | Captured |
| **2** | Executive Dashboard Browser View | Chrome window rendering `http://localhost:8080/index.html` | Ready to Capture |
| **3** | Claims Directory Browser View | Chrome window rendering `http://localhost:8080/claims.html` with data loaded | Ready to Capture |
| **4** | Submit Claim Intake Form | Chrome window rendering `http://localhost:8080/submit.html` with form fields | Ready to Capture |
| **5** | Forensic Review Console | Chrome window rendering `http://localhost:8080/review.html?id=1` with indicators | Ready to Capture |

---

## 8. Academic Viva Questions & Detailed Answers

### Q1: Why use Selenium WebDriver in addition to JUnit and MockMvc tests?
**Answer:** MockMvc tests verify Spring MVC controller routing, request mapping, status codes, and serialization in an in-memory mock environment without spinning up a real HTTP server or browser engine. MockMvc cannot execute client-side JavaScript, evaluate CSS styling, test dynamic DOM manipulation (like Tailwind classes or async `fetch()` calls), or verify actual user browser navigation. Selenium WebDriver runs against a real browser engine (Google Chrome), validating end-to-end user journeys, cross-page routing, and real DOM behavior.

### Q2: What is Selenium Manager and how does it improve test maintenance?
**Answer:** In older versions of Selenium (3.x and early 4.x), developers had to manually download browser driver binaries (e.g., `chromedriver.exe`), match the exact browser version, and set system properties (e.g., `webdriver.chrome.driver`). When Chrome auto-updated, tests broke due to version mismatch. Selenium Manager (introduced natively in Selenium 4) automatically detects the installed browser version, downloads the compatible driver release, caches it locally, and configures the environment on the fly, eliminating driver management overhead.

### Q3: Why use Explicit Waits (`WebDriverWait`) instead of `Thread.sleep()`?
**Answer:** `Thread.sleep()` pauses thread execution unconditionally for a fixed duration, which either wastes time if the element appears quickly or causes flaky test failures if network latency exceeds the sleep interval. Explicit waits (`WebDriverWait` combined with `ExpectedConditions`) poll the DOM dynamically with a configurable frequency (typically 500ms) until a specific condition is met (e.g., `visibilityOfElementLocated`, `elementToBeClickable`) or the timeout expires. This makes tests significantly faster, reliable, and immune to timing jitter.

### Q4: How are Selenium tests kept non-disruptive in CI environments where the app may not be running?
**Answer:** In `ClaimGuardSeleniumE2ETest`, the `@BeforeAll` setup method invokes a lightweight HTTP probe to `BASE_URL + "/api/health"`. Using JUnit 5's `Assumptions.assumeTrue(...)`, if the target application server is not responding, JUnit marks the test class as skipped rather than failing the build. This ensures that standard unit test builds (such as preliminary Jenkins compile/test stages) do not fail due to a missing live server.

---

*ClaimGuard — Claim Fraud Review Dashboard | College DevOps Assignment | Task 9 Documentation*
