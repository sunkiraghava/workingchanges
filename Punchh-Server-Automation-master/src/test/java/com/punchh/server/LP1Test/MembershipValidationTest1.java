package com.punchh.server.LP1Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
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
import com.punchh.server.utilities.NewMenu;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class MembershipValidationTest1 {

	private static Logger logger = LogManager.getLogger(MembershipValidationTest1.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String sTCName;
	private String env;
	private String baseUrl;
	private Utilities utils;
	private static Map<String, String> dataSet;
	String run = "ui";
	String amount = "65.0";

	@BeforeClass(alwaysRun = true)
	public void openBrowser() {
		driver = new BrowserUtilities().launchBrowser();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		// Single login to instance
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
		utils = new Utilities(driver);
		// Move to All Businesses page
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
	}

	@Test(description = "SQ-T2198, Membership Level; "
			+ "SQ-T4529 Verify annual report and guest stats option is not accessible from settings; "
			+ "SQ-T4530 Verify annual report and guest stats option is showing on settings -> membership levels page", groups = "regression")
	@Owner(name = "Rakhi Rawat")
	public void T2198_verifyMembership() throws InterruptedException {
		// singup user
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		// Login to instance
		// pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		// pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// search User
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().verifyGuestTimeline(userEmail);
		pageObj.guestTimelinePage().verifyBronzeMembership();
		// user checkin
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationKey"), amount);
		pageObj.apiUtils().verifyResponse(checkinResponse, "POS checkin");
		pageObj.guestTimelinePage().refreshTimeline();
		pageObj.guestTimelinePage().verifySilverMembership();
		// SQ-T4529 & SQ-T4530
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Has Membership Levels?", "uncheck");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Membership Level Bump on Edge?", "uncheck");
		pageObj.dashboardpage().clickOnUpdateButton();

		List<String> customSubMenusList = pageObj.menupage().subMenuItems(NewMenu.menu_LoyaltyProgram);
		pageObj.menupage().pinSidenavMenu();
		Assert.assertFalse(customSubMenusList.contains("Membership Tiers"));
		utils.logit("pass", "Membership sub menu is not visible");

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Has Membership Levels?", "check");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Membership Level Bump on Edge?", "check");
		pageObj.dashboardpage().clickOnUpdateButton();

		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		pageObj.menupage().guestStatsAndAnnualReport("Annual Report");
		pageObj.dashboardpage().refreshPageOn502or504();
		String annualReportText = pageObj.iframeConfigurationPage().getElementText("settingsPage.annualReportText", "");
		Assert.assertTrue(annualReportText.contains("Membership Levels: Annual Report"));
		utils.logit("pass", "Annual Report is visible");

		utils.navigateBackPage();
		pageObj.menupage().guestStatsAndAnnualReport("Guest Stats");
		String guestStatsText = pageObj.iframeConfigurationPage().getElementText("settingsPage.guestStatsText", "");
		Assert.assertEquals(guestStatsText, "Membership Level Stats");
		utils.logit("pass", "Guest Stats is visible");
	}

	// shaleen
	@Test(description = "SQ-T4717 Verify that Membership Tier is only assigned when guests sign up for loyalty (fresh sign up / eClub →Loyalty) ||"
			+ "SQ-T4716 (1.0) Verify that eClub guests are not assigned a Membership Tier before creating a loyalty account.")
	@Owner(name = "Shaleen Gupta")
	public void T4717_verifyMembershipForEclubToLoyalty() throws Exception {

		// eclub upload via email
		Boolean flag = false;
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response eclubUploadResponse = pageObj.endpoints().dashboardApiEClubUpload(userEmail, dataSet.get("adminToken"),
				dataSet.get("storeNumber"), flag);
		pageObj.apiUtils().verifyResponse(eclubUploadResponse, "eclubUpload");

		// open business
		// pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		// pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// verify eclub user created on eclub page
		// pageObj.menupage().clickGuestMenu();
		// pageObj.menupage().eclubUsersLink();
		pageObj.menupage().navigateToSubMenuItem("Guests", "eClub Guests");
		pageObj.EClubGuestPage().verifyGuestuploadInEclub(dataSet.get("storeNumber"), userEmail);

		// verify membership on timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().verifyGuestTimeline(userEmail);
		pageObj.guestTimelinePage().verifyEclubUser(userEmail);
		pageObj.guestTimelinePage().verifyGuestmembershipLabelPresence();

		// create check-in before Sign-up as loyalty member [ pos transaction ]
//		Response checkinResponse2 = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationKey"), amount);
//		pageObj.apiUtils().verifyResponse(checkinResponse2, "POS checkin");
		String key = CreateDateTime.getTimeDateString();
		String txn_no = "123456" + CreateDateTime.getTimeDateString();
		Response checkinResponse2 = pageObj.endpoints().ecrmPosCheckin(userEmail, dataSet.get("locationKey"), key,
				txn_no);
		Assert.assertEquals(checkinResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "ECRM checkin failure");
		logger.info("ECRM checkin is successfull");
		utils.logPass("ECRM checkin is successfull");
		pageObj.guestTimelinePage().refreshTimeline();
		pageObj.guestTimelinePage().verifyGuestmembershipLabelPresence();

		// get user-id for eclub user
		String query3 = "Select id from users where email = '" + userEmail + "'";
		pageObj.singletonDBUtilsObj();
		String eClubUserId = DBUtils.executeQueryAndGetColumnValue(env, query3, "id");

		// verify in DB before Sign-up as loyalty member
		String query4 = "Select membership_level from accounts where user_id = '" + eClubUserId + "'";
		pageObj.singletonDBUtilsObj();
		String colValue4 = DBUtils.executeQueryAndGetColumnValue(env, query4, "membership_level");
		Assert.assertNull(colValue4, "Entry in Accounts table in Database is not NULL");
		logger.info("Verified membership level Column is NULL in Accounts table in Database");
		utils.logPass("Verified membership level Column is NULL in Accounts table in Database");

		// --sign-up as loyalty member-- //
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api1 signup");
		logger.info("Verified user signup via API v1");
		utils.logPass("Verified user signup via API v1");
		String user_id = Integer.toString(signUpResponse.jsonPath().get("user_id"));

		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().getGuestJoiningChannelEclub();

		// create check-in after Sign-up as loyalty member
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationKey"), amount);
		pageObj.apiUtils().verifyResponse(checkinResponse, "POS checkin");
		pageObj.guestTimelinePage().refreshTimeline();
		pageObj.guestTimelinePage().verifyGuestmembershipLabelPresence();

		// verify in DB after Sign-up as loyalty member
		String query1 = "Select membership_level from accounts where user_id = '" + user_id + "'";
		pageObj.singletonDBUtilsObj();
		String colValue1 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query1, "membership_level",
				4);
		Assert.assertNotNull(colValue1, "Entry in Accounts table in Database not successfull");
		logger.info("Verified membership level Column in Accounts table in Database");
		utils.logPass("Verified membership level Column in Accounts table in Database");

		String query2 = "Select created_at from membership_level_histories where user_id = '" + user_id + "'";
		pageObj.singletonDBUtilsObj();
		String colValue2 = DBUtils.executeQueryAndGetColumnValue(env, query2, "created_at");
		Assert.assertNotNull(colValue2, "Entry in Membership history table in Database not successfull");
		logger.info("Verified created_at Column in Membership history table in Database");
		utils.logPass("Verified created_at Column in Membership history table in Database");

	}

	@Test(description = "SQ-T5391 Membership: Validate configuration of membership bump on edge")
	@Owner(name = "Rakhi Rawat")
	public void T5391_verifyMembershipBumpOnEdge() throws Exception {

		// open business
		// pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		// pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// memberships setting
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_has_membership_levels", "check");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_membership_level_bump_on_edge", "check");
		pageObj.cockpitGuestPage().clickUpdateBtn();

		// create user
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api1 signup");
		logger.info("Verified user signup via API v1");
		utils.logPass("Verified user signup via API v1");

		// create checkin
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"),
				dataSet.get("amount"));
		pageObj.apiUtils().verifyResponse(checkinResponse, "POS checkin");

		// open guestTimeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().verifyGuestTimeline(userEmail);
		pageObj.guestTimelinePage().refreshTimeline();

		// check membership level
		String level = pageObj.guestTimelinePage().verifyGuestmembershipLabel();
		Assert.assertEquals(level, "Silver Level", "Membership Level does not match as expected");
		logger.info("Verified membership level get bump at edge when flag is on - " + level);
		utils.logPass("Verified membership level get bump at edge when flag is on - " + level);

	}

	@Test(description = "SQ-T4522 [Points to Reward] Validate reward gifting on the basis of redemption mark set in business with membership OFF", groups = {
			"regression", "unstable", "dailyrun" })
	@Owner(name = "Rakhi Rawat")
	public void T4522_validateRewardGiftingWithMembershipOff() throws InterruptedException {
		// login to instance
		// pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		// pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// update redeemption mark
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("RedeemptionMark"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// off the memberships
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_has_membership_levels", "uncheck");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_membership_level_bump_on_edge", "uncheck");
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");
		pageObj.dashboardpage().navigateToTabs("Checkin Earning");
		pageObj.earningPage().setPointsConvertTo("Rewards");
		pageObj.earningPage().updateConfiguration();
		
		// Signup using mobile api
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
//		String token = signUpResponse.jsonPath().get("auth_token.token").toString(); 
//		String userID = signUpResponse.jsonPath().get("id").toString();

		int divide = Integer.parseInt(dataSet.get("divide"));

		// pos checkin -- 1
		Response resp1 = pageObj.endpoints().posCheckin(userEmail, dataSet.get("location_Key"), dataSet.get("amount1"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp1.getStatusCode(), "Status code 200 did not matched for post chekin api");
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		int start1 = Integer.parseInt(dataSet.get("start1"));
		int end1 = Integer.parseInt(dataSet.get("end1"));
		List<String> rangeList1 = pageObj.cockpitRedemptionsPage().divideRange(start1, end1, divide);
		int counter = 0;
		for (int i = 0; i < rangeList1.size(); i++) {
			boolean val = pageObj.guestTimelinePage().redeemablePointsRangeVisible(rangeList1.get(i),
					"Base Redeemable");
			Assert.assertTrue(val, "point range -- " + rangeList1.get(i) + " is not visible");
			logger.info("Verified point range " + rangeList1.get(i) + " is visible");
			utils.logPass("Verified point range " + rangeList1.get(i) + " is visible");
			counter++;
		}
		Assert.assertEquals(counter, Integer.parseInt(dataSet.get("expectedSize1")),
				"given rewards is not matching with the expected size -- " + dataSet.get("expectedSize1"));
		logger.info("Verified given rewards is matching with the expected size -- " + dataSet.get("expectedSize1"));
		TestListeners.extentTest.get()
				.pass("Verified given rewards is matching with the expected size -- " + dataSet.get("expectedSize1"));

		// pos checkin -- 2
		Response resp2 = pageObj.endpoints().posCheckin(userEmail, dataSet.get("location_Key"), dataSet.get("amount2"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp2.getStatusCode(), "Status code 200 did not matched for post chekin api");
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		int start2 = Integer.parseInt(dataSet.get("start2"));
		int end2 = Integer.parseInt(dataSet.get("end2"));
		List<String> rangeList2 = pageObj.cockpitRedemptionsPage().divideRange(start2, end2, divide);
		int counter2 = 0;
		for (int i = 0; i < rangeList2.size(); i++) {
			boolean val = pageObj.guestTimelinePage().redeemablePointsRangeVisible(rangeList2.get(i),
					"Base Redeemable");
			Assert.assertTrue(val, "point range -- " + rangeList2.get(i) + " is not visible");
			logger.info("Verified point range " + rangeList2.get(i) + " is visible");
			utils.logPass("Verified point range " + rangeList2.get(i) + " is visible");
			counter2++;
		}
		Assert.assertEquals(counter2, Integer.parseInt(dataSet.get("expectedSize2")),
				"given rewards is not matching with the expected size -- " + dataSet.get("expectedSize2"));
		logger.info("Verified given rewards is matching with the expected size -- " + dataSet.get("expectedSize1"));
		TestListeners.extentTest.get()
				.pass("Verified given rewards is matching with the expected size -- " + dataSet.get("expectedSize2"));

		// pos checkin -- 3
		Response resp3 = pageObj.endpoints().posCheckin(userEmail, dataSet.get("location_Key"), dataSet.get("amount3"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp3.getStatusCode(), "Status code 200 did not matched for post chekin api");
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		int start3 = Integer.parseInt(dataSet.get("start3"));
		int end3 = Integer.parseInt(dataSet.get("end3"));
		List<String> rangeList3 = pageObj.cockpitRedemptionsPage().divideRange(start3, end3, divide);
		int counter3 = 0;
		for (int i = 0; i < rangeList3.size(); i++) {
			boolean val = pageObj.guestTimelinePage().redeemablePointsRangeVisible(rangeList3.get(i),
					"Base Redeemable");
			Assert.assertTrue(val, "point range -- " + rangeList3.get(i) + " is not visible");
			logger.info("Verified point range " + rangeList3.get(i) + " is visible");
			utils.logPass("Verified point range " + rangeList3.get(i) + " is visible");
			counter3++;
		}
		Assert.assertEquals(counter3, Integer.parseInt(dataSet.get("expectedSize3")),
				"given rewards is not matching with the expected size -- " + dataSet.get("expectedSize3"));
		logger.info("Verified given rewards is matching with the expected size -- " + dataSet.get("expectedSize3"));
		TestListeners.extentTest.get()
				.pass("Verified given rewards is matching with the expected size -- " + dataSet.get("expectedSize3"));

		// On the memberships
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_has_membership_levels", "check");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_membership_level_bump_on_edge", "check");
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		logger.info("Data set cleared");
	}

	@AfterClass(alwaysRun = true)
	public void closeBrowser() {
		driver.quit();
		logger.info("Browser closed");
	}

}
