package com.punchh.server.OMMTest;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;

import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class DiscountBasketAutoUnlockWorkerTest {
	private static Logger logger = LogManager.getLogger(DiscountBasketAutoUnlockWorkerTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String sTCName;
	private String baseUrl;
	String blankString = "";
	private String env, run = "ui";
	private static Map<String, String> dataSet;
	Utilities utils;

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {

		driver = new BrowserUtilities().launchBrowser();
		sTCName = method.getName();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run , env), sTCName);
		dataSet = pageObj.readData().readTestData;
//		TestData.ReadDataFromJsonFileForClientSecretKey(TestData.getJsonFilePath(run , env , "Secrets"),
//				dataSet.get("slug"));
//		dataSet.putAll(TestData.readTestData);
		logger.info(sTCName + " ==>" + dataSet);
		utils = new Utilities(driver);
	}

	// Deprecated https://punchhdev.atlassian.net/browse/OMM-1256
//	@Test(description = "SQ-T3738 Verify discount basket will unlock automatically (via Sidekiq DiscountBasketAutoUnlockWorker) when Enable Auto Unlock flag is enable || "
//			+ "SQ-T3739 Verify whether discount basket will unlock automatically or not when Enable Auto Unlock flag is disable", dataProvider = "TestDataProvider", groups = { "regression", "dailyrun" }, priority = 0)
	public void T3738_DiscountBasketAutoUnlockWorker(String flag1, String flag2, String flag3, String flag4,
			String tCase) throws InterruptedException {
		utils.logit("-------- " + tCase + " ---------");

		String client = dataSet.get("client");
		String secret = dataSet.get("secret");
		utils.logPass("Client = " + client + " Secret = " + secret + " for " + tCase);

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Navigate to Multiple Redemption Tab and Uncheck the flags
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_discount_locking", flag1);
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_auto_unlock", flag2);
		pageObj.dashboardpage().updateCheckBox();

		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.cockpitRedemptionsPage().setAutoUnlockPeriod(flag3, "10");
		pageObj.dashboardpage().updateCheckBox();

		// check locked account tab in Guest tab
		pageObj.menupage().navigateToSubMenuItem("Guests", "Guests");
		boolean result = pageObj.guestTimelinePage().lockedAccountTab(6);
		Assert.assertTrue(result, "Locked Account Tab is visible in Guest section");
		utils.logPass("Locked Account Tab is visible in Guest section");

		// User SignUp using API
		long phone = (long) (Math.random() * Math.pow(10, 10));
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, client, secret, phone);
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("API2 Signup is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				dataSet.get("redeemable_id"), "", "");

		utils.logPass("Send redeemable to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		String redeemableID = dataSet.get("redeemable_id");

		// get reward id
		String rewardId = pageObj.redeemablesPage().getRewardId(token, client, secret, redeemableID);

		utils.logPass("Reward id " + rewardId + " is generated successfully ");

		String externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		// AUTH Add Discount to Basket
		Response discountBasketResponse5 = pageObj.endpoints().addDiscountToBasketAUTH(token, client, secret, "reward",
				rewardId, externalUID);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponse5.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		utils.logPass("AUTH add discount to basket is successful");
		pageObj.guestTimelinePage().navigateToLockedAccountTab();

		// search user in locked account by email
		boolean status = pageObj.guestTimelinePage().searchLockedAccountTab(userEmail, 2);
		Assert.assertTrue(status, "Error in searching email in locked account");
		utils.logPass("Email search in locked account is successful");

		utils.switchToWindowN(1);

		// SideKiq schedules running
		pageObj.cockpitRedemptionsPage().runSidekiqJob(baseUrl, "DiscountBasketAutoUnlockWorker"); // Timeline
																									// validation
		utils.switchToParentWindow();

		// search user in locked account by email
		boolean status1 = pageObj.guestTimelinePage().searchUserInLockedAccountTab(userEmail, 2);
		pageObj.cockpitRedemptionsPage().checkAssert(flag4, status1);

	}

	@DataProvider(name = "TestDataProvider")
	public Object[][] testDataProvider() {

		return new Object[][] {
				// {"flag1","flag2","flag3","flag4","testCaseName"},
				{ "check", "check", "present", "false", " Auto Unlock flag is enable" },
				{ "check", "uncheck", "not present", "true", " Auto Unlock flag is disable" }, 
				};

	}

	// Deprecated https://punchhdev.atlassian.net/browse/OMM-1256
//	@Test(description = "SQ-T3740 Verify whether discount basket will unlock automatically or not when DiscountBasketAutoUnlockWorker worker is Deleted from Sidekiq in between Auto-Unlock Period ", groups = { "regression", "dailyrun" }, priority = 1)
	public void T3740_DiscountBasketAutoUnlockWorker() throws InterruptedException {

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Navigate to Multiple Redemption Tab and Uncheck the flags
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_discount_locking", "check");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_auto_unlock", "check");
		pageObj.dashboardpage().updateCheckBox();

		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().setAutoUnlockPeriod("present", "10");

		// check locked account tab in Guest tab
		pageObj.menupage().navigateToSubMenuItem("Guests", "Guests");
		boolean result = pageObj.guestTimelinePage().lockedAccountTab(6);
		Assert.assertTrue(result, "Locked Account Tab is visible in Guest section");
		utils.logPass("Locked Account Tab is visible in Guest section");

		// User SignUp using API
		long phone = (long) (Math.random() * Math.pow(10, 10));
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"), phone);
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("API2 Signup is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				dataSet.get("redeemable_id"), "", "");

		utils.logPass("Send redeemable to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		String redeemableID = dataSet.get("redeemable_id");

		// get reward id
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemableID);

		utils.logPass("Reward id " + rewardId + " is generated successfully ");

		String externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		// AUTH Add Discount to Basket
		Response discountBasketResponse5 = pageObj.endpoints().addDiscountToBasketAUTH(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId, externalUID);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponse5.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		utils.logPass("AUTH add discount to basket is successful");
		pageObj.guestTimelinePage().navigateToLockedAccountTab();

		// search user in locked account by email
		boolean status = pageObj.guestTimelinePage().searchLockedAccountTab(userEmail, 2);
		Assert.assertTrue(status, "Error in searching email in locked account");
		utils.logPass("Email search in locked account is successful");

		((JavascriptExecutor) driver).executeScript("window.open()");
		ArrayList<String> tabs = new ArrayList<String>(driver.getWindowHandles());
		driver.switchTo().window(tabs.get(1));

		// SideKiq schedules running
		pageObj.cockpitRedemptionsPage().deleteSidekiqJob(baseUrl, "DiscountBasketAutoUnlockWorker"); // Timeline
																										// validation
		driver.switchTo().window(tabs.get(0));

		// search user in locked account by email
		boolean status1 = pageObj.guestTimelinePage().searchUserInLockedAccountTab(userEmail, 2);
		pageObj.cockpitRedemptionsPage().checkAssert("true", status1);

	}

	// Deprecated https://punchhdev.atlassian.net/browse/OMM-1256
//	@Test(description = "SQ-T3741 Verify whether discount basket will unlock automatically or not when discount basket is unlocked from Locked Account Tab in after DiscountBasketAutoUnlockWorker gets created", groups = { "regression", "dailyrun" }, priority = 2)
	public void T3741_DiscountBasketAutoUnlockWorker() throws InterruptedException {

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Navigate to Multiple Redemption Tab and Uncheck the flags
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_discount_locking", "check");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_auto_unlock", "check");
		pageObj.dashboardpage().updateCheckBox();

		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().setAutoUnlockPeriod("present", "10");

		((JavascriptExecutor) driver).executeScript("window.open()");
		ArrayList<String> tabs = new ArrayList<String>(driver.getWindowHandles());
		driver.switchTo().window(tabs.get(1));

		// SideKiq schedules running
		pageObj.cockpitRedemptionsPage().deleteSidekiqJob(baseUrl, "DiscountBasketAutoUnlockWorker"); // Timeline
																										// validation
		driver.switchTo().window(tabs.get(0));

		// check locked account tab in Guest tab
		pageObj.menupage().navigateToSubMenuItem("Guests", "Guests");
		boolean result = pageObj.guestTimelinePage().lockedAccountTab(6);
		Assert.assertTrue(result, "Locked Account Tab is visible in Guest section");
		utils.logPass("Locked Account Tab is visible in Guest section");

		// User SignUp using API
		long phone = (long) (Math.random() * Math.pow(10, 10));
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"), phone);
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("API2 Signup is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				dataSet.get("redeemable_id"), "", "");

		utils.logPass("Send redeemable to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		String redeemableID = dataSet.get("redeemable_id");

		// get reward id
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemableID);

		utils.logPass("Reward id " + rewardId + " is generated successfully ");

		String externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		// AUTH Add Discount to Basket
		Response discountBasketResponse5 = pageObj.endpoints().addDiscountToBasketAUTH(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId, externalUID);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponse5.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		utils.logPass("AUTH add discount to basket is successful");
		pageObj.guestTimelinePage().navigateToLockedAccountTab();

		// search user in locked account by email and accept the unlock basket request
		pageObj.guestTimelinePage().unlockOrCancelLockedAccount(userEmail, "ok");
		boolean result2 = pageObj.guestTimelinePage()
				.successOrErrorConfirmationMessage("Guest Account has been unlocked successfully.");
		Assert.assertTrue(result2, "Given Message does not matches with the displayed message");
		utils.logPass("Given Message matches with the displayed message");

		driver.switchTo().window(tabs.get(1));

		// SideKiq schedules running
		String discountBasketAutoUnlockWorkerList = pageObj.cockpitRedemptionsPage().checkSidekiqJob(baseUrl,
				"DiscountBasketAutoUnlockWorker");
		driver.switchTo().window(tabs.get(0));
		Assert.assertNotEquals("0", discountBasketAutoUnlockWorkerList,
				"DiscountBasketAutoUnlockWorker is not present in the for the basket");
		utils.logPass("DiscountBasketAutoUnlockWorker is present in Sidekiq for the basket");

		// search user in locked account by email
		boolean status1 = pageObj.guestTimelinePage().searchUserInLockedAccountTab(userEmail, 2);
		pageObj.cockpitRedemptionsPage().checkAssert("false", status1);
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}

}
