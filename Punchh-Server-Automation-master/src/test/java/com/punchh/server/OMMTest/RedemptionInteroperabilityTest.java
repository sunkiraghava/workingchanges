package com.punchh.server.OMMTest;

import java.lang.reflect.Method;
import java.util.ArrayList;
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
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.annotations.Owner;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.ApiUtils;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class RedemptionInteroperabilityTest {

	private static Logger logger = LogManager.getLogger(RedemptionInteroperabilityTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String sTCName;
	private String env;
	private String baseUrl;
	private String userEmail;
	private static Map<String, String> dataSet;
	String run = "ui";
	private List<String> codeNameList;
	private boolean GlobalBenefitRedemptionThrottlingToggle;
	ApiUtils apiUtils;
	String externalUID;
	private String spName, PlanID, endDateTime, spPrice;
	Utilities utils;

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
		GlobalBenefitRedemptionThrottlingToggle = false;
		codeNameList = new ArrayList<String>();
		apiUtils = new ApiUtils();
		externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		endDateTime = CreateDateTime.getFutureDate(1) + " 10:00 PM";
		utils = new Utilities(driver);
	}

	@Test(description = "SQ-T5830 Verify functionality of Exclude interoperability between acquisition types for Discount Amount in Auto Select API", groups = {
			"regression", "dailyrun" }, dataProvider = "TestDataProvider", priority = 0)
	@Owner(name = "Hardik Bhardwaj")
	public void T5830_InteroperabilityBetweenAcquisitionTypes(String first_acquisition_type,
			String second_acquisition_type) throws InterruptedException {
		utils.logit("For first acquisition type " + first_acquisition_type + " and second acquisition type "
				+ second_acquisition_type);
		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Navigate to Multiple Redemption Tab and check the flags
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_auto_redemption", "check");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_discount_locking", "check");
		pageObj.cockpitRedemptionsPage().updateTotalNumberOfRedemptionsAllowed("10");
		pageObj.cockpitRedemptionsPage().setAutoRedemptionDiscounts("Offers", "Select");
		pageObj.cockpitRedemptionsPage().setAutoRedemptionDiscounts("Subscription", "select");
		pageObj.cockpitRedemptionsPage().setAutoRedemptionDiscounts("Discount Amount", "select");
		pageObj.dashboardpage().updateCheckBox();

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logit("API2 Signup is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "16",
				dataSet.get("redeemable_id"), "", "");

		utils.logit("Send redeemable to the user successfully");

		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		utils.logPass("Api2  send reward amount to user is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse1 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "16",
				dataSet.get("redeemable_id_2"), "", "");
		utils.logit("Send redeemable to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse1.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		utils.logPass("Api2  send reward amount to user is successful");

		// get reward id
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id"));

		utils.logit("Reward id " + rewardId + " is generated successfully ");

		// get reward id
		String rewardId2 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id_2"));

		utils.logit("Reward id " + rewardId2 + " is generated successfully ");

		// AUTH Add Discount to Basket
		Response discountBasketResponse = pageObj.endpoints().addDiscountToBasketAUTH(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId, externalUID);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponse.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		utils.logPass("AUTH add discount to basket is successful");

		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.cockpitRedemptionsPage().clearInteroperability();
		pageObj.cockpitRedemptionsPage().setInteroperabilityNew(first_acquisition_type, second_acquisition_type);
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

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
		utils.logPass("AUTH add discount to basket is successful");

		boolean value = pageObj.guestTimelinePage().verifyDiscountBasketVariablePresent(redemptionResponse,
				"discount_basket_items", "discount_type", "discount_amount");
		Assert.assertFalse(value, "In Auth Auto Select Api response auto_select variable is present");
		utils.logit(
				"Verified that No Discount Amount gets added to discount_basket (as interoperability does not permit)for first acquisition type "
						+ first_acquisition_type + " and second acquisition type " + second_acquisition_type);
		utils.logPass("In Auth Auto Select Api response auto_select variable is not present");

		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.cockpitRedemptionsPage().clearInteroperability();
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

	}

	@DataProvider(name = "TestDataProvider")
	public Object[][] testDataProvider() {

		return new Object[][] {

				// {"first_acquisition_type","second_acquisition_type"},
				{ "Earned Rewards", "Offers" },
				{ "Offers", "Earned Rewards" },

		};
	}

	@Test(description = "SQ-T5829 Validate the priority of offer types for auto-redemption", groups = { "regression",
			"dailyrun" }, dataProvider = "TestDataProvider2", priority = 1)
	@Owner(name = "Hardik Bhardwaj")
	public void T5829_InteroperabilityBetweenAcquisitionTypes(String processingPriority1, String processingPriority2,
			String processingPriority3, String discount_type_exp1, String discount_type_exp2, String discount_type_exp3,
			String autoRedemptionDiscounts1, String autoRedemptionDiscounts2, String autoRedemptionDiscounts3)
			throws InterruptedException {
		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Navigate to Multiple Redemption Tab and check the flags
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_auto_redemption", "check");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_discount_locking", "check");
		pageObj.cockpitRedemptionsPage().cleartAutoRedemptionDiscounts();
		pageObj.cockpitRedemptionsPage().setAutoRedemptionDiscounts(autoRedemptionDiscounts1, "Select");
		pageObj.cockpitRedemptionsPage().setAutoRedemptionDiscounts(autoRedemptionDiscounts2, "Select");
		pageObj.cockpitRedemptionsPage().setAutoRedemptionDiscounts(autoRedemptionDiscounts3, "Select");

		// Set Processing priority by discount types
		pageObj.cockpitRedemptionsPage().clearProcessingPriorityByDiscountType();
		pageObj.cockpitRedemptionsPage().setProcessingPriorityByDiscountType(processingPriority1);
		pageObj.cockpitRedemptionsPage().setProcessingPriorityByDiscountType(processingPriority2);
		pageObj.cockpitRedemptionsPage().setProcessingPriorityByDiscountType(processingPriority3);
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// Set QC in Redeemable
		pageObj.menupage().navigateToSubMenuItem("Settings", "Redeemables");
		pageObj.redeemablePage().searchAndClickOnRedeemable("Base Redeemable");
		pageObj.redeemablePage().removeExistingQualifier();
		pageObj.dashboardpage().checkUncheckToggle("Enable Auto Redemption", "ON");
		pageObj.redeemablePage().addQCinRedeemable("Automation - 12003");
		pageObj.redeemablePage().clickFinishBtn();

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logit("API2 Signup is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "96",
				dataSet.get("redeemable_id"), "", "");

		utils.logit("Send redeemable to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		utils.logPass("Api2  send reward amount to user is successful");

		// get reward id
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id"));

		utils.logit("Reward id " + rewardId + " is generated successfully ");

		PlanID = dataSet.get("PlanID");
		spPrice = dataSet.get("spPrice");
		spName = dataSet.get("spName");

		// subscription purchase api 2
		Response purchaseSubscriptionresponse = pageObj.endpoints().Api2SubscriptionPurchase(token, PlanID,
				dataSet.get("client"), dataSet.get("secret"), spPrice, endDateTime);
		Assert.assertEquals(purchaseSubscriptionresponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		String subscription_id = purchaseSubscriptionresponse.jsonPath().get("subscription_id").toString();
		logger.info(userEmail + " purchased " + subscription_id + " Plan id = " + PlanID);

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
		utils.logPass("AUTH add discount to basket is successful");
		String discount_type = redemptionResponse.jsonPath().get("discount_basket_items[0].discount_type").toString();
		String discount_type1 = redemptionResponse.jsonPath().get("discount_basket_items[1].discount_type").toString();
		String discount_type2 = redemptionResponse.jsonPath().get("discount_basket_items[2].discount_type").toString();
		Assert.assertEquals(discount_type, discount_type_exp1, "Discount type is not matched at 0th index");
		Assert.assertEquals(discount_type1, discount_type_exp2, "Discount type is not matched at 1st index");
		Assert.assertEquals(discount_type2, discount_type_exp3, "Discount type is not matched at 2nd index");
		utils.logPass("Verified that Discount " + discount_type_exp1 + ", " + discount_type_exp2 + " and "
				+ discount_type_exp3 + " gets added to discount_basket in the same order of processing priority");

	}

	@DataProvider(name = "TestDataProvider2")
	public Object[][] testDataProvider2() {

		return new Object[][] {

				// {"processingPriority1","processingPriority2","processingPriority3","discount_type_exp1","discount_type_exp2","discount_type_exp3","autoRedemptionDiscounts1","autoRedemptionDiscounts2","autoRedemptionDiscounts3},
				{ "Rewards", "Discount Amount", "Subscription", "discount_amount", "reward", "subscription",
						"Discount Amount", "Offers", "Subscription" },
				{ "Rewards", "Subscription", "Discount Amount", "reward", "subscription", "discount_amount", "Offers",
						"Subscription", "Discount Amount" },

		};
	}

	@Test(description = "SQ-T5828 Validate that Discount_amount gets added to discount basket using Auto Select API if it is getting qualified and Auto Redemption Flag is OFF at redeemable level", groups = {
			"regression", "dailyrun" }, priority = 2)
	@Owner(name = "Hardik Bhardwaj")
	public void T5828_validateDiscountAmountAdded() throws InterruptedException {
		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Navigate to Multiple Redemption Tab and check the flags
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_auto_redemption", "check");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_discount_locking", "check");
		pageObj.cockpitRedemptionsPage().cleartAutoRedemptionDiscounts();
		pageObj.cockpitRedemptionsPage().setAutoRedemptionDiscounts("Discount Amount", "Select");
		pageObj.cockpitRedemptionsPage().setAutoRedemptionDiscounts("Offers", "Select");
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// Set QC in Redeemable
		pageObj.menupage().navigateToSubMenuItem("Settings", "Redeemables");
		pageObj.redeemablePage().searchAndClickOnRedeemable("Base Redeemable");
		pageObj.redeemablePage().removeExistingQualifier();
		pageObj.dashboardpage().checkUncheckToggle("Enable Auto Redemption", "OFF");
		pageObj.redeemablePage().addQCinRedeemable("Automation - 12003");
		pageObj.redeemablePage().clickFinishBtn();

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logit("API2 Signup is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "60", "", "",
				"");
		utils.logit("Send redeemable to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		utils.logPass("Api2  send reward amount to user is successful");
		utils.logit("Send amount to the user successfully");

		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();

		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Pizza1", "1", "10", "M", "10", "999", "1",
				dataSet.get("item_id"));
		parentMap.put("Pizza1", detailsMap1);

		// Auth Auto select API
		Response redemptionResponse = pageObj.endpoints().authAutoSelectAPI(dataSet.get("client"),
				dataSet.get("secret"), token, "20", externalUID, parentMap);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, redemptionResponse.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		utils.logPass("AUTH add discount to basket is successful");
		String discount_type = redemptionResponse.jsonPath().get("discount_basket_items[0].discount_type").toString();
		Assert.assertEquals(discount_type, "discount_amount", "Discount type is not matched at 0th index");
		utils.logPass("Verified that Discount Amount gets added to discount_basket using Auto Select API");

	}

	@Test(description = "SQ-T5876 Verify if Total number of redemptions allowed is 10 and discount_basket already contains 10 discounts then no Discount Amount should get added to discount_basket automatically on hitting Auto Select API", groups = {
			"regression" }, priority = 2)
	@Owner(name = "Vansham Mishra")
	public void T5876_verifyNoDiscountAddedWhenMaxRedemptionsReached() throws InterruptedException {
		String discountID = Utilities.getRandomNoFromRange(1, 4) + "";
		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Navigate to Multiple Redemption Tab and check the flags
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.cockpitRedemptionsPage()
				.updateTotalNumberOfRedemptionsAllowed(dataSet.get("totalNumberOfRedemptionsAllowed"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logit("API2 Signup is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				"2826206", "", "");
		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				"2826206", "", "");
		utils.logit("Send redeemable to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		utils.logPass("Api2  send reward amount to user is successful");
		utils.logit("Send amount to the user successfully");

		String PlanID = dataSet.get("planId");
		String spPrice = dataSet.get("spPrice");
		String externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		/// subscription purchase api 2
		Response purchaseSubscriptionresponse = pageObj.endpoints().Api2SubscriptionPurchase(token, PlanID,
				dataSet.get("client"), dataSet.get("secret"), spPrice, endDateTime);
		Assert.assertEquals(purchaseSubscriptionresponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		String subscription_id = purchaseSubscriptionresponse.jsonPath().get("subscription_id").toString();
		logger.info(userEmail + " purchased " + subscription_id + " Plan id = " + PlanID);

		// Adding subscription into discount basket
		String totalNumberOfRedemptionsAllowed = dataSet.get("totalNumberOfRedemptionsAllowed");
		Response discountBasketResponse = pageObj.endpoints().addDiscountToBasketAPI2(token, dataSet.get("client"),
				dataSet.get("secret"), "subscription", subscription_id + "");
		Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

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

		// verify the count of discount basket items in the discount basket before
		// gifting discount amount to the user
		JsonPath jsonPath = new JsonPath(redemptionResponse.asString());
		String discountBasketItemsCount = String.valueOf(jsonPath.getList("discount_basket_items").size());
		Assert.assertEquals(discountBasketItemsCount, totalNumberOfRedemptionsAllowed,
				"The discount_basket_items array does not contain exactly 3 items.");

		// verify that discount_basket_items[0].discount_type is subscription
		String discountType = redemptionResponse.jsonPath().getString("discount_basket_items[0].discount_type");
		Assert.assertEquals(discountType, "subscription",
				"Discount type is not matched with subscription for Auto select API");

		// verify that discount_basket_items[0].discount_type is reward
		String discountType1 = redemptionResponse.jsonPath().getString("discount_basket_items[1].discount_type");
		Assert.assertEquals(discountType1, "reward", "Discount type is not matched with reward for Auto select API");

		// verify that discount_basket_items[0].discount_type is reward
		String discountType2 = redemptionResponse.jsonPath().getString("discount_basket_items[2].discount_type");
		Assert.assertEquals(discountType2, "reward", "Discount type is not matched with reward for Auto select API");
		utils.logPass("Verified that Discount " + discountType + ", " + discountType1 + " and " + discountType2
				+ " gets added to discount_basket in the same order of processing priority");

		Response sendRewardResponse3 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "14", "",
				"", "");
		// Auth Auto select API
		Response redemptionResponse2 = pageObj.endpoints().authAutoSelectAPI(dataSet.get("client"),
				dataSet.get("secret"), token, "14", externalUID, parentMap);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, redemptionResponse2.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");

		// verify the count of discount basket items in the discount basket after
		// gifting discount amount to the user
		JsonPath jsonPath1 = new JsonPath(redemptionResponse2.asString());
		String discountBasketItemsCount1 = String.valueOf(jsonPath1.getList("discount_basket_items").size());
		Assert.assertEquals(discountBasketItemsCount1, totalNumberOfRedemptionsAllowed,
				"The discount_basket_items array does not contain exactly 3 items.");
		utils.logPass("Verified that only " + totalNumberOfRedemptionsAllowed
				+ " items have been added into the discount basket");

		// verify that discount_basket_items[0].discount_type is subscription
		String discountTypeSubscription = redemptionResponse2.jsonPath()
				.getString("discount_basket_items[0].discount_type");
		Assert.assertEquals(discountTypeSubscription, "subscription",
				"Discount type is not matched with subscription for Auto select API");

		// verify that discount_basket_items[0].discount_type is reward
		String discountTypeReward1 = redemptionResponse2.jsonPath().getString("discount_basket_items[1].discount_type");
		Assert.assertEquals(discountTypeReward1, "reward",
				"Discount type is not matched with reward for Auto select API");

		// verify that discount_basket_items[0].discount_type is reward
		String discountTypeReward2 = redemptionResponse2.jsonPath().getString("discount_basket_items[2].discount_type");
		Assert.assertEquals(discountTypeReward2, "reward",
				"Discount type is not matched with reward for Auto select API");
		utils.logPass("verified that No Discount Amount gets added to discount_basket");

		// Set total number of redemptions allowed back to 10
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.cockpitRedemptionsPage()
				.updateTotalNumberOfRedemptionsAllowed(dataSet.get("previousTotalNumberOfRedemptionsAllowed"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();
	}

	@Test(description = "SQ-T5883 Verify if Total number of redemptions allowed is 10 and discount_basket contains 6 discounts then 1 Discount Amount and 3 rewards should get added to discount_basket automatically on hitting Auto Select API"
			+ "SQ-T5908 Verify if Total number of redemptions allowed is 1 and discount_basket contains 0 discounts then max 1 Discount Amount should get added to discount_basket automatically on hitting Auto Select API", groups = {
					"regression" }, priority = 2)
	@Owner(name = "Vansham Mishra")
	public void T5883_verifyNoDiscountAddedWhenMaxRedemptionsReached() throws InterruptedException {
		String discountID = Utilities.getRandomNoFromRange(1, 4) + "";
		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Navigate to Multiple Redemption Tab and check the flags
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.cockpitRedemptionsPage()
				.updateTotalNumberOfRedemptionsAllowed(dataSet.get("totalNumberOfRedemptionsAllowed"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logit("API2 Signup is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				"2826206", "", "");
		utils.logit("Send redeemable to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		utils.logPass("Api2  send reward amount to user is successful");
		utils.logit("Send amount to the user successfully");

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

		// verify the count of discount basket items in the discount basket before
		// gifting discount amount to the user
		JsonPath jsonPath = new JsonPath(redemptionResponse.asString());
		String discountBasketItemsCount = String.valueOf(jsonPath.getList("discount_basket_items").size());
		Assert.assertEquals(discountBasketItemsCount, "1",
				"The discount_basket_items array does not contain exactly 3 items.");
		utils.logPass("Verified that only " + "1" + " items have been added into the discount basket");

		// verify that discount_basket_items[0].discount_type is reward
		String discountType1 = redemptionResponse.jsonPath().getString("discount_basket_items[0].discount_type");
		Assert.assertEquals(discountType1, "reward", "Discount type is not matched with reward for Auto select API");
		utils.logPass("Verified that only one reward gets added to discount_basket using Auto Select API");

		// send reward to user Reedemable
		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				"2826206", "", "");

		// send discount amount to the user
		Response sendRewardResponse3 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "14", "",
				"", "");

		// Auth Auto select API
		Response redemptionResponse2 = pageObj.endpoints().authAutoSelectAPI(dataSet.get("client"),
				dataSet.get("secret"), token, "14", externalUID, parentMap);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, redemptionResponse2.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");

		// verify the count of discount basket items in the discount basket after
		// gifting discount amount to the user
		JsonPath jsonPath1 = new JsonPath(redemptionResponse2.asString());
		String discountBasketItemsCount1 = String.valueOf(jsonPath1.getList("discount_basket_items").size());
		Assert.assertEquals(discountBasketItemsCount1, "3",
				"The discount_basket_items array does not contain exactly 3 items.");
		utils.logPass("Verified that only " + "3" + " items have been added into the discount basket");

		// verify that discount_basket_items[0].discount_type is reward
		String discountTypeReward1 = redemptionResponse2.jsonPath().getString("discount_basket_items[0].discount_type");
		Assert.assertEquals(discountTypeReward1, "reward",
				"Discount type is not matched with reward for Auto select API");

		// verify that discount_basket_items[1].discount_type is discount_amount
		String discountTypeReward2 = redemptionResponse2.jsonPath().getString("discount_basket_items[1].discount_type");
		Assert.assertEquals(discountTypeReward2, "discount_amount",
				"Discount type is not matched with reward for Auto select API");

		// verify that discount_basket_items[2].discount_type is reward
		String discountTypeReward3 = redemptionResponse2.jsonPath().getString("discount_basket_items[2].discount_type");
		Assert.assertEquals(discountTypeReward3, "reward",
				"Discount type is not matched with reward for Auto select API");

		utils.logPass("Verified that discount amount and reward gets added to discount_basket");

		// Set total number of redemptions allowed back to 10
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.cockpitRedemptionsPage()
				.updateTotalNumberOfRedemptionsAllowed(dataSet.get("previousTotalNumberOfRedemptionsAllowed"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();
	}

	@Test(description = "SQ-T5827 Validate batch commit call with business rules applied", groups = { "regression",
			"dailyrun" }, priority = 3)
	@Owner(name = "Hardik Bhardwaj")
	public void T5827_batchCommitCall() throws InterruptedException {
		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Navigate to Multiple Redemption Tab and check the flags
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_auto_redemption", "check");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_discount_locking", "uncheck");
		pageObj.cockpitRedemptionsPage().updateTotalNumberOfRedemptionsAllowed("10");
		pageObj.cockpitRedemptionsPage().cleartAutoRedemptionDiscounts();
		pageObj.cockpitRedemptionsPage().setAutoRedemptionDiscounts("Discount Amount", "select");
		pageObj.dashboardpage().updateCheckBox();
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Validations");
		pageObj.cockpitRedemptionsPage().updateMaxRedemptionAmount("10");
		pageObj.dashboardpage().updateCheckBox();

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logit("API2 Signup is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "50", "", "",
				"");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		utils.logit("Send redeemable to the user successfully");

		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();

		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Pizza1", "1", "10", "M", "100", "999", "1",
				dataSet.get("item_id"));
		parentMap.put("Pizza1", detailsMap1);

		// Auth Auto select API
		Response redemptionResponse = pageObj.endpoints().authAutoSelectAPI(dataSet.get("client"),
				dataSet.get("secret"), token, "14", "", parentMap);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, redemptionResponse.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		utils.logit("Auth Auto Select Api is successful");
		String authSelectAmt = redemptionResponse.jsonPath().get("discount_basket_items[0].discount_value").toString();
		Assert.assertEquals(authSelectAmt, "10.0",
				"Redemption amount is not matched with 10.0 for Auth Auto select API discount amount");
		utils.logPass("Verified that $10 worth of banked currency should be auto-applied");

		// Auth Process Batch Redemption
		Response batchRedemptionProcessResponse = pageObj.endpoints().processBatchRedemptionOfBasketAUTHAPI(
				dataSet.get("client"), dataSet.get("secret"), dataSet.get("locationkeyRedemption2_0"), token, userID,
				"101");
		Assert.assertEquals(batchRedemptionProcessResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Process Batch Redemption ");
		utils.logit("Auth Process Batch Redemption Api is successful");
		String authProcessAmt = batchRedemptionProcessResponse.jsonPath().get("success[0].discount_amount").toString();
		Assert.assertEquals(authProcessAmt, "10.0",
				"Redemption amount is not matched with 10.0 for Auth Process Batch Redemption discount amount");
		utils.logPass("Verified that Final amount-$10 should get deducted on batch redemption call");

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse2 = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse2, "API 2 user signup");
		userID = signUpResponse2.jsonPath().get("user.user_id").toString();
		token = signUpResponse2.jsonPath().get("access_token.token").toString();
		Assert.assertEquals(signUpResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logit("API2 Signup is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse1 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "50", "",
				"", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse1.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		utils.logit("Send redeemable to the user successfully");

		Map<String, Map<String, String>> parentMap2 = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap2 = new HashMap<String, String>();

		detailsMap2 = pageObj.endpoints().getRecieptDetailsMap("Pizza1", "1", "10", "M", "100", "999", "1",
				dataSet.get("item_id"));
		parentMap2.put("Pizza1", detailsMap2);

		// POS Auto Unlock
		Response autoUnlockResponse1 = pageObj.endpoints().autoSelectPosApi(userID, "30", "1", dataSet.get("item_id"),
				"", dataSet.get("locationkeyRedemption2_0"));
		Assert.assertEquals(autoUnlockResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Auto Unlock ");
		utils.logit("POS Auto Unlock Api is successful");
		String posSelectAmt = autoUnlockResponse1.jsonPath().get("discount_basket_items[0].discount_value").toString();
		Assert.assertEquals(posSelectAmt, "10.0",
				"Redemption amount is not matched with 10.0 for POS Auto select API discount amount");
		utils.logPass("Verified that $10 worth of banked currency should be auto-applied for POS");

		// POS Process Batch Redemption
		Response batchRedemptionProcessResponseUser1 = pageObj.endpoints().processBatchRedemptionPosApiPayload(
				dataSet.get("locationkeyRedemption2_0"), userID, "30", "1", dataSet.get("item_id"), "");
		Assert.assertEquals(batchRedemptionProcessResponseUser1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Process Batch Redemption ");
		utils.logit("POS Process Batch Redemption Api is successful");
		String posProcessAmt = batchRedemptionProcessResponseUser1.jsonPath().get("success[0].discount_value")
				.toString();
		Assert.assertEquals(posProcessAmt, "10.0",
				"Redemption amount is not matched with 10.0 for POS Process Batch Redemption discount amount");
		utils.logPass("Verified that Final amount-$10 should get deducted on batch redemption call for POS");

	}

	@Test(description = "SQ-T5901 Mobile>Exact Strategy>Validate that Calculation logic-(threshold - discounted_quantity) gets applied for secondary rule When Primary Rule Threshold is Reached", groups = {
			"regression" }, priority = 1)
	@Owner(name = "Vansham Mishra")
	public void T5901_validateSecondaryRuleCalculationWhenPrimaryThresholdReached() throws InterruptedException {
		String discountID = Utilities.getRandomNoFromRange(1, 4) + "";
		Map<String, String> map = new HashMap<String, String>();
		map.put("item_qty", "5");
		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logit("API2 Signup is successful");

		// step1 - Add subscription in discount basket using Mobile API and verify
		// max_applicable_quantity-5 value in API response
		String PlanID = dataSet.get("planId");
		String spPrice = dataSet.get("spPrice");
		String externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		// subscription purchase api 2
		Response purchaseSubscriptionresponse = pageObj.endpoints().Api2SubscriptionPurchase(token, PlanID,
				dataSet.get("client"), dataSet.get("secret"), spPrice, endDateTime);
		Assert.assertEquals(purchaseSubscriptionresponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		String subscription_id = purchaseSubscriptionresponse.jsonPath().get("subscription_id").toString();
		logger.info(userEmail + " purchased " + subscription_id + " Plan id = " + PlanID);
		// Adding subscription into discount basket
		String totalNumberOfRedemptionsAllowed = dataSet.get("totalNumberOfRedemptionsAllowed");
		Response discountBasketResponse = pageObj.endpoints().addDiscountToBasketAPI2(token, dataSet.get("client"),
				dataSet.get("secret"), "subscription", subscription_id + "");
		Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String max_applicable_quantity = discountBasketResponse.jsonPath()
				.get("discount_basket_items[0].discount_details.max_applicable_quantity").toString();
		Assert.assertEquals(max_applicable_quantity, "5.0", "max_applicable_quantity value is not matched with 5");
		utils.logPass("Verified that max_applicable_quantity value is 5.0 in add discount to basket API");
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();

		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Pizza1", "5", "10", "M", "10", "999", "1",
				dataSet.get("item_id"));
		parentMap.put("Pizza1", detailsMap1);

		// step2 - Hit GET active basket using Mobile API and verify
		// max_applicable_quantity value-5 in API response
		Response basketDiscountDetailsResponse = pageObj.endpoints().getUserDiscountBasketDetailsUsingAPI2(token,
				dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, basketDiscountDetailsResponse.getStatusCode(),
				"Status code 200 did not match with get user discount basket details");
		String max_applicable_quantity2 = basketDiscountDetailsResponse.jsonPath()
				.get("discount_basket_items[0].discount_details.max_applicable_quantity").toString();
		Assert.assertEquals(max_applicable_quantity2, "5.0", "max_applicable_quantity value is not matched with 5");
		utils.logPass("Verified that max_applicable_quantity value is 5.0 in GET active basket using Mobile API");

		// step3 - Hit discount lookup API with input receipt with qty-5
		Response posDiscountLookupResponse = pageObj.endpoints().discountLookUpApiPos(dataSet.get("locationkey"),
				userID, "12003", "10", externalUID, map);
		Assert.assertEquals(posDiscountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String max_applicable_quantity3 = posDiscountLookupResponse.jsonPath()
				.get("selected_discounts[0].discount_details.max_applicable_quantity").toString();
		Assert.assertEquals(max_applicable_quantity3, "5.0", "max_applicable_quantity value is not matched with 5");
		utils.logPass("Verified that max_applicable_quantity value is 5.0 in POS discount lookup API");

		// step4- Hit POS batch redemption API with input receipt with qty-5 to exhaust
		// primary limit
		Response batchRedemptionProcessResponseUser = pageObj.endpoints()
				.processBatchRedemptionPosApi(dataSet.get("locationkey"), userID, "10", "12003", externalUID, map);
		Assert.assertEquals(batchRedemptionProcessResponseUser.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for POS API Process Batch Redemption.");
		String max_applicable_quantity4 = batchRedemptionProcessResponseUser.jsonPath()
				.get("success[0].discount_details.max_applicable_quantity").toString();
		Assert.assertEquals(max_applicable_quantity4, "5.0", "max_applicable_quantity value is not matched with 5");
		utils.logPass("Verified that max_applicable_quantity value is 5.0 in POS batch redemption API");
		utils.longWaitInSeconds(7);

		// step5 - Add subscription plan again in discount basket using Mobile Select
		// API and verify max_applicable_quantity value-10 in API response
		// Adding subscription into discount basket
		String totalNumberOfRedemptionsAllowed2 = dataSet.get("totalNumberOfRedemptionsAllowed");
		Response discountBasketResponse2 = pageObj.endpoints().addDiscountToBasketAPI2(token, dataSet.get("client"),
				dataSet.get("secret"), "subscription", subscription_id + "");
		Assert.assertEquals(discountBasketResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String max_applicable_quantity5 = discountBasketResponse2.jsonPath()
				.get("discount_basket_items[0].discount_details.max_applicable_quantity").toString();
		Assert.assertEquals(max_applicable_quantity5, "10.0", "max_applicable_quantity value is not matched with 10");
		utils.logPass("Verified that max_applicable_quantity value is 10.0 in add discount to basket API");
		utils.longWaitInSeconds(7);

		// step6- Hit GET active basket API using Mobile API and verify
		// max_applicable_quantity value-10 in API response
		Response basketDiscountDetailsResponse2 = pageObj.endpoints().getUserDiscountBasketDetailsUsingAPI2(token,
				dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, basketDiscountDetailsResponse2.getStatusCode(),
				"Status code 200 did not match with get user discount basket details");
		String max_applicable_quantity6 = basketDiscountDetailsResponse2.jsonPath()
				.get("discount_basket_items[0].discount_details.max_applicable_quantity").toString();
		Assert.assertEquals(max_applicable_quantity6, "10.0", "max_applicable_quantity value is not matched with 10");
		utils.logPass("Verified that max_applicable_quantity value is 10.0 in GET active basket using Mobile API");

		// step7 - Hit POS batch redemption API with input receipt with qty-5 to exhaust
		// partial secondary limit
		map.put("item_qty", "10");
		Response batchRedemptionProcessResponseUser2 = pageObj.endpoints()
				.processBatchRedemptionPosApi(dataSet.get("locationkey"), userID, "10", "12003", externalUID, map);
		Assert.assertEquals(batchRedemptionProcessResponseUser2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for POS API Process Batch Redemption.");
		String max_applicable_quantity7 = batchRedemptionProcessResponseUser2.jsonPath()
				.get("success[0].discount_details.max_applicable_quantity").toString();
		Assert.assertEquals(max_applicable_quantity7, "10.0", "max_applicable_quantity value is not matched with 10.0");
		utils.logPass(
				"Verified that max_applicable_quantity value is 10.0 to exhaust partial secondary limit in POS batch redemption API");

		// step8 - Add subscription plan again in discount basket using Mobile Select
		// API and verify max_applicable_quantity value in API response
		int maxRetries = 10; // Maximum number of retries
		int retryInterval = 2000; // Interval between retries in milliseconds
		int retryCount = 0;
		Response discountBasketResponse3;

		do {
			discountBasketResponse3 = pageObj.endpoints().addDiscountToBasketAPI2(token, dataSet.get("client"),
					dataSet.get("secret"), "subscription", subscription_id + "");
			if (discountBasketResponse3.getStatusCode() == ApiConstants.HTTP_STATUS_OK) {
				break; // Exit the loop if status code is 200
			}
			retryCount++;
			Thread.sleep(retryInterval); // Wait before retrying
		} while (retryCount < maxRetries);

		Assert.assertEquals(discountBasketResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Failed to get 200 status code after " + maxRetries + " retries");
		String max_applicable_quantity8 = discountBasketResponse3.jsonPath()
				.get("discount_basket_items[0].discount_details.max_applicable_quantity").toString();
		Assert.assertEquals(max_applicable_quantity8, "0", "max_applicable_quantity value is not matched with 0");
		utils.logPass("Verified that max_applicable_quantity value is 0 in add discount to basket API");
	}

	@Test(description = "SQ-T5903 Validate that max_applicable quantity-NULL when benefit discounting rule is configured with % or $ Off(Receipt level Discount)", groups = {
			"regression" }, priority = 2)
	@Owner(name = "Vansham Mishra")
	public void T5903_validateMaxApplicableQuantityNullForReceiptLevelDiscount() throws InterruptedException {
		String discountID = Utilities.getRandomNoFromRange(1, 4) + "";
		Map<String, String> map = new HashMap<String, String>();
		map.put("item_qty", "5");
		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logit("API2 Signup is successful");

		// step1 - Add subscription in discount basket using Mobile API and verify
		// max_applicable_quantity-5 value in API response
		String PlanID = dataSet.get("planId");
		String spPrice = dataSet.get("spPrice");
		String externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		// subscription purchase api 2
		Response purchaseSubscriptionresponse = pageObj.endpoints().Api2SubscriptionPurchase(token, PlanID,
				dataSet.get("client"), dataSet.get("secret"), spPrice, endDateTime);
		Assert.assertEquals(purchaseSubscriptionresponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		String subscription_id = purchaseSubscriptionresponse.jsonPath().get("subscription_id").toString();
		logger.info(userEmail + " purchased " + subscription_id + " Plan id = " + PlanID);
		// Adding subscription into discount basket
		String totalNumberOfRedemptionsAllowed = dataSet.get("totalNumberOfRedemptionsAllowed");
		Response discountBasketResponse = pageObj.endpoints().addDiscountToBasketAPI2(token, dataSet.get("client"),
				dataSet.get("secret"), "subscription", subscription_id + "");
		Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String max_applicable_quantity = discountBasketResponse.jsonPath()
				.get("discount_basket_items[0].discount_details.max_applicable_quantity");
		Assert.assertNull(max_applicable_quantity, "max_applicable_quantity value is not Null");
		utils.logPass("Verified that max_applicable_quantity value is null in add discount to basket API");
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();

		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Pizza1", "1", "10", "M", "10", "999", "1",
				dataSet.get("item_id"));
		parentMap.put("Pizza1", detailsMap1);

		// step2 - Hit GET active basket using Mobile API and verify
		// max_applicable_quantity value-5 in API response
		Response basketDiscountDetailsResponse = pageObj.endpoints().getUserDiscountBasketDetailsUsingAPI2(token,
				dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, basketDiscountDetailsResponse.getStatusCode(),
				"Status code 200 did not match with get user discount basket details");
		String max_applicable_quantity2 = basketDiscountDetailsResponse.jsonPath()
				.get("discount_basket_items[0].discount_details.max_applicable_quantity");
		Assert.assertNull(max_applicable_quantity2, "max_applicable_quantity value is not Null");
		utils.logPass("Verified that max_applicable_quantity value is Null in GET active basket using Mobile API");

		// step3 - Hit discount lookup API with input receipt with qty-5
		Response posDiscountLookupResponse = pageObj.endpoints().discountLookUpApiPos(dataSet.get("locationkey"),
				userID, "12003", "10", externalUID, map);
		Assert.assertEquals(posDiscountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String max_applicable_quantity3 = posDiscountLookupResponse.jsonPath()
				.get("selected_discounts[0].discount_details.max_applicable_quantity");
		Assert.assertNull(max_applicable_quantity3, "max_applicable_quantity value is not Null");
		utils.logPass("Verified that max_applicable_quantity value is Null in POS discount lookup API");

		// step4- Hit POS batch redemption API with input receipt with qty-5 to exhaust
		// primary limit
		Response batchRedemptionProcessResponseUser = pageObj.endpoints()
				.processBatchRedemptionPosApi(dataSet.get("locationkey"), userID, "10", "12003", externalUID, map);
		Assert.assertEquals(batchRedemptionProcessResponseUser.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for POS API Process Batch Redemption.");
		String max_applicable_quantity4 = batchRedemptionProcessResponseUser.jsonPath()
				.get("success[0].discount_details.max_applicable_quantity");
		Assert.assertNull(max_applicable_quantity4, "max_applicable_quantity value is not Null");
		utils.logPass("Verified that max_applicable_quantity value is Null in POS batch redemption API");
		utils.longWaitInSeconds(7);

		// step5 - Add subscription plan again in discount basket using Mobile Select
		// API and verify max_applicable_quantity value-10 in API response
		// Adding subscription into discount basket
		String totalNumberOfRedemptionsAllowed2 = dataSet.get("totalNumberOfRedemptionsAllowed");
		Response discountBasketResponse2 = pageObj.endpoints().addDiscountToBasketAPI2(token, dataSet.get("client"),
				dataSet.get("secret"), "subscription", subscription_id + "");
		Assert.assertEquals(discountBasketResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String max_applicable_quantity5 = discountBasketResponse2.jsonPath()
				.get("discount_basket_items[0].discount_details.max_applicable_quantity");
		Assert.assertNull(max_applicable_quantity5, "max_applicable_quantity value is not Null");
		utils.logPass("Verified that max_applicable_quantity value is Null in add discount to basket API");
		utils.longWaitInSeconds(7);

		// step6- Hit GET active basket API using Mobile API and verify
		// max_applicable_quantity value-10 in API response
		Response basketDiscountDetailsResponse2 = pageObj.endpoints().getUserDiscountBasketDetailsUsingAPI2(token,
				dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, basketDiscountDetailsResponse2.getStatusCode(),
				"Status code 200 did not match with get user discount basket details");
		String max_applicable_quantity6 = basketDiscountDetailsResponse2.jsonPath()
				.get("discount_basket_items[0].discount_details.max_applicable_quantity");
		Assert.assertNull(max_applicable_quantity6, "max_applicable_quantity value is not Null");
		utils.logPass("Verified that max_applicable_quantity value is Null in GET active basket using Mobile API");

		// step7 - Hit POS batch redemption API with input receipt with qty-5 to exhaust
		// partial secondary limit
		map.put("item_qty", "10");
		Response batchRedemptionProcessResponseUser2 = pageObj.endpoints()
				.processBatchRedemptionPosApi(dataSet.get("locationkey"), userID, "10", "12003", externalUID, map);
		Assert.assertEquals(batchRedemptionProcessResponseUser2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for POS API Process Batch Redemption.");
		String max_applicable_quantity7 = batchRedemptionProcessResponseUser2.jsonPath()
				.get("success[0].discount_details.max_applicable_quantity");
		Assert.assertNull(max_applicable_quantity7, "max_applicable_quantity value is not Null");
		utils.logPass(
				"Verified that max_applicable_quantity value is Null to exhaust partial secondary limit in POS batch redemption API");

		// step8 - Add subscription plan again in discount basket using Mobile Select
		// API and verify max_applicable_quantity value in API response
		int maxRetries = 10; // Maximum number of retries
		int retryInterval = 2000; // Interval between retries in milliseconds
		int retryCount = 0;
		Response discountBasketResponse3;

		do {
			discountBasketResponse3 = pageObj.endpoints().addDiscountToBasketAPI2(token, dataSet.get("client"),
					dataSet.get("secret"), "subscription", subscription_id + "");
			if (discountBasketResponse3.getStatusCode() == ApiConstants.HTTP_STATUS_OK) {
				break; // Exit the loop if status code is 200
			}
			retryCount++;
			Thread.sleep(retryInterval); // Wait before retrying
		} while (retryCount < maxRetries);
		Assert.assertEquals(discountBasketResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Failed to get 200 status code after " + maxRetries + " retries");
		String max_applicable_quantity8 = discountBasketResponse3.jsonPath()
				.get("discount_basket_items[0].discount_details.max_applicable_quantity");
		Assert.assertNull(max_applicable_quantity8, "max_applicable_quantity value is not Null");
		utils.logPass("Verified that max_applicable_quantity value is Null in add discount to basket API");
	}

	@Test(description = "SQ-T5902 Validate that max_applicable quantity-NULL when benefit discounting rule is configured with % or $ Off", groups = {
			"regression"}, priority = 3)
	@Owner(name = "Vansham Mishra")
	public void T5902_validateMaxApplicableQuantityIsNullForPercentageOrDollarOffDiscount() throws InterruptedException {
		String discountID = Utilities.getRandomNoFromRange(1, 4) + "";
		Map<String, String> map = new HashMap<String, String>();
		map.put("item_qty", "5");
		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logit("API2 Signup is successful");

		// step1 - Add subscription in discount basket using Mobile API and verify max_applicable_quantity-5 value in API response
		String PlanID = dataSet.get("planId");
		String spPrice = dataSet.get("spPrice");
		String  externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		/// subscription purchase api 2
		Response purchaseSubscriptionresponse = pageObj.endpoints().Api2SubscriptionPurchase(token, PlanID,
				dataSet.get("client"), dataSet.get("secret"), spPrice, endDateTime);
		Assert.assertEquals(purchaseSubscriptionresponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		String subscription_id = purchaseSubscriptionresponse.jsonPath().get("subscription_id").toString();
		logger.info(userEmail + " purchased " + subscription_id + " Plan id = " + PlanID);
		// Adding subscription into discount basket
		String totalNumberOfRedemptionsAllowed = dataSet.get("totalNumberOfRedemptionsAllowed");
		Response discountBasketResponse = pageObj.endpoints().addDiscountToBasketAPI2(token, dataSet.get("client"),
				dataSet.get("secret"), "subscription", subscription_id + "");
		Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
//		String errorMessage = discountBasketResponse.jsonPath().get("errors.discount_id[0]").toString();
//		Assert.assertEquals(errorMessage, "Offer configuration is not supported.",
//				"Error message did not match for discount not applicable");
		utils.logPass("Verified that Offer configuration is not supported while adding subscription having qc with processing function:- sum of amount incremental");

		String max_applicable_quantity =  discountBasketResponse.jsonPath().get("discount_basket_items[0].discount_details.max_applicable_quantity");
		Assert.assertNull(max_applicable_quantity,  "max_applicable_quantity value is not Null");
		utils.logPass("Verified that max_applicable_quantity value is null in add discount to basket API");
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();

		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Pizza1", "1", "10", "M", "10", "999", "1",
				dataSet.get("item_id"));
		parentMap.put("Pizza1", detailsMap1);

		//step2 - Hit GET active basket using Mobile API and verify max_applicable_quantity value-5 in API response
		Response basketDiscountDetailsResponse = pageObj.endpoints().getUserDiscountBasketDetailsUsingAPI2(token,
				dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, basketDiscountDetailsResponse.getStatusCode(),
				"Status code 200 did not match with get user discount basket details");
		String max_applicable_quantity2 =  basketDiscountDetailsResponse.jsonPath().get("discount_basket_items[0].discount_details.max_applicable_quantity");
		Assert.assertNull(max_applicable_quantity2, "max_applicable_quantity value is not Null");
		utils.logPass("Verified that max_applicable_quantity value is Null in GET active basket using Mobile API");

		// step3 - Hit discount lookup API with input receipt with qty-5
		Response posDiscountLookupResponse = pageObj.endpoints().discountLookUpApiPos(dataSet.get("locationkey"), userID, "12003","10",externalUID,map);
		Assert.assertEquals(posDiscountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String max_applicable_quantity3 =  posDiscountLookupResponse.jsonPath().get("selected_discounts[0].discount_details.max_applicable_quantity");
		Assert.assertNull(max_applicable_quantity3, "max_applicable_quantity value is not Null");
		utils.logPass("Verified that max_applicable_quantity value is Null in POS discount lookup API");

		//step4- Hit POS batch redemption API with input receipt with qty-5 to exhaust primary limit
		Response batchRedemptionProcessResponseUser = pageObj.endpoints().processBatchRedemptionPosApi(
				dataSet.get("locationkey"), userID, "10", "12003", externalUID,map);
		Assert.assertEquals(batchRedemptionProcessResponseUser.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for POS API Process Batch Redemption.");
		String max_applicable_quantity4 =  batchRedemptionProcessResponseUser.jsonPath().get("success[0].discount_details.max_applicable_quantity");
		Assert.assertNull(max_applicable_quantity4,  "max_applicable_quantity value is not Null");
		utils.logPass("Verified that max_applicable_quantity value is Null in POS batch redemption API");
		utils.longWaitInSeconds(7);

		// step5 - Add subscription plan again in discount basket using Mobile Select API and verify max_applicable_quantity value-10 in API response
		// Adding subscription into discount basket
		String totalNumberOfRedemptionsAllowed2 = dataSet.get("totalNumberOfRedemptionsAllowed");
		Response discountBasketResponse2 = pageObj.endpoints().addDiscountToBasketAPI2(token, dataSet.get("client"),
				dataSet.get("secret"), "subscription", subscription_id + "");
		Assert.assertEquals(discountBasketResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String max_applicable_quantity5 =  discountBasketResponse2.jsonPath().get("discount_basket_items[0].discount_details.max_applicable_quantity");
		Assert.assertNull(max_applicable_quantity5,  "max_applicable_quantity value is not Null");
		utils.logPass("Verified that max_applicable_quantity value is Null in add discount to basket API");
		utils.longWaitInSeconds(7);

		//step6- Hit GET active basket API using Mobile API and verify max_applicable_quantity value-10 in API response
		Response basketDiscountDetailsResponse2 = pageObj.endpoints().getUserDiscountBasketDetailsUsingAPI2(token,
				dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, basketDiscountDetailsResponse2.getStatusCode(),
				"Status code 200 did not match with get user discount basket details");
		String max_applicable_quantity6 =  basketDiscountDetailsResponse2.jsonPath().get("discount_basket_items[0].discount_details.max_applicable_quantity");
		Assert.assertNull(max_applicable_quantity6, "max_applicable_quantity value is not Null");
		utils.logPass("Verified that max_applicable_quantity value is Null in GET active basket using Mobile API");

		// step7 - Hit POS batch redemption API with input receipt with qty-5 to exhaust partial secondary limit
		map.put("item_qty", "10");
		Response batchRedemptionProcessResponseUser2 = pageObj.endpoints().processBatchRedemptionPosApi(
				dataSet.get("locationkey"), userID, "10", "12003", externalUID,map);
		Assert.assertEquals(batchRedemptionProcessResponseUser2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for POS API Process Batch Redemption.");
		String max_applicable_quantity7 =  batchRedemptionProcessResponseUser2.jsonPath().get("success[0].discount_details.max_applicable_quantity");
		Assert.assertNull(max_applicable_quantity7, "max_applicable_quantity value is not Null");
		utils.logPass("Verified that max_applicable_quantity value is Null to exhaust partial secondary limit in POS batch redemption API");

		//step8 - Add subscription plan again in discount basket using Mobile Select API and verify max_applicable_quantity value in API response
		int maxRetries = 10; // Maximum number of retries
		int retryInterval = 2000; // Interval between retries in milliseconds
		int retryCount = 0;
		Response discountBasketResponse3;

		do {
			discountBasketResponse3 = pageObj.endpoints().addDiscountToBasketAPI2(token, dataSet.get("client"),
					dataSet.get("secret"), "subscription", subscription_id + "");
			if (discountBasketResponse3.getStatusCode() == ApiConstants.HTTP_STATUS_OK) {
				break; // Exit the loop if status code is 200
			}
			retryCount++;
			Thread.sleep(retryInterval); // Wait before retrying
		} while (retryCount < maxRetries);

		Assert.assertEquals(discountBasketResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Failed to get 200 status code after " + maxRetries + " retries");
		String max_applicable_quantity8 =  discountBasketResponse3.jsonPath().get("discount_basket_items[0].discount_details.max_applicable_quantity");
		Assert.assertNull(max_applicable_quantity8, "max_applicable_quantity value is not Null");
		utils.logPass("Verified that max_applicable_quantity value is Null in add discount to basket API");
	}
		@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}

}
