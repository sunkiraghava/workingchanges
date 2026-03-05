package com.punchh.server.cambetaTest;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import org.testng.Assert;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.Listeners;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;

import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;
import java.lang.reflect.Method;

@Listeners(TestListeners.class)
public class CampaignBetaDeleteTest {

	private static Logger logger = LogManager.getLogger(CampaignBetaDeleteTest.class);
	public WebDriver driver;
	private Properties prop;
	private PageObj pageObj;
	private String sTCName;
	private String env;
	private String baseUrl;
	private static Map<String, String> dataSet;
	String run = "ui";
	Utilities utils;

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {

		prop = Utilities.loadPropertiesFile("config.properties");
		driver = new BrowserUtilities().launchBrowser();
		sTCName = method.getName();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		dataSet = new ConcurrentHashMap<>();
		utils = new Utilities(driver);
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run , env), sTCName);
		dataSet = pageObj.readData().readTestData;
		logger.info(sTCName + " ==>" + dataSet);

	}

	// cam bets page filter page based on status is no longer exist so disabled
//	@Test(description = "SQ-T2543,PS-T81 Delete Campaign", groups = "Regression", priority = 0)
	public void T2543_verifyDeleteCampaign_PartOne() throws Exception {
		// admin user login
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		//// Click Campaigns Link
//		pageObj.menupage().clickCampaignsMenu();
//		pageObj.menupage().clickCampaignsBetaLink();

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.campaignspage().selectMessagedrpValue("Mass Notification");

//		Thread.sleep(15000);
		pageObj.campaignsbetaPage().filterStatus("Scheduled");

		// delete campaign from campaign beta page
		pageObj.campaignsbetaPage().deleteCampFromBetaPage();
		Thread.sleep(4000);

		pageObj.campaignsbetaPage().selectCampaign();
		Thread.sleep(2000);
		utils.switchToWindow();
//		Thread.sleep(4000);
		pageObj.campaignsbetaPage().selectdotOptionsValue("Delete");
		String value = pageObj.campaignsbetaPage().getPopUpValues();
		Assert.assertTrue(value.contains("Do you want to delete this campaign?"),
				"Delete popup message did not matched");
		pageObj.campaignsbetaPage().clickCancelBtn();

		utils.goBack();
		utils.acceptAlert(driver);
//		Thread.sleep(15000);
		pageObj.campaignsbetaPage().filterStatus("Draft");

		pageObj.campaignsbetaPage().selectCampaign();
		Thread.sleep(2000);
		utils.switchToWindow();
//		Thread.sleep(4000);
		pageObj.campaignsbetaPage().selectdotOptionsValue("Delete");
		pageObj.campaignsbetaPage().clickDeleteBtn();
	}

//	@Test(description = "SQ-T2543 Delete Campaign", groups = "Regression", priority = 1)
	public void T2543_verifyDeleteCampaign_PartThree() throws Exception {
		String CampaignName = "Automation Notification Campaign" + CreateDateTime.getTimeDateString();
		// Busines manager with no workflow permissions
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().logintoInstance(dataSet.get("userSubmitter"), dataSet.get("password"));
//		pageObj.menupage().clickCampaignsMenu();
//		pageObj.menupage().clickCampaignsBetaLink();

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.campaignspage().selectMessagedrpValue("Mass Notification");

//		Thread.sleep(5000);

		pageObj.campaignsbetaPage().clickNewCampaignBtn();
//		Thread.sleep(2000);
		pageObj.campaignsbetaPage().SetCampaignName(CampaignName);
//		Thread.sleep(2000);
		pageObj.campaignsbetaPage().setCampaignType("message");
		pageObj.campaignsbetaPage().clickContinueBtn();
		Thread.sleep(2000);
		// create campaign
		utils.switchToWindow();
		pageObj.campaignsbetaPage().setSegmentType("custom");
//		Thread.sleep(2000);
		pageObj.campaignsbetaPage().clickNextBtn();
		pageObj.campaignsbetaPage().setEmailNotification("Test Title", "Test Body");
		pageObj.campaignsbetaPage().clickNextBtn();
		pageObj.campaignsbetaPage().setStartDate();
		pageObj.campaignsbetaPage().setTimeZone("(GMT+05:30) New Delhi (IST)");
		pageObj.campaignsbetaPage().clickNextBtn();

		// Submit for approval
		pageObj.campaignsbetaPage().clickSubmitForApprovalBtn();
		String msg = pageObj.campaignsbetaPage().validateSuccessMessage();
		Assert.assertEquals(msg, "Your campaign has been submitted for approval.",
				"Success message text did not matched");
		TestListeners.extentTest.get().pass("Campaign created with success message");
		// validate campaign status
//		pageObj.menupage().clickCampaignsMenu();
//		pageObj.menupage().clickCampaignsBetaLink();

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// pageObj.campaignspage().selectMessagedrpValue("Mass Notification");

//		Thread.sleep(10000);
		pageObj.campaignsbetaPage().filterStatus("Pending Approval");
		pageObj.campaignsbetaPage().selectCampaign();
//		Thread.sleep(4000);
		pageObj.campaignsbetaPage().selectdotOptionsValue("Delete");
		pageObj.campaignsbetaPage().clickCancelBtn();
		// Franchise admin part is covered in SQ-T2543

	}

//	@Test(description = "SQ-T2543 Delete Campaign", groups = "Regression", priority = 2)
	public void T2543_verifyDeleteCampaign_PartTwo() throws Exception {

		// Busines owner with workflowpermissions
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().logintoInstance(dataSet.get("userAprrover"), dataSet.get("password"));

		// commenting below 2 lines for now
		// pageObj.menupage().clickCampaignsMenu();
//		pageObj.menupage().clickCampaignsBetaLink();
//		Thread.sleep(5000);
		pageObj.campaignsbetaPage().filterStatus("Pending Approval");
		pageObj.campaignsbetaPage().selectCampaign();
		// utils.switchToWindow();
		Thread.sleep(2000);
		pageObj.campaignsbetaPage().selectdotOptionsValue("Delete");
		pageObj.campaignsbetaPage().clickCancelBtn();

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}
}
