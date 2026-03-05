package com.punchh.server.apiTest;

import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.github.javaparser.ast.stmt.Statement;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.ApiUtils;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.DBManager;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;
import com.punchh.server.apiConfig.ApiConstants;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class GiftApiTest_LPE1299 {
	static Logger logger = LogManager.getLogger(GiftApiTest_LPE1299.class);
	public WebDriver driver;
	ApiUtils apiUtils;
	String userEmail;
	// Properties uiProp;
	Properties prop;
	String punchKey, amount;
	PageObj pageObj;
	String sTCName;
	String run = "api";
	private String env;
	private Utilities utils;
	private static Map<String, String> dataSet;
	
	
	@BeforeMethod(alwaysRun = true)
	public void beforeMethod(Method method) {

		// uiProp = Utilities.loadPropertiesFile("config.properties");
		apiUtils = new ApiUtils();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		utils = new Utilities();
		dataSet = new ConcurrentHashMap<>();
		sTCName = method.getName();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run , env), sTCName);
		dataSet = pageObj.readData().readTestData;
		logger.info(sTCName + " ==>" + dataSet);

	}
	@Test(description = "LPE_T2231: Verify exclude_gifted_points_from_tier_progression is set true and exclude_from_membership_points False")
	public void LPE_T2242FlagTrueParamFalse() throws Exception{
		
		String excludeMembershiptierQuery = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
		          + dataSet.get("business_id") + "';";
		  String excludeMembershiptierData = DBUtils.executeQueryAndGetColumnValue(env, excludeMembershiptierQuery, "preferences");
		  boolean excludeMembershiptierFlag = DBUtils.updateBusinessesPreference(env, excludeMembershiptierData, "true",
		        "exclude_gifted_points_from_tier_progression: true", dataSet.get("business_id"));
		  Assert.assertTrue(excludeMembershiptierFlag);

		// User register/signup using API2 Signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("Api2 user signup is successful");
		
		// users/support api
		Response supportGiftingResponse = pageObj.endpoints().sendPointsToUserFlagOn(userID,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(supportGiftingResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for PLATFORM FUNCTIONS API Support Gifting to a User");
		TestListeners.extentTest.get().pass("PLATFORM FUNCTIONS API Support Gifting to a User is successful");
		String query1 = "SELECT exclude_from_membership_points FROM checkins WHERE user_id = '" + userID + "';";
		String value = DBUtils.executeQueryAndGetColumnValue(env, query1,"exclude_from_membership_points");
		Assert.assertTrue(value != null && value.isEmpty());
		
		// send_message api
		Response sendPointResponse = pageObj.endpoints().supportGiftingToUserFlagon(userID, dataSet.get("adminAuthorization"));
		Assert.assertEquals(sendPointResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED, "Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get().pass("Api2  send reward amount to user is successful");
		String value1 = DBUtils.executeQueryAndGetColumnValue(env, query1,"exclude_from_membership_points");
			Assert.assertTrue(value == null && !value.isEmpty());
		
		}
	
	@Test(description = "LPE_T2231: Verify exclude_gifted_points_from_tier_progression is set true and exclude_from_membership_points true")
	public void LPE_T2243FlagTrueParamTrue() throws Exception{
		
		String excludeMembershiptierQuery = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
		          + dataSet.get("business_id") + "';";
		  String excludeMembershiptierData = DBManager.executeQueryAndGetColumnValue(env, excludeMembershiptierQuery, "preferences");
		  boolean excludeMembershiptierFlag = DBUtils.updateBusinessesPreference(env, excludeMembershiptierData, "true",
		        "exclude_gifted_points_from_tier_progression: true", dataSet.get("business_id"));
		  Assert.assertTrue(excludeMembershiptierFlag);

		// User register/signup using API2 Signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("Api2 user signup is successful");
		
		// users/support api
		Response supportGiftingResponse = pageObj.endpoints().sendPointsToUserFlagOnParamTrue(userID,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(supportGiftingResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for PLATFORM FUNCTIONS API Support Gifting to a User");
		TestListeners.extentTest.get().pass("PLATFORM FUNCTIONS API Support Gifting to a User is successful");
		String query1 = "SELECT exclude_from_membership_points FROM checkins WHERE user_id = '" + userID + "';";
		String value = DBUtils.executeQueryAndGetColumnValue(env, query1,"exclude_from_membership_points");
		Assert.assertTrue(value != null || value.isEmpty());
		
		// send_message api
		Response sendPointResponse = pageObj.endpoints().supportGiftingToUserFlagonParamTrue(userID, dataSet.get("adminAuthorization"));
		Assert.assertEquals(sendPointResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED, "Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get().pass("Api2  send reward amount to user is successful");
		String value1 = DBUtils.executeQueryAndGetColumnValue(env, query1,"exclude_from_membership_points");
			Assert.assertTrue(value == null || !value.isEmpty());
		
		}
	}
	
	

