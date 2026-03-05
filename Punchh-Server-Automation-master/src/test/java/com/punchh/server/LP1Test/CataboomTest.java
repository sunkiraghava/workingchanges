package com.punchh.server.LP1Test;

import java.lang.reflect.Method;
import java.util.Arrays;
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

import com.punchh.server.annotations.Owner;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.GmailConnection;
import com.punchh.server.utilities.MessagesConstants;
import com.punchh.server.utilities.NewMenu;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class CataboomTest {

	private static Logger logger = LogManager.getLogger(CataboomTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String sTCName, env, baseUrl;
	String run = "ui";
	private static Map<String, String> dataSet;
	private Utilities utils;
	String cataboomUrl;

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
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==>" + dataSet);
		utils = new Utilities(driver);
		cataboomUrl = dataSet.get("cataboomUrl");
		// Move to All businesses page
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
	}

	@Test(description = "SQ-T6709: Verify Cataboom Games integration E2E")
	@Owner(name = "Vaibhav Agnihotri")
	public void T6709_verifyGamesCataboomIntegration() throws Exception {
		String gamingLevelUrl = baseUrl + dataSet.get("gamingLevelUrl");
		String gamesReportUrl = baseUrl + dataSet.get("gamesReportUrl");
		String scratchCardUrl = baseUrl + dataSet.get("scratchCardUrl");
		String whitelabelUrl = baseUrl + "/whitelabel/" + dataSet.get("slug");

		// Select business
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// OFF: Both internal and Cataboom games
		utils.logit("OFF: Both internal and Cataboom games");
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Campaigns");
		pageObj.dashboardpage().checkUncheckAnyFlagWitoutUpdate("Has Games?", "uncheck");
		pageObj.dashboardpage().checkUncheckAnyFlagWitoutUpdate("Enable Scratch Match & Win Game?", "uncheck");
		pageObj.dashboardpage().checkUncheckAnyFlagWitoutUpdate("Enable Slot Machine Game?", "uncheck");
		pageObj.dashboardpage().checkUncheckAnyFlagWitoutUpdate("Enable Cataboom Integration?", "uncheck");
		pageObj.dashboardpage().clickOnUpdateButton();
		boolean isCustomMenuDisplayed = pageObj.gamesPage().isPresent(utils.getLocatorValue("gamesPage.customNavTab"));
		Assert.assertFalse(isCustomMenuDisplayed);
		utils.logit("pass", "When all game related flags are OFF, Custom menu having Games is not displayed");

		// ON: Both internal and Cataboom games
		utils.logit("ON: Both internal and Cataboom games");
		pageObj.dashboardpage().checkUncheckAnyFlagWitoutUpdate("Has Games?", "check");
		pageObj.dashboardpage().checkUncheckAnyFlagWitoutUpdate("Enable Scratch Match & Win Game?", "check");
		pageObj.dashboardpage().checkUncheckAnyFlagWitoutUpdate("Enable Slot Machine Game?", "check");
		pageObj.dashboardpage().checkUncheckAnyFlagWitoutUpdate("Enable Cataboom Integration?", "check");
		pageObj.dashboardpage().clickOnUpdateButton();
		Assert.assertEquals(utils.getSuccessMessage(), MessagesConstants.cataboomRequireDisabledFlags);
		utils.logit("pass", "When both internal and Cataboom games are enabled together, error message is displayed");

		// ON: Has Games?, Enable Scratch Match & Win Game?, Enable Slot Machine Game?
		// OFF: Enable Cataboom Integration?
		utils.logit(
				"ON: Has Games?, Enable Scratch Match & Win Game?, Enable Slot Machine Game?\nOFF: Enable Cataboom Integration?");
		pageObj.dashboardpage().checkUncheckAnyFlagWitoutUpdate("Has Games?", "check");
		pageObj.dashboardpage().checkUncheckAnyFlagWitoutUpdate("Enable Scratch Match & Win Game?", "check");
		pageObj.dashboardpage().checkUncheckAnyFlagWitoutUpdate("Enable Slot Machine Game?", "check");
		pageObj.dashboardpage().checkUncheckAnyFlagWitoutUpdate("Enable Cataboom Integration?", "uncheck");
		pageObj.dashboardpage().clickOnUpdateButton();

		// Gaming Levels, Games Report, Scratch Cards sub-menu are available
		List<String> customSubMenusList = pageObj.menupage().subMenuItems(NewMenu.menu_Custom);
		pageObj.menupage().pinSidenavMenu();
		Assert.assertTrue(customSubMenusList.contains("Gaming Levels"));
		Assert.assertTrue(customSubMenusList.contains("Games Report"));
		Assert.assertTrue(customSubMenusList.contains("Scratch Cards"));
		utils.logit("pass", "Custom menu contains Gaming Levels, Games Report & Scratch Cards sub-menu");

		// Cataboom Dashboard Access button is not present on Games page
		pageObj.menupage().navigateToSubMenuItem("Settings", "Games");
		boolean isCataboomDashboardAccessBtnPresent = pageObj.gamesPage()
				.isPresent(utils.getLocatorValue("gamesPage.loginToGamesDashboardBtn"));
		Assert.assertFalse(isCataboomDashboardAccessBtnPresent);
		utils.logit("pass", "Cataboom Dashboard Access button is not present on Games page");

		// Signup a user and Login to iFrame
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		pageObj.iframeSingUpPage().navigateToIframe(whitelabelUrl);
		pageObj.iframeSingUpPage().iframeLogin(userEmail);
		// Scratch Match & Win and Slot Machine tabs are present
		boolean isScratchMatchAndWinTabPresent = pageObj.gamesPage()
				.isPresent(utils.getLocatorValue("gamesPage.iFrameScratchMatchWinTab"));
		boolean isSlotMachineTabPresent = pageObj.gamesPage()
				.isPresent(utils.getLocatorValue("gamesPage.iFrameSlotMachineTab"));
		Assert.assertTrue(isScratchMatchAndWinTabPresent);
		Assert.assertTrue(isSlotMachineTabPresent);
		utils.logit("pass", "Scratch Match & Win and Slot Machine tabs are present on iFrame");
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Campaigns");

		// ON: Enable Cataboom Integration?
		// OFF: Has Games?, Enable Scratch Match & Win Game?, Enable Slot Machine Game?
		utils.logit(
				"ON: Enable Cataboom Integration?\nOFF: Has Games?, Enable Scratch Match & Win Game?, Enable Slot Machine Game?");
		pageObj.dashboardpage().checkUncheckAnyFlagWitoutUpdate("Has Games?", "uncheck");
		pageObj.dashboardpage().checkUncheckAnyFlagWitoutUpdate("Enable Scratch Match & Win Game?", "uncheck");
		pageObj.dashboardpage().checkUncheckAnyFlagWitoutUpdate("Enable Slot Machine Game?", "uncheck");
		pageObj.dashboardpage().checkUncheckAnyFlagWitoutUpdate("Enable Cataboom Integration?", "check");
		pageObj.dashboardpage().clickOnUpdateButton();

		// Ensure Cataboom credentials are configured
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "Services");
		pageObj.dashboardpage().navigateToTabs("Games");
		String jwtSecret = pageObj.iframeConfigurationPage().getAttributeValue("gamesPage.cataboomJwtSecretKey",
				"value", "");
		Assert.assertFalse(jwtSecret.isEmpty(), "JWT Secret key is empty, please check in UI");
		pageObj.iframeConfigurationPage().editInputField("Games Dashboard URL", "gamesPage.cataboomDashboardUrl",
				cataboomUrl);
		pageObj.gamesPage().clickButton("gamesPage.servicesCataboomUpdateBtn");
		utils.logit("Cataboom credentials are configured");

		// Gaming Levels, Games Report, Scratch Cards sub-menu are unavailable
		customSubMenusList = pageObj.menupage().subMenuItems(NewMenu.menu_Custom);
		pageObj.menupage().pinSidenavMenu();
		Assert.assertFalse(customSubMenusList.contains("Gaming Levels"));
		Assert.assertFalse(customSubMenusList.contains("Games Report"));
		Assert.assertFalse(customSubMenusList.contains("Scratch Cards"));
		utils.logit("pass", "Custom menu does not contain Gaming Levels, Games Report & Scratch Cards sub-menu");

		// Scratch Match & Win and Slot Machine tabs are not present
		pageObj.gamesPage().navigateToURL(whitelabelUrl);
		isScratchMatchAndWinTabPresent = pageObj.gamesPage()
				.isPresent(utils.getLocatorValue("gamesPage.iFrameScratchMatchWinTab"));
		isSlotMachineTabPresent = pageObj.gamesPage()
				.isPresent(utils.getLocatorValue("gamesPage.iFrameSlotMachineTab"));
		Assert.assertFalse(isScratchMatchAndWinTabPresent);
		Assert.assertFalse(isSlotMachineTabPresent);
		utils.logit("pass", "Scratch Match & Win and Slot Machine tabs are not present on iFrame");

		// 'Login to Games Dashboard Button' functionality on Games page
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Settings", "Games");

		// Verify UI messaging on Games page
		String heading = pageObj.iframeConfigurationPage().getElementText("gamesPage.heading", "");
		Assert.assertEquals(heading, MessagesConstants.gamesPageHeading);
		String subHeading = pageObj.iframeConfigurationPage().getElementText("gamesPage.subHeading", "");
		Assert.assertEquals(subHeading, MessagesConstants.gamesPageSubHeading);
		String description = pageObj.iframeConfigurationPage().getElementText("gamesPage.description", "");
		Assert.assertEquals(description, MessagesConstants.gamesPageDescription);
		String listItems = pageObj.iframeConfigurationPage().getElementText("gamesPage.listItems", "");
		Assert.assertEquals(listItems, MessagesConstants.gamesPageListItems);
		Assert.assertFalse(pageObj.gamesPage().verifyTextOnPage("Powered by Cataboom"));
		utils.logit("pass", "UI messaging on Games page is as expected");
		pageObj.gamesPage().clickButton("gamesPage.loginToGamesDashboardBtn");
		utils.switchToWindow();
		Assert.assertTrue(utils.verifyPartOfURL(cataboomUrl));
		utils.logit("pass", "Login to Games Dashboard Button functionality is working fine");

		// Access of Gaming Levels, Games Report, Scratch Cards through saved links
		// are restricted
		pageObj.gamesPage().navigateToURL(gamingLevelUrl);
		Assert.assertEquals(utils.getSuccessMessage(), MessagesConstants.gamesDisabled);
		pageObj.gamesPage().navigateToURL(gamesReportUrl);
		Assert.assertEquals(utils.getSuccessMessage(), MessagesConstants.insufficientPrivileges);
		pageObj.gamesPage().navigateToURL(scratchCardUrl);
		Assert.assertEquals(utils.getSuccessMessage(), MessagesConstants.gamesDisabled);
		utils.logit("pass", "Access of Gaming Levels, Games Report, Scratch Cards through saved links are restricted");

	}

	@Test(description = "SQ-T6727: Verify JWT Token Tag with Description and redirection to Cataboom via Mass Offer Campaign; "
			+ "SQ-T7100: Test {{current_year}} in email templates; "
			+ "SQ-T7099: Verify {{current_year}} tag availability across all template editors",groups = {"nonNightly" })
	@Owner(name = "Vaibhav Agnihotri")
	public void T6727_verifyCataboomLinkMassOfferCampaign() throws Exception {
		// Pre-requisites: Services > Email settings are configured.
		List<String> expectedTagAndDescriptionList = Arrays.asList(dataSet.get("tagString1"),
				MessagesConstants.currentYearTagDescription, dataSet.get("tagString2"),
				MessagesConstants.gameJwtTokenTagDescription);
		String massCampaignName = CreateDateTime.getUniqueString("T6727_MassOfferCampaign_");
		String emailSubject = massCampaignName + "_EmailSubject";
		String dateTime = CreateDateTime.getTomorrowDate() + " 11:00 PM";
		String currentYearText = "Current year: " + CreateDateTime.getCurrentYear();
		String urlWithTags = dataSet.get("loginUrl") + dataSet.get("tagString2") + " | Current year: "
				+ dataSet.get("tagString1");

		// Select business
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Enable Cataboom Integration and disable other game options
		utils.logit("Enable Cataboom Integration and disable other game options");
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Campaigns");
		pageObj.dashboardpage().checkUncheckAnyFlagWitoutUpdate("Has Games?", "uncheck");
		pageObj.dashboardpage().checkUncheckAnyFlagWitoutUpdate("Enable Scratch Match & Win Game?", "uncheck");
		pageObj.dashboardpage().checkUncheckAnyFlagWitoutUpdate("Enable Slot Machine Game?", "uncheck");
		pageObj.dashboardpage().checkUncheckAnyFlagWitoutUpdate("Enable Cataboom Integration?", "check");
		pageObj.dashboardpage().clickOnUpdateButton();

		// Navigate to Campaigns
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("offerDropdown"));
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().setCampaignName(massCampaignName);
		pageObj.signupcampaignpage().setGiftType(dataSet.get("giftType"));
		pageObj.signupcampaignpage().setFixedPoints(dataSet.get("fixedPoints"));
		pageObj.signupcampaignpage().clickNextBtn();
		pageObj.signupcampaignpage().setAudienceType(dataSet.get("audience"));
		pageObj.signupcampaignpage().setSegmentType(dataSet.get("segment"));
		// Verify Tags and Description section
		pageObj.campaignspage().openOrCloseTagsButton("open");
		List<String> tagsAndDescriptionList = pageObj.campaignspage().getTagsAndDescriptionList();
		boolean isTagVerified = tagsAndDescriptionList.containsAll(expectedTagAndDescriptionList);
		Assert.assertTrue(isTagVerified);
		utils.logit("pass", "Tags and Description list contains expected values: " + expectedTagAndDescriptionList);
		//pageObj.campaignspage().openOrCloseTagsButton("close");
		pageObj.gamesPage().clickButton("campaignsPage.closeTagsButton");
		// Provide Cataboom URL with JWT token tag in PN and Email
		pageObj.signupcampaignpage().setPushNotification(urlWithTags);
		pageObj.dashboardpage().checkUncheckToggle("Enable Email Editor", "On");
		pageObj.notificationTemplatePage().selectEmailtemplateAndAttach(dataSet.get("templateName"));
		pageObj.notificationTemplatePage().waitTillEmailSubjectVisible("Mass Notification Campaign");
		pageObj.signupcampaignpage().setEmailSubject(emailSubject);
		pageObj.signupcampaignpage().clickNextButton();
		pageObj.signupcampaignpage().setFrequencyAndTimeInCampaign("Once", dateTime);
		pageObj.signupcampaignpage().scheduleCampaign();
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().findMassCampaignNameandRun(massCampaignName);

		// User Timeline validation
		pageObj.instanceDashboardPage().navigateToGuestTimeline(dataSet.get("userEmail"));
		String campaignName = pageObj.guestTimelinePage().getcampaignNameWithWait(massCampaignName);
		Assert.assertTrue(campaignName.equalsIgnoreCase(massCampaignName));
		utils.logit("Mass Offer campaign '" + massCampaignName + "' is found on user timeline");

		// Hit `/api/mobile/users/notifications` API to verify the message
		Response fetchNotificationsResponse = pageObj.endpoints().api1FetchNotifications(dataSet.get("client"),
				dataSet.get("secret"), dataSet.get("userAccessToken"));
		Assert.assertEquals(fetchNotificationsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code did not match for API1 Fetch User Notifications");
//		String notificationMessage = fetchNotificationsResponse.jsonPath().get("[0].message").toString();
		String notificationMessage = utils.getJsonReponseKeyValueFromJsonArrayWithoutArrayName(
				fetchNotificationsResponse, "message", currentYearText);
		Assert.assertTrue(notificationMessage.contains(dataSet.get("loginUrl")));
		Assert.assertTrue(notificationMessage.contains(currentYearText));
		utils.logit("pass", "Verified via API that notification message contains the expected text");

		// Verify the URL present on email received by user
		String emailBody = GmailConnection.getGmailEmailBody("subject", emailSubject, true);
		Assert.assertTrue(!emailBody.isEmpty(), "Email body is empty.");
		String[] emailContent = emailBody.split("\n");
		pageObj.gamesPage().navigateToURL(emailContent[0]);
		boolean isUrlVerified = utils.verifyPartOfURL(cataboomUrl);
		Assert.assertTrue(isUrlVerified);
		boolean isCurrentYearMatched = emailBody.contains(currentYearText);
		Assert.assertTrue(isCurrentYearMatched);
		utils.logit("pass",
				"Verified that user received email containing URL (which leads to Cataboom website) and current year");

	}

	@Test(description = "SQ-T6691: Verify that users are receiving the Cataboom game link correctly when a Mass notification campaign is sent with a tag containing the Cataboom URL",groups = {"nonNightly" })
	@Owner(name = "Vaibhav Agnihotri")
	public void T6691_verifyCataboomLinkMassNotificationCampaign() throws Exception {
		// Pre-requisites: Services > Email settings are configured.
		String massCampaignName = CreateDateTime.getUniqueString("T6691_MassNotificationCampaign_");
		String emailSubject = massCampaignName + "Email";
		String urlWithGameToken = dataSet.get("urlWithGameToken");
		String dateTime = CreateDateTime.getTomorrowDate() + " 11:00 PM";

		// Select business
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Enable Cataboom Integration and disable other game options
		utils.logit("Enable Cataboom Integration and disable other game options");
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Campaigns");
		pageObj.dashboardpage().checkUncheckAnyFlagWitoutUpdate("Has Games?", "uncheck");
		pageObj.dashboardpage().checkUncheckAnyFlagWitoutUpdate("Enable Scratch Match & Win Game?", "uncheck");
		pageObj.dashboardpage().checkUncheckAnyFlagWitoutUpdate("Enable Slot Machine Game?", "uncheck");
		pageObj.dashboardpage().checkUncheckAnyFlagWitoutUpdate("Enable Cataboom Integration?", "check");
		pageObj.dashboardpage().clickOnUpdateButton();

		// Ensure Cataboom credentials are configured
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "Services");
		pageObj.dashboardpage().navigateToTabs("Games");
		String jwtSecret = pageObj.iframeConfigurationPage().getAttributeValue("gamesPage.cataboomJwtSecretKey",
				"value", "");
		Assert.assertFalse(jwtSecret.isEmpty(), "JWT Secret key is empty, please check in UI");
		pageObj.iframeConfigurationPage().editInputField("Games Dashboard URL", "gamesPage.cataboomDashboardUrl",
				cataboomUrl);
		pageObj.gamesPage().clickButton("gamesPage.servicesCataboomUpdateBtn");
		utils.logit("Cataboom credentials are configured");

		// Navigate to Campaigns
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.campaignspage().selectMessagedrpValue(dataSet.get("messageDropdown"));
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().setCampaignName(massCampaignName);
		pageObj.signupcampaignpage().clickNextBtn();
		pageObj.signupcampaignpage().setAudienceType(dataSet.get("audience"));
		pageObj.signupcampaignpage().setSegmentType(dataSet.get("segment"));
		// Provide Cataboom URL with JWT token tag in PN and Email
		pageObj.signupcampaignpage().setPushNotification(urlWithGameToken);
		pageObj.signupcampaignpage().setEmailSubject(emailSubject);
		pageObj.signupcampaignpage().setEmailTemplate(urlWithGameToken);
		pageObj.signupcampaignpage().clickNextButton();
		pageObj.signupcampaignpage().setFrequencyAndTimeInCampaign("Once", dateTime);
		pageObj.signupcampaignpage().scheduleCampaign();
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().findMassCampaignNameandRun(massCampaignName);

		// User Timeline validation
		pageObj.instanceDashboardPage().navigateToGuestTimeline(dataSet.get("userEmail"));
		String campaignName = pageObj.guestTimelinePage().getcampaignNameWithWait(massCampaignName);
		Assert.assertTrue(campaignName.equalsIgnoreCase(massCampaignName));
		utils.logit("Mass Notification campaign '" + massCampaignName + "' is found on user timeline");

		// Verify the URL present on email received by user
		String emailBody = GmailConnection.getGmailEmailBody("subject", emailSubject, true);
		Assert.assertTrue(!emailBody.isEmpty(), "Email body is empty.");
		pageObj.gamesPage().navigateToURL(emailBody);
		boolean isUrlVerified = utils.verifyPartOfURL(cataboomUrl);
		Assert.assertTrue(isUrlVerified);
		utils.logit("pass", "Verified that user received email containing URL which leads to Cataboom website");

	}

	@Test(description = "SQ-T6826: Verify JWT Secret Key Copy on Services > Games page")
	@Owner(name = "Vaibhav Agnihotri")
	public void T6826_verifyGamesJwtSecretKey() throws Exception {
		// Select business
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Enable Cataboom Integration and disable other game options
		utils.logit("Enable Cataboom Integration and disable other game options");
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Campaigns");
		pageObj.dashboardpage().checkUncheckAnyFlagWitoutUpdate("Has Games?", "uncheck");
		pageObj.dashboardpage().checkUncheckAnyFlagWitoutUpdate("Enable Scratch Match & Win Game?", "uncheck");
		pageObj.dashboardpage().checkUncheckAnyFlagWitoutUpdate("Enable Slot Machine Game?", "uncheck");
		pageObj.dashboardpage().checkUncheckAnyFlagWitoutUpdate("Enable Cataboom Integration?", "check");
		pageObj.dashboardpage().clickOnUpdateButton();

		// Verify Edit and Copy functionality for Games JWT secret key
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "Services");
		pageObj.dashboardpage().navigateToTabs("Games");
		String inputValue = CreateDateTime.getRandomString(270);
		pageObj.gamesPage().performSecretKeyAction("edit", inputValue);
		pageObj.gamesPage().clickButton("gamesPage.servicesCataboomUpdateBtn");
		String copiedValue = pageObj.gamesPage().performSecretKeyAction("copy", "");
		Assert.assertEquals(copiedValue, inputValue);
		utils.logit("pass", "Copy functionality is working fine for JWT secret key > 256 chars");
		inputValue = dataSet.get("jwtSampleKeyUnmasked");
		pageObj.gamesPage().performSecretKeyAction("edit", inputValue);
		pageObj.gamesPage().clickButton("gamesPage.servicesCataboomUpdateBtn");
		copiedValue = pageObj.gamesPage().performSecretKeyAction("copy", "");
		Assert.assertEquals(copiedValue, inputValue);
		utils.logit("pass", "Copy functionality is working fine for JWT secret key < 256 chars");
		String uiValue = pageObj.iframeConfigurationPage().getAttributeValue("gamesPage.cataboomJwtSecretKey", "value",
				"");
		Assert.assertEquals(uiValue, dataSet.get("jwtSampleKeyMasked"));
		utils.logit("pass", "JWT Secret key is masked in UI");

		// Verify that Copy button doesn't show up when field is empty
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug2"));
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "Services");
		pageObj.dashboardpage().navigateToTabs("Games");
		uiValue = pageObj.iframeConfigurationPage().getAttributeValue("gamesPage.cataboomJwtSecretKey", "value", "");
		Assert.assertTrue(uiValue.isEmpty());
		boolean isCopyBtnPresent = pageObj.gamesPage().isPresent(utils.getLocatorValue("gamesPage.copyToClipboardBtn"));
		Assert.assertFalse(isCopyBtnPresent);
		utils.logit("pass", "Copy button is not present when JWT Secret key field is empty");
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
