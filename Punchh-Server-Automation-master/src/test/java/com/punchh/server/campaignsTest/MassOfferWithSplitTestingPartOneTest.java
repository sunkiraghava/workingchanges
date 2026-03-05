package com.punchh.server.campaignsTest;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

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
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Test
@Listeners(TestListeners.class)
public class MassOfferWithSplitTestingPartOneTest {
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
		utils.logit(sTCName + " ==>" + dataSet);
		// Instance move to all business page
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
	}

	@Test(description = "SQ-T1165 Create split Testing mass offer campaign", groups = { "regression" }, priority = 0)
	@Owner(name = "Amit Kumar")
	public void TC_1165_SplitPanelVerificationMassOfferCampaign() throws Exception {

		String massCampaignName = "AutomationMassOffer_" + CreateDateTime.getTimeDateString();
		String dateTime = CreateDateTime.getCurrentDate() + " 11:00 PM";

		userEmail = dataSet.get("userEmail");
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.dashboardpage().navigateToTabs("Miscellaneous Config");
		pageObj.dashboardpage().checkUncheckAnyFlag("Enable to show the list of all redeemables including recurrence",
				"uncheck");
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

		pageObj.campaignsplitpage().enterRedeemableNameVarAOfferField(dataSet.get("recurringRedeemable"));
		boolean isNoMatchFoundInOfferVisible = pageObj.campaignsplitpage().visibilityOfNoMatchFoundInOfferDrpDwn();
		Assert.assertTrue(isNoMatchFoundInOfferVisible, "No match found for var A is not visible on Whom Page");
		pageObj.campaignsplitpage().clickOnExpandLessOfOfferDrpDwn();

		pageObj.campaignsplitpage().selectOfferOfVarB();
		pageObj.campaignsplitpage().selectRedeemableFromVarBOfferField(dataSet.get("recurringRedeemable"));
		boolean isNoMatchFoundVarBInOfferVisible = pageObj.campaignsplitpage().visibilityOfNoMatchFoundInOfferDrpDwn();
		Assert.assertTrue(isNoMatchFoundVarBInOfferVisible, "No match found for var B is not visible on Whom Page");

		pageObj.campaignsplitpage().splitCancelButton();
		pageObj.campaignsplitpage().splitYesCancelButton();

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().navigateToTabs("Miscellaneous Config");
		pageObj.dashboardpage().checkUncheckAnyFlag("Enable to show the list of all redeemables including recurrence",
				"check");

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		pageObj.newCamHomePage().clickNewCamHomePageBtn();
		pageObj.newCamHomePage().searchCampaign(massCampaignName);
		pageObj.newCamHomePage().clickOptionFromDotsDropDown(dataSet.get("editOption"));

		pageObj.signupcampaignpage().createWhatDetailsMassCampaign(massCampaignName, dataSet.get("giftType"),
				dataSet.get("redeemableName"));

		pageObj.signupcampaignpage().setSegmentType(dataSet.get("segment"));

		pageObj.campaignsplitpage().clickOnSplitButton();

		pageObj.campaignsplitpage().variantA_Audience(dataSet.get("varApercent"));
		pageObj.campaignsplitpage().variantB_Audience(dataSet.get("varBpercent"));
		pageObj.campaignsplitpage().controlgroup_Audience(dataSet.get("controlgrouppercent"));

		pageObj.campaignsplitpage().clickOnNextButton();

		pageObj.campaignsplitpage().enterRedeemableNameVarAOfferField(dataSet.get("recurringRedeemable"));
		boolean isNoMatchFoundInOfferVisible1 = pageObj.campaignsplitpage().visibilityOfNoMatchFoundInOfferDrpDwn();
		Assert.assertTrue(isNoMatchFoundInOfferVisible1, "No match found for var A is not visible on Whom Page");
		pageObj.campaignsplitpage().clickOnExpandLessOfOfferDrpDwn();

		pageObj.campaignsplitpage().selectOfferOfVarB();
		pageObj.campaignsplitpage().selectRedeemableFromVarBOfferField(dataSet.get("recurringRedeemable"));
		boolean isNoMatchFoundVarBInOfferVisible1 = pageObj.campaignsplitpage().visibilityOfNoMatchFoundInOfferDrpDwn();
		Assert.assertTrue(isNoMatchFoundVarBInOfferVisible1, "No match found for var B is not visible on Whom Page");

		utils.logPass("Recurring redeemable is not visible in offer dropdown for Var A and Var B");

		pageObj.campaignsplitpage().closeSplitDialog("cancelButton");
		pageObj.campaignsplitpage().clickOnSplitButton();

		pageObj.campaignsplitpage().variantA_Audience(dataSet.get("varApercent"));
		pageObj.campaignsplitpage().variantB_Audience(dataSet.get("varBpercent"));
		pageObj.campaignsplitpage().controlgroup_Audience(dataSet.get("controlgrouppercent"));

		pageObj.campaignsplitpage().clickOnNextButton();

		// Add email, PN, SMS and RM in both the variant
		pageObj.campaignsplitpage().selectAndAddEmailOfVarA(dataSet.get("varAEmail") + " " + massCampaignName + " "
				+ "{{{business_name}}}" + "and reward name as" + "{{{reward_name}}}");
		pageObj.campaignsplitpage().clickOnSaveButton();
		pageObj.campaignsplitpage().selectAndAddPNOfVarA(dataSet.get("varAPushNoti") + " " + massCampaignName + " "
				+ "{{{reward_name}}}" + "and reward id as" + "{{{reward_id}}}");
		pageObj.campaignsplitpage().clickOnSaveButton();
		pageObj.campaignsplitpage().selectAndAddSMSOfVarA(dataSet.get("varASMS") + " " + massCampaignName);
		pageObj.campaignsplitpage().clickOnSaveButton();
		pageObj.campaignsplitpage().selectAndAddRMOfVarA(dataSet.get("varARichMsg") + " " + massCampaignName + " "
				+ "{{{first_name}}}" + "and reward id as" + "{{{reward_end_date}}}");
		pageObj.campaignsplitpage().clickOnSaveButton();
		pageObj.campaignsplitpage().selectAndAddEmailOfVarB(dataSet.get("varBEmail") + " " + massCampaignName + " "
				+ "{{last_name}}}" + "and user id as" + "{{{user_id}}}");
		pageObj.campaignsplitpage().clickOnSaveButton();
		pageObj.campaignsplitpage().selectAndAddPNOfVarB(dataSet.get("varBPushNoti") + " " + massCampaignName + " "
				+ "{{{user_name}}}" + "and reward id as" + "{{{gift_reason}}}");
		pageObj.campaignsplitpage().clickOnSaveButton();
		pageObj.campaignsplitpage().selectAndAddSMSOfVarB(dataSet.get("varBSMS") + " " + massCampaignName);
		pageObj.campaignsplitpage().clickOnSaveButton();
		pageObj.campaignsplitpage().selectAndAddRMOfVarB(dataSet.get("varBRichMsg") + " " + massCampaignName + " "
				+ "{{{business_name}}}" + "and reward id as" + "{{{reward_id}}}");
		pageObj.campaignsplitpage().clickOnSaveButton();

		pageObj.campaignsplitpage().clickOnNextButton();
		pageObj.campaignsplitpage().clickOnSaveButton();
		Thread.sleep(2000);

		pageObj.campaignsplitpage().clickOnRemoveButton();
		pageObj.campaignsplitpage().clickOnYesRemoveButton();
		String splitButtonText = pageObj.campaignsplitpage().getSplitButtonText();

		Assert.assertTrue(splitButtonText.equalsIgnoreCase("Set up splits and messaging"),
				"Set up splits and messaging text didn't appear");
		utils.logPass("Remove Button feature validated successfully");

		pageObj.campaignsplitpage().clickOnSplitButton();

		pageObj.campaignsplitpage().variantA_Audience(dataSet.get("varApercent"));
		pageObj.campaignsplitpage().variantB_Audience(dataSet.get("varBpercent"));
		pageObj.campaignsplitpage().controlgroup_Audience(dataSet.get("controlgrouppercent"));

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

		// click on next button on whom page
		pageObj.signupcampaignpage().clickNextBtn();
		pageObj.campaignsplitpage().clickOnSaveButton();

		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.campaignsplitpage().setStartTimesplitmasscampaign(dateTime);
		pageObj.signupcampaignpage().scheduleCampaign();
		Thread.sleep(3000);
		// pageObj.signupcampaignpage().createWhenDetailsMassCampaign("Once", dateTime);
		// boolean status = pageObj.campaignspage().validateSuccessMessage();
		// Assert.assertTrue(status, "Schedule created successfully Success message did
		// not displayed....");

		// check the archival status
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickNewCamHomePageBtn();
		pageObj.newCamHomePage().searchCampaign(massCampaignName);
		boolean flag = pageObj.newCamHomePage().checkOptionPresent(dataSet.get("option"));
		Assert.assertFalse(flag, "Mass Offer split campaign is in scheduled state but the Archive option is present");
		utils.logPass(
				"Verfied when mass Offer split campaign is in scheduled state as expected archieve option is not present");

		// Verify Edit split button feature
		pageObj.utils().refreshPage();
		pageObj.newCamHomePage().searchCampaign(massCampaignName);
		pageObj.newCamHomePage().clickOptionFromDotsDropDown(dataSet.get("editOption"));

		// Click on next button to navigate on Whom page
		pageObj.signupcampaignpage().clickNextButton();

		pageObj.campaignsplitpage().clickOnSplitButton();

		pageObj.campaignsplitpage().addPercentInVarAandVarBandControlgroup(dataSet.get("updatedVarApercent"),
				dataSet.get("updatedVarBpercent"), dataSet.get("updatedControlgrouppercent"));

		pageObj.campaignsplitpage().clickOnNextButton();

		pageObj.campaignsplitpage().setRedeemableVarAOfferField(dataSet.get("updatedRedeemableNameVarA"));

		// Add email, PN, SMS and RM in variant A
		pageObj.campaignsplitpage().editEmailOfVarA(massCampaignName);
		pageObj.campaignsplitpage().clickOnSaveButton();
		pageObj.campaignsplitpage().editPNOfVarA(massCampaignName);
		pageObj.campaignsplitpage().clickOnSaveButton();
		pageObj.campaignsplitpage().editSMSOfVarA(massCampaignName);
		pageObj.campaignsplitpage().clickOnSaveButton();
		pageObj.campaignsplitpage().editRMOfVarA(massCampaignName);
		pageObj.campaignsplitpage().clickOnSaveButton();

		// set redeemable of Var B
		pageObj.campaignsplitpage().selectOfferOfVarB();
		pageObj.campaignsplitpage().setRedeemableVarBOfferField(dataSet.get("updatedRedeemableNameVarB"));

		// Add email, PN, SMS and RM in variant B
		pageObj.campaignsplitpage().editEmailOfVarB(massCampaignName);
		pageObj.campaignsplitpage().clickOnSaveButton();
		pageObj.campaignsplitpage().editPNOfVarB(massCampaignName);
		pageObj.campaignsplitpage().clickOnSaveButton();
		pageObj.campaignsplitpage().editSMSOfVarB(massCampaignName);
		pageObj.campaignsplitpage().clickOnSaveButton();
		pageObj.campaignsplitpage().editRMOfVarB(massCampaignName);
		pageObj.campaignsplitpage().clickOnSaveButton();

		pageObj.campaignsplitpage().clickOnNextButton();
		pageObj.campaignsplitpage().clickOnSaveButton();

		// click on next button on whom page
		pageObj.signupcampaignpage().clickNextBtn();

		// disable Segmentation via s3 flow
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Campaigns");
		pageObj.dashboardpage().checkUncheckAnyFlag("Segmentation via s3 flow", "uncheck");

		// run mass offer
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().findMassCampaignNameandRun(massCampaignName);

		Response loginResponse = pageObj.endpoints().Api2Login(userEmail, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(loginResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token = loginResponse.jsonPath().get("access_token.token").toString();
		String campaignName = pageObj.guestTimelinePage().getCampaignNameByNotificationsAPILongPolling(massCampaignName,
				dataSet.get("client"), dataSet.get("secret"), token);
		String rewardGiftedAccountHistory = pageObj.guestTimelinePage().getUserAccountHistory(massCampaignName,
				dataSet.get("client"), dataSet.get("secret"), token);

		Assert.assertTrue(campaignName.equalsIgnoreCase(massCampaignName), "Campaign name did not matched");
		Assert.assertTrue(rewardGiftedAccountHistory.contains(massCampaignName),
				"Gifted redeemable did not appeared in account history");

		utils.logPass(
				"Mass offer with split test detail: push notification, campaign name, redeemable validated successfully on timeline");
	}

	// Shubham Kumar Gupta
	@Test(description = "SQ-T6137 Verify whether parent scheduled campaign CSP is opening or not when var campaign "
			+ "passes in URL from superadmin page"
			+ "SQ-T6139 Verify whether split button is enable/disable when segment is "
			+ "selected on whom page", groups = { "regression", "nonNightly" }, priority = 4)
	@Owner(name = "Shubham Gupta")
	public void T16137_VerifyScheduledParentCampaignCSPWhenChildCamIdInURL() throws Exception {

		String massCampaignName = CreateDateTime.getUniqueString("Automation Mass Campaign with Split Test");
		utils.logit("Campaign name is :" + massCampaignName);
		String dateTime = CreateDateTime.getCurrentDate() + " 11:00 PM";

		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Campaigns");

		pageObj.dashboardpage().checkBoxFlagOnOffAndUpdate("business_enable_split_testing", "check", true);
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickSwitchToClassicBtn(); // Select offer dropdownvalue
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
				dataSet.get("varBpercent"), dataSet.get("controlgrouppercent")); // Click on save button
		pageObj.campaignsplitpage().clickOnNextButton();

		// Add email, PN, SMS and RM in variant A
		pageObj.campaignsplitpage().selectAndAddEmailOfVarA(dataSet.get("varAEmail") + " " + massCampaignName);
		pageObj.campaignsplitpage().clickOnSaveButton();
		pageObj.campaignsplitpage().selectAndAddPNOfVarA(dataSet.get("varAPushNoti") + " " + massCampaignName);
		pageObj.campaignsplitpage().clickOnSaveButton();
		pageObj.campaignsplitpage().selectAndAddSMSOfVarA(dataSet.get("varASMS") + " " + massCampaignName);
		pageObj.campaignsplitpage().clickOnSaveButton();
		pageObj.campaignsplitpage().selectAndAddRMOfVarA(dataSet.get("varARichMsg") + " " + massCampaignName);
		pageObj.campaignsplitpage().clickOnSaveButton();
		// set redeemable in var B
		pageObj.campaignsplitpage().selectOfferOfVarB();
		pageObj.campaignsplitpage().setRedeemableVarBOfferField(dataSet.get("VarBoffer"));

		// Add email, PN, SMS and RM in variant B
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
		pageObj.newCamHomePage().clickNewCamHomePageBtn();
		pageObj.newCamHomePage().searchCampaign(massCampaignName);
		pageObj.newCamHomePage().viewCampaignSidePanel();

		String[] parentVarAandVarBCamId = pageObj.campaignsplitpage().getParentAndVarCamIdFromSidePanel();
		// parentVarAandVarBCamId[0]=parent cam id
		// parentVarAandVarBCamId[1]=var A cam id
		// parentVarAandVarBCamId[2]=var B cam id

		pageObj.newCamHomePage().closeCampaignSidePanel();

		// When Var A cam id is entered in URL
		// navigate to all businesses page
		pageObj.dashboardpage().navigateToAllBusinessPage();

		// open mass campaigns on superadmin page
		pageObj.menupage().navigateToSubMenuItem("SRE", "Mass Campaigns");
		int counter = pageObj.menupage().getMassCampaignPageNo(massCampaignName);
		// get campaigns from superadmin scheduled page

		utils.logit("Campaign is present in superadmin scheduled page at page no.: " + counter);
		String superadminURL = pageObj.utils().getCurrentURL() + "/scheduled?page=" + counter;
		pageObj.instanceDashboardPage().navigateToPunchhInstance(superadminURL);

		pageObj.superadminMassCampaignPage().selectCamapign(massCampaignName);
		String campaignURL = pageObj.superadminMassCampaignPage().getCampaignURL();
		String varACampaignURL = campaignURL.replace(parentVarAandVarBCamId[0], parentVarAandVarBCamId[1]);
		pageObj.superadminMassCampaignPage().openCampaignURL(varACampaignURL);
		String updatedCampaignURLFromVarA = pageObj.superadminMassCampaignPage().getCampaignURL();
		String camIdsFromCSP = pageObj.campaignsplitpage().getCamIdsFromCSP();
		Assert.assertTrue(updatedCampaignURLFromVarA.contains(parentVarAandVarBCamId[0]),
				"After entering var A cam id in URL it didn't redirect to parent campaign");
		Assert.assertTrue(camIdsFromCSP.contains(parentVarAandVarBCamId[0]),
				"After entering var A cam id in URL it didn't redirect to parent campaign");
		Assert.assertTrue(camIdsFromCSP.contains(parentVarAandVarBCamId[1]),
				"After entering var A cam id in URL it didn't redirect to parent campaign");
		Assert.assertTrue(camIdsFromCSP.contains(parentVarAandVarBCamId[2]),
				"After entering var A cam id in URL it didn't redirect to parent campaign");

		// When Var B cam id is entered in URL
		// navigate to all businesses page
		pageObj.dashboardpage().navigateToAllBusinessPage();

		// open mass campaigns on superadmin page
		pageObj.menupage().navigateToSubMenuItem("SRE", "Mass Campaigns");
		pageObj.instanceDashboardPage().navigateToPunchhInstance(superadminURL);

		pageObj.superadminMassCampaignPage().selectCamapign(massCampaignName);
		String campaignURL1 = pageObj.superadminMassCampaignPage().getCampaignURL();
		String varBCampaignURL = campaignURL1.replace(parentVarAandVarBCamId[0], parentVarAandVarBCamId[2]);
		pageObj.superadminMassCampaignPage().openCampaignURL(varBCampaignURL);
		String updatedCampaignURLFromVarB = pageObj.superadminMassCampaignPage().getCampaignURL();
		String camIdsFromCSP1 = pageObj.campaignsplitpage().getCamIdsFromCSP();
		Assert.assertTrue(updatedCampaignURLFromVarB.contains(parentVarAandVarBCamId[0]),
				"After entering var B cam id in URL it didn't redirect to parent campaign");
		Assert.assertTrue(camIdsFromCSP1.contains(parentVarAandVarBCamId[0]),
				"After entering var B cam id in URL it didn't redirect to parent campaign");
		Assert.assertTrue(camIdsFromCSP1.contains(parentVarAandVarBCamId[1]),
				"After entering var B cam id in URL it didn't redirect to parent campaign");
		Assert.assertTrue(camIdsFromCSP1.contains(parentVarAandVarBCamId[2]),
				"After entering var B cam id in URL it didn't redirect to parent campaign");

		utils.logPass("T1314_VerifyParentCampaignCSPWhenChildCamIdInURL feature validated successfully");
	}

	// Shubham Kumar Gupta
	@Test(description = "SQ-T6140 Verify whether parent processed campaign CSP is opening or not when var "
			+ "campaign passes in URL from superadmin page"
			+ "SQ-T6420 Verify mass offer split campaign summary page when campaign is created and scheduled.", groups = {
					"regression", "nonNightly" }, priority = 5)
	@Owner(name = "Shubham Gupta")
	public void T16140_VerifyProcessedParentCampaignCSPWhenChildCamIdInURL() throws Exception {

		String massCampaignName = CreateDateTime.getUniqueString("Automation Mass Campaign with Split Test");
		utils.logit("Campaign name is :" + massCampaignName);
		String dateTime = CreateDateTime.getCurrentDate() + " 11:00 PM";

		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Campaigns");

		pageObj.dashboardpage().checkBoxFlagOnOffAndUpdate("business_enable_split_testing", "check", true);
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickSwitchToClassicBtn();
		// Select offer dropdownvalue
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

		// Add email, PN, SMS and RM in variant A
		pageObj.campaignsplitpage().selectAndAddEmailOfVarA(dataSet.get("varAEmail") + " " + massCampaignName);
		pageObj.campaignsplitpage().clickOnSaveButton();
		pageObj.campaignsplitpage().selectAndAddPNOfVarA(dataSet.get("varAPushNoti") + " " + massCampaignName);
		pageObj.campaignsplitpage().clickOnSaveButton();
		pageObj.campaignsplitpage().selectAndAddSMSOfVarA(dataSet.get("varASMS") + " " + massCampaignName);
		pageObj.campaignsplitpage().clickOnSaveButton();
		pageObj.campaignsplitpage().selectAndAddRMOfVarA(dataSet.get("varARichMsg") + " " + massCampaignName);
		pageObj.campaignsplitpage().clickOnSaveButton();
		// set redeemable in var B
		pageObj.campaignsplitpage().selectOfferOfVarB();
		pageObj.campaignsplitpage().setRedeemableVarBOfferField(dataSet.get("VarBoffer"));

		// Add email, PN, SMS and RM in variant B
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

		pageObj.newCamHomePage().clickNewCamHomePageBtn();
		pageObj.newCamHomePage().searchCampaign(massCampaignName);
		pageObj.newCamHomePage().viewCampaignSidePanel();

		String[] parentVarAandVarBCamId = pageObj.campaignsplitpage().getParentAndVarCamIdFromSidePanel();
		// parentVarAandVarBCamId[0]=parent cam id
		// parentVarAandVarBCamId[1]=var A cam id
		// parentVarAandVarBCamId[2]=var B cam id

		pageObj.newCamHomePage().closeCampaignSidePanel();

		// <---SQ-T6420 start--->
		pageObj.newCamHomePage().viewCampaignSummary();

		String elementsText = pageObj.campaignsplitpage().getAllElementsTextOnSplitCamSummaryPage();
		Assert.assertTrue(elementsText.contains("Parent: " + parentVarAandVarBCamId[0]),
				"Parent Campaign ID is not present on split campaign summary page");
		Assert.assertTrue(elementsText.contains("Variant A: " + parentVarAandVarBCamId[1]),
				"Variant A Campaign ID is not present on split campaign summary page");
		Assert.assertTrue(elementsText.contains("Variant B: " + parentVarAandVarBCamId[2]),
				"Variant B Campaign ID is not present on split campaign summary page");
		Assert.assertTrue(elementsText.contains(dataSet.get("varAEmail") + " " + massCampaignName),
				"Variant A email is not present on split campaign summary page");
		Assert.assertTrue(elementsText.contains(dataSet.get("varAPushNoti") + " " + massCampaignName),
				"Variant A PN is not present on split campaign summary page");
		Assert.assertTrue(elementsText.contains(dataSet.get("varARichMsg") + " " + massCampaignName),
				"Variant A RM is not present on split campaign summary page");
		Assert.assertTrue(elementsText.contains(dataSet.get("varBEmail") + " " + massCampaignName),
				"Variant B email is not present on split campaign summary page");
		Assert.assertTrue(elementsText.contains(dataSet.get("varBPushNoti") + " " + massCampaignName),
				"Variant B PN is not present on split campaign summary page");
		Assert.assertTrue(elementsText.contains(dataSet.get("varBRichMsg") + " " + massCampaignName),
				"Variant B RM is not present on split campaign summary page");
		utils.logPass("Split Campaign Summary page elements validated successfully");
		// <---SQ-T6420 end--->

		// run mass offer
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().findMassCampaignNameandRun(massCampaignName);

		// navigate to all businesses page
		pageObj.dashboardpage().navigateToAllBusinessPage();

		// open mass campaigns on superadmin page
		pageObj.menupage().navigateToSubMenuItem("SRE", "Mass Campaigns");
		pageObj.superadminMassCampaignPage().selectTab("Finished");

		pageObj.superadminMassCampaignPage().selectCamapign(massCampaignName);
		String campaignURLOfProcessedCamp = pageObj.superadminMassCampaignPage().getCampaignURL();// https://shubhamgupta.punchh.io/user_campaigns/1885?mode=new&type=mass_gifting_campaigns
		String varACampaignURLOfProcessedCamp = campaignURLOfProcessedCamp.replace(parentVarAandVarBCamId[0],
				parentVarAandVarBCamId[1]);// https://shubhamgupta.punchh.io/user_campaigns/1886?mode=new&type=mass_gifting_campaigns
		pageObj.superadminMassCampaignPage().openCampaignURL(varACampaignURLOfProcessedCamp);
		String updatedCampaignURLFromVarAOfProcessedCamp = pageObj.superadminMassCampaignPage().getCampaignURL();// https://shubhamgupta.punchh.io/user_campaigns/1885?mode=new&type=mass_gifting_campaigns
		String camNameFromCSPOfProcessedCamp = pageObj.campaignsplitpage().getCamNameFromCSP();

		Assert.assertTrue(updatedCampaignURLFromVarAOfProcessedCamp.contains(parentVarAandVarBCamId[0]),
				"After entering var A cam id in URL it didn't redirect to parent processed campaign");
		Assert.assertTrue(camNameFromCSPOfProcessedCamp.contains(massCampaignName),
				"After entering var A cam id in URL it didn't redirect to parent processed campaign");

		// checked for Var B
		// navigate to all businesses page
		pageObj.dashboardpage().navigateToAllBusinessPage();

		// open mass campaigns on superadmin page
		pageObj.menupage().navigateToSubMenuItem("SRE", "Mass Campaigns");
		pageObj.superadminMassCampaignPage().selectTab("Finished");
		pageObj.superadminMassCampaignPage().selectCamapign(massCampaignName);
		String campaignURLOfProcessedCamp1 = pageObj.superadminMassCampaignPage().getCampaignURL();
		String varBCampaignURLOfProcessedCamp = campaignURLOfProcessedCamp1.replace(parentVarAandVarBCamId[0],
				parentVarAandVarBCamId[2]);
		pageObj.superadminMassCampaignPage().openCampaignURL(varBCampaignURLOfProcessedCamp);
		String updatedCampaignURLFromVarBOfProcessedCamp1 = pageObj.superadminMassCampaignPage().getCampaignURL();
		Thread.sleep(2000);
		String camNameFromCSPOfProcessedCamp1 = pageObj.campaignsplitpage().getCamNameFromCSP();

		Assert.assertTrue(updatedCampaignURLFromVarBOfProcessedCamp1.contains(parentVarAandVarBCamId[0]),
				"After entering var B cam id in URL it didn't redirect to parent processed campaign");
		Assert.assertTrue(camNameFromCSPOfProcessedCamp1.contains(massCampaignName),
				"After entering var B cam id in URL it didn't redirect to parent processed campaign");

		utils.logPass("T1315_VerifyParentCampaignCSPWhenChildCamIdInURL feature validated successfully");
	}

	// Jeevraj
	@Test(description = "SQ-T6141 Verify the Live preview section on campaign summary page for mass offer split campaign having Scheduled status"
			+ "SQ-T6301 Verify that user is able to copy comm channel content of Variant A in mass offer campaign with split test for Variant B configuration.", groups = {
					"regression" }, priority = 6)
	@Owner(name = "Jeevraj")
	public void T6141_VerifySplitCampaignLivePreviewSectionSummaryPage() throws Exception {

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

		// Duplicate content of Varaint A communication channel data and copy to Variant
		// B channels data
		pageObj.campaignsplitpage().splitDuplicateContentFromVarAToVarB("Variant B", "Email");
		utils.logit("Duplicate content from Variant A Email was successfull");
		pageObj.campaignsplitpage().splitDuplicateContentFromVarAToVarB("Variant B", "Push notification");
		utils.logit("Duplicate content from Variant A PN was successfull");
		pageObj.campaignsplitpage().splitDuplicateContentFromVarAToVarB("Variant B", "SMS");
		utils.logit("Duplicate content from Variant A SMS was successfull");
		pageObj.campaignsplitpage().splitDuplicateContentFromVarAToVarB("Variant B", "Rich message");
		utils.logit("Duplicate content from Variant A Rich message was successfull");

		// Get all Variant B section communication channel data copied and validate from
		// Variant A data
		List<String> VariantBCommChannelData = pageObj.campaignsplitpage()
				.validateDataUsingDuplicateFromVariantAOption();
		Assert.assertTrue(VariantBCommChannelData.get(0).contains(dataSet.get("varAEmail")),
				"Copy of Email data from Variant A failed");
		Assert.assertTrue(VariantBCommChannelData.get(0).contains(dataSet.get("varAPushNoti")),
				"Copy of PN data from Variant A failed");
		Assert.assertTrue(VariantBCommChannelData.get(0).contains(dataSet.get("varASMS")),
				"Copy of SMS data from Variant A failed");
		Assert.assertTrue(VariantBCommChannelData.get(0).contains(dataSet.get("varARichMsg")),
				"Copy of Rich message data from Variant A failed");
		utils.logPass("Copy of Variant A section communication channel data was successfull");

		pageObj.campaignsplitpage().clickOnNextButton();
		pageObj.campaignsplitpage().clickOnSaveButton();
		Thread.sleep(2000);

		// Set split campaign to Draft mode and check View Summary for Live preview
		// section
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickNewCamHomePageBtn();
		pageObj.newCamHomePage().searchCampaign(massCampaignName);
		pageObj.newCamHomePage().viewCampaignSummary();
		boolean variantASectionVisibleInDraftMode = pageObj.campaignsplitpage()
				.validateLivePreviewVariantASectionOnSummaryPage();
		boolean variantBSectionVisibleInDraftMode = pageObj.campaignsplitpage()
				.validateLivePreviewVariantBSectionOnSummaryPage();
		boolean livePreviewSectionVisibleInDraftMode = pageObj.campaignsplitpage()
				.validateLivePreviewSectionVisibleOnSummaryPage();
		Assert.assertTrue(variantASectionVisibleInDraftMode,
				"Variant A section on Summary page for live preview did not displayed....");
		Assert.assertTrue(variantBSectionVisibleInDraftMode,
				"Variant B section on Summary page for live preview did not displayed....");
		Assert.assertTrue(livePreviewSectionVisibleInDraftMode,
				"Live Preview section on Summary page for live preview did not displayed....");

		// Edit split campaign and Schedule the mass split campaign
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().searchCampaign(massCampaignName);
		pageObj.newCamHomePage().clickOptionFromDotsDropDown(dataSet.get("Editoption"));
		pageObj.signupcampaignpage().clickNextButton();
		pageObj.signupcampaignpage().clickNextButton();
		pageObj.campaignsplitpage().setStartTimesplitmasscampaign(dateTime);
		pageObj.signupcampaignpage().scheduleCampaign();
		utils.waitTillNewCamsTableAppear();

		// Search campaign and click View summary option
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		utils.waitTillPagePaceDone();
		pageObj.newCamHomePage().searchCampaign(massCampaignName);
		pageObj.newCamHomePage().clickOptionFromDotsDropDown(dataSet.get("option"));

		// Assert to validate the Live preview section on summary page for Split
		// Campaign in Scheduled mode
		boolean variantASectionVisibleInScheduledMode = pageObj.campaignsplitpage()
				.validateLivePreviewVariantASectionOnSummaryPage();
		boolean variantBSectionVisibleInScheduledMode = pageObj.campaignsplitpage()
				.validateLivePreviewVariantBSectionOnSummaryPage();
		boolean livePreviewSectionVisibleInScheduledMode = pageObj.campaignsplitpage()
				.validateLivePreviewSectionVisibleOnSummaryPage();
		Assert.assertTrue(variantASectionVisibleInScheduledMode,
				"Variant A section on Summary page for live preview did not displayed....");
		Assert.assertTrue(variantBSectionVisibleInScheduledMode,
				"Variant B section on Summary page for live preview did not displayed....");
		Assert.assertTrue(livePreviewSectionVisibleInScheduledMode,
				"Live Preview section on Summary page for live preview did not displayed....");
		utils.logPass(
				"Live Preview section validated successfully for Split Mass Scheduled and Draft campaign summary page");

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() throws Exception {
		pageObj.utils().clearDataSet(dataSet);
		utils.logit("Data set cleared");
	}

	@AfterClass(alwaysRun = true)
	public void closeBrowser() {
		driver.quit();
		utils.logit("Browser closed");
	}

}
