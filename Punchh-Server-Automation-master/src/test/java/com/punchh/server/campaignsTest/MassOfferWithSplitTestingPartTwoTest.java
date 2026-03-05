package com.punchh.server.campaignsTest;

import java.lang.reflect.Method;
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

import io.restassured.response.Response;

@Test
@Listeners(TestListeners.class)
public class MassOfferWithSplitTestingPartTwoTest {
	private static Logger logger = LogManager.getLogger(MassOfferWithSplitTestingPartTwoTest.class);
	public WebDriver driver;
	private Properties prop;
	private PageObj pageObj;
	private String userEmail;
	private String sTCName;
	private String env;
	private String baseUrl;
	Utilities utils;
	private static Map<String, String> dataSet;
	String run = "ui";
	String redeemableName = "AutomationRedeemable";

	@BeforeClass(alwaysRun = true)
	public void openBrowser() {
		prop = Utilities.loadPropertiesFile("config.properties");
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
		// Instance move to all business page
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		utils = new Utilities();
	}

	// Rakhi
	@Test(description = "SQ-T5957 Verify duplicate option is not available for split mass offer campaign", groups = {
			"regression" }, priority = 0)
	@Owner(name = "Rakhi Rawat")
	public void T5957_VerifyDuplicateOptionForSplitTestCampaign() throws Exception {

		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// verify duplicate options is visible or not for split test campaign in newCHP
		// options for Draft, Pending Approval and Stopped status
		for (int i = 1; i <= 3; i++) {
			pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
			// select scheduled mass split campaign from
			pageObj.newCamHomePage().clickMoreFilterBtn();
			pageObj.newCamHomePage().selectSplitTestOnly();
			pageObj.newCamHomePage().selectDrpDownValFromSidePanel(dataSet.get("status" + i), "Status");
			pageObj.newCamHomePage().clickSidePanelApplyBtn();

			List<String> options = pageObj.campaignsplitpage().verifySplitTestNewCHPoptions();
			Assert.assertFalse(options.contains("Duplicate"),
					"Duplicate option is available in ellipsis for split test campaign " + dataSet.get("status" + i)
							+ " status on new CHP");

			// verify Split test duplicate option on classic CPP
			pageObj.newCamHomePage().clickOnFirstCampaign();
			List<String> options1 = pageObj.campaignsplitpage().verifySplitTestEllipsisOptionsClassicCPP();
			Assert.assertFalse(options1.contains("Duplicate"),
					"Duplicate option is available in ellipsis for split test campaign " + dataSet.get("status" + i)
							+ " status on classic CPP");

			logger.info("Duplicate option is not available in ellipsis for split test campaign "
					+ dataSet.get("status" + i) + " status");
			TestListeners.extentTest.get().pass("Duplicate option is not available in ellipsis for split test campaign "
					+ dataSet.get("status" + i) + " status");
		}
		// verify duplicate options is visible or not for split test campaign in newCHP
		// for Scheduled status
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().searchCampaign(dataSet.get("scheduledSplitTestCampaignName"));
		List<String> options = pageObj.campaignsplitpage().verifySplitTestNewCHPoptions();
		Assert.assertFalse(options.contains("Duplicate"),
				"Duplicate option is available in ellipsis for split test campaign for Scheduled status on new CHP");

		// verify Split test duplicate option on classic CPP
		pageObj.newCamHomePage().clickOnFirstCampaign();
		List<String> options1 = pageObj.campaignsplitpage().verifySplitTestEllipsisOptionsClassicCPP();
		Assert.assertFalse(options1.contains("Duplicate"),
				"Duplicate option is available in ellipsis for split test campaign for Scheduled status on classic CPP");

		logger.info("Duplicate option is not available in ellipsis for split test campaign for Scheduled status");
		TestListeners.extentTest.get()
				.pass("Duplicate option is not available in ellipsis for split test campaign for Scheduled status");
	}

