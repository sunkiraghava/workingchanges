package com.punchh.server.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
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
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.TestListeners;

import io.restassured.response.Response;

@Listeners(TestListeners.class)

public class RedemptionUsingUUIDTest {
	private static Logger logger = LogManager.getLogger(RedemptionUsingUUIDTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String userEmail;
	private String sTCName;
	private String baseUrl;
	String blankString = "";
	private String env, run = "ui";
	private static Map<String, String> dataSet;
	String rewardId = "";
	String rewardId1 = "";
	String rewardId2 = "";
	Properties prop;

	@BeforeClass(alwaysRun = true)
	public void openBrowser() {
		driver = new BrowserUtilities().launchBrowser();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		// Single Login to instance
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
		// Move to All businesses page
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
	}

	@Test(description = "SQ-T4104 Verify uuid redemption for discount_amount using api/pos/redemptions API when business has Redemption Code Strategy -> UUID selected", groups = {
			"regression", "dailyrun" }, priority = 0)
	@Owner(name = "Hardik Bhardwaj")
	public void T4104_UuidRedemption_discount_amount() throws Exception {

		// login to instance
		//pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		//pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Codes");
		pageObj.cockpitRedemptionsPage().setRedemptionCodeStrategy("UUID");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_make_uuid_for_online_redemption_code", "uncheck");
		pageObj.settingsPage().clickUpdateBtn();

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("API2 Signup is successful");
		logger.info("API2 Signup is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "200", "",
				"", "200");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		logger.info("Send amount to the user successfully");
		pageObj.utils().logPass("Send amount to the user successfully");

		// generate redemption code using mobile api
		Response redemption_codeResponse = pageObj.endpoints().Api1MobileRedemptionRedeemed_Points(token, "1",
				dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(redemption_codeResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		String redemption_Code = redemption_codeResponse.jsonPath().get("internal_tracking_code").toString();
		logger.info("redemption code => " + redemption_Code);

		// Redemption using Pos redemption api
		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key = CreateDateTime.getTimeDateString();
		Response resp = pageObj.endpoints().posRedemptionOfCode(userEmail, date, redemption_Code, key, txn,
				dataSet.get("locationKey"));
		Assert.assertEquals(resp.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for pos redemption api");

	}

	@Test(description = "SQ-T4103 Verify uuid redemption for reward using api/auth/redemptions/online_order API when business has Redemption Code Strategy -> UUID selected", groups = {
			"regression", "dailyrun" }, priority = 1)
	@Owner(name = "Hardik Bhardwaj")
	public void T4103_UuidRedemptionReward() throws Exception {

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("API2 Signup is successful");
		logger.info("API2 Signup is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				dataSet.get("redeemable_id"), "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		logger.info("Send Redeemable to the user successfully");
		pageObj.utils().logPass("Send Redeemable to the user successfully");

		// get reward id
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id"));
		logger.info("Reward id " + rewardId + " is generated successfully ");
		pageObj.utils().logPass("Reward id " + rewardId + " is generated successfully ");

		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();
		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Pizza1", "1", "1", "M", "10", "999", "1",
				dataSet.get("item_id"));
		parentMap.put("Pizza1", detailsMap1);

		// generate redemption code using mobile api
		Response redemption_codeResponse = pageObj.endpoints().Api1MobileRedemptionReward_id(token, rewardId,
				dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(redemption_codeResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		String redemption_Code = redemption_codeResponse.jsonPath().get("internal_tracking_code").toString();
		logger.info("redemption code => " + redemption_Code);

		Response redemptionResponse = pageObj.endpoints().onlineOrderWithOrderModeAndClientPlatformTest(
				dataSet.get("client"), dataSet.get("secret"), "", token, "redemption_code", "redemption_code",
				redemption_Code, "", "", "15", parentMap);
		Assert.assertEquals(redemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

	}

	// Author - Hardik
	@Test(description = "SQ-T4103 Verify uuid redemption for reward using api/auth/redemptions/online_order API when business has Redemption Code Strategy -> UUID selected", groups = {
			"regression", "dailyrun" }, priority = 2)
	@Owner(name = "Hardik Bhardwaj")
	public void T4105_UuidRedemptionVoid() throws Exception {

		//pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		//pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Codes");
		pageObj.cockpitRedemptionsPage().setRedemptionCodeStrategy("UUID");
		pageObj.settingsPage().clickUpdateBtn();

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("API2 Signup is successful");
		logger.info("API2 Signup is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				dataSet.get("redeemable_id"), "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		logger.info("Send Redeemable to the user successfully");
		pageObj.utils().logPass("Send Redeemable to the user successfully");

		// get reward id
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id"));

		logger.info("Reward id " + rewardId + " is generated successfully ");
		pageObj.utils().logPass("Reward id " + rewardId + " is generated successfully ");

		// send reward amount to user Reedemable
		Response sendRewardResponse1 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				dataSet.get("redeemable_id1"), "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse1.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		logger.info("Send Redeemable to the user successfully");
		pageObj.utils().logPass("Send Redeemable to the user successfully");

		// get reward id
		String rewardId1 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id1"));

		logger.info("Reward id " + rewardId1 + " is generated successfully ");
		pageObj.utils().logPass("Reward id " + rewardId1 + " is generated successfully ");

		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();
		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Pizza1", "1", "1", "M", "10", "999", "1",
				dataSet.get("item_id"));
		parentMap.put("Pizza1", detailsMap1);

		// generate redemption code using mobile api
		Response redemption_codeResponse = pageObj.endpoints().Api1MobileRedemptionReward_id(token, rewardId,
				dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(redemption_codeResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		String redemption_Code = redemption_codeResponse.jsonPath().get("internal_tracking_code").toString();
		logger.info("redemption code 1 => " + redemption_Code);

		// generate redemption code using mobile api
		Response redemption_codeResponse1 = pageObj.endpoints().Api1MobileRedemptionReward_id(token, rewardId1,
				dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(redemption_codeResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		String redemption_Code1 = redemption_codeResponse1.jsonPath().get("internal_tracking_code").toString();
		logger.info("redemption code 2 => " + redemption_Code1);

		// navigate to user timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		boolean title = pageObj.guestTimelinePage().verifyTitleFromTimeline("Redeemable Redemption");
		Assert.assertTrue(title, "Void Honored Redemption Title did not displayed...");
		pageObj.utils().logPass("Void Honored Redemption Title is displayed successfully on timeline");

		// Void redemption using api
		Response respo = pageObj.endpoints().posVoidRedemptionAPI(userEmail, "redemption_code", redemption_Code,
				dataSet.get("locationKey"), "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_ACCEPTED, respo.getStatusCode(), "Status code 202 did not matched for pos redemption api");

		// Void redemption using api
		String txn = "123456" + CreateDateTime.getTimeDateString();
		Response respo1 = pageObj.endpoints().posVoidRedemptionAPI(userEmail, "redemption_code", redemption_Code1,
				dataSet.get("locationKey"), txn);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_ACCEPTED, respo1.getStatusCode(), "Status code 202 did not matched for pos redemption api");

		boolean title2 = pageObj.guestTimelinePage().verifyTitleFromTimeline("Void Honored Redemption");
		try {
			Assert.assertTrue(title2, "Void Honored Redemption Title did not displayed...");
			pageObj.utils().logPass("Void Honored Redemption Title is displayed successfully on timeline");
		} catch (Exception e) {
			logger.error("Error in validating Void Honored Redemption Title on timeline" + e);
			TestListeners.extentTest.get().fail("Error in validating Void Honored Redemption Title on timeline" + e);
		}

		List<Object> obj = new ArrayList<Object>();
		String description;
		int j = 0;
		// User Account History
		Response accountHistoryResponse = pageObj.endpoints().Api2UserAccountHistory(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, accountHistoryResponse.getStatusCode(),
				"Status code 200 did not matched for api2 point conversion");
		obj = accountHistoryResponse.jsonPath().getList("description");
		for (int i = 0; i < obj.size(); i++) {
			description = accountHistoryResponse.jsonPath().getString("[" + i + "].description");
			if (description.contains(dataSet.get("redeemableName"))) {
				i = j;
				break;
			}
		}

		String eventValue = accountHistoryResponse.jsonPath().get("[" + j + "].event_value").toString();
		Assert.assertEquals(eventValue, "+Item", "reward is not reverted back to user account (Account History)");
		logger.info("reward is reverted back to user account (Account History)");
		pageObj.utils().logPass("reward is reverted back to user account (Account History)");

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Codes");
		pageObj.cockpitRedemptionsPage().setRedemptionCodeStrategy("7 Digits");
		pageObj.settingsPage().clickUpdateBtn();

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