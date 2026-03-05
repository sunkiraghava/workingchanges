package com.punchh.server.segmentsTest;

import java.lang.reflect.Method;
import java.text.ParseException;
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
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class SegmentCampaignTest {
	public WebDriver driver;
	private Properties prop;
	private PageObj pageObj;
	private String sTCName;
	private String env;
	private String baseUrl;
	private Utilities utils;
	private static Map<String, String> dataSet;
	String run = "ui";
	private String userEmail, segmentName, recallCampaignName;

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

		prop = Utilities.loadPropertiesFile("config.properties");
		sTCName = method.getName();
		utils = new Utilities(driver);
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		pageObj.utils().logit(sTCName + " ==>" + dataSet);
		// move to all business page
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);

	}

	@Test(description = "SQ-T5975 Verify In Recall  campaign when we click on Add new segment it navigate to Beta segment create page", groups = {
			"regression", "dailyrun" }, priority = 0)
	@Owner(name = "Hardik Bhardwaj")
	public void T5975_verifySegmentInRecallCampaign() throws Exception {

		recallCampaignName = "Automation Recall Campaign" + CreateDateTime.getTimeDateString();
		segmentName = CreateDateTime.getUniqueString("Profile_Detail_Segment_Zipcode_");
		String zipCode = dataSet.get("zipCode");

//		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
//		pageObj.instanceDashboardPage().loginToInstance();

		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().navigateToTabs("Miscellaneous Config");
		pageObj.dashboardpage().checkUncheckAnyFlag("Enable recall campaigns bulk segment", "uncheck");
		// Click Guest menu Link
		pageObj.menupage().navigateToSubMenuItem("Guests", "Lapsed Guests Trend");
		String days = pageObj.lapsedguestPage().getLapsedGuestsDetails("2"); // 1 days user

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		// Select offer dropdown value
		pageObj.campaignspage().selectOngoingdrpValue("Recall");
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().setCampaignName(recallCampaignName);
		pageObj.signupcampaignpage().setGiftType("Gift Redeemable");
		pageObj.signupcampaignpage().setGiftReason(recallCampaignName);
		pageObj.signupcampaignpage().setRedeemable("Automation 12003");
		pageObj.signupcampaignpage().clickNextBtn();
		String campaignid = pageObj.signupcampaignpage().getCampaignid();
		pageObj.signupcampaignpage().setLapseDays(days);
		pageObj.segmentsPage().clickOnAddNewSegmentInCampaign();
		pageObj.newSegmentHomePage().verifyCreateSegmentPage();
		pageObj.segmentsPage().setSegmentName(segmentName);

		pageObj.segmentsBetaPage().selectSegmentType("Profile Details");
		pageObj.segmentsBetaPage().selectAttribute("Zip Code");
		pageObj.segmentsBetaPage().setOperator("Operator", "In");
		pageObj.segmentsBetaPage().setValue(zipCode);
		pageObj.segmentsPage().saveAndShowSegmentBtn();
		String segmentId = pageObj.segmentsPage().getSegmentID();
		Response response = pageObj.endpoints().getSegmentCount(dataSet.get("apiKey"), segmentId);
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for Fetch segment count dashboard api");
		utils.logPass("Fetch segment count Dashboard API is Successful");
		String cnt = response.jsonPath().get("count");
		String cnt1 = cnt.replaceAll(",", "");
		int count = Integer.parseInt(cnt1);
		Assert.assertNotEquals(count, 0, "Segment guest count from API and UI does not match");
		utils.logit("Segment guest count for Segment " + segmentName + " is " + count);
		userEmail = pageObj.segmentsBetaPage().getSegmentGuestList(dataSet.get("apiKey"), segmentId);

		// navigate to Segment tab on user timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().navigateToTabs("Segments");
		boolean isGreenColourBGVisible = pageObj.segmentsPage().guestTimelineSegmentColor(segmentName, "#dff0d8");
		Assert.assertTrue(isGreenColourBGVisible, "Expected Green colour NOT matched for segment");
		utils.logPass("Segment highlighted with green color on Guest Timeline");

		// delete recall campaign
		pageObj.utils().deleteCampaignFromDb(recallCampaignName, env);

		// delete segment
		String query = "DELETE FROM `segment_definitions` WHERE `id` = '" + segmentId + "';";
		DBUtils.executeQuery(env, query);

	}

	@Test(description = "SQ-T3643 Verify the response of custom segment API and date time format || "
			+ "SQ-T3644 Verify that User is able to Add users via API into an existing empty custom segment || "
			+ "SQ-T4642 Verify custom segment export properly or not || "
			+ "SQ-T4645 Verify that custom segment combined with other segment is giving the count", groups = {
					"regression", "dailyrun" }, priority = 1)
	@Owner(name = "Hardik Bhardwaj")
	public void T3643_createCustomSegment() throws InterruptedException, ParseException {

		// user signup using pos signup api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response resp = pageObj.endpoints().posSignUp(userEmail, dataSet.get("locationkey"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp.getStatusCode(), "Status code 200 did not matched for pos signup api");
		String userID = resp.jsonPath().get("id").toString();

		// create custom segment
		segmentName = CreateDateTime.getUniqueString("Custom_Segment_");
		Response createSegmentResponse = pageObj.endpoints().createCustomSegment(segmentName, dataSet.get("apiKey"));
		pageObj.apiUtils().verifyCreateResponse(createSegmentResponse, "Custom segment created");
		utils.logPass("custom segment created " + segmentName);

		int customSegmentId = createSegmentResponse.jsonPath().get("custom_segment_id");
		Assert.assertNotNull(customSegmentId, "Custom Segment ID is null");

		String segName = createSegmentResponse.jsonPath().get("name").toString();
		Assert.assertEquals(segName, segmentName, "Segment Name is not matching with expected Name for custom segment");
		utils.logit(
				"Verified that  Name of the custom segment as given by the admin user while creating the custom segment.");

		String segDescription = createSegmentResponse.jsonPath().get("description").toString();
		boolean descriptionFlag = utils.textContains(segDescription, "Auto custom segment " + segName);
		Assert.assertTrue(descriptionFlag, "Description is not matching with expected description for custom segment");
		utils.logit(
				"Verified that Description of the custom segment as given by the admin user while creating the custom segment.");

		String segCreationDate = createSegmentResponse.jsonPath().get("created_at").toString();
		boolean creationDate = CreateDateTime.DateValidation(segCreationDate);
		Assert.assertTrue(creationDate, "Segment Creation date is not in correct format");
		utils.logit(
				"Verified that created_at stringDatetime when custom segment was created in system in ISO 8601 format.");

		String segUpdationDate = createSegmentResponse.jsonPath().get("updated_at").toString();
		boolean updationDate = CreateDateTime.DateValidation(segUpdationDate);
		Assert.assertTrue(updationDate, "Segment Creation date is not in correct format");
		utils.logit(
				"Verified that updated_at stringDatetime when custom segment was created in system in ISO 8601 format.");

		// add user to custom segment
		Response addUserResponse = pageObj.endpoints().addUserToCustomSegment(customSegmentId, userEmail,
				dataSet.get("apiKey"));
		Assert.assertEquals(addUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED, "Status code 201 did not matched");
		utils.logPass("user added to custom segment " + segmentName);

		int customSegmentId1 = addUserResponse.jsonPath().get("custom_segment_id");
		Assert.assertEquals(customSegmentId, customSegmentId1, "Custom Segment ID is not matching");
		utils.logit("Verified that Custom Segment ID is matching");

		String email = addUserResponse.jsonPath().get("email").toString();
		Assert.assertEquals(email, userEmail, "User Email is not matching with expected email added in custom segment");
		utils.logit("Verified that User Email is matching with expected email added in custom segment");

		String user_id = addUserResponse.jsonPath().get("user_id").toString();
		Assert.assertEquals(userID, user_id, "user_id is not matching with expected user_id added in custom segment");
		utils.logit("Verified that user_id is matching with expected user_id added in custom segment");

		segCreationDate = addUserResponse.jsonPath().get("created_at").toString();
		creationDate = CreateDateTime.DateValidation(segCreationDate);
		Assert.assertTrue(creationDate, "Segment Creation date is not in correct format");
		utils.logit(
				"Verified that created_at stringDatetime when custom segment was created in system in ISO 8601 format.");

		segUpdationDate = addUserResponse.jsonPath().get("updated_at").toString();
		updationDate = CreateDateTime.DateValidation(segUpdationDate);
		Assert.assertTrue(updationDate, "Segment Creation date is not in correct format");
		utils.logit(
				"Verified that updated_at stringDatetime when custom segment was created in system in ISO 8601 format.");

		// Login to instance
//		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
//		pageObj.instanceDashboardPage().loginToInstance();

		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// navigate to custom segment
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		pageObj.newSegmentHomePage().clickOnSwitchToClassicBtn();
		String segmentId = createSegmentResponse.jsonPath().get("custom_segment_id").toString();

//		// get segment guest count
//		int count = pageObj.segmentsPage().getSegmentCountPolling(dataSet.get("apiKey"), segmentId, 200);
//		Assert.assertNotEquals(count, 0, "segment guest count from api and UI is not matching");
//		pageObj.utils().logit("Segment guest count for Segment " + segmentName + " is " + count);
//		pageObj.utils().logit("Segment guest count for Segment " + segmentName + " is " + count);

		// verifying In_segment query using API
		Response userInSegmentResp2 = pageObj.endpoints().userInSegment(userEmail, dataSet.get("apiKey"), segmentId);
		Assert.assertEquals(userInSegmentResp2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 201 did not matched for auth user signup api");
		utils.logPass("Verified that user " + userEmail + " is present in Segment");
		String result = userInSegmentResp2.jsonPath().get("result").toString();
		Assert.assertEquals(result, "true", "Guest is present in segment");
		utils.logPass("Verified that status of  " + userEmail + " is present in Segment");

		// segment export and verify logs
		String exportName = "Export_Report_" + segmentName;
		pageObj.segmentsPage().searchAndOpenSegment(segmentName, segmentId);
		pageObj.segmentsBetaPage().segmentEllipsisOptions("Export");
		utils.switchToWindow();
		String message = pageObj.schedulePage().segmentExportSchedule("N/A", exportName, "Immediately");
		Assert.assertEquals(message, "Schedule created successfully.",
				"Message did not contain expected text for segment export schedule");
		utils.logPass("Successfully scheduled segment export for " + segmentName);

		// delete segment
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		pageObj.segmentsPage().searchAndOpenSegment(segmentName, segmentId);
		pageObj.segmentsPage().deleteSegment(segmentName);
	}

	// Author: Shaleen
	@Test(description = "SQ-T6441 : Create Profile detail segment with age_verified attribute and run with Trigger based campaign", priority = 3)
	@Owner(name = "Shaleen Gupta")
	public void T6441_profileDetailSegmentAgeVerified() throws Exception {
//			pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
//			pageObj.instanceDashboardPage().loginToInstance();

		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Create a segment with 'Age Verified' attribute
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		pageObj.newSegmentHomePage().clickOnSwitchToClassicBtn();
		String segmentName = CreateDateTime.getUniqueString("ProfileDetailAgeVerified_");
		pageObj.segmentsBetaPage().setSegmentBetaName(segmentName);
		pageObj.segmentsBetaPage().selectSegmentType("Profile Details");
		pageObj.segmentsBetaPage().setAttribute("Age Verified Status");
		pageObj.segmentsBetaPage().saveSegment(segmentName);
		String segmentId = pageObj.segmentsPage().getSegmentID();

		// create user with age_verified status as true
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api1 signup");
		String userID = signUpResponse.jsonPath().get("id").toString();
		String currentDateTime = CreateDateTime.getCurrentDateTimeInUtc() + "+05:30";
		Response ageVerificationResponse = pageObj.endpoints().kouponMediaAgeVerification(dataSet.get("slug"),
				dataSet.get("kouponMedia_authorizationToken"), userID, "true", currentDateTime);
		Assert.assertEquals(ageVerificationResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched");
		utils.logPass("Successfully created user with age_verified status as true");

		// verify entry in db
		String query = "SELECT verified FROM age_verifications WHERE user_id = '" + userID + "' ;";
		String expColValue = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query,
				"verified", 3);
		Assert.assertEquals(expColValue, "1", "Age verification status is not true in DB");
		pageObj.utils().logit("Verified that age verification status is true in DB for user: " + userID);

		// verify user in segment
		utils.refreshPage();
		utils.waitTillPagePaceDone();
		Response response1 = pageObj.endpoints().userInSegment(userEmail, dataSet.get("apiKey"), segmentId);
		Assert.assertEquals(response1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched");
		Assert.assertTrue(response1.jsonPath().getBoolean("result"), "User is not present in segment");
		utils.logPass("Verified that user " + userEmail + " is present in segment: " + segmentName);

		// get random user from segment
		utils.refreshPage();
		utils.waitTillPagePaceDone();
		int guestCountInSegment = pageObj.segmentsBetaPage().getGuestInSegmentCount();
		Assert.assertTrue(guestCountInSegment > 0, "Guest count in segment is not greater than 0");
		String randomUserFromSegment = pageObj.segmentsBetaPage().getRandomGuestFromSegmentList(dataSet.get("apiKey"),
				segmentId, 10);

		// create mass notification campaign
		String massCampaignName = CreateDateTime.getUniqueString("MassNotification_");
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.campaignspage().selectMessagedrpValue("Mass Notification");
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().setCampaignName(massCampaignName);
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().setAudienceType("Segment");
		pageObj.signupcampaignpage().setSegmentType(segmentName);
		String pushNotification = massCampaignName + "_Push_Notification";
		pageObj.signupcampaignpage().setPushNotification(pushNotification);
		pageObj.signupcampaignpage().clickNextButton();

		pageObj.signupcampaignpage().createWhenDetailsMassCampaign("Once",
				CreateDateTime.getCurrentDate() + " 10:00 PM");
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "Success message did not displayed....");
		// run mass offer schedule
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().findMassCampaignNameandRun(massCampaignName);

		// Timeline validation
		pageObj.instanceDashboardPage().navigateToGuestTimeline(randomUserFromSegment);
		/*boolean campaignNotificationStatus = pageObj.guestTimelinePage().checkPNForCampaign(massCampaignName,
				pushNotification);
		Assert.assertTrue(campaignNotificationStatus, "Campaign notification is not visible in guest timeline");*/
		String campaignName = pageObj.guestTimelinePage().getcampaignNameWithWait(massCampaignName);
		Assert.assertTrue(campaignName.equalsIgnoreCase(massCampaignName), "Campaign name did not matched");
		utils.logPass("Verified that campaign notification is visible in guest timeline for user: " + userEmail);

		// delete user from age_verifications table
		String query2 = "DELETE FROM age_verifications WHERE user_id = '" + userID + "' ;";
		int response2 = DBUtils.executeUpdateQuery(env, query2);
		Assert.assertEquals(response2, 1, "User is not deleted from age_verifications table");
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