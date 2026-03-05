package com.punchh.server.Test;

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

import com.punchh.server.annotations.Owner;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)

public class PerishRewardTest {
	static Logger logger = LogManager.getLogger(PerishRewardTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String userEmail;
	private String sTCName;
	private String baseUrl;
	String blankString = "";
	private String env, run = "ui";
	private static Map<String, String> dataSet;
	private Utilities utils;
	Properties prop;
	String b_id;

	@BeforeClass(alwaysRun = true)
	public void openBrowser() {
		prop = Utilities.loadPropertiesFile("config.properties");
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

	@Test(description = "SQ-T4200 [Points to Currency]Verify v2 account history api that expired items should not be shown", groups = {
			"regression", "dailyrun" })
	@Owner(name = "Hardik Bhardwaj")
	public void T4200_PointsToCurrencyPerishWorker() throws Exception {

		b_id = dataSet.get("business_id");
		String redeemableID = dataSet.get("redeemable_id");
		String redeemableID1 = dataSet.get("redeemable_id1");

//		// DB connection open
//		conn = dbUtils.createMySqlDatabaseConnection(prop.getProperty("pp.host"), prop.getProperty("pp.port"),
//				utils.decrypt(prop.getProperty("pp.username")), utils.decrypt(prop.getProperty("pp.password")));
//		stmt = conn.createStatement();

		// DB - update preference column in business table
		// updating enable_account_improvement to false
		String query3 = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		pageObj.singletonDBUtilsObj();
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query3, "preferences");

		pageObj.singletonDBUtilsObj();
		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag"),
				b_id);
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to false");
		logger.info(dataSet.get("dbFlag") + " value is updated to false");
		utils.logit(dataSet.get("dbFlag") + " value is updated to false");

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("API2 Signup is successful");
		logger.info("API2 Signup is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				redeemableID, "", "");

		logger.info("Send redeemable to the user successfully");
		utils.logPass("Send redeemable to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		// get reward id
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemableID);

		logger.info("Reward id " + rewardId + " is generated successfully ");
		utils.logPass("Reward id " + rewardId + " is generated successfully ");

		// send reward amount to user Reedemable
		Response sendRewardResponse1 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				redeemableID1, "", "");

		logger.info("Send redeemable to the user successfully");
		utils.logPass("Send redeemable to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse1.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		// DB query for updation
		String query = "UPDATE rewards SET end_time = '2023-12-06 07:59:59', status = 'perished' WHERE id = '"
				+ rewardId + "'";
		pageObj.singletonDBUtilsObj();
		int rs = DBUtils.executeUpdateQuery(env, query);
		Assert.assertEquals(rs, 1);

		String query1 = "INSERT INTO reward_archives SELECT * FROM rewards WHERE id = " + rewardId;
		pageObj.singletonDBUtilsObj();
		int rs1 = DBUtils.executeUpdateQuery(env, query1);
		Assert.assertEquals(rs1, 1);

		String query2 = "DELETE FROM rewards WHERE id = '" + rewardId + "'";
		pageObj.singletonDBUtilsObj();
		int rs2 = DBUtils.executeUpdateQuery(env, query2);
		Assert.assertEquals(rs2, 1);

		boolean status = pageObj.guestTimelinePage().accountHistoryApi2(dataSet.get("client"), dataSet.get("secret"),
				token, dataSet.get("redeemableName"), "description");

		Assert.assertEquals(status, false, "Reward is not perished from Account History");
		logger.info("Reward is perished from Account History");
		utils.logPass("Reward is perished from Account History");

		// DB - update preference column in business table
		// updating enable_account_improvement to true
		String query5 = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		pageObj.singletonDBUtilsObj();
		String expColValue1 = DBUtils.executeQueryAndGetColumnValue(env, query5, "preferences");

		pageObj.singletonDBUtilsObj();
		boolean flag1 = DBUtils.updateBusinessesPreference(env, expColValue1, "false", dataSet.get("dbFlag"),
				b_id);
		Assert.assertTrue(flag1, dataSet.get("dbFlag") + " value is not updated to true");
		logger.info(dataSet.get("dbFlag") + " value is updated to true");
		utils.logit(dataSet.get("dbFlag") + " value is updated to true");

		boolean status1 = pageObj.guestTimelinePage().accountHistoryApi2(dataSet.get("client"), dataSet.get("secret"),
				token, dataSet.get("redeemableName") + " (Expired)", "description");
		Assert.assertEquals(status1, true, "Reward is not marked as expired in user Account History");
		logger.info("Reward is marked as expired in user Account History");
		utils.logPass("Reward is marked as expired in user Account History");

//		DBUtils.closeConnection();
	}

	@Test(description = "SQ-T4199 [Points to Manual]Verify v1 secure account history api that expired items should not be shown", groups = {
			"regression", "dailyrun" })
	@Owner(name = "Hardik Bhardwaj")
	public void T4199_PointsToManualPerishWorker() throws Exception {

		b_id = dataSet.get("business_id");
		String redeemableID = dataSet.get("redeemable_id");
		String redeemableID1 = dataSet.get("redeemable_id1");

		// login to instance
		//pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		//pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Set QC in Redeemable
		pageObj.menupage().navigateToSubMenuItem("Settings", "Redeemables");
		pageObj.redeemablePage().searchAndClickOnRedeemable("Base Redeemable");
		pageObj.redeemablePage().removeExistingQualifier();
		pageObj.dashboardpage().checkUncheckToggle("Enable Auto Redemption", "ON");
		pageObj.redeemablePage().addQCinRedeemable("Fuel Discount with 101");
		pageObj.redeemablePage().clickFinishBtn();

		// DB - update preference column in business table
		// updating enable_account_improvement to false
		String query3 = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		pageObj.singletonDBUtilsObj();
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query3, "preferences");

		pageObj.singletonDBUtilsObj();
		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag"),
				b_id);
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to false");
		logger.info(dataSet.get("dbFlag") + " value is updated to false");
		utils.logit(dataSet.get("dbFlag") + " value is updated to false");

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("API2 Signup is successful");
		logger.info("API2 Signup is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				redeemableID, "", "");

		logger.info("Send redeemable to the user successfully");
		utils.logPass("Send redeemable to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		// get reward id
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemableID);

		logger.info("Reward id " + rewardId + " is generated successfully ");
		utils.logPass("Reward id " + rewardId + " is generated successfully ");

		// send reward amount to user Reedemable
		Response sendRewardResponse1 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				redeemableID1, "", "");

