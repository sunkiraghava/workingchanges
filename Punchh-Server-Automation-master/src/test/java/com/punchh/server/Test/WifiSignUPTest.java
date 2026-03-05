package com.punchh.server.Test;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.annotations.Owner;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.TestListeners;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class WifiSignUPTest {
	static Logger logger = LogManager.getLogger(WifiSignUPTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String sTCName;
	private String run = "ui";
	private String env;
	private String baseUrl;
	private static Map<String, String> dataSet;
	Properties prop;

	@BeforeClass(alwaysRun = true)
	public void openBrowser() {
		driver = new BrowserUtilities().launchBrowser();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		// single Login to instance
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		pageObj.instanceDashboardPage().instanceLogin(baseUrl);
	}

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
		sTCName = method.getName();
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		logger.info(sTCName + " ==>" + dataSet);
		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
	}

	@Test(description = "SQ-T2282, Validate Signup, Sign in using WIFI API", groups = { "regression",
			"dailyrun" }, priority = 0)
	@Owner(name = "Amit Kumar")
	public void T2282_wifiSignUpValidationString() throws InterruptedException {
		logger.info("== Wifi user signup Test ==");
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().wifiUserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"), dataSet.get("locationId"));
		Assert.assertEquals(202, signUpResponse.getStatusCode());
		// Verifying user Signup on guest timeline
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		boolean joiningchannel = pageObj.guestTimelinePage().verifyGuestTimelineNew("Joined Us via Wifi");
		String guestTimelineEmail = pageObj.guestTimelinePage().getGuestTimelineEmail();

		Assert.assertTrue(joiningchannel, "Joined channel guest details did not matched");
		Assert.assertEquals(userEmail, guestTimelineEmail, "Guest email did not matched on timeline header");
		TestListeners.extentTest.get().pass("Successfully verified guest email and joined channel");

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		logger.info("Data set cleared");
	}

	@AfterClass(alwaysRun = true)
	public void closeBrowser() {
		driver.quit();
		logger.info("Browser closed");
	}
}
