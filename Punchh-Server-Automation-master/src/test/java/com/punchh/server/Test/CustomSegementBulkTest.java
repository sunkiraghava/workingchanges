package com.punchh.server.Test;

import java.lang.reflect.Method;
import java.util.List;
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
import com.punchh.server.apiConfig.ApiResponseJsonSchema;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class CustomSegementBulkTest {
	private static Logger logger = LogManager.getLogger(CustomSegementBulkTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String sTCName;
	private String baseUrl;
	String blankString = "";
	private String env, run = "ui";
	private static Map<String, String> dataSet;
	Properties prop;
	Utilities utils;

	// cannot be run on single browser instance
	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
		prop = Utilities.loadPropertiesFile("config.properties");
		driver = new BrowserUtilities().launchBrowser();
		sTCName = method.getName();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==>" + dataSet);
		utils = new Utilities(driver);
	}

	@Test(description = "SQ-T4647 Verify that signed up users can be added into the custom segment via bulk add api")
	public void T4647_customSegmentBulkApi() throws InterruptedException {

		// create custom segment
		String segName = "CustomSeg" + CreateDateTime.getTimeDateString();
		Response createSegmentResponse = pageObj.endpoints().createCustomSegment(segName, dataSet.get("apiKey"));
		pageObj.apiUtils().verifyCreateResponse(createSegmentResponse, "Custom segment created");
		Assert.assertEquals(createSegmentResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED, "Status code 201 did not matched");
		utils.logPass("custom segment created " + segName);
		int customSegmentId = createSegmentResponse.jsonPath().get("custom_segment_id");
		utils.logit("segment id is -- " + customSegmentId);

		// add user to custom segment
		String csvFilePath = System.getProperty("user.dir") + "/resources/Images/PP_CSV_signedupUser_upload.csv";
		Response addUserResponse = pageObj.endpoints().addBulkUserToCustomSegment(segName, customSegmentId, csvFilePath,
				dataSet.get("apiKey"));
		Assert.assertEquals(addUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched");
		utils.logPass("Bulk user added to custom segment " + segName);

		// Bulk add user to custom segment with invalid segment name
		utils.logit("== Bulk Add Users to a Custom Segment with invalid segment name ==");
		Response addUserInvalidSegmentNameResponse = pageObj.endpoints().addBulkUserToCustomSegment(" ",
				customSegmentId, csvFilePath, dataSet.get("apiKey"));
		Assert.assertEquals(addUserInvalidSegmentNameResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY);
		boolean isAddUserInvalidSegmentNameSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.dashboardNameErrorSchema, addUserInvalidSegmentNameResponse.asString());
		Assert.assertTrue(isAddUserInvalidSegmentNameSchemaValidated,
				"Bulk Add Users to a Custom Segment with invalid segment name Schema Validation failed");
		String addUserInvalidSegmentNameMsg = addUserInvalidSegmentNameResponse.jsonPath().get("errors.name[0]")
				.toString();
		Assert.assertEquals(addUserInvalidSegmentNameMsg, "can't be blank");
		utils.logPass("Bulk Add Users to a Custom Segment with invalid segment name is unsuccessful");

		// Bulk add user to custom segment with invalid admin authorization
		utils.logit("== Bulk Add Users to a Custom Segment with invalid admin authorization ==");
		Response addUserInvalidAdminAuthorizationResponse = pageObj.endpoints().addBulkUserToCustomSegment(segName,
				customSegmentId, csvFilePath, "1");
		Assert.assertEquals(addUserInvalidAdminAuthorizationResponse.getStatusCode(), 401);
		boolean isAddUserInvalidAdminAuthorizationSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, addUserInvalidAdminAuthorizationResponse.asString());
		Assert.assertTrue(isAddUserInvalidAdminAuthorizationSchemaValidated,
				"Bulk Add Users to a Custom Segment with invalid admin authorization Schema Validation failed");
		String addUserInvalidAdminAuthorizationMsg = addUserInvalidAdminAuthorizationResponse.jsonPath().get("error")
				.toString();
		Assert.assertEquals(addUserInvalidAdminAuthorizationMsg, "You need to sign in or sign up before continuing.");
		utils.logPass("Bulk Add Users to a Custom Segment with invalid admin authorization is unsuccessful");

		// Bulk add user to custom segment with invalid custom segment Id
		utils.logit("== Bulk Add Users to a Custom Segment with invalid Custom segment Id ==");
		Response addUserInvalidCustomSegmentIdResponse = pageObj.endpoints().addBulkUserToCustomSegment(segName, 0,
				csvFilePath, dataSet.get("apiKey"));
		Assert.assertEquals(addUserInvalidCustomSegmentIdResponse.getStatusCode(), 404);
		boolean isAddUserInvalidCustomSegmentIdSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2NotFoundErrorSchema, addUserInvalidCustomSegmentIdResponse.asString());
		Assert.assertTrue(isAddUserInvalidCustomSegmentIdSchemaValidated,
				"Bulk Add Users to a Custom Segment with invalid Custom segment Id Schema Validation failed");
		String addUserInvalidCustomSegmentIdMsg = addUserInvalidCustomSegmentIdResponse.jsonPath()
				.get("errors.not_found").toString();
		Assert.assertEquals(addUserInvalidCustomSegmentIdMsg, "Custom Segment not found.");
		utils.logPass("Bulk Add Users to a Custom Segment with invalid Custom segment Id is unsuccessful");

		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// navigate to custom segment
		utils.longWaitInSeconds(2);
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		pageObj.newCamHomePage().clickSwitchToClassicBtn();
		String segmentId = createSegmentResponse.jsonPath().get("custom_segment_id").toString();
		logger.info(segmentId);
		pageObj.segmentsPage().searchAndOpenSegment(segName, segmentId);

