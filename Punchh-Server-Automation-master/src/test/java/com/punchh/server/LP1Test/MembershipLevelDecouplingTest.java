package com.punchh.server.LP1Test;

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
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class MembershipLevelDecouplingTest {
	private static Logger logger = LogManager.getLogger(MembershipLevelDecouplingTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String userEmail;
	private String sTCName;
	private String baseUrl;
	String blankString = "";
	private String env, run = "ui";
	private static Map<String, String> dataSet;
	Properties prop;
	Utilities utils;
	String externalUID;

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

		prop = Utilities.loadPropertiesFile("config.properties");
		sTCName = method.getName();
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==>" + dataSet);
		utils = new Utilities(driver);
	}

	@Test(description = "SQ-T4488 [Point to Currency]Verify all types of redemptions and void them also", groups = {
			"regression", "dailyrun" }, priority = 0)
	@Owner(name = "Hardik Bhardwaj")
	public void T4488_typesOfRedemptions() throws Exception {

//		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
//		pageObj.instanceDashboardPage().loginToInstance();

		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_subscriptions", "check");
		pageObj.dashboardpage().clickOnUpdateButton();

		String spPrice = Integer.toString(Utilities.getRandomNoFromRange(300, 500));
		String endDateTime = CreateDateTime.getFutureDate(1) + " 10:00 PM";
		String PlanID = dataSet.get("PlanID");

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
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "200",
				dataSet.get("reedemable_id"), blankString, blankString);
		logger.info("Send point to the user successfully");
		pageObj.utils().logit("Send Reedemable to the user successfully");
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send message to user");
		pageObj.utils().logit("Api2  send reward amount to user is successful");

		// get reward id
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("reedemable_id"));

		logger.info("Reward id " + rewardId + " is generated successfully ");
		pageObj.utils().logPass("Reward id " + rewardId + " is generated successfully ");

		// subscription purchase api 2
		Response purchaseSubscriptionresponse2 = pageObj.endpoints().Api2SubscriptionPurchaseAutorenewal(token, PlanID,
				dataSet.get("client"), dataSet.get("secret"), spPrice, endDateTime, "false");
		Assert.assertEquals(purchaseSubscriptionresponse2.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for subscription purchase api 2");
		logger.info("User will be able to purchase the subscription plan successfully using api2");
		TestListeners.extentTest.get()
				.info("User will be able to purchase the subscription plan successfully using api2");
		String subscriptionId = purchaseSubscriptionresponse2.jsonPath().get("subscription_id").toString();

		// Pos redemption api for Amount
		String key = CreateDateTime.getTimeDateString();
		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T10:50:00+05:30";
		Response posRedemptionOfAmountRespo = pageObj.endpoints().posRedemptionOfAmount(userEmail, date, key, txn,
				dataSet.get("redeemAmount"), dataSet.get("locationkey"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, posRedemptionOfAmountRespo.getStatusCode(),
				"Status code 200 did not matched for pos redemption api");
		String redemptionCodeAmount = posRedemptionOfAmountRespo.jsonPath().get("redemption_id").toString();
		logger.info("Pos redemption api for Amount is successful with redemption code " + redemptionCodeAmount);
		TestListeners.extentTest.get()
				.pass("Pos redemption api for Amount is successful with redemption code " + redemptionCodeAmount);

//		utils.longwait(7000);
		// Void redemption api for amount
		Response posVoidRedemptionAmtRespo = pageObj.endpoints().posVoidRedemptionPolling(userEmail,
				redemptionCodeAmount, dataSet.get("locationkey"));
		Assert.assertEquals(posVoidRedemptionAmtRespo.getStatusCode(), ApiConstants.HTTP_STATUS_ACCEPTED,
				"Status code 200 did not matched for pos redemption api");
		logger.info("Pos Void redemption api for amount is successful with redemption code " + redemptionCodeAmount);
		TestListeners.extentTest.get()
				.pass("Pos Void redemption api for amount is successful with redemption code " + redemptionCodeAmount);

		// pos redemption API for Reward
		int posRedemptionOfRewardRespo_attempts = 0;
		Response posRedemptionOfRewardRespo = null;
		while (posRedemptionOfRewardRespo_attempts < 10) {
			Utilities.longWait(1500);
			posRedemptionOfRewardRespo = pageObj.endpoints().posRedemptionOfReward(userEmail,
					dataSet.get("locationkey"), rewardId);
			if (posRedemptionOfRewardRespo.getStatusCode() == ApiConstants.HTTP_STATUS_OK) {
				break;
			}
			logger.info("Attempt no for pos redemption API for Reward is " + posRedemptionOfRewardRespo_attempts);
			posRedemptionOfRewardRespo_attempts++;
		}
		Assert.assertEquals(posRedemptionOfRewardRespo.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for pos redemption api");
		String redemptionCodeReward = posRedemptionOfRewardRespo.jsonPath().get("redemption_id").toString();
		logger.info("pos redemption API for Reward is successful with redemption code " + redemptionCodeReward);
		TestListeners.extentTest.get()
				.pass("pos redemption API for Reward is successful with redemption code " + redemptionCodeReward);

//		utils.longwait(7000);
		// Void redemption api for Reward
		Response posVoidRedemptionRewardRespo = pageObj.endpoints().posVoidRedemptionPolling(userEmail,
				redemptionCodeReward, dataSet.get("locationkey"));
		Assert.assertEquals(posVoidRedemptionRewardRespo.getStatusCode(), ApiConstants.HTTP_STATUS_ACCEPTED,
				"Status code 200 did not matched for pos redemption api");
		logger.info("Pos Void redemption api for Reward is successful with redemption code " + redemptionCodeReward);
		TestListeners.extentTest.get()
				.pass("Pos Void redemption api for Reward is successful with redemption code " + redemptionCodeReward);

		// Pos redemption api for Redeemable
		String txn1 = "123456" + CreateDateTime.getTimeDateString();
		String date1 = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key1 = CreateDateTime.getTimeDateString();
		int posRedemptionOfRedeemableResp_attempts = 0;
		Response posRedemptionOfRedeemableResp = null;
		while (posRedemptionOfRedeemableResp_attempts < 10) {
			Utilities.longWait(1500);
			posRedemptionOfRedeemableResp = pageObj.endpoints().posRedemptionOfRedeemable(userEmail, date1, key1, txn1,
					dataSet.get("locationkey"), dataSet.get("reedemable_id"), dataSet.get("item_id"));
			if (posRedemptionOfRedeemableResp.getStatusCode() == ApiConstants.HTTP_STATUS_OK) {
				break;
			}
			logger.info(
					"Attempt no for Pos redemption api for Redeemable is " + posRedemptionOfRedeemableResp_attempts);
			posRedemptionOfRedeemableResp_attempts++;
		}
		Assert.assertEquals(posRedemptionOfRedeemableResp.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for pos redemption api");
		String redemptionCodeRedeemable = posRedemptionOfRedeemableResp.jsonPath().get("redemption_id").toString();
		logger.info("Pos redemption api for Redeemable is successful with redemption code " + redemptionCodeRedeemable);
		pageObj.utils().logPass(
				"Pos redemption api for Redeemable is successful with redemption code " + redemptionCodeRedeemable);

//		utils.longwait(7000);
		// Void redemption api for Redeemable
		Response posVoidRedemptionRedeemableRespo = pageObj.endpoints().posVoidRedemptionPolling(userEmail,
				redemptionCodeRedeemable, dataSet.get("locationkey"));
		Assert.assertEquals(posVoidRedemptionRedeemableRespo.getStatusCode(), ApiConstants.HTTP_STATUS_ACCEPTED,
				"Status code 200 did not matched for pos redemption api");
		logger.info("Pos Void redemption api for Redeemable is successful with redemption code "
				+ redemptionCodeRedeemable);
		pageObj.utils().logPass("Pos Void redemption api for Redeemable is successful with redemption code "
				+ redemptionCodeRedeemable);

//		utils.longWaitInSeconds(7);
		// generate redemption code using mobile api
		int redemption_codeResponse_attempts = 0;
		Response redemption_codeResponse = null;
		while (redemption_codeResponse_attempts < 10) {
			Utilities.longWait(1500);
			redemption_codeResponse = pageObj.endpoints().Api1MobileRedemptionRedeemed_Points(token, "11",
					dataSet.get("client"), dataSet.get("secret"));
			if (redemption_codeResponse.getStatusCode() == ApiConstants.HTTP_STATUS_CREATED) {
				break;
			}
			logger.info(
					"Attempt no for generate redemption code using mobile api is " + redemption_codeResponse_attempts);
			redemption_codeResponse_attempts++;
		}
		Assert.assertEquals(redemption_codeResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		String redemption_Code = redemption_codeResponse.jsonPath().get("internal_tracking_code").toString();
		logger.info("generate redemption code using mobile api is successful with redemption code " + redemption_Code);
		pageObj.utils().logPass(
				"generate redemption code using mobile api is successful with redemption code " + redemption_Code);

		// Pos redemption api for Redemption code
		String txn2 = "123456" + CreateDateTime.getTimeDateString();
		String date2 = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key2 = CreateDateTime.getTimeDateString();
		int posRedemptionOfRedemptionCode_attempts = 0;
		Response posRedemptionOfRedemptionCodeResp = null;
		while (posRedemptionOfRedemptionCode_attempts < 10) {
			Utilities.longWait(1500);
			posRedemptionOfRedemptionCodeResp = pageObj.endpoints().posRedemptionOfCouponCode(userEmail, date2,
					redemption_Code, key2, txn2, dataSet.get("locationkey"));
			if (posRedemptionOfRedemptionCodeResp.getStatusCode() == ApiConstants.HTTP_STATUS_OK) {
				break;
			}
			logger.info("Attempt no for Pos redemption api for Redemption code is "
					+ posRedemptionOfRedemptionCode_attempts);
			posRedemptionOfRedemptionCode_attempts++;
		}
		Assert.assertEquals(posRedemptionOfRedemptionCodeResp.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for pos redemption api");
		String redemptionCodeRedemptionCode = posRedemptionOfRedemptionCodeResp.jsonPath().get("redemption_id")
				.toString();
		logger.info("Pos redemption api for Redemption code is successful with redemption code "
				+ redemptionCodeRedemptionCode);
		pageObj.utils().logPass("Pos redemption api for Redemption code is successful with redemption code "
				+ redemptionCodeRedemptionCode);

//		utils.longwait(7000);
		// Void redemption api for Redemption code
		Response posVoidRedemptionRedemptionCodeRespo = pageObj.endpoints().posVoidRedemptionPolling(userEmail,
				redemptionCodeRedemptionCode, dataSet.get("locationkey"));
		Assert.assertEquals(posVoidRedemptionRedemptionCodeRespo.getStatusCode(), ApiConstants.HTTP_STATUS_ACCEPTED,
				"Status code 200 did not matched for pos redemption api");
		logger.info("Pos Void redemption api for Redemption code is successful with redemption code "
				+ redemptionCodeRedemptionCode);
		TestListeners.extentTest.get()
				.pass("Pos Void redemption api for Redemption code is successful with redemption code "
						+ redemptionCodeRedemptionCode);

		utils.longWaitInSeconds(6);
		// Pos redemption api for Subscription
		String txn3 = "123456" + CreateDateTime.getTimeDateString();
		String date3 = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key3 = CreateDateTime.getTimeDateString();
		int posRedemptionOfSubscription_attempts = 0;
		Response posRedemptionOfSubscriptionResp = null;
		while (posRedemptionOfSubscription_attempts < 10) {
			Utilities.longWait(1500);
			posRedemptionOfSubscriptionResp = pageObj.endpoints().posRedemptionOfSubscription(userEmail, date3,
					subscriptionId, key3, txn3, dataSet.get("locationkey"), "8", "12003");
			if (posRedemptionOfSubscriptionResp.getStatusCode() == ApiConstants.HTTP_STATUS_OK) {
				break;
			}
			logger.info(
					"Attempt no for Pos redemption api for Subscription is " + posRedemptionOfSubscription_attempts);
			posRedemptionOfSubscription_attempts++;
		}
		Assert.assertEquals(posRedemptionOfSubscriptionResp.statusCode(), ApiConstants.HTTP_STATUS_OK);
		String subscriptionRedemptionId = posRedemptionOfSubscriptionResp.jsonPath().get("redemption_id").toString();
		logger.info(
				"Pos redemption api for Subscription is successful with redemption code " + subscriptionRedemptionId);
		pageObj.utils().logPass(
				"Pos redemption api for Subscription is successful with redemption code " + subscriptionRedemptionId);

		utils.longWaitInSeconds(6);
		// Void redemption api for Subscription
		Response posVoidRedemptionSubscriptionRespo = pageObj.endpoints().posVoidRedemptionPolling(userEmail,
				subscriptionRedemptionId, dataSet.get("locationkey"));
		Assert.assertEquals(posVoidRedemptionSubscriptionRespo.getStatusCode(), ApiConstants.HTTP_STATUS_ACCEPTED,
				"Status code 200 did not matched for pos redemption api");
		logger.info("Pos Void redemption api for Subscription is successful with redemption code "
				+ subscriptionRedemptionId);
		TestListeners.extentTest.get()
				.pass("Pos Void redemption api for Subscription is successful with redemption code "
						+ subscriptionRedemptionId);

	}

	@Test(description = "SQ-T4489 [Point Unlock]Verify all types of redemptions and void them also", groups = {
			"regression", "dailyrun" }, priority = 1)
	@Owner(name = "Hardik Bhardwaj")
	public void T4489_typesOfRedemptions() throws Exception {

		String spPrice = Integer.toString(Utilities.getRandomNoFromRange(300, 500));
		String endDateTime = CreateDateTime.getFutureDate(1) + " 10:00 PM";
		String PlanID = dataSet.get("PlanID");

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
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "900",
				dataSet.get("reedemable_id"), blankString, "980");
		logger.info("Send point to the user successfully");
		pageObj.utils().logit("Send Reedemable to the user successfully");
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send message to user");
		pageObj.utils().logit("Api2  send reward amount to user is successful");

		// get reward id
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("reedemable_id"));

		logger.info("Reward id " + rewardId + " is generated successfully ");
		pageObj.utils().logPass("Reward id " + rewardId + " is generated successfully ");

		// subscription purchase api 2
		Response purchaseSubscriptionresponse2 = pageObj.endpoints().Api2SubscriptionPurchaseWithStartTimeAutorenewal(
				token, PlanID, dataSet.get("client"), dataSet.get("secret"), spPrice, endDateTime, "false");
		Assert.assertEquals(purchaseSubscriptionresponse2.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for subscription purchase api 2");
		logger.info("User will be able to purchase the subscription plan successfully using api2");
		TestListeners.extentTest.get()
				.info("User will be able to purchase the subscription plan successfully using api2");
		String subscriptionId = purchaseSubscriptionresponse2.jsonPath().get("subscription_id").toString();

		// pos redemption API for Reward
		Response posRedemptionOfRewardRespo = pageObj.endpoints().posRedemptionOfReward(userEmail,
				dataSet.get("locationkey"), rewardId);
		Assert.assertEquals(posRedemptionOfRewardRespo.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for pos redemption api");
		String redemptionCodeReward = posRedemptionOfRewardRespo.jsonPath().get("redemption_id").toString();
		logger.info("pos redemption API for Reward is successful with redemption code " + redemptionCodeReward);
		TestListeners.extentTest.get()
				.pass("pos redemption API for Reward is successful with redemption code " + redemptionCodeReward);

//		utils.longwait(7000);
		// Void redemption api for Reward
		Response posVoidRedemptionRewardRespo = pageObj.endpoints().posVoidRedemptionPolling(userEmail,
				redemptionCodeReward, dataSet.get("locationkey"));
		Assert.assertEquals(posVoidRedemptionRewardRespo.getStatusCode(), ApiConstants.HTTP_STATUS_ACCEPTED,
				"Status code 200 did not matched for pos redemption api");
		logger.info("Pos Void redemption api for Reward is successful with redemption code " + redemptionCodeReward);
		TestListeners.extentTest.get()
				.pass("Pos Void redemption api for Reward is successful with redemption code " + redemptionCodeReward);

		// Pos redemption api for Redeemable via offer
		String txn3 = "123456" + CreateDateTime.getTimeDateString();
		String date3 = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key3 = CreateDateTime.getTimeDateString();
		int posRedemptionOfRedeemableViaOfferResp_attempts = 0;
		Response posRedemptionOfRedeemableViaOfferResp = null;
		while (posRedemptionOfRedeemableViaOfferResp_attempts < 10) {
			Utilities.longWait(1500);
			posRedemptionOfRedeemableViaOfferResp = pageObj.endpoints().posRedemptionOfRedeemable(userEmail, date3,
					key3, txn3, dataSet.get("locationkey"), dataSet.get("reedemable_id1"), dataSet.get("item_id"));
			if (posRedemptionOfRedeemableViaOfferResp.getStatusCode() == ApiConstants.HTTP_STATUS_OK) {
				break;
			}
			logger.info("Attempt no for Pos redemption api for Redeemable via offer is "
					+ posRedemptionOfRedeemableViaOfferResp_attempts);
			posRedemptionOfRedeemableViaOfferResp_attempts++;
		}
		Assert.assertEquals(posRedemptionOfRedeemableViaOfferResp.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for pos redemption api via offer");
		String redemptionCodeRedeemableViaOffer = posRedemptionOfRedeemableViaOfferResp.jsonPath().get("redemption_id")
				.toString();
		logger.info("Pos redemption api for Redeemable via offer is successful with redemption code "
				+ redemptionCodeRedeemableViaOffer);
		TestListeners.extentTest.get()
				.pass("Pos redemption api for Redeemable via offer is successful with redemption code "
						+ redemptionCodeRedeemableViaOffer);

//		utils.longwait(7000);
		// Void redemption api for Redeemable
		Response posVoidRedemptionRedeemableViaOfferRespo = pageObj.endpoints().posVoidRedemptionPolling(userEmail,
				redemptionCodeRedeemableViaOffer, dataSet.get("locationkey"));
		Assert.assertEquals(posVoidRedemptionRedeemableViaOfferRespo.getStatusCode(), ApiConstants.HTTP_STATUS_ACCEPTED,
				"Status code 200 did not matched for pos redemption api");
		logger.info("Pos Void redemption api for Redeemable via offer is successful with redemption code "
				+ redemptionCodeRedeemableViaOffer);
		TestListeners.extentTest.get()
				.pass("Pos Void redemption api for Redeemable via offer is successful with redemption code "
						+ redemptionCodeRedeemableViaOffer);

		// Pos redemption api for Redeemable
		String txn1 = "123456" + CreateDateTime.getTimeDateString();
		String date1 = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key1 = CreateDateTime.getTimeDateString();
		utils.longwait(4000);
		Response posRedemptionOfRedeemableResp = pageObj.endpoints().posRedemptionOfRedeemable(userEmail, date1, key1,
				txn1, dataSet.get("locationkey"), dataSet.get("reedemable_id"), dataSet.get("item_id"));
		Assert.assertEquals(posRedemptionOfRedeemableResp.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for pos redemption api");
		String redemptionCodeRedeemable = posRedemptionOfRedeemableResp.jsonPath().get("redemption_id").toString();
		logger.info("Pos redemption api for Redeemable is successful with redemption code " + redemptionCodeRedeemable);
		pageObj.utils().logPass(
				"Pos redemption api for Redeemable is successful with redemption code " + redemptionCodeRedeemable);

//		utils.longwait(7000);
		// Void redemption api for Redeemable
		Response posVoidRedemptionRedeemableRespo = pageObj.endpoints().posVoidRedemptionPolling(userEmail,
				redemptionCodeRedeemable, dataSet.get("locationkey"));
		Assert.assertEquals(posVoidRedemptionRedeemableRespo.getStatusCode(), ApiConstants.HTTP_STATUS_ACCEPTED,
				"Status code 200 did not matched for pos redemption api");
		logger.info("Pos Void redemption api for Redeemable is successful with redemption code "
				+ redemptionCodeRedeemable);
		pageObj.utils().logPass("Pos Void redemption api for Redeemable is successful with redemption code "
				+ redemptionCodeRedeemable);

		// generate redemption code using mobile api
		int redemption_codeResponse_attempts = 0;
		Response redemption_codeResponse = null;
		while (redemption_codeResponse_attempts < 10) {
			Utilities.longWait(1500);
			redemption_codeResponse = pageObj.endpoints().Api1MobileRedemptionReward_id(token, rewardId,
					dataSet.get("client"), dataSet.get("secret"));
			if (redemption_codeResponse.getStatusCode() == ApiConstants.HTTP_STATUS_CREATED) {
				break;
			}
			logger.info(
					"Attempt no for generate redemption code using mobile api is " + redemption_codeResponse_attempts);
			redemption_codeResponse_attempts++;
		}
		Assert.assertEquals(redemption_codeResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		String redemption_Code = redemption_codeResponse.jsonPath().get("internal_tracking_code").toString();
		logger.info("generate redemption code using mobile api is successful with redemption code " + redemption_Code);
		pageObj.utils().logPass(
				"generate redemption code using mobile api is successful with redemption code " + redemption_Code);

		// Pos redemption api for Redemption code
		String txn2 = "123456" + CreateDateTime.getTimeDateString();
		String date2 = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key2 = CreateDateTime.getTimeDateString();
		int posRedemptionOfRedemptionCodeResp_attempts = 0;
		Response posRedemptionOfRedemptionCodeResp = null;
		while (posRedemptionOfRedemptionCodeResp_attempts < 10) {
			Utilities.longWait(1500);
			posRedemptionOfRedemptionCodeResp = pageObj.endpoints().posRedemptionOfCouponCode(userEmail, date2,
					redemption_Code, key2, txn2, dataSet.get("locationkey"));
			if (posRedemptionOfRedemptionCodeResp.getStatusCode() == ApiConstants.HTTP_STATUS_OK) {
				break;
			}
			logger.info("Attempt no for Pos redemption api for Redemption code is "
					+ posRedemptionOfRedemptionCodeResp_attempts);
			posRedemptionOfRedemptionCodeResp_attempts++;
		}
		Assert.assertEquals(posRedemptionOfRedemptionCodeResp.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for pos redemption api");
		String redemptionCodeRedemptionCode = posRedemptionOfRedemptionCodeResp.jsonPath().get("redemption_id")
				.toString();
		logger.info("Pos redemption api for Redemption code is successful with redemption code "
				+ redemptionCodeRedemptionCode);
		pageObj.utils().logPass("Pos redemption api for Redemption code is successful with redemption code "
				+ redemptionCodeRedemptionCode);

//		utils.longwait(7000);
		// Void redemption api for Redemption code
		Response posVoidRedemptionRedemptionCodeRespo = pageObj.endpoints().posVoidRedemptionPolling(userEmail,
				redemptionCodeRedemptionCode, dataSet.get("locationkey"));
		Assert.assertEquals(posVoidRedemptionRedemptionCodeRespo.getStatusCode(), ApiConstants.HTTP_STATUS_ACCEPTED,
				"Status code 200 did not matched for pos redemption api");
		logger.info("Pos Void redemption api for Redemption code is successful with redemption code "
				+ redemptionCodeRedemptionCode);
		TestListeners.extentTest.get()
				.pass("Pos Void redemption api for Redemption code is successful with redemption code "
						+ redemptionCodeRedemptionCode);

		utils.longWaitInSeconds(6);
		// Pos redemption api for Subscription
		String txn4 = "123456" + CreateDateTime.getTimeDateString();
		String date4 = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key4 = CreateDateTime.getTimeDateString();
		int posRedemptionOfSubscriptionResp_attempts = 0;
		Response posRedemptionOfSubscriptionResp = null;
		while (posRedemptionOfSubscriptionResp_attempts < 10) {
			Utilities.longWait(1500);
			posRedemptionOfSubscriptionResp = pageObj.endpoints().posRedemptionOfSubscription(userEmail, date4,
					subscriptionId, key4, txn4, dataSet.get("locationkey"), "8", "12003");
			if (posRedemptionOfSubscriptionResp.getStatusCode() == ApiConstants.HTTP_STATUS_OK) {
				break;
			}
			logger.info("Attempt no for Pos redemption api for Subscription is "
					+ posRedemptionOfSubscriptionResp_attempts);
			posRedemptionOfSubscriptionResp_attempts++;
		}
		Assert.assertEquals(posRedemptionOfSubscriptionResp.statusCode(), ApiConstants.HTTP_STATUS_OK);
		String subscriptionRedemptionId = posRedemptionOfSubscriptionResp.jsonPath().get("redemption_id").toString();
		logger.info(
				"Pos redemption api for Subscription is successful with redemption code " + subscriptionRedemptionId);
		pageObj.utils().logPass(
				"Pos redemption api for Subscription is successful with redemption code " + subscriptionRedemptionId);

//		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
//		pageObj.instanceDashboardPage().loginToInstance();

		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_subscriptions", "check");
		pageObj.dashboardpage().clickOnUpdateButton();

		// Void redemption api for Subscription
		utils.longWaitInSeconds(10); // applying this wait beacause subscription redemption entry take time to
										// reflect in DB
		Response posVoidRedemptionSubscriptionRespo = pageObj.endpoints().posVoidRedemptionPolling(userEmail,
				subscriptionRedemptionId, dataSet.get("locationkey"));
		Assert.assertEquals(posVoidRedemptionSubscriptionRespo.getStatusCode(), ApiConstants.HTTP_STATUS_ACCEPTED,
				"Status code 202 did not matched for pos redemption api");
		logger.info("Pos Void redemption api for Subscription is successful with redemption code "
				+ subscriptionRedemptionId);
		TestListeners.extentTest.get()
				.pass("Pos Void redemption api for Subscription is successful with redemption code "
						+ subscriptionRedemptionId);

	}

	@Test(description = "SQ-T4490 [Point to Manual]Verify all types of redemptions and void them also", priority = 2, groups = {
			"unstable", "regression", "dailyrun" })
	@Owner(name = "Hardik Bhardwaj")
	public void T4490_typesOfRedemptions() throws Exception {

//		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
//		pageObj.instanceDashboardPage().loginToInstance();

		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_subscriptions", "check");
		pageObj.dashboardpage().clickOnUpdateButton();

		String spPrice = Integer.toString(Utilities.getRandomNoFromRange(300, 500));
		String endDateTime = CreateDateTime.getFutureDate(1) + " 10:00 PM";
		String PlanID = dataSet.get("PlanID");

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

		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		pageObj.guestTimelinePage().messageGiftRewardsToUser(dataSet.get("subject"), "Reward Amount", "100",
				dataSet.get("giftReason"));

		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "Message sent did not displayed on timeline");
		logger.info("Verified that Success message of Reward Amount send to user ");
		pageObj.utils().logPass("Verified that Success message of Reward Amount send to user ");

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().Api2SendMessageToUser(userID, dataSet.get("apiKey"), "310",
				dataSet.get("reedemable_id"), "320", "330");
		logger.info("Send point to the user successfully");
		pageObj.utils().logit("Send Reedemable to the user successfully");
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send message to user");
		pageObj.utils().logit("Api2  send reward amount to user is successful");

		// get reward id
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("reedemable_id"));
		logger.info("Reward id " + rewardId + " is generated successfully ");
		pageObj.utils().logPass("Reward id " + rewardId + " is generated successfully ");

		// subscription purchase api 2
		Response purchaseSubscriptionresponse2 = pageObj.endpoints().Api2SubscriptionPurchaseAutorenewal(token, PlanID,
				dataSet.get("client"), dataSet.get("secret"), spPrice, endDateTime, "false");
		Assert.assertEquals(purchaseSubscriptionresponse2.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for subscription purchase api 2");
		logger.info("User will be able to purchase the subscription plan successfully using api2");
		TestListeners.extentTest.get()
				.info("User will be able to purchase the subscription plan successfully using api2");
		String subscriptionId = purchaseSubscriptionresponse2.jsonPath().get("subscription_id").toString();

		// Pos redemption api for Amount
		String key = CreateDateTime.getTimeDateString();
		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T10:50:00+05:30";
		Response posRedemptionOfAmountRespo = pageObj.endpoints().posRedemptionOfAmount(userEmail, date, key, txn,
				dataSet.get("redeemAmount"), dataSet.get("locationkey"));
		Assert.assertEquals(posRedemptionOfAmountRespo.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for pos redemption api");
		String redemptionCodeAmount = posRedemptionOfAmountRespo.jsonPath().get("redemption_id").toString();
		logger.info("Pos redemption api for Amount is successful with redemption code " + redemptionCodeAmount);
		TestListeners.extentTest.get()
				.pass("Pos redemption api for Amount is successful with redemption code " + redemptionCodeAmount);

		// Void redemption api for amount
		Response posVoidRedemptionAmtRespo = pageObj.endpoints().posVoidRedemptionPolling(userEmail,
				redemptionCodeAmount, dataSet.get("locationkey"));
		Assert.assertEquals(posVoidRedemptionAmtRespo.getStatusCode(), ApiConstants.HTTP_STATUS_ACCEPTED,
				"Status code 200 did not matched for pos redemption api");
		logger.info("Pos Void redemption api for amount is successful with redemption code " + redemptionCodeAmount);
		TestListeners.extentTest.get()
				.pass("Pos Void redemption api for amount is successful with redemption code " + redemptionCodeAmount);

		// Pos redemption API for fuel
		String txn5 = "123456" + CreateDateTime.getTimeDateString();
		String date5 = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key5 = CreateDateTime.getTimeDateString();
//		utils.longwait(7000);

		int posRedemptionOfFuelRespo_attempts = 0;
		Response posRedemptionOfFuelRespo = null;
		while (posRedemptionOfFuelRespo_attempts < 10) {
			Utilities.longWait(1500);
			posRedemptionOfFuelRespo = pageObj.endpoints().posRedemptionOfFuel(userEmail, date5, key5, txn5,
					dataSet.get("locationkey"));
			if (posRedemptionOfFuelRespo.getStatusCode() == ApiConstants.HTTP_STATUS_OK) {
				break;
			}
			logger.info("Attempt no for Pos redemption API for fuel is " + posRedemptionOfFuelRespo_attempts);
			posRedemptionOfFuelRespo_attempts++;
		}
		Assert.assertEquals(posRedemptionOfFuelRespo.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for pos redemption api");
		String redemptionCodeFuel = posRedemptionOfFuelRespo.jsonPath().get("redemption_id").toString();
		logger.info("Pos redemption api for fuel is successful with redemption code " + redemptionCodeFuel);
		TestListeners.extentTest.get()
				.pass("Pos redemption api for fuel is successful with redemption code " + redemptionCodeFuel);

		// Void redemption api for fuel
		Response posVoidRedemptionFuelRespo = pageObj.endpoints().posVoidRedemptionPolling(userEmail,
				redemptionCodeFuel, dataSet.get("locationkey"));
		Assert.assertEquals(posVoidRedemptionFuelRespo.getStatusCode(), ApiConstants.HTTP_STATUS_ACCEPTED,
				"Status code 200 did not matched for pos redemption api");
		logger.info("Pos Void redemption api for fuel is successful with redemption code " + redemptionCodeFuel);
		TestListeners.extentTest.get()
				.pass("Pos Void redemption api for fuel is successful with redemption code " + redemptionCodeFuel);

		// pos redemption API for Reward
		int posRedemptionOfRewardRespo_attempts = 0;
		Response posRedemptionOfRewardRespo = null;
		while (posRedemptionOfRewardRespo_attempts < 10) {
			Utilities.longWait(1500);
			posRedemptionOfRewardRespo = pageObj.endpoints().posRedemptionOfReward(userEmail,
					dataSet.get("locationkey"), rewardId);
			if (posRedemptionOfRewardRespo.getStatusCode() == ApiConstants.HTTP_STATUS_OK) {
				break;
			}
			logger.info("Attempt no for pos redemption API for Reward is " + posRedemptionOfRewardRespo_attempts);
			posRedemptionOfRewardRespo_attempts++;
		}
		Assert.assertEquals(posRedemptionOfRewardRespo.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for pos redemption api");
		String redemptionCodeReward = posRedemptionOfRewardRespo.jsonPath().get("redemption_id").toString();
		logger.info("pos redemption API for Reward is successful with redemption code " + redemptionCodeReward);
		TestListeners.extentTest.get()
				.pass("pos redemption API for Reward is successful with redemption code " + redemptionCodeReward);

		utils.longwait(7000);
		// Void redemption api for Reward
		Response posVoidRedemptionRewardRespo = pageObj.endpoints().posVoidRedemptionPolling(userEmail,
				redemptionCodeReward, dataSet.get("locationkey"));
		Assert.assertEquals(posVoidRedemptionRewardRespo.getStatusCode(), ApiConstants.HTTP_STATUS_ACCEPTED,
				"Status code 200 did not matched for pos redemption api");
		logger.info("Pos Void redemption api for Reward is successful with redemption code " + redemptionCodeReward);
		TestListeners.extentTest.get()
				.pass("Pos Void redemption api for Reward is successful with redemption code " + redemptionCodeReward);

		// Pos redemption api for Redeemable
		String txn1 = "123456" + CreateDateTime.getTimeDateString();
		String date1 = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key1 = CreateDateTime.getTimeDateString();
		int posRedemptionOfRedeemableResp_attempts = 0;
		Response posRedemptionOfRedeemableResp = null;
		while (posRedemptionOfRedeemableResp_attempts < 10) {
			Utilities.longWait(1500);
			posRedemptionOfRedeemableResp = pageObj.endpoints().posRedemptionOfRedeemable(userEmail, date1, key1, txn1,
					dataSet.get("locationkey"), dataSet.get("reedemable_id"), dataSet.get("item_id"));
			if (posRedemptionOfRedeemableResp.getStatusCode() == ApiConstants.HTTP_STATUS_OK) {
				break;
			}
			logger.info(
					"Attempt no for Pos redemption api for Redeemable is " + posRedemptionOfRedeemableResp_attempts);
			posRedemptionOfRedeemableResp_attempts++;
		}
		Assert.assertEquals(posRedemptionOfRedeemableResp.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for pos redemption api");
		String redemptionCodeRedeemable = posRedemptionOfRedeemableResp.jsonPath().get("redemption_id").toString();
		logger.info("Pos redemption api for Redeemable is successful with redemption code " + redemptionCodeRedeemable);
		pageObj.utils().logPass(
				"Pos redemption api for Redeemable is successful with redemption code " + redemptionCodeRedeemable);

//		utils.longwait(7000);
		// Void redemption api for Redeemable
		Response posVoidRedemptionRedeemableRespo = pageObj.endpoints().posVoidRedemptionPolling(userEmail,
				redemptionCodeRedeemable, dataSet.get("locationkey"));
		Assert.assertEquals(posVoidRedemptionRedeemableRespo.getStatusCode(), ApiConstants.HTTP_STATUS_ACCEPTED,
				"Status code 200 did not matched for pos redemption api");
		logger.info("Pos Void redemption api for Redeemable is successful with redemption code "
				+ redemptionCodeRedeemable);
		pageObj.utils().logPass("Pos Void redemption api for Redeemable is successful with redemption code "
				+ redemptionCodeRedeemable);

		// generate redemption code using mobile api
//		utils.longwait(7000);
		int redemption_codeResponse_attempts = 0;
		Response redemption_codeResponse = null;
		while (redemption_codeResponse_attempts < 10) {
			Utilities.longWait(1500);
			redemption_codeResponse = pageObj.endpoints().Api1MobileRedemptionRedeemed_Points(token, "11",
					dataSet.get("client"), dataSet.get("secret"));
			if (redemption_codeResponse.getStatusCode() == ApiConstants.HTTP_STATUS_CREATED) {
				break;
			}
			logger.info(
					"Attempt no for generate redemption code using mobile api is " + redemption_codeResponse_attempts);
			redemption_codeResponse_attempts++;
		}
		Assert.assertEquals(redemption_codeResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		String redemption_Code = redemption_codeResponse.jsonPath().get("internal_tracking_code").toString();
		logger.info("generate redemption code using mobile api is successful with redemption code " + redemption_Code);
		pageObj.utils().logPass(
				"generate redemption code using mobile api is successful with redemption code " + redemption_Code);

		// Pos redemption api for Redemption code
		String txn2 = "123456" + CreateDateTime.getTimeDateString();
		String date2 = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key2 = CreateDateTime.getTimeDateString();
		int posRedemptionOfRedemptionCodeResp_attempts = 0;
		Response posRedemptionOfRedemptionCodeResp = null;
		while (posRedemptionOfRedemptionCodeResp_attempts < 10) {
			Utilities.longWait(1500);
			posRedemptionOfRedemptionCodeResp = pageObj.endpoints().posRedemptionOfCouponCode(userEmail, date2,
					redemption_Code, key2, txn2, dataSet.get("locationkey"));
			if (posRedemptionOfRedemptionCodeResp.getStatusCode() == ApiConstants.HTTP_STATUS_OK) {
				break;
			}
			logger.info("Attempt no for Pos redemption api for Redemption code is "
					+ posRedemptionOfRedemptionCodeResp_attempts);
			posRedemptionOfRedemptionCodeResp_attempts++;
		}
		Assert.assertEquals(posRedemptionOfRedemptionCodeResp.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for pos redemption api");
		String redemptionCodeRedemptionCode = posRedemptionOfRedemptionCodeResp.jsonPath().get("redemption_id")
				.toString();
		logger.info("Pos redemption api for Redemption code is successful with redemption code "
				+ redemptionCodeRedemptionCode);
		pageObj.utils().logPass("Pos redemption api for Redemption code is successful with redemption code "
				+ redemptionCodeRedemptionCode);

//		utils.longwait(7000);
		// Void redemption api for Redemption code
		Response posVoidRedemptionRedemptionCodeRespo = pageObj.endpoints().posVoidRedemptionPolling(userEmail,
				redemptionCodeRedemptionCode, dataSet.get("locationkey"));
		Assert.assertEquals(posVoidRedemptionRedemptionCodeRespo.getStatusCode(), ApiConstants.HTTP_STATUS_ACCEPTED,
				"Status code 200 did not matched for pos redemption api");
		logger.info("Pos Void redemption api for Redemption code is successful with redemption code "
				+ redemptionCodeRedemptionCode);
		TestListeners.extentTest.get()
				.pass("Pos Void redemption api for Redemption code is successful with redemption code "
						+ redemptionCodeRedemptionCode);

		utils.longWaitInSeconds(6);
		// Pos redemption api for Subscription
		String txn3 = "123456" + CreateDateTime.getTimeDateString();
		String date3 = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key3 = CreateDateTime.getTimeDateString();
		int posRedemptionOfSubscriptionResp_attempts = 0;
		Response posRedemptionOfSubscriptionResp = null;
		while (posRedemptionOfSubscriptionResp_attempts < 10) {
			Utilities.longWait(1500);
			posRedemptionOfSubscriptionResp = pageObj.endpoints().posRedemptionOfSubscription(userEmail, date3,
					subscriptionId, key3, txn3, dataSet.get("locationkey"), "8", "12003");
			if (posRedemptionOfSubscriptionResp.getStatusCode() == ApiConstants.HTTP_STATUS_OK) {
				break;
			}
			logger.info(
					"Attempt no Pos redemption api for Subscription is " + posRedemptionOfSubscriptionResp_attempts);
			posRedemptionOfSubscriptionResp_attempts++;
		}
		Assert.assertEquals(posRedemptionOfSubscriptionResp.statusCode(), ApiConstants.HTTP_STATUS_OK);
		String subscriptionRedemptionId = posRedemptionOfSubscriptionResp.jsonPath().get("redemption_id").toString();
		logger.info(
				"Pos redemption api for Subscription is successful with redemption code " + subscriptionRedemptionId);
		pageObj.utils().logPass(
				"Pos redemption api for Subscription is successful with redemption code " + subscriptionRedemptionId);

		// Void redemption api for Subscription
//		utils.longwait(7000);
		Response posVoidRedemptionSubscriptionRespo = pageObj.endpoints().posVoidRedemptionPolling(userEmail,
				subscriptionRedemptionId, dataSet.get("locationkey"));
		Assert.assertEquals(posVoidRedemptionSubscriptionRespo.getStatusCode(), ApiConstants.HTTP_STATUS_ACCEPTED,
				"Status code 200 did not matched for pos redemption api");
		logger.info("Pos Void redemption api for Subscription is successful with redemption code "
				+ subscriptionRedemptionId);
		TestListeners.extentTest.get()
				.pass("Pos Void redemption api for Subscription is successful with redemption code "
						+ subscriptionRedemptionId);

	}

	@Test(description = "SQ-T4720 Verify the API v1.   /api/mobile/users/balance for notifications verification with turning flag -on/off", groups = {
			"regression", "dailyrun" }, priority = 3)
	@Owner(name = "Hardik Bhardwaj")
	public void T4720_notificationsVerification() throws Exception {

		// login to business
//		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
//		pageObj.instanceDashboardPage().loginToInstance();

		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// navigate to cockpit -> earning
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");

		// set Global Checkin Rate Limit as 0 receipts within 4 hours
		pageObj.earningPage().setGlobalCheckinRateLimit("25", "1");

		// set Scanning Rate Limit
		pageObj.earningPage().setScanningRateLimit("20");

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
//		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("API2 Signup is successful");
		logger.info("API2 Signup is successful");

		// POS checkin
		// Pos api checkin
		String key = CreateDateTime.getTimeDateString();
		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T10:50:00+05:30";
		Response resp = pageObj.endpoints().posCheckin(date, userEmail, key, txn, dataSet.get("locationkey"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp.getStatusCode(), "Status code 200 did not matched for post chekin api");
		logger.info("POS user checkin is successful");
		pageObj.utils().logit("POS user checkin is successful");

		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		boolean flagverify = pageObj.guestTimelinePage().verifyPosCheckinInTimeLine(key, dataSet.get("amount1"),
				dataSet.get("baseConversionRate"));
		Assert.assertTrue(flagverify, "notification for checkin is not present on timeline");
		logger.info("Verified the timeline for notification for checkin");
		pageObj.utils().logPass("Verified the timeline for notification for checkin");

		for (int i = 0; i < 15; i++) {
			// POS checkin
			Response checkinResponse1 = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"),
					dataSet.get("amount"));
			Assert.assertEquals(checkinResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
					"Status code 200 did not match with POS Checkin ");
		}

		String b_id = dataSet.get("business_id");
		// DB - update preference column in business table
		// updating enable_account_improvement to false
		String query3 = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		pageObj.singletonDBUtilsObj();
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query3, "preferences");

		pageObj.singletonDBUtilsObj();
		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, "false", dataSet.get("dbFlag"),
				b_id);
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to false");
		logger.info(dataSet.get("dbFlag") + " value is updated to false");
		pageObj.utils().logit(dataSet.get("dbFlag") + " value is updated to false");

		// Call user balance api of mobile v1 (/api/mobile/users/balance")
		Response balance_Response = pageObj.endpoints().Api1MobileUsersbalance(token, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(balance_Response.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api v1 mobile users balance");
		String notification = balance_Response.jsonPath().get("notifications").toString();
		Assert.assertEquals(notification, "[]", "Notification is not null");
		logger.info("Secure Account Balance is successful and notification is not visible");
		pageObj.utils().logPass("Secure Account Balance is successful and notification is not visible");

		// DB - update preference column in business table
		// updating enable_account_improvement to false
		String query1 = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		pageObj.singletonDBUtilsObj();
		String expColValue1 = DBUtils.executeQueryAndGetColumnValue(env, query1, "preferences");

		pageObj.singletonDBUtilsObj();
		boolean flag1 = DBUtils.updateBusinessesPreference(env, expColValue1, "true", dataSet.get("dbFlag"),
				b_id);
		Assert.assertTrue(flag1, dataSet.get("dbFlag") + " value is not updated to true");
		logger.info(dataSet.get("dbFlag") + " value is updated to true");
		pageObj.utils().logit(dataSet.get("dbFlag") + " value is updated to true");

		// Call user balance api of mobile v1 (/api/mobile/users/balance")
		Response balance_Response1 = pageObj.endpoints().Api1MobileUsersbalance(token, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(balance_Response1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api v1 mobile users balance");
		String notification1 = balance_Response1.jsonPath().get("notifications").toString();
		Assert.assertNotEquals(notification1, "[]", "Notification is null");
		logger.info("Secure Account Balance is successful and notification is visible");
		pageObj.utils().logPass("Secure Account Balance is successful and notification is visible");

	}

	@Test(description = "SQ-T4721 Verify the API v2 /api2/mobile/users/balance for notifications verification with turning flag -on/off", groups = {
			"regression", "dailyrun" }, priority = 4)
	@Owner(name = "Hardik Bhardwaj")
	public void T4721_notificationsVerification() throws Exception {

		// login to business
//		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
//		pageObj.instanceDashboardPage().loginToInstance();

		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// navigate to cockpit -> earning
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");

		// set Global Checkin Rate Limit as 0 receipts within 4 hours
		pageObj.earningPage().setGlobalCheckinRateLimit("25", "1");

		// set Scanning Rate Limit
		pageObj.earningPage().setScanningRateLimit("20");

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
//		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("API2 Signup is successful");
		logger.info("API2 Signup is successful");

		// POS checkin
		// Pos api checkin
		String key = CreateDateTime.getTimeDateString();
		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T10:50:00+05:30";
		Response resp = pageObj.endpoints().posCheckin(date, userEmail, key, txn, dataSet.get("locationkey"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp.getStatusCode(), "Status code 200 did not matched for post chekin api");
		logger.info("POS user checkin is successful");
		pageObj.utils().logit("POS user checkin is successful");

		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		boolean flagverify = pageObj.guestTimelinePage().verifyPosCheckinInTimeLine(key, dataSet.get("amount1"),
				dataSet.get("baseConversionRate"));
		Assert.assertTrue(flagverify, "notification for checkin is not present on timeline");
		logger.info("Verified the timeline for notification for checkin");
		pageObj.utils().logPass("Verified the timeline for notification for checkin");

		for (int i = 0; i < 15; i++) {
			// POS checkin
			Response checkinResponse1 = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"),
					dataSet.get("amount"));
			Assert.assertEquals(checkinResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
					"Status code 200 did not match with POS Checkin ");
		}

		String b_id = dataSet.get("business_id");
		// DB - update preference column in business table
		// updating enable_account_improvement to false
		String query3 = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		pageObj.singletonDBUtilsObj();
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query3, "preferences");

		pageObj.singletonDBUtilsObj();
		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, "false", dataSet.get("dbFlag"),
				b_id);
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to false");
		logger.info(dataSet.get("dbFlag") + " value is updated to false");
		pageObj.utils().logit(dataSet.get("dbFlag") + " value is updated to false");

		// Mobile User Balance
		Response userBalanceResponse = pageObj.endpoints().Api2FetchUserBalance(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, userBalanceResponse.getStatusCode(),
				"Status code 200 did not matched for api2 fetch user balance");
		String notification = userBalanceResponse.jsonPath().get("notifications").toString();
		Assert.assertEquals(notification, "[]", "Notification is not null");
		pageObj.utils().logPass("API2 User Balance is successful and notification is not visible");
		logger.info("API2 User Balance is successful and notification is not visible");

		// DB - update preference column in business table
		// updating enable_account_improvement to false
		String query1 = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		pageObj.singletonDBUtilsObj();
		String expColValue1 = DBUtils.executeQueryAndGetColumnValue(env, query1, "preferences");

		pageObj.singletonDBUtilsObj();
		boolean flag1 = DBUtils.updateBusinessesPreference(env, expColValue1, "true", dataSet.get("dbFlag"),
				b_id);
		Assert.assertTrue(flag1, dataSet.get("dbFlag") + " value is not updated to true");
		logger.info(dataSet.get("dbFlag") + " value is updated to true");
		pageObj.utils().logit(dataSet.get("dbFlag") + " value is updated to true");

		// Mobile User Balance
		Response userBalanceResponse1 = pageObj.endpoints().Api2FetchUserBalance(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, userBalanceResponse1.getStatusCode(),
				"Status code 200 did not matched for api2 fetch user balance");
		String notification1 = userBalanceResponse1.jsonPath().get("notifications").toString();
		Assert.assertNotEquals(notification1, "[]", "Notification is null");
		pageObj.utils().logPass("API2 User Balance is successful and notification is visible");
		logger.info("API2 User Balance is successful and notification is visible");

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