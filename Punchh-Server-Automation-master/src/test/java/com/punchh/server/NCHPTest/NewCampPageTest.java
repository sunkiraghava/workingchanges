package com.punchh.server.NCHPTest;

import java.lang.reflect.Method;
import java.util.ArrayList;
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
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.annotations.Owner;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class NewCampPageTest {

	private static Logger logger = LogManager.getLogger(NewCampPageTest.class);
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
	}

	// Anant
	@Test(description = "SQ-T4760 (1.0) Verify URL when Gift Type and Location filter is selected"
			+ "SQ-T4761 (1.0) Verify URL when Creator filter is selected", groups = { "regression",
					"dailyrun" }, priority = 0)
	@Owner(name = "Rakhi Rawat")
	public void T4760_verifyUrlNewCampaignPage() throws InterruptedException {
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */

		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// goto new campaign page
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		// select gift type and location
		pageObj.newCamHomePage().clickMoreFilterBtn();
		pageObj.newCamHomePage().checkGiftType();
		pageObj.newCamHomePage().searchFieldSidePanel(dataSet.get("filterVal1"), dataSet.get("filter1"));
		pageObj.newCamHomePage().selectGiftTypeFromDropDown(dataSet.get("filter1"), 5);
		pageObj.newCamHomePage().sidePanelDrpDownExpand(dataSet.get("filterVal1"));
		pageObj.newCamHomePage().selectDrpDownValFromSidePanel(dataSet.get("filter2"), dataSet.get("filterVal2"));
		pageObj.newCamHomePage().clickSidePanelApplyBtn();
		String giftTypeVal = pageObj.newCamHomePage().getValueFromUrlUsingRE(dataSet.get("giftTypeRE"));
		String locationVal = pageObj.newCamHomePage().getValueFromUrlUsingRE(dataSet.get("locationRE"));
		Assert.assertEquals(giftTypeVal, dataSet.get("filter1"), "gift type url value is not match");
		Assert.assertEquals(locationVal, dataSet.get("filter2"), "location url value is not match");
		utils.logPass("Verified when the value is selected from gift type and location filter URL changed");

		// removed the selected filter
		pageObj.newCamHomePage().removeGiftTypeFilter();
		pageObj.newCamHomePage().removeFilter(dataSet.get("filterVal2"));

		// select creator
		pageObj.newCamHomePage().clickMoreFilterBtn();
		pageObj.newCamHomePage().selectDrpDownValFromSidePanel(dataSet.get("filterVal3"), dataSet.get("filter3"));
		pageObj.newCamHomePage().clickSidePanelApplyBtn();
		String creatorVal = pageObj.newCamHomePage().getValueFromUrlUsingRE(dataSet.get("creatorRE"));
		creatorVal = creatorVal.replace("+", " ");
		String[] arr = creatorVal.split("%");
		Assert.assertEquals(arr[0], dataSet.get("filterVal3"),
				"url value is not updated when alue from creator filter is selected");
		utils.logPass("Verified when the value is selected from creator filter URL changed");
	}

	// Anant
	@Test(description = "SQ-T4753 Verify the appearance of \"Try CMT\" and \"Switch to Classic\" buttons on Campaigns page? flag"
			+ "SQ-T4754 Verify the appearance of \"Switch to Classic\" button when \"Try CMT\" and \"Switch to Classic\" buttons on Campaigns page? flag is off"
			+ "SQ-T4755 Verify the appearance of \"Switch to Classic\" button when \"Try CMT\" and \"Switch to Classic\" buttons on Campaigns page? flag is on", priority = 1)
	public void T4753_verifyTryCmtFlag() throws InterruptedException {
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */

		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_show_new_and_classic_chp_switch_buttons",
				"uncheck");
		pageObj.dashboardpage().clickOnUpdateButton();

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		boolean val = pageObj.newCamHomePage().switchToClassicVisible();
		Assert.assertFalse(val, "switch to classic button is still visible");
		utils.logPass("Verify switch to classic is not visible when flag is off");

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_show_new_and_classic_chp_switch_buttons", "check");
		pageObj.dashboardpage().clickOnUpdateButton();

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		boolean val1 = pageObj.newCamHomePage().switchToClassicVisible();
		Assert.assertTrue(val1, "switch to classic button is not visible");
		utils.logPass("Verify switch to classic is visible when flag is ON");

	}

	// Anant
	@Test(description = "SQ-T4758 (1.0) Verify that the \"Export report\" button is available for Mass, Post-checkin, Challenge, and Social Cause campaign types.", groups = {
			"regression", "dailyrun" }, priority = 2)
	@Owner(name = "Vansham Mishra")
	public void T4758_VerifyExportCodeAndCouponCode() throws InterruptedException {
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */

		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// go to campaign page
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		for (int i = 1; i < 7; i++) {
			pageObj.newCamHomePage().searchCampaign(dataSet.get("camp" + i));
			List<String> lst = pageObj.newCamHomePage().optionsVisibleInThreeDotsOfCampaign();
			logger.info(lst);
			if (i == 1) {
				Assert.assertTrue(lst.contains(dataSet.get("expected1")), dataSet.get("expected1")
						+ " value is not visible in the campaign -- " + dataSet.get("camp" + i));
				Assert.assertTrue(lst.contains(dataSet.get("expected2")), dataSet.get("expected2")
						+ " value is not visible in the campaign -- " + dataSet.get("camp" + i));
				utils.logPass("Verified " + dataSet.get("expected1") + " and " + dataSet.get("expected2")
						+ " are visible in the campaign -- " + dataSet.get("camp" + i));
			} else if (i == 2) {
				Assert.assertTrue(lst.contains(dataSet.get("expected1")), dataSet.get("expected1")
						+ " value is not visible in the campaign -- " + dataSet.get("camp" + i));
				Assert.assertTrue(lst.contains(dataSet.get("expected4")), dataSet.get("expected4")
						+ " value is not visible in the campaign -- " + dataSet.get("camp" + i));
				utils.logPass("Verified " + dataSet.get("expected1") + " and " + dataSet.get("expected4")
						+ " are visible in the campaign -- " + dataSet.get("camp" + i));
			} else {
				Assert.assertTrue(lst.contains(dataSet.get("expected3")), dataSet.get("expected3")
						+ " value is not visible in the campaign -- " + dataSet.get("camp" + i));
				utils.logPass("Verified " + dataSet.get("expected3")
						+ " is visible in the campaign -- " + dataSet.get("camp" + i));
			}
		}
	}

	// Amit
	@Test(description = "SQ-T4757 (1.0) Verify that \"Export codes\" and \"Coupon codes list\" are available for Coupon campaigns.", groups = {
			"regression", "dailyrun" }, priority = 3)
	public void T4757_VerifyExportCodeAndCouponCode1() throws InterruptedException {
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */

		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// go to campaign page
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

	}

	// Anant
	@Test(description = "SQ-T4781 (1.0) Error validation for create tag name"
			+ "SQ-T4782 (1.0) Rename tag in Manage Tag Modal", groups = { "regression", "unstable",
					"dailyrun" }, priority = 5)
	@Owner(name = "Rakhi Rawat")
	public void T4781_renameTagAndErrorValidation() throws InterruptedException {
		String tag = "Tag" + CreateDateTime.getTimeDateString();
		String tag2 = "a";
		String renameTag = utils.getAlphaNumericString(10);

		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */

		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// go to campaign page
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickManageTagsBtn();
		pageObj.newCamHomePage().clickCreateTagBtn();
		String msg = pageObj.newCamHomePage().createNewTag(tag2);
		Assert.assertEquals(msg, dataSet.get("errorMsg"),
				"when single character is given while creating tag error msg is not displayed");
		utils.logPass("Verified when single character is given error msg is visible");
		pageObj.newCamHomePage().closeCreateTagPopup();
		pageObj.newCamHomePage().closeMangeTagFrame();
		// create a new tag
		// pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickManageTagsBtn();
		pageObj.newCamHomePage().clickCreateTagBtn();

		msg = pageObj.newCamHomePage().createNewTag(tag);
		Assert.assertEquals(msg, dataSet.get("creationMsg"), "tag is not created");
		utils.logit("tag is created successfully " + tag);
//		pageObj.newCamHomePage().closeCreateTagPopup();

		pageObj.newCamHomePage().closeMangeTagFrame();
		// create a new tag with the same name
		// pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickManageTagsBtn();
		pageObj.newCamHomePage().clickCreateTagBtn();

		msg = pageObj.newCamHomePage().createNewTag(tag);
		Assert.assertEquals(msg, dataSet.get("errorMsg2"),
				"when creating a tag with the same name which is already present then error msg is not visible");
		utils.logPass(
				"Verified when creating a tag with the same name which is already present then error msg is visible");
		pageObj.newCamHomePage().closeCreateTagPopup();

		pageObj.newCamHomePage().closeMangeTagFrame();
		// rename the tag
		// pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickManageTagsBtn();
		utils.waitTillNewCamsTableAppear();
		pageObj.newCamHomePage().renameTag(tag, renameTag);

		// delete the tag
		// pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickManageTagsBtn();
		pageObj.newCamHomePage().deleteTag(renameTag);
		// pageObj.newCamHomePage().closeMangeTagFrame();
	}

	// Anant
	@Test(description = "SQ-T4787 (1.0) Campaign home page filters should respect tab selection Others tab", groups = {
			"regression", "dailyrun" }, priority = 6)
	@Owner(name = "Rakhi Rawat")
	public void T4787_campaignHomePageFilter() throws InterruptedException {
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */

		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// goto new campaign page
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// other tab
		pageObj.newCamHomePage().switchTab(dataSet.get("tab"));
		pageObj.newCamHomePage().clickMoreFilterBtn();
		//
		pageObj.newCamHomePage().sidePanelDrpDownExpand(dataSet.get("filter1"));
		List<String> filterVal1 = pageObj.newCamHomePage().getFilterDropDownvalues(dataSet.get("filter1"));
		Assert.assertEquals(filterVal1, dataSet.get("expectedLst1"),
				"when other tab is selected then campaign type list is not coming as expected");
		utils.logPass("Verified when other tab is selected then campaign type list is coming as expected");

		// goto new campaign page
		/*
		 * pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		 * pageObj.newCamHomePage().switchTab(dataSet.get("tab"));
		 * pageObj.newCamHomePage().clickMoreFilterBtn();
		 */
		//
		pageObj.newCamHomePage().sidePanelDrpDownExpand("Type");
		pageObj.newCamHomePage().sidePanelDrpDownExpand(dataSet.get("filter2"));
		List<String> filterVal2 = pageObj.newCamHomePage().getFilterDropDownvalues(dataSet.get("filter2"));
		Assert.assertEquals(filterVal2, dataSet.get("expectedLst2"),
				"when other tab is selected then campaign status list is not coming as expected");
		utils.logPass("Verified when other tab is selected then campaign status list is coming as expected");
	}

	// Anant
	@Test(description = "SQ-T4786 (1.0) Campaign home page filters should respect tab selection Automations tab", groups = {
			"regression", "unstable", "dailyrun" }, priority = 7)
	@Owner(name = "Rakhi Rawat")
	public void T4786_campaignHomePageFilter() throws InterruptedException {
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */

		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// goto new campaign page
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().switchTab(dataSet.get("tab"));
		pageObj.newCamHomePage().clickMoreFilterBtn();

		pageObj.newCamHomePage().sidePanelDrpDownExpand(dataSet.get("filter1"));
		List<String> filterVal1 = pageObj.newCamHomePage().getFilterDropDownvalues(dataSet.get("filter1"));
		Assert.assertEquals(filterVal1, dataSet.get("expectedLst1"),
				"when other tab is selected then campaign type list is not coming as expected");
		utils.logPass("Verified when other tab is selected then campaign type list is coming as expected");

		// goto new campaign page
		/*
		 * pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		 * pageObj.newCamHomePage().switchTab(dataSet.get("tab"));
		 * pageObj.newCamHomePage().clickMoreFilterBtn();
		 */
		pageObj.newCamHomePage().sidePanelDrpDownExpand("Type");
		pageObj.newCamHomePage().sidePanelDrpDownExpand(dataSet.get("filter2"));
		List<String> filterVal2 = pageObj.newCamHomePage().getFilterDropDownvalues(dataSet.get("filter2"));
		Assert.assertEquals(filterVal2, dataSet.get("expectedLst2"),
				"when other tab is selected then campaign status list is not coming as expected");
		utils.logPass("Verified when other tab is selected then campaign status list is coming as expected");
	}

	// Anant
	@Test(description = "SQ-T4785 (1.0) Campaign home page filters should respect tab selection Mass tab", groups = {
			"regression", "unstable" }, priority = 8)
	@Owner(name = "Rakhi Rawat")
	public void T4785_campaignHomePageFilter() throws InterruptedException {
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */

		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// goto new campaign page
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().switchTab(dataSet.get("tab"));
		pageObj.newCamHomePage().clickMoreFilterBtn();

		pageObj.newCamHomePage().sidePanelDrpDownExpand(dataSet.get("filter1"));
		List<String> filterVal1 = pageObj.newCamHomePage().getFilterDropDownvalues(dataSet.get("filter1"));
		Assert.assertEquals(filterVal1, dataSet.get("expectedLst1"),
				"when other tab is selected then campaign type list is not coming as expected");
		utils.logPass("Verified when other tab is selected then campaign type list is coming as expected");

		// goto new campaign page
		/*
		 * pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		 * pageObj.newCamHomePage().switchTab(dataSet.get("tab"));
		 * pageObj.newCamHomePage().clickMoreFilterBtn();
		 */
		pageObj.newCamHomePage().sidePanelDrpDownExpand("Type");
		pageObj.newCamHomePage().sidePanelDrpDownExpand(dataSet.get("filter2"));
		List<String> filterVal2 = pageObj.newCamHomePage().getFilterDropDownvalues(dataSet.get("filter2"));
		Assert.assertEquals(filterVal2, dataSet.get("expectedLst2"),
				"when other tab is selected then campaign status list is not coming as expected");
		utils.logPass("Verified when other tab is selected then campaign status list is coming as expected");
	}

	// Anant/shashank (fixed removed dataprovider)
	@Test(description = "SQ-T4888 Verify more filter side panel when applied filters are Type( @, $, %, ^, *, (, % ) and Tag( @, $, %, ^, *, (, % )", groups = {
			"regression", "dailyrun" }, priority = 9)
	@Owner(name = "Rakhi Rawat")
	public void T4888_verifyFilterWhenSpecialCharacter() throws InterruptedException {
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */

		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		String typesFilterValues = dataSet.get("filterValue");
		// goto new campaign page and search for campaign type
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		List<String> listOfTypeFilter = Arrays.asList(typesFilterValues.split(","));
		pageObj.newCamHomePage().clickMoreFilterBtn();
		pageObj.newCamHomePage().sidePanelDrpDownExpand("Type");
		for (String filterTypeValue : listOfTypeFilter) {
			Thread.sleep(1000);
			pageObj.newCamHomePage().searchValueInSidePanel(dataSet.get("filter1"), filterTypeValue);
			int size = pageObj.newCamHomePage().searchBoxResultList(dataSet.get("filter1"));
			Assert.assertNotEquals(size, 0, "when enter special character -- " + filterTypeValue
					+ " entered in the campaign type search it is not showing the list");
			utils.logPass("Verified when enter special character -- " + filterTypeValue
					+ " entered in the campaign type search it is showing the list");
			pageObj.newCamHomePage().clearSearchValueInSidePanel(dataSet.get("filter1"));

		}

		pageObj.newCamHomePage().sidePanelDrpDownExpand("Type");
		pageObj.newCamHomePage().sidePanelDrpDownExpand("Tag");
		String tagsFilterValues = dataSet.get("filterValue2");
		List<String> listOfTagFilter = Arrays.asList(tagsFilterValues.split(","));
		for (String filterTagValue : listOfTagFilter) {
			Thread.sleep(1000);
			pageObj.newCamHomePage().searchValueInSidePanel(dataSet.get("filter2"), filterTagValue);
			int size = pageObj.newCamHomePage().searchBoxResultList(dataSet.get("filter2"));
			Assert.assertNotEquals(size, 0, "when enter special character -- " + filterTagValue
					+ " entered in the campaign type search it is not showing the list");
			utils.logPass("Verified when enter special character -- " + filterTagValue
					+ " entered in the campaign type search it is showing the list");

			pageObj.newCamHomePage().clearSearchValueInSidePanel(dataSet.get("filter2"));
		}

	}

	// Anant
	@Test(description = "SQ-T4793 (1.0) Checkbox column - Bulk delete campaign(s)"
			+ "SQ-T4792 (1.0) Checkbox column - Bulk delete campaign(s)", groups = { "regression",
					"dailyrun" }, priority = 10)
	public void T4793_BulkCampaignDelete() throws InterruptedException {
		String campaignName = "Automation Location";
		String campaignName2 = "Automation Location 2";

		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */

		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// switch to new cam page and select location presence
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().createOtherCampaignCHP(dataSet.get("campaignCategory"), dataSet.get("campaignType"));
		pageObj.signupcampaignpage().presenceCampaignWhatPage(campaignName, dataSet.get("giftType"),
				dataSet.get("redeemableName"), dataSet.get("giftReason"));
		pageObj.signupcampaignpage().clickNextBtn();

		// switch to new cam page and select location presence
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().createOtherCampaignCHP(dataSet.get("campaignCategory"), dataSet.get("campaignType"));
		pageObj.signupcampaignpage().presenceCampaignWhatPage(campaignName2, dataSet.get("giftType"),
				dataSet.get("redeemableName"), dataSet.get("giftReason"));
		pageObj.signupcampaignpage().clickNextBtn();

		// search the campaign
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().searchCampaign(campaignName);

		// select the campaign
		pageObj.newCamHomePage().selectCampaignFromNewCampaignPage(campaignName);
		pageObj.newCamHomePage().selectCampaignFromNewCampaignPage(campaignName2);

		// select campaign count
		String count = pageObj.newCamHomePage().selectedCampaignCount();
		Assert.assertEquals(count, "2", "2 campaigns are not selected");
		utils.logPass("Verified 2 campaigns are selected");

		// select the delete option
		pageObj.newCamHomePage().selectedBulkCampaignOptionSelect(dataSet.get("optionName"));

		// delete the campaign
		String str = pageObj.newCamHomePage().deleteCampaignModelText();
		Assert.assertTrue(str.contains(dataSet.get("expectedMessage1")), "expected message did not match");
		Assert.assertTrue(str.contains(dataSet.get("expectedMessage2")), "expected message did not match");
		utils.logPass("Verified expected message is displayed in the delete modal");

		pageObj.newCamHomePage().deleteBulkCampaign();
	}

	// Anant
	@Test(description = "SQ-T4789 No results found when using search & filter combined"
			+ "SQ-T4768 Update search CTA content for \"No matches found\""
			+ "SQ-T4869 Verify URL when text is searched from search field", groups = { "regression",
					"dailyrun" }, priority = 11)
	public void T4789_noResultUsingSearchAndFilter() throws InterruptedException {
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */

		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// nagivate to campaign Page
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		// apply filter and enter random text
		pageObj.newCamHomePage().clickMoreFilterBtn();
		pageObj.newCamHomePage().selectDrpDownValFromSidePanel(dataSet.get("filterValue"), dataSet.get("filter"));
		pageObj.newCamHomePage().clickSidePanelApplyBtn();

		pageObj.newCamHomePage().searchCampaignNew(dataSet.get("randomCharacters"));
		boolean value = pageObj.newCamHomePage().noResultFoundVisible();
		Assert.assertTrue(value, "when random values are enter in the search box 'no match found' is not visible ");
		utils.logPass("Verified when random values are enter in the search box 'no match found' is visible");

		String 		msg = pageObj.newCamHomePage().invalidSearchMsg();
		Assert.assertTrue(msg.contains(dataSet.get("expectedMsg1")), "error msg is not equal");
		utils.logPass("Verified error msg when random value and any filter is selected");

		value = pageObj.newCamHomePage().clearSearchAndFilterButtonVisible();
		Assert.assertTrue(value,
				"when random values and filter is selected then 'clear search and filter' button is not visible");
		utils.logPass(
				"Verified when random values and filter is selected then 'clear search and filter' button is visible");

		// enter random text
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		pageObj.newCamHomePage().searchCampaignNew(dataSet.get("randomCharacters"));
		value = pageObj.newCamHomePage().noResultFoundVisible();
		Assert.assertTrue(value, "when random values are enter in the search box 'no match found' is not visible ");
		utils.logPass("Verified when random values are enter in the search box 'no match found' is visible");

		msg = pageObj.newCamHomePage().invalidSearchMsg();
		Assert.assertTrue(msg.contains(dataSet.get("expectedMsg2")), "error msg is not equal");
		utils.logPass("Verified error msg when random value are entered");

		value = pageObj.newCamHomePage().clearSearchButtonVisible();
		Assert.assertTrue(value, "when random values are enterd then 'clear search' button is not visible");
		utils.logPass("Verified when random values are entered then 'clear search' button is visible");

		// check the URL
		String itemVal = pageObj.newCamHomePage().getValueFromUrlUsingRE(dataSet.get("searchRE"));
		Assert.assertEquals(itemVal, dataSet.get("randomCharacters"), "searched value is not displayed in URL");
		utils.logPass("Verified searched value is displayed in URL");
	}

	// Anant
	@Test(description = "SQ-T4769 Bento changes for UI & UX cleanup from MVP release"
			+ "SQ-T4770 Bento changes for UI & UX cleanup from MVP release -- 3 or more items"
			+ "SQ-T4774 UI & UX cleanup from MVP release deactivate"
			+ "SQ-T4815 Verify appearance of filters on new campaign home page", groups = { "unstable", "regression",
					"dailyrun" }, priority = 12)
	@Owner(name = "Rakhi Rawat")
	public void T4769_BentoChangesForUI() throws InterruptedException {
		List<String> lst = new ArrayList<>();
		lst.add(dataSet.get("option1"));
		lst.add(dataSet.get("option2"));

		List<String> lst2 = new ArrayList<>();
		lst2.add(dataSet.get("option3"));
		lst2.add(dataSet.get("option4"));

		List<String> lst3 = new ArrayList<>();
		lst3.add(dataSet.get("option5"));
		lst3.add(dataSet.get("option6"));

		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */

		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// goto new campaign page
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		// select filter value
		pageObj.newCamHomePage().clickMoreFilterBtn();
		pageObj.newCamHomePage().selectMultipleDrpDownValFromSidePanel(lst, dataSet.get("filter1"));
		pageObj.newCamHomePage().sidePanelDrpDownExpand("Type");
		pageObj.newCamHomePage().selectMultipleDrpDownValFromSidePanel(lst2, dataSet.get("filter2"));
		pageObj.newCamHomePage().sidePanelDrpDownExpand("Status");
		pageObj.newCamHomePage().selectMultipleDrpDownValFromSidePanel(lst3, dataSet.get("filter3"));
		pageObj.newCamHomePage().sidePanelDrpDownExpand("Segment");
		pageObj.newCamHomePage().clickSidePanelApplyBtn();

		String actual1 = pageObj.newCamHomePage().selectedFilterTextVisible(dataSet.get("filter1"));
		String actual2 = pageObj.newCamHomePage().selectedFilterTextVisible(dataSet.get("filter2"));
		String actual3 = pageObj.newCamHomePage().selectedFilterTextVisible(dataSet.get("filter3"));

		String expected1 = dataSet.get("option1") + ", " + dataSet.get("option2");
		String expected2 = dataSet.get("option3") + ", " + dataSet.get("option4");
		String expected3 = dataSet.get("option5") + ", " + dataSet.get("option6");

		Assert.assertEquals(actual1, expected1, "selected campaign type filter is not visible");
		Assert.assertEquals(actual2, expected2, "selected Status filter is not visible");
		Assert.assertEquals(actual3, expected3, "selected segment filter is not visible");
		utils.logPass("Verified selected filters are visible on the UI");

		// removed the selected filter
		pageObj.newCamHomePage().removeFilter(dataSet.get("filter1"));
		pageObj.newCamHomePage().removeFilter(dataSet.get("filter2"));
		pageObj.newCamHomePage().removeFilter(dataSet.get("filter3"));

		lst.add(dataSet.get("option7"));
		lst2.add(dataSet.get("option8"));

		// select filter value
		pageObj.newCamHomePage().clickMoreFilterBtn();
		pageObj.newCamHomePage().selectMultipleDrpDownValFromSidePanel(lst, dataSet.get("filter1"));
		pageObj.newCamHomePage().sidePanelDrpDownExpand("Type");
		pageObj.newCamHomePage().selectMultipleDrpDownValFromSidePanel(lst2, dataSet.get("filter2"));
		pageObj.newCamHomePage().sidePanelDrpDownExpand("Status");
		pageObj.newCamHomePage().selectMultipleDrpDownValFromSidePanel(lst3, dataSet.get("filter3"));
		pageObj.newCamHomePage().sidePanelDrpDownExpand("Segment");
		pageObj.newCamHomePage().clickSidePanelApplyBtn();

		actual1 = pageObj.newCamHomePage().selectedFilterTextVisible(dataSet.get("filter1"));
		actual2 = pageObj.newCamHomePage().selectedFilterTextVisible(dataSet.get("filter2"));
		actual3 = pageObj.newCamHomePage().selectedFilterTextVisible(dataSet.get("filter3"));

		Assert.assertTrue(actual1.contains(dataSet.get("expectedValue")),
				"selected campaign type filter is not visible");
		Assert.assertTrue(actual1.contains(dataSet.get("expectedValue")), "selected Status filter is not visible");
		Assert.assertTrue(actual1.contains(dataSet.get("expectedValue")), "selected segment filter is not visible");
		utils.logPass("Verified selected filters are visible on the UI");

		// removed the selected filter
		pageObj.newCamHomePage().removeFilter(dataSet.get("filter1"));
		pageObj.newCamHomePage().removeFilter(dataSet.get("filter2"));
		pageObj.newCamHomePage().removeFilter(dataSet.get("filter3"));

		// select filter value
		pageObj.newCamHomePage().clickMoreFilterBtn();
		List<String> lst5 = pageObj.newCamHomePage().getSidePanelFilterList();
		Assert.assertEquals(lst5, dataSet.get("expectedLst"), "filter present in the side panel are not equal");
		utils.logPass("Verified filter present in the side panel are equal");

		pageObj.newCamHomePage().sidePanelDrpDownExpand(dataSet.get("filter2"));
		List<String> lst4 = pageObj.newCamHomePage().getFilterValueList(dataSet.get("filter2"));
		Assert.assertTrue(!lst4.contains(dataSet.get("option10")), "deactivate option is present");
		utils.logPass("Verified deactivate option is not present");
	}

	@DataProvider(name = "TestDataProvider")
	public Object[][] testDataProvider() {

		return new Object[][] { { "(", "!" }, { "-", "@" }, { ")", "#" }, };

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
