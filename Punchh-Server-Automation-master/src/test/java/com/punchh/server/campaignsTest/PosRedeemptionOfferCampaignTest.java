package com.punchh.server.campaignsTest;

import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class PosRedeemptionOfferCampaignTest {

	private static Logger logger = LogManager.getLogger(PosRedeemptionOfferCampaignTest.class);
	public WebDriver driver;
	private Properties prop;
	private PageObj pageObj;
	private String userEmail;
	private String sTCName;
	private String env;
	private String baseUrl;
	private static Map<String, String> dataSet;
	String run = "ui";
	private String postRedemptionCampaignName;

	@BeforeClass(alwaysRun = true)
	public void openBrowser() {
		prop = Utilities.loadPropertiesFile("config.properties");
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
		logger.info(sTCName + " ==>" + dataSet);
		// Instance move to all business page
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
	}

	@Test(description = "SQ-T4552 Verify post redemption offer campaign when don't re-gift flag is on and user has already unused redeemable", priority = 0)
	@Owner(name = "Vansham Mishra")
	public void T4552_postRedeemptionOfferCampaignWhenReGiftFlagON() throws InterruptedException, ParseException {
		postRedemptionCampaignName = "AutoPostRedeemption" + CreateDateTime.getTimeDateString()
				+ CreateDateTime.getHourAndMinuteForCurrentSystemTime(1);

		// Login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// go to campaign page
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		// select offer drpdown
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("offerDrpDown"));
		pageObj.campaignspage().clickNewCampaignBtn();

		// create new Pos Redeemption Campaign
		pageObj.campaignspage().createWhatDetailsPosRedeemptionCampaign(postRedemptionCampaignName,
				dataSet.get("giftType"), dataSet.get("reason"), dataSet.get("redeemable"));
		pageObj.campaignspage().setRedeemableInWhomPagePosRedeemableCampaign(dataSet.get("redeemable2"));
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick(dataSet.get("flagId"), "check");
		pageObj.campaignspage().setPNEmail(dataSet.get("pushNotification"), dataSet.get("email"));
		pageObj.signupcampaignpage().setStartDate();
		pageObj.signupcampaignpage().setEndDateforCouponCampaign(1);
		pageObj.signupcampaignpage().activateCampaign();

		// search and select campaign
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		String campaignID = pageObj.campaignspage().searchAndGetCampaignID(postRedemptionCampaignName);

		// user signup using api2
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		// nagivate to guest timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		// gift a redeemable
		pageObj.guestTimelinePage().giftRedeemableToUser(dataSet.get("reward"), dataSet.get("redeemable"));
		pageObj.guestTimelinePage().clickReward();

		pageObj.guestTimelinePage().giftRedeemableToUser(dataSet.get("reward"), dataSet.get("redeemable2"));
		pageObj.guestTimelinePage().clickReward();

		List<String> rewardLst = pageObj.guestTimelinePage().getRewardIds(dataSet.get("redeemable2"), 1);

		Response posRedeem1 = null;
		posRedeem1 = pageObj.endpoints().posRedemptionOfReward(userEmail, dataSet.get("locationkey"), rewardLst.get(0));
		Assert.assertEquals(posRedeem1.statusCode(), ApiConstants.HTTP_STATUS_OK, "api status code did not match");
		pageObj.utils().logPass("Verified able to redeem the redeemable having the redeemable id --" + rewardLst.get(0));

		// check the if the campaign trigger
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		boolean val = pageObj.guestTimelinePage().checkRedeemableGiftingThroughCampaignIDFalse(campaignID,
				dataSet.get("redeemable"));
		Assert.assertFalse(val, "User get the gifting which is not the expected behaviour");
		pageObj.utils().logPass("Verfied user did not get the gifting which is the expected behaviour");

		/*
		 * pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		 * pageObj.newCamHomePage().clickNewCamHomePageBtn();
		 * pageObj.newCamHomePage().searchCampaign(campaignName);
		 * pageObj.newCamHomePage().deleteCampaign(campaignName);
		 */
	}

	// Anant
	@Test(description = "SQ-T4553 Verify post redemption offer campaign when don't re-gift flag is off and user has already unused redeemable", priority = 1)
	@Owner(name = "Vansham Mishra")
	public void T4553_postRedeemptionOfferCampaignWhenReGiftFlagOff() throws InterruptedException, ParseException {
		postRedemptionCampaignName = "AutoPostRedeemption" + CreateDateTime.getTimeDateString()
				+ CreateDateTime.getHourAndMinuteForCurrentSystemTime(1)
				+ CreateDateTime.getHourAndMinuteForCurrentSystemTime(2);

		// Login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// go to campaign page
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		// select offer drpdown
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("offerDrpDown"));
		pageObj.campaignspage().clickNewCampaignBtn();

		// create new Pos Redeemption Campaign
		pageObj.campaignspage().createWhatDetailsPosRedeemptionCampaign(postRedemptionCampaignName,
				dataSet.get("giftType"), dataSet.get("reason"), dataSet.get("redeemable2"));
		pageObj.campaignspage().setRedeemableInWhomPagePosRedeemableCampaign(dataSet.get("redeemable"));
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick(dataSet.get("flagId"), "uncheck");
		pageObj.campaignspage().setPNEmail(
				dataSet.get("pushNotification") + " " + postRedemptionCampaignName + " " + "{{{reward_name}}}"
						+ "and reward id as" + "{{{reward_id}}}",
				dataSet.get("email") + " " + postRedemptionCampaignName + " " + "{{{business_name}}}"
						+ "and reward name as" + "{{{reward_name}}}");
		pageObj.signupcampaignpage().setStartDate();
		pageObj.signupcampaignpage().setEndDateforCouponCampaign(1);
		pageObj.signupcampaignpage().activateCampaign();

		// search and select campaign
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		String campaignID = pageObj.campaignspage().searchAndGetCampaignID(postRedemptionCampaignName);

		// create User
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUp = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(signUp.statusCode(), ApiConstants.HTTP_STATUS_OK, "sign up api status code did not match");
		pageObj.utils().logPass("Api2 user signup is successful");

		// nagivate to guest timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		// gift a redeemable
		pageObj.guestTimelinePage().giftRedeemableToUser(dataSet.get("reward"), dataSet.get("redeemable"));
		pageObj.guestTimelinePage().clickReward();

		// gift a redeemable
		pageObj.guestTimelinePage().giftRedeemableToUser(dataSet.get("reward"), dataSet.get("redeemable2"));
		pageObj.guestTimelinePage().clickReward();

		// gift a redeemable
		pageObj.guestTimelinePage().giftRedeemableToUser(dataSet.get("reward"), dataSet.get("redeemable"));
		pageObj.guestTimelinePage().clickReward();
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().clickReward();
		List<String> rewardLst = pageObj.guestTimelinePage().getRewardIds(dataSet.get("redeemable"), 1);

		Response posRedeem1 = null;
		posRedeem1 = pageObj.endpoints().posRedemptionOfReward(userEmail, dataSet.get("locationkey"), rewardLst.get(0));
		Assert.assertEquals(posRedeem1.statusCode(), ApiConstants.HTTP_STATUS_OK, "api status code did not match");
		pageObj.utils().logPass("Verified able to redeem the redeemable having the redeemable id --" + rewardLst.get(0));

		// check the if the campaign trigger
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		boolean val = pageObj.guestTimelinePage().checkRedeemableGiftingThroughCampaignID(campaignID,
				dataSet.get("redeemable2"));
		Assert.assertTrue(val, "User did  get the gifting which is not the expected behaviour");
		pageObj.utils().logPass("Verfied user get the gifting which is the expected behaviour");

		/*
		 * pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		 * pageObj.newCamHomePage().clickNewCamHomePageBtn();
		 * pageObj.newCamHomePage().searchCampaign(campaignName);
		 * pageObj.newCamHomePage().deleteCampaign(campaignName);
		 */
	}

	// Anant
	@Test(description = "SQ-T4560 Verify post redemption offer campaign when don't re-gift flag is on and user has already expired redeemable", priority = 2)
	@Owner(name = "Vansham Mishra")
	public void T4560_postRedeemptionOfferCampaignWhenReGiftFlagOnRedeemableExpired() throws Exception {
		postRedemptionCampaignName = "AutoPostRedeemption" + CreateDateTime.getTimeDateString()
				+ CreateDateTime.getHourAndMinuteForCurrentSystemTime(1);

		// Login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// go to campaign page
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		// select offer drpdown
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("offerDrpDown"));
		pageObj.campaignspage().clickNewCampaignBtn();

		// create new Pos Redeemption Campaign
		pageObj.campaignspage().createWhatDetailsPosRedeemptionCampaign(postRedemptionCampaignName,
				dataSet.get("giftType"), dataSet.get("reason"), dataSet.get("redeemable"));
		pageObj.campaignspage().setRedeemableInWhomPagePosRedeemableCampaign(dataSet.get("redeemable2"));
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick(dataSet.get("flagId"), "check");
		pageObj.campaignspage().setPNEmail(dataSet.get("pushNotification"), dataSet.get("email"));
		pageObj.signupcampaignpage().setStartDate();
		pageObj.signupcampaignpage().setEndDateforCouponCampaign(1);
		pageObj.signupcampaignpage().activateCampaign();

		// search and select campaign
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		String campaignID = pageObj.campaignspage().searchAndGetCampaignID(postRedemptionCampaignName);

		// create User
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUp = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(signUp.statusCode(), ApiConstants.HTTP_STATUS_OK, "sign up api status code did not match");
		pageObj.utils().logPass("Api2 user signup is successful");

		String userId = signUp.jsonPath().get("user.user_id").toString();
		pageObj.utils().logit("user id -- " + userId);

		// nagivate to guest timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		// gift a redeemable
		pageObj.guestTimelinePage().giftRedeemableToUser(dataSet.get("reward"), dataSet.get("redeemable"));
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().clickReward();
		List<String> rewardLst = pageObj.guestTimelinePage().getRewardIds(dataSet.get("redeemable"), 1);
		String rewardID = rewardLst.get(0);

		String query = "update rewards set end_time='2024-03-02 10:42:07' where user_id='" + userId + "' and id='"
				+ rewardID + "'";
		DBUtils.executeUpdateQuery(env, query);

		pageObj.guestTimelinePage().giftRedeemableToUser(dataSet.get("reward"), dataSet.get("redeemable2"));
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().clickReward();

		List<String> rewardLst2 = pageObj.guestTimelinePage().getRewardIds(dataSet.get("redeemable2"), 1);

		Response posRedeem1 = null;
		posRedeem1 = pageObj.endpoints().posRedemptionOfReward(userEmail, dataSet.get("locationkey"),
				rewardLst2.get(0));
		Assert.assertEquals(posRedeem1.statusCode(), ApiConstants.HTTP_STATUS_OK, "api status code did not match");
		pageObj.utils().logPass("Verified able to redeem the redeemable having the redeemable id --" + rewardLst2.get(0));

		// check the if the campaign trigger
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		boolean val = pageObj.guestTimelinePage().checkRedeemableGiftingThroughCampaignID(campaignID,
				dataSet.get("redeemable"));
		Assert.assertTrue(val, "User did not get the gifting which is not the expected behaviour");
		pageObj.utils().logPass("Verfied user get the gifting which is the expected behaviour");

		/*
		 * pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		 * pageObj.newCamHomePage().clickNewCamHomePageBtn();
		 * pageObj.newCamHomePage().searchCampaign(campaignName);
		 * pageObj.newCamHomePage().deleteCampaign(campaignName);
		 */
	}

	// Anant
	@Test(description = "SQ-T4561 Verify post redemption offer campaign when don't re-gift flag is off and user has already expired redeemable", priority = 3)
	@Owner(name = "Vansham Mishra")
	public void T4561_postRedeemptionOfferCampaignWhenReGiftFlagOffRedeemableExpired() throws Exception {
		postRedemptionCampaignName = "AutoPostRedeemption" + CreateDateTime.getTimeDateString()
				+ CreateDateTime.getHourAndMinuteForCurrentSystemTime(1);

		// Login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// go to campaign page
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		// select offer drpdown
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("offerDrpDown"));
		pageObj.campaignspage().clickNewCampaignBtn();

		// create new Pos Redeemption Campaign
		pageObj.campaignspage().createWhatDetailsPosRedeemptionCampaign(postRedemptionCampaignName,
				dataSet.get("giftType"), dataSet.get("reason"), dataSet.get("redeemable"));
		pageObj.campaignspage().setRedeemableInWhomPagePosRedeemableCampaign(dataSet.get("redeemable2"));
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick(dataSet.get("flagId"), "uncheck");
		pageObj.campaignspage().setPNEmail(dataSet.get("pushNotification"), dataSet.get("email"));
		pageObj.signupcampaignpage().setStartDate();
		pageObj.signupcampaignpage().setEndDateforCouponCampaign(1);
		pageObj.signupcampaignpage().activateCampaign();

		// search and select campaign
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		String campaignID = pageObj.campaignspage().searchAndGetCampaignID(postRedemptionCampaignName);

		// create User
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUp = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(signUp.statusCode(), ApiConstants.HTTP_STATUS_OK, "sign up api status code did not match");
		pageObj.utils().logPass("Api2 user signup is successful");

		String userId = signUp.jsonPath().get("user.user_id").toString();
		pageObj.utils().logit("user id -- " + userId);

		// nagivate to guest timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		// gift a redeemable
		pageObj.guestTimelinePage().giftRedeemableToUser(dataSet.get("reward"), dataSet.get("redeemable"));
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().clickReward();
		List<String> rewardLst = pageObj.guestTimelinePage().getRewardIds(dataSet.get("redeemable"), 1);
		String rewardID = rewardLst.get(0);

		// String query="update rewards set end_time='2024-03-02 10:42:07' where
		// user_id='"+userId+"' and id='"+rewardID+"'";
		String query = dataSet.get("query");
		query = query.replace("${userID}", userId).replace("${rewardID}", rewardID);
		DBUtils.executeUpdateQuery(env, query);

		pageObj.guestTimelinePage().giftRedeemableToUser(dataSet.get("reward"), dataSet.get("redeemable2"));
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().clickReward();

		List<String> rewardLst2 = pageObj.guestTimelinePage().getRewardIds(dataSet.get("redeemable2"), 1);

		Response posRedeem1 = null;
		posRedeem1 = pageObj.endpoints().posRedemptionOfReward(userEmail, dataSet.get("locationkey"),
				rewardLst2.get(0));
		Assert.assertEquals(posRedeem1.statusCode(), ApiConstants.HTTP_STATUS_OK, "api status code did not match");
		pageObj.utils().logPass("Verified able to redeem the redeemable having the redeemable id --" + rewardLst2.get(0));

		// check the if the campaign trigger
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		boolean val = pageObj.guestTimelinePage().checkRedeemableGiftingThroughCampaignID(campaignID,
				dataSet.get("redeemable"));
		Assert.assertTrue(val, "User did not get the gifting which is not the expected behaviour");
		pageObj.utils().logPass("Verfied user get the gifting which is the expected behaviour");

		/*
		 * pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		 * pageObj.newCamHomePage().clickNewCamHomePageBtn();
		 * pageObj.newCamHomePage().searchCampaign(campaignName);
		 * pageObj.newCamHomePage().deleteCampaign(campaignName);
		 */
	}

	// Rakhi
	@Test(description = "SQ-T5686 Verify deactivation of post redemption campaign when max gifting reached", priority = 4)
	@Owner(name = "Rakhi Rawat")
	public void T5686_verifyPostRedemptionCampiagnDeactivatedMaxGiftingReached() throws InterruptedException {

		postRedemptionCampaignName = CreateDateTime.getUniqueString("PostRedemptionCampaign");
		// login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.menupage().navigateToSubMenuItem("Settings", "Global Configuration");
		pageObj.dashboardpage().verifyGlobalConfigFlagCheckedUnchecked("Deactivate campaign when max gifting breached");
		pageObj.dashboardpage().navigateToAllBusinessPage();

		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Enable Bulking For Post Redemption Campaign
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Campaigns");
		pageObj.dashboardpage().checkUncheckFlagCockpitDashboard("Enable Bulking For Post Redemption Campaign",
				"check");
		pageObj.dashboardpage().updateCheckBox();

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().createWhatDetailsPostredemptionCampaign(postRedemptionCampaignName,
				dataSet.get("giftType"), postRedemptionCampaignName, dataSet.get("redeemable"));
		pageObj.signupcampaignpage().setSegmentType(dataSet.get("segmentName"));
		pageObj.signupcampaignpage().setCampaignTrigger("Guest Redeems an Offer");
		pageObj.signupcampaignpage().setCampTriggerRedeemable(dataSet.get("redeemableRedemption"));
		pageObj.signupcampaignpage().setFiniteGifting("2");
		pageObj.signupcampaignpage().setPushNotification(postRedemptionCampaignName);
		pageObj.signupcampaignpage().setEmailSubject(dataSet.get("emailSubject"));
		pageObj.signupcampaignpage().setEmailTemplate(dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().clickNextBtn();
		pageObj.signupcampaignpage().setStartDate();
		pageObj.signupcampaignpage().createWhenDetailsCampaign();
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "Post Redemption Campaign is not created...");
		pageObj.utils().logit("Post Redemption campaign created successfully: " + postRedemptionCampaignName);

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("API2 Signup is successful");

		// send reward to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				dataSet.get("redeemable_id"), "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get().pass("Api2  send reward amount to user is successful");
		// get reward id
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id"));
		pageObj.utils().logPass("Reward id " + rewardId + " is generated successfully ");
		// perform redemption first time
		Response posRedeem = pageObj.endpoints().posRedemptionOfReward(userEmail, dataSet.get("locationkey"), rewardId);
		Assert.assertEquals(posRedeem.statusCode(), ApiConstants.HTTP_STATUS_OK, "api status code did not match");
		pageObj.utils().logPass("Verified able to redeem the redeemable having the redeemable id --" + rewardId);

		// send reward to user Reedemable
		Response sendRewardResponse1 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				dataSet.get("redeemable_id"), "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse1.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get().pass("Api2  send reward amount to user is successful");
		// get reward id
		String rewardId1 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id"));
		pageObj.utils().logPass("Reward id " + rewardId1 + " is generated successfully ");
		// perform redemption second time
		Response posRedeem1 = pageObj.endpoints().posRedemptionOfReward(userEmail, dataSet.get("locationkey"),
				rewardId1);
		Assert.assertEquals(posRedeem1.statusCode(), ApiConstants.HTTP_STATUS_OK, "api status code did not match");
		pageObj.utils().logPass("Verified able to redeem the redeemable having the redeemable id --" + rewardId1);

		// verify campaign name through API
		String campaignName = pageObj.guestTimelinePage().getCampaignNameByNotificationsAPI(postRedemptionCampaignName,
				dataSet.get("client"), dataSet.get("secret"), token);
		Assert.assertTrue(campaignName.equalsIgnoreCase(postRedemptionCampaignName), "Campaign name did not matched");
		// verify gifting in account history
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().pingSessionforLongWait(1);
		pageObj.guestTimelinePage().clickAccountHistory();
		int itemCount = pageObj.accounthistoryPage().getAccountDetailsforItemGifted(postRedemptionCampaignName);
		Assert.assertTrue(itemCount == 2,
				"User did not get the gifting from first two checkin which is not the expected behaviour");
		pageObj.utils().logPass("User get the gifting from first two checkin which is expected behaviour");
		pageObj.guestTimelinePage().pingSessionforLongWait(4);

		// send reward to user Reedemable
		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				dataSet.get("redeemable_id"), "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse2.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get().pass("Api2  send reward amount to user is successful");
		// get reward id
		String rewardId2 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id"));
		pageObj.utils().logPass("Reward id " + rewardId2 + " is generated successfully ");
		// perform redemption third time
		Response posRedeem2 = pageObj.endpoints().posRedemptionOfReward(userEmail, dataSet.get("locationkey"),
				rewardId2);
		Assert.assertEquals(posRedeem2.statusCode(), ApiConstants.HTTP_STATUS_OK, "api status code did not match");
		pageObj.utils().logPass("Verified able to redeem the redeemable having the redeemable id --" + rewardId2);

		// timeline Validation
		pageObj.guestTimelinePage().refreshTimeline();
		int itemCount2 = pageObj.accounthistoryPage().getAccountDetailsforItemGifted(postRedemptionCampaignName);
		Assert.assertTrue(itemCount2 == 2,
				"User get the gifting from third checkin as well which is not the expected behaviour");
		pageObj.utils().logPass("User did not get the gifting after third checkin which is expected behaviour");

		pageObj.guestTimelinePage().pingSessionforLongWait(4);
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// check if campaign gets deactivated after max gifting
		String camStatus = pageObj.campaignspage().checkMassCampStatusBeforeOpening(postRedemptionCampaignName,
				"Inactive");
		Assert.assertEquals(camStatus, "Inactive", "Camapign not deactiaved after max gifting");
		pageObj.utils().logPass("Camapign gets deactiaved after max gifting");

	}

	@Test(description = "SQ-T5874 Verify re-gifting on next day when guest frequency Times per day when bulking enable", priority = 5)
	@Owner(name = "Hardik Bhardwaj")
	public void T5874_VerifyUserRegiftWorker() throws Exception {

		postRedemptionCampaignName = CreateDateTime.getUniqueString("Automation Postredemption Campaign");

		// login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.menupage().navigateToSubMenuItem("Settings", "Global Configuration");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Enhanced finite gifting flow", "check");
		pageObj.dashboardpage().clickOnUpdateGlobalConfigButton();
		pageObj.dashboardpage().navigateToAllBusinessPage();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// disable "Bulking For Post Redemption Campaign" flag
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Campaigns");
		pageObj.dashboardpage().checkUncheckFlagCockpitDashboard("Enable Bulking For Post Redemption Campaign",
				"check");
		pageObj.dashboardpage().updateCheckBox();

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().createWhatDetailsPostredemptionCampaign(postRedemptionCampaignName,
				dataSet.get("giftType"), postRedemptionCampaignName, dataSet.get("redeemable"));
		pageObj.signupcampaignpage().setCampaignTrigger("Guest Redeems an Offer");
		pageObj.signupcampaignpage().setCampTriggerRedeemable(dataSet.get("redeemableRedemption"));
		pageObj.signupcampaignpage().setGuestFrequencyOfGiting(dataSet.get("guestFrequencyOption"));
		pageObj.signupcampaignpage().enterTimesPerDay("2");
		pageObj.signupcampaignpage().checkUncheckFlag("Re-gift after reaching frequency limit", "ON");
		pageObj.signupcampaignpage().setPushNotification(postRedemptionCampaignName);
		pageObj.signupcampaignpage().setEmailSubject(dataSet.get("emailSubject"));
		pageObj.signupcampaignpage().setEmailTemplate(dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().clickNextBtn();
		pageObj.signupcampaignpage().setStartDate();
		pageObj.signupcampaignpage().createWhenDetailsCampaign();

		// search and select campaign
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		String campaignID = pageObj.campaignspage().searchAndGetCampaignID(postRedemptionCampaignName);

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("API2 Signup is successful");

		// send reward to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				dataSet.get("redeemable_id"), "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get().pass("Api2  send reward amount to user is successful");

		// get reward id
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id"));
		pageObj.utils().logPass("Reward id " + rewardId + " is generated successfully ");

		// perform redemption first time
		Response posRedeem = pageObj.endpoints().posRedemptionOfReward(userEmail, dataSet.get("locationkey"), rewardId);
		Assert.assertEquals(posRedeem.statusCode(), ApiConstants.HTTP_STATUS_OK, "api status code did not match");
		pageObj.utils().logPass("Verified able to redeem the redeemable having the redeemable id --" + rewardId);

		// verify campaign name through API
		String campaignName = pageObj.guestTimelinePage().getCampaignNameByNotificationsAPI(postRedemptionCampaignName,
				dataSet.get("client"), dataSet.get("secret"), token);
		Assert.assertTrue(campaignName.equalsIgnoreCase(postRedemptionCampaignName), "Campaign name did not matched");

		// check the if the campaign is trigger
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		boolean val = pageObj.guestTimelinePage().checkRedeemableGiftingThroughCampaignID(campaignID,
				dataSet.get("redeemable"));
		Assert.assertTrue(val, "User did not get the gifting which is not the expected behaviour");
		pageObj.utils().logPass("Verfied user get the gifting which is the expected behaviour");

		// send reward to user Reedemable
		Response sendRewardResponse1 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				dataSet.get("redeemable_id"), "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse1.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get().pass("Api2  send reward amount to user is successful");

		// get reward id
		String rewardId1 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id"));
		pageObj.utils().logPass("Reward id " + rewardId1 + " is generated successfully ");

		// perform redemption second time
		Response posRedeem1 = pageObj.endpoints().posRedemptionOfReward(userEmail, dataSet.get("locationkey"),
				rewardId1);
		Assert.assertEquals(posRedeem1.statusCode(), ApiConstants.HTTP_STATUS_OK, "api status code did not match");
		pageObj.utils().logPass("Verified able to redeem the redeemable having the redeemable id --" + rewardId1);

		// verify campaign name through API
		String campaignName1 = pageObj.guestTimelinePage().getCampaignNameByNotificationsAPI(postRedemptionCampaignName,
				dataSet.get("client"), dataSet.get("secret"), token);
		Assert.assertTrue(campaignName1.equalsIgnoreCase(postRedemptionCampaignName), "Campaign name did not matched");

		// check the if the campaign is trigger
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		boolean val1 = pageObj.guestTimelinePage().checkRedeemableGiftingThroughCampaignID(campaignID,
				dataSet.get("redeemable"));
		Assert.assertTrue(val1, "User did not get the gifting which is not the expected behaviour");
		pageObj.utils().logPass("Verfied user get the gifting Second time which is the expected behaviour");

		// send reward to user Reedemable
		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				dataSet.get("redeemable_id"), "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse2.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get().pass("Api2  send reward amount to user is successful");

		// get reward id
		String rewardId2 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id"));
		pageObj.utils().logPass("Reward id " + rewardId2 + " is generated successfully ");

		pageObj.guestTimelinePage().pingSessionforLongWait(1);
		// perform redemption third time
		Response posRedeem2 = pageObj.endpoints().posRedemptionOfReward(userEmail, dataSet.get("locationkey"),
				rewardId2);
		Assert.assertEquals(posRedeem2.statusCode(), ApiConstants.HTTP_STATUS_OK, "api status code did not match");
		pageObj.utils().logPass("Verified able to redeem the redeemable having the redeemable id --" + rewardId1);

		// user timeline Validation
		pageObj.guestTimelinePage().refreshTimeline();
		pageObj.guestTimelinePage().clickAccountHistory();
		int itemCount = pageObj.accounthistoryPage().getAccountDetailsforItemGifted(postRedemptionCampaignName);
		Assert.assertTrue(itemCount == 2,
				"User get the gifting after third redemption which is not the expected behaviour");
		pageObj.utils().logPass("User did not get the gifting after third redemption which is expected behaviour");

		// validate sidekiq jobs for UserRegiftingWorker
		int count = pageObj.sidekiqPage().checkSidekiqJobWithId(baseUrl, "UserRegiftingWorker",
				dataSet.get("business_id"));
		Assert.assertTrue(count == 1, "UserRegiftingWorker count did not matched");
		pageObj.utils().logPass("UserRegiftingWorker count matched ie : " + count);
		pageObj.sidekiqPage().checkSelectAllJobsCheckBox();
		// delete UserRegiftingWorker job
//		pageObj.sidekiqPage().deleteSidekiqJob();

		// Login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		 * 
		 * pageObj.utils().deleteCampaignByIdCampaignsTable(campaignID, env);
		 */
	}

	@Test(description = "SQ-T6442 Verify gifting from post redemption campaign when business has date 9 Jun and in UTC date is 10 June", priority = 6)
	@Owner(name = "Shubham Gupta")
	public void T6442_verifyGiftingAndRewardEndDate() throws Exception {

		postRedemptionCampaignName = CreateDateTime.getUniqueString("Automation Postredemption Campaign");

		String timezone = pageObj.signupcampaignpage().getTimezoneGreaterOrLessThanUTC();

		// login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().dashboardPageMiscellaneousConfig();
		pageObj.cockpitDashboardMiscPage().setBusinessTimezone("New Delhi ( IST )"); // Asia/Kolkata;

		// disable "Bulking For Post Redemption Campaign" flag
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Campaigns");
		pageObj.dashboardpage().checkUncheckFlagCockpitDashboard("Enable Bulking For Post Redemption Campaign",
				"uncheck");
		pageObj.dashboardpage().updateCheckBox();

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().createWhatDetailsPostredemptionCampaign(postRedemptionCampaignName,
				dataSet.get("giftType"), postRedemptionCampaignName, dataSet.get("redeemable"));
		pageObj.signupcampaignpage().setCampaignTrigger("Guest Redeems an Offer");
		pageObj.signupcampaignpage().setCampTriggerRedeemable(dataSet.get("redeemableRedemption"));
		pageObj.signupcampaignpage().setPushNotification(postRedemptionCampaignName);
		pageObj.signupcampaignpage().setEmailSubject(dataSet.get("emailSubject"));
		pageObj.signupcampaignpage().setEmailTemplate(dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().clickNextBtn();
		pageObj.signupcampaignpage().setStartDate();
		pageObj.signupcampaignpage().setEndDateforCouponCampaign(0);
		pageObj.signupcampaignpage().createWhenDetailsCampaign();

		// search and select campaign
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		String campaignID = pageObj.campaignspage().searchAndGetCampaignID(postRedemptionCampaignName);

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("API2 Signup is successful");

		// send reward to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				dataSet.get("redeemable_id"), "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get().pass("Api2  send reward amount to user is successful");

		// get reward id
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id"));
		pageObj.utils().logPass("Reward id " + rewardId + " is generated successfully ");

		// perform redemption first time
		Response posRedeem = pageObj.endpoints().posRedemptionOfReward(userEmail, dataSet.get("locationkey"), rewardId);
		Assert.assertEquals(posRedeem.statusCode(), ApiConstants.HTTP_STATUS_OK, "api status code did not match");
		pageObj.utils().logPass("Verified able to redeem the redeemable having the redeemable id --" + rewardId);

		// verify campaign name through API
		String campaignName = pageObj.guestTimelinePage().getCampaignNameByNotificationsAPI(postRedemptionCampaignName,
				dataSet.get("client"), dataSet.get("secret"), token);
		Assert.assertTrue(campaignName.equalsIgnoreCase(postRedemptionCampaignName), "Campaign name did not matched");

		// check the if the campaign is trigger
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		boolean val = pageObj.guestTimelinePage().checkRedeemableGiftingThroughCampaignID(campaignID,
				dataSet.get("redeemable"));
		Assert.assertTrue(val, "User did not get the gifting which is not the expected behaviour");
		pageObj.utils().logPass("Verfied user get the gifting which is the expected behaviour");

		pageObj.utils().updateBusinessTimezone(dataSet.get("slug"), timezone, env);
		// Thread.sleep(10000);

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse1 = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse1, "API 2 user signup");
		String token1 = signUpResponse1.jsonPath().get("access_token.token").toString();
		String userID1 = signUpResponse1.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("API2 Signup is successful");

		// send reward to user Reedemable
		Response sendRewardResponse1 = pageObj.endpoints().sendMessageToUser(userID1, dataSet.get("apiKey"), "",
				dataSet.get("redeemable_id"), "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse1.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get().pass("Api2  send reward amount to user is successful");

		// get reward id
		String rewardId1 = pageObj.redeemablesPage().getRewardId(token1, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id"));
		pageObj.utils().logPass("Reward id " + rewardId1 + " is generated successfully ");

		// perform redemption second time
		Response posRedeem1 = pageObj.endpoints().posRedemptionOfReward(userEmail, dataSet.get("locationkey"),
				rewardId1);
		Assert.assertEquals(posRedeem1.statusCode(), ApiConstants.HTTP_STATUS_OK, "api status code did not match");
		pageObj.utils().logPass("Verified able to redeem the redeemable having the redeemable id --" + rewardId1);

		// verify campaign name through API
		String campaignName1 = pageObj.guestTimelinePage().getCampaignNameByNotificationsAPI(postRedemptionCampaignName,
				dataSet.get("client"), dataSet.get("secret"), token1);
		Assert.assertFalse(campaignName1.equalsIgnoreCase(postRedemptionCampaignName), "Campaign name matched");

		// check if the campaign is trigger
		String rewardGiftedAccountHistory = pageObj.guestTimelinePage().getUserAccountHistory(
				postRedemptionCampaignName, dataSet.get("client"), dataSet.get("secret"), token1);
		String expectedResult = "You were gifted: " + dataSet.get("redeemable") + " (" + postRedemptionCampaignName
				+ ")";
		Assert.assertFalse(campaignName1.equalsIgnoreCase(postRedemptionCampaignName), "Campaign name matched");
		Assert.assertFalse(rewardGiftedAccountHistory.contains(expectedResult),
				"Gifted rewards appeared in account history");
		pageObj.utils().logPass("Verfied user didn't get the gifting Second time which is the expected behaviour");

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() throws Exception {
		pageObj.utils().clearDataSet(dataSet);
		pageObj.utils().deleteCampaignFromDb(postRedemptionCampaignName, env);
		logger.info("Data set cleared");
	}

	@AfterClass(alwaysRun = true)
	public void closeBrowser() {
		driver.quit();
		logger.info("Browser closed");
	}

}
