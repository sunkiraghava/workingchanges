package com.punchh.server.OMMTest;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
import com.punchh.server.apiConfig.ApiResponseJsonSchema;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class PerishedErrorMessageTest {
	private static Logger logger = LogManager.getLogger(PerishedErrorMessageTest.class);
	public WebDriver driver;
	private Properties prop;
	private PageObj pageObj;
	private String userEmail;
	private String sTCName;
	private String baseUrl;
	String blankString = "";
	private String env, run = "ui";
	private static Map<String, String> dataSet;
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

		prop = Utilities.loadPropertiesFile("segmentBeta.properties");
		sTCName = method.getName();
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run , env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run , env , "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==>" + dataSet);
		utils = new Utilities(driver);
		externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
	}

	@SuppressWarnings("static-access")
	@Test(description = "SQ-T5152 Auth>Validate that if reward has been added into basket and it got perished then error message gets displayed on hitting Get Active Discount Basket API", priority = 3)
	@Owner(name = "Hardik Bhardwaj")
	public void T5152_PerishedRewardErrorMessage() throws Exception {
		String redeemableID = dataSet.get("redeemable_id");

		// login to instance
//		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
//		pageObj.instanceDashboardPage().loginToInstance();

		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Navigate to Multiple Redemption Tab and check the flags
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.cockpitRedemptionsPage().setAcquisitionType("Offers", "10");
		pageObj.dashboardpage().updateCheckBox();

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");
		String authToken = signUpResponse.jsonPath().get("authentication_token");
		String token = signUpResponse.jsonPath().get("access_token").toString(); // to be used in non auth api
		String userID = signUpResponse.jsonPath().get("user_id").toString();
		TestListeners.extentTest.get().pass("Auth Signup is successful");
		logger.info("Auth Signup is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				redeemableID, "", "");

		utils.logPass("Send redeemable to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		// get reward id
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemableID);

		utils.logPass("Reward id " + rewardId + " is generated successfully ");

		// add reward completion to basket
		Response discountBasketResponse1 = pageObj.endpoints().authListDiscountBasketAddedForPOSAPIWithExt_Uid(
				dataSet.get("location_key2"), userID, "reward", rewardId, externalUID);
		Assert.assertEquals(discountBasketResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for POS add reward completion to basket");

		// DB query for updation
		String query = "UPDATE rewards SET end_time = '2023-12-06 07:59:59', status = 'perished' WHERE id = '"
				+ rewardId + "'";
		int rs = DBUtils.executeUpdateQuery(env, query);
		Assert.assertEquals(rs, 1);

		// Auth GET Active Discount Basket API
		Response basketDiscountDetailsResponse = pageObj.endpoints().fetchActiveBasketAuthApi(token,
				dataSet.get("client"), dataSet.get("secret"), externalUID);
		Assert.assertEquals(basketDiscountDetailsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for  Auth GET Active Discount Basket API");
		String value1 = pageObj.guestTimelinePage().verifyDiscountBasketVariable(basketDiscountDetailsResponse,
				"discount_basket_items", "discount_id", rewardId, "message[0]");
		Assert.assertEquals(value1, "Invalid Reward ID.",
				"In Auth GET Active Discount Basket API Invalid Reward ID. error response is not present");
		logger.info("In Auth GET Active Discount Basket API Invalid Reward ID. error response is present");
		TestListeners.extentTest.get()
				.pass("In Auth GET Active Discount Basket API Invalid Reward ID. error response is present");

	}

	@SuppressWarnings("static-access")
	@Test(description = "SQ-T5153 API2 Mobile>Validate that if reward has been added into basket and it got archived then error message gets displayed on hitting Get Active Discount Basket API", priority = 2)
	@Owner(name = "Hardik Bhardwaj")
	public void T5153_PerishedRewardErrorMessage() throws Exception {
		String redeemableID = dataSet.get("redeemable_id");

		// login to instance
//		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
//		pageObj.instanceDashboardPage().loginToInstance();

		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Navigate to Multiple Redemption Tab and check the flags
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.cockpitRedemptionsPage().setAcquisitionType("Offers", "10");
		pageObj.dashboardpage().updateCheckBox();

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");
		String token = signUpResponse.jsonPath().get("access_token").toString(); // to be used in non auth api
		String userID = signUpResponse.jsonPath().get("user_id").toString();
		TestListeners.extentTest.get().pass("Auth Signup is successful");
		logger.info("Auth Signup is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				redeemableID, "", "");

		utils.logPass("Send redeemable to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		// get reward id
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemableID);

		utils.logPass("Reward id " + rewardId + " is generated successfully ");

		// add reward completion to basket
		Response discountBasketResponse1 = pageObj.endpoints().authListDiscountBasketAddedForPOSAPIWithExt_Uid(
				dataSet.get("location_key2"), userID, "reward", rewardId, externalUID);
		Assert.assertEquals(discountBasketResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for POS add reward completion to basket");

		// DB query for updation
		String query = "UPDATE rewards SET end_time = '2023-12-06 07:59:59', status = 'perished' WHERE id = '"
				+ rewardId + "'";
		int rs = DBUtils.executeUpdateQuery(env, query);
		Assert.assertEquals(rs, 1);

		// Mobile GET Active Discount Basket API
		Response basketDiscountDetailsResponse = pageObj.endpoints().getUserDiscountBasketDetailsUsingAPI2(token,
				dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(basketDiscountDetailsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for  Auth GET Active Discount Basket API");
		String value1 = pageObj.guestTimelinePage().verifyDiscountBasketVariable(basketDiscountDetailsResponse,
				"discount_basket_items", "discount_id", rewardId, "message[0]");
		Assert.assertEquals(value1, "Invalid Reward ID.",
				"In Auth GET Active Discount Basket API Invalid Reward ID. error response is not present");
		logger.info("In Auth GET Active Discount Basket API Invalid Reward ID. error response is present");
		TestListeners.extentTest.get()
				.pass("In Auth GET Active Discount Basket API Invalid Reward ID. error response is present");

	}

	@SuppressWarnings("static-access")
	@Test(description = "SQ-T5154 POS>Validate that if reward has been added into basket and it got expired/perished/archived then error message gets displayed on hitting Add Discount Basket API", priority = 0)
	@Owner(name = "Hardik Bhardwaj")
	public void T5154_PerishedRewardErrorMessage() throws Exception {
		String redeemableID = dataSet.get("redeemable_id");
		String redeemableID2 = dataSet.get("redeemable_id2");
		String redeemableID3 = dataSet.get("redeemable_id3");
		String redeemableID4 = dataSet.get("redeemable_id4");
		String redeemableID5 = dataSet.get("redeemable_id5");

		// login to instance
//		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
//		pageObj.instanceDashboardPage().loginToInstance();

		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Navigate to Multiple Redemption Tab and check the flags
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.cockpitRedemptionsPage().updateTotalNumberOfRedemptionsAllowed("30");
		pageObj.cockpitRedemptionsPage().setAcquisitionType("Offers", "25");
		pageObj.dashboardpage().updateCheckBox();
		
		utils.longWaitInSeconds(5);

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");
		String token = signUpResponse.jsonPath().get("access_token").toString(); // to be used in non auth api
		String userID = signUpResponse.jsonPath().get("user_id").toString();
		TestListeners.extentTest.get().pass("Auth Signup is successful");
		logger.info("Auth Signup is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "50",
				redeemableID, "", "");

		utils.logPass("Send redeemable to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		// get reward id
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemableID);

		utils.logPass("Reward id " + rewardId + " is generated successfully ");

		// add reward completion to basket
		Response discountBasketResponse1 = pageObj.endpoints().authListDiscountBasketAddedForPOSAPIWithExt_Uid(
				dataSet.get("location_key2"), userID, "reward", rewardId, externalUID);
		Assert.assertEquals(discountBasketResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for POS add reward completion to basket");

		// send reward amount to user Reedemable
		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "50",
				redeemableID2, "", "");

		utils.logPass("Send redeemable to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse2.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		// get reward id
		String rewardId2 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemableID2);

		utils.logPass("Reward id " + rewardId2 + " is generated successfully ");

		// add reward completion to basket
		Response discountBasketResponse2 = pageObj.endpoints().authListDiscountBasketAddedForPOSAPIWithExt_Uid(
				dataSet.get("location_key2"), userID, "reward", rewardId2, externalUID);
		Assert.assertEquals(discountBasketResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for POS add reward completion to basket");

		// send reward amount to user Reedemable
		Response sendRewardResponse3 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "50",
				redeemableID3, "", "");

		utils.logPass("Send redeemable to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse3.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		// get reward id
		String rewardId3 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemableID3);

		utils.logPass("Reward id " + rewardId3 + " is generated successfully ");
		utils.longWaitInSeconds(6);

		// add reward completion to basket
		Response discountBasketResponse3 = pageObj.endpoints().authListDiscountBasketAddedForPOSAPIWithExt_Uid(
				dataSet.get("location_key2"), userID, "reward", rewardId3, externalUID);
		Assert.assertEquals(discountBasketResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for POS add reward completion to basket");

		// DB query for updation
		String query = "UPDATE rewards SET status = 'expired' WHERE id = '" + rewardId + "'";
		int rs = DBUtils.executeUpdateQuery(env, query);
		Assert.assertEquals(rs, 1);

		String query2 = "UPDATE rewards SET end_time = '2023-12-06 07:59:59', status = 'perished' WHERE id = '"
				+ rewardId2 + "'";
		int rs2 = DBUtils.executeUpdateQuery(env, query2);
		Assert.assertEquals(rs2, 1);

		String query3 = "DELETE FROM rewards WHERE id = '" + rewardId3 + "'";
		int rs3 = DBUtils.executeUpdateQuery(env, query3);
		Assert.assertEquals(rs3, 1);

		// send reward amount to user Reedemable
		Response sendRewardResponse4 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "50",
				redeemableID4, "", "");

		utils.logPass("Send redeemable to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse4.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		// get reward id
		String rewardId4 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemableID4);

		logger.info("Reward id " + rewardId4 + " is generated successfully ");
		TestListeners.extentTest.get().pass("Reward id " + rewardId4 + " is generated successfully ");

		// POS Add Discount to Basket
		Response discountBasketResponse4 = pageObj.endpoints().authListDiscountBasketAddedForPOSAPIWithExt_Uid(
				dataSet.get("location_key2"), userID, "reward", rewardId4, externalUID);
		Assert.assertEquals(discountBasketResponse4.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for POS add reward completion to basket");
		String value1 = pageObj.guestTimelinePage().verifyDiscountBasketVariable(discountBasketResponse4,
				"discount_basket_items", "discount_id", rewardId, "message[0]");
		Assert.assertEquals(value1, "Invalid Reward ID.",
				"In POS Add Discount to Basket API Invalid Reward ID. error response is not present for expired reward");
		logger.info(
				"In POS Add Discount to Basket API Invalid Reward ID. error response is present for expired reward");
		TestListeners.extentTest.get().pass(
				"In POS Add Discount to Basket API Invalid Reward ID. error response is present for expired reward");

		String value2 = pageObj.guestTimelinePage().verifyDiscountBasketVariable(discountBasketResponse4,
				"discount_basket_items", "discount_id", rewardId2, "message[0]");
		Assert.assertEquals(value2, "Invalid Reward ID.",
				"In POS Add Discount to Basket API Invalid Reward ID. error response is not present for perished reward");
		logger.info(
				"In POS Add Discount to Basket API Invalid Reward ID. error response is present for perished reward");
		TestListeners.extentTest.get().pass(
				"In POS Add Discount to Basket API Invalid Reward ID. error response is present for perished reward");

		String value3 = pageObj.guestTimelinePage().verifyDiscountBasketVariable(discountBasketResponse4,
				"discount_basket_items", "discount_id", rewardId3, "message[0]");
		Assert.assertEquals(value3, "Invalid Reward ID.",
				"In POS Add Discount to Basket API Invalid Reward ID. error response is not present for archived reward");
		logger.info(
				"In POS Add Discount to Basket API Invalid Reward ID. error response is present for archived reward");
		TestListeners.extentTest.get().pass(
				"In POS Add Discount to Basket API Invalid Reward ID. error response is present for archived reward");

	}

	@SuppressWarnings("static-access")
	@Test(description = "SQ-T5155 API Mobile>Validate that if reward has been added into basket and it got expired/perished/archived then error message gets displayed on hitting Add Discount Basket API", priority = 4)
	@Owner(name = "Hardik Bhardwaj")
	public void T5155_PerishedRewardErrorMessage() throws Exception {
		String redeemableID = dataSet.get("redeemable_id");
		String redeemableID2 = dataSet.get("redeemable_id2");
		String redeemableID3 = dataSet.get("redeemable_id3");
		String redeemableID4 = dataSet.get("redeemable_id4");
		String redeemableID5 = dataSet.get("redeemable_id5");

		// login to instance
//		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
//		pageObj.instanceDashboardPage().loginToInstance();

		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Navigate to Multiple Redemption Tab and check the flags
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_discount_locking", "uncheck");
		pageObj.cockpitRedemptionsPage().setAcquisitionType("Offers", "10");
		pageObj.dashboardpage().updateCheckBox();

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");
		String token = signUpResponse.jsonPath().get("access_token").toString(); // to be used in non auth api
		String userID = signUpResponse.jsonPath().get("user_id").toString();
		TestListeners.extentTest.get().pass("Auth Signup is successful");
		logger.info("Auth Signup is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "50",
				redeemableID, "", "");

		utils.logPass("Send redeemable to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		// get reward id
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemableID);

		utils.logPass("Reward id " + rewardId + " is generated successfully ");

		// add reward completion to basket
		Response discountBasketResponse1 = pageObj.endpoints().authListDiscountBasketAddedForPOSAPIWithExt_Uid(
				dataSet.get("location_key2"), userID, "reward", rewardId, "");
		Assert.assertEquals(discountBasketResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for POS add reward completion to basket");

		// send reward amount to user Reedemable
		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "50",
				redeemableID2, "", "");

		utils.logPass("Send redeemable to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse2.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		// get reward id
		String rewardId2 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemableID2);

		utils.logPass("Reward id " + rewardId2 + " is generated successfully ");

		// add reward completion to basket
		Response discountBasketResponse2 = pageObj.endpoints().authListDiscountBasketAddedForPOSAPIWithExt_Uid(
				dataSet.get("location_key2"), userID, "reward", rewardId2, "");
		Assert.assertEquals(discountBasketResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for POS add reward completion to basket");

		// send reward amount to user Reedemable
		Response sendRewardResponse3 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "50",
				redeemableID3, "", "");

		utils.logPass("Send redeemable to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse3.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		// get reward id
		String rewardId3 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemableID3);

		utils.logPass("Reward id " + rewardId3 + " is generated successfully ");

		// add reward completion to basket
		Response discountBasketResponse3 = pageObj.endpoints().authListDiscountBasketAddedForPOSAPIWithExt_Uid(
				dataSet.get("location_key2"), userID, "reward", rewardId3, "");
		Assert.assertEquals(discountBasketResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for POS add reward completion to basket");

		// DB query for updation
		String query = "UPDATE rewards SET status = 'expired' WHERE id = '" + rewardId + "'";
		int rs = DBUtils.executeUpdateQuery(env, query);
		Assert.assertEquals(rs, 1);

		String query2 = "UPDATE rewards SET end_time = '2023-12-06 07:59:59', status = 'perished' WHERE id = '"
				+ rewardId2 + "'";
		int rs2 = DBUtils.executeUpdateQuery(env, query2);
		Assert.assertEquals(rs2, 1);

		String query3 = "DELETE FROM rewards WHERE id = '" + rewardId3 + "'";
		int rs3 = DBUtils.executeUpdateQuery(env, query3);
		Assert.assertEquals(rs3, 1);

		// send reward amount to user Reedemable
		Response sendRewardResponse4 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "50",
				redeemableID4, "", "");

		utils.logPass("Send redeemable to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse4.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		// get reward id
		String rewardId4 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemableID4);

		logger.info("Reward id " + rewardId4 + " is generated successfully ");
		TestListeners.extentTest.get().pass("Reward id " + rewardId4 + " is generated successfully ");

		// POS Add Discount to Basket
		Response discountBasketResponse4 = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId4);
		Assert.assertEquals(discountBasketResponse4.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for POS add reward completion to basket");
		String value1 = pageObj.guestTimelinePage().verifyDiscountBasketVariable(discountBasketResponse4,
				"discount_basket_items", "discount_id", rewardId, "message[0]");
		Assert.assertEquals(value1, "Invalid Reward ID.",
				"In POS Add Discount to Basket API Invalid Reward ID. error response is not present for expired reward");
		logger.info(
				"In POS Add Discount to Basket API Invalid Reward ID. error response is present for expired reward");
		TestListeners.extentTest.get().pass(
				"In POS Add Discount to Basket API Invalid Reward ID. error response is present for expired reward");

		String value2 = pageObj.guestTimelinePage().verifyDiscountBasketVariable(discountBasketResponse4,
				"discount_basket_items", "discount_id", rewardId2, "message[0]");
		Assert.assertEquals(value2, "Invalid Reward ID.",
				"In POS Add Discount to Basket API Invalid Reward ID. error response is not present for perished reward");
		logger.info(
				"In POS Add Discount to Basket API Invalid Reward ID. error response is present for perished reward");
		TestListeners.extentTest.get().pass(
				"In POS Add Discount to Basket API Invalid Reward ID. error response is present for perished reward");

		String value3 = pageObj.guestTimelinePage().verifyDiscountBasketVariable(discountBasketResponse4,
				"discount_basket_items", "discount_id", rewardId3, "message[0]");
		Assert.assertEquals(value3, "Invalid Reward ID.",
				"In POS Add Discount to Basket API Invalid Reward ID. error response is not present for archived reward");
		logger.info(
				"In POS Add Discount to Basket API Invalid Reward ID. error response is present for archived reward");
		TestListeners.extentTest.get().pass(
				"In POS Add Discount to Basket API Invalid Reward ID. error response is present for archived reward");

		// AUTH Add Discount to Basket - add amount
		Response discountBasketResp3 = pageObj.endpoints().addDiscountAmountToBasketAUTH(token, dataSet.get("client"),
				dataSet.get("secret"), "discount_amount", "10", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResp3.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		logger.info("AUTH add discount to basket is successful add amount");
		TestListeners.extentTest.get().pass("AUTH add discount to basket is successful add amount");
		String expDiscountBasketItemId = discountBasketResp3.jsonPath()
				.getString("discount_basket_items.discount_basket_item_id");

		// Auth Discount unselect API
		expDiscountBasketItemId = expDiscountBasketItemId.replace("[", "").replace("]", "");
		Response deleteBasketResponse = pageObj.endpoints().deleteDiscountBasketForUserAUTH(token,
				dataSet.get("client"), dataSet.get("secret"), expDiscountBasketItemId);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, deleteBasketResponse.getStatusCode());
		logger.info("Auth Discount unselect API is successful in removing Discount");
		TestListeners.extentTest.get().pass("Auth Discount unselect API is successful in removing Discount");

		String value5 = pageObj.guestTimelinePage().verifyDiscountBasketVariable(deleteBasketResponse,
				"discount_basket_items", "discount_id", rewardId2, "message[0]");
		Assert.assertEquals(value5, "Invalid Reward ID.",
				"In POS Add Discount to Basket API Invalid Reward ID. error response is not present for perished reward");
		logger.info(
				"In POS Add Discount to Basket API Invalid Reward ID. error response is present for perished reward");
		TestListeners.extentTest.get().pass(
				"In POS Add Discount to Basket API Invalid Reward ID. error response is present for perished reward");

		String value6 = pageObj.guestTimelinePage().verifyDiscountBasketVariable(deleteBasketResponse,
				"discount_basket_items", "discount_id", rewardId3, "message[0]");
		Assert.assertEquals(value6, "Invalid Reward ID.",
				"In POS Add Discount to Basket API Invalid Reward ID. error response is not present for archived reward");
		logger.info(
				"In POS Add Discount to Basket API Invalid Reward ID. error response is present for archived reward");
		TestListeners.extentTest.get().pass(
				"In POS Add Discount to Basket API Invalid Reward ID. error response is present for archived reward");
	}

	@SuppressWarnings("static-access")
	@Test(description = "SQ-T5156 Auth Unselect>Validate that if reward has been added into basket and it got expired/perished/archived then error message gets displayed on hitting Remove discount type from Discount Basket API "
			+ "|| SQ-T5157 Auth Auto-Select>Validate that if reward has been added into basket and it got expired/perished/archived then error message gets displayed on hitting Auth Auto select API", priority = 1)
	@Owner(name = "Hardik Bhardwaj")
	public void T5156_PerishedRewardErrorMessage() throws Exception {
		String redeemableID = dataSet.get("redeemable_id");
		String redeemableID2 = dataSet.get("redeemable_id2");
		String redeemableID3 = dataSet.get("redeemable_id3");
		String redeemableID5 = dataSet.get("redeemable_id5");
		String redeemableID4 = dataSet.get("redeemable_id4");

		// login to instance
//		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
//		pageObj.instanceDashboardPage().loginToInstance();

		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Navigate to Multiple Redemption Tab and check the flags
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.cockpitRedemptionsPage().updateTotalNumberOfRedemptionsAllowed("10");
		pageObj.cockpitRedemptionsPage().setAcquisitionType("Offers", "10");
		pageObj.dashboardpage().updateButton();

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");
		String token = signUpResponse.jsonPath().get("access_token").toString(); // to be used in non auth api
		String userID = signUpResponse.jsonPath().get("user_id").toString();
		TestListeners.extentTest.get().pass("Auth Signup is successful");
		logger.info("Auth Signup is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "50",
				redeemableID, "", "");

		utils.logPass("Send redeemable to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		// get reward id
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemableID);

		utils.logPass("Reward id " + rewardId + " is generated successfully ");

		// add reward completion to basket
		Response discountBasketResponse1 = pageObj.endpoints().authListDiscountBasketAddedForPOSAPIWithExt_Uid(
				dataSet.get("location_key2"), userID, "reward", rewardId, "");
		Assert.assertEquals(discountBasketResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for POS add reward completion to basket");

		// send reward amount to user Reedemable
		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "50",
				redeemableID2, "", "");

		utils.logPass("Send redeemable to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse2.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		// get reward id
		String rewardId2 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemableID2);

		utils.logPass("Reward id " + rewardId2 + " is generated successfully ");

		// add reward completion to basket
		Response discountBasketResponse2 = pageObj.endpoints().authListDiscountBasketAddedForPOSAPIWithExt_Uid(
				dataSet.get("location_key2"), userID, "reward", rewardId2, "");
		Assert.assertEquals(discountBasketResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for POS add reward completion to basket");

		// send reward amount to user Reedemable
		Response sendRewardResponse3 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "50",
				redeemableID3, "", "");

		utils.logPass("Send redeemable to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse3.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		// get reward id
		String rewardId3 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemableID3);

		utils.logPass("Reward id " + rewardId3 + " is generated successfully ");

		// add reward completion to basket
		Response discountBasketResponse3 = pageObj.endpoints().authListDiscountBasketAddedForPOSAPIWithExt_Uid(
				dataSet.get("location_key2"), userID, "reward", rewardId3, "");
		Assert.assertEquals(discountBasketResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for POS add reward completion to basket");

		// DB query for updation
		String query = "UPDATE rewards SET status = 'expired' WHERE id = '" + rewardId + "'";
		int rs = DBUtils.executeUpdateQuery(env, query);
		Assert.assertEquals(rs, 1);

		String query2 = "UPDATE rewards SET end_time = '2023-12-06 07:59:59', status = 'perished' WHERE id = '"
				+ rewardId2 + "'";
		int rs2 = DBUtils.executeUpdateQuery(env, query2);
		Assert.assertEquals(rs2, 1);

		String query3 = "DELETE FROM rewards WHERE id = '" + rewardId3 + "'";
		int rs3 = DBUtils.executeUpdateQuery(env, query3);
		Assert.assertEquals(rs3, 1);

		// send reward amount to user Reedemable
		Response sendRewardResponse4 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "50",
				redeemableID5, "", "");

		utils.logPass("Send redeemable to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse4.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		// get reward id
		String rewardId4 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemableID5);

		logger.info("Reward id " + rewardId4 + " is generated successfully ");
		TestListeners.extentTest.get().pass("Reward id " + rewardId4 + " is generated successfully ");

		// POS Add Discount to Basket
		Response discountBasketResponse4 = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId4);
		Assert.assertEquals(discountBasketResponse4.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for POS add reward completion to basket");

		String value = pageObj.guestTimelinePage().verifyDiscountBasketVariable(discountBasketResponse4,
				"discount_basket_items", "discount_id", rewardId4, "discount_basket_item_id");

		// Auth Discount unselect API
		Response deleteBasketResponse = pageObj.endpoints().deleteDiscountBasketForUserAUTH(token,
				dataSet.get("client"), dataSet.get("secret"), value);
		Assert.assertEquals(deleteBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean isAuthRemoveDiscountBasketItemSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.authRemoveDiscountBasketItemSchema, deleteBasketResponse.asString());
		Assert.assertTrue(isAuthRemoveDiscountBasketItemSchemaValidated,
				"Auth Remove Discount Basket Item schema validation failed");
		logger.info("Auth Discount unselect API is successful in removing Discount");
		TestListeners.extentTest.get().pass("Auth Discount unselect API is successful in removing Discount");

		String value4 = pageObj.guestTimelinePage().verifyDiscountBasketVariable(deleteBasketResponse,
				"discount_basket_items", "discount_id", rewardId, "message[0]");
		Assert.assertEquals(value4, "Invalid Reward ID.",
				"In POS Add Discount to Basket API Invalid Reward ID. error response is not present for expired reward");
		logger.info(
				"In POS Add Discount to Basket API Invalid Reward ID. error response is present for expired reward");
		TestListeners.extentTest.get().pass(
				"In POS Add Discount to Basket API Invalid Reward ID. error response is present for expired reward");

		String value5 = pageObj.guestTimelinePage().verifyDiscountBasketVariable(deleteBasketResponse,
				"discount_basket_items", "discount_id", rewardId2, "message[0]");
		Assert.assertEquals(value5, "Invalid Reward ID.",
				"In POS Add Discount to Basket API Invalid Reward ID. error response is not present for perished reward");
		logger.info(
				"In POS Add Discount to Basket API Invalid Reward ID. error response is present for perished reward");
		TestListeners.extentTest.get().pass(
				"In POS Add Discount to Basket API Invalid Reward ID. error response is present for perished reward");

		String value6 = pageObj.guestTimelinePage().verifyDiscountBasketVariable(deleteBasketResponse,
				"discount_basket_items", "discount_id", rewardId3, "message[0]");
		Assert.assertEquals(value6, "Invalid Reward ID.",
				"In POS Add Discount to Basket API Invalid Reward ID. error response is not present for archived reward");
		logger.info(
				"In POS Add Discount to Basket API Invalid Reward ID. error response is present for archived reward");
		TestListeners.extentTest.get().pass(
				"In POS Add Discount to Basket API Invalid Reward ID. error response is present for archived reward");

		// send reward amount to user Reedemable
		Response sendRewardResponse5 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "50",
				redeemableID4, "", "");

		utils.logPass("Send redeemable to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse5.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		// get reward id
		String rewardId5 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemableID4);

		logger.info("Reward id " + rewardId5 + " is generated successfully ");
		TestListeners.extentTest.get().pass("Reward id " + rewardId5 + " is generated successfully ");

		// POS Add Discount to Basket
		Response discountBasketResponse5 = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId5);
		Assert.assertEquals(discountBasketResponse5.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for POS add reward completion to basket");

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
		boolean isAuthAutoSelectInvalidRewardSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.authAutoSelectMessageSchema, redemptionResponse.asString());
		Assert.assertTrue(isAuthAutoSelectInvalidRewardSchemaValidated,
				"Auth Auto Select with Invalid Reward schema validation failed");
		TestListeners.extentTest.get().pass("AUTH add discount to basket is successful");

		String value1 = pageObj.guestTimelinePage().verifyDiscountBasketVariable(redemptionResponse,
				"discount_basket_items", "discount_id", rewardId, "message[0]");
		Assert.assertEquals(value1, "Invalid Reward ID.",
				"In POS Add Discount to Basket API Invalid Reward ID. error response is not present for expired reward");
		logger.info(
				"In POS Add Discount to Basket API Invalid Reward ID. error response is present for expired reward");
		TestListeners.extentTest.get().pass(
				"In POS Add Discount to Basket API Invalid Reward ID. error response is present for expired reward");

		String value2 = pageObj.guestTimelinePage().verifyDiscountBasketVariable(redemptionResponse,
				"discount_basket_items", "discount_id", rewardId2, "message[0]");
		Assert.assertEquals(value2, "Invalid Reward ID.",
				"In POS Add Discount to Basket API Invalid Reward ID. error response is not present for perished reward");
		logger.info(
				"In POS Add Discount to Basket API Invalid Reward ID. error response is present for perished reward");
		TestListeners.extentTest.get().pass(
				"In POS Add Discount to Basket API Invalid Reward ID. error response is present for perished reward");

		String value3 = pageObj.guestTimelinePage().verifyDiscountBasketVariable(redemptionResponse,
				"discount_basket_items", "discount_id", rewardId3, "message[0]");
		Assert.assertEquals(value3, "Invalid Reward ID.",
				"In POS Add Discount to Basket API Invalid Reward ID. error response is not present for archived reward");
		logger.info(
				"In POS Add Discount to Basket API Invalid Reward ID. error response is present for archived reward");
		TestListeners.extentTest.get().pass(
				"In POS Add Discount to Basket API Invalid Reward ID. error response is present for archived reward");

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
