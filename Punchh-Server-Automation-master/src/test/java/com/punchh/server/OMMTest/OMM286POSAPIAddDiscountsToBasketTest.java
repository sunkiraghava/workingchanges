package com.punchh.server.OMMTest;

import java.lang.reflect.Method;
import java.text.ParseException;
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
import com.punchh.server.apiConfig.ApiResponseJsonSchema;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.ApiUtils;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

/*
 * @Author : Shashank Sharma 
 */
@Listeners(TestListeners.class)
public class OMM286POSAPIAddDiscountsToBasketTest {
	static Logger logger = LogManager.getLogger(OMM286POSAPIAddDiscountsToBasketTest.class);
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
		endDateTime = CreateDateTime.getTomorrowDate() + " 10:00 AM";
		utils = new Utilities(driver);
	}

	@Test(description = "Fuel reward to added into discount basket "
			+ "OMM-T573- POS discount lookup API>Fuel_reward>Validate that if discount type->Fuel_reward is added into basket then no data gets displayed in discount_details under Selected discounts"
			+ "OMM-T724- POS Batch Redemption>Fuel_reward>Validate that if discount type->Fuel_reward is added into basket then no data gets displayed in discount_details under Success and failures", groups = {
					"regression", "dailyrun" })
	@Owner(name = "Shashank Sharma")
	public void OMMT573_T724AddFuelRewardToDiscountBasketPOS() {

		String fuelAmount = Utilities.getRandomNoFromRange(2, 8) + "";
		// create user
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();

		pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "", "", "10", "");

		Response discountBasketResponse = pageObj.endpoints()
				.authListDiscountBasketAddedForPOSAPI(dataSet.get("locationkey2"), userID, "fuel_reward", fuelAmount);

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

		Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		String expDiscountBasketItemId = discountBasketResponse.jsonPath()
				.getString("discount_basket_items.discount_basket_item_id");

		Response userDiscountLookupResponse = pageObj.endpoints().POSDiscountLookup(dataSet.get("locationkey2"), userID,
				"");
		Assert.assertEquals(userDiscountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		String userLookpDiscountDetails = userDiscountLookupResponse.jsonPath()
				.getString("selected_discounts.discount_details");
		Assert.assertEquals(userLookpDiscountDetails, "[null]");

		Response basketDiscountDetailsResponse = pageObj.endpoints().getUserDiscountBasketDetailsUsingPOS(userID,
				dataSet.get("locationkey2"));

		String actualDiscountDetailsResponse = basketDiscountDetailsResponse.jsonPath()
				.getString("discount_basket_items.discount_basket_item_id");

		Assert.assertEquals(actualDiscountDetailsResponse, expDiscountBasketItemId);

		String userActiveBasketResponseDiscountDetails = basketDiscountDetailsResponse.jsonPath()
				.getString("discount_basket_items.discount_details");
		Assert.assertEquals(userActiveBasketResponseDiscountDetails, "[null]");

		expDiscountBasketItemId = expDiscountBasketItemId.replace("[", "").replace("]", "");
		Response deleteBasketResponse = pageObj.endpoints().deleteDiscountBasketForUserPOS(dataSet.get("locationkey2"),
				userID, expDiscountBasketItemId);

		String delete_discount_basket_items = deleteBasketResponse.jsonPath().get("deleteBasketResponse");

		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, deleteBasketResponse.getStatusCode());
		Assert.assertEquals(delete_discount_basket_items, null);

		utils.logPass("Verified that basket id " + expDiscountBasketItemId + " is deleted successfully");

	}

	@Test(description = "Discount amount to added into discount basket "
			+ "OMM-T61 / SQ-T3134[Batched Redemptions] Verify functionality cases for \"discount_type -> discount_amount\" in \"Add Discounts To Basket\" API (POS)", groups = {
					"regression", "dailyrun" })
	@Owner(name = "Shashank Sharma")
	public void addDiscountAmountToDiscountBasketPOS() {
		System.out.println("slug=== " + dataSet.get("slug"));
		String discountAmount = dataSet.get("discountAmount");
		// create user
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		String userID = signUpResponse.jsonPath().get("user.user_id").toString();

		pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), dataSet.get("discountAmount"), "", "", "");

		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAddedForPOSAPI(
				dataSet.get("locationkey"), userID, "discount_amount", discountAmount);
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

		String expDiscountBasketItemId = discountBasketResponse.jsonPath()
				.getString("discount_basket_items.discount_basket_item_id");

		Response basketDiscountDetailsResponse = pageObj.endpoints().getUserDiscountBasketDetailsUsingPOS(userID,
				dataSet.get("locationkey"));

		String actualDiscountDetailsResponse = basketDiscountDetailsResponse.jsonPath()
				.getString("discount_basket_items.discount_basket_item_id");

		Assert.assertEquals(actualDiscountDetailsResponse, expDiscountBasketItemId);
		expDiscountBasketItemId = expDiscountBasketItemId.replace("[", "").replace("]", "");
		Response deleteBasketResponse = pageObj.endpoints().deleteDiscountBasketForUserPOS(dataSet.get("locationkey"),
				userID, expDiscountBasketItemId);

		Response discountBasketResponse1 = pageObj.endpoints()
				.authListDiscountBasketAddedForPOSAPI(dataSet.get("locationkey"), userID, "discount_amount", "200");
		boolean isDiscountBasketInsufficientBalanceSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.posDiscountBasketDiscountValueErrorSchema, discountBasketResponse1.asString());
		Assert.assertTrue(isDiscountBasketInsufficientBalanceSchemaValidated,
				"POS Add Discount Basket Response Schema Validation failed");
		System.out.println("discountBasketResponse1--- " + discountBasketResponse1.asPrettyString());
		String errorMessage = discountBasketResponse1.jsonPath().getString("error.discount_value").replace("[", "")
				.replace("]", "");
		Assert.assertEquals(errorMessage, "Balance is insufficent to process request.",
				errorMessage + " error message not matched");

		String delete_discount_basket_items = deleteBasketResponse.jsonPath().get("deleteBasketResponse");

		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, deleteBasketResponse.getStatusCode());
		Assert.assertEquals(delete_discount_basket_items, null);

		utils.logPass("Verified that basket id " + expDiscountBasketItemId + " is deleted successfully");

	}

	// OMM-T522 / OMM-T521
	@Test(description = "Subscription to added into discount basket"
			+ "OMM-T521 - Auth API>subscription plan>Validate that if subscription with Flat discount is added into basket then mentioned NULL value in Base Amount under 'discount_details' gets displayed"
			+ "OMM-T522 - Auth API>Subscription plan>Validate that if Subscription plan with name,description and image is added into basket then mentioned name,description and image path gets displayed under discount_details ", groups = {
					"regression", "dailyrun" })
	@Owner(name = "Shashank Sharma")
	public void subscriptionToAddInDiscountBucketPOS() throws InterruptedException {
//		boolean GlobalBenefitRedemptionThrottlingToggle = false;
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboard("Enable Reward Locking", "uncheck");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboard("Enable Auto-redemption", "check");

		// create subscription plan
		String spName = "OMM SubcriptionPlan";
//		String QCname = "QcSubscription_" + CreateDateTime.getTimeDateString();
//		String amountcap = Integer.toString(Utilities.getRandomNoFromRange(10, 200));
//		String unitDiscount = Integer.toString(Utilities.getRandomNoFromRange(1, 10));
		String spPrice = Integer.toString(Utilities.getRandomNoFromRange(300, 500));
//		int expSpPrice = Integer.parseInt(spPrice);
		String startDate = CreateDateTime.getYesterdayDays(2); // "2024-01-10T19:30:00Z";
		String endDate = CreateDateTime.getFutureDate(1);// "2024-01-13T04:30:00Z";

//		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
//		pageObj.instanceDashboardPage().loginToInstance();
//		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
//		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");
//		pageObj.qualificationcriteriapage().createQualificationCriteria(QCname, amountcap,
//				dataSet.get("qcFucntionName"), unitDiscount, true, dataSet.get("lineItemSelectorName"));
//		pageObj.menupage().clickSubscriptionsMenuIcon();
//		pageObj.menupage().clickSubscriptionPlansLink();
//		pageObj.subscriptionPlansPage().createSubscriptionPlan(spName, spPrice, QCname, dataSet.get("qcFucntionName"),
//				GlobalBenefitRedemptionThrottlingToggle, endDateTime);
//		String PlanID = pageObj.subscriptionPlansPage().getSbuscriptionPlanID(spName);

		// create user
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();

		// subscription purchase api 2
		Response purchaseSubscriptionresponse = pageObj.endpoints().Api2SubscriptionPurchase(token,
				dataSet.get("PlanID"), dataSet.get("client"), dataSet.get("secret"), spPrice, endDateTime);
		Assert.assertEquals(purchaseSubscriptionresponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		int subscription_id = Integer
				.parseInt(purchaseSubscriptionresponse.jsonPath().get("subscription_id").toString());
		logger.info(userEmail + " purchased " + subscription_id + " Plan id = " + dataSet.get("PlanID"));

		// Adding subscription into discount basket

		Response addDiscountIntoBasketResponse = pageObj.endpoints().authListDiscountBasketAddedForPOSAPI(
				dataSet.get("locationkey"), userID, "subscription", subscription_id + "");
		Assert.assertEquals(addDiscountIntoBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		int actual_discount_discount_id = Integer.parseInt(addDiscountIntoBasketResponse.jsonPath()
				.getString("discount_basket_items.discount_id").replace("[", "").replace("]", ""));
		Assert.assertEquals(actual_discount_discount_id, subscription_id);

		utils.logPass("Verified that " + actual_discount_discount_id + " in add basket response");

		String actualBaseAmountBasketAddedRespo = addDiscountIntoBasketResponse.jsonPath()
				.getString("discount_basket_items.discount_details.base_amount").replace("[", "").replace("]", "");
		Assert.assertEquals(actualBaseAmountBasketAddedRespo, "null");

		String actual_discount_type_addBasketResponse = addDiscountIntoBasketResponse.jsonPath()
				.getString("discount_basket_items.discount_type");

		Assert.assertEquals(actual_discount_type_addBasketResponse, "[subscription]");

		utils.logPass("Verified the redemption_code in add basket response");

		int userSubscriptionID = Integer.parseInt(addDiscountIntoBasketResponse.jsonPath()
				.getString("discount_basket_items.discount_id").replace("[", "").replace("]", ""));

		String expDiscountBasketID = addDiscountIntoBasketResponse.jsonPath()
				.getString("discount_basket_items.discount_basket_item_id").replace("[", "").replace("]", "");
		Assert.assertEquals(userSubscriptionID, subscription_id, "User got the wrong subscription plan ");

		String actualSubscriptionNameAddBasketReponse = addDiscountIntoBasketResponse.jsonPath()
				.getString("discount_basket_items.discount_details.name").replace("[", "").replace("]", "");
		Assert.assertEquals(actualSubscriptionNameAddBasketReponse, spName, spName + " subscription name not matched.");
		String actualSubscriptionImageAddBasketReponse = addDiscountIntoBasketResponse.jsonPath()
				.getString("discount_basket_items.discount_details.image").replace("[", "").replace("]", "");
		Assert.assertTrue(actualSubscriptionImageAddBasketReponse.contains("subscription_images"),
				"Image link is not coming in response");

		String actualSubscriptionDescriptionAddBasketReponse = addDiscountIntoBasketResponse.jsonPath()
				.getString("discount_basket_items.discount_details.description").replace("[", "").replace("]", "");
		Assert.assertEquals(actualSubscriptionDescriptionAddBasketReponse, "Automated Subscription Plan");

		String actualStartDateAddDiscountIntoBasketResponse = addDiscountIntoBasketResponse.jsonPath()
				.getString("discount_basket_items.discount_details.start_date_tz").replace("[", "").replace("]", "");
		Assert.assertTrue(actualStartDateAddDiscountIntoBasketResponse.contains(startDate),
				"Start date is not displaying");

		String actualEndDateAddDiscountIntoBasketResponse = addDiscountIntoBasketResponse.jsonPath()
				.getString("discount_basket_items.discount_details.end_date_tz").replace("[", "").replace("]", "");

		Assert.assertTrue(actualEndDateAddDiscountIntoBasketResponse.contains(endDate),
				endDate + " expected not matched with actual end date " + actualEndDateAddDiscountIntoBasketResponse);

		Response basketDiscountDetailsResponse = pageObj.endpoints().getUserDiscountBasketDetailsUsingPOS(userID,
				dataSet.get("locationkey"));
		Assert.assertEquals(basketDiscountDetailsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for POS API Get Active Discount Basket");
		boolean isPosGetActiveDiscountBasketSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.posGetActiveDiscountBasketSchema, basketDiscountDetailsResponse.asString());
		Assert.assertTrue(isPosGetActiveDiscountBasketSchemaValidated,
				"POS API Get Active Discount Basket Schema Validation failed");
		String actualBaseAmountGetBasketDetailsResponse = basketDiscountDetailsResponse.jsonPath()
				.getString("discount_basket_items.discount_details.base_amount").replace("[", "").replace("]", "");

		Assert.assertEquals(actualBaseAmountGetBasketDetailsResponse, "null");

		String actualDiscountBasketItemIDFromBasket = basketDiscountDetailsResponse.jsonPath()
				.getString("discount_basket_items.discount_basket_item_id").replace("[", "").replace("]", "");
		String actualDiscpuntIDFromBasket = basketDiscountDetailsResponse.jsonPath()
				.getString("discount_basket_items.discount_id").replace("[", "").replace("]", "");

		String actualSubscriptionNameFromBasket = basketDiscountDetailsResponse.jsonPath()
				.getString("discount_basket_items.discount_details.name").replace("[", "").replace("]", "");

		Assert.assertEquals(actualDiscountBasketItemIDFromBasket, expDiscountBasketID + "");
		Assert.assertEquals(actualDiscpuntIDFromBasket, userSubscriptionID + "");
		Assert.assertEquals(spName, actualSubscriptionNameFromBasket);

		String actualSubscriptionNameActiveBasketReponse = basketDiscountDetailsResponse.jsonPath()
				.getString("discount_basket_items.discount_details.name").replace("[", "").replace("]", "");
		Assert.assertEquals(actualSubscriptionNameActiveBasketReponse, spName,
				spName + " subscription name not matched.");

		String actualSubscriptionImageActiveBasketReponse = basketDiscountDetailsResponse.jsonPath()
				.getString("discount_basket_items.discount_details.image").replace("[", "").replace("]", "");
		Assert.assertTrue(actualSubscriptionImageActiveBasketReponse.contains("subscription_images"),
				"Image link is not coming in response");

		String actualSubscriptionDescriptionActiveBasketReponse = basketDiscountDetailsResponse.jsonPath()
				.getString("discount_basket_items.discount_details.description").replace("[", "").replace("]", "");
		Assert.assertEquals(actualSubscriptionDescriptionActiveBasketReponse, "Automated Subscription Plan");

		String actualStartDatebasketDiscountDetailsResponse = basketDiscountDetailsResponse.jsonPath()
				.getString("discount_basket_items.discount_details.start_date_tz").replace("[", "").replace("]", "");
		Assert.assertTrue(actualStartDatebasketDiscountDetailsResponse.contains(startDate),
				"Start date is not displaying");

		String actualEndDatebasketDiscountDetailsResponse = basketDiscountDetailsResponse.jsonPath()
				.getString("discount_basket_items.discount_details.end_date_tz").replace("[", "").replace("]", "");
		Assert.assertTrue(actualEndDatebasketDiscountDetailsResponse.contains(endDate), "End date is not displaying");

		expDiscountBasketID = expDiscountBasketID.replace("[", "").replace("]", "");
		Response deleteBasketResponse = pageObj.endpoints().deleteDiscountBasketForUserPOS(dataSet.get("locationkey"),
				userID, expDiscountBasketID);

		String delete_discount_basket_items = deleteBasketResponse.jsonPath().get("deleteBasketResponse");

		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, deleteBasketResponse.getStatusCode());
		Assert.assertEquals(delete_discount_basket_items, null);

		utils.logPass("Verified that basket id " + expDiscountBasketID + " is deleted successfully");
	}

	@Test(description = "Promo Coupon to added into discount basket ", groups = { "sanity", "dailyrun" })
	@Owner(name = "Shashank Sharma")
	public void AddingPromoCodeCouponToUserDiscountBasketPOS() throws InterruptedException, ParseException {

		String promoCode = CreateDateTime.getRandomString(6).toUpperCase() + Utilities.getRandomNoFromRange(500, 2000);
		String coupanCampaignName = "Auto_PromoCampaign" + CreateDateTime.getTimeDateString();

		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();

		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_promos", "check");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_coupons", "check");
		pageObj.dashboardpage().updateCheckBox();
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

		Response discountBasketResponse = pageObj.endpoints()
				.authListDiscountBasketAddedForPOSAPI(dataSet.get("locationkey"), userID, "redemption_code", promoCode);

		String actual_discount_discount_id = discountBasketResponse.jsonPath()
				.getString("discount_basket_items.discount_id").replace("[", "").replace("]", "");
		actual_discount_discount_id = actual_discount_discount_id.replace("[", "").replace("]", "");
		Assert.assertEquals(actual_discount_discount_id, promoCode);

		utils.logPass("Verified that " + actual_discount_discount_id + " in add basket response");

		String actual_discount_type_addBasketResponse = discountBasketResponse.jsonPath()
				.getString("discount_basket_items.discount_type");

		Assert.assertEquals(actual_discount_type_addBasketResponse, "[redemption_code]");

		logger.info("Verified the redemption_code in add basket response");

		String expdiscount_basket_item_id = discountBasketResponse.jsonPath()
				.getString("discount_basket_items.discount_basket_item_id").replace("[", "").replace("]", "");

		Response basketDiscountDetailsResponse = pageObj.endpoints().getUserDiscountBasketDetailsUsingPOS(userID,
				dataSet.get("locationkey"));

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
		Response deleteBasketResponse = pageObj.endpoints().deleteDiscountBasketForUserPOS(dataSet.get("locationkey"),
				userID, expdiscount_basket_item_id);
		String delete_discount_basket_items = deleteBasketResponse.jsonPath().get("deleteBasketResponse");

		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, deleteBasketResponse.getStatusCode());
		Assert.assertEquals(delete_discount_basket_items, null);

		utils.logPass("Verified that basket id " + expdiscount_basket_item_id + " is deleted successfully");

	}

	@Test(description = "OMM-T29 Verify API parameter validations for Cupon added into discountsm to basket "
			+ "OMM-T390 / SQ-T3869 (1.0)	POS API>Coupon Campaign>Validate that if redemption_code with Flat discount is added into basket then mentioned NULL value in Base Amount under 'discount_details' gets displayed"
			+ "OMM-T392 / SQ-T3871 (1.0)	POS API>Coupon Campaign>Validate that if Redemption_code with name,description and image is added into basket then mentioned name,description and image path gets displayed under discount_details"
			+ "OMM-T391 /SQ-T3870 (1.0)	POS API>Coupon Campaign>Validate that if redemption_code with QC is added into basket then NULL value in Base Amount under 'discount_details' gets displayed", groups = {
					"regression", "dailyrun" })
	@Owner(name = "Shashank Sharma")
	public void AddRedemptionCuponCodeIntoUserDiscountBasketPOS() throws InterruptedException, ParseException {

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
				dataSet.get("usagePerGuest"), dataSet.get("giftType"), dataSet.get("amount"), "", dataSet.get("qcName"),
				GlobalBenefitRedemptionThrottlingToggle);

		//pageObj.signupcampaignpage().setStartDate();
		//pageObj.signupcampaignpage().setEndDateforCouponCampaign(1);

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
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		utils.longWaitInSeconds(4);
		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAddedForPOSAPI(
				dataSet.get("locationkey"), userID, "redemption_code", generatedCodeName);

		String base_amountDiscountBasketResponse = discountBasketResponse.jsonPath()
				.getString("discount_basket_items.discount_details.base_amount").replace("[", "").replace("]", "");

		Assert.assertEquals(base_amountDiscountBasketResponse, "null");

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

		Response basketDiscountDetailsResponse = pageObj.endpoints().getUserDiscountBasketDetailsUsingPOS(userID,
				dataSet.get("locationkey"));
		String base_amountbasketDiscountDetailsResponse = basketDiscountDetailsResponse.jsonPath()
				.getString("discount_basket_items.discount_details.base_amount").replace("[", "").replace("]", "");

		Assert.assertEquals(base_amountbasketDiscountDetailsResponse, "null");

		String actualCouponNamebasketDiscountDetailsResponse = basketDiscountDetailsResponse.jsonPath()
				.getString("discount_basket_items.discount_details.name").replace("[", "").replace("]", "");
		Assert.assertEquals(actualCouponNamebasketDiscountDetailsResponse, coupanCampaignName,
				coupanCampaignName + " coupon name not matched.");
		String actualSCouponImageGetBasketReponse = basketDiscountDetailsResponse.jsonPath()
				.getString("discount_basket_items.discount_details.image").replace("[", "").replace("]", "");
		Assert.assertTrue(actualSCouponImageGetBasketReponse.contains(".png"), "Image link is not coming in response");

		String actualCouponDescriptionGetBasketReponse = basketDiscountDetailsResponse.jsonPath()
				.getString("discount_basket_items.discount_details.description").replace("[", "").replace("]", "");
		Assert.assertEquals(actualCouponDescriptionGetBasketReponse, "Automated Coupon");

		String actualDiscountBasketItemIDFromBasket = basketDiscountDetailsResponse.jsonPath()
				.getString("discount_basket_items.discount_basket_item_id").replace("[", "").replace("]", "");
		String actualPromoCodeFromBasket = basketDiscountDetailsResponse.jsonPath()
				.getString("discount_basket_items.discount_id").replace("[", "").replace("]", "");

		Assert.assertEquals(actualDiscountBasketItemIDFromBasket, expdiscount_basket_item_id);
		Assert.assertEquals(actualPromoCodeFromBasket, generatedCodeName);

		expdiscount_basket_item_id = expdiscount_basket_item_id.replace("[", "").replace("]", "");
		Response deleteBasketResponse = pageObj.endpoints().deleteDiscountBasketForUserPOS(dataSet.get("locationkey"),
				userID, expdiscount_basket_item_id);

		String delete_discount_basket_items = deleteBasketResponse.jsonPath().get("deleteBasketResponse");

		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, deleteBasketResponse.getStatusCode());
		Assert.assertEquals(delete_discount_basket_items, null);

		utils.logPass("Verified that basket id " + expdiscount_basket_item_id + " is deleted successfully");

	}

	@Test(description = "OMM-T29 Verify API parameter validations for Add Discounts To Basket API2 (mobile)", groups = {
			"unstable", "sanity", "dailyrun" })
	@Owner(name = "Shashank Sharma")
	public void AddRewardDiscountToBasketPOS() throws InterruptedException {
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

		utils.logit("== Auth API List Available Rewards ==");
		Response rewardResponse = pageObj.endpoints().authListAvailableRewardsNew(token, dataSet.get("client"),
				dataSet.get("secret"));
		boolean isAuthListAvailableRewardsSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.authListAvailableRewardsSchema, rewardResponse.asString());
		Assert.assertTrue(isAuthListAvailableRewardsSchemaValidated,
				"Auth List Available Rewards Schema Validation failed");
		String rewardID = rewardResponse.jsonPath().getString("id").replace("[", "").replace("]", "");

		utils.logit("Reward id " + rewardID + " is generated successfully ");

		Response discountBasketResponse = pageObj.endpoints()
				.authListDiscountBasketAddedForPOSAPI(dataSet.get("locationkey"), userID, "reward", rewardID);

		String actualDiscountType_InBasketAddResponse = discountBasketResponse.jsonPath()
				.getString("discount_basket_items.discount_type");
		String actualDiscountID_InBasketAddResponse = discountBasketResponse.jsonPath()
				.getString("discount_basket_items.discount_id").replace("[", "").replace("]", "");

		Assert.assertEquals(actualDiscountType_InBasketAddResponse, "[reward]");
		Assert.assertEquals(actualDiscountID_InBasketAddResponse, rewardID);

		utils.logPass("Verified the discount type and ID in Basket add response");

		String expdiscount_basket_item_id = discountBasketResponse.jsonPath()
				.getString("discount_basket_items.discount_basket_item_id").replace("[", "").replace("]", "");

		String discount_id = discountBasketResponse.jsonPath().getString("discount_basket_items.discount_id")
				.replace("[", "").replace("]", "");
		Assert.assertEquals(discount_id, rewardID);

		Response basketDiscountDetailsResponse = pageObj.endpoints().getUserDiscountBasketDetailsUsingPOS(userID,
				dataSet.get("locationkey"));

		String actualDiscountBasketItemIDFromBasket = basketDiscountDetailsResponse.jsonPath()
				.getString("discount_basket_items.discount_basket_item_id").replace("[", "").replace("]", "");
		String actualDiscountIDFromBasket = basketDiscountDetailsResponse.jsonPath()
				.getString("discount_basket_items.discount_id").replace("[", "").replace("]", "");

		Assert.assertEquals(actualDiscountIDFromBasket, rewardID);
		Assert.assertEquals(expdiscount_basket_item_id, actualDiscountBasketItemIDFromBasket);

		expdiscount_basket_item_id = expdiscount_basket_item_id.replace("[", "").replace("]", "");
		Response deleteBasketResponse = pageObj.endpoints().deleteDiscountBasketForUserPOS(dataSet.get("locationkey"),
				userID, expdiscount_basket_item_id);

		String delete_discount_basket_items = deleteBasketResponse.jsonPath().get("deleteBasketResponse");

		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, deleteBasketResponse.getStatusCode());
		Assert.assertEquals(delete_discount_basket_items, null);

		utils.logPass("Verified that basket is deleted successfully");

	}

	@Test(description = "OMM-T29 Verify API parameter validations for card completion added into discountsm to basket / "
			+ "OMM-T574 - POS discount lookup API>Card completion>Validate that if discount type->Card completion is added into basket then no data gets displayed in discount_details under Selected discounts"
			+ "OMM-T725 - POS Batch Redemption>Card completion>Validate that if discount type->Card completion is added into basket then no data gets displayed in discount_details under Success and failures"
			+ "OMM-T229	Validate that in API-{{path}}/api/pos/discounts/lookup response, qualified_menu_items are renamed to qualified_items"
			+ "OMM-T231	/ SQ-T3867 Validate that in API-{{path}}/api/auth/batch_redemptions response, qualified_menu_items are renamed to qualified_items", groups = {
					"regression", "dailyrun" })
	@Owner(name = "Shashank Sharma")
	public void addCardCompletionToUserDiscountBasketPOS() {

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
		Assert.assertEquals(sendAmountResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		// Adding subscription into discount basket

		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAddedForPOSAPI(
				dataSet.get("locationkey"), userID, "card_completion", discountID);

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

		Response posuserLookUpCall_CarCompletionresponse = pageObj.endpoints()
				.POSDiscountLookup(dataSet.get("locationkey"), userID, "");
		Assert.assertEquals(posuserLookUpCall_CarCompletionresponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		String qualified_itemsList = posuserLookUpCall_CarCompletionresponse.jsonPath()
				.getString("selected_discounts.qualified_items");

		Assert.assertTrue(qualified_itemsList.length() > 1);
		String qualified_menu_itemsNotDisplayed = posuserLookUpCall_CarCompletionresponse.jsonPath()
				.getString("selected_discounts.qualified_menu_items").replace("[", "").replace("]", "");

		Assert.assertEquals(qualified_menu_itemsNotDisplayed, "null");

		String userLookpDiscountDetails = posuserLookUpCall_CarCompletionresponse.jsonPath()
				.getString("selected_discounts.discount_details");
		Assert.assertEquals(userLookpDiscountDetails, "[null]");

		// user batch redemption
		Response batchRedemptionProcessResponseUser1 = pageObj.endpoints()
				.processBatchRedemptionOfBasketPOSNew(dataSet.get("locationkey"), userID);

		String batchRedemptionProcessResponseUser1DiscountDetails = batchRedemptionProcessResponseUser1.jsonPath()
				.getString("success.discount_details");
		Assert.assertEquals(batchRedemptionProcessResponseUser1DiscountDetails, "[null]");

		String qualified_itemsListBatchRedemption = batchRedemptionProcessResponseUser1.jsonPath()
				.getString("success.qualified_items");

		Assert.assertTrue(qualified_itemsListBatchRedemption.length() > 1);
		String qualified_menu_itemsNotDisplayedBatchRedemption = batchRedemptionProcessResponseUser1.jsonPath()
				.getString("success.qualified_menu_items").replace("[", "").replace("]", "");

		Assert.assertEquals(qualified_menu_itemsNotDisplayedBatchRedemption, "null");

	}

	// Author: Shashank
	@Test(description = "SQ-T3411-- Point based currency>Validate that if max redemption "
			+ "limit is 10 and discount_basket already contain 10 offers then no reward "
			+ "should get added in discount_basket automatically on hitting auto_select API", groups = { "sanity", "dailyrun" })
	@Owner(name = "Shashank Sharma")
	public void T3411_VerifyMaxRedemptionLimitExceedAutoSelectNotAddingRewardInBasket() throws Exception {
		int maxRedemptionAllowed = Integer.parseInt(dataSet.get("maxRedemptionAllowed"));
		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");

		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.cockpitRedemptionsPage().updateTotalNumberOfRedemptionsAllowed(maxRedemptionAllowed + "");
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// create user
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		String userID = signUpResponse.jsonPath().get("user.user_id").toString();

		// Gift 1
		pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "", dataSet.get("redeemable_id"), "", "");
		pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "", dataSet.get("redeemable_id"), "", "");
		pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "", dataSet.get("redeemable_id"), "", "");
		pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "", dataSet.get("redeemable_id"), "", "");
		pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "", dataSet.get("redeemable_id"), "", "");
		pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "", dataSet.get("redeemable_id"), "", "");
		pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "", dataSet.get("redeemable_id"), "", "");
		pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "", dataSet.get("redeemable_id"), "", "");
		pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "", dataSet.get("redeemable_id"), "", "");
		pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "", dataSet.get("redeemable_id"), "", "");
		pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "", dataSet.get("redeemable_id"), "", "");

		Response rewardResponse = pageObj.endpoints().authListAvailableRewardsNew(token, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(rewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		String rewardID1 = rewardResponse.jsonPath().getString("id[0]").replace("[", "").replace("]", "");
		utils.logPass("Reward ID 1 = " + rewardID1);

		String rewardID2 = rewardResponse.jsonPath().getString("id[1]").replace("[", "").replace("]", "");
		utils.logPass("Reward ID 2 = " + rewardID2);

		String rewardID3 = rewardResponse.jsonPath().getString("id[2]").replace("[", "").replace("]", "");
		utils.logPass("Reward ID 3 = " + rewardID3);

		String rewardID4 = rewardResponse.jsonPath().getString("id[3]").replace("[", "").replace("]", "");
		utils.logPass("Reward ID 4 = " + rewardID4);

		String rewardID5 = rewardResponse.jsonPath().getString("id[4]").replace("[", "").replace("]", "");
		utils.logPass("Reward ID 5 = " + rewardID5);
		
		
		
		String rewardID6 = rewardResponse.jsonPath().getString("id[5]").replace("[", "").replace("]", "");
		utils.logPass("Reward ID 6 = " + rewardID6);

		String rewardID7 = rewardResponse.jsonPath().getString("id[6]").replace("[", "").replace("]", "");
		utils.logPass("Reward ID 7 = " + rewardID7);

		String rewardID8 = rewardResponse.jsonPath().getString("id[7]").replace("[", "").replace("]", "");
		utils.logPass("Reward ID 8 = " + rewardID8);

		String rewardID9 = rewardResponse.jsonPath().getString("id[8]").replace("[", "").replace("]", "");
		utils.logPass("Reward ID 9 = " + rewardID9);

		String rewardID10 = rewardResponse.jsonPath().getString("id[9]").replace("[", "").replace("]", "");
		utils.logPass("Reward ID 10 = " + rewardID10);
		
		

		Response discountBasketResponse1 = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID1);
		Assert.assertEquals(discountBasketResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		utils.logPass(rewardID1 + " is added to basket ");

		Response discountBasketResponse2 = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID2);
		Assert.assertEquals(discountBasketResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		utils.logPass(rewardID2 + " is added to basket ");

		Response discountBasketResponse3 = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID3);
		Assert.assertEquals(discountBasketResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		utils.logPass(rewardID3 + " is added to basket ");

		Response discountBasketResponse4 = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID4);
		Assert.assertEquals(discountBasketResponse4.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		utils.logPass(rewardID4 + " is added to basket ");

		Response discountBasketResponse5 = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID5);
		Assert.assertEquals(discountBasketResponse5.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		utils.logPass(rewardID5 + " is added to basket ");
		
		
		Response discountBasketResponse6 = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID6);
		Assert.assertEquals(discountBasketResponse6.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		utils.logPass(rewardID6 + " is added to basket ");

		Response discountBasketResponse7 = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID7);
		Assert.assertEquals(discountBasketResponse7.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		utils.logPass(rewardID7 + " is added to basket ");

		Response discountBasketResponse8 = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID8);
		Assert.assertEquals(discountBasketResponse8.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		utils.logPass(rewardID8 + " is added to basket ");

		Response discountBasketResponse9 = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID9);
		Assert.assertEquals(discountBasketResponse9.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		utils.logPass(rewardID9 + " is added to basket ");

		Response discountBasketResponse10 = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID10);
		Assert.assertEquals(discountBasketResponse10.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		utils.logPass(rewardID10 + " is added to basket ");
		
		
		
		
		
		

		Response autoUnlockResponse = pageObj.endpoints().autoSelectPosApi(userID, "30", "1", "101", "",
				dataSet.get("locationkey"));
		Assert.assertEquals(autoUnlockResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		List<Object> listOfAutoSelectBasket = autoUnlockResponse.jsonPath().getList("discount_basket_items");

		Assert.assertEquals(listOfAutoSelectBasket.size(), maxRedemptionAllowed, " Auto Select is happen for 6 reward");

		utils.logPass(
				"Verified that auto select is not happening if limit is exceed of 'Total number of redemptions allowed'");

	}

	// Author: Shashank
	@Test(description = "SQ-T3414	Point based currency>Validate that if auto-redemption, "
			+ "strategy-Offers is enabled and user does not have any active basket then new "
			+ "discount_basket should get created on hitting auto_select API", groups = { "sanity", "dailyrun" })
	@Owner(name = "Shashank Sharma")
	public void T3414_VerifyAutoSelectAPICreatingNewBasket() throws InterruptedException {

		int maxRedemptionAllowed = Integer.parseInt(dataSet.get("maxRedemptionAllowed"));
		int expectedRedeemableID = Integer.parseInt(dataSet.get("redeemable_id"));

		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");

		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.cockpitRedemptionsPage().updateTotalNumberOfRedemptionsAllowed(maxRedemptionAllowed + "");
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// create user
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		String userID = signUpResponse.jsonPath().get("user.user_id").toString();

		// Gift 1
		pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "", dataSet.get("redeemable_id"), "", "");

		Response rewardResponse = pageObj.endpoints().authListAvailableRewardsNew(token, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(rewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		String rewardID1 = rewardResponse.jsonPath().getString("id[0]").replace("[", "").replace("]", "");
		utils.logPass("Reward ID 1 = " + rewardID1);

		Response autoUnlockResponse = pageObj.endpoints().autoSelectPosApi(userID, "30", "1", "12003", "",
				dataSet.get("locationkey"));

		Assert.assertEquals(autoUnlockResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

//		int actualRedeemableID = Integer.parseInt(autoUnlockResponse.jsonPath()
//				.getString("discount_basket_items.discount_details.item_id").replace("[", "").replace("]", ""));
//		Assert.assertEquals(actualRedeemableID, expectedRedeemableID,
//				expectedRedeemableID + " redeemableID not matched in auto select response ");
//
//		List<Object> listOfAutoSelectBasket = autoUnlockResponse.jsonPath().getList("discount_basket_items");
//
//		Assert.assertEquals(listOfAutoSelectBasket.size(), maxRedemptionAllowed, " Auto Select is not happen ");
//
//		logger.info(
//				"Verified that auto select is not happening if limit is exceed of 'Total number of redemptions allowed'");
//		utils.logPass(
//				"Verified that auto select is not happening if limit is exceed of 'Total number of redemptions allowed'");

	}

	// Author: Shashank
	@Test(description = "SQ-T3410	Point based currency>Validate that if none of non-loyalty rewards available in "
			+ "'Rewards' table is getting qualified then no reward should get added in discount_basket automatically "
			+ "on hitting auto_select API", groups = { "sanity", "dailyrun" })
	@Owner(name = "Shashank Sharma")
	public void T3410_VerifyAutoSelectDidNotAddIntobasketIfNoRewardAvailable() throws Exception {
		int expectedRedeemableID = Integer.parseInt(dataSet.get("redeemable_id"));
		// create user
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");

		String userID = signUpResponse.jsonPath().get("user.user_id").toString();

		String deleteQuery = "delete from rewards where user_id='" + userID + "'";
		boolean deleteQueryResult = DBUtils.executeQuery(env, deleteQuery);
		// Assert.assertFalse(deleteQueryResult);
		// Assert.assertTrue(deleteQueryResult>=1, deleteQuery + " Delete query not
		// executed successfully ");

		Response autoUnlockResponse = pageObj.endpoints().autoSelectPosApi(userID, "30", "1", "101", "",
				dataSet.get("locationkey"));

		Assert.assertEquals(autoUnlockResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		List<Object> listOfAutoSelectBasket = autoUnlockResponse.jsonPath().getList("discount_basket_items");

		Assert.assertEquals(listOfAutoSelectBasket.size(), 0, " Auto Select is not happen ");

		utils.logPass("Verified that if no reward available then Auto select will not add any discount basket");

	}

	// Author: Shashank
	@Test(description = "SQ-T3415	Point based currency>Validate that If 'Offers' value is set to 1 in 'Set redemption processing priority by Acquisition Type', then user is not able to add more than 1 non-loyalty rewards using auto-redemption", groups = {
			"regression", "dailyrun" })
	@Owner(name = "Shashank Sharma")
	public void T3415_VerifyAutoSelectDidNotAddIntobasketMoreThanOffersValueSetInAcquisitionType()
			throws InterruptedException {

		String offersValueInAcquisitionType = dataSet.get("offersValueInAcquisitionType");

		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");

		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");

		pageObj.cockpitRedemptionsPage().setAutoRedemptionDiscounts("Offers", "Select");

		pageObj.cockpitRedemptionsPage().setAcquisitionType("Offers", offersValueInAcquisitionType);
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// create user
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");

		String userID = signUpResponse.jsonPath().get("user.user_id").toString();

		pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "", dataSet.get("redeemable_id"), "", "");

		pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "", dataSet.get("redeemable_id"), "", "");

		Response autoUnlockResponse = pageObj.endpoints().autoSelectPosApi(userID, "30", "1", "101", "",
				dataSet.get("locationkey"));

		Assert.assertEquals(autoUnlockResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		List<Object> listOfAutoSelectBasket = autoUnlockResponse.jsonPath().getList("discount_basket_items");

		Assert.assertEquals(listOfAutoSelectBasket.size(), 1, " Auto Select is should not happen for 2 times");

		utils.logPass("Verified that if Ofers set to 1 then auto select can not add basket 2 times");

		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");

		pageObj.cockpitRedemptionsPage().setAutoRedemptionDiscounts("Offers", "Select");

		pageObj.cockpitRedemptionsPage().setAcquisitionType("Offers", "5");
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

	}

	// Author: Shashank
	@Test(description = "SQ-T3413	Point based currency>Validate that if auto-redemption is enabled but there is no strategy and user does not have any active basket then empty basket gets crearted on hitting auto_select API", groups = {
			"regression", "dailyrun" })
	@Owner(name = "Shashank Sharma")
	public void T3413_VerifyAutoSelectDidNotAddIntobasketIfNoStrategyMention() throws InterruptedException {

//		String offersValueInAcquisitionType = dataSet.get("offersValueInAcquisitionType");

		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");

		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");

		pageObj.cockpitRedemptionsPage().setAutoRedemptionDiscounts("Offers", "Unselect");

//		pageObj.cockpitRedemptionsPage().setAcquisitionType("Offers", offersValueInAcquisitionType);
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		Thread.sleep(10000);
		// create user
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");

		String userID = signUpResponse.jsonPath().get("user.user_id").toString();

		pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "", dataSet.get("redeemable_id"), "", "");

		pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "", dataSet.get("redeemable_id"), "", "");

		Response autoUnlockResponse = pageObj.endpoints().autoSelectPosApi(userID, "30", "1", "101", "",
				dataSet.get("locationkey"));

		Assert.assertEquals(autoUnlockResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		List<Object> listOfAutoSelectBasket = autoUnlockResponse.jsonPath().getList("discount_basket_items");

		Assert.assertEquals(listOfAutoSelectBasket.size(), 0, " Auto Select is should not happen for 2 times");

		utils.logPass("Verified that if Ofers set to 1 then auto select can not add basket 2 times");

		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");

		pageObj.cockpitRedemptionsPage().setAutoRedemptionDiscounts("Offers", "Select");

		pageObj.cockpitRedemptionsPage().setAcquisitionType("Offers", "5");
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

	}

	// Author: Shashank
	@Test(description = "SQ-T3409	Point based currency>Validate that if partial non-loyalty rewards available in 'Rewards' table are getting qualified then rewards which are getting qualified should only get added in discount_basket automatically on hitting auto_select API", groups = {
			"regression", "dailyrun" })
	@Owner(name = "Shashank Sharma")
	public void T3409_VerifyAutoSelectDidNotAddIntobasketIfQCFails() throws InterruptedException {

		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.dashboardpage().checkUncheckFlagCockpitDashboard("Enable Auto-redemption", "check");
		
		pageObj.cockpitRedemptionsPage().setAutoRedemptionDiscounts("Offers", "Select");
		pageObj.cockpitRedemptionsPage().setAcquisitionType("Offers", "10");
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// create user
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");

		String userID = signUpResponse.jsonPath().get("user.user_id").toString();

        pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
            dataSet.get("redeemable_id1"), "",
				"");

        pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
            dataSet.get("redeemable_id2"), "",
				"");

        pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
            dataSet.get("redeemable_id3"), "",
				"");

		pageObj.endpoints().sendMessageToUser(userID, dataSet.get("admin_key"), "", dataSet.get("redeemable_id4"), "",
				"");

		Response autoUnlockResponse = pageObj.endpoints().autoSelectPosApi(userID, "30", "1", "1010", "1123333",
            dataSet.get("locationKey"));
		Assert.assertEquals(autoUnlockResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		List<Object> listOfAutoSelectBasket = autoUnlockResponse.jsonPath()
				.getList("discount_basket_items.discount_details.name");

		Assert.assertTrue(listOfAutoSelectBasket.contains(dataSet.get("redeemableName1")),
				dataSet.get("redeemableName1") + " is NOT available in autoseletct api response");
		Assert.assertTrue(listOfAutoSelectBasket.contains(dataSet.get("redeemableName2")),
				dataSet.get("redeemableName2") + " is NOT available in autoseletct api response");
		Assert.assertFalse(listOfAutoSelectBasket.contains(dataSet.get("redeemableName3")),
				dataSet.get("redeemableName3") + " is available in autoseletct api response");
		Assert.assertTrue(listOfAutoSelectBasket.contains(dataSet.get("redeemableName4")),
				dataSet.get("redeemableName4") + " is NOT available in autoseletct api response");


		utils.logPass("Verified that if Ofers set to 1 then auto select can not add basket 2 times");

		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.cockpitRedemptionsPage().setAutoRedemptionDiscounts("Offers", "Select");
		pageObj.cockpitRedemptionsPage().setAcquisitionType("Offers", "5");
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();
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

	@Test(description = "SQ-T5676 POS discount lookup>Validate that 'Max discounted units-NULL' is displayed for subscriptions in selected discounts, regardless of whether they qualify or not", groups = {
			"regression", "dailyrun" })
	@Owner(name = "Hardik Bhardwaj")
	public void T5676_subscriptionDisCountLookUp() throws InterruptedException {
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboard("Enable Reward Locking", "check");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboard("Enable Auto-redemption", "check");

		// create subscription plan
		String spName = dataSet.get("PlanIDName");
		String spPrice = Integer.toString(Utilities.getRandomNoFromRange(300, 500));
		String startDate = CreateDateTime.getYesterdayDays(2); // "2024-01-10T19:30:00Z";
		String endDate = CreateDateTime.getFutureDate(1);// "2024-01-13T04:30:00Z";

		// create user
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();

		// subscription purchase api 2
		Response purchaseSubscriptionresponse = pageObj.endpoints().Api2SubscriptionPurchase(token,
				dataSet.get("PlanID"), dataSet.get("client"), dataSet.get("secret"), spPrice, endDateTime);
		Assert.assertEquals(purchaseSubscriptionresponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		String subscription_id = (purchaseSubscriptionresponse.jsonPath().get("subscription_id").toString());
		logger.info(userEmail + " purchased " + subscription_id + " Plan id = " + dataSet.get("PlanID"));

		// Adding subscription into discount basket

		// Response addDiscountIntoBasketResponse =
		// pageObj.endpoints().authListDiscountBasketAddedForPOSAPI(dataSet.get("location_Key"),
		// userID, "subscription", subscription_id + "");
		String externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));

		// POS Add Discount to Basket
		Response addDiscountIntoBasketResponse = pageObj.endpoints().authListDiscountBasketAddedForPOSAPIWithExt_Uid(
				dataSet.get("location_Key"), userID, "subscription", subscription_id, externalUID);

		Assert.assertEquals(addDiscountIntoBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		String actual_discount_discount_id = addDiscountIntoBasketResponse.jsonPath()
				.getString("discount_basket_items.discount_id").replace("[", "").replace("]", "");
		Assert.assertEquals(actual_discount_discount_id, subscription_id);

		utils.logPass("Verified that " + actual_discount_discount_id + " in add basket response");

		// https://punchhdev.atlassian.net/browse/OMM-1169 - assertion changed because
		// of this ticket added in 22 April 2025 release

		// POS Discount Lookup
		Response discountLookupResponse = pageObj.endpoints().discountLookUpPosApi(dataSet.get("location_Key"), userID,
				dataSet.get("invalid_item_id"), "10", externalUID);
		Assert.assertEquals(discountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Discount Lookup Api ");
		utils.logit("POS Discount Lookup Api is successful");

		String qualified = discountLookupResponse.jsonPath().getString("selected_discounts[0].qualified");
		Assert.assertEquals(qualified, "false", "In POS Discount Lookup Api QC is Qualified");
		utils.logit("In POS Discount Lookup Api QC is not Qualified");

		String maxAppQtyVal = discountLookupResponse.jsonPath()
				.getString("selected_discounts[0].discount_details.max_applicable_quantity");
		Assert.assertEquals(maxAppQtyVal, "1000.0", "max_applicable_quantity is  NULL for unselected discounts");
		utils.logit("In POS Discount Lookup Api max_applicable_quantity is 1000.0 for unselected discounts");

		// POS Discount Lookup
		Response discountLookupResponse0 = pageObj.endpoints().discountLookUpPosApi(dataSet.get("location_Key"), userID,
				dataSet.get("item_id"), "10", externalUID);
		Assert.assertEquals(discountLookupResponse0.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Discount Lookup Api ");
		utils.logit("POS Discount Lookup Api is successful");
		String maxAppQtyVal1 = discountLookupResponse0.jsonPath()
				.getString("selected_discounts[0].discount_details.max_applicable_quantity");
		Assert.assertEquals(maxAppQtyVal1, "1.0", "max_applicable_quantity is NULL for selected discounts");
		utils.logit("In POS Discount Lookup Api max_applicable_quantity is 1.0 for selected discounts");
	}

	@Test(description = "SQ-T5677 Verify that max_applicable_quantity value is equal to NULL if the processing function does not have a Max Discounted Units field", groups = {
			"regression", "dailyrun" })
	@Owner(name = "Hardik Bhardwaj")
	public void T5677_rewardMaxAppQty() throws InterruptedException {
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboard("Enable Reward Locking", "check");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboard("Enable Auto-redemption", "check");

		// create subscription plan
		String externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		String redeemableID = dataSet.get("redeemableID");

		// create user
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				redeemableID, "", "");

		utils.logPass("Send redeemable to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		// get reward id
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemableID);

		utils.logit("Reward id " + rewardId + " is generated successfully ");

		// add reward completion to basket
		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAddedForPOSAPIWithExt_Uid(
				dataSet.get("location_Key"), userID, "reward", rewardId, externalUID);
		Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for POS add reward completion to basket");

		// Auth GET Active Discount Basket API
		Response basketDiscountDetailsResponse = pageObj.endpoints().fetchActiveBasketAuthApi(token,
				dataSet.get("client"), dataSet.get("secret"), externalUID);
		Assert.assertEquals(basketDiscountDetailsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for  Auth GET Active Discount Basket API");
		utils.logit("In Auth GET Active Discount Basket API is successful");
		String maxAppQtyVal = basketDiscountDetailsResponse.jsonPath()
				.getString("discount_basket_items[0].discount_details.max_applicable_quantity");
		Assert.assertEquals(maxAppQtyVal, null, "max_applicable_quantity is not NULL for Active Discount Basket");
		utils.logit("In Auth GET Active Discount Basket API max_applicable_quantity is NULL");
	}

	@Test(description = "SQ-T5678 POS>Add Discount in Discount Basket>Verify max_applicable_quantity is returned for processing function-Target Price for Bundle,Rate rollback and Target Price for Bundle advanced", groups = {
			"regression", "dailyrun" })
	@Owner(name = "Hardik Bhardwaj")
	public void T5678_addDiscountMaxAppQty() throws InterruptedException {
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
		String externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
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

		utils.logit("Reward id " + rewardId1 + " is generated successfully ");

		// send reward amount to user Reedemable
		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				redeemableID2, "", "");

		utils.logPass("Send redeemable to the user successfully");
		Assert.assertEquals(sendRewardResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send message to user");

		// get reward id
		String rewardId2 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemableID2);

		utils.logit("Reward id " + rewardId2 + " is generated successfully ");

		// send reward amount to user Reedemable
		Response sendRewardResponse3 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				redeemableID3, "", "");

		utils.logPass("Send redeemable to the user successfully");
		Assert.assertEquals(sendRewardResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send message to user");

		// get reward id
		String rewardId3 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemableID3);

		utils.logit("Reward id " + rewardId3 + " is generated successfully ");

		// add reward completion to basket
		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAddedForPOSAPIWithExt_Uid(
				dataSet.get("location_Key"), userID, "reward", rewardId1, externalUID);
		Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for POS add reward completion to basket");

		// add reward completion to basket
		Response discountBasketResponse2 = pageObj.endpoints().authListDiscountBasketAddedForPOSAPIWithExt_Uid(
				dataSet.get("location_Key"), userID, "reward", rewardId2, externalUID);
		Assert.assertEquals(discountBasketResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for POS add reward completion to basket");

		// add reward completion to basket
		Response discountBasketResponse3 = pageObj.endpoints().authListDiscountBasketAddedForPOSAPIWithExt_Uid(
				dataSet.get("location_Key"), userID, "reward", rewardId3, externalUID);
		Assert.assertEquals(discountBasketResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for POS add reward completion to basket");

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
				"In POS add discount to basket, The max_applicable_quantity field should be present and contain a valid value for QC as Target Price For a Bundle Advance, Target Price For a Bundle and Rate Roll Back");

	}


	  //shashank
	@Test(
			description = "OMM-T3326 /SQ-T5794 POS discount lookup>Verify that Max discounted units is getting displayed in selected discounts if reward/redeemable is getting qualified i.e. -Qualified-TRUE " +
					"OMM-T3327/SQ-T5795  POS discount lookup>Verify that Max discounted units is getting displayed in selected discounts even if reward/redeemable is not getting qualified i.e. -Qualified-FALSE",
			groups = {"regression", "dailyrun"})
	@Owner(name = "Shashank Sharma")
    public void T3326_ValidatePOSDiscountLookupForRedeemableQCTypeRateRollBack()
        throws InterruptedException {
      //navigate to application
        pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
        //login to application
        pageObj.instanceDashboardPage().loginToInstance();
        //select the business
        pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

        //navigate to cocpit > redemptions page
        pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
        //navigate to multiple redemption tab
        pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
        //unselect the flags
        pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_auto_redemption", "uncheck");
        pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_discount_locking",
            "uncheck");
        //set the acquisition type
        pageObj.cockpitRedemptionsPage().setAcquisitionType("Offers", "3");
        //click on update checkbox
        pageObj.dashboardpage().updateCheckBox();

        // create subscription plan
        String externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
        String redeemableID3 = dataSet.get("redeemableID3");

        // create user
        userEmail = pageObj.iframeSingUpPage().generateEmail();
        //hit the user sign up api
        Response signUpResponse =
            pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"), dataSet.get("secret"));
        pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
        // get the userid and token
        String token = signUpResponse.jsonPath().get("access_token.token").toString();
        String userID = signUpResponse.jsonPath().get("user.user_id").toString();

        // send redeemable to user
        Response sendRewardResponse3 = pageObj.endpoints().sendMessageToUser(userID,
            dataSet.get("apiKey"), "", redeemableID3, "", "");

        utils.logPass("Send redeemable to the user successfully");
        Assert.assertEquals(sendRewardResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
            "Status code 201 did not matched for api2 send message to user");

        // get reward id
        String rewardId3 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"),
            dataSet.get("secret"), redeemableID3);

        utils.logit("Reward id " + rewardId3 + " is generated successfully ");
         
        //add basktet to user 
         Response discountBasketResponse3 =
         pageObj.endpoints().authListDiscountBasketAddedForPOSAPIWithExt_Uid(
         dataSet.get("location_Key"), userID, "reward", rewardId3, externalUID);
        
         //hit the discount lookup api
        Response userDiscountLookupResponse =
            pageObj.endpoints().POSDiscountLookup(dataSet.get("location_Key"), userID, "1222222");
       
        // add reward completion to basket

        Assert.assertEquals(userDiscountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
            "Status code 200 did not matched for POS add reward completion to basket");

        //fetch the max_applicable_quantity from discount lookup api response
        int maxAppQtyVal3 = userDiscountLookupResponse.jsonPath()
            .getInt("selected_discounts[0].discount_details.max_applicable_quantity");

        Assert.assertEquals(maxAppQtyVal3,Integer.parseInt( dataSet.get("maxAplcQty3")),
            "max_applicable_quantity is NULL for add reward to basket with QC as Rate Roll Back");

        utils.logPass(
            "In POS add discount to basket, The max_applicable_quantity field should be present and contain a valid value for QC as Target Price For a Bundle Advance, Target Price For a Bundle and Rate Roll Back");

      }
	@Test(description = "SQ-T5869When redeeming for card completion, the discount_amount does not take into account the flat_discount value specified in the Base redeemable." +
			"SQ-T5870 When redeeming for card completion, the discount_amount specified in QC attached to Base redeemable is considered", groups = {
			"regression", "dailyrun" })
	@Owner(name = "Vansham Mishra")
	public void T5869_VerifyCardCompletionDiscountAmountExcludesFlatDiscount() {
		// generate the random discount id for adding card completion into the discount basket
		String discountID = Utilities.getRandomNoFromRange(1, 4) + "";
		// create user
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		String giftAmount = dataSet.get("giftAmount");

		Response sendAmountResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "", "", "",
				giftAmount);
		Assert.assertEquals(sendAmountResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,"Status code 201 did not matched for api2 send message to user");

		// Adding card completion into discount basket
		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAddedForPOSAPI(
				dataSet.get("locationkey"), userID, "card_completion", discountID);
		Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		// hit pos lookup api then verify the discount amount and discount type
		Response posuserLookUpCall_CarCompletionresponse = pageObj.endpoints()
				.POSDiscountLookup(dataSet.get("locationkey"), userID, "");
		Assert.assertEquals(posuserLookUpCall_CarCompletionresponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		String discountAmountPosLookupResponse = posuserLookUpCall_CarCompletionresponse.jsonPath()
				.getString("selected_discounts[0].discount_amount");
		Assert.assertEquals(discountAmountPosLookupResponse, dataSet.get("discountAmount"), "Discount amount is not as expected");

		String discountTypePosLookupResponse = posuserLookUpCall_CarCompletionresponse.jsonPath()
				.getString("selected_discounts[0].discount_type");
		Assert.assertEquals(discountTypePosLookupResponse, "card_completion", "Discount type is not as expected");
		utils.logPass("Verified that card completion discount type is qualified and also verified the discount amount in POS lookup api response");

		// user batch redemption then verify the discount amount and discount type
		Response batchRedemptionProcessResponseUser1 = pageObj.endpoints()
				.processBatchRedemptionOfBasketPOSNew(dataSet.get("locationkey"), userID);

		String batchRedemptionProcessResponseUser1DiscountDetails = batchRedemptionProcessResponseUser1.jsonPath()
				.getString("success.discount_details");
		Assert.assertEquals(batchRedemptionProcessResponseUser1DiscountDetails, "[null]");

		String discountAmountBatchRedemptionResponse = batchRedemptionProcessResponseUser1.jsonPath()
				.getString("success[0].discount_amount");
		Assert.assertEquals(discountAmountBatchRedemptionResponse, dataSet.get("discountAmount"), "Discount amount is not as expected");
		String discountTypeBatchRedemptionResponse = batchRedemptionProcessResponseUser1.jsonPath()
				.getString("success[0].discount_type");
		Assert.assertEquals(discountTypeBatchRedemptionResponse, "card_completion", "Discount type is not as expected");
		utils.logPass("Verified that card completion discount type is qualified and also verified the discount amount in batch redemption");

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}
}
