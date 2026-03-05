package com.punchh.server.NCHPTest;

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

import com.punchh.server.annotations.Owner;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class CampaignFilterTest {
	private static Logger logger = LogManager.getLogger(CampaignFilterTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String sTCName;
	private String baseUrl;
	String blankString = "";
	private String env, run = "ui";
	private static Map<String, String> dataSet;
	Properties prop;
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

	@Test(description = "SQ-T4865 Verify URL when Campaign Date filter is selected", priority = 0, groups = {
			"regression", "dailyrun" })
	@Owner(name = "Rakhi Rawat")
	public void T4865_verifyUrlWhenCampaigndateFilterIsSelected() throws InterruptedException, ParseException {
		// Login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */

		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickMoreFilterBtn();

		String startDate = CreateDateTime.getPastDateString();
		String endDate = CreateDateTime.getCurrentDateString();

		String startDateDay = startDate.substring(0, 2);
		String startDateMonthYear = CreateDateTime.getMonthAndYear(startDate);

		String endDateDay = startDate.substring(0, 2);
		String endDateMonthYear = CreateDateTime.getMonthAndYear(endDate);

		pageObj.newCamHomePage().selectCampaignDate("From", startDateDay, startDateMonthYear);
		pageObj.newCamHomePage().selectCampaignDate("To", endDateDay, endDateMonthYear);
		pageObj.newCamHomePage().clickSidePanelApplyBtn();
		// utils.longWaitInSeconds(1);
		String expStartDate = CreateDateTime.getPastDateStringNew();
		String expEndDate = CreateDateTime.getCurrentDateStringNew();

		// verify value from url
		String itemVal = pageObj.newCamHomePage().getValueFromUrlUsingRE(dataSet.get("startDateRE"));
		String itemVal1 = pageObj.newCamHomePage().getValueFromUrlUsingRE(dataSet.get("endDateRE"));

		itemVal = itemVal.replace("%2F", "/");
		itemVal1 = itemVal1.replace("%2F", "/");
		Assert.assertEquals(itemVal, expStartDate, "on selecting the start date filter url did not change");
		Assert.assertEquals(itemVal1, expEndDate, "on selecting the end date filter url did not change");

		utils.logPass("Verifed on selecting the date filter url change");

	}

	@Test(description = "MPC-T655 Verify URL when Date and Segment filter is selected", groups = { "regression",
			"unstable", "dailyrun" }, priority = 1)
	@Owner(name = "Rakhi Rawat")
	public void T655_verifyUrlWhenCampaigndateAndSegmentFilterIsSelected() throws InterruptedException, ParseException {
		// Login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */

		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickMoreFilterBtn();

		String startDate = CreateDateTime.getPastDateString();
		String endDate = CreateDateTime.getCurrentDateString();
		// pageObj.newCamHomePage().applyDateFilter(startDate, endDate);
		// pageObj.newCamHomePage().applyDateFilterForInvalidDate(startDate, endDate);
		String startDateDay = startDate.substring(0, 2);
		String startDateMonthYear = CreateDateTime.getMonthAndYear(startDate);

		String endDateDay = startDate.substring(0, 2);
		String endDateMonthYear = CreateDateTime.getMonthAndYear(endDate);

		pageObj.newCamHomePage().selectCampaignDate("From", startDateDay, startDateMonthYear);
		pageObj.newCamHomePage().selectCampaignDate("To", endDateDay, endDateMonthYear);

		pageObj.newCamHomePage().selectDrpDownValFromSidePanel("custom automation", "Segment");
		pageObj.newCamHomePage().clickSidePanelApplyBtn();
		String expStartDate = CreateDateTime.getPastDateStringNew();
		String expEndDate = CreateDateTime.getCurrentDateStringNew();

		// verify value from url
		String itemVal = pageObj.newCamHomePage().getValueFromUrlUsingRE(dataSet.get("startDateRE"));
		String itemVal1 = pageObj.newCamHomePage().getValueFromUrlUsingRE(dataSet.get("endDateRE"));
		String itemVal2 = pageObj.newCamHomePage().getValueFromUrlUsingRE(dataSet.get("segmentRE"));

		itemVal = itemVal.replace("%2F", "/");
		itemVal1 = itemVal1.replace("%2F", "/");
		itemVal2 = itemVal2.replace("+", " ");

		Assert.assertEquals(itemVal, expStartDate, "on selecting the start date filter url did not change");
		Assert.assertEquals(itemVal1, expEndDate, "on selecting the end date filter url did not change");
		Assert.assertEquals(itemVal2, dataSet.get("segmentName"), "on selecting the segment filter url did not change");
		utils.logPass("Verified on selecting the date and segment filter url change");

	}

	// Rakhi
	// verify selected date highlighted in calendar
	@Test(description = "SQ-T4801 Update date picker experience to follow bento guidelines", priority = 2, groups = {
			"regression", "unstable" })
	@Owner(name = "Rakhi Rawat")
	public void T4801_UpdateDatePickerExperienceToFollowBentoGuideline() throws InterruptedException, ParseException {
		// Login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */

		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickMoreFilterBtn();

		String startDate = CreateDateTime.getPastDateString();
		String startDateDay = startDate.substring(0, 2);
		String startDateMonthYear = CreateDateTime.getMonthAndYear(startDate);
		pageObj.newCamHomePage().selectCampaignDate("From", startDateDay, startDateMonthYear);

		String val = pageObj.newCamHomePage().getSelectedDateBGColor();
		Assert.assertEquals(val, dataSet.get("hexCode"), "selected date is highlighted on the calander");
		utils.logPass("selected date is highlighted on the calander");
	}

	// Rakhi
	// Date Range within 13 months
	@Test(description = "SQ-T4802,  Update date picker experience to follow bento guidelines", priority = 3, groups = {
			"regression", "dailyrun" })
	@Owner(name = "Rakhi Rawat")
	public void T4802_UpdateDatePickerExperienceToFollowBentoGuideline() throws InterruptedException, ParseException {
		// Login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */

		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickMoreFilterBtn();

		String startDate = CreateDateTime.getPastYearDateInDifferentFormat(2);
		String endDate = CreateDateTime.getCurrentDateString();

		pageObj.newCamHomePage().applyDateFilterForInvalidDate(startDate, endDate);
		pageObj.newCamHomePage().clickSidePanelApplyBtn();

		String text = pageObj.newCamHomePage().verifyErrorMsgOnDateNotWithinRange();
		Assert.assertEquals(text, "Enter a date within the 13 months range");
		utils.logPass("error message verified : " + text);
	}

	// Rakhi
	@Test(description = "SQ-T4662 Verify that the Reachability stats are appearing on the campaign overview page", priority = 4)
	@Owner(name = "Rakhi Rawat")
	public void T4662_ReachabilityStatsAppearingOnCampaignOverviewPage() throws InterruptedException {

		// Login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */

		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		String campaignName = "MassGifting" + CreateDateTime.getTimeDateString();
		String dateTime = CreateDateTime.getCurrentDate() + " 11:00 PM";

		// navigate to cockpit -> earning
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();

		// set campaign name and gift type
		pageObj.signupcampaignpage().setCampaignName(campaignName);
		pageObj.signupcampaignpage().turnOnAvailableAsATemplate();
		pageObj.signupcampaignpage().setGiftType(dataSet.get("giftType"));
		pageObj.signupcampaignpage().setFixedPoints(dataSet.get("fixedPoints"));
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().createWhomDetailsMassCampaign(dataSet.get("audiance"), dataSet.get("segment"),
				dataSet.get("pushNotification"), dataSet.get("emailSubject"), dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().createWhenDetailsMassCampaign("Once", dateTime);
		utils.logit("mass gifting campaign created " + campaignName);

		// run mass offer
		// navigate to cockpit -> earning
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().findMassCampaignNameandRun(campaignName);
		utils.logit("Mass gifting campaign Scheduled " + campaignName);

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		String camStatus = pageObj.campaignspage().checkMassCampStatusBeforeOpening(campaignName, "Processed");
		Assert.assertEquals(camStatus, "Processed", "Camapign status is not processed");
		pageObj.campaignspage().searchAndSelectCamapign(campaignName);

		pageObj.campaignspage().selectnewCPPOptions("Classic Page");
		String logParam = pageObj.campaignspage().verifyCampaignLogs();
		Assert.assertTrue(logParam.contains("Potential Reach"));
		utils.logPass("Campaign log also includes: Potential Reach log parameter");

	}

	// Anant
	@Test(description = "SQ-T4790 Campaign Action - Duplicate campaign", groups = { "unstable", "regression",
			"dailyrun" }, priority = 5)
	@Owner(name = "Rakhi Rawat")
	public void T4790_campaignActionDuplicateCampaign() throws InterruptedException, ParseException {

		String couponCampaignName = "Coupon Campaign" + CreateDateTime.getTimeDateString();
		// Login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */

		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
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

		// pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickNewCamHomePageBtn();
		pageObj.newCamHomePage().searchCampaign(couponCampaignName);
		pageObj.campaignspage().createDuplicateCampaign(couponCampaignName);

		utils.logPass("navigated into campaign what we are duplicating");

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickSwitchToClassicBtn();

		// delete coupon campaign
		pageObj.campaignspage().searchCampaign(couponCampaignName);
		pageObj.campaignspage().deactivateOrDeleteTheCoupon("delete");

	}

	// Rakhi
	@Test(description = "SQ-T4784 Campaign home page filters should respect tab selection", groups = { "regression",
			"unstable", "regression", "dailyrun" }, priority = 6)
	@Owner(name = "Rakhi Rawat")
	public void T4784_respectTabSelection() throws InterruptedException {
		// Login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */

		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Click Campaigns Link
		// pageObj.menupage().clickCampaignsMenu();
		// pageObj.menupage().clickCampaignsLink();
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickNewCamHomePageBtn();
		pageObj.newCamHomePage().clickMoreFilterBtn();

		List<String> filterList = pageObj.newCamHomePage().getSidePanelFilterList();
		logger.info(filterList);
		Assert.assertEquals(filterList, dataSet.get("expectedLst"), "filter present in the side panel are not equal");
		utils.logPass("Verified filter present in the side panel are equal");

		// Type Filter
		pageObj.newCamHomePage().sidePanelDrpDownExpand("Type");
		int size = pageObj.newCamHomePage().sidePanelDrpDownFilterOptionsCount("Type", 19);
		Assert.assertEquals(size, 19, "Filter options size does not matched for " + dataSet.get("filterName1"));
		utils.logPass("Filter options size for Type filter matched : " + size);
		pageObj.newCamHomePage().sidePanelDrpDownExpand("Type");

		// Status
		pageObj.newCamHomePage().sidePanelDrpDownExpand("Status");
		int size1 = pageObj.newCamHomePage().sidePanelDrpDownFilterOptionsCount("Status", 11);
		Assert.assertEquals(size1, 11, "Filter options size does not matched for " + "Status");
		utils.logPass("Filter options size for Status filter matched : " + size1);
		pageObj.newCamHomePage().sidePanelDrpDownExpand("Status");

		// Tag
		pageObj.newCamHomePage().sidePanelDrpDownExpand("Tag");
		int size2 = pageObj.newCamHomePage().sidePanelDrpDownFilterOptionsCount("Tag", 7);
		Assert.assertTrue(size2 > 0, "Filter options size does not matched for " + "Tag");
		utils.logPass("Filter options size for Tag Filter  matched : " + size2);
		pageObj.newCamHomePage().sidePanelDrpDownExpand("Tag");

		// Creator
		pageObj.newCamHomePage().sidePanelDrpDownExpand("Creator");
		int size3 = pageObj.newCamHomePage().sidePanelDrpDownFilterOptionsCount("Creator", 3);
		Assert.assertTrue(size3 > 0, "Filter options size does not matched for " + "Creator");
		utils.logPass("Filter options size for Creator Filter matched : " + size3);
		pageObj.newCamHomePage().sidePanelDrpDownExpand("Creator");

		// Segment
		pageObj.newCamHomePage().sidePanelDrpDownExpand("Segment");
		int size4 = pageObj.newCamHomePage().sidePanelDrpDownFilterOptionsCount("Segment", 5);
		Assert.assertTrue(size4 > 0, "Filter options size does not matched for " + "Segment");
		utils.logPass("Filter options size for Segment Filter matched : " + size4);
		pageObj.newCamHomePage().sidePanelDrpDownExpand("Segment");

		// Message Type
		pageObj.newCamHomePage().sidePanelDrpDownExpand("Message type");
		int size8 = pageObj.newCamHomePage().sidePanelDrpDownFilterOptionsCount("Message type", 3);
		Assert.assertEquals(size8, 3, "Filter options size does not matched for " + "Message type");
		utils.logPass("Filter options size for Message type Filter matched : " + size8);
		pageObj.newCamHomePage().sidePanelDrpDownExpand("Message type");

		// GiftType
		pageObj.newCamHomePage().sidePanelDrpDownExpand("Gift type");
		int size5 = pageObj.newCamHomePage().sidePanelDrpDownFilterOptionsCount("Gift type", 6);
		Assert.assertTrue(size5 > 0, "Filter options size does not matched for " + "Gift type");
		utils.logPass("Filter options size for GiftType Filter matched : " + size5);
		pageObj.newCamHomePage().sidePanelDrpDownExpand("Gift type");

		// Location
		pageObj.newCamHomePage().sidePanelDrpDownExpand("Location");
		int size6 = pageObj.newCamHomePage().sidePanelDrpDownFilterOptionsCount("Location", 1);
		Assert.assertTrue(size6 > 0, "Filter options size does not matched for " + "Location");
		utils.logPass("Filter options size for Location Filter matched : " + size6);
		pageObj.newCamHomePage().sidePanelDrpDownExpand("Location");

		// Franchisee
		pageObj.newCamHomePage().sidePanelDrpDownExpand("Franchisee");
		int size7 = pageObj.newCamHomePage().sidePanelDrpDownFilterOptionsCount("Franchisee", 1);
		Assert.assertTrue(size7 > 0, "Filter options size does not matched for " + "Franchisee");
		utils.logPass("Filter options size for Franchisee Filter matched : " + size7);
		pageObj.newCamHomePage().sidePanelDrpDownExpand("Franchisee");

	}

	// Rakhi
	@Test(description = "SQ-T4923 Verify success message in case of recurring mass campaign when it run only one-time", groups = {
			"regression", "dailyrun" }, priority = 7)
	@Owner(name = "Rakhi Rawat")
	public void T4923_verifySuccessMessageOfReccuranceMassCamp() throws InterruptedException {
		String recurringMassGiftingName = "Recurring Mass Gifting Campaign_T4923_" + CreateDateTime.getTimeDateString();
		// login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */

		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// Click Campaigns Link
		// pageObj.menupage().clickCampaignsMenu();
		// pageObj.menupage().clickCampaignsLink();
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		// Select offer dropdown value
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().createWhatDetailsMassCampaign(recurringMassGiftingName, dataSet.get("giftType"),
				dataSet.get("redemable"));
		pageObj.signupcampaignpage().createWhomDetailsMassCampaign(dataSet.get("audiance"), dataSet.get("segment"),
				dataSet.get("pushNotification"), dataSet.get("emailSubject"), dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().setFrequency("Daily");
		String startdateTime = CreateDateTime.getCurrentDate() + " 11:00 PM";
		String enddateTime = CreateDateTime.getFutureDate(1) + " 11:00 PM";
		pageObj.signupcampaignpage().setStartEndDateTime(startdateTime, enddateTime);

		// schedule mass gifting campaign
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().findMassCampaignNameandRun(recurringMassGiftingName);

		utils.logit("Mass gifting campaign Scheduled " + recurringMassGiftingName);

		// naviagte to CHP
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickNewCamHomePageBtn();
		utils.waitTillNewCamsTableAppear();
		String ActualCampStatus = pageObj.newCamHomePage().getAnyCampaignStatusWithPoolingNCHP(recurringMassGiftingName,
				"Processed");
		Assert.assertEquals(ActualCampStatus, "Processed", "Campaign is not in Processed state");
		logger.info("Campaign status is " + ActualCampStatus);
		pageObj.newCamHomePage().exportReport();
		String text = pageObj.newCamHomePage().getexportReportMsg();
		Assert.assertEquals(text, "Your campaign report is on its way");
		utils.logPass("Toast message verified as : " + text);
	}

	@Test(description = "SQ-T4818 Verify functionality of Tags filter on new campaign home page", groups = {
			"regression", "dailyrun" }, priority = 8)
	@Owner(name = "Rakhi Rawat")
	public void T4818_verifyTagsFilterFunctionality() throws InterruptedException {
		// Login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */

		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickMoreFilterBtn();
		pageObj.newCamHomePage().selectDrpDownValFromSidePanel(dataSet.get("tag1"), "Tag");
		pageObj.newCamHomePage().selectDrpDownValFromSidePanel(dataSet.get("tag2"), "Tag");
		pageObj.newCamHomePage().selectDrpDownValFromSidePanel(dataSet.get("tag3"), "Tag");
		pageObj.newCamHomePage().clickSidePanelApplyBtn();

		// verify selected tags
		String text = pageObj.newCamHomePage().getTagSelected("All");
		utils.logPass("Selected tags visible as : " + text);
		String text1 = pageObj.newCamHomePage().getTagSelected("Hover");
		utils.logPass("All selected tags are : " + text1);

	}

	// @Author = Hardik Bhardwaj
	@Test(description = "SQ-T5389 Add any/all option for Tags & Message Type", groups = { "unstable" }, priority = 10)
	@Owner(name = "Hardik Bhardwaj")
	public void T5389_verifyanyOrAllTagsAndMessageType() throws InterruptedException {
		String tagname1 = "Tag_1_" + CreateDateTime.getTimeDateString();
		String tagname2 = "Tag_2_" + CreateDateTime.getTimeDateString();

		// Login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */

		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// validate tags is created
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickNewCamHomePageBtn();
		pageObj.newCamHomePage().createNewCampaignTags(tagname1);
		pageObj.newCamHomePage().closeSidePanel();

		// validate tags is created
		// pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// pageObj.newCamHomePage().clickNewCamHomePageBtn();
		pageObj.newCamHomePage().createNewCampaignTags(tagname2);
		pageObj.newCamHomePage().closeSidePanel();

		pageObj.newCamHomePage().searchCampaignNCHP(dataSet.get("campaignName1"));
		pageObj.newCamHomePage().clickEllipsesOptions();
		pageObj.newCamHomePage().clickOptionsInEllipsesButton("Tag");
		pageObj.newCamHomePage().selectTagForCampaign(tagname1);
		List<String> message = pageObj.newCamHomePage().clickApplyBtn();
		Assert.assertEquals(message.get(0), dataSet.get("tagUpdateMsg"), "campaign did not update with tags");
		utils.logit("Successfully added tags " + tagname1 + " in the campaign " + dataSet.get("campaignName1"));

		pageObj.newCamHomePage().searchCampaignNCHP(dataSet.get("campaignName2"));
		pageObj.newCamHomePage().clickEllipsesOptions();
		pageObj.newCamHomePage().clickOptionsInEllipsesButton("Tag");
		pageObj.newCamHomePage().selectTagForCampaign(tagname2);
		List<String> message2 = pageObj.newCamHomePage().clickApplyBtn();
		Assert.assertEquals(message2.get(0), dataSet.get("tagUpdateMsg"), "campaign did not update with tags");
		utils.logit("Successfully added tags " + tagname2 + " in the campaign " + dataSet.get("campaignName2"));

		pageObj.newCamHomePage().searchCampaignNCHP(dataSet.get("campaignName5"));
		pageObj.newCamHomePage().clickEllipsesOptions();
		pageObj.newCamHomePage().clickOptionsInEllipsesButton("Tag");
		pageObj.newCamHomePage().selectTagForCampaign(tagname1);
		pageObj.newCamHomePage().clickApplyBtn();
		utils.logit("Successfully added tags " + tagname1 + " in the campaign " + dataSet.get("campaignName5"));
		pageObj.newCamHomePage().clickEllipsesOptions();
		pageObj.newCamHomePage().clickOptionsInEllipsesButton("Tag");
		pageObj.newCamHomePage().selectTagForCampaign(tagname2);
		pageObj.newCamHomePage().clickApplyBtn();
		utils.logit("Successfully added tags " + tagname2 + " in the campaign " + dataSet.get("campaignName5"));

		pageObj.newCamHomePage().searchCampaignNCHP(dataSet.get("campaignName6"));
		pageObj.newCamHomePage().clickEllipsesOptions();
		pageObj.newCamHomePage().clickOptionsInEllipsesButton("Tag");
		pageObj.newCamHomePage().selectTagForCampaign(tagname1);
		pageObj.newCamHomePage().clickApplyBtn();
		utils.logit("Successfully added tags " + tagname1 + " in the campaign " + dataSet.get("campaignName6"));
		pageObj.newCamHomePage().clickEllipsesOptions();
		pageObj.newCamHomePage().clickOptionsInEllipsesButton("Tag");
		pageObj.newCamHomePage().selectTagForCampaign(tagname2);
		pageObj.newCamHomePage().clickApplyBtn();
		utils.logit("Successfully added tags " + tagname2 + " in the campaign " + dataSet.get("campaignName6"));

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// pageObj.newCamHomePage().clickNewCamHomePageBtn();
		pageObj.newCamHomePage().clickMoreFilterBtn();
		pageObj.newCamHomePage().selectDrpDownValFromSidePanel(tagname1, "Tag");
		pageObj.newCamHomePage().selectDrpDownValFromSidePanel(tagname2, "Tag");
		pageObj.newCamHomePage().anyOrAndTagOrMessageFilter("Tag", "Any");
		pageObj.newCamHomePage().clickSidePanelApplyBtn();

		boolean anyTagFlag = false;
		List<String> campNameList = pageObj.newCamHomePage().campaignList();
		if (campNameList.contains(dataSet.get("campaignName1"))
				|| campNameList.contains(dataSet.get("campaignName2"))) {
			anyTagFlag = true;
		}

		Assert.assertTrue(anyTagFlag, "Campaign is not visible after applying Filter by Tag and Any option");
		utils.logPass("Verified that the page retrive data with any of the tag that we selected");

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// pageObj.newCamHomePage().clickNewCamHomePageBtn();
		pageObj.newCamHomePage().clickMoreFilterBtn();
		pageObj.newCamHomePage().selectDrpDownValFromSidePanel(tagname1, "Tag");
//		pageObj.newCamHomePage().sidePanelDrpDownExpand("Tag");
		pageObj.newCamHomePage().selectDrpDownValFromSidePanel(tagname2, "Tag");
//		pageObj.newCamHomePage().sidePanelDrpDownExpand("Tag");
		pageObj.newCamHomePage().anyOrAndTagOrMessageFilter("Tag", "All");
		pageObj.newCamHomePage().clickSidePanelApplyBtn();

		boolean allTagFlag = false;
		List<String> campNameList2 = pageObj.newCamHomePage().campaignList();
		if (campNameList2.contains(dataSet.get("campaignName5"))
				&& campNameList2.contains(dataSet.get("campaignName6"))) {
			allTagFlag = true;
		}

		Assert.assertTrue(allTagFlag, "Campaign is not visible after applying Filter by Tag and All option");
		utils.logPass("Verified that the page retrieve data with all the tags that we selected");

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// pageObj.newCamHomePage().clickNewCamHomePageBtn();
		pageObj.newCamHomePage().clickMoreFilterBtn();
		pageObj.newCamHomePage().selectDrpDownValFromSidePanel("Rich", "Message type");
//		pageObj.newCamHomePage().sidePanelDrpDownExpand("Message type");
		pageObj.newCamHomePage().selectDrpDownValFromSidePanel("SMS", "Message type");
//		pageObj.newCamHomePage().sidePanelDrpDownExpand("Message type");
		pageObj.newCamHomePage().anyOrAndTagOrMessageFilter("Message type", "Any");
		pageObj.newCamHomePage().clickSidePanelApplyBtn();

		boolean anyMessageFlag = false;
		List<String> campNameList3 = pageObj.newCamHomePage().campaignList();
		if (campNameList3.contains(dataSet.get("campaignName3"))
				|| campNameList3.contains(dataSet.get("campaignName4"))) {
			anyMessageFlag = true;
		}

		Assert.assertTrue(anyMessageFlag, "Campaign is not visible after applying Filter by Message and Any option");
		utils.logPass("Verified that the page retrive data with any of the Message that we selected");

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// pageObj.newCamHomePage().clickNewCamHomePageBtn();
		pageObj.newCamHomePage().clickMoreFilterBtn();
		pageObj.newCamHomePage().selectDrpDownValFromSidePanel("Rich", "Message type");
//		pageObj.newCamHomePage().sidePanelDrpDownExpand("Message type");
		pageObj.newCamHomePage().selectDrpDownValFromSidePanel("SMS", "Message type");
//		pageObj.newCamHomePage().sidePanelDrpDownExpand("Message type");
		pageObj.newCamHomePage().anyOrAndTagOrMessageFilter("Message type", "All");
		pageObj.newCamHomePage().clickSidePanelApplyBtn();

		boolean allMessageFlag = false;
		List<String> campNameList4 = pageObj.newCamHomePage().campaignList();
		if (campNameList4.contains(dataSet.get("campaignName3"))
				&& campNameList4.contains(dataSet.get("campaignName7"))) {
			allMessageFlag = true;
		}

		Assert.assertTrue(allMessageFlag, "Campaign is not visible after applying Filter by Message and Any option");
		utils.logPass("Verified that the page retrive data with any of Message tag that we selected");

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickManageTagsBtn();
		pageObj.newCamHomePage().deleteCampaignTag(tagname1);
		pageObj.newCamHomePage().closeOptionsDailog();

		pageObj.newCamHomePage().clickManageTagsBtn();
		pageObj.newCamHomePage().deleteCampaignTag(tagname2);
		pageObj.newCamHomePage().closeOptionsDailog();
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
