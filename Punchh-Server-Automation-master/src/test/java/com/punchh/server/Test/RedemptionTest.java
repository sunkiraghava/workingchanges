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

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class RedemptionTest {

	private static Logger logger = LogManager.getLogger(RedemptionTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String iFrameEmail;
	private String sTCName;
	private String baseUrl;
	private String env, run = "ui";
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
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==>" + dataSet);
	}

	@SuppressWarnings("unused")
	@Test(description = "SQ-T2248 Verify the fuel Redemption || SQ-T2205 Verify Guest timeline and Account history in point to manual business", groups = {
			"regression", "dailyrun" }, priority = 0)
	@Owner(name = "Rakhi Rawat")
	public void verify_the_fuel_Redemption() throws InterruptedException {

		// user creation using pos signup api
		iFrameEmail = pageObj.iframeSingUpPage().generateEmail();
		Response resp = pageObj.endpoints().posSignUp(iFrameEmail, dataSet.get("locationKey"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp.getStatusCode(), "Status code 200 did not matched for post chekin api");
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.instanceDashboardPage().navigateToGuestTimeline(iFrameEmail);

		// click message gift and gift fuel discount
		pageObj.guestTimelinePage().messageFuelDiscountToUser(dataSet.get("subject"), dataSet.get("giftTypes"),
				dataSet.get("fuelPoints"), dataSet.get("giftReason"));
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		boolean fuelGiftedNotification = pageObj.guestTimelinePage().verifyfuelGiftedNotification();
		Assert.assertTrue(status, "Message sent did not displayed on guest timeline");
		Assert.assertTrue(fuelGiftedNotification, "Fuel gifted notification did not displayed on timeline");

		// Pos redemption api
		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key = CreateDateTime.getTimeDateString();
		Response respo = pageObj.endpoints().posRedemptionOfFuel(iFrameEmail, date, key, txn,
				dataSet.get("locationKey"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, respo.getStatusCode(),
				"Status code 200 did not matched for pos redemption api :" + respo.asString());
		// validate guest timeline
		pageObj.guestTimelinePage().refreshTimeline();
		String discountValueWebCheckin = pageObj.guestTimelinePage().getDiscountValueWebCheckinDetails();
		String totalamoutnWebCheckin = pageObj.guestTimelinePage().getTotalAmountWebCheckin();

	}

	@SuppressWarnings("unused")
	@Test(description = "SQ-T2269 Verify the Redemption with Iframe", groups = { "regression",
			"dailyrun" }, priority = 1)
	@Owner(name = "Amit Kumar")
	public void T2269_verifytheRedemptionwithIframe() throws InterruptedException {

		String reward_Code = "";
		// iFrame user Signup
		pageObj.iframeSingUpPage().navigateToIframe(baseUrl + dataSet.get("whiteLabel") + dataSet.get("slug"));
		iFrameEmail = pageObj.iframeSingUpPage().iframeSignUp();
		pageObj.iframeSingUpPage().iframeSignOut();
		// Instance login and goto timeline
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.instanceDashboardPage().navigateToGuestTimeline(iFrameEmail);

		// click message gift and gift reward to user
		pageObj.guestTimelinePage().messageGiftToUser(dataSet.get("subject"), dataSet.get("reward"),
				dataSet.get("redeemable"), dataSet.get("giftReason"));
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		String rewardName = pageObj.guestTimelinePage().getRewardName();
		Assert.assertTrue(status, "Message sent did not displayed on timeline");
		Assert.assertEquals(rewardName, "Rewarded $2.0 OFF");
		// iFrame Login and redeem reward by generating code
		pageObj.iframeSingUpPage().navigateToIframe(baseUrl + dataSet.get("whiteLabel") + dataSet.get("slug"));
		pageObj.iframeSingUpPage().iframeLogin(iFrameEmail);
		reward_Code = pageObj.iframeSingUpPage().redeemRewardOffer(dataSet.get("rewardName"));
		// Pos redemption api
		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key = CreateDateTime.getTimeDateString();
		Response resp = pageObj.endpoints().posRedemptionOfCode(iFrameEmail, date, reward_Code, key, txn,
				dataSet.get("locationKey"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp.getStatusCode(), "Status code 200 did not matched for pos redemption api");
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().navigateToGuestTimeline(iFrameEmail);
		pageObj.guestTimelinePage().clickTimeLine();
		pageObj.guestTimelinePage().refreshTimeline();
		Thread.sleep(5000);
		// Validate timeline for redemption and receipt
		String redeemedRedemption = pageObj.guestTimelinePage().redeemedRedemption(reward_Code);
		String discountValueWebCheckin = pageObj.guestTimelinePage().getDiscountValueWebCheckinDetails();
		String totalamoutnWebCheckin = pageObj.guestTimelinePage().getTotalAmountWebCheckin();
		/*
		 * pageObj.guestTimelinePage().refreshTimeline(); Thread.sleep(5000); String
		 * discountValuePosCheckin =
		 * pageObj.guestTimelinePage().getDiscountValuePosCheckinDetails(); String
		 * discountedAmount =
		 * pageObj.guestTimelinePage().getDiscountedAmountPosCheckinDetails();
		 */

		Assert.assertEquals(redeemedRedemption, reward_Code, "Redemption code is not displayed on time line");
		Assert.assertTrue(discountValueWebCheckin.contains("Discounted: 2.00"),
				"Discounted value for redemption did not matched");
		TestListeners.extentTest.get()
				.pass("redemption code and receipt validated successfully :" + redeemedRedemption);

	}

	@SuppressWarnings("unused")
	@Test(description = "SQ-T2281 Verify the Force Redemption through Dashboard", groups = { "regression",
			"dailyrun" }, priority = 2)
	@Owner(name = "Rakhi Rawat")
	public void T2281_verifyForceRedemptionthroughDashboard() throws InterruptedException {

		// user creation using pos signup api
		iFrameEmail = pageObj.iframeSingUpPage().generateEmail();
		Response resp = pageObj.endpoints().posSignUp(iFrameEmail, dataSet.get("locationKey"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp.getStatusCode(), "Status code 200 did not matched for pos signup api");
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.instanceDashboardPage().navigateToGuestTimeline(iFrameEmail);

		// send gift reward to user
		pageObj.guestTimelinePage().messageGiftToUser(dataSet.get("subject"), dataSet.get("reward"),
				dataSet.get("redeemable"), dataSet.get("giftReason"));
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		String rewardName = pageObj.guestTimelinePage().getRewardName();
		Assert.assertTrue(status, "Message sent did not displayed on timeline");
		Assert.assertEquals(rewardName, "Rewarded $2.0 OFF");

		// verify gift reward in account history
		pageObj.guestTimelinePage().clickAccountHistory();
		List<String> rewarddata = pageObj.accounthistoryPage().getAccountDetailsforGiftedItem();
		Assert.assertTrue(rewarddata.get(0).contains("Item Gifted"),
				"Reward gifted to user did not appeared in account history");
		// Assert.assertTrue(rewarddata.get(1).contains("1 Item"), "reward item did not
		// added to account balance");
		TestListeners.extentTest.get().pass("Verified Gifted reward to account history");
		// send gift points to user
		pageObj.guestTimelinePage().messagePointsToUser(dataSet.get("subject"), dataSet.get("option"),
				dataSet.get("giftTypes"), dataSet.get("giftReason"));
		boolean pointStatus = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(pointStatus, "Message sent did not displayed on timeline");

		// verify gift reward in account history
		pageObj.guestTimelinePage().clickAccountHistory();
		List<String> pointsdata = pageObj.accounthistoryPage().getAccountDetailsforGiftedItem();
		Assert.assertTrue(pointsdata.get(0).contains("Bonus points Earned"),
				"Points gifted to user did not appeared in account history");
		Assert.assertTrue(pointsdata.get(1).contains(dataSet.get("giftTypes") + " Points"),
				"Points did not added to account balance");
		TestListeners.extentTest.get().pass("Verified Gifted points to account history");
		// Force Redemption Reward
		// pageObj.forceredemptionPage().clickForceRedemptionBtn();
		pageObj.iframeConfigurationPage().clickOnPageTab("Force Redemption");
		pageObj.forceredemptionPage().forceRedemptionreward(dataSet.get("comment"), dataSet.get("redeemable"));
		boolean forceRedemptionRewardStatus = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(forceRedemptionRewardStatus, "Force redemption success message did not displayed");
		TestListeners.extentTest.get().pass("Force redemption of reward is done successfully");
		// verify account history after Item redemption

		pageObj.guestTimelinePage().clickAccountHistory();
		Thread.sleep(15000);
		pageObj.guestTimelinePage().refreshTimeline();
		List<String> Itemdata = pageObj.accounthistoryPage().getAccountDetailsforGiftedItem();
		Assert.assertTrue(Itemdata.get(0).contains("Item Redeemed"),
				"Redeemed Reward with force redemption did not appeared in account history");
		// Assert.assertTrue(Itemdata.get(1).contains("0 Items"), "reward item did not
		// decreased in account balance");
		TestListeners.extentTest.get().pass("Force redemption of reward is validated in acount history");

		// Force Redemption Points
		// pageObj.forceredemptionPage().clickForceRedemptionBtn();
		pageObj.iframeConfigurationPage().clickOnPageTab("Force Redemption");
		pageObj.forceredemptionPage().forceRedemptionPoints(dataSet.get("comment"), dataSet.get("giftTypes"));
		boolean forceRedemptionPointsStatus = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(forceRedemptionRewardStatus, "Force redemption success message did not displayed");
		TestListeners.extentTest.get().pass("Force redemption of points is done successfully");
		// verify account history after points redemption
		pageObj.guestTimelinePage().clickAccountHistory();
		List<String> data = pageObj.accounthistoryPage().getAccountDetailsforGiftedItem();
		Assert.assertTrue(data.get(0).contains("Rewards Redeemed"),
				"Redeemed Reward with force redemption did not appeared in account history");
		Assert.assertTrue(data.get(1).contains("0 Points"), "Redeemed Points did not decreased in point balance");
		TestListeners.extentTest.get().pass("Force redemption of points is validated in account history");

	}

	@SuppressWarnings("unused")
	@Test(description = "SQ-T2207 Verify the Redemption with mobile apis", groups = { "regression",
			"dailyrun" }, priority = 3)
	@Owner(name = "Rakhi Rawat")
	public void T2207_verifyRedemptionwithmobileapis() throws InterruptedException {

		iFrameEmail = pageObj.iframeSingUpPage().generateEmail();
		// Signup using mobile api
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(iFrameEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 1 user signup");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertEquals(signUpResponse.jsonPath().get("email").toString(), iFrameEmail.toLowerCase());
		String token = signUpResponse.jsonPath().get("auth_token.token").toString();
		logger.info(token);

		// navigate to user timeline
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.instanceDashboardPage().navigateToGuestTimeline(iFrameEmail);

		// click message gift and gift points
		pageObj.guestTimelinePage().messagePointsToUser(dataSet.get("subject"), dataSet.get("option"),
				dataSet.get("giftTypes"), dataSet.get("giftReason"));
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "Message sent did not displayed on timeline");

		// generate redemption code using mobile api with redeemable_id
		Response redemption_codeResponse = pageObj.endpoints().Api1MobileRedemptionRedeemable_id(token,
				dataSet.get("redeemable_id"), dataSet.get("client").trim(), dataSet.get("secret").trim());
		Assert.assertEquals(redemption_codeResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		String redemption_Code = redemption_codeResponse.jsonPath().get("internal_tracking_code").toString();
		logger.info(redemption_Code);

		// Redemption using Pos redemption api
		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key = CreateDateTime.getTimeDateString();
		Response resp = pageObj.endpoints().posRedemptionOfCode(iFrameEmail, date, redemption_Code, key, txn,
				dataSet.get("locationKey"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp.getStatusCode(), "Status code 200 did not matched for pos redemption api");

		// Validate timeline for redemption and receipt
		pageObj.guestTimelinePage().refreshTimeline();
		Thread.sleep(5000);
		String redeemedRedemption = pageObj.guestTimelinePage().redeemedRedemption(redemption_Code);
		String discountValueWebCheckin = pageObj.guestTimelinePage().getDiscountValueWebCheckinDetails();
		String totalamoutnWebCheckin = pageObj.guestTimelinePage().getTotalAmountWebCheckin();

		// verify gift reward in account history
		Thread.sleep(2000);
		pageObj.guestTimelinePage().clickAccountHistory();
		List<String> Itemdata = pageObj.accounthistoryPage().getAccountDetailsforRewardRedeemed();
		System.out.println(Itemdata);
		Assert.assertTrue(Itemdata.get(0).contains("Rewards Redeemed"),
				"Redemption did not redeemed in account history");
		Assert.assertTrue(Itemdata.get(0).contains("Simplicitea redeemed using redemption code " + redemption_Code),
				"Reward redeemed using redemption code msg did not matched");
		TestListeners.extentTest.get().pass("Redemption of reward is validated in acount history");
	}

	@Test(description = "SQ-T5549 Verify redemption_status: processed in API response when flag is enabled.", priority = 1, groups = {
			"regression" })
	@Owner(name = "Vansham Mishra")
	public void T5549_VerifyThatRedemptionStatusShouldBeProcessed() throws InterruptedException {

		// User SignUp using API
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("API2 Signup is successful");
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// click message gift and gift reward to user
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().giftRedeemableToUser(dataSet.get("reward"), dataSet.get("redeemable"));

		// login user in iframe and redeem reward by generating code
		pageObj.iframeSingUpPage().navigateToIframe(baseUrl + dataSet.get("whiteLabel") + dataSet.get("slug"));

		// redeem reward by generating code
		pageObj.iframeSingUpPage().iframeLogin(userEmail);
		String redemptionCode = pageObj.iframeSingUpPage().redeemRewardOffer(dataSet.get("rewardName"));

		// process redemption code in POS api
		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key = CreateDateTime.getTimeDateString();
		Response resp = pageObj.endpoints().posRedemptionOfCode(userEmail, date, redemptionCode, key, txn,
				dataSet.get("locationKey"));
		Assert.assertEquals(resp.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for pos redemption api");
		logger.info("Verified that redemption code has been processed successfully");
		TestListeners.extentTest.get().pass("Verified that redemption code has been processed successfully");

		// verify that redemption_status should be processed in the response of users
		// extensive_timeline API
		Response extendedUserHistoryResponse = pageObj.endpoints().extendedUserHistory(userID, dataSet.get("apiKey"));
		Assert.assertEquals(extendedUserHistoryResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Get Extended User History");
		String redemptionStatus = extendedUserHistoryResponse.jsonPath().get("redemptions[0].redemption_status")
				.toString();
		Assert.assertEquals(redemptionStatus, "processed",
				"Redemption status is not processed in the response of users extensive_timeline API");
		logger.info("Verified that redemption status is processed in the response of users extensive_timeline API");
		TestListeners.extentTest.get()
				.pass("Verified that redemption status is processed in the response of users extensive_timeline API");
	}

	@Test(description = "SQ-T6257/SQ-T6321 Verify Fuel Reward option is visible in UI when Enable Multiple Redemptions flag enable.", priority = 1, groups = {
			"regression" })
	@Owner(name = "Vansham Mishra")
	public void T6257_verifyFuelRewardOptionVisibleWhenMultipleRedemptionsEnabled() throws InterruptedException {

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().checkUncheckFlagCockpitDashboard(dataSet.get("cockpitDashboardPageFlag"), "check");
		logger.info("Enabled the flag Enable Multiple Redemptions in Cockpit Dashboard");
		TestListeners.extentTest.get().pass("Enabled the flag Enable Multiple Redemptions in Cockpit Dashboard");
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.cockpitRedemptionsPage().setAutoRedemptionDiscounts(dataSet.get("cockpitRedemptionsPageDropdown"),
				"select");
		logger.info("Verified that Fuel reward option should visible in auto redemption option dropdown.");
		TestListeners.extentTest.get()
				.pass("Verified that Fuel reward option should visible in auto redemption option dropdown.");
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}

}
