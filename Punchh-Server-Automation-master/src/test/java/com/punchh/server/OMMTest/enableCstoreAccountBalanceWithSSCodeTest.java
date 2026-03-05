package com.punchh.server.OMMTest;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
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
public class enableCstoreAccountBalanceWithSSCodeTest {
	private static Logger logger = LogManager.getLogger(enableCstoreAccountBalanceWithSSCodeTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String sTCName;
	private String baseUrl;
	private String env, run = "ui";
	private static Map<String, String> dataSet;
	private String endDateTime;

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
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
		endDateTime = CreateDateTime.getFutureDate(1) + " 10:00 PM";
	}

	@Test(description = "SQ-T3664 Verify response of User Search API when enable_cstore_account_balance is true and lookup field is single_scan_code", groups = {
			"regression", "dailyrun" }, priority = 1)
	@Owner(name = "Hardik Bhardwaj")
	public void T3664_UserSearchAPIwithSSO() throws InterruptedException {
		// User SignUp using API
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String firstName = signUpResponse.jsonPath().get("user.first_name").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("API2 Signup is successful");
		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Click Cockpit
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Gift Cards");
		pageObj.giftcardsPage().selectGiftCardAdapter("Mock Gift Card Adapter");
		pageObj.giftcardsPage().setMinMaxAmountGiftCard("0", "0");
		pageObj.giftcardsPage().selectPaymentAdapter("Braintree");

		Response purchaseGiftCardResponse = pageObj.endpoints().Api2PurchaseGiftCard(dataSet.get("client"),
				dataSet.get("secret"), token, dataSet.get("design_id"), dataSet.get("amount"), dataSet.get("expDate"),
				firstName);
		Assert.assertEquals(purchaseGiftCardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 purchase gift card");
		pageObj.utils().logPass("Api2 Purchase Gift Card is successful ");
		String uuidNumber = purchaseGiftCardResponse.jsonPath().getString("uuid");
		pageObj.utils().logit("Gift card UUID is " + uuidNumber);

		Response userTokenGenerateResponse = pageObj.endpoints().ssoUserTokenMobileApi(dataSet.get("client"),
				dataSet.get("secret"), token, "", "GiftCard", "", uuidNumber, "", "", "");
		Assert.assertEquals(userTokenGenerateResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Mobile user user token generate ");
		pageObj.utils().logPass("Mobile user user token generate is successful");
		String singleScanCode = userTokenGenerateResponse.jsonPath().getString("single_scan_code");
		pageObj.utils().logit("Mobile single scan code is " + singleScanCode);

		Response balanceResponse = pageObj.endpoints().posUserLookupSingleScanToken(singleScanCode,
				dataSet.get("locationKey"));
		Assert.assertEquals(balanceResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Error in getting user balance");

		String jsonObjectString = balanceResponse.asString();
		JSONObject finalResponse = new JSONObject(jsonObjectString);

		Boolean addressLine = finalResponse.has("address_line1"); // will return false
		Assert.assertEquals(true, addressLine, "Address line not found");
		pageObj.utils().logPass("Address line found");

		Boolean anniversary = finalResponse.has("anniversary");
		Assert.assertEquals(true, anniversary, "anniversary not found");
		pageObj.utils().logPass("anniversary found");

		Boolean avatar_remote_url = finalResponse.has("avatar_remote_url");
		Assert.assertEquals(true, avatar_remote_url, "avatar_remote_url not found");
		pageObj.utils().logPass("avatar_remote_url found");

		Boolean birthday = finalResponse.has("birthday");
		Assert.assertEquals(true, birthday, "birthday not found");
		pageObj.utils().logPass("birthday found");

		Boolean city = finalResponse.has("city");
		Assert.assertEquals(true, city, "city not found");
		pageObj.utils().logPass("city found");

		Boolean created_at = finalResponse.has("created_at");
		Assert.assertEquals(true, created_at, "created_at not found");
		pageObj.utils().logPass("created_at found");

		Boolean email = finalResponse.has("email");
		Assert.assertEquals(true, email, "email not found");
		pageObj.utils().logPass("email found");

		Boolean email_verified = finalResponse.has("email_verified");
		Assert.assertEquals(true, email_verified, "email_verified not found");
		pageObj.utils().logPass("email_verified found");

		Boolean fb_uid = finalResponse.has("fb_uid");
		Assert.assertEquals(true, fb_uid, "fb_uid not found");
		pageObj.utils().logPass("fb_uid found");

		Boolean first_name = finalResponse.has("first_name");
		Assert.assertEquals(true, first_name, "first_name not found");
		pageObj.utils().logPass("first_name found");

		Boolean age_verified = finalResponse.has("age_verified");
		Assert.assertEquals(true, age_verified, "age_verified not found");
		pageObj.utils().logPass("age_verified found");

		Boolean privacy_policy = finalResponse.has("privacy_policy");
		Assert.assertEquals(true, privacy_policy, "privacy_policy not found");
		pageObj.utils().logPass("privacy_policy found");

		Boolean gender = finalResponse.has("gender");
		Assert.assertEquals(true, gender, "gender not found");
		pageObj.utils().logPass("gender found");

		Boolean id = finalResponse.has("id");
		Assert.assertEquals(true, id, "id not found");
		pageObj.utils().logPass("id found");

		Boolean last_name = finalResponse.has("last_name");
		Assert.assertEquals(true, last_name, "last_name not found");
		pageObj.utils().logPass("last_name found");

		Boolean state = finalResponse.has("state");
		Assert.assertEquals(true, state, "state not found");
		pageObj.utils().logPass("state found");

		Boolean updated_at = finalResponse.has("updated_at");
		Assert.assertEquals(true, updated_at, "updated_at not found");
		pageObj.utils().logPass("updated_at found");

		Boolean zip_code = finalResponse.has("zip_code");
		Assert.assertEquals(true, zip_code, "zip_code not found");
		pageObj.utils().logPass("zip_code found");

		Boolean test_user = finalResponse.has("test_user");
		Assert.assertEquals(true, test_user, "test_user not found");
		pageObj.utils().logPass("test_user found");

		Boolean user_joined_at = finalResponse.has("user_joined_at");
		Assert.assertEquals(true, user_joined_at, "user_joined_at not found");
		pageObj.utils().logPass("user_joined_at found");

		Boolean balance = finalResponse.has("balance");
		Assert.assertEquals(true, balance, "balance not found");
		pageObj.utils().logPass("balance found");

		Boolean selected_card_number = finalResponse.has("selected_card_number");
		Assert.assertEquals(true, selected_card_number, "selected_card_number not found");
		pageObj.utils().logPass("selected_card_number found");

		Boolean selected_rewards = finalResponse.has("selected_rewards");
		Assert.assertEquals(true, selected_rewards, "selected_rewards not found");
		pageObj.utils().logPass("selected_rewards found");

		Boolean selected_coupons = finalResponse.has("selected_coupons");
		Assert.assertEquals(true, selected_coupons, "selected_coupons not found");
		pageObj.utils().logPass("selected_coupons found");

		Boolean converted_category_balances = finalResponse.has("converted_category_balances");
		Assert.assertEquals(true, converted_category_balances, "converted_category_balances not found");
		pageObj.utils().logPass("converted_category_balances found");

		Boolean payment_mode = finalResponse.has("payment_mode");
		Assert.assertEquals(true, payment_mode, "payment_mode not found");
		pageObj.utils().logPass("payment_mode found");

		Boolean selected_tip_amount = finalResponse.has("selected_tip_amount");
		Assert.assertEquals(true, selected_tip_amount, "selected_tip_amount not found");
		pageObj.utils().logPass("selected_tip_amount found");

		Boolean phone = finalResponse.has("phone");
		Assert.assertEquals(true, phone, "phone not found");
		pageObj.utils().logPass("phone found");
	}

	@Test(description = "SQ-T3663 Verify response of User Search API when enable_cstore_account_balance is true and lookup field is email", groups = {"regression", "dailyrun"}, priority = 0)
	@Owner(name = "Hardik Bhardwaj")
	public void T3663_UserSearchAPIwithEmail() throws Exception {

		String b_id = dataSet.get("business_id");

		String preferenceQuery = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id
				+ "'";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, preferenceQuery, "preferences");

		// updating the business preference for business live
		DBUtils.updateBusinessFlag(env, expColValue, "false", "live", b_id);

		// updating the business preference for went_live
		DBUtils.updateBusinessFlag(env, expColValue, "false", "went_live", b_id);

		// updating the business preference for track_points_spent
		DBUtils.updateBusinessFlag(env, expColValue, "true", "enable_cstore_account_balance", b_id);

		// updating the business preference for enable_api_parameterization
		DBUtils.updateBusinessFlag(env, expColValue, "false", "enable_api_parameterization", b_id);

		// User SignUp using API
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("API2 Signup is successful");

		String spPrice = Integer.toString(Utilities.getRandomNoFromRange(300, 500));
		String PlanID = dataSet.get("PlanID");
		// subscription purchase api 2
		Response purchaseSubscriptionresponse = pageObj.endpoints().Api2SubscriptionPurchase(token, PlanID,
				dataSet.get("client"), dataSet.get("secret"), spPrice, endDateTime);
		Assert.assertEquals(purchaseSubscriptionresponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		String subscription_id = purchaseSubscriptionresponse.jsonPath().get("subscription_id").toString();
		pageObj.utils().logit(userEmail + " purchased " + subscription_id + " Plan id = " + PlanID);

		Response balanceResponse = pageObj.endpoints().posUserLookupFetchBalance(userEmail, dataSet.get("locationKey"));
		Assert.assertEquals(balanceResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Error in getting user balance");
		pageObj.utils().logPass("POS user search is successful");

		String jsonObjectString = balanceResponse.asString();
		JSONObject finalResponse = new JSONObject(jsonObjectString);

		Boolean user_id = finalResponse.has("user_id"); // will return true
		Assert.assertEquals(true, user_id, "user_id not found");
		pageObj.utils().logPass("user_id found");

		Boolean converted_category_balances = finalResponse.has("converted_category_balances");
		Assert.assertEquals(true, converted_category_balances, "converted_category_balances not found");
		pageObj.utils().logPass("converted_category_balances found");

		Boolean converted_category_balances1_name = finalResponse.getJSONArray("converted_category_balances")
				.getJSONObject(0).has("name");
		Assert.assertEquals(true, converted_category_balances1_name, "converted_category_balances name not found");
		pageObj.utils().logPass("converted_category_balances name found");

		Boolean converted_category_balances1_balance = finalResponse.getJSONArray("converted_category_balances")
				.getJSONObject(0).has("balance");
		Assert.assertEquals(true, converted_category_balances1_balance,
				"converted_category_balances balance not found");
		pageObj.utils().logPass("converted_category_balances balance found");

		Boolean converted_category_balances1_DiscountType = finalResponse.getJSONArray("converted_category_balances")
				.getJSONObject(0).has("discount_type");
		Assert.assertEquals(true, converted_category_balances1_DiscountType,
				"converted_category_balances Discount type not found");
		pageObj.utils().logPass("converted_category_balances Discount type found");

//		Boolean converted_category_balances2_name = finalResponse.getJSONArray("converted_category_balances")
//				.getJSONObject(1).has("name");
//		Assert.assertEquals(true, converted_category_balances2_name, "converted_category_balances name not found");
//		pageObj.utils().logPass("converted_category_balances name found");
//
//		Boolean converted_category_balances2_balance = finalResponse.getJSONArray("converted_category_balances")
//				.getJSONObject(1).has("balance");
//		Assert.assertEquals(true, converted_category_balances2_balance,
//				"converted_category_balances balance not found");
//		pageObj.utils().logPass("converted_category_balances balance found");
//
//		Boolean converted_category_balances2_DiscountType = finalResponse.getJSONArray("converted_category_balances")
//				.getJSONObject(1).has("discount_type");
//		Assert.assertEquals(true, converted_category_balances2_DiscountType,
//				"converted_category_balances Discount type not found");
//		pageObj.utils().logPass("converted_category_balances Discount type found");
//
//		Boolean converted_category_balances3_name = finalResponse.getJSONArray("converted_category_balances")
//				.getJSONObject(2).has("name");
//		Assert.assertEquals(true, converted_category_balances3_name, "converted_category_balances name not found");
//		pageObj.utils().logPass("converted_category_balances name found");
//
//		Boolean converted_category_balances3_balance = finalResponse.getJSONArray("converted_category_balances")
//				.getJSONObject(2).has("balance");
//		Assert.assertEquals(true, converted_category_balances3_balance,
//				"converted_category_balances balance not found");
//		pageObj.utils().logPass("converted_category_balances balance found");
//
//		Boolean converted_category_balances3_DiscountType = finalResponse.getJSONArray("converted_category_balances")
//				.getJSONObject(2).has("discount_type");
//		Assert.assertEquals(true, converted_category_balances3_DiscountType,
//				"converted_category_balances Discount type not found");
//		pageObj.utils().logPass("converted_category_balances Discount type found");

		Boolean created_at = finalResponse.has("created_at"); // will return false
		Assert.assertEquals(true, created_at, "created_at not found");
		pageObj.utils().logPass("created_at found");

		Boolean first_name = finalResponse.has("first_name"); // will return false
		Assert.assertEquals(true, first_name, "first_name not found");
		pageObj.utils().logPass("first_name found");

		Boolean fuel_reward_locked = finalResponse.has("fuel_reward_locked"); // will return false
		Assert.assertEquals(true, fuel_reward_locked, "fuel_reward_locked not found");
		pageObj.utils().logPass("fuel_reward_locked found");

		Boolean last_name = finalResponse.has("last_name"); // will return false
		Assert.assertEquals(true, last_name, "last_name not found");
		pageObj.utils().logPass("last_name found");

		Boolean phone = finalResponse.has("phone"); // will return false
		Assert.assertEquals(true, phone, "phone not found");
		pageObj.utils().logPass("phone found");

		Boolean updated_at = finalResponse.has("updated_at"); // will return false
		Assert.assertEquals(true, updated_at, "updated_at not found");
		pageObj.utils().logPass("updated_at found");

		Boolean subscriptions = finalResponse.has("subscriptions"); // will return false
		Assert.assertEquals(true, subscriptions, "subscriptions not found");
		pageObj.utils().logPass("subscriptions found");

		Boolean plan_name = finalResponse.getJSONArray("subscriptions").getJSONObject(0).has("plan_name");
		Assert.assertEquals(true, plan_name, "subscriptions plan_name not found");
		pageObj.utils().logPass("subscriptions plan_name found");

		Boolean pos_meta = finalResponse.getJSONArray("subscriptions").getJSONObject(0).has("pos_meta");
		Assert.assertEquals(true, pos_meta, "subscriptions pos_meta not found");
		pageObj.utils().logPass("subscriptions pos_meta found");

		Boolean subscription_id1 = finalResponse.getJSONArray("subscriptions").getJSONObject(0).has("subscription_id");
		Assert.assertEquals(true, subscription_id1, "subscriptions subscription_id not found");
		pageObj.utils().logPass("subscriptions subscription_id found");
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}
}
