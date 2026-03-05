package com.punchh.server.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
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
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.apiConfig.ApiResponseJsonSchema;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

import static com.punchh.server.utilities.SingletonDBUtils.utils;

/*
 * @Author : Hardik Bhardwaj
 */

@Listeners(TestListeners.class)
public class POSVoidBatchedRedemptionAPITest {
	static Logger logger = LogManager.getLogger(POSVoidBatchedRedemptionAPITest.class);
	public WebDriver driver;
	private String userEmail;
	private PageObj pageObj;
	private String sTCName;
	private String env, run = "ui";
	private String baseUrl;
	private static Map<String, String> dataSet;
	private boolean GlobalBenefitRedemptionThrottlingToggle;
	private List<String> codeNameList;
	Properties prop;

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
		prop = Utilities.loadPropertiesFile("segmentBeta.properties");
		// env = prop.getProperty("environment");
		driver = new BrowserUtilities().launchBrowser();
		sTCName = method.getName();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==>" + dataSet);
		GlobalBenefitRedemptionThrottlingToggle = false;
		codeNameList = new ArrayList<String>();

	}

	@Test(description = "SQ-T3286 Step-2 [Batched Redemptions-OMM-T728(495)] Verify dashboard / DB logic when redemption is done Void (POS Void Batched Redemption API); "
			+ "SQ-T5561: Verify Void Redemption POS API Negative Scenarios", groups = { "regression", "dailyrun" })
	@Owner(name = "Hardik Bhardwaj")
	public void T3286_AddRewardDiscountToBasket_Step2() throws Exception {

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("API2 Signup is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUserWithEndDate(userID, dataSet.get("apiKey"),
				"", dataSet.get("redeemable_id"), "", "", "");
		pageObj.utils().logPass("Send redeemable to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		// get reward id
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id"));

		pageObj.utils().logPass("Reward id " + rewardId + " is generated successfully ");

		// Add reward in basket
		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId);

		Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with add discount to basket ");
		pageObj.utils().logPass("AUTH add discount to basket is successful");

		// Auth Process Batch Redemption
		Response batchRedemptionProcessResponse = pageObj.endpoints().processBatchRedemptionOfBasketAUTHAPI(
				dataSet.get("client"), dataSet.get("secret"), dataSet.get("locationkey"), token, userID, "101");
		Assert.assertEquals(batchRedemptionProcessResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Process Batch Redemption ");
		String redemption_ref1 = batchRedemptionProcessResponse.jsonPath().get("redemption_ref").toString();
		System.out.println("POS Process Batch Redemption api is working properly and redemption refrence code is - "
				+ redemption_ref1);
		pageObj.utils().logPass("Auth Process Batch Redemption Api is successful");

		// POS void redemption with invalid user Id
		pageObj.utils().logit("== POS API void redemption with invalid user Id ==");
		Response voidRedemptionInvalidUserIdResponse = pageObj.endpoints().voidProcessBatchRedemptionOfBasketPOSAPI(
				dataSet.get("client"), dataSet.get("secret"), "123", dataSet.get("locationkey"), redemption_ref1);
		Assert.assertEquals(voidRedemptionInvalidUserIdResponse.getStatusCode(), ApiConstants.HTTP_STATUS_NOT_FOUND);
		boolean isPosVoidRedemptionInvalidUserIdSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, voidRedemptionInvalidUserIdResponse.asString());
		Assert.assertTrue(isPosVoidRedemptionInvalidUserIdSchemaValidated,
				"POS void redemption schema validation failed");
		String voidRedemptionInvalidUserIdMsg = voidRedemptionInvalidUserIdResponse.jsonPath().getString("error");
		Assert.assertEquals(voidRedemptionInvalidUserIdMsg, "User not found.");
		pageObj.utils().logPass("POS API void redemption with invalid user Id call is unsuccessful");

		// POS void redemption with missing user Id
		pageObj.utils().logit("== POS API void redemption with missing user Id ==");
		Response voidRedemptionMissingUserIdResponse = pageObj.endpoints().voidProcessBatchRedemptionOfBasketPOSAPI(
				dataSet.get("client"), dataSet.get("secret"), "", dataSet.get("locationkey"), redemption_ref1);
		Assert.assertEquals(voidRedemptionMissingUserIdResponse.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST);
		boolean isPosVoidRedemptionMissingUserIdSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, voidRedemptionMissingUserIdResponse.asString());
		Assert.assertTrue(isPosVoidRedemptionMissingUserIdSchemaValidated,
				"POS void redemption schema validation failed");
		String voidRedemptionMissingUserIdMsg = voidRedemptionMissingUserIdResponse.jsonPath().getString("error");
		Assert.assertEquals(voidRedemptionMissingUserIdMsg,
				"Required parameter missing or the value is empty: user_id");
		pageObj.utils().logPass("POS API void redemption with missing user Id call is unsuccessful");

		// POS void redemption with invalid location key
		pageObj.utils().logit("== POS API void redemption with invalid location key ==");
		Response voidRedemptionInvalidLocationKeyResponse = pageObj.endpoints()
				.voidProcessBatchRedemptionOfBasketPOSAPI(dataSet.get("client"), dataSet.get("secret"), userID, "123",
						redemption_ref1);
		Assert.assertEquals(voidRedemptionInvalidLocationKeyResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED);
		boolean isPosVoidRedemptionInvalidLocationKeySchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, voidRedemptionInvalidLocationKeyResponse.asString());
		Assert.assertTrue(isPosVoidRedemptionInvalidLocationKeySchemaValidated,
				"POS void redemption schema validation failed");
		String voidRedemptionInvalidLocationKeyMsg = voidRedemptionInvalidLocationKeyResponse.jsonPath()
				.getString("[0]");
		Assert.assertEquals(voidRedemptionInvalidLocationKeyMsg, "Invalid LocationKey");
		pageObj.utils().logPass("POS API void redemption with invalid location key call is unsuccessful");

		// POS void redemption with invalid redemption reference
		pageObj.utils().logit("== POS API void redemption with invalid redemption reference ==");
		Response voidRedemptionInvalidRedemptionRefResponse = pageObj.endpoints()
				.voidProcessBatchRedemptionOfBasketPOSAPI(dataSet.get("client"), dataSet.get("secret"), userID,
						dataSet.get("locationkey"), "123");
		Assert.assertEquals(voidRedemptionInvalidRedemptionRefResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY);
		boolean isPosVoidRedemptionInvalidRedemptionRefSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, voidRedemptionInvalidRedemptionRefResponse.asString());
		Assert.assertTrue(isPosVoidRedemptionInvalidRedemptionRefSchemaValidated,
				"POS void redemption schema validation failed");
		String voidRedemptionInvalidRedemptionRefMsg = voidRedemptionInvalidRedemptionRefResponse.jsonPath()
				.getString("error");
		Assert.assertEquals(voidRedemptionInvalidRedemptionRefMsg, "No Basket found.");
		pageObj.utils().logPass("POS API void redemption with invalid redemption reference call is unsuccessful");

		// POS void redemption with valid details
		pageObj.utils().logit("== POS API void redemption with valid details ==");
		Response voidRedemptionResponse = pageObj.endpoints().voidProcessBatchRedemptionOfBasketPOSAPI(
				dataSet.get("client"), dataSet.get("secret"), userID, dataSet.get("locationkey"), redemption_ref1);
		Assert.assertEquals(voidRedemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_ACCEPTED,
				"Status code 200 did not match with Void Batch Redemption ");
		pageObj.utils().logPass("Verified the discount type and ID in Basket add response");

		String query = "Select status from discount_baskets where user_id = '" + userID + "'";
		pageObj.singletonDBUtilsObj();
		String statusColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "status");
		Assert.assertEquals(statusColValue, "3", "Value is not present at status column in discount basket ");
		pageObj.utils().logPass("Value is present at status column in discount basket ");

		String query1 = "Select status from discount_basket_items where user_id = '" + userID + "'";
		pageObj.singletonDBUtilsObj();
		String statusColValue1 = DBUtils.executeQueryAndGetColumnValue(env, query1, "status");
		Assert.assertEquals(statusColValue1, "0", "Value is not present at status column in discount basket ");
		pageObj.utils().logPass("Value is present at status column in discount basket ");

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// navigate to user timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		boolean title = pageObj.guestTimelinePage().verifyTitleFromTimeline("Void Honored Redemption");

		try {
			Assert.assertTrue(title, "Void Honored Redemption Title did not displayed...");
			pageObj.utils().logPass("Void Honored Redemption Title is displayed successfully on timeline");
		} catch (Exception e) {
			logger.error("Error in validating Void Honored Redemption Title on timeline" + e);
			TestListeners.extentTest.get().fail("Error in validating Void Honored Redemption Title on timeline" + e);
		}

		boolean title1 = pageObj.guestTimelinePage().verifyTitleFromTimeline("Redeemed Redemption");

		try {
			Assert.assertFalse(title1, "Redeemed Redemption Title did displayed...");
			pageObj.utils().logPass("Redeemed Redemption Title is not displayed successfully on timeline");
		} catch (Exception e) {
			logger.error("Error in validating Redeemed Redemption Title on timeline" + e);
			TestListeners.extentTest.get().fail("Error in validating Redeemed Redemption Title on timeline" + e);
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
		pageObj.utils().logPass("reward is reverted back to user account (Account History)");

		// get reward id
		String rewardId1 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id"));

		Assert.assertEquals(rewardId1, rewardId,
				"Reward is not moved to Live/Unredeemed section in Rewards tab (user timeline)");
		pageObj.utils().logPass("Reward moved to Live/Unredeemed section in Rewards tab (user timeline)");

//		DBUtils.closeConnection();
	}

	@Test(description = "SQ-T3286 Step-3 [Batched Redemptions-OMM-T728(495)] Verify dashboard / DB logic when redemption is done Void (POS Void Batched Redemption API)", groups = {
			"regression", "unstable", "dailyrun" })
	@Owner(name = "Hardik Bhardwaj")
	public void T3286_AddRedemptionCuponCodeIntoUserDiscountBasket_Step3() throws Exception {

		String coupanCampaignName = "Auto_CuponCampaign" + CreateDateTime.getTimeDateString();

		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().createWhatDetailsCoupanCampaign(coupanCampaignName);
		pageObj.signupcampaignpage().setCouponCampaignUsageType(dataSet.get("usageType"));
		pageObj.signupcampaignpage().setCouponCampaignCodeGenerationType(dataSet.get("codeType"));
		pageObj.signupcampaignpage().setCouponCampaignGuestUsagewithGiftAmount(dataSet.get("noOfGuests"),
				dataSet.get("usagePerGuest"), dataSet.get("giftType"), dataSet.get("amount"), "", "",
				GlobalBenefitRedemptionThrottlingToggle);

		pageObj.signupcampaignpage().setStartDate();
		pageObj.signupcampaignpage().setEndDateforCouponCampaign(1);

		pageObj.signupcampaignpage().createWhenDetailsCampaign();
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "Campaign created success message did not displayed....");
		pageObj.utils().logPass("Coupon campaign created successfuly");

		Thread.sleep(8000);
		pageObj.campaignspage().searchCampaign(coupanCampaignName);
		codeNameList = pageObj.campaignspage().getPreGeneratedCuponCodeList();
		int numberOfGuestExpected = Integer.parseInt(dataSet.get("noOfGuests"));
		Assert.assertEquals(codeNameList.size(), numberOfGuestExpected);
		String generatedCodeName = codeNameList.get(0).toString();// pageObj.campaignspage().getPreGeneratedCuponCode();

		// user create
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();

		// add basket
		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "redemption_code", generatedCodeName);

		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponse.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		pageObj.utils().logPass("AUTH add discount to basket is successful");

		// Auth Process Batch Redemption
		Response batchRedemptionProcessResponse = pageObj.endpoints().processBatchRedemptionOfBasketAUTHAPI(
				dataSet.get("client"), dataSet.get("secret"), dataSet.get("locationkey"), token, userID, "101");
		Assert.assertEquals(batchRedemptionProcessResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Process Batch Redemption ");
		String redemption_ref1 = batchRedemptionProcessResponse.jsonPath().get("redemption_ref").toString();
		pageObj.utils().logit("POS Process Batch Redemption api is working properly and redemption refrence code is - "
				+ redemption_ref1);
		pageObj.utils().logPass("Auth Process Batch Redemption Api is successful");

		// POS void redemption
		Response voidRedemptionResponse = pageObj.endpoints().voidProcessBatchRedemptionOfBasketPOSAPI(
				dataSet.get("client"), dataSet.get("secret"), userID, dataSet.get("locationkey"), redemption_ref1);
		Assert.assertEquals(voidRedemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_ACCEPTED,
				"Status code 200 did not match with Void Batch Redemption ");
		pageObj.utils().logPass("Verified the discount type and ID in Basket add response");

		String query = "Select status from discount_baskets where user_id = '" + userID + "'";
		pageObj.singletonDBUtilsObj();
		String statusColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "status");
		Assert.assertEquals(statusColValue, "3", "Value is not present at status column in discount basket ");
		pageObj.utils().logPass("Value is present at status column in discount basket ");

		String query1 = "Select status from discount_basket_items where user_id = '" + userID + "'";
		pageObj.singletonDBUtilsObj();
		String statusColValue1 = DBUtils.executeQueryAndGetColumnValue(env, query1, "status");
		Assert.assertEquals(statusColValue1, "0", "Value is not present at status column in discount basket ");
		pageObj.utils().logPass("Value is present at status column in discount basket ");

		// navigate to user timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		boolean title = pageObj.guestTimelinePage().verifyTitleFromTimeline("Redeemed Redemption");

		try {
			Assert.assertFalse(title, "Redeemed Redemption Title did displayed...");
			pageObj.utils().logPass("Redeemed Redemption Title is not displayed successfully on timeline");
		} catch (Exception e) {
			logger.error("Error in validating Redeemed Redemption Title on timeline" + e);
			TestListeners.extentTest.get().fail("Error in validating Redeemed Redemption Title on timeline" + e);
		}

		boolean title1 = pageObj.guestTimelinePage().verifyTitleFromTimeline("Void Honored Redemption");

		try {
			Assert.assertFalse(title1, "Void Honored Redemption Title did displayed...");
		pageObj.utils().logPass("Void Honored Redemption Title is not displayed successfully on timeline");
		} catch (Exception e) {
			logger.error("Error in validating Void Honored Redemption Title on timeline" + e);
			TestListeners.extentTest.get().fail("Error in validating Void Honored Redemption Title on timeline" + e);
		}
