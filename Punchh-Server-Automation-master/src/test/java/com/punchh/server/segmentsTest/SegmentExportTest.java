package com.punchh.server.segmentsTest;

import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.Map;
import java.util.Properties;

import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.annotations.Owner;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class SegmentExportTest {
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

	@BeforeMethod(alwaysRun = true)
	public void beforeClass(Method method) {
		prop = Utilities.loadPropertiesFile("config.properties");
		sTCName = method.getName();
		driver = new BrowserUtilities().launchBrowser();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		utils = new Utilities(driver);
		blankSpace = "";
	}

	@Test(description = "SQ-T2195, Segment Export || "
			+ "SQ-T6099 Verify on schedule page when click on Mass campaign and segment export name it should be navigate to preview page - segment name", groups = {
					"regression", "dailyrun" }, priority = 0)
	@Owner(name = "Hardik Bhardwaj")
	public void T2195_verifySegmentExport() throws InterruptedException, ParseException {
		pageObj.utils().logit(sTCName + " ==>" + dataSet);
		pageObj.sidekiqPage().sidekiqCheck(baseUrl);

		String segmentName = CreateDateTime.getUniqueString("Profile_Details_Gender_Segment_");
		String exportName = "Export_Report_" + segmentName;
		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
//		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// navigate to segments
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		// create new segment
		pageObj.segmentsBetaPage().createNewSegmentWithLocation(segmentName, dataSet.get("segmentType"),
				dataSet.get("attribute"), "Gender", dataSet.get("guestTypeValue"));
		pageObj.segmentsPage().saveAndShowSegmentBtn();
		String segmentId = pageObj.segmentsPage().getSegmentID();
		pageObj.segmentsPage().segmentOverviewPageOptionList("Export");
		utils.switchToWindow();
		pageObj.schedulePage().segmentExportSchedule("OFF", exportName, "Immediately");
		boolean logsConfirmation = pageObj.schedulePage()
				.selectDataExportSchedule(prop.getProperty("segmentExportSchedule"), segmentName);
		Assert.assertTrue(logsConfirmation, "Failed to verify data export logs");
		pageObj.utils().logPass("Successfully verify data export logs for " + segmentName);

		// SQ-T6099 Verify on schedule page when click on Mass campaign and segment
		// export name it should be navigate to preview page - segment name
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType("Segment Export Schedules");
		boolean nameCheckFlag = pageObj.schedulespage().clickOnScheduledFunctionalityName("segment", segmentName);
		Assert.assertTrue(nameCheckFlag, "Clicked Segment is not redirecting to the same Segment overview page");
		utils.logPass("Verified that clicking on a Segment name redirects to its corresponding Segment overview page.");

		// navigate to segments
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		pageObj.segmentsPage().searchAndOpenSegment(segmentName, segmentId);
		pageObj.segmentsPage().deleteSegment(segmentName);
	}

	@Test(description = "SQ-T4359 Segment export should be deactivated after click on deactivated button || "
			+ "SQ-T4360 Verify Deactivated Segment Export activated properly || "
			+ "SQ-T4361 Verify delete segment export properly and try to open deleted segment export using segmet export ID || "
			+ "SQ-T6099 Verify on schedule page when click on Mass campaign and segment export name it should be navigate to preview page - segment edit", groups = {
					"regression", "dailyrun" }, priority = 1)
	@Owner(name = "Hardik Bhardwaj")
	public void T4359_verifySegmentExportDeactivate() throws InterruptedException, ParseException {
		pageObj.utils().logit(sTCName + " ==>" + dataSet);
		pageObj.sidekiqPage().sidekiqCheck(baseUrl);
		String segmentName = CreateDateTime.getUniqueString("Checkin_Segment_");
		String exportName = "Export_Report_" + segmentName;
		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
//		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// navigate to segments
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		// create new segment
		pageObj.segmentsBetaPage().setSegmentBetaName(segmentName);
		pageObj.segmentsBetaPage().selectSegmentType(dataSet.get("segmentType"));
		pageObj.segmentsBetaPage().selectAttribute(dataSet.get("attribute"));
		pageObj.segmentsBetaPage().enterValueInSegment(segmentName, "Value ", dataSet.get("optionValue"));
		pageObj.segmentsBetaPage().getSegmentSizeBeforeSave();
		pageObj.segmentsPage().saveAndShowSegmentBtn();
		String segmentId = pageObj.segmentsPage().getSegmentID();
		pageObj.segmentsPage().segmentOverviewPageOptionList("Export");
		utils.switchToWindow();
		pageObj.schedulePage().segmentExportSchedule("OFF", exportName, "Immediately");
		pageObj.schedulePage().openSchedule(prop.getProperty("segmentExportSchedule"), segmentName);
		pageObj.schedulePage().selectDeleteOrDeactivateOption(segmentName, "Deactivate");
		utils.acceptAlert(driver);
		String message = utils.getSuccessMessage();
		pageObj.utils().logPass("Segment Export Deactivated Successfully");
		pageObj.schedulespage().selectScheduleType("Segment Export Schedules");
		boolean result = pageObj.schedulePage().scheduleJobBackgroundColor(segmentName, "#f2dede");
		Assert.assertTrue(result,
				"On segment export list page that deactivated segment is not highlighting with red color");
		utils.logPass("On segment export list page that deactivated segment is highlight with red color");

//		SQ-T4360 Verify Deactivated Segment Export activated properly
		// SQ-T6099 Verify on schedule page when click on Mass campaign and segment
		// export name it should be navigate to preview page - segment edit
		pageObj.schedulePage().openSchedule(prop.getProperty("segmentExportSchedule"), segmentName);
		pageObj.schedulePage().selectDeleteOrDeactivateOption(segmentName, "Activate");
		utils.acceptAlert(driver);
		String activateMessage = utils.getSuccessMessage();
		Assert.assertEquals(activateMessage, "Schedule activated Successfully.", "Message did not match.");
		utils.logPass("Segment Export Activated Successfully");
		pageObj.schedulespage().selectScheduleType("Segment Export Schedules");
		boolean resultAfterActivate = pageObj.schedulePage().scheduleJobBackgroundColor(segmentName, "#f2dede");
		Assert.assertFalse(resultAfterActivate,
				"On segment export list page that Activated segment is highlighting with red color");
		utils.logPass("On segment export list page that Activated segment is not highlight with red color");

//		SQ-T4361 Verify delete segment export properly and try to open deleted segment export using segmet export ID
		pageObj.schedulePage().openSchedule(prop.getProperty("segmentExportSchedule"), segmentName);
		String currentUrl = driver.getCurrentUrl();
		pageObj.schedulePage().selectDeleteOrDeactivateOption(segmentName, "Delete");
		utils.acceptAlert(driver);
		String deleteMessage = utils.getSuccessMessage();
		Assert.assertEquals(deleteMessage, "Schedule deleted successfully.", "Message did not match.");
		pageObj.utils().logPass("Segment Export Deleted Successfully");
		utils.longWaitInSeconds(4);

		// switch to child window
		driver.navigate().to(currentUrl);
		utils.waitTillPagePaceDone();
		String scheduleMessage = utils.getSuccessMessage();
		Assert.assertEquals(scheduleMessage, "Schedule not found.", "Message did not match.");
		pageObj.utils().logPass("On Segment Export Page Schedule not found (expected)");

		// navigate to segments
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		pageObj.segmentsPage().searchAndOpenSegment(segmentName, segmentId);
		pageObj.segmentsPage().deleteSegment(segmentName);
	}

	@SuppressWarnings("static-access")
	@Test(description = "SQ-T5518 Verify the functionality for segment rule limit", groups = { "regression",
			"dailyrun" }, priority = 2)
	@Owner(name = "Hardik Bhardwaj")
	public void T5518_verifyfunctionalityForSegment() throws Exception {

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();

		pageObj.menupage().navigateToSubMenuItem("SRE", "Segment Configuration");
		pageObj.segmentsPage().checkSegmentRuleLimit("5");
		pageObj.dashboardpage().navigateToAllBusinessPage();

		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		String segmentName = CreateDateTime.getUniqueString("Segment_combination_");
		String exportName = "T5518_Segment_Export_Report_" + segmentName;
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");

		// create new complex beta segment
		// Segment - 1
		pageObj.segmentsBetaPage().setSegmentBetaNameAndType(segmentName, "Profile Details");
		pageObj.segmentsBetaPage().setAttribute("Zip Code");
		pageObj.segmentsBetaPage().setValue("112233");

		// Segment - 2
		pageObj.segmentsBetaPage().selectSegmentType("Profile Details");
		pageObj.segmentsBetaPage().setAttributeOR("Gender", "OR");

		// Segment - 3
		pageObj.segmentsBetaPage().selectSegmentType("Profile Details");
		pageObj.segmentsBetaPage().setAttributeNI("Favorite Location", "NOT INCLUDING");

		// Segment - 4
		utils.longWaitInSeconds(2);
		pageObj.segmentsBetaPage().selectSegmentType("Profile Details");
		pageObj.segmentsBetaPage().setAttributeAnd("Guest Type", "AND");

		// Segment - 5
		pageObj.segmentsBetaPage().selectSegmentType("Profile Details");
		pageObj.segmentsBetaPage().setAttributeORIndex("Gender", "OR", "5", "3");

		// Segment - 6
		pageObj.segmentsBetaPage().selectSegmentType("Profile Details");
		pageObj.segmentsBetaPage().setAttributeORIndex("Gender", "OR", "6", "5");

		pageObj.segmentsPage().saveAndShowSegment();
		utils.logit(
				"Verified that The validation message should appear saying Base Combined number of rules in this segment and its sub-segment has exceeded the permitted limit and the user should not be able to create the segment");

		// Delete segment combination for index 5
		pageObj.segmentsPage().deleteSegmentCombination("5");
		utils.longWaitInSeconds(5);

		// Segment count before save
		int segmentCount = pageObj.segmentsBetaPage().getSegmentSizeBeforeSave();
		Assert.assertTrue(segmentCount > 0, segmentName);

		pageObj.segmentsPage().saveAndShowSegmentBtn();
		String segmentId = pageObj.segmentsPage().getSegmentID();
		pageObj.segmentsPage().segmentOverviewPageOptionList("Export");
		utils.switchToWindow();
		pageObj.schedulePage().segmentExportSchedule("OFF", exportName, "Immediately");
		boolean logsConfirmation = pageObj.schedulePage()
				.selectDataExportSchedule(prop.getProperty("segmentExportSchedule"), segmentName);
		Assert.assertTrue(logsConfirmation, "Failed to verify data export logs");
		utils.logPass("Successfully verify data export logs for " + segmentName);

		// navigate to segments
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		pageObj.segmentsPage().searchAndOpenSegment(segmentName, segmentId);
		pageObj.segmentsPage().segmentOverviewPageOptionList("Edit");

		// Delete segment combination for index 4
		utils.longWaitInSeconds(4);
		pageObj.segmentsPage().deleteSegmentCombination("4");

		// Segment count before save
		segmentCount = pageObj.segmentsBetaPage().getSegmentSizeBeforeSave();
		Assert.assertTrue(segmentCount > 0, segmentName);
		pageObj.segmentsPage().saveAndShowSegmentBtn();

		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		// create new segment punchh segment
		String punchhSegmentName = CreateDateTime.getUniqueString("Punchh_Segment");
		pageObj.segmentsBetaPage().setSegmentBetaNameAndType(punchhSegmentName, "Punchh Segment");
		pageObj.segmentsBetaPage().setSegmentlistType(segmentName);

		// Segment - 2
		pageObj.segmentsBetaPage().selectSegmentType("Profile Details");
		pageObj.segmentsBetaPage().setAttributeAndIndex("Gender", "AND", "1");

		// Segment - 3
		pageObj.segmentsBetaPage().selectSegmentType("Profile Details");
		pageObj.segmentsBetaPage().setAttributeORIndex("Gender", "OR", "2", "3");
		pageObj.segmentsPage().saveAndShowSegment();
		utils.logit(
				"Verified that The validation message should appear saying Base Combined number of rules in this segment and its sub-segment has exceeded the permitted limit and the user should not be able to create the segment");

		// Delete segment combination for index 2
		pageObj.segmentsPage().deleteSegmentCombination("2");

		// Segment count before save
		segmentCount = pageObj.segmentsBetaPage().getSegmentSizeBeforeSave();
		Assert.assertTrue(segmentCount > 0, segmentName);
		pageObj.segmentsPage().saveAndShowSegmentBtn();

		// delete segment
		String query = "DELETE FROM `segment_definitions` WHERE `id` = '" + segmentId + "';";
		DBUtils.executeQuery(env, query);
	}

	@AfterMethod(alwaysRun = true)
	public void afterClass() {
		driver.quit();
		pageObj.utils().logit("Browser closed");
	}
}