package com.punchh.server.segmentsTest;

import java.io.IOException;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
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
public class CampaignWithTransactionalAndNonTransactionalSegmentTest {

	public WebDriver driver;
	private PageObj pageObj;
	private String userEmail, redeemable;
	private String sTCName;
	private String env;
	private String baseUrl;
	private static Map<String, String> dataSet;
	String run = "ui";
	String campaignName, campaignName2;
	private Utilities utils;

	@BeforeClass(alwaysRun = true)
	public void openBrowser() {
		driver = new BrowserUtilities().launchBrowser();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		// single Login to instance
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		pageObj.instanceDashboardPage().instanceLogin(baseUrl);
	}

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {

		sTCName = method.getName();
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		pageObj.utils().logit(sTCName + " ==>" + dataSet);
		utils = new Utilities(driver);
		// Navigate to All Business Page
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);

	}

	@Test(description = "SQ-T3018 Verify Signup campaign working properly with Transactional, In Memory check and Non Transactional Segments|| "
			+ "SQ-T3015 Verify User is able to browse Segments on the campaign edit page || "
			+ "SQ-T2185 Validate Segment Beta in guest timeline", groups = { "regression", "dailyrun" }, priority = 0)
	@Owner(name = "Hardik Bhardwaj")
	public void T3018_SignupCampaignWithTransactionalSegment() throws InterruptedException {
		redeemable = dataSet.get("redeemable1");
		campaignName = CreateDateTime.getUniqueString("Automation SignUpCampaign" + dataSet.get("CamSeg1"));
		// Login to instance
//		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
//		pageObj.instanceDashboardPage().loginToInstance();

		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// check redeemable's availability in business and create redeemable if not
		// available Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select ongoing dropdown value
		pageObj.campaignspage().selectOngoingdrpValue(dataSet.get("onGoingDrp"));
		pageObj.campaignspage().clickNewCampaignBtn();
		// create signup campaign with PN Email configured
		// pageObj.signupcampaignpage().setAudianceType(dataSet.get("audiance"));
		pageObj.signupcampaignpage().createWhatDetailsSignupCampaign(campaignName, dataSet.get("giftType"),
				dataSet.get("giftReason1"), dataSet.get("redeemable1"));
		boolean flag = pageObj.signupcampaignpage().verifySegmentListVisibility();
		Assert.assertTrue(flag, "In Segment dropdown Segment list are not showing");
		utils.logPass("In Segment dropdown Segment list are showing");

		pageObj.signupcampaignpage().setSegmentType(dataSet.get("segment1"));

		pageObj.signupcampaignpage().setPushNotification(dataSet.get("pushNotification1"));
		pageObj.signupcampaignpage().setEmailSubject(dataSet.get("emailSubject1"));
		pageObj.signupcampaignpage().setEmailTemplate(dataSet.get("emailTemplate1"));
		pageObj.signupcampaignpage().clickNextButton();
		pageObj.signupcampaignpage().createWhenDetailsCampaign();

		// user signup using pos signup api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response resp = pageObj.endpoints().posSignUp(userEmail, dataSet.get("locationkey"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp.getStatusCode(), "Status code 200 did not matched for pos signup api");

		// Timeline validation
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		String signUpCampaignName = pageObj.guestTimelinePage().getcampaignNameWithWait(campaignName);
		Assert.assertTrue(signUpCampaignName.equalsIgnoreCase(campaignName), "Campaign name did not matched");
		utils.logPass("Signup campaign with name " + campaignName + " is found on user Timeline");

		boolean campaignNotificationStatus = pageObj.guestTimelinePage().verifyCampaignNotification();
		Assert.assertTrue(campaignNotificationStatus, "Campaign notification did not displayed...");
		utils.logPass("Signup campaign with name " + campaignName
				+ " is found on user Timeline and Campaign Notification Status is :- " + campaignNotificationStatus);

		boolean pushNotificationStatus = pageObj.guestTimelinePage().verifyPushNotification();
		Assert.assertTrue(pushNotificationStatus, "Push notification did not displayed...");
		utils.logPass("Signup campaign with name " + campaignName
				+ " is found on user Timeline and Push Notification Status is :- " + pushNotificationStatus);

		String rewardedRedeemableStatus = pageObj.guestTimelinePage().verifyrewardedRedeemableCampaign();
		String rewardedRedeemable = rewardedRedeemableStatus.replace("Rewarded ", "");
		Assert.assertEquals(rewardedRedeemable, redeemable, "Rewarded Redeemable notification did not displayed...");
		utils.logPass("Redeemable whose name is " + redeemable + " which is attached with Signup campaign with name "
				+ campaignName + " is found on user Timeline");

		// verifying In_segment query using API
		Response userInSegmentResp2 = pageObj.endpoints().userInSegment(userEmail, dataSet.get("apiKey"),
				dataSet.get("segment_id1"));
		Assert.assertEquals(userInSegmentResp2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 201 did not matched for auth user signup api");
		utils.logit("PASS", "Verified that user " + userEmail + " is present in Segment");
		String result = userInSegmentResp2.jsonPath().get("result").toString();
		Assert.assertEquals(result, "true", "Guest is present in segment");
		utils.logit("PASS", "Verified that status of  " + userEmail + " is present in Segment");

		// navigate to Segment tab on user timeline
		pageObj.guestTimelinePage().navigateToTabs("Segments");
		boolean isGreenColourBGVisible = pageObj.segmentsPage().guestTimelineSegmentColor(dataSet.get("segment1"),
				"#dff0d8");
		Assert.assertTrue(isGreenColourBGVisible, "Expected Green colour NOT matched for segment");
		utils.logPass("Segment highlighted with green color on Guest Timeline");

		// verifying In_segment query using API
		Response userInSegmentResp1 = pageObj.endpoints().userInSegment(userEmail, dataSet.get("apiKey"),
				dataSet.get("segment_id2"));
		Assert.assertEquals(userInSegmentResp1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 201 did not matched for auth user signup api");
		utils.logit("PASS", "Verified that user " + userEmail + " is present in Segment");
		String result1 = userInSegmentResp1.jsonPath().get("result").toString();
		Assert.assertEquals(result1, "false", "Guest is present in segment");
		utils.logit("PASS", "Verified that status of  " + userEmail + " is not present in Segment");

		boolean isRedColourBGVisible = pageObj.segmentsPage().guestTimelineSegmentColor(dataSet.get("segment2"),
				"#f2dede");
		Assert.assertTrue(isRedColourBGVisible, "Expected RED colour NOT matched for segment");
		utils.logPass("Segment highlighted with red color on Guest Timeline");

		// Delete created campaign
		// pageObj.signupcampaignpage().deleteSignUpCampaign(signUpCampaignName);
	}

	@Test(description = "SQ-T3018 Verify Signup campaign working properly with Transactional, In Memory check and Non Transactional Segments", groups = {
			"regression", "dailyrun" }, priority = 1)
	@Owner(name = "Hardik Bhardwaj")
	public void T3018_SignupCampaignWithNonTransactionalSegment() throws InterruptedException {
		redeemable = dataSet.get("redeemable2");
		campaignName = CreateDateTime.getUniqueString("Automation SignUpCampaign" + dataSet.get("CamSeg2"));
		// Login to instance
//		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
//		pageObj.instanceDashboardPage().loginToInstance();

		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// check redeemable's availability in business and create redeemable if not
		// available
		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select ongoing dropdown value
		pageObj.campaignspage().selectOngoingdrpValue(dataSet.get("onGoingDrp"));
		pageObj.campaignspage().clickNewCampaignBtn();
		// create signup campaign with PN Email configured
		// pageObj.signupcampaignpage().setAudianceType(dataSet.get("audiance"));
		pageObj.signupcampaignpage().createWhatDetailsSignupCampaign(campaignName, dataSet.get("giftType"),
				dataSet.get("giftReason2"), dataSet.get("redeemable2"));
		boolean flag = pageObj.signupcampaignpage().verifySegmentListVisibility();
		Assert.assertTrue(flag, "In Segment dropdown Segment list are not showing");
		utils.logPass("In Segment dropdown Segment list are showing");

		pageObj.signupcampaignpage().setSegmentType(dataSet.get("segment2"));

		pageObj.signupcampaignpage().setPushNotification(dataSet.get("pushNotification2"));
		pageObj.signupcampaignpage().setEmailSubject(dataSet.get("emailSubject2"));
		pageObj.signupcampaignpage().setEmailTemplate(dataSet.get("emailTemplate2"));
		pageObj.signupcampaignpage().clickNextButton();
		pageObj.signupcampaignpage().createWhenDetailsCampaign();

		// Signup using mobile api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		// verifying In_segment query using API
		Response userInSegmentResp2 = pageObj.endpoints().userInSegment(userEmail, dataSet.get("apiKey"),
				dataSet.get("segment_id2"));
		Assert.assertEquals(userInSegmentResp2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 201 did not matched for auth user signup api");
		utils.logit("PASS", "Verified that user " + userEmail + " is present in Segment");
		String result = userInSegmentResp2.jsonPath().get("result").toString();
		Assert.assertEquals(result, "true", "Guest is present in segment");
		utils.logit("PASS", "Verified that status of  " + userEmail + " is present in Segment");

		// Timeline validation
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		String signUpCampaignName = pageObj.guestTimelinePage().getcampaignNameMasscampaign(campaignName);
		Assert.assertTrue(signUpCampaignName.equalsIgnoreCase(campaignName), "Campaign name did not matched");
		utils.logPass("Signup campaign with name " + campaignName + " is found on user Timeline");

		boolean campaignNotificationStatus = pageObj.guestTimelinePage().verifyCampaignNotification();
		Assert.assertTrue(campaignNotificationStatus, "Campaign notification did not displayed...");
		utils.logPass("Signup campaign with name " + campaignName
				+ " is found on user Timeline and Campaign Notification Status is :- " + campaignNotificationStatus);

		boolean pushNotificationStatus = pageObj.guestTimelinePage().verifyPushNotification();
		Assert.assertTrue(pushNotificationStatus, "Push notification did not displayed...");
		utils.logPass("Signup campaign with name " + campaignName
				+ " is found on user Timeline and Push Notification Status is :- " + pushNotificationStatus);

		String rewardedRedeemableStatus = pageObj.guestTimelinePage().verifyrewardedRedeemableCampaign();
		String rewardedRedeemable = rewardedRedeemableStatus.replace("Rewarded ", "");
		Assert.assertEquals(rewardedRedeemable, redeemable, "Rewarded Redeemable notification did not displayed...");
		utils.logPass("Redeemable whose name is " + redeemable + " which is attached with Signup campaign with name "
				+ campaignName + " is found on user Timeline");

		// Delete created campaign
		// pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value
		// pageObj.signupcampaignpage().deleteSignUpCampaign(campaignName);
	}

	// https://punchhdev.atlassian.net/browse/QAA-178?focusedCommentId=429338 - user
	// in not present in segment
//  @Test(
//      description = "SQ-T5743 Verify the functionality for "Single Scan Flow" segment type is working fine with discount type and discount amount attributes",
//      groups = "Regression")

	public void T5743_segmentTypeSingleScanFlow() throws InterruptedException, IOException, ParseException {
		String segmentName = CreateDateTime.getUniqueString("Single Scan Flow");
		campaignName = "AutomationMassOffer_Single Scan Flow_" + CreateDateTime.getTimeDateString();
		// String dateTime = CreateDateTime.getCurrentDate() + " 11:00 PM";
		int count = 0;
		// login to instance
//		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
//		pageObj.instanceDashboardPage().loginToInstance();

		pageObj.menupage().navigateToSubMenuItem("Settings", "Global Configuration");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage(
				"Enable discount type and amount for ssf segmentation", "check");
		pageObj.dashboardpage().clickOnUpdateGlobalConfigButton();

		pageObj.dashboardpage().navigateToAllBusinessPage();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// navigate to segments
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		pageObj.newSegmentHomePage().clickOnSwitchToClassicBtn();
		pageObj.segmentsBetaPage().setSegmentBetaName(segmentName);
		pageObj.segmentsBetaPage().selectSegmentType("Single Scan Flow");
		pageObj.segmentsBetaPage().addAttributeBUttton();

		// options in attributes list of Single Scan Flow Segment
		List<String> actualTypeAttributeList = pageObj.segmentsPage().getAttributeFromSegmentType("Attribute");
		List<String> expTypeAttributeList = Arrays.asList("Payment Type", "Payment Amount", "Discount Type",
				"Discount Amount");
		boolean paymentTypeFlag = false;
		if (actualTypeAttributeList.containsAll(expTypeAttributeList)) {
			paymentTypeFlag = true;
		}
		Assert.assertTrue(paymentTypeFlag, "Failed to verify options in attributes list  of Single Scan Flow Segment");
		utils.logPass("Verify options in attributes list  of Single Scan Flow Segment");
		pageObj.segmentsBetaPage().selectAttribute("Attribute", "Discount Type");
		pageObj.segmentsBetaPage().selectAttribute("Discount Type", "Any");

		// options in Discount Type list of Single Scan Flow Segment
		List<String> actualDiscountTypeAttributeList = pageObj.segmentsPage()
				.getAttributeFromSegmentType("Discount Type");
		List<String> expDiscountTypeAttributeList = Arrays.asList("Any", "RewardRedemption", "RedeemableRedemption",
				"SubscriptionRedemption", "CardRedemption", "BankedRewardRedemption");

		boolean discountTypeFlag = false;
		if (actualDiscountTypeAttributeList.containsAll(expDiscountTypeAttributeList)) {
			discountTypeFlag = true;
		}
		Assert.assertTrue(discountTypeFlag,
				"Failed to verify options in Discount Type list  of Single Scan Flow Segment");
		utils.logPass("Verify options in Discount Type list  of Single Scan Flow Segment");

		// select Discount Type and get segment guest count
		pageObj.segmentsBetaPage().selectAttribute("Discount Type", "Any");
		int segmentCount = pageObj.segmentsBetaPage().getSegmentSizeBeforeSave();
		Assert.assertTrue(segmentCount >= count,
				"Failed to verify guest count label, label not appeard for Attribute as Discount Type and Discount Type as Any");
		utils.logit("Verfied guest Count label appeard for Attribute as Discount Type and Discount Type as Any");

		// refresh page
		utils.refreshPage();
		pageObj.segmentsPage().setSegmentName(segmentName);
		pageObj.segmentsBetaPage().selectSegmentType("Single Scan Flow");
		pageObj.segmentsBetaPage().addAttributeBUttton();
		pageObj.segmentsBetaPage().selectAttribute("Attribute", "Discount Amount");
		pageObj.segmentsBetaPage().setOperator("Operator", "At least");
		pageObj.segmentsBetaPage().setValue("1");
		int segmentCount2 = pageObj.segmentsBetaPage().getSegmentSizeBeforeSave();
		Assert.assertTrue(segmentCount2 >= count,
				"Failed to verify guest count label, label not appeard for Attribute as Discount Amount and operator as At least");
		utils.logit("Verfied guest Count label appeard for Attribute as Discount Amount and operator as At least");

		pageObj.segmentsPage().saveAndShowSegmentBtn();
		String segmentId = pageObj.segmentsPage().getSegmentID();
		int segmentDefCount = pageObj.segmentsBetaPage().getSegmentCountSegmentDefination();

		Assert.assertTrue(segmentDefCount == segmentCount2,
				"Failed to verify guest Count label not appeard on segment overview page for channel as Email and Engagement Metric as Subscribed");
		utils.logit(
				"Verfied guest Count label appeard for on segment overview page channel as Email and Engagement Metric as Subscribed");

		String userEmail = pageObj.segmentsBetaPage().getSegmentGuestList(dataSet.get("apiKey"), segmentId);
		Assert.assertNotEquals(userEmail, "", "User is not present in segment");

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.campaignspage().selectMessagedrpValue(dataSet.get("messageDrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().setCampaignName(campaignName);
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().setAudienceType(dataSet.get("audiance"));
		pageObj.signupcampaignpage().setSegmentType(segmentName);
		pageObj.signupcampaignpage().setPushNotification(dataSet.get("pushNotification"));
		pageObj.signupcampaignpage().setEmailSubject(dataSet.get("emailSubject"));
		pageObj.signupcampaignpage().setEmailTemplate(dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().clickNextButton();
		pageObj.signupcampaignpage().setFrequency("Once");
		pageObj.campaignsbetaPage().setStartDateNew(0);
		pageObj.signupcampaignpage().clickScheduleBtn();

		String msg = pageObj.guestTimelinePage().validateSuccessMessage();
		Assert.assertTrue(msg.contains("Schedule created successfully"), "Success message text did not matched");
		utils.logPass("Mass Campaign scheduled successuly : " + campaignName);

		// run mass offer
		// navigate to Menu -> submenu
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().findMassCampaignNameandRun(campaignName);

		// Timeline validation
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		String massOfferCampaignName = pageObj.guestTimelinePage().getcampaignNameMasscampaign(campaignName);
		Assert.assertTrue(massOfferCampaignName.equalsIgnoreCase(campaignName), "Campaign name did not matched");
		utils.logPass("Mass Offer campaign with name " + campaignName + " is found on user Timeline");

		boolean campaignNotificationStatus = pageObj.guestTimelinePage().verifyCampaignNotification();
		Assert.assertTrue(campaignNotificationStatus, "Campaign notification did not displayed...");
		utils.logPass("Mass Offer campaign with name " + campaignName
				+ " is found on user Timeline and Campaign Notification Status is :- " + campaignName);

		boolean pushNotificationStatus = pageObj.guestTimelinePage().verifyPushNotification();
		Assert.assertTrue(pushNotificationStatus, "Push notification did not displayed...");
		utils.logPass("Mass Offer campaign with name " + campaignName
				+ " is found on user Timeline and Push Notification Status is :- " + pushNotificationStatus);

	}

	@Test(description = "SQ-T6521 verifies the functionality of Post-redemption Campaign, when eClub & Loyalty user redeem then it get rewards after replica lag  when in segment complex (Non_Transaction-OR Transactional)", priority = 2)
	@Owner(name = "Hardik Bhardwaj")
	public void T6521_verifyPostRedemptionCampaignTransactionalAndNonTransactional() throws Exception {

		campaignName = CreateDateTime.getUniqueString("PostRedemption Campaign with Transactional Segment Gifting");
		// login to instance
//		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
//		pageObj.instanceDashboardPage().loginToInstance();

		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().createWhatDetailsPostredemptionCampaign(campaignName, dataSet.get("giftType"),
				campaignName, dataSet.get("redeemable1"));
		pageObj.signupcampaignpage().setSegmentType(dataSet.get("segment1"));
		pageObj.signupcampaignpage().setCampaignTrigger("Guest Redeems an Offer");
		pageObj.signupcampaignpage().setCampTriggerRedeemable(dataSet.get("redeemableRedemption"));
		pageObj.signupcampaignpage().setFiniteGifting("1");
		pageObj.signupcampaignpage().setPushNotification(campaignName);
		pageObj.signupcampaignpage().setEmailSubject(dataSet.get("emailSubject1"));
		pageObj.signupcampaignpage().clickNextBtn();
		pageObj.signupcampaignpage().setStartDate();
		pageObj.signupcampaignpage().createWhenDetailsCampaign();
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "Post Redemption Campaign is not created...");
		utils.logit("Post Redemption campaign created successfully: " + campaignName);

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("API2 Signup is successful");

		// send reward to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				dataSet.get("redeemable_id"), "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		pageObj.utils().logPass("Api2  send reward amount to user is successful");

		// get reward id
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id"));
		utils.logPass("Reward id " + rewardId + " is generated successfully ");

		// perform redemption first time
		Response posRedeem = pageObj.endpoints().posRedemptionOfReward(userEmail, dataSet.get("locationkey"), rewardId);
		Assert.assertEquals(posRedeem.statusCode(), ApiConstants.HTTP_STATUS_OK, "api status code did not match");
		utils.logPass("Verified able to redeem the redeemable having the redeemable id --" + rewardId);

		String postRedemptionCampaignName = pageObj.guestTimelinePage().getCampaignNameByNotificationsAPILongPolling(
				campaignName, dataSet.get("client"), dataSet.get("secret"), token);
		String rewardGiftedAccountHistory = pageObj.guestTimelinePage().getUserAccountHistory(campaignName,
				dataSet.get("client"), dataSet.get("secret"), token);
		Assert.assertTrue(postRedemptionCampaignName.equalsIgnoreCase(campaignName), "Campaign name did not matched");
		Assert.assertTrue(
				rewardGiftedAccountHistory
						.contains("You were gifted: " + dataSet.get("redeemable1") + " (" + campaignName + ")"),
				"Gifted points did not appeared in account history");
		pageObj.utils().logPass(
				"Post purchase campaign detail: push notification, campaign name, validated successfully on timeline");

		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		// navigate to Segment tab on user timeline
		pageObj.guestTimelinePage().navigateToTabs("Segments");
		boolean isGreenColourBGVisible = pageObj.segmentsPage().guestTimelineSegmentColor(dataSet.get("segment1"),
				"#dff0d8");
		Assert.assertTrue(isGreenColourBGVisible, "Expected Green colour NOT matched for segment");
		utils.logPass("Segment highlighted with green color on Guest Timeline");

		// delete campaign from db
		// pageObj.utils().deleteCampaignFromDb(postRedemptionCampaignName, env);

		campaignName2 = CreateDateTime
				.getUniqueString("PostRedemption Campaign with Non Transactional Segment Gifting");

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().createWhatDetailsPostredemptionCampaign(campaignName2, dataSet.get("giftType"),
				campaignName2, dataSet.get("redeemable2"));
		pageObj.signupcampaignpage().setSegmentType(dataSet.get("segment2"));
		pageObj.signupcampaignpage().setCampaignTrigger("Guest Redeems an Offer");
		pageObj.signupcampaignpage().setCampTriggerRedeemable(dataSet.get("redeemableRedemption2"));
		pageObj.signupcampaignpage().setFiniteGifting("1");
		pageObj.signupcampaignpage().setPushNotification(campaignName2);
		pageObj.signupcampaignpage().setEmailSubject(dataSet.get("emailSubject2"));
		pageObj.signupcampaignpage().clickNextBtn();
		pageObj.signupcampaignpage().setStartDate();
		pageObj.signupcampaignpage().createWhenDetailsCampaign();
		boolean status2 = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status2, "Post Redemption Campaign is not created...");
		utils.logit("Post Redemption campaign created successfully: " + campaignName2);

		// send reward to user Reedemable
		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				dataSet.get("redeemable_id2"), "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse2.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		pageObj.utils().logPass("Api2  send reward amount to user is successful");

		// get reward id
		String rewardId2 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id2"));
		utils.logPass("Reward id " + rewardId2 + " is generated successfully ");

		// perform redemption first time
		Response posRedeem2 = pageObj.endpoints().posRedemptionOfReward(userEmail, dataSet.get("locationkey"),
				rewardId2);
		Assert.assertEquals(posRedeem2.statusCode(), ApiConstants.HTTP_STATUS_OK, "api status code did not match");
		utils.logPass("Verified able to redeem the redeemable having the redeemable id --" + rewardId2);

		String postRedemptionCampaignName2 = pageObj.guestTimelinePage()
				.getCampaignNameByNotificationsAPI(campaignName2, dataSet.get("client"), dataSet.get("secret"), token);
		String rewardGiftedAccountHistory2 = pageObj.guestTimelinePage().getUserAccountHistory(campaignName2,
				dataSet.get("client"), dataSet.get("secret"), token);
		Assert.assertTrue(postRedemptionCampaignName2.equalsIgnoreCase(campaignName2), "Campaign name did not matched");
		Assert.assertTrue(
				rewardGiftedAccountHistory2
						.contains("You were gifted: " + dataSet.get("redeemable2") + " (" + campaignName2 + ")"),
				"Gifted points did not appeared in account history");
		pageObj.utils().logPass(
				"Post purchase campaign detail: push notification, campaign name, validated successfully on timeline");

		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		// navigate to Segment tab on user timeline
		pageObj.guestTimelinePage().navigateToTabs("Segments");
		boolean isGreenColourBGVisible2 = pageObj.segmentsPage().guestTimelineSegmentColor(dataSet.get("segment2"),
				"#dff0d8");
		Assert.assertTrue(isGreenColourBGVisible2,
				"Expected Green colour NOT matched for segment " + (dataSet.get("segment2")));
		utils.logPass("Segment " + (dataSet.get("segment2")) + " highlighted with green color on Guest Timeline");

		// delete campaign from db
		// pageObj.utils().deleteCampaignFromDb(campaignName2, env);

	}

	@Test(description = "SQ-T3648 Verify the functionality of Trigger based campaign -(Post checkin) with Transactional segment", priority = 3)
	@Owner(name = "Hardik Bhardwaj")
	public void T3648_verifyPostCheckinCampaignTransactionalAndNonTransactional() throws Exception {

		campaignName = CreateDateTime.getUniqueString("PostCheckin Campaign with Transactional Segment Gifting");
		// login to instance
//		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
//		pageObj.instanceDashboardPage().loginToInstance();

		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value

		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();

		pageObj.signupcampaignpage().createWhatDetailsPostcheckinCampaign(campaignName, dataSet.get("giftType"),
				campaignName, dataSet.get("redeemable1"));
		pageObj.signupcampaignpage().setSegmentType(dataSet.get("segment1"));
		pageObj.signupcampaignpage().createWhomDetailsPostcheckinCampaign(campaignName, dataSet.get("emailSubject1"),
				dataSet.get("emailSubject1"));

		pageObj.signupcampaignpage().createWhenDetailsCampaign();
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "Post Checkin Campaign is not created...");
		utils.logit("Post Checkin campaign created successfully: " + campaignName);

		// navigate to segment
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		// navigate to guest timeline that exists in segment attached to derived reward
		pageObj.segmentsPage().searchAndOpenSegment(dataSet.get("segment1"), dataSet.get("customSegmentId"));
		int guestCountInSegment = pageObj.segmentsBetaPage().getGuestInSegmentCount();
		Assert.assertTrue(guestCountInSegment > 0, "Guest count in segment is not greater than 0");
		String randomUserFromSegment = dataSet.get("customSegmentUser");

		// api 2 login
		Response loginResponse = pageObj.endpoints().Api2Login(randomUserFromSegment, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(loginResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token = loginResponse.jsonPath().get("access_token.token").toString();
		Assert.assertNotNull(token, "Token is null");

		// Pos api checkin
		String key = CreateDateTime.getTimeDateString();
		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T10:50:00+05:30";
		Response resp = pageObj.endpoints().posCheckin(date, randomUserFromSegment, key, txn,
				dataSet.get("locationkey"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp.getStatusCode(), "Status code 200 did not matched for pos checkin api");

		String postCheckinCampaignName = pageObj.guestTimelinePage().getCampaignNameByNotificationsAPILongPolling(
				campaignName, dataSet.get("client"), dataSet.get("secret"), token);
		String rewardGiftedAccountHistory = pageObj.guestTimelinePage().getUserAccountHistory(campaignName,
				dataSet.get("client"), dataSet.get("secret"), token);
		Assert.assertTrue(postCheckinCampaignName.equalsIgnoreCase(campaignName), "Campaign name did not matched");
		Assert.assertTrue(
				rewardGiftedAccountHistory
						.contains("You were gifted: " + dataSet.get("redeemable1") + " (" + campaignName + ")"),
				"Gifted points did not appeared in account history");
		pageObj.utils().logPass(
				"Post purchase campaign detail: push notification, campaign name, validated successfully on timeline");

		pageObj.instanceDashboardPage().navigateToGuestTimeline(randomUserFromSegment);
		// navigate to Segment tab on user timeline
		pageObj.guestTimelinePage().navigateToTabs("Segments");
		boolean isGreenColourBGVisible = pageObj.segmentsPage().guestTimelineSegmentColor(dataSet.get("segment1"),
				"#dff0d8");
		Assert.assertTrue(isGreenColourBGVisible, "Expected Green colour NOT matched for segment");
		utils.logPass("Segment highlighted with green color on Guest Timeline");

		// delete campaign from db
		// pageObj.utils().deleteCampaignFromDb(postCheckinCampaignName, env);

		campaignName2 = CreateDateTime.getUniqueString("PostCheckin Campaign with Non Transactional Segment Gifting");

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value

		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();

		pageObj.signupcampaignpage().createWhatDetailsPostcheckinCampaign(campaignName2, dataSet.get("giftType"),
				campaignName2, dataSet.get("redeemable2"));
		pageObj.signupcampaignpage().setSegmentType(dataSet.get("segment2"));
		pageObj.signupcampaignpage().createWhomDetailsPostcheckinCampaign(campaignName2, dataSet.get("emailSubject2"),
				dataSet.get("emailSubject2"));

		pageObj.signupcampaignpage().createWhenDetailsCampaign();
		boolean status1 = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status1, "Post Checkin Campaign is not created...");
		utils.logit("Post Checkin campaign created successfully: " + campaignName2);

		// create User
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUp = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(signUp.statusCode(), ApiConstants.HTTP_STATUS_OK, "sign up api status code did not match");
		utils.logPass("Api2 user signup is successful");
		String token2 = signUp.jsonPath().get("access_token.token").toString();
		String userID = signUp.jsonPath().get("user.user_id").toString();

		// Pos api checkin
		key = CreateDateTime.getTimeDateString();
		txn = "123456" + CreateDateTime.getTimeDateString();
		date = CreateDateTime.getCurrentDate() + "T10:50:00+05:30";
		Response resp2 = pageObj.endpoints().posCheckin(date, userEmail, key, txn, dataSet.get("locationkey"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp2.getStatusCode(), "Status code 200 did not matched for pos checkin api");

		String postCheckinCampaignName2 = pageObj.guestTimelinePage().getCampaignNameByNotificationsAPI(campaignName2,
				dataSet.get("client"), dataSet.get("secret"), token2);
		String rewardGiftedAccountHistory2 = pageObj.guestTimelinePage().getUserAccountHistory(campaignName2,
				dataSet.get("client"), dataSet.get("secret"), token2);
		Assert.assertTrue(postCheckinCampaignName2.equalsIgnoreCase(campaignName2), "Campaign name did not matched");
		Assert.assertTrue(
				rewardGiftedAccountHistory2
						.contains("You were gifted: " + dataSet.get("redeemable2") + " (" + campaignName2 + ")"),
				"Gifted points did not appeared in account history");
		pageObj.utils().logPass(
				"Post purchase campaign detail: push notification, campaign name, validated successfully on timeline");

		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		// navigate to Segment tab on user timeline
		pageObj.guestTimelinePage().navigateToTabs("Segments");
		boolean isGreenColourBGVisible2 = pageObj.segmentsPage().guestTimelineSegmentColor(dataSet.get("segment2"),
				"#dff0d8");
		Assert.assertTrue(isGreenColourBGVisible2,
				"Expected Green colour NOT matched for segment " + (dataSet.get("segment2")));
		utils.logPass("Segment " + (dataSet.get("segment2")) + " highlighted with green color on Guest Timeline");

		// delete campaign from db
		// pageObj.utils().deleteCampaignFromDb(campaignName2, env);

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() throws Exception {
		pageObj.utils().clearDataSet(dataSet);
		pageObj.utils().deleteCampaignFromDb(campaignName, env);
		pageObj.utils().deleteCampaignFromDb(campaignName2, env);
		pageObj.utils().logit("Data set cleared");
	}

	@AfterClass(alwaysRun = true)
	public void closeBrowser() {
		driver.quit();
		pageObj.utils().logit("Browser closed");
	}

}
