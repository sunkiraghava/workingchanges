package com.punchh.server.deprecatedTest;

import java.lang.reflect.Method;
import java.text.ParseException;
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

import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
//not needed in regression suite as per discussion with Khushboo Soni basic flows not needed now 
public class CampaignBetaTest {

	private static Logger logger = LogManager.getLogger(CampaignBetaTest.class);
	public WebDriver driver;
	@SuppressWarnings("unused")
	private Properties prop;
	private PageObj pageObj;
	private String sTCName;
	private String env;
	private String baseUrl;
	private static Map<String, String> dataSet;
	String run = "ui";
	Utilities utils;
	String approveCampaignName;
	String rejectCampaignName;
	String campaignName;
	String massOfferCampaignName;

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

	// not needed in regression suite
	@Test(description = "SQ-T2536 Add New Campaign BETA action for Mass Notification", groups = { "regression",
			"dailyrun" }, priority = 0)
	public void T2536_verifyAddNewCampaignBETAactionforMassNotification() throws InterruptedException {

		String campaignName = "Automation Notification Campaign" + CreateDateTime.getTimeDateString();
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value
		pageObj.campaignspage().selectMessagedrpValue("Mass Notification");
		pageObj.campaignspage().clickNewCampaignBetaBtn();
		boolean msgType = pageObj.campaignsbetaPage().messageGuest();
		boolean offerType = pageObj.campaignsbetaPage().sendOffer();
		Assert.assertFalse(msgType, "Issue in validating appearance of Message Guest button");
		Assert.assertFalse(offerType, "Issue in validating appearance of Send Offer button");
		pageObj.campaignsbetaPage().SetCampaignName(campaignName);
		pageObj.campaignsbetaPage().clickContinueBtn();
		utils.switchToWindow();
		String whoTitle = pageObj.campaignsbetaPage().whoTitle();
		Assert.assertEquals(whoTitle, "Who is the target audience?", "Who title did not matched");

	}

	// not needed in regression suite
	@Test(description = "SQ-T2537 Add New Campaign BETA action for Mass Offer", groups = { "regression",
			"dailyrun" }, priority = 1)
	public void T2537_verifyAddNewCampaignBETAactionforMassOffer() throws InterruptedException {

		String campaignName = "Automation Mass Campaign" + CreateDateTime.getTimeDateString();
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value
		pageObj.campaignspage().selectOfferdrpValue("Mass Offer");
		pageObj.campaignspage().clickNewCampaignBetaBtn();
		boolean msgType = pageObj.campaignsbetaPage().messageGuest();
		boolean offerType = pageObj.campaignsbetaPage().sendOffer();
		Assert.assertFalse(msgType, "Issue in validating appearance of Message Guest button");
		Assert.assertFalse(offerType, "Issue in validating appearance of Send Offer button");
		pageObj.campaignsbetaPage().SetCampaignName(campaignName);
		pageObj.campaignsbetaPage().clickContinueBtn();
		Thread.sleep(2000);
		utils.switchToWindow();
		String whoTitle = pageObj.campaignsbetaPage().whoTitle();
		Assert.assertEquals(whoTitle, "Who is the target audience?", "Who title did not matched");
	}

