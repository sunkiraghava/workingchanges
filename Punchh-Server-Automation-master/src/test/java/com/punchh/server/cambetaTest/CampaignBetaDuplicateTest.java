package com.punchh.server.cambetaTest;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Properties;
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
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class CampaignBetaDuplicateTest {
// single login cannot be used as multiple user roles logins are there
	private static Logger logger = LogManager.getLogger(CampaignBetaDuplicateTest.class);
	public WebDriver driver;
	private Properties prop;
	private PageObj pageObj;
	private String sTCName;
	private String env;
	private String baseUrl;
	private static Map<String, String> dataSet;
	String run = "ui";
	Utilities utils;
	String massOfferCampaignName;
	private String campaignName;
	private String campaignName2;

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
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		logger.info(sTCName + " ==>" + dataSet);

	}

	@Test(description = "SQ-T2534 Duplicate Mass Campaign ", groups = { "regression" }, priority = 0)
	@Owner(name = "Amit Kumar")
	public void T2534_verifyDuplicateMassCampaign_PartOne() throws Exception {

		String CampaignName = "Automation Notification Campaign" + CreateDateTime.getTimeDateString();
		// admin user login
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickSwitchToClassicBtn();
		pageObj.campaignspage().selectMessagedrpValue("Mass Notification");
		Thread.sleep(15000);

		pageObj.campaignsbetaPage().clickNewCampaignBtn();
		Thread.sleep(2000);
		pageObj.campaignsbetaPage().SetCampaignName(CampaignName);
		Thread.sleep(2000);
		// pageObj.campaignsbetaPage().setCampaignType("message");
		pageObj.campaignsbetaPage().clickContinueBtn();
		Thread.sleep(4000);
		utils.switchToWindow();
		// Set segment
		pageObj.campaignsbetaPage().clickSegmentBtn();
		pageObj.campaignsbetaPage().setSegmentType("custom");
		Thread.sleep(2000);
		pageObj.campaignsbetaPage().clickNextBtn();
		pageObj.campaignsbetaPage().setEmailNotification("Test Title", "Test Body");
		pageObj.campaignsbetaPage().clickNextBtn();
		pageObj.campaignsbetaPage().setStartDateNew(0);
		pageObj.campaignsbetaPage().setTimeZone(dataSet.get("timeZone"));
		pageObj.campaignsbetaPage().clickNextBtn();
		// Submit for approval

		pageObj.campaignsbetaPage().clickActivateCampaignBtn();
		List<String> summaryData = pageObj.campaignsbetaPage().CheckSummary();
		System.out.println(summaryData);

//		pageObj.menupage().clickCampaignsMenu();
//		pageObj.menupage().clickCampaignsBetaLink();
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		Thread.sleep(5000);
		pageObj.campaignsbetaPage().searchCampaign(CampaignName);
		pageObj.campaignsbetaPage().selectCampaign();
		utils.switchToWindow();
		Thread.sleep(4000);

		pageObj.campaignsbetaPage().selectdotOptionsValue("Duplicate");
		Thread.sleep(2000);
		pageObj.campaignsbetaPage().clickContinueBtn();
		Thread.sleep(5000);
		utils.switchToWindow();
		Thread.sleep(5000);
		pageObj.campaignsbetaPage().clickWhenTab();
		pageObj.campaignsbetaPage().setStartDateNew(0);
		pageObj.campaignsbetaPage().setTimeZone(dataSet.get("timeZone"));
		pageObj.campaignsbetaPage().clickNextBtn();

		List<String> duplicatesummaryData = pageObj.campaignsbetaPage().CheckSummary();
		System.out.println(summaryData);
		// Assert.assertTrue(summaryData.equals(duplicatesummaryData),"Duplicate
		// campaign summary details did not matched");
		pageObj.campaignsbetaPage().clickActivateCampaignBtn();
		String schedulEMsg = pageObj.campaignsbetaPage().validateSuccessMessage();
		Assert.assertEquals(schedulEMsg, "Schedule created successfully.", "Success message text did not matched");
		TestListeners.extentTest.get().pass("Duplicate campaign created successfully");
		logger.info("Duplicate campaign created successfully");

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// utils.acceptAlert(driver);
		Thread.sleep(5000);
		pageObj.campaignsbetaPage().searchCampaign("Automation Notification Campaign");
		pageObj.campaignspage().searchAndSelectCamapign("Automation Notification Campaign");
		Thread.sleep(4000);
		pageObj.campaignsbetaPage().selectdotOptionsValue("Duplicate");
		String value = pageObj.campaignsbetaPage().getPopUpValues();
		Assert.assertTrue(value.contains("Create a New Campaign"), "Duplicate popup message did not matched");
		pageObj.campaignsbetaPage().clickCancelBtn();

	}

	// This code will be available in upcoming release on pp for CAM-7028
	// @Test(description = "SQ-T7552 Verify access is restricted for users to create
	// new beta campaigns and duplicate beta campaigns."
	// +"SQ-T7553 Verify user is restricted from duplicate beta campaigns from
	// classic home page."
	// +"SQ-T7554 Verify user is restricted to duplicate beta campaigns from
	// campaign builder page."
	// +"SQ-T7555 Verify user is restricted to duplicate beta campaigns from
	// campaign summary page.", groups = { "regression" }, priority = 0)
	// @Owner(name = "Jeevraj Singh")
	public void T7552_verifyAccessRestrictForNewBetaCampaign() throws Exception {

		String campaignName = "Automation Mass offer Beta Campaign" + CreateDateTime.getTimeDateString();
		String campaignName2 = "Automation Post checkin offer Beta Campaign" + CreateDateTime.getTimeDateString();
		// admin user login
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Enable flag for Campaign BETA
		pageObj.dashboardpage().checkUncheckFlagCockpitDashboard("Enable Campaign BETA?", "check");

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickSwitchToClassicBtn();
		pageObj.campaignspage().selectOfferdrpValue("Mass Offer");
		Thread.sleep(15000);

		pageObj.campaignsbetaPage().clickNewCampaignBtn();
		Thread.sleep(2000);
		pageObj.campaignsbetaPage().SetCampaignName(campaignName);
		Thread.sleep(2000);
		pageObj.campaignsbetaPage().clickContinueBtn();
		Thread.sleep(4000);
		utils.switchToWindow();
		// Set segment
		pageObj.campaignsbetaPage().clickSegmentBtn();
		pageObj.campaignsbetaPage().setSegmentType(dataSet.get("segment"));
		Thread.sleep(2000);
		pageObj.campaignsbetaPage().clickNextBtn();
		// Set redeemable
		Thread.sleep(10000);
		pageObj.campaignsbetaPage().setRedeemable(dataSet.get("redemable"));
		pageObj.campaignsbetaPage().setOfferReason("Support Offer");
		pageObj.campaignsbetaPage().setEmailNotification("Test Title", "Test Body");
		pageObj.campaignsbetaPage().clickNextBtn();
		pageObj.campaignsbetaPage().setStartDateNew(0);
		pageObj.campaignsbetaPage().setTimeZone(dataSet.get("timeZone"));
		pageObj.campaignsbetaPage().clickNextBtn();
		Thread.sleep(2000);
		pageObj.campaignsbetaPage().clickActivateCampaignBtn();
		Thread.sleep(3000);

		// Disable flag for Campaign BETA
		pageObj.newCamHomePage().clickNewCamHomePageBtn();
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().checkUncheckFlagCockpitDashboard("Enable Campaign BETA?", "uncheck");

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickSwitchToClassicBtn();

		// Verify Beta create option is not present on classic home page listing for
		// mass campaign
		boolean isNewCamBetaBtnPresent = pageObj.campaignsbetaPage().verifyNewCamBetaBtnPresent();
		Assert.assertFalse(isNewCamBetaBtnPresent,
				"New campaign beta create option is present for Beta mass offer campaign on classic home page");
		utils.logit("pass",
				"New Campaign (Beta) option is not present on classic home page for mass offer beta campaign");
		pageObj.campaignspage().selectOfferdrpValue("Mass Offer");

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		Thread.sleep(5000);
		pageObj.campaignsbetaPage().searchCampaign(campaignName);

		// Verify duplicate icon on classic home page for beta campaign is not present
		// on listing page.
		boolean isDuplicateIconPresent = pageObj.campaignsbetaPage().verifyDuplicateIconPresentForBetaCamHomePage();
		Assert.assertFalse(isDuplicateIconPresent, "Duplicate icon is present for Beta campaign on classic home page");
		utils.logit("pass", "Duplicate icon is not present on classic summary page for beta mass campaigns");

		pageObj.campaignsbetaPage().selectCampaign();
		Thread.sleep(4000);

		pageObj.campaignsbetaPage().clickPopOverListDots();

		boolean isDuplicateOptionPresentCamBuilderBeta = pageObj.campaignsbetaPage()
				.verifyDuplicateOptPresentForBetaCamBuilderSummPage();
		Assert.assertFalse(isDuplicateOptionPresentCamBuilderBeta,
				"Duplicate option is present on beta campaign builder page for mass offer");
		utils.logit("pass",
				"Duplicate option is not present on beta campaign builder page options for mass offer beta campaign");

		// run mass offer beta campaign
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));

		pageObj.schedulespage().findMassCampaignNameandRun(campaignName);
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		Thread.sleep(5000);
		pageObj.campaignspage().selectOfferdrpValue("Mass Offer");
		pageObj.campaignsbetaPage().searchCampaign(campaignName);

		pageObj.campaignsbetaPage().selectCampaign();
		Thread.sleep(4000);

		pageObj.campaignsbetaPage().clickPopOverListDots();

		boolean isDuplicateOptionPresentCamSummaryBeta = pageObj.campaignsbetaPage()
				.verifyDuplicateOptPresentForBetaCamBuilderSummPage();
		Assert.assertFalse(isDuplicateOptionPresentCamSummaryBeta,
				"Duplicate option is present on beta mass offer campaign summary page");
		utils.logit("pass", "Duplicate option is not present on beta mass offer campaign summary page options");

		// Verify signup beta campaign create button is present when Beta campaign flag
		// is disabled.
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		Thread.sleep(5000);
		pageObj.campaignspage().selectOngoingdrpValue("Signup");

		boolean isNewCamBetaBtnPresentSignup = pageObj.campaignsbetaPage().verifyNewCamBetaBtnPresent();
		Assert.assertFalse(isNewCamBetaBtnPresentSignup,
				"New campaign beta create option is present for Beta signup campaign on classic home page");
		utils.logit("pass", "New Campaign (Beta) option is not present on classic home page for signup beta campaign");

		// Enable Campaign beta flag in cockpit
		pageObj.newCamHomePage().clickNewCamHomePageBtn();
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().checkUncheckFlagCockpitDashboard("Enable Campaign BETA?", "check");

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickSwitchToClassicBtn();

		pageObj.campaignspage().selectOfferdrpValue("Post Checkin Offer");
		Thread.sleep(15000);

		pageObj.campaignsbetaPage().clickNewCampaignBtn();
		Thread.sleep(2000);
		pageObj.campaignsbetaPage().SetCampaignName(campaignName2);
		Thread.sleep(2000);
		pageObj.campaignsbetaPage().clickContinueBtn();
		Thread.sleep(4000);
		utils.switchToWindow();
		// Set segment
		pageObj.campaignsbetaPage().setSegmentType1(dataSet.get("segment"));
		Thread.sleep(2000);
		pageObj.campaignsbetaPage().clickNextBtn();
		// Set redeemable
		Thread.sleep(10000);
		pageObj.campaignsbetaPage().setRedeemable(dataSet.get("redemable"));
		pageObj.campaignsbetaPage().setOfferReason("Support Offer");
		pageObj.campaignsbetaPage().setEmailNotification("Test Title", "Test Body");
		pageObj.campaignsbetaPage().clickNextBtn();
		pageObj.campaignsbetaPage().setStartDateNew(0);
		pageObj.campaignsbetaPage().setTimeZone(dataSet.get("timeZone"));
		pageObj.campaignsbetaPage().setExecutionDelay();
		pageObj.campaignsbetaPage().clickNextBtn();
		Thread.sleep(2000);
		pageObj.campaignsbetaPage().clickActivateCampaignBtn();
		Thread.sleep(3000);

		// Disable flag for Campaign BETA
		pageObj.newCamHomePage().clickNewCamHomePageBtn();
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().checkUncheckFlagCockpitDashboard("Enable Campaign BETA?", "uncheck");

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickSwitchToClassicBtn();

		// Verify Beta create option is not present on classic home page listing
		boolean isNewCamBetaBtnPresentPostCheckin = pageObj.campaignsbetaPage().verifyNewCamBetaBtnPresent();
		Assert.assertFalse(isNewCamBetaBtnPresentPostCheckin,
				"New campaign beta create option is present for Beta post checkin offer campaign on classic home page");
		utils.logit("pass",
				"New Campaign (Beta) option is not present on classic home page for post checkin offer beta campaign");
		pageObj.campaignspage().selectOfferdrpValue("Post Checkin Offer");

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		Thread.sleep(5000);
		pageObj.campaignsbetaPage().searchCampaign(campaignName2);
		// utils.switchToWindow();
		// Thread.sleep(4000);

		// Verify duplicate icon on classic home page for beta post checkin campaign is
		// not present on listing page.
		boolean isDuplicateIconPresentPostCheckin = pageObj.campaignsbetaPage()
				.verifyDuplicateIconPresentForBetaCamHomePage();
		Assert.assertFalse(isDuplicateIconPresentPostCheckin,
				"Duplicate icon is present for Beta post checkin campaign on classic home page");
		utils.logit("pass", "Duplicate icon is not present for beta post checkin campaigns");

		pageObj.campaignsbetaPage().selectCampaign();
		Thread.sleep(4000);

		pageObj.campaignsbetaPage().clickPopOverListDots();

		boolean isDuplicateOptionPresentCamSummaryBetaPostCheckin = pageObj.campaignsbetaPage()
				.verifyDuplicateOptPresentForBetaCamBuilderSummPage();
		Assert.assertFalse(isDuplicateOptionPresentCamSummaryBetaPostCheckin,
				"Duplicate option is present on beta post checkin offer campaign summary page");
		utils.logit("pass", "Duplicate option is not present on beta post checkin offer campaign summary page options");

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() throws Exception {
		pageObj.utils().deleteCampaignFromDb(campaignName, env);
		pageObj.utils().deleteCampaignFromDb(campaignName2, env);
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}
}