//		DBUtils.closeConnection();

	}

	@Test(description = "SQ-T3286 Step-5 [Batched Redemptions-OMM-T728(495)] Verify dashboard / DB logic when redemption is done Void (POS Void Batched Redemption API)", groups = {
			"regression", "dailyrun" })
	@Owner(name = "Hardik Bhardwaj")
	public void T3286_addDiscountAmountToDiscountBasket_Step5() throws Exception {
		String discountAmount = dataSet.get("discountAmount");
		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("API2 Signup is successful");
		pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), dataSet.get("discountAmount"), "", "", "");

		Utilities.longWait(2000);
		// Adding amount into discount basket
		Response discountBasketResponse = pageObj.endpoints().addDiscountAmountToDiscountBasket(token,
				dataSet.get("client"), dataSet.get("secret"), "discount_amount", discountAmount);
		Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with add discount to basket");
		pageObj.utils().logPass("AUTH add discount to basket is successful");

		// Auth Process Batch Redemption
		Response batchRedemptionProcessResponse = pageObj.endpoints().processBatchRedemptionOfBasketAUTHAPI(
				dataSet.get("client"), dataSet.get("secret"), dataSet.get("locationkey"), token, userID, "101");
		Assert.assertEquals(batchRedemptionProcessResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Process Batch Redemption ");
		String redemption_ref1 = batchRedemptionProcessResponse.jsonPath().get("redemption_ref").toString();
		System.out.println("POS Process Batch Redemption api is working properly and redemption refrence code is - "
				+ redemption_ref1);
		pageObj.utils().logPass("Auth Process Batch Redemption Api is successful");

		// POS void redemption
		Response voidRedemptionResponse = pageObj.endpoints().voidProcessBatchRedemptionOfBasketPOSAPI(
				dataSet.get("client"), dataSet.get("secret"), userID, dataSet.get("locationkey"), redemption_ref1);
		Assert.assertEquals(voidRedemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_ACCEPTED,
				"Status code 200 did not match with Void Batch Redemption ");
		pageObj.utils().logPass("Verified the discount type and ID in Basket add response");

		String query = "Select status from discount_baskets where user_id = '" + userID + "'";
		pageObj.singletonDBUtilsObj();
		String statusColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "status");
		Assert.assertEquals(statusColValue, "3", "Value is not present at status column in discount basket ");
		pageObj.utils().logPass("Value is present at status column in discount basket ");

		String query1 = "Select status from discount_basket_items where user_id = '" + userID + "'";
		pageObj.singletonDBUtilsObj();
		String statusColValue1 = DBUtils.executeQueryAndGetColumnValue(env, query1, "status");
		Assert.assertEquals(statusColValue1, "0", "Value is not present at status column in discount basket ");
		pageObj.utils().logPass("Value is present at status column in discount basket ");

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// navigate to user timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		boolean title = pageObj.guestTimelinePage().verifyTitleFromTimeline("Void Honored Redemption");

		try {
			Assert.assertTrue(title, "Void Honored Redemption Title did not displayed...");
			pageObj.utils().logPass("Void Honored Redemption Title is displayed successfully on timeline");
		} catch (Exception e) {
			logger.error("Error in validating Void Honored Redemption Title on timeline" + e);
			TestListeners.extentTest.get().fail("Error in validating Void Honored Redemption Title on timeline" + e);
		}

		boolean title1 = pageObj.guestTimelinePage().verifyTitleFromTimeline("Redeemed Redemption");

		try {
			Assert.assertFalse(title1, "Redeemed Redemption Title did displayed...");
			pageObj.utils().logPass("Redeemed Redemption Title is not displayed successfully on timeline");
		} catch (Exception e) {
			logger.error("Error in validating Redeemed Redemption Title on timeline" + e);
			TestListeners.extentTest.get().fail("Error in validating Redeemed Redemption Title on timeline" + e);
		}

		List<Object> obj = new ArrayList<Object>();
		String eventValue;
		int j = 0;
		// User Account History
		Response accountHistoryResponse = pageObj.endpoints().Api2UserAccountHistory(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, accountHistoryResponse.getStatusCode(),
				"Status code 200 did not matched for api2 point conversion");
		obj = accountHistoryResponse.jsonPath().getList("description");
		for (int i = 0; i < obj.size(); i++) {
			eventValue = accountHistoryResponse.jsonPath().getString("[" + i + "].event_value");
			if (eventValue.contains(dataSet.get("discountAmount"))) {
				i = j;
				break;
			}
		}

		String points = accountHistoryResponse.jsonPath().get("[" + j + "].total_banked_currency").toString();
		Assert.assertEquals(points, "5.0", "points is not reverted back to user account (Account History)");
		pageObj.utils().logPass("points is reverted back to user account (Account History)");
