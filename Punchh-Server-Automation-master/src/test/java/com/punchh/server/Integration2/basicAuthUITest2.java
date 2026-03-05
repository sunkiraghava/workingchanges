package com.punchh.server.Integration2;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javafaker.Faker;
import com.punchh.server.annotations.Owner;
import com.punchh.server.api.payloadbuilder.DynamicPayloadBuilder;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.GmailConnection;
import com.punchh.server.utilities.MessagesConstants;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class basicAuthUITest2 {
	static Logger logger = LogManager.getLogger(com.punchh.server.Integration2.basicAuthUITest2.class);
	public WebDriver driver;
	private String userEmail;
	private PageObj pageObj;
	private String sTCName;
	private String env, run = "ui";
	private String baseUrl;
	private static Map<String, String> dataSet;
	private Utilities utils;
	private IntUtils intUtils;
	private String client, secret;
	private String guestIdentityhost = "guestIdentity";
	private Faker faker;

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
		driver = new BrowserUtilities().launchBrowser();
		sTCName = method.getName();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		utils = new Utilities(driver);
		intUtils = new IntUtils(driver);
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==>" + dataSet);
		client = dataSet.get("client");
		secret = dataSet.get("secret");
		this.faker = new Faker();
		userEmail = intUtils.getRandomGmailEmail();
	}

	@Test(description = "SQ-T6972 INT2-2544 | Change Password (User): Spec + Build", priority = 10)
	@Owner(name = "Vansham Mishra")
	public void T6972_basicAuthChangePasswordForUser() throws Exception {
		// login to Punchh
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
        intUtils.updateAdvanceAndBasicAuthConfig(false, true);

		// set password policy to strong password
		pageObj.cockpitDashboardMiscPage().selectPasswordPolicy(MessagesConstants.guestBasicAuthPPStrong);
		String strongPassword = "1A@" + faker.internet().password();

		// Strong password signup and validate with Punchh APIs
		DynamicPayloadBuilder userPayload = new DynamicPayloadBuilder();
		userPayload.setEmail(userEmail).setPassword(strongPassword).setPassword_confirmation(strongPassword)
				.setTerms_and_conditions(true).setPrivacy_policy(true);
		Response response = pageObj.endpoints().basicAuthSignUp(client, userPayload.buildPayloadMap());
		intUtils.validateGIS_SignUp_SignIn_Response(response);
		String token = response.jsonPath().get("data.access_token");
		intUtils.basicAuthSignUpSignInDBValidations(userEmail, "sign_up");

		// Change password and validate with Signin
		String newStrongPassword = "1A@" + faker.internet().password();
		response = pageObj.endpoints().basicAuthChangePassword(client, token, strongPassword, newStrongPassword);
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		response = pageObj.endpoints().basicAuthSignIn(client, userEmail, newStrongPassword);
		intUtils.validateGIS_SignUp_SignIn_Response(response);
		token = response.jsonPath().get("data.access_token");
		intUtils.verifyGISAccessTokenWithPunchhAPIs(client, secret, token);
		utils.logPass("Verified change password with strong password policy.");

		// set password policy to medium
		String mediumPassword = faker.internet().password(8, 12, true, true, false);
		pageObj.cockpitDashboardMiscPage().selectPasswordPolicy(MessagesConstants.guestBasicAuthPPModerate);
		String userEmail = intUtils.getRandomGmailEmail();
		userPayload = new DynamicPayloadBuilder();
		userPayload.setEmail(userEmail).setPassword(mediumPassword).setPassword_confirmation(mediumPassword)
				.setTerms_and_conditions(true).setPrivacy_policy(true);
		response = pageObj.endpoints().basicAuthSignUp(client, userPayload.buildPayloadMap());
		intUtils.validateGIS_SignUp_SignIn_Response(response);
		intUtils.basicAuthSignUpSignInDBValidations(userEmail, "sign_up");
		token = response.jsonPath().get("data.access_token");

		// Change password and validate with Signin
		String newMediumPassword = faker.internet().password(8, 12, true, true, false);
		response = pageObj.endpoints().basicAuthChangePassword(client, token, mediumPassword, newMediumPassword);
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		response = pageObj.endpoints().basicAuthSignIn(client, userEmail, newMediumPassword);
		intUtils.validateGIS_SignUp_SignIn_Response(response);
		token = response.jsonPath().get("data.access_token");
		intUtils.verifyGISAccessTokenWithPunchhAPIs(client, secret, token);
		utils.logPass("Verified change password with medium password policy.");

		// set password policy to weak
		String weakPassword = faker.internet().password(6, 8, false, false, false);
		pageObj.cockpitDashboardMiscPage().selectPasswordPolicy(MessagesConstants.guestBasicAuthPPWeak);
		userEmail = intUtils.getRandomGmailEmail();
		userPayload = new DynamicPayloadBuilder();
		userPayload.setEmail(userEmail).setPassword(weakPassword).setPassword_confirmation(weakPassword)
				.setTerms_and_conditions(true).setPrivacy_policy(true);
		response = pageObj.endpoints().basicAuthSignUp(client, userPayload.buildPayloadMap());
		intUtils.validateGIS_SignUp_SignIn_Response(response);
		intUtils.basicAuthSignUpSignInDBValidations(userEmail, "sign_up");
		token = response.jsonPath().get("data.access_token");

		// Change password and validate with Signin
		String newWeakPassword = faker.internet().password(6, 8, false, false, false);
		response = pageObj.endpoints().basicAuthChangePassword(client, token, weakPassword, newWeakPassword);
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		response = pageObj.endpoints().basicAuthSignIn(client, userEmail, newWeakPassword);
		intUtils.validateGIS_SignUp_SignIn_Response(response);
		token = response.jsonPath().get("data.access_token");
		intUtils.verifyGISAccessTokenWithPunchhAPIs(client, secret, token);
		utils.logPass("Verified change password with weak password policy.");

		// set password policy back to strong password
		pageObj.cockpitDashboardMiscPage().selectPasswordPolicy(MessagesConstants.guestBasicAuthPPStrong);
	}

	@Test(description = "SQ-T6965 INT2-2105 | Test reset Password : Spec + Build | Case 1: Positive", priority = 10)
	@Owner(name = "Vansham Mishra")
	public void T6965_basicAuthResetPasswordPositiveFlow() throws Exception {
		// login to Punchh
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
        intUtils.updateAdvanceAndBasicAuthConfig(false, true);

		// set password policy to strong password
		pageObj.cockpitDashboardMiscPage().selectPasswordPolicy(MessagesConstants.guestBasicAuthPPStrong);
		String strongPassword = "1A@" + faker.internet().password();

		// Strong password signup and validate with Punchh APIs
		DynamicPayloadBuilder userPayload = new DynamicPayloadBuilder();
		userPayload.setEmail(userEmail).setPassword(strongPassword).setPassword_confirmation(strongPassword)
				.setTerms_and_conditions(true).setPrivacy_policy(true);
		Response response = pageObj.endpoints().basicAuthSignUp(client, userPayload.buildPayloadMap());
		intUtils.validateGIS_SignUp_SignIn_Response(response);
		intUtils.basicAuthSignUpSignInDBValidations(userEmail, "sign_up");

		// Forgot password and validate with Reset password and Signin
		response = pageObj.endpoints().basicAuthForgotPassword(client, userEmail, "true");
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String resetToken = response.jsonPath().get("data.reset_token");

		String newStrongPassword = "1A@" + faker.internet().password();
		response = pageObj.endpoints().basicAuthResetPassword(client, resetToken, newStrongPassword);
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		response = pageObj.endpoints().basicAuthSignIn(client, userEmail, newStrongPassword);
		intUtils.validateGIS_SignUp_SignIn_Response(response);
		intUtils.basicAuthSignUpSignInDBValidations(userEmail, "sign_in");
		String token = response.jsonPath().get("data.access_token");
		intUtils.verifyGISAccessTokenWithPunchhAPIs(client, secret, token);

		// set password policy to medium
		String mediumPassword = faker.internet().password(8, 12, true, true, false);
		pageObj.cockpitDashboardMiscPage().selectPasswordPolicy(MessagesConstants.guestBasicAuthPPModerate);
		String userEmail = intUtils.getRandomGmailEmail();
		userPayload.setEmail(userEmail).setPassword(mediumPassword).setPassword_confirmation(mediumPassword)
				.setTerms_and_conditions(true).setPrivacy_policy(true);
		response = pageObj.endpoints().basicAuthSignUp(client, userPayload.buildPayloadMap());
		intUtils.validateGIS_SignUp_SignIn_Response(response);
		intUtils.basicAuthSignUpSignInDBValidations(userEmail, "sign_up");

		// Forgot password and validate with Reset password and Signin
		response = pageObj.endpoints().basicAuthForgotPassword(client, userEmail, "true");
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		resetToken = response.jsonPath().get("data.reset_token");

		String newMediumPassword = faker.internet().password(8, 12, true, true, false);
		response = pageObj.endpoints().basicAuthResetPassword(client, resetToken, newMediumPassword);
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		response = pageObj.endpoints().basicAuthSignIn(client, userEmail, newMediumPassword);
		intUtils.validateGIS_SignUp_SignIn_Response(response);
		intUtils.basicAuthSignUpSignInDBValidations(userEmail, "sign_in");
		token = response.jsonPath().get("data.access_token");
		intUtils.verifyGISAccessTokenWithPunchhAPIs(client, secret, token);

		// set password policy to weak
		String weakPassword = faker.internet().password(6, 8, false, false, false);
		pageObj.cockpitDashboardMiscPage().selectPasswordPolicy(MessagesConstants.guestBasicAuthPPWeak);
		userEmail = intUtils.getRandomGmailEmail();

		userPayload.setEmail(userEmail).setPassword(weakPassword).setPassword_confirmation(weakPassword)
				.setTerms_and_conditions(true).setPrivacy_policy(true);
		response = pageObj.endpoints().basicAuthSignUp(client, userPayload.buildPayloadMap());
		intUtils.validateGIS_SignUp_SignIn_Response(response);
		intUtils.basicAuthSignUpSignInDBValidations(userEmail, "sign_up");

		// Forgot password and validate with Reset password and Signin
		response = pageObj.endpoints().basicAuthForgotPassword(client, userEmail, "true");
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		resetToken = response.jsonPath().get("data.reset_token");

		String newWeakPassword = faker.internet().password(6, 8, false, false, false);
		response = pageObj.endpoints().basicAuthResetPassword(client, resetToken, newWeakPassword);
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		response = pageObj.endpoints().basicAuthSignIn(client, userEmail, newWeakPassword);
		intUtils.validateGIS_SignUp_SignIn_Response(response);
		intUtils.basicAuthSignUpSignInDBValidations(userEmail, "sign_in");
		token = response.jsonPath().get("data.access_token");
		intUtils.verifyGISAccessTokenWithPunchhAPIs(client, secret, token);

		pageObj.cockpitDashboardMiscPage().selectPasswordPolicy(MessagesConstants.guestBasicAuthPPStrong);
	}

	@Test(description = "SQ-T6966 INT2-2105 | Test reset Password : Spec + Build | Case 2:- Negative" +
            "SQ-T7409 INT2-2475 - Identity: [Punchh - identity]- Verify the error message and code when user is deactivated and error message is configured at punchh.", priority = 10)
	@Owner(name = "Vansham Mishra")
	public void T6966_basicAuthResetPasswordNegativeFlow() throws Exception {

		String strongPassword = "1A@" + faker.internet().password();
        // login to Punchh
        pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
        pageObj.instanceDashboardPage().loginToInstance();
        pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
        intUtils.updateAdvanceAndBasicAuthConfig(false, true);
		// Strong password signup and validate with Punchh APIs
		DynamicPayloadBuilder userPayload = new DynamicPayloadBuilder();
		userPayload.setEmail(userEmail).setPassword(strongPassword).setPassword_confirmation(strongPassword)
				.setTerms_and_conditions(true).setPrivacy_policy(true);
		Response response = pageObj.endpoints().basicAuthSignUp(client, userPayload.buildPayloadMap());
		intUtils.validateGIS_SignUp_SignIn_Response(response);
		intUtils.basicAuthSignUpSignInDBValidations(userEmail, "sign_up");

		// Forgot password and validate with Reset password and Signin
		response = pageObj.endpoints().basicAuthForgotPassword(client, userEmail, "true");
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String resetToken = response.jsonPath().get("data.reset_token");

		String newStrongPassword = faker.internet().password(256, 260, true, true, true);
		response = pageObj.endpoints().basicAuthResetPassword(client, resetToken, newStrongPassword);
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		response = pageObj.endpoints().basicAuthSignIn(client, userEmail, newStrongPassword);
		intUtils.validateGIS_SignUp_SignIn_Response(response);
		intUtils.basicAuthSignUpSignInDBValidations(userEmail, "sign_in");
		String token = response.jsonPath().get("data.access_token");
		intUtils.verifyGISAccessTokenWithPunchhAPIs(client, secret, token);

		// When reset token used again
		response = pageObj.endpoints().basicAuthResetPassword(client, resetToken, newStrongPassword);
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY);
		String errorMessage = response.jsonPath().getString("errors.reset_token[0]");
		Assert.assertEquals(errorMessage, "Invalid or expired reset token",
				"Error message did not match for invalid/expired reset token");
		utils.logPass("Verified the error message for invalid/expired reset token.");
		// reset the password 5 times
		for (int i = 0; i < 6; i++) {
			// Forgot password and validate with Reset password and Signin
			response = pageObj.endpoints().basicAuthForgotPassword(client, userEmail, "true");
			Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
			resetToken = response.jsonPath().get("data.reset_token");

			newStrongPassword = "1A@" + faker.internet().password();
			response = pageObj.endpoints().basicAuthResetPassword(client, resetToken, newStrongPassword);
			Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
			response = pageObj.endpoints().basicAuthSignIn(client, userEmail, newStrongPassword);
			intUtils.validateGIS_SignUp_SignIn_Response(response);
			intUtils.basicAuthSignUpSignInDBValidations(userEmail, "sign_in");
			token = response.jsonPath().get("data.access_token");
			intUtils.verifyGISAccessTokenWithPunchhAPIs(client, secret, token);
		}

		// 6th time should give error
		response = pageObj.endpoints().basicAuthForgotPassword(client, userEmail, "true");
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		resetToken = response.jsonPath().get("data.reset_token");
		response = pageObj.endpoints().basicAuthResetPassword(client, resetToken, newStrongPassword);
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY);
		errorMessage = response.jsonPath().getString("errors.password[0]");
		Assert.assertEquals(errorMessage, "Password has been used recently. Please choose a different password.",
				"Error message did not matched");
		utils.logPass("Verified the error message for recently used password.");

		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().navigateToTabs("Edit Profile");
		pageObj.guestTimelinePage().deactivateReactivateUser("Deactivate");

		newStrongPassword = "1A@" + faker.internet().password();
		response = pageObj.endpoints().basicAuthForgotPassword(client, userEmail, "true");
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_FORBIDDEN);
		String userDeactivatedErrorMsg = response.jsonPath().get("errors.user_deactivated[0]");
		Assert.assertEquals(userDeactivatedErrorMsg, MessagesConstants.deactivatedUserMessage,
				"Error message did not match for deactivated user during forgot password");
		utils.logPass("Verified the error message for deactivated user during forgot password.");
        response = pageObj.endpoints().basicAuthSignUp(client, userPayload.buildPayloadMap());
        Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_FORBIDDEN);
        userDeactivatedErrorMsg = response.jsonPath().get("errors.deactivated_user[0]");
        Assert.assertEquals(userDeactivatedErrorMsg, MessagesConstants.deactivatedUserMessage,
                "Error message did not match for deactivated user during sign up and password_less/verify api");
        utils.logit("PASS","Verified the error message for deactivated user during sign up and password_less/verify api");


		// reset Password with invalid token
		String invalidResetToken = resetToken + "1234";
		response = pageObj.endpoints().basicAuthResetPassword(client, invalidResetToken, newStrongPassword);
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY);
		errorMessage = response.jsonPath().getString("errors.reset_token[0]");
		Assert.assertEquals(errorMessage, "Invalid or expired reset token",
				"Error message did not match for invalid reset token during reset password");
		utils.logPass("Verified the error message for invalid reset token during reset password.");

		// reset password with expired token
		response = pageObj.endpoints().basicAuthResetPassword(client, dataSet.get("expiredToken"), newStrongPassword);
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY);
		errorMessage = response.jsonPath().getString("errors.reset_token[0]");
		Assert.assertEquals(errorMessage, "Invalid or expired reset token",
				"Error message did not match for invalid reset token during reset password");
		utils.logPass("Verified the error message for expired reset token during reset password.");
	}

	@Test(description = "SQ-T6949 INT2-2628| Punchh - identity service - Add basic auth flag and required fields in the under cockpit Tab and sync with the identity service", priority = 11)
	@Owner(name = "Vansham Mishra")
	public void T6949_validateBasicAuthSyncWithIdentityService() throws Exception {
		// login to Punchh
		int accessTokenExpiration = Utilities.getRandomNoFromRange(5, 10);
		int refreshTokenExpiration = Utilities.getRandomNoFromRange(1, 5);
		// convert transaction_no to string

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guest Identity Management");
		intUtils.updateAdvanceAndBasicAuthConfig(true, true);

		// set password policy to strong password
		pageObj.cockpitDashboardMiscPage().selectPasswordPolicy(MessagesConstants.guestBasicAuthPPStrong);
		pageObj.cockpitDashboardMiscPage().setSelectedNumericFieldValues(" Access Token Expiration (Minutes)",
				String.valueOf(accessTokenExpiration));
		pageObj.cockpitDashboardMiscPage().setSelectedNumericFieldValues(" Refresh Token Expiration (Days)",
				String.valueOf(refreshTokenExpiration));
		String query1 = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.slug='"
				+ dataSet.get("slug") + "';";
		String preferences = DBUtils.executeQueryAndGetColumnValue(env, query1, "preferences");

		List<String> keyValueFromPreferences = Utilities.getPreferencesKeyValue(preferences, "enable_basic_auth");
		// verify enable_basic_auth is true in preferences
		Assert.assertEquals(keyValueFromPreferences.get(0), "true",
				"Value of enable_basic_auth in preferences is not true");
		utils.logit("Value of enable_basic_auth in preferences is: " + keyValueFromPreferences.get(0));

		// verify that access_token_expiration_basic_auth should be same as
		// accessTokenExpiration
		keyValueFromPreferences = Utilities.getPreferencesKeyValue(preferences, "access_token_expiration_basic_auth");
		Assert.assertEquals(keyValueFromPreferences.get(0), String.valueOf(accessTokenExpiration),
				"Value of access_token_expiration_basic_auth in preferences is not matching with UI value");
		utils.logit("Value of access_token_expiration_basic_auth in preferences is: " + keyValueFromPreferences.get(0));

		// verify that password_policy_basic_auth should be strong_password
		keyValueFromPreferences = Utilities.getPreferencesKeyValue(preferences, "password_policy_basic_auth");
		Assert.assertEquals(keyValueFromPreferences.get(0), "strong_password",
				"Value of password_policy_basic_auth in preferences is not strong_password");
		utils.logit("Value of password_policy_basic_auth in preferences is: " + keyValueFromPreferences.get(0));

		// verify that refresh_token_expiration_basic_auth should be same as
		// refreshTokenExpiration
		keyValueFromPreferences = Utilities.getPreferencesKeyValue(preferences, "refresh_token_expiration_basic_auth");
		Assert.assertEquals(keyValueFromPreferences.get(0), String.valueOf(refreshTokenExpiration),
				"Value of refresh_token_expiration_basic_auth in preferences is not matching with UI value");
		utils.logit(
				"Value of refresh_token_expiration_basic_auth in preferences is: " + keyValueFromPreferences.get(0));

		// access_token_expiration should be same as accessTokenExpiration
		String guestDbPreferences = DBUtils.executeQueryAndGetColumnValue(env, guestIdentityhost, query1,
				"preferences");
		keyValueFromPreferences = Utilities.getPreferencesKeyValue(guestDbPreferences, "access_token_expiration");
		Assert.assertEquals(keyValueFromPreferences.get(0), String.valueOf(accessTokenExpiration),
				"Value of access_token_expiration in preferences is not matching with UI value");
		utils.logit("Value of access_token_expiration in guest db business preferences is: "
				+ keyValueFromPreferences.get(0));

		// refresh_token_expiration should be same as refreshTokenExpiration
		keyValueFromPreferences = Utilities.getPreferencesKeyValue(guestDbPreferences, "refresh_token_expiration");
		Assert.assertEquals(keyValueFromPreferences.get(0), String.valueOf(refreshTokenExpiration),
				"Value of refresh_token_expiration in preferences is not matching with UI value");
		utils.logit("Value of refresh_token_expiration in guest db business preferences is: "
				+ keyValueFromPreferences.get(0));

		// password_policy should be strong_password
		keyValueFromPreferences = Utilities.getPreferencesKeyValue(preferences, "password_policy_basic_auth");
		Assert.assertEquals(keyValueFromPreferences.get(0), "strong_password",
				"Value of password_policy_basic_auth in preferences is not strong_password");
		utils.logit("Value of password_policy in preferences is: " + keyValueFromPreferences.get(0));

		// verify that enale_basic_auth should be 1 in guest db business_mapping table
		String query2 = "select Count(*) as count from business_mappings where slug='" + dataSet.get("slug")
				+ "' and enable_basic_auth='1';";
		String count = DBUtils.executeQueryAndGetColumnValue(env, guestIdentityhost, query2, "count");
		Assert.assertEquals(count, "1", "enable_basic_auth is not 1 in guest db business_mapping table");
		// set expiration field values
		pageObj.cockpitDashboardMiscPage().setSelectedNumericFieldValues(" Access Token Expiration (Minutes)", "5");
		pageObj.cockpitDashboardMiscPage().setSelectedNumericFieldValues(" Refresh Token Expiration (Days)", "1");

		utils.logit("pass", "Verified that basic auth is enabled in guest db business_mapping table");
	}

	@Test(description = "SQ-T6950 INT2-2627 | Add enable Basic authentication in Oauth App creation/Updation.")
	@Owner(name = "Vansham Mishra")
	public void T6950_verifyOAuthApplicationCreateUpdateWithBasicAuthFlag() throws Exception {
		String slug = dataSet.get("slug");
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(slug);
		intUtils.updateAdvanceAndBasicAuthConfig(false, true);
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "OAuth Apps");

		// Create a new oauth application
		String oauthAppName = "AutoTest Oauth App " + CreateDateTime.getTimeDateAsneed("yyyyMMddHHmmss");
		createOAuthApplication(oauthAppName, baseUrl, "Mobile App - iOS", "Advance Auth", "PAR Loyalty",
				MessagesConstants.oauthAppCreateSuccessMsg);

		verifyOAuthAppBasicAuthFlagInDB(oauthAppName, "true", "true", slug);
		utils.logit("OAuth application created with Advance Auth scope.");

		// update the name of the created oauth application
		String updatedOauthAppName = oauthAppName + " Updated";
		pageObj.oAuthAppPage().enterAppName(updatedOauthAppName);
		pageObj.oAuthAppPage().clickSave();
		pageObj.oAuthAppPage().verifyDisplayedMessage(MessagesConstants.oauthAppUpdateSuccessMsg);
		verifyOAuthAppBasicAuthFlagInDB(updatedOauthAppName, "true", "true", slug);
		utils.logPass("Verified that user can update any new OAuth app with scope as Advanced Auth.");
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "OAuth Apps");
		pageObj.oAuthAppPage().deleteOauthAppByName(updatedOauthAppName);
		utils.logPass("Verified that user can update any new OAuth app with scope as Advanced Auth.");
		pageObj.oAuthAppPage().verifyDisplayedMessage(MessagesConstants.oauthAppDeleteSuccessMsg);
		utils.logit("Pass", "Created OAuthApp deleted");

		intUtils.updateAdvanceAndBasicAuthConfig(false, false);
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "OAuth Apps");
		oauthAppName = "AutoTest Oauth App " + CreateDateTime.getTimeDateAsneed("yyyyMMddHHmmss");
		createOAuthApplication(oauthAppName, baseUrl, "Mobile App - iOS", "Advance Auth", "PAR Loyalty",
				MessagesConstants.GISEnablementRequiredMsg);
		verifyOAuthAppBasicAuthFlagInDB(oauthAppName, "false", "false", slug);
		utils.logit(
				"Verified that user cannot create OAuth app with scope as Advanced Auth when both Basic Auth and Advance Auth are disabled.");
		updatedOauthAppName = oauthAppName + " Updated2";
		pageObj.oAuthAppPage().enterAppName(updatedOauthAppName);
		pageObj.oAuthAppPage().clickSave();
		pageObj.oAuthAppPage().verifyDisplayedMessage(MessagesConstants.GISEnablementRequiredMsg);
		verifyOAuthAppBasicAuthFlagInDB(updatedOauthAppName, "false", "false", slug);
		utils.logit(
				"Verified that user cannot update any existing OAuth app with scope as Advanced Auth when both Basic Auth and Advance Auth are disabled.");
	}

	@DataProvider(name = "upsertUserAndSetAdminPassword")
	public Object[][] upsertUserAndSetAdminPassword() {
		return new Object[][] {
				// SignupNamespace, SignupUserType,
				{ "auth", "EmailOnly" }, { "auth", "EmailPhone" }, };
	}

	@Test(description = "SQ-T6979 INT2-2599 - Upsert User on identity with Admin change password", dataProvider = "upsertUserAndSetAdminPassword")
	@Owner(name = "Vansham Mishra")
	public void T6979_upsertUserAndSetAdminPasswordWithBasicAuth(String signupNamespace, String signupUserType)
			throws Exception {
		if (signupUserType.equalsIgnoreCase("EmailOnly")) {
			userEmail = utils.getConfigProperty("gmail.username") + "+EmailOnlyUpsert"
					+ utils.getTimestampInNanoseconds() + "@gmail.com";
		} else {
			userEmail = utils.getConfigProperty("gmail.username") + "+EmailPhoneUpsert"
					+ utils.getTimestampInNanoseconds() + "@gmail.com";
		}
		// Punchh SignUp
		String phone = dataSet.get("phoneNumber");
		String locationKey = dataSet.get("locationKey");

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().navigateToTabs("Guest Validation");
		pageObj.dashboardpage().checkUncheckAnyFlag("Send Email Verification", "check");
		// Punchh SignUp
		intUtils.userSignUpPunchh(signupNamespace, signupUserType, client, secret, userEmail, phone, locationKey);
		// STEP 2: Extract OTP from Email
		String emailBody = GmailConnection.getGmailEmailBody("toAndFromEmail", userEmail + "," + "help@punchh.com",
				true);
		List<String> urls = intUtils.extractUrls(emailBody);
		Assert.assertTrue(!urls.isEmpty(), "No URLs found in the email body");
		pageObj.instanceDashboardPage().confirmEmail(urls.get(0));
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		String updatedPassword = "2A@" + faker.internet().password();
		pageObj.guestTimelinePage().UpdateUserPWDFromTimeline(updatedPassword);

        // Sign in and validate with Punchh APIs
		Response response = pageObj.endpoints().basicAuthSignIn(client, userEmail, updatedPassword);
		intUtils.validateGIS_SignUp_SignIn_Response(response);
		String token = response.jsonPath().get("data.access_token");
		intUtils.verifyGISAccessTokenWithPunchhAPIs(client, secret, token);
		intUtils.basicAuthSignUpSignInDBValidations(userEmail, "sign_in");
		verifyPasswordHistory(userEmail);
		utils.logPass("Verified the Change Password functionality from UI and able to login with updated password from Dashboard successfully");
	}

	public void verifyOAuthAppBasicAuthFlagInDB(String oauthAppName, String expectedBasicAuthFlag,
			String advanceAuthFlag, String businessSlug) throws Exception {

		String basicAuth = "0";
		if (expectedBasicAuthFlag.equalsIgnoreCase("true")) {
			basicAuth = "1";
		}

		String query = "Select Count(*) as count from oauth_applications where name='" + oauthAppName
				+ "' and scopes='advance_auth';";
		String count = DBUtils.executeQueryAndGetColumnValue(env, query, "count");
		Assert.assertEquals(count, "" + basicAuth + "",
				"OAuth application basic auth flag did not match in oauth_applications table");
		utils.logPass("Verified that valid entry has been created in punchh oauth_applications table");

		// verify the entry in guest identity oauth_applications table
		count = DBUtils.executeQueryAndGetColumnValue(env, guestIdentityhost, query, "count");
		Assert.assertEquals(count, "" + basicAuth + "",
				"OAuth application basic auth flag did not match in guest identity oauth_applications table");
		utils.logPass("Verified that valid entry has been created in guest identity oauth_applications table");
		utils.logit("pass", "Verified the entry in guest db business_mapping table");
	}

	public void createOAuthApplication(String appName, String redirectUri, String applicationType, String scope,
			String businessUnit, String message) {
		pageObj.oAuthAppPage().clickNewApplication();
		pageObj.oAuthAppPage().enterAppName(appName);
		pageObj.oAuthAppPage().enterRedirectUri(redirectUri);
		pageObj.oAuthAppPage().selectApplicationType(applicationType);
		pageObj.oAuthAppPage().selectScope(scope);
		pageObj.oAuthAppPage().selectBusinessUnit(businessUnit);
		pageObj.oAuthAppPage().clickSave();
		pageObj.oAuthAppPage().verifyDisplayedMessage(message);
	}

	public void verifyPasswordHistory(String email) throws Exception {
		// get punchh user id
		String query = "SELECT id FROM users WHERE email='" + email + "';";
		String punchhUserId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");

		// check password history entries using punchh user id
		String historyQuery = "SELECT COUNT(*) as count FROM password_histories WHERE resource_id='" + punchhUserId
				+ "';";
		String count = DBUtils.executeQueryAndGetColumnValue(env, historyQuery, "count");
		Assert.assertEquals(count, "0", "Password history entries are not as expected in password_histories table");
		utils.logit(" Verified in punch password_histories table for user: " + email);

		// get guest identity user id
		String guestIdentityUserId = DBUtils.executeQueryAndGetColumnValue(env, guestIdentityhost, query, "id");
		// check password history entries using guest identity user id
		historyQuery = "SELECT COUNT(*) as count FROM password_histories WHERE user_id='" + guestIdentityUserId + "';";
		count = DBUtils.executeQueryAndGetColumnValue(env, guestIdentityhost, historyQuery, "count");
		Assert.assertEquals(count, "1",
				"Password history entries are not as expected in guest identity password_histories table");
		utils.logit(" Verified in guest identity password_histories table for user: " + email);
	}

	@Test(description = "SQ-T7194 INT2-2724 | JTI for access token | advance auth | basic auth")
	@Owner(name = "Vansham Mishra")
	public void T7194_verifyJTIAccessTokenForBasicAndAdvanceAuth() throws Exception {
        pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
        pageObj.instanceDashboardPage().loginToInstance();
        pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
        intUtils.updateAdvanceAndBasicAuthConfig(true, true);
		// Strong password signup and validate with Punchh APIs
		String strongPassword = "1A@" + faker.internet().password();
		DynamicPayloadBuilder userPayload = new DynamicPayloadBuilder();
		userPayload.setEmail(userEmail).setPassword(strongPassword).setPassword_confirmation(strongPassword)
				.setTerms_and_conditions(true).setPrivacy_policy(true);
		Response response = pageObj.endpoints().basicAuthSignUp(client, userPayload.buildPayloadMap());
		intUtils.validateGIS_SignUp_SignIn_Response(response);
		String accessToken = response.jsonPath().get("data.access_token");
		String payload = utils.decodeJwtPayload(accessToken);
		verifyJtiInJwtPayload(payload);
		String email = intUtils.getRandomGmailEmail();
		String[] tokens = intUtils.userSignUpSignInAdvanceAuth(client, email, "internal_mobile_app", "SignUp");
		accessToken = tokens[0];
		// access token should not be null
		Assert.assertNotNull(accessToken, "Access token is null for advance auth signup/signin");
		payload = utils.decodeJwtPayload(accessToken);
		verifyJtiInJwtPayload(payload);
	}

	public void verifyJtiInJwtPayload(String payload) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		if (payload == null) {
			Assert.fail("JWT payload is null");
		}
		JsonNode root = mapper.readTree(payload);
		Assert.assertTrue(root.has("jti"), "jti is not present in JWT payload");
		JsonNode jtiNode = root.get("jti");
		Assert.assertNotNull(jtiNode, "jti node is null in JWT payload");
		String jti = jtiNode.asText();
		Assert.assertFalse(jti == null || jti.trim().isEmpty(), "jti value is null or empty in JWT payload");
		utils.logit("PASS", "Verified that jti is present in JWT payload with value: " + jti);
	}

	@Test(description = "SQ-T6982 Basic Auth | Verify that proper error message and code should be there in all Basic Auth apis when Basic Auth is OFF")
	@Owner(name = "Vansham Mishra")
	public void T6982_verifyBasicAuthApisReturnProperErrorWhenDisabled() throws Exception {
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		intUtils.updateAdvanceAndBasicAuthConfig(false, false);
		String mediumPassword = faker.internet().password(8, 12, true, true, false);
		String strongPassword = "1A@" + faker.internet().password();

		// Weak/Medium password
		DynamicPayloadBuilder userPayload = new DynamicPayloadBuilder();
		userPayload.setEmail(userEmail).setPassword(mediumPassword).setPassword_confirmation(mediumPassword)
				.setTerms_and_conditions(true).setPrivacy_policy(true);

		// Validate error response for weak/medium password
		Response response = pageObj.endpoints().basicAuthSignUp(client, userPayload.buildPayloadMap());
		Assert.assertTrue(response.getStatusCode() == ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Expected status code: " + ApiConstants.HTTP_STATUS_PRECONDITION_FAILED + ", but got: "
						+ response.getStatusCode());
		String errorMessage = response.jsonPath().getString("errors.basic_authentication_disabled[0]");
		Assert.assertEquals(errorMessage, MessagesConstants.disabledBasicAuthErrorMessage,
				"Error message did not match for disabled basic auth signup api");
		utils.logPass("Verified the error message for disabled basic auth with signup api");

		// Sign in and validate with Punchh APIs
		response = pageObj.endpoints().basicAuthSignIn(client, userEmail, strongPassword);
		Assert.assertTrue(response.getStatusCode() == ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Expected status code: " + ApiConstants.HTTP_STATUS_PRECONDITION_FAILED + ", but got: "
						+ response.getStatusCode());
		errorMessage = response.jsonPath().getString("errors.basic_authentication_disabled[0]");
		Assert.assertEquals(errorMessage, MessagesConstants.disabledBasicAuthErrorMessage,
				"Error message did not match for disabled basic auth signin api");
		utils.logPass("Verified the error message for disabled basic auth with signin api");

		// Refresh token and validate with Punchh APIs
		response = pageObj.endpoints().basicAuthRefresh(client, "refreshToken");
		Assert.assertTrue(response.getStatusCode() == ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Expected status code: " + ApiConstants.HTTP_STATUS_PRECONDITION_FAILED + ", but got: "
						+ response.getStatusCode());
		errorMessage = response.jsonPath().getString("errors.identity_service_not_enabled[0]");
		Assert.assertEquals(errorMessage, MessagesConstants.identityServiceNotEnabledMsg,
				"Error message did not match for disabled basic auth refresh token api");
		utils.logPass("Verified the error message for disabled basic auth with refresh token api");

		// Change password and validate with Signin
		String newStrongPassword = "1A@" + faker.internet().password();
		response = pageObj.endpoints().basicAuthChangePassword(client, "token", strongPassword, newStrongPassword);
		Assert.assertTrue(response.getStatusCode() == ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Expected status code: " + ApiConstants.HTTP_STATUS_PRECONDITION_FAILED + ", but got: "
						+ response.getStatusCode());
		errorMessage = response.jsonPath().getString("errors.basic_authentication_disabled[0]");
		Assert.assertEquals(errorMessage, MessagesConstants.disabledBasicAuthErrorMessage,
				"Error message did not match for disabled basic auth change password api");
		utils.logPass("Verified the error message for disabled basic auth with change password api");

		// Forgot password and validate with Reset password and Signin
		response = pageObj.endpoints().basicAuthForgotPassword(client, userEmail, "true");
		Assert.assertTrue(response.getStatusCode() == ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Expected status code: " + ApiConstants.HTTP_STATUS_PRECONDITION_FAILED + ", but got: "
						+ response.getStatusCode());
		errorMessage = response.jsonPath().getString("errors.basic_authentication_disabled[0]");
		Assert.assertEquals(errorMessage, MessagesConstants.disabledBasicAuthErrorMessage,
				"Error message did not match for disabled basic auth forgot password api");
		utils.logPass("Verified the error message for disabled basic auth with forgot password api");

		newStrongPassword = "1A@" + faker.internet().password();
		response = pageObj.endpoints().basicAuthResetPassword(client, "resetToken", newStrongPassword);
		Assert.assertTrue(response.getStatusCode() == ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Expected status code: " + ApiConstants.HTTP_STATUS_PRECONDITION_FAILED + ", but got: "
						+ response.getStatusCode());
		errorMessage = response.jsonPath().getString("errors.basic_authentication_disabled[0]");
		Assert.assertEquals(errorMessage, MessagesConstants.disabledBasicAuthErrorMessage,
				"Error message did not match for disabled basic auth reset password api");
		utils.logPass("Verified the error message for disabled basic auth with reset password api");

		// Sign out and validate with Punchh API
		response = pageObj.endpoints().basicAuthSignOut(client, "token", "refreshToken");
		Assert.assertTrue(response.getStatusCode() == ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Expected status code: " + ApiConstants.HTTP_STATUS_PRECONDITION_FAILED + ", but got: "
						+ response.getStatusCode());
		errorMessage = response.jsonPath().getString("errors.identity_service_not_enabled[0]");
		Assert.assertEquals(errorMessage, MessagesConstants.identityServiceNotEnabledMsg,
				"Error message did not match for disabled basic auth sign out api");
		utils.logPass("Verified the error message for disabled basic auth with sign out api");
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}

}