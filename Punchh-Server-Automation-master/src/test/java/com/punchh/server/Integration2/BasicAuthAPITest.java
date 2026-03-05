package com.punchh.server.Integration2;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.github.javafaker.Faker;
import com.punchh.server.annotations.Owner;
import com.punchh.server.api.payloadbuilder.DynamicPayloadBuilder;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
@SuppressWarnings("static-access")
public class BasicAuthAPITest {
	static Logger logger = LogManager.getLogger(BasicAuthAPITest.class);
	private String userEmail;
	private PageObj pageObj;
	private String sTCName;
	private String env, run = "api";
	private static Map<String, String> dataSet;
	private Utilities utils;
	private IntUtils intutils;
	private String client, secret;
	private Faker faker;
	private String guestIdentityhost = "guestIdentity";

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
		sTCName = method.getName();
		pageObj = new PageObj();
		env = pageObj.getEnvDetails().setEnv();
		utils = new Utilities();
		intutils = new IntUtils();
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath("ui", env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==>" + dataSet);
		client = dataSet.get("client");
		secret = dataSet.get("secret");
		faker = new Faker();
		userEmail = intutils.getRandomGmailEmail();
	}

	@Test(description = "(SQ-T6954, SQ-T6955, SQ-T6956) INT2-2600 | Update user sync flows for new flag basic auth with api1, api2 and auth APIs")
	@Owner(name = "Nipun Jain")
	public void T6954_T6955_T6956_testUpdateUserSyncFlows_BasicAuth() throws Exception {

		// Strong password signup and validate with Punchh APIs
		String strongPassword = "1A@" + faker.internet().password();
		DynamicPayloadBuilder userPayload = new DynamicPayloadBuilder();
		userPayload.setEmail(userEmail).setPassword(strongPassword).setPassword_confirmation(strongPassword)
				.setTerms_and_conditions(true).setPrivacy_policy(true);
		Response response = pageObj.endpoints().basicAuthSignUp(client, userPayload.buildPayloadMap());
		intutils.validateGIS_SignUp_SignIn_Response(response);
		String accessToken = response.jsonPath().get("data.access_token");

		intutils.validateUpdatedUserSyncsWithGIS(client, secret, userEmail, accessToken);
		utils.logit("Pass", "User sync flows test and validation with Guest Identity completed for basic auth flag for all update APIs (api1, api2, auth)");
	}

	@Test(description = " (SQ-T6967) Basic Auth user end to end flow when PAR Loyalty client is used and mapping is present for the PAR Loyalty only.")
	@Owner(name = "Nipun Jain")
	public void T6967_testBasicAuthUserEndToEndFlow_PARLoyaltyMappingOnly() throws Exception {
		String mediumPassword = faker.internet().password(8, 12, true, true, false);
		String strongPassword = "1A@" + faker.internet().password();

		// Weak/Medium password
		DynamicPayloadBuilder userPayload = new DynamicPayloadBuilder();
		userPayload.setEmail(userEmail).setPassword(mediumPassword).setPassword_confirmation(mediumPassword)
				.setTerms_and_conditions(true).setPrivacy_policy(true);

		// Validate error response for weak/medium password
		Response response = pageObj.endpoints().basicAuthSignUp(client, userPayload.buildPayloadMap());
		intutils.verifyErrorResponse(response, ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY, "errors.password[0]",
				"minimum 8 characters with at least one letter, one symbol and one number without spaces",
				"Basic Auth Sign Up");

		// Strong password signup and validate with Punchh APIs
		userPayload = new DynamicPayloadBuilder();
		userPayload.setEmail(userEmail).setPassword(strongPassword).setPassword_confirmation(strongPassword)
				.setTerms_and_conditions(true).setPrivacy_policy(true);
		response = pageObj.endpoints().basicAuthSignUp(client, userPayload.buildPayloadMap());
		intutils.validateGIS_SignUp_SignIn_Response(response);
		String token = response.jsonPath().get("data.access_token");
		intutils.verifyGISAccessTokenWithPunchhAPIs(client, secret, token);
		intutils.basicAuthSignUpSignInDBValidations(userEmail, "sign_up");

		// Sign in and validate with Punchh APIs
		response = pageObj.endpoints().basicAuthSignIn(client, userEmail, strongPassword);
		intutils.validateGIS_SignUp_SignIn_Response(response);
		token = response.jsonPath().get("data.access_token");
		intutils.verifyGISAccessTokenWithPunchhAPIs(client, secret, token);
		String refreshToken = response.jsonPath().get("data.refresh_token");
		intutils.basicAuthSignUpSignInDBValidations(userEmail, "sign_in");

		// Refresh token and validate with Punchh APIs
		response = pageObj.endpoints().basicAuthRefresh(client, refreshToken);
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		token = response.jsonPath().get("data.access_token");
		intutils.verifyGISAccessTokenWithPunchhAPIs(client, secret, token);

		// Change password and validate with Signin
		String newStrongPassword = "1A@" + faker.internet().password();
		response = pageObj.endpoints().basicAuthChangePassword(client, token, strongPassword, newStrongPassword);
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		response = pageObj.endpoints().basicAuthSignIn(client, userEmail, newStrongPassword);
		intutils.validateGIS_SignUp_SignIn_Response(response);

		// Forgot password and validate with Reset password and Signin
		response = pageObj.endpoints().basicAuthForgotPassword(client, userEmail, "true");
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String resetToken = response.jsonPath().get("data.reset_token");

		newStrongPassword = "1A@" + faker.internet().password();
		response = pageObj.endpoints().basicAuthResetPassword(client, resetToken, newStrongPassword);
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		response = pageObj.endpoints().basicAuthSignIn(client, userEmail, newStrongPassword);
		intutils.validateGIS_SignUp_SignIn_Response(response);
		token = response.jsonPath().get("data.access_token");
		refreshToken = response.jsonPath().get("data.refresh_token");

		// Sign out and validate with Punchh API
		response = pageObj.endpoints().basicAuthSignOut(client, token, refreshToken);
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Response balMobileAPI2Resp = pageObj.endpoints().Api2FetchUserBalance(client, secret, token);
		intutils.verifyErrorResponse(balMobileAPI2Resp, ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"errors.blacklisted_token[0]", "Authentication failed as token sent is blacklisted.",
				"Basic Auth Sign Out");

		utils.logit("Pass", "Basic Auth e2e test completed successfully");
	}

