package com.punchh.server.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.annotations.Owner;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.apiConfig.ApiResponseJsonSchema;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.ExcelUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)

public class ExpiryStrategyTest {
	private static Logger logger = LogManager.getLogger(ExpiryStrategyTest.class);
	public WebDriver driver;
	// private Properties prop;
	private PageObj pageObj;
	private String sTCName, businessId, businessesQuery;
	private String userEmail;
	private String env;
	private String baseUrl;
	private static Map<String, String> dataSet;
	boolean flag1, flag2;
	String run = "ui";

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {

		// prop = Utilities.loadPropertiesFile("config.properties");
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
		businessId = dataSet.get("business_id");
		businessesQuery = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + businessId
				+ "'";
		logger.info(sTCName + " ==>" + dataSet);

	}

	// @Test(description = "LPE-T1034 Verify Inactivity expiry strategy API changes
	// expiry from Rolling expiry to Inactivity expiry strategy")
	public void LPE_T1034_ExpiryStrategy() throws InterruptedException {
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");
		pageObj.dashboardpage().navigateToTabs("Checkin Expiry");

		pageObj.cockpitearningPage().editExpiresAfterField("365");
		pageObj.cockpitearningPage().selectAccountReEvaluationStrategyVal("No Re-evaluation");
		pageObj.cockpitearningPage().clickUpdateBtn();

		Response status = pageObj.endpoints().rollingToInactivityExpiryAPI2(dataSet.get("apiKey"));
		Assert.assertEquals(status.getStatusCode(), 200);
		TestListeners.extentTest.get().pass("200 status");

		String apiMsg = status.jsonPath().get("message").toString();
		Assert.assertEquals(apiMsg, "API Restructure successfully initiated.", "Incorrect sg is displayed");
		TestListeners.extentTest.get().pass("correct API msg is displayed");

		pageObj.dashboardpage().navigateToTabs("Checkin Expiry");
		pageObj.cockpitearningPage().refreshPage();
		pageObj.dashboardpage().navigateToTabs("Checkin Expiry");

		String expiresAfterFieldVal = pageObj.cockpitearningPage().editExpiresAfterField("");
		Assert.assertEquals(expiresAfterFieldVal, "", "Expires after field contain some value after running the API");
		TestListeners.extentTest.get().pass("Expires after field does not contain value");

		Assert.assertEquals(pageObj.cockpitearningPage().getInactiveDaysField(), "180",
				"inactive days value is not updated");
		TestListeners.extentTest.get().pass("Inactive days value is updated");
	}

