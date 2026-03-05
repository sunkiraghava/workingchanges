# Punchh Server Automation Framework Documentation

## Table of Contents
1. [Overview](#overview)
2. [Framework Architecture](#framework-architecture)
3. [Project Structure](#project-structure)
4. [Technology Stack](#technology-stack)
5. [Configuration Management](#configuration-management)
6. [Test Execution Framework](#test-execution-framework)
7. [Jenkins Pipeline Integration](#jenkins-pipeline-integration)
8. [Selenium Grid and Kubernetes Deployment](#selenium-grid-and-kubernetes-deployment)
9. [Test Data Management](#test-data-management)
10. [Reporting and Logging](#reporting-and-logging)
11. [Execution Commands](#execution-commands)
12. [Suites Description](#suites-description) 
13. [Troubleshooting](#troubleshooting)
14. [Best Practices](#best-practices)

## Overview

The Punchh Server Automation Framework is a comprehensive Java-based Selenium automation testing framework designed to test the Punchh loyalty platform. The framework supports both UI and API testing, with extensive coverage of loyalty program features, campaigns, segments, and integrations.

### Key Features
- **Multi-Environment Support**: QA, PP (Pre-Prod), Dev, and Local environments
- **Parallel Execution**: TestNG-based parallel test execution with configurable thread counts
- **Cross-Browser Testing**: Chrome, Firefox, Safari, and IE support
- **API & UI Testing**: Comprehensive coverage of both API endpoints and UI functionality
- **Mobile Testing**: Appium-based mobile application testing
- **Database Integration**: MySQL and MongoDB connectivity for data validation
- **Extensive Reporting**: ExtentReports with detailed test execution reports
- **CI/CD Integration**: Jenkins pipeline integration for automated test execution

## Framework Architecture

### High-Level Architecture
```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Jenkins CI    │───▶│  Maven Build    │───▶│  TestNG Suite   │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                                       │
                       ┌─────────────────┐             ▼
                       │  Selenium Grid  │◀───┌─────────────────┐
                       │   (Kubernetes)  │    │  Test Execution │
                       └─────────────────┘    └─────────────────┘
                                                       │
                       ┌─────────────────┐             ▼
                       │   Test Reports  │◀───┌─────────────────┐
                       │  (ExtentReports)│    │  Result Analysis│
                       └─────────────────┘    └─────────────────┘
```

### Component Architecture
```
src/
├── main/java/
│   ├── com/punchh/server/
│   │   ├── pages/              # Page Object Model classes
│   │   ├── utilities/          # Utility classes
│   │   ├── apiConfig/          # API configuration
│   │   ├── mobilePages/        # Mobile page objects
│   │   └── annotations/        # Custom annotations
│   └── Support/                # Framework support classes
└── test/java/
    └── com/punchh/server/
        ├── Test/               # UI test classes
        ├── apiTest/            # API test classes
        ├── campaignsTest/      # Campaign-specific tests
        ├── segmentsTest/       # Segment-specific tests
        └── mobileTests/        # Mobile test classes
```

## Project Structure

### Directory Structure
```
Punchh-Server-Automation/
├── src/
│   ├── main/java/
│   │   └── com/punchh/server/
│   │       ├── pages/                    # Page Object Model
│   │       │   ├── PageObj.java         # Main page object factory
│   │       │   ├── DashboardPage.java   # Dashboard page
│   │       │   ├── CampaignsPage.java   # Campaigns page
│   │       │   ├── SegmentsPage.java    # Segments page
│   │       │   └── ...                  # Other page objects
│   │       ├── utilities/                # Utility classes
│   │       │   ├── BrowserUtilities.java
│   │       │   ├── TestListeners.java
│   │       │   ├── ExtentReportManager.java
│   │       │   ├── DBUtils.java
│   │       │   └── Utilities.java
│   │       ├── apiConfig/                # API configuration
│   │       │   ├── Endpoints.java
│   │       │   └── ProdEndpoints.java
│   │       └── mobilePages/              # Mobile page objects
│   └── test/java/
│       └── com/punchh/server/
│           ├── Test/                     # UI test classes
│           ├── apiTest/                  # API test classes
│           ├── campaignsTest/            # Campaign tests
│           ├── segmentsTest/             # Segment tests
│           └── mobileTests/              # Mobile tests
├── resources/
│   ├── Properties/                       # Configuration files
│   │   ├── config.properties
│   │   ├── apiConfig.properties
│   │   └── dbConfig.properties
│   ├── Locators/                         # Element locators
│   │   ├── objRepository.json
│   │   └── mobileObjRepository.json
│   ├── Testdata/                         # Test data files
│   │   ├── qa/
│   │   ├── pp/
│   │   └── prod/
│   └── Images/                           # Test images
├── test-output/                          # Test execution reports
├── logs/                                 # Execution logs
├── pom.xml                              # Maven configuration
├── regression.xml                       # Regression test suite
├── api.xml                              # API test suite
├── smoke.xml                            # Smoke test suite
└── suitesLocal/                         # Local test suites
```

## Technology Stack

### Core Technologies
- **Java 11**: Primary programming language
- **Maven 3.8.1**: Build and dependency management
- **TestNG 7.10.2**: Test framework
- **Selenium 4.22.0**: Web automation
- **Appium 9.3.0**: Mobile automation

### Dependencies
```xml
<!-- Core Testing Framework -->
<dependency>
    <groupId>org.testng</groupId>
    <artifactId>testng</artifactId>
    <version>7.10.2</version>
</dependency>

<!-- Selenium WebDriver -->
<dependency>
    <groupId>org.seleniumhq.selenium</groupId>
    <artifactId>selenium-java</artifactId>
    <version>4.22.0</version>
</dependency>

<!-- API Testing -->
<dependency>
    <groupId>io.rest-assured</groupId>
    <artifactId>rest-assured</artifactId>
    <version>4.5.0</version>
</dependency>

<!-- Reporting -->
<dependency>
    <groupId>com.aventstack</groupId>
    <artifactId>extentreports</artifactId>
    <version>5.0.9</version>
</dependency>

<!-- Database Connectivity -->
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <version>8.0.33</version>
</dependency>

<!-- Mobile Testing -->
<dependency>
    <groupId>io.appium</groupId>
    <artifactId>java-client</artifactId>
    <version>9.3.0</version>
</dependency>
```

## Configuration Management

### Environment Configuration
The framework supports multiple environments through configuration files:

#### config.properties
```properties
# Environment URLs
pp.baseurl=https://dashboard.staging.punchh.io
qa.baseurl=https://dashboard.qa.punchh.io
dev.baseurl=https://development.punchh.io
prod.baseurl=https://dashboard.punchh.com

# Authentication
userName=super****4@example.com
password=Vk5/82gB3lL2EOixpbnGag==

# Browser Configuration
BROWSER=chrome
RUNTYPE=local
HUB_HOST=localhost
```

#### apiConfig.properties
```properties
# API Configuration
secret=acf3f27dca79705dc14a1fdc63d84904ab90c76e97cf92326fae0dd04df9f9d4
client=5ed3c669a2c31304b066207507a32156462d9c1d4ed48dd1f2e773d78cbdbcbf

# Common API Parameters
email=supera****@example.com
password=*****
contentType=application/json
```

#### dbConfig.properties
```properties
# Database Configuration
pp.Host=primary-punchh-pre-prod.punchh-internal.net
pp.DBName=punchh_production
pp.UserName=UMcHcDyppihm8tT3Vrckvg==
pp.Password=TYulmBazB7byzbTHG4LFiw==
pp.Port=3306
```

### Test Suite Configuration
Test suites are configured using TestNG XML files:

#### regression.xml
```xml
<suite name="Regression Suite" parallel="classes" thread-count="20">
    <parameter name="BROWSER" value="chrome" />
    <parameter name="RUNTYPE" value="local" />
    <parameter name="HUB_HOST" value="" />
    <parameter name="suite" value="regression" />
    <parameter name="environment" value="pp" />
    
    <listeners>
        <listener class-name="com.punchh.server.utilities.TestListeners" />
    </listeners>
    
    <test name="Regression Test">
        <classes>
            <class name="Support.ConfigurationClass" />
            <class name="com.punchh.server.Test.RedemptionTest" />
            <!-- Additional test classes -->
        </classes>
    </test>
</suite>
```

## Test Execution Framework

### Page Object Model Implementation

#### PageObj.java - Main Page Factory
```java
public class PageObj {
    public WebDriver driver;
    private DashboardPage dashboardPage;
    private CampaignsPage campaignsPage;
    private SegmentsPage segmentsPage;
    
    public PageObj(WebDriver driver) {
        this.driver = driver;
    }
    
    public DashboardPage dashboardPage() {
        if (dashboardPage == null) {
            dashboardPage = new DashboardPage(driver);
        }
        return dashboardPage;
    }
    
    // Additional page object getters
}
```

#### BrowserUtilities.java - WebDriver Management
```java
public class BrowserUtilities {
    public WebDriver launchBrowser() {
        String browserName = getBrowser();
        String runType = getRunType();
        String host = getHost();
        
        if (runType.equalsIgnoreCase("local")) {
            driver = invokeLocalDriver(browserName);
        } else if (runType.equalsIgnoreCase("remote")) {
            driver = invokeRemoteDriver(browserName, host);
        }
        
        return driver;
    }
    
    public WebDriver invokeRemoteDriver(String browserName, String host) {
        ChromeOptions options = new ChromeOptions();
        // Configure Chrome options
        URL url = new URL("http://" + host + ":4444");
        driver = new RemoteWebDriver(url, options);
        return driver;
    }
}
```

### Test Class Structure
```java
@Listeners(TestListeners.class)
public class RedemptionTest {
    private static Logger logger = LogManager.getLogger(RedemptionTest.class);
    public WebDriver driver;
    PageObj pageObj;
    
    @BeforeMethod
    public void setUp(Method method) {
        driver = new BrowserUtilities().launchBrowser();
        pageObj = new PageObj(driver);
    }
    
    @Test
    public void testRedemptionFlow() {
        // Test implementation
        pageObj.dashboardPage().navigateToRedemptions();
        // Additional test steps
    }
    
    @AfterMethod
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
}
```

## Jenkins Pipeline Integration

### Maven Surefire Plugin Configuration
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <version>3.5.4</version>
    <configuration>
        <suiteXmlFiles>
            <suiteXmlFile>${suiteXmlFile}</suiteXmlFile>
        </suiteXmlFiles>
        <forkCount>1</forkCount>
        <reuseForks>true</reuseForks>
        <argLine>-Xmx2048m -XX:+UseG1GC</argLine>
    </configuration>
</plugin>
```

### Jenkins Pipeline Execution
The framework is designed to be executed through Jenkins with the following parameters:

#### Pipeline Parameters
- **suiteXmlFile**: Test suite XML file (regression.xml, api.xml, smoke.xml)
- **environment**: Target environment (pp, qa, dev, prod)
- **BROWSER**: Browser type (chrome, firefox, safari)
- **RUNTYPE**: Execution type (local, remote)
- **HUB_HOST**: Selenium Grid hub host

#### Jenkins Pipeline Script example
```groovy
pipeline {
    agent any
    tools {
        maven 'M3'
        jdk 'JDK11'
    }
    parameters {
        choice(
            name: 'TEST_SUITE',
            choices: ['regression.xml', 'api.xml', 'smoke.xml'],
            description: 'Select test suite to execute'
        )
        choice(
            name: 'ENVIRONMENT',
            choices: ['pp', 'qa', 'dev', 'prod'],
            description: 'Select target environment'
        )
        choice(
            name: 'BROWSER',
            choices: ['chrome', 'firefox', 'safari'],
            description: 'Select browser for execution'
        )
    }
    stages {
        stage('Checkout') {
            steps {
                git branch: 'main', url: 'https://github.com/punchh/automation.git'
            }
        }
        stage('Build') {
            steps {
                sh 'mvn clean compile'
            }
        }
        stage('Test') {
            steps {
                sh "mvn test -DsuiteXmlFile=${params.TEST_SUITE} -Denvironment=${params.ENVIRONMENT} -DBROWSER=${params.BROWSER} -DRUNTYPE=remote -DHUB_HOST=selenium-hub"
            }
        }
        stage('Publish Reports') {
            steps {
                publishHTML([
                    allowMissing: false,
                    alwaysLinkToLastBuild: true,
                    keepAll: true,
                    reportDir: 'test-output',
                    reportFiles: 'index.html',
                    reportName: 'Test Report'
                ])
            }
        }
    }
    post {
        always {
            archiveArtifacts artifacts: 'test-output/**/*', fingerprint: true
        }
    }
}
```

## Selenium Grid and Kubernetes Deployment

### Selenium Grid Configuration
The framework supports both local and remote execution through Selenium Grid:

#### Remote WebDriver Configuration
```java
public WebDriver invokeRemoteDriver(String browserName, String host) {
    ChromeOptions options = new ChromeOptions();
    options.addArguments("--no-sandbox");
    options.addArguments("--disable-dev-shm-usage");
    options.addArguments("--window-size=1920,1080");
    
    URL url = new URL("http://" + host + ":4444");
    driver = new RemoteWebDriver(url, options);
    return driver;
}
```

### Kubernetes Deployment
For Kubernetes deployment, the framework connects to a Selenium Grid deployed on Kubernetes:

#### Selenium Hub Service
```yaml
apiVersion: v1
kind: Service
metadata:
  name: selenium-hub
spec:
  ports:
  - port: 4444
    targetPort: 4444
  selector:
    app: selenium-hub
```

#### Selenium Node Deployment
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: selenium-chrome-node
spec:
  replicas: 3
  selector:
    matchLabels:
      app: selenium-chrome-node
  template:
    metadata:
      labels:
        app: selenium-chrome-node
    spec:
      containers:
      - name: selenium-chrome-node
        image: selenium/node-chrome:latest
        env:
        - name: HUB_HOST
          value: "selenium-hub"
        - name: HUB_PORT
          value: "4444"
```

### Grid Connection Parameters
- **HUB_HOST**: Kubernetes service name (e.g., selenium-hub)
- **RUNTYPE**: Set to "remote" for grid execution
- **BROWSER**: Target browser (chrome, firefox)

## Test Data Management

### Test Data Structure
Test data is organized by environment and test type:

```
resources/Testdata/
├── qa/
│   ├── testdata_apiqa.json
│   └── testdata_uiqa.json
├── pp/
│   ├── testdata_apipp.json
│   └── testdata_uipp.json
└── prod/
    ├── testdata_apiprod.json
    └── testdata_uiprod.json
```

### Test Data Format
```json
{
    "verifyPosAPiSingUp_Checkin_Redemption_VoidRedemption": {
        "slug": "autoone",
        "locationKey": "e39d1b277424df7790cd2d9057f4b680",
        "redeemAmount": "5.00",
        "file": "STAGE"
    },
    "verify_Api2_UserSignUp_Login_Logout": {
        "instanceUrl": "https://dashboard.staging.punchh.io",
        "whiteLabel": "whitelabel/",
        "slug": "autoone",
        "client": "6467217411ef2fd996169b44752a970c7d516bc5da6463bccea482c6b8bd6328",
        "secret": "81be3eb6cb7c2a0db61c3dd2ebfd2712433f0bd5ea9a19e9400b5a631c18b4e1"
    }
}
```

### Locator Management
Element locators are stored in JSON format:

```json
{
    "instanceLoginPage": {
        "emailField": "//input[@id='admin_email']",
        "passwordField": "//input[@id='admin_password']",
        "loginButton": "//button[normalize-space()='Login']"
    },
    "dashboardPage": {
        "campaignsMenu": "//a[contains(text(),'Campaigns')]",
        "segmentsMenu": "//a[contains(text(),'Segments')]"
    }
}
```

## Reporting and Logging

### ExtentReports Integration
The framework uses ExtentReports for comprehensive test reporting:

#### TestListeners.java
```java
@Listeners(TestListeners.class)
public class TestListeners implements ITestListener {
    public static ExtentReports extentReport;
    public static ExtentTest test;
    
    public void onTestStart(ITestResult result) {
        String testName = result.getMethod().getMethodName();
        test = extentReport.createTest(testName);
        extentTest.set(test);
    }
    
    public void onTestSuccess(ITestResult result) {
        extentTest.get().log(Status.PASS, "Test Passed");
    }
    
    public void onTestFailure(ITestResult result) {
        extentTest.get().log(Status.FAIL, "Test Failed: " + result.getThrowable());
    }
}
```

### Logging Configuration
Log4j2 is used for comprehensive logging:

```xml
<Configuration status="WARN">
    <Appenders>
        <Console name="Console" target="SYSTEM_OUT">
            <PatternLayout pattern="%d{HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n"/>
        </Console>
        <File name="FileAppender" fileName="logs/AutomationLog.log">
            <PatternLayout pattern="%d{yyyy-MM-dd HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n"/>
        </File>
    </Appenders>
    <Loggers>
        <Root level="INFO">
            <AppenderRef ref="Console"/>
            <AppenderRef ref="FileAppender"/>
        </Root>
    </Loggers>
</Configuration>
```

## Execution Commands

### Local Execution
```bash
# Run regression suite locally
mvn test -DsuiteXmlFile=regression.xml -Denvironment=pp -DBROWSER=chrome -DRUNTYPE=local

# Run API tests
mvn test -DsuiteXmlFile=api.xml -Denvironment=pp -DBROWSER=chrome -DRUNTYPE=local

# Run smoke tests
mvn test -DsuiteXmlFile=smoke.xml -Denvironment=pp -DBROWSER=chrome -DRUNTYPE=local
```

### Remote Execution (Selenium Grid)
```bash
# Run tests on Selenium Grid
mvn test -DsuiteXmlFile=regression.xml -Denvironment=pp -DBROWSER=chrome -DRUNTYPE=remote -DHUB_HOST=selenium-hub

# Run with specific thread count
mvn test -DsuiteXmlFile=regression.xml -Denvironment=pp -DBROWSER=chrome -DRUNTYPE=remote -DHUB_HOST=selenium-hub -DthreadCount=10
```

### Jenkins Pipeline Execution
```bash
# Trigger Jenkins pipeline with parameters
curl -X POST "http://jenkins-server/job/automation-pipeline/buildWithParameters" \
  -d "TEST_SUITE=regression.xml" \
  -d "ENVIRONMENT=pp" \
  -d "BROWSER=chrome"
```
## Suites Description

### Main Test Suites
- **regression.xml**: Comprehensive regression tests covering all major functionalities.
- **api.xml**: Focused API tests for validating backend services.
- **smoke.xml**: Quick smoke tests for basic functionality verification.


## Troubleshooting

### Common Issues

#### 1. WebDriver Connection Issues
**Problem**: Unable to connect to Selenium Grid
**Solution**: 
- Verify Selenium Grid is running
- Check HUB_HOST parameter
- Ensure network connectivity

#### 2. Test Data Issues
**Problem**: Test data not found
**Solution**:
- Verify test data files exist in correct environment folder
- Check JSON format validity
- Ensure proper file permissions

#### 3. Database Connection Issues
**Problem**: Database connection failures
**Solution**:
- Verify database credentials
- Check network connectivity
- Ensure database service is running


### Debug Mode
Enable debug mode for detailed logging:
```bash
mvn test -DsuiteXmlFile=regression.xml -Denvironment=pp -DBROWSER=chrome -DRUNTYPE=local -Dlog4j.configurationFile=log4j2-debug.xml
```

## Best Practices

### 1. Test Design
- Use Page Object Model for maintainable code
- Implement proper wait strategies
- Use meaningful test method names
- Group related tests using TestNG groups

### 2. Data Management
- Use environment-specific test data
- Encrypt sensitive information
- Maintain data consistency across environments
- Use data-driven testing where applicable

### 3. Execution Strategy
- Run smoke tests for quick validation
- Execute regression tests for comprehensive coverage
- Use parallel execution for faster feedback
- Implement proper test categorization

### 4. Maintenance
- Regular framework updates
- Monitor test execution trends
- Maintain test data accuracy
- Update locators when UI changes

### 5. Reporting
- Generate comprehensive reports
- Include screenshots for failed tests
- Track test execution metrics
- Implement proper logging

---

## Conclusion

The Punchh Server Automation Framework provides a robust, scalable, and maintainable solution for automated testing of the Punchh loyalty platform. With its comprehensive coverage of UI and API testing, multi-environment support, and seamless CI/CD integration, it ensures high-quality software delivery while reducing manual testing efforts.

The framework's architecture supports both local and distributed execution, making it suitable for various testing scenarios from development to production environments. Its integration with Jenkins and Kubernetes enables efficient continuous integration and deployment processes.

For questions or support regarding this framework, please contact the automation team or refer to the project documentation.
