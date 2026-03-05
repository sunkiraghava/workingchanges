package com.punchh.server.NCHPTest;

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

import com.punchh.server.annotations.Owner;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.TestListeners;

// Author: Amit
@Listeners(TestListeners.class)
public class NCHPArchivalTest {

	private static Logger logger = LogManager.getLogger(NCHPArchivalTest.class);
	public WebDriver driver;
	private Properties prop;
	private PageObj pageObj;
	private String sTCName;
	private String env;
	private String baseUrl;
	private static Map<String, String> dataSet;
	String run = "ui";
	String campaignName;

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
	}

// Amit
	@Test(description = "SQ-T4590 Verify the archival of mass notification campaign with draft status", priority = 0, groups = {
			"regression", "dailyrun" })
	@Owner(name = "Amit Kumar")
	public void T4590_verifyArchivalOfMassNotificationCampaignWithDraftStatus()
			throws InterruptedException, ParseException {
		campaignName = "AutomationMassNotificationCampaign" + CreateDateTime.getTimeDateString();
		String dateTime = CreateDateTime.getCurrentDate() + " 11:00 PM";
		logger.info(campaignName + " mass notification campaign name is created");
		// Login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// navigate to cockpit -> earning
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		// Switch to new cam home page and set mass offer cam name
		pageObj.newCamHomePage().createMassCampaignCHP("Mass campaign", "No");
		pageObj.signupcampaignpage().setCampaignName(campaignName);
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().searchCampaign(campaignName);

		String archiveStatus = pageObj.newCamHomePage().archiveCampaign();
		Assert.assertEquals(archiveStatus, "Campaign(s) archived successfully",
				"Campaign archived message did not matched");
		pageObj.utils().logPass("successfully archived the campaign");
	}

	@Test(description = "SQ-T4591 Verify the archival of mass offer campaign with draft status", priority = 1, groups = {
			"regression", "dailyrun" })
	@Owner(name = "Amit Kumar")
	public void T4591_verifyArchivalOfMassOfferCampaignWithDraftStatus() throws InterruptedException, ParseException {
		campaignName = "AutomationMassNotificationCampaign" + CreateDateTime.getTimeDateString();
		String dateTime = CreateDateTime.getCurrentDate() + " 11:00 PM";
		logger.info(campaignName + " mass notification campaign name is created");
		// Login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */

		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// navigate to cockpit -> earning
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Switch to new cam home page and set mass offer cam name
		pageObj.newCamHomePage().createMassCampaignCHP("Mass campaign", "Yes");
		// Set details
		pageObj.signupcampaignpage().createWhatDetailsMassCampaign(campaignName, dataSet.get("giftType"),
				dataSet.get("redemable"));

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().searchCampaign(campaignName);

		String archiveStatus = pageObj.newCamHomePage().archiveCampaign();
		Assert.assertEquals(archiveStatus, "Campaign(s) archived successfully",
				"Campaign archived message did not matched");
		pageObj.utils().logPass("successfully archived the campaign");
	}

	@Test(description = "SQ-T4592 Verify archival of Automation type campaign with Draft status", priority = 2, groups = {
			"regression", "dailyrun" })
	@Owner(name = "Amit Kumar")
	public void T4592_verifyArchivalOfAutomationTypeCampaignWithDraftStatus()
			throws InterruptedException, ParseException {
		campaignName = "AutomationPostCheckinCampaign" + CreateDateTime.getTimeDateString();
		logger.info(campaignName + " post checkin campaign name is created");
		// Login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */

		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// navigate to cockpit -> earning
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// switch to new cam page and set automations post checkins campaign details
		pageObj.newCamHomePage().createAutomationsCampaignCHP("Automations", "Post-checkin", "Yes");
		// Set what whom and whendetails
		pageObj.signupcampaignpage().createWhatDetailsPostcheckinCampaign(campaignName, dataSet.get("giftType"),
				dataSet.get("giftReason"), dataSet.get("redeemable"));

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().searchCampaign(campaignName);

		String archiveStatus = pageObj.newCamHomePage().archiveCampaign();
		Assert.assertEquals(archiveStatus, "Campaign(s) archived successfully",
				"Campaign archived message did not matched");
		pageObj.utils().logPass("successfully archived the campaign");
	}

	@Test(description = "SQ-T4593 Verify the archival of Other type campaign with Draft status", priority = 3, groups = {
			"regression", "dailyrun" })
	@Owner(name = "Amit Kumar")
	public void T4593_verifyArchivalOfOtherTypeCampaignWithDraftStatus() throws InterruptedException, ParseException {
		campaignName = "AutomationSocialCauseCampaign" + CreateDateTime.getTimeDateString();
		logger.info("Social cause campaign name is created :" + campaignName);
		// Login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */

		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// navigate to cockpit -> earning
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// switch to new cam page and set
		pageObj.newCamHomePage().createOtherCampaignCHP("Other", "Social Cause");
		// create social cause campaign
		pageObj.signupcampaignpage().setCampaignName(campaignName);
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().searchCampaign(campaignName);

		String archiveStatus = pageObj.newCamHomePage().archiveCampaign();
		Assert.assertEquals(archiveStatus, "Campaign(s) archived successfully",
				"Campaign archived message did not matched");
		pageObj.utils().logPass("successfully archived the campaign");
	}

	// Amit
	@Test(description = "SQ-T4594  Verify archival of Other type campaign with Inactive status", priority = 4, groups = {
			"regression", "dailyrun" })
	@Owner(name = "Amit Kumar")
	public void T4594_verifyArchivalOfOtherTypeCampaignWithInactiveStatus()
			throws InterruptedException, ParseException {

		campaignName = "AutomationPoromoCampaign" + CreateDateTime.getTimeDateString();
		logger.info("poromo campaign name is created :" + campaignName);
		// Login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */

		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// navigate to cockpit -> earning
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// switch to new cam page and set
		pageObj.newCamHomePage().createOtherCampaignCHP("Other", "Promo");
		// create promo campaign
		String promoCode = "P" + CreateDateTime.getTimeDateAsneed("HHmmssddMM");
		pageObj.signupcampaignpage().createWhatDetailsPromoCampaign(campaignName, promoCode);
		pageObj.signupcampaignpage().setCouponCampaignUsageType(dataSet.get("usageType"));
		pageObj.signupcampaignpage().setPromoCampaignWhomDetails(dataSet.get("noOfGuests"), dataSet.get("giftType"),
				dataSet.get("amount"));

		pageObj.signupcampaignpage().setStartDate();
		pageObj.signupcampaignpage().setEndDateforCouponCampaign(1);

		pageObj.signupcampaignpage().setCamTimeZone("(GMT+05:30) New Delhi ( IST )");
		pageObj.signupcampaignpage().createWhenDetailsCampaign();

		// Search created campaihn in new cam home page and deactivate
		pageObj.newCamHomePage().searchCampaign(campaignName);
		String inactiveStatus = pageObj.newCamHomePage().deactivateCampaign();
		Assert.assertEquals(inactiveStatus, "Inactive", "Campaign deativated status did not matched");
		String archiveStatus = pageObj.newCamHomePage().archiveCampaign();
		Assert.assertEquals(archiveStatus, "Campaign(s) archived successfully",
				"Campaign archived message did not matched");
		pageObj.utils().logPass("successfully archived the campaign");
	}

	// Amit
	@Test(description = "SQ-T4595 Verify archival of Automation type campaign with Inactive status", priority = 5, groups = {
			"regression", "dailyrun" })
	@Owner(name = "Amit Kumar")
	public void T4595_verifyArchivalOfAutomationTypeCampaignWithInactiveStatu() throws InterruptedException {

		campaignName = "AutomationSignupCampaign" + CreateDateTime.getTimeDateString();
		logger.info(campaignName + " Signup campaign name is created");
		// Login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */

		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// navigate to cockpit -> earning
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// switch to new cam page and set automations post checkins campaign details
		// pageObj.newCamHomePage().createAutomationsCampaignCHP("Automations",
		// "Signup", "Yes");
		pageObj.newCamHomePage().createOtherCampaignCHP("Automations", "Signup");

		// create signup campaign with PN Email configured
		pageObj.signupcampaignpage().createWhatDetailsSignupCampaign(campaignName, dataSet.get("giftType"),
				dataSet.get("giftReason"), dataSet.get("redeemable"));
		pageObj.signupcampaignpage().createWhomDetailsSignupCampaign(dataSet.get("pushNotification"),
				dataSet.get("emailSubject"), dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().createWhenDetailsCampaign();
		// Search created campaihn in new cam home page and deactivate
		pageObj.newCamHomePage().searchCampaign(campaignName);
		String inactiveStatus = pageObj.newCamHomePage().deactivateCampaign();
		Assert.assertEquals(inactiveStatus, "Inactive", "Campaign deativated status did not matched");
		String archiveStatus = pageObj.newCamHomePage().archiveCampaign();
		Assert.assertEquals(archiveStatus, "Campaign(s) archived successfully",
				"Campaign archived message did not matched");
		pageObj.utils().logPass("successfully archived the campaign");

	}

	// Amit
	@Test(description = "SQ-T4657 Verify the archival of mass notification campaign with inactive status", priority = 6, groups = {
			"regression", "dailyrun" })
	@Owner(name = "Amit Kumar")
	public void T4657_verifyArchivalOfMassNotificationCampaignWithInactiveStatus()
			throws InterruptedException, ParseException {
		campaignName = "AutomationMassNotificationCampaign" + CreateDateTime.getTimeDateString();
		String dateTime = CreateDateTime.getCurrentDate() + " 11:00 PM";
		logger.info(campaignName + " mass notification campaign name is created");
		// Login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */

		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// navigate to cockpit -> earning
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Switch to new cam home page and set mass offer cam name
		pageObj.newCamHomePage().createMassCampaignCHP("Mass campaign", "No");
		pageObj.signupcampaignpage().setCampaignName(campaignName);
		pageObj.signupcampaignpage().clickNextBtn();
		pageObj.signupcampaignpage().setAudienceType(dataSet.get("audiance"));
		pageObj.signupcampaignpage().setSegmentType(dataSet.get("segment"));
		pageObj.signupcampaignpage().setPushNotification(dataSet.get("pushNotification"));
		pageObj.signupcampaignpage().setEmailSubject(dataSet.get("emailSubject"));
		pageObj.signupcampaignpage().setEmailTemplate(dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().clickNextButton();
		pageObj.signupcampaignpage().createWhenDetailsMassCampaign("Once", dateTime);

		// navigate to cockpit -> earning
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().searchCampaign(campaignName);
		String inactiveStatus = pageObj.newCamHomePage().deactivateCampaign();
		Assert.assertEquals(inactiveStatus, "Inactive", "Campaign deativated status did not matched");
		String archiveStatus = pageObj.newCamHomePage().archiveCampaign();
		Assert.assertEquals(archiveStatus, "Campaign(s) archived successfully",
				"Campaign archived message did not matched");
		pageObj.utils().logPass("successfully archived the campaign");

	}

	// Amit
	@Test(description = "SQ-T4656 Verify the archival of mass offer campaign with inactive status", priority = 7, groups = {
			"regression", "dailyrun" })
	@Owner(name = "Amit Kumar")
	public void T4656_verifyArchivalOfMassOfferCampaignWithInactiveStatus()
			throws InterruptedException, ParseException {
		campaignName = "Automation-MassOffer_Campaign" + CreateDateTime.getTimeDateString();
		String dateTime = CreateDateTime.getCurrentDate() + " 11:00 PM";
		logger.info(campaignName + " mass offer campaign name is created");
		// Login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */

		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// navigate to cockpit -> earning
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Switch to new cam home page and set mass offer cam name
		pageObj.newCamHomePage().createMassCampaignCHP("Mass campaign", "Yes");
		// Set details
		pageObj.signupcampaignpage().createWhatDetailsMassCampaign(campaignName, dataSet.get("giftType"),
				dataSet.get("redemable"));
		pageObj.signupcampaignpage().createWhomDetailsMassCampaign(dataSet.get("audiance"), dataSet.get("segment"),
				dataSet.get("pushNotification"), dataSet.get("emailSubject"), dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().createWhenDetailsMassCampaign("Once", dateTime);

		// navigate to cockpit -> earning
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().searchCampaign(campaignName);
		String inactiveStatus = pageObj.newCamHomePage().deactivateCampaign();
		Assert.assertEquals(inactiveStatus, "Inactive", "Campaign deativated status did not matched");
		String archiveStatus = pageObj.newCamHomePage().archiveCampaign();
		Assert.assertEquals(archiveStatus, "Campaign(s) archived successfully",
				"Campaign archived message did not matched");
		pageObj.utils().logPass("successfully archived the campaign");
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() throws Exception {
		pageObj.utils().clearDataSet(dataSet);
		pageObj.utils().deleteCampaignFromDb(campaignName, env);
		logger.info("Data set cleared");
	}

	@AfterClass(alwaysRun = true)
	public void closeBrowser() {
		driver.quit();
		logger.info("Browser closed");
	}
}