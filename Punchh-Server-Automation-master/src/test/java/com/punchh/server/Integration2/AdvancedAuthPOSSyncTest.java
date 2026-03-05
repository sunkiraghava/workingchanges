// package com.punchh.server.Integration2;

// import java.lang.reflect.Method;
// import java.util.Map;
// import java.util.concurrent.ConcurrentHashMap;

// import org.openqa.selenium.WebDriver;
// import org.testng.Assert;
// import org.testng.annotations.AfterMethod;
// import org.testng.annotations.BeforeMethod;
// import org.testng.annotations.Listeners;
// import org.testng.annotations.Test;

// import com.punchh.server.pages.PageObj;
// import com.punchh.server.utilities.BrowserUtilities;
// import com.punchh.server.utilities.CreateDateTime;
// import com.punchh.server.utilities.TestListeners;
// import com.punchh.server.utilities.Utilities;

// import io.restassured.response.Response;

// @SuppressWarnings("static-access")
// @Listeners(TestListeners.class)
// public class AdvancedAuthPOSSyncTest {
// 	public WebDriver driver;
// 	private PageObj pageObj;
// 	private String sTCName;
// 	private String baseUrl;
// 	private IntUtils intUtils;
// 	private String env, run = "ui";
// 	private static Map<String, String> dataSet;
// 	private Utilities utils;
// 	private String identityDbName;
// 	private String client, secret, locationKey, loyaltyCardNumber;
// 	private String userEmail;
// 	private long phone;

// 	@BeforeMethod(alwaysRun = true)
// 	public void setUp(Method method) {
// 		driver = new BrowserUtilities().launchBrowser();
// 		pageObj = new PageObj(driver);
// 		env = pageObj.getEnvDetails().setEnv().toLowerCase();
// 		identityDbName = Utilities.getDBConfigProperty(env, "identityDBName");
// 		Utilities.loadPropertiesFile("config.properties");
// 		sTCName = method.getName();
// 		baseUrl = pageObj.getEnvDetails().setBaseUrl();
// 		utils = new Utilities(driver);
// 		intUtils = new IntUtils(driver);
// 		dataSet = new ConcurrentHashMap<>();
// 		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env),
// 				"AdvancedAuthPOSUserMergeSync");
// 		dataSet = pageObj.readData().readTestData;
// 		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
// 				pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
// 		// Merge datasets without overwriting existing keys
// 		pageObj.readData().readTestData.forEach(dataSet::putIfAbsent);
// 		utils.logit("Using env as ==> " + env);
// 		utils.logit("Using identityDbName as ==> " + identityDbName);
// 		utils.logit(sTCName + " ==>" + dataSet);

// 		// Common data for all tests
// 		client = dataSet.get("client");
// 		secret = dataSet.get("secret");
// 		locationKey = dataSet.get("locationKey");
// 		phone = Long.parseLong(Utilities.phonenumber());
// 		loyaltyCardNumber = String.valueOf((long) (Math.random() * 9_000_000_000_000_000L) + 1_000_000_000_000_000L);
// 		userEmail = "adv_auth_auto_pos_sync_" + utils.getTimestampInNanoseconds() + "@partech.com";
// 	}

// 	@Test(description = "Set Dashboard Flags for POS user merge", priority = -1)
// 	public void setDashboardFlags() throws InterruptedException {
// 		String slug = dataSet.get("slug");
// 		// Pre-conditions for POS user merge	
// 		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
// 		pageObj.instanceDashboardPage().loginToInstance();
// 		pageObj.instanceDashboardPage().selectBusiness(slug);

// 		// Whitelabel >> iframe configuration >> basic configuration
// 		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "iFrame Configuration");
// 		pageObj.dashboardpage().navigateToTabs("Basic Configuration");
// 		utils.setCheckboxStateViaCheckBoxText("Accept Phone Number?");
// 		utils.setCheckboxStateViaCheckBoxText("Enable Phone Number as a mandatory field?");
// 		utils.setCheckboxStateViaCheckBoxText("Accept minimum 10 digit Phone Number?");
// 		utils.logit(pageObj.dashboardpage().updateCheckBox());

