package com.punchh.server.OMMTest;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.punchh.server.annotations.Owner;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.apiConfig.ApiResponseJsonSchema;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class DiscountBasketExternalUIDTest {
	private static Logger logger = LogManager.getLogger(DiscountBasketExternalUIDTest.class);
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
	String discount_details0 = "";

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
		logger.info(sTCName + " ==>" + dataSet);
	}

	@Test(description = "SQ-T3682 [Auth API-> Fetch Active Basket] Verify external_uid gets saved in DB or not when Enable Reward Locking Flag is ON  ||"
			+ " SQ-T3688 [Auth API-> Add Discounts To Basket] Verify external_uid gets saved in DB or not when Enable Reward Locking Flag is ON ||"
			+ " SQ-T3683 [POS API-> Remove Discounts From Basket] Verify external_uid gets saved in DB or not when Enable Reward Locking Flag is ON ||"
			+ " SQ-T3687 [POS API-> Auto Select] Verify external_uid gets saved in DB or not when Enable Reward Locking Flag is ON ||"
			+ "SQ-T3293 [Batched Redemptions-OMM-T947(592)] Verify Redemptions 2.0 API's (location specific) are not functional on locations having Allow Location for Multiple Redemption On", groups = { "regression", "dailyrun" }, priority = 0)
	@Owner(name = "Hardik Bhardwaj")
	public void T3682_AuthAPIFetchActiveBasket() throws InterruptedException {

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Click Cockpit

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().navigateToTabs("Guest Validation");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_validate_unverified_age", "uncheck");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_validate_privacy_policy", "uncheck");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_validate_external_source", "uncheck");
		pageObj.dashboardpage().clickOnUpdateButton();

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.cockpitRedemptionsPage().offEnableRewardLocking();
		pageObj.dashboardpage().updateCheckBox();

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
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "16",
				dataSet.get("redeemable_id"), "", "");

		pageObj.utils().logit("Send redeemable to the user successfully");

		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		pageObj.utils().logPass("Api2  send reward amount to user is successful");

        // get reward id
        String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"),
            dataSet.get("secret"), dataSet.get("redeemable_id"));
        Assert.assertNotEquals(rewardId, null, "Reward Id is null");
        pageObj.utils().logit("Reward id " + rewardId + " is generated successfully ");

		// send reward amount to user Reedemable
		Response sendRewardResponse1 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "16",
				dataSet.get("redeemable_id1"), "", "");

		pageObj.utils().logit("Send redeemable to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse1.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

        // get reward id
        String rewardId1 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"),
            dataSet.get("secret"), dataSet.get("redeemable_id1"));
        Assert.assertNotEquals(rewardId, null, "Reward Id is null");
        pageObj.utils().logit("Reward id " + rewardId + " is generated successfully ");

		// AUTH Add Discount to Basket
		Response discountBasketResponse = pageObj.endpoints().addDiscountToBasketAUTH(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId, "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponse.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		pageObj.utils().logPass("AUTH add discount to basket is successful");

		// Click Cockpit
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.cockpitRedemptionsPage().onEnableRewardLocking();

		// Deprecated https://punchhdev.atlassian.net/browse/OMM-1256
//		// AUTH Add Discount to Basket
//		Response discountBasketResponse1 = pageObj.endpoints().addDiscountToBasketAUTH(token, dataSet.get("client"),
//				dataSet.get("secret"), "reward", rewardId1, "");
//		Assert.assertEquals(discountBasketResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
//				"Status code 400 did not match with add discount to basket ");
//		boolean isAuthAddDiscountBasketMissingExternalUidSchemaValidated = Utilities.validateJsonAgainstSchema(
//				ApiResponseJsonSchema.apiErrorObjectSchema, discountBasketResponse1.asString());
//		Assert.assertTrue(isAuthAddDiscountBasketMissingExternalUidSchemaValidated,
//				"Auth API Add Discount to Basket Schema Validation failed");
//		Assert.assertEquals(discountBasketResponse1.jsonPath().getString("error"),
//				"Required parameter missing or the value is empty: external_uid");
//		pageObj.utils().logPass("AUTH add discount to basket is successful");

		// Deprecated https://punchhdev.atlassian.net/browse/OMM-1256
//		// Auth API fetch active basket
//		Response basketDiscountDetailsResponse2 = pageObj.endpoints().fetchActiveBasketAuthApi(token,
//				dataSet.get("client"), dataSet.get("secret"), "");
//		Assert.assertEquals(basketDiscountDetailsResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
//				"Status code 400 did not match with fetch active basket ");
//		boolean isAuthFetchActiveBasketMissingExternalUidSchemaValidated = Utilities.validateJsonAgainstSchema(
//				ApiResponseJsonSchema.apiErrorObjectSchema, basketDiscountDetailsResponse2.asString());
//		Assert.assertTrue(isAuthFetchActiveBasketMissingExternalUidSchemaValidated,
//				"Auth API Fetch Active Basket Schema Validation failed");
//		Assert.assertEquals(basketDiscountDetailsResponse2.jsonPath().getString("error"),
//				"Required parameter missing or the value is empty: external_uid");
//		pageObj.utils().logPass("POS  fetch active basket is successful");

		// Deprecated https://punchhdev.atlassian.net/browse/OMM-1256
//		// POS remove discount from basket
//		Response deleteBasketResponse3 = pageObj.endpoints().removeDiscountFromBasketPOSAPI(dataSet.get("locationkey"),
//				userID, "", "");
//		Assert.assertEquals(deleteBasketResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
//				"Status code 400 did not match with remove discount from basket ");
//		pageObj.utils().logPass("POS remove discount from basket is successful");

		// POS user lookUp
		Thread.sleep(10000);
		Response userLookupResponse = pageObj.endpoints().userLookupPosApi("email", userEmail,
				dataSet.get("locationkey"), "");
		Assert.assertEquals(userLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with POS user lookUp ");
		pageObj.utils().logPass("POS user lookU is successful");
		String externalUidResponse = userLookupResponse.jsonPath().getString("external_uid");
		String locked = userLookupResponse.jsonPath().getString("locked").toString();
		Assert.assertTrue(
				pageObj.cockpitRedemptionsPage().resultOfUserLookUpPosAPI1(externalUidResponse, locked, null, "false"));
		pageObj.utils().logPass("POS user lookUp is successful");

		Thread.sleep(10000);
		// POS Process Batch Redemption
		Response batchRedemptionProcessResponseUser1 = pageObj.endpoints()
				.processBatchRedemptionPosApiPayload(dataSet.get("locationkey"), userID, "30", "1", "12002", "");
		Assert.assertEquals(batchRedemptionProcessResponseUser1.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Status code 400 did not match with Process Batch Redemption ");
		boolean isPosBatchRedemptionMissingExternalUidSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, batchRedemptionProcessResponseUser1.asString());
		Assert.assertTrue(isPosBatchRedemptionMissingExternalUidSchemaValidated,
				"POS API Process Batch Redemption Schema Validation failed");
		Assert.assertEquals(batchRedemptionProcessResponseUser1.jsonPath().getString("error"),
				"Required parameter missing or the value is empty: external_uid");
		pageObj.utils().logPass("POS Process Batch Redemption Api is successful");

		// POS Discount Lookup Api
		Thread.sleep(10000);
		Response discountLookupResponse = pageObj.endpoints().discountLookUpPosApi(dataSet.get("locationkey"), userID,
				dataSet.get("item_id"), "30", "");
		Assert.assertEquals(discountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Status code 400 did not match with Discount Lookup Api ");
		boolean isPosDiscountLookupMissingExternalUidSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, discountLookupResponse.asString());
		Assert.assertTrue(isPosDiscountLookupMissingExternalUidSchemaValidated,
				"POS API Discount Lookup Schema Validation failed");
		Assert.assertEquals(discountLookupResponse.jsonPath().getString("error"),
				"Required parameter missing or the value is empty: external_uid");
		pageObj.utils().logPass("POS Discount Lookup Api is successful");

		Thread.sleep(7000);
		// Auth Process Batch Redemption
		Response batchRedemptionProcessResponse = pageObj.endpoints().processBatchRedemptionAUTHAPI(
				dataSet.get("client"), dataSet.get("secret"), dataSet.get("locationkey"), token, userID, "12003", "");
		Assert.assertEquals(batchRedemptionProcessResponse.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Status code 400 did not match with Process Batch Redemption ");
		boolean isAuthBatchRedemptionMissingExternalUidSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, batchRedemptionProcessResponse.asString());
		Assert.assertTrue(isAuthBatchRedemptionMissingExternalUidSchemaValidated,
				"Auth API Process Batch Redemption Schema Validation failed");
		Assert.assertEquals(batchRedemptionProcessResponse.jsonPath().getString("error"),
				"Required parameter missing or the value is empty: external_uid");
		pageObj.utils().logPass("Auth Process Batch Redemption Api is successful");

		// Deprecated https://punchhdev.atlassian.net/browse/OMM-1256
//		// POS Auto Select
//		Thread.sleep(10000);
//		Response autoUnlockResponse = pageObj.endpoints().autoSelectPosApi(userID, "30", "1", "12003", "",
//				dataSet.get("locationkey"));
//		Assert.assertEquals(autoUnlockResponse.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST, "Status code 400 did not match with Auto Unlock ");
//		boolean isPosAutoUnlockMissingExternalUidSchemaValidated = Utilities
//				.validateJsonAgainstSchema(ApiResponseJsonSchema.apiErrorObjectSchema, autoUnlockResponse.asString());
//		Assert.assertTrue(isPosAutoUnlockMissingExternalUidSchemaValidated,
//				"POS API Auto Unlock Schema Validation failed");
//		Assert.assertEquals(autoUnlockResponse.jsonPath().getString("error"),
//				"Required parameter missing or the value is empty: external_uid");
//		pageObj.utils().logPass("POS Auto Unlock Api is successful");

	}

	@Test(description = "SQ-T3685 [POS API-> Discount Lookup] Verify external_uid gets saved in DB or not, when Enable Reward Locking Flag is ON "
			+ "|| SQ-T3684 [POS API-> User Lookup] Verify external_uid gets saved in DB or not when Enable Reward Locking Flag is ON "
			+ "|| SQ-T3689 [Auth API-> Process Batch Redemption] Verify external_uid gets saved in DB or not when Enable Reward Locking Flag is ON "
			+ "|| SQ-T3686 [POS API-> Process Batch Redemption] Verify external_uid gets saved in DB or not when Enable Reward Locking Flag is ON ||"
			+ "SQ-T3895 Verify that if Discount amount is not added in basket and max redemption is empty then discount amount-5(default) gets displayed In unselected discounts", groups = { "regression", "dailyrun" }, priority = 1)
	@Owner(name = "Hardik Bhardwaj")
	public void T3685_POS_API_DiscountLookup() throws InterruptedException {

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().navigateToTabs("Guest Validation");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_validate_unverified_age", "uncheck");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_validate_privacy_policy", "uncheck");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_validate_external_source", "uncheck");
		pageObj.dashboardpage().clickOnUpdateButton();

		// Click Cockpit
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.cockpitRedemptionsPage().offEnableRewardLocking();
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Validations");
		pageObj.cockpitRedemptionsPage().updateMaxRedemptionAmount("");
		pageObj.dashboardpage().updateCheckBox();

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
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "16",
				dataSet.get("redeemable_id"), "", "");

		pageObj.utils().logit("Send redeemable to the user successfully");

		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		pageObj.utils().logPass("Api2  send reward amount to user is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse1 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "16",
				dataSet.get("redeemable_id1"), "", "");

		pageObj.utils().logit("Send redeemable to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse1.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		String rewardId, rewardId2, rewardId1 = "";

		// get reward id
		rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id"));
		pageObj.utils().logPass("Reward id " + rewardId + " and " + rewardId1 + " is generated successfully ");

		// get reward id
		rewardId1 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id1"));
		pageObj.utils().logPass("Reward id 1 " + rewardId1 + " and " + rewardId1 + " is generated successfully ");

		// AUTH Add Discount to Basket
		Response discountBasketResponse = pageObj.endpoints().addDiscountToBasketAUTH(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId, "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponse.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		pageObj.utils().logPass("AUTH add discount to basket is successful");

		// Click Cockpit
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.cockpitRedemptionsPage().onEnableRewardLocking();

		String externalUID1 = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		// AUTH Add Discount to Basket
		Response discountBasketResponse2 = pageObj.endpoints().addDiscountToBasketAUTH(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId1, externalUID1);
		Assert.assertEquals(discountBasketResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with add discount to basket ");
		pageObj.utils().logPass("AUTH add discount to basket is successful");

		// Auth API fetch active basket
		Response basketDiscountDetailsResponse4 = pageObj.endpoints().fetchActiveBasketAuthApi(token,
				dataSet.get("client"), dataSet.get("secret"), externalUID1);
		Assert.assertEquals(basketDiscountDetailsResponse4.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with fetch active basket ");
		pageObj.utils().logPass("POS  fetch active basket is successful");

		// POS remove discount from basket
		String expdiscount_basket_item_id1 = discountBasketResponse2.jsonPath()
				.get("discount_basket_items[0].discount_basket_item_id").toString();
		Response deleteBasketResponse5 = pageObj.endpoints().removeDiscountFromBasketPOSAPI(dataSet.get("locationkey"),
				userID, expdiscount_basket_item_id1, externalUID1);
		Assert.assertEquals(deleteBasketResponse5.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with remove discount from basket ");
		boolean isPosRemoveDiscountBasketSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.posGetActiveDiscountBasketSchema, deleteBasketResponse5.asString());
		Assert.assertTrue(isPosRemoveDiscountBasketSchemaValidated,
				"POS API Remove Discount from Basket Schema Validation failed");
		pageObj.utils().logPass("POS remove discount from basket is successful");

		// Deprecated https://punchhdev.atlassian.net/browse/OMM-1256
		// POS user lookUp
//		Assert.assertTrue(pageObj.cockpitRedemptionsPage().getExternalUidFromPOSUserLookUpApi("email", userEmail,
//				dataSet.get("locationkey"), externalUID1, "true"));
//		logger.info("POS user lookUp is successful");
//		pageObj.utils().logPass("POS user lookUp is successful");

//		SQ-T3895 Verify that if Discount amount is not added in basket and max redemption is empty then discount amount-5(default) gets displayed In unselected discounts

		// POS Discount Lookup Api
		Response discountLookupResponse1 = pageObj.endpoints().discountLookUpPosApi(dataSet.get("locationkey"), userID,
				dataSet.get("item_id"), "30", externalUID1);
		Assert.assertEquals(discountLookupResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Discount Lookup Api ");
		String externalUidResponse2 = discountLookupResponse1.jsonPath().getString("external_uid");
		String locked2 = discountLookupResponse1.jsonPath().getString("locked");
		Assert.assertTrue(pageObj.cockpitRedemptionsPage().resultOfUserLookUpPosAPI(externalUidResponse2, locked2,
				externalUID1, "true"));
		pageObj.utils().logPass("POS user lookUp is successful");
		String discountAmount = discountLookupResponse1.jsonPath().getString("selected_discounts[0].discount_amount");
		Assert.assertEquals(discountAmount, null, "Discount amount is not matching");
		pageObj.utils().logPass("In discount LookUp Pos Api Discount amount is 0 and is equal to max redemption amount");

		// POS Process Batch Redemption
		Response batchRedemptionProcessResponseUser2 = pageObj.endpoints().processBatchRedemptionPosApiPayload(
				dataSet.get("locationkey"), userID, "30", "1", "12003", externalUID1);
		Assert.assertEquals(batchRedemptionProcessResponseUser2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Process Batch Redemption ");
		String redemption_ref = batchRedemptionProcessResponseUser2.jsonPath().getString("redemption_ref");
		System.out.println("POS Process Batch Redemption api is working properly and redemption refrence code is - "
				+ redemption_ref);
		pageObj.utils().logPass("POS Process Batch Redemption Api is successful");

		// POS Auto Unlock
		Response autoUnlockResponse1 = pageObj.endpoints().autoSelectPosApi(userID, "30", "1", "12003", externalUID1,
				dataSet.get("locationkey"));
		Assert.assertEquals(autoUnlockResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Auto Unlock ");
		pageObj.utils().logPass("POS Auto Unlock Api is successful");

		// User SignUp using API
		String userEmail1 = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse1 = pageObj.endpoints().Api2SignUp(userEmail1, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse1, "API 2 user signup");
		String token1 = signUpResponse1.jsonPath().get("access_token.token").toString();

		String userID1 = signUpResponse1.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("API2 Signup is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(userID1, dataSet.get("apiKey"), "16",
				dataSet.get("redeemable_id"), "", "");
		pageObj.utils().logit("Send redeemable to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse2.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		rewardId2 = pageObj.redeemablesPage().getRewardId(token1, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id"));
		pageObj.utils().logPass("Reward id " + rewardId2 + " is generated successfully ");

		// AUTH Add Discount to Basket
		Response discountBasketResponse5 = pageObj.endpoints().addDiscountToBasketAUTH(token1, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId2, externalUID1);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponse5.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		pageObj.utils().logPass("AUTH add discount to basket is successful");

		// Auth Process Batch Redemption
		Response batchRedemptionProcessResponse1 = pageObj.endpoints().processBatchRedemptionAUTHAPI(
				dataSet.get("client"), dataSet.get("secret"), dataSet.get("locationkey"), token, userID, "12003",
				externalUID1);
		Assert.assertEquals(batchRedemptionProcessResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Process Batch Redemption ");
		String redemption_ref1 = batchRedemptionProcessResponse1.jsonPath().get("redemption_ref").toString();
		System.out.println("POS Process Batch Redemption api is working properly and redemption refrence code is - "
				+ redemption_ref1);
		pageObj.utils().logPass("Auth Process Batch Redemption Api is successful");

	}

	@Test(description = "SQ-T3882 Auth Batch Redemption>Validate that if Redeemable with name,description and image gifted as reward is added into basket then mentioned name,description and image path gets displayed under discount_details under Success and failures ||"
			+ "SQ-T3880 POS discount lookup API>Validate that rewards which are not added in discount_basket those reward's discount_details are getting displayed under unselected discounts ||"
			+ "SQ-T3881 Auth Batch Redemption>Validate that if Redeemable with QC gifted as reward is added into basket then NULL value in Base Amount under 'discount_details' gets displayed under Selected discounts", groups = { "regression", "dailyrun" }, priority = 2)
	@Owner(name = "Hardik Bhardwaj")
	public void T3882_ValidateThatIfRedeemableWithNameDescriptionAndImage() throws InterruptedException {

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().navigateToTabs("Guest Validation");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_validate_unverified_age", "uncheck");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_validate_privacy_policy", "uncheck");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_validate_external_source", "uncheck");
		pageObj.dashboardpage().clickOnUpdateButton();

		// Click Cockpit
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.cockpitRedemptionsPage().onEnableRewardLocking();

		// User SignUp using API
		String userEmail1 = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse1 = pageObj.endpoints().Api2SignUp(userEmail1, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse1, "API 2 user signup");
		String token1 = signUpResponse1.jsonPath().get("access_token.token").toString();

		String userID1 = signUpResponse1.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("API2 Signup is successful");

		// send reward amount to user Reedemable-1
		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(userID1, dataSet.get("apiKey"), "16",
				dataSet.get("redeemable_id"), "", "");
		pageObj.utils().logit("Send redeemable to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse2.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		// send reward amount to user Reedemable-1
		Response sendRewardResponse3 = pageObj.endpoints().sendMessageToUser(userID1, dataSet.get("apiKey"), "16",
				dataSet.get("redeemable_id1"), "", ""); // redeemable_id1- 1218
		pageObj.utils().logit("Send redeemable to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse3.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		// get reward id-1
		String rewardId = pageObj.redeemablesPage().getRewardId(token1, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id"));
		pageObj.utils().logPass("Reward id 1 " + rewardId + " is generated successfully ");

		String externalUID1 = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		// AUTH Add Discount to Basket-1
		Response discountBasketResponse5 = pageObj.endpoints().addDiscountToBasketAUTH(token1, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId, externalUID1);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponse5.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		pageObj.utils().logPass("AUTH add discount to basket is successful");
		String name = discountBasketResponse5.jsonPath().get("discount_basket_items[0].discount_details.name")
				.toString();
		String image = discountBasketResponse5.jsonPath().get("discount_basket_items[0].discount_details.image")
				.toString();
		String description = discountBasketResponse5.jsonPath()
				.get("discount_basket_items[0].discount_details.description").toString();
		String baseAmount = discountBasketResponse5.jsonPath()
				.getString("discount_basket_items[0].discount_details.base_amount");

		Assert.assertEquals(name, "Automatio Hardik 12003",
				"AUTH add discount to basket Redeemable name is not visible");
		pageObj.utils().logPass("AUTH add discount to basket Redeemable name is visible");

		String imageURL = "https://res.cloudinary.com/punchh/image/upload/fl_lossy,q_auto/v1/static/punchh-icon-thumb.png";
		Assert.assertEquals(image, imageURL, "AUTH add discount to basket Redeemable image is not visible");
		pageObj.utils().logPass("AUTH add discount to basket Redeemable image is visible");

		Assert.assertEquals(description, "Test Automation Redeemable",
				"AUTH add discount to basket Redeemable description is not visible");
		pageObj.utils().logPass("AUTH add discount to basket Redeemable description is visible");

		Assert.assertEquals(baseAmount, null, "AUTH add discount to basket base_amount is not null");
		pageObj.utils().logPass("AUTH add discount to basket base_amount is null");

//		SQ-T3880 POS discount lookup API>Validate that rewards which are not added in discount_basket those reward's discount_details are getting displayed under unselected discounts

		// POS Discount Lookup Api-1
		List<Object> obj = new ArrayList<Object>();
		Response discountLookupResponse0 = pageObj.endpoints().discountLookUpPosApi(dataSet.get("locationkey"), userID1,
				dataSet.get("item_id"), "30", externalUID1);
		Assert.assertEquals(discountLookupResponse0.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Discount Lookup Api ");
		obj = discountLookupResponse0.jsonPath().getList("unselected_discounts");
		for (int i = 0; i < obj.size(); i++) {
			discount_details0 = discountLookupResponse0.jsonPath()
					.getString("unselected_discounts[" + i + "].discount_details.name");
			if (discount_details0.equalsIgnoreCase("Base Redeemable")) {
				break;
			}
		}
		Assert.assertEquals(discount_details0, "Base Redeemable",
				"rewards which are not added in discount_basket those reward's discount_details are not getting displayed under unselected discounts");
		pageObj.utils().logPass(
				"rewards which are not added in discount_basket those reward's discount_details are getting displayed under unselected discounts");

		// Auth Process Batch Redemption
		Response batchRedemptionProcessResponse2 = pageObj.endpoints().processBatchRedemptionAUTHAPI(
				dataSet.get("client"), dataSet.get("secret"), dataSet.get("locationkey"), token1, userID1, "12003",
				externalUID1);
		Assert.assertEquals(batchRedemptionProcessResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Process Batch Redemption ");
		String redemption_ref1 = batchRedemptionProcessResponse2.jsonPath().get("redemption_ref").toString();
		pageObj.utils().logit("POS Process Batch Redemption api is working properly and redemption refrence code is - "
				+ redemption_ref1);
		pageObj.utils().logPass("Auth Process Batch Redemption Api is successful");

		String name_success = batchRedemptionProcessResponse2.jsonPath().get("success[0].discount_details.name")
				.toString();
		String image_success = batchRedemptionProcessResponse2.jsonPath().get("success[0].discount_details.image")
				.toString();
		String description_success = batchRedemptionProcessResponse2.jsonPath()
				.get("success[0].discount_details.description").toString();

//		SQ-T3881 Auth Batch Redemption>Validate that if Redeemable with QC gifted as reward is added into basket then NULL value in Base Amount under 'discount_details' gets displayed under Selected discounts

		String baseAmount_success = batchRedemptionProcessResponse2.jsonPath()
				.getString("success[0].discount_details.base_amount");

		pageObj.utils().logPass("Auth Process Batch Redemption Api Redeemable name is visible");

		Assert.assertEquals(name_success, "Automatio Hardik 12003",
				"Auth Process Batch Redemption name is not visible");
		pageObj.utils().logPass("Auth Process Batch Redemption Redeemable name is visible");

		String imageURL_success = "https://res.cloudinary.com/punchh/image/upload/fl_lossy,q_auto/v1/static/punchh-icon-thumb.png";
		Assert.assertEquals(image_success, imageURL_success,
				"Auth Process Batch Redemption Redeemable image is not visible");
		pageObj.utils().logPass("Auth Process Batch Redemption Redeemable image is visible");

		Assert.assertEquals(description_success, "Test Automation Redeemable",
				"Auth Process Batch Redemption Redeemable description is not visible");
		pageObj.utils().logPass("Auth Process Batch Redemption Redeemable description is visible");

		Assert.assertEquals(baseAmount_success, null, "Auth Process Batch Redemption base_amount is not null");
		pageObj.utils().logPass("Auth Process Batch Redemption base_amount is null");

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}

}
