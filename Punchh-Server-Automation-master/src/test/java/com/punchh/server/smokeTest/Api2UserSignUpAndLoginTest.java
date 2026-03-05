package com.punchh.server.smokeTest;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.annotations.Owner;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.TestListeners;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class Api2UserSignUpAndLoginTest {
	static Logger logger = LogManager.getLogger(Api2UserSignUpAndLoginTest.class);
	public WebDriver driver;
	private String userEmail;
	private PageObj pageObj;
	private String sTCName;
	private String env, run = "ui";
	private String baseUrl;
	private Map<String, String> dataSet;

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) throws InterruptedException {
		driver = new BrowserUtilities().launchBrowser();
		sTCName = method.getName();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		logger.info(sTCName + " ==>" + dataSet);
	}

	@Test(description = "SQ-T2358, T3217, T3218 Validate Login/Signup from API 2", groups = "sanity", priority = 0)
	@Owner(name = "Amit Kumar")
	public void T2358_Api2UserSignUpValidation() throws InterruptedException {
		logger.info("== API 2 user signup Test ==");
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		// apiUtils.verifyResponse(signUpResponse, "API 2 user signup");
		Assert.assertEquals(signUpResponse.getStatusCode(), 200);
		Assert.assertEquals(signUpResponse.jsonPath().get("user.communicable_email").toString(),
				userEmail.toLowerCase());
		logger.info("Response time for mobile api2 signup api in milliseconds is :" + signUpResponse.getTime());
		// api 2 login
		Response loginResponse = pageObj.endpoints().Api2Login(userEmail, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(loginResponse.getStatusCode(), 200);
		Assert.assertEquals(loginResponse.jsonPath().get("user.communicable_email").toString(),
				userEmail.toLowerCase());
		logger.info("Response time for mobile api2 login api in milliseconds is :" + loginResponse.getTime());
		// Verifying user Signup on timeline
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		String guestTimelineEmail = pageObj.guestTimelinePage().getGuestTimelineEmail();
		String joiningchannel = pageObj.guestTimelinePage().getGuestJoiningChannelMobileEmail();

		Assert.assertEquals("Joined Us via MobileEmail", joiningchannel,
				"Joined channel guest details did not matched");
		Assert.assertEquals(userEmail, guestTimelineEmail, "Guest email did not matched on timeline header");
		pageObj.utils().logPass("Successfully verified guest email and joined channel");
	}

	@AfterMethod(alwaysRun = true)
	public void teraDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}
}