//		DBUtils.closeConnection();
	}

	// Hardik
	@SuppressWarnings("unused")
	@Test(description = "SQ-T3286 Step-11 [Batched Redemptions-OMM-T728(495)] Verify dashboard / DB logic when redemption is done Void (POS Void Batched Redemption API)", groups = {
			"regression", "dailyrun" })
	@Owner(name = "Hardik Bhardwaj")
	public void T3286_addFuelRewardToDiscountBasket_Step11() throws Exception {
		// create user
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();

		Response sendAmountResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "", "", "14",
				"");

		Response accountHistoryResponse = pageObj.endpoints().Api2UserAccountHistory(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, accountHistoryResponse.getStatusCode(),
				"Status code 200 did not matched for api2 point conversion");
		String amountBeforeVoidRedemption = accountHistoryResponse.jsonPath().get("[0].event_value").toString();
		pageObj.utils().logPass("Fuel amount before void redemption API is : " + amountBeforeVoidRedemption);

		// Adding fuel_reward into discount basket
		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "fuel_reward", dataSet.get("fuelAmount"));
		Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with add discount to basket ");
		pageObj.utils().logPass("AUTH add discount to basket is successful");

		// Auth Process Batch Redemption
		Response batchRedemptionProcessResponse = pageObj.endpoints().processBatchRedemptionOfBasketAUTHAPI(
				dataSet.get("client"), dataSet.get("secret"), dataSet.get("location_key"), token, userID, "101");
		Assert.assertEquals(batchRedemptionProcessResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Process Batch Redemption ");
		String redemption_ref1 = batchRedemptionProcessResponse.jsonPath().get("redemption_ref").toString();
		System.out.println("POS Process Batch Redemption api is working properly and redemption refrence code is - "
				+ redemption_ref1);
		pageObj.utils().logPass("Auth Process Batch Redemption Api is successful");

		// POS void redemption
		Response voidRedemptionResponse = pageObj.endpoints().voidProcessBatchRedemptionOfBasketPOSAPI(
				dataSet.get("client"), dataSet.get("secret"), userID, dataSet.get("location_key"), redemption_ref1);
		Assert.assertEquals(voidRedemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_ACCEPTED,
				"Status code 200 did not match with Void Batch Redemption ");
		pageObj.utils().logPass("Verified the discount type and ID in Basket add response");

		String query = "Select status from discount_baskets where user_id = '" + userID + "'";
		pageObj.singletonDBUtilsObj();
		String statusColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "status");
		Assert.assertEquals(statusColValue, "3", "Value is not present at status column in discount basket ");
		pageObj.utils().logPass("Value is present at status column in discount basket ");

		String query1 = "Select status from discount_basket_items where user_id = '" + userID + "'";
		pageObj.singletonDBUtilsObj();
		String statusColValue1 = DBUtils.executeQueryAndGetColumnValue(env, query1, "status");
		Assert.assertEquals(statusColValue1, "0", "Value is not present at status column in discount basket ");
		pageObj.utils().logPass("Value is present at status column in discount basket ");

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// navigate to user timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		boolean title = pageObj.guestTimelinePage().verifyTitleFromTimeline("Void Honored Redemption");

		Assert.assertTrue(title, "Void Honored Redemption Title did not displayed...");
		pageObj.utils().logPass("Void Honored Redemption Title is displayed successfully on timeline");

		boolean title1 = pageObj.guestTimelinePage().verifyTitleFromTimeline("Redeemed Redemption");

		Assert.assertFalse(title1, "Redeemed Redemption Title did displayed...");
		pageObj.utils().logPass("Redeemed Redemption Title is not displayed successfully on timeline");

		Response accountHistoryResponse1 = pageObj.endpoints().Api2UserAccountHistory(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, accountHistoryResponse1.getStatusCode(),
				"Status code 200 did not matched for api2 point conversion");
		String amountAfterVoidRedemption = accountHistoryResponse1.jsonPath().get("[0].event_value").toString();
		pageObj.utils().logPass("Fuel amount before void redemption API is : " + amountAfterVoidRedemption);

		Assert.assertEquals(amountAfterVoidRedemption, amountBeforeVoidRedemption,
				"Fuel amount before and after void redemption API is not same");
		pageObj.utils().logPass("Fuel amount before and after void redemption API is same");
