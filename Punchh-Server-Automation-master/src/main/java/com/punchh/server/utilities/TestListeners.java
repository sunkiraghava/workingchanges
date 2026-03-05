package com.punchh.server.utilities;

import java.lang.reflect.Method;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.IClassListener;
import org.testng.ITestClass;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;
import org.testng.annotations.Listeners;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import com.punchh.server.annotations.Owner;

@Listeners(TestListeners.class)
public class TestListeners implements ITestListener, IClassListener {
	static Logger log = LogManager.getLogger(TestListeners.class);
	public static Properties prop;
	public static ExtentReports extentReport;

	// private WebDriver testlistnerDriver;
	// private PageObj pageObj;
	@SuppressWarnings("unused")
	// private String env, baseUrl;
	// public static String client, secret, locationKey;
	// public static ExtentTest extentLogger;
	/** The logger. */
	// static Logger logger;
	public static ExtentTest test;
	public static ThreadLocal<ExtentTest> extentTest = new ThreadLocal<ExtentTest>();
	String jiraCycleKey;
	private String sTCName;
	public WebDriver driver;
	private String zephyrRunParam;
	private static int TOTAL_COUNT, PASS_COUNT, FAIL_COUNT, SKIP_COUNT;
	public static ExtentTest executionSummaryTest = null;

	public void onTestStart(ITestResult result) {
		String testName = result.getMethod().getMethodName();
		// Fetch @Owner annotation
		String qaOwner = "Unassigned";
		try {
			Method method = result.getMethod().getConstructorOrMethod().getMethod();
			Owner owner = method.getAnnotation(Owner.class);
			if (owner != null) {
				qaOwner = owner.name();
			}
		} catch (Exception e) {
		}

		// Build test display name
		String testDisplayName = testName + " | QA Owner: " + qaOwner;
		// Create ExtentTest and assign author
		test = extentReport.createTest(testDisplayName).assignAuthor(qaOwner);
		extentTest.set(test); // store in ThreadLocal
		// Optional logging
		log.info("==== Initializing TestCase: " + result.getTestClass().getName() + "." + result.getName()
				+ " | QA Owner: " + qaOwner + " ====");
		extentTest.get().log(Status.INFO, result.getMethod().getDescription());
		// pageObj = new PageObj(driver);
		TOTAL_COUNT++;
		
		logDataProviderInfo(result);
	}

	public void onTestSuccess(ITestResult result) {
		PASS_COUNT += 1;
		extentTest.get().log(Status.PASS, "==== Test Case Passed ====");
		log.info("==== Completed TestCase: " + result.getTestClass().getName() + "." + result.getName()
				+ " Status: Passed ====");
		if (Boolean.parseBoolean(zephyrRunParam)) {
			if (jiraCycleKey != null && !jiraCycleKey.isEmpty()) {
				try {
					new Utilities().updateZephyrTCViaDescription(result, jiraCycleKey, "Pass");
				} catch (Exception e) {
					log.info("Error while updating JIRA Zephyr TC: " + result.getMethod().getMethodName() + " - " + e);
				}
			}
		}
	}

