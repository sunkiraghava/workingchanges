package com.punchh.server.loyalty2;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.annotations.Owner;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.apiConfig.ApiResponseJsonSchema;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.ApiUtils;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

// Author - Shashank sharma
@Listeners(TestListeners.class)
public class ActivateDeactivateUsersFunctionalityTest {
	static Logger logger = LogManager.getLogger(ActivateDeactivateUsersFunctionalityTest.class);
	public WebDriver driver;
	String email = "autoemailTemp@punchh.com";
	ApiUtils apiUtils;
	PageObj pageObj;
	String sTCName;
	private String env, run = "ui";
	private String baseUrl;
	private Utilities utils;
	private static Map<String, String> dataSet;

	@BeforeMethod(alwaysRun = true)
	public void beforeMethod(Method method) {
		driver = new BrowserUtilities().launchBrowser();
		apiUtils = new ApiUtils();
		utils = new Utilities(driver);
		pageObj = new PageObj(driver);
		dataSet = new ConcurrentHashMap<>();
		sTCName = method.getName();
		env = pageObj.getEnvDetails().setEnv();
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run , env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run , env , "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==>" + dataSet);
	}

	@Test(description = "SQ-T3262 When “Hide Banned” option is selected in Deactivated Filter drop down all banned guests will be hidden from the list displayed", groups = { "regression", "dailyrun" })
	@Owner(name = "Shashank Sharma")
	public void T3262_HideBanneduserFilter() throws InterruptedException {

		// signup user1
		String userEmail1 = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail1, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token1 = signUpResponse.jsonPath().get("access_token.token").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("Api2 user signup is successful");
		Thread.sleep(2000);
		// // signup user2
		String userEmail2 = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse2 = pageObj.endpoints().Api2SignUp(userEmail2, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse2, "API 2 user signup");
		String token2 = signUpResponse2.jsonPath().get("access_token.token").toString();
		Assert.assertEquals(signUpResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("Api2 user signup is successful");

		// get the barcode
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// generateBarcode
		pageObj.menupage().navigateToSubMenuItem("Support", "Test Barcodes");
		pageObj.instanceDashboardPage().generateBarcodeWithTransactionNumber(dataSet.get("location"), "", "150", "300");
		pageObj.instanceDashboardPage().clickOnGenerateBarcodeButton();
		// pageObj.instanceDashboardPage().generateBarcode(dataSet.get("location"));
		String barcode = pageObj.instanceDashboardPage().captureBarcode();
		utils.logPass(barcode + " barcode generated successfully");

		// user1 checkin barcode
		Response barcodeCheckinResponse1 = pageObj.endpoints().Api2LoyaltyCheckinBarCode(dataSet.get("client"),
				dataSet.get("secret"), token1, barcode);
		Assert.assertEquals(barcodeCheckinResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 Loyalty Checkin by bar code");
		TestListeners.extentTest.get().pass("api2 loyalty checkin using barcode is done");

		// same barcode checkin by user2 ( getting error message )
		Response barcodeCheckinResponse2 = pageObj.endpoints().Api2LoyaltyCheckinBarCode(dataSet.get("client"),
				dataSet.get("secret"), token2, barcode);
		Assert.assertEquals(barcodeCheckinResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 201 did not matched for api2 Loyalty Checkin by bar code");
		TestListeners.extentTest.get().pass("api2 loyalty checkin using barcode is done");

		// same barcode checkin by user2 ( getting error message )
		Response barcodeCheckinResponse3 = pageObj.endpoints().Api2LoyaltyCheckinBarCode(dataSet.get("client"),
				dataSet.get("secret"), token2, barcode);
		Assert.assertEquals(barcodeCheckinResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 201 did not matched for api2 Loyalty Checkin by bar code");
		TestListeners.extentTest.get().pass("api2 loyalty checkin using barcode is done");

		// go to user2 timeline page
		System.out.println("Searching user2 for banning --" + userEmail2);
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail2);

		// add note with comment
		pageObj.guestTimelinePage().navigateToTabs("Add Note");
		pageObj.guestTimelinePage().addNoteAndBanGuest();
		pageObj.menupage().clickDashboardMenu();
		// navigate to fraud suspect window Guest> Fraud suspect
		pageObj.menupage().navigateToSubMenuItem("Guests", "Fraud Suspects");

		pageObj.guestTimelinePage().clickOnTopSuspectsTabInFraudSuspectPage();
		boolean userIsVisible = pageObj.guestTimelinePage().verifyBanGuestIsVisilble(userEmail2);
		Assert.assertTrue(userIsVisible, "Banned guest is not displayed in the top suspect list");
		logger.info("Verified that Banned guest is displayed in top suspect list ");
		TestListeners.extentTest.get().pass("Verified that Banned guest is displayed in top suspect list ");

		pageObj.guestTimelinePage().selectDeactivatedFilter("Hide Banned");
		Thread.sleep(2000);
		boolean userNotVisible = pageObj.guestTimelinePage().verifyBanGuestIsVisilble(userEmail2);
		// boolean userNotVisible =
		// pageObj.guestTimelinePage().verifyBanGuestIsVisilble(userEmail2);
		Assert.assertFalse(userNotVisible, "Banned guest is displayed in the top suspect list");

		logger.info("Verified that Banned guest is not displayed in top suspect list after apply Hide Banned filter");
		TestListeners.extentTest.get()
				.pass("Verified that Banned guest is not displayed in top suspect list after apply Hide Banned filter");

		pageObj.guestTimelinePage().clickOnSuspiciousActivitiesInFraudSuspectPage();
		pageObj.guestTimelinePage().cleanAllUsersFromSuspiciousActivitiesTab();
	}

	@Test(description = "SQ-T4248, SQ-T4247, SQ-T4246,SQ-T4249,SQ-T4245 SQ-T3198 Verify deactivated guest try to login via AUTH API and response", groups = {
			"regression", "dailyrun" }, priority = 1)
	@Owner(name = "Shashank Sharma")
	public void T4248_verifyDeactivatedGuestTryToLoginViaAUTHAPIAndResponse() throws InterruptedException {

		// user creation using pos signup api
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response resp = pageObj.endpoints().posSignUp(userEmail, dataSet.get("locationkey"));
		Assert.assertEquals(200, resp.getStatusCode(), "Status code 200 did not matched for pos signup api");
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().performGuestFunctions("Deactivate", "");
		String val = pageObj.guestTimelinePage().deactivationStatus();
		Assert.assertEquals(val, "Deactivated", "Deactivated lable text didnt matched on timeline");
		String guestEmail = pageObj.guestTimelinePage().getGuestTimelineEmail();
		Assert.assertEquals(guestEmail, userEmail, "Guest email didnot matched after deactivation");
		// login via auth API
		Response loginResponse = pageObj.endpoints().authApiUserLogin(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(loginResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED, "Deactivated user should not able to login");
		String msg = loginResponse.jsonPath().get("error").toString();
		Assert.assertEquals("This guest has been deactivated from the loyalty program.", msg,
				"Auth login error message did not matched");

		// API1 user login
		Response loginResponseAPI1 = pageObj.endpoints().Api1UserLogin(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(loginResponseAPI1.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY);
		String errMsgAPI1 = loginResponseAPI1.jsonPath().get("base[0]").toString();
		Assert.assertEquals("This guest has been deactivated from the loyalty program.", errMsgAPI1,
				"API1 login error message did not matched");

		// api 2 login
		Response loginResponseAPI2 = pageObj.endpoints().Api2Login(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(loginResponseAPI2.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY);
		String errMsgAPI2 = loginResponseAPI2.jsonPath().get("errors.base[0]").toString();
		Assert.assertEquals("This guest has been deactivated from the loyalty program.", errMsgAPI2,
				"API2 login error message did not matched");

		// navigate to Menu -> Submenu
		pageObj.menupage().navigateToSubMenuItem("Guests", "Guests");
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		String guestTimelineEmail = pageObj.guestTimelinePage().getGuestTimelineEmail();
		String val1 = pageObj.guestTimelinePage().deactivationStatus();
		Assert.assertEquals(val1, "Deactivated", "Deactivated lable text didnt matched on timeline");
		Assert.assertEquals(userEmail, guestTimelineEmail, "Guest email did not matched on timeline header");
		TestListeners.extentTest.get().pass("Successfully verified guest email and Banned status from global search");

		// navigate to Menu -> Submenu
		pageObj.menupage().navigateToSubMenuItem("Guests", "Deactivated");
		boolean status = pageObj.guestTimelinePage().checkSearchOption();
		Assert.assertFalse(status, "Search option found on deactivated guest page. It should not appear");
		// navigate to Menu -> Submenu
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.redemptionsPage().verifyEnableUUIDStrategyForOnlineOrderingChannel();

	}

	@Test(description = "SQ-T2648 Verify user can be deactivated and anyomised by superadmin", groups = {
			"regression", "dailyrun" }, priority = 2)
	@Owner(name = "Shashank Sharma")
	public void T3579_verifyUserCanBeDeactivatedAndAnyomisedBySuperadmin() throws InterruptedException {

		// user creation using pos signup api
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response resp = pageObj.endpoints().posSignUp(userEmail, dataSet.get("locationKey"));
		Assert.assertEquals(200, resp.getStatusCode(), "Status code 200 did not matched for pos signup api");
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().performGuestFunctions("Deactivate", "");
		String val = pageObj.guestTimelinePage().deactivationStatus();
		Assert.assertEquals(val, "Deactivated", "Deactivated lable text didnt matched on timeline");
		pageObj.guestTimelinePage().performGuestFunctions("Delete/Anonymize Guest?", "Delete-General");
		String delstatus = pageObj.guestTimelinePage().delationStatus();
		Assert.assertTrue(delstatus.contains("This guest profile will be deleted on"),
				"guest deletion label did not displayed on timeline");

	}

	// shashank sharma
	@Test(description = "SQ-T4683 (1.0)/ LPE-T1324 Verify Reactivation Requested tab should have list of users which have requested for reactivation.; "
			+ "SQ-T4690 (1.0)/LPE-T1335 Verify the reactivation template should be visible in setting --> notification template admin should be able to enable; "
			+ "SQ-T4684 Verify Reactivation Requested tab should have field Name, Email, reactivate button; "
			+ "SQ-T4682 (1.0)/LPE-T1323 Verify Reactivation Requested tab is visible under guest TAB; "
			+ "SQ-T4685 (1.0)/LPE-T1326 Verify Reactivation Requested tab is having reactivate button working; "
			+ "SQ-T5738 Verify Negative Scenarios of Deactivate and Reactivate User Profile", groups = { "regression", "dailyrun" })
	@Owner(name = "Shashank Sharma")
	public void T4683_VerifyUsersIsComingInReactivationRequestedTab() throws InterruptedException {

		// User signup
		utils.logit("== User signup ==");
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api1 signup");
		TestListeners.extentTest.get().pass("Api1 user signup is successful");
		String authToken = signUpResponse.jsonPath().get("auth_token.token");
		utils.logPass("User signup is successful");

		// Deactivate user with valid data
		utils.logit("== Deactivate user with valid data ==");
		Response deactivateResponse = pageObj.endpoints().DeactivateUserAPI(dataSet.get("client"),
				dataSet.get("secret"), authToken);
		Assert.assertEquals(deactivateResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean isApiDeactivateUserSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.apiMessageObjectSchema, deactivateResponse.asString());
		Assert.assertTrue(isApiDeactivateUserSchemaValidated,
				"API Deactivate User with valid data Schema Validation failed");
		Assert.assertTrue(
				deactivateResponse.jsonPath().get("message").toString().contains("Guest deactivated successfully"));
		utils.logPass("Deactivate User with valid data is successful");

		// Negative case: Deactivate User Profile with invalid signature
		utils.logit("== Deactivate User Profile with invalid signature ==");
		Response deactivateUserInvalidSignatureResponse = pageObj.endpoints().DeactivateUserAPI("1",
				dataSet.get("secret"), authToken);
		Assert.assertEquals(deactivateUserInvalidSignatureResponse.getStatusCode(), 412);
		boolean isApiDeactivateUserInvalidSignatureSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, deactivateUserInvalidSignatureResponse.asString());
		Assert.assertTrue(isApiDeactivateUserInvalidSignatureSchemaValidated,
				"API Deactivate User with invalid signature Schema Validation failed");
		String deactivateUserInvalidSignatureErrorMsg = deactivateUserInvalidSignatureResponse.jsonPath().get("[0]");
		Assert.assertEquals(deactivateUserInvalidSignatureErrorMsg, "Invalid Signature");
		utils.logPass("Deactivate User Profile is unsuccessful due to invalid signature");

		// Negative case: Deactivate User Profile with invalid auth token
		utils.logit("== Deactivate User Profile with invalid auth token ==");
		Response deactivateUserInvalidAuthTokenResponse = pageObj.endpoints().DeactivateUserAPI(dataSet.get("client"),
				dataSet.get("secret"), "1");
		Assert.assertEquals(deactivateUserInvalidAuthTokenResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED);
		boolean isApiDeactivateUserInvalidAuthTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, deactivateUserInvalidAuthTokenResponse.asString());
		Assert.assertTrue(isApiDeactivateUserInvalidAuthTokenSchemaValidated,
				"API Deactivate User with invalid auth token Schema Validation failed");
		String deactivateUserInvalidAuthTokenErrorMsg = deactivateUserInvalidAuthTokenResponse.jsonPath().get("error");
		Assert.assertEquals(deactivateUserInvalidAuthTokenErrorMsg,
				"You need to sign in or sign up before continuing.");
		utils.logPass("Deactivate User Profile is unsuccessful due to invalid auth token");

		// Reactivate User Profile with valid data
		utils.logit("== Reactivate User Profile with valid data ==");
		Response reactivationRequestResponse = pageObj.endpoints().mobileAPI2ReactivationRequest(dataSet.get("client"),
				dataSet.get("secret"), userEmail);
		Assert.assertEquals(reactivationRequestResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean isApi2ReactivationRequestSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiMessageObjectSchema, reactivationRequestResponse.asString());
		Assert.assertTrue(isApi2ReactivationRequestSchemaValidated,
				"API v2 Reactivation Request Schema Validation failed");
		String reactivationRequestSuccessMsg = reactivationRequestResponse.jsonPath().get("message");
		Assert.assertEquals(reactivationRequestSuccessMsg,
				"Please check your email for instructions on reactivating your account");
		utils.logPass("Reactivate User Profile with valid data is successful");

		// Negative case: Reactivate User Profile by resending reactivation request
		TestListeners.extentTest.get().info("== Reactivate User Profile by resending reactivation request ==");
		logger.info("== Reactivate User Profile by resending reactivation request ==");
		Response reactivateUserResendResponse = pageObj.endpoints().mobileAPI2ReactivationRequest(dataSet.get("client"),
				dataSet.get("secret"), userEmail);
		Assert.assertEquals(reactivateUserResendResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY);
		boolean isApi2ReactivateUserResendSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, reactivateUserResendResponse.asString());
		Assert.assertTrue(isApi2ReactivateUserResendSchemaValidated,
				"API2 Reactivate User by resending reactivation request Schema Validation failed");
		String reactivateUserResendErrorMsg = reactivateUserResendResponse.jsonPath().get("error");
		Assert.assertEquals(reactivateUserResendErrorMsg, "There is already an ongoing request");
		utils.logPass("Reactivate User Profile by resending reactivation request is unsuccessful");

		// Negative case: Reactivate User Profile with invalid client
		utils.logit("== Reactivate User Profile with invalid client ==");
		Response reactivateUserInvalidClientResponse = pageObj.endpoints().mobileAPI2ReactivationRequest("1",
				dataSet.get("secret"), userEmail);
		Assert.assertEquals(reactivateUserInvalidClientResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED);
		boolean isApi2ReactivateUserInvalidClientSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnknownClientSchema, reactivateUserInvalidClientResponse.asString());
		Assert.assertTrue(isApi2ReactivateUserInvalidClientSchemaValidated,
				"API2 Reactivate User with invalid client Schema Validation failed");
		String reactivateUserInvalidClientErrorMsg = reactivateUserInvalidClientResponse.jsonPath()
				.get("errors.unknown_client[0]");
		Assert.assertEquals(reactivateUserInvalidClientErrorMsg,
				"Client ID is incorrect. Please check client param or contact us");
		utils.logPass("Reactivate User Profile is unsuccessful due to invalid client");

		// Negative case: Reactivate User Profile with invalid secret
		utils.logit("== Reactivate User Profile with invalid secret ==");
		Response reactivateUserInvalidSecretResponse = pageObj.endpoints()
				.mobileAPI2ReactivationRequest(dataSet.get("client"), "1", userEmail);
		Assert.assertEquals(reactivateUserInvalidSecretResponse.getStatusCode(), 412);
		boolean isApi2ReactivateUserInvalidSecretSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2InvalidSignatureSchema, reactivateUserInvalidSecretResponse.asString());
		Assert.assertTrue(isApi2ReactivateUserInvalidSecretSchemaValidated,
				"API2 Reactivate User with invalid secret Schema Validation failed");
		String reactivateUserInvalidSecretErrorMsg = reactivateUserInvalidSecretResponse.jsonPath()
				.get("errors.invalid_signature[0]");
		Assert.assertEquals(reactivateUserInvalidSecretErrorMsg,
				"Signature doesn't match. For information about generating the x-pch-digest header, see https://developers.punchh.com.");
		utils.logPass("Reactivate User Profile is unsuccessful due to invalid secret");

		// Negative case: Reactivate User Profile with invalid email
		utils.logit("== Reactivate User Profile with invalid email ==");
		Response reactivateUserInvalidEmailResponse = pageObj.endpoints()
				.mobileAPI2ReactivationRequest(dataSet.get("client"), dataSet.get("secret"), "1");
		Assert.assertEquals(reactivateUserInvalidEmailResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY);
		boolean isApi2ReactivateUserInvalidEmailSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, reactivateUserInvalidEmailResponse.asString());
		Assert.assertTrue(isApi2ReactivateUserInvalidEmailSchemaValidated,
				"API2 Reactivate User with invalid email Schema Validation failed");
		String reactivateUserInvalidEmailErrorMsg = reactivateUserInvalidEmailResponse.jsonPath().get("error");
		Assert.assertTrue(reactivateUserInvalidEmailErrorMsg.contains("Unable to find user"));
		utils.logPass("Reactivate User Profile is unsuccessful due to invalid email");

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Guests", "Reactivation Requested");
		boolean userIsDisplayed = pageObj.lapsedguestPage().verifyUserIsExistOnReactivationRequestedPage(userEmail);
		Assert.assertTrue(userIsDisplayed, userEmail + " is not visible on Reactivation Requested page");

		utils.logPass(userEmail + " user is visible on reactivatation requested page ");

		pageObj.lapsedguestPage().reactivateTheUser(userEmail);

		pageObj.menupage().navigateToSubMenuItem("Settings", "Notification Templates");

		boolean notificationTemplateIsDisplayed = pageObj.settingsPage()
				.verifyNotificationTemplateIsDisplaying("Reactivation Instruction");

		Assert.assertTrue(notificationTemplateIsDisplayed,
				"Reactivation Instruction is not displayed on Notification Template page.");

		logger.info("Reactivation Instruction is  displayed on Notification Template page.");
		TestListeners.extentTest.get().pass("Reactivation Instruction is  displayed on Notification Template page.");

	}

	@Test(description = "SQ-T2912 Deletion/deactivation of the user using API"
			+ "SQ-T4691 (1.0) / LPE-T1182 Verify after deactivating user, it can be reactivated or not", groups = {
					"regression", "dailyrun" }, priority = 0)
	@Owner(name = "Shashank Sharma")
	public void T2912_deletedeactivateuser() throws InterruptedException {

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// pageObj.menupage().clickWhiteLabelMenu();
		// pageObj.menupage().mobileConfigurationslink();
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "Mobile Configuration");
		pageObj.mobileconfigurationPage().ClickAccountDeletionBtn("Account Deletion");
		pageObj.Accountdeletionpage().clickIosDeactivation();
		pageObj.Accountdeletionpage().clickAndroidDeactivation();
		pageObj.Accountdeletionpage().appDeletionTypeAndReason(dataSet.get("iosDeletionType"),
				dataSet.get("androidDeletionType"), dataSet.get("iosDeletionReason"));
		pageObj.Accountdeletionpage().Update();
		// // SignupAPI
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 1 user signup");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api1 signup");
		TestListeners.extentTest.get().pass("Api1 user signup is successful");
		String authToken = signUpResponse.jsonPath().get("auth_token.token");
		System.out.println(signUpResponse.prettyPrint());

		// Deactivateuser
		Response deactivateResponse = pageObj.endpoints().DeactivateUserAPI(dataSet.get("client"),
				dataSet.get("secret"), authToken);
		Assert.assertEquals(deactivateResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertTrue(
				deactivateResponse.jsonPath().get("message").toString().contains("Guest deactivated successfully"));

		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		boolean deactivateIsDispalyed = pageObj.guestTimelinePage().verifyTagsOnUserTimeline(userEmail, "Deactivated");
		Assert.assertTrue(deactivateIsDispalyed, "Deactivated tag is not visible on user time line page");

		pageObj.guestTimelinePage().navigateToTabs("Edit Profile");
		pageObj.guestTimelinePage().deactivateReactivateUser("Reactivate");

		deactivateIsDispalyed = pageObj.guestTimelinePage().verifyTagsOnUserTimeline(userEmail, "Deactivated");
		Assert.assertFalse(deactivateIsDispalyed, "Deactivated tag is visible on user time line page");

		// DeleteUserAPI
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse1 = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse1, "API 1 user signup");
		Assert.assertEquals(signUpResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api1 signup");
		TestListeners.extentTest.get().pass("Api1 user signup is successful");
		String authToken1 = signUpResponse1.jsonPath().get("auth_token.token");
		// pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		Response deleteUserResponse = pageObj.endpoints().DeleteUserAPI(dataSet.get("client"), dataSet.get("secret"),
				authToken1);
		Assert.assertEquals(deleteUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertTrue(deleteUserResponse.jsonPath().get("message").toString()
				.contains("Guest marked for deletion. Data will be automatically deleted within"));
		// pageObj.menupage().clickGuestMenu();
		pageObj.instanceDashboardPage().verifyDeletedGuests(userEmail);

	}

	// Anant
	@Test(description = "SQ-T4559 Verify banned and deactivated user are both banned and deactivated list", groups = { "regression", "dailyrun" })
	@Owner(name = "Shashank Sharma")
	public void T4559_verifiedBannedDeactivatedList() throws Exception {
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// create a user
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api1 signup");
		utils.logPass("Api1 user signup is successful");
		String userID = signUpResponse.jsonPath().get("user_id").toString();

		// nagivate to user timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().navigateToTabs("Add Note");

		// add comment and ban the user
		pageObj.guestTimelinePage().addComment(dataSet.get("comment"));
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("user_note_banned", "check");
		pageObj.guestTimelinePage().clickUserNoteButton();

		// now deactivate the user
		pageObj.guestTimelinePage().navigateToTabs("Edit Profile");
		pageObj.guestTimelinePage().deactivateGuestWithAllowReactivationOrNot(dataSet.get("allowReactivation"));

		//verify deactivated guest through db
		String query6 = "SELECT `unsubscribe_reason` FROM `users` WHERE `id` = '" + userID + "';";
		String expColValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query6, "unsubscribe_reason", 50);
		Assert.assertNotNull(expColValue,
				"Value for unsubscribe_reason is Null for deactivated user");
		boolean flag = expColValue.contains("Deactivated by Admin");
		Assert.assertEquals(flag, true, "unsubscribe_reason does not contain 'Deactivated by Admin'");
		utils.logPass("Verified deactivated email found in the deactivation tab");

		// check user status
		for (int i = 1; i < 3; i++) {
			boolean val = pageObj.guestTimelinePage().labelVisible(dataSet.get("label" + i));
			Assert.assertTrue(val, "label --" + dataSet.get("label" + i) + " is not visible");
			utils.logit("label --" + dataSet.get("label" + i) + " is visible");
		}

		// SideKiq schedules running
		utils.switchToWindowN(1);
		pageObj.cockpitRedemptionsPage().runSidekiqJob(baseUrl, userID);
		utils.switchToParentWindow();

		// go to guest --> guest --> banned
		pageObj.menupage().navigateToSubMenuItem("Guests", "Guests");
		pageObj.guestTimelinePage().guestTabNagivation("Banned");
		boolean val2 = pageObj.guestTimelinePage().verifyBannedGuestIsVisible(userEmail);
		Assert.assertTrue(val2, "Ban user is not visible in the Banned tab");
		utils.logPass("Verified user is visible on the Banned tab");

	}

	@AfterMethod(alwaysRun = true)
	public void afterClass() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}

}
