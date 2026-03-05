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
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.ApiUtils;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.GmailConnection;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Test
@Listeners(TestListeners.class)
public class SplitMassOfferCampaignTest {
	private static Logger logger = LogManager.getLogger(SplitMassOfferCampaignTest.class);
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
	ApiUtils apiUtils;

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
		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
	}

	// Amit
	@Test(description = "SQ-T5951 Verify user can create, schedule, process a mass offer split campaign"
			+ "SQ-T6116 Verify user can select email template for both variant A and B"
			+ "SQ-T6201 Verify after user select Variant A and Variant B redeemable for Split testing setup button on whom page for mass gifting campaign and return to What page Redeemable A field will be disabled"
			+ "SQ-T6145 Verify If send test feature is working or not when split test is configured in mass campaign", groups = {
					"regression" }, priority = 0)
	@Owner(name = "Amit Kumar")
	public void T5951_verifyCreateScheduleProcessMassOfferSplitCampaign() throws Exception {
		// enable Enable Superfetch Redis Lock in misc config in cockpit
		String massCampaignName = "AutomationMassOffer_" + CreateDateTime.getTimeDateString();
		String dateTime = CreateDateTime.getCurrentDate() + " 11:00 PM";

		String userEmail1 = dataSet.get("userEmail1");
		String userEmail2 = dataSet.get("userEmail2");

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

		// Add email, PN, SMS and RM in both the variant
		// Add email content with template to Variant A
		boolean flag = pageObj.campaignsplitpage().addEmailTemplateToVariant("Variant A", "Email",
				dataSet.get("varAEmail") + " " + massCampaignName);
		Assert.assertTrue(flag, "Email template is not added to Variant A");
		pageObj.campaignsplitpage().clickOnSaveButton();
		pageObj.campaignsplitpage().selectAndAddPNOfVarA(dataSet.get("varAPushNoti") + " " + massCampaignName);
		pageObj.campaignsplitpage().clickOnSaveButton();
		pageObj.campaignsplitpage().selectAndAddSMSOfVarA(dataSet.get("varASMS") + " " + massCampaignName);
		pageObj.campaignsplitpage().clickOnSaveButton();
		pageObj.campaignsplitpage().selectAndAddRMOfVarA(dataSet.get("varARichMsg") + " " + massCampaignName);
		pageObj.campaignsplitpage().clickOnSaveButton();

		// Add redeemable, email, PN, SMS and RM in B variant
		pageObj.campaignsplitpage().selectOfferOfVarB();
		pageObj.campaignsplitpage().enterRedeemableNameVarBOfferField(dataSet.get("redeemableNameB"));
		// Add email content with template to Variant B
		boolean flag1 = pageObj.campaignsplitpage().addEmailTemplateToVariant("Variant B", "Email",
				dataSet.get("varBEmail") + " " + massCampaignName);
		Assert.assertTrue(flag1, "Email template is not added to Variant B");
		utils.logPass("Verified user can select email template for both variant A and B");
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

		// verify text below Split test panel after setting split testing offer and
		// messages
		String text = pageObj.campaignsplitpage().verifyTextUnderSplitTestPanel();
		Assert.assertTrue(text.contains(
				"Since you've configured the split test, the 'Message' section is now embedded within the split test modal."),
				"Text under Split test panel is not matching");
		utils.logPass("Text under Split test panel matched successfully");

		// navigate to What page and validate redeemable field is disabled
		pageObj.signupcampaignpage().clickPreviousButton();
		boolean state = pageObj.campaignsplitpage().checkSplitTestRedeemableFieldState();
		Assert.assertFalse(state,
				"Redeemable A field is enabled on What page after setting split testing offer and messages");
		utils.logPass("Redeemable A field is disabled on What page after setting split testing offer and messages");

		// click on next button on what page
		pageObj.signupcampaignpage().clickNextBtn();
		utils.longWaitInSeconds(2);

		// enter email in Test User Email and click Send Test Notification button ,
		// include gift flag is OFF
		pageObj.campaignspage().sendTestNotification(dataSet.get("userEmail1"));

		Response loginResponse = pageObj.endpoints().Api2Login(dataSet.get("userEmail1"), dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(loginResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token = loginResponse.jsonPath().get("access_token.token").toString();
		// verify campaign name and push notification
		String campaignName = pageObj.guestTimelinePage().getCampaignNameByNotificationsAPIShortPoll(massCampaignName,
				dataSet.get("client"), dataSet.get("secret"), token);
		String rewardGiftedAccountHistory = pageObj.guestTimelinePage().getUserAccountHistory(massCampaignName,
				dataSet.get("client"), dataSet.get("secret"), token);

		Assert.assertTrue(campaignName.contains(massCampaignName), "Campaign name did not matched");
		Assert.assertFalse(rewardGiftedAccountHistory.contains(massCampaignName),
				"Gifted redeemable appeared in account history even if include gift flag is not selected");
		utils.logPass("Verified Split test send test notification feature is wokring fine with include gift flag is OFF");

		// enter email in Test User Email and click Send Test Notification button ,
		// include gift flag is ON
		pageObj.signupcampaignpage().setSendNotificationGuest(dataSet.get("userEmail1"));

		// verify campaign name , push notification and Reward gifted in account history
		String campaignName1 = pageObj.guestTimelinePage().getCampaignNameByNotificationsAPIShortPoll(massCampaignName,
				dataSet.get("client"), dataSet.get("secret"), token);
		String rewardGiftedAccountHistory1 = pageObj.guestTimelinePage().getUserAccountHistory(massCampaignName,
				dataSet.get("client"), dataSet.get("secret"), token);

		Assert.assertTrue(campaignName1.contains(massCampaignName), "Campaign name did not matched");
		Assert.assertTrue(rewardGiftedAccountHistory1.contains(massCampaignName),
				"Gifted redeemable did not appeared in account history when include gift flag is selected");
		utils.logPass("Verified Split test send test notification feature is wokring fine with include gift flag is ON");

		// click on next button on whom page
		pageObj.signupcampaignpage().clickNextBtn();
		utils.longWaitInSeconds(2);

		// schedule campaign
		pageObj.campaignsplitpage().setStartTimesplitmasscampaign(dateTime);
		pageObj.signupcampaignpage().scheduleCampaign();
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "Schedule created successfully Success message did not displayed....");

		// disable Segmentation via s3 flow
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Campaigns");
		pageObj.dashboardpage().checkUncheckAnyFlag("Segmentation via s3 flow", "uncheck");

		// run mass offer
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().findMassCampaignNameandRun(massCampaignName);

		// user email1
		Response loginResponse1 = pageObj.endpoints().Api2Login(userEmail1, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(loginResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token1 = loginResponse1.jsonPath().get("access_token.token").toString();
		// fectch campaign name and reward gifted in account history user email1
		String campaignName2 = pageObj.guestTimelinePage().getCampaignNameByNotificationsAPI(massCampaignName,
				dataSet.get("client"), dataSet.get("secret"), token1);
		String rewardGiftedAccountHistory2 = pageObj.guestTimelinePage().getUserAccountHistory(massCampaignName,
				dataSet.get("client"), dataSet.get("secret"), token1);

		// user email2
		Response loginResponse2 = pageObj.endpoints().Api2Login(userEmail2, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(loginResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token2 = loginResponse2.jsonPath().get("access_token.token").toString();
		// fetch campaign name and reward gifted in account history user email2
		String campaignName3 = pageObj.guestTimelinePage().getCampaignNameByNotificationsAPI(massCampaignName,
				dataSet.get("client"), dataSet.get("secret"), token2);
		String rewardGiftedAccountHistory3 = pageObj.guestTimelinePage().getUserAccountHistory(massCampaignName,
				dataSet.get("client"), dataSet.get("secret"), token2);

		Assert.assertTrue(campaignName2.contains(massCampaignName), "Campaign name did not matched");
		Assert.assertTrue(rewardGiftedAccountHistory2.contains(massCampaignName),
				"Gifted redeemable did not appeared in account history");
		Assert.assertTrue(campaignName3.contains(massCampaignName), "Campaign name did not matched");
		Assert.assertTrue(rewardGiftedAccountHistory3.contains(massCampaignName),
				"Gifted redeemable did not appeared in account history");
		TestListeners.extentTest.get().pass(
				"Mass offer with split test detail: push notification, campaign name, redeemable validated successfully on timeline "
						+ rewardGiftedAccountHistory + "::" + rewardGiftedAccountHistory2);

	}

	@Test(description = "SQ-T5954 Verify split mass offer campaign with Variant A has redeemable and all message type and in Variant B select no redeemable and all message type"
			+ "SQ-T5956 Verify for split mass offer campaign we can update the segment on Whom page and the segment is updated in split test configuration", groups = {
					"regression" }, priority = 1)
	@Owner(name = "Amit Kumar")
	public void T5954_verifySplitMassOfferCampaignWithVariantAHasRedeemableAndAllMessageTypeAndVariantBSelectNoRedeemableAndAllMessageType()
			throws Exception {
		// enable Enable Superfetch Redis Lock in misc config in cockpit
		String massCampaignName = "AutomationMassOffer_" + CreateDateTime.getTimeDateString();
		String dateTime = CreateDateTime.getCurrentDate() + " 11:00 PM";

		userEmail = dataSet.get("userEmail");

		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Campaigns");

		pageObj.dashboardpage().checkBoxFlagOnOffAndUpdate("business_enable_split_testing", "check", true);

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickSwitchToClassicBtn();
		// Select offer drop down value
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();

		// set campaign name and gift type
		pageObj.signupcampaignpage().setCampaignName(massCampaignName);
		pageObj.signupcampaignpage().setGiftType(dataSet.get("giftType"));
		pageObj.signupcampaignpage().setRedeemable(dataSet.get("redeemableName")); // redeemable for variant A
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

		// Add email, PN, SMS and RM in A variant
		pageObj.campaignsplitpage()
				.selectAndAddEmailOfVarA(dataSet.get("varAEmail") + " " + massCampaignName + " " + "{{{first_name}}}");
		pageObj.campaignsplitpage().clickOnSaveButton();
		pageObj.campaignsplitpage().selectAndAddPNOfVarA(dataSet.get("varAPushNoti") + " " + massCampaignName);
		pageObj.campaignsplitpage().clickOnSaveButton();
		pageObj.campaignsplitpage().selectAndAddSMSOfVarA(dataSet.get("varASMS") + " " + massCampaignName);
		pageObj.campaignsplitpage().clickOnSaveButton();
		pageObj.campaignsplitpage().selectAndAddRMOfVarA(dataSet.get("varARichMsg") + " " + massCampaignName);
		pageObj.campaignsplitpage().clickOnSaveButton();

		// Add email, PN, SMS and RM in B variant
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
		// select segment update segment
		// pageObj.signupcampaignpage().setAudianceType(dataSet.get("audiance"));
		pageObj.signupcampaignpage().setSegmentType(dataSet.get("segmentNew"));

		// click on next button on whom page
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.campaignsplitpage().setStartTimesplitmasscampaign(dateTime);
		pageObj.signupcampaignpage().scheduleCampaign();
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "Schedule created successfully Success message did not displayed....");

		// run mass offer
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().findMassCampaignNameandRun(massCampaignName);

		Response loginResponse = pageObj.endpoints().Api2Login(userEmail, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(loginResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token = loginResponse.jsonPath().get("access_token.token").toString();

		// fectch campaign name and reward gifted in account history
		String campaignName = pageObj.guestTimelinePage().getCampaignNameByNotificationsAPILongPolling(massCampaignName,
				dataSet.get("client"), dataSet.get("secret"), token);

		String rewardGiftedAccountHistory = pageObj.guestTimelinePage().getUserAccountHistory(massCampaignName,
				dataSet.get("client"), dataSet.get("secret"), token);

		Assert.assertTrue(campaignName.contains(massCampaignName), "Campaign name did not matched");
		if (campaignName.contains("Variant A")) {
			Assert.assertTrue(rewardGiftedAccountHistory.contains(massCampaignName),
					"Gifted redeemable did not appeared in account history");
		}
		TestListeners.extentTest.get().pass(
				"Mass offer with split test detail: push notification, campaign name, redeemable validated successfully on timeline");

	}

	// Rakhi
	@Test(description = "SQ-T6111 Verify If same percentage in Var A and B showing same user count"
			+ "SQ-T6113 Verify If Review page loading correctly when rich message doesn't have any content")
	@Owner(name = "Rakhi Rawat")
	public void T6111_VerifySamePercentageInBothVariant() throws Exception {

		String massCampaignName = CreateDateTime.getUniqueString("Automation Mass Campaign with Split Test");
		logger.info("Campaign name is :" + massCampaignName);

		// user signup using pos signup api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response resp = pageObj.endpoints().posSignUp(userEmail, dataSet.get("locationkey"));
		Assert.assertEquals(200, resp.getStatusCode(), "Status code 200 did not matched for pos signup api");
		String userID = resp.jsonPath().get("id").toString();

		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Campaigns");

		pageObj.dashboardpage().checkBoxFlagOnOffAndUpdate("business_enable_split_testing", "check", true);

		// navigate to campaigns
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
		pageObj.signupcampaignpage().setAudienceType(dataSet.get("audiance"));
		pageObj.signupcampaignpage().setSegmentType(dataSet.get("segment"));
		pageObj.campaignsplitpage().clickOnSplitButton();

		// Verify elements on split test dialogue box after clicking split panel
		for (int i = 1; i <= 5; i++) {
			Assert.assertTrue(
					pageObj.campaignsplitpage().verifySplitTestElementsVisibility(dataSet.get("element" + i), "", ""),
					dataSet.get("element" + i) + " is not visible on Whom page");
			utils.logPass("Verified " + dataSet.get("element" + i) + " is visible on Whom page");
		}
		// Verify split test variants percentage
		for (int i = 1; i <= 3; i++) {
			Assert.assertEquals(
					pageObj.campaignsplitpage().verifySplitTestVariantPercentage(dataSet.get("variant" + i)),
					dataSet.get("percentage" + i), "Percentage of " + dataSet.get("variant" + i) + " is not "
							+ dataSet.get("percentage" + i) + " by default");
			utils.logPass("Verified percentage of " + dataSet.get("variant" + i) + " is " + dataSet.get("percentage" + i)
					+ " by default");
		}
		// enter segment percentage for variant A , variant B and control group
		pageObj.campaignsplitpage().addPercentInVarAandVarBandControlgroup(dataSet.get("varApercent"),
				dataSet.get("varBpercent"), dataSet.get("controlgrouppercent"));
		pageObj.campaignsplitpage().clickOnNextButton();

		// Verify elements on Gift and message page after clicking next button
		for (int i = 1; i <= 4; i++) {
			Assert.assertTrue(
					pageObj.campaignsplitpage().verifySplitTestElementsVisibility("checkBox", "Variant A",
							dataSet.get("channel" + i)),
					dataSet.get("channel" + i) + " checkbox is not visible on Whom page for Variant A");
			utils.logPass("Verified " + dataSet.get("channel" + i) + " checkbox is visible on Whom page for Variant A");
		}
		for (int i = 3; i <= 6; i++) {
			Assert.assertTrue(
					pageObj.campaignsplitpage().verifySplitTestElementsVisibility(dataSet.get("element" + i), "", ""),
					dataSet.get("element" + i) + " is not visible on Gift and message page");
			utils.logPass("Verified " + dataSet.get("element" + i) + " is visible on Gift and message page");
		}
		// Add email content to Variant A
		pageObj.campaignsplitpage().selectAndAddEmailOfVarA(dataSet.get("varAEmail") + " " + massCampaignName);
		pageObj.campaignsplitpage().clickOnSaveButton();

		// SQ-T6113 starting from here
		// add rich msg for variant A without any content and save
		pageObj.campaignsplitpage().checkGivenSplitTestCheckBox("Variant A", "Rich message");
		pageObj.signupcampaignpage().clickNextBtn();

		// verify review page contents {Segment breakdown}
		List<String> actualValues = pageObj.campaignsplitpage().verifySplitTestReviewPage("Segment breakdown");
		logger.info(actualValues);
		Assert.assertTrue(actualValues.get(0).contains("40%"), "Variant A percentage did not matched on summary page");
		Assert.assertTrue(actualValues.get(1).contains("40%"), "Variant B percentage did not matched on summary page");
		Assert.assertTrue(actualValues.get(2).contains("20%"),
				"Control group percentage did not matched on summary page");
		utils.logPass("Verified split test review page with Variant A, Variant B and Control group percentage");

		// verify review page contents {Redeemable}
		List<String> actualGiftAttached = pageObj.campaignsplitpage().verifySplitTestReviewPage("Redeemable");
		logger.info(actualGiftAttached);
		Assert.assertTrue(actualGiftAttached.size() == 1,
				"Variant B Redeemable section is expected to be absent but it is present");
		Assert.assertTrue(actualGiftAttached.get(0).contains(dataSet.get("redeemableName")),
				"Variant A attched gift redeemable did not matched on review page");
		utils.logPass("Attached Gift Redeemable is visible on split test review page");

		// verify review page contents {Email}
		List<String> actualEmail = pageObj.campaignsplitpage().verifySplitTestReviewPage("Email");
		logger.info(actualEmail);
		Assert.assertTrue(actualEmail.get(0).contains(dataSet.get("varAEmail") + " " + massCampaignName),
				"Variant A Email did not matched on review page");
		Assert.assertTrue(actualEmail.size() == 1,
				"Variant B Email section is expected to be absent but it is present");
		utils.logPass("Attached Email is visible on split test review page");

	}

	// Rakhi
	@Test(description = "SQ-T6124 Verify Users can select which variant(s) to send or NOT send an email"
			+ "SQ-T6125 Verify If preview of Email, PN and RM showing correct in new tab or not"
			+ "SQ-T6114 Verify user can duplicate email content from variant A to variant B")
	@Owner(name = "Rakhi Rawat")
	public void T6124_VerifyWhichVariantToSendEmail() throws Exception {

		String massCampaignName = CreateDateTime.getUniqueString("Automation Mass Campaign with Split Test");
		logger.info("Campaign name is :" + massCampaignName);

		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Campaigns");

		pageObj.dashboardpage().checkBoxFlagOnOffAndUpdate("business_enable_split_testing", "check", true);
		pageObj.dashboardpage().checkBoxFlagOnOffAndUpdate("business_enable_control_group_in_campaigns", "check", true);
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
		pageObj.campaignsplitpage().clickOnNextButton();

		// Add email, PN, Rich message to Variant A
		pageObj.campaignsplitpage().selectAndAddEmailOfVarA(dataSet.get("varAEmail") + " " + massCampaignName);
		pageObj.campaignsplitpage().clickOnSaveButton();
		pageObj.campaignsplitpage().selectAndAddPNOfVarA(dataSet.get("varAPushNoti") + " " + massCampaignName);
		pageObj.campaignsplitpage().clickOnSaveButton();
		pageObj.campaignsplitpage().selectAndAddRMOfVarA(dataSet.get("varARichMsg") + " " + massCampaignName);
		pageObj.campaignsplitpage().clickOnSaveButton();

		// add offer to variant B
		pageObj.campaignsplitpage().selectOfferOfVarB();
		pageObj.campaignsplitpage().enterRedeemableNameVarBOfferField(dataSet.get("VarBoffer"));
		pageObj.campaignsplitpage().clickOnNextButton();

		// verify email should only be attached for variant A on summary page
		List<String> actualEmail = pageObj.campaignsplitpage().verifySplitTestReviewPage("Email");
		logger.info(actualEmail);
		Assert.assertTrue(actualEmail.size() == 1,
				"Variant B Email section is expected to be absent but it is present");
		Assert.assertTrue(actualEmail.get(0).contains(dataSet.get("varAEmail") + " " + massCampaignName),
				"Variant A Email did not matched on review page");
		utils.logPass("Verified users can select which variant(s) to send or NOT send an email");

		// clickPreviewLink
		pageObj.campaignsplitpage().clickPreviewLink("Push notification");
		String text1 = pageObj.campaignsplitpage().verifyFontFamilyAndroidApple("Android", "pushTitleAndroid");
		logger.info("Font family for Android is: " + text1);
		Assert.assertTrue(text1.contains("font-family: Manrope"), "Font family for Android is not Manrope");
		logger.info("Verified font family for Android is Manrope");
		utils.switchToParentWindow();
		String text2 = pageObj.campaignsplitpage().verifyFontFamilyAndroidApple("Apple", "pushTitleApple");
		Assert.assertTrue(text2.contains("font-family: SFProTextRegular"),
				"Font family for Android is not SFProTextRegular");
		logger.info("Verified font family for Android is SFProTextRegular");

		utils.switchToParentWindow();
		pageObj.campaignsplitpage().clickGotItBtn();
		pageObj.campaignsplitpage().clickOnBackBtn();

		// Duplicate content from variant A to variant B
		String text = pageObj.campaignsplitpage().setDuplicateContentFromVariantA("Variant B");
		Assert.assertEquals(text, dataSet.get("varAEmail") + " " + massCampaignName,
				"Content duplication from Variant A to Variant B failed");
		utils.logPass("Content duplication from Variant A to Variant B successful");

		// edit email content of variant B
		pageObj.campaignsplitpage().editEmailOfVarB(dataSet.get("varBEmail") + " " + massCampaignName);
		pageObj.campaignsplitpage().clickOnSaveButton();
		String titleB = pageObj.campaignsplitpage().verifyVariantEmail("Variant B");
		Assert.assertEquals(titleB, dataSet.get("varBEmail") + " " + massCampaignName,
				"Email content of Variant B is not updated");
		utils.logPass(
				"Email content of Variant B is updated successfully , user can edit the email content of Variant B");
	}

	@Test(description = "SQ-T6346 Verify users in both variant A and variant B receives email after split mass offer campaign is processed", groups = {
			"regression", "nonNightly" })
	@Owner(name = "Rakhi Rawat")
	public void T6346_verifyEmailFromMassOfferSplitCampaign() throws Exception {

		String massCampaignName = "AutomationMassOffer_" + CreateDateTime.getTimeDateString();
		String dateTime = CreateDateTime.getCurrentDate() + " 11:00 PM";

		String userEmail = dataSet.get("userEmail");
		String emailSubject = massCampaignName + " - Email Notification";

		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Campaigns");
		pageObj.dashboardpage().checkBoxFlagOnOffAndUpdate("business_enable_control_group_in_campaigns", "check", true);
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

		// Add email, PN, SMS and RM in both the variant
		pageObj.campaignsplitpage().selectAndAddEmailOfVarA(emailSubject);
		pageObj.campaignsplitpage().clickOnSaveButton();
		pageObj.campaignsplitpage().selectAndAddPNOfVarA(dataSet.get("varAPushNoti") + " " + massCampaignName);
		pageObj.campaignsplitpage().clickOnSaveButton();
		pageObj.campaignsplitpage().selectAndAddSMSOfVarA(dataSet.get("varASMS") + " " + massCampaignName);
		pageObj.campaignsplitpage().clickOnSaveButton();
		pageObj.campaignsplitpage().selectAndAddRMOfVarA(dataSet.get("varARichMsg") + " " + massCampaignName);
		pageObj.campaignsplitpage().clickOnSaveButton();

		// Add redeemable, email, PN, SMS and RM in B variant
		pageObj.campaignsplitpage().selectOfferOfVarB();
		pageObj.campaignsplitpage().enterRedeemableNameVarBOfferField(dataSet.get("VarBoffer"));
		pageObj.campaignsplitpage().selectAndAddEmailOfVarB(emailSubject);
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
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "Schedule created successfully Success message did not displayed....");

		// run mass offer
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().findMassCampaignNameandRun(massCampaignName);
		pageObj.guestTimelinePage().pingSessionforLongWait(5);
		// validate campaign status
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickNewCamHomePageBtn();
		String camStatus = pageObj.newCamHomePage().getAnyCampaignStatusWithPoolingNCHP(massCampaignName, "Processed");
		Assert.assertEquals(camStatus, "Processed", "Campaign status is not processed");
		TestListeners.extentTest.get().pass("Mass offer campaign processed successfully");

		// user email
		Response loginResponse = pageObj.endpoints().Api2Login(userEmail, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(loginResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token = loginResponse.jsonPath().get("access_token.token").toString();
		// fectch campaign name and reward gifted in account history user email1
		String campaignName = pageObj.guestTimelinePage().getCampaignNameByNotificationsAPILongPolling(massCampaignName,
				dataSet.get("client"), dataSet.get("secret"), token);
		String rewardGiftedAccountHistory = pageObj.guestTimelinePage().getUserAccountHistory(massCampaignName,
				dataSet.get("client"), dataSet.get("secret"), token);
		Assert.assertTrue(campaignName.contains(massCampaignName), "Campaign name did not matched");
		Assert.assertTrue(rewardGiftedAccountHistory.contains(massCampaignName),
				"Gifted redeemable did not appeared in account history");

		// validate email received
		String emailBody = GmailConnection.getGmailEmailBody("subject", emailSubject, true);
		Assert.assertTrue(emailBody.contains(massCampaignName),
				"User did mot receive email from Split Mass Offer Campaign");
		utils.logPass("Verified user received email from Split Mass Offer Campaign");

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
