package com.punchh.server.OMMTest;

import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
import com.punchh.server.apiConfig.ApiResponseJsonSchema;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.ApiUtils;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

/*
 * @Author : Shashank Sharma 
 * SQ-T2748 (1.0)
 */
@Listeners(TestListeners.class)
public class OMM252AuthAPIAddDeleteDiscountsToBasketTest {
	static Logger logger = LogManager.getLogger(OMM252AuthAPIAddDeleteDiscountsToBasketTest.class);
	public WebDriver driver;
	private String userEmail;
	private ApiUtils apiUtils;
	private Properties prop;
	private PageObj pageObj;
	private String sTCName;
	private String env, run = "ui";
	private String baseUrl;
	private static Map<String, String> dataSet;
	private boolean GlobalBenefitRedemptionThrottlingToggle;
	private String endDateTime;
	private Utilities utils;

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
		prop = Utilities.loadPropertiesFile("config.properties");
		// env = prop.getProperty("environment");
		driver = new BrowserUtilities().launchBrowser();
		sTCName = method.getName();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		apiUtils = new ApiUtils();
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run , env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run , env , "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==>" + dataSet);
		GlobalBenefitRedemptionThrottlingToggle = false;
		endDateTime = CreateDateTime.getFutureDate(1) + " 10:00 PM";
		utils = new Utilities(driver);

	}

	@Test(description = "OMM-T29 Verify API parameter validations for Add Discounts To Basket API (mobile)", priority = 9, groups = {
			"sanity", "dailyrun" })
	@Owner(name ="Shashank Sharma")
	public void T29_AddRewardDiscountToBasketAUTH() throws InterruptedException {

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Navigate to Multiple Redemption Tab and check the flags
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_auto_redemption", "check");
		pageObj.cockpitRedemptionsPage().setAutoRedemptionDiscounts("Offers", "Select");
		pageObj.dashboardpage().updateCheckBox();

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logit("API2 Signup is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				dataSet.get("redeemable_id"), "", "");

		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		utils.logPass("Api2  send reward amount to user is successful");

		Response rewardResponse = pageObj.endpoints().authListAvailableRewardsNew(token, dataSet.get("client"),
				dataSet.get("secret"));

		String rewardID = rewardResponse.jsonPath().getString("id").replace("[", "").replace("]", "");

		utils.logit("Reward id " + rewardID + " is generated successfully ");

		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAddedAUTH(token,
				dataSet.get("client"), dataSet.get("secret"), "reward", rewardID);
		Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean isAuthAddSelectionDiscountBasketSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.authAutoSelectSchema, discountBasketResponse.asString());
		Assert.assertTrue(isAuthAddSelectionDiscountBasketSchemaValidated,
				"Auth Add Selection to Discount Basket schema validation failed");
		String actualDiscountType_InBasketAddResponse = discountBasketResponse.jsonPath()
				.getString("discount_basket_items.discount_type");
		String actualDiscountID_InBasketAddResponse = discountBasketResponse.jsonPath()
				.getString("discount_basket_items.discount_id").replace("[", "").replace("]", "");

		Assert.assertEquals(actualDiscountType_InBasketAddResponse, "[reward]");
		Assert.assertEquals(actualDiscountID_InBasketAddResponse, rewardID);

		utils.logPass("Verified the discount type and ID in Basket add response");

		String expdiscount_basket_item_id = discountBasketResponse.jsonPath()
				.getString("discount_basket_items.discount_basket_item_id").replace("[", "").replace("]", "");

		utils.logit("User " + userEmail + " add the " + rewardID + " into discount basket successfully");

		String discount_basket_item_id = discountBasketResponse.jsonPath()
				.getString("discount_basket_items.discount_basket_item_id");

		String discount_id = discountBasketResponse.jsonPath().getString("discount_basket_items.discount_id")
				.replace("[", "").replace("]", "");
		Assert.assertEquals(discount_id, rewardID);

		Response basketDiscountDetailsResponse = pageObj.endpoints().getUserDiscountBasketDetailsUsingAUTH(token,
				dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(basketDiscountDetailsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean isAuthGetActiveDiscountBasketSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.authAutoSelectSchema, basketDiscountDetailsResponse.asString());
		Assert.assertTrue(isAuthGetActiveDiscountBasketSchemaValidated,
				"Auth Get Active Discount Basket schema validation failed");
		String actualDiscountBasketItemIDFromBasket = basketDiscountDetailsResponse.jsonPath()
				.getString("discount_basket_items.discount_basket_item_id").replace("[", "").replace("]", "");
		String actualDiscountIDFromBasket = basketDiscountDetailsResponse.jsonPath()
				.getString("discount_basket_items.discount_id").replace("[", "").replace("]", "");

		Assert.assertEquals(actualDiscountIDFromBasket, rewardID);
		Assert.assertEquals(expdiscount_basket_item_id, actualDiscountBasketItemIDFromBasket);

		expdiscount_basket_item_id = expdiscount_basket_item_id.replace("[", "").replace("]", "");
		Response deleteBasketResponse = pageObj.endpoints().deleteDiscountBasketForUserAUTH(token,
				dataSet.get("client"), dataSet.get("secret"), expdiscount_basket_item_id);

		String delete_discount_basket_items = deleteBasketResponse.jsonPath().get("deleteBasketResponse");

		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, deleteBasketResponse.getStatusCode());
		Assert.assertEquals(delete_discount_basket_items, null);

		utils.logPass("Verified that basket is deleted successfully");

	}

	@Test(description = "OMM-T29 Verify API parameter validations for Cupon added into discountsm to basket ", priority = 2, groups = {
			"regression", "unstable", "dailyrun" })
	@Owner(name ="Shashank Sharma")
	public void T29_AddRedemptionCuponCodeIntoUserDiscountBasketAUTH() throws InterruptedException, ParseException {
		String coupanCampaignName = "Auto_CuponCampaign" + CreateDateTime.getTimeDateString();

		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().createWhatDetailsCoupanCampaign(coupanCampaignName);
		pageObj.signupcampaignpage().setCouponCampaignUsageType(dataSet.get("usageType"));
		pageObj.signupcampaignpage().setCouponCampaignCodeGenerationType(dataSet.get("codeType"));
		pageObj.signupcampaignpage().setCouponCampaignGuestUsagewithGiftAmount(dataSet.get("noOfGuests"),
				dataSet.get("usagePerGuest"), dataSet.get("giftType"), dataSet.get("amount"), "", "",
				GlobalBenefitRedemptionThrottlingToggle);

		pageObj.signupcampaignpage().setStartDate();
		pageObj.signupcampaignpage().setEndDateforCouponCampaign(1);

		pageObj.signupcampaignpage().createWhenDetailsCampaign();
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "Campaign created success message did not displayed....");
		utils.logPass("Coupon campaign created successfuly");

		Thread.sleep(8000);
		pageObj.campaignspage().searchCampaign(coupanCampaignName);
		String generatedCodeName = pageObj.campaignspage().getPreGeneratedCuponCode();

		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAddedAUTH(token,
				dataSet.get("client"), dataSet.get("secret"), "redemption_code", generatedCodeName);

		// verified that coupon name in add basket response
		String actual_discount_discount_id = discountBasketResponse.jsonPath()
				.getString("discount_basket_items.discount_id");
		actual_discount_discount_id = actual_discount_discount_id.replace("[", "").replace("]", "");
		Assert.assertEquals(actual_discount_discount_id, generatedCodeName);

		utils.logPass("Verified that " + actual_discount_discount_id + " in add basket response");

		String actual_discount_type_addBasketResponse = discountBasketResponse.jsonPath()
				.getString("discount_basket_items.discount_type");

		Assert.assertEquals(actual_discount_type_addBasketResponse, "[redemption_code]");

		utils.logPass("Verified the redemption_code in add basket response");

		String expdiscount_basket_item_id = discountBasketResponse.jsonPath()
				.getString("discount_basket_items.discount_basket_item_id").replace("[", "").replace("]", "");
		actual_discount_discount_id = actual_discount_discount_id.replace("[", "").replace("]", "");
		Assert.assertEquals(actual_discount_discount_id, generatedCodeName);

		Response basketDiscountDetailsResponse = pageObj.endpoints().getUserDiscountBasketDetailsUsingAUTH(token,
				dataSet.get("client"), dataSet.get("secret"));

		String actualDiscountBasketItemIDFromBasket = basketDiscountDetailsResponse.jsonPath()
				.getString("discount_basket_items.discount_basket_item_id").replace("[", "").replace("]", "");
		String actualPromoCodeFromBasket = basketDiscountDetailsResponse.jsonPath()
				.getString("discount_basket_items.discount_id").replace("[", "").replace("]", "");

		Assert.assertEquals(actualDiscountBasketItemIDFromBasket, expdiscount_basket_item_id);
		Assert.assertEquals(actualPromoCodeFromBasket, generatedCodeName);

		expdiscount_basket_item_id = expdiscount_basket_item_id.replace("[", "").replace("]", "");
		Response deleteBasketResponse = pageObj.endpoints().deleteDiscountBasketForUserAUTH(token,
				dataSet.get("client"), dataSet.get("secret"), expdiscount_basket_item_id);

		String delete_discount_basket_items = deleteBasketResponse.jsonPath().get("deleteBasketResponse");

		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, deleteBasketResponse.getStatusCode());
		Assert.assertEquals(delete_discount_basket_items, null);

		utils.logPass("Verified that basket id " + expdiscount_basket_item_id + " is deleted successfully");

	}

	@Test(priority = 3, groups = { "regression", "dailyrun" })
	@Owner(name ="Shashank Sharma")
	public void T29_AddingPromoCodeCuponToUserDiscountBasketAUTH() throws InterruptedException, ParseException {

		String promoCode = CreateDateTime.getRandomString(6).toUpperCase() + Utilities.getRandomNoFromRange(500, 2000);
		String coupanCampaignName = "Auto_PromoCampaign" + CreateDateTime.getTimeDateString();
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().createWhatDetailsPromoCampaign(coupanCampaignName, promoCode);
		pageObj.signupcampaignpage().setCouponCampaignUsageType(dataSet.get("usageType"));

		pageObj.signupcampaignpage().setPromoCampaignGuestUsagewithGiftAmount(dataSet.get("noOfGuests"),
				dataSet.get("giftType"), dataSet.get("amount"), "", "");

		pageObj.signupcampaignpage().setStartDate();
		pageObj.signupcampaignpage().setEndDateforCouponCampaign(1);

		pageObj.signupcampaignpage().createWhenDetailsCampaign();
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "Campaign created success message did not displayed....");
		utils.logPass("Coupon campaign created successfuly");

		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAddedAUTH(token,
				dataSet.get("client"), dataSet.get("secret"), "redemption_code", promoCode);
		Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		String actual_discount_discount_id = discountBasketResponse.jsonPath()
				.getString("discount_basket_items.discount_id").replace("[", "").replace("]", "");
		actual_discount_discount_id = actual_discount_discount_id.replace("[", "").replace("]", "");
		Assert.assertEquals(actual_discount_discount_id, promoCode);

		utils.logPass("Verified that " + actual_discount_discount_id + " in add basket response");

		String actual_discount_type_addBasketResponse = discountBasketResponse.jsonPath()
				.getString("discount_basket_items.discount_type");

		Assert.assertEquals(actual_discount_type_addBasketResponse, "[redemption_code]");

		utils.logPass("Verified the redemption_code in add basket response");

		String expdiscount_basket_item_id = discountBasketResponse.jsonPath()
				.getString("discount_basket_items.discount_basket_item_id").replace("[", "").replace("]", "");

		Response basketDiscountDetailsResponse = pageObj.endpoints().getUserDiscountBasketDetailsUsingAUTH(token,
				dataSet.get("client"), dataSet.get("secret"));

		String actualDiscountBasketItemIDFromBasket = basketDiscountDetailsResponse.jsonPath()
				.getString("discount_basket_items.discount_basket_item_id").replace("[", "").replace("]", "");
		String actualPromoCodeFromBasket = basketDiscountDetailsResponse.jsonPath()
				.getString("discount_basket_items.discount_id").replace("[", "").replace("]", "");
		String actualDiscountType = basketDiscountDetailsResponse.jsonPath()
				.getString("discount_basket_items.discount_type").replace("[", "").replace("]", "");

		Assert.assertEquals(actualDiscountBasketItemIDFromBasket, expdiscount_basket_item_id);
		Assert.assertEquals(actualPromoCodeFromBasket, promoCode);
		Assert.assertEquals(actualDiscountType, "redemption_code");

		expdiscount_basket_item_id = expdiscount_basket_item_id.replace("[", "").replace("]", "");
		Response deleteBasketResponse = pageObj.endpoints().deleteDiscountBasketForUserAUTH(token,
				dataSet.get("client"), dataSet.get("secret"), expdiscount_basket_item_id);
		String delete_discount_basket_items = deleteBasketResponse.jsonPath().get("deleteBasketResponse");

		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, deleteBasketResponse.getStatusCode());
		Assert.assertEquals(delete_discount_basket_items, null);

		utils.logPass("Verified that basket id " + expdiscount_basket_item_id + " is deleted successfully");

	}

	@Test(priority = 4, groups = { "regression", "dailyrun" })
	@Owner(name ="Shashank Sharma")
	public void T29_subscriptionToAddInDiscountBucketAUTH() throws InterruptedException {
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboard("Enable Reward Locking", "uncheck");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboard("Enable Auto-redemption", "check");

		// create subscription plan
		String spName = dataSet.get("subscriptionName");
		String spPrice = Integer.toString(Utilities.getRandomNoFromRange(300, 500));
		String PlanID = dataSet.get("PlanID");

		// create user
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		// subscription purchase api 2
		Response purchaseSubscriptionresponse = pageObj.endpoints().Api2SubscriptionPurchase(token, PlanID,
				dataSet.get("client"), dataSet.get("secret"), spPrice, endDateTime);
		Assert.assertEquals(purchaseSubscriptionresponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);

		int subscription_id = Integer
				.parseInt(purchaseSubscriptionresponse.jsonPath().get("subscription_id").toString());
		logger.info(userEmail + " purchased " + subscription_id + " Plan id = " + PlanID);

		// Adding subscription into discount basket
		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAddedAUTH(token,
				dataSet.get("client"), dataSet.get("secret"), "subscription", subscription_id + "");
		Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		int actual_discount_discount_id = Integer.parseInt(discountBasketResponse.jsonPath()
				.getString("discount_basket_items.discount_id").replace("[", "").replace("]", ""));
		Assert.assertEquals(actual_discount_discount_id, subscription_id);

		utils.logPass("Verified that " + actual_discount_discount_id + " in add basket response");

		String actual_discount_type_addBasketResponse = discountBasketResponse.jsonPath()
				.getString("discount_basket_items.discount_type");

		Assert.assertEquals(actual_discount_type_addBasketResponse, "[subscription]");

		int userSubscriptionID = Integer.parseInt(discountBasketResponse.jsonPath()
				.getString("discount_basket_items.discount_id").replace("[", "").replace("]", ""));
		Assert.assertEquals(userSubscriptionID, subscription_id, "User got the wrong subscription plan ");
		String expDiscountBasketID = discountBasketResponse.jsonPath()
				.getString("discount_basket_items.discount_basket_item_id").replace("[", "").replace("]", "");

		Response basketDiscountDetailsResponse = pageObj.endpoints().getUserDiscountBasketDetailsUsingAUTH(token,
				dataSet.get("client"), dataSet.get("secret"));

		String actualDiscountBasketItemIDFromBasket = basketDiscountDetailsResponse.jsonPath()
				.getString("discount_basket_items.discount_basket_item_id").replace("[", "").replace("]", "");
		String actualDiscpuntIDFromBasket = basketDiscountDetailsResponse.jsonPath()
				.getString("discount_basket_items.discount_id").replace("[", "").replace("]", "");

		String actualSubscriptionNameFromBasket = basketDiscountDetailsResponse.jsonPath()
				.getString("discount_basket_items.discount_details.name").replace("[", "").replace("]", "");

		Assert.assertEquals(actualDiscountBasketItemIDFromBasket, expDiscountBasketID + "");
		Assert.assertEquals(actualDiscpuntIDFromBasket, userSubscriptionID + "");
		Assert.assertEquals(actualSubscriptionNameFromBasket, spName);

		expDiscountBasketID = expDiscountBasketID.replace("[", "").replace("]", "");
		Response deleteBasketResponse = pageObj.endpoints().deleteDiscountBasketForUserAUTH(token,
				dataSet.get("client"), dataSet.get("secret"), expDiscountBasketID);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, deleteBasketResponse.getStatusCode());

		String delete_discount_basket_items = deleteBasketResponse.jsonPath().get("deleteBasketResponse");

		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, deleteBasketResponse.getStatusCode());
		Assert.assertEquals(delete_discount_basket_items, null);

		utils.logPass("Verified that basket id " + expDiscountBasketID + " is deleted successfully");

	}

	@Test(priority = 5, groups = { "regression", "dailyrun" })
	@Owner(name ="Shashank Sharma")
	public void T29_addDiscountAmountToDiscountBasketAUTH() {
		String discountAmount = dataSet.get("discountAmount");
		// create user
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		String userID = signUpResponse.jsonPath().get("user.user_id").toString();

		pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), dataSet.get("discountAmount"), "", "", "");

		// Adding subscription into discount basket
		Response discountBasketResponse = pageObj.endpoints().addDiscountAmountToDiscountBasketAUTH(token,
				dataSet.get("client"), dataSet.get("secret"), "discount_amount", discountAmount);
		Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		String actual_discount_type_addBasketResponse = discountBasketResponse.jsonPath()
				.getString("discount_basket_items.discount_type");

		Assert.assertEquals(actual_discount_type_addBasketResponse, "[discount_amount]");

		String actual_discount_discount_id = discountBasketResponse.jsonPath()
				.getString("discount_basket_items.discount_id").replace("[", "").replace("]", "").replace("]", "")
				.replace("]", "");
		Assert.assertEquals(actual_discount_discount_id, "null");

		String discount_details = discountBasketResponse.jsonPath().getString("discount_basket_items.discount_details")
				.replace("[", "").replace("]", "").replace("]", "").replace("]", "");
		Assert.assertEquals(discount_details, "null");

		String expDiscountBasketID = discountBasketResponse.jsonPath()
				.getString("discount_basket_items.discount_basket_item_id").replace("[", "").replace("]", "");

		utils.logit("User add the discount ammount to his discount basket successfully");

		Response basketDiscountDetailsResponse = pageObj.endpoints().getUserDiscountBasketDetailsUsingAUTH(token,
				dataSet.get("client"), dataSet.get("secret"));

		String actualDiscountBasketItemIDFromBasket = basketDiscountDetailsResponse.jsonPath()
				.getString("discount_basket_items.discount_basket_item_id").replace("[", "").replace("]", "");

		int expDiscountValue = Integer.parseInt(dataSet.get("discountAmount"));
		int actualDiscountValue = Integer.parseInt(basketDiscountDetailsResponse.jsonPath()
				.getString("discount_basket_items.discount_value").replace("[", "").replace("]", "").replace(".0", ""));

		Assert.assertEquals(actualDiscountValue, expDiscountValue);

		Assert.assertEquals(actualDiscountBasketItemIDFromBasket, expDiscountBasketID);
		expDiscountBasketID = expDiscountBasketID.replace("[", "").replace("]", "");
		Response deleteBasketResponse = pageObj.endpoints().deleteDiscountBasketForUserAUTH(token,
				dataSet.get("client"), dataSet.get("secret"), expDiscountBasketID);

		String delete_discount_basket_items = deleteBasketResponse.jsonPath().get("deleteBasketResponse");

		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, deleteBasketResponse.getStatusCode());
		Assert.assertEquals(delete_discount_basket_items, null);

		utils.logPass("Verified that basket id " + expDiscountBasketID + " is deleted successfully");

	}

	@Test(priority = 6, groups = { "regression", "dailyrun" })
	@Owner(name ="Shashank Sharma")
	public void T29_addFuelRewardToDiscountBasketAUTH() {

		String fuelAmount = Utilities.getRandomNoFromRange(2, 8) + "";
		// create user
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();

		pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "", "", "10", "");

		// Adding subscription into discount basket
		Response discountBasketResponse = pageObj.endpoints().addDiscountAmountToDiscountBasketAUTH(token,
				dataSet.get("client"), dataSet.get("secret"), "fuel_reward", fuelAmount);
		Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String actual_discount_type_addBasketResponse = discountBasketResponse.jsonPath()
				.getString("discount_basket_items.discount_type");

		Assert.assertEquals(actual_discount_type_addBasketResponse, "[fuel_reward]");

		String actual_discount_discount_id = discountBasketResponse.jsonPath()
				.getString("discount_basket_items.discount_id").replace("[", "").replace("]", "").replace("]", "")
				.replace("]", "");
		Assert.assertEquals(actual_discount_discount_id, "null");

		String discount_details = discountBasketResponse.jsonPath().getString("discount_basket_items.discount_details")
				.replace("[", "").replace("]", "").replace("]", "").replace("]", "");
		Assert.assertEquals(discount_details, "null");

		String expDiscountBasketItemId = discountBasketResponse.jsonPath()
				.getString("discount_basket_items.discount_basket_item_id");

		Response basketDiscountDetailsResponse = pageObj.endpoints().getUserDiscountBasketDetailsUsingAUTH(token,
				dataSet.get("client"), dataSet.get("secret"));

		String actualDiscountDetailsResponse = basketDiscountDetailsResponse.jsonPath()
				.getString("discount_basket_items.discount_basket_item_id");

		Assert.assertEquals(actualDiscountDetailsResponse, expDiscountBasketItemId);

		expDiscountBasketItemId = expDiscountBasketItemId.replace("[", "").replace("]", "");
		Response deleteBasketResponse = pageObj.endpoints().deleteDiscountBasketForUserAUTH(token,
				dataSet.get("client"), dataSet.get("secret"), expDiscountBasketItemId);

		String delete_discount_basket_items = deleteBasketResponse.jsonPath().get("deleteBasketResponse");

		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, deleteBasketResponse.getStatusCode());
		Assert.assertEquals(delete_discount_basket_items, null);

		utils.logPass("Verified that basket id " + expDiscountBasketItemId + " is deleted successfully");

	}

	@Test(description = "OMM-T29 Verify API parameter validations for card completion added into discountsm to basket ", priority = 7, groups = {
			"sanity", "dailyrun" })
	@Owner(name ="Shashank Sharma")
	public void T29_addCardCompletionToUserDiscountBasketAUTH() {

		String discountID = Utilities.getRandomNoFromRange(1, 4) + "";
		// create user
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();

		Response sendAmountResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "", "", "",
				"10");

		// Adding subscription into discount basket
		Response discountBasketResponse = pageObj.endpoints().addDiscountAmountToDiscountBasketAUTH(token,
				dataSet.get("client"), dataSet.get("secret"), "card_completion", discountID);

		Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		String actual_discount_type_addBasketResponse = discountBasketResponse.jsonPath()
				.getString("discount_basket_items.discount_type");

		Assert.assertEquals(actual_discount_type_addBasketResponse, "[card_completion]");

		String actual_discount_discount_id = discountBasketResponse.jsonPath()
				.getString("discount_basket_items.discount_id").replace("[", "").replace("]", "").replace("]", "")
				.replace("]", "");
		Assert.assertEquals(actual_discount_discount_id, "null");

		String discount_details = discountBasketResponse.jsonPath().getString("discount_basket_items.discount_details")
				.replace("[", "").replace("]", "").replace("]", "").replace("]", "");
		Assert.assertEquals(discount_details, "null");

		String dsiscountValue = discountBasketResponse.jsonPath().getString("discount_basket_items.discount_value")
				.replace("[", "").replace("]", "").replace("]", "").replace("]", "");
		Assert.assertEquals(dsiscountValue, "null");

		String expDiscountBasketItemId = discountBasketResponse.jsonPath()
				.getString("discount_basket_items.discount_basket_item_id");

		Response basketDiscountDetailsResponse = pageObj.endpoints().getDiscountBasketDetailsOfUsersAPIMobile(token,
				dataSet.get("client"), dataSet.get("secret"));

		String actualDiscountDetailsResponse = basketDiscountDetailsResponse.jsonPath()
				.getString("discount_basket_items.discount_basket_item_id");

		Assert.assertEquals(actualDiscountDetailsResponse, expDiscountBasketItemId);

		expDiscountBasketItemId = expDiscountBasketItemId.replace("[", "").replace("]", "");
		Response deleteBasketResponse = pageObj.endpoints().deleteDiscountBasketForUserAUTH(token,
				dataSet.get("client"), dataSet.get("secret"), expDiscountBasketItemId);
		String delete_discount_basket_items = deleteBasketResponse.jsonPath().get("deleteBasketResponse");

		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, deleteBasketResponse.getStatusCode());
		Assert.assertEquals(delete_discount_basket_items, null);

		utils.logPass("Verified that basket id " + expDiscountBasketItemId + " is deleted successfully");

	}

	@Test(description = "OMM-T1314"
			+ "Auth>Verify that if subscription plan with processing function-Receipt Level discount purchased by user and it does not qualify the input receipt then it does not get added in discount_basket"
			+ "OMM-T1315 Auth>Verify that if subscription plan with processing function-Receipt Level discount purchased by user and it qualifies the input receipt then it gets added in discount_basket", priority = 8, groups = {
					"regression", "dailyrun" })
	@Owner(name ="Shashank Sharma")
	public void T1314_VerifyAutoSelectSubscriptionNotAddedIfSubscriptionNotQualifiedAndViceVewrsa() {

		String subscriptionName = dataSet.get("subscriptionName");
		String subscriptionID = dataSet.get("subscriptionID");
		String spPrice = dataSet.get("spPrice");
		String externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));

		// create user
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		// subscription purchase api 2
		Response purchaseSubscriptionresponse = pageObj.endpoints().Api2SubscriptionPurchase(token, subscriptionID,
				dataSet.get("client"), dataSet.get("secret"), spPrice, endDateTime);
		Assert.assertEquals(purchaseSubscriptionresponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);

		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();

		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Pizza1", "1", "10", "M", "10", "999", "1", "1");
		parentMap.put("Pizza1", detailsMap1);

		// Auth Auto select API
		Response redemptionResponse = pageObj.endpoints().authAutoSelectAPI(dataSet.get("client"),
				dataSet.get("secret"), token, "14", externalUID, parentMap);

		String redemptionResponseString = redemptionResponse.asString();

		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, redemptionResponse.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		utils.logPass("AUTH add discount to basket is successful");

		List<String> discount_basket_itemsValueList = redemptionResponse.jsonPath()
				.getList("discount_basket_items.name");

		Assert.assertFalse(redemptionResponseString.contains(subscriptionName),
				"discount_basket_items should not be coming ");

		utils.logPass("Verified that subscription name did not  qualified and not added into basket via auto select API");
		parentMap.clear();
		detailsMap1.clear();
		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Pizza1", "1", "10", "M", "10", "999", "1", "101");
		parentMap.put("Pizza1", detailsMap1);

		Response redemptionResponse1 = pageObj.endpoints().authAutoSelectAPI(dataSet.get("client"),
				dataSet.get("secret"), token, "14", externalUID, parentMap);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, redemptionResponse1.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		boolean isAuthAutoSelectSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.authAutoSelectSchema, redemptionResponse1.asString());
		Assert.assertTrue(isAuthAutoSelectSchemaValidated, "Auth Auto Select API schema validation failed");
		utils.logPass("AUTH add discount to basket is successful");

		String discount_basket_itemsValue1 = redemptionResponse1.asString();

		Assert.assertTrue(discount_basket_itemsValue1.contains(subscriptionName), discount_basket_itemsValue1
				+ " actual subscription name is not matched with expected - " + subscriptionName);

		utils.logPass("Verified that subscription name got qualified and added to basket via auto select API");

	}

	// Shashank sharma
	@Test(description = "OMM-T1274 Verify if there are multiple subscription plans, one having end date-May 21, 2023 and other have end date-June 18, 2023 then subscription with earlier end date should get added first in the discount_basket"
			+ "OMM-T1269 (1.0) / SQ-T4277 Verify if user has purchased multiple subscriptions and then only 1 subscription plan getting qualified gets added to discount_basket automatically on hitting Auth Auto Select API"
			+ "OMM-T1267 /SQ-T4276  Auth Auto Select>Verify if \"Enable Auto-redemption\" is enabled and strategy \"Auto redemption discount types-> Subscription\" is selected and user has an Active basket containing discounts then qualified-Subscription gets added to basket", priority = 9, groups = {
					"regression", "dailyrun" })
	@Owner(name ="Shashank Sharma")
	public void OMM_T1274_SubscriptionWithEarlyEndDateShouldAddFirstInAutoSelectAPI() throws InterruptedException {

		String dateTime1 = CreateDateTime.getFutureDate(5) + " 10:00 PM";
		String dateTime2 = CreateDateTime.getFutureDate(2) + " 10:00 PM";

		// create subscription plan
		String spName1 = dataSet.get("subscriptionPlanName1");
		String QCname = "QcSubscription_" + CreateDateTime.getTimeDateString();
		String amountcap = Integer.toString(Utilities.getRandomNoFromRange(10, 200));
		String unitDiscount = Integer.toString(Utilities.getRandomNoFromRange(1, 10));
		String spPrice = Integer.toString(Utilities.getRandomNoFromRange(300, 500));

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboard("Enable Subscriptions?", "check");
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.cockpitRedemptionsPage().setAutoRedemptionDiscounts("Subscription", "Select");
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification
		// Criteria");
		// pageObj.qualificationcriteriapage().createQualificationCriteria(QCname,
		// amountcap,
		// dataSet.get("qcFucntionName"), unitDiscount, true,
		// dataSet.get("lineItemSelectorName"));
		// pageObj.menupage().clickSubscriptionsMenuIcon();
		// pageObj.menupage().clickSubscriptionPlansLink();
		// pageObj.subscriptionPlansPage().createSubscriptionPlan(spName1, spPrice,
		// QCname, dataSet.get("qcFucntionName"),
		// GlobalBenefitRedemptionThrottlingToggle, dateTime1);
		// String PlanID1 =
		// pageObj.subscriptionPlansPage().getSbuscriptionPlanID(spName1);
		String PlanID1 = dataSet.get("PlanID1");

		String spName2 = dataSet.get("subscriptionPlanName2");

		// pageObj.subscriptionPlansPage().createSubscriptionPlan(spName2, spPrice,
		// QCname, dataSet.get("qcFucntionName"),
		// GlobalBenefitRedemptionThrottlingToggle, dateTime2);
		// String PlanID2 =
		// pageObj.subscriptionPlansPage().getSbuscriptionPlanID(spName2);
		String PlanID2 = dataSet.get("PlanID2");

		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		// subscription purchase api 2
		Response purchaseSubscriptionresponse = pageObj.endpoints().Api2SubscriptionPurchase(token, PlanID1,
				dataSet.get("client"), dataSet.get("secret"), spPrice, dateTime1);

		Assert.assertEquals(purchaseSubscriptionresponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		int subscription_id1 = Integer
				.parseInt(purchaseSubscriptionresponse.jsonPath().get("subscription_id").toString());
		logger.info(userEmail + " purchased " + subscription_id1 + " Plan id = " + PlanID1);

		// subscription purchase api 2
		Response purchaseSubscriptionresponse2 = pageObj.endpoints().Api2SubscriptionPurchase(token, PlanID2,
				dataSet.get("client"), dataSet.get("secret"), spPrice, dateTime2);

		Assert.assertEquals(purchaseSubscriptionresponse2.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);

		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();

		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Pizza1", "1", "10", "M", "10", "999", "1", "101");
		parentMap.put("Pizza1", detailsMap1);

		// Auth Auto select API
		Response autoSelectResponse = pageObj.endpoints().authAutoSelectAPI(dataSet.get("client"),
				dataSet.get("secret"), token, "14", "", parentMap);
		Assert.assertEquals(autoSelectResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		String actualQualifiedSubscriptionID = autoSelectResponse.jsonPath()
				.getString("discount_basket_items[0].discount_details.item_id").replace("[", "").replace("]", "");
		String actualQualifiedSubscriptionName = autoSelectResponse.jsonPath()
				.getString("discount_basket_items[0].discount_details.name").replace("[", "").replace("]", "");

		Assert.assertEquals(actualQualifiedSubscriptionID, PlanID2);
		Assert.assertEquals(actualQualifiedSubscriptionName, spName2);

		utils.logPass(actualQualifiedSubscriptionName
				+ " subscription which have early expired date got qualified and added to basket via auto select API");

	}

	// Shashank
	@Test(description = "SQ-T4295 Verify use case 1 on IP-3775", priority = 10, groups = { "regression", "dailyrun" })
	@Owner(name ="Shashank Sharma")
	public void T4295_VerifyUseCase1_OnIP3775() throws InterruptedException {
		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logit("API2 Signup is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "16",
				dataSet.get("redeemable_id"), "", "");

		utils.logit("Send redeemable to the user successfully");

		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		utils.logPass("Api2  send reward amount to user is successful");

		// get reward id
		String rewardID1 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id"));

		utils.logit("Reward id " + rewardID1 + " is generated successfully ");

		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();

		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap(dataSet.get("expectedReceiptItemName"), "5", "35", "M",
				"2001", "800", "152", "2001");
		parentMap.put(dataSet.get("expectedReceiptItemName"), detailsMap1);

		Response resp = pageObj.endpoints().posRedemptionOfRewardWithoutUUID(userEmail, "10",
				dataSet.get("locationkey"), parentMap, rewardID1);

		String actualItemName = resp.jsonPath().getString("qualified_menu_items.item_name").replace("[", "")
				.replace("]", "");
		Assert.assertEquals(actualItemName, dataSet.get("expectedReceiptItemName"));
		utils.logPass("Verified that Papadia1 item name in POSRedemption response ");

		double actualRedemptionAmount = Double
				.parseDouble(resp.jsonPath().getString("redemption_amount").replace("[", "").replace("]", ""));
		Assert.assertEquals(actualRedemptionAmount, 7.0);
		utils.logPass("Verified that 7.0 amount is redemption ");

	}

	// Shashank
	@Test(description = "SQ-T4296 Verify use case 2 on IP-3775", priority = 10, groups = { "regression", "dailyrun" })
	@Owner(name ="Shashank Sharma")
	public void T4296_VerifyUseCase2_OnIP3775() throws InterruptedException {
		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logit("API2 Signup is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "16",
				dataSet.get("redeemable_id"), "", "");

		utils.logit("Send redeemable to the user successfully");

		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		utils.logPass("Api2  send reward amount to user is successful");

		// get reward id
		String rewardID1 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id"));

		utils.logit("Reward id " + rewardID1 + " is generated successfully ");

		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();

		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Papadia1", "5", "35", "M", "1001", "800", "152",
				"1001");
		parentMap.put("Papadia1", detailsMap1);
		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Papadia2", "5", "35", "M", "2001", "800", "152",
				"2001");
		parentMap.put("Papadia2", detailsMap1);

		Response resp = pageObj.endpoints().posRedemptionOfRewardWithoutUUID(userEmail, "10",
				dataSet.get("locationkey"), parentMap, rewardID1);

		String actualItemName = resp.jsonPath().getString("qualified_menu_items.item_name").replace("[", "")
				.replace("]", "");
		Assert.assertEquals(actualItemName, dataSet.get("expectedReceiptItemName"));
		utils.logPass("Verified that Papadia1 item name in POSRedemption response ");

		double actualRedemptionAmount = Double
				.parseDouble(resp.jsonPath().getString("redemption_amount").replace("[", "").replace("]", ""));
		Assert.assertEquals(actualRedemptionAmount, 7.0);
		utils.logPass("Verified that 7.0 amount is redemption ");

	}

	@Test(description = "OMM-T2115 Verify Enable Auto Redemption\" flag is no visible in UI when allow_multiple_redemptions -> true and Enable Auto-redemption -> On and Auto redemption discounts -> Blank on create update redeemable", priority = 10, groups = {
			"regression", "dailyrun" })
	@Owner(name ="Shashank Sharma")
	public void T2115_VerifyEnableAutoRedemptionFlagOnOffFunctionality() throws InterruptedException {

		String redeemableName = "AutoRedeemable_" + CreateDateTime.getTimeDateString();
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboard("Enable Auto-redemption", "uncheck");

		pageObj.menupage().navigateToSubMenuItem("Settings", "Redeemables");

		boolean result = pageObj.redeemablePage()
				.verifyEnableAutoRedemptionVisisbleWhileCreatingRedeemable(redeemableName, "Enable Auto Redemption");
		Assert.assertFalse(result, "Enable Auto Redemption toggle button should not be visible ");

		utils.logPass(
				"Verified that Enable Auto Redemption toggle button is not visible when flag is OFF while creating new Redeemable ");

		pageObj.menupage().navigateToSubMenuItem("Settings", "Redeemables");
		boolean result2 = pageObj.redeemablePage().verifyEnableAutoRedemptionToggleDisplayed(redeemableName,
				"Enable Auto Redemption");

		Assert.assertFalse(result2, "Enable Auto Redemption toggle button should not be visible ");
		utils.logPass(
				"Verified that Enable Auto Redemption toggle button is not visible when flag is OFF while Editing Redeemable ");

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboard("Enable Auto-redemption", "check");
		// pageObj.dashboardpage().updateCheckBox();
		pageObj.menupage().navigateToSubMenuItem("Settings", "Redeemables");
		utils.longWaitInSeconds(5);
		String redeemableName2 = "AutoRedeemable_" + CreateDateTime.getTimeDateString();
		boolean result3 = pageObj.redeemablePage()
				.verifyEnableAutoRedemptionVisisbleWhileCreatingRedeemable(redeemableName2, "Enable Auto Redemption");
		Assert.assertTrue(result3, "Enable Auto Redemption toggle button should be visible ");
		utils.logPass(
				"Verified that Enable Auto Redemption toggle button is visible when flag is ON while creating new Redeemable ");

		pageObj.menupage().navigateToSubMenuItem("Settings", "Redeemables");
		boolean result4 = pageObj.redeemablePage().verifyEnableAutoRedemptionToggleDisplayed(redeemableName2,
				"Enable Auto Redemption");
		Assert.assertTrue(result4, "Enable Auto Redemption toggle button should be visible ");
		utils.logPass(
				"Verified that Enable Auto Redemption toggle button is visible when flag is ON while Editing Redeemable ");

	}

	@Test(description = "OMM-T2117 (1.0)\n"
			+ "Verify \"Enable Auto Redemption\" flag is not visible in UI when allow_multiple_redemptions -> true and Enable Auto-redemption -> On and Auto redemption discounts -> Only Subscriptions selected on create / update redeemable", priority = 10, groups = {
					"regression", "unstable", "dailyrun" })
	@Owner(name ="Shashank Sharma")
	public void T2117_VerifyEnableAutoRedemptionFlagOnOffFunctionalityForSubscription() throws InterruptedException {

		String redeemableName = "AutoRedeemable_" + CreateDateTime.getTimeDateString();
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.dashboardpage().checkUncheckFlagCockpitDashboard("Enable Auto-redemption", "check");
		pageObj.cockpitRedemptionsPage().setAutoRedemptionDiscounts("Subscription", "Select");
		pageObj.cockpitRedemptionsPage().setAutoRedemptionDiscounts("Offers", "Unselect");
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();
		pageObj.menupage().navigateToSubMenuItem("Settings", "Redeemables");

		boolean result = pageObj.redeemablePage()
				.verifyEnableAutoRedemptionVisisbleWhileCreatingRedeemable(redeemableName, "Enable Auto Redemption");
		Assert.assertFalse(result, "Enable Auto Redemption toggle button should not be visible ");

		utils.logPass(
				"Verified that Enable Auto Redemption toggle button is not visible when flag is OFF while creating new Redeemable ");

		pageObj.menupage().navigateToSubMenuItem("Settings", "Redeemables");
		boolean result2 = pageObj.redeemablePage().verifyEnableAutoRedemptionToggleDisplayed(redeemableName,
				"Enable Auto Redemption");

		Assert.assertFalse(result2, "Enable Auto Redemption toggle button should not be visible ");
		utils.logPass(
				"Verified that Enable Auto Redemption toggle button is not visible when flag is OFF while Editing Redeemable ");

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.dashboardpage().checkUncheckFlagCockpitDashboard("Enable Auto-redemption", "check");
		pageObj.cockpitRedemptionsPage().setAutoRedemptionDiscounts("Offers", "Select");
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// pageObj.dashboardpage().updateCheckBox();
		pageObj.menupage().navigateToSubMenuItem("Settings", "Redeemables");

		String redeemableName2 = "AutoRedeemable_" + CreateDateTime.getTimeDateString();
		boolean result3 = pageObj.redeemablePage()
				.verifyEnableAutoRedemptionVisisbleWhileCreatingRedeemable(redeemableName2, "Enable Auto Redemption");
		Assert.assertTrue(result3, "Enable Auto Redemption toggle button should be visible ");
		utils.logPass(
				"Verified that Enable Auto Redemption toggle button is visible when flag is ON while creating new Redeemable ");

		pageObj.menupage().navigateToSubMenuItem("Settings", "Redeemables");
		boolean result4 = pageObj.redeemablePage().verifyEnableAutoRedemptionToggleDisplayed(redeemableName2,
				"Enable Auto Redemption");
		Assert.assertTrue(result4, "Enable Auto Redemption toggle button should be visible ");
		utils.logPass(
				"Verified that Enable Auto Redemption toggle button is visible when flag is ON while Editing Redeemable ");

	}

	// @Test(priority = -1)
	public void ommPrerequisite() throws InterruptedException {
		List<String> slugList = new ArrayList<String>();
		slugList.add("moes");
		slugList.add("caseys");
		slugList.add("pizzafactory");
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		for (String str : slugList) {
			pageObj.instanceDashboardPage().selectBusiness(str);
			Thread.sleep(5000);
			pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
			pageObj.dashboardpage().checkUncheckFlagCockpitDashboard("Enable Promos?", "check");
			pageObj.dashboardpage().checkUncheckFlagCockpitDashboard("Enable Coupons?", "check");
			pageObj.dashboardpage().clickOnUpdateButton();
			pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
			pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
			pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboard("Enable Reward Locking", "uncheck");
			pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboard("Enable Auto-redemption", "check");
			pageObj.cockpitRedemptionsPage().clickOnUpdateButton();
			pageObj.dashboardpage().navigateToAllBusinessPage();
			utils.logPass("Flags are updated for the business: " + str);
		}

	}

	@Test(description = "SQ-T5680 Auth>Remove Discount in Discount Basket>Verify max_applicable_quantity is returned for processing function-Target Price for Bundle,Rate rollback and Target Price for Bundle advanced", priority = 11, groups = {
			"regression", "dailyrun" })
	@Owner(name ="Hardik Bhardwaj")
	public void T5680_addDiscountAuthMaxAppQty() throws InterruptedException {
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_allow_multiple_redemptions", "check");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_allow_multiple_redemptions_on_all_locations",
				"check");
		pageObj.dashboardpage().updateCheckBox();

		// Navigate to Multiple Redemption Tab and check the flags
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_auto_redemption", "check");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_discount_locking", "check");
		pageObj.cockpitRedemptionsPage().updateTotalNumberOfRedemptionsAllowed("10");
		pageObj.cockpitRedemptionsPage().setAutoRedemptionDiscounts("Offers", "Select");
		pageObj.cockpitRedemptionsPage().setAcquisitionType("Offers", "6");
		pageObj.dashboardpage().updateCheckBox();

		// create subscription plan
		String externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		String redeemableID1 = dataSet.get("redeemableID1");
		String redeemableID2 = dataSet.get("redeemableID2");
		String redeemableID3 = dataSet.get("redeemableID3");
		String redeemableID4 = dataSet.get("redeemableID4");

		// create user
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				redeemableID1, "", "");
		utils.logit("Send redeemable to the user successfully");
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send message to user");

		// get reward id
		String rewardId1 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemableID1);
		utils.logit("Reward id " + rewardId1 + " is generated successfully ");

		// send reward amount to user Reedemable
		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				redeemableID2, "", "");
		utils.logit("Send redeemable to the user successfully");
		Assert.assertEquals(sendRewardResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send message to user");

		// get reward id
		String rewardId2 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemableID2);
		utils.logit("Reward id " + rewardId2 + " is generated successfully ");

		// send reward amount to user Reedemable
		Response sendRewardResponse3 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				redeemableID3, "", "");
		utils.logit("Send redeemable to the user successfully");
		Assert.assertEquals(sendRewardResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send message to user");

		// get reward id
		String rewardId3 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemableID3);
		utils.logit("Reward id " + rewardId3 + " is generated successfully ");

		// send reward amount to user Reedemable
		Response sendRewardResponse4 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				redeemableID4, "", "");
		utils.logit("Send redeemable to the user successfully");
		Assert.assertEquals(sendRewardResponse4.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send message to user");

		// get reward id
		String rewardId4 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemableID4);
		utils.logit("Reward id " + rewardId4 + " is generated successfully ");

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_auto_redemption", "check");
		pageObj.cockpitRedemptionsPage().updateTotalNumberOfRedemptionsAllowed("10");
		pageObj.dashboardpage().updateCheckBox();

		// add reward completion to basket
		Response discountBasketResponse = pageObj.endpoints().addDiscountToBasketAUTH(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId1, externalUID);
		Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for Auth add reward completion to basket");
		String maxAppQtyVal1 = discountBasketResponse.jsonPath()
				.getString("discount_basket_items[0].discount_details.max_applicable_quantity");

		// add reward completion to basket
		Response discountBasketResponse2 = pageObj.endpoints().addDiscountToBasketAUTH(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId2, externalUID);
		Assert.assertEquals(discountBasketResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for Auth add reward completion to basket");
		String maxAppQtyVal2 = discountBasketResponse2.jsonPath()
				.getString("discount_basket_items[0].discount_details.max_applicable_quantity");

		// add reward completion to basket
		Response discountBasketResponse3 = pageObj.endpoints().addDiscountToBasketAUTH(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId3, externalUID);
		Assert.assertEquals(discountBasketResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for Auth add reward completion to basket");
		String maxAppQtyVal3 = discountBasketResponse3.jsonPath()
				.getString("discount_basket_items[0].discount_details.max_applicable_quantity");

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_auto_redemption", "check");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_discount_locking", "check");
		pageObj.cockpitRedemptionsPage().setAcquisitionType("Offers", "10");
		pageObj.dashboardpage().updateCheckBox();

		// add reward completion to basket
		Response discountBasketResponse4 = pageObj.endpoints().addDiscountToBasketAUTH(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId4, externalUID);
		Assert.assertEquals(discountBasketResponse4.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for Auth add reward completion to basket");
		String discount_basket_item_id = discountBasketResponse4.jsonPath()
				.get("discount_basket_items[0].discount_basket_item_id").toString();

		Assert.assertEquals(maxAppQtyVal1, dataSet.get("maxAplcQty1"),
				"max_applicable_quantity is NULL for add reward to basket with QC as Target Price For a Bundle Advance");

		Assert.assertEquals(maxAppQtyVal2, dataSet.get("maxAplcQty2"),
				"max_applicable_quantity is NULL for add reward to basket with QC as Target Price For a Bundle");

		Assert.assertEquals(maxAppQtyVal3, dataSet.get("maxAplcQty3"),
				"max_applicable_quantity is NULL for add reward to basket with QC as Rate Roll Back");

		utils.logPass(
				"In Auth add discount to basket, The max_applicable_quantity field should be present and contain a valid value for QC as Target Price For a Bundle Advance, Target Price For a Bundle and Rate Roll Back");

		// Auth remove discount from basket
		Response deleteBasketResponse = pageObj.endpoints().deleteDiscountBasketForUserWithExt_UidAUTH(token,
				dataSet.get("client"), dataSet.get("secret"), discount_basket_item_id, externalUID);
		Assert.assertEquals(deleteBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with remove discount from basket ");
		utils.logit("Auth remove discount basket is successful");

		String maxAppQtyVal4 = deleteBasketResponse.jsonPath()
				.getString("discount_basket_items[0].discount_details.max_applicable_quantity");
		String maxAppQtyVal5 = deleteBasketResponse.jsonPath()
				.getString("discount_basket_items[0].discount_details.max_applicable_quantity");
		String maxAppQtyVal6 = deleteBasketResponse.jsonPath()
				.getString("discount_basket_items[0].discount_details.max_applicable_quantity");

		Assert.assertEquals(maxAppQtyVal4, dataSet.get("maxAplcQty1"),
				"max_applicable_quantity is NULL for remove reward to basket with QC as Target Price For a Bundle Advance");

		Assert.assertEquals(maxAppQtyVal5, dataSet.get("maxAplcQty2"),
				"max_applicable_quantity is NULL for remove reward to basket with QC as Target Price For a Bundle");

		Assert.assertEquals(maxAppQtyVal6, dataSet.get("maxAplcQty3"),
				"max_applicable_quantity is NULL for remove reward to basket with QC as Rate Roll Back");

		utils.logPass(
				"In Auth remove discount to basket, The max_applicable_quantity field should be present and contain a valid value for QC as Target Price For a Bundle Advance, Target Price For a Bundle and Rate Roll Back");
	}

	@AfterMethod(alwaysRun = true)
	public void teraDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}
}
