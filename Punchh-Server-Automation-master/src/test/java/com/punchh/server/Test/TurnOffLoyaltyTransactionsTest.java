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

import com.punchh.server.annotations.Owner;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.TestListeners;

@Listeners(TestListeners.class)
public class TurnOffLoyaltyTransactionsTest {

	static Logger logger = LogManager.getLogger(TurnOffLoyaltyTransactionsTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	@SuppressWarnings("unused")
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
		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
	}

	@Test(description = "LPE-T1441 Verify iFrame Barcode Checkin >> Check-in through valid Barcode", priority = 1, groups = {
			"dailyrun" })
	@Owner(name = "Hardik Bhardwaj")
	public void T1440_TurnOffCheckinsAndTurnOffRedemptionsIframeCheck() throws InterruptedException {

		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.menupage().miscellaneousConfigInCockpit();
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_turn_off_checkins", "check");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_turn_off_redemptions", "check");
		pageObj.dashboardpage().updateCheckBox();

		pageObj.menupage().navigateToSubMenuItem("Support", "Test Barcodes");
		// generateBarcode
		pageObj.instanceDashboardPage().generateBarcode(dataSet.get("location"));
		String barcode = pageObj.instanceDashboardPage().captureBarcode();
		// iframeCheckin
		pageObj.iframeSingUpPage().navigateToIframe(baseUrl + dataSet.get("whiteLabel") + dataSet.get("slug"));
		userEmail = pageObj.iframeSingUpPage().iframeSignUp();
		String message = pageObj.iframeSingUpPage().getIframeMessage(barcode);
		Assert.assertEquals(message,
				"Loyalty Program Maintenance is currently in Progress. Please try again after some time.",
				"Checkin via Iframe is not giving expected error");
		logger.info("Checkin via Iframe is giving expected error");
		TestListeners.extentTest.get().pass("Checkin via Iframe is giving expected error");
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