package com.punchh.server.loyalty2;

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
import com.punchh.server.apimodel.lineitemselector.LineItemSelectorFilterItemSet;
import com.punchh.server.pages.ApiPayloadObj;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.MessagesConstants;
import com.punchh.server.utilities.NewMenu;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class LocationGroupTest {
	static Logger logger = LogManager.getLogger(LocationGroupTest.class);

	public WebDriver driver;
	private Properties prop;
	private PageObj pageObj;
	private String sTCName;
	private String env, run = "ui";
	private String baseUrl;
	private static Map<String, String> dataSet;
	private Utilities utils;
	private ApiPayloadObj apipayloadObj;
	private String lisExternalID ,qcExternalID, redeemableExternalID;;

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
		prop = Utilities.loadPropertiesFile("apiConfig.properties");
		driver = new BrowserUtilities().launchBrowser();
		sTCName = method.getName();
		pageObj = new PageObj(driver);
		utils = new Utilities(driver);
		apipayloadObj = new ApiPayloadObj();
		env = pageObj.getEnvDetails().setEnv();
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run , env , "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==>" + dataSet);
	}

	// Author: Amit
	@Test(description = "LPE-T2141, LPE-2137 Verify LG with 0 locations cannot be created"
			+ "LPE-T2139 Verify LG with more than 1 location can be created", priority = 0)
	@Owner(name = "Amit Kumar")
	public void LPE_T2141_verifyLGWith0LocationsCannotBeCreated() throws InterruptedException {
		// Instance login and goto dashboard
		// LPE-T2141
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Settings", "Location Groups");
		pageObj.locationPage().selectAddnewLocationGruoup();
		pageObj.iframeConfigurationPage().clickOnPageTab("Location Groups");
		pageObj.locationPage().searchLocationOnLocationGroupPage(dataSet.get("invalidLocatinonName"));
		String status = pageObj.locationPage().CheckNoresultFoundFOrInvalidLocation();
		Assert.assertEquals(status, "No Result Found", "No results found message should be displayed");
		pageObj.locationPage().seletctAllLocationsgroupsCheckbox();
		boolean disabledStatus = pageObj.locationPage().checkCretateLocationGroupBtnDisabled();
		Assert.assertFalse(disabledStatus, "create location group button should be disabled");
		TestListeners.extentTest.get().pass("validated location group creation wit 0 locations on locations group tab");

		// LPE-T2137
		pageObj.iframeConfigurationPage().clickOnPageTab("Locations");
		pageObj.locationPage().searchLocationOnLocationGroupPage(dataSet.get("invalidLocatinonName"));
		String status1 = pageObj.locationPage().CheckNoresultFoundFOrInvalidLocation();
		Assert.assertEquals(status1, "No Result Found", "No results found message should be displayed");
		pageObj.locationPage().seletctAllLocationsgroupsCheckbox();
		boolean disabledStatus1 = pageObj.locationPage().checkCretateLocationGroupBtnDisabled();
		Assert.assertFalse(disabledStatus1, "create location group button should be disabled");
		TestListeners.extentTest.get().pass("validated location group creation wit 0 locations on locations tab");

		// Add New Location LPE-T2139
		String locationGroupName = "AutoamtionLocationGroup_" + CreateDateTime.getTimeDateString();
		pageObj.menupage().navigateToSubMenuItem("Settings", "Location Groups");
		pageObj.locationPage().selectAddnewLocationGruoup();
		pageObj.locationPage().searchLocationOnLocationGroupPage(dataSet.get("validLocatinonName"));
		pageObj.locationPage().seletctAllLocationsgroupsCheckbox();
		String msg = pageObj.locationPage().createLocationGroup(locationGroupName);
		Assert.assertEquals(msg, "Location Group created", "Location Group should be created successfully");
		pageObj.locationPage().deleteLocationGroup(locationGroupName);
		TestListeners.extentTest.get().pass("validated location group creation and deletion for admin");

	}

	// Author: Amit
	@Test(description = "LPE-T2102, LPE-T2103, LPE-T2104, LPE-T2116  Verify Archived LG's are not available in dropdown on new QC page when enable_location_group_archive is true", priority = 1)
	@Owner(name = "Amit Kumar")
	public void LPE_T2102_verifyArchivedLGAreNotAvailableInDropdownOnNewQCPageWhenEnableLocationGroupArchiveIsTrue()
			throws Exception {
		String lisName = "AutoSelect_LIS_LPE_T2102_" + Utilities.getTimestamp();
		String qcname = "AutoSelect_QC_LPE_T2102_" + Utilities.getTimestamp();
		// Create LIS with base item 101,102,103,104
		String lisPayload = apipayloadObj.lineItemSelectorBuilder().setName(lisName)
				.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY).addBaseItemClause("item_id", "in", "101,102")
				.build();
		// Create LIS
		Response lisResponse = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));
		logger.info("API response: " + lisResponse.prettyPrint());

		// Get LIS External ID
		lisExternalID 	= lisResponse.jsonPath().getString("results[0].external_id");
		utils.logPass(lisName + " LIS is Created with External ID: " + lisExternalID);

		// Create QC-payload with 100% off on total amount of items matching LIS
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname)
				.setQCProcessingFunction("sum_amounts").addLineItemFilter(lisExternalID, "", 0)
				.addItemQualifier("line_item_exists", lisExternalID, 0.0, 0).build();

		// Create QC
		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		logger.info("API response: " + qcResponse.prettyPrint());

		// Get QC External ID
		qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		utils.logPass(qcname + "QC is created with External ID: " + qcExternalID);

		// enable_location_group_archive -> true [businesses -> preferences]
		// Business has archived LG's created
		// Instance login and goto dashboard LPE-T2102
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");
		pageObj.qualificationcriteriapage().clickOnAddNewQualificationCriteria();
		List<String> optionsList = pageObj.qualificationcriteriapage().getEffectivelocationDrpList();
		Assert.assertFalse(optionsList.contains(dataSet.get("archivedLGName")),
				"List does not contain archived LG name");
		TestListeners.extentTest.get().pass("Archived LG name is not present in the effective location dropdown list");

		// LPE-T2104
		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");
		pageObj.qualificationcriteriapage().SearchQC(qcname);
		List<String> optionsList2 = pageObj.qualificationcriteriapage().getEffectivelocationDrpList();
		Assert.assertFalse(optionsList2.contains(dataSet.get("archivedLGName")),
				"List does not contain archived LG name");
		TestListeners.extentTest.get().pass("Archived LG name is not present in the effective location dropdown list");

		// create a new location group and attach to q
		String store_number = CreateDateTime.getTimeDateString();
		String locationGroupName = "Location Group" + store_number;
		Response createLocationGroupresponse = pageObj.endpoints().createLocationgroup(locationGroupName,
				dataSet.get("storeNo"), dataSet.get("location_id"), dataSet.get("apiKey"));
		Assert.assertEquals(createLocationGroupresponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for PLATFORM FUNCTIONS API Create Location Group");
		// create qc and attach location group
		String qcName = "TestQc" + CreateDateTime.getTimeDateString();
		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");
		pageObj.qualificationcriteriapage().setQCName(qcName);
		pageObj.qualificationcriteriapage().setProcessingFunction("Sum of Amounts Incremental");
		pageObj.qualificationcriteriapage().setEfectivelocationQC(locationGroupName);
		String qcSuccessMsg = pageObj.qualificationcriteriapage().createQC();
		Assert.assertEquals(qcSuccessMsg, "Qualification Criterion created",
				"Qualification Criterion created success message not displayed");

		// archive attached location group
		pageObj.menupage().navigateToSubMenuItem("Settings", "Location Groups");
		pageObj.locationPage().searchLocationGroup(locationGroupName);
		pageObj.locationPage().archiveLocationsGroup();

		// edit qc and remove archived location group and attach active location group
		// and save
		// LPE-T2103, LPE-T2116
		String archivedName = "(Archived) " + locationGroupName;
		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");
		pageObj.qualificationcriteriapage().SearchQC(qcName);
		String val3 = pageObj.qualificationcriteriapage().getAlreadyAttachedEffectiveLocationValue(archivedName);
		List<String> optionsList3 = pageObj.qualificationcriteriapage().getEffectivelocationDrpList();
		Assert.assertEquals(val3, archivedName,
				"Effective location dropdown's already selected value should match archived LG name");
		Assert.assertTrue(optionsList3.contains(archivedName), "List does not contain archived LG name");
		TestListeners.extentTest.get().pass("Archived LG name is present in the effective location dropdown list");

		// LPE-T2123
		pageObj.qualificationcriteriapage().setEfectivelocationQC(dataSet.get("activeLGName"));
		String qcSuccessMsg1 = pageObj.qualificationcriteriapage().createQC();
		Assert.assertEquals(qcSuccessMsg1, "Qualification Criterion updated",
				"Qualification Criterion created success message not displayed");

		// cleanup qc and archived location gropup

	}

	@Test(description = "SQ-T6650: Verify admin without relevant permission is unable to access Bulk Action button; "
			+ "SQ-T6668: Verify admin without relevant permission is unable to access Export Location List button; "
			+ "SQ-T6708: Verify Bulk Action should not be visible to site Admin")
	@Owner(name = "Vaibhav Agnihotri")
	public void T6650_bulkActionUnavailableWithoutRelevantPermission() throws Exception {
		// Login with a Site admin without Business Manager permission
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance(dataSet.get("username"), prop.getProperty("password"));

		// Verify that 'Location Groups' sub menu is available or not
		List<String> storeLocationsSubMenusList = pageObj.menupage().subMenuItems(NewMenu.menu_StoreLocations);
		boolean isLocationGroupsAvailable = storeLocationsSubMenusList.contains("Location Groups");
		Assert.assertFalse(isLocationGroupsAvailable, "Location Groups sub menu is available.");
		utils.logPass("Location Groups sub menu is not available for admin: " + dataSet.get("username"));

		// Verify that 'Export Location List' is not on Locations page options
		pageObj.menupage().navigateToSubMenuItem("Settings", "Locations");
		boolean isLocationBulkActionPresent = pageObj.segmentsBetaPage()
				.verifyPresenceAndClick(utils.getLocatorValue("locationPage.bulkActionButton"));
		Assert.assertFalse(isLocationBulkActionPresent, "Bulk Action button is available.");
		pageObj.locationPage().clickElipsisButton();
		boolean isExportLocationListOptionPresent = pageObj.segmentsBetaPage()
				.verifyPresenceAndClick(utils.getLocatorValue("locationPage.exportLocationListLink"));
		Assert.assertFalse(isExportLocationListOptionPresent, "Export Location List option is available.");
		utils.logPass("Export Location List option is not available for admin: " + dataSet.get("username"));

	}

	@Test(description = "SQ-T6667: Verify admin with relevant permission is able to access Export Location List button; "
			+ "SQ-T6669: Verify CSV export file contains all [approved / disapproved] locations of the selected business; "
			+ "SQ-T6902: Verify that a business admins can access all franchise and LGs.")
	@Owner(name = "Vaibhav Agnihotri")
	public void T6667_exportLocationListOnLocationsPage() throws Exception {
		/*
		 * Pre-requisites: 1) Cockpit > Dashboard > Misc Config: Stage=Live, Business
		 * Live Now?=ON. 2) AWS Secrets for Local Env contains: MANDRILL_USERNAME &
		 * MANDRILL_PASSWORD. 3) Login with an Admin having 'Business Owner' role having
		 * 'Franchisee Level Access' permission but not associated with any franchisee.
		 */
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance(dataSet.get("username"), prop.getProperty("password"));
		pageObj.menupage().navigateToSubMenuItem("Settings", "Locations");

		// Commented out as admin email validation can only be done on local env
		/* String downloadedFilePath = Paths.get(System.getProperty("user.dir"), "resources", "ExportData").toString();
		pageObj.guestTimelinePage().createAndCleanDownloadBrowserDownloadFolder(downloadedFilePath); */
		
		// Verify 'Export Location List' on Locations page
		pageObj.menupage().navigateToSubMenuItem("Settings", "Locations");
		pageObj.locationPage().clickElipsisButton();
		pageObj.segmentsBetaPage().verifyPresenceAndClick(utils.getLocatorValue("locationPage.exportLocationListLink"));
		Assert.assertEquals(utils.getSuccessMessage(), MessagesConstants.locationListExportSuccessMessage);
		utils.logPass("Export Location List option is clicked and success message is verified.");

		/* if (env.contains("local")) {
			// Verify email body and attachment
			Map<String, Object> emailData = GmailConnection.getGmailEmailBodyAndAttachments("subject",
					MessagesConstants.locationListExportEmailSubject);
			String body = (String) emailData.get("body");
			String downloadedFileName = ((List<String>) emailData.get("attachments")).get(0);
			String expectedEmailText = MessagesConstants.locationListExportEmailBody;
			Assert.assertTrue(body.contains(expectedEmailText));
			String outputFilePath = Paths.get(downloadedFilePath, downloadedFileName).toString();
			File outputFile = new File(outputFilePath);
			List<Map<String, String>> outputFileContent = ExcelUtils.readCSV(outputFile);
			utils.logit(downloadedFileName + " contents: \n" + outputFileContent);
			boolean isApprovedAndDisapprovedLocationPresent = pageObj.locationPage()
					.verifyApprovedAndDisapprovedLocations(outputFileContent, "Automation");
			Assert.assertTrue(isApprovedAndDisapprovedLocationPresent);
			utils.logPass(
					"Verified that email received for exporting the locations list contains a CSV file with approved and disapproved locations.");
		} */

		// Verify that expected Franchise is listed
		pageObj.menupage().navigateToSubMenuItem("Settings", "Admin Users");
		pageObj.dashboardpage().navigateToTabs("Franchises");
		utils.waitTillPagePaceDone();
		String franchiseName = pageObj.iframeConfigurationPage().getElementText("adminRolesPage.franchiseListedName",
				dataSet.get("franchiseId"));
		Assert.assertEquals(franchiseName, dataSet.get("franchiseName"));
		utils.logPass("Franchise " + franchiseName + " is listed under Franchises tab.");

		// Verify 'Export Franchise List' on Franchises page
		pageObj.locationPage().clickElipsisButton();
		pageObj.segmentsBetaPage()
				.verifyPresenceAndClick(utils.getLocatorValue("adminRolesPage.exportFranchiseListLink"));
		Assert.assertEquals(utils.getSuccessMessage(), MessagesConstants.franchiseListExportSuccessMessage);
		utils.logPass("Export Franchise List option is clicked and success message is verified.");

		// Verify that expected Location Groups are listed
		String franchiseLocationGroupName = dataSet.get("franchiseLocationGroupName");
		String nonFranchiseLocationGroupName = dataSet.get("nonFranchiseLocationGroupName");
		pageObj.menupage().navigateToSubMenuItem("Settings", "Location Groups");
		String franchiseLocationGroupId = pageObj.locationPage().searchLocationGroupAndGetId(franchiseLocationGroupName);
		Assert.assertEquals(franchiseLocationGroupId, dataSet.get("franchiseLocationGroupId"));
		String nonFranchiseLocationGroupId = pageObj.locationPage().searchLocationGroupAndGetId(nonFranchiseLocationGroupName);
		Assert.assertEquals(nonFranchiseLocationGroupId, dataSet.get("nonFranchiseLocationGroupId"));
		utils.logPass("Both Franchisee '" + franchiseLocationGroupName + "' and Non-Franchisee LG '"
				+ nonFranchiseLocationGroupName + "' are listed.");

		// Verify 'Export Location Group List' on Locations Groups page
		pageObj.locationPage().clickElipsisButton();
		pageObj.segmentsBetaPage()
				.verifyPresenceAndClick(utils.getLocatorValue("locationPage.exportLocationGroupListLink"));
		Assert.assertEquals(utils.getSuccessMessage(), MessagesConstants.locationGroupListExportSuccessMessage);
		utils.logPass("Export Location List option is clicked and success message is verified.");

	}

	@Test(description = "SQ-T6672: Verify when Franchisee admin exports the CSV file, it contains locations of the associated Franchisee only [not all business locations]; "
			+ "SQ-T6901: Verify that a franchise Admin can only access the assigned franchise and its linked LGs.")
	@Owner(name = "Vaibhav Agnihotri")
	public void T6672_exportLocationListForFranchisee() throws Exception {
		/*
		 * Pre-requisites: 1) Cockpit > Dashboard > Misc Config: Stage=Live, Business
		 * Live Now?=ON. 2) AWS Secrets for Local Env contains: MANDRILL_USERNAME &
		 * MANDRILL_PASSWORD. 3) Login with an Admin having 'Business Owner' role and
		 * 'Franchisee Level Access' permission
		 */
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance(dataSet.get("username"), prop.getProperty("password"));
		pageObj.menupage().navigateToSubMenuItem("Settings", "Locations");

		// Commented out as admin email validation can only be done on local env
		/* String downloadedFilePath = Paths.get(System.getProperty("user.dir"), "resources", "ExportData").toString();
		pageObj.guestTimelinePage().createAndCleanDownloadBrowserDownloadFolder(downloadedFilePath); */
		
		// Verify 'Export Location List' on Locations page
		pageObj.menupage().navigateToSubMenuItem("Settings", "Locations");
		pageObj.locationPage().clickElipsisButton();
		pageObj.segmentsBetaPage().verifyPresenceAndClick(utils.getLocatorValue("locationPage.exportLocationListLink"));
		Assert.assertEquals(utils.getSuccessMessage(), MessagesConstants.locationListExportSuccessMessage);
		utils.logPass("Export Location List option is clicked and success message is verified.");

		/* if (env.contains("local")) {
			// Verify email body and attachment
			Map<String, Object> emailData = GmailConnection.getGmailEmailBodyAndAttachments("subject",
					MessagesConstants.locationListExportEmailSubject);
			String body = (String) emailData.get("body");
			String downloadedFileName = ((List<String>) emailData.get("attachments")).get(0);
			String expectedEmailText = MessagesConstants.locationListExportEmailBody;
			Assert.assertTrue(body.contains(expectedEmailText));
			String outputFilePath = Paths.get(downloadedFilePath, downloadedFileName).toString();
			File outputFile = new File(outputFilePath);
			List<Map<String, String>> outputFileContent = ExcelUtils.readCSV(outputFile);
			utils.logit(downloadedFileName + " contents: \n" + outputFileContent);
			boolean isApprovedAndDisapprovedLocationPresent = pageObj.locationPage()
					.verifyApprovedAndDisapprovedLocations(outputFileContent, "Automation franchisee");
			Assert.assertTrue(isApprovedAndDisapprovedLocationPresent);
			utils.logPass(
					"Verified that Franchisee admin received the email having CSV of locations list containing associated Franchisee locations.");
		} */

		// Verify that expected Franchise is listed
		pageObj.menupage().navigateToSubMenuItem("Settings", "Admin Users");
		pageObj.dashboardpage().navigateToTabs("Franchises");
		utils.waitTillPagePaceDone();
		String franchiseName = pageObj.iframeConfigurationPage().getElementText("adminRolesPage.franchiseListedName",
				dataSet.get("franchiseId"));
		Assert.assertEquals(franchiseName, dataSet.get("franchiseName"));
		utils.logPass("Franchise " + franchiseName + " is listed under Franchises tab.");

		// Verify 'Export Franchise List' on Franchises page
		pageObj.locationPage().clickElipsisButton();
		pageObj.segmentsBetaPage()
				.verifyPresenceAndClick(utils.getLocatorValue("adminRolesPage.exportFranchiseListLink"));
		Assert.assertEquals(utils.getSuccessMessage(), MessagesConstants.franchiseListExportSuccessMessage);
		utils.logPass("Export Franchise List option is clicked and success message is verified.");

		// Verify that only the Franchisee associated Location Group is listed
		String franchiseLocationGroup = dataSet.get("franchiseLocationGroupName");
		pageObj.menupage().navigateToSubMenuItem("Settings", "Location Groups");
		String franchiseLocationGroupId = pageObj.locationPage().searchLocationGroupAndGetId(franchiseLocationGroup);
		Assert.assertEquals(franchiseLocationGroupId, dataSet.get("franchiseLocationGroupId"));
		pageObj.locationPage().searchLocationGroup(dataSet.get("nonFranchiseLocationGroupName"));
		boolean isNoResultsFoundMsgPresent = pageObj.gamesPage().isPresent(utils.getLocatorValue("locationPage.noResultsFoundMsg"));
		Assert.assertTrue(isNoResultsFoundMsgPresent);
		utils.logPass("Only Franchisee associated Location Group '" + franchiseLocationGroup + "' is present.");

		// Verify 'Export Location Group List' on Locations Groups page
		pageObj.locationPage().clickElipsisButton();
		pageObj.segmentsBetaPage()
				.verifyPresenceAndClick(utils.getLocatorValue("locationPage.exportLocationGroupListLink"));
		Assert.assertEquals(utils.getSuccessMessage(), MessagesConstants.locationGroupListExportSuccessMessage);
		utils.logPass("Export Location List option is clicked and success message is verified.");

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() throws Exception {
		utils.deleteLISQCRedeemable(env , lisExternalID, qcExternalID, redeemableExternalID);
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}

}
