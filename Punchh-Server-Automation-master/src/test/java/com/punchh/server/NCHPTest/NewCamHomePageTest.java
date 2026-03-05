package com.punchh.server.NCHPTest;

import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;
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
import com.punchh.server.utilities.Utilities;

// Author: Amit
@Listeners(TestListeners.class)
public class NewCamHomePageTest {

	private static Logger logger = LogManager.getLogger(NewCamHomePageTest.class);
	public WebDriver driver;
	private Properties prop;
	private PageObj pageObj;
	private String sTCName;
	private String env;
	private String baseUrl;
	private static Map<String, String> dataSet;
	String run = "ui";
	private Utilities utils;

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
		utils = new Utilities(driver);
	}

// Amit
	@Test(description = "SQ-T4316, SQ-T4318 Verify functionality of Deactivate Delete button on new campaign home page", priority = 0, groups = {
			"regression", "dailyrun" })
	@Owner(name = "Amit Kumar")
	public void T4316_verifyFunctionalityOfDeactivateDeleteButtonOnNewCampaignHomePage() throws InterruptedException {

		String campaignName = "AutomationPostCheckinCampaign_" + CreateDateTime.getTimeDateString();
		logger.info(campaignName + " post checking campaign name is generated");
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
		// Select offer dropdown value
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();
		// set cam name and reward details
		pageObj.signupcampaignpage().setCampaignName(campaignName);
		pageObj.signupcampaignpage().setGiftType(dataSet.get("giftType"));
		pageObj.signupcampaignpage().setGiftReason("campaignName");
		pageObj.signupcampaignpage().setGiftPoints(dataSet.get("points"));
		pageObj.signupcampaignpage().clickNextBtn();
		// set PN email subject and activate cam
		pageObj.signupcampaignpage().setPostCheckinPushNotification(dataSet.get("pushNotification"));
		pageObj.signupcampaignpage().setPostCheckinPushEmailSubject(dataSet.get("emailSubject"));
		pageObj.signupcampaignpage().setPostCheckinEmailTemplate(dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().clickNextBtn();
		pageObj.signupcampaignpage().setStartDate();
		pageObj.signupcampaignpage().createWhenDetailsCampaign();
		// Switch to new cam home page
		pageObj.newCamHomePage().clickNewCamHomePageBtn();
		pageObj.newCamHomePage().searchCampaign(campaignName);
		String inactiveStatus = pageObj.newCamHomePage().deactivateCampaign();
		String activeStatus = pageObj.newCamHomePage().activateCampaign();

		Assert.assertEquals(inactiveStatus, "Inactive", "Campaign deativated status did not matched");
		Assert.assertEquals(activeStatus, "Active", "Campaign ativated status did not matched");
		// delete created campaign
		pageObj.newCamHomePage().deleteCampaign(campaignName);

	}

	// Amit
	@Test(description = "SQ-T4313, SQ-T4414, SQ-T4589, SQ-T4654,SQ-T4783, SQ-T4796, SQ-T4797 Verify creation of mass offer campaign from new CHP", groups = {
			"regression" }, priority = 1)
	@Owner(name = "Amit Kumar")
	public void T4313_verifyCreationOfMassOfferCampaignFromNewCHP() throws InterruptedException, ParseException {
		// campaign name with -_ special charactters
		String campaignName = "Automation-MassOffer_Campaign" + CreateDateTime.getTimeDateString();
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

		// disable STO is from cockpit
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().offEnableSTOCheckbox();
		// navigate to cockpit -> earning
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// pageObj.newCamHomePage().clickSwitchToClassicBtn();
		// Switch to new cam home page and set mass offer cam name
		pageObj.newCamHomePage().createMassCampaignCHP("Mass campaign", "Yes");
		// Set details
		pageObj.signupcampaignpage().createWhatDetailsMassCampaign(campaignName, dataSet.get("giftType"),
				dataSet.get("redemable"));
		pageObj.signupcampaignpage().createWhomDetailsMassCampaign(dataSet.get("audiance"), dataSet.get("segment"),
				dataSet.get("pushNotification"), dataSet.get("emailSubject"), dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().createWhenDetailsMassCampaign("Once", dateTime);

		// check status before running Schedule
		pageObj.newCamHomePage().searchCampaign(campaignName);
		boolean archiveStatus = pageObj.newCamHomePage().checkAchivalpresence(false);
		Assert.assertFalse(archiveStatus, "Archive option status False did not matched");
		utils.logPass("Verified archive button not present when campaign is in Schedule status");

		// disable Segmentation via s3 flow
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Campaigns");
		pageObj.dashboardpage().checkUncheckAnyFlag("Segmentation via s3 flow", "uncheck");

		// run mass offer
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType("Mass Campaign Schedules");
		pageObj.schedulespage().findMassCampaignNameandRun(campaignName);

		// navigate to cockpit -> earning
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		// check status after running Schedule
		// pageObj.newCamHomePage().searchCampaign(campaignName);
		String camStatus = pageObj.newCamHomePage().getAnyCampaignStatusWithPoolingNCHP(campaignName, "Processed");
		Assert.assertEquals(camStatus, "Processed", "Camapign status is not processed");
		boolean archiveStatus2 = pageObj.newCamHomePage().checkAchivalpresence(true);
		Assert.assertTrue(archiveStatus2, "Archive option status true did not matched");
		utils.logPass("Verified archive button is present when campaign is in processed status");

		pageObj.newCamHomePage().viewCampaignSidePanel();
		String camid = pageObj.newCamHomePage().getSidePanelCampaignId();
		String val = pageObj.newCamHomePage().getSidePanelCampaignDetails();
		Assert.assertTrue(val.contains(campaignName), "Campaign name did not matched on side panel");
		Assert.assertTrue(val.contains("Mass (Once)"), "Campaign type did not matched on side panel");
		Assert.assertTrue(val.contains("Campaign time"), "Campaign time zone not showing on side panel");
		Assert.assertTrue(val.contains("Starts on"), "Campaign time zone not showing on side panel");
		Assert.assertNotNull(camid, "Campaign id not showing on side panel");
		pageObj.newCamHomePage().viewCampaignSummary(); // view summary from side panel
		pageObj.campaignspage().selectCPPOptions(dataSet.get("optionVal"));
		String status = pageObj.campaignspage().campaignStatusThroughStatusTracker(campaignName);
		Assert.assertEquals(status, "Processed", "Campaign status did not matched on status tracker page");

	}

	// Amit
	@Test(description = "SQ-T4314, SQ-T4415, SQ-T4655 Verify creation of mass notification campaign from new CHP", priority = 2, groups = {
			"regression", "unstable" })
	@Owner(name = "Amit Kumar")
	public void T4314_verifyCreationOfMassNotificationCampaignFromNewCHP() throws InterruptedException, ParseException {
		String campaignName = "AutomationMassNotificationCampaign" + CreateDateTime.getTimeDateString();
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
		// pageObj.newCamHomePage().clickSwitchToClassicBtn();
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

		// check status before running Schedule
		pageObj.newCamHomePage().searchCampaign(campaignName);
		boolean archiveStatus1 = pageObj.newCamHomePage().checkAchivalpresence(false);
		Assert.assertFalse(archiveStatus1, "Archive option status False did not matched");
		utils.logPass("Verified archive button not present when campaign is in Schedule status");

		// disable Segmentation via s3 flow
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Campaigns");
		pageObj.dashboardpage().checkUncheckAnyFlag("Segmentation via s3 flow", "uncheck");

		// run mass offer schedule
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType("Mass Campaign Schedules");
		pageObj.schedulespage().findMassCampaignNameandRun(campaignName);
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		utils.longWaitInSeconds(8);
		// check status after running Schedule
		pageObj.newCamHomePage().searchCampaign(campaignName);
		boolean archiveStatus2 = pageObj.newCamHomePage().checkAchivalpresence(true);
		Assert.assertTrue(archiveStatus2, "Archive option status true did not matched");
		utils.logPass("Verified archive button is present when campaign is in processed status");

		pageObj.newCamHomePage().viewCampaignSummary();
		pageObj.campaignspage().selectCPPOptions(dataSet.get("optionValue"));
		String status = pageObj.campaignspage().campaignStatusThroughStatusTracker(campaignName);
		Assert.assertEquals(status, "Processed", "Campaign status did not matched on status tracker page");
	}

//Amit
	@Test(description = "SQ-T4315, SQ-T4788, SQ-T4798, SQ-T4791, SQ-T4772, SQ-T5140 Verify creation of automation campaign Post Checkin from new CHP", priority = 3, groups = {
			"regression", "dailyrun" })
	@Owner(name = "Amit Kumar")
	public void T4315_verifyCreationOfAutomationCampaignPostCheckinFromNewCHP() throws InterruptedException {

		String campaignName = "AutomationPostCheckinCampaign" + CreateDateTime.getTimeDateString();
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
		pageObj.signupcampaignpage().createWhomDetailsPostcheckinCampaign(dataSet.get("pushNotification"),
				dataSet.get("emailSubject"), dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().createWhenDetailsCampaign();
		// Search created campaihn in new cam home page and delete
		pageObj.newCamHomePage().selectCampaignsTab("Automations");
		pageObj.newCamHomePage().searchCampaign(campaignName);
		String time = pageObj.newCamHomePage().getCamStartTimeFromTable();
		pageObj.newCamHomePage().viewCampaignSidePanel();
		String camid = pageObj.newCamHomePage().getSidePanelCampaignId();
		String val = pageObj.newCamHomePage().getSidePanelCampaignDetails();
		Assert.assertFalse(time.contains("(IST)"), "Campaign timezone showing in table. it should not show");
		Assert.assertTrue(val.contains(campaignName), "Campaign name did not matched on side panel");
		Assert.assertTrue(val.contains("Post-Checkin"), "Campaign type did not matched on side panel");
		Assert.assertTrue(val.contains("No end date"), "Campaign type did not matched on side panel");
		Assert.assertNotNull(camid, "Campaign id not showing on side panel");
		pageObj.newCamHomePage().deleteCampaignFromStickyBar(); // black sticky bar on bottom after selecting any cam
	}

//Amit  run manually in every regression(can be run with sto on to keep this under processing for smoetime)
	@Test(description = "SQ-T4317,SQ-T4822 Verify Stop processing campaign on new campaign home page", groups = {
			"regression", "unstable", "dailyrun" }, priority = 4)
	@Owner(name = "Amit Kumar")
	public void T4317_verifyStopProcessingCampaignOnNewCampaignHomePage() throws InterruptedException, ParseException {
		String campaignName = "AutomationMassOfferCampaign" + CreateDateTime.getTimeDateString();
		String dateTime = CreateDateTime.getTomorrowDate() + " 11:00 PM";
		logger.info(campaignName + " campaign name is created");
		// Login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */

		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// Click Cockpit and enable STO campaign
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().onEnableSTOCheckbox();
		// navigate to cockpit -> earning
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		// Switch to new cam home page and set mass offer cam name
		pageObj.newCamHomePage().createMassCampaignCHP("Mass campaign", "Yes");

		pageObj.signupcampaignpage().createWhatDetailsMassCampaign(campaignName, dataSet.get("giftType"),
				dataSet.get("redemable"));
		pageObj.signupcampaignpage().createWhomDetailsMassCampaign(dataSet.get("audiance"), dataSet.get("segment"),
				dataSet.get("pushNotification"), dataSet.get("emailSubject"), dataSet.get("emailTemplate"));
		// pageObj.signupcampaignpage().createWhenDetailsMassCampaign("Once", dateTime);
		pageObj.signupcampaignpage().setFrequencyAndTimeInCampaign("Once", dateTime);
		pageObj.signupcampaignpage().setStoOn();
		pageObj.signupcampaignpage().scheduleCampaign();
		TestListeners.extentTest.get().pass("Mass Offer Campaign from NCHP created successfully");
		pageObj.utils().waitTillPagePaceDone();

		// Run cam schedule
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType("Mass Campaign Schedules");
		pageObj.schedulespage().findMassCampaignNameandRun(campaignName);
		// Search created campaihn in new cam home page and stop processing
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().searchCampaign(campaignName);
		String stoppedStatus = pageObj.newCamHomePage().stopProcessingCampaign();
		Assert.assertEquals(stoppedStatus, "Stopped", "Campaign Stopped status did not matched");

	}

	// Amit
	@Test(description = "SQ-T4383, SQ-T4422 Verify the creation deletion of tags from manage tag button on new Campaign home page", priority = 5, groups = {
			"regression", "dailyrun" })
	@Owner(name = "Amit Kumar")
	public void T4383_verifyCreationDeletionOfTagsFromManageTagButtonOnNewCampaignHomePage()
			throws InterruptedException {

		String tagName = "AutoTag" + CreateDateTime.getTimeDateString();

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
		// pageObj.newCamHomePage().clickNewCamHomePageBtn();
		String val = pageObj.newCamHomePage().createTags(tagName);
		String deleteval = pageObj.newCamHomePage().deleteTag(tagName);
		Assert.assertEquals(val, "Tag name is limited to 50 characters.",
				"Tag name range error message did not matched");
		Assert.assertEquals(deleteval, "Tag deleted successfully", "Campaign Stopped status did not matched");

	}

	// Amit
	@Test(description = "SQ-T4386 SQ-T4757 Verify creation of Promo campaign from new CHP", priority = 6)
	@Owner(name = "Amit Kumar")
	public void T4386_verifyCreationOfPromoCampaignFromNewCHP() throws InterruptedException, ParseException {

		String campaignName = "AutomationPoromoCampaign" + CreateDateTime.getTimeDateString();
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

		// Search created campaihn in new cam home page and delete
		pageObj.newCamHomePage().searchCampaign(campaignName);
		// validate campaign options
		pageObj.newCamHomePage().searchCampaign(campaignName);
		pageObj.newCamHomePage().selectCampaignOption("Delete");
		pageObj.newCamHomePage().closeOptionsDailog();
		pageObj.newCamHomePage().selectCampaignOption("Deactivate");
		pageObj.newCamHomePage().closeOptionsDailog();
		pageObj.newCamHomePage().selectCampaignOption("Tag");
		pageObj.newCamHomePage().closeOptionsDailog();

		pageObj.newCamHomePage().selectCampaignOption("Export codes");
		String val = pageObj.newCamHomePage().getExportCodeSuccessMsg();
		Assert.assertEquals(val, "Your promo codes list is on its way", "Export code success message did not matched");

		pageObj.newCamHomePage().selectCampaignOption("Duplicate");
		String duplicateTitle = pageObj.newCamHomePage().getNewPageTitleClassic();
		pageObj.newCamHomePage().navigateToBackPage();
		Assert.assertEquals(duplicateTitle, "New Promo Campaign", "Duplicate option page title did not matched");

		pageObj.newCamHomePage().selectCampaignOption("Edit");
		String editTitle = pageObj.newCamHomePage().getNewPageTitleClassic();
		pageObj.newCamHomePage().navigateToBackPage();
		Assert.assertEquals(editTitle, "Edit " + campaignName, "Edit option page title did not matched");

		pageObj.newCamHomePage().selectCampaignOption("Audit log");
		String auditLogTitle = pageObj.newCamHomePage().getNewPageTitleClassic();
		pageObj.newCamHomePage().navigateToBackPage();
		Assert.assertTrue(auditLogTitle.contains("Audit Logs for"), "Audit Log option page title did not matched");

		pageObj.newCamHomePage().selectCampaignOption("Promo codes list");
		String couponCodesListTitle = pageObj.newCamHomePage().getNewPageTitleClassic();
		pageObj.newCamHomePage().navigateToBackPage();
		Assert.assertTrue(couponCodesListTitle.contains(campaignName),
				"Promo codes list option page title did not matched");

		pageObj.newCamHomePage().selectCampaignOption("View summary");
		String campaignSummaryTitle = pageObj.newCamHomePage().getNewPageTitleCPP();
		pageObj.newCamHomePage().navigateToBackPage();
		Assert.assertEquals(campaignSummaryTitle, "Campaign Summary", "View summary option page title did not matched");
		// Delete campaign
		pageObj.newCamHomePage().deleteCampaign(campaignName);
	}

	// Amit
	@Test(description = "SQ-T4387, SQ-T4757 Verify creation of Coupon campaign from new CHP", priority = 7, groups = {
			"unstable" })
	@Owner(name = "Amit Kumar")
	public void T4387_verifyCreationOfCouponCampaignFromNewCHP() throws InterruptedException, ParseException {

		String campaignName = "AutomationCouponCampaign" + CreateDateTime.getTimeDateString();
		logger.info("Coupon campaign name is created :" + campaignName);
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
		pageObj.newCamHomePage().createOtherCampaignCHP("Other", "Coupon");
		// create coupon campaign
		pageObj.signupcampaignpage().createWhatDetailsCoupanCampaign(campaignName);
		pageObj.signupcampaignpage().setCouponCampaignUsageType(dataSet.get("usageType"));
		pageObj.signupcampaignpage().setCouponCampaignCodeGenerationType(dataSet.get("codeType"));
		pageObj.signupcampaignpage().setCouponCampaignGuestUsagewithGiftAmount(dataSet.get("noOfGuests"),
				dataSet.get("usagePerGuest"), dataSet.get("giftType"), dataSet.get("amount"), "", "", false);
		pageObj.signupcampaignpage().setStartDate();
		pageObj.signupcampaignpage().setEndDateforCouponCampaign(1);
		pageObj.signupcampaignpage().createWhenDetailsCampaign();
		// Search created campaihn in new cam home page and delete
		// validate campaign options
		pageObj.newCamHomePage().searchCampaign(campaignName);
		pageObj.newCamHomePage().selectCampaignOption("Delete");
		pageObj.newCamHomePage().closeOptionsDailog();
		pageObj.newCamHomePage().selectCampaignOption("Deactivate");
		pageObj.newCamHomePage().closeOptionsDailog();
		pageObj.newCamHomePage().selectCampaignOption("Tag");
		pageObj.newCamHomePage().closeOptionsDailog();

		pageObj.newCamHomePage().selectCampaignOption("Export codes");
		String val = pageObj.newCamHomePage().getExportCodeSuccessMsg();
		Assert.assertEquals(val, "Your coupon codes list is on its way", "Export code success message did not matched");

		pageObj.newCamHomePage().selectCampaignOption("Duplicate");
		String duplicateTitle = pageObj.newCamHomePage().getNewPageTitleClassic();
		pageObj.newCamHomePage().navigateToBackPage();
		Assert.assertEquals(duplicateTitle, "New Coupon Campaign", "Duplicate option page title did not matched");

		pageObj.newCamHomePage().selectCampaignOption("Edit");
		String editTitle = pageObj.newCamHomePage().getNewPageTitleClassic();
		pageObj.newCamHomePage().navigateToBackPage();
		Assert.assertEquals(editTitle, "Edit " + campaignName, "Edit option page title did not matched");

		pageObj.newCamHomePage().selectCampaignOption("Audit log");
		String auditLogTitle = pageObj.newCamHomePage().getNewPageTitleClassic();
		pageObj.newCamHomePage().navigateToBackPage();
		Assert.assertTrue(auditLogTitle.contains("Audit Logs for"), "Audit Log option page title did not matched");

		pageObj.newCamHomePage().selectCampaignOption("Coupon codes list");
		String couponCodesListTitle = pageObj.newCamHomePage().getNewPageTitleClassic();
		pageObj.newCamHomePage().navigateToBackPage();
		Assert.assertEquals(couponCodesListTitle, campaignName, "Coupon codes list option page title did not matched");

		pageObj.newCamHomePage().selectCampaignOption("View summary");
		String campaignSummaryTitle = pageObj.newCamHomePage().getNewPageTitleCPP();
		pageObj.newCamHomePage().navigateToBackPage();
		Assert.assertEquals(campaignSummaryTitle, "Campaign Summary", "View summary option page title did not matched");
		// Delete campaign
		pageObj.newCamHomePage().deleteCampaign(campaignName);
	}

	@Test(description = "SQ-T4413 Verify creation of Checkin survey campaign from new CHP", priority = 9, groups = {
			"regression", "dailyrun" })
	@Owner(name = "Amit Kumar")
	public void T4413_verifyCreationOfCheckinSurveyCampaignFromNewCHP() throws InterruptedException, ParseException {

		String campaignName = "AutomationCSuerveyCampaign" + CreateDateTime.getTimeDateString();
		logger.info("Checkin Survey campaign name is created :" + campaignName);
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
		pageObj.newCamHomePage().createOtherCampaignCHP("Other", "Checkin Survey");

		// create checkin Surevy campaign
		pageObj.signupcampaignpage().setCampaignName(campaignName);
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().setLocationApplicability("Automation - 1");
		pageObj.signupcampaignpage().setPriority("1");
		pageObj.signupcampaignpage().setSurvey("Automation Servey");
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().createWhenDetailsCampaign();

		// Search created campaihn in new cam home page and delete
		pageObj.newCamHomePage().searchCampaign(campaignName);
		pageObj.newCamHomePage().deleteCampaign(campaignName);
	}

	@Test(description = "SQ-T4588 Verify creation of Social cause campaign from new CHP", priority = 10, groups = {
			"regression", "dailyrun" })
	@Owner(name = "Amit Kumar")
	public void T4588_verifyCreationOfSocialCauseCampaignFromNewCHP() throws InterruptedException, ParseException {
		String campaignName = "AutomationSocialCauseCampaign" + CreateDateTime.getTimeDateString();
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
		pageObj.signupcampaignpage().setPushNotification(dataSet.get("pushNotification"));
		pageObj.signupcampaignpage().setEmailSubject(dataSet.get("emailSubject"));
		pageObj.signupcampaignpage().setEmailTemplate(dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().clickNextButton();
		pageObj.signupcampaignpage().setStartDate();
		pageObj.signupcampaignpage().createWhenDetailsCampaign();
		// Search created campaihn in new cam home page and delete
		pageObj.newCamHomePage().searchCampaign(campaignName);
		pageObj.newCamHomePage().deleteCampaign(campaignName);
	}

	// Amit
	@Test(description = "SQ-T4811, SQ-T5139 Verify the campaign list on new CHP", priority = 11, groups = {
			"regression", "dailyrun" })
	@Owner(name = "Amit Kumar")
	public void T4811_verifyCampaignListOnNewCHP() throws InterruptedException, ParseException {
		List<String> automationsCampaigns = Arrays.asList("Post-checkin", "Post-redemption",
				"Post-purchase (gift card)", "Anniversary", "Signup", "Referral", "Profile Update", "Recall",
				"Compression");
		List<String> otherCampaigns = Arrays.asList("Promo", "Coupon", "Challenge", "Social Cause", "A/B Testing",
				"Checkin Survey", "Bounce Back", "Location Presence");
		List<String> allOptions = Arrays.asList("A/B Testing", "Anniversary", "Bounce Back", "Challenge",
				"Checkin Survey", "Compression", "Coupon", "Location Presence", "Mass (One-time)", "Mass (Repeating)",
				"Post-checkin", "Post-purchase (gift card)", "Post-redemption", "Profile Update", "Promo", "Recall",
				"Referral", "Signup", "Social Cause");
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
		// Mass tab values
		List<String> massList = pageObj.newCamHomePage().getCampaignType("Mass");
		boolean status = massList.stream().allMatch("Mass"::equals);
		Assert.assertTrue(status, "Other campaign type found in mass tab NCHP");
		TestListeners.extentTest.get().pass("Mass tab campaigns type validated");

		// automations tab values
		List<String> automationsList = pageObj.newCamHomePage().getCampaignType("Automations");
		System.out.println(automationsList);
		boolean automationsStatus = automationsCampaigns.containsAll(automationsList);
		Assert.assertTrue(automationsStatus,
				"Some Other campaign type found or name not matched in Automations tab  NCHP");
		TestListeners.extentTest.get().pass("Automations tab campaigns type validated");

		// Other tab values
		List<String> othersList = pageObj.newCamHomePage().getCampaignType("Other");
		System.out.println(othersList);
		boolean othersStatus = otherCampaigns.containsAll(othersList);
		Assert.assertTrue(othersStatus, "Some Other campaign type found or name not matched in Other tab NCHP");
		TestListeners.extentTest.get().pass("Other tab campaigns type validated");

		// More filters Type with All
		List<String> optionsList = pageObj.newCamHomePage().getCampaignTypeDrpValues("All", "Type");
		System.out.println(optionsList);
		boolean optionsStatus = allOptions.containsAll(optionsList);
		Assert.assertTrue(allOptions.size() == optionsList.size(), "Optins count did not matched");
		Assert.assertTrue(optionsStatus, "Some Other campaign type found or name not matched in Other tab NCHP");
		TestListeners.extentTest.get().pass("Other tab campaigns type validated");

	}

	// Anant
	@Test(description = "SQ-T439 Verify appearance of new CHP when flag from cockpit is disable", groups = {
			"regression", "unstable", "dailyrun" }, priority = 12)
	@Owner(name = "Amit Kumar")
	public void T439_verifyAppearanceWhenFlagDisable() throws InterruptedException {
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */

		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// enable the flag
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().navigateToTabs("Bento Apps");
		pageObj.dashboardpage().checkUncheckAnyFlag("Enable Campaign Management Tool?", "check");

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// pageObj.newCamHomePage().clickSwitchToClassicBtn();

		boolean elementPresent = pageObj.campaignspage().tryNewCampaignBtnVisible();
		Assert.assertTrue(elementPresent, "try new campaign button is not visible when flag is enable");
		utils.logPass("try new campaign button is visible when flag is enable");

		// disable the flag
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().navigateToTabs("Bento Apps");
		pageObj.dashboardpage().checkUncheckAnyFlag("Enable Campaign Management Tool?", "uncheck");

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		elementPresent = pageObj.campaignspage().tryNewCampaignBtnVisible();
		Assert.assertFalse(elementPresent, "try new campaign button is visible when flag is disable");
		utils.logPass("try new campaign button is not visible when flag is disable");

		// enable nchp flag
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().navigateToTabs("Bento Apps");
		pageObj.dashboardpage().checkUncheckAnyFlag("Enable Campaign Management Tool?", "check");

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