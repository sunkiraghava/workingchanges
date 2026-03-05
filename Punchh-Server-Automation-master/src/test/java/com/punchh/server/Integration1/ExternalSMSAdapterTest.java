package com.punchh.server.Integration1;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.mongodb.client.model.Sorts;
import com.punchh.server.annotations.Owner;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.ApiUtils;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class ExternalSMSAdapterTest {
	static Logger logger = LogManager.getLogger(ExternalSMSAdapterTest.class);
	public WebDriver driver;
	String userEmail;
	String email = "autoemailTemp@punchh.com";
	ApiUtils apiUtils;
	PageObj pageObj;
	String sTCName;
	private String timeStamp;
	private String env, run = "ui";
	private String baseUrl;
	private static Map<String, String> dataSet;
	private Utilities utils;
	private Properties prop;

	@BeforeClass(alwaysRun = true)
	public void openBrowser() {
		prop = Utilities.loadPropertiesFile("config.properties");
		driver = new BrowserUtilities().launchBrowser();
		pageObj = new PageObj(driver);
		utils = new Utilities(driver);
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

	@Test(description = "SQ-T2945 [Attentive] - Verification of Bulk & Non Bulk SMS triggering functionality",groups = {"nonNightly" }, priority = 0)
	@Owner(name = "Rohit Doraya")
	public void T2945_Attentive_Bulk_Non_Bulk_SMS_Triggering_Flows() throws Exception {

		// Call the deleteDocuments method
		Bson delFilter = and(eq("business_uuid", dataSet.get("business_uuid")));
		long deletedCount = pageObj.mongoDBUtils().deleteDocuments(env, "other", "helios_messaging", delFilter);
		logger.info("Deleted " + deletedCount + " documents.");
		TestListeners.extentTest.get().info("Deleted " + deletedCount + " documents.");

		// Select Test Business
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Campaigns");
		pageObj.dashboardpage().smsAdapter("Attentive");
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "Integration Services");
		pageObj.integrationServicesPage().enableBulkSms(false);
		logger.info("Attentive SMS Integration is updated with Enable Bulk SMS unchecked");

		String amount = dataSet.get("amount");
		Response checkinResp = pageObj.endpoints().posCheckinPhone(dataSet.get("valid_phone_without_country_code"),
				dataSet.get("buregerMongerLocationKey"), amount);
		Assert.assertEquals(checkinResp.getStatusCode(), 200, "Status code 200 did not matched for POS checkin");
		TestListeners.extentTest.get().pass("POS checkin API is successful with amount: " + amount);

		Bson filter = and(eq("business_uuid", dataSet.get("business_uuid")), eq("method", "send_message"),
				eq("status", "success"), eq("request_params.user_id", Integer.parseInt(dataSet.get("valid_user_id"))),
				eq("request_params.to", dataSet.get("valid_phone_with_country_code")),
				eq("request_params.campaign_type", "NotificationTemplate"));
		Bson sort = Sorts.descending("created_at");

		Document result = pageObj.mongoDBUtils().getSingleDocumentWithPolling(env, "other", "helios_messaging", filter,
				sort, 1, 5, 10);
		Assert.assertNotNull(result, "No document found matching the criteria in the database.");

		// Document result = getValidatedDocument(filter, sort, 1, "Send Message
		// Verification");
		Assert.assertEquals(utils.getFieldValue(result, "response_params.formattedMessageBody", String.class),
				utils.getFieldValue(result, "request_params.body", String.class).trim());
		Assert.assertEquals(utils.getFieldValue(result, "response_params.to", String.class),
				dataSet.get("valid_phone_with_country_code"));
		logger.info("Non Bulking SMS verification has been verified " + result.toJson());
		TestListeners.extentTest.get().info("Non Bulking SMS verification has been verified " + result.toJson());

		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "Integration Services");
		pageObj.integrationServicesPage().enableBulkSms(true);
		logger.info("Attentive SMS Integration is updated with Enable Bulk SMS checked");

		Response checkinResp1 = pageObj.endpoints().posCheckinPhone(dataSet.get("valid_phone_without_country_code"),
				dataSet.get("buregerMongerLocationKey"), amount);
		Assert.assertEquals(checkinResp1.getStatusCode(), 200, "Status code 200 did not matched for POS checkin");
		TestListeners.extentTest.get().pass("POS checkin API is successful with amount: " + amount);

		Bson filter1 = and(eq("business_uuid", dataSet.get("business_uuid")), eq("method", ""), eq("status", "success"),
				eq("request_params.body.to", dataSet.get("valid_phone_without_country_code")),
				eq("request_params.body.campaign_type", "NotificationTemplate"));

		Document result1 = pageObj.mongoDBUtils().getSingleDocumentWithPolling(env, "other", "helios_messaging",
				filter1, sort, 1, 5, 10);
		Assert.assertNotNull(result1, "No document found matching the criteria in the database.");
		// Document result1 = getValidatedDocument(filter1, sort, 1, "Bulk Message Data
		// Verification");

		// Document result1 = getValidatedDocument(filter1, sort, 1, "Bulk Message Data
		// Verification");
		Assert.assertEquals(utils.getFieldValue(result1, "vendor_response_params.failed", Double.class), 0);
		Assert.assertTrue(utils.getFieldValue(result1, "vendor_response_params.succeeded", Double.class) > 0);
		logger.info("Bulking SMS verification has been verified " + result1.toJson());
		TestListeners.extentTest.get().info("Bulking SMS verification has been verified " + result1.toJson());

	}

	@Test(description = "SQ-T5984 [Attentive] Verify the phone and subscription status update through Attentive Webhook",groups = {"nonNightly" }, priority = 1)
	@Owner(name = "Rohit Doraya")
	public void T5984_Verify_phone_and_subscription_status_update_through_attentive_webhook() throws Exception {

		String phoneNumber = dataSet.get("phone");
		// Select Test Business
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().checkUncheckFlagCockpitDashboard("Enable SMS?", "check");
		pageObj.dashboardpage().checkUncheckFlagCockpitDashboard("Enable Text to Join?", "check");
		pageObj.dashboardpage().clickOnUpdateButton();
		utils.longWaitInSeconds(4);

		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), 200, "Status code 200 did not matched for api1 signup");
		String userId = signUpResponse.jsonPath().getString("user_id");
		String userToken = signUpResponse.jsonPath().getString("auth_token.token");
		TestListeners.extentTest.get().pass(userEmail + " Api1 user signup is successful");

		Response response = pageObj.endpoints().smsWebhookAPI(dataSet.get("client"), dataSet.get("secret"), "attentive",
				dataSet.get("business_uuid"), phoneNumber, userEmail, "loyalty", true, false, false);
		Assert.assertEquals(response.getStatusCode(), 202, "Having some problem in Web hook API response code");

		String[] db_columns = { "phone", "subscription_status" };
		String query = "SELECT phone, subscription_status " + "FROM `users` " + "WHERE `users`.business_id = '"
				+ dataSet.get("business_id") + "' " + "AND email = '" + userEmail + "' " + "ORDER BY id ASC LIMIT 1;";

		List<Map<String, String>> data = DBUtils.executeQueryAndGetMultipleColumns(env, query, db_columns);
		Assert.assertEquals(data.get(0).get("phone"), phoneNumber, "Phone is not matched with DB");
		Assert.assertEquals(data.get(0).get("subscription_status"), "11", "Subscription status is not matched with DB");
		logger.info("Phone number has successfully update with user email of " + userEmail
				+ " with sms_subscription status of 11");
		TestListeners.extentTest.get().info("Phone number has successfully update with user email of " + userEmail
				+ " with sms_subscription status of 11");

		Response response1 = pageObj.endpoints().smsWebhookAPI(dataSet.get("client"), dataSet.get("secret"),
				"attentive", dataSet.get("business_uuid"), phoneNumber, userEmail, "loyalty", false, false, false);
		Assert.assertEquals(response1.getStatusCode(), 202, "Having some problem in Web hook API response code");
		String query1 = "SELECT subscription_status " + "FROM `users` " + "WHERE `users`.business_id = '"
				+ dataSet.get("business_id") + "' " + "AND id = '" + userId + "' " + "ORDER BY id ASC LIMIT 1;";
		String sms_subscription = DBUtils.executeQueryAndGetColumnValue(env, query1, "subscription_status");
		Assert.assertEquals(sms_subscription, "3", "SMS Subscription status is not matched with DB");
		logger.info("SMS subscription status is verified with DB with status: " + sms_subscription);
		TestListeners.extentTest.get()
				.info("SMS subscription status is verified with DB with status: " + sms_subscription);

		Response updateUserResp = pageObj.endpoints().api1UpdateUser(dataSet.get("client"), dataSet.get("secret"),
				userToken, "phone", "");
		Assert.assertEquals(updateUserResp.getStatusCode(), 200, "Having some problem in Update User API");
		Assert.assertTrue(updateUserResp.jsonPath().getString("phone").trim().isEmpty(), "Phone number is not removed");
		logger.info("Phone number is removed from user profile");
		TestListeners.extentTest.get().pass("Phone number is removed from user profile");

	}

	@Test(description = "SQ-T6200 [TapOnIt] Verify the phone and subscription status update through Attentive Webhook",groups = {"nonNightly" }, priority = 2)
	@Owner(name = "Rohit Doraya")
	public void T6200_Verify_phone_and_subscription_status_update_through_taponit_webhook() throws Exception {

		// Update the flag "enable_only_sms_subscription_update" to true in business to
		// enable the TapOnIt SMS Webhook
		String bizPrefGetQuery = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ dataSet.get("business_id") + "';";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, bizPrefGetQuery, "preferences");
		DBUtils.updateBusinessesPreference(env, expColValue, "true", "enable_only_sms_subscription_update",
				dataSet.get("business_id"));

		String phoneNumber = dataSet.get("phone");
		// Select Test Business
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().checkUncheckFlagCockpitDashboard("Enable SMS?", "check");
		pageObj.dashboardpage().checkUncheckFlagCockpitDashboard("Enable Text to Join?", "check");
		pageObj.dashboardpage().clickOnUpdateButton();
		utils.longWaitInSeconds(4);
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Campaigns");
		pageObj.dashboardpage().smsAdapter("TapOnIt");

		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"), Long.parseLong(phoneNumber));
		Assert.assertEquals(signUpResponse.getStatusCode(), 200, "Status code 200 did not matched for api2 signup");
		String userId = signUpResponse.jsonPath().getString("user.user_id");
		String userToken = signUpResponse.jsonPath().getString("access_token.token");
		TestListeners.extentTest.get()
				.pass(userEmail + " Api2 user signup is successful with phone number: " + phoneNumber);

		Response response = pageObj.endpoints().smsWebhookAPI(dataSet.get("client"), dataSet.get("secret"), "taponit",
				dataSet.get("business_uuid"), phoneNumber, "", "loyalty", true, false, false);
		Assert.assertEquals(response.getStatusCode(), 202, "Having some problem in Web hook API response code");

		String[] db_columns = { "phone", "subscription_status" };
		String query = "SELECT phone, subscription_status " + "FROM `users` " + "WHERE `users`.business_id = '"
				+ dataSet.get("business_id") + "' " + "AND email = '" + userEmail + "' " + "ORDER BY id ASC LIMIT 1;";

		List<Map<String, String>> data = DBUtils.executeQueryAndGetMultipleColumns(env, query, db_columns);
		Assert.assertEquals(data.get(0).get("phone"), phoneNumber, "Phone is not matched with DB");
		Assert.assertEquals(data.get(0).get("subscription_status"), "11", "Subscription status is not matched with DB");
		logger.info("Phone number has successfully update with user email of " + userEmail
				+ " with sms_subscription status of 11");
		TestListeners.extentTest.get().info("Phone number has successfully update with user email of " + userEmail
				+ " with sms_subscription status of 11");

		Response response1 = pageObj.endpoints().smsWebhookAPI(dataSet.get("client"), dataSet.get("secret"), "taponit",
				dataSet.get("business_uuid"), phoneNumber, "", "loyalty", false, false, false);
		Assert.assertEquals(response1.getStatusCode(), 202, "Having some problem in Web hook API response code");
		String query1 = "SELECT subscription_status " + "FROM `users` " + "WHERE `users`.business_id = '"
				+ dataSet.get("business_id") + "' " + "AND id = '" + userId + "' " + "ORDER BY id ASC LIMIT 1;";
		String sms_subscription = DBUtils.executeQueryAndGetColumnValue(env, query1, "subscription_status");
		Assert.assertEquals(sms_subscription, "3", "SMS Subscription status is not matched with DB");
		logger.info("SMS subscription status is verified with DB with status: " + sms_subscription);
		TestListeners.extentTest.get()
				.info("SMS subscription status is verified with DB with status: " + sms_subscription);

		Response updateUserResp = pageObj.endpoints().api1UpdateUser(dataSet.get("client"), dataSet.get("secret"),
				userToken, "phone", "");
		Assert.assertEquals(updateUserResp.getStatusCode(), 200, "Having some problem in Update User API");
		Assert.assertTrue(updateUserResp.jsonPath().getString("phone").trim().isEmpty(), "Phone number is not removed");
		logger.info("Phone number is removed from user profile");
		TestListeners.extentTest.get().pass("Phone number is removed from user profile");

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