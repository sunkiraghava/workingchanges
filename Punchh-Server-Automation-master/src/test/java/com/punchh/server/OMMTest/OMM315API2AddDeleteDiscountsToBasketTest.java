package com.punchh.server.OMMTest;

import java.lang.reflect.Method;
import java.text.ParseException;
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
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.annotations.Owner;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.apiConfig.ApiResponseJsonSchema;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

/*
 * @Author : Shashank Sharma 
 */
@Listeners(TestListeners.class)
public class OMM315API2AddDeleteDiscountsToBasketTest {
	static Logger logger = LogManager.getLogger(OMM315API2AddDeleteDiscountsToBasketTest.class);
	public WebDriver driver;
	private String userEmail;
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
		// env = prop.getProperty("environment");
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
		GlobalBenefitRedemptionThrottlingToggle = false;
		endDateTime = CreateDateTime.getFutureDate(1) + " 10:00 PM";
		utils = new Utilities(driver);
	}

	@Test(description = "OMM-T29 Verify API parameter validations for Add Discounts To Basket API (mobile)", groups = { "regression", "dailyrun" }, priority = 1)
	@Owner(name = "Shashank Sharma")
	public void AddRewardDiscountToBasketAPI2() throws InterruptedException {

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("API2 Signup is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				dataSet.get("redeemable_id"), "", "");

		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		utils.logPass("Api2  send reward amount to user is successful");

		Response rewardResponse = pageObj.endpoints().authListAvailableRewardsNew(token, dataSet.get("client"),
				dataSet.get("secret"));

		String rewardID = rewardResponse.jsonPath().getString("id").replace("[", "").replace("]", "");

		utils.logPass("Reward id " + rewardID + " is generated successfully ");

		Response discountBasketResponse = pageObj.endpoints().addDiscountToBasketAPI2(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID);

		String actualDiscountType_InBasketAddResponse = discountBasketResponse.jsonPath()
				.getString("discount_basket_items.discount_type");
		String actualDiscountID_InBasketAddResponse = discountBasketResponse.jsonPath()
				.getString("discount_basket_items.discount_id").replace("[", "").replace("]", "");

		Assert.assertEquals(actualDiscountType_InBasketAddResponse, "[reward]");
		Assert.assertEquals(actualDiscountID_InBasketAddResponse, rewardID);

		utils.logPass("Verified the discount type and ID in Basket add response");

		String expdiscount_basket_item_id = discountBasketResponse.jsonPath()
				.getString("discount_basket_items.discount_basket_item_id").replace("[", "").replace("]", "");

		Response basketDiscountDetailsResponse = pageObj.endpoints().getUserDiscountBasketDetailsUsingAPI2(token,
				dataSet.get("client"), dataSet.get("secret"));

		String actualDiscountBasketItemIDFromBasket = basketDiscountDetailsResponse.jsonPath()
				.getString("discount_basket_items.discount_basket_item_id").replace("[", "").replace("]", "");
		String actualDiscountIDFromBasket = basketDiscountDetailsResponse.jsonPath()
				.getString("discount_basket_items.discount_id").replace("[", "").replace("]", "");

		Assert.assertEquals(actualDiscountIDFromBasket, rewardID);
		Assert.assertEquals(expdiscount_basket_item_id, actualDiscountBasketItemIDFromBasket);

		expdiscount_basket_item_id = expdiscount_basket_item_id.replace("[", "").replace("]", "");
		Response deleteBasketResponse = pageObj.endpoints().deleteDiscountToBasketAPI2(token, dataSet.get("client"),
				dataSet.get("secret"), expdiscount_basket_item_id);

		String delete_discount_basket_items = deleteBasketResponse.jsonPath().get("deleteBasketResponse");

		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, deleteBasketResponse.getStatusCode());
		Assert.assertEquals(delete_discount_basket_items, null);

		utils.logPass("Verified that basket is deleted successfully");

	}

	@Test(description = "OMM-T29 Verify API parameter validations for Coupon added into discounts to basket ", priority = 2,groups = {
			"sanity", "unstable", "dailyrun" })
	@Owner(name = "Shashank Sharma")
	public void AddRedemptionCuponCodeIntoUserDiscountBasketAPI2() throws InterruptedException, ParseException {

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
		Response discountBasketResponse = pageObj.endpoints().addDiscountToBasketAPI2(token, dataSet.get("client"),
				dataSet.get("secret"), "redemption_code", generatedCodeName);

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

		Response basketDiscountDetailsResponse = pageObj.endpoints().getUserDiscountBasketDetailsUsingAPI2(token,
				dataSet.get("client"), dataSet.get("secret"));

		String actualDiscountBasketItemIDFromBasket = basketDiscountDetailsResponse.jsonPath()
				.getString("discount_basket_items.discount_basket_item_id").replace("[", "").replace("]", "");
		String actualPromoCodeFromBasket = basketDiscountDetailsResponse.jsonPath()
				.getString("discount_basket_items.discount_id").replace("[", "").replace("]", "");

		Assert.assertEquals(actualDiscountBasketItemIDFromBasket, expdiscount_basket_item_id);
		Assert.assertEquals(actualPromoCodeFromBasket, generatedCodeName);

		Response deleteBasketResponse = pageObj.endpoints().deleteDiscountToBasketAPI2(token, dataSet.get("client"),
				dataSet.get("secret"), expdiscount_basket_item_id);
		String delete_discount_basket_items = deleteBasketResponse.jsonPath().get("deleteBasketResponse");

		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, deleteBasketResponse.getStatusCode());
		Assert.assertEquals(delete_discount_basket_items, null);

		utils.logPass("Verified that basket id " + expdiscount_basket_item_id + " is deleted successfully");

	}

	@Test(description = "OMM-T29 Verify API parameter validations for Promo Coupon added into discounts to basket ",priority = 3, groups = { "sanity", "dailyrun" })
	@Owner(name = "Shashank Sharma")
	public void AddingPromoCodeCuponToUserDiscountBasketAPI2() throws InterruptedException, ParseException {

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

		Response discountBasketResponse = pageObj.endpoints().addDiscountToBasketAPI2(token, dataSet.get("client"),
				dataSet.get("secret"), "redemption_code", promoCode);

		Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean isApi2AddDiscountBasketSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2AddDiscountBasketItemSchema, discountBasketResponse.asString());
		Assert.assertTrue(isApi2AddDiscountBasketSchemaValidated,
				"API v2 Add Selection to Discount Basket Schema Validation failed");
		String actual_discount_discount_id = discountBasketResponse.jsonPath()
				.getString("discount_basket_items.discount_id").replace("[", "").replace("]", "");
		actual_discount_discount_id = actual_discount_discount_id.replace("[", "").replace("]", "");
		Assert.assertEquals(actual_discount_discount_id, promoCode);

		utils.logPass("Verified that " + actual_discount_discount_id + " in add basket response");

		String actual_discount_type_addBasketResponse = discountBasketResponse.jsonPath()
				.getString("discount_basket_items.discount_type");

		Assert.assertEquals(actual_discount_type_addBasketResponse, "[redemption_code]");

		utils.logit("Verified the redemption_code in add basket response");

		String expdiscount_basket_item_id = discountBasketResponse.jsonPath()
				.getString("discount_basket_items.discount_basket_item_id").replace("[", "").replace("]", "");

		Response basketDiscountDetailsResponse = pageObj.endpoints().getUserDiscountBasketDetailsUsingAPI2(token,
				dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(basketDiscountDetailsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean isApi2GetActiveDiscountBasketSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2AddDiscountBasketItemSchema, basketDiscountDetailsResponse.asString());
		Assert.assertTrue(isApi2GetActiveDiscountBasketSchemaValidated,
				"API v2 Get Active Discount Basket Schema Validation failed");
		String actualDiscountBasketItemIDFromBasket = basketDiscountDetailsResponse.jsonPath()
				.getString("discount_basket_items.discount_basket_item_id").replace("[", "").replace("]", "");
		String actualPromoCodeFromBasket = basketDiscountDetailsResponse.jsonPath()
				.getString("discount_basket_items.discount_id").replace("[", "").replace("]", "");

		Assert.assertEquals(actualDiscountBasketItemIDFromBasket, expdiscount_basket_item_id);
		Assert.assertEquals(actualPromoCodeFromBasket, promoCode);

		Response deleteBasketResponse = pageObj.endpoints().deleteDiscountToBasketAPI2(token, dataSet.get("client"),
				dataSet.get("secret"), expdiscount_basket_item_id);
		String delete_discount_basket_items = deleteBasketResponse.jsonPath().get("deleteBasketResponse");

		Assert.assertEquals(deleteBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean isApi2RemoveDiscountBasketItemSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2RemoveDiscountBasketItemSchema, deleteBasketResponse.asString());
		Assert.assertTrue(isApi2RemoveDiscountBasketItemSchemaValidated,
				"API v2 Remove Item From Discount Basket Schema Validation failed");
		Assert.assertEquals(delete_discount_basket_items, null);

		utils.logPass("Verified that basket id " + expdiscount_basket_item_id + " is deleted successfully");

	}

	@Test(description = "OMM-T29 Verify API parameter validations for Subscription added into discounts to basket ",priority = 4, groups = { "sanity", "dailyrun" })
	@Owner(name = "Shashank Sharma")
	public void subscriptionToAddInDiscountBucketAPI2() throws InterruptedException {
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
		Response discountBasketResponse = pageObj.endpoints().addDiscountToBasketAPI2(token, dataSet.get("client"),
				dataSet.get("secret"), "subscription", subscription_id + "");
		Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		int userSubscriptionID = Integer.parseInt(discountBasketResponse.jsonPath()
				.getString("discount_basket_items.discount_id").replace("[", "").replace("]", ""));

		Assert.assertEquals(userSubscriptionID, subscription_id, "User got the wrong subscription plan ");
		int actual_discount_discount_id = Integer.parseInt(discountBasketResponse.jsonPath()
				.getString("discount_basket_items.discount_id").replace("[", "").replace("]", ""));
		Assert.assertEquals(actual_discount_discount_id, subscription_id);

		utils.logPass("Verified that " + actual_discount_discount_id + " in add basket response");

		String actual_discount_type_addBasketResponse = discountBasketResponse.jsonPath()
				.getString("discount_basket_items.discount_type");

		Assert.assertEquals(actual_discount_type_addBasketResponse, "[subscription]");

		utils.logit("Verified the redemption_code in add basket response");

		String expDiscountBasketID = discountBasketResponse.jsonPath()
				.getString("discount_basket_items.discount_basket_item_id").replace("[", "").replace("]", "");

		Response basketDiscountDetailsResponse = pageObj.endpoints().getUserDiscountBasketDetailsUsingAPI2(token,
				dataSet.get("client"), dataSet.get("secret"));

		String actualDiscountBasketItemIDFromBasket = basketDiscountDetailsResponse.jsonPath()
				.getString("discount_basket_items.discount_basket_item_id").replace("[", "").replace("]", "");
		String actualDiscpuntIDFromBasket = basketDiscountDetailsResponse.jsonPath()
				.getString("discount_basket_items.discount_id").replace("[", "").replace("]", "");

		String actualSubscriptionNameFromBasket = basketDiscountDetailsResponse.jsonPath()
				.getString("discount_basket_items.discount_details.name").replace("[", "").replace("]", "");

		Assert.assertEquals(actualDiscountBasketItemIDFromBasket, expDiscountBasketID + "");
		Assert.assertEquals(actualDiscpuntIDFromBasket, userSubscriptionID + "");
		Assert.assertEquals(spName, actualSubscriptionNameFromBasket);

		utils.logPass("User add the subscription plan to his discount basket successfully");
		Response deleteBasketResponse = pageObj.endpoints().deleteDiscountToBasketAPI2(token, dataSet.get("client"),
				dataSet.get("secret"), expDiscountBasketID);
		String delete_discount_basket_items = deleteBasketResponse.jsonPath().get("deleteBasketResponse");

		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, deleteBasketResponse.getStatusCode());
		Assert.assertEquals(delete_discount_basket_items, null);

		utils.logPass("Verified that basket id " + expDiscountBasketID + " is deleted successfully");

	}

	@Test(description = "OMM-T29 Verify API parameter validations for discount amount added into discounts to basket ",priority = 5, groups = { "sanity", "dailyrun" })
	@Owner(name = "Shashank Sharma")
	public void addDiscountAmountToDiscountBasketAPI2() {
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
		Response discountBasketResponse = pageObj.endpoints().addDiscountToBasketAPI2(token, dataSet.get("client"),
				dataSet.get("secret"), "discount_amount", discountAmount);
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

		utils.logPass("User add the discount ammount to his discount basket successfully");

		Response basketDiscountDetailsResponse = pageObj.endpoints().getUserDiscountBasketDetailsUsingAPI2(token,
				dataSet.get("client"), dataSet.get("secret"));

		String actualDiscountBasketItemIDFromBasket = basketDiscountDetailsResponse.jsonPath()
				.getString("discount_basket_items.discount_basket_item_id").replace("[", "").replace("]", "");

		int expDiscountValue = Integer.parseInt(dataSet.get("discountAmount"));
		int actualDiscountValue = Integer.parseInt(basketDiscountDetailsResponse.jsonPath()
				.getString("discount_basket_items.discount_value").replace("[", "").replace("]", "").replace(".0", ""));

		Assert.assertEquals(actualDiscountValue, expDiscountValue);

		Assert.assertEquals(actualDiscountBasketItemIDFromBasket, expDiscountBasketID);
		utils.logPass("User add the discount ammount to his discount basket successfully");

		Response deleteBasketResponse = pageObj.endpoints().deleteDiscountToBasketAPI2(token, dataSet.get("client"),
				dataSet.get("secret"), expDiscountBasketID);
		String delete_discount_basket_items = deleteBasketResponse.jsonPath().get("deleteBasketResponse");

		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, deleteBasketResponse.getStatusCode());
		Assert.assertEquals(delete_discount_basket_items, null);

		utils.logPass("Verified that basket id " + expDiscountBasketID + " is deleted successfully");

	}

	@Test(description = "OMM-T29 Verify API parameter validations for Fuel reward added into discounts to basket ",priority = 6, groups = { "sanity", "dailyrun" })
	@Owner(name = "Shashank Sharma")
	public void addFuelRewardToDiscountBasketAPI2() {

		String fuelAmount = Utilities.getRandomNoFromRange(2, 8) + "";
		// create user
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();

		pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "", "", fuelAmount, "");

		// Adding subscription into discount basket
		Response discountBasketResponse = pageObj.endpoints().addDiscountToBasketAPI2(token, dataSet.get("client"),
				dataSet.get("secret"), "fuel_reward", fuelAmount);
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
				.getString("discount_basket_items.discount_basket_item_id").replace("[", "").replace("]", "");

		Response basketDiscountDetailsResponse = pageObj.endpoints().getUserDiscountBasketDetailsUsingAPI2(token,
				dataSet.get("client"), dataSet.get("secret"));

		String actualDiscountDetailsResponse = basketDiscountDetailsResponse.jsonPath()
				.getString("discount_basket_items.discount_basket_item_id").replace("[", "").replace("]", "");

		Assert.assertEquals(actualDiscountDetailsResponse, expDiscountBasketItemId);

		utils.logPass("User add the fuel ammount to his discount basket successfully");

		Response deleteBasketResponse = pageObj.endpoints().deleteDiscountToBasketAPI2(token, dataSet.get("client"),
				dataSet.get("secret"), expDiscountBasketItemId);
		String delete_discount_basket_items = deleteBasketResponse.jsonPath().get("deleteBasketResponse");

		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, deleteBasketResponse.getStatusCode());
		Assert.assertEquals(delete_discount_basket_items, null);

		utils.logPass("Verified that basket id " + expDiscountBasketItemId + " is deleted successfully");

	}

	@Test(description = "OMM-T29 Verify API parameter validations for card completion added into discounts to basket",priority = 7, groups = {
			"regression", "unstable", "dailyrun" })
	@Owner(name = "Shashank Sharma")
	public void addCardCompletionToUserDiscountBasketAPI2() {

		String discountID = Utilities.getRandomNoFromRange(1, 4) + "";
		// create user
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();

		@SuppressWarnings("unused")
		Response sendAmountResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "", "", "",
				"10");

		// Adding subscription into discount basket
		Response discountBasketResponse = pageObj.endpoints().addDiscountToBasketAPI2(token, dataSet.get("client"),
				dataSet.get("secret"), "card_completion", discountID);

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
		Response deleteBasketResponse = pageObj.endpoints().deleteDiscountBasketForUserAPI2(token,
				dataSet.get("client"), dataSet.get("secret"), expDiscountBasketItemId);
		String delete_discount_basket_items = deleteBasketResponse.jsonPath().get("deleteBasketResponse");

		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, deleteBasketResponse.getStatusCode());
		Assert.assertEquals(delete_discount_basket_items, null);

		utils.logPass("Verified that basket id " + expDiscountBasketItemId + " is deleted successfully");

	}

	// @Test(priority = -1)
	public void ommPrerequisite() throws InterruptedException {
		System.out.println("OMM252AuthAPIAddDeleteDiscountsToBasketTest.ommPrerequisite()--- START");
		List<String> slugList = new ArrayList<String>();
		slugList.add("autoone");
		slugList.add("autothree");
		slugList.add("autofour");
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
			pageObj.cockpitRedemptionsPage().clickOnUpdateButton();
			pageObj.dashboardpage().navigateToAllBusinessPage();
			utils.logPass("Flags are updated for the business: " + str);

		}

	}

	@Test(description = "SQ-T5679 Mobile>Add Discount in Discount Basket>Verify max_applicable_quantity is returned for processing function-Target Price for Bundle,Rate rollback and Target Price for Bundle advanced || "
			+ "SQ-T5681 API2>Get Active Discount Basket>Verify max_applicable_quantity is returned for processing function-Target Price for Bundle,rate rollback and Target Price for Bundle advanced", groups = {
					"regression", "dailyrun" },priority = 8)
	@Owner(name = "Hardik Bhardwaj")
	public void T5679_addDiscountMobileMaxAppQty() throws InterruptedException {
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_auto_redemption", "check");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_discount_locking", "check");
		pageObj.cockpitRedemptionsPage().setAcquisitionType("Offers", "3");
		pageObj.dashboardpage().updateCheckBox();

		// create subscription plan
		String redeemableID1 = dataSet.get("redeemableID1");
		String redeemableID2 = dataSet.get("redeemableID2");
		String redeemableID3 = dataSet.get("redeemableID3");

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

		utils.logPass("Send redeemable to the user successfully");
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send message to user");

		// get reward id
		String rewardId1 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemableID1);

		utils.logPass("Reward id " + rewardId1 + " is generated successfully ");

		// send reward amount to user Reedemable
		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				redeemableID2, "", "");

		utils.logPass("Send redeemable to the user successfully");
		Assert.assertEquals(sendRewardResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send message to user");

		// get reward id
		String rewardId2 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemableID2);

		utils.logPass("Reward id " + rewardId2 + " is generated successfully ");

		// send reward amount to user Reedemable
		Response sendRewardResponse3 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				redeemableID3, "", "");

		utils.logPass("Send redeemable to the user successfully");
		Assert.assertEquals(sendRewardResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send message to user");

		// get reward id
		String rewardId3 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemableID3);

		utils.logPass("Reward id " + rewardId3 + " is generated successfully ");

		// add reward completion to basket
		Response discountBasketResponse = pageObj.endpoints().addDiscountToBasketAPI2(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId1);
		Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for Mobile add reward completion to basket");

		// add reward completion to basket
		Response discountBasketResponse2 = pageObj.endpoints().addDiscountToBasketAPI2(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId2);
		Assert.assertEquals(discountBasketResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for Mobile add reward completion to basket");

		// add reward completion to basket
		Response discountBasketResponse3 = pageObj.endpoints().addDiscountToBasketAPI2(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId3);
		Assert.assertEquals(discountBasketResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for Mobile add reward completion to basket");

		String maxAppQtyVal1 = discountBasketResponse3.jsonPath()
				.getString("discount_basket_items[0].discount_details.max_applicable_quantity");
		String maxAppQtyVal2 = discountBasketResponse3.jsonPath()
				.getString("discount_basket_items[1].discount_details.max_applicable_quantity");
		String maxAppQtyVal3 = discountBasketResponse3.jsonPath()
				.getString("discount_basket_items[2].discount_details.max_applicable_quantity");

		Assert.assertEquals(maxAppQtyVal1, dataSet.get("maxAplcQty1"),
				"max_applicable_quantity is NULL for add reward to basket with QC as Target Price For a Bundle Advance");

		Assert.assertEquals(maxAppQtyVal2, dataSet.get("maxAplcQty2"),
				"max_applicable_quantity is NULL for add reward to basket with QC as Target Price For a Bundle");

		Assert.assertEquals(maxAppQtyVal3, dataSet.get("maxAplcQty3"),
				"max_applicable_quantity is NULL for add reward to basket with QC as Rate Roll Back");

		utils.logPass(
				"In Mobile add discount to basket, The max_applicable_quantity field should be present and contain a valid value for QC as Target Price For a Bundle Advance, Target Price For a Bundle and Rate Roll Back");

		// Mobile GET Active Discount Basket API
		Response basketDiscountDetailsResponse = pageObj.endpoints().getUserDiscountBasketDetailsUsingAPI2(token,
				dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(basketDiscountDetailsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for  Auth GET Active Discount Basket API");
		utils.logit("In Auth GET Active Discount Basket API is successful");

		String maxAppQtyVal4 = basketDiscountDetailsResponse.jsonPath()
				.getString("discount_basket_items[0].discount_details.max_applicable_quantity");
		String maxAppQtyVal5 = basketDiscountDetailsResponse.jsonPath()
				.getString("discount_basket_items[1].discount_details.max_applicable_quantity");
		String maxAppQtyVal6 = basketDiscountDetailsResponse.jsonPath()
				.getString("discount_basket_items[2].discount_details.max_applicable_quantity");

		Assert.assertEquals(maxAppQtyVal4, dataSet.get("maxAplcQty1"),
				"max_applicable_quantity is NULL for Fetch Active Basket with QC as Target Price For a Bundle Advance");

		Assert.assertEquals(maxAppQtyVal5, dataSet.get("maxAplcQty2"),
				"max_applicable_quantity is NULL for Fetch Active Basket with QC as Target Price For a Bundle");

		Assert.assertEquals(maxAppQtyVal6, dataSet.get("maxAplcQty3"),
				"max_applicable_quantity is NULL for Fetch Active Basket with QC as Rate Roll Back");

		utils.logPass(
				"In Mobile Fetch Active Basket, The max_applicable_quantity field should be present and contain a valid value for QC as Target Price For a Bundle Advance, Target Price For a Bundle and Rate Roll Back");

	}

	@Test(description = "SQ-T5848 Verify Auto Redemption of Banked Currency with Reward Discount Applied", groups = { "regression", "dailyrun" }, priority = 9)
	@Owner(name = "Vansham Mishra")
	public void T5848_VerifyRewardAmountInAutoRedemptionOfBankedCurrency() throws Exception {
		String discountAmount = dataSet.get("discountAmount");
		String remainingBalance = dataSet.get("remainingBalance");
		// create user
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		String externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));

		//send reward amount to the user
		pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "50", "", "", "");

		// POS Auto Unlock
		Response autoUnlockResponse1 = pageObj.endpoints().autoSelectPosApi(userID, "30", "1", "123456", externalUID,
				dataSet.get("locationKey"));
		Assert.assertEquals(autoUnlockResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Auto Unlock ");
		String discountType = autoUnlockResponse1.jsonPath().getString("discount_basket_items[0].discount_type");
		Assert.assertEquals(discountType, "discount_amount",
				"Discount type is not matched with discount_amount for Auto select API");
		utils.logPass("Discount type is not matched with discount_amount for Auto select API");

		// POS Discount Lookup Api
		Response discountLookupResponse0 = pageObj.endpoints().discountLookUpPosApi(dataSet.get("locationKey"), userID,
				"101", "30", externalUID);
		Assert.assertEquals(discountLookupResponse0.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Discount Lookup Api ");

		// assert selected_discounts[0].discount_type is discount_amount
		String discountType0 = discountLookupResponse0.jsonPath().getString("selected_discounts[0].discount_type");
		Assert.assertEquals(discountType0, "discount_amount",
				"Discount type is not matched with discount_amount for Discount Lookup API");
		utils.logPass("Discount type is not matched with discount_amount for Discount Lookup API");

		// verify that selected_discounts[0].discount_value should be 10
		String discountValue0 = discountLookupResponse0.jsonPath().getString("selected_discounts[0].discount_amount");
		Assert.assertEquals(discountValue0, discountAmount,
				"Discount value is not matched with "+discountAmount+" for Discount Lookup API");
		utils.logPass("Discount Amount is matched with "+discountAmount+" for Discount Lookup API");

		// verify that selected_discounts[0].remaining_balance should be 40
		String remainingBalance0 = discountLookupResponse0.jsonPath().getString("selected_discounts[0].remaining_balance");
		Assert.assertEquals(remainingBalance0, remainingBalance,
				"Remaining balance is not matched with "+remainingBalance+" for Discount Lookup API");
		utils.logPass("Remaining balance is matched with "+remainingBalance+" for Discount Lookup API");


	}
	@Test(description = "SQ-T5847 Verify if Enable Auto-redemption is enabled and strategy Auto redemption discount types-> Discount Amount is selected and user has an empty Active basket then applicable Discount Amount gets added to basket on hitting Auto Select API" +
			"SQ-T5909 Verify if Enable Auto-redemption is enabled and strategy Auto redemption discount types-> Discount Amount is selected and user does not have an Active basket then basket gets created on hitting Auto Select API", groups = { "regression", "dailyrun" }, priority = 10)
	@Owner(name = "Vansham Mishra")
	public void T5847_VerifyDiscountAmountGetsAddedToBasketOnHittingAuthAutoSelectAPI() throws Exception {
		String discountAmount = dataSet.get("discountAmount");
		String remainingBalance = dataSet.get("remainingBalance");
		// create user
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		String externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));

		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();
		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Pizza1", "1", "14", "M", "10", "999", "1", "12003");
		parentMap.put("Pizza1", detailsMap1);

		// hit auth Auto Select API when user does not have any rewards
		Response redemptionResponse1 = pageObj.endpoints().authAutoSelectAPI(dataSet.get("client"),
				dataSet.get("secret"), token, "14", externalUID, parentMap);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, redemptionResponse1.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");

		List<String> discountBasketItems = redemptionResponse1.jsonPath().getList("discount_basket_items");
		Assert.assertTrue(discountBasketItems.isEmpty(),"Discount basket is not empty");
		logger.info("Discount basket is empty for Auto Unlock API");
		utils.logPass("Discount basket is empty for Auto Unlock API");

		logger.info("Verified that user has the empty discount basket");
		utils.logPass("Discount basket is empty for Auto Unlock API");

		//send reward amount to the user
		pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "50", "", "", "");

		// hit auth Auto Select API when user have some reward amount
		Response autoUnlockResponse1 = pageObj.endpoints().authAutoSelectAPI(dataSet.get("client"),
				dataSet.get("secret"), token, "14", externalUID, parentMap);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, redemptionResponse1.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		Assert.assertEquals(autoUnlockResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Auto Unlock ");

		// verify that Discount Amount gets qualified and added to discount_basket
		String discountType = autoUnlockResponse1.jsonPath().getString("discount_basket_items[0].discount_type");
		Assert.assertEquals(discountType, "discount_amount",
				"Discount type is not matched with discount_amount for Auto select API");
		String discountValue = autoUnlockResponse1.jsonPath().getString("discount_basket_items[0].discount_value");
		Assert.assertEquals(discountValue, discountAmount,
				"Discount value is not matched with "+discountAmount+" for Auto select API");
		logger.info("verified that Discount Amount gets qualified and added to discount_basket");
		utils.logPass("verified that Discount Amount gets qualified and added to discount_basket");

	}
	@Test(description = "SQ-T5837 Verify the UI Option for Selecting Discount Amount Discount Type", groups = { "regression", "dailyrun" }, priority = 11)
	@Owner(name = "Vansham Mishra")
	public void T5837_verifyUiOptionForDiscountType() throws Exception {
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.cockpitRedemptionsPage().setAutoRedemptionDiscounts("Discount Amount","Select");
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();
		utils.logPass("Verified that Discount Amount is displayed in discount type dropdown and user is able to select it");
	}

	@Test(description = "SQ-T5861 Verify if Set redemption processing priority by Acquisition Type is set to Earned Rewards -> 1 then user is not able to add more than 1 Discount Amount using Auto Select API", groups = { "regression", "dailyrun" }, priority = 12)
	@Owner(name = "Vansham Mishra")
	public void T5861_VerifySingleDiscountAmountInBasket() throws Exception {

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Validations");
		pageObj.cockpitRedemptionsPage().updateMaxRedemptionAmount("50");
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboard("Enable Reward Locking", "uncheck");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboard("Enable Auto-redemption", "check");
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.cockpitRedemptionsPage().setAcquisitionType("Earned Rewards", "1");
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("API2 Signup is successful");

		pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "25", "", "", "");

		// Adding reward amount into discount basket
		Response discountBasketResponse = pageObj.endpoints().addDiscountAmountToDiscountBasketAUTH(token,
				dataSet.get("client"), dataSet.get("secret"), "discount_amount", "25");
		Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		utils.logPass("Verified the discount type and ID in Basket add response");

		// create subscription plan
		String spName = dataSet.get("subscriptionName");
		String spPrice = Integer.toString(Utilities.getRandomNoFromRange(300, 500));
		String PlanID = dataSet.get("PlanID");

		// subscription purchase api 2
		Response purchaseSubscriptionresponse = pageObj.endpoints().Api2SubscriptionPurchase(token, PlanID,
				dataSet.get("client"), dataSet.get("secret"), spPrice, endDateTime);
		Assert.assertEquals(purchaseSubscriptionresponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);

		int subscription_id = Integer
				.parseInt(purchaseSubscriptionresponse.jsonPath().get("subscription_id").toString());
		logger.info(userEmail + " purchased " + subscription_id + " Plan id = " + PlanID);

		// Adding subscription into discount basket
		Response discountBasketResponse2 = pageObj.endpoints().authListDiscountBasketAddedAUTH(token,
				dataSet.get("client"), dataSet.get("secret"), "subscription", subscription_id + "");
		Assert.assertEquals(discountBasketResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		int actual_discount_discount_id = Integer.parseInt(discountBasketResponse2.jsonPath()
				.getString("discount_basket_items[1].discount_id").replace("[", "").replace("]", ""));
		Assert.assertEquals(actual_discount_discount_id, subscription_id);

		utils.logPass("Verified that " + actual_discount_discount_id + " in add basket response");

		//send reward amount to the user
		pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "50", "", "", "");
		Response rewardResponse = pageObj.endpoints().authListAvailableRewardsNew(token, dataSet.get("client"),
				dataSet.get("secret"));

		String rewardID = rewardResponse.jsonPath().getString("id").replace("[", "").replace("]", "");

		utils.logPass("Reward id " + rewardID + " is generated successfully ");

		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();
		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Pizza1", "1", "30", "M", "10", "999", "1", "12003");
		parentMap.put("Pizza1", detailsMap1);
		String externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));

		Response redemptionResponse1 = pageObj.endpoints().authAutoSelectAPI(dataSet.get("client"),
				dataSet.get("secret"), token, "30", externalUID, parentMap);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, redemptionResponse1.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");

		// verify that only 1 item with discount_type "discount_amount" is present in discountAmountItems
		List<String> discountAmountItems = redemptionResponse1.jsonPath().getList("discount_basket_items.discount_type");
		long count = discountAmountItems.stream().filter(item -> item.equals("discount_amount")).count();
		Assert.assertEquals(count, 1,
				"Only 1 Discount Amount  gets added to discount_basket for Auto Select API");
		utils.logPass("Verified that Only 1 Discount Amount  gets added to discount_basket for Auto Select API");

	}

	@Test(description = "SQ-T5943 Verify that when the Guest has $50 banked currency, max redemption limit is EMPTY, and receipt total amount is $30 with no other discounts applied, discount_amount should be auto-applied with value equal to $30", priority = 13)
	@Owner(name = "Vansham Mishra")
	public void T5943_verifyAutoAppliedDiscountAmountWithEmptyMaxRedemptionLimit() throws Exception {

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Validations");
		pageObj.cockpitRedemptionsPage().SetMaxRedemption("clear","10.0");
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("API2 Signup is successful");

		pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "50", "", "", "");

		// Adding reward amount into discount basket
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();
		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Pizza1", "1", "30", "M", "10", "999", "1", "12003");
		parentMap.put("Pizza1", detailsMap1);
		String externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));

		String discountAmount = dataSet.get("discountAmount");

		Response redemptionResponse1 = pageObj.endpoints().authAutoSelectAPI(dataSet.get("client"),
				dataSet.get("secret"), token, "30", externalUID, parentMap);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, redemptionResponse1.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");

		// verify that only 1 item with discount_type "discount_amount" is present in discountAmountItems
		List<String> discountAmountItems = redemptionResponse1.jsonPath().getList("discount_basket_items.discount_type");
		long count = discountAmountItems.stream().filter(item -> item.equals("discount_amount")).count();
		Assert.assertEquals(count, 1,
				"Only 1 Discount Amount  gets added to discount_basket for Auto Select API");
		utils.logPass("Verified that Only 1 Discount Amount  gets added to discount_basket for Auto Select API");

		// validate that discount_type is discount_amount
		String discountType = redemptionResponse1.jsonPath().getString("discount_basket_items[0].discount_type");
		Assert.assertEquals(discountType, "discount_amount",
				"Discount type is not matched with discount_amount for Auto select API");
		utils.logPass("Discount type is matched with discount_amount for Auto select API");
		String discountValue = redemptionResponse1.jsonPath().getString("discount_basket_items[0].discount_value");
		Assert.assertEquals(discountValue, discountAmount,
				"Discount value is not matched with "+discountAmount+" for Auto select API");
		utils.logPass("Discount Amount is matched with "+discountAmount+" for Auto select API");

		// setting max redemption limit back to 10.0
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Validations");
		pageObj.cockpitRedemptionsPage().SetMaxRedemption("update","10.0");
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

	}
	@Test(description = "SQ-T6056 Verify if Enable Auto-redemption is enabled and strategy Auto redemption discount types-> Discount Amount is selected and user has an Active basket containing discounts then Discount Amount gets added to basket on hitting Auto Select API.", priority = 14)
	@Owner(name = "Vansham Mishra")
	public void T6056_verifyEnableAutoRedemptionAddsDiscountAmountToBasket() throws Exception {
		String discountAmount = dataSet.get("discountAmount");
		String remainingBalance = dataSet.get("remainingBalance");
		// create user
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		String externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));

		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();
		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Pizza1", "1", "14", "M", "10", "999", "1", "12003");
		parentMap.put("Pizza1", detailsMap1);

		// hit auth Auto Select API when user does not have any rewards
		Response redemptionResponse1 = pageObj.endpoints().authAutoSelectAPI(dataSet.get("client"),
				dataSet.get("secret"), token, "14", externalUID, parentMap);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, redemptionResponse1.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");

		List<String> discountBasketItems = redemptionResponse1.jsonPath().getList("discount_basket_items");
		Assert.assertTrue(discountBasketItems.isEmpty(),"Discount basket is not empty");
		logger.info("Discount basket is empty for Auto Unlock API");
		utils.logPass("Discount basket is empty for Auto Unlock API");

		logger.info("Verified that user has the empty discount basket");
		utils.logPass("Discount basket is empty for Auto Unlock API");

		//send reward amount to the user
		pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "50", "", "", "");

		// hit auth Auto Select API when user have some reward amount
		Response autoUnlockResponse1 = pageObj.endpoints().authAutoSelectAPI(dataSet.get("client"),
				dataSet.get("secret"), token, "14", externalUID, parentMap);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, redemptionResponse1.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		Assert.assertEquals(autoUnlockResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Auto Unlock ");

		// verify that Discount Amount gets qualified and added to discount_basket
		String discountType = autoUnlockResponse1.jsonPath().getString("discount_basket_items[0].discount_type");
		Assert.assertEquals(discountType, "discount_amount",
				"Discount type is not matched with discount_amount for Auto select API");
		String discountValue = autoUnlockResponse1.jsonPath().getString("discount_basket_items[0].discount_value");
		Assert.assertEquals(discountValue, discountAmount,
				"Discount value is not matched with "+discountAmount+" for Auto select API");
		logger.info("verified that Discount Amount gets qualified and added to discount_basket");
		utils.logPass("verified that Discount Amount gets qualified and added to discount_basket");

	}
	@Test(description = "SQ-T6287 Base redeemable with QC>Verify that when the Guest has $50 banked currency, max redemption limit is EMPTY, and receipt total amount is $30 with no other discounts applied, discount_amount should be auto-applied with value equal to $30", priority = 15)
	@Owner(name = "Vansham Mishra")
	public void T6287_verifyAutoAppliedDiscountAmountWithEmptyMaxRedemptionLimit() throws Exception {

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Validations");
		pageObj.cockpitRedemptionsPage().SetMaxRedemption("clear","10.0");
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("API2 Signup is successful");

		pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "50", "", "", "");

		// Adding reward amount into discount basket
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();
		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Pizza1", "1", "30", "M", "10", "999", "1", "12003");
		parentMap.put("Pizza1", detailsMap1);
		String externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));

		String discountAmount = dataSet.get("discountAmount");

		// Fetch user balance
		Response userBalanceResponse = pageObj.endpoints().Api2FetchUserBalance(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(userBalanceResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 fetch user balance");
		utils.logPass("Api2 Fetch user balance is successful");

		Response redemptionResponse1 = pageObj.endpoints().authAutoSelectAPI(dataSet.get("client"),
				dataSet.get("secret"), token, "30", externalUID, parentMap);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, redemptionResponse1.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");

		// verify that only 1 item with discount_type "discount_amount" is present in discountAmountItems
		List<String> discountAmountItems = redemptionResponse1.jsonPath().getList("discount_basket_items.discount_type");
		long count = discountAmountItems.stream().filter(item -> item.equals("discount_amount")).count();
		Assert.assertEquals(count, 1,
				"Only 1 Discount Amount  gets added to discount_basket for Auto Select API");
		utils.logPass("Verified that Only 1 Discount Amount  gets added to discount_basket for Auto Select API");

		// validate that discount_type is discount_amount
		String discountType = redemptionResponse1.jsonPath().getString("discount_basket_items[0].discount_type");
		Assert.assertEquals(discountType, "discount_amount",
				"Discount type is not matched with discount_amount for Auto select API");
		utils.logPass("Discount type is matched with discount_amount for Auto select API");
		String discountValue = redemptionResponse1.jsonPath().getString("discount_basket_items[0].discount_value");
		Assert.assertEquals(discountValue, discountAmount,
				"Discount value is not matched with "+discountAmount+" for Auto select API");
		utils.logPass("Discount Amount is matched with "+discountAmount+" for Auto select API");

		// setting max redemption limit back to 10.0
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Validations");
		pageObj.cockpitRedemptionsPage().SetMaxRedemption("update","10.0");
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

	}
	@Test(description = "SQ-T6336 Test the calculation logic for fuel rewards when qualified items are present." +
			"SQ-T6321 Verify auto-redemption for fuel rewards is supported for manual conversion type businesses with auto-redemption flag turned on in the cockpit.", groups = { "sanity", "dailyrun" },priority = 16)
	@Owner(name = "Vansham Mishra")
	public void T6336_testFuelRewardCalculationWithQualifiedItems() {

		String fuelAmount = Utilities.getRandomNoFromRange(2, 8) + "";
		// create user
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		String externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "", "", fuelAmount, "");
		Response autoUnlockResponse1 = pageObj.endpoints().autoSelectPosApi(userID, "30", "1", "101", externalUID,
				dataSet.get("locationkey"));
		Assert.assertEquals(autoUnlockResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for auto select pos api");
		String discountType = autoUnlockResponse1.jsonPath().getString("discount_basket_items[0].discount_type");
		Assert.assertEquals(discountType, "fuel_reward",
				"Discount type is not matched with fuel_reward for Auto select API");
		utils.logPass("Discount type is matched with fuel_reward for Auto select API");
	}
	@AfterMethod(alwaysRun = true)
	public void teraDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}
}