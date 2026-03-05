package com.punchh.server.deprecatedTest;

import java.lang.reflect.Method;
import java.text.ParseException;
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
import org.testng.asserts.SoftAssert;

import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
// no need to keep this class in regression suite as discussed with  khushbu basic validations now not needed so moving to depricated
public class NewModalMassCampaign {

	private static Logger logger = LogManager.getLogger(NewModalMassCampaign.class);
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

	@Test(description = "SQ-T2591,PS-T80 Create Mass Offer Campaign From New Modal", groups = { "regression",
			"unstable", "dailyrun" }, priority = 0)
	public void T2591_verifyCreateMassOfferCampaignFromNewModal() throws InterruptedException, ParseException {

		campaignName = "Automation Massoffer Campaign" + CreateDateTime.getTimeDateString();
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value
		pageObj.campaignspage().selectOfferdrpValue("Mass Offer");

		// validate error msg for blank name field
		pageObj.campaignspage().clickNewCampaignBetaBtn();
		pageObj.campaignsbetaPage().clickContinueBtn();
		String nameFieldErrormsg = pageObj.campaignsbetaPage().elementFieldErrorMessage();
		Assert.assertEquals(nameFieldErrormsg, "Enter a campaign name.",
				"Campaign name field error message text did not matched");
		pageObj.campaignsbetaPage().clickCancelBtn();

		pageObj.campaignspage().clickNewCampaignBetaBtn();
		pageObj.campaignsbetaPage().SetCampaignName(campaignName);

		pageObj.campaignsbetaPage().clickContinueBtn();
		Thread.sleep(2000);
		utils.switchToWindow();

		pageObj.campaignsbetaPage().clickSummaryTab();
		pageObj.campaignsbetaPage().clickActivateCampaignBtn();
		// Who tab errors validation
		pageObj.campaignsbetaPage().clickWhoTab();
		String givenCampaignName = pageObj.campaignsbetaPage().givenCampaignName();
		String whoTitle = pageObj.campaignsbetaPage().whoTitle();
		// boolean infoStatus = pageObj.campaignsbetaPage().givenCampaigninfoLabel();
		Assert.assertEquals(givenCampaignName, campaignName, "Given campaign name did not matched");
		Assert.assertEquals(whoTitle, "Who is the target audience?", "Who title did not matched");
		// Assert.assertTrue(infoStatus, "Campaign info label did not appeared on who
		// tab");

		pageObj.campaignsbetaPage().clickLocationBtn();
		String locationFieldErrormsg = pageObj.campaignsbetaPage().elementFieldErrorMessage();
		Assert.assertEquals(locationFieldErrormsg, "Select a Location.",
				"Location field error message text did not matched");

		pageObj.campaignsbetaPage().clickSegmentBtn();
		String segmentFieldErrormsg = pageObj.campaignsbetaPage().elementFieldErrorMessage();
		Assert.assertEquals(segmentFieldErrormsg, "Select a Segment.",
				"Segment field error message text did not matched");

		// reachability section validation
		pageObj.campaignsbetaPage().setSegmentType(dataSet.get("segment"));
		pageObj.campaignsbetaPage().validateReachablityandControlGroup();
		pageObj.campaignsbetaPage().clickNextBtn();

		// What tab errors validation
		pageObj.campaignsbetaPage().clickWhatTab();
		String whatTitle = pageObj.campaignsbetaPage().whatTitle();
		Assert.assertEquals(whatTitle, "What is being offered?", "What title did not matched");
		// Enable Promos? in cockpit dashboard should be enabled
		boolean promostatus = pageObj.campaignsbetaPage().checkPrormoBtn();
		Assert.assertFalse(promostatus, "Promo button in what details should not appear but appearing");

		pageObj.campaignsbetaPage().clickCurrencyBtn();
		String currencyFieldErrormsg = pageObj.campaignsbetaPage().elementFieldErrorMessage();
		Assert.assertEquals(currencyFieldErrormsg, "Enter a valid amount.",
				"Currency field error message text did not matched");

		pageObj.campaignsbetaPage().clickCouponBtn();
		String couponFieldErrormsg = pageObj.campaignsbetaPage().elementFieldErrorMessage();
		Assert.assertEquals(couponFieldErrormsg, "Select a coupon or other type of offer.",
				"Coupon field error message text did not matched");

		pageObj.campaignsbetaPage().clickPointsBtn();
		String pointsFieldErrormsg = pageObj.campaignsbetaPage().elementFieldErrorMessage();
		Assert.assertEquals(pointsFieldErrormsg, "Enter an amount.", "Points field error message text did not matched");

		pageObj.campaignsbetaPage().clickRedeemablesBtn();
		String redeemablesFieldErrormsg = pageObj.campaignsbetaPage().elementFieldErrorMessage();
		Assert.assertEquals(redeemablesFieldErrormsg, "Select a redeemable or other type of offer.",
				"Redeemables field error message text did not matched");
		// Set redeemable

		pageObj.campaignsbetaPage().setRedeemable(dataSet.get("redemable"));
		pageObj.campaignsbetaPage().setOfferReason("Support Offer");
		// boolean redeemableImage = pageObj.campaignsbetaPage().redeemableImage();
		// Assert.assertTrue(redeemableImage, "Redeemable image is not dispalyed");

		pageObj.campaignsbetaPage().clickIncludeSurvey();
		String includeSurveyFieldErrormsg = pageObj.campaignsbetaPage().elementFieldErrorMessage();
		Assert.assertEquals(includeSurveyFieldErrormsg, "Select a survey.",
				" Include Surveyfield error message text did not matched");
		pageObj.campaignsbetaPage().clickIncludeSurvey();

		pageObj.campaignsbetaPage().clickIncludeAdvertisedCampaign();
		String includeAdvertisedCampaignFieldErrormsg = pageObj.campaignsbetaPage().elementFieldErrorMessage();
		Assert.assertEquals(includeAdvertisedCampaignFieldErrormsg, "Select an Advertised Campaign.",
				"Include Advertised Campaign field error message text did not matched");
		pageObj.campaignsbetaPage().clickIncludeAdvertisedCampaign();

		String richMessageFieldErrormsg = pageObj.campaignsbetaPage().richMessageFieldErrorMessage();
		String smsFieldErrormsg = pageObj.campaignsbetaPage().smsMessageFieldErrorMessage();
		String pnFieldErrormsg = pageObj.campaignsbetaPage().pnMessageFieldErrorMessage();
		String emailFieldErrormsg = pageObj.campaignsbetaPage().emailMessageFieldErrorMessage();
		Assert.assertEquals(richMessageFieldErrormsg, "Rich Message Style not selected",
				"Rich message style error message text did not matched");
		Assert.assertEquals(smsFieldErrormsg, "Enter SMS Body Text.", " SMS body error message text did not matched");
		Assert.assertEquals(pnFieldErrormsg, "Enter body text for your push notification.",
				" PN bodyerror message text did not matched");
		Assert.assertEquals(emailFieldErrormsg, "Enter a subject line.", "Email error message text did not matched");

		pageObj.campaignsbetaPage().setEmailNotification(campaignName, campaignName);
		pageObj.campaignsbetaPage().clickNextBtn();

		// When tab errors validation
		pageObj.campaignsbetaPage().clickWhenTab();
		String whenTitle = pageObj.campaignsbetaPage().whenTitle();
		Assert.assertEquals(whenTitle, "When should the campaign be sent?", "What title did not matched");

		String startDateFieldErrormsg = pageObj.campaignsbetaPage().elementFieldErrorMessage();
		boolean blackoutDates = pageObj.campaignsbetaPage().blackoutDates();
		Assert.assertEquals(startDateFieldErrormsg, "Enter a date.",
				"Frequency Once Start date field error message text did not matched");
		Assert.assertTrue(blackoutDates, "Blackout Dates is not dispalyed");

		pageObj.campaignsbetaPage().setFrequency("Monthly");
		List<String> monthlyFrequencyerrors = pageObj.campaignsbetaPage().getListofErrors();
		System.out.println(monthlyFrequencyerrors);
		Assert.assertEquals(monthlyFrequencyerrors.get(0), "Select a repeat frequency.",
				"Repeat Every # Month(s) error text did not matched");
		Assert.assertEquals(monthlyFrequencyerrors.get(1), "Select a Day of the Month.",
				"Day of Month error text did not matched");
		Assert.assertEquals(monthlyFrequencyerrors.get(2), "Enter a date.", "Start Date error text did not matched");
		Assert.assertEquals(monthlyFrequencyerrors.get(3), "Enter a date.", "End Date error text did not matched");

		pageObj.campaignsbetaPage().setFrequency("Weekly");
		List<String> weeklyFrequencyerrors = pageObj.campaignsbetaPage().getListofErrors();
		System.out.println(weeklyFrequencyerrors);
		Assert.assertEquals(weeklyFrequencyerrors.get(0), "Select a repeat frequency.",
				"Repeat Every # Week(s) error text did not matched");
		Assert.assertEquals(weeklyFrequencyerrors.get(1), "Select a day of the week.",
				"Repeat On error text did not matched");
		Assert.assertEquals(weeklyFrequencyerrors.get(2), "Enter a date.", "Start Date error text did not matched");
		Assert.assertEquals(weeklyFrequencyerrors.get(3), "Enter a date.", "End Date error text did not matched");

		pageObj.campaignsbetaPage().setFrequency("Daily");
		List<String> dailyFrequencyerrors = pageObj.campaignsbetaPage().getListofErrors();
		Assert.assertEquals(dailyFrequencyerrors.get(0), "Enter a date.", "Start Date error text did not matched");
		Assert.assertEquals(dailyFrequencyerrors.get(1), "Enter a date.", "End Date error text did not matched");
		// Set frequency
		pageObj.campaignsbetaPage().setFrequency("Once");
		pageObj.campaignsbetaPage().setStartDateNew(0);
		boolean status = pageObj.campaignsbetaPage().verifyStartDateSelectedOrNot("Select");
		if (status == false) {
			utils.logit("Selecting start date again");
			pageObj.campaignsbetaPage().setStartDateNew(0);
		}
		boolean status1 = pageObj.campaignsbetaPage().verifyStartDateSelectedOrNot("Select");
		Assert.assertTrue(status1, "Start date not selected");
		pageObj.campaignsbetaPage().setTimeZone(dataSet.get("timeZone"));
		/*
		 * boolean priorty = pageObj.campaignsbetaPage().campaignPriorty();
		 * Assert.assertTrue(priorty, "Campaign priorty dropdown is not dispalyed");
		 */
		pageObj.campaignsbetaPage().clickNextBtn();

		// Summary Page
		String summaryTitle = pageObj.campaignsbetaPage().summaryTitle();
		Assert.assertEquals(summaryTitle, "Campaign Summary", "Summary title did not matched");
		List<String> summaryData = pageObj.campaignsbetaPage().CheckSummary();
		System.out.println(summaryData);
		SoftAssert softassert = new SoftAssert();
		softassert.assertTrue(summaryData.get(0).contains("Audience"), "Audience did not found on summary page");
		softassert.assertTrue(summaryData.get(1).contains("Segment size"),
				"Segment size did not found on summary page");
		softassert.assertTrue(summaryData.get(2).contains("Control Group Size"),
				"Control group did not found on summary page");
		softassert.assertTrue(summaryData.get(3).contains("Redeemable"), "Redeemable did not found on summary page");
		softassert.assertTrue(summaryData.get(5).contains("Frequency"), "Frequency did not found  on summary page");
		/*
		 * softassert.assertTrue(summaryData.get(6).contains("Start and End Dates"),
		 * "Start and End Dates did not found on summary page");
		 */
		softassert.assertTrue(summaryData.get(7).contains("Time Zone"), "Time Zone did not found on summary page");
		softassert.assertAll();
		utils.logPass("Campaign summary validated");

		pageObj.campaignsbetaPage().clickActivateCampaignBtn();
		String scheduleMsg = pageObj.campaignsbetaPage().validateSuccessMessage();
		Assert.assertEquals(scheduleMsg, "Schedule created successfully.", "Success message text did not matched");
		utils.logPass("Campaign edit and actiavte by approver is done and validated");
		// validate campaign status

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		String scheduledcampName = pageObj.campaignsbetaPage().searchCampaign(campaignName);
		String scheduledcampStatus = pageObj.campaignsbetaPage().getCampaignStatus();
		Assert.assertTrue(scheduledcampName.contains(campaignName), "Campaign name did not matched");
		Assert.assertEquals(scheduledcampStatus, "Scheduled", "Campaign status did not matched");
		utils.logPass("Campaign name and status validated scheduled");
	}

