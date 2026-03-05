package com.punchh.server.segmentsTest;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
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
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class DigitalPaymentZipcodeSegmentTest {
	public WebDriver driver;
	private Properties prop;
	private PageObj pageObj;
	private String baseUrl;
	private static Map<String, String> dataSet;
	private String env, run = "ui";
	private String sTCName;
	Utilities utils;
	String blankSpace;
	private String userEmail;
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
	public void beforeClass(Method method) throws JsonMappingException, JsonProcessingException, IOException {
		prop = Utilities.loadPropertiesFile("config.properties");
		sTCName = method.getName();

		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		pageObj.utils().logit(sTCName + " ==>" + dataSet);
		utils = new Utilities(driver);
		blankSpace = "";
		ObjectMapper mapper = new ObjectMapper();
		segmentBuilderClauses = mapper.readTree(Files
				.readString(Paths.get(System.getProperty("user.dir") + prop.getProperty("segmentBuilderClauseJson"))));
	}

	// Rakhi
	@Test(description = "SQ-T5520 Verify the functionality of flag for setting maximum limit for zip code", groups = {
			"regression", "dailyrun" }, priority = 0)
	@Owner(name = "Rakhi Rawat")
	public void T5520_verifySegmentZipcodeLimitFunctionalityPartOne() throws Exception {

		// login to instance
//		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
//		pageObj.instanceDashboardPage().loginToInstance();

		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.menupage().navigateToSubMenuItem("SRE", "Segment Configuration");

		// set segment size as 20
		pageObj.dashboardpage().checkSegmentZipcodeSize("segment_zip_code_size", "20");

	}

	@Test(description = "SQ-T5135 Verify that the functionality for segment type Digital Stored Value is working fine. ", groups = {
			"unstable" }, priority = 1)
	@Owner(name = "Rakhi Rawat")
	public void T5135_digitalStoredValueSegmentFunctionality() throws Exception {
		String dateTime = CreateDateTime.getTomorrowDate() + " 11:00 PM";
		pageObj.utils().logit(sTCName + " ==>" + dataSet);

		String userEmail1 = dataSet.get("email1");
		String userEmail2 = dataSet.get("email2");
		// String userEmail3 = dataSet.get("email3");
		int count = 0;

		// Digital Stored value Status Active - Segment 1
		String segmentName = CreateDateTime.getUniqueString("Segment_Digital_Stored_ValueStatusActive");
		String digitalStoredValueStatusActive_builder_clause = utils
				.convertToDoubleStringified(segmentBuilderClauses.get("digitalStoredValueStatusActive"));

		Response segmentCreationUsingBuilderClauseResponse = pageObj.endpoints().segmentCreationUsingBuilderClause(
				dataSet.get("apiKey"), segmentName, digitalStoredValueStatusActive_builder_clause);
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
		utils.logPass("Segment count is greater than 0  ie " + segmentCount);
		// deleting segment
		String query = "DELETE FROM `segment_definitions` WHERE `id` = '" + segmentId + "';";
		DBUtils.executeQuery(env, query);

//		Digital Stored value Status Deactivated  - Segment 2
		segmentName = CreateDateTime.getUniqueString("Segment_Digital_Stored_ValueStatusDeactivated");
		String digitalStoredValueStatusDeactive_builder_clause = utils
				.convertToDoubleStringified(segmentBuilderClauses.get("digitalStoredValueStatusDeactivated"));

		Response segmentCreationUsingBuilderClauseResponse1 = pageObj.endpoints().segmentCreationUsingBuilderClause(
				dataSet.get("apiKey"), segmentName, digitalStoredValueStatusDeactive_builder_clause);
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
		utils.logPass("Segment count is greater than 0  ie " + segmentCount1);
		// deleting segment
		query = "DELETE FROM `segment_definitions` WHERE `id` = '" + segmentId1 + "';";
		DBUtils.executeQuery(env, query);

//		Digital Stored value Activation date  - Segment 3
		segmentName = CreateDateTime.getUniqueString("Segment_Digital_Stored_ValueActivationDateEverTimezoneIsChennai");
		String digitalStoredValueActivationDate_builder_clause = utils.convertToDoubleStringified(
				segmentBuilderClauses.get("digitalStoredValueActivationDateEverTimezoneIsChennai"));

		Response segmentCreationUsingBuilderClauseResponse2 = pageObj.endpoints().segmentCreationUsingBuilderClause(
				dataSet.get("apiKey"), segmentName, digitalStoredValueActivationDate_builder_clause);
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
		Assert.assertTrue(segmentCount2 >= count, "Segment count is not greater than 0.");
		utils.logPass("Segment count is greater than 0  ie " + segmentCount2);

		Response segmentResponse = pageObj.endpoints().userInSegment(userEmail1, dataSet.get("apiKey"), segmentId2);
		Assert.assertEquals(segmentResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for user in segment API");
		String result = segmentResponse.jsonPath().get("result").toString();
		Assert.assertEquals(result, "true", "Guest is present in segment");
		utils.logPass(userEmail1 + " user found in segment : " + segmentName);

		// login to instance
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// navigate to segments
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		pageObj.newCamHomePage().clickOnSwitchToClassicCamp();

		pageObj.segmentsPage().searchAndOpenSegment(segmentName, segmentId2);

		pageObj.segmentsBetaPage().segmentEllipsisOptions("Export");
		utils.switchToWindow();
		String exportName = "Export_Report_" + segmentName;
		pageObj.schedulePage().segmentExportSchedule("OFF", exportName, "Immediately");
		boolean logsConfirmation = pageObj.schedulePage()
				.selectDataExportSchedule(prop.getProperty("segmentExportSchedule"), segmentName);
		Assert.assertTrue(logsConfirmation, "Failed to verify data export logs");
		utils.logPass("Successfully verify data export logs for " + segmentName);

		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		// create new segment punchh segment
		String punchhSegmentName = CreateDateTime.getUniqueString("Punchh_Segment");
		pageObj.segmentsBetaPage().setSegmentBetaNameAndType(punchhSegmentName, "Punchh Segment");
		pageObj.segmentsBetaPage().setSegmentlistType(segmentName);
		pageObj.segmentsBetaPage().saveSegment(punchhSegmentName);
		utils.waitTillSpinnerDisappear();
		String punchhSegmentId = pageObj.segmentsPage().getSegmentID();

		int punchhSegmenGuestCount = pageObj.segmentsBetaPage().getGuestInSegmentCount();
		Assert.assertTrue(punchhSegmenGuestCount > count, "Segment count is not greater than 0");
		Assert.assertEquals(segmentCount2, punchhSegmenGuestCount, "Guest count in punchh segment " + punchhSegmentName
				+ " is not equal to the segment count in Digital Stored Value Segment " + segmentName);
		utils.logPass("Verified that Guest count in punchh segment " + punchhSegmentName
				+ " is equal to the segment count in Digital Stored Value Segment " + segmentName);
		// create mass campaign with the Digital Stored Value segment
		String massCampaignName = "Automation MassNotification Campaign" + CreateDateTime.getTimeDateString();

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		pageObj.campaignspage().selectMessagedrpValue(dataSet.get("messageDrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().setCampaignName(massCampaignName);
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().setAudienceType(dataSet.get("audiance"));
		pageObj.signupcampaignpage().setSegmentType(segmentName);
		pageObj.signupcampaignpage().setPushNotification(dataSet.get("pushNotification"));
		pageObj.signupcampaignpage().setEmailSubject(dataSet.get("emailSubject"));
		pageObj.signupcampaignpage().setEmailTemplate(dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().clickNextButton();
		pageObj.signupcampaignpage().setFrequencyAndTimeInCampaign("Once", dateTime);
		pageObj.signupcampaignpage().clickScheduleBtn();

		String msg = pageObj.guestTimelinePage().validateSuccessMessage();
		Assert.assertTrue(msg.contains("Schedule created successfully"), "Success message text did not match");
		utils.logPass("Mass Campaign scheduled successfully : " + massCampaignName);

		// navigate to Menu -> submenu
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().findMassCampaignNameandRun(massCampaignName);

		// Timeline validation (move to user timeline)
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail1);
		utils.longWaitInSeconds(10);
		String campaignName = pageObj.guestTimelinePage().getcampaignNameWithWait(massCampaignName);
		Assert.assertTrue(campaignName.equalsIgnoreCase(massCampaignName),
				massCampaignName + " Campaign name did not matched");

		boolean campaignNotificationStatus = pageObj.guestTimelinePage()
				.verifyCampaignSystemNotificationIsVisible(massCampaignName);
		Assert.assertTrue(campaignNotificationStatus, "Campaign notification did not displayed...");

		boolean pushNotificationStatus = pageObj.guestTimelinePage().verifyPushNotificationMassCampaign();
		Assert.assertTrue(pushNotificationStatus, "Push notification did not displayed...");
		pageObj.utils().logit("Guest received PN for campaign :" + massCampaignName);

		// create trigger campaign with the digital-stored value segment
		String postCheckinCampaignName = CreateDateTime.getUniqueString("Automation Postcheckin Campaign");

		// check redeemable's availability in business and create redeemable if not
		// available
		pageObj.menupage().navigateToSubMenuItem("Settings", "Redeemables");
		pageObj.redeemablePage().createRedeemableIfNotExistWithExistingQC(dataSet.get("redeemable"), "Flat Discount",
				"", "2.0");

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();

		pageObj.signupcampaignpage().createWhatDetailsPostcheckinCampaign(postCheckinCampaignName,
				dataSet.get("giftType"), dataSet.get("giftReason"), dataSet.get("redeemable"));
		pageObj.signupcampaignpage().setSegmentType(segmentName);
		pageObj.signupcampaignpage().createWhomDetailsPostcheckinCampaign(postCheckinCampaignName,
				dataSet.get("emailSubject1"), dataSet.get("emailTemplate1"));

		pageObj.signupcampaignpage().createWhenDetailsCampaign();
		boolean status1 = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status1, "Post Checkin Campaign is not created...");

		// Pos api checkin
		String key = CreateDateTime.getTimeDateString();
		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T07:17:32Z";
		Response resp = pageObj.endpoints().posCheckin(date, userEmail1, key, txn, dataSet.get("locationkey"));
		Assert.assertEquals(resp.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for post chekin api");

		// timeline validation
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail1);
		String campaignName1 = pageObj.guestTimelinePage().getcampaignNameWithWait(postCheckinCampaignName);
		Assert.assertTrue(campaignName1.equalsIgnoreCase(postCheckinCampaignName), "Campaign name did not matched");
		boolean campaignNotificationStatus1 = pageObj.guestTimelinePage().verifyCampaignNotification();
		Assert.assertTrue(campaignNotificationStatus1, "Campaign notification did not displayed...");
		String giftedItemName = pageObj.guestTimelinePage().verifyrewardedRedeemablePostCheckin();
		Assert.assertTrue(giftedItemName.contains(dataSet.get("redeemable")), "Gifted item name did not matched");
		utils.logPass(
				"Postcheckin campaign with segment user: push notification, campaign name, reward notification validated successfully on timeline");

		// Delete created campaign
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value
		Thread.sleep(2000);
		pageObj.campaignspage().selectOngoingdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().removeSearchedCampaign(postCheckinCampaignName);

		// delete punchh segment
		query = "DELETE FROM `segment_definitions` WHERE `id` = '" + punchhSegmentId + "';";
		DBUtils.executeQuery(env, query);

		// delete segment
		query = "DELETE FROM `segment_definitions` WHERE `id` = '" + segmentId2 + "';";
		DBUtils.executeQuery(env, query);

	}

	// Rakhi
	@Test(description = "SQ-T5134 Verify that the functionality for segment type 'Saved Payment' is working fine", priority = 2)
	@Owner(name = "Rakhi Rawat")
	public void T5134_savedPaymentSegmentFunctionality() throws Exception {
		pageObj.utils().logit(sTCName + " ==>" + dataSet);
		// String userEmail1 = dataSet.get("email1");
		String dateTime = CreateDateTime.getTomorrowDate() + " 11:00 PM";
		int count = 0;

//		Saved Payment Card Type Master Card
		String segmentName = CreateDateTime.getUniqueString("Segment_savedPaymentCardTypeMasterCard");
		String savedPaymentCardTypeMasterCard_builder_clause = utils
				.convertToDoubleStringified(segmentBuilderClauses.get("savedPaymentCardTypeMasterCard"));

		Response segmentCreationUsingBuilderClauseResponse = pageObj.endpoints().segmentCreationUsingBuilderClause(
				dataSet.get("apiKey"), segmentName, savedPaymentCardTypeMasterCard_builder_clause);
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
		utils.logPass("Segment count is greater than 0  ie " + segmentCount);
		// deleting segment
		String query = "DELETE FROM `segment_definitions` WHERE `id` = '" + segmentId + "';";
		DBUtils.executeQuery(env, query);

//		Saved Payment Card Count Greater than 0
		segmentName = CreateDateTime.getUniqueString("Segment_SavedPaymentCardCountGreaterThan0");
		String SavedPaymentCardCountGreaterThan0_builder_clause = utils
				.convertToDoubleStringified(segmentBuilderClauses.get("SavedPaymentCardCountGreaterThan0"));

		Response segmentCreationUsingBuilderClauseResponse1 = pageObj.endpoints().segmentCreationUsingBuilderClause(
				dataSet.get("apiKey"), segmentName, SavedPaymentCardCountGreaterThan0_builder_clause);
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
		utils.logPass("Segment count is greater than 0  ie " + segmentCount1);
		// deleting segment
		query = "DELETE FROM `segment_definitions` WHERE `id` = '" + segmentId1 + "';";
		DBUtils.executeQuery(env, query);

//		Saved Payment Card Count Equal to 0 Profile Details Zip Code In 123456
		segmentName = CreateDateTime
				.getUniqueString("Segment_SavedPaymentCardCountEqualto0ProfileDetailsZipCodeIn123456");
		String SavedPaymentCardCountEqualto0ProfileDetailsZipCodeIn123456_builder_clause = utils
				.convertToDoubleStringified(
				segmentBuilderClauses.get("SavedPaymentCardCountEqualto0ProfileDetailsZipCodeIn123456"));

		Response segmentCreationUsingBuilderClauseResponse2 = pageObj.endpoints().segmentCreationUsingBuilderClause(
				dataSet.get("apiKey"), segmentName,
				SavedPaymentCardCountEqualto0ProfileDetailsZipCodeIn123456_builder_clause);
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
		Assert.assertTrue(segmentCount2 >= count, "Segment count is not greater than 0.");
		utils.logPass("Segment count is greater than 0  ie " + segmentCount2);

		// login to instance
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// navigate to cockpit -> dashboard
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().onEnableSavedPaymentCheckbox();

		// navigate to segments
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		pageObj.newCamHomePage().clickOnSwitchToClassicCamp();

		pageObj.segmentsPage().searchAndOpenSegment(segmentName, segmentId2);

		String userEmail = pageObj.segmentsPage().clickFirstUserFromSegment();
		pageObj.guestTimelinePage().nagivateBack();
		Boolean flag = pageObj.segmentsPage().verifyUserPresentInSegment(userEmail, "#ddf3dd");
		Assert.assertTrue(flag, "user not found in segment : " + segmentName);
//		// verifying In_segment query using API
//		Response userInSegmentResp = pageObj.endpoints().userInSegment(userEmail, dataSet.get("apiKey"), segId);
//		Assert.assertEquals(userInSegmentResp.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
//				"Status code 200 did not matched for auth user signup api");
//		Assert.assertTrue(userInSegmentResp.asString().contains("true"), "User not found in segment : " + segmentName);
		utils.logPass(userEmail + " user found in segment : " + segmentName);

		pageObj.segmentsBetaPage().segmentEllipsisOptions("Export");
		utils.switchToWindow();
		String exportName = "Export_Report_" + segmentName;
		pageObj.schedulePage().segmentExportSchedule("OFF", exportName, "Immediately");
		boolean logsConfirmation = pageObj.schedulePage()
				.selectDataExportSchedule(prop.getProperty("segmentExportSchedule"), segmentName);
		Assert.assertTrue(logsConfirmation, "Failed to verify data export logs");
		utils.logPass("Successfully verify data export logs for " + segmentName);

		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		// create new segment punchh segment
		String punchhSegmentName = CreateDateTime.getUniqueString("Punchh_Segment");
		pageObj.segmentsBetaPage().setSegmentBetaNameAndType(punchhSegmentName, "Punchh Segment");
		pageObj.segmentsBetaPage().setSegmentlistType(segmentName);
		pageObj.segmentsBetaPage().saveSegment(punchhSegmentName);
		int guestCount = pageObj.segmentsBetaPage().getGuestInSegmentCount();
		Assert.assertTrue(guestCount > count, "Segment count is not greater than 0");

		// create mass campaign with the Saved Payment type segment
		String massCampaignName = "Automation MassNotification Campaign" + CreateDateTime.getTimeDateString();
		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		pageObj.campaignspage().selectMessagedrpValue(dataSet.get("messageDrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().setCampaignName(massCampaignName);
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().setAudienceType(dataSet.get("audiance"));
		pageObj.signupcampaignpage().setSegmentType(segmentName);
		pageObj.signupcampaignpage().setPushNotification(dataSet.get("pushNotification"));
		pageObj.signupcampaignpage().setEmailSubject(dataSet.get("emailSubject"));
		pageObj.signupcampaignpage().setEmailTemplate(dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().clickNextButton();
		pageObj.signupcampaignpage().setFrequencyAndTimeInCampaign("Once", dateTime);
		pageObj.signupcampaignpage().clickScheduleBtn();

		String msg = pageObj.guestTimelinePage().validateSuccessMessage();
		Assert.assertTrue(msg.contains("Schedule created successfully"), "Success message text did not matched");
		utils.logPass("Mass Campaign scheduled successfully : " + massCampaignName);

		// navigate to Menu -> submenu
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().findMassCampaignNameandRun(massCampaignName);

		// create trigger campaign with the Saved Payment type segment
		String postCheckinCampaignName = CreateDateTime.getUniqueString("Automation Postcheckin Campaign");

		// check redeemable's availability in business and create redeemable if not
		// available
		pageObj.menupage().navigateToSubMenuItem("Settings", "Redeemables");
		pageObj.redeemablePage().createRedeemableIfNotExistWithExistingQC(dataSet.get("redeemable"), "Flat Discount",
				"", "2.0");

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		// Select offer dropdown value
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();

		pageObj.signupcampaignpage().createWhatDetailsPostcheckinCampaign(postCheckinCampaignName,
				dataSet.get("giftType"), dataSet.get("giftReason"), dataSet.get("redeemable"));
		pageObj.signupcampaignpage().setSegmentType(segmentName);
		pageObj.signupcampaignpage().createWhomDetailsPostcheckinCampaign(dataSet.get("pushNotification1"),
				dataSet.get("emailSubject1"), dataSet.get("emailTemplate1"));

		pageObj.signupcampaignpage().createWhenDetailsCampaign();
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "Post Checkin Campaign is not created...");

		// Pos api checkin
		String key = CreateDateTime.getTimeDateString();
		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T07:17:32Z";
		Response resp = pageObj.endpoints().posCheckin(date, userEmail, key, txn, dataSet.get("locationkey"));
		Assert.assertEquals(resp.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for post chekin api");

		// validate campaign status for mass campaign
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.campaignspage().searchCampaign(massCampaignName);
		String camStatus = pageObj.campaignspage().checkMassCampStatusBeforeOpening(massCampaignName, "Processed");
		Assert.assertEquals(camStatus, "Processed", "Campaign status is not processed");
		pageObj.utils().logPass("Mass offer campaign processed successfully");

		// Timeline validation (move to user timeline)
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		String campaignName = pageObj.guestTimelinePage().getcampaignNameMasscampaign(massCampaignName);
		Assert.assertTrue(campaignName.equalsIgnoreCase(massCampaignName),
				massCampaignName + " Campaign name did not matched");

		boolean campaignNotificationStatus = pageObj.guestTimelinePage()
				.verifyCampaignSystemNotificationIsVisible(massCampaignName);
		Assert.assertTrue(campaignNotificationStatus, "Campaign notification did not displayed...");

		boolean pushNotificationStatus = pageObj.guestTimelinePage().verifyPushNotificationMassCampaign();
		Assert.assertTrue(pushNotificationStatus, "Push notification did not displayed...");
		pageObj.utils().logit("Guest received PN for campaign :" + massCampaignName);

		// timeline validation for post checkin campaign
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		String campaignName1 = pageObj.guestTimelinePage().getcampaignNameMasscampaign(postCheckinCampaignName);
		boolean campaignNotificationStatus1 = pageObj.guestTimelinePage().verifyCampaignNotification();
		boolean pushNotificationStatus1 = pageObj.guestTimelinePage().verifyPushNotificationPostCheckin();
		String giftedItemName = pageObj.guestTimelinePage().verifyrewardedRedeemablePostCheckin();

		Assert.assertTrue(campaignNotificationStatus1, "Campaign notification did not displayed...");
		Assert.assertTrue(pushNotificationStatus1, "Push notification did not displayed...");
		Assert.assertTrue(campaignName1.equalsIgnoreCase(postCheckinCampaignName), "Campaign name did not matched");
		Assert.assertTrue(giftedItemName.contains(dataSet.get("redeemable")), "Gifted item name did not matched");
		pageObj.utils().logPass(
				"Postcheckin campaign with segment user: push notification, campaign name, reward notification validated successfully on timeline");

		// Delete created campaign
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value
		Thread.sleep(2000);
		pageObj.campaignspage().selectOngoingdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().removeSearchedCampaign(postCheckinCampaignName);

		// deleting segment
		query = "DELETE FROM `segment_definitions` WHERE `id` = '" + segmentId2 + "';";
		DBUtils.executeQuery(env, query);

	}

	// Rakhi
	@Test(description = "SQ-T5520 Verify the functionality of flag for setting maximum limit for zip code", groups = {
			"unstable" }, priority = 10)
	@Owner(name = "Rakhi Rawat")
	public void T5520_verifySegmentZipcodeLimitFunctionalityPartTwo() throws Exception {

		pageObj.utils().logit(sTCName + " ==>" + dataSet);
		String segmentName = CreateDateTime.getUniqueString("Zipcode_Profile_Detail_Segment");
		String exportName = "Export_Report_" + segmentName;
		String userEmail = dataSet.get("email");
		String falseZipcodeValues = dataSet.get("zipCodes");
		// String zipCode = CreateDateTime.getRandomNumberSixDigit();
		String zipCode = dataSet.get("zipCode");

		// login to instance
//		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
//		pageObj.instanceDashboardPage().loginToInstance();

		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// navigate to segments
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");

		// create new segment with attribute as Card Type
//		pageObj.segmentsBetaPage().setSegmentBetaNameAndType(segmentName, "Profile Details");
		pageObj.segmentsBetaPage().setSegmentBetaName(segmentName);
		pageObj.segmentsBetaPage().selectSegmentType("Profile Details");
		pageObj.segmentsBetaPage().selectAttribute("Zip Code");
		pageObj.segmentsBetaPage().setOperator("Operator", "In");
		String operatorValue = pageObj.segmentsPage().getOperatorValue();
		Assert.assertEquals(operatorValue, "In", "Selected Segment Operator is not matching");
		utils.logit("Selected Segment Operator is opted");
		pageObj.segmentsBetaPage().setValue(falseZipcodeValues);
		pageObj.segmentsPage().saveAndShowSegment();
		boolean flag = pageObj.segmentsBetaPage().verifyValueErrorMsg();
		Assert.assertTrue(flag, "User will be able to save the segment");
		utils.logPass("User will not be able to save the segment");

		// enter zipcode under 20 digits
		pageObj.segmentsBetaPage().removeSegmentAttribute();
		pageObj.segmentsBetaPage().selectAttribute("Zip Code");
		pageObj.segmentsBetaPage().setOperator("Operator", "In");
		pageObj.segmentsBetaPage().setValue(zipCode);
		utils.longWaitInSeconds(1);

		int segmentCountBeta = pageObj.segmentsBetaPage().getSegmentSizeBeforeSave();
		Assert.assertTrue(segmentCountBeta > 0, segmentName);
		boolean flag1 = pageObj.segmentsBetaPage().verifyValueErrorMsg();
		Assert.assertFalse(flag1, "User will be not able to save the segment");
		// get segment id through api
		String getSegmentIdQuery = "select id from segment_definitions where name ='" + segmentName + "'"
				+ "and business_id=" + dataSet.get("slugid");
		String segId = DBUtils.executeQueryAndGetColumnValue(env, getSegmentIdQuery, "id");
		pageObj.utils().logit("successfully fetched segment id : " + segId);
//		// verify user presence in segment
//		Boolean flag2 = pageObj.segmentsPage().verifyUserPresentInSegment(userEmail, "#ddf3dd");
//		Assert.assertTrue(flag2, "user not found in segment : " + segmentName);
		// verifying In_segment query using API
		Response userInSegmentResp = pageObj.endpoints().userInSegment(userEmail, dataSet.get("apiKey"), segId);
		Assert.assertEquals(userInSegmentResp.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for auth user signup api");
		Assert.assertTrue(userInSegmentResp.asString().contains("true"), "User not found in segment : " + segmentName);
		utils.logPass(userEmail + " user found in segment : " + segmentName);

		pageObj.segmentsBetaPage().segmentEllipsisOptions("Export");
		utils.switchToWindow();
		pageObj.schedulePage().segmentExportSchedule("OFF", exportName, "Immediately");
		boolean logsConfirmation = pageObj.schedulePage()
				.selectDataExportSchedule(prop.getProperty("segmentExportSchedule"), segmentName);
		Assert.assertTrue(logsConfirmation, "Failed to verify data export logs");
		utils.logPass("Successfully verify data export logs for " + segmentName);

		// create trigger campaign with the ZipCode Profile Detail type segment
		String postCheckinCampaignName = CreateDateTime.getUniqueString("Automation Postcheckin Campaign");

		// check redeemable's availability in business and create redeemable if not
		// available
		pageObj.menupage().navigateToSubMenuItem("Settings", "Redeemables");
		pageObj.redeemablePage().createRedeemableIfNotExistWithExistingQC(dataSet.get("redeemable"), "Flat Discount",
				"", "2.0");

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickOnSwitchToClassicCamp();

		// Select offer dropdown value
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();

		pageObj.signupcampaignpage().createWhatDetailsPostcheckinCampaign(postCheckinCampaignName,
				dataSet.get("giftType"), dataSet.get("giftReason"), dataSet.get("redeemable"));
		pageObj.signupcampaignpage().setSegmentType(segmentName);
		pageObj.signupcampaignpage().createWhomDetailsPostcheckinCampaign(dataSet.get("pushNotification1"),
				dataSet.get("emailSubject1"), dataSet.get("emailTemplate1"));

		pageObj.signupcampaignpage().createWhenDetailsCampaign();
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "Post Checkin Campaign is not created...");

		// Pos api checkin
		String key = CreateDateTime.getTimeDateString();
		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T07:17:32Z";
		Response resp = pageObj.endpoints().posCheckin(date, userEmail, key, txn, dataSet.get("locationkey"));
		Assert.assertEquals(resp.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for post chekin api");

		// timeline validation
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		String campaignName1 = pageObj.guestTimelinePage().getcampaignNameWithWait(postCheckinCampaignName);
		boolean campaignNotificationStatus1 = pageObj.guestTimelinePage().verifyCampaignNotification();
		boolean pushNotificationStatus1 = pageObj.guestTimelinePage().verifyPushNotificationPostCheckin();
		String giftedItemName = pageObj.guestTimelinePage().verifyrewardedRedeemablePostCheckin();

		Assert.assertTrue(campaignNotificationStatus1, "Campaign notification did not displayed...");
		Assert.assertTrue(pushNotificationStatus1, "Push notification did not displayed...");
		Assert.assertTrue(campaignName1.equalsIgnoreCase(postCheckinCampaignName), "Campaign name did not matched");
		Assert.assertTrue(giftedItemName.contains(dataSet.get("redeemable")), "Gifted item name did not matched");
		pageObj.utils().logPass(
				"Postcheckin campaign with segment user: push notification, campaign name, reward notification validated successfully on timeline");

		// Delete created campaign
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickOnSwitchToClassicCamp();
		// Select offer dropdown value
		Thread.sleep(2000);
		pageObj.campaignspage().selectOngoingdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().removeSearchedCampaign(postCheckinCampaignName);

	}

	// Rakhi
	@Test(description = "SQ-T5757 Verify the functionality for the Custom List >> Freeze from segment segmentation", priority = 3)
	@Owner(name = "Rakhi Rawat")
	public void T5757_FreezeFromSegmentFunctionality() throws Exception {
		pageObj.utils().logit(sTCName + " ==>" + dataSet);
		String segmentNameNew = CreateDateTime.getUniqueString("Freeze_From_Segment");
		String exportName = "Export_Report_" + segmentNameNew;
		String dateTime = CreateDateTime.getTomorrowDate() + " 11:00 PM";
		// login to instance
//		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
//		pageObj.instanceDashboardPage().loginToInstance();

		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// navigate to cockpit -> dashboard
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().onEnableSavedPaymentCheckbox();

		// navigate to segments
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		pageObj.segmentsPage().searchAndOpenSegment(dataSet.get("segmentName1"), dataSet.get("segmentId1"));
		int segmentCount = pageObj.segmentsBetaPage().getSegmentCount();

		// create new segment with attribute as Card Type
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		pageObj.segmentsBetaPage().setSegmentBetaNameAndType(segmentNameNew, "Custom List");

		// verifying the attribute dropdown list on segments page.
		List<String> actualAttributeList = pageObj.segmentsPage().getAttributeFromSegmentType("List Type");
		List<String> expPaymentAttributeList = Arrays.asList("Freeze from Segment", "New Custom List");
		boolean attributeFlag = false;
		if (actualAttributeList.containsAll(expPaymentAttributeList)) {
			attributeFlag = true;
		}
		Assert.assertTrue(attributeFlag,
				"Failed to verify that user is not able to view List Type dropdown options Freeze from Segment , Custom List");
		utils.logPass(
				"The user is able to see the following List Type dropdown options Freeze from Segment, Custom List");

		// refresh page
		utils.refreshPage();
		pageObj.segmentsPage().setSegmentName(segmentNameNew);
		pageObj.segmentsBetaPage().selectSegmentType("Custom List");
		pageObj.segmentsBetaPage().setOperator("List Type", "Freeze from Segment");
		pageObj.segmentsBetaPage().selectAttribute("Freeze from segment", dataSet.get("segmentName1"));
		pageObj.segmentsBetaPage().saveSegment(segmentNameNew);
		utils.waitTillSpinnerDisappear();

		// navigate to segments
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		pageObj.segmentsPage().searchAndOpenSegment(dataSet.get("segmentName2"), dataSet.get("segmentId2"));
		int segmentCount1 = pageObj.segmentsBetaPage().getSegmentCount();
		Assert.assertTrue(segmentCount1 == segmentCount, "Guest Count did not matched");
		utils.logPass("Guest Count matched and count is " + segmentCount1);

		// verifying In_segment query
		Boolean flag1 = pageObj.segmentsPage().verifyUserPresentInSegment(dataSet.get("userEmail"), "#ddf3dd");
		Assert.assertTrue(flag1, "user not found in segment : " + dataSet.get("segmentName2"));
		utils.logPass(dataSet.get("userEmail") + " user found in segment : " + dataSet.get("segmentName2"));

		// Segment export for exisiting freeze from segemnt
		pageObj.segmentsBetaPage().segmentEllipsisOptions("Export");
		utils.switchToWindow();
		pageObj.schedulePage().segmentExportSchedule("OFF", exportName, "Immediately");
		boolean logsConfirmation = pageObj.schedulePage()
				.selectDataExportSchedule(prop.getProperty("segmentExportSchedule"), dataSet.get("segmentName2"));
		Assert.assertTrue(logsConfirmation, "Failed to verify data export logs");
		utils.logPass("Successfully verify data export logs for " + dataSet.get("segmentName2"));

		// create mass campaign with the Digital Stored Value segment
		String massCampaignName = "Automation MassNotification Campaign" + CreateDateTime.getTimeDateString();

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		pageObj.campaignspage().selectMessagedrpValue(dataSet.get("messageDrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().setCampaignName(massCampaignName);
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().setAudienceType(dataSet.get("audiance"));
		pageObj.signupcampaignpage().setSegmentType(dataSet.get("segmentName2"));
		pageObj.signupcampaignpage().setPushNotification(dataSet.get("pushNotification"));
		pageObj.signupcampaignpage().setEmailSubject(dataSet.get("emailSubject"));
		pageObj.signupcampaignpage().setEmailTemplate(dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().clickNextButton();
//		pageObj.signupcampaignpage().setFrequency("Once");
//		pageObj.campaignsbetaPage().setStartTime();
		pageObj.signupcampaignpage().setFrequencyAndTimeInCampaign("Once", dateTime);
		pageObj.signupcampaignpage().clickScheduleBtn();

		String msg = pageObj.guestTimelinePage().validateSuccessMessage();
		Assert.assertTrue(msg.contains("Schedule created successfully"),
				"Failed to verify campaign scheduled sucess message");
		utils.logPass("Mass Campaign scheduled successuly : " + massCampaignName);

		// navigate to Menu -> submenu
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().findMassCampaignNameandRun(massCampaignName);

		// Timeline validation (move to user timeline)
		pageObj.instanceDashboardPage().navigateToGuestTimeline(dataSet.get("userEmail"));
		utils.longWaitInSeconds(10);
		String campaignName = pageObj.guestTimelinePage().getcampaignNameMasscampaign(massCampaignName);
		Assert.assertTrue(campaignName.equalsIgnoreCase(massCampaignName),
				massCampaignName + " Campaign name did not matched");

		boolean campaignNotificationStatus = pageObj.guestTimelinePage()
				.verifyCampaignSystemNotificationIsVisible(massCampaignName);
		Assert.assertTrue(campaignNotificationStatus, "Failed to verify Campaign notification on user timeline...");

		boolean pushNotificationStatus = pageObj.guestTimelinePage().verifyPushNotificationMassCampaign();
		Assert.assertTrue(pushNotificationStatus, "Failed to verify Push notification on usertimeline...");
		pageObj.utils().logit("Guest received PN for campaign :" + massCampaignName);

	}

	// Rakhi
	@Test(description = "SQ-T5742 Verify the functionality for the receipt tag segment type"
			+ "SQ-T5959 Verify New attribute Tag count in receipt tag", priority = 4)
	@Owner(name = "Rakhi Rawat")
	public void T5742_ReceiptTagSegmentFunctionality() throws Exception {
		pageObj.utils().logit(sTCName + " ==>" + dataSet);
		int count = 0;
//		// create new segment with attribute as Card Type
//		pageObj.segmentsBetaPage().setSegmentBetaNameAndType(segmentName, "Contains Receipt Tag(s)");
//
//		List<String> expPaymentAttributeList = Arrays.asList("Segment Type", "Receipt Tag", "Attribute", "Operator",
//				"Value", "Membership Tier", "Membership", "Location(s)", "Duration", "Timezone");
//		List<String> actualAttributeNameList = pageObj.segmentsPage().getAttributeNameList();
//
//		boolean attributeFlag = false;
//		if (actualAttributeNameList.containsAll(expPaymentAttributeList)) {
//			attributeFlag = true;
//		}
//		Assert.assertTrue(attributeFlag,
//				"Failed to verify that user is able to see the following Payment type dropdown options Purchased, Reloaded, Auto Reload, Card Added, Gifted");
//
//		pageObj.utils().logit(
//				"The user is able to see the following List Type dropdown options Purchased, Reloaded, Auto Reload, Card Added, Gifted");
//		pageObj.utils().logPass(
//				"The user is able to see the following List Type dropdown options Purchased, Reloaded, Auto Reload, Card Added, Gifted");
//
//		pageObj.segmentsBetaPage().selectAttribute("Receipt Tag", "Any");
//		pageObj.segmentsBetaPage().setOperatorText("Attribute", "% of receipts");
//		pageObj.segmentsBetaPage().setOperator("Operator", "At least");
//		pageObj.segmentsBetaPage().setOperator("Membership Tier", "Current");
//		pageObj.segmentsBetaPage().selectAttribute("Membership", "Any");
//		pageObj.segmentsBetaPage().selectAttribute("Location(s)", "Any");
//		pageObj.segmentsBetaPage().setOperator("Duration", "Ever");
//		pageObj.segmentsBetaPage().selectAttribute("Timezone", "(GMT+05:30) Chennai ( IST )");
//		pageObj.segmentsBetaPage().setValue("1");
//		pageObj.segmentsBetaPage().saveSegment(segmentName);

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");

//		Contains Receipt Tag Receipt Tag is Any Attribute is % of receipts At least 1 Membership Tier Is Current Membership Is Any Location is at Any Ever Timezone Chennai
		String segmentName = CreateDateTime.getUniqueString("Segment_Receipt_Tag_Segment");
		String AttributeisPercentofreceiptsAtleast1MembershipTier_builder_clause = utils
				.convertToDoubleStringified(segmentBuilderClauses
				.get("ContainsReceiptTagReceiptTagisAnyAttributeisPercentofreceiptsAtleast1MembershipTierIsCurrentMembershipIsAnyLocationisatAnyEverTimezoneChennai"));

		Response segmentCreationUsingBuilderClauseResponse = pageObj.endpoints().segmentCreationUsingBuilderClause(
				dataSet.get("apiKey"), segmentName, AttributeisPercentofreceiptsAtleast1MembershipTier_builder_clause);
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
		utils.logPass("Segment count is greater than 0  ie " + segmentCount);
		// deleting segment
		String query = "DELETE FROM `segment_definitions` WHERE `id` = '" + segmentId + "';";
		DBUtils.executeQuery(env, query);

//		// update segment
//		pageObj.segmentsBetaPage().segmentEllipsisOptions("Edit");
//
//		// verifying newly added attribute Tag count
//		List<String> actualAttributeList = pageObj.segmentsPage().getAttributeFromSegmentType("Attribute");
//		List<String> expectedtAttributeList = Arrays.asList("% of receipts", "sum quantities", "Tag count", "Tagged");
//		System.out.println(actualAttributeList);
//		boolean flag = false;
//		if (actualAttributeList.containsAll(expectedtAttributeList)) {
//			flag = true;
//		}
//		Assert.assertTrue(flag,
//				"Failed to verify that user is able to view newly added attribute Tag count in the Receipt Tag Segment dropdown");
//		pageObj.utils().logit("The user is able to see the newly added attribute Tag count in the Receipt Tag Segment dropdown");
//		pageObj.utils().logPass(
//				"The user is able to see the newly added attribute Tag count in the Receipt Tag Segment dropdown");
//
//		// refresh page
//		utils.refreshPage();
//		pageObj.segmentsBetaPage().setOperatorText("Attribute", "Tag count");
//		pageObj.segmentsBetaPage().setOperator("Operator", "Equal to");
//		pageObj.segmentsBetaPage().selectAttribute("Membership", "None");
//		pageObj.segmentsBetaPage().selectAttribute("Timezone", "(GMT-11:00) Midway Island ( SST )");
//		pageObj.segmentsBetaPage().setValue("2");
//		pageObj.segmentsBetaPage().saveSegment(segmentName);

//		Contains Receipt Tag Receipt Tag is Any Attribute is Tag Count Equal To 2 Membership Tier Is Current Membership Is None Location is at Any Ever Timezone Chennai
		segmentName = CreateDateTime.getUniqueString("Segment_AttributeIsTagCountEqualTo2MembershipTier");
		String AttributeIsTagCountEqualTo2MembershipTier_builder_clause = utils
				.convertToDoubleStringified(segmentBuilderClauses.get(
						"ContainsReceiptTagReceiptTagIsAnyAttributeIsTagCountEqualTo2MembershipTierIsCurrentMembershipIsNoneLocationIsatAnyEverTimezoneIsMidwayIsland"));

		Response segmentCreationUsingBuilderClauseResponse1 = pageObj.endpoints().segmentCreationUsingBuilderClause(
				dataSet.get("apiKey"), segmentName, AttributeIsTagCountEqualTo2MembershipTier_builder_clause);
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
		utils.logPass("Segment count is greater than 0  ie " + segmentCount1);
		// deleting segment
		query = "DELETE FROM `segment_definitions` WHERE `id` = '" + segmentId1 + "';";
		DBUtils.executeQuery(env, query);

////		Contains Receipt Tag Receipt Tag is Any Attribute is Tag Count Equal To 2 Membership Tier Is Current Membership Is None Location is at Any Ever Timezone Chennai
//		segmentName = CreateDateTime.getUniqueString("Segment_AnyAttributeissumquantitiesGreaterthan100MembershipTier");
//		String AnyAttributeissumquantitiesGreaterthan100MembershipTier_builder_clause = utils
//				.convertToDoubleStringified(segmentBuilderClauses.get(
//						"ContainsReceiptTagReceiptTagisAnyAttributeissumquantitiesGreaterthan100MembershipTierisCurrentMembershipisAnyLocationisatAnyEverTimezoneisChennai"));
//
//		Response segmentCreationUsingBuilderClauseResponse2 = pageObj.endpoints().segmentCreationUsingBuilderClause(
//				dataSet.get("apiKey"), segmentName,
//				AnyAttributeissumquantitiesGreaterthan100MembershipTier_builder_clause);
//		Assert.assertEquals(segmentCreationUsingBuilderClauseResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
//		String segmentId2 = segmentCreationUsingBuilderClauseResponse2.jsonPath().get("data.id").toString();
//
//		Response segmentCountresponse2 = pageObj.endpoints().getSegmentCount(dataSet.get("apiKey"), segmentId2);
//		Assert.assertEquals(segmentCountresponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
//				"Status code 200 did not matched for Fetch segment count dashboard api");
//		pageObj.utils().logit("Fetch segment count Dashboard API is Successful");
//		pageObj.utils().logPass("Fetch segment count Dashboard API is Successful");
//		String segmentCountMessage2 = segmentCountresponse2.jsonPath().getString("message");
//		Assert.assertEquals(segmentCountMessage2, "Fetched Count Successfully");
//		int segmentCount2 = Integer
//				.parseInt((segmentCountresponse2.jsonPath().get("count").toString()).replaceAll(",", ""));
//		Assert.assertTrue(segmentCount2 >= count, "Segment count is not greater than 0.");
//		pageObj.utils().logit("Segment count is greater than 0  ie " + segmentCount2);
//		pageObj.utils().logPass("Segment count is greater than 0  ie " + segmentCount2);

		// login to instance
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// navigate to segments
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		pageObj.segmentsBetaPage().setSegmentBetaNameAndType(segmentName, "Contains Receipt Tag(s)");

		// update segment
//		pageObj.segmentsBetaPage().segmentEllipsisOptions("Edit");
		pageObj.segmentsBetaPage().setOperatorText("Attribute", "sum quantities");
		pageObj.segmentsBetaPage().setOperator("Operator", "Greater than");
		pageObj.segmentsBetaPage().selectAttribute("Membership", "Any");
		pageObj.segmentsBetaPage().selectAttribute("Timezone", "(GMT+05:30) Chennai ( IST )");
		pageObj.segmentsBetaPage().setValue("100");
		utils.longWaitInSeconds(1);
		pageObj.segmentsBetaPage().selectSegmentType("Profile Details");
		pageObj.segmentsBetaPage().setAndOrNiCondition("OR");
		pageObj.segmentsBetaPage().setAttributeList("Gender");
		pageObj.segmentsBetaPage().setOperatorList("Operator", "Equal to");
		// pageObj.segmentsBetaPage().setValueList("40");
		int segmentCount2 = pageObj.segmentsBetaPage().getSegmentSizeBeforeSave();
		Assert.assertTrue(segmentCount2 >= count, "Guest Count label not appeared");
		utils.logPass("Guest Count label appeared and count is " + segmentCount2);
		pageObj.segmentsBetaPage().saveSegment(segmentName);
		String segmentId2 = pageObj.segmentsPage().getSegmentID();
		int guestCount = pageObj.segmentsBetaPage().getGuestInSegmentCount();
		Assert.assertTrue(guestCount == segmentCount2, "Segment guest count is " + guestCount);


//		// login to instance
//		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
//		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
//
//		// navigate to segments
//		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
//		pageObj.segmentsPage().searchAndOpenSegment(segmentName, segmentId2);

		// verifying in segment query by searching user that does not exists in segment
		utils.refreshPage();
		String userEmail = "autoiframe03403403052024@punchh.com";
		Boolean flag1 = pageObj.segmentsPage().verifyUserPresentInSegment(userEmail, "#fbd9d7");
		Assert.assertTrue(flag1, "user not in segment : " + segmentName);
		utils.logPass(userEmail + " user not found in segment : " + segmentName);

		// verifying in segment query by searching user that exists in segment
		utils.refreshPage();
		String userEmail2 = "autoiframe04215719042024qz3a3m@punchh.com";
		Boolean flag2 = pageObj.segmentsPage().verifyUserPresentInSegment(userEmail2, "#ddf3dd");
		Assert.assertTrue(flag2, "user not found in segment : " + segmentName);
		utils.logPass(userEmail2 + " user found in segment : " + segmentName);

		// deleting segment
		query = "DELETE FROM `segment_definitions` WHERE `id` = '" + segmentId2 + "';";
		DBUtils.executeQuery(env, query);

	}

	// Rakhi
	@Test(description = "SQ-T6514 verify in guest targted from campaign when we selet split testing camp then Audience feild showing properly"
			+ "SQ-T6513 verify when we create a guest targted from campaign select audience feild and check count showing properly or not", priority = 5)
	@Owner(name = "Rakhi Rawat")
	public void T6513_GuestTargetedFromCampaignSegmentTypeFunctionality() throws Exception {
		pageObj.utils().logit(sTCName + " ==>" + dataSet);
		String segmentName = CreateDateTime.getUniqueString("Members_Targeted_From_Campaign_Segment");
		String userEmail = dataSet.get("userEmail");
		int count = 0;
		// login to instance
//		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
//		pageObj.instanceDashboardPage().loginToInstance();

		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// navigate to segments
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");

		// create new segment of segment type as Members Targeted from a Campaign
		pageObj.segmentsBetaPage().setSegmentBetaNameAndType(segmentName, "Members Targeted from a Campaign");

		List<String> expPaymentAttributeList = Arrays.asList("Segment Type", "Campaign Name", "Control Group",
				"Duration", "Timezone");
		List<String> actualAttributeNameList = pageObj.segmentsPage().getAttributeNameList();

		boolean attributeFlag = false;
		if (actualAttributeNameList.containsAll(expPaymentAttributeList)) {
			attributeFlag = true;
		}
		Assert.assertTrue(attributeFlag,
				"Failed to verify that user is able to see the following attributes for Members Targeted from a Campaign type segment");
		utils.logPass(
				"The user is able to see the following attributes for Members Targeted from a Campaign type segment");

		// select split test campaign from Campaign Name dropdown
		pageObj.segmentsBetaPage().selectAttribute("Campaign Name", dataSet.get("splitCampaignName"));
		utils.longWaitInSeconds(3);
		// verify options under Audience List dropdown
		List<String> actualAttributeList = pageObj.segmentsPage().getAttributeFromSegmentType("Audience List");
		List<String> expectedtAttributeList = Arrays.asList("All Guests", "Variant A", "Variant B");
		pageObj.utils().logit(actualAttributeList.toString());
		boolean flag = false;
		if (actualAttributeList.containsAll(expectedtAttributeList)) {
			flag = true;
		}
		Assert.assertTrue(flag,
				"Failed to verify that user is able to view expected options in the Audience List dropdown");
		utils.logPass("The user is able to see the expected options in the Audience List dropdown");

		// refresh page
		utils.refreshPage();
		pageObj.segmentsPage().setSegmentName(segmentName);
		pageObj.segmentsBetaPage().selectSegmentType("Members Targeted from a Campaign");
		pageObj.segmentsBetaPage().selectAttribute("Campaign Name", dataSet.get("splitCampaignName"));
		utils.longWaitInSeconds(2);
		pageObj.segmentsBetaPage().setOperator("Audience List", "All Guests");
		pageObj.segmentsBetaPage().setOperator("Control Group", "Excluded");
		pageObj.segmentsBetaPage().setOperator("Duration", "Ever");
		utils.scrollToElement(driver, utils.getLocator("segmentBetaPage.calculateNow1Button"));
		int segmentCount = pageObj.segmentsBetaPage().getSegmentSizeBeforeSave();
		Assert.assertTrue(segmentCount >= count, "Segment count is not greater than 0");
		utils.logPass("Segment count is greater than 0  ie " + segmentCount);
		pageObj.segmentsBetaPage().saveSegment(segmentName);

		int guestCount = pageObj.segmentsBetaPage().getGuestInSegmentCount();
		Assert.assertTrue(guestCount == segmentCount, "Segment guest count is " + guestCount);

		// verifying In_segment query
		utils.refreshPage();
		Boolean flag2 = pageObj.segmentsPage().verifyUserPresentInSegment(userEmail, "#ddf3dd");
		Assert.assertTrue(flag2, "user not found in segment : " + segmentName);
		utils.logPass(userEmail + " user found in segment : " + segmentName);

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