// 		// Cockpit -> Guest -> Guest validation
// 		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
// 		pageObj.dashboardpage().navigateToTabs("Guest Validation");
// 		utils.setCheckboxStateViaCheckBoxText("Validate uniqueness of phone number across guests?");
// 		utils.setCheckboxStateViaCheckBoxText("Use parsed phone number for guests?");
// 		utils.logit(pageObj.dashboardpage().updateCheckBox());

// 		// Cockpit -> Pos Integration
// 		pageObj.menupage().navigateToSubMenuItem("Cockpit", "POS Integration");
// 		utils.setCheckboxStateViaCheckBoxText(
// 				"Create a new guest based on phone number if the guest doesn't exist (via POS/SMS)?");
// 		utils.logit(pageObj.dashboardpage().updateCheckBox());

// 		// Cockpit -> Physical Cards
// 		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Physical Cards");
// 		pageObj.cockpitPhysicalCardPage().selectLoyaltyCardAdapter("External Vendor Loyalty Cards", "16");
// 		utils.logit(pageObj.dashboardpage().updateCheckBox());
// 		utils.setCheckboxStateViaCheckBoxText("Enable Unmasking For Physical/Loyalty Card Numbers");
// 		utils.logit(pageObj.dashboardpage().updateCheckBox());
// 	}

// 	@Test(description = "SQ-T6202 INT2-2028 | POSUserMerge_MobileApi1")
// 	public void SQ_T6202_POSUserMerge_MobileApi1_test() throws Exception {
// 		// Create a POS user via signup API
// 		Response posResp = pageObj.endpoints().posSignUpWithoutEmail(phone, locationKey);
// 		Assert.assertEquals(200, posResp.getStatusCode(), "Status code 200 did not matched for pos signup api");
// 		String posUserEmail = posResp.jsonPath().get("email").toString();
// 		Assert.assertTrue(posUserEmail.contains("@phone.punchh.com"), "POS user email did not match expected format");
// 		String posUserId = posResp.jsonPath().get("id").toString();
// 		utils.logit("POS signup with phone only is successful");

// 		// Validate sync with identity
// 		intUtils.validateUserSyncWithIdentity(posUserId);

// 		// User Sign-up with same phone number
// 		Response signUpResponse = null;
// 		signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, client, secret, String.valueOf(phone));

// 		Assert.assertEquals(signUpResponse.getStatusCode(), 200,
// 				"Status code 200 did not match for mobile sign up API");
// 		utils.logit("Pass", "Mobile sign up is successful");

// 		String emailUserId = signUpResponse.jsonPath().get("id").toString();
// 		String phoneNumber = signUpResponse.jsonPath().get("phone").toString();

// 		Assert.assertEquals(posUserId, emailUserId, "POS user ID and email user ID did not match");
// 		Assert.assertEquals(phoneNumber, String.valueOf(phone),
// 				"Phone number did not match between POS and email user");

// 		// Validate sync with identity
// 		intUtils.validateUserSyncWithIdentity(emailUserId);
// 		utils.logit("Pass", "POS user merge with mobile user is successfull");
// 	}

// 	@Test(description = "SQ-T6203 INT2-2028 | POSUserMerge_MobileApi2")
// 	public void SQ_T6203_POSUserMerge_MobileApi2_test() throws Exception {
// 		// Create a POS user via signup API
// 		Response posResp = pageObj.endpoints().posSignUpWithoutEmail(phone, locationKey);
// 		Assert.assertEquals(200, posResp.getStatusCode(), "Status code 200 did not matched for pos signup api");
// 		String posUserEmail = posResp.jsonPath().get("email").toString();
// 		Assert.assertTrue(posUserEmail.contains("@phone.punchh.com"), "POS user email did not match expected format");
// 		String posUserId = posResp.jsonPath().get("id").toString();
// 		utils.logit("POS signup with phone only is successful");

// 		// Validate sync with identity
// 		intUtils.validateUserSyncWithIdentity(posUserId);

// 		// User Sign-up with same phone number
// 		Response signUpResponse = null;
// 		signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, client, secret, phone);

// 		Assert.assertEquals(signUpResponse.getStatusCode(), 200,
// 				"Status code 200 did not match for mobile sign up API");

// 		utils.logit("Pass", "Mobile sign up is successful");

// 		String emailUserId = signUpResponse.jsonPath().get("user.user_id").toString();
// 		String phoneNumber = signUpResponse.jsonPath().get("user.phone").toString();

