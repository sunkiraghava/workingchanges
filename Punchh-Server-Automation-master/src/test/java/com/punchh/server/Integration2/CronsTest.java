package com.punchh.server.Integration2;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.annotations.Owner;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.MessagesConstants;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class CronsTest {
	public WebDriver driver;
	private PageObj pageObj;
	private String sTCName, env;
	private String run = "ui";
	private static Map<String, String> dataSet;
	private Utilities utils;
	private String guestIdentityhost = "guestIdentity";
	private String baseUrl;

	@BeforeMethod(alwaysRun = true)
	public void beforeMethod(Method method) throws Exception {
		utils = new Utilities();
		driver = new BrowserUtilities().launchBrowser();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		utils.logit("Using env as ==> " + env);
		sTCName = method.getName();
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run , env), sTCName);
		dataSet = pageObj.readData().readTestData;
		utils.logit("Dataset for " + sTCName + " ==> " + dataSet);
		pageObj.singletonDBUtilsObj();
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
	}

	@Test(description = "SQ_T4255 INT2-1015 | PunchhCron | Verify deletion of expired (7 day+) oauth_access_tokens via cron",groups = {"nonNightly" })
	@Owner(name ="Nipun Jain")
	public void SQ_T4255_AuthServiceAccessTokensDeleteWorkerCronTest() throws Exception {
		int minRecordCount = 50;
		String applicationId = dataSet.get("applicationId");

		// Query for expired tokens with auth_service scope (should be deleted)
		String queryExpiredAuthService = "SELECT count(*) as count FROM oauth_access_tokens "
				+ "WHERE scopes = 'auth_service' AND created_at <= NOW() - INTERVAL 7 DAY "
				+ "AND application_id IN (SELECT id FROM oauth_applications WHERE scopes = 'auth_service' "
				+ "AND owner_id IN (SELECT id FROM businesses WHERE preferences NOT LIKE '%external_source_id_as_access_token: true%'))";

		// Query for expired tokens with non-auth_service scope (should remain)
		String queryExpiredNonAuth = "SELECT count(*) as count FROM oauth_access_tokens "
				+ "WHERE scopes != 'auth_service' AND created_at <= NOW() - INTERVAL 7 DAY "
				+ "AND application_id IN (SELECT id FROM oauth_applications WHERE scopes = 'auth_service' "
				+ "AND owner_id IN (SELECT id FROM businesses WHERE preferences NOT LIKE '%external_source_id_as_access_token: true%'))";

		// Query for new tokens with auth_service scope (should remain)
		String queryNewAuthService = "SELECT count(*) as count FROM oauth_access_tokens "
				+ "WHERE scopes = 'auth_service' AND created_at >= NOW() - INTERVAL 7 DAY "
				+ "AND application_id IN (SELECT id FROM oauth_applications WHERE scopes = 'auth_service' "
				+ "AND owner_id IN (SELECT id FROM businesses WHERE preferences NOT LIKE '%external_source_id_as_access_token: true%'))";

		// Get current counts from the DB (we now use verifyRecordCount later)
		int expiredAuthServiceCount = Integer
				.parseInt(DBUtils.executeQueryAndGetColumnValue(env, queryExpiredAuthService, "count"));
		int expiredNonAuthCount = Integer
				.parseInt(DBUtils.executeQueryAndGetColumnValue(env, queryExpiredNonAuth, "count"));
		int newAuthServiceCount = Integer
				.parseInt(DBUtils.executeQueryAndGetColumnValue(env, queryNewAuthService, "count"));

		utils.logit("Initial Expired Auth Service Count: " + expiredAuthServiceCount);
		utils.logit("Initial Expired Non-Auth Count: " + expiredNonAuthCount);
		utils.logit("Initial New Auth Service Count: " + newAuthServiceCount);

		// Ensure we have enough expired auth_service tokens (to be deleted)
		if (expiredAuthServiceCount < minRecordCount) {
			utils.logit("Less than " + minRecordCount + " expired auth_service records found. Inserting test records.");
			insertTokensOauthAccessTokens(env, applicationId, "auth_service", minRecordCount,
					"NOW() - INTERVAL 10 DAY");
			verifyRecordCount(queryExpiredAuthService, minRecordCount);
			utils.logPass("Expired auth_service test records created successfully.");
		} else {
			utils.logit("Expired auth_service tokens found. Proceeding with cron validation.");
		}

		// Ensure we have enough expired non-auth_service tokens (should remain)
		if (expiredNonAuthCount < minRecordCount) {
			utils.logit(
					"Less than " + minRecordCount + " expired non-auth_service records found. Inserting test records.");
			insertTokensOauthAccessTokens(env, applicationId, "wifi", minRecordCount, "NOW() - INTERVAL 10 DAY");
			verifyRecordCount(queryExpiredNonAuth, minRecordCount);
			utils.logPass("Expired non-auth_service test records created successfully.");
		} else {
			utils.logit("Expired non-auth_service tokens found. Proceeding with cron validation.");
		}

		// Ensure we have enough new auth_service tokens (should remain)
		if (newAuthServiceCount < minRecordCount) {
			utils.logit("Less than " + minRecordCount + " new auth_service records found. Inserting test records.");
			insertTokensOauthAccessTokens(env, applicationId, "auth_service", minRecordCount, "NOW()");
			verifyRecordCount(queryNewAuthService, minRecordCount);
			utils.logPass("New auth_service test records created successfully.");
		} else {
			utils.logit("New auth_service tokens found. Proceeding with cron validation.");
		}

		// Execute the cron job to delete expired oauth_access_tokens with auth_service scope
		Response resp = pageObj.endpoints().enqueueWorker("AuthServiceAccessTokensDeleteWorker");
		Assert.assertEquals(resp.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Incorrect status code from cron job");

		utils.longWaitInSeconds(20);
		utils.logit("Starting post-cron validation.");

		// Validate that expired auth_service tokens have been deleted (using polling to wait for deletion)
		verifyRecordCount(queryExpiredAuthService, 0);
		utils.logit("Pass", "Expired auth_service tokens successfully deleted.");

		// Validate that new auth_service tokens remain
		verifyRecordCount(queryNewAuthService, minRecordCount);
		utils.logit("Pass", "New auth_service tokens were not deleted.");

		// Validate that expired non-auth_service tokens remain
		verifyRecordCount(queryExpiredNonAuth, minRecordCount);
		utils.logit("Pass", "Expired non-auth_service tokens were not deleted.");
	}

	@Test(description = "SQ_T4395 INT2-1116 | PunchhCron | Validate OauthApplicationDeleteWorker",groups = {"nonNightly" })
	@Owner(name ="Nipun Jain")
	public void SQ_T4395_OauthAppDeleteWorkerCronPunchhTest() throws Exception {
		int minRecordCount = 50;
		
		// Oauth app creation and deletion
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "OAuth Apps");
		pageObj.oAuthAppPage().clickNewApplication();
		String oauthAppName = "AutoTest Oauth App " + CreateDateTime.getTimeDateAsneed("yyyyMMddHHmmss");
		pageObj.oAuthAppPage().enterAppName(oauthAppName);
		pageObj.oAuthAppPage().enterRedirectUri(dataSet.get("redirectUri"));
		pageObj.oAuthAppPage().selectScope("Wifi");
		pageObj.oAuthAppPage().clickSave();
		pageObj.oAuthAppPage().verifyDisplayedMessage(MessagesConstants.oauthAppCreateSuccessMsg);
		utils.logit(oauthAppName + " created successfully");

		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "OAuth Apps");
		pageObj.oAuthAppPage().deleteOauthAppByName(oauthAppName);

		pageObj.oAuthAppPage().verifyDisplayedMessage(MessagesConstants.oauthAppDeleteSuccessMsg);
		utils.logit(oauthAppName + " deleted successfully");

		// Verify inactive app in Punchh DB
		String query = "select id from oauth_applications where name = '"+ oauthAppName + "' and is_active = 0";
		String appId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");
		Assert.assertNotNull(appId, "OauthApp is not created or inactive in Punchh DB");
		utils.logPass("Verified OAuthApp is created and inactive in Punchh DB");
		
		// Insert access_tokens for oauth app created
		insertTokensOauthAccessTokens(env, appId, "wifi", minRecordCount, "NOW()");

		// Validate access tokens inserted in Punchh DB for inactive oauth app
		String queryExpiredTokens = " SELECT count(*) as count FROM oauth_access_tokens where application_id = " + appId;
		verifyRecordCount(queryExpiredTokens, minRecordCount);

		// Execute the cron job to delete oauth applications
		Response resp = pageObj.endpoints().enqueueWorker("OauthApplicationDeleteWorker");
		Assert.assertEquals(resp.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Incorrect status code from cron job");

		utils.longWaitInSeconds(10);
		utils.logit("Starting post-cron validation.");

		// Query for expired tokens (should be deleted)
		verifyRecordCount(queryExpiredTokens, 0);
		utils.logit("Pass", "Expired tokens successfully deleted.");

		// Query for Oauth App (should be deleted)
		String queryOauthApp = " SELECT count(*) as count FROM oauth_applications where id = " + appId;
		verifyRecordCount(queryOauthApp, 0);
		utils.logit("Pass", "Deleted OauthApp deleted from DB also successfully.");
	}

	@Test(description = "SQ_T3506 INT2-991 | INT2-1117 IdentityCron | oauthApplicationDeleteWorker | Verify oauth_application and oauth_access_tokens deletion via cron",
			groups = {"nonNightly"})
	@Owner(name ="Nipun Jain")
	public void SQ_T3506_OauthAppDeleteWorkerCronIdentityTest() throws Exception {
		int minRecordCount = 50;

		// Oauth app creation and deletion
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "OAuth Apps");
		pageObj.oAuthAppPage().clickNewApplication();
		String oauthAppName = "AutoTest Oauth App " + CreateDateTime.getTimeDateAsneed("yyyyMMddHHmmss");
		pageObj.oAuthAppPage().enterAppName(oauthAppName);
		pageObj.oAuthAppPage().enterRedirectUri(dataSet.get("redirectUri"));
		pageObj.oAuthAppPage().selectScope("Auth Service");
		pageObj.oAuthAppPage().enterValidationURI(baseUrl + "/invalid");
		pageObj.oAuthAppPage().enterVerificationKey("invalid");
		pageObj.oAuthAppPage().clickSave();
		pageObj.oAuthAppPage().verifyDisplayedMessage(MessagesConstants.oauthAppCreateSuccessMsg);
		utils.logit(oauthAppName + " created successfully");

		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "OAuth Apps");
		pageObj.oAuthAppPage().deleteOauthAppByName(oauthAppName);

		pageObj.oAuthAppPage().verifyDisplayedMessage(MessagesConstants.oauthAppDeleteSuccessMsg);
		utils.logit(oauthAppName + " deleted successfully");

		// Verify inactive app in Identity DB
		String query = "select id from oauth_applications where name = '" + oauthAppName + "' and is_active = 0";
		String appId = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, guestIdentityhost, query, "id", 10);
		Assert.assertNotNull(appId, "OauthApp is not created or inactive in Identity DB");
		utils.logPass("Verified OAuthApp is created and inactive in Identity DB");

		// Insert access_tokens for oauth app created in identity DB
		insertTokensOauthAccessTokens(env, guestIdentityhost, appId, "auth_service", minRecordCount, "NOW()");

		// Validate access tokens inserted in Punchh DB for inactive oauth app
		String queryExpiredTokens = " SELECT count(*) as count FROM oauth_access_tokens where application_id = " + appId;
		verifyRecordCount(guestIdentityhost, queryExpiredTokens, minRecordCount);

		// Execute the cron job to delete oauth applications
		Response resp = pageObj.endpoints().enqueueIdentityWorker("OauthApplicationDeleteWorker");
		Assert.assertEquals(resp.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Incorrect status code from cron job");

		utils.longWaitInSeconds(10);
		utils.logit("Starting post-cron validation.");

		// Query for expired tokens (should be deleted)
		verifyRecordCount(queryExpiredTokens, 0);
		utils.logit("Pass", "Expired tokens successfully deleted.");

		// Query for Oauth App (should be deleted)
		String queryOauthApp = " SELECT count(*) as count FROM oauth_applications where id = " + appId;
		verifyRecordCount(guestIdentityhost, queryOauthApp, 0);
		utils.logit("Pass", "Deleted OauthApp deleted from DB also successfully.");
	}

	// Helper Method - Count greater than expected count if not 0
	private void verifyRecordCount(String query, Integer expectedCount) throws Exception {
		verifyRecordCount(null, query, expectedCount);
	}

	private void verifyRecordCount(String dbHost, String query, Integer expectedCount) throws Exception {
		String count = DBUtils.executeQueryAndGetColumnValue(env, dbHost, query, "count");
		utils.logit("Record count: " + count);
		if (expectedCount != 0)
			Assert.assertTrue(Integer.parseInt(count) >= expectedCount, "Record count mismatch for query.");
		else
			Assert.assertEquals(Integer.parseInt(count), 0, "Record count mismatch for query.");
		utils.logit("Pass", "Record count verified, expected count: " + expectedCount + " actual count: " + count);
	}

	// Oauth Access Tokens Helper Method
	private void insertTokensOauthAccessTokens(String env, String applicationId, String scope, int count,
			String createdAtValue) throws Exception {
		insertTokensOauthAccessTokens(env, null, applicationId, scope, count, createdAtValue);
	}

	private void insertTokensOauthAccessTokens(String env, String dbHost, String applicationId, String scope, int count,
			String createdAtValue) throws Exception {

		StringBuilder insertQuery = new StringBuilder(
				"INSERT INTO oauth_access_tokens (resource_owner_id, application_id, token, refresh_token, expires_in, revoked_at, created_at, scopes) VALUES ");
		for (int i = 1; i <= count; i++) {
			insertQuery.append(String.format(
					"('100000', '%s', 'tokenCreatedByAutomation%s', UUID(), NULL, NULL, %s, '%s')%s", applicationId,
					utils.getTimestampInNanoseconds(), createdAtValue, scope, (i < count) ? ", " : ""));
		}
		DBUtils.executeQuery(env, dbHost, insertQuery.toString());
	}
	
	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		if (dataSet != null)
			pageObj.utils().clearDataSet(dataSet);
		utils.logit("Test Case: " + sTCName + " finished");
		driver.quit();
		utils.logit("Browser closed");
	}
}
