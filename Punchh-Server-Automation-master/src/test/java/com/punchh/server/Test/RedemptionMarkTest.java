package com.punchh.server.Test;

import java.awt.AWTException;
import java.lang.reflect.Method;
import java.util.List;
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
import com.punchh.server.utilities.ApiUtils;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class RedemptionMarkTest {

	private static Logger logger = LogManager.getLogger(RedemptionMarkTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String iFrameEmail;
	private String sTCName;
	private String baseUrl;
	private String env, run = "ui";
	private static Map<String, String> dataSet;
	Utilities utils;
	SeleniumUtilities selUtils;
	ApiUtils apiUtils;

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
		utils = new Utilities(driver);
		selUtils = new SeleniumUtilities(driver);
		apiUtils = new ApiUtils();

	}

	/* author: Ashwini */
	@Test(description = "SQ-T2646 Validate that if redemption mark is entered at the membership level and business level, then force redemption respects the value entered at membership level", groups = {
			"regression", "unstable", "dailyrun" }, priority = 0)
	@Owner(name = "Ashwini Shetty")
	public void T2646_ForceRedeemptionOnUserTimeline() throws InterruptedException, AWTException {
		String rewardValue1 = dataSet.get("rewardValueAtMembershipLevel");
		dataSet.get("rewardValueAtBusinessLevel");
		String redemptionMark1 = dataSet.get("redemptionMarkAtMembershipLevel");
		String redemptionMark2 = dataSet.get("redemptionMarkAtBusinessLevel");
		String requestedPoints = dataSet.get("requestedPoints");
		String expectedRewardRedeemed = rewardValue1; // because force redemption respects the value entered at
														// membership level

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// configurations
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		pageObj.settingsPage().clickMemberLevel(dataSet.get("membershipLevel"));
		pageObj.settingsPage().editMembershipLevelMinMaxPoints(dataSet.get("membershipMinPoints"),
				dataSet.get("membershipMaxPoints"));
		pageObj.settingsPage().clickUpdateMembership();
		pageObj.forceredemptionPage().membershipConfig(dataSet.get("membershipLevel"), rewardValue1, redemptionMark1);

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.forceredemptionPage().redemptionMarkInCockpit(redemptionMark2);

		// User SignUp using API
		String iFrameEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(iFrameEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("API2 Signup is successful");
		logger.info("API2 Signup is successful");

		// Gift points to the user
		pageObj.instanceDashboardPage().navigateToGuestTimeline(iFrameEmail);
		pageObj.guestTimelinePage().messagePointsToUser(dataSet.get("subject"), dataSet.get("option"),
				dataSet.get("giftTypes"), dataSet.get("giftReason"));
		boolean pointStatus = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(pointStatus, "Message sent did not displayed on timeline");

		// force redemption in the user timeline
		pageObj.forceredemptionPage().forceRedemptionPointsUpdated(dataSet.get("comment"),
				dataSet.get("forceRedemptionType"), requestedPoints);

		// check account history
		boolean checkinStatus = pageObj.guestTimelinePage().accountHistoryApi2(dataSet.get("client"),
				dataSet.get("secret"), token, "$20.00 of banked rewards", "description");
		Assert.assertEquals(checkinStatus, true, "Force redemption value does not match in account history");
		logger.info("Force redemption value matched in account history");
		utils.logPass("Force redemption value matched in account history");

		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		pageObj.forceredemptionPage().membershipConfig(dataSet.get("membershipLevel"), rewardValue1, "");
	}

	@Test(description = "SQ-T5279 Attempt redemption from POS console at a disapproved location.", priority = 1, groups = {
			"regression", "unstable" ,"nonNightly"})
	@Owner(name = "Rakhi Rawat")
	public void T5279_RedemptionAtDisapprovedLocation() throws InterruptedException {

		// User SignUp using API
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("API2 Signup is successful");

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// set location status to approved
		pageObj.menupage().navigateToSubMenuItem("Settings", "Locations");
		String status = pageObj.locationPage().getLocationStatus("DisapprovedLocation", "Status");
		if (status.equalsIgnoreCase("Disapproved")) {
			// pageObj.locationPage().setLocationStatus("DisapprovedLocation", "approved");
			pageObj.locationPage().clickOnLocationName("DisapprovedLocation");
			pageObj.locationPage().locationOperation("Enable for loyalty checkins");
//			utils.longWaitInSeconds(2);
//			selUtils.navigateBack();
			pageObj.menupage().navigateToSubMenuItem("Settings", "Locations");
			String status1 = pageObj.locationPage().getLocationStatus("DisapprovedLocation", "Status");
			Assert.assertEquals(status1, "Approved", "Location is not approved");
		}

		// click message gift and gift reward to user
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().giftRedeemableToUser(dataSet.get("reward"), dataSet.get("redeemable"));

		// login user in iframe and redeem reward by generating code
		pageObj.iframeSingUpPage().navigateToIframe(baseUrl + dataSet.get("whiteLabel") + dataSet.get("slug"));

		// redeem reward by generating code
		pageObj.iframeSingUpPage().iframeLogin(userEmail);
		String redemptionCode = pageObj.iframeSingUpPage().redeemRewardOffer(dataSet.get("rewardName"));

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().movedToLoginAndSelectBusiness(dataSet.get("slug"));

		// Disaaprove location
		pageObj.menupage().navigateToSubMenuItem("Settings", "Locations");
		pageObj.locationPage().clickOnLocationName("DisapprovedLocation");
		pageObj.locationPage().locationOperation("Disable for all further loyalty checkins");
		utils.acceptAlert(driver);

		// naviagte to dashboard
		pageObj.menupage().clickDashboardMenu();
		pageObj.dashboardpage().clickPosConsoleBtn();
		pageObj.dashboardpage().selectLocationFromConsolePage("DisapprovedLocation");
		String text = pageObj.dashboardpage().attemptRedemptionFromPosConsole(redemptionCode);
		Assert.assertEquals(text, "Error on Honoring.", "Validation message does not verified");
		logger.info("Validation message verified for disapproved location");
		utils.logPass("Validation message verified for disapproved location");
		selUtils.navigateBack();
		selUtils.navigateBack();

		// Approve location
		pageObj.menupage().navigateToSubMenuItem("Settings", "Locations");
		pageObj.locationPage().clickOnLocationName("DisapprovedLocation");
		pageObj.locationPage().locationOperation("Enable for loyalty checkins");
		utils.acceptAlert(driver);

	}

	@Test(description = "SQ-T5280 Attempt force redemption from user's timeline at a disapproved location",groups = {"nonNightly" }, priority = 2)
	@Owner(name = "Rakhi Rawat")
	public void T5280_RedemptionAtDisapprovedLocation() throws InterruptedException {

		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 user signup");

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Disaaprove location
		pageObj.menupage().navigateToSubMenuItem("Settings", "Locations");
		pageObj.locationPage().clickOnLocationName("DisapprovedLocation");
		pageObj.locationPage().locationOperation("Disable for all further loyalty checkins");

		// redemption after disabling flag
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.dashboardpage().navigateToTabs("Redemption Validations");
		pageObj.dashboardpage().checkUncheckFlagCockpitDashboard(
				"Do locations have to be enabled or approved to allow a redemption?", "uncheck");
		pageObj.dashboardpage().clickOnUpdateButton();

		// navigate to guest timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
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
		utils.logPass("Verified Gifted reward to account history");

		// Force Redemption Reward
		pageObj.forceredemptionPage().clickForceRedemptionBtn();
		pageObj.forceredemptionPage().forceRedemptionreward(dataSet.get("comment"), dataSet.get("redeemable"));
		boolean forceRedemptionRewardStatus = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(forceRedemptionRewardStatus, "Force redemption success message did not displayed");
		utils.logPass("Force redemption of reward is done successfully");

		// redemption after enabling flag
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.dashboardpage().navigateToTabs("Redemption Validations");
		pageObj.dashboardpage().checkUncheckFlagCockpitDashboard(
				"Do locations have to be enabled or approved to allow a redemption?", "check");
		pageObj.dashboardpage().clickOnUpdateButton();
		// waiting for 1 minute to ensure the flag is updated
		pageObj.guestTimelinePage().pingSessionforLongWait(1);
		// navigate to guest timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		// send gift reward to user
		pageObj.guestTimelinePage().messageGiftToUser(dataSet.get("subject"), dataSet.get("reward"),
				dataSet.get("redeemable"), dataSet.get("giftReason"));
		boolean status1 = pageObj.campaignspage().validateSuccessMessage();
		String rewardName1 = pageObj.guestTimelinePage().getRewardName();
		Assert.assertTrue(status1, "Message sent did not displayed on timeline");
		Assert.assertEquals(rewardName1, "Rewarded $2.0 OFF");

		// verify gift reward in account history
		pageObj.guestTimelinePage().clickAccountHistory();
		List<String> rewarddata1 = pageObj.accounthistoryPage().getAccountDetailsforGiftedItem();
		Assert.assertTrue(rewarddata1.get(0).contains("Item Gifted"),
				"Reward gifted to user did not appeared in account history");
		utils.logPass("Verified Gifted reward to account history");

		// Force Redemption Reward
		pageObj.forceredemptionPage().clickForceRedemptionBtn();
		pageObj.forceredemptionPage().forceRedemptionreward(dataSet.get("comment"), dataSet.get("redeemable"));
		String forceRedemptionRewardStatus1 = pageObj.campaignspage().validateErrorsMessagee();
		Assert.assertTrue(forceRedemptionRewardStatus1.contains(
				"has been disabled for redemptions. Please alert a staff member if you think this is an error."),
				"Force redemption is successful");
		logger.info("Force redemption is unsuccessful for disapproved location");
		utils.logPass("Force redemption is unsuccessful for disapproved location");

		// Approve location
		pageObj.menupage().navigateToSubMenuItem("Settings", "Locations");
		pageObj.locationPage().clickOnLocationName("DisapprovedLocation");
		pageObj.locationPage().locationOperation("Enable for loyalty checkins");
		utils.acceptAlert(driver);
	}
	
	@Test(description = "SQ-T5281 Attempt force redemption from API \"api2/dashboard/redemptions/force_redeem\" end point at a disapproved location.", priority = 3, groups = {
			"regression", "unstable", "dailyrun" ,"nonNightly"})
	@Owner(name = "Rakhi Rawat")
	public void T5281_RedemptionAtDisapprovedLocation() throws InterruptedException {

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.dashboardpage().navigateToTabs("Redemption Validations");
		pageObj.dashboardpage().checkUncheckFlagCockpitDashboard(
				"Do locations have to be enabled or approved to allow a redemption?", "check");
		pageObj.dashboardpage().clickOnUpdateButton();
		// Disaaprove location
		pageObj.menupage().navigateToSubMenuItem("Settings", "Locations");
		pageObj.locationPage().clickOnLocationName("DisapprovedLocation");
		pageObj.locationPage().locationOperation("Disable for all further loyalty checkins");
		// waiting for 1 minute to ensure the flag is updated
		pageObj.guestTimelinePage().pingSessionforLongWait(1);

		// User register/signup using API2 Signup
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("Api2 user signup is successful");

		// send points to user
		Response sendPointsResponse = pageObj.endpoints().sendPointsToUser(userID, dataSet.get("amount"),
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendPointsResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send points to user");
		utils.logPass("Api2  send gift points to user is successful");

		// Force Redeem
		Response forceRedeemResponse = pageObj.endpoints().forceRedeemption(dataSet.get("adminAuthorization"), userID,
				"unbanked_points_redemption", "requested_punches", "2", "points");
		Assert.assertTrue(forceRedeemResponse.asString().contains("has been disabled for redemptions"),
				"Force Redemption is successful.");
		logger.info("Force redemption is unsuccessful for disapproved location");
		utils.logPass("Force redemption of reward is unsuccessful for disapproved location");

		// fetch user offers/ reward_id using ap2 mobileOffers
		Response offerResponse1 = pageObj.endpoints().getUserOffers(token, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, offerResponse1.getStatusCode(),
				"Status code 200 did not matched for api2 user offers");
		String reward_id = offerResponse1.jsonPath().get("rewards[0].reward_id").toString();
		utils.logPass("Api2 user fetch user offers is successful");

		// Force Redeem
		Response forceRedeemResponse1 = pageObj.endpoints().forceRedeem(dataSet.get("adminAuthorization"), reward_id,
				userID);
		Assert.assertTrue(forceRedeemResponse1.asString().contains("has been disabled for redemptions"),
				"Force Redemption is successful.");
		logger.info("Force redemption is unsuccessful for disapproved location");
		utils.logPass("Force redemption of points is unsuccessful for disapproved location");

		// Approve location
		pageObj.menupage().navigateToSubMenuItem("Settings", "Locations");
		pageObj.locationPage().clickOnLocationName("DisapprovedLocation");
		pageObj.locationPage().locationOperation("Enable for loyalty checkins");
		utils.acceptAlert(driver);

	}

	@Test(description = "SQ-T5287 Attempt redemption from Pos/Possible API 'api/pos/redemptions/possible' end point at a disapproved location.", priority = 4, groups = {
			"regression", "unstable", "dailyrun" })
	@Owner(name = "Amit Kumar")
	public void T5287_RedemptionAtDisapprovedLocation() throws InterruptedException {

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Disaaprove location
		pageObj.menupage().navigateToSubMenuItem("Settings", "Locations");
		pageObj.locationPage().clickOnLocationName("DisapprovedLocation");
		pageObj.locationPage().locationOperation("Disable for all further loyalty checkins");

		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key = CreateDateTime.getTimeDateString();

		// User register/signup using API2 Signup
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("Api2 user signup is successful");

		// send reward amount to user Amount
		Response sendRewardResponse1 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("adminAuthorization"),
				dataSet.get("amount"), dataSet.get("redeemable_id"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse1.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		utils.logPass("Api2  send reward amount to user is successful");

		// fetch user offers/ reward_id using ap2 mobileOffers
		Response offerResponse = pageObj.endpoints().getUserOffers(token, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, offerResponse.getStatusCode(), "Status code 200 did not matched for api2 user offers");
		String reward_id = offerResponse.jsonPath().get("rewards[0].reward_id").toString();
		logger.info(reward_id);
		utils.logPass("Api2 user fetch user offers is successful");

		// ********Possible redemptions********
		// pos possible redemption with disapproved location
		Response respo = pageObj.endpoints().posPossibleRedemptionOfAmount(userEmail, date, key, txn,
				dataSet.get("redeemAmount"), "Token token=" + dataSet.get("disapprovedLocation"));
		Assert.assertTrue(respo.asString().contains("has been disabled for redemptions"),
				"POS Possible Redemption is successful.");
		logger.info("Possible redemption of points is unsuccessful for disapproved location");
		utils.logPass("Possible redemption of points is unsuccessful for disapproved location");
		// send points to user
		Response sendPointsResponse = pageObj.endpoints().sendPointsToUser(userID, "5",
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendPointsResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send points to user");
		utils.logPass("Api2  send gift points to user is successful");

		// fetch user offers/ reward_id using ap2 mobileOffers
		Response offerResponse1 = pageObj.endpoints().getUserOffers(token, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, offerResponse1.getStatusCode(),
				"Status code 200 did not matched for api2 user offers");
		String pointsReward_id = offerResponse1.jsonPath().get("rewards[0].reward_id").toString();
		logger.info(pointsReward_id);
		utils.logPass("Api2 user fetch user offers is successful");

		// pos possible redemption with disapproved location
		logger.info("== POS API: verify possible redemptions with invalid location key ==");
		Response respo1 = pageObj.endpoints().posPossibleRedemptionOfAmount(userEmail, date, key, txn,
				dataSet.get("redeemAmount"), "Token token=" + dataSet.get("disapprovedLocation"));
		Assert.assertTrue(respo1.asString().contains("has been disabled for redemptions"),
				"POS Possible Redemption is successful.");
		logger.info("Possible redemption of points is unsuccessful for disapproved location");
		utils.logPass("Possible redemption of points is unsuccessful for disapproved location");

		// Approve location
		pageObj.menupage().navigateToSubMenuItem("Settings", "Locations");
		pageObj.locationPage().clickOnLocationName("DisapprovedLocation");
		pageObj.locationPage().locationOperation("Enable for loyalty checkins");
		utils.acceptAlert(driver);
		// check if location approved or not
		pageObj.menupage().navigateToSubMenuItem("Settings", "Locations");
		String status1 = pageObj.locationPage().getLocationStatus("DisapprovedLocation", "Status");
		Assert.assertEquals(status1, "Approved", "Location is not approved");

	}

	// Rakhi
	@Test(description = "SQ-T5553 Verify newly added fields in case of Fuel redemption"
			+ "SQ-T5989 Verify the UI changes for Redemption Mark, Banked Redeemable and Reward Value Fields for Category Chosen by Guest", priority = 5, groups = {
					"regression", "dailyrun" })
	@Owner(name = "Rakhi Rawat")
	public void T5553_verifyNewlyAddedFieldsFuelRedemption() throws Exception {

		String b_id = dataSet.get("business_id");

		// enable flag from the db
		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		pageObj.singletonDBUtilsObj();
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");

		pageObj.singletonDBUtilsObj();
		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag"), b_id);
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to true");
		logger.info(dataSet.get("dbFlag") + " value is updated to true");
		utils.logit(dataSet.get("dbFlag") + " value is updated to true");

		String iFrameEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse1 = pageObj.endpoints().Api1UserSignUp(iFrameEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 user signup");
		String userID = signUpResponse1.jsonPath().get("user_id").toString();
		utils.logPass("API v1 user #1 signup is successful");
		logger.info("API v1 user #1 signup is successful");

		// Navigating to the guest timeline
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// update membership
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Has Membership Levels?", "check");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Membership Level Bump on Edge?", "check");
		pageObj.dashboardpage().clickOnUpdateButton();

		// navigate to membership
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		pageObj.settingsPage().clickMemberLevel(dataSet.get("membership"));

		for (int i = 1; i < 4; i++) {
			Boolean flag1 = pageObj.settingsPage().verifyFiledAvailableOrNot(dataSet.get("field" + i));
			Assert.assertFalse(flag1, dataSet.get("field" + i)
					+ " is visible for Points Convert To Category Chosen by Guest earning type");
			logger.info("Verified " + dataSet.get("field" + i)
					+ " is not visible for Points Convert To Category Chosen by Guest earning type");
			utils.logit("Verified " + dataSet.get("filed" + i)
					+ " is not visible for Points Convert To Category Chosen by Guest earning type");
		}

		pageObj.instanceDashboardPage().navigateToGuestTimeline(iFrameEmail);

		// send fuel amount to user
		Response sendFuelResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "", "", "10",
				"");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendFuelResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		logger.info("Send fuel amount to the user successfully");
		utils.logPass("Send fuel amount to the user successfully");

		// force redemption of fuel
		Response forceFuelRedeem_Response = pageObj.endpoints().forceRedeemption(dataSet.get("apiKey"), userID,
				"fuel_redemption", "requested_punches", "5", "fuel");
		Assert.assertEquals(forceFuelRedeem_Response.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for pos redemption api");
		logger.info("Force redemption of 10 points is successful");
		utils.logit("Force redemption of 10 points is successful");

		// verify newly added fields from extendedUserHistory api
		Response extendedUserHistoryResponse = pageObj.endpoints().extendedUserHistory(userID, dataSet.get("apiKey"));
		Assert.assertEquals(extendedUserHistoryResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Get Extended User History");
		utils.logPass("PLATFORM FUNCTIONS API Get Extended User History is successful");
		String redemptionType = extendedUserHistoryResponse.jsonPath().get("redemptions[0].type").toString();
		Assert.assertEquals(redemptionType, "FuelRewardRedemption", "Redemption type does not matched");
		Assert.assertTrue(extendedUserHistoryResponse.asString().contains("updated_at")
				&& extendedUserHistoryResponse.asString().contains("processed_at")
				&& extendedUserHistoryResponse.asString().contains("redemption_status"));
		logger.info("Verified newly added fields in API response in case of Fuel redemption when "
				+ dataSet.get("dbFlag") + " flag is enabled");
		utils.logPass("Verified newly added fields in API response in case of Fuel redemption when "
						+ dataSet.get("dbFlag") + " flag is enabled");
	}

	// Rakhi
	@Test(description = "SQ-T5554 Verify newly added fields in case of currency redemption", priority = 6, groups = {
			"regression", "dailyrun" })
	@Owner(name = "Rakhi Rawat")
	public void T5554_verifyNewlyAddedFieldsCurrencyRedemption() throws Exception {

		String b_id = dataSet.get("business_id");

		// enable flag from the db
		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		pageObj.singletonDBUtilsObj();
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");

		pageObj.singletonDBUtilsObj();
		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag"), b_id);
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to true");
		logger.info(dataSet.get("dbFlag") + " value is updated to true");
		utils.logit(dataSet.get("dbFlag") + " value is updated to true");

		String iFrameEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse1 = pageObj.endpoints().Api1UserSignUp(iFrameEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 user signup");
		String userID = signUpResponse1.jsonPath().get("user_id").toString();
		utils.logPass("API v1 user #1 signup is successful");
		logger.info("API v1 user #1 signup is successful");

		// Navigating to the guest timeline
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// send reward amount to user Amount
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"),
				dataSet.get("amount"), "", "", "");
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send message to user");
		logger.info("Api2  send reward amount to user is successful");
		utils.logPass("Api2  send reward amount to user is successful");

		// do force redemption of currency
		Response forceRedeem_Response5 = pageObj.endpoints().forceRedeemption(dataSet.get("apiKey"), userID,
				"amount_redemption", "requested_punches", "2", "reward");
		Assert.assertEquals(forceRedeem_Response5.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for pos redemption api");
		logger.info("Force redemption of $1 is successful");
		utils.logit("Force redemption of $1 is successful");

		// verify newly added fields from extendedUserHistory api
		Response extendedUserHistoryResponse4 = pageObj.endpoints().extendedUserHistory(userID, dataSet.get("apiKey"));
		Assert.assertEquals(extendedUserHistoryResponse4.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Get Extended User History");
		utils.logPass("PLATFORM FUNCTIONS API Get Extended User History is successful");
		String redemptionType = extendedUserHistoryResponse4.jsonPath().get("redemptions[0].type").toString();
		Assert.assertEquals(redemptionType, "BankedRewardRedemption", "Redemption Status does not matched");
		Assert.assertTrue(
				extendedUserHistoryResponse4.asString().contains("updated_at")
						&& extendedUserHistoryResponse4.asString().contains("processed_at")
						&& extendedUserHistoryResponse4.asString().contains("redemption_status"),
				"Unable to verify newly added fields in API response in case of Currency redemption when flag is enabled");
		logger.info("Verified newly added fields in API response in case of Currency redemption when "
				+ dataSet.get("dbFlag") + " flag is enabled");
		utils.logPass("Verified newly added fields in API response in case of Currency redemption when "
						+ dataSet.get("dbFlag") + " flag is enabled");
	}

	// Rakhi
	@Test(description = "SQ-T5552 Verify newly added fields in case of unbanked points redemption", priority = 7, groups = {
			"regression", "dailyrun" })
	@Owner(name = "Rakhi Rawat")
	public void T5552_verifyNewlyAddedFieldsUnbankedPointsRedemption() throws Exception {

		String b_id = dataSet.get("business_id");

		// enable flag from the db
		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		pageObj.singletonDBUtilsObj();
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");

		pageObj.singletonDBUtilsObj();
		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag"), b_id);
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to true");
		logger.info(dataSet.get("dbFlag") + " value is updated to true");
		utils.logit(dataSet.get("dbFlag") + " value is updated to true");

		String iFrameEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse1 = pageObj.endpoints().Api1UserSignUp(iFrameEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 user signup");
		String userID = signUpResponse1.jsonPath().get("user_id").toString();
		utils.logPass("API v1 user #1 signup is successful");
		logger.info("API v1 user #1 signup is successful");

		// send points to user
		Response sendPointsResponse = pageObj.endpoints().sendPointsToUser(userID, "10", dataSet.get("apiKey"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendPointsResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send points to user");
		utils.logPass("Api2  send gift points to user is successful");

		// force redemption of 10 points
		Response forceRedeem_Response = pageObj.endpoints().pointForceRedeemWithType(dataSet.get("apiKey"), userID, "5",
				"unbanked_points_redemption");
		Assert.assertEquals(forceRedeem_Response.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for force Redemption of Points");

		// verify newly added fields from extendedUserHistory api
		Response extendedUserHistoryResponse2 = pageObj.endpoints().extendedUserHistory(userID, dataSet.get("apiKey"));
		Assert.assertEquals(extendedUserHistoryResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Get Extended User History");
		utils.logPass("PLATFORM FUNCTIONS API Get Extended User History is successful");
		String redemptionStatus1 = extendedUserHistoryResponse2.jsonPath().get("redemptions[0].type").toString();
		Assert.assertEquals(redemptionStatus1, "UnbankedPointRedemption", "Redemption type does not matched");
		Assert.assertTrue(extendedUserHistoryResponse2.asString().contains("updated_at")
				&& extendedUserHistoryResponse2.asString().contains("processed_at")
				&& extendedUserHistoryResponse2.asString().contains("redemption_status"));
		logger.info("Verified newly added fields in API response in case of Unbanked Points redemption when "
				+ dataSet.get("dbFlag") + " flag is enabled");
		utils.logPass("Verified newly added fields in API response in case of Unbanked Points redemption when "
						+ dataSet.get("dbFlag") + " flag is enabled");

	}

	// Rakhi
	@Test(description = "SQ-T5550 Verify redemption_status: redeemable in API response when flag is enabled"
			+ "SQ-T5555 Verify 'api2/dashboard/users/extensive_timeline' is not returning added fields when flag is disabled", priority = 8, groups = {
					"regression", "dailyrun" })
	@Owner(name = "Rakhi Rawat")
	public void T5550_verifyRedemptionStatus() throws Exception {

		String b_id = dataSet.get("business_id");

		// enable flag from the db
		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		pageObj.singletonDBUtilsObj();
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");

		pageObj.singletonDBUtilsObj();
		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag"), b_id);
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to true");
		logger.info(dataSet.get("dbFlag") + " value is updated to true");
		utils.logit(dataSet.get("dbFlag") + " value is updated to true");

		String iFrameEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse1 = pageObj.endpoints().Api1UserSignUp(iFrameEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 user signup");
		String token = signUpResponse1.jsonPath().get("auth_token.token").toString();
		String userID = signUpResponse1.jsonPath().get("user_id").toString();
		utils.logPass("API v1 user #1 signup is successful");
		logger.info("API v1 user #1 signup is successful");

		// Navigating to the guest timeline
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// click message gift and gift reward to user
		pageObj.instanceDashboardPage().navigateToGuestTimeline(iFrameEmail);
		pageObj.guestTimelinePage().giftRedeemableToUser(dataSet.get("reward"), dataSet.get("redeemable"));
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "Message sent did not displayed on timeline");

		// fetch user offers/ reward_id using ap2 mobileOffers
		Response offerResponse = pageObj.endpoints().getUserOffers(token, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, offerResponse.getStatusCode(), "Status code 200 did not matched for api2 user offers");
		String reward_id = offerResponse.jsonPath().get("rewards[0].reward_id").toString();
		logger.info(reward_id);
		utils.logPass("Api2 user fetch user offers is successful");

		// Create Redemption using "reward_id" (fetch redemption code)
		Response redemptionResponse = pageObj.endpoints().Api2RedemptionWithRewardId(dataSet.get("client"),
				dataSet.get("secret"), token, reward_id);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, redemptionResponse.getStatusCode(),
				"Status code 201 did not matched for api2 create redemption using reward_id");
		utils.logPass("Api2 Create Redemption using reward_id is successful");

		// get redemption_status from extendedUserHistory api
		Response extendedUserHistoryResponse1 = pageObj.endpoints().extendedUserHistory(userID, dataSet.get("apiKey"));
		Assert.assertEquals(extendedUserHistoryResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Get Extended User History");
		utils.logPass("PLATFORM FUNCTIONS API Get Extended User History is successful");
		String redemptionStatus = extendedUserHistoryResponse1.jsonPath().get("redemptions[0].redemption_status")
				.toString();
		Assert.assertEquals(redemptionStatus, "redeemable", "Redemption Status does not matched");
		logger.info("Verified that redemption_status is redeemable in API response when " + dataSet.get("dbFlag")
				+ " flag is enabled");
		utils.logPass("Verified that redemption_status is redeemable in API response when "
				+ dataSet.get("dbFlag") + " flag is enabled");

		// disable flag from the db
		String query1 = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		pageObj.singletonDBUtilsObj();
		String expColValue1 = DBUtils.executeQueryAndGetColumnValue(env, query1, "preferences");

		pageObj.singletonDBUtilsObj();
		boolean flag1 = DBUtils.updateBusinessesPreference(env, expColValue1, "false", dataSet.get("dbFlag"), b_id);
		Assert.assertTrue(flag1, dataSet.get("dbFlag") + " value is not updated to false");
		logger.info(dataSet.get("dbFlag") + " value is updated to false");
		utils.logit(dataSet.get("dbFlag") + " value is updated to false");

		// verify newly added fields from extendedUserHistory api
		Response extendedUserHistoryResponse2 = pageObj.endpoints().extendedUserHistory(userID, dataSet.get("apiKey"));
		Assert.assertEquals(extendedUserHistoryResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Get Extended User History");
		utils.logPass("PLATFORM FUNCTIONS API Get Extended User History is successful");
		Assert.assertFalse(extendedUserHistoryResponse2.asString().contains("updated_at")
				&& extendedUserHistoryResponse2.asString().contains("processed_at")
				&& extendedUserHistoryResponse2.asString().contains("redemption_status"));
		logger.info("Verified API response not returning newly added fields in case of Redeemable redemption when "
				+ dataSet.get("dbFlag") + " flag is disabled");
		utils.logPass("Verified API response not returning newly added fields in case of Redeemable redemption when "
						+ dataSet.get("dbFlag") + " flag is disabled");

	}

	@SuppressWarnings("unused")
	@Test(description = "SQ-T5549 Verify redemption_status: processed in API response when flag is enabled", priority = 9, groups = {
			"unstable", "regression", "dailyrun" })
	@Owner(name = "Amit Kumar")
	public void T5549_VerifyRedemptionStatus() throws Exception {
		String b_id = dataSet.get("business_id");
		// enable flag from the db
		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		pageObj.singletonDBUtilsObj();
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");
		pageObj.singletonDBUtilsObj();
		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag"), b_id);
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to true");
		logger.info(dataSet.get("dbFlag") + " value is updated to true");
		utils.logit(dataSet.get("dbFlag") + " value is updated to true");
		String iFrameEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse1 = pageObj.endpoints().Api1UserSignUp(iFrameEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 user signup");
		String token = signUpResponse1.jsonPath().get("auth_token.token").toString();
		String authToken = signUpResponse1.jsonPath().get("authentication_token").toString();
		String userIDUser = signUpResponse1.jsonPath().get("user_id").toString();

		// Navigating to the guest timeline
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// click message gift and gift reward to user
		pageObj.instanceDashboardPage().navigateToGuestTimeline(iFrameEmail);
		pageObj.guestTimelinePage().giftRedeemableToUser(dataSet.get("reward"), dataSet.get("Redeemable"));
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "Message sent did not displayed on timeline");

		// login user in IFrame
		pageObj.iframeSingUpPage().navigateToIframe(baseUrl + dataSet.get("whiteLabel") + dataSet.get("slug"));
		pageObj.iframeSingUpPage().iframeLogin(iFrameEmail, Utilities.getApiConfigProperty("password"));

		// generate redemption code via IFrame
		String redemptionCode = pageObj.iframeSingUpPage().redeemRewardOffer(dataSet.get("rewardName"));

		// Pos redemption api
		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key = CreateDateTime.getTimeDateString();
		Response resp = pageObj.endpoints().posRedemptionOfCode(iFrameEmail, date, redemptionCode, key, txn,
				dataSet.get("locationKey"));
		Assert.assertEquals(resp.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for pos redemption api");
		logger.info("POS redemption is successful");
		utils.logPass("POS redemption is successful");

		// get redemption_status from extendedUserHistory api
		Response extendedUserHistoryResponse1 = pageObj.endpoints().extendedUserHistory(userIDUser,
				dataSet.get("apiKey"));
		Assert.assertEquals(extendedUserHistoryResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Get Extended User History");
		utils.logPass("PLATFORM FUNCTIONS API Get Extended User History is successful");
		String redemptionStatus = extendedUserHistoryResponse1.jsonPath().get("redemptions[0].redemption_status")
				.toString();
		Assert.assertEquals(redemptionStatus, dataSet.get("expectedRedemptionStatus"),
				"Redemption Status does not matched");
		logger.info("Verified that redemption_status is processed in API response when " + dataSet.get("dbFlag")
				+ " flag is enabled");
		utils.logPass("Verified that redemption_status is processed in API response when "
				+ dataSet.get("dbFlag") + " flag is enabled");
	}

	// Rakhi
	@Test(description = "SQ-T5556 Verify newly added fields in case of Subscriptions redemption", priority = 10, groups = {
			"regression", "dailyrun" })
	@Owner(name = "Rakhi Rawat")
	public void T5556_verifyNewlyAddedFiledsSubscriptionRedemption() throws Exception {

		String b_id = dataSet.get("business_id");

		// enable flag from the db
		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		pageObj.singletonDBUtilsObj();
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");

		pageObj.singletonDBUtilsObj();
		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag"), b_id);
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to true");
		logger.info(dataSet.get("dbFlag") + " value is updated to true");
		utils.logit(dataSet.get("dbFlag") + " value is updated to true");

		String iFrameEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse1 = pageObj.endpoints().Api1UserSignUp(iFrameEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 user signup");
		String token = signUpResponse1.jsonPath().get("auth_token.token").toString();
		String userID = signUpResponse1.jsonPath().get("user_id").toString();
		utils.logPass("API v1 user #1 signup is successful");
		logger.info("API v1 user #1 signup is successful");

		// Navigating to the guest timeline
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// subscription purchase
		String PlanID = dataSet.get("PlanID");
		String spPrice = dataSet.get("spPrice");
		Response purchaseSubscriptionresponse = pageObj.endpoints().Api2SubscriptionPurchase(token, PlanID,
				dataSet.get("client"), dataSet.get("secret"), spPrice, "");
		Assert.assertEquals(purchaseSubscriptionresponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		String subscription_id = purchaseSubscriptionresponse.jsonPath().get("subscription_id").toString();
		logger.info(iFrameEmail + " purchased " + subscription_id + " Plan id = " + PlanID);

		// Create Redemption using subscription (fetch redemption code)
		Response card = pageObj.endpoints().Api2SubscriptionRedemption(dataSet.get("client"), dataSet.get("secret"),
				subscription_id, token);
		Assert.assertEquals(card.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED, "Status code 201 did not matched for pos redemption api");
		logger.info("Api2 Subscription redemption is successful");
		utils.logPass("Api2 Subscription redemption is successful");

		// verify newly added fields from extendedUserHistory api
		Response extendedUserHistoryResponse2 = pageObj.endpoints().extendedUserHistory(userID, dataSet.get("apiKey"));
		Assert.assertEquals(extendedUserHistoryResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Get Extended User History");
		utils.logPass("PLATFORM FUNCTIONS API Get Extended User History is successful");
		String redemptionType = extendedUserHistoryResponse2.jsonPath().get("redemptions[0].type").toString();
		Assert.assertEquals(redemptionType, "SubscriptionRedemption", "Redemption Status does not matched");

		String jsonObjectString = extendedUserHistoryResponse2.asString();
		JSONObject finalResponse = new JSONObject(jsonObjectString);

		Boolean field1 = finalResponse.getJSONArray("redemptions").getJSONObject(0).has("updated_at");
		Assert.assertEquals(field1, true, "updated_at field not found");
		logger.info("Verified updated_at field found in case of Subscriptions redemption when " + dataSet.get("dbFlag")
				+ " flag is enabled");
		utils.logPass("Verified updated_at field found in case of Subscriptions redemption when "
				+ dataSet.get("dbFlag") + " flag is enabled");

		Boolean field2 = finalResponse.getJSONArray("redemptions").getJSONObject(0).has("processed_at");
		Assert.assertEquals(field2, true, "processed_at field not found");
		logger.info("Verified processed_at field found in case of Subscriptions redemption when "
				+ dataSet.get("dbFlag") + " flag is enabled");
		utils.logPass("Verified processed_at field found in case of Subscriptions redemption when "
						+ dataSet.get("dbFlag") + " flag is enabled");

		Boolean field3 = finalResponse.getJSONArray("redemptions").getJSONObject(0).has("redemption_status");
		Assert.assertEquals(field3, true, "redemption_status field not found");
		logger.info("Verified redemption_status field found in case of Subscriptions redemption when "
				+ dataSet.get("dbFlag") + " flag is enabled");
		utils.logPass("Verified redemption_status field found in Subscriptions of Card redemption when "
						+ dataSet.get("dbFlag") + " flag is enabled");

	}

	@Test(description = "SQ-T5557 Verify newly added fields in case of Card redemption", priority = 11, groups = {
			"unstable" })
	@Owner(name = "Rakhi Rawat")
	public void T5557_VerifyNewlyAddedFiledsCardRedemption() throws Exception {

		String b_id = dataSet.get("business_id");

		// enable flag from the db
		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		pageObj.singletonDBUtilsObj();
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");

		pageObj.singletonDBUtilsObj();
		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag"), b_id);
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to true");
		logger.info(dataSet.get("dbFlag") + " value is updated to true");
		utils.logit(dataSet.get("dbFlag") + " value is updated to true");

		String iFrameEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse1 = pageObj.endpoints().Api1UserSignUp(iFrameEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 user signup");
		signUpResponse1.jsonPath().get("auth_token.token").toString();
		String userID = signUpResponse1.jsonPath().get("user_id").toString();
		utils.logPass("API v1 user #1 signup is successful");
		logger.info("API v1 user #1 signup is successful");

		// click message gift and gift orders visits
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.instanceDashboardPage().navigateToGuestTimeline(iFrameEmail);

		pageObj.guestTimelinePage().messageOrdersToUser(dataSet.get("subject"), dataSet.get("giftTypes"),
				dataSet.get("giftOrders"), dataSet.get("giftReason"));
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "Message sent did not displayed on timeline");

		// Pos redemption api
		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key = CreateDateTime.getTimeDateString();
		Response respo = pageObj.endpoints().posRedemptionOfCard(iFrameEmail, date, key, txn,
				dataSet.get("locationKey"));
		Assert.assertEquals(respo.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for pos redemption api");
		logger.info("POS card redemption is successful");
		utils.logPass("POS card redemption is successful");

		// verify newly added fields from extendedUserHistory api
		Response extendedUserHistoryResponse2 = pageObj.endpoints().extendedUserHistory(userID, dataSet.get("apiKey"));
		Assert.assertEquals(extendedUserHistoryResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Get Extended User History");
		utils.logPass("PLATFORM FUNCTIONS API Get Extended User History is successful");
		String redemptionType = extendedUserHistoryResponse2.jsonPath().get("redemptions[0].type").toString();
		Assert.assertEquals(redemptionType, "CardRedemption", "Redemption Status does not matched");

		String jsonObjectString = extendedUserHistoryResponse2.asString();
		JSONObject finalResponse = new JSONObject(jsonObjectString);

		Boolean field1 = finalResponse.getJSONArray("redemptions").getJSONObject(0).has("updated_at");
		Assert.assertEquals(field1, true, "updated_at field not found");
		logger.info("Verified updated_at field found in case of Card redemption when " + dataSet.get("dbFlag")
				+ " flag is enabled");
		utils.logPass("Verified updated_at field found in case of Card redemption when "
				+ dataSet.get("dbFlag") + " flag is enabled");

		Boolean field2 = finalResponse.getJSONArray("redemptions").getJSONObject(0).has("processed_at");
		Assert.assertEquals(field2, true, "processed_at field not found");
		logger.info("Verified processed_at field found in case of Card redemption when " + dataSet.get("dbFlag")
				+ " flag is enabled");
		utils.logPass("Verified processed_at field found in case of Card redemption when "
				+ dataSet.get("dbFlag") + " flag is enabled");

		Boolean field3 = finalResponse.getJSONArray("redemptions").getJSONObject(0).has("redemption_status");
		Assert.assertEquals(field3, true, "redemption_status field not found");
		logger.info("Verified redemption_status field found in case of Card redemption when " + dataSet.get("dbFlag")
				+ " flag is enabled");
		utils.logPass("Verified redemption_status field found in case of Card redemption when "
				+ dataSet.get("dbFlag") + " flag is enabled");

	}

	// Rakhi
	@Test(description = "SQ-T5558 Verify newly added fields in case of reward redemption", priority = 12, groups = {
			"regression", "dailyrun" })
	@Owner(name = "Rakhi Rawat")
	public void T5558_verifyNewlyAddedFiledsRewardRedemption() throws Exception {

		String b_id = dataSet.get("business_id");

		// enable flag from the db
		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		pageObj.singletonDBUtilsObj();
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");

		pageObj.singletonDBUtilsObj();
		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag"), b_id);
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to true");
		logger.info(dataSet.get("dbFlag") + " value is updated to true");
		utils.logit(dataSet.get("dbFlag") + " value is updated to true");

		String iFrameEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse1 = pageObj.endpoints().Api1UserSignUp(iFrameEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 user signup");
		String token = signUpResponse1.jsonPath().get("auth_token.token").toString();
		String userID = signUpResponse1.jsonPath().get("user_id").toString();
		utils.logPass("API v1 user #1 signup is successful");
		logger.info("API v1 user #1 signup is successful");

		// Navigating to the guest timeline
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// click message gift and gift reward to user
		pageObj.instanceDashboardPage().navigateToGuestTimeline(iFrameEmail);
		pageObj.guestTimelinePage().giftRedeemableToUser(dataSet.get("reward"), dataSet.get("redeemable"));
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "Message sent did not displayed on timeline");

		// fetch user offers/ reward_id using ap2 mobileOffers
		Response offerResponse = pageObj.endpoints().getUserOffers(token, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(offerResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 user offers");
		String reward_id = offerResponse.jsonPath().get("rewards[0].reward_id").toString();
		logger.info(reward_id);
		utils.logPass("Api2 user fetch user offers is successful");

		// Create Redemption using "reward_id" (fetch redemption code)
		Response redemptionResponse = pageObj.endpoints().Api2RedemptionWithRewardId(dataSet.get("client"),
				dataSet.get("secret"), token, reward_id);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, redemptionResponse.getStatusCode(),
				"Status code 201 did not matched for api2 create redemption using reward_id");
		String redemptionTrackingCode = redemptionResponse.jsonPath().get("redemption_tracking_code").toString();
		utils.logPass("Api2 Create Redemption using reward_id is successful");

		// get redemption_status from extendedUserHistory api
		Response extendedUserHistoryResponse1 = pageObj.endpoints().extendedUserHistory(userID, dataSet.get("apiKey"));
		Assert.assertEquals(extendedUserHistoryResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Get Extended User History");
		utils.logPass("PLATFORM FUNCTIONS API Get Extended User History is successful");

		String internalTrackingCode = extendedUserHistoryResponse1.jsonPath()
				.get("redemptions[0].internal_tracking_code").toString();
		Assert.assertEquals(internalTrackingCode, redemptionTrackingCode, "code did not mathed");

		String redemptionType = extendedUserHistoryResponse1.jsonPath().get("redemptions[0].type").toString();
		Assert.assertEquals(redemptionType, "RewardRedemption", "Redemption Status does not matched");
//		Assert.assertTrue(extendedUserHistoryResponse1.asString().contains("updated_at")
//				&& extendedUserHistoryResponse1.asString().contains("processed_at")
//				&& extendedUserHistoryResponse1.asString().contains("redemption_status"));
//		logger.info("Verified newly added fields in case of Reward redemption when " + dataSet.get("dbFlag")
//				+ " flag is enabled");
//		utils.logPass("Verified newly added fields in case of Reward redemption when "
//				+ dataSet.get("dbFlag") + " flag is enabled");

		String jsonObjectString = extendedUserHistoryResponse1.asString();
		JSONObject finalResponse = new JSONObject(jsonObjectString);

		Boolean field1 = finalResponse.getJSONArray("redemptions").getJSONObject(0).has("updated_at");
		Assert.assertEquals(field1, true, "updated_at field not found");
		logger.info("Verified updated_at field found in case of reward redemption when " + dataSet.get("dbFlag")
				+ " flag is enabled");
		utils.logPass("Verified updated_at field found in case of reward redemption when "
				+ dataSet.get("dbFlag") + " flag is enabled");

		Boolean field2 = finalResponse.getJSONArray("redemptions").getJSONObject(0).has("processed_at");
		Assert.assertEquals(field2, true, "processed_at field not found");
		logger.info("Verified processed_at field found in case of reward redemption when " + dataSet.get("dbFlag")
				+ " flag is enabled");
		utils.logPass("Verified processed_at field found in case of reward redemption when "
				+ dataSet.get("dbFlag") + " flag is enabled");

		Boolean field3 = finalResponse.getJSONArray("redemptions").getJSONObject(0).has("redemption_status");
		Assert.assertEquals(field3, true, "redemption_status field not found");
		logger.info("Verified redemption_status field found in case of reward redemption when " + dataSet.get("dbFlag")
				+ " flag is enabled");
		utils.logPass("Verified redemption_status field found in reward of Card redemption when "
				+ dataSet.get("dbFlag") + " flag is enabled");
	}

	// Rakhi
	@Test(description = "SQ-T5559 Verify newly added fields in case of redeemable redemption", priority = 13)
	@Owner(name = "Rakhi Rawat")
	public void T5559_verifyNewlyAddedFiledsRedeemableRedemption() throws Exception {

		String b_id = dataSet.get("business_id");

		// enable flag from the db
		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		pageObj.singletonDBUtilsObj();
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");

		pageObj.singletonDBUtilsObj();
		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag"), b_id);
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to true");
		logger.info(dataSet.get("dbFlag") + " value is updated to true");
		utils.logit(dataSet.get("dbFlag") + " value is updated to true");

		String iFrameEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse1 = pageObj.endpoints().Api1UserSignUp(iFrameEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 user signup");
		String token = signUpResponse1.jsonPath().get("auth_token.token").toString();
		String userID = signUpResponse1.jsonPath().get("user_id").toString();
		utils.logPass("API v1 user #1 signup is successful");
		logger.info("API v1 user #1 signup is successful");

		// send points to user
		Response sendPointsResponse = pageObj.endpoints().sendPointsToUser(userID, "110", dataSet.get("apiKey"));
		Assert.assertEquals(sendPointsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send points to user");
		utils.logPass("Api2  send gift points to user is successful");

		// Create Redemption using redeemable id
		Response redeemableRedemptionResp = pageObj.endpoints().Api2RedemptionWitReedemableIdAndLocationId(
				dataSet.get("client"), dataSet.get("secret"), token, dataSet.get("redeemableId"),
				dataSet.get("locationId"));
		Assert.assertEquals(redeemableRedemptionResp.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send points to user");
		String redemptionTrackingCode = redeemableRedemptionResp.jsonPath().get("redemption_tracking_code").toString();

		logger.info("Verified Redemption using redeemable is not possible at Disapproved Location when Flag is ON");
		utils.logPass("Verified Redemption using redeemable is not possible at Disapproved Location when Flag is ON");

		// get redemption_status from extendedUserHistory api
		Response extendedUserHistoryResponse1 = pageObj.endpoints().extendedUserHistory(userID, dataSet.get("apiKey"));
		Assert.assertEquals(extendedUserHistoryResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Get Extended User History");
		utils.logPass("PLATFORM FUNCTIONS API Get Extended User History is successful");
		String redemptionType = extendedUserHistoryResponse1.jsonPath().get("redemptions[0].type").toString();
		Assert.assertEquals(redemptionType, "RedeemableRedemption", "Redemption Status does not matched");

		String internalTrackingCode = extendedUserHistoryResponse1.jsonPath()
				.get("redemptions[0].internal_tracking_code").toString();
		Assert.assertEquals(internalTrackingCode, redemptionTrackingCode, "code did not mathed");

		String jsonObjectString = extendedUserHistoryResponse1.asString();
		JSONObject finalResponse = new JSONObject(jsonObjectString);

		Boolean field1 = finalResponse.getJSONArray("redemptions").getJSONObject(0).has("updated_at");
		Assert.assertEquals(field1, true, "updated_at field not found");
		logger.info("Verified updated_at field found in case of redeemable redemption when " + dataSet.get("dbFlag")
				+ " flag is enabled");
		utils.logPass("Verified updated_at field found in case of redeemable redemption when "
				+ dataSet.get("dbFlag") + " flag is enabled");

		Boolean field2 = finalResponse.getJSONArray("redemptions").getJSONObject(0).has("processed_at");
		Assert.assertEquals(field2, true, "processed_at field not found");
		logger.info("Verified processed_at field found in case of redeemable redemption when " + dataSet.get("dbFlag")
				+ " flag is enabled");
		utils.logPass("Verified processed_at field found in case of redeemable redemption when "
				+ dataSet.get("dbFlag") + " flag is enabled");

		Boolean field3 = finalResponse.getJSONArray("redemptions").getJSONObject(0).has("redemption_status");
		Assert.assertEquals(field3, true, "redemption_status field not found");
		logger.info("Verified redemption_status field found in case of redeemable redemption when "
				+ dataSet.get("dbFlag") + " flag is enabled");
		utils.logPass("Verified redemption_status field found in redeemable of Card redemption when "
						+ dataSet.get("dbFlag") + " flag is enabled");

	}

	// Rakhi
	@Test(description = "SQ-T5566 Verify processed_at is updated once redemption is honored", priority = 14, groups = {
			"regression", "dailyrun" })
	@Owner(name = "Rakhi Rawat")
	public void T5566_VerifyStatusOfProcessedAt() throws InterruptedException {

		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse1 = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 user signup");
		String userID = signUpResponse1.jsonPath().get("user_id").toString();
		utils.logPass("API v1 user #1 signup is successful");
		logger.info("API v1 user #1 signup is successful");

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
		logger.info("Redemption code is " + redemptionCode);

		// verify status of processed_at from extendedUserHistory api
		Response extendedUserHistoryResponse1 = pageObj.endpoints().extendedUserHistory(userID, dataSet.get("apiKey"));
		Assert.assertEquals(extendedUserHistoryResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Get Extended User History");
		utils.logPass("PLATFORM FUNCTIONS API Get Extended User History is successful");
		String status = extendedUserHistoryResponse1.jsonPath().get("redemptions[0].processed_at");
		Assert.assertNull(status, "Status of processed_at is not null");
		logger.info("Verified status of processed_at does not update when redemption is not honoured");
		utils.logPass("Verified status of processed_at does not update when redemption is not honoured");

		// Pos redemption api
		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key = CreateDateTime.getTimeDateString();
		Response posRedemptionResp = pageObj.endpoints().posRedemptionOfCode(userEmail, date, redemptionCode, key, txn,
				dataSet.get("locationkey"));
		Assert.assertEquals(posRedemptionResp.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for POS redemption");
		logger.info("POS redemption is successful");
		utils.logPass("POS redemption is successful");

		// verify status of processed_at from extendedUserHistory api
		Response extendedUserHistoryResponse2 = pageObj.endpoints().extendedUserHistory(userID, dataSet.get("apiKey"));
		Assert.assertEquals(extendedUserHistoryResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Get Extended User History");
		utils.logPass("PLATFORM FUNCTIONS API Get Extended User History is successful");
		String internalTrackingCode = extendedUserHistoryResponse2.jsonPath()
				.get("redemptions[0].internal_tracking_code").toString();
		Assert.assertEquals(internalTrackingCode, redemptionCode, "code did not mathed");

		String status1 = extendedUserHistoryResponse2.jsonPath().get("redemptions[0].processed_at").toString();
		Assert.assertNotNull(status1, "Status of processed_at is null");
		logger.info("Verified status of processed_at is updated when redemption is honoured");
		utils.logPass("Verified status of processed_at is updated when redemption is honoured");
	}

	// Rakhi
	@Test(description = "SQ-T5669 Verify updated_at is updated once the status is changed", priority = 15, groups = {
			"regression", "dailyrun" })
	@Owner(name = "Rakhi Rawat")
	public void T5669_VerifyStatusOfUpdatedAt() throws Exception {

		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse1 = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 user signup");
		String userID = signUpResponse1.jsonPath().get("user_id").toString();
		utils.logPass("API v1 user #1 signup is successful");
		logger.info("API v1 user #1 signup is successful");

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

		String query1 = "SELECT `updated_at` FROM `redemptions` where `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		pageObj.singletonDBUtilsObj();
		String updated_At = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query1, "updated_at", 5);

		// verify status of processed_at from extendedUserHistory api
		Response extendedUserHistoryResponse1 = pageObj.endpoints().extendedUserHistory(userID, dataSet.get("apiKey"));
		Assert.assertEquals(extendedUserHistoryResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Get Extended User History");
		utils.logPass("PLATFORM FUNCTIONS API Get Extended User History is successful");
		String value = extendedUserHistoryResponse1.jsonPath().get("redemptions[0].updated_at");
		String updatedValue = value.replace("Z", "").replace("T", " ");
//		Assert.assertNull(status, "Status of processed_at is not null");
		Assert.assertEquals(updatedValue, updated_At, "updated_at did not not matched");
		logger.info("Verified status of updated_at does not update when redemption is not honoured");
		utils.logPass("Verified status of updated_at does not update when redemption is not honoured");

		// Pos redemption api
		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key = CreateDateTime.getTimeDateString();
		Response posRedemptionResp = pageObj.endpoints().posRedemptionOfCode(userEmail, date, redemptionCode, key, txn,
				dataSet.get("locationkey"));
		Assert.assertEquals(posRedemptionResp.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for POS redemption");
		logger.info("POS redemption is successful");
		utils.logPass("POS redemption is successful");

		String query2 = "SELECT `updated_at` FROM `redemptions` where `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		pageObj.singletonDBUtilsObj();
		String updated_At2 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query2, "updated_at", 5);

		// verify status of processed_at from extendedUserHistory api
		Response extendedUserHistoryResponse2 = pageObj.endpoints().extendedUserHistory(userID, dataSet.get("apiKey"));
		Assert.assertEquals(extendedUserHistoryResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Get Extended User History");
		utils.logPass("PLATFORM FUNCTIONS API Get Extended User History is successful");
		String internalTrackingCode = extendedUserHistoryResponse2.jsonPath()
				.get("redemptions[0].internal_tracking_code").toString();
		Assert.assertEquals(internalTrackingCode, redemptionCode, "code did not mathed");

		String value2 = extendedUserHistoryResponse2.jsonPath().get("redemptions[0].updated_at");
		String updatedValue2 = value2.replace("Z", "").replace("T", " ");
		Assert.assertEquals(updatedValue2, updated_At2, "updated_at did not not matched");
		logger.info("Verified status of updated_at is updated when redemption is honoured");
		utils.logPass("Verified status of updated_at is updated when redemption is honoured");
	}

	// Rakhi
	// transfer non-loyalty reward to other user and verify response when
	// "enable_v2_for_extensive_timeline_api" is true
	@Test(description = "SQ-T5758 Verify transferred redemption details in api2/dashboard/users/extensive_timeline API response for user who has transferred reward [non-loyalty] and enable_v2_for_extensive_timeline_api is true", groups = {
			"regression", "dailyrun" })
	@Owner(name = "Rakhi Rawat")
	public void T5758_VerifyTransferredRewardRedemptionWhenFlagEnabled() throws Exception {

		String b_id = dataSet.get("business_id");

		// enable "enable_v2_for_extensive_timeline_api" flag from the db
		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		pageObj.singletonDBUtilsObj();
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");

		pageObj.singletonDBUtilsObj();
		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag"), b_id);
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to true");
		logger.info(dataSet.get("dbFlag") + " value is updated to true");
		utils.logit(dataSet.get("dbFlag") + " value is updated to true");

		// User register/signup using API2 Signup
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("Api2 user signup is successful");

		// send reward to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				dataSet.get("redeemable_id"), "", "");
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send message to user");
		utils.logPass("Api2  send reward reedemable to user is successful");

		// fetch user offers/ reward_id using ap2 mobileOffers
		Response offerResponse = pageObj.endpoints().getUserOffers(token, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(offerResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 user offers");
		String reward_id = offerResponse.jsonPath().get("rewards[0].reward_id").toString();
		logger.info(reward_id);
		utils.logPass("Api2 user fetch user offers is successful");

		// Sign-up other user
		String newUserEmail = pageObj.iframeSingUpPage().generateEmail();
		Response resp = pageObj.endpoints().Api2SignUp(newUserEmail, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(resp.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match for API v2 User Signup");

		// Gift non-loyalty reward to other user
		Response giftRewardResponse = pageObj.endpoints().Api2GiftRewardToUser(dataSet.get("client"),
				dataSet.get("secret"), reward_id, newUserEmail, token);
		Assert.assertEquals(giftRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for Gift loyalty reward to other user");

		// api2/dashboard/users/extensive_timeline
		Response extendedUserHistoryResponse1 = pageObj.endpoints().extendedUserHistory(userID, dataSet.get("apiKey"));
		Assert.assertEquals(extendedUserHistoryResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Get Extended User History");
		utils.logPass("PLATFORM FUNCTIONS API Get Extended User History is successful");
		String jsonObjectString = extendedUserHistoryResponse1.asString();
		JSONObject finalResponse = new JSONObject(jsonObjectString);

		Boolean field1 = finalResponse.getJSONArray("redemptions").getJSONObject(0).has("updated_at");
		Assert.assertEquals(field1, true, "updated_at field not found");
		logger.info("Verified updated_at field found in case of transferred reward [non-loyalty] redemption when "
				+ dataSet.get("dbFlag") + " flag is enabled");
		utils.logPass("Verified updated_at field found in case of transferred reward [non-loyalty] redemption when "
						+ dataSet.get("dbFlag") + " flag is enabled");

		Boolean field2 = finalResponse.getJSONArray("redemptions").getJSONObject(0).has("processed_at");
		Assert.assertEquals(field2, true, "processed_at field not found");
		logger.info("Verified processed_at field found in case of transferred reward [non-loyalty] redemption when "
				+ dataSet.get("dbFlag") + " flag is enabled");
		utils.logPass("Verified processed_at field found in case of transferred reward [non-loyalty] redemption when "
						+ dataSet.get("dbFlag") + " flag is enabled");

		Boolean field3 = finalResponse.getJSONArray("redemptions").getJSONObject(0).has("redemption_status");
		Assert.assertEquals(field3, true, "redemption_status field not found");
		logger.info(
				"Verified redemption_status field found in case of transferred reward [non-loyalty] redemption when "
						+ dataSet.get("dbFlag") + " flag is enabled");
		utils.logPass(
				"Verified redemption_status field found in case of transferred reward [non-loyalty] redemption when "
						+ dataSet.get("dbFlag") + " flag is enabled");

		String redemptionType = extendedUserHistoryResponse1.jsonPath().get("redemptions[0].type").toString();
		String status = extendedUserHistoryResponse1.jsonPath().get("redemptions[0].status");
		String internalTrackingCode = extendedUserHistoryResponse1.jsonPath()
				.get("redemptions[0].internal_tracking_code");
		String redemptionStatus = extendedUserHistoryResponse1.jsonPath().get("redemptions[0].redemption_status");
		String processedAt = extendedUserHistoryResponse1.jsonPath().get("redemptions[0].processed_at");

		Assert.assertEquals(redemptionType, "RewardRedemption", "Redemption Status does not matched");
		Assert.assertNull(status, "Value of status is not null");
		Assert.assertNull(internalTrackingCode, "Value of internal_tracking_code is not null");
		Assert.assertNull(redemptionStatus, "Value of redemption_status is not null");
		Assert.assertNull(processedAt, "Value of processed_at is not null");
		logger.info(
				"Verify transferred redemption details in api2/dashboard/users/extensive_timeline API response for user who has transferred reward and "
						+ dataSet.get("dbFlag") + " flag is enabled");
		utils.logPass(
				"Verify transferred redemption details in api2/dashboard/users/extensive_timeline API response for user who has transferred reward and "
						+ dataSet.get("dbFlag") + " flag is enabled");

	}

	// Rakhi
	// transfer currency to other user and verify response when
	// "enable_v2_for_extensive_timeline_api" is true
	@Test(description = "SQ-T5759 Verify transferred redemption details in api2/dashboard/users/extensive_timeline API response for user who has transferred currency and enable_v2_for_extensive_timeline_api is true", groups = {
			"regression", "dailyrun" })
	@Owner(name = "Rakhi Rawat")
	public void T5759_VerifyTransferredCurrencyRedemptionWhenFlagEnabled() throws Exception {

		String b_id = dataSet.get("business_id");

		// enable "enable_v2_for_extensive_timeline_api" flag from the db
		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		pageObj.singletonDBUtilsObj();
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");

		pageObj.singletonDBUtilsObj();
		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag"), b_id);
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to true");
		logger.info(dataSet.get("dbFlag") + " value is updated to true");
		utils.logit(dataSet.get("dbFlag") + " value is updated to true");

		// User register/signup using API2 Signup
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("Api2 user signup is successful");

		// send reward amount to user Amount
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"),
				dataSet.get("amount"), "", "", "");
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send message to user");
		utils.logPass("Api2  send reward amount to user is successful");

		// Sign-up other user
		String newUserEmail = pageObj.iframeSingUpPage().generateEmail();
		Response resp = pageObj.endpoints().Api2SignUp(newUserEmail, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(resp.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match for API v2 User Signup");

		// Gift Banked Currency to other user
		Response giftRewardResponse = pageObj.endpoints().Api2GiftAmountToUser(dataSet.get("client"),
				dataSet.get("secret"), newUserEmail, token);
		Assert.assertEquals(giftRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v2 Gift Banked Currency to other user");

		// api2/dashboard/users/extensive_timeline
		Response extendedUserHistoryResponse1 = pageObj.endpoints().extendedUserHistory(userID, dataSet.get("apiKey"));
		Assert.assertEquals(extendedUserHistoryResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Get Extended User History");
		utils.logPass("PLATFORM FUNCTIONS API Get Extended User History is successful");
		String jsonObjectString = extendedUserHistoryResponse1.asString();
		JSONObject finalResponse = new JSONObject(jsonObjectString);

		Boolean field1 = finalResponse.getJSONArray("redemptions").getJSONObject(0).has("updated_at");
		Assert.assertEquals(field1, true, "updated_at field not found");
		logger.info("Verified updated_at field found in case of transferred currency redemption when "
				+ dataSet.get("dbFlag") + " flag is enabled");
		utils.logPass("Verified updated_at field found in case of transferred currency redemption when "
						+ dataSet.get("dbFlag") + " flag is enabled");

		Boolean field2 = finalResponse.getJSONArray("redemptions").getJSONObject(0).has("processed_at");
		Assert.assertEquals(field2, true, "processed_at field not found");
		logger.info("Verified processed_at field found in case of transferred currency redemption when "
				+ dataSet.get("dbFlag") + " flag is enabled");
		utils.logPass("Verified processed_at field found in case of transferred currency redemption when "
						+ dataSet.get("dbFlag") + " flag is enabled");

		Boolean field3 = finalResponse.getJSONArray("redemptions").getJSONObject(0).has("redemption_status");
		Assert.assertEquals(field3, true, "redemption_status field not found");
		logger.info("Verified redemption_status field found in case of transferred currency redemption when "
				+ dataSet.get("dbFlag") + " flag is enabled");
		utils.logPass("Verified redemption_status field found in case of transferred currency redemption when "
						+ dataSet.get("dbFlag") + " flag is enabled");

		String redemptionType = extendedUserHistoryResponse1.jsonPath().get("redemptions[0].type").toString();
		String status = extendedUserHistoryResponse1.jsonPath().get("redemptions[0].status");
		String internalTrackingCode = extendedUserHistoryResponse1.jsonPath()
				.get("redemptions[0].internal_tracking_code");
		String redemptionStatus = extendedUserHistoryResponse1.jsonPath().get("redemptions[0].redemption_status");
		String processedAt = extendedUserHistoryResponse1.jsonPath().get("redemptions[0].processed_at");

		Assert.assertEquals(redemptionType, "BankedRewardRedemption", "Redemption Status does not matched");
		Assert.assertNull(status, "Value of status is not null");
		Assert.assertNull(internalTrackingCode, "Value of internal_tracking_code is not null");
		Assert.assertNull(redemptionStatus, "Value of redemption_status is not null");
		Assert.assertNull(processedAt, "Value of processed_at is not null");
		logger.info(
				"Verify transferred redemption details in api2/dashboard/users/extensive_timeline API response for user who has transferred currency and "
						+ dataSet.get("dbFlag") + " flag is enabled");
		utils.logPass(
				"Verify transferred redemption details in api2/dashboard/users/extensive_timeline API response for user who has transferred currency and "
						+ dataSet.get("dbFlag") + " flag is enabled");

	}

	// Rakhi
	// transfer points to other user and verify response when
	// "enable_v2_for_extensive_timeline_api" is true
	@Test(description = "SQ-5770 Verify transferred redemption details in api2/dashboard/users/extensive_timeline API response for user who has transferred points and enable_v2_for_extensive_timeline_api is false", groups = {
			"regression", "dailyrun" })
	@Owner(name = "Rakhi Rawat")
	public void T5770_VerifyTransferredPointRedemptionWhenFlagEnabled() throws Exception {

		String b_id = dataSet.get("business_id");

		// enable "enable_v2_for_extensive_timeline_api" flag from the db
		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		pageObj.singletonDBUtilsObj();
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");

		pageObj.singletonDBUtilsObj();
		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag"), b_id);
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to true");
		logger.info(dataSet.get("dbFlag") + " value is updated to true");
		utils.logit(dataSet.get("dbFlag") + " value is updated to true");

		// User register/signup using API2 Signup
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("Api2 user signup is successful");

		// User sign-up for user #2
		String userEmail2 = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse2 = pageObj.endpoints().Api1UserSignUp(userEmail2, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 user signup");

		// Send points to user #1
		Response sendMessageToUserResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				"", "", dataSet.get("points"));
		Assert.assertEquals(sendMessageToUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Send reward amount to user");
		utils.logPass("Send points to user #1 is successful");
		logger.info("Send points to user #1 is successful");

		// Loyalty points transfer to user #2
		Response transferPointsResponse = pageObj.endpoints().Api1TransferLoyaltyPointsToUser(userEmail2,
				dataSet.get("points"), dataSet.get("client"), dataSet.get("secret"), token);
		Assert.assertEquals(transferPointsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 transfer loyalty points to other user");
		utils.logPass("API v1 Loyalty points transfer to user #2 is successful");
		logger.info("API v1 Loyalty points transfer to user #2 is successful");

		// api2/dashboard/users/extensive_timeline
		Response extendedUserHistoryResponse1 = pageObj.endpoints().extendedUserHistory(userID, dataSet.get("apiKey"));
		Assert.assertEquals(extendedUserHistoryResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Get Extended User History");
		utils.logPass("PLATFORM FUNCTIONS API Get Extended User History is successful");
		String jsonObjectString = extendedUserHistoryResponse1.asString();
		JSONObject finalResponse = new JSONObject(jsonObjectString);

		Boolean field1 = finalResponse.getJSONArray("redemptions").getJSONObject(0).has("updated_at");
		Assert.assertEquals(field1, true, "updated_at field not found");
		logger.info("Verified updated_at field found in case of transferred points redemption when "
				+ dataSet.get("dbFlag") + " flag is enabled");
		utils.logPass("Verified updated_at field found in case of transferred points redemption when "
						+ dataSet.get("dbFlag") + " flag is enabled");

		Boolean field2 = finalResponse.getJSONArray("redemptions").getJSONObject(0).has("processed_at");
		Assert.assertEquals(field2, true, "processed_at field not found");
		logger.info("Verified processed_at field found in case of transferred points redemption when "
				+ dataSet.get("dbFlag") + " flag is enabled");
		utils.logPass("Verified processed_at field found in case of transferred points redemption when "
						+ dataSet.get("dbFlag") + " flag is enabled");

		Boolean field3 = finalResponse.getJSONArray("redemptions").getJSONObject(0).has("redemption_status");
		Assert.assertEquals(field3, true, "redemption_status field not found");
		logger.info("Verified redemption_status field found in case of transferred points redemption when "
				+ dataSet.get("dbFlag") + " flag is enabled");
		utils.logPass("Verified redemption_status field found in case of transferred points redemption when "
						+ dataSet.get("dbFlag") + " flag is enabled");

		String redemptionType = extendedUserHistoryResponse1.jsonPath().get("redemptions[0].type").toString();
		String status = extendedUserHistoryResponse1.jsonPath().get("redemptions[0].status");
		String internalTrackingCode = extendedUserHistoryResponse1.jsonPath()
				.get("redemptions[0].internal_tracking_code");
		String redemptionStatus = extendedUserHistoryResponse1.jsonPath().get("redemptions[0].redemption_status");
		String processedAt = extendedUserHistoryResponse1.jsonPath().get("redemptions[0].processed_at");

		Assert.assertEquals(redemptionType, "UnbankedPointRedemption", "Redemption Status does not matched");
		Assert.assertNull(status, "Value of status is not null");
		Assert.assertNull(internalTrackingCode, "Value of internal_tracking_code is not null");
		Assert.assertNull(redemptionStatus, "Value of redemption_status is not null");
		Assert.assertNull(processedAt, "Value of processed_at is not null");
		logger.info(
				"Verify transferred redemption details in api2/dashboard/users/extensive_timeline API response for user who has transferred points and "
						+ dataSet.get("dbFlag") + " flag is enabled");
		utils.logPass(
				"Verify transferred redemption details in api2/dashboard/users/extensive_timeline API response for user who has transferred points and "
						+ dataSet.get("dbFlag") + " flag is enabled");

	}

	// Rakhi
	// transfer non-loyalty reward to other user and verify response when
	// "enable_v2_for_extensive_timeline_api" is false
	@Test(description = "SQ-T5771 Verify transferred redemption details in api2/dashboard/users/extensive_timeline API response for user who has transferred reward [non-loyalty] and enable_v2_for_extensive_timeline_api is false", groups = {
			"regression", "dailyrun" })
	@Owner(name = "Rakhi Rawat")
	public void T5771_VerifyTransferredRewardRedemptionWhenFlagDisabled() throws Exception {

		String b_id = dataSet.get("business_id");

		// disable "enable_v2_for_extensive_timeline_api" flag from the db
		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		pageObj.singletonDBUtilsObj();
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");

		pageObj.singletonDBUtilsObj();
		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, "false", dataSet.get("dbFlag"), b_id);
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to true");
		logger.info(dataSet.get("dbFlag") + " value is updated to false");
		utils.logit(dataSet.get("dbFlag") + " value is updated to false");

		// User register/signup using API2 Signup
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("Api2 user signup is successful");

		// send reward to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				dataSet.get("redeemable_id"), "", "");
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send message to user");
		utils.logPass("Api2  send reward reedemable to user is successful");

		// fetch user offers/ reward_id using ap2 mobileOffers
		Response offerResponse = pageObj.endpoints().getUserOffers(token, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(offerResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 user offers");
		String reward_id = offerResponse.jsonPath().get("rewards[0].reward_id").toString();
		logger.info(reward_id);
		utils.logPass("Api2 user fetch user offers is successful");

		// Sign-up other user
		String newUserEmail = pageObj.iframeSingUpPage().generateEmail();
		Response resp = pageObj.endpoints().Api2SignUp(newUserEmail, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(resp.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match for API v2 User Signup");

		// Gift loyalty reward to other user
		Response giftRewardResponse = pageObj.endpoints().Api2GiftRewardToUser(dataSet.get("client"),
				dataSet.get("secret"), reward_id, newUserEmail, token);
		Assert.assertEquals(giftRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for Gift loyalty reward to other user");

		// api2/dashboard/users/extensive_timeline
		Response extendedUserHistoryResponse1 = pageObj.endpoints().extendedUserHistory(userID, dataSet.get("apiKey"));
		Assert.assertEquals(extendedUserHistoryResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Get Extended User History");

		String jsonObjectString = extendedUserHistoryResponse1.asString();
		JSONObject finalResponse = new JSONObject(jsonObjectString);

		Boolean field1 = finalResponse.getJSONArray("redemptions").getJSONObject(0).has("updated_at");
		Assert.assertEquals(field1, false, "updated_at field found");
		logger.info("Verified updated_at field not found in case of transferred reward redemption when "
				+ dataSet.get("dbFlag") + " flag is disabled");
		utils.logPass("Verified updated_at field not found in case of transferred reward redemption when "
						+ dataSet.get("dbFlag") + " flag is disabled");

		Boolean field2 = finalResponse.getJSONArray("redemptions").getJSONObject(0).has("processed_at");
		Assert.assertEquals(field2, false, "processed_at field found");
		logger.info("Verified processed_at field not found in case of transferred reward redemption when "
				+ dataSet.get("dbFlag") + " flag is disabled");
		utils.logPass("Verified processed_at field not found in case of transferred reward redemption when "
						+ dataSet.get("dbFlag") + " flag is disabled");

		Boolean field3 = finalResponse.getJSONArray("redemptions").getJSONObject(0).has("redemption_status");
		Assert.assertEquals(field3, false, "redemption_status field found");
		logger.info("Verified redemption_status field not found in case of transferred reward redemption when "
				+ dataSet.get("dbFlag") + " flag is disabled");
		utils.logPass("Verified redemption_status field not found in case of transferred reward redemption when "
						+ dataSet.get("dbFlag") + " flag is disabled");

	}

	// Rakhi
	// transfer currency to other user and verify response when
	// "enable_v2_for_extensive_timeline_api" is false
	@Test(description = "SQ-T5772 Verify transferred redemption details in api2/dashboard/users/extensive_timeline API response for user who has transferred currency and enable_v2_for_extensive_timeline_api is false", groups = {
			"regression", "dailyrun" })
	@Owner(name = "Rakhi Rawat")
	public void T5772_VerifyTransferredCurrencyRedemptionWhenFlagDisabled() throws Exception {

		String b_id = dataSet.get("business_id");

		// disable "enable_v2_for_extensive_timeline_api" flag from the db
		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		pageObj.singletonDBUtilsObj();
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");

		pageObj.singletonDBUtilsObj();
		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, "false", dataSet.get("dbFlag"), b_id);
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to false");
		logger.info(dataSet.get("dbFlag") + " value is updated to false");
		utils.logit(dataSet.get("dbFlag") + " value is updated to false");

		// User register/signup using API2 Signup
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("Api2 user signup is successful");

		// send reward amount to user Amount
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"),
				dataSet.get("amount"), "", "", "");
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send message to user");
		utils.logPass("Api2  send reward amount to user is successful");

		// Sign-up other user
		String newUserEmail = pageObj.iframeSingUpPage().generateEmail();
		Response resp = pageObj.endpoints().Api2SignUp(newUserEmail, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(resp.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match for API v2 User Signup");

		// Gift Banked Currency to other user
		Response giftRewardResponse = pageObj.endpoints().Api2GiftAmountToUser(dataSet.get("client"),
				dataSet.get("secret"), newUserEmail, token);
		Assert.assertEquals(giftRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v2 Gift Banked Currency to other user");
		// api2/dashboard/users/extensive_timeline
		Response extendedUserHistoryResponse1 = pageObj.endpoints().extendedUserHistory(userID, dataSet.get("apiKey"));
		Assert.assertEquals(extendedUserHistoryResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Get Extended User History");

		String jsonObjectString = extendedUserHistoryResponse1.asString();
		JSONObject finalResponse = new JSONObject(jsonObjectString);

		Boolean field1 = finalResponse.getJSONArray("redemptions").getJSONObject(0).has("updated_at");
		Assert.assertEquals(field1, false, "updated_at field found");
		logger.info("Verified updated_at field not found in case of transferred currency redemption when "
				+ dataSet.get("dbFlag") + " flag is disabled");
		utils.logPass("Verified updated_at field not found in case of transferred currency redemption when "
						+ dataSet.get("dbFlag") + " flag is disabled");

		Boolean field2 = finalResponse.getJSONArray("redemptions").getJSONObject(0).has("processed_at");
		Assert.assertEquals(field2, false, "processed_at field found");
		logger.info("Verified processed_at field not found in case of transferred currency redemption when "
				+ dataSet.get("dbFlag") + " flag is disabled");
		utils.logPass("Verified processed_at field not found in case of transferred currency redemption when "
						+ dataSet.get("dbFlag") + " flag is disabled");

		Boolean field3 = finalResponse.getJSONArray("redemptions").getJSONObject(0).has("redemption_status");
		Assert.assertEquals(field3, false, "redemption_status field found");
		logger.info("Verified redemption_status field not found in case of transferred currency redemption when "
				+ dataSet.get("dbFlag") + " flag is disabled");
		utils.logPass("Verified redemption_status field not found in case of transferred currency redemption when "
						+ dataSet.get("dbFlag") + " flag is disabled");

	}

	// Rakhi
	// transfer points to other user and verify response when
	// "enable_v2_for_extensive_timeline_api" is false
	@Test(description = "SQ-T5773 Verify transferred redemption details in api2/dashboard/users/extensive_timeline API response for user who has transferred points and enable_v2_for_extensive_timeline_api is false", groups = {
			"regression", "dailyrun" })
	@Owner(name = "Rakhi Rawat")
	public void T5773_VerifyTransferredPointRedemptionWhenFlagDisabled() throws Exception {

		String b_id = dataSet.get("business_id");

		// disable "enable_v2_for_extensive_timeline_api" flag from the db
		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		pageObj.singletonDBUtilsObj();
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");

		pageObj.singletonDBUtilsObj();
		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, "false", dataSet.get("dbFlag"), b_id);
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to false");
		logger.info(dataSet.get("dbFlag") + " value is updated to false");
		utils.logit(dataSet.get("dbFlag") + " value is updated to false");

		// User register/signup using API2 Signup
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("Api2 user signup is successful");

		// User sign-up for user #2
		String userEmail2 = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse2 = pageObj.endpoints().Api1UserSignUp(userEmail2, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 user signup");

		// Send points to user #1
		Response sendMessageToUserResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				"", "", dataSet.get("points"));
		Assert.assertEquals(sendMessageToUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Send reward amount to user");
		utils.logPass("Send points to user #1 is successful");
		logger.info("Send points to user #1 is successful");

		// Loyalty points transfer to user #2
		Response transferPointsResponse = pageObj.endpoints().Api1TransferLoyaltyPointsToUser(userEmail2,
				dataSet.get("points"), dataSet.get("client"), dataSet.get("secret"), token);
		Assert.assertEquals(transferPointsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 transfer loyalty points to other user");
		utils.logPass("API v1 Loyalty points transfer to user #2 is successful");
		logger.info("API v1 Loyalty points transfer to user #2 is successful");

		// api2/dashboard/users/extensive_timeline
		Response extendedUserHistoryResponse1 = pageObj.endpoints().extendedUserHistory(userID, dataSet.get("apiKey"));
		Assert.assertEquals(extendedUserHistoryResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Get Extended User History");
		utils.logPass("PLATFORM FUNCTIONS API Get Extended User History is successful");
		String jsonObjectString = extendedUserHistoryResponse1.asString();
		JSONObject finalResponse = new JSONObject(jsonObjectString);

		Boolean field1 = finalResponse.getJSONArray("redemptions").getJSONObject(0).has("updated_at");
		Assert.assertEquals(field1, false, "updated_at field found");
		logger.info("Verified updated_at field not found in case of transferred points redemption when "
				+ dataSet.get("dbFlag") + " flag is disabled");
		utils.logPass("Verified updated_at field not found in case of transferred points redemption when "
						+ dataSet.get("dbFlag") + " flag is disabled");

		Boolean field2 = finalResponse.getJSONArray("redemptions").getJSONObject(0).has("processed_at");
		Assert.assertEquals(field2, false, "processed_at field found");
		logger.info("Verified processed_at field not found in case of transferred points redemption when "
				+ dataSet.get("dbFlag") + " flag is disabled");
		utils.logPass("Verified processed_at field not found in case of transferred points redemption when "
						+ dataSet.get("dbFlag") + " flag is disabled");

		Boolean field3 = finalResponse.getJSONArray("redemptions").getJSONObject(0).has("redemption_status");
		Assert.assertEquals(field3, false, "redemption_status field found");
		logger.info("Verified redemption_status field not found in case of transferred points redemption when "
				+ dataSet.get("dbFlag") + " flag is disabled");
		utils.logPass("Verified redemption_status field not found in case of transferred points redemption when "
						+ dataSet.get("dbFlag") + " flag is disabled");
	}

	// Rakhi
	// transfer loyalty reward to other user and verify response when
	// "enable_v2_for_extensive_timeline_api" is true
	@Test(description = "SQ-T5775 Verify transferred redemption details in api2/dashboard/users/extensive_timeline API response for user who has transferred reward [loyalty] and enable_v2_for_extensive_timeline_api is true")
	@Owner(name = "Rakhi Rawat")
	public void T5775_VerifyTransferredLoyaltyRewardRedemptionWhenFlagEnabled() throws Exception {

		String b_id = dataSet.get("business_id");

		// enable "enable_v2_for_extensive_timeline_api" flag from the db
		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		pageObj.singletonDBUtilsObj();
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");

		pageObj.singletonDBUtilsObj();
		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag"), b_id);
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to true");
		logger.info(dataSet.get("dbFlag") + " value is updated to true");
		utils.logit(dataSet.get("dbFlag") + " value is updated to true");

		// User register/signup using API2 Signup
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("Api2 user signup is successful");

		// User sign-up for user #2
		String userEmail2 = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse2 = pageObj.endpoints().Api1UserSignUp(userEmail2, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 user signup");

		// Pos api checkin of 120 points
		Response resp1 = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"), "120");
		Assert.assertEquals(resp1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for post chekin api");

		// fetch user offers/ reward_id using ap2 mobileOffers
		Response offerResponse = pageObj.endpoints().getUserOffers(token, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(offerResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 user offers");
		String reward_id = offerResponse.jsonPath().get("rewards[0].reward_id").toString();
		logger.info(reward_id);
		utils.logPass("Api2 user fetch user offers is successful");

		// Gift loyalty reward to other user
		Response giftRewardResponse = pageObj.endpoints().Api2GiftRewardToUser(dataSet.get("client"),
				dataSet.get("secret"), reward_id, userEmail2, token);
		Assert.assertEquals(giftRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for Gift loyalty reward to other user");
		logger.info("Loyalty reward transferred from " + userEmail + " to " + userEmail2);
		utils.logPass("Loyalty reward transferred from " + userEmail + " to " + userEmail2);

		// api2/dashboard/users/extensive_timeline
		Response extendedUserHistoryResponse1 = pageObj.endpoints().extendedUserHistory(userID, dataSet.get("apiKey"));
		Assert.assertEquals(extendedUserHistoryResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Get Extended User History");
		utils.logPass("PLATFORM FUNCTIONS API Get Extended User History is successful");
		String jsonObjectString = extendedUserHistoryResponse1.asString();
		JSONObject finalResponse = new JSONObject(jsonObjectString);

		Boolean field1 = finalResponse.getJSONArray("redemptions").getJSONObject(0).has("updated_at");
		Assert.assertEquals(field1, true, "updated_at field not found");
		logger.info("Verified updated_at field found in case of transferred loyalty reward redemption when "
				+ dataSet.get("dbFlag") + " flag is enabled");
		utils.logPass("Verified updated_at field found in case of transferred loyalty reward redemption when "
						+ dataSet.get("dbFlag") + " flag is enabled");

		Boolean field2 = finalResponse.getJSONArray("redemptions").getJSONObject(0).has("processed_at");
		Assert.assertEquals(field2, true, "processed_at field not found");
		logger.info("Verified processed_at field found in case of transferred loyalty reward redemption when "
				+ dataSet.get("dbFlag") + " flag is enabled");
		utils.logPass("Verified processed_at field found in case of transferred loyalty reward redemption when "
						+ dataSet.get("dbFlag") + " flag is enabled");

		Boolean field3 = finalResponse.getJSONArray("redemptions").getJSONObject(0).has("redemption_status");
		Assert.assertEquals(field3, true, "redemption_status field not found");
		logger.info("Verified redemption_status field found in case of transferred loyalty reward redemption when "
				+ dataSet.get("dbFlag") + " flag is enabled");
		utils.logPass("Verified redemption_status field found in case of transferred loyalty reward redemption when "
						+ dataSet.get("dbFlag") + " flag is enabled");

		String redemptionType = extendedUserHistoryResponse1.jsonPath().get("redemptions[0].type").toString();
		String status = extendedUserHistoryResponse1.jsonPath().get("redemptions[0].status");
		String internalTrackingCode = extendedUserHistoryResponse1.jsonPath()
				.get("redemptions[0].internal_tracking_code");
		String redemptionStatus = extendedUserHistoryResponse1.jsonPath().get("redemptions[0].redemption_status");
		String processedAt = extendedUserHistoryResponse1.jsonPath().get("redemptions[0].processed_at");

		Assert.assertEquals(redemptionType, "RewardRedemption", "Redemption Status does not matched");
		Assert.assertNull(status, "Value of status is not null");
		Assert.assertNull(internalTrackingCode, "Value of internal_tracking_code is not null");
		Assert.assertNull(redemptionStatus, "Value of redemption_status is not null");
		Assert.assertNull(processedAt, "Value of processed_at is not null");
		logger.info(
				"Verify transferred redemption details in api2/dashboard/users/extensive_timeline API response for user who has transferred reward and "
						+ dataSet.get("dbFlag") + " flag is enabled");
		utils.logPass(
				"Verify transferred redemption details in api2/dashboard/users/extensive_timeline API response for user who has transferred reward and "
						+ dataSet.get("dbFlag") + " flag is enabled");

	}

	// Rakhi
	// transfer loyalty reward to other user and verify response when
	// "enable_v2_for_extensive_timeline_api" is false
	@Test(description = "SQ-T5777 Verify transferred redemption details in api2/dashboard/users/extensive_timeline API response for user who has transferred reward [loyalty] and enable_v2_for_extensive_timeline_api is false")
	@Owner(name = "Rakhi Rawat")
	public void T5777_VerifyTransferredLoyaltyRewardRedemptionWhenFlagDisabled() throws Exception {

		String b_id = dataSet.get("business_id");

		// disable "enable_v2_for_extensive_timeline_api" flag from the db
		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		pageObj.singletonDBUtilsObj();
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");

		pageObj.singletonDBUtilsObj();
		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, "false", dataSet.get("dbFlag"), b_id);
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to false");
		logger.info(dataSet.get("dbFlag") + " value is updated to false");
		utils.logit(dataSet.get("dbFlag") + " value is updated to false");

		// User register/signup using API2 Signup
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("Api2 user signup is successful");

		// User sign-up for user #2
		String userEmail2 = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse2 = pageObj.endpoints().Api1UserSignUp(userEmail2, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 user signup");

		// Pos api checkin of 120 points
		Response resp1 = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"), "120");
		Assert.assertEquals(resp1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for post chekin api");

		// fetch user offers/ reward_id using ap2 mobileOffers
		Response offerResponse = pageObj.endpoints().getUserOffers(token, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(offerResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 user offers");
		String reward_id = offerResponse.jsonPath().get("rewards[0].reward_id").toString();
		logger.info(reward_id);
		utils.logPass("Api2 user fetch user offers is successful");

		// Gift loyalty reward to other user
		Response giftRewardResponse = pageObj.endpoints().Api2GiftRewardToUser(dataSet.get("client"),
				dataSet.get("secret"), reward_id, userEmail2, token);
		Assert.assertEquals(giftRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for Gift loyalty reward to other user");
		logger.info("Loyalty reward transferred from " + userEmail + " to " + userEmail2);
		utils.logPass("Loyalty reward transferred from " + userEmail + " to " + userEmail2);

		// api2/dashboard/users/extensive_timeline
		Response extendedUserHistoryResponse1 = pageObj.endpoints().extendedUserHistory(userID, dataSet.get("apiKey"));
		Assert.assertEquals(extendedUserHistoryResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Get Extended User History");
		utils.logPass("PLATFORM FUNCTIONS API Get Extended User History is successful");
		String jsonObjectString = extendedUserHistoryResponse1.asString();
		JSONObject finalResponse = new JSONObject(jsonObjectString);

		Boolean field1 = finalResponse.getJSONArray("redemptions").getJSONObject(0).has("updated_at");
		Assert.assertEquals(field1, false, "updated_at field found");
		logger.info("Verified updated_at field not found in case of transferred loyalty reward redemption when "
				+ dataSet.get("dbFlag") + " flag is disabled");
		utils.logPass("Verified updated_at field not found in case of transferred loyalty reward redemption when "
						+ dataSet.get("dbFlag") + " flag is disabled");

		Boolean field2 = finalResponse.getJSONArray("redemptions").getJSONObject(0).has("processed_at");
		Assert.assertEquals(field2, false, "processed_at field found");
		logger.info("Verified processed_at field not found in case of transferred loyalty reward redemption when "
				+ dataSet.get("dbFlag") + " flag is disabled");
		utils.logPass("Verified processed_at field not found in case of transferred loyalty reward redemption when "
						+ dataSet.get("dbFlag") + " flag is disabled");

		Boolean field3 = finalResponse.getJSONArray("redemptions").getJSONObject(0).has("redemption_status");
		Assert.assertEquals(field3, false, "redemption_status field found");
		logger.info("Verified redemption_status field not found in case of transferred loyalty reward redemption when "
				+ dataSet.get("dbFlag") + " flag is disabled");
		utils.logPass(
				"Verified redemption_status field not found in case of transferred loyalty reward redemption when "
						+ dataSet.get("dbFlag") + " flag is disabled");

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}

}
