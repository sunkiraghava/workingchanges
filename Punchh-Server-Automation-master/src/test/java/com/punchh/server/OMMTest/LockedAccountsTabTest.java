package com.punchh.server.OMMTest;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

import com.punchh.server.annotations.Owner;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.apiConfig.ApiPayloads;
import com.punchh.server.apiConfig.ApiResponseJsonSchema;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class LockedAccountsTabTest {
	private static Logger logger = LogManager.getLogger(LockedAccountsTabTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String sTCName;
	private String baseUrl;
	String blankString = "";
	private String env, run = "ui";
	private static Map<String, String> dataSet;
	String discount_details0;
	private Utilities utils;

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
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==>" + dataSet);
		utils = new Utilities(driver);
	}

	// Deprecated https://punchhdev.atlassian.net/browse/OMM-1256
//	@Test(description = "SQ-T3735 Verify Locked Accounts tab visibility and Functionality in dashboard || "
//			+ "SQ-T3736 Verify Locked Accounts tab visibility and Functionality in dashboard when Enable Reward Locking flag is disabled and user have an unlocked basket || "
//			+ "SQ-T3737 Verify Locked Accounts tab Functionality for cancel button in confirmation Alert box ||"
//			+ "SQ-T3742 Verify user is able to search guest by entering first name of guest in Search Bar under Locked Account tab ||"
//			+ "SQ-T3743 Verify user is able to search guest by entering email of guest in Search Bar under Locked Account tab ||"
//			+ "SQ-T3744 Verify user is able to search guest by entering phone number of guest in Search Bar under Locked Account tab when phone number uniqueness flag is disabled", groups = {"regression", "dailyrun"}, priority = 0)
	public void T3735_LockedAccountTab() throws InterruptedException {

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Navigate to Multiple Redemption Tab and check or Uncheck the flags
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_discount_locking", "check");
		String sMsg1 = pageObj.dashboardpage().updateCheckBox();
		Assert.assertEquals(sMsg1, "Business was successfully updated.");

		// check locked account tab in Guest tab
		pageObj.menupage().navigateToSubMenuItem("Guests", "Guests");
		boolean result1 = pageObj.guestTimelinePage().lockedAccountTab(6);
		Assert.assertTrue(result1, "Locked Account Tab is visible in Guest section");
		utils.logPass("Locked Account Tab is visible in Guest section");

		// User SignUp using API
		long phone = (long) (Math.random() * Math.pow(10, 10));
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"), phone);
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		String firstName = signUpResponse.jsonPath().get("user.first_name").toString();
		String lastName = signUpResponse.jsonPath().get("user.last_name").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("API2 Signup is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				dataSet.get("redeemable_id"), "", "");

		utils.logit("Send redeemable to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		String redeemableID = dataSet.get("redeemable_id");

		// get reward id
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemableID);

		utils.logit("Reward id " + rewardId + " is generated successfully ");

		String externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));

		// AUTH Add Discount to Basket
		Response discountBasketResponse5 = pageObj.endpoints().addDiscountToBasketAUTH(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId, externalUID);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponse5.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		utils.logPass("AUTH add discount to basket is successful");

		// search user in locked account by email
		pageObj.menupage().navigateToSubMenuItem("Guests", "Guests");
		pageObj.guestTimelinePage().navigateToLockedAccountTab();
		boolean status = pageObj.guestTimelinePage().searchLockedAccountTab(userEmail, 5);
		Assert.assertTrue(status, "Error in searching email in locked account");
		utils.logPass("Email search in locked account is successful");

		// search user in locked account by first name
		pageObj.menupage().navigateToSubMenuItem("Guests", "Guests");
		pageObj.guestTimelinePage().navigateToLockedAccountTab();
		boolean status1 = pageObj.guestTimelinePage().searchLockedAccountTab(firstName, 10);
		Assert.assertTrue(status1, "Error in searching first name in locked account");
		utils.logPass("first name search in locked account is successful");

		// search user in locked account by last name
		pageObj.menupage().navigateToSubMenuItem("Guests", "Guests");
		pageObj.guestTimelinePage().navigateToLockedAccountTab();
		boolean status2 = pageObj.guestTimelinePage().searchLockedAccountTab(lastName, 5);
		Assert.assertTrue(status2, "Error in searching last name in locked account");
		utils.logPass("last name search in locked account is successful");

		// search user in locked account by phone number
		pageObj.menupage().navigateToSubMenuItem("Guests", "Guests");
		pageObj.guestTimelinePage().navigateToLockedAccountTab();
		String phone1 = String.valueOf(phone);
		boolean status3 = pageObj.guestTimelinePage().searchLockedAccountTab(phone1, 5);
		Assert.assertTrue(status3, "Error in searching last name in locked account");
		utils.logPass("last name search in locked account is successful");

		// search user in locked account by email and cancel the unlock basket request
		pageObj.guestTimelinePage().unlockOrCancelLockedAccount(userEmail, "cancel");
		pageObj.menupage().navigateToSubMenuItem("Guests", "Guests");
		pageObj.guestTimelinePage().navigateToLockedAccountTab();
		boolean status4 = pageObj.guestTimelinePage().searchLockedAccountTab(userEmail, 5);
		Assert.assertTrue(status4, "Error in searching email in locked account");
		utils.logPass("Email search in locked account is successful");

//		 search user in locked account by email and accept the unlock basket request
//		 success message is intermittent
		pageObj.guestTimelinePage().unlockOrCancelLockedAccount(userEmail, "ok");
//		boolean result2 = pageObj.guestTimelinePage()
//				.successOrErrorConfirmationMessage("Guest Account has been unlocked successfully.");
//		Assert.assertTrue(result2, "Given Message does not matches with the displayed message");
//		logger.info("Given Message matches with the displayed message");
//		utils.logPass("Given Message matches with the displayed message");

		pageObj.menupage().navigateToSubMenuItem("Guests", "Guests");
		pageObj.guestTimelinePage().navigateToLockedAccountTab();
		boolean status5 = pageObj.guestTimelinePage().searchUserInLockedAccountTab(userEmail, 2);
		Assert.assertEquals(status5, false, "user in not unlocked");
		utils.logPass("User basket unlocked from locked account is successful");

		// Navigate to Multiple Redemption Tab and Uncheck the flags
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_discount_locking", "uncheck");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_auto_unlock", "uncheck");
		String sMsg = pageObj.dashboardpage().updateCheckBox();
		Assert.assertEquals(sMsg, "Business was successfully updated.");

		// check locked account tab in Guest tab
		utils.longWaitInSeconds(15);
		pageObj.menupage().navigateToSubMenuItem("Guests", "Guests");
		boolean result = pageObj.guestTimelinePage().lockedAccountTab(2);
		Assert.assertFalse(result, "Locked Account Tab is present in Guest section");
		utils.logPass("Locked Account Tab is not visible in Guest section");

	}

	@Test(description = "SQ-T3892 DELETE API>Validate that user is unable to delete the discount_basket_item of locked discount_basket_item if reward locking is ON but external_uid is invalid/empty ||"
			+ "SQ-T3889 Validate that If user is passing correct external uid in {{path}}/api/pos/discounts/select API, then only user is able to update the locked basket. ||"
			+ "SQ-T3893 POS discount lookup API>Validate that user is able to get the details of selected and unselected discounts of locked discount_basket_item if reward locking is ON and external_uid is correct ||"
			+ "SQ-T3891 GET ACTIVE API>Validate that user is able to get the details of locked discount_basket_item if reward locking is ON and external_uid is passed correctly in GET API ||"
			+ "SQ-T3894 POS Batch Redemption API>Validate that user is able to commit the locked discount_basket_item if reward locking is ON and external_uid is correct ||"
			+ "SQ-T3888 Verify that If user is passing correct external uid in {{path}}/api/mobile/discounts/unselect?client={{client}}&hash={{span}}API, then only user is able to delete the locked basket items. ||"
			+ "SQ-T3896 Verify that if Discount amount is not added in basket then Discount_details-NULL gets displayed in unselected discounts", groups = {
					"regression", "dailyrun" }, priority = 2)
	@Owner(name = "Hardik Bhardwaj")
	public void T3892_DiscountBasketItemNegativeScenario() throws InterruptedException {

//		 login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Navigate to Multiple Redemption Tab and Uncheck the flags
//		pageObj.menupage().clickCockPitMenu();
//		pageObj.menupage().redeemptionLinkInCockpit();
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_discount_locking", "check");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_auto_unlock", "check");
		pageObj.cockpitRedemptionsPage().setAutoUnlockPeriod("present", "10");
		pageObj.dashboardpage().updateCheckBox();

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
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "50",
				dataSet.get("redeemable_id"), "", "");
		utils.logit("Send redeemable to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		String redeemableID = dataSet.get("redeemable_id");

		// get reward id
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemableID);

		utils.logit("Reward id " + rewardId + " is generated successfully ");

		String externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		// AUTH Add Discount to Basket
		Response discountBasketResponse5 = pageObj.endpoints().addDiscountToBasketAUTH(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId, externalUID);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponse5.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		utils.logPass("AUTH add discount to basket is successful");
		String discount_basket_item_id = discountBasketResponse5.jsonPath()
				.get("discount_basket_items[0].discount_basket_item_id").toString();

