package com.punchh.server.loyalty2;

import java.lang.reflect.Method;
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
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class Location_TestCases {
	static Logger logger = LogManager.getLogger(Location_TestCases.class);

	public WebDriver driver;
	private Properties prop;
	private PageObj pageObj;
	private String sTCName;
	private String env, run = "ui";
	private String baseUrl;
	private static Map<String, String> dataSet;
	private String userEmail;
	private Utilities utils;

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
		prop = Utilities.loadPropertiesFile("config.properties");
		driver = new BrowserUtilities().launchBrowser();
		utils = new Utilities(driver);
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

	@Test(description = "SQ-T4086	Verify a location can not be deleted if an location groups has only one location attached to it"
			+ "SQ-T4087	Verify superadmin can create location group")
	@Owner(name = "Shashank Sharma")
	public void T4086_LocationCantNotBeDeletedIfSingleLocationIsAddedIntoLocationGroup() throws InterruptedException {

		String storeNumber = utils.getCurrentDate("ddmmyyhhmmss");
		// Instance login and goto dashboard
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Settings", "Locations");

		// Add New Location
		String locationName = pageObj.locationPage().newLocation(storeNumber);
		String locationGroupName = "AutoamtionLocationGroup_" + storeNumber;
		pageObj.menupage().navigateToSubMenuItem("Settings", "Location Groups");
		pageObj.locationPage().createLocationGroup(locationName, locationGroupName);
		pageObj.menupage().navigateToSubMenuItem("Settings", "Locations");
		pageObj.locationPage().searchAndClickOnLocation(locationName);
		pageObj.locationPage().deletenewLocation();

		String actualErrorMessage = pageObj.locationPage().getErrorSuccessMessage();
		String expErrorMessage = "This location cannot be deleted as it is the only location associated with one of the location groups";
		Assert.assertEquals(actualErrorMessage, expErrorMessage);

		TestListeners.extentTest.get()
				.pass("User is not able to delete location if single location is added into locationGroup");

		pageObj.menupage().navigateToSubMenuItem("Settings", "Location Groups");
		pageObj.locationPage().deleteLocationGroup(locationGroupName);
		String actualSuccessMessage = pageObj.locationPage().getErrorSuccessMessage();
		String expSuccessMessage = "Location Group deleted";
		Assert.assertEquals(actualSuccessMessage, expSuccessMessage,
				"Expected location group deletion Success Message not matched");

		TestListeners.extentTest.get().pass(locationGroupName + " location group is deleted successfully");

		pageObj.menupage().navigateToSubMenuItem("Settings", "Locations");
		pageObj.locationPage().searchAndClickOnLocation(locationName);
		pageObj.locationPage().deletenewLocation();

		String locationDeletionSuccessMessage = pageObj.locationPage().getErrorSuccessMessage();
		String expLocationSuccessMessage = "Location Deleted";
		Assert.assertEquals(locationDeletionSuccessMessage, expLocationSuccessMessage,
				"Expected location deletion Success Message not matched");

		TestListeners.extentTest.get().pass(locationName + " location is deleted successfully");
	}

	// shashank
	@Test(description = "SQ-T3555 Verify users are getting reward on the basis on location groups", groups = "regression", priority = 2)
	@Owner(name = "Shashank Sharma")
	public void T3555_verifyMassOfferCampaignWithLocationGroup() throws InterruptedException {
		// Precondition: segement and user is present Segement:custom automation

		String massCampaignName = "AutomationMassOffer_" + CreateDateTime.getTimeDateString();
		String dateTime = CreateDateTime.getTomorrowDate() + " 11:00 PM";
		String pushNotification = dataSet.get("pushNotification") + " " + massCampaignName;
		userEmail = dataSet.get("email");
		// Pos api checkin
		/*
		 * String key = CreateDateTime.getTimeDateString(); String txn = "123456" +
		 * CreateDateTime.getTimeDateString(); String date =
		 * CreateDateTime.getCurrentDate() + "T10:50:00+05:30"; Response respo =
		 * pageObj.endpoints().posCheckin(date, userEmail, key, txn,
		 * dataSet.get("locationKey")); Assert.assertEquals(200, respo.getStatusCode(),
		 * "Status code 200 did not matched for post chekin api");
		 */

		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();

		// set campaign name and gift type
		pageObj.signupcampaignpage().setCampaignName(massCampaignName);
		pageObj.signupcampaignpage().setGiftType(dataSet.get("giftType"));
		pageObj.signupcampaignpage().setRedeemable(dataSet.get("redeemable"));
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().setAudienceType(dataSet.get("audiance"));
		pageObj.signupcampaignpage().setLocationGroup(dataSet.get("location"));
		pageObj.signupcampaignpage().setPushNotification(pushNotification);
		pageObj.signupcampaignpage().setEmailSubject(dataSet.get("emailSubject"));
		pageObj.signupcampaignpage().setEmailTemplate(dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().createWhenDetailsMassCampaign("Once", dateTime);
		/*
		 * boolean status = pageObj.campaignspage().validateSuccessMessage();
		 * Assert.assertTrue(status,
		 * "Schedule created successfully Success message did not displayed....");
		 */
		// run mass offer
		// navigate to menu -> submenu
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().findMassCampaignNameandRun(massCampaignName);

		// Timeline validation (move to user timeline)
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		// wait for 5-6 Min
		// utils.longwait(6000*60*1);
		String campaignName = pageObj.guestTimelinePage().getcampaignNameWithWait(massCampaignName);
		boolean campaignNotificationStatus = pageObj.guestTimelinePage().verifyCampaignNotification();
		boolean pushNotificationStatus = pageObj.guestTimelinePage().verifyPushNotificationMassCampaign();

		Assert.assertTrue(campaignName.equalsIgnoreCase(massCampaignName),
				massCampaignName + " Campaign name did not matched");
		Assert.assertTrue(campaignNotificationStatus, massCampaignName + " Campaign notification did not displayed...");
		Assert.assertTrue(pushNotificationStatus, massCampaignName + " Push notification did not displayed...");
		TestListeners.extentTest.get().pass(
				"Mass offer points campaign detail: push notification, campaign name, pointsnotification validated successfully on timeline");

	}

	// Author : Amit
	// POS scoreboard validation can't be performed as report are moved to JS
	// enabled, need to verify this manually
	// so commenting below script and assigning it to shivam as discussed with him
	// @Test(description = "SQ-T3200, T3199 Check the POS scoreboard store number
	// should be there with respective id", groups = {"Regression" }, priority = 0)
	public void T3200_verifyPOSScoreBoardStorNumberShouldBeThereWithRespectiveId() throws InterruptedException {

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// navigate to cockpit -> earning
		pageObj.menupage().navigateToSubMenuItem("Support", "POS Stats");

		String storeNumber = pageObj.posStatsPage().checkStoreNumber();
		String storeId = pageObj.posStatsPage().checkStoreId();
		String status = pageObj.posStatsPage().checkEditStorePage();

		Assert.assertNotNull(storeNumber, "Store Number is null or not appeard on page");
		Assert.assertNotNull(storeId, "Store Id is null or not appeard on page");
		Assert.assertTrue(status.contains("Edit Location:"), "Edit/Update store location page did not displayed");
		TestListeners.extentTest.get().pass("Store number and id validated on POS Scoreboard");
	}

	@Test(description = "SQ-T5276 Attempt redemption at a disapproved location with a loyalty reward.")
	@Owner(name = "Shaleen Gupta")
	public void T5276_verifyRedemptionAtDisapprovedLocationLoyaltyReward() throws InterruptedException {

		// login to business
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		/// step-1
		// [flag OFF] Redemption possible at disapproved location
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.dashboardpage().navigateToTabs("Redemption Validations");
		pageObj.dashboardpage().checkUncheckFlagCockpitDashboard(dataSet.get("flagName"),
				dataSet.get("checkBoxFlagUnCheck"));

		// Signup using mobile api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		String userID = signUpResponse.jsonPath().get("user_id").toString();
		String token = signUpResponse.jsonPath().get("auth_token.token").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		// click message gift button and gift points to guest
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().messagePointsToUser(dataSet.get("subject"), dataSet.get("giftType"),
				dataSet.get("giftPoints"), dataSet.get("giftReason"));

		// create a check-in
		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key = CreateDateTime.getTimeDateString();
		Response checkinResp = pageObj.endpoints().posCheckin(date, userEmail, key, txn,
				dataSet.get("disapprovedLocationKey"));
		Assert.assertEquals(checkinResp.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for pos redemption api");

		// fetch user offers/ reward_id using ap2 mobileOffers
		Response offerResponse = pageObj.endpoints().getUserOffers(token, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(offerResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 user offers");
		String reward_id = offerResponse.jsonPath().get("rewards[0].reward_id").toString();
		utils.logPass("Api2 user fetch user offers is successful: " + reward_id);

		// Create Redemption using "reward_id" (fetch redemption code)
		Response redemptionResponse = pageObj.endpoints().Api2RedemptionWithRewardIdAndLocationId(dataSet.get("client"),
				dataSet.get("secret"), token, reward_id, dataSet.get("disapprovedLocationId"));
		Assert.assertEquals(redemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 create redemption using reward_id");
		String redemption_code = redemptionResponse.jsonPath().get("redemption_tracking_code").toString();
		Assert.assertNotNull(redemption_code, "Unable to generate redemption code");
		utils.logPass("Verified Redemption is possible at Disapproved Location when Flag is OFF");

		// step-2
		// [flag ON] Redemption is not possible at disapproved location
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.dashboardpage().navigateToTabs("Redemption Validations");
		pageObj.dashboardpage().checkUncheckFlagCockpitDashboard(dataSet.get("flagName"),
				dataSet.get("checkBoxFlagCheck"));

		// Signup using mobile api
		String userEmail2 = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse2 = pageObj.endpoints().Api1UserSignUp(userEmail2, dataSet.get("client"),
				dataSet.get("secret"));
		String userID2 = signUpResponse2.jsonPath().get("user_id").toString();
		String token2 = signUpResponse2.jsonPath().get("auth_token.token").toString();
		Assert.assertEquals(signUpResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		// click message gift button and gift points to guest
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail2);
		pageObj.guestTimelinePage().messagePointsToUser(dataSet.get("subject"), dataSet.get("giftType"),
				dataSet.get("giftPoints"), dataSet.get("giftReason"));

		// create a check-in
		String txn3 = "123456" + CreateDateTime.getTimeDateString();
		String date3 = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key3 = CreateDateTime.getTimeDateString();
		Response checkinResp2 = pageObj.endpoints().posCheckin(date3, userEmail2, key3, txn3,
				dataSet.get("disapprovedLocationKey"));
		Assert.assertEquals(checkinResp2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for pos redemption api");

		// fetch user offers/ reward_id using ap2 mobileOffers
		Response offerResponse2 = pageObj.endpoints().getUserOffers(token2, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(offerResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 user offers");
		String reward_id2 = offerResponse2.jsonPath().get("rewards[0].reward_id").toString();
		utils.logPass("Api2 user fetch user offers is successful: " + reward_id2);

		// Create Redemption using "reward_id" (fetch redemption code)
		Response redemptionResponse2 = pageObj.endpoints().Api2RedemptionWithRewardIdAndLocationId(
				dataSet.get("client"), dataSet.get("secret"), token2, reward_id2, dataSet.get("disapprovedLocationId"));
		Assert.assertEquals(redemptionResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for api2 create redemption using reward_id");
		String response_val2 = redemptionResponse2.jsonPath().get().toString();
		boolean verify = response_val2.contains(dataSet.get("expectedMsg"));
		Assert.assertTrue(verify, "Expected msg did not matched");
		utils.logPass("Verified Redemption is not possible at Disapproved Location when Flag is ON");

	}

	@Test(description = "SQ-T5278 Generate a redemption code at an approved location, then attempt redemption at the same location when it is disapproved.")
	@Owner(name = "Shaleen Gupta")
	public void T5278_verifyRedemptionAtDisapprovedLocationIFrame() throws InterruptedException {

		// login to business
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// [flag ON] Redemption is not possible at disapproved location
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.dashboardpage().navigateToTabs("Redemption Validations");
		pageObj.dashboardpage().checkUncheckFlagCockpitDashboard(dataSet.get("flagName"),
				dataSet.get("checkBoxFlagCheck"));

		// Signup using mobile api
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse3 = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		String userID = signUpResponse3.jsonPath().get("user_id").toString();
		String token = signUpResponse3.jsonPath().get("auth_token.token").toString();
		Assert.assertEquals(signUpResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		// click message gift button and gift redeemable to guest
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().giftRedeemableToUser(dataSet.get("reward"), dataSet.get("redeemable"));

		// set location status to approved
		pageObj.menupage().navigateToSubMenuItem("Settings", "Locations");
		pageObj.locationPage().setLocationStatus(dataSet.get("HQLocation"), "approved");

		// login user in IFrame
		pageObj.iframeSingUpPage().navigateToIframe(baseUrl + dataSet.get("whiteLabel") + dataSet.get("slug"));
		pageObj.iframeSingUpPage().iframeLogin(userEmail, Utilities.getApiConfigProperty("password"));

		// generate redemption code via IFrame
		String redemptionCode = pageObj.iframeSingUpPage().redeemRewardOffer(dataSet.get("rewardName"));

		// open business
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		// no need to enter credentials again
		// pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// set location status to disapproved
		pageObj.menupage().navigateToSubMenuItem("Settings", "Locations");
		pageObj.locationPage().setLocationStatus(dataSet.get("HQLocation"), "disapproved");

		// Pos redemption api
		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key = CreateDateTime.getTimeDateString();
		Response posRedemptionResp = pageObj.endpoints().posRedemptionOfCode(userEmail, date, redemptionCode, key, txn,
				dataSet.get("locationkey"));

		String response_val = posRedemptionResp.jsonPath().get("status").toString();
		logger.info(response_val);
		Assert.assertEquals(response_val, dataSet.get("expectedMsg"), "expected msg is not equal");
		utils.logPass("Verified Redemption is not possible at Disapproved Location");

		// set location status to approved
		pageObj.menupage().navigateToSubMenuItem("Settings", "Locations");
		pageObj.locationPage().setLocationStatus(dataSet.get("HQLocation"), "approved");

	}

	@Test(description = "SQ-T5277 Attempt redemption at a disapproved location with a non-loyalty reward. ")
	@Owner(name = "Shaleen Gupta")
	public void T5277_verifyRedemptionAtDisapprovedLocationNonLoyaltyReward() throws InterruptedException {

		// login to business
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// step-1
		// [flag OFF] Redemption possible at disapproved location
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.dashboardpage().navigateToTabs("Redemption Validations");
		pageObj.dashboardpage().checkUncheckFlagCockpitDashboard(dataSet.get("flagName"),
				dataSet.get("checkBoxFlagUnCheck"));

		// Signup using mobile api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		String userID = signUpResponse.jsonPath().get("user_id").toString();
		String token = signUpResponse.jsonPath().get("auth_token.token").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		// click message gift button and gift redeemable to guest
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().giftRedeemableToUser(dataSet.get("reward"), dataSet.get("redeemable"));

		// fetch user offers/ reward_id using ap2 mobileOffers
		Response offerResponse = pageObj.endpoints().getUserOffers(token, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(offerResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 user offers");
		String reward_id = offerResponse.jsonPath().get("rewards[0].reward_id").toString();
		utils.logPass("Api2 user fetch user offers is successful: " + reward_id);

		// Create Redemption using "reward_id" (fetch redemption code)
		Response redemptionResponse = pageObj.endpoints().Api2RedemptionWithRewardIdAndLocationId(dataSet.get("client"),
				dataSet.get("secret"), token, reward_id, dataSet.get("disapprovedLocationId"));
		Assert.assertEquals(redemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 create redemption using reward_id");
		String redemption_code = redemptionResponse.jsonPath().get("redemption_tracking_code").toString();
		Assert.assertNotNull(redemption_code, "Unable to generate redemption code");
		utils.logPass("Verified Redemption is possible at Disapproved Location when Flag is OFF");

		// step-2
		// [flag ON] Redemption is not possible at disapproved location
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.dashboardpage().navigateToTabs("Redemption Validations");
		pageObj.dashboardpage().checkUncheckFlagCockpitDashboard(dataSet.get("flagName"),
				dataSet.get("checkBoxFlagCheck"));

		// Signup using mobile api
		String userEmail2 = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse2 = pageObj.endpoints().Api1UserSignUp(userEmail2, dataSet.get("client"),
				dataSet.get("secret"));
		String userID2 = signUpResponse2.jsonPath().get("user_id").toString();
		String token2 = signUpResponse2.jsonPath().get("auth_token.token").toString();
		Assert.assertEquals(signUpResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		// click message gift button and gift redeemable to guest
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail2);
		pageObj.guestTimelinePage().giftRedeemableToUser(dataSet.get("reward"), dataSet.get("redeemable"));

		// fetch user offers/ reward_id using ap2 mobileOffers
		Response offerResponse2 = pageObj.endpoints().getUserOffers(token2, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(offerResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 user offers");
		String reward_id2 = offerResponse2.jsonPath().get("rewards[0].reward_id").toString();
		utils.logPass("Api2 user fetch user offers is successful: " + reward_id2);

		// Create Redemption using "reward_id" (fetch redemption code)
		Response redemptionResponse2 = pageObj.endpoints().Api2RedemptionWithRewardIdAndLocationId(
				dataSet.get("client"), dataSet.get("secret"), token2, reward_id2, dataSet.get("disapprovedLocationId"));
		Assert.assertEquals(redemptionResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for api2 create redemption using reward_id");
		String response_val2 = redemptionResponse2.jsonPath().get().toString();
		boolean verify = response_val2.contains(dataSet.get("expectedMsg"));
		Assert.assertTrue(verify, "Expected msg did not matched");
		utils.logPass("Verified Redemption is not possible at Disapproved Location when Flag is ON");

	}

	@Test(description = "SQ-T5285 Attempt redemption from v2 API \"api2/mobile/redemptions\" end point at a disapproved location. ")
	@Owner(name = "Shaleen Gupta")
	public void T5285_verifyRedemptionV2ApiAtDisapprovedLocation() throws InterruptedException {

		// login to business
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// [flag ON] Redemption is not possible at disapproved location
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.dashboardpage().navigateToTabs("Redemption Validations");
		pageObj.dashboardpage().checkUncheckFlagCockpitDashboard(dataSet.get("flagName"),
				dataSet.get("checkBoxFlagCheck"));

		// Signup using mobile api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		String userID = signUpResponse.jsonPath().get("user_id").toString();
		String token = signUpResponse.jsonPath().get("auth_token.token").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		// send points to user
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "", "", "",
				dataSet.get("giftPoints"));
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get().pass("Api2  send reward amount to user is successful");

		// fetch user offers/ reward_id using ap2 mobileOffers
		Response offerResponse = pageObj.endpoints().getUserOffers(token, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(offerResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 user offers");
		String reward_id = offerResponse.jsonPath().get("rewards[0].reward_id").toString();
		utils.logPass("Api2 user fetch user offers is successful: " + reward_id);

		// Create Redemption using "rewardId" (fetch redemption code)
		Response rewardIdRedemptionResp = pageObj.endpoints().Api2RedemptionWithRewardIdAndLocationId(
				dataSet.get("client"), dataSet.get("secret"), token, reward_id, dataSet.get("disapprovedLocationId"));
		String response_val1 = rewardIdRedemptionResp.jsonPath().get().toString();
		boolean verify1 = response_val1.contains(dataSet.get("expectedMsg"));
		Assert.assertTrue(verify1, "Expected msg did not matched");
		utils.logPass("Verified Redemption using rewardId is not possible at Disapproved Location when Flag is ON");

		// redeem redeemable
		// Create Redemption using "redeemable" (fetch redemption code)
		Response redeemableRedemptionResp = pageObj.endpoints().Api2RedemptionWitReedemableIdAndLocationId(
				dataSet.get("client"), dataSet.get("secret"), token, dataSet.get("redeemableId"),
				dataSet.get("disapprovedLocationId"));
		String response_val2 = redeemableRedemptionResp.jsonPath().get().toString();
		boolean verify2 = response_val2.contains(dataSet.get("expectedMsg"));
		Assert.assertTrue(verify2, "Expected msg did not matched");
		utils.logPass("Verified Redemption using redeemable is not possible at Disapproved Location when Flag is ON");

		// subscription purchase
		String PlanID = dataSet.get("PlanID");
		String spPrice = dataSet.get("spPrice");
		Response purchaseSubscriptionresponse = pageObj.endpoints().Api2SubscriptionPurchase(token, PlanID,
				dataSet.get("client"), dataSet.get("secret"), spPrice, "");
		Assert.assertEquals(purchaseSubscriptionresponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		boolean isApi2PurchaseSubscriptionSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.authPurchaseSubscriptionSchema, purchaseSubscriptionresponse.asString());
		Assert.assertTrue(isApi2PurchaseSubscriptionSchemaValidated,
				"API v2 Purchase Subscription Schema Validation failed");
		String subscription_id = purchaseSubscriptionresponse.jsonPath().get("subscription_id").toString();
		logger.info(userEmail + " purchased " + subscription_id + " Plan id = " + PlanID);

		// Create Redemption using subscription (fetch redemption code)
		Response card = pageObj.endpoints().Api2SubscriptionRedemptionWithLocationId(dataSet.get("client"),
				dataSet.get("secret"), subscription_id, token, dataSet.get("disapprovedLocationId"));
		boolean isApi2SubscriptionRedemptionSchemaValidated = Utilities
				.validateJsonArrayAgainstSchema(ApiResponseJsonSchema.apiStringArraySchema, card.asString());
		Assert.assertTrue(isApi2SubscriptionRedemptionSchemaValidated,
				"API v2 Create Redemption using subscription Schema Validation failed");
		String response_val3 = card.jsonPath().get().toString();
		boolean verify3 = response_val3.contains(dataSet.get("expectedMsg"));
		Assert.assertTrue(verify3, "Expected msg did not matched");
		utils.logPass("Verified Redemption using subscription is not possible at Disapproved Location when Flag is ON");
	}

	// Rakhi
	@Test(description = "SQ-T6290 Verify Archived location group should not be selectable once removed during editing"
			+ "SQ-T6291 Verify Able to Select Active Location Group During Creation")
	@Owner(name = "Rakhi Rawat")
	public void T6290_VerifyArchivedLocationShouldNotBeSelectable() throws Exception {

		String redeemableName = "AutomationRedeemable" + CreateDateTime.getTimeDateString();

		// enable enable_location_group_archive from db
		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ dataSet.get("business_id") + "';";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");

		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, "true",
				"enable_location_group_archive", dataSet.get("business_id"));
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to true");
		utils.logit(dataSet.get("dbFlag") + " value is updated to true");

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Add New Location
		String locationGroupName = "AutoamtionLocationGroup_" + CreateDateTime.getTimeDateString();
		pageObj.menupage().navigateToSubMenuItem("Settings", "Location Groups");
		pageObj.locationPage().createLocationGroup("Automation", locationGroupName);

		pageObj.menupage().navigateToSubMenuItem("Settings", "Redeemables");
		pageObj.redeemablePage().createRedeemable(redeemableName);
		pageObj.redeemablePage().selectRecieptRule("1");
		pageObj.redeemablePage().expiryInDays("", "3");
		pageObj.redeemablePage().addRedeemingLocation(locationGroupName);
		pageObj.redeemablePage().clickOnFinishButton();
		boolean flag2 = pageObj.redeemablePage().successOrErrorConfirmationMessage("Redeemable successfully saved.");
		Assert.assertTrue(flag2, " Failed to create Redeemable ");
		utils.logPass("Verified user is able to select Active Location Group during Redeemable creation");
		utils.longWaitInSeconds(2);
		// SQ-T6290 starting from here
		// Archive location group attched to redeemable
		pageObj.menupage().navigateToSubMenuItem("Settings", "Location Groups");
		pageObj.locationPage().archiveLocationGroup(locationGroupName);

		// verify if location group is archived
		pageObj.dashboardpage().navigateToTabs("Archived Location Groups");
		boolean flag1 = pageObj.locationPage().verifyLocationGroupArchivedOrNot("(Archived) " + locationGroupName);
		logger.info("(Archived) " + locationGroupName);
		Assert.assertTrue(flag1, "Location group is not archived successfully");
		utils.logPass("Verified location group is archived successfully");

		// open created redeemable
		pageObj.menupage().navigateToSubMenuItem("Settings", "Redeemables");
		pageObj.redeemablePage().searchAndClickOnRedeemable(redeemableName);
		pageObj.signupcampaignpage().clickNextBtn();
		pageObj.signupcampaignpage().clickNextBtn();
		boolean flag4 = pageObj.redeemablePage().verifyArchivedLocationInRedeemingLocationsDrpDwn(locationGroupName);
		Assert.assertTrue(flag4, "Archived location group is not present in redeeming locations dropdown");
		utils.logPass("Verified Archived location group is present in redeeming locations dropdown");

		// now remove the archived location group and select another active group
		pageObj.redeemablePage().addRedeemingLocation(dataSet.get("locationGroupName1"));
		pageObj.redeemablePage().clickOnFinishButton();
		utils.longWaitInSeconds(3);

		// Archived group is no longer visible in the dropdown and cannot be re-selected
		pageObj.redeemablePage().searchAndClickOnRedeemable(redeemableName);
		pageObj.signupcampaignpage().clickNextBtn();
		pageObj.signupcampaignpage().clickNextBtn();
		boolean flag3 = pageObj.redeemablePage().verifyArchivedLocationInRedeemingLocationsDrpDwn(locationGroupName);
		Assert.assertFalse(flag3, "Archived location group is present in redeeming locations dropdown");
		utils.logPass("Verified Archived location group is not present in redeeming locations dropdown");

		// delete redeemable
		pageObj.menupage().navigateToSubMenuItem("Settings", "Redeemables");
		pageObj.redeemablePage().searchRedeemable(redeemableName);
		pageObj.redeemablePage().deleteRedeemable(redeemableName);
	}

	// Rakhi
	@Test(description = "SQ-T6327 Verify all location groups should be visible when feature flag is disabled"
			+ "SQ-T6331 Verify Location groups dropdown should render without error")
	@Owner(name = "Rakhi Rawat")
	public void T6327_VerifyAllLocationGroupVisibleWhenFlagDisabled() throws Exception {

		String redeemableName = "AutomationRedeemable" + CreateDateTime.getTimeDateString();

		// disable enable_location_group_archive from db
		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ dataSet.get("business_id") + "';";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");

		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, "false",
				"enable_location_group_archive", dataSet.get("business_id"));
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to false");
		utils.logit(dataSet.get("dbFlag") + " value is updated to false");

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// get location group count
		pageObj.menupage().navigateToSubMenuItem("Settings", "Location Groups");
		int Count = pageObj.locationPage().getLocationGroupCount();
		// create redeemable
		pageObj.menupage().navigateToSubMenuItem("Settings", "Redeemables");
		pageObj.redeemablePage().createRedeemable(redeemableName);
		pageObj.redeemablePage().selectRecieptRule("1");
		pageObj.redeemablePage().expiryInDays("", "3");
		List<String> locationList = pageObj.redeemablePage().getRedeemingLocationList(); // verifying SQ-T6331 here
		Assert.assertTrue(locationList.size() == Count,
				"Redeeming locations list size is not equal to Location group count");
		utils.logPass("Verified all location groups are visible when feature flag is disabled");

		// delete redeemable
		pageObj.menupage().navigateToSubMenuItem("Settings", "Redeemables");
		pageObj.redeemablePage().searchRedeemable(redeemableName);
		pageObj.redeemablePage().deleteRedeemable(redeemableName);
	}

	// Rakhi
	@Test(description = "SQ-T6325 Verify Admin page when enable_location_group_archive is true"
			+ "SQ-T6326 Verify Admin page when enable_location_group_archive is false")
	@Owner(name = "Rakhi Rawat")
	public void T6325_VerifyAdminPageWithEnableLocationGroupArchiveFlag() throws Exception {

		// enable enable_location_group_archive from db
		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ dataSet.get("business_id") + "';";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");

		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, "true",
				"enable_location_group_archive", dataSet.get("business_id"));
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to true");
		utils.logit(dataSet.get("dbFlag") + " value is updated to true");

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// get location group count
		pageObj.menupage().navigateToSubMenuItem("Settings", "Location Groups");
		int Count1 = pageObj.locationPage().getLocationGroupCount();

		// navigate to Admin Users page
		pageObj.menupage().navigateToSubMenuItem("Settings", "Admin Users");
		// pageObj.AdminUsersPage().searchUser(dataSet.get("userSubmitter"));
		pageObj.AdminUsersPage().clickRole("vansham.mishragetqclist+0302@partech.com", "Vansham Mishra");
		List<String> locationList = pageObj.AdminUsersPage().getAccessibleLocationGroupList();
		Assert.assertTrue(locationList.size() == Count1,
				"Accessible location group list size is not equal to active location group count");
		utils.logPass("Verified only active location groups are visible for admin when feature flag is enabled");

		// disable enable_location_group_archive from db
		logger.info(
				"---------Initiating SQ-T6326 Verify Admin page when enable_location_group_archive is false-----------");
		String query1 = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ dataSet.get("business_id") + "';";
		String expColValue1 = DBUtils.executeQueryAndGetColumnValue(env, query1, "preferences");

		boolean flag1 = DBUtils.updateBusinessesPreference(env, expColValue1, "false",
				"enable_location_group_archive", dataSet.get("business_id"));
		Assert.assertTrue(flag1, dataSet.get("dbFlag") + " value is not updated to false");
		utils.logit(dataSet.get("dbFlag") + " value is updated to false");

		// get location group count
		pageObj.menupage().navigateToSubMenuItem("Settings", "Location Groups");
		int Count2 = pageObj.locationPage().getLocationGroupCount();

		// navigate to Admin Users page
		pageObj.menupage().navigateToSubMenuItem("Settings", "Admin Users");

		pageObj.AdminUsersPage().clickRole("vansham.mishragetqclist+0302@partech.com", "Vansham Mishra");
		List<String> locationList1 = pageObj.AdminUsersPage().getAccessibleLocationGroupList();
		Assert.assertTrue(locationList1.size() == Count2,
				"Accessible location group list size is not equal to all location group count");
		utils.logPass(
				"Verified all location groups active and archived are visible for admin when feature flag is disabled");

	}

	// Rakhi
	@Test(description = "SQ-T6334 Verify Active LG's are available on eClub CSV Upload page [new page] when enable_location_group_archive is true"
			+ "SQ-T6335 Verify Active LG's are available on eClub CSV Upload page [new page] when enable_location_group_archive is false")
	@Owner(name = "Rakhi Rawat")
	public void T6334_VerifyEclubCsvUploadPageWithEnableLocationGroupArchiveFlag() throws Exception {

		// enable enable_location_group_archive from db
		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ dataSet.get("business_id") + "';";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");

		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, "true",
				"enable_location_group_archive", dataSet.get("business_id"));
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to true");
		utils.logit(dataSet.get("dbFlag") + " value is updated to true");

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// get location group count
		pageObj.menupage().navigateToSubMenuItem("Settings", "Location Groups");
		int Count1 = pageObj.locationPage().getLocationGroupCount();

		// navigate to eclub guests page
		pageObj.menupage().navigateToSubMenuItem("Guests", "eClub Guests");
		pageObj.EClubGuestPage().clickOnImportLink();
		List<String> locationList = pageObj.EClubGuestPage().getEffectiveLocationGroupList();
		Assert.assertTrue(locationList.size() == Count1,
				"Effective location group list size is not equal to active location group count");
		utils.logPass(
				"Verified only active location groups are visible on eClub CSV Upload page when feature flag is enabled");

		// disable enable_location_group_archive from db
		logger.info(
				"---------Initiating SQ-T6335 Verify Active LG's are available on eClub CSV Upload page [new page] when enable_location_group_archive is false-----------");
		String query1 = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ dataSet.get("business_id") + "';";
		String expColValue1 = DBUtils.executeQueryAndGetColumnValue(env, query1, "preferences");

		boolean flag1 = DBUtils.updateBusinessesPreference(env, expColValue1, "false",
				"enable_location_group_archive", dataSet.get("business_id"));
		Assert.assertTrue(flag1, dataSet.get("dbFlag") + " value is not updated to false");
		utils.logit(dataSet.get("dbFlag") + " value is updated to false");

		// get location group count
		pageObj.menupage().navigateToSubMenuItem("Settings", "Location Groups");
		int Count2 = pageObj.locationPage().getLocationGroupCount();

		// navigate to eclub guests page
		pageObj.menupage().navigateToSubMenuItem("Guests", "eClub Guests");
		pageObj.EClubGuestPage().clickOnImportLink();
		List<String> locationList1 = pageObj.EClubGuestPage().getEffectiveLocationGroupList();
		Assert.assertTrue(locationList1.size() == Count2,
				"Effective location group list size is not equal to all location group count");
		utils.logPass(
				"Verified all location groups active and archived are visible on eClub CSV Upload page when feature flag is disabled");
	}

	// Rakhi
	@Test(description = "SQ-T6343 Verify Active LG's are available on eClub CSV Upload page [edit page] if already uploaded on active LG and enable_location_group_archive is true"
			+ "SQ-T6342 Verify Active LG's are available on eClub CSV Upload page [edit page] if already uploaded on active LG and enable_location_group_archive is false")
	@Owner(name = "Rakhi Rawat")
	public void T6343_VerifyEclubCsvUploadEditPageWithEnableLocationGroupArchiveFlag() throws Exception {

		// enable enable_location_group_archive from db
		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ dataSet.get("business_id") + "';";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");

		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, "true",
				"enable_location_group_archive", dataSet.get("business_id"));
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to true");
		utils.logit(dataSet.get("dbFlag") + " value is updated to true");

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// navigate to eclub guests page
		pageObj.menupage().navigateToSubMenuItem("Guests", "eClub Guests");
		pageObj.EClubGuestPage().clickOnCSVuploadsOnEclubGuestsPage(dataSet.get("csvName"));
		pageObj.EClubGuestPage().clickOnAddListBtn();

		// verify effective location dropdown is disabled or not
		boolean flag1 = pageObj.EClubGuestPage().verifyEffectiveLocationDrpDwn();
		Assert.assertFalse(flag1, "Effective Location dropdown is not disabled on eClub CSV Upload page");
		utils.logPass(
				"Verified Effective Location dropdown is disabled on eClub CSV Upload page when feature flag is enabled");

		// verify effective location dropdown value
		String text = pageObj.EClubGuestPage().verifyEffectiveLocationDrpDwnValue();
		Assert.assertEquals(text, dataSet.get("effectiveLocation"),
				"Effective Location dropdown value is not equal to 'Effective Location'");
		utils.logPass(
				"Verified Effective Location dropdown value is equal to 'Effective Location' on eClub CSV Upload page ie : "
						+ text);

		// disable enable_location_group_archive from db
		logger.info(
				"---------Initiating SQ-T6342 Verify Active LG's are available on eClub CSV Upload page [edit page] if already uploaded on active LG and enable_location_group_archive is false-----------");
		String query1 = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ dataSet.get("business_id") + "';";
		String expColValue1 = DBUtils.executeQueryAndGetColumnValue(env, query1, "preferences");

		boolean flag3 = DBUtils.updateBusinessesPreference(env, expColValue1, "false",
				"enable_location_group_archive", dataSet.get("business_id"));
		Assert.assertTrue(flag3, dataSet.get("dbFlag") + " value is not updated to false");
		utils.logit(dataSet.get("dbFlag") + " value is updated to false");

		// navigate to eclub guests page
		pageObj.menupage().navigateToSubMenuItem("Guests", "eClub Guests");
		pageObj.EClubGuestPage().clickOnCSVuploadsOnEclubGuestsPage(dataSet.get("csvName"));
		pageObj.EClubGuestPage().clickOnAddListBtn();

		// verify effective location dropdown is disabled or not
		boolean flag4 = pageObj.EClubGuestPage().verifyEffectiveLocationDrpDwn();
		Assert.assertFalse(flag4, "Effective Location dropdown is not disabled on eClub CSV Upload page");
		utils.logPass(
				"Verified Effective Location dropdown is disabled on eClub CSV Upload page when feature flag is disabled");

		// verify effective location dropdown value
		String text1 = pageObj.EClubGuestPage().verifyEffectiveLocationDrpDwnValue();
		Assert.assertEquals(text1, dataSet.get("effectiveLocation"),
				"Effective Location dropdown value is not equal to 'Effective Location'");
		utils.logPass(
				"Verified Effective Location dropdown value is equal to 'Effective Location' on eClub CSV Upload page ie : "
						+ text1);
	}

	@Test(description = "SQ-T5806 Verify that when a new location is added to a Business with both the 'Multiple Redemption' flag and the 'Enable Multiple Redemptions on All Locations' flag enabled, the location is automatically enabled for multiple redemptions", groups = "regression", priority = 1)
	@Owner(name = "Vansham Mishra")
	public void T5806_verifyLocationAutoEnabledForMultipleRedemptionsWhenFlagsSet() throws Exception {

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// navigate to eclub guests page
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().checkUncheckFlagCockpitDashboard(dataSet.get("Enable Multiple Redemptions"),
				dataSet.get("flagValue"));
		pageObj.dashboardpage().checkUncheckFlagCockpitDashboard(
				dataSet.get("Enable Multiple Redemptions on All Locations"), dataSet.get("flagValue"));
		pageObj.dashboardpage().clickOnUpdateButton();
		pageObj.menupage().navigateToSubMenuItem("Settings", "Locations");
		String storeNumber = String.valueOf(Utilities.getRandomNoFromRange(500, 2000));
		String locationName = pageObj.locationPage().createNewLocation(storeNumber, "Alabama");
		utils.logit("New location created with name : " + locationName);
		pageObj.locationPage().selectLocationSearch2(locationName);
		boolean flag = pageObj.locationPage().flagValue("Allow Location for Multiple Redemption");
		Assert.assertTrue(flag, "Allow Location for Multiple Redemption flag is not enabled by default");
		utils.logPass(
				"Verified that new location is enabled for multiple redemptions by default when both the flags are enabled");
		pageObj.locationPage().deletenewLocation();
	}

	@Test(description = "SQ-T4332 Add new location in the business"
			+ "SQ-T6938 Verify error message appears when admin enters more than 255 characters in Location name field on Location creation"
			+ "SQ-T6939 Verify max 255 characters can be saved in Location name field on Location creation", groups = {
					"regression", "dailyrun" })
	@Owner(name = "Rakhi Rawat")
	public void T4332_addNewLocation() throws InterruptedException {

		String invalidLocationName = "Invalid_Location_Name" + CreateDateTime.getRandomString(256 - "Invalid_Location_Name".length()).toLowerCase();
		String validLocationName = "Automation_Location" + CreateDateTime.getRandomString(255 - "Automation_Location".length()).toLowerCase();
		String storeNo = CreateDateTime.getRandomNumberSixDigit();
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Settings", "Locations");

		// create location with more than 255 characters in location name field
		pageObj.locationPage().newLocationWithBusiness(invalidLocationName, storeNo, dataSet.get("businessHours"));
		String error = pageObj.locationPage().getErrorSuccessMessage();
		Assert.assertTrue(error.contains("Name is too long (maximum is 255 characters)"),
				"Error message is not displayed for invalid location name");
		utils.logPass(
				"Verified error message is displayed when more than 255 characters entered in Location name field");

		pageObj.menupage().navigateToSubMenuItem("Settings", "Locations");

		// create location with max 255 characters in location name field
		pageObj.locationPage().newLocationWithBusiness(validLocationName, storeNo, dataSet.get("businessHours"));
		String msg = pageObj.locationPage().getErrorSuccessMessage();
		Assert.assertTrue(msg.contains("Location was successfully created"), "Success message is not displayed");
		utils.logPass("Verified max 255 characters can be saved in Location name field on Location creation");

		pageObj.locationPage().searchAndDeleteLocation(validLocationName);
		String msg1 = pageObj.mobileconfigurationPage().getSuccessMessage();
		Assert.assertTrue(msg1.contains("Location Deleted"), "Location is not delete");
		utils.logPass("successfully deleted new location " + validLocationName);

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}

}
