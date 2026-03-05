package com.punchh.server.LP1Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
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
import com.punchh.server.utilities.ApiUtils;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

//this class will run in the regression
@Listeners(TestListeners.class)
public class SubscriptionWithNoAdaptorTest {
	static Logger logger = LogManager.getLogger(SubscriptionWithNoAdaptorTest.class);
	public WebDriver driver;
	String userEmail;
	String email = "autoemailTemp@punchh.com";
	ApiUtils apiUtils;
	PageObj pageObj;
	String sTCName;
	private String timeStamp, QCname, spPrice, spName, PlanID, iFrameEmail, txn, date, key;
	private String env, run = "ui";
	private String baseUrl;
	private static Map<String, String> dataSet;
	private String amountcap, unitDiscount;
	private boolean GlobalBenefitRedemptionThrottlingToggle;
	Properties prop;
	private String endDateTime;
	SeleniumUtilities selUtils;

	@BeforeClass(alwaysRun = true)
	public void openBrowser() {
		driver = new BrowserUtilities().launchBrowser();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		// single login to instance
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
		logger.info(sTCName + " ==> " + dataSet);
		endDateTime = CreateDateTime.getFutureDate(1) + " 10:00 PM";
		selUtils = new SeleniumUtilities(driver);
		// move to All Businesses page
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
	}

	@Test(description = "LS-T77  / SQ-T4011 Verify cancellation_reason and cancellation_reason_id  appears in the Meta API for canned cancellation"
			+ "LS-T75 / SQ-T4009 (1.0)	Verify enable cancellation feedback value in Program Meta API v1 & v2"
			+ "LS-T76 / SQ-T4010 (1.0)	Verify component_code appears in the Meta API for canned cancellation", groups = {
					"regression", "dailyrun" })
	@Owner(name = "Vansham Mishra")
	public void T4011_VerifySubscriptionCancelReasonsMetaV2Response() throws InterruptedException {
		//pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);

		//pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().navigateToTabs("Miscellaneous Config");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboard("Enable meta cache update on request", "check");

		Response MetaAPI2Response = pageObj.endpoints().metaAPI2SubscriptionCancelReason(dataSet.get("client"),
				dataSet.get("secret"));

		String actualSubscriptionComponent_code = MetaAPI2Response.jsonPath()
				.getString("subscriptions.subscription_cancellation_reasons.component_code");
		Assert.assertFalse(actualSubscriptionComponent_code.contains("null"));
		Assert.assertTrue(actualSubscriptionComponent_code.contains("automationcancelreason"),
				actualSubscriptionComponent_code + " actual not matched with expected ");
		TestListeners.extentTest.get().pass(actualSubscriptionComponent_code + " actual matched with expected ");

		String actualSubscriptionCancellation_reason = MetaAPI2Response.jsonPath()
				.getString("subscriptions.subscription_cancellation_reasons.cancellation_reason");
		Assert.assertTrue(actualSubscriptionCancellation_reason.contains("automationcancelreason"),
				actualSubscriptionCancellation_reason + " actual not matched with expected ");
		TestListeners.extentTest.get().pass(actualSubscriptionCancellation_reason + " actual matched with expected ");

		String actualSubscription_enable_cancellation_feedback = MetaAPI2Response.jsonPath()
				.getString("subscriptions.enable_cancellation_feedback").replace("[", "").replace("]", "");

		Assert.assertTrue(!actualSubscription_enable_cancellation_feedback.contains("null"),
				actualSubscription_enable_cancellation_feedback + " actual is displaying ");
		TestListeners.extentTest.get().pass(actualSubscription_enable_cancellation_feedback + " actual is displaying ");

	}

	@Test(description = "SQ-T2801 Gift a subscription plan to user", groups = { "regression",
			"dailyrun" }, priority = 4)
	@Owner(name = "Vansham Mishra")
	public void T2801_GiftSubscriptionPlanToUser() throws InterruptedException {
		//pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		//pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		spName = dataSet.get("spName");

		// user signup using api2
		iFrameEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(iFrameEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertEquals(signUpResponse.jsonPath().get("user.communicable_email").toString(),
				iFrameEmail.toLowerCase());
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		pageObj.instanceDashboardPage().navigateToGuestTimeline(iFrameEmail);

		pageObj.guestTimelinePage().messageGiftRewardsToUser(dataSet.get("subject"), "Subscription", spName,
				dataSet.get("giftReason"));

		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "Message sent did not displayed on timeline");
		logger.info("Verified that Success message of subscription send to user ");
		TestListeners.extentTest.get().pass("Verified that Success message of subscription send to user ");

		pageObj.guestTimelinePage().navigateToTabs("Subscriptions");

		String actualSubscriptionPlanName = pageObj.guestTimelinePage().getSubscriptionPlansFromTimeline();
		Assert.assertEquals(actualSubscriptionPlanName, spName);
		logger.info("Verified the subscription name " + spName + " on timeline subscription page");
		TestListeners.extentTest.get()
				.pass("Verified the subscription name " + spName + " on timeline subscription page");

	}

