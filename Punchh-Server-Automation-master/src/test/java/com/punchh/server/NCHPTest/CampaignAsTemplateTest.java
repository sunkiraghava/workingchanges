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
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.annotations.Owner;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
// all dependent case with different role logins, single login cannot be used here
public class CampaignAsTemplateTest {

	private static Logger logger = LogManager.getLogger(CampaignAsTemplateTest.class);
	public WebDriver driver;
	private Properties prop;
	private PageObj pageObj;
	private String sTCName, businessId, businessesQuery;
	private String env;
	private String baseUrl;
	private static Map<String, String> dataSet;
	String run = "ui";
	Utilities utils;
	SeleniumUtilities selUtils;
	String massOfferCampaignName;
	public static String CampaignName;
	String massOfferCampaign;
	String massCampaign;
	String massCamp;

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
		selUtils = new SeleniumUtilities(driver);
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==>" + dataSet);
		businessId = dataSet.get("business_id");
		businessesQuery = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + businessId
				+ "'";

	}

	@Test(description = "SQ-T2659 Campaign as Template", groups = {"regression", "nonNightly"}, priority = 0)
	@Owner(name = "Shashank Sharma")
	public void T2659_CampaignAsTemplate() throws Exception {

		CampaignName = "AutomationNotificationCampaign" + CreateDateTime.getTimeDateString();
		// admin user login
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		pageObj.campaignspage().selectMessagedrpValue("Mass Notification");
		pageObj.campaignspage().clickNewCampaignBetaBtn();
		pageObj.campaignsbetaPage().SetCampaignName(CampaignName);

		pageObj.campaignsbetaPage().clickContinueBtn();

		Thread.sleep(5000);

		List<String> setOfWindowIds = selUtils.getAllOpenedWindowId();
		System.out.println("Parent id = " + setOfWindowIds.get(1));
		selUtils.switchToWindow(setOfWindowIds.get(1));

		pageObj.campaignsbetaPage().clickSegmentBtn();
		pageObj.campaignsbetaPage().setSegmentType("custom automation_a");
		pageObj.campaignsbetaPage().clickNextBtn();

		pageObj.campaignsbetaPage().setEmailNotification("Test Title", "Test Body");
		pageObj.campaignsbetaPage().clickNextBtn();
		pageObj.campaignsbetaPage().setStartDateNew(0);
		boolean status1 = pageObj.campaignsbetaPage().verifyStartDateSelectedOrNot("Select");
		Assert.assertTrue(status1, "Start date not selected");
		pageObj.campaignsbetaPage().setTimeZone(dataSet.get("timeZone"));

		pageObj.campaignsbetaPage().setTimeZone("(GMT+05:30) New Delhi (IST)");
		pageObj.campaignsbetaPage().clickNextBtn();

		pageObj.campaignsbetaPage().clickOnSetCampaignAsTemplateCheckbox();

		pageObj.campaignsbetaPage().clickActivateCampaignBtn();

		String currentURL = driver.getCurrentUrl();

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl + "/dashboard");
		Thread.sleep(5000);
		System.out.println("Going to navigate Campaign page ");
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		boolean templateIsVisible = pageObj.campaignsbetaPage().getTemplateIconText(CampaignName);
		Assert.assertTrue(templateIsVisible);

		logger.info("Verified that template icon is displayed ");
		TestListeners.extentTest.get().pass("Verified that template icon is displayed ");

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().navigateToTabs("Miscellaneous Config");
		pageObj.dashboardpage().checkUncheckFlagCockpitDashboard("Enable MFA?", "uncheck");
		pageObj.dashboardpage().updateCheckBox();

	}

	@Test(description = "verifyTemplateIconIsDispalyedForSecondUserCampaign", priority = 1, dependsOnMethods = "T2659_CampaignAsTemplate",groups = "nonNightly")
	@Owner(name = "Shashank Sharma")
	public void verifyTemplateIconIsDispalyedForSecondUserCampaign() throws InterruptedException {
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance(dataSet.get("email"), dataSet.get("password"));
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.campaignspage().selectMessagedrpValue("Mass Notification");

		if (CampaignName != "") {
			boolean templateIsVisible1 = pageObj.campaignsbetaPage().getTemplateIconText(CampaignName);
			Assert.assertTrue(templateIsVisible1,
					CampaignName + " is not visible on " + dataSet.get("email") + " user time line page");

		} else {
			TestListeners.extentTest.get().fail("CampaignName is empty ");
		}

	}

	// Rakhi
	@Test(description = "SQ-T5210 Create template campaign from template page", priority = 2, groups = { "regression",
			"dailyrun" })
	@Owner(name = "Rakhi Rawat")
	public void T5210_campaignTemplateCreation() throws Exception {

		// admin user login
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Templates");

		// mass notification template
		pageObj.newCamHomePage().createCampaignTemplate("Mass campaign", "No");

		String massNotificationCampaign = "Automation Notification Campaign" + CreateDateTime.getTimeDateString();
		String dateTime = CreateDateTime.getCurrentDate() + " 11:00 PM";

		pageObj.signupcampaignpage().setCampaignName(massNotificationCampaign);
		pageObj.signupcampaignpage().EnableAvailableAsTemplateToggle();
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().setAudienceType(dataSet.get("audiance"));
		pageObj.signupcampaignpage().setSegmentType("custom automation");
		pageObj.signupcampaignpage().setPushNotification(dataSet.get("pushNotification"));
		pageObj.signupcampaignpage().setEmailSubject(dataSet.get("emailSubject"));
		pageObj.signupcampaignpage().setEmailTemplate(dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().clickNextButton();
		pageObj.signupcampaignpage().createWhenDetailsMassCampaign("Once", dateTime);
		pageObj.newCamHomePage().searchTemplate(massNotificationCampaign);
		boolean templateIsVisible = pageObj.newCamHomePage().getTemplateText();
		Assert.assertTrue(templateIsVisible, "Mass Notification template did not created");
		logger.info("Mass Notification template is created : " + massNotificationCampaign);
		TestListeners.extentTest.get().pass("Mass Notification template is created : " + massNotificationCampaign);

		// mass offer template
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Templates");
		pageObj.newCamHomePage().createCampaignTemplate("Mass campaign", "Yes");

		String massOfferCampaign = "Automation Mass Offer Campaign" + CreateDateTime.getTimeDateString();
		pageObj.signupcampaignpage().setCampaignName(massOfferCampaign);
		pageObj.signupcampaignpage().EnableAvailableAsTemplateToggle();
		pageObj.signupcampaignpage().clickNextBtn();
		pageObj.signupcampaignpage().setGiftType(dataSet.get("giftType"));
		pageObj.signupcampaignpage().setFixedPoints(dataSet.get("giftPoints"));
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().setAudienceType(dataSet.get("audiance"));
		pageObj.signupcampaignpage().setSegmentType("custom automation");
		pageObj.signupcampaignpage().setPushNotification(dataSet.get("pushNotification"));
		pageObj.signupcampaignpage().setEmailSubject(dataSet.get("emailSubject"));
		pageObj.signupcampaignpage().setEmailTemplate(dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().clickNextButton();
		pageObj.signupcampaignpage().createWhenDetailsMassCampaign("Once", dateTime);
		pageObj.newCamHomePage().searchTemplate(massOfferCampaign);
		boolean templateIsVisible1 = pageObj.newCamHomePage().getTemplateText();
		Assert.assertTrue(templateIsVisible1, "Mass Offer Template did not created");
		logger.info("Mass offer template created : " + massOfferCampaign);
		TestListeners.extentTest.get().pass("Mass offer template created : " + massOfferCampaign);

		// Post checkin message template
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Templates");
		pageObj.newCamHomePage().createCampaignTemplate("Post-checkin", "No");

		String postCheckinMessageCampaign = "Automation Post Checkin Message Campaign"
				+ CreateDateTime.getTimeDateString();
		pageObj.signupcampaignpage().setCampaignName(postCheckinMessageCampaign);
		pageObj.signupcampaignpage().EnableAvailableAsTemplateToggle();
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().createWhomDetailsPostcheckinCampaign(dataSet.get("pushNotification"), "", "");
		pageObj.signupcampaignpage().createWhenDetailsCampaign();
		pageObj.newCamHomePage().searchTemplate(postCheckinMessageCampaign);
		boolean templateIsVisible2 = pageObj.newCamHomePage().getTemplateText();
		Assert.assertTrue(templateIsVisible2, "Post Checkin message Template did not created");
		logger.info("Post Checkin Message Template created : " + postCheckinMessageCampaign);
		TestListeners.extentTest.get().pass("Post Checkin Message Template created : " + postCheckinMessageCampaign);

		// Post checkin offer template
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Templates");
		pageObj.newCamHomePage().createCampaignTemplate("Post-checkin", "Yes");

		String postCheckinOfferCampaign = "Automation Post Checkin Offer Campaign" + CreateDateTime.getTimeDateString();
		pageObj.signupcampaignpage().setCampaignName(postCheckinOfferCampaign);
		pageObj.signupcampaignpage().EnableAvailableAsTemplateToggle();
		pageObj.signupcampaignpage().setGiftType(dataSet.get("giftType1"));
		pageObj.signupcampaignpage().setPostCheckinGiftReason(dataSet.get("giftReason"));
		pageObj.signupcampaignpage().setGiftPoints(dataSet.get("giftPoints"));
		pageObj.signupcampaignpage().clickNextBtn();
		pageObj.signupcampaignpage().createWhomDetailsPostcheckinCampaign(dataSet.get("pushNotification"), "", "");
		pageObj.signupcampaignpage().createWhenDetailsCampaign();
		pageObj.newCamHomePage().searchTemplate(postCheckinOfferCampaign);
		boolean templateIsVisible3 = pageObj.newCamHomePage().getTemplateText();
		Assert.assertTrue(templateIsVisible3, "Post Checkin Offer Template did not created");
		logger.info("Post Checkin Offer Template created : " + postCheckinOfferCampaign);
		TestListeners.extentTest.get().pass("Post Checkin Template Offer created : " + postCheckinOfferCampaign);

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Templates");
		pageObj.newCamHomePage().clickNewCamHomePageBtn();
		pageObj.newCamHomePage().searchCampaign(postCheckinOfferCampaign);
		pageObj.newCamHomePage().deleteCampaign(postCheckinOfferCampaign);
		pageObj.newCamHomePage().searchCampaign(postCheckinMessageCampaign);
		pageObj.newCamHomePage().deleteCampaign(postCheckinMessageCampaign);

	}

	// Rakhi
	@Test(description = "SQ-T5212 Template for Franchise Module + SQ-T5252 Update the create campaign flow for franchisee login", priority = 3)
	@Owner(name = "Rakhi Rawat")
	public void T5212_franchiseModuleTemplate() throws Exception {
		List<String> expectedList = Arrays.asList("Mass campaign", "Post-checkin");

		// admin user login
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().logintoInstance(dataSet.get("username"), dataSet.get("password"));

		/// navigate to NCHP
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().campaignAdvertiseBlock();
		List<String> option = pageObj.newCamHomePage().getOptionsInCreateCampBtn();
		boolean verify = option.containsAll(expectedList) && option.size() == 2;
		Assert.assertTrue(verify, "Expected option list did not matched");
		logger.info("Verified that Franchise admin is able to create only mass and post campaign");
		TestListeners.extentTest.get()
				.pass("Verified that Franchise admin is able to create only mass and post campaign");
		pageObj.newCamHomePage().closeOptionsDailog();
		// use template from template three dots
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Templates");
		pageObj.newCamHomePage().searchTemplate(dataSet.get("templateName"));
		boolean flag = pageObj.newCamHomePage().templateEllipsesOptions(dataSet.get("templateOption"),
				dataSet.get("heading"));
		Assert.assertTrue(flag, "Functionality did not duplicate a campaign from use template");
		logger.info("Functionality duplicate a campaign from use template");
		TestListeners.extentTest.get().pass("Functionality duplicate a campaign from use template");

		// use template from sidepanel CTA
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Templates");
		pageObj.newCamHomePage().searchTemplate(dataSet.get("templateName"));
		boolean flag1 = pageObj.newCamHomePage().useTemplateFromSidePanelCTA(dataSet.get("templateOption"),
				dataSet.get("heading"));
		Assert.assertTrue(flag1, "Functionality did not duplicate a campaign from sidepanel CTA");
		logger.info("Functionality duplicate a campaign from sidepanel CTA");
		TestListeners.extentTest.get().pass("Functionality duplicate a campaign from sidepanel CTA");

	}

	// Rakhi
	@Test(description = "SQ-T5214 Update duplicate classic campaign functionality"
			+ "SQ-T5254 Set default states for CMT flags whenever a new business is created"
			+ "SQ-T5988 Verify the UI changes for Reward Value,Banked Redeemable, Redemption mark Fields for Points Convert to Currency", priority = 4, groups = {
					"unstable", "regression", "dailyrun" })
	@Owner(name = "Rakhi Rawat")
	public void T5214_duplicateClassicCampaignFunctionality() throws Exception {

		String couponCampaignName = "Coupon Campaign" + CreateDateTime.getTimeDateString();
		String tagName = "AutoTag" + CreateDateTime.getTimeDateString();

		String expValue = DBUtils.executeQueryAndGetColumnValue(env, businessesQuery, "preferences");

		// Set live flag to false
		boolean status1 = DBUtils.updateBusinessesPreference(env, expValue, "false", dataSet.get("dbFlag1"),
				businessId);
		Assert.assertTrue(status1, dataSet.get("dbFlag1") + " value is not updated to false");
		// Set went_live to false
		boolean status2 = DBUtils.updateBusinessesPreference(env, expValue, "false", dataSet.get("dbFlag2"),
				businessId);
		Assert.assertTrue(status2, dataSet.get("dbFlag2") + " value is not updated to false");

		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// update membership
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Has Membership Levels?", "check");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Membership Level Bump on Edge?", "check");
		pageObj.dashboardpage().clickOnUpdateButton();

		// Set checkin expiry Point Based Points Convert To> Rewards
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");
		pageObj.dashboardpage().navigateToTabsNew("Checkin Earning");
		pageObj.earningPage().setProgramType(dataSet.get("earningType1"));
		pageObj.earningPage().setPointsConvertTo("Currency");
		pageObj.earningPage().updateConfiguration();

		// navigate to membership
		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		pageObj.settingsPage().clickMemberLevel(dataSet.get("membership"));

		for (int i = 1; i < 3; i++) {
			Boolean flag = pageObj.settingsPage().verifyFiledAvailableOrNot(dataSet.get("field" + i));
			Assert.assertTrue(flag,
					dataSet.get("field" + i) + " is not visible for Points Convert To Currency earning type");
			logger.info(
					"Verified " + dataSet.get("field" + i) + " is visible for Points Convert To Currency earning type");
			TestListeners.extentTest.get().pass(
					"Verified " + dataSet.get("filed" + i) + " is visible for Points Convert To Currency earning type");
		}
		Boolean flag1 = pageObj.settingsPage().verifyFiledAvailableOrNot(dataSet.get("field3"));
		Assert.assertFalse(flag1, dataSet.get("field3") + " is visible for Points Convert To Currency earning type");
		logger.info(
				"Verified " + dataSet.get("field3") + " is not visible for Points Convert To Currency earning type");
		TestListeners.extentTest.get().pass(
				"Verified " + dataSet.get("field3") + " is not visible for Points Convert To Currency earning type");

		// validating that Campaign Homepage (CHP) should be opened by default
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		String val = pageObj.newCamHomePage().createTags(tagName);
		Assert.assertEquals(val, "Tag name is limited to 50 characters.",
				"Tag name range error message did not matched");
		logger.info("Upon clicking on the campaigns link, the new Campaign Homepage (CHP) opened by default.");
		TestListeners.extentTest.get()
				.pass("Upon clicking on the campaigns link, the new Campaign Homepage (CHP) opened by default.");
		String msg = pageObj.newCamHomePage().deleteTag(tagName);
		Assert.assertEquals(msg, "Tag deleted successfully", "Tag did not deleted");
		// pageObj.newCamHomePage().closeMangeTagFrame();

		pageObj.newCamHomePage().clickSwitchToClassicBtn();
		// Select offer dropdown value and create coupon campaign
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().createWhatDetailsCoupanCampaign(couponCampaignName);
		pageObj.signupcampaignpage().setCouponCampaignUsageType(dataSet.get("usageType"));
		pageObj.signupcampaignpage().setCouponCampaignCodeGenerationType(dataSet.get("codeType"));
		pageObj.signupcampaignpage().setCouponCampaignGuestUsagewithGiftAmount(dataSet.get("noOfGuests"),
				dataSet.get("usagePerGuest"), dataSet.get("giftType"), dataSet.get("amount"), "", "", false);
		pageObj.signupcampaignpage().setStartDate();
		pageObj.signupcampaignpage().setEndDateforCouponCampaign(1);
		pageObj.signupcampaignpage().createWhenDetailsCampaign();
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "Campaign created success message did not displayed....");
		TestListeners.extentTest.get().pass("Coupon campaign created successfuly");

		// search and open classic campaign
		pageObj.campaignspage().searchAndSelectCamapign(couponCampaignName);
		String name = pageObj.campaignspage().createDuplicateCampaignOnClassicPage(couponCampaignName, "Edit");
		Assert.assertEquals(name, couponCampaignName + " - copy");
		logger.info("Campaign name is prefilled as : " + couponCampaignName + " - copy");
		TestListeners.extentTest.get().pass("Campaign name is prefilled as : " + couponCampaignName + " - copy");

		// cancel duplicating the campaign with no option
		String text = pageObj.campaignspage().verifyModalPopupOptions("no");
		Assert.assertEquals(text, "Do you want to save this campaign as a draft before leaving?",
				"Modal PopUp does not appear after clicking on cancel button");
		logger.info("Modal PopUp appeared after clicking on cancel button");
		TestListeners.extentTest.get().pass("Modal PopUp appeared after clicking on cancel button");

		// cancel duplicating the campaign with yes option
		pageObj.campaignspage().searchAndSelectCamapign(couponCampaignName);
		pageObj.campaignspage().createDuplicateCampaignOnClassicPage(couponCampaignName, "Edit");
		String text1 = pageObj.campaignspage().verifyModalPopupOptions("yes");
		Assert.assertEquals(text1, "Do you want to save this campaign as a draft before leaving?",
				"Modal PopUp does not appear after clicking on cancel button");
		logger.info("Modal PopUp appeared after clicking on cancel button");
		TestListeners.extentTest.get().pass("Modal PopUp appeared after clicking on cancel button");

		// create new campaign to verify if Modal PopUp appears or not after clicking on
		// cancel button
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().setCampaignName(couponCampaignName);

		// verify of modal popup appears or not
		boolean flag = pageObj.campaignspage().verifyIfModalPopupAppearsOrNot();
		Assert.assertFalse(flag, "Modal Popup appeared after clicking on cancel button");
		logger.info("Modal PopUp does not appear after clicking on cancel button");
		TestListeners.extentTest.get().pass("Modal PopUp does not appear after clicking on cancel button");

	}

	@Test(description = "SQ-T5213 Template for Franchise Module"
			+ "SQ-T5253 Template for Franchise Module", priority = 5)
	@Owner(name = "Rakhi Rawat")
	public void T5213_verifyTemplatePageFunctionality() throws Exception {

		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().checkEnableFranchiseesFlag();

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Templates");

		// verify Template tab filtering
		// Post Checkin tab values
		List<String> massList = pageObj.newCamHomePage().getCampaignType("Mass");
		boolean status = massList.stream().allMatch("Mass"::equals);
		Assert.assertTrue(status, "Other campaign type found in mass tab NCHP");
		logger.info("Mass tab campaigns type validated");
		TestListeners.extentTest.get().pass("Mass tab campaigns type validated");

		// Post Checkin tab values
		List<String> postCheckinList = pageObj.newCamHomePage().getCampaignType("Post-checkin");
		boolean status1 = postCheckinList.stream().allMatch("Post-checkin"::equals);
		Assert.assertTrue(status1, "Other campaign type found in Post Checkin tab NCHP");
		logger.info("Post Checkin tab campaigns type validated");
		TestListeners.extentTest.get().pass("Post Checkin tab campaigns type validated");

		// All tab values
		List<String> allOptions = List.of("Mass", "Post-checkin");
		List<String> ALlList = pageObj.newCamHomePage().getCampaignType("All");
		System.out.println(ALlList);
		boolean status2 = ALlList.stream().allMatch(allOptions::contains);
		Assert.assertTrue(status2, "Other campaign type found in Post Checkin tab NCHP");
		logger.info("ALl tab campaigns type validated");
		TestListeners.extentTest.get().pass("ALl tab campaigns type validated");

		pageObj.newCamHomePage().getCampaignType("All");

		// verify bulk action for deletion
		pageObj.newCamHomePage().selectAllCampaignCheckBox();
		// validate all rows selected
		int total = pageObj.newCamHomePage().getBulkSelectedCampaignSCount();
		int count = pageObj.newCamHomePage().getAllSelectedCampaignListSize();
		Assert.assertEquals(count, total, "Bulk selected capaigns count did not matched");
		logger.info("Bulk action is working fine");
		TestListeners.extentTest.get().pass("Bulk action is working fine");

		pageObj.newCamHomePage().unSelectAllCampaignCheckBox();

		// verify search functionality
		pageObj.newCamHomePage().searchTemplate(dataSet.get("templateName"));
		boolean templateIsVisible = pageObj.newCamHomePage().getTemplateText();
		Assert.assertTrue(templateIsVisible, "Mass Notification template is not visible on New Cam Home Page");
		logger.info("Search functionality is working fine");
		TestListeners.extentTest.get().pass("Search functionality is working fine");

		// template three dots(ellipsis)
		boolean flag = pageObj.newCamHomePage().templateEllipsesOptions(dataSet.get("templateOption"),
				dataSet.get("heading"));
		Assert.assertTrue(flag, "Functionality did not duplicate a campaign from use template");
		logger.info("Template Ellipsis functionality is working fine");
		TestListeners.extentTest.get().pass("Template Ellipsis functionality is working fine");

		pageObj.newCamHomePage().navigateToBackPage();
		// template table content
		pageObj.newCamHomePage().verifyTableContentPresence("Name");
		pageObj.newCamHomePage().verifyTableContentPresence("Type");
		pageObj.newCamHomePage().verifyTableContentPresence("Segment & Gift");

	}

	// Rakhi
	@Test(description = "SQ-T5241 Campaign Actions for status of On Hold", priority = 6)
	@Owner(name = "Rakhi Rawat")
	public void T5241_ValidateDateCampaignActionsForHoldStatusPartOne() throws InterruptedException, ParseException {

		massCamp = "Automation Mass Offer Campaign" + CreateDateTime.getTimeDateString();

		// admin user login
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().logintoInstance(dataSet.get("userName"), dataSet.get("passWord"));

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().campaignAdvertiseBlock();
		pageObj.newCamHomePage().createMassCampaignCHP("Mass campaign", "Yes");

		String dateTime = CreateDateTime.getTomorrowDate() + " 11:00 PM";
		pageObj.signupcampaignpage().setCampaignName(massCamp);
		pageObj.signupcampaignpage().clickNextBtn();
		pageObj.signupcampaignpage().setGiftType(dataSet.get("giftType"));
		pageObj.signupcampaignpage().setFixedPoints(dataSet.get("giftPoints"));
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().setAudienceType(dataSet.get("audiance"));
		pageObj.signupcampaignpage().setSegmentType("TestSegment");
		pageObj.signupcampaignpage().setPushNotification(dataSet.get("pushNotification"));
		pageObj.signupcampaignpage().setEmailSubject(dataSet.get("emailSubject"));
		pageObj.signupcampaignpage().setEmailTemplate(dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().clickNextButton();
		pageObj.signupcampaignpage().createWhenDetailsMassCampaign("Once", dateTime);

		pageObj.newCamHomePage().searchCampaign(massCamp);
		String status = pageObj.newCamHomePage().getCampaignStatus();
		Assert.assertEquals(status, "Pending Approval", "Campaign is not in Pending Approval status");
		logger.info("Verfied that campaign is in Pending Approval status");
		TestListeners.extentTest.get().pass("Verfied that campaign is in Pending Approval status");
	}

	@Test(description = "SQ-T5241 Campaign Actions for status of On Hold", dependsOnMethods = "T5241_ValidateDateCampaignActionsForHoldStatusPartOne")
	@Owner(name = "Rakhi Rawat")
	public void T5241_ValidateDateCampaignActionsForHoldStatusPartTwo() throws InterruptedException, ParseException {

		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().switchTab(dataSet.get("tab"));
		pageObj.newCamHomePage().moveToBalckoutdates();
		pageObj.newCamHomePage().createBlackoutDateFromNewCHP();
		// search campaign and validate the available options in the action menu
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().searchCampaign(massCamp);
		pageObj.newCamHomePage().selectCampaignOption("Edit");
		pageObj.signupcampaignpage().editCampaignFromNewCHP();
		pageObj.newCamHomePage().searchCampaign(massCamp);
		// validate campaign options
		pageObj.newCamHomePage().selectCampaignOption("Tag");
		pageObj.newCamHomePage().closeOptionsDailog();

		pageObj.newCamHomePage().selectCampaignOption("Duplicate");
		String duplicateTitle = pageObj.newCamHomePage().getNewPageTitleClassic();
		pageObj.newCamHomePage().navigateToBackPage();
		Assert.assertEquals(duplicateTitle, "New Mass Offer Campaign", "Duplicate option page title did not matched");

		pageObj.newCamHomePage().selectCampaignOption("Edit");
		String editTitle = pageObj.newCamHomePage().getNewPageTitleClassic();
		pageObj.newCamHomePage().navigateToBackPage();
		Assert.assertEquals(editTitle, "Edit " + massCamp, "Edit option page title did not matched");

		pageObj.newCamHomePage().selectCampaignOption("View summary");
		String viewSummaryTitle = pageObj.newCamHomePage().getNewPageTitleClassic();
		pageObj.newCamHomePage().navigateToBackPage();
		Assert.assertEquals(viewSummaryTitle, massCamp, "View Summary option page title did not matched");

		pageObj.newCamHomePage().selectCampaignOption("Audit log");
		String auditLogTitle = pageObj.newCamHomePage().getNewPageTitleClassic();
		pageObj.newCamHomePage().navigateToBackPage();
		Assert.assertTrue(auditLogTitle.contains("Audit Logs for"), "Audit Log option page title did not matched");

		// verify call to action from campaign SidePanel
		pageObj.newCamHomePage().searchCampaign(massCamp);
		pageObj.newCamHomePage().checkSidepanelCTAoptions(massCamp, "View summary");
		String viewSummary = pageObj.newCamHomePage().getNewPageTitleClassic();
		pageObj.newCamHomePage().navigateToBackPage();
		Assert.assertEquals(viewSummary, massCamp, "View Summary option page title did not matched");

		pageObj.newCamHomePage().checkSidepanelCTAoptions(massCamp, "Edit");
		String edit = pageObj.newCamHomePage().getNewPageTitleClassic();
		pageObj.newCamHomePage().navigateToBackPage();
		Assert.assertEquals(edit, "Edit " + massCamp, "Edit option page title did not matched");

		pageObj.newCamHomePage().deleteCampaign(massCamp);
	}

	// Rakhi
	@Test(description = "SQ-T5240 Campaign Actions for status of Pending Approval")
	@Owner(name = "Rakhi Rawat")
	public void T5240_ValidateDateCampaignActionsForPendingApprovalStatusPartOne() throws Exception {

		massOfferCampaign = "Automation Mass Offer Campaign" + CreateDateTime.getTimeDateString();

		// admin user login
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().logintoInstance(dataSet.get("userName"), dataSet.get("passWord"));
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().campaignAdvertiseBlock();
		pageObj.newCamHomePage().createMassCampaignCHP("Mass campaign", "Yes");

		String dateTime = CreateDateTime.getCurrentDate() + " 11:00 PM";
		pageObj.signupcampaignpage().setCampaignName(massOfferCampaign);
		pageObj.signupcampaignpage().clickNextBtn();
		pageObj.signupcampaignpage().setGiftType(dataSet.get("giftType"));
		pageObj.signupcampaignpage().setFixedPoints(dataSet.get("giftPoints"));
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().setAudienceType(dataSet.get("audiance"));
		pageObj.signupcampaignpage().setSegmentType("TestSegment");
		pageObj.signupcampaignpage().setPushNotification(dataSet.get("pushNotification"));
		pageObj.signupcampaignpage().setEmailSubject(dataSet.get("emailSubject"));
		pageObj.signupcampaignpage().setEmailTemplate(dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().clickNextButton();
		pageObj.signupcampaignpage().createWhenDetailsMassCampaign("Once", dateTime);

		// search campaign and validate the available options in the action menu
		pageObj.newCamHomePage().searchCampaign(massOfferCampaign);
		String status = pageObj.newCamHomePage().getCampaignStatus();
		Assert.assertEquals(status, "Pending Approval", "Campaign is not in Pending Approval status");
		logger.info("Verfied that campaign is in Pending Approval status");
		TestListeners.extentTest.get().pass("Verfied that campaign is in Pending Approval status");

		// validate campaign options
		pageObj.newCamHomePage().selectCampaignOption("Delete");
		pageObj.newCamHomePage().closeOptionsDailog();

		pageObj.newCamHomePage().selectCampaignOption("Duplicate");
		String duplicateTitle = pageObj.newCamHomePage().getNewPageTitleClassic();
		pageObj.newCamHomePage().navigateToBackPage();
		Assert.assertEquals(duplicateTitle, "New Mass Offer Campaign", "Duplicate option page title did not matched");

		pageObj.newCamHomePage().selectCampaignOption("Edit");
		String editTitle = pageObj.newCamHomePage().getNewPageTitleClassic();
		pageObj.newCamHomePage().navigateToBackPage();
		Assert.assertEquals(editTitle, "Edit " + massOfferCampaign, "Edit option page title did not matched");

		pageObj.newCamHomePage().selectCampaignOption("View summary");
		String viewSummaryTitle = pageObj.newCamHomePage().getNewPageTitleClassic();
		pageObj.newCamHomePage().navigateToBackPage();
		Assert.assertEquals(viewSummaryTitle, massOfferCampaign, "View Summary option page title did not matched");

		pageObj.newCamHomePage().selectCampaignOption("Audit log");
		String auditLogTitle = pageObj.newCamHomePage().getNewPageTitleClassic();
		pageObj.newCamHomePage().navigateToBackPage();
		Assert.assertTrue(auditLogTitle.contains("Audit Logs for"), "Audit Log option page title did not matched");

		// verify call to action from campaign SidePanel
		pageObj.newCamHomePage().searchCampaign(massOfferCampaign);
		pageObj.newCamHomePage().checkSidepanelCTAoptions(massOfferCampaign, "View summary");
		String viewSummary = pageObj.newCamHomePage().getNewPageTitleClassic();
		pageObj.newCamHomePage().navigateToBackPage();
		Assert.assertEquals(viewSummary, massOfferCampaign, "View Summary option page title did not matched");

		pageObj.newCamHomePage().checkSidepanelCTAoptions(massOfferCampaign, "Edit");
		String edit = pageObj.newCamHomePage().getNewPageTitleClassic();
		pageObj.newCamHomePage().navigateToBackPage();
		Assert.assertEquals(edit, "Edit " + massOfferCampaign, "Edit option page title did not matched");

	}

	// Rakhi
	@Test(description = "SQ-T5240 Campaign Actions for status of Pending Approval", dependsOnMethods = "T5240_ValidateDateCampaignActionsForPendingApprovalStatusPartOne")
	@Owner(name = "Rakhi Rawat")
	public void T5240_ValidateDateCampaignActionsForPendingApprovalStatusPartTwo() throws Exception {

		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// search campaign and validate the available options in the action menu
		pageObj.newCamHomePage().searchCampaign(massOfferCampaign);

		// validate campaign options
		pageObj.newCamHomePage().selectCampaignOption("Delete");
		pageObj.newCamHomePage().closeOptionsDailog();

		pageObj.newCamHomePage().selectCampaignOption("Duplicate");
		String duplicateTitle1 = pageObj.newCamHomePage().getNewPageTitleClassic();
		pageObj.newCamHomePage().navigateToBackPage();
		Assert.assertEquals(duplicateTitle1, "New Mass Offer Campaign", "Duplicate option page title did not matched");

		pageObj.newCamHomePage().selectCampaignOption("Edit");
		String editTitle1 = pageObj.newCamHomePage().getNewPageTitleClassic();
		pageObj.newCamHomePage().navigateToBackPage();
		Assert.assertEquals(editTitle1, "Edit " + massOfferCampaign, "Edit option page title did not matched");

		pageObj.newCamHomePage().selectCampaignOption("Review");
		String reviewTitle = pageObj.newCamHomePage().getNewPageTitleClassic();
		pageObj.newCamHomePage().navigateToBackPage();
		Assert.assertEquals(reviewTitle, massOfferCampaign, "Review option page title did not matched");

		pageObj.newCamHomePage().selectCampaignOption("Audit log");
		String auditLogTitle1 = pageObj.newCamHomePage().getNewPageTitleClassic();
		pageObj.newCamHomePage().navigateToBackPage();
		Assert.assertTrue(auditLogTitle1.contains("Audit Logs for"), "Audit Log option page title did not matched");

		// verify call to action from campaign SidePanel
		pageObj.newCamHomePage().searchCampaign(massOfferCampaign);
		pageObj.newCamHomePage().checkSidepanelCTAoptions(massOfferCampaign, "Review");
		String review = pageObj.newCamHomePage().getNewPageTitleClassic();
		pageObj.newCamHomePage().navigateToBackPage();
		Assert.assertEquals(review, massOfferCampaign, "Review option page title did not matched");

		pageObj.newCamHomePage().checkSidepanelCTAoptions(massOfferCampaign, "Edit");
		String edit1 = pageObj.newCamHomePage().getNewPageTitleClassic();
		pageObj.newCamHomePage().navigateToBackPage();
		Assert.assertEquals(edit1, "Edit " + massOfferCampaign, "Edit option page title did not matched");

		pageObj.newCamHomePage().deleteCampaign(massOfferCampaign);
	}

	// Rakhi
	@Test(description = "SQ-T5251 Campaign Actions for status of Disapproved")
	@Owner(name = "Rakhi Rawat")
	public void T5251_ValidateDateCampaignActionsForDisapprovedStatusPartOne()
			throws InterruptedException, ParseException {

		// admin user login
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().logintoInstance(dataSet.get("userName"), dataSet.get("passWord"));

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().campaignAdvertiseBlock();
		pageObj.newCamHomePage().createMassCampaignCHP("Mass campaign", "Yes");

		massCampaign = "Automation Mass Offer Campaign" + CreateDateTime.getTimeDateString();
		String dateTime = CreateDateTime.getCurrentDate() + " 11:00 PM";
		pageObj.signupcampaignpage().setCampaignName(massCampaign);
		pageObj.signupcampaignpage().clickNextBtn();
		pageObj.signupcampaignpage().setGiftType(dataSet.get("giftType"));
		pageObj.signupcampaignpage().setFixedPoints(dataSet.get("giftPoints"));
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().setAudienceType(dataSet.get("audiance"));
		pageObj.signupcampaignpage().setSegmentType("TestSegment");
		pageObj.signupcampaignpage().setPushNotification(dataSet.get("pushNotification"));
		pageObj.signupcampaignpage().setEmailSubject(dataSet.get("emailSubject"));
		pageObj.signupcampaignpage().setEmailTemplate(dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().clickNextButton();
		pageObj.signupcampaignpage().createWhenDetailsMassCampaign("Once", dateTime);

		pageObj.newCamHomePage().searchCampaign(massCampaign);
		String status = pageObj.newCamHomePage().getCampaignStatus();
		Assert.assertEquals(status, "Pending Approval", "Campaign is not in Pending Approval status");
		logger.info("Verfied that campaign is in Pending Approval status");
		TestListeners.extentTest.get().pass("Verfied that campaign is in Pending Approval status");

	}

	@Test(description = "SQ-T5251 Campaign Actions for status of Disapproved", dependsOnMethods = "T5251_ValidateDateCampaignActionsForDisapprovedStatusPartOne")
	@Owner(name = "Rakhi Rawat")
	public void T5251_ValidateDateCampaignActionsForDisapprovedStatusPartTwo()
			throws InterruptedException, ParseException {

		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().searchCampaign(massCampaign);
		// Disapprove selected campaign
		pageObj.newCamHomePage().selectCampaignOption("Review");
		pageObj.newCamHomePage().approveDisapproveCampaign("Disapprove", "Disapprove Campaign");

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().searchCampaign(massCampaign);
		// validate campaign options
		pageObj.newCamHomePage().selectCampaignOption("Delete");
		pageObj.newCamHomePage().closeOptionsDailog();

		pageObj.newCamHomePage().selectCampaignOption("Duplicate");
		String duplicateTitle = pageObj.newCamHomePage().getNewPageTitleClassic();
		pageObj.newCamHomePage().navigateToBackPage();
		Assert.assertEquals(duplicateTitle, "New Mass Offer Campaign", "Duplicate option page title did not matched");

		pageObj.newCamHomePage().selectCampaignOption("Edit");
		String editTitle = pageObj.newCamHomePage().getNewPageTitleClassic();
		pageObj.newCamHomePage().navigateToBackPage();
		Assert.assertEquals(editTitle, "Edit " + massCampaign, "Edit option page title did not matched");

		pageObj.newCamHomePage().selectCampaignOption("View summary");
		String viewSummaryTitle = pageObj.newCamHomePage().getNewPageTitleClassic();
		pageObj.newCamHomePage().navigateToBackPage();
		Assert.assertEquals(viewSummaryTitle, massCampaign, "View Summary option page title did not matched");

		pageObj.newCamHomePage().selectCampaignOption("Audit log");
		String auditLogTitle = pageObj.newCamHomePage().getNewPageTitleClassic();
		pageObj.newCamHomePage().navigateToBackPage();
		Assert.assertTrue(auditLogTitle.contains("Audit Logs for"), "Audit Log option page title did not matched");

		// verify call to action from campaign SidePanel
		pageObj.newCamHomePage().searchCampaign(massCampaign);
		pageObj.newCamHomePage().checkSidepanelCTAoptions(massCampaign, "View summary");
		String viewSummary = pageObj.newCamHomePage().getNewPageTitleClassic();
		pageObj.newCamHomePage().navigateToBackPage();
		Assert.assertEquals(viewSummary, massCampaign, "View Summary option page title did not matched");

		pageObj.newCamHomePage().checkSidepanelCTAoptions(massCampaign, "Edit");
		String edit = pageObj.newCamHomePage().getNewPageTitleClassic();
		pageObj.newCamHomePage().navigateToBackPage();
		Assert.assertEquals(edit, "Edit " + massCampaign, "Edit option page title did not matched");

		pageObj.newCamHomePage().deleteCampaign(massCampaign);
	}

	// @Author = Hardik Bhardwaj
	@Test(description = "SQ-T5388 Add Template button to CMT", groups = { "regression", "dailyrun" })
	@Owner(name = "Hardik Bhardwaj")
	public void T5388_verifyTemplateFunctionality_part1() throws InterruptedException {

		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();

		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// validate tags is created
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickNewCamHomePageBtn();
		pageObj.newCamHomePage().clickOnTemplate();
		utils.waitTillPagePaceDone();

		String currentURL = driver.getCurrentUrl();
		boolean checkTemplate = false;
		if (currentURL.contains(dataSet.get("templateUrl"))) {
			checkTemplate = true;
		}
		Assert.assertTrue(checkTemplate, "unable to navigates to template page for Super Admin");
		logger.info("Navigates to template page for Super Admin");
		TestListeners.extentTest.get().pass("Navigates to template page for Super Admin");

		String pageName = pageObj.newCamHomePage().checkCurrentCampaignPage();
		Assert.assertEquals(pageName, "Campaign Templates",
				"unable to Navigates to template page from NCHP for Super Admin");
		logger.info("Navigates to template page from NCHP for Super Admin");
		TestListeners.extentTest.get().pass("Navigates to template page from NCHP for Super Admin");

	}

	@Test(description = "SQ-T5388 Add Template button to CMT", groups = { "regression", "dailyrun" })
	@Owner(name = "Amit Kumar")
	public void T5388_verifyTemplateFunctionality_part2() throws InterruptedException {
		// admin user login
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().logintoInstance(dataSet.get("username"), dataSet.get("password"));

		/// navigate to NCHP
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		utils.longWaitInSeconds(2);
		pageObj.newCamHomePage().campaignAdvertiseBlock();
		utils.longWaitInSeconds(2);
		pageObj.newCamHomePage().campaignAdvertiseBlock();
		utils.longWaitInSeconds(2);
		pageObj.newCamHomePage().campaignAdvertiseBlock();
		pageObj.newCamHomePage().clickOnTemplate();
		utils.waitTillPagePaceDone();

		String currentURL2 = driver.getCurrentUrl();
		boolean checkTemplate2 = false;
		if (currentURL2.contains(dataSet.get("templateUrl"))) {
			checkTemplate2 = true;
		}
		Assert.assertTrue(checkTemplate2, "unable to navigates to template page for Franchise Admin");
		logger.info("Navigates to template page for Franchise Admin");
		TestListeners.extentTest.get().pass("Navigates to template page for Franchise Admin");

		String pageName2 = pageObj.newCamHomePage().checkCurrentCampaignPage();
		Assert.assertEquals(pageName2, "Campaign Templates",
				"unable to Navigates to template page from NCHP for Franchise Admin");
		logger.info("Navigates to template page from NCHP for Franchise Admin");
		TestListeners.extentTest.get().pass("Navigates to template page from NCHP for Franchise Admin");
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}
}
