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
import org.testng.annotations.DataProvider;
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
public class SegmentAndMassCampaignTest {
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

	@Test(description = "SQ-T6049 verify In Profile detail segment favourite location attribute showing count properly || "
			+ "SQ-T6053 Verify Most visited location attribute in profile details segment type", groups = {
					"regression", "dailyrun" }, priority = 0, dataProvider = "TestDataProvider")
	@Owner(name = "Hardik Bhardwaj")
	public void T6049_verifySegmentfavouriteLocation(String SegmentAttribute) throws Exception {
		String segmentId = "";
		segmentName = "AutomationSegment_" + SegmentAttribute + "_" + CreateDateTime.getTimeDateString();
		massCampaignName = "AutomationMassOffer_" + SegmentAttribute + "_" + CreateDateTime.getTimeDateString();

		if (SegmentAttribute.equalsIgnoreCase("Most Visited Location")) {
//			Profile Details Most Visited Location Equal To At Any

			String ProfileDetailsMostVisitedLocationEqualToAtAny_builder_clause = utils.convertToDoubleStringified(
					segmentBuilderClauses.get("ProfileDetailsMostVisitedLocationEqualToAtAny"));

			Response segmentCreationUsingBuilderClauseResponse = pageObj.endpoints().segmentCreationUsingBuilderClause(
					dataSet.get("apiKey"), segmentName, ProfileDetailsMostVisitedLocationEqualToAtAny_builder_clause);
			Assert.assertEquals(segmentCreationUsingBuilderClauseResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
			segmentId = segmentCreationUsingBuilderClauseResponse.jsonPath().get("data.id").toString();

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
			utils.logit("PASS", "Verified that Profile Detail Segment with Attribute Type " + SegmentAttribute
					+ " is Created successfully and have guest count of " + segmentCount);
		}

		if (SegmentAttribute.equalsIgnoreCase("Favorite Location")) {
//			Profile Details Favorite Location Location Equal To At Any

			String ProfileDetailsFavoriteVisitedLocationEqualToAtAny_builder_clause = utils.convertToDoubleStringified(
					segmentBuilderClauses.get("ProfileDetailsFavoriteVisitedLocationEqualToAtAny"));

			Response segmentCreationUsingBuilderClauseResponse = pageObj.endpoints().segmentCreationUsingBuilderClause(
					dataSet.get("apiKey"), segmentName,
					ProfileDetailsFavoriteVisitedLocationEqualToAtAny_builder_clause);
			Assert.assertEquals(segmentCreationUsingBuilderClauseResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
			segmentId = segmentCreationUsingBuilderClauseResponse.jsonPath().get("data.id").toString();

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
			utils.logit("PASS", "Verified that Profile Detail Segment with Attribute Type " + SegmentAttribute
					+ " is Created successfully and have guest count of " + segmentCount);
		}

		// login to instance
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// navigate to segments
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		pageObj.newSegmentHomePage().clickOnSwitchToClassicBtn();
		pageObj.segmentsPage().searchAndOpenSegment(segmentName, segmentId);
		pageObj.segmentsBetaPage().getGuestInSegmentCount();
		userEmail = pageObj.segmentsBetaPage().getSegmentGuestList(dataSet.get("apiKey"), segmentId);
		Assert.assertNotNull(userEmail);

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickSwitchToClassicBtn();
		// Select offer dropdown value
		pageObj.campaignspage().selectOfferdrpValue(OfferdrpValue);
		pageObj.campaignspage().clickNewCampaignBtn();

		// set campaign name and gift type
		pageObj.signupcampaignpage().setCampaignName(massCampaignName);
		pageObj.signupcampaignpage().setGiftType(dataSet.get("giftType"));
		pageObj.signupcampaignpage().setFixedPoints(dataSet.get("fixedPoints"));
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().createWhomDetailsMassCampaign(audiance, segmentName, massCampaignName,
				emailSubject, emailTemplate);
		pageObj.signupcampaignpage().setFrequencyAndTimeInCampaign("Once", dateTime);
		pageObj.signupcampaignpage().scheduleCampaign();

		// run mass offer
		// navigate to Menu -> submenu
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(scheduleName);
		pageObj.schedulespage().findMassCampaignNameandRun(massCampaignName);

		// Timeline validation
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		String campaignName = pageObj.guestTimelinePage().getcampaignNameWithWait(massCampaignName);
		Assert.assertTrue(campaignName.equalsIgnoreCase(massCampaignName), "Campaign name did not matched");
		utils.logPass("Mass Offer campaign with name " + massCampaignName + " is found on user Timeline");

		boolean campaignNotificationStatus = pageObj.guestTimelinePage().verifyCampaignNotification();
		Assert.assertTrue(campaignNotificationStatus, "Campaign notification did not displayed...");
		utils.logPass("Mass Offer campaign with name " + massCampaignName
				+ " is found on user Timeline and Campaign Notification Status is :- " + massCampaignName);

		// deleting segment
		String query = "DELETE FROM `segment_definitions` WHERE `id` = '" + segmentId + "';";
		DBUtils.executeQuery(env, query);

	}

	@DataProvider(name = "TestDataProvider")
	public Object[][] testDataProvider() {

		return new Object[][] {
				// {"SegmentAttribute"},
				{ "Favorite Location" }, { "Most Visited Location" } };

	}

	@SuppressWarnings("static-access")
	@Test(description = "SQ-T6073 verify create profile details segment with Two attribute Birthday(kids), Program Join date and run with mass campaign || "
			+ "SB-T938 verify all segment count present on segment preview page is visible", groups = { "regression",
					"dailyrun" }, priority = 1)
	@Owner(name = "Sachin Bakshi")
	public void verifySegmentWithKIDDOB_ProgramJoinDate() throws Exception {

		massCampaignName = "AutomationMassOffer_PD_Kids(Birthday)_ProgramJoinDate_"
				+ CreateDateTime.getTimeDateString();

		// Profile Details Birthday Kid Ever Program Join Date Ever Chennai - Segment 1
		segmentName = CreateDateTime.getUniqueString("ProfileDetails_BirthdayKid_EverProgramJoinDateEverChennai");
		String ProfileDetails_BirthdayKid_EverProgramJoinDateEverChennai_builder_clause = utils
				.convertToDoubleStringified(
						segmentBuilderClauses.get("ProfileDetailsBirthdayKidEverProgramJoinDateEverChennai"));

		Response segmentCreationUsingBuilderClauseResponse2 = pageObj.endpoints().segmentCreationUsingBuilderClause(
				dataSet.get("apiKey"), segmentName,
				ProfileDetails_BirthdayKid_EverProgramJoinDateEverChennai_builder_clause);
		Assert.assertEquals(segmentCreationUsingBuilderClauseResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String segmentId2 = segmentCreationUsingBuilderClauseResponse2.jsonPath().get("data.id").toString();

		Response segmentCountresponse2 = pageObj.endpoints().getSegmentCount(dataSet.get("apiKey"), segmentId2);
		Assert.assertEquals(segmentCountresponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for Fetch segment count dashboard api");
		utils.logit("Fetch segment count Dashboard API is Successful");
		String segmentCountMessage2 = segmentCountresponse2.jsonPath().getString("message");
		Assert.assertEquals(segmentCountMessage2, "Fetched Count Successfully");
		int segmentCount2 = Integer
				.parseInt((segmentCountresponse2.jsonPath().get("count").toString()).replaceAll(",", ""));
		Assert.assertTrue(segmentCount2 > count, "Segment count is not greater than 0.");
		utils.logit("Segment count is greater than 0  ie " + segmentCount2);
		// deleting segment
		String query = "DELETE FROM `segment_definitions` WHERE `id` = '" + segmentId2 + "';";
		DBUtils.executeQuery(env, query);

		// Profile Details Gender Equal to Male

		segmentName = CreateDateTime.getUniqueString("Profile_Details_GenderEqualtoMale");
		String Profile_Details_GenderEqualtoMale_builder_clause = utils
				.convertToDoubleStringified(segmentBuilderClauses.get("ProfileDetailsGenderEqualtoMale"));

		Response segmentCreationUsingBuilderClauseResponse3 = pageObj.endpoints().segmentCreationUsingBuilderClause(
				dataSet.get("apiKey"), segmentName, Profile_Details_GenderEqualtoMale_builder_clause);
		Assert.assertEquals(segmentCreationUsingBuilderClauseResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String segmentId3 = segmentCreationUsingBuilderClauseResponse3.jsonPath().get("data.id").toString();

		Response segmentCountresponse3 = pageObj.endpoints().getSegmentCount(dataSet.get("apiKey"), segmentId3);
		Assert.assertEquals(segmentCountresponse3.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for Fetch segment count dashboard api");
		utils.logit("Fetch segment count Dashboard API is Successful");
		String segmentCountMessage3 = segmentCountresponse3.jsonPath().getString("message");
		Assert.assertEquals(segmentCountMessage3, "Fetched Count Successfully");
		int segmentCount3 = Integer
				.parseInt((segmentCountresponse3.jsonPath().get("count").toString()).replaceAll(",", ""));
		Assert.assertTrue(segmentCount3 > count, "Segment count is not greater than 0.");
		utils.logit("Segment count is greater than 0  ie " + segmentCount3);

		// deleting segment
		query = "DELETE FROM `segment_definitions` WHERE `id` = '" + segmentId3 + "';";
		DBUtils.executeQuery(env, query);

		// Profile Details Birthday Kid Ever Program Join Date Ever Chennai And Gender
		segmentName = CreateDateTime
				.getUniqueString("ProfileDetails_BirthdayKid_EverProgramJoinDateEverChennaiAndGender");
		String ProfileDetails_BirthdayKid_EverProgramJoinDateEverChennaiAndGender_builder_clause = utils
				.convertToDoubleStringified(
						segmentBuilderClauses.get("ProfileDetailsBirthdayKidEverProgramJoinDateEverChennaiAndGender"));

		Response segmentCreationUsingBuilderClauseResponse = pageObj.endpoints().segmentCreationUsingBuilderClause(
				dataSet.get("apiKey"), segmentName,
				ProfileDetails_BirthdayKid_EverProgramJoinDateEverChennaiAndGender_builder_clause);
		Assert.assertEquals(segmentCreationUsingBuilderClauseResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String segmentId = segmentCreationUsingBuilderClauseResponse.jsonPath().get("data.id").toString();

		Response segmentCountresponse = pageObj.endpoints().getSegmentCount(dataSet.get("apiKey"), segmentId);
		Assert.assertEquals(segmentCountresponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for Fetch segment count dashboard api");
		utils.logit("Fetch segment count Dashboard API is Successful");
		String segmentCountMessage = segmentCountresponse.jsonPath().getString("message");
		Assert.assertEquals(segmentCountMessage, "Fetched Count Successfully");
		int segmentCount = Integer
				.parseInt((segmentCountresponse.jsonPath().get("count").toString()).replaceAll(",", ""));
		Assert.assertTrue(segmentCount > count, "Segment count is not greater than 0.");
		utils.logit("Segment count is greater than 0  ie " + segmentCount);

		// login to instance
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// navigate to segments
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		pageObj.newSegmentHomePage().clickOnSwitchToClassicBtn();

		pageObj.segmentsPage().searchAndOpenSegment(segmentName, segmentId);
//		int guestInSegmentCount = pageObj.segmentsBetaPage().getGuestInSegmentCount();
		int getLeftSideSegmentSize1 = pageObj.segmentsBetaPage().getLeftSideSegmentSize("1");
		int guestInSegmentCount = pageObj.segmentsBetaPage().getGuestInSegmentCount();
		int getLeftSideSegmentSize2 = pageObj.segmentsBetaPage().getLeftSideSegmentSize("2");
		int getLeftSideSegmentSize3 = pageObj.segmentsBetaPage().getLeftSideSegmentSize("3");
		int getLeftSideSegmentSize4 = pageObj.segmentsBetaPage().getLeftSideSegmentSize("4");
		int getLeftSideSegmentSize5 = pageObj.segmentsBetaPage().getLeftSideSegmentSize("5");
		
		Assert.assertEquals(segmentCount, guestInSegmentCount,
				"Segment count before and after save are not matched");
		utils.logit("PASS", "Segment count before and after save are matched: " + guestInSegmentCount);

		Assert.assertEquals(segmentCount2, getLeftSideSegmentSize2);
		Assert.assertEquals(getLeftSideSegmentSize2, getLeftSideSegmentSize3);
		utils.logit("PASS",
				"getLeftSideSegmentSize2 and getLeftSideSegmentSize3 are equal: " + getLeftSideSegmentSize3);

		Assert.assertEquals(segmentCount3, getLeftSideSegmentSize4);
		Assert.assertEquals(getLeftSideSegmentSize4, getLeftSideSegmentSize5);
		utils.logit("PASS",
				"getLeftSideSegmentSize4 and getLeftSideSegmentSize5 are equal: " + getLeftSideSegmentSize5);
		
//		String segmentId = pageObj.segmentsPage().getSegmentID();
		userEmail = pageObj.segmentsBetaPage().getSegmentGuestList(dataSet.get("apiKey"), segmentId);
		utils.logPass("Verified that Profile Detail Segment with Attribute Type "
				+ " is Created successfully and have guest count of " + segmentCount);

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickSwitchToClassicBtn();
		// Select offer dropdown value
		pageObj.campaignspage().selectOfferdrpValue(OfferdrpValue);
		pageObj.campaignspage().clickNewCampaignBtn();

		// set campaign name and gift type
		pageObj.signupcampaignpage().setCampaignName(massCampaignName);
		pageObj.signupcampaignpage().setGiftType(dataSet.get("giftType"));
		pageObj.signupcampaignpage().setFixedPoints(dataSet.get("fixedPoints"));
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().createWhomDetailsMassCampaign(audiance, segmentName, massCampaignName,
				emailSubject, emailTemplate);
		pageObj.signupcampaignpage().setFrequencyAndTimeInCampaign("Once", dateTime);
		pageObj.signupcampaignpage().scheduleCampaign();

		// run mass offer
		// navigate to Menu -> submenu
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(scheduleName);
		pageObj.schedulespage().findMassCampaignNameandRun(massCampaignName);

		// Timeline validation
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		String campaignName = pageObj.guestTimelinePage().getcampaignNameWithWait(massCampaignName);
		Assert.assertTrue(campaignName != null && campaignName.equalsIgnoreCase(massCampaignName),
				"Campaign name did not matched");
		utils.logPass("Mass Offer campaign with name " + massCampaignName + " is found on user Timeline");

		boolean campaignNotificationStatus = pageObj.guestTimelinePage().verifyCampaignNotification();
		Assert.assertTrue(campaignNotificationStatus, "Campaign notification did not displayed...");
		utils.logPass("Mass Offer campaign with name " + massCampaignName
				+ " is found on user Timeline and Campaign Notification Status is :- " + massCampaignName);

		// deleting segment
		query = "DELETE FROM `segment_definitions` WHERE `id` = '" + segmentId + "';";
		DBUtils.executeQuery(env, query);

	}

	@SuppressWarnings("static-access")
	@Test(description = "SQ-T6319: Create segment with location and location group(which have locations) and verify count showing properly or not; "
			+ "SQ-T6320: verify in profile detail segment use favourite location attribute and use other segment an attributes and make some combinations and count showing properly", groups = {
					"regression" }, priority = 2, dataProvider = "T6319_testDataProvider")
	@Owner(name = "Vaibhav Agnihotri")
	public void T6319_multiAttributeMultiLocationSegment(String segmentType1, String attributeType1,
			String segmentType2, String attributeType2) throws Exception {
		segmentName = "MultiAttributeSegment_" + CreateDateTime.getTimeDateString();
//		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
//		pageObj.instanceDashboardPage().loginToInstance();

		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Create Profile Detail segment
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		pageObj.newSegmentHomePage().clickOnSwitchToClassicBtn();
		pageObj.segmentsBetaPage().setSegmentBetaName(segmentName);
		pageObj.segmentsBetaPage().selectSegmentType(segmentType1);
		pageObj.segmentsBetaPage().selectAttribute(attributeType1);
		pageObj.segmentsBetaPage().setOperator("Operator", "Equal to");
		pageObj.segmentsBetaPage().selectAttribute("Location(s)", "Automation - 1");
		pageObj.segmentsBetaPage().selectAttributeValueOnly("Location(s)", "Location group automation");
		int segmentCount = pageObj.segmentsBetaPage().getSegmentSizeBeforeSave();
		Assert.assertTrue(segmentCount > 0, "Segment count is not greater than 0");
		// pageObj.segmentsBetaPage().saveSegment(segmentName);
		pageObj.segmentsPage().saveAndShowSegment();
		String segmentId = pageObj.segmentsPage().getSegmentID();

		// Verify segment count and in-segment
		int guestInSegmentCount = pageObj.segmentsBetaPage().getGuestsInSegmentCount();
		Assert.assertTrue(guestInSegmentCount > 0, "Guest in segment count is not greater than 0");
		userEmail = pageObj.segmentsBetaPage().getSegmentGuestList(dataSet.get("apiKey"), segmentId);
		utils.logPass("Verified that " + segmentType1 + " segment with Attribute Type " + attributeType1
				+ " is created. Guest count: " + guestInSegmentCount + ". In-segment for user email: "
				+ userEmail);

		// Add attribute to existing segment and verify segment count and in-segment
		pageObj.segmentsBetaPage().segmentEllipsisOptions("Edit");
		pageObj.segmentsBetaPage().selectSegmentType(segmentType2);
		pageObj.segmentsBetaPage().setAndOrNiCondition("AND");
		pageObj.segmentsBetaPage().setAttributeList(attributeType2);
		pageObj.segmentsBetaPage().setOperatorText("Operator", "Equal to");
		pageObj.segmentsBetaPage().setSelectedValue("Location(s)", "Automation - 1");

		// Add another attribute to reduce the segment size
		pageObj.segmentsBetaPage().selectSegmentType("Profile Details");
		pageObj.segmentsBetaPage().setAndOrNiCondition("AND");
		pageObj.segmentsBetaPage().setAttributeList("Gender");
		pageObj.segmentsBetaPage().setOperatorText("Operator", "Equal to");
		pageObj.segmentsBetaPage().selectAttribute("Gender", "Male");
		// pageObj.segmentsBetaPage().saveSegment(segmentName);
		pageObj.segmentsPage().saveAndShowSegment();
		guestInSegmentCount = pageObj.segmentsBetaPage().getGuestInSegmentCount();
		Assert.assertTrue(guestInSegmentCount > 0, "Guest in segment count is not greater than 0");
		userEmail = pageObj.segmentsBetaPage().getSegmentGuestList(dataSet.get("apiKey"), segmentId);
		utils.logPass("Verified that " + segmentType2 + " segment with Attribute Type " + attributeType2
				+ " is added to existing segment. Guest count: " + guestInSegmentCount + ". In-segment for user email: "
				+ userEmail);

		// Run a mass campaign with the saved segment
		pageObj.segmentsBetaPage().segmentEllipsisOptions("Create Mass Gifting");
		utils.switchToWindow();
		massCampaignName = "MassCampaign_" + segmentName;
		String dateTime = CreateDateTime.getCurrentDate() + " 11:00 PM";
		String pointsToGift = dataSet.get("pointsToGift");
		pageObj.signupcampaignpage().setCampaignName(massCampaignName);
		pageObj.signupcampaignpage().setGiftType(dataSet.get("giftType"));
		pageObj.signupcampaignpage().setFixedPoints(pointsToGift);
		pageObj.signupcampaignpage().clickNextBtn();
		pageObj.signupcampaignpage().createWhomDetailsMassCampaign(audiance, segmentName, massCampaignName,
				emailSubject, emailTemplate);
		pageObj.signupcampaignpage().setFrequencyAndTimeInCampaign("Once", dateTime);
		pageObj.signupcampaignpage().scheduleCampaign();
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().findMassCampaignNameandRun(massCampaignName);

		// Timeline validation
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		String campaignName = pageObj.guestTimelinePage().getcampaignNameWithWait(massCampaignName);
		Assert.assertTrue(campaignName.equalsIgnoreCase(massCampaignName), "Campaign name did not matched");
		boolean campaignNotificationStatus = pageObj.guestTimelinePage().verifyCampaignNotification();
		Assert.assertTrue(campaignNotificationStatus, "Campaign notification did not displayed...");
		utils.logPass("Mass Offer campaign with name " + massCampaignName
				+ " is found on user Timeline and Campaign Notification Status is :- " + campaignNotificationStatus);

		// deleting segment
		String query = "DELETE FROM `segment_definitions` WHERE `id` = '" + segmentId + "';";
		DBUtils.executeQuery(env, query);

	}

	@DataProvider(name = "T6319_testDataProvider")
	public Object[][] T6319_testDataProvider() {
		return new Object[][] {
				{ "Profile Details", "Most Visited Location", "Profile Details", "Last Visited Location" },
				{ "Profile Details", "Favorite Location", "Checkins", "Checkin Location" } };
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