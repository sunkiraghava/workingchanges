package com.punchh.server.LP1Test;

import org.testng.Assert;
import org.testng.annotations.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;

import io.restassured.response.Response;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.MessagesConstants;
import com.punchh.server.utilities.Utilities;
import com.punchh.server.utilities.TestListeners;

@Listeners(TestListeners.class)
public class DriveThruPayUsingSSFTest {
    private static final Logger logger = LogManager.getLogger(DriveThruPayUsingSSFTest.class);
    public WebDriver driver;
    private PageObj pageObj;
    private String sTCName;
    private String baseUrl;
    private String env;
    private String run = "ui";
    private static Map<String, String> dataSet;
    private Utilities utils;

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
		// Move to All businesses page
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
	}

	@Test(
		    description = "SQ-T7275 + SQ-T7276 + SQ-T7277 + SQ-T7284 : Drive Thru SSF end-to-end validation",
		    groups = "Regression",
		    enabled = true
		)
		public void SQ_T7275_verifyDriveThruSSF_EndToEnd() throws InterruptedException {

		    // ---------- NAVIGATE ----------
		    pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		    pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		    pageObj.dashboardpage().navigateToTabs("Miscellaneous Config");

		    // =====================================================
		    // STEP 1: RESET STATE
		    // =====================================================
		    pageObj.dashboardpage().checkUncheckAnyFlagWitoutUpdate(
		            "Enable Loyalty Identification at Drive-Thru", "uncheck");
		    pageObj.dashboardpage().checkUncheckAnyFlagWitoutUpdate(
		            "Enable Drive-Thru Pay using Short Code (using SSF)", "uncheck");
		    pageObj.dashboardpage().clickOnUpdateButton();
		    pageObj.dashboardpage().navigateToTabs("Miscellaneous Config");

		    // =====================================================
		    // STEP 2: SQ-T7284
		    // Enable SSF without prerequisite → Expect error
		    // =====================================================
		    pageObj.dashboardpage().checkUncheckAnyFlagWitoutUpdate(
		            "Enable Drive-Thru Pay using Short Code (using SSF)", "check");
		    pageObj.dashboardpage().clickOnUpdateButton();

		    String errorMessage =
		            pageObj.mobileconfigurationPage().getSuccessMessage();

		    Assert.assertEquals(
		            errorMessage,
		            MessagesConstants.driveThruPayErrorMessage,
		            "Error message is incorrect when prerequisite is missing"
		    );

		    pageObj.dashboardpage().navigateToTabs("Miscellaneous Config");

		    // =====================================================
		    // STEP 3: SQ-T7275
		    // Enable prerequisite + SSF successfully
		    // =====================================================
		    pageObj.dashboardpage().checkUncheckAnyFlagWitoutUpdate(
		            "Enable Loyalty Identification at Drive-Thru", "check");
		    pageObj.dashboardpage().checkUncheckAnyFlagWitoutUpdate(
		            "Enable Drive-Thru Pay using Short Code (using SSF)", "check");
		    pageObj.dashboardpage().clickOnUpdateButton();
		    pageObj.dashboardpage().navigateToTabs("Miscellaneous Config");

		    // =====================================================
		    // STEP 4: SQ-T7276
		    // Disable SSF → Dropdown should NOT show new options
		    // =====================================================
		    pageObj.dashboardpage().checkUncheckAnyFlagWitoutUpdate(
		            "Enable Drive-Thru Pay using Short Code (using SSF)", "uncheck");
		    pageObj.dashboardpage().clickOnUpdateButton();

		    pageObj.menupage().navigateToSubMenuItem("Cockpit", "POS Integration");

		    List<String> dropdownOptionsWhenDisabled =
		            utils.getAllVisibleTextFromDropdwon(
		                    utils.getLocator("PosIntegrationPage.singleScanTypeDropdown"));

		    Assert.assertEquals(
		            dropdownOptionsWhenDisabled,
		            dataSet.get("singlescandropdownlist"),
		            "SSF options should NOT be visible when SSF is disabled"
		    );

		    // =====================================================
		    // STEP 5: SQ-T7277
		    // Enable SSF → Dropdown should show new options
		    // =====================================================
		    pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		    pageObj.dashboardpage().navigateToTabs("Miscellaneous Config");

		    pageObj.dashboardpage().checkUncheckAnyFlagWitoutUpdate(
		            "Enable Drive-Thru Pay using Short Code (using SSF)", "check");
		    pageObj.dashboardpage().clickOnUpdateButton();

		    pageObj.menupage().navigateToSubMenuItem("Cockpit", "POS Integration");

		    // Selecting values itself validates presence
		    pageObj.mobileconfigurationPage().selectDropdownValue(
		            utils.getLocator("PosIntegrationPage.singleScanTypeDropdown"),
		            "Short Code Only");
		    pageObj.posIntegrationPage().clickUpdateBtn();

		    pageObj.mobileconfigurationPage().selectDropdownValue(
		            utils.getLocator("PosIntegrationPage.singleScanTypeDropdown"),
		            "Short Code and Single Scan Code Only");
		    pageObj.posIntegrationPage().clickUpdateBtn();

		    // =====================================================
		    // STEP 6: API VALIDATION
		    // =====================================================
		
		 // ---------- API 1 (Handles Array Response: [{...}]) ----------
		    Response cardsResponse = pageObj.endpoints().Api1Cards(dataSet.get("client"), dataSet.get("secret"));
		    Assert.assertEquals(cardsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match for API1 cards");
		    utils.logPass("API1 cards is successful with response code: " + cardsResponse.statusCode());

		    // Use Boolean object to avoid NullPointerException during unboxing
		    Boolean drive = cardsResponse.jsonPath().get("[0].enable_loyalty_identification_at_drive_thru");
		    Assert.assertNotNull(drive, "API1: enable_loyalty_identification_at_drive_thru is MISSING from response");
		    Assert.assertTrue(drive, "API1: enable_loyalty_identification_at_drive_thru should be TRUE");

		    Boolean driveThruPay = cardsResponse.jsonPath().get("[0].enable_drive_thru_pay");
		    Assert.assertNotNull(driveThruPay, "API1: enable_drive_thru_pay is MISSING from response");
		    Assert.assertTrue(driveThruPay, "API1: enable_drive_thru_pay should be TRUE");

		    String scanMode = cardsResponse.jsonPath().getString("[0].single_scan_type");
		    Assert.assertEquals(scanMode, "short_code_and_single_scan_code_only", "API1: Scan mode value mismatch");


		    // ---------- API 2 (Handles Object Response: {...}) ----------
		    utils.logit("Calling API2 for business meta data");
		    Response cardsResponse2 = pageObj.endpoints().Api2Cards(dataSet.get("client"), dataSet.get("secret"));
		    Assert.assertEquals(cardsResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match for API2 cards");
		    utils.logPass("API2 cards is successful with response code: " + cardsResponse2.statusCode());

		    // Note: No "[0]" prefix here because API2 returns a direct Object
		    Boolean drivethru2 = cardsResponse2.jsonPath().get("enable_loyalty_identification_at_drive_thru");
		    Assert.assertNotNull(drivethru2, "API2: enable_loyalty_identification_at_drive_thru is MISSING from response");
		    Assert.assertTrue(drivethru2, "API2: enable_loyalty_identification_at_drive_thru should be TRUE");

		    Boolean driveThruPay2 = cardsResponse2.jsonPath().get("enable_drive_thru_pay");
		    Assert.assertNotNull(driveThruPay2, "API2: enable_drive_thru_pay is MISSING from response");
		    Assert.assertTrue(driveThruPay2, "API2: enable_drive_thru_pay should be TRUE");

		    String scanMode2 = cardsResponse2.jsonPath().getString("single_scan_type");
		    Assert.assertEquals(scanMode2, "short_code_and_single_scan_code_only", "API2: Scan mode value mismatch");
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