	@Test(description = "SQ-T2592 Create Mass Notification Campaign From New Modal", groups = { "regression",
			"unstable", "dailyrun" }, priority = 1)
	public void T2592_verifyCreateMassNotificationCampaignFromNewModal() throws InterruptedException, ParseException {

		campaignName = "Automation Notification Campaign" + CreateDateTime.getTimeDateString();

		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// Click Campaigns Link

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value
		pageObj.campaignspage().selectMessagedrpValue("Mass Notification");

		// validate error msg for blank name field
		pageObj.campaignspage().clickNewCampaignBetaBtn();
		pageObj.campaignsbetaPage().clickContinueBtn();
		String nameFieldErrormsg = pageObj.campaignsbetaPage().elementFieldErrorMessage();
		Assert.assertEquals(nameFieldErrormsg, "Enter a campaign name.",
				"Campaign name field error message text did not matched");
		pageObj.campaignsbetaPage().clickCancelBtn();

		pageObj.campaignspage().clickNewCampaignBetaBtn();
		pageObj.campaignsbetaPage().SetCampaignName(campaignName);

		pageObj.campaignsbetaPage().clickContinueBtn();

		utils.switchToWindow();

		pageObj.campaignsbetaPage().clickSummaryTab();
		pageObj.campaignsbetaPage().clickActivateCampaignBtn();
		// Who tab errors validation
		pageObj.campaignsbetaPage().clickWhoTab();
		String givenCampaignName = pageObj.campaignsbetaPage().givenCampaignName();
		String whoTitle = pageObj.campaignsbetaPage().whoTitle();
		// boolean infoStatus = pageObj.campaignsbetaPage().givenCampaigninfoLabel();
		Assert.assertEquals(givenCampaignName, campaignName, "Given campaign name did not matched");
		Assert.assertEquals(whoTitle, "Who is the target audience?", "Who title did not matched");
		// Assert.assertTrue(infoStatus, "Campaign info label did not appeared on who
		// tab");

		pageObj.campaignsbetaPage().clickLocationBtn();
		String locationFieldErrormsg = pageObj.campaignsbetaPage().elementFieldErrorMessage();
		Assert.assertEquals(locationFieldErrormsg, "Select a Location.",
				"Location field error message text did not matched");

		pageObj.campaignsbetaPage().clickSegmentBtn();
		String segmentFieldErrormsg = pageObj.campaignsbetaPage().elementFieldErrorMessage();
		Assert.assertEquals(segmentFieldErrormsg, "Select a Segment.",
				"Segment field error message text did not matched");

		// reachability section validation
		pageObj.campaignsbetaPage().setSegmentType(dataSet.get("segment"));
		pageObj.campaignsbetaPage().validateReachablityandControlGroup();
		pageObj.campaignsbetaPage().clickNextBtn();

		// What tab errors validation
		pageObj.campaignsbetaPage().clickWhatTab();
		// String whatTitle=pageObj.campaignsbetaPage().whatTitle();
		// Assert.assertEquals(whatTitle,"What is being offered?","What title did not
		// matched");

		pageObj.campaignsbetaPage().clickAttachaCoupontoMessageCampaign();
		String attachAcouponFieldErrormsg = pageObj.campaignsbetaPage().elementFieldErrorMessage();
		Assert.assertEquals(attachAcouponFieldErrormsg, "Select a coupon or other type of offer.",
				" Include Surveyfield error message text did not matched");
		pageObj.campaignsbetaPage().clickAttachaCoupontoMessageCampaign();

		pageObj.campaignsbetaPage().clickIncludeSurvey();
		String includeSurveyFieldErrormsg = pageObj.campaignsbetaPage().elementFieldErrorMessage();
		Assert.assertEquals(includeSurveyFieldErrormsg, "Select a survey.",
				" Include Surveyfield error message text did not matched");
		pageObj.campaignsbetaPage().clickIncludeSurvey();

		pageObj.campaignsbetaPage().clickIncludeAdvertisedCampaign();
		String includeAdvertisedCampaignFieldErrormsg = pageObj.campaignsbetaPage().elementFieldErrorMessage();
		Assert.assertEquals(includeAdvertisedCampaignFieldErrormsg, "Select an Advertised Campaign.",
				"Include Advertised Campaign field error message text did not matched");
		pageObj.campaignsbetaPage().clickIncludeAdvertisedCampaign();

		String richMessageFieldErrormsg = pageObj.campaignsbetaPage().richMessageFieldErrorMessage();
		String smsFieldErrormsg = pageObj.campaignsbetaPage().smsMessageFieldErrorMessage();
		String pnFieldErrormsg = pageObj.campaignsbetaPage().pnMessageFieldErrorMessage();
		String emailFieldErrormsg = pageObj.campaignsbetaPage().emailMessageFieldErrorMessage();
		Assert.assertEquals(richMessageFieldErrormsg, "Rich Message Style not selected",
				"Rich message style error message text did not matched");
		Assert.assertEquals(smsFieldErrormsg, "Enter SMS Body Text.", " SMS body error message text did not matched");
		Assert.assertEquals(pnFieldErrormsg, "Enter body text for your push notification.",
				" PN bodyerror message text did not matched");
		Assert.assertEquals(emailFieldErrormsg, "Enter a subject line.", "Email error message text did not matched");

		pageObj.campaignsbetaPage().setEmailNotificationWithEmailEditor("Test Email");
		pageObj.campaignsbetaPage().clickNextBtn();

		// When tab errors validation
		pageObj.campaignsbetaPage().clickWhenTab();
		String whenTitle = pageObj.campaignsbetaPage().whenTitle();
		Assert.assertEquals(whenTitle, "When should the campaign be sent?", "What title did not matched");

		String startDateFieldErrormsg = pageObj.campaignsbetaPage().elementFieldErrorMessage();
		boolean blackoutDates = pageObj.campaignsbetaPage().blackoutDates();
		Assert.assertEquals(startDateFieldErrormsg, "Enter a date.",
				"Frequency Once Start date field error message text did not matched");
		Assert.assertTrue(blackoutDates, "Blackout Dates is not dispalyed");

		pageObj.campaignsbetaPage().setFrequency("Monthly");
		List<String> monthlyFrequencyerrors = pageObj.campaignsbetaPage().getListofErrors();
		System.out.println(monthlyFrequencyerrors);
		Assert.assertEquals(monthlyFrequencyerrors.get(0), "Select a repeat frequency.",
				"Repeat Every # Month(s) error text did not matched");
		Assert.assertEquals(monthlyFrequencyerrors.get(1), "Select a Day of the Month.",
				"Day of Month error text did not matched");
		Assert.assertEquals(monthlyFrequencyerrors.get(2), "Enter a date.", "Start Date error text did not matched");
		Assert.assertEquals(monthlyFrequencyerrors.get(3), "Enter a date.", "End Date error text did not matched");

		pageObj.campaignsbetaPage().setFrequency("Weekly");
		List<String> weeklyFrequencyerrors = pageObj.campaignsbetaPage().getListofErrors();
		System.out.println(weeklyFrequencyerrors);
		Assert.assertEquals(weeklyFrequencyerrors.get(0), "Select a repeat frequency.",
				"Repeat Every # Week(s) error text did not matched");
		Assert.assertEquals(weeklyFrequencyerrors.get(1), "Select a day of the week.",
				"Repeat On error text did not matched");
		Assert.assertEquals(weeklyFrequencyerrors.get(2), "Enter a date.", "Start Date error text did not matched");
		Assert.assertEquals(weeklyFrequencyerrors.get(3), "Enter a date.", "End Date error text did not matched");

		pageObj.campaignsbetaPage().setFrequency("Daily");
		List<String> dailyFrequencyerrors = pageObj.campaignsbetaPage().getListofErrors();
		Assert.assertEquals(dailyFrequencyerrors.get(0), "Enter a date.", "Start Date error text did not matched");
		Assert.assertEquals(dailyFrequencyerrors.get(1), "Enter a date.", "End Date error text did not matched");
		// Set frequency
		pageObj.campaignsbetaPage().setFrequency("Once");
		pageObj.campaignsbetaPage().setStartDateNew(0);
		utils.longWaitInSeconds(2);
		boolean status1 = pageObj.campaignsbetaPage().verifyStartDateSelectedOrNot("Select");
		Assert.assertTrue(status1, "Start date not selected");
		pageObj.campaignsbetaPage().setTimeZone(dataSet.get("timeZone"));
		/*
		 * boolean priorty = pageObj.campaignsbetaPage().campaignPriorty();
		 * Assert.assertTrue(priorty, "Campaign priorty dropdown is not dispalyed");
		 */
		pageObj.campaignsbetaPage().clickNextBtn();

		// Summary Page
		String summaryTitle = pageObj.campaignsbetaPage().summaryTitle();
		Assert.assertEquals(summaryTitle, "Campaign Summary", "Summary title did not matched");
		List<String> summaryData = pageObj.campaignsbetaPage().CheckSummary();
		System.out.println(summaryData);
		SoftAssert softassert = new SoftAssert();
		softassert.assertTrue(summaryData.get(0).contains("Audience"), "Audience did not found on summary page");
		softassert.assertTrue(summaryData.get(1).contains("Segment size"),
				"Segment size did not found on summary page");
		softassert.assertTrue(summaryData.get(2).contains("Control Group Size"),
				"Control group did not found on summary page");
		softassert.assertTrue(summaryData.get(3).contains("Coupon"), "Coupon did not found on summary page");
		softassert.assertTrue(summaryData.get(4).contains("Frequency"), "Frequency did not found  on summary page");
		/*
		 * softassert.assertTrue(summaryData.get(5).contains("Start and End Dates"),
		 * "Start and End Dates did not found on summary page");
		 */
		softassert.assertTrue(summaryData.get(6).contains("Time Zone"), "Time Zone did not found on summary page");
		softassert.assertAll();
		utils.logPass("Campaign summary validated");

		pageObj.campaignsbetaPage().clickActivateCampaignBtn();
		String scheduleMsg = pageObj.campaignsbetaPage().validateSuccessMessage(campaignName);
		Assert.assertEquals(scheduleMsg, "Schedule created successfully.", "Success message text did not matched");
		utils.logPass("Campaign edit and actiavte by approver is done and validated");
		// validate campaign status

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// pageObj.dashboardpage().navigateToTabs("Ordering App Configs");

		String scheduledcampName = pageObj.campaignsbetaPage().searchCampaign(campaignName);
		String scheduledcampStatus = pageObj.campaignsbetaPage().getCampaignStatus();
		Assert.assertTrue(scheduledcampName.contains(campaignName), "Campaign name did not matched");
		Assert.assertEquals(scheduledcampStatus, "Scheduled", "Campaign status did not matched");
		utils.logPass("Campaign name and status validated scheduled");
	}