	// Anant
	@Test(description = "T4329 Verify the redeemable(with timezone X) end date in the rewards tab and user's timeline when redeemable has explicit end date in case of mass offer having business timezone Y and admin time zone same as redeemable timezone X")
	public void T4329_verifyRedeemableExpiryDate() throws InterruptedException, ParseException {
		String signUpCampaignName = CreateDateTime.getUniqueString("Automation SignUpCampaign");
		String redeeamable = CreateDateTime.getUniqueString("Automation redeeemable");

		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// create a redeemable
		pageObj.menupage().navigateToSubMenuItem("Settings", "Redeemables");
		// pageObj.dashboardpage().navigateToTabs("");

		pageObj.redeemablePage().enterRedeemableWithQCAndFlatDiscountWithEndDate(redeeamable, "Flat Discount", "",
				"2.0", dataSet.get("timeZone"));
		pageObj.signupcampaignpage().setEndDateforCouponCampaign(3);
		pageObj.redeemablePage().clickFinishBtn();

		String redeemableEndDate = pageObj.redeemablePage().getEndDateOfRedeemable(redeeamable);

		redeemableEndDate = redeemableEndDate + " HST";
		redeemableEndDate = pageObj.guestTimelinePage().convertTimeZone(redeemableEndDate, "");

		redeemableEndDate = redeemableEndDate.toLowerCase();
		// System.out.println(redeemableEndDate);

		// create Sign up campaign
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.campaignspage().selectOngoingdrpValue(dataSet.get("onGoingDrp"));
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().createWhatDetailsSignupCampaign(signUpCampaignName, dataSet.get("giftType"),
				dataSet.get("giftReason"), redeeamable);
		pageObj.signupcampaignpage().setPushNotification(dataSet.get("pushNotification"));
		pageObj.signupcampaignpage().clickNextButton();
		pageObj.signupcampaignpage().setEndDateforCouponCampaign(1);
		pageObj.signupcampaignpage().createWhenDetailsCampaign();

		// user signup using api2
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), 200);

		// nagivate to user Timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		String val = pageObj.guestTimelinePage().getRedeemableExpiryDateGuestTimeline(redeeamable);

		String expiryDateTimeline = val.substring(val.lastIndexOf(" and ") + 5);

		String actualExpiryDateTimeline = pageObj.guestTimelinePage().convertTimeZone(expiryDateTimeline, "");
		actualExpiryDateTimeline = actualExpiryDateTimeline.toLowerCase();
		Assert.assertTrue(actualExpiryDateTimeline.contains(redeemableEndDate),
				"after converting the timezone, date and time is not same in the guest timeline expected Date "
						+ actualExpiryDateTimeline + " but found " + redeemableEndDate);
		pageObj.utils().logPass("after converting the timezone, date and time is same in the guest timeline");

		pageObj.guestTimelinePage().clickRewards();
		String expiryDateRewards = pageObj.guestTimelinePage().getRedeemableExpiryFromRewards(redeeamable);

		String actualExpiryDateRewards = pageObj.guestTimelinePage().convertTimeZone(expiryDateRewards, "");
		actualExpiryDateRewards = actualExpiryDateRewards.toLowerCase();
		Assert.assertTrue(actualExpiryDateRewards.contains(redeemableEndDate),
				"after converting the timezone, date and time is not same in the rewards");
		pageObj.utils().logPass("after converting the timezone, date and time is same in the rewards");

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickNewCamHomePageBtn();
		pageObj.newCamHomePage().searchCampaign(signUpCampaignName);
		pageObj.newCamHomePage().deleteCampaign(signUpCampaignName);
