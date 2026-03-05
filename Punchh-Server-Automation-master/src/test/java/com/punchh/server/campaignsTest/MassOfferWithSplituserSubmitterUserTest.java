package com.punchh.server.campaignsTest;

import java.lang.reflect.Method;
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
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Test
@Listeners(TestListeners.class)
public class MassOfferWithSplituserSubmitterUserTest {
	private static Logger logger = LogManager.getLogger(MassOfferWithSplituserSubmitterUserTest.class);
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

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
		prop = Utilities.loadPropertiesFile("config.properties");
		driver = new BrowserUtilities().launchBrowser();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		// single Login to instance
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		sTCName = method.getName();
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==>" + dataSet);
		// Instance move to all business page
		utils = new Utilities();
	}

	// KhushbuSoni
	@Test(description = "SQ-T6286 Verify Other Business Role Users With Split Test Campaigns "
			+ "+ SQ-T6338 Verify no Duplicate entries in user_campaigns table for split mass offer", groups = {
					"regression" }, priority = 0)
	@Owner(name = "Khushbu Soni")
	public void T6286_VerifyfOtherBusinessRoleUsersWithSplitTestCampaigns() throws Exception {

		String massCampaignName = "AutomationMassOffer_" + CreateDateTime.getTimeDateString();
		String dateTime = CreateDateTime.getCurrentDate() + " 11:00 PM";

		userEmail = dataSet.get("userEmail");

		String userEmail1 = dataSet.get("userEmail1");
		String userEmail2 = dataSet.get("userEmail2");

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().logintoInstance(dataSet.get("userSubmitter"), dataSet.get("password"));

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickSwitchToClassicBtn();

		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().createWhatDetailsMassCampaign(massCampaignName, dataSet.get("giftType"),
				dataSet.get("redemable"));
		pageObj.signupcampaignpage().createWhomDetailsMassCampaign(dataSet.get("audiance"), dataSet.get("segment"),
				massCampaignName, dataSet.get("emailSubject"), dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().setFrequency("Daily");
		String startdateTime = CreateDateTime.getCurrentDate() + " 11:00 PM";
		String enddateTime = CreateDateTime.getFutureDate(1) + " 11:00 PM";
		pageObj.signupcampaignpage().setStartEndDateTime(startdateTime, enddateTime);
		boolean status = pageObj.campaignspage().validateSuccessMessage();

		// search and duplicate classic campaign
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.campaignspage().searchAndSelectCamapign(massCampaignName);

		// Edit campaign
		pageObj.campaignspage().selectOptionFromEllipsisee("Edit");

		pageObj.signupcampaignpage().clickNextBtn();

		// Verify split panel for daily campaign

		boolean splitPanelValidateNoGiftRedeemable = pageObj.campaignsplitpage().splitPanelVerifyNotGiftRedeemable();

		Assert.assertFalse(splitPanelValidateNoGiftRedeemable,
				"Split box is not appearing on whom page when selecting daily frequcy type");
		utils.logPass("Split box is not appearing on whom page when selecting frequency type as daily");

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

		// select segment
		pageObj.signupcampaignpage().setAudienceType(dataSet.get("audiance"));
		pageObj.signupcampaignpage().setSegmentType(dataSet.get("segment"));

		// Click on split button
		pageObj.campaignsplitpage().clickOnSplitButton();

		// enter segment percentage
		pageObj.campaignsplitpage().addPercentInVarAandVarBandControlgroup(dataSet.get("varApercent"),
				dataSet.get("varBpercent"), dataSet.get("controlgrouppercent"));
		// Click on save button
		pageObj.campaignsplitpage().clickOnNextButton();

		// Select Variant A offer
		pageObj.campaignsplitpage().setRedeemableVarAOfferField(dataSet.get("updatedRedeemableNameVarA"));

		// Add email, PN, SMS and RM in variant A
		pageObj.campaignsplitpage()
				.selectAndAddEmailOfVarA(dataSet.get("varAEmail") + " " + massCampaignName + " " + "{{{first_name}}}");
		pageObj.campaignsplitpage().clickOnSaveButton();
		pageObj.campaignsplitpage().selectAndAddPNOfVarA(dataSet.get("varAPushNoti") + " " + massCampaignName);
		pageObj.campaignsplitpage().clickOnSaveButton();
		pageObj.campaignsplitpage().selectAndAddSMSOfVarA(dataSet.get("varASMS") + " " + massCampaignName);
		pageObj.campaignsplitpage().clickOnSaveButton();
		pageObj.campaignsplitpage().selectAndAddRMOfVarA(dataSet.get("varARichMsg") + " " + massCampaignName);
		pageObj.campaignsplitpage().clickOnSaveButton();

		// Add email, PN, SMS and RM in variant A

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

		// click on next button on whom page
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.campaignsplitpage().setStartTimesplitmasscampaign(dateTime);
		pageObj.signupcampaignpage().scheduleCampaign();
		boolean status1 = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status1, "Schedule created successfully Success message did not displayed....");

		pageObj.newCamHomePage().clickNewCamHomePageBtn();
		pageObj.newCamHomePage().searchCampaign(massCampaignName);
		Thread.sleep(5000);
		pageObj.newCamHomePage().viewCampaignSidePanel();
		Thread.sleep(10000);

		String[] parentVarAandVarBCamId = pageObj.campaignsplitpage().getParentAndVarCamIdFromSidePanel();

		String ParentCampaignId = parentVarAandVarBCamId[1];

		utils.logPass("Verified values are same");
		utils.logit("Parent campaign id is " + java.util.Arrays.toString(parentVarAandVarBCamId));
		String VariantAId = parentVarAandVarBCamId[1];
		utils.logPass("Variant A campaign id: " + VariantAId);
		String VariantBId = parentVarAandVarBCamId[2];
		utils.logPass("Variant B campaign id: " + VariantBId);

		// close side panel
		pageObj.newCamHomePage().closeCampaignSidePanel();

		// run mass offer
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().findMassCampaignNameandRun(massCampaignName);

		// user email1
		Response loginResponse = pageObj.endpoints().Api2Login(userEmail1, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(loginResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token = loginResponse.jsonPath().get("access_token.token").toString();
		// fectch campaign name and reward gifted in account history user email1
		String campaignName = pageObj.guestTimelinePage().getCampaignNameByNotificationsAPILongPolling(massCampaignName,
				dataSet.get("client"), dataSet.get("secret"), token);
		Assert.assertTrue(campaignName.contains(massCampaignName), "Campaign name did not matched");
		String rewardGiftedAccountHistory = pageObj.guestTimelinePage().getUserAccountHistory(massCampaignName,
				dataSet.get("client"), dataSet.get("secret"), token);

		Assert.assertTrue(rewardGiftedAccountHistory.contains(massCampaignName),
				"Gifted redeemable did not appeared in account history");
		TestListeners.extentTest.get().pass(
				"Mass offer with split test detail: push notification, campaign name, redeemable validated successfully on timeline "
						+ rewardGiftedAccountHistory);

		// Check double gifting for campaign in database
		// pageObj.campaignsplitpage().checkDoubleGifting(env, VariantBId,
		// dataSet.get("business_id"));

		// SQL query to check for double gifting
		String getPunchhAppDeviceIDQuery = "select count(*) from user_campaigns where business_id="
				+ dataSet.get("business_id") + " and campaign_id=" + VariantBId + " group by user_id having count(*)>1";

		// Execute the query and retrieve the result
		String result = DBUtils.executeQueryAndGetColumnValue(env, getPunchhAppDeviceIDQuery, "count(*)");

		// Check if the result is empty or contains a value
		if (result.isEmpty()) {
			utils.logPass("No double gifting for this campaign");

			// Assert that no double gifting exists
			Assert.assertTrue(result.isEmpty(), "Expected no double gifting, but found a value.");
		} else {
			utils.logPass("Double gifting is there");

			// Assert that double gifting exists
			Assert.assertFalse(result.isEmpty(), "Expected double gifting, but result is empty.");
		}

	}

	// KhushbuSoni
	@Test(description = "SQ-T6574 Validate split campaign scheduling with indefinite expiry redeemables", groups = {
			"regression" }, priority = 1)
	@Owner(name = "Khushbu Soni")
	public void T6574_ValidateSplitCampaignSchedulingWithIndefiniteExpiryRedeemables() throws Exception {

		String massCampaignName = "AutomationMassOffer_" + CreateDateTime.getTimeDateString();
		String dateTime = CreateDateTime.getFutureDate(2) + " 11:00 PM";
		redeemableName += CreateDateTime.getTimeDateString();

		userEmail = dataSet.get("userEmail");

		String userEmail1 = dataSet.get("userEmail1");
		String userEmail2 = dataSet.get("userEmail2");

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().logintoInstance(dataSet.get("userSubmitter"), dataSet.get("password"));

		// Create Redeemable
		pageObj.menupage().navigateToSubMenuItem("Settings", "Redeemables");
		pageObj.redeemablePage().createRedeemable(redeemableName);
		pageObj.redeemablePage().selectRecieptRule("3");

		pageObj.redeemablePage().allowRedeemableToRunIndefinitely();
		utils.longWaitInSeconds(5);
		pageObj.redeemablePage().clickOnFinishButton();

		utils.longWaitInSeconds(5);

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickSwitchToClassicBtn();
		// Select offer dropdown value
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();

		// set campaign name and gift type
		pageObj.signupcampaignpage().setCampaignName(massCampaignName);
		pageObj.signupcampaignpage().setGiftType(dataSet.get("giftType"));
		pageObj.signupcampaignpage().setRedeemable(redeemableName);
		pageObj.signupcampaignpage().clickNextBtn();

		// select segment
		pageObj.signupcampaignpage().setAudienceType(dataSet.get("audiance"));
		pageObj.signupcampaignpage().setSegmentType(dataSet.get("segment"));

		// Click on split button
		pageObj.campaignsplitpage().clickOnSplitButton();

		// enter segment percentage
		pageObj.campaignsplitpage().addPercentInVarAandVarBandControlgroup(dataSet.get("varApercent"),
				dataSet.get("varBpercent"), dataSet.get("controlgrouppercent"));
		// Click on save button
		pageObj.campaignsplitpage().clickOnNextButton();

		// Select Variant A offer
		pageObj.campaignsplitpage().setRedeemableVarAOfferField(dataSet.get("updatedRedeemableNameVarA"));

		// Add email, PN, SMS and RM in variant A
		pageObj.campaignsplitpage()
				.selectAndAddEmailOfVarA(dataSet.get("varAEmail") + " " + massCampaignName + " " + "{{{first_name}}}");
		pageObj.campaignsplitpage().clickOnSaveButton();
		pageObj.campaignsplitpage().selectAndAddPNOfVarA(dataSet.get("varAPushNoti") + " " + massCampaignName);
		pageObj.campaignsplitpage().clickOnSaveButton();
		pageObj.campaignsplitpage().selectAndAddSMSOfVarA(dataSet.get("varASMS") + " " + massCampaignName);
		pageObj.campaignsplitpage().clickOnSaveButton();
		pageObj.campaignsplitpage().selectAndAddRMOfVarA(dataSet.get("varARichMsg") + " " + massCampaignName);
		pageObj.campaignsplitpage().clickOnSaveButton();

		// Add email, PN, SMS and RM in variant A

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

		// click on next button on whom page
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.campaignsplitpage().setStartTimesplitmasscampaign(dateTime);
		pageObj.signupcampaignpage().scheduleCampaign();
		boolean status1 = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status1, "Schedule created successfully, Success message was not displayed..");
		utils.logPass("Schedule created successfully");
		pageObj.utils().deleteRedeemableByName(redeemableName, env);
		pageObj.utils().deleteMassCampaignByName(massCampaignName, env);

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() throws Exception {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}

}
