package com.punchh.server.apiTest;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import com.punchh.server.apiConfig.ApiResponseJsonSchema;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.ApiUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;
import com.punchh.server.apiConfig.ApiConstants;

import io.restassured.response.Response;

@Listeners(TestListeners.class)

public class CockpitApiTest {

	private static Logger logger = LogManager.getLogger(CockpitApiTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String sTCName;
	private String env, run = "api";
	private static Map<String, String> dataSet;

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
		sTCName = method.getName();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		// baseUrl = pageObj.getEnvDetails().setBaseUrl();
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run , env), sTCName);
		dataSet = pageObj.readData().readTestData;
		logger.info(sTCName + " ==>" + dataSet);
	}

	@Test(description = "SQ-T3195 cockpit api")
	public void T3195_CockpitApi() {
		// get dashboard config api
		TestListeners.extentTest.get().info("== Get Dashboard Business Config API ==");
		logger.info("== Get Dashboard Business Config API ==");
		Response getResponse = pageObj.endpoints().getDashboardBusinessConfig(dataSet.get("adminAuthorization"),
				dataSet.get("business_ID"));
		Assert.assertEquals(getResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for CockpitAPI");
		boolean isGetBusinessConfigSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.dashboardBusinessConfigSchema, getResponse.asString());
		Assert.assertTrue(isGetBusinessConfigSchemaValidated,
				"Dashboard Get Business Config API Schema Validation failed");
		// String programType = getResponse.jsonPath().get("loyalty_program_type");
		// logger.info(programType);
		Assert.assertEquals(getResponse.jsonPath().get("min_checkin_amount").toString(), "2.0");
		logger.info("Verified expected response for CockpitApi "
				+ getResponse.jsonPath().get("min_checkin_amount").toString());
		TestListeners.extentTest.get().pass("Verified expected response for CockpitApi "
				+ getResponse.jsonPath().get("min_checkin_amount").toString());

		// update dashboard config api
		TestListeners.extentTest.get().info("== Update Dashboard Business Config API ==");
		logger.info("== Update Dashboard Business Config API ==");
		String res = "\"min_checkin_amount\":2.0";
		Response putResponse = pageObj.endpoints().updateDashboardBusinessConfigSingleKey(
				dataSet.get("adminAuthorization"), dataSet.get("business_ID"), res);
		Assert.assertEquals(putResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for CockpitAPI");
		boolean isUpdateBusinessConfigSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.dashboardBusinessConfigSchema, putResponse.asString());
		Assert.assertTrue(isUpdateBusinessConfigSchemaValidated,
				"Dashboard Update Business Config API Schema Validation failed");

		// get dashboard config api
		TestListeners.extentTest.get().info("== Get Dashboard Business Config API ==");
		logger.info("== Get Dashboard Business Config API ==");
		Response getResponse1 = pageObj.endpoints().getDashboardBusinessConfig(dataSet.get("adminAuthorization"),
				dataSet.get("business_ID"));
		Assert.assertEquals(getResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for CockpitAPI");
		Assert.assertEquals(getResponse1.jsonPath().get("min_checkin_amount").toString(), "2.0");
		logger.info("Verified expected response for CockpitApi "
				+ getResponse1.jsonPath().get("min_checkin_amount").toString());
		TestListeners.extentTest.get().pass("Verified expected response for CockpitApi "
				+ getResponse1.jsonPath().get("min_checkin_amount").toString());

		// Get Dashboard Business Config API with invalid admin authorization
		TestListeners.extentTest.get().info("== Get Dashboard Business Config API with invalid admin authorization ==");
		logger.info("== Get Dashboard Business Config API with invalid admin authorization ==");
		Response getBusinessConfigInvalidAuthResponse = pageObj.endpoints().getDashboardBusinessConfig("1",
				dataSet.get("business_ID"));
		Assert.assertEquals(getBusinessConfigInvalidAuthResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED);
		boolean isGetBusinessConfigInvalidAuthSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, getBusinessConfigInvalidAuthResponse.asString());
		Assert.assertTrue(isGetBusinessConfigInvalidAuthSchemaValidated,
				"Dashboard Get Business Config API with invalid admin authorization Schema Validation failed");
		String getBusinessConfigInvalidAuthMsg = getBusinessConfigInvalidAuthResponse.jsonPath().get("error");
		Assert.assertEquals(getBusinessConfigInvalidAuthMsg, "You need to sign in or sign up before continuing.");
		TestListeners.extentTest.get()
				.pass("Verified expected response for Get Dashboard Business Config with invalid admin authorization");
		logger.info("Verified expected response for Get Dashboard Business Config with invalid admin authorization");

		// Get Dashboard Business Config API with invalid business id
		TestListeners.extentTest.get().info("== Get Dashboard Business Config API with invalid business id ==");
		logger.info("== Get Dashboard Business Config API with invalid business id ==");
		Response getBusinessConfigInvalidBusinessIdResponse = pageObj.endpoints()
				.getDashboardBusinessConfig(dataSet.get("adminAuthorization"), "abc");
		Assert.assertEquals(getBusinessConfigInvalidBusinessIdResponse.getStatusCode(), ApiConstants.HTTP_STATUS_NOT_FOUND);
		boolean isGetBusinessConfigInvalidBusinessIdSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, getBusinessConfigInvalidBusinessIdResponse.asString());
		Assert.assertTrue(isGetBusinessConfigInvalidBusinessIdSchemaValidated,
				"Dashboard Get Business Config API with invalid business id Schema Validation failed");
		String getBusinessConfigInvalidBusinessIdMsg = getBusinessConfigInvalidBusinessIdResponse.jsonPath()
				.get("error");
		Assert.assertEquals(getBusinessConfigInvalidBusinessIdMsg, "can't find record with friendly id: \"abc\"");
		TestListeners.extentTest.get()
				.pass("Verified expected response for Get Dashboard Business Config with invalid business id");
		logger.info("Verified expected response for Get Dashboard Business Config with invalid business id");

		// Update Dashboard Business Config API with invalid admin authorization
		TestListeners.extentTest.get()
				.info("== Update Dashboard Business Config API with invalid admin authorization ==");
		logger.info("== Update Dashboard Business Config API with invalid admin authorization ==");
		Response putBusinessConfigInvalidAuthResponse = pageObj.endpoints().updateDashboardBusinessConfigSingleKey("1",
				dataSet.get("business_ID"), res);
		Assert.assertEquals(putBusinessConfigInvalidAuthResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED);
		boolean isUpdateBusinessConfigInvalidAuthSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, putBusinessConfigInvalidAuthResponse.asString());
		Assert.assertTrue(isUpdateBusinessConfigInvalidAuthSchemaValidated,
				"Dashboard Update Business Config API with invalid admin authorization Schema Validation failed");
		String putBusinessConfigInvalidAuthMsg = putBusinessConfigInvalidAuthResponse.jsonPath().get("error");
		Assert.assertEquals(putBusinessConfigInvalidAuthMsg, "You need to sign in or sign up before continuing.");
		TestListeners.extentTest.get().pass(
				"Verified expected response for Update Dashboard Business Config with invalid admin authorization");
		logger.info("Verified expected response for Update Dashboard Business Config with invalid admin authorization");

	}

	@AfterMethod(alwaysRun = true)
	public void afterMethod() {
		pageObj.utils().clearDataSet(dataSet);
	}

}
