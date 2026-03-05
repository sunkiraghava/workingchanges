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
public class PosUserSignUpTest {
	static Logger logger = LogManager.getLogger(PosUserSignUpTest.class);
	public WebDriver driver;
	private String userEmail;
	private PageObj pageObj;
	private String sTCName;
	private String env, run = "ui";
	private String baseUrl;
	private Map<String, String> dataSet;

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
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

	@Test(description = "SQ-T2172 POS user signup test", priority = 0)
	@Owner(name = "Amit Kumar")
	public void T2172_PosUserSignUp() throws InterruptedException {
		logger.info("== POS user signup and test ==");
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		/*
		 * pageObj.instanceDashboardPage().forgotPassword();
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 */
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		Response response = pageObj.endpoints().posSignUp(userEmail, dataSet.get("locationKey"));
		Assert.assertEquals(response.getStatusCode(), 200);
		Assert.assertEquals(response.jsonPath().get("email").toString(), userEmail.toLowerCase());
		logger.info("Response time for Pos Signup api in milliseconds is :" + response.getTime());
		// Verify User Signin On Timeline

		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		String guestTimelineEmail = pageObj.guestTimelinePage().getGuestTimelineEmail();
		String joiningchannel = pageObj.guestTimelinePage().getGuestJoiningChannelPOS();
		Assert.assertEquals(dataSet.get("joinedChannel"), joiningchannel,
				"Joined channel guest details did not matched");
		Assert.assertEquals(userEmail, guestTimelineEmail, "Guest email did not matched on timeline header");
		pageObj.utils().logPass("Successfully verified guest email and joined channel");
	}

	@AfterMethod(alwaysRun = true)
	public void afterClass() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}
}
