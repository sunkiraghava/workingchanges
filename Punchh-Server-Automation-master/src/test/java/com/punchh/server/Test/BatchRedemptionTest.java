package com.punchh.server.Test;

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
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class BatchRedemptionTest {
	private static Logger logger = LogManager.getLogger(BatchRedemptionTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String userEmail;
	private String sTCName;
	private String baseUrl;
	String blankString = "";
	private String env, run = "ui";
	private static Map<String, String> dataSet;
	String externalUID;
	Properties prop;

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
		sTCName = method.getName();
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==>" + dataSet);
		externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
	}

	// Author -- Hardik Bhardwaj

	@Test(description = "SQ-T4555 Verify fuel_reward redemption when discount_basket contains only fuel_reward having rate rollback QC attached", groups = {
			"regression", "dailyrun" }, priority = 0)
	@Owner(name = "Hardik Bhardwaj")
	public void T4555_FuelReward() throws Exception {

		String b_id = dataSet.get("business_id");

		String preferenceQuery = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id
				+ "'";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, preferenceQuery, "preferences");

		// updating the business preference for enable_optin_for_challenges
		DBUtils.updateBusinessFlag(env, expColValue, "false", "enable_optin_for_challenges", b_id);

		// updating the business preference for enable_optin_for_challenges
		DBUtils.updateBusinessFlag(env, expColValue, "false", "enable_optout_for_challenges", b_id);


		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();
		Map<String, String> detailsMap2 = new HashMap<String, String>();
		Map<String, String> detailsMap3 = new HashMap<String, String>();

		Map<String, Map<String, String>> parentMap2 = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap4 = new HashMap<String, String>();
		Map<String, String> detailsMap5 = new HashMap<String, String>();
		Map<String, String> detailsMap6 = new HashMap<String, String>();
		Map<String, String> detailsMap7 = new HashMap<String, String>();
		Map<String, String> detailsMap8 = new HashMap<String, String>();

		// login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */

		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// set conversion rule
		pageObj.menupage().navigateToSubMenuItem("Settings", "Conversion Rules");
		pageObj.settingsPage().clickConversionRule(dataSet.get("conversionRule"));
		pageObj.settingsPage().selectConversionCriteriaInConversionRules("Set", dataSet.get("conversionCriteria"));
		pageObj.settingsPage().clickSaveBtn();

		// Navigate to Multiple Redemption Tab and Uncheck the flags
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Validations");
		pageObj.cockpitRedemptionsPage().updateMinimumPayablePrice("0.01");
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

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
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "16", "",
				"10", "");

		pageObj.utils().logPass("Send redeemable to the user successfully");

		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get().pass("Api2  send reward amount to user is successful");

		// POS Add Discount to Basket
		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAddedForPOSAPIWithExt_Uid(
				dataSet.get("locationKey"), userID, "fuel_reward", "10", externalUID);
		Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with add discount to basket ");
		pageObj.utils().logit("POS Add Discount to Basket is sucessfull");

		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Fuel1", "1", "10", "M", "10", "999", "1",
				dataSet.get("item_id"));
		parentMap.put("Fuel1", detailsMap1);

		detailsMap2 = pageObj.endpoints().getRecieptDetailsMap("Fuel2", "1", "10", "M", "10", "999", "2",
				dataSet.get("item_id"));
		parentMap.put("Fuel2", detailsMap2);

		detailsMap3 = pageObj.endpoints().getRecieptDetailsMap("Fuel3", "1", "10", "M", "10", "999", "3",
				dataSet.get("item_id_1"));
		parentMap.put("Fuel3", detailsMap3);

		// POS batch redemption
		Response batchRedemptionProcessResponseUser = pageObj.endpoints()
				.processBatchRedemptionOfBasketWithQueryParamPOSAPI(dataSet.get("locationKey"), userID, "30", "true",
						parentMap);
		TestListeners.extentTest.get()
				.info("POS batch redemption response " + batchRedemptionProcessResponseUser.asPrettyString());
		Assert.assertEquals(batchRedemptionProcessResponseUser.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"batch_redemptions status code is not 200 ");
		pageObj.utils().logPass("User process the discount_basket with POS batch redemption is sucessfull");
		String discountAmount = batchRedemptionProcessResponseUser.jsonPath().get("success[0].discount_amount")
				.toString();
		Assert.assertEquals(discountAmount, "19.98",
				"Discount amount did not match for User process the discount_basket");
		pageObj.utils().logPass("Verified that Discount amount matched for User process the discount_basket");

		detailsMap4 = pageObj.endpoints().getRecieptDetailsMap("Fuel1", "1", "10", "M", "10", "999", "1", "300");
		parentMap2.put("Fuel1", detailsMap4);

		detailsMap5 = pageObj.endpoints().getRecieptDetailsMap("Oil1", "1", "2", "M", "10", "999", "1.1",
				dataSet.get("item_id"));
		parentMap2.put("Oil1", detailsMap5);

		detailsMap6 = pageObj.endpoints().getRecieptDetailsMap("Oil2", "1", "3", "M", "10", "999", "1.2",
				dataSet.get("item_id"));
		parentMap2.put("Oil2", detailsMap6);

		detailsMap7 = pageObj.endpoints().getRecieptDetailsMap("Fuel2", "1", "10", "M", "10", "999", "2",
				dataSet.get("item_id"));
		parentMap2.put("Fuel2", detailsMap7);

		detailsMap8 = pageObj.endpoints().getRecieptDetailsMap("Fuel3", "1", "10", "M", "10", "999", "3",
				dataSet.get("item_id_1"));
		parentMap2.put("Fuel3", detailsMap8);

		Response batchRedemptionProcessResponseUser1 = pageObj.endpoints()
				.processBatchRedemptionOfBasketWithQueryParamPOSAPI(dataSet.get("locationKey"), userID, "35", "true",
						parentMap2);
		Assert.assertEquals(batchRedemptionProcessResponseUser1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"batch_redemptions status code is not 200 ");
		pageObj.utils().logPass("User process the discount_basket with POS batch redemption is sucessfull");
		String discountAmount1 = batchRedemptionProcessResponseUser1.jsonPath().get("success[0].discount_amount")
				.toString();
		Assert.assertEquals(discountAmount1, "14.97",
				"Discount amount did not match for User process the discount_basket");
		pageObj.utils().logPass("Verified that Discount amount matched for User process the discount_basket");

		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Validations");
		pageObj.cockpitRedemptionsPage().updateMinimumPayablePrice("0.0");
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

	}

	@Test(description = "SQ-T4556 Verify fuel_reward redemption when QC is attached to conversion criteria and input receipt has more than 1 quantity", groups = {
			"regression", "dailyrun" }, priority = 1)
	@Owner(name = "Hardik Bhardwaj")
	public void T4556_FuelReward() throws Exception {

		String b_id = dataSet.get("business_id");

		String preferenceQuery = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id
				+ "'";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, preferenceQuery, "preferences");

		// updating the business preference for enable_optin_for_challenges
		DBUtils.updateBusinessFlag(env, expColValue, "false", "enable_optin_for_challenges", b_id);

		// updating the business preference for enable_optin_for_challenges
		DBUtils.updateBusinessFlag(env, expColValue, "false", "enable_optout_for_challenges", b_id);

		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();
		Map<String, String> detailsMap2 = new HashMap<String, String>();
		Map<String, String> detailsMap3 = new HashMap<String, String>();

		Map<String, Map<String, String>> parentMap2 = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap4 = new HashMap<String, String>();
		Map<String, String> detailsMap5 = new HashMap<String, String>();
		Map<String, String> detailsMap6 = new HashMap<String, String>();
		Map<String, String> detailsMap7 = new HashMap<String, String>();
		Map<String, String> detailsMap8 = new HashMap<String, String>();

		// login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */

		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// set conversion rule
		pageObj.menupage().navigateToSubMenuItem("Settings", "Conversion Rules");
		pageObj.settingsPage().clickConversionRule(dataSet.get("conversionRule"));
		pageObj.settingsPage().selectConversionCriteriaInConversionRules("Set", dataSet.get("conversionCriteria"));
		pageObj.settingsPage().clickSaveBtn();

		// Navigate to Multiple Redemption Tab and Uncheck the flags
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Validations");
		pageObj.cockpitRedemptionsPage().updateMinimumPayablePrice("0.01");
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

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
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "16", "",
				"30", "");

		pageObj.utils().logPass("Send redeemable to the user successfully");

		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get().pass("Api2  send reward amount to user is successful");

		// POS Add Discount to Basket
		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAddedForPOSAPIWithExt_Uid(
				dataSet.get("locationKey"), userID, "fuel_reward", "30", externalUID);
		Assert.assertEquals(200, discountBasketResponse.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		pageObj.utils().logit("POS Add Discount to Basket is sucessfull");

		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Fuel1", "2", "30", "M", "10", "999", "1",
				dataSet.get("item_id"));
		parentMap.put("Fuel1", detailsMap1);

		detailsMap2 = pageObj.endpoints().getRecieptDetailsMap("Gas1", "2", "80", "M", "10", "999", "1.1",
				dataSet.get("item_id"));
		parentMap.put("Gas1", detailsMap2);
		detailsMap3 = pageObj.endpoints().getRecieptDetailsMap("Fuel2", "1", "10", "M", "10", "999", "2",
				dataSet.get("item_id_1"));
		parentMap.put("Fuel2", detailsMap3);

		// POS batch redemption
		Response batchRedemptionProcessResponseUser = pageObj.endpoints()
				.processBatchRedemptionOfBasketWithQueryParamPOSAPI(dataSet.get("locationKey"), userID, "107", "true",
						parentMap);
		TestListeners.extentTest.get()
				.info("POS batch redemption response " + batchRedemptionProcessResponseUser.asPrettyString());
		Assert.assertEquals(batchRedemptionProcessResponseUser.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"batch_redemptions status code is not 200 ");
		pageObj.utils().logPass("User process the discount_basket with POS batch redemption is sucessfull");
		String discountAmount = batchRedemptionProcessResponseUser.jsonPath().get("success[0].discount_amount")
				.toString();
		Assert.assertTrue((discountAmount.contains("109.96") || discountAmount.contains("107.0")),
				"Discount amount did not match for User process the discount_basket");
		pageObj.utils().logPass("Verified that Discount amount matched for User process the discount_basket");

		detailsMap4 = pageObj.endpoints().getRecieptDetailsMap("Fuel1", "1", "9", "M", "10", "999", "1", "300");
		parentMap2.put("Fuel1", detailsMap4);

		detailsMap5 = pageObj.endpoints().getRecieptDetailsMap("Gas1", "2", "40", "M", "10", "999", "1.1",
				dataSet.get("item_id"));
		parentMap2.put("Gas1", detailsMap5);

		detailsMap6 = pageObj.endpoints().getRecieptDetailsMap("Gas2", "3", "90", "M", "10", "999", "1.2",
				dataSet.get("item_id"));
		parentMap2.put("Gas2", detailsMap6);

		detailsMap7 = pageObj.endpoints().getRecieptDetailsMap("Fuel2", "1", "10", "M", "10", "999", "2", "401");
		parentMap2.put("Fuel2", detailsMap7);

		detailsMap8 = pageObj.endpoints().getRecieptDetailsMap("Oil3", "1", "10", "M", "10", "999", "3",
				dataSet.get("item_id_1"));
		parentMap2.put("Oil3", detailsMap8);

		Response batchRedemptionProcessResponseUser1 = pageObj.endpoints()
				.processBatchRedemptionOfBasketWithQueryParamPOSAPI(dataSet.get("locationKey"), userID, "159", "true",
						parentMap2);
		Assert.assertEquals(batchRedemptionProcessResponseUser1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"batch_redemptions status code is not 200 ");
		pageObj.utils().logPass("User process the discount_basket with POS batch redemption is sucessfull");
		String discountAmount1 = batchRedemptionProcessResponseUser1.jsonPath().get("success[0].discount_amount")
				.toString();
		Assert.assertEquals(discountAmount1, "129.95",
				"Discount amount did not match for User process the discount_basket");
		pageObj.utils().logPass("Verified that Discount amount matched for User process the discount_basket");

		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Validations");
		pageObj.cockpitRedemptionsPage().updateMinimumPayablePrice("0.0");
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

	}

	@Test(description = "SQ-T4557 Verify fuel_reward redemption when no QC is attached to conversion criteria and user's fuel_reward balance is in decimal value", groups = {
			"regression", "dailyrun" }, priority = 2)
	@Owner(name = "Hardik Bhardwaj")
	public void T4557_FuelReward() throws Exception {

		String b_id = dataSet.get("business_id");

		String preferenceQuery = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id
				+ "'";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, preferenceQuery, "preferences");

		// updating the business preference for enable_optin_for_challenges
		DBUtils.updateBusinessFlag(env, expColValue, "false", "enable_optin_for_challenges", b_id);

		// updating the business preference for enable_optin_for_challenges
		DBUtils.updateBusinessFlag(env, expColValue, "false", "enable_optout_for_challenges", b_id);

		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();
		Map<String, String> detailsMap2 = new HashMap<String, String>();
		Map<String, String> detailsMap3 = new HashMap<String, String>();

		Map<String, Map<String, String>> parentMap2 = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap4 = new HashMap<String, String>();
		Map<String, String> detailsMap5 = new HashMap<String, String>();
		Map<String, String> detailsMap6 = new HashMap<String, String>();
		Map<String, String> detailsMap7 = new HashMap<String, String>();
		Map<String, String> detailsMap8 = new HashMap<String, String>();

		// login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */

		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// set conversion rule
		pageObj.menupage().navigateToSubMenuItem("Settings", "Conversion Rules");
		pageObj.settingsPage().clickConversionRule(dataSet.get("conversionRule"));
		pageObj.settingsPage().selectConversionCriteriaInConversionRules("Clear", "");
		pageObj.settingsPage().clickSaveBtn();

		// Navigate to Multiple Redemption Tab and Uncheck the flags
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Validations");
		pageObj.cockpitRedemptionsPage().updateMinimumPayablePrice("0.01");
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

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
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "16", "",
				"2.14", "");

		pageObj.utils().logPass("Send redeemable to the user successfully");

		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get().pass("Api2  send reward amount to user is successful");

		// POS Add Discount to Basket
		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAddedForPOSAPIWithExt_Uid(
				dataSet.get("locationKey"), userID, "fuel_reward", "2.14", externalUID);
		Assert.assertEquals(200, discountBasketResponse.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		pageObj.utils().logit("POS Add Discount to Basket is sucessfull");

		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Fuel", "20", "54.18", "M", "10", "999", "1",
				dataSet.get("item_id"));
		parentMap.put("Fuel", detailsMap1);

		// POS batch redemption
		Response batchRedemptionProcessResponseUser = pageObj.endpoints()
				.processBatchRedemptionOfBasketWithQueryParamPOSAPI(dataSet.get("locationKey"), userID, "54.18", "true",
						parentMap);
		TestListeners.extentTest.get()
				.info("POS batch redemption response " + batchRedemptionProcessResponseUser.asPrettyString());
		Assert.assertEquals(batchRedemptionProcessResponseUser.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"batch_redemptions status code is not 200 ");
		pageObj.utils().logPass("User process the discount_basket with POS batch redemption is sucessfull");
		String discountAmount = batchRedemptionProcessResponseUser.jsonPath().get("success[0].discount_amount")
				.toString();
		Assert.assertEquals(discountAmount, "42.8",
				"Discount amount did not match for User process the discount_basket");
		pageObj.utils().logPass("Verified that Discount amount matched for User process the discount_basket");

		detailsMap4 = pageObj.endpoints().getRecieptDetailsMap("Fuel1", "1", "9", "M", "10", "999", "1", "300");
		parentMap2.put("Fuel1", detailsMap4);

		detailsMap5 = pageObj.endpoints().getRecieptDetailsMap("Gas1", "1", "10", "M", "10", "999", "1.1",
				dataSet.get("item_id"));
		parentMap2.put("Gas1", detailsMap5);

		detailsMap6 = pageObj.endpoints().getRecieptDetailsMap("Gas2", "1", "5", "M", "10", "999", "1.2",
				dataSet.get("item_id"));
		parentMap2.put("Gas2", detailsMap6);

		detailsMap7 = pageObj.endpoints().getRecieptDetailsMap("Fuel2", "1", "10", "M", "10", "999", "2", "401");
		parentMap2.put("Fuel2", detailsMap7);

		Response batchRedemptionProcessResponseUser1 = pageObj.endpoints()
				.processBatchRedemptionOfBasketWithQueryParamPOSAPI(dataSet.get("locationKey"), userID, "34", "true",
						parentMap2);
		Assert.assertEquals(batchRedemptionProcessResponseUser1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"batch_redemptions status code is not 200 ");
		pageObj.utils().logPass("User process the discount_basket with POS batch redemption is sucessfull");
		String discountAmount1 = batchRedemptionProcessResponseUser1.jsonPath().get("success[0].discount_amount")
				.toString();
		Assert.assertEquals(discountAmount1, "8.56",
				"Discount amount did not match for User process the discount_basket");
		pageObj.utils().logPass("Verified that Discount amount matched for User process the discount_basket");

		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Validations");
		pageObj.cockpitRedemptionsPage().updateMinimumPayablePrice("0.0");
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

	}

	@Test(description = "SQ-T4558 Verify fuel_reward redemption when no QC is attached to conversion criteria and receipt_amount is in decimal and user's fuel_reward balance is in integer", groups = {
			"regression", "dailyrun" }, priority = 3)
	@Owner(name = "Hardik Bhardwaj")
	public void T4558_FuelReward() throws Exception {

		String b_id = dataSet.get("business_id");

		String preferenceQuery = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id
				+ "'";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, preferenceQuery, "preferences");

		// updating the business preference for enable_optin_for_challenges
		DBUtils.updateBusinessFlag(env, expColValue, "false", "enable_optin_for_challenges", b_id);

		// updating the business preference for enable_optin_for_challenges
		DBUtils.updateBusinessFlag(env, expColValue, "false", "enable_optout_for_challenges", b_id);

		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();
		Map<String, String> detailsMap2 = new HashMap<String, String>();
		Map<String, String> detailsMap3 = new HashMap<String, String>();

		Map<String, Map<String, String>> parentMap2 = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap4 = new HashMap<String, String>();
		Map<String, String> detailsMap5 = new HashMap<String, String>();
		Map<String, String> detailsMap6 = new HashMap<String, String>();
		Map<String, String> detailsMap7 = new HashMap<String, String>();
		Map<String, String> detailsMap8 = new HashMap<String, String>();

		// login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */

		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// set conversion rule
		pageObj.menupage().navigateToSubMenuItem("Settings", "Conversion Rules");
		pageObj.settingsPage().clickConversionRule(dataSet.get("conversionRule"));
		pageObj.settingsPage().selectConversionCriteriaInConversionRules("Clear", "");
		pageObj.settingsPage().clickSaveBtn();

		// Navigate to Multiple Redemption Tab and Uncheck the flags
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Validations");
		pageObj.cockpitRedemptionsPage().updateMinimumPayablePrice("0.01");
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

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
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "16", "",
				"10", "");

		pageObj.utils().logPass("Send redeemable to the user successfully");

		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get().pass("Api2  send reward amount to user is successful");

		// POS Add Discount to Basket
		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAddedForPOSAPIWithExt_Uid(
				dataSet.get("locationKey"), userID, "fuel_reward", "10", externalUID);
		Assert.assertEquals(200, discountBasketResponse.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		pageObj.utils().logit("POS Add Discount to Basket is sucessfull");

		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Fuel1", "2", "30.99", "M", "10", "999", "1",
				dataSet.get("item_id"));
		parentMap.put("Fuel1", detailsMap1);

		// POS batch redemption
		Response batchRedemptionProcessResponseUser = pageObj.endpoints()
				.processBatchRedemptionOfBasketWithQueryParamPOSAPI(dataSet.get("locationKey"), userID, "30.99", "true",
						parentMap);
		TestListeners.extentTest.get()
				.info("POS batch redemption response " + batchRedemptionProcessResponseUser.asPrettyString());
		Assert.assertEquals(batchRedemptionProcessResponseUser.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"batch_redemptions status code is not 200 ");
		pageObj.utils().logPass("User process the discount_basket with POS batch redemption is sucessfull");
		String discountAmount = batchRedemptionProcessResponseUser.jsonPath().get("success[0].discount_amount")
				.toString();
		Assert.assertEquals(discountAmount, "20.0",
				"Discount amount did not match for User process the discount_basket");
		pageObj.utils().logPass("Verified that Discount amount matched for User process the discount_basket");

		detailsMap4 = pageObj.endpoints().getRecieptDetailsMap("Fuel1", "2", "30.99", "M", "10", "999", "1", "300");
		parentMap2.put("Fuel1", detailsMap4);

		detailsMap5 = pageObj.endpoints().getRecieptDetailsMap("Gas1", "1", "7.89", "M", "10", "999", "1.1",
				dataSet.get("item_id"));
		parentMap2.put("Gas1", detailsMap5);

		detailsMap6 = pageObj.endpoints().getRecieptDetailsMap("Gas2", "1", "5.43", "M", "10", "999", "1.2",
				dataSet.get("item_id"));
		parentMap2.put("Gas2", detailsMap6);

		Response batchRedemptionProcessResponseUser1 = pageObj.endpoints()
				.processBatchRedemptionOfBasketWithQueryParamPOSAPI(dataSet.get("locationKey"), userID, "44.31", "true",
						parentMap2);
		Assert.assertEquals(batchRedemptionProcessResponseUser1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"batch_redemptions status code is not 200 ");
		pageObj.utils().logPass("User process the discount_basket with POS batch redemption is sucessfull");
		String discountAmount1 = batchRedemptionProcessResponseUser1.jsonPath().get("success[0].discount_amount")
				.toString();
		Assert.assertEquals(discountAmount1, "40.0",
				"Discount amount did not match for User process the discount_basket");
		pageObj.utils().logPass("Verified that Discount amount matched for User process the discount_basket");

		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Validations");
		pageObj.cockpitRedemptionsPage().updateMinimumPayablePrice("0.0");
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();
	}

	@Test(description = "SQ-T4697 [Batched Redemptions] [OMM-878] Verify fuel_reward (having rate rollback QC attached) redemption with discount_amount (having sum of amounts QC attached) and fuel added prior to amount in discount_basket", groups = {
			"regression", "dailyrun" }, priority = 4)
	@Owner(name = "Hardik Bhardwaj")
	public void T4697_FuelReward() throws Exception {

		String b_id = dataSet.get("business_id");

		String preferenceQuery = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id
				+ "'";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, preferenceQuery, "preferences");

		// updating the business preference for enable_optin_for_challenges
		DBUtils.updateBusinessFlag(env, expColValue, "false", "enable_optin_for_challenges", b_id);

		// updating the business preference for enable_optin_for_challenges
		DBUtils.updateBusinessFlag(env, expColValue, "false", "enable_optout_for_challenges", b_id);

		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();
		Map<String, String> detailsMap2 = new HashMap<String, String>();
		Map<String, String> detailsMap3 = new HashMap<String, String>();

		// login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */

		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// set conversion rule
		pageObj.menupage().navigateToSubMenuItem("Settings", "Conversion Rules");
		pageObj.settingsPage().clickConversionRule(dataSet.get("conversionRule"));
		pageObj.settingsPage().selectConversionCriteriaInConversionRules("Set", dataSet.get("conversionCriteria"));
		pageObj.settingsPage().clickSaveBtn();

		// change Existing Qualifier of redeemable
		pageObj.menupage().navigateToSubMenuItem("Settings", "Redeemables");
		pageObj.redeemablePage().searchAndClickOnRedeemable("Base Redeemable");
		pageObj.redeemablePage().removeExistingQualifier();
		pageObj.redeemablePage().addQCinRedeemable(dataSet.get("QcName"));
		pageObj.redeemablePage().clickFinishBtn();

		// Navigate to Multiple Redemption Tab and Uncheck the flags
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Validations");
		pageObj.cockpitRedemptionsPage().updateMinimumPayablePrice("0.01");
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("API2 Signup is successful");

		// Navigate to user
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().messageGiftRewardsToUser(dataSet.get("subject"), "Reward Amount", "100",
				dataSet.get("giftReason"));

		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "Message sent did not displayed on timeline");
		pageObj.utils().logPass("Verified that Success message of Reward Amount send to user ");

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "", "", "10",
				"");
		Assert.assertEquals(sendRewardResponse.getStatusCode(), 201,
				"Status code 201 did not matched for api2 send message to user");
		pageObj.utils().logit("Api2  send reward amount to user is successful");
		pageObj.utils().logit("Send redeemable to the user successfully");

		// POS Add Discount to Basket
		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAddedForPOSAPIWithExt_Uid(
				dataSet.get("locationKey"), userID, "fuel_reward", "10", externalUID);
		Assert.assertEquals(200, discountBasketResponse.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		pageObj.utils().logit("POS Add Discount to Basket is sucessfull");

		// POS Add Discount to Basket
		Response discountBasketResponse1 = pageObj.endpoints().authListDiscountBasketAddedForPOSAPIWithExt_Uid(
				dataSet.get("locationKey"), userID, "discount_amount", "5", externalUID);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponse1.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		pageObj.utils().logit("POS Add Discount to Basket is sucessfull");

		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Fuel1", "1", "8", "M", "10", "999", "1",
				dataSet.get("item_id"));
		parentMap.put("Fuel1", detailsMap1);

		detailsMap2 = pageObj.endpoints().getRecieptDetailsMap("Fuel2", "1", "7", "M", "10", "999", "2",
				dataSet.get("item_id"));
		parentMap.put("Fuel2", detailsMap2);
		detailsMap3 = pageObj.endpoints().getRecieptDetailsMap("Fuel3", "1", "3", "M", "10", "999", "3",
				dataSet.get("item_id_1"));
		parentMap.put("Fuel3", detailsMap3);

		// POS batch redemption
		Response batchRedemptionProcessResponseUser = pageObj.endpoints()
				.processBatchRedemptionOfBasketWithQueryParamPOSAPI(dataSet.get("locationKey"), userID, "18", "true",
						parentMap);
		TestListeners.extentTest.get()
				.info("POS batch redemption response " + batchRedemptionProcessResponseUser.asPrettyString());
		Assert.assertEquals(batchRedemptionProcessResponseUser.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"batch_redemptions status code is not 200 ");
		pageObj.utils().logPass("User process the discount_basket with POS batch redemption is sucessfull");
		String discountAmount = batchRedemptionProcessResponseUser.jsonPath().get("success[0].discount_amount")
				.toString();
		String discountAmount2 = batchRedemptionProcessResponseUser.jsonPath().get("success[1].discount_amount")
				.toString();
		double discountSum = Double.parseDouble(discountAmount) + Double.parseDouble(discountAmount2);
		Assert.assertEquals(discountSum, 14.99, "Discount amount did not match for User process the discount_basket");
		pageObj.utils().logPass("Verified that Discount amount matched for User process the discount_basket");

		// Navigate to Multiple Redemption Tab and Uncheck the flags
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Validations");
		pageObj.cockpitRedemptionsPage().updateMinimumPayablePrice("0.0");
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// change Existing Qualifier of redeemable
		pageObj.menupage().navigateToSubMenuItem("Settings", "Redeemables");
		pageObj.redeemablePage().editRedeemableExistingQualifier(dataSet.get("redeemableName"),
				"Remove Existing Qualifier", dataSet.get("QcName"), "");

	}

	@Test(description = "SQ-T4696 [Batched Redemptions] [OMM-878] Verify fuel_reward redemption when discount_basket contains only fuel_reward having rate rollback QC attached", groups = {
			"regression", "dailyrun" }, priority = 5)
	@Owner(name = "Hardik Bhardwaj")
	public void T4696_FuelReward() throws Exception {

		String b_id = dataSet.get("business_id");

		String preferenceQuery = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id
				+ "'";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, preferenceQuery, "preferences");

		// updating the business preference for enable_optin_for_challenges
		DBUtils.updateBusinessFlag(env, expColValue, "false", "enable_optin_for_challenges", b_id);

		// updating the business preference for enable_optin_for_challenges
		DBUtils.updateBusinessFlag(env, expColValue, "false", "enable_optout_for_challenges", b_id);

		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();
		Map<String, String> detailsMap2 = new HashMap<String, String>();
		Map<String, String> detailsMap3 = new HashMap<String, String>();

		Map<String, Map<String, String>> parentMap2 = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap4 = new HashMap<String, String>();
		Map<String, String> detailsMap5 = new HashMap<String, String>();
		Map<String, String> detailsMap6 = new HashMap<String, String>();
		Map<String, String> detailsMap7 = new HashMap<String, String>();

		// login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */

		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// set conversion rule
		pageObj.menupage().navigateToSubMenuItem("Settings", "Conversion Rules");
		pageObj.settingsPage().clickConversionRule(dataSet.get("conversionRule"));
		pageObj.settingsPage().selectConversionCriteriaInConversionRules("Set", dataSet.get("conversionCriteria"));
		pageObj.settingsPage().clickSaveBtn();

		// Navigate to Multiple Redemption Tab and Uncheck the flags
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Validations");
		pageObj.cockpitRedemptionsPage().updateMinimumPayablePrice("0.01");
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

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
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "16", "",
				"10", "");
		Assert.assertEquals(sendRewardResponse.getStatusCode(), 201,
				"Status code 201 did not matched for api2 send message to user");
		pageObj.utils().logit("Api2  send reward amount to user is successful");
		pageObj.utils().logit("Send redeemable to the user successfully");

		// POS Add Discount to Basket
		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAddedForPOSAPIWithExt_Uid(
				dataSet.get("locationKey"), userID, "fuel_reward", "10", externalUID);
		Assert.assertEquals(200, discountBasketResponse.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		pageObj.utils().logit("POS Add Discount to Basket is sucessfull");

		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Fuel1", "1", "10", "M", "10", "999", "1",
				dataSet.get("item_id"));
		parentMap.put("Fuel1", detailsMap1);

		detailsMap2 = pageObj.endpoints().getRecieptDetailsMap("Fuel2", "1", "10", "M", "10", "999", "2",
				dataSet.get("item_id"));
		parentMap.put("Fuel2", detailsMap2);
		detailsMap3 = pageObj.endpoints().getRecieptDetailsMap("Fuel3", "1", "10", "M", "10", "999", "3",
				dataSet.get("item_id_1"));
		parentMap.put("Fuel3", detailsMap3);

		// POS batch redemption
		Response batchRedemptionProcessResponseUser = pageObj.endpoints()
				.processBatchRedemptionOfBasketWithQueryParamPOSAPI(dataSet.get("locationKey"), userID, "30", "true",
						parentMap);
		TestListeners.extentTest.get()
				.info("POS batch redemption response " + batchRedemptionProcessResponseUser.asPrettyString());
		Assert.assertEquals(batchRedemptionProcessResponseUser.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"batch_redemptions status code is not 200 ");
		pageObj.utils().logPass("User process the discount_basket with POS batch redemption is sucessfull");
		String discountAmount = batchRedemptionProcessResponseUser.jsonPath().get("success[0].discount_amount")
				.toString();
		Assert.assertEquals(discountAmount, "19.98",
				"Discount amount did not match for User process the discount_basket");
		pageObj.utils().logPass("Verified that Discount amount matched for User process the discount_basket");

		detailsMap4 = pageObj.endpoints().getRecieptDetailsMap("Fuel1", "1", "15", "M", "10", "999", "1",
				dataSet.get("item_id"));
		parentMap2.put("Fuel1", detailsMap4);

		detailsMap5 = pageObj.endpoints().getRecieptDetailsMap("Oil1", "1", "5", "M", "10", "999", "1.1",
				dataSet.get("item_id"));
		parentMap2.put("Oil1", detailsMap5);
		detailsMap6 = pageObj.endpoints().getRecieptDetailsMap("Fuel2", "1", "20", "M", "10", "999", "2",
				dataSet.get("item_id"));
		parentMap2.put("Fuel2", detailsMap6);
		detailsMap7 = pageObj.endpoints().getRecieptDetailsMap("Fuel3", "1", "10", "M", "10", "999", "3",
				dataSet.get("item_id_1"));
		parentMap2.put("Fuel3", detailsMap7);

		// POS batch redemption
		Response batchRedemptionProcessResponseUser1 = pageObj.endpoints()
				.processBatchRedemptionOfBasketWithQueryParamPOSAPI(dataSet.get("locationKey"), userID, "50", "true",
						parentMap2);
		Assert.assertEquals(batchRedemptionProcessResponseUser1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"batch_redemptions status code is not 200 ");
		pageObj.utils().logPass("User process the discount_basket with POS batch redemption is sucessfull");
		String discountAmount1 = batchRedemptionProcessResponseUser1.jsonPath().get("success[0].discount_amount")
				.toString();
		Assert.assertEquals(discountAmount1, "30.0",
				"Discount amount did not match for User process the discount_basket");
		pageObj.utils().logPass("Verified that Discount amount matched for User process the discount_basket");

		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Validations");
		pageObj.cockpitRedemptionsPage().updateMinimumPayablePrice("0.0");
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

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