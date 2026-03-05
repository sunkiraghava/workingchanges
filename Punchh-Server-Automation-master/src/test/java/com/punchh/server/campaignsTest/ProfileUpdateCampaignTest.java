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
public class ProfileUpdateCampaignTest {
	private static Logger logger = LogManager.getLogger(ProfileUpdateCampaignTest.class);
	public WebDriver driver;
	private Properties prop;
	private PageObj pageObj;
	private String userEmail;
	private String sTCName;
	private String env;
	private String baseUrl;
	private static Map<String, String> dataSet;
	private String run = "ui";
	private String profileupdateCampaignName, profileupdateCampaignName2, profileupdateCampaignName3;
	private Utilities utils;

	@BeforeClass(alwaysRun = true)
	public void openBrowser() {
		prop = Utilities.loadPropertiesFile("config.properties");
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
		prop = Utilities.loadPropertiesFile("config.properties");
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

	@Test(description = "SQ-T2550 Verify Profile_Update_Campaign", groups = { "regression", "dailyrun" }, priority = 0)
	@Owner(name = "Amit Kumar")
	public void T2550_verifyProfileUpdateCampaign() throws Exception {

		profileupdateCampaignName = "Automation Profileupdate Campaign" + CreateDateTime.getTimeDateString();

		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value

		pageObj.campaignspage().selectOngoingdrpValue("Profile Update");
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().setCampaignName(profileupdateCampaignName);
		pageObj.signupcampaignpage().setGiftType("Gift Redeemable");
		pageObj.signupcampaignpage().setGiftReason(profileupdateCampaignName);
		pageObj.signupcampaignpage().setRedeemable("$5 Off");
		pageObj.signupcampaignpage().clickNextBtn();
		// campaignid = pageObj.signupcampaignpage().getCampaignid();
		pageObj.signupcampaignpage().setUserFileds("Gender");
		pageObj.signupcampaignpage().setPushNotification(profileupdateCampaignName);
		pageObj.signupcampaignpage().setEmailSubject(dataSet.get("emailSubject"));
		pageObj.signupcampaignpage().setEmailTemplate(dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().setStartDate();
		pageObj.signupcampaignpage().createWhenDetailsCampaign();

		// User register/signup using API2 Signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");

		// Update user profile
		Response updateUserInfoResponse = pageObj.endpoints().Api2UpdateUserProfile(dataSet.get("client"), userEmail,
				dataSet.get("secret"), token);
		Assert.assertEquals(200, updateUserInfoResponse.getStatusCode(),
				"Status code 200 did not matched for api2 update user info");

		// Timeline validation
		/*
		 * pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail); String
		 * campaignName = pageObj.guestTimelinePage().getcampaignNameMasscampaign(
		 * profileupdateCampaignName); boolean campaignNotificationStatus =
		 * pageObj.guestTimelinePage().verifyCampaignNotification();
		 */

		String campaignName = pageObj.guestTimelinePage().getCampaignNameByNotificationsAPI(profileupdateCampaignName,
				dataSet.get("client"), dataSet.get("secret"), token);
		String rewardGiftedAccountHistory = pageObj.guestTimelinePage().getUserAccountHistory(profileupdateCampaignName,
				dataSet.get("client"), dataSet.get("secret"), token);

		Assert.assertTrue(campaignName.equalsIgnoreCase(profileupdateCampaignName), "Campaign name did not matched");
		Assert.assertTrue(
				rewardGiftedAccountHistory.contains("You were gifted: $5 Off (" + profileupdateCampaignName + ")"),
				"Gifted points did not appeared in account history");
		TestListeners.extentTest.get().pass(
				"Profile update campaign detail: push notification, campaign name, validated successfully on timeline");

		// Delete created campaign
		/*
		 * pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns"); // Select
		 * offer dropdown value
		 * pageObj.campaignspage().selectOngoingdrpValue("Profile Update");
		 * pageObj.campaignspage().removeSearchedCampaign(profileupdateCampaignName);
		 */
		// pageObj.utils().deleteCampaignByIdCampaignsTable(campaignid,env);
	}

	// Anant
	@Test(description = "SQ-T310 Derived Gift reward is not showing in cpp page of profile update campaign"
			+ "SQ-T298 Getting 500 error in Campaigns page when we filter sort by type is STATUS in Pre-prod and local instance", groups = {
					"regression", "dailyrun","nonNightly" }, priority = 1)
	@Owner(name = "Rakhi Rawat")
	public void T310_derivedRewardInProfileUpdateCampaign() throws Exception {
		profileupdateCampaignName = "Automation Profileupdate Campaign" + CreateDateTime.getTimeDateString();
		String derivedReward = dataSet.get("derivedReward");

		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.campaignspage().selectOngoingdrpValue("Profile Update");
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().setCampaignName(profileupdateCampaignName);
		pageObj.signupcampaignpage().setGiftType("Gift Dynamic Rewards");
		pageObj.signupcampaignpage().setGiftReason("Test " + profileupdateCampaignName);
		pageObj.signupcampaignpage().setDerivedReward(derivedReward);
		pageObj.signupcampaignpage().clickNextBtn();
		// campaignid = pageObj.signupcampaignpage().getCampaignid();
		// userField
		pageObj.signupcampaignpage().setUserFileds(dataSet.get("userField"));
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().createWhenDetailsCampaign();

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		String activeStatus = pageObj.campaignspage().getActiveCampaign();
		Assert.assertEquals(activeStatus, "Active", "after sorting the active campaigns are not displaying");
		utils.logPass("after sorting the active campaigns are displaying");

		pageObj.campaignspage().searchAndSelectCamapign(profileupdateCampaignName);
		String derivedRewardVal = pageObj.campaignspage().checkDerivedRewardCampaignSummary(derivedReward);
		Assert.assertEquals(derivedRewardVal,derivedReward, 
				"derived reward is not visible on the campaign summary page");
		utils.logPass("derived reward is not visible on the campaign summary page");

		// pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// pageObj.campaignspage().classicCampaignPageDeleteCampaign();
		// pageObj.campaignspage().deleteCampaign(CampaignName);
		// pageObj.utils().deleteCampaignByIdCampaignsTable(campaignid,env);

	}

	// shaleen
	@Test(description = "SQ-T4330 (1.0) Derived Gift reward is not showing in cpp page of profile update campaign", groups = {
			"regression", "dailyrun", "nonNightly" }, priority = 2)
	@Owner(name = "Shaleen Gupta")
	public void T4330_verifyDerivedRewardInCPP() throws Exception {

		profileupdateCampaignName = "AutomationProfileUpdate_T310" + CreateDateTime.getTimeDateString();
		// open business
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// create profile update campaign
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.campaignspage().selectOngoingdrpValue("Profile Update");
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().setCampaignName(profileupdateCampaignName);
		pageObj.signupcampaignpage().setGiftType(dataSet.get("giftType"));
		pageObj.signupcampaignpage().setGiftReason("Testcpp");
		pageObj.signupcampaignpage().setDerivedReward(dataSet.get("derivedReward"));
		pageObj.signupcampaignpage().clickNextBtn();
		// campaignid = pageObj.signupcampaignpage().getCampaignid();
		pageObj.signupcampaignpage().setUserFileds(dataSet.get("userField"));
		pageObj.signupcampaignpage().setPushNotification(dataSet.get("pushNotification"));
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().createWhenDetailsCampaign();

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.campaignspage().searchAndSelectCamapign(profileupdateCampaignName);
		String reward = pageObj.campaignspage().checkDerivedRewardCampaignSummary(dataSet.get("derivedReward"));
		Assert.assertEquals(reward, dataSet.get("derivedReward"), "Derived reward is not showing in CPP");
		utils.logPass("Derived reward is showing in CPP of profile update campaign");

		// delete campaign
		/*
		 * pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		 * pageObj.campaignspage().removeSearchedCampaign(campName);
		 */
		// pageObj.utils().deleteCampaignByIdCampaignsTable(campaignid,env);
	}

	// Shubham Kumar Gupta
	@Test(description = "SQ-T6308 Verify gifting from profile update campaign when business has date 9 Jun and in UTC date is 10 June", groups = {
			"regression", "dailyrun","nonNightly" }, priority = 3)
	@Owner(name = "Shubham Gupta")
	public void T6308_verifyGiftingAndRewardEndDate() throws Exception {

		String b_id = dataSet.get("business_id");

		String businessPreferenceLiveQuery = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ b_id + "'";
		String businessPreferenceLiveExpColValue = DBUtils.executeQueryAndGetColumnValue(env,
				businessPreferenceLiveQuery, "preferences");

		boolean businessLiveFlag = DBUtils.updateBusinessesPreference(env, businessPreferenceLiveExpColValue, "true",
				"enable_new_sidenav_header_and_footer", b_id);
		Assert.assertTrue(businessLiveFlag, "enable_new_sidenav_header_and_footer value is not updated to true");
		utils.logit("enable_new_sidenav_header_and_footer value is updated to true");

		String timezone = pageObj.signupcampaignpage().getTimezoneGreaterOrLessThanUTC();
		profileupdateCampaignName = "Automation Profileupdate Campaign" + CreateDateTime.getTimeDateString();
		String redeemableName = dataSet.get("redeemableName");

		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().dashboardPageMiscellaneousConfig();
		pageObj.cockpitDashboardMiscPage().setBusinessTimezone("New Delhi ( IST )"); // Asia/Kolkata;

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value

		pageObj.campaignspage().selectOngoingdrpValue("Profile Update");
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().setCampaignName(profileupdateCampaignName);
		pageObj.signupcampaignpage().setGiftType("Gift Redeemable");
		pageObj.signupcampaignpage().setGiftReason(profileupdateCampaignName);
		pageObj.signupcampaignpage().setRedeemable(dataSet.get("redeemableName"));
		pageObj.signupcampaignpage().clickNextBtn();
		// campaignid = pageObj.signupcampaignpage().getCampaignid();
		pageObj.signupcampaignpage().setUserFileds("Gender");
		pageObj.signupcampaignpage().setPushNotification(profileupdateCampaignName);
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().setStartDate();
		pageObj.signupcampaignpage().setEndDateforCouponCampaign(0);

		pageObj.signupcampaignpage().createWhenDetailsCampaign();

		// User register/signup using API2 Signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");

		// Update user profile
		Response updateUserInfoResponse = pageObj.endpoints().Api2UpdateUserProfile(dataSet.get("client"), userEmail,
				dataSet.get("secret"), token);
		Assert.assertEquals(200, updateUserInfoResponse.getStatusCode(),
				"Status code 200 did not matched for api2 update user info");

		String campaignName = pageObj.guestTimelinePage().getCampaignNameByNotificationsAPI(profileupdateCampaignName,
				dataSet.get("client"), dataSet.get("secret"), token);
		String rewardGiftedAccountHistory = pageObj.guestTimelinePage().getUserAccountHistory(profileupdateCampaignName,
				dataSet.get("client"), dataSet.get("secret"), token);
		String expectedResult = "You were gifted: " + redeemableName + " (" + profileupdateCampaignName + ")";
		Assert.assertTrue(campaignName.equalsIgnoreCase(profileupdateCampaignName), "Campaign name did not matched");
		Assert.assertTrue(rewardGiftedAccountHistory.contains(expectedResult),
				"Gifted rewards did not appeared in account history");

		pageObj.utils().updateBusinessTimezone(dataSet.get("slug"), timezone, env);

		// Wait for 5 sec for caching
		Thread.sleep(5000);

		// User register/signup using API2 Signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse1 = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		String token1 = signUpResponse1.jsonPath().get("access_token.token").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");

		// Update user profile
		Response updateUserInfoResponse1 = pageObj.endpoints().Api2UpdateUserProfile(dataSet.get("client"), userEmail,
				dataSet.get("secret"), token1);
		Assert.assertEquals(200, updateUserInfoResponse1.getStatusCode(),
				"Status code 200 did not matched for api2 update user info");

		String campaignName1 = pageObj.guestTimelinePage().getCampaignNameByNotificationsAPI(profileupdateCampaignName,
				dataSet.get("client"), dataSet.get("secret"), token1);
		String rewardGiftedAccountHistory1 = pageObj.guestTimelinePage()
				.getUserAccountHistory(profileupdateCampaignName, dataSet.get("client"), dataSet.get("secret"), token1);
		String expectedResult1 = "You were gifted: " + redeemableName + " (" + profileupdateCampaignName + ")";
		Assert.assertFalse(campaignName1.equalsIgnoreCase(profileupdateCampaignName), "Campaign name matched");
		Assert.assertFalse(rewardGiftedAccountHistory1.contains(expectedResult1),
				"Gifted rewards appeared in account history");
		TestListeners.extentTest.get().pass(
				"Profile update campaign detail: push notification, campaign name, validated successfully on timeline");

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() throws Exception {
		pageObj.utils().clearDataSet(dataSet);
		pageObj.utils().deleteCampaignFromDb(profileupdateCampaignName, env);
		pageObj.utils().deleteCampaignFromDb(profileupdateCampaignName2, env);
		pageObj.utils().deleteCampaignFromDb(profileupdateCampaignName3, env);
		logger.info("Data set cleared");
	}

	@AfterClass(alwaysRun = true)
	public void closeBrowser() {
		driver.quit();
		logger.info("Browser closed");
	}
}
