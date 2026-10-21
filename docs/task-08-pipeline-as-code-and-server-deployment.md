# Task 8 â€” Pipeline as Code and Server Deployment

**Project:** ClaimGuard â€” Claim Fraud Review Dashboard
**Assignment Module:** Jenkins Deployment for a Claim Fraud Review Dashboard
**Author:** Raj Yadav
**Date:** 2026-10-22
**Branch:** `main`
**Pipeline Definition:** Root `Jenkinsfile` (Declarative Pipeline)
**Jenkins Target Instance:** Jenkins 2.580.1 LTS (`http://localhost:8081`)
**JDK Path:** `C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot` (Java 21.0.12.1 LTS)
**Build Tool:** Maven Wrapper (`mvnw.cmd` / Maven 3.10.0)

---

## 1. Executive Summary & Purpose of Pipeline as Code

### 1.1 From Freestyle CI to Pipeline as Code

In Task 7, a Jenkins Freestyle project (`ClaimGuard-CI`) was configured through the Jenkins web GUI to compile the project, execute automated tests, and produce build artifacts. While Freestyle jobs are effective for initial validation, they carry significant operational limitations in modern DevOps workflows:

1. **Configuration Drift & Lack of Versioning:** Freestyle job configurations reside in XML files on the Jenkins controller filesystem. Changes made in the GUI are not version-controlled, lack peer review, and cannot be rolled back alongside source code modifications.
2. **Branching & Portability:** Freestyle configurations do not automatically track or adapt across multiple branches or forks without manual replication.
3. **Disaster Recovery & Reproducibility:** If the Jenkins server fails, rebuilding Freestyle jobs requires manual re-entry of build steps, environment variables, and post-build actions.

**Pipeline as Code** resolves these challenges by defining the entire build, test, packaging, artifact archiving, and deployment lifecycle in a versioned text file (`Jenkinsfile`) stored directly in the repository root.

### 1.2 Core Benefits for ClaimGuard

- **Single Source of Truth:** The pipeline definition evolves synchronously with the application code on the `main` branch.
- **Auditability & Traceability:** Every modification to the build and release workflow is committed to Git with commit history, author attribution, and diff review.
- **Deterministic Execution:** The pipeline explicitly enforces environment prerequisites (JDK 21 path, UTF-8 encoding, Maven Wrapper), preventing agent-dependent compilation failures.
- **Visual Stage Telemetry:** The Declarative Pipeline provides fine-grained visibility into each pipeline stage (Initialize â†’ Verify Environment â†’ Run Automated Tests â†’ Package Application â†’ Server Deployment Standby).

---

## 2. Root Jenkinsfile Architecture & Stage Descriptions

The root [`Jenkinsfile`](../Jenkinsfile) is written as a Declarative Pipeline optimized for the local Windows Jenkins controller and any Windows build agent.

### 2.1 Complete Declarative Structure

```groovy
pipeline {
    agent any

    environment {
        JAVA_HOME = 'C:\\Program Files\\Eclipse Adoptium\\jdk-21.0.12.101-hotspot'
        PATH = "${env.JAVA_HOME}\\bin;${env.PATH}"
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
        disableConcurrentBuilds()
        timeout(time: 15, unit: 'MINUTES')
    }

    stages {
        stage('Initialize') { ... }
        stage('Verify Environment') { ... }
        stage('Run Automated Tests') { ... }
        stage('Package Application') { ... }
        stage('Server Deployment Standby') { ... }
    }

    post {
        success { ... }
        failure { ... }
        always { ... }
    }
}
```

### 2.2 Detailed Stage Breakdown

