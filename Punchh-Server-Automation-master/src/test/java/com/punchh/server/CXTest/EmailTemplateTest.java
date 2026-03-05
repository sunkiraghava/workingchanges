package com.punchh.server.CXTest;

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

import com.punchh.server.annotations.Owner;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class EmailTemplateTest {

	private static Logger logger = LogManager.getLogger(EmailTemplateTest.class);
	public WebDriver driver;
	private Properties prop;
	private PageObj pageObj;
	private String userEmail;
	private String timstamp;
	private String sTCName;
	private String env, run = "ui";
	private String baseUrl;
	private static Map<String, String> dataSet;
	SeleniumUtilities selUtils;
	private Utilities utils;

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
		timstamp = CreateDateTime.getTimeDateString();
		prop = Utilities.loadPropertiesFile("config.properties");
		driver = new BrowserUtilities().launchBrowser();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		sTCName = method.getName();
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		logger.info(sTCName + " ==>" + dataSet);
		selUtils = new SeleniumUtilities(driver);
		utils = new Utilities(driver);
	}

	@Test(description = "SQ-T2686 Create new email template and publish(admin user) || SQ-T2688, Edit created email template || SQ-T2689, Delete created email template || SQ-T2690, Duplicate created email template", groups = "Regression", priority = 0)
	@Owner(name = "Hardik Bhardwaj")
	public void T2686_CreateEditDeleteDuplicateEmailtemplete() throws InterruptedException {
		String emailTemplateName = "AutoEmailTemplatetemp";
		emailTemplateName = emailTemplateName.replace("temp", timstamp);
		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// navigate to email templates
		pageObj.menupage().navigateToSubMenuItem("Settings", "Email Templates");
		// create email templates
		pageObj.emailTemplatePage().createNewEmailTemplate(emailTemplateName);
		// pageObj.emailTemplatePage().addMediaToTemplate();
		pageObj.emailTemplatePage().addContentToTemplate();
		pageObj.emailTemplatePage().addButtonToTemplate();
		pageObj.emailTemplatePage().addSocialToTemplate();
		pageObj.emailTemplatePage().addDividerToTemplate();
		pageObj.emailTemplatePage().addHTMLToTemplate();
		pageObj.emailTemplatePage().addSubjectToTemplate();
		pageObj.emailTemplatePage().saveAndPublish(emailTemplateName);
		utils.waitTillSpinnerDisappear(10);
		// edit email templates with no update
		pageObj.emailTemplatePage().editTemplateSaveAndDraft(emailTemplateName);
		pageObj.emailTemplatePage().saveAndPublish1(emailTemplateName);
		utils.waitTillSpinnerDisappear(10);
		// edit email templates with update
		pageObj.emailTemplatePage().editTemplate(emailTemplateName);
		pageObj.emailTemplatePage().addVideoToTemplate(dataSet.get("youtubeVideoLink"));
		pageObj.emailTemplatePage().saveAndPublish(emailTemplateName);
		utils.waitTillSpinnerDisappear(10);
		// create duplicate email templates
		pageObj.emailTemplatePage().createDuplicateTemplate(emailTemplateName);
		// delete email templates
		pageObj.emailTemplatePage().deleteTemplate(emailTemplateName);
		selUtils.implicitWait(50);
	}

	@Test(description = "SQ-T2683 (1.0) Create a new email template and save as draft || SQ-T2684 Edit and create Email template from draft || SQ-T2685 (1.0) Delete email template from draft", groups = "Regression", priority = 1)
	@Owner(name = "Hardik Bhardwaj")
	public void T2683_CreateTemplateDraft() throws InterruptedException {
		String emailTemplateName = "AutoEmailTemplatetemp";
		emailTemplateName = emailTemplateName.replace("temp", timstamp);
		// navigate to user timeline
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// navigate to email templates
		pageObj.menupage().navigateToSubMenuItem("Settings", "Email Templates");
		// edit email templates with no update
		pageObj.emailTemplatePage().createNewEmailTemplate(emailTemplateName);
		pageObj.emailTemplatePage().addMediaToTemplate();
		pageObj.emailTemplatePage().addContentToTemplate();
		pageObj.emailTemplatePage().addButtonToTemplate();
		pageObj.emailTemplatePage().addSocialToTemplate();
		pageObj.emailTemplatePage().saveAsDraft(emailTemplateName);
		utils.waitTillSpinnerDisappear(10);
		// edit email templates with no update
		pageObj.emailTemplatePage().editTemplateSaveAndDraft(emailTemplateName);
		pageObj.emailTemplatePage().saveAsDraft1(emailTemplateName);
		utils.waitTillSpinnerDisappear(10);
		// delete email templates
		pageObj.emailTemplatePage().deleteTemplate(emailTemplateName);
		selUtils.implicitWait(50);

	}

	@Test(description = "SQ-T2691 Create new email template and publish(franchise admin) || SQ-T2692 (1.0) Delete created email template(franchise user)  ", groups = "Regression", priority = 2)
	@Owner(name = "Hardik Bhardwaj")
	public void T2691_CreateEmailTempleteFranchiseAdmin() throws InterruptedException {
		String emailTemplateName = "AutoEmailTemplatetemp";
		emailTemplateName = emailTemplateName.replace("temp", timstamp);
		// navigate to user timeline
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance(dataSet.get("userName"), dataSet.get("password"));
		// navigate to email templates
		pageObj.menupage().navigateToSubMenuItem("Settings", "Email Templates");
		// create email templates
		pageObj.emailTemplatePage().clickOnNewFolder();
		pageObj.emailTemplatePage().createNewEmailTemplate(emailTemplateName);
		pageObj.emailTemplatePage().addMediaToTemplate();
		pageObj.emailTemplatePage().addContentToTemplate();
		pageObj.emailTemplatePage().addButtonToTemplate();
		pageObj.emailTemplatePage().addSocialToTemplate();
		pageObj.emailTemplatePage().saveAndPublish(emailTemplateName);
		utils.waitTillSpinnerDisappear(10);
		pageObj.emailTemplatePage().deleteTemplate(emailTemplateName);
		selUtils.implicitWait(50);
	}

	@Test(description = "SQ-T2687 Create new email template shared with franchise and publish(admin user) ", groups = "Regression", priority = 3)
	@Owner(name = "Hardik Bhardwaj")
	public void T2687_CreateEmailTempleteAndShareWithFranchiseAdmin() throws InterruptedException {
		String emailTemplateName = "AutoEmailTemplatetemp";
		emailTemplateName = emailTemplateName.replace("temp", timstamp);
		// navigate to user timeline
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// Click Cockpit and click External Source Id Flag
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().checkEnableRichEmailEditorFlag();
		pageObj.dashboardpage().checkEnableFranchiseesFlag();
		// navigate to email templates
		pageObj.menupage().navigateToSubMenuItem("Settings", "Email Templates");
		// create email templates
		pageObj.emailTemplatePage().createNewEmailTemplate(emailTemplateName);
		pageObj.emailTemplatePage().addContentToTemplate();
		pageObj.emailTemplatePage().addButtonToTemplate();
		pageObj.emailTemplatePage().addSocialToTemplate();
		pageObj.emailTemplatePage().availableForFranchise();
		pageObj.emailTemplatePage().saveAndPublishFranchise(emailTemplateName);
		utils.waitTillSpinnerDisappear(10);
		pageObj.emailTemplatePage().validateFranchiseTemplate(emailTemplateName);
		pageObj.emailTemplatePage().deleteTemplate(emailTemplateName);
		selUtils.implicitWait(50);
	}

	@SuppressWarnings("unused")
	// this campaign is planned to be deprecated from campaigns - SQ-1959
	// @Test(description = "SQ-T2287 Verify to create AB Campaign"
	// + " SQ-T6415 verify Potential reachability in segment and subscription count
	// in AB cam", groups = "Regression", priority = 2)
	@Owner(name = "Vansham Mishra")
	public void T2287_verifytocreateABCampaign() throws InterruptedException {
		// Precondition: segement and user is present Segement:Automation Mass Offer
		String abCampaignName = "Automation AB Campaign" + CreateDateTime.getTimeDateString();
		String dateTime = CreateDateTime.getCurrentDate() + " 05:00 PM";

		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Click Campaigns Link
		Thread.sleep(2000);
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value
		Thread.sleep(2000);

		pageObj.campaignspage().selectMessagedrpValue(dataSet.get("messageDrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();

		pageObj.signupcampaignpage().setCampaignName(abCampaignName);
		pageObj.signupcampaignpage().clickNextBtn();
		pageObj.signupcampaignpage().setAudienceType(dataSet.get("audiance"));
		pageObj.signupcampaignpage().setSegmentType(dataSet.get("segment"));
		// pageObj.signupcampaignpage().setSampleSize();
		pageObj.signupcampaignpage().setWinningCombination("Highest unique email opens");
		pageObj.signupcampaignpage().setPushNotification(dataSet.get("pushNotification"));
		pageObj.signupcampaignpage().setEmailSubject(dataSet.get("emailSubject"));
		pageObj.signupcampaignpage().setEmailTemplate(dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().subjectTemplateGroup();
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().setStartDateforABCampaign();
		pageObj.signupcampaignpage().clickScheduleBtn();

		/*
		 * boolean status = pageObj.campaignspage().validateSuccessMessage();
		 * Assert.assertTrue(status,
		 * "Schedule created successfully Success message did not displayed....");
		 */

		// run campaign schedule
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().findMassCampaignNameandRun(abCampaignName);

		// get segent count
		Response response = pageObj.endpoints().getSegmentCount(dataSet.get("apiKey"), dataSet.get("segmentid"));
		Assert.assertEquals(response.getStatusCode(), 200, "Status code did not matched");
		String cnt = response.jsonPath().get("count");
		String cnt1 = cnt.replaceAll(",", "");
		int segCount = Integer.parseInt(cnt1);
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// search for the campaign
		pageObj.campaignspage().searchAndSelectCamapign(abCampaignName);
		int segSize = pageObj.campaignspage().getABCampaignsTotalSegmentSize();
		Assert.assertEquals(segCount, segSize, " Segment count did not matched with the ab campaign segment size");

		int count = pageObj.sidekiqPage().checkSidekiqJobWithPolling(baseUrl, "ABCampaignMemberWorker");
		Assert.assertTrue(count > 0, "ABCampaignMemberWorker count did not matched");
		TestListeners.extentTest.get().pass("AB Campaign Job validated in sidekiq successfully");
	}

	// Anant
	// Remove alternate language from autoseven
	@Test(description = "SQ-T4550 Verify creation of Challenge campaign from new CHP")
	@Owner(name = "Vansham Mishra")
	public void T4550_verifyCreationChallengeCampaign() throws Exception {
		String campaign = "AutmationChallengeCampaign" + CreateDateTime.getTimeDateString();
		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// navigate to settings menu
		pageObj.menupage().navigateToSubMenuItem("Settings", "Business");
		// pageObj.menupage().address_tab();
		pageObj.dashboardpage().navigateToTabs("Address");
		pageObj.settingsPage().removeAlternateLanguages();
		pageObj.settingsPage().clickSaveBtn();

		// goto new campaign page
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().createOtherCampaignCHP("Other", "Challenge");
		selUtils.waitforPageLoad();
		// create the campaign
		pageObj.newCamHomePage().challengeCampaignCreation(campaign, dataSet.get("giftType"), dataSet.get("redeemable"),
				dataSet.get("segmentName"));

		// check the status of the campaign
		pageObj.newCamHomePage().searchCampaign(campaign);
		Boolean activeStatus = pageObj.newCamHomePage().checkCampaignStatus(campaign, "Active");
		Assert.assertTrue(activeStatus, "Campaign ativated status did not matched");
		logger.info("Verified successfully created the challenge campaign");
		TestListeners.extentTest.get().info("Verified successfully created the challenge campaign");

		// delete campaign from db
		pageObj.utils().deleteCampaignFromDb(campaign, env);
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}
}