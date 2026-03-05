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
public class NewSegmentHomePageTest {
	public WebDriver driver;
	private Properties prop;
	private PageObj pageObj;
	private String sTCName;
	private String env;
	private String baseUrl;
	private Utilities utils;
	private static Map<String, String> dataSet;
	String run = "ui";
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

	@Test(description = "SQ-T5521 Api for guest search in segment || "
			+ "SQ-T5522 API for fetching guest count from segment", groups = { "regression", "dailyrun" }, priority = 1)
	@Owner(name = "Hardik Bhardwaj")
	public void T5521_segmentAPIs() throws InterruptedException, IOException {

		// Login to instance. Select the business
//		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
//		pageObj.instanceDashboardPage().loginToInstance();

		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Go to Cockpit > Dashboard, ensure the required flag is turned OFF
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().navigateToTabs("Bento Apps");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_new_segments_homepage", "check");
		pageObj.dashboardpage().updateCheckBox();
		pageObj.dashboardpage().navigateToTabs("Bento Apps");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_show_new_and_classic_shp_switch_buttons", "check");
		pageObj.dashboardpage().updateCheckBox();

		// Go to Guests > Segments
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		pageObj.newSegmentHomePage().switchToNewSegmentManagementToolBtn();
		pageObj.newSegmentHomePage().verifyNewSegmentHomePage("All Segments");

		pageObj.newSegmentHomePage().searchSegmentonNewSegmentPage("All Signed Up", "withAssertion");
		pageObj.newSegmentHomePage().clickEllipsesOptions();
		pageObj.newSegmentHomePage().clickOptionsInEllipsesButton("View details");
		utils.waitTillPagePaceDone();
		boolean viewDetailsFlag = utils.verifyPartOfURL("segment-overview");
		Assert.assertTrue(viewDetailsFlag, "Not able to navigate to View details");
		utils.logit("Navigated to View details on New Segment Home Page");
		String segmentId = pageObj.segmentsPage().getSegmentID();
		int guestInSegmentCount = pageObj.segmentsBetaPage().getGuestInSegmentCount();
		userEmail = pageObj.segmentsBetaPage().getSegmentGuestList(dataSet.get("apiKey"), segmentId);

		// Segment size in API
		Response response = pageObj.endpoints().getSegmentCount(dataSet.get("apiKey"), segmentId);
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for Fetch segment count dashboard api");
		utils.logPass("Fetch segment count Dashboard API is Successful");
		String message = response.jsonPath().getString("message");
		Assert.assertEquals(message, "Fetched Count Successfully", "Not Fetched Count Successfully");
		String cnt = response.jsonPath().get("count");
		String cnt1 = cnt.replaceAll(",", "");
		int count = Integer.parseInt(cnt1);
		Assert.assertEquals(guestInSegmentCount, count, "segment guest count from api and UI is not matching");
		utils.logPass("Segment guest count from api and UI is matching");

		// verifying In_segment query using API
		Response userInSegmentResp2 = pageObj.endpoints().userInSegment(userEmail, dataSet.get("apiKey"), segmentId);
		Assert.assertEquals(userInSegmentResp2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for auth user signup api");
		utils.logPass("Verified that user " + userEmail + " is present in Segment");
		String result = userInSegmentResp2.jsonPath().get("result").toString();
		Assert.assertEquals(result, "true", "Guest is present in segment");
		utils.logPass("Verified that status of " + userEmail + " is present in Segment");

		// Go to Cockpit > Dashboard, ensure the required flag is turned OFF
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().navigateToTabs("Bento Apps");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_new_segments_homepage", "uncheck");
		pageObj.dashboardpage().updateCheckBox();
	}

// Commenting this case - covering this under SQ-T5952
//	@Test(description = "SQ-T5734 creation and deleteion of classic segment then verify audit log on new segment", groups = {
//			"regression", "dailyrun" }, priority = 2)
//	@Owner(name = "Sachin Bakshi")
	public void T5734_verifySegmentDeletionAuditlogTest() throws InterruptedException, IOException {
		// Login to instance. Select the business
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		utils.logit("Navigated to Guests > Segments");

		pageObj.newSegmentHomePage().clickOnSwitchToClassicBtn();
		pageObj.newSegmentHomePage().verifyClassicSegmentPage("Segments");

		String segmentName = CreateDateTime.getUniqueString("Profile_Details_Gender_Segment_");

		pageObj.segmentsBetaPage().createNewSegmentWithLocation(segmentName, dataSet.get("segmentType"),
				dataSet.get("attribute"), "Gender", dataSet.get("guestTypeValue"));
		pageObj.segmentsPage().saveAndShowSegmentBtn();
		String segmentId = pageObj.segmentsPage().getSegmentID();

		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		pageObj.segmentsPage().searchAndOpenSegment(segmentName, segmentId);
		pageObj.segmentsPage().deleteSegment(segmentName);
		pageObj.newSegmentHomePage().switchToNewSegmentManagementToolBtn();

		pageObj.newSegmentHomePage().clickAuditLogEllipseButton();
		pageObj.newSegmentHomePage().clickAuditLoAftergEllipseButton();
		utils.switchToWindow();
		utils.waitTillPagePaceDone();
		boolean flag = pageObj.segmentsPage().auditLogWithSegmentId(segmentId, "#ff0000");
		Assert.assertTrue(flag, "Segment deltetion log is not visible in Audit Logs");
		utils.logPass("Segment deltetion log is visible in Audit Logs");
	}

	@Test(description = "SQ-T5952 verified search field on segment homepage || "
			+ "SQ-T5734 creation and deleteion of classic segment then verify audit log on new segment", groups = {
					"regression" }, priority = 2)
	@Owner(name = "Hardik Bhardwaj")
	public void T5952_verifySearchField() throws Exception {

		// Login to instance. Select the business
//		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
//		pageObj.instanceDashboardPage().loginToInstance();

		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		pageObj.newSegmentHomePage().clickOnSwitchToClassicBtn();
		pageObj.newSegmentHomePage().verifyClassicSegmentPage("Segments");

		// Profile Details Gender Equal To Male

		String segmentName = CreateDateTime.getUniqueString("Profile_Details_Segment_&*@_");
		String ProfileDetailsGenderEqualToMale_builder_clause = utils
				.convertToDoubleStringified(segmentBuilderClauses.get("ProfileDetailsGenderEqualToMale"));

		Response segmentCreationUsingBuilderClauseResponse = pageObj.endpoints().segmentCreationUsingBuilderClause(
				dataSet.get("apiKey"), segmentName, ProfileDetailsGenderEqualToMale_builder_clause);
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
		Assert.assertTrue(segmentCount > 0, "Segment count is not greater than 0.");
		utils.logPass("Segment count is greater than 0  ie " + segmentCount);

		pageObj.segmentsPage().searchAndOpenSegment(segmentName, segmentId);
		pageObj.segmentsBetaPage().getGuestInSegmentCount();
		String userEmail = pageObj.segmentsBetaPage().getSegmentGuestList(dataSet.get("apiKey"), segmentId);

		// Searching with name
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		pageObj.newSegmentHomePage().switchToNewSegmentManagementToolBtn();
		pageObj.newSegmentHomePage().verifyNewSegmentHomePage("All Segments");
		pageObj.newSegmentHomePage().searchSegmentonNewSegmentPage(segmentName, "withoutAssertion");
		List<String> segmentNameList = pageObj.newSegmentHomePage().segmentList();
		String segmentNameOriginal = segmentNameList.get(0);
		Assert.assertEquals(segmentNameOriginal, segmentName, "Original Segment name is not matching");
		utils.logPass("Original Segment name is matching");

		// Searching with name with uppwecase
		String segmentNameUpperCase = segmentName.toUpperCase();
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		pageObj.newSegmentHomePage().searchSegmentonNewSegmentPage(segmentNameUpperCase, "withoutAssertion");
		List<String> segmentNameListUpperCase = pageObj.newSegmentHomePage().segmentList();
		String segmentUpperCaseName = segmentNameListUpperCase.get(0);
		Assert.assertEquals(segmentUpperCaseName, segmentName, "Uppercase Segment name is not matching");
		utils.logPass("Uppercase Segment name is matching");

		// Searching segment name with lowercase
		String segmentNameLowerCase = segmentName.toLowerCase();
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		pageObj.newSegmentHomePage().searchSegmentonNewSegmentPage(segmentNameLowerCase, "withoutAssertion");
		List<String> segmentNameListLowerCase = pageObj.newSegmentHomePage().segmentList();
		String segmentLowerCaseName = segmentNameListLowerCase.get(0);
		Assert.assertEquals(segmentLowerCaseName, segmentName, "Lowercase Segment name is not matching");
		utils.logPass("Lowercase Segment name is matching");

		pageObj.newSegmentHomePage().clickOnSwitchToClassicBtn();
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		pageObj.segmentsPage().searchAndOpenSegment(segmentName, segmentId);
		pageObj.segmentsPage().deleteSegment(segmentName);
		pageObj.newSegmentHomePage().switchToNewSegmentManagementToolBtn();

		pageObj.newSegmentHomePage().clickAuditLogEllipseButton();
		pageObj.newSegmentHomePage().clickAuditLoAftergEllipseButton();
		utils.switchToWindow();
		utils.waitTillPagePaceDone();
		boolean flag = pageObj.segmentsPage().auditLogWithSegmentId(segmentId, "#37c936");
		Assert.assertTrue(flag, "Segment deltetion log is not visible in Audit Logs");
		utils.logPass("Segment deltetion log is visible in Audit Logs");

//		// Deleting segment for New Segment Home Page
//		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
//		pageObj.newSegmentHomePage().searchSegmentonNewSegmentPage(segmentName, "withoutAssertion");
//		pageObj.newSegmentHomePage().clickEllipsesOptions();
//		pageObj.newSegmentHomePage().clickOptionsInEllipsesButton("Delete");
//		pageObj.newSegmentHomePage().clickOnYesDelete();
//		pageObj.newSegmentHomePage().searchSegmentonNewSegmentPage(segmentName, "withoutAssertion");
//		List<String> segmentNameListDeletion = pageObj.newSegmentHomePage().segmentList();
//		String segmentLowerAfterDeletion = segmentNameListDeletion.get(0);
//		Assert.assertEquals(segmentLowerAfterDeletion, "", "Segment is not deleted");
//		pageObj.utils().logit("Segment is deleted " + segmentName);
//		pageObj.utils().logPass("Segment is deleted " + segmentName);
	}

	@Test(description = "SQ-T6067: Verify that a new segment tag is created correctly", priority = 3)
	@Owner(name = "Hardik Bhardwaj")
	public void T6067_NewSegmentHomePageTagTest() throws InterruptedException, IOException {

		String tagName = CreateDateTime.getUniqueString("Tag@&");
		// Login to instance. Select the business
//		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
//		pageObj.instanceDashboardPage().loginToInstance();

		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Go to Cockpit > Dashboard, ensure the required flag is turned OFF
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().navigateToTabs("Bento Apps");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_new_segments_homepage", "check");
		pageObj.dashboardpage().updateCheckBox();
		pageObj.dashboardpage().navigateToTabs("Bento Apps");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_show_new_and_classic_shp_switch_buttons", "check");
		pageObj.dashboardpage().updateCheckBox();

		// Go to Guests > Segments
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		utils.logit("Navigated to Guests > Segments");

		// Create Tag
		pageObj.newSegmentHomePage().switchToNewSegmentManagementToolBtn();
		pageObj.newSegmentHomePage().clickOnManageTagButton();
		pageObj.newSegmentHomePage().clickOnCreateTag("Manage Tags", tagName);
		pageObj.newSegmentHomePage().closeManageTagOrTag("Manage Tags");
		String segName = pageObj.newSegmentHomePage().getSegmentNameOnSegmentHomePage();
		utils.logit("Segment name is " + segName);

		// Get Segment
		pageObj.newSegmentHomePage().searchSegmentonNewSegmentPage(segName, "withAssertion");
		pageObj.newSegmentHomePage().clickEllipsesOptions();

		// Attaching tag to segment
		pageObj.newSegmentHomePage().clickOptionsInEllipsesButton("Tag");
		pageObj.newSegmentHomePage().selectTagForSegment(tagName);
		List<String> message = pageObj.newCamHomePage().clickApplyBtn();
		Assert.assertEquals(message.get(0), "Tag(s) updated successfully", "Segment did not update with tags");
		utils.logit("Successfully added tags " + tagName + " in the segment " + segName);
		utils.logit("Created a new tag " + tagName);

		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		pageObj.newSegmentHomePage().switchToNewSegmentManagementToolBtn();

		// Filter by Tag
		pageObj.newSegmentHomePage().clickOnMoreFilterButton();
		pageObj.newSegmentHomePage().selectDrpDownValFromSidePanel(tagName, "Tag");
		pageObj.newSegmentHomePage().sidePanelDrpDownExpand("Tag");
		pageObj.newSegmentHomePage().clickSidePanelApplyBtn();
		utils.longWaitInSeconds(3);
		// Check Tagged Segment
		boolean anyTagFlag = false;
		List<String> campNameList = pageObj.newSegmentHomePage().searchedSegmentList();
		if (campNameList.contains(segName)) {
			anyTagFlag = true;
		}

		Assert.assertTrue(anyTagFlag, "Segment is not visible after applying Filter by Tag");
		utils.logPass("Verified that the page retrive data with the tag that we selected");

		// Delete Tag
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		pageObj.newSegmentHomePage().clickOnManageTagButton();
		pageObj.newSegmentHomePage().deleteTag(tagName);
		pageObj.newSegmentHomePage().closeManageTagOrTag("Manage Tags");
	}

	@Test(description = "SQ-T6661 verify Filter option on segment list page", priority = 4)
	@Owner(name = "Hardik Bhardwaj")
	public void T6661_NewSegmentHomePageSidePanelDesignTest() throws InterruptedException, IOException {

		// Login to instance. Select the business
//		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
//		pageObj.instanceDashboardPage().loginToInstance();

		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Go to Cockpit > Dashboard, ensure the required flag is turned OFF
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().navigateToTabs("Bento Apps");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_new_segments_homepage", "check");
		pageObj.dashboardpage().updateCheckBox();
		pageObj.dashboardpage().navigateToTabs("Bento Apps");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_show_new_and_classic_shp_switch_buttons", "check");
		pageObj.dashboardpage().updateCheckBox();

		// Go to Guests > Segments
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		utils.logit("Navigated to Guests > Segments");
		String segmentName = "All Signed Up";

		// Filter by Segment type (positive scenario)
		pageObj.newSegmentHomePage().clickOnMoreFilterButton();
		pageObj.newSegmentHomePage().selectDrpDownValFromSidePanel("Profile Details", "Segment type");
		pageObj.newSegmentHomePage().sidePanelDrpDownExpand("Segment type");
		pageObj.newSegmentHomePage().clickSidePanelApplyBtn();
		utils.longWaitInSeconds(3);
		pageObj.newSegmentHomePage().searchSegmentonNewSegmentPage(segmentName, "withoutAssertion");
		List<String> segmentNameList = pageObj.newSegmentHomePage().segmentList();
		String segmentSearchedName = segmentNameList.get(0);
		Assert.assertEquals(segmentSearchedName, segmentName, "Segment is not found in search");
		utils.logPass("Segment is found in search " + segmentName);

		// Go to Guests > Segments
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		utils.logit("Navigated to Guests > Segments");

		// Filter by Segment type (negative scenario)
		pageObj.newSegmentHomePage().clickOnMoreFilterButton();
		pageObj.newSegmentHomePage().selectDrpDownValFromSidePanel("Average Checkins", "Segment type");
		pageObj.newSegmentHomePage().sidePanelDrpDownExpand("Segment type");
		pageObj.newSegmentHomePage().clickSidePanelApplyBtn();
		utils.longWaitInSeconds(3);
		pageObj.newSegmentHomePage().searchSegment(segmentName);
		segmentNameList = pageObj.newSegmentHomePage().segmentList();
		boolean flag = segmentNameList.isEmpty();
		Assert.assertTrue(flag, "Segment is found in searched filter i.e. Segment type");
		utils.logPass(segmentName + " Segment is not found in searched filter i.e. Segment type");

		// Go to Guests > Segments
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		utils.logit("Navigated to Guests > Segments");

		// Filter by Linked to active campaigns (positive scenario)
		pageObj.newSegmentHomePage().clickOnMoreFilterButton();
		pageObj.newSegmentHomePage().selectCheckBoxOptionInFilterPanel("Linked to active campaigns");
		pageObj.newSegmentHomePage().clickSidePanelApplyBtn();
		utils.longWaitInSeconds(3);
		segmentName = "SQ-T6381_NonTransactionalSegment";
		pageObj.newSegmentHomePage().searchSegmentonNewSegmentPage(segmentName, "withoutAssertion");
		segmentNameList = pageObj.newSegmentHomePage().segmentList();
		segmentSearchedName = segmentNameList.get(0);
		Assert.assertEquals(segmentSearchedName, segmentName, "Segment is not found in search");
		utils.logPass("Segment is found in search " + segmentName);

		String status = pageObj.newSegmentHomePage().getCampaignLinksStatusForSearchedSegment(segmentName);
		Assert.assertEquals(status, "Active",
				"Campaign status with filter Linked to active campaigns is not active for segment " + segmentName);
		utils.logPass("Campaign status with filter Linked to active campaigns is active for segment " + segmentName);

		// Filter by Linked to active campaigns (negative scenario)
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		pageObj.newSegmentHomePage().clickOnMoreFilterButton();
		pageObj.newSegmentHomePage().selectCheckBoxOptionInFilterPanel("Linked to active campaigns");
		pageObj.newSegmentHomePage().clickSidePanelApplyBtn();
		segmentName = "DND_Automation_ProfileDetails_T6659";
		utils.longWaitInSeconds(3);
		pageObj.newSegmentHomePage().searchSegment(segmentName);
		segmentNameList = pageObj.newSegmentHomePage().segmentList();
		flag = segmentNameList.isEmpty();
		Assert.assertTrue(flag, "Segment is found in searched filter i.e. Linked to active campaigns");
		pageObj.utils().logPass(segmentName + " Segment is not found in searched filter i.e. Linked to active campaigns");

		// Filter by Linked to non-active campaigns (positive scenario)
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		pageObj.newSegmentHomePage().clickOnMoreFilterButton();
		pageObj.newSegmentHomePage().selectCheckBoxOptionInFilterPanel("Linked to non-active campaigns");
		pageObj.newSegmentHomePage().clickSidePanelApplyBtn();
		utils.longWaitInSeconds(3);
		pageObj.newSegmentHomePage().searchSegmentonNewSegmentPage(segmentName, "withoutAssertion");
		segmentNameList = pageObj.newSegmentHomePage().segmentList();
		segmentSearchedName = segmentNameList.get(0);
		Assert.assertEquals(segmentSearchedName, segmentName, "Segment is not found in search");
		utils.logPass("Segment is found in search " + segmentName);

		status = pageObj.newSegmentHomePage().getCampaignLinksStatusForSearchedSegment(segmentName);
		Assert.assertEquals(status, "Non-active",
				"Campaign status with filter Linked to non-active campaigns is not Non-active for segment "
						+ segmentName);
		pageObj.utils().logit(
				"Campaign status with filter Linked to non-active campaigns is Non-active for segment " + segmentName);
		pageObj.utils().logPass(
				"Campaign status with filter Linked to non-active campaigns is Non-active for segment " + segmentName);

		// Filter by Linked to non-active campaigns (negative scenario)
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		pageObj.newSegmentHomePage().clickOnMoreFilterButton();
		pageObj.newSegmentHomePage().selectCheckBoxOptionInFilterPanel("Linked to non-active campaigns");
		pageObj.newSegmentHomePage().clickSidePanelApplyBtn();
		segmentName = "DND_Automation_ProfileDetails_NoCampaign_T6659";
		utils.longWaitInSeconds(3);
		pageObj.newSegmentHomePage().searchSegment(segmentName);
		segmentNameList = pageObj.newSegmentHomePage().segmentList();
		flag = segmentNameList.isEmpty();
		Assert.assertTrue(flag, "Segment is found in searched filter i.e. Linked to non-active campaigns");
		pageObj.utils().logPass(segmentName + " Segment is not found in searched filter i.e. Linked to non-active campaigns");

		// Filter by No link to any campaign (positive scenario)
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		pageObj.newSegmentHomePage().clickOnMoreFilterButton();
		pageObj.newSegmentHomePage().selectCheckBoxOptionInFilterPanel("No link to any campaign");
		pageObj.newSegmentHomePage().clickSidePanelApplyBtn();
		utils.longWaitInSeconds(3);
		pageObj.newSegmentHomePage().searchSegmentonNewSegmentPage(segmentName, "withoutAssertion");
		segmentNameList = pageObj.newSegmentHomePage().segmentList();
		segmentSearchedName = segmentNameList.get(0);
		Assert.assertEquals(segmentSearchedName, segmentName, "Segment is not found in search");
		utils.logPass("Segment is found in search " + segmentName);

		status = pageObj.newSegmentHomePage().getCampaignLinksStatusForSearchedSegment(segmentName);
		Assert.assertEquals(status, "-",
				"Campaign status with filter No link to any campaign is not - for segment " + segmentName);
		pageObj.utils().logPass("Campaign status with filter No link to any campaign is - for segment " + segmentName);

		// Filter by No link to any campaign (negative scenario)
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		pageObj.newSegmentHomePage().clickOnMoreFilterButton();
		pageObj.newSegmentHomePage().selectCheckBoxOptionInFilterPanel("No link to any campaign");
		pageObj.newSegmentHomePage().clickSidePanelApplyBtn();
		segmentName = "SQ-T6381_NonTransactionalSegment";
		utils.longWaitInSeconds(3);
		pageObj.newSegmentHomePage().searchSegment(segmentName);
		segmentNameList = pageObj.newSegmentHomePage().segmentList();
		flag = segmentNameList.isEmpty();
		Assert.assertTrue(flag, "Segment is found in searched filter i.e. No link to any campaign");
		pageObj.utils().logPass(segmentName + " Segment is not found in searched filter i.e. No link to any campaign");

	}

	@SuppressWarnings("static-access")
	@Test(description = "SQ-T6659 verify segment side panel design working as expected or not in bento || "
			+ "SQ-216, Segment Show Page -> Segment Count || "
			+ "	SQ-456, Validate Profile Details based type segment || "
			+ "	SQ-T2200, Segment Show Page -> User Presence in segment || "
			+ "	SQ-T6416 Verify segment reachbility showing properly", priority = 5)
	@Owner(name = "Hardik Bhardwaj")
	public void T6659_NewSegmentHomePageSidePanelTest() throws Exception {
		// Login to instance. Select the business
//		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
//		pageObj.instanceDashboardPage().loginToInstance();

		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Go to Cockpit > Dashboard, ensure the required flag is turned OFF
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().navigateToTabs("Bento Apps");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_new_segments_homepage", "check");
		pageObj.dashboardpage().updateCheckBox();
		pageObj.dashboardpage().navigateToTabs("Bento Apps");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_show_new_and_classic_shp_switch_buttons", "check");
		pageObj.dashboardpage().updateCheckBox();

		// Go to Guests > Segments
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		utils.logit("Navigated to Guests > Segments");

		pageObj.newSegmentHomePage().switchToNewSegmentManagementToolBtn();
		utils.waitTillNewCamsTableAppear();
		pageObj.newSegmentHomePage().waitTillSegmentSidePanelClickable(dataSet.get("segmentName"), 4);
		utils.waitTillSegmentSidePanelClickable();
		String segmentSizeSidePanel = utils.removePercentageBracketsAndGetPercentage(
				pageObj.newSegmentHomePage().getSidePanelOptionValue("Segment size"));
		String reachabilitySidePanel = utils.removePercentageBracketsAndGetPercentage(
				pageObj.newSegmentHomePage().getSidePanelOptionValue("Reachability"));
		String emailSidePanel = utils.removePercentageBracketsAndGetPercentage(
				pageObj.newSegmentHomePage().getSidePanelOptionValue("Email"));
		String pushNotificationSidePanel = utils.removePercentageBracketsAndGetPercentage(
				pageObj.newSegmentHomePage().getSidePanelOptionValue("Push Notification"));
		String smsSidePanel = utils
				.removePercentageBracketsAndGetPercentage(pageObj.newSegmentHomePage().getSidePanelOptionValue("SMS"));
		String notSubscribedSidePanel = utils.removePercentageBracketsAndGetPercentage(
				pageObj.newSegmentHomePage().getSidePanelOptionValue("Not Subscribed"));
		try {
		String sampleMatchingGuestsSidePanel = pageObj.newSegmentHomePage()
				.getSidePanelOptionValue("Sample matching guests");
//		 	Fetching user id for users table
			String query2 = "Select `email` from `users` WHERE `id` = \"" + sampleMatchingGuestsSidePanel + "\" ;";
			String userEmail = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query2,
					"email", 5);
			utils.logit("userEmail column in users table is equal to " + userEmail);

			// verifying In_segment query using API
			Response userInSegmentResp2 = pageObj.endpoints().userInSegment(userEmail, dataSet.get("apiKey"),
					dataSet.get("segmentId"));
			Assert.assertEquals(userInSegmentResp2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
					"Status code 200 did not matched for auth user signup api");
			utils.logPass("Verified that user " + sampleMatchingGuestsSidePanel + " is present in Segment");
			String result = userInSegmentResp2.jsonPath().get("result").toString();
			Assert.assertEquals(result, "true", "Guest is present in segment");
			utils.logPass("Verified that status of " + sampleMatchingGuestsSidePanel + " is present in Segment");
		} catch (Exception e) {
			utils.logit("Sample matching guests is not present in side panel");
		}

		pageObj.newSegmentHomePage().editOrViewSegmentInsideOnSidePanel("View details and insights");
		utils.waitTillSpinnerDisappear();
		pageObj.segmentsBetaPage().waitTillCustomerReachabilityClickable();

		String customerReachabilityCount = Integer
				.toString(pageObj.segmentsBetaPage().getSegmentCustomerReachabilityCount());
		pageObj.newSegmentHomePage().customerReachabilityRefresh();
		pageObj.segmentsBetaPage().waitTillCustomerReachabilityClickable();
		String emailPerformanceMatric = utils
				.extractLeadingInteger(pageObj.newSegmentHomePage().performanceMatricData("Email"));
		String pushNotificationPerformanceMatric = utils
				.extractLeadingInteger(pageObj.newSegmentHomePage()
				.performanceMatricData("Push Notification"));
		String smsPerformanceMatric = utils
				.extractLeadingInteger(pageObj.newSegmentHomePage().performanceMatricData("SMS"));
		String notSubscribedPerformanceMatric = utils
				.extractLeadingInteger(pageObj.newSegmentHomePage().performanceMatricData("Not Subscribed"));

		Assert.assertEquals(segmentSizeSidePanel, customerReachabilityCount,
				"Segment size count is not matching with customer reachability count");
		utils.logPass("Segment size count is matching with customer reachability count");

		Assert.assertEquals(reachabilitySidePanel, customerReachabilityCount,
				"Reachability count is not matching with customer reachability count");
		utils.logPass("Reachability count is matching with customer reachability count");

		Assert.assertEquals(emailPerformanceMatric, emailSidePanel,
				"Email count is not matching with Email performance matric count");
		utils.logPass("Email count is matching with Email performance matric count");

		Assert.assertEquals(pushNotificationPerformanceMatric, pushNotificationSidePanel,
				"Push Notification count is not matching with Push Notification performance matric count");
		utils.logPass("Push Notification count is matching with Push Notification performance matric count");

		Assert.assertEquals(smsPerformanceMatric, smsSidePanel,
				"SMS count is not matching with SMS performance matric count");
		utils.logPass("SMS count is matching with SMS performance matric count");

		Assert.assertEquals(notSubscribedPerformanceMatric, notSubscribedSidePanel,
				"Not Subscribed count is not matching with Not Subscribed performance matric count");
		utils.logPass("Not Subscribed count is matching with Not Subscribed performance matric count");
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