//		pageObj.campaignspage().deleteCampaign(signUpCampaignName);
	}

	@Test(description = "LPE-T1548 Verify entry created in bulk_guest_activities table when BMU file upload via API"
			+ "LPE-T1547 Verify BMU user file uploaded via API is present on Guests->Awaiting Migration->Bulk BMU Upload"
			+ "LPE-T1546 / SQ-T4995 Verify Business Migration User can be uploaded to punchh dashboard via BMU API"
			+ "SQ-T7013 Verify BMU file extension other than .CSV not valid to upload via BMU API"
			+ "SQ-T7025 Verify entry created in bulk_guest_activities table when BMU file upload via API")
	public void T1548_VerifyBulkUploadOfMigratedUsers() throws Exception {

		// Path to the directory
		String directoryPath = System.getProperty("user.dir") + "/resources";
		File sourceFile = null;
		File destinationFile = null;
		// File destinationFileFolderPath = null;

		String reNamedCsvFilename = "BUM_UserList_" + CreateDateTime.getTimeDateString() + ".csv";

		List<String> fileNameList = ExcelUtils.getCsvFileNameFromDir(directoryPath + "/Testdata");
		for (String str : fileNameList) {
			if (str.startsWith("BUM_UserList_")) {
				sourceFile = new File(directoryPath + "/Testdata/" + str);
				destinationFile = new File(directoryPath + "/BMU_TestData/" + reNamedCsvFilename);

			}
		}

		Files.copy(sourceFile.toPath(), destinationFile.toPath());
		// String randomValue = CreateDateTime.getTimeDateString();
		String fileName = CreateDateTime.getUniqueString("BMU_UserList_");
		String columnName = "email"; // Replace with your column name
		List<String> newValue = new ArrayList<String>();
		for (int i = 0; i < 4; i++) {
			String userEmail = pageObj.iframeSingUpPage().generateEmail();
			newValue.add(userEmail.trim());

		}

		// Read CSV
		List<Map<String, String>> records = ExcelUtils.readCSV(destinationFile);

		// Update CSV
		ExcelUtils.updateColumnValues(records, columnName, newValue);

		// Write updated CSV
		ExcelUtils.writeCSV(destinationFile, records);

		Response bmuUploadResponse = pageObj.endpoints().bulkUploadOfBMUusers(fileName, destinationFile,
				dataSet.get("apiKey"));

		Assert.assertEquals(bmuUploadResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, " Bulk upload of users failed");
		pageObj.utils().logPass("Bulk upload of csv file is successfully");

		// Bulk Upload Business Migration User with invalid api key
		pageObj.utils().logit("== Bulk Upload Business Migration User with invalid api key ==");
		Response bmuUploadResponseInvalidApiKeyResponse = pageObj.endpoints().bulkUploadOfBMUusers(fileName,
				destinationFile, "1");
		Assert.assertEquals(bmuUploadResponseInvalidApiKeyResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED);
		boolean isBmuUploadInvalidApiKeySchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, bmuUploadResponseInvalidApiKeyResponse.asString());
		Assert.assertTrue(isBmuUploadInvalidApiKeySchemaValidated,
				"Bulk Upload Business Migration User with invalid api key Schema Validation failed");
		String removeUserInvalidSegmentNameMsg = bmuUploadResponseInvalidApiKeyResponse.jsonPath().get("error")
				.toString();
		Assert.assertEquals(removeUserInvalidSegmentNameMsg, "You need to sign in or sign up before continuing.");
		pageObj.utils().logPass("Bulk Upload Business Migration User with invalid api key is unsuccessful");

		// Bulk Upload Business Migration User with invalid file name
		pageObj.utils().logit("== Bulk Upload Business Migration User with invalid file name ==");
		Response bmuUploadResponseInvalidFileName = pageObj.endpoints().bulkUploadOfBMUusers(" ", destinationFile,
				dataSet.get("apiKey"));
		Assert.assertEquals(bmuUploadResponseInvalidFileName.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY);
		boolean isBmuUploadInvalidFileNameSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.dashboardInvalidBulkMigrationUploadNameSchema,
				bmuUploadResponseInvalidFileName.asString());
		Assert.assertTrue(isBmuUploadInvalidFileNameSchemaValidated,
				"Bulk Upload Business Migration User with invalid file name Schema Validation failed");
		String removeUserInvalidFileNameMsg = bmuUploadResponseInvalidFileName.jsonPath().get("errors.name[0]")
				.toString();
		Assert.assertEquals(removeUserInvalidFileNameMsg, "can't be blank");
		pageObj.utils().logPass("Bulk Upload Business Migration User with invalid file name is unsuccessful");

		File invalidFile = new File(System.getProperty("user.dir") + "/resources/Images/image_100kb.jpg");
		// Bulk Upload Business Migration User other than .CSV file
		Response bmuUploadResponseInvalidFileFormat = pageObj.endpoints().bulkUploadOfBMUusers(fileName, invalidFile,
				dataSet.get("apiKey"));
		Assert.assertEquals(bmuUploadResponseInvalidFileFormat.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY);
		String msg = bmuUploadResponseInvalidFileFormat.jsonPath().getString("errors.bulk_guest_activity_file[0]");
		Assert.assertEquals(msg, "is invalid",
				"Bulk Upload Business Migration User with invalid file format message did not matched");
		pageObj.utils().logPass("Bulk Upload Business Migration User with invalid file format is unsuccessful");

		// verify entry created in bulk_guest_activities table
		String query = "select COUNT(*) as count FROM `bulk_guest_activities` WHERE `name` = '" + fileName
				+ "' AND `business_id` = '" + dataSet.get("businessId") + "';";
		
		int expColValue = DBUtils.executeQueryAndGetCount(env, query);
		Assert.assertEquals(expColValue, 1, "Entry not created in bulk_guest_activities table for BMU file upload");
		pageObj.utils().logPass("Verified entry created in bulk_guest_activities table for BMU file upload via API");

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Guests", "Awaiting Migration");
		// pageObj.dashboardpage().navigateToTabsNew(" Bulk BMU Upload");

		String userListIsNotExist = pageObj.AdminUsersPage().verifyBMUListIsExist(newValue);
		Assert.assertTrue(userListIsNotExist.isEmpty(),
				" Migrated users are not uploaded/visible on UI , Please check the uploaded csv file and date format");
		pageObj.utils().logPass("Verified that migrated users are visible on UI");
	}

	// covered in T1553_VerifiedDeactivateUserBulkAPI