// 		Assert.assertEquals(posUserId, emailUserId, "POS user ID and email user ID did not match");
// 		Assert.assertEquals(phoneNumber, String.valueOf(phone),
// 				"Phone number did not match between POS and email user");

// 		// Validate sync with identity
// 		intUtils.validateUserSyncWithIdentity(emailUserId);
// 		utils.logit("Pass", "POS user merge with mobile user is successfull");
// 	}

// 	@Test(description = "SQ-T6204 INT2-2028 | POSUserMerge_AuthApi")
// 	public void SQ_T6204_POSUserMerge_AuthApi_test() throws Exception {
// 		// Create a POS user via signup API
// 		Response posResp = pageObj.endpoints().posSignUpWithoutEmail(phone, locationKey);
// 		Assert.assertEquals(200, posResp.getStatusCode(), "Status code 200 did not matched for pos signup api");
// 		String posUserEmail = posResp.jsonPath().get("email").toString();
// 		Assert.assertTrue(posUserEmail.contains("@phone.punchh.com"), "POS user email did not match expected format");
// 		String posUserId = posResp.jsonPath().get("id").toString();
// 		utils.logit("POS signup with phone only is successful");

// 		// Validate sync with identity
// 		intUtils.validateUserSyncWithIdentity(posUserId);

// 		// User Sign-up with same phone number
// 		Response signUpResponse = null;
// 		signUpResponse = pageObj.endpoints().authApiSignUp(userEmail, client, secret, String.valueOf(phone));

// 		Assert.assertEquals(signUpResponse.getStatusCode(), 201,
// 				"Status code 201 did not match for mobile sign up API");

// 		utils.logit("Pass", "Mobile sign up is successful");

// 		String emailUserId = signUpResponse.jsonPath().get("id").toString();
// 		String phoneNumber = signUpResponse.jsonPath().get("phone").toString();

// 		Assert.assertEquals(posUserId, emailUserId, "POS user ID and email user ID did not match");
// 		Assert.assertEquals(phoneNumber, String.valueOf(phone),
// 				"Phone number did not match between POS and email user");

// 		// Validate sync with identity
// 		intUtils.validateUserSyncWithIdentity(emailUserId);
// 		utils.logit("Pass", "POS user merge with mobile user is successfull");
// 	}

// 	@Test(description = "SQ-T6202 INT2-2025 | UserMerge_MobileApi1_loyaltyCard")
// 	public void SQ_T6205_UserMerge_MobileApi1_loyaltyCard_test() throws Exception {
// 		// Create a Loyalty card user via signup API
// 		Response resp = pageObj.endpoints().posSignUpWithLoyaltyCardOnly(loyaltyCardNumber, locationKey);
// 		Assert.assertEquals(200, resp.getStatusCode(), "Status code 200 did not matched for pos signup api");
// 		String loyaltyCardUserEmail = resp.jsonPath().get("email").toString();
// 		Assert.assertTrue(loyaltyCardUserEmail.contains("@cards.punchh.com"), "POS user email did not match expected format");
// 		String loyaltyCardUserId = resp.jsonPath().get("id").toString();
// 		utils.logit("POS signup with phone only is successful");

// 		// Validate sync with identity
// 		intUtils.validateUserSyncWithIdentity(loyaltyCardUserId);

// 		// User Sign-up with same loyalty card number
// 		Response signUpResponse = null;
// 		signUpResponse = pageObj.endpoints().Api1UserSignUpWithLoyaltyCard(client, secret, userEmail,
// 				loyaltyCardNumber);

// 		Assert.assertEquals(signUpResponse.getStatusCode(), 200,
// 				"Status code 200 did not match for mobile sign up API");

// 		utils.logit("Pass", "Mobile sign up is successful");

// 		String emailUserId = signUpResponse.jsonPath().get("id").toString();
// 		String loyaltyCardNumberResp = signUpResponse.jsonPath().get("loyalty_cards[0].card_number").toString();

// 		Assert.assertEquals(loyaltyCardUserId, emailUserId, "Loyalty card user ID and email user ID did not match");
// 		Assert.assertEquals(loyaltyCardNumberResp, loyaltyCardNumber,
// 				"Loyalty card number did not match between POS and email user");