//		DBUtils.closeConnection();
	}

	@Test(description = "SQ-T3286 [Batched Redemptions-OMM-T728(495)] Verify dashboard / DB logic when redemption is done Void (POS Void Batched Redemption API)", groups = {
			"regression", "dailyrun" })
	@Owner(name = "Hardik Bhardwaj")
	public void T3286_addCardCompletionToUserDiscountBasket_Step() throws Exception {

		String discountID = Utilities.getRandomNoFromRange(1, 4) + "";
		// create user
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();

		Response sendAmountResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "", "", "",
				"10");
		System.out.println("sendAmountResponse== " + sendAmountResponse.asPrettyString());

		// Adding subscription into discount basket
		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "card_completion", discountID);

		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponse.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		pageObj.utils().logPass("AUTH add discount to basket is successful");

		// Auth Process Batch Redemption
		Response batchRedemptionProcessResponse = pageObj.endpoints().processBatchRedemptionOfBasketAUTHAPI(
				dataSet.get("client"), dataSet.get("secret"), dataSet.get("locationkey"), token, userID, "101");
		Assert.assertEquals(batchRedemptionProcessResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Process Batch Redemption ");
		String redemption_ref1 = batchRedemptionProcessResponse.jsonPath().get("redemption_ref").toString();
		System.out.println("POS Process Batch Redemption api is working properly and redemption refrence code is - "
				+ redemption_ref1);
		pageObj.utils().logPass("Auth Process Batch Redemption Api is successful");

		// POS void redemption
		Response voidRedemptionResponse = pageObj.endpoints().voidProcessBatchRedemptionOfBasketPOSAPI(
				dataSet.get("client"), dataSet.get("secret"), userID, dataSet.get("locationkey"), redemption_ref1);
		Assert.assertEquals(voidRedemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_ACCEPTED,
				"Status code 200 did not match with Void Batch Redemption ");
		pageObj.utils().logPass("Verified the discount type and ID in Basket add response");

		String query = "Select status from discount_baskets where user_id = '" + userID + "'";
		pageObj.singletonDBUtilsObj();
		String statusColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "status");
		Assert.assertEquals(statusColValue, "3", "Value is not present at status column in discount basket ");
		pageObj.utils().logPass("Value is present at status column in discount basket ");

		String query1 = "Select status from discount_basket_items where user_id = '" + userID + "'";
		pageObj.singletonDBUtilsObj();
		String statusColValue1 = DBUtils.executeQueryAndGetColumnValue(env, query1, "status");
		Assert.assertEquals(statusColValue1, "0", "Value is not present at status column in discount basket ");
		pageObj.utils().logPass("Value is present at status column in discount basket ");

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// navigate to user timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		boolean title = pageObj.guestTimelinePage().verifyTitleFromTimeline("Void Honored Redemption");

		try {
			Assert.assertTrue(title, "Void Honored Redemption Title did not displayed...");
			pageObj.utils().logPass("Void Honored Redemption Title is displayed successfully on timeline");
		} catch (Exception e) {
			logger.error("Error in validating Void Honored Redemption Title on timeline" + e);
			TestListeners.extentTest.get().fail("Error in validating Void Honored Redemption Title on timeline" + e);
		}

		boolean title1 = pageObj.guestTimelinePage().verifyTitleFromTimeline("Redeemed Redemption");

		try {
			Assert.assertFalse(title1, "Redeemed Redemption Title did displayed...");
			pageObj.utils().logPass("Redeemed Redemption Title is not displayed successfully on timeline");
		} catch (Exception e) {
			logger.error("Error in validating Redeemed Redemption Title on timeline" + e);
			TestListeners.extentTest.get().fail("Error in validating Redeemed Redemption Title on timeline" + e);
		}

		Response accountHistoryResponse1 = pageObj.endpoints().Api2UserAccountHistory(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, accountHistoryResponse1.getStatusCode(),
				"Status code 200 did not matched for api2 point conversion");
		String amountAfterVoidRedemption = accountHistoryResponse1.jsonPath().get("[0].event_value").toString();
		pageObj.utils().logPass("Fuel amount before void redemption API is : " + amountAfterVoidRedemption);

