package com.punchh.server.segmentsTest;

import java.lang.reflect.Method;
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
import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class SegmentWithLocationTest {
	public WebDriver driver;
	Properties prop;
	private String sTCName, env, deleteSegmentQuery;
	private String run = "ui";
	private String baseUrl;
	private static Map<String, String> dataSet;
	private PageObj pageObj;
	private Utilities utils;
	private SeleniumUtilities selUtils;

	@BeforeClass(alwaysRun = true)
	public void openBrowser() {
		prop = Utilities.loadPropertiesFile("config.properties");
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
		pageObj.utils().logit(sTCName + " ==>" + dataSet);
		utils = new Utilities(driver);
		selUtils = new SeleniumUtilities(driver);
		deleteSegmentQuery = "DELETE FROM segment_definitions WHERE id = $segmentId;";
		// move to All Business Page
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
	}

	@Test(description = "SQ-T6322: Create a Location group with 0 locations and verify segment count comes 0; "
			+ "SQ-T6323: verify in profile details we use favourite location and use Not equal to attribute and use location group(with 0 location) and verify it show all guest without having any favouite location; "
			+ "SQ-T6398: Verify in profile details segments 'Last visit location' attribute showing count", priority = 0, groups = {
					"regression" })
	@Owner(name = "Vaibhav Agnihotri")
	public void T6323_segmentNotEqualLocationGroup() throws Exception {
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Check that location group has 0 locations. SQ-T6322 and SQ-T6323 starts
		String locationGroup = dataSet.get("locationGroup");
		pageObj.menupage().navigateToSubMenuItem("Settings", "Location Groups");
		String groupLocationsCount = pageObj.iframeConfigurationPage()
				.getElementText("locationPage.groupLocationsCount", locationGroup);
		Assert.assertTrue(groupLocationsCount.contains("0"), "Location group does not have 0 locations");

		// Create a segment with 'Equal to' location group
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		pageObj.newSegmentHomePage().clickOnSwitchToClassicBtn();
		String segmentName = CreateDateTime.getUniqueString("ProfileDetailNotEqualTo_");
		pageObj.segmentsBetaPage().setSegmentBetaName(segmentName);
		pageObj.segmentsBetaPage().selectSegmentType("Profile Details");
		pageObj.segmentsBetaPage().selectAttribute("Favorite Location");
		pageObj.segmentsBetaPage().setOperator("Operator", "Equal to");
		pageObj.segmentsBetaPage().selectAttribute("Location(s)", locationGroup);
		pageObj.segmentsPage().saveAndShowSegment();
		// int guestInSegmentCount = pageObj.segmentsBetaPage().getGuestsInSegmentCount();
		String segmentId = pageObj.segmentsPage().getSegmentID();
		Response response = pageObj.endpoints().getSegmentCount(dataSet.get("apiKey"), segmentId);
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		int guestInSegmentCount = Integer.parseInt((response.jsonPath().getString("count")).replaceAll(",", ""));
		Assert.assertEquals(guestInSegmentCount, 0, "Guest in segment count is not equal to 0");
		pageObj.utils().logPass("Verified that segment with 'Equal to' empty location group has guest count as 0");

		// SQ-T6322 ends. Get segment count with 'Not equal to' location group
		pageObj.segmentsBetaPage().segmentEllipsisOptions("Edit");
		pageObj.segmentsBetaPage().setOperator("Operator", "Not Equal to");
		pageObj.segmentsBetaPage().selectAttribute("Location(s)", locationGroup);
		pageObj.segmentsPage().saveAndShowSegment();
		// guestInSegmentCount = pageObj.segmentsBetaPage().getGuestsInSegmentCount();
		response = pageObj.endpoints().getSegmentCount(dataSet.get("apiKey"), segmentId);
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		guestInSegmentCount = Integer.parseInt((response.jsonPath().getString("count")).replaceAll(",", ""));
		Assert.assertTrue(guestInSegmentCount > 0, "Guest in segment count is not greater than 0");

		// Get total guest count of business using that same attribute
		pageObj.segmentsBetaPage().segmentEllipsisOptions("Edit");
		pageObj.segmentsBetaPage().setOperator("Operator", "Equal to");
		pageObj.segmentsBetaPage().selectAttribute("Location(s)", "Any");
		int totalGuestCountInBusiness = pageObj.segmentsBetaPage().getSegmentSizeBeforeSave();
		Assert.assertTrue(totalGuestCountInBusiness >= guestInSegmentCount,
				"Segment count is not matching with total guest count in business");
		utils.logPass("Verified that total guest count on business i.e. " + totalGuestCountInBusiness
				+ " is greater than or equal to count of segment with 'Not Equal to' empty location group i.e. "
				+ guestInSegmentCount);
		pageObj.segmentsPage().saveAndShowSegment();
		String query = deleteSegmentQuery.replace("$segmentId", segmentId);
		DBUtils.executeQuery(env, query);
		// pageObj.segmentsBetaPage().segmentDefinationDelete(); // SQ-T6323 ends

		// SQ-T6398 starts. Verify count in 'Last Visited Location' segment combinations
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		segmentName = CreateDateTime.getUniqueString("LastVisitedLocationSegment_");
		pageObj.segmentsBetaPage().setSegmentBetaName(segmentName);
		pageObj.segmentsBetaPage().selectSegmentType("Profile Details");
		pageObj.segmentsBetaPage().selectAttribute("Last Visited Location");
		locationGroup = dataSet.get("locationGroupWithGuests");
		String location = dataSet.get("locationWithGuests");
		String[][] combinations = { { "Equal to", "Any" }, { "Equal to", locationGroup }, { "Equal to", location },
				{ "Not Equal to", location }, { "Not Equal to", locationGroup } };
		segmentId = "";
		for (String[] combination : combinations) {
			String key = combination[0];
			String value = combination[1];
			pageObj.segmentsBetaPage().setOperator("Operator", key);
			pageObj.segmentsBetaPage().selectAttribute("Location(s)", value);
			pageObj.segmentsPage().saveAndShowSegment();
			// guestInSegmentCount = pageObj.segmentsBetaPage().getGuestsInSegmentCount();
			// Fetch segment ID once; it remains same while the segment is edited
			segmentId = (segmentId.isEmpty()) ? pageObj.segmentsPage().getSegmentID() : segmentId;
			response = pageObj.endpoints().getSegmentCount(dataSet.get("apiKey"), segmentId);
			Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
			guestInSegmentCount = Integer.parseInt((response.jsonPath().getString("count")).replaceAll(",", ""));
			Assert.assertTrue(guestInSegmentCount > 0,
					"Segment count is not greater than 0 for 'Last Visited Location " + key + " at " + value + "'");
			pageObj.utils().logPass("Verified Guest in segment count for 'Last Visited Location " + key + " at " + value + "'");
			pageObj.segmentsBetaPage().segmentEllipsisOptions("Edit");
		}

		// Verify 'Last Visited Location' segment export
		utils.navigateBackPage();
		String exportName = "Export_" + segmentName;
		// String segmentOverviewWindow = driver.getWindowHandle();
		pageObj.segmentsBetaPage().segmentEllipsisOptions("Export");
		utils.switchToWindow();
		pageObj.schedulePage().segmentExportSchedule("OFF", exportName, "Immediately");
		boolean logsConfirmation = pageObj.schedulePage()
				.selectDataExportSchedule(prop.getProperty("segmentExportSchedule"), segmentName);
		Assert.assertTrue(logsConfirmation, "Failed to verify data export logs");
		pageObj.utils().logPass("Verified data export logs for " + segmentName);
		query = deleteSegmentQuery.replace("$segmentId", segmentId);
		DBUtils.executeQuery(env, query);
		// selUtils.switchToWindow(segmentOverviewWindow);
		// pageObj.segmentsBetaPage().segmentDefinationDelete(); // SQ-T6398 ends
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