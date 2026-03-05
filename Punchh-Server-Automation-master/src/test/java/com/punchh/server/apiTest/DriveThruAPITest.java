package com.punchh.server.apiTest;

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

import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.ApiUtils;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;


@Listeners(TestListeners.class)
public class DriveThruAPITest {

	private static Logger logger = LogManager.getLogger(DriveThruAPITest.class);
	public WebDriver driver;
	private PageObj pageObj;
	Utilities utils;
	private String sTCName, businessesQuery, businessId;
	private String userEmail;
	private String env, run = "ui";
	private String baseUrl;
	private Properties prop;
	private ApiUtils apiUtils;
	private static Map<String, String> dataSet;
	
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
		utils = new Utilities(driver);
		sTCName = method.getName();
		apiUtils = new ApiUtils();
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==>" + dataSet);
	}	
	
	// akansha jain
	@Test(description = "SQ-T6941 Verify that the API returns a valid short code when using location-level strategy", groups = "api", priority = 8)
	public void SQ_T6941_GenerateShortCodeAPIForDriveThru() throws InterruptedException {
		// select business
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		
		// Need :enable drive thru feature from cockpit >> dashboard >> miscellaneous configuration
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().navigateToTabsNew("Miscellaneous Config");
		pageObj.dashboardpage().checkUncheckAnyFlag("Enable Loyalty Identification at Drive-Thru", "check");
		
		// Need :select drive thru short code length strategy from cockpit >> POS integration >> Drive-Thru tab
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "POS Integration");
		utils.longWaitInSeconds(5);
		pageObj.dashboardpage().navigateToTabsNew("Drive-Thru");
		pageObj.posIntegrationPage().selectDriveThruDrpDownValue(dataSet.get("driveThruCodeLengthStrategy"));
		pageObj.posIntegrationPage().clickUpdateBtn();
		
		// User1 register/sign up using API2 Sign up
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse1 = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"), dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse1, "API 2 user signup");
		String token1 = signUpResponse1.jsonPath().get("access_token.token").toString();
		Assert.assertEquals(signUpResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("Api2 user signup is successful");
				
		// api/mobile/drivethru_code with valid loc id
		Response v1ApiResponse = pageObj.endpoints().Api1DriveThruShortCode(dataSet.get("validLocID"), dataSet.get("client"),
				dataSet.get("secret"), token1, "body");
		Assert.assertEquals(v1ApiResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api drive thru code generation with valid loc id");
		TestListeners.extentTest.get()
				.pass("Status code 200 did not matched for api drive thru code generation with valid loc id");
		String v1APIShortCode = v1ApiResponse.jsonPath().get("short_code").toString();
		Assert.assertEquals(v1ApiResponse.jsonPath().get("short_code"), v1APIShortCode, "Drive thru code not generated");
		utils.logPass("Drive thru code generated successfully");
		
		// User2 register/sign up using API2 Sign up
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse2 = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse2, "API 2 user signup");
		String token2 = signUpResponse2.jsonPath().get("access_token.token").toString();
		Assert.assertEquals(signUpResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("Api2 user signup is successful");
		
		// api2/mobile/drivethru_code with valid loc id
		Response v2ApiResponse = pageObj.endpoints().Api2DriveThruShortCode(dataSet.get("validLocID"), dataSet.get("client"),
				dataSet.get("secret"), token2);
		Assert.assertEquals(v2ApiResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api drive thru code generation with valid loc id");
		TestListeners.extentTest.get()
				.pass("Status code 200 did not matched for api drive thru code generation with valid loc id");
		String v2APIShortCode = v2ApiResponse.jsonPath().get("short_code").toString();
		Assert.assertEquals(v2ApiResponse.jsonPath().get("short_code"), v2APIShortCode, "Drive thru code not generated");
		logger.info("Drive thru code generated successfully");
		TestListeners.extentTest.get().pass("Drive thru code generated successfully");
	}
	
	// akansha jain
	@Test(description = "SQ-T6942 Verify that the API returns an error when location_id is missing for location-level strategy", groups = "api", priority = 8)
	public void SQ_T6942_GenerateShortCodeWithoutLocation() throws InterruptedException {
		// select business
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
				
		// Need :enable drive thru feature from cockpit >> dashboard >> miscellaneous configuration
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().navigateToTabsNew("Miscellaneous Config");
		pageObj.dashboardpage().checkUncheckAnyFlag("Enable Loyalty Identification at Drive-Thru", "check");
		
		// Need :select drive thru short code length strategy from cockpit >> POS integration >> Drive-Thru tab
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "POS Integration");
		utils.longWaitInSeconds(5);
		pageObj.dashboardpage().navigateToTabsNew("Drive-Thru");
		pageObj.posIntegrationPage().selectDriveThruDrpDownValue(dataSet.get("driveThruCodeLengthStrategy"));
		pageObj.posIntegrationPage().clickUpdateBtn();
			
		// User register/sign up using API2 Sign up
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("Api2 user signup is successful");
					
		// api/mobile/drivethru_code with valid loc id
		Response v1ApiResponse = pageObj.endpoints().Api1DriveThruShortCode(dataSet.get("blankLocID"), dataSet.get("client"),
				dataSet.get("secret"), token, "body");
		Assert.assertEquals(v1ApiResponse.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Status code 400 did not matched for api drive thru code generation with blank loc id");
		TestListeners.extentTest.get()
				.pass("Status code 400 did not matched for api drive thru code generation with blank loc id");
		String v1APIShortCodeErrorMessage = v1ApiResponse.jsonPath().get("error").toString();
		Assert.assertEquals(v1ApiResponse.jsonPath().get("error"), v1APIShortCodeErrorMessage, "Location id is blank");
		logger.info("Location Id is missing");
		TestListeners.extentTest.get().pass("Location Id is missing");
			
		// api2/mobile/drivethru_code with valid loc id
		Response v2ApiResponse = pageObj.endpoints().Api2DriveThruShortCode(dataSet.get("blankLocID"), dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(v2ApiResponse.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Status code 400 did not matched for api drive thru code generation with blank loc id");
		TestListeners.extentTest.get()
				.pass("Status code 400 did not matched for api drive thru code generation with blank loc id");
		String v2APIShortCodeErrorMessage = v2ApiResponse.jsonPath().get("errors.location_id").toString();
		Assert.assertEquals(v2ApiResponse.jsonPath().get("errors.location_id"), v2APIShortCodeErrorMessage, "Location id is blank");
		logger.info("Location Id is missing");
		TestListeners.extentTest.get().pass("Location Id is missing");
	}

	// akansha jain
	@Test(description = "SQ-T6940 Verify that the API returns an error when short code length strategy is not selected", groups = "api", priority = 8)
	public void SQ_T6940_GenerateShortCodeWithoutStrategySelected() throws InterruptedException {
		// select business
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
				
		// Need :enable drive thru feature from cockpit >> dashboard >> miscellaneous configuration
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().navigateToTabsNew("Miscellaneous Config");
		pageObj.dashboardpage().checkUncheckAnyFlag("Enable Loyalty Identification at Drive-Thru", "check");
		
		// Need :select drive thru short code strategy and length from cockpit >> POS integration >> Drive-Thru tab
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "POS Integration");
		utils.longWaitInSeconds(5);
		pageObj.dashboardpage().navigateToTabsNew("Drive-Thru");
		pageObj.posIntegrationPage().deselectDriveThruDrpDownValue();
		pageObj.posIntegrationPage().clickDriveThroughUpdateBtn();
		pageObj.dashboardpage().navigateToTabsNew("Drive-Thru");
				
		// User register/sign up using API2 Sign up
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");			
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("Api2 user signup is successful");
							
		// api/mobile/drivethru_code without strategy selected			
		Response v1ApiResponse = pageObj.endpoints().Api1DriveThruShortCode(dataSet.get("validLocID"), dataSet.get("client"),
				dataSet.get("secret"), token, "body");
		Assert.assertEquals(v1ApiResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for api drive thru code strategy is not selected");
		TestListeners.extentTest.get()
				.pass("Status code 422 did not matched for api drive thru code strategy is not selected");
		String v1APIErrorMessage = v1ApiResponse.jsonPath().get("errors").toString();
		Assert.assertEquals(v1ApiResponse.jsonPath().get("errors"), v1APIErrorMessage, "Drive thru code strategy not selected");
		utils.logPass("Drive-Thru Code Length Strategy is not selected");
					
		// api2/mobile/drivethru_code without strategy selected
		Response v2ApiResponse = pageObj.endpoints().Api2DriveThruShortCode(dataSet.get("validLocID"), dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(v2ApiResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for api drive thru code strategy is not selected");
		TestListeners.extentTest.get()
				.pass("Status code 422 did not matched for api drive thru code strategy is not selected");
		String v2APIErrorMessage = v2ApiResponse.jsonPath().get("errors").toString();
		Assert.assertEquals(v2ApiResponse.jsonPath().get("errors"), v2APIErrorMessage, "Drive thru code strategy not selected");
		utils.logPass("Drive-Thru Code Length Strategy is not selected");
	}
				
	// akansha jain
	@Test(description = "SQ-T6943 Verify that the API returns an error when drive through feature flag is disabled ", groups = "api", priority = 8)
	public void SQ_T6943_GenerateShortCodeWithoutFeatureEnabled() throws InterruptedException {
		// select business
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
				
		// Need :disable drive thru feature from cockpit >> dashboard >> miscellaneous configuration
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().navigateToTabsNew("Miscellaneous Config");
		pageObj.dashboardpage().checkUncheckAnyFlag("Enable Loyalty Identification at Drive-Thru", "uncheck");
		
		// User register/sign up using API2 Sign up
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("Api2 user signup is successful");
										
		// api/mobile/drivethru_code without feature enabled
		Response v1ApiResponse = pageObj.endpoints().Api1DriveThruShortCode(dataSet.get("validLocID"), dataSet.get("client"),
				dataSet.get("secret"), token, "body");
		Assert.assertEquals(v1ApiResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for api drive thru code feature not enabled");
		TestListeners.extentTest.get()
				.pass("Status code 422 did not matched for api drive thru code feature not enabled");
		String v1APIErrorMessage = v1ApiResponse.jsonPath().get("errors").toString();
		Assert.assertEquals(v1ApiResponse.jsonPath().get("errors"), v1APIErrorMessage, "Drive thru feature not enabled");
		utils.logPass("Drive-Thru feature is disabled");
								
		// api2/mobile/drivethru_code without feature enabled
		Response v2ApiResponse = pageObj.endpoints().Api2DriveThruShortCode(dataSet.get("validLocId"), dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(v2ApiResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for api drive thru code feature not enabled");
		TestListeners.extentTest.get()
				.pass("Status code 422 did not matched for api drive thru code feature not enabled");
		String v2APIErrorMessage = v2ApiResponse.jsonPath().get("errors").toString();
		Assert.assertEquals(v2ApiResponse.jsonPath().get("errors"), v2APIErrorMessage, "Drive thru feature not enabled");
		utils.logPass("Drive-Thru feature is disabled");
	}
	
	@AfterMethod(alwaysRun = true)
	public void afterMethod() {
		pageObj.utils().clearDataSet(dataSet);
	}
	
	@AfterClass(alwaysRun = true)
	public void closeBrowser() {
		driver.quit();
		logger.info("Browser closed");
	}
}