	// Shubham Kumar Gupta
	@Test(description = "SQ-T6146 Verify the case when user select 'X' on top left of Split modal pop "
			+ "up after launching the modal" + "SQ-T6147 Verify the case when user select 'Cancel' button on "
			+ "Split test set up screen" + "SQ-6148 Verify the case when user select 'Yes, cancel' button on"
			+ "pop up which appears when we choose 'Cancel' button on split test modal screen "
			+ "in mass offer campaign Whom page." + "SQ-T6149 Verify the case when user select "
			+ "'No, continue editing' button on pop up launched after selecting 'Cancel' on"
			+ " split test modal screen in mass offer campaign.", groups = { "regression" }, priority = 3)
	@Owner(name = "Shubham Gupta")
	public void T6146_VerifyXCancelYesCanceAndNoEditingButtonFeature() throws Exception {

		String massCampaignName = "AutomationMassOfferT6146_" + CreateDateTime.getTimeDateString();
		boolean splitDialogClosed = false;
		boolean noContinueEditingButtonVisible = true;

		userEmail = dataSet.get("email");
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Campaigns");

		pageObj.dashboardpage().checkBoxFlagOnOffAndUpdate("business_enable_split_testing", "check", true);

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickSwitchToClassicBtn();
		// Select offer dropdown value
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();

		// set campaign name and gift type
		pageObj.signupcampaignpage().setCampaignName(massCampaignName);
		pageObj.signupcampaignpage().setGiftType(dataSet.get("giftType"));
		pageObj.signupcampaignpage().setRedeemable(dataSet.get("redeemableName"));
		pageObj.signupcampaignpage().clickNextBtn();

		// Verify split panel before selecting segment
		pageObj.campaignsplitpage().splitPanelVerifyonMassOfferWhomPage();

		// select segment
		pageObj.signupcampaignpage().setAudienceType(dataSet.get("audiance"));
		pageObj.signupcampaignpage().setSegmentType(dataSet.get("segment"));

		// Verify split panel after selecting segment
		pageObj.campaignsplitpage().splitPanelVerifyonMassOfferWhomPage();

		// Click on split button
		pageObj.campaignsplitpage().clickOnSplitButton();

		// enter segment percentage
		pageObj.campaignsplitpage().addPercentInVarAandVarBandControlgroup(dataSet.get("varApercent"),
				dataSet.get("varBpercent"), dataSet.get("controlgrouppercent"));

		// close split dialog on audieance page by clicking on 'X' button
		pageObj.campaignsplitpage().closeSplitDialog("xButton");

		// Assert to validate the split dialog is closed
		splitDialogClosed = pageObj.campaignsplitpage().verifySplitTestElementsVisibility("splitButton", null, null);
		Assert.assertTrue(splitDialogClosed, "Split dialog did not closed after clicking on 'X' button");

		// Click on split button
		pageObj.campaignsplitpage().clickOnSplitButton();
		// enter segment percentage
		pageObj.campaignsplitpage().addPercentInVarAandVarBandControlgroup(dataSet.get("varApercent"),
				dataSet.get("varBpercent"), dataSet.get("controlgrouppercent"));
		// Click on save button
		pageObj.campaignsplitpage().clickOnNextButton();

		// close split dialog on gift and message page by clicking on 'X' button
		pageObj.campaignsplitpage().closeSplitDialog("xButton");

		// Assert to validate the split dialog is closed
		splitDialogClosed = pageObj.campaignsplitpage().verifySplitTestElementsVisibility("splitButton", null, null);
		Assert.assertTrue(splitDialogClosed, "Split dialog did not closed after clicking on 'X' button");

		// Click on split button
		pageObj.campaignsplitpage().clickOnSplitButton();
		// enter segment percentage
		pageObj.campaignsplitpage().addPercentInVarAandVarBandControlgroup(dataSet.get("varApercent"),
				dataSet.get("varBpercent"), dataSet.get("controlgrouppercent"));
		// Click on save button
		pageObj.campaignsplitpage().clickOnNextButton();

		// Add email, PN, SMS and RM in both the variant
		pageObj.campaignsplitpage().selectAndAddEmailOfVarA(dataSet.get("varAEmail") + " " + massCampaignName);
		pageObj.campaignsplitpage().clickOnSaveButton();
		pageObj.campaignsplitpage().selectAndAddPNOfVarA(dataSet.get("varAPushNoti") + " " + massCampaignName);
		pageObj.campaignsplitpage().clickOnSaveButton();
		pageObj.campaignsplitpage().selectAndAddSMSOfVarA(dataSet.get("varASMS") + " " + massCampaignName);
		pageObj.campaignsplitpage().clickOnSaveButton();
		pageObj.campaignsplitpage().selectAndAddRMOfVarA(dataSet.get("varARichMsg") + " " + massCampaignName);
		pageObj.campaignsplitpage().clickOnSaveButton();
		pageObj.campaignsplitpage().selectAndAddEmailOfVarB(dataSet.get("varBEmail") + " " + massCampaignName);
		pageObj.campaignsplitpage().clickOnSaveButton();
		pageObj.campaignsplitpage().selectAndAddPNOfVarB(dataSet.get("varBPushNoti") + " " + massCampaignName);
		pageObj.campaignsplitpage().clickOnSaveButton();
		pageObj.campaignsplitpage().selectAndAddSMSOfVarB(dataSet.get("varBSMS") + " " + massCampaignName);
		pageObj.campaignsplitpage().clickOnSaveButton();
		pageObj.campaignsplitpage().selectAndAddRMOfVarB(dataSet.get("varBRichMsg") + " " + massCampaignName);
		pageObj.campaignsplitpage().clickOnSaveButton();

		pageObj.campaignsplitpage().clickOnNextButton();

		// close split dialog on review page by clicking on 'X' button
		pageObj.campaignsplitpage().closeSplitDialog("xButton");

		// Assert to validate the split dialog is closed
		splitDialogClosed = pageObj.campaignsplitpage().verifySplitTestElementsVisibility("splitButton", null, null);
		Assert.assertTrue(splitDialogClosed, "Split dialog did not closed after clicking on 'X' button");

		// Verify the split dialog is closed after clicking on 'Cancel' button
		// Click on split button
		pageObj.campaignsplitpage().clickOnSplitButton();

		// enter segment percentage
		pageObj.campaignsplitpage().addPercentInVarAandVarBandControlgroup(dataSet.get("varApercent"),
				dataSet.get("varBpercent"), dataSet.get("controlgrouppercent"));

		// close split dialog on audieance page by clicking on cancel button
		pageObj.campaignsplitpage().closeSplitDialog("cancelButton");

		// Assert to validate the split dialog is closed
		splitDialogClosed = pageObj.campaignsplitpage().verifySplitTestElementsVisibility("splitButton", null, null);
		Assert.assertTrue(splitDialogClosed, "Split dialog did not closed after clicking on Cancel button");

		// Click on split button
		pageObj.campaignsplitpage().clickOnSplitButton();
		// enter segment percentage
		pageObj.campaignsplitpage().addPercentInVarAandVarBandControlgroup(dataSet.get("varApercent"),
				dataSet.get("varBpercent"), dataSet.get("controlgrouppercent"));
		// Click on save button
		pageObj.campaignsplitpage().clickOnNextButton();

		// close split dialog on gift and message page by clicking on cancel button
		pageObj.campaignsplitpage().closeSplitDialog("cancelButton");

		// Assert to validate the split dialog is closed
		splitDialogClosed = pageObj.campaignsplitpage().verifySplitTestElementsVisibility("splitButton", null, null);
		Assert.assertTrue(splitDialogClosed, "Split dialog did not closed after clicking on Cancel button");

		// Click on split button
		pageObj.campaignsplitpage().clickOnSplitButton();
		// enter segment percentage
		pageObj.campaignsplitpage().addPercentInVarAandVarBandControlgroup(dataSet.get("varApercent"),
				dataSet.get("varBpercent"), dataSet.get("controlgrouppercent"));
		// Click on save button
		pageObj.campaignsplitpage().clickOnNextButton();

		// Add email, PN, SMS and RM in both the variant
		pageObj.campaignsplitpage().selectAndAddEmailOfVarA(dataSet.get("varAEmail") + " " + massCampaignName);
		pageObj.campaignsplitpage().clickOnSaveButton();
		pageObj.campaignsplitpage().selectAndAddPNOfVarA(dataSet.get("varAPushNoti") + " " + massCampaignName);
		pageObj.campaignsplitpage().clickOnSaveButton();
		pageObj.campaignsplitpage().selectAndAddSMSOfVarA(dataSet.get("varASMS") + " " + massCampaignName);
		pageObj.campaignsplitpage().clickOnSaveButton();
		pageObj.campaignsplitpage().selectAndAddRMOfVarA(dataSet.get("varARichMsg") + " " + massCampaignName);
		pageObj.campaignsplitpage().clickOnSaveButton();
		pageObj.campaignsplitpage().selectAndAddEmailOfVarB(dataSet.get("varBEmail") + " " + massCampaignName);
		pageObj.campaignsplitpage().clickOnSaveButton();
		pageObj.campaignsplitpage().selectAndAddPNOfVarB(dataSet.get("varBPushNoti") + " " + massCampaignName);
		pageObj.campaignsplitpage().clickOnSaveButton();
		pageObj.campaignsplitpage().selectAndAddSMSOfVarB(dataSet.get("varBSMS") + " " + massCampaignName);
		pageObj.campaignsplitpage().clickOnSaveButton();
		pageObj.campaignsplitpage().selectAndAddRMOfVarB(dataSet.get("varBRichMsg") + " " + massCampaignName);
		pageObj.campaignsplitpage().clickOnSaveButton();

		pageObj.campaignsplitpage().clickOnNextButton();

		// close split dialog on review page by clicking on cancel button
		pageObj.campaignsplitpage().closeSplitDialog("cancelButton");

		// Assert to validate the split dialog is closed
		splitDialogClosed = pageObj.campaignsplitpage().verifySplitTestElementsVisibility("splitButton", null, null);
		Assert.assertTrue(splitDialogClosed, "Split dialog did not closed after clicking on Cancel button");

		// Verify the feature of No, continue editing button
		// Click on split button
		pageObj.campaignsplitpage().clickOnSplitButton();

		// enter segment percentage
		pageObj.campaignsplitpage().addPercentInVarAandVarBandControlgroup(dataSet.get("varApercent"),
				dataSet.get("varBpercent"), dataSet.get("controlgrouppercent"));

		// click on X button on audience page
		pageObj.campaignsplitpage().clickXButton();

		// click on No, continue editing button
		pageObj.campaignsplitpage().clickNoContinueEditingButton();
		noContinueEditingButtonVisible = pageObj.campaignsplitpage().isNoContinueButtonVisible();
		Assert.assertFalse(noContinueEditingButtonVisible,
				"No, continue editing button is still visible after clicking on No, continue editing button through X button");

		// click on cancel button on audience page
		pageObj.campaignsplitpage().splitCancelButton();

		// click on No, continue editing button
		pageObj.campaignsplitpage().clickNoContinueEditingButton();
		noContinueEditingButtonVisible = pageObj.campaignsplitpage().isNoContinueButtonVisible();
		Assert.assertFalse(noContinueEditingButtonVisible,
				"No, continue editing button is still visible after clicking on No, continue editing button through Cancel button");

		pageObj.campaignsplitpage().clickOnNextButton();

		// click on X button on gift and message page
		pageObj.campaignsplitpage().clickXButton();

		// click on No, continue editing button
		pageObj.campaignsplitpage().clickNoContinueEditingButton();
		noContinueEditingButtonVisible = pageObj.campaignsplitpage().isNoContinueButtonVisible();
		Assert.assertFalse(noContinueEditingButtonVisible,
				"No, continue editing button is still visible after clicking on No, continue editing button through X button");

		// click on cancel button on gift and message page
		pageObj.campaignsplitpage().splitCancelButton();

		// click on No, continue editing button
		pageObj.campaignsplitpage().clickNoContinueEditingButton();
		noContinueEditingButtonVisible = pageObj.campaignsplitpage().isNoContinueButtonVisible();
		Assert.assertFalse(noContinueEditingButtonVisible,
				"No, continue editing button is still visible after clicking on No, continue editing button through Cancel button");

	}

