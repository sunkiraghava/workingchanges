package com.punchh.server.OMMTest;

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
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class RewardLockingFunctionalityEnableTest {
	private static Logger logger = LogManager.getLogger(RewardLockingFunctionalityEnableTest.class);
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
	private Utilities utils;

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

		prop = Utilities.loadPropertiesFile("segmentBeta.properties");
		sTCName = method.getName();
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==>" + dataSet);
		externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		utils = new Utilities(driver);
	}

	@Test(description = "SQ-T3293 [Batched Redemptions-OMM-T920(609)] Verify if user does not have any active discount_basket -> Discount Lookup API is functional (Reward Locking On)", priority = 0, groups = {
			"regression", "dailyrun" })
	@Owner(name = "Hardik Bhardwaj")
	public void T3293_DiscountLookUp() throws InterruptedException {

		// login to instance
//		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
//		pageObj.instanceDashboardPage().loginToInstance();

		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Navigate to Multiple Redemption Tab and check the flags
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_discount_locking", "check");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_auto_unlock", "check");
		pageObj.cockpitRedemptionsPage().updateTotalNumberOfRedemptionsAllowed("10");
		pageObj.cockpitRedemptionsPage().setAcquisitionType("Offers", "2");
		pageObj.dashboardpage().updateCheckBox();

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("API2 Signup is successful");
		logger.info("API2 Signup is successful");

		// send reward amount to user Redeemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "16",
				dataSet.get("redeemable_id"), "", "");

		utils.logPass("Send redeemable to the user successfully");

		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get().pass("Api2  send reward amount to user is successful");

		// get reward id
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id"));

		utils.logPass("Reward id " + rewardId + " is generated successfully ");

		// POS Discount Lookup Api-1

		Response discountLookupResponse0 = pageObj.endpoints().discountLookUpPosApi(dataSet.get("location_key"), userID,
				"", "30", "");
		Assert.assertEquals(discountLookupResponse0.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Discount Lookup Api ");
		String externalUidResponse2 = discountLookupResponse0.jsonPath().getString("external_uid");
		String locked2 = discountLookupResponse0.jsonPath().getString("locked");
		Assert.assertTrue(
				pageObj.cockpitRedemptionsPage().resultOfUserLookUpPosAPI1(externalUidResponse2, locked2, null, null));
		String jsonObjectString = discountLookupResponse0.asString();
		JSONObject finalResponse = new JSONObject(jsonObjectString);
		Boolean unselected_discountsAvailability = finalResponse.has("unselected_discounts");
		Assert.assertEquals(true, unselected_discountsAvailability, "unselected_discounts not found");
		utils.logPass("unselected_discounts found");

		utils.logPass("unselected_discounts ->  Returns the response of applicable rewards");

		// POS Discount Lookup Api-1

		Response discountLookupResponse = pageObj.endpoints().discountLookUpPosApi(dataSet.get("location_key"), userID,
				dataSet.get("item_id"), "30", "");
		Assert.assertEquals(discountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Discount Lookup Api ");
		String externalUidResponse = discountLookupResponse.jsonPath().getString("external_uid");
		String locked = discountLookupResponse.jsonPath().getString("locked");
		Assert.assertTrue(
				pageObj.cockpitRedemptionsPage().resultOfUserLookUpPosAPI1(externalUidResponse, locked, null, null));
		String jsonObjectString1 = discountLookupResponse.asString();
		JSONObject finalResponse1 = new JSONObject(jsonObjectString1);
		Boolean selected_discountsAvailability = finalResponse1.has("selected_discounts");
		Assert.assertEquals(true, selected_discountsAvailability, "selected_discounts not found");
		utils.logPass("shows selected response");

		// POS Discount Lookup Api-1

		List<Object> obj2 = new ArrayList<Object>();
		Response discountLookupResponse1 = pageObj.endpoints().discountLookUpPosApi(dataSet.get("location_key"), userID,
				dataSet.get("item_id"), "30", "");
		Assert.assertEquals(discountLookupResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Discount Lookup Api ");
		String externalUidResponse1 = discountLookupResponse1.jsonPath().getString("external_uid");
		String locked1 = discountLookupResponse1.jsonPath().getString("locked");
		Assert.assertTrue(
				pageObj.cockpitRedemptionsPage().resultOfUserLookUpPosAPI1(externalUidResponse1, locked1, null, null));
		obj2 = discountLookupResponse1.jsonPath().getList("unselected_discounts");
		for (int i = 0; i < obj2.size(); i++) {
			discount_details2 = discountLookupResponse1.jsonPath()
					.getString("unselected_discounts[" + i + "].discount_details.name");

			if (discount_details2.equalsIgnoreCase("Automation 12003")) {
				break;
			}
		}
		Assert.assertEquals(discount_details2, "Automation - 12003",
				"unselected_discount -> not Returns the response of applicable rewards / coupons");
		logger.info("unselected_discounts -> Returns the response of applicable rewards / coupons");
		TestListeners.extentTest.get()
				.pass("unselected_discounts -> Returns the response of applicable rewards / coupons");

		// POS Discount Lookup Api-1

		Response discountLookupResponse2 = pageObj.endpoints().discountLookUpPosApi(dataSet.get("location_key"), userID,
				"1234", "30", "");
		Assert.assertEquals(discountLookupResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Discount Lookup Api ");
		String externalUidResponse3 = discountLookupResponse2.jsonPath().getString("external_uid");
		String locked3 = discountLookupResponse2.jsonPath().getString("locked");
		Assert.assertTrue(
				pageObj.cockpitRedemptionsPage().resultOfUserLookUpPosAPI1(externalUidResponse3, locked3, null, null));
		String jsonObjectString4 = discountLookupResponse2.asString();
		JSONObject finalResponse4 = new JSONObject(jsonObjectString4);
		Boolean unselected_discountsAvailability4 = finalResponse4.has("unselected_discounts");
		Assert.assertEquals(true, unselected_discountsAvailability4, "unselected_discounts not found");
		utils.logPass("shows unselected response");

	}

	// Deprecated https://punchhdev.atlassian.net/browse/OMM-1256
	@SuppressWarnings("static-access")
//    @Test(
//        description = "SQ-T3296 [Batched Redemptions-OMM-T923(609)] Verify if user does not have any active discount_basket and has available rewards -> Auto Select API is functional and discount_basket gets created (Reward Locking On/Off)",
//        priority = 1, dependsOnMethods = "T3293_DiscountLookUp",
//        groups = {"regression", "dailyrun"})
	public void T3296_AutoSelectApiStep3_4() throws Exception {

		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("API2 Signup is successful");
		logger.info("API2 Signup is successful");

		String query = "DELETE FROM rewards WHERE user_id = '" + userID + "'";
		int rs = DBUtils.executeUpdateQuery(env, query);
		Assert.assertEquals(rs, 0);

		// Deprecated https://punchhdev.atlassian.net/browse/OMM-1256
		// POS Auto Unlock
		Response autoUnlockResponse1 = pageObj.endpoints().autoSelectPosApi(userID, "30", "1", "12003", externalUID,
				dataSet.get("location_key"));
		Assert.assertEquals(autoUnlockResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Auto Unlock ");
		TestListeners.extentTest.get().pass("POS Auto Unlock Api is successful");

		String query1 = "Select external_uid from discount_baskets where user_id ='" + userID + "'";

		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query1, "external_uid"); // dbUtils.getValueFromColumn(query1,
																														// "external_uid");
		Assert.assertEquals(expColValue, externalUID,
				"Value is not present at external_uid column in discount basket ");
		logger.info("external_uid sent -> Empty discount_basket gets created and external_uid is saved in DB ");
		TestListeners.extentTest.get()
				.pass("external_uid sent -> Empty discount_basket gets created and external_uid is saved in DB ");

		// User SignUp using API
		String userEmail1 = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse1 = pageObj.endpoints().Api2SignUp(userEmail1, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse1, "API 2 user signup");
		String userID1 = signUpResponse1.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("API2 Signup is successful");
		logger.info("API2 Signup is successful");

		String query2 = "DELETE FROM rewards WHERE user_id = '" + userID1 + "'";
		int rs2 = DBUtils.executeUpdateQuery(env, query2); // stmt.executeUpdate(query2);
		Assert.assertEquals(rs2, 0);

		// Deprecated https://punchhdev.atlassian.net/browse/OMM-1256
		// POS Auto Unlock
		Response autoUnlockResponse = pageObj.endpoints().autoSelectPosApi(userID1, "30", "1", "12003", "",
				dataSet.get("location_key"));
		Assert.assertEquals(autoUnlockResponse.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST, "Status code 400 did not match with Auto Unlock ");
		Assert.assertEquals(autoUnlockResponse.jsonPath().getString("error"),
				"Required parameter missing or the value is empty: external_uid");
		TestListeners.extentTest.get().pass("POS Auto Select Api is successful");
		logger.info("POS Auto Select Api is successful");
		logger.info(
				"external_uid not sent -> API gives error -> error:- Required parameter missing or the value is empty: external_uid ");
		TestListeners.extentTest.get().pass(
				"external_uid not sent -> API gives error -> error:- Required parameter missing or the value is empty: external_uid ");

		// User SignUp using API
		String userEmail2 = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse2 = pageObj.endpoints().Api2SignUp(userEmail2, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse2, "API 2 user signup");
		String token2 = signUpResponse2.jsonPath().get("access_token.token").toString();

		String userID2 = signUpResponse2.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("API2 Signup is successful");
		logger.info("API2 Signup is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID2, dataSet.get("apiKey"), "16",
				dataSet.get("redeemable_id"), "", "");

		utils.logPass("Send redeemable to the user successfully");

		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get().pass("Api2  send reward amount to user is successful");

		// get reward id
		rewardId = pageObj.redeemablesPage().getRewardId(token2, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id"));

		utils.logPass("Reward id " + rewardId + " is generated successfully ");

		// POS Auto Unlock
		Response autoUnlockResponse3 = pageObj.endpoints().autoSelectPosApi(userID2, "30", "1", "12003", externalUID,
				dataSet.get("location_key"));
		Assert.assertEquals(autoUnlockResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Auto Unlock ");
		TestListeners.extentTest.get().pass("POS Auto Select Api is successful");
		logger.info("POS Auto Select Api is successful");

		String query4 = "Select external_uid from discount_baskets where user_id ='" + userID2 + "'";
		String expColValue4 = DBUtils.executeQueryAndGetColumnValue(env, query4, "external_uid"); // dbUtils.getValueFromColumn(query4,
																														// "external_uid");
		Assert.assertEquals(expColValue4, externalUID,
				"Value is not present at external_uid column in discount basket ");
		logger.info("external_uid not sent -> Empty discount_basket gets created and external_uid remains NULL in DB ");
		TestListeners.extentTest.get().pass(
				"external_uid not sent -> Empty discount_basket gets created and external_uid remains NULL in DB ");

		// User SignUp using API
		String userEmail3 = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse3 = pageObj.endpoints().Api2SignUp(userEmail3, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse3, "API 2 user signup");
		String token3 = signUpResponse3.jsonPath().get("access_token.token").toString();

		String userID3 = signUpResponse3.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("API2 Signup is successful");
		logger.info("API2 Signup is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse3 = pageObj.endpoints().sendMessageToUser(userID3, dataSet.get("apiKey"), "16",
				dataSet.get("redeemable_id"), "", "");

		utils.logPass("Send redeemable to the user successfully");

		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse3.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get().pass("Api2  send reward amount to user is successful");

		// get reward id
		rewardId1 = pageObj.redeemablesPage().getRewardId(token3, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id"));

		utils.logPass("Reward id " + rewardId1 + " is generated successfully ");

		// Deprecated https://punchhdev.atlassian.net/browse/OMM-1256
		// POS Auto Unlock
		Response autoUnlockResponse4 = pageObj.endpoints().autoSelectPosApi(userID3, "30", "1", "12003", "",
				dataSet.get("location_key"));
		Assert.assertEquals(autoUnlockResponse4.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Status code 400 did not match with Auto Unlock ");
		Assert.assertEquals(autoUnlockResponse4.jsonPath().getString("error"),
				"Required parameter missing or the value is empty: external_uid");
		TestListeners.extentTest.get().pass("POS Auto Select Api is successful");
		logger.info("POS Auto Select Api is successful");

		logger.info(
				"external_uid not sent -> API gives error -> error: Required parameter missing or the value is empty: external_uid ");
		TestListeners.extentTest.get().pass(
				"external_uid not sent -> API gives error -> error: Required parameter missing or the value is empty: external_uid");

//		DBUtils.closeConnection();

	}

	@SuppressWarnings({ "static-access", "unused" })
	@Test(description = "SQ-T3288 [Batched Redemptions-OMM-T909(594)] Verify if \"Enable Reward Locking\" is On \"external_uid\" is not mandatory in User Lookup API)", priority = 1, groups = {
			"regression", "dailyrun" })
	@Owner(name = "Hardik Bhardwaj")
	public void T3288_UserLookupAPI() throws Exception {

		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("API2 Signup is successful");
		logger.info("API2 Signup is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "16",
				dataSet.get("redeemable_id"), "", "");

		utils.logPass("Send redeemable to the user successfully");

		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get().pass("Api2  send reward amount to user is successful");

		// get reward id
		rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id"));

		utils.logPass("Reward id " + rewardId + " is generated successfully ");

		// AUTH Add Discount to Basket
		Response discountBasketResponse = pageObj.endpoints().addDiscountToBasketAUTH(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId, externalUID);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponse.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		TestListeners.extentTest.get().pass("AUTH add discount to basket is successful");

		String query = "UPDATE discount_baskets SET external_uid = NULL WHERE user_id='" + userID + "'";
		int rs = DBUtils.executeUpdateQuery(env, query); // stmt.executeUpdate(query);
//		Assert.assertEquals(rs, 1);

		String query1 = "UPDATE discount_baskets SET locked_at = NULL WHERE user_id='" + userID + "'";
		int rs1 = DBUtils.executeUpdateQuery(env, query1); // stmt.executeUpdate(query1);
//		Assert.assertEquals(rs1, 1);

		// POS user lookUp
//				Thread.sleep(10000);
		Response userLookupResponse = pageObj.endpoints().userLookupPosApiWithoutExt_uid("email", userEmail,
				dataSet.get("location_key"));
		Assert.assertEquals(userLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with POS user lookUp ");
		TestListeners.extentTest.get().pass("POS user lookU is successful");
		String externalUidResponse = userLookupResponse.jsonPath().getString("external_uid");
		String locked = userLookupResponse.jsonPath().getString("locked");
		Assert.assertTrue(
				pageObj.cockpitRedemptionsPage().resultOfUserLookUpPosAPI1(externalUidResponse, locked, null, "false"));
		TestListeners.extentTest.get().pass("POS user lookUp is successful and external_uid not sent");
		logger.info("POS user lookUp is successful and external_uid not sent");

		String query2 = "UPDATE discount_baskets SET external_uid = NULL WHERE user_id='" + userID + "'";
		int rs2 = DBUtils.executeUpdateQuery(env, query2); // stmt.executeUpdate(query2);
//		Assert.assertEquals(rs2, 1);

		String query3 = "UPDATE discount_baskets SET locked_at = NULL WHERE user_id='" + userID + "'";
		int rs3 = DBUtils.executeUpdateQuery(env, query3); // stmt.executeUpdate(query3);
//		Assert.assertEquals(rs3, 1);

		// POS user lookUp
//				Thread.sleep(10000);
		Response userLookupResponse1 = pageObj.endpoints().userLookupPosApi("email", userEmail,
				dataSet.get("location_key"), "");
		Assert.assertEquals(userLookupResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with POS user lookUp ");
		TestListeners.extentTest.get().pass("POS user lookU is successful");
		String externalUidResponse1 = userLookupResponse1.jsonPath().getString("external_uid");
		String locked1 = userLookupResponse1.jsonPath().getString("locked");
		Assert.assertTrue(pageObj.cockpitRedemptionsPage().resultOfUserLookUpPosAPI1(externalUidResponse1, locked1,
				null, "false"));
		TestListeners.extentTest.get().pass("POS user lookUp is successful and external_uid\": \"\" sent");
		logger.info("POS user lookUp is successful and external_uid\": \"\" sent");

		String query4 = "UPDATE discount_baskets SET external_uid = NULL WHERE user_id='" + userID + "'";
		@SuppressWarnings("unused")
		int rs4 = DBUtils.executeUpdateQuery(env, query4); // stmt.executeUpdate(query4);
//		Assert.assertEquals(rs4, 1);

		String query5 = "UPDATE discount_baskets SET locked_at = NULL WHERE user_id='" + userID + "'";
		int rs5 = DBUtils.executeUpdateQuery(env, query5); // stmt.executeUpdate(query5);
//		Assert.assertEquals(rs5, 1);

		// Deprecated https://punchhdev.atlassian.net/browse/OMM-1256
//		// POS user lookUp
//				Thread.sleep(10000);
//		Response userLookupResponse2 = pageObj.endpoints().userLookupPosApi("email", userEmail,
//            dataSet.get("location_key"), externalUID);
//		Assert.assertEquals(userLookupResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
//				"Status code 200 did not match with POS user lookUp ");
//		TestListeners.extentTest.get().pass("POS user lookU is successful");
//		String externalUidResponse2 = userLookupResponse2.jsonPath().getString("external_uid");
//		String locked2 = userLookupResponse2.jsonPath().get("locked").toString();
//		Assert.assertTrue(pageObj.cockpitRedemptionsPage().resultOfUserLookUpPosAPI(externalUidResponse2, locked2,
//				externalUID, "true"));
//		TestListeners.extentTest.get().pass("POS user lookUp is successful and external_uid sent");
//		logger.info("POS user lookUp is successful and external_uid sent");

//		DBUtils.closeConnection();

	}

	@Test(description = "SQ-T3667 [Batched Redemptions] Verify if \"Set redemption processing priority by Acquisition Type\" is set to \"Offers -> 2\" and user's discount_basket contains an unredeemed reward, then also user is able to add non-loyalty reward based on Acquisition Type limit)", priority = 2, groups = {
			"regression", "dailyrun" })
	@Owner(name = "Hardik Bhardwaj")
	public void T3667_AcquisitionTypeLimit() throws Exception {

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("API2 Signup is successful");
		logger.info("API2 Signup is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "16",
				dataSet.get("redeemable_id"), "", "");

		utils.logPass("Send redeemable to the user successfully");

		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get().pass("Api2  send reward amount to user is successful");

		// get reward id
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id"));

		logger.info("Reward id 1 " + rewardId + " is generated successfully ");
		TestListeners.extentTest.get().pass("Reward id 1 " + rewardId + " is generated successfully ");

		// AUTH Add Discount to Basket
		Response discountBasketResponse = pageObj.endpoints().addDiscountToBasketAUTH(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId, externalUID);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponse.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		TestListeners.extentTest.get().pass("AUTH add discount to basket is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse1 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "16",
				dataSet.get("redeemable_id"), "", "");

		utils.logPass("Send redeemable to the user successfully");

		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse1.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get().pass("Api2  send reward amount to user is successful");

		// get reward id
		rewardId1 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id"));

		logger.info("Reward id 2 " + rewardId1 + " is generated successfully ");
		TestListeners.extentTest.get().pass("Reward id 2 " + rewardId1 + " is generated successfully ");

		// send reward amount to user Reedemable
		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "16",
				dataSet.get("redeemable_id"), "", "");

		utils.logPass("Send redeemable to the user successfully");

		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse2.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get().pass("Api2  send reward amount to user is successful");

		// get reward id
		rewardId2 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id"));

		logger.info("Reward id 3 " + rewardId2 + " is generated successfully ");
		TestListeners.extentTest.get().pass("Reward id 3 " + rewardId2 + " is generated successfully ");

		// send reward amount to user Reedemable
		Response sendRewardResponse3 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "16",
				dataSet.get("redeemable_id"), "", "");

		utils.logPass("Send redeemable to the user successfully");

		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse3.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get().pass("Api2  send reward amount to user is successful");

		// get reward id
		String rewardId3 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id"));

		logger.info("Reward id 4 " + rewardId3 + " is generated successfully ");
		TestListeners.extentTest.get().pass("Reward id 4 " + rewardId3 + " is generated successfully ");

		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();

		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Pizza1", "1", "10", "M", "10", "999", "1",
				dataSet.get("item_id"));
		parentMap.put("Pizza1", detailsMap1);

		// Auth Auto select API
		Response redemptionResponse = pageObj.endpoints().authAutoSelectAPI(dataSet.get("client"),
				dataSet.get("secret"), token, "14", externalUID, parentMap);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, redemptionResponse.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		TestListeners.extentTest.get().pass("AUTH add discount to basket is successful");

	}

	// Deprecated https://punchhdev.atlassian.net/browse/OMM-1256
//	@Test(description = "SQ-T5933 Verify the external_uid is restricted in the Mobile API flow while applying Reward Locking.", priority = 0)
	public void T5933_SelectDiscountBasket() throws InterruptedException {

		// login to instance
//		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
//		pageObj.instanceDashboardPage().loginToInstance();

		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Navigate to Multiple Redemption Tab and check the flags
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_discount_locking", "check");
		pageObj.dashboardpage().updateCheckBox();

		String externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("API2 Signup is successful");
		logger.info("API2 Signup is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse1 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "16",
				dataSet.get("redeemable_id"), "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse1.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		utils.logPass("Send redeemable to the user successfully");

		// get reward id
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id"));
		utils.logPass("Reward id " + rewardId + " is generated successfully ");

		// send reward amount to user Reedemable
		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "16",
				dataSet.get("redeemable_id"), "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse2.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		utils.logPass("Send redeemable to the user successfully");

		// get reward id
		String rewardId2 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id"));
		logger.info("Reward id " + rewardId2 + " is generated successfully ");
		TestListeners.extentTest.get().pass("Reward id " + rewardId2 + " is generated successfully ");

		// POS Api Adding Reward into discount basket
		Response discountBasketResponse1 = pageObj.endpoints().authListDiscountBasketAddedForPOSAPIWithExt_Uid(
				dataSet.get("location_key"), userID, "reward", rewardId2, externalUID);
		Assert.assertEquals(discountBasketResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for POS add reward completion to basket");
		TestListeners.extentTest.get().pass("POS Api Adding Reward into discount basket is successful");
		logger.info("POS Api Adding Reward into discount basket is successful");

		// Secure Api Adding Reward into discount basket
		Response discountBasketResponse3 = pageObj.endpoints().secureApiDiscountBasketAdded(token,
				dataSet.get("client"), dataSet.get("secret"), "reward", rewardId, externalUID);
		Assert.assertEquals(discountBasketResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for Secure Api add reward completion to basket");
		Assert.assertEquals(discountBasketResponse3.jsonPath().getString("errors[0]"),
				"Unable to find valid discount basket.");
		TestListeners.extentTest.get().pass(
				"Verified that for Secure Api Adding Reward into discount basket, User should not be able to access mobile api with external_uid and error should appear as Unable to find valid discount basket");
		logger.info(
				"Secure Api Adding Reward into discount basket, User should not be able to access mobile api with external_uid and error should appear as Unable to find valid discount basket");

		// Secure Api remove discount basket
		String expDiscountBasketItemId1 = Integer.toString(Utilities.getRandomNoFromRange(1000000, 5000000));
		Response deleteBasketResponse = pageObj.endpoints().removeDiscountBasketExtUIDSecureAPI(token,
				dataSet.get("client"), dataSet.get("secret"), expDiscountBasketItemId1, externalUID);
		Assert.assertEquals(deleteBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for Secure Api remove reward from basket");
		Assert.assertEquals(deleteBasketResponse.jsonPath().getString("errors[0]"),
				"Unable to find valid discount basket.");
		logger.info(
				"Verified that for Secure API remove discount from basket, User should not be able to access mobile api with external_uid and error should appear as Unable to find valid discount basket");
		TestListeners.extentTest.get().pass(
				"Verified that for Secure API remove discount from basket, User should not be able to access mobile api with external_uid and error should appear as Unable to find valid discount basket");

		// Adding Reward into discount basket
		Response discountBasketResponse = pageObj.endpoints().addDiscountToBasketWithExtIdAPI2(token,
				dataSet.get("client"), dataSet.get("secret"), "reward", rewardId, externalUID);
		Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for Mobile Api add reward in basket");
		Assert.assertEquals(discountBasketResponse.jsonPath().getString("errors.invalid_basket"),
				"Unable to find valid discount basket.");
		logger.info(
				"Verified that for Mobile API add discount in basket, User should not be able to access mobile api with external_uid and error should appear as Unable to find valid discount basket");
		TestListeners.extentTest.get().pass(
				"Verified that for Mobile API add discount in basket, User should not be able to access mobile api with external_uid and error should appear as Unable to find valid discount basket");

		// API2 remove discount from basket
		String expDiscountBasketItemId = Integer.toString(Utilities.getRandomNoFromRange(1000000, 5000000));
		Response deleteBasketResponse1 = pageObj.endpoints().deleteDiscountToBasketWithExtUidAPI2(token,
				dataSet.get("client"), dataSet.get("secret"), expDiscountBasketItemId, externalUID);
		Assert.assertEquals(deleteBasketResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for Mobile Api remove reward from basket");
		Assert.assertEquals(deleteBasketResponse1.jsonPath().getString("errors.invalid_basket"),
				"Unable to find valid discount basket.");
		logger.info(
				"Verified that for Mobile API remove discount from basket, User should not be able to access mobile api with external_uid and error should appear as Unable to find valid discount basket");
		TestListeners.extentTest.get().pass(
				"Verified that for Mobile API remove discount from basket, User should not be able to access mobile api with external_uid and error should appear as Unable to find valid discount basket");
	}

	@SuppressWarnings("static-access")
	@Test(description = "SQ-T4440 Verify Enable Auto Redemption flag is visible in UI when allow_multiple_redemptions -> true and Enable Auto-redemption -> On and Auto redemption discounts -> Offers, Subscriptions selected on create / update redeemable", priority = 3)
	@Owner(name = "Hardik Bhardwaj")
	public void T4440_verifyAutoRedemptionFlag() throws Exception {

		String redeemableName = "AutoRedeemable_" + CreateDateTime.getTimeDateString();
//		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
//		pageObj.instanceDashboardPage().loginToInstance();

		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().checkUncheckFlagCockpitDashboard("Enable Subscriptions?", "check");
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// Navigate to Multiple Redeemables tab
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.dashboardpage().checkUncheckFlagCockpitDashboard("Enable Auto-redemption", "check");
		pageObj.cockpitRedemptionsPage().setAutoRedemptionDiscounts("Subscription", "Select");
		pageObj.cockpitRedemptionsPage().setAutoRedemptionDiscounts("Offers", "Select");
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// Navigate to Redeemables tab
		pageObj.menupage().navigateToSubMenuItem("Settings", "Redeemables");
		pageObj.redeemablePage().createRedeemable(redeemableName);
		pageObj.dashboardpage().checkUncheckToggle("Enable Auto Redemption", "ON");
		boolean result = utils.checkFlagVisiblityOnUi("Enable Auto Redemption");
		Assert.assertTrue(result, "Enable Auto Redemption toggle button should be visible when the flag is ON");
		logger.info(
				"Verified that Enable Auto Redemption toggle button is visible when flag is ON while creating new Redeemable");
		TestListeners.extentTest.get().pass(
				"Verified that Enable Auto Redemption toggle button is visible when flag is ON while creating new Redeemable");

		pageObj.redeemablePage().selectRecieptRule("1");
		pageObj.redeemablePage().allowRedeemableToRunIndefinitely();
		pageObj.redeemablePage().clickOnFinishButton();
		pageObj.guestTimelinePage().successOrErrorConfirmationMessage("Redeemable successfully saved.");
		logger.info("Redeemable " + redeemableName + " created successfully");
		TestListeners.extentTest.get().info("Redeemable " + redeemableName + " created successfully");

		boolean result2 = pageObj.redeemablePage().verifyEnableAutoRedemptionToggleDisplayed(redeemableName,
				"Enable Auto Redemption");
		Assert.assertTrue(result2, "Enable Auto Redemption toggle button is not visible for existing Redeemable");
		logger.info("Verified that Enable Auto Redemption toggle button is visible for existing Redeemable");
		TestListeners.extentTest.get()
				.pass("Verified that Enable Auto Redemption toggle button is visible for existing Redeemable");

		pageObj.menupage().navigateToSubMenuItem("Settings", "Redeemables");
		pageObj.redeemablePage().selectRedeemableEllipsisOption(redeemableName, "Audit Log");
		String logValue = pageObj.redeemablePage().getAuditLogOfRedeemable("Auto Applicable", "2");
		Assert.assertEquals(logValue, "true",
				"Audit log value for Enable Auto Redemption is not true for Redeemable " + redeemableName);
		logger.info(
				"Verified that Audit log value for Enable Auto Redemption is true for Redeemable " + redeemableName);
		TestListeners.extentTest.get().pass(
				"Verified that Audit log value for Enable Auto Redemption is true for Redeemable " + redeemableName);

		String query = "Select `auto_applicable` from `redeemables` where name = '" + redeemableName + "';";
		String expColValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query,
				"auto_applicable", 1);
		Assert.assertEquals(expColValue, "1",
				"auto_applicable value in redeemables table in DB is not 1 for Redeemable " + redeemableName);
		logger.info(
				"Verified that auto_applicable value in redeemables table in DB is 1 for Redeemable " + redeemableName);
		TestListeners.extentTest.get().pass(
				"Verified that auto_applicable value in redeemables table in DB is 1 for Redeemable " + redeemableName);

		pageObj.menupage().navigateToSubMenuItem("Settings", "Redeemables");
		pageObj.redeemablePage().searchRedeemable(redeemableName);
		pageObj.redeemablePage().deleteRedeemable(redeemableName);
		logger.info("Redeemable is deleted");
		TestListeners.extentTest.get().info("Redeemable is deleted");
	}

	@Test(description = "SQ-T3287 [Batched Redemptions-OMM-T832(564)] Verify Short Prompt or Standard Prompt in Discount Lookup API response",groups = {"nonNightly" }, priority = 4)
	@Owner(name = "Hardik Bhardwaj")
	public void T3287_verifyShortAndStandardPrompt() throws Exception {

		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_subscriptions", "check");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_allow_multiple_redemptions", "check");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_coupons", "check");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_promos", "check");
		pageObj.dashboardpage().updateCheckBox();

		// Step - 1 discount_details under selected_discounts returns configured value
		// of Short Prompt and Standard Prompt for coupon
		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("API2 Signup is successful");
		logger.info("API2 Signup is successful");

		// POS Api Adding Reward into discount basket
		Response discountBasketResponse1 = pageObj.endpoints().authListDiscountBasketAddedForPOSAPIWithExt_Uid(
				dataSet.get("location_key"), userID, "redemption_code",
				dataSet.get("couponCodeForShortAndStandardPromt"), externalUID);
		Assert.assertEquals(discountBasketResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for POS add reward completion to basket");
		TestListeners.extentTest.get().pass("POS Api Adding Coupon Redemption Code into discount basket is successful");
		logger.info("POS Api Adding Coupon Redemption Code into discount basket is successful");

		// POS Discount Lookup Api
		Response discountLookupResponse0 = pageObj.endpoints().discountLookUpPosApi(dataSet.get("location_key"), userID,
				"1010", "30", externalUID);
		Assert.assertEquals(discountLookupResponse0.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Discount Lookup Api ");
		String shortPrompt = discountLookupResponse0.jsonPath()
				.get("selected_discounts[0].discount_details.prompt_text_short").toString();
		String longPrompt = discountLookupResponse0.jsonPath()
				.get("selected_discounts[0].discount_details.prompt_text_long").toString();
		Assert.assertEquals(shortPrompt, "AutoShortPrompt",
				"Short Prompt did not match with Discount Lookup Api prompt_text_short");
		Assert.assertEquals(longPrompt, "Automation Standard Prompt",
				"Long Prompt did not match with Discount Lookup Api prompt_text_long");
		logger.info(
				"Verified that in Coupon redemption code, discount_details under selected_discounts returns configured value of Short Prompt and Standard Prompt successfully for POS Discount Lookup Api");
		TestListeners.extentTest.get().pass(
				"Verified that in Coupon redemption code, discount_details under selected_discounts returns configured value of Short Prompt and Standard Prompt successfully for POS Discount Lookup Api");

		// Step - 2 discount_details under selected_discounts returns blank value of
		// Short Prompt and Standard Prompt for coupon
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse2 = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse2, "API 2 user signup");
		String userID2 = signUpResponse2.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("API2 Signup is successful");
		logger.info("API2 Signup is successful");

		// POS Api Adding Reward into discount basket
		Response discountBasketResponse2 = pageObj.endpoints().authListDiscountBasketAddedForPOSAPIWithExt_Uid(
				dataSet.get("location_key"), userID2, "redemption_code",
				dataSet.get("couponCodeForBlankShortAndStandardPromt"), externalUID);
		Assert.assertEquals(discountBasketResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for POS add reward completion to basket");
		TestListeners.extentTest.get().pass("POS Api Adding Coupon Redemption Code into discount basket is successful");
		logger.info("POS Api Adding Coupon Redemption Code into discount basket is successful");

		// POS Discount Lookup Api
		Response discountLookupResponse2 = pageObj.endpoints().discountLookUpPosApi(dataSet.get("location_key"),
				userID2, "1020", "30", externalUID);
		Assert.assertEquals(discountLookupResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Discount Lookup Api ");
		String shortPrompt2 = discountLookupResponse2.jsonPath()
				.get("selected_discounts[0].discount_details.prompt_text_short").toString();
		String longPrompt2 = discountLookupResponse2.jsonPath()
				.get("selected_discounts[0].discount_details.prompt_text_long").toString();
		Assert.assertEquals(shortPrompt2, "", "Short Prompt did not match with Discount Lookup Api prompt_text_short");
		Assert.assertEquals(longPrompt2, "", "Long Prompt did not match with Discount Lookup Api prompt_text_long");
		logger.info(
				"Verified that in Coupon redemption code, discount_details under selected_discounts returns blank value of Short Prompt and Standard Prompt successfully for POS Discount Lookup Api");
		TestListeners.extentTest.get().pass(
				"Verified that in Coupon redemption code, discount_details under selected_discounts returns blank value of Short Prompt and Standard Prompt successfully for POS Discount Lookup Api");

		// Step - 3 discount_details under selected_discounts returns NULL value of
		// Short Prompt and Standard Prompt for coupon
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse3 = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse3, "API 2 user signup");
		String userID3 = signUpResponse3.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("API2 Signup is successful");
		logger.info("API2 Signup is successful");

		// POS Api Adding Reward into discount basket
		Response discountBasketResponse3 = pageObj.endpoints().authListDiscountBasketAddedForPOSAPIWithExt_Uid(
				dataSet.get("location_key"), userID3, "redemption_code",
				dataSet.get("couponCodeForNullShortAndStandardPromt"), externalUID);
		Assert.assertEquals(discountBasketResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for POS add reward completion to basket");
		TestListeners.extentTest.get().pass("POS Api Adding Coupon Redemption Code into discount basket is successful");
		logger.info("POS Api Adding Coupon Redemption Code into discount basket is successful");

		// POS Discount Lookup Api
		Response discountLookupResponse3 = pageObj.endpoints().discountLookUpPosApi(dataSet.get("location_key"),
				userID3, "24", "30", externalUID);
		Assert.assertEquals(discountLookupResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Discount Lookup Api ");
		String shortPrompt3 = discountLookupResponse3.jsonPath()
				.get("selected_discounts[0].discount_details.prompt_text_short");
		String longPrompt3 = discountLookupResponse3.jsonPath()
				.get("selected_discounts[0].discount_details.prompt_text_long");
		Assert.assertEquals(shortPrompt3, null,
				"Short Prompt did not match with Discount Lookup Api prompt_text_short");
		Assert.assertEquals(longPrompt3, null, "Long Prompt did not match with Discount Lookup Api prompt_text_long");
		logger.info(
				"Verified that in Coupon redemption code, discount_details under selected_discounts returns NULL value of Short Prompt and Standard Prompt successfully for POS Discount Lookup Api");
		TestListeners.extentTest.get().pass(
				"Verified that in Coupon redemption code, discount_details under selected_discounts returns NULL value of Short Prompt and Standard Prompt successfully for POS Discount Lookup Api");

		String endDateTime = CreateDateTime.getTomorrowDate() + " 10:00 AM";
		// Step - 16 discount_details under selected_discounts returns configured value
		// of Short Prompt and Standard Prompt for subscription

		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse4 = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse4, "API 2 user signup");
		String userID4 = signUpResponse4.jsonPath().get("user.user_id").toString();
		String token4 = signUpResponse4.jsonPath().get("access_token.token").toString();
		Assert.assertEquals(signUpResponse4.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("API2 Signup is successful");
		logger.info("API2 Signup is successful");

		// subscription purchase api 2
		Response purchaseSubscriptionresponse = pageObj.endpoints().Api2SubscriptionPurchase(token4,
				dataSet.get("PlanIDForShortAndStandardPromt"), dataSet.get("client"), dataSet.get("secret"), "2",
				endDateTime);
		Assert.assertEquals(purchaseSubscriptionresponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		int subscription_id = Integer
				.parseInt(purchaseSubscriptionresponse.jsonPath().get("subscription_id").toString());
		logger.info(userEmail + " purchased " + subscription_id + " Plan id = "
				+ dataSet.get("PlanIDForShortAndStandardPromt"));

		// POS Api Adding Reward into discount basket
		Response discountBasketResponse4 = pageObj.endpoints().authListDiscountBasketAddedForPOSAPIWithExt_Uid(
				dataSet.get("location_key"), userID4, "subscription", subscription_id + "", externalUID);
		Assert.assertEquals(discountBasketResponse4.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for POS add reward completion to basket");
		TestListeners.extentTest.get().pass("POS Api Adding subscription into discount basket is successful");
		logger.info("POS Api Adding subscription into discount basket is successful");

		// POS Discount Lookup Api
		Response discountLookupResponse4 = pageObj.endpoints().discountLookUpPosApi(dataSet.get("location_key"),
				userID4, "110011", "30", externalUID);
		Assert.assertEquals(discountLookupResponse4.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Discount Lookup Api ");
		String shortPrompt4 = discountLookupResponse4.jsonPath()
				.get("selected_discounts[0].discount_details.prompt_text_short");
		String longPrompt4 = discountLookupResponse4.jsonPath()
				.get("selected_discounts[0].discount_details.prompt_text_long");
		Assert.assertEquals(shortPrompt4, "AutoShortPrompt",
				"Short Prompt did not match with Discount Lookup Api prompt_text_short");
		Assert.assertEquals(longPrompt4, "Automation Standard Prompt",
				"Long Prompt did not match with Discount Lookup Api prompt_text_long");
		logger.info(
				"Verified that in subscription, discount_details under selected_discounts returns configured value of Short Prompt and Standard Prompt successfully for POS Discount Lookup Api");
		TestListeners.extentTest.get().pass(
				"Verified that in subscription, discount_details under selected_discounts returns configured value of Short Prompt and Standard Prompt successfully for POS Discount Lookup Api");

		// Step - 17 discount_details under selected_discounts returns Blank value
		// of Short Prompt and Standard Prompt for subscription

		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse5 = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse5, "API 2 user signup");
		String userID5 = signUpResponse5.jsonPath().get("user.user_id").toString();
		String token5 = signUpResponse5.jsonPath().get("access_token.token").toString();
		Assert.assertEquals(signUpResponse5.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("API2 Signup is successful");
		logger.info("API2 Signup is successful");

		// subscription purchase api 2
		Response purchaseSubscriptionresponse2 = pageObj.endpoints().Api2SubscriptionPurchase(token5,
				dataSet.get("PlanIDForBlankShortAndStandardPromt"), dataSet.get("client"), dataSet.get("secret"), "2",
				endDateTime);
		Assert.assertEquals(purchaseSubscriptionresponse2.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		int subscription_id2 = Integer
				.parseInt(purchaseSubscriptionresponse2.jsonPath().get("subscription_id").toString());
		logger.info(userEmail + " purchased " + subscription_id2 + " Plan id = "
				+ dataSet.get("PlanIDForBlankShortAndStandardPromt"));

		// POS Api Adding Reward into discount basket
		Response discountBasketResponse5 = pageObj.endpoints().authListDiscountBasketAddedForPOSAPIWithExt_Uid(
				dataSet.get("location_key"), userID5, "subscription", subscription_id2 + "", externalUID);
		Assert.assertEquals(discountBasketResponse5.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for POS add reward completion to basket");
		TestListeners.extentTest.get().pass("POS Api Adding subscription into discount basket is successful");
		logger.info("POS Api Adding subscription into discount basket is successful");

		// POS Discount Lookup Api
		Response discountLookupResponse5 = pageObj.endpoints().discountLookUpPosApi(dataSet.get("location_key"),
				userID5, "12003", "30", externalUID);
		Assert.assertEquals(discountLookupResponse4.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Discount Lookup Api ");
		String shortPrompt5 = discountLookupResponse5.jsonPath()
				.get("selected_discounts[0].discount_details.prompt_text_short");
		String longPrompt5 = discountLookupResponse5.jsonPath()
				.get("selected_discounts[0].discount_details.prompt_text_long");
		Assert.assertEquals(shortPrompt5, "", "Short Prompt did not match with Discount Lookup Api prompt_text_short");
		Assert.assertEquals(longPrompt5, "", "Long Prompt did not match with Discount Lookup Api prompt_text_long");
		logger.info(
				"Verified that in subscription, discount_details under selected_discounts returns Blank value of Short Prompt and Standard Prompt successfully for POS Discount Lookup Api");
		TestListeners.extentTest.get().pass(
				"Verified that in subscription, discount_details under selected_discounts returns Blank value of Short Prompt and Standard Prompt successfully for POS Discount Lookup Api");

		// Step - 18 discount_details under selected_discounts returns NULL value
		// of Short Prompt and Standard Prompt for subscription

		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse6 = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse6, "API 2 user signup");
		String userID6 = signUpResponse6.jsonPath().get("user.user_id").toString();
		String token6 = signUpResponse6.jsonPath().get("access_token.token").toString();
		Assert.assertEquals(signUpResponse6.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("API2 Signup is successful");
		logger.info("API2 Signup is successful");

		// subscription purchase api 2
		Response purchaseSubscriptionresponse3 = pageObj.endpoints().Api2SubscriptionPurchase(token6,
				dataSet.get("PlanIDForNullShortAndStandardPromt"), dataSet.get("client"), dataSet.get("secret"), "2",
				endDateTime);
		Assert.assertEquals(purchaseSubscriptionresponse3.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		int subscription_id3 = Integer
				.parseInt(purchaseSubscriptionresponse3.jsonPath().get("subscription_id").toString());
		logger.info(userEmail + " purchased " + subscription_id3 + " Plan id = "
				+ dataSet.get("PlanIDForNullShortAndStandardPromt"));

		// POS Api Adding Reward into discount basket
		Response discountBasketResponse6 = pageObj.endpoints().authListDiscountBasketAddedForPOSAPIWithExt_Uid(
				dataSet.get("location_key"), userID6, "subscription", subscription_id3 + "", externalUID);
		Assert.assertEquals(discountBasketResponse6.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for POS add reward completion to basket");
		TestListeners.extentTest.get().pass("POS Api Adding subscription into discount basket is successful");
		logger.info("POS Api Adding subscription into discount basket is successful");

		// POS Discount Lookup Api
		Response discountLookupResponse6 = pageObj.endpoints().discountLookUpPosApi(dataSet.get("location_key"),
				userID6, "12003", "30", externalUID);
		Assert.assertEquals(discountLookupResponse6.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Discount Lookup Api ");
		String shortPrompt6 = discountLookupResponse6.jsonPath()
				.get("selected_discounts[0].discount_details.prompt_text_short");
		String longPrompt6 = discountLookupResponse6.jsonPath()
				.get("selected_discounts[0].discount_details.prompt_text_long");
		Assert.assertEquals(shortPrompt6, null,
				"Short Prompt did not match with Discount Lookup Api prompt_text_short");
		Assert.assertEquals(longPrompt6, null, "Long Prompt did not match with Discount Lookup Api prompt_text_long");
		logger.info(
				"Verified that in subscription, discount_details under selected_discounts returns NULL value of Short Prompt and Standard Prompt successfully for POS Discount Lookup Api");
		TestListeners.extentTest.get().pass(
				"Verified that in subscription, discount_details under selected_discounts returns NULL value of Short Prompt and Standard Prompt successfully for POS Discount Lookup Api");

	}

	@Test(description = "SQ-T3287 [Batched Redemptions-OMM-T832(564)] Verify Short Prompt or Standard Prompt in Discount Lookup API response",groups = {"nonNightly" }, priority = 5)
	@Owner(name = "Hardik Bhardwaj")
	public void T3287_verifyShortAndStandardPromptPartTwo() throws Exception {

		// Step - 10 discount_details under selected_discounts returns configured value
		// of Short Prompt and Standard Prompt for redeemable

		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse7 = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("clientAutoTwo"),
				dataSet.get("secretAutoTwo"));
		pageObj.apiUtils().verifyResponse(signUpResponse7, "API 2 user signup");
		String userID7 = signUpResponse7.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse7.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("API2 Signup is successful");
		logger.info("API2 Signup is successful");

		// send reward amount to user Redeemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID7, dataSet.get("apiKeyAutoTwo"), "7",
				dataSet.get("redeemableIdForShortAndStandardPromt"), "9", "9");

		utils.logPass("Send redeemable to the user successfully");

		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get().pass("Api2  send reward amount to user is successful");

		// POS Api Adding Reward into discount basket
		Response discountBasketResponse7 = pageObj.endpoints().authListDiscountBasketAddedForPOSAPIWithExt_Uid(
				dataSet.get("locationKeyAutoTwo"), userID7, "redeemable",
				dataSet.get("redeemableIdForShortAndStandardPromt"), externalUID);
		Assert.assertEquals(discountBasketResponse7.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for POS add reward completion to basket");
		TestListeners.extentTest.get().pass("POS Api Adding redeemable into discount basket is successful");
		logger.info("POS Api Adding redeemable into discount basket is successful");

		utils.longWaitInSeconds(5);
		// POS Discount Lookup Api
		Response discountLookupResponse7 = pageObj.endpoints().discountLookUpPosApi(dataSet.get("locationKeyAutoTwo"),
				userID7, "110011", "30", externalUID);
		Assert.assertEquals(discountLookupResponse7.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Discount Lookup Api ");
		String shortPrompt7 = discountLookupResponse7.jsonPath()
				.get("selected_discounts[0].discount_details.prompt_text_short");
		String longPrompt7 = discountLookupResponse7.jsonPath()
				.get("selected_discounts[0].discount_details.prompt_text_long");
		Assert.assertEquals(shortPrompt7, "AutoShortPrompt",
				"Short Prompt did not match with Discount Lookup Api prompt_text_short");
		Assert.assertEquals(longPrompt7, "Automation Standard Prompt",
				"Long Prompt did not match with Discount Lookup Api prompt_text_long");
		logger.info(
				"Verified that in redeemable, discount_details under selected_discounts returns configured value of Short Prompt and Standard Prompt successfully for POS Discount Lookup Api");
		TestListeners.extentTest.get().pass(
				"Verified that in redeemable, discount_details under selected_discounts returns configured value of Short Prompt and Standard Prompt successfully for POS Discount Lookup Api");

		// Step - 11 discount_details under selected_discounts returns Blank value
		// of Short Prompt and Standard Prompt for redeemable

		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse8 = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("clientAutoTwo"),
				dataSet.get("secretAutoTwo"));
		pageObj.apiUtils().verifyResponse(signUpResponse8, "API 2 user signup");
		String userID8 = signUpResponse8.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse8.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("API2 Signup is successful");
		logger.info("API2 Signup is successful");

		// send reward amount to user Redeemable
		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(userID8, dataSet.get("apiKeyAutoTwo"), "7",
				dataSet.get("redeemableIdForBlankShortAndStandardPromt"), "11", "11");

		utils.logPass("Send redeemable to the user successfully");

		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse2.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get().pass("Api2  send reward amount to user is successful");

		// POS Api Adding Reward into discount basket
		Response discountBasketResponse8 = pageObj.endpoints().authListDiscountBasketAddedForPOSAPIWithExt_Uid(
				dataSet.get("locationKeyAutoTwo"), userID8, "redeemable",
				dataSet.get("redeemableIdForBlankShortAndStandardPromt"), externalUID);
		Assert.assertEquals(discountBasketResponse8.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for POS add reward completion to basket");
		TestListeners.extentTest.get().pass("POS Api Adding redeemable into discount basket is successful");
		logger.info("POS Api Adding redeemable into discount basket is successful");

		// POS Discount Lookup Api
		Response discountLookupResponse8 = pageObj.endpoints().discountLookUpPosApi(dataSet.get("locationKeyAutoTwo"),
				userID8, "12003", "30", externalUID);
		Assert.assertEquals(discountLookupResponse8.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Discount Lookup Api ");
		String shortPrompt8 = discountLookupResponse8.jsonPath()
				.get("unselected_discounts[0].discount_details.prompt_text_short");
		String longPrompt8 = discountLookupResponse8.jsonPath()
				.get("unselected_discounts[0].discount_details.prompt_text_long");
		Assert.assertEquals(shortPrompt8, "", "Short Prompt did not match with Discount Lookup Api prompt_text_short");
		Assert.assertEquals(longPrompt8, "", "Long Prompt did not match with Discount Lookup Api prompt_text_long");
		logger.info(
				"Verified that in redeemable, discount_details under selected_discounts returns Blank value of Short Prompt and Standard Prompt successfully for POS Discount Lookup Api");
		TestListeners.extentTest.get().pass(
				"Verified that in redeemable, discount_details under selected_discounts returns Blank value of Short Prompt and Standard Prompt successfully for POS Discount Lookup Api");

		// Step - 12 discount_details under selected_discounts returns NULL value
		// of Short Prompt and Standard Prompt for redeemable

		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse9 = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("clientAutoTwo"),
				dataSet.get("secretAutoTwo"));
		pageObj.apiUtils().verifyResponse(signUpResponse9, "API 2 user signup");
		String userID9 = signUpResponse9.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse9.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("API2 Signup is successful");
		logger.info("API2 Signup is successful");

		// send reward amount to user Redeemable
		Response sendRewardResponse3 = pageObj.endpoints().sendMessageToUser(userID9, dataSet.get("apiKeyAutoTwo"),
				"13", dataSet.get("redeemableIdForNullShortAndStandardPromt"), "13", "13");

		utils.logPass("Send redeemable to the user successfully");

		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse3.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get().pass("Api2  send reward amount to user is successful");

		// POS Api Adding Reward into discount basket
		Response discountBasketResponse9 = pageObj.endpoints().authListDiscountBasketAddedForPOSAPIWithExt_Uid(
				dataSet.get("locationKeyAutoTwo"), userID9, "redeemable",
				dataSet.get("redeemableIdForNullShortAndStandardPromt"), externalUID);
		Assert.assertEquals(discountBasketResponse9.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for POS add reward completion to basket");
		TestListeners.extentTest.get().pass("POS Api Adding redeemable into discount basket is successful");
		logger.info("POS Api Adding redeemable into discount basket is successful");

		// POS Discount Lookup Api
		Response discountLookupResponse9 = pageObj.endpoints().discountLookUpPosApi(dataSet.get("locationKeyAutoTwo"),
				userID9, "12003", "30", externalUID);
		Assert.assertEquals(discountLookupResponse9.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Discount Lookup Api ");
		String shortPrompt9 = discountLookupResponse9.jsonPath()
				.get("selected_discounts[0].discount_details.prompt_text_short");
		String longPrompt9 = discountLookupResponse9.jsonPath()
				.get("selected_discounts[0].discount_details.prompt_text_long");
		Assert.assertEquals(shortPrompt9, "", "Short Prompt did not match with Discount Lookup Api prompt_text_short");
		Assert.assertEquals(longPrompt9, "", "Long Prompt did not match with Discount Lookup Api prompt_text_long");
		logger.info(
				"Verified that in redeemable, discount_details under selected_discounts returns NULL value of Short Prompt and Standard Prompt successfully for POS Discount Lookup Api");
		TestListeners.extentTest.get().pass(
				"Verified that in redeemable, discount_details under selected_discounts returns NULL value of Short Prompt and Standard Prompt successfully for POS Discount Lookup Api");

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