// 		// Validate sync with identity
// 		intUtils.validateUserSyncWithIdentity(emailUserId);
// 		utils.logit("Pass", "POS user merge with mobile user is successfull");
// 	}

// 	@Test(description = "SQ-T6202 INT2-2026 | UserMerge_MobileApi3_loyaltyCard")
// 	public void SQ_T6206_UserMerge_MobileApi2_loyaltyCard_test() throws Exception {
// 		// Create a Loyalty card user via signup API
// 		Response resp = pageObj.endpoints().posSignUpWithLoyaltyCardOnly(loyaltyCardNumber, locationKey);
// 		Assert.assertEquals(200, resp.getStatusCode(), "Status code 200 did not matched for pos signup api");
// 		String loyaltyCardUserEmail = resp.jsonPath().get("email").toString();
// 		Assert.assertTrue(loyaltyCardUserEmail.contains("@cards.punchh.com"), "POS user email did not match expected format");
// 		String loyaltyCardUserId = resp.jsonPath().get("id").toString();
// 		utils.logit("POS signup with phone only is successful");

// 		// Validate sync with identity
// 		intUtils.validateUserSyncWithIdentity(loyaltyCardUserId);

// 		// User Sign-up with same loyalty card number
// 		Response signUpResponse = null;
// 		signUpResponse = pageObj.endpoints().Api2SignUpWithLoyaltyCard(userEmail, client, secret,
// 				loyaltyCardNumber);

// 		Assert.assertEquals(signUpResponse.getStatusCode(), 200,
// 				"Status code 200 did not match for mobile sign up API");

// 		utils.logit("Pass", "Mobile sign up is successful");

// 		String emailUserId = signUpResponse.jsonPath().get("user.user_id").toString();
// 		String loyaltyCardNumberResp = signUpResponse.jsonPath().get("user.loyalty_cards[0].card_number").toString();

// 		Assert.assertEquals(loyaltyCardUserId, emailUserId, "Loyalty card user ID and email user ID did not match");
// 		Assert.assertEquals(loyaltyCardNumberResp, loyaltyCardNumber,
// 				"Loyalty card number did not match between POS and email user");

// 		// Validate sync with identity
// 		intUtils.validateUserSyncWithIdentity(emailUserId);
// 		utils.logit("Pass", "POS user merge with mobile user is successfull");
// 	}

// 	@Test(description = "SQ-T6202 INT2-2027 | UserMerge_AuthApi_loyaltyCard")
// 	public void SQ_T6207_UserMerge_AuthApi_loyaltyCard_test() throws Exception {
// 		// Create a Loyalty card user via signup API
// 		Response resp = pageObj.endpoints().posSignUpWithLoyaltyCardOnly(loyaltyCardNumber, locationKey);
// 		Assert.assertEquals(200, resp.getStatusCode(), "Status code 200 did not matched for pos signup api");
// 		String loyaltyCardUserEmail = resp.jsonPath().get("email").toString();
// 		Assert.assertTrue(loyaltyCardUserEmail.contains("@cards.punchh.com"), "POS user email did not match expected format");
// 		String loyaltyCardUserId = resp.jsonPath().get("id").toString();
// 		utils.logit("POS signup with phone only is successful");

// 		// Validate sync with identity
// 		intUtils.validateUserSyncWithIdentity(loyaltyCardUserId);

// 		// User Sign-up with same loyalty card number
// 		Response signUpResponse = null;
// 		signUpResponse = pageObj.endpoints().authApiSignUpLoyaltyCard(userEmail, client, secret,
// 				loyaltyCardNumber);

// 		Assert.assertEquals(signUpResponse.getStatusCode(), 201,
// 				"Status code 201 did not match for mobile sign up API");

// 		utils.logit("Pass", "Mobile sign up is successful");

// 		String emailUserId = signUpResponse.jsonPath().get("id").toString();
// 		String loyaltyCardNumberResp = signUpResponse.jsonPath().get("loyalty_cards[0].card_number").toString();

// 		Assert.assertEquals(loyaltyCardUserId, emailUserId, "Loyalty card user ID and email user ID did not match");
// 		Assert.assertEquals(loyaltyCardNumberResp, loyaltyCardNumber,
// 				"Loyalty card number did not match between POS and email user");

// 		// Validate sync with identity
// 		intUtils.validateUserSyncWithIdentity(emailUserId);
// 		utils.logit("Pass", "POS user merge with mobile user is successfull");
// 	}

