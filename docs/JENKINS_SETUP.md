# Jenkins Setup Guide

This guide explains how to set up and run the Bond Trading Robot Framework on Jenkins.

## Prerequisites

### 1. Jenkins Plugins Required

Install the following plugins in Jenkins:

- **Pipeline Plugin** (usually pre-installed)
- **HTML Publisher Plugin** (for Allure report viewing)
- **JUnit Plugin** (for test result publishing)
- **Allure Jenkins Plugin** (optional, for enhanced Allure integration)

### 2. Jenkins Tools Configuration

Configure the following tools in Jenkins:

**Go to**: `Manage Jenkins` → `Tools` → `Global Tool Configuration`

#### Java JDK
- **Name**: `jdk17`
- **Type**: JDK
- **JAVA_HOME**: Path to Java 17 installation
  - Example: `/usr/lib/jvm/java-17-openjdk` (Linux)
  - Example: `/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home` (macOS)
  - Example: `C:\Program Files\Java\jdk-17` (Windows)

#### Maven
- **Name**: `M3`
- **Type**: Maven
- **Version**: 3.9+ (or use "Install automatically" option)

## Jenkinsfile Location

The `Jenkinsfile` is located in the **root directory** of the project:
```
bond-trading-framework/Jenkinsfile
```

Jenkins will automatically detect and use this file when you create a Pipeline job.

## Creating a Jenkins Job

### Option 1: Pipeline Job (Recommended)

1. **Create New Item**
   - Go to Jenkins dashboard
   - Click "New Item"
   - Enter job name (e.g., "Bond-Trading-Tests")
   - Select "Pipeline"
   - Click "OK"

2. **Configure Pipeline**
   - **Definition**: Pipeline script from SCM
   - **SCM**: Git
   - **Repository URL**: Your Git repository URL
   - **Credentials**: Add if repository is private
   - **Branches to build**: `*/main` or `*/master`
   - **Script Path**: `Jenkinsfile` (default)

3. **Save and Build**
   - Click "Save"
   - Click "Build Now"

### Option 2: Copy Jenkinsfile Content

If you prefer to paste the pipeline directly:

1. Create a Pipeline job
2. In Pipeline configuration, select "Pipeline script"
3. Copy the content from `Jenkinsfile`
4. Paste into the script text area
5. Save and build

## Pipeline Stages

The Jenkinsfile executes the following stages:

1. **Checkout**: Clones the repository
2. **Build**: Compiles Java source code (`mvn clean compile`)
3. **Test**: Runs Robot Framework tests (`mvn verify`)
4. **Generate Allure Report**: Creates Allure report (`mvn allure:report`)

## Viewing Results

### After Build Completion

1. **Build Status**
   - Green ball = Success
   - Yellow ball = Unstable (some tests failed)
   - Red ball = Failed

2. **Allure Report**
   - Click on the build number
   - Look for "Allure Test Report" link in the left sidebar
   - Or check "Build Artifacts" section

3. **Robot Framework Reports**
   - Navigate to build artifacts
   - Download `report.html` or `log.html`

4. **Test Results**
   - Click "Test Result" link to see JUnit-style results
   - Shows pass/fail summary

## Customizing Tool Names

If your Jenkins has different tool names, update the `tools` section in `Jenkinsfile`:

```groovy
tools {
    jdk 'your-jdk-name'      // Change 'jdk17' to your JDK tool name
    maven 'your-maven-name'  // Change 'M3' to your Maven tool name
}
```

## Troubleshooting

### Build Fails with "Tool not found"

**Error**: `Tool 'jdk17' is not configured`

**Solution**: 
1. Go to `Manage Jenkins` → `Tools`
2. Configure JDK with name exactly matching `jdk17`
3. Or update Jenkinsfile to match your tool name

### Maven Not Found

**Error**: `mvn: command not found`

**Solution**:
1. Ensure Maven is configured in Jenkins Tools
2. Check the tool name matches `M3` in Jenkinsfile
3. Verify Maven is in PATH

### Allure Report Not Showing

**Error**: Allure report link not visible

**Solution**:
1. Install "HTML Publisher Plugin"
2. Check build artifacts for `target/allure-report/index.html`
3. Manually download and open the HTML file

### Tests Not Running

**Error**: Robot Framework tests not executing

**Solution**:
1. Check Maven logs for errors
2. Verify `pom.xml` is correct
3. Ensure Java 17 is being used
4. Check Robot Framework plugin is configured correctly

## Environment Variables

The pipeline sets these environment variables:

- `JAVA_HOME`: Java 17 installation path
- `MAVEN_HOME`: Maven installation path
- `PATH`: Updated to include Java and Maven binaries

## Build Artifacts

The following artifacts are archived after each build:

- `target/robotframework/*.html` - Robot Framework HTML reports
- `target/robotframework/*.xml` - Robot Framework XML output
- `target/allure-report/**/*` - Complete Allure report
- `target/robotframework/allure-results/**/*` - Allure raw results

## Email Notifications (Optional)

To add email notifications on build failure, add to the `post` section:

```groovy
post {
    failure {
        emailext (
            subject: "Build Failed: ${env.JOB_NAME} - ${env.BUILD_NUMBER}",
            body: "Build failed. Check console output: ${env.BUILD_URL}",
            to: "your-email@example.com"
        )
    }
}
```

## Scheduled Builds

To run tests on a schedule, add a `triggers` section:

```groovy
triggers {
    cron('H 2 * * *')  // Run daily at 2 AM
    // or
    pollSCM('H * * * *')  // Poll SCM every hour
}
```

## Parallel Execution (Advanced)

To run tests in parallel, modify the Test stage:

```groovy
stage('Test') {
    parallel {
        stage('Test Suite 1') {
            steps {
                sh 'mvn verify -B -Dtest.suite=suite1'
            }
        }
        stage('Test Suite 2') {
            steps {
                sh 'mvn verify -B -Dtest.suite=suite2'
            }
        }
    }
}
```

## Support

For issues or questions:
1. Check Jenkins console output
2. Review Maven build logs
3. Verify all prerequisites are installed
4. Check tool configurations match Jenkinsfile

---

**Last Updated**: November 2024

