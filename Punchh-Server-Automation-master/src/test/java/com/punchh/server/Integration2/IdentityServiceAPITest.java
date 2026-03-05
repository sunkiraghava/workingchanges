package com.punchh.server.Integration2;

import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import com.punchh.server.annotations.Owner;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.DBManager;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
@SuppressWarnings("static-access")
public class IdentityServiceAPITest {
	public WebDriver driver;
	private PageObj pageObj;
	private String sTCName;
	private String env;
	private String run = "ui";
	private static Map<String, String> dataSet;
	private Utilities utils;
	private String client, secret;
	private String userAccessToken;
	private String guestIdentityhost = "guestIdentity";
	private IntUtils intUtils;

	@BeforeMethod(alwaysRun = true)
	public void beforeMethod(Method method) throws SQLException {
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		utils = new Utilities();
		sTCName = method.getName();
		intUtils = new IntUtils(driver);

		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env),
				"identityServiceApiTests");
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
		// Put without overwriting existing keys
		pageObj.readData().readTestData.forEach((key, value) -> dataSet.putIfAbsent(key, value));

		utils.logit("Using env as ==> " + env);
		utils.logit(sTCName + " ==>" + dataSet);

		client = dataSet.get("identityClient");
		secret = dataSet.get("identitySecret");
	}

	private String generateBrandLevelToken() throws Exception {
		Response generateBrandLevelTokenResponse = pageObj.endpoints().identityGenerateBrandLevelToken(client, secret);
		String brandLevelToken = generateBrandLevelTokenResponse.jsonPath().getString("data.oauth_token.access_token");
		utils.logit("Using brandLevelToken as ==> " + brandLevelToken);
		if (brandLevelToken == null || brandLevelToken.isEmpty()) {
			utils.logit("Error", "Brand level token is empty.");
			Assert.fail("Brand level token is empty.");
		}
		return brandLevelToken;
	}

	// Identity User SignUp SignIn assertions:
	public void identityUserSignUpAndSignInAssertions(Response response) throws Exception {
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Incorrect status code");
		Assert.assertNotNull(response.jsonPath().get("oauth_token.access_token"), "Access token is null");
		Assert.assertTrue(utils.isJwtToken(response.jsonPath().get("oauth_token.access_token")),
				"Access token is not JWT");
		Assert.assertEquals(response.jsonPath().get("oauth_token.token_type"), "JWT", "Incorrect token type");
		Assert.assertNotNull(response.jsonPath().get("oauth_token.scope"), "Scope is null");
		Assert.assertNotNull(response.jsonPath().get("oauth_token.expiry"), "Expiry is null");
		Assert.assertNotNull(response.jsonPath().get("user"), "User is null");

		// Modified for INT2-1298
		utils.logit("Verifying NO identityUserSignUpAndSignIn entires in guest identity DB");
		String email = response.jsonPath().get("user.email");
		String query = "select count(*) as count from users where email = '" + email + "'";
		String count = DBManager.executeQueryAndGetColumnValue(env, guestIdentityhost, query, "count");
		Assert.assertEquals(count, "0", query + " count is not matched.");
	}

	// Identity Client Secret and Punchh User API2 SignUp/SignIn assertions:
	public void identityMobileAPI2UserSignUpAndSignInAssertions(Response response) {
		SoftAssert softAssert = new SoftAssert();
		softAssert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Incorrect status code");
		softAssert.assertNotNull(response.jsonPath().get("access_token.token"), "Access token is null");
		softAssert.assertTrue(utils.isJwtToken(response.jsonPath().get("access_token.token")),
				"Access token is not JWT");
		softAssert.assertNotNull(response.jsonPath().get("user.email"), "User email is null");
		softAssert.assertAll();
	}

	@Test(description = "SQ_T3511_Identity User Sign Up with Password || SQ-T3801 INT2-914 | Auth Service | User SignUp is failing with password containing special characters '&', '<', '>'")
	@Owner(name = "Nipun Jain")
	public void SQ_T3511_userSignUpWithPasswordTest() throws Exception {
		String brandLevelToken = generateBrandLevelToken();
		String punchhAppDeviceId = UUID.randomUUID().toString();
		String userAgent = UUID.randomUUID().toString();
		Response userSignUpWithPasswordRes = pageObj.endpoints().identityUserSignUp(brandLevelToken, client, true,
				punchhAppDeviceId, userAgent);
		identityUserSignUpAndSignInAssertions(userSignUpWithPasswordRes);
		intUtils.verifyGISAccessTokenWithPunchhAPIs(client, secret,
				userSignUpWithPasswordRes.jsonPath().get("oauth_token.access_token"));
		userAccessToken = userSignUpWithPasswordRes.jsonPath().get("oauth_token.access_token");
		utils.logit("UserAccessToken as ==> " + userAccessToken);
	}

	@Test(description = "SQ_T3531 Identity User Sign Up without Password", groups = { "nonNightly" })
	@Owner(name = "Nipun Jain")
	public void SQ_T3531_userSignUpWithoutPasswordTest() throws Exception {
		String brandLevelToken = generateBrandLevelToken();
		String punchhAppDeviceId = UUID.randomUUID().toString();
		String userAgent = UUID.randomUUID().toString();
		Response userSignUpWithoutPasswordRes = pageObj.endpoints().identityUserSignUp(brandLevelToken, client, false,
				punchhAppDeviceId, userAgent);
		identityUserSignUpAndSignInAssertions(userSignUpWithoutPasswordRes);
		intUtils.verifyGISAccessTokenWithPunchhAPIs(client, secret,
				userSignUpWithoutPasswordRes.jsonPath().get("oauth_token.access_token"));
	}

	@Test(description = "SQ_T3508_SQ_T3507 User SignUp with punchh server API and identity Client-Secret", groups = {
			"nonNightly" })
	@Owner(name = "Nipun Jain")
	public void SQ_T3508_SQ_T3507_userSignUp_And_SignInWithPunchhServerAPIAndIdentityClientSecretTest()
			throws Exception {
		String email = "identity_auto_" + CreateDateTime.getTimeDateAsneed("yyyyMMddHHmmss") + "@partech.com";
		Response api2PunchhSignUpResponse = pageObj.endpoints().Api2SignUp(email, client, secret);
		identityMobileAPI2UserSignUpAndSignInAssertions(api2PunchhSignUpResponse);
		intUtils.verifyGISAccessTokenWithPunchhAPIs(client, secret,
				api2PunchhSignUpResponse.jsonPath().get("access_token.token"));

		// Sign in with old sign in API - /api2/mobile/users/login
		Response userSignInResponse = pageObj.endpoints().Api2Login(email, client, secret);
		identityMobileAPI2UserSignUpAndSignInAssertions(userSignInResponse);
		intUtils.verifyGISAccessTokenWithPunchhAPIs(client, secret,
				userSignInResponse.jsonPath().get("access_token.token"));
	}

	// Sign in with new sign in API - /api2/mobile/users/auth_sign_in
	@Test(description = "SQ_T3509 User SignIn with punchh server New API and identity Client-Secret", groups = {
			"nonNightly" })
	@Owner(name = "Nipun Jain")
	public void SQ_T3509_userSignInWithNewMobileAPI2AndIdentityClientSecretTest() throws Exception {
		String brandLevelToken = generateBrandLevelToken();
		String punchhAppDeviceId = UUID.randomUUID().toString();
		String userAgent = UUID.randomUUID().toString();
		Response userSignUpWithoutPasswordRes = pageObj.endpoints().identityUserSignUp(brandLevelToken, client, false,
				punchhAppDeviceId, userAgent);
		String email = userSignUpWithoutPasswordRes.jsonPath().get("user.email");
		Response userSignInResponse = pageObj.endpoints().identityUserSignInWithMobileAPI2AndIdentityClientSecret(email,
				client, secret, punchhAppDeviceId, userAgent);
		identityMobileAPI2UserSignUpAndSignInAssertions(userSignInResponse);
		intUtils.verifyGISAccessTokenWithPunchhAPIs(client, secret,
				userSignInResponse.jsonPath().get("access_token.token"));
	}

	@Test(description = "SQ_T3510_Identity User Sign IN", groups = { "nonNightly" })
	@Owner(name = "Nipun Jain")
	public void SQ_T3510_userSignInMenuTest() throws Exception {
		String brandLevelToken = generateBrandLevelToken();
		String punchhAppDeviceId = UUID.randomUUID().toString();
		String userAgent = UUID.randomUUID().toString();
		Response userSignUpWithoutPasswordRes = pageObj.endpoints().identityUserSignUp(brandLevelToken, client, false,
				punchhAppDeviceId, userAgent);
		String email = userSignUpWithoutPasswordRes.jsonPath().get("user.email");
		Response signInres = pageObj.endpoints().identityUserSignIn(brandLevelToken, client, email, "Menu");
		identityUserSignUpAndSignInAssertions(signInres);
		intUtils.verifyGISAccessTokenWithPunchhAPIs(client, secret,
				signInres.jsonPath().get("oauth_token.access_token"));
	}

	@Test(description = "SQ_T3544 Identity User Sign IN with valid and invalid verification key combinations", groups = {
			"nonNightly" })
	@Owner(name = "Nipun Jain")
	public void SQ_T3544_SignInAPIWithVerificationKeyCombinationsMenuTest() throws Exception {
		String brandLevelToken = generateBrandLevelToken();
		String punchhAppDeviceId = UUID.randomUUID().toString();
		String userAgent = UUID.randomUUID().toString();

		String verificationKeyMenu = dataSet.get("verificationKeyMenu");
		String email = null;

		// Scenario 1 - valid verification key in API
		email = "identity_auto_" + CreateDateTime.getTimeDateAsneed("yyyyMMddHHmmss") + "@partech.com";
		pageObj.endpoints().identityUserSignUp(brandLevelToken, client, true, punchhAppDeviceId, userAgent);
		Response signInres = pageObj.endpoints().identityUserSignIn(brandLevelToken, client, email, "Menu",
				verificationKeyMenu);
		identityUserSignUpAndSignInAssertions(signInres);
		intUtils.verifyGISAccessTokenWithPunchhAPIs(client, secret,
				signInres.jsonPath().get("oauth_token.access_token"));
		String punchhAppDeviceId1 = UUID.randomUUID().toString();
		String userAgent1 = UUID.randomUUID().toString();

		// Scenario 2 - Valid client in Oauth Application Dashboard and valid
		// verification key in API
		email = "identity_auto_" + CreateDateTime.getTimeDateAsneed("yyyyMMddHHmmss") + "@partech.com";
		pageObj.endpoints().identityUserSignUp(brandLevelToken, client, true, punchhAppDeviceId1, userAgent1);
		signInres = pageObj.endpoints().identityUserSignIn(brandLevelToken, client, email, "Menu", "invalid_key");
		Assert.assertEquals(signInres.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST, "Incorrect status code");
	}

	@Test(description = "SQ_T3694 Identity User Sign Out", dependsOnMethods = "SQ_T3511_userSignUpWithPasswordTest", groups = {
			"nonNightly" })
	@Owner(name = "Shashank Sharma")
	public void SQ_T3694_userSignOutTest() throws Exception {
		String brandLevelToken = generateBrandLevelToken();
		String query = "SELECT count(*) as 'count' FROM oauth_access_tokens WHERE token = '" + userAccessToken + "'";
		int count1 = Integer.parseInt(DBUtils.executeQueryAndGetColumnValue(env, query, "count"));
		Assert.assertEquals(count1, 1, query + " Query count not matched ");
		utils.logit("UserAccessToken found in Punchh DB");

		Response userSignOutRes = pageObj.endpoints().identityUserSignOut(brandLevelToken, userAccessToken);
		Assert.assertEquals(userSignOutRes.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Incorrect status code");

		query = "SELECT count(*) as 'count' FROM oauth_access_tokens WHERE token = '" + userAccessToken + "'";
		int count = Integer.parseInt(DBUtils.executeQueryAndGetColumnValue(env, query, "count"));
		Assert.assertEquals(count, 0, query + " count is not matched with 0");
		utils.logPass("UserAccessToken deleted from DB");
	}

	@Test(description = "SQ-T4542 INT2-1198 , INT2-1246 | Identity SignUp and SignIn API with headers", groups = {
			"nonNightly" })
	@Owner(name = "Nipun Jain")
	public void T4542_userSignUpWithHeaders() throws Exception {
		String brandLevelToken = generateBrandLevelToken();
		String punchhAppDeviceId = UUID.randomUUID().toString();
		String userAgent = UUID.randomUUID().toString();

		Response userSignUpWithoutPasswordRes = pageObj.endpoints().identityUserSignUp(brandLevelToken, client, false,
				punchhAppDeviceId, userAgent);
		String userID = userSignUpWithoutPasswordRes.jsonPath().getString("user.user_id").replace("[", "").replace("]",
				"");

		identityUserSignUpAndSignInAssertions(userSignUpWithoutPasswordRes);
		intUtils.verifyGISAccessTokenWithPunchhAPIs(client, secret,
				userSignUpWithoutPasswordRes.jsonPath().get("oauth_token.access_token"));

		String getPunchhAppDeviceIDQuery = "select app_device_id from user_devices where user_id = '" + userID + "';";
		String actualAppDeviceID = DBUtils.executeQueryAndGetColumnValue(env, getPunchhAppDeviceIDQuery,
				"app_device_id");
		Assert.assertEquals(actualAppDeviceID, punchhAppDeviceId,
				actualAppDeviceID + " punch_app_device not matched with expected " + punchhAppDeviceId);

		String getUserAgentIDQuery = "select user_id from user_agent_trackings where user_id = '" + userID
				+ "' and user_agent = '" + userAgent + "';";
		String userId = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, getUserAgentIDQuery, "user_id", 30);
		Assert.assertEquals(userId, userID, userId + " user_id not matched with expected " + userID
				+ ". There might be delay in syncing of relica DB");
		utils.logPass("Verified that user_agent is synced successfully in DB for user " + userID);
	}

	@Test(description = "SQ-T4541 INT2-1198 | Identity SignUp API with menu subject_token", groups = { "nonNightly" })
	@Owner(name = "Nipun Jain")
	public void T4541_VerifyIdentitySignUpAPIWithMenuSubjectToken() throws Exception {
		String brandLevelToken = generateBrandLevelToken();
		String slug = dataSet.get("slug");
		String emailID = "identity_auto_" + CreateDateTime.getTimeDateAsneed("yyyyMMddHHmmss") + "@partech.com";
		String brandUserToken = pageObj.endpoints().identityGenerateBrandUserToken(emailID, "Menu").jsonPath()
				.get("data.token.value");

		String userExistsInPunchDBQuery = "select email from users where email='" + emailID
				+ "' and business_id = (select id from businesses where slug = '" + slug + "');";
		String isUserExist = DBUtils.executeQueryAndGetColumnValue(env, userExistsInPunchDBQuery, "email");
		Assert.assertEquals(isUserExist, "", emailID + " user should not exist in punchh DB");
		utils.logPass("Verified that " + emailID + " user does not exist in Punchh DB before signup in Identity");

		String punchhAppDeviceId = UUID.randomUUID().toString();
		String userAgent = UUID.randomUUID().toString();

		Response userSignUpWithoutPasswordRes = pageObj.endpoints().identityUserSignUpWithEmail(brandLevelToken, client,
				false, punchhAppDeviceId, userAgent, emailID, brandUserToken);

		String identityUserID = userSignUpWithoutPasswordRes.jsonPath().getString("user.user_id").replace("]", "")
				.replace("[", "");

		int identityUserIDInt = Integer.parseInt(identityUserID);

		String query = "select user_id from user_external_identifiers where user_id='" + identityUserID + "';";
		String actualuserID = DBUtils.executeQueryAndGetColumnValue(env, query, "user_id");
		int actualuserIDInt = Integer.parseInt(actualuserID);

		Assert.assertEquals(actualuserIDInt, identityUserIDInt, emailID + " user should exist in punchh DB");
		utils.logPass("Verified that " + emailID + " user is exist in Punchh DB after signup in Identity");
	}

	@AfterMethod(alwaysRun = true)
	public void afterMethod() {
		if (dataSet != null)
			pageObj.utils().clearDataSet(dataSet);
		utils.logit("Test Case: " + sTCName + " finished");
	}
}