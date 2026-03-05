package com.punchh.server.segmentsTest;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.punchh.server.annotations.Owner;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class MassCampaignAndSegmentTest {
	public WebDriver driver;
	private Properties prop;
	private PageObj pageObj;
	private String sTCName;
	private String env;
	private String baseUrl;
	private Utilities utils;
	private SeleniumUtilities selUtils;
	private static Map<String, String> dataSet;
	String run = "ui";
	private String userEmail, segmentName, massCampaignName, scheduleName, audiance, emailSubject, emailTemplate,
			OfferdrpValue;
	private int count = 0;
	private String dateTime = CreateDateTime.getTomorrowDate() + " 11:00 PM";
	private static JsonNode segmentBuilderClauses;

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
	public void setUp(Method method) throws JsonMappingException, JsonProcessingException, IOException {

		prop = Utilities.loadPropertiesFile("config.properties");
		sTCName = method.getName();
		utils = new Utilities(driver);
		selUtils = new SeleniumUtilities(driver);
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		pageObj.utils().logit(sTCName + " ==>" + dataSet);
		scheduleName = "Mass Campaign Schedules";
		audiance = "Segment";
		emailSubject = "Automation Mass Email Subject";
		emailTemplate = "Automation Mass Email Template";
		OfferdrpValue = "Mass Offer";
		ObjectMapper mapper = new ObjectMapper();
		segmentBuilderClauses = mapper.readTree(Files
				.readString(Paths.get(System.getProperty("user.dir") + prop.getProperty("segmentBuilderClauseJson"))));
	}

	@SuppressWarnings("static-access")
	@Test(description = "SQ-T6397: verify segment count when Create Profile details segment with Birthday Attribute and Select Duration 'Between' with value 'Specific date' ||"
			+ "SQ-T3431 Mass Campaign Trigger For User When Gift type is Currency", groups = {
					"regression" }, priority = 3)
	@Owner(name = "Vaibhav Agnihotri")
	public void T6397_birthdayBetweenSpecificDateSegment() throws Exception {
//		T3431_verifyMassCampaignTriggerForUserWhenGifttypeIsCurrency
		segmentName = CreateDateTime.getUniqueString("BirthdayBetweenSegment_");
		String exportName = "Export_" + segmentName;
//		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
//		pageObj.instanceDashboardPage().loginToInstance();

		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		pageObj.newSegmentHomePage().clickOnSwitchToClassicBtn();
		pageObj.segmentsBetaPage().setSegmentBetaName(segmentName);
		pageObj.segmentsBetaPage().selectSegmentType("Profile Details");
		pageObj.segmentsBetaPage().selectAttribute("Birthday");
		pageObj.segmentsBetaPage().setOperator("User relation", "Self");
		pageObj.segmentsBetaPage().setDuration("Between");

		// Verify count and guest in segment
		int segmentCount = pageObj.segmentsBetaPage().getSegmentSizeBeforeSave();
		Assert.assertTrue(segmentCount > 0, "Segment count is not greater than 0 for segment: " + segmentName);
		// pageObj.segmentsBetaPage().saveSegment(segmentName);
		pageObj.segmentsPage().saveAndShowSegment();
		String segmentId = pageObj.segmentsPage().getSegmentID();
		int guestInSegmentCount = pageObj.segmentsBetaPage().getGuestsInSegmentCount();
		Assert.assertTrue(guestInSegmentCount > 0,
				"Guest in segment count is not greater than 0 for segment: " + segmentName);
		userEmail = pageObj.segmentsBetaPage().getSegmentGuestList(dataSet.get("apiKey"), segmentId);
		utils.logPass(
				"Verified that Profile Details segment with Birthday between specific duration is created. Guest count: "
						+ guestInSegmentCount + ". In-segment for user email: " + userEmail);

		// Run a mass campaign with the saved segment
		String segmentOverviewWindow = driver.getWindowHandle();
		pageObj.segmentsBetaPage().segmentEllipsisOptions("Create Mass Gifting");
		utils.switchToWindow();
		massCampaignName = "MassCampaign_" + segmentName;
//		String pointsToGift = dataSet.get("pointsToGift");
		pageObj.signupcampaignpage().setCampaignName(massCampaignName);
		pageObj.signupcampaignpage().setGiftType(dataSet.get("giftType"));
		pageObj.signupcampaignpage().setGiftCurrency(dataSet.get("giftCurrency"));
		pageObj.signupcampaignpage().clickNextBtn();
		pageObj.signupcampaignpage().createWhomDetailsMassCampaign(audiance, segmentName, massCampaignName,
				emailSubject, emailTemplate);
		pageObj.signupcampaignpage().setFrequencyAndTimeInCampaign("Once", dateTime);
		pageObj.signupcampaignpage().scheduleCampaign();
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(scheduleName);
		pageObj.schedulespage().findMassCampaignNameandRun(massCampaignName);

		// Timeline validation
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		String campaignName = pageObj.guestTimelinePage().getcampaignNameWithWait(massCampaignName);
		Assert.assertTrue(campaignName.equalsIgnoreCase(massCampaignName), "Campaign name did not matched");
		boolean campaignNotificationStatus = pageObj.guestTimelinePage().verifyCampaignNotification();
		Assert.assertTrue(campaignNotificationStatus, "Campaign notification did not displayed...");
		utils.logPass("Mass Offer campaign with name " + massCampaignName
				+ " is found on user Timeline and Campaign Notification Status is :- " + campaignNotificationStatus);

		pageObj.guestTimelinePage().clickAccountHistory();
		boolean itemCountFlag = pageObj.accounthistoryPage().getAccountDetailsforRewardEarnedWithPooling(campaignName,
				1);
		Assert.assertTrue(itemCountFlag, "Failed to verify reward earned in account history");
		utils.logPass("Verified reward earned in account history for " + campaignName);

		// Verify segment export
		selUtils.switchToWindow(segmentOverviewWindow);
		pageObj.segmentsPage().segmentOverviewPageOptionList("Export");
		utils.switchToWindow();
		pageObj.schedulePage().segmentExportSchedule("OFF", exportName, "Immediately");
		boolean logsConfirmation = pageObj.schedulePage()
				.selectDataExportSchedule(prop.getProperty("segmentExportSchedule"), segmentName);
		Assert.assertTrue(logsConfirmation, "Failed to verify data export logs");
		utils.logPass("Verified data export logs for " + segmentName);

		// deleting segment
		String query = "DELETE FROM `segment_definitions` WHERE `id` = '" + segmentId + "';";
		DBUtils.executeQuery(env, query);
	}

	@SuppressWarnings("static-access")
	@Test(description = "SQ-T6310, SQ-T6311 verify external segment created successfully and count or in_segment query working properly || "
			+ "SQ-T3430 Mass Campaign Trigger For User When Gift type is Derived Reward", groups = {
					"regression" }, priority = 4)
	@Owner(name = "Sachin Bakshi")
	public void T6310_ExternalSegment() throws Exception {
		// T3430_verifyMassCampaignTriggerForUserWhenGifttypeIsDerivedReward

//		External Segment External Segment List is Loyalty Guests
		segmentName = CreateDateTime.getUniqueString("ExternalSegment_");
		String ExternalSegmentExternalSegmentListIsLoyaltyGuests_builder_clause = utils.convertToDoubleStringified(
				segmentBuilderClauses.get("ExternalSegmentExternalSegmentListIsLoyaltyGuests"));

		Response segmentCreationUsingBuilderClauseResponse = pageObj.endpoints().segmentCreationUsingBuilderClause(
				dataSet.get("apiKey"), segmentName, ExternalSegmentExternalSegmentListIsLoyaltyGuests_builder_clause);
		Assert.assertEquals(segmentCreationUsingBuilderClauseResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String segmentId = segmentCreationUsingBuilderClauseResponse.jsonPath().get("data.id").toString();

		Response segmentCountresponse = pageObj.endpoints().getSegmentCount(dataSet.get("apiKey"), segmentId);
		Assert.assertEquals(segmentCountresponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for Fetch segment count dashboard api");
		utils.logPass("Fetch segment count Dashboard API is Successful");
		String segmentCountMessage = segmentCountresponse.jsonPath().getString("message");
		Assert.assertEquals(segmentCountMessage, "Fetched Count Successfully");
		int segmentCount = Integer
				.parseInt((segmentCountresponse.jsonPath().get("count").toString()).replaceAll(",", ""));
		Assert.assertTrue(segmentCount > count, "Segment count is not greater than 0.");
		utils.logit("Segment count is greater than 0  ie " + segmentCount);

		// login to instance
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		pageObj.newSegmentHomePage().clickOnSwitchToClassicBtn();
		pageObj.segmentsPage().searchAndOpenSegment(segmentName, segmentId);
		pageObj.segmentsBetaPage().getGuestInSegmentCount();
		userEmail = pageObj.segmentsBetaPage().getSegmentGuestList(dataSet.get("apiKey"), segmentId);
		utils.logPass("Verified that External Segment is created. Guest count: "
				+ segmentCount + ". In-segment for user email: " + userEmail);

		// Run a mass campaign with the saved segment
		// (SQ-T6311: Run External segment Type with mass campaign)
		pageObj.segmentsBetaPage().segmentEllipsisOptions("Create Mass Gifting");
		utils.switchToWindow();
		massCampaignName = "MassCampaign_" + segmentName;

		// set campaign name and gift type
		pageObj.signupcampaignpage().setCampaignName(massCampaignName);
		pageObj.signupcampaignpage().setGiftType(dataSet.get("giftType"));
		pageObj.signupcampaignpage().setDerivedReward(dataSet.get("derivedReward"));
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().createWhomDetailsMassCampaign(audiance, segmentName, massCampaignName,
				emailSubject, emailTemplate);
		pageObj.signupcampaignpage().setFrequencyAndTimeInCampaign("Once", dateTime);

		// navigate to Menu -> submenu
		pageObj.signupcampaignpage().scheduleCampaign();
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(scheduleName);
		pageObj.schedulespage().findMassCampaignNameandRun(massCampaignName);

		// Timeline validation
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		String campaignName = pageObj.guestTimelinePage().getcampaignNameWithWait(massCampaignName);
		Assert.assertTrue(campaignName.equalsIgnoreCase(massCampaignName), "Campaign name did not matched");
		boolean campaignNotificationStatus = pageObj.guestTimelinePage().verifyCampaignNotification();
		Assert.assertTrue(campaignNotificationStatus, "Campaign notification did not displayed...");
		utils.logPass("Mass Offer campaign with name " + massCampaignName
				+ " is found on user Timeline and Campaign Notification Status is :- " + campaignNotificationStatus);

		pageObj.guestTimelinePage().clickAccountHistory();
		boolean itemCountFlag = pageObj.accounthistoryPage()
				.getAccountDetailsforBonusPointsEarnedWithPooling(campaignName, 1);
		Assert.assertTrue(itemCountFlag, "Failed to verify bonus points earned in account history");
		utils.logPass("Verified bonus points earned in account history for " + campaignName);

		// Verify segment export
		// (SQ-T6312 verify segment export working properly with external segment)
		utils.switchToParentWindow();
		pageObj.segmentsPage().segmentOverviewPageOptionList("Export");
		utils.switchToWindow();
		String exportName = "Export_" + segmentName;
		pageObj.segmentsPage().setSegmentExportName(segmentName);
		pageObj.schedulePage().segmentExportSchedule("OFF", exportName, "Immediately");
		boolean logsConfirmation = pageObj.schedulePage()
				.selectDataExportSchedule(prop.getProperty("segmentExportSchedule"), segmentName);
		Assert.assertTrue(logsConfirmation, "Failed to verify data export logs");
		utils.logPass("Verified data export logs for " + segmentName);

		// deleting segment
		String query = "DELETE FROM `segment_definitions` WHERE `id` = '" + segmentId + "';";
		DBUtils.executeQuery(env, query);
	}

	@Test(description = "SQ-T5753 Verify the functionality for the Engagement Segment type ", groups = { "regression",
			"dailyrun" })
	@Owner(name = "Hardik Bhardwaj")
	public void T5397_segmentTypeEngagement() throws Exception {
		String segmentName = CreateDateTime.getUniqueString("Engagement");
		String massCampaignName = "AutomationMassOffer_Engagement_" + CreateDateTime.getTimeDateString();
		String dateTime = CreateDateTime.getTomorrowDate() + " 11:00 PM";

		int count = 0;
		// login to instance
//		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
//		pageObj.instanceDashboardPage().loginToInstance();

		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// navigate to segments
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
//		pageObj.segmentsBetaPage().setSegmentBetaNameAndType(segmentName, "Engagement");
//
//		// The user selects "Engagement" segment type from the dropdown
//		List<String> actualAttributeNameList = pageObj.segmentsPage().getAttributeNameList();
//		Assert.assertEquals(actualAttributeNameList, dataSet.get("expectedAttributeNameList"),
//				"Failed to verify sub-attributes of Engagement Segment");
//		pageObj.utils().logit("Verfied sub-attributes of Engagement Segment");
//		pageObj.utils().logPass("Verfied sub-attributes of Engagement Segment");
//
//		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		pageObj.newSegmentHomePage().clickOnSwitchToClassicBtn();

//		Engagement Channel is SMS Opted-in
		segmentName = CreateDateTime.getUniqueString("Segment_EngagementChannelisSMSOptedin");
		String EngagementChannelisSMSOptedin_builder_clause = utils
				.convertToDoubleStringified(segmentBuilderClauses.get("EngagementChannelisSMSOptedin"));

		Response segmentCreationUsingBuilderClauseResponse = pageObj.endpoints().segmentCreationUsingBuilderClause(
				dataSet.get("apiKey"), segmentName, EngagementChannelisSMSOptedin_builder_clause);
		Assert.assertEquals(segmentCreationUsingBuilderClauseResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String segmentId = segmentCreationUsingBuilderClauseResponse.jsonPath().get("data.id").toString();

		Response segmentCountresponse = pageObj.endpoints().getSegmentCount(dataSet.get("apiKey"), segmentId);
		Assert.assertEquals(segmentCountresponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for Fetch segment count dashboard api");
		utils.logPass("Fetch segment count Dashboard API is Successful");
		String segmentCountMessage = segmentCountresponse.jsonPath().getString("message");
		Assert.assertEquals(segmentCountMessage, "Fetched Count Successfully");
		int segmentCount = Integer
				.parseInt((segmentCountresponse.jsonPath().get("count").toString()).replaceAll(",", ""));
		Assert.assertTrue(segmentCount >= count, "Segment count is not greater than 0.");
		utils.logit("Segment count is greater than 0  ie " + segmentCount);
		// deleting segment
		String query = "DELETE FROM `segment_definitions` WHERE `id` = '" + segmentId + "';";
		DBUtils.executeQuery(env, query);
		utils.logit("PASS", "Verfied guest count label appeard for channel as SMS and Engagement Metric as Opted-in");

//		Engagement Channel is Push Notification Opted-in
		segmentName = CreateDateTime.getUniqueString("Segment_EngagementChannelisPushNotificationOptedin");
		String EngagementChannelisPushNotificationOptedin_builder_clause = utils
				.convertToDoubleStringified(segmentBuilderClauses.get("EngagementChannelisPushNotificationOptedin"));

		Response segmentCreationUsingBuilderClauseResponse1 = pageObj.endpoints().segmentCreationUsingBuilderClause(
				dataSet.get("apiKey"), segmentName, EngagementChannelisPushNotificationOptedin_builder_clause);
		Assert.assertEquals(segmentCreationUsingBuilderClauseResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String segmentId1 = segmentCreationUsingBuilderClauseResponse1.jsonPath().get("data.id").toString();

		Response segmentCountresponse1 = pageObj.endpoints().getSegmentCount(dataSet.get("apiKey"), segmentId1);
		Assert.assertEquals(segmentCountresponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for Fetch segment count dashboard api");
		utils.logPass("Fetch segment count Dashboard API is Successful");
		String segmentCountMessage1 = segmentCountresponse1.jsonPath().getString("message");
		Assert.assertEquals(segmentCountMessage1, "Fetched Count Successfully");
		int segmentCount1 = Integer
				.parseInt((segmentCountresponse1.jsonPath().get("count").toString()).replaceAll(",", ""));
		Assert.assertTrue(segmentCount1 >= count, "Segment count is not greater than 0.");
		utils.logit("Segment count is greater than 0  ie " + segmentCount1);
		// deleting segment
		query = "DELETE FROM `segment_definitions` WHERE `id` = '" + segmentId1 + "';";
		DBUtils.executeQuery(env, query);
		utils.logit("PASS",
				"Verfied guest Count label appeard for channel as Push Notification and Engagement Metric as Opted-in");

//		Engagement Channel is Email Subscribed
		segmentName = CreateDateTime.getUniqueString("Segment_EngagementChannelisEmailSubscribed");
		String EngagementChannelisEmailSubscribed_builder_clause = utils
				.convertToDoubleStringified(segmentBuilderClauses.get("EngagementChannelisEmailSubscribed"));

		Response segmentCreationUsingBuilderClauseResponse2 = pageObj.endpoints().segmentCreationUsingBuilderClause(
				dataSet.get("apiKey"), segmentName, EngagementChannelisEmailSubscribed_builder_clause);
		Assert.assertEquals(segmentCreationUsingBuilderClauseResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String segmentId2 = segmentCreationUsingBuilderClauseResponse2.jsonPath().get("data.id").toString();

		Response segmentCountresponse2 = pageObj.endpoints().getSegmentCount(dataSet.get("apiKey"), segmentId2);
		Assert.assertEquals(segmentCountresponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for Fetch segment count dashboard api");
		utils.logPass("Fetch segment count Dashboard API is Successful");
		String segmentCountMessage2 = segmentCountresponse2.jsonPath().getString("message");
		Assert.assertEquals(segmentCountMessage2, "Fetched Count Successfully");
		int segmentCount2 = Integer
				.parseInt((segmentCountresponse2.jsonPath().get("count").toString()).replaceAll(",", ""));
		Assert.assertTrue(segmentCount2 > count, "Segment count is not greater than 0.");
		utils.logit("Segment count is greater than 0  ie " + segmentCount2);
		utils.logit("PASS",
				"Verfied guest Count label appeard for on segment overview page channel as Email and Engagement Metric as Subscribed");
		pageObj.segmentsPage().searchAndOpenSegment(segmentName, segmentId2);
		pageObj.segmentsBetaPage().getGuestInSegmentCount();
		String userEmail = pageObj.segmentsBetaPage().getSegmentGuestList(dataSet.get("apiKey"), segmentId2);
		pageObj.segmentsPage().segmentOverviewPageOptionList("Export");
		utils.switchToWindow();
		String exportName = "Export_Report_" + segmentName;
		pageObj.schedulePage().segmentExportSchedule("OFF", exportName, "Immediately");
		boolean logsConfirmation = pageObj.schedulePage()
				.selectDataExportSchedule(prop.getProperty("segmentExportSchedule"), segmentName);
		Assert.assertTrue(logsConfirmation, "Failed to verify data export logs");
		utils.logPass("Successfully verify data export logs for " + segmentName);

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.campaignspage().selectMessagedrpValue(dataSet.get("messageDrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().setCampaignName(massCampaignName);
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().setAudienceType(dataSet.get("audiance"));
		pageObj.signupcampaignpage().setSegmentType(segmentName);
		pageObj.signupcampaignpage().setPushNotification(massCampaignName);
		pageObj.signupcampaignpage().setEmailSubject(dataSet.get("emailSubject"));
		pageObj.signupcampaignpage().setEmailTemplate(dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().clickNextButton();
		pageObj.signupcampaignpage().createWhenDetailsMassCampaign("Once", dateTime);
		String msg = pageObj.guestTimelinePage().validateSuccessMessage();
		Assert.assertTrue(msg.contains("Schedule created successfully"), "Success message text did not matched");
		utils.logPass("Mass Campaign scheduled successfully : " + massCampaignName);

		// run mass offer
		// navigate to Menu -> submenu
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().findMassCampaignNameandRun(massCampaignName);

		// Timeline validation
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		String campaignName = pageObj.guestTimelinePage().getcampaignNameWithWait(massCampaignName);
		Assert.assertTrue(campaignName.equalsIgnoreCase(massCampaignName), "Campaign name did not matched");
		utils.logPass("Mass Offer campaign with name " + massCampaignName + " is found on user Timeline");

		boolean campaignNotificationStatus = pageObj.guestTimelinePage().verifyCampaignNotification();
		Assert.assertTrue(campaignNotificationStatus, "Campaign notification did not displayed...");
		utils.logPass("Mass Offer campaign with name " + massCampaignName
				+ " is found on user Timeline and Campaign Notification Status is :- " + campaignNotificationStatus);

		// deleting segment
		query = "DELETE FROM `segment_definitions` WHERE `id` = '" + segmentId2 + "';";
		DBUtils.executeQuery(env, query);
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		pageObj.utils().logit("Data set cleared");
	}

	@AfterClass(alwaysRun = true)
	public void closeBrowser() {
		driver.quit();
		pageObj.utils().logit("Browser closed");
	}
}