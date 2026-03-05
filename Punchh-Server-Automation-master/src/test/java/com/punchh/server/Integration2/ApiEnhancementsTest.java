package com.punchh.server.Integration2;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.annotations.Owner;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.apiConfig.ApiResponseJsonSchema;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.ApiUtils;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class ApiEnhancementsTest {

	private static Logger logger = LogManager.getLogger(ApiEnhancementsTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String sTCName;
	private String baseUrl;
	private String env;
	private String run = "ui";
	private static Map<String, String> dataSet;
	private Utilities utils;
	private ApiUtils apiUtils;
	private String userEmail;

	@BeforeClass
	public void BeforeClass() throws Exception {
		utils = new Utilities();
		apiUtils = new ApiUtils();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv().toLowerCase();
	}

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
		driver = new BrowserUtilities().launchBrowser();
		Utilities.loadPropertiesFile("config.properties");
		sTCName = method.getName();
		pageObj = new PageObj(driver);
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run , env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath("ui" , env , "Secrets"), dataSet.get("slug"));
		// Merge datasets without overwriting existing keys
		pageObj.readData().readTestData.forEach(dataSet::putIfAbsent);
		logger.info(sTCName + " ==>" + dataSet);
	}

	public void select_cockpit_dashboard_miscalleaneous_tab_external_source_id_flag() throws InterruptedException {
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.instanceDashboardPage().select_cockpit_dashboard_miscalleaneous_tab_external_source_id_flag();
	}

	@Test(description = "SQ-T2955 CCA2-760 | Update API 1 - api2/dashboard/users - Existing user with no ext. source and source_id | Update/Add new ext. source and source_id")
	@Owner(name = "Nipun Jain")
	public void SQ_T2955_ExistingUserWithNoExternalSourceAndIDCombinationsTest()
			throws NumberFormatException, Exception {
		select_cockpit_dashboard_miscalleaneous_tab_external_source_id_flag();

		// Select value as salesforce
		pageObj.instanceDashboardPage().select_external_identifier_updation_idp_drpdwn("Salesforce");

		// User update with value in API equals and not equals in GUI dropdown
		String[] idpValues = { "salesforce", "customer_id", "mparticle", "azure", "aws_cognito", "auth0", "ext_idp" };

		for (String idpValue : idpValues) {
			String email = "autouser_" + CreateDateTime.getTimeDateAsneed("yyyyMMddHHmmss") + "@partech.com";
			Response signUpResp = pageObj.endpoints().Api2SignUp(email, dataSet.get("client"), dataSet.get("secret"));
			pageObj.apiUtils().verifyResponse(signUpResp, "API 2 user signup");
			String user_id = signUpResp.jsonPath().get("user.user_id").toString();

			String external_id = "EXT_ID_" + CreateDateTime.getTimeDateAsneed("yyyyMMddHHmmss");
			Response updateResp = pageObj.endpoints().api2UpdateUserExternalSourceAndID(user_id, idpValue, external_id,
					dataSet.get("admin_key"));
			Assert.assertEquals(updateResp.getStatusCode(), 200);

			String query = "SELECT COUNT(*) as count FROM oauth_access_tokens WHERE token = '" + external_id
					+ "' and resource_owner_id = '" + user_id + "'";

			if (idpValue.equals("salesforce")) {
				// Entry should be created in oauth_access_tokens
				int count = Integer
						.parseInt(DBUtils.executeQueryAndGetColumnValue(env, query, "count"));

				Assert.assertEquals(count, 1, query + " query count is not matched ");
				utils.logPass("Expected and Actual column count match for DB query : " + query);
			} else {
				// No entry should be created in oauth_access_tokens for other cases

				int count = Integer
						.parseInt(DBUtils.executeQueryAndGetColumnValue(env, query, "count"));

				Assert.assertEquals(count, 0, query + " query count is not matched ");
				// Assert.assertTrue(dbUtils.verifyColumnValue(query, "count", "0"));
				utils.logPass("Expected and Actual column count match for DB query : " + query);
			}

			// All entries should be created in user_external_identifiers with corresponding source
			query = "SELECT COUNT(*) as count FROM user_external_identifiers WHERE source_id = '"
					+ external_id + "' and source = '" + idpValue + "'";

			int count = Integer
					.parseInt(DBUtils.executeQueryAndGetColumnValue(env, query, "count"));

			Assert.assertEquals(count, 1, query + " query count is not matched ");

			// Assert.assertTrue(dbUtils.verifyColumnValue(query, "count", "1"));
			utils.logPass("Expected and Actual value match for DB query : " + query);
		}
	}

	@Test(description = "SQ-T2956 CCA2-760 | Update API 2 - api2/mobile/users - Exiting user with ext. source and source_id, updates on same user | Update only source, source_id same (new added)")
	@Owner(name = "Nipun Jain")
	public void SQ_T2956_ExistingUserWithExternalSourceAndIDCombinationsTest() throws NumberFormatException, Exception {
		select_cockpit_dashboard_miscalleaneous_tab_external_source_id_flag();

		// Select value as salesforce
		pageObj.instanceDashboardPage().select_external_identifier_updation_idp_drpdwn("Salesforce");

		String email = "autouser_" + CreateDateTime.getTimeDateAsneed("yyyyMMddHHmmss") + "@partech.com";

		// SignUp user with external source id as salesforce
		// Source id same each time
		String external_id = "EXT_ID_" + CreateDateTime.getTimeDateAsneed("yyyyMMddHHmmss");
		Response signUpResp = pageObj.endpoints().Api2SignUpWithExternalSourceAndID(email, dataSet.get("client"),
				dataSet.get("secret"), external_id, "salesforce");
		pageObj.apiUtils().verifyResponse(signUpResp, "API 2 user signup");

		String user_id = signUpResp.jsonPath().get("user.user_id").toString();

		String[] idpValues = { "salesforce", "customer_id", "mparticle", "azure", "aws_cognito", "auth0", "ext_idp" };

		for (String idpValue : idpValues) {
			// Make updates on same user
			Response updateResp = pageObj.endpoints().api2UpdateUserExternalSourceAndID(user_id, idpValue, external_id,
					dataSet.get("admin_key"));
			Assert.assertEquals(updateResp.getStatusCode(), 200);

			String query = "SELECT COUNT(*) as count FROM oauth_access_tokens WHERE token = '"
					+ external_id + "' and resource_owner_id = '" + user_id + "'";

			int count = Integer
					.parseInt(DBUtils.executeQueryAndGetColumnValue(env, query, "count"));

			Assert.assertEquals(count, 1, query + " query count is not matched ");

			// Count will always be one as same external_id
			Assert.assertEquals(DBUtils.executeQueryAndGetColumnValue(env, query, "count"), "1");
			utils.logPass("Expected and Actual column count match for DB query : " + query);

			// All entries should be cerated in user_external_identifiers with corresponding source
			query = "SELECT COUNT(*) as count FROM user_external_identifiers WHERE source_id = '"
					+ external_id + "' and source = '" + idpValue + "'";
			int count1 = Integer
					.parseInt(DBUtils.executeQueryAndGetColumnValue(env, query, "count"));

			Assert.assertEquals(count1, 1, query + " query count is not matched ");
			Assert.assertEquals(DBUtils.executeQueryAndGetColumnValue(env, query, "count"), "1");
			utils.logPass("Expected and Actual column count match for DB query : " + query);
		}
	}

	@Test(description = "SQ-T2957 CCA2-760 | Update API 3 - api/auth/users - Exiting user with ext. source and source_id, updates on same user | Update only source_id, same source")
	@Owner(name = "Nipun Jain")
	public void SQ_T2957_ExistingUserWithNoExternalSourceAndIDCombinationsTest_SourceSame()
			throws NumberFormatException, Exception {
		select_cockpit_dashboard_miscalleaneous_tab_external_source_id_flag();

		// Select value as salesforce
		pageObj.instanceDashboardPage().select_external_identifier_updation_idp_drpdwn("Salesforce");
		String email = "autouser_" + CreateDateTime.getTimeDateAsneed("yyyyMMddHHmmss") + "@partech.com";
		Response signUpResp = pageObj.endpoints().authApiSignUp(email, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(signUpResp.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);

		String user_id = signUpResp.jsonPath().get("user_id").toString();

		String external_id = "EXT_ID_" + CreateDateTime.getTimeDateAsneed("yyyyMMddHHmmss");
		Response updateResp = pageObj.endpoints().api2UpdateUserExternalSourceAndID(user_id, "salesforce", external_id,
				dataSet.get("admin_key"));
		Assert.assertEquals(updateResp.getStatusCode(), 200);

		String query = "SELECT COUNT(*) as count FROM oauth_access_tokens WHERE token = '" + external_id
				+ "' and resource_owner_id = '" + user_id + "'";
		// Entry should be created in oauth_access_tokens
		int count = Integer.parseInt(DBUtils.executeQueryAndGetColumnValue(env, query, "count"));

		Assert.assertEquals(count, 1, query + " query count is not matched ");
		Assert.assertEquals(DBUtils.executeQueryAndGetColumnValue(env, query, "count"), "1");
		utils.logPass("Expected and Actual column count match for DB query : " + query);

		// All entries should be created in user_external_identifiers with corresponding source
		query = "SELECT COUNT(*) as count FROM user_external_identifiers WHERE user_id = '" + user_id
				+ "' and source = '" + "salesforce" + "'";
		int count1 = Integer.parseInt(DBUtils.executeQueryAndGetColumnValue(env, query, "count"));

		Assert.assertEquals(count1, 1, query + " query count is not matched ");
		Assert.assertEquals(DBUtils.executeQueryAndGetColumnValue(env, query, "count"), "1");
		utils.logPass("Expected and Actual value match for DB query : " + query);
	}

	@Test(description = "SQ-T2759_INTD-406 | User look-up API | Phone number not unique on business", groups = {"regression", "dailyrun"}, priority = 0)
	@Owner(name = "Nipun Jain")
	public void T2759_Phone_number_not_unique_on_business() throws InterruptedException {

		long phone = (long) (Math.random() * Math.pow(10, 10));
		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// Click Cockpit
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.menupage().clickGuestValidation();
		pageObj.dashboardpage().offEnableBusinessPhoneUniqueness();
		// User signUp
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"), phone);
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("API2 Signup is successful");
		// hit api api2/dashboard/users/info only phone
		Response userLookUpApi2 = pageObj.endpoints().userLookUpApi("phone", "", dataSet.get("admin_key"),
				phone, "");
		Assert.assertEquals(userLookUpApi2.getStatusCode(), 422,
				"Status code 422 did not matched for the User Look Up (phone only) API");
		boolean isUserInfoInvalidPhoneSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.dashboardUserInfoPhoneErrorSchema, userLookUpApi2.asString());
		Assert.assertTrue(isUserInfoInvalidPhoneSchemaValidated,
				"Platform Functions API Get User Information Schema Validation failed");
		utils.logPass("User Look Up (phone only) is not successful (criteria satisfied)");
		// E-mail and phone both
		Response userLookUpApi2ii = pageObj.endpoints().userLookUpApi("email_phone", userEmail,
				dataSet.get("admin_key"), phone, "");
		Assert.assertEquals(userLookUpApi2ii.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for the User Look Up (phone and email both) API");
		boolean isUserInfoSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.dashboardUserInfoPhoneEmailSchema, userLookUpApi2ii.asString());
		Assert.assertTrue(isUserInfoSchemaValidated,
				"Platform Functions API Get User Information Schema Validation failed");
		utils.logPass("User Look Up (phone and email both) is successful");
	}

	@Test(description = "SQ-T2760_INTD-406 | User look-up AP | Phone number unique on business", groups = {"regression", "dailyrun"}, priority = 1)
	@Owner(name = "Nipun Jain")
	public void T2760_Phone_number_unique_on_business() throws InterruptedException {
		long phone = (long) (Math.random() * Math.pow(10, 10));
		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// Click Cockpit and enable STO campaign
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.menupage().clickGuestValidation();
		pageObj.dashboardpage().onEnableBusinessPhoneUniqueness();
		// User signUp
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"), phone);
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("API2 Signup is successful");
		// hit api api2/dashboard/users/info User ID + Phone only
		Response userLookUpApi2i = pageObj.endpoints().userLookUpApi("userId_phone", "",
				dataSet.get("admin_key"), phone, userID);
		Assert.assertEquals(userLookUpApi2i.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for the User Look Up (phone and userId) API");
		utils.logPass("User Look Up (phone and userId both) is successful");
		// phone only
		Response userLookUpApi2ii = pageObj.endpoints().userLookUpApi("phone", "", dataSet.get("admin_key"),
				phone, "");
		Assert.assertEquals(userLookUpApi2ii.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for the User Look Up (phone only) API");
		utils.logPass("User Look Up (phone only) is successful");
		// Email only
		Response userLookUpApi2iii = pageObj.endpoints().userLookUpApi("emailOnly", userEmail, dataSet.get("admin_key"),
				phone, "");
		Assert.assertEquals(userLookUpApi2iii.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for the User Look Up (phone and email both) API");
		utils.logPass("User Look Up (email only) is successful");
		// ban user
		Response banUserresponse = pageObj.endpoints().banUser(userID, dataSet.get("admin_key"));
		Assert.assertEquals(banUserresponse.getStatusCode(), ApiConstants.HTTP_STATUS_ACCEPTED,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Ban a User");
		utils.logPass("PLATFORM FUNCTIONS API Ban a User is successful");
		// Email only
		Response userLookUpApi2iiii = pageObj.endpoints().userLookUpApi("emailOnly", userEmail,
				dataSet.get("admin_key"), phone, "");
		Assert.assertEquals(userLookUpApi2iiii.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for the User Look Up (email ony) API");
		utils.logPass("User Look Up (email only) is not successful (criteria satisfied)");
	}

	@Test(description = "SQ-T2939 CCA2-662 | Verify the user signup with external_source as auth0 for auth api", groups = "Regression")
	@Owner(name = "Nipun Jain")
	public void T2939_VerifyTheUserSignupWithExternalSourceAsAuth0ForAuthApi() throws InterruptedException {
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Click Cockpit and click External Source Id Flag
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.instanceDashboardPage().checkExternalSourceIdFlag();
		pageObj.instanceDashboardPage().setExternalSourceIdFlag(dataSet.get("ExternalSourceId"));
		pageObj.instanceDashboardPage().enableGenerationOfAccessTokensFlag();
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "OAuth Apps");
		pageObj.oAuthAppPage().enableSSO();

		// User SignUp from Auth Api
		String external_source_id = CreateDateTime.getTimeDateString();
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"), dataSet.get("external_source"), external_source_id);
		Assert.assertEquals(signUpResponse.getStatusCode(), 201,
				"Status code 201 did not matched for auth user signup api");
		String token = signUpResponse.jsonPath().get("access_token");
		Assert.assertEquals(token, external_source_id, "access token don't matches with external_source_id");

		// Verify fetch user info
		Response fetchUserResponse = pageObj.endpoints().authApiFetchUserInfo1(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(fetchUserResponse.getStatusCode(), 200,
				"Status code 201 did not matched for api2 fetch user info");
		utils.logPass("Auth Api fetch user info is successful");

		// get account history and verify checkin
		Response accountHistoryResponse = pageObj.endpoints().authApiAccountHistory1(token, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(accountHistoryResponse.getStatusCode(), 200,
				"Status code 201 did not matched for api2 account history");
		utils.logPass("Auth Api account history is successful");
	}

	@Test(description = "SQ-T2939 CCA2-662 | Verify the user signup with external_source as auth0 for mobile", groups = "Regression")
	@Owner(name = "Nipun Jain")
	public void T2939_VerifyTheUserSignupWithExternalSourceAsAuth0ForMobileAppi() throws InterruptedException {
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Click Cockpit and click External Source Id Flag
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.instanceDashboardPage().checkExternalSourceIdFlag();
		pageObj.instanceDashboardPage().setExternalSourceIdFlag(dataSet.get("ExternalSourceId"));
		pageObj.instanceDashboardPage().enableGenerationOfAccessTokensFlag();
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "OAuth Apps");
		pageObj.oAuthAppPage().enableSSO();

		// User register/signup using API2 Signup
		String external_source_id = CreateDateTime.getTimeDateString();
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"), dataSet.get("external_source"), external_source_id);
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("Api2 user signup is successful");
		Assert.assertEquals(token, external_source_id, "access token don't matches with external_source_id");

		// Create user relation
		Response createUserRelationResponse = pageObj.endpoints().Api2CreateUserrelation(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(createUserRelationResponse.getStatusCode(), 201,
				"Status code 200 did not matched for api2 signup");
		utils.logPass("Api2 Create user relation is successful");
	}

	@Test(description = "SQ-T2933 CCA2-662 | Verify the user signup with external_source as Ext IDP for auth api", groups = "Regression")
	@Owner(name = "Nipun Jain")
	public void T2933_VerifyTheUserSignupWithExternalSourceAsExtIDPForAuthApi() throws InterruptedException {
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Click Cockpit and click External Source Id Flag
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.instanceDashboardPage().checkExternalSourceIdFlag();
		pageObj.instanceDashboardPage().setExternalSourceIdFlag(dataSet.get("ExternalSourceId"));
		pageObj.instanceDashboardPage().enableGenerationOfAccessTokensFlag();
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "OAuth Apps");
		pageObj.oAuthAppPage().enableSSO();

		// User SignUp from Auth Api
		String external_source_id = CreateDateTime.getTimeDateString();
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"), dataSet.get("external_source"), external_source_id);
		Assert.assertEquals(signUpResponse.getStatusCode(), 201,
				"Status code 201 did not matched for auth user signup api");
		String token = signUpResponse.jsonPath().get("access_token");
		Assert.assertEquals(token, external_source_id, "access token don't matches with external_source_id");

		// Verify fetch user info
		Response fetchUserResponse = pageObj.endpoints().authApiFetchUserInfo1(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(fetchUserResponse.getStatusCode(), 200,
				"Status code 201 did not matched for api2 fetch user info");
		utils.logPass("Auth Api fetch user info is successful");

		// get account history and verify checkin
		Response accountHistoryResponse = pageObj.endpoints().authApiAccountHistory1(token, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(accountHistoryResponse.getStatusCode(), 200,
				"Status code 201 did not matched for api2 account history");
		utils.logPass("Auth Api account history is successful");
	}

	@Test(description = "SQ-T2933 CCA2-662 | Verify the user signup with external_source as Ext IDP for mobile", groups = "Regression")
	@Owner(name = "Nipun Jain")
	public void T2933_VerifyTheUserSignupWithExternalSourceAsExtIDPForMobileAppi() throws InterruptedException {
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Click Cockpit and click External Source Id Flag
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.instanceDashboardPage().checkExternalSourceIdFlag();
		pageObj.instanceDashboardPage().setExternalSourceIdFlag(dataSet.get("ExternalSourceId"));
		pageObj.instanceDashboardPage().enableGenerationOfAccessTokensFlag();
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "OAuth Apps");
		pageObj.oAuthAppPage().enableSSO();

		// User register/signup using API2 Signup
		String external_source_id = CreateDateTime.getTimeDateString();
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"), dataSet.get("external_source"), external_source_id);
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("Api2 user signup is successful");
		Assert.assertEquals(token, external_source_id, "access token don't matches with external_source_id");

		// Create user relation
		Response createUserRelationResponse = pageObj.endpoints().Api2CreateUserrelation(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(createUserRelationResponse.getStatusCode(), 201,
				"Status code 200 did not matched for api2 signup");
		utils.logPass("Api2 Create user relation is successful");
	}

	@Test(description = "SQ-T4265 - INT2-1082 | Verify new dashboard API for user checkin")
	@Owner(name = "Nipun Jain")
	public void T4265_VerifyingNewDashboardAPI() {
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		// User signUp
		Response signUpRes = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(signUpRes.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "API2 Punchh SignUp failed");
		utils.logPass("Api2 Punchh sign-up was successful");

		String startTime = CreateDateTime.getCurrentDate();
		Response checkinResponse = pageObj.endpoints().Api2FetchCheckinDashboard(dataSet.get("apiKey"), "email",
				userEmail, startTime);
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Dashboard checkin got failed");
		boolean isCreateCheckinSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.dashboardCreateCheckinSchema, checkinResponse.asString());
		Assert.assertTrue(isCreateCheckinSchemaValidated,
				"Platform Functions API Create Loyalty Checkin Schema Validation failed");

		int points_earnedExpected = Integer.parseInt(dataSet.get("points_earnedExpected"));
		int actualPointsEarned = checkinResponse.jsonPath().getInt("checkin.points_earned");
		Assert.assertEquals(actualPointsEarned, points_earnedExpected,
				actualPointsEarned + " actual points earned not matched with expected points " + points_earnedExpected);
		utils.logPass("Verified that user is able to checkin with new api2 dashboard API using EMAIL");

	}

	@Test(description = "SQ-T4288 INT2-1123, INT2-1125 | Sync User deletion on identity for business reset",groups = {"nonNightly" })
	@Owner(name = "Nipun Jain")
	public void T4288_VerifyUserDeletionOnIdentityForBusinessReset() throws InterruptedException {
		String userEmail1 = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse1 = pageObj.endpoints().authApiSignUp(userEmail1, dataSet.get("client"),
				dataSet.get("secret"));
		apiUtils.verifyCreateResponse(signUpResponse1, "Auth API user signup");
		Assert.assertEquals(signUpResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");

		String userEmail2 = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse2 = pageObj.endpoints().authApiSignUp(userEmail2, dataSet.get("client"),
				dataSet.get("secret"));
		apiUtils.verifyCreateResponse(signUpResponse2, "Auth API user signup");
		Assert.assertEquals(signUpResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");

		String userEmail3 = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse3 = pageObj.endpoints().authApiSignUp(userEmail3, dataSet.get("client"),
				dataSet.get("secret"));
		apiUtils.verifyCreateResponse(signUpResponse3, "Auth API user signup");
		Assert.assertEquals(signUpResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");

		String userEmail4 = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse4 = pageObj.endpoints().authApiSignUp(userEmail4, dataSet.get("client"),
				dataSet.get("secret"));
		apiUtils.verifyCreateResponse(signUpResponse4, "Auth API user signup");
		Assert.assertEquals(signUpResponse4.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");

		String userEmail5 = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse5 = pageObj.endpoints().authApiSignUp(userEmail5, dataSet.get("client"),
				dataSet.get("secret"));
		apiUtils.verifyCreateResponse(signUpResponse5, "Auth API user signup");
		Assert.assertEquals(signUpResponse5.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().clickOnResetDeleteBusinessButton();

		pageObj.dashboardpage().enterSlugNameAndClickOnResetButton(dataSet.get("slug"));
		Thread.sleep(10000);

		utils.logPass("Verified that " + userEmail1 + " user is not exist after reset the business ");

		boolean userIsExist2 = pageObj.instanceDashboardPage().verifyGuestsPresence(userEmail2);
		Assert.assertFalse(userIsExist2, userEmail2 + " User should not be visible .");
		utils.logPass("Verified that " + userEmail2 + " user is not exist after reset the business ");

		boolean userIsExist3 = pageObj.instanceDashboardPage().verifyGuestsPresence(userEmail3);
		Assert.assertFalse(userIsExist3, userEmail3 + " User should not be visible .");
		utils.logPass("Verified that " + userEmail3 + " user is not exist after reset the business ");

		boolean userIsExist4 = pageObj.instanceDashboardPage().verifyGuestsPresence(userEmail4);
		Assert.assertFalse(userIsExist4, userEmail4 + " User should not be visible .");
		utils.logPass("Verified that " + userEmail4 + " user is not exist after reset the business ");

		boolean userIsExist5 = pageObj.instanceDashboardPage().verifyGuestsPresence(userEmail5);
		Assert.assertFalse(userIsExist5, userEmail5 + " User should not be visible .");
		utils.logPass("Verified that " + userEmail5 + " user is not exist after reset the business ");

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		if (dataSet != null)
			pageObj.utils().clearDataSet(dataSet);
		Utilities.screenShotCapture(driver, sTCName);
		logger.info("Test Case: " + sTCName + " finished");
		driver.quit();
		logger.info("Browser closed");
	}

}