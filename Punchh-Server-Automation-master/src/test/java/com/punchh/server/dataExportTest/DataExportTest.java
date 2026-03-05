package com.punchh.server.dataExportTest;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class DataExportTest {
	static Logger logger = LogManager.getLogger(DataExportTest.class);
	public WebDriver driver;
	private Properties prop;
	private PageObj pageObj;
	private String baseUrl;
	private static Map<String, String> dataSet;
	private String env, run = "ui";
	private String sTCName;
	Utilities utils;

	@BeforeMethod(alwaysRun = true)
	public void beforeClass(Method method) {
		prop = Utilities.loadPropertiesFile("config.properties");
		sTCName = method.getName();
		driver = new BrowserUtilities().launchBrowser();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		utils = new Utilities(driver);

	}

	@Test(description = "SQ-T2529	Run Datalake exports for all 17 exports and verify the logs in UI || "
			+ "SQ-T2530 Run SQL exports for all 14 data exports and verify logs in UI || "
			+ "SQ-T5346 Run All Data Export with all field selected on Point to Currency Business (Objective-2)", groups = "Regression")
	public void verifyDataExport() throws InterruptedException {
		pageObj.sidekiqPage().sidekiqCheck(baseUrl);

		HashMap<String, ArrayList<String>> fieldMap = new HashMap<>();

		ArrayList<String> fieldList = new ArrayList<String>();

		List<String> exportNames = Arrays.asList("Migrated Guest Data", "Guests Awaiting Migration Data",
				"Campaign Data", "Coupon Data", "Redeemable Data", "Coupon Redemption Data", "User Subscription Data",
				"Subscription Discount Data", "Subscription Credit Data", "Subscription Debit Data", "Redemption Data",
				"User Feedback Data", "Checkin Data", "Checkin Failure Data", "Referral Data", "Receipt Item Data",
				"Tipping Data", "Locations Data", "Guest Data", "Reward Data", "Archive Rewards Data", "Gift Card Data",
				"Payment Data");

		List<String> logList = Arrays.asList("redemption", "user_feedback", "checkin", "checkin_failure", "referral",
				"receipt_item", "tipping", "locations", "guest", "reward", "archive_rewards", "gift_card",
				"migrated_guest", "guests_awaiting_migration", "campaign", "coupon", "redeemable", "payment",
				"coupon_redemption", "user_subscription", "subscription_discount", "subscription_credit",
				"subscription_debit");

		logger.info("== Data export validation test ==");
		String exportName = CreateDateTime.getUniqueString("AutoDataExport");
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		// pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// pageObj.menupage().clickCockPitMenu();
		// pageObj.menupage().clickCockpitDashboardLink();
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().onEnabledataExportBetabox();
		// pageObj.menupage().clickCockpitGuest();
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().enableGuestMigrationMgmt();
		pageObj.dataExportPage().goToDataExport();
		pageObj.dataExportPage().setDataExportNameAndDate(exportName);
		for (String str : exportNames) {
			fieldList = pageObj.dataExportPage().clickDataExportNameCheckBox(str);
			String a = str.replace(" Data", "");
			String b = (a.replace(" ", "_")).toLowerCase();
			fieldMap.put(b, fieldList);
		}
		pageObj.dataExportPage().clickOnSaveExportButton();
		pageObj.schedulePage().scheduleNewEmailExport("AutoExport");
		ArrayList<String> fileName = pageObj.schedulePage()
				.verifyExportScheduleForAll(prop.getProperty("dataExportSchedule"), exportName, env, logList, fieldMap);
		Assert.assertEquals(fileName.size(), exportNames.size(), "Count don't match");
		logger.info("Successfully verified export file count: " + fileName.size());
		TestListeners.extentTest.get().pass("Successfully verified export file count: " + fileName.size());
		// Delete Data Export
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulePage().openSchedule(prop.getProperty("dataExportSchedule"), exportName);
		pageObj.schedulePage().selectDeleteOrDeactivateOptionDataExport(exportName, "Delete");
		utils.acceptAlert(driver);
		String message = utils.getSuccessMessage();
		Assert.assertEquals(message, "Schedule deleted successfully.", "Message did not match.");
		logger.info(exportName + " Data Export Export deleted Successfully");
		TestListeners.extentTest.get().pass(exportName + " Data Export Export deleted Successfully");
		for (String fname : fileName) {
			utils.deleteExistingDownload(fname);
		}
	}

	@Test(description = "SQ-T6760 Validate the disclaimer under the guest awaiting migration under data export")
	public void T6760_verifyDisclaimerInGuestAwaitingMigration() throws InterruptedException {
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.dataExportPage().goToDataExport();

		String actual = pageObj.dataExportPage().getDisclaimerTextInGuestAwaitingMigration();
		String expected = "Note: Date range filter and location filter will not be applicable on Guests Awaiting Migration Data.";

		Assert.assertEquals(actual, expected, "Disclaimer text does not match");
	}

	@AfterMethod(alwaysRun = true)
	public void afterClass() {
		driver.quit();
		logger.info("Browser closed");
	}
}
