package com.punchh.server.campaignsTest;

import java.lang.reflect.Method;
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

@Test
@Listeners(TestListeners.class)
public class MassOfferWithSplitTestingPartThreeTest {
	private static Logger logger = LogManager.getLogger(MassOfferWithSplitTestingPartThreeTest.class);
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

	// Jeevraj
	@Test(description = "SQ-T6303 Verify the Preview option for Email Editor options in mass offer split setup."
			+ "SQ-T6304 Verify the Preview option for Push notification Editor options in mass offer split setup."
			+ "SQ-T6305 Verify the Preview option for SMS Editor options in mass offer split setup."
			+ "SQ-T1395 Verify If we create campaign from redeemable page, redeemable is already selected on campaign What page", groups = {
					"regression" }, priority = 2)
	@Owner(name = "Jeevraj")
	public void T6303_VerifySplitCamPreviewEditorOption() throws Exception {

		String massCampaignName = "AutomationMassOffer_" + CreateDateTime.getTimeDateString();
		String dateTime = CreateDateTime.getCurrentDate() + " 11:00 PM";

		userEmail = dataSet.get("email");
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Campaigns");

		pageObj.dashboardpage().checkBoxFlagOnOffAndUpdate("business_enable_split_testing", "check", true);
		pageObj.menupage().navigateToSubMenuItem("Settings", "Redeemables");
		pageObj.redeemablePage().searchRedeemable(dataSet.get("redeemableName2"));
		pageObj.redeemablePage().selectRedeemableEllipsisOption(dataSet.get("redeemableName2"), dataSet.get("option2"));

		String giftType = pageObj.campaignsplitpage().getActualGiftType();
		Assert.assertEquals(giftType, dataSet.get("giftType"), "Gift type is not matching with the selected gift type");

		String redeemableName = pageObj.campaignsplitpage().getActualRedeemableName();
		Assert.assertEquals(redeemableName, dataSet.get("redeemableName2"),
				"Redeemable name is not matching with the selected redeemable name");
		TestListeners.extentTest.get().pass(
				"Gift type is preselected as Redeemable when campaign is created from redeemable page");

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

		// Add email, PN, SMS and RM for variant A and validate preview editor option is
		// visible
		pageObj.campaignsplitpage()
				.selectAndAddEmailOfVarA(dataSet.get("varAEmail") + " " + massCampaignName + " " + "{{{first_name}}}");
		pageObj.campaignsplitpage().clickOnSaveButton();
		pageObj.campaignsplitpage().selectAndAddPNOfVarA(dataSet.get("varAPushNoti") + " " + massCampaignName);
		pageObj.campaignsplitpage().clickOnSaveButton();
		pageObj.campaignsplitpage().selectAndAddSMSOfVarA(dataSet.get("varASMS") + " " + massCampaignName);
		pageObj.campaignsplitpage().clickOnSaveButton();
		pageObj.campaignsplitpage().selectAndAddRMOfVarA(dataSet.get("varARichMsg") + " " + massCampaignName);
		pageObj.campaignsplitpage().clickOnSaveButton();

		pageObj.campaignsplitpage().clickThreeDotsIconForVariants("Variant A", "Email");
		// store preview editor button isvisible value
		boolean isPreviewSplitEditorButtonVisible = pageObj.campaignsplitpage().isSplitPreviewEditorOptionVisible();
		Assert.assertTrue(isPreviewSplitEditorButtonVisible, "Preview button is not visible for Variant A Email");
		logger.info("Verified: Preview button is visible for Variant A Email");
		// close the three dots dropdown by clicking on same three dots section
		pageObj.campaignsplitpage().clickThreeDotsIconForVariants("Variant A", "Email");

		pageObj.campaignsplitpage().clickThreeDotsIconForVariants("Variant A", "Push notification");
		Assert.assertTrue(isPreviewSplitEditorButtonVisible, "Preview button is not visible for Variant A PN");
		logger.info("Verified: Preview button is visible for Variant A PN");
		pageObj.campaignsplitpage().clickThreeDotsIconForVariants("Variant A", "Push notification");

		pageObj.campaignsplitpage().clickThreeDotsIconForVariants("Variant A", "SMS");
		Assert.assertTrue(isPreviewSplitEditorButtonVisible, "Preview button is not visible for Variant A SMS");
		logger.info("Verified: Preview button is visible for Variant A SMS");
		pageObj.campaignsplitpage().clickThreeDotsIconForVariants("Variant A", "SMS");

		// Duplicate content of Varaint A communication channel data and copy to Variant
		// B channels data and validate preview editor button is visible
		pageObj.campaignsplitpage().splitDuplicateContentFromVarAToVarB("Variant B", "Email");
		pageObj.campaignsplitpage().clickThreeDotsIconForVariants("Variant B", "Email");
		Assert.assertTrue(isPreviewSplitEditorButtonVisible, "Preview button is not visible for Variant B Email");
		logger.info("Verified: Preview button is visible for Variant B Email");

		pageObj.campaignsplitpage().splitDuplicateContentFromVarAToVarB("Variant B", "Push notification");
		pageObj.campaignsplitpage().clickThreeDotsIconForVariants("Variant B", "Push notification");
		Assert.assertTrue(isPreviewSplitEditorButtonVisible, "Preview button is not visible for Variant B PN");
		logger.info("Verified: Preview button is visible for Variant B PN");

		pageObj.campaignsplitpage().splitDuplicateContentFromVarAToVarB("Variant B", "SMS");
		pageObj.campaignsplitpage().clickThreeDotsIconForVariants("Variant B", "Push notification");
		Assert.assertTrue(isPreviewSplitEditorButtonVisible, "Preview button is not visible for Variant B SMS");
		logger.info("Verified: Preview button is visible for Variant B SMS");
		pageObj.campaignsplitpage().splitDuplicateContentFromVarAToVarB("Variant B", "Rich message");
		TestListeners.extentTest.get().pass(
				"Preview editor option validation was successfull for both Variant A and Variant B Email,PN and SMS");

	}

