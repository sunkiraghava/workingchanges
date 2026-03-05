package com.punchh.server.Integration1;

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

import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.TestListeners;

import io.restassured.response.Response;

// @Author = Pradeep Kumar
@Listeners(TestListeners.class)
public class PercentageBasedTippingTest {
	static Logger logger = LogManager.getLogger(PercentageBasedTippingTest.class);
	public WebDriver driver;
	String userEmail;
	String email = "autoemailTemp@punchh.com";
	PageObj pageObj;
	String sTCName;
	private String env, run = "ui";
	private String baseUrl;
	private static Map<String, String> dataSet;

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
		// Instance move to all business page
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
	}

	// Case 1 : With tip_type:Percentage & tip
	@Test(description = "SQ-T6820 Verify POS User Lookup For tip_type:Percentage & tip")
	// @Owner(name = "Pradeep Kumar")
	public void T6820_verifyTipTypePercentageAndTipInPOSUserLookupApi() {
		// API2 User Sign-up
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), 200, "Status code 200 did not match for api2 user signup");
		TestListeners.extentTest.get().pass("API2 User Signup is successful");

		// API2 Generate Single Scan Code
		Response singleScanCodeResponse = pageObj.endpoints().api2SingleScanCodeForTipType(token,
				dataSet.get("payment_type"), dataSet.get("transaction_token"), dataSet.get("client"),
				dataSet.get("secret"), dataSet.get("tip_type"), dataSet.get("tip"));
		Assert.assertEquals(singleScanCodeResponse.getStatusCode(), 200,
				"Status code 200 did not match for API2 Generate Single Scan Code");
		TestListeners.extentTest.get().pass("API2 Generate Single Scan Code call is successful.");

		String singleScanCode = singleScanCodeResponse.jsonPath().getString("single_scan_code");
		logger.info("Mobile single scan code is " + singleScanCode);
		TestListeners.extentTest.get().pass("Mobile single scan code is " + singleScanCode);

		// POS User Lookup for tip_type:Percentage & tip
		Response userLookupResponse = pageObj.endpoints().posUserLookupSingleScanToken(singleScanCode,
				dataSet.get("location_key"));
		Assert.assertEquals(userLookupResponse.getStatusCode(), 200, "Error in getting user user lookup");
		TestListeners.extentTest.get().pass("POS User Lookup call is successful.");

		String selected_tip_amount = userLookupResponse.jsonPath().getString("selected_tip_amount");
		String selected_tip_type = userLookupResponse.jsonPath().getString("selected_tip_type");

		Assert.assertEquals(selected_tip_type, dataSet.get("tip_type"), "Error in getting user lookup");
		Assert.assertEquals(selected_tip_amount, dataSet.get("tip"), "Error in getting user lookup");
		logger.info("POS User Lookup For tip type & tip are validated successful for percentage." + selected_tip_type);
		TestListeners.extentTest.get().pass("POS User Lookup For tip type & tip are validated successful.");

	}

	// Case 2 : With tip_type:flat & tip
	@Test(description = "SQ-T6820 Verify POS User Lookup For tip_type:flat & tip")
	// @Owner(name = "Pradeep Kumar")
	public void T6820_verifyTipTypeFlatAndTipInPOSUserLookupApi() {
		// API2 User Sign-up
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), 200, "Status code 200 did not match for api2 user signup");
		TestListeners.extentTest.get().pass("API2 User Signup is successful");

		// API2 Generate Single Scan Code
		Response singleScanCodeResponse = pageObj.endpoints().api2SingleScanCodeForTipType(token,
				dataSet.get("payment_type"), dataSet.get("transaction_token"), dataSet.get("client"),
				dataSet.get("secret"), dataSet.get("tip_type"), dataSet.get("tip"));
		Assert.assertEquals(singleScanCodeResponse.getStatusCode(), 200,
				"Status code 200 did not match for API2 Generate Single Scan Code");
		TestListeners.extentTest.get().pass("API2 Generate Single Scan Code call is successful.");

		String singleScanCode = singleScanCodeResponse.jsonPath().getString("single_scan_code");
		logger.info("Mobile single scan code is " + singleScanCode);
		TestListeners.extentTest.get().pass("Mobile single scan code is " + singleScanCode);

		// POS User Lookup for tip_type:flat & tip
		Response userLookupResponse = pageObj.endpoints().posUserLookupSingleScanToken(singleScanCode,
				dataSet.get("location_key"));
		Assert.assertEquals(userLookupResponse.getStatusCode(), 200, "Error in getting user user lookup");
		TestListeners.extentTest.get().pass("POS User Lookup call is successful.");

		String selected_tip_amount = userLookupResponse.jsonPath().getString("selected_tip_amount");
		String selected_tip_type = userLookupResponse.jsonPath().getString("selected_tip_type");

		Assert.assertEquals(selected_tip_type, dataSet.get("tip_type"), "Error in getting user lookup");
		Assert.assertEquals(selected_tip_amount, dataSet.get("tip"), "Error in getting user lookup");
		TestListeners.extentTest.get().pass("POS User Lookup For tip type & tip are validated successful.");

	}

	// Case 3 : With tip
	@Test(description = "SQ-T6820 Verify POS User Lookup For tip")
	// @Owner(name = "Pradeep Kumar")
	public void T6820_verifyTipInPOSUserLookupApi() {
		// API2 User Sign-up
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), 200, "Status code 200 did not match for api2 user signup");
		TestListeners.extentTest.get().pass("API2 User Signup is successful");

		// API2 Generate Single Scan Code
		Response singleScanCodeResponse = pageObj.endpoints().api2SingleScanCodeForTipType(token,
				dataSet.get("payment_type"), dataSet.get("transaction_token"), dataSet.get("client"),
				dataSet.get("secret"), dataSet.get("tip_type"), dataSet.get("tip"));
		Assert.assertEquals(singleScanCodeResponse.getStatusCode(), 200,
				"Status code 200 did not match for API2 Generate Single Scan Code");
		TestListeners.extentTest.get().pass("API2 Generate Single Scan Code call is successful.");

		String singleScanCode = singleScanCodeResponse.jsonPath().getString("single_scan_code");
		logger.info("Mobile single scan code is " + singleScanCode);
		TestListeners.extentTest.get().pass("Mobile single scan code is " + singleScanCode);

		// POS User Lookup for tip (backward compatibility)
		Response userLookupResponse = pageObj.endpoints().posUserLookupSingleScanToken(singleScanCode,
				dataSet.get("location_key"));
		Assert.assertEquals(userLookupResponse.getStatusCode(), 200, "Error in getting user user lookup");
		TestListeners.extentTest.get().pass("POS User Lookup call is successful.");

		String selected_tip_amount = userLookupResponse.jsonPath().getString("selected_tip_amount");
		// String selected_tip_type =
		// userLookupResponse.jsonPath().getString("selected_tip_type");

		// Assert.assertEquals(selected_tip_type, dataSet.get("tip_type"), "Error in
		// getting user lookup");
		Assert.assertEquals(selected_tip_amount, dataSet.get("tip"), "Error in getting user lookup");
		TestListeners.extentTest.get().pass("POS User Lookup For tip type & tip are validated successful.");

	}

	// Author=Pradeep
	@Test(description = "SQ-T6819_newAddedSingleScanFlowTab Validate newly added Single scan flow tab")
	// @Owner(name = "Pradeep Kumar")
	public void T6819_newAddedSingleScanFlowTab() throws InterruptedException {
		// Select test Business
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Click on Enable SSF Tip Type Support? tab
		pageObj.dashboardpage().checkUncheckFlagCockpitDashboard("Enable SSF Tip Type Support?", "check");

		// go to whitelabel->service->menu
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "Mobile Configuration");

		// Click on Single scan flow
		pageObj.dashboardpage().navigateToTabs("Single Scan Flow");

		// Fill Flat Tip Amount
		pageObj.mobileconfigurationPage().clickOnEnableFlatAmountTippingAndFillTipAmount(
				dataSet.get("amount_tip_option_1"), dataSet.get("amount_tip_option_2"),
				dataSet.get("amount_tip_option_3"));

		// Fill Percentage Tip Amount
		pageObj.mobileconfigurationPage().ClickOnEnablePercentageAndFillTipAmount(
				dataSet.get("percentage_tip_option_1"), dataSet.get("percentage_tip_option_2"),
				dataSet.get("percentage_tip_option_3"));

		pageObj.mobileconfigurationPage().clickOnUpdateBtn();

		// Success message validation
		String successMsg = pageObj.mobileconfigurationPage().getSuccesMessage();
		Assert.assertEquals(successMsg, "Mobile configuration updated for Single Scan Flow");
		logger.info("Success msg is displayed after clicking update button");
		TestListeners.extentTest.get().pass("Success msg is displayed after clicking update button");

		// Validate Tip type by cards api
		Response cardsResponse = pageObj.endpoints().Api1Cards(dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(cardsResponse.getStatusCode(), 200, "Status code 200 did not matched for api1 cards");

		String flatTipType = cardsResponse.jsonPath().get("[0].ssf_tipping_configuration.tipping_options[0].tip_type")
				.toString();
		Assert.assertEquals(flatTipType, dataSet.get("flat_tip_type"), "Flat Tip Type didnot match in cards api");

		String percentageTipType = cardsResponse.jsonPath()
				.get("[0].ssf_tipping_configuration.tipping_options[1].tip_type").toString();
		Assert.assertEquals(percentageTipType, dataSet.get("percentage_tip_type"),
				"Percentage Tip Type didnot match  in cards api");

		TestListeners.extentTest.get().pass("Api V1 cards is successful with response :" + cardsResponse.asString());

	}

	@Test(description = "SQ-T6821_Verify Dahboard Meta Endpoint Old Flow")
	// @Owner(name = "Pradeep Kumar")
	public void T6821_verifyDashboardAPIOldMeta() throws InterruptedException {

		// Select test business
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Click Cockpit set Enable Extended Loyalty Configs in Platform Meta API : OFF
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().navigateToTabs("Miscellaneous Config");
		pageObj.dashboardpage().checkUncheckFlagCockpitDashboard("Enable Extended Loyalty Configs in Platform Meta API",
				"uncheck");

		Response dashboardMetaResponse = pageObj.endpoints().dashboardAPI2Meta(dataSet.get("adminAuthorization"));
		Assert.assertEquals(dashboardMetaResponse.statusCode(), 200,
				"Status code 200 did not match for Platform Functions API Dashboard Meta");
		TestListeners.extentTest.get().pass("Platform Functions API Dashboard Meta call is successful");
		logger.info("Platform Functions API Dashboard Meta call is successful");

		// redeemable_id validation
		String redeemablesId = dashboardMetaResponse.jsonPath().getString("redeemables[0].redeemable_id").toString();
		Assert.assertEquals(redeemablesId, dataSet.get("redeemable_id"), "redeemables Id did not match");

		// redeemable Name validation
		String redeemablesName = dashboardMetaResponse.jsonPath().getString("redeemables[0].name").toString();
		Assert.assertEquals(redeemablesName, dataSet.get("name"), "redeemables Name did not match");

		TestListeners.extentTest.get().pass("Platform Functions API Dashboard Meta response is validated successful");
		logger.info("Platform Functions API Dashboard Meta response is validated successful");

	}

	@Test(description = "SQ-T6821_Verify Dahboard Meta Endpoint New Flow")
	// @Owner(name = "Pradeep Kumar")
	public void T6821_verifyDashboardAPIMetaNewFlow() throws InterruptedException {

		// Select Test Business
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Click Cockpit set Enable Extended Loyalty Configs in Platform Meta API : ON
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().navigateToTabs("Miscellaneous Config");
		pageObj.dashboardpage().checkUncheckFlagCockpitDashboard("Enable Extended Loyalty Configs in Platform Meta API",
				"check");

		Response dashboardMetaResponse = pageObj.endpoints().dashboardAPI2Meta(dataSet.get("adminAuthorization"));
		Assert.assertEquals(dashboardMetaResponse.statusCode(), 200,
				"Status code 200 did not match for Platform Functions API Dashboard Meta");
		TestListeners.extentTest.get().pass("Platform Functions API Dashboard Meta call is successful");
		logger.info("Platform Functions API Dashboard Meta call is successful");

		// program_type validation
		String programType = dashboardMetaResponse.jsonPath().get("program_type").toString();
		Assert.assertEquals(programType, dataSet.get("program_type"), "program type did not match");

		// points_conversion_type validation
		String pointsCconversionType = dashboardMetaResponse.jsonPath().get("points_conversion_type").toString();
		Assert.assertEquals(pointsCconversionType, dataSet.get("points_conversion_type"),
				"points conversion type did not match");

		// guest_identity_code_type validation
		String guestIidentityCodeType = dashboardMetaResponse.jsonPath().get("guest_identity_code_type").toString();
		Assert.assertEquals(guestIidentityCodeType, dataSet.get("guest_identity_code_type"),
				"guest identity code type did not match");

		// points_conversion_threshold validation
		String pointsConversionThreshold = dashboardMetaResponse.jsonPath().get("points_conversion_threshold")
				.toString();
		Assert.assertEquals(pointsConversionThreshold, dataSet.get("points_conversion_threshold"),
				"points conversion threshold did not match");

		// guest_lookup_type validation
		String guestLookupType = dashboardMetaResponse.jsonPath().get("guest_lookup_type").toString();
		Assert.assertEquals(guestLookupType, dataSet.get("guest_lookup_type"), "guest lookup type did not match");

		// redeemable_id validation
		String redeemableId = dashboardMetaResponse.jsonPath().get("redeemables[0].redeemable_id").toString();
		Assert.assertEquals(redeemableId, dataSet.get("redeemable_id"), "redeemable id did not match");

		// membership_levels validation
		String membershipLevels = dashboardMetaResponse.jsonPath().getString("membership_levels[0].name").toString();
		Assert.assertEquals(membershipLevels, dataSet.get("name"), "membership levels did not match");

		TestListeners.extentTest.get()
				.pass("Platform Functions API Dashboard Meta call is validated successful successful");
		logger.info("Platform Functions API Dashboard Meta call is successful");

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() throws Exception {
		pageObj.utils().clearDataSet(dataSet);
		logger.info("Data set cleared");
	}

	@AfterClass(alwaysRun = true)
	public void closeBrowser() {
		driver.quit();
		logger.info("Browser closed");
	}

}