	// not needed in regression suite
	@SuppressWarnings("static-access")
	@Test(description = "SQ-T2539 Campaign Audit Logs with admin", groups = { "regression", "dailyrun" }, priority = 2)
	public void T2539_verifyCampaignAuditLogsAdminUser() throws InterruptedException {

		// admin user login
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickOnSwitchToClassicCamp();
//		Thread.sleep(10000);
		pageObj.campaignsbetaPage().searchCampaign("Automation MassOffer Campaign_DND");
		pageObj.campaignsbetaPage().selectCampaign();
		Thread.sleep(2000);
		// utils.switchToWindow();
		pageObj.campaignsbetaPage().goToAuditLogs();
		// pageObj.campaignsbetaPage().auditLogs();
		if (utils.isAlertpresent(driver)) {
			utils.acceptAlert(driver);
		}
		boolean adminStatus = pageObj.campaignsbetaPage().checkAuditLogs();
		Assert.assertTrue(adminStatus, "Audit Logs page did not displayed");
		utils.logPass("Admin user navigated to Audit logs successfully");

	}

//		As discussed with Khushboo soni this functionality has been removed 

//	@Test(description = "SQ-T2540 Export Campaign Report", groups = "Regression", priority = 3)
	public void T2540_verifyExportCampaignReport() throws InterruptedException {
		// admin user login
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// Click Campaigns Link
//		pageObj.menupage().clickCampaignsMenu();
//		pageObj.menupage().clickCampaignsBetaLink();
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickSwitchToClassicBtn();
//		Thread.sleep(20000);
//		List<String> processedStatus = pageObj.campaignsbetaPage().filterStatus("Processed");
//		boolean c = pageObj.campaignsbetaPage().verifyAllEqual(processedStatus);
//		Assert.assertTrue(processedStatus.contains("PROCESSED"), " Campaign status is other than Processed");
//		Assert.assertTrue(c, " Status value for processed did not matched");
//		pageObj.campaignsbetaPage().searchCampaign("Mass Offer");
//		Thread.sleep(2000);

		pageObj.campaignspage().searchAndSelectCamapign("Automation MassOffer Campaign05102613122024");
		// pageObj.campaignsbetaPage().selectCampaign();
		Thread.sleep(2000);
		utils.switchToWindow();
//		Thread.sleep(4000);
		// pageObj.campaignsbetaPage().clickOptionsLink();
		String value = pageObj.campaignsbetaPage().exportReport();
		Assert.assertTrue(value.contains(
				"It may take a few minutes to process your request. We will send you an email as soon as the campaign report is ready."),
				"Success message did not displayed");

	}

	// not needed in regression suite
	@Test(description = "SQ-T2546 Classic Campaign for New Campaign(Beta)", groups = { "regression",
			"dailyrun" }, priority = 1)
	public void T2546_verifyClassicCampaignforNewCampaignBeta() throws InterruptedException {

		campaignName = "Automation MassNotification Campaign" + CreateDateTime.getTimeDateString();
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value
		pageObj.campaignspage().selectMessagedrpValue("Mass Notification");
		pageObj.campaignspage().clickNewCampaignBetaBtn();
		boolean msgType = pageObj.campaignsbetaPage().messageGuest();
		boolean offerType = pageObj.campaignsbetaPage().sendOffer();
		Assert.assertFalse(msgType, "Issue in validating appearance of Message Guest button");
		Assert.assertFalse(offerType, "Issue in validating appearance of Send Offer button");
		pageObj.campaignsbetaPage().SetCampaignName(campaignName);
		Thread.sleep(2000);
		pageObj.campaignsbetaPage().clickContinueBtn();
		Thread.sleep(5000);
		utils.switchToWindow();
		String whoTitle = pageObj.campaignsbetaPage().whoTitle();
		Assert.assertEquals(whoTitle, "Who is the target audience?", "Who title did not matched");

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value
		String campaignNamee = "Automation Mass Campaign" + CreateDateTime.getTimeDateString();
		pageObj.campaignspage().selectOfferdrpValue("Mass Offer");
		pageObj.campaignspage().clickNewCampaignBetaBtn();
		boolean msgTypee = pageObj.campaignsbetaPage().messageGuest();
		boolean offerTypee = pageObj.campaignsbetaPage().sendOffer();
		Assert.assertFalse(msgTypee, "Issue in validating appearance of Message Guest button");
		Assert.assertFalse(offerTypee, "Issue in validating appearance of Send Offer button");
		pageObj.campaignsbetaPage().SetCampaignName(campaignNamee);
		Thread.sleep(2000);
		pageObj.campaignsbetaPage().clickContinueBtn();
		Thread.sleep(5000);
		utils.switchToWindow();
		String whoTitlee = pageObj.campaignsbetaPage().whoTitle();
		Assert.assertEquals(whoTitlee, "Who is the target audience?", "Who title did not matched");

	}