	@Test(description = "SQ-T6528 Validate split campaign scheduling when Var a has specific expiry date and Var B has specific expiry date", groups = "Regression", priority = 3)
	@Owner(name = "Amit Kumar")
	public void T6528_VerifyErrorOnWhenPageInSplitCampaignWithSpecificredeemable() throws Exception {

		String massCampaignName = "AutomationMassOffer_" + CreateDateTime.getTimeDateString();
		String campaignScheduledateTime = "";
		String redeembale1EnddDateTime = CreateDateTime.getFutureDate(10) + " 11:00 PM";
		String redeemable_1Name = "Redeemable1_" + CreateDateTime.getTimeDateString();
		campaignScheduledateTime = CreateDateTime.getDateTimePlusDays(redeembale1EnddDateTime, 3);

		// Login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Settings", "Redeemables");
		pageObj.redeemablePage().createRedeemableWithFlatDiscountAndEndDate(redeemable_1Name, "1",
				redeembale1EnddDateTime, "(GMT+05:30) New Delhi ( IST )");

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickSwitchToClassicBtn();
		// Select offer dropdown value
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();

		// set campaign name and gift type
		pageObj.signupcampaignpage().setCampaignName(massCampaignName);
		pageObj.signupcampaignpage().setGiftType(dataSet.get("giftType"));
		pageObj.signupcampaignpage().setRedeemable(redeemable_1Name);
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().setAudienceType(dataSet.get("audiance"));
		pageObj.signupcampaignpage().setSegmentType(dataSet.get("segment"));

		// Click on split button
		pageObj.campaignsplitpage().clickOnSplitButton();

		// enter segment percentage
		pageObj.campaignsplitpage().addPercentInVarAandVarBandControlgroup(dataSet.get("varApercent"),
				dataSet.get("varBpercent"), dataSet.get("controlgrouppercent"));
		// Click on save button
		pageObj.campaignsplitpage().clickOnNextButton();

		// Add email, PN, SMS and RM for variant A and validate preview editor option is
		// visible
		pageObj.campaignsplitpage().selectAndAddEmailOfVarA(dataSet.get("varAEmail") + " " + massCampaignName);
		pageObj.campaignsplitpage().clickOnSaveButton();
		pageObj.campaignsplitpage().selectAndAddPNOfVarA(dataSet.get("varAPushNoti") + " " + massCampaignName);
		pageObj.campaignsplitpage().clickOnSaveButton();
		pageObj.campaignsplitpage().selectAndAddSMSOfVarA(dataSet.get("varASMS") + " " + massCampaignName);
		pageObj.campaignsplitpage().clickOnSaveButton();
		pageObj.campaignsplitpage().selectAndAddRMOfVarA(dataSet.get("varARichMsg") + " " + massCampaignName);
		pageObj.campaignsplitpage().clickOnSaveButton();

		// set redeemable of Var B
		pageObj.campaignsplitpage().selectOfferOfVarB();
		pageObj.campaignsplitpage().setRedeemableVarBOfferField(redeemable_1Name);

		pageObj.campaignsplitpage().selectAndAddEmailOfVarB(dataSet.get("varAEmail") + " " + massCampaignName);
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
		pageObj.signupcampaignpage().clickNextButton();

		pageObj.campaignsplitpage().setStartTimesplitmasscampaign(campaignScheduledateTime);
		pageObj.signupcampaignpage().scheduleCampaign();
		String actualErrorMessage = pageObj.signupcampaignpage().campaignScheduleErrorMessage();
		String expectedErrorMessage = "Error creating schedule: Start time should be less than or equal to redeemable effective expiry time "
				+ CreateDateTime.convertDateFormatTo(redeembale1EnddDateTime) + " IST.";
		Assert.assertTrue(actualErrorMessage.contains(expectedErrorMessage),
				"Error message is not matching when Var A and Var B has different redeemable end date");
		utils.logPass("Error message is displayed when Var A and Var B has different redeemable end date");
	}

