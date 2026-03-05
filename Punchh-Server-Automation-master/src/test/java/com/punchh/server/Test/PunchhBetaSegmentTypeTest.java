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
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.annotations.Owner;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class PunchhBetaSegmentTypeTest {
	static Logger logger = LogManager.getLogger(PunchhBetaSegmentTypeTest.class);
	public WebDriver driver;
	Properties prop;
	String userEmail;
	String segmentName, guestCount;
	private String sTCName;
	private String env, run = "ui";
	private String baseUrl;
	private static Map<String, String> dataSet;
	private PageObj pageObj;
	Utilities utils;

	@BeforeClass(alwaysRun = true)
	public void openBrowser() {
		prop = Utilities.loadPropertiesFile("config.properties");
		driver = new BrowserUtilities().launchBrowser();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		// Single login to instance
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		pageObj.instanceDashboardPage().instanceLogin(baseUrl);
	}

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
		sTCName = method.getName();
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		logger.info(sTCName + " ==>" + dataSet);
		segmentName = CreateDateTime.getUniqueString("Segment");
		utils = new Utilities(driver);
		// Move to All Businesses page
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
	}

// Author : Amit
	@Test(description = "SQ-SBT388,SBT389,SBT392,SQ-T3016,SQ-T3024 Validate count of segment browsed in Punchh segment type is same as the original existing beta segment used", groups = {
			"regression", "dailyrun" }, priority = 0)
	@Owner(name = "Amit Kumar")
	public void T3016_verifyCountOfSegmentBrowsedInPunchhSegmenTypeIsSameAsOriginalBetaSegmentUsed()
			throws InterruptedException {

		//pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		//pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// pageObj.menupage().clickGuestMenu();
		// pageObj.menupage().segmentBetaLink();
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		pageObj.segmentsBetaPage().findSegmentCountOnAllSegmentsTab("complex automation segment");// check count and
																									// refersh to check
																									// calculate link
																									// again
		int count = pageObj.segmentsBetaPage().findSegmentCountOnAllSegmentsTab("complex automation segment");
		// create new complex beta segment
		pageObj.segmentsBetaPage().setSegmentBetaNameAndType(segmentName, "Punchh Segment");
		pageObj.segmentsBetaPage().setSegmentlistType("complex automation segment");

		int segmentCountBeta = pageObj.segmentsBetaPage().getSegmentSizeBeforeSave();
		pageObj.segmentsBetaPage().saveSegment(segmentName);
		int segmentDefCount = pageObj.segmentsBetaPage().getSegmentCountSegmentDefination();
		int guestInSegmentCount = pageObj.segmentsBetaPage().getGuestInSegmentCount();
		Assert.assertEquals(count, segmentCountBeta, "Segment calculate now count did not matched with segment count");
		Assert.assertEquals(count, segmentDefCount, "Segment Definition count did not matched with segment count");
		Assert.assertEquals(count, guestInSegmentCount, "Guest in segment count did not matched with segment count");

		pageObj.segmentsBetaPage().segmentDefinationDelete();

	}

	@Test(description = "SQ-T3022, Validate  Beta segment logical operators are working as expected And OR NI", groups = {
			"regression", "dailyrun" }, priority = 1)
	@Owner(name = "Amit Kumar")
	public void T3022_verifyBetaSegmentLogicalOperatorsWorkingAsExpectedAndORNI() throws InterruptedException {

		//pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		//pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		String segmentName = CreateDateTime.getUniqueString("Segment");
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		// create new complex beta segment
		pageObj.segmentsBetaPage().setSegmentBetaNameAndType(segmentName, "Profile Details");
		pageObj.segmentsBetaPage().setAttribute("Zip Code");
		pageObj.segmentsBetaPage().setValue("112233");

		pageObj.segmentsBetaPage().selectSegmentType("Profile Details");
		pageObj.segmentsBetaPage().setAttributeOR("Gender", "OR");

		pageObj.segmentsBetaPage().selectSegmentType("Profile Details");
		pageObj.segmentsBetaPage().setAttributeNI("Favorite Location", "NOT INCLUDING");

		pageObj.segmentsBetaPage().selectSegmentType("Profile Details");
		pageObj.segmentsBetaPage().setAttributeAnd("Guest Type", "AND");

		int segmentCountBeta = pageObj.segmentsBetaPage().getSegmentSizeBeforeSave();
		Assert.assertTrue(segmentCountBeta > 0, segmentName);

	}

	@Test(description = "SQ-,Validate user is able to combine one or more beta segments to create complex segment using punchh segment type", groups = {
			"regression", "dailyrun" }, priority = 2)
	@Owner(name = "Amit Kumar")
	public void verifyUserIAbleTooCreateComplexSegmentUsingPunchhSegmentType() throws InterruptedException {

		//pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		//pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		String segmentName = CreateDateTime.getUniqueString("Segment");
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");

		// create new complex beta segment
		pageObj.segmentsBetaPage().setSegmentBetaNameAndType(segmentName, "Punchh Segment");
		pageObj.segmentsBetaPage().setSegmentlistType("complex automation segment");

		pageObj.segmentsBetaPage().selectSegmentType("Punchh Segment");
		pageObj.segmentsBetaPage().setSegmentlistType("complex automation segment");
		// pageObj.segmentsBetaPage().setExternalSegmentListype("TestOne");

		int segmentCountBeta = pageObj.segmentsBetaPage().getSegmentSizeBeforeSave();
		Assert.assertTrue(segmentCountBeta > 0, segmentName);
		pageObj.segmentsBetaPage().saveSegment(segmentName);
		pageObj.segmentsBetaPage().segmentDefinationDelete();
	}

