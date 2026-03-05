package com.punchh.server.OMMTest;

import java.lang.reflect.Method;
import java.sql.SQLException;
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
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class DiscountBasketExpired_atTest {
	private static Logger logger = LogManager.getLogger(DiscountBasketExpired_atTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String sTCName;
	private String baseUrl;
	Properties prop;
	String blankString = "";
	private String env, run = "ui";
	private static Map<String, String> dataSet;
	String discount_details0;
	String externalUID;

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) throws Exception {
		prop = Utilities.loadPropertiesFile("segmentBeta.properties");
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

		externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));

	}

	@Test(description = "SQ-T3901 Point unlock redeemable>Validate that if Redeemable does not exist in active discount_basket and it has been redeemed using redemption-1.0, then discount_basket for corresponding user should be marked as expired ||"
			+ "SQ-T3902 Point unlock redeemable>Validate that if subscription does not exist in discount_basket and it has been redeemed using redemption-1.0, then discount_basket for corresponding user should be marked as expired ||"
			+ "SQ-T3900 Point based currency>Validate that if active discount_basket exists for the user and discount_amount has been redeemed using redemption-1.0, then discount_basket for corresponding user should be marked as expired ||"
			+ "SQ-T3876 POS discount lookup API>Validate that if reward end date is after redeemable end date then Reward end date gets stored in DB in UTC and same is returned in API ||"
			+ "SQ-T3883 POS Batch Redemption>Validate that if reward end date is after redeemable end date then Reward end date gets stored in DB in UTC and same is returned in API", priority = 0)
	@Owner(name = "Hardik Bhardwaj")
	public void T3901_DiscountBasketExpired_at() throws Exception {

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Settings", "Locations");
		String result = pageObj.locationPage().clickOnSelectedLocation(dataSet.get("location_name"));
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Allow Location for Multiple Redemption",
				"uncheck");
		pageObj.dashboardpage().updateCheckBox();

		// Navigate to Multiple Redemption Tab and Uncheck the flags
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_discount_locking", "check");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_auto_unlock", "check");
		pageObj.cockpitRedemptionsPage().setAutoUnlockPeriod("present", "10");
		pageObj.dashboardpage().updateCheckBox();

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Subscriptions");
		pageObj.dashboardpage().setPaymentAdapterInSubscriptionPage("No Adapter");
		pageObj.dashboardpage().clickOnUpdateButton();

		// User SignUp using API
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		System.out.println("signUpResponse -- " + signUpResponse.asPrettyString());
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertEquals(signUpResponse.jsonPath().get("user.communicable_email").toString(),
				userEmail.toLowerCase());
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();

		// subscription purchase api 2
		String PlanID = dataSet.get("PlanID");
		String spPrice = Integer.toString(Utilities.getRandomNoFromRange(300, 500));
		Response purchaseSubscriptionresponse = pageObj.endpoints().Api2SubscriptionPurchase(token, PlanID,
				dataSet.get("client"), dataSet.get("secret"), spPrice, "");
		Assert.assertEquals(purchaseSubscriptionresponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		String subscription_id = purchaseSubscriptionresponse.jsonPath().get("subscription_id").toString();
		logger.info(userEmail + " purchased " + subscription_id + " Plan id = " + PlanID);

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "50",
				dataSet.get("redeemable_id"), "", "");

		pageObj.utils().logPass("Send redeemable to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		String redeemableID = dataSet.get("redeemable_id");

		// get reward id
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemableID);

		pageObj.utils().logPass("Reward id " + rewardId + " is generated successfully ");

		String externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		// AUTH Add Discount to Basket
		Response discountBasketResponse5 = pageObj.endpoints().addDiscountToBasketAUTH(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId, externalUID);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponse5.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		pageObj.utils().logPass("AUTH add discount to basket is successful");

		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key = CreateDateTime.getTimeDateString();
		Response resp = pageObj.endpoints().posRedemptionOfSubscription(userEmail, date, subscription_id, key, txn,
				dataSet.get("locationKey1"), "8", "10");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp.getStatusCode(), "Status code 200 did not matched for pos redemption api");
		pageObj.utils().logPass("Verified that user is able to do redemption of subscription plan ");
		Thread.sleep(15000);
		String query = "Select expired_at from discount_baskets where user_id = '" + userID + "'";
		String expColValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query,
				"expired_at", 100);
		Assert.assertNotEquals(expColValue, null, "Value is not present at expired_at column in discount basket ");
		pageObj.utils().logPass("Value is present at expired_at column in discount basket ");

		String query2 = "Select status from discount_baskets where user_id = '" + userID + "'";
		String statusColValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query2,
				"status", 100);
		Assert.assertEquals(statusColValue, "4", "Value is not present at status column in discount basket ");
		pageObj.utils().logPass("Value is present at status column in discount basket ");

		// User SignUp using API
		String userEmail1 = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse1 = pageObj.endpoints().Api2SignUp(userEmail1, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse1, "API 2 user signup");
		Assert.assertEquals(signUpResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token1 = signUpResponse1.jsonPath().get("access_token.token").toString();
		String userID1 = signUpResponse1.jsonPath().get("user.user_id").toString();

		// send reward amount to user Reedemable
		Response sendRewardResponse1 = pageObj.endpoints().sendMessageToUser(userID1, dataSet.get("apiKey"), "50", "",
				"", "");
		pageObj.utils().logPass("Send redeemable to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse1.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		String externalUID1 = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		// AUTH Add Discount to Basket - add amount
		Response discountBasketResponse3 = pageObj.endpoints().addDiscountAmountToBasketAUTH(token1,
				dataSet.get("client"), dataSet.get("secret"), "discount_amount", "10", externalUID1);
		Assert.assertEquals(discountBasketResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with add discount to basket ");
		pageObj.utils().logPass("AUTH add discount to basket is successful add amount");

		// Pos redemption api
		String txn1 = "123456" + CreateDateTime.getTimeDateString();
		String date1 = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key1 = CreateDateTime.getTimeDateString();
		Response resp1 = pageObj.endpoints().posRedemptionOfRedeemable(userEmail1, date1, key1, txn1,
				dataSet.get("locationKey1"), redeemableID, dataSet.get("item_id"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp1.getStatusCode(), "Status code 200 did not matched for pos redemption api");
//		Utilities.longWait(20000);
		String query1 = "Select expired_at from discount_baskets where user_id = '" + userID1 + "'";

		String expColValue1 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query1,
				"expired_at", 100);
		Assert.assertNotEquals(expColValue1, null, "Value is not present at expired_at column in discount basket ");
		pageObj.utils().logPass("Value is present at expired_at column in discount basket ");

		String query3 = "Select status from discount_baskets where user_id = '" + userID + "'";
		String statusColValue1 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3,
				"status", 100);
		Assert.assertEquals(statusColValue1, "4", "Value is not present at status column in discount basket ");
		pageObj.utils().logPass("Value is present at status column in discount basket ");

		// User SignUp using API
		String userEmail2 = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse2 = pageObj.endpoints().Api2SignUp(userEmail2, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse2, "API 2 user signup");
		Assert.assertEquals(signUpResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token2 = signUpResponse2.jsonPath().get("access_token.token").toString();
		String userID2 = signUpResponse2.jsonPath().get("user.user_id").toString();

		// send reward amount to user Reedemable
		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUserWithEndDate(userID2, dataSet.get("apiKey"),
				"50", dataSet.get("redeemable_id"), "", "10", "2040-12-31");
		pageObj.utils().logPass("Send redeemable to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse2.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		String redeemableID2 = dataSet.get("redeemable_id");
		// get reward id
		String rewardId2 = pageObj.redeemablesPage().getRewardId(token2, dataSet.get("client"), dataSet.get("secret"),
				redeemableID2);
		pageObj.utils().logPass("Reward id " + rewardId2 + " is generated successfully ");

		String externalUID2 = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		// AUTH Add Discount to Basket
		Response discountBasketResponse = pageObj.endpoints().addDiscountToBasketAUTH(token2, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId2, externalUID2);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponse.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		pageObj.utils().logPass("AUTH add discount to basket is successful");

//		SQ-T3876 POS discount lookup API>Validate that if reward end date is after redeemable end date then Reward end date gets stored in DB in UTC and same is returned in API

		// POS Discount Lookup Api
		Response discountLookupResponse = pageObj.endpoints().discountLookUpPosApi(dataSet.get("locationkey0"), userID2,
				dataSet.get("item_id"), "30", externalUID2);
		Assert.assertEquals(discountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Discount Lookup Api ");
		String startTime = discountLookupResponse.jsonPath().get("selected_discounts[0].discount_details.start_date_tz")
				.toString();
		String formatedStartTime = startTime.replace("T", " ").replace("Z", "");
		String endTime = discountLookupResponse.jsonPath().get("selected_discounts[0].discount_details.end_date_tz")
				.toString();
		String formatedEndTime = endTime.replace("T", " ").replace("Z", "");
//		Utilities.longWait(20000);

		String query6 = "Select start_time from rewards where id=  '" + rewardId2 + "'";
		String startTimeDB = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query6,
				"start_time", 100);
		Assert.assertEquals(startTimeDB, formatedStartTime,
				"Start time of discount LookUp Pos Api is not matching with rewards table DB start time");
		pageObj.utils().logPass("Start time of discount LookUp Pos Api is matching with rewards table DB start time");

		String query7 = "Select end_time from rewards where id=  '" + rewardId2 + "'";
		String endTimeDB = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query7,
				"end_time", 100);
		Assert.assertEquals(endTimeDB, formatedEndTime,
				"End time of discount LookUp Pos Api is not matching with rewards table DB end time");
		pageObj.utils().logPass("End time of discount LookUp Pos Api is matching with rewards table DB end time");

		// POS fetch active basket
		Response basketDiscountDetailsResponse3 = pageObj.endpoints().fetchActiveBasketPOSAPI(userID2,
				dataSet.get("locationkey0"), externalUID2);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, basketDiscountDetailsResponse3.getStatusCode(),
				"Status code 200 did not matched for POS fetch active basket");
		TestListeners.extentTest.get().pass("POS fetch active basket is successful");
		String startTime1 = basketDiscountDetailsResponse3.jsonPath()
				.get("discount_basket_items[0].discount_details.start_date_tz").toString();
		String formatedStartTime1 = startTime1.replace("T", " ").replace("Z", "");
		String endTime1 = basketDiscountDetailsResponse3.jsonPath()
				.get("discount_basket_items[0].discount_details.end_date_tz").toString();
		String formatedEndTime1 = endTime1.replace("T", " ").replace("Z", "");

		Assert.assertEquals(startTimeDB, formatedStartTime1,
				"Start time of fetch Active Basket Pos Api is not matching with rewards table DB start time");
		pageObj.utils().logPass("Start time of fetch Active Basket Pos Pos Api is matching with rewards table DB start time");

		Assert.assertEquals(endTimeDB, formatedEndTime1,
				"End time of fetch Active Basket Pos Api is not matching with rewards table DB end time");
		pageObj.utils().logPass("End time of fetch Active Basket Pos Api is matching with rewards table DB end time");

//		SQ-T3883 POS Batch Redemption>Validate that if reward end date is after redeemable end date then Reward end date gets stored in DB in UTC and same is returned in API

		// POS fetch active basket
		Response batchRedemptionResponse = pageObj.endpoints().posBatchRedemptionWithQueryTrue(
				dataSet.get("locationkey0"), userID2, dataSet.get("item_id"), externalUID2);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, batchRedemptionResponse.getStatusCode(),
				"Status code 200 did not matched for POS fetch active basket");
		TestListeners.extentTest.get().pass("POS fetch active basket is successful");
		String startTime2 = batchRedemptionResponse.jsonPath().get("success[0].discount_details.start_date_tz")
				.toString();
		String formatedStartTime2 = startTime2.replace("T", " ").replace("Z", "");
		String endTime2 = batchRedemptionResponse.jsonPath().get("success[0].discount_details.end_date_tz").toString();
		String formatedEndTime2 = endTime2.replace("T", " ").replace("Z", "");

		Assert.assertEquals(startTimeDB, formatedStartTime2,
				"Start time of fetch Active Basket Pos Api is not matching with rewards table DB start time");
		pageObj.utils().logPass("Start time of fetch Active Basket Pos Pos Api is matching with rewards table DB start time");

		Assert.assertEquals(endTimeDB, formatedEndTime2,
				"End time of fetch Active Basket Pos Api is not matching with rewards table DB end time");
		pageObj.utils().logPass("End time of fetch Active Basket Pos Api is matching with rewards table DB end time");

		// POS redemption API
		String txn2 = "123456" + CreateDateTime.getTimeDateString();
		String date2 = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key2 = CreateDateTime.getTimeDateString();
		Response respo = pageObj.endpoints().posRedemptionOfAmount(userEmail2, date2, key2, txn2,
				dataSet.get("redeemAmount"), dataSet.get("locationKey1"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, respo.getStatusCode(), "Status code 200 did not matched for pos redemption api");
		String query4 = "Select expired_at from discount_baskets where user_id = '" + userID1 + "'";
		String expColValue3 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4,
				"expired_at", 100);
		Assert.assertNotEquals(expColValue3, null, "Value is not present at expired_at column in discount basket ");
		pageObj.utils().logPass("Value is present at expired_at column in discount basket ");

		String query5 = "Select status from discount_baskets where user_id = '" + userID + "'";
		String statusColValue3 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query5,
				"status", 100);
		Assert.assertEquals(statusColValue3, "4", "Value is not present at status column in discount basket ");
		pageObj.utils().logPass("Value is present at status column in discount basket ");