	// Rakhi
	@Test(description = "SQ-T5955 Verify split testing campaigns can be searched via filter on campaigns homepage", groups = {
			"regression" }, priority = 4)
	@Owner(name = "Rakhi Rawat")
	public void T5955_VerifySplitCampaignSearchViaFilterOnNewCHP() throws Exception {

		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickNewCamHomePageBtn();
		// verify split campaign can be searched via filter Split Test Only
		pageObj.newCamHomePage().clickMoreFilterBtn();
		pageObj.newCamHomePage().selectSplitTestOnly();
		pageObj.newCamHomePage().clickSidePanelApplyBtn();
		List<String> massList = pageObj.campaignsplitpage().getCampaignTypeOnNewCHP();
		boolean status = massList.stream().allMatch("Mass (Split)"::equals);
		Assert.assertTrue(status, "Other campaign type found in mass tab NCHP");
		logger.info("Verified Split test campaigns can be searched via Split Test Only filter on campaigns homepage");
		TestListeners.extentTest.get()
				.pass("Verified Split test campaigns can be searched via Split Test Only filter on campaigns homepage");

		// verify split campaign can be searched via Type,Status,Segment,Message type
		// filters
		pageObj.newCamHomePage().clickMoreFilterBtn();
		pageObj.newCamHomePage().selectDrpDownValFromSidePanel("Mass (One-time)", "Type");
		pageObj.newCamHomePage().selectDrpDownValFromSidePanel(dataSet.get("status1"), "Status");
		pageObj.newCamHomePage().selectDrpDownValFromSidePanel(dataSet.get("segment"), "Segment");
		pageObj.newCamHomePage().selectDrpDownValFromSidePanel(dataSet.get("msgType"), "Message type");
		pageObj.newCamHomePage().clickSidePanelApplyBtn();
		List<String> massList1 = pageObj.campaignsplitpage().getCampaignTypeOnNewCHP();
		boolean status1 = massList1.stream().allMatch("Mass (Split)"::equals);
		Assert.assertTrue(status1, "Other campaign type found after applying filters");
		logger.info(
				"Verified Split test campaigns can be searched via Type,Status,Segment,Message type filters on campaigns homepage");
		TestListeners.extentTest.get().pass(
				"Verified Split test campaigns can be searched via Type,Status,Segment,Message type filters on campaigns homepage");

		// verify split campaign can be searched via Redeemable, Tags, Creator filters
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickMoreFilterBtn();
		pageObj.newCamHomePage().selectSplitTestOnly();
		pageObj.newCamHomePage().selectDrpDownValFromSidePanel(dataSet.get("tag"), "Tag");
		pageObj.newCamHomePage().selectDrpDownValFromSidePanel("Redeemable", "Gift type");
		pageObj.newCamHomePage().selectDrpDownValFromSidePanel(dataSet.get("creator"), "Creator");
		pageObj.newCamHomePage().clickSidePanelApplyBtn();
		List<String> massList2 = pageObj.campaignsplitpage().getCampaignTypeOnNewCHP();
		boolean status2 = massList2.stream().allMatch("Mass (Split)"::equals);
		Assert.assertTrue(status2, "Other campaign type found after applying filters");
		logger.info(
				"Verified Split test campaigns can be searched via Redeemable, Tags, Creator filters on campaigns homepage");
		TestListeners.extentTest.get().pass(
				"Verified Split test campaigns can be searched via Redeemable, Tags, Creator filters on campaigns homepage");

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// verify split campaign can be searched via Date filters
		pageObj.newCamHomePage().clickMoreFilterBtn();
		pageObj.newCamHomePage().selectSplitTestOnly();
		pageObj.newCamHomePage().selectDrpDownValFromSidePanel(dataSet.get("status1"), "Status");

		String startDate = dataSet.get("startDate");
		String endDate = dataSet.get("endDate");

		pageObj.newCamHomePage().applyDateFilterForInvalidDate(startDate, endDate);
		pageObj.newCamHomePage().clickSidePanelApplyBtn();
		List<String> massList3 = pageObj.campaignsplitpage().getCampaignTypeOnNewCHP();
		boolean status3 = massList3.stream().allMatch("Mass (Split)"::equals);
		Assert.assertTrue(status3, "Other campaign type found after applying filters");
		logger.info("Verified Split test campaigns can be searched via date filter filters on campaigns homepage");
		TestListeners.extentTest.get()
				.pass("Verified Split test campaigns can be searched via date filter filters on campaigns homepage");

	}

