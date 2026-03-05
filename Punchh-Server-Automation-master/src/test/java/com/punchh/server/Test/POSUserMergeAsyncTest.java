package com.punchh.server.Test;

import java.lang.reflect.Method;
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
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class POSUserMergeAsyncTest {
	public WebDriver driver;
	private PageObj pageObj;
	private String sTCName;
	private String baseUrl;
	private String env, run = "ui";
	private static Map<String, String> dataSet;
	private Utilities utils;

	@BeforeClass(alwaysRun = true)
	public void openBrowser() {
		driver = new BrowserUtilities().launchBrowser();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		// Single Login to instance
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
		// Put without overwriting existing keys
		pageObj.readData().readTestData.forEach((key, value) -> dataSet.putIfAbsent(key, value));
		utils = new Utilities(driver);
		utils.logit(sTCName + " ==>" + dataSet);
		// Move to All businesses page
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
	}
	
	@Test(description = "SQ-T3960,T5788 INT2-953 | 1036 | 1039 - POS user merge async | INT2-1910 | POS user merge case , api giving 422 error 'phone has already been taken'", groups = {
			"regression", "dailyrun" }, priority = 0)
	@Owner(name = "Nipun Jain")
	public void T3960_T5788_POSUserMergeAsync() throws Exception {

		// Create a POS user via signup API
		long phone = (long) (Math.random() * Math.pow(10, 10));
		Response respo = pageObj.endpoints().posSignUpWithoutEmail(phone, dataSet.get("locationKey"));
		Assert.assertEquals(200, respo.getStatusCode(), "Status code 200 did not matched for pos signup api");
		String posUserEmail = respo.jsonPath().get("email").toString();
		String posUserId = respo.jsonPath().get("id").toString();
		utils.logPass("POS signup with phone only is successful");

		// Pre-conditions for POS user merge
		//pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		//pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// whitelabel >> iframe configuration >> basic configuration
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "iFrame Configuration");
		pageObj.dashboardpage().navigateToTabs("Basic Configuration");
		utils.setCheckboxStateViaCheckBoxText("Accept Phone Number?");
		utils.setCheckboxStateViaCheckBoxText("Enable Phone Number as a mandatory field?");
		utils.setCheckboxStateViaCheckBoxText("Accept minimum 10 digit Phone Number?");
		pageObj.dashboardpage().updateCheckBox();

		// Cockpit -> Guest -> Guest validation
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().navigateToTabs("Guest Validation");
		utils.setCheckboxStateViaCheckBoxText("Validate uniqueness of phone number across guests?");
		utils.setCheckboxStateViaCheckBoxText("Use parsed phone number for guests?");
		pageObj.dashboardpage().updateCheckBox();

		// Cockpit -> Pos Integration
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "POS Integration");
		utils.setCheckboxStateViaCheckBoxText(
				"Create a new guest based on phone number if the guest doesn't exist (via POS/SMS)?");
		pageObj.dashboardpage().updateCheckBox();

		// Pos API checkin - 1
		String key = CreateDateTime.getTimeDateString() + "324";
		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T10:50:00+05:30";
		Response resp = pageObj.endpoints().posCheckin(date, posUserEmail, key, txn, dataSet.get("locationKey"));
		Assert.assertEquals(200, resp.getStatusCode(), "Status code 200 did not matched for pos chekin api");
		Assert.assertEquals(resp.jsonPath().get("email").toString(), posUserEmail.toLowerCase());
		utils.logit("Pass", "First POS checkin for user with phone only was successful");

		// Pos api checkin - 2
		String key1 = CreateDateTime.getTimeDateString();
		String txn1 = "123456" + CreateDateTime.getTimeDateString();
		String date1 = CreateDateTime.getCurrentDate() + "T10:50:00+05:30";
		Response resp1 = pageObj.endpoints().posCheckin(date1, posUserEmail, key1, txn1, dataSet.get("locationKey"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp1.getStatusCode(), "Status code 200 did not matched for pos chekin api");
		Assert.assertEquals(resp1.jsonPath().get("email").toString(), posUserEmail.toLowerCase());
		String total_credits = resp1.jsonPath().get("balance.total_credits").toString();
		utils.logit("Pass", "Second POS checkin for user with phone only was successful");

		// Mobile Sign-up
		String mobileUserEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(mobileUserEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		utils.logit("Pass", "Mobile sign up is successful");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String mobileUserId = signUpResponse.jsonPath().get("user.user_id").toString();

		// Update user profile using API1
		String phoneStr = Long.toString(phone);

		Response updateGuestResponse = pageObj.endpoints().Api1MobileUpdateGuestDetailsWithoutEmail(dataSet.get("Npwd"),
				phoneStr, dataSet.get("client"), dataSet.get("secret"), token);
		Assert.assertEquals(updateGuestResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 signup");
		utils.logit("Pass", "Update user profile using API1 is successful");

		Thread.sleep(8000);

		String mergeQuery = "select merged_user_id from loyalty_pos_users where loyalty_user_id = '" + mobileUserId
				+ "'";
		pageObj.singletonDBUtilsObj();
		String mergedUserId = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, mergeQuery,
				"merged_user_id", 10);
		Assert.assertEquals(mergedUserId, posUserId, "merged_user_id is not equal to expected user_id ");
		utils.logit("Pass", "Merged user id " + mergedUserId + " matches the POS user id " + posUserId);

		// Validate POS (phone-only) User Deletion from DB
		String userDeletionQuery = "select count(*) as count from users where id = '" + posUserId + "'";
		boolean isUserDeleted = DBUtils.verifyValueFromDBUsingPolling(env, userDeletionQuery,
				"count", "0");
		Assert.assertTrue(isUserDeleted, "pos(phone) User is not deleted from DB");
		utils.logit("Pass", "POS (phone-only) User is deleted from DB");

		// Validate checkins moved to loyalty(Email) user from POS(mobile) user
		String checkinQueryOldUser = "select count(*) as count from checkins where user_id = '" + posUserId
				+ "' and channel ='POS'";
		boolean checkinsNotPresentOld = DBUtils.verifyValueFromDBUsingPolling(env,
				checkinQueryOldUser, "count", "0");
		Assert.assertTrue(checkinsNotPresentOld, "Checkins are not moved to loyalty(Email) user from POS(mobile) user");

		String checkinQueryMobileUser = "select count(*) as count from checkins where user_id = '" + mobileUserId
				+ "' and channel ='POS'";
		boolean checkinsPresentMobile = DBUtils.verifyValueFromDBUsingPolling(env,
				checkinQueryMobileUser, "count", "2");
		Assert.assertTrue(checkinsPresentMobile, "Checkins are not moved to loyalty(Email) user from POS(mobile) user");
		utils.logit("Pass", "Checkins are moved to loyalty(Email) user from POS(mobile) user");

		// Validate points/gifts moved to loyalty(Email) user from POS(mobile) user
		String pointsQueryOldUser = "select count(*) as count from accounts where user_id = '" + posUserId + "'";
		boolean pointsNotPresentOld = DBUtils.verifyValueFromDBUsingPolling(env,
				pointsQueryOldUser, "count", "0");
		Assert.assertTrue(pointsNotPresentOld,
				"Points/gifts are not moved to loyalty(Email) user from POS(mobile) user");

		String pointsQueryMobileUser = "select count(*) as count,total_credits from accounts where user_id = '"
				+ mobileUserId + "'";
		boolean accountRecordExists = DBUtils.verifyValueFromDBUsingPolling(env,
				pointsQueryMobileUser, "count", "1");
		boolean creditsMatch = DBUtils.verifyValueFromDBUsingPolling(env, pointsQueryMobileUser,
				"total_credits", total_credits);
		Assert.assertTrue(accountRecordExists,
				"Points/gifts are not moved to loyalty(Email) user from POS(mobile) user");
		Assert.assertTrue(creditsMatch, "Points/gifts are not moved to loyalty(Email) user from POS(mobile) user");
		utils.logit("Pass", "Points/gifts are moved to loyalty(Email) user from POS(mobile) user");

		// Validate POS user phone number moved to loyalty user
		String phoneQuery = "select count(*) as count, id from users where phone = '" + phone + "'";
		boolean phoneRecordExists = DBUtils.verifyValueFromDBUsingPolling(env, phoneQuery,
				"count", "1");
		boolean phoneUserLinked = DBUtils.verifyValueFromDBUsingPolling(env, phoneQuery, "id",
				mobileUserId);
		Assert.assertTrue(phoneRecordExists, "POS user phone number is not moved to loyalty user");
		Assert.assertTrue(phoneUserLinked, "POS user phone number is not moved to loyalty user");
		utils.logit("Pass", "POS user phone number is moved to loyalty user");
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		utils.logit("Data set cleared");
	}

	@AfterClass(alwaysRun = true)
	public void closeBrowser() {
		driver.quit();
		utils.logit("Browser closed");
	}
}