//		Boolean flag = pageObj.segmentsPage().verifyUserPresentInSegment(dataSet.get("customUserEmail"), "#ddf3dd");
//		Assert.assertTrue(flag, "user not found in segment : " + segName);
		// verifying In_segment query using API
		Response userInSegmentResp = pageObj.endpoints().userInSegment(dataSet.get("customUserEmail"),
				dataSet.get("apiKey"), segmentId);
		Assert.assertEquals(userInSegmentResp.getStatusCode(), 200,
				"Status code 200 did not matched for auth user signup api");
		Assert.assertTrue(userInSegmentResp.asString().contains("true"), "User not found in segment : " + segName);
		utils.logPass(dataSet.get("customUserEmail") + " user found in segment : " + segName);

		// remove user from custom segment
		String csvFilePath1 = System.getProperty("user.dir") + "/resources/Images/PP_CSV_signedupUser_upload.csv";
		Response removeUserResponse = pageObj.endpoints().removeBulkUserfromCustomSegment(segName, customSegmentId,
				csvFilePath1, dataSet.get("apiKey"));
		Assert.assertEquals(removeUserResponse.getStatusCode(), 200, "Status code 200 did not matched");
		utils.logPass("bulk user removed from custom segment " + segName);

		// Bulk remove user from custom segment with invalid segment name
		utils.logit("== Bulk Remove Users from a Custom Segment with invalid segment name ==");
		Response removeUserInvalidSegmentNameResponse = pageObj.endpoints().removeBulkUserfromCustomSegment(" ",
				customSegmentId, csvFilePath1, dataSet.get("apiKey"));
		Assert.assertEquals(removeUserInvalidSegmentNameResponse.getStatusCode(), 422);
		boolean isRemoveUserInvalidSegmentNameSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.dashboardNameErrorSchema, removeUserInvalidSegmentNameResponse.asString());
		Assert.assertTrue(isRemoveUserInvalidSegmentNameSchemaValidated,
				"Bulk Remove Users from a Custom Segment with invalid segment name Schema Validation failed");
		String removeUserInvalidSegmentNameMsg = removeUserInvalidSegmentNameResponse.jsonPath().get("errors.name[0]")
				.toString();
		Assert.assertEquals(removeUserInvalidSegmentNameMsg, "can't be blank");
		utils.logPass("Bulk Remove Users from a Custom Segment with invalid segment name is unsuccessful");

		// Bulk remove user from custom segment with invalid admin authorization
		utils.logit("== Bulk Remove Users from a Custom Segment with invalid admin authorization ==");
		Response removeUserInvalidAdminAuthorizationResponse = pageObj.endpoints()
				.removeBulkUserfromCustomSegment(segName, customSegmentId, csvFilePath1, "1");
		Assert.assertEquals(removeUserInvalidAdminAuthorizationResponse.getStatusCode(), 401);
		boolean isRemoveUserInvalidAdminAuthorizationSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, removeUserInvalidAdminAuthorizationResponse.asString());
		Assert.assertTrue(isRemoveUserInvalidAdminAuthorizationSchemaValidated,
				"Bulk Remove Users from a Custom Segment with invalid admin authorization Schema Validation failed");
		String removeUserInvalidAdminAuthorizationMsg = removeUserInvalidAdminAuthorizationResponse.jsonPath()
				.get("error").toString();
		Assert.assertEquals(removeUserInvalidAdminAuthorizationMsg,
				"You need to sign in or sign up before continuing.");
		utils.logPass("Bulk Remove Users from a Custom Segment with invalid admin authorization is unsuccessful");

		// Bulk remove user from custom segment with invalid custom segment Id
		utils.logit("== Bulk Remove Users from a Custom Segment with invalid Custom segment Id ==");
		Response removeUserInvalidCustomSegmentIdResponse = pageObj.endpoints().removeBulkUserfromCustomSegment(segName,
				0, csvFilePath1, dataSet.get("apiKey"));
		Assert.assertEquals(removeUserInvalidCustomSegmentIdResponse.getStatusCode(), 404);
		boolean isRemoveUserInvalidCustomSegmentIdSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2NotFoundErrorSchema, removeUserInvalidCustomSegmentIdResponse.asString());
		Assert.assertTrue(isRemoveUserInvalidCustomSegmentIdSchemaValidated,
				"Bulk Remove Users from a Custom Segment with invalid Custom segment Id Schema Validation failed");
		String removeUserInvalidCustomSegmentIdMsg = removeUserInvalidCustomSegmentIdResponse.jsonPath()
				.get("errors.not_found").toString();
		Assert.assertEquals(removeUserInvalidCustomSegmentIdMsg, "Custom Segment not found.");
		utils.logPass("Bulk Remove Users from a Custom Segment with invalid Custom segment Id is unsuccessful");

		// navigate to custom segment
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		String segmentId1 = createSegmentResponse.jsonPath().get("custom_segment_id").toString();
		pageObj.segmentsPage().searchAndOpenSegment(segName, segmentId1);

