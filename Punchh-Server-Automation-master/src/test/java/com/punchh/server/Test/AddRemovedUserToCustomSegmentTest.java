package com.punchh.server.Test;

import java.lang.reflect.Method;
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
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class AddRemovedUserToCustomSegmentTest {
	private static Logger logger = LogManager.getLogger(AddRemovedUserToCustomSegmentTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String sTCName;
	private String baseUrl;
	String blankString = "";
	private String env, run = "ui";
	private static Map<String, String> dataSet;
	Properties prop;
	Utilities utils;

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
		sTCName = method.getName();
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==>" + dataSet);
		utils = new Utilities(driver);
	}

	@Test(description = "SQ-T4631, SQ-T4630  Verify that a removed user within custom segment can be added back via User's Profile > Segments menu page - new segment", groups = {
			"regression", "dailyrun","nonNightly" })
	@Owner(name = "Rakhi Rawat")
	public void T4631_addRemovedUserToCustomSegmentAgain() throws InterruptedException {

		// create custom segment
		String segName = "CustomSeg" + CreateDateTime.getTimeDateString();
		System.out.println(segName);
		Response createSegmentResponse = pageObj.endpoints().createCustomSegment(segName, dataSet.get("apiKey"));
		pageObj.apiUtils().verifyCreateResponse(createSegmentResponse, "Custom segment created");
		Assert.assertEquals(createSegmentResponse.getStatusCode(), 201, "Status code 201 did not matched");
		logger.info("custom segment created " + segName);
		TestListeners.extentTest.get().pass("custom segment created " + segName);
		int customSegmentId = createSegmentResponse.jsonPath().get("custom_segment_id");
		logger.info("segment id is -- " + customSegmentId);
		TestListeners.extentTest.get().info("segment id is -- " + customSegmentId);

		// add user to custom segment
		Response addUserResponse = pageObj.endpoints().addUserToCustomSegment(customSegmentId, dataSet.get("userEmail"),
				dataSet.get("apiKey"));
		Assert.assertEquals(addUserResponse.getStatusCode(), 201, "Status code 201 did not matched");
		logger.info("user added to custom segment " + segName);
		TestListeners.extentTest.get().pass("user added to custom segment " + segName);

		// Login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */

		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Go to Cockpit > Dashboard, ensure the required flag is turned OFF
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().navigateToTabs("Bento Apps");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_new_segments_homepage", "uncheck");
		pageObj.dashboardpage().updateCheckBox();

		// navigate to custom segment
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		String segmentId = createSegmentResponse.jsonPath().get("custom_segment_id").toString();
		pageObj.segmentsPage().searchAndOpenSegment(segName, segmentId);

		Boolean flag = pageObj.segmentsPage().verifyUserPresentInSegment(dataSet.get("userEmail"),
				dataSet.get("hexCode1"));
		Assert.assertTrue(flag, "user not found in segment : " + segName);
		logger.info("user found in segment : " + segName);
		TestListeners.extentTest.get().pass("user found in segment : " + segName);

		/*
		 * utils.getLocator("segmentPage.searchResult").isDisplayed();
		 * utils.getLocator("segmentPage.searchResult").click();
		 */
		pageObj.segmentsPage().getGuestVisitCountText();
		String msg = pageObj.segmentsPage().removeUserFromSegment(segName, dataSet.get("hexcode2"));
		String expectedMsg = dataSet.get("expectedMsg").replace("${segmentName}", segName);
		Assert.assertTrue(msg.contains(expectedMsg), "after removing the user success msg is not same");
		logger.info("user deleted from segment message appears in green color");
		TestListeners.extentTest.get().pass("user deleted from segment message appears in green color");

		pageObj.segmentsPage().addUserToSegment(segName, dataSet.get("hexcode2"));
		logger.info("user added to segment message appears in green color");
		TestListeners.extentTest.get().pass("user added to segment message appears in green color");

//		// navigate to custom segment
//		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
//		pageObj.segmentsPage().searchAndOpenSegment(segName, segmentId);
//
//		Boolean flag1 = pageObj.segmentsPage().verifyUserPresentInSegment(dataSet.get("userEmail"),
//				dataSet.get("hexCode1"));
//		Assert.assertTrue(flag1, "user not found in segment : " + segName);

		// verifying In_segment query using API
		Response userInSegmentResp = pageObj.endpoints().userInSegment(dataSet.get("userEmail"), dataSet.get("apiKey"),
				segmentId);
		Assert.assertEquals(userInSegmentResp.getStatusCode(), 200,
				"Status code 200 did not matched for auth user signup api");
		Assert.assertTrue(userInSegmentResp.asString().contains("true"), "User not found in segment : " + segName);
		logger.info("added user found in segment : " + segName);
		TestListeners.extentTest.get().pass("added user found in segment : " + segName);
	}

	// Rakhi
	@Test(description = "SQ-T4629 Verify that a removed user within custom segment can be added back via User's Profile > Segments menu page - Existing segment", groups = {
			"unstable", "dailyrun" })
	@Owner(name = "Rakhi Rawat")
	public void T4629_addRemovedUserToExistingSegment() throws InterruptedException {

		// Login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */

		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// navigate to segment
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		pageObj.newCamHomePage().clickSwitchToClassicBtn();
		// String segmentId =
		// createSegmentResponse.jsonPath().get("custom_segment_id").toString();
		pageObj.segmentsPage().searchAndOpenSegment(dataSet.get("segmentName"), dataSet.get("segmentId"));
		String userEmail = pageObj.segmentsPage().clickFirstUserFromSegment();
		// verify if user exists in segment
		driver.navigate().back();
		Boolean flag = pageObj.segmentsPage().verifyUserColorInSegment(userEmail);
		if (!flag) {
			utils.refreshPage();
			pageObj.segmentsPage().clickFirstUserFromSegment();
			pageObj.guestTimelinePage().navigateToTabs("Segments");
			pageObj.segmentsPage().addUserToSegment(dataSet.get("segmentName"), dataSet.get("hexcode2"));
		} else {
			utils.refreshPage();
			pageObj.segmentsPage().clickFirstUserFromSegment();
			// navigate to Segment tab on user timeline
			pageObj.guestTimelinePage().navigateToTabs("Segments");
		}

		// remove user from segment through guest timeline
		String msg = pageObj.segmentsPage().removeUserFromSegment(dataSet.get("segmentName"), dataSet.get("hexcode2"));
		Assert.assertTrue(msg.contains(dataSet.get("expectedMsg1")),
				"After removing the user success message is not same");
		logger.info("User deleted from segment message appears in green color");
		TestListeners.extentTest.get().pass("User deleted from segment message appears in green color");

		// add user to custom segment through guest timeline
		String msg1 = pageObj.segmentsPage().addUserToSegment(dataSet.get("segmentName"), dataSet.get("hexcode2"));
		Assert.assertTrue(msg1.contains(dataSet.get("expectedMsg2")),
				"After adding the user success message is not same");
		logger.info("User added to segment message appears in green color");
		TestListeners.extentTest.get().pass("User added to segment message appears in green color");

//		// navigate to segment
//		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
//		pageObj.segmentsPage().searchAndOpenSegment(dataSet.get("segmentName"), dataSet.get("segmentId"));
//
//		Boolean flag1 = pageObj.segmentsPage().verifyUserPresentInSegment(userEmail, dataSet.get("hexCode1"));
//		Assert.assertTrue(flag1, "user not found in segment : " + dataSet.get("segmentName"));

		// verifying In_segment query using API
		Response userInSegmentResp = pageObj.endpoints().userInSegment(userEmail, dataSet.get("apiKey"),
				dataSet.get("segmentId"));
		Assert.assertEquals(userInSegmentResp.getStatusCode(), 200,
				"Status code 200 did not matched for auth user signup api");
		Assert.assertTrue(userInSegmentResp.asString().contains("true"),
				"User not found in segment : " + dataSet.get("segmentName"));
		logger.info("Added user found in segment : " + dataSet.get("segmentName"));
		TestListeners.extentTest.get().pass("Added user found in segment : " + dataSet.get("segmentName"));
	}

	@Test(description = "SQ-T4658, Verify that The segment : containing most visited location = any is now populating users having more than 1 visits in any location", groups = {
			"regression", "dailyrun" })
	@Owner(name = "Rakhi Rawat")
	public void T4658_segmentContainingMostVisitedLocation() throws Exception {

		String segmentName = CreateDateTime.getUniqueString("Profile_Details_");
		// login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */

		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Go to Cockpit > Dashboard, ensure the required flag is turned OFF
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().navigateToTabs("Bento Apps");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_new_segments_homepage", "uncheck");
		pageObj.dashboardpage().updateCheckBox();

		// navigate to segments
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		// create new segment
		pageObj.segmentsBetaPage().createNewSegmentWithLocation(segmentName, dataSet.get("segmentType"),
				dataSet.get("attribute"), "Location(s)", "Any");

		pageObj.segmentsBetaPage().saveSegment(segmentName);
		String segmentId = pageObj.segmentsPage().getSegmentID();
		pageObj.segmentsPage().clickFirstUserFromSegment();

		String text = pageObj.guestTimelinePage().getGuestVisitCount();
		Assert.assertTrue(text.contains("has made"), "text message does not verified on guest timeline");
		Assert.assertTrue(text.contains("visit"), "Text message does not verified on guest timeline");
		logger.info("Text message verified on guest timeline :" + text);
		TestListeners.extentTest.get().pass("Text message verified on guest timeline :" + text);

		// deleting segment
		String query = "DELETE FROM `segment_definitions` WHERE `id` = '" + segmentId + "';";
		DBUtils.executeQuery(env, query);

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