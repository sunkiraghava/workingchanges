package com.punchh.server.LP1Test;

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

import com.punchh.server.annotations.Owner;
import com.punchh.server.apiConfig.ApiResponseJsonSchema;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class ApprovedCheckinTest {

	private static Logger logger = LogManager.getLogger(ApprovedCheckinTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String userEmail;
	private String sTCName;
	private String baseUrl;
	private String env, run = "ui";
	private static Map<String, String> dataSet;

	@BeforeClass(alwaysRun = true)
	public void openBrowser() {
		driver = new BrowserUtilities().launchBrowser();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		// Single login to instance
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
		// Move to All Businesses page
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
	}

//Author : Amit
	@SuppressWarnings("unused")
	@Test(description = "SQ-T2175 Verify Perform checkin on API2 api", groups = { "regression",
			"dailyrun" }, priority = 0)
	@Owner(name = "Amit Kumar")
	public void T2175_verifyPerformcheckinonAPI2api() throws InterruptedException {
		TestListeners.extentTest.get().info(sTCName + " ==>" + dataSet);
		// Navigate to instance
		//pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		//pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// set No Pending Checkin Strategy
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");
		pageObj.earningPage().selectPendingCheckinStrategy("No pending checkins", "0");
		TestListeners.extentTest.get().pass("checkin strategy updated successfully to create approved checkin");

		// User register/signup using API2 Signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), 200, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("Api2 user signup is successful");

		// generateBarcode
		pageObj.menupage().navigateToSubMenuItem("Support", "Test Barcodes");
		pageObj.instanceDashboardPage().generateBarcode(dataSet.get("location"));
		String barcode = pageObj.instanceDashboardPage().captureBarcode();
		logger.info(barcode);
		TestListeners.extentTest.get().pass("barcode generated successfully");

		// Create Loyalty Checkin by Barcode
		Response barcodeCheckinResponse = pageObj.endpoints().Api2LoyaltyCheckinBarCode(dataSet.get("client"),
				dataSet.get("secret"), token, barcode);
		Assert.assertEquals(barcodeCheckinResponse.getStatusCode(), 201,
				"Status code 201 did not matched for api2 Loyalty Checkin by bar code");
		boolean isApi2BarcodeCheckinSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2CreateLoyaltyCheckinSchema, barcodeCheckinResponse.asString());
		Assert.assertTrue(isApi2BarcodeCheckinSchemaValidated,
				"API v2 Create Loyalty Checkin by Barcode Schema Validation failed");
		TestListeners.extentTest.get().pass("api2 loyalty checkin using barcode  is done");
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		String approvadLoyalty = pageObj.guestTimelinePage().getApprovedLoyalty();

		String discountValuePosCheckin = pageObj.guestTimelinePage().getRecieptApprovedLoyaltyDetails();
		Assert.assertTrue(approvadLoyalty.contains(barcode),
				"loyalty checkin approved barcode did not matched or displayed on timeline");
		TestListeners.extentTest.get().pass("api2 loyalty checkin verified on user timeline");

	}

	@SuppressWarnings("unused")
	@Test(description = "SQ-T2240 Verify Perform checkin on APIV1 secure api", groups = { "regression",
			"dailyrun" }, priority = 1)
	@Owner(name = "Amit Kumar")
	public void T2240_verifyPerformcheckinonAPIV1secureapi() throws InterruptedException {

		// User register/signup using API1 Signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 1 user signup");
		String token = signUpResponse.jsonPath().get("auth_token.token").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), 200, "Status code 200 did not matched for api1 signup");
		TestListeners.extentTest.get().pass("Api1 user signup is successful");

		// Navigate to instance
		//pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		//pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// set No Pending Checkin Strategy
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");
		pageObj.earningPage().selectPendingCheckinStrategy("No pending checkins", "0");
		TestListeners.extentTest.get().pass("checkin strategy updated successfully to create approved checkin");

		// generateBarcode
		pageObj.menupage().navigateToSubMenuItem("Support", "Test Barcodes");
		pageObj.instanceDashboardPage().generateBarcode(dataSet.get("location"));
		String barcode = pageObj.instanceDashboardPage().captureBarcode();
		logger.info(barcode);
		TestListeners.extentTest.get().pass("barcode generated successfully");

		// Create Loyalty Checkin by Barcode
		Response barcodeCheckinResponse = pageObj.endpoints().Api1LoyaltyCheckinBarCode(dataSet.get("client"),
				dataSet.get("secret"), token, barcode);
		Assert.assertEquals(barcodeCheckinResponse.getStatusCode(), 201,
				"Status code 201 did not matched for api2 Loyalty Checkin by bar code");
		TestListeners.extentTest.get().pass("Api1 loyalty checkin is done");
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		String approvadLoyalty = pageObj.guestTimelinePage().getApprovedLoyalty();
		String discountValuePosCheckin = pageObj.guestTimelinePage().getRecieptApprovedLoyaltyDetails();
		Assert.assertTrue(approvadLoyalty.contains(barcode),
				"loyalty checkin approved barcode did not matched or displayed on timeline");
		TestListeners.extentTest.get().pass("loyalty checkin verified on user timeline");

	}

	// merged with T2170_verifyGuestTimelineInPointToCurrency
	@SuppressWarnings("unused")
