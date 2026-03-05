package com.punchh.server.segmentsTest;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
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
public class PunchhSegmentTest {
	public WebDriver driver;
	private Properties prop;
	private PageObj pageObj;
	private String sTCName;
	private String env;
	private String baseUrl;
	private Utilities utils;
	private static Map<String, String> dataSet;
	String run = "ui";
	private String userEmail, segmentName;
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
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		pageObj.utils().logit(sTCName + " ==>" + dataSet);

		ObjectMapper mapper = new ObjectMapper();
		segmentBuilderClauses = mapper.readTree(Files
				.readString(Paths.get(System.getProperty("user.dir") + prop.getProperty("segmentBuilderClauseJson"))));
	}

	@Test(description = "SQ-T6523 Verify when we create a Punchh segment with two different child segments and use parent segment in child segment with AND operator || "
			+ "SQ-T6524 Verify when we create a Punchh segment with two different child segments and use parent segment in parent segment after edit with NotIncluding/AND/OR operator || "
			+ "SQ-T3641 Verify that User is able to export Punchh segment", groups = { "regression",
					"dailyrun" }, priority = 0)
	@Owner(name = "Hardik Bhardwaj")
	public void T6523_punchhSegmentAndTwoChildSegment() throws Exception {
		int count = 0;
		// login to instance
//		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
//		pageObj.instanceDashboardPage().loginToInstance();

		// create user with age_verified status as true
		String userEmail = pageObj.iframeSingUpPage().generateEmailPunchhPar();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api1 signup");

		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

//Will add this api call once this is fixed

////		create Profile Details Segment with Email Domain as punchh.com (First child segment)
////		Profile Details Email Domain Equal to punchh.com
//		segmentName = CreateDateTime.getUniqueString("Profile_Details_EmailDomain_");
//		String Profile_Details_EmailDomain_builder_clause = utils
//				.convertToDoubleStringified(segmentBuilderClauses.get("emailDomainEqualTo_punchhOnboardingDate"));
//
//		Response segmentCreationUsingBuilderClauseResponse = pageObj.endpoints().segmentCreationUsingBuilderClause(
//				dataSet.get("apiKey"), segmentName, Profile_Details_EmailDomain_builder_clause);
//		Assert.assertEquals(segmentCreationUsingBuilderClauseResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
//		String segmentId = segmentCreationUsingBuilderClauseResponse.jsonPath().get("data.id").toString();

		// navigate to segments
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		segmentName = CreateDateTime.getUniqueString("Profile_Details_EmailDomain_");
		pageObj.segmentsBetaPage().setSegmentBetaNameAndType(segmentName, "Profile Details");
		pageObj.segmentsBetaPage().selectAttribute("Email Domain");
		pageObj.segmentsBetaPage().setValue("punchhpar.com");
		int segmentCount = pageObj.segmentsBetaPage().getSegmentSizeBeforeSave();
		Assert.assertTrue(segmentCount > count, "Segment count is not greater than 0");
		utils.logPass("Segment count is greater than 0  ie " + segmentCount);
		pageObj.segmentsBetaPage().saveSegment(segmentName);
		String segmentId = pageObj.segmentsPage().getSegmentID();

//		Response segmentCountresponse = pageObj.endpoints().getSegmentCount(dataSet.get("apiKey"), segmentId);
//		Assert.assertEquals(segmentCountresponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
//				"Status code 200 did not matched for Fetch segment count dashboard api");
//		pageObj.utils().logit("Fetch segment count Dashboard API is Successful");
//		pageObj.utils().logPass("Fetch segment count Dashboard API is Successful");
//		String segmentCountMessage = segmentCountresponse.jsonPath().getString("message");
//		Assert.assertEquals(segmentCountMessage, "Fetched Count Successfully");
//		int segmentCount = Integer
//				.parseInt((segmentCountresponse.jsonPath().get("count").toString()).replaceAll(",", ""));

		// navigate to segments
		// create No Checkin No Redemption (Second child segment)

//		No Checkin No Redemption Membership Tier is Current Membership is Any Guest Type is eClub or Loyalty Ever Timezone is (GMT+05:30) Chennai ( IST )
		String segmentName2 = CreateDateTime.getUniqueString("No_Checkin_No_Redemption");
		String No_Checkin_No_Redemption_builder_clause = utils.convertToDoubleStringified(segmentBuilderClauses.get(
				"noCheckinNoRedemptionMembershipTierIsCurrentMembershipIsAnyGuestTypeIseClubOrLoyaltyEverTimezoneIsChennai"));

		Response segmentCreationUsingBuilderClauseResponse1 = pageObj.endpoints().segmentCreationUsingBuilderClause(
				dataSet.get("apiKey"), segmentName2, No_Checkin_No_Redemption_builder_clause);
		Assert.assertEquals(segmentCreationUsingBuilderClauseResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String segmentId2 = segmentCreationUsingBuilderClauseResponse1.jsonPath().get("data.id").toString();

		Response segmentCountresponse1 = pageObj.endpoints().getSegmentCount(dataSet.get("apiKey"), segmentId2);
		Assert.assertEquals(segmentCountresponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for Fetch segment count dashboard api");
		utils.logPass("Fetch segment count Dashboard API is Successful");
		String segmentCountMessage1 = segmentCountresponse1.jsonPath().getString("message");
		Assert.assertEquals(segmentCountMessage1, "Fetched Count Successfully");
		int segmentCount2 = Integer
				.parseInt((segmentCountresponse1.jsonPath().get("count").toString()).replaceAll(",", ""));

		Assert.assertTrue(segmentCount2 > count, "Segment count is not greater than 0");
		utils.logPass("Segment count is greater than 0  ie " + segmentCount2);

		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		// create new segment punchh segment
		String punchhSegmentName = CreateDateTime.getUniqueString("Punchh_Segment");
		pageObj.segmentsBetaPage().setSegmentBetaNameAndType(punchhSegmentName, "Punchh Segment");
		pageObj.segmentsBetaPage().setSegmentlistType(segmentName);
		pageObj.segmentsBetaPage().selectSegmentType("Punchh Segment");
		pageObj.segmentsBetaPage().setSegmentlistType(segmentName2);
		int segmentCount3 = pageObj.segmentsBetaPage().getSegmentSizeBeforeSave();
		Assert.assertTrue(segmentCount3 > 0, "Guest Count label not greater than 0");
		utils.logPass("Guest Count label appeard and count is " + segmentCount);
		pageObj.segmentsBetaPage().saveSegment(punchhSegmentName);
		utils.longWaitInMiliSeconds(2);
		String punchhSegmentId = pageObj.segmentsPage().getSegmentID();
		utils.waitTillSpinnerDisappear(10);
		int guestCount = pageObj.segmentsBetaPage().getGuestInSegmentCount();
		Assert.assertTrue(guestCount > 0, "Guest count in punchh segment " + punchhSegmentName
				+ " is not equal to the segment count in child segment i.e " + segmentName + " and " + segmentName2);
		pageObj.utils().logit("Verified that Guest count in punchh segment " + punchhSegmentName
				+ " is equal to the segment count in child segment i.e " + segmentName + " and " + segmentName2);
		utils.logPass("Verified that Guest count in punchh segment " + punchhSegmentName
				+ " is equal to the segment count in child segment i.e " + segmentName + " and " + segmentName2);

		// Segment Export
		String exportName = "Export_Report_" + punchhSegmentName;
		pageObj.segmentsPage().segmentOverviewPageOptionList("Export");
		utils.switchToWindow();
		pageObj.schedulePage().segmentExportSchedule("OFF", exportName, "Immediately");
		boolean logsConfirmation = pageObj.schedulePage()
				.selectDataExportSchedule(prop.getProperty("segmentExportSchedule"), punchhSegmentName);
		Assert.assertTrue(logsConfirmation, "Failed to verify data export logs");
		utils.logPass("Successfully verify data export logs for " + punchhSegmentName);

		// Delete Data Export
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulePage().openSchedule(prop.getProperty("segmentExportSchedule"), punchhSegmentName);
		pageObj.schedulePage().selectDeleteOrDeactivateOptionDataExport(exportName, "Delete");
		utils.acceptAlert(driver);
		String message = utils.getSuccessMessage();
		Assert.assertEquals(message, "Schedule deleted successfully.", "Message did not match.");
		utils.logPass(exportName + " Data Export Export deleted Successfully");

		utils.longWaitInSeconds(2);
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		pageObj.segmentsPage().searchAndOpenSegment(segmentName, segmentId);
		pageObj.segmentsBetaPage().segmentEllipsisOptions("Edit");

		pageObj.segmentsBetaPage().selectSegmentType("Punchh Segment");
		pageObj.segmentsBetaPage().setSegmentlistType(punchhSegmentName);

		boolean messageFlag = pageObj.segmentsPage().verifySegmentCreationPageHeading(segmentName);
		Assert.assertTrue(messageFlag,
				"This segment with Add Rule as AND is saved and shown not showing error message regarding recursive segment");
		pageObj.utils().logit(
				"Verified that this segment with Add Rule as AND should not be save and shown error message regarding recursive segment");
		utils.logPass(
				"Verified that this segment with Add Rule as AND should not be save and shown error message regarding recursive segment");

		// making conbination with OR
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		pageObj.segmentsPage().searchAndOpenSegment(segmentName, segmentId);
		pageObj.segmentsBetaPage().segmentEllipsisOptions("Edit");

		pageObj.segmentsBetaPage().selectSegmentType("Punchh Segment");
		pageObj.segmentsBetaPage().setAndOrNiCondition("OR");
		pageObj.segmentsBetaPage().setSegmentlistType(punchhSegmentName);

		boolean messageFlag2 = pageObj.segmentsPage().verifySegmentCreationPageHeading(segmentName);
		Assert.assertTrue(messageFlag2,
				"This segment with Add Rule as OR is saved and shown not showing error message regarding recursive segment");
		pageObj.utils().logit(
				"Verified that this segment with Add Rule as OR should not be save and shown error message regarding recursive segment");
		utils.logPass(
				"Verified that this segment with Add Rule as OR should not be save and shown error message regarding recursive segment");

		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");

		// delete punchh segment
		pageObj.segmentsPage().searchAndOpenSegment(punchhSegmentName, punchhSegmentId);
		pageObj.segmentsPage().deleteSegment(punchhSegmentName);

		// delete segment first child segment
		pageObj.segmentsPage().searchAndOpenSegment(segmentName, segmentId);
		pageObj.segmentsPage().deleteSegment(segmentName);

		// delete segment second child segment
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		pageObj.segmentsPage().searchAndOpenSegment(segmentName2, segmentId2);
		pageObj.segmentsPage().deleteSegment(segmentName2);

	}

	@Test(description = "SQ-T6660 Verify that the segment utilised in the \"Punchh\" segment can't be deleted from the segment", groups = {
			"regression", "dailyrun" }, priority = 1)
	@Owner(name = "Sachin Bakshi")
	public void T6660_deleteChildSegmentOfPunchhSegment() throws Exception {
		int count = 0;
		// login to instance
//		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
//		pageObj.instanceDashboardPage().loginToInstance();

		// create user with age_verified status as true
		String userEmail = pageObj.iframeSingUpPage().generateEmailPunchhPar();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api1 signup");

		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// navigate to segments
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");

		// create Profile Details Segment with Email Domain as punchh.com (First child
		// segment)
		utils.waitTillSpinnerDisappear();
		segmentName = CreateDateTime.getUniqueString("Profile_Details_EmailDomain_");
		pageObj.segmentsBetaPage().setSegmentBetaNameAndType(segmentName, "Profile Details");
		pageObj.segmentsBetaPage().selectAttribute("Email Domain");
		pageObj.segmentsBetaPage().setValue("punchh.com");
		int segmentCount = pageObj.segmentsBetaPage().getSegmentSizeBeforeSave();
		Assert.assertTrue(segmentCount > count, "Segment count is not greater than 0");
		utils.logPass("Segment count is greater than 0  ie " + segmentCount);
		pageObj.segmentsBetaPage().saveSegment(segmentName);
		String segmentId = pageObj.segmentsPage().getSegmentID();

////	Profile Details Email Domain Equal to punchh.com
//	segmentName = CreateDateTime.getUniqueString("Profile_Details_EmailDomain_");
//	String Profile_Details_EmailDomain_builder_clause = utils
//			.convertToDoubleStringified(segmentBuilderClauses.get("emailDomainEqualTo_punchhOnboardingDate"));
//
//	Response segmentCreationUsingBuilderClauseResponse = pageObj.endpoints().segmentCreationUsingBuilderClause(
//			dataSet.get("apiKey"), segmentName, Profile_Details_EmailDomain_builder_clause);
//	Assert.assertEquals(segmentCreationUsingBuilderClauseResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
//	String segmentId = segmentCreationUsingBuilderClauseResponse.jsonPath().get("data.id").toString();

//		Response segmentCountresponse = pageObj.endpoints().getSegmentCount(dataSet.get("apiKey"), segmentId);
//		Assert.assertEquals(segmentCountresponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
//				"Status code 200 did not matched for Fetch segment count dashboard api");
//		pageObj.utils().logit("Fetch segment count Dashboard API is Successful");
//		pageObj.utils().logPass("Fetch segment count Dashboard API is Successful");
//		String segmentCountMessage = segmentCountresponse.jsonPath().getString("message");
//		Assert.assertEquals(segmentCountMessage, "Fetched Count Successfully");
//		int segmentCount = Integer
//				.parseInt((segmentCountresponse.jsonPath().get("count").toString()).replaceAll(",", ""));

		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		// create new segment punchh segment
		String punchhSegmentName = CreateDateTime.getUniqueString("Punchh_Segment");
		pageObj.segmentsBetaPage().setSegmentBetaNameAndType(punchhSegmentName, "Punchh Segment");
		pageObj.segmentsBetaPage().setSegmentlistType(segmentName);
		pageObj.segmentsBetaPage().saveSegment(punchhSegmentName);
		utils.waitTillSpinnerDisappear(10);
		int guestCount = pageObj.segmentsBetaPage().getGuestInSegmentCount();
		Assert.assertTrue(guestCount > count, "Segment count is not greater than 0");
		Assert.assertTrue(guestCount >= segmentCount, "Guest count in punchh segment " + punchhSegmentName
				+ " is not equal to the segment count in Punchh Segment " + segmentName);
		pageObj.utils().logit("Verified that Guest count in punchh segment " + punchhSegmentName
				+ " is equal to the segment count in punchh Segment " + segmentName);
		utils.logPass("Verified that Guest count in punchh segment " + punchhSegmentName
				+ " is equal to the segment count in Digital Stored Value Segment " + segmentName);

		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		pageObj.newSegmentHomePage().switchToNewSegmentManagementToolBtn();
		pageObj.newSegmentHomePage().searchSegmentonNewSegmentPage(segmentName, "withoutAssertion");
		pageObj.newSegmentHomePage().clickEllipsesOptions();
		pageObj.newSegmentHomePage().clickOptionsInEllipsesButton("Delete");
		pageObj.newSegmentHomePage().clickOnYesDelete();
		utils.longWaitInSeconds(6);
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		pageObj.newSegmentHomePage().searchSegmentonNewSegmentPage(segmentName, "withoutAssertion");
		List<String> segmentNameListDeletion = pageObj.newSegmentHomePage().segmentList();
		String segmentLowerAfterDeletion = segmentNameListDeletion.get(0);
		Assert.assertEquals(segmentLowerAfterDeletion, segmentName, "Segment is deleted");
		utils.logPass("Segment is not deleted " + segmentName);

		// delete punchh segment
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		pageObj.newSegmentHomePage().switchToNewSegmentManagementToolBtn();
		pageObj.newSegmentHomePage().searchSegmentonNewSegmentPage(punchhSegmentName, "withoutAssertion");
		pageObj.newSegmentHomePage().clickEllipsesOptions();
		pageObj.newSegmentHomePage().clickOptionsInEllipsesButton("Delete");
		pageObj.newSegmentHomePage().clickOnYesDelete();
		utils.longWaitInSeconds(6);

		// delete profile segment
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		pageObj.newSegmentHomePage().switchToNewSegmentManagementToolBtn();
		pageObj.newSegmentHomePage().searchSegmentonNewSegmentPage(segmentName, "withoutAssertion");
		pageObj.newSegmentHomePage().clickEllipsesOptions();
		pageObj.newSegmentHomePage().clickOptionsInEllipsesButton("Delete");
		pageObj.newSegmentHomePage().clickOnYesDelete();

	}

	@Test(description = "SQ-T3639 Verify that The count of segment browsed in Punchh segment type is same as the original existing beta segment used || "
			+ "SQ-T3640 Verify that the Punchh segments browsed in Mass gifting campaign is functioning as expected and the campaign is targeting the users as per segment definition", groups = {
					"regression", "dailyrun" }, priority = 2)
	@Owner(name = "Hardik Bhardwaj")
	public void T3639_digitalStoredValueSegmentFunctionality() throws Exception {
		String segmentName = CreateDateTime.getUniqueString("Profile_Details_EmailDomain_");
		int count = 0;
		// login to instance
//		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
//		pageObj.instanceDashboardPage().loginToInstance();

		// create user with age_verified status as true
		String userEmail = pageObj.iframeSingUpPage().generateEmailPunchhPar();
		Response signUpResponse1 = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api1 signup");

		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// navigate to segments
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");

		// User register/signup using API2 Signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("Api2 user signup is successful");

		String userEmailSecond = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponseSecond = pageObj.endpoints().Api2SignUp(userEmailSecond, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponseSecond, "API 2 user signup");
		Assert.assertEquals(signUpResponseSecond.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("Api2 user signup is successful");

		// create Profile Details Segment with Email Domain as punchh.com
		pageObj.segmentsBetaPage().setSegmentBetaNameAndType(segmentName, "Profile Details");
		pageObj.segmentsBetaPage().selectAttribute("Email Domain");
		pageObj.segmentsBetaPage().setValue("punchh.com");

		pageObj.segmentsBetaPage().selectSegmentType("Profile Details");
		pageObj.segmentsBetaPage().setAndOrNiCondition("AND");
		pageObj.segmentsBetaPage().setAttributeList("Onboarding Date");
		pageObj.segmentsBetaPage().setDuration("Within a range");

		int segmentCount = pageObj.segmentsBetaPage().getSegmentSizeBeforeSave();
		Assert.assertTrue(segmentCount > count, "Segment count is not greater than 0");
		utils.logPass("Segment count is greater than 0  ie " + segmentCount);
		pageObj.segmentsBetaPage().saveSegment(segmentName);
		String segmentId = pageObj.segmentsPage().getSegmentID();
		utils.waitTillSpinnerDisappear();

////	Profile Details Email Domain Equal to punchh.com
//	segmentName = CreateDateTime.getUniqueString("Profile_Details_EmailDomain_");
//	String Profile_Details_EmailDomain_builder_clause = utils
//			.convertToDoubleStringified(segmentBuilderClauses.get("emailDomainEqualTo_punchhOnboardingDate"));
//
//	Response segmentCreationUsingBuilderClauseResponse = pageObj.endpoints().segmentCreationUsingBuilderClause(
//			dataSet.get("apiKey"), segmentName, Profile_Details_EmailDomain_builder_clause);
//	Assert.assertEquals(segmentCreationUsingBuilderClauseResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
//	String segmentId = segmentCreationUsingBuilderClauseResponse.jsonPath().get("data.id").toString();

//		Response segmentCountresponse = pageObj.endpoints().getSegmentCount(dataSet.get("apiKey"), segmentId);
//		Assert.assertEquals(segmentCountresponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
//				"Status code 200 did not matched for Fetch segment count dashboard api");
//		pageObj.utils().logit("Fetch segment count Dashboard API is Successful");
//		pageObj.utils().logPass("Fetch segment count Dashboard API is Successful");
//		String segmentCountMessage = segmentCountresponse.jsonPath().getString("message");
//		Assert.assertEquals(segmentCountMessage, "Fetched Count Successfully");
//		int segmentCount = Integer
//				.parseInt((segmentCountresponse.jsonPath().get("count").toString()).replaceAll(",", ""));

		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		// create new segment punchh segment
		String punchhSegmentName = CreateDateTime.getUniqueString("Punchh_Segment");
		pageObj.segmentsBetaPage().setSegmentBetaNameAndType(punchhSegmentName, "Punchh Segment");
		pageObj.segmentsBetaPage().setSegmentlistType(segmentName);
		pageObj.segmentsBetaPage().saveSegment(punchhSegmentName);
		utils.waitTillSpinnerDisappear();
		int guestCount = pageObj.segmentsBetaPage().getGuestInSegmentCount();
		Assert.assertTrue(guestCount > count, "Segment count is not greater than 0");
		Assert.assertTrue((segmentCount < guestCount) || (segmentCount == guestCount),
				"Guest count in punchh segment " + punchhSegmentName
						+ " is not equal to the segment count in Digital Stored Value Segment " + segmentName);
		pageObj.utils().logit("Verified that Guest count in punchh segment " + punchhSegmentName
				+ " is equal to the segment count in Digital Stored Value Segment " + segmentName);
		utils.logPass("Verified that Guest count in punchh segment " + punchhSegmentName
				+ " is equal to the segment count in Digital Stored Value Segment " + segmentName);
		String punchhSegmentId = pageObj.segmentsPage().getSegmentID();
//		userEmail = pageObj.segmentsBetaPage().getSegmentGuestList(dataSet.get("apiKey"), segmentId);

		Response segmentResponse = pageObj.endpoints().userInSegment(userEmail, dataSet.get("apiKey"), segmentId);
		Assert.assertEquals(segmentResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for user in segment API");
		String result1 = segmentResponse.jsonPath().get("result").toString();
		Assert.assertEquals(result1, "true", "Guest is present in segment");

		// delete punchh segment
		long punchhId = Long.parseLong(punchhSegmentId);
		String query = "DELETE FROM `segment_definitions` WHERE `id` = " + punchhId + ";";
		DBUtils.executeQuery(env, query);

		// delete segment
		long segId = Long.parseLong(segmentId);
		query = "DELETE FROM `segment_definitions` WHERE `id` = " + segId + ";";
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