//		Boolean flag1 = pageObj.segmentsPage().verifyUserPresentInSegment(dataSet.get("customUserEmail"), "#fbd9d7");
//		Assert.assertTrue(flag1, "user found in segment : " + segName);
		// verifying In_segment query using API
		Response userInSegmentResp1 = pageObj.endpoints().userInSegment(dataSet.get("customUserEmail"),
				dataSet.get("apiKey"), segmentId);
		Assert.assertTrue(userInSegmentResp1.asString().contains("false"), "User found in segment : " + segName);
		utils.logPass(dataSet.get("customUserEmail") + " user not found in custom segment :" + segName);

		// create custom segment
		String seg_Name = "CustomSeg" + CreateDateTime.getTimeDateString();
		Response createSegResponse = pageObj.endpoints().createCustomSegment(seg_Name, dataSet.get("apiKey"));
		pageObj.apiUtils().verifyCreateResponse(createSegResponse, "Custom segment created");
		Assert.assertEquals(createSegResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED, "Status code 201 did not matched");
		utils.logPass("custom segment created " + seg_Name);
		int customSegtId = createSegResponse.jsonPath().get("custom_segment_id");
		utils.logit("segment id is -- " + customSegtId);

		// add anonymous user to custom segment
		String csvFilePath2 = System.getProperty("user.dir") + "/resources/Images/PP_CSV_AnnonymousUser.csv";
		Response addAnonymousUserResponse = pageObj.endpoints().addBulkUserToCustomSegment(seg_Name, customSegtId,
				csvFilePath2, dataSet.get("apiKey"));
		Assert.assertEquals(addAnonymousUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched");
		utils.logPass("Bulk anonymous users added to custom segment " + seg_Name);

		// navigate to custom segment
//		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments ");
		String segmentId2 = createSegResponse.jsonPath().get("custom_segment_id").toString();
//		pageObj.segmentsPage().searchAndOpenSegment(seg_Name, segmentId2);

		// user search API in segment
		Response searchUserInSegmentResponse = pageObj.endpoints().searchUserExistsInCustomSegment(segmentId2,
				dataSet.get("annonymousUserEmail"), dataSet.get("apiKey"));
		Assert.assertEquals(searchUserInSegmentResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched");
		utils.logPass(dataSet.get("annonymousUserEmail") + " found in custom segment " + seg_Name);

		// remove anonymous user from custom segment
		String csvFilePath3 = System.getProperty("user.dir") + "/resources/Images/PP_CSV_AnnonymousUser.csv";
		Response removeAnonymousUserResponse = pageObj.endpoints().removeBulkUserfromCustomSegment(seg_Name,
				customSegtId, csvFilePath3, dataSet.get("apiKey"));
		Assert.assertEquals(removeAnonymousUserResponse.getStatusCode(), 200, "Status code 200 did not matched");
		utils.logPass("Bulk anonymous user removed from custom segment " + seg_Name);

		String segmentId3 = createSegResponse.jsonPath().get("custom_segment_id").toString();

		// user search API in segment
		Response searchUserResponse = pageObj.endpoints().searchUserExistsInCustomSegment(segmentId3,
				dataSet.get("annonymousUserEmail"), dataSet.get("apiKey"));
		Assert.assertEquals(searchUserResponse.getStatusCode(), 404, "Status code 404 did not matched");
		utils.logPass(dataSet.get("annonymousUserEmail") + " not found in custom segment " + seg_Name);

	}

	// Rakhi
	@Test(description = "SQ-T5961 create a segment with deactivated/Banned users and run with mass campaign and check banned user dont get rewards")
	@Owner(name = "Rakhi Rawat")
	public void T5961_VerifyBannedUserDontGetRewards() throws InterruptedException {

		// login to instance
		String dateTime = CreateDateTime.getTomorrowDate() + " 11:00 PM";
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.menupage().navigateToSubMenuItem("Settings", "Global Configuration");
		pageObj.dashboardpage().verifyGlobalConfigFlagCheckedUnchecked("Mass campaign filter banned users");
		pageObj.dashboardpage().navigateToAllBusinessPage();

		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// create mass campaign with segment contains banned and active users
		String massCampaignName = "Automation MassNotification Campaign" + CreateDateTime.getTimeDateString();

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		pageObj.campaignspage().selectMessagedrpValue(dataSet.get("messageDrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().setCampaignName(massCampaignName);
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().setAudienceType(dataSet.get("audiance"));
		pageObj.signupcampaignpage().setSegmentType("DND_BannedUserSegment");
		pageObj.signupcampaignpage().setPushNotification(massCampaignName);
		pageObj.signupcampaignpage().setEmailSubject(dataSet.get("emailSubject"));
		pageObj.signupcampaignpage().setEmailTemplate(dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().clickNextButton();
		pageObj.signupcampaignpage().setFrequencyAndTimeInCampaign("Once", dateTime);
		pageObj.signupcampaignpage().clickScheduleBtn();

		String msg = pageObj.guestTimelinePage().validateSuccessMessage();
		Assert.assertTrue(msg.contains("Schedule created successfully"),
				"Failed to verify campaign scheduled sucess message");
		utils.logPass("Mass Campaign scheduled successfully : " + massCampaignName);

		// navigate to Menu -> submenu
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().findMassCampaignNameandRun(massCampaignName);

		// verify loaded users in campaign logs
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		String camStatus = pageObj.campaignspage().checkMassCampStatusBeforeOpening(massCampaignName, "Processed");
		Assert.assertEquals(camStatus, "Processed", "Camapign status is not processed");
		pageObj.campaignspage().searchAndSelectCamapign(massCampaignName);

		pageObj.campaignspage().selectnewCPPOptions("Classic Page");
		String logParam = pageObj.campaignspage().verifyCampaignLogs();
		Assert.assertTrue(logParam.contains(dataSet.get("expectedMsg")));
		utils.logPass("Campaign logs loaded the correct count of users");

		// api 2 login
		Response loginResponse = pageObj.endpoints().Api2Login(dataSet.get("userEmail"), dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(loginResponse.getStatusCode(), 200);
		String token = loginResponse.jsonPath().get("access_token.token").toString();

		String campaignName1 = pageObj.guestTimelinePage().getCampaignNameByNotificationsAPIShortPoll(massCampaignName,
				dataSet.get("client"), dataSet.get("secret"), token);
		Assert.assertFalse(campaignName1.equalsIgnoreCase(massCampaignName),
				"Push notification is visible on Banned user's timeline...");
		utils.logPass("Banned Users did not received PN for campaign :" + massCampaignName);

	}

	// Rakhi
	@Test(description = "SQ-T5962 verified Segment read only admin can not create new segment")
	public void T5962_VerifySegmentReadOnlyAdminFunctionality() throws InterruptedException {

		// admin user login with Segment read only access
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().logintoInstance(dataSet.get("userName"), dataSet.get("passWord"));

		pageObj.menupage().navigateToSubMenuItem("Settings", "Admin Users");
		pageObj.AdminUsersPage().clickRole(dataSet.get("userName"), dataSet.get("role"));

		// validating the expected disabled permissions with the actual disabled
		// permissions
		List<String> ActualDisabledPermissions = pageObj.AdminUsersPage().verifyDisabledPermissions();
		Assert.assertTrue(ActualDisabledPermissions.contains(dataSet.get("expectedDisabledPermissions")),
				"Actual disabled permissions does not match with the expected disabled permissions");
		utils.logPass(
				dataSet.get("expectedDisabledPermissions") + " permissions is disabled for " + dataSet.get("userName"));

		// validating the expected Enabled permissions with the actual enabled
		// permissions
		List<String> ActualEnabledPermissions = pageObj.AdminUsersPage().verifyEnabledPermissions();
		Assert.assertTrue(ActualEnabledPermissions.contains(dataSet.get("expectedEnabledPermissions")),
				"Actual enabled permissions does not match with the expected enabled permissions");
		utils.logPass(
				dataSet.get("expectedEnabledPermissions") + " permissions is enabled for " + dataSet.get("userName"));

		String segmentName = CreateDateTime.getUniqueString("BannedUserSegment");
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		pageObj.newCamHomePage().campaignAdvertiseBlock();
		// verify if user is able to create new segment
		pageObj.newSegmentHomePage().setSegmentNameNewSegmentPage(segmentName);
		// pageObj.segmentsBetaPage().setSegmentBetaNameAndType(segmentName, "Profile
		// Details");
		pageObj.segmentsBetaPage().selectSegmentType("Profile Details");
		pageObj.segmentsBetaPage().setAttribute("Activation Status");
		pageObj.segmentsBetaPage().setOperator("Operator", "Equal to");
		pageObj.segmentsBetaPage().setOperator("Activation status", "Banned");
		boolean status = pageObj.segmentsBetaPage().verifyInsufficientPrivilegesPopUp();
		Assert.assertTrue(status, "Segment read only admin can create and save new segment");
		utils.logPass("Verified Segment read only admin can not create new segment");

		utils.switchToParentWindow();
		pageObj.newCamHomePage().clickSwitchToClassicBtn();
		pageObj.segmentsPage().searchAndOpenSegment(dataSet.get("segmentName"), dataSet.get("segmentId"));
		pageObj.segmentsBetaPage().segmentEllipsisOptions("View");
		// edit existing segment and save
		boolean status1 = pageObj.segmentsBetaPage().verifyInsufficientPrivilegesPopUp();
		Assert.assertTrue(status1, "Insufficient Privileges pop up is not displayed while saving the segment");
		utils.logPass("Verified Segment read only admin can not edit existing segment");

	}

	@Test(description = "SQ-T4644 Verify upload csv file in custom segment show processed on UI level")
	public void T4644_customSegmentBulkAddUI() throws InterruptedException {
		String segmentName = "Custom_Segment_" + CreateDateTime.getTimeDateString();
		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		String csvFilePath = System.getProperty("user.dir") + "/resources/Images/customSegmentuser_1000.csv";
		// navigate to custom segment
		utils.longWaitInSeconds(2);
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		pageObj.newCamHomePage().clickSwitchToClassicBtn();
		pageObj.segmentsBetaPage().setSegmentBetaName(segmentName);
		pageObj.segmentsBetaPage().selectSegmentType("Custom List");
		pageObj.segmentsBetaPage().setOperator("List Type", "New Custom List");
		pageObj.segmentsBetaPage().customListSegment(csvFilePath);
		pageObj.segmentsBetaPage().saveSegment(segmentName);
		boolean statusFlag = pageObj.segmentsBetaPage().customSegmentProcessed("customSegmentuser_1000");
		Assert.assertTrue(statusFlag, "CSV for 1000 user is not loaded in Custom Segment " + segmentName);
		utils.logPass("Verified that CSV for 1000 user is loaded in Custom Segment " + segmentName);
		pageObj.segmentsPage().deleteSegment(segmentName);
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();

	}

}
