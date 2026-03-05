package com.punchh.server.Test;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.TestListeners;

import io.restassured.response.Response;

@Listeners(TestListeners.class)

public class BarcodeValidationTest {
	static Logger logger = LogManager.getLogger(BarcodeValidationTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String userEmail;
	private String sTCName;
	private String baseUrl;
	String blankString = "";
	private String env, run = "ui";
	private static Map<String, String> dataSet;

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
		// Instance move to all business page
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
	}

	@Test(description = "SQ-T4198 Verify the receipt should be shown green and punchh key should be updated in DB checkins table for gift type checkin with barcode", groups = {
			"regression", "dailyrun" }, priority = 0)
	public void T4198_VerifyBarcodeReceiptColor() throws Exception {
		// User register/signup using API2 Signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("Api2 user signup is successful");
		String userId = signUpResponse.jsonPath().get("user.user_id").toString();

		// Navigate to business
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */

		// Instance select business

		// pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// generateBarcode
		pageObj.menupage().navigateToSubMenuItem("Support", "Test Barcodes");
		pageObj.instanceDashboardPage().generateBarcode(dataSet.get("location"));
		String barcode = pageObj.instanceDashboardPage().captureBarcode();

		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().messageReceiptBarcode(dataSet.get("reply"), barcode, dataSet.get("location"),
				dataSet.get("option"), dataSet.get("giftTypes"), dataSet.get("giftReason"));
		pageObj.menupage().navigateToSubMenuItem("Support", "Barcode Lookup");
		pageObj.barcodelookup().selectLocation("Any");
		pageObj.barcodelookup().enterBarcode(barcode);
		pageObj.barcodelookup().clickOnSubmitButton();
		boolean isGreenColourBGVisible = pageObj.barcodelookup().verifyTheColour(userId, "green-background");
		Assert.assertTrue(isGreenColourBGVisible, " Expected Green colour background NOT matched");
	}

	@Test(description = "SQ-T3055 Negative Scenarios Of Auth Api Create Loyalty Checkin", groups = { "api",
			"dailyrun" }, priority = 1)
	public void T3055_verifyNegativeScenariosOfAuthApiCreateLoyaltyCheckin() throws InterruptedException {
		// User SignUp from Auth Api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");
		String authToken = signUpResponse.jsonPath().get("authentication_token");

		// Generate Barcode
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(dataSet.get(
		 * "instanceUrl")); pageObj.instanceDashboardPage().loginToInstance();
		 */

		// Instance select business

		// pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// generateBarcode
		pageObj.menupage().navigateToSubMenuItem("Support", "Test Barcodes");
		pageObj.instanceDashboardPage().generateBarcode(dataSet.get("location"));
		String barcode = pageObj.instanceDashboardPage().captureBarcode();
		logger.info(barcode);

		// Create Loyalty Checkin with invalid Authentication Token
		String authToken1 = CreateDateTime.getTimeDateString();
		Response createLoyaltyCheckinResponse = pageObj.endpoints().authApiCreateLoyaltyCheckin(authToken1,
				dataSet.get("client"), dataSet.get("secret"), dataSet.get("store_num"), barcode);
		Assert.assertEquals(createLoyaltyCheckinResponse.jsonPath().getString("error"),
				"You need to sign in or sign up before continuing.");
		Assert.assertEquals(createLoyaltyCheckinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for auth Api create Loyalty Checkin with invalid Authentication Token");
		TestListeners.extentTest.get()
				.pass("auth Api create Loyalty Checkin with invalid Authentication Token is unsuccessful (expected)");

		// Create Loyalty Checkin with invalid client
		Response createLoyaltyCheckinResponse1 = pageObj.endpoints().authApiCreateLoyaltyCheckin(authToken,
				dataSet.get("invalidClient"), dataSet.get("secret"), dataSet.get("store_num"), barcode);
		Assert.assertEquals(createLoyaltyCheckinResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not matched for auth Api create Loyalty Checkin with invalid client");
		Assert.assertEquals(createLoyaltyCheckinResponse1.jsonPath().get("[0]"), "Invalid Signature");
		TestListeners.extentTest.get()
				.pass("auth Api create Loyalty Checkin with invalid client is unsuccessful (expected)");

		// Create Loyalty Checkin with invalid secret
		Response createLoyaltyCheckinResponse2 = pageObj.endpoints().authApiCreateLoyaltyCheckin(authToken,
				dataSet.get("client"), dataSet.get("invalidSecret"), dataSet.get("store_num"), barcode);
		Assert.assertEquals(createLoyaltyCheckinResponse2.jsonPath().get("[0]"), "Invalid Signature");
		Assert.assertEquals(createLoyaltyCheckinResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED,
				"Status code 412 did not matched for auth Api create Loyalty Checkin with invalid secret");
		TestListeners.extentTest.get()
				.pass("auth Api create Loyalty Checkin with invalid secret is unsuccessful (expected)");

		// Create Loyalty Checkin with invalid Barcode
		String invalidBarcode = CreateDateTime.getTimeDateString() + "a";
		Response createLoyaltyCheckinResponse4 = pageObj.endpoints().authApiCreateLoyaltyCheckin(authToken,
				dataSet.get("client"), dataSet.get("secret"), dataSet.get("store_num"), invalidBarcode);
		Assert.assertEquals(createLoyaltyCheckinResponse4.getStatusCode(), 416,
				"Status code 416 did not matched for auth Api create Loyalty Checkin with invalid store_num");
		TestListeners.extentTest.get()
				.pass("auth Api create Loyalty Checkin with invalid store_num is unsuccessful (expected)");
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
