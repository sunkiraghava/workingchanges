package com.punchh.server.NCHPTest;

import java.lang.reflect.Method;
import java.util.ArrayList;
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
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class NewCampPageNcpTest {

	private static Logger logger = LogManager.getLogger(NewCampPageNcpTest.class);
	public WebDriver driver;
	private Properties prop;
	private PageObj pageObj;
	private String sTCName;
	private String env;
	private String baseUrl;
	private static Map<String, String> dataSet;
	String run = "ui";
	Utilities utils;

	@BeforeClass(alwaysRun = true)
	public void openBrowser() {
		driver = new BrowserUtilities().launchBrowser();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		// single Login to instance
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		pageObj.instanceDashboardPage().instanceLogin(baseUrl);
		utils = new Utilities(driver);
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
	}
	// utils = new Utilities(driver);

	// Anant
	// create a tag with name "DoNotDelete!@#$%^&*()" and attach it to any
	// "campaign"
	@Test(description = "SQ-T4874 Verify URL when Tag and Creator filter is selected and then creator name deleted partially from URL", priority = 0)
	@Owner(name = "Rakhi Rawat")
	public void T4874_deletePartiallyFromUrl() throws InterruptedException {
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */

		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// goto new campaign page
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		utils.waitTillNewCamsTableAppear();

		// select tag and creator
		pageObj.newCamHomePage().clickMoreFilterBtn();
		pageObj.newCamHomePage().selectDrpDownValFromSidePanel(dataSet.get("tag"), dataSet.get("filter1"));
		pageObj.newCamHomePage().selectDrpDownValFromSidePanel(dataSet.get("creator"), dataSet.get("filter2"));
		pageObj.newCamHomePage().sidePanelDrpDownExpand("Creator");
		pageObj.newCamHomePage().clickSidePanelApplyBtn();

		String value = pageObj.newCamHomePage().getValueFromUrlUsingRE(dataSet.get("creatorRE"));
		pageObj.newCamHomePage().moveToUrl(value);
		boolean val = pageObj.newCamHomePage().selectedFilterVisible(dataSet.get("filter2"));
		Assert.assertFalse(val, "when creator value is removed from the url then also in the UI value is displayed");
		utils.logPass("Verified when creator value is removed from the url then from the UI also filter is removed");
	}

	// Anant
	@Test(description = "SQ-T4861 Verify URL when Campaign Type filter is selected", priority = 1)
	public void T4861_verifyUrlWhenCampaignFilter() throws InterruptedException {

		String lst = dataSet.get("filterLst");
		String[] arr = lst.split(",");
		List<String> actualLst = new ArrayList<>();

		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */

		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// goto new campaign page
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		for (int i = 0; i < arr.length; i++) {
			pageObj.newCamHomePage().clickMoreFilterBtn();
			pageObj.newCamHomePage().selectDrpDownValFromSidePanel(arr[i], dataSet.get("filter"));
			pageObj.newCamHomePage().clickSidePanelApplyBtn();
			String value = pageObj.newCamHomePage().getValueFromUrlUsingRE(dataSet.get("campaignRE"));
			actualLst.add(value);
			pageObj.newCamHomePage().removeFilter(dataSet.get("filter"));
		}
		Assert.assertEquals(actualLst, dataSet.get("expectedLst"),
				"url did not changed when campaign type is selected");
		utils.logPass("Verified url is changed when campaign type is selected");
	}

	// shaleen
	@Test(description = "SQ-T4915 Verify the feature of Archive modal dialog", priority = 2)
	public void T4915_verifyDailogueBoxOfArchiveCampaign() throws InterruptedException {

		// open business
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */

		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// select checkbox and verify bulk action
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickMoreFilterBtn();
		pageObj.newCamHomePage().sidePanelDrpDownClick("Status");
		pageObj.newCamHomePage().selectFilter("Status", "Draft");
		pageObj.newCamHomePage().selectFilter("Status", "Active");
		pageObj.newCamHomePage().clickSidePanelApplyBtn();

		pageObj.newCamHomePage().selectAllCampaignCheckBox();
		String count = pageObj.newCamHomePage().selectedCampaignCount();
		Assert.assertEquals(count, dataSet.get("count"), "All 10 campaigns are not selected");
		utils.logPass("Verified all campaigns are selected");

		String tagButton = pageObj.newCamHomePage().selectedBulkCampaignOptionVisible(dataSet.get("tagButton"));
		Assert.assertEquals(tagButton, dataSet.get("tagButton"), " Tag button is not visible ");
		utils.logPass("Verified Tag button is visible ");

		String deleteButton = pageObj.newCamHomePage().selectedBulkCampaignOptionVisible(dataSet.get("deleteButton"));
		Assert.assertEquals(deleteButton, dataSet.get("deleteButton"), " delete button is not visible ");
		utils.logPass("Verified delete button is visible ");

		String archiveButton = pageObj.newCamHomePage().selectedBulkCampaignOptionVisible(dataSet.get("archiveButton"));
		Assert.assertEquals(archiveButton, dataSet.get("archiveButton"), "  Archive button is not visible ");
		utils.logPass("Verified Archive button is visible ");

		String clearButton = pageObj.newCamHomePage().selectedBulkCampaignClearSelectionVisible();
		Assert.assertEquals(clearButton, dataSet.get("clearButton"), "Clear selection button is not visible ");
		utils.logPass("Verified Clear selection button is visible ");

		// click archive and verify modal dialogue box
		pageObj.newCamHomePage().selectedBulkCampaignOptionSelect(dataSet.get("archiveButton"));
		String title = pageObj.newCamHomePage().modalDialogueBoxContent();
		boolean verify = title.contains(dataSet.get("expectedTitle"));
		Assert.assertTrue(verify, " Archive campaigns heading not appeared ");
		utils.logPass("Verified Archive campaigns heading appeared ");

		List<String> body = pageObj.newCamHomePage().getArchiveModalText();
		Assert.assertEquals(body.get(0), dataSet.get("modalBoxMesssage"), "expected msg is not equal");
		String text2 = dataSet.get("modalBoxMesssage2");
		Assert.assertTrue(body.get(1).contains(text2), "expected msg is not equal");
		pageObj.newCamHomePage().getCampaignsInArchivalModalBox();

		String yesButton = pageObj.newCamHomePage().archiveCampaignModalDialogueButtonvisible(dataSet.get("yes"));
		Assert.assertEquals(yesButton, dataSet.get("yes"), "<Yes, archive> button is not visible");
		utils.logPass("Verified <Yes, archive> button is visible");

		String noButton = pageObj.newCamHomePage().archiveCampaignModalDialogueButtonvisible(dataSet.get("no"));
		Assert.assertEquals(noButton, dataSet.get("no"), "<No, go back> button is not visible ");
		utils.logPass(" Verified <No, go back> button is visible ");

	}

	// shaleen
	@Test(description = "SQ-T4923 (1.0) Verify success message in case of recurring mass campaign when it run only one-time", priority = 3)
	@Owner(name = "Rakhi Rawat")
	public void T4923_verifySuccessMessageMassRecurringCampaign() throws InterruptedException {

		String campaignName = "Automation-recurringMassOffer_Campaign" + CreateDateTime.getTimeDateString();
		String startDate = CreateDateTime.getFutureDate(1);
		String endDate = CreateDateTime.getFutureDate(4);

		// open business
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */

		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// create campaign
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().createMassCampaignCHP("Mass campaign", "Yes");
		pageObj.signupcampaignpage().createWhatDetailsMassCampaign(campaignName, dataSet.get("giftType"),
				dataSet.get("redemable"));
		pageObj.signupcampaignpage().createWhomDetailsMassCampaign(dataSet.get("audiance"), dataSet.get("segment"),
				dataSet.get("pushNotification"), dataSet.get("emailSubject"), dataSet.get("emailTemplate"));
//			pageObj.signupcampaignpage().createWhenDetailsMassCampaign("Once", dateTime);

		// when details of campaign
		pageObj.signupcampaignpage().setFrequency(dataSet.get("frequency"));
		pageObj.signupcampaignpage().setStartEndDateTime(startDate, endDate);

		// schedule run
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType("Mass Campaign Schedules");
		pageObj.schedulespage().findMassCampaignNameandRun(campaignName);

		// goto new campaign homepage and verify toast message
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		String ActualCampStatus = pageObj.newCamHomePage().getAnyCampaignStatusWithPoolingNCHP(campaignName,
				"Processed");
		Assert.assertEquals(ActualCampStatus, "Processed", "Campaign is not in Processed state");
		utils.logit("Campaign status is " + ActualCampStatus);
		pageObj.newCamHomePage().clickOptionFromDotsDropDown("Export report");
		String toastMessage = pageObj.newCamHomePage().getToastMessage();
		boolean verify = toastMessage.contains(dataSet.get("toastMessage"));
		Assert.assertTrue(verify, " toast message not visible ");
		utils.logPass("Verified Toast message is visible ");

	}

	// shaleen
	@Test(description = "SQ-T4776 UI & UX cleanup from MVP release", priority = 4)
	@Owner(name = "Rakhi Rawat")
	public void T4776_verifyWorkingOfMassOnceAndMassRecurrenceFilter() throws InterruptedException {
		// open business
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */

		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// click on campaign
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// click on more filter
		pageObj.newCamHomePage().clickMoreFilterBtn();
		pageObj.newCamHomePage().selectDrpDownValFromSidePanel("Mass (Repeating)", "Type");
		pageObj.newCamHomePage().sidePanelDrpDownExpand("Type");
		pageObj.newCamHomePage().clickSidePanelApplyBtn();

		// select Mass recurrence campaign
		String url = driver.getCurrentUrl();
		boolean verifyMassRepeatingFilter = url.contains(dataSet.get("massRepeatingFilter"));
		Assert.assertEquals(verifyMassRepeatingFilter, true, " Mass recurrence campaign filter not applied ");
		utils.logPass("Verified Mass recurrence campaign filter applied ");

		// select Mass Once campaign
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickMoreFilterBtn();
		pageObj.newCamHomePage().selectDrpDownValFromSidePanel("Mass (One-time)", "Type");
		pageObj.newCamHomePage().sidePanelDrpDownExpand("Type");
		pageObj.newCamHomePage().clickSidePanelApplyBtn();

		String url2 = driver.getCurrentUrl();
		boolean verifyMassOnceFilter = url2.contains(dataSet.get("massOnceFilter"));
		Assert.assertEquals(verifyMassOnceFilter, true, " Mass once campaign filter not applied ");
		utils.logPass("Verified Mass once campaign filter applied ");

	}

	// shaleen
	@Test(description = "SQ-T4790 Campaign Action - Duplicate campaign", priority = 5)
	public void T4790_verifyDuplicateCampaignAction() throws InterruptedException {
		// open business
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */

		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// goto new cam homepage
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().searchCampaign(dataSet.get("campName"));
		pageObj.newCamHomePage().viewCampaignSidePanel();
		String campSourceId = pageObj.newCamHomePage().getCampSourceIDFromSidePanel();
		pageObj.newCamHomePage().closeCampaignSidePanel();
		pageObj.newCamHomePage().clickOptionFromDotsDropDown("Duplicate");

		// verify source id
		String id = pageObj.newCamHomePage().getValueFromUrlUsingRE(dataSet.get("sourceID_RE"));
		Assert.assertEquals(id, campSourceId, " Does not navigate to same campaign ");
		utils.logPass(" Verified user navigate into campaign that is duplicating ");

	}

	// shaleen
	@Test(description = "SQ-T4905 (1.0) verify the archival of mass offer campaign with Pending Approval status when end date is in past ||"
			+ "SQ-T4916 (1.0) When users click pending approval in the main navigation under campaigns they should go to a page with their franchisee campaigns", groups = {"nonNightly"}, priority = 5)
	@Owner(name = "Shaleen Gupta")
	public void T4905_archivePendingApprovalMassCampaignEndDateInPast() throws Exception {

		// open business
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */

		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// set end date in database
		String pastDate = CreateDateTime.getYesterdaysDate();
		String minute = Integer.toString(Utilities.getRandomNoFromRange(10, 60));

		String pastDateTime = pastDate + " 01:" + minute + ":00";
		String query = "UPDATE `schedules` SET `end_time`='" + pastDateTime + "' WHERE `source_id`='"
				+ dataSet.get("source_id") + "';";
		int status = DBUtils.executeUpdateQuery(env, query);
		Assert.assertEquals(status, 1, " Unable to update end date in past ");
		utils.logPass(" Successfully Updated end date in past ");

		// navigate to new CHP and verify
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().searchCampaign(dataSet.get("source_id"));
		boolean verify = pageObj.newCamHomePage().checkOptionPresent("Archive");
		Assert.assertTrue(verify, " Archive option is not present ");
		utils.logPass(" Verified archival of pending Approval mass campaign whose end date in past ");

		// verify Pending Approval filter
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Pending Approval");
		String value = pageObj.newCamHomePage().getSelectedFilter("Status");
		boolean flag = value.contains("Pending Approval");
		Assert.assertTrue(flag, "Pending Approval Filter not Applied");
		utils.logPass("Verfied that user navigates to new CHP page with pending approval as filter");

	}

	// shaleen
	@Test(description = "SQ-T5249 Add error toast for uneditable campaign", priority = 6)
	@Owner(name = "Vansham Mishra")
	public void T5249_verifyErrorMessageForUneditableCampaign() throws Exception {

		// open business
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */

		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// navigate to NCHP
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().searchCampaign(dataSet.get("camp1"));
		String status = pageObj.newCamHomePage().getCampaignStatus();
		Assert.assertEquals(status, "On Hold", "Campaign is not in On Hold status");
		utils.logPass("Verfied that campaign is in On Hold status");

		// verify editing of campaign
		pageObj.newCamHomePage().clickOptionFromDotsDropDown("Edit");
		String msg = pageObj.newCamHomePage().getToastMessage();
		boolean verify = msg.contains(dataSet.get("expectedMsg"));
		Assert.assertTrue(verify, "Expected error message did not matched");
		utils.logPass("Verified that campaign in On Hold status and passed its start date are uneditable");

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