//	@Test(description = "LPE-T1552 Verify Bulk delete API")
	public void T1552_VerifiedBulkDeleteAPI() throws InterruptedException, IOException {
		List<String> userNameList = new ArrayList<String>();

		List<String> userIDList = new ArrayList<String>();

		// User SignUp using API
		String userEmail1 = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse1 = pageObj.endpoints().Api2SignUp(userEmail1, dataSet.get("client"),
				dataSet.get("secret"));

		String userID1 = signUpResponse1.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("API2 Signup is successful");

		userNameList.add(userEmail1);
		userIDList.add(userID1);
		// User SignUp using API
		String userEmail2 = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse2 = pageObj.endpoints().Api2SignUp(userEmail2, dataSet.get("client"),
				dataSet.get("secret"));

		String userID2 = signUpResponse2.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse2.getStatusCode(), 200, "Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("API2 Signup is successful");

		userNameList.add(userEmail2);
		userIDList.add(userID2);

		// User SignUp using API
		String userEmail3 = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse3 = pageObj.endpoints().Api2SignUp(userEmail3, dataSet.get("client"),
				dataSet.get("secret"));

		String userID3 = signUpResponse1.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("API2 Signup is successful");

		userNameList.add(userEmail3);
		userIDList.add(userID3);

		// Path to the directory
		String directoryPath = System.getProperty("user.dir") + "/resources";
		File sourceFile = null;
		File destinationFile = null;

		String reNamedCsvFilename = "BMU_DeleteUsers_" + CreateDateTime.getTimeDateString() + ".csv";

		List<String> fileNameList = ExcelUtils.getCsvFileNameFromDir(directoryPath + "/Testdata");
		for (String str : fileNameList) {
			if (str.startsWith("BMU_DeleteUsers_")) {
				sourceFile = new File(directoryPath + "/Testdata/" + str);
				destinationFile = new File(directoryPath + "/BMU_TestData/" + reNamedCsvFilename);
				pageObj.utils().logit("destination file Path = " + destinationFile);

			}
		}
		Files.copy(sourceFile.toPath(), destinationFile.toPath());

//		boolean isRenamed = sourceFile.renameTo(destinationFile);
//		Assert.assertTrue(isRenamed, " File is not renamed ");

		// File f1 = new File(destinationFile.toString());
		// boolean fileIsExist = f1.exists();
		pageObj.utils().logPass(fileNameList.get(0) + " file is renamed to " + reNamedCsvFilename);

		// String randomValue = CreateDateTime.getTimeDateString();
		String fileName = CreateDateTime.getUniqueString("BMU_DeleteUsers_");
		String columnName = "user_id"; // Replace with your column name

		// Read CSV
		List<Map<String, String>> records = ExcelUtils.readCSV(destinationFile);

		// Update CSV
		ExcelUtils.updateColumnValues(records, columnName, userIDList);

		// Write updated CSV
		ExcelUtils.writeCSV(destinationFile, records);

		Response bmuUploadResponse = pageObj.endpoints().bulkDeleteLoayltyUsers(fileName, destinationFile,
				dataSet.get("apiKey"));

		Assert.assertEquals(bmuUploadResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, " Bulk upload of users failed");
		pageObj.utils().logPass("Bulk upload of csv file is successfully");

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		boolean user1_isExist = pageObj.instanceDashboardPage().verifyDeletedGuests(userEmail1);
		Assert.assertFalse(user1_isExist, userEmail1 + " user is not deleted ");

		boolean user2_isExist = pageObj.instanceDashboardPage().verifyDeletedGuests(userEmail2);
		Assert.assertFalse(user2_isExist, userEmail2 + " user is not deleted ");

		boolean user3_isExist = pageObj.instanceDashboardPage().verifyDeletedGuests(userEmail3);
		Assert.assertFalse(user3_isExist, userEmail3 + " user is not deleted ");

	}

	@Test(description = "LPE-T1553  Verify Bulk deactivated API" + "LPE-T1552 Verify Bulk delete API")
	public void T1553_VerifiedDeactivateUserBulkAPI() throws InterruptedException, IOException {
		List<String> userNameList = new ArrayList<String>();

		List<String> userIDList = new ArrayList<String>();

		// User SignUp using API
		String userEmail1 = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse1 = pageObj.endpoints().Api2SignUp(userEmail1, dataSet.get("client"),
				dataSet.get("secret"));

		String userID1 = signUpResponse1.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("API2 Signup is successful");

		userNameList.add(userEmail1);
		userIDList.add(userID1);
		// User SignUp using API
		String userEmail2 = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse2 = pageObj.endpoints().Api2SignUp(userEmail2, dataSet.get("client"),
				dataSet.get("secret"));

		String userID2 = signUpResponse2.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse2.getStatusCode(), 200, "Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("API2 Signup is successful");

		userNameList.add(userEmail2);
		userIDList.add(userID2);

		// User SignUp using API
		String userEmail3 = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse3 = pageObj.endpoints().Api2SignUp(userEmail3, dataSet.get("client"),
				dataSet.get("secret"));

		String userID3 = signUpResponse3.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("API2 Signup is successful");

		userNameList.add(userEmail3);
		userIDList.add(userID3);

		// Path to the directory
		String directoryPath = System.getProperty("user.dir") + "/resources";
		File sourceFile = null;
		File destinationFile = null;

		String reNamedCsvFilename = "BMU_DeactivateUsers_" + CreateDateTime.getTimeDateString() + ".csv";

		List<String> fileNameList = ExcelUtils.getCsvFileNameFromDir(directoryPath + "/Testdata");
		for (String str : fileNameList) {
			if (str.startsWith("BMU_DeactivateUsers_")) {
				sourceFile = new File(directoryPath + "/Testdata/" + str);
				destinationFile = new File(directoryPath + "/BMU_TestData/" + reNamedCsvFilename);

			}
		}
		Files.copy(sourceFile.toPath(), destinationFile.toPath());

		pageObj.utils().logPass(fileNameList.get(0) + " file is renamed to " + reNamedCsvFilename);

		// String randomValue = CreateDateTime.getTimeDateString();
		String fileName = CreateDateTime.getUniqueString("BMU_DeactivateUsers_");
		String columnName = "user_id"; // Replace with your column name

		// Read CSV
		List<Map<String, String>> records = ExcelUtils.readCSV(destinationFile);

		// Update CSV
		ExcelUtils.updateColumnValues(records, columnName, userIDList);

		// Write updated CSV
		ExcelUtils.writeCSV(destinationFile, records);

		Response bmuUploadResponse = pageObj.endpoints().bulkDeactivateLoayltyUsers(fileName, destinationFile,
				dataSet.get("apiKey"));

		Assert.assertEquals(bmuUploadResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, " Bulk upload of users failed");
		pageObj.utils().logPass("Bulk upload of csv file is successfully");
//
//		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
//		pageObj.instanceDashboardPage().loginToInstance();
//		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
//
//		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail1);
//
//		
//		 pageObj.guestTimelinePage().waitTillDeactivateLabelIsVisibleOnUserTimelinePage();
//		String val1 = pageObj.guestTimelinePage().deactivationStatus();
//		Assert.assertEquals(val1, "Deactivated", "Deactivated lable text didnt matched on timeline");
//
//		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail1);
//
//		String val2 = pageObj.guestTimelinePage().deactivationStatus();
//		Assert.assertEquals(val2, "Deactivated", "Deactivated lable text didnt matched on timeline");
//
//		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail1);
//
//		String val3 = pageObj.guestTimelinePage().deactivationStatus();
//		Assert.assertEquals(val3, "Deactivated", "Deactivated lable text didnt matched on timeline");
//
//		Response bmuUploadResponseDelete = pageObj.endpoints().bulkDeleteLoayltyUsers(fileName, destinationFile,
//				dataSet.get("apiKey"));
//
//		Assert.assertEquals(bmuUploadResponseDelete.getStatusCode(), 200, " Bulk upload of users failed");
//		logger.info("Bulk upload of csv file is successfully");
//		TestListeners.extentTest.get().pass("Bulk upload of csv file is successfully");
//
//		pageObj.sidekiqPage().runSidekiqJobsBasedOnID(baseUrl, "UserIncinerateWorker", userIDList);
//
//		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
//		pageObj.instanceDashboardPage().loginToInstance();
//		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
//
//		boolean user1_isExist = pageObj.instanceDashboardPage().verifyDeletedGuests(userEmail1);
//		Assert.assertFalse(user1_isExist, userEmail1 + " user is not deleted ");
//
//		boolean user2_isExist = pageObj.instanceDashboardPage().verifyDeletedGuests(userEmail2);
//		Assert.assertFalse(user2_isExist, userEmail2 + " user is not deleted ");
//
//		boolean user3_isExist = pageObj.instanceDashboardPage().verifyDeletedGuests(userEmail3);
//		Assert.assertFalse(user3_isExist, userEmail3 + " user is not deleted ");

//		boolean user1_isExist = pageObj.instanceDashboardPage().verifyDeletedGuests(userEmail1);
//		Assert.assertFalse(user1_isExist, userEmail1 + " user is not deleted ");
//		
//		boolean user2_isExist = pageObj.instanceDashboardPage().verifyDeletedGuests(userEmail2);
//		Assert.assertFalse(user2_isExist, userEmail2 + " user is not deleted ");
//		
//		boolean user3_isExist = pageObj.instanceDashboardPage().verifyDeletedGuests(userEmail3);
//		Assert.assertFalse(user3_isExist, userEmail3 + " user is not deleted ");

	}

	@Test(description = "SQ-T6999 Verify BMU bulk upload via csv when optimise flag is disabled"
			+ "SQ-T7000 Verify BMU bulk upload via csv when optimise flag is enabled", dataProvider = "TestDataProvider1")
	@Owner(name = "Rakhi Rawat")
	public void T6999_VerifyBulkUploadOfMigratedUsersWithOptimiseFlag(String flagValue) throws Exception {

		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, businessesQuery, "preferences");

		// Set enable_bulk_bmu_upload to true
		DBUtils.updateBusinessFlag(env, expColValue, "true", dataSet.get("dbFlag1"),
				businessId);
		// Set optimise_s3_bmu_upload 
		DBUtils.updateBusinessFlag(env, expColValue, flagValue, dataSet.get("dbFlag2"),
				businessId);

		// Path to the directory
		String directoryPath = System.getProperty("user.dir") + "/resources";
		File sourceFile = null;
		File destinationFile = null;
		String reNamedCsvFilename = "BUM_UserList_" + CreateDateTime.getTimeDateString() + ".csv";

		List<String> fileNameList = ExcelUtils.getCsvFileNameFromDir(directoryPath + "/Testdata");
		for (String str : fileNameList) {
			if (str.startsWith("BUM_UserList_")) {
				sourceFile = new File(directoryPath + "/Testdata/" + str);
				destinationFile = new File(directoryPath + "/BMU_TestData/" + reNamedCsvFilename);

			}
		}
		Files.copy(sourceFile.toPath(), destinationFile.toPath());
		String columnName = "email";
		List<String> newValue = new ArrayList<String>();
		for (int i = 0; i < 4; i++) {
			String userEmail = pageObj.iframeSingUpPage().generateEmail();
			newValue.add(userEmail.trim());

		}
		// Read CSV
		List<Map<String, String>> records = ExcelUtils.readCSV(destinationFile);
		// Update CSV
		ExcelUtils.updateColumnValues(records, columnName, newValue);
		// Write updated CSV
		ExcelUtils.writeCSV(destinationFile, records);

		String updatedCsvPath = destinationFile.getAbsolutePath();
		String uploadName = "Bulk_BMU_Upload" + CreateDateTime.getTimeDateString();
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Guests", "Awaiting Migration");
		pageObj.dashboardpage().navigateToTabs("Bulk BMU Upload");
		
		// uploading csv
		pageObj.awaitingMigrationPage().uploadBMUFile(uploadName, "Browse", updatedCsvPath);
		String msg = pageObj.awaitingMigrationPage().validateBMUSuccessMessage();
		Assert.assertTrue(msg.contains("File has been successfully uploaded"),
				"Success message text did not matched");
		pageObj.utils().logPass("Bulk BMU upload success message text matched");

		// verifying uploaded users
		pageObj.menupage().navigateToSubMenuItem("Guests", "Awaiting Migration");
		String userListIsNotExist = pageObj.AdminUsersPage().verifyBMUListIsExist(newValue);
		Assert.assertTrue(userListIsNotExist.isEmpty(),
				" Migrated users are not uploaded/visible on UI , Please check the uploaded csv file and date format");
		pageObj.utils().logPass("Verified that migrated users are visible on UI");

	}
	@Test(description = "SQ-T7010 Verify BMU bulk upload via link when optimise flag is disabled"
			+ "SQ-T7012 Verify BMU bulk upload via link when optimise flag is enabled", dataProvider = "TestDataProvider1")
	@Owner(name = "Rakhi Rawat")
	public void T7010_VerifyBulkUploadOfMigratedUsersViaLink(String flagValue) throws Exception {

		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, businessesQuery, "preferences");

		// Set enable_bulk_bmu_upload to true
		DBUtils.updateBusinessFlag(env, expColValue, "true", dataSet.get("dbFlag1"),
				businessId);
		// Set optimise_s3_bmu_upload 
		DBUtils.updateBusinessFlag(env, expColValue, flagValue, dataSet.get("dbFlag2"),
				businessId);

        // Read existing CSV		
		File existingCsv = new File(System.getProperty("user.dir") + "/resources/Testdata/BMU_s3_UserList.csv");
		List<Map<String, String>> csvRecords = ExcelUtils.readCSV(existingCsv);
		
		List<String> newValue = new ArrayList<>();
		for (Map<String, String> record : csvRecords) {
		    String email = record.get("email"); 
		    if (email != null && !email.isEmpty()) {
		        newValue.add(email.trim());
		    }
		}
		pageObj.utils().logit("Extracted email values from existing CSV: " + newValue);
		
		// delete previously uploaded csv from db
		String deletionQuery = "DELETE FROM bulk_guest_activities " + "WHERE `name` LIKE '" + dataSet.get("bmuName")
				+ "%' AND `business_id` = '" + dataSet.get("business_id")+"';";
		DBUtils.executeQuery(env, deletionQuery);

		// remove uploaded migrated users from db
		for (String email : newValue) {
			String query = "DELETE FROM business_migration_users WHERE email = '" + email + "';";
			DBUtils.executeQuery(env, query);
		}
		
		String uploadName = "Bulk BMU Upload" + CreateDateTime.getTimeDateString();
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Guests", "Awaiting Migration");
		pageObj.dashboardpage().navigateToTabs("Bulk BMU Upload");
		
		// uploading csv
		pageObj.awaitingMigrationPage().uploadBMUFile(uploadName, "Filepath", dataSet.get("bmuFileLink"));
		String msg = pageObj.awaitingMigrationPage().validateBMUSuccessMessage();
		Assert.assertTrue(msg.contains("File has been successfully uploaded"),
				"Success message text did not matched");
		pageObj.utils().logPass("Bulk BMU upload success message text matched");

		// verifying uploaded users
		pageObj.menupage().navigateToSubMenuItem("Guests", "Awaiting Migration");
		String userListIsNotExist = pageObj.AdminUsersPage().verifyBMUListIsExist(newValue);
		Assert.assertTrue(userListIsNotExist.isEmpty(),
				" Migrated users are not uploaded/visible on UI , Please check the uploaded csv file and date format");
		pageObj.utils().logPass("Verified that migrated users are visible on UI");

		//delete uploaded csv from db
		pageObj.utils().deleteCsvFileFromBulkGuestActivitiesTable(uploadName,env);		
		
		//remove uploaded migrated users from db
		for (String email : newValue) {
			String query = "DELETE FROM business_migration_users WHERE email = '" + email + "';";
			DBUtils.executeQuery(env, query);
		}
	}

	@DataProvider(name = "TestDataProvider1")
	public Object[][] testDataProvider() {

		return new Object[][] {

				{ "false"}, {"true" } };
	}	

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		String bmuFolderPath = System.getProperty("user.dir") + "/resources/BMU_TestData";
		Utilities.clearFolder(bmuFolderPath, ".csv");

		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}
}
