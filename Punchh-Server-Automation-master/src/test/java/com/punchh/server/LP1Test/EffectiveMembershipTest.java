
package com.punchh.server.LP1Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class EffectiveMembershipTest {
	private static Logger logger = LogManager.getLogger(EffectiveMembershipTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String userEmail;
	private String sTCName;
	private String baseUrl;
	String blankString = "";
	private String env, run = "ui";
	private static Map<String, String> dataSet;
	Properties prop;
	Utilities utils;
	String externalUID;

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
		prop = Utilities.loadPropertiesFile("config.properties");
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
	}

	// Anant
	@Test(description = "SQ-T4714 Verify a guest should receive tier bump notifications and attainment reward when upgrades their effective tier due to earning/gifting post the account reset if the flag \"notify_on_operative_level_bump\" is False", priority = 0, groups = {
			"regression", "unstable", "dailyrun" })
	@Owner(name = "Hardik Bhardwaj")
	public void T4714_guestReceiveTierBumpNotification() throws Exception {
		String updateQuery = null;

		String membershipText1 = dataSet.get("membership1") + " " + CreateDateTime.getCurrentDateTimeInUtc();
		String membershipText2 = dataSet.get("membership2") + " " + CreateDateTime.getCurrentDateTimeInUtc();
		String membershipText3 = dataSet.get("membership3") + " " + CreateDateTime.getCurrentDateTimeInUtc();
		logger.info("membershipText1 " + membershipText1);
		logger.info("membershipText2 " + membershipText2);
		logger.info("membershipText3 " + membershipText3);

		List<String> textLst = new ArrayList<>();
		textLst.add(membershipText1);
		textLst.add(membershipText2);
		textLst.add(membershipText3);

		String membershipId1 = dataSet.get("bronze");
		String membershipId2 = dataSet.get("gold");
		String membershipId3 = dataSet.get("silver");

		List<String> posCheckinAmount = new ArrayList<>();
		posCheckinAmount.add(dataSet.get("amount1"));
		posCheckinAmount.add(dataSet.get("amount2"));
		posCheckinAmount.add(dataSet.get("amount3"));

		String query3 = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ dataSet.get("business_id") + "'";
		pageObj.singletonDBUtilsObj();
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query3, "preferences");

		pageObj.singletonDBUtilsObj();
		// set value true
		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, "false", dataSet.get("dbFlag"),
				dataSet.get("business_id"));
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to false");
		logger.info(dataSet.get("dbFlag") + " value is updated to false");
		TestListeners.extentTest.get().info(dataSet.get("dbFlag") + " value is updated to false");

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// on the memberships
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_has_membership_levels", "check");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_membership_level_bump_on_edge", "check");
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		pageObj.earningPage().accountReEvaluationStrategy("Guest Inactivity");
		pageObj.earningPage().updateConfiguration();
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		pageObj.earningPage().setExpiresAfter("clear", 0);
		pageObj.earningPage().expiryDateStrategy("Exact Days");
		pageObj.earningPage().setInactiveDays("set", 365);
		pageObj.earningPage().membershipLevelResetStrategy("Reset Based On Points Earned");
		// pageObj.dashboardpage().checkUncheckToggle("Decouple Checkin and Membership
		// Expiry", "OFF");
		pageObj.earningPage().updateConfiguration();
		pageObj.earningPage().verifyMessage("Business was successfully updated.");

		// update redeemption mark
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("RedeemptionMark"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		for (int i = 1; i < 4; i++) {
			pageObj.settingsPage().clickMemberLevel(dataSet.get("membership" + i));
			// pageObj.settingsPage().editRedeemptionMarkFromMembership(dataSet.get("RedeemptionMark"));
			pageObj.settingsPage().setPushNotificationInMembership(textLst.get(i - 1));
			pageObj.settingsPage().setEmailInMembership(textLst.get(i - 1));
			pageObj.settingsPage().clickUpdateMembership();
			logger.info("update the membership -- " + dataSet.get("membership" + i));
			TestListeners.extentTest.get().info("update the membership -- " + dataSet.get("membership" + i));
		}

		// Signup using mobile api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		String userID = signUpResponse.jsonPath().get("id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), 200);

		// pos checkin
		Response poscheckin = null;
		poscheckin = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationKey"), posCheckinAmount.get(0));
		Assert.assertEquals(200, poscheckin.getStatusCode(), "Status code 200 did not matched for post chekin api");
		logger.info("done a checkin of amount -- " + posCheckinAmount.get(0));
		TestListeners.extentTest.get().info("done a checkin of amount -- " + posCheckinAmount.get(0));

		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		int silverMembership = pageObj.guestTimelinePage().checkMembershipNotify(textLst.get(1), 1);
		Assert.assertEquals(silverMembership, 1, textLst.get(1) + " is not visible 1 time on the guest timeline");
		logger.info("Verified as expected " + textLst.get(1) + " is visible 1 time on the timeline");
		TestListeners.extentTest.get()
				.pass("Verified as expected " + textLst.get(1) + " is visible 1 time on the timeline");

		// effective
		String query1 = "SELECT `membership_level_id` from `membership_level_histories` where `user_id` = '" + userID
				+ "'order by previous_membership_level_id desc;";
		pageObj.singletonDBUtilsObj();
		String expColValue1 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query1, "membership_level_id", 10);

		Assert.assertEquals(expColValue1, membershipId3, "Effective membership strategy is not " + membershipId3 + "");
		logger.info("Verified that Effective membership strategy is " + membershipId3 + " ");
		TestListeners.extentTest.get().pass("Verified that Effective membership strategy is " + membershipId3 + "");

		// operative
		String query2 = "SELECT `operative_membership_level_id` from `membership_level_histories` where `user_id` = '"
				+ userID + "' order by operative_membership_level_id desc;";
		pageObj.singletonDBUtilsObj();
		String expColValue2 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query2,
				"operative_membership_level_id", 10); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertEquals(expColValue2, membershipId3, "Operative membership strategy is not " + membershipId3 + "");
		logger.info("Verified that Operative membership strategy is " + membershipId3 + "");
		TestListeners.extentTest.get().pass("Verified that Operative membership strategy is " + membershipId3 + "");

		String dateAndTime = CreateDateTime.getCurrentDateInPreviousYear(1);

		// update checkin table
		updateQuery = "update checkins set created_at='" + dateAndTime + "' where user_id='" + userID
				+ "' and receipt_amount= '" + posCheckinAmount.get(0) + "'";
		pageObj.singletonDBUtilsObj();
		DBUtils.executeUpdateQuery(env, updateQuery);

		// update the account table
		updateQuery = "update accounts set created_at='" + dateAndTime + "' where user_id=" + userID;
		pageObj.singletonDBUtilsObj();
		DBUtils.executeUpdateQuery(env, updateQuery);

		// update user table
		updateQuery = "update users set last_activity_at='" + dateAndTime + "' where id=" + userID;
		pageObj.singletonDBUtilsObj();
		DBUtils.executeUpdateQuery(env, updateQuery);

		updateQuery = "update users set joined_at='" + dateAndTime + "' where id=" + userID;
		pageObj.singletonDBUtilsObj();
		DBUtils.executeUpdateQuery(env, updateQuery);

		updateQuery = "update users set created_at='" + dateAndTime + "' where id=" + userID;
		pageObj.singletonDBUtilsObj();
		DBUtils.executeUpdateQuery(env, updateQuery);

		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().runRecallScheule();

		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		// effective
		String query4 = "SELECT `membership_level_id` from `membership_level_histories` where `user_id` = '" + userID
				+ "'order by previous_membership_level_id;";
		boolean verifyColValue4 = DBUtils.verifyValueFromDBUsingPolling(env, query4, "membership_level_id",
				membershipId1);
		Assert.assertTrue(verifyColValue4,
				"Effective membership strategy is not " + membershipId1 + " after scheduler run");
		logger.info("Verified that Effective membership strategy is " + membershipId1 + " after scheduler run");
		TestListeners.extentTest.get()
				.pass("Verified that Effective membership strategy is " + membershipId1 + " after scheduler run");

		// operative
		String query6 = "SELECT `operative_membership_level_id` from `membership_level_histories` where `user_id` = '"
				+ userID + "' order by operative_membership_level_id desc;";
		boolean verifyColValue6 = DBUtils.verifyValueFromDBUsingPolling(env, query6, "operative_membership_level_id",
				membershipId3);
		Assert.assertTrue(verifyColValue6,
				"Operative membership strategy is not " + membershipId3 + "after scheduler run");
		logger.info("Verified that Operative membership strategy is " + membershipId3 + "after scheduler run");
		TestListeners.extentTest.get()
				.pass("Verified that Operative membership strategy is " + membershipId3 + "after scheduler run");

		// checkin of 110
		poscheckin = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationKey"), posCheckinAmount.get(1));
		Assert.assertEquals(200, poscheckin.getStatusCode(), "Status code 200 did not matched for post chekin api");
		logger.info("done a checkin of amount -- " + posCheckinAmount.get(1));
		TestListeners.extentTest.get().info("done a checkin of amount -- " + posCheckinAmount.get(1));

		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		silverMembership = pageObj.guestTimelinePage().checkMembershipNotify(textLst.get(1), 1);
		Assert.assertEquals(silverMembership, 1, textLst.get(1) + " is not visible 2 time on the guest timeline");
		logger.info("Verified as expected " + textLst.get(1) + " is visible 2 time on the timeline");
		TestListeners.extentTest.get()
				.pass("Verified as expected " + textLst.get(1) + " is visible 2 time on the timeline");

		utils.longWaitInSeconds(3);
		boolean expColValue7Flag = false;
		// effective
		String query7 = "SELECT `membership_level_id` from `membership_level_histories` where `user_id` = '" + userID
				+ "'order by created_at desc limit 1;";
		pageObj.singletonDBUtilsObj();
		String expColValue7 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query7, "membership_level_id", 10);

		if (Objects.equals(expColValue7, membershipId3) || Objects.equals(expColValue7, membershipId2)
				|| Objects.equals(expColValue7, membershipId1)) {
			expColValue7Flag = true;
		}
		Assert.assertTrue(expColValue7Flag, "Effective membership strategy is not matched");
		logger.info("Verified that Effective membership strategy is " + membershipId3 + " ");
		TestListeners.extentTest.get().pass("Verified that Effective membership strategy is " + membershipId3 + "");

		// operative
		String query8 = "SELECT `operative_membership_level_id` from `membership_level_histories` where `user_id` = '"
				+ userID + "' order by created_at desc limit 1;";
		pageObj.singletonDBUtilsObj();
		String expColValue8 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query8,
				"operative_membership_level_id", 10); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertEquals(expColValue8, membershipId3, "Operative membership strategy is not " + membershipId3 + "");
		logger.info("Verified that Operative membership strategy is " + membershipId3 + "");
		TestListeners.extentTest.get().pass("Verified that Operative membership strategy is " + membershipId3 + "");

		// checkin of 400
		poscheckin = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationKey"), posCheckinAmount.get(2));
		Assert.assertEquals(200, poscheckin.getStatusCode(), "Status code 200 did not matched for post chekin api");
		logger.info("done a checkin of amount -- " + posCheckinAmount.get(2));
		TestListeners.extentTest.get().info("done a checkin of amount -- " + posCheckinAmount.get(2));

		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		int goldMembership = pageObj.guestTimelinePage().checkMembershipNotify(textLst.get(2), 1);
		Assert.assertEquals(goldMembership, 1, textLst.get(2) + " is not visible 1 time on the guest timeline");
		logger.info("Verified as expected " + textLst.get(2) + " is visible 1 time on the timeline");
		TestListeners.extentTest.get()
				.pass("Verified as expected " + textLst.get(2) + " is visible 1 time on the timeline");

		// effective
		String query9 = "SELECT `membership_level_id` from `membership_level_histories` where `user_id` = '" + userID
				+ "'order by created_at desc limit 1;";
		pageObj.singletonDBUtilsObj();
		String expColValue9 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query9, "membership_level_id", 10);

		Assert.assertEquals(expColValue9, membershipId2, "Effective membership strategy is not " + membershipId3 + "");
		logger.info("Verified that Effective membership strategy is " + membershipId2 + " ");
		TestListeners.extentTest.get().pass("Verified that Effective membership strategy is " + membershipId2 + "");

		// operative
		String query10 = "SELECT `operative_membership_level_id` from `membership_level_histories` where `user_id` = '"
				+ userID + "' order by created_at desc limit 1;";
		pageObj.singletonDBUtilsObj();
		String expColValue10 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query10,
				"operative_membership_level_id", 10); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertEquals(expColValue10, membershipId2, "Operative membership strategy is not " + membershipId2 + "");
		logger.info("Verified that Operative membership strategy is " + membershipId2 + "");
		TestListeners.extentTest.get().pass("Verified that Operative membership strategy is " + membershipId2 + "");
	}

	@Test(description = "SQ-T4715 Verify a guest should receive tier bump notifications and attainment reward when upgrades their operative tier due to earning/gifting post the account reset if the flag \"notify_on_operative_level_bump\" is True\n", groups = {
			"regression", "dailyrun" }, priority = 1)
	@Owner(name = "Vansham Mishra")
	public void T4715_guestReceiveTierBumpNotification() throws Exception {
		String updateQuery = null;

		String membershipText1 = dataSet.get("membership1") + " " + CreateDateTime.getCurrentDateTimeInUtc();
		String membershipText2 = dataSet.get("membership2") + " " + CreateDateTime.getCurrentDateTimeInUtc();
		String membershipText3 = dataSet.get("membership3") + " " + CreateDateTime.getCurrentDateTimeInUtc();

		List<String> textLst = new ArrayList<>();
		textLst.add(membershipText1);
		textLst.add(membershipText2);
		textLst.add(membershipText3);

		String membershipId1 = dataSet.get("bronze");
		String membershipId2 = dataSet.get("gold");
		String membershipId3 = dataSet.get("silver");

		List<String> posCheckinAmount = new ArrayList<>();
		posCheckinAmount.add(dataSet.get("amount1"));
		posCheckinAmount.add(dataSet.get("amount2"));
		posCheckinAmount.add(dataSet.get("amount3"));

		String query3 = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ dataSet.get("business_id") + "'";
		pageObj.singletonDBUtilsObj();
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query3, "preferences");

		pageObj.singletonDBUtilsObj();
		// set value true
		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag"),
				dataSet.get("business_id"));
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to false");
		logger.info(dataSet.get("dbFlag") + " value is updated to false");
		TestListeners.extentTest.get().info(dataSet.get("dbFlag") + " value is updated to false");

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// on the memberships
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_has_membership_levels", "check");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_membership_level_bump_on_edge", "check");
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		pageObj.earningPage().accountReEvaluationStrategy("Guest Inactivity");
		pageObj.earningPage().updateConfiguration();
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		pageObj.earningPage().setExpiresAfter("clear", 0);
		pageObj.earningPage().expiryDateStrategy("Exact Days");
		pageObj.earningPage().setInactiveDays("set", 365);
		pageObj.earningPage().membershipLevelResetStrategy("Reset Based On Points Earned");
		// pageObj.dashboardpage().checkUncheckToggle("Decouple Checkin and Membership
		// Expiry", "OFF");
		pageObj.earningPage().updateConfiguration();
		pageObj.earningPage().verifyMessage("Business was successfully updated.");

		// update redeemption mark
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("RedeemptionMark"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		for (int i = 1; i < 4; i++) {
			pageObj.settingsPage().clickMemberLevel(dataSet.get("membership" + i));
			// pageObj.settingsPage().editRedeemptionMarkFromMembership(dataSet.get("RedeemptionMark"));
			pageObj.settingsPage().setPushNotificationInMembership(textLst.get(i - 1));
			pageObj.settingsPage().setEmailInMembership(textLst.get(i - 1));
			pageObj.settingsPage().clickUpdateMembership();
			logger.info("update the membership -- " + dataSet.get("membership" + i));
			TestListeners.extentTest.get().info("update the membership -- " + dataSet.get("membership" + i));
		}

		// Signup using mobile api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		String userID = signUpResponse.jsonPath().get("id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), 200);

		// pos checkin
		Response poscheckin = null;
		poscheckin = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationKey"), posCheckinAmount.get(0));
		Assert.assertEquals(200, poscheckin.getStatusCode(), "Status code 200 did not matched for post chekin api");
		logger.info("done a checkin of amount -- " + posCheckinAmount.get(0));
		TestListeners.extentTest.get().info("done a checkin of amount -- " + posCheckinAmount.get(0));

		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		int silverMembership = pageObj.guestTimelinePage().checkMembershipNotify(textLst.get(1), 1);
		Assert.assertEquals(silverMembership, 1, textLst.get(1) + " is not visible 1 time on the guest timeline");
		logger.info("Verified as expected " + textLst.get(1) + " is visible 1 time on the timeline");
		TestListeners.extentTest.get()
				.pass("Verified as expected " + textLst.get(1) + " is visible 1 time on the timeline");

		// effective
		String query1 = "SELECT `membership_level_id` from `membership_level_histories` where `user_id` = '" + userID
				+ "'order by previous_membership_level_id desc;";
		pageObj.singletonDBUtilsObj();
		String expColValue1 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query1, "membership_level_id", 10);

		Assert.assertEquals(expColValue1, membershipId3, "Effective membership strategy is not " + membershipId3 + "");
		logger.info("Verified that Effective membership strategy is " + membershipId3 + " ");
		TestListeners.extentTest.get().pass("Verified that Effective membership strategy is " + membershipId3 + "");

		// operative
		String query2 = "SELECT `operative_membership_level_id` from `membership_level_histories` where `user_id` = '"
				+ userID + "' order by operative_membership_level_id desc;";
		pageObj.singletonDBUtilsObj();
		String expColValue2 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query2,
				"operative_membership_level_id", 10); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertEquals(expColValue2, membershipId3, "Operative membership strategy is not " + membershipId3 + "");
		logger.info("Verified that Operative membership strategy is " + membershipId3 + "");
		TestListeners.extentTest.get().pass("Verified that Operative membership strategy is " + membershipId3 + "");

		String dateAndTime = CreateDateTime.getCurrentDateInPreviousYear(1);

		// update checkin table
		updateQuery = "update checkins set created_at='" + dateAndTime + "' where user_id='" + userID
				+ "' and receipt_amount= '" + posCheckinAmount.get(0) + "'";
		pageObj.singletonDBUtilsObj();
		DBUtils.executeUpdateQuery(env, updateQuery);

		// update the account table
		updateQuery = "update accounts set created_at='" + dateAndTime + "' where user_id=" + userID;
		pageObj.singletonDBUtilsObj();
		DBUtils.executeUpdateQuery(env, updateQuery);

		// update user table
		updateQuery = "update users set last_activity_at='" + dateAndTime + "' where id=" + userID;
		pageObj.singletonDBUtilsObj();
		DBUtils.executeUpdateQuery(env, updateQuery);

		updateQuery = "update users set joined_at='" + dateAndTime + "' where id=" + userID;
		pageObj.singletonDBUtilsObj();
		DBUtils.executeUpdateQuery(env, updateQuery);

		updateQuery = "update users set created_at='" + dateAndTime + "' where id=" + userID;
		pageObj.singletonDBUtilsObj();
		DBUtils.executeUpdateQuery(env, updateQuery);

		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().runRecallScheule();

		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		boolean expColValue4Flag = false;
		// effective
		String query4 = "SELECT `membership_level_id` from `membership_level_histories` where `user_id` = '" + userID
				+ "'order by previous_membership_level_id desc;";
		pageObj.singletonDBUtilsObj();
		String expColValue4 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query4, "membership_level_id", 10);

		if (membershipId1.equals(expColValue4) || membershipId2.equals(expColValue4)
				|| membershipId3.equals(expColValue4)) {
			expColValue4Flag = true;
		}
		Assert.assertTrue(expColValue4Flag,
				"Effective membership strategy is not " + membershipId1 + " after scheduler run");
		logger.info("Verified that Effective membership strategy is " + membershipId1 + " after scheduler run");
		TestListeners.extentTest.get()
				.pass("Verified that Effective membership strategy is " + membershipId1 + " after scheduler run");

		// operative
		String query6 = "SELECT `operative_membership_level_id` from `membership_level_histories` where `user_id` = '"
				+ userID + "' order by operative_membership_level_id desc;";
		pageObj.singletonDBUtilsObj();
		String expColValue6 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query6,
				"operative_membership_level_id", 10); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertEquals(expColValue6, membershipId3,
				"Operative membership strategy is not " + membershipId3 + "after scheduler run");
		logger.info("Verified that Operative membership strategy is " + membershipId3 + "after scheduler run");
		TestListeners.extentTest.get()
				.pass("Verified that Operative membership strategy is " + membershipId3 + "after scheduler run");

		// checkin of 110
		poscheckin = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationKey"), posCheckinAmount.get(1));
		Assert.assertEquals(200, poscheckin.getStatusCode(), "Status code 200 did not matched for post chekin api");
		logger.info("done a checkin of amount -- " + posCheckinAmount.get(1));
		TestListeners.extentTest.get().info("done a checkin of amount -- " + posCheckinAmount.get(1));

		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		utils.longWaitInSeconds(2);

		// effective
		String query7 = "SELECT `membership_level_id` from `membership_level_histories` where `user_id` = '" + userID
				+ "'order by created_at desc limit 1;";
		pageObj.singletonDBUtilsObj();
		String expColValue7 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query7, "membership_level_id", 10);

		Assert.assertEquals(expColValue7, membershipId3, "Effective membership strategy is not " + membershipId3 + "");
		logger.info("Verified that Effective membership strategy is " + membershipId3 + " ");
		TestListeners.extentTest.get().pass("Verified that Effective membership strategy is " + membershipId3 + "");

		// operative
		String query8 = "SELECT `operative_membership_level_id` from `membership_level_histories` where `user_id` = '"
				+ userID + "' order by created_at desc limit 1;";
		pageObj.singletonDBUtilsObj();
		String expColValue8 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query8,
				"operative_membership_level_id", 10); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertEquals(expColValue8, membershipId3, "Operative membership strategy is not " + membershipId3 + "");
		logger.info("Verified that Operative membership strategy is " + membershipId3 + "");
		TestListeners.extentTest.get().pass("Verified that Operative membership strategy is " + membershipId3 + "");

		// checkin of 400
		poscheckin = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationKey"), posCheckinAmount.get(2));
		Assert.assertEquals(200, poscheckin.getStatusCode(), "Status code 200 did not matched for post chekin api");
		logger.info("done a checkin of amount -- " + posCheckinAmount.get(2));
		TestListeners.extentTest.get().info("done a checkin of amount -- " + posCheckinAmount.get(2));

		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		int goldMembership = pageObj.guestTimelinePage().checkMembershipNotify(textLst.get(2), 1);
		Assert.assertEquals(goldMembership, 1, textLst.get(2) + " is not visible 1 time on the guest timeline");
		logger.info("Verified as expected " + textLst.get(2) + " is visible 1 time on the timeline");
		TestListeners.extentTest.get()
				.pass("Verified as expected " + textLst.get(2) + " is visible 1 time on the timeline");

		// effective
		String query9 = "SELECT `membership_level_id` from `membership_level_histories` where `user_id` = '" + userID
				+ "'order by created_at desc limit 1;";
		pageObj.singletonDBUtilsObj();
		String expColValue9 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query9, "membership_level_id", 10);

		Assert.assertEquals(expColValue9, membershipId2, "Effective membership strategy is not " + membershipId3 + "");
		logger.info("Verified that Effective membership strategy is " + membershipId2 + " ");
		TestListeners.extentTest.get().pass("Verified that Effective membership strategy is " + membershipId2 + "");

		// operative
		String query10 = "SELECT `operative_membership_level_id` from `membership_level_histories` where `user_id` = '"
				+ userID + "' order by created_at desc limit 1;";
		pageObj.singletonDBUtilsObj();
		String expColValue10 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query10,
				"operative_membership_level_id", 10); // dbUtils.getValueFromColumn(query2, "membership_level_id");
		Assert.assertEquals(expColValue10, membershipId2, "Operative membership strategy is not " + membershipId2 + "");
		logger.info("Verified that Operative membership strategy is " + membershipId2 + "");
		TestListeners.extentTest.get().pass("Verified that Operative membership strategy is " + membershipId2 + "");
	}

	// Hardik Bhardwaj
	@Test(description = "SQ-T5439 Verify points should not set as negative in accounts table when run guest inactivity expiry", groups = {
			"regression", "dailyrun" }, priority = 5)
	@Owner(name = "Hardik Bhardwaj")
	public void T5439_guestReceiveTierBumpNotification() throws Exception {

		pageObj.sidekiqPage().sidekiqCheck(baseUrl);

		String b_id = dataSet.get("business_id");

		String businessPreferenceLiveQuery = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ b_id + "'";
		String businessPreferenceLiveExpColValue = DBUtils.executeQueryAndGetColumnValue(env,
				businessPreferenceLiveQuery, "preferences");

		boolean businessLiveFlag = DBUtils.updateBusinessesPreference(env,
				businessPreferenceLiveExpColValue, "false", "live", b_id);
		Assert.assertTrue(businessLiveFlag, "live value is not updated to false");
		logger.info("live value is updated to false");
		TestListeners.extentTest.get().info("live value is updated to false");

		boolean businessWentLIveflag = DBUtils.updateBusinessesPreference(env,
				businessPreferenceLiveExpColValue, "false", "went_live", b_id);
		Assert.assertTrue(businessWentLIveflag, "went_live value is not updated to false");
		logger.info("went_live value is updated to false");
		TestListeners.extentTest.get().info("went_live value is updated to false");

		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ dataSet.get("business_id") + "'";
		pageObj.singletonDBUtilsObj();
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");

		pageObj.singletonDBUtilsObj();
		// set value true
		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag"),
				dataSet.get("business_id"));
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to true");
		logger.info(dataSet.get("dbFlag") + " value is updated to true");
		TestListeners.extentTest.get().info(dataSet.get("dbFlag") + " value is updated to true");

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), 200, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("API2 Signup is successful");
		logger.info("API2 Signup is successful");

		// Do checkin of 320
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationKey"), "320");
		Assert.assertEquals(checkinResponse.getStatusCode(), 200, "Status code 200 did not match with POS Checkin ");
		TestListeners.extentTest.get().pass("POS checkin is successful for 320 dollar");
		logger.info("POS checkin is successful for 320 dollar");

		// force redemption of 10 points using api
		Response forceRedeem_Response = pageObj.endpoints().pointForceRedeem(dataSet.get("apiKey"), userID, "10");
		Assert.assertEquals(forceRedeem_Response.getStatusCode(), 201,
				"Status code 201 did not matched for pos redemption api");
		logger.info("Force redemption of 10 points is successful");
		TestListeners.extentTest.get().info("Force redemption of 10 points is successful");
		// String redemption_id1 =
		// forceRedeem_Response.jsonPath().get("redemption_id").toString();

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
//		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		pageObj.earningPage().accountReEvaluationStrategy("Guest Inactivity");
		pageObj.earningPage().updateConfiguration();
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		pageObj.earningPage().setExpiresAfter("clear", 0);
		pageObj.earningPage().expiryDateStrategy("Exact Days");
		pageObj.earningPage().setInactiveDays("set", 1);
		pageObj.earningPage().membershipLevelResetStrategy("Reset Based On Points Earned");
		pageObj.dashboardpage().checkUncheckToggle("Decouple Checkin and Membership Expiry", "OFF");
		pageObj.earningPage().updateConfiguration();
		pageObj.earningPage().verifyMessage("Business was successfully updated.");

		// update redeemption mark
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("RedeemptionMark"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// Fetch Checkins
		Response fetchCheckinResponse = pageObj.endpoints().Api2FetchCheckin(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(fetchCheckinResponse.getStatusCode(), 200,
				"Status code 200 did not matched for api2 fetch checkin");
		String checkin_id = fetchCheckinResponse.jsonPath().get("[0].checkin_id").toString();

		String newcreated_at = CreateDateTime.getYesterdayDateTimeUTC(2);

		// DB query for user updation

		String query4 = "UPDATE `users` SET `created_at` = '" + newcreated_at + "' , `updated_at` = '" + newcreated_at
				+ "', `joined_at` = '" + newcreated_at + "', `last_activity_at`= '" + newcreated_at + "' WHERE id = '"
				+ userID + "';";
		pageObj.singletonDBUtilsObj();
		int rs = DBUtils.executeUpdateQuery(env, query4); // stmt.executeUpdate(query);
		Assert.assertEquals(rs, 1, "user table query is not working");
		logger.info("Users table updated successfully.");
		TestListeners.extentTest.get().info("Users table updated successfully.");

		// DB query for checkins updation
		String query5 = "UPDATE `checkins` SET `created_at` = '" + newcreated_at + "' , `updated_at` = '"
				+ newcreated_at + "' WHERE id = '" + checkin_id + "';";
		pageObj.singletonDBUtilsObj();
		int rs1 = DBUtils.executeUpdateQuery(env, query5); // stmt.executeUpdate(query1);
		Assert.assertEquals(rs1, 1, "checkins table query is not working");
		logger.info("checkins table updated successfully.");
		TestListeners.extentTest.get().info("checkins table updated successfully.");

		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().runRecallScheule();

		utils.longWaitInSeconds(8);
		String query1 = "Select `business_id` from `expiry_events` where `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue1 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query1, "business_id",
				150); // dbUtils.getValueFromColumn(query3, "expire_item_type");
		Assert.assertEquals(expColValue1, dataSet.get("business_id"),
				"Entry is not created in expiry_events table in DB");
		logger.info("Entry is created in expiry_events table in DB");
		TestListeners.extentTest.get().pass("Entry is created in expiry_events table in DB");

		String query2 = "Select `total_debits` from `accounts` where `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue2 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query2, "total_debits",
				100); // dbUtils.getValueFromColumn(query3, "expire_item_type");
		Assert.assertEquals(expColValue2, "0.00",
				"total_debits column in accounts table is not equal to expected value i.e. 0");
		logger.info("total_debits column in accounts table is equal to expected value i.e. 0");
		TestListeners.extentTest.get().pass("total_debits column in accounts table is equal to expected value i.e. 0");

		String query3 = "Select `total_credits` from `accounts` where `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue3 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3, "total_credits",
				100); // dbUtils.getValueFromColumn(query3, "expire_item_type");
		Assert.assertEquals(expColValue3, "0.0",
				"total_credits column in accounts table is not equal to expected value i.e. 0");
		logger.info("total_credits column in accounts table is equal to expected value i.e. 0");
		TestListeners.extentTest.get().pass("total_credits column in accounts table is equal to expected value i.e. 0");

		// Mobile User Balance
		Response userBalanceResponse = pageObj.endpoints().Api2FetchUserBalance(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(userBalanceResponse.getStatusCode(), 200,
				"Status code 200 did not matched for api2 fetch user balance");
		String userBalanceInMobile = userBalanceResponse.jsonPath().get("account_balance.redeemable_points").toString();
		TestListeners.extentTest.get().info("API2 User Balance is successful");
		logger.info("API2 User Balance is successful");
		Assert.assertEquals(userBalanceInMobile, "0.0", "user balance is not equal to expected value i.e. 0");
		logger.info("user balance is equal to expected value i.e. 0");
		TestListeners.extentTest.get().pass("user balance is equal to expected value i.e. 0");

		// Mobile User Account History
		Response accountHistoryResponse = pageObj.endpoints().Api2UserAccountHistory(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(200, accountHistoryResponse.getStatusCode(),
				"Status code 200 did not matched for api2 point conversion");
		String userBalanceRewardInHistory = accountHistoryResponse.jsonPath().getString("[0].total_points");
		TestListeners.extentTest.get().info("API2 User Balance is successful");
		logger.info("API2 User Balance is successful");
		Assert.assertEquals(userBalanceRewardInHistory, "0",
				"user balance in account histroy is not equal to expected value i.e. 0");
		logger.info("user balance in account histroy is equal to expected value i.e. 0");
		TestListeners.extentTest.get().pass("user balance in account histroy is equal to expected value i.e. 0");

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUserWithEndDate(userID, dataSet.get("apiKey"),
				"", "", "", "150", "");
		logger.info("Send redeemable to the user successfully");
		TestListeners.extentTest.get().pass("Send redeemable to the user successfully");
		Assert.assertEquals(201, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		// Mobile User Balance
		Response userBalanceResponse1 = pageObj.endpoints().Api2FetchUserBalance(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(userBalanceResponse1.getStatusCode(), 200,
				"Status code 200 did not matched for api2 fetch user balance");
		String userBalanceInMobile1 = userBalanceResponse1.jsonPath().get("account_balance.redeemable_points")
				.toString();
		TestListeners.extentTest.get().info("API2 User Balance is successful");
		logger.info("API2 User Balance is successful");
		Assert.assertEquals(userBalanceInMobile1, "150.0", "user balance is not equal to expected value i.e. 0");
		logger.info("user balance is equal to expected value i.e. 0");
		TestListeners.extentTest.get().pass("user balance is equal to expected value i.e. 0");

		// Mobile User Account History
		Response accountHistoryResponse1 = pageObj.endpoints().Api2UserAccountHistory(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(200, accountHistoryResponse1.getStatusCode(),
				"Status code 200 did not matched for api2 point conversion");
		String userBalanceRewardInHistory1 = accountHistoryResponse1.jsonPath().getString("[0].total_points");
		TestListeners.extentTest.get().info("API2 User Balance is successful");
		logger.info("API2 User Balance is successful");
		Assert.assertEquals(userBalanceRewardInHistory1, "150",
				"user balance in account histroy is not equal to expected value i.e. 0");
		logger.info("user balance in account histroy is equal to expected value i.e. 0");
		TestListeners.extentTest.get().pass("user balance in account histroy is equal to expected value i.e. 0");
	}

	// Hardik Bhardwaj
	@Test(description = "SQ-T5438 Verify points should not set as negative in accounts table with the consider_effective_redeemed_unbanked_points flag enabled when run annual on specific date expiry", priority = 3, groups = {
			"regression", "unstable", "dailyrun" })
	@Owner(name = "Hardik Bhardwaj")
	public void T5438_guestReceiveTierBumpNotification() throws Exception {

		pageObj.sidekiqPage().sidekiqCheck(baseUrl);

		String b_id = dataSet.get("business_id");

		String businessPreferenceLiveQuery = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ b_id + "'";
		String businessPreferenceLiveExpColValue = DBUtils.executeQueryAndGetColumnValue(env,
				businessPreferenceLiveQuery, "preferences");

		boolean businessLiveFlag = DBUtils.updateBusinessesPreference(env,
				businessPreferenceLiveExpColValue, "false", "live", b_id);
		Assert.assertTrue(businessLiveFlag, "live value is not updated to false");
		logger.info("live value is updated to false");
		TestListeners.extentTest.get().info("live value is updated to false");

		boolean businessWentLIveflag = DBUtils.updateBusinessesPreference(env,
				businessPreferenceLiveExpColValue, "false", "went_live", b_id);
		Assert.assertTrue(businessWentLIveflag, "went_live value is not updated to false");
		logger.info("went_live value is updated to false");
		TestListeners.extentTest.get().info("went_live value is updated to false");

		String date = CreateDateTime.getCurrentDateOnly();
		String month = CreateDateTime.getCurrentMonthOnly();

		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ dataSet.get("business_id") + "'";
		pageObj.singletonDBUtilsObj();
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");

		pageObj.singletonDBUtilsObj();
		// set value true
		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag"),
				dataSet.get("business_id"));
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to true");
		logger.info(dataSet.get("dbFlag") + " value is updated to true");
		TestListeners.extentTest.get().info(dataSet.get("dbFlag") + " value is updated to true");

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), 200, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("API2 Signup is successful");
		logger.info("API2 Signup is successful");

		// Do checkin of 320
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationKey"), "320");
		Assert.assertEquals(checkinResponse.getStatusCode(), 200, "Status code 200 did not match with POS Checkin ");
		TestListeners.extentTest.get().pass("POS checkin is successful for 320 dollar");
		logger.info("POS checkin is successful for 320 dollar");

		// force redemption of 10 points using api
		Response forceRedeem_Response = pageObj.endpoints().pointForceRedeem(dataSet.get("apiKey"), userID, "10");
		Assert.assertEquals(forceRedeem_Response.getStatusCode(), 201,
				"Status code 201 did not matched for pos redemption api");
		logger.info("Force redemption of 10 points is successful");
		TestListeners.extentTest.get().info("Force redemption of 10 points is successful");
		// String redemption_id1 =
		// forceRedeem_Response.jsonPath().get("redemption_id").toString();

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
//		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		pageObj.earningPage().accountReEvaluationStrategy("Annually on a Specific Date");
		pageObj.earningPage().updateConfiguration();
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
//		pageObj.earningPage().setExpiresAfter("set", 1);
		pageObj.earningPage().expiryDateStrategy("Exact Days");
//		pageObj.earningPage().setInactiveDays("set", 1);
		pageObj.earningPage().setExpiryDay("set", date);
		pageObj.earningPage().setExpiryMonth(month);
		pageObj.earningPage().membershipLevelResetStrategy("Reset Based On Points Earned");
		pageObj.earningPage().updateConfiguration();
		pageObj.earningPage().verifyMessage("Business was successfully updated.");

		// update redeemption mark
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("RedeemptionMark"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// Fetch Checkins
		Response fetchCheckinResponse = pageObj.endpoints().Api2FetchCheckin(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(fetchCheckinResponse.getStatusCode(), 200,
				"Status code 200 did not matched for api2 fetch checkin");
		String checkin_id = fetchCheckinResponse.jsonPath().get("[0].checkin_id").toString();

		String newcreated_at = CreateDateTime.getYesterdayDateTimeUTC(2);

		// DB query for user updation

		String query4 = "UPDATE `users` SET `created_at` = '" + newcreated_at + "' , `updated_at` = '" + newcreated_at
				+ "', `joined_at` = '" + newcreated_at + "', `last_activity_at`= '" + newcreated_at + "' WHERE id = '"
				+ userID + "';";
		pageObj.singletonDBUtilsObj();
		int rs = DBUtils.executeUpdateQuery(env, query4); // stmt.executeUpdate(query);
		Assert.assertEquals(rs, 1, "user table query is not working");
		logger.info("Users table updated successfully.");
		TestListeners.extentTest.get().info("Users table updated successfully.");

		// DB query for checkins updation
		String query5 = "UPDATE `checkins` SET `created_at` = '" + newcreated_at + "' , `updated_at` = '"
				+ newcreated_at + "' WHERE id = '" + checkin_id + "';";
		pageObj.singletonDBUtilsObj();
		int rs1 = DBUtils.executeUpdateQuery(env, query5); // stmt.executeUpdate(query1);
		Assert.assertEquals(rs1, 1, "checkins table query is not working");
		logger.info("checkins table updated successfully.");
		TestListeners.extentTest.get().info("checkins table updated successfully.");

		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().runRecallScheule();

		utils.longWaitInSeconds(8);
		String query1 = "Select `business_id` from `expiry_events` where `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue1 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query1, "business_id",
				150); // dbUtils.getValueFromColumn(query3, "expire_item_type");
		Assert.assertEquals(expColValue1, dataSet.get("business_id"),
				"Entry is not created in expiry_events table in DB");
		logger.info("Entry is created in expiry_events table in DB");
		TestListeners.extentTest.get().pass("Entry is created in expiry_events table in DB");

		String query2 = "Select `total_debits` from `accounts` where `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue2 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query2, "total_debits",
				100); // dbUtils.getValueFromColumn(query3, "expire_item_type");
		Assert.assertEquals(expColValue2, "0.00",
				"total_debits column in accounts table is not equal to expected value i.e. 0");
		logger.info("total_debits column in accounts table is equal to expected value i.e. 0");
		TestListeners.extentTest.get().pass("total_debits column in accounts table is equal to expected value i.e. 0");

		String query3 = "Select `total_credits` from `accounts` where `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue3 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3, "total_credits",
				100); // dbUtils.getValueFromColumn(query3, "expire_item_type");
		Assert.assertEquals(expColValue3, "0.0",
				"total_credits column in accounts table is not equal to expected value i.e. 0");
		logger.info("total_credits column in accounts table is equal to expected value i.e. 0");
		TestListeners.extentTest.get().pass("total_credits column in accounts table is equal to expected value i.e. 0");

		// Mobile User Balance
		Response userBalanceResponse = pageObj.endpoints().Api2FetchUserBalance(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(userBalanceResponse.getStatusCode(), 200,
				"Status code 200 did not matched for api2 fetch user balance");
		String userBalanceInMobile = userBalanceResponse.jsonPath().get("account_balance.redeemable_points").toString();
		TestListeners.extentTest.get().info("API2 User Balance is successful");
		logger.info("API2 User Balance is successful");
		Assert.assertEquals(userBalanceInMobile, "0.0", "user balance is not equal to expected value i.e. 0");
		logger.info("user balance is equal to expected value i.e. 0");
		TestListeners.extentTest.get().pass("user balance is equal to expected value i.e. 0");

		// Mobile User Account History
		Response accountHistoryResponse = pageObj.endpoints().Api2UserAccountHistory(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(200, accountHistoryResponse.getStatusCode(),
				"Status code 200 did not matched for api2 point conversion");
		String userBalanceRewardInHistory = accountHistoryResponse.jsonPath().getString("[0].total_points");
		TestListeners.extentTest.get().info("API2 User Balance is successful");
		logger.info("API2 User Balance is successful");
		Assert.assertEquals(userBalanceRewardInHistory, "0",
				"user balance in account histroy is not equal to expected value i.e. 0");
		logger.info("user balance in account histroy is equal to expected value i.e. 0");
		TestListeners.extentTest.get().pass("user balance in account histroy is equal to expected value i.e. 0");

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUserWithEndDate(userID, dataSet.get("apiKey"),
				"", "", "", "150", "");
		logger.info("Send redeemable to the user successfully");
		TestListeners.extentTest.get().pass("Send redeemable to the user successfully");
		Assert.assertEquals(201, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		// Mobile User Balance
		Response userBalanceResponse1 = pageObj.endpoints().Api2FetchUserBalance(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(userBalanceResponse1.getStatusCode(), 200,
				"Status code 200 did not matched for api2 fetch user balance");
		String userBalanceInMobile1 = userBalanceResponse1.jsonPath().get("account_balance.redeemable_points")
				.toString();
		TestListeners.extentTest.get().info("API2 User Balance is successful");
		logger.info("API2 User Balance is successful");
		Assert.assertEquals(userBalanceInMobile1, "150.0", "user balance is not equal to expected value i.e. 0");
		logger.info("user balance is equal to expected value i.e. 0");
		TestListeners.extentTest.get().pass("user balance is equal to expected value i.e. 0");

		// Mobile User Account History
		Response accountHistoryResponse1 = pageObj.endpoints().Api2UserAccountHistory(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(200, accountHistoryResponse1.getStatusCode(),
				"Status code 200 did not matched for api2 point conversion");
		String userBalanceRewardInHistory1 = accountHistoryResponse1.jsonPath().getString("[0].total_points");
		TestListeners.extentTest.get().info("API2 User Balance is successful");
		logger.info("API2 User Balance is successful");
		Assert.assertEquals(userBalanceRewardInHistory1, "150",
				"user balance in account histroy is not equal to expected value i.e. 0");
		logger.info("user balance in account histroy is equal to expected value i.e. 0");
		TestListeners.extentTest.get().pass("user balance in account histroy is equal to expected value i.e. 0");
	}

	// Hardik Bhardwaj
	@Test(description = "SQ-T5437 Verify points should not set as negative in accounts table when run annual guest signup expiry", groups = {
			"unstable", "regression", "dailyrun" })
	@Owner(name = "Hardik Bhardwaj")
	public void T5437_guestReceiveTierBumpNotification() throws Exception {

		pageObj.sidekiqPage().sidekiqCheck(baseUrl);

		String b_id = dataSet.get("business_id");

		String businessPreferenceLiveQuery = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ b_id + "'";
		String businessPreferenceLiveExpColValue = DBUtils.executeQueryAndGetColumnValue(env,
				businessPreferenceLiveQuery, "preferences");

		boolean businessLiveFlag = DBUtils.updateBusinessesPreference(env,
				businessPreferenceLiveExpColValue, "false", "live", b_id);
		Assert.assertTrue(businessLiveFlag, "live value is not updated to false");
		logger.info("live value is updated to false");
		TestListeners.extentTest.get().info("live value is updated to false");

		boolean businessWentLIveflag = DBUtils.updateBusinessesPreference(env,
				businessPreferenceLiveExpColValue, "false", "went_live", b_id);
		Assert.assertTrue(businessWentLIveflag, "went_live value is not updated to false");
		logger.info("went_live value is updated to false");
		TestListeners.extentTest.get().info("went_live value is updated to false");

		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ dataSet.get("business_id") + "'";
		pageObj.singletonDBUtilsObj();
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");

		pageObj.singletonDBUtilsObj();
		// set value true
		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag"),
				dataSet.get("business_id"));
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to true");
		logger.info(dataSet.get("dbFlag") + " value is updated to true");
		TestListeners.extentTest.get().info(dataSet.get("dbFlag") + " value is updated to true");

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), 200, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("API2 Signup is successful");
		logger.info("API2 Signup is successful");

		// Do checkin of 320
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationKey"), "320");
		Assert.assertEquals(checkinResponse.getStatusCode(), 200, "Status code 200 did not match with POS Checkin ");
		TestListeners.extentTest.get().pass("POS checkin is successful for 320 dollar");
		logger.info("POS checkin is successful for 320 dollar");

		// force redemption of 10 points using api
		Response forceRedeem_Response = pageObj.endpoints().pointForceRedeem(dataSet.get("apiKey"), userID, "10");
		Assert.assertEquals(forceRedeem_Response.getStatusCode(), 201,
				"Status code 201 did not matched for pos redemption api");
		logger.info("Force redemption of 10 points is successful");
		TestListeners.extentTest.get().info("Force redemption of 10 points is successful");

		// Fetch Checkins
		Response fetchCheckinResponse = pageObj.endpoints().Api2FetchCheckin(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(fetchCheckinResponse.getStatusCode(), 200,
				"Status code 200 did not matched for api2 fetch checkin");
		String checkin_id = fetchCheckinResponse.jsonPath().get("[0].checkin_id").toString();

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
//		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		pageObj.earningPage().accountReEvaluationStrategy("Annually on Guest Signup Anniversary");
		pageObj.earningPage().updateConfiguration();
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		pageObj.earningPage().setExpiresAfter("clear", 0);
		pageObj.earningPage().expiryDateStrategy("Exact Days");
		pageObj.earningPage().membershipLevelResetStrategy("Reset Based On Points Earned");
		pageObj.dashboardpage().checkUncheckToggle("Decouple Checkin and Membership Expiry", "OFF");
		pageObj.earningPage().updateConfiguration();
		pageObj.earningPage().verifyMessage("Business was successfully updated.");

		String newcreated_at = CreateDateTime.previousYearDateAndTime(1);

		// DB query for user updation

		String query5 = "UPDATE `users` SET `created_at` = '" + newcreated_at + "' , `updated_at` = '" + newcreated_at
				+ "', `joined_at` = '" + newcreated_at + "', `last_activity_at`= '" + newcreated_at + "' WHERE id = '"
				+ userID + "';";
		pageObj.singletonDBUtilsObj();
		int rs = DBUtils.executeUpdateQuery(env, query5); // stmt.executeUpdate(query);
		Assert.assertEquals(rs, 1, "user table query is not working");
		logger.info("Users table updated successfully.");
		TestListeners.extentTest.get().info("Users table updated successfully.");

//		// DB query for account table updation
		String query6 = "Update `accounts` Set `created_at` = '" + newcreated_at + "' where `user_id` = '" + userID
				+ "';";
		pageObj.singletonDBUtilsObj();
		int rs1 = DBUtils.executeUpdateQuery(env, query6); // stmt.executeUpdate(query1);
		Assert.assertEquals(rs1, 1, "accouts table query for updating created_at is not working");
		logger.info("accouts table query for updating created_at is successfully.");
		TestListeners.extentTest.get().info("accouts table query for updating created_at is successfully.");

		// DB query for checkins updation
		String query7 = "UPDATE `checkins` SET `created_at` = '" + newcreated_at + "' , `updated_at` = '"
				+ newcreated_at + "' WHERE id = '" + checkin_id + "';";
		pageObj.singletonDBUtilsObj();
		int rs2 = DBUtils.executeUpdateQuery(env, query7); // stmt.executeUpdate(query1);
		Assert.assertEquals(rs2, 1, "checkins table query is not working");
		logger.info("checkins table updated successfully.");
		TestListeners.extentTest.get().info("checkins table updated successfully.");

		// Rolling expiry scheduling

		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().runRecallScheule();

		Utilities.longWait(4000);
		String query1 = "Select `business_id` from `expiry_events` where `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue1 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query1, "business_id",
				180); // dbUtils.getValueFromColumn(query3, "expire_item_type");
		Assert.assertEquals(expColValue1, dataSet.get("business_id"),
				"Entry is not created in expiry_events table in DB");
		logger.info("Entry is created in expiry_events table in DB");
		TestListeners.extentTest.get().pass("Entry is created in expiry_events table in DB");

		String query2 = "Select `total_debits` from `accounts` where `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue2 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query2, "total_debits",
				100); // dbUtils.getValueFromColumn(query3, "expire_item_type");
		Assert.assertEquals(expColValue2, "0.00",
				"total_debits column in accounts table is not equal to expected value i.e. 0");
		logger.info("total_debits column in accounts table is equal to expected value i.e. 0");
		TestListeners.extentTest.get().pass("total_debits column in accounts table is equal to expected value i.e. 0");

		String query3 = "Select `total_credits` from `accounts` where `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue3 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3, "total_credits",
				100); // dbUtils.getValueFromColumn(query3, "expire_item_type");
		Assert.assertEquals(expColValue3, "0.0",
				"total_credits column in accounts table is not equal to expected value i.e. 0");
		logger.info("total_credits column in accounts table is equal to expected value i.e. 0");
		TestListeners.extentTest.get().pass("total_credits column in accounts table is equal to expected value i.e. 0");

		// Mobile User Balance
		Response userBalanceResponse = pageObj.endpoints().Api2FetchUserBalance(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(userBalanceResponse.getStatusCode(), 200,
				"Status code 200 did not matched for api2 fetch user balance");
		String userBalanceInMobile = userBalanceResponse.jsonPath().get("account_balance.redeemable_points").toString();
		TestListeners.extentTest.get().info("API2 User Balance is successful");
		logger.info("API2 User Balance is successful");
		Assert.assertEquals(userBalanceInMobile, "0.0", "user balance is not equal to expected value i.e. 0");
		logger.info("user balance is equal to expected value i.e. 0");
		TestListeners.extentTest.get().pass("user balance is equal to expected value i.e. 0");

		// Mobile User Account History
		Response accountHistoryResponse = pageObj.endpoints().Api2UserAccountHistory(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(200, accountHistoryResponse.getStatusCode(),
				"Status code 200 did not matched for api2 point conversion");
		String userBalanceRewardInHistory = accountHistoryResponse.jsonPath().getString("[0].total_points");
		TestListeners.extentTest.get().info("API2 User Balance is successful");
		logger.info("API2 User Balance is successful");
		Assert.assertEquals(userBalanceRewardInHistory, "0",
				"user balance in account histroy is not equal to expected value i.e. 0");
		logger.info("user balance in account histroy is equal to expected value i.e. 0");
		TestListeners.extentTest.get().pass("user balance in account histroy is equal to expected value i.e. 0");

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUserWithEndDate(userID, dataSet.get("apiKey"),
				"", "", "", "150", "");
		logger.info("Send redeemable to the user successfully");
		TestListeners.extentTest.get().pass("Send redeemable to the user successfully");
		Assert.assertEquals(201, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		// Mobile User Balance
		Response userBalanceResponse1 = pageObj.endpoints().Api2FetchUserBalance(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(userBalanceResponse1.getStatusCode(), 200,
				"Status code 200 did not matched for api2 fetch user balance");
		String userBalanceInMobile1 = userBalanceResponse1.jsonPath().get("account_balance.redeemable_points")
				.toString();
		TestListeners.extentTest.get().info("API2 User Balance is successful");
		logger.info("API2 User Balance is successful");
		Assert.assertEquals(userBalanceInMobile1, "150.0", "user balance is not equal to expected value i.e. 0");
		logger.info("user balance is equal to expected value i.e. 0");
		TestListeners.extentTest.get().pass("user balance is equal to expected value i.e. 0");

		// Mobile User Account History
		Response accountHistoryResponse1 = pageObj.endpoints().Api2UserAccountHistory(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(200, accountHistoryResponse1.getStatusCode(),
				"Status code 200 did not matched for api2 point conversion");
		String userBalanceRewardInHistory1 = accountHistoryResponse1.jsonPath().getString("[0].total_points");
		TestListeners.extentTest.get().info("API2 User Balance is successful");
		logger.info("API2 User Balance is successful");
		Assert.assertEquals(userBalanceRewardInHistory1, "150",
				"user balance in account histroy is not equal to expected value i.e. 0");
		logger.info("user balance in account histroy is equal to expected value i.e. 0");
		TestListeners.extentTest.get().pass("user balance in account histroy is equal to expected value i.e. 0");
	}

	// Hardik Bhardwaj
	@Test(description = "SQ-T5436 Verify points should not set as negative in accounts table when run annual membership expiry", priority = 10, groups = {
			"regression", "unstable", "dailyrun" })
	@Owner(name = "Hardik Bhardwaj")
	public void T5436_guestReceiveTierBumpNotification() throws Exception {

		pageObj.sidekiqPage().sidekiqCheck(baseUrl);

		String b_id = dataSet.get("business_id");

		String businessPreferenceLiveQuery = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ b_id + "'";
		String businessPreferenceLiveExpColValue = DBUtils.executeQueryAndGetColumnValue(env,
				businessPreferenceLiveQuery, "preferences");

		boolean businessLiveFlag = DBUtils.updateBusinessesPreference(env,
				businessPreferenceLiveExpColValue, "false", "live", b_id);
		Assert.assertTrue(businessLiveFlag, "live value is not updated to false");
		logger.info("live value is updated to false");
		TestListeners.extentTest.get().info("live value is updated to false");

		boolean businessWentLIveflag = DBUtils.updateBusinessesPreference(env,
				businessPreferenceLiveExpColValue, "false", "went_live", b_id);
		Assert.assertTrue(businessWentLIveflag, "went_live value is not updated to false");
		logger.info("went_live value is updated to false");
		TestListeners.extentTest.get().info("went_live value is updated to false");

		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ dataSet.get("business_id") + "'";
		pageObj.singletonDBUtilsObj();
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");

		pageObj.singletonDBUtilsObj();
		// set value true
		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag"),
				dataSet.get("business_id"));
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to true");
		logger.info(dataSet.get("dbFlag") + " value is updated to true");
		TestListeners.extentTest.get().info(dataSet.get("dbFlag") + " value is updated to true");

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), 200, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("API2 Signup is successful");
		logger.info("API2 Signup is successful");

		// Do checkin of 320
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationKey"), "320");
		Assert.assertEquals(checkinResponse.getStatusCode(), 200, "Status code 200 did not match with POS Checkin ");
		TestListeners.extentTest.get().pass("POS checkin is successful for 320 dollar");
		logger.info("POS checkin is successful for 320 dollar");

		// force redemption of 10 points using api
		Response forceRedeem_Response = pageObj.endpoints().pointForceRedeem(dataSet.get("apiKey"), userID, "10");
		Assert.assertEquals(forceRedeem_Response.getStatusCode(), 201,
				"Status code 201 did not matched for pos redemption api");
		logger.info("Force redemption of 10 points is successful");
		TestListeners.extentTest.get().pass("Force redemption of 10 points is successful");

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		pageObj.earningPage().accountReEvaluationStrategy("Annually on Membership Level Bump Anniversary");
		pageObj.earningPage().updateConfiguration();
		pageObj.earningPage().navigateToEarningTabs("Checkin Expiry");
		pageObj.earningPage().setExpiresAfter("clear", 0);
		pageObj.earningPage().expiryDateStrategy("Exact Days");
		pageObj.earningPage().membershipLevelResetStrategy("Reset Based On Points Earned");
		pageObj.dashboardpage().checkUncheckToggle("Decouple Checkin and Membership Expiry", "OFF");
		pageObj.earningPage().updateConfiguration();
		pageObj.earningPage().verifyMessage("Business was successfully updated.");

		// update redeemption mark
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("RedeemptionMark"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// Fetch Checkins
		Response fetchCheckinResponse = pageObj.endpoints().Api2FetchCheckin(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(fetchCheckinResponse.getStatusCode(), 200,
				"Status code 200 did not matched for api2 fetch checkin");
		String checkin_id = fetchCheckinResponse.jsonPath().get("[0].checkin_id").toString();

		String newcreated_at = CreateDateTime.previousYearDateAndTime(1);

		// DB query for user updation

		String query4 = "UPDATE `users` SET `created_at` = '" + newcreated_at + "' , `updated_at` = '" + newcreated_at
				+ "', `joined_at` = '" + newcreated_at + "' WHERE id = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		int rs = DBUtils.executeUpdateQuery(env, query4); // stmt.executeUpdate(query);
		Assert.assertEquals(rs, 1, "user table query is not working");
		logger.info("Users table updated successfully.");
		TestListeners.extentTest.get().pass("Users table updated successfully.");

		// DB query for accounts updation

		String query5 = "UPDATE `accounts` SET `created_at` = '" + newcreated_at + "' , `updated_at` = '"
				+ newcreated_at + "' WHERE `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		int rs2 = DBUtils.executeUpdateQuery(env, query5); // stmt.executeUpdate(query2);
		Assert.assertEquals(rs2, 1, "accounts table query is not working");
		logger.info("Accounts table updated successfully.");
		TestListeners.extentTest.get().pass("Accounts table updated successfully.");

		// DB query for checkins updation
		String query6 = "UPDATE `checkins` SET `created_at` = '" + newcreated_at + "' , `updated_at` = '"
				+ newcreated_at + "' WHERE id = '" + checkin_id + "';";
		pageObj.singletonDBUtilsObj();
		int rs1 = DBUtils.executeUpdateQuery(env, query6); // stmt.executeUpdate(query1);
		Assert.assertEquals(rs1, 1, "checkins table query is not working");
		logger.info("checkins table updated successfully.");
		TestListeners.extentTest.get().pass("checkins table updated successfully.");

		// DB query for membership_level_histories updation
		String query7 = "UPDATE `membership_level_histories` SET `created_at` = '" + newcreated_at
				+ "' , `updated_at` = '" + newcreated_at + "' WHERE `user_id` = '" + userID + "' ;";
		pageObj.singletonDBUtilsObj();
		int rs4 = DBUtils.executeUpdateQuery(env, query7); // stmt.executeUpdate(query4);
		Assert.assertEquals(rs4, 2, "membership_level_histories table query is not working");
		logger.info("membership_level_histories table updated successfully.");
		TestListeners.extentTest.get().pass("membership_level_histories table updated successfully.");

		String newcreated_at1 = CreateDateTime.previousYearDate(0);
		String membershipLevelId = dataSet.get("membershipLevelId");

		// DB query for membership_level_histories updation
		String query8 = "UPDATE `membership_level_histories` SET `scheduled_expiry_on` = '" + newcreated_at1
				+ "' WHERE `user_id` = '" + userID + "' AND `membership_level_id` = '" + membershipLevelId + "' ;";
		pageObj.singletonDBUtilsObj();
		int rs5 = DBUtils.executeUpdateQuery(env, query8); // stmt.executeUpdate(query5);
		Assert.assertEquals(rs5, 1, "membership_level_histories table query is not working");
		logger.info("membership_level_histories table updated successfully.");
		TestListeners.extentTest.get().pass("membership_level_histories table updated successfully.");

		// Rolling expiry scheduling
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().runRecallScheule();

		utils.longWaitInSeconds(8);
		String query1 = "Select `business_id` from `expiry_events` where `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue1 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query1, "business_id",
				200); // dbUtils.getValueFromColumn(query3, "expire_item_type");
		Assert.assertEquals(expColValue1, dataSet.get("business_id"),
				"Entry is not created in expiry_events table in DB");
		logger.info("Entry is created in expiry_events table in DB");
		TestListeners.extentTest.get().pass("Entry is created in expiry_events table in DB");

		String query2 = "Select `total_debits` from `accounts` where `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue2 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query2, "total_debits",
				100); // dbUtils.getValueFromColumn(query3, "expire_item_type");
		Assert.assertEquals(expColValue2, "0.00",
				"total_debits column in accounts table is not equal to expected value i.e. 0");
		logger.info("total_debits column in accounts table is equal to expected value i.e. 0");
		TestListeners.extentTest.get().pass("total_debits column in accounts table is equal to expected value i.e. 0");

		String query3 = "Select `total_credits` from `accounts` where `user_id` = '" + userID + "';";
		pageObj.singletonDBUtilsObj();
		String expColValue3 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query3, "total_credits",
				100); // dbUtils.getValueFromColumn(query3, "expire_item_type");
		Assert.assertEquals(expColValue3, "0.0",
				"total_credits column in accounts table is not equal to expected value i.e. 0");
		logger.info("total_credits column in accounts table is equal to expected value i.e. 0");
		TestListeners.extentTest.get().pass("total_credits column in accounts table is equal to expected value i.e. 0");

		// Mobile User Balance
		Response userBalanceResponse = pageObj.endpoints().Api2FetchUserBalance(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(userBalanceResponse.getStatusCode(), 200,
				"Status code 200 did not matched for api2 fetch user balance");
		String userBalanceInMobile = userBalanceResponse.jsonPath().get("account_balance.redeemable_points").toString();
		TestListeners.extentTest.get().pass("API2 User Balance is successful");
		logger.info("API2 User Balance is successful");
		Assert.assertEquals(userBalanceInMobile, "0.0", "user balance is not equal to expected value i.e. 0");
		logger.info("user balance is equal to expected value i.e. 0");
		TestListeners.extentTest.get().pass("user balance is equal to expected value i.e. 0");

		// Mobile User Account History
		Response accountHistoryResponse = pageObj.endpoints().Api2UserAccountHistory(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(200, accountHistoryResponse.getStatusCode(),
				"Status code 200 did not matched for api2 point conversion");
		String userBalanceRewardInHistory = accountHistoryResponse.jsonPath().getString("[0].total_points");
		TestListeners.extentTest.get().pass("API2 User Balance is successful");
		logger.info("API2 User Balance is successful");
		Assert.assertEquals(userBalanceRewardInHistory, "0",
				"user balance in account histroy is not equal to expected value i.e. 0");
		logger.info("user balance in account histroy is equal to expected value i.e. 0");
		TestListeners.extentTest.get().pass("user balance in account histroy is equal to expected value i.e. 0");

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUserWithEndDate(userID, dataSet.get("apiKey"),
				"", "", "", "150", "");
		logger.info("Send redeemable to the user successfully");
		TestListeners.extentTest.get().pass("Send redeemable to the user successfully");
		Assert.assertEquals(201, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		// Mobile User Balance
		Response userBalanceResponse1 = pageObj.endpoints().Api2FetchUserBalance(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(userBalanceResponse1.getStatusCode(), 200,
				"Status code 200 did not matched for api2 fetch user balance");
		String userBalanceInMobile1 = userBalanceResponse1.jsonPath().get("account_balance.redeemable_points")
				.toString();
		TestListeners.extentTest.get().pass("API2 User Balance is successful");
		logger.info("API2 User Balance is successful");
		Assert.assertEquals(userBalanceInMobile1, "150.0", "user balance is not equal to expected value i.e. 0");
		logger.info("user balance is equal to expected value i.e. 0");
		TestListeners.extentTest.get().pass("user balance is equal to expected value i.e. 0");

		// Mobile User Account History
		Response accountHistoryResponse1 = pageObj.endpoints().Api2UserAccountHistory(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(200, accountHistoryResponse1.getStatusCode(),
				"Status code 200 did not matched for api2 point conversion");
		String userBalanceRewardInHistory1 = accountHistoryResponse1.jsonPath().getString("[0].total_points");
		TestListeners.extentTest.get().pass("API2 User Balance is successful");
		logger.info("API2 User Balance is successful");
		Assert.assertEquals(userBalanceRewardInHistory1, "150",
				"user balance in account histroy is not equal to expected value i.e. 0");
		logger.info("user balance in account histroy is equal to expected value i.e. 0");
		TestListeners.extentTest.get().pass("user balance in account histroy is equal to expected value i.e. 0");
	}



	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}

}