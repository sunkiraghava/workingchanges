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
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class MembershipValidationTest2 {

	private static Logger logger = LogManager.getLogger(MembershipValidationTest2.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String sTCName;
	private String env;
	private String baseUrl;
	private String userEmail;
	private static Map<String, String> dataSet;
	private Utilities utils;
	String run = "ui";
	String amount = "65.0";

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
		logger.info(sTCName + " ==>" + dataSet);
		utils = new Utilities(driver);
		// Instance move to all business page
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
	}

	@Test(description = "SQ-T5395 [Points to Rewards] Validate banked redeemable gifting from membership configuration", groups = {
			"regression", "unstable" }, priority = 0)
	@Owner(name = "Shaleen Gupta")
	public void T5395_verifyBankedRedeemableGiftingFromMembership() throws Exception {

		// open instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// memberships setting
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_has_membership_levels", "check");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_membership_level_bump_on_edge", "uncheck");
		pageObj.cockpitGuestPage().clickUpdateBtn();

		// edit membership
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		for (int i = 1; i <= 3; i++) {
			String membershipMinPoints = dataSet.get("membership" + i + "MinPoints");
			String membershipMaxPoints = dataSet.get("membership" + i + "MaxPoints");
			pageObj.settingsPage().clickMemberLevel(dataSet.get("membership" + i));
			pageObj.settingsPage().editMembershipLevelMinMaxPoints(membershipMinPoints, membershipMaxPoints);

			pageObj.settingsPage().editRedeemptionMarkFromMembership(dataSet.get("redemptionMark"));
			pageObj.settingsPage().setMembershipLevelAttainmentReward("Base Redeemable");
			pageObj.settingsPage().setBankedRedeemable(dataSet.get("bankedRedeemable" + i));
			pageObj.settingsPage().clickUpdateMembership();
			logger.info(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
			TestListeners.extentTest.get()
					.info(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
		}

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

		String level = pageObj.guestTimelinePage().verifyGuestmembershipLabel();
		Assert.assertEquals(level, "Silver Level", "Membership Level does not match as expected");
		logger.info("Verified membership level bump to : " + level);
		utils.logPass("Verified membership level bump to : " + level);

		for (int i = 1; i <= 2; i++) {
			boolean verify = pageObj.guestTimelinePage().redeemablePointsRangeVisible(dataSet.get("range" + i),
					dataSet.get("bankedRedeemable1"));
			Assert.assertTrue(verify, "Banked Redeemable gifting is not successful");
			logger.info(
					"Verified banked redeemable gifting is happening from membership configuration for Point range : "
							+ dataSet.get("range" + i));
			utils.logPass(
					"Verified banked redeemable gifting is happening from membership configuration for Point range : "
							+ dataSet.get("range" + i));
		}
		boolean verify = pageObj.guestTimelinePage().redeemablePointsRangeVisible(dataSet.get("range3"),
				dataSet.get("bankedRedeemable2"));
		Assert.assertTrue(verify, "Banked Redeemable gifting is not successful");
		logger.info("Verified banked redeemable gifting is happening from membership configuration for Point range : "
				+ dataSet.get("range3"));
		TestListeners.extentTest.get()
				.pass("Verified banked redeemable gifting is happening from membership configuration for Point range : "
						+ dataSet.get("range3"));

	}

	// shaleen
	@Test(description = "SQ-T4520 [Points to Reward] Validate reward gifting on the basis of reward redemption mark set in business.", priority = 1)
	@Owner(name = "Shaleen Gupta")
	public void T4520_validateRewardGiftingWithMembershipLevel() throws Exception {

		// open instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// on the memberships
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_has_membership_levels", "check");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_membership_level_bump_on_edge", "check");
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// update redemption mark
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("RedemptionMark"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// edit membership
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		for (int i = 1; i <= 3; i++) {
			String membershipMinPoints = dataSet.get("membership" + i + "MinPoints");
			String membershipMaxPoints = dataSet.get("membership" + i + "MaxPoints");
			pageObj.settingsPage().clickMemberLevel(dataSet.get("membership" + i));
			pageObj.settingsPage().editMembershipLevelMinMaxPoints(membershipMinPoints, membershipMaxPoints);

			pageObj.settingsPage().editRedeemptionMarkFromMembership("");
			pageObj.settingsPage().setMembershipLevelAttainmentReward("Base Redeemable");
			pageObj.settingsPage().setBankedRedeemable(dataSet.get("redeemable" + i));
			pageObj.settingsPage().clickUpdateMembership();
			logger.info(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
			TestListeners.extentTest.get()
					.info(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
		}

		// Signup using mobile api
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		// String userID = signUpResponse.jsonPath().get("id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		int divide = Integer.parseInt(dataSet.get("divide"));

		// pos checkin -- 1
		Response resp1 = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"), dataSet.get("amount1"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp1.getStatusCode(), "Status code 200 did not matched for post chekin api");
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		int start1 = Integer.parseInt(dataSet.get("start1"));
		int end1 = Integer.parseInt(dataSet.get("end1"));
		List<String> rangeList1 = pageObj.cockpitRedemptionsPage().divideRange(start1, end1, divide);
		int counter = 0;
		for (int i = 0; i < rangeList1.size(); i++) {
			boolean val = pageObj.guestTimelinePage().redeemablePointsRangeVisible(rangeList1.get(i),
					dataSet.get("redeemable1"));
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

		boolean val1 = pageObj.guestTimelinePage().labelVisible(dataSet.get("label1"));
		Assert.assertTrue(val1, "Membership level --" + dataSet.get("label1") + " not visible");
		logger.info("Verified Membership level --" + dataSet.get("label1") + " visible");
		utils.logPass("Verified Membership level --" + dataSet.get("label1") + " visible");

		// pos checkin -- 2
		Response resp2 = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"), dataSet.get("amount2"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp2.getStatusCode(), "Status code 200 did not matched for post chekin api");
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		int start2 = Integer.parseInt(dataSet.get("start2"));
		int end2 = Integer.parseInt(dataSet.get("end2"));
		List<String> rangeList2 = pageObj.cockpitRedemptionsPage().divideRange(start2, end2, divide);
		int counter2 = 0;
		for (int i = 0; i < rangeList2.size(); i++) {
			boolean val = pageObj.guestTimelinePage().redeemablePointsRangeVisible(rangeList2.get(i),
					dataSet.get("redeemable2"));
			Assert.assertTrue(val, "point range -- " + rangeList2.get(i) + " is not visible");
			logger.info("Verified point range " + rangeList2.get(i) + " is visible");
			utils.logPass("Verified point range " + rangeList2.get(i) + " is visible");
			counter2++;
		}
		Assert.assertEquals(counter2, Integer.parseInt(dataSet.get("expectedSize2")),
				"given rewards is not matching with the expected size -- " + dataSet.get("expectedSize2"));
		logger.info("Verified given rewards is matching with the expected size -- " + dataSet.get("expectedSize2"));
		TestListeners.extentTest.get()
				.pass("Verified given rewards is matching with the expected size -- " + dataSet.get("expectedSize2"));

		boolean val2 = pageObj.guestTimelinePage().labelVisible(dataSet.get("label2"));
		Assert.assertTrue(val2, "Membership level --" + dataSet.get("label2") + " not visible");
		logger.info("Verified Membership level --" + dataSet.get("label2") + " visible");
		utils.logPass("Verified Membership level --" + dataSet.get("label2") + " visible");

		// pos checkin -- 3
		Response resp3 = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"), dataSet.get("amount3"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp3.getStatusCode(), "Status code 200 did not matched for post chekin api");
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		int start3 = Integer.parseInt(dataSet.get("start3"));
		int end3 = Integer.parseInt(dataSet.get("end3"));
		List<String> rangeList3 = pageObj.cockpitRedemptionsPage().divideRange(start3, end3, divide);
		int counter3 = 0;
		for (int i = 0; i < rangeList3.size(); i++) {
			boolean val = pageObj.guestTimelinePage().redeemablePointsRangeVisible(rangeList3.get(i),
					dataSet.get("redeemable3"));
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

		boolean val3 = pageObj.guestTimelinePage().labelVisible(dataSet.get("label3"));
		Assert.assertTrue(val3, "Membership level --" + dataSet.get("label3") + " not visible");
		logger.info("Verified Membership level --" + dataSet.get("label3") + " visible");
		utils.logPass("Verified Membership level --" + dataSet.get("label3") + " visible");
	}

	// Rakhi
	@Test(description = "SQ-T5991 [Points to Reward] Validate reward gifting on the basis of reward redemption mark set in business.", priority = 2)
	@Owner(name = "Rakhi Rawat")
	public void T5991_validateRewardGiftingWithMembershipLevelRedemptionMarkOnBusiness() throws Exception {

		// open instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// on the memberships
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_has_membership_levels", "check");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_membership_level_bump_on_edge", "check");
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// update redeemption mark
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("RedemptionMark"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// edit membership
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		for (int i = 1; i <= 3; i++) {
			String membershipMinPoints = dataSet.get("membership" + i + "MinPoints");
			String membershipMaxPoints = dataSet.get("membership" + i + "MaxPoints");
			pageObj.settingsPage().clickMemberLevel(dataSet.get("membership" + i));
			pageObj.settingsPage().editRedeemptionMarkFromMembership("");
			pageObj.settingsPage().setMembershipLevelAttainmentReward("Base Redeemable");
			pageObj.settingsPage().editMembershipLevelMinMaxPoints(membershipMinPoints, membershipMaxPoints);
			pageObj.settingsPage().setBankedRedeemable(dataSet.get("redeemable" + i));
			pageObj.settingsPage().clickUpdateMembership();
			logger.info(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
			TestListeners.extentTest.get()
					.info(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
		}

		// Signup using mobile api
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		// String userID = signUpResponse.jsonPath().get("id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		int divide = Integer.parseInt(dataSet.get("divide"));

		// pos checkin -- 1
		Response resp1 = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"), dataSet.get("amount1"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp1.getStatusCode(), "Status code 200 did not matched for post chekin api");
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		int start1 = Integer.parseInt(dataSet.get("start1"));
		int end1 = Integer.parseInt(dataSet.get("end1"));
		List<String> rangeList1 = pageObj.cockpitRedemptionsPage().divideRange(start1, end1, divide);
		int counter = 0;
		for (int i = 0; i < rangeList1.size(); i++) {
			boolean val = pageObj.guestTimelinePage().redeemablePointsRangeVisible(rangeList1.get(i),
					dataSet.get("redeemable1"));
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

		boolean val1 = pageObj.guestTimelinePage().labelVisible(dataSet.get("label2"));
		Assert.assertTrue(val1, "Membership level --" + dataSet.get("label2") + " not visible");
		logger.info("Verified Membership level --" + dataSet.get("label2") + " visible");
		utils.logPass("Verified Membership level --" + dataSet.get("label2") + " visible");

		// pos checkin -- 2
		Response resp2 = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"), dataSet.get("amount2"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp2.getStatusCode(), "Status code 200 did not matched for post chekin api");
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		int start2 = Integer.parseInt(dataSet.get("start2"));
		int end2 = Integer.parseInt(dataSet.get("end2"));
		List<String> rangeList2 = pageObj.cockpitRedemptionsPage().divideRange(start2, end2, divide);
		int counter2 = 0;
		for (int i = 0; i < rangeList2.size(); i++) {
			boolean val = pageObj.guestTimelinePage().redeemablePointsRangeVisible(rangeList2.get(i),
					dataSet.get("redeemable2"));
			Assert.assertTrue(val, "point range -- " + rangeList2.get(i) + " is not visible");
			logger.info("Verified point range " + rangeList2.get(i) + " is visible");
			utils.logPass("Verified point range " + rangeList2.get(i) + " is visible");
			counter2++;
		}
		Assert.assertEquals(counter2, Integer.parseInt(dataSet.get("expectedSize2")),
				"given rewards is not matching with the expected size -- " + dataSet.get("expectedSize2"));
		logger.info("Verified given rewards is matching with the expected size -- " + dataSet.get("expectedSize2"));
		TestListeners.extentTest.get()
				.pass("Verified given rewards is matching with the expected size -- " + dataSet.get("expectedSize2"));

		boolean val2 = pageObj.guestTimelinePage().labelVisible(dataSet.get("label3"));
		Assert.assertTrue(val2, "Membership level --" + dataSet.get("label3") + " not visible");
		logger.info("Verified Membership level --" + dataSet.get("label3") + " visible");
		utils.logPass("Verified Membership level --" + dataSet.get("label3") + " visible");

		// pos checkin -- 3
		Response resp3 = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"), dataSet.get("amount3"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp3.getStatusCode(), "Status code 200 did not matched for post chekin api");
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		int start3 = Integer.parseInt(dataSet.get("start3"));
		int end3 = Integer.parseInt(dataSet.get("end3"));
		List<String> rangeList3 = pageObj.cockpitRedemptionsPage().divideRange(start3, end3, divide);
		int counter3 = 0;
		for (int i = 0; i < rangeList3.size(); i++) {
			boolean val = pageObj.guestTimelinePage().redeemablePointsRangeVisible(rangeList3.get(i),
					dataSet.get("redeemable3"));
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

		boolean val3 = pageObj.guestTimelinePage().labelVisible(dataSet.get("label3"));
		Assert.assertTrue(val3, "Membership level --" + dataSet.get("label3") + " not visible");
		logger.info("Verified Membership level --" + dataSet.get("label3") + " visible");
		utils.logPass("Verified Membership level --" + dataSet.get("label3") + " visible");
	}

	// Rakhi
	@Test(description = "SQ-T5994 [Point converts to Reward] Verify the Rewards gifting with membership level and Redemption Mark on Membership level", priority = 3)
	@Owner(name = "Rakhi Rawat")
	public void T5994_validateRewardGiftingWithMembershipLevelRedemptionMarkOnMembership() throws Exception {

		// open instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// on the memberships
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_has_membership_levels", "check");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_membership_level_bump_on_edge", "check");
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// edit membership
		// (bronze ->0-99 ) (Silver-> 100-499) (Gold -> 500 -999999)
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		for (int i = 1; i <= 3; i++) {
			String membershipMinPoints = dataSet.get("membership" + i + "MinPoints");
			String membershipMaxPoints = dataSet.get("membership" + i + "MaxPoints");
			pageObj.settingsPage().clickMemberLevel(dataSet.get("membership" + i));
			pageObj.settingsPage().editRedeemptionMarkFromMembership(dataSet.get("RedemptionMark"));
			pageObj.settingsPage().editMembershipLevelMinMaxPoints(membershipMinPoints, membershipMaxPoints);
			pageObj.settingsPage().setMembershipLevelAttainmentReward("Base Redeemable");
			pageObj.settingsPage().setBankedRedeemable(dataSet.get("redeemable" + i));
			pageObj.settingsPage().clickUpdateMembership();
			logger.info(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
			TestListeners.extentTest.get()
					.info(" This membership: " + dataSet.get("membership" + i) + " is successfully updated ");
		}

		// Signup using mobile api
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		// String userID = signUpResponse.jsonPath().get("id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		int divide = Integer.parseInt(dataSet.get("divide"));

		// pos checkin -- 1
		Response resp1 = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"), dataSet.get("amount1"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp1.getStatusCode(), "Status code 200 did not matched for post chekin api");
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		int start1 = Integer.parseInt(dataSet.get("start1"));
		int end1 = Integer.parseInt(dataSet.get("end1"));
		List<String> rangeList1 = pageObj.cockpitRedemptionsPage().divideRange(start1, end1, divide);
		int counter = 0;
		for (int i = 0; i < rangeList1.size(); i++) {
			boolean val = pageObj.guestTimelinePage().redeemablePointsRangeVisible(rangeList1.get(i),
					dataSet.get("redeemable1"));
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

		boolean val1 = pageObj.guestTimelinePage().labelVisible(dataSet.get("label2"));
		Assert.assertTrue(val1, "Membership level --" + dataSet.get("label2") + " not visible");
		logger.info("Verified Membership level --" + dataSet.get("label2") + " visible");
		utils.logPass("Verified Membership level --" + dataSet.get("label2") + " visible");

		// pos checkin -- 2
		Response resp2 = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"), dataSet.get("amount2"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp2.getStatusCode(), "Status code 200 did not matched for post chekin api");
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		int start2 = Integer.parseInt(dataSet.get("start2"));
		int end2 = Integer.parseInt(dataSet.get("end2"));
		List<String> rangeList2 = pageObj.cockpitRedemptionsPage().divideRange(start2, end2, divide);
		int counter2 = 0;
		for (int i = 0; i < rangeList2.size(); i++) {
			boolean val = pageObj.guestTimelinePage().redeemablePointsRangeVisible(rangeList2.get(i),
					dataSet.get("redeemable2"));
			Assert.assertTrue(val, "point range -- " + rangeList2.get(i) + " is not visible");
			logger.info("Verified point range " + rangeList2.get(i) + " is visible");
			utils.logPass("Verified point range " + rangeList2.get(i) + " is visible");
			counter2++;
		}
		Assert.assertEquals(counter2, Integer.parseInt(dataSet.get("expectedSize2")),
				"given rewards is not matching with the expected size -- " + dataSet.get("expectedSize2"));
		logger.info("Verified given rewards is matching with the expected size -- " + dataSet.get("expectedSize2"));
		TestListeners.extentTest.get()
				.pass("Verified given rewards is matching with the expected size -- " + dataSet.get("expectedSize2"));

		boolean val2 = pageObj.guestTimelinePage().labelVisible(dataSet.get("label3"));
		Assert.assertTrue(val2, "Membership level --" + dataSet.get("label3") + " not visible");
		logger.info("Verified Membership level --" + dataSet.get("label3") + " visible");
		utils.logPass("Verified Membership level --" + dataSet.get("label3") + " visible");

		// pos checkin -- 3
		Response resp3 = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"), dataSet.get("amount3"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp3.getStatusCode(), "Status code 200 did not matched for post chekin api");
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		int start3 = Integer.parseInt(dataSet.get("start3"));
		int end3 = Integer.parseInt(dataSet.get("end3"));
		List<String> rangeList3 = pageObj.cockpitRedemptionsPage().divideRange(start3, end3, divide);
		int counter3 = 0;
		for (int i = 0; i < rangeList3.size(); i++) {
			boolean val = pageObj.guestTimelinePage().redeemablePointsRangeVisible(rangeList3.get(i),
					dataSet.get("redeemable3"));
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

		boolean val3 = pageObj.guestTimelinePage().labelVisible(dataSet.get("label3"));
		Assert.assertTrue(val3, "Membership level --" + dataSet.get("label3") + " not visible");
		logger.info("Verified Membership level --" + dataSet.get("label3") + " visible");
		utils.logPass("Verified Membership level --" + dataSet.get("label3") + " visible");
	}

	@Test(description = "SQ-T4524 [Points to Reward] Validate reward gifting with diff membership configuration and redemption mark set at business level.", groups = {
			"regression", "unstable", "dailyrun" }, priority = 5)
	@Owner(name = "Rakhi Rawat")
	public void T4524_ValidateRedemptionMarkSetBusinessLevel() throws InterruptedException {
		// open instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// on the memberships
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_has_membership_levels", "check");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_membership_level_bump_on_edge", "check");
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// update redeemption mark
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("redeemptionMark"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// update redeemption mark
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		for (int i = 1; i < 4; i++) {
			String membershipMinPoints = dataSet.get("membership" + i + "MinPoints");
			String membershipMaxPoints = dataSet.get("membership" + i + "MaxPoints");
			pageObj.settingsPage().clickMemberLevel(dataSet.get("membership" + i));
			pageObj.settingsPage().editMembershipLevelMinMaxPoints(membershipMinPoints, membershipMaxPoints);
			pageObj.settingsPage().editRedeemptionMarkFromMembership(dataSet.get("redeemptionMark"));
			pageObj.settingsPage().setMembershipLevelAttainmentReward("Base Redeemable");
			pageObj.settingsPage().setBankedRedeemable(dataSet.get("redeemable" + i));
			pageObj.settingsPage().clickUpdateMembership();
			logger.info("update the membership -- " + dataSet.get("membership" + i));
			utils.logit("update the membership -- " + dataSet.get("membership" + i));
		}

		// Signup using mobile api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		int divide = Integer.parseInt(dataSet.get("divide"));

		// pos checkin -- 1
		Response resp1 = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"), dataSet.get("amount1"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp1.getStatusCode(), "Status code 200 did not matched for post chekin api");
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		int start1 = Integer.parseInt(dataSet.get("start1"));
		int end1 = Integer.parseInt(dataSet.get("end1"));
		List<String> rangeList1 = pageObj.cockpitRedemptionsPage().divideRange(start1, end1, divide);
		int counter = 0;
		for (int i = 0; i < rangeList1.size(); i++) {
			boolean val = pageObj.guestTimelinePage().redeemablePointsRangeVisible(rangeList1.get(i),
					dataSet.get("redeemable1"));
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
		Response resp2 = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"), dataSet.get("amount2"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp2.getStatusCode(), "Status code 200 did not matched for post chekin api");
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		int start2 = Integer.parseInt(dataSet.get("start2"));
		int end2 = Integer.parseInt(dataSet.get("end2"));
		List<String> rangeList2 = pageObj.cockpitRedemptionsPage().divideRange(start2, end2, divide);
		int counter2 = 0;
		for (int i = 0; i < rangeList2.size(); i++) {
			boolean val = pageObj.guestTimelinePage().redeemablePointsRangeVisible(rangeList2.get(i),
					dataSet.get("redeemable2"));
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
		Response resp3 = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"), dataSet.get("amount3"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp3.getStatusCode(), "Status code 200 did not matched for post chekin api");
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		int start3 = Integer.parseInt(dataSet.get("start3"));
		int end3 = Integer.parseInt(dataSet.get("end3"));
		List<String> rangeList3 = pageObj.cockpitRedemptionsPage().divideRange(start3, end3, divide);
		int counter3 = 0;
		for (int i = 0; i < rangeList3.size(); i++) {
			boolean val = pageObj.guestTimelinePage().redeemablePointsRangeVisible(rangeList3.get(i),
					dataSet.get("redeemable3"));
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

		//
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		for (int i = 1; i < 4; i++) {
			pageObj.settingsPage().clickMemberLevel(dataSet.get("membership" + i));

			pageObj.settingsPage().editRedeemptionMarkFromMembership("");

			pageObj.settingsPage().clickUpdateMembership();
			logger.info("update the membership -- " + dataSet.get("membership" + i));
			utils.logit("update the membership -- " + dataSet.get("membership" + i));
		}

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
