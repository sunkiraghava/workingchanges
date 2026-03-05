package com.punchh.server.OMMTest;

import java.lang.reflect.Method;
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

import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;

import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

/*
 * @Author :- Ashwini Shetty
 * 
 * TC - OMM-T77
*/

@Listeners(TestListeners.class)
public class VerifyInvalidRewardAndQualifiedOMM_T266 {
	static Logger logger = LogManager.getLogger(VerifyInvalidRewardAndQualifiedOMM_T266.class);
	public WebDriver driver;
	private Properties prop;
	private PageObj pageObj;
	private String sTCName;
	private String env, run = "ui";
	private String baseUrl;
	private static Map<String, String> dataSet;
	private String userEmail;
	boolean enableMenuItemAggregatorFlag;

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
	}

	@Test(description = "SQ-T3443 Validate that API is not returning 500 error when user is trying to get 'Invalid Reward' using discount lookup API and 'Set Processing order' is set to 'Date of Expiry', SQ-T3442 Validate that If user has added reward into discount_basket_item table and then reward has been force redeemed then also qualified-\"false\" should get displayed in selected discounts of API response", priority = 0)
	public void OMMT232_verifyInvalidRewardAndQualified() throws InterruptedException {

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
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse1.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get()
				.pass("Api2  send reward " + dataSet.get("redeemable_id") + " to user is successful");

		Response rewardResponse = pageObj.endpoints().authListAvailableRewardsNew(token, dataSet.get("client"),
				dataSet.get("secret"));

		String rewardID1 = rewardResponse.jsonPath().getString("id[0]").replace("[", "").replace("]", "");

		pageObj.utils().logPass("Reward id " + rewardID1 + "is generated successfully ");

		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID1);
		pageObj.utils().logPass(rewardID1 + " rewardid is added to the basket ");