//	@Test(description = "SQ-T2201 Verify POS Checkin performed on test Location", groups = { "regression",
//			"dailyrun" }, priority = 2)
	@Owner(name = "Amit Kumar")
	public void T2201_verifyPOSCheckinperformedontestLocation() throws InterruptedException {
		// User register/signup using POS Signup api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response resp = pageObj.endpoints().posSignUp(userEmail, dataSet.get("locationKey"));
		Assert.assertEquals(200, resp.getStatusCode(), "Status code 200 did not matched for pos signup api");

		// Pos api checkin
		String key = CreateDateTime.getTimeDateString();
		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T10:50:00+05:30";
		Response response = pageObj.endpoints().posCheckin(date, userEmail, key, txn, dataSet.get("locationKey"));
		Assert.assertEquals(200, response.getStatusCode(), "Status code 200 did not matched for post chekin api");

		// Navigate to instance
		//pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		//pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		String approvadLoyalty = pageObj.guestTimelinePage().getApprovedLoyalty();
		String discountValuePosCheckin = pageObj.guestTimelinePage().getRecieptApprovedLoyaltyDetails();
		Assert.assertTrue(approvadLoyalty.contains(key),
				"loyalty checkin approved barcode did not matched or displayed on timeline");

	}

	@Test(description = "SQ-T2226 Verify POS Console Checkin through valid details", groups = { "regression",
			"dailyrun" }, priority = 3)
	@Owner(name = "Amit Kumar")
	public void T2226_verifyPOSConsoleCheckinthroughvaliddetails() throws InterruptedException {

		// Navigate to instance
		//pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		//pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// check turn off checkin and redemption flag
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().navigateToTabs("Miscellaneous Config");
		pageObj.dashboardpage().checkUncheckFlagCockpitDashboard(dataSet.get("flagName1"), dataSet.get("checkBoxFlag"));
		pageObj.dashboardpage().checkUncheckFlagCockpitDashboard(dataSet.get("flagName2"), dataSet.get("checkBoxFlag"));
		pageObj.dashboardpage().updateCheckBox();

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().navigateToTabs("Guest Validation");
		pageObj.dashboardpage().checkUncheckFlagCockpitDashboard("Validate privacy policy", "uncheck");
		pageObj.dashboardpage().updateCheckBox();

		// User register/signup using POS Signup api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response resp = pageObj.endpoints().posSignUp(userEmail, dataSet.get("locationKey"));
		Assert.assertEquals(200, resp.getStatusCode(), "Status code 200 did not matched for pos signup api");
		TestListeners.extentTest.get().pass("user signup is done using pos signup api");

		// set No Pending Checkin Strategy
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");
		pageObj.earningPage().selectPendingCheckinStrategy("No pending checkins", "0");
		TestListeners.extentTest.get().pass("checkin strategy updated successfully to create approved checkin");

		// Redeem reward from pos console
		pageObj.menupage().clickDashboardMenu();
		pageObj.dashboardpage().clickPosConsoleBtn();
		pageObj.dashboardpage().searchUser(userEmail);
		pageObj.consolePage().setDiscountAmount(dataSet.get("amount"));
		// pageObj.consolePage().setRedemptionCode(reward_Code);
		String msg = pageObj.consolePage().processTransaction();
		Assert.assertTrue(msg.contains("Successfully awarded"), "success message did not displayed on pos console");
		TestListeners.extentTest.get().pass("checkin done from pos console");

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		String approvadLoyalty = pageObj.guestTimelinePage().getApprovedLoyalty();
		// String discountValuePosCheckin =
		// pageObj.guestTimelinePage().getRecieptApprovedLoyaltyDetails();
		Assert.assertTrue(approvadLoyalty.contains(""),
				"loyalty checkin approved barcode did not matched or displayed on timeline");
		TestListeners.extentTest.get().pass("Approved loyalty verified on user timeline");
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