	public void onTestFailure(ITestResult result) {
		FAIL_COUNT += 1;
		sTCName = result.getMethod().getMethodName();

		// Fetch QA Owner (optional, for logging)
		/*
		 * String qaOwner = "Unassigned"; try { Method method =
		 * result.getMethod().getConstructorOrMethod().getMethod(); Owner owner =
		 * method.getAnnotation(Owner.class); if (owner != null) { qaOwner =
		 * owner.name(); } } catch (Exception e) { }
		 */

		// Log failure in Extent (no new test creation!)
		extentTest.get().log(Status.FAIL, result.getThrowable());
		// Screenshot capture
		try {
			WebDriver driver = (WebDriver) result.getTestClass().getRealClass().getDeclaredField("driver")
					.get(result.getInstance());
			if (driver != null) {
				Utilities.screenShotCapture(driver, sTCName);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		log.error("=== Failed TestCase: " + result.getTestClass().getName() + "." + result.getName()
				+ "Status: Failed ====");
	}

	public void onTestSkipped(ITestResult result) {
		SKIP_COUNT += 1;
		extentTest.get().log(Status.SKIP, result.getTestClass().getName() + "." + result.getName());
		log.info("==== Completed TestCase: " + result.getTestClass().getName() + "." + result.getName()
				+ "Status: Skipped ====");
	}

	public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
		// TODO Auto-generated method stub

	}

	// on suite start
	public void onStart(ITestContext context) {

		zephyrRunParam = context.getCurrentXmlTest().getParameter("zephyrExecution");
		if (Boolean.parseBoolean(zephyrRunParam)) {
			jiraCycleKey = new Utilities().getLatestCycleKeyZephyr("Regression Cycle");
			log.info("==== JIRA Cycle Key: " + jiraCycleKey + " ====");
		} else {
			log.warn("==== JIRA Zephy execution not set for this run ====");
		}
		extentReport = ExtentReportManager.getExtent();
		extentReport.setSystemInfo("Author", "Test1");
		extentReport.setSystemInfo("OS", System.getProperty("os.name"));
		extentReport.setSystemInfo("OS Version", System.getProperty("os.version"));
		extentReport.setSystemInfo("OS Arch", System.getProperty("os.arch"));
		extentReport.setSystemInfo("Environment", "QA");
		extentReport.setSystemInfo("User Name", "QA_User");
		executionSummaryTest = extentReport.createTest("Execution Time Summary");
		// check and fetch values for smoke suite
		/*
		 * try { String suite = ConfigurationClass.suite; if
		 * (suite.equalsIgnoreCase("smoke") || suite.equalsIgnoreCase("regression") ||
		 * suite.equalsIgnoreCase("stableregression")) { test =
		 * extentReport.createTest(suite + " Configuration Test"); extentTest.set(test);
		 * 
		 * testlistnerDriver = new BrowserUtilities().launchBrowser(); pageObj = new
		 * PageObj(testlistnerDriver); env = pageObj.getEnvDetails().setEnv(); baseUrl =
		 * pageObj.getEnvDetails().setBaseUrl(); testlistnerDriver.get(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 * 
		 * pageObj.menupage().navigateToSubMenuItem("SRE", "Stats Configuration");
		 * pageObj.dashboardpage().StatsConfigToDatabrickes("Databricks");
		 * pageObj.dashboardpage().clickOnUpdateButton();
		 * pageObj.menupage().navigateToSubMenuItem("Businesses", "All"); }
		 */

		/*
		 * if (suite.equalsIgnoreCase("smoke")) {
		 * pageObj.instanceDashboardPage().selectBusiness("autoone");
		 * pageObj.menupage().navigateToSubMenuItem("Whitelabel", "OAuth Apps"); client
		 * = pageObj.oAuthAppPage().getClient(); secret =
		 * pageObj.oAuthAppPage().getSecret();
		 * pageObj.menupage().navigateToSubMenuItem("Settings", "Locations");
		 * locationKey = pageObj.oAuthAppPage().getLocationKey(); }
		 */
		/*
		 * testlistnerDriver.quit();
		 * log.info(" Test Listener setup driver closed successfully");
		 * 
		 * } catch (Exception e) { if (testlistnerDriver != null) {
		 * testlistnerDriver.quit();
		 * log.info(" Test Listener setup driver closed successfully"); } }
		 */
	}

	public void onFinish(ITestContext context) {

		log.info("============Execution Results==============");
		log.info("TEST CASE EXECUTION COUNT -- " + TOTAL_COUNT);
		log.info("PASSED COUNT -- " + PASS_COUNT);
		log.info("FAILED COUNT -- " + FAIL_COUNT);
		log.info("SKIPPED COUNT -- " + SKIP_COUNT);
		log.info("===========================================");

		executionSummaryTest.log(Status.INFO, "=========================================");
		executionSummaryTest.log(Status.INFO, "Total Test Cases Executed: " + TOTAL_COUNT);
		executionSummaryTest.log(Status.INFO, "Total Passed: " + PASS_COUNT);
		executionSummaryTest.log(Status.INFO, "Total Failed: " + FAIL_COUNT);
		executionSummaryTest.log(Status.INFO, "Total Skipped: " + SKIP_COUNT);
		executionSummaryTest.log(Status.INFO, "=========================================");

		// pls dont use testlistener logger for methods after suite
//			SingletonDBUtils.closeAllConnections();
		try {
			// SingletonDBUtils.closeAllConnectionsUpdated();
			DBManager.shutdownAllPools();
			GmailConnection.closeEmailSession();

		} catch (Exception e) {
			e.printStackTrace();
		}

		extentReport.flush();
		// ExcelUtils.updateLocalRegressionExecutionResultInSheet(context);
	}

	private long classStartTime;

	@Override
	public void onBeforeClass(ITestClass testClass) {
		classStartTime = System.currentTimeMillis();
	}

	@Override
	public void onAfterClass(ITestClass testClass) {
		long durationSeconds = (System.currentTimeMillis() - classStartTime) / 1000;
		double durationMinutes = durationSeconds / 60.0;
		executionSummaryTest.log(Status.INFO, testClass.getName() + " executed in " + durationSeconds
				+ " seconds - equivalent minutes : " + durationMinutes);
		log.info("==== Class: " + testClass.getName() + " took " + durationSeconds + " seconds ====");
	}

	private void logDataProviderInfo(ITestResult result) {
		try {
			Object[] values = result.getParameters();
			if (values == null || values.length == 0) {
				log.info("No data provider info found for test: " + result.getTestClass().getName() + "." + result.getName());
				return;
			}

			Method method = result.getMethod().getConstructorOrMethod().getMethod();
			String[] parameterNames = Utilities.resolveParameterNames(method, values.length);

			StringBuilder tableContent = new StringBuilder();
			tableContent.append("Data Provider Information:\n");
			int columnWidth = 30;
			String horizontalLine = "+" + "-".repeat(columnWidth + 2) + "+" + "-".repeat(columnWidth + 2) + "+\n";
			tableContent.append(horizontalLine);
			tableContent.append(String.format("| %-" + columnWidth + "s | %-" + columnWidth + "s |\n", "Parameter Name", "Value"));
			tableContent.append(horizontalLine);
			for (int i = 0; i < values.length; i++) {
				String paramName = Utilities.trimValue(parameterNames[i], columnWidth);
				String value = Utilities.trimValue(String.valueOf(values[i]), columnWidth);
				tableContent.append(String.format("| %-" + columnWidth + "s | %-" + columnWidth + "s |\n", paramName, value));
				log.info("Parameter Name: " + paramName + " - Value: " + value);
			}
			tableContent.append(horizontalLine.trim());
			new Utilities().logit("<pre>" + tableContent.toString() + "</pre>");
			log.info("Data provider info logged successfully for test: " + result.getTestClass().getName() + "." + result.getName());
		} catch (Exception e) {
			log.info("Failed to log data provider info for test: " + result.getTestClass().getName() + "." + result.getName() + " - " + e.getMessage(), e);
		}
	}
}