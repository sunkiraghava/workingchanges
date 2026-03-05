package com.punchh.server.OMMTest;

import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
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
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class RewardLockingFunctionalityDisableTest {
	private static Logger logger = LogManager.getLogger(RewardLockingFunctionalityDisableTest.class);
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
	String discount_details0, discount_details1, discount_details2, discount_details3 = "";
	String externalUID;
	Properties prop;

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) throws Exception {

		prop = Utilities.loadPropertiesFile("segmentBeta.properties");
		driver = new BrowserUtilities().launchBrowser();
		sTCName = method.getName();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run , env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run , env , "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==>" + dataSet);
		externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));

	}

	@Test(description = "SQ-T3292 [Batched Redemptions-OMM-T919(609)] Verify if user does not have any active discount_basket -> Discount Lookup API is functional (Reward Locking Off))", groups = { "regression", "dailyrun" }, priority = 0)
	@Owner(name = "Hardik Bhardwaj")
	public void T3292_DiscountLookUp() throws InterruptedException {

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Navigate to Multiple Redemption Tab and check the flags
		// pageObj.menupage().clickCockPitMenu();
		// pageObj.menupage().redeemptionLinkInCockpit();
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_discount_locking", "uncheck");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_auto_unlock", "check");
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

		pageObj.utils().logPass("Send redeemable to the user successfully");

		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		pageObj.utils().logPass("Api2  send reward amount to user is successful");

		// get reward id
		rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id"));

		pageObj.utils().logPass("Reward id " + rewardId + " is generated successfully ");

		// POS Discount Lookup Api-1

		Response discountLookupResponse0 = pageObj.endpoints().discountLookUpPosApi(dataSet.get("locationkey"), userID,
				"", "30", "");
		Assert.assertEquals(discountLookupResponse0.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Discount Lookup Api ");
//		String externalUidResponse2 = discountLookupResponse0.jsonPath().getString("external_uid");
//		String locked2 = discountLookupResponse0.jsonPath().getString("locked");
//		Assert.assertTrue(pageObj.cockpitRedemptionsPage().resultOfUserLookUpPosAPI1(externalUidResponse2, locked2,
//				null, null));
		String jsonObjectString = discountLookupResponse0.asString();
		JSONObject finalResponse = new JSONObject(jsonObjectString);
		Boolean unselected_discountsAvailability = finalResponse.has("unselected_discounts");
		Assert.assertTrue(unselected_discountsAvailability, "unselected_discounts not found");
		pageObj.utils().logPass("unselected_discounts found");

		pageObj.utils().logPass("unselected_discounts -> Returns the response of applicable rewards");

		// POS Discount Lookup Api-1

		Response discountLookupResponse = pageObj.endpoints().discountLookUpPosApi(dataSet.get("locationkey"), userID,
				dataSet.get("item_id"), "30", "");
		Assert.assertEquals(discountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Discount Lookup Api ");
//		String externalUidResponse = discountLookupResponse.jsonPath().getString("external_uid");
//		String locked = discountLookupResponse.jsonPath().getString("locked");
//		Assert.assertTrue(
//				pageObj.cockpitRedemptionsPage().resultOfUserLookUpPosAPI1(externalUidResponse, locked, null,null));
		String jsonObjectString1 = discountLookupResponse.asString();
		JSONObject finalResponse1 = new JSONObject(jsonObjectString1);
		Boolean selected_discountsAvailability = finalResponse1.has("selected_discounts");
		Assert.assertTrue(selected_discountsAvailability, "selected_discounts not found");
		pageObj.utils().logPass("shows selected response");

		// POS Discount Lookup Api-1

		List<Object> obj2 = new ArrayList<Object>();
		Response discountLookupResponse1 = pageObj.endpoints().discountLookUpPosApi(dataSet.get("locationkey"), userID,
				dataSet.get("item_id"), "30", "");
		Assert.assertEquals(discountLookupResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Discount Lookup Api ");
//		String externalUidResponse1 = discountLookupResponse1.jsonPath().getString("external_uid");
//		String locked1 = discountLookupResponse1.jsonPath().getString("locked");
//		Assert.assertTrue(pageObj.cockpitRedemptionsPage().resultOfUserLookUpPosAPI1(externalUidResponse1, locked1,
//				null,null));
		obj2 = discountLookupResponse1.jsonPath().getList("unselected_discounts");
		for (int i = 0; i < obj2.size(); i++) {
			discount_details2 = discountLookupResponse1.jsonPath()
					.getString("unselected_discounts[" + i + "].discount_details.name");

			if (discount_details2.equalsIgnoreCase("Automation 12003")) {
				break;
			}
		}
		Assert.assertEquals(discount_details2, "Automation 12003",
				"unselected_discount -> not Returns the response of applicable rewards / coupons");
		pageObj.utils().logPass("unselected_discounts -> Returns the response of applicable rewards / coupons");

		// POS Discount Lookup Api-1

		Response discountLookupResponse2 = pageObj.endpoints().discountLookUpPosApi(dataSet.get("locationkey"), userID,
				"1234", "30", "");
		Assert.assertEquals(discountLookupResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Discount Lookup Api ");
//		String externalUidResponse3 = discountLookupResponse2.jsonPath().getString("external_uid");
//		String locked3 = discountLookupResponse2.jsonPath().getString("locked");
//		Assert.assertTrue(pageObj.cockpitRedemptionsPage().resultOfUserLookUpPosAPI1(externalUidResponse3, locked3,
//				null, null));
		String jsonObjectString4 = discountLookupResponse2.asString();
		JSONObject finalResponse4 = new JSONObject(jsonObjectString4);
		Boolean unselected_discountsAvailability4 = finalResponse4.has("unselected_discounts");
		Assert.assertTrue(unselected_discountsAvailability4, "unselected_discounts not found");
		pageObj.utils().logPass("shows unselected response");

	}

	@SuppressWarnings("static-access")
	@Test(description = "SQ-T3294 [Batched Redemptions-OMM-T921(609)] Verify if user does not have any active discount_basket and has available rewards -> pos/discounts/select API is functional and discount_basket gets created (Reward Locking Off)", groups = { "regression", "dailyrun" }, priority = 1)
	@Owner(name = "Hardik Bhardwaj")
	public void T3294_POSDiscountSelectBasket() throws Exception {

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

		pageObj.utils().logPass("Send redeemable to the user successfully");

		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		pageObj.utils().logPass("Api2  send reward amount to user is successful");

		// get reward id
		rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id"));

		pageObj.utils().logPass("Reward id " + rewardId + " is generated successfully ");

		// POS Add Discount to Basket
		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAddedForPOSAPIWithExt_Uid(
				dataSet.get("locationkey"), userID, "reward", rewardId, externalUID);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponse.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		pageObj.utils().logPass("POS add discount to basket is successful");

//		// AUTH Add Discount to Basket
//		Response discountBasketResponse = pageObj.endpoints().addDiscountToBasketAUTH(token, dataSet.get("client"),
//				dataSet.get("secret"), "reward", rewardId, externalUID);
//		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponse.getStatusCode(),
//				"Status code 200 did not match with add discount to basket ");
//		pageObj.utils().logPass("AUTH add discount to basket is successful");

		// https://punchhdev.atlassian.net/browse/OMM-1227 - functionality deprecated -
		// confirmed by Rahul Garg

//		String query = "Select external_uid from discount_baskets where user_id ='" + userID + "'";
//		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "external_uid");
//		Assert.assertEquals(expColValue, externalUID,
//				"Value is not present at external_uid column in discount basket ");
//		logger.info("Value is present at external_uid column in discount basket ");
//		pageObj.utils().logPass("Value is present at external_uid column in discount basket ");

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
		Response sendRewardResponse1 = pageObj.endpoints().sendMessageToUser(userID1, dataSet.get("apiKey"), "16",
				dataSet.get("redeemable_id"), "", "");

		pageObj.utils().logPass("Send redeemable to the user successfully");

		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse1.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		pageObj.utils().logPass("Api2  send reward amount to user is successful");

		// get reward id
		rewardId1 = pageObj.redeemablesPage().getRewardId(token1, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id"));

		pageObj.utils().logPass("Reward id " + rewardId1 + " is generated successfully ");

		// POS Add Discount to Basket
		Response discountBasketResponse1 = pageObj.endpoints()
				.authListDiscountBasketAddedForPOSAPI(dataSet.get("locationkey"), userID1, "reward", rewardId1);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponse1.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		pageObj.utils().logPass("POS add discount to basket is successful");

		String query1 = "Select external_uid from discount_baskets where user_id ='" + userID1 + "'";
		String expColValue1 = DBUtils.executeQueryAndGetColumnValue(env, query1, "external_uid");
		Assert.assertEquals(expColValue1, null, "null is not present at external_uid column in discount basket ");
		pageObj.utils().logPass("null is present at external_uid column in discount basket ");

	}

	@SuppressWarnings("static-access")
	@Test(description = "SQ-T3295 [Batched Redemptions-OMM-T922(609)] Verify if user does not have any active discount_basket and has available rewards -> auth/discounts/select  API is functional and discount_basket gets created (Reward Locking Off)", groups = { "regression", "dailyrun" }, priority = 2)
	@Owner(name = "Hardik Bhardwaj")
	public void T3295_AuthDiscountSelectBasket() throws Exception {

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

		pageObj.utils().logPass("Send redeemable to the user successfully");

		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		pageObj.utils().logPass("Api2  send reward amount to user is successful");

		// get reward id
		rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id"));

		pageObj.utils().logPass("Reward id " + rewardId + " is generated successfully ");

		// AUTH Add Discount to Basket
		Response discountBasketResponse = pageObj.endpoints().addDiscountToBasketAUTH(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId, externalUID);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponse.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		pageObj.utils().logPass("AUTH add discount to basket is successful");

		// https://punchhdev.atlassian.net/browse/OMM-1227 - functionality deprecated -
		// confirmed by Rahul Garg

//		String query = "Select external_uid from discount_baskets where user_id ='" + userID + "'";
//		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "external_uid");
//		Assert.assertEquals(expColValue, externalUID,
//				"Value is not present at external_uid column in discount basket ");
//		logger.info("Value is present at external_uid column in discount basket ");
//		pageObj.utils().logPass("Value is present at external_uid column in discount basket ");

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
		Response sendRewardResponse1 = pageObj.endpoints().sendMessageToUser(userID1, dataSet.get("apiKey"), "16",
				dataSet.get("redeemable_id"), "", "");

		pageObj.utils().logPass("Send redeemable to the user successfully");

		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse1.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		pageObj.utils().logPass("Api2  send reward amount to user is successful");

		// get reward id
		rewardId1 = pageObj.redeemablesPage().getRewardId(token1, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id"));

		pageObj.utils().logPass("Reward id " + rewardId1 + " is generated successfully ");

		// AUTH Add Discount to Basket
		Response discountBasketResponse1 = pageObj.endpoints().authListDiscountBasketAddedAUTH(token1,
				dataSet.get("client"), dataSet.get("secret"), "reward", rewardId1);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponse1.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		pageObj.utils().logPass("AUTH add discount to basket is successful");

		String query1 = "Select external_uid from discount_baskets where user_id ='" + userID1 + "'";
		String expColValue1 = DBUtils.executeQueryAndGetColumnValue(env, query1, "external_uid");
		Assert.assertEquals(expColValue1, null, "null is not present at external_uid column in discount basket ");
		pageObj.utils().logPass("null is present at external_uid column in discount basket ");

	}

	@SuppressWarnings("static-access")
	@Test(description = "SQ-T3296 [Batched Redemptions-OMM-T923(609)] Verify if user does not have any active discount_basket and has available rewards -> Auto Select API is functional and discount_basket gets created (Reward Locking On/Off)", groups = { "regression", "dailyrun" }, priority = 2)
	@Owner(name = "Hardik Bhardwaj")
	public void T3296_AutoSelectApiStep1_2() throws Exception {

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("API2 Signup is successful");

		String query = "DELETE FROM rewards WHERE user_id = '" + userID + "'";
		int rs = DBUtils.executeUpdateQuery(env, query);
		Assert.assertEquals(rs, 0);

		// POS Auto Unlock
		Response autoUnlockResponse1 = pageObj.endpoints().autoSelectPosApi(userID, "30", "1", "12003", externalUID,
				dataSet.get("locationkey"));
		Assert.assertEquals(autoUnlockResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Auto Unlock ");
		pageObj.utils().logPass("POS Auto Unlock Api is successful");

		// https://punchhdev.atlassian.net/browse/OMM-1227 - functionality deprecated -
		// confirmed by Rahul Garg

//		String query1 = "Select external_uid from discount_baskets where user_id ='" + userID + "'";
//		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query1, "external_uid");
//		Assert.assertEquals(expColValue, externalUID,
//				"Value is not present at external_uid column in discount basket ");
//		logger.info("external_uid sent -> Empty discount_basket gets created and external_uid is saved in DB ");
//		TestListeners.extentTest.get()
//				.pass("external_uid sent -> Empty discount_basket gets created and external_uid is saved in DB ");

		// User SignUp using API
		String userEmail1 = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse1 = pageObj.endpoints().Api2SignUp(userEmail1, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse1, "API 2 user signup");
		String userID1 = signUpResponse1.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("API2 Signup is successful");

		String query2 = "DELETE FROM rewards WHERE user_id = '" + userID1 + "'";
		int rs2 = DBUtils.executeUpdateQuery(env, query2);
		Assert.assertEquals(rs2, 0);

		// POS Auto Unlock
		Response autoUnlockResponse = pageObj.endpoints().autoSelectPosApi(userID1, "30", "1", "12003", "",
				dataSet.get("locationkey"));
		Assert.assertEquals(autoUnlockResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with Auto Unlock ");
		pageObj.utils().logPass("POS Auto Select Api is successful");

		String query3 = "Select external_uid from discount_baskets where user_id ='" + userID1 + "'";
		String expColValue3 = DBUtils.executeQueryAndGetColumnValue(env, query3, "external_uid");
		Assert.assertEquals(expColValue3, null, "Value is not present at external_uid column in discount basket ");
		pageObj.utils().logPass(
				"external_uid not sent -> Empty discount_basket gets created and external_uid remains NULL in DB ");

		// User SignUp using API
		String userEmail2 = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse2 = pageObj.endpoints().Api2SignUp(userEmail2, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse2, "API 2 user signup");
		String token2 = signUpResponse2.jsonPath().get("access_token.token").toString();

		String userID2 = signUpResponse2.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("API2 Signup is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID2, dataSet.get("apiKey"), "16",
				dataSet.get("redeemable_id"), "", "");

		pageObj.utils().logPass("Send redeemable to the user successfully");

		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		pageObj.utils().logPass("Api2  send reward amount to user is successful");

		// get reward id
		rewardId = pageObj.redeemablesPage().getRewardId(token2, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id"));

		pageObj.utils().logPass("Reward id " + rewardId + " is generated successfully ");

		// POS Auto Unlock
		Response autoUnlockResponse3 = pageObj.endpoints().autoSelectPosApi(userID2, "30", "1", "12003", externalUID,
				dataSet.get("locationkey"));
		Assert.assertEquals(autoUnlockResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Auto Unlock ");
		pageObj.utils().logPass("POS Auto Select Api is successful");

		// https://punchhdev.atlassian.net/browse/OMM-1227 - functionality deprecated -
		// confirmed by Rahul Garg

//		String query4 = "Select external_uid from discount_baskets where user_id ='" + userID2 + "'";
//		String expColValue4 = DBUtils.executeQueryAndGetColumnValue(env, query4, "external_uid");
//		Assert.assertEquals(expColValue4, externalUID,
//				"Value is not present at external_uid column in discount basket ");
//		logger.info("external_uid not sent -> Empty discount_basket gets created and external_uid remains NULL in DB ");
//		pageObj.utils().logPass(
//				"external_uid not sent -> Empty discount_basket gets created and external_uid remains NULL in DB ");

		// User SignUp using API
		String userEmail3 = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse3 = pageObj.endpoints().Api2SignUp(userEmail3, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse3, "API 2 user signup");
		String token3 = signUpResponse3.jsonPath().get("access_token.token").toString();

		String userID3 = signUpResponse3.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("API2 Signup is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse3 = pageObj.endpoints().sendMessageToUser(userID3, dataSet.get("apiKey"), "16",
				dataSet.get("redeemable_id"), "", "");

		pageObj.utils().logPass("Send redeemable to the user successfully");

		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse3.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		pageObj.utils().logPass("Api2  send reward amount to user is successful");

		// get reward id
		rewardId1 = pageObj.redeemablesPage().getRewardId(token3, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id"));

		pageObj.utils().logPass("Reward id " + rewardId1 + " is generated successfully ");

		// POS Auto Unlock
		Response autoUnlockResponse4 = pageObj.endpoints().autoSelectPosApi(userID3, "30", "1", "12003", "",
				dataSet.get("locationkey"));
		Assert.assertEquals(autoUnlockResponse4.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Auto Unlock ");
		pageObj.utils().logPass("POS Auto Select Api is successful");

		// https://punchhdev.atlassian.net/browse/OMM-1227 - functionality deprecated -
		// confirmed by Rahul Garg

//		String query5 = "Select external_uid from discount_baskets where user_id ='" + userID3 + "'";
//		String expColValue5 = DBUtils.executeQueryAndGetColumnValue(env, query5, "external_uid");
//		Assert.assertEquals(expColValue5, null, "Value is not present at external_uid column in discount basket ");
//		logger.info(
//				"external_uid not sent -> Basket gets created with applicable rewards added in basket and external uid remains NULL in DB ");
//		pageObj.utils().logPass(
//				"external_uid not sent -> Basket gets created with applicable rewards added in basket and external uid remains NULL in DB");
	}

	@Test(description = "SQ-T4305 POS discount lookup API>Base Item and Modifier>Menu Aggregator>Validate that offer#1 and offer#2 gets applied in which bundle price is greater than target bundle price and both LIF's are having Filter Item Set as Base item and modifier", groups = { "regression", "dailyrun" }, priority = 3)
	@Owner(name = "Hardik Bhardwaj")
	public void T4305_TargetBundlePriceAndBothLIFs() throws Exception {

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Navigate to Multiple Redemption Tab and check the flags
		// pageObj.menupage().clickCockPitMenu();
		// pageObj.menupage().redeemptionLinkInCockpit();
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_discount_locking", "uncheck");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_auto_unlock", "check");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_allow_multiple_redemption_on_item", "check");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_allow_qualifying_items_reused", "check");
		pageObj.dashboardpage().updateCheckBox();

		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();
		Map<String, String> detailsMap2 = new HashMap<String, String>();
		Map<String, String> detailsMap3 = new HashMap<String, String>();
		Map<String, String> detailsMap4 = new HashMap<String, String>();
		Map<String, String> detailsMap5 = new HashMap<String, String>();
		Map<String, String> detailsMap6 = new HashMap<String, String>();

		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Sandwich", "1", "11", "M", "1001", "522", "1.0", "108");
		parentMap.put("Sandwich", detailsMap1);

		detailsMap2 = pageObj.endpoints().getRecieptDetailsMap("Chicken", "1", "3", "M", "1002", "522", "1.1", "106");
		parentMap.put("Chicken", detailsMap2);

		detailsMap3 = pageObj.endpoints().getRecieptDetailsMap("Onion", "1", "5", "M", "9015", "522", "1.2", "106");
		parentMap.put("Onion", detailsMap3);

		detailsMap4 = pageObj.endpoints().getRecieptDetailsMap("Tomato", "2", "9", "M", "9015", "522", "1.3", "106");
		parentMap.put("Tomato", detailsMap4);

		detailsMap5 = pageObj.endpoints().getRecieptDetailsMap("Coke", "1", "4", "M", "860", "522", "2.0", "107");
		parentMap.put("Coke", detailsMap5);

		detailsMap6 = pageObj.endpoints().getRecieptDetailsMap("Eggs", "2", "8", "M", "86", "522", "2.1", "106");
		parentMap.put("Eggs", detailsMap6);

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
		pageObj.utils().logPass("API2 Signup is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse1 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "16",
				dataSet.get("redeemable_id1"), "", "");
		Assert.assertEquals(sendRewardResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		pageObj.utils().logPass("Send redeemable to the user successfully");

		// get reward id
		String rewardId1 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id1"));

		// send reward amount to user Reedemable
		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "16",
				dataSet.get("redeemable_id2"), "", "");
		Assert.assertEquals(sendRewardResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		pageObj.utils().logPass("Send redeemable to the user successfully");

		// get reward id
		String rewardId2 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id2"));

		// add reward completion to basket
		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAddedForPOSAPIWithExt_Uid(
				dataSet.get("locationkey"), userID, "reward", rewardId1, externalUID);
		Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		pageObj.utils().logPass("add reward completion to basket is successfully");

		// add reward completion to basket
		Response discountBasketResponse1 = pageObj.endpoints().authListDiscountBasketAddedForPOSAPIWithExt_Uid(
				dataSet.get("locationkey"), userID, "reward", rewardId2, externalUID);
		Assert.assertEquals(discountBasketResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		pageObj.utils().logPass("add reward completion to basket is successfully");

		Response batchRedemptionProcessResponseUser = pageObj.endpoints()
				.processBatchRedemptionOfBasketPOSDiscountLookup(dataSet.get("locationkey"), userID, "36", parentMap);
		Assert.assertEquals(batchRedemptionProcessResponseUser.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"POS Discount Look up status code is not 200 ");

		String discountAmount1 = batchRedemptionProcessResponseUser.jsonPath()
				.get("selected_discounts[0].discount_amount").toString();
		Assert.assertEquals(discountAmount1, "30.0",
				"discount amount for offer 1 is not matching and its actual value is  " + discountAmount1);
		pageObj.utils().logPass("discount amount for offer 1 is matching");

	}

	@SuppressWarnings("static-access")
	@Test(description = "SQ-T4306 POS discount lookup API> Validate the discount amount when offer#1 is having processing function-BOGO Fix and Offer#2 is having Target bundle price", groups = { "regression", "dailyrun" }, priority = 4)
	@Owner(name = "Hardik Bhardwaj")
	public void T4306_TargetBundlePriceBOGOAndBothLIFs() throws Exception {

		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();
		Map<String, String> detailsMap2 = new HashMap<String, String>();
		Map<String, String> detailsMap3 = new HashMap<String, String>();
		Map<String, String> detailsMap4 = new HashMap<String, String>();
		Map<String, String> detailsMap5 = new HashMap<String, String>();
		Map<String, String> detailsMap6 = new HashMap<String, String>();

		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Sandwich", "2", "11", "M", "1001", "522", "1.0", "108");
		parentMap.put("Sandwich", detailsMap1);

		detailsMap2 = pageObj.endpoints().getRecieptDetailsMap("Chicken", "1", "3", "M", "1002", "522", "1.1", "106");
		parentMap.put("Chicken", detailsMap2);

		detailsMap3 = pageObj.endpoints().getRecieptDetailsMap("Onion", "1", "5", "M", "9015", "522", "1.2", "106");
		parentMap.put("Onion", detailsMap3);

		detailsMap4 = pageObj.endpoints().getRecieptDetailsMap("Tomato", "2", "9", "M", "9015", "522", "1.3", "106");
		parentMap.put("Tomato", detailsMap4);

		detailsMap5 = pageObj.endpoints().getRecieptDetailsMap("Coke", "1", "4", "M", "860", "522", "2.0", "107");
		parentMap.put("Coke", detailsMap5);

		detailsMap6 = pageObj.endpoints().getRecieptDetailsMap("Eggs", "2", "8", "M", "86", "522", "2.1", "106");
		parentMap.put("Eggs", detailsMap6);

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
		pageObj.utils().logPass("API2 Signup is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse1 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "16",
				dataSet.get("redeemable_id1"), "", "");
		Assert.assertEquals(sendRewardResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		pageObj.utils().logPass("Send redeemable to the user successfully");

		// get reward id
		String rewardId1 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id1"));

		// send reward amount to user Reedemable
		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "16",
				dataSet.get("redeemable_id2"), "", "");
		Assert.assertEquals(sendRewardResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		pageObj.utils().logPass("Send redeemable to the user successfully");

		// get reward id
		String rewardId2 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id2"));

		String query1 = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ dataSet.get("business_id") + "';";
		String preferences = DBUtils.executeQueryAndGetColumnValue(env, query1, "preferences");
		String flag = DBUtils.businessesPreferenceFlag(preferences,
				"enable_decoupled_redemption_engine");
		pageObj.utils().logPass("enable_decoupled_redemption_engine is set to " + flag + " in preferences");

		if (flag.equalsIgnoreCase("false")) {
			// add reward completion to basket
			Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAddedForPOSAPIWithExt_Uid(
					dataSet.get("locationkey"), userID, "reward", rewardId1, externalUID);
			Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
			pageObj.utils().logPass("add reward completion to basket is successfully");

			// add reward completion to basket
			Response discountBasketResponse1 = pageObj.endpoints().authListDiscountBasketAddedForPOSAPIWithExt_Uid(
					dataSet.get("locationkey"), userID, "reward", rewardId2, externalUID);
			Assert.assertEquals(discountBasketResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
			pageObj.utils().logPass("add reward completion to basket is successfully");

			Response batchRedemptionProcessResponseUser = pageObj.endpoints()
					.processBatchRedemptionOfBasketPOSDiscountLookup(dataSet.get("locationkey"), userID, "40",
							parentMap);
			Assert.assertEquals(batchRedemptionProcessResponseUser.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
					"POS Discount Look up status code is not 200 ");

			String discountAmount1 = batchRedemptionProcessResponseUser.jsonPath()
					.get("selected_discounts[0].discount_amount").toString();
			Assert.assertEquals(discountAmount1, "14.0",
					"discount amount for offer 1 is not matching and its actual value is  " + discountAmount1);
			pageObj.utils().logPass("discount amount for offer 1 is matching");

			String discountAmount2 = batchRedemptionProcessResponseUser.jsonPath()
					.get("selected_discounts[1].discount_amount").toString();
			Assert.assertEquals(discountAmount2, "11.7",
					"discount amount for offer 2 is not matching and its actual value is  " + discountAmount2);
			pageObj.utils().logPass("discount amount for offer 2 is matching");
		}

	}

	@SuppressWarnings("static-access")
	@Test(description = "SQ-T4307 POS discount lookup API> Validate the discount amount when offer#1 is having processing function-Rate Rollbackand Offer#2 is having Target bundle price", groups = { "regression", "dailyrun" }, priority = 5)
	@Owner(name = "Hardik Bhardwaj")
	public void T4307_TargetBundlePriceRateRollbackAndBothLIFs() throws Exception {

		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();
		Map<String, String> detailsMap2 = new HashMap<String, String>();
		Map<String, String> detailsMap3 = new HashMap<String, String>();
		Map<String, String> detailsMap4 = new HashMap<String, String>();
		Map<String, String> detailsMap5 = new HashMap<String, String>();
		Map<String, String> detailsMap6 = new HashMap<String, String>();

		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Sandwich", "1", "11", "M", "1001", "522", "1.0", "108");
		parentMap.put("Sandwich", detailsMap1);

		detailsMap2 = pageObj.endpoints().getRecieptDetailsMap("Chicken", "1", "3", "M", "1002", "522", "1.1", "106");
		parentMap.put("Chicken", detailsMap2);

		detailsMap3 = pageObj.endpoints().getRecieptDetailsMap("Onion", "1", "5", "M", "9015", "522", "1.2", "106");
		parentMap.put("Onion", detailsMap3);

		detailsMap4 = pageObj.endpoints().getRecieptDetailsMap("Tomato", "2", "9", "M", "9015", "522", "1.3", "106");
		parentMap.put("Tomato", detailsMap4);

		detailsMap5 = pageObj.endpoints().getRecieptDetailsMap("Coke", "1", "4", "M", "860", "522", "2.0", "107");
		parentMap.put("Coke", detailsMap5);

		detailsMap6 = pageObj.endpoints().getRecieptDetailsMap("Eggs", "2", "8", "M", "86", "522", "2.1", "106");
		parentMap.put("Eggs", detailsMap6);

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
		pageObj.utils().logPass("API2 Signup is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse1 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "16",
				dataSet.get("redeemable_id1"), "", "");
		Assert.assertEquals(sendRewardResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		pageObj.utils().logPass("Send redeemable to the user successfully");

		// get reward id
		String rewardId1 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id1"));

		// send reward amount to user Reedemable
		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "16",
				dataSet.get("redeemable_id2"), "", "");
		Assert.assertEquals(sendRewardResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		pageObj.utils().logPass("Send redeemable to the user successfully");

		// get reward id
		String rewardId2 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id2"));

		// add reward completion to basket
		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAddedForPOSAPIWithExt_Uid(
				dataSet.get("locationkey"), userID, "reward", rewardId1, externalUID);
		Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		pageObj.utils().logPass("add reward completion to basket is successfully");

		// add reward completion to basket
		Response discountBasketResponse1 = pageObj.endpoints().authListDiscountBasketAddedForPOSAPIWithExt_Uid(
				dataSet.get("locationkey"), userID, "reward", rewardId2, externalUID);
		Assert.assertEquals(discountBasketResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		pageObj.utils().logPass("add reward completion to basket is successfully");

		String query1 = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ dataSet.get("business_id") + "';";
		String preferences = DBUtils.executeQueryAndGetColumnValue(env, query1, "preferences");
		String flag = DBUtils.businessesPreferenceFlag(preferences,
				"enable_decoupled_redemption_engine");
		pageObj.utils().logPass("enable_decoupled_redemption_engine is set to " + flag + " in preferences");

		Response batchRedemptionProcessResponseUser = pageObj.endpoints()
				.processBatchRedemptionOfBasketPOSDiscountLookup(dataSet.get("locationkey"), userID, "40", parentMap);
		Assert.assertEquals(batchRedemptionProcessResponseUser.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"POS Discount Look up status code is not 200 ");

		String discountAmount1 = batchRedemptionProcessResponseUser.jsonPath()
				.get("selected_discounts[0].discount_amount").toString();
		Assert.assertEquals(discountAmount1, "2.0",
				"discount amount for offer 1 is not matching and its actual value is  " + discountAmount1);
		pageObj.utils().logPass("discount amount for offer 1 is matching");

		String discountAmount2 = batchRedemptionProcessResponseUser.jsonPath()
				.get("selected_discounts[1].discount_amount").toString();
		if (flag.equalsIgnoreCase("true")) {
			Assert.assertEquals(discountAmount2, "28.01",
					"discount amount for offer 2 is not matching and its actual value is  " + discountAmount2);
			pageObj.utils().logPass("discount amount for offer 2 is matching");
		} else if (flag.equalsIgnoreCase("false")) {
			Assert.assertEquals(discountAmount2, "28.0",
					"discount amount for offer 2 is not matching and its actual value is  " + discountAmount2);
			pageObj.utils().logPass("discount amount for offer 2 is matching");
		}

	}

	@SuppressWarnings("static-access")
	@Test(description = "SQ-T4308 POS discount lookup API> Validate the discount amount when offer#1 is having processing function-Hit Target Menu Min and Offer#2 is having Target bundle price", groups = { "regression", "dailyrun" }, priority = 6)
	@Owner(name = "Hardik Bhardwaj")
	public void T4308_TargetBundlePriceHitTargetMenuAndBothLIFs() throws Exception {

		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();
		Map<String, String> detailsMap2 = new HashMap<String, String>();
		Map<String, String> detailsMap3 = new HashMap<String, String>();
		Map<String, String> detailsMap4 = new HashMap<String, String>();
		Map<String, String> detailsMap5 = new HashMap<String, String>();
		Map<String, String> detailsMap6 = new HashMap<String, String>();

		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Sandwich", "1", "11", "M", "1001", "522", "1.0", "108");
		parentMap.put("Sandwich", detailsMap1);

		detailsMap2 = pageObj.endpoints().getRecieptDetailsMap("Chicken", "1", "3", "M", "1002", "522", "1.1", "106");
		parentMap.put("Chicken", detailsMap2);

		detailsMap3 = pageObj.endpoints().getRecieptDetailsMap("Onion", "1", "5", "M", "9015", "522", "1.2", "106");
		parentMap.put("Onion", detailsMap3);

		detailsMap4 = pageObj.endpoints().getRecieptDetailsMap("Tomato", "2", "9", "M", "9015", "522", "1.3", "106");
		parentMap.put("Tomato", detailsMap4);

		detailsMap5 = pageObj.endpoints().getRecieptDetailsMap("Coke", "1", "4", "M", "860", "522", "2.0", "107");
		parentMap.put("Coke", detailsMap5);

		detailsMap6 = pageObj.endpoints().getRecieptDetailsMap("Eggs", "2", "8", "M", "86", "522", "2.1", "106");
		parentMap.put("Eggs", detailsMap6);

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
		pageObj.utils().logPass("API2 Signup is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse1 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "16",
				dataSet.get("redeemable_id1"), "", "");
		Assert.assertEquals(sendRewardResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		pageObj.utils().logPass("Send redeemable to the user successfully");

		// get reward id
		String rewardId1 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id1"));

		// send reward amount to user Reedemable
		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "16",
				dataSet.get("redeemable_id2"), "", "");
		Assert.assertEquals(sendRewardResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		pageObj.utils().logPass("Send redeemable to the user successfully");

		// get reward id
		String rewardId2 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id2"));

		// add reward completion to basket
		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAddedForPOSAPIWithExt_Uid(
				dataSet.get("locationkey"), userID, "reward", rewardId1, externalUID);
		Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		pageObj.utils().logPass("add reward completion to basket is successfully");

		// add reward completion to basket
		Response discountBasketResponse1 = pageObj.endpoints().authListDiscountBasketAddedForPOSAPIWithExt_Uid(
				dataSet.get("locationkey"), userID, "reward", rewardId2, externalUID);
		Assert.assertEquals(discountBasketResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		pageObj.utils().logPass("add reward completion to basket is successfully");

		Response batchRedemptionProcessResponseUser = pageObj.endpoints()
				.processBatchRedemptionOfBasketPOSDiscountLookup(dataSet.get("locationkey"), userID, "40", parentMap);
		Assert.assertEquals(batchRedemptionProcessResponseUser.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"POS Discount Look up status code is not 200 ");

		String discountAmount1 = batchRedemptionProcessResponseUser.jsonPath()
				.get("selected_discounts[0].discount_amount").toString();
		Assert.assertEquals(discountAmount1, "30.0",
				"discount amount for offer 1 is not matching and its actual value is  " + discountAmount1);
		pageObj.utils().logPass("discount amount for offer 1 is matching");

		String query1 = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ dataSet.get("business_id") + "';";
		String preferences = DBUtils.executeQueryAndGetColumnValue(env, query1, "preferences");
		String flag = DBUtils.businessesPreferenceFlag(preferences,
				"enable_decoupled_redemption_engine");
		pageObj.utils().logPass("enable_decoupled_redemption_engine is set to " + flag + " in preferences");

		if (flag.equalsIgnoreCase("true")) {
			String itemType1 = batchRedemptionProcessResponseUser.jsonPath()
					.get("selected_discounts[0].qualified_items[2].item_type").toString();
			Assert.assertEquals(itemType1, "R",
					"item_typefor offer 1 is not matching and its actual value is  " + itemType1);
			pageObj.utils().logPass("item_type for offer 1 is matching");

			String itemType2 = batchRedemptionProcessResponseUser.jsonPath()
					.get("selected_discounts[0].qualified_items[6].item_type").toString();
			Assert.assertEquals(itemType2, "R",
					"item_typefor offer 1 is not matching and its actual value is  " + itemType2);
			pageObj.utils().logPass("item_type for offer 1 is matching");

			String itemType3 = batchRedemptionProcessResponseUser.jsonPath()
					.get("selected_discounts[0].qualified_items[7].item_type").toString();
			Assert.assertEquals(itemType3, "R",
					"item_typefor offer 1 is not matching and its actual value is  " + itemType3);
			pageObj.utils().logPass("item_type for offer 1 is matching");

			String itemType4 = batchRedemptionProcessResponseUser.jsonPath()
					.get("selected_discounts[0].qualified_items[8].item_type").toString();
			Assert.assertEquals(itemType4, "R",
					"item_typefor offer 1 is not matching and its actual value is  " + itemType4);
			pageObj.utils().logPass("item_type for offer 1 is matching");

			String itemType5 = batchRedemptionProcessResponseUser.jsonPath()
					.get("selected_discounts[0].qualified_items[9].item_type").toString();
			Assert.assertEquals(itemType5, "R",
					"item_typefor offer 1 is not matching and its actual value is  " + itemType5);
			pageObj.utils().logPass("item_type for offer 1 is matching");

			String itemType6 = batchRedemptionProcessResponseUser.jsonPath()
					.get("selected_discounts[0].qualified_items[11].item_type").toString();
			Assert.assertEquals(itemType6, "R",
					"item_typefor offer 1 is not matching and its actual value is  " + itemType6);
			pageObj.utils().logPass("item_type for offer 1 is matching");

			String itemType7 = batchRedemptionProcessResponseUser.jsonPath()
					.get("selected_discounts[1].qualified_items[2].item_type").toString();
			Assert.assertEquals(itemType7, "R",
					"item_typefor offer 1 is not matching and its actual value is  " + itemType7);
			pageObj.utils().logPass("item_type for offer 2 is matching");

			String itemType8 = batchRedemptionProcessResponseUser.jsonPath()
					.get("selected_discounts[1].qualified_items[6].item_type").toString();
			Assert.assertEquals(itemType8, "R",
					"item_typefor offer 1 is not matching and its actual value is  " + itemType8);
			pageObj.utils().logPass("item_type for offer 2 is matching");

			String itemType9 = batchRedemptionProcessResponseUser.jsonPath()
					.get("selected_discounts[1].qualified_items[7].item_type").toString();
			Assert.assertEquals(itemType9, "R",
					"item_typefor offer 1 is not matching and its actual value is  " + itemType9);
			pageObj.utils().logPass("item_type for offer 2 is matching");

			String itemType10 = batchRedemptionProcessResponseUser.jsonPath()
					.get("selected_discounts[1].qualified_items[8].item_type").toString();
			Assert.assertEquals(itemType10, "R",
					"item_typefor offer 1 is not matching and its actual value is  " + itemType10);
			pageObj.utils().logPass("item_type for offer 2 is matching");

			String itemType11 = batchRedemptionProcessResponseUser.jsonPath()
					.get("selected_discounts[1].qualified_items[9].item_type").toString();
			Assert.assertEquals(itemType11, "R",
					"item_typefor offer 1 is not matching and its actual value is  " + itemType11);
			pageObj.utils().logPass("item_type for offer 2 is matching");

			String itemType12 = batchRedemptionProcessResponseUser.jsonPath()
					.get("selected_discounts[1].qualified_items[11].item_type").toString();
			Assert.assertEquals(itemType12, "R",
					"item_typefor offer 1 is not matching and its actual value is  " + itemType12);
			pageObj.utils().logPass("item_type for offer 2 is matching");
		}

	}

	@Test(description = "SQ-T4304 POS discount lookup API>Base Item and Modifier>Validate that discount amount when bundle price is greater than target bundle price and both LIF's are having Filter Item Set as Base item and modifier", groups = { "regression", "dailyrun" }, priority = 7)
	@Owner(name = "Hardik Bhardwaj")
	public void T4304_TargetBundlePriceWithOutMenuItemAggregatorAndBothLIFs() throws Exception {

		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();
		Map<String, String> detailsMap2 = new HashMap<String, String>();
		Map<String, String> detailsMap3 = new HashMap<String, String>();
		Map<String, String> detailsMap4 = new HashMap<String, String>();
		Map<String, String> detailsMap5 = new HashMap<String, String>();
		Map<String, String> detailsMap6 = new HashMap<String, String>();

		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Sandwich", "1", "11", "M", "1001", "522", "1.0", "108");
		parentMap.put("Sandwich", detailsMap1);

		detailsMap2 = pageObj.endpoints().getRecieptDetailsMap("Chicken", "1", "3", "M", "1002", "522", "1.1", "106");
		parentMap.put("Chicken", detailsMap2);

		detailsMap3 = pageObj.endpoints().getRecieptDetailsMap("Onion", "1", "5", "M", "9015", "522", "1.2", "106");
		parentMap.put("Onion", detailsMap3);

		detailsMap4 = pageObj.endpoints().getRecieptDetailsMap("Tomato", "2", "9", "M", "9015", "522", "1.3", "106");
		parentMap.put("Tomato", detailsMap4);

		detailsMap5 = pageObj.endpoints().getRecieptDetailsMap("Coke", "1", "4", "M", "860", "522", "2.0", "107");
		parentMap.put("Coke", detailsMap5);

		detailsMap6 = pageObj.endpoints().getRecieptDetailsMap("Eggs", "2", "8", "M", "86", "522", "2.1", "106");
		parentMap.put("Eggs", detailsMap6);

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
		pageObj.utils().logPass("User SignUp using API is successfully");

		// send reward amount to user Reedemable
		Response sendRewardResponse1 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "16",
				dataSet.get("redeemable_id1"), "", "");
		Assert.assertEquals(sendRewardResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		pageObj.utils().logPass("Send redeemable to the user successfully");

		// get reward id
		String rewardId1 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id1"));

		// send reward amount to user Reedemable
		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "16",
				dataSet.get("redeemable_id2"), "", "");
		Assert.assertEquals(sendRewardResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		pageObj.utils().logPass("Send redeemable to the user successfully");

		// get reward id
		String rewardId2 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id2"));

		// add reward completion to basket
		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAddedForPOSAPIWithExt_Uid(
				dataSet.get("locationkey"), userID, "reward", rewardId1, externalUID);
		Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		pageObj.utils().logPass("add reward completion to basket is successfully");

		// add reward completion to basket
		Response discountBasketResponse1 = pageObj.endpoints().authListDiscountBasketAddedForPOSAPIWithExt_Uid(
				dataSet.get("locationkey"), userID, "reward", rewardId2, externalUID);
		Assert.assertEquals(discountBasketResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		pageObj.utils().logPass("add reward completion to basket is successfully");

		String query1 = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ dataSet.get("business_id") + "';";
		String preferences = DBUtils.executeQueryAndGetColumnValue(env, query1, "preferences");
		String flag = DBUtils.businessesPreferenceFlag(preferences,
				"enable_decoupled_redemption_engine");
		pageObj.utils().logPass("enable_decoupled_redemption_engine is set to " + flag + " in preferences");

		Response batchRedemptionProcessResponseUser = pageObj.endpoints()
				.processBatchRedemptionOfBasketPOSDiscountLookup(dataSet.get("locationkey"), userID, "40", parentMap);
		Assert.assertEquals(batchRedemptionProcessResponseUser.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"POS Discount Look up status code is not 200 ");

		String discountAmount1 = batchRedemptionProcessResponseUser.jsonPath()
				.get("selected_discounts[0].discount_amount").toString();
		Assert.assertEquals(discountAmount1, "30.0",
				"discount amount for offer 1 is not matching and its actual value is  " + discountAmount1);
		pageObj.utils().logPass("discount amount for offer 1 is matching");

		String discountAmount2 = batchRedemptionProcessResponseUser.jsonPath()
				.get("selected_discounts[1].discount_amount").toString();

		if (flag.equalsIgnoreCase("true")) {
			Assert.assertEquals(discountAmount2, "2.71",
					"discount amount for offer 2 is not matching and its actual value is  " + discountAmount2);
			pageObj.utils().logPass("discount amount for offer 2 is matching");
		} else if (flag.equalsIgnoreCase("false")) {
			Assert.assertEquals(discountAmount2, "2.7",
					"discount amount for offer 2 is not matching and its actual value is  " + discountAmount2);
			pageObj.utils().logPass("discount amount for offer 2 is matching");
		}

	}

	@Test(description = "SQ-T4303 POS discount lookup API>Base Item and Modifier>Validate that discount amount when bundle price is greater than target bundle price and LIF-1 is having base item and modifier and LIF-2 is having Only Modifiers", groups = { "regression", "dailyrun" }, priority = 8)
	@Owner(name = "Hardik Bhardwaj")
	public void T4303_TargetBundlePriceAndBothLIFsOnlyModifier() throws Exception {

		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();
		Map<String, String> detailsMap2 = new HashMap<String, String>();
		Map<String, String> detailsMap3 = new HashMap<String, String>();
		Map<String, String> detailsMap4 = new HashMap<String, String>();
		Map<String, String> detailsMap5 = new HashMap<String, String>();
		Map<String, String> detailsMap6 = new HashMap<String, String>();

		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Sandwich", "1", "11", "M", "1001", "522", "1.0", "108");
		parentMap.put("Sandwich", detailsMap1);

		detailsMap2 = pageObj.endpoints().getRecieptDetailsMap("Chicken", "1", "3", "M", "1002", "522", "1.1", "106");
		parentMap.put("Chicken", detailsMap2);

		detailsMap3 = pageObj.endpoints().getRecieptDetailsMap("Onion", "1", "5", "M", "9015", "522", "1.2", "106");
		parentMap.put("Onion", detailsMap3);

		detailsMap4 = pageObj.endpoints().getRecieptDetailsMap("Tomato", "2", "9", "M", "9015", "522", "1.3", "106");
		parentMap.put("Tomato", detailsMap4);

		detailsMap5 = pageObj.endpoints().getRecieptDetailsMap("Coke", "1", "4", "M", "860", "522", "2.0", "107");
		parentMap.put("Coke", detailsMap5);

		detailsMap6 = pageObj.endpoints().getRecieptDetailsMap("Eggs", "2", "8", "M", "86", "522", "2.1", "106");
		parentMap.put("Eggs", detailsMap6);

		// User SignUp using API
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.utils().logPass("User SignUp using API is successfully");
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertEquals(signUpResponse.jsonPath().get("user.communicable_email").toString(),
				userEmail.toLowerCase());
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();

		// send reward amount to user Reedemable
		Response sendRewardResponse1 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "16",
				dataSet.get("redeemable_id1"), "", "");
		Assert.assertEquals(sendRewardResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		pageObj.utils().logPass("Send redeemable to the user successfully");

		// get reward id
		String rewardId1 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id1"));

		// send reward amount to user Reedemable
		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "16",
				dataSet.get("redeemable_id2"), "", "");
		Assert.assertEquals(sendRewardResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		pageObj.utils().logPass("Send redeemable to the user successfully");

		// get reward id
		String rewardId2 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id2"));

		// add reward completion to basket
		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAddedForPOSAPIWithExt_Uid(
				dataSet.get("locationkey"), userID, "reward", rewardId1, externalUID);
		Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		pageObj.utils().logPass("add reward completion to basket is successfully");

		// add reward completion to basket
		Response discountBasketResponse1 = pageObj.endpoints().authListDiscountBasketAddedForPOSAPIWithExt_Uid(
				dataSet.get("locationkey"), userID, "reward", rewardId2, externalUID);
		Assert.assertEquals(discountBasketResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		pageObj.utils().logPass("add reward completion to basket is successfully");

		String query1 = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ dataSet.get("business_id") + "';";
		String preferences = DBUtils.executeQueryAndGetColumnValue(env, query1, "preferences");
		String flag = DBUtils.businessesPreferenceFlag(preferences,
				"enable_decoupled_redemption_engine");
		pageObj.utils().logPass("enable_decoupled_redemption_engine is set to " + flag + " in preferences");

		Response batchRedemptionProcessResponseUser = pageObj.endpoints()
				.processBatchRedemptionOfBasketPOSDiscountLookup(dataSet.get("locationkey"), userID, "40", parentMap);
		Assert.assertEquals(batchRedemptionProcessResponseUser.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"POS Discount Look up status code is not 200 ");

		String discountAmount1 = batchRedemptionProcessResponseUser.jsonPath()
				.get("selected_discounts[0].discount_amount").toString();
		Assert.assertEquals(discountAmount1, "22.0",
				"discount amount for offer 1 is not matching and its actual value is  " + discountAmount1);
		pageObj.utils().logPass("discount amount for offer 1 is matching");

		String discountAmount2 = batchRedemptionProcessResponseUser.jsonPath()
				.get("selected_discounts[1].discount_amount").toString();

		if (flag.equalsIgnoreCase("true")) {
			Assert.assertEquals(discountAmount2, "2.71",
					"discount amount for offer 2 is not matching and its actual value is  " + discountAmount2);
			pageObj.utils().logPass("discount amount for offer 2 is matching");
		} else if (flag.equalsIgnoreCase("false")) {
			Assert.assertEquals(discountAmount2, "4.08",
					"discount amount for offer 2 is not matching and its actual value is  " + discountAmount2);
			pageObj.utils().logPass("discount amount for offer 2 is matching");
		}

	}

	@Test(description = "SQ-T4302 POS discount lookup API>Only Modifier>Multiple Offers>Multiple bundles>Validate that no discount gets applied of bundle price is less than target bundle price", groups = { "regression", "dailyrun" }, priority = 8)
	@Owner(name = "Hardik Bhardwaj")
	public void T4302_TargetBundlePriceAndBothLIFsOnlyModifier() throws Exception {

		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();
		Map<String, String> detailsMap2 = new HashMap<String, String>();
		Map<String, String> detailsMap3 = new HashMap<String, String>();
		Map<String, String> detailsMap5 = new HashMap<String, String>();
		Map<String, String> detailsMap6 = new HashMap<String, String>();

		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Sandwich", "1", "11", "M", "1001", "522", "1.0", "106");
		parentMap.put("Sandwich", detailsMap1);

		detailsMap2 = pageObj.endpoints().getRecieptDetailsMap("Chicken", "2", "5", "M", "1002", "522", "1.1", "106");
		parentMap.put("Chicken", detailsMap2);

		detailsMap3 = pageObj.endpoints().getRecieptDetailsMap("Arepas", "2", "5", "M", "9015", "522", "1.2", "106");
		parentMap.put("Arepas", detailsMap3);

		detailsMap5 = pageObj.endpoints().getRecieptDetailsMap("Coke", "1", "4", "M", "860", "522", "2.0", "106");
		parentMap.put("Coke", detailsMap5);

		detailsMap6 = pageObj.endpoints().getRecieptDetailsMap("Eggs", "2", "5", "M", "860", "522", "2.1", "106");
		parentMap.put("Eggs", detailsMap6);

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
		pageObj.utils().logPass("User SignUp using API is successfully");

		// send reward amount to user Reedemable
		Response sendRewardResponse1 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "16",
				dataSet.get("redeemable_id1"), "", "");
		Assert.assertEquals(sendRewardResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		pageObj.utils().logPass("Send redeemable to the user successfully");

		// get reward id
		String rewardId1 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id1"));

		// send reward amount to user Reedemable
		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "16",
				dataSet.get("redeemable_id2"), "", "");
		Assert.assertEquals(sendRewardResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		pageObj.utils().logPass("Send redeemable to the user successfully");

		// get reward id
		String rewardId2 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id2"));

		// add reward completion to basket
		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAddedForPOSAPIWithExt_Uid(
				dataSet.get("locationkey"), userID, "reward", rewardId1, externalUID);
		Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		pageObj.utils().logPass("add reward completion to basket is successfully");

		// add reward completion to basket
		Response discountBasketResponse1 = pageObj.endpoints().authListDiscountBasketAddedForPOSAPIWithExt_Uid(
				dataSet.get("locationkey"), userID, "reward", rewardId2, externalUID);
		Assert.assertEquals(discountBasketResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		pageObj.utils().logPass("add reward completion to basket is successfully");

		Response batchRedemptionProcessResponseUser = pageObj.endpoints()
				.processBatchRedemptionOfBasketPOSDiscountLookup(dataSet.get("locationkey"), userID, "30", parentMap);
		Assert.assertEquals(batchRedemptionProcessResponseUser.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"POS Discount Look up status code is not 200 ");
		String message1 = batchRedemptionProcessResponseUser.jsonPath().get("selected_discounts[0].message[0]")
				.toString();
		Assert.assertEquals(message1, "Redemption not possible since amount is 0.",
				"message for offer 1 'Redemption not possible since amount is 0.' is not matching and its actual message is  "
						+ message1);
		pageObj.utils().logPass("message for offer 1 is matching");

		String message2 = batchRedemptionProcessResponseUser.jsonPath().get("selected_discounts[0].message[0]")
				.toString();
		Assert.assertEquals(message2, "Redemption not possible since amount is 0.",
				"message for offer 2 'Redemption not possible since amount is 0.' is not matching and its actual message is  "
						+ message2);
		pageObj.utils().logPass("message for offer 2 is matching");

	}

	@Test(description = "SQ-T4301 POS discount lookup API>Only Modifier items>Single Offer>Validate that discount amount for target bundle price processing method when there is no base item and modifiers having different item_ids and LIF is having qty-2", groups = { "regression", "dailyrun" }, priority = 8)
	@Owner(name = "Hardik Bhardwaj")
	public void T4301_TargetBundlePriceAndBothLIFsOnlyModifier() throws Exception {

		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();
		Map<String, String> detailsMap2 = new HashMap<String, String>();
		Map<String, String> detailsMap5 = new HashMap<String, String>();

		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Sandwich", "1", "20", "M", "1002", "522", "1.1", "106");
		parentMap.put("Sandwich", detailsMap1);

		detailsMap2 = pageObj.endpoints().getRecieptDetailsMap("Sandwich", "1", "30", "M", "1003", "522", "1.1", "106");
		parentMap.put("Sandwich", detailsMap2);

		detailsMap5 = pageObj.endpoints().getRecieptDetailsMap("Coke", "1", "11", "M", "860", "522", "4.0", "106");
		parentMap.put("Coke", detailsMap5);

		// User SignUp using API
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.utils().logPass("User SignUp using API is successfully");
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertEquals(signUpResponse.jsonPath().get("user.communicable_email").toString(),
				userEmail.toLowerCase());
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();

		// send reward amount to user Reedemable
		Response sendRewardResponse1 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "16",
				dataSet.get("redeemable_id1"), "", "");
		Assert.assertEquals(sendRewardResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		pageObj.utils().logPass("Send redeemable to the user successfully");

		// get reward id
		String rewardId1 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id1"));

		// add reward completion to basket
		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAddedForPOSAPIWithExt_Uid(
				dataSet.get("locationkey"), userID, "reward", rewardId1, externalUID);
		Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		pageObj.utils().logPass("add reward completion to basket is successfully");

		Response batchRedemptionProcessResponseUser = pageObj.endpoints()
				.processBatchRedemptionOfBasketPOSDiscountLookup(dataSet.get("locationkey"), userID, "61", parentMap);
		Assert.assertEquals(batchRedemptionProcessResponseUser.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"POS Discount Look up status code is not 200 ");
		String discountAmount1 = batchRedemptionProcessResponseUser.jsonPath()
				.getString("selected_discounts[0].discount_amount");
		Assert.assertEquals(discountAmount1, null,
				"discount amount for offer 1 is not matching and its actual value is  " + discountAmount1);
		pageObj.utils().logPass("discount amount for offer 1 is matching");

	}

	@Test(description = "SQ-T4300 POS discount lookup API>Validate the discount amount when Offer #1 is having processing function-Target bundle price and Offer#2 is having 'Sum of Amounts' with Stacking-ON, Reusability-OFF and Item qualifier is same in both offers", groups = { "regression", "dailyrun" }, priority = 9)
	@Owner(name = "Hardik Bhardwaj")
	public void T4300_TargetBundlePriceAndBothLIFsOnlyModifier() throws Exception {

		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();
		Map<String, String> detailsMap2 = new HashMap<String, String>();
		Map<String, String> detailsMap3 = new HashMap<String, String>();
		Map<String, String> detailsMap5 = new HashMap<String, String>();
		Map<String, String> detailsMap6 = new HashMap<String, String>();

		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Sandwich", "1", "11", "M", "1001", "522", "1.0", "106");
		parentMap.put("SaNdwich", detailsMap1);

		detailsMap2 = pageObj.endpoints().getRecieptDetailsMap("Sandwich", "1", "11", "M", "1001", "522", "2.0", "106");
		parentMap.put("Sandwich", detailsMap2);

		detailsMap3 = pageObj.endpoints().getRecieptDetailsMap("Sandwich", "1", "11", "M", "1001", "522", "3.0", "106");
		parentMap.put("SanDwich", detailsMap3);

		detailsMap5 = pageObj.endpoints().getRecieptDetailsMap("Coke", "1", "4", "M", "860", "522", "4.0", "106");
		parentMap.put("Coke", detailsMap5);

		detailsMap6 = pageObj.endpoints().getRecieptDetailsMap("Scotch Eggs", "1", "4", "M", "86", "522", "5.0", "106");
		parentMap.put("Scotch Eggs", detailsMap6);

		// User SignUp using API
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.utils().logPass("User SignUp using API is successfully");
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertEquals(signUpResponse.jsonPath().get("user.communicable_email").toString(),
				userEmail.toLowerCase());
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();

		// send reward amount to user Reedemable
		Response sendRewardResponse1 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "16",
				dataSet.get("redeemable_id1"), "", "");
		Assert.assertEquals(sendRewardResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		pageObj.utils().logPass("Send redeemable to the user successfully");

		// get reward id
		String rewardId1 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id1"));

		// send reward amount to user Reedemable
		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "16",
				dataSet.get("redeemable_id2"), "", "");
		Assert.assertEquals(sendRewardResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		pageObj.utils().logPass("Send redeemable to the user successfully");

		// get reward id
		String rewardId2 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id2"));

		// add reward completion to basket
		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAddedForPOSAPIWithExt_Uid(
				dataSet.get("locationkey"), userID, "reward", rewardId1, externalUID);
		Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		pageObj.utils().logPass("add reward completion to basket is successfully");

		// add reward completion to basket
		Response discountBasketResponse1 = pageObj.endpoints().authListDiscountBasketAddedForPOSAPIWithExt_Uid(
				dataSet.get("locationkey"), userID, "reward", rewardId2, externalUID);
		Assert.assertEquals(discountBasketResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		pageObj.utils().logPass("add reward completion to basket is successfully");

		Response batchRedemptionProcessResponseUser = pageObj.endpoints()
				.processBatchRedemptionOfBasketPOSDiscountLookup(dataSet.get("locationkey"), userID, "41", parentMap);
		Assert.assertEquals(batchRedemptionProcessResponseUser.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"POS Discount Look up status code is not 200 ");

	}

	@Test(description = "SQ-T4299 POS discount lookup API>Only Base Items>Validate that error message gets displayed when LIF is having bundle of item with qty-2 but in input receipt item is present with qty-1 only", groups = { "regression", "dailyrun" }, priority = 9)
	@Owner(name = "Hardik Bhardwaj")
	public void T4299_TargetBundlePriceAndBothLIFsOnlyModifier() throws Exception {

		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();
		Map<String, String> detailsMap5 = new HashMap<String, String>();

		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Sandwich", "1", "5", "M", "1001", "522", "1.0", "106");
		parentMap.put("Sandwich", detailsMap1);

		detailsMap5 = pageObj.endpoints().getRecieptDetailsMap("Coke", "1", "5", "M", "860", "522", "3.0", "106");
		parentMap.put("Coke", detailsMap5);

		// User SignUp using API
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.utils().logPass("User SignUp using API is successfully");
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertEquals(signUpResponse.jsonPath().get("user.communicable_email").toString(),
				userEmail.toLowerCase());
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();

		// send reward amount to user Reedemable
		Response sendRewardResponse1 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "16",
				dataSet.get("redeemable_id1"), "", "");
		Assert.assertEquals(sendRewardResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		pageObj.utils().logPass("Send redeemable to the user successfully");

		// get reward id
		String rewardId1 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id1"));

		// add reward completion to basket
		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAddedForPOSAPIWithExt_Uid(
				dataSet.get("locationkey"), userID, "reward", rewardId1, externalUID);
		Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		pageObj.utils().logPass("add reward completion to basket is successfully");

		Response batchRedemptionProcessResponseUser = pageObj.endpoints()
				.processBatchRedemptionOfBasketPOSDiscountLookup(dataSet.get("locationkey"), userID, "10", parentMap);
		Assert.assertEquals(batchRedemptionProcessResponseUser.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"POS Discount Look up status code is not 200 ");
		String message1 = batchRedemptionProcessResponseUser.jsonPath().get("selected_discounts[0].message[0]")
				.toString();
		Assert.assertEquals(message1, "Redemption not possible since amount is 0.",
				"message for offer 1 'Redemption not possible since amount is 0.' is not matching and its actual message is  "
						+ message1);
		pageObj.utils().logPass("message for offer 1 is matching");

	}

	@Test(description = "SQ-T4298 POS discount lookup API>Only Base items>Multiple Offers>Validate the discount amount for target bundle price processing method having multiple rewards in discount basket when Item Qualifiers recycling OFF; Stacked discounting OFF", groups = { "regression", "dailyrun" }, priority = 10)
	@Owner(name = "Hardik Bhardwaj")
	public void T4298_TargetBundlePriceAndBothLIFsOnlyModifier() throws Exception {

		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();
		Map<String, String> detailsMap5 = new HashMap<String, String>();
		Map<String, String> detailsMap2 = new HashMap<String, String>();

		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Sandwich", "1", "7.5", "M", "", "", "1.0", "22222211");
		parentMap.put("Sandwich", detailsMap1);

		detailsMap2 = pageObj.endpoints().getRecieptDetailsMap("SandwicH", "1", "7.5", "M", "", "", "1.0", "22222211");
		parentMap.put("SandwicH", detailsMap2);

		detailsMap5 = pageObj.endpoints().getRecieptDetailsMap("French Fries", "1", "4", "M", "", "", "2.0", "8001");
		parentMap.put("French Fries", detailsMap5);

		// User SignUp using API
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.utils().logPass("User SignUp using API is successfully");
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertEquals(signUpResponse.jsonPath().get("user.communicable_email").toString(),
				userEmail.toLowerCase());
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();

		// send reward amount to user Reedemable
		Response sendRewardResponse1 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "16",
				dataSet.get("redeemable_id1"), "", "");
		Assert.assertEquals(sendRewardResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		pageObj.utils().logPass("Send redeemable to the user successfully");

		// get reward id
		String rewardId1 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id1"));

		// send reward amount to user Reedemable
		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "16",
				dataSet.get("redeemable_id2"), "", "");
		Assert.assertEquals(sendRewardResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		pageObj.utils().logPass("Send redeemable to the user successfully");

		// get reward id
		String rewardId2 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id2"));

		// add reward completion to basket
		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAddedForPOSAPIWithExt_Uid(
				dataSet.get("locationkey"), userID, "reward", rewardId1, externalUID);
		Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		pageObj.utils().logPass("add reward completion to basket is successfully");

		// add reward completion to basket
		Response discountBasketResponse1 = pageObj.endpoints().authListDiscountBasketAddedForPOSAPIWithExt_Uid(
				dataSet.get("locationkey"), userID, "reward", rewardId2, externalUID);
		Assert.assertEquals(discountBasketResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		pageObj.utils().logPass("add reward completion to basket is successfully");

		Response batchRedemptionProcessResponseUser = pageObj.endpoints()
				.processBatchRedemptionOfBasketPOSDiscountLookup(dataSet.get("locationkey"), userID, "19", parentMap);
		Assert.assertEquals(batchRedemptionProcessResponseUser.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"POS Discount Look up status code is not 200 ");

		String discountAmount1 = batchRedemptionProcessResponseUser.jsonPath()
				.get("selected_discounts[0].discount_amount").toString();
		Assert.assertEquals(discountAmount1, "5.0",
				"discount amount for offer 1 is not matching and its actual value is  " + discountAmount1);
		pageObj.utils().logPass("discount amount for offer 1 is matching");

	}

	// Shashank
	@Test(description = "SQ-T4297 (1.0) POS discount lookup API>Only Base items>Multiple Offers>Validate the discount amount for target bundle price processing method having multiple rewards in discount basket when Item Qualifiers recycling ON; Stacked discounting ON", groups = { "regression", "dailyrun" })
	@Owner(name = "Shashank Sharma")
	public void T4297_VerifiedPOSDiscontLookupAPIWithMultipleOffers() {
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
		Response sendRewardResponse1 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				dataSet.get("redeemableID_1"), "", "");
		Assert.assertEquals(sendRewardResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send message to user");
		pageObj.utils().logPass("Api2  send reward " + dataSet.get("redeemableID_1") + " to user is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				dataSet.get("redeemableID_2"), "", "");
		Assert.assertEquals(sendRewardResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send message to user");
		pageObj.utils().logPass("Api2  send reward " + dataSet.get("redeemableID_2") + " to user is successful");

		Response rewardResponse = pageObj.endpoints().authListAvailableRewardsNew(token, dataSet.get("client"),
				dataSet.get("secret"));

		String rewardID1 = rewardResponse.jsonPath().getString("id[0]").replace("[", "").replace("]", "");
		String rewardID2 = rewardResponse.jsonPath().getString("id[1]").replace("[", "").replace("]", "");

		pageObj.utils().logPass("Reward id " + rewardID1 + "  and " + rewardID2 + " is generated successfully ");

		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();
		Map<String, String> detailsMap2 = new HashMap<String, String>();
		Map<String, String> detailsMap3 = new HashMap<String, String>();

		// Sandwich|2|7.5|M|22222211|||1.0
		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Sandwich", "1", "7.5", "M", "", "", "1.0", "22222211");
		parentMap.put("Sandwich", detailsMap1);

		// Sandwich|2|7.5|M|22222211|||1.0
		detailsMap2 = pageObj.endpoints().getRecieptDetailsMap("SandwicH", "1", "7.5", "M", "", "", "1.0", "22222211");
		parentMap.put("SandwicH", detailsMap2);

		// French Fries|1|4|M|8001|||2.0
		detailsMap3 = pageObj.endpoints().getRecieptDetailsMap("French Fries", "1", "4", "M", "", "", "1.0", "8001");
		parentMap.put("French Fries", detailsMap3);

		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID1);
		Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		pageObj.utils().logPass(rewardID1 + " rewardid is added to the basket ");

		Response discountBasketResponse2 = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID2);
		Assert.assertEquals(discountBasketResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		pageObj.utils().logPass(rewardID2 + " rewardid is added to the basket ");

		Response batchRedemptionProcessResponseUser1 = pageObj.endpoints()
				.processBatchRedemptionOfBasketPOSDiscountLookup(dataSet.get("locationkey"), userID, "19", parentMap);
		Assert.assertEquals(batchRedemptionProcessResponseUser1.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		pageObj.utils().logPass("Verified the expected qualified item in POS Discount Lookup API response ");

		String discountAmount1 = batchRedemptionProcessResponseUser1.jsonPath()
				.get("selected_discounts[0].discount_amount").toString();
		Assert.assertEquals(discountAmount1, "5.0",
				"discount amount for offer 1 is not matching and its actual value is  " + discountAmount1);
		pageObj.utils().logPass("discount amount for offer 1 is matching");

		String discountAmount2 = batchRedemptionProcessResponseUser1.jsonPath()
				.get("selected_discounts[1].discount_amount").toString();
		Assert.assertEquals(discountAmount2, "2.0",
				"discount amount for offer 2 is not matching and its actual value is  " + discountAmount2);
		pageObj.utils().logPass("discount amount for offer 2 is matching");

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() throws SQLException {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}

}