//		// Force Redeem
//		Response forceRedeemResponse = pageObj.endpoints().forceRedeem(adminAuthorization, rewardID1, userID);
//		Assert.assertEquals(forceRedeemResponse.getStatusCode(), 201,
//				"Status code 201 did not matched for Platform Api Force Redeem");
//		TestListeners.extentTest.get().pass("PLATFORM FUNCTIONS API Force Redeem is successful");

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.forceredemptionPage().clickForceRedemptionBtn();
		pageObj.forceredemptionPage().forceRedemptionreward(dataSet.get("comment"), dataSet.get("redeemable"));
		boolean forceRedemptionRewardStatus = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(forceRedemptionRewardStatus, "Force redemption success message did not displayed");
		TestListeners.extentTest.get().pass("Force redemption of reward is done successfully");

		Thread.sleep(20000);

		Response discountLookupResponse = pageObj.endpoints().processDiscountLookup(dataSet.get("locationKey"), userID,
				"15", "8", "7", "3");

		System.out.println("discountLookupResponse=" + discountLookupResponse.asPrettyString());

		String verifyInvalidRewardIdMessage = discountLookupResponse.jsonPath().getString("selected_discounts.message");

		pageObj.utils().logPass(verifyInvalidRewardIdMessage + " Invalid Reward id message is displayed");
		System.out.println(verifyInvalidRewardIdMessage);
		String verifyQualifiedMessage = discountLookupResponse.jsonPath().getString("selected_discounts.qualified");
		pageObj.utils().logPass(verifyQualifiedMessage + " Qualified message as False is displayed");
	}

	@Test(description = "SQ-T3441 Validate that If user_id sent in API has an active basket but it is empty then “API Response -> 200", priority = 1)
	public void OMMT217_verifyEmptyActiveBasket() throws InterruptedException {
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
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse1.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get()
				.pass("Api2  send reward " + dataSet.get("redeemable_id") + " to user is successful");

		Response rewardResponse = pageObj.endpoints().authListAvailableRewardsNew(token, dataSet.get("client"),
				dataSet.get("secret"));

		String rewardID1 = rewardResponse.jsonPath().getString("id[0]").replace("[", "").replace("]", "");

		pageObj.utils().logPass("Reward id " + rewardID1 + "is generated successfully ");

		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID1);
		pageObj.utils().logPass(rewardID1 + " rewardid is added to the basket ");
		String expdiscount_basket_item_id = discountBasketResponse.jsonPath()
				.getString("discount_basket_items.discount_basket_item_id").replace("[", "").replace("]", "");

		// delete the reward
		Response deleteBasketResponse = pageObj.endpoints().deleteDiscountBasketForUserAPI(token, dataSet.get("client"),
				dataSet.get("secret"), expdiscount_basket_item_id);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, deleteBasketResponse.getStatusCode(), "Status code 200 did not match");

		Thread.sleep(20000);

		Response discountLookupResponse = pageObj.endpoints().processDiscountLookup(dataSet.get("locationKey"), userID,
				"15", "8", "7", "3");

		System.out.println("discountLookupResponse=" + discountLookupResponse.asPrettyString());
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountLookupResponse.getStatusCode(), "Status code 200 did not match");
	}

	@Test(description = "SQ-T3439 Validate that discount items(D item) are not getting displayed in qualified menu items", priority = 2)
	public void OMMT215_verifyQualifiedItem() throws InterruptedException {
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
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse1.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get()
				.pass("Api2  send reward " + dataSet.get("redeemable_id") + " to user is successful");

		Response rewardResponse = pageObj.endpoints().authListAvailableRewardsNew(token, dataSet.get("client"),
				dataSet.get("secret"));

		String rewardID1 = rewardResponse.jsonPath().getString("id[0]").replace("[", "").replace("]", "");

		pageObj.utils().logPass("Reward id " + rewardID1 + "is generated successfully ");

		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID1);
		pageObj.utils().logPass(rewardID1 + " rewardid is added to the basket ");

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.forceredemptionPage().clickForceRedemptionBtn();
		pageObj.forceredemptionPage().forceRedemptionreward(dataSet.get("comment"), dataSet.get("redeemable"));
		boolean forceRedemptionRewardStatus = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(forceRedemptionRewardStatus, "Force redemption success message did not displayed");
		TestListeners.extentTest.get().pass("Force redemption of reward is done successfully");

		Thread.sleep(10000);

		Response discountLookupResponse = pageObj.endpoints().processDiscountLookup(dataSet.get("locationKey"), userID,
				"15", "8", "7", "3");
		System.out.println("discountLookupResponse=" + discountLookupResponse.asPrettyString());
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountLookupResponse.getStatusCode(), "Status code 200 did not match");
		String verifyQualifiedItems = discountLookupResponse.jsonPath().getString("selected_discounts.qualified_items");
		pageObj.utils().logPass(verifyQualifiedItems + " Qualified items for discount is empty");
	}

	@Test(description = "SQ-T3438 Validate that there is no ordering in unselected discounts i.e. it first return all the rewards and then coupons.", priority = 3)
	public void OMMT212_verifyNoOrdering() throws InterruptedException {
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
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse1.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get()
				.pass("Api2  send reward " + dataSet.get("redeemable_id") + " to user is successful");

		Response rewardResponse = pageObj.endpoints().authListAvailableRewardsNew(token, dataSet.get("client"),
				dataSet.get("secret"));

		String rewardID1 = rewardResponse.jsonPath().getString("id[0]").replace("[", "").replace("]", "");

		pageObj.utils().logPass("Reward id " + rewardID1 + "is generated successfully ");

		// coupon to the user
		String campaign_uuid = dataSet.get("uuid");
		Response postDynamicCouponResponse = pageObj.endpoints().postDynamicCoupon1(dataSet.get("adminAuthorization"),
				userEmail, campaign_uuid);
		Assert.assertEquals(postDynamicCouponResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Failed to verify Dynamic Coupon Distribution API response");

		Response discountLookupResponse = pageObj.endpoints().processDiscountLookup(dataSet.get("locationKey"), userID,
				"15", "8", "7", "3");
		System.out.println("discountLookupResponse=" + discountLookupResponse.asPrettyString());
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountLookupResponse.getStatusCode(), "Status code 200 did not match");
		TestListeners.extentTest.get()
				.pass("Verified that unselected discounts first returns all the rewards and then coupons.");
	}

	@Test(description = "SQ-T3440 Validate that expired/honored rewards do not get displayed in unselected discounts list", priority = 3)
	public void OMMT216_verifyExpiredRewards() throws InterruptedException {
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
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse1.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get()
				.pass("Api2  send reward " + dataSet.get("redeemable_id") + " to user is successful");

		// send an expired reward
		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				dataSet.get("redeemable_id1"), "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse2.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get()
				.pass("Api2  send reward " + dataSet.get("redeemable_id1") + " to user is successful");

		Response discountLookupResponse = pageObj.endpoints().processDiscountLookup(dataSet.get("locationKey"), userID,
				"15", "8", "7", "3");
		System.out.println("discountLookupResponse=" + discountLookupResponse.asPrettyString());
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountLookupResponse.getStatusCode(), "Status code 200 did not match");
		TestListeners.extentTest.get()
				.pass("Verified that expired/honored rewards do not get displayed in unselected discounts list");

	}

	@Test(description = "SQ-T3437 Validate that sequencing in selected discount is based on business rules configured in Cockpit->Multiple redemptions tab", priority = 6)
	public void OMMT211_verifySequencingInSelectedDiscount() throws InterruptedException {
		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("API2 Signup is successful");

		// configurations in the multiple redemption tab
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.forceredemptionPage().cockpitMultipleRedemption();

		// send reward amount to user Redeemable
		Response sendRewardResponse1 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				dataSet.get("redeemable_id"), "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse1.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get()
				.pass("Api2  send reward " + dataSet.get("redeemable_id") + " to user is successful");

		Response rewardResponse = pageObj.endpoints().authListAvailableRewardsNew(token, dataSet.get("client"),
				dataSet.get("secret"));

		String rewardID1 = rewardResponse.jsonPath().getString("id[0]").replace("[", "").replace("]", "");

		pageObj.utils().logPass("Reward id " + rewardID1 + "is generated successfully ");

		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID1);
		pageObj.utils().logPass(rewardID1 + " rewardid is added to the basket ");

		// coupon to the user
		String campaign_uuid = dataSet.get("uuid");
		Response postDynamicCouponResponse = pageObj.endpoints().postDynamicCoupon1(dataSet.get("adminAuthorization"),
				userEmail, campaign_uuid);
		Assert.assertEquals(postDynamicCouponResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Failed to verify Dynamic Coupon Distribution API response");
		String couponCode = postDynamicCouponResponse.jsonPath().get("coupon").toString();

		Response discountBasketResponse1 = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "redemption_code", couponCode);
		pageObj.utils().logPass(couponCode + " coupon code is added to the basket ");

		Response discountLookupResponse = pageObj.endpoints().processDiscountLookup(dataSet.get("locationKey"), userID,
				"15", "8", "7", "3");
		System.out.println("discountLookupResponse=" + discountLookupResponse.asPrettyString());
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountLookupResponse.getStatusCode(), "Status code 200 did not match");

	}

	@Test(description = "SQ-T3140 Validate that api returns selected discounts(rewards,redeemable,redemption_code) and unselected discounts(rewards,redeemable,POS dynamic code assigned to user)", priority = 5)
	public void OMMT206_verifyLookupResponse() throws InterruptedException {

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
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse1.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get()
				.pass("Api2  send reward " + dataSet.get("redeemable_id") + " to user is successful");

		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				dataSet.get("redeemable_id1"), "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse2.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get()
				.pass("Api2  send reward " + dataSet.get("redeemable_id1") + " to user is successful");

		Response rewardResponse = pageObj.endpoints().authListAvailableRewardsNew(token, dataSet.get("client"),
				dataSet.get("secret"));

		String rewardID1 = rewardResponse.jsonPath().getString("id[0]").replace("[", "").replace("]", "");
		String rewardID2 = rewardResponse.jsonPath().getString("id[1]").replace("[", "").replace("]", "");

		pageObj.utils().logPass("Reward id " + rewardID1 + "Reward id " + rewardID2 + "is generated successfully ");

		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID1);
		pageObj.utils().logPass(rewardID1 + " rewardid is added to the basket ");

		// coupon to the user
		String campaign_uuid1 = dataSet.get("uuid");
		Response postDynamicCouponResponse1 = pageObj.endpoints().postDynamicCoupon1(dataSet.get("adminAuthorization"),
				userEmail, campaign_uuid1);
		Assert.assertEquals(postDynamicCouponResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Failed to verify Dynamic Coupon Distribution API response");
		String couponCode = postDynamicCouponResponse1.jsonPath().get("coupon").toString();

		Response discountBasketResponse1 = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "redemption_code", couponCode);
		pageObj.utils().logPass(couponCode + " coupon code is added to the basket ");

		Response discountLookupResponse = pageObj.endpoints().processDiscountLookup(dataSet.get("locationKey"), userID,
				"15", "8", "7", "3");
		System.out.println("discountLookupResponse=" + discountLookupResponse.asPrettyString());
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountLookupResponse.getStatusCode(), "Status code 200 did not match");

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}
}
