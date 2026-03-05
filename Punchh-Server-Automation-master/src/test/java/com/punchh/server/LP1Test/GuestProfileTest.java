package com.punchh.server.LP1Test;

import java.lang.reflect.Method;
import java.util.Map;
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
import com.punchh.server.utilities.ApiUtils;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.TestListeners;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class GuestProfileTest {
	static Logger logger = LogManager.getLogger(GuestProfileTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private static Map<String, String> dataSet;
	private String env;
	private String baseUrl;
	ApiUtils apiUtils;
	String sTCName, userEmail;
	String run = "ui";

	@BeforeClass(alwaysRun = true)
	public void openBrowser() {
		driver = new BrowserUtilities().launchBrowser();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		// Single login to instance
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		pageObj.instanceDashboardPage().instanceLogin(baseUrl);
	}

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
		sTCName = method.getName();
		apiUtils = new ApiUtils();
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==>" + dataSet);
		// Move to All Businesses page
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
	}

//	merged in T2648_verifyGuestPhoneNumberDisplayedOnTimelineBelowEmailSalesforceId
//	@Test(description = "SQ-T2649,Validate that If the phone number is not available, nothing is getting displayed on guest timeline || SQ-T2650 Validate that if user enters phone numbers containing ‘(', ’)', '-', on saving the phone number all these type of characters gets removed and phone number with 10 digits gets saved.", groups = {
//			"regression", "dailyrun" })
	@Owner(name = "Ashwini Shetty")
	public void T2649_VerifyGuestTimelineForPhonbeNumber() throws InterruptedException {
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		// Guest signup
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client").trim(),
				dataSet.get("secret").trim());
		apiUtils.verifyResponse(signUpResponse, "API 1 user signup");
		Assert.assertEquals(signUpResponse.getStatusCode(), 200);

		// Verify checkin in guest timeline
		// pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		// pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.menupage().clickGuestValidation();
		pageObj.dashboardpage().offEnableBusinessPhoneUniqueness();
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_phone_uniqueness", "uncheck");
		pageObj.dashboardpage().updateCheckBox();

		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		// verify the phone number of the guest
		pageObj.guestTimelinePage().verifyPhoneNumber(userEmail);
	}

	@Test(description = "SQ-T2644 Validate that In the Account History section, this Force Redemption would display the points equivalent dollar value", groups = {
			"regression", "unstable", "dailyrun" })
	@Owner(name = "Hardik Bhardwaj")
	public void T2644_VerifyForceRedemptionWouldDisplayThePointsEquivalentDollarValue() throws InterruptedException {
		// login to instance
		// pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		// pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Navigate to Cockpit -> Redemptions -> Redemption Display -> configuration
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.redeemablePage().configureRedemptionDisplay("100");

		// Navigate to settings-> membership-> configurations
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		pageObj.settingsPage().membershipSetting("Mem_Level Bronze Guest", "0.0");
		pageObj.settingsPage().membershipSetting("Mem_Level Silver Guest", "0.0");
		pageObj.settingsPage().membershipSetting("Mem_Level Gold Guest", "0.0");

		// Signup by API 2
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), 200, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("API2 Signup is successful");
		logger.info("API2 Signup is successful");

		// send points to user Redeemable
		Response sendRewardResponse1 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "100", "",
				"", "100");
		Assert.assertEquals(sendRewardResponse1.getStatusCode(), 201,
				"Status code 201 did not matched for api2 send message to user");
		logger.info("Send redeemable to the user successfully");
		TestListeners.extentTest.get().pass("Send redeemable to the user successfully");

		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		// force redemption of 30 points
		Response forceRedeem_Response = pageObj.endpoints().pointForceRedeemWithType(dataSet.get("apiKey"), userID,
				"30", "points_redemption");
		Assert.assertEquals(forceRedeem_Response.getStatusCode(), 201,
				"Status code 201 did not match for force Redemption of Points");

		// verifying on Account history
		boolean result1 = pageObj.awaitingMigrationPage().checkPointsAfterForceRedemption("$3.00");
		Assert.assertTrue(result1, "Force Redemption amount is not $3.00");
		// verifying on Guest Timeline
		pageObj.guestTimelinePage().navigateInsideGuestTimeline("Timeline");
		boolean result2 = pageObj.guestTimelinePage().checkAumoutAfterForceRedemptionOnTimeline("$3.00");
		Assert.assertTrue(result2, "Force Redemption amount is not $3.00");

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
