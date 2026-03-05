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
import com.punchh.server.apiConfig.ApiResponseJsonSchema;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class EcrmSignUpTest {
	static Logger logger = LogManager.getLogger(EcrmSignUpTest.class);
	public WebDriver driver;
	private String userEmail;
	private PageObj pageObj;
	private String baseUrl;
	private String sTCName;
	private static Map<String, String> dataSet;
	private String env, run = "ui";

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

	@Test(description = "SQ-T2272, Validate Signup in ecrm"
			+ "SQ-T2280, [ECRM>CHECKIN] Perform ECRM Checkins in e-club user", groups = { "regression",
					"dailyrun" }, priority = 0)
	@Owner(name = "Ashwini Shetty")
	public void T2280_ecrmCheckin() throws InterruptedException {
		logger.info("== ECRM Signup test ==");
		pageObj.iframeSingUpPage().navigateToEcrm(baseUrl + dataSet.get("ecrmUrl"));
		userEmail = pageObj.iframeSingUpPage().ecrmSignUp(dataSet.get("location"));
		// Verify eclub user on timeline
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */

		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		Assert.assertTrue(pageObj.guestTimelinePage().verifyEclubGuestOnGuestTimeline(userEmail),
				"Error in verifying guest time line ");

		String key = CreateDateTime.getTimeDateString();
		String txn_no = "123456" + CreateDateTime.getTimeDateString();
		Response response = pageObj.endpoints().ecrmPosCheckin(userEmail, dataSet.get("locationKey"), key, txn_no);
		pageObj.apiUtils().verifyResponse(response, "ECRM checkin");
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "ECRM checkin failure");
		boolean isPosCreateTransactionValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.posCreateTransactionSchema, response.asString());
		Assert.assertTrue(isPosCreateTransactionValidated, "POS create transaction schema validation failed");
		pageObj.guestTimelinePage().refreshTimeline();
		Assert.assertTrue(pageObj.guestTimelinePage().verifyEcrmTransaction(key, txn_no),
				"Failed to verify ECRM checkin on guest timeline");
	}

	@Test(description = "SQ-T3072, [ECRM>CHECKIN] Perform ECRM Checkins in e-club user negative flow", groups = {
			"regression", "dailyrun" }, priority = 1)
	@Owner(name = "Ashwini Shetty")
	public void T3072_ecrmCheckinnegative() throws InterruptedException {
		String invaliduserEmail = "testabc";
		logger.info("== ECRM Signup test ==");
		pageObj.iframeSingUpPage().navigateToEcrm(baseUrl + dataSet.get("ecrmUrl"));
		userEmail = pageObj.iframeSingUpPage().ecrmSignUp(dataSet.get("location"));
		// Verify eclub user on timeline
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */

		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		Assert.assertTrue(pageObj.guestTimelinePage().verifyEclubGuestOnGuestTimeline(userEmail),
				"Error in verifying guest time line ");

		// create transaction with invalid location key
		String key = CreateDateTime.getTimeDateString();
		String txn_no = "123456" + CreateDateTime.getTimeDateString();
		Response response = pageObj.endpoints().ecrmPosCheckin(userEmail, dataSet.get("invalidlocationKey"), key,
				txn_no);
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED, "ECRM checkin failure");
		boolean isPosCreateTransactionInvalidLocationKeySchemaValidated = Utilities
				.validateJsonArrayAgainstSchema(ApiResponseJsonSchema.apiStringArraySchema, response.asString());
		Assert.assertTrue(isPosCreateTransactionInvalidLocationKeySchemaValidated,
				"POS create transaction schema validation failed");
		TestListeners.extentTest.get().pass("Verified create transaction with invalid location key");

		// create transaction with invalid mail id
		String key1 = CreateDateTime.getTimeDateString();
		String txn_no1 = "123456" + CreateDateTime.getTimeDateString();
		Response response1 = pageObj.endpoints().ecrmPosCheckin(invaliduserEmail, dataSet.get("locationKey"), key1,
				txn_no1);
		Assert.assertEquals(response1.getStatusCode(), ApiConstants.HTTP_STATUS_NOT_FOUND, "ECRM checkin failure");
		boolean isPosCreateTransactionInvalidEmailSchemaValidated = Utilities
				.validateJsonArrayAgainstSchema(ApiResponseJsonSchema.apiStringArraySchema, response1.asString());
		Assert.assertTrue(isPosCreateTransactionInvalidEmailSchemaValidated,
				"POS create transaction schema validation failed");
		TestListeners.extentTest.get().pass("Verified create transaction with invalid email");
	}

	@Test(description = "SQ-T6980,SQ-T6981 Verify Dybamic Reward creation when  Conditional Gifting toggle is ON/OFF", groups = {
			"regression" }, priority = 2)
	@Owner(name = "Amit Kumar")
	public void T6980_verifyDybamicRewardCreationWhenConditionalGiftingToggleIsOnOFF() throws InterruptedException {

		// Instance select business
		String dynamicRewardSetName = "AutoDynamicReward" + CreateDateTime.getTimeDateString();
//		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Loyalty Program", "Dynamic Rewards");

		pageObj.dynamicRewardsPage().createDynamicRewardSetName(dynamicRewardSetName);
		// set surprise set with points
		pageObj.dynamicRewardsPage().enterDynamicRewardSurpriseSetWithPoints(dataSet.get("weight"),
				dataSet.get("rewardType"), dataSet.get("points"));

		// set surprise set with redeemable
		pageObj.dynamicRewardsPage().enterDynamicRewardSurpriseSetWithRedeemable(dataSet.get("weight1"),
				dataSet.get("rewardType1"), dataSet.get("redeemable"));
		String successMsg = pageObj.dynamicRewardsPage().saveDynamicRewardSet();
		Assert.assertTrue(successMsg.contains("Dynamic Reward created."));

		// delete dynamic reward set
		String deleteMsg = pageObj.dynamicRewardsPage().deleteDynamicRewardSet(dynamicRewardSetName);
		Assert.assertTrue(deleteMsg.contains("Dynamic Reward destroyed."));

		// SQ-T6981
		String dynamicRewardSetName1 = "AutoDynamicReward" + CreateDateTime.getTimeDateString();
		pageObj.dynamicRewardsPage().createDynamicRewardSetName(dynamicRewardSetName1);
		pageObj.dynamicRewardsPage().setConditionalGifingOn();

		// set Gifting rule with points
		pageObj.dynamicRewardsPage().enterDynamicRewardGiftingRuletWithPoints(dataSet.get("segment"), dataSet.get("qc"),
				dataSet.get("rewardType"), dataSet.get("points"));

		// set Gifting rule with redeemable
		pageObj.dynamicRewardsPage().enterDynamicRewardGiftingWithRedeemable(dataSet.get("segment1"),
				dataSet.get("qc1"), dataSet.get("rewardType1"), dataSet.get("redeemable"));
		pageObj.dynamicRewardsPage().setDefaultReward(dataSet.get("defaultRewardType"));

		String successMsg1 = pageObj.dynamicRewardsPage().saveDynamicRewardSet();
		Assert.assertTrue(successMsg1.contains("Dynamic Reward created."));

		// delete dynamic reward set
		String deleteMsg1 = pageObj.dynamicRewardsPage().deleteDynamicRewardSet(dynamicRewardSetName1);
		Assert.assertTrue(deleteMsg1.contains("Dynamic Reward destroyed."));

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
