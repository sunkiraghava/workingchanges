package com.punchh.server.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
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

import com.punchh.server.annotations.Owner;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.TestListeners;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class ZipCodeTest {
	private static Logger logger = LogManager.getLogger(ZipCodeTest.class);
	public WebDriver driver;
	private String userEmail;
	private PageObj pageObj;
	private String sTCName;
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
		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
	}

	@Test(description = "SQ-T2652 Verify the Zip code validation", priority = 0, groups = { "regression",
			"dailyrun" })
	@Owner(name = "Ashwini Shetty")
	public void T2652_VerifyZipCodeValidation() throws InterruptedException {
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "iFrame Configuration");
		pageObj.dashboardpage().enableZipCode();

		// ******signup using iframe with zipcode*********
		// Sign up using the right zip code
		pageObj.iframeSingUpPage().navigateToIframe(baseUrl + dataSet.get("whiteLabel") + dataSet.get("slug"));
		pageObj.iframeSingUpPage().iframeSignUpWithZipcode(dataSet.get("inValidZipCode"));

		ArrayList<String> chromeTabs = new ArrayList<>(driver.getWindowHandles());
		driver.close();
		driver.switchTo().window(chromeTabs.get(0));
		pageObj.iframeSingUpPage().navigateToIframe(baseUrl + dataSet.get("whiteLabel") + dataSet.get("slug"));
		pageObj.iframeSingUpPage().iframeSignUpWithZipcode(dataSet.get("validZipCode"));

		String errorMsg = pageObj.iframeSingUpPage().editZipcodeInIframe(dataSet.get("inValidZipCode"),
				"inValidZipCode");
		Assert.assertEquals(errorMsg, "ZipCode is too long (maximum is 10 characters)",
				"Error message is not as expected");
		logger.info("Zip code is too long error message is verified");
		TestListeners.extentTest.get().pass("Zip code is too long error message is verified");

		String errorMsg2 = pageObj.iframeSingUpPage().editZipcodeInIframe(dataSet.get("validZipCode"), "validZipCode");
		Assert.assertEquals(errorMsg2, "You updated your account successfully.", "Error message is not as expected");
		logger.info("Zip code is valid and updated successfully");
		TestListeners.extentTest.get().pass("Zip code is valid and updated successfully");

		// pageObj.iframeSingUpPage().iframeSignOut();

		// Sign up using Mobile signup API
		// Sign up using the right zip code
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 1 user signup");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api1 signup");
		TestListeners.extentTest.get().pass("Api1 user signup is successful");

		// Sign up using the wrong zip code
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse1 = pageObj.endpoints().Api1UserSignUpWrongZipcode(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY, "Status code 422 did not matched for api1 signup");
		TestListeners.extentTest.get().pass("Api1 user signup is unsuccessful");

		// Sign up using Pos signup API
		// Sign up using the right zip code
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response resp = pageObj.endpoints().posSignUp(userEmail, dataSet.get("locationKey"));
		Assert.assertEquals(resp.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for pos chekin api");
		TestListeners.extentTest.get().pass("POS signup is successful");

		// Sign up using the wrong zip code
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response resp1 = pageObj.endpoints().posSignUpNegativeZipcode(userEmail, dataSet.get("locationKey"));
		Assert.assertEquals(resp1.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY, "Status code 422 did not matched for pos chekin api");
		TestListeners.extentTest.get().pass("POS signup is unsuccessful");

		// sign up using Auth API
		// Sign up using the right zip code
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response AuthsignUpResponse = pageObj.endpoints().authApiSignUpPositive(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyCreateResponse(AuthsignUpResponse, "Auth API user signup");
		Assert.assertEquals(AuthsignUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");
		TestListeners.extentTest.get().pass("Auth API signup is successful");

		// sign up using the wrong zip code
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response AuthsignUpResponse1 = pageObj.endpoints().authApiSignUpNegative(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(AuthsignUpResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for auth user signup api");
		TestListeners.extentTest.get().pass("Auth API signup is unsuccessful");

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