		logger.info("Send redeemable to the user successfully");
		utils.logPass("Send redeemable to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse1.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		// DB query for updation
		String query = "UPDATE rewards SET end_time = '2023-12-06 07:59:59', status = 'perished' WHERE id = '"
				+ rewardId + "'";
		pageObj.singletonDBUtilsObj();
		int rs = DBUtils.executeUpdateQuery(env, query);
		Assert.assertEquals(rs, 1);

		String query1 = "INSERT INTO reward_archives SELECT * FROM rewards WHERE id = " + rewardId;
		pageObj.singletonDBUtilsObj();
		int rs1 = DBUtils.executeUpdateQuery(env, query1);
		Assert.assertEquals(rs1, 1);

		String query2 = "DELETE FROM rewards WHERE id = '" + rewardId + "'";
		pageObj.singletonDBUtilsObj();
		int rs2 = DBUtils.executeUpdateQuery(env, query2);
		Assert.assertEquals(rs2, 1);

		boolean status = pageObj.guestTimelinePage().accountHistoryApi1(dataSet.get("client"), dataSet.get("secret"),
				token, dataSet.get("redeemableName"), "description");

		Assert.assertEquals(status, false, "Reward is not perished from Account History");
		logger.info("Reward is perished from Account History");
		utils.logPass("Reward is perished from Account History");

		// DB - update preference column in business table
		// updating enable_account_improvement to true
		String query5 = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		pageObj.singletonDBUtilsObj();
		String expColValue1 = DBUtils.executeQueryAndGetColumnValue(env, query5, "preferences");

		pageObj.singletonDBUtilsObj();
		boolean flag1 = DBUtils.updateBusinessesPreference(env, expColValue1, "false", dataSet.get("dbFlag"),
				b_id);
		Assert.assertTrue(flag1, dataSet.get("dbFlag") + " value is not updated to true");
		logger.info(dataSet.get("dbFlag") + " value is updated to true");
		utils.logit(dataSet.get("dbFlag") + " value is updated to true");

		boolean status1 = pageObj.guestTimelinePage().accountHistoryApi1(dataSet.get("client"), dataSet.get("secret"),
				token, dataSet.get("redeemableName") + " (Expired)", "description");
		Assert.assertEquals(status1, true, "Reward is not marked as expired in user Account History");
		logger.info("Reward is marked as expired in user Account History");
		utils.logPass("Reward is marked as expired in user Account History");

//		DBUtils.closeConnection();
	}

