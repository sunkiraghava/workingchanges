package com.punchh.server.OMMTest;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.punchh.server.annotations.Owner;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.apiConfig.ApiResponseJsonSchema;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;
import io.restassured.response.Response;

@Listeners(TestListeners.class)

public class AnonymiseUserCreationViaUserLookUpApiTest {
	private static Logger logger = LogManager.getLogger(AnonymiseUserCreationViaUserLookUpApiTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String sTCName;
	private String env, run = "ui";
	private String userEmail;
	private static Map<String, String> dataSet;
	private Utilities utils;

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
		sTCName = method.getName();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==>" + dataSet);
		utils = new Utilities();
	}

	@Test(description = "SQ-T3138 Validate that user is able to create anonymise guest using lookup_field: none, lookup_value: anonymous || "
			+ "SQ-T3139 Validate that anonymise guest is able to find using email", groups = { "regression",
					"dailyrun" })
	@Owner(name = "Hardik Bhardwaj")
	public void T3138_AnonymiseUserCreationAndVerificationViaUserLookUpApiTest() throws InterruptedException {
		// POS user lookUp
		Response userLookupResponse = pageObj.endpoints().userLookupPosApi("none", "anonymous",
				dataSet.get("locationkey"), "");
		Assert.assertEquals(userLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with POS user lookUp ");
		userEmail = userLookupResponse.jsonPath().getString("email");
		String first_name = userLookupResponse.jsonPath().getString("first_name");
		String last_name = userLookupResponse.jsonPath().getString("last_name");
		String user_id = userLookupResponse.jsonPath().getString("user_id");
		String business_id = userLookupResponse.jsonPath().getString("business_id");
		utils.logPass("POS user lookUp API successfully created an Anonymous User " + userEmail);

		Response userLookupResponse1 = pageObj.endpoints().userLookupPosApi("email", userEmail,
				dataSet.get("location_key"), "");
		Assert.assertEquals(userLookupResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with POS user lookUp ");
		boolean isPosUserLookupSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.posFindUserSchema, userLookupResponse1.asString());
		Assert.assertTrue(isPosUserLookupSchemaValidated, "POS API User Lookup Schema Validation failed");
		String userEmail1 = userLookupResponse1.jsonPath().getString("email");
		String first_name1 = userLookupResponse1.jsonPath().getString("first_name");
		String last_name1 = userLookupResponse1.jsonPath().getString("last_name");
		String user_id1 = userLookupResponse1.jsonPath().getString("user_id");
		String business_id1 = userLookupResponse1.jsonPath().getString("business_id");
		utils.logPass("POS user lookUp API successfully created an Anonymous User " + userEmail);

		Assert.assertEquals(userEmail, userEmail1, "Both userEmail is not same");
		utils.logPass("Both user userEmail are same : " + userEmail);

		Assert.assertEquals(first_name, first_name1, "Both first_name is not same");
		utils.logPass("Both user first_name are same : " + first_name);

		Assert.assertEquals(last_name, last_name1, "Both last_name is not same");
		utils.logPass("Both user last_name are same : " + last_name);

		Assert.assertEquals(user_id, user_id1, "Both user_id is not same");
		utils.logPass("Both user user_id are same : " + user_id);

		Assert.assertEquals(business_id, business_id1, "Both business_id is not same");
		utils.logPass("Both user business_id are same : " + business_id);

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
	}

}