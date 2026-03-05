package com.punchh.server.Test;

import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.apiConfig.Endpoints;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.ApiUtils;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class OrderCreationTest {
	static Logger logger = LogManager.getLogger(OrderCreationTest.class);
	public WebDriver driver;
	Endpoints ep;
	String userEmail;
	ApiUtils apiUtils;
	PageObj pageObj;
	Properties prop;

	@BeforeMethod(alwaysRun = true)
	public void setUp() {
		prop = Utilities.loadPropertiesFile("config.properties");
		driver = new BrowserUtilities().launchBrowser();
		ep = new Endpoints();
		apiUtils = new ApiUtils();
		pageObj = new PageObj(driver);

	}

	@Test(groups = { "sanity" })
	public void T2189_Api1UserSignUpAndLoginValidation() throws InterruptedException {
		logger.info("== API 1 user signup Test ==");
		String buisness = prop.getProperty("moesSlug");
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		pageObj.instanceDashboardPage().navigateToPunchhInstance(prop.getProperty("instanceUrl"));
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(buisness);
		String client = pageObj.oAuthAppPage().getClient();
		String secret = pageObj.oAuthAppPage().getSecret();
		// String location = pageObj.oAuthAppPage().getLocationKey();
		Response signUpResponse = ep.Api1UserSignUp(userEmail, client, secret);
		apiUtils.verifyResponse(signUpResponse, "API 1 user signup");
		Assert.assertEquals(signUpResponse.getStatusCode(), 200);
		Assert.assertEquals(signUpResponse.jsonPath().get("email").toString(), userEmail.toLowerCase());
		Response loginResponse = ep.Api1UserLogin(userEmail, client, secret);
		apiUtils.verifyResponse(loginResponse, "API 1 user login");
		Assert.assertEquals(loginResponse.getStatusCode(), 200);
		Assert.assertEquals(loginResponse.jsonPath().get("email").toString(), userEmail.toLowerCase());
		// Verifying user Signup on guest timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		String joiningchannel = pageObj.guestTimelinePage().getGuestJoiningChannelMobileEmail();
		String guestTimelineEmail = pageObj.guestTimelinePage().getGuestTimelineEmail();

		Assert.assertEquals(prop.getProperty("joinedViaMobile"), joiningchannel,
				"Joined channel guest details did not matched");
		Assert.assertEquals(userEmail, guestTimelineEmail, "Guest email did not matched on timeline header");
		TestListeners.extentTest.get().pass("Successfully verified guest email and joined channel");
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		driver.close();
		driver.quit();
	}
}
