package com.punchh.server.LP1Test;

import java.lang.reflect.Method;
import java.util.Map;
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
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.apiConfig.ApiResponseJsonSchema;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class EClubUploadTest {

	private static Logger logger = LogManager.getLogger(EClubUploadTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String userEmail;
	private String sTCName;
	private String env;
	private String baseUrl;
	private static Map<String, String> dataSet;
	String run = "ui";
	Utilities utils;

	@BeforeClass(alwaysRun = true)
	public void openBrowser() {
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
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==>" + dataSet);
		utils = new Utilities(driver);
		// Move to All Businesses page
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
	}

	@Test(description = "SQ-T2202 eClub Upload via Dashboard API", groups = { "regression", "dailyrun" } , priority = 0)
	@Owner(name = "Amit Kumar")
	public void T2202_eClubUploadViaDashboardApi() throws InterruptedException {
		// eclub upload via email
		Boolean flag = false;
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response eclubUploadResponse = pageObj.endpoints().dashboardApiEClubUpload(userEmail, dataSet.get("adminToken"),
				dataSet.get("storeNumber"), flag);
		pageObj.apiUtils().verifyResponse(eclubUploadResponse, "eclubUpload");
		// Login to instance
		//pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		//pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Guests", "eClub Guests");
		// verify eclub user created on eclub page
		pageObj.EClubGuestPage().verifyGuestuploadInEclub(dataSet.get("storeNumber"), userEmail);
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		// verify eclub user appear on timeline
		pageObj.guestTimelinePage().verifyEclubUser(userEmail);
		/*
		 * ==== eclub upload(email) when active registration & email subscription is set
		 * as yes ===
		 */
		String userEmail2 = pageObj.iframeSingUpPage().generateEmail();
		eclubUploadResponse = pageObj.endpoints().dashboardApiEClubUpload(userEmail2, dataSet.get("adminToken"),
				dataSet.get("storeNumber"), true);
		pageObj.apiUtils().verifyResponse(eclubUploadResponse, "eclubUpload");
		// pageObj.menupage().clickGuestMenu();
		// pageObj.menupage().eclubUsersLink();
		pageObj.menupage().navigateToSubMenuItem("Guests", "eClub Guests");
		// verify eclub user created on eclub page
		pageObj.EClubGuestPage().verifyGuestuploadInEclub(dataSet.get("storeNumber"), userEmail2);
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail2);
		// verify eclub user appear on timeline
		pageObj.guestTimelinePage().verifyEclubUser(userEmail2);

		/* ==== verify eclub guest upload via phone ==== */
		String phoneNumber = CreateDateTime.getTimeDateString();
		String userEmail3 = pageObj.iframeSingUpPage().generateEmail();
		Response eclubUploadViaPhoneResponse = pageObj.endpoints().dashboardApiEClubUploadWithPhone(phoneNumber,
				dataSet.get("adminToken"), dataSet.get("storeNumber"), userEmail3);
		pageObj.apiUtils().verifyResponse(eclubUploadViaPhoneResponse, "eclubUpload");
		// pageObj.menupage().clickGuestMenu();
		// pageObj.menupage().eclubUsersLink();
		pageObj.menupage().navigateToSubMenuItem("Guests", "eClub Guests");
		// verify eclub user created on eclub page
		pageObj.EClubGuestPage().verifyGuestuploadInEclub(dataSet.get("storeNumber"), phoneNumber);
		// td??
//		pageObj.instanceDashboardPage().navigateToGuestTimeline("");
		// verify eclub user appear on timeline
		// tddd pageObj.guestTimelinePage().verifyEclubUser("");
	}

	@Test(description = "SQ-T2651 Validate that the email id-cs@punchh.com gets replaced with the email support@punchh.com for error message getting displayed after disabling -'Enable transactions for users'", groups = { "regression", "dailyrun" } , priority = 1)
	@Owner(name = "Hardik Bhardwaj")
	public void T2651_EnableTransactionsForUsers() throws InterruptedException {

		// login to instance
		//pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		//pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "eClub Configuration");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_eclub_checkins", "uncheck");
		pageObj.dashboardpage().updateCheckBox();

		// mobile sign up
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		logger.info("mobile sign up is sucessful");
		utils.logPass("mobile sign up is sucessful");

		// ECRM checkin
		String key = CreateDateTime.getTimeDateString();
		String txn_no = "123456" + CreateDateTime.getTimeDateString();
		Response response = pageObj.endpoints().ecrmPosCheckin(userEmail, dataSet.get("locationkey"), key, txn_no);
		// pageObj.apiUtils().verifyResponse(response, "ECRM checkin");
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY, "ECRM checkin failure");
		boolean isCreateTransactionEclubCheckinsDisabledSchemaValidated = Utilities
				.validateJsonArrayAgainstSchema(ApiResponseJsonSchema.apiStringArraySchema, response.asString());
		Assert.assertTrue(isCreateTransactionEclubCheckinsDisabledSchemaValidated,
				"POS Create Transaction Schema Validation failed");
		Assert.assertEquals(response.jsonPath().getString("[0]"),
				"Sorry, this functionality is not available. Please contact support@punchh.com to enable this.");

		logger.info(" Email id cs@punchh.com for error message should get replaced with the email support@punchh.com");
		utils.logPass(
				" Email id cs@punchh.com for error message should get replaced with the email support@punchh.com");

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