	// not needed in regression suite
	@Test(description = "SQ-T2542 Stop Processing || "
			+ "SQ-T6099 Verify on schedule page when click on Mass campaign and segment export name it should be navigate to preview page - Mass campaign- stopped", groups = {
					"regression", "unstable", "dailyrun" }, priority = 1)
	public void T2542_verifyStopProcessing() throws InterruptedException, ParseException {

		campaignName = "Automation Notification Campaign" + CreateDateTime.getTimeDateString();
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Disable STO flag
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().offEnableSTOCheckbox();

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.campaignspage().selectMessagedrpValue("Mass Notification");

		Thread.sleep(18000);
		pageObj.campaignsbetaPage().clickNewCampaignBtn();
		Thread.sleep(2000);
		pageObj.campaignsbetaPage().SetCampaignName(campaignName);
		Thread.sleep(2000);
		// pageObj.campaignsbetaPage().setCampaignType("message");
		pageObj.campaignsbetaPage().clickContinueBtn();
		Thread.sleep(5000);
		// create campaign
		utils.switchToWindow();
		Thread.sleep(10000);
		pageObj.campaignsbetaPage().setSegmentType(dataSet.get("segment"));
		Thread.sleep(2000);
		pageObj.campaignsbetaPage().clickNextBtn();
		pageObj.campaignsbetaPage().setEmailNotification("Test Title", "Test Body");
		pageObj.campaignsbetaPage().clickNextBtn();
		pageObj.campaignsbetaPage().setStartDateNew(0);
		boolean flag = pageObj.campaignsbetaPage().verifyStartDateSelectedOrNot("Select");
		Assert.assertTrue(flag, "Start date not selected");
		// pageObj.campaignsbetaPage().setTimeZone("(GMT+05:30) New Delhi (IST)");
		pageObj.campaignsbetaPage().clickNextBtn(); // Submit for approval
		pageObj.campaignsbetaPage().clickActivateCampaignBtn();
		/*
		 * String msg = pageObj.campaignsbetaPage().validateSuccessMessage();
		 * Assert.assertEquals(msg, "Schedule created successfully.",
		 * "Success message text did not matched");
		 * TestListeners.extentTest.get().pass("Campaign created with success message");
		 */

		// run mass offer
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType("Mass Campaign Schedules");
		pageObj.schedulespage().findMassCampaignNameandRun(campaignName);

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		String campName = pageObj.campaignsbetaPage().searchCampaign(campaignName);
		String campStatus = pageObj.campaignsbetaPage().getCampaignStatus();
		Assert.assertTrue(campName.contains(campaignName), "Campaign name did not matched");
		Assert.assertEquals(campStatus, "Processing", "Campaign status did not matched");
		utils.logPass("Campaign name and status validated");
		pageObj.campaignsbetaPage().selectCampaign(); /// Stop processing

		pageObj.campaignsbetaPage().selectdotOptionsValue("Stop Processing");
		String value = pageObj.campaignsbetaPage().getPopUpValues();
		Assert.assertTrue(value.contains("Do you want to stop this campaign?"),
				"Deactivate popup message did not matched");
		pageObj.campaignsbetaPage().clickCancelBtn();

		pageObj.campaignsbetaPage().selectdotOptionsValue("Stop Processing");
		pageObj.campaignsbetaPage().clickSubmitBtn();
		String stopMsg = pageObj.campaignsbetaPage().validateSuccessMessage();
		Assert.assertTrue(stopMsg.contains("campaign has been stopped successfully."),
				"Deactivate campain message did not matched");

		// Click Campaigns Link

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// pageObj.campaignspage().selectMessagedrpValue("Mass Notification");
		Thread.sleep(5000);
		String stoppedCampName = pageObj.campaignsbetaPage().searchCampaign(campaignName);
		String stoppedCampStatus = pageObj.campaignsbetaPage().getCampaignStatus();
		Assert.assertTrue(stoppedCampName.contains(campaignName), "Campaign name did not matched");
		Assert.assertEquals(stoppedCampStatus, "Stopped", "Campaign status did not matched");
		utils.logPass("Campaign name and status validated");

		// SQ-T6099 Verify on schedule page when click on Mass campaign and segment
		// export name it should be navigate to preview page - Mass campaign- stopped
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType("Mass Campaign Schedules");
		boolean nameCheckFlag = pageObj.schedulespage().clickOnScheduledFunctionalityName("campaign", campaignName);
		Assert.assertTrue(nameCheckFlag,
				"Clicked Mass Campaign is not redirecting to the same Mass campaign performance page");
		utils.logPass(
				"Verified that clicking on a stopped Mass Campaign redirects to its corresponding performance page.");

	}

