package com.punchh.server.OMMTest;

import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.HashMap;
import java.util.LinkedHashMap;
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

import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;

import com.punchh.server.utilities.TestListeners;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.apiConfig.*;

import io.restassured.response.Response;
import jline.internal.Log;

@Listeners(TestListeners.class)

public class OMM530FuelRewardTest {

	static Logger logger = LogManager.getLogger(OMM530FuelRewardTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String sTCName;
	private String env, run = "ui";
	private String baseUrl;
	private static Map<String, String> dataSet;
	private String userEmail;
	ApiPayloads apipaylods;
	private List<String> codeNameList;

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
		driver = new BrowserUtilities().launchBrowser();
		sTCName = method.getName();
		pageObj = new PageObj(driver);
		apipaylods = new ApiPayloads();
		env = pageObj.getEnvDetails().setEnv();
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run , env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run , env , "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==>" + dataSet);

	}

	@Test(description = "Error message appears if fuel_reward is honoured via 1.0 prior to processing the basket", priority = 0, groups = {
			"OMMFeatureTesting" }, enabled = true)
	public void validateOMMT355() throws InterruptedException {

		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Navigate to Multiple Redemption Tab and Uncheck the flags
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");

		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_discount_locking", "uncheck");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_auto_unlock", "uncheck");
		pageObj.cockpitRedemptionsPage().updateTotalNumberOfRedemptionsAllowed("2");
		pageObj.dashboardpage().updateCheckBox();

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponseUser = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponseUser, "API 2 user signup");
		String token = signUpResponseUser.jsonPath().get("access_token.token").toString();
		System.out.println("tokenUser1=" + token);

		String userID = signUpResponseUser.jsonPath().get("user.user_id").toString();
		System.out.println("userIDUser1=" + userID);

		// Navigate to user timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		// Click message gift and gift fuel discount
		pageObj.guestTimelinePage().messageFuelDiscountToUser(dataSet.get("subject"), dataSet.get("giftTypes"),
				dataSet.get("fuelPoints"), dataSet.get("giftReason"));
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		boolean fuelGiftedNotification = pageObj.guestTimelinePage().verifyfuelGiftedNotification();
		Assert.assertTrue(status, "Message sent did not displayed on timeline");
		Assert.assertTrue(fuelGiftedNotification, "Fuel gifted notification did not displayed on timeline");

		// Add fuel reward in discount basket
		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAddedForPOSAPI(
				dataSet.get("locationKey"), userID, "fuel_reward", dataSet.get("fuelPoints"));
		Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String actual_discount_type_addBasketResponse = discountBasketResponse.jsonPath()
				.getString("discount_basket_items[0].discount_type");
		Log.info(actual_discount_type_addBasketResponse);
		Assert.assertEquals(actual_discount_type_addBasketResponse, "fuel_reward");

		// POS redemption api
		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key = CreateDateTime.getTimeDateString();
		Response resp = pageObj.endpoints().posRedemptionOfFuel(userEmail, date, key, txn, dataSet.get("locationKey"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp.getStatusCode(), "Status code 200 did not matched for pos redemption api");

		// Process the basket
		Response batchRedemptionProcessResponse = pageObj.endpoints().processBatchRedemptionOfBasketAUTHAPI(
				dataSet.get("client"), dataSet.get("secret"), dataSet.get("locationKey"), token, userID,
				dataSet.get("item_id"));
		Assert.assertEquals(batchRedemptionProcessResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		System.out.println("batchRedemptionProcessResponseUser=" + batchRedemptionProcessResponse.asPrettyString());
		String actualErrorMessage = batchRedemptionProcessResponse.jsonPath().getString("failures[0].message[0]");
		Log.info(actualErrorMessage);
		String expectedErrorMessage = dataSet.get("expectedFailureMessage");

		// Verify the error message
		Assert.assertEquals(actualErrorMessage, expectedErrorMessage);

	}

	@Test(description = "Verify 'Total number of redemptions allowed' limit", priority = 1, groups = {
			"OMMFeatureTesting" }, enabled = true)
	public void validateOMMT358() throws InterruptedException {

		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponseUser = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponseUser, "API 2 user signup");
		String token = signUpResponseUser.jsonPath().get("access_token.token").toString();
		System.out.println("tokenUser1=" + token);

		String userID = signUpResponseUser.jsonPath().get("user.user_id").toString();
		System.out.println("userIDUser1=" + userID);

		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Update Total number of Redemptions to 1
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.cockpitRedemptionsPage().updateTotalNumberOfRedemptionsAllowed("2");
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// Check redeemable's availability in business and create redeemable if not
		// available
		pageObj.menupage().navigateToSubMenuItem("Settings", "Redeemables");
		pageObj.redeemablePage().createRedeemableIfNotExistWithExistingQC(dataSet.get("redeemable"), "Flat Discount",
				"", "2.0");

		// Navigate to user timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		// Click message gift and gift reward
		pageObj.guestTimelinePage().messageGiftToUser(dataSet.get("subject"), dataSet.get("reward"),
				dataSet.get("redeemable"), dataSet.get("giftReason"));
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		String rewardName = pageObj.guestTimelinePage().getRewardNme();
		logger.info(rewardName);
		Assert.assertTrue(status, "Message sent did not displayed on timeline");
		Assert.assertEquals(rewardName, "Rewarded Redeemable - T358");

		// click message gift and gift fuel discount
		pageObj.guestTimelinePage().messageFuelDiscountToUser(dataSet.get("subject"), dataSet.get("giftTypes"),
				dataSet.get("fuelPoints"), dataSet.get("giftReason"));
		status = pageObj.campaignspage().validateSuccessMessage();
		boolean fuelGiftedNotification = pageObj.guestTimelinePage().verifyfuelGiftedNotification();
		Assert.assertTrue(status, "Message sent did not displayed on timeline");
		Assert.assertTrue(fuelGiftedNotification, "Fuel gifted notification did not displayed on timeline");

		// fetch user offers using ap2 mobileOffers
		Response offerResponse = pageObj.endpoints().getUserOffers(token, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, offerResponse.getStatusCode(), "Status code 200 did not matched for api2 user offers");
		String reward_id = offerResponse.jsonPath().get("rewards[0].reward_id").toString();
		logger.info(reward_id);

		// Add reward in discount basket
		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", reward_id);
		Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		pageObj.utils().logPass(reward_id + " rewardid is added to the basket ");

		// Add fuel reward in discount basket
		Response discountBasketResponse1 = pageObj.endpoints().authListDiscountBasketAddedForPOSAPI(
				dataSet.get("locationKey"), userID, "fuel_reward", dataSet.get("fuelPoints"));
		Assert.assertEquals(discountBasketResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String actual_discount_type_addBasketResponse = discountBasketResponse1.jsonPath()
				.getString("discount_basket_items[1].discount_type");
		Log.info(actual_discount_type_addBasketResponse);
		Assert.assertEquals(actual_discount_type_addBasketResponse, "fuel_reward");

		// Update Total number of Redemptions to 1
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.cockpitRedemptionsPage().updateTotalNumberOfRedemptionsAllowed("1");
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// Process the basket
		Response batchRedemptionProcessResponse = pageObj.endpoints().processBatchRedemptionOfBasketAUTHAPI(
				dataSet.get("client"), dataSet.get("secret"), dataSet.get("locationKey"), token, userID,
				dataSet.get("item_id"));
		Assert.assertEquals(batchRedemptionProcessResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY);
		System.out.println("batchRedemptionProcessResponse=" + batchRedemptionProcessResponse.asPrettyString());
		String actualErrorMessage = batchRedemptionProcessResponse.jsonPath().getString("error").replace("[", "")
				.replace("]", "");
		String expectedErrorMessage = dataSet.get("expectedFailureMessage");

		// Verify the error message
		Assert.assertEquals(actualErrorMessage, expectedErrorMessage);

	}

	@Test(description = "Verify discount_basket is processed when 'Total number of redemptions allowed' limit is fulfilled", priority = 2, groups = {
			"OMMFeatureTesting" }, enabled = true)
	public void validateOMMT2173() throws InterruptedException {

		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Navigate to Multiple Redemption Tab and Uncheck the flags
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_discount_locking", "uncheck");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_auto_unlock", "uncheck");
		pageObj.cockpitRedemptionsPage().updateTotalNumberOfRedemptionsAllowed("2");
		pageObj.dashboardpage().updateCheckBox();

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponseUser = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponseUser, "API 2 user signup");
		String token = signUpResponseUser.jsonPath().get("access_token.token").toString();
		System.out.println("token=" + token);

		String userID = signUpResponseUser.jsonPath().get("user.user_id").toString();
		System.out.println("userID=" + userID);

		// Navigate to user timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		// click message gift and gift fuel discount
		pageObj.guestTimelinePage().messageFuelDiscountToUser(dataSet.get("subject"), dataSet.get("giftTypes"),
				dataSet.get("fuelPoints"), dataSet.get("giftReason"));
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		boolean fuelGiftedNotification = pageObj.guestTimelinePage().verifyfuelGiftedNotification();
		Assert.assertTrue(status, "Message sent did not displayed on timeline");
		Assert.assertTrue(fuelGiftedNotification, "Fuel gifted notification did not displayed on timeline");

		// Add fuel reward in discount basket
		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAddedForPOSAPI(
				dataSet.get("locationKey"), userID, "fuel_reward", dataSet.get("fuelPoints"));
		Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String actual_discount_type_addBasketResponse = discountBasketResponse.jsonPath()
				.getString("discount_basket_items[0].discount_type");
		Log.info(actual_discount_type_addBasketResponse);
		Assert.assertEquals(actual_discount_type_addBasketResponse, "fuel_reward");

		// Process the basket
		Response batchRedemptionProcessResponse = pageObj.endpoints().processAuthBatchRedemptionUsingStoreNum(
				dataSet.get("client"), dataSet.get("secret"), dataSet.get("store_num"), token, userID,
				dataSet.get("item_id"));
		Assert.assertEquals(batchRedemptionProcessResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		System.out.println("batchRedemptionProcessResponseUser=" + batchRedemptionProcessResponse.asPrettyString());
		String actualDiscountType = batchRedemptionProcessResponse.jsonPath().getString("success[0].discount_type");
		String actualQualifiedType = batchRedemptionProcessResponse.jsonPath().getString("success[0].qualified");
		String actualDiscountAmt = batchRedemptionProcessResponse.jsonPath().getString("success[0].discount_amount");

		// Verify the response
		Assert.assertEquals(actualDiscountType, dataSet.get("expectedDiscountType"));
		Assert.assertEquals(actualQualifiedType, dataSet.get("expectedQualifiedType"));
		Assert.assertTrue(Float.parseFloat(actualDiscountAmt) > 1.0);

	}

	@Test(description = "Verify Acquisition Type limit gets applied on discount_basket containing fuel_reward when processing the discount_basket", priority = 3, groups = {
			"OMMFeatureTesting" }, enabled = true)
	public void validateOMMT2174() throws InterruptedException, ParseException {

		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Navigate to Multiple Redemption Tab and Uncheck the flags
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_discount_locking", "uncheck");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_auto_unlock", "uncheck");
		pageObj.cockpitRedemptionsPage().updateTotalNumberOfRedemptionsAllowed("10");

		pageObj.cockpitRedemptionsPage().clearAcquisitionTypeData("Offers");
		pageObj.cockpitRedemptionsPage().clearAcquisitionTypeData("Earned Rewards");
		pageObj.cockpitRedemptionsPage().clearAcquisitionTypeData("Pre-Purchased Discount");
		pageObj.cockpitRedemptionsPage().clearAcquisitionTypeData("Coupons & Promos");
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.cockpitRedemptionsPage().setAcquisitionType("Earned Rewards", "2");
		pageObj.cockpitRedemptionsPage().setAcquisitionType("Coupons & Promos", "1");
		pageObj.cockpitRedemptionsPage().setAcquisitionType("Offers", "1");
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		// Select offer dropdown value and create coupon campaign
		pageObj.campaignspage().createCouponCampaignIfNotExist(dataSet.get("campaign_name"), "POS", "PreGenerated",
				dataSet.get("noOfGuests"), "1", "$ OFF", "10", "", "", false, 0, "", "", "");

		pageObj.campaignspage().searchCampaign(dataSet.get("campaign_name"));
		codeNameList = pageObj.campaignspage().getPreGeneratedCuponList();
		String redemption_code = codeNameList.get(0).toString();

		// Check redeemable's availability in business and create redeemable if not
		// available
		pageObj.menupage().navigateToSubMenuItem("Settings", "Redeemables");
		pageObj.redeemablePage().createRedeemableIfNotExistWithExistingQC(dataSet.get("redeemable"), "Flat Discount",
				"", "2.0");
		String redeemable_id = pageObj.redeemablePage().getRedeemableID(dataSet.get("redeemable"));

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponseUser = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponseUser, "API 2 user signup");
		String token = signUpResponseUser.jsonPath().get("access_token.token").toString();
		System.out.println("token=" + token);

		String userID = signUpResponseUser.jsonPath().get("user.user_id").toString();
		System.out.println("userID=" + userID);

		// Gift Reedemable to User
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				redeemable_id);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		pageObj.utils().logPass("Api2  send reward reedemable to user is successful");

		// Get reward_id
		Response rewardResponse = pageObj.endpoints().authListAvailableRewardsNew(token, dataSet.get("client"),
				dataSet.get("secret"));
		String rewardID = rewardResponse.jsonPath().getString("id").replace("[", "").replace("]", "");
		pageObj.utils().logPass("Reward id " + rewardID + " is generated successfully ");

		// Navigate to user timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		// click message gift and gift fuel discount
		pageObj.guestTimelinePage().messageFuelDiscountToUser(dataSet.get("subject"), dataSet.get("giftTypes"),
				dataSet.get("fuelPoints"), dataSet.get("giftReason"));
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		boolean fuelGiftedNotification = pageObj.guestTimelinePage().verifyfuelGiftedNotification();
		Assert.assertTrue(status, "Message sent did not displayed on timeline");
		Assert.assertTrue(fuelGiftedNotification, "Fuel gifted notification did not displayed on timeline");

		// send reward amount to user Amount
		Response sendRewardAmountResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"),
				dataSet.get("amount"), "");
		pageObj.utils().logPass("Send reward amount to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardAmountResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		// Add discount types to discount basket
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> discount1 = new HashMap<String, String>();
		Map<String, String> discount2 = new HashMap<String, String>();
		Map<String, String> discount3 = new HashMap<String, String>();
		Map<String, String> discount4 = new HashMap<String, String>();

		discount1 = pageObj.endpoints().getDiscountDetailsMapExceptDiscountAmountFuel("reward", rewardID);
		parentMap.put("discount1", discount1);

		discount2 = pageObj.endpoints().getDiscountDetailsMapExceptDiscountAmountFuel("redemption_code",
				redemption_code);
		parentMap.put("discount2", discount2);

		discount3 = pageObj.endpoints().getDiscountDetailsMapForDiscountAmountFuel("discount_amount", "1");
		parentMap.put("discount3", discount3);

		discount4 = pageObj.endpoints().getDiscountDetailsMapForDiscountAmountFuel("fuel_reward", "1");
		parentMap.put("discount4", discount4);

		Response discountBasketResponse = pageObj.endpoints().mobileDiscountBasketforMultipleDiscountTypes(token,
				dataSet.get("client"), dataSet.get("secret"), parentMap);
		Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		System.out.println("discountBasketResponse=" + discountBasketResponse.asPrettyString());

		// change Acquisition Type -> Earned Rewards -> 1
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_discount_locking", "uncheck");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_auto_unlock", "uncheck");
		pageObj.cockpitRedemptionsPage().updateTotalNumberOfRedemptionsAllowed("10");

		pageObj.cockpitRedemptionsPage().clearAcquisitionTypeData("Offers");
		pageObj.cockpitRedemptionsPage().clearAcquisitionTypeData("Earned Rewards");
		pageObj.cockpitRedemptionsPage().clearAcquisitionTypeData("Pre-Purchased Discount");
		pageObj.cockpitRedemptionsPage().clearAcquisitionTypeData("Coupons & Promos");
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.cockpitRedemptionsPage().setAcquisitionType("Earned Rewards", "1");
		pageObj.cockpitRedemptionsPage().setAcquisitionType("Coupons & Promos", "1");
		pageObj.cockpitRedemptionsPage().setAcquisitionType("Offers", "1");
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// Process the basket
		Response batchRedemptionProcessResponse = pageObj.endpoints().processAuthBatchRedemptionUsingStoreNum(
				dataSet.get("client"), dataSet.get("secret"), dataSet.get("store_num"), token, userID,
				dataSet.get("item_id"));

		System.out.println("batchRedemptionProcessResponseUser=" + batchRedemptionProcessResponse.asPrettyString());
		String actualErrorMessage = batchRedemptionProcessResponse.jsonPath().getString("error").replace("[", "")
				.replace("]", "");

		// Verify the response
		Assert.assertEquals(batchRedemptionProcessResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY);
		Assert.assertEquals(actualErrorMessage, dataSet.get("expectedFailureMessage"));

	}

	@Test(description = "Verify discount_basket is processed when Acquisition Type limit gets fulfilled", priority = 4, groups = {
			"OMMFeatureTesting" }, enabled = true)
	public void validateOMMT2175() throws InterruptedException {

		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Navigate to Multiple Redemption Tab and Uncheck the flags
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_discount_locking", "uncheck");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_auto_unlock", "uncheck");
		pageObj.cockpitRedemptionsPage().updateTotalNumberOfRedemptionsAllowed("10");

		pageObj.cockpitRedemptionsPage().clearAcquisitionTypeData("Offers");
		pageObj.cockpitRedemptionsPage().clearAcquisitionTypeData("Earned Rewards");
		pageObj.cockpitRedemptionsPage().clearAcquisitionTypeData("Pre-Purchased Discount");
		pageObj.cockpitRedemptionsPage().clearAcquisitionTypeData("Coupons & Promos");
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.cockpitRedemptionsPage().setAcquisitionType("Earned Rewards", "2");
		pageObj.cockpitRedemptionsPage().setAcquisitionType("Coupons & Promos", "1");
		pageObj.cockpitRedemptionsPage().setAcquisitionType("Offers", "1");
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		// Select offer dropdown value and create coupon campaign
		pageObj.campaignspage().createCouponCampaignIfNotExist(dataSet.get("campaign_name"), "POS", "PreGenerated",
				dataSet.get("noOfGuests"), "1", "$ OFF", "10", "", "", false, 0, "", "", "");

		pageObj.campaignspage().searchCampaign(dataSet.get("campaign_name"));
		codeNameList = pageObj.campaignspage().getPreGeneratedCuponList();
		String redemption_code = codeNameList.get(0).toString();

		// Check redeemable's availability in business and create redeemable if not
		// available
		pageObj.menupage().navigateToSubMenuItem("Settings", "Redeemables");
		pageObj.redeemablePage().createRedeemableIfNotExistWithExistingQC(dataSet.get("redeemable"), "Flat Discount",
				"", "2.0");
		String redeemable_id = pageObj.redeemablePage().getRedeemableID(dataSet.get("redeemable"));

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponseUser = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponseUser, "API 2 user signup");
		String token = signUpResponseUser.jsonPath().get("access_token.token").toString();
		System.out.println("token=" + token);

		String userID = signUpResponseUser.jsonPath().get("user.user_id").toString();
		System.out.println("userID=" + userID);

		// Gift Reedemable to User
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				redeemable_id);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		pageObj.utils().logPass("Api2  send reward reedemable to user is successful");

		// Get reward_id
		Response rewardResponse = pageObj.endpoints().authListAvailableRewardsNew(token, dataSet.get("client"),
				dataSet.get("secret"));
		String rewardID = rewardResponse.jsonPath().getString("id").replace("[", "").replace("]", "");
		pageObj.utils().logPass("Reward id " + rewardID + " is generated successfully ");

		// Navigate to User timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		// click message gift and gift fuel discount
		pageObj.guestTimelinePage().messageFuelDiscountToUser(dataSet.get("subject"), dataSet.get("giftTypes"),
				dataSet.get("fuelPoints"), dataSet.get("giftReason"));
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		boolean fuelGiftedNotification = pageObj.guestTimelinePage().verifyfuelGiftedNotification();
		Assert.assertTrue(status, "Message sent did not displayed on timeline");
		Assert.assertTrue(fuelGiftedNotification, "Fuel gifted notification did not displayed on timeline");

		// send reward amount to user Amount
		Response sendRewardAmountResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"),
				dataSet.get("amount"), "");
		pageObj.utils().logPass("Send reward amount to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardAmountResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		// Add discount type to discount basket
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> discount1 = new HashMap<String, String>();
		Map<String, String> discount2 = new HashMap<String, String>();
		Map<String, String> discount3 = new HashMap<String, String>();
		Map<String, String> discount4 = new HashMap<String, String>();

		discount1 = pageObj.endpoints().getDiscountDetailsMapExceptDiscountAmountFuel("reward", rewardID);
		parentMap.put("discount1", discount1);

		discount2 = pageObj.endpoints().getDiscountDetailsMapExceptDiscountAmountFuel("redemption_code",
				redemption_code);
		parentMap.put("discount2", discount2);

		discount3 = pageObj.endpoints().getDiscountDetailsMapForDiscountAmountFuel("discount_amount", "1");
		parentMap.put("discount3", discount3);

		discount4 = pageObj.endpoints().getDiscountDetailsMapForDiscountAmountFuel("fuel_reward", "1");
		parentMap.put("discount4", discount4);

		Response discountBasketResponse = pageObj.endpoints().mobileDiscountBasketforMultipleDiscountTypes(token,
				dataSet.get("client"), dataSet.get("secret"), parentMap);
		Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		System.out.println("discountBasketResponse=" + discountBasketResponse.asPrettyString());

		// Process the basket
		Response batchRedemptionProcessResponse = pageObj.endpoints().processAuthBatchRedemptionUsingStoreNum(
				dataSet.get("client"), dataSet.get("secret"), dataSet.get("store_num"), token, userID,
				dataSet.get("item_id"));

		System.out.println("batchRedemptionProcessResponseUser=" + batchRedemptionProcessResponse.asPrettyString());
		String actualQualifiedType = batchRedemptionProcessResponse.jsonPath().getString("success[0].qualified");
		String actualDiscountAmt = batchRedemptionProcessResponse.jsonPath().getString("success[0].discount_amount");

		// Verify the response
		Assert.assertEquals(batchRedemptionProcessResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertEquals(actualQualifiedType, dataSet.get("expectedQualifiedType"));
		Assert.assertTrue(Float.parseFloat(actualDiscountAmt) > 1.0);

	}

	@Test(description = "Verify Discount Type priority when processing discount_basket containing fuel_reward", priority = 5, groups = {
			"OMMFeatureTesting" }, enabled = true)
	public void validateOMMT2176() throws InterruptedException {

		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Navigate to Multiple Redemption Tab and Uncheck the flags
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_discount_locking", "uncheck");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_auto_unlock", "uncheck");
		pageObj.cockpitRedemptionsPage().updateTotalNumberOfRedemptionsAllowed("10");

		// Set Processing priority by discount types
		pageObj.cockpitRedemptionsPage().clearProcessingPriorityByDiscountType();
		pageObj.cockpitRedemptionsPage().setProcessingPriorityByDiscountType("Fuel discount");
		pageObj.cockpitRedemptionsPage().setProcessingPriorityByDiscountType("Discount Amount");
		pageObj.cockpitRedemptionsPage().setProcessingPriorityByDiscountType("Coupon/Promos");
		pageObj.cockpitRedemptionsPage().setProcessingPriorityByDiscountType("Rewards");
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		// Select offer dropdown value and create coupon campaign
		pageObj.campaignspage().createCouponCampaignIfNotExist(dataSet.get("campaign_name"), "POS", "PreGenerated",
				dataSet.get("noOfGuests"), "1", "$ OFF", "1", "", "", false, 0, "", "", "");

		pageObj.campaignspage().searchCampaign(dataSet.get("campaign_name"));
		codeNameList = pageObj.campaignspage().getPreGeneratedCuponList();
		String redemption_code = codeNameList.get(0).toString();

		// Check redeemable's availability in business and create redeemable if not
		// available
		pageObj.menupage().navigateToSubMenuItem("Settings", "Redeemables");
		pageObj.redeemablePage().createRedeemableIfNotExistWithExistingQC(dataSet.get("redeemable"), "Flat Discount",
				"", "1.0");
		String redeemable_id = pageObj.redeemablePage().getRedeemableID(dataSet.get("redeemable"));

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponseUser = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponseUser, "API 2 user signup");
		String token = signUpResponseUser.jsonPath().get("access_token.token").toString();
		System.out.println("token=" + token);

		String userID = signUpResponseUser.jsonPath().get("user.user_id").toString();
		System.out.println("userID=" + userID);

		// Gift Reedemable to User
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				redeemable_id);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		pageObj.utils().logPass("Api2  send reward reedemable to user is successful");

		// Get reward_id
		Response rewardResponse = pageObj.endpoints().authListAvailableRewardsNew(token, dataSet.get("client"),
				dataSet.get("secret"));
		String rewardID = rewardResponse.jsonPath().getString("id").replace("[", "").replace("]", "");
		pageObj.utils().logPass("Reward id " + rewardID + " is generated successfully ");

		// Navigate to User timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		// click message gift and gift fuel discount
		pageObj.guestTimelinePage().messageFuelDiscountToUser(dataSet.get("subject"), dataSet.get("giftTypes"),
				dataSet.get("fuelPoints"), dataSet.get("giftReason"));
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		boolean fuelGiftedNotification = pageObj.guestTimelinePage().verifyfuelGiftedNotification();
		Assert.assertTrue(status, "Message sent did not displayed on timeline");
		Assert.assertTrue(fuelGiftedNotification, "Fuel gifted notification did not displayed on timeline");

		// send reward amount to user Amount
		Response sendRewardAmountResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"),
				dataSet.get("amount"), "");
		pageObj.utils().logPass("Send reward amount to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardAmountResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		// Add discount type to discount basket
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> discount1 = new HashMap<String, String>();
		Map<String, String> discount2 = new HashMap<String, String>();
		Map<String, String> discount3 = new HashMap<String, String>();
		Map<String, String> discount4 = new HashMap<String, String>();

		discount1 = pageObj.endpoints().getDiscountDetailsMapForDiscountAmountFuel("fuel_reward", "1");
		parentMap.put("discount1", discount1);

		discount2 = pageObj.endpoints().getDiscountDetailsMapForDiscountAmountFuel("discount_amount", "1");
		parentMap.put("discount2", discount2);

		discount3 = pageObj.endpoints().getDiscountDetailsMapExceptDiscountAmountFuel("redemption_code",
				redemption_code);
		parentMap.put("discount3", discount3);

		discount4 = pageObj.endpoints().getDiscountDetailsMapExceptDiscountAmountFuel("reward", rewardID);
		parentMap.put("discount4", discount4);

		// Add discount types in basket
		Response discountBasketResponse = pageObj.endpoints().mobileDiscountBasketforMultipleDiscountTypes(token,
				dataSet.get("client"), dataSet.get("secret"), parentMap);
		Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		System.out.println("discountBasketResponse=" + discountBasketResponse.asPrettyString());

		// Process the basket
		Response batchRedemptionProcessResponse = pageObj.endpoints().processAuthBatchRedemptionUsingStoreNum(
				dataSet.get("client"), dataSet.get("secret"), dataSet.get("store_num"), token, userID,
				dataSet.get("item_id"));

		System.out.println("batchRedemptionProcessResponseUser=" + batchRedemptionProcessResponse.asPrettyString());

		String actualQualifiedType = batchRedemptionProcessResponse.jsonPath().getString("success[0].qualified");
		String discountTypeProcessedFirst = batchRedemptionProcessResponse.jsonPath()
				.getString("success[0].discount_type");
		String discountTypeProcessedSecond = batchRedemptionProcessResponse.jsonPath()
				.getString("success[1].discount_type");
		String discountTypeProcessedThird = batchRedemptionProcessResponse.jsonPath()
				.getString("success[2].discount_type");
		String discountTypeProcessedFourth = batchRedemptionProcessResponse.jsonPath()
				.getString("success[3].discount_type");

		// Verify the response
		Assert.assertEquals(batchRedemptionProcessResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertEquals(actualQualifiedType, dataSet.get("expectedQualifiedType"));
		Assert.assertEquals(discountTypeProcessedFirst, "fuel_reward");
		Assert.assertEquals(discountTypeProcessedSecond, "discount_amount");
		Assert.assertEquals(discountTypeProcessedThird, "redemption_code");
		Assert.assertEquals(discountTypeProcessedFourth, "reward");

	}

	@Test(description = "Verify Acquisition Type priority when processing discount_basket containing fuel_reward", priority = 6, groups = {
			"OMMFeatureTesting" }, enabled = true)
	public void validateOMMT2177() throws InterruptedException {

		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Navigate to Multiple Redemption Tab and Uncheck the flags
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_discount_locking", "uncheck");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_auto_unlock", "uncheck");
		pageObj.cockpitRedemptionsPage().updateTotalNumberOfRedemptionsAllowed("10");

		// Set Processing priority by discount types
		pageObj.cockpitRedemptionsPage().clearProcessingPriorityByDiscountType();

		// Set redemption processing priority by Acquisition Type
		pageObj.cockpitRedemptionsPage().clearAcquisitionTypeData("Offers");
		pageObj.cockpitRedemptionsPage().clearAcquisitionTypeData("Earned Rewards");
		pageObj.cockpitRedemptionsPage().clearAcquisitionTypeData("Pre-Purchased Discount");
		pageObj.cockpitRedemptionsPage().clearAcquisitionTypeData("Coupons & Promos");
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.cockpitRedemptionsPage().setAcquisitionType("Offers", "1");
		pageObj.cockpitRedemptionsPage().setAcquisitionType("Earned Rewards", "2");
		pageObj.cockpitRedemptionsPage().setAcquisitionType("Coupons & Promos", "1");
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		// Select offer dropdown value and create coupon campaign
		pageObj.campaignspage().createCouponCampaignIfNotExist(dataSet.get("campaign_name"), "POS", "PreGenerated",
				dataSet.get("noOfGuests"), "1", "$ OFF", "1", "", "", false, 0, "", "", "");

		pageObj.campaignspage().searchCampaign(dataSet.get("campaign_name"));
		codeNameList = pageObj.campaignspage().getPreGeneratedCuponList();
		String redemption_code = codeNameList.get(0).toString();

		// Check redeemable's availability in business and create redeemable if not
		// available
		pageObj.menupage().navigateToSubMenuItem("Settings", "Redeemables");
		pageObj.redeemablePage().createRedeemableIfNotExistWithExistingQC(dataSet.get("redeemable"), "Flat Discount",
				"", "1.0");
		String redeemable_id = pageObj.redeemablePage().getRedeemableID(dataSet.get("redeemable"));

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponseUser = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponseUser, "API 2 user signup");
		String token = signUpResponseUser.jsonPath().get("access_token.token").toString();
		System.out.println("token=" + token);

		String userID = signUpResponseUser.jsonPath().get("user.user_id").toString();
		System.out.println("userID=" + userID);

		// Gift Reedemable to User
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				redeemable_id);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		pageObj.utils().logPass("Api2  send reward reedemable to user is successful");

		// Get reward_id
		Response rewardResponse = pageObj.endpoints().authListAvailableRewardsNew(token, dataSet.get("client"),
				dataSet.get("secret"));
		String rewardID = rewardResponse.jsonPath().getString("id").replace("[", "").replace("]", "");
		pageObj.utils().logPass("Reward id " + rewardID + " is generated successfully ");

		// Navigate to User timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		// click message gift and gift fuel discount
		pageObj.guestTimelinePage().messageFuelDiscountToUser(dataSet.get("subject"), dataSet.get("giftTypes"),
				dataSet.get("fuelPoints"), dataSet.get("giftReason"));
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		boolean fuelGiftedNotification = pageObj.guestTimelinePage().verifyfuelGiftedNotification();
		Assert.assertTrue(status, "Message sent did not displayed on timeline");
		Assert.assertTrue(fuelGiftedNotification, "Fuel gifted notification did not displayed on timeline");

		// send reward amount to user Amount
		Response sendRewardAmountResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"),
				dataSet.get("amount"), "");
		pageObj.utils().logPass("Send reward amount to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardAmountResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		// Add discount type to discount basket
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> discount1 = new HashMap<String, String>();
		Map<String, String> discount2 = new HashMap<String, String>();
		Map<String, String> discount3 = new HashMap<String, String>();
		Map<String, String> discount4 = new HashMap<String, String>();

		discount1 = pageObj.endpoints().getDiscountDetailsMapExceptDiscountAmountFuel("redemption_code",
				redemption_code);
		parentMap.put("discount1", discount1);

		discount2 = pageObj.endpoints().getDiscountDetailsMapExceptDiscountAmountFuel("reward", rewardID);
		parentMap.put("discount2", discount2);

		discount3 = pageObj.endpoints().getDiscountDetailsMapForDiscountAmountFuel("discount_amount", "1");
		parentMap.put("discount3", discount3);

		discount4 = pageObj.endpoints().getDiscountDetailsMapForDiscountAmountFuel("fuel_reward", "1");
		parentMap.put("discount4", discount4);

		// Add discount types in basket
		Response discountBasketResponse = pageObj.endpoints().mobileDiscountBasketforMultipleDiscountTypes(token,
				dataSet.get("client"), dataSet.get("secret"), parentMap);
		Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		System.out.println("discountBasketResponse=" + discountBasketResponse.asPrettyString());

		// Process the basket
		Response batchRedemptionProcessResponse = pageObj.endpoints().processAuthBatchRedemptionUsingStoreNum(
				dataSet.get("client"), dataSet.get("secret"), dataSet.get("store_num"), token, userID,
				dataSet.get("item_id"));

		System.out.println("batchRedemptionProcessResponseUser=" + batchRedemptionProcessResponse.asPrettyString());

		String actualQualifiedType = batchRedemptionProcessResponse.jsonPath().getString("success[0].qualified");
		String discountTypeProcessedFirst = batchRedemptionProcessResponse.jsonPath()
				.getString("success[0].discount_type");
		String discountTypeProcessedSecond = batchRedemptionProcessResponse.jsonPath()
				.getString("success[1].discount_type");
		String discountTypeProcessedThird = batchRedemptionProcessResponse.jsonPath()
				.getString("success[2].discount_type");
		String discountTypeProcessedFourth = batchRedemptionProcessResponse.jsonPath()
				.getString("success[3].discount_type");

		// Verify the response
		Assert.assertEquals(batchRedemptionProcessResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertEquals(actualQualifiedType, dataSet.get("expectedQualifiedType"));
		Assert.assertEquals(discountTypeProcessedSecond, "discount_amount");
		Assert.assertEquals(discountTypeProcessedThird, "fuel_reward");
		Assert.assertEquals(discountTypeProcessedFourth, "redemption_code");
		Assert.assertEquals(discountTypeProcessedFirst, "reward");

	}

	@Test(description = "Verify 'Exclude interoperability between acquisition types' validation when processing discount_basket containing fuel_reward", priority = 7, groups = {
			"OMMFeatureTesting" }, enabled = true)
	public void validateOMMT2178() throws InterruptedException {

		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Navigate to Multiple Redemption Tab and Uncheck the flags
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_discount_locking", "uncheck");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_auto_unlock", "uncheck");
		pageObj.cockpitRedemptionsPage().updateTotalNumberOfRedemptionsAllowed("10");

		// Clear Processing priority by discount types
		pageObj.cockpitRedemptionsPage().clearProcessingPriorityByDiscountType();

		// Clear redemption processing priority by Acquisition Type
		pageObj.cockpitRedemptionsPage().clearAcquisitionTypeData("Offers");
		pageObj.cockpitRedemptionsPage().clearAcquisitionTypeData("Earned Rewards");
		pageObj.cockpitRedemptionsPage().clearAcquisitionTypeData("Pre-Purchased Discount");
		pageObj.cockpitRedemptionsPage().clearAcquisitionTypeData("Coupons & Promos");
		pageObj.cockpitRedemptionsPage().clearInteroperability();
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		// Select offer dropdown value and create coupon campaign
		pageObj.campaignspage().createCouponCampaignIfNotExist(dataSet.get("campaign_name"), "POS", "PreGenerated",
				dataSet.get("noOfGuests"), "1", "$ OFF", "1", "", "", false, 0, "", "", "");

		pageObj.campaignspage().searchCampaign(dataSet.get("campaign_name"));
		codeNameList = pageObj.campaignspage().getPreGeneratedCuponList();
		String redemption_code = codeNameList.get(0).toString();

		// Check redeemable's availability in business and create redeemable if not
		// available
		pageObj.menupage().navigateToSubMenuItem("Settings", "Redeemables");
		pageObj.redeemablePage().createRedeemableIfNotExistWithExistingQC(dataSet.get("redeemable"), "Flat Discount",
				"", "1.0");
		String redeemable_id = pageObj.redeemablePage().getRedeemableID(dataSet.get("redeemable"));

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponseUser = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponseUser, "API 2 user signup");
		String token = signUpResponseUser.jsonPath().get("access_token.token").toString();
		System.out.println("token=" + token);

		String userID = signUpResponseUser.jsonPath().get("user.user_id").toString();
		System.out.println("userID=" + userID);

		// Gift Reedemable to User
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				redeemable_id);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		pageObj.utils().logPass("Api2  send reward reedemable to user is successful");

		// Get reward_id
		Response rewardResponse = pageObj.endpoints().authListAvailableRewardsNew(token, dataSet.get("client"),
				dataSet.get("secret"));
		String rewardID = rewardResponse.jsonPath().getString("id").replace("[", "").replace("]", "");
		pageObj.utils().logPass("Reward id " + rewardID + " is generated successfully ");

		// Navigate to User timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		// click message gift and gift fuel discount
		pageObj.guestTimelinePage().messageFuelDiscountToUser(dataSet.get("subject"), dataSet.get("giftTypes"),
				dataSet.get("fuelPoints"), dataSet.get("giftReason"));
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		boolean fuelGiftedNotification = pageObj.guestTimelinePage().verifyfuelGiftedNotification();
		Assert.assertTrue(status, "Message sent did not displayed on timeline");
		Assert.assertTrue(fuelGiftedNotification, "Fuel gifted notification did not displayed on timeline");

		// send reward amount to user Amount
		Response sendRewardAmountResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"),
				dataSet.get("amount"), "");
		pageObj.utils().logPass("Send reward amount to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardAmountResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		// Add discount type to discount basket
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> discount1 = new HashMap<String, String>();
		Map<String, String> discount2 = new HashMap<String, String>();
		Map<String, String> discount3 = new HashMap<String, String>();

		discount1 = pageObj.endpoints().getDiscountDetailsMapForDiscountAmountFuel("fuel_reward", "1");
		parentMap.put("discount1", discount1);

		discount2 = pageObj.endpoints().getDiscountDetailsMapExceptDiscountAmountFuel("reward", rewardID);
		parentMap.put("discount2", discount2);

		discount3 = pageObj.endpoints().getDiscountDetailsMapExceptDiscountAmountFuel("redemption_code",
				redemption_code);
		parentMap.put("discount3", discount3);

		// Add discount types in basket
		Response discountBasketResponse = pageObj.endpoints().mobileDiscountBasketforMultipleDiscountTypes(token,
				dataSet.get("client"), dataSet.get("secret"), parentMap);
		Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		System.out.println("discountBasketResponse=" + discountBasketResponse.asPrettyString());

		// Navigate to Multiple Redemption Tab and Uncheck the flags
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.cockpitRedemptionsPage().setInteroperability("Earned Rewards", "Coupons & Promos");
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// Process the basket
		Response batchRedemptionProcessResponse = pageObj.endpoints().processAuthBatchRedemptionUsingStoreNum(
				dataSet.get("client"), dataSet.get("secret"), dataSet.get("store_num"), token, userID,
				dataSet.get("item_id"));

		System.out.println("batchRedemptionProcessResponseUser=" + batchRedemptionProcessResponse.asPrettyString());

		String actualQualifiedType = batchRedemptionProcessResponse.jsonPath().getString("success[0].qualified");
		String discountTypeProcessedFirst = batchRedemptionProcessResponse.jsonPath()
				.getString("success[0].discount_type");
		String failure1 = batchRedemptionProcessResponse.jsonPath().getString("failures[0].discount_type");
		String failureMsg1 = batchRedemptionProcessResponse.jsonPath().getString("failures[0].message");
		String failure2 = batchRedemptionProcessResponse.jsonPath().getString("failures[1].discount_type");
		String failureMsg2 = batchRedemptionProcessResponse.jsonPath().getString("failures[1].message");

		// Verify the response
		Assert.assertEquals(batchRedemptionProcessResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertEquals(actualQualifiedType, dataSet.get("expectedQualifiedType"));
		Assert.assertEquals(discountTypeProcessedFirst, "reward");
		Assert.assertEquals(failure1, "fuel_reward");
		Assert.assertEquals(failure2, "redemption_code");
		Assert.assertEquals(failureMsg1, "[Interoperability validation failed.]");
		Assert.assertEquals(failureMsg2, "[Interoperability validation failed.]");

		// Reset Interoperability
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.cockpitRedemptionsPage().clearInteroperability();
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

	}

	@Test(description = "Verify discount_basket is processed when 'Exclude interoperability between acquisition types' validation is met", priority = 8, groups = {
			"OMMFeatureTesting" }, enabled = true)
	public void validateOMMT2179() throws InterruptedException {

		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Navigate to Multiple Redemption Tab and Uncheck the flags
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_discount_locking", "uncheck");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_auto_unlock", "uncheck");
		pageObj.cockpitRedemptionsPage().updateTotalNumberOfRedemptionsAllowed("10");

		// Clear Processing priority by discount types
		pageObj.cockpitRedemptionsPage().clearProcessingPriorityByDiscountType();

		// Clear redemption processing priority by Acquisition Type
		pageObj.cockpitRedemptionsPage().clearAcquisitionTypeData("Offers");
		pageObj.cockpitRedemptionsPage().clearAcquisitionTypeData("Earned Rewards");
		pageObj.cockpitRedemptionsPage().clearAcquisitionTypeData("Pre-Purchased Discount");
		pageObj.cockpitRedemptionsPage().clearAcquisitionTypeData("Coupons & Promos");
		pageObj.cockpitRedemptionsPage().clearInteroperability();
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// Check redeemable's availability in business and create redeemable if not
		// available
		pageObj.menupage().navigateToSubMenuItem("Settings", "Redeemables");
		pageObj.redeemablePage().createRedeemableIfNotExistWithExistingQC(dataSet.get("redeemable"), "Flat Discount",
				"", "1.0");
		String redeemable_id = pageObj.redeemablePage().getRedeemableID(dataSet.get("redeemable"));

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponseUser = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponseUser, "API 2 user signup");
		String token = signUpResponseUser.jsonPath().get("access_token.token").toString();
		System.out.println("token=" + token);

		String userID = signUpResponseUser.jsonPath().get("user.user_id").toString();
		System.out.println("userID=" + userID);

		// Gift Reedemable to User
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				redeemable_id);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		pageObj.utils().logPass("Api2  send reward reedemable to user is successful");

		// Get reward_id
		Response rewardResponse = pageObj.endpoints().authListAvailableRewardsNew(token, dataSet.get("client"),
				dataSet.get("secret"));
		String rewardID = rewardResponse.jsonPath().getString("id").replace("[", "").replace("]", "");
		pageObj.utils().logPass("Reward id " + rewardID + " is generated successfully ");

		// Navigate to User timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		// click message gift and gift fuel discount
		pageObj.guestTimelinePage().messageFuelDiscountToUser(dataSet.get("subject"), dataSet.get("giftTypes"),
				dataSet.get("fuelPoints"), dataSet.get("giftReason"));
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		boolean fuelGiftedNotification = pageObj.guestTimelinePage().verifyfuelGiftedNotification();
		Assert.assertTrue(status, "Message sent did not displayed on timeline");
		Assert.assertTrue(fuelGiftedNotification, "Fuel gifted notification did not displayed on timeline");

		// send reward amount to user Amount
		Response sendRewardAmountResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"),
				dataSet.get("amount"), "");
		pageObj.utils().logPass("Send reward amount to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardAmountResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		// Add discount type to discount basket
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> discount1 = new HashMap<String, String>();
		Map<String, String> discount2 = new HashMap<String, String>();

		discount1 = pageObj.endpoints().getDiscountDetailsMapForDiscountAmountFuel("fuel_reward", "1");
		parentMap.put("discount1", discount1);

		discount2 = pageObj.endpoints().getDiscountDetailsMapExceptDiscountAmountFuel("reward", rewardID);
		parentMap.put("discount2", discount2);

		// Add discount types in basket
		Response discountBasketResponse = pageObj.endpoints().mobileDiscountBasketforMultipleDiscountTypes(token,
				dataSet.get("client"), dataSet.get("secret"), parentMap);
		Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		System.out.println("discountBasketResponse=" + discountBasketResponse.asPrettyString());

		// Process the basket
		Response batchRedemptionProcessResponse = pageObj.endpoints().processAuthBatchRedemptionUsingStoreNum(
				dataSet.get("client"), dataSet.get("secret"), dataSet.get("store_num"), token, userID,
				dataSet.get("item_id"));

		System.out.println("batchRedemptionProcessResponseUser=" + batchRedemptionProcessResponse.asPrettyString());

		String actualQualifiedTypeFirst = batchRedemptionProcessResponse.jsonPath().getString("success[0].qualified");
		String discountTypeProcessedFirst = batchRedemptionProcessResponse.jsonPath()
				.getString("success[0].discount_type");
		String discountTypeProcessedSecond = batchRedemptionProcessResponse.jsonPath()
				.getString("success[1].discount_type");
		String actualQualifiedTypeSecond = batchRedemptionProcessResponse.jsonPath().getString("success[1].qualified");

		// Verify the response
		Assert.assertEquals(batchRedemptionProcessResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertEquals(actualQualifiedTypeFirst, dataSet.get("expectedQualifiedType"));
		Assert.assertEquals(discountTypeProcessedFirst, "fuel_reward");
		Assert.assertEquals(actualQualifiedTypeSecond, dataSet.get("expectedQualifiedType"));
		Assert.assertEquals(discountTypeProcessedSecond, "reward");

	}

	@Test(description = "Verify discount_basket processing sequence when 'Set processing order /Discount Type/Acquisition Type' are configured", priority = 9, groups = {
			"OMMFeatureTesting" }, enabled = true)
	public void validateOMMT2180() throws InterruptedException, ParseException {

		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> discount1 = new HashMap<String, String>();
		Map<String, String> discount2 = new HashMap<String, String>();
		Map<String, String> discount3 = new HashMap<String, String>();
		Map<String, String> discount4 = new HashMap<String, String>();
		Map<String, String> discount5 = new HashMap<String, String>();
		Map<String, String> discount6 = new HashMap<String, String>();
		Map<String, String> discount7 = new HashMap<String, String>();
		List<String> codeNameList1, codeNameList2;

		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Navigate to Multiple Redemption Tab and Uncheck the flags
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_discount_locking", "uncheck");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_auto_unlock", "uncheck");
		pageObj.cockpitRedemptionsPage().updateTotalNumberOfRedemptionsAllowed("10");
		pageObj.cockpitRedemptionsPage().setProcessingOrder("remove", "");
		pageObj.cockpitRedemptionsPage().setProcessingOrder("update", "Date of Expiry");

		// Clear Processing priority by discount types
		pageObj.cockpitRedemptionsPage().clearProcessingPriorityByDiscountType();

		// clear redemption processing priority by Acquisition Type
		pageObj.cockpitRedemptionsPage().clearAcquisitionTypeData("Offers");
		pageObj.cockpitRedemptionsPage().clearAcquisitionTypeData("Earned Rewards");
		pageObj.cockpitRedemptionsPage().clearAcquisitionTypeData("Pre-Purchased Discount");
		pageObj.cockpitRedemptionsPage().clearAcquisitionTypeData("Coupons & Promos");

		// clear Interoperability
		pageObj.cockpitRedemptionsPage().clearInteroperability();

		// Update Multiple Redemption page
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// Set redemption processing priority by Acquisition Type
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.cockpitRedemptionsPage().setProcessingPriorityByDiscountType("Coupon/Promos");
		pageObj.cockpitRedemptionsPage().setProcessingPriorityByDiscountType("Rewards");
		pageObj.cockpitRedemptionsPage().setProcessingPriorityByDiscountType("Fuel discount");
		pageObj.cockpitRedemptionsPage().setProcessingPriorityByDiscountType("Discount Amount");

		// Set redemption processing priority by Acquisition Type
		pageObj.cockpitRedemptionsPage().setAcquisitionType("Offers", "3");
		pageObj.cockpitRedemptionsPage().setAcquisitionType("Earned Rewards", "3");
		pageObj.cockpitRedemptionsPage().setAcquisitionType("Coupons & Promos", "3");

		// Update Multiple Redemption page
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// Set expiry days in conversion rule
		pageObj.menupage().navigateToSubMenuItem("Settings", "Conversion Rules");
		pageObj.settingsPage().clickConversionRule("Fuel");
		pageObj.settingsPage().chooseExpiryStrategy("exact_days");
		pageObj.settingsPage().setExpiryDays("30");
		pageObj.settingsPage().clickSaveBtn();

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		// Select offer dropdown value and create coupon campaign with end date
		pageObj.campaignspage().createCouponCampaignIfNotExist(dataSet.get("campaign_name1"), "POS", "PreGenerated",
				dataSet.get("noOfGuests"), "1", "$ OFF", "1", "", "", false, 365, "10", "30", "PM");

		pageObj.campaignspage().searchCampaign(dataSet.get("campaign_name1"));
		codeNameList1 = pageObj.campaignspage().getPreGeneratedCuponList();
		String redemption_code1 = codeNameList1.get(0).toString();

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		// Create another coupon campaign with other end date
		pageObj.campaignspage().createCouponCampaignIfNotExist(dataSet.get("campaign_name2"), "POS", "PreGenerated",
				dataSet.get("noOfGuests"), "1", "$ OFF", "1", "", "", false, 365, "9", "15", "AM");

		pageObj.campaignspage().searchCampaign(dataSet.get("campaign_name2"));
		codeNameList2 = pageObj.campaignspage().getPreGeneratedCuponList();
		String redemption_code2 = codeNameList2.get(0).toString();

		// Check redeemable's availability in business and create redeemable if not
		// available
		pageObj.menupage().navigateToSubMenuItem("Settings", "Redeemables");
		pageObj.redeemablePage().createRedeemableIfNotExistWithExistingQC(dataSet.get("redeemable"), "Flat Discount",
				"", "1.0");
		String redeemable_id = pageObj.redeemablePage().getRedeemableID(dataSet.get("redeemable"));

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponseUser = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponseUser, "API 2 user signup");
		String token = signUpResponseUser.jsonPath().get("access_token.token").toString();
		System.out.println("token=" + token);

		String userID = signUpResponseUser.jsonPath().get("user.user_id").toString();
		System.out.println("userID=" + userID);

		// Gift Reedemable to User with no end date
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUserWithEndDate(userID, dataSet.get("apiKey"),
				dataSet.get("amount"), redeemable_id, "", "", "");
		pageObj.utils().logPass("Send redeemable to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		pageObj.utils().logPass("Api2  send reward amount to user is successful");

		// Gift another Reedemable to User with end date-2040-11-22
		Response sendRewardResponse1 = pageObj.endpoints().sendMessageToUserWithEndDate(userID, dataSet.get("apiKey"),
				dataSet.get("amount"), redeemable_id, "", "", "2040-11-22");
		pageObj.utils().logPass("Send redeemable to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse1.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		pageObj.utils().logPass("Api2  send reward amount to user is successful");

		// Gift another Reedemable to user with end date-2040-11-20
		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUserWithEndDate(userID, dataSet.get("apiKey"),
				dataSet.get("amount"), redeemable_id, "", "", "2040-11-20");
		pageObj.utils().logPass("Send redeemable to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse2.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		pageObj.utils().logPass("Api2  send reward amount to user is successful");

		// Get reward_id
		Response rewardResponse = pageObj.endpoints().authListAvailableRewardsNew(token, dataSet.get("client"),
				dataSet.get("secret"));
		String rewardID1 = rewardResponse.jsonPath().getString("[0].id");
		String rewardID2 = rewardResponse.jsonPath().getString("[1].id");
		String rewardID3 = rewardResponse.jsonPath().getString("[2].id");
		pageObj.utils().logPass(
				"Reward id " + rewardID1 + " + " + rewardID2 + " + " + rewardID3 + " is generated successfully");

		// Navigate to User timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		// click message gift and gift fuel discount
		pageObj.guestTimelinePage().messageFuelDiscountToUser(dataSet.get("subject"), dataSet.get("giftTypes"),
				dataSet.get("fuelPoints"), dataSet.get("giftReason"));
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		boolean fuelGiftedNotification = pageObj.guestTimelinePage().verifyfuelGiftedNotification();
		Assert.assertTrue(status, "Message sent did not displayed on timeline");
		Assert.assertTrue(fuelGiftedNotification, "Fuel gifted notification did not displayed on timeline");

		// Send reward amount to user Amount
		Response sendRewardAmountResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"),
				dataSet.get("amount"), "");
		pageObj.utils().logPass("Send reward amount to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardAmountResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		// Add discount type to discount basket
		discount1 = pageObj.endpoints().getDiscountDetailsMapExceptDiscountAmountFuel("reward", rewardID1);
		parentMap.put("discount1", discount1);

		discount2 = pageObj.endpoints().getDiscountDetailsMapForDiscountAmountFuel("fuel_reward", "1");
		parentMap.put("discount2", discount2);

		discount3 = pageObj.endpoints().getDiscountDetailsMapExceptDiscountAmountFuel("reward", rewardID2);
		parentMap.put("discount3", discount3);

		discount4 = pageObj.endpoints().getDiscountDetailsMapExceptDiscountAmountFuel("redemption_code",
				redemption_code1);
		parentMap.put("discount4", discount4);

		discount5 = pageObj.endpoints().getDiscountDetailsMapForDiscountAmountFuel("discount_amount", "1");
		parentMap.put("discount5", discount5);

		discount6 = pageObj.endpoints().getDiscountDetailsMapExceptDiscountAmountFuel("redemption_code",
				redemption_code2);
		parentMap.put("discount6", discount6);

		discount7 = pageObj.endpoints().getDiscountDetailsMapExceptDiscountAmountFuel("reward", rewardID3);
		parentMap.put("discount7", discount7);

		// Add discount types in basket
		Response discountBasketResponse = pageObj.endpoints().mobileDiscountBasketforMultipleDiscountTypes(token,
				dataSet.get("client"), dataSet.get("secret"), parentMap);
		Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		System.out.println("discountBasketResponse=" + discountBasketResponse.asPrettyString());

		// Process the basket
		Response batchRedemptionProcessResponse = pageObj.endpoints().processAuthBatchRedemptionUsingStoreNum(
				dataSet.get("client"), dataSet.get("secret"), dataSet.get("store_num"), token, userID,
				dataSet.get("item_id"));

		System.out.println("batchRedemptionProcessResponseUser=" + batchRedemptionProcessResponse.asPrettyString());

		String actualQualifiedType = batchRedemptionProcessResponse.jsonPath().getString("success[0].qualified");
		String discountTypeProcessedFirst = batchRedemptionProcessResponse.jsonPath()
				.getString("success[0].discount_type");
		String discountTypeProcessedSecond = batchRedemptionProcessResponse.jsonPath()
				.getString("success[1].discount_type");
		String discountTypeProcessedThird = batchRedemptionProcessResponse.jsonPath()
				.getString("success[2].discount_type");
		String discountTypeProcessedThirdID = batchRedemptionProcessResponse.jsonPath()
				.getString("success[2].discount_id");
		String discountTypeProcessedFourth = batchRedemptionProcessResponse.jsonPath()
				.getString("success[3].discount_type");
		String discountTypeProcessedFourthID = batchRedemptionProcessResponse.jsonPath()
				.getString("success[3].discount_id");
		String discountTypeProcessedFifth = batchRedemptionProcessResponse.jsonPath()
				.getString("success[4].discount_type");
		String discountTypeProcessedFifthID = batchRedemptionProcessResponse.jsonPath()
				.getString("success[4].discount_id");
		String discountTypeProcessedSixth = batchRedemptionProcessResponse.jsonPath()
				.getString("success[5].discount_type");
		String discountTypeProcessedSeventh = batchRedemptionProcessResponse.jsonPath()
				.getString("success[6].discount_type");

		// Verify the response
		Assert.assertEquals(batchRedemptionProcessResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertEquals(actualQualifiedType, dataSet.get("expectedQualifiedType"));
		Assert.assertEquals(discountTypeProcessedFirst, "redemption_code");
		Assert.assertEquals(discountTypeProcessedSecond, "redemption_code");
		Assert.assertEquals(discountTypeProcessedThird, "reward");
		Assert.assertEquals(discountTypeProcessedThirdID, rewardID3);
		Assert.assertEquals(discountTypeProcessedFourth, "reward");
		Assert.assertEquals(discountTypeProcessedFourthID, rewardID2);
		Assert.assertEquals(discountTypeProcessedFifth, "reward");
		Assert.assertEquals(discountTypeProcessedFifthID, rewardID1);
		Assert.assertEquals(discountTypeProcessedSixth, "fuel_reward");
		Assert.assertEquals(discountTypeProcessedSeventh, "discount_amount");

		// Navigate to Multiple Redemption Tab and reset the configuration
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.cockpitRedemptionsPage().setProcessingOrder("remove", "");

		// Clear Processing priority by discount types
		pageObj.cockpitRedemptionsPage().clearProcessingPriorityByDiscountType();

		// clear redemption processing priority by Acquisition Type
		pageObj.cockpitRedemptionsPage().clearAcquisitionTypeData("Offers");
		pageObj.cockpitRedemptionsPage().clearAcquisitionTypeData("Earned Rewards");
		pageObj.cockpitRedemptionsPage().clearAcquisitionTypeData("Pre-Purchased Discount");
		pageObj.cockpitRedemptionsPage().clearAcquisitionTypeData("Coupons & Promos");

		// clear Interoperability
		pageObj.cockpitRedemptionsPage().clearInteroperability();

		// Update Multiple Redemption page
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// Reset expiry days
		pageObj.menupage().navigateToSubMenuItem("Settings", "Conversion Rules");
		pageObj.settingsPage().clickConversionRule("Fuel");
		pageObj.settingsPage().chooseExpiryStrategy("None");
		pageObj.settingsPage().clickSaveBtn();

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}

}