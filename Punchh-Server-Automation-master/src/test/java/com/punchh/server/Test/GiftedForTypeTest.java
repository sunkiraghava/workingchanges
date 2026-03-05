package com.punchh.server.Test;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
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
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class GiftedForTypeTest {
	private static Logger logger = LogManager.getLogger(GiftedForTypeTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String userEmail;
	private String sTCName;
	private String baseUrl;
	String blankString = "";
	private String env, run = "ui";
	private static Map<String, String> dataSet;
	String externalUID;

	@BeforeClass(alwaysRun = true)
	public void openBrowser() {
		driver = new BrowserUtilities().launchBrowser();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		// Single login to instance
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
		// Move to All Businesses page
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
	}

// Author -- Hardik Bhardwaj
	@Test(description = "SQ-T4379 Verify reward (Flat discount) having gifted_for_type User (received via deals) gets added to discount basket via Auth auto select API", priority = 0, groups = {
			"regression", "dailyrun" })
	@Owner(name = "Hardik Bhardwaj")
	public void T4379_dealGiftedForType() throws InterruptedException {

		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();

		// login to instance
		//pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		//pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Navigate to Multiple Redemption Tab and check the flags
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_auto_redemption", "check");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_auto_unlock", "check");
		pageObj.cockpitRedemptionsPage().setAutoRedemptionDiscounts("Offers", "Select");
		pageObj.cockpitRedemptionsPage().updateTotalNumberOfRedemptionsAllowed("20");
		pageObj.cockpitRedemptionsPage().setProcessingOrder("update", "Date of Expiry");
		pageObj.dashboardpage().updateCheckBox();

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), 200, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("API2 Signup is successful");
		logger.info("API2 Signup is successful");

		// List all deals - GET Call
		String redeemable_uuid = pageObj.redeemablesPage().getDealRedeemableUuid(dataSet.get("client"),
				dataSet.get("secret"), token, "OMM - SQ-T4379 Flat discount deal");

		// List all deals - POST Call
		Response postAPI1UsedDealResponse = pageObj.endpoints().Api1PostDeals(dataSet.get("client"),
				dataSet.get("secret"), token, redeemable_uuid);
		Assert.assertEquals(postAPI1UsedDealResponse.getStatusCode(), 200,
				"Status code 200 did not matched for post api1 deals for already used deal");
		logger.info("Verified already used deal api 1 response");
		TestListeners.extentTest.get().pass("Verified already used deal api 1 response");

		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Pizza1", "1", "10", "M", "10", "999", "1",
				dataSet.get("item_id"));
		parentMap.put("Pizza1", detailsMap1);

		// Auth Auto select API
		Response redemptionResponse = pageObj.endpoints().authAutoSelectAPI(dataSet.get("client"),
				dataSet.get("secret"), token, "14", blankString, parentMap);
		Assert.assertEquals(redemptionResponse.getStatusCode(), 200,
				"Status code 200 did not match with add discount to basket ");
		TestListeners.extentTest.get().pass("AUTH add discount to basket is successful");
		String dealNameInResp = redemptionResponse.jsonPath()
				.getString("discount_basket_items[0].discount_details.name");
		Assert.assertEquals(dealNameInResp, dataSet.get("dealName"), "Deal name is not matching");
		logger.info("Deal name is matching" + dealNameInResp);
		TestListeners.extentTest.get().pass("Deal name is matching" + dealNameInResp);

	}

	@Test(description = "SQ-T4380 Verify reward (Flat discount) having gifted_for_type User (received via reward transfer) gets added to discount basket via POS auto select API", priority = 1, groups = {
			"regression", "unstable", "dailyrun" })
	@Owner(name = "Hardik Bhardwaj")
	public void T4380_RewardGiftedForType() throws InterruptedException {

		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), 200, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("API2 Signup is successful");
		logger.info("API2 Signup is successful");

		// User SignUp using API
		String userEmail1 = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse1 = pageObj.endpoints().Api2SignUp(userEmail1, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse1, "API 2 user signup");
		String token1 = signUpResponse1.jsonPath().get("access_token.token").toString();
		Assert.assertEquals(signUpResponse1.getStatusCode(), 200, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("API2 Signup is successful");
		logger.info("API2 Signup is successful");

		// send reward amount to user Reedemable
		pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "16", dataSet.get("redeemableID"), "", "");
		logger.info("Send redeemable to the user successfully");
		TestListeners.extentTest.get().pass("Send redeemable to the user successfully");

		// get reward id
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemableID"));

		// API reward gifted reward to other user
		Response Api1GiftRewardToOtherUserResponse = pageObj.endpoints()
				.Api1GiftRewardToOtherUser(dataSet.get("client"), dataSet.get("secret"), token, userEmail1, rewardId);
		Assert.assertEquals(Api1GiftRewardToOtherUserResponse.getStatusCode(), 200,
				"Status code 200 did not matched for post api1 deals for already used deal");

		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Pizza1", "1", "10", "M", "10", "999", "1",
				dataSet.get("item_id"));
		parentMap.put("Pizza1", detailsMap1);

		// Auth Auto select API
		Response redemptionResponse = pageObj.endpoints().authAutoSelectAPI(dataSet.get("client"),
				dataSet.get("secret"), token1, "14", blankString, parentMap);
		Assert.assertEquals(redemptionResponse.getStatusCode(), 200,
				"Status code 200 did not match with add discount to basket ");
		TestListeners.extentTest.get().pass("AUTH add discount to basket is successful");
		String redeemableNameInResp = redemptionResponse.jsonPath()
				.getString("discount_basket_items[0].discount_details.name");
		Assert.assertEquals(redeemableNameInResp, dataSet.get("redeemableName"), "Redeemable name is not matching");
		logger.info("Redeemable name is matching" + redeemableNameInResp);
		TestListeners.extentTest.get().pass("Redeemable name is matching" + redeemableNameInResp);

	}

	@Test(description = "SQ-T4378 Verify reward (Flat discount) having gifted_for_type GamingLevel gets added to discount basket via Auth auto select API", priority = 2, groups = {
			"regression", "dailyrun" })
	@Owner(name = "Hardik Bhardwaj")
	public void T4378_GamingLevelGiftedForType() throws InterruptedException {

		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();

		// login to instance
		//pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		//pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Navigate to Multiple Redemption Tab and check the flags
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_auto_redemption", "check");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_auto_unlock", "check");
		pageObj.cockpitRedemptionsPage().setAutoRedemptionDiscounts("Offers", "Select");
		pageObj.cockpitRedemptionsPage().updateTotalNumberOfRedemptionsAllowed("20");
		pageObj.dashboardpage().updateCheckBox();

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Campaigns");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_has_games", "check");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_has_scratch_game", "check");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_has_slot_machine_game", "check");
		pageObj.dashboardpage().updateButton();

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		Assert.assertEquals(signUpResponse.getStatusCode(), 200, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("API2 Signup is successful");
		logger.info("API2 Signup is successful");

		// API reward gifted reward to other user
		Response APi1GamingAchievementsResponse = pageObj.endpoints().APi1GamingAchievements(dataSet.get("client"),
				dataSet.get("secret"), token, dataSet.get("kind"), dataSet.get("level"), dataSet.get("score"),
				dataSet.get("gaming_level_id"));
		Assert.assertEquals(APi1GamingAchievementsResponse.getStatusCode(), 200,
				"Status code 200 did not matched for post api1 deals for already used deal");

		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Pizza1", "1", "10", "M", "10", "999", "1",
				dataSet.get("item_id"));
		parentMap.put("Pizza1", detailsMap1);

		// Auth Auto select API
		Response redemptionResponse = pageObj.endpoints().authAutoSelectAPI(dataSet.get("client"),
				dataSet.get("secret"), token, "14", externalUID, parentMap);
		Assert.assertEquals(redemptionResponse.getStatusCode(), 200,
				"Status code 200 did not match with add discount to basket ");
		TestListeners.extentTest.get().pass("AUTH add discount to basket is successful");
		String redeemableNameInResp = redemptionResponse.jsonPath()
				.getString("discount_basket_items[0].discount_details.name");
		Assert.assertEquals(redeemableNameInResp, dataSet.get("redeemableName"), "Redeemable name is not matching");
		logger.info("Redeemable name is matching" + redeemableNameInResp);
		TestListeners.extentTest.get().pass("Redeemable name is matching" + redeemableNameInResp);

	}

	@Test(description = "SQ-T4381 Verify processing priority when discount basket has subscription plan having end_time greater than the reward end_time and Set redemption processing priority by Discount Type -> Blank", groups = {
			"regression", "unstable", "dailyrun" }, priority = 0)
	@Owner(name = "Hardik Bhardwaj")
	public void T4381_processingPriorityDiscountBasket() throws InterruptedException {

		String PlanID = dataSet.get("PlanID");
		String spPrice = dataSet.get("spPrice");
		String subAmount = Integer.toString(Utilities.getRandomNoFromRange(100, 150));

		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();
		Map<String, String> detailsMap2 = new HashMap<String, String>();

		// login to instance
		//pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		//pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Navigate to Multiple Redemption Tab and check the flags
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_auto_redemption", "check");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_auto_unlock", "check");
		pageObj.cockpitRedemptionsPage().setAutoRedemptionDiscounts("Offers", "Select");
		pageObj.cockpitRedemptionsPage().updateTotalNumberOfRedemptionsAllowed("20");
		pageObj.cockpitRedemptionsPage().setProcessingOrder("remove", "");
		pageObj.cockpitRedemptionsPage().clearAcquisitionTypeData("Offers");
		pageObj.cockpitRedemptionsPage().clearAcquisitionTypeData("Earned Rewards");
		pageObj.cockpitRedemptionsPage().clearAcquisitionTypeData("Pre-Purchased Discount");
		pageObj.cockpitRedemptionsPage().clearAcquisitionTypeData("Coupons & Promos");
		pageObj.dashboardpage().updateCheckBox();

		// User SignUp using API
		// Signup using mobile api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), 200);
		String token = signUpResponse.jsonPath().get("auth_token.token").toString();
		String authToken = signUpResponse.jsonPath().get("authentication_token").toString();
		String userID = signUpResponse.jsonPath().get("id").toString();

		// send reward amount to user Reedemable
		pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "16", dataSet.get("redeemableID"), "",
				"100");
		logger.info("Send redeemable to the user successfully");
		TestListeners.extentTest.get().pass("Send redeemable to the user successfully");

		// get reward id
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemableID"));

		// subscription purchase api 2
		Response purchaseSubscriptionresponse = pageObj.endpoints().Api2SubscriptionPurchaseFutureDate(token, PlanID,
				dataSet.get("client"), dataSet.get("secret"), spPrice);
		Assert.assertEquals(purchaseSubscriptionresponse.getStatusCode(), 201);
		String subscription_id = purchaseSubscriptionresponse.jsonPath().get("subscription_id").toString();
		logger.info(userEmail + " purchased " + subscription_id + " Plan id = " + PlanID);

		// add reward completion to basket
		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAddedForPOSAPIWithExt_Uid(
				dataSet.get("locationKey"), userID, "reward", rewardId, externalUID);
		System.out.println("discountBasketResponse==" + discountBasketResponse.asPrettyString());
		Assert.assertEquals(discountBasketResponse.getStatusCode(), 200);

		// add subscription completion to basket
		Response discountBasketResponse1 = pageObj.endpoints().authListDiscountBasketAddedForPOSAPIWithExt_Uid(
				dataSet.get("locationKey"), userID, "subscription", subscription_id, externalUID);
		System.out.println("discountBasketResponse==" + discountBasketResponse1.asPrettyString());
		Assert.assertEquals(discountBasketResponse1.getStatusCode(), 200);

		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Pizza1", "1", "10", "M", "10", "999", "1",
				dataSet.get("item_id"));
		parentMap.put("Pizza1", detailsMap1);

		detailsMap2 = pageObj.endpoints().getRecieptDetailsMap("Pizza1", "1", "10", "M", "10", "999", "1",
				dataSet.get("item_id"));
		parentMap.put("Pizza2", detailsMap2);

		Response batchRedemptionProcessResponseUser = pageObj.endpoints()
				.processBatchRedemptionOfBasketPOSAPI(dataSet.get("locationKey"), userID, subAmount, parentMap);
		Assert.assertEquals(batchRedemptionProcessResponseUser.getStatusCode(), 200,
				"batch_redemptions status code is not 200 ");
		String redeemableName = batchRedemptionProcessResponseUser.jsonPath().get("success[0].discount_details.name")
				.toString();
		Assert.assertEquals(redeemableName, dataSet.get("redeemableName"), "Redeemable name is not matching");
		logger.info("Redeemable name is matching " + redeemableName);
		TestListeners.extentTest.get().pass("Redeemable name is matching " + redeemableName);
		String subscriptionNamde = batchRedemptionProcessResponseUser.jsonPath().get("success[1].discount_details.name")
				.toString();
		Assert.assertEquals(subscriptionNamde, dataSet.get("subscriptionName"), "Subscription name is not matching");
		logger.info("Subscription name is matching " + redeemableName);
		TestListeners.extentTest.get().pass("Subscription name is matching " + redeemableName);

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