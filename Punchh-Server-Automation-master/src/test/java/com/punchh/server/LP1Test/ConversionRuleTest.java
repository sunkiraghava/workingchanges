package com.punchh.server.LP1Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
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
public class ConversionRuleTest {

	private static Logger logger = LogManager.getLogger(ConversionRuleTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String userEmail;
	private String sTCName;
	private String env;
	private String baseUrl;
	private Utilities utils;
	String b_id;
	private static Map<String, String> dataSet;
	String run = "ui";

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
		logger.info(sTCName + " ==> " + dataSet);
		utils = new Utilities(driver);
		// Move to All Businesses page
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
	}

	@Test(description = "SQ-T4346 [Points to Manual] Verify \"Show balance in POS Api\" flag can be disabled from UI"
			+ "SQ-T4347 [Points to Manual] Verify \"Show balance in POS Api\" flag can be enabled from UI"
			+ "SQ-T4344 [Points to Manual]Verify v2 user profile api optimisation ", groups = { "regression",
					"dailyrun" }, priority = 0)
	@Owner(name = "Vansham Mishra")
	public void T4346_showBalanceCanDisabled() throws Exception {
		// Login to instance
		//pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		//pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// user signup using api2
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), 200);
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		//
		Response fetchUserResponse = pageObj.endpoints().Api2FetchUserInfo(token, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(fetchUserResponse.statusCode(), 200);

		pageObj.menupage().navigateToSubMenuItem("Settings", "Conversion Rules");

		// queries
		String query1 = "Select * from conversion_rules where business_id=" + dataSet.get("business_id") + " and name='"
				+ dataSet.get("rule1") + "'";
		String query2 = "Select * from conversion_rules where business_id=" + dataSet.get("business_id") + " and name='"
				+ dataSet.get("rule2") + "'";
		String query3 = "Select * from conversion_rules where business_id=" + dataSet.get("business_id") + " and name='"
				+ dataSet.get("rule3") + "'";

		// Fuel rule uncheck
		pageObj.settingsPage().clickConversionRule("Fuel");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("conversion_rule_enable_balance", "uncheck");
		pageObj.settingsPage().clickSaveBtn();
		DBUtils.verifyValueFromDBUsingPolling(env, query1, "enable_balance", "0");

		// Fuel rule check
		pageObj.settingsPage().clickConversionRule("Fuel");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("conversion_rule_enable_balance", "check");
		pageObj.settingsPage().clickSaveBtn();
		DBUtils.verifyValueFromDBUsingPolling(env, query1, "enable_balance", "1");

		// currency rule uncheck
		pageObj.settingsPage().clickConversionRule("currency");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("conversion_rule_enable_balance", "uncheck");
		pageObj.settingsPage().clickSaveBtn();
		DBUtils.verifyValueFromDBUsingPolling(env, query2, "enable_balance", "0");

		// currency rule check
		pageObj.settingsPage().clickConversionRule("currency");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("conversion_rule_enable_balance", "check");
		pageObj.settingsPage().clickSaveBtn();
		DBUtils.verifyValueFromDBUsingPolling(env, query2, "enable_balance", "1");

		// Charity rule uncheck
		pageObj.settingsPage().clickConversionRule("Charity");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("conversion_rule_enable_balance", "uncheck");
		pageObj.settingsPage().clickSaveBtn();
		DBUtils.verifyValueFromDBUsingPolling(env, query3, "enable_balance", "0");

		// Charity rule check
		pageObj.settingsPage().clickConversionRule("Charity");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("conversion_rule_enable_balance", "check");
		pageObj.settingsPage().clickSaveBtn();
		DBUtils.verifyValueFromDBUsingPolling(env, query3, "enable_balance", "1");
	}

	// Anant
	@Test(description = "SQ-T4370 Verify the \"points_to_next_level\", \"points_to_retain_level\" do not appear when the flag is turned off for the business", priority = 1, groups = {
			"regression", "dailyrun" })
	@Owner(name = "Vansham Mishra")
	public void T4370_tagsNotAppearWhenFlagOff() throws Exception {
		b_id = dataSet.get("business_id");
		// Login to instance
		//pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		//pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		pageObj.settingsPage().clickMemberLevel("Bronze Level");

		String query3 = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		pageObj.singletonDBUtilsObj();
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query3, "preferences");

		pageObj.singletonDBUtilsObj();
		// set value true
		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag"),
				b_id);
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to true");
		logger.info(dataSet.get("dbFlag") + " value is updated to true");
		TestListeners.extentTest.get().info(dataSet.get("dbFlag") + " value is updated to true");

		List<String> messageTag1 = pageObj.settingsPage().tagsOnMembershipPage(dataSet.get("MessageTag"),
				dataSet.get("tag1"));
		Assert.assertTrue(messageTag1.contains(dataSet.get("tag1")),
				dataSet.get("tag1") + " value is not display when flag is On");
		Assert.assertTrue(messageTag1.contains(dataSet.get("tag1")),
				dataSet.get("tag2") + " value is not display when flag is On");
		logger.info(dataSet.get("tag1") + " and " + dataSet.get("tag2") + " value is display when flag is On");
		TestListeners.extentTest.get()
				.pass(dataSet.get("tag1") + " and " + dataSet.get("tag2") + " value is display when flag is On");

		// set value false
		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		pageObj.singletonDBUtilsObj();
		String expColValue2 = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");
		pageObj.singletonDBUtilsObj();
		boolean flag2 = DBUtils.updateBusinessesPreference(env, expColValue2, "false", dataSet.get("dbFlag"),
				b_id);
		Assert.assertTrue(flag2, dataSet.get("dbFlag") + " value is not updated to true");
		logger.info(dataSet.get("dbFlag") + " value is updated to false");
		TestListeners.extentTest.get().info(dataSet.get("dbFlag") + " value is updated to false");

		pageObj.menupage().navigateToSubMenuItem("Settings", "Memberships");
		pageObj.settingsPage().clickMemberLevel("Bronze Level");

		List<String> messageTagList = pageObj.settingsPage().tagsOnMembershipPageNotVisible(dataSet.get("MessageTag"),
				dataSet.get("tag1"));
		Assert.assertFalse(messageTagList.contains(dataSet.get("tag1")),
				dataSet.get("tag1") + " value is display when flag is Off");
		Assert.assertFalse(messageTagList.contains(dataSet.get("tag2")),
				dataSet.get("tag2") + " value is not display when flag is Off");
		logger.info(dataSet.get("tag1") + " and " + dataSet.get("tag2") + " value is not display when flag is off");
		TestListeners.extentTest.get()
				.pass(dataSet.get("tag1") + " and " + dataSet.get("tag2") + " value is not display when flag is off");
	}

	// Anant
	@Test(description = "SQ-T4348 [Points to Manual] Verify currency, fuel and charity balance in apis when flag is ON with cstore flag ON/OFF", priority = 2, groups = {
			"regression", "dailyrun" })
	@Owner(name = "Vansham Mishra")
	public void T4348_verifyCurrencyfuelCharityWhenFlagOnWithCstore() throws Exception {
		b_id = dataSet.get("business_id");
		List<String> conversionRuleLst = new ArrayList<>();
		conversionRuleLst.add(dataSet.get("conversionRule1"));
		conversionRuleLst.add(dataSet.get("conversionRule2"));
		conversionRuleLst.add(dataSet.get("conversionRule3"));
		// Login to instance
		//pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		//pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Settings", "Conversion Rules");

		// Fuel rule check
		pageObj.settingsPage().clickConversionRule(dataSet.get("conversionRule1"));
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("conversion_rule_enable_balance", "check");
		pageObj.settingsPage().clickSaveBtn();

		// currency rule check
		pageObj.settingsPage().clickConversionRule(dataSet.get("conversionRule2"));
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("conversion_rule_enable_balance", "check");
		pageObj.settingsPage().clickSaveBtn();

		// Charity rule check
		pageObj.settingsPage().clickConversionRule(dataSet.get("conversionRule3"));
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("conversion_rule_enable_balance", "check");
		pageObj.settingsPage().clickSaveBtn();

		logger.info("check the flag 'Show balance in POS Api' in the currency,fuel and charity conversion rule");
		TestListeners.extentTest.get()
				.info("check the flag 'Show balance in POS Api' in the currency,fuel and charity conversion rule");

		// enable flag from the db
		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		pageObj.singletonDBUtilsObj();
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");

		pageObj.singletonDBUtilsObj();
		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag"),
				b_id);
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to true");
		logger.info(dataSet.get("dbFlag") + " value is updated to true");
		TestListeners.extentTest.get().info(dataSet.get("dbFlag") + " value is updated to true");

		// create a user
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signupResponse = pageObj.endpoints().posSignUp(userEmail, dataSet.get("locationkey"));
		Assert.assertEquals(signupResponse.getStatusCode(), 200, "POS signup api response 200 did not matched");
		checkResponseOfApi(signupResponse, "Pos sign up", conversionRuleLst);

		// User Look-up and Fetch Balance
		Response balanceResponse = pageObj.endpoints().posUserLookupFetchBalance(userEmail, dataSet.get("locationkey"));
		Assert.assertEquals(balanceResponse.getStatusCode(), 200, "Error in getting user balance");
		checkResponseOfApi(balanceResponse, "User Look-up and Fetch Balance", conversionRuleLst);

		//
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();

		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap(dataSet.get("itemName"), dataSet.get("itemQty"),
				dataSet.get("amount"), dataSet.get("itemType"), dataSet.get("itemFamily"), dataSet.get("itemGroup"),
				dataSet.get("serialNumber"), dataSet.get("itemId"));
		parentMap.put("Pizza1", detailsMap1);

		// Pos api checkin
		String external_uid = CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T10:50:00+05:30";
		Response resp = pageObj.endpoints().posCheckinN_QC(dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("locationkey"), "21", userEmail, date, external_uid, "21", parentMap);
		Assert.assertEquals(200, resp.getStatusCode(), "Status code 200 did not matched for post chekin api");
		TestListeners.extentTest.get().pass("POS checkin Api is successful");
		logger.info("POS checkinn Api is successful");

		String jsonObjectString = resp.asString();
		JSONObject finalResponse = new JSONObject(jsonObjectString);

		Boolean convertedCategoryBalances = finalResponse.has("converted_category_balances"); // will return false
		Assert.assertEquals(false, convertedCategoryBalances, "converted Category Balances found");
		logger.info("converted Category Balances not found");
		TestListeners.extentTest.get().pass("converted Category Balances not found");

		// disable the flag from db
		query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		pageObj.singletonDBUtilsObj();
		expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");

		pageObj.singletonDBUtilsObj();
		flag = DBUtils.updateBusinessesPreference(env, expColValue, "false", dataSet.get("dbFlag"), b_id);
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to false");
		logger.info(dataSet.get("dbFlag") + " value is updated to false");
		TestListeners.extentTest.get().info(dataSet.get("dbFlag") + " value is updated to false");

		// create a user
		String userEmail2 = pageObj.iframeSingUpPage().generateEmail();
		Response signupResponse2 = pageObj.endpoints().posSignUp(userEmail2, dataSet.get("locationkey"));
		Assert.assertEquals(signupResponse2.getStatusCode(), 200, "POS signup api response 200 did not matched");
		checkResponseOfApi(signupResponse2, "Pos sign up", conversionRuleLst);

		// User Look-up and Fetch Balance
		Response balanceResponse2 = pageObj.endpoints().posUserLookupFetchBalance(userEmail,
				dataSet.get("locationkey"));
		Assert.assertEquals(balanceResponse2.getStatusCode(), 200, "Error in getting user balance");
		checkResponseOfApi(balanceResponse2, "User Look-up and Fetch Balance", conversionRuleLst);

		// Pos api checkin
		external_uid = CreateDateTime.getTimeDateString();
		date = CreateDateTime.getCurrentDate() + "T10:50:00+05:30";
		Response resp2 = pageObj.endpoints().posCheckinN_QC(dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("locationkey"), "21", userEmail, date, external_uid, "21", parentMap);
		Assert.assertEquals(200, resp2.getStatusCode(), "Status code 200 did not matched for post chekin api");
		TestListeners.extentTest.get().pass("POS checkin Api is successful");
		logger.info("POS checkinn Api is successful");
		checkResponseOfApi(resp2, "Pos api checkin", conversionRuleLst);
	}

	// Anant
	@Test(description = "SQ-T4349 [Points to Manual] Verify currency, fuel and charity balance in apis when flag is OFF with cstore flag is ON/OFF", priority = 3, groups = {
			"regression", "dailyrun" })
	@Owner(name = "Vansham Mishra")
	public void T4349_verifyCurrencyfuelCharityWhenFlagOffWithCstore() throws Exception {
		b_id = dataSet.get("business_id");
		List<String> conversionRuleLst = new ArrayList<>();
		conversionRuleLst.add(dataSet.get("conversionRule1"));
		conversionRuleLst.add(dataSet.get("conversionRule2"));
		conversionRuleLst.add(dataSet.get("conversionRule3"));
		// Login to instance
		//pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		//pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Settings", "Conversion Rules");

		// Fuel rule check
		pageObj.settingsPage().clickConversionRule(dataSet.get("conversionRule1"));
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("conversion_rule_enable_balance", "uncheck");
		pageObj.settingsPage().clickSaveBtn();

		// currency rule check
		pageObj.settingsPage().clickConversionRule(dataSet.get("conversionRule2"));
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("conversion_rule_enable_balance", "uncheck");
		pageObj.settingsPage().clickSaveBtn();

		// Charity rule check
		pageObj.settingsPage().clickConversionRule(dataSet.get("conversionRule3"));
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("conversion_rule_enable_balance", "uncheck");
		pageObj.settingsPage().clickSaveBtn();

		logger.info("uncheck the flag 'Show balance in POS Api' in the currency,fuel and charity conversion rule");
		TestListeners.extentTest.get()
				.info("uncheck the flag 'Show balance in POS Api' in the currency,fuel and charity conversion rule");

		// enable flag from the db
		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		pageObj.singletonDBUtilsObj();
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");

		pageObj.singletonDBUtilsObj();
		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag"),
				b_id);
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to true");
		logger.info(dataSet.get("dbFlag") + " value is updated to true");
		TestListeners.extentTest.get().info(dataSet.get("dbFlag") + " value is updated to true");

		// create a user
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signupResponse = pageObj.endpoints().posSignUp(userEmail, dataSet.get("locationkey"));
		Assert.assertEquals(signupResponse.getStatusCode(), 200, "POS signup api response 200 did not matched");
		String balance = signupResponse.jsonPath().get("converted_category_balances").toString();
		Assert.assertEquals(balance, "[]", "");

		// User Look-up and Fetch Balance
		Response balanceResponse = pageObj.endpoints().posUserLookupFetchBalance(userEmail, dataSet.get("locationkey"));
		Assert.assertEquals(balanceResponse.getStatusCode(), 200, "Error in getting user balance");
		String balance2 = balanceResponse.jsonPath().get("converted_category_balances").toString();
		Assert.assertEquals(balance2, "[]", "");

		//
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();

		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap(dataSet.get("itemName"), dataSet.get("itemQty"),
				dataSet.get("amount"), dataSet.get("itemType"), dataSet.get("itemFamily"), dataSet.get("itemGroup"),
				dataSet.get("serialNumber"), dataSet.get("itemId"));
		parentMap.put("Pizza1", detailsMap1);

		// Pos api checkin
		String external_uid = CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T10:50:00+05:30";
		Response resp = pageObj.endpoints().posCheckinN_QC(dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("locationkey"), "21", userEmail, date, external_uid, "21", parentMap);
		Assert.assertEquals(200, resp.getStatusCode(), "Status code 200 did not matched for post chekin api");
		TestListeners.extentTest.get().pass("POS checkin Api is successful");
		logger.info("POS checkinn Api is successful");

		String jsonObjectString = resp.asString();
		JSONObject finalResponse = new JSONObject(jsonObjectString);

		Boolean convertedCategoryBalances = finalResponse.has("converted_category_balances"); // will return false
		Assert.assertEquals(false, convertedCategoryBalances, "converted Category Balances found");
		logger.info("converted Category Balances not found");
		TestListeners.extentTest.get().pass("converted Category Balances not found");

		// disable the flag from db
		query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		pageObj.singletonDBUtilsObj();
		expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");

		pageObj.singletonDBUtilsObj();
		flag = DBUtils.updateBusinessesPreference(env, expColValue, "false", dataSet.get("dbFlag"), b_id);
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to false");
		logger.info(dataSet.get("dbFlag") + " value is updated to false");
		TestListeners.extentTest.get().info(dataSet.get("dbFlag") + " value is updated to false");

		// create a user
		String userEmail2 = pageObj.iframeSingUpPage().generateEmail();
		Response signupResponse2 = pageObj.endpoints().posSignUp(userEmail2, dataSet.get("locationkey"));
		Assert.assertEquals(signupResponse2.getStatusCode(), 200, "POS signup api response 200 did not matched");
		String balance3 = signupResponse.jsonPath().get("converted_category_balances").toString();
		Assert.assertEquals(balance3, "[]", "");

		// User Look-up and Fetch Balance
		Response balanceResponse2 = pageObj.endpoints().posUserLookupFetchBalance(userEmail,
				dataSet.get("locationkey"));
		Assert.assertEquals(balanceResponse2.getStatusCode(), 200, "Error in getting user balance");
		String balance4 = balanceResponse2.jsonPath().get("converted_category_balances").toString();
		Assert.assertEquals(balance4, "[]", "");

		// Pos api checkin
		external_uid = CreateDateTime.getTimeDateString();
		date = CreateDateTime.getCurrentDate() + "T10:50:00+05:30";
		Response resp2 = pageObj.endpoints().posCheckinN_QC(dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("locationkey"), "21", userEmail, date, external_uid, "21", parentMap);
		Assert.assertEquals(200, resp2.getStatusCode(), "Status code 200 did not matched for post chekin api");
		TestListeners.extentTest.get().pass("POS checkin Api is successful");
		logger.info("POS checkinn Api is successful");