	@Test(description = "SQ-T4203 [Visits Based]Verify auth account history api that expired items should not be shown", groups = {
			"regression", "dailyrun" })
	@Owner(name = "Hardik Bhardwaj")
	public void T4203_VisitsBasedPerishWorker() throws Exception {

		b_id = dataSet.get("business_id");
		String redeemableID = dataSet.get("redeemable_id");
		String redeemableID1 = dataSet.get("redeemable_id1");

		// DB - update preference column in business table
		// updating enable_account_improvement to false
		String query3 = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		pageObj.singletonDBUtilsObj();
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query3, "preferences");

		pageObj.singletonDBUtilsObj();
		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag"),
				b_id);
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to false");
		logger.info(dataSet.get("dbFlag") + " value is updated to false");
		utils.logit(dataSet.get("dbFlag") + " value is updated to false");

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");
		String authToken = signUpResponse.jsonPath().get("authentication_token");
		String token = signUpResponse.jsonPath().get("access_token").toString(); // to be used in non auth api
		String userID = signUpResponse.jsonPath().get("user_id").toString();
		utils.logPass("Auth Signup is successful");
		logger.info("Auth Signup is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				redeemableID, "", "");

		logger.info("Send redeemable to the user successfully");
		utils.logPass("Send redeemable to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		// get reward id
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemableID);

		logger.info("Reward id " + rewardId + " is generated successfully ");
		utils.logPass("Reward id " + rewardId + " is generated successfully ");

		// send reward amount to user Reedemable
		Response sendRewardResponse1 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				redeemableID1, "", "");

		logger.info("Send redeemable to the user successfully");
		utils.logPass("Send redeemable to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse1.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		// DB query for updation
		String query = "UPDATE rewards SET end_time = '2023-12-06 07:59:59', status = 'perished' WHERE id = '"
				+ rewardId + "'";
		pageObj.singletonDBUtilsObj();
		int rs = DBUtils.executeUpdateQuery(env, query);
		Assert.assertEquals(rs, 1);

		String query1 = "INSERT INTO reward_archives SELECT * FROM rewards WHERE id = " + rewardId;
		pageObj.singletonDBUtilsObj();
		int rs1 = DBUtils.executeUpdateQuery(env, query1);
		Assert.assertEquals(rs1, 1);

		String query2 = "DELETE FROM rewards WHERE id = '" + rewardId + "'";
		pageObj.singletonDBUtilsObj();
		int rs2 = DBUtils.executeUpdateQuery(env, query2);
		Assert.assertEquals(rs2, 1);

		boolean status = pageObj.guestTimelinePage().accountHistoryApiAuth(dataSet.get("client"), dataSet.get("secret"),
				authToken, dataSet.get("redeemableName"), "description");

		Assert.assertEquals(status, false, "Reward is not perished from Account History");
		logger.info("Reward is perished from Account History");
		utils.logPass("Reward is perished from Account History");

		// DB - update preference column in business table
		// updating enable_account_improvement to true
		String query5 = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		pageObj.singletonDBUtilsObj();
		String expColValue1 = DBUtils.executeQueryAndGetColumnValue(env, query5, "preferences");

		pageObj.singletonDBUtilsObj();
		boolean flag1 = DBUtils.updateBusinessesPreference(env, expColValue1, "false", dataSet.get("dbFlag"),
				b_id);
		Assert.assertTrue(flag1, dataSet.get("dbFlag") + " value is not updated to true");
		logger.info(dataSet.get("dbFlag") + " value is updated to true");
		utils.logit(dataSet.get("dbFlag") + " value is updated to true");

		boolean status1 = pageObj.guestTimelinePage().accountHistoryApiAuth(dataSet.get("client"),
				dataSet.get("secret"), authToken, dataSet.get("redeemableName") + " (Expired)", "description");
		Assert.assertEquals(status1, true, "Reward is not marked as expired in user Account History");
		logger.info("Reward is marked as expired in user Account History");
		utils.logPass("Reward is marked as expired in user Account History");

//		DBUtils.closeConnection();
	}

	@Test(description = "SQ-T4202 [Points Unlock Redeemable]Verify v1 secure account history api that expired items should not be shown", groups = {
			"regression", "dailyrun" })
	@Owner(name = "Hardik Bhardwaj")
	public void T4202_PointsUnlockRedeemablePerishWorker() throws Exception {

		b_id = dataSet.get("business_id");
		String redeemableID = dataSet.get("redeemable_id");
		String redeemableID1 = dataSet.get("redeemable_id1");

		// DB - update preference column in business table
		// updating enable_account_improvement to false
		String query3 = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		pageObj.singletonDBUtilsObj();
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query3, "preferences");

		pageObj.singletonDBUtilsObj();
		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag"),
				b_id);
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to false");
		logger.info(dataSet.get("dbFlag") + " value is updated to false");
		utils.logit(dataSet.get("dbFlag") + " value is updated to false");

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("API2 Signup is successful");
		logger.info("API2 Signup is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				redeemableID, "", "");

		logger.info("Send redeemable to the user successfully");
		utils.logPass("Send redeemable to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		// get reward id
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemableID);

		logger.info("Reward id " + rewardId + " is generated successfully ");
		utils.logPass("Reward id " + rewardId + " is generated successfully ");

		// send reward amount to user Reedemable
		Response sendRewardResponse1 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				redeemableID1, "", "");

		logger.info("Send redeemable to the user successfully");
		utils.logPass("Send redeemable to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse1.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		// DB query for updation
		String query = "UPDATE rewards SET end_time = '2023-12-06 07:59:59', status = 'perished' WHERE id = '"
				+ rewardId + "'";
		pageObj.singletonDBUtilsObj();
		int rs = DBUtils.executeUpdateQuery(env, query);
		Assert.assertEquals(rs, 1);

		String query1 = "INSERT INTO reward_archives SELECT * FROM rewards WHERE id = " + rewardId;
		pageObj.singletonDBUtilsObj();
		int rs1 = DBUtils.executeUpdateQuery(env, query1);
		Assert.assertEquals(rs1, 1);

		String query2 = "DELETE FROM rewards WHERE id = '" + rewardId + "'";
		pageObj.singletonDBUtilsObj();
		int rs2 = DBUtils.executeUpdateQuery(env, query2);
		Assert.assertEquals(rs2, 1);

		boolean status = pageObj.guestTimelinePage().accountHistoryApi1(dataSet.get("client"), dataSet.get("secret"),
				token, dataSet.get("redeemableName"), "description");

		Assert.assertEquals(status, false, "Reward is not perished from Account History");
		logger.info("Reward is perished from Account History");
		utils.logPass("Reward is perished from Account History");

		// DB - update preference column in business table
		// updating enable_account_improvement to true
		String query5 = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		pageObj.singletonDBUtilsObj();
		String expColValue1 = DBUtils.executeQueryAndGetColumnValue(env, query5, "preferences");

		pageObj.singletonDBUtilsObj();
		boolean flag1 = DBUtils.updateBusinessesPreference(env, expColValue1, "false", dataSet.get("dbFlag"),
				b_id);
		Assert.assertTrue(flag1, dataSet.get("dbFlag") + " value is not updated to true");
		logger.info(dataSet.get("dbFlag") + " value is updated to true");
		utils.logit(dataSet.get("dbFlag") + " value is updated to true");

		boolean status1 = pageObj.guestTimelinePage().accountHistoryApi1(dataSet.get("client"), dataSet.get("secret"),
				token, dataSet.get("redeemableName") + " (Expired)", "description");
		Assert.assertEquals(status1, true, "Reward is not marked as expired in user Account History");
		logger.info("Reward is marked as expired in user Account History");
		utils.logPass("Reward is marked as expired in user Account History");

//		DBUtils.closeConnection();
	}

	@Test(description = "SQ-T4201 [Points to Reward]Verify v2 account history api that expired items should not be shown", groups = {
			"regression", "dailyrun" })
	@Owner(name = "Hardik Bhardwaj")
	public void T4201_PointsToRewardPerishWorker() throws Exception {

		b_id = dataSet.get("business_id");
		String redeemableID = dataSet.get("redeemable_id");
		String redeemableID1 = dataSet.get("redeemable_id1");

		// DB - update preference column in business table
		// updating enable_account_improvement to false
		String query3 = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		pageObj.singletonDBUtilsObj();
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query3, "preferences");

		pageObj.singletonDBUtilsObj();
		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag"),
				b_id);
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to false");
		logger.info(dataSet.get("dbFlag") + " value is updated to false");
		utils.logit(dataSet.get("dbFlag") + " value is updated to false");

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("API2 Signup is successful");
		logger.info("API2 Signup is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				redeemableID, "", "");

		logger.info("Send redeemable to the user successfully");
		utils.logPass("Send redeemable to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		// get reward id
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemableID);

		logger.info("Reward id " + rewardId + " is generated successfully ");
		utils.logPass("Reward id " + rewardId + " is generated successfully ");

		// send reward amount to user Reedemable
		Response sendRewardResponse1 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				redeemableID1, "", "");

		logger.info("Send redeemable to the user successfully");
		utils.logPass("Send redeemable to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse1.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		// DB query for updation
		String query = "UPDATE rewards SET end_time = '2023-12-06 07:59:59', status = 'perished' WHERE id = '"
				+ rewardId + "'";
		pageObj.singletonDBUtilsObj();
		int rs = DBUtils.executeUpdateQuery(env, query);
		Assert.assertEquals(rs, 1);

		String query1 = "INSERT INTO reward_archives SELECT * FROM rewards WHERE id = " + rewardId;
		pageObj.singletonDBUtilsObj();
		int rs1 = DBUtils.executeUpdateQuery(env, query1);
		Assert.assertEquals(rs1, 1);

		String query2 = "DELETE FROM rewards WHERE id = '" + rewardId + "'";
		pageObj.singletonDBUtilsObj();
		int rs2 = DBUtils.executeUpdateQuery(env, query2);
		Assert.assertEquals(rs2, 1);

		boolean status = pageObj.guestTimelinePage().accountHistoryApi2(dataSet.get("client"), dataSet.get("secret"),
				token, dataSet.get("redeemableName"), "description");

		Assert.assertEquals(status, false, "Reward is not perished from Account History");
		logger.info("Reward is perished from Account History");
		utils.logPass("Reward is perished from Account History");

		// DB - update preference column in business table
		// updating enable_account_improvement to true
		String query5 = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		pageObj.singletonDBUtilsObj();
		String expColValue1 = DBUtils.executeQueryAndGetColumnValue(env, query5, "preferences");

		pageObj.singletonDBUtilsObj();
		boolean flag1 = DBUtils.updateBusinessesPreference(env, expColValue1, "false", dataSet.get("dbFlag"),
				b_id);
		Assert.assertTrue(flag1, dataSet.get("dbFlag") + " value is not updated to true");
		logger.info(dataSet.get("dbFlag") + " value is updated to true");
		utils.logit(dataSet.get("dbFlag") + " value is updated to true");

		utils.longWaitInSeconds(4);
		boolean status1 = pageObj.guestTimelinePage().accountHistoryApi2(dataSet.get("client"), dataSet.get("secret"),
				token, dataSet.get("redeemableName") + " (Expired)", "description");
		Assert.assertEquals(status1, true, "Reward is not marked as expired in user Account History");
		logger.info("Reward is marked as expired in user Account History");
		utils.logPass("Reward is marked as expired in user Account History");

//		DBUtils.closeConnection();
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
