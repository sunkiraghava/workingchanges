package com.punchh.server.OMMTest;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Properties;
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
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class OMM357_singleScanCode {
	private static Logger logger = LogManager.getLogger(OMM357_singleScanCode.class);
	public WebDriver driver;
	private Properties prop;
	private PageObj pageObj;
	private String userEmail;
	private String sTCName;
	private String baseUrl;
	String blankString = "";
	private String env, run = "ui";
	private static Map<String, String> dataSet;

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {

		prop = Utilities.loadPropertiesFile("config.properties");
		driver = new BrowserUtilities().launchBrowser();
		sTCName = method.getName();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==>" + dataSet);

	}

	@Test(priority = -1, groups = { "regression", "dailyrun" })
	@Owner(name = "Shashank Sharma")
	public void singleScanPreRequisite() throws InterruptedException {

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().navigateToTabs("Guest Validation");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboard("Validate privacy policy", "uncheck");

	}

	@Test(description = "OMM-T168 Validate that User is able to generate single scan token using payment method- PaypalBA", groups = {
			"regression", "dailyrun" })
	@Owner(name = "Ashwini Shetty")
	public void SingleScanCodeWithPaypalBA() {

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 1 user signup");
		String token = signUpResponse.jsonPath().get("auth_token.token").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api1 signup");
		pageObj.utils().logPass("API1 Signup is successful");

		Response singleScanCodeResponse = pageObj.endpoints().singleScanCodeWithPaypalBA(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(singleScanCodeResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for the single scan code API");
		pageObj.utils().logPass("single Scan Code successful");

	}

	@Test(description = "OMM-T157 If single scan token has been generated and user tried to lookup using '{{path}}/api/pos/users/find' API and puts valid token", groups = {
			"regression", "dailyrun" })
	@Owner(name = "Ashwini Shetty")
	public void SingleScanCodeonUserLookup() {

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 1 user signup");
		String token = signUpResponse.jsonPath().get("auth_token.token").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api1 signup");
		pageObj.utils().logPass("API1 Signup is successful");

		Response singleScanCodeResponse = pageObj.endpoints().singleScanCodeWithPaypalBA(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(singleScanCodeResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for the single scan code API");
		pageObj.utils().logPass("single Scan Code successful");

		String code = singleScanCodeResponse.jsonPath().get("single_scan_code").toString();

		Response userLookupResponse = pageObj.endpoints().userLookup(code, dataSet.get("locationkey"));
		Assert.assertEquals(userLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for the user lookup API");
		pageObj.utils().logPass("userlookup using single scan code successful");

	}

	@Test(groups = { "regression", "dailyrun" })
	@Owner(name = "Ashwini Shetty")
	public void SingleScanCodeUsingDifferentPaymentType() {
		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 1 user signup");
		String token = signUpResponse.jsonPath().get("auth_token.token").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api1 signup");
		pageObj.utils().logPass("API1 Signup is successful");

//		Response purchaseGiftCardResp = pageObj.endpoints().Api1PurchaseGiftCard(userEmail, dataSet.get("client"),
//				dataSet.get("secret"), dataSet.get("amount"), token);
//		Assert.assertEquals(purchaseGiftCardResp.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
//				"Status code 200 did not matched for api1 gift card purchase");
//		TestListeners.extentTest.get().pass("Api1 purchase goft card is successful ");
//		String uuid = purchaseGiftCardResp.jsonPath().get("uuid").toString();
//
//		Response singleScanCodeResponse = pageObj.endpoints().singleScanCodeWithGiftcard(dataSet.get("client"),
//				dataSet.get("secret"), token, uuid);
//		Assert.assertEquals(singleScanCodeResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
//				"Status code 200 did not matched for the single scan code API");
//		TestListeners.extentTest.get().pass("single Scan Code successful");
//		logger.info("single Scan Code is successful");

		Response singleScanCodeResponse1 = pageObj.endpoints().singleScanCodeWithCreditCard(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(singleScanCodeResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for the single scan code API");
		pageObj.utils().logPass("single Scan Code successful");

	}

	@Test(description = "OMM-T172 valid redeemable id", groups = { "regression", "dailyrun" })
	@Owner(name = "Ashwini Shetty")
	public void T172_singleScanRedeemableID() {
		// User register/signup using API2 Signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("Api2 user signup is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				dataSet.get("redeemable_id"), "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		pageObj.utils().logPass("Api2  send reward reedemable to user is successful");

		Response singleScanCodeResponse = pageObj.endpoints().singleScanCodeWithRedeemableID(dataSet.get("client"),
				dataSet.get("secret"), token, dataSet.get("redeemable_id"));
		Assert.assertEquals(singleScanCodeResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for the single scan code API");
		pageObj.utils().logPass("single Scan Code successful");

	}

	@Test(description = "OMM-T170 valid reward id", groups = { "regression", "dailyrun" })
	@Owner(name = "Ashwini Shetty")
	public void T170_singleScanRewardID() {
		// User register/signup using API2 Signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("Api2 user signup is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				dataSet.get("redeemable_id"), "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		pageObj.utils().logPass("Api2  send reward reedemable to user is successful");

		// fetch user offers/ reward_id using ap2 mobileOffers
		Response offerResponse = pageObj.endpoints().getUserOffers(token, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, offerResponse.getStatusCode(),
				"Status code 200 did not matched for api2 user offers");
		String reward_id = offerResponse.jsonPath().get("rewards[0].reward_id").toString();
		pageObj.utils().logit(reward_id);
		pageObj.utils().logPass("Api2 user fetch user offers is successful");

		Response singleScanCodeResponse = pageObj.endpoints().singleScanCodeWithRewardID(dataSet.get("client"),
				dataSet.get("secret"), token, reward_id);
		Assert.assertEquals(singleScanCodeResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for the single scan code API");
		pageObj.utils().logPass("single Scan Code successful");

	}

	@Test(description = "OMM-T1171 User is able to generate single scan token even if payment method is empty.", groups = {
			"regression", "dailyrun" })
	@Owner(name = "Ashwini Shetty")
	public void T1171_singleScanCodeEmptyPaymentMethod() {

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 1 user signup");
		String token = signUpResponse.jsonPath().get("auth_token.token").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api1 signup");
		pageObj.utils().logPass("API1 Signup is successful");

		Response singleScanCodeResponse = pageObj.endpoints().singleScanCodeNoPaymentMethod(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(singleScanCodeResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for the single scan code API");
		pageObj.utils().logPass("single Scan Code successful");

	}

	@Test(description = "OMM-T158 If user is deactivated and user tries to generate single scan token", groups = {
			"regression", "dailyrun" })
	@Owner(name = "Ashwini Shetty")
	public void singleScanCodeDeactivatedUser() {
		// SignupAPI
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 1 user signup");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api1 signup");
		pageObj.utils().logPass("Api1 user signup is successful");
		String token = signUpResponse.jsonPath().get("auth_token.token");

		// Deactivateuser
		Response deactivateResponse = pageObj.endpoints().DeactivateUserAPI(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(deactivateResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		// error in generating single scan token
		Response singleScanCodeResponse = pageObj.endpoints().singleScanCodeWithPaypalBA(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(singleScanCodeResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED);
		Assert.assertTrue(singleScanCodeResponse.jsonPath().get("error").toString()
				.contains("You need to sign in or sign up before continuing."));
		pageObj.utils().logPass("single Scan Code unsuccessful for deactivated user");
	}

	@Test(description = "OMM-T160 If user is deactivated and user tries to generate single scan token", groups = {
			"regression", "dailyrun" })
	@Owner(name = "Ashwini Shetty")
	public void singleScanCodeForBannedUser() {
		// SignupAPI
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 1 user signup");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api1 signup");
		pageObj.utils().logPass("Api1 user signup is successful");
		String token = signUpResponse.jsonPath().get("auth_token.token");
		String userID = signUpResponse.jsonPath().get("id").toString();

		Response banUserresponse = pageObj.endpoints().banUser(userID, dataSet.get("apiKey"));
		Assert.assertEquals(banUserresponse.getStatusCode(), 202,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Ban a User");
		pageObj.utils().logPass("PLATFORM FUNCTIONS API Ban a User is successful");

		// error in generating single scan token
		Response singleScanCodeResponse = pageObj.endpoints().singleScanCodeWithPaypalBA(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(singleScanCodeResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY);
		System.out.println(singleScanCodeResponse.prettyPrint());
		pageObj.utils().logPass("single Scan Code unsuccessful for banned user");

	}

	@Test(description = "SQ-T3137 Validate that user is able to generate single scan token of length based on 'Single scan code length' value entered in Cockpit>Guests>Misc Config. (Min-6, Max- 34)", groups = {
			"regression", "dailyrun" })
	@Owner(name = "Ashwini Shetty")
	public void SingleScanCodelength() throws InterruptedException {

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().navigateToTabs("Miscellaneous Config");
		pageObj.menupage().scanCodeLength(dataSet.get("length"));

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		// pageObj.apiUtils().verifyResponse(signUpResponse, "API 1 user signup");
		String token = signUpResponse.jsonPath().get("auth_token.token").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api1 signup");
		pageObj.utils().logPass("API1 Signup is successful");

		Response singleScanCodeResponse = pageObj.endpoints().singleScanCodeWithPaypalBA(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(singleScanCodeResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for the single scan code API");
		pageObj.utils().logPass("single Scan Code successful");
		// System.out.println(singleScanCodeResponse.prettyPrint());
		String code = singleScanCodeResponse.jsonPath().get("single_scan_code").toString();
		Assert.assertEquals(code.length(), Integer.parseInt(dataSet.get("length")),
				"Single scan code length does not match the configured length");
		pageObj.utils().logPass("single Scan Code successful" + code);

		// pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
//		pageObj.instanceDashboardPage().loginToInstance();
		// pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().navigateToTabs("Miscellaneous Config");
		pageObj.menupage().scanCodeLength(dataSet.get("length1"));

		Response singleScanCodeResponse1 = pageObj.endpoints().singleScanCodeWithPaypalBA(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(singleScanCodeResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for the single scan code API");
		String code1 = singleScanCodeResponse1.jsonPath().get("single_scan_code").toString();
		Assert.assertEquals(code1.length(), Integer.parseInt(dataSet.get("length1")),
				"Single scan code length does not match the configured length");
		pageObj.utils().logPass("single Scan Code successful" + code1);
		// System.out.println(singleScanCodeResponse1.prettyPrint());

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}
}
