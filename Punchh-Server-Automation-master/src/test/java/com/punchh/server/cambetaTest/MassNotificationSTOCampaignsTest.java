package com.punchh.server.cambetaTest;

import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.List;
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
import org.testng.asserts.SoftAssert;

import com.punchh.server.annotations.Owner;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class MassNotificationSTOCampaignsTest {

	private static Logger logger = LogManager.getLogger(MassNotificationSTOCampaignsTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String sTCName;
	private String env;
	private String baseUrl;
	private static Map<String, String> dataSet;
	private String campaignName;
	String run = "ui";
	Utilities utils;

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
		utils = new Utilities(driver);
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

	@Test(description = "SQ-T2663 Mass Notification STO Once Frequency", groups = { "regression", "unstable",
			"dailyrun" }, priority = 0)
	@Owner(name = "Amit Kumar")
	public void T2663_verifySTOCampaignWithFrequencyOnceSelected() throws InterruptedException, ParseException {

		campaignName = "Automation MassNotification Campaign" + CreateDateTime.getTimeDateString();

		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Click Cockpit and enable STO campaign
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().onEnableSTOCheckbox();

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value
		pageObj.campaignspage().selectMessagedrpValue("Mass Notification");

		// validate error msg for blank name field
		pageObj.campaignspage().clickNewCampaignBetaBtn();
		pageObj.campaignsbetaPage().SetCampaignName(campaignName);
		Thread.sleep(2000);
		pageObj.campaignsbetaPage().clickContinueBtn();
		Thread.sleep(5000);
		utils.switchToWindow();

		// Set segment
		pageObj.campaignsbetaPage().clickSegmentBtn();
		pageObj.campaignsbetaPage().setSegmentType("custom");
		Thread.sleep(2000);
		pageObj.campaignsbetaPage().clickNextBtn();
		pageObj.campaignsbetaPage().setEmailNotification("Test Title", "Test Body");
		pageObj.campaignsbetaPage().clickNextBtn();

		pageObj.campaignsbetaPage().setStartDateNew(0);

		pageObj.campaignsbetaPage().setRecommendedSendDate(); // STO
		pageObj.campaignsbetaPage().setTimeZone("Recipients’ Local Time Zone");
		pageObj.campaignsbetaPage().clickNextBtn();
		List<String> summaryData = pageObj.campaignsbetaPage().CheckSummary();
		SoftAssert softassert = new SoftAssert();
		softassert.assertTrue(summaryData.get(0).contains("Audience\n" + "custom"),
				"Audience type did not matched on summary page");
		softassert.assertTrue(summaryData.get(1).contains("Segment size"),
				"Segment size did not displayed on summary page");
		softassert.assertTrue(summaryData.get(4).contains("Frequency\n" + "Once"),
				"Frequencydid not matched  on summary page");
		softassert.assertTrue(summaryData.get(5).contains("RECOMMENDED SEND TIME"),
				"Recommended Time not matched  on summary page");
		softassert.assertAll();
		TestListeners.extentTest.get().pass("Campaign summary validated");
		pageObj.campaignsbetaPage().clickActivateCampaignBtn();
		String msg = pageObj.campaignsbetaPage().validateSuccessMessage();
		Assert.assertEquals(msg, "Schedule created successfully.", "Success message text did not matched");
		TestListeners.extentTest.get().pass("Campaign created with success message");

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() throws Exception {
		pageObj.utils().deleteCampaignFromDb(campaignName, env);
		pageObj.utils().clearDataSet(dataSet);
		logger.info("Data set cleared");
	}

	@AfterClass(alwaysRun = true)
	public void closeBrowser() {
		driver.quit();
		logger.info("Browser closed");
	}

}
