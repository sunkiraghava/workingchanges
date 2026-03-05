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
public class ReferralCampaignDisableFlagTest {

	private static Logger logger = LogManager.getLogger(ReferralCampaignDisableFlagTest.class);
	public WebDriver driver;
	private Properties prop;
	private PageObj pageObj;
	private String sTCName;
	private String env;
	private String baseUrl;
	private String userEmail;
	private String referralCampaignName;
	private static Map<String, String> dataSet;
	String run = "ui";
	Utilities utils;

	@BeforeClass(alwaysRun = true)
	public void openBrowser() {
		driver = new BrowserUtilities().launchBrowser();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		// single Login to instance
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		pageObj.instanceDashboardPage().instanceLogin(baseUrl);
		utils = new Utilities(driver);
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
	}

	// Shubham
	@Test(description = "SQ-T6477 Verify gifting from referral campaign when business has date less than the UTC date", priority = 0)
	@Owner(name = "Shubham Gupta")
	public void T6477_verifyGiftingAndRewardEndDate() throws Exception {

		String b_id = dataSet.get("business_id");

		String businessPreferenceLiveQuery = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ b_id + "'";
		String businessPreferenceLiveExpColValue = DBUtils.executeQueryAndGetColumnValue(env,
				businessPreferenceLiveQuery, "preferences");

		boolean businessLiveFlag = DBUtils.updateBusinessesPreference(env, businessPreferenceLiveExpColValue, "true",
				"enable_new_sidenav_header_and_footer", b_id);
		Assert.assertTrue(businessLiveFlag, "enable_new_sidenav_header_and_footer value is not updated to true");
		utils.logit("enable_new_sidenav_header_and_footer value is updated to true");

		Response response, signUpResponse;
		String referralCode, token, referralUserEmail, campaignName, rewardGiftedAccountHistory;
		referralCampaignName = CreateDateTime.getUniqueString("Automation ReferralCampaign");

		String timezone = pageObj.signupcampaignpage().getTimezoneGreaterOrLessThanUTC();

		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().dashboardPageMiscellaneousConfig();
		pageObj.cockpitDashboardMiscPage().setBusinessTimezone("New Delhi ( IST )");

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select ongoing dropdown value
		pageObj.campaignspage().selectOngoingdrpValue(dataSet.get("onGoingDrp"));
		pageObj.campaignspage().clickNewCampaignBtn();
		// create referral campaign with derived reward
		pageObj.signupcampaignpage().setCampaignName(referralCampaignName);
		pageObj.signupcampaignpage().setGiftType(dataSet.get("giftType"));
		pageObj.signupcampaignpage().setGiftReason(referralCampaignName);
		pageObj.signupcampaignpage().setRedeemable(dataSet.get("redeemable"));
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().clickNextBtn();
		pageObj.signupcampaignpage().setStartDate();
		pageObj.signupcampaignpage().setEndDateforCouponCampaign(0);
		pageObj.signupcampaignpage().createWhenDetailsCampaign();

		userEmail = pageObj.iframeSingUpPage().generateEmail();
		signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"), dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		token = signUpResponse.jsonPath().get("access_token.token").toString();
		referralCode = signUpResponse.jsonPath().get("user.referral_code").toString();
		logger.info("Referral code is " + referralCode);
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("API2 Signup is successful");

		referralUserEmail = pageObj.iframeSingUpPage().generateEmail();
		signUpResponse = pageObj.endpoints().Api2SignUp(referralUserEmail, dataSet.get("client"), dataSet.get("secret"),
				referralCode);
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("API2 Signup is successful");

		// Timeline validation
		pageObj.instanceDashboardPage().navigateToGuestTimeline(referralUserEmail);
		pageObj.guestTimelinePage().verifyGuestTimeline(referralUserEmail);
		pageObj.guestTimelinePage().verifyReferralCodeInReferredUserTimeline(referralCode);

		// POS Checkin
		response = pageObj.endpoints().posCheckin(referralUserEmail, dataSet.get("locationkey"));
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for POS Checkin");

		// SideKiq scheduled Jobs running
		pageObj.schedulespage().runSidekiqJob(baseUrl, "AutomaticReferralWorker");
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().verifyGuestTimeline(userEmail);

		// validate gifted redeemable through Api
		campaignName = pageObj.guestTimelinePage().getcampaignNameWithWait(referralCampaignName);
		Assert.assertEquals(campaignName, referralCampaignName,
				"Referral campaign did not trigger for user : " + userEmail);
		rewardGiftedAccountHistory = pageObj.guestTimelinePage().getUserAccountHistory(referralCampaignName,
				dataSet.get("client"), dataSet.get("secret"), token);
		Assert.assertTrue(rewardGiftedAccountHistory.contains(referralCampaignName),
				"Gifted redeemable did not appeared in account history");

		pageObj.utils().updateBusinessTimezone(dataSet.get("slug"), timezone, env);

		userEmail = pageObj.iframeSingUpPage().generateEmail();
		signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"), dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		token = signUpResponse.jsonPath().get("access_token.token").toString();
		referralCode = signUpResponse.jsonPath().get("user.referral_code").toString();
		logger.info("Referral code is " + referralCode);
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("API2 Signup is successful");

		// new user signup using iframe with referral code
		referralUserEmail = pageObj.iframeSingUpPage().generateEmail();
		signUpResponse = pageObj.endpoints().Api2SignUp(referralUserEmail, dataSet.get("client"), dataSet.get("secret"),
				referralCode);
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("API2 Signup is successful");

		// Timeline validation
		pageObj.instanceDashboardPage().navigateToGuestTimeline(referralUserEmail);
		pageObj.guestTimelinePage().verifyGuestTimeline(referralUserEmail);
		pageObj.guestTimelinePage().verifyReferralCodeInReferredUserTimeline(referralCode);

		// POS Checkin
		response = pageObj.endpoints().posCheckin(referralUserEmail, dataSet.get("locationkey"));
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for POS Checkin");

		// SideKiq scheduled Jobs running
		pageObj.schedulespage().runSidekiqJob(baseUrl, "AutomaticReferralWorker");
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().verifyGuestTimeline(userEmail);

		campaignName = pageObj.guestTimelinePage().getcampaignNameMasscampaignShortPool(referralCampaignName);
		Assert.assertFalse(campaignName.equalsIgnoreCase(referralCampaignName),
				"Referral campaign did not trigger for user : " + userEmail);
		utils.logPass("Verfied user didn't get the gifting Second time which is the expected behaviour");
		// pageObj.utils().deleteCampaignFromDb(referralCampaignName, env);

	}

