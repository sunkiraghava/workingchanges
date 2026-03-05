package com.punchh.server.Test;

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
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.ApiUtils;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class ValidateNegativePointsInAccountHistoryOfGuestTest {
	static Logger logger = LogManager.getLogger(ValidateNegativePointsInAccountHistoryOfGuestTest.class);
	public WebDriver driver;
	String userEmail;
	ApiUtils apiUtils;
	PageObj pageObj;
	String sTCName;
	String timeStamp;
	String run = "ui";
	private String env;
	private String baseUrl;
	private static Map<String, String> dataSet;
	Utilities utils;

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
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==>" + dataSet);
		utils = new Utilities(driver);
		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
	}

	@Test(groups = { "regression",
			"dailyrun" }, description = "SQ-T2671 Validate Negative points in account history of guest")
	@Owner(name = "Hardik Bhardwaj")
	public void T2671_ValidateNegativePointsInAccountHistoryOfGuest() throws InterruptedException {
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		timeStamp = CreateDateTime.getTimeDateString();
		// login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// create migrated guest
		// pageObj.menupage().clickGuestMenu();
		// pageObj.menupage().awaitingMigrationLink();
		pageObj.menupage().navigateToSubMenuItem("Guests", "Awaiting Migration");
		pageObj.awaitingMigrationPage().createNewMigrationGuest(userEmail, timeStamp, dataSet.get("location"),
				dataSet.get("gender"));
		boolean flag = pageObj.awaitingMigrationPage().verifyMigrationUser(userEmail, timeStamp);
		Assert.assertTrue(flag, "Migration user is not created successfully");
		// Signup by API 2
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("API2 Signup is successful");
		// navigate to migrated guest
		boolean guestDisplayed = pageObj.awaitingMigrationPage().clickawaitingMigrationGuestNameLink(userEmail);
		Assert.assertTrue(guestDisplayed, "Guest is not displayed");
		logger.error("searched awaiting Migration Guest Name is not present");
		TestListeners.extentTest.get().info("searched awaiting Migration Guest Name is not present");
//		pageObj.awaitingMigrationPage().clickawaitingMigrationGuestNameLink();
		boolean result = pageObj.awaitingMigrationPage().checkAccountHistory();
		Assert.assertTrue(result, "original points is not 30");
		// Verify Account history of guest after force redemption
		pageObj.guestTimelinePage().navigateInsideGuestTimeline("Force Redemption");
		pageObj.forceredemptionPage().forceRedemptionOfPoints(dataSet.get("comment"),
				"Unbanked Points/Checkins to Redeem", "130");
		pageObj.forceredemptionPage().verifyErrorInForceRedemption(
				"Error creating the forced redemption: Not enough reward balance available to redeem");
		pageObj.forceredemptionPage().forceRedemptionOfPoints(dataSet.get("comment"),
				"Unbanked Points/Checkins to Redeem", "15");
		pageObj.forceredemptionPage().verifyForceRedemption("Forced redemption created");
		boolean result1 = pageObj.awaitingMigrationPage().checkPointsAfterForceRedemption("15");
		Assert.assertTrue(result1, "Force Redemption points is not 15");
		// Verify Account history of guest after doing checkin
		// Pos api checkin
		String key = CreateDateTime.getTimeDateString();
		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T10:50:00+05:30";
		Response resp = pageObj.endpoints().posCheckin(date, userEmail, key, txn, dataSet.get("locationkey"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp.getStatusCode(), "Status code 200 did not matched for pos chekin api");
		Assert.assertEquals(resp.jsonPath().get("email").toString(), userEmail.toLowerCase());
		// verify checkin points
		boolean result2 = pageObj.awaitingMigrationPage().checkPointsAfterCheckIn();
		Assert.assertTrue(result2, "checkin points is not 20");
		// Verify Account history of guest after doing redemption
		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				dataSet.get("redeemable_id"));
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send message to user");
		utils.logPass("Api2 send reward reedemable to user is successful");
		utils.longWaitInSeconds(2);
		Response resp1 = pageObj.endpoints().posRedemptionOfRedeemable(userEmail, date, key, txn,
				dataSet.get("locationkey"), dataSet.get("redeemable_id"), "110011");
		Assert.assertEquals(resp1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for the POS Redemption API");
		utils.logPass("Redemption using POS redemption is successful");

		// verify redemption is successful
		boolean result3 = pageObj.awaitingMigrationPage().verifyRedemption();
		Assert.assertTrue(result3, "Redemption is not successful");
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