	@Test(description = " (SQ-T6971 INT2-2602 | Allow Menu or Other Business Units to Enable/Disable Basic Auth")
	@Owner(name = "Vansham Mishra")
	public void T6971_enableDisableBasicAuth() throws Exception {
		// "enable_basic_auth" => true, "enable_advance_auth" => true
		Response updateBusinessConfigResponse = pageObj.endpoints()
				.updateBusinessConfig(dataSet.get("encryptedEnabledAdvanceAuth"));
		Assert.assertEquals(updateBusinessConfigResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for update business config API");
		String successMessage = updateBusinessConfigResponse.jsonPath().getString("message");
		Assert.assertEquals(successMessage, "Business config updated successfully",
				"Success message did not match for update business config API");
		// validat that encrypted pem is not present in response
		Assert.assertNull(updateBusinessConfigResponse.jsonPath().getString("encrypted_pem"),
				"Encrypted pem is present in response for update business config API");
		verifyAdvanceAuthAndBasicAuthInDB("1", "1", dataSet.get("slug"));

		// "enable_basic_auth" => true, "enable_advance_auth" => false
		Response updateBusinessConfigResponse2 = pageObj.endpoints()
				.updateBusinessConfig(dataSet.get("encryptedDisabledAdvanceAuth"));
		Assert.assertEquals(updateBusinessConfigResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for update business config API");
		String successMessage2 = updateBusinessConfigResponse2.jsonPath().getString("message");
		Assert.assertEquals(successMessage2, "Business config updated successfully",
				"Success message did not match for update business config API");
		// validat that encrypted pem should be present in response
		Assert.assertNull(updateBusinessConfigResponse2.jsonPath().getString("pem_file"),
				"Encrypted pem is not present in response for update business config API");
		verifyAdvanceAuthAndBasicAuthInDB("0", "1", dataSet.get("slug"));

		// "enable_basic_auth" => false, "enable_advance_auth" => true
		Response updateBusinessConfigResponse3 = pageObj.endpoints()
				.updateBusinessConfig(dataSet.get("encryptedDisabledBasicAuth"));
		Assert.assertEquals(updateBusinessConfigResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for update business config API");
		String successMessage3 = updateBusinessConfigResponse3.jsonPath().getString("message");
		Assert.assertEquals(successMessage3, "Business config updated successfully",
				"Success message did not match for deactivate user API");
		// validat that encrypted pem should be present in response
		Assert.assertNotNull(updateBusinessConfigResponse3.jsonPath().getString("pem_file"),
				"Encrypted pem is not present in response for update business config API");
		verifyAdvanceAuthAndBasicAuthInDB("1", "0", dataSet.get("slug"));

		// "enable_basic_auth" => false, "enable_advance_auth" => false
		Response updateBusinessConfigResponse4 = pageObj.endpoints()
				.updateBusinessConfig(dataSet.get("encryptedBothBasicAndAdvanceAuthDisabled"));
		Assert.assertEquals(updateBusinessConfigResponse4.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for update business config API");
		String successMessage4 = updateBusinessConfigResponse4.jsonPath().getString("message");
		Assert.assertEquals(successMessage4, "Business config updated successfully",
				"Success message did not match for update business config API");
		// validat that encrypted pem is not present in response
		Assert.assertNull(updateBusinessConfigResponse4.jsonPath().getString("encrypted_pem"),
				"Encrypted pem is present in response for update business config API");
		verifyAdvanceAuthAndBasicAuthInDB("0", "0", dataSet.get("slug"));
	}

	public void verifyAdvanceAuthAndBasicAuthInDB(String expectedEnableAdvanceAuth, String expectedEnableBasicAuth,
			String slug) throws Exception {
		String query = "SELECT bm.enable_advance_auth, bm.enable_basic_auth FROM business_mappings bm "
				+ "JOIN business_unit_stacks bus ON bm.business_unit_stack_id = bus.id "
				+ "JOIN business_units bu ON bus.business_unit_id = bu.id " + "WHERE bm.slug = '" + slug
				+ "' AND bu.name = 'par_ordering';";

		String[] cols = { "enable_advance_auth", "enable_basic_auth" };
		List<Map<String, String>> res = DBUtils.executeQueryAndGetMultipleColumnsUsingPolling(env, guestIdentityhost,
				query, cols, 2, 20);
		Map<String, String> row = res.get(0);
		String enable_advance_auth = row.get("enable_advance_auth");
		String enable_basic_auth = row.get("enable_basic_auth");
		Assert.assertEquals(enable_advance_auth, expectedEnableAdvanceAuth,
				"enable_advance_auth value did not match in business_mapping table");
		Assert.assertEquals(enable_basic_auth, expectedEnableBasicAuth,
				"enable_basic_auth value did not match in business_mapping table");
		utils.logit("Pass", "Verified enable_advance_auth and enable_basic_auth values in business_mapping table");
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		if (utils != null) {
			utils.clearDataSet(dataSet);
		}
	}
}