	// not needed in regression suite
	@Test(description = "SQ-T2535 Remove Campaigns BETA Left Navigation menu", groups = { "regression",
			"dailyrun" }, priority = 2)
	public void T2535_verifyRemoveCampaignsBETALeftNavigationmenu() throws Exception {
		// admin revoke campaign beta permission for user
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Settings", "Admin Users");
		pageObj.AdminUsersPage().searchUser(dataSet.get("userSubmitter"));
		pageObj.AdminUsersPage().changeUserPermissionforCampaignBeta();
	}

	// not needed in regression suite
	@Test(description = "SQ-T2535 Remove Campaigns BETA Left Navigation menu", groups = { "regression",
			"dailyrun" }, priority = 3)
	public void T2535_verifyRemoveCampaignsBETALeftNavigationmenuPart2() throws Exception {
		// admin grant campaign beta permission for user
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Settings", "Admin Users");
		pageObj.AdminUsersPage().searchUser(dataSet.get("userSubmitter"));
		pageObj.AdminUsersPage().changeUserPermissionforCampaignBeta();
	}

	// not needed in regression suite
	@Test(description = "SQ-T2534 Duplicate Mass Campaign ", groups = { "regression", "dailyrun" }, priority = 1)
	public void T2534_verifyDuplicateMassCampaign_PartTwo() throws Exception {

		// Franchise admin 1 create campaign Franchise admin 2 wont see campaign (cannot
		// duplicate & delete)
		massOfferCampaignName = "Automation Notification Campaign" + CreateDateTime.getTimeDateString();
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().logintoInstance(dataSet.get("fAdminone"), dataSet.get("fPassword"));
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value
		pageObj.campaignspage().selectMessagedrpValue("Mass Notification");

		pageObj.campaignspage().clickNewCampaignBetaBtn();
		pageObj.campaignsbetaPage().SetCampaignName(massOfferCampaignName);
		Thread.sleep(2000);
		pageObj.campaignsbetaPage().clickContinueBtn();
		Thread.sleep(2000);
		utils.switchToWindow();
		pageObj.campaignsbetaPage().clickSegmentBtn();
		pageObj.campaignsbetaPage().setSegmentType(dataSet.get("segment"));
		pageObj.campaignsbetaPage().clickNextBtn();
		Thread.sleep(2000);
		pageObj.campaignsbetaPage().setEmailNotification("Test Title", "Test Body");
		pageObj.campaignsbetaPage().clickNextBtn();
		pageObj.campaignsbetaPage().setStartDateNew(0);
		pageObj.campaignsbetaPage().setTimeZone(dataSet.get("timeZone"));
		// Timezone need to be handle here
		// pageObj.campaignsbetaPage().setRecommendedSendDate(); STO
		pageObj.campaignsbetaPage().clickNextBtn();
		pageObj.campaignsbetaPage().clickActivateCampaignBtn();
	}

	// not needed in regression suite
	@Test(description = "SQ-T2534 Duplicate Mass Campaign ", groups = { "regression", "dailyrun" }, priority = 2)
	public void T2534_verifyDuplicateMassCampaign_PartThree() throws Exception {
		// Franchise admin 2 wont see campaign (cannotduplicate & delete)

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().logintoInstance(dataSet.get("fAdminTwo"), dataSet.get("fPassword"));
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value
		pageObj.campaignspage().selectMessagedrpValue("Mass Notification");
		boolean status = pageObj.campaignspage().searchClassicCampaign(massOfferCampaignName);
		Assert.assertFalse(status, "Error in searching campaign");

		// Busines owner with workflow permissions
		/*
		 * pageObj.instanceDashboardPage().logintoInstance(dataSet.get("userAprrover"),
		 * dataSet.get("password")); // Click Campaigns Link
		 * pageObj.menupage().clickCampaignsMenu();
		 * pageObj.menupage().clickCampaignsBetaLink(); Thread.sleep(5000);
		 * pageObj.campaignsbetaPage().searchCampaign("Automation Notification Campaign"
		 * ); pageObj.campaignsbetaPage().selectCampaign(); utils.switchToWindow();
		 * Thread.sleep(4000);
		 * pageObj.campaignsbetaPage().selectdotOptionsValue("Duplicate");
		 * pageObj.campaignsbetaPage().clickCancelBtn();
		 * pageObj.campaignsbetaPage().logoutApp(); utils.acceptAlert(driver);
		 * 
		 * // Busines manager with no workflow permissions
		 * pageObj.instanceDashboardPage().logintoInstance(dataSet.get("userSubmitter"),
		 * dataSet.get("password")); // Click Campaigns Link
		 * pageObj.menupage().clickCampaignsMenu();
		 * pageObj.menupage().clickCampaignsBetaLink(); Thread.sleep(5000);
		 * pageObj.campaignsbetaPage().searchCampaign("Automation Notification Campaign"
		 * ); pageObj.campaignsbetaPage().selectCampaign(); utils.switchToWindow();
		 * pageObj.campaignsbetaPage().selectdotOptionsValue("Duplicate");
		 * pageObj.campaignsbetaPage().clickCancelBtn();
		 */

	}