// 	@Test(description = "SQ-T6202 INT2-2028 | POSUserMerge_MobileApi1_UpdateExistingUserAsync")
// 	public void SQ_T6208_POSUserMerge_MobileApi1_UpdateExistingUserAsync_test() throws Exception {

// 		// Create a POS user via signup API
// 		Response posResp = pageObj.endpoints().posSignUpWithoutEmail(phone, locationKey);
// 		Assert.assertEquals(200, posResp.getStatusCode(), "Status code 200 did not matched for pos signup api");
// 		String posUserEmail = posResp.jsonPath().get("email").toString();
// 		Assert.assertTrue(posUserEmail.contains("@phone.punchh.com"), "POS user email did not match expected format");
// 		String posUserId = posResp.jsonPath().get("id").toString();
// 		utils.logit("POS signup with phone only is successful");

// 		// Validate sync with identity
// 		intUtils.validateUserSyncWithIdentity(posUserId);

// 		// Checkin 1 on POS user
// 		String key = utils.getTimestampInNanoseconds();
// 		String txn = "123456" + CreateDateTime.getTimeDateString();
// 		String date = CreateDateTime.getCurrentDate() + "T10:50:00+05:30";
// 		Response resp = pageObj.endpoints().posCheckin(date, posUserEmail, key, txn, dataSet.get("locationKey"));
// 		Assert.assertEquals(200, resp.getStatusCode(), "Status code 200 did not matched for pos chekin api");
// 		Assert.assertEquals(resp.jsonPath().get("email").toString(), posUserEmail.toLowerCase());
// 		utils.logit("Pass", "First POS checkin for user with phone only was successful");

// 		// Checkin 2 on POS user
// 		String key1 = utils.getTimestampInNanoseconds();
// 		String txn1 = "123456" + CreateDateTime.getTimeDateString();
// 		String date1 = CreateDateTime.getCurrentDate() + "T10:50:00+05:30";
// 		Response resp1 = pageObj.endpoints().posCheckin(date1, posUserEmail, key1, txn1, dataSet.get("locationKey"));
// 		Assert.assertEquals(200, resp1.getStatusCode(), "Status code 200 did not matched for pos chekin api");
// 		Assert.assertEquals(resp1.jsonPath().get("email").toString(), posUserEmail.toLowerCase());
// 		String total_credits = resp1.jsonPath().get("balance.total_credits").toString();
// 		utils.logit("Pass", "Second POS checkin for user with phone only was successful");

// 		// Loyalty User Sign-up with email only
// 		Response signUpResponse = null;
// 		signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, client, secret);
// 		Assert.assertEquals(signUpResponse.getStatusCode(), 200,
// 				"Status code 200 did not match for mobile sign up API");
// 		utils.logit("Pass", "Mobile sign up is successful");
// 		String token = signUpResponse.jsonPath().get("access_token.token").toString();
// 		String emailUserId = signUpResponse.jsonPath().get("user.user_id").toString();

// 		// Validate sync with identity
// 		intUtils.validateUserSyncWithIdentity(emailUserId);

// 		// Update loyalty user profile using API1 - update same phone number
// 		Response updateGuestResponse = pageObj.endpoints().api1UpdateUserEmailPhone(client, secret, token, userEmail,
// 				String.valueOf(phone));
// 		Assert.assertEquals(updateGuestResponse.getStatusCode(), 200, "Status code 200 did not matched for api2 signup");
// 		utils.logit("Pass", "Update user profile using API1 is successful");

// 		// Validate POS (phone-only) User Deletion from DB
// 		String userDeletionQuery = "select count(*) as count from users where id = '"+ posUserId +"'";
// 		boolean isUserDeleted = DBUtils.verifyValueFromDBUsingPolling(env, userDeletionQuery, "count","0");
// 		Assert.assertTrue(isUserDeleted, "POS (phone-only) User is not deleted from DB");
// 		utils.logit("Pass", "POS (phone-only) User is deleted from DB");

