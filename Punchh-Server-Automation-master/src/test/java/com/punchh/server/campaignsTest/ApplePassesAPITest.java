package com.punchh.server.campaignsTest;

import org.testng.annotations.Test;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.Listeners;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.apiTest.MobileApiTest3;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.SingletonDBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)

public class ApplePassesAPITest {
	private static Logger logger = LogManager.getLogger(MobileApiTest3.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String userEmail;
	private String sTCName;
	private String env, run = "api";
	private static Map<String, String> dataSet;
	private Utilities utils;

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
		utils = new Utilities();
		sTCName = method.getName();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		logger.info(sTCName + " ==>" + dataSet);
		pageObj.singletonDBUtilsObj();
	}

	@Test(description = "SQ-T3802_INT2-974 | Add 'apple_pass_url' attribute to the user object in api2/mobile", groups = "api", priority = 1)
	public void SQ_T3802_verify_applePassURL_Api2_Test() {
		// User register/signup using API2 Signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("Api2 user signup is successful ");

		// Verify apple pass for 200
		String apple_pass_url_signUp = signUpResponse.jsonPath().get("user.apple_pass_url");
		logger.info("Apple pass URL SignUp==> " + apple_pass_url_signUp);
		Assert.assertEquals(pageObj.endpoints().headRequest(apple_pass_url_signUp).getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Invalid apple pass url");
		TestListeners.extentTest.get().pass("apple_pass_url sign up verification is successful");

		// User login using API2 Signin
		Response loginResponse = pageObj.endpoints().Api2Login(userEmail, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(loginResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 login");
		String token = loginResponse.jsonPath().get("access_token.token").toString();
		TestListeners.extentTest.get().pass("Api2 user Login is successful ");
		String apple_pass_url_login = loginResponse.jsonPath().get("user.apple_pass_url");

		// Verify apple pass url are identical
		logger.info("Apple pass URL Login==> " + apple_pass_url_login);
		Assert.assertEquals(apple_pass_url_login, apple_pass_url_signUp);
		TestListeners.extentTest.get().pass("apple_pass_url sign in verification is successful");

		// Fetch user information
		Response userInfoResponse = pageObj.endpoints().Api2FetchUserInfo(token, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, userInfoResponse.getStatusCode(),
				"Status code 200 did not matched for api2 fetch user info");
		TestListeners.extentTest.get().pass("Api2 fetch user information is successful");
		String apple_pass_url_fetch = userInfoResponse.jsonPath().get("user.apple_pass_url");

		// Verify apple pass url are identical
		logger.info("Apple pass URL fetch ==> " + apple_pass_url_fetch);
		Assert.assertEquals(apple_pass_url_fetch, apple_pass_url_signUp);
		TestListeners.extentTest.get().pass("apple_pass_url fetch verification is successful");

		// Update user profile
		Response updateUserInfoResponse = pageObj.endpoints().Api2UpdateUserProfile(dataSet.get("client"), userEmail,
				dataSet.get("secret"), token);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, updateUserInfoResponse.getStatusCode(),
				"Status code 200 did not matched for api2 update user info");
		TestListeners.extentTest.get().pass("Api2 update user information is successful");
		String apple_pass_url_update_user = updateUserInfoResponse.jsonPath().get("user.apple_pass_url");

		// Verify apple pass url are identical
		logger.info("Apple pass URL user update ==> " + apple_pass_url_update_user);
		Assert.assertEquals(apple_pass_url_update_user, apple_pass_url_signUp);
		TestListeners.extentTest.get().pass("apple_pass_url user update verification is successful");
	}

	@Test(description = "SQ-T3799 (1.0) INT2-839 | PunchhCron | UnownedApplePassesDeleteWorker | Delete entries with null in the source for Apple pass")
	public void SQ_T3799_verifyUnownedApplePassesDeleteWorkerTest() throws Exception {
		Integer minimumRecordCount = 5;

		// Case 1: Insert Apple Passes older than 90 days with null owner
		utils.logit("Inserting Apple Passes older than 90 days with null owner.");
		insertApplePasses(minimumRecordCount, 100, true, null, null);
		String queryApplePassOlderThan90DaysWithNullOwner = "SELECT COUNT(*) AS count FROM apple_passes WHERE created_at < NOW() - INTERVAL 90 DAY AND owner_id IS NULL AND owner_type IS NULL";
		verifyRecordCount(queryApplePassOlderThan90DaysWithNullOwner, minimumRecordCount);
		String applePassIdOlderThan90DaysWithNullOwner = getSingleColumnValueFromQuery(
				"SELECT id FROM apple_passes WHERE created_at < NOW() - INTERVAL 90 DAY AND owner_id IS NULL AND owner_type IS NULL limit 1",
				"id");
		insertApplePassRegistrationsForPass(minimumRecordCount, applePassIdOlderThan90DaysWithNullOwner);
		String queryApplePassRegistrationCount1 = "SELECT COUNT(*) AS count FROM apple_pass_registrations WHERE apple_pass_id = "
				+ applePassIdOlderThan90DaysWithNullOwner;
		verifyRecordCount(queryApplePassRegistrationCount1, minimumRecordCount);

		// Case 2: Insert Apple Passes within 90 days with null owner
		utils.logit("Inserting Apple Passes within 90 days with null owner.");
		insertApplePasses(minimumRecordCount, 10, false, null, null);
		String queryApplePassWithin90DaysWithNullOwner = "SELECT COUNT(*) AS count FROM apple_passes WHERE created_at > NOW() - INTERVAL 90 DAY AND owner_id IS NULL AND owner_type IS NULL";
		verifyRecordCount(queryApplePassWithin90DaysWithNullOwner, minimumRecordCount);
		String applePassIdWithin90DaysWithNullOwner = getSingleColumnValueFromQuery(
				"SELECT id FROM apple_passes WHERE created_at > NOW() - INTERVAL 90 DAY AND owner_id IS NULL AND owner_type IS NULL limit 1",
				"id");
		insertApplePassRegistrationsForPass(minimumRecordCount, applePassIdWithin90DaysWithNullOwner);
		String queryApplePassRegistrationCount2 = "SELECT COUNT(*) AS count FROM apple_pass_registrations WHERE apple_pass_id = "
				+ applePassIdWithin90DaysWithNullOwner;
		verifyRecordCount(queryApplePassRegistrationCount2, minimumRecordCount);

		// Case 3: Insert Apple Passes older than 90 days with non-null owner
		utils.logit("Inserting Apple Passes older than 90 days with non-null owner.");
		insertApplePasses(minimumRecordCount, 100, true, 100, "User");
		String queryApplePassOlderThan90DaysWithNonNullOwner = "SELECT COUNT(*) AS count FROM apple_passes WHERE created_at < NOW() - INTERVAL 90 DAY AND owner_id IS NOT NULL AND owner_type IS NOT NULL";
		verifyRecordCount(queryApplePassOlderThan90DaysWithNonNullOwner, minimumRecordCount);
		String applePassIdOlderThan90DaysWithNonNullOwner = getSingleColumnValueFromQuery(
				"SELECT id FROM apple_passes WHERE created_at < NOW() - INTERVAL 90 DAY AND owner_id IS NOT NULL AND owner_type IS NOT NULL limit 1",
				"id");
		insertApplePassRegistrationsForPass(minimumRecordCount, applePassIdOlderThan90DaysWithNonNullOwner);
		String queryApplePassRegistrationCount3 = "SELECT COUNT(*) AS count FROM apple_pass_registrations WHERE apple_pass_id = "
				+ applePassIdOlderThan90DaysWithNonNullOwner;
		verifyRecordCount(queryApplePassRegistrationCount3, minimumRecordCount);

		// Case 4: Insert Apple Passes within 90 days with non-null owner
		utils.logit("Inserting Apple Passes within 90 days with non-null owner.");
		insertApplePasses(minimumRecordCount, 10, false, 100, "User");
		String queryApplePassWithin90DaysWithNonNullOwner = "SELECT COUNT(*) AS count FROM apple_passes WHERE created_at > NOW() - INTERVAL 90 DAY AND owner_id IS NOT NULL AND owner_type IS NOT NULL";
		verifyRecordCount(queryApplePassWithin90DaysWithNonNullOwner, minimumRecordCount);
		String applePassIdWithin90DaysWithNonNullOwner = getSingleColumnValueFromQuery(
				"SELECT id FROM apple_passes WHERE created_at > NOW() - INTERVAL 90 DAY AND owner_id IS NOT NULL AND owner_type IS NOT NULL limit 1",
				"id");
		insertApplePassRegistrationsForPass(minimumRecordCount, applePassIdWithin90DaysWithNonNullOwner);
		String queryApplePassRegistrationCount4 = "SELECT COUNT(*) AS count FROM apple_pass_registrations WHERE apple_pass_id = "
				+ applePassIdWithin90DaysWithNonNullOwner;
		verifyRecordCount(queryApplePassRegistrationCount4, minimumRecordCount);

		utils.logit("Pass", "Apple Passes inserted for all test cases. Now, triggering the cron job.");

		// Execute the cron job to delete unowned Apple Passes
		Response resp = pageObj.endpoints().enqueueWorker("UnownedApplePassesDeleteWorker");
		Assert.assertEquals(resp.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Incorrect status code");

		utils.longWaitInSeconds(20);
		utils.logit("Starting post-cron validation.");

		// Post-cron validation: Check record counts for each query after cron job
		// execution
		utils.logit("Verifying record counts after cron job.");
		verifyRecordCount(queryApplePassOlderThan90DaysWithNullOwner, 0);
		verifyRecordCount(queryApplePassWithin90DaysWithNullOwner, minimumRecordCount);
		verifyRecordCount(queryApplePassOlderThan90DaysWithNonNullOwner, minimumRecordCount);
		verifyRecordCount(queryApplePassWithin90DaysWithNonNullOwner, minimumRecordCount);

		verifyRecordCount(queryApplePassRegistrationCount1, 0);
		verifyRecordCount(queryApplePassRegistrationCount2, minimumRecordCount);
		verifyRecordCount(queryApplePassRegistrationCount3, minimumRecordCount);
		verifyRecordCount(queryApplePassRegistrationCount4, minimumRecordCount);

		utils.logit("Pass", "Test completed: Apple Pass deletion after cron job validated successfully.");
	}

	// Helper Methods
	private void insertApplePasses(Integer count, Integer daysOffset, boolean isOlder, Integer ownerId,
			String ownerType) throws Exception {
		StringBuilder query = new StringBuilder(
				"INSERT INTO apple_passes (apple_pass_design_id,business_id,owner_id,owner_type,serial_number,`type`,pass_type_identifier,pass_auth_token,created_at,updated_at,source) VALUES ");
		for (int i = 1; i <= count; i++) {
			query.append(String.format(
					"(5000, 562, %s, %s, UUID(), NULL, 'manually_created_entry', UUID(), NOW() - INTERVAL %d DAY, NOW() - INTERVAL %d DAY, 'automation')%s",
					ownerId != null ? ownerId : "NULL", ownerType != null ? "'" + ownerType + "'" : "NULL", daysOffset,
					daysOffset, (i < count) ? ", " : ""));
		}
		DBUtils.executeQuery(env, query.toString());
		utils.logit("Inserted " + count + " Apple Passes.");
	}

	private void insertApplePassRegistrationsForPass(Integer count, String applePassId) throws Exception {
		Assert.assertNotNull(applePassId, "Apple Pass ID is null");
		StringBuilder query = new StringBuilder(
				"INSERT INTO apple_pass_registrations (apple_pass_id,business_id,apple_pass_device_id,pass_type_identifier,created_at,updated_at) VALUES ");
		for (int i = 1; i <= count; i++) {
			query.append(String.format("(%s, 562, 100, 'automation_created_entry', NOW(), NOW())%s", applePassId,
					(i < count) ? ", " : ""));
		}
		DBUtils.executeQuery(env, query.toString());
		utils.logit("Inserted " + count + " Apple Pass Registrations for Apple Pass ID: " + applePassId);
	}

	private String getSingleColumnValueFromQuery(String query, String columnName) throws Exception {
		return DBUtils.executeQueryAndGetColumnValue(env, query, columnName);
	}

	// Count greater than expected count if not 0
	private void verifyRecordCount(String query, Integer expectedCount) throws Exception {
		String count = DBUtils.executeQueryAndGetColumnValue(env, query, "count");
		utils.logit("Record count: " + count);
		if (expectedCount != 0)
			Assert.assertTrue(Integer.parseInt(count) >= expectedCount, "Record count mismatch for query.");
		else
			Assert.assertEquals(Integer.parseInt(count), 0, "Record count mismatch for query.");
		utils.logit("Pass", "Record count verified, expected count: " + expectedCount + " actual count: " + count);
	}
}