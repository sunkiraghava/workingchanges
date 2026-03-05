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
import org.testng.asserts.SoftAssert;

import com.punchh.server.annotations.Owner;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.TestListeners;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class OnlineOrderChekinTest {
	static Logger logger = LogManager.getLogger(OnlineOrderChekinTest.class);
	public WebDriver driver;
	private String userEmail;
	private String externalUid;
	private PageObj pageObj;
	private String sTCName;
	private String run = "ui";
	private String env;
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

	@Test(description = "SQ-T2204 Verify Online Order Checkin >> Check-in through valid details", groups = "Sanity", priority = 0)
	@Owner(name = "Amit Kumar")
	public void T2204_authOnlineOrderCheckinTest() throws InterruptedException {
		logger.info("== Online order checkin test ==");
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// set earnig type and Base Conversion Rate to Points configuration
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");
		/*
		 * pageObj.earningPage().setProgramType("Points Based");
		 * pageObj.earningPage().setPointsConvertTo("Currency");
		 */
		pageObj.earningPage().setBaseConversionRate("1.0");
		pageObj.earningPage().updateConfiguration();

		userEmail = pageObj.iframeSingUpPage().generateEmail();

		// Auth api signup
		Response response = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		Assert.assertEquals(response.jsonPath().get("email").toString(), userEmail.toLowerCase());
		String authToken = response.jsonPath().get("authentication_token");

		String amount = "110";
		Response checkinResponse = pageObj.endpoints().onlineOrderCheckin(authToken, amount, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		externalUid = pageObj.endpoints().externalUid;
		logger.info("Response time for online order checkin api in milliseconds is :" + checkinResponse.getTime());

		SoftAssert softAssertion = new SoftAssert();
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		softAssertion.assertTrue(pageObj.guestTimelinePage().verifyAuthCheckinInTimeLine(externalUid, amount,
				dataSet.get("baseConversionRate")), "Error in verifying Checkin tiemline");
		softAssertion.assertTrue(pageObj.guestTimelinePage().verifyCheckinChannelAndLocation("OnlineOrder",
				dataSet.get("checkinLocation")), "Error in capturing checkin channel ");
		softAssertion.assertAll();
	}

	@Test(description = "SQ-T3232, T3220, T3236 Verify Auth API Signup Login And Change Password", groups = "Sanity", priority = 1)
	public void T3232_authApiSignupLoginAndChangePassword() throws InterruptedException {

		userEmail = pageObj.iframeSingUpPage().generateEmail();
		// Auth api signup
		Response response = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		Assert.assertEquals(response.jsonPath().get("email").toString(), userEmail.toLowerCase());
		String authToken = response.jsonPath().get("authentication_token");

		// Auth api login
		Response loginResponse = pageObj.endpoints().authApiUserLogin(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(loginResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED, "Failed - Auth API user login");
		// Auth api change password
		Response changePasswordResponse = pageObj.endpoints().authApiChangePassword(authToken, dataSet.get("client"),
				dataSet.get("secret"), dataSet.get("newPassword"));
		Assert.assertEquals(changePasswordResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Failed - Auth API change password api");

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}
}