//		String jsonObjectString2 = resp2.asString();
//		JSONObject finalResponse2 = new JSONObject(jsonObjectString2);

		Boolean convertedCategoryBalances2 = finalResponse.has("converted_category_balances"); // will return false
		Assert.assertEquals(false, convertedCategoryBalances2, "converted Category Balances found");
		logger.info("converted Category Balances not found");
		TestListeners.extentTest.get().pass("converted Category Balances not found");
	}

	// this method is use to check the 'converted_category_balances' is present in
	// the api response and also there balance is shown
	public void checkResponseOfApi(Response response, String ApiName, List<String> lst) {
		for (int i = 0; i < lst.size(); i++) {
			String conversionName = response.jsonPath().get("converted_category_balances[" + i + "].name").toString();
			Assert.assertTrue(lst.contains(conversionName),
					conversionName + " is not present in the " + ApiName + " response");
			logger.info(conversionName + " is present in the " + ApiName + " response");
			TestListeners.extentTest.get().pass(conversionName + " is  present in the " + ApiName + " response");

			String balance = response.jsonPath().get("converted_category_balances[" + i + "].balance").toString();
			double val = Double.parseDouble(balance);
			Boolean flag;
			if (val >= 0) {
				flag = true;
				Assert.assertTrue(flag,
						"Balance for " + conversionName + " is not present in " + ApiName + " response");
				logger.info("Balance for " + conversionName + " is present in " + ApiName + " response");
				TestListeners.extentTest.get()
						.pass("Balance for " + conversionName + " is present in " + ApiName + " response");
				continue;
			} else {
				flag = false;
				Assert.assertTrue(flag, "Balance for " + conversionName + " is not present in" + ApiName + " response");
			}
		}
	}

	@Test(description = "SQ-T4933 Validate the creation of reward debit entries where a user earns a single reward at a single time.", groups = {
			"regression", "dailyrun" })
	@Owner(name = "Shaleen Gupta")
	public void T4933_verifyRewardDebitEntries() throws Exception {
		pageObj.sidekiqPage().sidekiqCheckWithCustomLimits(baseUrl, "5000", "5000");
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		String b_id = dataSet.get("id");

		/// enable the flag from db
		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		pageObj.singletonDBUtilsObj();
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");

		pageObj.singletonDBUtilsObj();
		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag"),
				b_id);
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to true");
		logger.info(dataSet.get("dbFlag") + " value is updated to true");
		TestListeners.extentTest.get().info(dataSet.get("dbFlag") + " value is updated to true");

		// login to business
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// set redemption mark
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Display");
		pageObj.cockpitRedemptionsPage().updateRedemptionMark(dataSet.get("RedeemptionMark"));
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// Signup using mobile api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		String userID = signUpResponse.jsonPath().get("user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), 200);

		// create a check-in
		Response resp = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationkey"), dataSet.get("amount"));
		Assert.assertEquals(resp.getStatusCode(), 200, "Status code 200 did not matched for post chekin api");

		// check entry in reward_debits table
		String query1 = "select honored_reward_value from reward_debits where user_id=" + userID;
		pageObj.singletonDBUtilsObj();
		String actualColValue1 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query1,
				"honored_reward_value", 20);
		Assert.assertEquals(actualColValue1, "100.0", "Actual column value did not matched with expected column value");
		String query2 = "select converted_to_type from reward_debits where user_id=" + userID;
		pageObj.singletonDBUtilsObj();
		String actualColValue2 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query2,
				"converted_to_type", 2);
		Assert.assertEquals(actualColValue2, "Reward",
				"Actual column value did not matched with expected column value");
		logger.info("Verified entry in reward_debits table");
		TestListeners.extentTest.get().pass("Verified entry in reward_debits table");

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
