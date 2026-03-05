package com.punchh.server.campaignsTest;

import java.lang.reflect.Method;
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
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.annotations.Owner;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class PostRedemptionCampaignTest {

	private static Logger logger = LogManager.getLogger(PostRedemptionCampaignTest.class);
	public WebDriver driver;
	private Properties prop;
	private PageObj pageObj;
	private String userEmail;
	private String sTCName;
	private String env;
	private String baseUrl;
	private static Map<String, String> dataSet;
	private String run = "ui";
	private String campaignName, campaignName2, campaignName3, redeemableName2;

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

	// shaleen
	@Test(description = "SQ-T5073 (1.0) Verify the redeemable name if it is not expired in case of post redemption campaign || "
			+ "SQ-T5074 (1.0) Verify the redeemable name if it is already expired in case of post redemption campaign", priority = 0)
	@Owner(name = "Shaleen Gupta")
	public void T5073_verifyRedeemableName() throws Exception {

		campaignName = "postRedemptionCampaign_T344_" + CreateDateTime.getTimeDateString();

		// open instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// create post redemption offer campaign
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().createWhatDetailsPostredemptionCampaign(campaignName, dataSet.get("giftType"),
				dataSet.get("giftReason"), dataSet.get("redeemable"));
		// campaignid = pageObj.signupcampaignpage().getCampaignid();
		pageObj.signupcampaignpage().setCampaignTrigger("Guest Redeems an Offer");
		pageObj.signupcampaignpage().setCampTriggerRedeemable(dataSet.get("redeemableRedemption"));
		pageObj.signupcampaignpage().setPushNotification(dataSet.get("pushNotification"));
		pageObj.signupcampaignpage().setEmailSubject(dataSet.get("emailSubject"));
		pageObj.signupcampaignpage().setEmailTemplate(dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().setStartDate();
		pageObj.signupcampaignpage().activateCampaign();
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "Post Redemption offer Campaign is not created...");
		logger.info("Post Redemption offer campaign created successfully: " + campaignName);
		pageObj.utils().logPass(" Post Redemption offer campaign created successfully: " + campaignName);

		// search and verify 'redeemable name' & 'message'
		pageObj.campaignspage().searchAndSelectCamapign(campaignName);
		pageObj.campaignspage().selectCPPOptions("Edit");
		String text = pageObj.signupcampaignpage().getRedeemableAttachedToCampaign();
		Assert.assertEquals(text, dataSet.get("redeemable"), " Redeemable name does not match ");
		logger.info(" Verified Reedemable is attached to campaign");
		pageObj.utils().logPass(" Verified Reedemable is attached to campaign");

		String text2 = pageObj.signupcampaignpage().getMessageUnderBox("Redeemable Reward");
		String expectedText = dataSet.get("messageUnderBox").replace("{$name}", dataSet.get("redeemable"));
		Assert.assertEquals(text2, expectedText, " Expected message does not match ");
		logger.info(" Verified message under redeemable box ");
		pageObj.utils().logit(" Verified message under redeemable box ");

		/*-----SQ-T5074 (1.0)------*/

		redeemableName2 = "Redeemable_T345_" + CreateDateTime.getTimeDateString();
		campaignName2 = "postRedemptionCampaign_T345_" + CreateDateTime.getTimeDateString();
		// String campaignid2;
		// create redeemable
		pageObj.menupage().navigateToSubMenuItem("Settings", "Redeemables");
		pageObj.redeemablePage().createRedeemable(redeemableName2);
		pageObj.redeemablePage().selectRecieptRule("2");
		pageObj.redeemablePage().expiryInDays("", "3");
		pageObj.redeemablePage().clickFinishBtn();

		boolean flag2 = pageObj.redeemablePage().successOrErrorConfirmationMessage("Redeemable successfully saved.");
		Assert.assertTrue(flag2, " Failed to create Redeemable ");
		logger.info(" Redeemable is created successfully: " + redeemableName2);
		pageObj.utils().logPass("Redeemable is created successfully : " + redeemableName2);

		String redeemableID = pageObj.redeemablePage().getRedeemableID(redeemableName2);

		// create post redemption offer campaign
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().createWhatDetailsPostredemptionCampaign(campaignName2, dataSet.get("giftType"),
				dataSet.get("giftReason"), redeemableName2);
		// campaignid2 = pageObj.signupcampaignpage().getCampaignid();
		pageObj.signupcampaignpage().setCampaignTrigger("Guest Redeems an Offer");
		pageObj.signupcampaignpage().setCampTriggerRedeemable(dataSet.get("redeemableRedemption"));
		pageObj.signupcampaignpage().setPushNotification(dataSet.get("pushNotification"));
		pageObj.signupcampaignpage().setEmailSubject(dataSet.get("emailSubject"));
		pageObj.signupcampaignpage().setEmailTemplate(dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().setStartDate();
		pageObj.signupcampaignpage().activateCampaign();
		boolean status2 = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status2, "Post Redemption offer Campaign is not created...");
		logger.info("Post Redemption offer campaign created successfully: " + campaignName2);
		pageObj.utils().logPass(" Post Redemption offer campaign created successfully: " + campaignName2);

		// set redeemable end date in past
		String query = "UPDATE redeemables SET end_time= '2023-03-10 06:30:00' WHERE id ='" + redeemableID + "';";
		int status3 = DBUtils.executeUpdateQuery(env, query);
		Assert.assertEquals(status3, 1, " Unable to update end date in past ");
		logger.info(" Successfully Updated end date in past ");
		pageObj.utils().logPass(" Successfully Updated end date in past ");

		pageObj.campaignspage().refreshPage();
		// search and verify 'redeemable name' & 'message'
		pageObj.campaignspage().searchAndSelectCamapign(campaignName2);
		pageObj.campaignspage().selectCPPOptions("Edit");
		String text3 = pageObj.signupcampaignpage().getRedeemableAttachedToCampaign();
		Assert.assertNull(text3, " Redeemable is attached in campaign ");
		logger.info(" Verified that Reedemable is not attached to campaign");
		pageObj.utils().logPass(" Verified that Reedemable is not attached to campaign");

		String text4 = pageObj.signupcampaignpage().getMessageUnderBox("Redeemable Reward");
		String expectedText2 = dataSet.get("messageUnderBox2").replace("{$name}", redeemableName2);
		Assert.assertEquals(text4, expectedText2, " Expected message does not match ");
		logger.info(" Verified message-> redeemable is expired :" + redeemableName2);
		pageObj.utils().logit(" Verified message-> redeemable is expired :" + redeemableName2);

		// delete campaign and redeemable
		/*
		 * pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		 * pageObj.campaignspage().removeSearchedCampaign(CampaignName);
		 * pageObj.campaignspage().removeSearchedCampaign(CampaignName2);
		 */

		// Commented these line of code because we are not able to delete redeemable
		// when it is used in camp
//		pageObj.menupage().navigateToSubMenuItem("Settings", "Redeemables");
//		pageObj.redeemablePage().deleteRedeemable(redeemableName2);
		// pageObj.utils().deleteCampaignFromDb(campaignName, env);
		// pageObj.utils().deleteCampaignFromDb(campaignName2, env);
		// pageObj.utils().deleteRedeemableByName(redeemableName2, env);

	}

	// Rakhi
	@Test(description = "SQ-T5674 Verify duplicate job of User re-gift worker when bulking is disabled"
			+ "SQ-T6059 Verify duplicate job of UserRegiftWorker when bulking enabled", priority = 2, dataProvider = "TestDataProvider")
	@Owner(name = "Rakhi Rawat")
	public void T5674_VerifyduplicateJobUserRegiftWorkerWithBulkingDisabled(String flagName, String checkBoxFlag)
			throws Exception {

		String reward_Code = "";
		campaignName = CreateDateTime.getUniqueString("Automation Postredemption Campaign");
		campaignName2 = CreateDateTime.getUniqueString("Postredemption Campaign Two");
		campaignName3 = CreateDateTime.getUniqueString("Postredemption Campaign Three");
		String campaignid2;
		String campaignid3;
		// open instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// disable "Bulking For Post Redemption Campaign" flag
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Campaigns");
		pageObj.dashboardpage().checkUncheckFlagCockpitDashboard(flagName, checkBoxFlag);
		pageObj.dashboardpage().updateCheckBox();
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.menupage().dashboardPageMiscellaneousConfig();
		pageObj.cockpitDashboardMiscPage().setBusinessTimezone("New Delhi ( IST )");
		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().createWhatDetailsPostredemptionCampaign(campaignName, dataSet.get("giftType"),
				campaignName, dataSet.get("redeemable"));
		// campaignid = pageObj.signupcampaignpage().getCampaignid();
		pageObj.signupcampaignpage().setCampaignTrigger("Guest Redeems an Offer");
		pageObj.signupcampaignpage().setCampTriggerRedeemable(dataSet.get("redeemableRedemption"));
		pageObj.signupcampaignpage().setGuestFrequencyOfGiting(dataSet.get("guestFrequencyOption"));
		pageObj.signupcampaignpage().enterTimesPerDay("1");
		pageObj.signupcampaignpage().checkUncheckFlag("Re-gift", "ON");
		pageObj.signupcampaignpage().setPushNotification(campaignName);
		pageObj.signupcampaignpage().setEmailSubject(dataSet.get("emailSubject"));
		pageObj.signupcampaignpage().setEmailTemplate(dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().clickNextBtn();
		pageObj.signupcampaignpage().setStartDate();
		pageObj.signupcampaignpage().createWhenDetailsCampaign();

		// search and select campaign
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		String campaignID = pageObj.campaignspage().searchAndGetCampaignID(campaignName);

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("API2 Signup is successful");
		logger.info("API2 Signup is successful");

		// send reward to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				dataSet.get("redeemable_id"), "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		pageObj.utils().logPass("Api2  send reward amount to user is successful");

		// get reward id
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id"));
		logger.info("Reward id " + rewardId + " is generated successfully ");
		pageObj.utils().logPass("Reward id " + rewardId + " is generated successfully ");

		// perform redemption first time
		Response posRedeem = pageObj.endpoints().posRedemptionOfReward(userEmail, dataSet.get("locationkey"), rewardId);
		Assert.assertEquals(posRedeem.statusCode(), ApiConstants.HTTP_STATUS_OK, "api status code did not match");
		logger.info("Verified able to redeem the redeemable having the redeemable id --" + rewardId);
		pageObj.utils().logPass("Verified able to redeem the redeemable having the redeemable id --" + rewardId);

		// verify campaign name through API
		String postRedemptionCampaignName1 = pageObj.guestTimelinePage().getCampaignNameByNotificationsAPI(campaignName,
				dataSet.get("client"), dataSet.get("secret"), token);
		Assert.assertTrue(postRedemptionCampaignName1.equalsIgnoreCase(campaignName), "Campaign name did not matched");

		// check the if the campaign is trigger
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		boolean val = pageObj.guestTimelinePage().checkRedeemableGiftingThroughCampaignID(campaignID,
				dataSet.get("redeemable"));
		Assert.assertTrue(val, "User did not get the gifting which is not the expected behaviour");
		logger.info("Verfied user get the gifting which is the expected behaviour");
		pageObj.utils().logPass("Verfied user get the gifting which is the expected behaviour");

		// send reward to user Reedemable
		Response sendRewardResponse1 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				dataSet.get("redeemable_id"), "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse1.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		pageObj.utils().logPass("Api2  send reward amount to user is successful");

		// get reward id
		String rewardId1 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id"));
		logger.info("Reward id " + rewardId1 + " is generated successfully ");
		pageObj.utils().logPass("Reward id " + rewardId1 + " is generated successfully ");

		// perform redemption second time
		Response posRedeem1 = pageObj.endpoints().posRedemptionOfReward(userEmail, dataSet.get("locationkey"),
				rewardId1);
		Assert.assertEquals(posRedeem1.statusCode(), ApiConstants.HTTP_STATUS_OK, "api status code did not match");
		logger.info("Verified able to redeem the redeemable having the redeemable id --" + rewardId1);
		pageObj.utils().logPass("Verified able to redeem the redeemable having the redeemable id --" + rewardId1);

		// user timeline Validation
		pageObj.guestTimelinePage().refreshTimeline();
		pageObj.guestTimelinePage().clickAccountHistory();
		int itemCount = pageObj.accounthistoryPage().getAccountDetailsforItemGifted(campaignName);
		Assert.assertTrue(itemCount == 1,
				"User get the gifting after second redemption which is not the expected behaviour");
		logger.info("User did not get the gifting after second redemption which is expected behaviour");
		pageObj.utils().logPass("User did not get the gifting after second redemption which is expected behaviour");

		// validate sidekiq jobs for UserRegiftingWorker
		int count = pageObj.sidekiqPage().checkSidekiqJobWithId(baseUrl, "UserRegiftingWorker",
				dataSet.get("business_id"));
		Assert.assertTrue(count == 1, "UserRegiftingWorker count did not matched");
		logger.info("UserRegiftingWorker count matched ie : " + count);
		pageObj.utils().logPass("UserRegiftingWorker count matched ie : " + count);
		pageObj.sidekiqPage().checkSelectAllJobsCheckBox();
		// delete UserRegiftingWorker job
		pageObj.sidekiqPage().deleteSidekiqJob();

		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// create 2 new post redemption campaign
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// search and duplicate campaign
		pageObj.campaignspage().searchAndSelectCamapign(campaignName);
		pageObj.campaignspage().createDuplicateCampaignOnClassicPage(campaignName, "Edit");
		pageObj.signupcampaignpage().createWhatDetailsPostredemptionCampaign(campaignName2, dataSet.get("giftType"),
				campaignName2, dataSet.get("redeemable"));
		campaignid2 = pageObj.signupcampaignpage().getCampaignid();
		pageObj.signupcampaignpage().clickNextBtn();
		pageObj.signupcampaignpage().setStartDate();
		pageObj.signupcampaignpage().activateCampaign();

		// search and duplicate campaign
		pageObj.campaignspage().searchAndSelectCamapign(campaignName);
		pageObj.campaignspage().createDuplicateCampaignOnClassicPage(campaignName, "Edit");
		pageObj.signupcampaignpage().createWhatDetailsPostredemptionCampaign(campaignName3, dataSet.get("giftType"),
				campaignName3, dataSet.get("redeemable"));
		campaignid3 = pageObj.signupcampaignpage().getCampaignid();
		pageObj.signupcampaignpage().clickNextBtn();
		pageObj.signupcampaignpage().setStartDate();
		pageObj.signupcampaignpage().activateCampaign();

		// send reward to user Reedemable
		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				dataSet.get("redeemable_id"), "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse2.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		pageObj.utils().logPass("Api2  send reward amount to user is successful");

		// get reward id
		String rewardId2 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id"));
		logger.info("Reward id " + rewardId2 + " is generated successfully ");
		pageObj.utils().logPass("Reward id " + rewardId2 + " is generated successfully ");

		// perform redemption first time
		Response posRedeem2 = pageObj.endpoints().posRedemptionOfReward(userEmail, dataSet.get("locationkey"),
				rewardId2);
		Assert.assertEquals(posRedeem2.statusCode(), ApiConstants.HTTP_STATUS_OK, "api status code did not match");
		logger.info("Verified able to redeem the redeemable having the redeemable id --" + rewardId2);
		pageObj.utils().logPass("Verified able to redeem the redeemable having the redeemable id --" + rewardId2);

		// check the if the campaign trigger
		pageObj.guestTimelinePage().pingSessionforLongWait(0);
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().clickAccountHistory();
		int itemCount2 = pageObj.accounthistoryPage().getAccountDetailsforItemGiftedWithPooling(campaignName3,1);
		Assert.assertTrue(itemCount2 == 1,
				"User did not get the after first redemption gifting which is not the expected behaviour");
		logger.info("User get the gifting after first redemption which is expected behaviour");
		pageObj.utils().logPass("User get the gifting after first redemption which is expected behaviour");

		// send reward to user Reedemable
		Response sendRewardResponse3 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				dataSet.get("redeemable_id"), "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse3.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		pageObj.utils().logPass("Api2  send reward amount to user is successful");

		// get reward id
		String rewardId3 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id"));
		logger.info("Reward id " + rewardId3 + " is generated successfully ");
		pageObj.utils().logPass("Reward id " + rewardId3 + " is generated successfully ");

		pageObj.guestTimelinePage().pingSessionforLongWait(2);
		// perform redemption second time
		Response posRedeem3 = pageObj.endpoints().posRedemptionOfReward(userEmail, dataSet.get("locationkey"),
				rewardId3);
		Assert.assertEquals(posRedeem3.statusCode(), ApiConstants.HTTP_STATUS_OK, "api status code did not match");
		logger.info("Verified able to redeem the redeemable having the redeemable id --" + rewardId3);
		pageObj.utils().logPass("Verified able to redeem the redeemable having the redeemable id --" + rewardId3);

		// timeline Validation
		pageObj.guestTimelinePage().refreshTimeline();
		pageObj.guestTimelinePage().clickAccountHistory();
		int itemCount3 = pageObj.accounthistoryPage().getAccountDetailsforItemGifted(campaignName3);
		Assert.assertTrue(itemCount3 == 1,
				"User get the gifting after second redemption which is not the expected behaviour");
		logger.info("User did not get the gifting after second redemption which is expected behaviour");
		pageObj.utils().logPass("User did not get the gifting after second redemption which is expected behaviour");

		// validate that another UserRegiftingWorker job is not created on sidekiq even
		// when same type of campaign is created and regifitng is done though the same.
		int count2 = pageObj.sidekiqPage().checkSidekiqJobWithPolling(baseUrl, "UserRegiftingWorker");
		Assert.assertTrue(count2 == 1, "UserRegiftingWorker count did not matched");
		logger.info("UserRegiftingWorker count matched ie : " + count2);
		pageObj.utils().logPass("UserRegiftingWorker count matched ie : " + count2);

		// Login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug")); //
		 * Delete created campaign pageObj.menupage().navigateToSubMenuItem("Campaigns",
		 * "Campaigns"); // Select offer dropdown value
		 * pageObj.campaignspage().removeSearchedCampaign(postRedemptionCampaignName1);
		 * pageObj.campaignspage().removeSearchedCampaign(postRedemptionCampaignName2);
		 * pageObj.campaignspage().removeSearchedCampaign(postRedemptionCampaignName3);
		 */
		// pageObj.utils().deleteCampaignFromDb(campaignName, env);
		// pageObj.utils().deleteCampaignFromDb(campaignName2, env);
		// pageObj.utils().deleteCampaignFromDb(campaignName3, env);
	}

	@DataProvider(name = "TestDataProvider")
	public Object[][] testDataProvider() {

		return new Object[][] {

				{ "Enable Bulking For Post Redemption Campaign", "uncheck" },

				{ "Enable Bulking For Post Redemption Campaign", "check" }

		};
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() throws Exception {
		pageObj.utils().clearDataSet(dataSet);
		pageObj.utils().deleteCampaignFromDb(campaignName, env);
		pageObj.utils().deleteCampaignFromDb(campaignName2, env);
		pageObj.utils().deleteCampaignFromDb(campaignName3, env);
		pageObj.utils().deleteRedeemableByName(redeemableName2, env);
		logger.info("Data set cleared");
	}

	@AfterClass(alwaysRun = true)
	public void closeBrowser() {
		driver.quit();
		logger.info("Browser closed");
	}
}
