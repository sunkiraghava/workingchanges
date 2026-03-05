package com.punchh.server.Integration2;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.MessagesConstants;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
@SuppressWarnings("static-access")
public class IdentityServiceGUITest {
	public WebDriver driver;
	private PageObj pageObj;
	private String sTCName;
	private String baseUrl;
	private String env, run = "ui";
	private static Map<String, String> dataSet;
	private Utilities utils;
	private String guestIdentityhost = "guestIdentity";

	@BeforeClass
	public void setup() throws Exception {
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv().toLowerCase();
		utils = new Utilities();
		utils.logit("Using env as ==> " + env);
	}

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
		driver = new BrowserUtilities().launchBrowser();
		Utilities.loadPropertiesFile("config.properties");
		sTCName = method.getName();
		pageObj = new PageObj(driver);
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		utils.logit(sTCName + " ==>" + dataSet);
	}

	@Test(description = "SQ_T3605_Verify Auth Service - IP address whitelisting | BrandLevelToken")
	public void SQ_T3605_identityBrandLevelTokenIPWhitelistingTest() throws Exception {
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "OAuth Apps");
		String oauthAppName = dataSet.get("oauthAppName");
		pageObj.oAuthAppPage().openAppByName(oauthAppName);
		// Update invalid IP
		pageObj.oAuthAppPage().enterWhitelistIP("100.100.100.100");
		pageObj.oAuthAppPage().verifyDisplayedMessage(MessagesConstants.oauthAppUpdateSuccessMsg);
		utils.logit("Updated whitelist IP to 100.100.100.100");
		// Hit brand level token api
		Response generateBrandLevelTokenResponse = pageObj.endpoints()
				.identityGenerateBrandLevelToken(dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(generateBrandLevelTokenResponse.getStatusCode(), 412,
				"Incorrect status code for invalid IP");
		Assert.assertTrue(generateBrandLevelTokenResponse.jsonPath().get("errors").toString()
				.contains("Invalid request or host."), "Incorrect error message for invalid IP");
		TestListeners.extentTest.get().pass("Brand level token API returned 412 status code with invalid IP");
		// Update valid IP
		String sysIP = utils.getSystemPublicIPv4();
		pageObj.oAuthAppPage().enterWhitelistIP(sysIP);
		pageObj.oAuthAppPage().clickSave();
		pageObj.oAuthAppPage().verifyDisplayedMessage(MessagesConstants.oauthAppUpdateSuccessMsg);
		utils.logit("Updated whitelist IP to " + sysIP);
		// Hit brand level token api
		Response generateBrandLevelTokenResponse2 = pageObj.endpoints()
				.identityGenerateBrandLevelToken(dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(generateBrandLevelTokenResponse2.getStatusCode(), 200,
				"Incorrect status code for valid IP");
		utils.logit("Pass", "Brand level token API returned 200 status code with valid IP");
	}

	// Long test case to divide it
	@Test(description = "SQ-T4406 (1.0) INT2-1137 | Verify for sync archived oauth app in guest identity service")
	public void SQ_T4406_identityOauthAppSyncTest() throws Exception {
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "OAuth Apps");

		// Create a new oauth application
		String oauthAppName = "AutoTest Oauth App " + CreateDateTime.getTimeDateAsneed("yyyyMMddHHmmss");
		pageObj.oAuthAppPage().createIdentityOauthApp(oauthAppName, baseUrl, "Auth Service", baseUrl, "Test",
				MessagesConstants.oauthAppCreateSuccessMsg);

		String query = "select count(*) as count from oauth_applications where name = '" + oauthAppName
				+ "' and is_active = 1";
		String count = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, guestIdentityhost, query, "count", 10);
		Assert.assertEquals(count, "1", query + " Query count not matched ");
		utils.logPass("Verified OAuthApp is created and active in guest Identity DB");

		pageObj.oAuthAppPage().selectScope("Wifi");
		pageObj.oAuthAppPage().clickSave();
		pageObj.oAuthAppPage().verifyDisplayedMessage(MessagesConstants.oauthAppUpdateSuccessMsg);
		utils.logit("Created App Scope changed from AuthServce to Wifi");

		query = "select count(*) as count from oauth_applications where name = '" + oauthAppName
				+ "' and is_active = 0";
		count = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, guestIdentityhost, query, "count", 10);
		Assert.assertEquals(count, "1", query + " Query count not matched ");
		utils.logPass("Verified OAuthApp deactivated in guest Identity");

		query = "select count(*) as count from oauth_applications where name = '" + oauthAppName
				+ "' and is_active = 1";
		count = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query, "count", 10);
		Assert.assertEquals(count, "1", query + " Query count not matched ");
		utils.logPass("Verified OAuthApp still active in Punchh");

		pageObj.oAuthAppPage().selectScope("Auth Service");
		pageObj.oAuthAppPage().clickSave();
		pageObj.oAuthAppPage().verifyDisplayedMessage(MessagesConstants.oauthAppUpdateSuccessMsg);
		utils.logit("Created OAuthApp Scope changed from Wifi to AuthService");

		query = "select count(*) as count from oauth_applications where name = '" + oauthAppName
				+ "' and is_active = 1";
		count = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, guestIdentityhost, query, "count", 10);
		Assert.assertEquals(count, "1", query + " Query count not matched ");
		utils.logPass("Verified OAuthApp activated in guest Identity");

		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "OAuth Apps");
		pageObj.oAuthAppPage().deleteOauthAppByName(oauthAppName);
		pageObj.oAuthAppPage().verifyDisplayedMessage(MessagesConstants.oauthAppDeleteSuccessMsg);
		utils.logit("Created OAuthApp deleted");

		query = "select count(*) as count from oauth_applications where name = '" + oauthAppName
				+ "' and is_active = 0";
		count = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, guestIdentityhost, query, "count", 10);
		Assert.assertEquals(count, "1", query + " Query count not matched ");
		utils.logPass("Verified OAuthApp deactived in guest Identity");

		String oauthAppName2 = "AutoTest Oauth App " + CreateDateTime.getTimeDateAsneed("yyyyMMddHHmmss");
		pageObj.oAuthAppPage().createOauthApp(oauthAppName2, baseUrl, "Wifi",
				MessagesConstants.oauthAppCreateSuccessMsg);
		pageObj.oAuthAppPage().selectScope("Auth Service");
		pageObj.oAuthAppPage().enterValidationURI("https://punchh.com");
		pageObj.oAuthAppPage().enterVerificationKey("Test");
		pageObj.oAuthAppPage().clickSave();
		pageObj.oAuthAppPage().verifyDisplayedMessage(MessagesConstants.oauthAppUpdateSuccessMsg);
		utils.logit("OauthApp scope changed from Wifi to AuthService");

		query = "select count(*) as count from oauth_applications where name = '" + oauthAppName2
				+ "' and is_active = 1";
		count = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, guestIdentityhost, query, "count", 10);
		Assert.assertEquals(count, "1", query + " Query count not matched ");
		utils.logPass("Verified OauthApp created and active in guest Identity");

		try {
			pageObj.menupage().navigateToSubMenuItem("Whitelabel", "OAuth Apps");
			pageObj.oAuthAppPage().deleteOauthAppByName(oauthAppName2);
			utils.logPass("OauthApp deleted successfully - " + oauthAppName2);
		} catch (Exception e) {
			utils.logit("Warning", "Error in deleting oauth app - " + oauthAppName2);
		}

		utils.logit("Pass", "All combinations of oauth app updation and deletion successfully verified");
	}

	@Test(description = "SQ_T5358_INT2-1597 | Verify proper error in all Menu(Identity) API's if oauth_app is inactive")
	public void T5358_VerifyErrorWithInactiveOauthAppTest() throws Exception {
		SoftAssert softAssert = new SoftAssert();

		// Oauth app creation and deletion
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "OAuth Apps");
		pageObj.oAuthAppPage().clickNewApplication();
		String oauthAppName = "AutoTest Oauth App " + CreateDateTime.getTimeDateAsneed("yyyyMMddHHmmss");
		pageObj.oAuthAppPage().enterAppName(oauthAppName);
		pageObj.oAuthAppPage().enterRedirectUri(dataSet.get("redirectURI"));
		pageObj.oAuthAppPage().selectScope(dataSet.get("scope"));
		pageObj.oAuthAppPage().enterValidationURI(dataSet.get("redirectURI"));
		pageObj.oAuthAppPage().enterVerificationKey(dataSet.get("verificationKey"));
		pageObj.oAuthAppPage().clickSave();
		pageObj.oAuthAppPage().verifyDisplayedMessage(MessagesConstants.oauthAppCreateSuccessMsg);
		utils.logPass("OauthApp created successfully");

		// Get client and secret in format client|secret
		String query = "select CONCAT(uid, '|', secret) as client_secret from oauth_applications where name = '"
				+ oauthAppName + "'";
		String clientSecret = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, guestIdentityhost, query,
				"client_secret", 10);

		Assert.assertNotEquals(clientSecret, "", query + " - No record found in guest Identity DB");
		utils.logit("Client|Secret : => ", clientSecret);

		// Split to get client and secret separately
		String[] parts = clientSecret.split("\\|");
		String client = parts[0];
		String secret = parts.length > 1 ? parts[1] : "";

		utils.logit("Client : => ", client);
		utils.logit("Secret : => ", secret);

		Response generateBrandLevelTokenResponse = pageObj.endpoints().identityGenerateBrandLevelToken(client, secret);
		String validBrandLevelToken = generateBrandLevelTokenResponse.jsonPath()
				.getString("data.oauth_token.access_token");
		utils.logit("Using brandLevelToken as ==> " + validBrandLevelToken);

		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "OAuth Apps");
		pageObj.oAuthAppPage().deleteOauthAppByName(oauthAppName);
		pageObj.oAuthAppPage().verifyDisplayedMessage(MessagesConstants.oauthAppDeleteSuccessMsg);
		utils.logit("Created OAuthApp deleted");

		// Verify inactive app in identity DB
		query = "select uid as client from oauth_applications where name = '" + oauthAppName + "' and is_active = 0";
		client = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, guestIdentityhost, query, "client", 10);
		Assert.assertNotEquals(client, "", query + " - No record found in Identity DB");
		utils.logPass("Verified OAuthApp is created and inactive in Identity DB");

		// Step 1: Generate Brand Level Token
		Response brandLevelTokenResponse = pageObj.endpoints().identityGenerateBrandLevelToken(client, secret);
		verifyInactiveOauthAppError(brandLevelTokenResponse, "Identity - BrandLevelToken", softAssert);

		// Step 2: User Sign Up
		Response userSignUpResponse = pageObj.endpoints().identityUserSignUp(validBrandLevelToken, client, false,
				"punchhAppDeviceId", CreateDateTime.getTimeDateString());
		verifyInactiveOauthAppError(userSignUpResponse, "Identity - UserSignUp", softAssert);

		// Step 3: User Sign In
		Response userSignInResponse = pageObj.endpoints().identityUserSignIn(validBrandLevelToken, client, "email",
				"brand");
		verifyInactiveOauthAppError(userSignInResponse, "Identity - UserSignIn", softAssert);

		// Step 4: User Sign Out
		Response userSignOutResponse = pageObj.endpoints().identityUserSignOut(validBrandLevelToken, "userAccessToken");
		verifyInactiveOauthAppError(userSignOutResponse, "Identity - UserSignOut", softAssert);

		// Assert all
		softAssert.assertAll();
		utils.logit("Pass", "Verified proper error in all Menu API's if oauth_app is inactive");
	}

	// Helper method to verify error in all Menu(Identity) API's if oauth_app is
	// inactive
	private void verifyInactiveOauthAppError(Response response, String apiName, SoftAssert softAssert) {
		verifyErrorResponse(response, 412, "invalid_client", "Invalid Client / Client not found.", apiName, softAssert);
	}

	// Generalized error verification method
	private void verifyErrorResponse(Response response, int expectedStatusCode, String errorField,
			String expectedErrorMsg, String apiName, SoftAssert softAssert) {
		softAssert.assertEquals(response.getStatusCode(), expectedStatusCode,
				"Status code did not match for: '" + apiName + "'");
		softAssert.assertTrue(response.jsonPath().getList("errors." + errorField).contains(expectedErrorMsg),
				"Error message did not match for: '" + apiName + "'");
		utils.logit("Pass", "Proper error message is displayed for: " + apiName);
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