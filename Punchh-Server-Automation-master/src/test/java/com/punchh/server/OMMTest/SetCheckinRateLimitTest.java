package com.punchh.server.OMMTest;

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
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class SetCheckinRateLimitTest {
	private static Logger logger = LogManager.getLogger(SetCheckinRateLimitTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String sTCName;
	private String baseUrl;
	String blankString = "";
	private String env, run = "ui";
	private static Map<String, String> dataSet;
    private Utilities utils;

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {

		driver = new BrowserUtilities().launchBrowser();
		sTCName = method.getName();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run , env), sTCName);
		dataSet = pageObj.readData().readTestData;
		logger.info(sTCName + " ==>" + dataSet);
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run , env , "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
        utils = new Utilities(driver);
	}

    @Test(description = "SQ-T2393 Set Checkin Rate Limit by Channel -> Functionality",
        groups = {"regression", "dailyrun"})
	@Owner(name = "Hardik Bhardwaj")
	public void T2393_SetCheckinRateLimitByChannelFunctionality_PartOne() throws InterruptedException {
		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// navigate to cockpit -> earning
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");

		// check Enable checkin throttling based on receipt time? flag
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_validate_on_receipt_time", "check");
		pageObj.dashboardpage().updateCheckBox();

		// unckeck Set Checkin Rate Limit by Checkin Channel flag
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_checkin_channel_rate_limit", "uncheck");
		pageObj.dashboardpage().updateCheckBox();

		// set Global Checkin Rate Limit as 0 receipts within 4 hours
		pageObj.earningPage().setGlobalCheckinRateLimit("0", "4");

		// user signup
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("API2 Signup is successful");
        utils.longWaitInSeconds(5);

		// v2 checkin
		String barcode = Long.toString((long) (Math.random() * Math.pow(10, 10)));
		String transaction_no = Integer.toString(Utilities.getRandomNoFromRange(50000000, 100000000));
		String receipt_datetime = CreateDateTime.getCurrentDateTimeInUtc();
		Response v2CheckinResponse = pageObj.endpoints().v2Checkin(dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("locationkey"), userEmail, "10", barcode, dataSet.get("manuItem1"),
				dataSet.get("manuItem2"), receipt_datetime, transaction_no, "POS");
		Assert.assertEquals(v2CheckinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for v2 checkin API ");
		Assert.assertEquals(v2CheckinResponse.jsonPath().get("[0]"),
				"You are only allowed to scan 0 receipts within 4 hours of each other.");
		utils.logPass(
				"v2 checkin API with message You are only allowed to scan 0 receipts within 4 hours of each other is unsuccessful (expected)");

		// set Global Checkin Rate Limit as 1 receipts within 4 hours
		pageObj.earningPage().setGlobalCheckinRateLimit("1", "4");

		// user signup
		String userEmail1 = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse1 = pageObj.endpoints().Api2SignUp(userEmail1, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse1, "API 2 user signup");
		Assert.assertEquals(signUpResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("API2 Signup is successful");

		// v2 checkin
		Thread.sleep(5000);
		String barcode1 = Long.toString((long) (Math.random() * Math.pow(10, 10)));
		String transaction_no1 = Integer.toString(Utilities.getRandomNoFromRange(50000000, 100000000));
		String receipt_datetime1 = CreateDateTime.getCurrentDateTimeInUtc();
		Response v2CheckinResponse1 = pageObj.endpoints().v2Checkin(dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("locationkey"), userEmail1, "10", barcode1, dataSet.get("manuItem1"),
				dataSet.get("manuItem2"), receipt_datetime1, transaction_no1, "POS");
		Assert.assertEquals(v2CheckinResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for v2 checkin API ");
		utils.logPass(
				"v2 checkin API is successful for scenario Global Checkin Rate Limit as 1 receipts within 4 hours");

		// v2 checkin
		Thread.sleep(5000);
		String barcode2 = Long.toString((long) (Math.random() * Math.pow(10, 10)));
		String transaction_no2 = Integer.toString(Utilities.getRandomNoFromRange(50000000, 100000000));
		String receipt_datetime2 = CreateDateTime.getCurrentDateTimeInUtc();
		Response v2CheckinResponse2 = pageObj.endpoints().v2Checkin(dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("locationkey"), userEmail1, "10", barcode2, dataSet.get("manuItem1"),
				dataSet.get("manuItem2"), receipt_datetime2, transaction_no2, "POS");
		Assert.assertEquals(v2CheckinResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for v2 checkin API ");
		Assert.assertEquals(v2CheckinResponse2.jsonPath().get("[0]"),
				"You are only allowed to scan 1 receipts within 4 hours of each other.");
		utils.logPass(
				"v2 checkin API with message You are only allowed to scan 1 receipts within 4 hours of each other is unsuccessful (expected)");

		// ECRM checkin
		utils.longWaitInSeconds(6);
		String key = CreateDateTime.getTimeDateString();
		String txn_no = "123456" + CreateDateTime.getTimeDateString();
		Response response = pageObj.endpoints().ecrmPosCheckin(userEmail1, dataSet.get("locationkey"), key, txn_no);
		pageObj.apiUtils().verifyResponse(response, "ECRM checkin");
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "ECRM checkin failure");
		utils.logPass(
				"ECRM checkin is successful first time when Global Checkin Rate Limit is set as 1 receipts within 4 hours");

		String key1 = CreateDateTime.getTimeDateString();
		String txn_no1 = "123456" + CreateDateTime.getTimeDateString();
		Response response1 = pageObj.endpoints().ecrmPosCheckin(userEmail1, dataSet.get("locationkey"), key1, txn_no1);
		pageObj.apiUtils().verifyResponse(response1, "ECRM checkin");
		Assert.assertEquals(response1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "ECRM checkin failure");
		utils.logPass(
				"ECRM checkin is successful second time when Global Checkin Rate Limit is set as 1 receipts within 4 hours");
		Thread.sleep(5000);

		// navigate to user time line
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail1);
		Assert.assertTrue(pageObj.guestTimelinePage().verifyEcrmTransaction(key1, txn_no1),
				"Failed to verify ECRM checkin on guest timeline");
	}

    @Test(description = "SQ-T2393 Set Checkin Rate Limit by Channel -> Functionality",
        dependsOnMethods = "T2393_SetCheckinRateLimitByChannelFunctionality_PartOne",
        groups = {"regression", "dailyrun"})
	@Owner(name = "Hardik Bhardwaj")
	public void T2393_SetCheckinRateLimitByChannelFunctionality_PartTwo() throws InterruptedException {
		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// navigate to cockpit -> earning
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");

		// unckeck Set Checkin Rate Limit by Checkin Channel flag
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_checkin_channel_rate_limit", "check");
		pageObj.dashboardpage().updateCheckBox();

		// set Checkin Rate Limit by Channel as 0 for all
		pageObj.earningPage().setCheckinRateLimitByChannel("Online Order Checkin Rate Limit", "0");
		pageObj.earningPage().setCheckinRateLimitByChannel("POS Checkin Rate Limit", "0");
		pageObj.earningPage().setCheckinRateLimitByChannel("Mobile Checkin Rate Limit", "0");
//		pageObj.earningPage().setCheckinRateLimitByChannel("Chatbot Checkin Rate Limit", "0");
//		pageObj.earningPage().setCheckinRateLimitByChannel("Dashboard Checkin Rate Limit", "0");
		pageObj.earningPage().setCheckinRateLimitByChannel("Web Checkin Rate Limit", "0");
		pageObj.earningPage().setCheckinRateLimitByChannel("Kiosk Checkin Rate Limit", "0");
		pageObj.earningPage().updateConfiguration();
		pageObj.earningPage().updateConfiguration();

		// set Checkin Rate Limit by Channel as 2 for Online Order Checkin Rate Limit
		pageObj.earningPage().setCheckinRateLimitByChannel("Online Order Checkin Rate Limit", "2");

		// set Checkin Rate Limit by Channel as 1 for POS Checkin Rate Limit
		pageObj.earningPage().setCheckinRateLimitByChannel("POS Checkin Rate Limit", "1");
		pageObj.earningPage().updateConfiguration();

		// user signup
		String userEmail2 = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse2 = pageObj.endpoints().Api2SignUp(userEmail2, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse2, "API 2 user signup");
		Assert.assertEquals(signUpResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("API2 Signup is successful");

		// v2 checkin with channel Dashboard
		String barcode3 = Long.toString((long) (Math.random() * Math.pow(10, 10)));
		String transaction_no3 = Integer.toString(Utilities.getRandomNoFromRange(50000000, 100000000));
		String receipt_datetime3 = CreateDateTime.getCurrentDateTimeInUtc();
		Thread.sleep(5000);
		Response v2CheckinResponse3 = pageObj.endpoints().v2Checkin(dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("locationkey"), userEmail2, "10", barcode3, dataSet.get("manuItem1"),
				dataSet.get("manuItem2"), receipt_datetime3, transaction_no3, "Dashboard");
		Assert.assertEquals(v2CheckinResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for v2 checkin API ");
		Assert.assertEquals(v2CheckinResponse3.jsonPath().get("[0]"),
				"You are only allowed to scan 0 receipts within 4 hours of each other.");
		utils.logPass(
				"v2 checkin API with message You are only allowed to scan 0 receipts within 4 hours of each other is unsuccessful (expected)");

		// v2 checkin with channel POS
		String barcode4 = Long.toString((long) (Math.random() * Math.pow(10, 10)));
		String transaction_no4 = Integer.toString(Utilities.getRandomNoFromRange(50000000, 100000000));
		String receipt_datetime4 = CreateDateTime.getCurrentDateTimeInUtc();
		Response v2CheckinResponse4 = pageObj.endpoints().v2Checkin(dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("locationkey"), userEmail2, "10", barcode4, dataSet.get("manuItem1"),
				dataSet.get("manuItem2"), receipt_datetime4, transaction_no4, "POS");
		Assert.assertEquals(v2CheckinResponse4.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for v2 checkin API ");
		utils.logPass(
				"v2 checkin API is successful for scenario Global Checkin Rate Limit as 1 receipts within 4 hours");

		// v2 checkin with channel POS
		String barcode5 = Long.toString((long) (Math.random() * Math.pow(10, 10)));
		String transaction_no5 = Integer.toString(Utilities.getRandomNoFromRange(50000000, 100000000));
		String receipt_datetime5 = CreateDateTime.getCurrentDateTimeInUtc();
		Response v2CheckinResponse5 = pageObj.endpoints().v2Checkin(dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("locationkey"), userEmail2, "10", barcode5, dataSet.get("manuItem1"),
				dataSet.get("manuItem2"), receipt_datetime5, transaction_no5, "POS");
		Assert.assertEquals(v2CheckinResponse5.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for v2 checkin API ");
		Assert.assertEquals(v2CheckinResponse5.jsonPath().get("[0]"),
				"You are only allowed to scan 1 receipts within 4 hours of each other.");
		utils.logPass(
				"v2 checkin API with message You are only allowed to scan 1 receipts within 4 hours of each other (POS checkin limit is 1) is unsuccessful (expected)");

		// set Checkin Rate Limit by Channel as 0 for Online Order Checkin Rate Limit
		pageObj.earningPage().setCheckinRateLimitByChannel("Online Order Checkin Rate Limit", "0");

		// set Checkin Rate Limit by Channel as 1 for POS Checkin Rate Limit
		pageObj.earningPage().setCheckinRateLimitByChannel("POS Checkin Rate Limit", "1");
		pageObj.earningPage().updateConfiguration();

		// ECRM checkin
		String key2 = CreateDateTime.getTimeDateString();
		String txn_no2 = "123456" + CreateDateTime.getTimeDateString();
		Response response2 = pageObj.endpoints().ecrmPosCheckin(userEmail2, dataSet.get("locationkey"), key2, txn_no2);
		pageObj.apiUtils().verifyResponse(response2, "ECRM checkin");
		Assert.assertEquals(response2.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "ECRM checkin failure");
		utils.logPass(
				"ECRM checkin is successful first time when Checkin Rate Limit by Channel is set as 1 for POS Checkin Rate Limit");

		String key3 = CreateDateTime.getTimeDateString();
		String txn_no3 = "123456" + CreateDateTime.getTimeDateString();
		Response response3 = pageObj.endpoints().ecrmPosCheckin(userEmail2, dataSet.get("locationkey"), key3, txn_no3);
		pageObj.apiUtils().verifyResponse(response3, "ECRM checkin");
		Assert.assertEquals(response3.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "ECRM checkin failure");
		Assert.assertEquals(response2.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "ECRM checkin failure");
		utils.logPass(
				"ECRM checkin is successful second time when Checkin Rate Limit by Channel is set as 1 for POS Checkin Rate Limit");
		Thread.sleep(5000);

		// navigate to user time line
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail2);
		Assert.assertTrue(pageObj.guestTimelinePage().verifyEcrmTransaction(key3, txn_no3),
				"Failed to verify ECRM checkin on guest timeline");

		// navigate to cockpit -> earning
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");

		// unckeck Set Checkin Rate Limit by Checkin Channel flag
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_checkin_channel_rate_limit", "uncheck");
		pageObj.dashboardpage().updateCheckBox();

		// uncheck Enable checkin throttling based on receipt time? flag
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_validate_on_receipt_time", "uncheck");
		pageObj.dashboardpage().updateCheckBox();

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}

}
