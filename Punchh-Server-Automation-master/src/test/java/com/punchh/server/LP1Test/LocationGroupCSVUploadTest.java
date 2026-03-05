package com.punchh.server.LP1Test;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Paths;
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
import com.punchh.server.pages.LocationPage;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.ExcelUtils;
import com.punchh.server.utilities.GmailConnection;
import com.punchh.server.utilities.MessagesConstants;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class LocationGroupCSVUploadTest {

	private static Logger logger = LogManager.getLogger(LocationGroupCSVUploadTest.class);
	public WebDriver driver;
	private Properties prop;
	private PageObj pageObj;
	private String sTCName, env, baseUrl;
	String run = "ui";
	private static Map<String, String> dataSet;
	private Utilities utils;
	String downloadedFilePath;

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
		driver = new BrowserUtilities().launchBrowser();
		prop = Utilities.loadPropertiesFile("apiConfig.properties");
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
		downloadedFilePath = Paths.get(System.getProperty("user.dir"), "resources", "ExportData").toString();
	}

	@Test(description = "SQ-T6646: Verify Bulk Action button is added next to Add New Location Group button on Location Groups page; "
			+ "SQ-T6649: Verify admin with relevant permission is able to access Bulk Action button; "
			+ "SQ-T6647: Verify when CSV file is successfully uploaded, success message is displayed; "
			+ "SQ-T6651: Verify CSV file upload fails if CSV file is corrupted; "
			+ "SQ-T6648: Verify CSV file upload fails if CSV file contains more than 20 invalid location ids; "
			+ "SQ-T6665: Verify LG with relevant locations get created when CSV file contains valid location ids; "
			+ "SQ-T6664: Verify Failure Report is empty if all locations in CSV file are processed successfully; "
			+ "SQ-T6663: Verify Failure Report option in table listing can be dowloaded and shows the file containing invalid location ids that were not uploaded")
	@Owner(name = "Vaibhav Agnihotri")
	public void T6646_bulkActionOnLocationGroupPage() throws Exception {
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Settings", "Location Groups");

		// Verify Bulk Action button presence and click on it
		boolean isBulkActionButtonPresent = pageObj.segmentsBetaPage()
				.verifyPresenceAndClick(utils.getLocatorValue("locationPage.bulkActionButton"));
		Assert.assertTrue(isBulkActionButtonPresent, "Bulk Action button is not present or not clickable");
		// Verify the URL after clicking on Bulk Action button
		Assert.assertTrue(utils.verifyPartOfURL(dataSet.get("bulkActionPageURL")));
		TestListeners.extentTest.get().pass("Bulk Action Button is clicked and URL is verified");
		logger.info("Bulk Action Button is clicked and URL is verified");

		// Click on Import button and verify the URL
		pageObj.locationPage().clickImportButton();
		Assert.assertTrue(utils.verifyPartOfURL(dataSet.get("bulkImportPageURL")));
		TestListeners.extentTest.get().pass("Import button is clicked and URL is verified");
		logger.info("Import button is clicked and URL is verified");

		// Enter details and upload a valid CSV file. Verify success message
		String inputFilePath = Paths.get(System.getProperty("user.dir"), dataSet.get("validCSVPath")).toString();
		File inputFile = new File(inputFilePath);
		List<Map<String, String>> inputFileContent = ExcelUtils.readCSV(inputFile);
		String validLocationId1 = inputFileContent.get(0).get("location_id");
		String validLocationId2 = inputFileContent.get(1).get("location_id");
		String[] names1 = pageObj.locationPage().importLocationGroup(inputFilePath);
		String requestName1 = names1[0];
		String locationGroupName1 = names1[1];
		Assert.assertEquals(utils.getSuccessMessage(), MessagesConstants.locationGroupCreateRequestSubmitted);
		utils.refreshPage();
		String status1 = pageObj.locationPage().getCSVUploadsColumnValue(locationGroupName1, "1");
		Assert.assertEquals(status1, "Processed");
		TestListeners.extentTest.get()
				.pass("Location Group Creation Request " + requestName1 + " is submitted and processed.");
		logger.info("Location Group Creation Request " + requestName1 + " is submitted and processed.");

		// For 'Processed' upload, download the 'Failure Report' and verify its contents
		pageObj.guestTimelinePage().createAndCleanDownloadBrowserDownloadFolder(downloadedFilePath);
		String downloadedFileName1 = pageObj.locationPage().downloadCSVFile("Failure Report");
		File outputFile1 = new File(downloadedFilePath + "/" + downloadedFileName1);
		List<Map<String, String>> outputFileContent1 = ExcelUtils.readCSV(outputFile1);
		TestListeners.extentTest.get().info(downloadedFileName1 + " contents: \n" + outputFileContent1);
		logger.info(downloadedFileName1 + " contents: \n" + outputFileContent1);
		Assert.assertEquals(outputFileContent1.size(), 0);
		TestListeners.extentTest.get()
				.pass("Verified that Failure Report CSV file is empty for the 'Processed' upload.");
		logger.info("Verified that Failure Report CSV file is empty for the 'Processed' upload.");

		// Verify error on uploading CSV file with format issue
		pageObj.locationPage().clickImportButton();
		inputFilePath = Paths.get(System.getProperty("user.dir"), dataSet.get("formatIssueCSVPath")).toString();
		pageObj.locationPage().importLocationGroup(inputFilePath);
		Assert.assertEquals(utils.getSuccessMessage(), MessagesConstants.locationGroupCSVFormatIssue);
		TestListeners.extentTest.get()
				.pass("Location Group Creation Request is not submitted due to CSV format issue.");
		logger.info("Location Group Creation Request is not submitted due to CSV format issue.");

		// Verify error on uploading CSV file with 20+ invalid location_id
		inputFilePath = Paths.get(System.getProperty("user.dir"), dataSet.get("moreThan20LocCSVPath")).toString();
		String[] names2 = pageObj.locationPage().importLocationGroup(inputFilePath);
		String locationGroupName2 = names2[1];
		Assert.assertEquals(utils.getSuccessMessage(), MessagesConstants.locationGroupCreateRequestSubmitted);
		utils.refreshPage();
		String status2 = pageObj.locationPage().getCSVUploadsColumnValue(locationGroupName2, "1");
		Assert.assertEquals(status2, "Failed");
		String errorMsg2 = pageObj.locationPage().getCSVUploadsColumnValue(locationGroupName2, "2");
		Assert.assertEquals(errorMsg2, MessagesConstants.locationGroup20PlusInvalidLocationId);
		TestListeners.extentTest.get()
				.pass("Location Group Creation Request is failed due to more than 20 invalid location ids.");
		logger.info("Location Group Creation Request is failed due to more than 20 invalid location ids.");

		// For a 'Failed' upload, download the 'Failure Report' and verify its contents
		String downloadedFileName2 = pageObj.locationPage().downloadCSVFile("Failure Report");
		File outputFile2 = new File(downloadedFilePath + "/" + downloadedFileName2);
		List<Map<String, String>> outputFileContent2 = ExcelUtils.readCSV(outputFile2);
		TestListeners.extentTest.get().info(downloadedFileName2 + " contents: \n" + outputFileContent2);
		logger.info(downloadedFileName2 + " contents: \n" + outputFileContent2);
		Assert.assertTrue(outputFileContent2.get(0).containsKey("invalid_location_id"));
		Assert.assertTrue(outputFileContent2.size() > 20);
		TestListeners.extentTest.get().pass(
				"Verified that Failure Report CSV file contains more than 20 invalid location ids for the 'Failed' upload.");
		logger.info(
				"Verified that Failure Report CSV file contains more than 20 invalid location ids for the 'Failed' upload.");

		// Hit API to get the created Location Group details
		pageObj.menupage().navigateToSubMenuItem("Settings", "Location Groups");
		String locationGroupID = pageObj.locationPage().searchLocationGroupAndGetId(locationGroupName1);
		Response apiResponse = pageObj.endpoints().getLocationGroupDetails(locationGroupID, dataSet.get("apiKey"));
		Assert.assertEquals(apiResponse.getStatusCode(), 200);
		String apiLocationId1 = apiResponse.jsonPath().getString("[0].locations[0].location_id");
		Assert.assertEquals(apiLocationId1, validLocationId1);
		String apiLocationId2 = apiResponse.jsonPath().getString("[0].locations[1].location_id");
		Assert.assertEquals(apiLocationId2, validLocationId2);
		TestListeners.extentTest.get().pass("Verified that created Location Group contains the expected locations.");
		logger.info("Verified that created Location Group contains the expected locations.");
		deleteLocationGroup(locationGroupName1);
	}

	@Test(description = "SQ-T6670: Verify system proceeds with creation and sends response file to the logged in admin's email listing invalid entries if CSV has exact 20 invalid location Ids; "
			+ "SQ-T6671: Verify system proceeds with creation and sends response file to the logged in admin's email listing invalid entries if CSV has less than 20 invalid location Ids")
	@Owner(name = "Vaibhav Agnihotri")
	public void T6670_bulkActionAdminEmailVerification() throws Exception {
		/*
		 * Pre-requisites: 1) Cockpit > Dashboard > Misc Config: Stage=Live, Business
		 * Live Now?=ON. 2) AWS Secrets for Local Env contains: MANDRILL_USERNAME &
		 * MANDRILL_PASSWORD
		 */
		// Login with an admin having Business Manager permission
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance(dataSet.get("username"), prop.getProperty("password"));

		// Enter details and upload a CSV file with exact 20 invalid location IDs
		pageObj.menupage().navigateToSubMenuItem("Settings", "Location Groups");
		pageObj.locationPage().clickBulkActionButton();
		pageObj.locationPage().clickImportButton();
		String inputFilePath = Paths.get(System.getProperty("user.dir"), dataSet.get("exact20InvalidLocCSVPath"))
				.toString();
		String[] names1 = pageObj.locationPage().importLocationGroup(inputFilePath);
		String requestName1 = names1[0];
		String locationGroupName1 = names1[1];
		Assert.assertEquals(utils.getSuccessMessage(), MessagesConstants.locationGroupCreateRequestSubmitted);
		utils.refreshPage();
		String status1 = pageObj.locationPage().getCSVUploadsColumnValue(locationGroupName1, "1");
		Assert.assertEquals(status1, "Processed");
		TestListeners.extentTest.get()
				.pass("Location Group Creation Request " + requestName1 + " is submitted and processed.");
		logger.info("Location Group Creation Request " + requestName1 + " is submitted and processed.");

		if (env.contains("local")) {
			// Verify email received for the upload
			String emailBody1 =GmailConnection.getGmailEmailBody("subject",
					MessagesConstants.locationGroupSomeInvalidIDsEmailSubject,true);
			String expectedEmailText1 = MessagesConstants.locationGroupSomeInvalidIDsEmailBody.replace("temp",
					requestName1);
			Assert.assertTrue(emailBody1.contains(expectedEmailText1));
			TestListeners.extentTest.get()
					.pass("Verified the email received for uploading a CSV file with exact 20 invalid location IDs.");
			logger.info("Verified the email received for uploading a CSV file with exact 20 invalid location IDs.");
		}
		deleteLocationGroup(locationGroupName1);

		// Enter details and upload a CSV file with less than 20 invalid location IDs
		pageObj.locationPage().clickBulkActionButton();
		pageObj.locationPage().clickImportButton();
		inputFilePath = Paths.get(System.getProperty("user.dir"), dataSet.get("lessThan20InvalidLocCSVPath"))
				.toString();
		String[] names2 = pageObj.locationPage().importLocationGroup(inputFilePath);
		String requestName2 = names2[0];
		String locationGroupName2 = names2[1];
		Assert.assertEquals(utils.getSuccessMessage(), MessagesConstants.locationGroupCreateRequestSubmitted);
		utils.refreshPage();
		String status2 = pageObj.locationPage().getCSVUploadsColumnValue(locationGroupName2, "1");
		Assert.assertEquals(status2, "Processed");
		TestListeners.extentTest.get()
				.pass("Location Group Creation Request " + requestName2 + " is submitted and processed.");
		logger.info("Location Group Creation Request " + requestName2 + " is submitted and processed.");

		if (env.contains("local")) {
			// Verify email received for the upload
			String emailBody2 = GmailConnection.getGmailEmailBody("subject",
					MessagesConstants.locationGroupSomeInvalidIDsEmailSubject,true);
			String expectedEmailText2 = MessagesConstants.locationGroupSomeInvalidIDsEmailBody.replace("temp",
					requestName2);
			Assert.assertTrue(emailBody2.contains(expectedEmailText2));
			TestListeners.extentTest.get().pass(
					"Verified the email received for uploading a CSV file with less than 20 invalid location IDs.");
			logger.info("Verified the email received for uploading a CSV file with less than 20 invalid location IDs.");
		}
		deleteLocationGroup(locationGroupName2);
	}

	@Test(description = "SQ-T6717: Verify Bulk Action Create Location via CSV upload (E2E)")
	@Owner(name = "Vaibhav Agnihotri")
	public void T6717_bulkActionCreateLocation() throws Exception {
		String locationName = dataSet.get("locationName");
		String locationStoreNumber = dataSet.get("locationStoreNumber");
		String downloadedFileName = dataSet.get("locUploadSampleFileName");

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance(dataSet.get("username"), prop.getProperty("password"));
		pageObj.menupage().navigateToSubMenuItem("Settings", "Locations");

		// Click Bulk Action > Import. Verify page URL after each click
		pageObj.locationPage().clickBulkActionButton();
		Assert.assertTrue(utils.verifyPartOfURL(dataSet.get("bulkActionPageURL")));
		pageObj.locationPage().clickImportButton();
		Assert.assertTrue(utils.verifyPartOfURL(dataSet.get("bulkImportPageURL")));
		utils.logit("pass", "Bulk Action and Import buttons are clicked and URLs are verified.");

		// Click 'here' to download Sample location upload file
		pageObj.guestTimelinePage().createAndCleanDownloadBrowserDownloadFolder(downloadedFilePath);
		pageObj.locationPage().clickHereLink();
		File outputFile = new File(downloadedFilePath + "/" + downloadedFileName);
		List<Map<String, String>> outputFileContent = ExcelUtils.readCSV(outputFile);
		utils.logit(downloadedFileName + " contents: \n" + outputFileContent);
		Assert.assertTrue(outputFileContent.size() > 0);
		utils.logit("pass", "Sample location upload file is downloaded.");

		// Upload a CSV file having valid details
		String inputFilePath = Paths.get(System.getProperty("user.dir"), dataSet.get("validCSVPath")).toString();
		String requestName = pageObj.locationPage().importLocation(inputFilePath);
		Assert.assertEquals(utils.getSuccessMessage(), MessagesConstants.locationCreateRequestSubmitted);
		String status = pageObj.locationPage().getCSVUploadsColumnValue(requestName, "1");
		Assert.assertEquals(status, "Processed");
		utils.logit("Location Creation Request " + requestName + " is submitted.");

		// Hit API to get created location details
		pageObj.menupage().navigateToSubMenuItem("Settings", "Locations");
		pageObj.locationPage().clickOnLocationName(locationName);
		String currentUrl = utils.getCurrentURL();
		String locationID = LocationPage.extractUsingRegex(currentUrl, "/locations/(\\d+)");
		Response getApiResponse = pageObj.endpoints().getLocationDetails(locationID, locationStoreNumber,
				dataSet.get("apiKey"));
		Assert.assertEquals(getApiResponse.getStatusCode(), 200);
		String apiLocationName = getApiResponse.jsonPath().getString("[0].name");
		Assert.assertEquals(apiLocationName, locationName);
		utils.logit("pass", locationName + " with ID " + locationID + " has been created.");
		// Hit API to delete created location
		Response deleteApiResponse = pageObj.endpoints().deleteLocation(locationID, locationStoreNumber,
				dataSet.get("apiKey"));
		Assert.assertEquals(deleteApiResponse.getStatusCode(), 204);
		utils.logit(locationName + " with ID " + locationID + " is deleted.");
	}

	@Test(description = "SQ-T6718: Verify Bulk Action Create Location via CSV upload Error Scenarios")
	@Owner(name = "Vaibhav Agnihotri")
	public void T6718_bulkActionOnLocationsPageFailureCases() throws Exception {
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// 1) Hit Submit without providing any details
		pageObj.menupage().navigateToSubMenuItem("Settings", "Locations");
		pageObj.locationPage().clickBulkActionButton();
		pageObj.locationPage().clickImportButton();
		pageObj.subscriptionPlansPage().clickSubmitButton();
		String allBlankError = pageObj.iframeConfigurationPage().getElementText("locationPage.errorExplained", "");
		Assert.assertEquals(allBlankError, MessagesConstants.locationCreateAllBlankError);
		boolean isInlineErrorMsgVerified = pageObj.mobileconfigurationPage().validateInlineErrorMessage(
				"locationPage.locUploadInlineError", MessagesConstants.cantBeBlankError, dataSet.get("redColorHexCode"),
				2);
		Assert.assertTrue(isInlineErrorMsgVerified);
		utils.logit("pass", "1) Verified error messages on hitting Submit without providing any details.");

		// 2) Upload a CSV file having blank for location name
		String inputFilePath1 = Paths.get(System.getProperty("user.dir"), dataSet.get("blankNameCSVPath")).toString();
		String requestName1 = pageObj.locationPage().importLocation(inputFilePath1);
		Assert.assertEquals(utils.getSuccessMessage(), MessagesConstants.locationCreateRequestSubmitted);
		String status1 = pageObj.locationPage().getCSVUploadsColumnValue(requestName1, "1");
		Assert.assertEquals(status1, "Processed");
		utils.logit("Location Creation Request " + requestName1 + " is submitted.");
		// For above upload, download the 'Failure Report' and verify its contents
		pageObj.guestTimelinePage().createAndCleanDownloadBrowserDownloadFolder(downloadedFilePath);
		String downloadedFileName1 = pageObj.locationPage().downloadCSVFile("Failure Report");
		File outputFile1 = new File(downloadedFilePath + "/" + downloadedFileName1);
		List<Map<String, String>> outputFileContent1 = ExcelUtils.readCSV(outputFile1);
		utils.logit(downloadedFileName1 + " contents: \n" + outputFileContent1);
		String csvStatus1 = outputFileContent1.get(0).get("status");
		Assert.assertEquals(csvStatus1, "Failed");
		String csvError1 = outputFileContent1.get(0).get("error");
		Assert.assertTrue(csvError1.contains(MessagesConstants.locationCreateCSVBlankNameError));
		utils.logit("pass", "2) Failure Report CSV file contains failed location with reason mentioning blank name.");

		// 3) Upload a CSV file having rows with duplicate location names
		pageObj.locationPage().clickImportButton();
		String inputFilePath2 = Paths.get(System.getProperty("user.dir"), dataSet.get("duplicateNameCSVPath"))
				.toString();
		String requestName2 = pageObj.locationPage().importLocation(inputFilePath2);
		Assert.assertEquals(utils.getSuccessMessage(), MessagesConstants.locationCreateRequestSubmitted);
		String status2 = pageObj.locationPage().getCSVUploadsColumnValue(requestName2, "1");
		Assert.assertEquals(status2, "Processed");
		utils.logit("Location Creation Request " + requestName2 + " is submitted.");
		// For above upload, download the 'Failure Report' and verify its contents
		String downloadedFileName2 = pageObj.locationPage().downloadCSVFile("Failure Report");
		File outputFile2 = new File(downloadedFilePath + "/" + downloadedFileName2);
		List<Map<String, String>> outputFileContent2 = ExcelUtils.readCSV(outputFile2);
		utils.logit(downloadedFileName2 + " contents: \n" + outputFileContent2);
		String csvStatus2 = outputFileContent2.get(0).get("status");
		Assert.assertEquals(csvStatus2, "Failed");
		String csvError2 = outputFileContent2.get(0).get("error");
		Assert.assertTrue(csvError2.contains(MessagesConstants.locationCreateCSVDuplicateNameError));
		utils.logit("pass",
				"3) Failure Report CSV file contains failed location with reason mentioning duplicate location name.");

		// 4) Hit Submit while having a used upload request name and valid CSV file
		pageObj.locationPage().clickImportButton();
		String inputFilePath3 = Paths.get(System.getProperty("user.dir"), dataSet.get("validCSVPath")).toString();
		pageObj.locationPage().importLocation(inputFilePath3, dataSet.get("existingLocUploadRequestName"));
		String duplicateNameError = pageObj.iframeConfigurationPage().getElementText("locationPage.errorExplained", "");
		Assert.assertEquals(duplicateNameError, MessagesConstants.locationCreateDuplicateNameError);
		isInlineErrorMsgVerified = pageObj.mobileconfigurationPage().validateInlineErrorMessage(
				"locationPage.locUploadInlineError", MessagesConstants.alreadyTakenError,
				dataSet.get("redColorHexCode"), 1);
		Assert.assertTrue(isInlineErrorMsgVerified);
		utils.logit("pass", "4) Verified error messages on hitting Submit with already used bulk upload name.");

		// 5) Upload a CSV file having invalid location group
		String inputFilePath4 = Paths.get(System.getProperty("user.dir"), dataSet.get("invalidLocGroupCSVPath"))
				.toString();
		String requestName4 = pageObj.locationPage().importLocation(inputFilePath4);
		Assert.assertEquals(utils.getSuccessMessage(), MessagesConstants.locationCreateRequestSubmitted);
		String status4 = pageObj.locationPage().getCSVUploadsColumnValue(requestName4, "1");
		Assert.assertEquals(status4, "Processed");
		utils.logit("Location Creation Request " + requestName4 + " is submitted.");
		// For above upload, download the 'Failure Report' and verify its contents
		String downloadedFileName4 = pageObj.locationPage().downloadCSVFile("Failure Report");
		File outputFile4 = new File(downloadedFilePath + "/" + downloadedFileName4);
		List<Map<String, String>> outputFileContent4 = ExcelUtils.readCSV(outputFile4);
		utils.logit(downloadedFileName4 + " contents: \n" + outputFileContent4);
		String csvStatus4 = outputFileContent4.get(0).get("status");
		Assert.assertEquals(csvStatus4, "Failed");
		String csvError4 = outputFileContent4.get(0).get("error");
		Assert.assertTrue(csvError4.contains(MessagesConstants.locationCreateCSVInvalidLocGroupError));
		utils.logit("pass",
				"5) Failure Report CSV file contains failed location with reason mentioning invalid location group.");

		// 6) Upload a CSV file having order of columns changed
		pageObj.locationPage().clickImportButton();
		String inputFilePath5 = Paths.get(System.getProperty("user.dir"), dataSet.get("unorderedColumnsCSVPath"))
				.toString();
		String requestName5 = pageObj.locationPage().importLocation(inputFilePath5);
		Assert.assertEquals(utils.getSuccessMessage(), MessagesConstants.locationCreateRequestSubmitted);
		utils.logit("Location Creation Request " + requestName5 + " is submitted.");
		String status5 = pageObj.locationPage().getCSVUploadsColumnValue(requestName5, "1");
		Assert.assertEquals(status5, "Failed");
		String errorMsg5 = pageObj.locationPage().getCSVUploadsColumnValue(requestName5, "2");
		Assert.assertEquals(errorMsg5, MessagesConstants.locationCreateCSVUnorderedColumnsError);
		utils.logit("pass",
				"6) Location Creation Request " + requestName5 + " is failed due to unordered columns in CSV.");
	}

	// Deletes a created location group
	public void deleteLocationGroup(String locationGroupName) throws Exception {
		pageObj.menupage().navigateToSubMenuItem("Settings", "Location Groups");
		pageObj.locationPage().deleteLocationGroup(locationGroupName);
		TestListeners.extentTest.get().info(locationGroupName + " is deleted.");
		logger.info(locationGroupName + " is deleted.");
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		Utilities.clearFolder(downloadedFilePath, ".csv");
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}

}
