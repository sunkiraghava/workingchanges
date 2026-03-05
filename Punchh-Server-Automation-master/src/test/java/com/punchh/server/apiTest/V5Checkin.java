package com.punchh.server.apiTest;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Properties;
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
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;
import com.punchh.server.apiConfig.ApiConstants;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class V5Checkin {

	private static Logger logger = LogManager.getLogger(V5Checkin.class);
	public WebDriver driver;
	private Properties prop;
	private PageObj pageObj;
	private String userEmail;
	private String sTCName;
	private String baseUrl;
	private String env, run = "api";
	private static Map<String, String> dataSet;

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {

		prop = Utilities.loadPropertiesFile("config.properties");
		sTCName = method.getName();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run , env), sTCName);
		dataSet = pageObj.readData().readTestData;
		logger.info(sTCName + " ==>" + dataSet);

	}

	@Test(description = "SQ-T2908 Validate 1F Upgrade to Ruby 2.7 - Punchh Server-Core loyalty(V5 checkin", groups = "api", priority = 0)
	public void T2908_verifytheV5checkin() throws InterruptedException {
		// user creation
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 1 user signup");
//		String token = signUpResponse.jsonPath().get("auth_token.token").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api1 signup");
		TestListeners.extentTest.get().pass("Api1 user signup is successful");

		// Checkin API
		TestListeners.extentTest.get().info("== V5 Checkin ==");
		logger.info("== V5 Checkin ==");
		String receipt_datetime = CreateDateTime.getCurrentDate() + "T10:50:00+05:30";
		String transaction_no = "6724744734001";
		Response createPickupResponse = pageObj.endpoints().CheckinV5API(userEmail, transaction_no, receipt_datetime,
				dataSet.get("locationKey"));
		Assert.assertEquals(createPickupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for the V5 Checkin API");
		boolean isCheckinSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiV5RedemptionSchema, createPickupResponse.asString());
		Assert.assertTrue(isCheckinSchemaValidated, "API V5 Checkin Schema Validation failed");
		TestListeners.extentTest.get().pass("V5 Checkin is successful");
		logger.info("V5 Checkin is successful");

		// API V5 Checkin with invalid user email
		TestListeners.extentTest.get().info("== V5 Checkin with invalid user email ==");
		logger.info("== V5 Checkin with invalid user email ==");
		Response checkinInvalidEmailResponse = pageObj.endpoints().CheckinV5API("1", transaction_no, receipt_datetime,
				dataSet.get("locationKey"));
		Assert.assertEquals(checkinInvalidEmailResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY);
		boolean isCheckinInvalidEmailSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, checkinInvalidEmailResponse.asString());
		Assert.assertTrue(isCheckinInvalidEmailSchemaValidated,
				"API V5 Checkin with invalid user email Schema Validation failed");
		String redemptionWithRewardInvalidEmailMsg = checkinInvalidEmailResponse.jsonPath().getString("[0]");
		Assert.assertEquals(redemptionWithRewardInvalidEmailMsg, "Email is invalid", "Message does not match");
		TestListeners.extentTest.get().pass("API V5 Checkin with invalid user email is unsuccessful");
		logger.info("API V5 Checkin with invalid user email is unsuccessful");

	}

	@AfterMethod(alwaysRun = true)
	public void afterMethod() {
		pageObj.utils().clearDataSet(dataSet);
	}
}