	// Shubham Kumar Gupta
	@Test(description = "SQ-T6526 Validate split campaign scheduling when Var a has X expiry date and Var B has Y expiry date", groups = "Regression", priority = 4)
	@Owner(name = "Shubham Gupta")
	public void T6526_VerifyErrorOnWhenPageInSplitCampaign() throws Exception {

		String massCampaignName = "AutomationMassOffer_" + CreateDateTime.getTimeDateString();
		String campaignScheduledateTime = "";
		String redeembale1EnddDateTime = CreateDateTime.getFutureDate(10) + " 11:00 PM";
		String redeembale2EnddDateTime = CreateDateTime.getFutureDate(5) + " 11:00 PM";
		String redeemable_1Name = "Redeemable1_" + CreateDateTime.getTimeDateString();
		String redeemable_2Name = "Redeemable2_" + CreateDateTime.getTimeDateString();
		campaignScheduledateTime = CreateDateTime.getDateTimeMinusDays(redeembale1EnddDateTime, 3);

		// Login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Settings", "Redeemables");
		pageObj.redeemablePage().createRedeemableWithFlatDiscountAndEndDate(redeemable_1Name, "1",
				redeembale1EnddDateTime, "(GMT+05:30) New Delhi ( IST )");
		pageObj.redeemablePage().createRedeemableWithFlatDiscountAndEndDate(redeemable_2Name, "1",
				redeembale2EnddDateTime, "(GMT+05:30) New Delhi ( IST )");

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickSwitchToClassicBtn();
		// Select offer dropdown value
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();

		// set campaign name and gift type
		pageObj.signupcampaignpage().setCampaignName(massCampaignName);
		pageObj.signupcampaignpage().setGiftType(dataSet.get("giftType"));
		pageObj.signupcampaignpage().setRedeemable(redeemable_1Name);
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().setAudienceType(dataSet.get("audiance"));
		pageObj.signupcampaignpage().setSegmentType(dataSet.get("segment"));

		// Click on split button
		pageObj.campaignsplitpage().clickOnSplitButton();

		// enter segment percentage
		pageObj.campaignsplitpage().addPercentInVarAandVarBandControlgroup(dataSet.get("varApercent"),
				dataSet.get("varBpercent"), dataSet.get("controlgrouppercent"));
		// Click on save button
		pageObj.campaignsplitpage().clickOnNextButton();

		// Add email, PN, SMS and RM for variant A and validate preview editor option is
		// visible
		pageObj.campaignsplitpage().selectAndAddEmailOfVarA(dataSet.get("varAEmail") + " " + massCampaignName);
		pageObj.campaignsplitpage().clickOnSaveButton();
		pageObj.campaignsplitpage().selectAndAddPNOfVarA(dataSet.get("varAPushNoti") + " " + massCampaignName);
		pageObj.campaignsplitpage().clickOnSaveButton();
		pageObj.campaignsplitpage().selectAndAddSMSOfVarA(dataSet.get("varASMS") + " " + massCampaignName);
		pageObj.campaignsplitpage().clickOnSaveButton();
		pageObj.campaignsplitpage().selectAndAddRMOfVarA(dataSet.get("varARichMsg") + " " + massCampaignName);
		pageObj.campaignsplitpage().clickOnSaveButton();

		// set redeemable of Var B
		pageObj.campaignsplitpage().selectOfferOfVarB();
		pageObj.campaignsplitpage().setRedeemableVarBOfferField(redeemable_2Name);

		pageObj.campaignsplitpage().selectAndAddEmailOfVarB(dataSet.get("varAEmail") + " " + massCampaignName);
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
		pageObj.signupcampaignpage().clickNextButton();

		pageObj.campaignsplitpage().setStartTimesplitmasscampaign(campaignScheduledateTime);
		pageObj.signupcampaignpage().scheduleCampaign();
		String actualErrorMessage = pageObj.signupcampaignpage().campaignScheduleErrorMessage();
		String expectedErrorMessage = "Error creating schedule: Start time should be less than or equal to redeemable effective expiry time "
				+ CreateDateTime.convertDateFormatTo(redeembale2EnddDateTime) + " IST.";
		Assert.assertTrue(actualErrorMessage.contains(expectedErrorMessage),
				"Error message is not matching when Var A and Var B has different redeemable end date");

		campaignScheduledateTime = CreateDateTime.getDateTimePlusDays(redeembale2EnddDateTime, 7);
		pageObj.campaignsplitpage().setStartTimesplitmasscampaign(campaignScheduledateTime);
		pageObj.signupcampaignpage().scheduleCampaign();
		String actualErrorMessage1 = pageObj.signupcampaignpage().campaignScheduleErrorMessage();
		Assert.assertTrue(actualErrorMessage1.contains(expectedErrorMessage),
				"Error message is not matching when Var A and Var B has different redeemable end date");
		utils.logPass("Error message is displayed when Var A and Var B has different redeemable end date");

		pageObj.utils().deleteRedeemableByName(redeemable_1Name, env);
		pageObj.utils().deleteRedeemableByName(redeemable_2Name, env);
		pageObj.utils().deleteMassCampaignByName(massCampaignName, env);
	}