	// no need to keep in regression
	@Test(description = "SQ-T2508 Campaign reason overflow tooltip", groups = { "regression",
			"dailyrun" }, priority = 2)
	public void T2508_verifyCampaignReasonOverflowTooltip() throws Exception {

		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().logintoInstance(dataSet.get("userSubmitter"), dataSet.get("password"));

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickSwitchToClassicBtn();
//			Thread.sleep(10000);
		// List<String> activeStatus =
		// pageObj.campaignsbetaPage().filterStatus("Disapproved");
		pageObj.campaignspage().searchAndSelectCamapign("");// rejectCampaignName
//			Assert.assertTrue(activeStatus.contains("DISAPPROVED"),
//					" Campaign is other than Disapproved for Active status");
//			TestListeners.extentTest.get().pass("Campaign name and status validated");
		// pageObj.campaignsbetaPage().selectCampaign();
		String mssg = pageObj.campaignsbetaPage().validateErrorMessage();
		Assert.assertTrue(mssg.contains("This campaign was rejected by"), "Success message text did not matched");
		String rejectionmsg = pageObj.campaignsbetaPage().validateTooltip();
		utils.logPass("tooltip message appeared as : " + rejectionmsg);
		// Assert.assertEquals(rejectionmsg, "Test rejecting campaign", "Campaign
		// rejection reason tooltip not matched");

	}

//	As discussed with Khushboo soni this functionality has been removed 

//	@Test(description = "SQ-T2509 Campaign Listing changes for Workflow Module", groups = "Regression", priority = 4)
	public void T2509_verifyCampaignListingchangesforWorkflowModule() throws InterruptedException {
		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// Click Campaigns Link
//		pageObj.menupage().clickCampaignsMenu();
//		pageObj.menupage().clickCampaignsBetaLink();
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.campaignspage().selectMessagedrpValue("Mass Notification");
		Thread.sleep(12000);
		/*
		 * List<String> activeStatus =
		 * pageObj.campaignsbetaPage().filterStatus("Active");
		 * Assert.assertTrue(activeStatus.contains("PROCESSING") |
		 * activeStatus.contains("APPROVED"),
		 * " Campaign is other than Processing or Approved for Active status");
		 */

		List<String> scheduledStatus = pageObj.campaignsbetaPage().filterStatus("Scheduled");
		boolean a = pageObj.campaignsbetaPage().verifyAllEqual(scheduledStatus);
		Assert.assertTrue(scheduledStatus.contains("SCHEDULED"), " Campaign status is other than Scheduled");
		Assert.assertTrue(a, " Status value for scheduled did not matched");

		List<String> draftStatus = pageObj.campaignsbetaPage().filterStatus("Draft");
		boolean e = pageObj.campaignsbetaPage().verifyAllEqual(draftStatus);
		Assert.assertTrue(draftStatus.contains("DRAFT"), " Campaign status is other than Draft");
		Assert.assertTrue(e, " Status value for draft did not matched");

		List<String> processedStatus = pageObj.campaignsbetaPage().filterStatus("Processed");
		boolean c = pageObj.campaignsbetaPage().verifyAllEqual(processedStatus);
		Assert.assertTrue(processedStatus.contains("PROCESSED"), " Campaign status is other than Processed");
		Assert.assertTrue(c, " Status value for processed did not matched");

		List<String> rejectedStatus = pageObj.campaignsbetaPage().filterStatus("Disapproved");
		boolean f = pageObj.campaignsbetaPage().verifyAllEqual(rejectedStatus);
		Assert.assertTrue(rejectedStatus.contains("DISAPPROVED"), " Campaign status is other than Disapproved");
		Assert.assertTrue(f, " Status value for disapproved did not matched");

		List<String> pendingStatus = pageObj.campaignsbetaPage().filterStatus("Pending Approval");
		boolean b = pageObj.campaignsbetaPage().verifyAllEqual(pendingStatus);
		Assert.assertTrue(pendingStatus.contains("PENDING APPROVAL"),
				" Campaign status is other than Pending Approval");
		Assert.assertTrue(b, " Status value for pending approval did not matched");

		/*
		 * List<String> processingStatus =
		 * pageObj.campaignsbetaPage().filterStatus("Processing"); boolean d =
		 * pageObj.campaignsbetaPage().verifyAllEqual(processingStatus);
		 * Assert.assertTrue(processingStatus.contains("PROCESSING"),
		 * " Campaign status is other than Processing"); Assert.assertTrue(d,
		 * " Status value for processing did not matched");
		 */
	}

