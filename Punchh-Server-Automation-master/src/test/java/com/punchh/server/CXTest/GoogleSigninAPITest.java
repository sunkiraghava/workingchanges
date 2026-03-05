package com.punchh.server.CXTest;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.utilities.MessagesConstants;
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
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;

import io.restassured.response.Response;

import static com.punchh.server.utilities.SingletonDBUtils.utils;

@Listeners(TestListeners.class)
public class GoogleSigninAPITest {

	private static Logger logger = LogManager.getLogger(GoogleSigninAPITest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String sTCName, env;
	private String run = "ui";
	private String baseUrl;
	private static Map<String, String> dataSet;
	String activate = "/activate/";
	
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
		// move to All Business Page
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
	}

	@Test(description = "SQ-T2769 To validate google sign-in using v1 mobile api" +
            "SQ-T7086 Verify the  successful google sign up in API/mobile(POST)" +
            "SQ-T7087 Verify the  successful google sign in API/mobile(POST)" +
            "SQ-T7088 Verify the  successful google sign up in API2/mobile(POST)" +
            "SQ-T7089 Verify the  successful google sign in, in API2/mobile(POST)" +
            "SQ-T7093 Verify the google sign up api/auth(POST)" +
            "SQ-T7094 Verify the google sign in api/auth(POST)" +
            "SQ-T7095 Verify the proper error message for deactivated/deleted (soft) user  while signing in using google." +
            "SQ-T7231 Verify the proper error message for deactivated user while signing in using google for API/mobile." +
            "SQ-T7249 Verify the proper error message for deactivated user while signing in using google using API2/mobile." +
            "SQ-T7252 Verify the proper error message for deactivated user while signing in using google using API/Auth",groups = {"nonNightly" ,"Regression"}, priority = 0, enabled = true)
	@Owner(name = "Rajasekhar Reddy")
	public void T2769_VerifyGoogleSignupIn() throws Exception {
		TestListeners.extentTest.get().info(sTCName + " ==>" + dataSet);
		// Select the business
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().navigateToTabs("Miscellaneous Config");
		pageObj.dashboardpage().checkUncheckAnyFlag("Enable Google Sign-In?", "check");
		pageObj.whitelabelPage().verifyGooglesigninConfigs();
		String jwt_token = pageObj.googleServiceAccountJwtGenerator().generateIdToken(dataSet.get("google_client"));

		// User register/login using Google v1 API
		Response GooglesignUpResponse = pageObj.endpoints().Api1GoogleSignUp(dataSet.get("client"),
				dataSet.get("secret"), jwt_token);
		Assert.assertEquals(GooglesignUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api1 signup");
		String google_uid = GooglesignUpResponse.jsonPath().get("google_uid").toString();
		String email = GooglesignUpResponse.jsonPath().get("email").toString();
		boolean google_signup_status1 = GooglesignUpResponse.jsonPath().get("google_signup");
		Assert.assertTrue(google_signup_status1,"user was not signed up successfully using google v1 api");
        logger.info("user was signed up successfully: " + google_signup_status1);
		pageObj.instanceDashboardPage().navigateToGuestTimeline(email);
		pageObj.guestTimelinePage().verifyExternalSourceid();
        String currentPageUrl = pageObj.utils().getCurrentURL();
        // fetch the source_id from the GooglesignUpResponse
        String source_id = GooglesignUpResponse.jsonPath().get("google_uid").toString();
        punchhDbValidations(email,dataSet.get("business_id"),source_id,"api1");
        GooglesignUpResponse = pageObj.endpoints().Api1GoogleSignUp(dataSet.get("client"),
                dataSet.get("secret"), jwt_token);
        Assert.assertEquals(GooglesignUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
                "Status code 200 did not matched for api1 signup");
        google_signup_status1 = GooglesignUpResponse.jsonPath().get("google_signup");
        // on sign in using api1 the google_signup should be false
        Assert.assertFalse(google_signup_status1,"user sign-in using google v1 api is not successful");
        punchhDbValidations(email,dataSet.get("business_id"),source_id,"api1");

        // Get punchh user id from users table
        String query = "SELECT id FROM users WHERE email = '" + email + "'";
        String punchhUserId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");
        Assert.assertTrue(!punchhUserId.equals(""), "Punchh user ID is empty");
        logger.info("Punchh user ID: " + punchhUserId);
        TestListeners.extentTest.get().info("Punchh user ID: " + punchhUserId);
        // Deactivate user
        Response deacResponse = pageObj.endpoints().deactivateUser(punchhUserId, dataSet.get("apiKey"));
        Assert.assertEquals(deacResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
                "Status code 200 did not matched for deactivate user API");
        GooglesignUpResponse = pageObj.endpoints().Api1GoogleSignUp(dataSet.get("client"),
                dataSet.get("secret"), jwt_token);
        Assert.assertEquals(GooglesignUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
                "Status code 422 did not matched for api1 signup");
        String errorMessage = GooglesignUpResponse.jsonPath().getString("[0]");
        Assert.assertTrue(errorMessage.contains(MessagesConstants.deactivatedUser) ,
                "Error message does not contain 'This account has been deactivated'");
        logger.info("Error message displayed as expected when trying to sign-in with deactivated user: ");
        TestListeners.extentTest.get().info("Error message displayed as expected when trying to sign-in with deactivated user: ");
        // ElasticsearchIndexer::User::User worker trigerd.
        int count = pageObj.sidekiqPage().checkSidekiqJob(baseUrl, "ElasticsearchIndexer::User", 10);
        Assert.assertTrue(count > 0, "ElasticsearchIndexer::User is not called in sidekiq");
        logger.info("ElasticsearchIndexer::User is called in sidekiq");
        TestListeners.extentTest.get().info("ElasticsearchIndexer::User is called in sidekiq");
        pageObj.instanceDashboardPage().navigateToPunchhInstance(currentPageUrl);
		// Delete the Google uuid, and user for reuse the same account
        deleteUserFromDb(email);

		// User register/login using Google v2 API
		String jwt_token1 = pageObj.googleServiceAccountJwtGenerator().generateIdToken(dataSet.get("google_client"));
		Response GoogleV2signUpResponse = pageObj.endpoints().Api2GoogleSignUp(dataSet.get("client"),
				dataSet.get("secret"), jwt_token1);
		Assert.assertEquals(GoogleV2signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api signup");
		String google_uid1 = GoogleV2signUpResponse.jsonPath().get("user.google_uid").toString();
		String email1 = GoogleV2signUpResponse.jsonPath().get("user.email").toString();
		boolean google_signup_status = GoogleV2signUpResponse.jsonPath().get("user.google_signup");
        Assert.assertTrue(google_signup_status,"user was not signed up successfully using google v2 api");
		logger.info("user was signed up successfully: " + google_signup_status);
		TestListeners.extentTest.get().pass("user was signed up successfully: " + google_signup_status);
		pageObj.instanceDashboardPage().navigateToGuestTimeline(email1);
		pageObj.guestTimelinePage().verifyExternalSourceid();
        punchhDbValidations(email,dataSet.get("business_id"),source_id,"api2");
        currentPageUrl = pageObj.utils().getCurrentURL();
        // singin using v2 api
        GoogleV2signUpResponse = pageObj.endpoints().Api2GoogleSignUp(dataSet.get("client"),
                dataSet.get("secret"), jwt_token1);
        Assert.assertEquals(GoogleV2signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
                "Status code 200 did not matched for api2 signup");
        google_signup_status = GoogleV2signUpResponse.jsonPath().get("user.google_signup");
        // on sign in using api2 the google_signup should be false
        Assert.assertFalse(google_signup_status,"user sign-in using google v2 api is not successful");
        punchhDbValidations(email,dataSet.get("business_id"),source_id,"api2");
        // ElasticsearchIndexer::User::User worker trigerd.
        count = pageObj.sidekiqPage().checkSidekiqJob(baseUrl, "ElasticsearchIndexer::User", 10);
        Assert.assertTrue(count > 0, "ElasticsearchIndexer::User is not called in sidekiq");
        logger.info("ElasticsearchIndexer::User is called in sidekiq");
        TestListeners.extentTest.get().info("ElasticsearchIndexer::User is called in sidekiq");

        punchhUserId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");
        Assert.assertTrue(!punchhUserId.equals(""), "Punchh user ID is empty");
        logger.info("Punchh user ID: " + punchhUserId);
        TestListeners.extentTest.get().info("Punchh user ID: " + punchhUserId);
        // Deactivate user
        deacResponse = pageObj.endpoints().deactivateUser(punchhUserId, dataSet.get("apiKey"));
        Assert.assertEquals(deacResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
                "Status code 200 did not matched for deactivate user API");
        GooglesignUpResponse = pageObj.endpoints().Api2GoogleSignUp(dataSet.get("client"),
                dataSet.get("secret"), jwt_token1);
        Assert.assertEquals(GooglesignUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
                "Status code 422 did not matched for api1 signup");
        errorMessage = GooglesignUpResponse.jsonPath().getString("errors[0]");
        Assert.assertTrue(errorMessage.contains(MessagesConstants.deactivatedUser) ,
                "Error message does not contain 'This account has been deactivated'");
        logger.info("Error message displayed as expected when trying to sign-in through api2 with deactivated user: ");
        TestListeners.extentTest.get().info("Error message displayed as expected when trying to sign-in through api2 with deactivated user: ");
        pageObj.instanceDashboardPage().navigateToPunchhInstance(currentPageUrl);
        // Delete the Google uuid, and user for reuse the same account
        deleteUserFromDb(email);

		// User register/login using Google Auth API
		String jwt_token2 = pageObj.googleServiceAccountJwtGenerator().generateIdToken(dataSet.get("google_client"));
		Response GoogleauthsignUpResponse = pageObj.endpoints().authGoogleUserSignUp(dataSet.get("client"),
				dataSet.get("secret"), jwt_token2);
		Assert.assertEquals(GoogleauthsignUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api signup");
		String google_uid2 = GoogleauthsignUpResponse.jsonPath().get("google_uid").toString();
		String email2 = GoogleauthsignUpResponse.jsonPath().get("email").toString();
		boolean google_signup_status2 = GoogleauthsignUpResponse.jsonPath().get("google_signup");
        Assert.assertTrue(google_signup_status2,"user was not signed up successfully using google auth api");
		logger.info("user was signed up successfully: " + google_signup_status2);
		TestListeners.extentTest.get().pass("user was signed up successfully: " + google_signup_status2);
		pageObj.instanceDashboardPage().navigateToGuestTimeline(email2);
		pageObj.guestTimelinePage().verifyExternalSourceid();
        punchhDbValidations(email,dataSet.get("business_id"),source_id,"auth");
        // singin using auth api
        GoogleauthsignUpResponse = pageObj.endpoints().authGoogleUserSignUp(dataSet.get("client"),
                dataSet.get("secret"), jwt_token2);
        Assert.assertEquals(GoogleauthsignUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
                "Status code 200 did not matched for api signup");
        google_signup_status2 = GoogleauthsignUpResponse.jsonPath().get("google_signup");
        // on sign in using auth api the google_signup should be false
        Assert.assertFalse(google_signup_status2,"user sign-in using google auth api is not successful");
        punchhDbValidations(email,dataSet.get("business_id"),source_id,"auth");

        punchhUserId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");
        Assert.assertTrue(!punchhUserId.equals(""), "Punchh user ID is empty");
        logger.info("Punchh user ID: " + punchhUserId);
        TestListeners.extentTest.get().info("Punchh user ID: " + punchhUserId);
        // Deactivate user
        deacResponse = pageObj.endpoints().deactivateUser(punchhUserId, dataSet.get("apiKey"));
        Assert.assertEquals(deacResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
                "Status code 200 did not matched for deactivate user API");
        GooglesignUpResponse = pageObj.endpoints().authGoogleUserSignUp(dataSet.get("client"),
                dataSet.get("secret"), jwt_token2);
        Assert.assertEquals(GooglesignUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
                "Status code 422 did not matched for api1 signup");
        errorMessage = GooglesignUpResponse.jsonPath().getString("[0]");
        Assert.assertTrue(errorMessage.contains(MessagesConstants.deactivatedUser) ,
                "Error message does not contain 'This account has been deactivated'");
        logger.info("Error message displayed as expected when trying to sign-in through auth api with deactivated user: ");
        TestListeners.extentTest.get().info("Error message displayed as expected when trying to sign-in through auth api with deactivated user: ");
        // ElasticsearchIndexer::User::User worker trigerd.
        count = pageObj.sidekiqPage().checkSidekiqJob(baseUrl, "ElasticsearchIndexer::User", 10);
        Assert.assertTrue(count > 0, "ElasticsearchIndexer::User is not called in sidekiq");
        logger.info("ElasticsearchIndexer::User is called in sidekiq");
        TestListeners.extentTest.get().info("ElasticsearchIndexer::User is called in sidekiq");
        // Delete the Google uuid, and user for reuse the same account
        deleteUserFromDb(email);

	}
    public void punchhDbValidations(String email,String businessId,String sourceId,String apiNameSpace)
            throws Exception {
        String signup_channel="";
        if (apiNameSpace.equalsIgnoreCase("auth")){
            signup_channel = "WebGoogle";
        }
        else {
            signup_channel = "MobileGoogle";
        }
        String query = "SELECT id,joined_at,created_at,updated_at from users where email = '" + email + "' and signup_channel = '"+signup_channel+"'";
        String[] cols = {"id","joined_at","created_at","updated_at"};
        List<Map<String, String>> res = DBUtils
                .executeQueryAndGetMultipleColumnsUsingPolling(env,query, cols,2,20);
        Map<String, String> row = res.get(0);
        String user_id = row.get("id");
        String joined_at = row.get("joined_at");
        String created_at = row.get("created_at");
        String updated_at = row.get("updated_at");
        Assert.assertNotNull(user_id, "User ID is null in users table");
        Assert.assertNotNull(joined_at, "joined_at is null in users table");
        Assert.assertNotNull(created_at, "created_at is null in users table");
        Assert.assertNotNull(updated_at, "updated_at is null in users table");

        String query2 = "Select business_id,source,source_id from user_external_identifiers where user_id = '" + user_id + "'";
        String[] cols2 = {"business_id","source","source_id"};
        List<Map<String, String>> res2 = DBUtils
                .executeQueryAndGetMultipleColumnsUsingPolling(env,query2, cols2,2,20);
        Map<String, String> row2 = res2.get(0);
        String business_id = row2.get("business_id");
        String source = row2.get("source");
        String source_id = row2.get("source_id");
        Assert.assertEquals(business_id, businessId, "business_id does not match in user_external_identifiers table");
        Assert.assertEquals(source, "google", "source is not Google in user_external_identifiers table");
        Assert.assertEquals(source_id, sourceId, "source_id does not match in user_external_identifiers table");


    }
    public void deleteUserFromDb(String email) throws Exception {
        String query = "DELETE FROM user_external_identifiers WHERE user_id = (SELECT id FROM users WHERE email = '"
                + email + "')";
        DBUtils.executeUpdateQuery(env, query);
        String query1 = "DELETE FROM users WHERE email = '" + email + "'";
        DBUtils.executeUpdateQuery(env, query1);
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