// 		// Validate POS (phone-only) User Deletion from identity DB
// 		String identityUserDeletionQuery = "select count(*) as count from " + identityDbName + ".users where punchh_user_id = '"+ posUserId +"'";
// 		boolean isIdentityUserDeleted = DBUtils.verifyValueFromDBUsingPolling(env, identityUserDeletionQuery, "count","0");
// 		Assert.assertTrue(isIdentityUserDeleted, "POS (phone-only) User is not deleted from identity DB");
// 		utils.logit("Pass", "POS (phone-only) User is deleted from identity DB");

// 		// Validate POS user phone number moved to loyalty user
// 		String phoneQuery = "select count(*) as count, id from users where phone = '"+ phone + "'";
// 		boolean phoneRecordExists = DBUtils.verifyValueFromDBUsingPolling(env, phoneQuery, "count","1");
// 		boolean phoneUserLinked = DBUtils.verifyValueFromDBUsingPolling(env, phoneQuery, "id", emailUserId);
// 		Assert.assertTrue(phoneRecordExists, "POS user phone number is not moved to loyalty user");
// 		Assert.assertTrue(phoneUserLinked, "POS user phone number is not moved to loyalty user");
// 		utils.logit("Pass", "POS user phone number is moved to loyalty user");

// 		// Validate email user sync in identity DB
// 		intUtils.validateUserSyncWithIdentity(emailUserId);

// 		// Validate user_id's in loyalty_pos_users table
// 		String mergeQuery = "select merged_user_id from loyalty_pos_users where loyalty_user_id = '" + emailUserId + "'";
// 		String mergedUserId = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, mergeQuery,"merged_user_id", 10);
// 		Assert.assertEquals(mergedUserId, posUserId, "merged_user_id is not equal to expected POS user_id ");
// 		utils.logit("Pass", "Merged user id " + mergedUserId + " matches the POS user id " + posUserId);
		
// 		// Validate checkins moved to loyalty(Email) user from POS(mobile) user
// 		String checkinQueryOldUser = "select count(*) as count from checkins where user_id = '"+ posUserId + "' and channel ='POS'";
// 		boolean checkinsNotPresentOld = DBUtils.verifyValueFromDBUsingPolling(env, checkinQueryOldUser, "count","0");
// 		Assert.assertTrue(checkinsNotPresentOld, "Checkins are not moved to loyalty(Email) user from POS(mobile) user");
		
// 		String checkinQueryMobileUser = "select count(*) as count from checkins where user_id = '"+ emailUserId + "' and channel ='POS'";
// 		boolean checkinsPresentMobile = DBUtils.verifyValueFromDBUsingPolling(env, checkinQueryMobileUser, "count","2");
// 		Assert.assertTrue(checkinsPresentMobile, "Checkins are not moved to loyalty(Email) user from POS(mobile) user");
// 		utils.logit("Pass", "Checkins are moved to loyalty(Email) user from POS(mobile) user");
		
// 		// Validate points/gifts moved to loyalty(Email) user from POS(mobile) user
// 		String pointsQueryOldUser = "select count(*) as count from accounts where user_id = '"+ posUserId + "'";
// 		boolean pointsNotPresentOld = DBUtils.verifyValueFromDBUsingPolling(env, pointsQueryOldUser, "count","0");
// 		Assert.assertTrue(pointsNotPresentOld, "Points/gifts are not moved to loyalty(Email) user from POS(mobile) user");
		
// 		String pointsQueryMobileUser = "select count(*) as count,total_credits from accounts where user_id = '"+ emailUserId +"'";
// 		boolean accountRecordExists = DBUtils.verifyValueFromDBUsingPolling(env, pointsQueryMobileUser, "count","1");
// 		boolean creditsMatch = DBUtils.verifyValueFromDBUsingPolling(env, pointsQueryMobileUser, "total_credits", total_credits);
// 		Assert.assertTrue(accountRecordExists, "Points/gifts are not moved to loyalty(Email) user from POS(mobile) user");
// 		Assert.assertTrue(creditsMatch, "Points/gifts are not moved to loyalty(Email) user from POS(mobile) user");
// 		utils.logit("Pass", "Points/gifts are moved to loyalty(Email) user from POS(mobile) user");
// 	}

// 	@AfterMethod(alwaysRun = true)
// 	public void tearDown() {
// 		if (dataSet != null)
// 			pageObj.utils().clearDataSet(dataSet);
// 		utils.logit("Test Case: " + sTCName + " finished");
// 		driver.quit();
// 		utils.logit("Browser closed");
// 	}
// }