	@Test(groups = { "regression", "dailyrun" }, description = "SQ-T2802 Validate the subscription soft cancellation"
			+ "LS-T71 /SQ-T3930 Verify that when the subscription is soft / hard cancelled, it reflects in the response"
			+ "LS-T15 / SQ-T3923 Verify that without plan_id, the API shows subscription details ||"
			+ "SQ-T4225 Verify that API returns user_balance and subscription_plan details even if subscription plan has been soft cancelled. ||"
			+ "SQ-T4226 Verify that API returns 200 status code if valid user_id and discount_type-\"subscription\" is passed in user_balance API ||"
			+ "SQ-T4227 Verify that API returns user_balance and subscription plan details if valid user_id and discount_type subscription is passed in user_balance API"
			+ "SQ-T4052 (1.0) Verify the flag configuration on Admin Initiated Cancellation"
            + "SQ-T4051 (1.0) Verify on Soft or Hard cancellation of Subscription Admin Initiated cancellation list is displayed" +
            "SQ-T4005 (1.0) Verify that when the subscription is soft / hard cancelled, it reflects in the response", priority = 2)
	@Owner(name = "Vansham Mishra")
	public void T2802_subscriptionSoftCancellation() throws InterruptedException {
		//pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		//pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		spName = dataSet.get("spName");
		PlanID = dataSet.get("PlanID");
		spPrice = dataSet.get("spPrice");
		int expSpPrice = Integer.parseInt(spPrice);

		// user signup using api2
		iFrameEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(iFrameEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertEquals(signUpResponse.jsonPath().get("user.communicable_email").toString(),
				iFrameEmail.toLowerCase());
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userId = signUpResponse.jsonPath().get("user.user_id").toString();

		Response purchaseSubscriptionresponseWithoutPlanID = pageObj.endpoints().Api2SubscriptionPurchase(token, "",
				dataSet.get("client"), dataSet.get("secret"), spPrice, endDateTime);

		Assert.assertEquals(purchaseSubscriptionresponseWithoutPlanID.statusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Should not able to purchase Plan if Plan ID is blank");

		String errorMessage = purchaseSubscriptionresponseWithoutPlanID.jsonPath().getString("errors.invalid_request")
				.replace("[", "").replace("]", "");
		Assert.assertEquals(errorMessage, "Invalid Plan ID.", "Expected Error message is not coming");

		// subscription purchase api 2
		Response purchaseSubscriptionresponse = pageObj.endpoints().Api2SubscriptionPurchase(token, PlanID,
				dataSet.get("client"), dataSet.get("secret"), spPrice, endDateTime);
		Assert.assertEquals(purchaseSubscriptionresponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		String subscription_id = purchaseSubscriptionresponse.jsonPath().get("subscription_id").toString();
		logger.info(iFrameEmail + " purchased " + subscription_id + " Plan id = " + PlanID);

		pageObj.instanceDashboardPage().navigateToGuestTimeline(iFrameEmail);
		pageObj.guestTimelinePage().navigateToTabs("Subscriptions");

		String actualSubscriptionPlanName = pageObj.guestTimelinePage().getSubscriptionPlansFromTimeline();
		Assert.assertEquals(actualSubscriptionPlanName, spName);
		logger.info("Verified the subscription name " + spName + " on timeline subscription page");
		TestListeners.extentTest.get()
				.pass("Verified the subscription name " + spName + " on timeline subscription page");

		int actualSbuscriptionPlanPrice = pageObj.guestTimelinePage().getSubscriptionPlanPriceFromTimeline();
		Assert.assertEquals(actualSbuscriptionPlanPrice, expSpPrice);
		logger.info(
				"Verified the subscription price " + actualSbuscriptionPlanPrice + " on timeline subscription page");
		TestListeners.extentTest.get().pass(
				"Verified the subscription price " + actualSbuscriptionPlanPrice + " on timeline subscription page");

		pageObj.guestTimelinePage().clickOnSubscriptionCancel(dataSet.get("cancelType"));
		pageObj.guestTimelinePage().setCancellationFeedbackInTextArea(dataSet.get("cancellationFeedback"));

		pageObj.guestTimelinePage().accecptSubscriptionCancellation(dataSet.get("cancelReason"));

		String subscriptionCancellationText = pageObj.guestTimelinePage().getSubscriptionCancellationType();

		Assert.assertTrue(subscriptionCancellationText.contains(dataSet.get("cancelType")));
		Response userSubscriptionResponse = pageObj.endpoints().Api2UserSubscriptionWithCancellation(token,
				dataSet.get("client"), dataSet.get("secret"));

		String actualcancellation_reason = userSubscriptionResponse.jsonPath()
				.getString("subscriptions[0].cancellation_reason").replace("[", "").replace("]", "");
		Assert.assertEquals(actualcancellation_reason, dataSet.get("cancellationFeedback"));

		logger.info("Verified the " + dataSet.get("cancelType") + " cancellation type on timeline subscription page");
		TestListeners.extentTest.get()
				.pass("Verified the " + dataSet.get("cancelType") + " cancellation type on timeline subscription page");

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "10", "", "",
				"");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		logger.info("Send amount to the user successfully");
		TestListeners.extentTest.get().pass("Send amount to the user successfully");

		// POS fetch Account Balance
		Response fetchAccountBalanceResponse = pageObj.endpoints().posFetchAccountBalance(userId, "subscription",
				dataSet.get("locationkey"));
		Assert.assertEquals(fetchAccountBalanceResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean isPosAccountBalanceSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.posAccountBalanceSchema, fetchAccountBalanceResponse.asString());
		Assert.assertTrue(isPosAccountBalanceSchemaValidated, "POS Fetch Account Balance Schema validation failed");
		String subscriptionId = fetchAccountBalanceResponse.jsonPath().get("subscriptions[0].subscription_id")
				.toString();
//		String amount = fetchAccountBalanceResponse.jsonPath().get("account_balance").toString();
		Assert.assertEquals(subscriptionId, subscription_id, "Api is not returning subscription_plan details");
		logger.info("Api is returning subscription_plan details");
		TestListeners.extentTest.get().pass("Api is returning subscription_plan details");

		String jsonObjectString = fetchAccountBalanceResponse.asString();
		JSONObject finalResponse = new JSONObject(jsonObjectString);

		Boolean accountBalance = finalResponse.has("account_balance");
		Assert.assertEquals(true, accountBalance, "account_balance line not found");
		logger.info("account_balance line found");
		TestListeners.extentTest.get().pass("account_balance line  found");

	}

	@Test(groups = { "regression", "dailyrun" }, description = "SQ-T2803 Validate the subscription hard cancellation"
			+ "LS-T71	Verify that when the subscription is soft / hard cancelled, it reflects in the response"
			+ "SQ-T4052 (1.0) Verify the flag configuration on Admin Initiated Cancellation", priority = 3)
	@Owner(name = "Vansham Mishra")
	public void T2803_subscriptionHardCancellation() throws InterruptedException {
		//pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		//pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		spName = dataSet.get("spName");
		PlanID = dataSet.get("PlanID");
		spPrice = dataSet.get("spPrice");
		int expSpPrice = Integer.parseInt(spPrice);

		// user signup using api2
		iFrameEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(iFrameEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertEquals(signUpResponse.jsonPath().get("user.communicable_email").toString(),
				iFrameEmail.toLowerCase());
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		// subscription purchase api 2
		Response purchaseSubscriptionresponse = pageObj.endpoints().Api2SubscriptionPurchase(token, PlanID,
				dataSet.get("client"), dataSet.get("secret"), spPrice, endDateTime);
		Assert.assertEquals(purchaseSubscriptionresponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		String subscription_id = purchaseSubscriptionresponse.jsonPath().get("subscription_id").toString();
		logger.info(iFrameEmail + " purchased " + subscription_id + " Plan id = " + PlanID);

		pageObj.instanceDashboardPage().navigateToGuestTimeline(iFrameEmail);
		pageObj.guestTimelinePage().navigateToTabs("Subscriptions");

		String actualSubscriptionPlanName = pageObj.guestTimelinePage().getSubscriptionPlansFromTimeline();
		Assert.assertEquals(actualSubscriptionPlanName, spName);
		logger.info("Verified the subscription name " + spName + " on timeline subscription page");
		TestListeners.extentTest.get()
				.pass("Verified the subscription name " + spName + " on timeline subscription page");

		int actualSbuscriptionPlanPrice = pageObj.guestTimelinePage().getSubscriptionPlanPriceFromTimeline();
		Assert.assertEquals(actualSbuscriptionPlanPrice, expSpPrice);
		logger.info(
				"Verified the subscription price " + actualSbuscriptionPlanPrice + " on timeline subscription page");
		TestListeners.extentTest.get().pass(
				"Verified the subscription price " + actualSbuscriptionPlanPrice + " on timeline subscription page");

		pageObj.guestTimelinePage().clickOnSubscriptionCancel(dataSet.get("cancelType"));
		pageObj.guestTimelinePage().setCancellationFeedbackInTextArea(dataSet.get("cancellationFeedback"));

		pageObj.guestTimelinePage().accecptSubscriptionCancellation(dataSet.get("cancelReason"));
		String subscriptionCancellationText = pageObj.guestTimelinePage().getSubscriptionCancellationType();

		Assert.assertTrue(subscriptionCancellationText.contains(dataSet.get("cancelType")));

		Response userSubscriptionResponse = pageObj.endpoints().Api2UserSubscriptionWithCancellation(token,
				dataSet.get("client"), dataSet.get("secret"));

		String actualcancellation_reason = userSubscriptionResponse.jsonPath()
				.getString("subscriptions[0].cancellation_reason").replace("[", "").replace("]", "");
		Assert.assertEquals(actualcancellation_reason, dataSet.get("cancellationFeedback"));

		logger.info("Verified the " + dataSet.get("cancelType") + " cancellation type on timeline subscription page");
		TestListeners.extentTest.get()
				.pass("Verified the " + dataSet.get("cancelType") + " cancellation type on timeline subscription page");
		logger.info("== END of T2803	Validate the subscription hard cancellation ==");
	}

	// Anant
	@Test(description = "SQ-T4025 Verify when user enters same component code multiple times, it shows error", groups = {
			"regression", "dailyrun" })
	@Owner(name = "Vansham Mishra")
	public void T4025_sameComponentCodeMultipleTimes() throws InterruptedException {
		String actualError = dataSet.get("errorMsg");

		// Login to instance
		//pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		//pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Settings", "Subscription Cancellation Reasons");

		pageObj.settingsPage().clickAddNext();
		pageObj.settingsPage().clickAddNext();

		List<String> rowNumber = pageObj.settingsPage().getComponentCodeRowNumber();
		System.out.println(rowNumber);

		for (int i = 0; i < rowNumber.size(); i++) {
			pageObj.settingsPage().editComponentCode(dataSet.get("componentCode"), rowNumber.get(i));
			pageObj.settingsPage().editComponentCodeCancellationReason(dataSet.get("cancelationReason"),
					rowNumber.get(i));
		}

		pageObj.settingsPage().clickUpdateBtn();
		String error = pageObj.settingsPage().getErrorMessage();

		Assert.assertEquals(actualError, error, "Error msg value is not equal");
		TestListeners.extentTest.get().pass("getting error when tried to new Component code which value is not unique");
	}

	// Anant
	@Test(description = "SQ-T3998 PS_T64 Verify the API for user_subscriptions for plan_image_url, discount_type", groups = {
			"regression", "dailyrun" })
	@Owner(name = "Vansham Mishra")
	public void PS_T64_VerifyAPIUser_subscriptions() throws InterruptedException {
		// Login to instance
		//pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		//pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		spName = dataSet.get("spName");
		PlanID = dataSet.get("PlanID");
		spPrice = dataSet.get("spPrice");
		int expSpPrice = Integer.parseInt(spPrice);

		// user signup using api2
		iFrameEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(iFrameEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertEquals(signUpResponse.jsonPath().get("user.communicable_email").toString(),
				iFrameEmail.toLowerCase());
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		System.out.println(token);

		// subscription purchase api 2
		Response purchaseSubscriptionresponse = pageObj.endpoints().Api2SubscriptionPurchase(token, PlanID,
				dataSet.get("client"), dataSet.get("secret"), spPrice, endDateTime);
		Assert.assertEquals(purchaseSubscriptionresponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		String subscription_id = purchaseSubscriptionresponse.jsonPath().get("subscription_id").toString();
		logger.info(iFrameEmail + " purchased " + subscription_id + " Plan id = " + PlanID);

		// pageObj.instanceDashboardPage().navigateToGuestTimeline(iFrameEmail);

		// Fetch Subscription Plans for a User using valid data
		pageObj.utils().logit("== Fetch Subscription Plans for a User using valid data ==");
		logger.info("== Fetch Subscription Plans for a User using valid data ==");
		Response userSubcription = pageObj.endpoints().userSubcriptionForApi2(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(userSubcription.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean isApi2FetchSubscriptionPlanSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.authFetchSubscriptionPlansSchema, userSubcription.asString());
		Assert.assertTrue(isApi2FetchSubscriptionPlanSchemaValidated,
				"API v2 Fetch Subscription Plans for User Schema Validation failed");

		String val = userSubcription.jsonPath().get("subscriptions[0]").toString();
		Assert.assertTrue(val.contains("plan_image_url"));
		Assert.assertTrue(val.contains("discount_type"));
		TestListeners.extentTest.get().pass("API2 Fetch Subscription Plans for User with valid data is successful");
		logger.info("API2 Fetch Subscription Plans for User with valid data is successful");

		// Negative case: Fetch Subscription Plans for a User using invalid client
		pageObj.utils().logit("== Fetch Subscription Plans for a User using invalid client ==");
		logger.info("== Fetch Subscription Plans for a User using invalid client ==");
		Response userSubcriptionInvalidClientResponse = pageObj.endpoints().userSubcriptionForApi2("1",
				dataSet.get("secret"), token);
		Assert.assertEquals(userSubcriptionInvalidClientResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED);
		boolean isApi2FetchSubscriptionPlanInvalidClientSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnknownClientSchema, userSubcriptionInvalidClientResponse.asString());
		Assert.assertTrue(isApi2FetchSubscriptionPlanInvalidClientSchemaValidated,
				"API2 Fetch Subscription Plans for User with invalid client Schema Validation failed");
		String userSubcriptionInvalidClientMsg = userSubcriptionInvalidClientResponse.jsonPath()
				.get("errors.unknown_client[0]").toString();
		Assert.assertEquals(userSubcriptionInvalidClientMsg,
				"Client ID is incorrect. Please check client param or contact us");
		TestListeners.extentTest.get()
				.pass("API2 Fetch Subscription Plans for User with invalid client is unsuccessful");
		logger.info("API2 Fetch Subscription Plans for User with invalid client is unsuccessful");

		// Negative case: Fetch Subscription Plans for a User using invalid secret
		pageObj.utils().logit("== Fetch Subscription Plans for a User using invalid secret ==");
		logger.info("== Fetch Subscription Plans for a User using invalid secret ==");
		Response userSubcriptionInvalidSecretResponse = pageObj.endpoints()
				.userSubcriptionForApi2(dataSet.get("client"), "1", token);
		Assert.assertEquals(userSubcriptionInvalidSecretResponse.getStatusCode(), 412);
		boolean isApi2FetchSubscriptionPlanInvalidSecretSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2InvalidSignatureSchema, userSubcriptionInvalidSecretResponse.asString());
		Assert.assertTrue(isApi2FetchSubscriptionPlanInvalidSecretSchemaValidated,
				"API2 Fetch Subscription Plans for User with invalid secret Schema Validation failed");
		String userSubcriptionInvalidSecretMsg = userSubcriptionInvalidSecretResponse.jsonPath()
				.get("errors.invalid_signature[0]").toString();
		Assert.assertEquals(userSubcriptionInvalidSecretMsg,
				"Signature doesn't match. For information about generating the x-pch-digest header, see https://developers.punchh.com.");
		TestListeners.extentTest.get()
				.pass("API2 Fetch Subscription Plans for User with invalid secret is unsuccessful");
		logger.info("API2 Fetch Subscription Plans for User with invalid secret is unsuccessful");

		// Negative case: Fetch Subscription Plans for a User using invalid token
		pageObj.utils().logit("== Fetch Subscription Plans for a User using invalid token ==");
		logger.info("== Fetch Subscription Plans for a User using invalid token ==");
		Response userSubcriptionInvalidTokenResponse = pageObj.endpoints().userSubcriptionForApi2(dataSet.get("client"),
				dataSet.get("secret"), "1");
		Assert.assertEquals(userSubcriptionInvalidTokenResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED);
		boolean isApi2FetchSubscriptionPlanInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnauthorizedErrorSchema, userSubcriptionInvalidTokenResponse.asString());
		Assert.assertTrue(isApi2FetchSubscriptionPlanInvalidTokenSchemaValidated,
				"API2 Fetch Subscription Plans for User with invalid token Schema Validation failed");
		String userSubcriptionInvalidTokenMsg = userSubcriptionInvalidTokenResponse.jsonPath()
				.get("errors.unauthorized[0]").toString();
		Assert.assertEquals(userSubcriptionInvalidTokenMsg,
				"An active access token must be used to query information about the current user.");
		TestListeners.extentTest.get()
				.pass("API2 Fetch Subscription Plans for User with invalid token is unsuccessful");
		logger.info("API2 Fetch Subscription Plans for User with invalid token is unsuccessful");

	}

	// Anant
	@Test(description = "SQ-T4006 (1.0)  LS_T72 Verify the cancellation of Subscription", groups = { "regression",
			"dailyrun" })
	@Owner(name = "Vansham Mishra")
	public void PS_T72_CancellationSubscription() throws InterruptedException {
		// Login to instance
		//pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		//pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		spName = dataSet.get("spName");
		PlanID = dataSet.get("PlanID");
		spPrice = dataSet.get("spPrice");
		int expSpPrice = Integer.parseInt(spPrice);

		Response userMeta = pageObj.endpoints().metaAPI2SubscriptionCancelReason(dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(userMeta.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		String cancelId = userMeta.jsonPath()
				.get("subscriptions.subscription_cancellation_reasons[0].cancellation_reason_id").toString();
		String cancelResaon = userMeta.jsonPath()
				.get("subscriptions.subscription_cancellation_reasons[0].cancellation_reason").toString();

		// user1 signup using api2
		iFrameEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse1 = pageObj.endpoints().Api2SignUp(iFrameEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse1, "API 2 user signup");
		Assert.assertEquals(signUpResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertEquals(signUpResponse1.jsonPath().get("user.communicable_email").toString(),
				iFrameEmail.toLowerCase());
		String token1 = signUpResponse1.jsonPath().get("access_token.token").toString();

		// user2 signup using api2
		iFrameEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(iFrameEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertEquals(signUpResponse.jsonPath().get("user.communicable_email").toString(),
				iFrameEmail.toLowerCase());
		String token2 = signUpResponse.jsonPath().get("access_token.token").toString();

		// user 1 subscription purchase api 2
		Response purchaseSubscriptionresponse = pageObj.endpoints().Api2SubscriptionPurchase(token1, PlanID,
				dataSet.get("client"), dataSet.get("secret"), spPrice, endDateTime);
		Assert.assertEquals(purchaseSubscriptionresponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		String subscription_id = purchaseSubscriptionresponse.jsonPath().get("subscription_id").toString();
		logger.info(iFrameEmail + " purchased " + subscription_id + " Plan id = " + PlanID);

		// user 1 subscription purchase api 2
		Response purchaseSubscriptionresponse2 = pageObj.endpoints().Api2SubscriptionPurchase(token2, PlanID,
				dataSet.get("client"), dataSet.get("secret"), spPrice, endDateTime);
		Assert.assertEquals(purchaseSubscriptionresponse2.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		String subscription_id2 = purchaseSubscriptionresponse2.jsonPath().get("subscription_id").toString();
		logger.info(iFrameEmail + " purchased " + subscription_id2 + " Plan id = " + PlanID);

		// Subscription cancel using APi1
		Response cancelUser1 = pageObj.endpoints().MobAPiCancelSubscription(dataSet.get("client"),
				dataSet.get("secret"), token1, subscription_id, cancelId, cancelResaon);
		Assert.assertEquals(cancelUser1.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		TestListeners.extentTest.get().pass("user1 subscription cancel successfully using API");

		// Negative case: Subscription cancel using API2 with invalid client
		pageObj.utils().logit("== Subscription cancel using API2 with invalid client ==");
		logger.info("== Subscription cancel using API2 with invalid client ==");
		Response cancelSubscriptionInvalidClientResponse = pageObj.endpoints().MobAPi2CancelSubscription("1",
				dataSet.get("secret"), token2, subscription_id2, cancelId, cancelResaon);
		Assert.assertEquals(cancelSubscriptionInvalidClientResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED);
		boolean isApi2CancelSubscriptionInvalidClientSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnknownClientSchema, cancelSubscriptionInvalidClientResponse.asString());
		Assert.assertTrue(isApi2CancelSubscriptionInvalidClientSchemaValidated,
				"API2 Cancel Subscription with invalid client Schema Validation failed");
		String cancelSubscriptionInvalidClientMsg = cancelSubscriptionInvalidClientResponse.jsonPath()
				.get("errors.unknown_client[0]").toString();
		Assert.assertEquals(cancelSubscriptionInvalidClientMsg,
				"Client ID is incorrect. Please check client param or contact us");
		TestListeners.extentTest.get().pass("API2 Cancel Subscription with invalid client is unsuccessful");
		logger.info("API2 Cancel Subscription with invalid client is unsuccessful");

		// Negative case: Subscription cancel using API2 with invalid secret
		pageObj.utils().logit("== Subscription cancel using API2 with invalid secret ==");
		logger.info("== Subscription cancel using API2 with invalid secret ==");
		Response cancelSubscriptionInvalidSecretResponse = pageObj.endpoints().MobAPi2CancelSubscription(
				dataSet.get("client"), "1", token2, subscription_id2, cancelId, cancelResaon);
		Assert.assertEquals(cancelSubscriptionInvalidSecretResponse.getStatusCode(), 412);
		boolean isApi2CancelSubscriptionInvalidSecretSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2InvalidSignatureSchema, cancelSubscriptionInvalidSecretResponse.asString());
		Assert.assertTrue(isApi2CancelSubscriptionInvalidSecretSchemaValidated,
				"API2 Cancel Subscription with invalid secret Schema Validation failed");
		String cancelSubscriptionInvalidSecretMsg = cancelSubscriptionInvalidSecretResponse.jsonPath()
				.get("errors.invalid_signature[0]").toString();
		Assert.assertEquals(cancelSubscriptionInvalidSecretMsg,
				"Signature doesn't match. For information about generating the x-pch-digest header, see https://developers.punchh.com.");
		TestListeners.extentTest.get().pass("API2 Cancel Subscription with invalid secret is unsuccessful");
		logger.info("API2 Cancel Subscription with invalid secret is unsuccessful");

		// Negative case: Subscription cancel using API2 with invalid token
		pageObj.utils().logit("== Subscription cancel using API2 with invalid token ==");
		logger.info("== Subscription cancel using API2 with invalid token ==");
		Response cancelSubscriptionInvalidTokenResponse = pageObj.endpoints().MobAPi2CancelSubscription(
				dataSet.get("client"), dataSet.get("secret"), "1", subscription_id2, cancelId, cancelResaon);
		Assert.assertEquals(cancelSubscriptionInvalidTokenResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED);
		boolean isApi2CancelSubscriptionInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnauthorizedErrorSchema, cancelSubscriptionInvalidTokenResponse.asString());
		Assert.assertTrue(isApi2CancelSubscriptionInvalidTokenSchemaValidated,
				"API2 Cancel Subscription with invalid token Schema Validation failed");
		String cancelSubscriptionInvalidTokenMsg = cancelSubscriptionInvalidTokenResponse.jsonPath()
				.get("errors.unauthorized[0]").toString();
		Assert.assertEquals(cancelSubscriptionInvalidTokenMsg,
				"An active access token must be used to query information about the current user.");
		TestListeners.extentTest.get().pass("API2 Cancel Subscription with invalid token is unsuccessful");
		logger.info("API2 Cancel Subscription with invalid token is unsuccessful");

		// Negative case: Subscription cancel using API2 with invalid subscription_id
		pageObj.utils().logit("== Subscription cancel using API2 with invalid subscription_id ==");
		logger.info("== Subscription cancel using API2 with invalid subscription_id ==");
		Response cancelSubscriptionInvalidSubscriptionIdResponse = pageObj.endpoints().MobAPi2CancelSubscription(
				dataSet.get("client"), dataSet.get("secret"), token2, "1", cancelId, cancelResaon);
		Assert.assertEquals(cancelSubscriptionInvalidSubscriptionIdResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY);
		boolean isApi2CancelSubscriptionInvalidSubscriptionIdSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.dashboardInvalidSubscriptionErrorSchema,
				cancelSubscriptionInvalidSubscriptionIdResponse.asString());
		Assert.assertTrue(isApi2CancelSubscriptionInvalidSubscriptionIdSchemaValidated,
				"API2 Cancel Subscription with invalid subscription_id Schema Validation failed");
		String cancelSubscriptionInvalidSubscriptionIdMsg = cancelSubscriptionInvalidSubscriptionIdResponse.jsonPath()
				.get("errors.invalid_subscription[0]").toString();
		Assert.assertEquals(cancelSubscriptionInvalidSubscriptionIdMsg, "Invalid User Subscription.");
		TestListeners.extentTest.get().pass("API2 Cancel Subscription with invalid subscription_id is unsuccessful");
		logger.info("API2 Cancel Subscription with invalid subscription_id is unsuccessful");

		// Negative case: Subscription cancel using API2 with invalid cancel Id
		pageObj.utils().logit("== Subscription cancel using API2 with invalid cancelId ==");
		logger.info("== Subscription cancel using API2 with invalid cancelId ==");
		Response cancelSubscriptionInvalidCancelIdResponse = pageObj.endpoints().MobAPi2CancelSubscription(
				dataSet.get("client"), dataSet.get("secret"), token2, subscription_id2, "1", cancelResaon);
		Assert.assertEquals(cancelSubscriptionInvalidCancelIdResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY);
		boolean isApi2CancelSubscriptionInvalidCancelIdSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2InvalidCancellationRequestSchema,
				cancelSubscriptionInvalidCancelIdResponse.asString());
		Assert.assertTrue(isApi2CancelSubscriptionInvalidCancelIdSchemaValidated,
				"API2 Cancel Subscription with invalid cancel Id Schema Validation failed");
		String cancelSubscriptionInvalidCancelIdMsg = cancelSubscriptionInvalidCancelIdResponse.jsonPath()
				.get("errors.invalid_cancellation_request[0]").toString();
		Assert.assertEquals(cancelSubscriptionInvalidCancelIdMsg, "Invalid Cancellation Reason ID.");
		TestListeners.extentTest.get().pass("API2 Cancel Subscription with invalid cancel Id is unsuccessful");
		logger.info("API2 Cancel Subscription with invalid cancel Id is unsuccessful");

		// Negative case: Subscription cancel using API2 with empty cancel Id
		pageObj.utils().logit("== Subscription cancel using API2 with empty cancelId ==");
		logger.info("== Subscription cancel using API2 with empty cancelId ==");
		Response cancelSubscriptionEmptyCancelIdResponse = pageObj.endpoints().MobAPi2CancelSubscription(
				dataSet.get("client"), dataSet.get("secret"), token2, subscription_id2, "", cancelResaon);
		Assert.assertEquals(cancelSubscriptionEmptyCancelIdResponse.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST);
		boolean isApi2CancelSubscriptionEmptyCancelIdSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2CancellationReasonIdErrorSchema,
				cancelSubscriptionEmptyCancelIdResponse.asString());
		Assert.assertTrue(isApi2CancelSubscriptionEmptyCancelIdSchemaValidated,
				"API2 Cancel Subscription with empty cancel Id Schema Validation failed");
		String cancelSubscriptionEmptyCancelIdMsg = cancelSubscriptionEmptyCancelIdResponse.jsonPath()
				.get("errors.cancellation_reason_id").toString();
		Assert.assertEquals(cancelSubscriptionEmptyCancelIdMsg, "Required parameter missing or the value is empty.");
		TestListeners.extentTest.get().pass("API2 Cancel Subscription with empty cancel Id is unsuccessful");
		logger.info("API2 Cancel Subscription with empty cancel Id is unsuccessful");

		// Subscription cancel using APi2 with valid data
		pageObj.utils().logit("== Subscription cancel using API2 with valid data ==");
		logger.info("== Subscription cancel using API2 with valid data ==");
		Response cancelUser2 = pageObj.endpoints().MobAPi2CancelSubscription(dataSet.get("client"),
				dataSet.get("secret"), token2, subscription_id2, cancelId, cancelResaon);
		Assert.assertEquals(cancelUser2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean isApi2CancelSubscriptionSchemaValidated = Utilities
				.validateJsonArrayAgainstSchema(ApiResponseJsonSchema.apiStringArraySchema, cancelUser2.asString());
		Assert.assertTrue(isApi2CancelSubscriptionSchemaValidated,
				"API v2 Cancel Subscription Schema Validation failed");
		TestListeners.extentTest.get().pass("user2 subscription cancel successfully using API2");
		logger.info("user2 subscription cancel successfully using API2");

		// Negative case: Subscription cancel using APi2 for already cancelled
		// subscription
		pageObj.utils().logit("== Subscription cancel using API2 for already cancelled subscription ==");
		logger.info("== Subscription cancel using API2 for already cancelled subscription ==");
		Response cancelUser2AlreadyCancelledResponse = pageObj.endpoints().MobAPi2CancelSubscription(
				dataSet.get("client"), dataSet.get("secret"), token2, subscription_id2, cancelId, cancelResaon);
		Assert.assertEquals(cancelUser2AlreadyCancelledResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY);
		boolean isApi2CancelSubscriptionAlreadyCancelledSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.dashboardAlreadyCancelledErrorSchema,
				cancelUser2AlreadyCancelledResponse.asString());
		Assert.assertTrue(isApi2CancelSubscriptionAlreadyCancelledSchemaValidated,
				"API2 Cancel Subscription for already cancelled subscription Schema Validation failed");
		String cancelUser2AlreadyCancelledMsg = cancelUser2AlreadyCancelledResponse.jsonPath()
				.get("errors.already_canceled[0]").toString();
		Assert.assertEquals(cancelUser2AlreadyCancelledMsg, "Subscription is already canceled.");
		TestListeners.extentTest.get()
				.pass("API2 Cancel Subscription for already cancelled subscription is unsuccessful");
		logger.info("API2 Cancel Subscription for already cancelled subscription is unsuccessful");

	}

	// Anant
	@Test(description = "SQ-T4082 (1.0) Verify the guest is marked as superuser and is able to generate multiple redemption code", groups = {
			"regression", "dailyrun" })
	@Owner(name = "Vansham Mishra")
	public void T4082_guestIsSuperUserAbleToGenerateMultipleRedemptionCode() throws InterruptedException {
		//pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		//pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		spName = dataSet.get("spName");
		PlanID = dataSet.get("PlanID");
		spPrice = dataSet.get("spPrice");
		int expSpPrice = Integer.parseInt(spPrice);

		// user signup using api2
		iFrameEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(iFrameEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		// purchase subscription plan
		Response purchaseSubscriptionresponse = pageObj.endpoints().Api2SubscriptionPurchase(token, PlanID,
				dataSet.get("client"), dataSet.get("secret"), spPrice, endDateTime);
		Assert.assertEquals(purchaseSubscriptionresponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);

		String subscription_id = purchaseSubscriptionresponse.jsonPath().get("subscription_id").toString();
		logger.info(iFrameEmail + " purchased " + subscription_id + " Plan id = " + PlanID);

		// navigate to guest timeline->edit profile
		pageObj.instanceDashboardPage().navigateToGuestTimeline(iFrameEmail);
		pageObj.guestTimelinePage().navigateToTabs("Edit Profile");

		// make super user
		pageObj.guestTimelinePage().makeGuestSuperUser();

		// hit redemption api
		Response card1 = pageObj.endpoints().Api2SubscriptionRedemption(dataSet.get("client"), dataSet.get("secret"),
				subscription_id, token);
		Assert.assertEquals(card1.statusCode(), ApiConstants.HTTP_STATUS_CREATED);
		boolean isApi2SubscriptionRedemptionSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.api2SubscriptionRedemptionSchema, card1.asString());
		Assert.assertTrue(isApi2SubscriptionRedemptionSchemaValidated,
				"API v2 Generate a Redemption Code for Subscription Redemption Schema Validation failed");
		String redemptionTrackingCode1 = card1.jsonPath().get("redemption_tracking_code").toString();

		pageObj.instanceDashboardPage().navigateToGuestTimeline(iFrameEmail);
		pageObj.guestTimelinePage().navigateToTabs("Edit Profile");

		Response card2 = pageObj.endpoints().Api2SubscriptionRedemption(dataSet.get("client"), dataSet.get("secret"),
				subscription_id, token);
		Assert.assertEquals(card2.statusCode(), ApiConstants.HTTP_STATUS_CREATED);

		String redemptionTrackingCode2 = card2.jsonPath().get("redemption_tracking_code").toString();

		Assert.assertNotEquals(redemptionTrackingCode1, redemptionTrackingCode2,
				"same redemption code is coming when guest is map as super user and redemption api is hit twice");
		logger.info("different redemptionTrackingCode value is coming when api is hit twice");
		TestListeners.extentTest.get().pass("different redemptionTrackingCode value is coming when api is hit twice");
	}

	@Test(description = "LS-T67 If the subscription is hard cancelled the redemption_code should not be generated", groups = {
			"regression", "dailyrun" })
	@Owner(name = "Vansham Mishra")
	public void LS_T67_SubscriptionHardCancelledRedemptionCodeNotGenerated() throws InterruptedException {
		spName = dataSet.get("spName");
		PlanID = dataSet.get("PlanID");
		spPrice = dataSet.get("spPrice");
		int expSpPrice = Integer.parseInt(spPrice);

		//pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		//pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// user signup using api2
		iFrameEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(iFrameEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertEquals(signUpResponse.jsonPath().get("user.communicable_email").toString(),
				iFrameEmail.toLowerCase());
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		// subscription purchase api 2
		Response purchaseSubscriptionresponse = pageObj.endpoints().Api2SubscriptionPurchase(token, PlanID,
				dataSet.get("client"), dataSet.get("secret"), spPrice, endDateTime);
		Assert.assertEquals(purchaseSubscriptionresponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		String subscription_id = purchaseSubscriptionresponse.jsonPath().get("subscription_id").toString();
		logger.info(iFrameEmail + " purchased " + subscription_id + " Plan id = " + PlanID);

		pageObj.instanceDashboardPage().navigateToGuestTimeline(iFrameEmail);
		pageObj.guestTimelinePage().navigateToTabs("Subscriptions");

		String actualSubscriptionPlanName = pageObj.guestTimelinePage().getSubscriptionPlansFromTimeline();
		Assert.assertEquals(actualSubscriptionPlanName, spName);
		logger.info("Verified the subscription name " + spName + " on timeline subscription page");
		TestListeners.extentTest.get()
				.pass("Verified the subscription name " + spName + " on timeline subscription page");

		int actualSbuscriptionPlanPrice = pageObj.guestTimelinePage().getSubscriptionPlanPriceFromTimeline();
		Assert.assertEquals(actualSbuscriptionPlanPrice, expSpPrice);

		pageObj.guestTimelinePage().clickOnSubscriptionCancel(dataSet.get("cancelType"));
		pageObj.guestTimelinePage().setCancellationFeedbackInTextArea(dataSet.get("cancellationFeedback"));

		pageObj.guestTimelinePage().accecptSubscriptionCancellation(dataSet.get("cancelReason"));
		pageObj.guestTimelinePage().getSubscriptionCancellationType();

		Response card = pageObj.endpoints().Api2SubscriptionRedemption(dataSet.get("client"), dataSet.get("secret"),
				subscription_id, token);
		Assert.assertEquals(card.statusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY);
		boolean isApi2GenerateRedemptionHardCancelledSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.apiErrorObjectSchema, card.asString());
		Assert.assertTrue(isApi2GenerateRedemptionHardCancelledSchemaValidated,
				"API v2 Generate a Redemption Code for Subscription Redemption Schema Validation failed");
		logger.info("when hard cancel is done 422 status code is coming");
		TestListeners.extentTest.get().pass("when hard cancel is done 422 status code is coming");

		String errorMsg = card.jsonPath().get("error").toString();
		Assert.assertEquals(errorMsg, "Redemption Code cannot be generated for a hard cancelled subscription.",
				"Error mag of API is not equal");

		logger.info("Error mag of API is same");
		TestListeners.extentTest.get().pass("Error msg of API is same");

	}

	// Anant
	@Test(description = "SQ-T4076 Verify the Subscription Redemption Codes Limit", groups = { "regression",
			"dailyrun" })
	@Owner(name = "Vansham Mishra")
	public void T4076_SubscriptionRedemptionCodesLimit() throws InterruptedException {
		spName = dataSet.get("spName");
		PlanID = dataSet.get("PlanID");
		spPrice = dataSet.get("spPrice");
		int expSpPrice = Integer.parseInt(spPrice);

		//pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		//pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// user signup using api2
		iFrameEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(iFrameEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertEquals(signUpResponse.jsonPath().get("user.communicable_email").toString(),
				iFrameEmail.toLowerCase());
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		// subscription purchase api 2
		Response purchaseSubscriptionresponse = pageObj.endpoints().Api2SubscriptionPurchase(token, PlanID,
				dataSet.get("client"), dataSet.get("secret"), spPrice, "");
		Assert.assertEquals(purchaseSubscriptionresponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		String subscription_id = purchaseSubscriptionresponse.jsonPath().get("subscription_id").toString();
		logger.info(iFrameEmail + " purchased " + subscription_id + " Plan id = " + PlanID);

		Response card = pageObj.endpoints().Api2SubscriptionRedemption(dataSet.get("client"), dataSet.get("secret"),
				subscription_id, token);
		Assert.assertEquals(card.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.dashboardpage().navigateToTabs("Redemption Validations");
		pageObj.dashboardpage().editProcessableSubscriptionRedemptionCodesPerGuest("0");
		Response card1 = pageObj.endpoints().Api2SubscriptionRedemption(dataSet.get("client"), dataSet.get("secret"),
				subscription_id, token);
		Assert.assertEquals(card1.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.dashboardpage().navigateToTabs("Redemption Validations");
		pageObj.dashboardpage().editProcessableSubscriptionRedemptionCodesPerGuest("0");

	}

	// Anant
	@Test(description = "SQ-T4036 (1.0) Verify the past timezone is set to current timezone not in business timezone", groups = {
			"regression", "dailyrun" })
	@Owner(name = "Vansham Mishra")
	public void T4036_verifyPastTimeZoneSetTocurrentTimeZone() throws Exception {
		//pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		//pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		spName = dataSet.get("spName");
		PlanID = dataSet.get("PlanID");
		spPrice = dataSet.get("spPrice");
		int expSpPrice = Integer.parseInt(spPrice);

		String query = "Select * from subscription_plans where id = " + PlanID + "";
		pageObj.singletonDBUtilsObj();
		String statusColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "timezone");
		// System.out.println(statusColValue);

		Assert.assertNotEquals(statusColValue, "UTC", "Timezone is set to UTC");
		TestListeners.extentTest.get().pass("In db subscription plan ");

//		DBUtils.closeConnection();
	}

	@Test(groups = { "regression",
			"dailyrun" }, description = "SQ-T2800 Validate the Global Benefit Redemption Throttling on subscription plan", priority = 5)
	@Owner(name = "Shashank Sharma")
	public void T2800_GlobalBenefitRedemptionThrottlingOnSubscriptionPlan() throws InterruptedException {

		spName = dataSet.get("subscriptionName");
		QCname = dataSet.get("qcName");
		amountcap = dataSet.get("amountcap");
		unitDiscount = dataSet.get("unitDiscount");
		spPrice = dataSet.get("spPrice");

		GlobalBenefitRedemptionThrottlingToggle = true;
		txn = "123456" + CreateDateTime.getTimeDateString();
		date = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		key = CreateDateTime.getTimeDateString();

		//pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		//pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		PlanID = dataSet.get("subscriptionPlanID");

		// user signup using api2
		iFrameEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(iFrameEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertEquals(signUpResponse.jsonPath().get("user.communicable_email").toString(),
				iFrameEmail.toLowerCase());
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		// subscription purchase api 2
		Response purchaseSubscriptionresponse = pageObj.endpoints().Api2SubscriptionPurchase(token, PlanID,
				dataSet.get("client"), dataSet.get("secret"), spPrice, endDateTime);
		Assert.assertEquals(purchaseSubscriptionresponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		String subscription_id = purchaseSubscriptionresponse.jsonPath().get("subscription_id").toString();
		logger.info(iFrameEmail + " purchased " + subscription_id + " Plan id = " + PlanID);

		Response resp = pageObj.endpoints().posRedemptionOfSubscription(iFrameEmail, date, subscription_id, key, txn,
				dataSet.get("locationkey"), "8", "101");
		Assert.assertEquals(resp.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for pos redemption api");

		Assert.assertEquals(resp.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for pos redemption api");

		Response resp1 = pageObj.endpoints().posRedemptionOfSubscription(iFrameEmail, date, subscription_id, key, txn,
				dataSet.get("locationkey"), "8", "101");
		Assert.assertEquals(resp1.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY, "Status code 422 did not matched for pos redemption api");

		logger.info("Verified that user is not able to redemption of subscription plan in second attempt ");
		TestListeners.extentTest.get()
				.pass("Verified that user is not able to redemption of subscription plan in second attempt ");
	}

	@Test(description = "SQ-T2798 Validate The Renew subscription plan", groups = { "regression",
			"dailyrun" }, priority = 1)
	@Owner(name = "Shashank Sharma")
	public void T2798_ValidateRenewalSubscriptionPlan() throws InterruptedException {

		logger.info("== START -- T2798 Validate The Renew subscription plan ==");
		spName = dataSet.get("subscriptionName");
		// QCname = "QcSubscription_" + CreateDateTime.getTimeDateString();
		amountcap = dataSet.get("amountcap");
		unitDiscount = dataSet.get("unitDiscount");
		spPrice = dataSet.get("spPrice");
		int expSpPrice = Integer.parseInt(spPrice);

		//pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		//pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		PlanID = dataSet.get("subscriptionPlanID");
		System.out.println("PlanID-- " + PlanID);

		// user signup using api2
		iFrameEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(iFrameEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertEquals(signUpResponse.jsonPath().get("user.communicable_email").toString(),
				iFrameEmail.toLowerCase());
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		// subscription purchase api 2
		Response purchaseSubscriptionresponse = pageObj.endpoints().Api2SubscriptionPurchase(token, PlanID,
				dataSet.get("client"), dataSet.get("secret"), spPrice, endDateTime);
		Assert.assertEquals(purchaseSubscriptionresponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		String subscription_id = purchaseSubscriptionresponse.jsonPath().get("subscription_id").toString();

		pageObj.instanceDashboardPage().navigateToGuestTimeline(iFrameEmail);
		pageObj.guestTimelinePage().navigateToTabs("Subscriptions");

		String actualSubscriptionPlanName = pageObj.guestTimelinePage().getSubscriptionPlansFromTimeline();
		Assert.assertEquals(actualSubscriptionPlanName, spName);
		logger.info("Verified the subscription name " + spName + " on timeline subscription page");
		TestListeners.extentTest.get()
				.pass("Verified the subscription name " + spName + " on timeline subscription page");

		int actualSbuscriptionPlanPrice = pageObj.guestTimelinePage().getSubscriptionPlanPriceFromTimeline();
		Assert.assertEquals(actualSbuscriptionPlanPrice, expSpPrice);
		logger.info(
				"Verified the subscription price " + actualSbuscriptionPlanPrice + " on timeline subscription page");
		TestListeners.extentTest.get().pass(
				"Verified the subscription price " + actualSbuscriptionPlanPrice + " on timeline subscription page");

		Response renewalSubscriptionresponse = pageObj.endpoints().Api2SubscriptionPurchaseRenew(dataSet.get("apiKey"),
				subscription_id, dataSet.get("client"), dataSet.get("secret"), spPrice, "");

		Assert.assertEquals(renewalSubscriptionresponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean isDashboardSubscriptionRenewSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.authPurchaseSubscriptionSchema, renewalSubscriptionresponse.asString());
		Assert.assertTrue(isDashboardSubscriptionRenewSchemaValidated,
				"Platform Functions Renew Subscription Schema Validation failed");
		int renewed_subscription_id = Integer
				.parseInt(renewalSubscriptionresponse.jsonPath().get("subscription_id").toString());
		pageObj.driver.navigate().refresh();
		int actualSubscriptionID = pageObj.guestTimelinePage().getSubscriptionRenewID();

		Assert.assertEquals(actualSubscriptionID, renewed_subscription_id);

		logger.info("Verified the subscription renew ID  " + actualSubscriptionID + " on timeline subscription page");
		TestListeners.extentTest.get()
				.pass("Verified the subscription renew ID  " + actualSubscriptionID + " on timeline subscription page");

		logger.info("== END -- T2798 Validate The Renew subscription plan ==");

	}

	@Test(groups = { "regression", "dailyrun" }, description = "SQ-T2797 Validate The Purchase subscription plan"
			+ "SQ-T2794- Create subscription plan"
			+ "LS-T63/ SQ-T3927 - Verify the plan_image_url in user_subscription API"
			+ "LS-T68 / SQ-T3928 Verify the plan_id, plan_name and plan_description are coming in the API redemptions"
			+ "LS-T69 /SQ-T3929 Verify that plan_image_url, subscription_id and redemption_type appear in the api response"
			+ "LS-T39/SQ-T3926	Verify that \"Enable Subscription\" option is available in Cockpit>Dashboard>Major Features"
			+ "SQ-T3994 Verify the User Subscription response for active subscriptions, priority", priority = 0)
	@Owner(name = "Shashank Sharma")
	public void T2797_ValidatePurchaseSubscriptionPlan() throws InterruptedException {
		logger.info("== START of T2797:- Validate The Purchase subscription plan ==");

		spName = dataSet.get("subscriptionName");
		// QCname = "QcSubscription_" + CreateDateTime.getTimeDateString();
		amountcap = dataSet.get("amountcap");
		unitDiscount = dataSet.get("unitDiscount");
		spPrice = dataSet.get("spPrice");

		int expSpPrice = Integer.parseInt(spPrice);

		//pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		//pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// set Payment Adapter in subscription page
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Subscriptions");
		pageObj.dashboardpage().setPaymentAdapterInSubscriptionPage("No Adapter");
		pageObj.dashboardpage().clickOnUpdateButton();

		PlanID = dataSet.get("subscriptionPlanID");
		// user signup using api2
		iFrameEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(iFrameEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertEquals(signUpResponse.jsonPath().get("user.communicable_email").toString(),
				iFrameEmail.toLowerCase());
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		// subscription purchase api 2
		int counter = 0;
		boolean flag = false;
		Response purchaseSubscriptionresponse;
		do {
			purchaseSubscriptionresponse = pageObj.endpoints().Api2SubscriptionPurchase(token, PlanID,
					dataSet.get("client"), dataSet.get("secret"), spPrice, endDateTime);
			try {
				Assert.assertEquals(purchaseSubscriptionresponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
				flag = true;
				break;
			} catch (AssertionError ae) {
				flag = false;
				counter++;
				selUtils.longWait(1000);
			}
		} while (flag || counter <= 10);

//		Response purchaseSubscriptionresponse = pageObj.endpoints().Api2SubscriptionPurchase(token, PlanID,
//				dataSet.get("client"), dataSet.get("secret"), spPrice, endDateTime);

		Assert.assertEquals(purchaseSubscriptionresponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		String subscription_id = purchaseSubscriptionresponse.jsonPath().get("subscription_id").toString();
		logger.info(iFrameEmail + " purchased " + subscription_id + " Plan id = " + PlanID);

		Response apiV1RedemptionResponse = pageObj.endpoints().apiV1Redemption(iFrameEmail, subscription_id,
				dataSet.get("moesPunchhAppKey"), "password@123");

		Assert.assertEquals(apiV1RedemptionResponse.statusCode(), ApiConstants.HTTP_STATUS_CREATED, "V1 Redemption API is not working ");
		boolean isApiV1RedemptionSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiV1RedemptionSchema, apiV1RedemptionResponse.asString());
		Assert.assertTrue(isApiV1RedemptionSchemaValidated, "API v1 Redemption Schema Validation failed");
		String actual_plan_id = apiV1RedemptionResponse.jsonPath().getString("subscription_plan_id").replace("[", "")
				.replace("]", "");
		String actual_plan_name = apiV1RedemptionResponse.jsonPath().getString("subscription_plan_name")
				.replace("[", "").replace("]", "");
		String actual_plan_image_url = apiV1RedemptionResponse.jsonPath().getString("subscription_plan_image_url")
				.replace("[", "").replace("]", "");
		String actual_subscription_id = apiV1RedemptionResponse.jsonPath().getString("subscription_id").replace("[", "")
				.replace("]", "");
		String actual_redemption_type = apiV1RedemptionResponse.jsonPath().getString("redemption_type").replace("[", "")
				.replace("]", "");

		Assert.assertEquals(actual_plan_id, PlanID,
				" Actual plan id " + actual_plan_id + " is not matched with the expected plan id " + PlanID);

		Assert.assertEquals(actual_plan_name, spName,
				" Actual plan Name " + actual_plan_name + " is not matched with the expected plan name " + spName);

		Assert.assertEquals(actual_subscription_id, subscription_id, " Actual plan ID " + actual_subscription_id
				+ " is not matched with the expected plan ID  " + subscription_id);

		Assert.assertEquals(actual_redemption_type, "SubscriptionRedemption", " Actual redemption type "
				+ actual_redemption_type + " is not matched with the expected SubscriptionRedemption ");

		Assert.assertTrue(actual_plan_image_url.startsWith("https://res.cloudinary.com/punchh/image/upload"));
		Assert.assertTrue(actual_plan_image_url.endsWith(".png"));

		Response userSubscriptionResponse = pageObj.endpoints().Api2UserSubscription(token, dataSet.get("client"),
				dataSet.get("secret"));

		String actualPlan_image_url = userSubscriptionResponse.jsonPath().getString("subscriptions.plan_image_url")
				.replace("[", "").replace("]", "");

		Assert.assertTrue(actualPlan_image_url.startsWith("https://res.cloudinary.com/punchh/image"),
				"Image URL is not visible in user_subscription response");

		Assert.assertTrue(actualPlan_image_url.endsWith(".png"),
				"Image URL is not visible in user_subscription response");

		pageObj.instanceDashboardPage().navigateToGuestTimeline(iFrameEmail);
		pageObj.guestTimelinePage().navigateToTabs("Subscriptions");

		String actualSubscriptionPlanName = pageObj.guestTimelinePage().getSubscriptionPlansFromTimeline();
		Assert.assertEquals(actualSubscriptionPlanName, spName);
		logger.info("Verified the subscription name " + spName + " on timeline subscription page");
		TestListeners.extentTest.get()
				.pass("Verified the subscription name " + spName + " on timeline subscription page");

		int actualSbuscriptionPlanPrice = pageObj.guestTimelinePage().getSubscriptionPlanPriceFromTimeline();
		Assert.assertEquals(actualSbuscriptionPlanPrice, expSpPrice);
		logger.info(
				"Verified the subscription price " + actualSbuscriptionPlanPrice + " on timeline subscription page");
		TestListeners.extentTest.get().pass(
				"Verified the subscription price " + actualSbuscriptionPlanPrice + " on timeline subscription page");
		logger.info("== END of T2797:- Validate The Purchase subscription plan ==");
	}

	@Test(description = "SQ-T3551 Verify the Subscription plan is purchased with no adapter "
			+ "SQ-T3552 Verify the Subscription plan is renewed with no adapter", groups = { "regression",
					"dailyrun" }, priority = 1)
	@Owner(name = "Shashank Sharma")
	public void T3551_ValidateRenewalSubscriptionPlanWithNoAdapter() throws InterruptedException {

		logger.info("== START -- SQ-T3551 Verify the Subscription plan is purchased with no adapter  ==");
		spName = dataSet.get("subscriptionName");
		QCname = dataSet.get("QCname");
		amountcap = dataSet.get("amountcap"); // //Integer.toString(Utilities.getRandomNoFromRange(10, 200));
		unitDiscount = dataSet.get("unitDiscount"); // Integer.toString(Utilities.getRandomNoFromRange(1, 10));
		spPrice = dataSet.get("spPrice"); // Integer.toString(Utilities.getRandomNoFromRange(300, 500));
		int expSpPrice = Integer.parseInt(spPrice);
		PlanID = dataSet.get("subscriptionPlanID");

		//pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		//pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

//		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Subscriptions");
//		// set Payment Adapter in subscription page
//		pageObj.dashboardpage().setPaymentAdapterInSubscriptionPage("No Adapter");
//		pageObj.dashboardpage().clickOnUpdateButton();
//		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");
//		pageObj.qualificationcriteriapage().createQualificationCriteria(QCname, amountcap,
//				dataSet.get("qcFucntionName"), unitDiscount, true, dataSet.get("lineItemSelectorName"));
//
//		pageObj.menupage().clickSubscriptionsMenuIcon();
//		pageObj.menupage().clickSubscriptionPlansLink();
//		pageObj.subscriptionPlansPage().createSubscriptionPlan(spName, spPrice, QCname, dataSet.get("qcFucntionName"),
//				GlobalBenefitRedemptionThrottlingToggle, endDateTime);
//		PlanID = pageObj.subscriptionPlansPage().getSbuscriptionPlanID(spName);

		// user signup using api2
		iFrameEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(iFrameEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertEquals(signUpResponse.jsonPath().get("user.communicable_email").toString(),
				iFrameEmail.toLowerCase());
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		// subscription purchase api 2
		Response purchaseSubscriptionresponse = pageObj.endpoints().Api2SubscriptionPurchase(token, PlanID,
				dataSet.get("client"), dataSet.get("secret"), spPrice, endDateTime);
		Assert.assertEquals(purchaseSubscriptionresponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		String subscription_id = purchaseSubscriptionresponse.jsonPath().get("subscription_id").toString();

		pageObj.instanceDashboardPage().navigateToGuestTimeline(iFrameEmail);
		pageObj.guestTimelinePage().navigateToTabs("Subscriptions");

		String actualSubscriptionPlanName = pageObj.guestTimelinePage().getSubscriptionPlansFromTimeline();
		Assert.assertEquals(actualSubscriptionPlanName, spName);
		logger.info("Verified the subscription name " + spName + " on timeline subscription page");
		TestListeners.extentTest.get()
				.pass("Verified the subscription name " + spName + " on timeline subscription page");

		int actualSbuscriptionPlanPrice = pageObj.guestTimelinePage().getSubscriptionPlanPriceFromTimeline();
		Assert.assertEquals(actualSbuscriptionPlanPrice, expSpPrice);
		logger.info(
				"Verified the subscription price " + actualSbuscriptionPlanPrice + " on timeline subscription page");
		TestListeners.extentTest.get().pass(
				"Verified the subscription price " + actualSbuscriptionPlanPrice + " on timeline subscription page");

		Response renewalSubscriptionresponse = pageObj.endpoints().Api2SubscriptionPurchaseRenew(dataSet.get("apiKey"),
				subscription_id, dataSet.get("client"), dataSet.get("secret"), spPrice, "");

		Assert.assertEquals(renewalSubscriptionresponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		int renewed_subscription_id = Integer
				.parseInt(renewalSubscriptionresponse.jsonPath().get("subscription_id").toString());
		pageObj.driver.navigate().refresh();
		int actualSubscriptionID = pageObj.guestTimelinePage().getSubscriptionRenewID();

		Assert.assertEquals(actualSubscriptionID, renewed_subscription_id);

		logger.info("Verified the subscription renew ID  " + actualSubscriptionID + " on timeline subscription page");
		TestListeners.extentTest.get()
				.pass("Verified the subscription renew ID  " + actualSubscriptionID + " on timeline subscription page");

		logger.info("== END -- T2798 Validate The Renew subscription plan ==");

	}

	// Anant
	@Test(description = "SQ-T4342 Purchase multi use subscription with auto renewal as True/False", groups = {
			"regression", "dailyrun" })
	@Owner(name = "Vansham Mishra")
	public void T4342_purchaseMultiUseSubcription() throws InterruptedException {
		//pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		//pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		spName = dataSet.get("subscriptionName");
		amountcap = dataSet.get("amountcap");
		unitDiscount = dataSet.get("unitDiscount");
		spPrice = dataSet.get("spPrice");

		PlanID = dataSet.get("subscriptionPlanID");
		System.out.println("PlanID-- " + PlanID);

		// user signup using api2
		iFrameEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(iFrameEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertEquals(signUpResponse.jsonPath().get("user.communicable_email").toString(),
				iFrameEmail.toLowerCase());
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		String flag = "true";

		Response purchaseSubscriptionresponse2 = pageObj.endpoints().ApiSubscriptionPurchase(token, PlanID,
				dataSet.get("client"), dataSet.get("secret"), spPrice, endDateTime, flag);
		Assert.assertEquals(purchaseSubscriptionresponse2.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);

		// subscription purchase api 2

		flag = "false";
		Response purchaseSubscriptionresponse = pageObj.endpoints().Api2SubscriptionPurchaseAutorenewal(token, PlanID,
				dataSet.get("client"), dataSet.get("secret"), spPrice, endDateTime, flag);
		Assert.assertEquals(purchaseSubscriptionresponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);

	}

	// Anant
	@Test(description = "SQ-T4355 Verify search on the Subscription listing page based on Plan Name", groups = {
			"regression", "dailyrun" })
	@Owner(name = "Vansham Mishra")
	public void T4355_searchSubscriptionListingPage() throws InterruptedException {
		//pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		//pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Subscriptions", "Subscription Plans");

		pageObj.subscriptionPlansPage().searchSubscriptionPlan(dataSet.get("validPlan"));
		boolean present = pageObj.subscriptionPlansPage().getTopPlan(dataSet.get("validPlan"));
		Assert.assertTrue(present, dataSet.get("validPlan") + " subscription plan is not visible");
		logger.info(dataSet.get("validPlan") + " subscription plan is visible");
		TestListeners.extentTest.get().pass(dataSet.get("validPlan") + " subscription plan is visible");

		pageObj.subscriptionPlansPage().searchSubscriptionPlan(dataSet.get("inValidPlan"));
		String heading = pageObj.subscriptionPlansPage().getNoPlanHeading();
		heading = heading.trim();
		Assert.assertEquals(heading, "You do not have any subscription plans. Click on New Plan to create one!",
				"heading did not match");
		logger.info("when invalid subscription plan name is search correct heading is visible");
		TestListeners.extentTest.get().pass("when invalid subscription plan name is search correct heading is visible");
	}

	// shashank
	@Test(description = "LS-T186 Renew the single use subscription with auto renewal as True ( :single_use flag value should be true for the subscription plan in plan's preferences.)   ", groups = {
			"regression", "dailyrun" })
	@Owner(name = "Shashank Sharma")
	public void T186_VerifyrSingleUseSubscriptionAsTrueNotHappenWithAutoRenewalAsTrue() throws InterruptedException {

		spName = dataSet.get("subscriptionName");
		amountcap = dataSet.get("amountcap");
		unitDiscount = dataSet.get("unitDiscount");
		spPrice = dataSet.get("spPrice");

		PlanID = dataSet.get("subscriptionPlanID");
		System.out.println("PlanID-- " + PlanID);

		// user signup using api2
		iFrameEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(iFrameEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertEquals(signUpResponse.jsonPath().get("user.communicable_email").toString(),
				iFrameEmail.toLowerCase());
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		String flag = "false";

		Response purchaseSubscriptionresponse2 = pageObj.endpoints().ApiSubscriptionPurchase(token, PlanID,
				dataSet.get("client"), dataSet.get("secret"), spPrice, endDateTime, flag);
		Assert.assertEquals(purchaseSubscriptionresponse2.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);

		Response purchaseSubscriptionresponseRenewal = pageObj.endpoints().Api2SubscriptionPurchaseAutorenewal(token,
				PlanID, dataSet.get("client"), dataSet.get("secret"), spPrice, endDateTime, "true");

		Assert.assertEquals(purchaseSubscriptionresponseRenewal.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"For auto renewal API ,User should get the error message for single use subscription ");

		String actualErrorMessage = purchaseSubscriptionresponseRenewal.jsonPath().getString("errors.base")
				.replace("[", "").replace("]", "");
		String expectedErrorMessage = dataSet.get("expectedErrorMessage");
		Assert.assertEquals(actualErrorMessage, expectedErrorMessage,
				actualErrorMessage + " is not equal to " + expectedErrorMessage);
		logger.info(actualErrorMessage + " is matched with  " + expectedErrorMessage);
		TestListeners.extentTest.get().pass(actualErrorMessage + " is matched with  " + expectedErrorMessage);

	}

//	@Author = Hardik
	@Test(description = "SQ-T4338 Purchase single use subscription with auto renewal as True", groups = { "regression",
			"dailyrun" })
	@Owner(name = "Hardik Bhardwaj")
	public void T4338_VerifyrAutoRenewTrue() throws InterruptedException {

		spName = dataSet.get("subscriptionName");
		amountcap = dataSet.get("amountcap");
		unitDiscount = dataSet.get("unitDiscount");
		spPrice = dataSet.get("spPrice");

		PlanID = dataSet.get("subscriptionPlanID");
		logger.info("PlanID-- " + PlanID);
		pageObj.utils().logit("PlanID-- " + PlanID);

		// Signup using mobile api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token = signUpResponse.jsonPath().get("auth_token.token").toString();
		String authToken = signUpResponse.jsonPath().get("authentication_token").toString();
		String userID = signUpResponse.jsonPath().get("id").toString();

		String flag = "true";

		// Subscription Purchase - Secure Api
		Response purchaseSubscriptionresponse2 = pageObj.endpoints().ApiSubscriptionPurchase(token, PlanID,
				dataSet.get("client"), dataSet.get("secret"), spPrice, endDateTime, flag);
		Assert.assertEquals(purchaseSubscriptionresponse2.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for Secure Api for Subscription Purchase");
		Assert.assertEquals(purchaseSubscriptionresponse2.jsonPath().getString("base[0]"),
				"This is a single use subscription and cannot be renewed automatically. Please check request to send 'auto_renewal' as 'false'.");
		logger.info(
				"Secure API should throw an error as - This is a single use subscription and cannot be renewed automatically. Please check request to send auto_renewal as false with 422 error code");
		TestListeners.extentTest.get().pass(
				"Secure API should throw an error as - This is a single use subscription and cannot be renewed automatically. Please check request to send auto_renewal as false with 422 error code");

		// Subscription Purchase - Mobile Api
		Response purchaseSubscriptionresponse = pageObj.endpoints().Api2SubscriptionPurchaseAutorenewal(token, PlanID,
				dataSet.get("client"), dataSet.get("secret"), spPrice, endDateTime, flag);
		Assert.assertEquals(purchaseSubscriptionresponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for Mobile Api for Subscription Purchase");
		Assert.assertEquals(purchaseSubscriptionresponse.jsonPath().getString("errors.base[0]"),
				"This is a single use subscription and cannot be renewed automatically. Please check request to send 'auto_renewal' as 'false'.");
		logger.info(
				"Mobile API should throw an error as - This is a single use subscription and cannot be renewed automatically. Please check request to send auto_renewal as false with 422 error code");
		TestListeners.extentTest.get().pass(
				"Mobile API should throw an error as - This is a single use subscription and cannot be renewed automatically. Please check request to send auto_renewal as false with 422 error code");

		// Subscription Purchase - Auth Api
		String startDateTime = CreateDateTime.getYesterdaysDate() + " 01:00:00";
		Response authSubscriptionresponse2 = pageObj.endpoints().authApiSubscriptionPurchase(authToken, PlanID,
				dataSet.get("client"), dataSet.get("secret"), spPrice, startDateTime, endDateTime, flag);
		Assert.assertEquals(authSubscriptionresponse2.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for Auth Api for Subscription Purchase");
		Assert.assertEquals(authSubscriptionresponse2.jsonPath().getString("errors.base[0]"),
				"This is a single use subscription and cannot be renewed automatically. Please check request to send 'auto_renewal' as 'false'.");
		logger.info(
				"Auth API should throw an error as - This is a single use subscription and cannot be renewed automatically. Please check request to send auto_renewal as false with 422 error code");
		TestListeners.extentTest.get().pass(
				"Auth API should throw an error as - This is a single use subscription and cannot be renewed automatically. Please check request to send auto_renewal as false with 422 error code");

		// Subscription Purchase - Dashboard Api
		Response dashboardPurchaseSubscriptionresponse = pageObj.endpoints().dashboardSubscriptionPurchase(
				dataSet.get("apiKey"), PlanID, dataSet.get("client"), dataSet.get("secret"), spPrice, endDateTime, flag,
				userID);
		Assert.assertEquals(dashboardPurchaseSubscriptionresponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for Dashboard Api for Subscription Purchase");
		Assert.assertEquals(dashboardPurchaseSubscriptionresponse.jsonPath().getString("errors.base[0]"),
				"This is a single use subscription and cannot be renewed automatically. Please check request to send 'auto_renewal' as 'false'.");
		logger.info(
				"Dashboard API should throw an error as - This is a single use subscription and cannot be renewed automatically. Please check request to send auto_renewal as false with 422 error code");
		TestListeners.extentTest.get().pass(
				"Dashboard API should throw an error as - This is a single use subscription and cannot be renewed automatically. Please check request to send auto_renewal as false with 422 error code");

	}

//	@Author = Hardik
	@Test(description = "SQ-T4339 Purchase single use subscription with auto renewal as False", groups = { "regression",
			"dailyrun" })
	@Owner(name = "Hardik Bhardwaj")
	public void T4339_VerifyrAutoRenewFalse() throws InterruptedException {

		spName = dataSet.get("subscriptionName");
		amountcap = dataSet.get("amountcap");
		unitDiscount = dataSet.get("unitDiscount");
		spPrice = dataSet.get("spPrice");

		PlanID = dataSet.get("subscriptionPlanID");
		logger.info("PlanID-- " + PlanID);
		pageObj.utils().logit("PlanID-- " + PlanID);

		// Signup using mobile api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token = signUpResponse.jsonPath().get("auth_token.token").toString();

		String flag = "true";

		// Subscription Purchase - Secure Api
		Response purchaseSubscriptionresponse2 = pageObj.endpoints().ApiSubscriptionPurchase(token, PlanID,
				dataSet.get("client"), dataSet.get("secret"), spPrice, endDateTime, flag);
		Assert.assertEquals(purchaseSubscriptionresponse2.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for Secure Api for Subscription Purchase");
		logger.info("Secure API - User will be able to purchase the subscription plan successfully");
		TestListeners.extentTest.get()
				.pass("Secure API - User will be able to purchase the subscription plan successfully");

		// Signup using mobile api
		String userEmail1 = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse1 = pageObj.endpoints().Api1UserSignUp(userEmail1, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token1 = signUpResponse1.jsonPath().get("auth_token.token").toString();

		// Subscription Purchase - Mobile Api
		Response purchaseSubscriptionresponse = pageObj.endpoints().Api2SubscriptionPurchaseAutorenewal(token1, PlanID,
				dataSet.get("client"), dataSet.get("secret"), spPrice, endDateTime, flag);
		Assert.assertEquals(purchaseSubscriptionresponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for Mobile Api for Subscription Purchase");
		logger.info("Mobile API - User will be able to purchase the subscription plan successfully");
		TestListeners.extentTest.get()
				.pass("Mobile API - User will be able to purchase the subscription plan successfully");

		// Signup using mobile api
		String userEmail2 = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse2 = pageObj.endpoints().Api1UserSignUp(userEmail2, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String authToken = signUpResponse2.jsonPath().get("authentication_token").toString();

		// Subscription Purchase - Auth Api
		String startDateTime = CreateDateTime.getYesterdaysDate() + " 01:00:00";
		Response authSubscriptionresponse2 = pageObj.endpoints().authApiSubscriptionPurchase(authToken, PlanID,
				dataSet.get("client"), dataSet.get("secret"), spPrice, startDateTime, endDateTime, flag);
		Assert.assertEquals(authSubscriptionresponse2.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for Auth Api for Subscription Purchase");
		logger.info("Auth API - User will be able to purchase the subscription plan successfully");
		TestListeners.extentTest.get()
				.pass("Auth API - User will be able to purchase the subscription plan successfully");

		// Signup using mobile api
		String userEmail3 = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse3 = pageObj.endpoints().Api1UserSignUp(userEmail3, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String userID = signUpResponse3.jsonPath().get("id").toString();

		// Subscription Purchase - Dashboard Api
		Response dashboardPurchaseSubscriptionresponse = pageObj.endpoints().dashboardSubscriptionPurchase(
				dataSet.get("apiKey"), PlanID, dataSet.get("client"), dataSet.get("secret"), spPrice, endDateTime, flag,
				userID);
		Assert.assertEquals(dashboardPurchaseSubscriptionresponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for Dashboard Api for Subscription Purchase");
		logger.info("Dashboard API - User will be able to purchase the subscription plan successfully");
		TestListeners.extentTest.get()
				.pass("Dashboard API - User will be able to purchase the subscription plan successfully");

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