	// piyush
	@Test(description = "SQ-T6603 Validate split campaign scheduling when Var a has N expiry date and Var B has N expiry date", groups = "Regression", priority = 5)
	@Owner(name = "Piyush Kumar")
	public void T6603_VerifyErrorOnWhenPageInSplitCampaignWithNdaysExpiryredeemable() throws Exception {

		String massCampaignName = "AutomationMassOffer_" + CreateDateTime.getTimeDateString();
		String redeemable_1Name = "Redeemable1_" + CreateDateTime.getTimeDateString();
		String campaignScheduledateTime = CreateDateTime.getFutureDate(5) + " 11:00 PM";

		// Login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Settings", "Redeemables");
		pageObj.redeemablePage().createRedeemableWithFlatDiscountAndExpiryInDays(redeemable_1Name, "1", "3",
				"(GMT+05:30) New Delhi ( IST )");

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickSwitchToClassicBtn();
		// Select offer dropdown value
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();

		// set campaign name and gift type
		pageObj.signupcampaignpage().setCampaignName(massCampaignName);
		pageObj.signupcampaignpage().setGiftType(dataSet.get("giftType"));
		pageObj.signupcampaignpage().setRedeemable(redeemable_1Name);
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().setAudienceType(dataSet.get("audiance"));
		pageObj.signupcampaignpage().setSegmentType(dataSet.get("segment"));

		// Click on split button
		pageObj.campaignsplitpage().clickOnSplitButton();

		// enter segment percentage
		pageObj.campaignsplitpage().addPercentInVarAandVarBandControlgroup(dataSet.get("varApercent"),
				dataSet.get("varBpercent"), dataSet.get("controlgrouppercent"));
		// Click on save button
		pageObj.campaignsplitpage().clickOnNextButton();

		// Add email, PN, SMS and RM for variant A and validate preview editor option is
		// visible
		pageObj.campaignsplitpage().selectAndAddEmailOfVarA(dataSet.get("varAEmail") + " " + massCampaignName);
		pageObj.campaignsplitpage().clickOnSaveButton();
		pageObj.campaignsplitpage().selectAndAddPNOfVarA(dataSet.get("varAPushNoti") + " " + massCampaignName);
		pageObj.campaignsplitpage().clickOnSaveButton();
		pageObj.campaignsplitpage().selectAndAddSMSOfVarA(dataSet.get("varASMS") + " " + massCampaignName);
		pageObj.campaignsplitpage().clickOnSaveButton();
		pageObj.campaignsplitpage().selectAndAddRMOfVarA(dataSet.get("varARichMsg") + " " + massCampaignName);
		pageObj.campaignsplitpage().clickOnSaveButton();

		// set redeemable of Var B
		pageObj.campaignsplitpage().selectOfferOfVarB();
		pageObj.campaignsplitpage().setRedeemableVarBOfferField(redeemable_1Name);
		pageObj.campaignsplitpage().selectAndAddEmailOfVarB(dataSet.get("varAEmail") + " " + massCampaignName);
		pageObj.campaignsplitpage().clickOnSaveButton();
		pageObj.campaignsplitpage().selectAndAddPNOfVarB(dataSet.get("varBPushNoti") + " " + massCampaignName);
		pageObj.campaignsplitpage().clickOnSaveButton();
		pageObj.campaignsplitpage().selectAndAddSMSOfVarB(dataSet.get("varBSMS") + " " + massCampaignName);
		pageObj.campaignsplitpage().clickOnSaveButton();
		pageObj.campaignsplitpage().selectAndAddRMOfVarB(dataSet.get("varBRichMsg") + " " + massCampaignName);
		pageObj.campaignsplitpage().clickOnSaveButton();
		pageObj.campaignsplitpage().clickOnNextButton();
		pageObj.campaignsplitpage().clickOnSaveButton();
		pageObj.signupcampaignpage().clickNextButton();
		String dateTime = CreateDateTime.getTomorrowDate() + " 11:00 PM";
		pageObj.campaignsplitpage().setStartTimesplitmasscampaign(dateTime);
		pageObj.signupcampaignpage().scheduleCampaign();
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "Schedule created successfully Success message did not displayed....");
		utils.logPass("Schedule created successfully");
		pageObj.utils().deleteRedeemableByName(redeemable_1Name, env);
		pageObj.utils().deleteMassCampaignByName(massCampaignName, env);
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
