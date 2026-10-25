pipeline {
    agent any

    environment {
        JAVA_HOME = 'C:\\Program Files\\Eclipse Adoptium\\jdk-21.0.12.101-hotspot'
        DOCKER_BIN = 'C:\\Users\\User\\AppData\\Local\\Programs\\DockerDesktop\\resources\\bin'
        DOCKER_HOST = 'npipe:////./pipe/docker_engine'
        PATH = "${env.DOCKER_BIN};${env.JAVA_HOME}\\bin;${env.PATH}"
        STAGING_PORT = '8083'
        CONTAINER_PORT = '8080'
        STAGING_CONTAINER = 'claimguard-staging'
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
        disableConcurrentBuilds()
        timeout(time: 15, unit: 'MINUTES')
    }

    stages {
        stage('Initialize') {
            steps {
                script {
                    currentBuild.displayName = "#${env.BUILD_NUMBER} - ClaimGuard Main"
                    currentBuild.description = "Continuous Testing & Docker CD Pipeline for ClaimGuard Dashboard"
                }
                echo "============================================================"
                echo "Starting Build #${env.BUILD_NUMBER}"
                echo "Job Name: ${env.JOB_NAME}"
                echo "Workspace: ${env.WORKSPACE}"
                echo "============================================================"
            }
        }

        stage('Verify Environment') {
            steps {
                bat '''
                    @echo off
                    echo ============================================================
                    echo [STAGE: Verify Environment]
                    echo ============================================================
                    set "JAVA_HOME=C:\\Program Files\\Eclipse Adoptium\\jdk-21.0.12.101-hotspot"
                    set "PATH=%DOCKER_BIN%;%JAVA_HOME%\\bin;%PATH%"
                    echo Configured JAVA_HOME: %JAVA_HOME%
                    echo Checking Java runtime version:
                    java -version
                    if errorlevel 1 (
                        echo [ERROR] java -version check failed.
                        exit /b 1
                    )
                    echo Checking Java compiler version:
                    javac -version
                    if errorlevel 1 (
                        echo [ERROR] javac -version check failed.
                        exit /b 1
                    )
                    echo Checking Chrome installation:
                    if exist "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe" (
                        echo [SUCCESS] Chrome binary detected at C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe
                    ) else (
                        echo [WARN] Chrome not found at standard path. Selenium Manager will attempt automated resolution.
                    )
                    echo Checking Docker installation and Engine:
                    where docker
                    if errorlevel 1 (
                        echo [ERROR] docker CLI executable not found on PATH.
                        exit /b 1
                    )
                    docker --version
                    if errorlevel 1 (
                        echo [ERROR] docker --version check failed.
                        exit /b 1
                    )
                    docker info
                    if errorlevel 1 (
                        echo [ERROR] Docker engine is unreachable via %DOCKER_HOST%.
                        exit /b 1
                    )
                    echo [SUCCESS] Build, test, and Docker environment verified.
                '''
            }
        }

        stage('Run Unit & Integration Tests') {
            steps {
                bat '''
                    @echo off
                    echo ============================================================
                    echo [STAGE: Run Unit & Integration Tests]
                    echo ============================================================
                    set "JAVA_HOME=C:\\Program Files\\Eclipse Adoptium\\jdk-21.0.12.101-hotspot"
                    set "PATH=%JAVA_HOME%\\bin;%PATH%"
                    call mvnw.cmd clean test -Dtest="!*Selenium*"
                    if errorlevel 1 (
                        echo [ERROR] Unit and integration test suite failed.
                        exit /b 1
                    )
                    echo [SUCCESS] Unit and integration tests executed successfully (11 tests).
                '''
            }
            post {
                always {
                    junit testResults: '**/target/surefire-reports/*.xml', allowEmptyResults: false
                }
            }
        }

        stage('Package Application') {
            steps {
                bat '''
                    @echo off
                    echo ============================================================
                    echo [STAGE: Package Application]
                    echo ============================================================
                    set "JAVA_HOME=C:\\Program Files\\Eclipse Adoptium\\jdk-21.0.12.101-hotspot"
                    set "PATH=%JAVA_HOME%\\bin;%PATH%"
                    call mvnw.cmd package -DskipTests
                    if errorlevel 1 (
                        echo [ERROR] Maven packaging failed.
                        exit /b 1
                    )
                    echo [INFO] Verifying existence of packaged artifact...
                    if not exist "target\\claimguard-dashboard-1.0.0-SNAPSHOT.jar" (
                        echo [ERROR] Expected artifact target\\claimguard-dashboard-1.0.0-SNAPSHOT.jar was not generated!
                        exit /b 1
                    )
                    echo [SUCCESS] Package artifact verified: target\\claimguard-dashboard-1.0.0-SNAPSHOT.jar
                '''
                archiveArtifacts artifacts: 'target/claimguard-dashboard-1.0.0-SNAPSHOT.jar', fingerprint: true, onlyIfSuccessful: true
            }
        }

        stage('Run Selenium E2E Tests') {
            steps {
                bat '''
                    @echo off
                    echo ============================================================
                    echo [STAGE: Run Selenium E2E Tests]
                    echo ============================================================
                    set "JAVA_HOME=C:\\Program Files\\Eclipse Adoptium\\jdk-21.0.12.101-hotspot"
                    set "PATH=%JAVA_HOME%\\bin;%PATH%"

                    powershell -ExecutionPolicy Bypass -File scripts\\run-e2e-suite.ps1 -Port 8080 -JarPath target\\claimguard-dashboard-1.0.0-SNAPSHOT.jar -Headless $true
                    if errorlevel 1 (
                        echo [ERROR] Selenium E2E browser tests failed or application did not start.
                        exit /b 1
                    )
                    echo [SUCCESS] Selenium E2E tests executed successfully (6 tests).
                '''
            }
            post {
                always {
                    junit testResults: '**/target/surefire-reports/TEST-*Selenium*.xml', allowEmptyResults: false
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                bat '''
                    @echo off
                    echo ============================================================
                    echo [STAGE: Build Docker Image]
                    echo ============================================================
                    echo Packaging Docker images for build #%BUILD_NUMBER%...
                    docker build -t claimguard:b%BUILD_NUMBER% -t claimguard:staging .
                    if errorlevel 1 (
                        echo [ERROR] Docker build failed.
                        exit /b 1
                    )
                    echo [SUCCESS] Docker images built and tagged:
                    echo   - claimguard:b%BUILD_NUMBER%
                    echo   - claimguard:staging
                    docker images claimguard:staging --digests
                '''
            }
        }

        stage('Deploy Staging Container') {
            steps {
                bat '''
                    @echo off
                    echo ============================================================
                    echo [STAGE: Deploy Staging Container]
                    echo ============================================================
                    powershell -ExecutionPolicy Bypass -File scripts\\deploy-staging-container.ps1 ^
                        -ImageTag "claimguard:b%BUILD_NUMBER%" ^
                        -ContainerName "%STAGING_CONTAINER%" ^
                        -HostPort %STAGING_PORT% ^
                        -ContainerPort %CONTAINER_PORT% ^
                        -DockerBin "%DOCKER_BIN%" ^
                        -DockerHost "%DOCKER_HOST%" ^
                        -MaxHealthWaitSeconds 45
                    if errorlevel 1 (
                        echo [ERROR] Staging container deployment or health verification failed.
                        exit /b 1
                    )
                    echo [SUCCESS] Staging container successfully deployed and verified on port %STAGING_PORT%.
                '''
            }
        }
    }

    post {
        success {
            echo '============================================================'
            echo '[PIPELINE SUCCESS] ClaimGuard Pipeline completed successfully!'
            echo '[PIPELINE SUCCESS] Verified: 11 Unit/MockMvc Tests + 6 Selenium E2E Tests'
            echo '[PIPELINE SUCCESS] Archived artifact: target/claimguard-dashboard-1.0.0-SNAPSHOT.jar'
            echo '[PIPELINE SUCCESS] Deployed Docker Container: claimguard-staging on port 8083'
            echo '[PIPELINE SUCCESS] Verified Endpoints: http://localhost:8083/api/health and http://localhost:8083/'
            echo '============================================================'
        }
        failure {
            echo '============================================================'
            echo '[PIPELINE FAILURE] ClaimGuard Pipeline execution failed!'
            echo '[PIPELINE FAILURE] Please check the console output and test reports above.'
            echo '============================================================'
        }
        always {
            echo '[INFO] Post-build processing complete.'
            script {
                try {
                    // Safe cleanup: artifact is already preserved by archiveArtifacts in Package stage
                    cleanWs(deleteDirs: true, notFailBuild: true, disableDeferredWipeout: true)
                } catch (err) {
                    echo "[INFO] Workspace cleanup skipped or ws-cleanup step unavailable: ${err}"
                }
            }
        }
    }
}