| Stage | Name | Commands / Directives | Purpose & Failure Conditions |
|:---|:---|:---|:---|
| **0** | `Initialize` | `currentBuild.displayName`<br>`currentBuild.description` | Sets custom build title (`#<num> - ClaimGuard Main`) and description. Echoes workspace directory and job metadata. |
| **1** | `Verify Environment` | `java -version`<br>`javac -version` | Verifies JDK 21 runtime and compiler existence on the active PATH. Fails immediately if either command returns a non-zero exit code. |
| **2** | `Run Automated Tests` | `call mvnw.cmd clean test`<br>`junit testResults` | Cleans previous build state and executes all 11 JUnit 5 / MockMvc tests. Fails pipeline if any test fails or errors. Records XML test reports for trend analysis. |
| **3** | `Package Application` | `call mvnw.cmd package`<br>`archiveArtifacts` | Assembles the Spring Boot executable fat JAR. Checks that `target/claimguard-dashboard-1.0.0-SNAPSHOT.jar` exists; fails if missing. Archives the JAR as an immutable Jenkins artifact. |
| **4** | `Server Deployment Standby` | Diagnostic output | Explicit deployment gate. Informs operators that automated compilation, testing, and packaging are complete while live deployment is held pending server configuration in Tasks 10â€“12. |
| **Post** | `post` Actions | `success`<br>`failure`<br>`always` | Prints distinct banners for success or failure. In `always`, executes non-destructive safe workspace cleanup after artifacts have been stored. |

### 2.3 Post-Build Actions & Safe Workspace Cleanup

The pipeline employs a strict ordering strategy to guarantee artifact integrity:

1. **Artifact Archiving First:** `archiveArtifacts` is invoked within `stage('Package Application')`. This transfers `claimguard-dashboard-1.0.0-SNAPSHOT.jar` directly into the Jenkins controller's internal storage (`$JENKINS_HOME/jobs/ClaimGuard-Pipeline/builds/<build-num>/archive/`).
2. **Safe Workspace Cleanup Second:** Cleanup is deferred to `post { always { ... } }` and wrapped in a defensive `script { try { cleanWs(...) } catch (Throwable t) { ... } }` block. If the `ws-cleanup` plugin is installed, it safely resets the agent workspace without failing the build or affecting already-archived artifacts. If the plugin is unavailable, execution logs an informative message and completes normally.

---

## 3. Jenkins Pipeline Job Setup (`ClaimGuard-Pipeline`)

Follow these step-by-step instructions to create and configure the pipeline in Jenkins.

### 3.1 Prerequisites

- **Jenkins Controller:** Running locally at `http://localhost:8081` (Jenkins 2.580.1 LTS).
- **Installed Plugins:**
  - *Pipeline* (workflow-aggregator)
  - *Git Plugin*
  - *JUnit Plugin* (for test reporting)
  - *Workspace Cleanup Plugin* (recommended)
- **Local JDK 21:** Adoptium OpenJDK 21 installed at `C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot`.
- **Git Repository:** `https://github.com/Rajyadav05/Claim-Fraud-Review-Dashboard.git`

### 3.2 Step-by-Step Job Creation

1. Open a browser and navigate to `http://localhost:8081`.
2. From the Jenkins dashboard, click **New Item** (left menu) or navigate directly to `http://localhost:8081/newJob`.
3. In the **Enter an item name** field, enter:
   ```text
   ClaimGuard-Pipeline
   ```
4. Select the **Pipeline** project type (do NOT select Freestyle).
5. Click **OK** at the bottom of the page.
6. On the job configuration screen:
   - **General:**
     - Check **Discard old builds** â†’ Set *Max # of builds to keep* to `10`.
     - Description: `Declarative CI/CD Pipeline for ClaimGuard Claim Fraud Review Dashboard`.
   - **Build Triggers:**
     - Leave default (manual trigger via *Build Now*, or check *GitHub hook trigger for GITScm polling* if webhook is configured).
   - **Pipeline Section:**
     - **Definition:** Select `Pipeline script from SCM`.
     - **SCM:** Select `Git`.
     - **Repository URL:**
       ```text
       https://github.com/Rajyadav05/Claim-Fraud-Review-Dashboard.git
       ```
     - **Credentials:** `- none -` (public GitHub repository).
     - **Branches to build:**
       ```text
       */main
       ```
     - **Script Path:**
       ```text
       Jenkinsfile
       ```
     - **Lightweight checkout:** Keep checked.
7. Click **Save**.

---

## 4. Pipeline Execution & Verification Evidence

### 4.1 Running the Pipeline

1. On the `ClaimGuard-Pipeline` project page (`http://localhost:8081/job/ClaimGuard-Pipeline/`), click **Build Now** in the left sidebar.
2. In the **Build History** widget, click on `#1` (or current build number).
3. Click **Console Output** to observe live step execution.

