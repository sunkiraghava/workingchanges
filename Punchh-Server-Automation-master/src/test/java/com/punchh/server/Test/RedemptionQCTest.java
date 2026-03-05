package com.punchh.server.Test;

import java.lang.reflect.Method;
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
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class RedemptionQCTest {

	private static Logger logger = LogManager.getLogger(RedemptionQCTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String iFrameEmail;
	private String sTCName;
	private String env, run = "ui";
	private String baseUrl;
	private static Map<String, String> dataSet;

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
		logger.info(sTCName + " ==>" + dataSet);
	}

	@SuppressWarnings("unused")
	@Test(description = "SQ-T2268 Verify Free Gift type Redeemable with POS API", groups = { "regression",
			"dailyrun" }, priority = 0)
	@Owner(name = "Amit Kumar")
	public void T2268_verifyFreeGifttypeRedeemablewithPOSAPI() throws InterruptedException {
		String reward_Code = "";
		// User Signip using mobile api 2
		iFrameEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(iFrameEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Instance login and goto timeline
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.instanceDashboardPage().navigateToGuestTimeline(iFrameEmail);

		// click message gift and gift reward to user
		pageObj.guestTimelinePage().messageGiftToUser(dataSet.get("subject"), dataSet.get("reward"),
				dataSet.get("redeemable"), dataSet.get("giftReason"));
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "Message sent did not displayed on timeline");

		// iFrame Login
		pageObj.iframeSingUpPage().navigateToIframe(baseUrl + dataSet.get("whiteLabel") + dataSet.get("slug"));
		pageObj.iframeSingUpPage().iframeLogin(iFrameEmail);
		reward_Code = pageObj.iframeSingUpPage().redeemRewardOffer(dataSet.get("rewardName"));

		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key = CreateDateTime.getTimeDateString();
		// Pos redemption api
		Response resp = pageObj.endpoints().posRedemptionOfFreeGift(iFrameEmail, date, reward_Code, key, txn,
				dataSet.get("locationKey"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp.getStatusCode(), "Status code 200 did not matched for pos redemption api");
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().navigateToGuestTimeline(iFrameEmail);

		pageObj.guestTimelinePage().refreshTimeline();

		// Validate timeline for redemption and receipt
		String redeemedRedemption = pageObj.guestTimelinePage().redeemedRedemption(reward_Code);
		String discountValueWebCheckin = pageObj.guestTimelinePage().getDiscountValueWebCheckinDetails();
		String totalamoutnWebCheckin = pageObj.guestTimelinePage().getTotalAmountWebCheckin();
		pageObj.guestTimelinePage().refreshTimeline();

		String discountValuePosCheckin = pageObj.guestTimelinePage().getDiscountValuePosCheckinDetails();
		String discountedAmount = pageObj.guestTimelinePage().getDiscountedAmountPosCheckinDetails();

		pageObj.guestTimelinePage().clickAccountHistory();
		List<String> Itemdata = pageObj.accounthistoryPage().getAccountDetailsforItemRedeemed();
		System.out.println(Itemdata);
		Assert.assertTrue(Itemdata.get(0).contains("Item Redeemed"), "Redemption did not redeemed in account history");
		Assert.assertTrue(Itemdata.get(0).contains("Free Coffee redeemed"),
				"Redemption did not redeemed in account history");
		// Assert.assertTrue(Itemdata.get(1).contains("0 Items"), "reward item did not
		// decreased in account balance");
		pageObj.utils().logPass("Redemption of reward is validated in acount history");
		pageObj.guestTimelinePage().clickRewards();
		String honoredreward = pageObj.guestTimelinePage().getHonored();
		Assert.assertTrue(honoredreward.contains(dataSet.get("redeemable")),
				"Redeemed reward name did not matched in honored list");
		pageObj.utils().logPass("Redemption of reward is validated in honored list");

	}

	@SuppressWarnings("unused")
	@Test(description = "SQ-T2177 Verify Bogo type Redeemable with POS API", groups = { "regression",
			"dailyrun" }, priority = 1)
	@Owner(name = "Amit Kumar")
	public void T2177_verifyBogotypeRedeemablewithPOSAPI() throws InterruptedException {
		String reward_Code = "";
		// User Signip using mobile api 2
		iFrameEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(iFrameEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Instance login and goto timeline
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.instanceDashboardPage().navigateToGuestTimeline(iFrameEmail);

		// click message gift and gift reward to user
		pageObj.guestTimelinePage().messageGiftToUser(dataSet.get("subject"), dataSet.get("reward"),
				dataSet.get("redeemable"), dataSet.get("giftReason"));
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "Message sent did not displayed on timeline");

		// iFrame Login
		pageObj.iframeSingUpPage().navigateToIframe(baseUrl + dataSet.get("whiteLabel") + dataSet.get("slug"));
		pageObj.iframeSingUpPage().iframeLogin(iFrameEmail);
		reward_Code = pageObj.iframeSingUpPage().redeemRewardOffer(dataSet.get("rewardName"));

		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key = CreateDateTime.getTimeDateString();
		// Pos redemption api
		Response resp = pageObj.endpoints().posRedemptionOfBogoGift(iFrameEmail, date, reward_Code, key, txn,
				dataSet.get("locationKey"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp.getStatusCode(), "Status code 200 did not matched for pos redemption api");
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().navigateToGuestTimeline(iFrameEmail);

		pageObj.guestTimelinePage().refreshTimeline();
		Thread.sleep(5000);
		// Validate timeline for redemption and receipt
		String redeemedRedemption = pageObj.guestTimelinePage().redeemedRedemption(reward_Code);
		String discountValueWebCheckin = pageObj.guestTimelinePage().getDiscountValueWebCheckinDetails();
		String totalamoutnWebCheckin = pageObj.guestTimelinePage().getTotalAmountWebCheckin();
		pageObj.guestTimelinePage().refreshTimeline();
		Thread.sleep(5000);
		String discountValuePosCheckin = pageObj.guestTimelinePage().getDiscountValuePosCheckinDetails();
		String discountedAmount = pageObj.guestTimelinePage().getDiscountedAmountPosCheckinDetails();

		pageObj.guestTimelinePage().clickAccountHistory();
		List<String> Itemdata = pageObj.accounthistoryPage().getAccountDetailsforItemRedeemed();
		System.out.println(Itemdata);
		Assert.assertTrue(
				Itemdata.get(0).contains(
						"Item Redeemed\n" + "Sandwich (buy 1 get 1) redeemed using redemption code " + reward_Code),
				"Redemption did not redeemed in account history");
		// Assert.assertTrue(Itemdata.get(1).contains("0 Items"), "reward item did not
		// decreased in account balance");
		pageObj.utils().logPass("Redemption of reward is validated in acount history");
		pageObj.guestTimelinePage().clickRewards();
		String honoredreward = pageObj.guestTimelinePage().getHonored();
		Assert.assertTrue(honoredreward.contains(dataSet.get("redeemable")),
				"Redeemed reward name did not matched in honored list");
		pageObj.utils().logPass("Redemption of reward is validated in honored list");

	}

	@SuppressWarnings("unused")
	@Test(description = "SQ-T2259 Verify Promotional type Redeemable with POS API", groups = { "regression",
			"dailyrun" }, priority = 2)
	@Owner(name = "Amit Kumar")
	public void T2259_verifyPromotionaltypeRedeemablewithPOSAPI() throws InterruptedException {
		String reward_Code = "";
		// User Signip using mobile api 2
		iFrameEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(iFrameEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Instance login and goto timeline
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.instanceDashboardPage().navigateToGuestTimeline(iFrameEmail);

		// click message gift and gift reward to user
		pageObj.guestTimelinePage().messageGiftToUser(dataSet.get("subject"), dataSet.get("reward"),
				dataSet.get("redeemable"), dataSet.get("giftReason"));
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "Message sent did not displayed on timeline");

		// iFrame Login
		pageObj.iframeSingUpPage().navigateToIframe(baseUrl + dataSet.get("whiteLabel") + dataSet.get("slug"));
		pageObj.iframeSingUpPage().iframeLogin(iFrameEmail);
		reward_Code = pageObj.iframeSingUpPage().redeemRewardOffer(dataSet.get("rewardName"));
		Assert.assertNotNull(reward_Code, "Reward code is null after redeeming the reward");

		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key = CreateDateTime.getTimeDateString();
		// Pos redemption api
		Response resp = pageObj.endpoints().posRedemptionOfPromotionaltypeRedeemable(iFrameEmail, date, reward_Code,
				key, txn, dataSet.get("locationKey"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp.getStatusCode(), "Status code 200 did not matched for pos redemption api");
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().navigateToGuestTimeline(iFrameEmail);

		pageObj.guestTimelinePage().refreshTimeline();
		Thread.sleep(5000);
		// Validate timeline for redemption and receipt
		String redeemedRedemption = pageObj.guestTimelinePage().redeemedRedemption(reward_Code);
		String discountValueWebCheckin = pageObj.guestTimelinePage().getDiscountValueWebCheckinDetails();
		String totalamoutnWebCheckin = pageObj.guestTimelinePage().getTotalAmountWebCheckin();
		pageObj.guestTimelinePage().refreshTimeline();
		Thread.sleep(5000);
		String discountValuePosCheckin = pageObj.guestTimelinePage().getDiscountValuePosCheckinDetails();
		String discountedAmount = pageObj.guestTimelinePage().getDiscountedAmountPosCheckinDetails();

		pageObj.guestTimelinePage().clickAccountHistory();
		List<String> Itemdata = pageObj.accounthistoryPage().getAccountDetailsforItemRedeemed();
		System.out.println(Itemdata);
		Assert.assertTrue(Itemdata.get(0).contains("Item Redeemed"), "Redemption did not redeemed in account history");
		Assert.assertTrue(Itemdata.get(0).contains("Promotional Brownrice redeemed"),
				"Redemption did not redeemed in account history");
		// Assert.assertTrue(Itemdata.get(1).contains("0 Items"), "reward item did not
		// decreased in account balance");
		pageObj.utils().logPass("Redemption of reward is validated in acount history");
		pageObj.guestTimelinePage().clickRewards();
		String honoredreward = pageObj.guestTimelinePage().getHonored();
		Assert.assertTrue(honoredreward.contains(dataSet.get("redeemable")),
				"Redeemed reward name did not matched in honored list");
		pageObj.utils().logPass("Redemption of reward is validated in honored list");

	}

	@SuppressWarnings("unused")
	@Test(description = "SQ-T2273 Verify the Coupon Code Redemption", groups = { "regression" }, priority = 3)
	@Owner(name = "Amit Kumar")
	public void T2273_verifyCouponCodeRedemption() throws InterruptedException {

		String couponCampaignName = "Coupon Campaign" + CreateDateTime.getTimeDateString();
		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Click Campaigns Link
		// Thread.sleep(2000);
		// pageObj.menupage().clickCampaignsMenu();
		// pageObj.menupage().clickCampaignsLink();
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value and create coupon campaign
		Thread.sleep(2000);
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().createWhatDetailsCoupanCampaign(couponCampaignName);
		pageObj.signupcampaignpage().createWhomDetailsCouponCampaign(dataSet.get("noOfGuests"),
				dataSet.get("usagePerGuest"), dataSet.get("giftType"), dataSet.get("amount"));
		pageObj.signupcampaignpage().createWhenDetailsCampaign();
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "Campaign created success message did not displayed....");
		// get coupon code
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().searchCampaign(couponCampaignName);
		// String couponCode = pageObj.campaignspage().getCouponCampaignCode();
		String couponCode = pageObj.campaignspage().getPreGeneratedCuponCode();
		// user creation using pos api
		iFrameEmail = pageObj.iframeSingUpPage().generateEmail();
		Response resp = pageObj.endpoints().posSignUp(iFrameEmail, dataSet.get("locationKey"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp.getStatusCode(), "Status code 200 did not matched for pos signup api");
		// hit pos redemption api

		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key = CreateDateTime.getTimeDateString();
		Response respo = pageObj.endpoints().posRedemptionOfCouponCode(iFrameEmail, date, couponCode, key, txn,
				dataSet.get("locationKey"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, respo.getStatusCode(), "Status code 200 did not matched for pos redemption api");

		pageObj.instanceDashboardPage().navigateToGuestTimeline(iFrameEmail);

		String discountValueWebCheckin = pageObj.guestTimelinePage().getDiscountValueWebCheckinDetails();
		String totalamoutnWebCheckin = pageObj.guestTimelinePage().getTotalAmountWebCheckin();

		String redeemedCouponStatus = pageObj.guestTimelinePage().verifyRedeemedCouponCode();
		Assert.assertTrue(redeemedCouponStatus.contains(couponCode),
				"Rdeemed coupon code details did not displayed  on timeline....");
		pageObj.utils().logPass("Redemption of coupon code validate on time line");

	}

	@SuppressWarnings("unused")
	@Test(description = "SQ-T2188 Verify the Subscription Redemption", groups = { "regression",
			"dailyrun" }, priority = 4)
	@Owner(name = "Amit Kumar")
	public void T2188_verifytheSubscriptionRedemption() throws InterruptedException {
		String purchasePrice = Integer.toString(Utilities.getRandomNoFromRange(1, 200));
		String endDateTime = CreateDateTime.getFutureDate(1) + " 10:00 PM";
		// user signup using api2
		iFrameEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(iFrameEmail, dataSet.get("client"),
				dataSet.get("secret"));

		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertEquals(signUpResponse.jsonPath().get("user.communicable_email").toString(),
				iFrameEmail.toLowerCase());
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		// subscription purchase api 2
		Response purchaseSubscriptionresponse = pageObj.endpoints().Api2SubscriptionPurchase(token,
				dataSet.get("plan_id"), dataSet.get("client"), dataSet.get("secret"), purchasePrice, endDateTime);

		Assert.assertEquals(purchaseSubscriptionresponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		String subscription_id = purchaseSubscriptionresponse.jsonPath().get("subscription_id").toString();
		// Pos redemption api
		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key = CreateDateTime.getTimeDateString();
		Response resp = pageObj.endpoints().posRedemptionOfSubscription(iFrameEmail, date, subscription_id, key, txn,
				dataSet.get("locationKey"), "8", "101");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp.getStatusCode(), "Status code 200 did not matched for pos redemption api");
		// Instance login and goto timeline
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.instanceDashboardPage().navigateToGuestTimeline(iFrameEmail);

		pageObj.guestTimelinePage().refreshTimeline();
		Thread.sleep(5000);
		// Validate timeline for redemption and receipt
		String discountValueWebCheckin = pageObj.guestTimelinePage().getDiscountValueWebCheckinDetails();
		String totalamoutnWebCheckin = pageObj.guestTimelinePage().getTotalAmountWebCheckin();
		pageObj.guestTimelinePage().refreshTimeline();
		String discountValuePosCheckin = pageObj.guestTimelinePage().getDiscountValuePosCheckinDetails();
		String discountedAmount = pageObj.guestTimelinePage().getDiscountedAmountPosCheckinDetails();
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}
}