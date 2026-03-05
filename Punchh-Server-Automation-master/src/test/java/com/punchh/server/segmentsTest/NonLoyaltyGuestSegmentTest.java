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
public class NonLoyaltyGuestSegmentTest {
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
	private String userEmail;
	private String segmentName;
	private int count = 0;
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
		ObjectMapper mapper = new ObjectMapper();
		segmentBuilderClauses = mapper.readTree(Files
				.readString(Paths.get(System.getProperty("user.dir") + prop.getProperty("segmentBuilderClauseJson"))));
	}

	@Test(description = "SQ-T6713 : Verify (Non-Loyalty) Guest Segment type with Most Visited and Last Visited Locations (Any) – Segment Creation, Count, and In_Segment Validation  || "
			+ "SQ-T6786 : Validation of OLO Webhook User Creation and In_Segment Query for Guest Segments", groups = {
					"regression", "dailyrun" }, priority = 0, dataProvider = "TestDataProvider")
	@Owner(name = "Sachin Bakshi")
	public void T6713_GuestMostVisitLocation(String SegmentAttribute) throws Exception {

		segmentName = "AutomationSegment_" + SegmentAttribute + "_" + CreateDateTime.getTimeDateString();

		// API for create a Non Loyalty Guest and make a transaction
		String segmentId = "";
		String externalUID = Integer.toString(Utilities.getRandomNoFromRange(10000, 50000));
		String externalUID1 = Integer.toString(Utilities.getRandomNoFromRange(10000, 50000));
		String orderID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		String orderID1 = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		String userEmail = "Non_loyalty_Guest_" + CreateDateTime.getTimeDateAsneed("yyyyMMddHHmmss") + "@example.com";

		// 1st Transaction for Guest
		Response response = pageObj.endpoints().segmentCloseOrderOnline(dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("slug"), externalUID, "", userEmail, dataSet.get("locationStoreNumber"), "", "Punchh", "",
				orderID);
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for close order online");

		// 2nd Transaction for same Guest
		Response response1 = pageObj.endpoints().segmentCloseOrderOnline(dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("slug"), externalUID1, "", userEmail, dataSet.get("locationStoreNumber"), "", "Punchh", "",
				orderID1);
		Assert.assertEquals(response1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for close order online");

		if (SegmentAttribute.equalsIgnoreCase("Last Visited Location")) {
//		Profile Details Last Visited Location Equal To At Any

			String ProfileDetailsLastVisitedLocationEqualToAtAny_builder_clause = utils.convertToDoubleStringified(
					segmentBuilderClauses.get("ProfileDetailsLastVisitedLocationEqualToAtAny"));

			Response segmentCreationUsingBuilderClauseResponse = pageObj.endpoints().segmentCreationUsingBuilderClause(
					dataSet.get("apiKey"), segmentName, ProfileDetailsLastVisitedLocationEqualToAtAny_builder_clause);
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

		}

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
		}

		// verifying In_segment query using API for existing user
		Response userInSegmentResp2 = pageObj.endpoints().userInSegment(userEmail, dataSet.get("apiKey"), segmentId);
		Assert.assertEquals(userInSegmentResp2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for auth user signup api");
		Assert.assertEquals(userInSegmentResp2.jsonPath().get("result").toString(), "true",
				"Guest is not present in segment");
		utils.logPass("Verified that user " + userEmail + " is present in Segment");

		// deleting segment
		String query = "DELETE FROM `segment_definitions` WHERE `id` = '" + segmentId + "';";
		DBUtils.executeQuery(env, query);
	}

	@DataProvider(name = "TestDataProvider")
	public Object[][] testDataProvider() {

		return new Object[][] {
				// {"SegmentAttribute"},
				{ "Last Visited Location" }, { "Most Visited Location" } };
	}

	@Test(description = "SQ-T6723: Create Guest Segment with Visit Count > 0 and duration Current Year", priority = 1)
	@Owner(name = "Sachin Bakshi")
	public void T6723_createSegmentWithVisitCountAndDuration() throws Exception {

		// user creation using api2
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("Api2 user signup is successful");

		// Pos api checkin
		String key = CreateDateTime.getTimeDateString();
		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T10:50:00+05:30";
		Response resp = pageObj.endpoints().posCheckinQC(date, userEmail, key, txn, dataSet.get("locationKey"),
				dataSet.get("menuItemid"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp.getStatusCode(), "Status code 200 did not matched for post chekin api");
		utils.longWaitInSeconds(10);

		segmentName = "AutomationSegment_VisitCount_" + "Current year" + "_" + CreateDateTime.getTimeDateString();

//		Guests Visit Count Greater than 0 Visit Date Within a range Current year (GMT+05:30) Chennai ( IST )
		String GuestsVisitCountGreaterThan0VisitDateWithinARangeCurrentYearChennai_builder_clause = utils
				.convertToDoubleStringified(segmentBuilderClauses
						.get("GuestsVisitCountGreaterThan0VisitDateWithinARangeCurrentYearChennai"));

		Response segmentCreationUsingBuilderClauseResponse = pageObj.endpoints().segmentCreationUsingBuilderClause(
				dataSet.get("apiKey"), segmentName,
				GuestsVisitCountGreaterThan0VisitDateWithinARangeCurrentYearChennai_builder_clause);
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

		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		pageObj.newSegmentHomePage().clickOnSwitchToClassicBtn();
		pageObj.segmentsPage().searchAndOpenSegment(segmentName, segmentId);
		pageObj.segmentsBetaPage().getGuestInSegmentCount();
		userEmail = pageObj.segmentsBetaPage().getSegmentGuestList(dataSet.get("apiKey"), segmentId);
		Assert.assertNotNull(userEmail, "User email is null");
		utils.logPass("Verified that Guest Segment with Visit Count > 0 and duration " + "Current year"
				+ " is Created successfully and have guest count of " + segmentCount);

		// deleting segment
		String query = "DELETE FROM `segment_definitions` WHERE `id` = '" + segmentId + "';";
		DBUtils.executeQuery(env, query);
	}
	
	
	@Test(description= "SQ-T6985: Verify segment count for guest segmentation contains user count based on the ordering type selected i.e. OLO or PAR")
	@Owner(name = "Shivam Agrawal")
	public void T6985_orderingVendorVerificationInGuestsSegmentation() throws Exception
	{
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		String parVendor = "PAR Ordering";
		String oloVendor = "OLO";
		String SegmentAttribute = "Visit Date";

		// Go to Cockpit > Dashboard, Scroll to ordering vendor field, select PAR ordering as vendor
		segmentName = "AutomationSegment_VisitDate_" + "Ever" + "_" + CreateDateTime.getTimeDateString();
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.cockpitDashboardMiscPage().selectOrderingVendor(parVendor);
		utils.logPass("Successfully selected ordering vendor as " + parVendor);
		
		//Create a new guests segment with visit date as ever to get total guests count
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		pageObj.newSegmentHomePage().clickOnSwitchToClassicBtn();
		pageObj.segmentsBetaPage().setSegmentBetaName(segmentName);
		pageObj.segmentsBetaPage().selectSegmentType("Guests");
		pageObj.segmentsBetaPage().selectAttribute("Visit Date");
		pageObj.segmentsBetaPage().setOperator("Duration", "Ever");
		
		//Save the segment
		pageObj.segmentsBetaPage().saveSegment(segmentName);
		int parOrderingSegmentCount = pageObj.segmentsBetaPage().getGuestInSegmentCount();
		String segmentId = pageObj.segmentsPage().getSegmentID();
		utils.logPass("Verified that Guest Segment with Attribute Type " + SegmentAttribute
				+ " is Created successfully and have guest count of " + parOrderingSegmentCount);
		
		//Check the audit logs and verify that transition channel is "Menu-Eclub"
		pageObj.segmentsBetaPage().segmentEllipsisOptions("Audit Log");
		utils.waitTillPagePaceDone();
		String builderClauseText = pageObj.segmentsBetaPage().getAuditPageBuilderClauseText();
		Assert.assertEquals(builderClauseText, "Menu eClub", "Channel does not match!");
		utils.logPass("Verified that the guests inside segment are coming from Par Ordering");
		
		// Go to Cockpit > Dashboard again, Scroll to ordering vendor field, select OLO as vendor
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.cockpitDashboardMiscPage().selectOrderingVendor(oloVendor);
		utils.logPass("Successfully selected ordering vendor as " + oloVendor);

		//Open the same segment again and click on edit, also save without making any modification.
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		pageObj.segmentsPage().searchAndOpenSegment(segmentName, segmentId);
		pageObj.segmentsBetaPage().segmentEllipsisOptions("Edit");
		pageObj.segmentsBetaPage().saveSegment(segmentName);
		utils.waitTillSpinnerDisappear(10);
		int oloSegmentCount = pageObj.segmentsBetaPage().getGuestInSegmentCount();
		utils.logPass("Verified that Guest Segment saved again and count updates as " + oloSegmentCount + 
				" based on vendor as " + oloVendor);
		
		// Assert that PAR and OLO counts are different
		if (parOrderingSegmentCount == oloSegmentCount) {
		    Assert.fail("Segment count did NOT change after switching vendor from PAR to OLO. "
		            + "PAR count = " + parOrderingSegmentCount 
		            + ", OLO count = " + oloSegmentCount 
		            + ". Please check once manually.");
		} 
		else {
		    utils.logPass("Segment count changed correctly: PAR count = " + parOrderingSegmentCount 
		            + ", OLO count = " + oloSegmentCount);
		}
		
		//Revisit the audit log page again and verify the builder clause is updated as olo now
		pageObj.segmentsBetaPage().segmentEllipsisOptions("Audit Log");
		builderClauseText = pageObj.segmentsBetaPage().getAuditPageBuilderClauseText();
		Assert.assertEquals(builderClauseText, "Olo eClub", "Channel does not match!");
		utils.logPass("Verified that the guests inside segment are coming from Olo");
		
		// deleting segment
		String query = "DELETE FROM `segment_definitions` WHERE `id` = '" + segmentId + "';";
		DBUtils.executeQuery(env, query);
		utils.logit("Segment Deleted successfully");
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