//		DBUtils.closeConnection();
	}

	@Test(description = "SQ-T3185 Visit based>Validate that if card_completion does not exist in discount_basket and it has been redeemed using redemption-1.0, then discount_basket for corresponding user should be marked as expired ||"
			+ "SQ-T3180 Verify that if card_completion is not added in basket then Discount_details-NULL gets displayed in unselected discounts ||"
			+ "SQ-T3897 Verify that if Card_completion-10 is less than Receipt amount-60 added in basket then discount amount-10 gets displayed In selected discounts", priority = 1)
	@Owner(name = "Hardik Bhardwaj")
	public void T3185_cardCompletionDoesNotExistInDiscount_basket() throws Exception {

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Settings", "Locations");
		String result = pageObj.locationPage().clickOnSelectedLocation(dataSet.get("locationName1"));
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Allow Location for Multiple Redemption",
				"uncheck");
		pageObj.dashboardpage().updateCheckBox();

		// Navigate to Multiple Redemption Tab and Uncheck the flags
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
//		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_discount_locking", "check");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_auto_unlock", "uncheck");
		pageObj.dashboardpage().updateCheckBox();
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Validations");
		pageObj.cockpitRedemptionsPage().updateMaxRedemptionAmount("10");
		pageObj.dashboardpage().updateCheckBox();

		// User SignUp using API
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertEquals(signUpResponse.jsonPath().get("user.communicable_email").toString(),
				userEmail.toLowerCase());
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "60",
				dataSet.get("redeemable_id"), "", "10");
		pageObj.utils().logPass("Send redeemable to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		String redeemableID = dataSet.get("redeemable_id");

		// get reward id
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemableID);

		pageObj.utils().logPass("Reward id " + rewardId + " is generated successfully ");

		String externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		// AUTH Add Discount to Basket
		Response discountBasketResponse5 = pageObj.endpoints().addDiscountToBasketAUTH(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId, externalUID);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponse5.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		pageObj.utils().logPass("AUTH add discount to basket is successful");

		// POS Discount Lookup Api
		List<Object> obj = new ArrayList<Object>();
		Response discountLookupResponse0 = pageObj.endpoints().discountLookUpPosApi(dataSet.get("locationKeyRed2.0"),
				userID, dataSet.get("item_id"), "30", externalUID);
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
		pageObj.utils().logPass("POS Discount Lookup Api gives discount details as NULL");

		// POS redemption api
		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key = CreateDateTime.getTimeDateString();
		Response respo = pageObj.endpoints().posRedemptionOfCard(userEmail, date, key, txn,
				dataSet.get("locationKeyRed1.0"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, respo.getStatusCode(), "Status code 200 did not matched for pos redemption api");

		String query = "Select expired_at from discount_baskets where user_id = '" + userID + "'";
		String colValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query,
				"expired_at", 10);
		Assert.assertNotEquals(colValue, null, "Value is not present at expired_at column in discount basket ");
		pageObj.utils().logPass("Value is present at expired_at column in discount basket ");

		String query2 = "Select status from discount_baskets where user_id = '" + userID + "'";
		String statusColValue = DBUtils.executeQueryAndGetColumnValue(env, query2, "status");
		Assert.assertEquals(statusColValue, "4", "Value is not present at status column in discount basket ");
		pageObj.utils().logPass("Value is present at status column in discount basket ");

