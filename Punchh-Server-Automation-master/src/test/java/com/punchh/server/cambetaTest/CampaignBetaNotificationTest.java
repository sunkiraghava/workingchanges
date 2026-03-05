package com.punchh.server.cambetaTest;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import com.punchh.server.annotations.Owner;
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

import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class CampaignBetaNotificationTest {

	private static Logger logger = LogManager.getLogger(CampaignBetaNotificationTest.class);
	public WebDriver driver;
	private Properties prop;
	private PageObj pageObj;
	private String sTCName;
	private String env;
	private String baseUrl;
	private static Map<String, String> dataSet;
	String run = "ui";
	Utilities utils;
	private String campaignName;

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
		logger.info(sTCName + " ==>" + dataSet);
		// Instance move to all business page
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
	}

	@Test(description = "SQ-T2621 Send Test Notification", groups = { "regression", "dailyrun" }, priority = 0)
	@Owner(name = "Amit Kumar")
	public void T2621_verifySendTestNotification() throws InterruptedException {

		campaignName = "Automation MassNotification Campaign" + CreateDateTime.getTimeDateString();
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.campaignspage().selectMessagedrpValue("Mass Notification");

		pageObj.campaignsbetaPage().clickNewCampaignBtn();

		pageObj.campaignsbetaPage().SetCampaignName(campaignName);

		pageObj.campaignsbetaPage().clickContinueBtn();

		// create campaign
		utils.switchToWindow();
		pageObj.campaignsbetaPage().setSegmentType("custom");

		pageObj.campaignsbetaPage().clickNextBtn();
		pageObj.campaignsbetaPage().setEmailNotification("Test Title", "Test Body");

		// click send test message link invalid email
		pageObj.campaignsbetaPage().clickSendTestMsgLink();
		pageObj.campaignsbetaPage().sendEmail("abc@1,");
		boolean errorstatus = pageObj.campaignsbetaPage().emailerrorMsg();
		Assert.assertTrue(errorstatus, "email error msg did not displayed");

		// valid and invalid email combination
		pageObj.campaignsbetaPage().clickSendTestMsgLink();
		pageObj.campaignsbetaPage().sendEmail("a1@punchh.com, a2@punchh.com,c@3.com,abc@1,xyz@2,");
		boolean errstatus = pageObj.campaignsbetaPage().emailerrorMsg();
		Assert.assertTrue(errstatus, "email error msg did not displayed");

		// click send test message one valid email
		pageObj.campaignsbetaPage().clickSendTestMsgLink();
		pageObj.campaignsbetaPage().sendEmail("test.user1@punchh.com,");
		String msg = pageObj.campaignsbetaPage().validateNfsent();
		Assert.assertEquals(msg, "Test Notifications Sent", "test notification sent msg did not appeared");

		// 5 valid emails validation
		pageObj.campaignsbetaPage().clickSendTestMsgLink();
		pageObj.campaignsbetaPage().sendEmail(
				"test.user1@punchh.com,test.user2@punchh.com,test.user3@punchh.com,test.user4@punchh.com,test.user5@punchh.com,");

		String msg1 = pageObj.campaignsbetaPage().validateNfsent();
		Assert.assertEquals(msg1, "Test Notifications Sent", "test notification sent msg did not appeared");

		// 6 valid emails for max emails limit
		pageObj.campaignsbetaPage().clickSendTestMsgLink();
		pageObj.campaignsbetaPage().sendEmail(
				"test.user1@punchh.com,test.user2@punchh.com,test.user3@punchh.com,test.user4@punchh.com,test.user5@punchh.com,test.user6@punchh.com");
		String maxemailstatus = pageObj.campaignsbetaPage().maxemailMsg();
		Assert.assertEquals(maxemailstatus, "Maximum allowed email address 5 is reached",
				"test notification sent msg did not appeared");

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