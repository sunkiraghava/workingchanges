package com.punchh.server.campaignsTest;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import com.punchh.server.annotations.Owner;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class MassOfferCampaignTest_Template {

	private static Logger logger = LogManager.getLogger(MassOfferCampaignTest_Template.class);
	public WebDriver driver;
	private Properties prop;
	private PageObj pageObj;
	private String userEmail;
	private String sTCName;
	private String env;
	private String baseUrl;
	private static Map<String, String> dataSet;
	String run = "ui";
	public Map<String, String> allTestCasesMassCampNameMap = new LinkedHashMap<String, String>();;

	// @BeforeMethod(alwaysRun = true) <-- using this method in all my tests-->
	public void setUp(String methodName) {
		prop = Utilities.loadPropertiesFile("config.properties");
		driver = new BrowserUtilities().launchBrowser();
		sTCName = methodName;
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		logger.info(sTCName + " ==>" + dataSet);
	}

	// dependent methods exists so single login cant be possible
	// shashank
	@Test(description = "SQ-T3571 Verify the functionality of Available as a Template? flag in mass notification campaign", groups = {
			"regression", "dailyrun" }, priority = 0)
	@Owner(name = "Shashank Sharma")
	public void T3571_verifyMassOfferCampaignAsTemplateAndGiftingPointsToSegmentUsers() throws InterruptedException {
		setUp("T3571_verifyMassOfferCampaignAsTemplateAndGiftingPointsToSegmentUsers");
		String massCampaignName = "AutoPointBasedMassOffer_SQT3571_" + CreateDateTime.getTimeDateString();
		String dateTime = CreateDateTime.getTomorrowDate() + " 11:00 PM";

		userEmail = dataSet.get("email2");
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
		pageObj.signupcampaignpage().turnOnAvailableAsATemplate();
		pageObj.signupcampaignpage().setGiftType(dataSet.get("giftType"));
		pageObj.signupcampaignpage().setFixedPoints(dataSet.get("fixedPoints"));
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().createWhomDetailsMassCampaign(dataSet.get("audiance"), dataSet.get("segment"),
				dataSet.get("pushNotification"), dataSet.get("emailSubject"), dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().createWhenDetailsMassCampaign("Once", dateTime);

		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().findMassCampaignNameandRun(massCampaignName);
		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().searchCampaign(massCampaignName);

		boolean templateIsDisplayedResult = pageObj.campaignspage().verifyTemplateIconForCampaignName(massCampaignName);

		Assert.assertTrue(templateIsDisplayedResult,
				"TEMPLATE icon is not displayed for the user " + dataSet.get("email"));

		allTestCasesMassCampNameMap.put("T3571_verifyMassOfferCampaignAsTemplateAndGiftingPointsToSegmentUsers",
				massCampaignName);
		// Timeline validation (move to user timeline)
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		String campaignName = pageObj.guestTimelinePage().getcampaignNameMasscampaign(massCampaignName);
		boolean campaignNameStatus = pageObj.guestTimelinePage().verifyIsCampaignExistOnTimeLine(massCampaignName);
		boolean campaignNotificationStatus = pageObj.guestTimelinePage()
				.verifyCampaginOrSystemNotificationIsDisplayed(massCampaignName, campaignNameStatus);

		SoftAssert softassert = new SoftAssert();
		softassert.assertTrue(campaignName.equalsIgnoreCase(massCampaignName), "Campaign name did not matched");
		softassert.assertTrue(campaignNotificationStatus, "Campaign notification did not displayed...");
		softassert.assertAll();
		TestListeners.extentTest.get().pass(
				"Mass offer points campaign detail: push notification, campaign name, pointsnotification validated successfully on timeline");
		driver.quit();
		verifyTemplateIconIsDispalyedForSecondUser(
				"T3571_verifyMassOfferCampaignAsTemplateAndGiftingPointsToSegmentUsers");

	}

	// SQ-T3570
	@Test(description = "SQ-T3570 Mass Campaign Trigger For User When Gift type is Currency ", groups = { "regression",
	"dailyrun" }, priority = 1)
	@Owner(name = "Shashank Sharma")
	public void T3570_verifyMassCampaignAsTemplateAndGiftingCurrencyToUser() throws InterruptedException {
		// Precondition: segments and user is present Segement:custom automation
		setUp("T3570_verifyMassCampaignAsTemplateAndGiftingCurrencyToUser");

		String massCampaignName = "AutomationCurrencyBasedMassOffer_T3570_" + CreateDateTime.getTimeDateString();
		allTestCasesMassCampNameMap.put("T3570_verifyMassCampaignAsTemplateAndGiftingCurrencyToUser", massCampaignName);

		String dateTime = CreateDateTime.getTomorrowDate() + " 11:00 PM";
		String pushNotification = dataSet.get("pushNotification") + massCampaignName;
		userEmail = dataSet.get("email2");
		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Click Campaigns Link
		// pageObj.menupage().clickCampaignsMenu();
		// pageObj.menupage().clickCampaignsLink();
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		// Select offer dropdown value
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();

		// set campaign name and gift type
		pageObj.signupcampaignpage().setCampaignName(massCampaignName);
		pageObj.signupcampaignpage().turnOnAvailableAsATemplate();
		pageObj.signupcampaignpage().setGiftType(dataSet.get("giftType"));
		pageObj.signupcampaignpage().setGiftCurrency(dataSet.get("giftCurrency"));
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().createWhomDetailsMassCampaign(dataSet.get("audiance"), dataSet.get("segment"),
				pushNotification, dataSet.get("emailSubject"), dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().createWhenDetailsMassCampaign("Once", dateTime);
		/*
		 * boolean status = pageObj.campaignspage().validateSuccessMessage();
		 * Assert.assertTrue(status,
		 * "Schedule created successfully Success message did not displayed....");
		 */
		// run mass offer
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		// pageObj.menupage().clickDashboardMenu();
		// pageObj.menupage().clickSupportMenu();
		// pageObj.menupage().clickSchedulesLink();
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().findMassCampaignNameandRun(massCampaignName);

		// Click Campaigns Link
		// pageObj.menupage().clickCampaignsMenu();
		// pageObj.menupage().clickCampaignsLink();

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		// Select offer dropdown value
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().searchCampaign(massCampaignName);

		boolean templateIsDisplayedResult = pageObj.campaignspage().verifyTemplateIconForCampaignName(massCampaignName);

		Assert.assertTrue(templateIsDisplayedResult, "TEMPLATE icon is not displayed for the user " + userEmail);

		// Timeline validation (move to user timeline)
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		boolean campaignNameStatus = pageObj.guestTimelinePage().verifyIsCampaignExistOnTimeLine(massCampaignName);
		boolean campaignNotificationStatus = pageObj.guestTimelinePage()
				.verifyCampaginOrSystemNotificationIsDisplayed(massCampaignName, campaignNameStatus);
		boolean pushNotificationStatus = pageObj.guestTimelinePage().verifyPushNotificationIsDisplayed(massCampaignName,
				pushNotification, campaignNameStatus);

		SoftAssert softassert = new SoftAssert();
		softassert.assertTrue(campaignNameStatus, "Campaign name did not matched");
		softassert.assertTrue(campaignNotificationStatus, "Campaign notification did not displayed...");
		softassert.assertTrue(pushNotificationStatus, "Push notification did not displayed...");
		softassert.assertAll();
		TestListeners.extentTest.get().pass(
				"Mass offer currency campaign detail: push notification, campaign name, pointsnotification validated successfully on timeline");

		driver.quit();
		verifyTemplateIconIsDispalyedForSecondUser(
				"T3571_verifyMassOfferCampaignAsTemplateAndGiftingPointsToSegmentUsers");

	}

	// SQ-T3569 Automation_RedeemableWithTemplate
	@Test(description = "SQ-T3569 (1.0) - Verify the functionality of Available as a Template? flag when gift type is redeemable in mass offer campaign", groups = {
			"regression", "dailyrun" }, priority = 2)
	@Owner(name = "Shashank Sharma")
	public void T3569_verifyMassCampaignAsTemplateAndGiftingRedeemableToUser() throws InterruptedException {
		// Precondition: segement and user is present Segement:custom automation
		setUp("T3569_verifyMassCampaignAsTemplateAndGiftingRedeemableToUser");
		String massCampaignName = "Automation Mass Campaign" + CreateDateTime.getTimeDateString();
		allTestCasesMassCampNameMap.put("T3569_verifyMassCampaignAsTemplateAndGiftingRedeemableToUser",
				massCampaignName);

		String dateTime = CreateDateTime.getTomorrowDate() + " 11:00 PM";
		String pushNotification = dataSet.get("pushNotification") + massCampaignName;
		String expectedItemGifted_Message = "Automation_RedeemableWithTemplate (" + massCampaignName + ")";

		userEmail = dataSet.get("email2");
		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// // Click Campaigns Link
		// pageObj.menupage().clickCampaignsMenu();
		// pageObj.menupage().clickCampaignsLink();
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();

		// set campaign name and gift type
		pageObj.signupcampaignpage().setCampaignName(massCampaignName);
		pageObj.signupcampaignpage().turnOnAvailableAsATemplate();
		pageObj.signupcampaignpage().setGiftType(dataSet.get("giftType"));

		pageObj.signupcampaignpage().setRedeemable(dataSet.get("redeemableName"));
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().createWhomDetailsMassCampaign(dataSet.get("audiance"), dataSet.get("segment"),
				pushNotification, dataSet.get("emailSubject"), dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().createWhenDetailsMassCampaign("Once", dateTime);
		/*
		 * boolean status = pageObj.campaignspage().validateSuccessMessage();
		 * Assert.assertTrue(status,
		 * "Schedule created successfully Success message did not displayed....");
		 */
		// run mass offer
		// pageObj.menupage().clickDashboardMenu();
		// pageObj.menupage().clickSupportMenu();
		// pageObj.menupage().clickSchedulesLink();
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().findMassCampaignNameandRun(massCampaignName);

		// Timeline validation (move to user timeline)
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		boolean campaignNameStatus = pageObj.guestTimelinePage().verifyIsCampaignExistOnTimeLine(massCampaignName);

		Assert.assertTrue(campaignNameStatus, massCampaignName + "Campaign name did not matched");
		logger.info(massCampaignName + " campaign is visible on user timeline page");
		TestListeners.extentTest.get().pass(massCampaignName + " campaign is visible on user timeline page");

		boolean campaignNotificationStatus = pageObj.guestTimelinePage()
				.verifyCampaginOrSystemNotificationIsDisplayed(massCampaignName, campaignNameStatus);
		Assert.assertTrue(campaignNotificationStatus, "Campaign notification did not displayed...");
		logger.info(massCampaignName + " Campaign notification did not displayed... ");
		TestListeners.extentTest.get().pass(massCampaignName + " Campaign notification did not displayed...");

		String pushNotificationStatus = pageObj.guestTimelinePage().getPushNotificationForCampaign(massCampaignName);
		Assert.assertTrue(pushNotificationStatus.contains(pushNotification), "Push notification did not displayed...");
		logger.info("Push notification is visible on user timeline page");
		TestListeners.extentTest.get().pass("Push notification is visible on user timeline page");

		pageObj.guestTimelinePage().clickAccountHistory();
		boolean rewardPointsStatus = pageObj.accounthistoryPage()
				.getAccountDetailsforRewardEarned(expectedItemGifted_Message, "Item");

		SoftAssert softassert = new SoftAssert();
		softassert.assertTrue(rewardPointsStatus, "Gifted points did not appeared in account history");
		softassert.assertAll();
		TestListeners.extentTest.get().pass(
				"Mass offer derived reward campaign detail: push notification, campaign name, pointsnotification validated successfully on timeline");

		driver.quit();
		verifyTemplateIconIsDispalyedForSecondUser(
				"T3571_verifyMassOfferCampaignAsTemplateAndGiftingPointsToSegmentUsers");

	}

	// @Test(description = "It depends on the above 3 test cases , it required
	// campaign name which are created in above test cases ", priority = 5)
	public void verifyTemplateIconIsDispalyedForSecondUser(String methodName) throws InterruptedException {
		if (allTestCasesMassCampNameMap.size() != 0) {
			userEmail = dataSet.get("email");
			setUp(methodName);
			pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
			pageObj.instanceDashboardPage().logintoInstance(userEmail, dataSet.get("password"));

			// pageObj.menupage().clickCampaignsMenu();
			// pageObj.menupage().clickCampaignsLink();
			pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
			pageObj.newCamHomePage().clickNewCamHomePageBtn();
			pageObj.newCamHomePage().campaignAdvertiseBlock();

			for (Map.Entry<String, String> set : allTestCasesMassCampNameMap.entrySet()) {
				String testCaseName = set.getKey();
				String testCaseMassCampName = set.getValue();

				// Printing all elements of a Map
				boolean templateIsDisplayedResult1 = pageObj.campaignspage()
						.verifyTemplateIconForCampaignName(testCaseMassCampName);

				Assert.assertTrue(templateIsDisplayedResult1,
						"TEMPLATE icon is not displayed for the test case --  " + testCaseName);
				logger.info("Verified the TEMPLATE icon is displayed for the test case -- " + testCaseName);
				TestListeners.extentTest.get()
				.pass("Verified the TEMPLATE icon is displayed for the test case -- " + testCaseName);
			}
		} else {

			logger.error("NO mass campaign name is added into map ");
			TestListeners.extentTest.get().fail("NO mass campaign name is added into map, Map size is zero ");

		}
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}

}