	// Shubham Kumar Gupta
	@Test(description = "SQ-T6297 Verify the case when user enter or save data for Primary CTA button on Rich message modal and uncheck the checkbox for both Variant A and B"
	                   + "SQ-T6298 Verify the case when user enter or save data for Secondary CTA button on Rich message modal and uncheck the checkbox for both Variant A and B." 
			           + "SQ-T6299 Verify the case when user enter or save data for Background Content on Rich message modal and uncheck the checkbox for both Variant A and B."
			           + "SQ-T6300 Verify the case when user enter or save data for Meta field on Rich message modal and uncheck the checkbox for both Variant A and B.", groups = { "regression" }, priority = 5)
	@Owner(name = "Shubham Gupta")
	public void T6297_verifyPrimarySecondaryBackgroundMetaInRichMsg() throws Exception {
		String massCampaignName = "AutomationMassOffer_" + CreateDateTime.getTimeDateString();
		boolean isPrimaryCTAChecked = false;
		boolean isSecondaryCTAChecked = false;
		boolean isBackgroundContentChecked = false;
		boolean isMetaChecked = false;
		String primaryCTAText = "";
		String secondaryCTAText = "";
		String backgroundContentText = "";
		String metaText = "";
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Campaigns");

		pageObj.dashboardpage().checkBoxFlagOnOffAndUpdate("business_enable_split_testing", "check", true);

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickSwitchToClassicBtn();
		// Select offer dropdown value
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();

		// set campaign name and gift type
		pageObj.signupcampaignpage().setCampaignName(massCampaignName);
		pageObj.signupcampaignpage().setGiftType(dataSet.get("giftType"));
		pageObj.signupcampaignpage().setRedeemable(dataSet.get("redeemableName"));
		pageObj.signupcampaignpage().clickNextBtn();

		// Verify split panel before selecting segment
		pageObj.campaignsplitpage().splitPanelVerifyonMassOfferWhomPage();

		// select segment
		pageObj.signupcampaignpage().setAudienceType(dataSet.get("audiance"));
		pageObj.signupcampaignpage().setSegmentType(dataSet.get("segment"));

		// Verify split panel after selecting segment
		pageObj.campaignsplitpage().splitPanelVerifyonMassOfferWhomPage();

		// Click on split button
		pageObj.campaignsplitpage().clickOnSplitButton();

		// enter segment percentage
		pageObj.campaignsplitpage().addPercentInVarAandVarBandControlgroup(dataSet.get("varApercent"),
				dataSet.get("varBpercent"), dataSet.get("controlgrouppercent"));
		// Click on save button
		pageObj.campaignsplitpage().clickOnNextButton();

		pageObj.campaignsplitpage().selectAndAddRMOfVarA(dataSet.get("varARichMsg") + " " + massCampaignName);

		isPrimaryCTAChecked = pageObj.campaignsplitpage().isCheckboxChecked("Primary CTA");
		Assert.assertFalse(isPrimaryCTAChecked, "Primary CTA checkbox is checked by default for Rich Message");

		isSecondaryCTAChecked = pageObj.campaignsplitpage().isCheckboxChecked("Secondary CTA");
		Assert.assertFalse(isSecondaryCTAChecked, "Secondary CTA checkbox is checked by default for Rich Message");

		isBackgroundContentChecked = pageObj.campaignsplitpage().isCheckboxChecked("Background content");
		Assert.assertFalse(isBackgroundContentChecked,
				"Background Content checkbox is checked by default for Rich Message");

		isMetaChecked = pageObj.campaignsplitpage().isCheckboxChecked("Meta");
		Assert.assertFalse(isMetaChecked, "Meta checkbox is checked by default for Rich Message");

		boolean isAllowGuestToDismissRichMsgChecked = pageObj.campaignsplitpage()
				.isCheckboxChecked("Allows guests to dismiss a Rich Message");
		Assert.assertFalse(isAllowGuestToDismissRichMsgChecked,
				"Allow guests to dismiss rich message checkbox is checked by default for Rich Message");

		pageObj.campaignsplitpage().enablePrimaryCTAAndEnterData("Primary CTA", "External Web Link");
		pageObj.campaignsplitpage().enableSecondaryCTAAndEnterData("Secondary CTA", "External Web Link");
		pageObj.campaignsplitpage().enableBackgroundContentAndEnterData("Background content", "Image");
		pageObj.campaignsplitpage().enableMetaAndEnterData("Meta");

		pageObj.campaignsplitpage().setRichMessageCTAFlag("Primary CTA", false);
		pageObj.campaignsplitpage().setRichMessageCTAFlag("Primary CTA", true);
		primaryCTAText = pageObj.campaignsplitpage().getTextFieldValueFor("Primary CTA");
		Assert.assertFalse(primaryCTAText.contains("Primary CTA"),
				"Primary CTA text field is not updated after enabling Primary CTA checkbox");

		pageObj.campaignsplitpage().setRichMessageCTAFlag("Secondary CTA", false);
		pageObj.campaignsplitpage().setRichMessageCTAFlag("Secondary CTA", true);
		secondaryCTAText = pageObj.campaignsplitpage().getTextFieldValueFor("Secondary CTA");
		Assert.assertFalse(secondaryCTAText.contains("Secondary CTA"),
				"Secondary CTA text field is not updated after enabling Primary CTA checkbox");

		pageObj.campaignsplitpage().setRichMessageCTAFlag("Background content", false);
		pageObj.campaignsplitpage().setRichMessageCTAFlag("Background content", true);
		backgroundContentText = pageObj.campaignsplitpage().getTextFieldValueFor("Background content");
		Assert.assertFalse(backgroundContentText.contains("Background content"),
				"Background content text field is not updated after enabling Background content checkbox");

		pageObj.campaignsplitpage().setRichMessageCTAFlag("Meta", false);
		pageObj.campaignsplitpage().setRichMessageCTAFlag("Meta", true);
		metaText = pageObj.campaignsplitpage().getTextFieldValueFor("Meta");
		Assert.assertFalse(metaText.contains("Meta"), "Meta text field is not updated after enabling Meta checkbox");
		logger.info("Verified Primary CTA, Secondary CTA, Background content and Meta text fields are updated "
				+ "successfully after enabling respective checkboxes");
		TestListeners.extentTest.get()
				.pass("Verified Primary CTA, Secondary CTA, Background content and Meta text fields are updated "
						+ "successfully after enabling respective checkboxes");

		pageObj.campaignsplitpage().clickOnSaveButton();

		pageObj.campaignsplitpage().selectAndAddRMOfVarB(dataSet.get("varARichMsg") + " " + massCampaignName);

		isPrimaryCTAChecked = pageObj.campaignsplitpage().isCheckboxChecked("Primary CTA");
		Assert.assertFalse(isPrimaryCTAChecked, "Primary CTA checkbox is checked by default for Rich Message");

		isSecondaryCTAChecked = pageObj.campaignsplitpage().isCheckboxChecked("Secondary CTA");
		Assert.assertFalse(isSecondaryCTAChecked, "Secondary CTA checkbox is checked by default for Rich Message");

		isBackgroundContentChecked = pageObj.campaignsplitpage().isCheckboxChecked("Background content");
		Assert.assertFalse(isBackgroundContentChecked,
				"Background Content checkbox is checked by default for Rich Message");

		isMetaChecked = pageObj.campaignsplitpage().isCheckboxChecked("Meta");
		Assert.assertFalse(isMetaChecked, "Meta checkbox is checked by default for Rich Message");

		isAllowGuestToDismissRichMsgChecked = pageObj.campaignsplitpage()
				.isCheckboxChecked("Allows guests to dismiss a Rich Message");
		Assert.assertFalse(isAllowGuestToDismissRichMsgChecked,
				"Allow guests to dismiss rich message checkbox is checked by default for Rich Message");

		pageObj.campaignsplitpage().enablePrimaryCTAAndEnterData("Primary CTA", "External Web Link");
		pageObj.campaignsplitpage().enableSecondaryCTAAndEnterData("Secondary CTA", "External Web Link");
		pageObj.campaignsplitpage().enableBackgroundContentAndEnterData("Background content", "Image");
		pageObj.campaignsplitpage().enableMetaAndEnterData("Meta");

		pageObj.campaignsplitpage().setRichMessageCTAFlag("Primary CTA", false);
		pageObj.campaignsplitpage().setRichMessageCTAFlag("Primary CTA", true);
		primaryCTAText = pageObj.campaignsplitpage().getTextFieldValueFor("Primary CTA");
		Assert.assertFalse(primaryCTAText.contains("Primary CTA"),
				"Primary CTA text field is not updated after enabling Primary CTA checkbox");

		pageObj.campaignsplitpage().setRichMessageCTAFlag("Secondary CTA", false);
		pageObj.campaignsplitpage().setRichMessageCTAFlag("Secondary CTA", true);
		secondaryCTAText = pageObj.campaignsplitpage().getTextFieldValueFor("Secondary CTA");
		Assert.assertFalse(secondaryCTAText.contains("Secondary CTA"),
				"Secondary CTA text field is not updated after enabling Primary CTA checkbox");

		pageObj.campaignsplitpage().setRichMessageCTAFlag("Background content", false);
		pageObj.campaignsplitpage().setRichMessageCTAFlag("Background content", true);
		backgroundContentText = pageObj.campaignsplitpage().getTextFieldValueFor("Background content");
		Assert.assertFalse(backgroundContentText.contains("Background content"),
				"Background content text field is not updated after enabling Background content checkbox");

		pageObj.campaignsplitpage().setRichMessageCTAFlag("Meta", false);
		pageObj.campaignsplitpage().setRichMessageCTAFlag("Meta", true);
		metaText = pageObj.campaignsplitpage().getTextFieldValueFor("Meta");
		Assert.assertFalse(metaText.contains("Meta"), "Meta text field is not updated after enabling Meta checkbox");
		logger.info("Verified Primary CTA, Secondary CTA, Background content and Meta text fields are updated "
				+ "successfully after enabling respective checkboxes");
		TestListeners.extentTest.get()
				.pass("Verified Primary CTA, Secondary CTA, Background content and Meta text fields are updated "
						+ "successfully after enabling respective checkboxes");
	}