### 4.2 Expected Console Output Evidence

#### A. Environment Verification Output
```text
[STAGE: Verify Environment]
Configured JAVA_HOME: C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot
Checking Java runtime version:
openjdk version "21.0.12.1" 2026-08-18 LTS
OpenJDK Runtime Environment Temurin-21.0.12+10 (build 21.0.12.1+10-LTS)
OpenJDK 64-Bit Server VM Temurin-21.0.12+10 (build 21.0.12.1+10-LTS, mixed mode, sharing)
Checking Java compiler version:
javac 21.0.12.1
[SUCCESS] Java 21 environment verified.
```

#### B. Automated Test Output
```text
[STAGE: Run Automated Tests]
[INFO] Scanning for projects...
[INFO] Building claimguard-dashboard 1.0.0-SNAPSHOT
[INFO] --- clean:3.3.2:clean (default-clean) @ claimguard-dashboard ---
[INFO] --- compiler:3.13.0:compile (default-compile) @ claimguard-dashboard ---
[INFO] Compiling 28 source files with javac [debug parameters release 21] to target\classes
[INFO] --- compiler:3.13.0:testCompile (default-testCompile) @ claimguard-dashboard ---
[INFO] Compiling 4 source files with javac [debug parameters release 21] to target\test-classes
[INFO] --- surefire:3.2.5:test (default-test) @ claimguard-dashboard ---
[INFO] Running com.claimguard.dashboard.ClaimGuardApplicationTests
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.claimguard.dashboard.controller.ClaimControllerTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.claimguard.dashboard.rules.IndicatorRuleEngineTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.claimguard.dashboard.service.ClaimServiceTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
[INFO] Results:
[INFO] Tests run: 11, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
Recording test results
```

#### C. Packaging and Artifact Archival Output
```text
[STAGE: Package Application]
[INFO] Building jar: E:\...\target\claimguard-dashboard-1.0.0-SNAPSHOT.jar
[INFO] Replacing main artifact with repackaged archive, adding nested dependencies in BOOT-INF/.
[INFO] BUILD SUCCESS
[INFO] Verifying existence of packaged artifact...
[SUCCESS] Package artifact verified: target\claimguard-dashboard-1.0.0-SNAPSHOT.jar
Archiving artifacts
â€˜target/claimguard-dashboard-1.0.0-SNAPSHOT.jarâ€™ fingerprinted
```

#### D. Pipeline Completion Output
```text
[PIPELINE SUCCESS] ClaimGuard Pipeline completed successfully!
[PIPELINE SUCCESS] Archived artifact: target/claimguard-dashboard-1.0.0-SNAPSHOT.jar
Finished: SUCCESS
```

### 4.3 Retrieving the Archived JAR

1. Navigate to the build summary page:
   ```text
   http://localhost:8081/job/ClaimGuard-Pipeline/lastSuccessfulBuild/
   ```
2. In the **Build Artifacts** section, locate:
   ```text
   claimguard-dashboard-1.0.0-SNAPSHOT.jar
   ```
3. Click the filename to download the standalone executable JAR directly to your workstation.
4. Verify the JAR size (approximately 51.5 MB) and SHA-256 fingerprint displayed on the artifact page.

---

## 5. Deployment Architecture & Safe Staging Procedure

### 5.1 Deployment Safety Principles

In accordance with strict production engineering and academic DevOps requirements:
- **No Blind Process Termination:** The pipeline does not execute arbitrary `taskkill` or `kill -9` commands that could terminate unrelated processes or corrupt data.
- **No In-Place Overwrite of Running Binaries:** Windows locks open `.jar` files during execution; blindly copying over a running file causes an `Access Denied` exception.
- **No Premature Remote Deployments:** Without verified SSH keys, target IP addresses, firewall openings, and secret managers, automated remote deployment commands will fail or create security vulnerabilities.
- **H2 Ephemeral Data Awareness:** The embedded H2 database (`jdbc:h2:mem:claimguard`) resides in RAM. Terminating the process discards all transient claims. Any deployment restart must be deliberate and planned.

### 5.2 Controlled Local Windows Staging Procedure

For local deployment and evaluation on Windows, follow this verified, non-destructive staging procedure:

#### Step 1: Establish Dedicated Staging Directory
Create an isolated runtime directory outside the Jenkins workspace:
```powershell
New-Item -ItemType Directory -Path "E:\ClaimGuard-Staging" -Force
```

#### Step 2: Copy the Verified Archived JAR
Copy the build artifact from Jenkins or the target directory:
```powershell
Copy-Item -Path "target\claimguard-dashboard-1.0.0-SNAPSHOT.jar" -Destination "E:\ClaimGuard-Staging\claimguard-dashboard.jar" -Force
```

#### Step 3: Check for Existing Process on Port 8080
Before launching, confirm port 8080 is available:
```powershell
Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue | Select-Object LocalAddress, LocalPort, OwningProcess, State
```
If an older instance is running and needs to be replaced, stop it gracefully:
```powershell
Stop-Process -Id <OwningProcessId> -PassThru
```

#### Step 4: Launch Application as a Background Service
Launch using the verified Adoptium JDK 21:
```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

Start-Process -FilePath "java.exe" `
  -ArgumentList "-jar", "E:\ClaimGuard-Staging\claimguard-dashboard.jar", "--server.port=8080" `
  -WorkingDirectory "E:\ClaimGuard-Staging" `
  -RedirectStandardOutput "E:\ClaimGuard-Staging\app.log" `
  -RedirectStandardError "E:\ClaimGuard-Staging\error.log" `
  -WindowStyle Hidden
```

#### Step 5: Verify Deployment via Health Endpoints
Verify that the service is operational:
```powershell
# Custom Health Probe
Invoke-RestMethod -Uri "http://localhost:8080/api/health"

# Spring Boot Actuator Probe
Invoke-RestMethod -Uri "http://localhost:8080/actuator/health"
```
Expected response:
```json
{"status":"UP","application":"ClaimGuard Dashboard","version":"1.0.0-SNAPSHOT"}
```

#### Step 6: Test Frontend Web Console
Open `http://localhost:8080/` in the browser to verify dashboard rendering and API communication.

### 5.3 Requirements for Future Remote Server Deployment (Tasks 10â€“12)

To transition from local staging to a production-grade remote server deployment in subsequent tasks, the following infrastructure details are required:

| Infrastructure Area | Requirement | Planned Phase |
|:---|:---|:---:|
| **Containerization** | Multi-stage `Dockerfile` with minimal JRE base image (Eclipse Temurin 21 JRE Alpine) | Task 10 |
| **Container Registry** | Secure Docker Hub / GHCR image repository for versioned image tags | Task 10â€“11 |
| **Pipeline Integration** | Automated Docker build, image scan, and container push stages in `Jenkinsfile` | Task 11 |
| **Target Host Provisioning** | Target host IP / hostname, non-root service account, and SSH keys stored in Jenkins Credentials | Task 12 |
| **Configuration Management** | Ansible playbooks for Docker daemon installation, network setup, and service orchestration | Task 12 |
| **External Persistence** | Transition from in-memory H2 to containerized PostgreSQL with persistent volume mounts | Task 12 |
| **Reverse Proxy & SSL** | Nginx reverse proxy with TLS termination on port 443 | Task 13 |

---

## 6. Screenshots & Evaluation Evidence Checklist

Capture the following screenshots to substantiate Task 8 completion for grading and evaluation:

| # | Evidence Item | Description / UI Location | Status |
|:---:|:---|:---|:---:|
| **1** | **Pipeline Configuration Screen** | `http://localhost:8081/job/ClaimGuard-Pipeline/configure` showing Definition = *Pipeline script from SCM*, Git URL, branch `*/main`, script path `Jenkinsfile`. | Ready to Capture |
| **2** | **Stage View Dashboard** | `http://localhost:8081/job/ClaimGuard-Pipeline/` displaying green stage boxes for *Initialize*, *Verify Environment*, *Run Automated Tests*, *Package Application*, and *Server Deployment Standby*. | Ready to Capture |
| **3** | **Console Output â€” Environment Check** | Console log showing `java -version` and `javac -version` output confirming OpenJDK 21 LTS. | Ready to Capture |
| **4** | **Console Output â€” Test Suite** | Console log displaying `Tests run: 11, Failures: 0, Errors: 0, Skipped: 0` and `BUILD SUCCESS`. | Ready to Capture |
| **5** | **Console Output â€” Archiving** | Console log showing `Archiving artifacts` for `target/claimguard-dashboard-1.0.0-SNAPSHOT.jar`. | Ready to Capture |
| **6** | **Build Summary & Artifact Page** | Build overview page showing the archived `claimguard-dashboard-1.0.0-SNAPSHOT.jar` download link and JUnit Test Result trend. | Ready to Capture |
| **7** | **Local Staging Health Check** | PowerShell window showing successful HTTP 200 responses from `http://localhost:8080/api/health` and `http://localhost:8080/actuator/health`. | Ready to Capture |