//		DBUtils.closeConnection();
	}

	@Test(description = "SQ-T5798,Verify the POS user merge functionality for the business", groups = { "regression",
			"dailyrun" }, priority = 0)
	@Owner(name = "Vansham Mishra")
	public void T5798_VerifyPosMergeFunctionalityForBusiness() throws Exception {
		String b_id = dataSet.get("business_id");
		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		pageObj.singletonDBUtilsObj();
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");
		String status1 = "true";
		String status2 = "false";

		// set "autocreate_pos_user" flag -> true
		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, status1, dataSet.get("dbFlag1"),
				b_id);
		Assert.assertTrue(flag, dataSet.get("dbFlag1") + " value is not updated to " + status1);
		pageObj.utils().logit(dataSet.get("dbFlag1") + " value is updated to " + status1);

		// set "autocreate_pos_checkin" flag -> true
		boolean flag2 = DBUtils.updateBusinessesPreference(env, expColValue, status1, dataSet.get("dbFlag2"),
				b_id);
		Assert.assertTrue(flag2, dataSet.get("dbFlag2") + " value is not updated to " + status1);
		pageObj.utils().logit(dataSet.get("dbFlag2") + " value is updated to " + status1);

		// Step1 - Create a POS user by using a phone number
		long phone = (long) (Math.random() * Math.pow(10, 10));
		String phoneStr = Long.toString(phone);
		Response respo = pageObj.endpoints().posSignUpWithoutEmail(phone, dataSet.get("locationKey"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, respo.getStatusCode(), "Status code 200 did not matched for pos signup api");
		String posUserEmail = respo.jsonPath().get("email").toString();
		respo.jsonPath().get("id").toString();
		pageObj.utils().logPass("Pos user has been created successfully usng phone number");

		// Step2 - Perform 2 POS checkin for the user
		Response resp1 = pageObj.endpoints().posCheckin(posUserEmail, dataSet.get("locationkey"), "10");
		Assert.assertEquals(resp1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for post chekin api");
		pageObj.utils().logPass("Pos checkin of $10 is successful");

		// fetch checkin id from the user timeline of pos user
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.instanceDashboardPage().navigateToGuestTimeline(phoneStr);
		String checkinId = pageObj.guestTimelinePage().getCheckinId();
        Assert.assertNotNull(checkinId,"Checkin ID is null on POS user timeline");
		pageObj.utils().logit("checkin id displayed on the POS user timeline: " + checkinId);
		String PosGuestId = pageObj.guestTimelinePage().getGuestIdFromUrl();
        Assert.assertNotNull(PosGuestId,"Guest ID is null on POS user timeline");
		pageObj.utils().logPass("Pos user guest id is: " + PosGuestId);

		// step3 - Create a Mobile Loyalty user using the API
		// (do not use phone number for this step, use only email id)
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("API2 Signup is successful");

		// step4 - Update the phone number of the Loyalty user, by using the phone
		// number of the POS user created before
		// Use auth token for the created loyalty user in the third step
		Response updateGuestResponse = pageObj.endpoints().Api1MobileUpdateGuestDetailsWithoutEmail(dataSet.get("Npwd"),
				phoneStr, dataSet.get("client"), dataSet.get("secret"), token);
		Assert.assertEquals(updateGuestResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass(
				"Updated the phone number of the Loyalty user, by using the phone number of the POS user created");

		// fetch checkin id from the user timeline of loyalty user
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		String checkinId2 = pageObj.guestTimelinePage().getCheckinId();
        Assert.assertNotNull(checkinId2,"Checkin ID is null on Loyalty user timeline");
		pageObj.utils().logit("checkin id displayed on the POS user timeline: " + checkinId2);
		String LoyaltyGuestId = pageObj.guestTimelinePage().getGuestIdFromUrl();
        Assert.assertNotNull(LoyaltyGuestId,"Guest ID is null on Loyalty user timeline");
		pageObj.utils().logPass("Pos user guest id is: " + LoyaltyGuestId);

		// Step5- Verify the database table loyalty_pos_users for the data
		String GetLoyaltyUserIdQuery = "select loyalty_user_id from loyalty_pos_users where merged_user_id = '"
				+ PosGuestId + "'";
		pageObj.singletonDBUtilsObj();
		String expColValue2 = DBUtils.executeQueryAndGetColumnValue(env, GetLoyaltyUserIdQuery,
				"loyalty_user_id");
		Assert.assertEquals(expColValue2, LoyaltyGuestId,
				"Loyalty user id is not matched with the user id of loyalty user");
		pageObj.utils().logPass("Verified the database table loyalty_pos_users for the data");

		Assert.assertEquals(checkinId, checkinId2, "Checkin ID on loyalty user timeline doesn't match");
		pageObj.utils().logPass("Checkin ID on loyalty user timeline matched");

		// set "autocreate_pos_user" flag back to false
		boolean flag3 = DBUtils.updateBusinessesPreference(env, expColValue, status2, dataSet.get("dbFlag1"),
				b_id);
		Assert.assertTrue(flag3, dataSet.get("dbFlag1") + " value is not updated to " + status2);
		pageObj.utils().logit(dataSet.get("dbFlag1") + " value is updated to " + status2);

	}

	@SuppressWarnings("unused")
	@Test(description = "SQ-T5800,Verify the PendingCheckinNotifierWorker and AutoCreatePosCheckinWorker gets called in sidekiq", priority = 0)
	@Owner(name = "Vansham Mishra")
	public void T5800_VerifyPosMergeFunctionalityForBusiness() throws Exception {
		String b_id = dataSet.get("business_id");
		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		pageObj.singletonDBUtilsObj();
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");
		String status1 = "true";
		String status2 = "false";
		String key = CreateDateTime.getTimeDateString();
		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T10:50:00+05:30";

		// set "autocreate_pos_user" flag -> true
		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, status1, dataSet.get("dbFlag1"),
				b_id);
		Assert.assertTrue(flag, dataSet.get("dbFlag1") + " value is not updated to " + status1);
		pageObj.utils().logit(dataSet.get("dbFlag1") + " value is updated to " + status1);

		// set "autocreate_pos_checkin" flag -> true
		boolean flag2 = DBUtils.updateBusinessesPreference(env, expColValue, status1, dataSet.get("dbFlag2"),
				b_id);
		Assert.assertTrue(flag2, dataSet.get("dbFlag2") + " value is not updated to " + status1);
		pageObj.utils().logit(dataSet.get("dbFlag2") + " value is updated to " + status1);

		// Step1 - Create a POS user by using a phone number
		long phone = (long) (Math.random() * Math.pow(10, 10));
		String phoneStr = Long.toString(phone);
		Response respo = pageObj.endpoints().posSignUpWithoutEmail(phone, dataSet.get("locationKey"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, respo.getStatusCode(), "Status code 200 did not matched for pos signup api");
		String posUserEmail = respo.jsonPath().get("email").toString();
		String posUserId = respo.jsonPath().get("id").toString();
		pageObj.utils().logPass("Pos user has been created successfully usng phone number");

		// Step2 - Perform 2 POS checkin for the user
		Response resp1 = pageObj.endpoints().posCheckin(posUserEmail, dataSet.get("locationkey"), "10");
		Assert.assertEquals(resp1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for post chekin api");
		pageObj.utils().logPass("Pos checkin of $10 is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(posUserId, dataSet.get("apiKey"), "",
				dataSet.get("redeemable_id"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		pageObj.utils().logPass("Api2 send reward reedemable to user is successful");

		Response response1 = pageObj.endpoints().posRedemptionOfRedeemable(posUserEmail, date, key, txn,
				dataSet.get("locationkey"), dataSet.get("redeemable_id"), "110011");
		Assert.assertEquals(response1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for the POS Redemption API");
		pageObj.utils().logPass("Redemption using POS redemption is successful");

		// fetch checkin id from the user timeline of pos user
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.instanceDashboardPage().navigateToGuestTimeline(phoneStr);
		String checkinId = pageObj.guestTimelinePage().getCheckinId();
        Assert.assertNotNull(checkinId,"Checkin ID is null on POS user timeline");
		pageObj.utils().logit("checkin id displayed on the POS user timeline: " + checkinId);
		String PosGuestId = pageObj.guestTimelinePage().getGuestIdFromUrl();
        Assert.assertNotNull(PosGuestId,"Guest ID is null on POS user timeline");
		pageObj.utils().logPass("Pos user guest id is: " + PosGuestId);

		// step3 - Create a Mobile Loyalty user using the API
		// (do not use phone number for this step, use only email id)
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		// String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("API2 Signup is successful");

		// step4 - Update the phone number of the Loyalty user, by using the phone
		// number of the POS user created before
		// Use auth token for the created loyalty user in the third step
		Response updateGuestResponse = pageObj.endpoints().Api1MobileUpdateGuestDetailsWithoutEmail(dataSet.get("Npwd"),
				phoneStr, dataSet.get("client"), dataSet.get("secret"), token);
		Assert.assertEquals(updateGuestResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass(
				"Updated the phone number of the Loyalty user, by using the phone number of the POS user created");

		// fetch checkin id from the user timeline of loyalty user
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		String checkinId2 = pageObj.guestTimelinePage().getCheckinId();
        Assert.assertNotNull(checkinId2,"Checkin ID is null on POS user timeline");
		pageObj.utils().logit("checkin id displayed on the POS user timeline: " + checkinId2);
		String LoyaltyGuestId = pageObj.guestTimelinePage().getGuestIdFromUrl();
        Assert.assertNotNull(LoyaltyGuestId,"Guest ID is null on loyalty user timeline");
		pageObj.utils().logPass("Mobile loyalty user guest id is: " + LoyaltyGuestId);

		// Step5- Verify the database table loyalty_pos_users for the data
		String GetLoyaltyUserIdQuery = "select loyalty_user_id from loyalty_pos_users where merged_user_id = '"
				+ PosGuestId + "'";
		pageObj.singletonDBUtilsObj();
		String expColValue2 = DBUtils.executeQueryAndGetColumnValue(env, GetLoyaltyUserIdQuery,
				"loyalty_user_id");
		Assert.assertEquals(expColValue2, LoyaltyGuestId,
				"Loyalty user id is not matched with the user id of loyalty user");
		pageObj.utils().logPass("Verified the database table loyalty_pos_users for the data");

		Assert.assertEquals(checkinId, checkinId2, "Checkin ID on loyalty user timeline doesn't match");
		pageObj.utils().logPass("Checkin ID on loyalty user timeline matched");

		// verify jobs in sidekiq
//        int count =
//            pageObj.sidekiqPage().checkSidekiqJob(baseUrl, "AutoCreatePosCheckinWorker", 100);
//		Assert.assertTrue(count>0,"AutoCreatePosCheckinWorker is not called in sidekiq");
//		logger.info("AutoCreatePosCheckinWorker is called in sidekiq");
//		TestListeners.extentTest.get().info("AutoCreatePosCheckinWorker is called in sidekiq");
//
//        int count2 =
//            pageObj.sidekiqPage().checkSidekiqJob(baseUrl, "PendingCheckinNotifierWorker", 100);
//		Assert.assertTrue(count2>0,"PendingCheckinNotifierWorker is not called in sidekiq");
//		logger.info("PendingCheckinNotifierWorker is called in sidekiq");
//		TestListeners.extentTest.get().info("PendingCheckinNotifierWorker is called in sidekiq");

		// set "autocreate_pos_user" flag back to false
		boolean flag3 = DBUtils.updateBusinessesPreference(env, expColValue, status2, dataSet.get("dbFlag1"),
				b_id);
		Assert.assertTrue(flag3, dataSet.get("dbFlag1") + " value is not updated to " + status2);
		pageObj.utils().logit(dataSet.get("dbFlag1") + " value is updated to " + status2);

	}

	@Test(description = "SQ-T5801,Verify the UnredeemedPointExpiryWorker functionality for a user with checkin process"
			+ "SQ-T5814 Verify the reminder email for final expiry and expiry warning days", groups = { "regression",
					"dailyrun" }, priority = 0)
	@Owner(name = "Vansham Mishra")
	public void T5801_VerifyUnredeemedPointExpiryWorkerFunctionalityForUserWithCheckinProcess() throws Exception {

		String b_id = dataSet.get("business_id");
		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		pageObj.singletonDBUtilsObj();
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");
		String status1 = "true";
		// String status2 = "false";
		String key = CreateDateTime.getTimeDateString();
		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T10:50:00+05:30";

		// set "enable_optimise_unredeemed_point_expiry" flag -> true
		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, status1, dataSet.get("dbFlag1"),
				b_id);
		Assert.assertTrue(flag, dataSet.get("dbFlag1") + " value is not updated to " + status1);
		pageObj.utils().logit(dataSet.get("dbFlag1") + " value is updated to " + status1);

		// set "track_reward_banked_points" flag -> true
		boolean flag2 = DBUtils.updateBusinessesPreference(env, expColValue, status1, dataSet.get("dbFlag2"),
				b_id);
		Assert.assertTrue(flag2, dataSet.get("dbFlag2") + " value is not updated to " + status1);
		pageObj.utils().logit(dataSet.get("dbFlag2") + " value is updated to " + status1);

		// Step1 - Create a POS user by using a phone number
		long phone = (long) (Math.random() * Math.pow(10, 10));
		// String phoneStr = Long.toString(phone);
		Response respo = pageObj.endpoints().posSignUpWithoutEmail(phone, dataSet.get("locationKey"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, respo.getStatusCode(), "Status code 200 did not matched for pos signup api");
		String posUserEmail = respo.jsonPath().get("email").toString();
		String posUserId = respo.jsonPath().get("id").toString();
		pageObj.utils().logPass("Pos user has been created successfully usng phone number");

		// step2 - Expiry is set to 1 days
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
        pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
        pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboard("Has Membership Levels?", "check");
        pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboard("Membership Level Bump on Edge?", "check");
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		pageObj.earningPage().setFinalExpiryDaysFields("set", 1, "Expires After");
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
//		pageObj.earningPage().accountReEvaluationStrategy("No Re-evaluation");
		pageObj.earningPage().expiryDateStrategy("Exact Days");
		pageObj.earningPage().membershipLevelResetStrategy("Reset Based On Points Earned");
		pageObj.earningPage().updateConfiguration();

		// Step3- Set the expiry warning days and final expiry warning days to blank
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		pageObj.earningPage().setFinalExpiryDaysFields("clear", 1, "Expiry Warning Days");
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		pageObj.earningPage().setFinalExpiryDaysFields("clear", 1, "Final Expiry Warning Days");
		pageObj.earningPage().verifyMessage("Business was successfully updated.");

		// Step4 - Perform 2 POS checkin for the user
		Response resp1 = pageObj.endpoints().posCheckin(posUserEmail, dataSet.get("locationkey"), "10");
		Assert.assertEquals(resp1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for post chekin api");
		pageObj.utils().logPass("Pos checkin of $10 is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(posUserId, dataSet.get("apiKey"), "",
				dataSet.get("redeemable_id"));
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
                "Status code 201 did not match for api2 send message to user");
		pageObj.utils().logPass("Api2 send reward reedemable to user is successful");

		Response response1 = pageObj.endpoints().posRedemptionOfRedeemable(posUserEmail, date, key, txn,
				dataSet.get("locationkey"), dataSet.get("redeemable_id"), "12003");
		Assert.assertEquals(response1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for the POS Redemption API");
		pageObj.utils().logPass("Redemption using POS redemption is successful");

		// String newcreated_at = CreateDateTime.getYesterdayDateTimeUTC(2);
		String newscheduled_at = CreateDateTime.getYesterdayDays(1);
        utils.longWaitInSeconds(15);
		// DB query for checkins updation
		String query1 = "UPDATE `checkins` SET `scheduled_expiry_on` = '" + newscheduled_at
				+ "' WHERE `checkin_type`='PosCheckin' and user_id = '" + posUserId + "' ORDER BY `id` DESC;";
		pageObj.singletonDBUtilsObj();
		int rs1 = DBUtils.executeUpdateQuery(env, query1); // stmt.executeUpdate(query1);
		Assert.assertEquals(rs1, 2, "checkins table query is not working");
		pageObj.utils().logit("checkins table updated successfully.");

		// step5 - run recall schedule
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().runRecallScheule();

		// step6 - verify jobs in sidekiq

        int count =
            pageObj.sidekiqPage().checkSidekiqJob(baseUrl, "UnredeemedPointExpiryWorker", 5);
		Assert.assertTrue(count>0,"UnredeemedPointExpiryWorker is not called in sidekiq");
		pageObj.utils().logPass("UnredeemedPointExpiryWorker is called in sidekiq");

		// step7 -verify the entry in expiry_events table for the created user
		String query2 = "SELECT * FROM `expiry_events` WHERE `user_id` = '" + posUserId + "'";
		pageObj.singletonDBUtilsObj();
		String expColValue2 = DBUtils.executeQueryAndGetColumnValue(env, query2, "user_id");
		Assert.assertEquals(expColValue2, posUserId, "User id is not matched with the user id of created user");
		pageObj.utils().logPass(
				"Verified  that the expiry is ran for the user and in checkins table- expired_at is set for the user");
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}
}
