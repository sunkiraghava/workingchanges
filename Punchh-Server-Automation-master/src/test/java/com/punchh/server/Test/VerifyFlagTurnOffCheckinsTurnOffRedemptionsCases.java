package com.punchh.server.Test;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Properties;
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
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.TestListeners;

import io.restassured.response.Response;

//shaleen

@Listeners(TestListeners.class)

public class VerifyFlagTurnOffCheckinsTurnOffRedemptionsCases {
	private static Logger logger = LogManager.getLogger(VerifyFlagTurnOffCheckinsTurnOffRedemptionsCases.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String userEmail;
	private String sTCName;
	private String baseUrl;
	String blankString = "";
	private String env, run = "ui";
	private static Map<String, String> dataSet;
	Properties prop;

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
		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
	}

	@Test(description = "SQ-T5052 (1.0) -- Verify flag i.e. Turn off Checkins is enabled user should not be able to checking via API V1 ")
	@Owner(name = "Shaleen Gupta")
	public void T5052_verifyFlagTurnOffCheckinsUserUnableToCheckinViaAPIV1() throws InterruptedException {

		// sign-up user
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), 200, "Status code 200 did not matched for api1 signup");
		logger.info("Api1 user signup is successful");
		TestListeners.extentTest.get().pass("Api1 user signup is successful");
		String access_token = signUpResponse.jsonPath().get("auth_token.token");

		// open business
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// click turn off checkins flag
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().navigateToTabs("Miscellaneous Config");
		pageObj.dashboardpage().checkUncheckFlagCockpitDashboard(dataSet.get("flagName"),
				dataSet.get("checkBoxFlagCheck"));

		// generateBarcode
		pageObj.menupage().navigateToSubMenuItem("Support", "Test Barcodes");
		pageObj.instanceDashboardPage().generateBarcode(dataSet.get("locationkey"));
		String barcode = pageObj.instanceDashboardPage().captureBarcode();

		// checkin via APIV1
		Response checkinRes = pageObj.endpoints().Api1LoyaltyCheckinBarCode(dataSet.get("client"),
				dataSet.get("secret"), access_token, barcode);
		Assert.assertEquals(checkinRes.statusCode(), 422, "Verification of flag i.e. Turn off checkins is failed ");
		logger.info("Verified user is not able to checkin via APIV1");
		TestListeners.extentTest.get().pass("Verified user is not able to checkin via APIV1");
		String response_val = checkinRes.jsonPath().get("[0]").toString();

		Assert.assertEquals(response_val, dataSet.get("expectedMsg"), "expected msg is not equal");
		logger.info("Verified user not able to checkin via APIV1");
		TestListeners.extentTest.get().pass("Verified user not able to checkin via APIV1");

		// open business
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 * pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		 */

		// click turn off checkins flag
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().navigateToTabs("Miscellaneous Config");
		pageObj.dashboardpage().checkUncheckFlagCockpitDashboard(dataSet.get("flagName"),
				dataSet.get("checkBoxFlagUnCheck"));

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