	// Reason - camp beta link is no more in application
	// @Test(description = "SQ-T2535 Remove Campaigns BETA Left Navigation menu",
	// groups = "Regression", priority = 5)
	public void T2535_verifyRemoveCampaignsBETALeftNavigationmenuPart3() throws Exception {
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().logintoInstance(dataSet.get("userSubmitter"), dataSet.get("password"));
		// pageObj.menupage().clickCampaignsMenu();
		// boolean grantStatus = pageObj.menupage().checkCampaignBetaMenu();
		// Assert.assertTrue(grantStatus, "Campign beta menu status is not true");
		// Assert.assertFalse(grantStatus, "Campign beta menu status is not true");
		utils.logPass("Campaign BETA Left Navigation menu validated");

	}

	// Reason - camp beta link is no more in application
	// @Test(description = "SQ-T2535 Remove Campaigns BETA Left Navigation menu",
	// groups = "Regression", priority = 3)
	public void T2535_verifyRemoveCampaignsBETALeftNavigationmenuPart1() throws Exception {
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().logintoInstance(dataSet.get("userSubmitter"), dataSet.get("password"));
		// Click Campaigns Link
		// pageObj.menupage().clickCampaignsMenu();
		// boolean revokeStatus = pageObj.menupage().checkCampaignBetaMenu();
		// Assert.assertFalse(revokeStatus, "Campign beta menu status is not false");
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