//	covering this in T6659_NewSegmentHomePageSidePanelTest
//	@Test(description = "SQ-216, Segment Show Page -> Segment Count || "
//			+ "SQ-456, Validate Profile Details based type segment || "
//			+ "SQ-T2200, Segment Show Page -> User Presence in segment || "
//			+ "SQ-T6416 Verify segment reachbility showing properly", groups = {
//			"regression", "dailyrun" }, priority = 3)

//	@Test(description = "SQ-216, Segment Show Page -> Segment Count || "
//			+ "SQ-456, Validate Profile Details based type segment || "
//			+ "SQ-T2200, Segment Show Page -> User Presence in segment || "
//			+ "SQ-T6416 Verify segment reachbility showing properly", groups = { "regression",
//					"dailyrun" }, priority = 3)
	@Owner(name = "Hardik Bhardwaj")
	public void T2200_verifySegmentGuestCount() throws InterruptedException {
		//pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		//pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		// create new segment
		pageObj.segmentsBetaPage().createNewSegmentGuestType(segmentName, dataSet.get("segmentType"),
				dataSet.get("attribute"), dataSet.get("guestTypeValue"));
		// Verifiy and get guest count

		pageObj.segmentsBetaPage().getSegmentCount();
		pageObj.segmentsBetaPage().saveSegment(segmentName);
		int segmentDefCount = pageObj.segmentsBetaPage().getSegmentCountSegmentDefination();
		int segmentReachCount = pageObj.segmentsBetaPage().getCustomerReachabilityCount();
		List<String> summaryData = pageObj.segmentsBetaPage().checkCustomerReachability();
		logger.info(summaryData);

		Assert.assertTrue(summaryData.get(0).contains("Email"), "Segment Reachability Summary Email did not matched");
		Assert.assertTrue(summaryData.get(1).contains("Push Notification"),
				"Segment Reachability Summary Push Notification did not matched");
		Assert.assertTrue(summaryData.get(2).contains("SMS"), "Segment Reachability Summary SMS did not matched");
		Assert.assertTrue(summaryData.get(3).contains("Not Subscribed"),
				"Segment Reachability Summary Not Subscribed did not matched");
		Assert.assertTrue(summaryData.get(4).contains("Deactivated"),
				"Segment Reachability Summary Deactivated did not matched");
		Assert.assertTrue(segmentReachCount <= segmentDefCount,
				"SegmentReach count did not matched with segment count");
		pageObj.segmentsBetaPage().segmentDefinationDelete();

		// create new user
		/*
		 * userEmail = pageObj.iframeSingUpPage().generateEmail(); Response
		 * signUpResponse =
		 * pageObj.endpoints().Api2SignUp(userEmail,dataSet.get("client")
		 * ,dataSet.get("secret") );
		 * 
		 * pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		 * Assert.assertEquals(signUpResponse.getStatusCode(), 200,
		 * "Status code 200 did not matched for api2 signup");
		 * TestListeners.extentTest.get().pass("Api2 user signup is successful ");
		 * logger.info("Api2 user signup is successful "); // Verifying existing users
		 * presence pageObj.segmentsBetaPage().verifySegmentGuestPresence(segmentName,
		 * dataSet.get("exisitngUser")); // Verifying new users presence //
		 * pageObj.segmentsBetaPage().verifySegmentGuestPresence(segmentName, //
		 * userEmail); // waiting for segment count sync int segCount =
		 * pageObj.segmentsBetaPage().getSegmentUserCount(segmentCount); //
		 * Assert.assertTrue(segCount > segmentCount, "Failed to verify segment count //
		 * after new guest signup"); if (segCount > segmentCount) {
		 * logger.info("Segment count increased after user signup: " + segCount);
		 * TestListeners.extentTest.get().
		 * pass("Segment count increased after user signup: " + segCount); } else {
		 * logger.
		 * error("Failed to verrify Segment count increased after user signup, before signup: "
		 * + segCount + "after Signup: " + segmentCount); TestListeners.extentTest.get()
		 * .warning("Failed to verrify Segment count increased after user signup, before signup: "
		 * + segCount + "after Signup: " + segmentCount); }
		 */

	}

	@Test(description = "SQ-T3043 Verify Smart segment calculate user properly or not", groups = {
			"sanity", "nonNightly" }, priority = 4)
	@Owner(name = "Amit Kumar")
	public void T3043_verifySmartSegmentCalculateUserProperly() throws InterruptedException {
		//pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		//pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		int count = pageObj.segmentsBetaPage().findAndSelectSegment(dataSet.get("segmentName"));
		int segmentDefCount = pageObj.segmentsBetaPage().getSegmentCountSegmentDefination();
		int guestInSegmentCount = pageObj.segmentsBetaPage().getGuestInSegmentCount();
		Assert.assertEquals(segmentDefCount, count, "Segment Definition count did not matched with segment count");
		Assert.assertEquals(guestInSegmentCount, count, "Guest in segment count did not matched with segment count");
	}

	@Test(description = "SQ-T3070 Verify External segment type showing external segment list dropdown and count is proper", groups = {
			"regression", "nonNightly" }, priority = 5)
	@Owner(name = "Amit Kumar")
	public void T3070_verifyExternalSegmentTypeShowingExternalSegmentListDropdownAndCountIsProper()
			throws InterruptedException {

		//pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		//pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		String segmentName = CreateDateTime.getUniqueString("Segment");
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		// create new complex beta segment
		pageObj.segmentsBetaPage().setSegmentBetaNameAndType(segmentName, "External Segment");
		// pageObj.segmentsBetaPage().setExternalSegmentListype("Loyalty Guests");
		pageObj.segmentsBetaPage().setSegmentlistType("Dev Test Campaign");
		pageObj.segmentsBetaPage().getSegmentCount();
		pageObj.segmentsBetaPage().saveSegment(segmentName);
	}

	@Test(description = "SQ-T6402: Verify Punchh segment and other segment combine and come in limit then export the segment", groups = {
			"regression" }, priority = 7)
	@Owner(name = "Vaibhav Agnihotri")
	public void T6402_punchhSegmentCombinedLimit() throws Exception {
		String combinedSegmentName = dataSet.get("combinedSegment");
		String segmentName = CreateDateTime.getUniqueString("PunchhSegment_");
		String exportName = "Export_" + segmentName;

		//pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		//pageObj.instanceDashboardPage().loginToInstance();
		pageObj.menupage().navigateToSubMenuItem("SRE", "Segment Configuration");
		pageObj.segmentsPage().checkSegmentRuleLimit("5");
		pageObj.dashboardpage().navigateToAllBusinessPage();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		pageObj.newSegmentHomePage().clickOnSwitchToClassicBtn();
		pageObj.segmentsBetaPage().setSegmentBetaNameAndType(segmentName, "Punchh Segment");
		pageObj.segmentsBetaPage().setSegmentlistType(combinedSegmentName);

		// Add segment combination at index 1
		pageObj.segmentsBetaPage().selectSegmentType("Checkins");
		pageObj.segmentsBetaPage().setAndOrNiCondition("AND");
		pageObj.segmentsBetaPage().setAttributeList("Checkin Location");
		pageObj.segmentsBetaPage().setOperatorText("Operator", "Equal to");
		pageObj.gamesPage().clickButton("segmentBetaPage.saveAndShowButton");

		// Verify limit exceeded message
		boolean isErrorMessageDisplayed = utils
				.checkElementPresent(utils.getLocator("segmentPage.limitExceededMessage"));
		Assert.assertTrue(isErrorMessageDisplayed, "Limit exceeded message is not displayed");
		logger.info("Verified segment limit exceeded message is displayed");
		TestListeners.extentTest.get().info("Verified segment limit exceeded message is displayed");

		// Delete segment attribute at index 1
		pageObj.segmentsPage().deleteSegmentCombination("1");

		// Verify segment count before save
		int segmentCount = pageObj.segmentsBetaPage().getSegmentSizeBeforeSave();
		Assert.assertTrue(segmentCount > 0, "Segment count before save is not greater than 0 for " + segmentName);
		pageObj.segmentsPage().saveAndShowSegmentBtn();

		// Verify segment export
		pageObj.segmentsPage().segmentOverviewPageOptionList("Export");
		utils.switchToWindow();
		pageObj.schedulePage().segmentExportSchedule("OFF", exportName, "Immediately");
		boolean logsConfirmation = pageObj.schedulePage()
				.selectDataExportSchedule(prop.getProperty("segmentExportSchedule"), segmentName);
		Assert.assertTrue(logsConfirmation, "Failed to verify data export logs");
		logger.info("Verifed data export logs for " + segmentName);
		TestListeners.extentTest.get().pass("Verifed data export logs for " + segmentName);

		// Delete segment
		utils.switchToParentWindow();
		pageObj.segmentsBetaPage().segmentDefinationDelete();
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		logger.info("Data set cleared");
	}

	@AfterClass(alwaysRun = true)
	public void closeBrowser() {
		driver.quit();
		logger.info("Browser closed");
	}
}