	// not needed in regression suite
	@Test(description = "SQ-T2538 Gifting scoping & availability", groups = { "regression", "dailyrun" }, priority = 1)
	public void T2538_verifyGiftingscopingAndavailability() throws Exception {

		String massOfferCampaignName = "Automation Massoffer Campaign" + CreateDateTime.getTimeDateString();
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// navigate to email templates
		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value
		pageObj.campaignspage().selectOfferdrpValue("Mass Offer");

		// validate error msg for blank name field
		pageObj.campaignspage().clickNewCampaignBetaBtn();
		pageObj.campaignsbetaPage().SetCampaignName(massOfferCampaignName);
		Thread.sleep(2000);
		pageObj.campaignsbetaPage().clickContinueBtn();
		Thread.sleep(5000);
		utils.switchToWindow();
		Thread.sleep(5000);
		pageObj.campaignsbetaPage().clickWhatTab();
		pageObj.campaignsbetaPage().clickRedeemablesBtn();
		// Set redeemable
		pageObj.campaignsbetaPage().setRedeemable(dataSet.get("redemable"));
	}

	// not needed in regression suite
	@Test(description = "SQ-T2538 Gifting scoping & availability", groups = { "regression", "dailyrun" }, priority = 2)
	public void T2538_verifyGiftingscopingAndavailabilityPart1() throws Exception {

		String campaignName = "Automation Massoffer Campaign" + CreateDateTime.getTimeDateString();
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().logintoInstance(dataSet.get("fAdminone"), dataSet.get("fPassword"));
		// pageObj.menupage().clickCampaignsMenu();
		// pageObj.menupage().clickCampaignsLink();
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value
		pageObj.campaignspage().selectOfferdrpValue("Mass Offer");

		// validate error msg for blank name field
		pageObj.campaignspage().clickNewCampaignBetaBtn();
		pageObj.campaignsbetaPage().SetCampaignName(campaignName);
		Thread.sleep(2000);
		pageObj.campaignsbetaPage().clickContinueBtn();
		Thread.sleep(2000);
		utils.switchToWindow();
		pageObj.campaignsbetaPage().clickWhatTab();
		pageObj.campaignsbetaPage().clickRedeemablesBtn();
		// Set redeemable
		pageObj.campaignsbetaPage().setRedeemable("(T) F1_Franchise");
	}

	// not needed in regression suite
	@Test(description = "SQ-T2538 Gifting scoping & availability", groups = { "regression", "dailyrun" }, priority = 3)
	public void T2538_verifyGiftingscopingAndavailabilityPart2() throws Exception {
		String campaigNName = "Automation Massoffer Campaign" + CreateDateTime.getTimeDateString();
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().logintoInstance(dataSet.get("fAdminTwo"), dataSet.get("fPassword"));

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value
		pageObj.campaignspage().selectOfferdrpValue("Mass Offer");

		// validate error msg for blank name field
		pageObj.campaignspage().clickNewCampaignBetaBtn();
		pageObj.campaignsbetaPage().SetCampaignName(campaigNName);
		Thread.sleep(2000);
		pageObj.campaignsbetaPage().clickContinueBtn();
		Thread.sleep(2000);
		utils.switchToWindow();
		pageObj.campaignsbetaPage().clickWhatTab();
		pageObj.newCamHomePage().campaignAdvertiseBlock();
		pageObj.campaignsbetaPage().clickRedeemablesBtn();
		// Set redeemable
		pageObj.campaignsbetaPage().setRedeemable("(T) F2_Franchise");

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() throws Exception {
		pageObj.utils().clearDataSet(dataSet);
		logger.info("Data set cleared");
	}

	@AfterClass(alwaysRun = true)
	public void closeBrowser() {
		driver.quit();
		logger.info("Browser closed");
	}
}
