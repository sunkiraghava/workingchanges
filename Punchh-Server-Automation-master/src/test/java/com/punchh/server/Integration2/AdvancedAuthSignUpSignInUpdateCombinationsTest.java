package com.punchh.server.Integration2;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.annotations.Owner;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class AdvancedAuthSignUpSignInUpdateCombinationsTest {
	public WebDriver driver;
	private PageObj pageObj;
	private String sTCName;
	private String env, run = "api";
	private IntUtils intUtils;
	private static Map<String, String> dataSet;
	private Utilities utils;
	private String guestIdentityhost = "guestIdentity";
	// Common test data shared among all tests
	private String client, secret;

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) throws Exception {
		sTCName = method.getName();
		intUtils = new IntUtils(driver);
		utils = new Utilities(driver);
		pageObj = new PageObj();
		env = pageObj.getEnvDetails().setEnv();
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath("ui", env, "Secrets"), dataSet.get("slug"));
		// Merge datasets without overwriting existing keys
		pageObj.readData().readTestData.forEach(dataSet::putIfAbsent);
		utils.logit("Using env as ==> " + env);
		utils.logit(sTCName + " ==>" + dataSet);

		client = dataSet.get("client");
		secret = dataSet.get("secret");
	}

	@DataProvider(name = "AA_Signup_AA_SignIn_Update")
	public Object[][] AA_Signup_AA_SignIn_Update() {
		return new Object[][] {
				// SignupClientType, SignupUserType, SigninClientType, UpdateNamespace, UpdateType
				{ "internal_mobile_app", "EmailOnly", "external_mobile_app", "api1", "Email" },
				{ "internal_mobile_app", "PhoneOnly", "online_ordering", "api1", "Email" },
				{ "external_mobile_app", "EmailOnly", "internal_mobile_app", "api2", "Phone" },
				{ "external_mobile_app", "PhoneOnly", "iframe", "api2", "Phone" },
				{ "online_ordering", "EmailOnly", "external_mobile_app", "auth", "EmailPhone" },
				{ "online_ordering", "PhoneOnly", "online_ordering", "auth", "EmailPhone" },
				{ "iframe", "EmailOnly", "internal_mobile_app", "api1", null },
				{ "iframe", "PhoneOnly", "iframe", "api2", null },
		};
	}

	@Test(description = "AA Signup → AA SignIn → Update"
			+ "SQ-T6731 Verify Advanced Authentication with Guest Auth DB flow end to end for the email only user Case-1"
			+ "SQ-T6732 Verify Advanced Authentication with Guest Auth DB flow end to end for the email only user Case-2"
			+ "SQ-T6733 Verify Advanced Authentication with Guest Auth DB flow end to end for the email only user Case-3"
			+ "SQ-T6734 Verify Advanced Authentication with Guest Auth DB flow end to end for the phone only user Case-1"
			+ "SQ-T6735 Verify Advanced Authentication with Guest Auth DB flow end to end for the phone only user Case-2"
			+ "SQ-T6736 Verify Advanced Authentication with Guest Auth DB flow end to end for the phone only user Case-3"
			+ "SQ-T6890 INT2-2265 | Validate metadata for email signup/signin."
			+ "SQ-T6893 INT2-2265 | Validate metadata for phone signup/sign in."
			+ "SQ-T6892 INT2-2265 | Validate last_synced_at updation in users table extra scenarios.", dataProvider = "AA_Signup_AA_SignIn_Update")
	@Owner(name = "Vansham Mishra")
	public void AA_Signup_AA_SignIn_Update_Test(String signupClientType, String signupUserType, String signinClientType,
			String updateNamespace, String updateType) throws Exception {

		// Signup channel
		String communicationChannel;
		if ("EmailOnly".equals(signupUserType)) {
			communicationChannel = intUtils.getRandomGmailEmail();
		} else {
			communicationChannel = dataSet.get("phoneNumber");
		}
		utils.logit("Using communicationChannel as ==> " + communicationChannel);
		// AdvanceAuth Signup
		String[] tokensSignUp = intUtils.userSignUpSignInAdvanceAuth(client, communicationChannel, signupClientType,
				"SignUp");

		// Verify AdvanceAuth tokens with Punchh APIs
		intUtils.verifyGISAccessTokenWithPunchhAPIs(client, secret, tokensSignUp[0]);

		// AdvanceAuth SignIn
		String[] tokensSignIn = intUtils.userSignUpSignInAdvanceAuth(client, communicationChannel, signinClientType,
				"SignIn");

		// Verify AdvanceAuth tokens with Punchh APIs
		intUtils.verifyGISAccessTokenWithPunchhAPIs(client, secret, tokensSignIn[0]);

		// Validate Refresh and sign out AdvanceAuth tokens
		intUtils.validateRefreshAndSignOutAdvanceAuth(client, secret, communicationChannel, tokensSignUp[1]);

		List<String> userIds = intUtils.validateGuestIdentityDbRecords(communicationChannel, signupUserType,
				signupClientType, dataSet.get("slug"), dataSet.get("business_id"));
		String previousLastSyncedAt = getLastSyncedAt(userIds.get(1));

		// Update user
		if (updateType != null && !updateType.isEmpty()) {
			intUtils.legacyUserUpdateWithAdvanceAuthTokens(client, secret, signupUserType, updateNamespace, updateType,
					tokensSignIn[0]);
		}

		// Validate guest identity updated values
		intUtils.validateUserSyncWithGIS(userIds.get(0));

		// Verify that sign_in_count should be incremented in punchh users table
		String signInCountQuery = "SELECT sign_in_count FROM users WHERE id = '" + userIds.get(0) + "'";
		String signInCountStr = DBUtils.executeQueryAndGetColumnValue(env, signInCountQuery, "sign_in_count");
		int signInCount = Integer.parseInt(signInCountStr);
		Assert.assertTrue(signInCount >= 1, "sign_in_count is not incremented in Punchh users table");
		utils.logit("sign_in_count is incremented in Punchh users table for PunchhUserId: " + userIds.get(0));

		String currentLastSyncedAt = getLastSyncedAt(userIds.get(1));
		if (updateType != null && !updateType.isEmpty()) {
			utils.logit(
					"previousLastSyncedAt: " + previousLastSyncedAt + ", currentLastSyncedAt: " + currentLastSyncedAt);
			Assert.assertNotEquals(currentLastSyncedAt, previousLastSyncedAt,
					"last_synced_at should be updated after user update");
			utils.logPass("last_synced_at updated successfully after user update");
		} else {
			utils.logit(
					"previousLastSyncedAt: " + previousLastSyncedAt + ", currentLastSyncedAt: " + currentLastSyncedAt);
			Assert.assertEquals(currentLastSyncedAt, previousLastSyncedAt,
					"last_synced_at should not be updated as no update performed");
			utils.logit("last_synced_at not updated as no update performed");
		}

		// Validate metadata in guest identity db
		intUtils.validateMetaDataInGuestIdentityDb(userIds.get(1));
	}

	// User type is EMAIL only here as Punchh Signin Possible with Email Only
	// Not checking update user here as update scenarios for AdvanceAuth SignUp are already covered with Advance Auth SignUp
	@DataProvider(name = "AA_Signup_Punchh_SignIn")
	public Object[][] AA_Signup_Punchh_SignIn() {
		return new Object[][] {
				// AA_SignupClientType, AA_SignupUserType, PunchhSigninNamespace
				{ "internal_mobile_app", "EmailOnly", "api1" },
				{ "external_mobile_app", "EmailOnly", "api1" },
				{ "online_ordering", "EmailOnly", "api1" },
				{ "internal_mobile_app", "EmailOnly", "api2" },
				{ "external_mobile_app", "EmailOnly", "api2" },
				{ "online_ordering", "EmailOnly", "api2" },
				{ "internal_mobile_app", "EmailOnly", "auth" },
				{ "external_mobile_app", "EmailOnly", "auth" },
				{ "online_ordering", "EmailOnly", "auth" },
				{ "iframe", "EmailOnly", "api1" },
		};
	}

	@Test(description = "AA Signup → Punchh SignIn", dataProvider = "AA_Signup_Punchh_SignIn")
	public void AA_Signup_Punchh_SignIn_Test(String signupClientType, String signupUserType, String signinNamespace)
			throws Exception {

		String userPassword = Utilities.getApiConfigProperty("password");

		// Signup channel
		String communicationChannel;
		if ("EmailOnly".equals(signupUserType)) {
			communicationChannel = intUtils.getRandomGmailEmail();
		} else {
			communicationChannel = dataSet.get("phoneNumber");
		}
		utils.logit("Using communicationChannel as ==> " + communicationChannel);

		// AdvanceAuth SignUp
		String[] tokens = intUtils.userSignUpSignInAdvanceAuth(client, communicationChannel, signupClientType,
				"SignUp");

		// Set password
		pageObj.endpoints().authApiChangePassword(tokens[0], client, secret, userPassword);
		// Punchh SignIn
		intUtils.userSignInPunchh(signinNamespace, client, secret, communicationChannel, userPassword);
		utils.logit("Pass", "AA Signup → Punchh SignIn test passed");
	}

	// Signup via Punchh api1, api2, auth is not possible for PhoneOnly
	// POS only user can't be updated
	@DataProvider(name = "Punchh_Signup_AA_SignIn_Update")
	public Object[][] Punchh_Signup_AA_SignIn_Update() {
		return new Object[][] {
				// SignupNamespace, SignupUserType, SigninClientType, UpdateNamespace, UpdateType
				{ "api1", "EmailOnly", "internal_mobile_app", "api1", "Email" },
				{ "api1", "EmailPhone", "online_ordering", "api2", "EmailPhone" },
				{ "api2", "EmailOnly", "iframe", "auth", "Phone" },
				{ "api2", "EmailPhone", "external_mobile_app", "api1", "Email" },
				{ "auth", "EmailOnly", "online_ordering", "api2", "Phone" },
				{ "auth", "EmailPhone", "internal_mobile_app", "auth", "EmailPhone" },
				{ "pos",  "EmailOnly", 	"external_mobile_app", 	"api1", "Phone" },
				{ "pos", "PhoneOnly", "online_ordering", "api2", "Email" },
				{ "pos", "PhoneOnly", "internal_mobile_app", "api2", "Phone" },
				{ "pos", "PhoneOnly", "external_mobile_app", "api2", "EmailPhone" },
				{ "pos", "EmailPhone", "iframe", "auth", "EmailPhone" }, 
		};
	}

	@Test(description = "Punchh Signup → AA SignIn → Update"
			+ "SQ-T6750 Verify Advanced Auth SignUp/ SignIn flow for the Guest Identity when email only user present in the downstream system"
			+ "SQ-T6751 Verify Advanced Auth SignUp/ SignIn flow for the Guest Identity when phone only user present in the downstream system", dataProvider = "Punchh_Signup_AA_SignIn_Update")
	public void Punchh_Signup_AA_SignIn_Update_Test(String signupNamespace, String signupUserType,
			String signinClientType, String updateNamespace, String updateType) throws Exception {

		String email = intUtils.getRandomGmailEmail();
		String phone = dataSet.get("phoneNumber");
		String locationKey = dataSet.get("locationKey");

		// Punchh SignUp
		intUtils.userSignUpPunchh(signupNamespace, signupUserType, client, secret, email, phone, locationKey);

		// AdvanceAuth SignIn
		String[] tokens = null;
		switch (signupUserType) {
		case "EmailOnly":
			tokens = intUtils.userSignUpSignInAdvanceAuth(client, email, signinClientType, "SignIn");
			break;
		case "PhoneOnly":
			tokens = intUtils.userSignUpSignInAdvanceAuth(client, phone, signinClientType, "SignIn");
			break;
		case "EmailPhone":
			intUtils.userSignUpSignInAdvanceAuth(client, email, signinClientType, "SignIn");
			tokens = intUtils.userSignUpSignInAdvanceAuth(client, phone, signinClientType, "SignIn");
			break;
		}
		
		// Signup channel
		String communicationChannel;
		if ("EmailOnly".equals(signupUserType)) {
			communicationChannel = email;
		} else {
			communicationChannel = dataSet.get("phoneNumber");
		}
		utils.logit("Using communicationChannel as ==> " + communicationChannel);
		intUtils.validateGuestIdentityDbRecords(communicationChannel, signupUserType, signinClientType,
				dataSet.get("slug"), dataSet.get("business_id") + "_downstream");
		intUtils.legacyUserUpdateWithAdvanceAuthTokens(client, secret, signupUserType, updateNamespace, updateType,
				tokens[0]);
		utils.logit("Pass", "Punchh Signup → AA SignIn → Update test passed");
	}

	// Signup via Punchh api1, api2, auth is not possible for PhoneOnly
	// POS only user can't be updated
	@DataProvider(name = "Punchh_Signup_Punchh_SignIn_Update")
	public Object[][] Punchh_Signup_Punchh_SignIn_Update() {
		return new Object[][] {
				// SignupNamespace, SignupUserType(with email), SigninNamespace, UpdateNamespace, UpdateType
				{ "api1", "EmailOnly", "api1", "auth", "Email" },
				{ "api1", "EmailPhone", "api2", "api1", "Email" },
				{ "api2", "EmailOnly", "auth", "api2", "Phone" },
				{ "api2", "EmailPhone", "api1", "auth", "Phone" },
				{ "auth", "EmailOnly", "api2", "api1", "EmailPhone" },
				{ "auth", "EmailPhone", "auth", "api2", "EmailPhone" },
				{ "pos", "EmailOnly", null, null, null },
				{ "pos", "PhoneOnly", null, null, null },
				{ "pos", "EmailPhone", null, null, null },
		};
	}

	@Test(description = "Punchh Signup → Punchh SignIn → Update", dataProvider = "Punchh_Signup_Punchh_SignIn_Update")
	public void Punchh_Signup_Punchh_SignIn_Update_Test(String signupNamespace, String signupUserType,
			String signinNamespace, String updateNamespace, String updateType) throws Exception {

		String email = intUtils.getRandomGmailEmail();
		String phone = dataSet.get("phoneNumber");
		String locationKey = dataSet.get("locationKey");

		// Punchh SignUp
		intUtils.userSignUpPunchh(signupNamespace, signupUserType, client, secret, email, phone, locationKey);

		String bearer = null;
		String userPassword = Utilities.getApiConfigProperty("password");
		// Punchh SignIn
		if (signupUserType.contains("Email") && !signupNamespace.equals("pos")) {
			bearer = intUtils.userSignInPunchh(signinNamespace, client, secret, email, userPassword);
			utils.logit("Bearer: " + bearer);
			// User update
			intUtils.legacyUserUpdateWithBearerToken(client, secret, signupUserType, updateNamespace, updateType,
					bearer);
		} else {
			utils.logit(
					"Punchh SignIn and Update skipped as Punchh SignIn and Update is not possible for PhoneOnly or SignupNamespace is POS");
		}
		utils.logit("Pass", "Punchh Signup → Punchh SignIn → Update test passed");
	}

	public String getLastSyncedAt(String userId) throws Exception {
		String lastSyncedAt;
		// fetch last_synced_at from guest_identity_db
		String query = "SELECT last_synced_at FROM users WHERE id = '" + userId + "';";
		lastSyncedAt = DBUtils.executeQueryAndGetColumnValue(env, guestIdentityhost, query, "last_synced_at");
		return lastSyncedAt;
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		utils.logit("Test Case: " + sTCName + " finished");
	}
}