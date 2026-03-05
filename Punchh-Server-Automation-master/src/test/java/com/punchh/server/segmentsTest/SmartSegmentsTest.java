package com.punchh.server.segmentsTest;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

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
import com.punchh.server.utilities.MessagesConstants;
import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class SmartSegmentsTest {
	public WebDriver driver;
	private Properties prop;
	private PageObj pageObj;
	private String userEmail;
	private String sTCName;
	private String env;
	private String baseUrl;
	private SeleniumUtilities selUtils;
	private Utilities utils;
	private static Map<String, String> dataSet;
	String run = "ui";

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
		prop = Utilities.loadPropertiesFile("config.properties");
		driver = new BrowserUtilities().launchBrowser();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		dataSet = new ConcurrentHashMap<>();
		sTCName = method.getName();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		pageObj.utils().logit(sTCName + " ==>" + dataSet);
		utils = new Utilities(driver);
		selUtils = new SeleniumUtilities(driver);
	}

	@Test(description = "SQ-T4999: Validate switching of Smart Segments option in Cockpit; "
			+ "SQ-T4998: Validate Smart segments User Interface", priority = 0)
	@Owner(name = "Vaibhav Agnihotri")
	public void T4999_validateSmartSegmentSwitching() throws InterruptedException {
		// Login to instance. Select the business not having smart segment data
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Go to Cockpit > Dashboard, ensure the required flags are turned OFF
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick(dataSet.get("flagID"), "uncheck");
		pageObj.dashboardpage().updateCheckBox();
		pageObj.dashboardpage().navigateToTabs("Bento Apps");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick(dataSet.get("flagID2"), "uncheck");
		pageObj.dashboardpage().updateCheckBox();

		// Go to Guests > Segments
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		pageObj.utils().logit("Navigated to Guests > Segments");

		// Verify that the Smart segments tab is not available
		Assert.assertFalse(pageObj.segmentsBetaPage()
				.verifyPresenceAndClick(utils.getLocatorValue("segmentBetaPage.smartSegmentsTab")));
		utils.logPass("As flag is turned OFF, the Smart segments tab is not available.");

		// Go back to Cockpit > Dashboard, ensure the required flag is turned ON
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick(dataSet.get("flagID"), "check");
		pageObj.dashboardpage().updateCheckBox();

		// Go to Guests > Segments
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		pageObj.utils().logit("Navigated to Guests > Segments");

		// Verify that the Smart segments tab is available
		Assert.assertTrue(pageObj.segmentsBetaPage()
				.verifyPresenceAndClick(utils.getLocatorValue("segmentBetaPage.smartSegmentsTab")));
		utils.logPass("The Smart segments tab is available and clicked.");

		// Verify the UI message on Smart segment UI when data is not available
		Assert.assertEquals(utils.getLocator("segmentBetaPage.smartSegmentNoDataMsg").getText(),
				dataSet.get("noDataAvailableMsg"), "UI message does not match.");
		utils.logPass("UI message is displayed as Smart segment data is not available.");

		// Now go to the business having smart segment data
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug2"));

		// Go to Cockpit > Dashboard, ensure the required flags are set as expected
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick(dataSet.get("flagID"), "check");
		pageObj.dashboardpage().updateCheckBox();
		pageObj.dashboardpage().navigateToTabs("Bento Apps");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick(dataSet.get("flagID2"), "uncheck");
		pageObj.dashboardpage().updateCheckBox();

		// Go to Guests > Segments
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		Assert.assertTrue(pageObj.segmentsBetaPage()
				.verifyPresenceAndClick(utils.getLocatorValue("segmentBetaPage.smartSegmentsTab")));
		utils.logit("pass", "The Smart segments tab is available and clicked.");

		// Verify the Smart segment UI components
		Assert.assertTrue(utils.checkElementPresent(utils.getLocator("segmentBetaPage.smartSegmentHeading")));
		Assert.assertTrue(
				utils.checkElementPresent(utils.getLocator("segmentBetaPage.smartSegmentWithNoEngagementHeading")));
		Assert.assertTrue(utils.checkElementPresent(utils.getLocator("segmentBetaPage.smartSegmentLocationsDropdown")));
		Assert.assertTrue(pageObj.segmentsBetaPage().doesSmartSegmentSubHeadersMatch(
				utils.getLocatorList("segmentBetaPage.smartSegmentCategoriesList"),
				dataSet.get("smartSegmentCategories")));
		Assert.assertTrue(pageObj.segmentsBetaPage().verifyYaxisHeadings("segmentBetaPage.smartSegmentYaxisHeading",
				dataSet.get("smartSegmentYaxisHeadings")));
		Assert.assertTrue(
				utils.checkElementPresent(utils.getLocator("segmentBetaPage.smartSegmentOverviewDefinitionHeading")));
		Assert.assertTrue(utils
				.checkElementPresent(utils.getLocator("segmentBetaPage.smartSegmentOverviewConsiderationHeading")));
		Assert.assertTrue(pageObj.segmentsBetaPage().doesSmartSegmentSubHeadersMatch(
				utils.getLocatorList("segmentBetaPage.smartSegmentViewByCategoriesList"),
				dataSet.get("smartSegmentViewByCategories")));
		Assert.assertEquals(pageObj.segmentsBetaPage().verifyAndGetTextsCount(
				utils.getLocatorList("segmentBetaPage.smartSegmentTooltipsList"),
				dataSet.get("smartSegmentTooltipsSubstrings")), dataSet.get("smartSegmentTooltipsCount"));
		Assert.assertTrue(pageObj.segmentsBetaPage().doesSmartSegmentSubHeadersMatch(
				utils.getLocatorList("segmentBetaPage.smartSegmentReachabilityStatsHeadings"),
				dataSet.get("smartSegmentReachabilityStatsHeadings")));
		Assert.assertTrue(pageObj.segmentsBetaPage().doesSmartSegmentOverviewHeadersMatch());
		Assert.assertTrue(pageObj.segmentsBetaPage().verifyReachabilityPopup());
		utils.logit("pass", "The Smart segment UI components are verified.");
	}

	@Test(description = "SQ-T5001 Validate Segment Details page for Smart Segments; "
			+ "SQ-T5000 Validate Smart Segments within the Mass Campaign workflow", priority = 1)
	@Owner(name = "Vaibhav Agnihotri")
	public void T5000_validateSmartSegmentsRedirections() throws Exception {

		// Login to instance. Select the business
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Go to Cockpit > Dashboard, ensure the required flags are set as expected
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick(dataSet.get("flagID"), "check");
		pageObj.dashboardpage().updateCheckBox();
		pageObj.dashboardpage().navigateToTabs("Bento Apps");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick(dataSet.get("flagID2"), "uncheck");
		pageObj.dashboardpage().updateCheckBox();

		// Go to Guests > Segments
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		utils.logit("The Smart segments tab is available and clicked. Navigated to the Smart segments main page.");
		utils.waitTillPagePaceDone();
		Assert.assertTrue(pageObj.segmentsBetaPage()
				.verifyPresenceAndClick(utils.getLocatorValue("segmentBetaPage.smartSegmentsTab")));

		// Verify the redirection to Segment details page from Smart Segment's Details
		// link
		String smartSegmentOverviewHeader = utils.getLocator("segmentBetaPage.smartSegmentOverviewHeader").getText();
		String parentWindow = pageObj.segmentsBetaPage().verifySegmentDetailsPageRedirection(smartSegmentOverviewHeader,
				dataSet.get("segmentDetailsSubHeadings"));
		pageObj.utils().logPass("The Segment details page redirection is verified.");

		selUtils.switchToWindow(parentWindow);
		pageObj.utils().logit("Switched back to the main Smart Segments page.");
		pageObj.utils().logit("Switched back to the main Smart Segments page.");

		// Create Campaign within Smart segments and Navigate to the Campaign builder
		// screen
		String campaignName = CreateDateTime.getUniqueString("SmartSegmentCampaign");
		Assert.assertTrue(pageObj.segmentsBetaPage().createCampaignWithinSmartSegment(campaignName));
		pageObj.utils().logit("Navigated to Campaign builder screen.");
		pageObj.utils().logit("Navigated to Campaign builder screen.");
		Assert.assertTrue(
				utils.checkElementPresent(utils.getLocator("segmentBetaPage.segmentDetailsSmartSegmentDropdownLabel")));
		Assert.assertTrue(
				utils.checkElementPresent(utils.getLocator("segmentBetaPage.segmentDetailsLocationsDropdownLabel")));
		Assert.assertTrue(pageObj.segmentsBetaPage().verifySmartSegmentCategoriesInCampaigns(
				dataSet.get("campaignSmartSegmentDropdownCategories"), smartSegmentOverviewHeader));
		pageObj.utils().logPass("Smart segment components on Campaign builder page are verified.");


		// Delete the created campaign
		pageObj.utils().deleteMassCampaignByName(campaignName, env);
		utils.logit(campaignName + " campaign deleted successfully.");
	}

	@Test(description = "SQ-T4663: Verify 'Smart segments' as New Segment type; SQ-T5752: Verify the functionality of the 'Smart segments' type", groups = {
			"regression", "unstable" }, priority = 2)
	@Owner(name = "Vaibhav Agnihotri")
	public void T4663_validateSmartSegmentInSegmentType() throws Exception {

		// Login to instance. Select the business
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Go to Cockpit > Dashboard, ensure the required flags are set as expected
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().checkUncheckAnyFlag(dataSet.get("flagID"), "check");
		pageObj.dashboardpage().navigateToTabs("Bento Apps");
		pageObj.dashboardpage().checkUncheckAnyFlag(dataSet.get("flagID2"), "uncheck");

		// Go to Guests > Segments
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");

		// Get the Segment count for a Smart Segment from Segment list page
		String smartSegmentName = dataSet.get("smartSegmentToUse");
		int count = pageObj.segmentsBetaPage().findSegmentCountOnAllSegmentsTab(smartSegmentName);

		// Verify the creation of new segment with Smart segment as segment type
		String segmentName = CreateDateTime.getUniqueString("SmartSegment");
		pageObj.segmentsBetaPage().setSegmentBetaNameAndType(segmentName, "Smart Segment");
		Assert.assertTrue(pageObj.segmentsPage().checkSegmentLabel("Segment Type"));
		Assert.assertTrue(pageObj.segmentsPage().checkSegmentLabel("Smart Segment List"));
		pageObj.utils().logPass("Smart segments is present as a Segment type.");


		// Verify that all expected smart segment categories are present in the Segment
		// list dropdown and Tooltip is present
		boolean isSmartSegmentTypeTooltipVerified = pageObj.segmentsBetaPage().verifyTooltipText(
				utils.getLocatorValue("segmentBetaPage.segmentTypeTooltip"), MessagesConstants.smartSegmentTypeTooltip);
		boolean isSmartSegmentDropdownVerified = pageObj.segmentsBetaPage().verifySmartSegmentInSegmentBuilder(
				dataSet.get("smartSegmentListDropdownCategories"), smartSegmentName, 10);
		Assert.assertTrue(isSmartSegmentTypeTooltipVerified, "Segment Type tooltip for Smart Segment is not verified.");
		Assert.assertTrue(isSmartSegmentDropdownVerified, "Smart Segment Dropdown List is not verified.");
		utils.logPass(
				"All expected smart segment categories are present in the Segment list dropdown and Tooltip is present");

		// Verify that all the counts of Smart Segment guests remains same after saving
		int segmentCountBeforeSave = pageObj.segmentsBetaPage().getSegmentSizeBeforeSave();
		// pageObj.segmentsBetaPage().saveSegment(segmentName);
		pageObj.segmentsPage().saveAndShowSegment();
		int segmentDefinitionCount = pageObj.segmentsBetaPage().getSegmentCountSegmentDefination();
		int guestInSegmentCount = pageObj.segmentsBetaPage().getGuestsInSegmentCount();
		int customerReachabilityCount = pageObj.segmentsBetaPage().getSegmentCustomerReachabilityCount();

		Assert.assertEquals(segmentCountBeforeSave, count,
				"Segment count before save did not match with the count on segment list page.");
		Assert.assertEquals(segmentDefinitionCount, count,
				"'Segment Definition' count did not match with the count on segment list page.");
		Assert.assertEquals(guestInSegmentCount, count,
				"'Guests in Segment' count did not match with the count on segment list page.");
		Assert.assertTrue(customerReachabilityCount > 0, "'Customer Reachability' count is not greater than zero.");
		utils.logPass(
				"Guest counts are verified at all places for '" + smartSegmentName + "' smart segment category.");

		// Verify that a valid user email gets displayed in green under Guest list
		userEmail = dataSet.get("smartSegmentUserEmail");
		utils.refreshPage();
		boolean isUserUnderGuestListVerified = pageObj.segmentsPage().verifyUserPresentInSegment(userEmail, "#ddf3dd");
		Assert.assertTrue(isUserUnderGuestListVerified, "User email is not present under Guest list.");
		utils.logit("User email is present under 'Guests in Segment'.");

		// Run a mass campaign with the saved smart segment
		String segmentOverviewWindowHandle = driver.getWindowHandle();
		pageObj.segmentsPage().segmentOverviewPageOptionList("Create Mass Gifting");
		utils.switchToWindow();
		String massCampaignName = "MassCampaign_" + segmentName;
		String dateTime = CreateDateTime.getTomorrowDate() + " 11:00 PM";
		pageObj.signupcampaignpage().setCampaignName(massCampaignName);
		pageObj.signupcampaignpage().setGiftType("Gift Fixed Points");
		pageObj.signupcampaignpage().setFixedPoints("1");
		pageObj.signupcampaignpage().clickNextBtn();
		pageObj.signupcampaignpage().setPushNotification(massCampaignName);
		pageObj.signupcampaignpage().clickNextButton();
		pageObj.signupcampaignpage().setFrequencyAndTimeInCampaign("Once", dateTime);
		pageObj.signupcampaignpage().clickScheduleBtn();
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().findMassCampaignNameandRun(massCampaignName);

		// Verify that the campaign notification is received on user timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		String campaignName = pageObj.guestTimelinePage().getcampaignNameWithWait(massCampaignName);
		Assert.assertTrue(campaignName.equalsIgnoreCase(massCampaignName), "Campaign name did not matched");
		boolean campaignNotificationStatus = pageObj.guestTimelinePage().verifyCampaignNotification();
		Assert.assertTrue(campaignNotificationStatus, "Campaign notification did not displayed...");
		utils.logPass("Mass Offer campaign with name " + massCampaignName
				+ " is found on user Timeline and Campaign Notification Status is :- " + campaignNotificationStatus);

		// Verify the Export for the saved Smart segment
		String exportName = "Export_" + segmentName;
		selUtils.switchToWindow(segmentOverviewWindowHandle);
		pageObj.segmentsPage().segmentOverviewPageOptionList("Export");
		utils.switchToWindow();
		pageObj.schedulePage().segmentExportSchedule("N/A", exportName, "Immediately");
		boolean logsConfirmation = pageObj.schedulePage()
				.selectDataExportSchedule(prop.getProperty("segmentExportSchedule"), segmentName);
		Assert.assertTrue(logsConfirmation, "Failed to verify data export logs");
		utils.logPass("Successfully verified data export logs for '" + segmentName + "'");

	}

	// steps need to be updated by segment team sachin
	// @Test(description = "SQ-T6419 verify on smart segment page customise button
	// name change to build from this", priority = 3)
	@Owner(name = "Amit Kumar")
	public void T6419_validateSmartSegmentPageCustomiseButtonNameChangeToBuildFromThis() throws InterruptedException {

		String segmentName = CreateDateTime.getUniqueString("Segment");
		// Login to instance. Select the business
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Go to Guests > Segments
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		pageObj.segmentsBetaPage().verifyPresenceAndClick(utils.getLocatorValue("segmentBetaPage.smartSegmentsTab"));

		// select super fan or fan or infrquent
		pageObj.segmentsBetaPage().buildFromSuperFan();
		pageObj.segmentsBetaPage().updateSetSegmentName(segmentName);
		pageObj.segmentsBetaPage().saveSegment(segmentName);
		pageObj.segmentsBetaPage().segmentDefinationDelete();

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		pageObj.utils().logit("Browser closed");
	}
}