	// Jeevraj
	@Test(description = "SQ-T6294 Verify if the user selects ANYTHING other than redeemable as a gift type on What page of mass offer campaign then the split testing call out box on the “Whom” page is not visible"
			+ "SQ-T6295 Verify Users can click the “Previous” button on When page to return to the “Whom” page"
			+ "SQ-T6296 Verify side panel for split test"
			+ "SQ-T6302 Verify the case in split test setup for mass offer when user doest not enter total percentage = 100 for variant A,B and control group and select 'Next' button on split test modal.", groups = {
					"regression" }, priority = 6)
	@Owner(name = "Jeevraj")
	public void T6294_VerifySplitCampaignButtonWhenGiftTypeIsNotARedeemable() throws Exception {

		String massCampaignName = "AutomationMassOffer_T6294_" + CreateDateTime.getTimeDateString();
		String dateTime = CreateDateTime.getCurrentDate() + " 11:00 PM";

		userEmail = dataSet.get("email");
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Campaigns");

		pageObj.dashboardpage().checkBoxFlagOnOffAndUpdate("business_enable_split_testing", "check", true);

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickSwitchToClassicBtn();
		// Select offer dropdown value
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();

		// set campaign name and gift type
		pageObj.signupcampaignpage().setCampaignName(massCampaignName);
		pageObj.signupcampaignpage().setGiftType(dataSet.get("giftType"));
		// pageObj.signupcampaignpage().setRedeemable(dataSet.get("redeemableName"));
		pageObj.signupcampaignpage().setFixedPoints(dataSet.get("points"));
		pageObj.signupcampaignpage().clickNextBtn();

		// Verify split panel before selecting segment
		// pageObj.campaignsplitpage().splitPanelVerifyonMassOfferWhomPage();

		// select segment
		pageObj.signupcampaignpage().setAudienceType(dataSet.get("audiance"));
		pageObj.signupcampaignpage().setSegmentType(dataSet.get("segment"));

		// Verify split panel after selecting segment for Gift type selected is other
		// than "Gift Redeemable".
		boolean splitPanelValidateNoGiftRedeemable = pageObj.campaignsplitpage().splitPanelVerifyNotGiftRedeemable();
		Assert.assertFalse(splitPanelValidateNoGiftRedeemable,
				"Split box is not appearing on whom page after selecting segment when Gift type selected is not a Gift Redeemable....");
		logger.info(" Split testing button is visible :" + splitPanelValidateNoGiftRedeemable);
		TestListeners.extentTest.get().pass(
				"Split box is not appearing on whom page after selecting segment when Gift type selected is not a Gift Redeemable");

		// click on previous button and move back to whom page and select Gift type as
		// redeemable and set split campaign to draft mode
		pageObj.signupcampaignpage().clickPreviousButton();
		pageObj.signupcampaignpage().setCampaignName(massCampaignName);
		pageObj.signupcampaignpage().setGiftType(dataSet.get("giftType2"));
		pageObj.signupcampaignpage().setRedeemable(dataSet.get("redeemableName"));
		pageObj.signupcampaignpage().clickNextBtn();

		// Verify split panel before selecting segment
		pageObj.campaignsplitpage().splitPanelVerifyonMassOfferWhomPage();

		// select segment
		pageObj.signupcampaignpage().setAudienceType(dataSet.get("audiance"));
		pageObj.signupcampaignpage().setSegmentType(dataSet.get("segment"));

		// Click on split button
		pageObj.campaignsplitpage().clickOnSplitButton();

		// Enter segment percentage != 100 % and verify user not able to proceed with
		// split setup
		pageObj.campaignsplitpage().addPercentInVarAandVarBandControlgroup(dataSet.get("varApercent"),
				dataSet.get("varBpercent"), dataSet.get("controlgrouppercent"));

		// click on next button and get error label text and verify with expected error
		// label
		pageObj.campaignsplitpage().clickOnNextButton();
		String splitPercentErrorlabel = pageObj.campaignsplitpage().getErrorLabelSplitPercent();
		logger.info("Entered split percentage != 100% and get text for error label displayed ");
		Assert.assertEquals(splitPercentErrorlabel, "The total must add to 100%.");
		TestListeners.extentTest.get().pass("Error displayed when user enter split percentage != 100%");

		// Enter split percentage = 100%
		pageObj.campaignsplitpage().addPercentInVarAandVarBandControlgroup(dataSet.get("varApercent"),
				dataSet.get("varBpercent2"), dataSet.get("controlgrouppercent"));

		// Click on save button
		pageObj.campaignsplitpage().clickOnNextButton();

		// Add email, PN, SMS and RM in both the variant
		pageObj.campaignsplitpage()
				.selectAndAddEmailOfVarA(dataSet.get("varAEmail") + " " + massCampaignName + " " + "{{{first_name}}}");
		pageObj.campaignsplitpage().clickOnSaveButton();
		pageObj.campaignsplitpage().selectAndAddPNOfVarA(dataSet.get("varAPushNoti") + " " + massCampaignName);
		pageObj.campaignsplitpage().clickOnSaveButton();
		pageObj.campaignsplitpage().selectAndAddSMSOfVarA(dataSet.get("varASMS") + " " + massCampaignName);
		pageObj.campaignsplitpage().clickOnSaveButton();
		pageObj.campaignsplitpage().selectAndAddRMOfVarA(dataSet.get("varARichMsg") + " " + massCampaignName);
		pageObj.campaignsplitpage().clickOnSaveButton();
		pageObj.campaignsplitpage().selectAndAddEmailOfVarB(dataSet.get("varBEmail") + " " + massCampaignName);
		pageObj.campaignsplitpage().clickOnSaveButton();
		pageObj.campaignsplitpage().selectAndAddPNOfVarB(dataSet.get("varBPushNoti") + " " + massCampaignName);
		pageObj.campaignsplitpage().clickOnSaveButton();
		pageObj.campaignsplitpage().selectAndAddSMSOfVarB(dataSet.get("varBSMS") + " " + massCampaignName);
		pageObj.campaignsplitpage().clickOnSaveButton();
		pageObj.campaignsplitpage().selectAndAddRMOfVarB(dataSet.get("varBRichMsg") + " " + massCampaignName);
		pageObj.campaignsplitpage().clickOnSaveButton();

		pageObj.campaignsplitpage().clickOnNextButton();
		pageObj.campaignsplitpage().clickOnSaveButton();
		Thread.sleep(2000);

		// Set split campaign to Draft mode and check side panel for split mass
		// campaign.
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickNewCamHomePageBtn();
		pageObj.newCamHomePage().searchCampaign(massCampaignName);
		pageObj.newCamHomePage().viewCampaignSidePanel();

		// Get all the labels from split side panel to verify
		String splitSidePanelElementsText = pageObj.campaignsplitpage().checkSplitSidePanelData();

		// Click on Variant A tab on split side panel and verify the labels for Draft
		// mass split campaign
		pageObj.campaignsplitpage().clickVariantASectionOnSplitSidePanel();
		Assert.assertTrue(splitSidePanelElementsText.contains("Segment"),
				"Segment label was not present on Variant A section split side panel");
		Assert.assertTrue(splitSidePanelElementsText.contains("Variant A"),
				"Variant A label was not present on Variant A section split side panel");
		Assert.assertTrue(splitSidePanelElementsText.contains("Variant B"),
				"Variant B label was not present on Variant A section split side panel");
		Assert.assertTrue(splitSidePanelElementsText.contains("Redeemable"),
				"Redeemable label was not present on Variant A section split side panel");
		Assert.assertTrue(splitSidePanelElementsText.contains("Message"),
				"Message label was not present on Variant A section split side panel");

		// Click on Variant B tab on split side panel and verify the labels for Draft
		// mass split campaign
		pageObj.campaignsplitpage().clickVariantBSectionOnSplitSidePanel();
		Assert.assertTrue(splitSidePanelElementsText.contains("Segment"),
				"Segment label was not present on Variant A section split side panel");
		Assert.assertTrue(splitSidePanelElementsText.contains("Variant A"),
				"Variant A label was not present on Variant A section split side panel");
		Assert.assertTrue(splitSidePanelElementsText.contains("Variant B"),
				"Variant B label was not present on Variant A section split side panel");
		Assert.assertTrue(splitSidePanelElementsText.contains("Redeemable"),
				"Redeemable label was not present on Variant A section split side panel");
		Assert.assertTrue(splitSidePanelElementsText.contains("Message"),
				"Message label was not present on Variant A section split side panel");
		TestListeners.extentTest.get().pass("Mass split campaign side panel verified for Draft status");

		// close mass split campaign side panel
		pageObj.newCamHomePage().closeSidePanel();

		// Edit split campaign and Schedule the mass split campaign
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().searchCampaign(massCampaignName);
		pageObj.newCamHomePage().clickOptionFromDotsDropDown(dataSet.get("option"));
		pageObj.signupcampaignpage().clickNextButton();
		pageObj.signupcampaignpage().clickNextButton();
		pageObj.campaignsplitpage().setStartTimesplitmasscampaign(dateTime);
		pageObj.signupcampaignpage().scheduleCampaign();

		// Verify the Mass split side panel section for Scheduled status
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickNewCamHomePageBtn();
		pageObj.newCamHomePage().searchCampaign(massCampaignName);
		pageObj.newCamHomePage().viewCampaignSidePanel();

		// Click on Variant A tab on split side panel and verify the labels for
		// Scheduled mass split campaign
		pageObj.campaignsplitpage().clickVariantASectionOnSplitSidePanel();
		Assert.assertTrue(splitSidePanelElementsText.contains("Segment"),
				"Segment label was not present on Variant A section split side panel");
		Assert.assertTrue(splitSidePanelElementsText.contains("Variant A"),
				"Variant A label was not present on Variant A section split side panel");
		Assert.assertTrue(splitSidePanelElementsText.contains("Variant B"),
				"Variant B label was not present on Variant A section split side panel");
		Assert.assertTrue(splitSidePanelElementsText.contains("Redeemable"),
				"Redeemable label was not present on Variant A section split side panel");
		Assert.assertTrue(splitSidePanelElementsText.contains("Message"),
				"Message label was not present on Variant A section split side panel");

		// Click on Variant B tab on split side panel and verify the labels for
		// Scheduled mass split campaign
		pageObj.campaignsplitpage().clickVariantBSectionOnSplitSidePanel();
		Assert.assertTrue(splitSidePanelElementsText.contains("Segment"),
				"Segment label was not present on Variant A section split side panel");
		Assert.assertTrue(splitSidePanelElementsText.contains("Variant A"),
				"Variant A label was not present on Variant A section split side panel");
		Assert.assertTrue(splitSidePanelElementsText.contains("Variant B"),
				"Variant B label was not present on Variant A section split side panel");
		Assert.assertTrue(splitSidePanelElementsText.contains("Redeemable"),
				"Redeemable label was not present on Variant A section split side panel");
		Assert.assertTrue(splitSidePanelElementsText.contains("Message"),
				"Message label was not present on Variant A section split side panel");
		TestListeners.extentTest.get().pass("Mass split campaign side panel verified for Scheduled status");

		// close mass split campaign side panel
		pageObj.newCamHomePage().closeSidePanel();

		// run mass offer
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().findMassCampaignNameandRun(massCampaignName);

		// Search for Mass split processed campaign and verify the side panel labels
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().searchCampaign(massCampaignName);
		Thread.sleep(4000);
		// check for processed status campaign by search campaign and refresh page
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().searchCampaign(massCampaignName);
		pageObj.newCamHomePage().viewCampaignSidePanel();

		// Click on Variant A tab on split side panel and verify the labels for
		// Processed mass split campaign
		pageObj.campaignsplitpage().clickVariantASectionOnSplitSidePanel();
		Assert.assertTrue(splitSidePanelElementsText.contains("Segment"),
				"Segment label was not present on Variant A section split side panel");
		Assert.assertTrue(splitSidePanelElementsText.contains("Variant A"),
				"Variant A label was not present on Variant A section split side panel");
		Assert.assertTrue(splitSidePanelElementsText.contains("Variant B"),
				"Variant B label was not present on Variant A section split side panel");
		Assert.assertTrue(splitSidePanelElementsText.contains("Redeemable"),
				"Redeemable label was not present on Variant A section split side panel");
		Assert.assertTrue(splitSidePanelElementsText.contains("Message"),
				"Message label was not present on Variant A section split side panel");

		// Click on Variant B tab on split side panel and verify the labels for
		// Processed mass split campaign
		pageObj.campaignsplitpage().clickVariantBSectionOnSplitSidePanel();
		Assert.assertTrue(splitSidePanelElementsText.contains("Segment"),
				"Segment label was not present on Variant A section split side panel");
		Assert.assertTrue(splitSidePanelElementsText.contains("Variant A"),
				"Variant A label was not present on Variant A section split side panel");
		Assert.assertTrue(splitSidePanelElementsText.contains("Variant B"),
				"Variant B label was not present on Variant A section split side panel");
		Assert.assertTrue(splitSidePanelElementsText.contains("Redeemable"),
				"Redeemable label was not present on Variant A section split side panel");
		Assert.assertTrue(splitSidePanelElementsText.contains("Message"),
				"Message label was not present on Variant A section split side panel");
		TestListeners.extentTest.get().pass("Mass split campaign side panel verified for Processed status");
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() throws Exception {
		pageObj.utils().clearDataSet(dataSet);
		logger.info("Data set cleared");
	}

	@AfterClass(alwaysRun = true)
	public void closeBrowser() {
		driver.quit();
		logger.info("Browser closed");
	}

}
