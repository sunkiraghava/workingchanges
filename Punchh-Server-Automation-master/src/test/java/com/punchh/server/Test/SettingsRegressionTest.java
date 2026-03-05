package com.punchh.server.Test;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.punchh.server.annotations.Owner;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.TestListeners;

@Listeners(TestListeners.class)
public class SettingsRegressionTest {

	private static Logger logger = LogManager.getLogger(SettingsRegressionTest.class);

	public WebDriver driver;
	private PageObj pageObj;
	private String sTCName;
	private String baseUrl;
	String blankString = "";
	private String env, run = "ui";
	private static Map<String, String> dataSet;
	String redeemableName = "DistRedeemable";

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
		logger.info(sTCName + " ==>" + dataSet);
		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
	}

	@Test(groups = { "sanity",
			"dailyrun" }, description = "SQ-T2597 (1.0) Validate that on clicking on the Audit log within a location page shows audit log for that location.")
	@Owner(name = "Amit Kumar")
	public void T2597_verifyAuditLogLocationPageTest() throws InterruptedException {
		logger.info(
				"Validate that on clicking on the Audit log within a location page shows audit log for that location");
		// login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// pageObj.menupage().clickSettingsMenu();
		// pageObj.menupage().clickLocationsLink();
		pageObj.menupage().navigateToSubMenuItem("Settings", "Locations");
		pageObj.locationPage().verifyToAuditLogNavigation(dataSet.get("expHeaderText"));
	}

	@Test(groups = { "sanity",
			"dailyrun" }, description = "SQ-T2354 (1.0) Distributable Redeemables -> Schedule Feature (Validations and Functionality)")
	@Owner(name = "Amit Kumar")
	public void T2354_verifyDistributableScheduleRedeemableTest() throws InterruptedException {
		redeemableName += CreateDateTime.getTimeDateString();
		// login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Settings", "Redeemables");
		pageObj.redeemablePage().createRedeemable(redeemableName);
		pageObj.redeemablePage().enableDistributable(dataSet.get("segmentName"));
		pageObj.redeemablePage().selectRecieptRule("1");
		pageObj.redeemablePage().verifyDistributableRedeemableDateErrors();
		pageObj.redeemablePage().verifyDealWithFutureStartDate();
		pageObj.instanceDashboardPage().navigateToGuestTimeline(dataSet.get("exisitngUserEmail"));

		// verifying scheduled redeemable not appearing under guest timeline gift
		// redeemable dropdown
		pageObj.guestTimelinePage().verifyRedeemableWithFutureStartDate(redeemableName);

		// verifying scheduled redeemable not appearing on singup campaign
		String signUpCampaignName = CreateDateTime.getUniqueString("Automation SignUpCampaign");
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.campaignspage().selectOngoingdrpValue("Signup");
		pageObj.campaignspage().clickNewCampaignBtn();
		String giftRedeemable = "Gift Redeemable";
		pageObj.signupcampaignpage().verifyScheduledRedeemableInSignupCampaign(signUpCampaignName, giftRedeemable);
		pageObj.menupage().navigateToSubMenuItem("Settings", "Redeemables");

		pageObj.redeemablePage().searchRedeemable(redeemableName);
		pageObj.redeemablePage().verifyRedeemablColor(redeemableName);
		pageObj.redeemablePage().deleteRedeemable(redeemableName);
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
