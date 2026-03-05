package com.punchh.server.campaignsTest;

import java.lang.reflect.Method;
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

import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.ApiUtils;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
// just for internal use functinality no need to add in regression by: Shubham
public class ResetAllProcessedMassOfferCampaignsTest {

	private static Logger logger = LogManager.getLogger(ResetAllProcessedMassOfferCampaignsTest.class);
	public WebDriver driver;
	private Properties prop;
	private PageObj pageObj;
	private String userEmail;
	private String sTCName;
	private String env;
	private String baseUrl;
	private static Map<String, String> dataSet;
	String run = "ui";
	ApiUtils apiUtils;

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {

		prop = Utilities.loadPropertiesFile("config.properties");
		driver = new BrowserUtilities().launchBrowser();
		sTCName = method.getName();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		logger.info(sTCName + " ==>" + dataSet);

	}

	// Amit
	@Test(description = "SQ-T5726 Verify deletion of processed mass campaign when Reset All Processedis On for Mass Campaigns Single frequency", groups = {
			"regression", "dailyrun" }, priority = 0)
	public void T5726_verifyDeletionOfProcessedMassCampaignSingleFrequencyWhenResetAllProcessedisOn()
			throws InterruptedException {

		String massCampaignName = "AutomationMassOffer" + CreateDateTime.getTimeDateString();
		String dateTime = CreateDateTime.getTomorrowDate() + " 11:00 PM";

		// user creation using pos signup api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response respo = pageObj.endpoints().posSignUp(userEmail, dataSet.get("locationKey"),
				dataSet.get("favLocationId"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, respo.getStatusCode(), "Status code 200 did not matched for pos signup api");

		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();

		// set campaign name and gift type
		pageObj.signupcampaignpage().setCampaignName(massCampaignName);
		pageObj.signupcampaignpage().setGiftType(dataSet.get("giftType"));
		pageObj.signupcampaignpage().setFixedPoints(dataSet.get("fixedPoints"));
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().createWhomDetailsMassCampaign(dataSet.get("audiance"), dataSet.get("segment"),
				massCampaignName, dataSet.get("emailSubject"), dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().setFrequencyAndTimeInCampaign("Once", dateTime);
		pageObj.signupcampaignpage().scheduleCampaign();

		// run mass offer
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().findMassCampaignNameandRun(massCampaignName);

		// validate campaign status
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickNewCamHomePageBtn();
		String camStatus = pageObj.newCamHomePage().getAnyCampaignStatusWithPoolingNCHP(massCampaignName, "Processed");
		Assert.assertEquals(camStatus, "Processed", "Camapign status is not processed");
		pageObj.utils().logPass("Mass offer campaign processed successfully");

		// Disable Business Live Now? from Cockpit >>Dashboard >>Misc Config
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().navigateToTabs("Miscellaneous Config");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboard("Business Live Now?", "uncheck");
		pageObj.utils().logPass("Business Live Now? flag disabled");

		// Navigate to Cockpit >>Dashboard >>Scroll down to the bottom>>Click on
		// Delete/Reset business button
		pageObj.dashboardpage().navigateToTabs("Miscellaneous Config");
		pageObj.dashboardpage().deleteResetBusinessBtnClick();
		pageObj.dashboardpage().checkUncheckAnyFlagWitoutUpdate("Reset All Processed Mass Campaigns?", "check");
		String msg = pageObj.destructionPage().resetAllMassCampaigns(dataSet.get("slug"));
		Assert.assertEquals(msg, "Business will be reset shortly", "Business reset shortly message did not appeared");
		pageObj.utils().logPass("Business reset is successfull");

		// Validate processed mass offer campaign deleted
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		String camStatusDelete = pageObj.newCamHomePage().getCampaignAnyStatusNoPooling(massCampaignName);
		Assert.assertNull(camStatusDelete, "Camapign status is not null, campaign found and not deleted");
		pageObj.utils().logPass("Mass offer processed campaign  aftre business reset not found or deleted");
	}

	@Test(description = "SQ-T5726 Verify deletion of processed mass campaign when Reset All Processedis On for Mass Campaigns Daily frequency", groups = {
			"regression", "dailyrun" }, priority = 1)
	public void T5726_verifyDeletionOfProcessedMassCampaignDailyFrequencyWhenResetAllProcessedisOn()
			throws InterruptedException {

		String massCampaignName = "AutomationMassOffer" + CreateDateTime.getTimeDateString();
		String dateTime = CreateDateTime.getCurrentDate() + " 11:00 PM";

		// user creation using pos signup api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response respo = pageObj.endpoints().posSignUp(userEmail, dataSet.get("locationKey"),
				dataSet.get("favLocationId"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, respo.getStatusCode(), "Status code 200 did not matched for pos signup api");

		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();

		// set campaign name and gift type
		pageObj.signupcampaignpage().setCampaignName(massCampaignName);
		pageObj.signupcampaignpage().setGiftType(dataSet.get("giftType"));
		pageObj.signupcampaignpage().setFixedPoints(dataSet.get("fixedPoints"));
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().createWhomDetailsMassCampaign(dataSet.get("audiance"), dataSet.get("segment"),
				massCampaignName, dataSet.get("emailSubject"), dataSet.get("emailTemplate"));

		pageObj.signupcampaignpage().setFrequency("Daily");
		String startdateTime = CreateDateTime.getCurrentDate() + " 11:00 PM";
		String enddateTime = CreateDateTime.getFutureDate(1) + " 11:00 PM";
		pageObj.signupcampaignpage().setStartEndDateTime(startdateTime, enddateTime);

		// run mass offer
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().findMassCampaignNameandRun(massCampaignName);

		// validate campaign status
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickNewCamHomePageBtn();
		String camStatus = pageObj.newCamHomePage().getAnyCampaignStatusWithPoolingNCHP(massCampaignName, "Processed");
		Assert.assertEquals(camStatus, "Processed", "Camapign status is not processed");
		pageObj.utils().logPass("Mass offer campaign processed successfully");

		// Disable Business Live Now? from Cockpit >>Dashboard >>Misc Config
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().navigateToTabs("Miscellaneous Config");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboard("Business Live Now?", "uncheck");
		pageObj.utils().logPass("Business Live Now? flag disabled");

		// Navigate to Cockpit >>Dashboard >>Scroll down to the bottom>>Click on
		// Delete/Reset business button
		pageObj.dashboardpage().navigateToTabs("Miscellaneous Config");
		pageObj.dashboardpage().deleteResetBusinessBtnClick();
		pageObj.dashboardpage().checkUncheckAnyFlagWitoutUpdate("Reset All Processed Mass Campaigns?", "check");
		String msg = pageObj.destructionPage().resetAllMassCampaigns(dataSet.get("slug"));
		Assert.assertEquals(msg, "Business will be reset shortly", "Business reset shortly message did not appeared");
		pageObj.utils().logPass("Business reset is successfull");

		// Validate processed mass offer campaign deleted
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		String camStatusDelete = pageObj.newCamHomePage().getCampaignAnyStatusNoPooling(massCampaignName);
		Assert.assertNull(camStatusDelete, "Camapign status is not null, campaign found and not deleted");
		pageObj.utils().logPass("Mass offer processed campaign  aftre business reset not found or deleted");
	}

	@Test(description = "SQ-T5726 Verify deletion of processed mass notification campaign when Reset All Processedis On for Mass Campaigns Single frequency", groups = {
			"regression", "dailyrun" }, priority = 2)
	public void T5726_verifyDeletionOfProcessedMassNotificationCampaignSingleFrequencyWhenResetAllProcessedisOn()
			throws InterruptedException {
		// Precondition: segement and user is present Segement:Automation Mass Offer

		String massCampaignName = "Automation MassNotification Campaign" + CreateDateTime.getTimeDateString();
		String dateTime = CreateDateTime.getTomorrowDate() + " 11:00 PM";

		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value
		pageObj.campaignspage().selectMessagedrpValue(dataSet.get("messageDrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().setCampaignName(massCampaignName);
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().setAudienceType(dataSet.get("audiance"));
		pageObj.signupcampaignpage().setSegmentType(dataSet.get("segment"));
		pageObj.signupcampaignpage().setPushNotification(massCampaignName);
		pageObj.signupcampaignpage().setEmailSubject(dataSet.get("emailSubject"));
		pageObj.signupcampaignpage().setEmailTemplate(dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().clickNextButton();
		pageObj.signupcampaignpage().setFrequencyAndTimeInCampaign("Once", dateTime);
		pageObj.signupcampaignpage().scheduleCampaign();

		// run mass offer
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().findMassCampaignNameandRun(massCampaignName);

		// validate campaign status
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickNewCamHomePageBtn();
		String camStatus = pageObj.newCamHomePage().getAnyCampaignStatusWithPoolingNCHP(massCampaignName, "Processed");
		Assert.assertEquals(camStatus, "Processed", "Camapign status is not processed");
		pageObj.utils().logPass("Mass offer campaign processed successfully");

		// Disable Business Live Now? from Cockpit >>Dashboard >>Misc Config
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().navigateToTabs("Miscellaneous Config");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboard("Business Live Now?", "uncheck");
		pageObj.utils().logPass("Business Live Now? flag disabled");

		// Navigate to Cockpit >>Dashboard >>Scroll down to the bottom>>Click on
		// Delete/Reset business button
		pageObj.dashboardpage().navigateToTabs("Miscellaneous Config");
		pageObj.dashboardpage().deleteResetBusinessBtnClick();
		pageObj.dashboardpage().checkUncheckAnyFlagWitoutUpdate("Reset All Processed Mass Campaigns?", "check");
		String msg = pageObj.destructionPage().resetAllMassCampaigns(dataSet.get("slug"));
		Assert.assertEquals(msg, "Business will be reset shortly", "Business reset shortly message did not appeared");
		pageObj.utils().logPass("Business reset is successfull");

		// Validate processed mass offer campaign deleted
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		String camStatusDelete = pageObj.newCamHomePage().getCampaignAnyStatusNoPooling(massCampaignName);
		Assert.assertNull(camStatusDelete, "Camapign status is not null, campaign found and not deleted");
		pageObj.utils().logPass("Mass offer processed campaign  aftre business reset not found or deleted");
	}

	@Test(description = "SQ-T5726 Verify deletion of processed mass notification campaign when Reset All Processedis On for Mass Campaigns Daily frequency", groups = {
			"regression", "dailyrun" }, priority = 3)
	public void T5726_verifyDeletionOfProcessedMassNotificationCampaignDailyFrequencyWhenResetAllProcessedisOn()
			throws InterruptedException {
		// Precondition: segement and user is present Segement:Automation Mass Offer

		String massCampaignName = "Automation MassNotification Campaign" + CreateDateTime.getTimeDateString();
		String dateTime = CreateDateTime.getCurrentDate() + " 10:00 PM";

		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value
		pageObj.campaignspage().selectMessagedrpValue(dataSet.get("messageDrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().setCampaignName(massCampaignName);
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().setAudienceType(dataSet.get("audiance"));
		pageObj.signupcampaignpage().setSegmentType(dataSet.get("segment"));
		pageObj.signupcampaignpage().setPushNotification(massCampaignName);
		pageObj.signupcampaignpage().setEmailSubject(dataSet.get("emailSubject"));
		pageObj.signupcampaignpage().setEmailTemplate(dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().clickNextButton();
		pageObj.signupcampaignpage().setFrequency("Daily");
		String startdateTime = CreateDateTime.getCurrentDate() + " 11:00 PM";
		String enddateTime = CreateDateTime.getFutureDate(1) + " 11:00 PM";
		pageObj.signupcampaignpage().setStartEndDateTime(startdateTime, enddateTime);

		// run mass offer
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().findMassCampaignNameandRun(massCampaignName);

		// validate campaign status
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickNewCamHomePageBtn();
		String camStatus = pageObj.newCamHomePage().getAnyCampaignStatusWithPoolingNCHP(massCampaignName, "Processed");
		Assert.assertEquals(camStatus, "Processed", "Camapign status is not processed");
		pageObj.utils().logPass("Mass offer campaign processed successfully");

		// Disable Business Live Now? from Cockpit >>Dashboard >>Misc Config
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().navigateToTabs("Miscellaneous Config");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboard("Business Live Now?", "uncheck");
		pageObj.utils().logPass("Business Live Now? flag disabled");

		// Navigate to Cockpit >>Dashboard >>Scroll down to the bottom>>Click on
		// Delete/Reset business button
		pageObj.dashboardpage().navigateToTabs("Miscellaneous Config");
		pageObj.dashboardpage().deleteResetBusinessBtnClick();
		pageObj.dashboardpage().checkUncheckAnyFlagWitoutUpdate("Reset All Processed Mass Campaigns?", "check");
		String msg = pageObj.destructionPage().resetAllMassCampaigns(dataSet.get("slug"));
		Assert.assertEquals(msg, "Business will be reset shortly", "Business reset shortly message did not appeared");
		pageObj.utils().logPass("Business reset is successfull");

		// Validate processed mass offer campaign deleted
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		String camStatusDelete = pageObj.newCamHomePage().getCampaignAnyStatusNoPooling(massCampaignName);
		Assert.assertNull(camStatusDelete, "Camapign status is not null, campaign found and not deleted");
		pageObj.utils().logPass("Mass offer processed campaign  aftre business reset not found or deleted");

	}

	@Test(description = "SQ-T5694 Verify deletion of processed mass offer campaign when Reset All Processed is Off with Single frequency", groups = {
			"regression", "dailyrun" }, priority = 4)
	public void T5694_verifyDeletionOfProcessedMassOfferCampaignSingleFrequencyWhenResetAllProcessedisOff()
			throws InterruptedException {
		String massCampaignName = "AutomationMassOffer" + CreateDateTime.getTimeDateString();
		String dateTime = CreateDateTime.getTomorrowDate() + " 11:00 PM";

		// user creation using pos signup api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response respo = pageObj.endpoints().posSignUp(userEmail, dataSet.get("locationKey"),
				dataSet.get("favLocationId"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, respo.getStatusCode(), "Status code 200 did not matched for pos signup api");

		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();

		// set campaign name and gift type
		pageObj.signupcampaignpage().setCampaignName(massCampaignName);
		pageObj.signupcampaignpage().setGiftType(dataSet.get("giftType"));
		pageObj.signupcampaignpage().setFixedPoints(dataSet.get("fixedPoints"));
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().createWhomDetailsMassCampaign(dataSet.get("audiance"), dataSet.get("segment"),
				massCampaignName, dataSet.get("emailSubject"), dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().setFrequencyAndTimeInCampaign("Once", dateTime);
		pageObj.signupcampaignpage().scheduleCampaign();

		// run mass offer
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().findMassCampaignNameandRun(massCampaignName);

		// validate campaign status
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickNewCamHomePageBtn();
		String camStatus = pageObj.newCamHomePage().getAnyCampaignStatusWithPoolingNCHP(massCampaignName, "Processed");
		Assert.assertEquals(camStatus, "Processed", "Camapign status is not processed");
		pageObj.utils().logPass("Mass offer campaign processed successfully");

		// Disable Business Live Now? from Cockpit >>Dashboard >>Misc Config
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().navigateToTabs("Miscellaneous Config");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboard("Business Live Now?", "uncheck");
		pageObj.utils().logPass("Business Live Now? flag disabled");

		// Navigate to Cockpit >>Dashboard >>Scroll down to the bottom>>Click on
		// Delete/Reset business button
		pageObj.dashboardpage().navigateToTabs("Miscellaneous Config");
		pageObj.dashboardpage().deleteResetBusinessBtnClick();
		pageObj.dashboardpage().checkUncheckAnyFlagWitoutUpdate("Reset All Processed Mass Campaigns?", "uncheck");
		String msg = pageObj.destructionPage().resetAllMassCampaigns(dataSet.get("slug"));
		Assert.assertEquals(msg, "Business will be reset shortly", "Business reset shortly message did not appeared");
		pageObj.utils().logPass("Business reset is successfull");

		// Validate processed mass offer campaign deleted
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		String camStatusDelete = pageObj.newCamHomePage().getCampaignAnyStatusNoPooling(massCampaignName);
		Assert.assertTrue(camStatusDelete.equalsIgnoreCase("Processed"),
				"Camapign status is not processed or campaign is deleted");
		pageObj.utils().logPass("Mass offer processed campaign  aftre business reset found and not deleted");
	}

	@Test(description = "SQ-T5694 Verify deletion of processed mass offer campaign when Reset All Processed is Off for Mass Campaigns Daily frequency", groups = {
			"regression", "dailyrun" }, priority = 5)
	public void T5694_verifyDeletionOfProcessedMassCampaignDailyFrequencyWhenResetAllProcessedisOff()
			throws InterruptedException {

		String massCampaignName = "AutomationMassOffer" + CreateDateTime.getTimeDateString();
		String dateTime = CreateDateTime.getCurrentDate() + " 11:00 PM";

		// user creation using pos signup api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response respo = pageObj.endpoints().posSignUp(userEmail, dataSet.get("locationKey"),
				dataSet.get("favLocationId"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, respo.getStatusCode(), "Status code 200 did not matched for pos signup api");

		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();

		// set campaign name and gift type
		pageObj.signupcampaignpage().setCampaignName(massCampaignName);
		pageObj.signupcampaignpage().setGiftType(dataSet.get("giftType"));
		pageObj.signupcampaignpage().setFixedPoints(dataSet.get("fixedPoints"));
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().createWhomDetailsMassCampaign(dataSet.get("audiance"), dataSet.get("segment"),
				massCampaignName, dataSet.get("emailSubject"), dataSet.get("emailTemplate"));

		pageObj.signupcampaignpage().setFrequency("Daily");
		String startdateTime = CreateDateTime.getCurrentDate() + " 11:00 PM";
		String enddateTime = CreateDateTime.getFutureDate(1) + " 11:00 PM";
		pageObj.signupcampaignpage().setStartEndDateTime(startdateTime, enddateTime);

		// run mass offer
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().findMassCampaignNameandRun(massCampaignName);

		// validate campaign status
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickNewCamHomePageBtn();
		String camStatus = pageObj.newCamHomePage().getAnyCampaignStatusWithPoolingNCHP(massCampaignName, "Processed");
		Assert.assertEquals(camStatus, "Processed", "Camapign status is not processed");
		pageObj.utils().logPass("Mass offer campaign processed successfully");

		// Disable Business Live Now? from Cockpit >>Dashboard >>Misc Config
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().navigateToTabs("Miscellaneous Config");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboard("Business Live Now?", "uncheck");
		pageObj.utils().logPass("Business Live Now? flag disabled");

		// Navigate to Cockpit >>Dashboard >>Scroll down to the bottom>>Click on
		// Delete/Reset business button
		pageObj.dashboardpage().navigateToTabs("Miscellaneous Config");
		pageObj.dashboardpage().deleteResetBusinessBtnClick();
		pageObj.dashboardpage().checkUncheckAnyFlagWitoutUpdate("Reset All Processed Mass Campaigns?", "uncheck");
		String msg = pageObj.destructionPage().resetAllMassCampaigns(dataSet.get("slug"));
		Assert.assertEquals(msg, "Business will be reset shortly", "Business reset shortly message did not appeared");
		pageObj.utils().logPass("Business reset is successfull");

		// Validate processed mass offer campaign deleted
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		String camStatusDelete = pageObj.newCamHomePage().getCampaignAnyStatusNoPooling(massCampaignName);
		Assert.assertTrue(camStatusDelete.equalsIgnoreCase("Processed"),
				"Camapign status is not processed or campaign is deleted");
		pageObj.utils().logPass("Mass offer processed campaign  aftre business reset not found or deleted");
	}

	@Test(description = "SQ-T5694 Verify deletion of processed mass notification campaign when Reset All Processed is Off for Mass Campaigns Single frequency", groups = {
			"regression", "dailyrun" }, priority = 6)
	public void T5694_verifyDeletionOfProcessedMassNotificationCampaignSingleFrequencyWhenResetAllProcessedisOff()
			throws InterruptedException {
		// Precondition: segement and user is present Segement:Automation Mass Offer

		String massCampaignName = "Automation MassNotification Campaign" + CreateDateTime.getTimeDateString();
		String dateTime = CreateDateTime.getTomorrowDate() + " 11:00 PM";

		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value
		pageObj.campaignspage().selectMessagedrpValue(dataSet.get("messageDrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().setCampaignName(massCampaignName);
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().setAudienceType(dataSet.get("audiance"));
		pageObj.signupcampaignpage().setSegmentType(dataSet.get("segment"));
		pageObj.signupcampaignpage().setPushNotification(massCampaignName);
		pageObj.signupcampaignpage().setEmailSubject(dataSet.get("emailSubject"));
		pageObj.signupcampaignpage().setEmailTemplate(dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().clickNextButton();
		pageObj.signupcampaignpage().setFrequencyAndTimeInCampaign("Once", dateTime);
		pageObj.signupcampaignpage().scheduleCampaign();

		// run mass offer
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().findMassCampaignNameandRun(massCampaignName);

		// validate campaign status
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickNewCamHomePageBtn();
		String camStatus = pageObj.newCamHomePage().getAnyCampaignStatusWithPoolingNCHP(massCampaignName, "Processed");
		Assert.assertEquals(camStatus, "Processed", "Camapign status is not processed");
		pageObj.utils().logPass("Mass offer campaign processed successfully");

		// Disable Business Live Now? from Cockpit >>Dashboard >>Misc Config
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().navigateToTabs("Miscellaneous Config");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboard("Business Live Now?", "uncheck");
		pageObj.utils().logPass("Business Live Now? flag disabled");

		// Navigate to Cockpit >>Dashboard >>Scroll down to the bottom>>Click on
		// Delete/Reset business button
		pageObj.dashboardpage().navigateToTabs("Miscellaneous Config");
		pageObj.dashboardpage().deleteResetBusinessBtnClick();
		pageObj.dashboardpage().checkUncheckAnyFlagWitoutUpdate("Reset All Processed Mass Campaigns?", "uncheck");
		String msg = pageObj.destructionPage().resetAllMassCampaigns(dataSet.get("slug"));
		Assert.assertEquals(msg, "Business will be reset shortly", "Business reset shortly message did not appeared");
		pageObj.utils().logPass("Business reset is successfull");

		// Validate processed mass offer campaign deleted
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		String camStatusDelete = pageObj.newCamHomePage().getCampaignAnyStatusNoPooling(massCampaignName);
		Assert.assertTrue(camStatusDelete.equalsIgnoreCase("Processed"),
				"Camapign status is not processed or campaign is deleted");
		pageObj.utils().logPass("Mass offer processed campaign  aftre business reset not found or deleted");
	}

	@Test(description = "SQ-T5694 Verify deletion of processed mass notification campaign when Reset All Processed is Off for Mass Campaigns Daily frequency", groups = {
			"regression", "dailyrun" }, priority = 7)
	public void T5694_verifyDeletionOfProcessedMassNotificationCampaignDailyFrequencyWhenResetAllProcessedisOff()
			throws InterruptedException {
		// Precondition: segement and user is present Segement:Automation Mass Offer

		String massCampaignName = "Automation MassNotification Campaign" + CreateDateTime.getTimeDateString();

		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value
		pageObj.campaignspage().selectMessagedrpValue(dataSet.get("messageDrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().setCampaignName(massCampaignName);
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().setAudienceType(dataSet.get("audiance"));
		pageObj.signupcampaignpage().setSegmentType(dataSet.get("segment"));
		pageObj.signupcampaignpage().setPushNotification(massCampaignName);
		pageObj.signupcampaignpage().setEmailSubject(dataSet.get("emailSubject"));
		pageObj.signupcampaignpage().setEmailTemplate(dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().clickNextButton();
		pageObj.signupcampaignpage().setFrequency("Daily");
		String startdateTime = CreateDateTime.getCurrentDate() + " 11:00 PM";
		String enddateTime = CreateDateTime.getFutureDate(1) + " 11:00 PM";
		pageObj.signupcampaignpage().setStartEndDateTime(startdateTime, enddateTime);

		// run mass offer
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().findMassCampaignNameandRun(massCampaignName);

		// validate campaign status
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickNewCamHomePageBtn();
		String camStatus = pageObj.newCamHomePage().getAnyCampaignStatusWithPoolingNCHP(massCampaignName, "Processed");
		Assert.assertEquals(camStatus, "Processed", "Camapign status is not processed");
		pageObj.utils().logPass("Mass offer campaign processed successfully");

		// Disable Business Live Now? from Cockpit >>Dashboard >>Misc Config
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().navigateToTabs("Miscellaneous Config");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboard("Business Live Now?", "uncheck");
		pageObj.utils().logPass("Business Live Now? flag disabled");

		// Navigate to Cockpit >>Dashboard >>Scroll down to the bottom>>Click on
		// Delete/Reset business button
		pageObj.dashboardpage().navigateToTabs("Miscellaneous Config");
		pageObj.dashboardpage().deleteResetBusinessBtnClick();
		pageObj.dashboardpage().checkUncheckAnyFlagWitoutUpdate("Reset All Processed Mass Campaigns?", "uncheck");
		String msg = pageObj.destructionPage().resetAllMassCampaigns(dataSet.get("slug"));
		Assert.assertEquals(msg, "Business will be reset shortly", "Business reset shortly message did not appeared");
		pageObj.utils().logPass("Business reset is successfull");

		// Validate processed mass offer campaign deleted
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		String camStatusDelete = pageObj.newCamHomePage().getCampaignAnyStatusNoPooling(massCampaignName);
		Assert.assertTrue(camStatusDelete.equalsIgnoreCase("Processed"),
				"Camapign status is not processed or campaign is deleted");
		pageObj.utils().logPass("Mass offer processed campaign  aftre business reset not found or deleted");

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}
}
