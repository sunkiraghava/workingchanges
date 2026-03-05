package com.punchh.server.OMMTest;

import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import com.punchh.server.annotations.Owner;
import com.punchh.server.apiConfig.ApiConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.ApiUtils;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;

import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;
import io.restassured.response.Response;

/*
 * @Author : Shashank Sharma
 */

@Listeners(TestListeners.class)
public class OMM222MobileAPIAddDeleteDiscountsToBasketTest {
	static Logger logger = LogManager.getLogger(OMM222MobileAPIAddDeleteDiscountsToBasketTest.class);
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
	private List<String> codeNameList;
	private String endDateTime;

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
		prop = Utilities.loadPropertiesFile("config.properties");
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
		codeNameList = new ArrayList<String>();
		endDateTime = CreateDateTime.getFutureDate(1) + " 10:00 PM";
	}

	@Test(description = "OMM-T29 Verify API parameter validations for Add  Discounts To Basket API (mobile)", groups = { "regression", "dailyrun" }, priority = 0)
	@Owner(name = "Shashank Sharma")
	public void AddRewardDiscountToBasket() throws InterruptedException {

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
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				dataSet.get("redeemable_id"), "", "");

		pageObj.utils().logit("Send redeemable to the user successfully");

		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get().pass("Api2  send reward amount to user is successful");
		// get reward id
		String rewardID = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id"));

		// Response rewardResponse =
		// pageObj.endpoints().authListAvailableRewardsNew(token,
		// dataSet.get("client"),
		// dataSet.get("secret"));

		// String rewardID = rewardResponse.jsonPath().getString("id").replace("[",
		// "").replace("]",
		// "");

		pageObj.utils().logit("Reward id " + rewardID + " is generated successfully ");

		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID);

		String actualDiscountType_InBasketAddResponse = discountBasketResponse.jsonPath()
				.getString("discount_basket_items.discount_type");
		String actualDiscountID_InBasketAddResponse = discountBasketResponse.jsonPath()
				.getString("discount_basket_items.discount_id").replace("[", "").replace("]", "");

		Assert.assertEquals(actualDiscountType_InBasketAddResponse, "[reward]");
		Assert.assertEquals(actualDiscountID_InBasketAddResponse, rewardID);

		pageObj.utils().logPass("Verified the discount type and ID in Basket add response");

		String expdiscount_basket_item_id = discountBasketResponse.jsonPath()
				.getString("discount_basket_items.discount_basket_item_id").replace("[", "").replace("]", "");

		String discount_id = discountBasketResponse.jsonPath().getString("discount_basket_items.discount_id")
				.replace("[", "").replace("]", "");
		Assert.assertEquals(discount_id, rewardID);

		Response basketDiscountDetailsResponse = pageObj.endpoints().getDiscountBasketDetailsOfUsersAPIMobile(token,
				dataSet.get("client"), dataSet.get("secret"));

		String actualDiscountBasketItemIDFromBasket = basketDiscountDetailsResponse.jsonPath()
				.getString("discount_basket_items.discount_basket_item_id").replace("[", "").replace("]", "");
		String actualDiscountIDFromBasket = basketDiscountDetailsResponse.jsonPath()
				.getString("discount_basket_items.discount_id").replace("[", "").replace("]", "");

		Assert.assertEquals(actualDiscountIDFromBasket, rewardID);
		Assert.assertEquals(expdiscount_basket_item_id, actualDiscountBasketItemIDFromBasket);

		expdiscount_basket_item_id = expdiscount_basket_item_id.replace("[", "").replace("]", "");
		Response deleteBasketResponse = pageObj.endpoints().deleteDiscountBasketForUserAPI(token, dataSet.get("client"),
				dataSet.get("secret"), expdiscount_basket_item_id);

		String delete_discount_basket_items = deleteBasketResponse.jsonPath().get("deleteBasketResponse");

		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, deleteBasketResponse.getStatusCode());
		Assert.assertEquals(delete_discount_basket_items, null);

		pageObj.utils().logPass("Verified that basket is deleted successfully");

	}

	@Test(description = "OMM-T29 Verify API parameter validations for Cupon added into discountsm to basket ", groups = {
			"regression", "unstable", "dailyrun" }, priority = 1)
	@Owner(name = "Shashank Sharma")
	public void AddRedemptionCouponCodeIntoUserDiscountBasket() throws InterruptedException, ParseException {

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
		pageObj.utils().logPass("Coupon campaign created successfuly " + coupanCampaignName);

		// Thread.sleep(8000);
		pageObj.campaignspage().searchCampaign(coupanCampaignName);
		codeNameList = pageObj.campaignspage().getPreGeneratedCuponCodeList();
		int numberOfGuestExpected = Integer.parseInt(dataSet.get("noOfGuests"));
		Assert.assertEquals(codeNameList.size(), numberOfGuestExpected);
		pageObj.utils().logPass("Expected no of coupon code i.e. " + codeNameList
				+ "is equal to actual no of coupon code i.e. " + dataSet.get("noOfGuests"));

		String generatedCodeName = codeNameList.get(0).toString();// pageObj.campaignspage().getPreGeneratedCuponCode();
		pageObj.utils().logit("selected coupon code is " + generatedCodeName);

		// user create
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		// add basket
		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "redemption_code", generatedCodeName);

		// verified that coupon name in add basket response
		String actual_discount_discount_id = discountBasketResponse.jsonPath()
				.getString("discount_basket_items.discount_id");
		actual_discount_discount_id = actual_discount_discount_id.replace("[", "").replace("]", "");
		Assert.assertEquals(actual_discount_discount_id, generatedCodeName);

		pageObj.utils().logPass("Verified that " + actual_discount_discount_id + " in add basket response");

		String actual_discount_type_addBasketResponse = discountBasketResponse.jsonPath()
				.getString("discount_basket_items.discount_type");

		Assert.assertEquals(actual_discount_type_addBasketResponse, "[redemption_code]");

		pageObj.utils().logPass("Verified the redemption_code in add basket response");

		String expdiscount_basket_item_id = discountBasketResponse.jsonPath()
				.getString("discount_basket_items.discount_basket_item_id").replace("[", "").replace("]", "");

		Response basketDiscountDetailsResponse = pageObj.endpoints().getDiscountBasketDetailsOfUsersAPIMobile(token,
				dataSet.get("client"), dataSet.get("secret"));

		String actualDiscountBasketItemIDFromBasket = basketDiscountDetailsResponse.jsonPath()
				.getString("discount_basket_items.discount_basket_item_id").replace("[", "").replace("]", "");
		String actualPromoCodeFromBasket = basketDiscountDetailsResponse.jsonPath()
				.getString("discount_basket_items.discount_id").replace("[", "").replace("]", "");

		Assert.assertEquals(actualDiscountBasketItemIDFromBasket, expdiscount_basket_item_id);
		Assert.assertEquals(actualPromoCodeFromBasket, generatedCodeName);

		expdiscount_basket_item_id = expdiscount_basket_item_id.replace("[", "").replace("]", "");
		Response deleteBasketResponse = pageObj.endpoints().deleteDiscountBasketForUserAPI(token, dataSet.get("client"),
				dataSet.get("secret"), expdiscount_basket_item_id);

		String delete_discount_basket_items = deleteBasketResponse.jsonPath().get("deleteBasketResponse");

		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, deleteBasketResponse.getStatusCode());
		Assert.assertEquals(delete_discount_basket_items, null);

		pageObj.utils().logPass("Verified that basket id " + expdiscount_basket_item_id + " is deleted successfully");

	}

	@Test(description = "OMM-T29 Verify API parameter validations for Promo Coupon added into discounts to basket ", groups = { "regression", "dailyrun" }, priority = 2)
	@Owner(name = "Shashank Sharma")
	public void AddingPromoCodeCuponToUserDiscountBasket() throws InterruptedException, ParseException {

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
		TestListeners.extentTest.get().pass("Coupon campaign created successfuly");

		// add basket redemption code in basket - promo code

		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "redemption_code", promoCode);

		Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		String actual_discount_discount_id = discountBasketResponse.jsonPath()
				.getString("discount_basket_items.discount_id").replace("[", "").replace("]", "");
		actual_discount_discount_id = actual_discount_discount_id.replace("[", "").replace("]", "");
		Assert.assertEquals(actual_discount_discount_id, promoCode);

		pageObj.utils().logPass("Verified that " + actual_discount_discount_id + " in add basket response");

		String actual_discount_type_addBasketResponse = discountBasketResponse.jsonPath()
				.getString("discount_basket_items.discount_type");

		Assert.assertEquals(actual_discount_type_addBasketResponse, "[redemption_code]");

		logger.info("Verified the redemption_code in add basket response");

		String expdiscount_basket_item_id = discountBasketResponse.jsonPath()
				.getString("discount_basket_items.discount_basket_item_id").replace("[", "").replace("]", "");

		Response basketDiscountDetailsResponse = pageObj.endpoints().getDiscountBasketDetailsOfUsersAPIMobile(token,
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
		Response deleteBasketResponse = pageObj.endpoints().deleteDiscountBasketForUserAPI(token, dataSet.get("client"),
				dataSet.get("secret"), expdiscount_basket_item_id);

		String delete_discount_basket_items = deleteBasketResponse.jsonPath().get("deleteBasketResponse");

		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, deleteBasketResponse.getStatusCode());
		Assert.assertEquals(delete_discount_basket_items, null);

		pageObj.utils().logPass("Verified that basket id " + expdiscount_basket_item_id + " is deleted successfully");

	}

	@Test(description = "OMM-T29 Verify API parameter validations for Subscription added into discountsm to basket ", groups = { "regression", "dailyrun" }, priority = 3)
	@Owner(name = "Shashank Sharma")
	public void subscriptionToAddInDiscountBucket() throws InterruptedException {
		boolean GlobalBenefitRedemptionThrottlingToggle = false;

		// create subscription plan
		String spName = "OMM SubcriptionPlan";
		// String QCname = "QcSubscription_" + CreateDateTime.getTimeDateString();
		// String amountcap = Integer.toString(Utilities.getRandomNoFromRange(10, 200));
		// String unitDiscount = Integer.toString(Utilities.getRandomNoFromRange(1,
		// 10));
		String spPrice = Integer.toString(Utilities.getRandomNoFromRange(300, 500));
		// int expSpPrice = Integer.parseInt(spPrice);

		// pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		//
		// pageObj.instanceDashboardPage().loginToInstance();
		// pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		//
		// pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification
		// Criteria");
		//
		// pageObj.qualificationcriteriapage().createQualificationCriteria(QCname,
		// amountcap,
		// dataSet.get("qcFucntionName"), unitDiscount, true,
		// dataSet.get("lineItemSelectorName"));
		//
		// pageObj.menupage().clickSubscriptionsMenuIcon();
		// pageObj.menupage().clickSubscriptionPlansLink();
		// pageObj.subscriptionPlansPage().createSubscriptionPlan(spName, spPrice,
		// QCname,
		// dataSet.get("qcFucntionName"),
		// GlobalBenefitRedemptionThrottlingToggle,endDateTime);
		// String PlanID =
		// pageObj.subscriptionPlansPage().getSbuscriptionPlanID(spName);
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
		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "subscription", subscription_id + "");
		Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		int actual_discount_discount_id = Integer.parseInt(discountBasketResponse.jsonPath()
				.getString("discount_basket_items.discount_id").replace("[", "").replace("]", ""));
		Assert.assertEquals(actual_discount_discount_id, subscription_id);

		pageObj.utils().logPass("Verified that " + actual_discount_discount_id + " in add basket response");

		String actual_discount_type_addBasketResponse = discountBasketResponse.jsonPath()
				.getString("discount_basket_items.discount_type");

		Assert.assertEquals(actual_discount_type_addBasketResponse, "[subscription]");

		pageObj.utils().logit("Verified the redemption_code in add basket response");

		int userSubscriptionID = Integer.parseInt(discountBasketResponse.jsonPath()
				.getString("discount_basket_items.discount_id").replace("[", "").replace("]", ""));

		Assert.assertEquals(userSubscriptionID, subscription_id, "User got the wrong subscription plan ");
		String expDiscountBasketID = discountBasketResponse.jsonPath()
				.getString("discount_basket_items.discount_basket_item_id").replace("[", "").replace("]", "");

		Response basketDiscountDetailsResponse = pageObj.endpoints().getDiscountBasketDetailsOfUsersAPIMobile(token,
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

		expDiscountBasketID = expDiscountBasketID.replace("[", "").replace("]", "");
		Response deleteBasketResponse = pageObj.endpoints().deleteDiscountBasketForUserAPI(token, dataSet.get("client"),
				dataSet.get("secret"), expDiscountBasketID);

		String delete_discount_basket_items = deleteBasketResponse.jsonPath().get("deleteBasketResponse");

		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, deleteBasketResponse.getStatusCode());
		Assert.assertEquals(delete_discount_basket_items, null);

		pageObj.utils().logPass("Verified that basket id " + expDiscountBasketID + " is deleted successfully");

	}

	@Test(description = "OMM-T29 Verify API parameter validations for discount amount added into discounts to basket ", groups = { "regression", "dailyrun" }, priority = 4)
	@Owner(name = "Shashank Sharma")
	public void addDiscountAmountToDiscountBasket() {
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
		Response discountBasketResponse = pageObj.endpoints().addDiscountAmountToDiscountBasket(token,
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

		pageObj.utils().logPass("User add the discount ammount to his discount basket successfully");

		Response basketDiscountDetailsResponse = pageObj.endpoints().getDiscountBasketDetailsOfUsersAPIMobile(token,
				dataSet.get("client"), dataSet.get("secret"));

		String actualDiscountBasketItemIDFromBasket = basketDiscountDetailsResponse.jsonPath()
				.getString("discount_basket_items.discount_basket_item_id").replace("[", "").replace("]", "");

		int expDiscountValue = Integer.parseInt(dataSet.get("discountAmount"));
		int actualDiscountValue = Integer.parseInt(basketDiscountDetailsResponse.jsonPath()
				.getString("discount_basket_items.discount_value").replace("[", "").replace("]", "").replace(".0", ""));

		Assert.assertEquals(actualDiscountValue, expDiscountValue);

		Assert.assertEquals(actualDiscountBasketItemIDFromBasket, expDiscountBasketID);

		expDiscountBasketID = expDiscountBasketID.replace("[", "").replace("]", "");
		Response deleteBasketResponse = pageObj.endpoints().deleteDiscountBasketForUserAPI(token, dataSet.get("client"),
				dataSet.get("secret"), expDiscountBasketID);
		String delete_discount_basket_items = deleteBasketResponse.jsonPath().get("deleteBasketResponse");

		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, deleteBasketResponse.getStatusCode());
		Assert.assertEquals(delete_discount_basket_items, null);

		pageObj.utils().logPass("Verified that basket id " + expDiscountBasketID + " is deleted successfully");

	}

	@Test(description = "OMM-T29 Verify API parameter validations for Fuel reward added into discounts to basket ", groups = { "regression", "dailyrun" }, priority = 5)
	@Owner(name = "Shashank Sharma")
	public void addFuelRewardToDiscountBasket() {
		System.out.println("Slug=" + dataSet.get("slug"));
		String fuelAmount = Utilities.getRandomNoFromRange(2, 8) + "";
		// create user
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();

		Response sendAmountResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "", "", "10",
				"");

		// Adding subscription into discount basket
		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
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
				.getString("discount_basket_items.discount_basket_item_id");

		Response basketDiscountDetailsResponse = pageObj.endpoints().getDiscountBasketDetailsOfUsersAPIMobile(token,
				dataSet.get("client"), dataSet.get("secret"));

		String actualDiscountDetailsResponse = basketDiscountDetailsResponse.jsonPath()
				.getString("discount_basket_items.discount_basket_item_id");

		Assert.assertEquals(actualDiscountDetailsResponse, expDiscountBasketItemId);

		expDiscountBasketItemId = expDiscountBasketItemId.replace("[", "").replace("]", "");
		Response deleteBasketResponse = pageObj.endpoints().deleteDiscountBasketForUserAPI(token, dataSet.get("client"),
				dataSet.get("secret"), expDiscountBasketItemId);
		String delete_discount_basket_items = deleteBasketResponse.jsonPath().get("deleteBasketResponse");

		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, deleteBasketResponse.getStatusCode());
		Assert.assertEquals(delete_discount_basket_items, null);

		pageObj.utils().logPass("Verified that basket id " + expDiscountBasketItemId + " is deleted successfully");

	}

	@Test(description = "OMM-T29 Verify API parameter validations for card completion added into discountsm to basket ", groups = {
			"regression", "unstable", "dailyrun" }, priority = 6)
	@Owner(name = "Shashank Sharma")
	public void addCardCompletionToUserDiscountBasket() {

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
		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
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
		Response deleteBasketResponse = pageObj.endpoints().deleteDiscountBasketForUserAPI(token, dataSet.get("client"),
				dataSet.get("secret"), expDiscountBasketItemId);
		String delete_discount_basket_items = deleteBasketResponse.jsonPath().get("deleteBasketResponse");

		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, deleteBasketResponse.getStatusCode());
		Assert.assertEquals(delete_discount_basket_items, null);

		pageObj.utils().logPass("Verified that basket id " + expDiscountBasketItemId + " is deleted successfully");

	}

	@Test(description = "SQ-T3887-- OMM-T731 Validate that in {{path}}/api/mobile/discounts/unselect?client={{client}}&hash={{span}} API, datatype of the discount_item_ids is supported as array  \"discount_item_ids\": [\"265949\", \"265953\"]", groups = { "regression", "dailyrun" }, priority = 7)
	@Owner(name = "Shashank Sharma")
	public void T3887_AddRewardDiscountToBasketNew() throws InterruptedException {

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
				dataSet.get("redeemable_id"), "", "");

		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				dataSet.get("redeemable_id"), "", "");

		Response rewardResponse = pageObj.endpoints().authListAvailableRewardsNew(token, dataSet.get("client"),
				dataSet.get("secret"));

		String rewardID = rewardResponse.jsonPath().getString("id[0]").replace("[", "").replace("]", "");

		String rewardID2 = rewardResponse.jsonPath().getString("id[1]").replace("[", "").replace("]", "");

		pageObj.utils().logit("Reward id " + rewardID + " is generated successfully ");

		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID);
		String expdiscount_basket_item_id = discountBasketResponse.jsonPath()
				.getString("discount_basket_items.discount_basket_item_id").replace("[", "").replace("]", "");

		Response discountBasketResponse2 = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID2);

		String expdiscount_basket_item_id2 = discountBasketResponse2.jsonPath()
				.getString("discount_basket_items.discount_basket_item_id[1]").replace("[", "").replace("]", "");

		String allBasketIDArray = expdiscount_basket_item_id + "\",\"" + expdiscount_basket_item_id2;

		pageObj.utils().logPass("Verified the discount type and ID in Basket add response");

		Response deleteBasketResponse = pageObj.endpoints().deleteDiscountBasketForUserAPI(token, dataSet.get("client"),
				dataSet.get("secret"), allBasketIDArray);

		String delete_discount_basket_items = deleteBasketResponse.jsonPath().get("deleteBasketResponse");

		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, deleteBasketResponse.getStatusCode());
		Assert.assertEquals(delete_discount_basket_items, null);

		pageObj.utils().logPass("Verified that basket is deleted successfully");

	}

	// @Test(priority = -1)
	public void ommPrerequisite() throws InterruptedException {
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
			TestListeners.extentTest.get().pass("Flags are updated for the business: " + str);
		}

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}
}
