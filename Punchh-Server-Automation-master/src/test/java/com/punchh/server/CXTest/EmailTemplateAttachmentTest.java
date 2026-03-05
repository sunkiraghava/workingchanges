package com.punchh.server.CXTest;

import java.lang.reflect.Method;
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

import com.punchh.server.annotations.Owner;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.GmailConnection;
import com.punchh.server.utilities.MessagesConstants;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class EmailTemplateAttachmentTest {

	private static Logger logger = LogManager.getLogger(EmailTemplateAttachmentTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String sTCName;
	private String env, run = "ui";
	private String baseUrl;
	private static Map<String, String> dataSet;
	private Utilities utils;

	@BeforeClass(alwaysRun = true)
	public void openBrowser() {
		driver = new BrowserUtilities().launchBrowser();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		// Single login to instance
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		pageObj.instanceDashboardPage().instanceLogin(baseUrl);
	}

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
		sTCName = method.getName();
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==> " + dataSet);
		utils = new Utilities(driver);
		// Move to All Businesses page
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
	}

	// By Ajeet
	@Test(description = "SQ-T6119 Attach the email template to the system messages or notification template; "
			+ "SQ-T7101: Test {{current_year}} in notification templates;", groups = "regression", priority = 5)
	@Owner(name = "Vaibhav Agnihotri")
	public void T6119_AttachEmailTemplateToSystemMessage() throws Exception {
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Settings", "Notification Templates");
		String messageActual = pageObj.notificationTemplatePage().toastMessages();
		Assert.assertEquals(messageActual, MessagesConstants.toastMessage, "Failed to verify the toast message");
		utils.logit("pass", "Toast message under general tab verified");
		pageObj.notificationTemplatePage().setSystemMessageOptionSliderOnOff(dataSet.get("messageName"), "enable");
		pageObj.notificationTemplatePage().selectSystemMessageFor(dataSet.get("messageName"));
		String tagText = pageObj.iframeConfigurationPage().getElementText("NotificationTemplatePage.messagingTag",
				"current_year");
		Assert.assertEquals(tagText, "{{{current_year}}}");
		pageObj.dashboardpage().checkUncheckToggle("Enable Email Editor", "On");
		pageObj.notificationTemplatePage().selectEmailtemplateAndAttach(dataSet.get("templateName"));
		pageObj.notificationTemplatePage().waitTillEmailSubjectVisible("Notification Template");
		pageObj.notificationTemplatePage().clickOnSaveButton();
		messageActual = pageObj.notificationTemplatePage().toastMessages();
		Assert.assertEquals(messageActual, MessagesConstants.notificationTemplateSaveMessage,
				"Failed to verify email template saved message for notification template or system message");
		utils.logit("pass", "Email template attached to the system message successfully");
		// Verify that email body contains the current year
		pageObj.campaignspage().sendTestNotification(dataSet.get("userEmail"));
		String emailBody = GmailConnection.getGmailEmailBody("subject", dataSet.get("emailSubject"), true);
		Assert.assertTrue(!emailBody.isEmpty(), "Email body is empty.");
		boolean isCurrentYearMatched = emailBody.contains("Current year: " + CreateDateTime.getCurrentYear());
		Assert.assertTrue(isCurrentYearMatched);
		utils.logit("pass", "Verified that user received email contains the current year");

	}

	// By Ajeet
	@Test(description = "SQ-T6120 attach email template to membership level", groups = "Regression", priority = 6)
	@Owner(name = "Ajeet")
	public void T6120_AttachEmailTemplateToMembershipLevel() throws InterruptedException {
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		utils.checkUncheckFlag("Enable Rich Email Editor?", "check");
		pageObj.dashboardpage().clickOnUpdateButton();
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		pageObj.membershipsPage().selectMembership("Fan");
		pageObj.membershipsPage().scrollToEnableEmailEditor();
		pageObj.dashboardpage().checkUncheckToggle("Enable Email Editor", "On");
		pageObj.notificationTemplatePage().selectEmailtemplateAndAttach(dataSet.get("templateName1"));
		//utils.waitTillSpinnerDisappear();
		pageObj.notificationTemplatePage().waitTillEmailSubjectVisible("Membership Level");
		pageObj.membershipsPage().clickOnMembershipLevelButton();
		String messageActual = pageObj.notificationTemplatePage().toastMessages();
		Assert.assertEquals(messageActual, MessagesConstants.membershipLevelUpdatedMessage,
				"Failed to verify email template attachment to membership level");
		utils.logit("pass", "Email template attached to membership level successfully");
	}

	// By Ajeet
	@Test(description = "SQ-T6121 attach email template to surveys", groups = "Regression", priority = 7)
	@Owner(name = "Ajeet")
	public void T6121_AttachEmailTemplateToSurveys() throws InterruptedException {
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		utils.checkUncheckFlag("Enable Rich Email Editor?", "check");
		pageObj.dashboardpage().clickOnUpdateButton();
		pageObj.menupage().navigateToSubMenuItem("Settings", "Surveys");
		pageObj.surveysPage().selectSurveys("Checkin Survey");
		pageObj.membershipsPage().scrollToEnableEmailEditor();
		pageObj.dashboardpage().checkUncheckToggle("Enable Email Editor", "On");
		pageObj.notificationTemplatePage().selectEmailtemplateAndAttach(dataSet.get("templateName2"));
		//utils.waitTillSpinnerDisappear();
		pageObj.notificationTemplatePage().waitTillEmailSubjectVisible("Checkin Survey");
		pageObj.surveysPage().clickOnUpdateButton();
		String messageActual = pageObj.notificationTemplatePage().toastMessages();
		Assert.assertEquals(messageActual, MessagesConstants.surveysUpdated,
				"Failed to verify email template attachment to surveys");
		utils.logit("pass", "Email template attached to surveys successfully");
	}

	// By Ajeet
	@Test(description = "SQ-T6122 attach email template to Signup campaign under the Ongoing dropDown|| Attach email template to"
			+ " Mass Notification,A/B Test and Post Checkin Campaigns and delete these all after attachment of email templates", groups = "Regression", priority = 8)
	@Owner(name = "Ajeet")
	public void T6122_AttachEmailtemplateToCampaigns() throws Exception {
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		utils.checkUncheckFlag("Enable Rich Email Editor?", "check");
		pageObj.dashboardpage().clickOnUpdateButton();
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.campaignspage().selectOngoingdrpValue("Signup");
		pageObj.campaignspage().editOngoingSignupCampaigns("Campaign For Automation By Ajeet");
		// segment selected
		pageObj.signupcampaignpage().setSegmentType(dataSet.get("segmentName"));
		pageObj.notificationTemplatePage().selectEmailtemplateAndAttach(dataSet.get("templateName3"));
		//utils.waitTillSpinnerDisappear();
		pageObj.notificationTemplatePage().waitTillEmailSubjectVisible("Signup Campaign");
		pageObj.notificationTemplatePage().verifyTheTestNotificationMessageOnEmail(dataSet.get("testUserEmail"));
		utils.logit("pass", "Email template attached to ongoing signup campaign successfully");

		String campaignName = "AutomationMassNotificationCampaign_" + CreateDateTime.getTimeDateString();
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.campaignspage().selectMessagedrpValue("Mass Notification");
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().setCampaignName(campaignName);
		pageObj.signupcampaignpage().clickNextBtn();
		pageObj.signupcampaignpage().setAudienceType(dataSet.get("audiance"));
		pageObj.signupcampaignpage().setSegmentType(dataSet.get("segmentName"));
		pageObj.signupcampaignpage().setPushNotification(campaignName);
		pageObj.dashboardpage().checkUncheckToggle("Enable Email Editor", "On");
		pageObj.notificationTemplatePage().selectEmailtemplateAndAttach(dataSet.get("templateName3"));
		pageObj.notificationTemplatePage().waitTillEmailSubjectVisible("Mass Notification Campaign");
		pageObj.signupcampaignpage().setEmailSubject(dataSet.get("emailSubject"));
		pageObj.signupcampaignpage().setEmailTemplate(dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().clickNextButton();
		utils.logit("pass", "Email template attached to Mass Notification campaign successfully");
		// delete the campaign
		pageObj.utils().deleteMassCampaignByName(campaignName, env);
		utils.logit(campaignName + " campaign deleted successfully.");
		// Attach email Template to A/B Test
		campaignName = "AutomationABTestCampaign_" + CreateDateTime.getTimeDateString();
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.campaignspage().selectMessagedrpValue("A/B Test");
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().setCampaignName(campaignName);
		pageObj.signupcampaignpage().clickNextBtn();
		pageObj.signupcampaignpage().setAudienceType(dataSet.get("audiance"));
		pageObj.signupcampaignpage().setSegmentType(dataSet.get("abTestSegmentName"));
		pageObj.signupcampaignpage().setPushNotification(campaignName);
		pageObj.dashboardpage().checkUncheckToggle("Enable Email Editor", "On");
		pageObj.notificationTemplatePage().selectEmailtemplateAndAttach(dataSet.get("templateName3"));
		pageObj.signupcampaignpage().setEmailSubject(dataSet.get("emailSubject"));
		pageObj.signupcampaignpage().setEmailTemplate(dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().subjectTemplateGroup();
		//pageObj.signupcampaignpage().clickNextBtn();
		utils.logit("pass", "Email template attached to A/B Test campaign successfully");
		// Delete the campaign
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.campaignspage().removeSearchedCampaign(campaignName);
		utils.logit(campaignName + " campaign deleted successfully.");

		// Attach Email template to "Post Checkin Message"
		campaignName = "AutomationPostCheckinMessageCampaign_" + CreateDateTime.getTimeDateString();
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.campaignspage().selectMessagedrpValue("Post Checkin Message");
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().setCampaignName(campaignName);
		pageObj.signupcampaignpage().clickNextBtn();
		pageObj.campaignspage().selectSegmentFromDropDown("All signup segment");
		pageObj.dashboardpage().checkUncheckToggle("Enable Email Editor", "On");
		pageObj.notificationTemplatePage().selectEmailtemplateAndAttach(dataSet.get("templateName3"));
		pageObj.notificationTemplatePage().waitTillEmailSubjectVisible("Post Checkin Notification Campaign");
		pageObj.signupcampaignpage().setEmailSubject(dataSet.get("emailSubject"));
		pageObj.signupcampaignpage().setEmailTemplate(dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().clickNextButton();
		utils.logit("pass", "Email template attached to Post Checkin Message campaign successfully");
		// Delete the campaign
		pageObj.utils().deleteCampaignFromDb(campaignName, env);
		utils.logit(campaignName + " campaign deleted successfully.");

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