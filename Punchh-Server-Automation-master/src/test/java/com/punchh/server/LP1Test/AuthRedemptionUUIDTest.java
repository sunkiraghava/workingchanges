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
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class AuthRedemptionUUIDTest {
	static Logger logger = LogManager.getLogger(AuthRedemptionUUIDTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String sTCName;
	private String baseUrl;
	private String run = "ui";
	private String env;
	private static Map<String, String> dataSet;
	String userEmail;

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

	@Test(description = "LPE-1231 - SQ-T6445 Verify the UUID Redemption Code Generation feature for existing businesses with flag enabled")
	@Owner(name = "Neha Lodha")
	public void SQT6445_FlagEnabled() throws InterruptedException {
		//pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		//pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsCodePage().clickedOnGenerateUUIDforOLO("check");

		// User Sign-up
		TestListeners.extentTest.get().info("== Auth API User Sign-up ==");
		logger.info("== Auth API User Sign-up ==");
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), 201,
				"Status code 201 did not matched for auth user signup api");
		String authToken = signUpResponse.jsonPath().get("authentication_token");
		TestListeners.extentTest.get().pass("Auth API User Sign-up call is successful");
		logger.info("Auth API User Sign-up call is successful");

		// Checkin via auth API
		TestListeners.extentTest.get().info("== Auth API Checkin ==");
		logger.info("== Auth API Checkin ==");
		String amount = "210.0";
		Response checkinResponse = pageObj.endpoints().onlineOrderCheckin(authToken, amount, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(checkinResponse.getStatusCode(), 200);
		TestListeners.extentTest.get().pass("Auth API Checkin call is successful");
		logger.info("Auth API Checkin call is successful");

		// Create Online Redemption
		TestListeners.extentTest.get().info("== Auth API Create Online Redemption ==");
		logger.info("== Auth API Create Online Redemption ==");
		Response resp = pageObj.endpoints().authOnlineBankCurrencyRedemption(authToken, dataSet.get("client"),
				dataSet.get("secret"));

		Assert.assertEquals(resp.getStatusCode(), 200);
		boolean isAuthOnlineRedemptionSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.authOnlineRedemptionSchema, resp.asString());
		Assert.assertTrue(isAuthOnlineRedemptionSchemaValidated,
				"Auth API Create Online Redemption Schema Validation failed");
		Assert.assertTrue(resp.jsonPath().get("status").toString().contains("Please HONOR it."));
		TestListeners.extentTest.get().pass("Auth API Create Online Redemption call is successful");
		logger.info("Auth API Create Online Redemption call is successful");
		String redemption_code = resp.jsonPath().get("redemption_code");
		Assert.assertTrue(redemption_code.length() > 8, "Redemption code should be more than 8 digits");
	}

	@Test(description = "LPE-1231 - SQ-T6445 Verify the UUID Redemption Code Generation feature for existing businesses with flag disabled")
	@Owner(name = "Neha Lodha")
	public void SQT6446_FlagDisabled() throws InterruptedException {
		//pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		//pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsCodePage().clickedOnGenerateUUIDforOLO("uncheck");

		// User Sign-up
		TestListeners.extentTest.get().info("== Auth API User Sign-up ==");
		logger.info("== Auth API User Sign-up ==");
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), 201,
				"Status code 201 did not matched for auth user signup api");
		String authToken = signUpResponse.jsonPath().get("authentication_token");
		TestListeners.extentTest.get().pass("Auth API User Sign-up call is successful");
		logger.info("Auth API User Sign-up call is successful");

		// Checkin via auth API
		TestListeners.extentTest.get().info("== Auth API Checkin ==");
		logger.info("== Auth API Checkin ==");
		String amount = "210.0";
		Response checkinResponse = pageObj.endpoints().onlineOrderCheckin(authToken, amount, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(checkinResponse.getStatusCode(), 200);
		TestListeners.extentTest.get().pass("Auth API Checkin call is successful");
		logger.info("Auth API Checkin call is successful");

		// Create Online Redemption
		TestListeners.extentTest.get().info("== Auth API Create Online Redemption ==");
		logger.info("== Auth API Create Online Redemption ==");
		Response resp = pageObj.endpoints().authOnlineBankCurrencyRedemption(authToken, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(resp.getStatusCode(), 200);
		boolean isAuthOnlineRedemptionSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.authOnlineRedemptionSchema, resp.asString());
		Assert.assertTrue(isAuthOnlineRedemptionSchemaValidated,
				"Auth API Create Online Redemption Schema Validation failed");
		Assert.assertTrue(resp.jsonPath().get("status").toString().contains("Please HONOR it."));
		TestListeners.extentTest.get().pass("Auth API Create Online Redemption call is successful");
		logger.info("Auth API Create Online Redemption call is successful");
		String redemption_code = resp.jsonPath().get("redemption_code");
		Assert.assertTrue(redemption_code.length() <= 8, "Redemption code should be less than or equal to 8 digits");

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
