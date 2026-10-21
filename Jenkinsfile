pipeline {
    agent any

    environment {
        JAVA_HOME = 'C:\\Program Files\\Eclipse Adoptium\\jdk-21.0.12.101-hotspot'
        PATH = "C:\\Program Files\\Eclipse Adoptium\\jdk-21.0.12.101-hotspot\\bin;${env.PATH}"
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
                    currentBuild.description = "Pipeline as Code execution for ClaimGuard Dashboard"
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
                    set "PATH=%JAVA_HOME%\\bin;%PATH%"
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
                    echo [SUCCESS] Java 21 environment verified.
                '''
            }
        }

        stage('Run Automated Tests') {
            steps {
                bat '''
                    @echo off
                    echo ============================================================
                    echo [STAGE: Run Automated Tests]
                    echo ============================================================
                    set "JAVA_HOME=C:\\Program Files\\Eclipse Adoptium\\jdk-21.0.12.101-hotspot"
                    set "PATH=%JAVA_HOME%\\bin;%PATH%"
                    call mvnw.cmd clean test
                    if errorlevel 1 (
                        echo [ERROR] Maven clean test execution failed.
                        exit /b 1
                    )
                    echo [SUCCESS] All automated tests executed successfully.
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
                    call mvnw.cmd package
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

        stage('Server Deployment Standby') {
            steps {
                bat '''
                    @echo off
                    echo ============================================================
                    echo [STAGE: Server Deployment Standby]
                    echo ============================================================
                    echo [INFO] Build and packaging stages completed successfully.
                    echo [INFO] Automated deployment to remote server or container
                    echo        environment is on standby pending infrastructure
                    echo        provisioning in Tasks 10-12 (Docker, Ansible).
                    echo [INFO] Refer to docs/task-08-pipeline-as-code-and-server-deployment.md
                    echo        for the verified Windows staging and deployment guide.
                    echo ============================================================
                '''
            }
        }
    }

    post {
        success {
            echo '============================================================'
            echo '[PIPELINE SUCCESS] ClaimGuard Pipeline completed successfully!'
            echo '[PIPELINE SUCCESS] Archived artifact: target/claimguard-dashboard-1.0.0-SNAPSHOT.jar'
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