//	OMM-T933 Verify that if Discount amount is not added in basket then Discount_details-NULL gets displayed in unselected discounts
		// POS Discount Lookup Api
		List<Object> obj = new ArrayList<Object>();
		Response discountLookupResponse0 = pageObj.endpoints().discountLookUpPosApi(dataSet.get("location_key"), userID,
				dataSet.get("item_id"), "30", externalUID);
		Assert.assertEquals(discountLookupResponse0.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Discount Lookup Api ");
		obj = discountLookupResponse0.jsonPath().getList("unselected_discounts");
		for (int i = 0; i < obj.size(); i++) {
			discount_details0 = discountLookupResponse0.jsonPath()
					.getString("unselected_discounts[" + i + "].discount_details");
			if (discount_details0 == null) {
				break;
			}
		}

		Assert.assertEquals(discount_details0, null, "discount_details is not NULL");
		utils.logPass("POS Discount Lookup Api gives discount details as NULL");

//		OMM-T757 Validate that If user is passing correct external uid in {{path}}/api/pos/discounts/select API, then only user is able to update the locked basket. 

		// AUTH Add Discount to Basket - add amount
		Response discountBasketResponse3 = pageObj.endpoints().addDiscountAmountToBasketAUTH(token,
				dataSet.get("client"), dataSet.get("secret"), "discount_amount", "10", externalUID);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponse3.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		utils.logPass("AUTH add discount to basket is successful add amount");

		// Deprecated https://punchhdev.atlassian.net/browse/OMM-1256
//		// AUTH Add Discount to Basket - invalid external id
		String externalUID2 = Integer.toString(Utilities.getRandomNoFromRange(1000, 50000));
//		Response discountBasketResponse2 = pageObj.endpoints().addDiscountToBasketAUTH(token, dataSet.get("client"),
//				dataSet.get("secret"), "discount_amount", "10", externalUID2);
//		Assert.assertEquals(422, discountBasketResponse2.getStatusCode(),
//				"Status code 422 did not match with add discount to basket ");
//		boolean isAuthAddDiscountInvalidExternalUidSchemaValidated = Utilities.validateJsonAgainstSchema(
//				ApiResponseJsonSchema.authMessageCodeErrorObjectSchema, discountBasketResponse2.asString());
//		Assert.assertTrue(isAuthAddDiscountInvalidExternalUidSchemaValidated,
//				"AUTH API Add Discount Schema Validation failed");
//		Assert.assertEquals(discountBasketResponse2.jsonPath().getString("error.message"),
//				"Unable to find valid discount basket.");
//		Assert.assertEquals(discountBasketResponse2.jsonPath().getString("error.code"), "invalid_basket");
//		TestListeners.extentTest.get()
//				.pass("AUTH add discount to basket is unsuccessful (expected) add amount with invalid external id");
//		logger.info("AUTH add discount to basket is unsuccessful (expected) add amount with invalid external id");

//		OMM-T820 GET ACTIVE API>Validate that user is able to get the details of locked discount_basket_item if reward locking is ON and external_uid is passed correctly in GET API

		// Auth API fetch active basket
		Response basketDiscountDetailsResponse = pageObj.endpoints().fetchActiveBasketAuthApi(token,
				dataSet.get("client"), dataSet.get("secret"), externalUID);
		Assert.assertEquals(basketDiscountDetailsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with fetch active basket ");
		utils.logPass("POS  fetch active basket is successful");

		// Deprecated https://punchhdev.atlassian.net/browse/OMM-1256
//		// Auth API fetch active basket
//		Response basketDiscountDetailsResponse2 = pageObj.endpoints().fetchActiveBasketAuthApi(token,
//				dataSet.get("client"), dataSet.get("secret"), externalUID2);
//		Assert.assertEquals(basketDiscountDetailsResponse2.getStatusCode(), 422,
//				"Status code 422 did not match with fetch active basket ");
//		boolean isAuthFetchActiveBasketInvalidBasketSchemaValidated = Utilities.validateJsonAgainstSchema(
//				ApiResponseJsonSchema.authMessageCodeErrorObjectSchema, basketDiscountDetailsResponse2.asString());
//		Assert.assertTrue(isAuthFetchActiveBasketInvalidBasketSchemaValidated,
//				"AUTH API Fetch Active Basket Schema Validation failed");
//		Assert.assertEquals(basketDiscountDetailsResponse2.jsonPath().getString("error.message"),
//				"Unable to find valid discount basket.");
//		Assert.assertEquals(basketDiscountDetailsResponse2.jsonPath().getString("error.code"), "invalid_basket");
//		TestListeners.extentTest.get()
//				.pass("POS fetch active basket is unsuccessful (expected) with invalid external_uid");
//		logger.info("POS fetch active basket is unsuccessful (expected) with invalid external_uid");

//		OMM-T824 POS discount lookup API>Validate that user is able to get the details of selected and unselected discounts of locked discount_basket_item if reward locking is ON and external_uid is correct

		// POS Discount Lookup Api - Valid ext_uid
		Response discountLookupResponse1 = pageObj.endpoints().discountLookUpPosApi(dataSet.get("location_key"), userID,
				dataSet.get("item_id"), "30", externalUID);
		Assert.assertEquals(discountLookupResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Discount Lookup Api ");
		String externalUidResponse2 = discountLookupResponse1.jsonPath().getString("external_uid");
		String locked2 = discountLookupResponse1.jsonPath().getString("locked");
		Assert.assertTrue(pageObj.cockpitRedemptionsPage().resultOfUserLookUpPosAPI(externalUidResponse2, locked2,
				externalUID, "true"));
		utils.logPass("POS user lookUp is successful with Valid ext_uid");

		// Deprecated https://punchhdev.atlassian.net/browse/OMM-1256
		// POS Discount Lookup Api - inValid ext_uid
//		Response discountLookupResponse = pageObj.endpoints().discountLookUpPosApi(dataSet.get("location_key"), userID,
//				dataSet.get("item_id"), "30", externalUID2);
//		Assert.assertEquals(discountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
//				"Status code 422 did not match with Discount Lookup Api ");
//		boolean isPosDiscountLookupInvalidExternalUidSchemaValidated = Utilities.validateJsonAgainstSchema(
//				ApiResponseJsonSchema.apiErrorObjectSchema, discountLookupResponse.asString());
//		Assert.assertTrue(isPosDiscountLookupInvalidExternalUidSchemaValidated,
//				"POS API Discount Lookup Schema Validation failed");
//		Assert.assertEquals(discountLookupResponse.jsonPath().getString("error"),
//				"Unable to find valid discount basket.");
//		utils.logPass("POS user lookUp is unsuccessful (expected) inValid ext_uid");
//		logger.info("POS user lookUp is unsuccessful (expected) with inValid ext_uid");

//		OMM-T821 DELETE API>Validate that user is unable to delete the discount_basket_item of locked discount_basket_item if reward locking is ON but external_uid is invalid/empty

//		// POS remove discount from basket - Invalid ext_uid and Invalid ext_uid but
//		// valid discount item ids
		String externalUID1 = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
//		Response deleteBasketResponse3 = pageObj.endpoints().removeDiscountFromBasketPOSAPI(dataSet.get("location_key"),
//				userID, discount_basket_item_id, externalUID1);
//		Assert.assertEquals(deleteBasketResponse3.getStatusCode(), 422,
//				"Status code 422 did not match with remove discount from basket ");
//		boolean isPosRemoveDiscountInvalidExtUidSchemaValidated = Utilities.validateJsonAgainstSchema(
//				ApiResponseJsonSchema.apiErrorObjectSchema, deleteBasketResponse3.asString());
//		Assert.assertTrue(isPosRemoveDiscountInvalidExtUidSchemaValidated,
//				"POS API Remove Discount Schema Validation failed");
//		Assert.assertEquals(deleteBasketResponse3.jsonPath().getString("error"),
//				"Unable to find valid discount basket.");
//		utils.logPass(
//				"POS remove discount from basket is unsuccessful (expected) with Invalid ext_uid and Invalid ext_uid but valid discount item ids");
//		logger.info(
//				"POS remove discount from basket is unsuccessful (expected) with Invalid ext_uid and Invalid ext_uid but valid discount item ids");

		// Deprecated https://punchhdev.atlassian.net/browse/OMM-1256
		// POS remove discount from basket - Not passing ext_uid
//		Response deleteBasketResponse4 = pageObj.endpoints().removeDiscountFromBasketPOSAPI(dataSet.get("location_key"),
//				userID, discount_basket_item_id, "");
//		Assert.assertEquals(deleteBasketResponse4.getStatusCode(), 400,
//				"Status code 400 did not match with remove discount from basket ");
//		boolean isPosRemoveDiscountMissingExtUidSchemaValidated = Utilities.validateJsonAgainstSchema(
//				ApiResponseJsonSchema.apiErrorObjectSchema, deleteBasketResponse4.asString());
//		Assert.assertTrue(isPosRemoveDiscountMissingExtUidSchemaValidated,
//				"POS API Remove Discount Schema Validation failed");
//		Assert.assertEquals(deleteBasketResponse4.jsonPath().getString("error"),
//				"Required parameter missing or the value is empty: external_uid");
//		TestListeners.extentTest.get()
//				.pass("POS remove discount from basket is unsuccessful (expected) with Not passing ext_uid");
//		logger.info("POS remove discount from basket is unsuccessful (expected) with Not passing ext_uid");
//
//		// Auth remove discount from basket - Valid ext_uid but invalid discount item
//		// ids
//		String expdiscount_basket_item_id = Integer.toString(Utilities.getRandomNoFromRange(1000, 5000));
//		Response deleteBasketID_Response = pageObj.endpoints().deleteItemFromBasket_AuthAPI(dataSet.get("client"),
//				dataSet.get("secret"), dataSet.get("location_key"), token, expdiscount_basket_item_id, externalUID);
//		Assert.assertEquals(deleteBasketID_Response.getStatusCode(), 422,
//				"Status code 422 did not match with remove discount from basket ");
//		boolean isAuthRemoveDiscountInvalidDiscountItemIdSchemaValidated = Utilities.validateJsonAgainstSchema(
//				ApiResponseJsonSchema.authMessageCodeErrorObjectSchema, deleteBasketID_Response.asString());
//		Assert.assertTrue(isAuthRemoveDiscountInvalidDiscountItemIdSchemaValidated,
//				"AUTH API Remove Discount from basket Schema Validation failed");
//		Assert.assertEquals(deleteBasketID_Response.jsonPath().getString("error.message"),
//				"Discount Items not found with any given ids.");
//		Assert.assertEquals(deleteBasketID_Response.jsonPath().getString("error.code"), "record_not_found");
//		utils.logPass(
//				"POS remove discount from basket is unsuccessful (expected) with Valid ext_uid but invalid discount item ids");
//		logger.info(
//				"POS remove discount from basket is unsuccessful (expected) with Valid ext_uid but invalid discount item ids");

////		OMM-T744 Verify that If user is passing correct external uid in {{path}}/api/mobile/discounts/unselect?client={{client}}&hash={{span}}API, then only user is able to delete the locked basket items.
//
//		Response deleteBasketResponse = pageObj.endpoints().removeDiscountBasketExtUIDSecureAPI(token,
//				dataSet.get("client"), dataSet.get("secret"), discount_basket_item_id, externalUID);
//		Assert.assertEquals(deleteBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
//				"Status code 200 did not match with remove discount from basket ");
//		logger.info("Secure API remove discount from basket is successful with external uid");
//		utils.logPass("Secure API remove discount from basket is successful with external uid");
//		logger.info("Secure API remove discount from basket is successful with external uid");

//		OMM-T825 POS Batch Redemption API>Validate that user is able to commit the locked discount_basket_item if reward locking is ON and external_uid is correct

		// User SignUp using API
		String userEmail1 = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse1 = pageObj.endpoints().Api2SignUp(userEmail1, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse1, "API 2 user signup");
		String token1 = signUpResponse1.jsonPath().get("access_token.token").toString();

		String userID1 = signUpResponse1.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("API2 Signup is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse1 = pageObj.endpoints().sendMessageToUser(userID1, dataSet.get("apiKey"), "50",
				dataSet.get("redeemable_id"), "", "");

		utils.logit("Send redeemable to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse1.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		// get reward id
		String rewardId2 = pageObj.redeemablesPage().getRewardId(token1, dataSet.get("client"), dataSet.get("secret"),
				redeemableID);

		utils.logit("Reward id " + rewardId2 + " is generated successfully ");

		// AUTH Add Discount to Basket
		Response discountBasketResponse = pageObj.endpoints().addDiscountToBasketAUTH(token1, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId2, externalUID1);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponse.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		utils.logPass("AUTH add discount to basket is successful");

		// POS Process Batch Redemption
		Response batchRedemptionProcessResponseUser1 = pageObj.endpoints().processBatchRedemptionPosApiPayload(
				dataSet.get("location_key"), userID1, "5", "1", "101", externalUID1);
		Assert.assertEquals(batchRedemptionProcessResponseUser1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Process Batch Redemption ");
		utils.logPass("POS Process Batch Redemption Api is successful");

		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail1);
		pageObj.guestTimelinePage().refreshTimeline();
		Thread.sleep(5000);
		String discountValueWebCheckin = pageObj.guestTimelinePage().getDiscountValueWebCheckinDetails();
		String totalamoutnWebCheckin = pageObj.guestTimelinePage().getTotalAmountWebCheckin();
		Assert.assertEquals("Amount: 5.00", totalamoutnWebCheckin, "total amount Checkin is not same");
		utils.logPass("total amount Checkin is same i.e. " + totalamoutnWebCheckin);
	}

	@Test(description = "OMM-T970 POS Batch Redemption> Validate that if Receipt amount is 0 then user is not able to process {{path}}/api/pos/batch_redemptions API ||"
			+ "OMM-T972 POS discount lookup> Validate that if Receipt amount is 0 then user is not able to process {{path}}/api/pos/discounts/lookup API ||"
			+ "OMM-T572 POS discount lookup API>Discount amount>Validate that if discount type->discount_amount is added into basket then no data gets displayed in discount_details under Selected discounts ||"
			+ "SQ-T3157 Auth Batch Redemption>Validate that if Redeemable with Flat discount gifted as reward is added into basket then mentioned INTEGER value in Base Amount under 'discount_details' gets displayed", groups = {
					"regression", "dailyrun" }, priority = 1)
	@Owner(name = "Hardik Bhardwaj")
	public void T970_ValidateThatIfReceiptAmountIs0() throws InterruptedException {

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
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "50",
				dataSet.get("redeemable_id"), "", "");

		utils.logit("Send redeemable to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		String externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		// AUTH Add Discount to Basket - add amount
		Response discountBasketResponse3 = pageObj.endpoints().addDiscountAmountToBasketAUTH(token,
				dataSet.get("client"), dataSet.get("secret"), "discount_amount", "10", externalUID);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponse3.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		utils.logPass("AUTH add discount to basket is successful add amount");
		String discountDetails = discountBasketResponse3.jsonPath()
				.getString("discount_basket_items[0].discount_details");
		Assert.assertEquals(null, discountDetails, "discount_details is not null for AUTH Add Discount to Basket");
		utils.logPass("discount_details is null for AUTH Add Discount to Basket");

//		OMM-T970 POS Batch Redemption> Validate that if Receipt amount is 0 then user is not able to process {{path}}/api/pos/batch_redemptions API

		// POS Process Batch Redemption
		Response batchRedemptionProcessResponseUser1 = pageObj.endpoints().processBatchRedemptionPosApiPayload(
				dataSet.get("location_key"), userID, "0.00", "1", dataSet.get("item_id"), externalUID);
		Assert.assertEquals(batchRedemptionProcessResponseUser1.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not match with Process Batch Redemption ");
		boolean isPosBatchRedemptionInvalidAmountSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, batchRedemptionProcessResponseUser1.asString());
		Assert.assertTrue(isPosBatchRedemptionInvalidAmountSchemaValidated,
				"POS API Batch Redemption Schema Validation failed");
		Assert.assertEquals(batchRedemptionProcessResponseUser1.jsonPath().getString("error"),
				"Invalid Receipt Amount.");
		utils.logPass("POS Process Batch Redemption Api is unsuccessful (expected)");

//		OMM-T972 POS discount lookup> Validate that if Receipt amount is 0 then user is not able to process {{path}}/api/pos/discounts/lookup API

		// POS Discount Lookup Api
		Response discountLookupResponse = pageObj.endpoints().discountLookUpPosApi(dataSet.get("location_key"), userID,
				dataSet.get("item_id"), "0.00", externalUID);
		Assert.assertEquals(discountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not match with Discount Lookup Api ");
		boolean isPosDiscountLookupInvalidAmountSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, discountLookupResponse.asString());
		Assert.assertTrue(isPosDiscountLookupInvalidAmountSchemaValidated,
				"POS API Discount Lookup Schema Validation failed");
		Assert.assertEquals(discountLookupResponse.jsonPath().getString("error"), "Invalid Receipt Amount.");
		utils.logPass("POS user lookUp is unsuccessful (expected) ");

//		OMM-T572 POS discount lookup API>Discount amount>Validate that if discount type->discount_amount is added into basket then no data gets displayed in discount_details under Selected discounts

		// POS fetch active basket
		Response basketDiscountDetailsResponse3 = pageObj.endpoints().fetchActiveBasketPOSAPI(userID,
				dataSet.get("location_key"), externalUID);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, basketDiscountDetailsResponse3.getStatusCode(),
				"Status code 200 did not matched for POS fetch active basket");
		utils.logPass("POS fetch active basket is successful");
		String discountDetails1 = basketDiscountDetailsResponse3.jsonPath()
				.getString("discount_basket_items[0].discount_details");
		Assert.assertEquals(null, discountDetails1, "discount_details is not null for POS fetch active basket");
		utils.logPass("discount_details is null for POS fetch active basket");

		// User SignUp using API
		String userEmail2 = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse2 = pageObj.endpoints().Api2SignUp(userEmail2, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse2, "API 2 user signup");
		Assert.assertEquals(signUpResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token2 = signUpResponse2.jsonPath().get("access_token.token").toString();
		String userID2 = signUpResponse2.jsonPath().get("user.user_id").toString();

//		SQ-T3157 Auth Batch Redemption>Validate that if Redeemable with Flat discount gifted as reward is added into basket then mentioned INTEGER value in Base Amount under 'discount_details' gets displayed

		// send reward amount to user Reedemable
		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(userID2, dataSet.get("apiKey"), "50",
				dataSet.get("redeemableWithFlatDiscount_id"), "", "");

		String redeemableID = dataSet.get("redeemableWithFlatDiscount_id");

		// get reward id
		String rewardId = pageObj.redeemablesPage().getRewardId(token2, dataSet.get("client"), dataSet.get("secret"),
				redeemableID);

		utils.logit("Send redeemable to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse2.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		// AUTH Add Discount to Basket
		Response discountBasketResponse5 = pageObj.endpoints().addDiscountToBasketAUTH(token2, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId, externalUID);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponse5.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		utils.logPass("AUTH add discount to basket is successful");
		String base_amount = discountBasketResponse5.jsonPath()
				.getString("discount_basket_items[0].discount_details.base_amount");
		Assert.assertEquals(base_amount, "1.0", "base_amount is null for AUTH Add Discount to Basket");
		utils.logPass("base_amount is not null for AUTH Add Discount to Basket");

		// Auth Process Batch Redemption
		Response batchRedemptionProcessResponse2 = pageObj.endpoints().processBatchRedemptionAUTHAPI(
				dataSet.get("client"), dataSet.get("secret"), dataSet.get("location_key"), token2, userID2, "101",
				externalUID);
		Assert.assertEquals(batchRedemptionProcessResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Process Batch Redemption ");
		String base_amount1 = batchRedemptionProcessResponse2.jsonPath()
				.getString("success[0].discount_details.base_amount");
		Assert.assertEquals(base_amount1, "1.0", "base_amount is null for Auth Process Batch Redemption to Basket");
		utils.logPass("base_amount is not null for Auth Process Batch Redemption to Basket");

	}

	// Shashank sharma
	@Test(description = " SQ-T6386 [Stacking ON, Reusability ON]Verify the discount calculation for Offer 1 and Offer 2 with base only LIS having sum of amounts LIF and Line item exists LIQ\n")
	public void T6386_ValidateDiscountCalculationBasedOnQC() throws InterruptedException {
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

		// send reward amount to user Reedemable1
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "50",
				dataSet.get("redeemable1_id"), "", "");
		utils.logit(dataSet.get("redeemable1_name") + " Send redeemable to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		// send reward amount to user Reedemable2
		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "50",
				dataSet.get("redeemable2_id"), "", "");
		utils.logit(dataSet.get("redeemable2_name") + " Send redeemable to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse2.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		String redeemableID1 = dataSet.get("redeemable1_id");

		// get reward id
		String rewardId1 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemableID1);

		utils.logit("Reward id " + rewardId1 + " is generated successfully ");

		String externalUID1 = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		// AUTH Add Discount to Basket
		Response discountBasketResponse1 = pageObj.endpoints().addDiscountToBasketAUTH(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId1, externalUID1);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponse1.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		utils.logPass("AUTH add discount to basket is successful");

		String redeemableID2 = dataSet.get("redeemable2_id");

		// get reward id
		String rewardId2 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemableID2);

		utils.logit("Reward id " + rewardId2 + " is generated successfully ");

		String externalUID2 = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		// AUTH Add Discount to Basket
		Response discountBasketResponse2 = pageObj.endpoints().addDiscountToBasketAUTH(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId2, externalUID2);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponse2.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		utils.logPass("AUTH add discount to basket is successful");

		// Add multiple items dynamically
		List<Map<String, Object>> lineItems = new ArrayList<>();
		Map<String, Object> receiptItems = new HashMap<String, Object>();

		// Sandwich|1||M|10|306|522|1.0

		receiptItems = ApiPayloads.getInputForReceiptItems(dataSet.get("item_name"),
				Integer.parseInt(dataSet.get("item_qty")), Double.parseDouble(dataSet.get("amount")),
				dataSet.get("item_type"), dataSet.get("item_id"), dataSet.get("item_family"), dataSet.get("item_group"),
				dataSet.get("serial_number"));
		lineItems.add(receiptItems);

		String receipt_datetime = CreateDateTime.getCurrentDateTimeInUtc();
		String punchh_key = Integer.toString(Utilities.getRandomNoFromRange(100000000, 500000000));
		String transaction_no = Integer.toString(Utilities.getRandomNoFromRange(500000000, 900000000));

		Response discountLookupResponse0 = pageObj.endpoints().posDiscountLookupAPIInputPayload(lineItems,
				receipt_datetime, Double.parseDouble(dataSet.get("subtotal_amount")),
				Double.parseDouble(dataSet.get("receipt_amount")), punchh_key, transaction_no, userID,
				dataSet.get("locationKeyNew"), externalUID1);

		Assert.assertEquals(discountLookupResponse0.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Discount Lookup Api ");
		// check discount ammount for redeemable 1

		int actualItemIdForRedeemable1 = discountLookupResponse0.jsonPath()
				.getInt("selected_discounts[0].discount_details.item_id");
		int expItemIdForRedeemable1 = Integer.parseInt(dataSet.get("redeemable1_id"));
		Assert.assertEquals(actualItemIdForRedeemable1, expItemIdForRedeemable1, actualItemIdForRedeemable1
				+ " actual item did not matched with expected itemid " + dataSet.get("redeemable2_id"));
		utils.logPass(actualItemIdForRedeemable1 + " actual item is matched with expected itemid "
				+ dataSet.get("redeemable2_id"));

		String actualRedeemableName1 = discountLookupResponse0.jsonPath()
				.getString("selected_discounts[0].discount_details.name");
		Assert.assertEquals(actualRedeemableName1, dataSet.get("redeemable1_name"), actualRedeemableName1
				+ " actual redeemable name did not matched with expected itemid " + dataSet.get("redeemable1_name"));
		utils.logPass(actualRedeemableName1 + " actual redeemable name is matched with expected redeemable name "
				+ dataSet.get("redeemable1_name"));

		double actualdiscount_amountForRedeemable1 = discountLookupResponse0.jsonPath()
				.getDouble("selected_discounts[0].discount_amount");
		double expdiscount_amountdForRedeemable1 = Double.parseDouble(dataSet.get("expactualdiscount_amount1"));
		Assert.assertEquals(actualdiscount_amountForRedeemable1, expdiscount_amountdForRedeemable1,
				actualdiscount_amountForRedeemable1
						+ " actual discount_amount did not matched with expected discount_amount "
						+ expdiscount_amountdForRedeemable1);
		utils.logPass(actualdiscount_amountForRedeemable1
				+ " actual discount_amount is matched with expected discount_amount "
				+ expdiscount_amountdForRedeemable1);

		double actualItem_qty0_firstRedeemable = discountLookupResponse0.jsonPath()
				.getDouble("selected_discounts[0].qualified_items[0].item_qty");
		double expItem_qty0_firstRedeemable = Double.parseDouble(dataSet.get("expItem_qty0_firstRedeemable"));
		Assert.assertEquals(actualItem_qty0_firstRedeemable, expItem_qty0_firstRedeemable,
				actualItem_qty0_firstRedeemable + " actual item qty did not matched with expected item qty"
						+ expItem_qty0_firstRedeemable);
		utils.logPass(actualItem_qty0_firstRedeemable
				+ " actual item qty is matched with expected item qty" + expItem_qty0_firstRedeemable);

		double actualAmount0_firstRedeemable = discountLookupResponse0.jsonPath()
				.getDouble("selected_discounts[0].qualified_items[0].amount");
		double expAmount0_firstRedeemable = Double.parseDouble(dataSet.get("expAmount0_firstRedeemable"));
		Assert.assertEquals(actualAmount0_firstRedeemable, expAmount0_firstRedeemable, actualAmount0_firstRedeemable
				+ " actual item amount did not matched with expected item amount " + expAmount0_firstRedeemable);
		utils.logPass(actualAmount0_firstRedeemable
				+ " actual item amount is matched with expected item qty" + expAmount0_firstRedeemable);

		double actualItem_qty1_firstRedeemable = discountLookupResponse0.jsonPath()
				.getDouble("selected_discounts[0].qualified_items[1].item_qty");
		double expItem_qty1_firstRedeemable = Double.parseDouble(dataSet.get("expItem_qty1_firstRedeemable"));
		Assert.assertEquals(actualItem_qty1_firstRedeemable, expItem_qty1_firstRedeemable,
				actualItem_qty1_firstRedeemable + " actual item qty did not matched with expected item qty"
						+ expItem_qty1_firstRedeemable);
		utils.logPass(actualItem_qty1_firstRedeemable
				+ " actual item qty is matched with expected item qty" + expItem_qty1_firstRedeemable);

		String actualAmount1_firstRedeemable = discountLookupResponse0.jsonPath()
				.getString("selected_discounts[0].qualified_items[1].amount");
		String expAmount1_firstRedeemable = dataSet.get("expAmount1_firstRedeemable");
		Assert.assertEquals(actualAmount1_firstRedeemable, expAmount1_firstRedeemable, actualAmount1_firstRedeemable
				+ " actual item amount did not matched with expected item amount" + expAmount1_firstRedeemable);
		utils.logPass(actualAmount1_firstRedeemable
				+ " actual item amount is matched with expected item amount " + expAmount1_firstRedeemable);

		int actualItemIdForRedeemable2 = discountLookupResponse0.jsonPath()
				.getInt("selected_discounts[1].discount_details.item_id");
		int expItemIdForRedeemable2 = Integer.parseInt(dataSet.get("redeemable2_id"));
		Assert.assertEquals(actualItemIdForRedeemable2, expItemIdForRedeemable2, actualItemIdForRedeemable2
				+ " actual item did not matched with expected itemid " + dataSet.get("redeemable2_id"));
		utils.logPass(actualItemIdForRedeemable2 + " actual item is matched with expected itemid "
				+ dataSet.get("redeemable2_id"));

		String actualRedeemableName2 = discountLookupResponse0.jsonPath()
				.getString("selected_discounts[1].discount_details.name");
		Assert.assertEquals(actualRedeemableName2, dataSet.get("redeemable2_name"), actualRedeemableName2
				+ " actual redeemable name did not matched with expected itemid " + dataSet.get("redeemable2_name"));
		utils.logPass(actualRedeemableName2 + " actual redeemable name is matched with expected redeemable name "
				+ dataSet.get("redeemable2_name"));

		double actualdiscount_amountForRedeemable2 = discountLookupResponse0.jsonPath()
				.getDouble("selected_discounts[1].discount_amount");
		double expdiscount_amountdForRedeemable2 = Double.parseDouble(dataSet.get("expactualdiscount_amount2"));
		Assert.assertEquals(actualdiscount_amountForRedeemable2, expdiscount_amountdForRedeemable2,
				actualdiscount_amountForRedeemable2
						+ " actual discount_amount did not matched with expected discount_amount "
						+ expdiscount_amountdForRedeemable2);
		utils.logPass(actualdiscount_amountForRedeemable2
				+ " actual discount_amount is matched with expected discount_amount "
				+ expdiscount_amountdForRedeemable2);

		double actualItem_qty0_secondRedeemable = discountLookupResponse0.jsonPath()
				.getDouble("selected_discounts[1].qualified_items[0].item_qty");
		double expItem_qty0_secondRedeemable = Double.parseDouble(dataSet.get("expItem_qty0_secondRedeemable"));
		Assert.assertEquals(actualItem_qty0_secondRedeemable, expItem_qty0_secondRedeemable,
				actualItem_qty0_secondRedeemable + " actual item qty did not matched with expected item qty"
						+ expItem_qty0_secondRedeemable);
		utils.logPass(actualItem_qty0_secondRedeemable
				+ " actual item qty is matched with expected item qty" + expItem_qty0_secondRedeemable);

		double actualAmount0_secondRedeemable = discountLookupResponse0.jsonPath()
				.getDouble("selected_discounts[1].qualified_items[0].amount");
		double expAmount0_secondRedeemable = Double.parseDouble(dataSet.get("expAmount0_secondRedeemable"));
		Assert.assertEquals(actualAmount0_secondRedeemable, expAmount0_secondRedeemable, actualAmount0_secondRedeemable
				+ " actual item amount did not matched with expected item amount " + expAmount0_secondRedeemable);
		utils.logPass(actualAmount0_secondRedeemable
				+ " actual item amount is matched with expected item amount" + expAmount0_secondRedeemable);

		double actualItem_qty1_secondRedeemable = discountLookupResponse0.jsonPath()
				.getDouble("selected_discounts[1].qualified_items[1].item_qty");
		double expItem_qty1_secondRedeemable = Double.parseDouble(dataSet.get("expItem_qty1_secondRedeemable"));
		Assert.assertEquals(actualItem_qty1_secondRedeemable, expItem_qty1_secondRedeemable,
				actualItem_qty1_secondRedeemable + " actual item qty did not matched with expected item qty"
						+ expItem_qty1_secondRedeemable);
		utils.logPass(actualItem_qty1_secondRedeemable
				+ " actual item qty is matched with expected item qty" + expItem_qty1_secondRedeemable);

		String actualAmount1_secondRedeemable = discountLookupResponse0.jsonPath()
				.getString("selected_discounts[1].qualified_items[1].amount");
		String expAmount1_secondRedeemable = dataSet.get("expAmount1_secondRedeemable");
		Assert.assertEquals(actualAmount1_secondRedeemable, expAmount1_secondRedeemable, actualAmount1_secondRedeemable
				+ " actual item amount did not matched with expected item amount" + expAmount1_secondRedeemable);
		utils.logPass(actualAmount1_secondRedeemable
				+ " actual item amount is matched with expected item amount " + expAmount1_secondRedeemable);

	}

	// Shashank sharma // https://punchhdev.atlassian.net/browse/OMM-1176 right now
	// Rounding rule is not implemented so this test case is not valid and expected
	// result are updated as par current implementation
	@Test(description = "SQ-T6387 [Stacking ON, Reusability ON]Verify the discount calculation for Offer 1 and Offer 2 with rounding rule and effective location",groups = {"nonNightly" })
	@Owner(name = "Shashank Sharma")
	public void T6387_ValidateDiscountCalculationBasedOnQCAsForCeilAndFloorRoundingRule() throws Exception {
		double expdiscount_amountdForRedeemable1 = 0.0;
		String expAmount1_firstRedeemable = "";
		double expdiscount_amountdForRedeemable2 = 0.0;
		String expAmount1_secondRedeemable = "";

		String query1 = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ dataSet.get("business_id") + "';";
		pageObj.singletonDBUtilsObj();
		String preferences = DBUtils.executeQueryAndGetColumnValue(env, query1, "preferences");

		pageObj.singletonDBUtilsObj();
		List<String> keyValueFromPreferences_Kafka = Utilities.getPreferencesKeyValue(preferences,
				"enable_decoupled_redemption_engine");
		if (keyValueFromPreferences_Kafka.contains("true")) {
			utils.logPass("enable_decoupled_redemption_engine is set to TRUE in preferences");
			expdiscount_amountdForRedeemable1 = Double.parseDouble(dataSet.get("expactualdiscount_amount1_FlagON"));
			expAmount1_firstRedeemable = dataSet.get("expAmount1_firstRedeemable_FlagON");
			expdiscount_amountdForRedeemable2 = Double.parseDouble(dataSet.get("expactualdiscount_amount2_FlagON"));
			expAmount1_secondRedeemable = dataSet.get("expAmount1_secondRedeemable_FlagON");
		} else if (keyValueFromPreferences_Kafka.contains("false")) {
			utils.logPass("enable_decoupled_redemption_engine is set to FALSE in preferences");

			expdiscount_amountdForRedeemable1 = Double.parseDouble(dataSet.get("expactualdiscount_amount1_FlagOFF"));
			expAmount1_firstRedeemable = dataSet.get("expAmount1_firstRedeemable_FlagOFF");
			expdiscount_amountdForRedeemable2 = Double.parseDouble(dataSet.get("expactualdiscount_amount2_FlagOFF"));
			expAmount1_secondRedeemable = dataSet.get("expAmount1_secondRedeemable_FlagOFF");

		}

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

		// send reward amount to user Reedemable1
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "50",
				dataSet.get("redeemable1_id"), "", "");
		utils.logit(dataSet.get("redeemable1_name") + " Send redeemable to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		// send reward amount to user Reedemable2
		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "50",
				dataSet.get("redeemable2_id"), "", "");
		utils.logit(dataSet.get("redeemable2_name") + " Send redeemable to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse2.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		String redeemableID1 = dataSet.get("redeemable1_id");

		// get reward id
		String rewardId1 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemableID1);

		utils.logit("Reward id " + rewardId1 + " is generated successfully ");

		String externalUID1 = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		// AUTH Add Discount to Basket
		Response discountBasketResponse1 = pageObj.endpoints().addDiscountToBasketAUTH(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId1, externalUID1);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponse1.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		utils.logPass("AUTH add discount to basket is successful");

		String redeemableID2 = dataSet.get("redeemable2_id");

		// get reward id of redeemable2
		String rewardId2 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemableID2);

		utils.logit("Reward id " + rewardId2 + " is generated successfully ");

		String externalUID2 = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		// AUTH Add Discount to Basket
		Response discountBasketResponse2 = pageObj.endpoints().addDiscountToBasketAUTH(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId2, externalUID2);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponse2.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		utils.logPass("AUTH add discount to basket is successful");

		// Add multiple items dynamically
		List<Map<String, Object>> lineItems = new ArrayList<>();
		Map<String, Object> receiptItems = new HashMap<String, Object>();
		Map<String, Object> receiptItems2 = new HashMap<String, Object>();

		// Sandwich|2|10.4|M|101|1001|23|1.0

		receiptItems = ApiPayloads.getInputForReceiptItems("AAVacado", Integer.parseInt(dataSet.get("item_qty")),
				Double.parseDouble(dataSet.get("amount")), dataSet.get("item_type"), dataSet.get("item_id"),
				dataSet.get("item_family"), dataSet.get("item_group"), dataSet.get("serial_number"));
		lineItems.add(receiptItems);

		// Sandwich|2|18.9|M|201|1001|23|2.0
		receiptItems2 = ApiPayloads.getInputForReceiptItems(dataSet.get("item_name"),
				Integer.parseInt(dataSet.get("item_qty")), Double.parseDouble(dataSet.get("amount2")),
				dataSet.get("item_type"), dataSet.get("item_id2"), dataSet.get("item_family"),
				dataSet.get("item_group"), dataSet.get("serial_number"));
		lineItems.add(receiptItems2);

		String receipt_datetime = CreateDateTime.getCurrentDateTimeInUtc();
		String punchh_key = Integer.toString(Utilities.getRandomNoFromRange(100000000, 500000000));
		String transaction_no = Integer.toString(Utilities.getRandomNoFromRange(500000000, 900000000));

		// POS Discount Lookup Api
		Response discountLookupResponse0 = pageObj.endpoints().posDiscountLookupAPIInputPayload(lineItems,
				receipt_datetime, Double.parseDouble(dataSet.get("subtotal_amount")),
				Double.parseDouble(dataSet.get("receipt_amount")), punchh_key, transaction_no, userID,
				dataSet.get("locationKeyNew"), externalUID1);

		Assert.assertEquals(discountLookupResponse0.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Discount Lookup Api ");
		// check discount ammount for redeemable 1
		int actualItemIdForRedeemable1 = discountLookupResponse0.jsonPath()
				.getInt("selected_discounts[0].discount_details.item_id");
		int expItemIdForRedeemable1 = Integer.parseInt(dataSet.get("redeemable1_id"));
		Assert.assertEquals(actualItemIdForRedeemable1, expItemIdForRedeemable1, actualItemIdForRedeemable1
				+ " actual item did not matched with expected itemid " + dataSet.get("redeemable2_id"));
		utils.logPass(actualItemIdForRedeemable1 + " actual item is matched with expected itemid "
				+ dataSet.get("redeemable2_id"));

		// Check redeemable1 in response
		String actualRedeemableName1 = discountLookupResponse0.jsonPath()
				.getString("selected_discounts[0].discount_details.name");
		Assert.assertEquals(actualRedeemableName1, dataSet.get("redeemable1_name"), actualRedeemableName1
				+ " actual redeemable name did not matched with expected itemid " + dataSet.get("redeemable1_name"));
		utils.logPass(actualRedeemableName1 + " actual redeemable name is matched with expected redeemable name "
				+ dataSet.get("redeemable1_name"));

		// Check discount ammount for redeemable1:
		double actualdiscount_amountForRedeemable1 = discountLookupResponse0.jsonPath()
				.getDouble("selected_discounts[0].discount_amount");

		Assert.assertEquals(actualdiscount_amountForRedeemable1, expdiscount_amountdForRedeemable1,
				actualdiscount_amountForRedeemable1
						+ " actual discount_amount did not matched with expected discount_amount "
						+ expdiscount_amountdForRedeemable1);
		utils.logPass(actualdiscount_amountForRedeemable1
				+ " actual discount_amount is matched with expected discount_amount "
				+ expdiscount_amountdForRedeemable1);

		// Check item quantity for first qualified item
		double actualItem_qty0_firstRedeemable = discountLookupResponse0.jsonPath()
				.getDouble("selected_discounts[0].qualified_items[0].item_qty");
		double expItem_qty0_firstRedeemable = Double.parseDouble(dataSet.get("expItem_qty0_firstRedeemable"));
		Assert.assertEquals(actualItem_qty0_firstRedeemable, expItem_qty0_firstRedeemable,
				actualItem_qty0_firstRedeemable + " actual item qty did not matched with expected item qty"
						+ expItem_qty0_firstRedeemable);
		utils.logPass(actualItem_qty0_firstRedeemable
				+ " actual item qty is matched with expected item qty" + expItem_qty0_firstRedeemable);

		// Check qualified amount for first qualified redeemable
		double actualAmount0_firstRedeemable = discountLookupResponse0.jsonPath()
				.getDouble("selected_discounts[0].qualified_items[0].amount");
		double expAmount0_firstRedeemable = Double.parseDouble(dataSet.get("expAmount0_firstRedeemable"));
		Assert.assertEquals(actualAmount0_firstRedeemable, expAmount0_firstRedeemable, actualAmount0_firstRedeemable
				+ " actual item amount did not matched with expected item amount " + expAmount0_firstRedeemable);
		utils.logPass(actualAmount0_firstRedeemable
				+ " actual item amount is matched with expected item qty" + expAmount0_firstRedeemable);

		// check item quantity for second qualified item
		double actualItem_qty1_firstRedeemable = discountLookupResponse0.jsonPath()
				.getDouble("selected_discounts[0].qualified_items[1].item_qty");
		double expItem_qty1_firstRedeemable = Double.parseDouble(dataSet.get("expItem_qty1_firstRedeemable"));
		Assert.assertEquals(actualItem_qty1_firstRedeemable, expItem_qty1_firstRedeemable,
				actualItem_qty1_firstRedeemable + " actual item qty did not matched with expected item qty"
						+ expItem_qty1_firstRedeemable);
		utils.logPass(actualItem_qty1_firstRedeemable
				+ " actual item qty is matched with expected item qty" + expItem_qty1_firstRedeemable);

		// Check qualified amount for second qualified redeemable:- Test123
		String actualAmount1_firstRedeemable = discountLookupResponse0.jsonPath()
				.getString("selected_discounts[0].qualified_items[1].amount");

		Assert.assertEquals(actualAmount1_firstRedeemable, expAmount1_firstRedeemable, actualAmount1_firstRedeemable
				+ " actual item amount did not matched with expected item amount" + expAmount1_firstRedeemable);
		utils.logPass(actualAmount1_firstRedeemable
				+ " actual item amount is matched with expected item amount " + expAmount1_firstRedeemable);

		// Check item id for redeemable 2
		int actualItemIdForRedeemable2 = discountLookupResponse0.jsonPath()
				.getInt("selected_discounts[1].discount_details.item_id");
		int expItemIdForRedeemable2 = Integer.parseInt(dataSet.get("redeemable2_id"));
		Assert.assertEquals(actualItemIdForRedeemable2, expItemIdForRedeemable2, actualItemIdForRedeemable2
				+ " actual item did not matched with expected itemid " + dataSet.get("redeemable2_id"));
		utils.logPass(actualItemIdForRedeemable2 + " actual item is matched with expected itemid "
				+ dataSet.get("redeemable2_id"));

		// Check redeemable2 name in response
		String actualRedeemableName2 = discountLookupResponse0.jsonPath()
				.getString("selected_discounts[1].discount_details.name");
		Assert.assertEquals(actualRedeemableName2, dataSet.get("redeemable2_name"), actualRedeemableName2
				+ " actual redeemable name did not matched with expected itemid " + dataSet.get("redeemable2_name"));
		utils.logPass(actualRedeemableName2 + " actual redeemable name is matched with expected redeemable name "
				+ dataSet.get("redeemable2_name"));

		// check discount amount for redeemable2
		double actualdiscount_amountForRedeemable2 = discountLookupResponse0.jsonPath()
				.getDouble("selected_discounts[1].discount_amount");
		Assert.assertEquals(actualdiscount_amountForRedeemable2, expdiscount_amountdForRedeemable2,
				actualdiscount_amountForRedeemable2
						+ " actual discount_amount did not matched with expected discount_amount "
						+ expdiscount_amountdForRedeemable2);
		utils.logPass(actualdiscount_amountForRedeemable2
				+ " actual discount_amount is matched with expected discount_amount "
				+ expdiscount_amountdForRedeemable2);

		// Check item quantity for first qualified item of redeemable2
		double actualItem_qty0_secondRedeemable = discountLookupResponse0.jsonPath()
				.getDouble("selected_discounts[1].qualified_items[0].item_qty");
		double expItem_qty0_secondRedeemable = Double.parseDouble(dataSet.get("expItem_qty0_secondRedeemable"));
		Assert.assertEquals(actualItem_qty0_secondRedeemable, expItem_qty0_secondRedeemable,
				actualItem_qty0_secondRedeemable + " actual item qty did not matched with expected item qty"
						+ expItem_qty0_secondRedeemable);
		utils.logPass(actualItem_qty0_secondRedeemable
				+ " actual item qty is matched with expected item qty" + expItem_qty0_secondRedeemable);

		// Check qualified amount for first qualified redeemable2
		double actualAmount0_secondRedeemable = discountLookupResponse0.jsonPath()
				.getDouble("selected_discounts[1].qualified_items[0].amount");
		double expAmount0_secondRedeemable = Double.parseDouble(dataSet.get("expAmount0_secondRedeemable"));
		Assert.assertEquals(actualAmount0_secondRedeemable, expAmount0_secondRedeemable, actualAmount0_secondRedeemable
				+ " actual item amount did not matched with expected item amount " + expAmount0_secondRedeemable);
		utils.logPass(actualAmount0_secondRedeemable
				+ " actual item amount is matched with expected item amount" + expAmount0_secondRedeemable);

		// check item quantity for second qualified item of redeemable2
		double actualItem_qty1_secondRedeemable = discountLookupResponse0.jsonPath()
				.getDouble("selected_discounts[1].qualified_items[1].item_qty");
		double expItem_qty1_secondRedeemable = Double.parseDouble(dataSet.get("expItem_qty1_secondRedeemable"));
		Assert.assertEquals(actualItem_qty1_secondRedeemable, expItem_qty1_secondRedeemable,
				actualItem_qty1_secondRedeemable + " actual item qty did not matched with expected item qty"
						+ expItem_qty1_secondRedeemable);
		utils.logPass(actualItem_qty1_secondRedeemable
				+ " actual item qty is matched with expected item qty" + expItem_qty1_secondRedeemable);

		// Check qualified amount for second qualified redeemable2
		String actualAmount1_secondRedeemable = discountLookupResponse0.jsonPath()
				.getString("selected_discounts[1].qualified_items[1].amount");
		Assert.assertEquals(actualAmount1_secondRedeemable, expAmount1_secondRedeemable, actualAmount1_secondRedeemable
				+ " actual item amount did not matched with expected item amount" + expAmount1_secondRedeemable);
		utils.logPass(actualAmount1_secondRedeemable
				+ " actual item amount is matched with expected item amount " + expAmount1_secondRedeemable);

	}

	@Test(description = "SQ-T6388 [Stacking ON, Reusability ON]Verify the discount calculation for Offer 1 and Offer 2 with 100% discount on item")
	@Owner(name = "Shashank Sharma")
	public void T6388_ValidateDiscountCalculationBasedOnQCAsFullDiscount() throws InterruptedException {
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

		// send reward amount to user Reedemable1
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "50",
				dataSet.get("redeemable1_id"), "", "");
		utils.logit(dataSet.get("redeemable1_name") + " Send redeemable to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		// send reward amount to user Reedemable2
		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "50",
				dataSet.get("redeemable2_id"), "", "");
		utils.logit(dataSet.get("redeemable2_name") + " Send redeemable to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse2.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		String redeemableID1 = dataSet.get("redeemable1_id");

		// get reward id
		String rewardId1 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemableID1);

		utils.logit("Reward id " + rewardId1 + " is generated successfully ");

		String externalUID1 = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		// AUTH Add Discount to Basket
		Response discountBasketResponse1 = pageObj.endpoints().addDiscountToBasketAUTH(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId1, externalUID1);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponse1.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		utils.logPass("AUTH add discount to basket is successful");

		String redeemableID2 = dataSet.get("redeemable2_id");

		// get reward id of redeemable2
		String rewardId2 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemableID2);

		utils.logit("Reward id " + rewardId2 + " is generated successfully ");

		String externalUID2 = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		// AUTH Add Discount to Basket
		Response discountBasketResponse2 = pageObj.endpoints().addDiscountToBasketAUTH(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId2, externalUID2);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponse2.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		utils.logPass("AUTH add discount to basket is successful");

		// Add multiple items dynamically
		List<Map<String, Object>> lineItems = new ArrayList<>();
		Map<String, Object> receiptItems = new HashMap<String, Object>();
		Map<String, Object> receiptItems2 = new HashMap<String, Object>();

		// Sandwich|2|10.4|M|101|1001|23|1.0

		receiptItems = ApiPayloads.getInputForReceiptItems("Avocado", Integer.parseInt(dataSet.get("item_qty")),
				Double.parseDouble(dataSet.get("amount")), dataSet.get("item_type"), dataSet.get("item_id"),
				dataSet.get("item_family"), dataSet.get("item_group"), dataSet.get("serial_number"));
		lineItems.add(receiptItems);

		// Sandwich|2|18.9|M|201|1001|23|2.0
		receiptItems2 = ApiPayloads.getInputForReceiptItems(dataSet.get("item_name"),
				Integer.parseInt(dataSet.get("item_qty")), Double.parseDouble(dataSet.get("amount2")),
				dataSet.get("item_type"), dataSet.get("item_id2"), dataSet.get("item_family"),
				dataSet.get("item_group"), dataSet.get("serial_number"));
		lineItems.add(receiptItems2);

		String receipt_datetime = CreateDateTime.getCurrentDateTimeInUtc();
		String punchh_key = Integer.toString(Utilities.getRandomNoFromRange(100000000, 500000000));
		String transaction_no = Integer.toString(Utilities.getRandomNoFromRange(500000000, 900000000));

		// POS Discount Lookup Api
		Response discountLookupResponse0 = pageObj.endpoints().posDiscountLookupAPIInputPayload(lineItems,
				receipt_datetime, Double.parseDouble(dataSet.get("subtotal_amount")),
				Double.parseDouble(dataSet.get("receipt_amount")), punchh_key, transaction_no, userID,
				dataSet.get("locationKeyNew"), externalUID1);

		Assert.assertEquals(discountLookupResponse0.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Discount Lookup Api ");
		// check discount ammount for redeemable 1
		int actualItemIdForRedeemable1 = discountLookupResponse0.jsonPath()
				.getInt("selected_discounts[0].discount_details.item_id");
		int expItemIdForRedeemable1 = Integer.parseInt(dataSet.get("redeemable1_id"));
		Assert.assertEquals(actualItemIdForRedeemable1, expItemIdForRedeemable1, actualItemIdForRedeemable1
				+ " actual item did not matched with expected itemid " + dataSet.get("redeemable1_id"));
		utils.logPass(actualItemIdForRedeemable1 + " actual item is matched with expected itemid "
				+ dataSet.get("redeemable1_id"));

		// Check redeemable1 in response
		String actualRedeemableName1 = discountLookupResponse0.jsonPath()
				.getString("selected_discounts[0].discount_details.name");
		Assert.assertEquals(actualRedeemableName1, dataSet.get("redeemable1_name"), actualRedeemableName1
				+ " actual redeemable name did not matched with expected itemid " + dataSet.get("redeemable1_name"));
		utils.logPass(actualRedeemableName1 + " actual redeemable name is matched with expected redeemable name "
				+ dataSet.get("redeemable1_name"));

		// Check discount ammount for redeemable1
		double actualdiscount_amountForRedeemable1 = discountLookupResponse0.jsonPath()
				.getDouble("selected_discounts[0].discount_amount");
		double expdiscount_amountdForRedeemable1 = Double.parseDouble(dataSet.get("expactualdiscount_amount1"));
		Assert.assertEquals(actualdiscount_amountForRedeemable1, expdiscount_amountdForRedeemable1,
				actualdiscount_amountForRedeemable1
						+ " actual discount_amount did not matched with expected discount_amount "
						+ expdiscount_amountdForRedeemable1);
		utils.logPass(actualdiscount_amountForRedeemable1
				+ " actual discount_amount is matched with expected discount_amount "
				+ expdiscount_amountdForRedeemable1);

		// Check item quantity for first qualified item
		double actualItem_qty0_firstRedeemable = discountLookupResponse0.jsonPath()
				.getDouble("selected_discounts[0].qualified_items[0].item_qty");
		double expItem_qty0_firstRedeemable = Double.parseDouble(dataSet.get("expItem_qty0_firstRedeemable"));
		Assert.assertEquals(actualItem_qty0_firstRedeemable, expItem_qty0_firstRedeemable,
				actualItem_qty0_firstRedeemable + " actual item qty did not matched with expected item qty"
						+ expItem_qty0_firstRedeemable);
		utils.logPass(actualItem_qty0_firstRedeemable
				+ " actual item qty is matched with expected item qty" + expItem_qty0_firstRedeemable);

		// Check qualified amount for first qualified redeemable
		double actualAmount0_firstRedeemable = discountLookupResponse0.jsonPath()
				.getDouble("selected_discounts[0].qualified_items[0].amount");
		double expAmount0_firstRedeemable = Double.parseDouble(dataSet.get("expAmount0_firstRedeemable"));
		Assert.assertEquals(actualAmount0_firstRedeemable, expAmount0_firstRedeemable, actualAmount0_firstRedeemable
				+ " actual item amount did not matched with expected item amount " + expAmount0_firstRedeemable);
		utils.logPass(actualAmount0_firstRedeemable
				+ " actual item amount is matched with expected item qty" + expAmount0_firstRedeemable);

		// check item quantity for second qualified item
		double actualItem_qty1_firstRedeemable = discountLookupResponse0.jsonPath()
				.getDouble("selected_discounts[0].qualified_items[1].item_qty");
		double expItem_qty1_firstRedeemable = Double.parseDouble(dataSet.get("expItem_qty1_firstRedeemable"));
		Assert.assertEquals(actualItem_qty1_firstRedeemable, expItem_qty1_firstRedeemable,
				actualItem_qty1_firstRedeemable + " actual item qty did not matched with expected item qty"
						+ expItem_qty1_firstRedeemable);
		utils.logPass(actualItem_qty1_firstRedeemable
				+ " actual item qty is matched with expected item qty" + expItem_qty1_firstRedeemable);

		// Check qualified amount for second qualified redeemable
		String actualAmount1_firstRedeemable = discountLookupResponse0.jsonPath()
				.getString("selected_discounts[0].qualified_items[1].amount");
		String expAmount1_firstRedeemable = dataSet.get("expAmount1_firstRedeemable");
		Assert.assertEquals(actualAmount1_firstRedeemable, expAmount1_firstRedeemable, actualAmount1_firstRedeemable
				+ " actual item amount did not matched with expected item amount" + expAmount1_firstRedeemable);
		utils.logPass(actualAmount1_firstRedeemable
				+ " actual item amount is matched with expected item amount " + expAmount1_firstRedeemable);

		// Check item id for redeemable 2
		int actualItemIdForRedeemable2 = discountLookupResponse0.jsonPath()
				.getInt("selected_discounts[1].discount_details.item_id");
		int expItemIdForRedeemable2 = Integer.parseInt(dataSet.get("redeemable2_id"));
		Assert.assertEquals(actualItemIdForRedeemable2, expItemIdForRedeemable2, actualItemIdForRedeemable2
				+ " actual item did not matched with expected itemid " + dataSet.get("redeemable2_id"));
		utils.logPass(actualItemIdForRedeemable2 + " actual item is matched with expected itemid "
				+ dataSet.get("redeemable2_id"));

		// Check redeemable2 name in response
		String actualRedeemableName2 = discountLookupResponse0.jsonPath()
				.getString("selected_discounts[1].discount_details.name");
		Assert.assertEquals(actualRedeemableName2, dataSet.get("redeemable2_name"), actualRedeemableName2
				+ " actual redeemable name did not matched with expected itemid " + dataSet.get("redeemable2_name"));
		utils.logPass(actualRedeemableName2 + " actual redeemable name is matched with expected redeemable name "
				+ dataSet.get("redeemable2_name"));

		// check discount amount for redeemable2
		double actualdiscount_amountForRedeemable2 = discountLookupResponse0.jsonPath()
				.getDouble("selected_discounts[1].discount_amount");
		double expdiscount_amountdForRedeemable2 = Double.parseDouble(dataSet.get("expactualdiscount_amount2"));
		Assert.assertEquals(actualdiscount_amountForRedeemable2, expdiscount_amountdForRedeemable2,
				actualdiscount_amountForRedeemable2
						+ " actual discount_amount did not matched with expected discount_amount "
						+ expdiscount_amountdForRedeemable2);
		utils.logPass(actualdiscount_amountForRedeemable2
				+ " actual discount_amount is matched with expected discount_amount "
				+ expdiscount_amountdForRedeemable2);

		// Check item quantity for first qualified item of redeemable2
		double actualItem_qty0_secondRedeemable = discountLookupResponse0.jsonPath()
				.getDouble("selected_discounts[1].qualified_items[0].item_qty");
		double expItem_qty0_secondRedeemable = Double.parseDouble(dataSet.get("expItem_qty0_secondRedeemable"));
		Assert.assertEquals(actualItem_qty0_secondRedeemable, expItem_qty0_secondRedeemable,
				actualItem_qty0_secondRedeemable + " actual item qty did not matched with expected item qty"
						+ expItem_qty0_secondRedeemable);
		utils.logPass(actualItem_qty0_secondRedeemable
				+ " actual item qty is matched with expected item qty" + expItem_qty0_secondRedeemable);

		// Check qualified amount for first qualified redeemable2
		double actualAmount0_secondRedeemable = discountLookupResponse0.jsonPath()
				.getDouble("selected_discounts[1].qualified_items[0].amount");
		double expAmount0_secondRedeemable = Double.parseDouble(dataSet.get("expAmount0_secondRedeemable"));
		Assert.assertEquals(actualAmount0_secondRedeemable, expAmount0_secondRedeemable, actualAmount0_secondRedeemable
				+ " actual item amount did not matched with expected item amount " + expAmount0_secondRedeemable);
		utils.logPass(actualAmount0_secondRedeemable
				+ " actual item amount is matched with expected item amount" + expAmount0_secondRedeemable);

		// check item quantity for second qualified item of redeemable2
		double actualItem_qty1_secondRedeemable = discountLookupResponse0.jsonPath()
				.getDouble("selected_discounts[1].qualified_items[1].item_qty");
		double expItem_qty1_secondRedeemable = Double.parseDouble(dataSet.get("expItem_qty1_secondRedeemable"));
		Assert.assertEquals(actualItem_qty1_secondRedeemable, expItem_qty1_secondRedeemable,
				actualItem_qty1_secondRedeemable + " actual item qty did not matched with expected item qty"
						+ expItem_qty1_secondRedeemable);
		utils.logPass(actualItem_qty1_secondRedeemable
				+ " actual item qty is matched with expected item qty" + expItem_qty1_secondRedeemable);

		// Check qualified amount for second qualified redeemable2
		String actualAmount1_secondRedeemable = discountLookupResponse0.jsonPath()
				.getString("selected_discounts[1].qualified_items[1].amount");
		String expAmount1_secondRedeemable = dataSet.get("expAmount1_secondRedeemable");
		Assert.assertEquals(actualAmount1_secondRedeemable, expAmount1_secondRedeemable, actualAmount1_secondRedeemable
				+ " actual item amount did not matched with expected item amount" + expAmount1_secondRedeemable);
		utils.logPass(actualAmount1_secondRedeemable
				+ " actual item amount is matched with expected item amount " + expAmount1_secondRedeemable);

	}

	@Test(description = "SQ-T6389 [Stacking ON, Reusability ON]Verify the discount calculation for Offer 1 and Offer 2 with amount capping")
	@Owner(name = "Shashank Sharma")
	public void T6389_ValidateDiscountCalculationBasedOnQCWithAmountCap() throws InterruptedException {
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

		// send reward amount to user Reedemable1
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "50",
				dataSet.get("redeemable1_id"), "", "");
		utils.logit(dataSet.get("redeemable1_name") + " Send redeemable to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		// send reward amount to user Reedemable2
		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "50",
				dataSet.get("redeemable2_id"), "", "");
		utils.logit(dataSet.get("redeemable2_name") + " Send redeemable to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse2.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		String redeemableID1 = dataSet.get("redeemable1_id");

		// get reward id
		String rewardId1 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemableID1);

		utils.logit("Reward id " + rewardId1 + " is generated successfully ");

		String externalUID1 = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		// AUTH Add Discount to Basket
		Response discountBasketResponse1 = pageObj.endpoints().addDiscountToBasketAUTH(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId1, externalUID1);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponse1.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		utils.logPass("AUTH add discount to basket is successful");

		String redeemableID2 = dataSet.get("redeemable2_id");

		// get reward id of redeemable2
		String rewardId2 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemableID2);

		utils.logit("Reward id " + rewardId2 + " is generated successfully ");

		String externalUID2 = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		// AUTH Add Discount to Basket
		Response discountBasketResponse2 = pageObj.endpoints().addDiscountToBasketAUTH(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId2, externalUID2);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponse2.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		utils.logPass("AUTH add discount to basket is successful");

		// Add multiple items dynamically
		List<Map<String, Object>> lineItems = new ArrayList<>();
		Map<String, Object> receiptItems = new HashMap<String, Object>();
		Map<String, Object> receiptItems2 = new HashMap<String, Object>();

		// Sandwich|2|10.4|M|101|1001|23|1.0

		receiptItems = ApiPayloads.getInputForReceiptItems("Avocado", Integer.parseInt(dataSet.get("item_qty")),
				Double.parseDouble(dataSet.get("amount")), dataSet.get("item_type"), dataSet.get("item_id"),
				dataSet.get("item_family"), dataSet.get("item_group"), dataSet.get("serial_number"));
		lineItems.add(receiptItems);

		// Sandwich|2|18.9|M|201|1001|23|2.0
		receiptItems2 = ApiPayloads.getInputForReceiptItems(dataSet.get("item_name"),
				Integer.parseInt(dataSet.get("item_qty")), Double.parseDouble(dataSet.get("amount2")),
				dataSet.get("item_type"), dataSet.get("item_id2"), dataSet.get("item_family"),
				dataSet.get("item_group"), dataSet.get("serial_number"));
		lineItems.add(receiptItems2);

		String receipt_datetime = CreateDateTime.getCurrentDateTimeInUtc();
		String punchh_key = Integer.toString(Utilities.getRandomNoFromRange(100000000, 500000000));
		String transaction_no = Integer.toString(Utilities.getRandomNoFromRange(500000000, 900000000));

		// POS Discount Lookup Api
		Response discountLookupResponse0 = pageObj.endpoints().posDiscountLookupAPIInputPayload(lineItems,
				receipt_datetime, Double.parseDouble(dataSet.get("subtotal_amount")),
				Double.parseDouble(dataSet.get("receipt_amount")), punchh_key, transaction_no, userID,
				dataSet.get("locationKeyNew"), externalUID1);

		Assert.assertEquals(discountLookupResponse0.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Discount Lookup Api ");
		// check discount ammount for redeemable 1
		int actualItemIdForRedeemable1 = discountLookupResponse0.jsonPath()
				.getInt("selected_discounts[0].discount_details.item_id");
		int expItemIdForRedeemable1 = Integer.parseInt(dataSet.get("redeemable1_id"));
		Assert.assertEquals(actualItemIdForRedeemable1, expItemIdForRedeemable1, actualItemIdForRedeemable1
				+ " actual item did not matched with expected itemid " + dataSet.get("redeemable1_id"));
		utils.logPass(actualItemIdForRedeemable1 + " actual item is matched with expected itemid "
				+ dataSet.get("redeemable1_id"));

		// Check redeemable1 in response
		String actualRedeemableName1 = discountLookupResponse0.jsonPath()
				.getString("selected_discounts[0].discount_details.name");
		Assert.assertEquals(actualRedeemableName1, dataSet.get("redeemable1_name"), actualRedeemableName1
				+ " actual redeemable name did not matched with expected itemid " + dataSet.get("redeemable1_name"));
		utils.logPass(actualRedeemableName1 + " actual redeemable name is matched with expected redeemable name "
				+ dataSet.get("redeemable1_name"));

		// Check discount ammount for redeemable1
		double actualdiscount_amountForRedeemable1 = discountLookupResponse0.jsonPath()
				.getDouble("selected_discounts[0].discount_amount");
		double expdiscount_amountdForRedeemable1 = Double.parseDouble(dataSet.get("expactualdiscount_amount1"));
		Assert.assertEquals(actualdiscount_amountForRedeemable1, expdiscount_amountdForRedeemable1,
				actualdiscount_amountForRedeemable1
						+ " actual discount_amount did not matched with expected discount_amount "
						+ expdiscount_amountdForRedeemable1);
		utils.logPass(actualdiscount_amountForRedeemable1
				+ " actual discount_amount is matched with expected discount_amount "
				+ expdiscount_amountdForRedeemable1);

		// Check item quantity for first qualified item
		double actualItem_qty0_firstRedeemable = discountLookupResponse0.jsonPath()
				.getDouble("selected_discounts[0].qualified_items[0].item_qty");
		double expItem_qty0_firstRedeemable = Double.parseDouble(dataSet.get("expItem_qty0_firstRedeemable"));
		Assert.assertEquals(actualItem_qty0_firstRedeemable, expItem_qty0_firstRedeemable,
				actualItem_qty0_firstRedeemable + " actual item qty did not matched with expected item qty"
						+ expItem_qty0_firstRedeemable);
		utils.logPass(actualItem_qty0_firstRedeemable
				+ " actual item qty is matched with expected item qty" + expItem_qty0_firstRedeemable);

		// Check qualified amount for first qualified redeemable
		double actualAmount0_firstRedeemable = discountLookupResponse0.jsonPath()
				.getDouble("selected_discounts[0].qualified_items[0].amount");
		double expAmount0_firstRedeemable = Double.parseDouble(dataSet.get("expAmount0_firstRedeemable"));
		Assert.assertEquals(actualAmount0_firstRedeemable, expAmount0_firstRedeemable, actualAmount0_firstRedeemable
				+ " actual item amount did not matched with expected item amount " + expAmount0_firstRedeemable);
		utils.logPass(actualAmount0_firstRedeemable
				+ " actual item amount is matched with expected item qty" + expAmount0_firstRedeemable);

		// check item quantity for second qualified item
		double actualItem_qty1_firstRedeemable = discountLookupResponse0.jsonPath()
				.getDouble("selected_discounts[0].qualified_items[1].item_qty");
		double expItem_qty1_firstRedeemable = Double.parseDouble(dataSet.get("expItem_qty1_firstRedeemable"));
		Assert.assertEquals(actualItem_qty1_firstRedeemable, expItem_qty1_firstRedeemable,
				actualItem_qty1_firstRedeemable + " actual item qty did not matched with expected item qty"
						+ expItem_qty1_firstRedeemable);
		utils.logPass(actualItem_qty1_firstRedeemable
				+ " actual item qty is matched with expected item qty" + expItem_qty1_firstRedeemable);

		// Check qualified amount for second qualified redeemable
		String actualAmount1_firstRedeemable = discountLookupResponse0.jsonPath()
				.getString("selected_discounts[0].qualified_items[1].amount");
		String expAmount1_firstRedeemable = dataSet.get("expAmount1_firstRedeemable");
		Assert.assertEquals(actualAmount1_firstRedeemable, expAmount1_firstRedeemable, actualAmount1_firstRedeemable
				+ " actual item amount did not matched with expected item amount" + expAmount1_firstRedeemable);
		utils.logPass(actualAmount1_firstRedeemable
				+ " actual item amount is matched with expected item amount " + expAmount1_firstRedeemable);

		// Check item id for redeemable 2
		int actualItemIdForRedeemable2 = discountLookupResponse0.jsonPath()
				.getInt("selected_discounts[1].discount_details.item_id");
		int expItemIdForRedeemable2 = Integer.parseInt(dataSet.get("redeemable2_id"));
		Assert.assertEquals(actualItemIdForRedeemable2, expItemIdForRedeemable2, actualItemIdForRedeemable2
				+ " actual item did not matched with expected itemid " + dataSet.get("redeemable2_id"));
		utils.logPass(actualItemIdForRedeemable2 + " actual item is matched with expected itemid "
				+ dataSet.get("redeemable2_id"));

		// Check redeemable2 name in response
		String actualRedeemableName2 = discountLookupResponse0.jsonPath()
				.getString("selected_discounts[1].discount_details.name");
		Assert.assertEquals(actualRedeemableName2, dataSet.get("redeemable2_name"), actualRedeemableName2
				+ " actual redeemable name did not matched with expected itemid " + dataSet.get("redeemable2_name"));
		utils.logPass(actualRedeemableName2 + " actual redeemable name is matched with expected redeemable name "
				+ dataSet.get("redeemable2_name"));

		// check discount amount for redeemable2
		double actualdiscount_amountForRedeemable2 = discountLookupResponse0.jsonPath()
				.getDouble("selected_discounts[1].discount_amount");
		double expdiscount_amountdForRedeemable2 = Double.parseDouble(dataSet.get("expactualdiscount_amount2"));
		Assert.assertEquals(actualdiscount_amountForRedeemable2, expdiscount_amountdForRedeemable2,
				actualdiscount_amountForRedeemable2
						+ " actual discount_amount did not matched with expected discount_amount "
						+ expdiscount_amountdForRedeemable2);
		utils.logPass(actualdiscount_amountForRedeemable2
				+ " actual discount_amount is matched with expected discount_amount "
				+ expdiscount_amountdForRedeemable2);

		// Check item quantity for first qualified item of redeemable2
		double actualItem_qty0_secondRedeemable = discountLookupResponse0.jsonPath()
				.getDouble("selected_discounts[1].qualified_items[0].item_qty");
		double expItem_qty0_secondRedeemable = Double.parseDouble(dataSet.get("expItem_qty0_secondRedeemable"));
		Assert.assertEquals(actualItem_qty0_secondRedeemable, expItem_qty0_secondRedeemable,
				actualItem_qty0_secondRedeemable + " actual item qty did not matched with expected item qty"
						+ expItem_qty0_secondRedeemable);
		utils.logPass(actualItem_qty0_secondRedeemable
				+ " actual item qty is matched with expected item qty" + expItem_qty0_secondRedeemable);

		// Check qualified amount for first qualified redeemable2
		double actualAmount0_secondRedeemable = discountLookupResponse0.jsonPath()
				.getDouble("selected_discounts[1].qualified_items[0].amount");
		double expAmount0_secondRedeemable = Double.parseDouble(dataSet.get("expAmount0_secondRedeemable"));
		Assert.assertEquals(actualAmount0_secondRedeemable, expAmount0_secondRedeemable, actualAmount0_secondRedeemable
				+ " actual item amount did not matched with expected item amount " + expAmount0_secondRedeemable);
		utils.logPass(actualAmount0_secondRedeemable
				+ " actual item amount is matched with expected item amount" + expAmount0_secondRedeemable);

		// check item quantity for second qualified item of redeemable2
		double actualItem_qty1_secondRedeemable = discountLookupResponse0.jsonPath()
				.getDouble("selected_discounts[1].qualified_items[1].item_qty");
		double expItem_qty1_secondRedeemable = Double.parseDouble(dataSet.get("expItem_qty1_secondRedeemable"));
		Assert.assertEquals(actualItem_qty1_secondRedeemable, expItem_qty1_secondRedeemable,
				actualItem_qty1_secondRedeemable + " actual item qty did not matched with expected item qty"
						+ expItem_qty1_secondRedeemable);
		utils.logPass(actualItem_qty1_secondRedeemable
				+ " actual item qty is matched with expected item qty" + expItem_qty1_secondRedeemable);

		// Check qualified amount for second qualified redeemable2
		String actualAmount1_secondRedeemable = discountLookupResponse0.jsonPath()
				.getString("selected_discounts[1].qualified_items[1].amount");
		String expAmount1_secondRedeemable = dataSet.get("expAmount1_secondRedeemable");
		Assert.assertEquals(actualAmount1_secondRedeemable, expAmount1_secondRedeemable, actualAmount1_secondRedeemable
				+ " actual item amount did not matched with expected item amount" + expAmount1_secondRedeemable);
		utils.logPass(actualAmount1_secondRedeemable
				+ " actual item amount is matched with expected item amount " + expAmount1_secondRedeemable);

	}

	@Test(description = "SQ-T6488 [Stacking ON, Reusability ON]Verify discount calculation for Offer 1 and Offer 2 with base only type LIS having processing % and amount cap")
	@Owner(name = "Shashank Sharma")
	public void T6488_ValidateDiscountCalculationBasedOnQCWithPercentageAndAmountCap() throws InterruptedException {

		String redeemableID1 = dataSet.get("redeemable1_id");
		String redeemableID2 = dataSet.get("redeemable2_id");

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

		// send reward amount to user Reedemable1
		Response sendRewardResponse1_1 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "50",
				dataSet.get("redeemable1_id"), "", "");
		utils.logit(dataSet.get("redeemable1_name") + " Send redeemable to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse1_1.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		// send reward amount to user Reedemable1
		Response sendRewardResponse1_2 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "50",
				dataSet.get("redeemable1_id"), "", "");
		utils.logit(dataSet.get("redeemable1_name") + " Send redeemable to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse1_2.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		// send reward amount to user Reedemable2
		Response sendRewardResponse2_1 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "50",
				dataSet.get("redeemable2_id"), "", "");
		utils.logit(dataSet.get("redeemable2_name") + " Send redeemable to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse2_1.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		// send reward amount to user Reedemable2
		Response sendRewardResponse2_2 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "50",
				dataSet.get("redeemable2_id"), "", "");
		utils.logit(dataSet.get("redeemable2_name") + " Send redeemable to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse2_2.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		// get reward id for both redeemables
		List<String> rewardIdListForRedeemable_1 = pageObj.redeemablesPage().getRewardIdList(token,
				dataSet.get("client"), dataSet.get("secret"), redeemableID1, 20);

		List<String> rewardIdListForRedeemable_2 = pageObj.redeemablesPage().getRewardIdList(token,
				dataSet.get("client"), dataSet.get("secret"), redeemableID2, 20);

		String externalUID1 = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));

		// AUTH Add reward1 to Basket
		Response discountBasketResponse1_1 = pageObj.endpoints().addDiscountToBasketAUTH(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardIdListForRedeemable_1.get(0), externalUID1);
		Assert.assertEquals(discountBasketResponse1_1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with add discount to basket ");
		utils.logPass("AUTH add discount to basket is successful");

		externalUID1 = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));

		// AUTH Add reward2 to Basket
		Response discountBasketResponse1_2 = pageObj.endpoints().addDiscountToBasketAUTH(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardIdListForRedeemable_1.get(1), externalUID1);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponse1_2.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		utils.logPass("AUTH add discount to basket is successful");

		externalUID1 = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		// AUTH Add reward3 to Basket
		Response discountBasketResponse2_1 = pageObj.endpoints().addDiscountToBasketAUTH(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardIdListForRedeemable_2.get(0), externalUID1);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponse2_1.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		utils.logPass("AUTH add discount to basket is successful");

		externalUID1 = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		// AUTH Add reward3 to Basket
		Response discountBasketResponse2_2 = pageObj.endpoints().addDiscountToBasketAUTH(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardIdListForRedeemable_2.get(1), externalUID1);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponse2_2.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		utils.logPass("AUTH add discount to basket is successful");

		// Add multiple items dynamically
		List<Map<String, Object>> lineItems = new ArrayList<>();
		Map<String, Object> receiptItems = new HashMap<String, Object>();
		Map<String, Object> receiptItems2 = new HashMap<String, Object>();

		// Sandwich|2|10.4|M|101|1001|23|1.0

		receiptItems = ApiPayloads.getInputForReceiptItems("Avocado", Integer.parseInt(dataSet.get("item_qty")),
				Double.parseDouble(dataSet.get("amount")), dataSet.get("item_type"), dataSet.get("item_id"),
				dataSet.get("item_family"), dataSet.get("item_group"), "1.0");
		lineItems.add(receiptItems);

		// Sandwich|2|18.9|M|201|1001|23|2.0
		receiptItems2 = ApiPayloads.getInputForReceiptItems(dataSet.get("item_name"),
				Integer.parseInt(dataSet.get("item_qty")), Double.parseDouble(dataSet.get("amount2")),
				dataSet.get("item_type"), dataSet.get("item_id2"), dataSet.get("item_family"),
				dataSet.get("item_group"), "2.0");
		lineItems.add(receiptItems2);

		String receipt_datetime = CreateDateTime.getCurrentDateTimeInUtc();
		String punchh_key = Integer.toString(Utilities.getRandomNoFromRange(100000000, 500000000));
		String transaction_no = Integer.toString(Utilities.getRandomNoFromRange(500000000, 900000000));

		// POS Discount Lookup Api
		Response discountLookupResponse0 = pageObj.endpoints().posDiscountLookupAPIInputPayload(lineItems,
				receipt_datetime, Double.parseDouble(dataSet.get("subtotal_amount")),
				Double.parseDouble(dataSet.get("receipt_amount")), punchh_key, transaction_no, userID,
				dataSet.get("locationKeyNew"), externalUID1);

		Assert.assertEquals(discountLookupResponse0.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Discount Lookup Api ");

		// check discount item for redeemable 1 index 0
		int actualItemIdForRedeemable1 = discountLookupResponse0.jsonPath()
				.getInt("selected_discounts[0].discount_details.item_id");
		int expItemIdForRedeemable1 = Integer.parseInt(dataSet.get("redeemable1_id"));
		Assert.assertEquals(actualItemIdForRedeemable1, expItemIdForRedeemable1, actualItemIdForRedeemable1
				+ " actual item did not matched with expected itemid " + dataSet.get("redeemable1_id"));
		utils.logPass(actualItemIdForRedeemable1 + " actual item is matched with expected itemid "
				+ dataSet.get("redeemable1_id"));

		// Check redeemable1 in response
		String actualRedeemableName1 = discountLookupResponse0.jsonPath()
				.getString("selected_discounts[0].discount_details.name");
		Assert.assertEquals(actualRedeemableName1, dataSet.get("redeemable1_name"), actualRedeemableName1
				+ " actual redeemable name did not matched with expected itemid " + dataSet.get("redeemable1_name"));
		utils.logPass(actualRedeemableName1 + " actual redeemable name is matched with expected redeemable name "
				+ dataSet.get("redeemable1_name"));

		// Check discount ammount for redeemable1
		double actualdiscount_amountForRedeemable1 = discountLookupResponse0.jsonPath()
				.getDouble("selected_discounts[0].discount_amount");
		double expdiscount_amountdForRedeemable1 = Double.parseDouble(dataSet.get("expactualdiscount_amount1"));
		Assert.assertEquals(actualdiscount_amountForRedeemable1, expdiscount_amountdForRedeemable1,
				actualdiscount_amountForRedeemable1
						+ " actual discount_amount did not matched with expected discount_amount "
						+ expdiscount_amountdForRedeemable1);
		utils.logPass(actualdiscount_amountForRedeemable1
				+ " actual discount_amount is matched with expected discount_amount "
				+ expdiscount_amountdForRedeemable1);

		// Check item quantity for first qualified item
		double actualItem_qty0_firstRedeemable = discountLookupResponse0.jsonPath()
				.getDouble("selected_discounts[0].qualified_items[0].item_qty");
		double expItem_qty0_firstRedeemable = Double.parseDouble(dataSet.get("expItem_qty0_firstRedeemable"));
		Assert.assertEquals(actualItem_qty0_firstRedeemable, expItem_qty0_firstRedeemable,
				actualItem_qty0_firstRedeemable + " actual item qty did not matched with expected item qty"
						+ expItem_qty0_firstRedeemable);
		utils.logPass(actualItem_qty0_firstRedeemable
				+ " actual item qty is matched with expected item qty" + expItem_qty0_firstRedeemable);

		// Check item quantity for first qualified item
		double actualItem_Amount0_firstRedeemable = discountLookupResponse0.jsonPath()
				.getDouble("selected_discounts[0].qualified_items[0].amount");
		double expItem_Amount0_firstRedeemable = Double.parseDouble(dataSet.get("expAmount0_firstRedeemable"));
		Assert.assertEquals(actualItem_Amount0_firstRedeemable, expItem_Amount0_firstRedeemable,
				actualItem_Amount0_firstRedeemable + " actual item amount did not matched with expected item qty"
						+ expItem_Amount0_firstRedeemable);
		utils.logPass(actualItem_Amount0_firstRedeemable
				+ " actual item amount matched with expected item qty" + expItem_Amount0_firstRedeemable);

		// Check item quantity for first qualified item
		double actualItem_Amount1_firstRedeemable = discountLookupResponse0.jsonPath()
				.getDouble("selected_discounts[0].qualified_items[1].amount");
		double expItem_Amount1_firstRedeemable = Double.parseDouble(dataSet.get("expAmount1_firstRedeemable"));
		Assert.assertEquals(actualItem_Amount1_firstRedeemable, expItem_Amount1_firstRedeemable,
				actualItem_Amount1_firstRedeemable + " actual item amount did not matched with expected item qty"
						+ expItem_Amount1_firstRedeemable);
		utils.logPass(actualItem_Amount1_firstRedeemable
				+ " actual item amount matched with expected item qty" + expItem_Amount1_firstRedeemable);

		// Check item quantity for first qualified item
		String actualItem_ErrorMessage_firstRedeemable = discountLookupResponse0.jsonPath()
				.getString("selected_discounts[1].message").replace("[", "").replace("]", "");
		String expItem_ErrorMessage_firstRedeemable = dataSet.get("expErrorMessage");
		Assert.assertTrue(actualItem_ErrorMessage_firstRedeemable.startsWith(expItem_ErrorMessage_firstRedeemable),
				actualItem_ErrorMessage_firstRedeemable + " actual error message did not matched with expected item qty"
						+ expItem_ErrorMessage_firstRedeemable);
		utils.logPass(actualItem_ErrorMessage_firstRedeemable
				+ " actual error message matched with expected item qty" + expItem_ErrorMessage_firstRedeemable);

		// ******** for second redeemable

		// Check item id for redeemable 2
		int actualItemIdForRedeemable2 = discountLookupResponse0.jsonPath()
				.getInt("selected_discounts[2].discount_details.item_id");
		int expItemIdForRedeemable2 = Integer.parseInt(dataSet.get("redeemable2_id"));
		Assert.assertEquals(actualItemIdForRedeemable2, expItemIdForRedeemable2, actualItemIdForRedeemable2
				+ " actual item did not matched with expected itemid " + dataSet.get("redeemable2_id"));
		utils.logPass(actualItemIdForRedeemable2 + " actual item is matched with expected itemid "
				+ dataSet.get("redeemable2_id"));

		// Check redeemable2 name in response
		String actualRedeemableName2 = discountLookupResponse0.jsonPath()
				.getString("selected_discounts[2].discount_details.name");
		Assert.assertEquals(actualRedeemableName2, dataSet.get("redeemable2_name"), actualRedeemableName2
				+ " actual redeemable name did not matched with expected itemid " + dataSet.get("redeemable2_name"));
		utils.logPass(actualRedeemableName2 + " actual redeemable name is matched with expected redeemable name "
				+ dataSet.get("redeemable2_name"));

		// check discount amount for redeemable2
		double actualdiscount_amountForRedeemable2 = discountLookupResponse0.jsonPath()
				.getDouble("selected_discounts[2].discount_amount");
		double expdiscount_amountdForRedeemable2 = Double.parseDouble(dataSet.get("expactualdiscount_amount2"));
		Assert.assertEquals(actualdiscount_amountForRedeemable2, expdiscount_amountdForRedeemable2,
				actualdiscount_amountForRedeemable2
						+ " actual discount_amount did not matched with expected discount_amount "
						+ expdiscount_amountdForRedeemable2);
		utils.logPass(actualdiscount_amountForRedeemable2
				+ " actual discount_amount is matched with expected discount_amount "
				+ expdiscount_amountdForRedeemable2);

		// Check item quantity for first qualified item of redeemable2
		double actualItem_qty0_secondRedeemable = discountLookupResponse0.jsonPath()
				.getDouble("selected_discounts[2].qualified_items[0].item_qty");
		double expItem_qty0_secondRedeemable = Double.parseDouble(dataSet.get("expItem_qty0_secondRedeemable"));
		Assert.assertEquals(actualItem_qty0_secondRedeemable, expItem_qty0_secondRedeemable,
				actualItem_qty0_secondRedeemable + " actual item qty did not matched with expected item qty"
						+ expItem_qty0_secondRedeemable);
		utils.logPass(actualItem_qty0_secondRedeemable
				+ " actual item qty is matched with expected item qty" + expItem_qty0_secondRedeemable);

		// Check qualified amount for first qualified redeemable2
		double actualAmount0_secondRedeemable = discountLookupResponse0.jsonPath()
				.getDouble("selected_discounts[2].qualified_items[0].amount");
		double expAmount0_secondRedeemable = Double.parseDouble(dataSet.get("expAmount0_secondRedeemable"));
		Assert.assertEquals(actualAmount0_secondRedeemable, expAmount0_secondRedeemable, actualAmount0_secondRedeemable
				+ " actual item amount did not matched with expected item amount " + expAmount0_secondRedeemable);
		utils.logPass(actualAmount0_secondRedeemable
				+ " actual item amount is matched with expected item amount" + expAmount0_secondRedeemable);

		// check item quantity for second qualified item of redeemable2
		double actualItem_qty1_secondRedeemable = discountLookupResponse0.jsonPath()
				.getDouble("selected_discounts[2].qualified_items[1].item_qty");
		double expItem_qty1_secondRedeemable = Double.parseDouble(dataSet.get("expItem_qty1_secondRedeemable"));
		Assert.assertEquals(actualItem_qty1_secondRedeemable, expItem_qty1_secondRedeemable,
				actualItem_qty1_secondRedeemable + " actual item qty did not matched with expected item qty"
						+ expItem_qty1_secondRedeemable);
		utils.logPass(actualItem_qty1_secondRedeemable
				+ " actual item qty is matched with expected item qty" + expItem_qty1_secondRedeemable);

		// Check qualified amount for second qualified redeemable2
		String actualAmount1_secondRedeemable = discountLookupResponse0.jsonPath()
				.getString("selected_discounts[2].qualified_items[1].amount");
		String expAmount1_secondRedeemable = dataSet.get("expAmount1_secondRedeemable");
		Assert.assertEquals(actualAmount1_secondRedeemable, expAmount1_secondRedeemable, actualAmount1_secondRedeemable
				+ " actual item amount did not matched with expected item amount" + expAmount1_secondRedeemable);
		utils.logPass(actualAmount1_secondRedeemable
				+ " actual item amount is matched with expected item amount " + expAmount1_secondRedeemable);

		// Check item quantity for first qualified item
		String actualItem_ErrorMessage_firstRedeemable1 = discountLookupResponse0.jsonPath()
				.getString("selected_discounts[3].message").replace("[", "").replace("]", "");
		String expItem_ErrorMessage_firstRedeemable1 = dataSet.get("expErrorMessage");
		Assert.assertTrue(actualItem_ErrorMessage_firstRedeemable1.startsWith(expItem_ErrorMessage_firstRedeemable1),
				actualItem_ErrorMessage_firstRedeemable1
						+ " actual error message did not matched with expected item qty"
						+ expItem_ErrorMessage_firstRedeemable1);
		utils.logPass(actualItem_ErrorMessage_firstRedeemable1
				+ " actual error message matched with expected item qty" + expItem_ErrorMessage_firstRedeemable1);

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}

}