	// Rakhi
	@Test(description = "SQ-T5976 Verify gifting from referral campaign when Referral wait for replica is disabled and campaign has non-replica segment in derived reward global flag off"
			+ "SQ-T5978 Verify gifting from referral campaign when Referral wait for replica is disabled and campaign has replica segment in derived reward", groups = {"nonNightly"}, dataProvider = "TestDataProvider", priority = 1)
	@Owner(name = "Rakhi Rawat")
	public void T5976_VerigyGiftingFromReferralCampaignWhenFlagDisabled(String dynamicReward, String inviteCode,
			String userEmail, String reward) throws Exception {

		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);

		// verify that "Referral wait for replica" is enabled in global configuration
		pageObj.menupage().navigateToSubMenuItem("Settings", "Global Configuration");
		pageObj.dashboardpage().globalConfigFlagCheckedUnchecked("Referral wait for replica", "uncheck");
		pageObj.dashboardpage().navigateToAllBusinessPage();

		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		referralCampaignName = CreateDateTime.getUniqueString("Automation ReferralCampaign");
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select ongoing dropdown value
		pageObj.campaignspage().selectOngoingdrpValue(dataSet.get("onGoingDrp"));
		pageObj.campaignspage().clickNewCampaignBtn();
		// create referral campaign with derived reward
		pageObj.signupcampaignpage().setCampaignName(referralCampaignName);
		pageObj.signupcampaignpage().setGiftType(dataSet.get("giftType"));
		pageObj.signupcampaignpage().setGiftReason(referralCampaignName);
		pageObj.signupcampaignpage().setDerivedReward(dynamicReward);
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().createWhomDetailsReferralCampaign(referralCampaignName,
				dataSet.get("emailSubject"), dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().createWhenDetailsCampaign1();

		String referralUserEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(referralUserEmail, dataSet.get("client"),
				dataSet.get("secret"), inviteCode);
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("API2 Signup is successful");

		// Timeline validation
		pageObj.instanceDashboardPage().navigateToGuestTimeline(referralUserEmail);
		pageObj.guestTimelinePage().verifyGuestTimeline(referralUserEmail);
		pageObj.guestTimelinePage().verifyReferralCodeInReferredUserTimeline(inviteCode);

		// POS Checkin
		Response response2 = pageObj.endpoints().posCheckin(referralUserEmail, dataSet.get("locationkey"));
		Assert.assertEquals(response2.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for POS Checkin");

		// SideKiq scheduled Jobs running
		pageObj.schedulespage().runSidekiqJob(baseUrl, "AutomaticReferralWorker");
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().verifyGuestTimeline(userEmail);

		// validate gifted derived reward through Api
		String campName1 = pageObj.guestTimelinePage().getcampaignNameWithWait(referralCampaignName);
		Assert.assertEquals(campName1, referralCampaignName,
				"Referral campaign did not trigger for user : " + userEmail);
		pageObj.guestTimelinePage().navigateToTabs("Account History");
		String Itemdata = pageObj.accounthistoryPage().getAccountHistorydetailsForAnyevent(referralCampaignName);
		Assert.assertTrue(Itemdata.contains(reward + " (" + referralCampaignName + ")"),
				"Gifted points by campaign did not appeared in account history");
		utils.logPass("Gifted reward validated successfully in account history");

//				String campaignName1 = pageObj.guestTimelinePage().getCampaignNameByNotificationsAPIShortPoll(
//						referralCampaignName, dataSet.get("client"), dataSet.get("secret"), token);
//				String rewardGiftedAccountHistory = pageObj.guestTimelinePage().getUserAccountHistory(referralCampaignName,
//						dataSet.get("client"), dataSet.get("secret"), token);
//			Assert.assertTrue(campaignName1.equalsIgnoreCase(referralCampaignName),
//						"Referral campaign did not trigger for user : " + userEmail);
//				Assert.assertTrue(rewardGiftedAccountHistory.contains(expectedItemGifted_Message),
//						"User did not get reward from referral campaign");

	}

	@DataProvider(name = "TestDataProvider")
	public Object[][] testDataProvider() {

		return new Object[][] {

				{ "DND_AutomationDerivedReward", "Tesgt282534", "autoiframe11510717042024uxcsgt@punchh.com",
						"$2.0 OFF" },

				{ "DND_DynamicRewardT435", "Tesgt282534", "autoiframe11510717042024uxcsgt@punchh.com",
						"Silver $10 Off" } };
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() throws Exception {
		pageObj.utils().clearDataSet(dataSet);
		pageObj.utils().deleteCampaignFromDb(referralCampaignName, env);
		logger.info("Data set cleared");
	}

	@AfterClass(alwaysRun = true)
	public void closeBrowser() {
		try {
			pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
			pageObj.menupage().navigateToSubMenuItem("Settings", "Global Configuration");
			pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboard("Referral wait for replica", "check");
			logger.info("Referral wait for replica flag checked after tests");
		} catch (Exception e) {
			logger.error("Failed during navigation/flag update in tearDown: ", e);
		} finally {
			driver.quit();
			logger.info("Browser closed");
		}
	}

}