---

## 7. Troubleshooting Guide

| Symptom / Error | Root Cause | Solution |
|:---|:---|:---|
| **`No compiler is provided in this environment`** | `mvnw.cmd` ran against a JRE (such as Java 8) instead of the JDK 21 compiler. | Verify `JAVA_HOME` in `Jenkinsfile` points to `C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot` and that `%JAVA_HOME%\bin` is prepended to `PATH`. |
| **`mvnw.cmd is not recognized`** | Working directory does not contain `mvnw.cmd` or agent path is incorrect. | Ensure Git checkout succeeded and `mvnw.cmd` exists in the repository root. In Windows `bat` steps, always invoke via `call mvnw.cmd`. |
| **`Failed to connect to repository`** | Jenkins cannot reach GitHub or git executable is not configured. | In Jenkins, go to *Manage Jenkins â†’ Tools â†’ Git* and ensure the path to `git.exe` is set (typically `C:\Program Files\Git\bin\git.exe` or `git.exe` on system PATH). |
| **`Access is denied (target\....jar)`** | An active Java process has locked the existing JAR file in the workspace. | Ensure no local `java -jar` process is currently running from the Jenkins workspace directory before triggering the build. |
| **`No such DSL method 'cleanWs'`** | The Workspace Cleanup plugin is not installed on the Jenkins controller. | The `Jenkinsfile` wraps `cleanWs` in a `try/catch` block so this error will not fail the build. Install the *Workspace Cleanup* plugin via *Manage Jenkins â†’ Plugins* to enable cleanup. |
| **`HTTP 403 Forbidden` when querying Jenkins API** | CSRF protection or unauthorized anonymous API access. | Access Jenkins through the web browser with authenticated session credentials. |

---

## 8. Honest Task 8 Implementation Status Matrix

| Component / Requirement | Implementation & Verification Status | Evidence / Verification Method |
|:---|:---:|:---|
| Root `Jenkinsfile` created | **Implemented locally** | [`Jenkinsfile`](../Jenkinsfile) in repository root; Declarative syntax with Windows `bat` steps. |
| Explicit Java 21 environment | **Verified locally** | Configured in `environment` block and batch commands; verified locally via `javac 21.0.12.1`. |
| Clean automated test execution | **Verified locally** | `.\mvnw.cmd clean test` executed locally; 11 passed, 0 failures, 0 errors, 0 skipped. |
| Application packaging & artifact check | **Verified locally** | `.\mvnw.cmd package` executed locally; 51.5 MB JAR verified in `target/`. |
| Artifact archiving specification | **Implemented locally** | `archiveArtifacts` configured for `target/claimguard-dashboard-1.0.0-SNAPSHOT.jar`. |
| Non-destructive post actions | **Implemented locally** | Success/failure banners and resilient `cleanWs` cleanup implemented. |
| Safe deployment gate & procedure | **Implemented locally & documented** | Non-destructive staging procedure documented; remote deployment held for Tasks 10â€“12. |
| Stitch UI reference preservation | **Verified locally** | `git diff -- docs/stitch-export/` completely empty. |
| `ClaimGuard-Pipeline` execution in Jenkins | **Pending in Jenkins** | Not yet triggered in Jenkins UI; requires user to create job and trigger *Build Now*. |
| Docker / Remote Server Deployment | **Pending remote server configuration** | Explicitly held on standby for Tasks 10â€“12 in accordance with project roadmap. |

---

*ClaimGuard â€” Claim Fraud Review Dashboard | College DevOps Assignment | Task 8 Documentation*
