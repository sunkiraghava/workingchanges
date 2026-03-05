package com.punchh.server.LP1Test;

import java.lang.reflect.Method;
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
public class GuestTimelineTest {
	private static Logger logger = LogManager.getLogger(GuestTimelineTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String sTCName;
	private String env, run = "ui";
	private String baseUrl;
	private String userEmail;
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
// Author : Amit

	@Test(description = "SQ-T2648 Validate Guest Phone number displayed on Timeline below email address and salesforce id || "
			+ "SQ-T2649,Validate that If the phone number is not available, nothing is getting displayed on guest timeline || "
			+ "SQ-T2650 Validate that if user enters phone numbers containing ‘(', ’)', '-', on saving the phone number all these type of characters gets removed and phone number with 10 digits gets saved.", groups = {
			"Regression" }, priority = 0)
	@Owner(name = "Amit Kumar")
	public void T2648_verifyGuestPhoneNumberDisplayedOnTimelineBelowEmailSalesforceId() throws InterruptedException {

		// user creation using pos signup api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response resp = pageObj.endpoints().posSignUp(userEmail, dataSet.get("locationKey"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp.getStatusCode(), "Status code 200 did not matched for pos signup api");

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.menupage().clickGuestValidation();
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_phone_uniqueness", "uncheck");
		pageObj.dashboardpage().updateCheckBox();

		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		// verify the phone number of the guest
		pageObj.guestTimelinePage().verifyPhoneNumber(userEmail);

		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		String phoneNumber = pageObj.guestTimelinePage().setPhone();
		Assert.assertEquals(phoneNumber, "1234567890", "Phone number 1234567890 did not match");
	}

	@Test(description = "SQ-T2616,T2613,T2614 Validate Distributable Scheduled Redeemable With Post Redemption Offer Campaign", groups = {
			"regression", "dailyrun" }, priority = 1)
	@Owner(name = "Amit Kumar")
	public void T2616_verifyDistributableScheduledRedeemableWithPostRedemptionOfferCampaign()
			throws InterruptedException {

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// create distributable redeemable
		pageObj.menupage().navigateToSubMenuItem("Settings", "Redeemables");
		String redeemableName = "Distributable Redeemable" + CreateDateTime.getTimeDateString();
		// minutes have been incremented by 1 in start and end time for Distributable
		// Redeemables.
		String startdateTime = CreateDateTime.getFutureDate(2) + " 11:00 PM";
		String enddateTime = CreateDateTime.getFutureDate(4) + " 11:00 PM";
		pageObj.redeemablePage().createDistributableRedeemable(redeemableName, dataSet.get("flatDiscount"),
				startdateTime, enddateTime);
		String status = pageObj.redeemablePage().getRedeemableStatus(redeemableName);
		Assert.assertEquals(status, "Scheduled", "Distributable Scheduled redeemable status did not matched");

		// Post_redemption_campaign_type_of_rewards_Redeemable
		String CampaignName = CreateDateTime.getUniqueString("Automation Postredemption Campaign");
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value
		pageObj.campaignspage().selectOfferdrpValue("Post Redemption Offer");
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().createWhatDetailsPostredemptionCampaign(CampaignName, dataSet.get("giftType"),
				dataSet.get("giftReason"), dataSet.get("redeemable"));
		pageObj.signupcampaignpage().setCampaignTrigger("Guest Redeems an Offer");
		// Set new scheduled redeemable
		pageObj.signupcampaignpage().setCampTriggerRedeemable(redeemableName);
		pageObj.signupcampaignpage().setPushNotification(dataSet.get("pushNotification"));
		pageObj.signupcampaignpage().setEmailSubject(dataSet.get("emailSubject"));
		pageObj.signupcampaignpage().setEmailTemplate(dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().clickNextBtn();
		pageObj.signupcampaignpage().setStartDate();
		pageObj.signupcampaignpage().createWhenDetailsCampaign();

		// delete campaign and scheduled redeemable
		pageObj.campaignspage().removeSearchedCampaign(CampaignName);
		pageObj.menupage().navigateToSubMenuItem("Settings", "Redeemables");
		pageObj.redeemablePage().searchRedeemable(redeemableName);
		pageObj.redeemablePage().deleteRedeemable(redeemableName);
	}

//LP2
	/*
	 * @Test(description =
	 * "SQ-T3579 Verify user can be deactivated and anyomised by superadmin", groups
	 * = { "Regression" }, priority = 2) public void
	 * T3579_verifyUserCanBeDeactivatedAndAnyomisedBySuperadmin() throws
	 * InterruptedException {
	 * 
	 * // user creation using pos signup api userEmail =
	 * pageObj.iframeSingUpPage().generateEmail(); Response resp =
	 * pageObj.endpoints().posSignUp(userEmail, dataSet.get("locationKey"));
	 * Assert.assertEquals(200, resp.getStatusCode(),
	 * "Status code 200 did not matched for pos signup api");
	 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
	 * pageObj.instanceDashboardPage().loginToInstance();
	 * pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
	 * pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
	 * pageObj.guestTimelinePage().performGuestFunctions("Deactivate", ""); String
	 * val = pageObj.guestTimelinePage().deactivationStatus();
	 * Assert.assertEquals(val, "Deactivated",
	 * "Deactivated lable text didnt matched on timeline");
	 * pageObj.guestTimelinePage().performGuestFunctions("Delete/Anonymize Guest?",
	 * "Delete-General"); String delstatus =
	 * pageObj.guestTimelinePage().delationStatus();
	 * Assert.assertTrue(delstatus.contains("This guest profile will be deleted on"
	 * ), "guest deletion label did not displayed on timeline");
	 * 
	 * } //LP2
	 * 
	 * @Test(description =
	 * "SQ-T3556 Verify admins not having permission to create segments and deactivate guest cannot perform these functions_partOne"
	 * , groups = { "Regression" }, priority = 3) public void
	 * T3556_verifyAdminsHavingNoPermissionToCreateSegmentsAndDeactivateGuestCannotPerformTheseFunctions_partOne
	 * () throws InterruptedException {
	 * 
	 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
	 * pageObj.instanceDashboardPage().loginToInstance();
	 * pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
	 * 
	 * pageObj.menupage().clickSettingsMenu(); pageObj.menupage().adminUsersLink();
	 * 
	 * // Set business manager permissions off
	 * pageObj.AdminUsersPage().selectBussinessManager(dataSet.get("userEmail"));
	 * pageObj.AdminUsersPage().turnPermissionoff("segment:manage");
	 * pageObj.AdminUsersPage().turnPermissionoff("visitor:deactivate"); } //LP2
	 * 
	 * @Test(description =
	 * "SQ-T3556 Verify admins not having permission to create segments and deactivate guest cannot perform these functions_partTwo"
	 * , groups = { "Regression" }, priority = 4) public void
	 * T3556_verifyAdminsHavingNoPermissionToCreateSegmentsAndDeactivateGuestCannotPerformTheseFunctions_partTwo
	 * () throws InterruptedException {
	 * 
	 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
	 * pageObj.instanceDashboardPage().logintoInstance(dataSet.get("businessManager"
	 * ), dataSet.get("pwd")); pageObj.menupage().navigateToSubMenuItem("Guests",
	 * "Segments"); boolean permStatus =
	 * pageObj.segmentsBetaPage().checkBetaSegmentpermissions();
	 * Assert.assertFalse(permStatus,
	 * "Beta Segment permission to business manager is not false");
	 * pageObj.segmentsBetaPage().findAndSelectSegment("custom automation");
	 * pageObj.segmentsBetaPage().getGuestInSegmentCount(); String segmentGuest =
	 * pageObj.segmentsBetaPage().getSegmentGuset();
	 * pageObj.instanceDashboardPage().navigateToGuestTimeline(segmentGuest);
	 * boolean status = pageObj.guestTimelinePage().checkDeativatepermissions();
	 * Assert.assertFalse(status,
	 * "Guest functions>> Deactivate permission to business manager is not false");
	 * }
	 */

	// Rakhi
	@Test(description = "SQ-T4719 Verify that during account re-evaluation, Membership Tier reset does occur for loyalty guests(eClub guest >> Loyalty guest)"
			+ "SQ-T4718 Verify that during account re-evaluation, Membership Tier reset does not occur for eClub guests with no loyalty account", groups = {
					"regression", "dailyrun" })
	@Owner(name = "Rakhi Rawat")
	public void T4719_MembershipTierResetOccurForLoyaltyGuests() throws InterruptedException {
		logger.info("== ECRM Signup test ==");
		pageObj.iframeSingUpPage().navigateToEcrm(baseUrl + dataSet.get("ecrmUrl"));
		userEmail = pageObj.iframeSingUpPage().ecrmSignUp(dataSet.get("location"));
		// Verify eclub user on timeline
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		Assert.assertTrue(pageObj.guestTimelinePage().verifyEclubGuestOnGuestTimeline(userEmail),
				"Error in verifying guest time line ");

		// ecrm pos checkin
		String key = CreateDateTime.getTimeDateString();
		String txn_no = "123456" + CreateDateTime.getTimeDateString();
		Response response = pageObj.endpoints().ecrmPosCheckin(userEmail, dataSet.get("locationKey"), key, txn_no);
		pageObj.apiUtils().verifyResponse(response, "ECRM checkin");
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "ECRM checkin failure");
		pageObj.guestTimelinePage().refreshTimeline();
		Assert.assertTrue(pageObj.guestTimelinePage().verifyEcrmTransaction(key, txn_no),
				"Failed to verify ECRM checkin on guest timeline");

		boolean membershipFlag = pageObj.guestTimelinePage().flagPresentorNot("Mem_Level Bronze Guest");
		Assert.assertFalse(membershipFlag, "Membership flag should not displayed but it is visible");
		logger.info("Membership Tier is not assigned to this eClub guests.");
		pageObj.utils().logPass("Membership Tier is not assigned to this eClub guests.");

		boolean loyalityFlag = pageObj.guestTimelinePage().flagPresentorNot("Loyalty");
		Assert.assertFalse(loyalityFlag, "Loyalty flag should not displayed but it is visible");
		logger.info("Loyalty flag is not visible for this guest");
		pageObj.utils().logPass("Loyalty flag is not visible for this guest");

		// iFrame Signup
		pageObj.iframeSingUpPage().navigateToIframe(baseUrl + dataSet.get("whiteLabel") + dataSet.get("slug"));
		pageObj.iframeSingUpPage().iframeSignUp(userEmail);

		// Verify guest on timeline
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		// pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		String joiningchannel = pageObj.guestTimelinePage().getGuestJoiningChannelEclub();
		String guestTimelineEmail = pageObj.guestTimelinePage().getGuestTimelineEmail();
		Assert.assertEquals(dataSet.get("joinedChannel"), joiningchannel,
				"Joined channel guest details did not matched");
		Assert.assertEquals(userEmail, guestTimelineEmail, "Guest email did not matched on timeline header");

		// ecrm pos checkin
		String key2 = CreateDateTime.getTimeDateString();
		String txn_no2 = "123456" + CreateDateTime.getTimeDateString();
		Response response2 = pageObj.endpoints().ecrmPosCheckin(userEmail, dataSet.get("locationKey"), key2, txn_no2);
		pageObj.apiUtils().verifyResponse(response2, "ECRM checkin");
		Assert.assertEquals(response2.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "ECRM checkin failure");
		pageObj.guestTimelinePage().refreshTimeline();
		Assert.assertTrue(pageObj.guestTimelinePage().verifyEcrmTransaction(key2, txn_no2),
				"Failed to verify ECRM checkin on guest timeline");

		boolean membershipFlag1 = pageObj.guestTimelinePage().flagPresentorNot("Mem_Level Bronze Guest");
		Assert.assertTrue(membershipFlag1, "Membership flag should displayed but it is not visible");
		logger.info("Membership Tier is assigned to this eClub guests.");
		pageObj.utils().logPass("Membership Tier is assigned to this eClub guests.");

		boolean loyalityFlag1 = pageObj.guestTimelinePage().flagPresentorNot("Loyalty");
		Assert.assertTrue(loyalityFlag1, "Loyalty flag should displayed but it is not visible");
		logger.info("Loyalty flag is visible for this guest");
		pageObj.utils().logPass("Loyalty flag is visible for this guest");

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}
}