//		SQ-T3897 Verify that if Card_completion-10 is less than Receipt amount-60 added in basket then discount amount-10 gets displayed In selected discounts

		// User SignUp using API
		String userEmail3 = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse3 = pageObj.endpoints().Api2SignUp(userEmail3, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse3, "API 2 user signup");
		Assert.assertEquals(signUpResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertEquals(signUpResponse3.jsonPath().get("user.communicable_email").toString(),
				userEmail3.toLowerCase());
		String userID3 = signUpResponse3.jsonPath().get("user.user_id").toString();

		// send reward amount to user Reedemable
		Response sendRewardResponse3 = pageObj.endpoints().sendMessageToUser(userID3, dataSet.get("apiKey"), "60", "",
				"", "10");
		pageObj.utils().logPass("Send redeemable to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse3.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		// add card completion to basket
		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAddedForPOSAPIWithExt_Uid(
				dataSet.get("locationKeyRed2.0"), userID3, "card_completion", "10", externalUID);
		System.out.println("discountBasketResponse==" + discountBasketResponse.asPrettyString());
		Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		// POS Discount Lookup API
		Response discountLookupResponse = pageObj.endpoints().discountLookUpPosApi(dataSet.get("locationKeyRed2.0"),
				userID3, dataSet.get("item_id"), "60", externalUID);
		Assert.assertEquals(discountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Discount Lookup Api ");
		String discountAmount = discountLookupResponse.jsonPath().get("selected_discounts[0].discount_amount")
				.toString();
		Assert.assertEquals(discountAmount, "10.0", "Discount amount is not matching");
		pageObj.utils().logPass("In discount LookUp Pos Api Discount amount is 10 and is equal to max redemption amount");
//		DBUtils.closeConnection();
	}

	@Test(description = "SQ-T3884 POS Batch Redemption>Discount amount>Validate that if discount type->discount_amount is added into basket then no data gets displayed in discount_details under Success and failures", priority = 2)
	@Owner(name = "Hardik Bhardwaj")
	public void T3884_ValidateThatIfDiscountTypediscount_amount() throws InterruptedException {
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

		// User SignUp using API
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertEquals(signUpResponse.jsonPath().get("user.communicable_email").toString(),
				userEmail.toLowerCase());
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();

		// send reward amount to user Reedemable
		Response sendRewardResponse1 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "50", "",
				"", "");
		Assert.assertEquals(sendRewardResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 200 did not match with send Reward Response ");
		pageObj.utils().logPass("Send amount to the user successfully");

		String externalUID1 = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		// AUTH Add Discount to Basket - add amount
		Response discountBasketResponse3 = pageObj.endpoints().addDiscountAmountToBasketAUTH(token,
				dataSet.get("client"), dataSet.get("secret"), "discount_amount", "10", externalUID1);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponse3.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		pageObj.utils().logPass("AUTH add discount to basket is successful add amount");

		// Auth Process Batch Redemption
		Response batchRedemptionProcessResponse2 = pageObj.endpoints().processBatchRedemptionAUTHAPI(
				dataSet.get("client"), dataSet.get("secret"), dataSet.get("locationkey"), token, userID, "101",
				externalUID1);
		Assert.assertEquals(batchRedemptionProcessResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Process Batch Redemption ");
		String discount_details = batchRedemptionProcessResponse2.jsonPath().getString("success[0].discount_details");
		Assert.assertEquals(discount_details, null,
				"no data gets displayed in discount_details under Success and failures");
		pageObj.utils().logPass("no data gets displayed in discount_details under Success and failures");
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() throws SQLException {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}

}
